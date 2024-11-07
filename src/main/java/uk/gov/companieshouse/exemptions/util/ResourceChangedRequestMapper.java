package uk.gov.companieshouse.exemptions.util;

import java.time.Instant;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.chskafka.ChangedResource;
import uk.gov.companieshouse.api.chskafka.ChangedResourceEvent;
import uk.gov.companieshouse.exemptions.model.CompanyExemptionsDocument;
import uk.gov.companieshouse.exemptions.model.ResourceChangedRequest;

@Component
public class ResourceChangedRequestMapper {

    private final Supplier<Instant> instantSupplier;

    public ResourceChangedRequestMapper(Supplier<Instant> instantSupplier) {
        this.instantSupplier = instantSupplier;
    }

    public ChangedResource mapChangedResource(ResourceChangedRequest request) {
        boolean isDelete = request.isDelete();
        ChangedResourceEvent event = new ChangedResourceEvent()
                .publishedAt(DateUtils.publishedAtString(this.instantSupplier.get()))
                .type(isDelete ? "deleted" : "changed");
        ChangedResource changedResource = new ChangedResource()
                .resourceUri(String.format("company/%s/exemptions", request.companyNumber()))
                .resourceKind("company-exemptions")
                .event(event)
                .contextId(request.contextId());

        if (isDelete && request.document().isPresent()) {
            CompanyExemptionsDocument exemptionsDocument = request.document().get();
            changedResource.setDeletedData(exemptionsDocument.getData());
        }
        return changedResource;
    }
}
