package uk.gov.companieshouse.exemptions.controller;

import static uk.gov.companieshouse.exemptions.ExemptionsApplication.APPLICATION_NAME_SPACE;

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
import uk.gov.companieshouse.exemptions.logging.DataMapHolder;
import uk.gov.companieshouse.exemptions.service.ExemptionsService;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@RestController
public class ExemptionsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);

    private final ExemptionsService service;

    public ExemptionsController(ExemptionsService service) {
        this.service = service;
    }

    @PutMapping("/company-exemptions/{company_number}/internal")
    public ResponseEntity<Void> companyExemptionsUpsert(
            @PathVariable("company_number") String companyNumber,
            @RequestBody InternalExemptionsApi requestBody) {

        DataMapHolder.get().companyNumber(companyNumber);
        LOGGER.info("Processing upsert company exemptions", DataMapHolder.getLogMap());

        service.upsertCompanyExemptions(companyNumber, requestBody);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/company/{company_number}/exemptions")
    public ResponseEntity<CompanyExemptions> companyExemptionsGet(
            @PathVariable("company_number") String companyNumber) {

        DataMapHolder.get().companyNumber(companyNumber);
        LOGGER.info("Processing GET company exemptions", DataMapHolder.getLogMap());

        return ResponseEntity.ok().body(service.getCompanyExemptions(companyNumber));
    }

    @DeleteMapping("/company-exemptions/{company_number}/internal")
    public ResponseEntity<Void> companyExemptionsDelete(
            @PathVariable("company_number") String companyNumber,
            @RequestHeader("X-DELTA-AT") String deltaAt) {

        DataMapHolder.get().companyNumber(companyNumber);
        LOGGER.info("Processing DELETE company exemptions", DataMapHolder.getLogMap());

        service.deleteCompanyExemptions(companyNumber, deltaAt);
        return ResponseEntity.ok().build();
    }
}
