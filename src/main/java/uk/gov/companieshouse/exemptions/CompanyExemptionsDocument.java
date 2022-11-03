package uk.gov.companieshouse.exemptions;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions;
import uk.gov.companieshouse.api.model.exemptions.CompanyExemptionsApi;

import javax.persistence.Id;

@Document(collection = "company_exemptions")
public class CompanyExemptionsDocument {

    @Id
    private String id;

    private Created created;

    private CompanyExemptions data;

    @Field("delta_at")
    private String deltaAt;

    private Updated updated;

    public String getId() {
        return id;
    }

    public CompanyExemptionsDocument setId(String id) {
        this.id = id;
        return this;
    }

    public Created getCreated() {
        return created;
    }

    public CompanyExemptionsDocument setCreated(Created created) {
        this.created = created;
        return this;
    }

    public CompanyExemptions getData() {
        return data;
    }

    public CompanyExemptionsDocument setData(CompanyExemptions data) {
        this.data = data;
        return this;
    }

    public String getDeltaAt() {
        return deltaAt;
    }

    public CompanyExemptionsDocument setDeltaAt(String deltaAt) {
        this.deltaAt = deltaAt;
        return this;
    }

    public Updated getUpdated() {
        return updated;
    }

    public CompanyExemptionsDocument setUpdated(Updated updated) {
        this.updated = updated;
        return this;
    }
}
