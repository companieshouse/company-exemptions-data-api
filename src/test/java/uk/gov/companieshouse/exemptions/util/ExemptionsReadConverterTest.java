package uk.gov.companieshouse.exemptions.util;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    void throwExemptionsReadExceptionWhenNullIsPassedIn() {
        assertThatThrownBy(() -> converter.convert(null))
                .isInstanceOf(ExemptionsReadException.class);
    }
}