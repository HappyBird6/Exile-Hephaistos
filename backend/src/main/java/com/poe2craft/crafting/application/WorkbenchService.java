package com.poe2craft.crafting.application;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import java.util.*;
import java.util.random.RandomGenerator;

/** Workbench-only catalog dispatch. Support and Explorer retain their Solar catalog. */
public final class WorkbenchService {
  public static final String STOCKY_BASE_ID = "Metadata/Items/Armours/Gloves/FourGlovesStr1";
  private final ItemCatalog solar;
  private final ItemCatalog stocky;
  private final WorkbenchSimulator solarSimulator;
  private final WorkbenchSimulator stockySimulator;
  private final ItemCatalog wand;
  private final ItemCatalog ring;
  private final WorkbenchSimulator ringSimulator;
  private final ItemCatalog helmet;
  private final WorkbenchSimulator helmetSimulator;
  private final ItemCatalog belt;
  private final WorkbenchSimulator beltSimulator;
  private final ItemCatalog sceptre;
  private final WorkbenchSimulator sceptreSimulator;
  private final ItemCatalog body;
  private final WorkbenchSimulator bodySimulator;
  private final WorkbenchSimulator wandSimulator;
  private final ItemCatalog bow;
  private final WorkbenchSimulator bowSimulator;
  private final Map<String, ItemCatalog> basicJewels = new HashMap<>();
  private final Map<String, WorkbenchSimulator> basicJewelSimulators = new HashMap<>();
  private final Map<String, ItemCatalog> topBases = new HashMap<>();
  private final Map<String, WorkbenchSimulator> topBaseSimulators = new HashMap<>();
  private final ItemCatalog sapphire;
  private final WorkbenchSimulator sapphireSimulator;

  public WorkbenchService(
      ItemCatalog solar, WorkbenchSimulator solarSimulator, ItemCatalog stocky) {
    this(solar, solarSimulator, stocky, null);
  }

  public WorkbenchService(
      ItemCatalog solar, WorkbenchSimulator solarSimulator, ItemCatalog stocky, ItemCatalog bow) {
    this(solar, solarSimulator, stocky, bow, null);
  }

  public WorkbenchService(
      ItemCatalog solar,
      WorkbenchSimulator solarSimulator,
      ItemCatalog stocky,
      ItemCatalog bow,
      ItemCatalog wand) {
    this(solar, solarSimulator, stocky, bow, wand, null);
  }

  public WorkbenchService(
      ItemCatalog solar,
      WorkbenchSimulator solarSimulator,
      ItemCatalog stocky,
      ItemCatalog bow,
      ItemCatalog wand,
      ItemCatalog body) {
    this(solar, solarSimulator, stocky, bow, wand, body, null);
  }

  public WorkbenchService(
      ItemCatalog solar,
      WorkbenchSimulator solarSimulator,
      ItemCatalog stocky,
      ItemCatalog bow,
      ItemCatalog wand,
      ItemCatalog body,
      ItemCatalog sceptre) {
    this(solar, solarSimulator, stocky, bow, wand, body, sceptre, null);
  }

  public WorkbenchService(
      ItemCatalog solar,
      WorkbenchSimulator solarSimulator,
      ItemCatalog stocky,
      ItemCatalog bow,
      ItemCatalog wand,
      ItemCatalog body,
      ItemCatalog sceptre,
      ItemCatalog belt) {
    this(solar, solarSimulator, stocky, bow, wand, body, sceptre, belt, null);
  }

  public WorkbenchService(
      ItemCatalog solar,
      WorkbenchSimulator solarSimulator,
      ItemCatalog stocky,
      ItemCatalog bow,
      ItemCatalog wand,
      ItemCatalog body,
      ItemCatalog sceptre,
      ItemCatalog belt,
      ItemCatalog helmet) {
    this(solar, solarSimulator, stocky, bow, wand, body, sceptre, belt, helmet, null);
  }

  public WorkbenchService(
      ItemCatalog solar,
      WorkbenchSimulator solarSimulator,
      ItemCatalog stocky,
      ItemCatalog bow,
      ItemCatalog wand,
      ItemCatalog body,
      ItemCatalog sceptre,
      ItemCatalog belt,
      ItemCatalog helmet,
      ItemCatalog ring) {
    this(solar, solarSimulator, stocky, bow, wand, body, sceptre, belt, helmet, ring, null);
  }

  public WorkbenchService(
      ItemCatalog solar,
      WorkbenchSimulator solarSimulator,
      ItemCatalog stocky,
      ItemCatalog bow,
      ItemCatalog wand,
      ItemCatalog body,
      ItemCatalog sceptre,
      ItemCatalog belt,
      ItemCatalog helmet,
      ItemCatalog ring,
      ItemCatalog sapphire) {
    this(
        solar,
        solarSimulator,
        stocky,
        bow,
        wand,
        body,
        sceptre,
        belt,
        helmet,
        ring,
        sapphire,
        null,
        null,
        null);
  }

  public WorkbenchService(
      ItemCatalog solar,
      WorkbenchSimulator solarSimulator,
      ItemCatalog stocky,
      ItemCatalog bow,
      ItemCatalog wand,
      ItemCatalog body,
      ItemCatalog sceptre,
      ItemCatalog belt,
      ItemCatalog helmet,
      ItemCatalog ring,
      ItemCatalog sapphire,
      ItemCatalog ruby,
      ItemCatalog emerald,
      ItemCatalog diamond) {
    this(
        solar,
        solarSimulator,
        stocky,
        bow,
        wand,
        body,
        sceptre,
        belt,
        helmet,
        ring,
        sapphire,
        ruby,
        emerald,
        diamond,
        new ItemCatalog[0]);
  }

  public WorkbenchService(
      ItemCatalog solar,
      WorkbenchSimulator solarSimulator,
      ItemCatalog stocky,
      ItemCatalog bow,
      ItemCatalog wand,
      ItemCatalog body,
      ItemCatalog sceptre,
      ItemCatalog belt,
      ItemCatalog helmet,
      ItemCatalog ring,
      ItemCatalog sapphire,
      ItemCatalog ruby,
      ItemCatalog emerald,
      ItemCatalog diamond,
      ItemCatalog... timeLost) {
    var extras = new ItemCatalog[] {ruby, emerald, diamond};
    var names = new String[] {"ruby", "emerald", "diamond"};
    for (int i = 0; i < extras.length; i++)
      if (extras[i] != null) {
        var c = extras[i];
        basicJewels.put(names[i], c);
        basicJewelSimulators.put(c.base().id(), new WorkbenchSimulator(c, new CraftingEngine(c)));
      }
    for (var c : timeLost) {
      if (BodyEssenceTargets.supports(c.base().id())
          || HelmetEssenceTargets.supports(c.base().id())) {
        boolean isBody = BodyEssenceTargets.supports(c.base().id());
        topBases.put(isBody ? "soldier" : "imperial", c);
        topBaseSimulators.put(
            c.base().id(),
            new WorkbenchSimulator(
                c,
                new CraftingEngine(c),
                c.modifiers().values().stream()
                    .filter(d -> d.stats().size() > 1)
                    .map(ModifierDefinition::id)
                    .collect(java.util.stream.Collectors.toSet()),
                Map.of(),
                isBody ? BodyEssenceTargets.REPLACEMENTS : Map.of()));
        continue;
      }
      String name =
          "time-lost-"
              + c.base().name().substring("Time-Lost ".length()).toLowerCase(java.util.Locale.ROOT);
      basicJewels.put(name, c);
      basicJewelSimulators.put(c.base().id(), new WorkbenchSimulator(c, new CraftingEngine(c)));
    }
    this.sapphire = sapphire;
    this.sapphireSimulator =
        sapphire == null ? null : new WorkbenchSimulator(sapphire, new CraftingEngine(sapphire));
    this.ring = ring;
    this.ringSimulator =
        ring == null
            ? null
            : new WorkbenchSimulator(
                ring,
                new CraftingEngine(ring),
                ring.modifiers().values().stream()
                    .filter(
                        d -> d.layer() == ModifierDefinition.Layer.EXPLICIT && d.stats().size() > 1)
                    .map(ModifierDefinition::id)
                    .collect(java.util.stream.Collectors.toSet()),
                Map.of());
    this.helmet = helmet;
    this.helmetSimulator =
        helmet == null
            ? null
            : new WorkbenchSimulator(
                helmet,
                new CraftingEngine(helmet),
                helmet.modifiers().values().stream()
                    .filter(d -> d.stats().size() > 1)
                    .map(ModifierDefinition::id)
                    .collect(java.util.stream.Collectors.toSet()),
                Map.of());
    this.belt = belt;
    this.beltSimulator =
        belt == null
            ? null
            : new WorkbenchSimulator(
                belt,
                new CraftingEngine(belt),
                belt.modifiers().values().stream()
                    .filter(d -> d.stats().size() > 1)
                    .map(ModifierDefinition::id)
                    .collect(java.util.stream.Collectors.toSet()),
                Map.of());
    this.sceptre = sceptre;
    this.sceptreSimulator =
        sceptre == null
            ? null
            : new WorkbenchSimulator(
                sceptre,
                new CraftingEngine(sceptre),
                sceptre.modifiers().values().stream()
                    .filter(d -> d.stats().size() > 1)
                    .map(ModifierDefinition::id)
                    .collect(java.util.stream.Collectors.toSet()),
                SceptreEssenceTargets.VERIFIED);
    this.body = body;
    this.bodySimulator =
        body == null
            ? null
            : new WorkbenchSimulator(
                body,
                new CraftingEngine(body),
                body.modifiers().values().stream()
                    .filter(d -> d.stats().size() > 1)
                    .map(ModifierDefinition::id)
                    .collect(java.util.stream.Collectors.toSet()),
                Map.of(),
                BodyEssenceTargets.REPLACEMENTS);
    this.wand = wand;
    this.wandSimulator =
        wand == null
            ? null
            : new WorkbenchSimulator(
                wand,
                new CraftingEngine(wand),
                wand.modifiers().values().stream()
                    .filter(d -> d.stats().size() > 1)
                    .map(ModifierDefinition::id)
                    .collect(java.util.stream.Collectors.toSet()),
                WandEssenceTargets.VERIFIED);
    this.bow = bow;
    this.bowSimulator =
        bow == null
            ? null
            : new WorkbenchSimulator(
                bow,
                new CraftingEngine(bow),
                bow.modifiers().values().stream()
                    .filter(d -> d.stats().size() > 1)
                    .map(ModifierDefinition::id)
                    .collect(java.util.stream.Collectors.toSet()),
                BowEssenceTargets.VERIFIED);
    this.solar = solar;
    this.solarSimulator = solarSimulator;
    this.stocky = stocky;
    var coupled =
        stocky.modifiers().values().stream()
            .filter(d -> d.stats().size() > 1)
            .map(ModifierDefinition::id)
            .collect(java.util.stream.Collectors.toSet());
    stockySimulator =
        new WorkbenchSimulator(
            stocky,
            new CraftingEngine(stocky),
            coupled,
            StockyEssenceTargets.VERIFIED,
            StockyEssenceTargets.REPLACEMENTS);
  }

  public Initial initial(String base, int level) {
    var catalog = catalog(base);
    var state =
        base.equals("solar")
            ? SolarAmulet.initial(catalog, level, 15)
            : new ItemState(
                catalog.metadata().snapshotId(),
                catalog.base().id(),
                level,
                base.equals("sapphire") ? ItemState.Rarity.MAGIC : ItemState.Rarity.NORMAL,
                BasicJewel.timeLost(catalog.base().id())
                    ? List.of(
                        new ModifierInstance(
                            catalog.base().implicitModifierId(),
                            Map.of("local_jewel_effect_base_radius", 1000L)))
                    : base.equals("ring")
                        ? List.of(
                            new ModifierInstance(
                                RingEssenceTargets.IMPLICIT_ID,
                                Map.of(
                                    "attack_minimum_added_physical_damage",
                                    1L,
                                    "attack_maximum_added_physical_damage",
                                    4L)))
                        : (ReviewedBows.supports(catalog.base().id())
                                    || ReviewedRings.supports(catalog.base().id())
                                    || ReviewedAmulets.supports(catalog.base().id()))
                                && catalog.base().hasImplicit()
                            ? List.of(
                                new ModifierInstance(
                                    catalog.base().implicitModifierId(),
                                    catalog
                                        .find(catalog.base().implicitModifierId())
                                        .orElseThrow()
                                        .stats()
                                        .stream()
                                        .collect(
                                            java.util.stream.Collectors.toMap(
                                                ModifierDefinition.StatRange::id,
                                                stat -> stat.max()))))
                            : List.of(),
                List.of(),
                Set.of());
    var bucket = StateBucket.from(state);
    var engine = new CraftingEngine(catalog);
    engine.validate(bucket);
    return new Initial(
        simulator(state).ruleVersion(),
        catalog.metadata(),
        bucket.id(),
        bucket,
        catalog.modifiers(),
        engine.actions(bucket),
        catalog.compatibleSnapshotIds(),
        QualityLimitRules.describe(state, catalog),
        base.equals("stocky") ? 0 : null);
  }

  private ItemCatalog catalog(String base) {
    if (topBases.containsKey(base)) return topBases.get(base);
    if (basicJewels.containsKey(base)) return basicJewels.get(base);
    return switch (base) {
      case "sapphire" -> {
        if (sapphire == null) throw new IllegalArgumentException("Sapphire catalog unavailable");
        yield sapphire;
      }
      case "ring" -> {
        if (ring == null) throw new IllegalArgumentException("Ring catalog unavailable");
        yield ring;
      }
      case "helmet" -> {
        if (helmet == null) throw new IllegalArgumentException("Helmet catalog unavailable");
        yield helmet;
      }
      case "belt" -> {
        if (belt == null) throw new IllegalArgumentException("Belt catalog unavailable");
        yield belt;
      }
      case "solar" -> solar;
      case "stocky" -> stocky;
      case "sceptre" -> {
        if (sceptre == null) throw new IllegalArgumentException("Sceptre catalog unavailable");
        yield sceptre;
      }
      case "body" -> {
        if (body == null) throw new IllegalArgumentException("Body Armour catalog unavailable");
        yield body;
      }
      case "wand" -> {
        if (wand == null) throw new IllegalArgumentException("Wand catalog unavailable");
        yield wand;
      }
      case "bow" -> {
        if (bow == null) throw new IllegalArgumentException("Bow catalog unavailable");
        yield bow;
      }
      default -> throw new IllegalArgumentException("Unsupported Workbench base");
    };
  }

  private WorkbenchSimulator simulator(ItemState state) {
    if (state != null && topBaseSimulators.containsKey(state.baseItemId()))
      return topBaseSimulators.get(state.baseItemId());
    if (state != null && basicJewelSimulators.containsKey(state.baseItemId()))
      return basicJewelSimulators.get(state.baseItemId());
    if (state != null
        && state.baseItemId().equals(SapphireJewel.BASE_ID)
        && sapphireSimulator != null) return sapphireSimulator;
    if (state != null
        && state.baseItemId().equals(RingEssenceTargets.BASE_ID)
        && ringSimulator != null) return ringSimulator;
    if (state != null
        && state.baseItemId().equals(HelmetEssenceTargets.BASE_ID)
        && helmetSimulator != null) return helmetSimulator;
    if (state != null
        && state.baseItemId().equals(BeltEssenceTargets.BASE_ID)
        && beltSimulator != null) return beltSimulator;
    if (state == null) throw new IllegalArgumentException("State required");
    if (state.baseItemId().equals(SolarAmulet.BASE_ID)) return solarSimulator;
    if (state.baseItemId().equals(STOCKY_BASE_ID)) return stockySimulator;
    if (state.baseItemId().equals(SceptreEssenceTargets.BASE_ID) && sceptreSimulator != null)
      return sceptreSimulator;
    if (state.baseItemId().equals(BodyEssenceTargets.BASE_ID) && bodySimulator != null)
      return bodySimulator;
    if (state.baseItemId().equals(WandEssenceTargets.BASE_ID) && wandSimulator != null)
      return wandSimulator;
    if (state.baseItemId().equals(BowEssenceTargets.BASE_ID) && bowSimulator != null)
      return bowSimulator;
    throw new IllegalArgumentException("Unsupported Workbench base");
  }

  /**
   * Bootstrap registration uses source-reviewed catalog identities and per-class essence targets.
   */
  public void registerReviewedArmour(
      String key,
      ItemCatalog catalog,
      Map<WorkbenchCurrency, List<String>> fixed,
      Map<WorkbenchCurrency, List<String>> replacements) {
    if (!catalog
            .base()
            .id()
            .equals(
                ReviewedGloves.BASES.getOrDefault(
                    key,
                    ReviewedHelmets.BASES.getOrDefault(
                        key,
                        ReviewedBodies.BASES.getOrDefault(
                            key,
                            ReviewedBoots.BASES.getOrDefault(
                                key,
                                ReviewedBows.BASES.getOrDefault(
                                    key,
                                    ReviewedRings.BASES.getOrDefault(
                                        key, ReviewedAmulets.BASES.get(key))))))))
        || topBases.containsKey(key))
      throw new IllegalArgumentException("Unreviewed or duplicate armour registration");
    var simulator =
        new WorkbenchSimulator(
            catalog,
            new CraftingEngine(catalog),
            catalog.modifiers().values().stream()
                .filter(d -> d.stats().size() > 1)
                .map(ModifierDefinition::id)
                .collect(java.util.stream.Collectors.toSet()),
            fixed,
            replacements);
    topBases.put(key, catalog);
    topBaseSimulators.put(catalog.base().id(), simulator);
  }

  public WorkbenchSimulator.Result apply(
      ItemState state, WorkbenchCurrency action, Set<String> omens, RandomGenerator random) {
    return simulator(state).apply(state, action, omens, random);
  }

  public List<WorkbenchSimulator.Availability> actions(ItemState state, Set<String> omens) {
    return simulator(state).actions(state, omens);
  }

  public QualityDisplay qualityDisplay(ItemState state) {
    if (topBaseSimulators.containsKey(state.baseItemId())) {
      var selected =
          topBases.values().stream()
              .filter(c -> c.base().id().equals(state.baseItemId()))
              .findFirst()
              .orElseThrow();
      return new QualityDisplay(
          com.poe2craft.item.CatalystQualityDisplay.VERSION,
          state,
          QualityLimitRules.describe(state, selected),
          com.poe2craft.item.CatalystQualityDisplay.describe(state, selected));
    }
    var selected =
        java.util.stream.Stream.concat(
                basicJewels.values().stream(),
                java.util.stream.Stream.of(
                    solar, stocky, bow, wand, body, sceptre, belt, helmet, ring, sapphire))
            .filter(java.util.Objects::nonNull)
            .filter(c -> c.base().id().equals(state.baseItemId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported base"));
    return new QualityDisplay(
        com.poe2craft.item.CatalystQualityDisplay.VERSION,
        state,
        QualityLimitRules.describe(state, selected),
        com.poe2craft.item.CatalystQualityDisplay.describe(state, selected));
  }

  public record QualityDisplay(
      String ruleVersion,
      ItemState state,
      QualityLimitRules.Limit qualityLimit,
      List<com.poe2craft.item.CatalystQualityDisplay.Projection> modifiers) {}

  public record Initial(
      String ruleVersion,
      ItemCatalog.Metadata metadata,
      String id,
      StateBucket state,
      Map<String, ModifierDefinition> modifiers,
      List<CraftingEngine.Availability> actions,
      List<String> compatibleSnapshotIds,
      QualityLimitRules.Limit qualityLimit,
      Integer augmentSockets) {}
}
