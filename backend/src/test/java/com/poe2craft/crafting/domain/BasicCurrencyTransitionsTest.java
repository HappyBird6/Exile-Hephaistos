package com.poe2craft.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.BasicCurrencyTransitions.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter;
import com.poe2craft.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/** Tiny independently enumerable test catalog; no production pool is pruned or rewritten. */
class BasicCurrencyTransitionsTest {
  static final String RULESET = "synthetic-ruleset-one";
  final ModifierDefinition implicit =
      new ModifierDefinition(
          "implicit",
          "Implicit",
          ModifierDefinition.Layer.IMPLICIT,
          ModifierDefinition.AffixType.NONE,
          Set.of("implicit"),
          1,
          0,
          0,
          "Implicit",
          List.of(new ModifierDefinition.StatRange("spirit", 5, 5)),
          Set.of(),
          "https://example.test/implicit");
  final ModifierDefinition a = definition("a", ModifierDefinition.AffixType.PREFIX, 1, 2, 1);
  final ModifierDefinition b = definition("b", ModifierDefinition.AffixType.SUFFIX, 3, 4, 3);
  final ItemCatalog catalog = catalog(List.of(implicit, a, b));
  final BasicCurrencyTransitions kernel = new BasicCurrencyTransitions(catalog, RULESET);

  ModifierDefinition definition(
      String id, ModifierDefinition.AffixType type, long min, long max, int weight) {
    return new ModifierDefinition(
        id,
        id,
        ModifierDefinition.Layer.EXPLICIT,
        type,
        Set.of(id),
        1,
        weight,
        1,
        id,
        List.of(new ModifierDefinition.StatRange(id, min, max)),
        Set.of(),
        "https://example.test/" + id);
  }

  ItemCatalog catalog(List<ModifierDefinition> definitions) {
    long p =
        definitions.stream()
            .filter(d -> d.affixType() == ModifierDefinition.AffixType.PREFIX)
            .count();
    long s =
        definitions.stream()
            .filter(d -> d.affixType() == ModifierDefinition.AffixType.SUFFIX)
            .count();
    long pw =
        definitions.stream()
            .filter(d -> d.affixType() == ModifierDefinition.AffixType.PREFIX)
            .mapToLong(ModifierDefinition::weight)
            .sum();
    long sw =
        definitions.stream()
            .filter(d -> d.affixType() == ModifierDefinition.AffixType.SUFFIX)
            .mapToLong(ModifierDefinition::weight)
            .sum();
    return new ItemCatalog(
        new ItemCatalog.Metadata(
            "synthetic-basic",
            "test-only",
            "https://example.test/catalog",
            "POE2DB_AS_PUBLISHED",
            "a".repeat(64),
            "b".repeat(64),
            (int) p,
            (int) s,
            pw,
            sw),
        new ItemCatalog.BaseItem(
            SolarAmulet.BASE_ID,
            "Synthetic Solar",
            "https://example.test/base",
            implicit.id(),
            1,
            1,
            3,
            3),
        definitions);
  }

  ItemState item(ItemState.Rarity rarity, List<ModifierInstance> mods) {
    return new ItemState(
        catalog.metadata().snapshotId(),
        catalog.base().id(),
        82,
        rarity,
        List.of(new ModifierInstance("implicit", Map.of("spirit", 5L))),
        mods,
        Set.of());
  }

  ModifierInstance mod(String id, long value) {
    return new ModifierInstance(id, Map.of(id, value));
  }

  BasicCurrencyState state(ItemState item) {
    return new BasicCurrencyState(item, kernel.provenance());
  }

  Result expand(ItemState item, WorkbenchCurrency action) {
    return kernel.expand(state(item), action, Set.of(), 1000);
  }

  Map<ItemState, Fraction> masses(Result result) {
    var resultMap = new LinkedHashMap<ItemState, Fraction>();
    for (var outcome : result.outcomes())
      resultMap.put(outcome.state().item(), outcome.probability());
    return resultMap;
  }

  void normalized(Result result) {
    assertThat(
            result.outcomes().stream()
                .map(Outcome::probability)
                .reduce(Fraction.ZERO, Fraction::add)
                .add(result.unresolved()))
        .isEqualTo(Fraction.ONE);
  }

  @Test
  void allSixteenActionsKeepExactMassAndTheExplicitFirstTabScope() {
    assertThat(BasicCurrencyTransitions.supportedActions())
        .hasSize(16)
        .doesNotContain(
            WorkbenchCurrency.ALCHEMY,
            WorkbenchCurrency.DIVINE,
            WorkbenchCurrency.FRACTURING,
            WorkbenchCurrency.ARTIFICER,
            WorkbenchCurrency.ESSENCE_BODY,
            WorkbenchCurrency.DILUTED_LIQUID_IRE);
    for (var action : BasicCurrencyTransitions.supportedActions()) {
      var rarity =
          switch (action.baseAction()) {
            case TRANSMUTATION -> ItemState.Rarity.NORMAL;
            case AUGMENTATION, REGAL -> ItemState.Rarity.MAGIC;
            default -> ItemState.Rarity.RARE;
          };
      var mods =
          action == WorkbenchCurrency.ANNULMENT || action.baseAction() == CraftingAction.CHAOS
              ? List.of(mod("a", 1))
              : List.<ModifierInstance>of();
      var result = expand(item(rarity, mods), action);
      assertThat(result.status()).as(action.name()).isEqualTo(Status.COMPLETE);
      normalized(result);
      assertThat(result.interpretation()).isEqualTo(BasicCurrencyTransitions.INTERPRETATION);
      assertThat(result.outcomes())
          .allSatisfy(
              o -> {
                assertThat(o.state().provenance()).isEqualTo(kernel.provenance());
                assertThat(o.state().item().implicits())
                    .isEqualTo(result.source().item().implicits());
                assertThat(new ItemStateValidator(catalog).validate(o.state().item())).isEmpty();
              });
    }
  }

  @Test
  void independentEightTicketAdditionEnumerationAgreesWithFiniteOracle() {
    var root = item(ItemState.Rarity.RARE, List.of());
    var result = expand(root, WorkbenchCurrency.EXALTED);
    // Two equally probable integer rolls for each weight ticket: 1 a ticket, 3 b tickets.
    var elementary = new ArrayList<ItemState>();
    elementary.add(item(ItemState.Rarity.RARE, List.of(mod("a", 1))));
    elementary.add(item(ItemState.Rarity.RARE, List.of(mod("a", 2))));
    for (int ticket = 0; ticket < 3; ticket++) {
      elementary.add(item(ItemState.Rarity.RARE, List.of(mod("b", 3))));
      elementary.add(item(ItemState.Rarity.RARE, List.of(mod("b", 4))));
    }
    var oracle = new LinkedHashMap<ItemState, Fraction>();
    for (var concrete : elementary) oracle.merge(concrete, Fraction.of(1, 8), Fraction::add);
    assertThat(masses(result)).isEqualTo(oracle);
    var finite =
        ExactNumericDistribution.firstHit(
            root,
            List.of(
                i ->
                    elementary.stream()
                        .map(s -> new ExactNumericDistribution.Outcome(s, Fraction.of(1, 8)))
                        .toList()),
            i ->
                i.explicits().contains(mod("a", 2))
                    ? GoalFilter.Status.MATCH
                    : GoalFilter.Status.NO_MATCH,
            100);
    assertThat(finite.success()).isEqualTo(Fraction.of(1, 8));
    assertThat(result.assumptions())
        .anySatisfy(
            a -> {
              assertThat(a.id()).isEqualTo("uniform-integer-roll-v1");
              assertThat(a.sourceUrl()).startsWith("https://example.test/");
            });
  }

  @Test
  void chaosRemovesThenAddsAndMergesTwoElementarySelfLoopBranches() {
    var root = item(ItemState.Rarity.RARE, List.of(mod("a", 1), mod("b", 3)));
    var result = expand(root, WorkbenchCurrency.CHAOS);
    // Remove a -> a1/a2; remove b -> b3/b4. Each elementary event has probability 1/4.
    var elementary =
        List.of(
            root,
            item(ItemState.Rarity.RARE, List.of(mod("a", 2), mod("b", 3))),
            root,
            item(ItemState.Rarity.RARE, List.of(mod("a", 1), mod("b", 4))));
    var oracle = new LinkedHashMap<ItemState, Fraction>();
    elementary.forEach(i -> oracle.merge(i, Fraction.of(1, 4), Fraction::add));
    assertThat(result.elementaryOutcomes()).isEqualTo(4);
    assertThat(result.outcomes()).hasSize(3);
    assertThat(masses(result)).isEqualTo(oracle);
    assertThat(masses(result).get(root)).isEqualTo(Fraction.of(1, 2));
    normalized(result);
    var selfLoop =
        result.outcomes().stream()
            .filter(o -> o.state().item().equals(root))
            .findFirst()
            .orElseThrow();
    assertThat(selfLoop.state().canonicalKey()).isEqualTo(state(root).canonicalKey());
    assertThat(result.assumptions())
        .anySatisfy(
            a -> {
              assertThat(a.id()).isEqualTo("uniform-removal-v1");
              assertThat(a.n()).isEqualTo(2);
              assertThat(a.sourceUrl()).isEqualTo("https://poe2db.tw/us/Chaos_Orb");
            });
  }

  @Test
  void fractureProtectionAndSourceNumericQualityPreservationAreSharedWithWorkbench() {
    var fractured = new ModifierInstance("a", Map.of("a", 2L), true);
    var root = item(ItemState.Rarity.RARE, List.of(fractured, mod("b", 3)));
    root =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            root.itemLevel(),
            root.rarity(),
            root.implicits(),
            root.explicits(),
            root.conditions(),
            root.augmentSockets(),
            new CatalystQuality(CatalystQuality.Type.FLESH, 30));
    for (var action :
        List.of(
            WorkbenchCurrency.ANNULMENT,
            WorkbenchCurrency.CHAOS,
            WorkbenchCurrency.GREATER_CHAOS,
            WorkbenchCurrency.PERFECT_CHAOS)) {
      var result = expand(root, action);
      assertThat(result.status()).isEqualTo(Status.COMPLETE);
      normalized(result);
      var input = root;
      assertThat(result.outcomes())
          .allSatisfy(
              o -> {
                assertThat(o.state().item().explicits()).contains(fractured);
                assertThat(o.state().item().catalystQuality()).isEqualTo(input.catalystQuality());
                assertThat(o.state().item().augmentSockets()).isNull();
              });
    }
    assertThat(
            expand(item(ItemState.Rarity.RARE, List.of(fractured)), WorkbenchCurrency.ANNULMENT)
                .status())
        .isEqualTo(Status.UNAVAILABLE);
  }

  @Test
  void actionOrderIsNotSortedOrCollapsedAndNumericBucketProjectionIsNotLossless() {
    var root = item(ItemState.Rarity.RARE, List.of());
    var added = expand(root, WorkbenchCurrency.EXALTED);
    var removedThen = expand(root, WorkbenchCurrency.ANNULMENT);
    assertThat(removedThen.status()).isEqualTo(Status.UNAVAILABLE);
    for (var outcome : added.outcomes()) {
      var removed = expand(outcome.state().item(), WorkbenchCurrency.ANNULMENT);
      assertThat(masses(removed)).containsExactlyEntriesOf(Map.of(root, Fraction.ONE));
    }
    var low = item(ItemState.Rarity.RARE, List.of(mod("a", 1)));
    var high = item(ItemState.Rarity.RARE, List.of(mod("a", 2)));
    assertThat(StateBucket.from(low)).isEqualTo(StateBucket.from(high));
    assertThat(state(low).canonicalKey()).isNotEqualTo(state(high).canonicalKey());
  }

  @Test
  void partialEnumerationKeepsTheExactComplementWithoutRenormalizingOrHidingDuplicates() {
    var root = item(ItemState.Rarity.RARE, List.of(mod("a", 1), mod("b", 3)));
    var partial = kernel.expand(state(root), WorkbenchCurrency.CHAOS, Set.of(), 3);
    assertThat(partial.status()).isEqualTo(Status.PARTIAL);
    assertThat(partial.elementaryOutcomes()).isEqualTo(3);
    assertThat(partial.outcomes()).hasSize(2);
    assertThat(masses(partial).get(root)).isEqualTo(Fraction.of(1, 2));
    assertThat(partial.unresolved()).isEqualTo(Fraction.of(1, 4));
    normalized(partial);
    assertThat(kernel.expand(state(root), WorkbenchCurrency.CHAOS, Set.of(), 4).status())
        .isEqualTo(Status.COMPLETE);
  }

  @Test
  void unsupportedJointCandidateBlocksTheWholeDistributionIncludingUnrelatedDenominator() {
    var joint =
        new ModifierDefinition(
            "joint",
            "joint",
            ModifierDefinition.Layer.EXPLICIT,
            ModifierDefinition.AffixType.SUFFIX,
            Set.of("joint"),
            1,
            3,
            1,
            "joint",
            List.of(
                new ModifierDefinition.StatRange("cold", 0, 1),
                new ModifierDefinition.StatRange("all", 0, 1)),
            Set.of(),
            "https://example.test/joint");
    var model = new BasicCurrencyTransitions(catalog(List.of(implicit, a, joint)), RULESET);
    var root = new BasicCurrencyState(item(ItemState.Rarity.RARE, List.of()), model.provenance());
    var result = model.expand(root, WorkbenchCurrency.EXALTED, Set.of(), 1);
    assertThat(result.status()).isEqualTo(Status.UNSUPPORTED);
    assertThat(result.reason()).isEqualTo("JOINT_ROLL_MODEL_NOT_IMPLEMENTED");
    assertThat(result.outcomes()).isEmpty();
    assertThat(result.unresolved()).isEqualTo(Fraction.ONE);
    var existing =
        item(
            ItemState.Rarity.RARE,
            List.of(new ModifierInstance("joint", Map.of("cold", 1L, "all", 1L))));
    var removal =
        model.expand(
            new BasicCurrencyState(existing, model.provenance()),
            WorkbenchCurrency.ANNULMENT,
            Set.of(),
            1);
    assertThat(removal.status()).isEqualTo(Status.COMPLETE);
    assertThat(removal.outcomes().getFirst().state().item().explicits()).isEmpty();
    var preserve =
        model.expand(
            new BasicCurrencyState(existing, model.provenance()),
            WorkbenchCurrency.EXALTED,
            Set.of(),
            100);
    assertThat(preserve.status()).isEqualTo(Status.COMPLETE);
    assertThat(preserve.outcomes())
        .allSatisfy(
            o ->
                assertThat(o.state().item().explicits()).contains(existing.explicits().getFirst()));
  }

  @Test
  void tieredPoolRetainsHighestEligibleFamilyBelowThresholdInsteadOfInventingTierCutoffs() {
    var low =
        new ModifierDefinition(
            "a-low",
            "a-low",
            a.layer(),
            a.affixType(),
            Set.of("a"),
            1,
            1,
            2,
            "a",
            a.stats(),
            a.tags(),
            a.sourceUrl());
    var high =
        new ModifierDefinition(
            "a-high",
            "a-high",
            a.layer(),
            a.affixType(),
            Set.of("a"),
            30,
            5,
            1,
            "a",
            a.stats(),
            a.tags(),
            a.sourceUrl());
    var model = new BasicCurrencyTransitions(catalog(List.of(implicit, low, high, b)), RULESET);
    var root = new BasicCurrencyState(item(ItemState.Rarity.RARE, List.of()), model.provenance());
    var ordinary = model.expand(root, WorkbenchCurrency.EXALTED, Set.of(), 100);
    assertThat(
            ordinary.outcomes().stream()
                .map(o -> o.state().item().explicits().getFirst().modifierId())
                .distinct())
        .containsExactly("a-low", "a-high", "b");
    for (var currency :
        List.of(WorkbenchCurrency.GREATER_EXALTED, WorkbenchCurrency.PERFECT_EXALTED)) {
      var tiered = model.expand(root, currency, Set.of(), 100);
      assertThat(
              tiered.outcomes().stream()
                  .map(o -> o.state().item().explicits().getFirst().modifierId())
                  .distinct())
          .containsExactly("a-high", "b");
      normalized(tiered);
    }
  }

  @Test
  void nullEmptyUnsupportedAndProvenanceMismatchesAreExplicit() {
    var root = state(item(ItemState.Rarity.NORMAL, List.of()));
    assertThatThrownBy(() -> new BasicCurrencyTransitions(catalog, " "))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> kernel.expand(null, WorkbenchCurrency.TRANSMUTATION, Set.of(), 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> kernel.expand(root, null, Set.of(), 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> kernel.expand(root, WorkbenchCurrency.TRANSMUTATION, null, 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> kernel.expand(root, WorkbenchCurrency.TRANSMUTATION, Set.of(), 0))
        .isInstanceOf(IllegalArgumentException.class);
    var foreign = new BasicCurrencyTransitions(catalog, "synthetic-ruleset-two");
    assertThatThrownBy(
            () ->
                kernel.expand(
                    new BasicCurrencyState(root.item(), foreign.provenance()),
                    WorkbenchCurrency.TRANSMUTATION,
                    Set.of(),
                    1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(
            kernel
                .expand(root, WorkbenchCurrency.TRANSMUTATION, Set.of("Omen_of_Whittling"), 1)
                .status())
        .isEqualTo(Status.UNSUPPORTED);
    for (var action :
        List.of(
            WorkbenchCurrency.DIVINE,
            WorkbenchCurrency.ESSENCE_BODY,
            WorkbenchCurrency.ALCHEMY,
            WorkbenchCurrency.FRACTURING,
            WorkbenchCurrency.ARTIFICER))
      assertThat(kernel.expand(root, action, Set.of(), 1).status()).isEqualTo(Status.UNSUPPORTED);
    assertThat(expand(root.item(), WorkbenchCurrency.EXALTED).status())
        .isEqualTo(Status.UNAVAILABLE);
  }
}
