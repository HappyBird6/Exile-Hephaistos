package com.poe2craft.shared.item;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 확률·경로 이력과 분리된 불변 제작 아이템 상태다.
 *
 * <p>기존 계약: Immutable crafting state. Probabilities, actions and path history belong outside this
 * value.
 *
 * <p>필드·상수·record 구성요소:
 *
 * <ul>
 *   <li>{@code snapshotId}: 카탈로그 스냅샷 식별자 ({@code String}).
 *   <li>{@code baseItemId}: 원본 베이스 식별자 ({@code String}).
 *   <li>{@code itemLevel}: 옵션 생성 제한에 사용하는 아이템 레벨 ({@code int}).
 *   <li>{@code rarity}: 현재 아이템 희귀도 ({@code Rarity}).
 *   <li>{@code implicits}: 아이템의 암시 옵션 목록 ({@code List<ModifierInstance>}).
 *   <li>{@code explicits}: 아이템의 명시 옵션 목록 ({@code List<ModifierInstance>}).
 *   <li>{@code conditions}: 타락 등 아이템 상태 조건 ({@code Set<Condition>}).
 *   <li>{@code augmentSockets}: 증강물·장착홈수 ({@code Integer}).
 *   <li>{@code catalystQuality}: 기폭제·품질 ({@code CatalystQuality}).
 *   <li>{@code desecrated}: 훼손 ({@code List<DesecratedSlot>}).
 *   <li>{@code equipmentQuality}: 장비·품질 ({@code Integer}).
 *   <li>{@code observations}: 관측값목록 ({@code ItemObservations}).
 *   <li>{@code sanctification}: 신성화 ({@code Sanctification}).
 *   <li>{@code installedAugments}: 장착된·증강물목록 ({@code List<InstalledAugment>}).
 *   <li>{@code qualityInfused}: 품질·주입된 ({@code boolean}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ItemState(snapshotId, baseItemId, itemLevel, rarity, implicits, explicits,
 *       conditions, augmentSockets, catalystQuality, desecrated, equipmentQuality, observations,
 *       sanctification, installedAugments)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
 *       snapshotId}(스냅샷·식별자), {@code baseItemId}(베이스·아이템·식별자), {@code itemLevel}(아이템·레벨), {@code
 *       rarity}(희귀도), {@code implicits}(암시옵션목록), {@code explicits}(명시옵션목록), {@code
 *       conditions}(상태조건), {@code augmentSockets}(증강물·장착홈수), {@code catalystQuality}(기폭제·품질),
 *       {@code desecrated}(훼손), {@code equipmentQuality}(장비·품질), {@code observations}(관측값목록),
 *       {@code sanctification}(신성화), {@code installedAugments}(장착된·증강물목록).
 *   <li>{@code withQualityInfused(value)}: 품질·주입된을(를) 반영한 값을 구한다. 입력: {@code value}(값). 반환: {@code
 *       ItemState}.
 *   <li>{@code ItemState(snapshotId, baseItemId, itemLevel, rarity, implicits, explicits,
 *       conditions, augmentSockets, catalystQuality, desecrated, equipmentQuality, observations,
 *       sanctification)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code snapshotId}(스냅샷·식별자), {@code
 *       baseItemId}(베이스·아이템·식별자), {@code itemLevel}(아이템·레벨), {@code rarity}(희귀도), {@code
 *       implicits}(암시옵션목록), {@code explicits}(명시옵션목록), {@code conditions}(상태조건), {@code
 *       augmentSockets}(증강물·장착홈수), {@code catalystQuality}(기폭제·품질), {@code desecrated}(훼손), {@code
 *       equipmentQuality}(장비·품질), {@code observations}(관측값목록), {@code sanctification}(신성화).
 *   <li>{@code ItemState(snapshotId, baseItemId, itemLevel, rarity, implicits, explicits,
 *       conditions, augmentSockets, catalystQuality, desecrated, equipmentQuality, observations)}:
 *       기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code snapshotId}(스냅샷·식별자), {@code baseItemId}(베이스·아이템·식별자),
 *       {@code itemLevel}(아이템·레벨), {@code rarity}(희귀도), {@code implicits}(암시옵션목록), {@code
 *       explicits}(명시옵션목록), {@code conditions}(상태조건), {@code augmentSockets}(증강물·장착홈수), {@code
 *       catalystQuality}(기폭제·품질), {@code desecrated}(훼손), {@code equipmentQuality}(장비·품질), {@code
 *       observations}(관측값목록).
 *   <li>{@code withSanctification(value)}: 신성화을(를) 반영한 값을 구한다. 입력: {@code value}(값). 반환: {@code
 *       ItemState}.
 *   <li>{@code ItemState(snapshotId, baseItemId, itemLevel, rarity, implicits, explicits,
 *       conditions, augmentSockets, catalystQuality, desecrated, equipmentQuality)}: 기본 정책을 보충해 공통
 *       생성자로 위임한다. 입력: {@code snapshotId}(스냅샷·식별자), {@code baseItemId}(베이스·아이템·식별자), {@code
 *       itemLevel}(아이템·레벨), {@code rarity}(희귀도), {@code implicits}(암시옵션목록), {@code
 *       explicits}(명시옵션목록), {@code conditions}(상태조건), {@code augmentSockets}(증강물·장착홈수), {@code
 *       catalystQuality}(기폭제·품질), {@code desecrated}(훼손), {@code equipmentQuality}(장비·품질).
 *   <li>{@code withConditions(flags)}: 상태조건을(를) 반영한 값을 구한다. 입력: {@code flags}(상태플래그). 반환: {@code
 *       ItemState}.
 *   <li>{@code withObservations(value)}: 관측값목록을(를) 반영한 값을 구한다. 입력: {@code value}(값). 반환: {@code
 *       ItemState}.
 *   <li>{@code ItemState(snapshotId, baseItemId, itemLevel, rarity, implicits, explicits,
 *       conditions, augmentSockets, catalystQuality, desecrated)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력:
 *       {@code snapshotId}(스냅샷·식별자), {@code baseItemId}(베이스·아이템·식별자), {@code itemLevel}(아이템·레벨),
 *       {@code rarity}(희귀도), {@code implicits}(암시옵션목록), {@code explicits}(명시옵션목록), {@code
 *       conditions}(상태조건), {@code augmentSockets}(증강물·장착홈수), {@code catalystQuality}(기폭제·품질),
 *       {@code desecrated}(훼손).
 *   <li>{@code withEquipmentQuality(quality)}: 장비·품질을(를) 반영한 값을 구한다. 입력: {@code quality}(품질). 반환:
 *       {@code ItemState}.
 *   <li>{@code ItemState(snapshotId, baseItemId, itemLevel, rarity, implicits, explicits,
 *       conditions, augmentSockets, catalystQuality)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
 *       snapshotId}(스냅샷·식별자), {@code baseItemId}(베이스·아이템·식별자), {@code itemLevel}(아이템·레벨), {@code
 *       rarity}(희귀도), {@code implicits}(암시옵션목록), {@code explicits}(명시옵션목록), {@code
 *       conditions}(상태조건), {@code augmentSockets}(증강물·장착홈수), {@code catalystQuality}(기폭제·품질).
 *   <li>{@code withDesecrated(slots)}: 훼손을(를) 반영한 값을 구한다. 입력: {@code slots}(슬롯목록). 반환: {@code
 *       ItemState}.
 *   <li>{@code explicitSlotCount()}: 명시옵션·슬롯·개수 값을 조회·구성한다. 반환: {@code int}.
 *   <li>{@code ItemState(snapshotId, baseItemId, itemLevel, rarity, implicits, explicits,
 *       conditions, augmentSockets)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code snapshotId}(스냅샷·식별자),
 *       {@code baseItemId}(베이스·아이템·식별자), {@code itemLevel}(아이템·레벨), {@code rarity}(희귀도), {@code
 *       implicits}(암시옵션목록), {@code explicits}(명시옵션목록), {@code conditions}(상태조건), {@code
 *       augmentSockets}(증강물·장착홈수).
 *   <li>{@code ItemState(snapshotId, baseItemId, itemLevel, rarity, implicits, explicits,
 *       conditions)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code snapshotId}(스냅샷·식별자), {@code
 *       baseItemId}(베이스·아이템·식별자), {@code itemLevel}(아이템·레벨), {@code rarity}(희귀도), {@code
 *       implicits}(암시옵션목록), {@code explicits}(명시옵션목록), {@code conditions}(상태조건).
 *   <li>{@code hashCode()}: 동등성 비교와 일치하는 해시를 반환한다. 반환: {@code int}.
 *   <li>{@code withAugments(value)}: 증강물목록을(를) 반영한 값을 구한다. 입력: {@code value}(값). 반환: {@code
 *       ItemState}.
 *   <li>{@code ItemState(snapshotId, baseItemId, itemLevel, rarity, implicits, explicits,
 *       conditions, augmentSockets, catalystQuality, desecrated, equipmentQuality, observations,
 *       sanctification, installedAugments, qualityInfused)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       snapshotId}(스냅샷·식별자), {@code baseItemId}(베이스·아이템·식별자), {@code itemLevel}(아이템·레벨), {@code
 *       rarity}(희귀도), {@code implicits}(암시옵션목록), {@code explicits}(명시옵션목록), {@code
 *       conditions}(상태조건), {@code augmentSockets}(증강물·장착홈수), {@code catalystQuality}(기폭제·품질),
 *       {@code desecrated}(훼손), {@code equipmentQuality}(장비·품질), {@code observations}(관측값목록),
 *       {@code sanctification}(신성화), {@code installedAugments}(장착된·증강물목록), {@code
 *       qualityInfused}(품질·주입된). 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code canonical(modifiers)}: 정규형 처리에서 {@code modifiers.stream}, {@code
 *       Comparator.comparing}을(를) 사용한다. 입력: {@code modifiers}(옵션목록). 반환: {@code
 *       List<ModifierInstance>}.
 *   <li>{@code observedSanctified()}: 관측한·Sanctified 값을 조회·구성한다. 반환: {@code boolean}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Sanctification}: {@code ItemState}에서 사용하는 신성화의 불변 값이다.
 *   <li>{@code DesecratedSlot}: {@code ItemState}에서 사용하는 훼손·슬롯의 불변 값이다.
 *   <li>{@code InstalledAugment}: {@code ItemState}에서 사용하는 장착된·증강물의 불변 값이다.
 *   <li>{@code Rarity}: {@code ItemState}의 희귀도 구분값을 정의한다.
 *   <li>{@code Condition}: {@code ItemState}의 조건 구분값을 정의한다.
 * </ul>
 */
public record ItemState(
    String snapshotId,
    String baseItemId,
    int itemLevel,
    Rarity rarity,
    List<ModifierInstance> implicits,
    List<ModifierInstance> explicits,
    Set<Condition> conditions,
    Integer augmentSockets,
    CatalystQuality catalystQuality,
    List<DesecratedSlot> desecrated,
    Integer equipmentQuality,
    ItemObservations observations,
    Sanctification sanctification,
    List<InstalledAugment> installedAugments,
    boolean qualityInfused) {
  public ItemState(
      String snapshotId,
      String baseItemId,
      int itemLevel,
      Rarity rarity,
      List<ModifierInstance> implicits,
      List<ModifierInstance> explicits,
      Set<Condition> conditions,
      Integer augmentSockets,
      CatalystQuality catalystQuality,
      List<DesecratedSlot> desecrated,
      Integer equipmentQuality,
      ItemObservations observations,
      Sanctification sanctification,
      List<InstalledAugment> installedAugments) {
    this(
        snapshotId,
        baseItemId,
        itemLevel,
        rarity,
        implicits,
        explicits,
        conditions,
        augmentSockets,
        catalystQuality,
        desecrated,
        equipmentQuality,
        observations,
        sanctification,
        installedAugments,
        false);
  }

  public ItemState withQualityInfused(boolean value) {
    return new ItemState(
        snapshotId,
        baseItemId,
        itemLevel,
        rarity,
        implicits,
        explicits,
        conditions,
        augmentSockets,
        catalystQuality,
        desecrated,
        equipmentQuality,
        observations,
        sanctification,
        installedAugments,
        value);
  }

  public ItemState(
      String snapshotId,
      String baseItemId,
      int itemLevel,
      Rarity rarity,
      List<ModifierInstance> implicits,
      List<ModifierInstance> explicits,
      Set<Condition> conditions,
      Integer augmentSockets,
      CatalystQuality catalystQuality,
      List<DesecratedSlot> desecrated,
      Integer equipmentQuality,
      ItemObservations observations,
      Sanctification sanctification) {
    this(
        snapshotId,
        baseItemId,
        itemLevel,
        rarity,
        implicits,
        explicits,
        conditions,
        augmentSockets,
        catalystQuality,
        desecrated,
        equipmentQuality,
        observations,
        sanctification,
        List.of());
  }

  public ItemState(
      String snapshotId,
      String baseItemId,
      int itemLevel,
      Rarity rarity,
      List<ModifierInstance> implicits,
      List<ModifierInstance> explicits,
      Set<Condition> conditions,
      Integer augmentSockets,
      CatalystQuality catalystQuality,
      List<DesecratedSlot> desecrated,
      Integer equipmentQuality,
      ItemObservations observations) {
    this(
        snapshotId,
        baseItemId,
        itemLevel,
        rarity,
        implicits,
        explicits,
        conditions,
        augmentSockets,
        catalystQuality,
        desecrated,
        equipmentQuality,
        observations,
        null);
  }

  /**
   * {@code ItemState}에서 사용하는 신성화의 불변 값이다.
   *
   * <p>기존 계약: Original integer rolls remain untouched; these exact percentages are display
   * projections.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code policy}: 정책 ({@code String}).
   *   <li>{@code implicits}: 아이템의 암시 옵션 목록 ({@code java.util.Map<String, java.util.Map<String,
   *       Integer>>}).
   *   <li>{@code explicits}: 아이템의 명시 옵션 목록 ({@code java.util.Map<String, java.util.Map<String,
   *       Integer>>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Sanctification(policy, implicits, explicits)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       policy}(정책), {@code implicits}(암시옵션목록), {@code explicits}(명시옵션목록). 사용하는 예외: {@code
   *       IllegalArgumentException}.
   *   <li>{@code immutableFactors(factors)}: 불변·배율목록 처리에서 {@code factors.forEach}, {@code
   *       id.isBlank}을(를) 사용한다. 입력: {@code factors}(배율목록). 반환: {@code java.util.Map<String,
   *       java.util.Map<String, Integer>>}. 사용하는 예외: {@code IllegalArgumentException}.
   * </ul>
   */
  public record Sanctification(
      String policy,
      java.util.Map<String, java.util.Map<String, Integer>> implicits,
      java.util.Map<String, java.util.Map<String, Integer>> explicits) {
    public Sanctification {
      if (!QualitySanctification.recognizedPolicy(policy))
        throw new IllegalArgumentException("Unknown sanctification policy");
      implicits = immutableFactors(implicits);
      explicits = immutableFactors(explicits);
    }

    private static java.util.Map<String, java.util.Map<String, Integer>> immutableFactors(
        java.util.Map<String, java.util.Map<String, Integer>> factors) {
      var copy = new java.util.TreeMap<String, java.util.Map<String, Integer>>();
      factors.forEach(
          (id, values) -> {
            if (id == null
                || id.isBlank()
                || values.isEmpty()
                || values.entrySet().stream()
                    .anyMatch(
                        e ->
                            e.getKey() == null
                                || e.getKey().isBlank()
                                || e.getValue() == null
                                || e.getValue() < 78
                                || e.getValue() > 122))
              throw new IllegalArgumentException("Invalid sanctification factors");
            copy.put(id, java.util.Map.copyOf(values));
          });
      return java.util.Collections.unmodifiableMap(copy);
    }
  }

  public ItemState withSanctification(Sanctification value) {
    var flags = new java.util.HashSet<>(conditions);
    if (value != null) flags.add(Condition.SANCTIFIED);
    return new ItemState(
        snapshotId,
        baseItemId,
        itemLevel,
        rarity,
        implicits,
        explicits,
        flags,
        augmentSockets,
        catalystQuality,
        desecrated,
        equipmentQuality,
        observations,
        value,
        installedAugments,
        qualityInfused);
  }

  public ItemState(
      String snapshotId,
      String baseItemId,
      int itemLevel,
      Rarity rarity,
      List<ModifierInstance> implicits,
      List<ModifierInstance> explicits,
      Set<Condition> conditions,
      Integer augmentSockets,
      CatalystQuality catalystQuality,
      List<DesecratedSlot> desecrated,
      Integer equipmentQuality) {
    this(
        snapshotId,
        baseItemId,
        itemLevel,
        rarity,
        implicits,
        explicits,
        conditions,
        augmentSockets,
        catalystQuality,
        desecrated,
        equipmentQuality,
        null);
  }

  /** Immutable flag projection; every roll, slot and observation remains intact. */
  public ItemState withConditions(Set<Condition> flags) {
    return new ItemState(
        snapshotId,
        baseItemId,
        itemLevel,
        rarity,
        implicits,
        explicits,
        flags,
        augmentSockets,
        catalystQuality,
        desecrated,
        equipmentQuality,
        observations,
        sanctification,
        installedAugments,
        qualityInfused);
  }

  public ItemState withObservations(ItemObservations value) {
    return new ItemState(
        snapshotId,
        baseItemId,
        itemLevel,
        rarity,
        implicits,
        explicits,
        conditions,
        augmentSockets,
        catalystQuality,
        desecrated,
        equipmentQuality,
        value,
        sanctification,
        installedAugments,
        qualityInfused);
  }

  public ItemState(
      String snapshotId,
      String baseItemId,
      int itemLevel,
      Rarity rarity,
      List<ModifierInstance> implicits,
      List<ModifierInstance> explicits,
      Set<Condition> conditions,
      Integer augmentSockets,
      CatalystQuality catalystQuality,
      List<DesecratedSlot> desecrated) {
    this(
        snapshotId,
        baseItemId,
        itemLevel,
        rarity,
        implicits,
        explicits,
        conditions,
        augmentSockets,
        catalystQuality,
        desecrated,
        null);
  }

  public ItemState withEquipmentQuality(Integer quality) {
    return new ItemState(
        snapshotId,
        baseItemId,
        itemLevel,
        rarity,
        implicits,
        explicits,
        conditions,
        augmentSockets,
        catalystQuality,
        desecrated,
        quality,
        observations,
        sanctification,
        installedAugments,
        qualityInfused);
  }

  /**
   * {@code ItemState}에서 사용하는 훼손·슬롯의 불변 값이다.
   *
   * <p>기존 계약: Slot identity survives observation; a null modifier ID is an unrevealed explicit
   * slot.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code side}: 접두접미 ({@code ModifierDefinition.AffixType}).
   *   <li>{@code modifierId}: 실제 부여된 옵션 정의 식별자 ({@code String}).
   *   <li>{@code minimumModifierLevel}: 최소값·옵션·레벨 ({@code Integer}).
   *   <li>{@code revealPool}: 공개·후보풀 ({@code String}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code DesecratedSlot(id, side, modifierId, minimumModifierLevel)}: 기본 정책을 보충해 공통 생성자로
   *       위임한다. 입력: {@code id}(식별자), {@code side}(접두접미), {@code modifierId}(옵션·식별자), {@code
   *       minimumModifierLevel}(최소값·옵션·레벨).
   *   <li>{@code DesecratedSlot(id, side, modifierId)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
   *       id}(식별자), {@code side}(접두접미), {@code modifierId}(옵션·식별자).
   *   <li>{@code DesecratedSlot(id, side, modifierId, minimumModifierLevel, revealPool)}: 전달받은 값으로
   *       구성원을 초기화한다. 입력: {@code id}(식별자), {@code side}(접두접미), {@code modifierId}(옵션·식별자), {@code
   *       minimumModifierLevel}(최소값·옵션·레벨), {@code revealPool}(공개·후보풀). 사용하는 예외: {@code
   *       IllegalArgumentException}.
   * </ul>
   */
  public record DesecratedSlot(
      String id,
      ModifierDefinition.AffixType side,
      String modifierId,
      Integer minimumModifierLevel,
      String revealPool) {
    public DesecratedSlot(
        String id,
        ModifierDefinition.AffixType side,
        String modifierId,
        Integer minimumModifierLevel) {
      this(id, side, modifierId, minimumModifierLevel, null);
    }

    public DesecratedSlot(String id, ModifierDefinition.AffixType side, String modifierId) {
      this(id, side, modifierId, null);
    }

    public DesecratedSlot {
      if (revealPool != null
          && !Set.of("ALTERED", "AMANAMU", "KURGAL", "ULAMAN").contains(revealPool))
        throw new IllegalArgumentException("Unknown reveal pool");
      if (minimumModifierLevel != null && minimumModifierLevel != 40)
        throw new IllegalArgumentException("Unsupported desecration level constraint");
      if (id == null || id.isBlank() || side == null || side == ModifierDefinition.AffixType.NONE)
        throw new IllegalArgumentException("Desecrated slot identity and side required");
    }
  }

  public ItemState(
      String snapshotId,
      String baseItemId,
      int itemLevel,
      Rarity rarity,
      List<ModifierInstance> implicits,
      List<ModifierInstance> explicits,
      Set<Condition> conditions,
      Integer augmentSockets,
      CatalystQuality catalystQuality) {
    this(
        snapshotId,
        baseItemId,
        itemLevel,
        rarity,
        implicits,
        explicits,
        conditions,
        augmentSockets,
        catalystQuality,
        List.of());
  }

  public ItemState withDesecrated(List<DesecratedSlot> slots) {
    return new ItemState(
        snapshotId,
        baseItemId,
        itemLevel,
        rarity,
        implicits,
        explicits,
        conditions,
        augmentSockets,
        catalystQuality,
        slots,
        equipmentQuality,
        observations,
        sanctification,
        installedAugments,
        qualityInfused);
  }

  public int explicitSlotCount() {
    return explicits.size() + (int) desecrated.stream().filter(s -> s.modifierId() == null).count();
  }

  public ItemState(
      String snapshotId,
      String baseItemId,
      int itemLevel,
      Rarity rarity,
      List<ModifierInstance> implicits,
      List<ModifierInstance> explicits,
      Set<Condition> conditions,
      Integer augmentSockets) {
    this(
        snapshotId,
        baseItemId,
        itemLevel,
        rarity,
        implicits,
        explicits,
        conditions,
        augmentSockets,
        null);
  }

  /** Legacy states have unknown socket counts; never infer zero during migration. */
  public ItemState(
      String snapshotId,
      String baseItemId,
      int itemLevel,
      Rarity rarity,
      List<ModifierInstance> implicits,
      List<ModifierInstance> explicits,
      Set<Condition> conditions) {
    this(snapshotId, baseItemId, itemLevel, rarity, implicits, explicits, conditions, null);
  }

  /**
   * {@code ItemState}에서 사용하는 장착된·증강물의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code slotIndex}: 슬롯·색인 ({@code int}).
   *   <li>{@code augmentId}: 증강물·식별자 ({@code String}).
   * </ul>
   */
  public record InstalledAugment(int slotIndex, String augmentId) {}

  /** Preserve legacy empty-item hashing used by bounded search iteration and saved state keys. */
  @Override
  public int hashCode() {
    int hash = Objects.hashCode(snapshotId);
    hash = 31 * hash + Objects.hashCode(baseItemId);
    hash = 31 * hash + itemLevel;
    hash = 31 * hash + Objects.hashCode(rarity);
    hash = 31 * hash + Objects.hashCode(implicits);
    hash = 31 * hash + Objects.hashCode(explicits);
    hash = 31 * hash + Objects.hashCode(conditions);
    hash = 31 * hash + Objects.hashCode(augmentSockets);
    hash = 31 * hash + Objects.hashCode(catalystQuality);
    hash = 31 * hash + Objects.hashCode(desecrated);
    hash = 31 * hash + Objects.hashCode(equipmentQuality);
    hash = 31 * hash + Objects.hashCode(observations);
    hash = 31 * hash + Objects.hashCode(sanctification);
    hash = installedAugments.isEmpty() ? hash : 31 * hash + installedAugments.hashCode();
    return qualityInfused ? 31 * hash + 1 : hash;
  }

  public ItemState withAugments(List<InstalledAugment> value) {
    return new ItemState(
        snapshotId,
        baseItemId,
        itemLevel,
        rarity,
        implicits,
        explicits,
        conditions,
        augmentSockets,
        catalystQuality,
        desecrated,
        equipmentQuality,
        observations,
        sanctification,
        value,
        qualityInfused);
  }

  public ItemState {
    installedAugments = installedAugments == null ? List.of() : List.copyOf(installedAugments);
    if (snapshotId == null || snapshotId.isBlank() || baseItemId == null || baseItemId.isBlank()) {
      throw new IllegalArgumentException("Snapshot and base item IDs are required");
    }
    if (itemLevel < 1 || itemLevel > 100) {
      throw new IllegalArgumentException("Item level must be between 1 and 100");
    }
    Objects.requireNonNull(rarity, "rarity");
    SocketAugments.validate(baseItemId, augmentSockets, installedAugments);
    if (equipmentQuality != null && (equipmentQuality < 0 || equipmentQuality > 100))
      throw new IllegalArgumentException("Invalid observed equipment quality");
    implicits = canonical(implicits);
    explicits = canonical(explicits);
    conditions = Set.copyOf(conditions);
    if (sanctification != null
        && (!conditions.contains(Condition.SANCTIFIED) || rarity != Rarity.RARE))
      throw new IllegalArgumentException("Sanctification requires a Sanctified Rare item");
    desecrated = desecrated == null ? List.of() : List.copyOf(desecrated);
    if (desecrated.stream().map(DesecratedSlot::id).distinct().count() != desecrated.size())
      throw new IllegalArgumentException("Duplicate desecrated slot");
  }

  private static List<ModifierInstance> canonical(List<ModifierInstance> modifiers) {
    return modifiers.stream()
        .sorted(
            Comparator.comparing(ModifierInstance::modifierId)
                .thenComparing(m -> m.values().toString()))
        .toList();
  }

  /**
   * {@code ItemState}의 희귀도 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code NORMAL}: 일반 구분값.
   *   <li>{@code MAGIC}: 마법 구분값.
   *   <li>{@code RARE}: 희귀 구분값.
   *   <li>{@code UNIQUE}: 고유 구분값.
   * </ul>
   */
  public enum Rarity {
    NORMAL,
    MAGIC,
    RARE,
    UNIQUE
  }

  /** Preserved on input, but not supported by the initial base-affix model. */
  /** Imported Sanctified values are already final; no original roll or factor is reconstructed. */
  public boolean observedSanctified() {
    return conditions.contains(Condition.SANCTIFIED) && sanctification == null;
  }

  /**
   * {@code ItemState}의 조건 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code CORRUPTED}: 타락 구분값.
   *   <li>{@code MIRRORED}: MIRRORED 구분값.
   *   <li>{@code UNIDENTIFIED}: UNIDENTIFIED 구분값.
   *   <li>{@code SANCTIFIED}: SANCTIFIED 구분값.
   * </ul>
   */
  public enum Condition {
    CORRUPTED,
    MIRRORED,
    UNIDENTIFIED,
    SANCTIFIED
  }
}
