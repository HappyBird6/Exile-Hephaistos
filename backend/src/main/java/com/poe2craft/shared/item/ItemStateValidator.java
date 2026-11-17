package com.poe2craft.shared.item;

import com.poe2craft.shared.item.ModifierDefinition.AffixType;
import com.poe2craft.shared.item.ModifierDefinition.Layer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 입력을 수정하거나 미지원 옵션을 버리지 않고 카탈로그에 맞는지 검증한다.
 *
 * <p>기존 계약: Validation does not repair input or remove unsupported modifiers.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ItemStateValidator(catalog)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code catalog}(카탈로그).
 *   <li>{@code validate(state)}: 입력의 구조·값·지원 조건을 검증한다. 입력: {@code state}(상태). 반환: {@code
 *       List<Violation>}.
 *   <li>{@code validateModifiers(instances, layer, errors, finalValues)}: 옵션목록을(를) 검증한다. 입력: {@code
 *       instances}(instances), {@code layer}(옵션계층), {@code errors}(오류목록), {@code
 *       finalValues}(최종·값목록).
 *   <li>{@code validateForGeneration(state)}: 대상·세대번호을(를) 검증한다. 입력: {@code state}(상태). 반환: {@code
 *       List<Violation>}.
 *   <li>{@code slots(state)}: 슬롯목록 처리에서 {@code state.snapshotId}, {@code catalog.metadata}을(를)
 *       사용한다. 입력: {@code state}(상태). 반환: {@code AffixSlots}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code AffixSlots}: {@code ItemStateValidator}에서 사용하는 접두접미옵션·슬롯목록의 불변 값이다.
 *   <li>{@code Violation}: {@code ItemStateValidator}에서 사용하는 Violation의 불변 값이다.
 *   <li>{@code Code}: {@code ItemStateValidator}의 코드 구분값을 정의한다.
 * </ul>
 */
public final class ItemStateValidator {
  private final ItemCatalog catalog;

  public ItemStateValidator(ItemCatalog catalog) {
    this.catalog = java.util.Objects.requireNonNull(catalog);
  }

  public List<Violation> validate(ItemState state) {
    var errors = new ArrayList<Violation>();
    if (BaseRegistry.rareOnly(state.baseItemId()) && state.rarity() != ItemState.Rarity.RARE)
      errors.add(
          new Violation(
              Code.UNSUPPORTED_STATE,
              "",
              "This base requires Rare rarity under the selected crafting policy."));
    if (state.observations() != null) {
      var observed = state.observations();
      if ((observed.socketCount() != null && BasicJewel.supportedCrafting(state.baseItemId()))
          || (observed.socketCount() != null
              && state.augmentSockets() != null
              && !observed.socketCount().equals(state.augmentSockets()))
          || observed.grantedSkills().stream()
              .anyMatch(
                  s ->
                      !BaseRegistry.recognizesGrantedSkill(
                          state.baseItemId(), "Grants Skill: " + s.name())))
        errors.add(
            new Violation(
                Code.UNSUPPORTED_STATE, "", "Conflicting or unsupported clipboard observations"));
    }
    if (!state.desecrated().isEmpty()) {
      if (state.rarity() != ItemState.Rarity.RARE)
        errors.add(
            new Violation(Code.UNSUPPORTED_STATE, "", "Desecration requires Rare equipment"));
      var marked = new HashSet<String>();
      for (var slot : state.desecrated()) {
        if (slot.modifierId() == null) continue;
        var m =
            state.explicits().stream()
                .filter(v -> v.modifierId().equals(slot.modifierId()))
                .findFirst();
        if (!marked.add(slot.modifierId())
            || m.isEmpty()
            || m.get().fractured()
            || catalog
                .find(slot.modifierId())
                .map(
                    d ->
                        d.affixType() != slot.side()
                            || (slot.minimumModifierLevel() != null
                                && d.requiredItemLevel() < slot.minimumModifierLevel()))
                .orElse(true))
          errors.add(new Violation(Code.UNSUPPORTED_STATE, "", "Invalid desecrated modifier"));
      }
    }
    if (BasicJewel.supportedCrafting(state.baseItemId())
        && (state.augmentSockets() != null
            || state.explicits().stream().anyMatch(ModifierInstance::fractured)))
      errors.add(
          new Violation(
              Code.UNSUPPORTED_STATE,
              "",
              "Sapphire socket and fractured states remain unsupported"));
    if (BasicJewel.supportedCrafting(state.baseItemId())
        && state.explicits().stream().filter(m -> AugmentCraftingRules.crafted(m, catalog)).count()
            > 1)
      errors.add(new Violation(Code.UNSUPPORTED_STATE, "", "Only one Crafted modifier is allowed"));
    if (state.equipmentQuality() != null
        && (BaseRegistry.policy(state.baseItemId()) == null
            || !BaseRegistry.policy(state.baseItemId()).qualityLimit()
            || BaseRegistry.catalystQuality(state.baseItemId())
            || BasicJewel.supportedCrafting(state.baseItemId())))
      errors.add(
          new Violation(
              Code.UNSUPPORTED_STATE, "", "Equipment quality requires ordinary equipment"));
    if (state.catalystQuality() != null) {
      int cap =
          state.baseItemId().equals(SolarAmulet.BASE_ID)
                  || ReviewedRings.supports(state.baseItemId())
                  || ReviewedAmulets.supports(state.baseItemId())
              ? 40
              : 20;
      if (!CatalystQuality.supportedBase(state.baseItemId())
          || state.catalystQuality().amount() > cap + (state.qualityInfused() ? 10 : 0))
        errors.add(
            new Violation(
                Code.UNSUPPORTED_STATE,
                "",
                "Unsupported catalyst quality or amount above reviewed reachable maximum"));
    }
    if (!state.snapshotId().equals(catalog.metadata().snapshotId())) {
      errors.add(new Violation(Code.SNAPSHOT_MISMATCH, "", "State and catalog snapshots differ"));
    }
    if (!state.baseItemId().equals(catalog.base().id())) {
      errors.add(
          new Violation(Code.UNSUPPORTED_BASE, "", "Only this snapshot's base is supported"));
    }
    if (state.rarity() == ItemState.Rarity.UNIQUE
        || state.conditions().stream().anyMatch(c -> c != ItemState.Condition.SANCTIFIED)) {
      errors.add(
          new Violation(
              Code.UNSUPPORTED_STATE, "", "Unique or special item conditions are not supported"));
    }
    if (catalog.base().hasImplicit()
        ? state.implicits().size() != 1
            || !state
                .implicits()
                .getFirst()
                .modifierId()
                .equals(catalog.base().implicitModifierId())
        : !state.implicits().isEmpty()) {
      errors.add(new Violation(Code.INVALID_IMPLICIT, "", "Implicits must match the catalog base"));
    }
    validateModifiers(state.implicits(), Layer.IMPLICIT, errors, state.observedSanctified());
    validateModifiers(state.explicits(), Layer.EXPLICIT, errors, state.observedSanctified());
    if (state.implicits().stream().anyMatch(ModifierInstance::crafted)
        || state.explicits().stream().filter(m -> AugmentCraftingRules.crafted(m, catalog)).count()
            > 2)
      errors.add(new Violation(Code.UNSUPPORTED_STATE, "", "Invalid Crafted modifier state"));
    if (state.implicits().stream().anyMatch(ModifierInstance::fractured)
        || state.explicits().stream().filter(ModifierInstance::fractured).count() > 1
        || (state.explicits().stream().anyMatch(ModifierInstance::fractured)
            && state.rarity() != ItemState.Rarity.RARE)) {
      errors.add(
          new Violation(
              Code.UNSUPPORTED_STATE,
              "",
              "Only one fractured explicit on a Rare item is supported"));
    }
    if (state.rarity() != ItemState.Rarity.UNIQUE
        && state.snapshotId().equals(catalog.metadata().snapshotId())
        && state.baseItemId().equals(catalog.base().id())) {
      var slots = slots(state);
      if (BasicJewel.supportedCrafting(state.baseItemId())
              && state.rarity() == ItemState.Rarity.RARE
          ? !BasicJewel.existingCapacity(
              slots.usedPrefixes(), slots.usedSuffixes(), slots.maxPrefixes(), slots.maxSuffixes())
          : slots.usedPrefixes() > slots.maxPrefixes()
              || slots.usedSuffixes() > slots.maxSuffixes()) {
        errors.add(
            new Violation(Code.AFFIX_CAPACITY_EXCEEDED, "", "Explicit affix capacity exceeded"));
      }
    }
    if (state.observedSanctified() && state.rarity() != ItemState.Rarity.RARE)
      errors.add(
          new Violation(Code.UNSUPPORTED_STATE, "", "Sanctified imports require Rare rarity"));
    if (state.sanctification() != null && !QualitySanctification.valid(state, catalog))
      errors.add(new Violation(Code.UNSUPPORTED_STATE, "", "Invalid sanctification projection"));
    return List.copyOf(errors);
  }

  private void validateModifiers(
      List<ModifierInstance> instances, Layer layer, List<Violation> errors, boolean finalValues) {
    var ids = new HashSet<String>();
    var families = new HashSet<String>();
    for (var instance : instances) {
      String id = instance.modifierId();
      if (!ids.add(id))
        errors.add(new Violation(Code.DUPLICATE_MODIFIER, id, "Modifier appears twice"));
      var definition = catalog.find(id).orElse(null);
      if (definition == null) {
        errors.add(
            new Violation(Code.UNKNOWN_MODIFIER, id, "Modifier is not in the Base snapshot"));
        continue;
      }
      if (definition.layer() != layer) {
        errors.add(
            new Violation(Code.WRONG_MODIFIER_LAYER, id, "Implicit and explicit layers differ"));
      }
      if (layer == Layer.EXPLICIT) {
        if (definition.familyIds().stream().anyMatch(families::contains)) {
          errors.add(
              new Violation(Code.CONFLICTING_MODIFIERS, id, "Explicit modifier families overlap"));
        }
        families.addAll(definition.familyIds());
      }
      Set<String> expected = new HashSet<>();
      for (var stat : definition.stats()) {
        expected.add(stat.id());
        Long value = instance.values().get(stat.id());
        if (!finalValues && value != null && !stat.contains(value)) {
          errors.add(new Violation(Code.VALUE_OUT_OF_RANGE, id, "Stat out of range: " + stat.id()));
        }
      }
      if (!instance.values().keySet().equals(expected)) {
        errors.add(new Violation(Code.STAT_SET_MISMATCH, id, "Missing or additional stat values"));
      }
    }
  }

  /** Separate from existing-item validation: only newly generated affixes obey this check. */
  public List<Violation> validateForGeneration(ItemState state) {
    var errors = new ArrayList<>(validate(state));
    for (var modifier : state.explicits()) {
      catalog
          .find(modifier.modifierId())
          .ifPresent(
              definition -> {
                if (definition.requiredItemLevel() > state.itemLevel()) {
                  errors.add(
                      new Violation(
                          Code.ITEM_LEVEL_TOO_LOW,
                          definition.id(),
                          "New modifier requires a higher item level"));
                }
              });
    }
    if (state.sanctification() != null && !QualitySanctification.valid(state, catalog))
      errors.add(new Violation(Code.UNSUPPORTED_STATE, "", "Invalid sanctification projection"));
    return List.copyOf(errors);
  }

  /** Counts instances, not display lines. Only call for known, supported base states. */
  public AffixSlots slots(ItemState state) {
    if (!state.snapshotId().equals(catalog.metadata().snapshotId())
        || !state.baseItemId().equals(catalog.base().id())
        || state.rarity() == ItemState.Rarity.UNIQUE) {
      throw new IllegalArgumentException("Cannot resolve slots for this state");
    }
    int prefixes = 0, suffixes = 0;
    for (var instance : state.explicits()) {
      var definition = catalog.find(instance.modifierId()).orElse(null);
      if (definition != null && definition.layer() == Layer.EXPLICIT) {
        if (definition.affixType() == AffixType.PREFIX) prefixes++;
        if (definition.affixType() == AffixType.SUFFIX) suffixes++;
      }
    }
    prefixes +=
        (int)
            state.desecrated().stream()
                .filter(s -> s.modifierId() == null && s.side() == AffixType.PREFIX)
                .count();
    suffixes +=
        (int)
            state.desecrated().stream()
                .filter(s -> s.modifierId() == null && s.side() == AffixType.SUFFIX)
                .count();
    int maxPrefixes =
        switch (state.rarity()) {
          case NORMAL -> 0;
          case MAGIC -> catalog.base().magicPrefixes();
          case RARE -> catalog.base().rarePrefixes();
          default -> throw new IllegalArgumentException("Unsupported rarity");
        };
    int maxSuffixes =
        switch (state.rarity()) {
          case NORMAL -> 0;
          case MAGIC -> catalog.base().magicSuffixes();
          case RARE -> catalog.base().rareSuffixes();
          default -> throw new IllegalArgumentException("Unsupported rarity");
        };
    if (!catalog.augmentContextApplied() && state.rarity() != ItemState.Rarity.NORMAL)
      maxSuffixes += AugmentCraftingRules.suffixBonus(state);
    if (BasicJewel.supportedCrafting(state.baseItemId())
        && state.rarity() == ItemState.Rarity.RARE) {
      var ids = state.explicits().stream().map(ModifierInstance::modifierId).toList();
      maxPrefixes += BasicJewel.extra(ids, AffixType.PREFIX);
      maxSuffixes += BasicJewel.extra(ids, AffixType.SUFFIX);
    }
    return new AffixSlots(prefixes, suffixes, maxPrefixes, maxSuffixes);
  }

  /**
   * {@code ItemStateValidator}에서 사용하는 접두접미옵션·슬롯목록의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code usedPrefixes}: used·접두목록 ({@code int}).
   *   <li>{@code usedSuffixes}: used·접미목록 ({@code int}).
   *   <li>{@code maxPrefixes}: 최대값·접두목록 ({@code int}).
   *   <li>{@code maxSuffixes}: 최대값·접미목록 ({@code int}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code remainingPrefixes()}: 남은·접두목록 값을 조회·구성한다. 반환: {@code int}.
   *   <li>{@code remainingSuffixes()}: 남은·접미목록 값을 조회·구성한다. 반환: {@code int}.
   * </ul>
   */
  public record AffixSlots(int usedPrefixes, int usedSuffixes, int maxPrefixes, int maxSuffixes) {
    public int remainingPrefixes() {
      return Math.max(0, maxPrefixes - usedPrefixes);
    }

    public int remainingSuffixes() {
      return Math.max(0, maxSuffixes - usedSuffixes);
    }
  }

  /**
   * {@code ItemStateValidator}에서 사용하는 Violation의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code code}: 코드 ({@code Code}).
   *   <li>{@code modifierId}: 실제 부여된 옵션 정의 식별자 ({@code String}).
   *   <li>{@code message}: 메시지 ({@code String}).
   * </ul>
   */
  public record Violation(Code code, String modifierId, String message) {}

  /**
   * {@code ItemStateValidator}의 코드 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code SNAPSHOT_MISMATCH}: MISMATCH 구분값.
   *   <li>{@code UNSUPPORTED_BASE}: 베이스 구분값.
   *   <li>{@code UNSUPPORTED_STATE}: 상태 구분값.
   *   <li>{@code INVALID_IMPLICIT}: 암시옵션 구분값.
   *   <li>{@code UNKNOWN_MODIFIER}: 옵션 구분값.
   *   <li>{@code DUPLICATE_MODIFIER}: 옵션 구분값.
   *   <li>{@code WRONG_MODIFIER_LAYER}: 옵션계층 구분값.
   *   <li>{@code CONFLICTING_MODIFIERS}: 옵션목록 구분값.
   *   <li>{@code STAT_SET_MISMATCH}: MISMATCH 구분값.
   *   <li>{@code VALUE_OUT_OF_RANGE}: 범위 구분값.
   *   <li>{@code AFFIX_CAPACITY_EXCEEDED}: EXCEEDED 구분값.
   *   <li>{@code ITEM_LEVEL_TOO_LOW}: LOW 구분값.
   * </ul>
   */
  public enum Code {
    SNAPSHOT_MISMATCH,
    UNSUPPORTED_BASE,
    UNSUPPORTED_STATE,
    INVALID_IMPLICIT,
    UNKNOWN_MODIFIER,
    DUPLICATE_MODIFIER,
    WRONG_MODIFIER_LAYER,
    CONFLICTING_MODIFIERS,
    STAT_SET_MISMATCH,
    VALUE_OUT_OF_RANGE,
    AFFIX_CAPACITY_EXCEEDED,
    ITEM_LEVEL_TOO_LOW
  }
}
