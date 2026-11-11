package com.poe2craft.workbench.repeat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/** Deduplication and cancellation boundary. A request ID binds to exact bytes and one ruleset. */
public interface RepeatRequestStore {
  JsonNode execute(String id, String digest, Function<BooleanSupplier, JsonNode> operation);

  void cancel(String id);
}
