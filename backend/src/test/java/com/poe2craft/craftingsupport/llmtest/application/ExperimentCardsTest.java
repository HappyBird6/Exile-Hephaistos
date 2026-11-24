package com.poe2craft.craftingsupport.llmtest.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

/** 보강 카드의 표시·전송 메시지 일치와 키 누락·잘못된 카드에서 호출 예산을 보존하는지 검증한다. */
class ExperimentCardsTest {
  private final ObjectMapper mapper = new ObjectMapper();
  private final CardCallBudget budget = mock(CardCallBudget.class);

  @Test
  void sendsDisplayedMessagesAndSelectedSchemaOnlyOnce() throws Exception {
    var received = new AtomicReference<String>();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          received.set(
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          var response =
              """
          {"choices":[{"finish_reason":"stop","message":{"content":"{\\"routes\\":[]}"}}]}
          """
                  .getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, response.length);
          try (var out = exchange.getResponseBody()) {
            out.write(response);
          }
        });
    server.start();
    try {
      var client =
          new CardModelClient(
              mapper,
              "local-test",
              URI.create("http://127.0.0.1:" + server.getAddress().getPort()));
      var catalog = new ExperimentCards(mapper, client, budget);
      assertThat(catalog.index().cards())
          .hasSize(50)
          .extracting(ExperimentCards.GoalCard::id)
          .doesNotHaveDuplicates();
      var preview = catalog.preview("if-1");
      assertThat(received.get()).isNull();
      var reply = catalog.send(new ExperimentCards.Selection("if-1"));
      verify(budget).reserve();
      assertThat(reply.answer()).isEqualTo("{\"routes\":[]}");
      var body = mapper.readTree(received.get());
      assertThat(body.at("/messages/0/content").asText()).isEqualTo(preview.prompt());
      assertThat(body.at("/messages/1/content").asText()).isEqualTo(preview.inputJson());
      assertThat(mapper.readTree(preview.inputJson()).has("modifierFacts")).isTrue();
      assertThat(body.at("/response_format/json_schema/strict").asBoolean()).isTrue();
      assertThat(body.at("/response_format/json_schema/schema/type").asText()).isEqualTo("object");
    } finally {
      server.stop(0);
    }
  }

  @Test
  void unconfiguredServerStillPreviewsAndNeverReservesACall() throws Exception {
    var catalog = new ExperimentCards(mapper, new CardModelClient(mapper, ""), budget);
    assertThat(catalog.index().configured()).isFalse();
    assertThat(catalog.preview("if-1").prompt()).isNotBlank();
    assertThatExceptionOfType(ResponseStatusException.class)
        .isThrownBy(() -> catalog.send(new ExperimentCards.Selection("absent")))
        .matches(e -> e.getStatusCode().value() == 404);
    assertThatExceptionOfType(ResponseStatusException.class)
        .isThrownBy(() -> catalog.send(new ExperimentCards.Selection("if-1")))
        .matches(e -> e.getStatusCode().value() == 503);
    verifyNoInteractions(budget);
  }
}
