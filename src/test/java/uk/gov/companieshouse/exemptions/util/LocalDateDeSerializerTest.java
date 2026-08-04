package uk.gov.companieshouse.exemptions.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.companieshouse.exemptions.exception.BadRequestException;

class LocalDateDeSerializerTest {

    private final LocalDateDeSerializer deserializer = new LocalDateDeSerializer();
    private final JsonMapper mapper = new JsonMapper();

    @Test
    void dateShouldDeserialize() {
        // given
        final var jsonTestString = "{\"date\":{\"$date\": \"2023-01-09T00:00:00Z\"}}";

        // when
        final var returnedDate = deserialize(jsonTestString);

        // then
        assertEquals(LocalDate.of(2023, 1, 9), returnedDate);

    }

    @Test
    void longStringReturnsLong() {
        // given
        final var jsonTestString = "{\"date\":{\"$date\": {\"$numberLong\":\"-1431388800000\"}}}";

        // when
        final var returnedDate = deserialize(jsonTestString);

        // then
        assertEquals(LocalDate.of(1924, 8, 23), returnedDate);
    }

    @Test
    void nullStringThrowsIllegalArgumentException() {
        // given

        // when/ then
        assertThrows(IllegalArgumentException.class, () -> deserialize(null));

    }

    @Test
    void invalidlStringThrowsBadRequestException() {
        // given
        final var jsonTestString = "{\"date\":{\"$date\": \"NotADate\"}}}";

        // when/then
        assertThrows(BadRequestException.class,
                () -> deserialize(jsonTestString));
    }

    private LocalDate deserialize(String jsonString) {
        final var parser = mapper.createParser(jsonString);
        DeserializationContext deserializationContext = null;

        try {
            parser.nextToken();
            parser.nextToken();
            parser.nextToken();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return deserializer.deserialize(parser, deserializationContext);
    }
}