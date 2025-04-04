package uk.gov.companieshouse.exemptions.service;

import uk.gov.companieshouse.api.exemptions.CompanyExemptions;
import uk.gov.companieshouse.api.exemptions.InternalExemptionsApi;

public interface ExemptionsService {
    void upsertCompanyExemptions(String companyNumber, InternalExemptionsApi requestBody);
    CompanyExemptions getCompanyExemptions(String companyNumber);
    void deleteCompanyExemptions(String companyNumber, String deltaAt);
}
