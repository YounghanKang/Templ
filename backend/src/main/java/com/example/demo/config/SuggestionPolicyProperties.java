package com.example.demo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.suggestion.policy")
@Getter
@Setter
public class SuggestionPolicyProperties {

    private boolean autoRegenerateEnabled = false;
    private int maxRegenerationDepth = 5;
    private double impactThreshold = 0.6;
    private List<String> keywords = Arrays.asList("API", "schema", "feature", "conflict", "endpoint", "auth", "login", "spec", "db", "table");
    private int minEditDistance = 3;
}
