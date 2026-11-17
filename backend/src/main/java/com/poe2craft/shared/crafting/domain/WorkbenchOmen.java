package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.ModifierDefinition.AffixType;
import java.util.Arrays;

/**
 * 검토된 징조 효과·발동 조건·조합 가능성을 JSON 정의와 연결한다.
 *
 * <p>기존 계약: Bounded effects and explicitly reviewed composition rules; other combinations remain
 * blocked.
 *
 * <p>필드·상수·열거값:
 *
 * <ul>
 *   <li>{@code SINISTRAL_ALCHEMY}: 연금 구분값.
 *   <li>{@code DEXTRAL_ALCHEMY}: 연금 구분값.
 *   <li>{@code SINISTRAL_CORONATION}: CORONATION 구분값.
 *   <li>{@code DEXTRAL_CORONATION}: CORONATION 구분값.
 *   <li>{@code GREATER_ANNULMENT}: 소멸 구분값.
 *   <li>{@code SINISTRAL_CRYSTALLISATION}: 결정화 구분값.
 *   <li>{@code DEXTRAL_CRYSTALLISATION}: 결정화 구분값.
 *   <li>{@code GREATER_EXALTATION}: EXALTATION 구분값.
 *   <li>{@code HOMOGENISING_EXALTATION}: EXALTATION 구분값.
 *   <li>{@code HOMOGENISING_CORONATION}: CORONATION 구분값.
 *   <li>{@code SINISTRAL_EXALTATION}: EXALTATION 구분값.
 *   <li>{@code DEXTRAL_EXALTATION}: EXALTATION 구분값.
 *   <li>{@code SINISTRAL_ANNULMENT}: 소멸 구분값.
 *   <li>{@code DEXTRAL_ANNULMENT}: 소멸 구분값.
 *   <li>{@code SINISTRAL_ERASURE}: ERASURE 구분값.
 *   <li>{@code DEXTRAL_ERASURE}: ERASURE 구분값.
 *   <li>{@code WHITTLING}: WHITTLING 구분값.
 *   <li>{@code BLESSED}: BLESSED 구분값.
 *   <li>{@code SANCTIFICATION}: 신성화 구분값.
 *   <li>{@code LIGHT}: LIGHT 구분값.
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code definition()}: 정의 값을 조회·구성한다. 반환: {@code WorkbenchDefinitions.Omen}.
 *   <li>{@code id()}: 식별자 값을 조회·구성한다. 반환: {@code String}.
 *   <li>{@code trigger()}: trigger 값을 조회·구성한다. 반환: {@code WorkbenchCurrency}.
 *   <li>{@code affix()}: 접두접미옵션 값을 조회·구성한다. 반환: {@code AffixType}.
 *   <li>{@code homogenising()}: homogenising 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code supportsTieredCurrency()}: 등급별·화폐을(를) 지원 여부를 판정한다. 반환: {@code boolean}.
 *   <li>{@code sideWhittling(matching)}: 접두접미·Whittling 처리에서 {@code matching.size}, {@code
 *       matching.contains}을(를) 사용한다. 입력: {@code matching}(일치). 반환: {@code boolean}.
 *   <li>{@code sideDoubleRemoval(matching)}: 접두접미·Double·제거 처리에서 {@code matching.size}, {@code
 *       matching.contains}을(를) 사용한다. 입력: {@code matching}(일치). 반환: {@code boolean}.
 *   <li>{@code verifiedDoubleAddition(matching)}: 검증된·Double·추가 처리에서 {@code matching.contains},
 *       {@code matching.size}을(를) 사용한다. 입력: {@code matching}(일치). 반환: {@code boolean}.
 *   <li>{@code fromId(id)}: 식별자을(를) 입력에서 변환한다. 입력: {@code id}(식별자). 반환: {@code WorkbenchOmen}.
 * </ul>
 */
public enum WorkbenchOmen {
  SINISTRAL_ALCHEMY,
  DEXTRAL_ALCHEMY,
  SINISTRAL_CORONATION,
  DEXTRAL_CORONATION,
  GREATER_ANNULMENT,
  SINISTRAL_CRYSTALLISATION,
  DEXTRAL_CRYSTALLISATION,
  GREATER_EXALTATION,
  HOMOGENISING_EXALTATION,
  HOMOGENISING_CORONATION,
  SINISTRAL_EXALTATION,
  DEXTRAL_EXALTATION,
  SINISTRAL_ANNULMENT,
  DEXTRAL_ANNULMENT,
  SINISTRAL_ERASURE,
  DEXTRAL_ERASURE,
  WHITTLING,
  BLESSED,
  SANCTIFICATION,
  LIGHT;

  private WorkbenchDefinitions.Omen definition() {
    return WorkbenchDefinitions.active().omen(this);
  }

  public String id() {
    return definition().id();
  }

  public WorkbenchCurrency trigger() {
    return definition().trigger();
  }

  public AffixType affix() {
    return definition().affix();
  }

  public boolean homogenising() {
    return this == HOMOGENISING_EXALTATION || this == HOMOGENISING_CORONATION;
  }

  /** Reviewed single-effect composition with the currency's existing added-level pool. */
  public boolean supportsTieredCurrency() {
    return definition().supportsTieredCurrency();
  }

  /** User-specified order and its symmetric side: side pool, lowest level, uniform ties. */
  public static boolean sideWhittling(java.util.List<WorkbenchOmen> matching) {
    return matching.size() == 2
        && (matching.contains(SINISTRAL_ERASURE) || matching.contains(DEXTRAL_ERASURE))
        && matching.contains(WHITTLING);
  }

  /** Audited prefix/count example and symmetric suffix rule; not arbitrary combinations. */
  public static boolean sideDoubleRemoval(java.util.List<WorkbenchOmen> matching) {
    return matching.size() == 2
        && (matching.contains(SINISTRAL_ANNULMENT) || matching.contains(DEXTRAL_ANNULMENT))
        && matching.contains(GREATER_ANNULMENT);
  }

  /** Greater may compose with one directional or homogenising addition effect. */
  public static boolean verifiedDoubleAddition(java.util.List<WorkbenchOmen> matching) {
    return matching.contains(GREATER_EXALTATION)
        && (matching.size() == 1
            || (matching.size() == 2
                && (matching.contains(HOMOGENISING_EXALTATION)
                    || matching.contains(SINISTRAL_EXALTATION)
                    || matching.contains(DEXTRAL_EXALTATION))));
  }

  public static WorkbenchOmen fromId(String id) {
    return Arrays.stream(values())
        .filter(o -> o.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unsupported active omen"));
  }
}
