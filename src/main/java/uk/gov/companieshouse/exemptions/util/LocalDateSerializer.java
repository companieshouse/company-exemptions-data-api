package uk.gov.companieshouse.exemptions.util;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateSerializer extends StdSerializer<LocalDate> {

    private static final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final String ISO_DATE_FORMAT = "ISODate(\"%s\")";

    public LocalDateSerializer() {
        super(LocalDate.class);
        // Public constructor required by ValueSerializer.
    }

    /**
     * Serializes a provided LocalDate value.
     * Renders null value as a Json null.
     *
     * @param localDate Value to serialize; can <b>not</b> be null.
     * @param jsonGenerator Generator used to output resulting Json content
     * @param serializerContext Context that can be used to get serializers for
     *   serializing Objects value contains, if any.
     */
    @Override
    public void serialize(
            final LocalDate localDate,
            final JsonGenerator jsonGenerator,
            final SerializationContext serializerContext) {
        if (localDate == null) {
            jsonGenerator.writeNull();
        } else {
            final var format = localDate.atStartOfDay().format(dateTimeFormatter);
            jsonGenerator.writeRawValue(String.format(ISO_DATE_FORMAT,format));
        }
    }
}