package uk.gov.companieshouse.exemptions.util;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.commons.lang3.StringUtils;

public class DateUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSSSS")
            .withZone(UTC);

    private DateUtils() {}

    public static String publishedAtString(final Instant source) {
        return source.atOffset(UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    }

    public static boolean isDeltaStale(final String requestDeltaAt, final String existingDeltaAt) {
        return StringUtils.isNotBlank(existingDeltaAt) && OffsetDateTime.parse(requestDeltaAt, FORMATTER)
                .isBefore(OffsetDateTime.parse(existingDeltaAt, FORMATTER));
    }
}
