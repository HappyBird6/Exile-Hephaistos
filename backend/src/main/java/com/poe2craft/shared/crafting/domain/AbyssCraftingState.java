package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.BasicJewel;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierDefinition.AffixType;
import com.poe2craft.shared.item.ModifierInstance;
import java.util.*;

/**
 * 관측된 훼손 슬롯과 공개 후보·수동 선택 상태를 보존한다.
 *
 * <p>기존 계약: Manual observed Abyss slots. No distribution over reveal offers is assumed.
 *
 * <p>필드·상수·record 구성요소:
 *
 * <ul>
 *   <li>{@code item}: 아이템 ({@code ItemState}).
 *   <li>{@code slots}: 슬롯목록 ({@code List<Slot>}).
 *   <li>{@code offer}: offer ({@code Offer}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AbyssCraftingState(item, slots, offer)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code item}(아이템),
 *       {@code slots}(슬롯목록), {@code offer}(offer). 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code from(state, catalog)}: 다른 표현의 입력을 현재 표현으로 바꾼다. 입력: {@code state}(상태), {@code
 *       catalog}(카탈로그). 반환: {@code AbyssCraftingState}.
 *   <li>{@code knownItem()}: 알려진·아이템 값을 조회·구성한다. 반환: {@code ItemState}.
 *   <li>{@code storedItem()}: 저장된·아이템 값을 조회·구성한다. 반환: {@code ItemState}.
 *   <li>{@code validate(catalog)}: 입력의 구조·값·지원 조건을 검증한다. 입력: {@code catalog}(카탈로그). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code count(side)}: 개수 처리에서 {@code slots.stream}, {@code s.side}을(를) 사용한다. 입력: {@code
 *       side}(접두접미). 반환: {@code long}.
 *   <li>{@code addUnrevealed(id, side, catalog)}: Unrevealed을(를) 추가한다. 입력: {@code id}(식별자), {@code
 *       side}(접두접미), {@code catalog}(카탈로그). 반환: {@code AbyssCraftingState}.
 *   <li>{@code observe(slotId, candidates, catalog)}: observe 처리에서 {@code result.validate}을(를)
 *       사용한다. 입력: {@code slotId}(슬롯·식별자), {@code candidates}(후보목록), {@code catalog}(카탈로그). 반환:
 *       {@code AbyssCraftingState}.
 *   <li>{@code eligibleRevealIds(id, catalog)}: 적격·공개·식별자목록 처리에서 {@code slots.stream}, {@code
 *       s.id}을(를) 사용한다. 입력: {@code id}(식별자), {@code catalog}(카탈로그). 반환: {@code List<String>}.
 *   <li>{@code revealEligible(slot, d)}: 공개·적격 처리에서 {@code d.layer}, {@code d.affixType}을(를) 사용한다.
 *       입력: {@code slot}(슬롯), {@code d}(d). 반환: {@code boolean}.
 *   <li>{@code selectDirect(id, candidate, catalog)}: 선택·직접 처리에서 {@code candidate.modifierId}을(를)
 *       사용한다. 입력: {@code id}(식별자), {@code candidate}(후보), {@code catalog}(카탈로그). 반환: {@code
 *       AbyssCraftingState}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code choose(candidateIndex, catalog)}: choose 처리에서 {@code offer.slotId}, {@code
 *       offer.candidates}을(를) 사용한다. 입력: {@code candidateIndex}(후보·색인), {@code catalog}(카탈로그). 반환:
 *       {@code AbyssCraftingState}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code selected(id, candidate, catalog)}: 선택된 처리에서 {@code slots.stream}, {@code s.id}을(를)
 *       사용한다. 입력: {@code id}(식별자), {@code candidate}(후보), {@code catalog}(카탈로그). 반환: {@code
 *       AbyssCraftingState}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code append(slotId, modifier, catalog)}: 뒤에 값이나 계산 행을 추가한다. 입력: {@code slotId}(슬롯·식별자),
 *       {@code modifier}(옵션), {@code catalog}(카탈로그). 반환: {@code AbyssCraftingState}. 사용하는 예외:
 *       {@code IllegalArgumentException}.
 *   <li>{@code removalCandidates(side)}: 제거·후보목록 처리에서 {@code slots.stream}, {@code s.modifier}을(를)
 *       사용한다. 입력: {@code side}(접두접미). 반환: {@code List<Slot>}.
 *   <li>{@code remove(slotId, side, catalog)}: 지정한 항목을 제거한다. 입력: {@code slotId}(슬롯·식별자), {@code
 *       side}(접두접미), {@code catalog}(카탈로그). 반환: {@code AbyssCraftingState}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code requireMutable()}: 가변을(를) 필수 조건을 확인한다. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code withKnownItem(nextItem, removedModifierIds, catalog)}: 알려진·아이템을(를) 반영한 값을 구한다. 입력:
 *       {@code nextItem}(다음·아이템), {@code removedModifierIds}(제거된·옵션·식별자목록), {@code catalog}(카탈로그).
 *       반환: {@code AbyssCraftingState}.
 *   <li>{@code occupiedSlotId(id)}: occupied·슬롯·식별자 처리에서 {@code slots.stream}, {@code s.id}을(를)
 *       사용한다. 입력: {@code id}(식별자). 반환: {@code boolean}.
 *   <li>{@code withExplicits(s, modifiers)}: 명시옵션목록을(를) 반영한 값을 구한다. 입력: {@code s}(s), {@code
 *       modifiers}(옵션목록). 반환: {@code ItemState}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Slot}: {@code AbyssCraftingState}에서 사용하는 슬롯의 불변 값이다.
 *   <li>{@code Offer}: {@code AbyssCraftingState}에서 사용하는 Offer의 불변 값이다.
 * </ul>
 */
public record AbyssCraftingState(ItemState item, List<Slot> slots, Offer offer) {
  public AbyssCraftingState {
    Objects.requireNonNull(item);
    slots = List.copyOf(slots);
    if (!item.explicits().isEmpty())
      throw new IllegalArgumentException("Explicit instances belong in slots, not twice in item");
    if (slots.size() > 7 || slots.stream().map(Slot::id).distinct().count() != slots.size())
      throw new IllegalArgumentException("Invalid explicit slots");
    if (offer != null
        && slots.stream().noneMatch(s -> s.id().equals(offer.slotId()) && s.desecrated()))
      throw new IllegalArgumentException("Offer must target a desecrated slot");
  }

  /**
   * {@code AbyssCraftingState}에서 사용하는 슬롯의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code side}: 접두접미 ({@code AffixType}).
   *   <li>{@code modifier}: 옵션 ({@code ModifierInstance}).
   *   <li>{@code desecrated}: 훼손 ({@code boolean}).
   *   <li>{@code minimumModifierLevel}: 최소값·옵션·레벨 ({@code Integer}).
   *   <li>{@code revealPool}: 공개·후보풀 ({@code String}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Slot(id, side, modifier, desecrated, minimumModifierLevel)}: 기본 정책을 보충해 공통 생성자로
   *       위임한다. 입력: {@code id}(식별자), {@code side}(접두접미), {@code modifier}(옵션), {@code
   *       desecrated}(훼손), {@code minimumModifierLevel}(최소값·옵션·레벨).
   *   <li>{@code Slot(id, side, modifier, desecrated)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
   *       id}(식별자), {@code side}(접두접미), {@code modifier}(옵션), {@code desecrated}(훼손).
   *   <li>{@code Slot(id, side, modifier, desecrated, minimumModifierLevel, revealPool)}: 전달받은 값으로
   *       구성원을 초기화한다. 입력: {@code id}(식별자), {@code side}(접두접미), {@code modifier}(옵션), {@code
   *       desecrated}(훼손), {@code minimumModifierLevel}(최소값·옵션·레벨), {@code revealPool}(공개·후보풀).
   *       사용하는 예외: {@code IllegalArgumentException}.
   * </ul>
   */
  public record Slot(
      String id,
      AffixType side,
      ModifierInstance modifier,
      boolean desecrated,
      Integer minimumModifierLevel,
      String revealPool) {
    public Slot(
        String id,
        AffixType side,
        ModifierInstance modifier,
        boolean desecrated,
        Integer minimumModifierLevel) {
      this(id, side, modifier, desecrated, minimumModifierLevel, null);
    }

    public Slot(String id, AffixType side, ModifierInstance modifier, boolean desecrated) {
      this(id, side, modifier, desecrated, null);
    }

    public Slot {
      if (revealPool != null
          && (!desecrated
              || !Set.of("ALTERED", "AMANAMU", "KURGAL", "ULAMAN").contains(revealPool)))
        throw new IllegalArgumentException("Unknown reveal pool");
      if (minimumModifierLevel != null && (!desecrated || minimumModifierLevel != 40))
        throw new IllegalArgumentException("Unsupported desecration level constraint");
      if (id == null || id.isBlank() || id.length() > 160 || side == null || side == AffixType.NONE)
        throw new IllegalArgumentException("Slot identity and affix side are required");
      if (modifier == null && !desecrated)
        throw new IllegalArgumentException("Only desecrated slots can be unrevealed");
      if (desecrated && modifier != null && modifier.fractured())
        throw new IllegalArgumentException("Fractured desecrated interaction is unsupported");
    }
  }

  /**
   * {@code AbyssCraftingState}에서 사용하는 Offer의 불변 값이다.
   *
   * <p>기존 계약: Exactly the three candidates observed by the user; no probability or omen cost.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code slotId}: 슬롯·식별자 ({@code String}).
   *   <li>{@code candidates}: 후보목록 ({@code List<ModifierInstance>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Offer(slotId, candidates)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code slotId}(슬롯·식별자),
   *       {@code candidates}(후보목록). 사용하는 예외: {@code IllegalArgumentException}.
   * </ul>
   */
  public record Offer(String slotId, List<ModifierInstance> candidates) {
    public Offer {
      if (slotId == null || slotId.isBlank()) throw new IllegalArgumentException("Target required");
      candidates = List.copyOf(candidates);
      if (candidates.size() != 3)
        throw new IllegalArgumentException("Three observed candidates required");
    }
  }

  public static AbyssCraftingState from(ItemState state, ItemCatalog catalog) {
    var slots = new ArrayList<Slot>();
    for (int i = 0; i < state.explicits().size(); i++) {
      var m = state.explicits().get(i);
      var d =
          catalog
              .find(m.modifierId())
              .orElseThrow(() -> new IllegalArgumentException("Unknown slot or modifier"));
      var marked =
          state.desecrated().stream()
              .filter(s -> m.modifierId().equals(s.modifierId()))
              .findFirst();
      slots.add(
          new Slot(
              marked.map(ItemState.DesecratedSlot::id).orElse("explicit-" + i),
              d.affixType(),
              m,
              marked.isPresent(),
              marked.map(ItemState.DesecratedSlot::minimumModifierLevel).orElse(null),
              marked.map(ItemState.DesecratedSlot::revealPool).orElse(null)));
    }
    state.desecrated().stream()
        .filter(s -> s.modifierId() == null)
        .forEach(
            s ->
                slots.add(
                    new Slot(
                        s.id(), s.side(), null, true, s.minimumModifierLevel(), s.revealPool())));
    var result = new AbyssCraftingState(withExplicits(state, List.of()), slots, null);
    result.validate(catalog);
    return result;
  }

  /** Evaluation projection only: unrevealed slots contribute no invented stat values. */
  public ItemState knownItem() {
    return withExplicits(
        item, slots.stream().map(Slot::modifier).filter(Objects::nonNull).toList());
  }

  /** Lossless common state, unlike knownItem(), which is only a stat evaluation projection. */
  public ItemState storedItem() {
    return knownItem()
        .withDesecrated(
            slots.stream()
                .filter(Slot::desecrated)
                .map(
                    s ->
                        new ItemState.DesecratedSlot(
                            s.id(),
                            s.side(),
                            s.modifier() == null ? null : s.modifier().modifierId(),
                            s.minimumModifierLevel(),
                            s.revealPool()))
                .toList());
  }

  public void validate(ItemCatalog catalog) {
    if (item.observations() != null
        && item.observations().needsAugmentResolution()
        && com.poe2craft.shared.item.SocketAugmentText.affectsCrafting(
            item.observations().augmentEffects()))
      throw new IllegalArgumentException(
          "Installed Augment names and counts must be resolved before crafting; observations are preserved.");
    var validationItem = knownItem();
    if (item.conditions().equals(Set.of(ItemState.Condition.CORRUPTED))
        && slots.stream().anyMatch(Slot::desecrated))
      validationItem =
          new ItemState(
                  validationItem.snapshotId(),
                  validationItem.baseItemId(),
                  validationItem.itemLevel(),
                  validationItem.rarity(),
                  validationItem.implicits(),
                  validationItem.explicits(),
                  Set.of(),
                  validationItem.augmentSockets(),
                  validationItem.catalystQuality(),
                  validationItem.desecrated(),
                  validationItem.equipmentQuality(),
                  validationItem.observations())
              .withAugments(validationItem.installedAugments())
              .withQualityInfused(validationItem.qualityInfused());
    if (!new ItemStateValidator(catalog).validate(validationItem).isEmpty())
      throw new IllegalArgumentException("Invalid known item state");
    if (slots.stream().anyMatch(Slot::desecrated) && item.rarity() != ItemState.Rarity.RARE)
      throw new IllegalArgumentException("Desecration requires a supported Rare equipment base");
    var capacity = new ItemStateValidator(catalog).slots(knownItem());
    if (BasicJewel.supportedCrafting(item.baseItemId())
        ? !BasicJewel.existingCapacity(
            (int) count(AffixType.PREFIX),
            (int) count(AffixType.SUFFIX),
            capacity.maxPrefixes(),
            capacity.maxSuffixes())
        : count(AffixType.PREFIX) > capacity.maxPrefixes()
            || count(AffixType.SUFFIX) > capacity.maxSuffixes())
      throw new IllegalArgumentException("Explicit affix capacity exceeded");
    for (var slot : slots) {
      if (!DesecrationCurrency.supportsRevealPool(item.baseItemId(), slot.revealPool()))
        throw new IllegalArgumentException("Reveal pool does not match the item class");
      if (slot.modifier() != null
          && catalog
              .find(slot.modifier().modifierId())
              .map(
                  d ->
                      (d.tags().contains("reveal_only")
                              && (!slot.desecrated() || !revealEligible(slot, d)))
                          || (slot.revealPool() != null && !revealEligible(slot, d)))
              .orElse(false)) throw new IllegalArgumentException("Invalid stored reveal pool");
      if (slot.minimumModifierLevel() != null
          && slot.modifier() != null
          && catalog.find(slot.modifier().modifierId()).orElseThrow().requiredItemLevel()
              < slot.minimumModifierLevel())
        throw new IllegalArgumentException("Candidate violates currency level limit");
      if (slot.modifier() != null
          && catalog
                  .find(slot.modifier().modifierId())
                  .orElseThrow(() -> new IllegalArgumentException("Unknown slot or modifier"))
                  .affixType()
              != slot.side())
        throw new IllegalArgumentException("Slot and modifier affix sides differ");
    }
    if (offer != null)
      for (var candidate : offer.candidates()) selected(offer.slotId(), candidate, catalog);
  }

  public long count(AffixType side) {
    return slots.stream().filter(s -> s.side() == side).count();
  }

  public AbyssCraftingState addUnrevealed(String id, AffixType side, ItemCatalog catalog) {
    requireMutable();
    validate(catalog);
    var next = new ArrayList<>(slots);
    next.add(new Slot(id, side, null, true));
    var result = new AbyssCraftingState(item, next, offer);
    result.validate(catalog);
    return result;
  }

  public AbyssCraftingState observe(
      String slotId, List<ModifierInstance> candidates, ItemCatalog catalog) {
    requireMutable();
    validate(catalog);
    var result = new AbyssCraftingState(item, slots, new Offer(slotId, candidates));
    result.validate(catalog);
    return result;
  }

  public List<String> eligibleRevealIds(String id, ItemCatalog catalog) {
    validate(catalog);
    var target =
        slots.stream()
            .filter(s -> s.id().equals(id) && s.desecrated() && s.modifier() == null)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unrevealed slot required"));
    return catalog.modifiers().values().stream()
        .filter(d -> revealEligible(target, d))
        .filter(
            d -> {
              try {
                var values = new HashMap<String, Long>();
                d.stats().forEach(r -> values.put(r.id(), r.min()));
                selected(id, new ModifierInstance(d.id(), values), catalog);
                return true;
              } catch (IllegalArgumentException ex) {
                return false;
              }
            })
        .map(ModifierDefinition::id)
        .sorted()
        .toList();
  }

  private boolean revealEligible(Slot slot, ModifierDefinition d) {
    if (d.layer() != ModifierDefinition.Layer.EXPLICIT
        || d.affixType() != slot.side()
        || (d.weight() <= 0 && !d.tags().contains("reveal_only"))) return false;
    if (d.tags().contains("otherworldly") && !"ALTERED".equals(slot.revealPool())) return false;
    return slot.revealPool() == null
        || "ALTERED".equals(slot.revealPool())
        || d.tags().contains(slot.revealPool().toLowerCase(java.util.Locale.ROOT) + "_mod");
  }

  public AbyssCraftingState selectDirect(
      String id, ModifierInstance candidate, ItemCatalog catalog) {
    requireMutable();
    if (!eligibleRevealIds(id, catalog).contains(candidate.modifierId()))
      throw new IllegalArgumentException("Ineligible reveal option");
    return selected(id, candidate, catalog);
  }

  public AbyssCraftingState choose(int candidateIndex, ItemCatalog catalog) {
    requireMutable();
    validate(catalog);
    if (offer == null || candidateIndex < 0 || candidateIndex >= 3)
      throw new IllegalArgumentException("Observed candidate required");
    return selected(offer.slotId(), offer.candidates().get(candidateIndex), catalog);
  }

  private AbyssCraftingState selected(String id, ModifierInstance candidate, ItemCatalog catalog) {
    var target =
        slots.stream()
            .filter(s -> s.id().equals(id))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown slot or modifier"));
    if (!target.desecrated() || candidate.fractured())
      throw new IllegalArgumentException("Only the desecrated slot can be reworked");
    var definition =
        catalog
            .find(candidate.modifierId())
            .orElseThrow(() -> new IllegalArgumentException("Unknown slot or modifier"));
    if (!revealEligible(target, definition))
      throw new IllegalArgumentException("Candidate violates reveal pool");
    if (definition.requiredItemLevel() > item.itemLevel()
        || (target.minimumModifierLevel() != null
            && definition.requiredItemLevel() < target.minimumModifierLevel()))
      throw new IllegalArgumentException("Candidate violates item or currency level limits");
    var next =
        slots.stream()
            .map(
                s ->
                    s.id().equals(id)
                        ? new Slot(
                            id, s.side(), candidate, true, s.minimumModifierLevel(), s.revealPool())
                        : s)
            .toList();
    var result = new AbyssCraftingState(item, next, null);
    result.validate(catalog);
    return result;
  }

  public AbyssCraftingState append(String slotId, ModifierInstance modifier, ItemCatalog catalog) {
    requireMutable();
    var definition =
        catalog
            .find(modifier.modifierId())
            .orElseThrow(() -> new IllegalArgumentException("Unknown slot or modifier"));
    if (definition.requiredItemLevel() > item.itemLevel() || modifier.fractured())
      throw new IllegalArgumentException("Unsupported generated modifier");
    var next = new ArrayList<>(slots);
    next.add(new Slot(slotId, definition.affixType(), modifier, false));
    var result = new AbyssCraftingState(item, next, offer);
    result.validate(catalog);
    return result;
  }

  /** Side restrictions apply to the dummy exactly as to a known explicit. */
  public List<Slot> removalCandidates(AffixType side) {
    return slots.stream()
        .filter(s -> s.modifier() == null || !s.modifier().fractured())
        .filter(s -> side == null || s.side() == side)
        .toList();
  }

  public AbyssCraftingState remove(String slotId, AffixType side, ItemCatalog catalog) {
    requireMutable();
    validate(catalog);
    if (removalCandidates(side).stream().noneMatch(s -> s.id().equals(slotId)))
      throw new IllegalArgumentException("Ineligible removal slot");
    return new AbyssCraftingState(
        item,
        slots.stream().filter(s -> !s.id().equals(slotId)).toList(),
        offer != null && offer.slotId().equals(slotId) ? null : offer);
  }

  /** Sanctified states may be read/restored, but no Abyss operation may modify them. */
  public void requireMutable() {
    if (item.conditions().contains(ItemState.Condition.SANCTIFIED))
      throw new IllegalArgumentException("Sanctified items cannot be modified");
  }

  /**
   * Reconcile a known-value operation without losing hidden slots, reveal constraints or an offer.
   */
  public AbyssCraftingState withKnownItem(
      ItemState nextItem, Set<String> removedModifierIds, ItemCatalog catalog) {
    var slots = new ArrayList<AbyssCraftingState.Slot>();
    slots().stream().filter(s -> s.modifier() == null).forEach(slots::add);
    int index = 0;
    for (var modifier : nextItem.explicits()) {
      var previous =
          this.slots().stream()
              .filter(
                  s ->
                      s.modifier() != null
                          && !removedModifierIds.contains(s.modifier().modifierId())
                          && s.modifier().modifierId().equals(modifier.modifierId()))
              .findFirst();
      if (previous.isPresent()) {
        var s = previous.get();
        slots.add(
            new AbyssCraftingState.Slot(
                s.id(),
                s.side(),
                modifier,
                s.desecrated(),
                s.minimumModifierLevel(),
                s.revealPool()));
      } else {
        String id;
        do {
          id = "currency-" + index++;
        } while (occupiedSlotId(id));
        slots.add(
            new AbyssCraftingState.Slot(
                id,
                catalog.find(modifier.modifierId()).orElseThrow().affixType(),
                modifier,
                false));
      }
    }
    var retainedOffer = offer();
    if (retainedOffer != null && slots.stream().noneMatch(s -> s.id().equals(offer().slotId())))
      retainedOffer = null;
    var next = new AbyssCraftingState(withExplicits(nextItem, List.of()), slots, retainedOffer);
    next.validate(catalog);
    return next;
  }

  private boolean occupiedSlotId(String id) {
    return slots.stream().anyMatch(s -> s.id().equals(id));
  }

  static ItemState withExplicits(ItemState s, List<ModifierInstance> modifiers) {
    return new ItemState(
            s.snapshotId(),
            s.baseItemId(),
            s.itemLevel(),
            s.rarity(),
            s.implicits(),
            modifiers,
            s.conditions(),
            s.augmentSockets(),
            s.catalystQuality())
        .withAugments(s.installedAugments())
        .withQualityInfused(s.qualityInfused())
        .withEquipmentQuality(s.equipmentQuality())
        .withObservations(s.observations())
        .withSanctification(s.sanctification());
  }
}
