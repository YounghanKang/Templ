package com.example.demo.service;

import com.example.demo.dto.SuggestionDto;

import java.util.List;

public interface AiService {
    List<SuggestionDto.Create> generateSuggestions(String teamId, Long specId, String specText);

    default List<SuggestionDto.Create> generateSuggestions(String teamId, Long specId, String specText, String userFeedback, Long baseSuggestionId) {
        return generateSuggestions(teamId, specId, specText);
    }
}
