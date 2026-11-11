package com.poe2craft.admin.application;

import java.nio.file.Path;
import java.time.*;
import java.util.*;

public interface RequestLogRepository {
  ZoneId SEOUL = ZoneId.of("Asia/Seoul");

  record Filters(
      LocalDate date,
      String kind,
      String ip,
      String visitor,
      String route,
      Integer status,
      int page) {}

  void insert(List<RequestEvent> events);

  Map<String, Object> query(Filters filters);

  boolean archiveBefore(Instant cutoff, int limit);

  Map<String, Object> archives(int page);

  Path download(UUID id);
}
