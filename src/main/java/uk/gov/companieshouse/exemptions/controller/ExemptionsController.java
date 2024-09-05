package uk.gov.companieshouse.exemptions.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import uk.gov.companieshouse.exemptions.model.CompanyExemptionsDocument;
import uk.gov.companieshouse.exemptions.service.ExemptionsService;
import uk.gov.companieshouse.exemptions.model.ServiceStatus;
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
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } else if (serviceStatus.equals(ServiceStatus.CLIENT_ERROR)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }else {
            return ResponseEntity.ok().build();
        }
    }

    @GetMapping("/company/{company_number}/exemptions")
    public ResponseEntity<CompanyExemptions> companyExemptionsGet(
            @RequestHeader("x-request-id") String contextId,
            @PathVariable("company_number") String companyNumber) {
        logger.info(String.format("KCP - Getting company exemptions for company number %s", companyNumber));

        Optional<CompanyExemptionsDocument> document = service.getCompanyExemptions(companyNumber);

        return document.map(companyExemptionsDocument -> ResponseEntity.ok().body(companyExemptionsDocument.getData()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/company-exemptions/{company_number}/internal")
    public ResponseEntity<CompanyExemptionsDocument> companyExemptionsDelete(
            @RequestHeader("x-request-id") String contextId,
            @PathVariable("company_number") String companyNumber) {
        logger.info(String.format("Deleting company exemptions for company number %s", companyNumber));

        ServiceStatus serviceStatus = service.deleteCompanyExemptions(contextId, companyNumber);

        if (serviceStatus.equals(ServiceStatus.SERVER_ERROR)) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        } else if (serviceStatus.equals(ServiceStatus.CLIENT_ERROR)) {
            return ResponseEntity.notFound().build();
        } else {
            return ResponseEntity.ok().build();
        }
    }
}
