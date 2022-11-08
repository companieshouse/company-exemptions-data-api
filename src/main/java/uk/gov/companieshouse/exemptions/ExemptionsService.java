package uk.gov.companieshouse.exemptions;

import java.util.Optional;
import uk.gov.companieshouse.api.exemptions.InternalExemptionsApi;

public interface ExemptionsService {
    ServiceStatus upsertCompanyExemptions(String contextId, String companyNumber, InternalExemptionsApi requestBody);
    Optional<CompanyExemptionsDocument> getCompanyExemptions(String companyNumber);
}
