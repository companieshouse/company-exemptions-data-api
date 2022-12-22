package uk.gov.companieshouse.exemptions.exemptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doReturn;
import static uk.gov.companieshouse.exemptions.MongoConfig.mongoDBContainer;
import static uk.gov.companieshouse.exemptions.ServiceStatus.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions;
import uk.gov.companieshouse.api.exemptions.InternalExemptionsApi;
import uk.gov.companieshouse.exemptions.*;
import uk.gov.companieshouse.exemptions.util.FileReaderUtil;

public class ExemptionsSteps {

    private String contextId;

    @Autowired
    private ExemptionsApiService exemptionsApiService;

    @Autowired
    private ExemptionsService service;

    @Autowired
    private MongoTemplate mongoTemplate;

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

    @Given("exemptions exists for company number {string}")
    public void saveCompanyExemptionsResourceToMongo(String companyNumber) throws IOException {
        File source = new ClassPathResource("/fragments/responses/retrieved_exemptions_resource.json").getFile();
        CompanyExemptions exemptionsData = objectMapper.readValue(source, CompanyExemptions.class);
        CompanyExemptionsDocument companyExemptions = new CompanyExemptionsDocument();
        companyExemptions.setData(exemptionsData);
        companyExemptions.setId(companyNumber);

        mongoTemplate.save(companyExemptions);
        CucumberContext.CONTEXT.set("exemptionsData", exemptionsData);
    }

    @When("a GET request is sent for company number {string}")
    public void invokeCompanyExemptionsGetRequest(String companyNumber) throws IOException {
        String uri = "/company/"+companyNumber+"/exemptions";

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-request-id", this.contextId);
        headers.set("ERIC-Identity", "TEST-IDENTITY");
        headers.set("ERIC-Identity-Type", "KEY");
        HttpEntity<String> request = new HttpEntity<String>(null, headers);

        ResponseEntity<CompanyExemptions> response = restTemplate.exchange(uri, HttpMethod.GET, request,
                CompanyExemptions.class, companyNumber);

        CucumberContext.CONTEXT.set("statusCode", response.getStatusCodeValue());
        CucumberContext.CONTEXT.set("getResponseBody", response.getBody());
    }

    @Then("a response status code of {int} should be returned")
    public void verifyStatusCodeReturned(int statusCode) {
        int expectedStatusCode = CucumberContext.CONTEXT.get("statusCode");
        Assertions.assertThat(expectedStatusCode).isEqualTo(statusCode);
    }

    @And("the response body should match the data found within {string}")
    public void verifyCompanyExemptionsContent(String source) throws IOException {
        File file = new ClassPathResource("/fragments/responses/" + source + ".json").getFile();
        CompanyExemptions expected = objectMapper.readValue(file, CompanyExemptions.class);

        CompanyExemptions actual = CucumberContext.CONTEXT.get("getResponseBody");

        assertThat(expected.getExemptions()).isEqualTo(actual.getExemptions());
        assertThat(expected.getLinks()).isEqualTo(actual.getLinks());
        assertThat(expected.getEtag()).isEqualTo(actual.getEtag());
        assertThat(expected.getKind()).isEqualTo(actual.getKind());
    }

    @Then("a PUT request matching payload within {string} is sent for company number {string}")
    public void invokeCompanyExemptionsPutRequest(String source, String companyNumber) {
        String payload = FileReaderUtil.readFile("src/feature/resources/fragments/requests/" + source + ".json");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);
        headers.set("ERIC-Identity", "TEST-IDENTITY");
        headers.set("ERIC-Identity-Type", "KEY");

        when(exemptionsApiService.invokeChsKafkaApi(new ResourceChangedRequest(
                CucumberContext.CONTEXT.get("contextId"), companyNumber, null, false))).thenReturn(CucumberContext.CONTEXT.get("serviceStatus"));

        HttpEntity request = new HttpEntity(payload, headers);
        String uri = "/company-exemptions/"+ companyNumber + "/internal";
        ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.PUT, request, Void.class, companyNumber);

        CucumberContext.CONTEXT.set("statusCode", response.getStatusCodeValue());
    }

    @And("the CHS Kafka API service is invoked for company number {string}")
    public void verifyChsKafkaApiRuns(String companyNumber) {
        ResourceChangedRequest resourceChangedRequest = new ResourceChangedRequest(
                CucumberContext.CONTEXT.get("contextId"), companyNumber, null, false);
        verify(exemptionsApiService).invokeChsKafkaApi(resourceChangedRequest);
    }

    @And("the CHS Kafka API service is not invoked for company number {string}")
    public void verifyChsKafkaApiNotInvoked(String companyNumber){
        ResourceChangedRequest resourceChangedRequest = new ResourceChangedRequest(
                CucumberContext.CONTEXT.get("contextId"), companyNumber, null, false);
        verify(exemptionsApiService, times(0)).invokeChsKafkaApi(resourceChangedRequest);
    }

    @And("nothing is persisted in the database")
    public void nothingIsPersistedInTheDatabase() {
        List<CompanyExemptionsDocument> dbDocs = exemptionsRepository.findAll();
        assertThat(dbDocs).hasSize(0);
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

    @When("a Put request is sent without ERIC headers")
    public void PutRequestSentWithoutERICHeaders(){
        String payload = FileReaderUtil.readFile("src/feature/resources/fragments/requests/exemptions_api_request.json");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        headers.set("x-request-id", this.contextId);

        HttpEntity request = new HttpEntity(payload, headers);
        String uri = "/company-exemptions/{companyNumber}/internal";
        String companyNumber="00006400";

        ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.PUT, request, Void.class, companyNumber);

        CucumberContext.CONTEXT.set("statusCode", response.getStatusCodeValue());
    }

    @And("the exemptions {string} for {string} have been saved in the database")
    public void exemptionsForCompanyNumberHaveBeenSavedInDatabase(String source, String companyNumber) throws IOException {
        File file = new ClassPathResource("/fragments/responses/" + source + ".json").getFile();
        CompanyExemptions exemptionsData = objectMapper.readValue(file, CompanyExemptions.class);
        CompanyExemptionsDocument companyExemptions = new CompanyExemptionsDocument();
        companyExemptions.setData(exemptionsData);
        companyExemptions.setId(companyNumber);

        mongoTemplate.save(companyExemptions);

        CompanyExemptions actual = mongoTemplate.findById(companyNumber, CompanyExemptionsDocument.class).getData();

        CompanyExemptions expected = objectMapper.readValue(file, CompanyExemptions.class);

        assertThat(expected.getExemptions()).isEqualTo(actual.getExemptions());
        assertThat(expected.getLinks()).isEqualTo(actual.getLinks());
        assertThat(expected.getEtag()).isEqualTo(actual.getEtag());
        assertThat(expected.getKind()).isEqualTo(actual.getKind());

    }
}
