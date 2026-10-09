package com.poe2craft.crafting.domain.goalfilter;

import java.util.*;

/** Reviewed display units and direct pseudo sources; never grants probability support. */
public record GoalDefinitions(
    int schemaVersion, String ruleVersion, List<Direct> stats, List<Pseudo> pseudos) {
  public record Direct(String sourceStatId, String label, String unit) {}

  public record Pseudo(String id, String label, String unit, List<String> sourceStatIds) {
    public Pseudo {
      sourceStatIds = List.copyOf(sourceStatIds);
    }
  }

  public GoalDefinitions {
    if (schemaVersion != 1 || ruleVersion == null || ruleVersion.isBlank())
      throw new IllegalArgumentException("Unsupported goal definitions version");
    stats = List.copyOf(stats);
    pseudos = List.copyOf(pseudos);
    var ids = new HashMap<String, Direct>();
    for (var stat : stats) {
      require(stat.sourceStatId());
      require(stat.label());
      unit(stat.unit());
      if (ids.putIfAbsent(stat.sourceStatId(), stat) != null)
        throw new IllegalArgumentException("Duplicate goal source stat: " + stat.sourceStatId());
    }
    var pseudoIds = new HashSet<String>();
    for (var pseudo : pseudos) {
      require(pseudo.id());
      require(pseudo.label());
      unit(pseudo.unit());
      if (!pseudo.id().matches("[a-z0-9_]+")
          || !pseudoIds.add(pseudo.id())
          || pseudo.sourceStatIds().isEmpty()
          || new HashSet<>(pseudo.sourceStatIds()).size() != pseudo.sourceStatIds().size())
        throw new IllegalArgumentException("Invalid pseudo definition: " + pseudo.id());
      for (var source : pseudo.sourceStatIds()) {
        var direct = ids.get(source);
        if (direct == null || !direct.unit().equals(pseudo.unit()))
          throw new IllegalArgumentException("Unknown or incompatible pseudo source: " + source);
      }
    }
  }

  private static void require(String value) {
    if (value == null || value.isBlank())
      throw new IllegalArgumentException("Missing goal definition field");
  }

  private static void unit(String value) {
    if (!Set.of("flat", "percent").contains(value))
      throw new IllegalArgumentException("Unreviewed goal unit");
  }

  public Map<String, Direct> directStats() {
    var result = new TreeMap<String, Direct>();
    stats.forEach(s -> result.put(s.sourceStatId(), s));
    return Collections.unmodifiableMap(result);
  }

  /** Order-independent data identity. Length prefixes prevent delimiter collisions. */
  public String signature() {
    var result = new StringBuilder();
    stats.stream()
        .sorted(Comparator.comparing(Direct::sourceStatId))
        .forEach(
            s -> {
              append(result, s.sourceStatId());
              append(result, s.label());
              append(result, s.unit());
            });
    pseudos.stream()
        .sorted(Comparator.comparing(Pseudo::id))
        .forEach(
            p -> {
              append(result, p.id());
              append(result, p.label());
              append(result, p.unit());
              result.append(p.sourceStatIds().size()).append(':');
              p.sourceStatIds().stream().sorted().forEach(s -> append(result, s));
            });
    return result.toString();
  }

  private static void append(StringBuilder result, String value) {
    result.append(value.length()).append(':').append(value);
  }
}
