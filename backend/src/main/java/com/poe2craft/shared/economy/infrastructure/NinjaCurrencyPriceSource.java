package com.poe2craft.shared.economy.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.economy.application.CurrencyPriceSource;
import com.poe2craft.shared.economy.domain.PriceSnapshot;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/**
 * poe.ninja의 리그와 교환 시세 응답을 읽어 가격 스냅샷으로 변환한다.
 *
 * <p>기존 계약: Uses the same league order and exchange overview endpoints as poe.ninja's PoE2 website.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code http}: http ({@code HttpClient}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code root}: 시작점 ({@code URI}).
 *   <li>{@code timeout}: 제한시간 ({@code Duration}).
 *   <li>{@code userAgent}: 사용자·에이전트 ({@code String}).
 *   <li>{@code categories}: 분류목록 ({@code List<String>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code NinjaCurrencyPriceSource(http, json, root, timeout, userAgent, categories)}: 전달받은
 *       값으로 구성원을 초기화한다. 입력: {@code http}(http), {@code json}(JSON), {@code root}(시작점), {@code
 *       timeout}(제한시간), {@code userAgent}(사용자·에이전트), {@code categories}(분류목록).
 *   <li>{@code fetch(collectedAt)}: fetch 처리에서 {@code index.path}, {@code candidate.path}을(를) 사용한다.
 *       입력: {@code collectedAt}(collected·기준). 반환: {@code PriceSnapshot}. 사용하는 예외: {@code
 *       IOException}, {@code InterruptedException}, {@code Exception}.
 *   <li>{@code overview(league, category)}: overview 처리에서 {@code URLEncoder.encode}을(를) 사용한다. 입력:
 *       {@code league}(league), {@code category}(분류). 반환: {@code JsonNode}. 사용하는 예외: {@code
 *       IOException}, {@code InterruptedException}.
 *   <li>{@code rate(core, currency)}: rate 처리에서 {@code core.path}, {@code node.isNumber}을(를) 사용한다.
 *       입력: {@code core}(core), {@code currency}(화폐). 반환: {@code BigDecimal}. 사용하는 예외: {@code
 *       IOException}.
 *   <li>{@code merge(data, exaltedPerDivine, quotes)}: merge 처리에서 {@code data.path}, {@code
 *       item.path}을(를) 사용한다. 입력: {@code data}(데이터), {@code exaltedPerDivine}(엑잘티드·단위별·신성), {@code
 *       quotes}(quotes). 사용하는 예외: {@code IOException}.
 *   <li>{@code get(path)}: 키 또는 위치에 해당하는 값을 조회한다. 입력: {@code path}(경로). 반환: {@code JsonNode}. 사용하는
 *       예외: {@code IOException}, {@code InterruptedException}.
 * </ul>
 */
public final class NinjaCurrencyPriceSource implements CurrencyPriceSource {
  private final HttpClient http;
  private final ObjectMapper json;
  private final URI root;
  private final Duration timeout;
  private final String userAgent;
  private final List<String> categories;

  public NinjaCurrencyPriceSource(
      HttpClient http,
      ObjectMapper json,
      URI root,
      Duration timeout,
      String userAgent,
      List<String> categories) {
    this.http = http;
    this.json = json;
    this.root = root;
    this.timeout = timeout;
    this.userAgent = userAgent;
    this.categories = List.copyOf(categories);
  }

  @Override
  public PriceSnapshot fetch(Instant collectedAt) throws Exception {
    var index = get("/poe2/api/data/index-state");
    String league = null;
    for (var candidate : index.path("economyLeagues")) {
      var name = candidate.path("displayName").asText();
      if (candidate.path("hardcore").isBoolean()
          && !candidate.path("hardcore").asBoolean()
          && !name.isBlank()
          && !Set.of("Standard", "Hardcore").contains(name)) {
        league = name;
        break;
      }
    }
    if (league == null) throw new IOException("No current softcore trade league");
    var currency = overview(league, "Currency");
    var core = currency.path("core");
    var divineRate = rate(core, "divine");
    var exaltedRate = rate(core, "exalted");
    var ratio = exaltedRate.divide(divineRate, MathContext.DECIMAL64);
    var quotes = new TreeMap<String, PriceSnapshot.Quote>();
    merge(currency, ratio, quotes);
    if (!quotes.containsKey("Divine Orb") || !quotes.containsKey("Exalted Orb"))
      throw new IOException("Missing reference currency prices");
    final String selectedLeague = league;
    try (var executor = Executors.newFixedThreadPool(4)) {
      var tasks = new ArrayList<Future<JsonNode>>();
      for (var category : categories) {
        if (!category.equals("Currency"))
          tasks.add(executor.submit(() -> overview(selectedLeague, category)));
      }
      for (var task : tasks) {
        try {
          merge(task.get(), ratio, quotes);
        } catch (ExecutionException unavailableCategory) {
          if (unavailableCategory.getCause() instanceof InterruptedException)
            throw new InterruptedException();
          // Missing categories stay unpriced; never recycle another league's quotes or use zero.
        }
      }
    }
    quotes.put("Divine Orb", new PriceSnapshot.Quote("Divine Orb", BigDecimal.ONE, ratio));
    quotes.put(
        "Exalted Orb",
        new PriceSnapshot.Quote(
            "Exalted Orb", BigDecimal.ONE.divide(ratio, MathContext.DECIMAL64), BigDecimal.ONE));
    return new PriceSnapshot(league, collectedAt, ratio, quotes);
  }

  private JsonNode overview(String league, String category)
      throws IOException, InterruptedException {
    return get(
        "/poe2/api/economy/exchange/current/overview?league="
            + URLEncoder.encode(league, StandardCharsets.UTF_8)
            + "&type="
            + URLEncoder.encode(category, StandardCharsets.UTF_8));
  }

  private BigDecimal rate(JsonNode core, String currency) throws IOException {
    if (core.path("primary").asText().equals(currency)) return BigDecimal.ONE;
    var node = core.path("rates").path(currency);
    if (!node.isNumber() || node.decimalValue().signum() <= 0)
      throw new IOException("Invalid currency rate");
    return node.decimalValue();
  }

  private void merge(
      JsonNode data, BigDecimal exaltedPerDivine, Map<String, PriceSnapshot.Quote> quotes)
      throws IOException {
    if (!data.path("items").isArray() || !data.path("lines").isArray())
      throw new IOException("Invalid price response");
    var divineRate = rate(data.path("core"), "divine");
    var names = new HashMap<String, String>();
    for (var item : data.path("items")) {
      var id = item.path("id").asText();
      var name = item.path("name").asText();
      if (!id.isBlank() && !name.isBlank()) names.put(id, name);
    }
    for (var line : data.path("lines")) {
      var name = names.get(line.path("id").asText());
      var value = line.path("primaryValue");
      if (name == null || !value.isNumber() || value.decimalValue().signum() <= 0) continue;
      var divine = value.decimalValue().multiply(divineRate, MathContext.DECIMAL64);
      quotes.put(
          name,
          new PriceSnapshot.Quote(
              name, divine, divine.multiply(exaltedPerDivine, MathContext.DECIMAL64)));
    }
  }

  private JsonNode get(String path) throws IOException, InterruptedException {
    var request =
        HttpRequest.newBuilder(root.resolve(path))
            .timeout(timeout)
            .header("User-Agent", userAgent)
            .header("Accept", "application/json")
            .header("Cache-Control", "no-cache")
            .GET()
            .build();
    var pending = http.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
    try {
      var response = pending.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
      if (response.statusCode() != 200) throw new IOException("Price provider unavailable");
      if (response.body().length > 2_000_000) throw new IOException("Price response too large");
      return json.readTree(response.body());
    } catch (TimeoutException expired) {
      pending.cancel(true);
      throw new IOException("Price request timed out", expired);
    } catch (ExecutionException failed) {
      throw new IOException("Price provider unavailable", failed.getCause());
    } catch (InterruptedException interrupted) {
      pending.cancel(true);
      throw interrupted;
    }
  }
}
