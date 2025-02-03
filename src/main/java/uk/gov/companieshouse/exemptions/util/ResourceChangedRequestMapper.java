package uk.gov.companieshouse.exemptions.util;

import static uk.gov.companieshouse.exemptions.ExemptionsApplication.APPLICATION_NAME_SPACE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.chskafka.ChangedResource;
import uk.gov.companieshouse.api.chskafka.ChangedResourceEvent;
import uk.gov.companieshouse.exemptions.exception.SerDesException;
import uk.gov.companieshouse.exemptions.model.ResourceChangedRequest;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
public class ResourceChangedRequestMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);
    private static final String CHANGED = "changed";
    private static final String DELETED = "deleted";

    private final Supplier<Instant> instantSupplier;
    private final ObjectMapper objectMapper;

    public ResourceChangedRequestMapper(Supplier<Instant> instantSupplier, ObjectMapper objectMapper) {
        this.instantSupplier = instantSupplier;
        this.objectMapper = objectMapper;
    }

    public ChangedResource mapChangedEvent(ResourceChangedRequest request) {
        return buildChangedResource(CHANGED, request);
    }

    public ChangedResource mapDeletedEvent(ResourceChangedRequest request) {
        ChangedResource changedResource = buildChangedResource(DELETED, request);
        try {
            // Removes null fields using Jackson's Object Mapper
            Object data = objectMapper.readValue(objectMapper.writeValueAsString(request.document().getData()), Object.class);
            changedResource.setDeletedData(data);

            return changedResource;
        } catch (JsonProcessingException ex) {
            final String msg = "Failed to serialise/deserialise deleted data";
            LOGGER.error(msg);
            throw new SerDesException(msg, ex);
        }
    }

    private ChangedResource buildChangedResource(final String type, ResourceChangedRequest request) {
        ChangedResourceEvent event = new ChangedResourceEvent()
                .publishedAt(DateUtils.publishedAtString(this.instantSupplier.get()))
                .type(type);
        return new ChangedResource()
                .resourceUri(String.format("company/%s/exemptions", request.companyNumber()))
                .resourceKind("company-exemptions")
                .event(event)
                .contextId(request.contextId());
    }
}
