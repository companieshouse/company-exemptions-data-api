package uk.gov.companieshouse.exemptions.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.chskafka.ChangedResource;
import uk.gov.companieshouse.api.chskafka.ChangedResourceEvent;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions;
import uk.gov.companieshouse.api.exemptions.ExemptionItem;
import uk.gov.companieshouse.api.exemptions.Exemptions;
import uk.gov.companieshouse.api.exemptions.PscExemptAsTradingOnRegulatedMarketItem;
import uk.gov.companieshouse.api.exemptions.PscExemptAsTradingOnRegulatedMarketItem.ExemptionTypeEnum;
import uk.gov.companieshouse.exemptions.exception.SerDesException;
import uk.gov.companieshouse.exemptions.model.CompanyExemptionsDocument;
import uk.gov.companieshouse.exemptions.model.ResourceChangedRequest;

@ExtendWith(MockitoExtension.class)
class ResourceChangedRequestMapperTest {

    private static final String EXPECTED_CONTEXT_ID = "uninitialised";
    private static final Instant UPDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    private static final String PUBLISHED_AT = DateUtils.publishedAtString(UPDATED_AT);

    @InjectMocks
    private ResourceChangedRequestMapper mapper;

    @Mock
    private Supplier<Instant> instantSupplier;
    @Mock
    private ObjectMapper objectMapper;

    @Test
    void shouldMapChangedEvent() {
        // given
        ResourceChangedTestArgument argument = ResourceChangedTestArgument.builder()
                .withRequest(new ResourceChangedRequest("12345678", null, false))
                .withContextId(EXPECTED_CONTEXT_ID)
                .withResourceUri("company/12345678/exemptions")
                .withResourceKind("company-exemptions")
                .withEventType("changed")
                .withEventPublishedAt(PUBLISHED_AT)
                .build();
        when(instantSupplier.get()).thenReturn(UPDATED_AT);

        // when
        ChangedResource actual = mapper.mapChangedEvent(argument.request());

        // then
        assertEquals(argument.changedResource(), actual);
    }

    @ParameterizedTest
    @MethodSource("resourceChangedScenarios")
    void shouldMapDeletedEvent(ResourceChangedTestArgument argument) throws Exception {
        // given
        when(instantSupplier.get()).thenReturn(UPDATED_AT);
        when(objectMapper.writeValueAsString(any())).thenReturn("Data as string");
        when(objectMapper.readValue(anyString(), eq(Object.class))).thenReturn(argument.changedResource().getDeletedData());

        // when
        ChangedResource actual = mapper.mapDeletedEvent(argument.request());

        // then
        assertEquals(argument.changedResource(), actual);
        verify(objectMapper).writeValueAsString(argument.request().document().getData());
        verify(objectMapper).readValue("Data as string", Object.class);
    }

    @Test
    void shouldThrowSerDesExceptionWhenJsonProcessingExceptionCaughtDuringSerialisation() throws Exception {
        // given
        when(instantSupplier.get()).thenReturn(UPDATED_AT);
        when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);

        // when
        Executable ex = () -> mapper.mapDeletedEvent(
                new ResourceChangedRequest("12345678", getCompanyExemptionsDocument(), true));

        // then
        assertThrows(SerDesException.class, ex);
    }

    @Test
    void shouldThrowSerDesExceptionWhenJsonProcessingExceptionCaughtDuringDeserialisation() throws Exception {
        // given
        when(instantSupplier.get()).thenReturn(UPDATED_AT);
        when(objectMapper.writeValueAsString(any())).thenReturn("Data as string");
        when(objectMapper.readValue(anyString(), eq(Object.class))).thenThrow(JsonProcessingException.class);

        // when
        Executable ex = () -> mapper.mapDeletedEvent(
                new ResourceChangedRequest("12345678", getCompanyExemptionsDocument(), true));

        // then
        assertThrows(SerDesException.class, ex);
    }

    static Stream<ResourceChangedTestArgument> resourceChangedScenarios() {
        return Stream.of(
                ResourceChangedTestArgument.builder()
                        .withRequest(new ResourceChangedRequest("12345678", new CompanyExemptionsDocument(), true))
                        .withContextId(EXPECTED_CONTEXT_ID)
                        .withResourceUri("company/12345678/exemptions")
                        .withResourceKind("company-exemptions")
                        .withEventType("deleted")
                        .withEventPublishedAt(PUBLISHED_AT)
                        .build(),
                ResourceChangedTestArgument.builder()
                        .withRequest(new ResourceChangedRequest("12345678", getCompanyExemptionsDocument(), true))
                        .withContextId(EXPECTED_CONTEXT_ID)
                        .withResourceUri("company/12345678/exemptions")
                        .withResourceKind("company-exemptions")
                        .withEventType("deleted")
                        .withDeletedData(getExemptionsData())
                        .withEventPublishedAt(PUBLISHED_AT)
                        .build()
        );
    }

    record ResourceChangedTestArgument(ResourceChangedRequest request, ChangedResource changedResource) {

        public static ResourceChangedTestArgumentBuilder builder() {
            return new ResourceChangedTestArgumentBuilder();
        }

        @Override
        public String toString() {
            return this.request.toString();
        }
    }

    static class ResourceChangedTestArgumentBuilder {

        private ResourceChangedRequest request;
        private String resourceUri;
        private String resourceKind;
        private String contextId;
        private String eventType;
        private String eventPublishedAt;
        private Object deletedData;

        public ResourceChangedTestArgumentBuilder withRequest(ResourceChangedRequest request) {
            this.request = request;
            return this;
        }

        public ResourceChangedTestArgumentBuilder withResourceUri(String resourceUri) {
            this.resourceUri = resourceUri;
            return this;
        }

        public ResourceChangedTestArgumentBuilder withResourceKind(String resourceKind) {
            this.resourceKind = resourceKind;
            return this;
        }

        public ResourceChangedTestArgumentBuilder withContextId(String contextId) {
            this.contextId = contextId;
            return this;
        }

        public ResourceChangedTestArgumentBuilder withEventType(String eventType) {
            this.eventType = eventType;
            return this;
        }

        public ResourceChangedTestArgumentBuilder withEventPublishedAt(String eventPublishedAt) {
            this.eventPublishedAt = eventPublishedAt;
            return this;
        }

        public ResourceChangedTestArgumentBuilder withDeletedData(Object deletedData) {
            this.deletedData = deletedData;
            return this;
        }

        public ResourceChangedTestArgument build() {
            ChangedResource changedResource = new ChangedResource();
            changedResource.setResourceUri(this.resourceUri);
            changedResource.setResourceKind(this.resourceKind);
            changedResource.setContextId(this.contextId);
            ChangedResourceEvent event = new ChangedResourceEvent();
            event.setType(this.eventType);
            event.setPublishedAt(this.eventPublishedAt);
            changedResource.setEvent(event);
            changedResource.setDeletedData(deletedData);
            return new ResourceChangedTestArgument(this.request, changedResource);
        }
    }

    private static CompanyExemptionsDocument getCompanyExemptionsDocument() {
        CompanyExemptionsDocument document = new CompanyExemptionsDocument();
        document.setId("00006400");
        document.setDeltaAt("20221012091025774312");
        document.setData(getExemptionsData());
        return document;
    }

    private static CompanyExemptions getExemptionsData() {
        CompanyExemptions exemptionsData = new CompanyExemptions();
        Exemptions exemptions = new Exemptions();
        ExemptionItem exemptionItem = new ExemptionItem(LocalDate.of(2022, 1, 1));
        PscExemptAsTradingOnRegulatedMarketItem regulatedMarketItem =
                new PscExemptAsTradingOnRegulatedMarketItem(Collections.singletonList(exemptionItem),
                        ExemptionTypeEnum.PSC_EXEMPT_AS_TRADING_ON_REGULATED_MARKET);
        exemptions.setPscExemptAsTradingOnRegulatedMarket(regulatedMarketItem);
        exemptionsData.setExemptions(exemptions);
        return exemptionsData;
    }
}
