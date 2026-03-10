package effectivemobile.exception.handler;

import effectivemobile.dto.ResponseError;
import effectivemobile.exception.AliasAlreadyExistsException;
import effectivemobile.exception.InvalidUrlFormatException;
import effectivemobile.exception.ShortCodeGenerationException;
import effectivemobile.exception.ShortUrlNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ShortUrlNotFoundException.class)
    public ResponseEntity<ResponseError> handleNotFound(
            ShortUrlNotFoundException ex,
            HttpServletRequest request
    ) {
        log.warn("Short URL not found={}", ex.getMessage());
        ResponseError error = ResponseError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("Not Found")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(AliasAlreadyExistsException.class)
    public ResponseEntity<ResponseError> handleAlias(AliasAlreadyExistsException ex, HttpServletRequest req) {
        log.warn("Alias conflict: {}", ex.getMessage());
        ResponseError error = ResponseError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Conflict")
                .message(ex.getMessage())
                .path(req.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(InvalidUrlFormatException.class)
    public ResponseEntity<ResponseError> handleInvalidUrl(
            InvalidUrlFormatException ex,
            HttpServletRequest request
    ) {
        log.warn("Invalid URL format={}", ex.getMessage());
        ResponseError error = ResponseError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Bad Request")
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ShortCodeGenerationException.class)
    public ResponseEntity<ResponseError> handleShortCodeGeneration(
            ShortCodeGenerationException ex,
            HttpServletRequest req
    ) {
        log.error("Short code generation failed: {}", ex.getMessage());

        ResponseError error = ResponseError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("Service Unavailable")
                .message(ex.getMessage())
                .path(req.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseError> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .findFirst()
                .orElse("Invalid request body");

        log.warn("Validation error={}", message);
        ResponseError error = ResponseError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .message(message)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseError> handleOtherExceptions(
            Exception ex,
            HttpServletRequest request
    ) {

        log.error("Unexpected error", ex);
        ResponseError error = ResponseError.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error("Internal Server Error")
                .message("Unexpected error occurred")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}

