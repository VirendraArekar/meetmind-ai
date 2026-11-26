package com.meetmind.analytics;

import com.meetmind.common.response.ApiResponse;
import com.meetmind.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Dashboard metrics, meeting duration, and AI usage statistics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard analytics summary")
    public ResponseEntity<ApiResponse<AnalyticsService.DashboardAnalyticsDto>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getDashboard(SecurityUtils.currentUserId())));
    }

    @GetMapping("/meetings")
    @Operation(summary = "Get meeting and AI usage analytics")
    public ResponseEntity<ApiResponse<AnalyticsService.DashboardAnalyticsDto>> getMeetingAnalytics() {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getDashboard(SecurityUtils.currentUserId())));
    }
}
