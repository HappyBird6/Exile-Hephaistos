package com.poe2craft.craftingsupport.testroutes.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.probability.Fraction;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** 시작 아이템에서 희귀 1옵션까지의 승급·소멸 준비와 분열·훼손 점유 기준을 검사한다. */
class OneOptionPreparationTest {
  /** 실제 아이템 카탈로그에서 각 준비를 해당 작업대의 1회 전이로 적용한다. */
  private final RecipeTransitionsTest fixture = new RecipeTransitionsTest();

  private final RecipeTransitions transitions = new RecipeTransitions(fixture.workbench, true);

  /** 지정한 준비가 이번 아이템에 후보로 존재하면 그 전이의 실제 분기들을 반환한다. */
  private List<RecipeTransitions.Outcome> advance(
      ItemState item, WorkbenchCurrency currency, Set<WorkbenchOmen> omens) {
    assertThat(OneOptionPreparation.choices(item, 1))
        .anyMatch(
            c ->
                c.preparation()
                    && c.target() == 1
                    && c.move().action().equals(currency.name())
                    && c.move().omens().equals(omens));
    var law = transitions.successors(item, currency.name(), omens, Set.of(fixture.life), 5000);
    assertThat(law.complete()).isTrue();
    assertThat(
            law.outcomes().stream()
                .map(RecipeTransitions.Outcome::probability)
                .reduce(Fraction.ZERO, Fraction::add))
        .isEqualTo(Fraction.ONE);
    return law.outcomes();
  }

  /** 일반에서 진화·제왕을 거친 바로 그 희귀에 소멸을 써 일반 더미 하나가 남고 베이스와 고정 옵션은 유지된다. */
  @Test
  void normalUpgradesBeforeAnnulmentOnTheSameBase() {
    var start = fixture.item(ItemState.Rarity.NORMAL);
    var magic = advance(start, WorkbenchCurrency.TRANSMUTATION, Set.of()).getFirst().state();
    var rare = advance(magic, WorkbenchCurrency.REGAL, Set.of()).getFirst().state();
    assertThat(magic.rarity()).isEqualTo(ItemState.Rarity.MAGIC);
    assertThat(rare.rarity()).isEqualTo(ItemState.Rarity.RARE);
    assertThat(rare.explicits()).hasSize(2);
    var outcomes = advance(rare, WorkbenchCurrency.ANNULMENT, Set.of());
    assertThat(outcomes)
        .hasSize(2)
        .allSatisfy(
            outcome -> {
              var item = outcome.state();
              assertThat(item.baseItemId()).isEqualTo(start.baseItemId());
              assertThat(item.itemLevel()).isEqualTo(start.itemLevel());
              assertThat(item.implicits()).isEqualTo(start.implicits());
              assertThat(OneOptionPreparation.ready(item)).isTrue();
              assertThat(OneOptionPreparation.choices(item, 1)).isEmpty();
            });
  }

  /** 마법 아이템의 보유 L 접두를 유지하는 접미 제거 징조의 모든 분기는 같은 희귀 아이템에서 다음 소멸로 이어진다. */
  @Test
  void magicWithTwoOptionsRequiresTwoAnnulsAndCanPreserveTheLowPrefix() {
    var start = fixture.item(ItemState.Rarity.MAGIC, fixture.life, fixture.strength);
    var rare =
        advance(start, WorkbenchCurrency.REGAL, Set.of()).stream()
            .map(RecipeTransitions.Outcome::state)
            .filter(
                item ->
                    item.explicits().stream()
                            .filter(
                                mod ->
                                    fixture.catalog.find(mod.modifierId()).orElseThrow().affixType()
                                        == ModifierDefinition.AffixType.SUFFIX)
                            .count()
                        == 2)
            .findFirst()
            .orElseThrow();
    assertThat(rare.explicits()).hasSize(3);
    var suffixOnly = Set.of(WorkbenchOmen.DEXTRAL_ANNULMENT);
    for (var first : advance(rare, WorkbenchCurrency.ANNULMENT, suffixOnly)) {
      assertThat(first.state().explicits()).hasSize(2);
      assertThat(OneOptionPreparation.ready(first.state())).isFalse();
      var last = advance(first.state(), WorkbenchCurrency.ANNULMENT, suffixOnly).getFirst().state();
      assertThat(last.explicits()).containsExactly(start.explicits().getFirst());
      assertThat(OneOptionPreparation.ready(last)).isTrue();
    }
  }

  /** 공개된 훼손 한 칸은 한 번만 세고, 숨은 훼손은 이미 붙은 일반 명시와 별도로 센다. */
  @Test
  void countsRealOccupiedSlotsIncludingDesecratedOnes() {
    var empty = fixture.item(ItemState.Rarity.RARE);
    var plain = fixture.item(ItemState.Rarity.RARE, fixture.life);
    var hidden = new ItemState.DesecratedSlot("slot", ModifierDefinition.AffixType.SUFFIX, null);
    var revealed =
        new ItemState.DesecratedSlot("slot", ModifierDefinition.AffixType.PREFIX, fixture.life);
    assertThat(OneOptionPreparation.ready(plain.withDesecrated(List.of(revealed)))).isTrue();
    assertThat(OneOptionPreparation.ready(plain.withDesecrated(List.of(hidden)))).isFalse();
    assertThat(OneOptionPreparation.choices(plain.withDesecrated(List.of(hidden)), 1)).isNotEmpty();
    assertThat(OneOptionPreparation.ready(empty.withDesecrated(List.of(hidden)))).isFalse();
  }

  /** 분열은 고정하고 그 밖의 명시만 세어, 0개면 엑잘 준비·1개면 반복·2개면 소멸 준비가 된다. */
  @Test
  void fractureStaysOnTheSameBaseThroughExaltAndAnnulPreparation() {
    var plain = fixture.item(ItemState.Rarity.RARE, fixture.life);
    var life = plain.explicits().getFirst();
    var fracture = new ModifierInstance(life.modifierId(), life.values(), true);
    var onlyFracture =
        new ItemState(
            plain.snapshotId(),
            plain.baseItemId(),
            plain.itemLevel(),
            plain.rarity(),
            plain.implicits(),
            List.of(fracture),
            plain.conditions());
    assertThat(OneOptionPreparation.ready(onlyFracture)).isFalse();
    var exalted = advance(onlyFracture, WorkbenchCurrency.EXALTED, Set.of()).getFirst().state();
    assertThat(exalted.explicits()).contains(fracture);
    assertThat(OneOptionPreparation.ready(exalted)).isTrue();
    assertThat(OneOptionPreparation.choices(exalted, 1)).isEmpty();
    var twoMore = fixture.item(ItemState.Rarity.RARE, fixture.strength, "amulet:prefix:adept-s");
    var beforeAnnul =
        new ItemState(
            plain.snapshotId(),
            plain.baseItemId(),
            plain.itemLevel(),
            plain.rarity(),
            plain.implicits(),
            List.of(fracture, twoMore.explicits().getFirst(), twoMore.explicits().getLast()),
            plain.conditions());
    assertThat(OneOptionPreparation.ready(beforeAnnul)).isFalse();
    assertThat(advance(beforeAnnul, WorkbenchCurrency.ANNULMENT, Set.of()))
        .allSatisfy(
            o -> {
              assertThat(o.state().explicits()).contains(fracture);
              assertThat(OneOptionPreparation.ready(o.state())).isTrue();
              assertThat(o.removedModifierIds()).doesNotContain(fixture.life);
            });
  }

  /** 제왕의 피격과 미적중 모두 실제 가중치의 분기다. 목표 L을 먼저 얻었다면 그 아이템이 성공이다. */
  @Test
  void aLuckyRegalCanAcquireTheLowTargetDuringPreparation() {
    var start = fixture.item(ItemState.Rarity.MAGIC, fixture.strength);
    var outcomes = advance(start, WorkbenchCurrency.REGAL, Set.of());
    var masses =
        outcomes.stream()
            .collect(
                java.util.stream.Collectors.partitioningBy(
                    o ->
                        o.state().explicits().stream()
                            .anyMatch(mod -> mod.modifierId().equals(fixture.life)),
                    java.util.stream.Collectors.reducing(
                        Fraction.ZERO, RecipeTransitions.Outcome::probability, Fraction::add)));
    assertThat(masses.get(true)).isNotEqualTo(Fraction.ZERO);
    assertThat(masses.get(false)).isNotEqualTo(Fraction.ZERO);
    assertThat(masses.get(true).add(masses.get(false))).isEqualTo(Fraction.ONE);
  }
}
