package com.poe2craft.shared.crafting.application;

import com.poe2craft.shared.crafting.domain.AbyssCraftingPlan;
import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.crafting.domain.AbyssCurrencyExecutor;
import com.poe2craft.shared.crafting.domain.BodyEssenceTargets;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.HelmetEssenceTargets;
import com.poe2craft.shared.crafting.domain.QualityCurrencyRules;
import com.poe2craft.shared.crafting.domain.QualityLimitRules;
import com.poe2craft.shared.crafting.domain.RingEssenceTargets;
import com.poe2craft.shared.crafting.domain.StateBucket;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.BaseRegistry;
import com.poe2craft.shared.item.BasicJewel;
import com.poe2craft.shared.item.CatalystQualityDisplay;
import com.poe2craft.shared.item.EquipmentCurrencyCatalog;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ManualRevealCatalog;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SolarAmulet;
import java.util.*;
import java.util.random.RandomGenerator;

/**
 * 작업대와 제작 지원 API가 같은 베이스 카탈로그·시뮬레이터를 사용하는 진입점이다.
 *
 * <p>구성원: RegisteredBase는 catalog와 simulator 한 쌍이다. byKey는 화면용 베이스 키, byItemId는 아이템 원본 ID로 같은 등록
 * 객체를 찾는 인덱스다. 생성·등록 때만 변경하고 요청마다 아이템 상태를 보관하지 않는다. 생성자는 solar/stocky 및 기존 순서의 additional 카탈로그를
 * 받는다. additional 순서는 bow, wand, body, sceptre, belt, helmet, ring, sapphire, ruby, emerald,
 * diamond 다음 시간 잃은 주얼·soldier·imperial이다.
 *
 * <p>등록: register는 중복 키·ID와 카탈로그/시뮬레이터 불일치를 거부한다. registerBase는 장비 옵션을 보충하고 JSON의 에센스 대상 및 복합 수치
 * 정책으로 시뮬레이터를 만든다. registerReviewedBase는 BaseRegistry의 등록 정체성까지 검사하고, registerReviewedArmour는 이전
 * 호출자의 같은 등록 진입점을 유지한다. reviewedCatalogs는 원본 ID순 목록, catalog는 화면 키의 카탈로그를 반환한다.
 *
 * <p>요청: initial의 두 오버로드는 level과 수동 공개 여부로 초기 아이템·선택 가능 행동을 만든다. baseSimulator는 아이템 ID의 공용 시뮬레이터,
 * simulator는 증강물 문맥을 반영한 실행기를 선택한다. abyssCatalog는 snapshot도 검사한다. abyssPlan은 미공개 슬롯의 실행 계획,
 * applyAbyss와 applyAbyssCurrency는 해당 상태 전이 결과를 반환한다. apply는 일반 제작, actions는 사용 가능 여부,
 * qualityDisplay는 품질 한도·표시 결과다. 제작 메서드는 전달받은 random만 소비하며 원본 ItemState는 수정하지 않는다. 없는 베이스·잘못된 상태·중복
 * 등록은 IllegalArgumentException으로 거절한다.
 */
public final class CatalogCraftingService implements CraftingOperations {
  /** catalog는 공유 불변 옵션 목록, simulator는 그 베이스의 제작 규칙과 제한된 계산 캐시다. */
  private record RegisteredBase(ItemCatalog catalog, WorkbenchSimulator simulator) {}

  private final Map<String, RegisteredBase> byKey = new LinkedHashMap<>();
  private final Map<String, RegisteredBase> byItemId = new LinkedHashMap<>();

  private void register(String key, ItemCatalog catalog, WorkbenchSimulator simulator) {
    if (catalog == null) return;
    if (simulator == null
        || !simulator.catalog().base().id().equals(catalog.base().id())
        || byKey.containsKey(key)
        || byItemId.containsKey(catalog.base().id()))
      throw new IllegalArgumentException(
          "Duplicate or inconsistent Workbench base registration: " + key);
    var entry = new RegisteredBase(catalog, simulator);
    byKey.put(key, entry);
    byItemId.put(catalog.base().id(), entry);
  }

  public List<ItemCatalog> reviewedCatalogs() {
    return byItemId.values().stream()
        .map(RegisteredBase::catalog)
        .sorted(Comparator.comparing(c -> c.base().id()))
        .toList();
  }

  public CatalogCraftingService(
      ItemCatalog solar,
      WorkbenchSimulator solarSimulator,
      ItemCatalog stocky,
      ItemCatalog... additional) {
    solar = EquipmentCurrencyCatalog.enrich(Objects.requireNonNull(solar));
    register("solar", solar, solarSimulator.forCatalog(solar));
    registerBase("stocky", Objects.requireNonNull(stocky), true);
    String[] names = {
      "bow",
      "wand",
      "body",
      "sceptre",
      "belt",
      "helmet",
      "ring",
      "sapphire",
      "ruby",
      "emerald",
      "diamond"
    };
    for (int index = 0; index < additional.length; index++) {
      var catalog = additional[index];
      if (catalog == null) continue;
      if (index < names.length) {
        String key = names[index];
        registerBase(key, catalog, index < 7);
      } else if (BodyEssenceTargets.supports(catalog.base().id())
          || HelmetEssenceTargets.supports(catalog.base().id())) {
        registerBase(
            BodyEssenceTargets.supports(catalog.base().id()) ? "soldier" : "imperial",
            catalog,
            true);
      } else {
        String key =
            "time-lost-"
                + catalog.base().name().substring("Time-Lost ".length()).toLowerCase(Locale.ROOT);
        registerBase(key, EquipmentCurrencyCatalog.enrich(catalog), false);
      }
    }
  }

  private void registerBase(String key, ItemCatalog catalog, boolean equipment) {
    if (equipment) catalog = EquipmentCurrencyCatalog.enrich(catalog);
    var targets =
        com.poe2craft.shared.crafting.domain.WorkbenchDefinitions.active()
            .targets(catalog.base().id());
    var coupled =
        equipment
            ? catalog.modifiers().values().stream()
                .filter(
                    d ->
                        d.stats().size() > 1
                            && (!key.equals("ring")
                                || d.layer() == ModifierDefinition.Layer.EXPLICIT))
                .map(ModifierDefinition::id)
                .collect(java.util.stream.Collectors.toSet())
            : Set.<String>of();
    var simulator =
        new WorkbenchSimulator(
            catalog, new CraftingEngine(catalog), coupled, targets.fixed(), targets.replacements());
    register(key, catalog, simulator);
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
                        : (base.equals("belt")
                                    || (BaseRegistry.supports(catalog.base().id())
                                        && BaseRegistry.policy(catalog.base().id())
                                            .initializeImplicit()))
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
    var catalog = simulator(state).catalog();
    if (!catalog.metadata().snapshotId().equals(state.snapshotId()))
      throw new IllegalArgumentException("Unsupported item snapshot");
    return catalog;
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
    var entry = byKey.get(base);
    if (entry == null) throw new IllegalArgumentException("Unsupported Workbench base: " + base);
    return entry.catalog();
  }

  public WorkbenchSimulator simulator(ItemState state) {
    return baseSimulator(state).forItem(state);
  }

  private WorkbenchSimulator baseSimulator(ItemState state) {
    if (state == null) throw new IllegalArgumentException("State required");
    var entry = byItemId.get(state.baseItemId());
    if (entry == null) throw new IllegalArgumentException("Unsupported Workbench base");
    return entry.simulator();
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
    catalog = EquipmentCurrencyCatalog.enrich(catalog);
    if (!BaseRegistry.require(key).id().equals(catalog.base().id()) || byKey.containsKey(key))
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
    register(key, catalog, simulator);
  }

  public WorkbenchSimulator.Result apply(
      ItemState state, WorkbenchCurrency action, Set<String> omens, RandomGenerator random) {
    return simulator(state).apply(state, action, omens, random);
  }

  public List<WorkbenchSimulator.Availability> actions(ItemState state, Set<String> omens) {
    return simulator(state).actions(state, omens);
  }

  public QualityDisplay qualityDisplay(ItemState state) {
    var selected = baseSimulator(state).catalog();
    return new QualityDisplay(
        CatalystQualityDisplay.VERSION,
        state,
        QualityLimitRules.describe(state, selected),
        CatalystQualityDisplay.describe(state, selected));
  }
}
