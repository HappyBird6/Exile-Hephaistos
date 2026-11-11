package com.poe2craft.admin.application;

import java.time.Instant;
import java.util.UUID;

/** No request bodies, query strings, authorization, cookies or exception messages are stored. */
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
