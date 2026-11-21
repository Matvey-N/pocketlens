package com.pocketlens.common.error;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Centralised error handling: every error leaves the API as {@code application/problem+json} with a correct
 * HTTP status and a stable error code. Unexpected exceptions are logged with the correlation id and never expose
 * internal details to the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ProblemDetail> handleRateLimit(RateLimitExceededException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        return ResponseEntity.status(ex.code().status())
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.retryAfterSeconds()))
                .body(ProblemFactory.create(ex.code(), ex.getMessage(), ex.details()));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApi(ApiException ex) {
        if (ex.code().status().is5xxServerError()) {
            log.error("Request failed: code={} message={}", ex.code(), ex.getMessage());
        } else {
            log.info("Request rejected: code={} message={}", ex.code(), ex.getMessage());
        }
        return respond(ProblemFactory.create(ex.code(), ex.getMessage(), ex.details()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleInvalidBody(MethodArgumentNotValidException ex) {
        List<FieldViolation> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldViolation(e.getField(), e.getDefaultMessage()))
                .toList();
        return validationProblem(errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleInvalidParameters(HandlerMethodValidationException ex) {
        List<FieldViolation> errors = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> new FieldViolation(r.getMethodParameter().getParameterName(),
                                e.getDefaultMessage())))
                .toList();
        return validationProblem(errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException ex) {
        List<FieldViolation> errors = ex.getConstraintViolations().stream()
                .map(v -> new FieldViolation(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return validationProblem(errors);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class,
            MissingServletRequestPartException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ProblemDetail> handleMalformed(Exception ex) {
        String detail = ex instanceof HttpMessageNotReadableException
                ? "Request body is missing or is not valid JSON"
                : ex.getMessage();
        return respond(ProblemFactory.create(ErrorCode.MALFORMED_REQUEST, detail));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> handleTooLarge(MaxUploadSizeExceededException ex) {
        return respond(ProblemFactory.create(ErrorCode.FILE_TOO_LARGE, "The file exceeds the maximum size of 5 MB"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return respond(ProblemFactory.create(ErrorCode.UNSUPPORTED_MEDIA_TYPE, ex.getMessage()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return respond(ProblemFactory.create(ErrorCode.METHOD_NOT_ALLOWED, ex.getMessage()));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResource(NoResourceFoundException ex) {
        return respond(ProblemFactory.create(ErrorCode.NOT_FOUND, "No endpoint " + ex.getResourcePath()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleIntegrity(DataIntegrityViolationException ex) {
        log.info("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return respond(ProblemFactory.create(ErrorCode.CONFLICT, "The request conflicts with existing data"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex) {
        return respond(ProblemFactory.create(ErrorCode.FORBIDDEN, "You do not have permission for this action"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex) {
        return respond(ProblemFactory.create(ErrorCode.UNAUTHORIZED, "Authentication required"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return respond(ProblemFactory.create(ErrorCode.INTERNAL_ERROR,
                "Something went wrong. Quote the correlation id when reporting the problem."));
    }

    private ResponseEntity<ProblemDetail> validationProblem(List<FieldViolation> errors) {
        return respond(ProblemFactory.create(ErrorCode.VALIDATION_FAILED,
                "One or more fields are invalid", Map.of("errors", errors)));
    }

    private static ResponseEntity<ProblemDetail> respond(ProblemDetail problem) {
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }
}
