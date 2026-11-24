package com.poe2craft.craftingsupport.llmtest.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * 실험과 같은 모델·스키마로 한 번 요청한다. key는 서버 인증에만 쓰고, endpoint와 HTTP 클라이언트는 재사용한다. configured는 키 유무를, send는
 * 완결된 답변을 반환한다.
 */
@Component
public class CardModelClient {
  private final ObjectMapper mapper;
  private final String key;
  private final URI endpoint;
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

  @Autowired
  public CardModelClient(ObjectMapper mapper, @Value("${OPENAI_API_KEY:}") String key) {
    this(mapper, key, URI.create("https://api.openai.com/v1/chat/completions"));
  }

  CardModelClient(ObjectMapper mapper, String key, URI endpoint) {
    this.mapper = mapper;
    this.key = key.strip();
    this.endpoint = endpoint;
  }

  boolean configured() {
    return !key.isBlank();
  }

  String send(ExperimentCards.Preview preview, JsonNode schema) {
    var body =
        Map.of(
            "model",
            "gpt-4.1-mini-2025-04-14",
            "temperature",
            0,
            "max_completion_tokens",
            3200,
            "store",
            false,
            "messages",
            List.of(
                Map.of("role", "system", "content", preview.prompt()),
                Map.of("role", "user", "content", preview.inputJson())),
            "response_format",
            Map.of(
                "type",
                "json_schema",
                "json_schema",
                Map.of("name", "candidate_routes", "strict", true, "schema", schema)));
    try {
      var request =
          HttpRequest.newBuilder(endpoint)
              .timeout(Duration.ofSeconds(60))
              .header("Authorization", "Bearer " + key)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofByteArray(mapper.writeValueAsBytes(body)))
              .build();
      var result = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (result.statusCode() == 429)
        throw new ResponseStatusException(
            HttpStatus.TOO_MANY_REQUESTS, "Model request limit reached");
      if (result.statusCode() != 200) throw unavailable();
      var response = mapper.readTree(result.body());
      if (response == null) throw unavailable();
      var choice = response.path("choices").path(0);
      var message = choice.path("message");
      var answer = message.path("content").asText("");
      if (!"stop".equals(choice.path("finish_reason").asText())
          || !message.path("refusal").asText("").isBlank()
          || answer.isBlank()) throw unavailable();
      return answer;
    } catch (HttpTimeoutException e) {
      throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "Model timed out");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw unavailable();
    } catch (IOException e) {
      throw unavailable();
    }
  }

  private static ResponseStatusException unavailable() {
    return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Model response unavailable");
  }
}
