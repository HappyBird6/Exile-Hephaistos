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

/**
 * 시세 수집기와 Redis 캐시 및 갱신 일정을 연결한다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code currencyPriceCache(settings, redis, json)}: 화폐·시세·캐시 처리에서 {@code
 *       settings.prices}을(를) 사용한다. 입력: {@code settings}(운영설정), {@code redis}(Redis), {@code
 *       json}(JSON). 반환: {@code CurrencyPriceCache}.
 *   <li>{@code currencyPriceService(settings, cache, json)}: 화폐·시세·서비스 처리에서 {@code
 *       settings.prices}, {@code HttpClient.newBuilder}을(를) 사용한다. 입력: {@code settings}(운영설정),
 *       {@code cache}(캐시), {@code json}(JSON). 반환: {@code CurrencyPriceService}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code priceSchedule(prices)}: 시세·Schedule의 입력에 따른 분기·결과를 처리한다. 입력: {@code prices}(시세목록).
 *       반환: {@code PriceSchedule}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code PriceSchedule}: {@code CurrencyPriceConfiguration} 내부의 시세·Schedule 처리를 맡는다.
 * </ul>
 */
@Configuration
@EnableScheduling
public class CurrencyPriceConfiguration {
  @Bean
  CurrencyPriceCache currencyPriceCache(
      BackendSettings settings, StringRedisTemplate redis, ObjectMapper json) {
    return new RedisCurrencyPriceCache(redis, json, settings.prices().cacheKey());
  }

  @Bean
  CurrencyPriceService currencyPriceService(
      BackendSettings settings, CurrencyPriceCache cache, ObjectMapper json) {
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
        cache,
        Clock.systemUTC(),
        p.refreshInterval(),
        p.minimumRefreshInterval(),
        p.refreshLease());
  }

  @Bean
  PriceSchedule priceSchedule(CurrencyPriceService prices) {
    return new PriceSchedule(prices);
  }

  /**
   * {@code CurrencyPriceConfiguration} 내부의 시세·Schedule 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code prices}: 시세목록 ({@code CurrencyPriceService}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code PriceSchedule(prices)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code prices}(시세목록).
   *   <li>{@code collect()}: collect 처리에서 {@code prices.get}을(를) 사용한다.
   * </ul>
   */
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
