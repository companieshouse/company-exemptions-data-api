package uk.gov.companieshouse.exemptions.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions.KindEnum;

class ExemptionsWriteConverterTest {

    private final ExemptionsWriteConverter converter = new ExemptionsWriteConverter(new ObjectMapper());

    @Test
    void convertExemptionsToBasicDBObject() {
        // given
        final var exemptions = new CompanyExemptions();
        exemptions.setKind(KindEnum.EXEMPTIONS);

        // when
        final var actual = converter.convert(exemptions);

        // then
        assertNotNull(actual);
        final var json = actual.toJson();
        assertTrue(json.contains(KindEnum.EXEMPTIONS.getValue()));
    }

    @Test
    void throwRuntimeExceptionWhenNullIsPassedIn() {
        // given

        // when

        // then
        assertThrows(
                RuntimeException.class,
                () -> converter.convert(null));
    }
}
