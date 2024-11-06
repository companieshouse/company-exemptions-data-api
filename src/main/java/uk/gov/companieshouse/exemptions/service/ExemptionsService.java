package uk.gov.companieshouse.exemptions.service;

import java.util.Optional;
import uk.gov.companieshouse.api.exemptions.InternalExemptionsApi;
import uk.gov.companieshouse.exemptions.model.CompanyExemptionsDocument;
import uk.gov.companieshouse.exemptions.model.ServiceStatus;

public interface ExemptionsService {
    ServiceStatus upsertCompanyExemptions(String contextId, String companyNumber, InternalExemptionsApi requestBody);
    Optional<CompanyExemptionsDocument> getCompanyExemptions(String companyNumber);
    ServiceStatus deleteCompanyExemptions(String contextId, String companyNumber, String deltaAt);
}
