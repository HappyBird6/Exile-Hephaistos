package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.BasicJewel;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierDefinition;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 공개 가중치 모형 아래 한 번의 제작에서 가능한 모든 옵션 결과를 계산한다.
 *
 * <p>기존 계약: Pure exhaustive one-action distribution under the published-weight model.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code RULE_VERSION}: 버전; 초기값 {@code "solar-base-six-v1"} ({@code String}).
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code resolver}: resolver ({@code ModifierPoolResolver}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code CraftingEngine(catalog)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code catalog}(카탈로그).
 *   <li>{@code validate(state)}: 입력의 구조·값·지원 조건을 검증한다. 입력: {@code state}(상태). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code actions(state)}: 현재 상태에서 가능한 행동과 불가 이유를 조회한다. 입력: {@code state}(상태). 반환: {@code
 *       List<Availability>}.
 *   <li>{@code availability(state, action)}: 화폐의 현재 실행 가능 여부와 이유를 판정한다. 입력: {@code state}(상태),
 *       {@code action}(행동). 반환: {@code Availability}.
 *   <li>{@code transition(state, action)}: 전이 처리에서 {@code available.available}, {@code
 *       state.id}을(를) 사용한다. 입력: {@code state}(상태), {@code action}(행동). 반환: {@code
 *       TransitionResult}.
 *   <li>{@code add(state, branchChance, results)}: 값 또는 항목을 더한다. 입력: {@code state}(상태), {@code
 *       branchChance}(분기·Chance), {@code results}(결과목록).
 *   <li>{@code upgrade(state, action)}: upgrade 처리에서 {@code state.with}, {@code state.rarity}을(를)
 *       사용한다. 입력: {@code state}(상태), {@code action}(행동). 반환: {@code StateBucket}.
 *   <li>{@code remove(state, index)}: 지정한 항목을 제거한다. 입력: {@code state}(상태), {@code index}(색인). 반환:
 *       {@code StateBucket}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Availability}: {@code CraftingEngine}에서 사용하는 Availability의 불변 값이다.
 *   <li>{@code Outcome}: {@code CraftingEngine}에서 사용하는 결과의 불변 값이다.
 *   <li>{@code TransitionResult}: {@code CraftingEngine}에서 사용하는 전이·결과의 불변 값이다.
 * </ul>
 */
public final class CraftingEngine {
  public static final String RULE_VERSION = "solar-base-six-v1";
  private final ItemCatalog catalog;
  private final ModifierPoolResolver resolver;

  public CraftingEngine(ItemCatalog catalog) {
    this.catalog = Objects.requireNonNull(catalog);
    resolver = new ModifierPoolResolver(catalog);
  }

  public void validate(StateBucket state) {
    if (state == null) throw new IllegalArgumentException("State is required");
    if (!state.conditions().isEmpty())
      throw new IllegalArgumentException("Special item conditions cannot be modified");
    // Reuse concrete validation for the real implicit; no invented explicit rolls.
    var shell =
        new ItemState(
            state.snapshotId(),
            state.baseItemId(),
            state.itemLevel(),
            state.rarity(),
            state.implicits(),
            List.of(),
            state.conditions());
    if (!new ItemStateValidator(catalog).validate(shell).isEmpty()) {
      throw new IllegalArgumentException("Unsupported item state or snapshot");
    }
    var families = new HashSet<String>();
    int p = 0, s = 0;
    for (String id : state.modifierIds()) {
      var d = catalog.find(id).orElseThrow(() -> new IllegalArgumentException("Unknown modifier"));
      if (d.layer() != ModifierDefinition.Layer.EXPLICIT
          || d.familyIds().stream().anyMatch(families::contains)) {
        throw new IllegalArgumentException("Invalid modifier layer or conflicting families");
      }
      families.addAll(d.familyIds());
      if (d.affixType() == ModifierDefinition.AffixType.PREFIX) p++;
      else s++;
    }
    int maxP =
        state.rarity() == ItemState.Rarity.NORMAL
            ? 0
            : state.rarity() == ItemState.Rarity.MAGIC
                ? catalog.base().magicPrefixes()
                : catalog.base().rarePrefixes();
    int maxS =
        state.rarity() == ItemState.Rarity.NORMAL
            ? 0
            : state.rarity() == ItemState.Rarity.MAGIC
                ? catalog.base().magicSuffixes()
                : catalog.base().rareSuffixes();
    if (BasicJewel.supportedCrafting(state.baseItemId())
        && state.rarity() == ItemState.Rarity.RARE) {
      maxP += BasicJewel.extra(state.modifierIds(), ModifierDefinition.AffixType.PREFIX);
      maxS += BasicJewel.extra(state.modifierIds(), ModifierDefinition.AffixType.SUFFIX);
      if (!BasicJewel.existingCapacity(p, s, maxP, maxS))
        throw new IllegalArgumentException("Affix capacity exceeded");
      return;
    }
    if (p > maxP || s > maxS) throw new IllegalArgumentException("Affix capacity exceeded");
  }

  public List<Availability> actions(StateBucket state) {
    validate(state);
    return java.util.Arrays.stream(CraftingAction.values())
        .map(a -> availability(state, a))
        .toList();
  }

  private Availability availability(StateBucket state, CraftingAction action) {
    boolean rarity =
        switch (action) {
          case TRANSMUTATION -> state.rarity() == ItemState.Rarity.NORMAL;
          case AUGMENTATION, REGAL -> state.rarity() == ItemState.Rarity.MAGIC;
          case EXALTED, CHAOS -> state.rarity() == ItemState.Rarity.RARE;
          case ANNULMENT ->
              state.rarity() == ItemState.Rarity.MAGIC || state.rarity() == ItemState.Rarity.RARE;
        };
    if (!rarity)
      return new Availability(action, false, "This currency cannot be used on this rarity.");
    if (action == CraftingAction.ANNULMENT || action == CraftingAction.CHAOS) {
      if (state.modifierIds().isEmpty())
        return new Availability(action, false, "There is no explicit modifier to remove.");
      if (action == CraftingAction.CHAOS) {
        for (int i = 0; i < state.modifierIds().size(); i++) {
          if (resolver.resolve(remove(state, i)).totalWeight() == 0) {
            return new Availability(
                action, false, "A removal branch has no supported replacement modifier.");
          }
        }
      }
    } else if (resolver.resolve(upgrade(state, action)).totalWeight() == 0) {
      return new Availability(action, false, "No eligible modifier or free affix slot remains.");
    }
    return new Availability(action, true, "");
  }

  public TransitionResult transition(StateBucket state, CraftingAction action) {
    validate(state);
    Objects.requireNonNull(action, "action");
    var available = availability(state, action);
    if (!available.available())
      return new TransitionResult(state.id(), action, false, available.reason(), List.of());
    Map<StateBucket, Double> results = new LinkedHashMap<>();
    if (action == CraftingAction.ANNULMENT || action == CraftingAction.CHAOS) {
      double removalChance = 1.0 / state.modifierIds().size();
      for (int i = 0; i < state.modifierIds().size(); i++) {
        var removed = remove(state, i);
        if (action == CraftingAction.ANNULMENT) results.merge(removed, removalChance, Double::sum);
        else add(removed, removalChance, results);
      }
    } else add(upgrade(state, action), 1.0, results);
    var outcomes =
        results.entrySet().stream()
            .map(e -> new Outcome(e.getKey().id(), e.getKey(), e.getValue()))
            .sorted(
                Comparator.comparingDouble(Outcome::probability)
                    .reversed()
                    .thenComparing(Outcome::id))
            .toList();
    return new TransitionResult(state.id(), action, true, "", outcomes);
  }

  private void add(StateBucket state, double branchChance, Map<StateBucket, Double> results) {
    var pool = resolver.resolve(state);
    for (var candidate : pool.candidates()) {
      var ids = new ArrayList<>(state.modifierIds());
      ids.add(candidate.id());
      results.merge(
          state.with(state.rarity(), ids),
          branchChance * candidate.weight() / pool.totalWeight(),
          Double::sum);
    }
  }

  private StateBucket upgrade(StateBucket state, CraftingAction action) {
    return state.with(
        action == CraftingAction.TRANSMUTATION
            ? ItemState.Rarity.MAGIC
            : action == CraftingAction.REGAL ? ItemState.Rarity.RARE : state.rarity(),
        state.modifierIds());
  }

  private StateBucket remove(StateBucket state, int index) {
    var ids = new ArrayList<>(state.modifierIds());
    ids.remove(index);
    return state.with(state.rarity(), ids);
  }

  /**
   * {@code CraftingEngine}에서 사용하는 Availability의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code CraftingAction}).
   *   <li>{@code available}: 사용가능 ({@code boolean}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   * </ul>
   */
  public record Availability(CraftingAction action, boolean available, String reason) {}

  /**
   * {@code CraftingEngine}에서 사용하는 결과의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code StateBucket}).
   *   <li>{@code probability}: 확률 ({@code double}).
   * </ul>
   */
  public record Outcome(String id, StateBucket state, double probability) {}

  /**
   * {@code CraftingEngine}에서 사용하는 전이·결과의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code fromId}: 원본·식별자 ({@code String}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code CraftingAction}).
   *   <li>{@code available}: 사용가능 ({@code boolean}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code outcomes}: 결과목록 ({@code List<Outcome>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code TransitionResult(fromId, action, available, reason, outcomes)}: 전달받은 값으로 구성원을
   *       초기화한다. 입력: {@code fromId}(원본·식별자), {@code action}(행동), {@code available}(사용가능), {@code
   *       reason}(이유), {@code outcomes}(결과목록).
   * </ul>
   */
  public record TransitionResult(
      String fromId,
      CraftingAction action,
      boolean available,
      String reason,
      List<Outcome> outcomes) {
    public TransitionResult {
      outcomes = List.copyOf(outcomes);
    }
  }
}
