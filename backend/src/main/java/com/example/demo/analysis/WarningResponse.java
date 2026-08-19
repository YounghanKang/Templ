package com.example.demo.analysis;

import java.time.Instant;
import java.util.UUID;

/**
 * BE2 분석 결과 중 실제 경고만 프론트/외부 클라이언트에 제공하기 위한
 * 읽기 전용 응답 DTO입니다.
 *
 * 로드맵을 직접 수정하지 않습니다.
 */
public record WarningResponse(
        UUID analysisId,
        UUID eventId,
        String projectId,
        String taskId,
        AnalysisOutcomeType outcomeType,
        ChangeType changeType,
        String summary,
        int riskScore,
        double confidence,
        String sourceTool,
        String sourceLocation,
        String sourceUrl,
        Instant occurredAt,
        Instant analyzedAt
) {
}
