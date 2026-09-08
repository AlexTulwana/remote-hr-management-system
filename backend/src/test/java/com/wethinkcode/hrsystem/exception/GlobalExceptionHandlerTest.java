package com.wethinkcode.hrsystem.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleAccessDenied_returns403WithMessage() {
        AccessDeniedException ex = new AccessDeniedException("not allowed");

        ResponseEntity<Map<String, String>> response = handler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("not allowed", response.getBody().get("error"));
    }

    @Test
    void handleAccessDenied_withNullMessage_returns403WithFallback() {
        AccessDeniedException ex = new AccessDeniedException(null);

        ResponseEntity<Map<String, String>> response = handler.handleAccessDenied(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Access denied", response.getBody().get("error"));
    }

    @Test
    void handleRuntimeException_returns400WithMessage() {
        RuntimeException ex = new RuntimeException("something broke");

        ResponseEntity<Map<String, String>> response = handler.handleRuntimeException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("something broke", response.getBody().get("error"));
    }

    @Test
    void handleRuntimeException_withNullMessage_returns400WithFallback() {
        RuntimeException ex = new RuntimeException();

        ResponseEntity<Map<String, String>> response = handler.handleRuntimeException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Unknown error", response.getBody().get("error"));
    }
}
