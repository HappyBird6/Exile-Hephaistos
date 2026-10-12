package com.poe2craft.crafting.application.pathsearch;

import com.poe2craft.crafting.application.pathsearch.PathSearchProtocol.Probability;
import com.poe2craft.crafting.domain.BasicCurrencyState;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter;
import com.poe2craft.item.ItemState;
import java.util.*;

/** V2 observations count complete method cycles, not primitive currency uses. */
public final class MethodSearchProtocol {
  private MethodSearchProtocol() {}

  public record Create(
      Integer version,
      String clientRequestId,
      BasicCurrencyState start,
      GoalFilter goal,
      List<String> activeOmens) {}

  public record Mutation(
      Integer version,
      String commandId,
      Long expectedRevision,
      String operation,
      Integer retainedOutcomeLimit) {}

  public record Bucket(
      String id,
      int depth,
      String goal,
      List<String> entries,
      int states,
      ItemState representative,
      Map<String, com.poe2craft.crafting.domain.pathsearch.DeferredItem.Band> representativeRolls,
      Map<String, List<TierVariant>> representativeTiers,
      Map<String, OrbitView> representativeOrbits,
      boolean directComplete) {}

  public record TierVariant(String modifierId, String stat, long min, long max) {}

  public record OrbitView(
      List<String> representativeIds, List<List<List<TierVariant>>> assignments) {}

  public record Exit(String bucketId, String kind, Probability probability, int states) {}

  public record Point(
      int cycles,
      List<Exit> exits,
      Probability unresolved,
      Map<String, Probability> initialUses,
      Map<String, Probability> cycleUses) {}

  public record Arrow(
      String id,
      String sourceStateId,
      String sourceBucketId,
      ItemState sourceItem,
      Map<String, com.poe2craft.crafting.domain.pathsearch.DeferredItem.Band> sourceRolls,
      Map<String, List<TierVariant>> sourceTiers,
      Map<String, OrbitView> sourceOrbits,
      List<String> initialActions,
      List<String> cycleActions,
      String status,
      String reason,
      List<Point> points) {}

  public record Graph(
      int version,
      String jobId,
      long revision,
      List<Bucket> buckets,
      List<Arrow> methods,
      int methodCount,
      String nextCursor) {}

  public record ConditionalState(
      String stateId, ItemState item, String kind, Probability probability) {}

  public record Distribution(
      int version,
      String jobId,
      long revision,
      String methodId,
      int cycles,
      List<ConditionalState> states,
      Probability unresolved,
      String nextCursor) {}

  public record Local(String stateId, int cycles, List<String> methodIds) {}

  public record Strategy(
      String stateId,
      int cycles,
      String status,
      Probability lower,
      Probability upper,
      List<String> methodIds) {}

  public record Snapshot(
      int version,
      String jobId,
      String clientRequestId,
      long revision,
      long generation,
      String status,
      String reason,
      boolean resumable,
      boolean enumerationComplete,
      long operations,
      String rootStateId,
      BasicCurrencyState.Provenance transition,
      String goalCatalogVersion,
      String templateVersion,
      String calculationVersion,
      String rollRepresentationVersion,
      String rollPartitionVersion,
      String strategyVersion,
      String interpretation,
      List<com.poe2craft.crafting.domain.WorkbenchSimulator.Assumption> assumptions,
      List<Local> localRecommendations,
      List<Strategy> strategies,
      Graph graph) {}
}
