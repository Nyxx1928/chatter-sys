package org.example.chat.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GlobalExceptionHandler.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleChatApplicationException_ShouldReturnErrorResponseWithCorrectStatus() {
        // Given
        UserNotFoundException exception = new UserNotFoundException(1L);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleChatApplicationException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("User not found with id: 1", body.getMessage());
        assertEquals(404, body.getStatus());
        assertEquals("USER_NOT_FOUND", body.getErrorCode());
        assertNotNull(body.getTimestamp());
    }

    @Test
    void handleChatApplicationException_WithUnauthorizedException_ShouldReturnForbidden() {
        // Given
        UnauthorizedException exception = new UnauthorizedException("Access denied");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleChatApplicationException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("Access denied", body.getMessage());
        assertEquals(403, body.getStatus());
        assertEquals("UNAUTHORIZED", body.getErrorCode());
    }

    @Test
    void handleChatApplicationException_WithValidationException_ShouldReturnBadRequest() {
        // Given
        ValidationException exception = new ValidationException("Invalid input");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleChatApplicationException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("Invalid input", body.getMessage());
        assertEquals(400, body.getStatus());
        assertEquals("VALIDATION_ERROR", body.getErrorCode());
    }

    @Test
    void handleValidationException_ShouldReturnFieldErrors() throws Exception {
        // Given
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("user", "username", "Username is required");
        FieldError fieldError2 = new FieldError("user", "email", "Email is invalid");
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError1, fieldError2));
        
        // Create a valid MethodParameter for the exception
        java.lang.reflect.Method method = this.getClass().getMethod("dummyMethod", String.class);
        org.springframework.core.MethodParameter methodParameter = 
            new org.springframework.core.MethodParameter(method, 0);
        
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(methodParameter, bindingResult);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleValidationException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("Validation failed", body.getMessage());
        assertEquals(400, body.getStatus());
        assertEquals("VALIDATION_ERROR", body.getErrorCode());
        assertNotNull(body.getErrors());
        assertEquals(2, body.getErrors().size());
        assertEquals("Username is required", body.getErrors().get("username"));
        assertEquals("Email is invalid", body.getErrors().get("email"));
    }
    
    // Dummy method for creating MethodParameter in tests
    public void dummyMethod(String param) {
        // Used only for test reflection
    }

    @Test
    void handleIllegalArgumentException_ShouldReturnBadRequest() {
        // Given
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument provided");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalArgumentException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("Invalid argument provided", body.getMessage());
        assertEquals(400, body.getStatus());
        assertEquals("INVALID_ARGUMENT", body.getErrorCode());
    }

    @Test
    void handleIllegalStateException_ShouldReturnInternalServerError() {
        // Given
        IllegalStateException exception = new IllegalStateException("Invalid state");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleIllegalStateException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("Invalid state", body.getMessage());
        assertEquals(500, body.getStatus());
        assertEquals("INVALID_STATE", body.getErrorCode());
    }

    @Test
    void handleGenericException_ShouldReturnInternalServerError() {
        // Given
        Exception exception = new RuntimeException("Unexpected error");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("An unexpected error occurred", body.getMessage());
        assertEquals(500, body.getStatus());
        assertEquals("INTERNAL_ERROR", body.getErrorCode());
    }

    @Test
    void handleChatApplicationException_WithRoomNotFoundException_ShouldReturnNotFound() {
        // Given
        RoomNotFoundException exception = new RoomNotFoundException(5L);

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleChatApplicationException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("Chat room not found with id: 5", body.getMessage());
        assertEquals(404, body.getStatus());
        assertEquals("ROOM_NOT_FOUND", body.getErrorCode());
    }

    @Test
    void handleChatApplicationException_WithWebSocketException_ShouldReturnInternalServerError() {
        // Given
        WebSocketException exception = new WebSocketException("WebSocket connection failed");

        // When
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleChatApplicationException(exception);

        // Then
        assertNotNull(response);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        ErrorResponse body = response.getBody();
        assertNotNull(body);
        assertEquals("WebSocket connection failed", body.getMessage());
        assertEquals(500, body.getStatus());
        assertEquals("WEBSOCKET_ERROR", body.getErrorCode());
    }
}
