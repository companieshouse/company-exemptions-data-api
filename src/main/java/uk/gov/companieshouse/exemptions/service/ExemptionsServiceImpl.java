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
    private static final String COMPANY_EXEMPTION_IS_UPDATED_IN_MONGO = "Company exemption is updated in mongo";
    private static final String NOT_PERSISTED_AS_IT_IS_NOT_THE_LATEST_RECORD = "Record not persisted as it is not the latest record";
    private static final String ERROR_CALLING_CHS_KAFKA_API = "Error calling chs-kafka-api";
    private static final String ERROR_CONNECTING_TO_MONGO_DB = "Error connecting to MongoDB";
    private static final String EXEMPTIONS_DOES_NOT_EXIST_FOR_COMPANY = "Exemptions does not exist for company: %s ";
    private static final String DELTA_AT_MISSING_FROM_DELETE_REQUEST = "deltaAt missing from delete request";
    private static final String STALE_DELTA_RECEIVED = "Stale delta received; request delta_at: [%s] is not after existing delta_at: [%s]";
    private static final String COMPANY_EXEMPTIONS_DELETED_SUCCESSFULLY = "Company exemptions deleted in mongoDB successfully";
    private static final String DELETE_FOR_NON_EXISTENT_EXEMPTIONS_DOCUMENT = "Delete for non-existent exemptions document";

    private static final String ERROR_CALLING_CHS_KAFKA_API_MSG = "Error calling chs-kafka-api";
    private static final String ERROR_CONNECTING_TO_MONGO_DB_MSG = "Error connecting to MongoDB";

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
                LOGGER.info(COMPANY_EXEMPTION_IS_UPDATED_IN_MONGO, DataMapHolder.getLogMap());

                exemptionsApiService.invokeChsKafkaApi(new ResourceChangedRequest(companyNumber, null, false));
            } else {
                LOGGER.error(NOT_PERSISTED_AS_IT_IS_NOT_THE_LATEST_RECORD, DataMapHolder.getLogMap());
                throw new ConflictException(NOT_PERSISTED_AS_IT_IS_NOT_THE_LATEST_RECORD);
            }
        } catch (IllegalArgumentException ex) {
            LOGGER.info(ERROR_CALLING_CHS_KAFKA_API, DataMapHolder.getLogMap());
            throw new BadRequestException(ex.getMessage());
        } catch (DataAccessException ex) {
            LOGGER.info(ERROR_CONNECTING_TO_MONGO_DB, DataMapHolder.getLogMap());
            throw new ServiceUnavailableException(ex.getMessage());
        }
    }

    @Override
    public CompanyExemptions getCompanyExemptions(String companyNumber) {
        try {
            return repository.findById(companyNumber).map(CompanyExemptionsDocument::getData)
                    .orElseThrow(() -> new NotFoundException(String.format(
                            EXEMPTIONS_DOES_NOT_EXIST_FOR_COMPANY, companyNumber)));
        } catch (DataAccessException ex) {
            LOGGER.info(ERROR_CONNECTING_TO_MONGO_DB, DataMapHolder.getLogMap());
            throw new ServiceUnavailableException(ex.getMessage());
        }
    }

    @Override
    public void deleteCompanyExemptions(String companyNumber, String requestDeltaAt) {
        if (StringUtils.isBlank(requestDeltaAt)) {
            throw new BadRequestException(DELTA_AT_MISSING_FROM_DELETE_REQUEST);
        }
        try {
            Optional<CompanyExemptionsDocument> document = repository.findById(companyNumber);

            document.ifPresentOrElse(doc -> {
                String existingDeltaAt = doc.getDeltaAt();
                if (isDeltaStale(requestDeltaAt, existingDeltaAt)) {
                    final String msg = String.format(STALE_DELTA_RECEIVED,
                            requestDeltaAt, existingDeltaAt);
                    LOGGER.error(msg, DataMapHolder.getLogMap());
                    throw new ConflictException(msg);
                }

                repository.deleteById(companyNumber);
                LOGGER.info(COMPANY_EXEMPTIONS_DELETED_SUCCESSFULLY, DataMapHolder.getLogMap());
                exemptionsApiService.invokeChsKafkaApiDelete(new ResourceChangedRequest(companyNumber, doc, true));
            }, () -> {
                LOGGER.info(DELETE_FOR_NON_EXISTENT_EXEMPTIONS_DOCUMENT, DataMapHolder.getLogMap());
                exemptionsApiService.invokeChsKafkaApiDelete(new ResourceChangedRequest(companyNumber, new CompanyExemptionsDocument(), true));
            });
        } catch (IllegalArgumentException ex) {
            LOGGER.info(ERROR_CALLING_CHS_KAFKA_API, DataMapHolder.getLogMap());
            throw new BadRequestException(ex.getMessage());
        } catch (DataAccessException ex) {
            LOGGER.info(ERROR_CONNECTING_TO_MONGO_DB, DataMapHolder.getLogMap());
            throw new ServiceUnavailableException(ex.getMessage());
        }
    }
}