package com.poe2craft.craftingsupport.goal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.ModifierAcquisition;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.item.ModifierDefinition.AffixType;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** 실제 장갑과 주얼에서 한도가 부족한 경우에만 지원 옵션·증강물을 사용하며, 이미 찬 홈과 획득 자격을 지키는지 확인한다. */
class GoalExampleCapacityTest {
  /** 접미 넷은 Serle 장착으로 충족한다. 이미 사용 중인 홈과 장착 제한을 검사한 뒤 일반 제작과 같은 상태를 돌려준다. */
  @Test
  void installsSerleOnlyWhenNeededAndNeverOverwritesARequiredAugment() {
    var catalog = ItemCatalogLoader.loadStocky();
    var capacity = new GoalExampleCapacity(catalog, Map.of());
    var suffixes = normal(catalog, AffixType.SUFFIX, 4);
    var fits = state(catalog, suffixes.subList(0, 3));
    assertThat(capacity.complete(fits)).isSameAs(fits);
    var full = state(catalog, suffixes);
    var withPool = GoalExampleCapacity.install(full, "Kolrs_Hunt");
    assertThat(capacity.complete(withPool).installedAugments())
        .extracting(ItemState.InstalledAugment::augmentId)
        .containsExactly("Kolrs_Hunt", "Serles_Triumph");
    var occupied = GoalExampleCapacity.install(withPool, "Astrids_Creativity");
    assertThat(capacity.complete(occupied)).isNull();
    assertThat(capacity.limits(fits).suffixes()).isEqualTo(4);
    var amulet = ItemCatalogLoader.loadDefault();
    assertThat(GoalExampleCapacity.install(state(amulet, List.of()), "Serles_Triumph")).isNull();
  }

  /** 마법봉의 교체형 화폐로 얻을 수 있는 두 전용 옵션을 동시에 유지할 때만 Astrid가 필요하다. */
  @Test
  void installsAstridForTwoAcquirableCraftedAffixes() {
    var catalog = ItemCatalogLoader.loadWand();
    var sources = sources(catalog);
    var capacity = new GoalExampleCapacity(catalog, sources);
    var crafted = new ArrayList<ModifierInstance>();
    var families = new HashSet<String>();
    for (var d : catalog.modifiers().values()) {
      if (sources.getOrDefault(d.id(), List.of()).stream()
              .noneMatch(
                  s ->
                      s.supportsRarity(ItemState.Rarity.RARE)
                          && (s.kind() == ModifierAcquisition.Kind.ALLOY
                              || s.kind() == ModifierAcquisition.Kind.ESSENCE))
          || d.weight() != 0
          || !Collections.disjoint(families, d.familyIds())) continue;
      crafted.add(minimum(d).asCrafted());
      families.addAll(d.familyIds());
      if (crafted.size() == 2) break;
    }
    assertThat(crafted).hasSize(2);
    var one = state(catalog, crafted.subList(0, 1));
    assertThat(capacity.complete(one)).isSameAs(one);
    var two = capacity.complete(state(catalog, crafted));
    assertThat(two.installedAugments())
        .extracting(ItemState.InstalledAugment::augmentId)
        .containsExactly("Astrids_Creativity");
    assertThat(new ItemStateValidator(catalog).validateForGeneration(two)).isEmpty();
  }

  /** 어느 쪽이든 세 번째 주얼 옵션을 만들려면 감정액이 주는 반대편 접사를 실제로 같이 넣어야 한다. */
  @ParameterizedTest
  @EnumSource(
      value = AffixType.class,
      names = {"PREFIX", "SUFFIX"})
  void materializesTheAcquirableJewelCapacityAffix(AffixType side) {
    var catalog = ItemCatalogLoader.loadBasicJewel("ruby");
    var sources = sources(catalog);
    var capacity = new GoalExampleCapacity(catalog, sources);
    var triples = normal(catalog, side, 3);
    var two = state(catalog, triples.subList(0, 2));
    assertThat(capacity.complete(two)).isSameAs(two);
    var requested = state(catalog, triples);
    var result = capacity.complete(requested);
    assertThat(result.explicits()).hasSize(4).containsAll(triples);
    var support =
        result.explicits().stream().filter(m -> !triples.contains(m)).findFirst().orElseThrow();
    assertThat(BasicJewel.extra(List.of(support.modifierId()), side)).isOne();
    assertThat(support.crafted()).isTrue();
    assertThat(sources.get(support.modifierId()))
        .extracting(ModifierAcquisition::kind)
        .contains(ModifierAcquisition.Kind.LIQUID);
    assertThat(new ItemStateValidator(catalog).validateForGeneration(result)).isEmpty();
    assertThat(new GoalExampleCapacity(catalog, Map.of()).complete(requested)).isNull();
    var opposite = side == AffixType.PREFIX ? AffixType.SUFFIX : AffixType.PREFIX;
    var full = new ArrayList<>(triples);
    full.addAll(normal(catalog, opposite, 2));
    assertThat(capacity.complete(state(catalog, full))).isNull();
  }

  /** 카탈로그의 레벨 82 제작기가 반환한 실제 옵션별 획득처다. */
  private static Map<String, List<ModifierAcquisition>> sources(ItemCatalog catalog) {
    return new WorkbenchSimulator(catalog, new CraftingEngine(catalog)).modifierSources(82);
  }

  /** 서로 충돌하지 않으며 레벨이 맞는 일반 가중치 옵션을 요구한 접사 개수만큼 고른다. */
  private static List<ModifierInstance> normal(ItemCatalog catalog, AffixType side, int count) {
    var result = new ArrayList<ModifierInstance>();
    var families = new HashSet<String>();
    for (var d : catalog.modifiers().values()) {
      if (d.affixType() != side
          || d.weight() <= 0
          || d.requiredItemLevel() > 82
          || !Collections.disjoint(families, d.familyIds())) continue;
      result.add(minimum(d));
      families.addAll(d.familyIds());
      if (result.size() == count) break;
    }
    assertThat(result).hasSize(count);
    return result;
  }

  /** 정의의 모든 수치를 해당 티어 최솟값으로 설정한다. */
  private static ModifierInstance minimum(ModifierDefinition d) {
    var values = new HashMap<String, Long>();
    d.stats().forEach(s -> values.put(s.id(), s.min()));
    return new ModifierInstance(d.id(), values);
  }

  /** 그 베이스의 고정 옵션과 주어진 명시 옵션을 갖는 82레벨 희귀 예시를 만든다. */
  private static ItemState state(ItemCatalog catalog, List<ModifierInstance> modifiers) {
    return new ItemState(
        catalog.metadata().snapshotId(),
        catalog.base().id(),
        82,
        ItemState.Rarity.RARE,
        catalog.base().hasImplicit()
            ? List.of(minimum(catalog.find(catalog.base().implicitModifierId()).orElseThrow()))
            : List.of(),
        modifiers,
        Set.of());
  }
}
