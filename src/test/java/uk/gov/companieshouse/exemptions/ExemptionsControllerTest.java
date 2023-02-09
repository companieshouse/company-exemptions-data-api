package uk.gov.companieshouse.exemptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions;
import uk.gov.companieshouse.api.exemptions.ExemptionsUpdateData;
import uk.gov.companieshouse.api.exemptions.InternalData;
import uk.gov.companieshouse.api.exemptions.InternalExemptionsApi;
import uk.gov.companieshouse.logging.Logger;

import java.util.Optional;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = ExemptionsController.class)
@ContextConfiguration(classes = {ExemptionsController.class, ExceptionHandlerConfig.class})
@Import({WebSecurityConfig.class})
class ExemptionsControllerTest {
    private static final String URI = "/company-exemptions/12345678/internal";
    private static final String GET_URI = "/company/12345678/exemptions";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private Logger logger;

    @MockBean
    private ExemptionsService exemptionsService;

    private Gson gson = new Gson();

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Test
    @DisplayName("Successful upsert request")
    void upsertCompanyExemptions() throws Exception {
        when(exemptionsService.upsertCompanyExemptions(any(), any(), any())).thenReturn(ServiceStatus.SUCCESS);

        mockMvc.perform(put(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .content(gson.toJson(getRequestBody())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthorised oauth2 upsert request")
    void upsertCompanyExemptionsUnauthorisedOauth2() throws Exception {
        when(exemptionsService.upsertCompanyExemptions(any(), any(), any())).thenReturn(ServiceStatus.SUCCESS);

        mockMvc.perform(put(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "oauth2")
                .content(gson.toJson(getRequestBody())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthorised upsert request")
    void upsertCompanyExemptionsUnauthorised() throws Exception {
        when(exemptionsService.upsertCompanyExemptions(any(), any(), any())).thenReturn(ServiceStatus.SUCCESS);

        mockMvc.perform(put(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .content(gson.toJson(getRequestBody())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Server error upsert request")
    void upsertCompanyExemptionsServerError() throws Exception {
        when(exemptionsService.upsertCompanyExemptions(any(), any(), any())).thenReturn(ServiceStatus.SERVER_ERROR);

        mockMvc.perform(put(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .content(gson.toJson(getRequestBody())))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("Client error upsert request")
    void upsertCompanyExemptionsClientError() throws Exception {
        when(exemptionsService.upsertCompanyExemptions(any(), any(), any())).thenReturn(ServiceStatus.CLIENT_ERROR);

        mockMvc.perform(put(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .content(gson.toJson(getRequestBody())))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Successful get company exemptions request")
    void getCompanyExemptions() throws Exception {
        CompanyExemptionsDocument document = new CompanyExemptionsDocument();
        CompanyExemptions data = new CompanyExemptions();
        document.setData(data);

        when(exemptionsService.getCompanyExemptions(any())).thenReturn(Optional.of(document));

        MvcResult result = mockMvc.perform(get(GET_URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(data, objectMapper.readValue(result.getResponse().getContentAsString(), CompanyExemptions.class));
    }

    @Test
    @DisplayName("Successful get company exemptions request with oauth2")
    void getCompanyExemptionsOauth2() throws Exception {
        CompanyExemptionsDocument document = new CompanyExemptionsDocument();
        CompanyExemptions data = new CompanyExemptions();
        document.setData(data);

        when(exemptionsService.getCompanyExemptions(any())).thenReturn(Optional.of(document));

        MvcResult result = mockMvc.perform(get(GET_URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "oauth2"))
                .andExpect(status().isOk())
                .andReturn();

        assertEquals(data, objectMapper.readValue(result.getResponse().getContentAsString(), CompanyExemptions.class));
    }

    @Test
    @DisplayName("Document not found for get company exemptions request")
    void getCompanyExemptionsNotFound() throws Exception {
        when(exemptionsService.getCompanyExemptions(any())).thenReturn(Optional.empty());

        mockMvc.perform(get(GET_URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("MongoDB is unavailable for get company exemptions request")
    void getCompanyExemptionsMongoUnavailable() throws Exception {
        when(exemptionsService.getCompanyExemptions(any())).thenThrow(ServiceUnavailableException.class);

        mockMvc.perform(get(GET_URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("Successful delete company exemptions request")
    void deleteCompanyExemptions() throws Exception {
        when(exemptionsService.deleteCompanyExemptions(any(), any())).thenReturn(ServiceStatus.SUCCESS);

        mockMvc.perform(delete(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Server error delete request")
    void deleteCompanyExemptionsServerError() throws Exception {
        when(exemptionsService.deleteCompanyExemptions(any(), any())).thenReturn(ServiceStatus.SERVER_ERROR);

        mockMvc.perform(delete(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("Not found delete request")
    void deleteCompanyExemptionsNotFound() throws Exception {
        when(exemptionsService.deleteCompanyExemptions(any(), any())).thenReturn(ServiceStatus.CLIENT_ERROR);

        mockMvc.perform(delete(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isNotFound());
    }

    private InternalExemptionsApi getRequestBody() {
        InternalExemptionsApi request = new InternalExemptionsApi();
        request.setInternalData(new InternalData());
        request.setExternalData(new ExemptionsUpdateData());
        return request;
    }
}
