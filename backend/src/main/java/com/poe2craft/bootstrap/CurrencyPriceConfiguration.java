package com.poe2craft.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.shared.economy.application.*;
import com.poe2craft.shared.economy.infrastructure.*;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
public class CurrencyPriceConfiguration {
  @Bean
  CurrencyPriceService currencyPriceService(
      BackendSettings settings, StringRedisTemplate redis, ObjectMapper json) {
    var p = settings.prices();
    var http = HttpClient.newBuilder().connectTimeout(p.connectTimeout());
    if (!p.proxyUrl().isBlank()) {
      var proxy = URI.create(p.proxyUrl());
      if (proxy.getHost() == null)
        throw new IllegalArgumentException("Invalid currency proxy configuration");
      http.proxy(
          ProxySelector.of(
              new InetSocketAddress(proxy.getHost(), proxy.getPort() > 0 ? proxy.getPort() : 80)));
    }
    var source =
        new NinjaCurrencyPriceSource(
            http.build(),
            json,
            URI.create(p.sourceUrl()),
            p.requestTimeout(),
            p.userAgent(),
            p.categories());
    return new CurrencyPriceService(
        source,
        new RedisCurrencyPriceCache(redis, json, p.cacheKey()),
        Clock.systemUTC(),
        p.refreshInterval(),
        p.minimumRefreshInterval(),
        p.refreshLease());
  }

  @Bean
  PriceSchedule priceSchedule(CurrencyPriceService prices) {
    return new PriceSchedule(prices);
  }

  public static final class PriceSchedule {
    private final CurrencyPriceService prices;

    PriceSchedule(CurrencyPriceService prices) {
      this.prices = prices;
    }

    @Scheduled(
        fixedDelayString = "${backend.prices.check-interval}",
        initialDelayString = "${backend.prices.initial-delay}")
    public void collect() {
      prices.get(false);
    }
  }
}
