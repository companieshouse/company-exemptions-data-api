package uk.gov.companieshouse.exemptions.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExemptionsWriteExceptionTest {
    @Test
    void checkExceptionCause() {
        final var causeException = new NullPointerException();
        final var exception = new ExemptionsWriteException(causeException);
        assertThat(exception).cause().isEqualTo(causeException);
    }
}
