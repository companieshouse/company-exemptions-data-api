package uk.gov.companieshouse.exemptions.util;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions;
import uk.gov.companieshouse.api.exemptions.CompanyExemptions.KindEnum;
import uk.gov.companieshouse.exemptions.exception.ExemptionsWriteException;

class ExemptionsWriteConverterTest {

    private final ExemptionsWriteConverter converter = new ExemptionsWriteConverter(new ObjectMapper());

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
    void throwExemptionsWriteExceptionWhenNullIsPassedIn() {
        assertThatThrownBy(() -> converter.convert(null))
                .isInstanceOf(ExemptionsWriteException.class);
    }
}