package com.example.demo.service;

import com.example.demo.dto.EventFilterDto;

public interface EventFilterService {
    EventFilterDto.FilterResultResponse evaluate(String teamId, EventFilterDto.FilterRequest request);
}
