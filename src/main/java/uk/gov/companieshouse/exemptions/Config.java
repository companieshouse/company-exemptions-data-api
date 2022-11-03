package uk.gov.companieshouse.exemptions;

import java.time.OffsetDateTime;
import java.util.function.Supplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import static uk.gov.companieshouse.exemptions.ExemptionsApplication.NAMESPACE;

@Configuration
public class Config {

    @Bean
    public Logger logger() {
        return LoggerFactory.getLogger(NAMESPACE);
    }

    @Bean
    public Supplier<String> offsetDateTimeGenerator() {
        return () -> String.valueOf(OffsetDateTime.now());
    }
}

