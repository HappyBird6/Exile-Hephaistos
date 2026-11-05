package com.poe2craft.craftingsupport.search.application;

import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.search.application.PathSearchProtocol.Probability;
import com.poe2craft.craftingsupport.search.domain.DeferredItem;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemState;
import java.util.*;

/** V2 observations count complete method cycles, not primitive currency uses. */
public final class MethodSearchProtocol {
  private MethodSearchProtocol() {}

  /** Typed execution input with optional immutable provenance for historical JSON export. */
  public static final class Create {
    private final Integer version;
    private final String clientRequestId;
    private final BasicCurrencyState start;
    private final GoalFilter goal;
    private final List<String> activeOmens;
    private final com.fasterxml.jackson.databind.JsonNode originalDocument;

    @com.fasterxml.jackson.annotation.JsonCreator
    public Create(
        @com.fasterxml.jackson.annotation.JsonProperty("version") Integer version,
        @com.fasterxml.jackson.annotation.JsonProperty("clientRequestId") String clientRequestId,
        @com.fasterxml.jackson.annotation.JsonProperty("start") BasicCurrencyState start,
        @com.fasterxml.jackson.annotation.JsonProperty("goal") GoalFilter goal,
        @com.fasterxml.jackson.annotation.JsonProperty("activeOmens") List<String> activeOmens) {
      this(version, clientRequestId, start, goal, activeOmens, null);
    }

    private Create(
        Integer version,
        String clientRequestId,
        BasicCurrencyState start,
        GoalFilter goal,
        List<String> activeOmens,
        com.fasterxml.jackson.databind.JsonNode originalDocument) {
      this.version = version;
      this.clientRequestId = clientRequestId;
      this.start = start;
      this.goal = goal;
      this.activeOmens = activeOmens;
      this.originalDocument = originalDocument == null ? null : originalDocument.deepCopy();
    }

    public Integer version() {
      return version;
    }

    public String clientRequestId() {
      return clientRequestId;
    }

    public BasicCurrencyState start() {
      return start;
    }

    public GoalFilter goal() {
      return goal;
    }

    public List<String> activeOmens() {
      return activeOmens;
    }

    public Create withOriginalDocument(com.fasterxml.jackson.databind.JsonNode document) {
      return new Create(version, clientRequestId, start, goal, activeOmens, document);
    }

    @com.fasterxml.jackson.annotation.JsonValue
    public Object serializedDocument() {
      if (originalDocument != null) return originalDocument.deepCopy();
      var fields = new LinkedHashMap<String, Object>();
      fields.put("version", version);
      fields.put("clientRequestId", clientRequestId);
      fields.put("start", start);
      fields.put("goal", goal);
      fields.put("activeOmens", activeOmens);
      return fields;
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof Create value
          && Objects.equals(version, value.version)
          && Objects.equals(clientRequestId, value.clientRequestId)
          && Objects.equals(start, value.start)
          && Objects.equals(goal, value.goal)
          && Objects.equals(activeOmens, value.activeOmens);
    }

    @Override
    public int hashCode() {
      return Objects.hash(version, clientRequestId, start, goal, activeOmens);
    }
  }

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
      Map<String, DeferredItem.Band> representativeRolls,
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
      Map<String, DeferredItem.Band> sourceRolls,
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
      List<WorkbenchSimulator.Assumption> assumptions,
      List<Local> localRecommendations,
      List<Strategy> strategies,
      Graph graph) {}
}
