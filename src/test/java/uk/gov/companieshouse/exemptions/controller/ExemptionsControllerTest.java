package uk.gov.companieshouse.exemptions.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.LocalDate;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions.KindEnum;
import uk.gov.companieshouse.api.exemptions.ExemptionItem;
import uk.gov.companieshouse.api.exemptions.Exemptions;
import uk.gov.companieshouse.api.exemptions.ExemptionsUpdateData;
import uk.gov.companieshouse.api.exemptions.InternalData;
import uk.gov.companieshouse.api.exemptions.InternalExemptionsApi;
import uk.gov.companieshouse.api.exemptions.PscExemptAsTradingOnRegulatedMarketItem;
import uk.gov.companieshouse.api.exemptions.PscExemptAsTradingOnRegulatedMarketItem.ExemptionTypeEnum;
import uk.gov.companieshouse.exemptions.config.WebSecurityConfig;
import uk.gov.companieshouse.exemptions.exception.BadRequestException;
import uk.gov.companieshouse.exemptions.exception.ConflictException;
import uk.gov.companieshouse.exemptions.exception.NotFoundException;
import uk.gov.companieshouse.exemptions.exception.ServiceUnavailableException;
import uk.gov.companieshouse.exemptions.service.ExemptionsService;
import uk.gov.companieshouse.logging.Logger;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = ExemptionsController.class)
@ContextConfiguration(classes = {ExemptionsController.class, ControllerExceptionHandler.class})
@Import({WebSecurityConfig.class})
class ExemptionsControllerTest {
    private static final String URI = "/company-exemptions/12345678/internal";
    private static final String GET_URI = "/company/12345678/exemptions";
    private static final String DELTA_AT = "20240219123045999999";
    private static final String COMPANY_NUMBER = "12345678";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private Logger logger;

    @MockBean
    private ExemptionsService exemptionsService;

    private final Gson gson = new GsonBuilder().setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Test
    @DisplayName("Successful upsert request")
    void upsertCompanyExemptions() throws Exception {
        // given

        // when
        ResultActions result = mockMvc.perform(put(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .content(gson.toJson(getRequestBody())));

        // then
        result.andExpect(status().isOk());
    }

    @Test
    @DisplayName("Unauthorised oauth2 upsert request")
    void upsertCompanyExemptionsUnauthorisedOauth2() throws Exception {
        // given

        // when
        ResultActions result = mockMvc.perform(put(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "oauth2")
                .content(gson.toJson(getRequestBody())));

       // then
       result.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthorised upsert request")
    void upsertCompanyExemptionsUnauthorised() throws Exception {
        // given

        // when
        ResultActions result = mockMvc.perform(put(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .content(gson.toJson(getRequestBody())));

        // then
        result.andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Server error upsert request")
    void upsertCompanyExemptionsServerError() throws Exception {
        // given
        doThrow(ServiceUnavailableException.class).when(exemptionsService)
                .upsertCompanyExemptions(anyString(),any());

        // when
        ResultActions result = mockMvc.perform(put(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .content(gson.toJson(getRequestBody())));

        // then
        result.andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("Client error upsert request")
    void upsertCompanyExemptionsClientError() throws Exception {
        // given
        doThrow(ConflictException.class).when(exemptionsService)
                .upsertCompanyExemptions(anyString(),any());

        // when
        ResultActions result = mockMvc.perform(put(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .content(gson.toJson(getRequestBody())));

        // then
         result.andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Successful get company exemptions request")
    void getCompanyExemptions() throws Exception {
        // given
        CompanyExemptions data = getExemptionsData();
        doReturn(data).when(exemptionsService).getCompanyExemptions(COMPANY_NUMBER);

        // when
        MvcResult result = mockMvc.perform(get(GET_URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key"))
                .andExpect(status().isOk())
                .andReturn();

        // then
        assertEquals(data, objectMapper.readValue(result.getResponse().getContentAsString(), CompanyExemptions.class));
    }

    @Test
    @DisplayName("Successful get company exemptions request with oauth2")
    void getCompanyExemptionsOauth2() throws Exception {
        // given
        CompanyExemptions data = getExemptionsData();
        doReturn(data).when(exemptionsService).getCompanyExemptions(COMPANY_NUMBER);

        // when
        MvcResult result = mockMvc.perform(get(GET_URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "oauth2"))
                .andExpect(status().isOk())
                .andReturn();

        // then
        assertEquals(data, objectMapper.readValue(result.getResponse().getContentAsString(), CompanyExemptions.class));
    }

    @Test
    @DisplayName("Document not found for get company exemptions request")
    void getCompanyExemptionsNotFound() throws Exception {
        // given
        when(exemptionsService.getCompanyExemptions(any())).thenThrow(NotFoundException.class);

        // when
        ResultActions result = mockMvc.perform(get(GET_URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app"));

        // then
        result.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("MongoDB is unavailable for get company exemptions request")
    void getCompanyExemptionsMongoUnavailable() throws Exception {
        // given
        when(exemptionsService.getCompanyExemptions(any())).thenThrow(ServiceUnavailableException.class);

        // when
        ResultActions result = mockMvc.perform(get(GET_URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app"));

        // then
        result.andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("Successful delete company exemptions request")
    void deleteCompanyExemptions() throws Exception {
        // given

        // when
        ResultActions result = mockMvc.perform(delete(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .header("X-DELTA-AT", DELTA_AT));

        // then
        result.andExpect(status().isOk());
    }

    @Test
    @DisplayName("Server error delete request")
    void deleteCompanyExemptionsServerError() throws Exception {
        // given
        doThrow(ServiceUnavailableException.class).when(exemptionsService)
                .deleteCompanyExemptions(anyString(), anyString());

        // when
        ResultActions result = mockMvc.perform(delete(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .header("X-DELTA-AT", DELTA_AT));

        // then
        result.andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("Not found delete request")
    void deleteCompanyExemptionsNotFound() throws Exception {
        // given
        doThrow(NotFoundException.class).when(exemptionsService)
                .deleteCompanyExemptions(anyString(), anyString());

        // when
        ResultActions result = mockMvc.perform(delete(URI)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .header("X-DELTA-AT", DELTA_AT));

        // then
        result.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Delete request returned bad request error")
    void deleteCompanyExemptionsBadRequest() throws Exception {
        // given
        doThrow(BadRequestException.class).when(exemptionsService)
                .deleteCompanyExemptions(anyString(), anyString());

        // when
        ResultActions result = mockMvc.perform(delete(URI)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "5342342")
                        .header("ERIC-Identity", "Test-Identity")
                        .header("ERIC-Identity-Type", "Key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .header("X-DELTA-AT", DELTA_AT));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Delete request returned generic runtime exception error")
    void deleteCompanyExemptionsGenericRuntimeException() throws Exception {
        // given
        doThrow(RuntimeException.class).when(exemptionsService)
                .deleteCompanyExemptions(anyString(), anyString());

        // when
        ResultActions result = mockMvc.perform(delete(URI)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "5342342")
                        .header("ERIC-Identity", "Test-Identity")
                        .header("ERIC-Identity-Type", "Key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .header("X-DELTA-AT", DELTA_AT));

        // then
        result.andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Delete request returned conflict error")
    void deleteCompanyExemptionsConflict() throws Exception {
        // given
        doThrow(ConflictException.class).when(exemptionsService)
                .deleteCompanyExemptions(anyString(), anyString());

        // when
        ResultActions result = mockMvc.perform(delete(URI)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "5342342")
                        .header("ERIC-Identity", "Test-Identity")
                        .header("ERIC-Identity-Type", "Key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .header("X-DELTA-AT", DELTA_AT));

        // then
        result.andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Successful options company exemptions request - CORS")
    void optionsCompanyExemptionsCORS() throws Exception {
        mockMvc.perform(options(GET_URI)
                        .contentType(APPLICATION_JSON)
                        .header("Origin", ""))
                .andExpect(status().isNoContent())
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_MAX_AGE))
                .andReturn();
    }

    @Test
    @DisplayName("Successful get company exemptions request - CORS")
    void getCompanyExemptionsCORS() throws Exception {
        // given
        CompanyExemptions data = getExemptionsData();
        doReturn(data).when(exemptionsService).getCompanyExemptions(COMPANY_NUMBER);

        // when
        MvcResult result = mockMvc.perform(get(GET_URI)
                        .contentType(APPLICATION_JSON)
                        .header("Origin", "")
                        .header("ERIC-Allowed-Origin", "some-origin")
                        .header("x-request-id", "5342342")
                        .header("ERIC-Identity", "Test-Identity")
                        .header("ERIC-Identity-Type", "oauth2"))
                .andExpect(status().isOk())
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")))
                .andReturn();

        // then
        assertEquals(data, objectMapper.readValue(result.getResponse().getContentAsString(), CompanyExemptions.class));
    }

    @Test
    @DisplayName("Forbidden get company exemptions request - CORS")
    void getCompanyExemptionsForbiddenCORS() throws Exception {
        mockMvc.perform(get(GET_URI)
                        .contentType(APPLICATION_JSON)
                        .header("Origin", "")
                        .header("ERIC-Allowed-Origin", "")
                        .header("x-request-id", "5342342")
                        .header("ERIC-Identity", "Test-Identity")
                        .header("ERIC-Identity-Type", "oauth2"))
                .andExpect(status().isForbidden())
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")))
                .andExpect(content().string(""))
                .andReturn();
    }

    @Test
    @DisplayName("Forbidden upsert request")
    void upsertCompanyExemptionsForbidden() throws Exception {
        mockMvc.perform(put(URI)
                        .contentType(APPLICATION_JSON)
                        .header("Origin", "")
                        .header("ERIC-Allowed-Origin", "some-origin")
                        .header("x-request-id", "5342342")
                        .header("ERIC-Identity", "Test-Identity")
                        .header("ERIC-Identity-Type", "Key")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isForbidden())
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("GET")))
                .andExpect(content().string(""))
                .andReturn();
    }

    private InternalExemptionsApi getRequestBody() {
        InternalExemptionsApi request = new InternalExemptionsApi();
        request.setInternalData(new InternalData());
        request.setExternalData(new ExemptionsUpdateData());
        return request;
    }

    private CompanyExemptions getExemptionsData() {
        CompanyExemptions exemptionsData = new CompanyExemptions();
        exemptionsData.setKind(KindEnum.EXEMPTIONS);
        Exemptions exemptions = new Exemptions();
        ExemptionItem exemptionItem = new ExemptionItem(LocalDate.of(2022, 1, 1));
        PscExemptAsTradingOnRegulatedMarketItem regulatedMarketItem =
                new PscExemptAsTradingOnRegulatedMarketItem(Collections.singletonList(exemptionItem),
                        ExemptionTypeEnum.PSC_EXEMPT_AS_TRADING_ON_REGULATED_MARKET);
        exemptions.setPscExemptAsTradingOnRegulatedMarket(regulatedMarketItem);
        exemptionsData.setExemptions(exemptions);
        return exemptionsData;
    }
}
