package com.poe2craft.shared.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.ruleset.SealedResources;
import java.util.*;

/**
 * 장착한 증강물 ID에 따라 제작 한도·옵션 풀·수치 보정을 계산한다. 패치 가능한 값은 augment-crafting JSON에서 읽는다.
 *
 * <p>장착한 증강물 ID에 따라 제작 한도·옵션 풀·수치 보정을 계산한다. 패치 가능한 값은 augment-crafting JSON에서 읽는다.
 *
 * <p>private 생성자는 생성을 막는다. DATA는 load로 읽은 문서, NONE은 효과가 없는 기본값이다. load는 문서 버전·기본 한도·티어 가중치·증강물 옵션의
 * zero-spawn 조건을 검사하며 오류 시 초기화를 중단한다. effect(id)는 없으면 NONE, socketBound(id)는 제거 금지 여부다.
 * poolWeightPolicy/magnitudeRules는 공통 풀 가중치와 수치 보정 정의, baseCraftedLimit는 JSON의 기본 제작 옵션 수다.
 *
 * <p>suffixBonus/craftedLimit는 장착 효과를 합산하고 maximumQuality는 가장 높은 한도를 선택한다. crafted는 인스턴스 표식·등록
 * ID·태그로 제작 옵션인지 판정한다. pools는 중복 없는 활성 풀 ID, affectsSampling은 접미 한도나 풀이 바뀌는지 반환한다. 입력 상태는 변경하지 않는다.
 * enrich는 베이스에서 가능한 옵션을 가중치 0으로 추가한다. forItem은 활성 풀의 가중치와 접미 한도를 반영한 문맥 카탈로그를 만들고 이미 적용된 문맥에는 중복
 * 적용하지 않는다. rebuilt는 옵션 개수·가중치 합계를 다시 계산하되 snapshot과 호환 목록은 유지한다. forItem의 selected는 활성 옵션별 임시 가중치,
 * expanded는 늘어난 슬롯 한도다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code NONE}: NONE ({@code Effect}).
 *   <li>{@code DATA}: 데이터 ({@code Document}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AugmentCraftingRules()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code load()}: 검증된 자료를 로딩한다. 반환: {@code Document}. 사용하는 예외: {@code
 *       IllegalArgumentException}, {@code ExceptionInInitializerError}.
 *   <li>{@code poolWeightPolicy()}: 후보풀·가중치·정책 값을 조회·구성한다. 반환: {@code PoolWeightPolicy}.
 *   <li>{@code magnitudeRules()}: magnitude·규칙목록 값을 조회·구성한다. 반환: {@code
 *       List<ModifierMagnitudes.Rule>}.
 *   <li>{@code effect(id)}: 효과 처리에서 {@code DATA.effects}을(를) 사용한다. 입력: {@code id}(식별자). 반환: {@code
 *       Effect}.
 *   <li>{@code socketBound(id)}: 장착홈·경계값 처리에서 {@code DATA.socketBound}을(를) 사용한다. 입력: {@code
 *       id}(식별자). 반환: {@code boolean}.
 *   <li>{@code suffixBonus(state)}: 접미·Bonus 처리에서 {@code state.installedAugments}, {@code
 *       a.augmentId}을(를) 사용한다. 입력: {@code state}(상태). 반환: {@code int}.
 *   <li>{@code baseCraftedLimit()}: 베이스·제작옵션·한도 값을 조회·구성한다. 반환: {@code int}.
 *   <li>{@code craftedLimit(state)}: 제작옵션·한도 처리에서 {@code DATA.baseCraftedLimit}, {@code
 *       state.installedAugments}을(를) 사용한다. 입력: {@code state}(상태). 반환: {@code int}.
 *   <li>{@code crafted(modifier, catalog)}: 제작옵션 처리에서 {@code modifier.crafted}, {@code
 *       DATA.craftedModifierIds}을(를) 사용한다. 입력: {@code modifier}(옵션), {@code catalog}(카탈로그). 반환:
 *       {@code boolean}.
 *   <li>{@code maximumQuality(state)}: 최대값·품질 처리에서 {@code state.installedAugments}, {@code
 *       a.augmentId}을(를) 사용한다. 입력: {@code state}(상태). 반환: {@code int}.
 *   <li>{@code pools(state)}: 후보풀목록 처리에서 {@code state.installedAugments}, {@code
 *       augment.augmentId}을(를) 사용한다. 입력: {@code state}(상태). 반환: {@code Set<String>}.
 *   <li>{@code affectsSampling(state)}: affects·Sampling의 입력에 따른 분기·결과를 처리한다. 입력: {@code
 *       state}(상태). 반환: {@code boolean}.
 *   <li>{@code enrich(catalog)}: enrich 처리에서 {@code DATA.pools}, {@code catalog.base}을(를) 사용한다. 입력:
 *       {@code catalog}(카탈로그). 반환: {@code ItemCatalog}.
 *   <li>{@code forItem(catalog, state)}: 대상·아이템 처리에서 {@code catalog.augmentContextApplied}, {@code
 *       DATA.pools}을(를) 사용한다. 입력: {@code catalog}(카탈로그), {@code state}(상태). 반환: {@code
 *       ItemCatalog}.
 *   <li>{@code rebuilt(original, base, definitions, contextual)}: rebuilt 처리에서 {@code
 *       original.metadata}, {@code definitions.stream}을(를) 사용한다. 입력: {@code original}(원본), {@code
 *       base}(베이스), {@code definitions}(정의목록), {@code contextual}(contextual). 반환: {@code
 *       ItemCatalog}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Effect}: 증강물 하나의 효과다. craftedBonus/suffixBonus는 추가 제작·접미 한도, maximumQuality는 품질 한도,
 *       runeEffectPercent는 룬 효과 보너스, idolBonded는 우상 결합, marker는 표시 표식, pool은 활성 옵션 풀 ID다.
 *   <li>{@code PoolWeightPolicy}: id는 가중치 정책 식별자, weightPerTier는 티어별 가중치, description은 근거 설명,
 *       consumers는 적용 소비자 목록이다.
 *   <li>{@code Document}: schemaVersion은 문서 버전, baseCraftedLimit는 기본 제작 옵션 한도, socketBound는 제거 금지
 *       ID, craftedModifierIds는 제작 옵션 ID, modifiers는 정의, pools는 베이스→풀→옵션 가중치, effects는 증강물별 효과,
 *       magnitudeRules는 수치 보정, poolWeightPolicy는 풀 선택 정책이다.
 * </ul>
 */
public final class AugmentCraftingRules {
  private AugmentCraftingRules() {}

  /**
   * 증강물 하나의 효과다. craftedBonus/suffixBonus는 추가 제작·접미 한도, maximumQuality는 품질 한도, runeEffectPercent는 룬
   * 효과 보너스, idolBonded는 우상 결합, marker는 표시 표식, pool은 활성 옵션 풀 ID다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code craftedBonus}: 제작옵션·Bonus ({@code int}).
   *   <li>{@code suffixBonus}: 접미·Bonus ({@code int}).
   *   <li>{@code maximumQuality}: 최대값·품질 ({@code int}).
   *   <li>{@code runeEffectPercent}: 룬·효과·백분율 ({@code int}).
   *   <li>{@code idolBonded}: idol·Bonded ({@code boolean}).
   *   <li>{@code marker}: marker ({@code String}).
   *   <li>{@code pool}: 후보풀 ({@code String}).
   * </ul>
   */
  public record Effect(
      int craftedBonus,
      int suffixBonus,
      int maximumQuality,
      int runeEffectPercent,
      boolean idolBonded,
      String marker,
      String pool) {}

  /**
   * id는 가중치 정책 식별자, weightPerTier는 티어별 가중치, description은 근거 설명, consumers는 적용 소비자 목록이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code weightPerTier}: 가중치·단위별·티어 ({@code int}).
   *   <li>{@code description}: 설명 ({@code String}).
   *   <li>{@code consumers}: consumers ({@code List<String>}).
   * </ul>
   */
  public record PoolWeightPolicy(
      String id, int weightPerTier, String description, List<String> consumers) {}

  /**
   * schemaVersion은 문서 버전, baseCraftedLimit는 기본 제작 옵션 한도, socketBound는 제거 금지 ID, craftedModifierIds는
   * 제작 옵션 ID, modifiers는 정의, pools는 베이스→풀→옵션 가중치, effects는 증강물별 효과, magnitudeRules는 수치 보정,
   * poolWeightPolicy는 풀 선택 정책이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code schemaVersion}: 스키마·버전 ({@code int}).
   *   <li>{@code baseCraftedLimit}: 베이스·제작옵션·한도 ({@code int}).
   *   <li>{@code socketBound}: 장착홈·경계값 ({@code Set<String>}).
   *   <li>{@code craftedModifierIds}: 제작옵션·옵션·식별자목록 ({@code Set<String>}).
   *   <li>{@code modifiers}: 옵션목록 ({@code List<ModifierDefinition>}).
   *   <li>{@code pools}: 후보풀목록 ({@code Map<String, Map<String, Map<String, Integer>>>}).
   *   <li>{@code effects}: 효과목록 ({@code Map<String, Effect>}).
   *   <li>{@code magnitudeRules}: magnitude·규칙목록 ({@code List<ModifierMagnitudes.Rule>}).
   *   <li>{@code poolWeightPolicy}: 후보풀·가중치·정책 ({@code PoolWeightPolicy}).
   * </ul>
   */
  public record Document(
      int schemaVersion,
      int baseCraftedLimit,
      Set<String> socketBound,
      Set<String> craftedModifierIds,
      List<ModifierDefinition> modifiers,
      Map<String, Map<String, Map<String, Integer>>> pools,
      Map<String, Effect> effects,
      List<ModifierMagnitudes.Rule> magnitudeRules,
      PoolWeightPolicy poolWeightPolicy) {}

  private static final Effect NONE = new Effect(0, 0, 0, 0, false, null, null);
  private static final Document DATA = load();

  private static Document load() {
    try (var input = SealedResources.open("/catalog/augment-crafting.json")) {
      var result = new ObjectMapper().readValue(input, Document.class);
      if (result.poolWeightPolicy() == null
          || result.poolWeightPolicy().weightPerTier() <= 0
          || result.baseCraftedLimit() < 0
          || result.schemaVersion() != 1
          || result.modifiers().stream()
              .anyMatch(d -> d.weight() != 0 || !d.tags().contains("augment_pool")))
        throw new IllegalArgumentException("Invalid Augment crafting registry");
      return result;
    } catch (java.io.IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public static PoolWeightPolicy poolWeightPolicy() {
    return DATA.poolWeightPolicy();
  }

  public static List<ModifierMagnitudes.Rule> magnitudeRules() {
    return DATA.magnitudeRules();
  }

  public static Effect effect(String id) {
    return DATA.effects().getOrDefault(id, NONE);
  }

  public static boolean socketBound(String id) {
    return DATA.socketBound().contains(id);
  }

  public static int suffixBonus(ItemState state) {
    return state.installedAugments().stream()
        .mapToInt(a -> effect(a.augmentId()).suffixBonus())
        .sum();
  }

  public static int baseCraftedLimit() {
    return DATA.baseCraftedLimit();
  }

  public static int craftedLimit(ItemState state) {
    return DATA.baseCraftedLimit()
        + state.installedAugments().stream()
            .mapToInt(a -> effect(a.augmentId()).craftedBonus())
            .sum();
  }

  public static boolean crafted(ModifierInstance modifier, ItemCatalog catalog) {
    return modifier.crafted()
        || DATA.craftedModifierIds().contains(modifier.modifierId())
        || catalog.find(modifier.modifierId()).map(d -> d.tags().contains("crafted")).orElse(false);
  }

  public static int maximumQuality(ItemState state) {
    return state.installedAugments().stream()
        .mapToInt(a -> effect(a.augmentId()).maximumQuality())
        .max()
        .orElse(0);
  }

  public static Set<String> pools(ItemState state) {
    var result = new TreeSet<String>();
    for (var augment : state.installedAugments()) {
      var pool = effect(augment.augmentId()).pool();
      if (pool != null) result.add(pool);
    }
    return Set.copyOf(result);
  }

  /** 이 베이스에 장착 가능한 증강물과 장착으로 열리는 실제 추첨 옵션의 ID를 연결한다. */
  public static Map<String, Set<String>> poolSources(String baseId) {
    var pools = DATA.pools().getOrDefault(baseId, Map.of());
    var result = new TreeMap<String, Set<String>>();
    DATA.effects()
        .forEach(
            (id, effect) -> {
              if (effect.pool() != null
                  && pools.containsKey(effect.pool())
                  && SocketAugments.supports(baseId, id))
                result.put(id, Set.copyOf(pools.get(effect.pool()).keySet()));
            });
    return Collections.unmodifiableMap(result);
  }

  public static boolean affectsSampling(ItemState state) {
    return suffixBonus(state) > 0 || !pools(state).isEmpty();
  }

  /**
   * Permanent source definitions are installed with zero weight; equipment activates their source
   * temporary simulator weights.
   */
  public static ItemCatalog enrich(ItemCatalog catalog) {
    var ids = new HashSet<String>();
    DATA.pools()
        .getOrDefault(catalog.base().id(), Map.of())
        .values()
        .forEach(p -> ids.addAll(p.keySet()));
    ids.removeAll(catalog.modifiers().keySet());
    if (ids.isEmpty()) return catalog;
    var definitions = new ArrayList<>(catalog.modifiers().values());
    DATA.modifiers().stream().filter(d -> ids.contains(d.id())).forEach(definitions::add);
    return rebuilt(catalog, catalog.base(), definitions, false);
  }

  /**
   * An ephemeral sampler view adapts the existing bucket engine without putting socket state into
   * search buckets.
   */
  public static ItemCatalog forItem(ItemCatalog catalog, ItemState state) {
    if (catalog.augmentContextApplied() || !affectsSampling(state)) return catalog;
    var selected = new HashMap<String, Integer>();
    var pools = DATA.pools().getOrDefault(catalog.base().id(), Map.of());
    for (var pool : pools(state))
      pools
          .getOrDefault(pool, Map.of())
          .keySet()
          .forEach(id -> selected.put(id, poolWeightPolicy().weightPerTier()));
    var definitions =
        catalog.modifiers().values().stream()
            .map(
                d ->
                    selected.containsKey(d.id())
                        ? new ModifierDefinition(
                            d.id(),
                            d.name(),
                            d.layer(),
                            d.affixType(),
                            d.familyIds(),
                            d.requiredItemLevel(),
                            selected.get(d.id()),
                            d.tier(),
                            d.text(),
                            d.stats(),
                            d.tags(),
                            d.sourceUrl())
                        : d)
            .toList();
    var base = catalog.base();
    int suffix = suffixBonus(state);
    var expanded =
        new ItemCatalog.BaseItem(
            base.id(),
            base.name(),
            base.sourceUrl(),
            base.implicitModifierId(),
            base.magicPrefixes(),
            base.magicSuffixes() + suffix,
            base.rarePrefixes(),
            base.rareSuffixes() + suffix);
    return rebuilt(catalog, expanded, definitions, true);
  }

  private static ItemCatalog rebuilt(
      ItemCatalog original,
      ItemCatalog.BaseItem base,
      List<ModifierDefinition> definitions,
      boolean contextual) {
    var old = original.metadata();
    var prefixes =
        definitions.stream()
            .filter(d -> d.affixType() == ModifierDefinition.AffixType.PREFIX)
            .toList();
    var suffixes =
        definitions.stream()
            .filter(d -> d.affixType() == ModifierDefinition.AffixType.SUFFIX)
            .toList();
    var metadata =
        new ItemCatalog.Metadata(
            old.snapshotId(),
            old.retrievedAt(),
            old.sourceUrl(),
            old.weightPolicy(),
            old.rawSha256(),
            old.detailsSha256(),
            prefixes.size(),
            suffixes.size(),
            prefixes.stream().mapToLong(ModifierDefinition::weight).sum(),
            suffixes.stream().mapToLong(ModifierDefinition::weight).sum());
    return new ItemCatalog(
        metadata, base, definitions, original.compatibleSnapshotIds(), contextual);
  }
}
