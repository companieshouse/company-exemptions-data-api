package uk.gov.companieshouse.exemptions.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.chskafka.ChangedResource;
import uk.gov.companieshouse.api.chskafka.ChangedResourceEvent;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions;
import uk.gov.companieshouse.exemptions.model.ResourceChangedRequest;
import uk.gov.companieshouse.exemptions.util.ResourceChangedRequestMapper;

@ExtendWith(MockitoExtension.class)
class ResourceChangedRequestMapperTest {

    private static final String EXPECTED_CONTEXT_ID = "35234234";
    private static final Instant UPDATED_AT = Instant.now().truncatedTo(ChronoUnit.MILLIS);
    private static final String PUBLISHED_AT = DateUtils.publishedAtString(UPDATED_AT);

    @Mock
    private Supplier<Instant> instantSupplier;

    @InjectMocks
    private ResourceChangedRequestMapper mapper;

    @ParameterizedTest
    @MethodSource("resourceChangedScenarios")
    void testMapper(ResourceChangedTestArgument argument) {
        // given
        when(instantSupplier.get()).thenReturn(UPDATED_AT);

        // when
        ChangedResource actual = mapper.mapChangedResource(argument.request());

        // then
        assertEquals(argument.changedResource(), actual);
    }

    static Stream<ResourceChangedTestArgument> resourceChangedScenarios() {
        return Stream.of(
                ResourceChangedTestArgument.builder()
                        .withRequest(new ResourceChangedRequest(EXPECTED_CONTEXT_ID, "12345678", null, false))
                        .withContextId(EXPECTED_CONTEXT_ID)
                        .withResourceUri("company/12345678/exemptions")
                        .withResourceKind("company-exemptions")
                        .withEventType("changed")
                        .withEventPublishedAt(PUBLISHED_AT)
                        .build(),
                ResourceChangedTestArgument.builder()
                        .withRequest(new ResourceChangedRequest(EXPECTED_CONTEXT_ID, "12345678", null, null))
                        .withContextId(EXPECTED_CONTEXT_ID)
                        .withResourceUri("company/12345678/exemptions")
                        .withResourceKind("company-exemptions")
                        .withEventType("changed")
                        .withEventPublishedAt(PUBLISHED_AT)
                        .build(),
                ResourceChangedTestArgument.builder()
                        .withRequest(new ResourceChangedRequest(EXPECTED_CONTEXT_ID, "12345678", new CompanyExemptions(), true))
                        .withContextId(EXPECTED_CONTEXT_ID)
                        .withResourceUri("company/12345678/exemptions")
                        .withResourceKind("company-exemptions")
                        .withEventType("deleted")
                        .withDeletedData(new CompanyExemptions())
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
}
