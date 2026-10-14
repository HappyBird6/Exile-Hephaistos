package com.poe2craft.crafting.application.pathsearch;

import java.util.*;

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
