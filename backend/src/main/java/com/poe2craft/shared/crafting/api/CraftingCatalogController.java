package com.poe2craft.shared.crafting.api;

import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/**
 * 작업대·추천·기존 탐색에서 공유하는 초기 카탈로그를 제공한다.
 *
 * <p>기존 계약: Existing Solar bootstrap shared by the workbench, support and compatibility explorer.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code engine}: 계산 실행기 ({@code CraftingEngine}).
 *   <li>{@code workbench}: 작업대 ({@code CraftingOperations}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code CraftingCatalogController(catalog, engine, workbench)}: 전달받은 값으로 구성원을 초기화한다. 입력:
 *       {@code catalog}(카탈로그), {@code engine}(계산엔진), {@code workbench}(작업대).
 *   <li>{@code initial(itemLevel)}: 제작을 시작할 초기 상태를 만든다. 입력: {@code itemLevel}(아이템·레벨). 반환: {@code
 *       Initial}.
 *   <li>{@code initial(itemLevel, manualReveal)}: 제작을 시작할 초기 상태를 만든다. 입력: {@code
 *       itemLevel}(아이템·레벨), {@code manualReveal}(수동·공개). 반환: {@code Initial}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Initial}: {@code CraftingCatalogController}에서 사용하는 초기값의 불변 값이다.
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/crafting")
public final class CraftingCatalogController {
  private final ItemCatalog catalog;
  private final CraftingEngine engine;
  private final CraftingOperations workbench;

  public CraftingCatalogController(
      ItemCatalog catalog, CraftingEngine engine, CraftingOperations workbench) {
    this.catalog = catalog;
    this.engine = engine;
    this.workbench = workbench;
  }

  public Initial initial(int itemLevel) {
    return initial(itemLevel, false);
  }

  @GetMapping("/initial")
  public Initial initial(
      @RequestParam(defaultValue = "82") int itemLevel,
      @RequestParam(defaultValue = "false") boolean manualReveal) {
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

  /**
   * {@code CraftingCatalogController}에서 사용하는 초기값의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code ruleVersion}: 규칙·버전 ({@code String}).
   *   <li>{@code metadata}: 메타데이터 ({@code ItemCatalog.Metadata}).
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code StateBucket}).
   *   <li>{@code modifiers}: 옵션목록 ({@code Map<String, ModifierDefinition>}).
   *   <li>{@code actions}: 행동목록 ({@code List<CraftingEngine.Availability>}).
   *   <li>{@code compatibleSnapshotIds}: 호환되는·스냅샷·식별자목록 ({@code List<String>}).
   *   <li>{@code baseRules}: 베이스·규칙목록 ({@code ItemCatalog.BaseItem}).
   *   <li>{@code baseMaterialActions}: 베이스·제작재료·행동목록 ({@code List<WorkbenchCurrency>}).
   * </ul>
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
      List<WorkbenchCurrency> baseMaterialActions) {}
}
