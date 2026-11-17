package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 정신·파멸·에센스 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code simulator}: 공통 제작 규칙 실행기 ({@code WorkbenchSimulator}).
 *   <li>{@code root}: 시작점 ({@code ItemState}).
 *   <li>{@code actions}: 행동목록 ({@code List<WorkbenchCurrency>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code magic(level, ids)}: 마법 처리에서 {@code root.snapshotId}, {@code root.baseItemId}을(를)
 *       사용한다. 입력: {@code level}(레벨), {@code ids}(식별자목록). 반환: {@code ItemState}.
 *   <li>{@code fixedManaPrefixesAndChaosSuffixesPreserveOneOrTwoExistingModifiers()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code catalogLevelBoundaryIsSupportedButLowerLevelsAndFamilyOverlapAreNotGuessed()}: 이름에
 *       명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
class MindRuinEssenceTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  final WorkbenchSimulator simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
  final ItemState root = SolarAmulet.initial(catalog);
  final List<WorkbenchCurrency> actions =
      List.of(
          WorkbenchCurrency.LESSER_ESSENCE_MIND,
          WorkbenchCurrency.ESSENCE_MIND,
          WorkbenchCurrency.GREATER_ESSENCE_MIND,
          WorkbenchCurrency.LESSER_ESSENCE_RUIN,
          WorkbenchCurrency.ESSENCE_RUIN,
          WorkbenchCurrency.GREATER_ESSENCE_RUIN);

  ItemState magic(int level, String... ids) {
    return new ItemState(
        root.snapshotId(),
        root.baseItemId(),
        level,
        ItemState.Rarity.MAGIC,
        root.implicits(),
        Arrays.stream(ids)
            .map(
                id -> {
                  var stat = catalog.find(id).orElseThrow().stats().getFirst();
                  return new ModifierInstance(id, Map.of(stat.id(), stat.min()));
                })
            .toList(),
        root.conditions());
  }

  @Test
  void fixedManaPrefixesAndChaosSuffixesPreserveOneOrTwoExistingModifiers() {
    long[][] ranges = {{25, 34}, {80, 89}, {90, 104}, {4, 7}, {8, 11}, {16, 19}};
    for (int index = 0; index < actions.size(); index++) {
      var action = actions.get(index);
      var definition = catalog.find(action.fixedModifierId()).orElseThrow();
      assertThat(definition.affixType())
          .isEqualTo(
              index < 3
                  ? ModifierDefinition.AffixType.PREFIX
                  : ModifierDefinition.AffixType.SUFFIX);
      assertThat(definition.stats().getFirst().min()).isEqualTo(ranges[index][0]);
      assertThat(definition.stats().getFirst().max()).isEqualTo(ranges[index][1]);
      for (var before :
          List.of(
              magic(82, "amulet:suffix:of-the-brute"),
              magic(82, "amulet:suffix:of-the-brute", "amulet:prefix:adept-s")))
        for (int seed = 0; seed < 100; seed++) {
          var result =
              simulator.apply(
                  before, action, Set.of(WorkbenchOmen.BLESSED.id()), new Random(seed * 104729L));
          assertThat(result.applied()).isTrue();
          assertThat(result.state().rarity()).isEqualTo(ItemState.Rarity.RARE);
          assertThat(result.state().explicits())
              .hasSize(before.explicits().size() + 1)
              .containsAll(before.explicits());
          assertThat(result.state().implicits()).isEqualTo(before.implicits());
          assertThat(result.events()).hasSize(1);
          assertThat(result.events().getFirst().modifierId()).isEqualTo(definition.id());
          assertThat(result.events().getFirst().selectionProbability()).isEqualTo(1);
          assertThat(result.assumptions().getFirst().n())
              .isEqualTo(ranges[index][1] - ranges[index][0] + 1);
          assertThat(new ItemStateValidator(catalog).validate(result.state())).isEmpty();
          assertThat(result.consumedOmens()).isEmpty();
          assertThat(result.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
        }
    }
  }

  @Test
  void catalogLevelBoundaryIsSupportedButLowerLevelsAndFamilyOverlapAreNotGuessed() {
    for (var action : actions) {
      var definition = catalog.find(action.fixedModifierId()).orElseThrow();
      var boundary = magic(definition.requiredItemLevel(), "amulet:suffix:of-the-brute");
      assertThat(simulator.apply(boundary, action, Set.of(), new Random(1)).applied()).isTrue();
      var below = magic(definition.requiredItemLevel() - 1, "amulet:suffix:of-the-brute");
      var overlap = magic(82, definition.id());
      var rare = simulator.apply(boundary, action, Set.of(), new Random(1)).state();
      for (var before : List.of(root, below, overlap, rare)) {
        var result = simulator.apply(before, action, Set.of(), new Random(1));
        assertThat(result.applied()).isFalse();
        assertThat(result.state()).isEqualTo(before);
        assertThat(result.events()).isEmpty();
      }
    }
  }
}
