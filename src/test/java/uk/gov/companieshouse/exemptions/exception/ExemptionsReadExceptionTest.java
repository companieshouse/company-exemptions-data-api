package uk.gov.companieshouse.exemptions.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExemptionsReadExceptionTest {
    @Test
    void checkExceptionCause() {
        final var causeException = new IllegalAccessException();
        final var exception = new ExemptionsReadException(causeException);
        assertThat(exception).cause().isEqualTo(causeException);
    }
}
