package uk.gov.companieshouse.exemptions.controller;

import static uk.gov.companieshouse.exemptions.ExemptionsApplication.APPLICATION_NAME_SPACE;

import java.time.format.DateTimeParseException;
import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.companieshouse.exemptions.exception.BadRequestException;
import uk.gov.companieshouse.exemptions.exception.ConflictException;
import uk.gov.companieshouse.exemptions.exception.NotFoundException;
import uk.gov.companieshouse.exemptions.exception.ServiceUnavailableException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@ControllerAdvice
public class ControllerExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Void> handleNotFound(NotFoundException ex) {
        LOGGER.error("Unable to locate company exemptions", ex);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .build();
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Void> handleConflict() {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .build();
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Void> handleServiceUnavailable(ServiceUnavailableException ex) {
        LOGGER.info("Recoverable exception: %s".formatted(Arrays.toString(ex.getStackTrace())));
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .build();
    }

    @ExceptionHandler(value = {BadRequestException.class, DateTimeParseException.class,
            HttpMessageNotReadableException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<Void> handleRequestAndParseError(Exception ex) {
        LOGGER.error("Invalid request body", ex);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Void> handleUnknownException(Exception ex) {
        LOGGER.error(ex.getClass().getName(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .build();
    }
}
