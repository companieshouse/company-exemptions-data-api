package uk.gov.companieshouse.exemptions.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.chskafka.ChangedResource;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.chskafka.PrivateChangedResourceHandler;
import uk.gov.companieshouse.api.handler.chskafka.request.PrivateChangedResourcePost;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.exemptions.exception.ServiceUnavailableException;
import uk.gov.companieshouse.exemptions.model.ResourceChangedRequest;
import uk.gov.companieshouse.exemptions.util.ResourceChangedRequestMapper;
import uk.gov.companieshouse.logging.Logger;

@ExtendWith(MockitoExtension.class)
class ExemptionsApiServiceTest {

    @Mock
    private Supplier<InternalApiClient> apiClientSupplier;

    @Mock
    private InternalApiClient internalApiClient;

    @Mock
    private PrivateChangedResourceHandler privateChangedResourceHandler;

    @Mock
    private PrivateChangedResourcePost changedResourcePost;

    @Mock
    private ApiResponse<Void> response;

    @Mock
    private Logger logger;

    @Mock
    private ResourceChangedRequestMapper mapper;

    @Mock
    private ResourceChangedRequest resourceChangedRequest;

    @Mock
    private ChangedResource changedResource;

    private ExemptionsApiService exemptionsApiService;

    @BeforeEach
    void setup() {
        exemptionsApiService = new ExemptionsApiService(apiClientSupplier, logger, mapper);
    }

    @Test
    @DisplayName("Test should successfully invoke chs-kafka-api")
    void invokeChsKafkaApi() throws ApiErrorResponseException {
        // given
        when(apiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateChangedResourceHandler()).thenReturn(privateChangedResourceHandler);
        when(privateChangedResourceHandler.postChangedResource(any(), any())).thenReturn(changedResourcePost);
        when(changedResourcePost.execute()).thenReturn(response);
        when(mapper.mapChangedResource(resourceChangedRequest)).thenReturn(changedResource);

        // when
        exemptionsApiService.invokeChsKafkaApi(resourceChangedRequest);

        // then
        verify(apiClientSupplier).get();
        verify(internalApiClient).privateChangedResourceHandler();
        verify(privateChangedResourceHandler).postChangedResource("/private/resource-changed", changedResource);
        verify(changedResourcePost).execute();
    }

    @Test
    @DisplayName("Throw service unavailable exception when response code is HTTP 503")
    void invokeChsKafkaApi503() throws ApiErrorResponseException {
        // given
        setupExceptionScenario(503, "Service Unavailable");

        // when
        Executable actual = () -> exemptionsApiService.invokeChsKafkaApi(resourceChangedRequest);

        // then
        assertThrows(ServiceUnavailableException.class, actual);
        verifyExceptionScenario();
    }

    @Test
    @DisplayName("Throw service unavailable exception when response code is HTTP 500")
    void invokeChsKafkaApi500() throws ApiErrorResponseException {
        // given
        setupExceptionScenario(500, "Internal Service Error");

        // when
        Executable actual = () -> exemptionsApiService.invokeChsKafkaApi(resourceChangedRequest);

        // then
        assertThrows(ServiceUnavailableException.class, actual);
        verifyExceptionScenario();
    }

    @Test
    @DisplayName("Throw service unavailable exception when response code is HTTP 200 with errors")
    void invokeChsKafkaApi200Errors() throws ApiErrorResponseException {
        // given
        setupExceptionScenario(200, "");

        // when
        Executable actual = () -> exemptionsApiService.invokeChsKafkaApi(resourceChangedRequest);

        // then
        assertThrows(ServiceUnavailableException.class, actual);
        verifyExceptionScenario();
    }

    private void setupExceptionScenario(int statusCode, String statusMessage) throws ApiErrorResponseException {
        when(apiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateChangedResourceHandler()).thenReturn(privateChangedResourceHandler);
        when(privateChangedResourceHandler.postChangedResource(any(), any())).thenReturn(changedResourcePost);
        when(mapper.mapChangedResource(resourceChangedRequest)).thenReturn(changedResource);

        HttpResponseException.Builder builder = new HttpResponseException.Builder(statusCode,
                statusMessage, new HttpHeaders());
        ApiErrorResponseException apiErrorResponseException =
                new ApiErrorResponseException(builder);
        when(changedResourcePost.execute()).thenThrow(apiErrorResponseException);
    }

    private void verifyExceptionScenario() throws ApiErrorResponseException {
        verify(apiClientSupplier).get();
        verify(internalApiClient).privateChangedResourceHandler();
        verify(privateChangedResourceHandler).postChangedResource("/private/resource-changed", changedResource);
        verify(changedResourcePost).execute();
    }
}
