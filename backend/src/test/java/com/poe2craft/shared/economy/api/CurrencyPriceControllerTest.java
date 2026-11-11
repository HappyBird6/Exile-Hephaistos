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
