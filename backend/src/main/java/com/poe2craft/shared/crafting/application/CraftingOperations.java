package com.poe2craft.shared.crafting.application;

import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.item.CatalystQualityDisplay;
import java.util.*;
import java.util.random.RandomGenerator;

/**
 * 카탈로그 구현을 숨기고 제작 실행·검증의 공통 진입점을 제공한다.
 *
 * <p>기존 계약: Common catalog and simulation boundary; consumers do not choose catalog
 * implementations.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code initial(base, level)}: 제작을 시작할 초기 상태를 만든다. 입력: {@code base}(베이스), {@code level}(레벨).
 *       반환: {@code Initial}.
 *   <li>{@code initial(base, level, manualReveal)}: 제작을 시작할 초기 상태를 만든다. 입력: {@code base}(베이스),
 *       {@code level}(레벨), {@code manualReveal}(수동·공개). 반환: {@code Initial}.
 *   <li>{@code abyssCatalog(state)}: 훼손·카탈로그의 실행 계약을 정의한다. 입력: {@code state}(상태). 반환: {@code
 *       ItemCatalog}.
 *   <li>{@code simulator(state)}: 제작시뮬레이터의 실행 계약을 정의한다. 입력: {@code state}(상태). 반환: {@code
 *       WorkbenchSimulator}.
 *   <li>{@code abyssPlan(state, action, omens)}: 훼손 아이템의 다음 화폐 실행 가능성과 분기를 구한다. 입력: {@code
 *       state}(상태), {@code action}(행동), {@code omens}(징조목록). 반환: {@code AbyssCraftingPlan}.
 *   <li>{@code applyAbyss(state, action, omens, random)}: 훼손 슬롯을 보존하며 화폐를 실행한다. 입력: {@code
 *       state}(상태), {@code action}(행동), {@code omens}(징조목록), {@code random}(난수생성기). 반환: {@code
 *       AbyssCraftingState}.
 *   <li>{@code applyAbyssCurrency(state, action, omens, random)}: 훼손 상태의 공통 화폐 실행기로 전달한다. 입력:
 *       {@code state}(상태), {@code action}(행동), {@code omens}(징조목록), {@code random}(난수생성기). 반환:
 *       {@code AbyssCurrencyExecutor.Result}.
 *   <li>{@code apply(state, action, omens, random)}: 주어진 행동 또는 변경을 적용한 결과를 구한다. 입력: {@code
 *       state}(상태), {@code action}(행동), {@code omens}(징조목록), {@code random}(난수생성기). 반환: {@code
 *       WorkbenchSimulator.Result}.
 *   <li>{@code actions(state, omens)}: 현재 상태에서 가능한 행동과 불가 이유를 조회한다. 입력: {@code state}(상태), {@code
 *       omens}(징조목록). 반환: {@code List<WorkbenchSimulator.Availability>}.
 *   <li>{@code qualityDisplay(state)}: 품질·표시의 실행 계약을 정의한다. 입력: {@code state}(상태). 반환: {@code
 *       QualityDisplay}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code QualityDisplay}: {@code CraftingOperations}에서 사용하는 품질·표시의 불변 값이다.
 *   <li>{@code Initial}: {@code CraftingOperations}에서 사용하는 초기값의 불변 값이다.
 * </ul>
 */
public interface CraftingOperations extends CraftingCatalogs {
  Initial initial(String base, int level);

  Initial initial(String base, int level, boolean manualReveal);

  ItemCatalog abyssCatalog(ItemState state);

  WorkbenchSimulator simulator(ItemState state);

  AbyssCraftingPlan abyssPlan(
      AbyssCraftingState state, WorkbenchCurrency action, Set<String> omens);

  AbyssCraftingState applyAbyss(
      AbyssCraftingState state,
      WorkbenchCurrency action,
      Set<String> omens,
      RandomGenerator random);

  AbyssCurrencyExecutor.Result applyAbyssCurrency(
      AbyssCraftingState state,
      WorkbenchCurrency action,
      Set<String> omens,
      RandomGenerator random);

  WorkbenchSimulator.Result apply(
      ItemState state, WorkbenchCurrency action, Set<String> omens, RandomGenerator random);

  List<WorkbenchSimulator.Availability> actions(ItemState state, Set<String> omens);

  QualityDisplay qualityDisplay(ItemState state);

  /**
   * {@code CraftingOperations}에서 사용하는 품질·표시의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code ruleVersion}: 규칙·버전 ({@code String}).
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code ItemState}).
   *   <li>{@code qualityLimit}: 품질·한도 ({@code QualityLimitRules.Limit}).
   *   <li>{@code modifiers}: 옵션목록 ({@code List<CatalystQualityDisplay.Projection>}).
   * </ul>
   */
  public record QualityDisplay(
      String ruleVersion,
      ItemState state,
      QualityLimitRules.Limit qualityLimit,
      List<CatalystQualityDisplay.Projection> modifiers) {}

  /**
   * {@code CraftingOperations}에서 사용하는 초기값의 불변 값이다.
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
   *   <li>{@code qualityLimit}: 품질·한도 ({@code QualityLimitRules.Limit}).
   *   <li>{@code augmentSockets}: 증강물·장착홈수 ({@code Integer}).
   *   <li>{@code baseRules}: 베이스·규칙목록 ({@code ItemCatalog.BaseItem}).
   *   <li>{@code equipmentQuality}: 장비·품질 ({@code Integer}).
   *   <li>{@code baseMaterialActions}: 베이스·제작재료·행동목록 ({@code List<WorkbenchCurrency>}).
   *   <li>{@code compatibilityId}: 호환성·식별자 ({@code String}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Initial(ruleVersion, metadata, id, state, modifiers, actions,
   *       compatibleSnapshotIds, qualityLimit, augmentSockets, baseRules, equipmentQuality,
   *       baseMaterialActions)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code ruleVersion}(규칙·버전), {@code
   *       metadata}(메타데이터), {@code id}(식별자), {@code state}(상태), {@code modifiers}(옵션목록), {@code
   *       actions}(행동목록), {@code compatibleSnapshotIds}(호환되는·스냅샷·식별자목록), {@code
   *       qualityLimit}(품질·한도), {@code augmentSockets}(증강물·장착홈수), {@code baseRules}(베이스·규칙목록),
   *       {@code equipmentQuality}(장비·품질), {@code baseMaterialActions}(베이스·제작재료·행동목록).
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
      QualityLimitRules.Limit qualityLimit,
      Integer augmentSockets,
      ItemCatalog.BaseItem baseRules,
      Integer equipmentQuality,
      List<WorkbenchCurrency> baseMaterialActions,
      String compatibilityId) {
    public Initial(
        String ruleVersion,
        ItemCatalog.Metadata metadata,
        String id,
        StateBucket state,
        Map<String, ModifierDefinition> modifiers,
        List<CraftingEngine.Availability> actions,
        List<String> compatibleSnapshotIds,
        QualityLimitRules.Limit qualityLimit,
        Integer augmentSockets,
        ItemCatalog.BaseItem baseRules,
        Integer equipmentQuality,
        List<WorkbenchCurrency> baseMaterialActions) {
      this(
          ruleVersion,
          metadata,
          id,
          state,
          modifiers,
          actions,
          compatibleSnapshotIds,
          qualityLimit,
          augmentSockets,
          baseRules,
          equipmentQuality,
          baseMaterialActions,
          null);
    }
  }
}
