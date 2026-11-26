package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemObservations;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateWireDefaults;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.QualitySanctification;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 품질·최대값 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code whetstoneReachesReviewedOrdinaryCapForEverySupportedRarityAndRefusesRepeatedUse()}:
 *       이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code qualityPreservesUnrevealedFracturedObservedStateAndAllPendingOmens()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code ordinaryPolicyNeverExtendsToExceptionalAmuletCatalystCaps()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다.
 *   <li>{@code ordinaryPolicyRejectsConditionsUnknownQualityAndRepeatedDirectTargetRequests()}: 이름에
 *       명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code whetstoneRejectsCasterWeaponsAndJewellery()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
class QualityMaximumTest {
  @Test
  void whetstoneReachesReviewedOrdinaryCapForEverySupportedRarityAndRefusesRepeatedUse() {
    var catalog = ItemCatalogLoader.loadBow();
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    for (var rarity :
        List.of(ItemState.Rarity.NORMAL, ItemState.Rarity.MAGIC, ItemState.Rarity.RARE)) {
      for (int quality : List.of(0, 7, 19, 20, 30, 40, 100)) {
        var state =
            new ItemState(
                    catalog.metadata().snapshotId(),
                    catalog.base().id(),
                    82,
                    rarity,
                    List.of(),
                    List.of(),
                    Set.of())
                .withEquipmentQuality(quality);
        var result =
            simulator.apply(
                state,
                WorkbenchCurrency.BLACKSMITHS_WHETSTONE,
                Set.of("Omen_of_Sanctification"),
                new Random(1));
        assertThat(result.applied()).isEqualTo(quality < 20);
        assertThat(result.state()).isEqualTo(quality < 20 ? state.withEquipmentQuality(20) : state);
        assertThat(result.qualityLimit()).isEqualTo(QualityLimitRules.describe(state, catalog));
        assertThat(result.qualityLimit().maximumQuality()).isEqualTo(20);
        var repeated =
            simulator.apply(
                result.state(),
                WorkbenchCurrency.BLACKSMITHS_WHETSTONE,
                Set.of("Omen_of_Sanctification"),
                new Random(1));
        assertThat(repeated.applied()).isFalse();
        assertThat(repeated.state()).isEqualTo(result.state());
        assertThat(repeated.events()).isEmpty();
        assertThat(repeated.assumptions()).isEmpty();
        assertThat(repeated.consumedOmens()).isEmpty();
        assertThat(repeated.remainingOmens()).containsExactly("Omen_of_Sanctification");
        assertThat(result.consumedOmens()).isEmpty();
        assertThat(result.remainingOmens()).containsExactly("Omen_of_Sanctification");
        assertThat(result.events()).isEmpty();
        assertThat(result.assumptions()).hasSize(quality < 20 ? 1 : 0);
        assertThat(
                simulator
                    .apply(state, WorkbenchCurrency.ARMOURERS_SCRAP, Set.of(), new Random(1))
                    .applied())
            .isFalse();
      }
      var unknown =
          new ItemState(
              catalog.metadata().snapshotId(),
              catalog.base().id(),
              82,
              rarity,
              List.of(),
              List.of(),
              Set.of());
      assertThat(
              simulator
                  .apply(unknown, WorkbenchCurrency.BLACKSMITHS_WHETSTONE, Set.of(), new Random(1))
                  .applied())
          .isFalse();
    }
  }

  @Test
  void qualityPreservesUnrevealedFracturedObservedStateAndAllPendingOmens() {
    var fixture = new StockyCraftingFixture();
    var definition =
        fixture.catalog.modifiers().values().stream()
            .filter(d -> d.affixType() == ModifierDefinition.AffixType.PREFIX && d.weight() > 0)
            .findFirst()
            .orElseThrow();
    var values = new HashMap<String, Long>();
    definition.stats().forEach(s -> values.put(s.id(), s.min()));
    var state =
        new ItemState(
            fixture.root.snapshotId(),
            fixture.root.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            List.of(),
            List.of(new ModifierInstance(definition.id(), values, true)),
            Set.of(),
            0,
            null,
            List.of(
                new ItemState.DesecratedSlot(
                    "unrevealed", ModifierDefinition.AffixType.SUFFIX, null)),
            7,
            new ItemObservations(0, List.of("Preserved installed Augment effect"), List.of()));
    var omens = Set.of("Omen_of_Sanctification", "Omen_of_Whittling");
    var result =
        fixture.simulator.apply(state, WorkbenchCurrency.ARMOURERS_SCRAP, omens, new Random(1));
    assertThat(result.applied()).isTrue();
    assertThat(result.state()).isEqualTo(state.withEquipmentQuality(20));
    assertThatThrownBy(() -> AbyssCraftingState.from(state, fixture.catalog))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Augment");
    var resolvedSlots = state.withObservations(new ItemObservations(0, List.of(), List.of()));
    var slotAware =
        new AbyssCurrencyExecutor(fixture.catalog, fixture.simulator)
            .apply(
                AbyssCraftingState.from(resolvedSlots, fixture.catalog),
                WorkbenchCurrency.ARMOURERS_SCRAP,
                omens,
                new Random(1));
    assertThat(slotAware.applied()).isTrue();
    assertThat(slotAware.state().storedItem()).isEqualTo(resolvedSlots.withEquipmentQuality(20));
    var repeated =
        new AbyssCurrencyExecutor(fixture.catalog, fixture.simulator)
            .apply(slotAware.state(), WorkbenchCurrency.ARMOURERS_SCRAP, omens, new Random(1));
    assertThat(repeated.applied()).isFalse();
    assertThat(repeated.state()).isEqualTo(slotAware.state());
    assertThat(repeated.consumedOmens()).isEmpty();
    assertThat(repeated.remainingOmens()).containsExactlyInAnyOrderElementsOf(omens);
    assertThat(slotAware.consumedOmens()).isEmpty();
    assertThat(slotAware.remainingOmens()).containsExactlyInAnyOrderElementsOf(omens);
    assertThat(result.consumedOmens()).isEmpty();
    assertThat(result.remainingOmens()).containsExactlyInAnyOrderElementsOf(omens);
    var sanctified =
        state.withSanctification(QualitySanctification.roll(state, fixture.catalog, new Random(1)));
    var blocked =
        fixture.simulator.apply(
            sanctified, WorkbenchCurrency.ARMOURERS_SCRAP, omens, new Random(1));
    assertThat(blocked.applied()).isFalse();
    assertThat(blocked.state()).isEqualTo(sanctified);
    assertThat(blocked.consumedOmens()).isEmpty();
  }

  @Test
  void ordinaryPolicyNeverExtendsToExceptionalAmuletCatalystCaps() {
    var catalog = ItemCatalogLoader.loadDefault();
    var root = SolarAmulet.initial(catalog);
    var state =
        new ItemState(
            root.snapshotId(),
            root.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            root.implicits(),
            List.of(
                new ModifierInstance(
                    QualityLimitRules.BREACH_ID, Map.of(QualityLimitRules.STAT_ID, 20L))),
            Set.of());
    assertThat(QualityLimitRules.describe(state, catalog).maximumQuality()).isEqualTo(40);
    for (var action :
        List.of(
            WorkbenchCurrency.ARMOURERS_SCRAP,
            WorkbenchCurrency.BLACKSMITHS_WHETSTONE,
            WorkbenchCurrency.ARCANISTS_ETCHER)) {
      var result =
          new WorkbenchSimulator(catalog, new CraftingEngine(catalog))
              .apply(state, action, Set.of("Omen_of_Sanctification"), new Random(1));
      assertThat(result.applied()).isFalse();
      assertThat(result.state()).isEqualTo(state);
      assertThat(result.consumedOmens()).isEmpty();
      assertThat(result.remainingOmens()).containsExactly("Omen_of_Sanctification");
    }
  }

  @Test
  void ordinaryPolicyRejectsConditionsUnknownQualityAndRepeatedDirectTargetRequests() {
    var state =
        new ItemState(
                "fixture",
                "Metadata/Items/Armours/Gloves/FourGlovesStr1",
                82,
                ItemState.Rarity.RARE,
                List.of(),
                List.of(),
                Set.of())
            .withEquipmentQuality(7);
    for (var condition : ItemState.Condition.values()) {
      var special =
          new ItemState(
                  state.snapshotId(),
                  state.baseItemId(),
                  state.itemLevel(),
                  state.rarity(),
                  state.implicits(),
                  state.explicits(),
                  Set.of(condition))
              .withEquipmentQuality(7);
      assertThat(QualityCurrencyRules.refusal(special, WorkbenchCurrency.ARMOURERS_SCRAP))
          .isNotEmpty();
    }
    for (Integer amount : Arrays.asList(null, 20, 30, 40, 100))
      assertThatThrownBy(() -> QualityCurrencyRules.nextQuality(state.withEquipmentQuality(amount)))
          .isInstanceOf(IllegalArgumentException.class);
    var unique =
        new ItemState(
                state.snapshotId(),
                state.baseItemId(),
                state.itemLevel(),
                ItemState.Rarity.UNIQUE,
                List.of(),
                List.of(),
                Set.of())
            .withEquipmentQuality(7);
    assertThat(QualityCurrencyRules.refusal(unique, WorkbenchCurrency.ARMOURERS_SCRAP))
        .isNotEmpty();
    assertThat(QualityCurrencyRules.refusal(state, WorkbenchCurrency.DIVINE)).isNotEmpty();
  }

  /** 등록된 세 주문 무기 종류에서만 식각기를 적용하고 숫돌 및 두 바알 주입물의 경계를 대조한다. */
  @Test
  void etcherUsesRegisteredWeaponClassesIncludingTheSharedStaffMetadataDirectory() {
    var etcher = WorkbenchCurrency.ARCANISTS_ETCHER;
    var classes =
        Map.of(
            ItemCatalogLoader.loadWand(),
            true,
            ItemCatalogLoader.loadSceptre(),
            true,
            ItemCatalogLoader.loadTopBase("ravenous-staff"),
            true,
            ItemCatalogLoader.loadTopBase("bolting-quarterstaff"),
            false,
            ItemCatalogLoader.loadBow(),
            false);
    for (var entry : classes.entrySet()) {
      var catalog = entry.getKey();
      boolean caster = entry.getValue();
      var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
      for (var rarity :
          List.of(ItemState.Rarity.NORMAL, ItemState.Rarity.MAGIC, ItemState.Rarity.RARE)) {
        var item = weapon(catalog, rarity);
        assertThat(
                simulator.actions(item, Set.of()).stream()
                    .anyMatch(a -> a.action() == etcher && a.available()))
            .isEqualTo(caster);
        var result = simulator.apply(item, etcher, Set.of(), new Random(1));
        assertThat(result.applied()).isEqualTo(caster);
        assertThat(result.state()).isEqualTo(caster ? item.withEquipmentQuality(20) : item);
        assertThat(
                simulator
                    .availability(item, WorkbenchCurrency.BLACKSMITHS_WHETSTONE, Set.of())
                    .available())
            .isEqualTo(!caster);
        assertThat(
                simulator
                    .availability(item, WorkbenchCurrency.VAAL_BLACKSMITHS_INFUSER, Set.of())
                    .available())
            .isEqualTo(!caster);
        assertThat(
                simulator
                    .availability(item, WorkbenchCurrency.VAAL_ARCANISTS_INFUSER, Set.of())
                    .available())
            .isEqualTo(caster);
      }
    }
  }

  /** 훼손 슬롯·징조를 보존하고, 저장 후 재사용 및 기존의 미확정·상한 이상·타락 품질은 거부한다. */
  @Test
  void etchingPersistsQualityAndAnUnrevealedSlotWithoutConsumingOmens() throws Exception {
    var catalog = ItemCatalogLoader.loadTopBase("ravenous-staff");
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var etcher = WorkbenchCurrency.ARCANISTS_ETCHER;
    var omens = Set.of("Omen_of_Whittling");
    var item =
        weapon(catalog, ItemState.Rarity.RARE)
            .withDesecrated(
                List.of(
                    new ItemState.DesecratedSlot(
                        "unrevealed", ModifierDefinition.AffixType.PREFIX, null)));
    var applied =
        new AbyssCurrencyExecutor(catalog, simulator)
            .apply(AbyssCraftingState.from(item, catalog), etcher, omens, new Random(1));
    assertThat(applied.applied()).isTrue();
    assertThat(applied.consumedOmens()).isEmpty();
    assertThat(applied.remainingOmens()).containsExactlyElementsOf(omens);
    var result = applied.state().storedItem();
    assertThat(result).isEqualTo(item.withEquipmentQuality(20));
    var mapper = new ObjectMapper();
    var saved = mapper.readTree(mapper.writeValueAsBytes(result));
    var restored = mapper.treeToValue(ItemStateWireDefaults.normalized(saved), ItemState.class);
    assertThat(restored).isEqualTo(result);
    for (var blocked :
        List.of(restored, restored.withEquipmentQuality(30), item.withEquipmentQuality(null))) {
      var retry = simulator.apply(blocked, etcher, omens, new Random(1));
      assertThat(retry.applied()).isFalse();
      assertThat(retry.state()).isEqualTo(blocked);
      assertThat(retry.remainingOmens()).containsExactlyElementsOf(omens);
    }
    var corrupted = item.withConditions(Set.of(ItemState.Condition.CORRUPTED));
    assertThat(QualityCurrencyRules.refusal(corrupted, etcher)).isNotEmpty();
    assertThatThrownBy(() -> simulator.apply(corrupted, etcher, omens, new Random(1)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** 해당 베이스의 실제 내재 옵션을 가진 품질 7 무기를 지정 희귀도로 준비한다. */
  private static ItemState weapon(ItemCatalog catalog, ItemState.Rarity rarity) {
    var implicits = new ArrayList<ModifierInstance>();
    if (catalog.base().hasImplicit()) {
      var definition = catalog.find(catalog.base().implicitModifierId()).orElseThrow();
      var values = new TreeMap<String, Long>();
      definition.stats().forEach(stat -> values.put(stat.id(), stat.min()));
      implicits.add(new ModifierInstance(definition.id(), values));
    }
    return new ItemState(
            catalog.metadata().snapshotId(),
            catalog.base().id(),
            82,
            rarity,
            implicits,
            List.of(),
            Set.of())
        .withEquipmentQuality(7);
  }

  @Test
  void whetstoneRejectsCasterWeaponsAndJewellery() {
    for (var id :
        List.of(
            "Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand3",
            "Metadata/Items/Weapons/TwoHandWeapons/Staves/FourStaff1",
            "Metadata/Items/Weapons/OneHandWeapons/Sceptres/FourSceptre1",
            "Metadata/Items/Amulets/FourAmulet9")) {
      var state =
          new ItemState("fixture", id, 82, ItemState.Rarity.NORMAL, List.of(), List.of(), Set.of())
              .withEquipmentQuality(0);
      assertThat(QualityCurrencyRules.refusal(state, WorkbenchCurrency.BLACKSMITHS_WHETSTONE))
          .isNotEmpty();
    }
  }
}
