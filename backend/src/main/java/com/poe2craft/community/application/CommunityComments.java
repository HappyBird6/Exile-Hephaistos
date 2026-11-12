package com.poe2craft.community.application;

import java.util.*;

public interface CommunityComments {
  record Comment(
      UUID id,
      UUID authorId,
      String author,
      String body,
      long revision,
      String createdAt,
      String updatedAt) {}

  record Page(List<Comment> comments, long total, int page, int pageSize) {}

  Page list(UUID post, int page);

  Comment create(UUID post, SessionBoard.Actor actor, UUID id, String body);

  Comment update(UUID post, UUID id, SessionBoard.Actor actor, String body, long revision);

  void delete(UUID post, UUID id, SessionBoard.Actor actor, long revision);
}
