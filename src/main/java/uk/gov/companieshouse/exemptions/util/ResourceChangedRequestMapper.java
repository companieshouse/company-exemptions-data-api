package uk.gov.companieshouse.exemptions.util;

import java.time.Instant;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.chskafka.ChangedResource;
import uk.gov.companieshouse.api.chskafka.ChangedResourceEvent;
import uk.gov.companieshouse.exemptions.model.ResourceChangedRequest;

@Component
public class ResourceChangedRequestMapper {

    private static final String CHANGED = "changed";
    private static final String DELETED = "deleted";

    private final Supplier<Instant> instantSupplier;

    public ResourceChangedRequestMapper(Supplier<Instant> instantSupplier) {
        this.instantSupplier = instantSupplier;
    }

    public ChangedResource mapChangedEvent(ResourceChangedRequest request) {
        return buildChangedResource(CHANGED, request);
    }

    public ChangedResource mapDeletedEvent(ResourceChangedRequest request) {
        ChangedResource changedResource = buildChangedResource(DELETED, request);
        changedResource.setDeletedData(request.document().getData());
        return changedResource;
    }

    private ChangedResource buildChangedResource(final String type, ResourceChangedRequest request){
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
