package com.meetmind.health;

import java.time.Instant;

/**
 * Health check response payload.
 */
public record HealthStatusResponse(
        String status,
        String service,
        String version,
        Instant checkedAt
) {
}
