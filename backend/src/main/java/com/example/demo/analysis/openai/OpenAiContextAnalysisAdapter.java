package com.example.demo.analysis.openai;

import com.example.demo.analysis.AnalysisCommand;
import com.example.demo.analysis.ChangeType;
import com.example.demo.analysis.ContextAnalysisPort;
import com.example.demo.analysis.ContextAnalysisResult;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OpenAiContextAnalysisAdapter
        implements ContextAnalysisPort {

    private final OpenAIClient openAIClient;

    private final JsonMapper objectMapper;

    private final OpenAiPromptBuilder promptBuilder =
            new OpenAiPromptBuilder();


    @Value("${app.openai.model:gpt-5.6}")
    private String model;


    @Override
    public ContextAnalysisResult analyze(
            AnalysisCommand command
    ) {

        String prompt =
                promptBuilder.build(
                        command
                );


        ResponseCreateParams params =
                ResponseCreateParams
                        .builder()
                        .model(model)
                        .input(prompt)
                        .build();


        Response response =
                openAIClient
                        .responses()
                        .create(params);


        String outputText =
                extractOutputText(
                        response
                );


        OpenAiAnalysisResponse aiResponse =
                parseResponse(
                        outputText
                );


        return convert(
                aiResponse
        );
    }


    private String extractOutputText(
            Response response
    ) {

        StringBuilder text =
                new StringBuilder();


        response.output()
                .stream()
                .flatMap(
                        item ->
                                item.message()
                                        .stream()
                )
                .flatMap(
                        message ->
                                message.content()
                                        .stream()
                )
                .flatMap(
                        content ->
                                content.outputText()
                                        .stream()
                )
                .forEach(
                        outputText ->
                                text.append(
                                        outputText.text()
                                )
                );


        if (text.isEmpty()) {

            throw new IllegalStateException(
                    "OpenAI 응답에 text가 없습니다."
            );
        }


        return text.toString();
    }


    private OpenAiAnalysisResponse parseResponse(
            String rawText
    ) {

        String json =
                cleanJson(
                        rawText
                );


        try {

            return objectMapper.readValue(
                    json,
                    OpenAiAnalysisResponse.class
            );

        } catch (JacksonException exception) {

            throw new IllegalStateException(
                    "OpenAI 분석 결과 JSON 파싱에 실패했습니다.",
                    exception
            );
        }
    }


    private String cleanJson(
            String rawText
    ) {

        if (rawText == null) {

            throw new IllegalStateException(
                    "OpenAI 응답이 null입니다."
            );
        }


        String cleaned =
                rawText.trim();


        if (
                cleaned.startsWith(
                        "```json"
                )
        ) {

            cleaned =
                    cleaned.substring(
                            7
                    );
        } else if (
                cleaned.startsWith(
                        "```"
                )
        ) {

            cleaned =
                    cleaned.substring(
                            3
                    );
        }


        if (
                cleaned.endsWith(
                        "```"
                )
        ) {

            cleaned =
                    cleaned.substring(
                            0,
                            cleaned.length() - 3
                    );
        }


        return cleaned.trim();
    }


    private ContextAnalysisResult convert(
            OpenAiAnalysisResponse response
    ) {

        ChangeType changeType =
                null;


        if (
                response.changeType()
                        != null
                        &&
                        !response.changeType()
                                .isBlank()
        ) {

            try {

                changeType =
                        ChangeType.valueOf(
                                response.changeType()
                        );

            } catch (IllegalArgumentException exception) {

                throw new IllegalStateException(
                        "지원하지 않는 changeType입니다: "
                                + response.changeType(),
                        exception
                );
            }
        }


        return new ContextAnalysisResult(
                response.meaningfulChange(),
                changeType,
                response.summary(),
                response.riskScore(),
                response.confidence(),
                response.primaryNodeId(),
                safeUuidList(
                        response.impactedNodeIds()
                ),
                safeStringList(
                        response.proposedActions()
                ),
                safeStringList(
                        response.openQuestions()
                )
        );
    }


    private List<UUID> safeUuidList(
            List<UUID> values
    ) {

        if (values == null) {
            return List.of();
        }


        return List.copyOf(
                new ArrayList<>(
                        values
                )
        );
    }


    private List<String> safeStringList(
            List<String> values
    ) {

        if (values == null) {
            return List.of();
        }


        return List.copyOf(
                new ArrayList<>(
                        values
                )
        );
    }
}