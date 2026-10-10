package com.poe2craft.crafting.application.pathsearch;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.poe2craft.crafting.domain.BasicCurrencyState;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter;
import com.poe2craft.item.ItemState;
import java.util.List;

/** Fixed v1 wire records. Internal continuation is deliberately absent. */
public final class PathSearchProtocol {
  private PathSearchProtocol() {}

  public record Create(
      Integer version,
      String clientRequestId,
      BasicCurrencyState start,
      GoalFilter goal,
      List<String> activeOmens,
      List<String> observations) {}

  public record Mutation(
      Integer version, String operation, String commandId, Long expectedRevision) {}

  public record Recover(
      Integer version,
      String clientRequestId,
      Long parentRevision,
      String failureExecutionId,
      String checkpointStateId,
      List<String> observations) {}

  public record Probability(String numerator, String denominator) {
    public static Probability of(Fraction value) {
      return new Probability(value.numerator().toString(), value.denominator().toString());
    }

    public Fraction fraction() {
      return new Fraction(
          new java.math.BigInteger(numerator), new java.math.BigInteger(denominator));
    }
  }

  public record Capabilities(
      String evaluation,
      String search,
      String reasonCode,
      List<String> actions,
      List<String> goalGroupTypes,
      boolean conditionalRecovery,
      boolean resume) {}

  public record Provenance(
      BasicCurrencyState.Provenance transition,
      String goalCatalogVersion,
      String predicateVersion,
      String searchVersion,
      String candidateSetVersion,
      String interpretation) {}

  public record Point(
      String attempts,
      Probability lower,
      Probability upper,
      Probability active,
      Probability dead,
      Probability unresolved,
      String status) {}

  public record Policy(String id, List<String> actions, String mode) {}

  public record Eventual(String status, Probability probability, String proofVersion) {}

  public record ExitPoint(String attempts, Probability probability) {}

  public record MethodExit(String stateId, String kind, List<ExitPoint> points) {}

  public record OmittedPoint(String attempts, Probability hit, Probability active) {}

  public record MethodTransition(
      String fromStateId,
      String stopCondition,
      int cycleUses,
      String proofVersion,
      List<MethodExit> exits,
      List<OmittedPoint> omitted) {}

  public record Recommendation(
      Policy policy,
      List<Point> points,
      Eventual eventual,
      @JsonInclude(JsonInclude.Include.NON_NULL) MethodTransition method) {
    public Recommendation(Policy policy, List<Point> points, Eventual eventual) {
      this(policy, points, eventual, null);
    }
  }

  public record Rank(String policyId, int rank) {}

  public record Ranking(String attempts, String status, List<Rank> entries) {}

  public record Node(String id, ItemState item, String goalStatus) {}

  public record Execution(String id, String stateId, String policyId, int phase) {}

  public record Edge(
      String id, String from, String to, String action, Probability probability, String kind) {}

  public record Expansion(String executionId, String status, Probability unresolved) {}

  public record GraphPage(
      int version,
      String jobId,
      long revision,
      List<Node> nodes,
      List<Execution> executions,
      List<Edge> edges,
      List<Expansion> expansions,
      String nextCursor) {}

  public record Recovery(
      String parentJobId,
      long parentRevision,
      String failureExecutionId,
      String checkpointStateId,
      boolean conditional,
      boolean includedInMain) {}

  public record Scope(String kind, int generated, Integer total, boolean enumerationComplete) {}

  public record Snapshot(
      int version,
      String jobId,
      String clientRequestId,
      String requestFingerprint,
      long revision,
      String status,
      String expiresAt,
      Capabilities capabilities,
      Provenance provenance,
      Scope candidateScope,
      List<Recommendation> recommendations,
      List<Ranking> rankings,
      GraphPage graph,
      Recovery recovery,
      boolean resumable,
      String reasonCode) {}

  public static final class Rejected extends IllegalArgumentException {
    private final int status;
    private final String code;

    public Rejected(int status, String code) {
      super(code);
      this.status = status;
      this.code = code;
    }

    public int status() {
      return status;
    }

    public String code() {
      return code;
    }
  }
}
