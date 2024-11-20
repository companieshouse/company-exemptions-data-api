package uk.gov.companieshouse.exemptions.model;

import java.util.Objects;

public record ResourceChangedRequest(String contextId, String companyNumber, CompanyExemptionsDocument document, Boolean isDelete) {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResourceChangedRequest that = (ResourceChangedRequest) o;
        return Objects.equals(contextId, that.contextId) && Objects.equals(isDelete, that.isDelete)
                && Objects.equals(companyNumber, that.companyNumber) && Objects.equals(document,
                that.document);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contextId, companyNumber, document, isDelete);
    }
}
