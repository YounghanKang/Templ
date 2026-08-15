package com.example.demo.analysis.openai;

import com.example.demo.analysis.AnalysisCommand;
import com.example.demo.analysis.ChangeType;
import com.example.demo.analysis.ContextAnalysisPort;
import com.example.demo.analysis.ContextAnalysisResult;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponseCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OpenAiContextAnalysisAdapter
        implements ContextAnalysisPort {

    private final OpenAIClient openAIClient;

    private final OpenAiPromptBuilder promptBuilder =
            new OpenAiPromptBuilder();


    @Value("${app.openai.model:gpt-5.6-sol}")
    private String model;


    @Override
    public ContextAnalysisResult analyze(
            AnalysisCommand command
    ) {

        String prompt =
                promptBuilder.build(
                        command
                );


        StructuredResponseCreateParams<OpenAiAnalysisResponse> params =
                ResponseCreateParams
                        .builder()
                        .input(prompt)
                        .text(OpenAiAnalysisResponse.class)
                        .model(model)
                        .build();


        OpenAiAnalysisResponse aiResponse =
                openAIClient
                        .responses()
                        .create(params)
                        .output()
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
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "OpenAI Structured Output 응답이 없습니다."
                                        )
                        );


        return convert(
                aiResponse
        );
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
                response.taskId(),
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