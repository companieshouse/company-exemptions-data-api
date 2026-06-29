package uk.gov.companieshouse.exemptions.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import uk.gov.companieshouse.exemptions.exception.BadRequestException;

class LocalDateDeSerializerTest {

    private final LocalDateDeSerializer deserializer = new LocalDateDeSerializer();
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void dateShouldDeserialize() throws IOException {
        // given
        final var jsonTestString = "{\"date\":{\"$date\": \"2023-01-09T00:00:00Z\"}}";

        // when
        final var returnedDate = deserialize(jsonTestString);

        // then
        assertEquals(LocalDate.of(2023, 1, 9), returnedDate);

    }

    @Test
    void longStringReturnsLong() throws IOException {
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


        final Executable actual = () -> deserialize(null);


        // then
        assertThrows(NullPointerException.class, actual);
    }

    @Test
    void invalidlStringThrowsBadRequestException() {
        // given
        final var jsonTestString = "{\"date\":{\"$date\": \"NotADate\"}}}";

        // when/then
        assertThrows(BadRequestException.class,
                () -> deserialize(jsonTestString));
    }

    private LocalDate deserialize(String jsonString) throws IOException {
        final var parser = mapper.getFactory().createParser(jsonString);
        final var deserializationContext = mapper.getDeserializationContext();

        parser.nextToken();
        parser.nextToken();
        parser.nextToken();

        return deserializer.deserialize(parser, deserializationContext);
    }
}
