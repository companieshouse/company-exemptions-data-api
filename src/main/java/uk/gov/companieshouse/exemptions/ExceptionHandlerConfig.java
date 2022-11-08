package uk.gov.companieshouse.exemptions;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import uk.gov.companieshouse.logging.Logger;

@ControllerAdvice
public class ExceptionHandlerConfig {
    private final Logger logger;

    @Autowired
    public ExceptionHandlerConfig(Logger logger) {
        this.logger = logger;
    }

    /**
     * BadRequestException exception handler.
    * Thrown when data is given in the wrong format.
    *
    * @param ex      exception to handle.
    * @param request request.
    * @return error response to return.
    */
    @ExceptionHandler(value = {BadRequestException.class, DateTimeParseException.class,
        HttpMessageNotReadableException.class})
    public ResponseEntity<Object> handleBadRequestException(Exception ex, WebRequest request) {
        logger.error(String.format("Bad request, response code: %s", HttpStatus.BAD_REQUEST), ex);

        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("timestamp", LocalDateTime.now());
        responseBody.put("message", "Bad request.");
        request.setAttribute("javax.servlet.error.exception", ex, 0);
        return new ResponseEntity<>(responseBody, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(value = {ServiceUnavailableException.class, DataAccessException.class})
    public ResponseEntity<Object> handleServiceUnavailableException(Exception ex,
                                                                    WebRequest request) {
        logger.error(String.format("Service unavailable, response code: %s",
                HttpStatus.SERVICE_UNAVAILABLE), ex);

        Map<String, Object> responseBody = new LinkedHashMap<>();
        responseBody.put("timestamp", LocalDateTime.now());
        responseBody.put("message", "Service unavailable.");
        request.setAttribute("javax.servlet.error.exception", ex, 0);
        return new ResponseEntity<>(responseBody, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
