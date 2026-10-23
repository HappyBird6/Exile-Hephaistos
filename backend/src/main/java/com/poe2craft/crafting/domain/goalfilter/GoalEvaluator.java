package com.poe2craft.crafting.domain.goalfilter;

import com.poe2craft.crafting.domain.goalfilter.GoalFilter.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Pure evaluation over proven observations. Missing keys mean unknown, never absent. */
public final class GoalEvaluator {
  public record Observation(
      Presence presence, BigDecimal value, Capability support, GoalNumber exact) {
    public Observation(Presence presence, BigDecimal value, Capability support) {
      this(presence, value, support, value == null ? null : GoalNumber.of(value));
    }

    public Observation {
      if ((presence == Presence.PRESENT) != (value != null))
        throw new IllegalArgumentException("Only present observations have a numeric value");
      if ((presence == Presence.PRESENT) != (exact != null))
        throw new IllegalArgumentException("Only present observations have an exact number");
    }

    public static Observation present(BigDecimal value) {
      return new Observation(Presence.PRESENT, value, Capability.SUPPORTED);
    }

    public static Observation presentNumber(GoalNumber number) {
      return new Observation(Presence.PRESENT, number.display(), Capability.SUPPORTED, number);
    }

    public static Observation absent() {
      return new Observation(Presence.ABSENT, null, Capability.SUPPORTED);
    }

    public static Observation unknown() {
      return new Observation(Presence.UNKNOWN, null, Capability.UNKNOWN);
    }

    public static Observation unsupported() {
      return new Observation(Presence.UNKNOWN, null, Capability.UNSUPPORTED);
    }
  }

  public record EntryResult(
      String id,
      Status status,
      Presence presence,
      BigDecimal value,
      @com.fasterxml.jackson.annotation.JsonInclude(
              com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
          GoalNumber.Ratio exactValue) {
    public EntryResult(String id, Status status, Presence presence, BigDecimal value) {
      this(id, status, presence, value, null);
    }
  }

  public record GroupResult(
      String id,
      Status status,
      Integer count,
      BigDecimal score,
      List<EntryResult> entries,
      @com.fasterxml.jackson.annotation.JsonInclude(
              com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
          GoalNumber.Ratio exactScore) {
    public GroupResult(
        String id, Status status, Integer count, BigDecimal score, List<EntryResult> entries) {
      this(id, status, count, score, entries, null);
    }
  }

  public record Evaluation(
      int version,
      String catalogVersion,
      Status status,
      Status generalStatus,
      List<GroupResult> groups,
      List<Issue> issues) {}

  public Evaluation evaluate(
      GoalFilter goal, Status general, Map<String, Observation> observations, List<Issue> issues) {
    var results = new ArrayList<GroupResult>();
    for (var group : goal.groups()) {
      if (!group.disabled()) results.add(group(group, observations));
    }
    var statuses = new ArrayList<Status>();
    statuses.add(general);
    results.forEach(g -> statuses.add(g.status()));
    return new Evaluation(
        1,
        goal.catalogVersion(),
        and(statuses),
        general,
        List.copyOf(results),
        List.copyOf(issues));
  }

  public static Observation pseudo(
      List<GoalCatalog.Contribution> contributions, Map<String, Observation> sources) {
    var sum = GoalNumber.ZERO;
    boolean present = false, unknown = false, unsupported = false;
    for (var contribution : contributions) {
      var source = sources.getOrDefault(contribution.statId(), Observation.unknown());
      unsupported |= source.support() == Capability.UNSUPPORTED;
      unknown |= source.presence() == Presence.UNKNOWN;
      if (source.presence() == Presence.PRESENT) {
        present = true;
        sum = sum.add(source.exact().multiply(contribution.coefficient()));
      }
    }
    if (unsupported) return Observation.unsupported();
    if (unknown) return Observation.unknown();
    return present ? Observation.presentNumber(sum) : Observation.absent();
  }

  private GroupResult group(Group group, Map<String, Observation> observations) {
    var entries = new ArrayList<EntryResult>();
    var hard = new ArrayList<Status>();
    int matched = 0, uncertain = 0, present = 0;
    var score = GoalNumber.ZERO;
    for (var entry : group.entries()) {
      if (entry.disabled()) continue;
      var value = observations.getOrDefault(entry.statId(), Observation.unknown());
      var status = row(entry, value);
      entries.add(
          new EntryResult(
              entry.id(),
              status,
              value.presence(),
              value.value(),
              value.exact() == null ? null : value.exact().ratio()));
      if (status == Status.MATCH) matched++;
      if (status == Status.UNKNOWN || status == Status.UNSUPPORTED) uncertain++;
      if (value.presence() == Presence.PRESENT) present++;
      var ifStatus = value.presence() == Presence.ABSENT ? Status.MATCH : status;
      hard.add(ifStatus);
      if (value.presence() == Presence.PRESENT
          && entry.weight() != null
          && (group.type() == Type.WEIGHTED_V1 || status == Status.MATCH))
        score = score.add(value.exact().multiply(entry.weight()));
    }
    var statuses = entries.stream().map(EntryResult::status).toList();
    Status result;
    switch (group.type()) {
      case AND -> result = and(statuses);
      case IF -> result = and(hard);
      case NOT ->
          result = statuses.contains(Status.MATCH) ? Status.NO_MATCH : uncertainty(statuses);
      case COUNT -> {
        var low = BigDecimal.valueOf(matched);
        var high = BigDecimal.valueOf(matched + uncertain);
        if ((group.range().max() != null && low.compareTo(group.range().max()) > 0)
            || (group.range().min() != null && high.compareTo(group.range().min()) < 0))
          result = Status.NO_MATCH;
        else if (group.range().contains(low) && group.range().contains(high)) result = Status.MATCH;
        else result = uncertainty(statuses);
      }
      case WEIGHTED_V1 -> {
        result = and(hard);
        if (result == Status.MATCH)
          result = score.within(group.range()) ? Status.MATCH : Status.NO_MATCH;
      }
      case WEIGHTED_V2 -> {
        if (uncertain > 0) result = uncertainty(statuses);
        else result = present > 0 && score.within(group.range()) ? Status.MATCH : Status.NO_MATCH;
      }
      default -> throw new IllegalStateException("Unknown group type");
    }
    return new GroupResult(
        group.id(),
        result,
        group.type() == Type.COUNT && uncertain == 0 ? matched : null,
        (group.type() == Type.WEIGHTED_V1 || group.type() == Type.WEIGHTED_V2) && uncertain == 0
            ? score.display()
            : null,
        List.copyOf(entries),
        (group.type() == Type.WEIGHTED_V1 || group.type() == Type.WEIGHTED_V2) && uncertain == 0
            ? score.ratio()
            : null);
  }

  private static Status row(Entry entry, Observation observation) {
    if (observation.support() == Capability.UNSUPPORTED) return Status.UNSUPPORTED;
    if (observation.presence() == Presence.UNKNOWN) return Status.UNKNOWN;
    return observation.presence() == Presence.PRESENT && observation.exact().within(entry.range())
        ? Status.MATCH
        : Status.NO_MATCH;
  }

  public static Status and(List<Status> statuses) {
    return statuses.contains(Status.NO_MATCH) ? Status.NO_MATCH : uncertainty(statuses);
  }

  private static Status uncertainty(List<Status> statuses) {
    if (statuses.contains(Status.UNSUPPORTED)) return Status.UNSUPPORTED;
    return statuses.contains(Status.UNKNOWN) ? Status.UNKNOWN : Status.MATCH;
  }
}
