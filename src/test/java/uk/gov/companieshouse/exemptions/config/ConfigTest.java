package uk.gov.companieshouse.exemptions.config;

import com.mongodb.BasicDBObject;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.chskafka.ChangedResource;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions;
import uk.gov.companieshouse.api.exemptions.ExemptionItem;
import uk.gov.companieshouse.api.exemptions.Exemptions;
import uk.gov.companieshouse.api.exemptions.PscExemptAsSharesAdmittedOnMarketItem;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = Config.class)
class ConfigTest {

    private static final LocalDate EXEMPT_FROM = LocalDate.of(2017, 12, 19);

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

    @Test
    void internalApiClientCanBeCreated() {
        assertThat(apiClientSupplier.get()).isNotNull();
    }

    @Test
    void internalApiClientHasRuntimeDependenciesToPostChangedResource() {
        final var internalApiClient = apiClientSupplier.get();
        internalApiClient.setBasePath("http://localhost:1");
        final var changedResourcePost = internalApiClient.privateChangedResourceHandler()
                .postChangedResource("/private/resource-changed", new ChangedResource());

        // Nothing listens on port 1, so an Exception is expected; a missing SDK runtime dependency surfaces as an Error.
        assertThatThrownBy(changedResourcePost::execute).isInstanceOf(Exception.class);
    }

    @Test
    void readConverterDeserialisesMongoIsoDateIntoLocalDate() {
        final var stored = new Document("exemptions", new Document("psc_exempt_as_shares_admitted_on_market",
                new Document("items", List.of(new Document("exempt_from",
                        Date.from(Instant.parse("2017-12-19T00:00:00Z")))))));

        final var actual = conversionService().convert(stored, CompanyExemptions.class);

        assertThat(actual.getExemptions().getPscExemptAsSharesAdmittedOnMarket().getItems().getFirst().getExemptFrom())
                .isEqualTo(EXEMPT_FROM);
    }

    @Test
    void writeConverterStoresLocalDateAsMongoIsoDate() {
        final var exemptions = new CompanyExemptions().exemptions(new Exemptions()
                .pscExemptAsSharesAdmittedOnMarket(new PscExemptAsSharesAdmittedOnMarketItem()
                        .items(List.of(new ExemptionItem().exemptFrom(EXEMPT_FROM)))));

        final var written = conversionService().convert(exemptions, BasicDBObject.class);

        final var item = (BasicDBObject) ((List<?>) ((BasicDBObject) ((BasicDBObject) written.get("exemptions"))
                .get("psc_exempt_as_shares_admitted_on_market")).get("items")).getFirst();
        assertThat(item).containsEntry("exempt_from", Date.from(Instant.parse("2017-12-19T00:00:00Z")));
    }

    private DefaultConversionService conversionService() {
        final var conversionService = new DefaultConversionService();
        mongoCustomConversions.registerConvertersIn(conversionService);
        return conversionService;
    }
}
