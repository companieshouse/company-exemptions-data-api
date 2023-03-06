package uk.gov.companieshouse.exemptions;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.exemptions.InternalExemptionsApi;
import uk.gov.companieshouse.logging.Logger;

@Service
public class ExemptionsServiceImpl implements ExemptionsService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS").withZone(ZoneId.of("Z"));

    private final Logger logger;
    private final ExemptionsRepository repository;
    private final ExemptionsMapper mapper;
    private final ExemptionsApiService exemptionsApiService;

    public ExemptionsServiceImpl(Logger logger, ExemptionsRepository repository, ExemptionsMapper mapper, ExemptionsApiService exemptionsApiService) {
        this.logger = logger;
        this.repository = repository;
        this.mapper = mapper;
        this.exemptionsApiService = exemptionsApiService;
    }

    @Override
    public ServiceStatus upsertCompanyExemptions(String contextId, String companyNumber, InternalExemptionsApi requestBody) {
        try {
            Optional<CompanyExemptionsDocument> existingDocument = repository.findById(companyNumber);

            // If the document does not exist OR if the delta_at in the request is after the delta_at on the document
            if (existingDocument.isEmpty() ||
                    StringUtils.isBlank(existingDocument.get().getDeltaAt()) ||
                    requestBody.getInternalData().getDeltaAt()
                            .isAfter(ZonedDateTime.parse(existingDocument.get().getDeltaAt(), FORMATTER)
                                    .toOffsetDateTime())) {
                CompanyExemptionsDocument document = mapper.map(companyNumber, requestBody);

                // If a document already exists and it has a created field, then reuse it
                // otherwise, set it to the delta's updated_at field
                existingDocument.map(CompanyExemptionsDocument::getCreated)
                        .ifPresentOrElse(document::setCreated,
                                () -> document.setCreated(new Created().setAt(document.getUpdated().getAt())));

                ServiceStatus serviceStatus = exemptionsApiService.invokeChsKafkaApi(new ResourceChangedRequest(contextId, companyNumber, null, false));
                logger.info(String.format("ChsKafka api CHANGED invoked updated successfully for context id: %s and company number: %s",
                        contextId,
                        companyNumber));

                if (ServiceStatus.SUCCESS.equals(serviceStatus)) {
                    repository.save(document);
                    logger.info(String.format("Company exemptions for company number: %s updated in MongoDb for context id: %s",
                            companyNumber,
                            contextId));
                }
                return serviceStatus;
            } else {
                logger.info("Company exemptions record not persisted as it is not the latest record.");
                return ServiceStatus.CLIENT_ERROR;
            }
        } catch (IllegalArgumentException ex) {
            logger.error("Illegal argument exception caught when processing upsert", ex);
            return ServiceStatus.SERVER_ERROR;
        } catch (DataAccessException ex) {
            logger.error("Error connecting to MongoDB");
            return ServiceStatus.SERVER_ERROR;
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
            logger.info(String.format("ChsKafka api DELETED invoked successfully for context id: %s and company number: %s", contextId, companyNumber));

            if (ServiceStatus.SUCCESS.equals(serviceStatus)) {
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
}
