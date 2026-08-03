package uk.gov.companieshouse.exemptions.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class LocalDateSerializer extends JsonSerializer<LocalDate> {


    /**
     * Serializes a provided LocalDate value.
     * Renders null value as a Json null.
     *
     * @param localDate Value to serialize; can <b>not</b> be null.
     * @param jsonGenerator Generator used to output resulting Json content
     * @param serializerProvider .abstract serializer provider
     */
    @Override
    public void serialize(LocalDate localDate, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {
        if (localDate == null) {
            jsonGenerator.writeNull();
        } else {
            DateTimeFormatter dateTimeFormatter =
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            String format = localDate.atStartOfDay().format(dateTimeFormatter);
            jsonGenerator.writeRawValue("ISODate(\"" + format + "\")");
        }
    }
}
