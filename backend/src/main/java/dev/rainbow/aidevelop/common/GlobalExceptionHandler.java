package dev.rainbow.aidevelop.common;

import dev.rainbow.aidevelop.chat.ChatUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        List<ApiError.FieldViolation> violations = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiError.FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList();
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", violations);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleMalformedRequest() {
        return error(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Request body is invalid", List.of());
    }

    @ExceptionHandler(ChatUnavailableException.class)
    ResponseEntity<ApiError> handleChatUnavailable(ChatUnavailableException exception) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "AI_NOT_CONFIGURED", exception.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Unexpected error while processing " + request.getRequestURI(), List.of());
    }

    private ResponseEntity<ApiError> error(
            HttpStatus status,
            String code,
            String message,
            List<ApiError.FieldViolation> violations
    ) {
        return ResponseEntity.status(status)
                .body(new ApiError(Instant.now(), status.value(), code, message, violations));
    }
}
