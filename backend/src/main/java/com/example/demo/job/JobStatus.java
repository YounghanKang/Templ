package com.example.demo.job;

public enum JobStatus {
    READY,
    PROCESSING,
    RETRY_WAIT,
    COMPLETED,
    FAILED_FINAL
}
/*
* READY
→ 처리 대기

PROCESSING
→ Worker가 현재 처리 중

RETRY_WAIT
→ 실패했지만 다시 시도 예정

COMPLETED
→ 정상 완료

FAILED_FINAL
→ 재시도 횟수를 모두 사용한 최종 실패
* */