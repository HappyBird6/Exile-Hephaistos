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

/** Uses the same league order and exchange overview endpoints as poe.ninja's PoE2 website. */
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
