package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * Homogenising 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code exalt}: 엑잘티드 ({@code String}).
 *   <li>{@code regal}: 제왕 ({@code String}).
 *   <li>{@code greater}: greater ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code mod(id, side, weight, tags)}: mod 처리에서 {@code ModifierDefinition.StatRange}을(를)
 *       사용한다. 입력: {@code id}(식별자), {@code side}(접두접미), {@code weight}(가중치), {@code tags}(태그목록). 반환:
 *       {@code ModifierDefinition}.
 *   <li>{@code catalog(matching)}: 카탈로그 처리에서 {@code ItemCatalogLoader.loadDefault}, {@code
 *       source.find}을(를) 사용한다. 입력: {@code matching}(일치). 반환: {@code ItemCatalog}.
 *   <li>{@code state(c, rarity, mods)}: 현재 상태 표현을 조회한다. 입력: {@code c}(c), {@code rarity}(희귀도),
 *       {@code mods}(옵션목록). 반환: {@code ItemState}.
 *   <li>{@code singleTagUnionUsesOrdinaryWeightsAndConsumesOnlyMatchingTrigger()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code doubleAdditionNeverExpandsTagsAndRetainsFracturedOriginal()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다.
 *   <li>{@code noTagsFallsBackButNoMatchingCandidateRefusesWithoutConsumingOrDrawing()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code AssertionError}.
 *   <li>{@code tieredDoubleAdditionWorksButUnknownCombinationAndSecondBranchRefuse()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
class HomogenisingTest {
  final String exalt = WorkbenchOmen.HOMOGENISING_EXALTATION.id();
  final String regal = WorkbenchOmen.HOMOGENISING_CORONATION.id();
  final String greater = WorkbenchOmen.GREATER_EXALTATION.id();

  ModifierDefinition mod(
      String id, ModifierDefinition.AffixType side, int weight, Set<String> tags) {
    return new ModifierDefinition(
        id,
        id,
        ModifierDefinition.Layer.EXPLICIT,
        side,
        Set.of(id),
        1,
        weight,
        1,
        id,
        List.of(new ModifierDefinition.StatRange(id, 1, 1)),
        tags,
        "https://example.test/" + id);
  }

  ItemCatalog catalog(boolean matching) {
    var source = ItemCatalogLoader.loadDefault();
    var p = ModifierDefinition.AffixType.PREFIX;
    var s = ModifierDefinition.AffixType.SUFFIX;
    var defs =
        List.of(
            mod("seed", p, 1, Set.of("life")),
            mod("bridge", s, 2, matching ? Set.of("life", "damage") : Set.of("damage")),
            mod("same", p, 3, matching ? Set.of("life") : Set.of("damage")),
            mod("new-tag-only", s, 9999, Set.of("damage")),
            source.find(source.base().implicitModifierId()).orElseThrow());
    var m = source.metadata();
    return new ItemCatalog(
        new ItemCatalog.Metadata(
            m.snapshotId(),
            m.retrievedAt(),
            m.sourceUrl(),
            m.weightPolicy(),
            m.rawSha256(),
            m.detailsSha256(),
            2,
            2,
            4,
            10001),
        source.base(),
        defs);
  }

  ItemState state(ItemCatalog c, ItemState.Rarity rarity, List<ModifierInstance> mods) {
    var root = SolarAmulet.initial(c);
    return new ItemState(
        root.snapshotId(), root.baseItemId(), 82, rarity, root.implicits(), mods, Set.of());
  }

  @Test
  void singleTagUnionUsesOrdinaryWeightsAndConsumesOnlyMatchingTrigger() {
    var c = catalog(true);
    var sim = new WorkbenchSimulator(c, new CraftingEngine(c));
    for (var rarity : List.of(ItemState.Rarity.MAGIC, ItemState.Rarity.RARE)) {
      var action =
          rarity == ItemState.Rarity.MAGIC ? WorkbenchCurrency.REGAL : WorkbenchCurrency.EXALTED;
      var omen = rarity == ItemState.Rarity.MAGIC ? regal : exalt;
      var other = rarity == ItemState.Rarity.MAGIC ? exalt : regal;
      var before = state(c, rarity, List.of(new ModifierInstance("seed", Map.of("seed", 1L))));
      for (int seed = 0; seed < 20; seed++) {
        var result = sim.apply(before, action, Set.of(omen, other), new Random(seed));
        assertThat(result.applied()).isTrue();
        assertThat(result.consumedOmens()).containsExactly(omen);
        assertThat(result.remainingOmens()).containsExactly(other);
        assertThat(result.state().explicits()).containsAll(before.explicits());
        assertThat(result.state().implicits()).isEqualTo(before.implicits());
        var event = result.events().getFirst();
        assertThat(event.modifierId()).isIn("bridge", "same");
        assertThat(event.selectionProbability())
            .isEqualTo(c.find(event.modifierId()).orElseThrow().weight() / 5.0);
      }
      var plan = new AdditionRules(c).plan(StateBucket.from(before), action, Set.of(omen));
      assertThat(plan.candidates())
          .extracting(ModifierDefinition::id)
          .containsExactly("bridge", "same");
    }
  }

  @Test
  void doubleAdditionNeverExpandsTagsAndRetainsFracturedOriginal() {
    var c = catalog(true);
    var sim = new WorkbenchSimulator(c, new CraftingEngine(c));
    var before =
        state(
            c,
            ItemState.Rarity.RARE,
            List.of(new ModifierInstance("seed", Map.of("seed", 1L), true)));
    var result =
        sim.apply(
            before,
            WorkbenchCurrency.EXALTED,
            Set.of(exalt, greater),
            new Random() {
              @Override
              public long nextLong(long bound) {
                return 0;
              }
            });
    assertThat(result.applied()).isTrue();
    assertThat(result.events())
        .extracting(WorkbenchSimulator.Event::modifierId)
        .containsExactly("bridge", "same");
    assertThat(result.events().getLast().selectionProbability()).isEqualTo(1);
    assertThat(result.consumedOmens()).containsExactlyInAnyOrder(exalt, greater);
    assertThat(result.state().explicits()).contains(before.explicits().getFirst());
    assertThat(
            new AdditionRules(c)
                .plan(StateBucket.from(before), WorkbenchCurrency.EXALTED, Set.of(exalt, greater))
                .available())
        .isFalse();
  }

  @Test
  void noTagsFallsBackButNoMatchingCandidateRefusesWithoutConsumingOrDrawing() {
    var c = catalog(false);
    var sim = new WorkbenchSimulator(c, new CraftingEngine(c));
    for (var rarity : List.of(ItemState.Rarity.MAGIC, ItemState.Rarity.RARE)) {
      var action =
          rarity == ItemState.Rarity.MAGIC ? WorkbenchCurrency.REGAL : WorkbenchCurrency.EXALTED;
      var omen = rarity == ItemState.Rarity.MAGIC ? regal : exalt;
      var empty = state(c, rarity, List.of());
      var ordinary = sim.apply(empty, action, Set.of(), new Random(12));
      var fallback = sim.apply(empty, action, Set.of(omen), new Random(12));
      assertThat(fallback.state()).isEqualTo(ordinary.state());
      assertThat(fallback.events()).isEqualTo(ordinary.events());
      assertThat(fallback.consumedOmens()).containsExactly(omen);
      var before = state(c, rarity, List.of(new ModifierInstance("seed", Map.of("seed", 1L))));
      var failed =
          sim.apply(
              before,
              action,
              Set.of(omen),
              new Random() {
                @Override
                public long nextLong(long bound) {
                  throw new AssertionError("Must not draw");
                }
              });
      assertThat(failed.applied()).isFalse();
      assertThat(failed.state()).isSameAs(before);
      assertThat(failed.consumedOmens()).isEmpty();
      assertThat(failed.events()).isEmpty();
    }
  }

  @Test
  void tieredDoubleAdditionWorksButUnknownCombinationAndSecondBranchRefuse() {
    var c = catalog(true);
    var sim = new WorkbenchSimulator(c, new CraftingEngine(c));
    var before =
        state(c, ItemState.Rarity.RARE, List.of(new ModifierInstance("seed", Map.of("seed", 1L))));
    for (var action : List.of(WorkbenchCurrency.GREATER_EXALTED, WorkbenchCurrency.PERFECT_EXALTED))
      assertThat(sim.apply(before, action, Set.of(exalt, greater), new Random()).applied())
          .isTrue();
    for (var other :
        List.of(WorkbenchOmen.SINISTRAL_EXALTATION.id(), WorkbenchOmen.DEXTRAL_EXALTATION.id()))
      assertThat(
              sim.apply(before, WorkbenchCurrency.EXALTED, Set.of(exalt, other), new Random())
                  .applied())
          .isFalse();
    var oneLeft =
        state(
            c,
            ItemState.Rarity.RARE,
            List.of(
                new ModifierInstance("seed", Map.of("seed", 1L)),
                new ModifierInstance("same", Map.of("same", 1L))));
    assertThat(
            sim.apply(oneLeft, WorkbenchCurrency.EXALTED, Set.of(exalt, greater), new Random())
                .applied())
        .isFalse();
  }
}
