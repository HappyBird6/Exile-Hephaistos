package com.poe2craft.shared.economy;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.economy.infrastructure.NinjaCurrencyPriceSource;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.*;

/**
 * poe.ninja의 리그와 교환 시세 응답을 읽어 가격 스냅샷으로 변환한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code server}: 서버 ({@code HttpServer}).
 *   <li>{@code index}: 색인 ({@code String}).
 *   <li>{@code currency}: 화폐 ({@code String}).
 *   <li>{@code essence}: 에센스 ({@code String}).
 *   <li>{@code categoryStatus}: 분류·상태; 초기값 {@code 200} ({@code int}).
 *   <li>{@code paths}: 경로목록 ({@code List<String>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code start()}: 각 검증에 필요한 독립 초기 상태와 의존성을 준비한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code stop()}: 검증에서 사용한 상태와 자원을 정리한다.
 *   <li>{@code source()}: 출처 값을 조회·구성한다. 반환: {@code NinjaCurrencyPriceSource}.
 *   <li>{@code choosesCurrentSoftcoreAndReturnsConsistentImageIndependentCurrencyPrices()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code unavailableCategoryIsUnpricedWhileReferenceRateRemainsUsable()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code missingCurrentLeagueDoesNotFallBackToStandardOrHardcore()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다.
 *   <li>{@code malformedReferencePricesDoNotBecomeInventedZeroOrReferenceQuotes()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code everyCollectionResolvesTheLeagueAgainAndDoesNotMixOldLeaguePrices()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 * </ul>
 */
class NinjaCurrencyPriceSourceTest {
  HttpServer server;
  String index =
      """
      {"economyLeagues":[{"displayName":"Hardcore Current","hardcore":true},
      {"displayName":"Standard","hardcore":false},{"displayName":"Current League","hardcore":false},
      {"displayName":"Old League","hardcore":false}]}
      """;
  String currency =
      """
      {"core":{"primary":"divine","rates":{"exalted":500}},
      "items":[{"id":"divine","name":"Divine Orb"},{"id":"exalted","name":"Exalted Orb"},{"id":"chaos","name":"Chaos Orb"}],
      "lines":[{"id":"divine","primaryValue":1},{"id":"exalted","primaryValue":0.002001},{"id":"chaos","primaryValue":0.1}]}
      """;
  String essence =
      """
      {"core":{"primary":"divine","rates":{"exalted":500}},"items":[{"id":"essence","name":"Perfect Essence of the Body"}],"lines":[{"id":"essence","primaryValue":1.25}]}
      """;
  int categoryStatus = 200;
  List<String> paths = new CopyOnWriteArrayList<>();

  @BeforeEach
  void start() throws Exception {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          var path = exchange.getRequestURI().toString();
          paths.add(path);
          String body =
              path.contains("index-state")
                  ? index
                  : path.contains("type=Currency") ? currency : essence;
          int status =
              !path.contains("index-state") && !path.contains("type=Currency")
                  ? categoryStatus
                  : 200;
          var bytes = body.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(status, bytes.length);
          exchange.getResponseBody().write(bytes);
          exchange.close();
        });
    server.start();
  }

  @AfterEach
  void stop() {
    server.stop(0);
  }

  NinjaCurrencyPriceSource source() {
    return new NinjaCurrencyPriceSource(
        HttpClient.newHttpClient(),
        new ObjectMapper(),
        URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
        Duration.ofSeconds(2),
        "Hephaistos-test",
        List.of("Currency", "Essences"));
  }

  @Test
  void choosesCurrentSoftcoreAndReturnsConsistentImageIndependentCurrencyPrices() throws Exception {
    var result = source().fetch(Instant.EPOCH);
    assertThat(result.league()).isEqualTo("Current League");
    assertThat(result.updatedAt()).isEqualTo(Instant.EPOCH);
    assertThat(result.exaltedPerDivine()).isEqualByComparingTo("500");
    assertThat(result.prices().get("Exalted Orb").exalted()).isEqualByComparingTo("1");
    assertThat(result.prices().get("Perfect Essence of the Body").exalted())
        .isEqualByComparingTo("625");
    assertThat(paths).anyMatch(path -> path.contains("league=Current+League"));
    assertThat(paths).noneMatch(path -> path.contains("Hardcore") || path.contains("Standard"));
  }

  @Test
  void unavailableCategoryIsUnpricedWhileReferenceRateRemainsUsable() throws Exception {
    categoryStatus = 503;
    var result = source().fetch(Instant.EPOCH);
    assertThat(result.prices())
        .containsKey("Divine Orb")
        .doesNotContainKey("Perfect Essence of the Body");
  }

  @Test
  void missingCurrentLeagueDoesNotFallBackToStandardOrHardcore() {
    index =
        "{\"economyLeagues\":[{\"displayName\":\"Standard\",\"hardcore\":false},{\"displayName\":\"Hardcore\",\"hardcore\":true}]}";
    assertThatThrownBy(() -> source().fetch(Instant.EPOCH)).isInstanceOf(java.io.IOException.class);
  }

  @Test
  void malformedReferencePricesDoNotBecomeInventedZeroOrReferenceQuotes() {
    currency = currency.replace("\"primaryValue\":1}", "\"primaryValue\":0}");
    assertThatThrownBy(() -> source().fetch(Instant.EPOCH)).isInstanceOf(java.io.IOException.class);
  }

  @Test
  void everyCollectionResolvesTheLeagueAgainAndDoesNotMixOldLeaguePrices() throws Exception {
    var source = source();
    source.fetch(Instant.EPOCH);
    index = index.replace("Current League", "Next League");
    var result = source.fetch(Instant.EPOCH.plusSeconds(1800));
    assertThat(result.league()).isEqualTo("Next League");
    assertThat(paths).anyMatch(path -> path.contains("league=Next+League"));
  }
}
