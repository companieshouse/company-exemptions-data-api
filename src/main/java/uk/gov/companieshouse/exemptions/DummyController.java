package uk.gov.companieshouse.exemptions;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@Controller
public class DummyController {

    @PutMapping("/company-exemptions/{company_number}/internal")
    public ResponseEntity<Void> dummyPut(
            @RequestHeader("x-request-id") String contextId,
            @PathVariable("company_number") String companyNumber,
            @RequestHeader("simulate") int simulate) {
        if (simulate == 400) {
            return ResponseEntity.badRequest().build();
        } else if (simulate == 500) {
            return ResponseEntity.internalServerError().build();
        } else {
            return ResponseEntity.ok().build();
        }
    }
}
