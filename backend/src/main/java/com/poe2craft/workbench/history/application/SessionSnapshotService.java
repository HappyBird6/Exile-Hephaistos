package com.poe2craft.workbench.history.application;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

/** Validates display-only session documents and delegates immutable storage through a port. */
public final class SessionSnapshotService {
  private final SessionSnapshotRepository repository;
  private final ObjectMapper json;

  public SessionSnapshotService(ObjectMapper json, SessionSnapshotRepository repository) {
    this.json = json;
    this.repository = repository;
  }

  public String create(byte[] bytes) throws IOException {
    SessionSnapshotValidator.validate(
        json.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readTree(bytes));
    return repository.create(bytes);
  }

  public byte[] read(String id) throws IOException {
    return repository.read(id);
  }
}
