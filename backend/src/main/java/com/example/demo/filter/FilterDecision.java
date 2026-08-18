package com.example.demo.filter;

public enum FilterDecision {
    FILTERED_OUT,
    LOG_ONLY,
    AI_REQUIRED
}
/*
FILTERED_OUT
→ 분석 가치 없음
→ 여기서 처리 종료

LOG_ONLY
→ 기록은 남김
→ AI 호출은 하지 않음

AI_REQUIRED
→ 중요한 변화 가능성 있음
→ Task 후보 탐색 + AI 분석 진행
* */