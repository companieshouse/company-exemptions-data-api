package uk.gov.companieshouse.exemptions.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HealthcheckController {
    @GetMapping("company-exemptions/healthcheck")
    public ResponseEntity<Void> healthcheck() {
            return ResponseEntity.ok().build();
    }
}
