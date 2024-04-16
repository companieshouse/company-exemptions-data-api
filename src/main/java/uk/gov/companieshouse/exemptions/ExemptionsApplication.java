package uk.gov.companieshouse.exemptions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExemptionsApplication {

	public static final String APPLICATION_NAME_SPACE = "company-exemptions-data-api";

	public static void main(String[] args) {
		SpringApplication.run(ExemptionsApplication.class, args);
	}

}
