package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.BaseRegistry;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierDefinition;

/**
 * 출처로 검증된 품질 상한을 계산한다. 실제 품질과 표시 계산은 별도다.
 *
 * <p>기존 계약: Source-verified maximum only; applied quality and derived display use separate item
 * models.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "quality-limit-v1"} ({@code String}).
 *   <li>{@code BREACH_ID}: 식별자; 초기값 {@code "amulet:prefix:essence-maximum-quality"} ({@code
 *       String}).
 *   <li>{@code STAT_ID}: 식별자; 초기값 {@code "local_maximum_quality_+"} ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code QualityLimitRules()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code describe(state, catalog)}: describe 처리에서 {@code BaseRegistry.qualityLimit}, {@code
 *       state.baseItemId}을(를) 사용한다. 입력: {@code state}(상태), {@code catalog}(카탈로그). 반환: {@code
 *       Limit}. 사용하는 예외: {@code IllegalArgumentException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Limit}: {@code QualityLimitRules}에서 사용하는 한도의 불변 값이다.
 * </ul>
 */
public final class QualityLimitRules {
  public static final String VERSION = "quality-limit-v1";
  public static final String BREACH_ID = "amulet:prefix:essence-maximum-quality";
  public static final String STAT_ID = "local_maximum_quality_+";

  private QualityLimitRules() {}

  public static Limit describe(ItemState state, ItemCatalog catalog) {
    if (state == null || !new ItemStateValidator(catalog).validate(state).isEmpty())
      throw new IllegalArgumentException("Unsupported state for quality limit");
    if (!BaseRegistry.qualityLimit(state.baseItemId())) return null;
    int maximum =
        Math.max(20, com.poe2craft.shared.item.AugmentCraftingRules.maximumQuality(state));
    for (var instance : state.explicits()) {
      if (!instance.values().containsKey(STAT_ID)) continue;
      var definition = catalog.find(instance.modifierId()).orElseThrow();
      if (!BaseRegistry.maximumQualityBreach(state.baseItemId())
          || !instance.modifierId().equals(BREACH_ID)
          || !definition.familyIds().equals(java.util.Set.of("LocalMaximumQuality"))
          || definition.affixType() != ModifierDefinition.AffixType.PREFIX
          || definition.stats().size() != 1
          || definition.stats().getFirst().min() != 20
          || definition.stats().getFirst().max() != 20
          || instance.values().get(STAT_ID) != 20L)
        throw new IllegalArgumentException("Unreviewed maximum-quality modifier");
      maximum = 40;
    }
    return new Limit(VERSION, maximum);
  }

  /**
   * {@code QualityLimitRules}에서 사용하는 한도의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code ruleVersion}: 규칙·버전 ({@code String}).
   *   <li>{@code maximumQuality}: 최대값·품질 ({@code int}).
   * </ul>
   */
  public record Limit(String ruleVersion, int maximumQuality) {}
}
