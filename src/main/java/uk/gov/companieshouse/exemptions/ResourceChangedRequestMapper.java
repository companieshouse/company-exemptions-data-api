package uk.gov.companieshouse.exemptions;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.chskafka.ChangedResource;
import uk.gov.companieshouse.api.chskafka.ChangedResourceEvent;

@Component
public class ResourceChangedRequestMapper {

    private final Supplier<String> timestampGenerator;

    public ResourceChangedRequestMapper(Supplier<String> timestampGenerator) {
        this.timestampGenerator = timestampGenerator;
    }

    public ChangedResource mapChangedResource(ResourceChangedRequest request) {
        ChangedResource changedResource = new ChangedResource();

        changedResource.setResourceUri(String.format("company/%s/exemptions", request.getCompanyNumber()));
        changedResource.setResourceKind("company-exemptions");

        ChangedResourceEvent event = new ChangedResourceEvent();
        if (request.getIsDelete()) {
            event.setType("deleted");
            changedResource.setDeletedData(request.getExemptionsData());
        } else {
            event.setType("changed");
        }
        event.publishedAt(this.timestampGenerator.get());

        changedResource.event(event);
        changedResource.setContextId(request.getContextId());

        return changedResource;
    }
}
