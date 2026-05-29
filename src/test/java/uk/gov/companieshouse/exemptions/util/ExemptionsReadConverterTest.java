package uk.gov.companieshouse.exemptions.util;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions.KindEnum;
import uk.gov.companieshouse.exemptions.exception.ExemptionsReadException;

class ExemptionsReadConverterTest {

    private final ExemptionsReadConverter converter = new ExemptionsReadConverter(new ObjectMapper());

    @Test
    void convertDocumentToCompanyExemptions() {
        // given
        Document source = Document.parse("{\"kind\" : \"exemptions\"}");

        // when
        CompanyExemptions actual = converter.convert(source);

        // then
        assertNotNull(actual);
        assertEquals(KindEnum.EXEMPTIONS, actual.getKind());
    }

    @Test
    void throwExemptionsReadExceptionWhenNullIsPassedIn() throws Exception {
        final var mockObjectMapper = mock(ObjectMapper.class);
        when(mockObjectMapper.readValue(anyString(), eq(CompanyExemptions.class)))
                .thenThrow(new NullPointerException("Test error"));

        final var mockedConverter = new ExemptionsReadConverter(mockObjectMapper);

        assertThatThrownBy(() -> mockedConverter.convert(null))
                .isInstanceOf(ExemptionsReadException.class);
    }
}