package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.crafting.domain.BasicCurrencyTransitions.*;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import com.poe2craft.shared.probability.Fraction;
import com.poe2craft.shared.ruleset.RulesetManifestLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 기본·화폐·태양목걸이 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>기존 계약: Real sealed Solar integration cases, separate from the small synthetic oracle.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code workbench}: 작업대 ({@code WorkbenchSimulator}).
 *   <li>{@code kernel}: 전이커널 ({@code BasicCurrencyTransitions}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code input(rarity, mods)}: 입력 처리에서 {@code SolarAmulet.initial}, {@code
 *       root.snapshotId}을(를) 사용한다. 입력: {@code rarity}(희귀도), {@code mods}(옵션목록). 반환: {@code
 *       ItemState}.
 *   <li>{@code first()}: 최초 값을 조회·구성한다. 반환: {@code ModifierInstance}.
 *   <li>{@code sixteenActionsAgreeWithWorkbenchEligibilityRetainProvenanceAndNormalizeExactly()}:
 *       이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code ordinarySixAffixMarginalsMatchLegacyDistributionWithoutLosingNumericStates()}: 이름에
 *       명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code higherLevelCurrenciesAndFracturedInputsUseExistingRulesRatherThanNewTierRecipes()}:
 *       이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code
 *       otherBaseAndSpecialConditionsRemainUnsupportedWithoutChangingExistingWorkbenchSupport()}:
 *       이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
class BasicCurrencySolarTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  final WorkbenchSimulator workbench = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
  final BasicCurrencyTransitions kernel =
      new BasicCurrencyTransitions(catalog, RulesetManifestLoader.load().identity());

  ItemState input(ItemState.Rarity rarity, List<ModifierInstance> mods) {
    var root = SolarAmulet.initial(catalog);
    return new ItemState(
        root.snapshotId(),
        root.baseItemId(),
        root.itemLevel(),
        rarity,
        root.implicits(),
        mods,
        Set.of());
  }

  ModifierInstance first() {
    var definition =
        catalog.modifiers().values().stream()
            .filter(d -> d.layer() == ModifierDefinition.Layer.EXPLICIT)
            .findFirst()
            .orElseThrow();
    var range = definition.stats().getFirst();
    return new ModifierInstance(definition.id(), Map.of(range.id(), range.min()));
  }

  @Test
  void sixteenActionsAgreeWithWorkbenchEligibilityRetainProvenanceAndNormalizeExactly() {
    for (var action : BasicCurrencyTransitions.supportedActions()) {
      var rarity =
          switch (action.baseAction()) {
            case TRANSMUTATION -> ItemState.Rarity.NORMAL;
            case AUGMENTATION, REGAL -> ItemState.Rarity.MAGIC;
            default -> ItemState.Rarity.RARE;
          };
      var mods =
          action == WorkbenchCurrency.ANNULMENT || action.baseAction() == CraftingAction.CHAOS
              ? List.of(first())
              : List.<ModifierInstance>of();
      var root = input(rarity, mods);
      var source = new BasicCurrencyState(root, kernel.provenance());
      var expanded = kernel.expand(source, action, Set.of(), 100000);
      assertThat(expanded.status()).as(action.name()).isEqualTo(Status.COMPLETE);
      assertThat(
              workbench.actions(root, Set.of()).stream()
                  .filter(a -> a.action() == action)
                  .findFirst()
                  .orElseThrow()
                  .available())
          .isTrue();
      assertThat(
              expanded.outcomes().stream()
                  .map(Outcome::probability)
                  .reduce(Fraction.ZERO, Fraction::add))
          .isEqualTo(Fraction.ONE);
      assertThat(expanded.unresolved()).isEqualTo(Fraction.ZERO);
      assertThat(expanded.outcomes())
          .allSatisfy(
              o -> {
                assertThat(o.state().item().snapshotId()).isEqualTo(root.snapshotId());
                assertThat(o.state().item().implicits()).isEqualTo(root.implicits());
                assertThat(o.state().provenance()).isEqualTo(kernel.provenance());
                assertThat(o.state().provenance().rawSha256())
                    .isEqualTo(catalog.metadata().rawSha256());
                assertThat(new ItemStateValidator(catalog).validate(o.state().item())).isEmpty();
              });
    }
  }

  @Test
  void ordinarySixAffixMarginalsMatchLegacyDistributionWithoutLosingNumericStates() {
    var engine = new CraftingEngine(catalog);
    for (var action : CraftingAction.values()) {
      var rarity =
          switch (action) {
            case TRANSMUTATION -> ItemState.Rarity.NORMAL;
            case AUGMENTATION, REGAL -> ItemState.Rarity.MAGIC;
            default -> ItemState.Rarity.RARE;
          };
      var mods =
          action == CraftingAction.ANNULMENT || action == CraftingAction.CHAOS
              ? List.of(first())
              : List.<ModifierInstance>of();
      var root = input(rarity, mods);
      var full =
          kernel.expand(
              new BasicCurrencyState(root, kernel.provenance()),
              WorkbenchCurrency.valueOf(action.name()),
              Set.of(),
              100000);
      var marginalized = new HashMap<StateBucket, Fraction>();
      full.outcomes()
          .forEach(
              o ->
                  marginalized.merge(
                      StateBucket.from(o.state().item()), o.probability(), Fraction::add));
      var legacy = engine.transition(StateBucket.from(root), action);
      assertThat(marginalized).hasSize(legacy.outcomes().size());
      for (var outcome : legacy.outcomes()) {
        var fraction = marginalized.get(outcome.state());
        assertThat(fraction).isNotNull();
        double probability =
            fraction.numerator().doubleValue() / fraction.denominator().doubleValue();
        assertThat(probability).isCloseTo(outcome.probability(), within(1e-12));
      }
    }
  }

  @Test
  void higherLevelCurrenciesAndFracturedInputsUseExistingRulesRatherThanNewTierRecipes() {
    var normal = SolarAmulet.initial(catalog, 1, 15);
    var low =
        kernel.expand(
            new BasicCurrencyState(normal, kernel.provenance()),
            WorkbenchCurrency.PERFECT_TRANSMUTATION,
            Set.of(),
            100000);
    assertThat(low.status()).isEqualTo(Status.UNAVAILABLE);
    var selected = first();
    var fractured = new ModifierInstance(selected.modifierId(), selected.values(), true);
    var root = input(ItemState.Rarity.RARE, List.of(fractured));
    var blocked =
        kernel.expand(
            new BasicCurrencyState(root, kernel.provenance()),
            WorkbenchCurrency.CHAOS,
            Set.of(),
            100000);
    assertThat(blocked.status()).isEqualTo(Status.UNAVAILABLE);
    var added =
        kernel.expand(
            new BasicCurrencyState(root, kernel.provenance()),
            WorkbenchCurrency.PERFECT_EXALTED,
            Set.of(),
            100000);
    assertThat(added.status()).isEqualTo(Status.COMPLETE);
    assertThat(added.outcomes())
        .allSatisfy(o -> assertThat(o.state().item().explicits()).contains(fractured));
  }

  @Test
  void otherBaseAndSpecialConditionsRemainUnsupportedWithoutChangingExistingWorkbenchSupport() {
    var other = ItemCatalogLoader.loadSapphire();
    var otherKernel = new BasicCurrencyTransitions(other, RulesetManifestLoader.load().identity());
    var otherRoot =
        new ItemState(
            other.metadata().snapshotId(),
            other.base().id(),
            82,
            ItemState.Rarity.NORMAL,
            List.of(),
            List.of(),
            Set.of(),
            0);
    var unsupported =
        otherKernel.expand(
            new BasicCurrencyState(otherRoot, otherKernel.provenance()),
            WorkbenchCurrency.TRANSMUTATION,
            Set.of(),
            100);
    assertThat(unsupported.status()).isEqualTo(Status.UNSUPPORTED);
    assertThat(unsupported.reason()).isEqualTo("NUMERIC_BASE_NOT_IMPLEMENTED");
    assertThat(unsupported.source().item()).isSameAs(otherRoot);
    var root = SolarAmulet.initial(catalog);
    var corrupted =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            82,
            root.rarity(),
            root.implicits(),
            root.explicits(),
            Set.of(ItemState.Condition.CORRUPTED));
    assertThat(
            kernel
                .expand(
                    new BasicCurrencyState(corrupted, kernel.provenance()),
                    WorkbenchCurrency.TRANSMUTATION,
                    Set.of(),
                    100)
                .status())
        .isEqualTo(Status.UNSUPPORTED);
  }
}
