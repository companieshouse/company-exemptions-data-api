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
import uk.gov.companieshouse.api.exemptions.CompanyExemptions;
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
import static org.mockito.Mockito.doThrow;
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
        verify(exemptionsApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER, null, false));
        verify(repository).save(document);
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
        verify(exemptionsApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER, null, false));
        verify(repository).save(document);
    }

    @Test
    @DisplayName("Test should not update exemptions record from out of date delta")
    void outOfDateDelta() {
        when(repository.findUpdatedExemptions(eq(COMPANY_NUMBER), dateCaptor.capture())).thenReturn(Collections.singletonList(new CompanyExemptionsDocument()));

        ServiceStatus serviceStatus = service.upsertCompanyExemptions("", COMPANY_NUMBER, requestBody);

        assertEquals(ServiceStatus.CLIENT_ERROR, serviceStatus);
        assertEquals(dateString, dateCaptor.getValue());
        verify(repository).findUpdatedExemptions(COMPANY_NUMBER, dateString);
        verifyNoInteractions(exemptionsApiService);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("Test should return status server error when save to repository throws data access exception on findById")
    void saveToRepositoryFindError() {
        when(repository.findUpdatedExemptions(eq(COMPANY_NUMBER), dateCaptor.capture())).thenReturn(Collections.emptyList());
        when(mapper.map(COMPANY_NUMBER, requestBody)).thenReturn(document);
        when(repository.findById(COMPANY_NUMBER)).thenThrow(ServiceUnavailableException.class);

        ServiceStatus serviceStatus = service.upsertCompanyExemptions("", COMPANY_NUMBER, requestBody);

        assertEquals(ServiceStatus.SERVER_ERROR, serviceStatus);
        assertEquals(dateString, dateCaptor.getValue());
        verify(repository).findUpdatedExemptions(COMPANY_NUMBER, dateString);
        verifyNoInteractions(exemptionsApiService);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("Test should return status server error when save to repository throws data access exception")
    void saveToRepositoryError() {
        when(repository.findUpdatedExemptions(eq(COMPANY_NUMBER), dateCaptor.capture())).thenReturn(Collections.emptyList());
        when(repository.findById(COMPANY_NUMBER)).thenReturn(Optional.empty());
        when(mapper.map(COMPANY_NUMBER, requestBody)).thenReturn(document);
        when(repository.save(document)).thenThrow(ServiceUnavailableException.class);

        ServiceStatus serviceStatus = service.upsertCompanyExemptions("", COMPANY_NUMBER, requestBody);

        assertEquals(ServiceStatus.SERVER_ERROR, serviceStatus);
        assertEquals(dateString, dateCaptor.getValue());
        verify(repository).findUpdatedExemptions(COMPANY_NUMBER, dateString);
        verify(repository).save(document);
        verifyNoInteractions(exemptionsApiService);
    }

    @Test
    @DisplayName("Test call to upsert company exemptions when chs-kafka-api unavailable returns server error")
    void updateCompanyExemptionsServerError() {
        // given
        when(repository.findUpdatedExemptions(eq(COMPANY_NUMBER), dateCaptor.capture())).thenReturn(Collections.emptyList());
        when(repository.findById(COMPANY_NUMBER)).thenReturn(Optional.empty());
        when(mapper.map(COMPANY_NUMBER, requestBody)).thenReturn(document);
        when(repository.save(any())).thenReturn(document);
        when(exemptionsApiService.invokeChsKafkaApi(any())).thenReturn(ServiceStatus.SERVER_ERROR);

        // when
        ServiceStatus actual = service.upsertCompanyExemptions("", COMPANY_NUMBER, requestBody);

        // then
        assertEquals(ServiceStatus.SERVER_ERROR, actual);
        verify(repository).findById(COMPANY_NUMBER);
        verify(repository).save(document);
        verify(exemptionsApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER, null, false));
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

    @Test
    @DisplayName("Test successful call to delete company exemptions")
    void deleteCompanyExemptions() {
        // given
        document.setData(new CompanyExemptions());
        when(repository.findById(any())).thenReturn(Optional.of(document));
        when(exemptionsApiService.invokeChsKafkaApi(any())).thenReturn(ServiceStatus.SUCCESS);

        // when
        ServiceStatus actual = service.deleteCompanyExemptions("", COMPANY_NUMBER);

        // then
        assertEquals(ServiceStatus.SUCCESS, actual);
        verify(repository).findById(COMPANY_NUMBER);
        verify(exemptionsApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER, document.getData(), true));
        verify(repository).deleteById(COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Test call to delete company exemptions when document not found returns client error")
    void deleteCompanyExemptionsNotFound() {
        // given
        when(repository.findById(any())).thenReturn(Optional.empty());

        // when
        ServiceStatus actual = service.deleteCompanyExemptions("", COMPANY_NUMBER);

        // then
        assertEquals(ServiceStatus.CLIENT_ERROR, actual);
        verify(repository).findById(COMPANY_NUMBER);
        verifyNoInteractions(exemptionsApiService);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("Test call to delete company exemptions when chs-kafka-api unavailable returns server error")
    void deleteCompanyExemptionsServerError() {
        // given
        document.setData(new CompanyExemptions());
        when(repository.findById(any())).thenReturn(Optional.of(document));
        when(exemptionsApiService.invokeChsKafkaApi(any())).thenReturn(ServiceStatus.SERVER_ERROR);

        // when
        ServiceStatus actual = service.deleteCompanyExemptions("", COMPANY_NUMBER);

        // then
        assertEquals(ServiceStatus.SERVER_ERROR, actual);
        verify(repository).findById(COMPANY_NUMBER);
        verify(repository).deleteById(COMPANY_NUMBER);
        verify(exemptionsApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER, document.getData(), true));
    }

    @Test
    @DisplayName("Test call to delete company exemptions, when chs-kafka-api unavailable and throws illegal argument exception, returns server error")
    void deleteCompanyExemptionsServerErrorIllegalArg() {
        // given
        document.setData(new CompanyExemptions());
        when(repository.findById(any())).thenReturn(Optional.of(document));
        when(exemptionsApiService.invokeChsKafkaApi(any())).thenThrow(IllegalArgumentException.class);

        // when
        ServiceStatus actual = service.deleteCompanyExemptions("", COMPANY_NUMBER);

        // then
        assertEquals(ServiceStatus.SERVER_ERROR, actual);
        verify(repository).findById(COMPANY_NUMBER);
        verify(repository).deleteById(COMPANY_NUMBER);
        verify(exemptionsApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER, document.getData(), true));
    }

    @Test
    @DisplayName("Test call to delete company exemptions, when MongoDB unavailable and throws data access exception at findById, returns server error")
    void deleteCompanyExemptionsServerErrorDataAccessExceptionFindById() {
        // given
        when(repository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        ServiceStatus actual = service.deleteCompanyExemptions("", COMPANY_NUMBER);

        // then
        assertEquals(ServiceStatus.SERVER_ERROR, actual);
        verify(repository).findById(COMPANY_NUMBER);
        verifyNoInteractions(exemptionsApiService);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("Test call to delete company exemptions, when MongoDB unavailable and throws data access exception at deleteById, returns server error")
    void deleteCompanyExemptionsServerErrorDataAccessExceptionDeleteById() {
        // given
        document.setData(new CompanyExemptions());
        when(repository.findById(any())).thenReturn(Optional.of(document));
        doThrow(ServiceUnavailableException.class).when(repository).deleteById(any());

        // when
        ServiceStatus actual = service.deleteCompanyExemptions("", COMPANY_NUMBER);

        // then
        assertEquals(ServiceStatus.SERVER_ERROR, actual);
        verify(repository).findById(COMPANY_NUMBER);
        verify(repository).deleteById(COMPANY_NUMBER);
        verifyNoInteractions(exemptionsApiService);
    }
}