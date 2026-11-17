package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.*;
import java.util.*;

/**
 * 베이스별 제작 재료 후보를 시작 시 검증·해결해 공유한다. 후보 수나 적용 수치는 JSON 정책을 따른다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code essenceTargetOverrides}: 베이스별 에센스 추가 대상의 명시적 대체 목록 ({@code Map<WorkbenchCurrency,
 *       List<String>>}).
 *   <li>{@code replacementTargetOverrides}: 베이스별 교체 대상의 명시적 대체 목록 ({@code Map<WorkbenchCurrency,
 *       List<String>>}).
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code additions}: additions ({@code Map<WorkbenchCurrency, List<String>>}).
 *   <li>{@code replacements}: 교체목록 ({@code Map<WorkbenchCurrency, List<String>>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code WorkbenchMaterials(catalog, enriched, essenceTargetOverrides,
 *       replacementTargetOverrides)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code catalog}(카탈로그), {@code
 *       enriched}(enriched), {@code essenceTargetOverrides}(에센스·대상·대체설정), {@code
 *       replacementTargetOverrides}(교체·대상·대체설정). 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code essenceTargets(action)}: 해당 화폐의 에센스 추가 후보를 조회한다. 입력: {@code action}(행동). 반환: {@code
 *       List<String>}.
 *   <li>{@code replacementTargets(action)}: 해당 화폐의 교체 후보를 조회한다. 입력: {@code action}(행동). 반환: {@code
 *       List<String>}.
 *   <li>{@code resolveReplacements(action)}: 감정액 출처 또는 재료 정책에 맞는 교체 후보를 결정한다. 입력: {@code
 *       action}(행동). 반환: {@code List<String>}.
 *   <li>{@code targetFirstEssence(action)}: 합금·감정액을 제외한 대상 우선 교체인지 판정한다. 입력: {@code action}(행동).
 *       반환: {@code boolean}.
 *   <li>{@code essenceReplacementPlan(state, action, omen)}: 대상 우선 에센스 교체 분기를 공통 규칙으로 계산한다. 입력:
 *       {@code state}(상태), {@code action}(행동), {@code omen}(징조). 반환: {@code
 *       EssenceReplacementRules.Plan}.
 * </ul>
 */
final class WorkbenchMaterials {
  final Map<WorkbenchCurrency, List<String>> essenceTargetOverrides;
  final Map<WorkbenchCurrency, List<String>> replacementTargetOverrides;
  private final ItemCatalog catalog;
  private final Map<WorkbenchCurrency, List<String>> additions =
      new EnumMap<>(WorkbenchCurrency.class);
  private final Map<WorkbenchCurrency, List<String>> replacements =
      new EnumMap<>(WorkbenchCurrency.class);

  WorkbenchMaterials(
      ItemCatalog catalog,
      ItemCatalog enriched,
      Map<WorkbenchCurrency, List<String>> essenceTargetOverrides,
      Map<WorkbenchCurrency, List<String>> replacementTargetOverrides) {
    this.catalog = enriched;
    var replacements = new EnumMap<WorkbenchCurrency, List<String>>(WorkbenchCurrency.class);
    replacementTargetOverrides.forEach(
        (action, ids) -> {
          if (action.materialPolicy().mode() != WorkbenchDefinitions.MaterialMode.REPLACE
              || ids.isEmpty()
              || new HashSet<>(ids).size() != ids.size())
            throw new IllegalArgumentException(
                "Replacement override requires a complete distinct target set");
          for (var id : ids) {
            var definition =
                catalog
                    .find(id)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown replacement target"));
            if (definition.layer() != ModifierDefinition.Layer.EXPLICIT)
              throw new IllegalArgumentException("Replacement target must be explicit");
          }
          replacements.put(action, List.copyOf(ids));
        });
    EquipmentCurrencyCatalog.targets(catalog.base().id())
        .forEach(
            (action, ids) -> {
              if (ids.stream().allMatch(id -> enriched.find(id).isPresent()))
                replacements.put(WorkbenchCurrency.valueOf(action), ids);
            });
    this.replacementTargetOverrides = Map.copyOf(replacements);
    var reviewed = new EnumMap<WorkbenchCurrency, List<String>>(WorkbenchCurrency.class);
    essenceTargetOverrides.forEach(
        (action, ids) -> {
          if (action.materialPolicy().mode() != WorkbenchDefinitions.MaterialMode.ADD
              || ids.isEmpty()
              || new HashSet<>(ids).size() != ids.size())
            throw new IllegalArgumentException(
                "Only source-proven fixed basic essence overrides are supported");
          for (var id : ids) {
            var definition =
                catalog
                    .find(id)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown essence target"));
            if (definition.layer() != ModifierDefinition.Layer.EXPLICIT || definition.weight() <= 0)
              throw new IllegalArgumentException(
                  "Basic essence target requires a verified ordinary explicit");
          }
          reviewed.put(action, List.copyOf(ids));
        });
    this.essenceTargetOverrides = Map.copyOf(reviewed);

    for (var action : WorkbenchCurrency.values()) {
      additions.put(
          action, this.essenceTargetOverrides.getOrDefault(action, action.essenceModifierIds()));
      this.replacements.put(action, resolveReplacements(action));
    }
  }

  List<String> essenceTargets(WorkbenchCurrency action) {
    return additions.get(action);
  }

  List<String> replacementTargets(WorkbenchCurrency action) {
    return replacements.get(action);
  }

  private List<String> resolveReplacements(WorkbenchCurrency action) {
    if (action.isLiquid())
      return catalog.modifiers().values().stream()
          .filter(
              d -> d.tags().contains("crafted") && d.sourceUrl().equals(action.replacementSource()))
          .map(ModifierDefinition::id)
          .toList();
    return replacementTargetOverrides.getOrDefault(action, action.replacementModifiers());
  }

  boolean targetFirstEssence(WorkbenchCurrency action) {
    return !action.isAlloy() && !action.isLiquid() && !replacementTargets(action).isEmpty();
  }

  EssenceReplacementRules.Plan essenceReplacementPlan(
      AbyssCraftingState state, WorkbenchCurrency action, WorkbenchOmen omen) {
    var targets =
        replacementTargets(action).stream().map(id -> catalog.find(id).orElse(null)).toList();
    if (targets.stream().anyMatch(Objects::isNull))
      return new EssenceReplacementRules.Plan(List.of(), "UNREVIEWED_REPLACEMENT");
    return EssenceReplacementRules.plan(state, catalog, targets, omen);
  }
}
