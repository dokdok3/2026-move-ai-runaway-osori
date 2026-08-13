package com.hackathon.global.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiClientTest {

    @Test
    void generateCallsResponsesApiAndReturnsOutputText() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiClient client = new OpenAiClient(
                builder, "https://api.openai.com/v1", "test-key", "gpt-5.6");

        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("gpt-5.6"))
                .andExpect(jsonPath("$.input").value("안녕하세요"))
                .andRespond(withSuccess("""
                        {
                          "output": [
                            {
                              "type": "message",
                              "content": [
                                {"type": "output_text", "text": "반갑습니다"}
                              ]
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.generate("안녕하세요")).isEqualTo("반갑습니다");
        server.verify();
    }

    @Test
    void generateStructuredSendsStrictSchemaAndReturnsJsonText() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiClient client = new OpenAiClient(
                builder, "https://api.openai.com/v1", "test-key", "gpt-5.6-luna");
        var schema = new ObjectMapper().readTree("""
                {"type":"object","properties":{"cargoType":{"type":"string"}},
                 "required":["cargoType"],"additionalProperties":false}
                """);

        server.expect(requestTo("https://api.openai.com/v1/responses"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("gpt-5.6-luna"))
                .andExpect(jsonPath("$.instructions").value("화물 정보만 추출"))
                .andExpect(jsonPath("$.input").value("referenceDate: 2026-08-13"))
                .andExpect(jsonPath("$.text.format.type").value("json_schema"))
                .andExpect(jsonPath("$.text.format.name").value("freight_request"))
                .andExpect(jsonPath("$.text.format.strict").value(true))
                .andExpect(jsonPath("$.text.format.schema.additionalProperties").value(false))
                .andRespond(withSuccess("""
                        {
                          "output": [{
                            "type": "message",
                            "content": [{"type": "output_text", "text": "{\\\"cargoType\\\":\\\"GENERAL\\\"}"}]
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.generateStructured(
                "화물 정보만 추출", "referenceDate: 2026-08-13", schema))
                .isEqualTo("{\"cargoType\":\"GENERAL\"}");
        server.verify();
    }
}
