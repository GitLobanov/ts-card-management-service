package by.lobanov.cardmanagementservice.handler;

import by.lobanov.cardmanagementservice.exception.BadRequestException;
import by.lobanov.cardmanagementservice.exception.CardOperationException;
import by.lobanov.cardmanagementservice.exception.InsufficientFundsException;
import by.lobanov.cardmanagementservice.exception.ResourceNotFoundException;
import by.lobanov.cardmanagementservice.exception.TokenRefreshException;
import by.lobanov.cardmanagementservice.model.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {} at path {}", ex.getMessage(), request.getRequestURI());
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    // Обработчик для BadRequestException, CardOperationException, InsufficientFundsException
    @ExceptionHandler({BadRequestException.class, CardOperationException.class, InsufficientFundsException.class})
    public ResponseEntity<ErrorResponse> handleBadRequestAndOperationExceptions(RuntimeException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ResponseStatus responseStatus = ex.getClass().getAnnotation(ResponseStatus.class);
        if (responseStatus != null) {
            status = responseStatus.value();
        } else if (ex instanceof CardOperationException) {
            status = HttpStatus.CONFLICT; // Для CardOperationException используем 409 Conflict
        }
        log.warn("Operation error [{}]: {} at path {}", status, ex.getMessage(), request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, status);
    }

    // Обработчик для ошибок валидации DTO (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        String detailedMessage = "Validation failed: " + errors.toString();
        log.warn("Validation error: {} at path {}", detailedMessage, request.getRequestURI());

        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                detailedMessage, // Можно вернуть Map<String, String> errors в отдельном поле ответа
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<ErrorResponse> handleTokenRefreshException(TokenRefreshException ex, HttpServletRequest request) {
        log.warn("Token Refresh Error: {} at path {}", ex.getMessage(), request.getRequestURI());
        HttpStatus status = HttpStatus.FORBIDDEN;

        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(), // Сообщение из исключения
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, status);
    }


    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleTokenRefreshException(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Login Error: {} to {}", ex.getMessage(), request.getRequestURI());
        HttpStatus status = HttpStatus.FORBIDDEN;

        ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, HttpServletRequest request) {
        log.error("An unexpected error occurred at path {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An internal server error occurred. Please try again later.",
                request.getRequestURI()
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
