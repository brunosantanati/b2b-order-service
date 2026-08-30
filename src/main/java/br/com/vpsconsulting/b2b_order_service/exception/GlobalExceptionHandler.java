package br.com.vpsconsulting.b2b_order_service.exception;

import org.apache.commons.text.WordUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException ex) {
        String error = WordUtils.capitalizeFully(HttpStatus.NOT_FOUND.name().replace('_', ' '));
        return getResponseEntity(HttpStatus.NOT_FOUND, error, ex.getMessage());
    }

    @ExceptionHandler(InsufficientCreditException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientCredit(InsufficientCreditException ex) {
        String error = WordUtils.capitalizeFully(HttpStatus.UNPROCESSABLE_CONTENT.name().replace('_', ' '));
        return getResponseEntity(HttpStatus.UNPROCESSABLE_CONTENT, error, ex.getMessage());
    }

    @ExceptionHandler(CnpjAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleCnpjAlreadyExists(CnpjAlreadyExistsException ex) {
        String error = WordUtils.capitalizeFully(HttpStatus.CONFLICT.name().replace('_', ' '));
        return getResponseEntity(HttpStatus.CONFLICT, error, ex.getMessage());
    }

    @ExceptionHandler(OrderAlreadyCancelledException.class)
    public ResponseEntity<Map<String, Object>> handleOrderAlreadyCancelled(OrderAlreadyCancelledException ex) {
        String error = WordUtils.capitalizeFully(HttpStatus.CONFLICT.name().replace('_', ' '));
        return getResponseEntity(HttpStatus.CONFLICT, error, ex.getMessage());
    }

    @ExceptionHandler(EventSerializationException.class)
    public ResponseEntity<Map<String, Object>> handleOrderAlreadyCancelled(EventSerializationException ex) {
        String error = WordUtils.capitalizeFully(HttpStatus.INTERNAL_SERVER_ERROR.name().replace('_', ' '));
        return getResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, error, ex.getMessage());
    }

    private static ResponseEntity<Map<String, Object>> getResponseEntity(
            HttpStatus httpStatus, String error, String exceptionMessage) {

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", httpStatus.value());
        body.put("error", error);
        body.put("message", exceptionMessage);
        return ResponseEntity.status(httpStatus).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Bad Request");
        body.put("fieldErrors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
}