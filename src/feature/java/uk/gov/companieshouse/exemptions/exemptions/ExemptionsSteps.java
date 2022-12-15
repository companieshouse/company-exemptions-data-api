package uk.gov.companieshouse.exemptions.exemptions;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.companieshouse.exemptions.config.AbstractMongoConfig.mongoDBContainer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.File;
import java.io.IOException;
import org.assertj.core.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions;
import uk.gov.companieshouse.exemptions.CompanyExemptionsDocument;
import uk.gov.companieshouse.exemptions.config.CucumberContext;

public class ExemptionsSteps {

    private String contextId;

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Given("the company exemptions data api service is running")
    public void theApiServiceisRunning() {
        assertThat(restTemplate).isNotNull();
    }

    @And("the company exemptions database isn't available")
    public void stopMongoDbContainer() {
        mongoDBContainer.stop();
    }

    @Given("exemptions exists for a company with company number {string}")
    public void saveCompanyExemptionsResourceToMongo(String companyNumber) throws IOException {
        File source = new ClassPathResource("/fragments/output/retrieved_exemptions_resource.json").getFile();
        CompanyExemptions exemptionsData = objectMapper.readValue(source, CompanyExemptions.class);
        CompanyExemptionsDocument companyExemptions = new CompanyExemptionsDocument();
        companyExemptions.setData(exemptionsData);
        companyExemptions.setId(companyNumber);

        mongoTemplate.save(companyExemptions);
        CucumberContext.CONTEXT.set("exemptionsData", exemptionsData);
    }

    @When("a GET request is sent for a company with company number {string}")
    public void sendCompanyExemptionsGetRequest(String company_number) throws IOException {
        String uri = "/company/"+company_number+"/exemptions";

        this.contextId = "5234234234";
        CucumberContext.CONTEXT.set("contextId", this.contextId);
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-request-id", this.contextId);
        headers.set("ERIC-Identity", "TEST-IDENTITY");
        headers.set("ERIC-Identity-Type", "KEY");
        HttpEntity<String> request = new HttpEntity<String>(null, headers);

        ResponseEntity<CompanyExemptions> response = restTemplate.exchange(uri, HttpMethod.GET, request,
                CompanyExemptions.class, company_number);

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
        File file = new ClassPathResource("/fragments/output/" + source + ".json").getFile();
        CompanyExemptions expected = objectMapper.readValue(file, CompanyExemptions.class);

        CompanyExemptions actual = CucumberContext.CONTEXT.get("getResponseBody");

        assertThat(expected.getExemptions()).isEqualTo(actual.getExemptions());
        assertThat(expected.getLinks()).isEqualTo(actual.getLinks());
        assertThat(expected.getEtag()).isEqualTo(actual.getEtag());
        assertThat(expected.getKind()).isEqualTo(actual.getKind());
    }
}
