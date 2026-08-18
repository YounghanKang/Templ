package com.example.demo.analysis;

public enum AnalysisOutcomeType {

    UNMAPPED,

    NO_WARNING,

    SOFT_WARNING,

    HARD_WARNING
}

/*후보 Task 없음       → UNMAPPED
위험도 1~5          → NO_WARNING
위험도 6~7          → SOFT_WARNING
위험도 8~10         → HARD_WARNING*/