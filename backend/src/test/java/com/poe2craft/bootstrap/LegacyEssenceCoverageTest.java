package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.crafting.application.CatalogCraftingService;
import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.crafting.domain.AbyssCurrencyExecutor;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.infrastructure.WorkbenchDefinitionsLoader;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 초기 등록 베이스 9종의 에센스 대상과 실제 제작 결과를 검증한다. 서비스는 운영 등록 경로로 한 번 구성한다. 대상 개수는 장비별 에센스 카드와 대조한 값이고, 속성·티어
 * ID는 해당 베이스의 카탈로그 원본을 따른다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LegacyEssenceCoverageTest {
  /** 모든 검사가 같은 불변 장비 카탈로그를 사용하는 작업대 서비스. */
  private final CatalogCraftingService service;

  /** 운영에서 사용하는 카탈로그 보강과 베이스별 화폐 등록을 실행한다. */
  LegacyEssenceCoverageTest() {
    var configuration = new CatalogConfiguration();
    var solar = configuration.itemCatalog(WorkbenchDefinitionsLoader.initialize());
    var simulator = configuration.workbenchSimulator(solar, configuration.craftingEngine(solar));
    service = configuration.workbenchService(solar, simulator);
  }

  /** 베이스별 에센스 수와 하위 무한의 에센스의 모든 적격 접미를 검사한 뒤 마법 아이템에 실제로 적용한다. */
  @ParameterizedTest
  @CsvSource({
    "ring,27,iron-ring,wrestler lynx student",
    "stocky,33,stocky-mitts,wrestler lynx",
    "bow,31,crude-bow,lynx",
    "wand,15,attuned-wand,student",
    "body,26,rusted-cuirass,wrestler",
    "sceptre,8,rattling-sceptre,wrestler student",
    "belt,24,rawhide-belt,wrestler",
    "helmet,30,rusted-greathelm,wrestler student",
    "solar,29,amulet,wrestler lynx student"
  })
  void appliesOnlyTheBasesSourcedInfiniteOutcomes(
      String base, int materialCount, String idPrefix, String suffixNames) {
    var magic = magic(base);
    var simulator = service.simulator(magic);
    var targets = simulator.referenceEssenceTargets();
    assertThat(targets).hasSize(materialCount);
    var expectedIds =
        Arrays.stream(suffixNames.split(" "))
            .map(name -> idPrefix + ":suffix:of-the-" + name)
            .toList();
    var action = WorkbenchCurrency.LESSER_ESSENCE_INFINITE;
    assertThat(targets.get(action.name())).containsExactlyInAnyOrderElementsOf(expectedIds);
    for (String id : expectedIds) {
      var stat = simulator.catalog().find(id).orElseThrow().stats().getFirst();
      assertThat(stat.min()).isEqualTo(9);
      assertThat(stat.max()).isEqualTo(12);
    }

    var result = service.apply(magic, action, Set.of(), new Random(75));
    assertThat(result.applied()).as(result.reason()).isTrue();
    assertThat(result.state().rarity()).isEqualTo(ItemState.Rarity.RARE);
    assertThat(result.state().implicits()).isEqualTo(magic.implicits());
    assertThat(result.state().explicits())
        .singleElement()
        .satisfies(
            modifier -> {
              assertThat(expectedIds).contains(modifier.modifierId());
              assertThat(modifier.values().values())
                  .singleElement()
                  .satisfies(value -> assertThat(value).isBetween(9L, 12L));
              assertThat(modifier.crafted()).isFalse();
            });

    var abyss = WorkbenchCurrency.ESSENCE_ABYSS;
    var marks = targets.get(abyss.name());
    assertThat(marks)
        .extracting(id -> simulator.catalog().find(id).orElseThrow().sourceUrl())
        .containsExactlyInAnyOrder(
            "https://poe2db.tw/us/hover?s=Data%5CMods%2FEssenceAbyssPrefix",
            "https://poe2db.tw/us/hover?s=Data%5CMods%2FEssenceAbyssSuffix");
    var marked = service.apply(result.state(), abyss, Set.of(), new Random(41));
    assertThat(marked.applied()).as(marked.reason()).isTrue();
    assertThat(marked.state().explicits())
        .singleElement()
        .satisfies(
            modifier -> {
              assertThat(marks).contains(modifier.modifierId());
              assertThat(modifier.values()).containsEntry("essence_abyss_guaranteed_pick", 1L);
            });
  }

  /** 동일 수치의 일반 옵션과 에센스 전용 옵션을 구분하고, 발작의 에센스가 장비별로 사용하는 실제 옵션 ID를 검증한다. */
  @ParameterizedTest
  @CsvSource({
    "ring,ESSENCE_HYSTERIA,iron-ring:suffix:of-euphoria",
    "helmet,ESSENCE_HYSTERIA,rusted-greathelm:suffix:of-the-taskmaster",
    "body,ESSENCE_HYSTERIA,rusted-cuirass:prefix:edged",
    "belt,ESSENCE_HYSTERIA,rawhide-belt:suffix:of-obsidian-skin",
    "wand,PERFECT_ESSENCE_SORCERY,attuned-wand:suffix:essence-spell-skill-level"
  })
  void replacementUsesTheClassSpecificModifier(String base, WorkbenchCurrency action, String id) {
    var magic = magic(base);
    assertThat(service.simulator(magic).referenceEssenceTargets().get(action.name()))
        .containsExactly(id);
    var upgraded =
        service.apply(magic, WorkbenchCurrency.LESSER_ESSENCE_INFINITE, Set.of(), new Random(2));
    assertThat(upgraded.applied()).as(upgraded.reason()).isTrue();
    var replaced = service.apply(upgraded.state(), action, Set.of(), new Random(3));
    assertThat(replaced.applied()).as(replaced.reason()).isTrue();
    assertThat(replaced.state().explicits())
        .singleElement()
        .satisfies(modifier -> assertThat(modifier.modifierId()).isEqualTo(id));
  }

  /** 명중 접두가 일반 풀에 있는 투구와 갑옷에서도 상위 전투의 에센스는 재료 목록과 실제 작업에서 제외한다. */
  @ParameterizedTest
  @ValueSource(strings = {"freebooter", "gladiatorial", "grinning", "ancestral", "cryptic"})
  void ordinaryAccuracyOnArmourDoesNotEnableGreaterBattle(String base) {
    var magic = magic(base);
    var simulator = service.simulator(magic);
    var accuracy = simulator.catalog().find("stocky-mitts:prefix:hunter-s").orElseThrow();
    assertThat(accuracy.weight()).isPositive();
    var roll = accuracy.stats().getFirst();
    assertThat(roll.id()).isEqualTo("accuracy_rating");
    assertThat(roll.min()).isEqualTo(237);
    assertThat(roll.max()).isEqualTo(346);
    var action = WorkbenchCurrency.GREATER_ESSENCE_BATTLE;
    assertThat(simulator.referenceEssenceTargets()).doesNotContainKey(action.name());
    assertThat(simulator.baseMaterialActions()).doesNotContain(action);
    var refused = service.apply(magic, action, Set.of(), new Random(3));
    assertThat(refused.applied()).isFalse();
    assertThat(refused.state()).isEqualTo(magic);
  }

  /** 생명력이 자연 생성되지 않는 집중구에서 전용 에센스만 같은 옵션을 추가하며 희귀·훼손 상태는 보존한다. */
  @Test
  void focusGetsLifeOnlyFromItsVerifiedLesserBodyEssence() {
    var magic = magic("tasalian-focus");
    var simulator = service.simulator(magic);
    var catalog = simulator.catalog();
    var action = WorkbenchCurrency.LESSER_ESSENCE_BODY;
    var option = catalog.find("stocky-mitts:prefix:sanguine").orElseThrow();
    assertThat(option.weight()).isZero();
    assertThat(simulator.sourceMaterialTargets(100).get(action)).containsExactly(option.id());
    assertThat(simulator.recipePlan(magic, WorkbenchCurrency.REGAL, Set.of()).branches())
        .noneMatch(branch -> branch.candidates().contains(option));
    assertThat(simulator.recipePlan(magic, action, Set.of()).branches())
        .singleElement()
        .satisfies(branch -> assertThat(branch.candidates()).containsExactly(option));
    var result = service.apply(magic, action, Set.of(), new Random(1));
    assertThat(result.applied()).as(result.reason()).isTrue();
    assertThat(result.state().rarity()).isEqualTo(ItemState.Rarity.RARE);
    var added = result.state().explicits().getFirst();
    assertThat(added.modifierId()).isEqualTo(option.id());
    assertThat(added.values().get("base_maximum_life")).isBetween(30L, 39L);
    assertThat(added.crafted()).isFalse();

    var occupied =
        new ItemState(
            magic.snapshotId(),
            magic.baseItemId(),
            magic.itemLevel(),
            ItemState.Rarity.MAGIC,
            magic.implicits(),
            List.of(added),
            magic.conditions());
    assertThat(service.apply(occupied, action, Set.of(), new Random(1)).applied()).isFalse();
    var hidden =
        AbyssCraftingState.from(result.state(), catalog)
            .addUnrevealed("abyss", ModifierDefinition.AffixType.SUFFIX, catalog);
    var inAbyss =
        new AbyssCurrencyExecutor(catalog, simulator)
            .apply(hidden, action, Set.of(), new Random(1));
    assertThat(inAbyss.applied()).isFalse();
    assertThat(inAbyss.state()).isEqualTo(hidden);
  }

  /** 장비의 검증된 암시 옵션과 소켓·품질 정보를 보존한 레벨 100 마법 시작 아이템을 만든다. */
  private ItemState magic(String base) {
    var initial = service.initial(base, 100);
    var state = initial.state();
    return new ItemState(
            state.snapshotId(),
            state.baseItemId(),
            state.itemLevel(),
            ItemState.Rarity.MAGIC,
            state.implicits(),
            List.of(),
            state.conditions(),
            initial.augmentSockets())
        .withEquipmentQuality(initial.equipmentQuality());
  }
}
