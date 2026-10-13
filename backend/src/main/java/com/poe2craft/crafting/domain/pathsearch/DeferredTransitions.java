package com.poe2craft.crafting.domain.pathsearch;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.item.*;
import java.math.BigInteger;
import java.util.*;

/**
 * Basic actions carry retained roll variables and append a fresh declared independent roll cell.
 */
public final class DeferredTransitions {
  public record Identity(
      BasicCurrencyTransitions.KernelIdentity kernel,
      List<Map<String, DeferredItem.Band>> retainedLaws,
      List<Map<String, TierCell>> retainedTiers,
      List<Map<String, OrbitCell>> retainedOrbits) {}

  private final BasicCurrencyTransitions primitive;
  private final RollPartition partition;

  public DeferredTransitions(BasicCurrencyTransitions primitive, RollPartition partition) {
    this.primitive = primitive;
    this.partition = partition;
  }

  public Cursor primitiveCursor(DeferredItem source, WorkbenchCurrency action, Set<String> omens) {
    partition.orbits().validateSource(source);
    var cursor = primitive.primitiveCursor(source.representative(), action, omens);
    if (!cursor.blocked() && !partition.allows(action))
      throw new IllegalArgumentException(
          "Action is outside the partition's reachable rarity scope");
    return new Cursor(source, cursor);
  }

  public final class Cursor {
    private final DeferredItem source;
    private final BasicCurrencyTransitions.PrimitiveCursor cursor;
    private final Set<TierEmission> emitted = new HashSet<>();
    private ExactMethodRunner.Outcome<DeferredItem> next;

    private record TierEmission(int branch, TierCell cell) {}

    private Cursor(DeferredItem source, BasicCurrencyTransitions.PrimitiveCursor cursor) {
      this.source = source;
      this.cursor = cursor;
      if (!cursor.blocked()) advance();
    }

    public boolean blocked() {
      return cursor.blocked();
    }

    public boolean complete() {
      return !cursor.blocked() && next == null;
    }

    public BasicCurrencyTransitions.Status status() {
      return cursor.blocked()
          ? cursor.status()
          : complete()
              ? BasicCurrencyTransitions.Status.COMPLETE
              : BasicCurrencyTransitions.Status.PARTIAL;
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
                          .toList(),
                      k.branches().stream()
                          .map(
                              branch -> {
                                var retained = new HashMap<>(source.tiers());
                                retained
                                    .keySet()
                                    .retainAll(
                                        branch.remaining().explicits().stream()
                                            .map(ModifierInstance::modifierId)
                                            .toList());
                                return Map.copyOf(retained);
                              })
                          .toList(),
                      k.branches().stream()
                          .map(
                              branch -> {
                                var ids =
                                    branch.remaining().explicits().stream()
                                        .map(ModifierInstance::modifierId)
                                        .collect(java.util.stream.Collectors.toSet());
                                var retained = new HashMap<>(source.orbits());
                                retained
                                    .entrySet()
                                    .removeIf(
                                        entry ->
                                            entry.getValue().representatives().stream()
                                                .noneMatch(m -> ids.contains(m.modifierId())));
                                return Map.copyOf(retained);
                              })
                          .toList()));
    }

    public ExactMethodRunner.Outcome<DeferredItem> next() {
      if (next == null) throw new IllegalStateException("Cursor is not active");
      var result = next;
      advance();
      return result;
    }

    private void advance() {
      next = null;
      while (!cursor.complete()) {
        var group = cursor.nextGrouped(partition::end);
        var retained = new HashMap<>(source.rolls());
        var tiers = new HashMap<>(source.tiers());
        var ids =
            group.outcome().state().item().explicits().stream().map(m -> m.modifierId()).toList();
        retained.keySet().retainAll(ids);
        tiers.keySet().retainAll(ids);
        var state = group.outcome().state();
        var probability = group.outcome().probability();
        if (group.addedModifier() != null) {
          retained.remove(
              group.addedModifier()); // A removed-and-readded modifier gets a fresh roll law.
          tiers.remove(group.addedModifier());
          var member = partition.tiers().member(group.addedModifier(), group.min(), group.max());
          if (member.isPresent()) {
            var law = member.get();
            if (!emitted.add(new TierEmission(group.branch(), law.cell()))) continue;
            var candidates =
                cursor.identity().orElseThrow().branches().get(group.branch()).candidates().stream()
                    .map(ModifierDefinition::id)
                    .collect(java.util.stream.Collectors.toSet());
            if (law.cell().variants().stream().anyMatch(v -> !candidates.contains(v.modifierId())))
              throw new IllegalStateException("Tier partition crosses an eligibility boundary");
            var first = law.cell().representative(false);
            var explicits = new ArrayList<>(state.item().explicits());
            explicits.removeIf(m -> m.modifierId().equals(group.addedModifier()));
            explicits.add(first);
            var item = state.item();
            state =
                new BasicCurrencyState(
                    new ItemState(
                        item.snapshotId(),
                        item.baseItemId(),
                        item.itemLevel(),
                        item.rarity(),
                        item.implicits(),
                        explicits,
                        item.conditions(),
                        item.augmentSockets(),
                        item.catalystQuality()),
                    state.provenance());
            tiers.put(first.modifierId(), law.cell());
            probability =
                probability.multiply(
                    new Fraction(
                        law.cellWeight().numerator().multiply(law.intervalWeight().denominator()),
                        law.cellWeight().denominator().multiply(law.intervalWeight().numerator())));
          } else if (group.min() != group.max())
            retained.put(
                group.addedModifier(),
                new DeferredItem.Band(group.stat(), group.min(), group.max()));
        }
        next =
            new ExactMethodRunner.Outcome<>(
                partition.orbits().normalize(state, retained, tiers), probability);
        return;
      }
    }
  }
}
