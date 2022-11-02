package uk.gov.companieshouse.exemptions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.exemptions.InternalExemptionsApi;
import uk.gov.companieshouse.logging.Logger;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
public class ExemptionsService {

    @Autowired
    private Logger logger;

    @Autowired
    private ExemptionsRepository repository;

    @Autowired
    private ExemptionsMapper mapper;

    @Autowired
    private ExemptionsApiService exemptionsApiService;

    public ServiceStatus upsertCompanyExemptions(String contextId, String companyNumber, InternalExemptionsApi requestBody) {

        if (isLatestRecord(companyNumber, requestBody.getInternalData().getDeltaAt())) {
            CompanyExemptionsDocument document = mapper.map(companyNumber, requestBody);
            saveAndCallChsKafka(contextId, companyNumber, document);
            return ServiceStatus.SUCCESS;
        } else {
            logger.info("Company exemptions record not persisted as it is not the latest record.");
            return ServiceStatus.CLIENT_ERROR;
        }
    }

    private boolean isLatestRecord(String officerId, OffsetDateTime deltaAt) {
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        String formattedDate = deltaAt.format(dateTimeFormatter);

        return repository.findUpdatedExemptions(officerId, formattedDate).isEmpty();
    }

    private void saveAndCallChsKafka(String contextId, String companyNumber,
                                     CompanyExemptionsDocument document) {
        boolean savedToDb = false;
        Created created = getCreatedFromCurrentRecord(companyNumber);
        if(created == null) {
            document.setCreated(new Created().setAt(document.getUpdated().getAt()));
        } else {
            document.setCreated(created);
        }

        try {
            repository.save(document);
            savedToDb = true;
            logger.info(String.format("Company exemptions for company number: %s updated in MongoDb for context id: %s",
                    companyNumber,
                    contextId));
        } catch (IllegalArgumentException illegalArgumentEx) {
            throw new BadRequestException(illegalArgumentEx.getMessage());
        }

        if (savedToDb) {
            exemptionsApiService.invokeChsKafkaApi(
                    new ResourceChangedRequest(contextId, companyNumber, null, false));
            logger.info(String.format("ChsKafka api CHANGED invoked updated successfully for context id: %s and company number: %s",
                    contextId,
                    companyNumber));
        }
    }

    private Created getCreatedFromCurrentRecord(String companyNumber) {
        Optional<CompanyExemptionsDocument> doc = repository.findById(companyNumber);

        return doc.isPresent() ? doc.get().getCreated(): null;
    }
}
