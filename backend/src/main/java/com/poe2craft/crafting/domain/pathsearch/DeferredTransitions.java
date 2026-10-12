package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.*;
import java.math.BigInteger;
import java.util.*;

/**
 * Basic actions carry retained roll variables and append a fresh declared independent roll cell.
 */
public final class DeferredTransitions {
  public record Identity(
      BasicCurrencyTransitions.KernelIdentity kernel,
      List<Map<String, DeferredItem.Band>> retainedLaws) {}

  private final BasicCurrencyTransitions primitive;
  private final RollPartition partition;

  public DeferredTransitions(BasicCurrencyTransitions primitive, RollPartition partition) {
    this.primitive = primitive;
    this.partition = partition;
  }

  public Cursor primitiveCursor(DeferredItem source, WorkbenchCurrency action, Set<String> omens) {
    return new Cursor(source, primitive.primitiveCursor(source.representative(), action, omens));
  }

  public final class Cursor {
    private final DeferredItem source;
    private final BasicCurrencyTransitions.PrimitiveCursor cursor;

    private Cursor(DeferredItem source, BasicCurrencyTransitions.PrimitiveCursor cursor) {
      this.source = source;
      this.cursor = cursor;
    }

    public boolean blocked() {
      return cursor.blocked();
    }

    public boolean complete() {
      return cursor.complete();
    }

    public BasicCurrencyTransitions.Status status() {
      return cursor.status();
    }

    public String reason() {
      return cursor.reason();
    }

    public BigInteger remainingOutcomes() {
      return cursor.remainingOutcomes();
    }

    public List<WorkbenchSimulator.Assumption> assumptions() {
      return cursor.assumptions();
    }

    public boolean mayApplyAfter(WorkbenchCurrency action) {
      return cursor.mayApplyAfter(action);
    }

    public Optional<Identity> identity() {
      return cursor
          .identity()
          .map(
              k ->
                  new Identity(
                      k,
                      k.branches().stream()
                          .map(
                              branch -> {
                                var retained = new HashMap<>(source.rolls());
                                retained
                                    .keySet()
                                    .retainAll(
                                        branch.remaining().explicits().stream()
                                            .map(m -> m.modifierId())
                                            .toList());
                                return Map.copyOf(retained);
                              })
                          .toList()));
    }

    public ExactMethodRunner.Outcome<DeferredItem> next() {
      var group = cursor.nextGrouped(partition::end);
      var retained = new HashMap<>(source.rolls());
      var ids =
          group.outcome().state().item().explicits().stream().map(m -> m.modifierId()).toList();
      retained.keySet().retainAll(ids);
      if (group.addedModifier() != null) {
        retained.remove(
            group.addedModifier()); // A removed-and-readded modifier gets a fresh roll law.
        if (group.min() != group.max())
          retained.put(
              group.addedModifier(), new DeferredItem.Band(group.stat(), group.min(), group.max()));
      }
      return new ExactMethodRunner.Outcome<>(
          new DeferredItem(group.outcome().state(), retained), group.outcome().probability());
    }
  }
}
