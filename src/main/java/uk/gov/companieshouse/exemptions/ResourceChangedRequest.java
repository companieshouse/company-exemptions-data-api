package uk.gov.companieshouse.exemptions;

import java.util.Objects;

public record ResourceChangedRequest(String contextId, String companyNumber, Object exemptionsData, Boolean isDelete) {
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResourceChangedRequest that = (ResourceChangedRequest) o;
        return Objects.equals(contextId, that.contextId) &&
                Objects.equals(companyNumber, that.companyNumber) &&
                Objects.equals(exemptionsData, that.exemptionsData) &&
                Objects.equals(isDelete, that.isDelete);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contextId, companyNumber, exemptionsData, isDelete);
    }
}
