package com.meetmind.health;

import com.meetmind.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Public health-check endpoint for verifying API availability and version.
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Service health and readiness endpoints")
public class HealthController {

    private final String appName;
    private final String appVersion;

    public HealthController(
            @Value("${spring.application.name:meetmind-backend}") String appName,
            @Value("${app.version:0.1.0}") String appVersion
    ) {
        this.appName = appName;
        this.appVersion = appVersion;
    }

    @GetMapping
    @Operation(summary = "Get API health status")
    public ResponseEntity<ApiResponse<HealthStatusResponse>> getHealth() {
        HealthStatusResponse payload = new HealthStatusResponse(
                "UP",
                appName,
                appVersion,
                Instant.now()
        );
        return ResponseEntity.ok(ApiResponse.ok("MeetMind AI API is healthy", payload));
    }
}
