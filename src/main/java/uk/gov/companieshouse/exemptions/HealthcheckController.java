package uk.gov.companieshouse.exemptions;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HealthcheckController {
    @GetMapping("/healthcheck")
    public ResponseEntity<Void> healthcheck() {
            return ResponseEntity.ok().build();
    }
}
