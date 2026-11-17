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
 * Hysteria·에센스 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code simulator}: 공통 제작 규칙 실행기 ({@code WorkbenchSimulator}).
 *   <li>{@code root}: 시작점 ({@code ItemState}).
 *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code WorkbenchCurrency}).
 *   <li>{@code target}: 대상; 초기값 {@code "amulet:suffix:of-suturing"} ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code rare(level, locked, ids)}: 희귀 처리에서 {@code root.snapshotId}, {@code
 *       root.baseItemId}을(를) 사용한다. 입력: {@code level}(레벨), {@code locked}(잠긴), {@code ids}(식별자목록).
 *       반환: {@code ItemState}.
 *   <li>{@code removesOnlyUnlockedInstancesAndPreservesEveryOtherRollWithExactEvidence()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code blocksAnyUnverifiedRemovalBranchWithoutFilteringOrMutatingTheItem()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code canReplaceTheSoleUnlockedRecoupInstanceWithoutCreatingADuplicateFamily()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code fullSuffixAutomaticallyRestrictsRemovalToSuffixes()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다.
 * </ul>
 */
class HysteriaEssenceTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  final WorkbenchSimulator simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
  final ItemState root = SolarAmulet.initial(catalog);
  final WorkbenchCurrency action = WorkbenchCurrency.ESSENCE_HYSTERIA;
  final String target = "amulet:suffix:of-suturing";

  ItemState rare(int level, String locked, String... ids) {
    return new ItemState(
        root.snapshotId(),
        root.baseItemId(),
        level,
        ItemState.Rarity.RARE,
        root.implicits(),
        Arrays.stream(ids)
            .map(
                id -> {
                  var stat = catalog.find(id).orElseThrow().stats().getFirst();
                  return new ModifierInstance(id, Map.of(stat.id(), stat.min()), id.equals(locked));
                })
            .toList(),
        root.conditions());
  }

  @Test
  void removesOnlyUnlockedInstancesAndPreservesEveryOtherRollWithExactEvidence() {
    var definition = catalog.find(target).orElseThrow();
    assertThat(definition.affixType()).isEqualTo(ModifierDefinition.AffixType.SUFFIX);
    assertThat(definition.stats().getFirst().min()).isEqualTo(19);
    assertThat(definition.stats().getFirst().max()).isEqualTo(21);
    for (var before :
        List.of(
            rare(68, "", "amulet:prefix:hale", "amulet:suffix:of-the-brute"),
            rare(
                82,
                "amulet:prefix:hale",
                "amulet:prefix:hale",
                "amulet:prefix:adept-s",
                "amulet:suffix:of-the-brute",
                "amulet:suffix:of-the-penguin"),
            rare(
                82,
                "amulet:prefix:hale",
                "amulet:prefix:hale",
                "amulet:suffix:of-the-brute",
                "amulet:suffix:of-the-penguin",
                "amulet:suffix:of-the-salamander"))) {
      var eligible = before.explicits().stream().filter(m -> !m.fractured()).toList();
      var reached = new HashSet<String>();
      for (int seed = 0; seed < 100; seed++) {
        var result =
            simulator.apply(
                before,
                action,
                Set.of(WorkbenchOmen.BLESSED.id(), WorkbenchOmen.WHITTLING.id()),
                new Random(seed * 104729L));
        assertThat(result.applied()).isTrue();
        assertThat(result.state().explicits()).hasSize(before.explicits().size());
        assertThat(result.state().implicits()).isEqualTo(before.implicits());
        assertThat(result.events()).hasSize(2);
        var removed = result.events().getFirst();
        assertThat(removed.kind()).isEqualTo("REMOVE");
        assertThat(removed.selectionProbability()).isEqualTo(1.0 / eligible.size());
        assertThat(eligible.stream().map(ModifierInstance::modifierId))
            .contains(removed.modifierId());
        for (var old : before.explicits())
          if (!old.modifierId().equals(removed.modifierId()))
            assertThat(result.state().explicits()).contains(old);
        var added = result.events().getLast();
        assertThat(added.kind()).isEqualTo("ADD");
        assertThat(added.modifierId()).isEqualTo(target);
        assertThat(added.selectionProbability()).isEqualTo(1);
        assertThat(result.assumptions().getFirst().id()).isEqualTo("uniform-removal-v1");
        assertThat(result.assumptions().getFirst().candidates())
            .containsExactlyElementsOf(
                eligible.stream().map(ModifierInstance::modifierId).toList());
        assertThat(result.assumptions().getLast().n()).isEqualTo(3);
        assertThat(new ItemStateValidator(catalog).validate(result.state())).isEmpty();
        assertThat(result.consumedOmens()).isEmpty();
        assertThat(result.remainingOmens())
            .containsExactlyInAnyOrder(WorkbenchOmen.BLESSED.id(), WorkbenchOmen.WHITTLING.id());
        reached.add(removed.modifierId());
      }
      assertThat(reached)
          .containsExactlyInAnyOrderElementsOf(
              eligible.stream().map(ModifierInstance::modifierId).toList());
    }
  }

  @Test
  void blocksAnyUnverifiedRemovalBranchWithoutFilteringOrMutatingTheItem() {
    var magic =
        simulator.apply(root, WorkbenchCurrency.TRANSMUTATION, Set.of(), new Random(1)).state();
    for (var before :
        List.of(
            root,
            magic,
            rare(82, ""),
            rare(67, "", "amulet:prefix:hale"),
            rare(82, target, target),
            rare(82, "", "amulet:prefix:hale", target))) {
      var result = simulator.apply(before, action, Set.of(), new Random(1));
      assertThat(result.applied()).isFalse();
      assertThat(result.state()).isEqualTo(before);
      assertThat(result.events()).isEmpty();
      assertThat(result.assumptions()).isEmpty();
    }
  }

  @Test
  void canReplaceTheSoleUnlockedRecoupInstanceWithoutCreatingADuplicateFamily() {
    var result = simulator.apply(rare(82, "", target), action, Set.of(), new Random(1));
    assertThat(result.applied()).isTrue();
    assertThat(result.state().explicits()).hasSize(1);
    assertThat(result.events().getFirst().modifierId()).isEqualTo(target);
    assertThat(result.events().getFirst().selectionProbability()).isEqualTo(1);
    assertThat(result.events().getLast().modifierId()).isEqualTo(target);
  }

  @Test
  void fullSuffixAutomaticallyRestrictsRemovalToSuffixes() {
    var full =
        rare(
            82,
            "",
            "amulet:prefix:hale",
            "amulet:suffix:of-the-brute",
            "amulet:suffix:of-the-penguin",
            "amulet:suffix:of-the-salamander");
    var result = simulator.apply(full, action, Set.of(), new Random(1));
    assertThat(result.applied()).as(result.reason()).isTrue();
    assertThat(catalog.find(result.events().getFirst().modifierId()).orElseThrow().affixType())
        .isEqualTo(ModifierDefinition.AffixType.SUFFIX);
  }
}
