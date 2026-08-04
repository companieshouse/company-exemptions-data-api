package uk.gov.companieshouse.exemptions.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import uk.gov.companieshouse.api.InternalApiClient;

import java.time.Instant;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Config.class)
class ConfigTest {

    @Autowired
    private Supplier<Instant> instantSupplier;

    @Autowired
    private Supplier<InternalApiClient> apiClientSupplier;

    @Autowired
    private MongoCustomConversions mongoCustomConversions;

    @Test
    void whenContextLoads_thenBeansCorrectlyGenerated() {
        assertThat(instantSupplier).isNotNull();
        assertThat(apiClientSupplier).isNotNull();
        assertThat(mongoCustomConversions).isNotNull();
    }
}