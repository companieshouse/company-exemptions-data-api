package uk.gov.companieshouse.exemptions.service;

import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.chskafka.request.PrivateChangedResourcePost;
import uk.gov.companieshouse.exemptions.exception.ServiceUnavailableException;
import uk.gov.companieshouse.exemptions.model.ResourceChangedRequest;
import uk.gov.companieshouse.exemptions.util.ResourceChangedRequestMapper;
import uk.gov.companieshouse.logging.Logger;

@Component
public class ExemptionsApiService {
    private static final String CHANGED_RESOURCE_URI = "/private/resource-changed";
    private final Logger logger;
    private final Supplier<InternalApiClient> apiClientSupplier;
    private final ResourceChangedRequestMapper mapper;

    /**
     * Invoke API.
     */
    public ExemptionsApiService(Supplier<InternalApiClient> apiClientSupplier, Logger logger,
                                ResourceChangedRequestMapper mapper) {
        this.apiClientSupplier = apiClientSupplier;
        this.logger = logger;
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
                        CHANGED_RESOURCE_URI, mapper.mapChangedResource(resourceChangedRequest));
        try {
            changedResourcePost.execute();
        } catch (ApiErrorResponseException ex) {
            logger.info("Resource changed call failed: %s".formatted(ex.getStatusCode()));
            throw new ServiceUnavailableException("Error calling resource changed endpoint");
        }
    }
}
