package com.example.demo.service;

import com.example.demo.dto.EventFilterDto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class EventFilterServiceTest {

    @Autowired
    private EventFilterService eventFilterService;

    @Test
    public void testLowValueMessageIsFilteredOut() {
        EventFilterDto.FilterRequest req = EventFilterDto.FilterRequest.builder()
                .sourceTool("slack")
                .content("네 확인했습니다")
                .generatedBySystem(false)
                .build();

        EventFilterDto.FilterResultResponse res = eventFilterService.evaluate("T-001", req);
        Assertions.assertFalse(res.isPassed());
        Assertions.assertEquals("FILTERED_OUT", res.getDecision());
    }

    @Test
    public void testDomainKeywordPassesFilter() {
        EventFilterDto.FilterRequest req = EventFilterDto.FilterRequest.builder()
                .sourceTool("slack")
                .content("로그인 API 스키마 변경이 필요합니다")
                .generatedBySystem(false)
                .build();

        EventFilterDto.FilterResultResponse res = eventFilterService.evaluate("T-001", req);
        Assertions.assertTrue(res.isPassed());
        Assertions.assertEquals("PASS_TO_AI", res.getDecision());
        Assertions.assertTrue(res.getMatchedKeywords().contains("api") || res.getMatchedKeywords().contains("API") || res.getMatchedKeywords().contains("schema"));
    }

    @Test
    public void testMinEditDistanceFilter() {
        EventFilterDto.FilterRequest req = EventFilterDto.FilterRequest.builder()
                .sourceTool("slack")
                .content("API 스키마 변경")
                .previousContent("API 스키마 변경!") // distance 1 (< minEditDistance 3)
                .generatedBySystem(false)
                .build();

        EventFilterDto.FilterResultResponse res = eventFilterService.evaluate("T-001", req);
        Assertions.assertFalse(res.isPassed());
        Assertions.assertEquals("FILTERED_OUT", res.getDecision());
        Assertions.assertEquals(1, res.getEditDistance());
    }
}
