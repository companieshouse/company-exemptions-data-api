package uk.gov.companieshouse.exemptions.util;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.apache.commons.lang.StringUtils;

public class DateUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS")
            .withZone(UTC);

    public static String publishedAtString(final Instant source) {
        return source.atOffset(UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss"));
    }

    public static boolean isDeltaStale(final String requestDeltaAt, final String existingDeltaAt) {
        return StringUtils.isNotBlank(existingDeltaAt) && OffsetDateTime.parse(requestDeltaAt, FORMATTER)
                .isBefore(OffsetDateTime.parse(existingDeltaAt, FORMATTER));
    }
}
