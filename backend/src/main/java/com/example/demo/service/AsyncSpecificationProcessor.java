package com.example.demo.service;

import com.example.demo.domain.Specification;
import com.example.demo.dto.SuggestionDto;
import com.example.demo.repository.SpecificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class AsyncSpecificationProcessor {
    private static final Logger logger = LoggerFactory.getLogger(AsyncSpecificationProcessor.class);

    private final SpecificationRepository specificationRepository;
    private final AiService aiService;
    private final SuggestionService suggestionService;

    public AsyncSpecificationProcessor(SpecificationRepository specificationRepository, AiService aiService, SuggestionService suggestionService) {
        this.specificationRepository = specificationRepository;
        this.aiService = aiService;
        this.suggestionService = suggestionService;
    }

    @Async
    @Transactional
    public void processSpecification(Long specId) {
        Optional<Specification> maybe = specificationRepository.findById(specId);
        if (maybe.isEmpty()) {
            logger.warn("Spec not found for async processing: {}", specId);
            return;
        }
        Specification spec = maybe.get();
        try {
            logger.info("Async processing spec {} for team {}", spec.getId(), spec.getTeamId());
            // 1. Generate optimal WBS without forcing language constraints
            List<SuggestionDto.Create> suggestions = aiService.generateSuggestions(spec.getTeamId(), spec.getId(), spec.getSpecText());
            
            // 2. Translate if user preferred language is set
            if (spec.getLanguage() != null && !spec.getLanguage().isBlank()) {
                logger.info("Translating spec {} suggestions to language {}", spec.getId(), spec.getLanguage());
                suggestions = aiService.translateSuggestions(suggestions, spec.getLanguage());
            }

            for (SuggestionDto.Create sreq : suggestions) {
                // link back to spec
                if (sreq.getSourceId() == null) sreq.setSourceId(String.valueOf(spec.getId()));
                suggestionService.create(spec.getTeamId(), sreq);
            }
            spec.setStatus("READY");
            specificationRepository.save(spec);
            logger.info("Spec {} processed, {} suggestions created", spec.getId(), suggestions.size());
        } catch (Exception e) {
            logger.error("Failed to process spec {}: {}", spec.getId(), e.getMessage(), e);
            spec.setStatus("ERROR");
            specificationRepository.save(spec);
        }
    }
}
