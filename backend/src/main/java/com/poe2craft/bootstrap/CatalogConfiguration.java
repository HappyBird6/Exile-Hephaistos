package com.poe2craft.bootstrap;

import com.poe2craft.shared.crafting.application.CatalogCraftingService;
import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.WorkbenchDefinitions;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.crafting.infrastructure.ReviewedEssencesLoader;
import com.poe2craft.shared.crafting.infrastructure.WorkbenchDefinitionsLoader;
import com.poe2craft.shared.item.EquipmentCurrencyCatalog;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemObservations;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.SolarTextMapper;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 공유 JSON 변환기·아이템 카탈로그·제작 서비스를 Spring 빈으로 연결한다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code itemStateJson()}: 아이템·상태·JSON 값을 조회·구성한다. 반환: {@code
 *       org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer}.
 *   <li>{@code workbenchDefinitions()}: 작업대·정의목록 값을 조회·구성한다. 반환: {@code WorkbenchDefinitions}.
 *   <li>{@code itemCatalog(definitions)}: 아이템·카탈로그 처리에서 {@code EquipmentCurrencyCatalog.enrich},
 *       {@code ItemCatalogLoader.loadDefault}을(를) 사용한다. 입력: {@code definitions}(정의목록). 반환: {@code
 *       ItemCatalog}.
 *   <li>{@code craftingEngine(catalog)}: 제작·계산엔진의 입력에 따른 분기·결과를 처리한다. 입력: {@code catalog}(카탈로그).
 *       반환: {@code CraftingEngine}.
 *   <li>{@code workbenchSimulator(catalog, engine)}: 기본 베이스에도 JSON에 등록한 추가·교체 후보를 적용해 시뮬레이터를 생성한다.
 *       입력: {@code catalog}(카탈로그), {@code engine}(계산엔진). 반환: {@code WorkbenchSimulator}.
 *   <li>{@code workbenchService(catalog, simulator)}: 작업대·서비스 처리에서 {@code
 *       ItemCatalogLoader.loadStocky}, {@code ItemCatalogLoader.loadBow}을(를) 사용한다. 입력: {@code
 *       catalog}(카탈로그), {@code simulator}(제작시뮬레이터). 반환: {@code CatalogCraftingService}.
 *   <li>{@code solarTextMapper(workbench)}: 태양목걸이·텍스트·변환기 처리에서 {@code
 *       workbench.reviewedCatalogs}을(를) 사용한다. 입력: {@code workbench}(작업대). 반환: {@code
 *       SolarTextMapper}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code ItemStateJson}: {@code CatalogConfiguration} 내부의 아이템·상태·JSON 처리를 맡는다.
 *   <li>{@code ObservationJson}: {@code CatalogConfiguration} 내부의 관측값·JSON 처리를 맡는다.
 *   <li>{@code ModifierJson}: {@code CatalogConfiguration} 내부의 옵션·JSON 처리를 맡는다.
 *   <li>{@code DesecrationJson}: {@code CatalogConfiguration} 내부의 훼손·JSON 처리를 맡는다.
 * </ul>
 */
@Configuration
public class CatalogConfiguration {
  // Keep absent observation fields out of legacy protocol consumers; domain values stay
  // Jackson-free.
  /**
   * {@code CatalogConfiguration} 내부의 아이템·상태·JSON 처리를 맡는다.
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code installedAugments()}: 장착된·증강물목록 값을 조회·구성한다. 반환: {@code
   *       java.util.List<ItemState.InstalledAugment>}.
   *   <li>{@code sanctification()}: 신성화 값을 조회·구성한다. 반환: {@code ItemState.Sanctification}.
   *   <li>{@code qualityInfused()}: 품질·주입된 값을 조회·구성한다. 반환: {@code boolean}.
   *   <li>{@code equipmentQuality()}: 장비·품질 값을 조회·구성한다. 반환: {@code Integer}.
   *   <li>{@code observations()}: 관측값목록 값을 조회·구성한다. 반환: {@code ItemObservations}.
   * </ul>
   */
  abstract static class ItemStateJson {
    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY)
    abstract java.util.List<ItemState.InstalledAugment> installedAugments();

    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    abstract ItemState.Sanctification sanctification();

    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT)
    abstract boolean qualityInfused();

    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    abstract Integer equipmentQuality();

    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    abstract ItemObservations observations();
  }

  /**
   * {@code CatalogConfiguration} 내부의 관측값·JSON 처리를 맡는다.
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code augmentEffectScale()}: 증강물·효과·단위배율 값을 조회·구성한다. 반환: {@code Double}.
   * </ul>
   */
  abstract static class ObservationJson {
    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    abstract Double augmentEffectScale();
  }

  /**
   * {@code CatalogConfiguration} 내부의 옵션·JSON 처리를 맡는다.
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code crafted()}: 제작옵션 값을 조회·구성한다. 반환: {@code boolean}.
   * </ul>
   */
  abstract static class ModifierJson {
    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_DEFAULT)
    abstract boolean crafted();
  }

  /**
   * {@code CatalogConfiguration} 내부의 훼손·JSON 처리를 맡는다.
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code minimumModifierLevel()}: 최소값·옵션·레벨 값을 조회·구성한다. 반환: {@code Integer}.
   *   <li>{@code revealPool()}: 공개·후보풀 값을 조회·구성한다. 반환: {@code String}.
   * </ul>
   */
  abstract static class DesecrationJson {
    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    abstract Integer minimumModifierLevel();

    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    abstract String revealPool();
  }

  @Bean
  public org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer
      itemStateJson() {
    return builder ->
        builder
            .mixIn(ItemState.class, ItemStateJson.class)
            .mixIn(ItemObservations.class, ObservationJson.class)
            .mixIn(com.poe2craft.shared.item.ModifierInstance.class, ModifierJson.class)
            .mixIn(ItemState.DesecratedSlot.class, DesecrationJson.class)
            .mixIn(AbyssCraftingState.Slot.class, DesecrationJson.class);
  }

  @Bean
  WorkbenchDefinitions workbenchDefinitions() {
    return WorkbenchDefinitionsLoader.initialize();
  }

  @Bean
  ItemCatalog itemCatalog(WorkbenchDefinitions definitions) {
    return EquipmentCurrencyCatalog.enrich(ItemCatalogLoader.loadDefault());
  }

  @Bean
  CraftingEngine craftingEngine(ItemCatalog catalog) {
    return new CraftingEngine(catalog);
  }

  @Bean
  WorkbenchSimulator workbenchSimulator(ItemCatalog catalog, CraftingEngine engine) {
    var targets = WorkbenchDefinitions.active().targets(catalog.base().id());
    return new WorkbenchSimulator(
        catalog, engine, Set.of(), targets.fixed(), targets.replacements());
  }

  @Bean
  CatalogCraftingService workbenchService(ItemCatalog catalog, WorkbenchSimulator simulator) {
    var service =
        new CatalogCraftingService(
            catalog,
            simulator,
            ItemCatalogLoader.loadStocky(),
            ItemCatalogLoader.loadBow(),
            ItemCatalogLoader.loadWand(),
            ItemCatalogLoader.loadBody(),
            ItemCatalogLoader.loadSceptre(),
            ItemCatalogLoader.loadBelt(),
            ItemCatalogLoader.loadHelmet(),
            ItemCatalogLoader.loadRing(),
            ItemCatalogLoader.loadSapphire(),
            ItemCatalogLoader.loadBasicJewel("ruby"),
            ItemCatalogLoader.loadBasicJewel("emerald"),
            ItemCatalogLoader.loadBasicJewel("diamond"),
            ItemCatalogLoader.loadBasicJewel("time-lost-ruby"),
            ItemCatalogLoader.loadBasicJewel("time-lost-emerald"),
            ItemCatalogLoader.loadBasicJewel("time-lost-sapphire"),
            ItemCatalogLoader.loadBasicJewel("time-lost-diamond"),
            ItemCatalogLoader.loadTopBase("soldier"),
            ItemCatalogLoader.loadTopBase("imperial"));
    var manifest = ReviewedEssencesLoader.load();
    for (var entry : manifest.entrySet()) {
      service.registerReviewedBase(
          entry.getKey(),
          ItemCatalogLoader.loadTopBase(entry.getKey()),
          entry.getValue().fixed(),
          entry.getValue().replacements());
    }
    return service;
  }

  @Bean
  SolarTextMapper solarTextMapper(CatalogCraftingService workbench) {
    return new SolarTextMapper(workbench.reviewedCatalogs());
  }
}
