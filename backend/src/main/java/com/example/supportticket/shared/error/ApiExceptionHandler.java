package com.example.supportticket.shared.error;

import com.example.supportticket.ticket.application.InvalidStatusTransitionException;
import com.example.supportticket.ticket.application.TicketInputException;
import com.example.supportticket.ticket.application.TicketNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final HttpStatus UNPROCESSABLE_CONTENT = HttpStatus.valueOf(422);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleBeanValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<FieldErrorDetail> fieldErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList();

        return problem(
                UNPROCESSABLE_CONTENT,
                "Request validation failed",
                "One or more request fields are invalid.",
                "VALIDATION_FAILED",
                fieldErrors,
                request
        );
    }

    @ExceptionHandler(TicketInputException.class)
    ResponseEntity<ProblemDetail> handleTicketInput(
            TicketInputException exception,
            HttpServletRequest request
    ) {
        return problem(
                UNPROCESSABLE_CONTENT,
                "Request validation failed",
                exception.getMessage(),
                "VALIDATION_FAILED",
                exception.getFieldErrors(),
                request
        );
    }

    @ExceptionHandler(TicketNotFoundException.class)
    ResponseEntity<ProblemDetail> handleTicketNotFound(
            TicketNotFoundException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.NOT_FOUND,
                "Ticket not found",
                exception.getMessage(),
                "TICKET_NOT_FOUND",
                List.of(),
                request
        );
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    ResponseEntity<ProblemDetail> handleInvalidTransition(
            InvalidStatusTransitionException exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.CONFLICT,
                "Invalid ticket status transition",
                exception.getMessage(),
                "INVALID_STATUS_TRANSITION",
                List.of(),
                request
        );
    }

    @ExceptionHandler({
            MalformedRequestException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            ConstraintViolationException.class,
            HandlerMethodValidationException.class
    })
    ResponseEntity<ProblemDetail> handleMalformedRequest(
            Exception exception,
            HttpServletRequest request
    ) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Malformed request",
                exception instanceof MalformedRequestException
                        ? exception.getMessage()
                        : "The request could not be parsed or contains an invalid value.",
                "MALFORMED_REQUEST",
                List.of(),
                request
        );
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(
            Exception exception,
            HttpServletRequest request
    ) {
        LOGGER.error("Unhandled API error", exception);
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "The request could not be completed.",
                "INTERNAL_ERROR",
                List.of(),
                request
        );
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String title,
            String detail,
            String code,
            List<FieldErrorDetail> fieldErrors,
            HttpServletRequest request
    ) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("https://support-tickets.example/problems/" + toSlug(code)));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        problem.setProperty("fieldErrors", fieldErrors);
        return ResponseEntity.status(status).body(problem);
    }

    private String toSlug(String code) {
        return code.toLowerCase().replace('_', '-');
    }
}
