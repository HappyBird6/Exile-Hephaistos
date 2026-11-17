package com.poe2craft.shared.economy.api;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.poe2craft.shared.economy.application.CurrencyPriceService;
import com.poe2craft.shared.economy.domain.PriceSnapshot;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 제작 화폐 시세 조회 요청을 가격 서비스에 전달한다. 해당 동작과 실패 경계를 검증하는 테스트다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code unchangedSnapshotReturns304AndRefreshStatusDoesNotContainQuotes()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 * </ul>
 */
class CurrencyPriceControllerTest {
  @Test
  void unchangedSnapshotReturns304AndRefreshStatusDoesNotContainQuotes() throws Exception {
    var service = mock(CurrencyPriceService.class);
    var snapshot =
        new PriceSnapshot(
            "League",
            Instant.parse("2026-01-01T00:00:00Z"),
            new BigDecimal("500"),
            Map.of(
                "Exalted Orb",
                new PriceSnapshot.Quote("Exalted Orb", new BigDecimal("0.002"), BigDecimal.ONE)));
    when(service.current(false))
        .thenReturn(new CurrencyPriceService.View(snapshot, Instant.EPOCH, false, false, false));
    var mvc = MockMvcBuilders.standaloneSetup(new CurrencyPriceController(service)).build();
    String tag =
        mvc.perform(get("/api/v1/economy/prices"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getHeader("ETag");
    assertThat(tag).startsWith("W/\"");
    mvc.perform(get("/api/v1/economy/prices").header("If-None-Match", tag))
        .andExpect(status().isNotModified())
        .andExpect(content().string(""));
    mvc.perform(get("/api/v1/economy/prices/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.revision").value("League:2026-01-01T00:00:00Z"))
        .andExpect(jsonPath("$.snapshot").doesNotExist())
        .andExpect(header().string("Cache-Control", "no-store"));
    when(service.current(false))
        .thenReturn(
            new CurrencyPriceService.View(
                snapshot, Instant.now().plusSeconds(60), false, false, true));
    mvc.perform(get("/api/v1/economy/prices").header("If-None-Match", tag))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.refreshing").value(true));
    when(service.current(false))
        .thenReturn(new CurrencyPriceService.View(null, Instant.EPOCH, false, true, false));
    mvc.perform(get("/api/v1/economy/prices/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unavailable").value(true))
        .andExpect(jsonPath("$.revision").isEmpty());
  }
}
