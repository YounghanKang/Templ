package com.example.demo.matching;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class CandidateNodeMatcher {

    private static final int DEFAULT_LIMIT = 5;

    private static final int TITLE_WEIGHT = 5;

    private static final int KEYWORD_WEIGHT = 4;

    private static final int GOAL_WEIGHT = 3;

    private static final int SUMMARY_WEIGHT = 2;


    /**
     * 일반적으로 사용하는 기본 Top 5 매칭.
     */
    public List<CandidateNodeMatch> findTopCandidates(
            String eventText,
            List<NodeContext> candidates
    ) {

        return findTopCandidates(
                eventText,
                candidates,
                DEFAULT_LIMIT
        );
    }


    /**
     * 전달받은 후보 Node 중
     * 이벤트 메시지와 관련도가 높은 순서로 정렬합니다.
     */
    public List<CandidateNodeMatch> findTopCandidates(
            String eventText,
            List<NodeContext> candidates,
            int limit
    ) {

        if (
                eventText == null
                        || eventText.isBlank()
                        || candidates == null
                        || candidates.isEmpty()
        ) {
            return List.of();
        }


        if (limit <= 0) {
            return List.of();
        }


        Set<String> eventTerms =
                tokenize(eventText);


        if (eventTerms.isEmpty()) {
            return List.of();
        }


        return candidates
                .stream()
                .map(
                        node ->
                                calculateMatch(
                                        node,
                                        eventTerms
                                )
                )
                /*
                 * 관련 단어가 하나도 없는 Node는
                 * AI Context로 보내지 않습니다.
                 */
                .filter(
                        match ->
                                match.score() > 0
                )
                /*
                 * 점수가 높은 Node부터 정렬합니다.
                 */
                .sorted(
                        Comparator
                                .comparingInt(
                                        CandidateNodeMatch::score
                                )
                                .reversed()
                                .thenComparing(
                                        CandidateNodeMatch::title,
                                        Comparator.nullsLast(
                                                String.CASE_INSENSITIVE_ORDER
                                        )
                                )
                                .thenComparing(
                                        CandidateNodeMatch::nodeId
                                )
                )
                .limit(limit)
                .toList();
    }


    private CandidateNodeMatch calculateMatch(
            NodeContext node,
            Set<String> eventTerms
    ) {

        int score = 0;

        Set<String> matchedTerms =
                new LinkedHashSet<>();


        String normalizedTitle =
                normalize(node.title());

        String normalizedGoal =
                normalize(node.goal());

        String normalizedSummary =
                normalize(node.summary());


        /*
         * 제목은 가장 직접적인 Task 식별 정보이므로
         * 가장 높은 가중치를 줍니다.
         */
        for (String term : eventTerms) {

            if (containsTerm(
                    normalizedTitle,
                    term
            )) {

                score += TITLE_WEIGHT;

                matchedTerms.add(term);
            }


            if (containsTerm(
                    normalizedGoal,
                    term
            )) {

                score += GOAL_WEIGHT;

                matchedTerms.add(term);
            }


            if (containsTerm(
                    normalizedSummary,
                    term
            )) {

                score += SUMMARY_WEIGHT;

                matchedTerms.add(term);
            }
        }


        /*
         * Node의 명시적인 keywords 역시
         * 관련성 판단에 높은 가중치를 줍니다.
         */
        List<String> keywords =
                node.keywords() == null
                        ? List.of()
                        : node.keywords();


        for (String keyword : keywords) {

            String normalizedKeyword =
                    normalize(keyword);


            for (String term : eventTerms) {

                if (
                        containsTerm(
                                normalizedKeyword,
                                term
                        )
                ) {

                    score += KEYWORD_WEIGHT;

                    matchedTerms.add(term);
                }
            }
        }


        return new CandidateNodeMatch(
                node.nodeId(),
                node.title(),
                score,
                new ArrayList<>(
                        matchedTerms
                )
        );
    }


    /**
     * 이벤트 메시지를 비교 가능한 단어 단위로 나눕니다.
     *
     * 한글/영문/숫자는 남기고
     * 특수문자는 공백으로 바꿉니다.
     */
    private Set<String> tokenize(
            String text
    ) {

        String normalized =
                normalize(text)
                        .replaceAll(
                                "[^\\p{L}\\p{N}]+",
                                " "
                        );


        String[] rawTerms =
                normalized.split("\\s+");


        Set<String> terms =
                new LinkedHashSet<>();


        for (String term : rawTerms) {

            if (isUsefulTerm(term)) {
                terms.add(term);
            }
        }


        return terms;
    }


    private boolean isUsefulTerm(
            String term
    ) {

        if (
                term == null
                        || term.isBlank()
        ) {
            return false;
        }


        /*
         * 일반적으로 한 글자 단어는
         * 매칭 노이즈가 많아 제외합니다.
         */
        if (term.length() >= 2) {
            return true;
        }


        return false;
    }


    private boolean containsTerm(
            String target,
            String term
    ) {

        return target != null
                && !target.isBlank()
                && target.contains(term);
    }


    private String normalize(
            String text
    ) {

        if (text == null) {
            return "";
        }


        return text
                .trim()
                .replaceAll(
                        "\\s+",
                        " "
                )
                .toLowerCase(
                        Locale.ROOT
                );
    }
}