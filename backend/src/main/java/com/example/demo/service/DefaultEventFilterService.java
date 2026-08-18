package com.example.demo.service;

import com.example.demo.config.SuggestionPolicyProperties;
import com.example.demo.dto.EventFilterDto;
import com.example.demo.filter.FilterDecision;
import com.example.demo.filter.FilterResult;
import com.example.demo.filter.RuleBasedEventFilter;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DefaultEventFilterService implements EventFilterService {

    private final RuleBasedEventFilter ruleBasedEventFilter;
    private final SuggestionPolicyProperties policyProperties;

    public DefaultEventFilterService(RuleBasedEventFilter ruleBasedEventFilter, SuggestionPolicyProperties policyProperties) {
        this.ruleBasedEventFilter = ruleBasedEventFilter;
        this.policyProperties = policyProperties;
    }

    @Override
    public EventFilterDto.FilterResultResponse evaluate(String teamId, EventFilterDto.FilterRequest request) {
        if (request == null || request.getContent() == null) {
            return EventFilterDto.FilterResultResponse.builder()
                    .passed(false)
                    .decision("FILTERED_OUT")
                    .impactScore(0.0)
                    .editDistance(0)
                    .matchedKeywords(List.of())
                    .reason("Empty content")
                    .build();
        }

        boolean sys = Boolean.TRUE.equals(request.getGeneratedBySystem());
        FilterResult result = ruleBasedEventFilter.evaluate(request.getContent(), sys);

        int editDistance = 0;
        if (request.getPreviousContent() != null && !request.getPreviousContent().isBlank()) {
            editDistance = calculateLevenshteinDistance(request.getContent(), request.getPreviousContent());
            if (editDistance < policyProperties.getMinEditDistance()) {
                return EventFilterDto.FilterResultResponse.builder()
                        .passed(false)
                        .decision("FILTERED_OUT")
                        .impactScore(0.1)
                        .editDistance(editDistance)
                        .matchedKeywords(List.of())
                        .reason("Edit distance (" + editDistance + ") below min threshold (" + policyProperties.getMinEditDistance() + ")")
                        .build();
            }
        }

        // Custom domain keyword matching from SuggestionPolicyProperties
        List<String> matchedKeywords = new ArrayList<>();
        String lowerContent = request.getContent().toLowerCase(Locale.ROOT);
        for (String kw : policyProperties.getKeywords()) {
            if (lowerContent.contains(kw.toLowerCase(Locale.ROOT)) && !matchedKeywords.contains(kw)) {
                matchedKeywords.add(kw);
            }
        }

        boolean passed = (result.decision() == FilterDecision.AI_REQUIRED) || !matchedKeywords.isEmpty();
        if (result.decision() == FilterDecision.FILTERED_OUT && matchedKeywords.isEmpty()) {
            passed = false;
        }

        double impactScore = Math.min(1.0, (result.score() / 10.0) + (matchedKeywords.size() * 0.1));

        return EventFilterDto.FilterResultResponse.builder()
                .passed(passed)
                .decision(passed ? "PASS_TO_AI" : result.decision().name())
                .impactScore(impactScore)
                .editDistance(editDistance)
                .matchedKeywords(matchedKeywords)
                .reason(passed ? "Matched domain keywords or rule filter passed" : result.reason())
                .build();
    }

    private int calculateLevenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    int cost = (s1.charAt(i - 1) == s2.charAt(j - 1)) ? 0 : 1;
                    dp[i][j] = Math.min(
                            Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + cost
                    );
                }
            }
        }

        return dp[s1.length()][s2.length()];
    }
}
