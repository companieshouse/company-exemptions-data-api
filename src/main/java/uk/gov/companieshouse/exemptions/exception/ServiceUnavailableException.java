package uk.gov.companieshouse.exemptions.exception;

import org.springframework.dao.DataAccessException;

public class ServiceUnavailableException extends DataAccessException {
    public ServiceUnavailableException(String message) {
        super(message);
    }
}