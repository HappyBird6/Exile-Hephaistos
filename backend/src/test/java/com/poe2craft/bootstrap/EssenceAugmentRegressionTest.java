package com.poe2craft.bootstrap;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.crafting.application.*;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.crafting.infrastructure.*;
import com.poe2craft.shared.item.*;
import java.util.*;
import org.junit.jupiter.api.Test;

/**
 * 에센스·증강물·Regression 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code service()}: 서비스 값을 조회·구성한다. 반환: {@code CatalogCraftingService}.
 *   <li>{@code minimum(d)}: 최소값 처리에서 {@code d.stats}, {@code values.put}을(를) 사용한다. 입력: {@code
 *       d}(d). 반환: {@code ModifierInstance}.
 *   <li>{@code empty(c)}: 빈 초기값 또는 비어 있는지 여부를 구한다. 입력: {@code c}(c). 반환: {@code ItemState}.
 *   <li>{@code fill(sim, state)}: fill 처리에서 {@code sim.apply}, {@code r.applied}을(를) 사용한다. 입력:
 *       {@code sim}(sim), {@code state}(상태). 반환: {@code ItemState}.
 *   <li>{@code allSixAugmentPoolsKeepRevealCandidatesAndFullItemEssencesUsable()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code astridCanRetainOneEssenceAndAddAnotherOnFullGloves()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다.
 *   <li>{@code sovereignResistancePlainGameTextRecoversRawRollAndProjectsThirtyNine()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
class EssenceAugmentRegressionTest {
  private CatalogCraftingService service() {
    var cfg = new CatalogConfiguration();
    var catalog = cfg.itemCatalog(WorkbenchDefinitionsLoader.initialize());
    return cfg.workbenchService(
        catalog, new WorkbenchSimulator(catalog, new CraftingEngine(catalog)));
  }

  private ModifierInstance minimum(ModifierDefinition d) {
    var values = new TreeMap<String, Long>();
    d.stats().forEach(s -> values.put(s.id(), s.min()));
    return new ModifierInstance(d.id(), values);
  }

  private ItemState empty(ItemCatalog c) {
    return new ItemState(
            c.metadata().snapshotId(),
            c.base().id(),
            100,
            ItemState.Rarity.RARE,
            c.base().hasImplicit()
                ? List.of(minimum(c.find(c.base().implicitModifierId()).orElseThrow()))
                : List.of(),
            List.of(),
            Set.of(),
            BaseRegistry.socketExecutionMaximum(c.base().id()))
        .withEquipmentQuality(BaseRegistry.policy(c.base().id()).qualityLimit() ? 0 : null);
  }

  private ItemState fill(WorkbenchSimulator sim, ItemState state) {
    for (int n = 0; n < 6; n++) {
      var r = sim.apply(state, WorkbenchCurrency.EXALTED, Set.of(), new Random(300 + n));
      if (!r.applied()) break;
      state = r.state();
    }
    return state;
  }

  @Test
  void allSixAugmentPoolsKeepRevealCandidatesAndFullItemEssencesUsable() {
    var service = service();
    int pairs = 0, applied = 0;
    for (var catalog : service.reviewedCatalogs())
      for (var id :
          List.of(
              "Uhtreds_Sidereus",
              "Kolrs_Hunt",
              "Voranas_Carnage",
              "Thruds_Might",
              "Medveds_Tending",
              "Katlas_Gloom")) {
        if (!SocketAugments.supports(catalog.base().id(), id)) continue;
        var initial = SocketAugments.install(empty(catalog), 0, id);
        var sim = service.simulator(initial);
        var active =
            sim.pool(initial, WorkbenchCurrency.EXALTED, null).stream()
                .filter(d -> d.tags().contains("augment_pool"))
                .toList();
        var reveal = new HashSet<String>();
        for (var side :
            List.of(ModifierDefinition.AffixType.PREFIX, ModifierDefinition.AffixType.SUFFIX)) {
          var abyss =
              AbyssCraftingState.from(initial, sim.catalog())
                  .addUnrevealed("audit", side, sim.catalog());
          reveal.addAll(abyss.eligibleRevealIds("audit", sim.catalog()));
        }
        assertThat(active)
            .isNotEmpty()
            .allSatisfy(
                d -> {
                  assertThat(d.weight()).isEqualTo(500);
                  assertThat(reveal).contains(d.id());
                });
        var selected = active.getFirst();
        double total =
            sim.pool(initial, WorkbenchCurrency.EXALTED, null).stream()
                .mapToLong(ModifierDefinition::weight)
                .sum();
        var choice =
            sim.exaltChoices(initial, WorkbenchCurrency.EXALTED, Set.of()).stream()
                .filter(c -> c.modifierId().equals(selected.id()))
                .findFirst()
                .orElseThrow();
        assertThat(choice.probability()).isEqualTo(500.0 / total);
        var custom =
            sim.applyCustomExalt(
                initial, WorkbenchCurrency.EXALTED, Set.of(), new Random(9), selected.id());
        assertThat(custom.applied()).isTrue();
        assertThat(custom.events().getFirst().selectionProbability())
            .isEqualTo(choice.probability());
        assertThat(custom.assumptions())
            .anyMatch(a -> a.id().equals("user-approved-augment-weight-v1"));
        var full = fill(sim, initial);
        assertThat(full.explicits()).hasSize(6);
        for (var entry : sim.referenceEssenceTargets().entrySet()) {
          var action = WorkbenchCurrency.valueOf(entry.getKey());
          if (action.replacementModifiers().isEmpty() || entry.getValue().size() != 1) continue;
          var target = sim.catalog().find(entry.getValue().getFirst()).orElseThrow();
          if (full.explicits().stream()
              .anyMatch(
                  m ->
                      !Collections.disjoint(
                          sim.catalog().find(m.modifierId()).orElseThrow().familyIds(),
                          target.familyIds()))) continue;
          var result = sim.apply(full, action, Set.of(), new Random(71));
          assertThat(result.applied())
              .as(id + " / " + catalog.base().id() + " / " + action + ": " + result.reason())
              .isTrue();
          var removed = sim.catalog().find(result.events().getFirst().modifierId()).orElseThrow();
          assertThat(removed.affixType()).isEqualTo(target.affixType());
          assertThat(result.state().installedAugments()).isEqualTo(full.installedAugments());
          assertThat(new ItemStateValidator(sim.catalog()).validate(result.state())).isEmpty();
          applied++;
        }
        pairs++;
      }
    assertThat(pairs).isEqualTo(97);
    assertThat(applied).isGreaterThan(200);
  }

  @Test
  void astridCanRetainOneEssenceAndAddAnotherOnFullGloves() {
    var service = service();
    var catalog =
        service.reviewedCatalogs().stream()
            .filter(c -> BaseRegistry.itemClass(c.base().id()).equals("Gloves"))
            .findFirst()
            .orElseThrow();
    var state = SocketAugments.install(empty(catalog), 0, "Astrids_Creativity");
    var sim = service.simulator(state);
    // Use two independently sourced suffix guarantees, with spare ordinary suffix removal
    // candidates.
    state = sim.apply(state, WorkbenchCurrency.EXALTED, Set.of(), new Random(1)).state();
    var first =
        sim.apply(state, WorkbenchCurrency.PERFECT_ESSENCE_GROUNDING, Set.of(), new Random(2));
    assertThat(first.applied()).as(first.reason()).isTrue();
    state = fill(sim, first.state());
    assertThat(state.explicits()).hasSize(6);
    boolean retained = false;
    for (int seed = 0; seed < 30; seed++) {
      var result =
          sim.apply(state, WorkbenchCurrency.PERFECT_ESSENCE_OPULENCE, Set.of(), new Random(seed));
      assertThat(result.applied()).as(result.reason()).isTrue();
      if (result.state().explicits().stream()
              .filter(m -> AugmentCraftingRules.crafted(m, sim.catalog()))
              .count()
          == 2) {
        retained = true;
        var swapped = SocketAugments.install(result.state(), 0, "Perfect_Desert_Rune");
        assertThat(swapped.explicits()).isEqualTo(result.state().explicits());
        assertThat(
                service
                    .simulator(swapped)
                    .apply(
                        swapped,
                        WorkbenchCurrency.PERFECT_ESSENCE_OPULENCE,
                        Set.of(),
                        new Random(1))
                    .applied())
            .isFalse();
        break;
      }
    }
    assertThat(retained).isTrue();
  }

  @Test
  void sovereignResistancePlainGameTextRecoversRawRollAndProjectsThirtyNine() {
    var service = service();
    var catalog =
        service.reviewedCatalogs().stream()
            .filter(c -> c.base().id().equals(SolarAmulet.BASE_ID))
            .findFirst()
            .orElseThrow();
    String text =
        "Item Class: Amulets\nRarity: Rare\nTest Item\nSolar Amulet\n--------\nItem Level: 100\n--------\n+15 to Spirit (implicit)\n--------\n"
            + "30% increased Explicit Resistance Modifier magnitudes (crafted)\n+39% to Fire Resistance";
    var mapped =
        new SolarTextMapper(catalog)
            .map(new com.poe2craft.shared.item.text.ItemTextService().parseText(text));
    assertThat(mapped.issues()).isEmpty();
    var resistance =
        mapped.state().explicits().stream()
            .filter(m -> m.values().containsKey("base_fire_damage_resistance_%"))
            .findFirst()
            .orElseThrow();
    assertThat(resistance.values().get("base_fire_damage_resistance_%")).isEqualTo(30L);
    assertThat(
            ModifierMagnitudes.value(
                mapped.state(),
                catalog.find(resistance.modifierId()).orElseThrow(),
                resistance,
                "base_fire_damage_resistance_%",
                false))
        .isEqualByComparingTo("39");
  }
}
