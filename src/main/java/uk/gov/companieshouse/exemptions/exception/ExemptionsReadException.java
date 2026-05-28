package uk.gov.companieshouse.exemptions.exception;

public class ExemptionsReadException extends RuntimeException {

    public ExemptionsReadException(Exception exception) {
        super(exception);
    }

}