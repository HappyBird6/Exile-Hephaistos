package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.BasicJewel;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ManualRevealCatalog;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import java.util.*;
import java.util.random.RandomGenerator;

/**
 * 구체 상태 제작과 미공개 슬롯 제작을 공통 화폐 실행에 연결한다.
 *
 * <p>기존 계약: Slot-aware adapter around the existing concrete and Abyss samplers.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code simulator}: 공통 제작 규칙 실행기 ({@code WorkbenchSimulator}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AbyssCurrencyExecutor(catalog, simulator)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       catalog}(카탈로그), {@code simulator}(제작시뮬레이터).
 *   <li>{@code apply(state, action, activeOmens, random)}: 주어진 행동 또는 변경을 적용한 결과를 구한다. 입력: {@code
 *       state}(상태), {@code action}(행동), {@code activeOmens}(활성·징조목록), {@code random}(난수생성기). 반환:
 *       {@code Result}.
 *   <li>{@code exaltChoices(state, action, activeOmens)}: 엑잘티드·선택목록 처리에서 {@code state.validate},
 *       {@code state.item}을(를) 사용한다. 입력: {@code state}(상태), {@code action}(행동), {@code
 *       activeOmens}(활성·징조목록). 반환: {@code List<WorkbenchSimulator.ExaltChoice>}.
 *   <li>{@code applyChosenExalt(state, action, activeOmens, modifierId, random)}: Chosen·엑잘티드을(를)
 *       적용한다. 입력: {@code state}(상태), {@code action}(행동), {@code activeOmens}(활성·징조목록), {@code
 *       modifierId}(옵션·식별자), {@code random}(난수생성기). 반환: {@code Transition}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code exaltTransition(state, action, omens, random, selected)}: 엑잘티드·전이의 입력에 따른 분기·결과를
 *       처리한다. 입력: {@code state}(상태), {@code action}(행동), {@code omens}(징조목록), {@code
 *       random}(난수생성기), {@code selected}(선택된). 반환: {@code Transition}.
 *   <li>{@code concreteTransition(state, action, omens, random, selected)}: 구체값·전이 처리에서 {@code
 *       simulator.apply}, {@code state.knownItem}을(를) 사용한다. 입력: {@code state}(상태), {@code
 *       action}(행동), {@code omens}(징조목록), {@code random}(난수생성기), {@code selected}(선택된). 반환: {@code
 *       Transition}.
 *   <li>{@code hiddenTransition(state, action, omens, random, selected)}: 미공개·전이 처리에서 {@code
 *       simulator.triggeredOmens}, {@code simulator.abyssPlan}을(를) 사용한다. 입력: {@code state}(상태),
 *       {@code action}(행동), {@code omens}(징조목록), {@code random}(난수생성기), {@code selected}(선택된). 반환:
 *       {@code Transition}.
 *   <li>{@code transition(next, action, reason, events, assumptions, consumed, remaining)}: 전이 처리에서
 *       {@code next.validate}, {@code WorkbenchSimulator.Result}을(를) 사용한다. 입력: {@code next}(다음),
 *       {@code action}(행동), {@code reason}(이유), {@code events}(이벤트목록), {@code assumptions}(계산가정),
 *       {@code consumed}(소비된), {@code remaining}(남은). 반환: {@code Transition}.
 *   <li>{@code hasHidden(state)}: 미공개을(를) 보유 여부를 판정한다. 입력: {@code state}(상태). 반환: {@code boolean}.
 *   <li>{@code requireExalt(action)}: 엑잘티드을(를) 필수 조건을 확인한다. 입력: {@code action}(행동). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code blocked(state, action, omens, reason)}: 차단 처리에서 {@code omens.stream}을(를) 사용한다. 입력:
 *       {@code state}(상태), {@code action}(행동), {@code omens}(징조목록), {@code reason}(이유). 반환: {@code
 *       Result}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Result}: {@code AbyssCurrencyExecutor}에서 사용하는 결과의 불변 값이다.
 *   <li>{@code Transition}: {@code AbyssCurrencyExecutor}에서 사용하는 전이의 불변 값이다.
 * </ul>
 */
public final class AbyssCurrencyExecutor {
  private final ItemCatalog catalog;
  private final WorkbenchSimulator simulator;

  public AbyssCurrencyExecutor(ItemCatalog catalog, WorkbenchSimulator simulator) {
    this.catalog = ManualRevealCatalog.enrich(Objects.requireNonNull(catalog));
    this.simulator = Objects.requireNonNull(simulator);
  }

  /**
   * {@code AbyssCurrencyExecutor}에서 사용하는 결과의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code AbyssCraftingState}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code WorkbenchCurrency}).
   *   <li>{@code applied}: 요청한 변경의 실제 적용 여부 ({@code boolean}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code consumedOmens}: 이번 실행으로 소비된 징조 ID 목록 ({@code List<String>}).
   *   <li>{@code remainingOmens}: 실행 뒤 남은 징조 ID 목록 ({@code List<String>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Result(state, action, applied, reason, consumedOmens, remainingOmens)}: 전달받은 값으로
   *       구성원을 초기화한다. 입력: {@code state}(상태), {@code action}(행동), {@code applied}(적용여부), {@code
   *       reason}(이유), {@code consumedOmens}(소비된·징조목록), {@code remainingOmens}(남은·징조목록).
   * </ul>
   */
  public record Result(
      AbyssCraftingState state,
      WorkbenchCurrency action,
      boolean applied,
      String reason,
      List<String> consumedOmens,
      List<String> remainingOmens) {
    public Result {
      consumedOmens = List.copyOf(consumedOmens);
      remainingOmens = List.copyOf(remainingOmens);
    }
  }

  public Result apply(
      AbyssCraftingState state,
      WorkbenchCurrency action,
      Set<String> activeOmens,
      RandomGenerator random) {
    Objects.requireNonNull(state);
    Objects.requireNonNull(action);
    Objects.requireNonNull(random);
    var omens = activeOmens == null ? Set.<String>of() : Set.copyOf(activeOmens);
    state.validate(catalog);
    if (!state.item().conditions().isEmpty())
      return blocked(state, action, omens, "ITEM_CONDITION");
    if (action == WorkbenchCurrency.ANNULMENT && omens.contains(WorkbenchOmen.LIGHT.id())) {
      if (omens.stream().map(WorkbenchOmen::fromId).filter(o -> o.trigger() == action).count() != 1)
        return blocked(state, action, omens, "ABYSS_OMEN_COMBINATION_UNSUPPORTED");
      var candidates =
          state.removalCandidates(null).stream()
              .filter(AbyssCraftingState.Slot::desecrated)
              .toList();
      if (candidates.isEmpty()) return blocked(state, action, omens, "NO_ELIGIBLE_REMOVAL");
      var removed = candidates.get(random.nextInt(candidates.size()));
      var next = state.remove(removed.id(), null, catalog);
      var remaining = new TreeSet<>(omens);
      remaining.remove(WorkbenchOmen.LIGHT.id());
      return new Result(
          next, action, true, "", List.of(WorkbenchOmen.LIGHT.id()), List.copyOf(remaining));
    }
    boolean hidden = state.slots().stream().anyMatch(s -> s.modifier() == null);
    boolean valuesOnly =
        action == WorkbenchCurrency.DIVINE
            || QualityInfuserRules.supports(action)
            || QualityCurrencyRules.isQuality(action)
            || action == WorkbenchCurrency.ARTIFICER
            || action.catalystType() != null;
    try {
      if (action.baseAction() == CraftingAction.EXALTED)
        return exaltTransition(state, action, omens, random, null).outcome();
      // 미공개 슬롯은 분열의 사용 조건에 필요하므로 실제 슬롯을 보존한 상태로 작업대에 전달한다.
      if (hidden
          && action == WorkbenchCurrency.FRACTURING
          && state.offer() == null
          && state.slots().stream().noneMatch(s -> s.desecrated() && s.modifier() != null))
        return concreteTransition(state, action, omens, random, null).outcome();
      // The concrete sampler knows existing item values, quality, socket and omen rules.
      // Hidden slots are opaque and can only bypass it for operations that cannot change slots.
      if (!hidden || valuesOnly)
        return concreteTransition(state, action, omens, random, null).outcome();
      // Known slot-changing rules already count hidden slots, protect fractures and enforce
      // families.
      var matching = simulator.triggeredOmens(action, omens);
      if (action.baseAction() == CraftingAction.CHAOS) {
        var effects = matching.stream().map(WorkbenchOmen::fromId).toList();
        if (effects.contains(WorkbenchOmen.WHITTLING))
          return blocked(state, action, omens, "UNREVEALED_MODIFIER_LEVEL_UNKNOWN");
        if (effects.size() > 1
            || effects.stream()
                .anyMatch(o -> action.minimumModifierLevel() > 0 && !o.supportsTieredCurrency()))
          return blocked(state, action, omens, "ABYSS_OMEN_COMBINATION_UNSUPPORTED");
        if (state.offer() != null)
          return blocked(state, action, omens, "RESOLVE_OFFER_BEFORE_CURRENCY");
        var side = effects.isEmpty() ? null : effects.getFirst().affix();
        var locked = BasicJewel.chaosLockedSides(state.storedItem(), catalog);
        var removals =
            state.removalCandidates(side).stream()
                .filter(slot -> !locked.contains(slot.side()))
                .toList();
        if (removals.isEmpty()) return blocked(state, action, omens, "NO_ELIGIBLE_REMOVAL");
        var addition =
            action == WorkbenchCurrency.GREATER_CHAOS
                ? WorkbenchCurrency.GREATER_EXALTED
                : action == WorkbenchCurrency.PERFECT_CHAOS
                    ? WorkbenchCurrency.PERFECT_EXALTED
                    : WorkbenchCurrency.EXALTED;
        var pools = new ArrayList<List<ModifierDefinition>>();
        for (var removal : removals) {
          var remaining = state.remove(removal.id(), side, catalog);
          var pool = new AbyssProbabilityComparison(catalog).pool(remaining, addition, null);
          if (pool.isEmpty()) return blocked(state, action, omens, "NO_ELIGIBLE_ADDITION");
          pools.add(pool);
        }
        int selected = random.nextInt(removals.size());
        var removal = removals.get(selected);
        var remaining = state.remove(removal.id(), side, catalog);
        var pool = pools.get(selected);
        long draw = random.nextLong(pool.stream().mapToLong(ModifierDefinition::weight).sum());
        var chosen = pool.getLast();
        for (var candidate : pool) {
          draw -= candidate.weight();
          if (draw < 0) {
            chosen = candidate;
            break;
          }
        }
        // A valid concrete carrier delegates numeric and coupled rolls to the existing sampler.
        // It never supplies values for any unrevealed slot.
        var values = new HashMap<String, Long>();
        chosen.stats().forEach(s -> values.put(s.id(), s.min()));
        var carrier =
            AbyssCraftingState.withExplicits(
                state.item(), List.of(new ModifierInstance(chosen.id(), values)));
        var instance = carrier.explicits().getFirst();
        if (chosen.stats().stream().anyMatch(s -> s.min() != s.max())) {
          var rolled = simulator.apply(carrier, WorkbenchCurrency.DIVINE, Set.of(), random);
          if (!rolled.applied()) return blocked(state, action, omens, "NUMERIC_ROLL_UNAVAILABLE");
          instance = rolled.state().explicits().getFirst();
        }
        var next = remaining.append(removal.id(), instance, catalog);
        return new Result(
            next,
            action,
            true,
            "Simulator policy: uniform eligible removal, then published conditional modifier weights; reveal odds are unknown.",
            List.copyOf(matching),
            omens.stream().filter(id -> !matching.contains(id)).sorted().toList());
      }
      return hiddenTransition(state, action, omens, random, null).outcome();
    } catch (IllegalArgumentException unsupported) {
      // The original immutable state and every omen survive a rejected transition.
      return blocked(state, action, omens, "ABYSS_CURRENCY_UNAVAILABLE");
    }
  }

  /**
   * {@code AbyssCurrencyExecutor}에서 사용하는 전이의 불변 값이다.
   *
   * <p>기존 계약: Receipt and slot state are produced atomically from the same accepted operation.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code outcome}: 결과 ({@code Result}).
   *   <li>{@code receipt}: receipt ({@code WorkbenchSimulator.Result}).
   * </ul>
   */
  public record Transition(Result outcome, WorkbenchSimulator.Result receipt) {}

  public List<WorkbenchSimulator.ExaltChoice> exaltChoices(
      AbyssCraftingState state, WorkbenchCurrency action, Set<String> activeOmens) {
    requireExalt(action);
    state.validate(catalog);
    if (!state.item().conditions().isEmpty()) return List.of();
    if (!hasHidden(state)) return simulator.exaltChoices(state.knownItem(), action, activeOmens);
    var plan = simulator.abyssPlan(state, action, simulator.triggeredOmens(action, activeOmens));
    if (!plan.available()) return List.of();
    var candidates = plan.branches().getFirst().candidates();
    double total = candidates.stream().mapToLong(ModifierDefinition::weight).sum();
    return candidates.stream()
        .map(d -> new WorkbenchSimulator.ExaltChoice(d.id(), d.weight() / total))
        .toList();
  }

  public Transition applyChosenExalt(
      AbyssCraftingState state,
      WorkbenchCurrency action,
      Set<String> activeOmens,
      String modifierId,
      RandomGenerator random) {
    requireExalt(action);
    if (modifierId == null
        || exaltChoices(state, action, activeOmens).stream()
            .noneMatch(c -> c.modifierId().equals(modifierId)))
      throw new IllegalArgumentException("Choose an eligible Exalted modifier");
    return exaltTransition(
        state, action, activeOmens == null ? Set.of() : activeOmens, random, modifierId);
  }

  private Transition exaltTransition(
      AbyssCraftingState state,
      WorkbenchCurrency action,
      Set<String> omens,
      RandomGenerator random,
      String selected) {
    return hasHidden(state)
        ? hiddenTransition(state, action, omens, random, selected)
        : concreteTransition(state, action, omens, random, selected);
  }

  private Transition concreteTransition(
      AbyssCraftingState state,
      WorkbenchCurrency action,
      Set<String> omens,
      RandomGenerator random,
      String selected) {
    var input =
        action == WorkbenchCurrency.FRACTURING && hasHidden(state)
            ? state.storedItem()
            : state.knownItem();
    var result =
        selected == null
            ? simulator.apply(input, action, omens, random)
            : simulator.applyCustomExalt(input, action, omens, random, selected);
    if (!result.applied())
      return new Transition(blocked(state, action, omens, result.reason()), null);
    var removed =
        result.events().stream()
            .filter(e -> e.kind().equals("REMOVE"))
            .map(WorkbenchSimulator.Event::modifierId)
            .collect(java.util.stream.Collectors.toSet());
    var next = state.withKnownItem(result.state(), removed, catalog);
    return transition(
        next,
        action,
        result.reason(),
        result.events(),
        result.assumptions(),
        result.consumedOmens(),
        result.remainingOmens());
  }

  private Transition hiddenTransition(
      AbyssCraftingState state,
      WorkbenchCurrency action,
      Set<String> omens,
      RandomGenerator random,
      String selected) {
    var matching = simulator.triggeredOmens(action, omens);
    var plan = simulator.abyssPlan(state, action, matching);
    if (!plan.available())
      return new Transition(blocked(state, action, omens, plan.reason()), null);
    var next =
        selected == null
            ? simulator.applyAbyss(state, action, matching, random)
            : simulator.applyCustomExalt(state, action, matching, random, selected);
    var consumed = matching.stream().sorted().toList();
    var remaining = omens.stream().filter(id -> !matching.contains(id)).sorted().toList();
    var events = new ArrayList<WorkbenchSimulator.Event>();
    for (var m : next.knownItem().explicits())
      if (state.knownItem().explicits().stream()
          .noneMatch(old -> old.modifierId().equals(m.modifierId()))) {
        var candidates = plan.branches().getFirst().candidates();
        double total = candidates.stream().mapToLong(ModifierDefinition::weight).sum();
        double probability =
            plan.branches().getFirst().uniformSelection()
                ? 1.0 / candidates.size()
                : candidates.stream()
                        .filter(d -> d.id().equals(m.modifierId()))
                        .mapToLong(ModifierDefinition::weight)
                        .sum()
                    / total;
        events.add(new WorkbenchSimulator.Event("ADD", m.modifierId(), m.values(), probability));
      }
    return transition(next, action, "", events, List.of(), consumed, remaining);
  }

  private Transition transition(
      AbyssCraftingState next,
      WorkbenchCurrency action,
      String reason,
      List<WorkbenchSimulator.Event> events,
      List<WorkbenchSimulator.Assumption> assumptions,
      List<String> consumed,
      List<String> remaining) {
    next.validate(catalog);
    var outcome = new Result(next, action, true, reason, consumed, remaining);
    var receipt =
        new WorkbenchSimulator.Result(
            simulator.ruleVersion(),
            simulator.ledgerVersion(),
            next.item().snapshotId(),
            next.storedItem(),
            action,
            true,
            reason,
            events,
            assumptions,
            consumed,
            remaining,
            QualityLimitRules.describe(next.storedItem(), catalog));
    return new Transition(outcome, receipt);
  }

  private static boolean hasHidden(AbyssCraftingState state) {
    return state.slots().stream().anyMatch(s -> s.modifier() == null);
  }

  private static void requireExalt(WorkbenchCurrency action) {
    if (action == null || action.baseAction() != CraftingAction.EXALTED)
      throw new IllegalArgumentException("Exalted currency required");
  }

  private static Result blocked(
      AbyssCraftingState state, WorkbenchCurrency action, Set<String> omens, String reason) {
    return new Result(state, action, false, reason, List.of(), omens.stream().sorted().toList());
  }
}
