package uk.gov.companieshouse.exemptions.exception;

public class SerDesException extends RuntimeException {

    public SerDesException(String message, Throwable ex) {
        super(message, ex);
    }

}
