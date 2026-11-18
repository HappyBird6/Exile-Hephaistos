package com.poe2craft.shared.crafting.api.dto;

import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import java.util.List;
import java.util.Map;

/**
 * ruleVersion과 metadata는 규칙 버전과 카탈로그 출처다. id와 state는 초기 상태의 식별자와 값이다. modifiers와 actions는 옵션 사전과 행동
 * 목록이다. compatibleSnapshotIds는 호환 스냅샷, baseRules와 baseMaterialActions는 베이스 규칙과 제작 재료 행동이다. from은
 * 카탈로그에서 이 응답을 구성한다.
 */
public record Initial(
    String ruleVersion,
    ItemCatalog.Metadata metadata,
    String id,
    StateBucket state,
    Map<String, ModifierDefinition> modifiers,
    List<CraftingEngine.Availability> actions,
    List<String> compatibleSnapshotIds,
    ItemCatalog.BaseItem baseRules,
    List<WorkbenchCurrency> baseMaterialActions) {

  public static Initial from(
      ItemCatalog catalog,
      CraftingEngine engine,
      CraftingOperations workbench,
      int itemLevel,
      boolean manualReveal) {
    var displayCatalog = manualReveal ? ManualRevealCatalog.enrich(catalog) : catalog;
    var bucket = StateBucket.from(SolarAmulet.initial(catalog, itemLevel, 15));
    return new Initial(
        CraftingEngine.RULE_VERSION,
        displayCatalog.metadata(),
        bucket.id(),
        bucket,
        displayCatalog.modifiers(),
        engine.actions(bucket),
        catalog.compatibleSnapshotIds(),
        catalog.base(),
        workbench.initial("solar", itemLevel, manualReveal).baseMaterialActions());
  }
}
