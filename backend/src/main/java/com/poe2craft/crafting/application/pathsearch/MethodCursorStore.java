package com.poe2craft.crafting.application.pathsearch;

import java.util.*;
import java.util.function.Consumer;

/** Staged execution rows become visible only through an atomic graph checkpoint publication. */
public interface MethodCursorStore {
  String CHECKPOINT_PREFIX = "execution/";

  record ExecutionCursor(String id, String scope, String header, long rows, boolean sealed) {}

  record ExecutionRow(long ordinal, String content) {}

  record ExecutionPage(List<ExecutionRow> rows, boolean complete) {
    public ExecutionPage {
      rows = List.copyOf(rows);
    }
  }

  ExecutionCursor beginExecution(String scope, String header);

  long appendExecution(String id, long expectedRows, List<String> rows);

  ExecutionCursor sealExecution(String id, long expectedRows);

  /** Synchronous bounded-memory capture. The returned sealed cursor is not yet published. */
  default ExecutionCursor captureExecution(
      String scope, String header, Consumer<Consumer<String>> producer) {
    var draft = beginExecution(scope, header);
    long[] offset = {0};
    var batch = new ArrayList<String>();
    try {
      producer.accept(
          row -> {
            batch.add(row);
            if (batch.size() == 128) {
              offset[0] = appendExecution(draft.id(), offset[0], batch);
              batch.clear();
            }
          });
      if (!batch.isEmpty()) offset[0] = appendExecution(draft.id(), offset[0], batch);
      return sealExecution(draft.id(), offset[0]);
    } catch (RuntimeException | Error failure) {
      try {
        discardExecution(draft.id());
      } catch (RuntimeException cleanup) {
        failure.addSuppressed(cleanup);
      }
      throw failure;
    }
  }

  Optional<ExecutionCursor> execution(String scope);

  ExecutionPage executionRows(String id, long offset, int limit);

  /** Rejects deletion while any committed execution checkpoint still references this cursor. */
  void discardExecution(String id);

  static MethodGraphStore.Checkpoint reference(ExecutionCursor cursor) {
    if (!cursor.sealed()) throw new IllegalArgumentException("Execution must be sealed");
    return new MethodGraphStore.Checkpoint(CHECKPOINT_PREFIX + cursor.scope(), cursor.id());
  }

  /** Publish with the terminal observation; reclaim the now-unreferenced execution afterwards. */
  static MethodGraphStore.Checkpoint finished(String scope) {
    if (scope == null || scope.isBlank())
      throw new IllegalArgumentException("Execution scope required");
    return new MethodGraphStore.Checkpoint(CHECKPOINT_PREFIX + scope, "");
  }
}
