package uk.gov.companieshouse.exemptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.companieshouse.exemptions.service.ExemptionsApiService;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@CucumberContextConfiguration
@AutoConfigureMockMvc
@DirtiesContext
@ActiveProfiles({"test"})
@TestConfiguration
public class Configuration extends MongoConfig {

    @MockitoBean
    public ExemptionsApiService exemptionsApiService;

    @MockitoBean
    public ObjectMapper objectMapper;

}
