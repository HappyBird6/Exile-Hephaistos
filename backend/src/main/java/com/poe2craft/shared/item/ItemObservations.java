package com.poe2craft.shared.item;

import java.util.List;

/**
 * 복사 텍스트에서 관측한 속성을 보존한다. 게임 한도나 장착 증강물을 추측하지 않는다.
 *
 * <p>기존 계약: Clipboard observations, not inferred game limits or resolved installed-Augment
 * identities.
 *
 * <p>필드·상수·record 구성요소:
 *
 * <ul>
 *   <li>{@code socketCount}: 장착홈·개수 ({@code Integer}).
 *   <li>{@code augmentEffects}: 증강물·효과목록 ({@code List<String>}).
 *   <li>{@code grantedSkills}: 부여된·Skills ({@code List<GrantedSkill>}).
 *   <li>{@code augmentEffectScale}: 증강물·효과·단위배율 ({@code Double}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ItemObservations(socketCount, augmentEffects, grantedSkills)}: 기본 정책을 보충해 공통 생성자로
 *       위임한다. 입력: {@code socketCount}(장착홈·개수), {@code augmentEffects}(증강물·효과목록), {@code
 *       grantedSkills}(부여된·Skills).
 *   <li>{@code ItemObservations(socketCount, augmentEffects, grantedSkills, augmentEffectScale)}:
 *       전달받은 값으로 구성원을 초기화한다. 입력: {@code socketCount}(장착홈·개수), {@code augmentEffects}(증강물·효과목록),
 *       {@code grantedSkills}(부여된·Skills), {@code augmentEffectScale}(증강물·효과·단위배율). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code needsAugmentResolution()}: needs·증강물·Resolution 값을 조회·구성한다. 반환: {@code boolean}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code GrantedSkill}: {@code ItemObservations}에서 사용하는 부여된·Skill의 불변 값이다.
 * </ul>
 */
public record ItemObservations(
    Integer socketCount,
    List<String> augmentEffects,
    List<GrantedSkill> grantedSkills,
    Double augmentEffectScale) {
  public ItemObservations(
      Integer socketCount, List<String> augmentEffects, List<GrantedSkill> grantedSkills) {
    this(socketCount, augmentEffects, grantedSkills, null);
  }

  /**
   * {@code ItemObservations}에서 사용하는 부여된·Skill의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code name}: 표시 또는 조회에 사용하는 이름 ({@code String}).
   *   <li>{@code level}: 레벨 ({@code Integer}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code GrantedSkill(name, level)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code name}(이름), {@code
   *       level}(레벨). 사용하는 예외: {@code IllegalArgumentException}.
   * </ul>
   */
  public record GrantedSkill(String name, Integer level) {
    public GrantedSkill {
      if (name == null || name.isBlank() || name.length() > 256 || (level != null && level < 1))
        throw new IllegalArgumentException("Invalid observed granted skill");
    }
  }

  public ItemObservations {
    if (augmentEffectScale != null
        && (!Double.isFinite(augmentEffectScale)
            || augmentEffectScale < 0.01
            || augmentEffectScale > 1000))
      throw new IllegalArgumentException("Invalid observed socket-effect scale");
    if (socketCount != null && socketCount < 0)
      throw new IllegalArgumentException("Invalid observed socket count");
    augmentEffects = List.copyOf(augmentEffects);
    grantedSkills = List.copyOf(grantedSkills);
    if (augmentEffects.size() > 128
        || grantedSkills.size() > 16
        || augmentEffects.stream().anyMatch(s -> s.isBlank() || s.length() > 4096)
        || grantedSkills.stream().map(GrantedSkill::name).distinct().count()
            != grantedSkills.size())
      throw new IllegalArgumentException("Invalid clipboard observations");
  }

  public boolean needsAugmentResolution() {
    return !augmentEffects.isEmpty();
  }
}
