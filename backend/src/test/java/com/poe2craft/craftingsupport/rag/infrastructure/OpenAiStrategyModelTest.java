package com.poe2craft.craftingsupport.rag.infrastructure;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.craftingsupport.rag.RagFixture;
import com.poe2craft.craftingsupport.rag.application.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.*;

class OpenAiStrategyModelTest {
  final RagFixture f = new RagFixture();
  HttpServer server;
  final AtomicInteger calls = new AtomicInteger(), budget = new AtomicInteger();
  final AtomicReference<String> request = new AtomicReference<>();
  int status = 200;
  String body = "";
  long delay;

  @BeforeEach
  void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          calls.incrementAndGet();
          request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          try {
            if (delay > 0) Thread.sleep(delay);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
          var bytes = body.getBytes(StandardCharsets.UTF_8);
          try {
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
          } finally {
            exchange.close();
          }
        });
    server.start();
    body =
        f.json.writeValueAsString(
            Map.of(
                "choices",
                List.of(
                    Map.of(
                        "finish_reason",
                        "stop",
                        "message",
                        Map.of("content", "{\"status\":\"COMPLETE\",\"choices\":[]}"))),
                "usage",
                Map.of("prompt_tokens", 42, "completion_tokens", 8)));
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  OpenAiStrategyModel model(Map<String, Object> settings, RagCallBudget admission) {
    return new OpenAiStrategyModel(
        f.json,
        f.settings(settings),
        "test-key",
        admission,
        HttpClient.newHttpClient(),
        URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/"));
  }

  StrategyModel.Result call(OpenAiStrategyModel model) {
    return model.propose(
        f.knowledge.select(StrategyKnowledge.Mode.ALL, "", 3),
        f.facts(),
        f.input().request().goal(),
        "ko");
  }

  @Test
  void sendsStructuredBoundedRequestWithoutReferenceAnswersAndCountsUsage() throws Exception {
    var result = call(model(Map.of(), budget::incrementAndGet));
    assertThat(result.status()).isEqualTo("COMPLETE");
    assertThat(result.inputTokens()).isEqualTo(42);
    var sent = f.json.readTree(request.get());
    assertThat(sent.path("store").asBoolean()).isFalse();
    assertThat(sent.path("response_format").path("json_schema").path("strict").asBoolean())
        .isTrue();
    var content = f.json.readTree(sent.path("messages").path(1).path("content").asText());
    assertThat(content.path("facts").has("reference")).isFalse();
    assertThat(content.path("goal")).isEqualTo(f.json.valueToTree(f.input().request().goal()));
    assertThat(content.path("documents").size()).isEqualTo(8);
    assertThat(calls).hasValue(1);
    assertThat(budget).hasValue(1);
  }

  @Test
  void dailyBudgetRejectionAndOversizePromptMakeNoRequest() {
    assertThatThrownBy(
            () ->
                call(
                    model(
                        Map.of(),
                        () -> {
                          throw new RagFailure(429, "RAG_CALL_LIMIT");
                        })))
        .hasMessage("RAG_CALL_LIMIT");
    assertThatThrownBy(() -> call(model(Map.of("maxPromptBytes", 1000), budget::incrementAndGet)))
        .hasMessage("RAG_CONTEXT_LIMIT");
    assertThat(calls).hasValue(0);
    assertThat(budget).hasValue(0);
  }

  @Test
  void rateLimitIsNotRetriedAndProviderSecretsAreNeverReturned() {
    status = 429;
    body = "sensitive provider detail";
    assertThatThrownBy(() -> call(model(Map.of(), budget::incrementAndGet)))
        .hasMessage("RAG_PROVIDER_RATE_LIMIT");
    assertThat(calls).hasValue(1);
  }

  @Test
  void truncatedRefusedInvalidAndOversizedResponsesAreRejected() {
    body = "{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"secret\"}}]}";
    assertThatThrownBy(() -> call(model(Map.of(), budget::incrementAndGet)))
        .hasMessage("RAG_INCOMPLETE_OUTPUT");
    body = "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"refusal\":\"reason\"}}]}";
    assertThatThrownBy(() -> call(model(Map.of(), budget::incrementAndGet)))
        .hasMessage("RAG_INCOMPLETE_OUTPUT");
    body = "broken";
    assertThatThrownBy(() -> call(model(Map.of(), budget::incrementAndGet)))
        .hasMessage("RAG_INVALID_PROVIDER_RESPONSE");
    body = "x".repeat(2048);
    assertThatThrownBy(() -> call(model(Map.of("maxResponseBytes", 1024), budget::incrementAndGet)))
        .hasMessage("RAG_INVALID_PROVIDER_RESPONSE");
  }

  @Test
  void wholeResponseTimeoutDoesNotRetry() {
    delay = 250;
    assertThatThrownBy(
            () -> call(model(Map.of("requestTimeout", "PT0.05S"), budget::incrementAndGet)))
        .isInstanceOf(RagFailure.class);
    assertThat(calls.get()).isLessThanOrEqualTo(1);
    assertThat(budget).hasValue(1);
  }
}
