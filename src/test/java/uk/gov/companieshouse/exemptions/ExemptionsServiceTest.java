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
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExemptionsServiceTest {

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
    void insertCompanyExemptions() {
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
    void updateCompanyExemptions() {
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

    @Test
    @DisplayName("Test should not update exemptions record from out of date delta")
    void outOfDateDelta() {
        when(repository.findUpdatedExemptions(eq(COMPANY_NUMBER), dateCaptor.capture())).thenReturn(Collections.singletonList(new CompanyExemptionsDocument()));

        service.upsertCompanyExemptions("", COMPANY_NUMBER, requestBody);

        verify(repository).findUpdatedExemptions(COMPANY_NUMBER, dateString);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(exemptionsApiService);
        assertEquals(dateString, dateCaptor.getValue());
    }
}