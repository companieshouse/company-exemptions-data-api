package uk.gov.companieshouse.exemptions.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonGenerator;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocalDateSerializerTest {

    private final LocalDateSerializer serializer = new LocalDateSerializer();

    @Mock
    private JsonGenerator generator;

    @Captor
    private ArgumentCaptor<String> dateString;

    @Test
    void dateShouldSerialize() throws Exception {
        LocalDate date = LocalDate.of(2020, 1, 1);

        serializer.serialize(date, generator, null);

        verify(generator).writeRawValue(dateString.capture());
        assertEquals("ISODate(\"2020-01-01T00:00:00.000Z\")", dateString.getValue());
    }

    @Test
    void serializeWhenDataIsNull() throws Exception {
        serializer.serialize(null, generator, null);

        verify(generator).writeNull();
    }
}