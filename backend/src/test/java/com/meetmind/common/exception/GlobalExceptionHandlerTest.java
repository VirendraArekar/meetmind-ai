package com.meetmind.common.exception;

import com.meetmind.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleApiExceptionReturnsStructuredErrorResponse() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/meetings/42");
        ResourceNotFoundException exception = new ResourceNotFoundException("Meeting 42 not found");

        ResponseEntity<ApiResponse<Void>> response = handler.handleApiException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Meeting 42 not found");
        assertThat(response.getBody().getError().getCode()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().getError().getPath()).isEqualTo("/api/v1/meetings/42");
    }

    @Test
    void handleUnexpectedExceptionMasksInternalDetails() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/meetings");
        RuntimeException exception = new RuntimeException("Sensitive DB error");

        ResponseEntity<ApiResponse<Void>> response = handler.handleUnexpectedException(exception, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getError().getCode()).isEqualTo("INTERNAL_SERVER_ERROR");
    }
}
