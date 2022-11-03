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
        return new ChangedResource()
                .resourceUri(String.format("company/%s/exemptions", request.getCompanyNumber()))
                .resourceKind("company-exemptions")
                .event(new ChangedResourceEvent()
                        .type("changed")
                        .publishedAt(this.timestampGenerator.get()))
                .contextId(request.getContextId());
    }
}
