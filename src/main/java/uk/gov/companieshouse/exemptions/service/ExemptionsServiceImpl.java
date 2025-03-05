package uk.gov.companieshouse.exemptions.service;

import static uk.gov.companieshouse.exemptions.ExemptionsApplication.APPLICATION_NAME_SPACE;
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
import uk.gov.companieshouse.exemptions.logging.DataMapHolder;
import uk.gov.companieshouse.exemptions.model.CompanyExemptionsDocument;
import uk.gov.companieshouse.exemptions.model.Created;
import uk.gov.companieshouse.exemptions.model.ResourceChangedRequest;
import uk.gov.companieshouse.exemptions.util.ExemptionsMapper;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class ExemptionsServiceImpl implements ExemptionsService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS").withZone(ZoneId.of("Z"));
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);

    private final ExemptionsRepository repository;
    private final ExemptionsMapper mapper;
    private final ExemptionsApiService exemptionsApiService;

    public ExemptionsServiceImpl(ExemptionsRepository repository, ExemptionsMapper mapper, ExemptionsApiService exemptionsApiService) {
        this.repository = repository;
        this.mapper = mapper;
        this.exemptionsApiService = exemptionsApiService;
    }

    @Override
    public void upsertCompanyExemptions(String companyNumber, InternalExemptionsApi requestBody) {
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

                exemptionsApiService.invokeChsKafkaApi(new ResourceChangedRequest(companyNumber, null, false));
            } else {
                throw new ConflictException("Record not persisted as it is not the latest record");
            }
        } catch (IllegalArgumentException ex) {
            LOGGER.error("Illegal argument exception caught when processing upsert", ex, DataMapHolder.getLogMap());
            throw new BadRequestException("Illegal argument exception caught when processing upsert");
        } catch (DataAccessException ex) {
            LOGGER.error("Error connecting to MongoDB", DataMapHolder.getLogMap());
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
            LOGGER.error("Failed to connect to MongoDb", ex, DataMapHolder.getLogMap());
            throw new ServiceUnavailableException("Data access exception thrown when calling Mongo Repository");
        }
    }

    @Override
    public void deleteCompanyExemptions(String companyNumber, String requestDeltaAt) {
        if (StringUtils.isBlank(requestDeltaAt)) {
            throw new BadRequestException("deltaAt missing from delete request");
        }
        try {
            Optional<CompanyExemptionsDocument> document = repository.findById(companyNumber);

            document.ifPresentOrElse(doc -> {
                String existingDeltaAt = doc.getDeltaAt();
                if (isDeltaStale(requestDeltaAt, existingDeltaAt)) {
                    throw new ConflictException(String.format("Stale delta received; request delta_at: [%s] is not after existing delta_at: [%s]",
                            requestDeltaAt, existingDeltaAt));
                }

                repository.deleteById(companyNumber);
                LOGGER.info("Company exemptions deleted in mongoDB successfully", DataMapHolder.getLogMap());
                exemptionsApiService.invokeChsKafkaApiDelete(new ResourceChangedRequest(companyNumber, doc, true));
            }, () -> {
                LOGGER.error("Delete for non-existent document", DataMapHolder.getLogMap());
                exemptionsApiService.invokeChsKafkaApiDelete(new ResourceChangedRequest(companyNumber, new CompanyExemptionsDocument(), true));
            });
        } catch (IllegalArgumentException ex) {
            LOGGER.error("Error calling chs-kafka-api", DataMapHolder.getLogMap());
            throw new BadRequestException(ex.getMessage());
        } catch (DataAccessException ex) {
            LOGGER.error("Error connecting to MongoDB", DataMapHolder.getLogMap());
            throw new ServiceUnavailableException(ex.getMessage());
        }
    }
}
