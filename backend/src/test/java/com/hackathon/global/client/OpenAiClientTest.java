package com.hackathon.global.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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
}
