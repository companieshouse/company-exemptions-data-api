package uk.gov.companieshouse.exemptions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.exemptions.InternalExemptionsApi;
import uk.gov.companieshouse.logging.Logger;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class ExemptionsServiceImpl implements ExemptionsService {

    @Autowired
    private Logger logger;

    @Autowired
    private ExemptionsRepository repository;

    @Autowired
    private ExemptionsMapper mapper;

    @Autowired
    private ExemptionsApiService exemptionsApiService;

    @Override
    public ServiceStatus upsertCompanyExemptions(String contextId, String companyNumber, InternalExemptionsApi requestBody) {
        if (isLatestRecord(companyNumber, requestBody.getInternalData().getDeltaAt())) {
            CompanyExemptionsDocument document = mapper.map(companyNumber, requestBody);

            try {
                // If the document to be updated already has created_at then reuse it
                // otherwise, set it to the delta's updated_at field
                repository.findById(companyNumber).map(CompanyExemptionsDocument::getCreated)
                        .ifPresentOrElse((document::setCreated),
                                () -> document.setCreated(new Created().setAt(document.getUpdated().getAt())));

                repository.save(document);
                logger.info(String.format("Company exemptions for company number: %s updated in MongoDb for context id: %s",
                        companyNumber,
                        contextId));

                ServiceStatus serviceStatus = exemptionsApiService.invokeChsKafkaApi(new ResourceChangedRequest(contextId, companyNumber, null, false));
                logger.info(String.format("ChsKafka api CHANGED invoked updated successfully for context id: %s and company number: %s",
                        contextId,
                        companyNumber));

                return serviceStatus;
            } catch (IllegalArgumentException ex) {
                logger.error("Illegal argument exception caught when processing upsert", ex);
                return ServiceStatus.SERVER_ERROR;
            } catch (DataAccessException ex) {
                logger.error("Error connecting to MongoDB");
                return ServiceStatus.SERVER_ERROR;
            }
        } else {
            logger.info("Company exemptions record not persisted as it is not the latest record.");
            return ServiceStatus.CLIENT_ERROR;
        }
    }

    @Override
    public Optional<CompanyExemptionsDocument> getCompanyExemptions(String companyNumber) {
        try {
            return repository.findById(companyNumber);
        } catch (DataAccessException ex) {
            logger.error("Failed to connect to MongoDb", ex);
            throw new ServiceUnavailableException("Data access exception thrown when calling Mongo Repository");
        }
    }

    @Override
    public ServiceStatus deleteCompanyExemptions(String contextId, String companyNumber) {
        try {
            Optional<CompanyExemptionsDocument> document = repository.findById(companyNumber);
            if (document.isEmpty()) {
                logger.error(String.format("Company exemptions do not exist for company number %s", companyNumber));
                return ServiceStatus.CLIENT_ERROR;
            }
            ServiceStatus serviceStatus = exemptionsApiService.invokeChsKafkaApi(new ResourceChangedRequest(contextId, companyNumber, document.get().getData(), true));

            if (ServiceStatus.SUCCESS.equals(serviceStatus)) {
                logger.info(String.format("ChsKafka api DELETED invoked successfully for context id: %s and company number: %s", contextId, companyNumber));
                repository.deleteById(companyNumber);
                logger.info(String.format("Company exemptions for company number: %s deleted in MongoDb for context id: %s", companyNumber, contextId));
            }

            return serviceStatus;
        } catch (IllegalArgumentException ex) {
            logger.error("Error calling chs-kafka-api");
            return ServiceStatus.SERVER_ERROR;
        } catch (DataAccessException ex) {
            logger.error("Error connecting to MongoDB");
            return ServiceStatus.SERVER_ERROR;
        }
    }

    private boolean isLatestRecord(String companyNumber, OffsetDateTime deltaAt) {
        String formattedDate = deltaAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        return repository.findUpdatedExemptions(companyNumber, formattedDate).isEmpty();
    }
}
