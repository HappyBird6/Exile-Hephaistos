package com.poe2craft.craftingsupport.testroutes.application;

import com.poe2craft.shared.item.*;
import java.util.*;

/** 목표 밖의 일반 티어들이 모든 후속 추첨·제거·목표 평가에서 동등할 때, 경로 분포의 키에서만 묶는다. 실제 예시에는 원래 아이템을 유지한다. */
final class RecipeStateGroups {
  /** 같은 쪽·배타 계열·태그·수치 구성의 옵션은 같은 후보를 막는다. Divine 사용 자격과 절사의 징조의 제거 순위를 구분한다. */
  private record Equivalent(
      ModifierDefinition.AffixType side,
      Set<String> families,
      Set<String> tags,
      String stat,
      boolean rerollable,
      int removalLevel) {}

  /** 각 동등 클래스의 실제 대표 옵션을 상태 비교에만 사용한다. */
  private final Map<String, ModifierInstance> representatives = new HashMap<>();

  /** 수치 목표·수치 보정·품질 한도·제작 옵션·주얼 특수 규칙 등 구체적 옵션을 읽는 경우에는 그대로 둔다. */
  RecipeStateGroups(
      ItemCatalog catalog,
      RouteCards.Card card,
      Set<String> targets,
      boolean presenceOnly,
      Set<String> numericStats) {
    if ((!presenceOnly && numericStats == null)
        || BasicJewel.supportedCrafting(catalog.base().id())) return;
    boolean hasWhittling =
        card.options().stream()
            .flatMap(o -> o.tiers().values().stream())
            .flatMap(List::stream)
            .anyMatch(
                s ->
                    (s.label().equals("B") || s.label().equals("A"))
                        && s.frequency() != RouteCards.Frequency.L);
    var first = new HashMap<Equivalent, ModifierInstance>();
    for (var d : catalog.modifiers().values()) {
      if (d.layer() != ModifierDefinition.Layer.EXPLICIT
          || d.weight() <= 0
          || d.stats().size() != 1
          || d.familyIds().isEmpty()
          || targets.contains(d.id())
          || ModifierMagnitudes.provider(d)) continue;
      var stat = d.stats().getFirst();
      if (!presenceOnly && numericStats.contains(stat.id())) continue;
      var mod = new ModifierInstance(d.id(), Map.of(stat.id(), stat.min()));
      if (AugmentCraftingRules.crafted(mod, catalog)
          || !QualitySanctification.targets(mod, catalog).contains(stat.id())) continue;
      var key =
          new Equivalent(
              d.affixType(),
              d.familyIds(),
              d.tags(),
              stat.id(),
              stat.max() > stat.min(),
              hasWhittling ? d.requiredItemLevel() : 0);
      representatives.put(d.id(), first.computeIfAbsent(key, k -> mod));
    }
  }

  /** 장착·품질·훼손 상태와 시작 분열을 보존하는 동등성 키를 만든다. 교체·보호 판단과 출력은 이 키로 실행하지 않는다. */
  ItemState key(ItemState item) {
    if (representatives.isEmpty() || !item.desecrated().isEmpty()) return item;
    List<ModifierInstance> normalized = null;
    for (int i = 0; i < item.explicits().size(); i++) {
      var current = item.explicits().get(i);
      if (current.fractured() || current.crafted()) continue;
      var substitute = representatives.get(current.modifierId());
      if (substitute == null || substitute.equals(current)) continue;
      if (normalized == null) normalized = new ArrayList<>(item.explicits());
      normalized.set(i, substitute);
    }
    if (normalized == null) return item;
    return withAffixes(item, item.rarity(), normalized);
  }

  /** 비교에 사용하는 희귀도와 명시 속성만 교체한다. 저장 및 응답에 넣는 실제 제작 상태는 원본을 사용한다. */
  static ItemState withAffixes(
      ItemState item, ItemState.Rarity rarity, List<ModifierInstance> explicits) {
    return new ItemState(
        item.snapshotId(),
        item.baseItemId(),
        item.itemLevel(),
        rarity,
        item.implicits(),
        explicits,
        item.conditions(),
        item.augmentSockets(),
        item.catalystQuality(),
        item.desecrated(),
        item.equipmentQuality(),
        item.observations(),
        item.sanctification(),
        item.installedAugments(),
        item.qualityInfused());
  }
}
