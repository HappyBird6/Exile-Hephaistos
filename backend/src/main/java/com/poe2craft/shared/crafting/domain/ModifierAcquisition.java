package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.AugmentCraftingRules;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import java.util.*;
import java.util.stream.Stream;

/**
 * 검토된 베이스에서 옵션을 얻을 수 있는 한 출처를 나타낸다. kind는 획득 종류, id는 구체 재료 ID이며 베이스 고정·일반 추첨은 재료를 특정하지 않는다.
 * supportsRarity는 결과 희귀도를 검사하고, forBase는 실제 제작기가 해석한 재료 규칙과 레벨별 카탈로그로 옵션별 출처를 구성한다.
 */
public record ModifierAcquisition(Kind kind, String id) {
  /** 모든 옵션이 함께 사용할 베이스 고정·일반 추첨 출처. 재료 ID가 필요 없다. */
  private static final ModifierAcquisition BASE = new ModifierAcquisition(Kind.IMPLICIT, null);

  private static final ModifierAcquisition ROLL = new ModifierAcquisition(Kind.NORMAL, null);

  /** 고정 옵션, 일반 추첨, 에센스, 합금, 감정액, 훼손 슬롯의 수동 공개, 증강물 장착 중 추첨을 구분한다. */
  public enum Kind {
    IMPLICIT,
    NORMAL,
    ESSENCE,
    ALLOY,
    LIQUID,
    DESECRATION,
    AUGMENT
  }

  /** 출처가 해당 최종 희귀도에서 가능한지 판정한다. 재료 교체는 입력 희귀도를 보존하고 추가는 정의된 결과 희귀도를 만든다. */
  public boolean supportsRarity(ItemState.Rarity rarity) {
    return switch (kind) {
      case IMPLICIT -> true;
      case NORMAL, AUGMENT -> rarity == ItemState.Rarity.MAGIC || rarity == ItemState.Rarity.RARE;
      case DESECRATION -> rarity == ItemState.Rarity.RARE;
      case ESSENCE, ALLOY, LIQUID -> {
        var policy = WorkbenchCurrency.valueOf(id).materialPolicy();
        yield policy.resultRarity() == null
            ? policy.inputRarities().contains(rarity)
            : policy.resultRarity() == rarity;
      }
    };
  }

  /** 각 옵션에 베이스·레벨·공개 풀 조건을 충족하는 출처들을 연결한다. 미등록 옵션에 대한 획득법은 추정하지 않는다. */
  static Map<String, List<ModifierAcquisition>> forBase(
      ItemCatalog catalog,
      int level,
      List<WorkbenchCurrency> actions,
      WorkbenchMaterials materials) {
    var sources = new TreeMap<String, List<ModifierAcquisition>>();
    var base = catalog.base().id();
    var bones =
        DesecrationCurrency.currencies(base, level).stream()
            .map(id -> new ModifierAcquisition(Kind.DESECRATION, id))
            .toList();
    for (var d : catalog.modifiers().values()) {
      if (d.layer() == ModifierDefinition.Layer.IMPLICIT) {
        if (d.id().equals(catalog.base().implicitModifierId())) add(sources, d.id(), BASE);
        continue;
      }
      if (d.requiredItemLevel() > level || d.tags().contains("augment_pool")) continue;
      if (d.weight() > 0) add(sources, d.id(), ROLL);
      for (var bone : bones) {
        var minimum = DesecrationCurrency.minimumModifierLevel(bone.id());
        if ((minimum == null || d.requiredItemLevel() >= minimum)
            && AbyssCraftingState.revealPoolEligible(DesecrationCurrency.revealPool(bone.id()), d))
          add(sources, d.id(), bone);
      }
    }

    for (var action : actions) {
      var materialKind = action.materialPolicy().kind();
      if (materialKind == WorkbenchDefinitions.MaterialKind.OTHER
          || action.minimumModifierLevel() > level) continue;
      var targets =
          Stream.concat(
                  materials.essenceTargets(action).stream(),
                  materials.replacementTargets(action).stream())
              .distinct()
              .toList();
      // 제작기는 선택될 수 있는 모든 지정 결과가 요구 레벨을 충족할 때만 이 재료를 허용한다.
      if (targets.stream()
          .anyMatch(id -> catalog.find(id).orElseThrow().requiredItemLevel() > level)) continue;
      var source = new ModifierAcquisition(Kind.valueOf(materialKind.name()), action.name());
      for (var target : targets) add(sources, target, source);
    }

    AugmentCraftingRules.poolSources(base)
        .forEach(
            (augment, ids) -> {
              var source = new ModifierAcquisition(Kind.AUGMENT, augment);
              for (var id : ids) {
                if (catalog.find(id).filter(d -> d.requiredItemLevel() <= level).isPresent())
                  add(sources, id, source);
              }
            });
    sources.replaceAll(
        (id, entries) ->
            entries.stream().sorted(Comparator.comparing(ModifierAcquisition::kind)).toList());
    return Collections.unmodifiableMap(sources);
  }

  /** 생성 중인 옵션별 목록에 이 재료의 획득 경로 하나를 추가한다. */
  private static void add(
      Map<String, List<ModifierAcquisition>> sources, String modifier, ModifierAcquisition source) {
    sources.computeIfAbsent(modifier, ignored -> new ArrayList<>()).add(source);
  }
}
