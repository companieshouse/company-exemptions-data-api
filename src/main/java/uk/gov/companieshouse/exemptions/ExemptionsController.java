package uk.gov.companieshouse.exemptions;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.exemptions.InternalExemptionsApi;
import uk.gov.companieshouse.logging.Logger;

@RestController
public class ExemptionsController {

    @Autowired
    private Logger logger;

    @Autowired
    private ExemptionsService service;

    @PutMapping("/company-exemptions/{company_number}/internal")
    public ResponseEntity<Void> companyExemptionsUpsert(
            @RequestHeader("x-request-header") String contextId,
            @PathVariable("company_number") String companyNumber,
            @RequestBody InternalExemptionsApi requestBody) throws JsonProcessingException {
        logger.info(String.format(
                "Processing company exemptions information for company number %s",
                companyNumber));

        service.upsertCompanyExemptions(contextId, companyNumber, requestBody);

        return ResponseEntity.ok().build();
    }
}
