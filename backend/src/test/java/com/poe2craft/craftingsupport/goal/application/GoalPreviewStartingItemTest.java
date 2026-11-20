package com.poe2craft.craftingsupport.goal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.*;
import com.poe2craft.craftingsupport.goal.infrastructure.GoalDefinitionsLoader;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** 태양의 목걸이의 기존 상태와 작업대에서 실제로 달성한 저항을 카드 탐색과 대조한다. */
class GoalPreviewStartingItemTest {
  private static final String COLD = "base_cold_damage_resistance_%";
  private final ItemCatalog catalog =
      EquipmentCurrencyCatalog.enrich(ItemCatalogLoader.loadDefault());
  private final ItemState base = SolarAmulet.initial(catalog);
  private final Context context =
      new Context(base.snapshotId(), base.baseItemId(), base.itemLevel());
  private final WorkbenchSimulator simulator =
      new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
  private final GoalCatalogIndex index =
      new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load());
  private final GoalFilterService service = new GoalFilterService(index);
  private final CraftingOperations workbench = Mockito.mock(CraftingOperations.class);

  /** 테스트 베이스 요청을 실제 작업대에 연결한다. */
  GoalPreviewStartingItemTest() {
    Mockito.when(workbench.simulatorForBase(base.baseItemId())).thenReturn(simulator);
  }

  /** 명시 냉기 저항의 최소와 최대를 같은 수치로 설정한다. */
  private GoalFilter coldGoal(int value, Integer quality) {
    var exact = BigDecimal.valueOf(value);
    var row =
        new Entry(
            "cold",
            GoalCatalogIndex.id("explicit", COLD),
            "percent",
            new Range(exact, exact),
            null,
            false);
    return new GoalFilter(
        1,
        index.version(),
        new General(
            base.baseItemId(), new Range(null, null), List.of(ItemState.Rarity.RARE), quality),
        List.of(new Group("required", Type.AND, false, null, List.of(row))));
  }

  /** 50% 품질로 도달한 68%는 포함하고 새 축성까지 써야 달성하는 80%는 제외한다. */
  @Test
  void searchDoesNotApplySanctificationToReachTheGoal() {
    var cold = new ModifierInstance("amulet:suffix:of-haast", Map.of(COLD, 45L));
    var cap =
        new ModifierInstance(QualityLimitRules.BREACH_ID, Map.of(QualityLimitRules.STAT_ID, 20L))
            .asCrafted();
    var rare =
        new ItemState(
            base.snapshotId(),
            base.baseItemId(),
            82,
            ItemState.Rarity.RARE,
            base.implicits(),
            List.of(cold, cap),
            Set.of());
    var typed =
        simulator.apply(rare, WorkbenchCurrency.CATALYST_TUL, Set.of(), new Random(1)).state();
    var infused =
        simulator
            .apply(typed, WorkbenchCurrency.VAAL_CATALYSING_INFUSER, Set.of(), new Random(1))
            .state();
    var sanctified =
        simulator.apply(
            infused, WorkbenchCurrency.DIVINE, Set.of("Omen_of_Sanctification"), new Random(44));
    assertThat(sanctified.applied()).isTrue();
    assertThat(service.evaluate(infused, coldGoal(68, 40)).status()).isEqualTo(Status.MATCH);
    assertThat(service.evaluate(sanctified.state(), coldGoal(80, 40)).status())
        .isEqualTo(Status.MATCH);
    assertThat(service.preview(context, coldGoal(68, 40), workbench).cards())
        .isNotEmpty()
        .allSatisfy(
            c -> assertThat(c.item().conditions()).doesNotContain(ItemState.Condition.SANCTIFIED));
    assertThat(service.preview(context, coldGoal(80, 40), workbench).cards()).isEmpty();
    var locked =
        service
            .preview(context, coldGoal(80, 40), workbench, null, null, sanctified.state())
            .cards();
    assertThat(locked)
        .singleElement()
        .satisfies(
            card -> {
              assertThat(card.item()).isEqualTo(sanctified.state());
              assertThat(card.tierVariants()).containsExactly(sanctified.state());
              assertThat(card.projectedValues().get(cold.modifierId()).get(COLD))
                  .isEqualByComparingTo("80");
            });
  }

  /** 이미 붙은 T4는 레벨 20에서 보존되며 새 T4나 범위를 벗어난 분열 굴림은 생성되지 않는다. */
  @Test
  void existingFractureCanExceedTheGenerationLevel() {
    var context = new Context(base.snapshotId(), base.baseItemId(), 20);
    var tier = "amulet:suffix:of-the-walrus";
    var fracture = new ModifierInstance(tier, Map.of(COLD, 30L), true);
    assertThat(catalog.find(tier).orElseThrow().requiredItemLevel()).isEqualTo(50);
    var existing = service.preview(context, coldGoal(30, null), workbench, fracture, tier).cards();
    assertThat(existing)
        .anySatisfy(
            card -> {
              assertThat(card.item().explicits()).containsExactly(fracture);
              assertThat(card.optionTiers().get(tier)).containsExactly(tier);
              assertThat(card.sources().get(tier)).isEmpty();
              assertThat(card.occupancy()).isNotEmpty();
            });
    assertThat(service.preview(context, coldGoal(30, null), workbench, null, tier).cards())
        .isEmpty();
    assertThat(
            service
                .preview(
                    context,
                    coldGoal(31, null),
                    workbench,
                    new ModifierInstance(tier, Map.of(COLD, 31L), true),
                    tier)
                .cards())
        .isEmpty();
  }
}
