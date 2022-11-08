package uk.gov.companieshouse.exemptions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.companieshouse.api.exemptions.InternalExemptionsApi;
import uk.gov.companieshouse.logging.Logger;

import java.util.Optional;

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

    @GetMapping("/company/{company_number}/exemption")
    public ResponseEntity<CompanyExemptionsDocument> companyExemptionsGet(
            @RequestHeader("x-request-id") String contextId,
            @PathVariable("company_number") String companyNumber) {
        logger.info(String.format("Getting company exemptions for company number %s", companyNumber));

        Optional<CompanyExemptionsDocument> document = service.getCompanyExemptions(companyNumber);

        return document.map(companyExemptionsDocument -> ResponseEntity.ok().body(companyExemptionsDocument))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
