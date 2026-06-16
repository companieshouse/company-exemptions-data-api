package uk.gov.companieshouse.exemptions.util;

import static uk.gov.companieshouse.exemptions.ExemptionsApplication.APPLICATION_NAME_SPACE;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.deser.std.StdDeserializer;
import uk.gov.companieshouse.exemptions.exception.BadRequestException;
import uk.gov.companieshouse.exemptions.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

public class LocalDateDeSerializer extends StdDeserializer<LocalDate> {
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);

    public LocalDateDeSerializer() {
        super(LocalDate.class);
    }

    @Override
    public LocalDate deserialize(JsonParser jsonParser,
                                 DeserializationContext deserializationContext) {
        try {
            final var dateTimeFormatter = DateTimeFormatter
                    .ofPattern("yyyy-MM-dd'T'HH:mm[:ss[.SSS][.SS][.S]]'Z'");
            final JsonNode jsonNode = jsonParser.readValueAsTree();
            final JsonNode dateNode = jsonNode.get("$date");

            LocalDate parsedDate = null;

            if (dateNode != null && !dateNode.isNull()) {
                if (dateNode.isString() && dateNode.stringValue() != null) {
                    parsedDate = LocalDate.parse(dateNode.stringValue(), dateTimeFormatter);
                } else if (dateNode.has("$numberLong")) {
                    long epochMillis = dateNode.get("$numberLong").asLong();
                    parsedDate = LocalDate.ofInstant(
                            Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
                }
            }

            /** If textValue() returns a value we received a string of
             * format yyyy-MM-dd'T'HH:mm:ss'Z
             * and use dateTimeFormatter to return LocalDate.
             * Otherwise we received a long of milliseconds away
             * from 01/01/1970 and need to return
             * a LocalDate without dateTimeFormatter.
             */
            return parsedDate;
        } catch (Exception exception) {
            LOGGER.error("Deserialization failed.", exception, DataMapHolder.getLogMap());
            throw new BadRequestException(exception.getMessage());
        }
    }
}