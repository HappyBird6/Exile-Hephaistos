package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierDefinition.AffixType;
import com.poe2craft.shared.item.ModifierInstance;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 다음 옵션 추가 확률의 민감도를 비교한다. 훼손 후보 공개 확률은 계산하지 않는다.
 *
 * <p>기존 계약: Next-addition sensitivity, never the probability of observing an Abyss offer.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code additions}: additions ({@code AdditionRules}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AbyssProbabilityComparison(catalog)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       catalog}(카탈로그).
 *   <li>{@code compare(state, slotId, action, omen, targetIds)}: 비교 처리에서 {@code state.validate},
 *       {@code action.baseAction}을(를) 사용한다. 입력: {@code state}(상태), {@code slotId}(슬롯·식별자), {@code
 *       action}(행동), {@code omen}(징조), {@code targetIds}(대상·식별자목록). 반환: {@code
 *       Optional<Comparison>}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code pool(state, action, omen)}: 후보풀 처리에서 {@code state.validate}, {@code
 *       state.knownItem}을(를) 사용한다. 입력: {@code state}(상태), {@code action}(행동), {@code omen}(징조). 반환:
 *       {@code List<ModifierDefinition>}.
 *   <li>{@code chance(pool, targets)}: chance 처리에서 {@code pool.stream}, {@code
 *       targets.contains}을(를) 사용한다. 입력: {@code pool}(후보풀), {@code targets}(대상목록). 반환: {@code
 *       Chance}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Chance}: {@code AbyssProbabilityComparison}에서 사용하는 Chance의 불변 값이다.
 *   <li>{@code Comparison}: {@code AbyssProbabilityComparison}에서 사용하는 비교의 불변 값이다.
 * </ul>
 */
public final class AbyssProbabilityComparison {
  private final ItemCatalog catalog;
  private final AdditionRules additions;

  public AbyssProbabilityComparison(ItemCatalog catalog) {
    this.catalog = Objects.requireNonNull(catalog);
    additions = new AdditionRules(catalog);
  }

  /**
   * {@code AbyssProbabilityComparison}에서 사용하는 Chance의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code targetWeight}: 대상·가중치 ({@code long}).
   *   <li>{@code totalWeight}: 합계·가중치 ({@code long}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Chance(targetWeight, totalWeight)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       targetWeight}(대상·가중치), {@code totalWeight}(합계·가중치). 사용하는 예외: {@code
   *       IllegalArgumentException}.
   *   <li>{@code percent()}: 백분율 값을 조회·구성한다. 반환: {@code double}.
   * </ul>
   */
  public record Chance(long targetWeight, long totalWeight) {
    public Chance {
      if (targetWeight < 0 || totalWeight <= 0 || targetWeight > totalWeight)
        throw new IllegalArgumentException("Nonempty weighted pool required");
    }

    public double percent() {
      return 100.0 * targetWeight / totalWeight;
    }
  }

  /**
   * {@code AbyssProbabilityComparison}에서 사용하는 비교의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code baseline}: baseline ({@code Chance}).
   *   <li>{@code afterBlocking}: 변경후·Blocking ({@code Chance}).
   *   <li>{@code blockerModifierId}: blocker·옵션·식별자 ({@code String}).
   *   <li>{@code blockerWeight}: blocker·가중치 ({@code int}).
   * </ul>
   */
  public record Comparison(
      Chance baseline, Chance afterBlocking, String blockerModifierId, int blockerWeight) {}

  /** A missing comparison means the conditional scenario is not calculable, not zero chance. */
  public Optional<Comparison> compare(
      AbyssCraftingState state,
      String slotId,
      WorkbenchCurrency action,
      WorkbenchOmen omen,
      Set<String> targetIds) {
    state.validate(catalog);
    if (action.baseAction() != CraftingAction.EXALTED || targetIds.isEmpty())
      throw new IllegalArgumentException("Explicit targets and a Rare addition action required");
    var slot =
        state.slots().stream()
            .filter(s -> s.id().equals(slotId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown slot or modifier"));
    if (!slot.desecrated() || slot.modifier() != null)
      throw new IllegalArgumentException("Comparison starts from an unrevealed slot");
    var targetFamilies =
        targetIds.stream()
            .flatMap(
                id ->
                    catalog
                        .find(id)
                        .orElseThrow(() -> new IllegalArgumentException("Unknown slot or modifier"))
                        .familyIds()
                        .stream())
            .collect(Collectors.toSet());
    var baselinePool = pool(state, action, omen);
    if (baselinePool.isEmpty()) return Optional.empty();
    var baseline = chance(baselinePool, targetIds);
    var alternatives = new ArrayList<Comparison>();
    for (var blocker : catalog.modifiers().values()) {
      if (blocker.layer() != ModifierDefinition.Layer.EXPLICIT
          || blocker.affixType() != slot.side()
          || blocker.requiredItemLevel() > state.item().itemLevel()
          || blocker.weight() <= 0
          || !Collections.disjoint(blocker.familyIds(), targetFamilies)) continue;
      // Minimum values are only a valid concrete carrier for this family/slot scenario.
      // They do not model the roll or reveal distribution and never become a selected item.
      var values =
          blocker.stats().stream()
              .collect(
                  Collectors.toMap(
                      ModifierDefinition.StatRange::id, ModifierDefinition.StatRange::min));
      var slots =
          state.slots().stream()
              .map(
                  s ->
                      s.id().equals(slotId)
                          ? new AbyssCraftingState.Slot(
                              s.id(),
                              s.side(),
                              new ModifierInstance(blocker.id(), values),
                              true,
                              s.minimumModifierLevel(),
                              s.revealPool())
                          : s)
              .toList();
      var scenario = new AbyssCraftingState(state.item(), slots, null);
      try {
        scenario.validate(catalog);
      } catch (IllegalArgumentException unsupported) {
        continue;
      }
      var conditionalPool = pool(scenario, action, omen);
      if (conditionalPool.isEmpty()) continue;
      alternatives.add(
          new Comparison(
              baseline, chance(conditionalPool, targetIds), blocker.id(), blocker.weight()));
    }
    // Highest individual option weight, not an invented maximum reveal-success probability.
    return alternatives.stream()
        .sorted(
            Comparator.comparingInt(Comparison::blockerWeight)
                .reversed()
                .thenComparing(Comparison::blockerModifierId))
        .findFirst();
  }

  public List<ModifierDefinition> pool(
      AbyssCraftingState state, WorkbenchCurrency action, WorkbenchOmen omen) {
    state.validate(catalog);
    var item = state.knownItem();
    var bucket =
        new StateBucket(
            item.snapshotId(),
            item.baseItemId(),
            item.itemLevel(),
            item.rarity(),
            item.implicits(),
            item.explicits().stream().map(ModifierInstance::modifierId).toList(),
            item.conditions());
    var capacity = new ItemStateValidator(catalog).slots(item);
    var plan = additions.plan(bucket, action, omen == null ? Set.of() : Set.of(omen.id()));
    if (!plan.available()) return List.of();
    return plan.candidates().stream()
        .filter(
            d ->
                state.count(d.affixType())
                    < (d.affixType() == AffixType.PREFIX
                        ? capacity.maxPrefixes()
                        : capacity.maxSuffixes()))
        .toList();
  }

  private Chance chance(List<ModifierDefinition> pool, Set<String> targets) {
    return new Chance(
        pool.stream()
            .filter(d -> targets.contains(d.id()))
            .mapToLong(ModifierDefinition::weight)
            .sum(),
        pool.stream().mapToLong(ModifierDefinition::weight).sum());
  }
}
