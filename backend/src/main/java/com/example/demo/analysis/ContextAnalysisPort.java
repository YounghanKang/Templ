package com.example.demo.analysis;

public interface ContextAnalysisPort {

    ContextAnalysisResult analyze(
            AnalysisCommand command
    );
}