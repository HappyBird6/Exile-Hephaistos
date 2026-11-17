package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 신성·암시옵션·보호정책 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code simulator}: 공통 제작 규칙 실행기 ({@code WorkbenchSimulator}).
 *   <li>{@code maximum}: 최대값 ({@code Random}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code item(explicits)}: 아이템 처리에서 {@code SolarAmulet.initial}, {@code root.snapshotId}을(를)
 *       사용한다. 입력: {@code explicits}(명시옵션목록). 반환: {@code ItemState}.
 *   <li>{@code ordinaryDivineRerollsExplicitRangesAndPreservesUnfracturedBaseImplicit()}: 이름에 명시한
 *       시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code aPublishedFixedExplicitRollStaysFixedBesideAVariableExplicitRoll()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code variableImplicitAloneDoesNotMakeOrdinaryDivineAvailable()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다.
 *   <li>{@code fractureProtectionIsSeparateAndNeverMakesImplicitEligible()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다.
 *   <li>{@code explicitBlessedOmenKeepsItsExistingImplicitOnlyContract()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다.
 * </ul>
 */
class DivineImplicitProtectionTest {
  private final ItemCatalog catalog = ItemCatalogLoader.loadDefault();
  private final WorkbenchSimulator simulator =
      new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
  private final Random maximum =
      new Random() {
        @Override
        public long nextLong(long bound) {
          return bound - 1;
        }
      };

  private ItemState item(List<ModifierInstance> explicits) {
    var root = SolarAmulet.initial(catalog, 82, 10);
    return new ItemState(
        root.snapshotId(),
        root.baseItemId(),
        root.itemLevel(),
        ItemState.Rarity.RARE,
        root.implicits(),
        explicits,
        Set.of());
  }

  @Test
  void ordinaryDivineRerollsExplicitRangesAndPreservesUnfracturedBaseImplicit() {
    var before =
        item(
            List.of(
                new ModifierInstance("amulet:prefix:healthy", Map.of("base_maximum_life", 20L))));
    var result = simulator.apply(before, WorkbenchCurrency.DIVINE, Set.of(), maximum);
    assertThat(result.applied()).isTrue();
    assertThat(result.state().implicits()).isEqualTo(before.implicits());
    assertThat(result.state().explicits().getFirst().values())
        .containsEntry("base_maximum_life", 29L);
    assertThat(result.events()).allMatch(e -> e.kind().equals("REROLL_EXPLICIT"));
  }

  @Test
  void aPublishedFixedExplicitRollStaysFixedBesideAVariableExplicitRoll() {
    var fixed =
        new ModifierInstance(
            "amulet:prefix:essence-maximum-quality", Map.of("local_maximum_quality_+", 20L));
    var before =
        item(
            List.of(
                fixed,
                new ModifierInstance("amulet:prefix:healthy", Map.of("base_maximum_life", 20L))));
    var result = simulator.apply(before, WorkbenchCurrency.DIVINE, Set.of(), maximum);
    assertThat(result.applied()).isTrue();
    assertThat(result.state().implicits()).isEqualTo(before.implicits());
    assertThat(result.state().explicits().getFirst()).isEqualTo(fixed);
    assertThat(result.events()).hasSize(1);
    assertThat(result.events().getFirst().modifierId()).isEqualTo("amulet:prefix:healthy");
  }

  @Test
  void variableImplicitAloneDoesNotMakeOrdinaryDivineAvailable() {
    var before = item(List.of());
    var result = simulator.apply(before, WorkbenchCurrency.DIVINE, Set.of(), maximum);
    assertThat(result.applied()).isFalse();
    assertThat(result.state()).isEqualTo(before);
    assertThat(result.events()).isEmpty();
  }

  @Test
  void fractureProtectionIsSeparateAndNeverMakesImplicitEligible() {
    var before =
        item(
            List.of(
                new ModifierInstance(
                    "amulet:prefix:healthy", Map.of("base_maximum_life", 20L), true)));
    var result = simulator.apply(before, WorkbenchCurrency.DIVINE, Set.of(), maximum);
    assertThat(result.applied()).isFalse();
    assertThat(result.state()).isEqualTo(before);
  }

  @Test
  void explicitBlessedOmenKeepsItsExistingImplicitOnlyContract() {
    var before =
        item(
            List.of(
                new ModifierInstance("amulet:prefix:healthy", Map.of("base_maximum_life", 20L))));
    var result =
        simulator.apply(
            before, WorkbenchCurrency.DIVINE, Set.of(WorkbenchOmen.BLESSED.id()), maximum);
    assertThat(result.applied()).isTrue();
    assertThat(result.state().explicits()).isEqualTo(before.explicits());
    assertThat(result.state().implicits()).isNotEqualTo(before.implicits());
    assertThat(result.events()).allMatch(e -> e.kind().equals("REROLL_IMPLICIT"));
    assertThat(result.consumedOmens()).containsExactly(WorkbenchOmen.BLESSED.id());
  }
}
