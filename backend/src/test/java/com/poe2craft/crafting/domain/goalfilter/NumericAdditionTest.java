package com.poe2craft.crafting.domain.goalfilter;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.*;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

class NumericAdditionTest {
  final ItemCatalog production = ItemCatalogLoader.loadDefault();

  ItemState empty(ItemCatalog catalog, ItemState.Rarity rarity, int level) {
    var root = SolarAmulet.initial(catalog, level, 15);
    return new ItemState(
        root.snapshotId(),
        root.baseItemId(),
        level,
        rarity,
        root.implicits(),
        List.of(),
        Set.of(),
        0);
  }

  ItemCatalog small() {
    var implicit = production.find(SolarAmulet.IMPLICIT_ID).orElseThrow();
    var prefix =
        definition(
            "cold", ModifierDefinition.AffixType.PREFIX, "base_cold_damage_resistance_%", 1, 2, 1);
    var suffix =
        definition("unrelated", ModifierDefinition.AffixType.SUFFIX, "base_maximum_life", 3, 4, 3);
    var m = production.metadata();
    return new ItemCatalog(
        new ItemCatalog.Metadata(
            m.snapshotId(),
            m.retrievedAt(),
            m.sourceUrl(),
            m.weightPolicy(),
            m.rawSha256(),
            m.detailsSha256(),
            1,
            1,
            1,
            3),
        production.base(),
        List.of(implicit, prefix, suffix));
  }

  ModifierDefinition definition(
      String id, ModifierDefinition.AffixType type, String stat, long min, long max, int weight) {
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
        List.of(new ModifierDefinition.StatRange(stat, min, max)),
        Set.of(),
        "https://example.test/synthetic");
  }

  @Test
  void allTwelveProductionVariantsRetainRollsAndExactlyNormalizedMass() {
    int tested = 0;
    for (var action : WorkbenchCurrency.values()) {
      if (!AdditionRules.isAddition(action)) continue;
      tested++;
      var rarity =
          switch (action.baseAction()) {
            case TRANSMUTATION -> ItemState.Rarity.NORMAL;
            case AUGMENTATION, REGAL -> ItemState.Rarity.MAGIC;
            default -> ItemState.Rarity.RARE;
          };
      var root = empty(production, rarity, 82);
      var result = new NumericAdditionKernel(production).expand(root, action, () -> true);
      assertThat(result.available()).isTrue();
      assertThat(
              result.outcomes().stream()
                  .map(Outcome::probability)
                  .reduce(Fraction.ZERO, Fraction::add))
          .isEqualTo(Fraction.ONE);
      assertThat(result.outcomes())
          .allSatisfy(
              o -> {
                assertThat(o.item().implicits()).isEqualTo(root.implicits());
                assertThat(o.item().augmentSockets()).isEqualTo(0);
                assertThat(o.item().explicits()).hasSize(1);
                assertThat(new ItemStateValidator(production).validate(o.item())).isEmpty();
              });
    }
    assertThat(tested).isEqualTo(12);
  }

  @Test
  void exhaustiveEightCasesMatchExactOracleAndUnrelatedWeightStaysInDenominator() {
    var catalog = small();
    var root = empty(catalog, ItemState.Rarity.RARE, 82);
    var kernel = new NumericAdditionKernel(catalog);
    java.util.function.Function<ItemState, Status> evaluate =
        item ->
            item.explicits().stream()
                    .anyMatch(
                        m ->
                            Long.valueOf(2).equals(m.values().get("base_cold_damage_resistance_%")))
                ? Status.MATCH
                : Status.NO_MATCH;
    var expanded = kernel.expand(root, WorkbenchCurrency.EXALTED, () -> true);
    assertThat(expanded.outcomes()).hasSize(4);
    assertThat(
            expanded.outcomes().stream()
                .filter(o -> evaluate.apply(o.item()) == Status.MATCH)
                .map(Outcome::probability)
                .reduce(Fraction.ZERO, Fraction::add))
        .isEqualTo(Fraction.of(1, 8));
    var sequence = List.of(WorkbenchCurrency.EXALTED, WorkbenchCurrency.EXALTED);
    var actual =
        new NumericAdditionSearch()
            .firstHit(
                root,
                sequence,
                kernel,
                evaluate,
                new NumericAdditionSearch.Budget(100, 100, 10000));
    var oracle =
        ExactNumericDistribution.firstHit(
            root,
            List.of(
                i -> kernel.expand(i, WorkbenchCurrency.EXALTED, () -> true).outcomes(),
                i -> kernel.expand(i, WorkbenchCurrency.EXALTED, () -> true).outcomes()),
            evaluate,
            100);
    assertThat(actual.exactMass().success())
        .isEqualTo(oracle.success())
        .isEqualTo(Fraction.of(1, 2));
    assertThat(actual.exactMass().failure()).isEqualTo(Fraction.of(1, 2));
    // The two generation orders converge only when both numeric rolls also agree.
    var two =
        kernel.expand(expanded.outcomes().getFirst().item(), WorkbenchCurrency.EXALTED, () -> true);
    assertThat(two.outcomes()).hasSize(2);
    assertThat(two.outcomes().getFirst().item()).isNotEqualTo(two.outcomes().getLast().item());
    assertThat(StateBucket.from(two.outcomes().getFirst().item()))
        .isEqualTo(StateBucket.from(two.outcomes().getLast().item()));
  }

  @Test
  void budgetIllegalTerminalAndFirstSuccessAreDistinct() {
    var catalog = small();
    var root = empty(catalog, ItemState.Rarity.RARE, 82);
    var kernel = new NumericAdditionKernel(catalog);
    var search = new NumericAdditionSearch();
    var partial =
        search.firstHit(
            root,
            List.of(WorkbenchCurrency.EXALTED),
            kernel,
            i -> Status.NO_MATCH,
            new NumericAdditionSearch.Budget(100, 1, 10000));
    assertThat(partial.complete()).isFalse();
    assertThat(partial.exactMass().failure().add(partial.exactMass().unresolved()))
        .isEqualTo(Fraction.ONE);
    assertThat(partial.exactMass().illegalActionFailure()).isEqualTo(Fraction.ZERO);
    var illegal =
        search.firstHit(
            root,
            List.of(WorkbenchCurrency.TRANSMUTATION),
            kernel,
            i -> Status.NO_MATCH,
            new NumericAdditionSearch.Budget(10, 10, 10000));
    assertThat(illegal.exactMass().illegalActionFailure()).isEqualTo(Fraction.ONE);
    var absorbed =
        search.firstHit(
            root,
            List.of(WorkbenchCurrency.TRANSMUTATION),
            kernel,
            i -> Status.MATCH,
            new NumericAdditionSearch.Budget(10, 10, 10000));
    assertThat(absorbed.exactMass().success()).isEqualTo(Fraction.ONE);
    assertThat(absorbed.exactMass().illegalActionFailure()).isEqualTo(Fraction.ZERO);
    assertThat(
            kernel
                .expand(
                    empty(catalog, ItemState.Rarity.RARE, 1),
                    WorkbenchCurrency.PERFECT_EXALTED,
                    () -> true)
                .available())
        .isFalse();
    assertThat(NumericAdditionSearch.sequences(root))
        .allSatisfy(s -> assertThat(s).allMatch(AdditionRules::isAddition));
  }
}
