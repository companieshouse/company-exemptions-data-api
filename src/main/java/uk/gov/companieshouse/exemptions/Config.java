package uk.gov.companieshouse.exemptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.time.OffsetDateTime;
import java.util.function.Supplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.time.LocalDate;

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

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDate.class, new LocalDateSerializer());
        module.addDeserializer(LocalDate.class, new LocalDateDeSerializer());
        mapper.registerModule(module);
        return mapper;
    }
}

