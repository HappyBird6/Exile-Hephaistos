package com.poe2craft.admin.application;

import java.time.Instant;
import java.util.UUID;

/**
 * 본문·자격 증명·개인 아이템 원문을 제외한 요청 기록 한 건이다.
 *
 * <p>기존 계약: No request bodies, query strings, authorization, cookies or exception messages are
 * stored.
 *
 * <p>필드·상수·record 구성요소:
 *
 * <ul>
 *   <li>{@code id}: 이 값의 식별자 ({@code UUID}).
 *   <li>{@code occurredAt}: occurred·기준 ({@code Instant}).
 *   <li>{@code method}: 제작방법 ({@code String}).
 *   <li>{@code path}: 경로 ({@code String}).
 *   <li>{@code route}: 경로 ({@code String}).
 *   <li>{@code status}: 현재 처리 상태 ({@code int}).
 *   <li>{@code durationMs}: 소요시간·Ms ({@code long}).
 *   <li>{@code clientIp}: 클라이언트·Ip ({@code String}).
 *   <li>{@code visitorId}: visitor·식별자 ({@code String}).
 *   <li>{@code pageViews}: 페이지·Views ({@code int}).
 *   <li>{@code errorType}: 오류·타입 ({@code String}).
 *   <li>{@code userAgent}: 사용자·에이전트 ({@code String}).
 * </ul>
 */
public record RequestEvent(
    UUID id,
    Instant occurredAt,
    String method,
    String path,
    String route,
    int status,
    long durationMs,
    String clientIp,
    String visitorId,
    int pageViews,
    String errorType,
    String userAgent) {}
