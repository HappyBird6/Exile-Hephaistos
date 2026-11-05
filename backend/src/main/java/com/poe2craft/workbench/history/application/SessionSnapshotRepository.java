package com.poe2craft.workbench.history.application;

import java.io.IOException;

/**
 * Opaque IDs and immutable bytes; storage implementations cannot turn snapshots into executable
 * items.
 */
public interface SessionSnapshotRepository {
  String create(byte[] document) throws IOException;

  byte[] read(String id) throws IOException;
}
