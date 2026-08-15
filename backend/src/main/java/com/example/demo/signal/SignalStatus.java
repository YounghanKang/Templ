package com.example.demo.signal;

public enum SignalStatus {
    RECEIVED,

    FILTERED_OUT,
    LOG_ONLY,

    AI_PENDING,
    ANALYZING,
    ANALYZED,

    UNMAPPED,

    WARNING_CREATED,
    PROPOSAL_CREATED,

    INVALID_OUTPUT,

    IGNORED,

    FAILED_RETRYABLE,
    FAILED_FINAL
}

/*
RECEIVED
→ Slack/GitHub 이벤트는 들어왔지만 아직 분석 전

FILTERED_OUT
→ 중요하지 않아 AI 분석하지 않음

AI_PENDING
→ AI 분석 대기

ANALYZING
→ AI 분석 중

UNMAPPED
→ 관련 Task를 찾지 못함

WARNING_CREATED
→ 6점 이상 경고 생성

PROPOSAL_CREATED
→ 8점 이상으로 Backend1 Proposal 생성

INVALID_OUTPUT
→ AI 응답 검증 실패

FAILED_FINAL
→ 재시도 후 최종 실패
*/