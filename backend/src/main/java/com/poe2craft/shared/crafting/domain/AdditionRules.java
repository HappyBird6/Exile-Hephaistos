package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import java.util.*;

/**
 * 옵션 선택과 정확 확률 계산이 공유하는 후보·최소 레벨 규칙이다.
 *
 * <p>기존 계약: Shared eligibility and minimum-level pool for sampling and exact finite additions.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code resolver}: resolver ({@code ModifierPoolResolver}).
 *   <li>{@code validator}: 상태·입력 검증기 ({@code CraftingEngine}).
 *   <li>{@code generationGroups}: 세대번호·그룹목록 ({@code Map<String, Integer>}).
 *   <li>{@code generationGroupCount}: 세대번호·그룹·개수 ({@code int}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AdditionRules(catalog)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code catalog}(카탈로그).
 *   <li>{@code isAddition(action)}: 추가을(를) 여부를 판정한다. 입력: {@code action}(행동). 반환: {@code boolean}.
 *   <li>{@code plan(state, action, activeOmens)}: 지원 규칙 아래 실행할 분기를 계획한다. 입력: {@code state}(상태),
 *       {@code action}(행동), {@code activeOmens}(활성·징조목록). 반환: {@code Plan}.
 *   <li>{@code blocked(state, ids, reason)}: 차단의 입력에 따른 분기·결과를 처리한다. 입력: {@code state}(상태), {@code
 *       ids}(식별자목록), {@code reason}(이유). 반환: {@code Plan}.
 *   <li>{@code upgradedRarity(state, action)}: upgraded·희귀도 처리에서 {@code action.baseAction}, {@code
 *       state.rarity}을(를) 사용한다. 입력: {@code state}(상태), {@code action}(행동). 반환: {@code
 *       ItemState.Rarity}.
 *   <li>{@code pool(state, currency, omen)}: 후보풀 처리에서 {@code resolver.resolve}, {@code
 *       omen.affix}을(를) 사용한다. 입력: {@code state}(상태), {@code currency}(화폐), {@code omen}(징조). 반환:
 *       {@code List<ModifierDefinition>}.
 *   <li>{@code poolWithTags(state, currency, tags)}: 후보풀·With·태그목록 처리에서 {@code
 *       resolver.resolve}을(를) 사용한다. 입력: {@code state}(상태), {@code currency}(화폐), {@code
 *       tags}(태그목록). 반환: {@code List<ModifierDefinition>}.
 *   <li>{@code minimumLevelPool(eligible, currency)}: 최소값·레벨·후보풀 처리에서 {@code
 *       currency.minimumModifierLevel}, {@code d.requiredItemLevel}을(를) 사용한다. 입력: {@code
 *       eligible}(적격), {@code currency}(화폐). 반환: {@code List<ModifierDefinition>}.
 *   <li>{@code generationGroup(definition)}: 세대번호·그룹 처리에서 {@code generationGroups.get}, {@code
 *       definition.id}을(를) 사용한다. 입력: {@code definition}(정의). 반환: {@code int}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code group(d)}: 그룹 처리에서 {@code d.familyIds}, {@code d.affixType}을(를) 사용한다. 입력: {@code
 *       d}(d). 반환: {@code String}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code existingTags(state)}: 기존·태그목록 처리에서 {@code Stream.concat}, {@code
 *       state.modifierIds}을(를) 사용한다. 입력: {@code state}(상태). 반환: {@code Set<String>}.
 *   <li>{@code matchingTags(eligible, originalTags)}: 일치·태그목록 처리에서 {@code originalTags.isEmpty},
 *       {@code eligible.stream}을(를) 사용한다. 입력: {@code eligible}(적격), {@code originalTags}(원본·태그목록).
 *       반환: {@code List<ModifierDefinition>}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Plan}: {@code AdditionRules}에서 사용하는 계획의 불변 값이다.
 * </ul>
 */
public final class AdditionRules {
  private final ItemCatalog catalog;
  private final ModifierPoolResolver resolver;
  private final CraftingEngine validator;
  private final Map<String, Integer> generationGroups;
  private final int generationGroupCount;

  public AdditionRules(ItemCatalog catalog) {
    this.catalog = Objects.requireNonNull(catalog);
    resolver = new ModifierPoolResolver(catalog);
    validator = new CraftingEngine(catalog);
    var groups = new HashMap<String, Integer>();
    var definitions = new HashMap<String, Integer>();
    for (var definition : catalog.modifiers().values()) {
      // Keep the existing lazy rejection when a tiered operation encounters an
      // unverified multi-family modifier; catalog presence alone is not support.
      if (definition.familyIds().size() != 1) continue;
      int group = groups.computeIfAbsent(group(definition), ignored -> groups.size());
      definitions.put(definition.id(), group);
    }
    generationGroups = Map.copyOf(definitions);
    generationGroupCount = groups.size();
  }

  public static boolean isAddition(WorkbenchCurrency action) {
    return action != null
        && action.baseAction() != null
        && action.baseAction() != CraftingAction.CHAOS
        && action.baseAction() != CraftingAction.ANNULMENT;
  }

  public Plan plan(StateBucket state, WorkbenchCurrency action, Set<String> activeOmens) {
    validator.validate(state);
    Objects.requireNonNull(action, "action");
    var ids = activeOmens == null ? List.<String>of() : activeOmens.stream().sorted().toList();
    var omens = ids.stream().map(WorkbenchOmen::fromId).toList();
    if (!isAddition(action))
      return blocked(state, ids, "This action is outside the finite addition model.");
    var matching =
        omens.stream().filter(o -> o.trigger().baseAction() == action.baseAction()).toList();
    if (matching.size() > 1)
      return blocked(
          state,
          ids,
          "Multiple omens for the same operation need combination verification. Deactivate all but one.");
    if (matching.contains(WorkbenchOmen.GREATER_EXALTATION))
      return blocked(state, ids, "Two-modifier additions are outside the finite addition model.");
    if (action.minimumModifierLevel() > 0
        && matching.stream().anyMatch(o -> !o.supportsTieredCurrency()))
      return blocked(
          state,
          ids,
          "Omen interaction with Greater/Perfect currency is not verified. Deactivate the omen or use ordinary currency.");
    if (state.itemLevel() < action.minimumModifierLevel())
      return blocked(state, ids, "Item level is below the currency's minimum modifier level.");
    boolean rarity =
        switch (action.baseAction()) {
          case TRANSMUTATION -> state.rarity() == ItemState.Rarity.NORMAL;
          case AUGMENTATION, REGAL -> state.rarity() == ItemState.Rarity.MAGIC;
          case EXALTED -> state.rarity() == ItemState.Rarity.RARE;
          default -> false;
        };
    if (!rarity) return blocked(state, ids, "This currency cannot be used on this rarity.");
    var next = state.with(upgradedRarity(state, action), state.modifierIds());
    var candidates = pool(next, action, matching.isEmpty() ? null : matching.getFirst());
    if (candidates.isEmpty())
      return blocked(
          state,
          ids,
          "No eligible modifier or free affix slot remains for this currency and omen.");
    var consumed = matching.stream().map(WorkbenchOmen::id).toList();
    return new Plan(
        true,
        "",
        next,
        candidates,
        consumed,
        ids.stream().filter(id -> !consumed.contains(id)).toList());
  }

  private Plan blocked(StateBucket state, List<String> ids, String reason) {
    return new Plan(false, reason, state, List.of(), List.of(), ids);
  }

  public static ItemState.Rarity upgradedRarity(StateBucket state, WorkbenchCurrency action) {
    return action.baseAction() == CraftingAction.TRANSMUTATION
        ? ItemState.Rarity.MAGIC
        : action.baseAction() == CraftingAction.REGAL ? ItemState.Rarity.RARE : state.rarity();
  }

  /** PoE2DB GenGroup = generation type + first family; this snapshot is single-family. */
  public List<ModifierDefinition> pool(
      StateBucket state, WorkbenchCurrency currency, WorkbenchOmen omen) {
    var eligible = resolver.resolve(state).candidates();
    if (omen != null && omen.affix() != null)
      eligible = eligible.stream().filter(d -> d.affixType() == omen.affix()).toList();
    if (omen != null && omen.homogenising()) eligible = matchingTags(eligible, existingTags(state));
    return minimumLevelPool(eligible, currency);
  }

  /** Frozen pre-craft tag eligibility precedes the level pool for every multi-add stage. */
  public List<ModifierDefinition> poolWithTags(
      StateBucket state, WorkbenchCurrency currency, Set<String> tags) {
    return minimumLevelPool(matchingTags(resolver.resolve(state).candidates(), tags), currency);
  }

  private List<ModifierDefinition> minimumLevelPool(
      List<ModifierDefinition> eligible, WorkbenchCurrency currency) {
    if (currency.minimumModifierLevel() == 0) return eligible;
    var highest = new int[generationGroupCount];
    for (var d : eligible) {
      int group = generationGroup(d);
      highest[group] = Math.max(highest[group], d.requiredItemLevel());
    }
    return eligible.stream()
        .filter(
            d ->
                d.requiredItemLevel() >= currency.minimumModifierLevel()
                    || d.requiredItemLevel() == highest[generationGroup(d)])
        .toList();
  }

  private int generationGroup(ModifierDefinition definition) {
    var group = generationGroups.get(definition.id());
    if (group == null)
      throw new IllegalArgumentException("Modifier-type grouping needs verification");
    return group;
  }

  private String group(ModifierDefinition d) {
    if (d.familyIds().size() != 1)
      throw new IllegalArgumentException("Modifier-type grouping needs verification");
    return d.affixType() + ":" + d.familyIds().iterator().next();
  }

  /** Modifier tags, not spawn tags or families; the caller freezes this set before a multi-add. */
  public Set<String> existingTags(StateBucket state) {
    var tags = new HashSet<String>();
    java.util.stream.Stream.concat(
            state.modifierIds().stream(),
            state.implicits().stream().map(ModifierInstance::modifierId))
        .forEach(id -> tags.addAll(catalog.find(id).orElseThrow().tags()));
    return Set.copyOf(tags);
  }

  public static List<ModifierDefinition> matchingTags(
      List<ModifierDefinition> eligible, Set<String> originalTags) {
    return originalTags.isEmpty()
        ? eligible
        : eligible.stream().filter(d -> !Collections.disjoint(d.tags(), originalTags)).toList();
  }

  /**
   * {@code AdditionRules}에서 사용하는 계획의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code available}: 사용가능 ({@code boolean}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code upgraded}: upgraded ({@code StateBucket}).
   *   <li>{@code candidates}: 후보목록 ({@code List<ModifierDefinition>}).
   *   <li>{@code consumedOmens}: 이번 실행으로 소비된 징조 ID 목록 ({@code List<String>}).
   *   <li>{@code remainingOmens}: 실행 뒤 남은 징조 ID 목록 ({@code List<String>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Plan(available, reason, upgraded, candidates, consumedOmens, remainingOmens)}:
   *       전달받은 값으로 구성원을 초기화한다. 입력: {@code available}(사용가능), {@code reason}(이유), {@code
   *       upgraded}(upgraded), {@code candidates}(후보목록), {@code consumedOmens}(소비된·징조목록), {@code
   *       remainingOmens}(남은·징조목록).
   * </ul>
   */
  public record Plan(
      boolean available,
      String reason,
      StateBucket upgraded,
      List<ModifierDefinition> candidates,
      List<String> consumedOmens,
      List<String> remainingOmens) {
    public Plan {
      candidates = List.copyOf(candidates);
      consumedOmens = List.copyOf(consumedOmens);
      remainingOmens = List.copyOf(remainingOmens);
    }
  }
}
