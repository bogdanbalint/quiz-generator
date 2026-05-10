package com.quiz_generator.service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseOutputText;
import com.quiz_generator.domain.model.AiGeneratedQuestion;
import com.quiz_generator.domain.model.CreateQuizRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OpenAiQuizGenerationService {

    private final OpenAIClient openAIClient;
    private final ObjectMapper objectMapper;
    private final String model;

    public OpenAiQuizGenerationService(
            @Value("${app.openai.api-key}") String apiKey,
            @Value("${app.openai.model}") String model,
            ObjectMapper objectMapper
    ) {
        this.openAIClient = OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
        this.model = model;
        this.objectMapper = objectMapper;
    }

    public List<AiGeneratedQuestion> generateQuestions(CreateQuizRequest request) {
        String prompt = buildPrompt(request);

        ResponseCreateParams params = ResponseCreateParams.builder()
                .model(model)
                .input(prompt)
                .build();

        Response response = openAIClient.responses().create(params);

        String content = response.output().stream()
                .flatMap(item -> item.message().stream())
                .flatMap(message -> message.content().stream())
                .flatMap(contentItem -> contentItem.outputText().stream())
                .map(ResponseOutputText::text)
                .reduce("", (a, b) -> a + b);

        try {
            return objectMapper.readValue(content, new TypeReference<List<AiGeneratedQuestion>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse AI response into quiz questions", e);
        }
    }

    private String buildPrompt(CreateQuizRequest request) {
        String sourceText = request.getSourceText() == null ? "" : request.getSourceText().trim();

        return """
                Generate exactly %d multiple-choice quiz questions.

                Topic: %s
                Difficulty: %s
                Source text: %s

                Return ONLY valid JSON.
                Return a JSON array of objects.
                Each object must have exactly these fields:
                - questionText
                - optionA
                - optionB
                - optionC
                - optionD
                - correctAnswer
                - explanation

                Rules:
                - correctAnswer must be one of: A, B, C, D
                - questions must be clear and unambiguous
                - exactly 4 answer options for each question
                - only one correct answer
                - explanation should be short but useful
                - do not include markdown
                - do not include code fences
                - do not include any text before or after the JSON
                """.formatted(
                request.getQuestionCount(),
                request.getTopic().trim(),
                request.getDifficulty(),
                sourceText.isBlank() ? "N/A" : sourceText
        );
    }
}