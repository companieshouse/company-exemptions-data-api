package uk.gov.companieshouse.exemptions;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@CucumberContextConfiguration
@AutoConfigureMockMvc
@DirtiesContext
@ActiveProfiles({"test"})
public class Configuration extends MongoConfig {

    @MockBean
    public ExemptionsApiService exemptionsApiService;

}
