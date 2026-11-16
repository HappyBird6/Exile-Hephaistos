package com.poe2craft.craftingsupport.rag.application;

import com.poe2craft.craftingsupport.search.domain.StrategyEvidence;
import java.util.List;

/**
 * Provider-independent, non-executable proposal. Action IDs refer only to supplied engine facts.
 */
public interface StrategyModel {
  record Proposal(
      List<String> initial,
      List<String> cycle,
      List<String> documents,
      String preparationSuccess,
      String explanation) {}

  record Result(String status, List<Proposal> choices, long inputTokens, long outputTokens) {}

  boolean configured();

  Result propose(
      StrategyKnowledge.Selection selection,
      StrategyEvidence.Facts facts,
      com.poe2craft.craftingsupport.goal.domain.GoalFilter goal,
      String locale);
}
