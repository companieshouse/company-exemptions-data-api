package uk.gov.companieshouse.exemptions.service;

import static uk.gov.companieshouse.exemptions.ExemptionsApplication.APPLICATION_NAME_SPACE;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.chskafka.request.PrivateChangedResourcePost;
import uk.gov.companieshouse.exemptions.exception.ServiceUnavailableException;
import uk.gov.companieshouse.exemptions.logging.DataMapHolder;
import uk.gov.companieshouse.exemptions.model.ResourceChangedRequest;
import uk.gov.companieshouse.exemptions.util.ResourceChangedRequestMapper;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
public class ExemptionsApiService {

    private static final String CHANGED_RESOURCE_URI = "/private/resource-changed";
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);

    private final Supplier<InternalApiClient> apiClientSupplier;
    private final ResourceChangedRequestMapper mapper;

    /**
     * Invoke API.
     */
    public ExemptionsApiService(Supplier<InternalApiClient> apiClientSupplier, ResourceChangedRequestMapper mapper) {
        this.apiClientSupplier = apiClientSupplier;
        this.mapper = mapper;
    }


    /**
     * Calls the CHS Kafka api.
     * @param resourceChangedRequest encapsulates details relating to the updated or deleted company exemption
     * @return The service status of the response from chs kafka api
     */
    public void invokeChsKafkaApi(ResourceChangedRequest resourceChangedRequest) {
        InternalApiClient internalApiClient = apiClientSupplier.get();

        PrivateChangedResourcePost changedResourcePost =
                internalApiClient.privateChangedResourceHandler().postChangedResource(
                        CHANGED_RESOURCE_URI, mapper.mapChangedEvent(resourceChangedRequest));
        try {
            changedResourcePost.execute();
        } catch (ApiErrorResponseException ex) {
            LOGGER.error("Resource changed call failed: %s".formatted(ex.getStatusCode()), DataMapHolder.getLogMap());
            throw new ServiceUnavailableException("Error calling resource changed endpoint");
        }
    }

    public void invokeChsKafkaApiDelete(ResourceChangedRequest resourceChangedRequest) {
        InternalApiClient internalApiClient = apiClientSupplier.get();

        PrivateChangedResourcePost changedResourcePost =
                internalApiClient.privateChangedResourceHandler().postChangedResource(
                        CHANGED_RESOURCE_URI, mapper.mapDeletedEvent(resourceChangedRequest));
        try {
            changedResourcePost.execute();
        } catch (ApiErrorResponseException ex) {
            LOGGER.error("Resource changed call failed: %s".formatted(ex.getStatusCode()), DataMapHolder.getLogMap());
            throw new ServiceUnavailableException("Error calling resource changed endpoint");
        }
    }
}
