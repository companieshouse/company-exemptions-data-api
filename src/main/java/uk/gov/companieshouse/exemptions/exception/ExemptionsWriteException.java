package uk.gov.companieshouse.exemptions.exception;

public class ExemptionsWriteException extends RuntimeException {

    public ExemptionsWriteException(Exception exception) {
        super(exception);
    }

}
