package uk.gov.companieshouse.exemptions.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions.KindEnum;
import uk.gov.companieshouse.api.exemptions.ExemptionItem;
import uk.gov.companieshouse.api.exemptions.Exemptions;
import uk.gov.companieshouse.api.exemptions.InternalData;
import uk.gov.companieshouse.api.exemptions.InternalExemptionsApi;
import uk.gov.companieshouse.api.exemptions.PscExemptAsTradingOnRegulatedMarketItem;
import uk.gov.companieshouse.api.exemptions.PscExemptAsTradingOnRegulatedMarketItem.ExemptionTypeEnum;
import uk.gov.companieshouse.exemptions.exception.BadRequestException;
import uk.gov.companieshouse.exemptions.exception.ConflictException;
import uk.gov.companieshouse.exemptions.exception.NotFoundException;
import uk.gov.companieshouse.exemptions.exception.ServiceUnavailableException;
import uk.gov.companieshouse.exemptions.model.CompanyExemptionsDocument;
import uk.gov.companieshouse.exemptions.model.Created;
import uk.gov.companieshouse.exemptions.model.ResourceChangedRequest;
import uk.gov.companieshouse.exemptions.model.Updated;
import uk.gov.companieshouse.exemptions.util.ExemptionsMapper;
import uk.gov.companieshouse.logging.Logger;

@ExtendWith(MockitoExtension.class)
class ExemptionsServiceImplTest {

    private static final String COMPANY_NUMBER = "12345678";
    private static final String DELTA_AT = "20240219123045999999";

    @Mock
    private ExemptionsRepository repository;

    @Mock
    private ExemptionsMapper mapper;

    @Mock
    private ExemptionsApiService exemptionsApiService;

    @Mock
    private Logger logger;

    @InjectMocks
    private ExemptionsServiceImpl service;

    private InternalExemptionsApi requestBody;
    private CompanyExemptionsDocument exemptionsDocument;
    private CompanyExemptions companyExemptionsData;
    private CompanyExemptionsDocument existingDocument;
    private Optional<CompanyExemptionsDocument> document;

    @BeforeEach
    public void setUp() {
        OffsetDateTime date = OffsetDateTime.of(2023,1, 1,12,
                0,0,0, ZoneOffset.UTC);
        requestBody = new InternalExemptionsApi();
        InternalData internal = new InternalData();
        internal.setDeltaAt(date);
        requestBody.setInternalData(internal);

        exemptionsDocument = getExemptionsDocument(date);

        existingDocument = new CompanyExemptionsDocument();
        existingDocument.setDeltaAt("20221012091025774312");
        document = Optional.of(exemptionsDocument);
    }

    @Test
    @DisplayName("Upsert successful insert and call to chs kafka api")
    void insertCompanyExemptions() {
        // given
        when(repository.findById(COMPANY_NUMBER)).thenReturn(Optional.empty());
        when(mapper.map(COMPANY_NUMBER, requestBody)).thenReturn(exemptionsDocument);

        // when
        service.upsertCompanyExemptions("", COMPANY_NUMBER, requestBody);

        // then
        assertNotNull(exemptionsDocument.getCreated().getAt());
        verify(exemptionsApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER, null, false));
        verify(repository).save(exemptionsDocument);
    }

    @Test
    @DisplayName("Upsert successful update and call to chs kafka api")
    void updateCompanyExemptions() {
        // given
        existingDocument.setCreated(new Created().setAt(LocalDateTime.of(2022, 11, 2, 15, 55)));
        when(repository.findById(COMPANY_NUMBER)).thenReturn(Optional.of(existingDocument));
        when(mapper.map(COMPANY_NUMBER, requestBody)).thenReturn(exemptionsDocument);

        // when
        service.upsertCompanyExemptions("", COMPANY_NUMBER, requestBody);

        // then
        assertEquals(LocalDateTime.of(2022, 11, 2, 15, 55), exemptionsDocument.getCreated().getAt());
        verify(exemptionsApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER, null, false));
        verify(repository).save(exemptionsDocument);
    }

    @Test
    @DisplayName("Upsert errors with conflict for exemptions record with out of date delta")
    void outOfDateDelta() {
        // given
        requestBody.getInternalData().setDeltaAt(OffsetDateTime.of(2018,1,1,0,0,0,0,ZoneOffset.UTC));
        when(repository.findById(any())).thenReturn(Optional.of(existingDocument));

        // when
        Executable actual = () -> service.upsertCompanyExemptions("", COMPANY_NUMBER, requestBody);

        // then
        assertThrows(ConflictException.class, actual);
        verifyNoInteractions(exemptionsApiService);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("Upsert should update exemptions if existing document has no delta_at field")
    void updateExemptionsDeltaAtAbsent() {
        // given
        existingDocument.setDeltaAt(null);
        existingDocument.setCreated(new Created().setAt(LocalDateTime.of(2022, 11, 2, 15, 55)));
        when(repository.findById(COMPANY_NUMBER)).thenReturn(Optional.of(existingDocument));
        when(mapper.map(COMPANY_NUMBER, requestBody)).thenReturn(exemptionsDocument);

        // when
        service.upsertCompanyExemptions("", COMPANY_NUMBER, requestBody);

        // then
        assertEquals(LocalDateTime.of(2022, 11, 2, 15, 55), exemptionsDocument.getCreated().getAt());
        verify(exemptionsApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER, null, false));
        verify(repository).save(exemptionsDocument);
    }

    @Test
    @DisplayName("Upsert errors with service unavailable when save to repository throws data access exception on findById")
    void saveToRepositoryFindError() {
        // given
        when(repository.findById(COMPANY_NUMBER)).thenThrow(ServiceUnavailableException.class);

        // when
        Executable actual = () -> service.upsertCompanyExemptions("", COMPANY_NUMBER, requestBody);

        // then
        assertThrows(ServiceUnavailableException.class, actual);
        verifyNoInteractions(exemptionsApiService);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("Upsert errors with service unavailable when save to repository throws data access exception")
    void saveToRepositoryError() {
        // given
        when(repository.findById(COMPANY_NUMBER)).thenReturn(Optional.empty());
        when(mapper.map(COMPANY_NUMBER, requestBody)).thenReturn(exemptionsDocument);
        when(repository.save(exemptionsDocument)).thenThrow(ServiceUnavailableException.class);

        // when
        Executable actual = () -> service.upsertCompanyExemptions("", COMPANY_NUMBER, requestBody);

        // then
        assertThrows(ServiceUnavailableException.class, actual);
        verify(repository).save(exemptionsDocument);
        verifyNoInteractions(exemptionsApiService);
    }

    @Test
    @DisplayName("Upsert errors with service unavailable when chs-kafka-api unavailable")
    void updateCompanyExemptionsServerError() {
        // given
        when(repository.findById(COMPANY_NUMBER)).thenReturn(Optional.empty());
        when(mapper.map(COMPANY_NUMBER, requestBody)).thenReturn(exemptionsDocument);
        doThrow(ServiceUnavailableException.class).when(exemptionsApiService).invokeChsKafkaApi(any());

        // when
        Executable actual = () -> service.upsertCompanyExemptions("", COMPANY_NUMBER, requestBody);

        // then
        assertThrows(ServiceUnavailableException.class, actual);
        verify(repository).findById(COMPANY_NUMBER);
        verify(repository).save(exemptionsDocument);
        verify(exemptionsApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER, null, false));
    }

    @Test
    @DisplayName("Upsert errors with bad request when chs-kafka-api throws illegal argument exception")
    void updateCompanyExemptionsIllegalArg() {
        // given
        when(repository.findById(COMPANY_NUMBER)).thenReturn(Optional.empty());
        when(mapper.map(COMPANY_NUMBER, requestBody)).thenReturn(exemptionsDocument);
        doThrow(IllegalArgumentException.class).when(exemptionsApiService).invokeChsKafkaApi(any());

        // when
        Executable actual = () -> service.upsertCompanyExemptions("", COMPANY_NUMBER, requestBody);

        // then
        assertThrows(BadRequestException.class, actual);
        verify(repository).findById(COMPANY_NUMBER);
        verify(repository).save(exemptionsDocument);
        verify(exemptionsApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER, null, false));
    }

    @Test
    @DisplayName("Test successful call to get company exemptions")
    void getCompanyExemptions() {
        // given
        when(repository.findById(any())).thenReturn(Optional.of(exemptionsDocument));

        // when
        CompanyExemptions actual = service.getCompanyExemptions(COMPANY_NUMBER);

        // then
        assertNotNull(actual);
        assertEquals(getExemptionsData(), actual);
        verify(repository).findById(COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Test call to get company exemptions returns not found")
    void getCompanyExemptionsNotFound() {
        // given
        when(repository.findById(any())).thenReturn(Optional.empty());

        // when
        Executable actual = () -> service.getCompanyExemptions(COMPANY_NUMBER);

        // then
        assertThrows(NotFoundException.class, actual);
        verify(repository).findById(COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Test call to get company exemptions throws service unavailable")
    void getCompanyExemptionsDataAccessException() {
        // given
        when(repository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable executable = () -> service.getCompanyExemptions(COMPANY_NUMBER);

        // then
        Exception exception = assertThrows(ServiceUnavailableException.class, executable);
        assertEquals("Data access exception thrown when calling Mongo Repository", exception.getMessage());
        verify(repository).findById(COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Test successful call to delete company exemptions")
    void deleteCompanyExemptions() {
        // given
        when(repository.findById(any())).thenReturn(Optional.of(exemptionsDocument));

        // when
        service.deleteCompanyExemptions("", COMPANY_NUMBER, DELTA_AT);

        // then
        verify(repository).findById(COMPANY_NUMBER);
        verify(exemptionsApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER,
                document, true));
        verify(repository).deleteById(COMPANY_NUMBER);
    }

    @Test
    @DisplayName("Delete errors with bad request when deltaAt is missing")
    void deleteCompanyExemptionsBadRequest() {
        // given

        // when
        Executable actual = () -> service.deleteCompanyExemptions("", COMPANY_NUMBER, null);

        // then
        assertThrows(BadRequestException.class, actual);
        verifyNoInteractions(repository);
        verifyNoInteractions(exemptionsApiService);
    }

    @Test
    @DisplayName("Delete errors with conflict when delta is stale")
    void deleteCompanyExemptionsStaleDelta() {
        existingDocument.setDeltaAt(DELTA_AT);

        // given
        when(repository.findById(any())).thenReturn(Optional.of(existingDocument));

        // when
        Executable actual = () -> service
                .deleteCompanyExemptions("", COMPANY_NUMBER, "20230219123045999999");

        // then
        assertThrows(ConflictException.class, actual);
        verify(repository).findById(COMPANY_NUMBER);
        verifyNoInteractions(exemptionsApiService);
    }

    @Test
    @DisplayName("Test call to delete company exemptions is successful when document not found")
    void deleteCompanyExemptionsNotFound() {
        // given
        when(repository.findById(any())).thenReturn(Optional.empty());

        // when
        service.deleteCompanyExemptions("", COMPANY_NUMBER, DELTA_AT);

        // then
        verify(repository).findById(COMPANY_NUMBER);
        verify(exemptionsApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER,
                Optional.empty(), true));
    }

    @Test
    @DisplayName("Delete errors with service unavailable when chs-kafka-api unavailable")
    void deleteCompanyExemptionsServerError() {
        // given
        exemptionsDocument.setData(new CompanyExemptions());
        when(repository.findById(any())).thenReturn(Optional.of(exemptionsDocument));
        doThrow(ServiceUnavailableException.class).when(exemptionsApiService).invokeChsKafkaApi(any());

        // when
        Executable actual = () -> service.deleteCompanyExemptions("", COMPANY_NUMBER, DELTA_AT);

        // then
        assertThrows(ServiceUnavailableException.class, actual);
        verify(repository).findById(COMPANY_NUMBER);
        verify(repository).deleteById(COMPANY_NUMBER);
        verify(exemptionsApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER, document, true));
    }

    @Test
    @DisplayName("Delete errors with bad request when chs-kafka-api throws illegal argument exception")
    void deleteCompanyExemptionsServerErrorIllegalArg() {
        // given
        exemptionsDocument.setData(new CompanyExemptions());
        when(repository.findById(any())).thenReturn(Optional.of(exemptionsDocument));
        doThrow(IllegalArgumentException.class).when(exemptionsApiService).invokeChsKafkaApi(any());

        // when
        Executable actual = () -> service.deleteCompanyExemptions("", COMPANY_NUMBER, DELTA_AT);

        // then
        assertThrows(BadRequestException.class, actual);
        verify(repository).findById(COMPANY_NUMBER);
        verify(repository).deleteById(COMPANY_NUMBER);
        verify(exemptionsApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER, document, true));
    }

    @Test
    @DisplayName("Delete errors with service unavailable repository throws data access exception on findById")
    void deleteCompanyExemptionsServerErrorDataAccessExceptionFindById() {
        // given
        when(repository.findById(any())).thenThrow(ServiceUnavailableException.class);

        // when
        Executable actual = () -> service.deleteCompanyExemptions("", COMPANY_NUMBER, DELTA_AT);

        // then
        assertThrows(ServiceUnavailableException.class, actual);
        verify(repository).findById(COMPANY_NUMBER);
        verifyNoInteractions(exemptionsApiService);
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("Delete errors with service unavailable repository throws data access exception on deleteById")
    void deleteCompanyExemptionsServerErrorDataAccessExceptionDeleteById() {
        // given
        exemptionsDocument.setData(new CompanyExemptions());
        when(repository.findById(any())).thenReturn(Optional.of(exemptionsDocument));
        doThrow(ServiceUnavailableException.class).when(repository).deleteById(any());

        // when
        Executable actual = () -> service.deleteCompanyExemptions("", COMPANY_NUMBER, DELTA_AT);

        // then
        assertThrows(ServiceUnavailableException.class, actual);
        verify(repository).findById(COMPANY_NUMBER);
        verify(repository).deleteById(COMPANY_NUMBER);
        verifyNoInteractions(exemptionsApiService);
    }

    private CompanyExemptionsDocument getExemptionsDocument(OffsetDateTime date) {
        exemptionsDocument = new CompanyExemptionsDocument();
        exemptionsDocument.setUpdated(new Updated(LocalDateTime.now()));
        final DateTimeFormatter dateTimeFormatter =
                DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS")
                        .withZone(ZoneId.of("Z"));
        String dateString = date.format(dateTimeFormatter);
        exemptionsDocument.setDeltaAt(dateString);
        exemptionsDocument.setData(getExemptionsData());
        return exemptionsDocument;
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