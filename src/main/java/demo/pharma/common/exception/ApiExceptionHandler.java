package demo.pharma.common.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class ApiExceptionHandler {
    record ErrorResponse(Instant timestamp, int status, String error, String message, String path,
            Map<String, String> errors) {
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException e, HttpServletRequest r) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(x -> errors.put(x.getField(), x.getDefaultMessage()));
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid request", r, errors);
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(NotFoundException e, HttpServletRequest r) {
        return response(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage(), r, Map.of());
    }

    @ExceptionHandler({ BusinessException.class, DataIntegrityViolationException.class })
    ResponseEntity<ErrorResponse> business(Exception e, HttpServletRequest r) {
        return response(HttpStatus.CONFLICT, "BUSINESS_RULE",
                e instanceof DataIntegrityViolationException ? "Record conflicts with an existing record"
                        : e.getMessage(),
                r, Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> denied(AccessDeniedException e, HttpServletRequest r) {
        return response(HttpStatus.FORBIDDEN, "FORBIDDEN", "You do not have permission to perform this action", r,
                Map.of());
    }

    private ResponseEntity<ErrorResponse> response(HttpStatus s, String error, String message, HttpServletRequest r,
            Map<String, String> details) {
        return ResponseEntity.status(s)
                .body(new ErrorResponse(Instant.now(), s.value(), error, message, r.getRequestURI(), details));
    }
}
