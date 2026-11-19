package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.BaseRegistry;
import com.poe2craft.shared.item.BasicJewel;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ManualRevealCatalog;
import com.poe2craft.shared.item.ModifierDefinition.AffixType;
import java.util.*;
import java.util.random.RandomGenerator;

/**
 * 훼손 생성·제거를 앱 정책으로 실행하고 공개 후보 선택은 수동으로 유지한다.
 *
 * <p>기존 계약: Reveal choices remain manual. Automatic side/removal uses an explicit simulator policy,
 * not published game probabilities; explicit observed inputs remain supported.
 *
 * <p>POLICY는 버전 JSON의 화폐별 장비군, 레벨 및 공개 풀을 공유한다.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code DesecrationCurrency()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code supportsRevealPool(baseItemId, pool)}: 공개·후보풀을(를) 지원 여부를 판정한다. 입력: {@code
 *       baseItemId}(베이스·아이템·식별자), {@code pool}(후보풀). 반환: {@code boolean}.
 *   <li>{@code putrefy(item, currency, observedSides, omens, catalog)}: putrefy 처리에서 {@code
 *       observedSides.isEmpty}, {@code observedSides.size}을(를) 사용한다. 입력: {@code item}(아이템), {@code
 *       currency}(화폐), {@code observedSides}(관측한·Sides), {@code omens}(징조목록), {@code
 *       catalog}(카탈로그). 반환: {@code AbyssCraftingState}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code apply(item, currency, side, removedSlotId, activeOmens, catalog)}: 주어진 행동 또는 변경을 적용한
 *       결과를 구한다. 입력: {@code item}(아이템), {@code currency}(화폐), {@code side}(접두접미), {@code
 *       removedSlotId}(제거된·슬롯·식별자), {@code activeOmens}(활성·징조목록), {@code catalog}(카탈로그). 반환: {@code
 *       AbyssCraftingState}.
 *   <li>{@code apply(item, currency, side, removedSlotId, activeOmens, catalog, random)}: 주어진 행동 또는
 *       변경을 적용한 결과를 구한다. 입력: {@code item}(아이템), {@code currency}(화폐), {@code side}(접두접미), {@code
 *       removedSlotId}(제거된·슬롯·식별자), {@code activeOmens}(활성·징조목록), {@code catalog}(카탈로그), {@code
 *       random}(난수생성기). 반환: {@code AbyssCraftingState}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code applyEnriched(item, currency, side, removedSlotId, activeOmens, catalog)}:
 *       Enriched을(를) 적용한다. 입력: {@code item}(아이템), {@code currency}(화폐), {@code side}(접두접미), {@code
 *       removedSlotId}(제거된·슬롯·식별자), {@code activeOmens}(활성·징조목록), {@code catalog}(카탈로그). 반환: {@code
 *       AbyssCraftingState}. 사용하는 예외: {@code IllegalArgumentException}.
 * </ul>
 */
public final class DesecrationCurrency {
  private DesecrationCurrency() {}

  /** 화폐별 장비군·레벨·공개 풀 및 작업대 선택 정책이다. */
  private record Bone(
      String target,
      Integer maximumItemLevel,
      Integer minimumModifierLevel,
      String revealPool,
      boolean selectable) {}

  /** 버전별 JSON의 장비군과 화폐 사전이다. */
  private record Policy(
      java.util.Map<String, Set<String>> classes, java.util.Map<String, Bone> currencies) {}

  private static final Policy POLICY = loadPolicy();
  private static final Set<String> WEAPONS = POLICY.classes().get("WEAPONS");
  private static final Set<String> JEWELLERY = POLICY.classes().get("JEWELLERY");

  /** 봉인한 규칙 원본에서 런타임 사전을 한 번 읽는다. */
  private static Policy loadPolicy() {
    try (var input =
        com.poe2craft.shared.ruleset.SealedResources.open("/catalog/desecration-currencies.json")) {
      return new com.fasterxml.jackson.databind.ObjectMapper().readValue(input, Policy.class);
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Invalid desecration policy", e);
    }
  }

  /** 현재 베이스와 레벨에서 쓸 수 있는 화폐를 등록 순서로 반환한다. */
  static List<String> currencies(String baseId, int level) {
    return POLICY.currencies().keySet().stream()
        .filter(c -> supportsBase(c, baseId) && levelRefusal(c, level) == null)
        .toList();
  }

  /** 베이스의 장비 종류 또는 주얼 등록 여부가 화폐에 맞는지 검사한다. */
  private static boolean supportsBase(String currency, String baseId) {
    var group = POLICY.currencies().get(currency).target();
    if (group.equals("JEWEL")) return BasicJewel.supportedCrafting(baseId);
    var itemClass = BaseRegistry.itemClass(baseId);
    return itemClass != null && POLICY.classes().get(group).contains(itemClass);
  }

  /** 화폐 등급이 요구하는 아이템 레벨의 거절 사유, 또는 적용 가능하면 null을 반환한다. */
  private static String levelRefusal(String currency, int level) {
    var maximum = POLICY.currencies().get(currency).maximumItemLevel();
    if (maximum != null && level > maximum)
      return "Gnawed bones require item level " + maximum + " or lower";
    var minimum = minimumModifierLevel(currency);
    if (minimum != null && level < minimum)
      return "No candidate can satisfy both item and minimum modifier levels";
    return null;
  }

  /** 공개 슬롯에 유지할 옵션의 최소 요구 레벨을 반환한다. 하한이 없는 화폐는 null이다. */
  public static Integer minimumModifierLevel(String currency) {
    return POLICY.currencies().get(currency).minimumModifierLevel();
  }

  /** 별도 징조 없이 화폐 자체가 지정하는 공개 풀을 구한다. */
  public static String revealPool(String currency) {
    return POLICY.currencies().get(currency).revealPool();
  }

  public static boolean supportsRevealPool(String baseItemId, String pool) {
    if (pool == null) return true;
    var itemClass = BaseRegistry.itemClass(baseItemId);
    if (itemClass == null) return false;
    return "ALTERED".equals(pool)
        ? JEWELLERY.contains(itemClass)
        : (JEWELLERY.contains(itemClass) || WEAPONS.contains(itemClass));
  }

  public static AbyssCraftingState putrefy(
      ItemState item,
      String currency,
      List<AffixType> observedSides,
      Set<String> omens,
      ItemCatalog catalog) {
    if (!Set.of("Omen_of_Putrefaction").equals(omens)
        || observedSides == null
        || observedSides.isEmpty()
        || observedSides.size() > 7
        || observedSides.stream().anyMatch(s -> s == null || s == AffixType.NONE))
      throw new IllegalArgumentException("Record the observed slots for Putrefaction alone");
    if (item.explicits().stream().anyMatch(m -> m.fractured()))
      throw new IllegalArgumentException(
          "Putrefaction with fractured modifiers needs verified interaction rules");
    AbyssCraftingState.from(item, catalog);
    var empty =
        new ItemState(
                item.snapshotId(),
                item.baseItemId(),
                item.itemLevel(),
                item.rarity(),
                item.implicits(),
                List.of(),
                item.conditions(),
                item.augmentSockets(),
                item.catalystQuality(),
                item.desecrated(),
                item.equipmentQuality(),
                item.observations())
            .withAugments(item.installedAugments())
            .withQualityInfused(item.qualityInfused());
    var seed = apply(empty, currency, observedSides.getFirst(), null, Set.of(), catalog);
    var slots = new ArrayList<AbyssCraftingState.Slot>();
    var model = seed.slots().getFirst();
    for (var side : observedSides)
      slots.add(
          new AbyssCraftingState.Slot(
              "desecrated-" + UUID.randomUUID(),
              side,
              null,
              true,
              model.minimumModifierLevel(),
              model.revealPool()));
    var corrupt =
        new ItemState(
                empty.snapshotId(),
                empty.baseItemId(),
                empty.itemLevel(),
                empty.rarity(),
                empty.implicits(),
                List.of(),
                Set.of(ItemState.Condition.CORRUPTED),
                empty.augmentSockets(),
                empty.catalystQuality(),
                List.of(),
                empty.equipmentQuality(),
                empty.observations())
            .withAugments(empty.installedAugments())
            .withQualityInfused(empty.qualityInfused());
    var result = new AbyssCraftingState(corrupt, slots, null);
    var cap = new ItemStateValidator(catalog).slots(empty);
    if (result.count(AffixType.PREFIX) > cap.maxPrefixes()
        || result.count(AffixType.SUFFIX) > cap.maxSuffixes())
      throw new IllegalArgumentException("Observed slots exceed item capacity");
    result.validate(catalog);
    for (var slot : slots)
      if (result.eligibleRevealIds(slot.id(), catalog).isEmpty())
        throw new IllegalArgumentException("No eligible reveal options for observed slots");
    return result;
  }

  public static AbyssCraftingState apply(
      ItemState item,
      String currency,
      AffixType side,
      String removedSlotId,
      Set<String> activeOmens,
      ItemCatalog catalog) {
    return apply(
        item, currency, side, removedSlotId, activeOmens, catalog, RandomGenerator.getDefault());
  }

  /** Uniform eligible removal, then uniform eligible side; never samples a reveal modifier. */
  public static AbyssCraftingState apply(
      ItemState item,
      String currency,
      AffixType side,
      String removedSlotId,
      Set<String> activeOmens,
      ItemCatalog catalog,
      RandomGenerator random) {
    Objects.requireNonNull(random);
    catalog = ManualRevealCatalog.enrich(catalog);
    if (side != null && removedSlotId != null)
      return applyEnriched(item, currency, side, removedSlotId, activeOmens, catalog);
    var before = AbyssCraftingState.from(item, catalog);
    var cap = new ItemStateValidator(catalog).slots(item);
    boolean full = before.slots().size() == cap.maxPrefixes() + cap.maxSuffixes();
    if (side != null && !full)
      return applyEnriched(item, currency, side, removedSlotId, activeOmens, catalog);
    List<String> removals = new ArrayList<>();
    if (removedSlotId != null && !removedSlotId.isBlank()) removals.add(removedSlotId);
    else if (full) before.removalCandidates(null).forEach(s -> removals.add(s.id()));
    else removals.add(null);
    var outcomes = new ArrayList<List<AbyssCraftingState>>();
    for (var removed : removals) {
      var availableSides = new ArrayList<AbyssCraftingState>();
      for (var candidateSide :
          side == null ? List.of(AffixType.PREFIX, AffixType.SUFFIX) : List.of(side)) {
        try {
          availableSides.add(
              applyEnriched(item, currency, candidateSide, removed, activeOmens, catalog));
        } catch (IllegalArgumentException unavailable) {
          // Probe complete validated outcomes: capacity, fractures, pool, level and omen
          // restrictions.
        }
      }
      if (!availableSides.isEmpty()) outcomes.add(availableSides);
    }
    if (outcomes.isEmpty()) throw new IllegalArgumentException("No eligible desecration outcome");
    var selectedRemoval = outcomes.get(random.nextInt(outcomes.size()));
    return selectedRemoval.get(random.nextInt(selectedRemoval.size()));
  }

  private static AbyssCraftingState applyEnriched(
      ItemState item,
      String currency,
      AffixType side,
      String removedSlotId,
      Set<String> activeOmens,
      ItemCatalog catalog) {
    if (currency == null || !POLICY.currencies().containsKey(currency))
      throw new IllegalArgumentException("Unsupported desecration currency");
    var omens = activeOmens == null ? Set.<String>of() : activeOmens;
    var sides =
        Map.of(
            "Omen_of_Sinistral_Necromancy", AffixType.PREFIX,
            "Omen_of_Dextral_Necromancy", AffixType.SUFFIX);
    var pools =
        Map.of(
            "Omen_of_the_Liege",
            "AMANAMU",
            "Omen_of_the_Blackblooded",
            "KURGAL",
            "Omen_of_the_Sovereign",
            "ULAMAN");
    var sideOmens = omens.stream().filter(sides::containsKey).toList();
    var poolOmens = omens.stream().filter(pools::containsKey).toList();
    if (sideOmens.size() > 1
        || poolOmens.size() > 1
        || omens.size() != sideOmens.size() + poolOmens.size())
      throw new IllegalArgumentException("Unsupported desecration omen combination");
    if (!sideOmens.isEmpty()) {
      var forced = sides.get(sideOmens.getFirst());
      if (side != null && side != forced)
        throw new IllegalArgumentException("Observed side conflicts with the Necromancy omen");
      side = forced;
    }
    if (item.rarity() != ItemState.Rarity.RARE
        || !item.conditions().isEmpty()
        || !item.desecrated().isEmpty())
      throw new IllegalArgumentException(
          "Requires an identified, unmodified-condition Rare item without desecration");
    var itemClass = BaseRegistry.itemClass(item.baseItemId());
    if (!supportsBase(currency, item.baseItemId()))
      throw new IllegalArgumentException("Currency does not match the equipment class");
    if (!poolOmens.isEmpty()
        && (revealPool(currency) != null
            || (!WEAPONS.contains(itemClass) && !JEWELLERY.contains(itemClass))))
      throw new IllegalArgumentException(
          "Lich omens require an ordinary weapon or jewellery desecration");
    String pool = poolOmens.isEmpty() ? revealPool(currency) : pools.get(poolOmens.getFirst());
    var levelReason = levelRefusal(currency, item.itemLevel());
    if (levelReason != null) throw new IllegalArgumentException(levelReason);
    if (side == null || side == AffixType.NONE)
      throw new IllegalArgumentException("Observed affix side required");
    var before = AbyssCraftingState.from(item, catalog);
    var capacity = new ItemStateValidator(catalog).slots(item);
    boolean full = before.slots().size() == capacity.maxPrefixes() + capacity.maxSuffixes();
    if (full != (removedSlotId != null && !removedSlotId.isBlank()))
      throw new IllegalArgumentException("Record the removed modifier only when the item is full");
    var remaining = full ? before.remove(removedSlotId, null, catalog) : before;
    var currentCapacity = new ItemStateValidator(catalog).slots(remaining.knownItem());
    if (remaining.count(side)
        >= (side == AffixType.PREFIX
            ? currentCapacity.maxPrefixes()
            : currentCapacity.maxSuffixes()))
      throw new IllegalArgumentException("Observed side exceeds current affix capacity");
    var slots = new ArrayList<>(remaining.slots());
    slots.add(
        new AbyssCraftingState.Slot(
            "desecrated-" + UUID.randomUUID(),
            side,
            null,
            true,
            minimumModifierLevel(currency),
            pool));
    var result = new AbyssCraftingState(remaining.item(), slots, null);
    result.validate(catalog);
    if (result.eligibleRevealIds(slots.getLast().id(), catalog).isEmpty())
      throw new IllegalArgumentException("No eligible reveal option for this item and currency");
    return result;
  }
}
