package uk.gov.companieshouse.exemptions;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;
import org.springframework.boot.test.mock.mockito.MockBean;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/test_to_run.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "uk.gov.companieshouse.exemptions")
public class Runner  {

    @MockBean
    public ExemptionsApiService exemptionsApiService;

}
