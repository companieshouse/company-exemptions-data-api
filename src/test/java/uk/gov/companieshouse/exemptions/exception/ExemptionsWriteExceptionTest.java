package uk.gov.companieshouse.exemptions.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExemptionsWriteExceptionTest {
    @Test
    void checkExceptionCause() {
        final var causeException = new NullPointerException();
        final var exception = new ExemptionsWriteException(causeException);
        assertThat(exception).cause().isEqualTo(causeException);
    }
}