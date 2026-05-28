package uk.gov.companieshouse.exemptions.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExemptionsReadExceptionTest {
    @Test
    void checkExceptionCause() {
        final var causeException = new IllegalAccessException();
        final var exception = new ExemptionsReadException(causeException);
        assertThat(exception).cause().isEqualTo(causeException);
    }
}