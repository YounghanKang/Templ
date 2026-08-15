package com.example.demo.job;

public enum JobType {
    FILTER_EVENT,
    ANALYZE_EVENT
}
/*FILTER_EVENT
→ 규칙 기반 1차 필터 담당

ANALYZE_EVENT
→ Filter를 통과한 이벤트의
   Task Matching + AI 분석 담당*/