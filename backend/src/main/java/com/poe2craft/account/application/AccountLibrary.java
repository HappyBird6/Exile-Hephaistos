package com.poe2craft.account.application;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.UUID;

/** Owner comes from server authentication, never from an uploaded document. */
public interface AccountLibrary {
  record Account(UUID id, String displayName) {}

  record Document(String kind, String id, long version, JsonNode content) {}

  record Reference(String kind, String id, long version, boolean deleted) {}

  record Snapshot(long generation, List<Reference> documents) {}

  Snapshot snapshot(UUID owner);

  long clearSessions(UUID owner);

  Document saveSession(
      UUID owner, String id, long expectedVersion, long generation, JsonNode content);

  List<Reference> index(UUID owner);

  Document read(UUID owner, String kind, String id);

  Account signIn(String subject, String name);

  List<Document> list(UUID owner);

  Document save(UUID owner, String kind, String id, long expectedVersion, JsonNode content);
}
