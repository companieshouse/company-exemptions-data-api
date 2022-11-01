package uk.gov.companieshouse.exemptions;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.exemptions.InternalExemptionsApi;

@Service
public class ExemptionsService {

    public static final String APPLICATION_NAMESPACE = "company-exemptions-data-api";

    // private final CompanyExemptionsRepository repository;

    public void upsertCompanyExemptions(String contextId, String companyNumber, InternalExemptionsApi requestBody) {

    }
}
