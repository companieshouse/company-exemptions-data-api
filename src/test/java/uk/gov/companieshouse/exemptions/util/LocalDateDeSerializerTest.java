package uk.gov.companieshouse.exemptions.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
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
    void dateShouldDeserialize() throws IOException{
        // given
        String jsonTestString = "{\"date\":{\"$date\": \"2023-01-09T00:00:00Z\"}}";

        // when
        LocalDate returnedDate = deserialize(jsonTestString);

        // then
        assertEquals(LocalDate.of(2023, 1, 9), returnedDate);

    }

    @Test
    void longStringReturnsLong() throws IOException {
        // given
        String jsonTestString = "{\"date\":{\"$date\": {\"$numberLong\":\"-1431388800000\"}}}";

        // when
        LocalDate returnedDate = deserialize(jsonTestString);

        // then
        assertEquals(LocalDate.of(1924, 8, 23), returnedDate);
    }

    @Test
    void nullStringThrowsNullPointerException() {
        // given

        // when
        Executable actual = () -> deserialize(null);

        // then
        assertThrows(NullPointerException.class, actual);
    }

    @Test
    void invalidlStringThrowsBadRequestException() {
        // given
        String jsonTestString = "{\"date\":{\"$date\": \"NotADate\"}}}";

        // when
        Executable actual = () -> deserialize(jsonTestString);

        // then
        assertThrows(BadRequestException.class, actual);
    }

    private LocalDate deserialize(String jsonString) throws IOException {
        JsonParser parser = mapper.getFactory().createParser(jsonString);
        DeserializationContext deserializationContext = mapper.getDeserializationContext();

        parser.nextToken();
        parser.nextToken();
        parser.nextToken();

        return deserializer.deserialize(parser, deserializationContext);
    }
}