package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.crafting.application.CatalogCraftingService;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Rattling·셉터·에센스 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code simulator}: 공통 제작 규칙 실행기 ({@code WorkbenchSimulator}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code perfect()}: 완벽 값을 조회·구성한다. 반환: {@code Stream<WorkbenchCurrency>}.
 *   <li>{@code state(level, rarity, ids)}: 현재 상태 표현을 조회한다. 입력: {@code level}(레벨), {@code
 *       rarity}(희귀도), {@code ids}(식별자목록). 반환: {@code ItemState}.
 *   <li>{@code exactTargetsLevelsAtomicRefusalsAndNumericSource(action, suffixes)}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다. 입력: {@code action}(행동).
 *   <li>{@code crystallisationRemovalSidesLocksAndPairConflict(action)}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다. 입력: {@code action}(행동).
 *   <li>{@code completeWeightedPoolNoSpecialSpawnAndNoImplicitLottery()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다.
 *   <li>{@code independentSixthBaseAndPreviousCatalogsRemainIntact()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다.
 * </ul>
 */
class RattlingSceptreEssenceTest {
  final ItemCatalog catalog = ItemCatalogLoader.loadSceptre();
  final WorkbenchSimulator simulator =
      new WorkbenchSimulator(
          catalog,
          new CraftingEngine(catalog),
          catalog.modifiers().values().stream()
              .filter(d -> d.stats().size() > 1)
              .map(ModifierDefinition::id)
              .collect(java.util.stream.Collectors.toSet()),
          SceptreEssenceTargets.VERIFIED);

  static Stream<WorkbenchCurrency> perfect() {
    return Stream.of(WorkbenchCurrency.PERFECT_ESSENCE_COMMAND);
  }

  ItemState state(int level, ItemState.Rarity rarity, String... ids) {
    return new ItemState(
        catalog.metadata().snapshotId(),
        SceptreEssenceTargets.BASE_ID,
        level,
        rarity,
        List.of(),
        Arrays.stream(ids)
            .map(
                id -> {
                  var d = catalog.find(id).orElseThrow();
                  var values = new LinkedHashMap<String, Long>();
                  d.stats().forEach(s -> values.put(s.id(), s.min()));
                  return new ModifierInstance(id, values);
                })
            .toList(),
        Set.of());
  }

  @ParameterizedTest
  @CsvSource({
    "LESSER_ESSENCE_COMMAND,prefix:agitative",
    "ESSENCE_COMMAND,prefix:provocative",
    "GREATER_ESSENCE_COMMAND,prefix:motivating",
    "PERFECT_ESSENCE_COMMAND,suffix:essence-aura-magnitude",
    "LESSER_ESSENCE_INFINITE,suffix:of-the-wrestler suffix:of-the-student",
    "ESSENCE_INFINITE,suffix:of-the-lion suffix:of-the-augur",
    "GREATER_ESSENCE_INFINITE,suffix:of-the-goliath suffix:of-the-sage"
  })
  void exactTargetsLevelsAtomicRefusalsAndNumericSource(WorkbenchCurrency action, String suffixes) {
    boolean replacement = action == WorkbenchCurrency.PERFECT_ESSENCE_COMMAND;
    var expected = Arrays.stream(suffixes.split(" ")).map(s -> "rattling-sceptre:" + s).toList();
    var id = expected.getFirst();
    var level = catalog.find(id).orElseThrow().requiredItemLevel();
    var rarity = replacement ? ItemState.Rarity.RARE : ItemState.Rarity.MAGIC;
    var before = state(level, rarity, "rattling-sceptre:prefix:beryl");
    var choices = new HashSet<String>();
    for (int seed = 0; seed < 32; seed++) {
      var result = simulator.apply(before, action, Set.of(), new Random(seed * 104729L));
      assertThat(result.applied()).isTrue();
      var selected = result.events().getLast();
      choices.add(selected.modifierId());
      assertThat(selected.selectionProbability()).isEqualTo(1.0 / expected.size());
    }
    assertThat(choices).containsExactlyInAnyOrderElementsOf(expected);
    var r = simulator.apply(before, action, Set.of(WorkbenchOmen.BLESSED.id()), new Random(17));
    assertThat(r.applied()).isTrue();
    assertThat(r.state().rarity()).isEqualTo(ItemState.Rarity.RARE);
    var target = catalog.find(r.events().getLast().modifierId()).orElseThrow();
    assertThat(expected).contains(target.id());
    assertThat(target.requiredItemLevel()).isEqualTo(level);
    assertThat(new ItemStateValidator(catalog).validate(r.state())).isEmpty();
    assertThat(r.consumedOmens()).isEmpty();
    assertThat(r.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
    if (!replacement) assertThat(r.state().explicits()).containsAll(before.explicits());
    var range = target.stats().getFirst();
    if (range.min() != range.max())
      assertThat(r.assumptions())
          .anySatisfy(
              a -> {
                assertThat(a.id()).isEqualTo("assumed-source-integer-roll-v1");
                assertThat(a.sourceUrl()).isEqualTo(target.sourceUrl());
                assertThat(a.reason()).contains("UNVERIFIED");
              });
    else
      assertThat(r.assumptions())
          .noneSatisfy(a -> assertThat(a.id()).isEqualTo("assumed-source-integer-roll-v1"));
    var low = state(target.requiredItemLevel() - 1, rarity, "rattling-sceptre:prefix:beryl");
    var denied = simulator.apply(low, action, Set.of(), new Random(17));
    assertThat(denied.applied()).isFalse();
    assertThat(denied.state()).isEqualTo(low);
    assertThat(
            simulator
                .apply(state(82, ItemState.Rarity.NORMAL), action, Set.of(), new Random(17))
                .applied())
        .isFalse();
    var overlap =
        replacement
            ? state(82, rarity, id, "rattling-sceptre:prefix:beryl")
            : state(82, rarity, id);
    var conflict = simulator.apply(overlap, action, Set.of(), new Random(17));
    assertThat(conflict.applied()).isFalse();
    assertThat(conflict.state()).isEqualTo(overlap);
  }

  @ParameterizedTest
  @MethodSource("perfect")
  void crystallisationRemovalSidesLocksAndPairConflict(WorkbenchCurrency action) {
    var before =
        state(
            82,
            ItemState.Rarity.RARE,
            "rattling-sceptre:prefix:beryl",
            "rattling-sceptre:suffix:of-the-pupil");
    var seen = new HashSet<String>();
    for (int i = 0; i < 40; i++) {
      var r = simulator.apply(before, action, Set.of(), new Random(i * 104729L));
      assertThat(r.applied()).isTrue();
      assertThat(r.events().getFirst().selectionProbability()).isEqualTo(.5);
      seen.add(r.events().getFirst().modifierId());
    }
    assertThat(seen).hasSize(2);
    for (var omen :
        List.of(WorkbenchOmen.SINISTRAL_CRYSTALLISATION, WorkbenchOmen.DEXTRAL_CRYSTALLISATION)) {
      var r =
          simulator.apply(
              before, action, Set.of(omen.id(), WorkbenchOmen.BLESSED.id()), new Random(17));
      assertThat(r.applied()).isTrue();
      assertThat(catalog.find(r.events().getFirst().modifierId()).orElseThrow().affixType())
          .isEqualTo(omen.affix());
      assertThat(r.consumedOmens()).containsExactly(omen.id());
      assertThat(r.remainingOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
    }
    var locked =
        new ModifierInstance(
            before.explicits().getFirst().modifierId(),
            before.explicits().getFirst().values(),
            true);
    var state =
        new ItemState(
            before.snapshotId(),
            before.baseItemId(),
            82,
            before.rarity(),
            List.of(),
            List.of(locked, before.explicits().getLast()),
            Set.of());
    assertThat(simulator.apply(state, action, Set.of(), new Random(17)).state().explicits())
        .contains(locked);
    var refused =
        simulator.apply(
            before,
            action,
            Set.of(
                WorkbenchOmen.SINISTRAL_CRYSTALLISATION.id(),
                WorkbenchOmen.DEXTRAL_CRYSTALLISATION.id()),
            new Random(17));
    assertThat(refused.applied()).isFalse();
    assertThat(refused.state()).isEqualTo(before);
    assertThat(refused.consumedOmens()).isEmpty();
  }

  @Test
  void completeWeightedPoolNoSpecialSpawnAndNoImplicitLottery() {
    assertThat(catalog.modifiers()).hasSize(151);
    assertThat(catalog.modifiers().values().stream().filter(d -> d.weight() > 0)).hasSize(150);
    assertThat(catalog.metadata().prefixCount()).isEqualTo(70);
    assertThat(catalog.metadata().suffixCount()).isEqualTo(81);
    assertThat(catalog.metadata().prefixWeight()).isEqualTo(39600);
    assertThat(catalog.metadata().suffixWeight()).isEqualTo(62150);
    assertThat(catalog.base().hasImplicit()).isFalse();
    assertThat(catalog.modifiers().values().stream().filter(d -> d.stats().size() > 1)).hasSize(46);
    for (int i = 0; i < 60; i++) {
      var r =
          simulator.apply(
              state(82, ItemState.Rarity.NORMAL),
              WorkbenchCurrency.ALCHEMY,
              Set.of(),
              new Random(i));
      assertThat(r.applied()).isTrue();
      assertThat(r.state().explicits())
          .allSatisfy(
              m -> assertThat(catalog.find(m.modifierId()).orElseThrow().weight()).isPositive());
      assertThat(new ItemStateValidator(catalog).validate(r.state())).isEmpty();
    }
    assertThat(simulator.actions(state(82, ItemState.Rarity.NORMAL), Set.of()))
        .hasSize(59)
        .extracting(WorkbenchSimulator.Availability::action)
        .containsOnlyOnce(
            WorkbenchCurrency.ARCANISTS_ETCHER,
            WorkbenchCurrency.LESSER_ESSENCE_INFINITE,
            WorkbenchCurrency.ESSENCE_INFINITE,
            WorkbenchCurrency.GREATER_ESSENCE_INFINITE);
    assertThat(
            simulator
                .apply(
                    state(82, ItemState.Rarity.NORMAL),
                    WorkbenchCurrency.ARTIFICER,
                    Set.of(),
                    new Random(17))
                .applied())
        .isFalse();
  }

  @Test
  void independentSixthBaseAndPreviousCatalogsRemainIntact() {
    var solar = ItemCatalogLoader.loadDefault();
    var service =
        new CatalogCraftingService(
            solar,
            new WorkbenchSimulator(solar, new CraftingEngine(solar)),
            StockyCraftingFixture.stocky(),
            ItemCatalogLoader.loadBow(),
            ItemCatalogLoader.loadWand(),
            ItemCatalogLoader.loadBody(),
            catalog);
    assertThat(service.initial("sceptre", 82).state().baseItemId())
        .isEqualTo(SceptreEssenceTargets.BASE_ID);
    assertThat(service.initial("sceptre", 82).augmentSockets()).isNull();
    assertThat(service.initial("sceptre", 82).qualityLimit().maximumQuality()).isEqualTo(20);
    assertThat(service.initial("sceptre", 82).ruleVersion())
        .isEqualTo(
            "equipment-currency-v1-sceptre-workbench-essence-v1-homogenising-legacy-v1-legacy-five-v1-omen-composition-v2-fracture-whittling-v2-catalyst-max-v1-refined-sapphire-v1-quality-cap-preserve-v2-sanctification-unfractured-rounded-v3-user-selected-equipment-quality-maximum-v2-quality-infuser-once-plus-ten-v1-essence-full-side-v1-augment-weight-v1-resistance-magnitude-v1");
    for (var prior : Map.of("bow", 161, "wand", 204, "body", 161).entrySet()) {
      var modifiers = service.initial(prior.getKey(), 82).modifiers();
      assertThat(modifiers).hasSize(prior.getValue() + 2);
      assertThat(modifiers.values())
          .filteredOn(d -> d.familyIds().contains("EssenceAbyss"))
          .extracting(ModifierDefinition::affixType, ModifierDefinition::weight)
          .containsExactlyInAnyOrder(
              tuple(ModifierDefinition.AffixType.PREFIX, 0),
              tuple(ModifierDefinition.AffixType.SUFFIX, 0));
    }
    assertThat(service.initial("stocky", 82).modifiers()).hasSize(245);
    assertThat(service.initial("solar", 82).modifiers()).hasSize(219);
    assertThat(
            new WorkbenchSimulator(solar, new CraftingEngine(solar))
                .apply(
                    SolarAmulet.initial(solar, 82, 15),
                    WorkbenchCurrency.ESSENCE_COMMAND,
                    Set.of(),
                    new Random(17))
                .applied())
        .isFalse();
  }
}
