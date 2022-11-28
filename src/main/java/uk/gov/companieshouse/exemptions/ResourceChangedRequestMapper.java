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
        ChangedResourceEvent event = new ChangedResourceEvent().publishedAt(this.timestampGenerator.get());
        ChangedResource changedResource = new ChangedResource()
                .resourceUri(String.format("company/%s/exemptions", request.getCompanyNumber()))
                .resourceKind("company-exemptions")
                .event(event)
                .contextId(request.getContextId());

        if (request.getIsDelete()) {
            event.setType("deleted");
            changedResource.setDeletedData(request.getExemptionsData());
        } else {
            event.setType("changed");
        }
        return changedResource;
    }
}
