package uk.gov.companieshouse.exemptions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.exemptions.InternalData;
import uk.gov.companieshouse.api.exemptions.InternalExemptionsApi;
import uk.gov.companieshouse.logging.Logger;


import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExemptionsServiceTest {

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
    private ExemptionsService service;

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
    public void insertCompanyExemptions() {
        when(repository.findUpdatedExemptions(eq(COMPANY_NUMBER), dateCaptor.capture())).thenReturn(Collections.emptyList());
        when(repository.findById(COMPANY_NUMBER)).thenReturn(Optional.empty());
        when(mapper.map(COMPANY_NUMBER, requestBody)).thenReturn(document);

        service.upsertCompanyExemptions("", COMPANY_NUMBER, requestBody);

        verify(repository).save(document);
        verify(exemptionsApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER,
                 null, false));
        assertEquals(dateString, dateCaptor.getValue());
        assertNotNull(document.getCreated().getAt());
    }

    @Test
    @DisplayName("Test successful update and call to chs kafka api")
    public void updateCompanyExemptions() {
        document.setCreated(new Created().setAt(LocalDateTime.of(2022, 11, 2, 15, 55)));
        when(repository.findUpdatedExemptions(eq(COMPANY_NUMBER), dateCaptor.capture())).thenReturn(Collections.emptyList());
        when(repository.findById(COMPANY_NUMBER)).thenReturn(Optional.of(document));
        when(mapper.map(COMPANY_NUMBER, requestBody)).thenReturn(document);

        service.upsertCompanyExemptions("", COMPANY_NUMBER, requestBody);

        verify(repository).save(document);
        verify(exemptionsApiService).invokeChsKafkaApi(new ResourceChangedRequest("", COMPANY_NUMBER,
                null, false));
        assertEquals(dateString, dateCaptor.getValue());
        assertEquals(LocalDateTime.of(2022, 11, 2, 15, 55), document.getCreated().getAt());
    }
//
//    @Test
//    public void processNaturalDisqualificationDoesNotSavesDisqualificationWhenUpdateAlreadyMade() {
//
//        List<DisqualificationDocument> documents = new ArrayList<>();
//        documents.add(new DisqualificationDocument());
//        when(repository.findUpdatedDisqualification(eq(COMPANY_NUMBER), dateCaptor.capture())).thenReturn(documents);
//
//        service.processNaturalDisqualification("", COMPANY_NUMBER, requestBody);
//
//        verify(repository, times(0)).save(document);
//        verify(exemptionsApiService, times(0)).invokeChsKafkaApi(new ResourceChangedRequest("", "officerId",
//                DisqualificationResourceType.NATURAL, null, false));
//        assertEquals(dateString, dateCaptor.getValue());
//    }
//
//    @Test
//    public void processCorporateDisqualificationSavesDisqualification() {
//        when(repository.findUpdatedDisqualification(eq(COMPANY_NUMBER), dateCaptor.capture())).thenReturn(new ArrayList<>());
//        when(repository.findById(COMPANY_NUMBER)).thenReturn(Optional.empty());
//        when(mapper.transformCorporateDisqualifiedOfficer(COMPANY_NUMBER, corpRequest)).thenReturn(document);
//
//        service.processCorporateDisqualification("", COMPANY_NUMBER, corpRequest);
//
//        verify(repository).save(document);
//        verify(exemptionsApiService).invokeChsKafkaApi(new ResourceChangedRequest("", "officerId",
//                DisqualificationResourceType.CORPORATE, null, false));
//        assertEquals(dateString, dateCaptor.getValue());
//        assertNotNull(document.getCreated().getAt());
//    }
//
//    @Test
//    void correctOfficerIdIsGivenReturnsDisqualification() {
//        NaturalDisqualificationDocument naturalDocument = new NaturalDisqualificationDocument();
//        naturalDocument.setData(new NaturalDisqualificationApi());
//        naturalDocument.setId(COMPANY_NUMBER);
//        when(naturalRepository.findById(COMPANY_NUMBER)).thenReturn(Optional.of(naturalDocument));
//
//        NaturalDisqualificationDocument disqualification = service.retrieveNaturalDisqualification(COMPANY_NUMBER);
//
//        assertNotNull(disqualification);
//        verify(naturalRepository, times(1)).findById(any());
//    }
//
//    @Test
//    void correctOfficerIdIsGivenReturnsCorporateDisqualification() {
//        CorporateDisqualificationDocument corporateDocument = new CorporateDisqualificationDocument();
//        corporateDocument.setData(new CorporateDisqualificationApi());
//        corporateDocument.setCorporateOfficer(true);
//        corporateDocument.setId(COMPANY_NUMBER);
//        when(corporateRepository.findById(COMPANY_NUMBER)).thenReturn(Optional.of(corporateDocument));
//
//        CorporateDisqualificationDocument disqualification = service.retrieveCorporateDisqualification(COMPANY_NUMBER);
//
//        assertNotNull(disqualification);
//        verify(corporateRepository, times(1)).findById(any());
//    }
//
//    @Test
//    void throwsExceptionWhenCorporateIndIsTrueButNaturalOfficerCalled() {
//        NaturalDisqualificationDocument naturalDocument = new NaturalDisqualificationDocument();
//        naturalDocument.setData(new NaturalDisqualificationApi());
//        naturalDocument.setCorporateOfficer(true);
//        naturalDocument.setId(COMPANY_NUMBER);
//        when(naturalRepository.findById(COMPANY_NUMBER)).thenReturn(Optional.of(naturalDocument));
//
//        assertThrows(RuntimeException.class, () -> service.retrieveNaturalDisqualification
//                (COMPANY_NUMBER));
//        verify(naturalRepository, times(1)).findById(any());
//
//    }
//
//    @Test
//    void throwsExceptionWhenCorporateIndIsFalseButCorporateOfficerCalled() {
//        CorporateDisqualificationDocument corporateDocument = new CorporateDisqualificationDocument();
//        corporateDocument.setData(new CorporateDisqualificationApi());
//        corporateDocument.setCorporateOfficer(false);
//        corporateDocument.setId(COMPANY_NUMBER);
//        when(corporateRepository.findById(COMPANY_NUMBER)).thenReturn(Optional.of(corporateDocument));
//
//        assertThrows(RuntimeException.class, () -> service.retrieveCorporateDisqualification
//                (COMPANY_NUMBER));
//        verify(corporateRepository, times(1)).findById(any());
//
//    }
//
//    @Test
//    void throwsExceptionWhenInvalidIdGiven() {
//
//        assertThrows(RuntimeException.class, () -> service.retrieveNaturalDisqualification
//                ("asdfasdfasdf"));
//        verify(naturalRepository, times(1)).findById(any());
//
//    }
//
//    @Test
//    public void deleteNaturalDisqualificationDeletesDisqualification() {
//        when(repository.findById(COMPANY_NUMBER)).thenReturn(Optional.of(document));
//        NaturalDisqualificationDocument doc = new NaturalDisqualificationDocument();
//        doc.setData(new NaturalDisqualificationApi());
//        when(naturalRepository.findById(COMPANY_NUMBER)).thenReturn(Optional.of(doc));
//
//        service.deleteDisqualification("", COMPANY_NUMBER);
//
//        verify(repository).delete(doc);
//        verify(exemptionsApiService).invokeChsKafkaApi(new ResourceChangedRequest("", "officerId",
//                DisqualificationResourceType.NATURAL, doc.getData(), true));
//        assertEquals(doc.getData().getKind().toString(), "natural-disqualification");
//    }
//
//    @Test
//    public void deleteCorporateDisqualificationDeletesDisqualification() {
//        document.setCorporateOfficer(true);
//        when(repository.findById(COMPANY_NUMBER)).thenReturn(Optional.of(document));
//        CorporateDisqualificationDocument doc = new CorporateDisqualificationDocument();
//        doc.setCorporateOfficer(true);
//        doc.setData(new CorporateDisqualificationApi());
//        when(corporateRepository.findById(COMPANY_NUMBER)).thenReturn(Optional.of(doc));
//
//        service.deleteDisqualification("", COMPANY_NUMBER);
//
//        verify(repository).delete(doc);
//        verify(exemptionsApiService).invokeChsKafkaApi(new ResourceChangedRequest("", "officerId",
//                DisqualificationResourceType.CORPORATE, doc.getData(), true));
//        assertEquals(doc.getData().getKind().toString(), "corporate-disqualification");
//    }
//
//    @Test
//    public void deleteCorporateDisqualificationThrowsErrorWhenNatural() {
//        when(repository.findById(COMPANY_NUMBER)).thenReturn(Optional.of(document));
//
//        assertThrows(IllegalArgumentException.class, () -> service.deleteDisqualification
//                ("", COMPANY_NUMBER));
//        verify(naturalRepository, times(1)).findById(any());
//
//    }
//
//    @Test
//    public void deleteNaturalDisqualificationThrowsErrorWhenCorporate() {
//        document.setCorporateOfficer(true);
//        when(repository.findById(COMPANY_NUMBER)).thenReturn(Optional.of(document));
//
//        assertThrows(IllegalArgumentException.class, () -> service.deleteDisqualification
//                ("", COMPANY_NUMBER));
//        verify(corporateRepository, times(1)).findById(any());
//    }
}