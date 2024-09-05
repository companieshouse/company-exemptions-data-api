package uk.gov.companieshouse.exemptions.util;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class DateUtils {
    public static String publishedAtString(final Instant source) {
        return source.atOffset(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss"));

    }
}
