package com.meetmind.analytics;

import com.meetmind.actionitem.ActionItemRepository;
import com.meetmind.ai.AiUsageLog;
import com.meetmind.ai.AiUsageLogRepository;
import com.meetmind.meeting.Meeting;
import com.meetmind.meeting.MeetingRepository;
import com.meetmind.organization.OrganizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    public record DailyActivityPoint(String date, long meetings, long durationMinutes) {}
    public record CategoryBreakdown(String category, long count) {}
    public record AiOperationStats(String operation, long calls, long totalTokens) {}

    public record DashboardAnalyticsDto(
            long totalMeetings,
            long totalDurationSeconds,
            long totalActionItems,
            long pendingTasks,
            long completedTasks,
            double completionRatePercent,
            long aiOperationsCount,
            long totalAiTokens,
            List<DailyActivityPoint> activityTimeline,
            List<CategoryBreakdown> categoryBreakdown,
            List<AiOperationStats> aiUsageStats
    ) {}

    private final MeetingRepository meetingRepository;
    private final ActionItemRepository actionItemRepository;
    private final AiUsageLogRepository aiUsageLogRepository;
    private final OrganizationService organizationService;

    @Transactional(readOnly = true)
    public DashboardAnalyticsDto getDashboard(UUID userId) {
        UUID orgId = organizationService.requireDefaultOrganizationId(userId);
        List<Meeting> meetings = meetingRepository.findAllAccessibleByOrgAndUser(orgId, userId);

        long totalDuration = meetings.stream().mapToLong(Meeting::getDurationSeconds).sum();
        long totalItems = actionItemRepository.countByOrganizationId(orgId);
        long pending = actionItemRepository.countByOrganizationIdAndStatus(orgId, "PENDING");
        long completed = actionItemRepository.countByOrganizationIdAndStatus(orgId, "COMPLETED");
        double completionRate = totalItems == 0 ? 0.0 : Math.round((completed * 1000.0) / totalItems) / 10.0;

        List<AiUsageLog> logs = aiUsageLogRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
        long totalTokens = logs.stream().mapToLong(AiUsageLog::getTokensUsed).sum();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC);
        Map<String, long[]> byDay = new TreeMap<>();
        Map<String, Long> byCategory = new LinkedHashMap<>();

        for (Meeting m : meetings) {
            String day = fmt.format(m.getCreatedAt());
            byDay.computeIfAbsent(day, k -> new long[2]);
            byDay.get(day)[0] += 1;
            byDay.get(day)[1] += Math.max(1, m.getDurationSeconds() / 60);

            byCategory.merge(m.getCategory(), 1L, Long::sum);
        }

        List<DailyActivityPoint> timeline = byDay.entrySet().stream()
                .map(e -> new DailyActivityPoint(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();

        List<CategoryBreakdown> categories = byCategory.entrySet().stream()
                .map(e -> new CategoryBreakdown(e.getKey(), e.getValue()))
                .toList();

        Map<String, long[]> aiByOp = new LinkedHashMap<>();
        for (AiUsageLog log : logs) {
            aiByOp.computeIfAbsent(log.getOperation(), k -> new long[2]);
            aiByOp.get(log.getOperation())[0] += 1;
            aiByOp.get(log.getOperation())[1] += log.getTokensUsed();
        }
        List<AiOperationStats> aiStats = aiByOp.entrySet().stream()
                .map(e -> new AiOperationStats(e.getKey(), e.getValue()[0], e.getValue()[1]))
                .toList();

        return new DashboardAnalyticsDto(
                meetings.size(),
                totalDuration,
                totalItems,
                pending,
                completed,
                completionRate,
                logs.size(),
                totalTokens,
                timeline,
                categories,
                aiStats
        );
    }
}
