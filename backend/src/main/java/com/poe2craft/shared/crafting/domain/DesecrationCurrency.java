package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.BaseRegistry;
import com.poe2craft.shared.item.BasicJewel;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ManualRevealCatalog;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierDefinition.AffixType;
import java.util.*;
import java.util.random.RandomGenerator;

/**
 * 훼손 생성·제거를 앱 정책으로 실행하고 공개 후보 선택은 수동으로 유지한다.
 *
 * <p>기존 계약: Reveal choices remain manual. Automatic side/removal uses an explicit simulator policy,
 * not published game probabilities; explicit observed inputs remain supported.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code ARMOUR}: 방어구 ({@code Set<String>}).
 *   <li>{@code WEAPONS}: WEAPONS ({@code Set<String>}).
 *   <li>{@code JEWELLERY}: JEWELLERY ({@code Set<String>}).
 * </ul>
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

  private static final Set<String> ARMOUR =
      Set.of("Body Armours", "Helmets", "Gloves", "Boots", "Shields", "Bucklers", "Foci");
  private static final Set<String> WEAPONS =
      Set.of(
          "Bows",
          "Crossbows",
          "Wands",
          "Staves",
          "Sceptres",
          "Quarterstaves",
          "Spears",
          "One Hand Maces",
          "Two Hand Maces",
          "One Hand Axes",
          "Two Hand Axes",
          "One Hand Swords",
          "Two Hand Swords",
          "Daggers",
          "Claws",
          "Quivers",
          "Flails",
          "Traps",
          "Talismans");
  private static final Set<String> JEWELLERY = Set.of("Amulet", "Amulets", "Rings", "Belts");

  /** 훼손 재료와 공개 티어에 공통으로 적용하는 단계별 레벨 제한이다. */
  private static final int GNAWED_MAX_LEVEL = 64;

  private static final int ANCIENT_MIN_LEVEL = 40;

  /** 베이스별로 작업대가 받는 뼈의 이름이다. 옵션 티어의 획득처와 실제 적용이 이 목록을 공유한다. */
  private static List<String> bones(String base) {
    if (BasicJewel.supportedCrafting(base)) return List.of("Preserved_Cranium");
    var type = BaseRegistry.itemClass(base);
    if (type == null) return List.of();
    if (JEWELLERY.contains(type))
      return List.of(
          "Gnawed_Collarbone", "Preserved_Collarbone", "Ancient_Collarbone", "Altered_Collarbone");
    if (WEAPONS.contains(type))
      return List.of("Gnawed_Jawbone", "Preserved_Jawbone", "Ancient_Jawbone");
    if (ARMOUR.contains(type)) return List.of("Gnawed_Rib", "Preserved_Rib", "Ancient_Rib");
    return List.of();
  }

  /** 이 아이템 레벨·옵션 티어에서 공개 가능한 재료 ID다. inWeightedPool은 장착으로 열 수 있는 전용 풀을 포함한다. */
  public static List<String> sources(
      String base, int level, ModifierDefinition modifier, boolean inWeightedPool) {
    if (modifier.requiredItemLevel() > level) return List.of();
    return bones(base).stream()
        .filter(id -> !id.startsWith("Gnawed_") || level <= GNAWED_MAX_LEVEL)
        .filter(
            id -> !id.startsWith("Ancient_") || modifier.requiredItemLevel() >= ANCIENT_MIN_LEVEL)
        .filter(
            id ->
                AbyssCraftingState.revealPoolEligible(
                    id.equals("Altered_Collarbone") ? "ALTERED" : null, modifier, inWeightedPool))
        .toList();
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
    if (currency == null
        || !(currency.matches("(Gnawed|Preserved|Ancient)_(Rib|Jawbone|Collarbone)")
            || currency.equals("Preserved_Cranium")
            || currency.equals("Altered_Collarbone")))
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
    if (!bones(item.baseItemId()).contains(currency))
      throw new IllegalArgumentException("Currency does not match the equipment class");
    if (!poolOmens.isEmpty()
        && (currency.equals("Altered_Collarbone")
            || (!WEAPONS.contains(itemClass) && !JEWELLERY.contains(itemClass))))
      throw new IllegalArgumentException(
          "Lich omens require an ordinary weapon or jewellery desecration");
    String pool =
        currency.equals("Altered_Collarbone")
            ? "ALTERED"
            : poolOmens.isEmpty() ? null : pools.get(poolOmens.getFirst());
    if (currency.startsWith("Gnawed_") && item.itemLevel() > GNAWED_MAX_LEVEL)
      throw new IllegalArgumentException("Gnawed bones require item level 64 or lower");
    if (currency.startsWith("Ancient_") && item.itemLevel() < ANCIENT_MIN_LEVEL)
      throw new IllegalArgumentException(
          "No candidate can satisfy both item and minimum modifier levels");
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
            currency.startsWith("Ancient_") ? ANCIENT_MIN_LEVEL : null,
            pool));
    var result = new AbyssCraftingState(remaining.item(), slots, null);
    result.validate(catalog);
    if (result.eligibleRevealIds(slots.getLast().id(), catalog).isEmpty())
      throw new IllegalArgumentException("No eligible reveal option for this item and currency");
    return result;
  }
}
