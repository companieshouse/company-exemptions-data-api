package uk.gov.companieshouse.exemptions.service;

import static uk.gov.companieshouse.exemptions.util.DateUtils.isDeltaStale;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions;
import uk.gov.companieshouse.api.exemptions.InternalExemptionsApi;
import uk.gov.companieshouse.exemptions.exception.BadRequestException;
import uk.gov.companieshouse.exemptions.exception.ConflictException;
import uk.gov.companieshouse.exemptions.exception.NotFoundException;
import uk.gov.companieshouse.exemptions.exception.ServiceUnavailableException;
import uk.gov.companieshouse.exemptions.model.CompanyExemptionsDocument;
import uk.gov.companieshouse.exemptions.model.Created;
import uk.gov.companieshouse.exemptions.model.ResourceChangedRequest;
import uk.gov.companieshouse.exemptions.util.ExemptionsMapper;
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
    public void upsertCompanyExemptions(String contextId, String companyNumber, InternalExemptionsApi requestBody) {
        try {
            Optional<CompanyExemptionsDocument> existingDocument = repository.findById(companyNumber);

            // If the document does not exist OR if the delta_at in the request is not before the delta_at on the document
            if (existingDocument.isEmpty() ||
                    StringUtils.isBlank(existingDocument.get().getDeltaAt()) ||
                    !requestBody.getInternalData().getDeltaAt()
                            .isBefore(ZonedDateTime.parse(existingDocument.get().getDeltaAt(), FORMATTER)
                                    .toOffsetDateTime())) {
                CompanyExemptionsDocument document = mapper.map(companyNumber, requestBody);

                // If a document already exists and it has a created field, then reuse it
                // otherwise, set it to the delta's updated_at field
                existingDocument.map(CompanyExemptionsDocument::getCreated)
                        .ifPresentOrElse(document::setCreated,
                                () -> document.setCreated(new Created().setAt(document.getUpdated().at())));

                repository.save(document);
                logger.info(String.format("Company exemptions for company number: %s updated in MongoDb for context id: %s",
                        companyNumber,
                        contextId));

                exemptionsApiService.invokeChsKafkaApi(new ResourceChangedRequest(contextId, companyNumber, null, false));
                logger.info(String.format("ChsKafka api CHANGED invoked SUCCESSFULLY for context id: %s and company number: %s",
                        contextId, companyNumber));
            } else {
                logger.info(String.format("Record for company %s not persisted as it is not the latest record.", companyNumber));
                throw new ConflictException("Record not persisted as it is not the latest record");
            }
        } catch (IllegalArgumentException ex) {
            logger.error("Illegal argument exception caught when processing upsert", ex);
            throw new BadRequestException("Illegal argument exception caught when processing upsert");
        } catch (DataAccessException ex) {
            logger.error("Error connecting to MongoDB");
            throw new ServiceUnavailableException("Error connecting to MongoDB");
        }
    }

    @Override
    public CompanyExemptions getCompanyExemptions(String companyNumber) {
        try {
            return repository.findById(companyNumber).map(CompanyExemptionsDocument::getData)
                    .orElseThrow(() -> new NotFoundException(String.format(
                            "Exemptions does not exist for company: %s ", companyNumber)));
        } catch (DataAccessException ex) {
            logger.error("Failed to connect to MongoDb", ex);
            throw new ServiceUnavailableException("Data access exception thrown when calling Mongo Repository");
        }
    }

    @Override
    public void deleteCompanyExemptions(String contextId, String companyNumber, String requestDeltaAt) {
        if (StringUtils.isBlank(requestDeltaAt)) {
            logger.error("deltaAt missing from delete request");
            throw new BadRequestException("deltaAt missing from delete request");
        }
        try {
            Optional<CompanyExemptionsDocument> document = repository.findById(companyNumber);
            if (document.isPresent()) {
                String existingDeltaAt = document.get().getDeltaAt();
                if (isDeltaStale(requestDeltaAt, existingDeltaAt)) {
                    logger.error(String.format("Stale delta received; request delta_at: [%s] is not after existing delta_at: [%s]",
                            requestDeltaAt, existingDeltaAt));
                    throw new ConflictException("Stale delta received.");
                }

                repository.deleteById(companyNumber);
                logger.info(String.format("Company exemptions for company number: %s deleted in MongoDb for context id: %s", companyNumber, contextId));
            } else {
                logger.error(String.format("Company exemptions do not exist for company number %s", companyNumber));
            }

            exemptionsApiService.invokeChsKafkaApi(new ResourceChangedRequest(contextId, companyNumber, document, true));
            logger.info(String.format("ChsKafka api DELETED invoked successfully for context id: %s and company number: %s", contextId, companyNumber));
        } catch (IllegalArgumentException ex) {
            logger.error("Error calling chs-kafka-api");
            throw new BadRequestException("Illegal argument exception caught when processing delete");
        } catch (DataAccessException ex) {
            logger.error("Error connecting to MongoDB");
            throw new ServiceUnavailableException("Error connecting to MongoDB");
        }
    }
}
