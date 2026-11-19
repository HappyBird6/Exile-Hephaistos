package com.poe2craft.shared.crafting.domain;

import static com.poe2craft.shared.crafting.domain.ModifierAcquisition.Kind.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.item.EquipmentCurrencyCatalog;
import com.poe2craft.shared.item.ItemState.Rarity;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import org.junit.jupiter.api.Test;

/**
 * 테스트 작업대의 획득처를 실제 카탈로그의 레벨·희귀도·복수 경로와 대조한다. 장갑은 일반·에센스·합금을, 주얼은 고정·감정액·수동 공개를, 장화는 설치한 증강물만 여는 추첨
 * 풀을 검증한다.
 */
class ModifierAcquisitionTest {
  /** 같은 일반 옵션을 주는 에센스, 레벨 경계, 화폐별 훼손 후보와 합금 전용 옵션을 검증한다. */
  @Test
  void preservesEveryReviewedSourceWithoutInventingOrdinaryRolls() {
    var simulator = new StockyCraftingFixture().simulator;
    var rotund = "stocky-mitts:prefix:rotund";
    var low = simulator.modifierSources(45);
    assertThat(low).doesNotContainKey(rotund);
    var atLevel = simulator.modifierSources(46).get(rotund);
    var essence = new ModifierAcquisition(ESSENCE, "GREATER_ESSENCE_BODY");
    assertThat(atLevel)
        .contains(
            new ModifierAcquisition(NORMAL, null),
            essence,
            new ModifierAcquisition(DESECRATION, "Gnawed_Rib"),
            new ModifierAcquisition(DESECRATION, "Ancient_Rib"));
    assertThat(essence.supportsRarity(Rarity.MAGIC)).isFalse();
    assertThat(essence.supportsRarity(Rarity.RARE)).isTrue();
    assertThat(simulator.modifierSources(65).get(rotund))
        .doesNotContain(new ModifierAcquisition(DESECRATION, "Gnawed_Rib"));
    assertThat(low.get("stocky-mitts:prefix:sanguine"))
        .doesNotContain(new ModifierAcquisition(DESECRATION, "Ancient_Rib"));
    assertThat(low.get("stocky-mitts:prefix:alloy-elemental-penetration"))
        .containsExactly(new ModifierAcquisition(ALLOY, "PRISMATIC_ALLOY"));
    var high = simulator.modifierSources(82);
    // of Fury는 일반 풀 가중치가 500인 에센스 교체 대상이다. 실제 전용 옵션은 별도로 검증한다.
    assertThat(high.get("stocky-mitts:suffix:of-fury"))
        .containsExactly(
            new ModifierAcquisition(NORMAL, null),
            new ModifierAcquisition(ESSENCE, "ESSENCE_HYSTERIA"),
            new ModifierAcquisition(DESECRATION, "Preserved_Rib"),
            new ModifierAcquisition(DESECRATION, "Ancient_Rib"));
    assertThat(high.get("stocky-mitts:prefix:essence-abyssal-mark"))
        .containsExactly(new ModifierAcquisition(ESSENCE, "ESSENCE_ABYSS"));
  }

  /** 주얼의 고정 옵션과 감정액·수동 공개 전용 옵션이 일반 추첨으로 섞이지 않는지 검증한다. */
  @Test
  void separatesImplicitLiquidAndManualRevealOnAJewel() {
    var catalog = ItemCatalogLoader.loadBasicJewel("time-lost-ruby");
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var sources = simulator.modifierSources(100);
    assertThat(sources.get(catalog.base().implicitModifierId()))
        .containsExactly(new ModifierAcquisition(IMPLICIT, null));
    var liquid = WorkbenchCurrency.ANCIENT_DILUTED_LIQUID_IRE;
    var targets =
        catalog.modifiers().values().stream()
            .filter(
                d ->
                    d.tags().contains("crafted")
                        && d.sourceUrl().equals(liquid.replacementSource()))
            .toList();
    assertThat(targets)
        .isNotEmpty()
        .allSatisfy(
            d ->
                assertThat(sources.get(d.id()))
                    .containsExactly(new ModifierAcquisition(LIQUID, liquid.name())));
    var reveals =
        simulator.catalog().modifiers().values().stream()
            .filter(d -> d.tags().contains("reveal_only"))
            .toList();
    assertThat(reveals)
        .isNotEmpty()
        .allSatisfy(
            d ->
                assertThat(sources.get(d.id()))
                    .containsExactly(new ModifierAcquisition(DESECRATION, "Preserved_Cranium")));
  }

  /** 다른 세계 전용 공개 옵션에는 해당 풀을 여는 목뼈만 연결되는지 확인한다. */
  @Test
  void restrictsOtherworldlyOptionsToTheAlteredCollarbone() {
    var catalog = ItemCatalogLoader.loadDefault();
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var sources = simulator.modifierSources(100);
    var otherworldly =
        simulator.catalog().modifiers().values().stream()
            .filter(d -> d.tags().contains("otherworldly"))
            .toList();
    assertThat(otherworldly)
        .isNotEmpty()
        .allSatisfy(
            d ->
                assertThat(sources.get(d.id()))
                    .containsExactly(new ModifierAcquisition(DESECRATION, "Altered_Collarbone")));
  }

  /** 증강물이 열어 주는 장화 옵션은 그 증강물의 ID에만 연결되는지 검증한다. */
  @Test
  void listsTheAugmentThatUnlocksThePool() {
    var catalog = EquipmentCurrencyCatalog.enrich(ItemCatalogLoader.loadTopBase("tasalian"));
    var sources = new WorkbenchSimulator(catalog, new CraftingEngine(catalog)).modifierSources(100);
    var options =
        catalog.modifiers().values().stream()
            .filter(d -> d.tags().contains("chronomancy_pool"))
            .toList();
    assertThat(options)
        .isNotEmpty()
        .allSatisfy(
            d ->
                assertThat(sources.get(d.id()))
                    .containsExactly(new ModifierAcquisition(AUGMENT, "Uhtreds_Sidereus")));
  }
}
