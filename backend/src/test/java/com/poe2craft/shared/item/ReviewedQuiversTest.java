package com.poe2craft.shared.item;

import static org.assertj.core.api.Assertions.*;

import com.poe2craft.shared.crafting.domain.AugmentSocketRules;
import com.poe2craft.shared.crafting.domain.QualityLimitRules;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import java.util.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Quivers·검증의 출처 검토된 베이스 목록과 지원 여부를 제공한다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code independentClassCompletePoolAndRestrictions(key)}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과
 *       비교한다. 입력: {@code key}(키).
 * </ul>
 */
class ReviewedQuiversTest {
  @ParameterizedTest
  @ValueSource(
      strings = {
        "visceral-quiver",
        "volant-quiver",
        "penetrating-quiver",
        "primed-quiver",
        "serrated-quiver",
        "toxic-quiver",
        "blunt-quiver",
        "two-point-quiver",
        "sacral-quiver",
        "fire-quiver",
        "broadhead-quiver"
      })
  void independentClassCompletePoolAndRestrictions(String key) {
    var catalog = ItemCatalogLoader.loadTopBase(key);
    assertThat(catalog.base().id()).isEqualTo(ReviewedQuivers.BASES.get(key)).contains("/Quivers/");
    assertThat(catalog.modifiers().values().stream().filter(d -> d.weight() > 0)).hasSize(100);
    assertThat(
            catalog.modifiers().values().stream()
                .filter(d -> d.layer() == ModifierDefinition.Layer.IMPLICIT))
        .hasSize(1);
    assertThat(catalog.modifiers().values())
        .noneMatch(d -> d.stats().stream().anyMatch(s -> s.id().equals("local_attack_speed_+%")));
    assertThat(catalog.modifiers().values())
        .anyMatch(
            d ->
                d.stats().stream()
                    .anyMatch(
                        s -> s.id().equals("attack_speed_+%") && s.min() == 5 && s.max() == 7));
    var implicit = catalog.find(catalog.base().implicitModifierId()).orElseThrow();
    var values = new HashMap<String, Long>();
    implicit.stats().forEach(s -> values.put(s.id(), s.max()));
    var state =
        new ItemState(
            catalog.metadata().snapshotId(),
            catalog.base().id(),
            1,
            ItemState.Rarity.NORMAL,
            List.of(new ModifierInstance(implicit.id(), values)),
            List.of(),
            Set.of());
    assertThat(new ItemStateValidator(catalog).validate(state)).isEmpty();
    assertThat(QualityLimitRules.describe(state, catalog)).isNull();
    assertThat(CatalystQuality.supportedBase(state.baseItemId())).isFalse();
    assertThat(AugmentSocketRules.refusal(state)).isNotEmpty();
    if (key.equals("penetrating-quiver"))
      assertThat(values).containsEntry("base_chance_to_pierce_%", 100L);
  }
}
