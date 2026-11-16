package com.poe2craft.craftingsupport.rag.infrastructure;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.poe2craft.configuration.BackendSettings;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

class RedisRagCallBudgetTest {
  @SuppressWarnings("unchecked")
  @Test
  void storageErrorsAndDeniedAdmissionFailClosed() {
    var redis = mock(StringRedisTemplate.class);
    var budget =
        new RedisRagCallBudget(
            redis,
            BackendSettings.defaults().supportRag(),
            Clock.fixed(Instant.parse("2026-11-16T16:00:00Z"), ZoneOffset.UTC));
    when(redis.execute(any(RedisScript.class), anyList(), anyString(), anyString())).thenReturn(0L);
    assertThatThrownBy(budget::acquire).hasMessage("RAG_CALL_LIMIT");
    when(redis.execute(any(RedisScript.class), anyList(), anyString(), anyString()))
        .thenThrow(new IllegalStateException("private connection"));
    assertThatThrownBy(budget::acquire).hasMessage("RAG_BUDGET_UNAVAILABLE");
    when(redis.execute(any(RedisScript.class), anyList(), anyString(), anyString())).thenReturn(1L);
    assertThatCode(budget::acquire).doesNotThrowAnyException();
    verify(redis, times(3))
        .execute(
            any(RedisScript.class),
            eq(
                java.util.List.of(
                    "support-rag:{budget}:2026-11-16", "support-rag:{budget}:cooldown")),
            eq("20"),
            eq("30000"));
  }
}
