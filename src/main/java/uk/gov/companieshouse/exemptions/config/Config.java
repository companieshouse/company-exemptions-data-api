package uk.gov.companieshouse.exemptions.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
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
            final var internalApiClient = new InternalApiClient(new ApiKeyHttpClient(apiKey));
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
        final var mapper = mongoDbJsonMapper();
        return new MongoCustomConversions(
                List.of(
                        new ExemptionsWriteConverter(mapper),
                        new ExemptionsReadConverter(mapper)));
    }

    /**
     * Returns a configured Mongo DB Json Mapper, disabling two FAIL ON criteria and
     * registering LocalDate (de)serializers that map to and from Mongo ISODate values.
     *
     * @return JsonMapper.
     */
    private JsonMapper mongoDbJsonMapper() {
        final var localDateModule = new SimpleModule()
                .addSerializer(LocalDate.class, new LocalDateSerializer())
                .addDeserializer(LocalDate.class, new LocalDateDeSerializer());
        return JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .changeDefaultPropertyInclusion(inclusion -> inclusion.withValueInclusion(JsonInclude.Include.NON_EMPTY))
                .addModule(localDateModule)
                .build();
    }
}