package com.poe2craft.crafting.domain.pathsearch;

import java.math.BigInteger;
import java.util.*;

/**
 * Ranking uncertainty distinguishes absent methods, unsupported model mass and arithmetic width.
 */
public final class CertifiedRecommendations {
  private CertifiedRecommendations() {}

  public record Choice(String id, CertifiedInterval goal, CertifiedInterval unresolved) {
    public Choice {
      if (goal.fractionalBits() != unresolved.fractionalBits())
        throw new IllegalArgumentException("Precision mismatch");
    }

    BigInteger potentialUpper() {
      return goal.upperUnits()
          .add(unresolved.upperUnits())
          .min(BigInteger.ONE.shiftLeft(goal.fractionalBits()));
    }
  }

  public record Result(String status, List<String> methodIds) {}

  public static Result local(List<Choice> choices, boolean allMethodsEnumerated) {
    if (!allMethodsEnumerated) return new Result("PENDING_ENUMERATION", List.of());
    if (choices.isEmpty()) return new Result("NO_CHOICES", List.of());
    int bits = choices.getFirst().goal().fractionalBits();
    if (choices.stream().anyMatch(c -> c.goal().fractionalBits() != bits))
      throw new IllegalArgumentException("Precision mismatch");
    for (var candidate : choices)
      if (candidate.unresolved().zero()
          && choices.stream()
              .allMatch(
                  other ->
                      other == candidate
                          || candidate.goal().lowerUnits().compareTo(other.potentialUpper()) > 0))
        return new Result("CERTIFIED", List.of(candidate.id()));
    var highest =
        choices.stream().map(c -> c.goal().lowerUnits()).max(BigInteger::compareTo).orElseThrow();
    var tied =
        choices.stream()
            .filter(
                c ->
                    c.unresolved().zero()
                        && c.goal().exact()
                        && c.goal().lowerUnits().equals(highest))
            .toList();
    if (!tied.isEmpty()
        && choices.stream()
            .allMatch(c -> tied.contains(c) || c.potentialUpper().compareTo(highest) < 0))
      return new Result("CERTIFIED", tied.stream().map(Choice::id).toList());
    return new Result(
        choices.stream().anyMatch(c -> !c.unresolved().zero())
            ? "PENDING_MODEL"
            : "PENDING_ARITHMETIC",
        List.of());
  }
}
