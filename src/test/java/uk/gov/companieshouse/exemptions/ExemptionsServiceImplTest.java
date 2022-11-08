package uk.gov.companieshouse.exemptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import uk.gov.companieshouse.api.exemptions.InternalData;
import uk.gov.companieshouse.api.exemptions.InternalExemptionsApi;
import uk.gov.companieshouse.logging.Logger;


import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExemptionsServiceImplTest {

    private static final String COMPANY_NUMBER = "12345678";

    @Mock
    private ExemptionsRepository repository;

    @Mock
    private ExemptionsMapper mapper;

    @Mock
    private ExemptionsApiService exemptionsApiService;

    @Mock
    private Logger logger;

    @Captor
    private ArgumentCaptor<String> dateCaptor;

    @InjectMocks
    private ExemptionsServiceImpl service;

    private InternalExemptionsApi requestBody;
    private CompanyExemptionsDocument document;
    private String dateString;

    @BeforeEach
    public void setUp() {
        OffsetDateTime date = OffsetDateTime.now();
        requestBody = new InternalExemptionsApi();
        InternalData internal = new InternalData();
        internal.setDeltaAt(date);
        requestBody.setInternalData(internal);
        document = new CompanyExemptionsDocument();
        document.setUpdated(new Updated().setAt(LocalDateTime.now()));
        final DateTimeFormatter dateTimeFormatter =
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateString = date.format(dateTimeFormatter);
    }

    @Test
    @DisplayName("Test successful insert and call to chs kafka api")
    void insertCompanyExemptions() {
        when(repository.findUpdatedExemptions(eq(COMPANY_NUMBER), dateCaptor.capture())).thenReturn(Collections.emptyList());
        when(repository.findById(COMPANY_NUMBER)).thenReturn(Optional.empty());
        when(mapper.map(COMPANY_NUMBER, requestBody)).thenReturn(document);
        when(exemptionsApiService.invokeChsKafkaApi(any())).thenReturn(ServiceStatus.SUCCESS);

        ServiceStatus serviceStatus = service.upsertCompanyExemptions("", COMPANY_NUMBER, requestBody);

        assertEquals(ServiceStatus.SUCCESS, serviceStatus);
        assertEquals(dateString, dateCaptor.getValue());
        assertNotNull(document.getCreated().getAt());
        verify(repository).save(document);
        verify(exemptionsApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER, null, false));
    }

    @Test
    @DisplayName("Test successful update and call to chs kafka api")
    void updateCompanyExemptions() {
        document.setCreated(new Created().setAt(LocalDateTime.of(2022, 11, 2, 15, 55)));
        when(repository.findUpdatedExemptions(eq(COMPANY_NUMBER), dateCaptor.capture())).thenReturn(Collections.emptyList());
        when(repository.findById(COMPANY_NUMBER)).thenReturn(Optional.of(document));
        when(mapper.map(COMPANY_NUMBER, requestBody)).thenReturn(document);
        when(exemptionsApiService.invokeChsKafkaApi(any())).thenReturn(ServiceStatus.SUCCESS);

        ServiceStatus serviceStatus = service.upsertCompanyExemptions("", COMPANY_NUMBER, requestBody);

        assertEquals(ServiceStatus.SUCCESS, serviceStatus);
        assertEquals(dateString, dateCaptor.getValue());
        assertEquals(LocalDateTime.of(2022, 11, 2, 15, 55), document.getCreated().getAt());
        verify(repository).save(document);
        verify(exemptionsApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER, null, false));
    }

    @Test
    @DisplayName("Test should not update exemptions record from out of date delta")
    void outOfDateDelta() {
        when(repository.findUpdatedExemptions(eq(COMPANY_NUMBER), dateCaptor.capture())).thenReturn(Collections.singletonList(new CompanyExemptionsDocument()));

        ServiceStatus serviceStatus = service.upsertCompanyExemptions("", COMPANY_NUMBER, requestBody);

        assertEquals(ServiceStatus.CLIENT_ERROR, serviceStatus);
        assertEquals(dateString, dateCaptor.getValue());
        verify(repository).findUpdatedExemptions(COMPANY_NUMBER, dateString);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(exemptionsApiService);
    }

    @Test
    @DisplayName("Test should return status server error when save to repository throws illegal arg exception")
    void saveToRepositoryError() {
        when(repository.findUpdatedExemptions(eq(COMPANY_NUMBER), dateCaptor.capture())).thenReturn(Collections.emptyList());
        when(repository.findById(COMPANY_NUMBER)).thenReturn(Optional.empty());
        when(mapper.map(COMPANY_NUMBER, requestBody)).thenReturn(document);
        when(repository.save(document)).thenThrow(IllegalArgumentException.class);

        ServiceStatus serviceStatus = service.upsertCompanyExemptions("", COMPANY_NUMBER, requestBody);

        assertEquals(ServiceStatus.SERVER_ERROR, serviceStatus);
        assertEquals(dateString, dateCaptor.getValue());
        verify(repository).findUpdatedExemptions(COMPANY_NUMBER, dateString);
        verify(repository).save(document);
        verifyNoInteractions(exemptionsApiService);
    }

    @Test
    @DisplayName("Test successful call to get company exemptions")
    void getCompanyExemptions() {
        when(repository.findById(any())).thenReturn(Optional.of(document));

        Optional<CompanyExemptionsDocument> actual = service.getCompanyExemptions(COMPANY_NUMBER);

        assertEquals(document, actual.get());
        verify(repository).findById(COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Test call to get company exemptions returns not found")
    void getCompanyExemptionsNotFound() {
        when(repository.findById(any())).thenReturn(Optional.empty());

        Optional<CompanyExemptionsDocument> actual = service.getCompanyExemptions(COMPANY_NUMBER);

        assertEquals(Optional.empty(), actual);
        verify(repository).findById(COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Test call to get company exemptions throws service unavailable")
    void getCompanyExemptionsDataAccessException() {
        when(repository.findById(any())).thenThrow(ServiceUnavailableException.class);

        Executable executable = () -> service.getCompanyExemptions(COMPANY_NUMBER);

        Exception exception = assertThrows(ServiceUnavailableException.class, executable);
        assertEquals("Data access exception thrown when calling Mongo Repository", exception.getMessage());
        verify(repository).findById(COMPANY_NUMBER);
    }
}