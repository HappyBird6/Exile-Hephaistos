package com.poe2craft.shared.economy.api.dto;

/**
 * revision은 시세 판본, nextRefreshAllowedAt은 다시 갱신할 수 있는 시각이다. stale, unavailable, refreshing은 각각 만료·이용
 * 불가·갱신 중 상태다.
 */
public record Status(
    String revision,
    java.time.Instant nextRefreshAllowedAt,
    boolean stale,
    boolean unavailable,
    boolean refreshing) {}
