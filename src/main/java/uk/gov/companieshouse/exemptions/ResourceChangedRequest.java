package uk.gov.companieshouse.exemptions;

import java.util.Objects;

public class ResourceChangedRequest {

    private final String contextId;
    private final String companyNumber;
    private final Object exemptionsData;
    private final Boolean isDelete;

    public ResourceChangedRequest(String contextId, String companyNumber,
                                  Object exemptionsData, Boolean isDelete) {
        this.contextId = contextId;
        this.companyNumber = companyNumber;
        this.exemptionsData = exemptionsData;
        this.isDelete = isDelete;
    }

    public String getContextId() {
        return contextId;
    }

    public String getCompanyNumber() {
        return companyNumber;
    }

    public Object getExemptionsData() {
        return exemptionsData;
    }

    public Boolean getIsDelete() {
        return isDelete;
    }

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
