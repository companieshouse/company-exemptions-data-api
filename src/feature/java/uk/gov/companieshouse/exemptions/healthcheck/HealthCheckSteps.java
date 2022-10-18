package uk.gov.companieshouse.exemptions.healthcheck;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

public class HealthCheckSteps {

    @Autowired
    private MockMvc mockMvc;

    private ResultActions resultActions;

    @When("the user performs a healthcheck")
    public void performLivenessCheck() throws Exception {
        resultActions = mockMvc.perform(get("/healthcheck"));
    }

    @Then("the response code should be {int}")
    public void verifyResponseCode(int expected) throws Exception {
        resultActions.andExpect(status().is(expected));
    }
}
