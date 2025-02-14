package com.liushukov.cloud_file.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import java.time.Instant;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGlobalException(Exception exception, WebRequest webRequest) {

        if (exception instanceof BadCredentialsException) {
            ErrorDetails errorDetails = new ErrorDetails(
                    Instant.now(),
                    "The username or password is incorrect",
                    webRequest.getDescription(false)
            );
            return ResponseEntity.status(HttpStatusCode.valueOf(401)).body(errorDetails);
        }

        if (exception instanceof AccountStatusException) {
            var errorDetails = new ErrorDetails(
                    Instant.now(),
                    "The account is locked",
                    webRequest.getDescription(false)
            );
            return ResponseEntity.status(HttpStatusCode.valueOf(403)).body(errorDetails);
        }

        if (exception instanceof AccessDeniedException) {
            var errorDetails = new ErrorDetails(
                    Instant.now(),
                    "You are not authorized to access this resource",
                    webRequest.getDescription(false)
            );
            return ResponseEntity.status(HttpStatusCode.valueOf(403)).body(errorDetails);
        }

        if (exception instanceof SignatureException) {
            var errorDetails = new ErrorDetails(
                    Instant.now(),
                    "The JWT signature is invalid",
                    webRequest.getDescription(false)
            );
            return ResponseEntity.status(HttpStatusCode.valueOf(403)).body(errorDetails);
        }

        if (exception instanceof ExpiredJwtException) {
            var errorDetails = new ErrorDetails(
                    Instant.now(),
                    "The JWT has expired",
                    webRequest.getDescription(false)
            );
            return ResponseEntity.status(HttpStatusCode.valueOf(403)).body(errorDetails);
        }

        return ResponseEntity.status(HttpStatusCode.valueOf(500))
                .body(new ErrorDetails(
                        Instant.now(),
                        "Internal server error",
                        webRequest.getDescription(false)
                ));
    }
}
