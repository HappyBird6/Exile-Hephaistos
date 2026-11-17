package com.poe2craft.craftingsupport.recommendation.domain;

import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.StateBucket;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ModifierDefinition;
import java.util.*;

/**
 * 옵션 계열·티어 목표의 현재 달성과 추가 가능성을 판정한다.
 *
 * <p>기존 계약: One family/tier goal; assesses the root and feasibility under finite additions only.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code families}: 옵션계열목록 ({@code Map<String, List<ModifierDefinition>>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code SupportGoals(catalog)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code catalog}(카탈로그). 사용하는 예외:
 *       {@code IllegalArgumentException}.
 *   <li>{@code families()}: 옵션계열목록 값을 조회·구성한다. 반환: {@code List<Family>}.
 *   <li>{@code assess(state, goal)}: assess 처리에서 {@code goal.required}, {@code goal.candidates}을(를)
 *       사용한다. 입력: {@code state}(상태), {@code goal}(목표). 반환: {@code Assessment}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code canAdd(c, present, level)}: Add을(를) 가능 여부를 판정한다. 입력: {@code c}(c), {@code
 *       present}(present), {@code level}(레벨). 반환: {@code boolean}.
 *   <li>{@code invalid(reason)}: 잘못된 입력을 나타내는 오류 결과를 만든다. 입력: {@code reason}(이유). 반환: {@code
 *       Assessment}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Condition}: {@code SupportGoals}에서 사용하는 조건의 불변 값이다.
 *   <li>{@code Goal}: {@code SupportGoals}에서 사용하는 목표의 불변 값이다.
 *   <li>{@code Family}: {@code SupportGoals}에서 사용하는 옵션계열의 불변 값이다.
 *   <li>{@code Tier}: {@code SupportGoals}에서 사용하는 티어의 불변 값이다.
 *   <li>{@code Assessment}: {@code SupportGoals}에서 사용하는 평가의 불변 값이다.
 * </ul>
 */
public final class SupportGoals {
  private final ItemCatalog catalog;
  private final Map<String, List<ModifierDefinition>> families = new TreeMap<>();

  public SupportGoals(ItemCatalog catalog) {
    this.catalog = Objects.requireNonNull(catalog);
    for (var d : catalog.modifiers().values()) {
      if (d.layer() != ModifierDefinition.Layer.EXPLICIT || d.weight() == 0) continue;
      if (d.familyIds().size() != 1)
        throw new IllegalArgumentException("Support family grouping needs verification");
      families.computeIfAbsent(d.familyIds().iterator().next(), key -> new ArrayList<>()).add(d);
    }
    for (var ds : families.values()) {
      if (ds.stream().anyMatch(d -> d.affixType() != ds.getFirst().affixType()))
        throw new IllegalArgumentException("Cross-affix family projection needs verification");
      ds.sort(
          Comparator.comparingInt(ModifierDefinition::tier).thenComparing(ModifierDefinition::id));
      for (int i = 1; i < ds.size(); i++)
        if (ds.get(i - 1).requiredItemLevel() < ds.get(i).requiredItemLevel())
          throw new IllegalArgumentException("Catalog tier ranking needs verification");
    }
  }

  public List<Family> families() {
    return families.entrySet().stream()
        .map(
            e -> {
              var examples = new LinkedHashMap<List<String>, String>();
              for (var d : e.getValue())
                examples.putIfAbsent(
                    d.stats().stream().map(ModifierDefinition.StatRange::id).sorted().toList(),
                    d.text());
              return new Family(
                  e.getKey(),
                  e.getValue().getFirst().affixType(),
                  e.getValue().stream()
                      .map(d -> new Tier(d.tier(), d.requiredItemLevel(), d.text(), d.id()))
                      .toList(),
                  List.copyOf(examples.values()));
            })
        .toList();
  }

  public Assessment assess(StateBucket state, Goal goal) {
    new CraftingEngine(catalog).validate(state);
    if (goal == null || goal.required() == null || goal.candidates() == null)
      return invalid("Provide required and candidate family conditions.");
    var all = new ArrayList<>(goal.required());
    all.addAll(goal.candidates());
    if (all.size() > 30
        || goal.candidateCount() < 0
        || goal.candidateCount() > goal.candidates().size())
      return invalid(
          "Candidate N must be between zero and the number of distinct candidate families.");
    if (goal.required().isEmpty() && goal.candidateCount() == 0)
      return invalid("Choose at least one required condition or a positive candidate N.");
    var seen = new HashSet<String>();
    for (var condition : all) {
      if (condition == null || !families.containsKey(condition.family()))
        return invalid("Unknown modifier family.");
      if (!seen.add(condition.family()))
        return invalid("Each family may appear once across required and candidate conditions.");
      if (families.get(condition.family()).stream()
          .noneMatch(d -> d.tier() == condition.minimumTier()))
        return invalid("Select a tier that exists in the chosen family.");
    }
    var present = new HashMap<String, ModifierDefinition>();
    for (var id : state.modifierIds()) {
      var d = catalog.find(id).orElseThrow();
      if (d.requiredItemLevel() > state.itemLevel())
        throw new IllegalArgumentException("Modifier exceeds item level");
      present.put(d.familyIds().iterator().next(), d);
    }
    var matches = new TreeMap<String, Boolean>();
    all.forEach(
        c ->
            matches.put(
                c.family(),
                present.containsKey(c.family())
                    && present.get(c.family()).tier() <= c.minimumTier()));
    int requiredMatched =
        (int) goal.required().stream().filter(c -> matches.get(c.family())).count();
    int candidatesMatched =
        (int) goal.candidates().stream().filter(c -> matches.get(c.family())).count();
    boolean achieved =
        requiredMatched == goal.required().size() && candidatesMatched >= goal.candidateCount();
    if (achieved)
      return new Assessment(
          "ACHIEVED", true, true, true, requiredMatched, candidatesMatched, matches, List.of());
    var issues = new ArrayList<String>();
    int freeP =
        catalog.base().rarePrefixes()
            - (int)
                present.values().stream()
                    .filter(d -> d.affixType() == ModifierDefinition.AffixType.PREFIX)
                    .count();
    int freeS =
        catalog.base().rareSuffixes()
            - (int)
                present.values().stream()
                    .filter(d -> d.affixType() == ModifierDefinition.AffixType.SUFFIX)
                    .count();
    int needP = 0, needS = 0, possibleP = 0, possibleS = 0;
    for (var c : goal.required()) {
      if (matches.get(c.family())) continue;
      if (!canAdd(c, present, state.itemLevel()))
        issues.add(
            "Required family "
                + c.family()
                + " cannot meet T"
                + c.minimumTier()
                + " or better through additions; it is occupied below target or has no eligible tier at this item level.");
      if (families.get(c.family()).getFirst().affixType() == ModifierDefinition.AffixType.PREFIX)
        needP++;
      else needS++;
    }
    if (needP > freeP || needS > freeS)
      issues.add("Required conditions exceed the remaining prefix/suffix capacity.");
    for (var c : goal.candidates()) {
      if (matches.get(c.family()) || !canAdd(c, present, state.itemLevel())) continue;
      if (families.get(c.family()).getFirst().affixType() == ModifierDefinition.AffixType.PREFIX)
        possibleP++;
      else possibleS++;
    }
    int possible =
        candidatesMatched
            + Math.min(Math.max(0, freeP - needP), possibleP)
            + Math.min(Math.max(0, freeS - needS), possibleS);
    if (possible < goal.candidateCount())
      issues.add(
          "Candidate N cannot be reached with eligible distinct families and remaining affix capacity.");
    return new Assessment(
        issues.isEmpty() ? "READY" : "IMPOSSIBLE",
        true,
        false,
        issues.isEmpty(),
        requiredMatched,
        candidatesMatched,
        matches,
        issues);
  }

  private boolean canAdd(Condition c, Map<String, ModifierDefinition> present, int level) {
    return !present.containsKey(c.family())
        && families.get(c.family()).stream()
            .anyMatch(
                d ->
                    d.weight() > 0
                        && d.requiredItemLevel() <= level
                        && d.tier() <= c.minimumTier());
  }

  private Assessment invalid(String reason) {
    return new Assessment("INVALID_GOAL", false, false, false, 0, 0, Map.of(), List.of(reason));
  }

  /**
   * {@code SupportGoals}에서 사용하는 조건의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code family}: 옵션계열 ({@code String}).
   *   <li>{@code minimumTier}: 최소값·티어 ({@code int}).
   * </ul>
   */
  public record Condition(String family, int minimumTier) {}

  /**
   * {@code SupportGoals}에서 사용하는 목표의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code required}: 필수 ({@code List<Condition>}).
   *   <li>{@code candidates}: 후보목록 ({@code List<Condition>}).
   *   <li>{@code candidateCount}: 후보·개수 ({@code int}).
   * </ul>
   */
  public record Goal(List<Condition> required, List<Condition> candidates, int candidateCount) {}

  /**
   * {@code SupportGoals}에서 사용하는 옵션계열의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code affix}: 접두접미옵션 ({@code ModifierDefinition.AffixType}).
   *   <li>{@code tiers}: 티어목록 ({@code List<Tier>}).
   *   <li>{@code effectExamples}: 효과·Examples ({@code List<String>}).
   * </ul>
   */
  public record Family(
      String id,
      ModifierDefinition.AffixType affix,
      List<Tier> tiers,
      List<String> effectExamples) {}

  /**
   * {@code SupportGoals}에서 사용하는 티어의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code tier}: 정의된 옵션 티어 ({@code int}).
   *   <li>{@code requiredItemLevel}: 필수·아이템·레벨 ({@code int}).
   *   <li>{@code exampleText}: example·텍스트 ({@code String}).
   *   <li>{@code modifierId}: 실제 부여된 옵션 정의 식별자 ({@code String}).
   * </ul>
   */
  public record Tier(int tier, int requiredItemLevel, String exampleText, String modifierId) {}

  /**
   * {@code SupportGoals}에서 사용하는 평가의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code valid}: 유효한 ({@code boolean}).
   *   <li>{@code achieved}: achieved ({@code boolean}).
   *   <li>{@code feasible}: feasible ({@code boolean}).
   *   <li>{@code requiredMatched}: 필수·일치한 ({@code int}).
   *   <li>{@code candidatesMatched}: 후보목록·일치한 ({@code int}).
   *   <li>{@code matches}: 일치결과 ({@code Map<String, Boolean>}).
   *   <li>{@code issues}: 문제목록 ({@code List<String>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Assessment(status, valid, achieved, feasible, requiredMatched, candidatesMatched,
   *       matches, issues)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code status}(상태), {@code valid}(유효한),
   *       {@code achieved}(achieved), {@code feasible}(feasible), {@code requiredMatched}(필수·일치한),
   *       {@code candidatesMatched}(후보목록·일치한), {@code matches}(일치결과), {@code issues}(문제목록).
   * </ul>
   */
  public record Assessment(
      String status,
      boolean valid,
      boolean achieved,
      boolean feasible,
      int requiredMatched,
      int candidatesMatched,
      Map<String, Boolean> matches,
      List<String> issues) {
    public Assessment {
      matches = Map.copyOf(matches);
      issues = List.copyOf(issues);
    }
  }
}
