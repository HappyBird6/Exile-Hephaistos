package com.poe2craft.shared.crafting.application;

import com.poe2craft.shared.crafting.domain.AbyssCraftingPlan;
import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.crafting.domain.AbyssCurrencyExecutor;
import com.poe2craft.shared.crafting.domain.BeltEssenceTargets;
import com.poe2craft.shared.crafting.domain.BodyEssenceTargets;
import com.poe2craft.shared.crafting.domain.BowEssenceTargets;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.HelmetEssenceTargets;
import com.poe2craft.shared.crafting.domain.QualityCurrencyRules;
import com.poe2craft.shared.crafting.domain.QualityLimitRules;
import com.poe2craft.shared.crafting.domain.RingEssenceTargets;
import com.poe2craft.shared.crafting.domain.SceptreEssenceTargets;
import com.poe2craft.shared.crafting.domain.StateBucket;
import com.poe2craft.shared.crafting.domain.StockyEssenceTargets;
import com.poe2craft.shared.crafting.domain.WandEssenceTargets;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.BaseRegistry;
import com.poe2craft.shared.item.BasicJewel;
import com.poe2craft.shared.item.CatalystQualityDisplay;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ManualRevealCatalog;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SapphireJewel;
import com.poe2craft.shared.item.SolarAmulet;
import java.util.*;
import java.util.random.RandomGenerator;

/** Shared reviewed catalogs and crafting rules for the workbench and crafting support. */
public final class CatalogCraftingService implements CraftingOperations {
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

  /** The same immutable catalogs used by starting items, including registered equipment. */
  public List<ItemCatalog> reviewedCatalogs() {
    var catalogs = new TreeMap<String, ItemCatalog>();
    java.util.stream.Stream.of(
            solar, stocky, bow, wand, body, sceptre, belt, helmet, ring, sapphire)
        .filter(Objects::nonNull)
        .forEach(c -> catalogs.put(c.base().id(), c));
    basicJewels.values().forEach(c -> catalogs.put(c.base().id(), c));
    topBases.values().forEach(c -> catalogs.put(c.base().id(), c));
    return List.copyOf(catalogs.values());
  }

  public CatalogCraftingService(
      ItemCatalog solar, WorkbenchSimulator solarSimulator, ItemCatalog stocky) {
    this(solar, solarSimulator, stocky, null);
  }

  public CatalogCraftingService(
      ItemCatalog solar, WorkbenchSimulator solarSimulator, ItemCatalog stocky, ItemCatalog bow) {
    this(solar, solarSimulator, stocky, bow, null);
  }

  public CatalogCraftingService(
      ItemCatalog solar,
      WorkbenchSimulator solarSimulator,
      ItemCatalog stocky,
      ItemCatalog bow,
      ItemCatalog wand) {
    this(solar, solarSimulator, stocky, bow, wand, null);
  }

  public CatalogCraftingService(
      ItemCatalog solar,
      WorkbenchSimulator solarSimulator,
      ItemCatalog stocky,
      ItemCatalog bow,
      ItemCatalog wand,
      ItemCatalog body) {
    this(solar, solarSimulator, stocky, bow, wand, body, null);
  }

  public CatalogCraftingService(
      ItemCatalog solar,
      WorkbenchSimulator solarSimulator,
      ItemCatalog stocky,
      ItemCatalog bow,
      ItemCatalog wand,
      ItemCatalog body,
      ItemCatalog sceptre) {
    this(solar, solarSimulator, stocky, bow, wand, body, sceptre, null);
  }

  public CatalogCraftingService(
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

  public CatalogCraftingService(
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

  public CatalogCraftingService(
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

  public CatalogCraftingService(
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

  public CatalogCraftingService(
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

  public CatalogCraftingService(
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
    return initial(base, level, false);
  }

  public Initial initial(String base, int level, boolean manualReveal) {
    var catalog = catalog(base);
    var displayCatalog = manualReveal ? ManualRevealCatalog.enrich(catalog) : catalog;
    var state =
        base.equals("solar")
            ? SolarAmulet.initial(catalog, level, 15)
            : new ItemState(
                catalog.metadata().snapshotId(),
                catalog.base().id(),
                level,
                BaseRegistry.rareOnly(catalog.base().id())
                    ? ItemState.Rarity.RARE
                    : base.equals("sapphire") ? ItemState.Rarity.MAGIC : ItemState.Rarity.NORMAL,
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
                        : (BaseRegistry.supports(catalog.base().id())
                                    && BaseRegistry.policy(catalog.base().id())
                                        .initializeImplicit())
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
        displayCatalog.metadata(),
        bucket.id(),
        bucket,
        displayCatalog.modifiers(),
        engine.actions(bucket),
        catalog.compatibleSnapshotIds(),
        QualityLimitRules.describe(state, catalog),
        BaseRegistry.socketExecutionMaximum(catalog.base().id()) != null ? 0 : null,
        catalog.base(),
        QualityCurrencyRules.equipment(state.baseItemId()) ? 0 : null,
        simulator(state).baseMaterialActions(),
        simulator(state).compatibilityId());
  }

  public ItemCatalog abyssCatalog(ItemState state) {
    return reviewedCatalogs().stream()
        .filter(
            c ->
                c.base().id().equals(state.baseItemId())
                    && c.metadata().snapshotId().equals(state.snapshotId()))
        .findFirst()
        .map(ManualRevealCatalog::enrich)
        .orElseThrow(() -> new IllegalArgumentException("Unsupported Abyss catalog"));
  }

  public AbyssCraftingPlan abyssPlan(
      AbyssCraftingState state, WorkbenchCurrency action, Set<String> omens) {
    return simulator(state.item()).abyssPlan(state, action, omens);
  }

  public AbyssCraftingState applyAbyss(
      AbyssCraftingState state,
      WorkbenchCurrency action,
      Set<String> omens,
      RandomGenerator random) {
    return simulator(state.item()).applyAbyss(state, action, omens, random);
  }

  public AbyssCurrencyExecutor.Result applyAbyssCurrency(
      AbyssCraftingState state,
      WorkbenchCurrency action,
      Set<String> omens,
      RandomGenerator random) {
    return new AbyssCurrencyExecutor(abyssCatalog(state.item()), simulator(state.item()))
        .apply(state, action, omens, random);
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

  public WorkbenchSimulator simulator(ItemState state) {
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
    registerReviewedBase(key, catalog, fixed, replacements);
  }

  public void registerReviewedBase(
      String key,
      ItemCatalog catalog,
      Map<WorkbenchCurrency, List<String>> fixed,
      Map<WorkbenchCurrency, List<String>> replacements) {
    if (!BaseRegistry.require(key).id().equals(catalog.base().id()) || topBases.containsKey(key))
      throw new IllegalArgumentException("Unreviewed or duplicate base registration");
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
          CatalystQualityDisplay.VERSION,
          state,
          QualityLimitRules.describe(state, selected),
          CatalystQualityDisplay.describe(state, selected));
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
        CatalystQualityDisplay.VERSION,
        state,
        QualityLimitRules.describe(state, selected),
        CatalystQualityDisplay.describe(state, selected));
  }
}
