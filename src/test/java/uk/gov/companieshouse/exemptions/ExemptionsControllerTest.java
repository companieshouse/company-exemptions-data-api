package uk.gov.companieshouse.exemptions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.api.exemptions.ExemptionsUpdateData;
import uk.gov.companieshouse.api.exemptions.InternalData;
import uk.gov.companieshouse.api.exemptions.InternalExemptionsApi;
import uk.gov.companieshouse.logging.Logger;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = ExemptionsController.class)
@ContextConfiguration(classes = {ExemptionsController.class})
@Import({WebSecurityConfig.class})
class ExemptionsControllerTest {
    private static final String UPSERT_URL = "/company-exemptions/12345678/internal";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private Logger logger;

    @MockBean
    private ExemptionsService exemptionsService;

    private ObjectMapper mapper = new ObjectMapper();

    private Gson gson = new Gson();

    @BeforeEach
    void setUp() {
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Test
    @DisplayName("Successful upsert request")
    void upsertCompanyExemptions() throws Exception {
        when(exemptionsService.upsertCompanyExemptions(any(), any(), any())).thenReturn(ServiceStatus.SUCCESS);

        mockMvc.perform(put(UPSERT_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "5342342")
                        .header("ERIC-Identity", "Test-Identity")
                        .header("ERIC-Identity-Type", "Key")
                        .header("ERIC-Authorised-Key-Privileges", "*")
                        .content(gson.toJson(getRequestBody())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Server error upsert request")
    void upsertCompanyExemptionsServerError() throws Exception {
        when(exemptionsService.upsertCompanyExemptions(any(), any(), any())).thenReturn(ServiceStatus.SERVER_ERROR);

        mockMvc.perform(put(UPSERT_URL)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "*")
                .content(gson.toJson(getRequestBody())))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Client error upsert request")
    void upsertCompanyExemptionsClientError() throws Exception {
        when(exemptionsService.upsertCompanyExemptions(any(), any(), any())).thenReturn(ServiceStatus.CLIENT_ERROR);

        mockMvc.perform(put(UPSERT_URL)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "*")
                .content(gson.toJson(getRequestBody())))
                .andExpect(status().isNotFound());
    }

    private InternalExemptionsApi getRequestBody() {
        InternalExemptionsApi request = new InternalExemptionsApi();
        request.setInternalData(new InternalData());
        request.setExternalData(new ExemptionsUpdateData());
        return request;
    }
}
