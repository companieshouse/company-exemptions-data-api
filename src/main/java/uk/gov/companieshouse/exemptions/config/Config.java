package uk.gov.companieshouse.exemptions.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;
import uk.gov.companieshouse.exemptions.util.ExemptionsReadConverter;
import uk.gov.companieshouse.exemptions.util.ExemptionsWriteConverter;
import uk.gov.companieshouse.exemptions.util.LocalDateDeSerializer;
import uk.gov.companieshouse.exemptions.util.LocalDateSerializer;

@Configuration
public class Config {

    @Bean
    public Supplier<Instant> instantSupplier() {
        return Instant::now;
    }

    @Bean
    public Supplier<InternalApiClient> internalApiClientSupplier(
            @Value("${chs.kafka.api.key}") String apiKey,
            @Value("${chs.kafka.api.endpoint}") String apiUrl) {
        return () -> {
            InternalApiClient internalApiClient = new InternalApiClient(new ApiKeyHttpClient(apiKey));
            internalApiClient.setBasePath(apiUrl);
            return internalApiClient;
        };
    }

    /**
     * mongoCustomConversions.
     *
     * @return MongoCustomConversions.
     */
    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        ObjectMapper objectMapper = mongoDbObjectMapper();
        return new MongoCustomConversions(List.of(new ExemptionsWriteConverter(objectMapper), new ExemptionsReadConverter(objectMapper)));
    }

    /**
     * Mongo DB Object Mapper.
     *
     * @return ObjectMapper.
     */
    private ObjectMapper mongoDbObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        SimpleModule module = new SimpleModule();
        module.addSerializer(LocalDate.class, new LocalDateSerializer());
        module.addDeserializer(LocalDate.class, new LocalDateDeSerializer());
        objectMapper.registerModule(module);
        return objectMapper;
    }
}

