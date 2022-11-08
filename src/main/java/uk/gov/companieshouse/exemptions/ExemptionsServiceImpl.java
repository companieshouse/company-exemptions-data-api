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

            // If the document to be updated already has created_at then reuse it
            // otherwise, set it to the delta's updated_at field
            repository.findById(companyNumber).map(CompanyExemptionsDocument::getCreated)
                    .ifPresentOrElse((document::setCreated),
                            () -> document.setCreated(new Created().setAt(document.getUpdated().getAt())));

            try {
                repository.save(document);
                logger.info(String.format("Company exemptions for company number: %s updated in MongoDb for context id: %s",
                        companyNumber,
                        contextId));
                ServiceStatus serviceStatus = exemptionsApiService.invokeChsKafkaApi(new ResourceChangedRequest(contextId, companyNumber, null, false));
                logger.info(String.format("ChsKafka api CHANGED invoked updated successfully for context id: %s and company number: %s",
                        contextId,
                        companyNumber));
                return serviceStatus;
            } catch (IllegalArgumentException exp) {
                logger.error("Illegal argument exception caught when processing upsert", exp);
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
        } catch (DataAccessException exp) {
            logger.error("Failed to connect to MongoDb", exp);
            throw new ServiceUnavailableException("Data access exception thrown when calling Mongo Repository");
        }
    }

    private boolean isLatestRecord(String companyNumber, OffsetDateTime deltaAt) {
        String formattedDate = deltaAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"));
        return repository.findUpdatedExemptions(companyNumber, formattedDate).isEmpty();
    }
}
