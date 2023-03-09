package uk.gov.companieshouse.exemptions.exemptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.exemptions.MongoConfig.mongoDBContainer;
import static uk.gov.companieshouse.exemptions.ServiceStatus.SERVER_ERROR;
import static uk.gov.companieshouse.exemptions.ServiceStatus.SUCCESS;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.File;
import java.io.IOException;
import java.util.Collections;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions;
import uk.gov.companieshouse.exemptions.CompanyExemptionsDocument;
import uk.gov.companieshouse.exemptions.CucumberContext;
import uk.gov.companieshouse.exemptions.ExemptionsApiService;
import uk.gov.companieshouse.exemptions.ExemptionsRepository;
import uk.gov.companieshouse.exemptions.ResourceChangedRequest;
import uk.gov.companieshouse.exemptions.util.FileReaderUtil;

public class ExemptionsSteps {

    private String contextId;

    @Autowired
    private ExemptionsApiService exemptionsApiService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExemptionsRepository exemptionsRepository;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Before
    public void dbCleanUp(){
        if (!mongoDBContainer.isRunning()) {
            mongoDBContainer.start();
        }
        exemptionsRepository.deleteAll();
    }

    @Given("the company exemptions data api service is running")
    public void theApiServiceisRunning() {
        assertThat(restTemplate).isNotNull();
    }

    @And("the company exemptions database isn't available")
    public void stopMongoDbContainer() {
        mongoDBContainer.stop();
    }

    @And("exemptions exists for company number {string} with delta_at {string}")
    public void saveCompanyExemptionsResourceToTheDatabase(String companyNumber, String deltaAt) throws IOException {
        File source = new ClassPathResource("/fragments/responses/retrieved_exemptions_resource.json").getFile();
        CompanyExemptions exemptionsData = objectMapper.readValue(source, CompanyExemptions.class);

        CompanyExemptionsDocument companyExemptions = new CompanyExemptionsDocument();
        companyExemptions.setData(exemptionsData).setId(companyNumber);
        companyExemptions.setDeltaAt(deltaAt);

        exemptionsRepository.save(companyExemptions);
        CucumberContext.CONTEXT.set("exemptionsData", exemptionsData);
    }

    @And("exemptions exists for company number {string}")
    public void saveCompanyExemptionsResourceToTheDatabase(String companyNumber) throws IOException {
       saveCompanyExemptionsResourceToTheDatabase(companyNumber, "");
    }

    @When("a GET request is sent for company number {string}")
    public void invokeCompanyExemptionsGetRequest(String companyNumber) {
        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-request-id", this.contextId);
        headers.set("ERIC-Identity", "TEST-IDENTITY");
        headers.set("ERIC-Identity-Type", "KEY");
        HttpEntity<String> request = new HttpEntity<>(null, headers);

        String uri = String.format("/company/%s/exemptions", companyNumber);
        ResponseEntity<CompanyExemptions> response = restTemplate.exchange(uri, HttpMethod.GET, request,
                CompanyExemptions.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCodeValue());
        CucumberContext.CONTEXT.set("getResponseBody", response.getBody());
    }

    @Then("a response status code of {int} should be returned")
    public void verifyStatusCodeReturned(int statusCode) {
        int expectedStatusCode = CucumberContext.CONTEXT.get("statusCode");
        assertThat(expectedStatusCode).isEqualTo(statusCode);
    }

    @And("the response body should match the data found within {string}")
    public void verifyCompanyExemptionsContent(String source) throws IOException {
        File file = new ClassPathResource(String.format("/fragments/responses/%s.json", source)).getFile();
        CompanyExemptions expected = objectMapper.readValue(file, CompanyExemptions.class);

        CompanyExemptions actual = CucumberContext.CONTEXT.get("getResponseBody");

        assertThat(expected.getExemptions()).isEqualTo(actual.getExemptions());
        assertThat(expected.getLinks()).isEqualTo(actual.getLinks());
        assertThat(expected.getEtag()).isEqualTo(actual.getEtag());
        assertThat(expected.getKind()).isEqualTo(actual.getKind());
    }

    @Then("a PUT request matching payload within {string} is sent for {string}")
    public void invokeCompanyExemptionsPutRequest(String source, String companyNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);
        headers.set("ERIC-Identity", "TEST-IDENTITY");
        headers.set("ERIC-Identity-Type", "KEY");
        headers.set("ERIC-Authorised-Key-Privileges", "internal-app");

        when(exemptionsApiService.invokeChsKafkaApi(new ResourceChangedRequest(
                CucumberContext.CONTEXT.get("contextId"), companyNumber, null, false)))
                    .thenReturn(CucumberContext.CONTEXT.get("serviceStatus"));

        String payload = FileReaderUtil.readFile(String.format("src/feature/resources/fragments/requests/%s.json", source));
        HttpEntity<String> request = new HttpEntity<>(payload, headers);
        String uri = String.format("/company-exemptions/%s/internal", companyNumber);
        ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.PUT, request, Void.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCodeValue());
    }

    @And("the CHS Kafka API service is invoked for upsert with {string}")
    public void verifyChsKafkaApiRuns(String companyNumber) {
        ResourceChangedRequest resourceChangedRequest = new ResourceChangedRequest(
                CucumberContext.CONTEXT.get("contextId"), companyNumber, null, false);
        verify(exemptionsApiService).invokeChsKafkaApi(resourceChangedRequest);
    }

    @And("the CHS Kafka API service is not invoked")
    public void verifyChsKafkaApiNotInvoked(){
        verifyNoInteractions(exemptionsApiService);
    }

    @And("nothing is persisted in the database")
    public void nothingIsPersistedInTheDatabase() {
        assertThat(exemptionsRepository.findAll()).isEmpty();
    }

    @Given("the CHS Kafka API service is unavailable")
    public void ChsKafkaApiUnavailable() {
        CucumberContext.CONTEXT.set("serviceStatus", SERVER_ERROR);
    }

    @Given("CHS Kafka API Service is available")
    public void ChsKafKaApiAvailable(){
        CucumberContext.CONTEXT.set("serviceStatus", SUCCESS);
    }

    @Given ("the exemptions database is unavailable")
    public void exemptionsDatabaseIsDown(){
        mongoDBContainer.stop();
    }

    @When("a PUT request is sent without ERIC headers for {string}")
    public void PutRequestSentWithoutERICHeaders(String companyNumber){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);

        String payload = FileReaderUtil.readFile("src/feature/resources/fragments/requests/exemptions_api_request.json");
        HttpEntity<String> request = new HttpEntity<>(payload, headers);
        String uri = String.format("/company-exemptions/%s/internal", companyNumber);

        ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.PUT, request, Void.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCodeValue());
    }

    @And("the exemptions {string} for {string} exist in the database")
    public void exemptionsForCompanyNumberHaveBeenSavedInDatabase(String source, String companyNumber) throws IOException {
        File file = new ClassPathResource(String.format("/fragments/responses/%s.json", source)).getFile();

        Optional<CompanyExemptionsDocument> document = exemptionsRepository.findById(companyNumber);
        assertTrue(document.isPresent());

        CompanyExemptions actual = document.get().getData();
        CompanyExemptions expected = objectMapper.readValue(file, CompanyExemptions.class);

        assertThat(expected.getExemptions()).isEqualTo(actual.getExemptions());
        assertThat(expected.getLinks()).isEqualTo(actual.getLinks());
        assertThat(expected.getEtag()).isEqualTo(actual.getEtag());
        assertThat(expected.getKind()).isEqualTo(actual.getKind());

    }

    @When("a request is sent to the delete endpoint for {string}")
    public void sendRequestToDeleteEndpoint(String companyNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);
        headers.set("ERIC-Identity", "TEST-IDENTITY");
        headers.set("ERIC-Identity-Type", "KEY");
        headers.set("ERIC-Authorised-Key-Privileges", "internal-app");

        CompanyExemptions data = CucumberContext.CONTEXT.get("exemptionsData");

        when(exemptionsApiService.invokeChsKafkaApi(new ResourceChangedRequest(
                CucumberContext.CONTEXT.get("contextId"), companyNumber, data, true)))
                .thenReturn(CucumberContext.CONTEXT.get("serviceStatus"));

        HttpEntity<String> request = new HttpEntity<>(null, headers);
        String uri = String.format("/company-exemptions/%s/internal", companyNumber);
        ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.DELETE, request, Void.class, companyNumber);

        CucumberContext.CONTEXT.set("statusCode", response.getStatusCodeValue());
    }

    @And("the CHS Kafka Api service is invoked for {string} for a delete")
    public void verifyCHSKafkaApiIsInvokedForDelete(String companyNumber) {
        CompanyExemptions data = CucumberContext.CONTEXT.get("exemptionsData");

        ResourceChangedRequest resourceChangedRequest = new ResourceChangedRequest(
                CucumberContext.CONTEXT.get("contextId"), companyNumber, data, true);
        verify(exemptionsApiService).invokeChsKafkaApi(resourceChangedRequest);
    }

    @And("the resource does not exist in the database for {string}")
    public void verifyResourceWasDeletedInTheDatabase(String companyNumber) {
        assertThat(exemptionsRepository.findById(companyNumber)).isEmpty();
    }

    @When("a DELETE request is sent without ERIC headers for {string}")
    public void deleteRequestSentWithoutEricHeaders(String companyNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);

        HttpEntity<String> request = new HttpEntity<>(null, headers);
        String uri = String.format("/company-exemptions/%s/internal", companyNumber);

        ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.DELETE, request, Void.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCodeValue());
    }
}
