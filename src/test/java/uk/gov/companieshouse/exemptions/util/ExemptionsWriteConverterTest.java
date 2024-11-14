package uk.gov.companieshouse.exemptions.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions.KindEnum;

@ExtendWith(MockitoExtension.class)
class ExemptionsWriteConverterTest {

    private ExemptionsWriteConverter converter;

    @BeforeEach
    void setUp() {
        converter = new ExemptionsWriteConverter(new ObjectMapper());
    }

    @Test
    void convertExemptionsToBasicDBObject() {
        // given
        CompanyExemptions exemptions = new CompanyExemptions();
        exemptions.setKind(KindEnum.EXEMPTIONS);

        // when
        BasicDBObject actual = converter.convert(exemptions);

        // then
        assertNotNull(actual);
        String json = actual.toJson();
        assertTrue(json.contains(KindEnum.EXEMPTIONS.getValue()));
    }

    @Test
    void throwRuntimeExceptionWhenNullIsPassedIn() {
        // given

        // when
        Executable actual = () -> converter.convert(null);

        // then
        assertThrows(RuntimeException.class, actual);
    }
}
