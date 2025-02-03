package uk.gov.companieshouse.exemptions.util;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.companieshouse.api.chskafka.ChangedResource;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions;
import uk.gov.companieshouse.api.exemptions.Exemptions;
import uk.gov.companieshouse.exemptions.model.CompanyExemptionsDocument;
import uk.gov.companieshouse.exemptions.model.ResourceChangedRequest;

@SpringBootTest
class ResourceChangedRequestMapperIT {

    private static final String CONTEXT_ID = "CONTEXT_ID";
    private static final String COMPANY_NUMBER = "12345678";

    @Autowired
    private ResourceChangedRequestMapper mapper;

    @Test
    void shouldRemoveNullValuesFromDeletedDataUsingObjectMapper() {
        // given
        ResourceChangedRequest request = new ResourceChangedRequest(CONTEXT_ID, COMPANY_NUMBER, new CompanyExemptionsDocument()
                .setData(new CompanyExemptions()
                        .exemptions(new Exemptions())), true);

        // when
        ChangedResource actual = mapper.mapDeletedEvent(request);

        // then
        assertFalse(actual.getDeletedData().toString().contains("null"));
    }
}
