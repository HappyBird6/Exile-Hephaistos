package com.poe2craft.crafting.application.pathsearch;

import com.poe2craft.crafting.application.pathsearch.PathSearchProtocol.Probability;
import java.util.List;

/** Additive indexed API: class marginals are distinct from conditional concrete entry laws. */
public final class IndexedMethodProtocol {
  private IndexedMethodProtocol() {}

  public record Started(MethodSearchProtocol.Snapshot search, Entry rootEntry) {}

  public record Outcome(String kind, String stateId, Probability probability) {}

  public record Outcomes(
      String jobId,
      long revision,
      String methodId,
      int cycles,
      List<Outcome> outcomes,
      String nextCursor) {}

  public record Bind(
      Integer version,
      String requestId,
      Long revision,
      String parentEntryId,
      String methodId,
      Integer cycles,
      String kind,
      String targetStateId) {}

  public record Advance(
      Integer version, Long expectedGeneration, Integer maxSteps, Integer retainedStateLimit) {}

  public record Entry(
      String id,
      String stateId,
      String parentEntryId,
      String status,
      String reason,
      Probability conditionalProbability,
      long generation,
      List<MethodSearchProtocol.ConditionalState> states,
      Integer nextOffset) {}
}
