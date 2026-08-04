package uk.gov.companieshouse.exemptions.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions.KindEnum;

class ExemptionsReadConverterTest {

    private final ExemptionsReadConverter converter = new ExemptionsReadConverter(new JsonMapper());

    @Test
    void convertDocumentToCompanyExemptions() {
        // given
        final var source = Document.parse("{\"kind\" : \"exemptions\"}");

        // when
        final var actual = converter.convert(source);

        // then
        assertNotNull(actual);
        assertEquals(KindEnum.EXEMPTIONS, actual.getKind());
    }

    @Test
    void throwRuntimeExceptionWhenNullIsPassedIn() {
        // given

        // when

        // then
        assertThrows(RuntimeException.class,
                () -> converter.convert(null));
    }
}