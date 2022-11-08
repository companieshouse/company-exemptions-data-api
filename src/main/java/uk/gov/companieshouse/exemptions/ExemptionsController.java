package uk.gov.companieshouse.exemptions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
            @RequestHeader("x-request-id") String contextId,
            @PathVariable("company_number") String companyNumber,
            @RequestBody InternalExemptionsApi requestBody) {
        logger.info(String.format(
                "Processing company exemptions information for company number %s",
                companyNumber));

        ServiceStatus serviceStatus = service.upsertCompanyExemptions(contextId, companyNumber, requestBody);

        if (serviceStatus.equals(ServiceStatus.SERVER_ERROR)) {
            return ResponseEntity.internalServerError().build();
        } else if (serviceStatus.equals(ServiceStatus.CLIENT_ERROR)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } else {
            return ResponseEntity.ok().build();
        }
    }
}
