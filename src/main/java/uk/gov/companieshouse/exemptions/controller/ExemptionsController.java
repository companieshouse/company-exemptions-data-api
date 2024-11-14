package uk.gov.companieshouse.exemptions.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions;
import uk.gov.companieshouse.api.exemptions.InternalExemptionsApi;
import uk.gov.companieshouse.exemptions.service.ExemptionsService;
import uk.gov.companieshouse.logging.Logger;

@RestController
public class ExemptionsController {

    @Autowired
    private Logger logger;

    @Autowired
    private ExemptionsService service;

    @PutMapping("/company-exemptions/{company_number}/internal")
    public ResponseEntity<Void> companyExemptionsUpsert(
            @PathVariable("company_number") String companyNumber,
            @RequestHeader("x-request-id") String contextId,
            @RequestBody InternalExemptionsApi requestBody) {
        logger.info(String.format(
                "Processing company exemptions information for company number %s",
                companyNumber));
        service.upsertCompanyExemptions(contextId, companyNumber, requestBody);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/company/{company_number}/exemptions")
    public ResponseEntity<CompanyExemptions> companyExemptionsGet(
            @PathVariable("company_number") String companyNumber) {
        logger.info(String.format("Getting company exemptions for company number %s", companyNumber));
        return ResponseEntity.ok().body(service.getCompanyExemptions(companyNumber));
    }

    @DeleteMapping("/company-exemptions/{company_number}/internal")
    public ResponseEntity<Void> companyExemptionsDelete(
            @PathVariable("company_number") String companyNumber,
            @RequestHeader("x-request-id") String contextId,
            @RequestHeader("X-DELTA-AT") String deltaAt) {
        logger.info(String.format("Deleting company exemptions for company number %s", companyNumber));
        service.deleteCompanyExemptions(contextId, companyNumber, deltaAt);
        return ResponseEntity.ok().build();
    }
}
