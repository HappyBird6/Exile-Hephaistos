package com.poe2craft.shared.item;

import java.util.Set;

/**
 * 현재 기폭제 품질 값을 표현한다. 화폐 사용이나 원래 옵션 수치와는 구분한다.
 *
 * <p>기존 계약: Already present quality, never a catalyst use or a substitute for original modifier
 * rolls.
 *
 * <p>필드·상수·record 구성요소:
 *
 * <ul>
 *   <li>{@code type}: 타입 ({@code Type}).
 *   <li>{@code amount}: 양 ({@code int}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code CatalystQuality(type, amount)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code type}(타입), {@code
 *       amount}(양). 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code supportedBase(base)}: 지원·베이스 처리에서 {@code BaseRegistry.catalystQuality}을(를) 사용한다. 입력:
 *       {@code base}(베이스). 반환: {@code boolean}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Type}: {@code CatalystQuality}의 타입 구분값을 정의한다.
 * </ul>
 */
public record CatalystQuality(Type type, int amount) {
  public CatalystQuality {
    java.util.Objects.requireNonNull(type, "quality type");
    if (amount < 0 || amount > 100) throw new IllegalArgumentException("Invalid quality amount");
  }

  /**
   * {@code CatalystQuality}의 타입 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code FLESH}: FLESH 구분값.
   *   <li>{@code NEURAL}: NEURAL 구분값.
   *   <li>{@code CARAPACE}: CARAPACE 구분값.
   *   <li>{@code UUL_NETOL}: NETOL 구분값.
   *   <li>{@code XOPH}: XOPH 구분값.
   *   <li>{@code TUL}: TUL 구분값.
   *   <li>{@code ESH}: ESH 구분값.
   *   <li>{@code CHAYULA}: CHAYULA 구분값.
   *   <li>{@code REAVER}: REAVER 구분값.
   *   <li>{@code SIBILANT}: SIBILANT 구분값.
   *   <li>{@code SKITTERING}: SKITTERING 구분값.
   *   <li>{@code ADAPTIVE}: ADAPTIVE 구분값.
   *   <li>{@code NECROTIC}: NECROTIC 구분값.
   *   <li>{@code tags}: 태그목록 ({@code Set<String>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Type(tags)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code tags}(태그목록).
   *   <li>{@code matches(modifierTags)}: 일치결과 처리에서 {@code modifierTags.stream}을(를) 사용한다. 입력: {@code
   *       modifierTags}(옵션·태그목록). 반환: {@code boolean}.
   * </ul>
   */
  public enum Type {
    FLESH("life"),
    NEURAL("mana"),
    CARAPACE("defences", "armour", "evasion", "energyshield"),
    UUL_NETOL("physical"),
    XOPH("fire"),
    TUL("cold"),
    ESH("lightning"),
    CHAYULA("chaos"),
    REAVER("attack"),
    SIBILANT("caster"),
    SKITTERING("speed"),
    ADAPTIVE("attribute"),
    NECROTIC("minion");

    private final Set<String> tags;

    Type(String... tags) {
      this.tags = Set.of(tags);
    }

    public boolean matches(Set<String> modifierTags) {
      return modifierTags.stream().anyMatch(tags::contains);
    }
  }

  public static boolean supportedBase(String base) {
    return BaseRegistry.catalystQuality(base);
  }
}
