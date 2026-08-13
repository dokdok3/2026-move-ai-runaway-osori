package com.hackathon.global.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class OpenAiClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public OpenAiClient(
            RestClient.Builder restClientBuilder,
            @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.model:gpt-5.6}") String model
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.model = model;
    }

    /**
     * OpenAI Responses API에 프롬프트를 보내고 생성된 텍스트를 반환한다.
     */
    public String generate(String prompt) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("OPENAI_API_KEY가 설정되지 않았습니다.");
        }
        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException("프롬프트는 비어 있을 수 없습니다.");
        }

        try {
            JsonNode response = restClient.post()
                    .uri("/responses")
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "model", model,
                            "input", prompt
                    ))
                    .retrieve()
                    .body(JsonNode.class);

            String outputText = extractOutputText(response);
            if (!StringUtils.hasText(outputText)) {
                throw new IllegalStateException("OpenAI 응답에 생성된 텍스트가 없습니다.");
            }
            return outputText;
        } catch (RestClientResponseException e) {
            throw new IllegalStateException(
                    "OpenAI API 호출에 실패했습니다. status=" + e.getStatusCode().value(), e);
        }
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) {
            return null;
        }

        JsonNode output = response.path("output");
        if (!output.isArray()) {
            return null;
        }

        List<String> texts = output.findValues("text").stream()
                .filter(JsonNode::isTextual)
                .map(JsonNode::asText)
                .filter(StringUtils::hasText)
                .toList();

        return texts.isEmpty() ? null : String.join("\n", texts);
    }
}
