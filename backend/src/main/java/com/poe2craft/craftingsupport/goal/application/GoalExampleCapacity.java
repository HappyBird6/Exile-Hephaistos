package com.poe2craft.craftingsupport.goal.application;

import com.poe2craft.shared.crafting.domain.AugmentSocketRules;
import com.poe2craft.shared.crafting.domain.ModifierAcquisition;
import com.poe2craft.shared.item.*;
import java.util.*;

/**
 * 완성 예시에 필요한 접두·접미·제작 옵션 한도를 실제 장착 또는 주얼 옵션 부여로 충족시킨다. catalog·validator는 베이스와 상태 검증을,
 * capacityAugments와 jewelModifiers는 이번 베이스·레벨의 지원 후보를 보관한다.
 */
final class GoalExampleCapacity {
  private final ItemCatalog catalog;
  private final ItemStateValidator validator;
  private final List<String> capacityAugments;
  private final List<ModifierDefinition> jewelModifiers;

  /** 각 한도를 독립적으로 달성할 수 있는 최대 접두 수, 접미 수, 제작 옵션 수다. */
  record Limits(int prefixes, int suffixes, int crafted) {}

  /** 해당 카탈로그에서 현재 레벨의 획득처가 확인된 희귀 주얼 확장 옵션과 장착 가능한 증강물만 선택한다. */
  GoalExampleCapacity(ItemCatalog catalog, Map<String, List<ModifierAcquisition>> sources) {
    this.catalog = catalog;
    validator = new ItemStateValidator(catalog);
    capacityAugments = AugmentCraftingRules.capacityAugments(catalog.base().id());
    jewelModifiers =
        BasicJewel.supportedCrafting(catalog.base().id())
            ? catalog.modifiers().values().stream()
                .filter(
                    d ->
                        extra(d, ModifierDefinition.AffixType.PREFIX)
                            || extra(d, ModifierDefinition.AffixType.SUFFIX))
                .filter(
                    d ->
                        sources.getOrDefault(d.id(), List.of()).stream()
                            .anyMatch(
                                s ->
                                    s.kind() == ModifierAcquisition.Kind.LIQUID
                                        && s.supportsRarity(ItemState.Rarity.RARE)))
                .filter(d -> d.stats().stream().allMatch(stat -> stat.min() == stat.max()))
                .toList()
            : List.of();
  }

  /** 정의 ID가 실제 주얼의 해당 접사 한도를 늘리는지 공용 슬롯 규칙으로 확인한다. */
  private static boolean extra(ModifierDefinition definition, ModifierDefinition.AffixType side) {
    return BasicJewel.extra(List.of(definition.id()), side) > 0;
  }

  /** 실제 설치 가능한 조합의 최대치와 희귀 주얼 확장을 합쳐 탐색 가지치기에만 쓸 상한을 구한다. */
  Limits limits(ItemState seed) {
    var limits = socketLimits(seed, 0);
    if (seed.rarity() != ItemState.Rarity.RARE || jewelModifiers.isEmpty()) return limits;
    var ids =
        jewelModifiers.stream()
            .filter(d -> d.requiredItemLevel() <= seed.itemLevel())
            .map(ModifierDefinition::id)
            .toList();
    return new Limits(
        limits.prefixes() + BasicJewel.extra(ids, ModifierDefinition.AffixType.PREFIX),
        limits.suffixes() + BasicJewel.extra(ids, ModifierDefinition.AffixType.SUFFIX),
        limits.crafted());
  }

  /** 동일 장착 순열을 생략하며 남는 소켓에서 실제 설치 가능한 한도 확장 결과를 검사한다. */
  private Limits socketLimits(ItemState item, int first) {
    var slots = validator.slots(item);
    int suffixes = slots.maxSuffixes(), crafted = AugmentCraftingRules.craftedLimit(item);
    if (item.rarity() != ItemState.Rarity.NORMAL) {
      for (int i = first; i < capacityAugments.size(); i++) {
        var next = installNext(item, capacityAugments.get(i));
        if (next == null) continue;
        var limits = socketLimits(next, i);
        suffixes = Math.max(suffixes, limits.suffixes());
        crafted = Math.max(crafted, limits.crafted());
      }
    }
    return new Limits(slots.maxPrefixes(), suffixes, crafted);
  }

  /** 원래 상태가 충분하면 그대로 검증한다. 희귀 주얼 접사 수가 넘을 때만 반대쪽 슬롯에 실제 확장 옵션을 붙인다. */
  ItemState complete(ItemState item) {
    var slots = validator.slots(item);
    if (item.rarity() == ItemState.Rarity.RARE
        && BasicJewel.supportedCrafting(item.baseItemId())
        && (slots.usedPrefixes() > slots.maxPrefixes()
            || slots.usedSuffixes() > slots.maxSuffixes())) {
      var side =
          slots.usedPrefixes() > slots.maxPrefixes()
              ? ModifierDefinition.AffixType.PREFIX
              : ModifierDefinition.AffixType.SUFFIX;
      for (var modifier : jewelModifiers) {
        if (modifier.requiredItemLevel() > item.itemLevel() || !extra(modifier, side)) continue;
        var values = new LinkedHashMap<String, Long>();
        modifier.stats().forEach(stat -> values.put(stat.id(), stat.min()));
        var explicits = new ArrayList<>(item.explicits());
        explicits.add(new ModifierInstance(modifier.id(), values).asCrafted());
        var valid = fitSockets(copy(item, explicits, item.augmentSockets()), 0);
        if (valid != null) return valid;
      }
      return null;
    }
    return fitSockets(item, 0);
  }

  /** 부족한 접미·제작 한도에 기여하는 증강물만 빈 홈에 넣는다. 최종 슬롯과 validator를 통과한 상태 하나를 반환한다. */
  private ItemState fitSockets(ItemState item, int first) {
    var slots = validator.slots(item);
    if (slots.usedPrefixes() > slots.maxPrefixes()) return null;
    boolean needsSuffix = slots.usedSuffixes() > slots.maxSuffixes();
    boolean needsCrafted =
        item.explicits().stream().filter(m -> AugmentCraftingRules.crafted(m, catalog)).count()
            > AugmentCraftingRules.craftedLimit(item);
    if (!needsSuffix && !needsCrafted)
      return validator.validateForGenerationRetainingFracture(item).isEmpty() ? item : null;
    if (item.rarity() == ItemState.Rarity.NORMAL) return null;
    for (int i = first; i < capacityAugments.size(); i++) {
      var id = capacityAugments.get(i);
      var effect = AugmentCraftingRules.effect(id);
      if (!(needsSuffix && effect.suffixBonus() > 0 || needsCrafted && effect.craftedBonus() > 0))
        continue;
      var next = installNext(item, id);
      if (next != null) {
        var result = fitSockets(next, i);
        if (result != null) return result;
      }
    }
    return null;
  }

  /** 옵션 풀에 필요한 증강물이 이미 장착되었으면 재사용하고, 아니면 신규 장착 자격을 적용한다. */
  static ItemState install(ItemState item, String id) {
    if (item.installedAugments().stream().anyMatch(a -> a.augmentId().equals(id))) return item;
    return installNext(item, id);
  }

  /** 기존 증강물을 교체하지 않고 가장 앞 빈 홈에 장착한다. 새 예시 상태에서 필요한 홈만 Artificer 규칙으로 확보한다. */
  private static ItemState installNext(ItemState item, String id) {
    if (!SocketAugments.supports(item.baseItemId(), id)) return null;
    int slot = 0;
    for (var installed :
        item.installedAugments().stream()
            .sorted(Comparator.comparingInt(ItemState.InstalledAugment::slotIndex))
            .toList()) {
      if (installed.slotIndex() != slot) break;
      slot++;
    }
    int count = Objects.requireNonNullElse(item.augmentSockets(), 0);
    if (slot == count) {
      var known = item.augmentSockets() == null ? copy(item, item.explicits(), count) : item;
      if (!AugmentSocketRules.refusal(known).isEmpty()) return null;
      item = copy(known, known.explicits(), count + 1);
    }
    try {
      return SocketAugments.install(item, slot, id);
    } catch (IllegalArgumentException unavailable) {
      // 장착 대상·동일 아이템 그룹·신규 장착 허용 여부는 공용 작업대 규칙이 최종 판정한다.
      return null;
    }
  }

  /** 검증 중 바뀐 명시 옵션과 홈 수만 교체하고 나머지 상태를 보존한다. */
  private static ItemState copy(ItemState item, List<ModifierInstance> explicits, Integer sockets) {
    return new ItemState(
        item.snapshotId(),
        item.baseItemId(),
        item.itemLevel(),
        item.rarity(),
        item.implicits(),
        explicits,
        item.conditions(),
        sockets,
        item.catalystQuality(),
        item.desecrated(),
        item.equipmentQuality(),
        item.observations(),
        item.sanctification(),
        item.installedAugments(),
        item.qualityInfused());
  }
}
