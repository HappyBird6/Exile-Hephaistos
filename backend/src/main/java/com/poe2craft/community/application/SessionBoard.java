package com.poe2craft.community.application;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

public interface SessionBoard {
  record Actor(UUID account, UUID browser) {}

  record Draft(
      UUID id,
      String sourceId,
      String gameVersion,
      String title,
      String body,
      JsonNode film,
      String password,
      long revision) {}

  record Post(
      UUID id,
      UUID authorId,
      String author,
      String sourceId,
      String gameVersion,
      String baseId,
      String title,
      String body,
      JsonNode film,
      int sharedStep,
      long revision,
      String createdAt,
      String updatedAt) {}

  record Summary(
      UUID id,
      UUID authorId,
      String author,
      String gameVersion,
      String baseId,
      String title,
      int steps,
      String createdAt,
      String updatedAt,
      long revision,
      long downloads,
      long recommendations) {}

  record Page(List<Summary> posts, long total, int page, int pageSize) {}

  Post create(Actor actor, Draft draft);

  Post update(Actor actor, UUID id, Draft draft);

  Post editText(Actor actor, UUID id, String title, String body, long revision);

  void delete(Actor actor, UUID id, String password, long revision);

  Post get(UUID id);

  default Page list(String base, String version, UUID author, int page) {
    return list(base, version, author, page, "newest");
  }

  default Page list(String base, String version, UUID author, int page, String sort) {
    return list(base, version, author, page, sort, null);
  }

  Page list(String base, String version, UUID author, int page, String sort, String itemClass);

  List<Summary> mine(Actor actor, String source);

  Map<String, Object> options();
}
