package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.crafting.domain.AutomaticCraftingAction;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 훼손 상태의 조건부 분포를 확장한다. 공개 후보의 발생 확률은 가정하지 않는다.
 *
 * <p>기존 계약: Factored conditional state expansion. No law over reveal offers is supplied or implied.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code simulator}: 공통 제작 규칙 실행기 ({@code WorkbenchSimulator}).
 *   <li>{@code inventory}: 등록자료 ({@code List<AutomaticCraftingAction>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AutomaticAbyssSearch(catalog, simulator, inventory)}: 전달받은 값으로 구성원을 초기화한다. 입력:
 *       {@code catalog}(카탈로그), {@code simulator}(제작시뮬레이터), {@code inventory}(등록자료).
 *   <li>{@code expand(strategies)}: 다음 상태 또는 분포 구간을 확장한다. 입력: {@code strategies}(전략목록). 반환: {@code
 *       List<Route>}.
 *   <li>{@code expand(source, currency, omens)}: 다음 상태 또는 분포 구간을 확장한다. 입력: {@code source}(출처),
 *       {@code currency}(화폐), {@code omens}(징조목록). 반환: {@code Transition}.
 *   <li>{@code nextSlotId(state)}: 다음·슬롯·식별자 처리에서 {@code state.slots}, {@code Collectors.toSet}을(를)
 *       사용한다. 입력: {@code state}(상태). 반환: {@code String}.
 *   <li>{@code retainedRecords(routes)}: 보유·기록목록 처리에서 {@code routes.stream}, {@code r.assumed}을(를)
 *       사용한다. 입력: {@code routes}(routes). 반환: {@code long}.
 *   <li>{@code next(state)}: 다음 처리에서 {@code seen.add}, {@code action.currency}을(를) 사용한다. 입력: {@code
 *       state}(상태). 반환: {@code List<Next>}.
 *   <li>{@code retainedRanges(source, state)}: 보유·범위목록 처리에서 {@code state.slots}, {@code
 *       Collectors.toSet}을(를) 사용한다. 입력: {@code source}(출처), {@code state}(상태). 반환: {@code
 *       Map<String, List<Range>>}.
 *   <li>{@code ranges(definition)}: 범위목록 처리에서 {@code definition.stats}, {@code s.id}을(를) 사용한다. 입력:
 *       {@code definition}(정의). 반환: {@code List<Range>}.
 *   <li>{@code carrier(definition)}: carrier 처리에서 {@code definition.id}, {@code
 *       definition.stats}을(를) 사용한다. 입력: {@code definition}(정의). 반환: {@code ModifierInstance}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Ratio}: {@code AutomaticAbyssSearch}에서 사용하는 비율의 불변 값이다.
 *   <li>{@code Range}: {@code AutomaticAbyssSearch}에서 사용하는 범위의 불변 값이다.
 *   <li>{@code State}: {@code AutomaticAbyssSearch}에서 사용하는 상태의 불변 값이다.
 *   <li>{@code Candidate}: {@code AutomaticAbyssSearch}에서 사용하는 후보의 불변 값이다.
 *   <li>{@code Removal}: {@code AutomaticAbyssSearch}에서 사용하는 제거의 불변 값이다.
 *   <li>{@code Next}: {@code AutomaticAbyssSearch}에서 사용하는 다음의 불변 값이다.
 *   <li>{@code Transition}: {@code AutomaticAbyssSearch}에서 사용하는 전이의 불변 값이다.
 *   <li>{@code Route}: {@code AutomaticAbyssSearch}에서 사용하는 경로의 불변 값이다.
 * </ul>
 */
public final class AutomaticAbyssSearch {
  /**
   * {@code AutomaticAbyssSearch}에서 사용하는 비율의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code numerator}: 분자 ({@code long}).
   *   <li>{@code denominator}: 분모 ({@code long}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Ratio(numerator, denominator)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code numerator}(분자),
   *       {@code denominator}(분모). 사용하는 예외: {@code IllegalArgumentException}.
   * </ul>
   */
  public record Ratio(long numerator, long denominator) {
    public Ratio {
      if (numerator < 0 || denominator <= 0 || numerator > denominator)
        throw new IllegalArgumentException("Invalid probability mass");
    }
  }

  /**
   * {@code AutomaticAbyssSearch}에서 사용하는 범위의 불변 값이다.
   *
   * <p>기존 계약: Ranges describe a family of states, not a rolled value or a reveal probability.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code stat}: 수치 ({@code String}).
   *   <li>{@code min}: 허용·표시 범위의 하한 ({@code long}).
   *   <li>{@code max}: 허용·표시 범위의 상한 ({@code long}).
   * </ul>
   */
  public record Range(String stat, long min, long max) {}

  /**
   * {@code AutomaticAbyssSearch}에서 사용하는 상태의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code item}: 아이템 ({@code AbyssCraftingState}).
   *   <li>{@code ranges}: 범위목록 ({@code Map<String, List<Range>>}).
   *   <li>{@code condition}: 조건 ({@code String}).
   * </ul>
   */
  public record State(AbyssCraftingState item, Map<String, List<Range>> ranges, String condition) {}

  /**
   * {@code AutomaticAbyssSearch}에서 사용하는 후보의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code modifierId}: 실제 부여된 옵션 정의 식별자 ({@code String}).
   *   <li>{@code selection}: 선택 ({@code Ratio}).
   *   <li>{@code result}: 결과 ({@code State}).
   *   <li>{@code next}: 다음 ({@code List<Next>}).
   * </ul>
   */
  public record Candidate(String modifierId, Ratio selection, State result, List<Next> next) {}

  /**
   * {@code AutomaticAbyssSearch}에서 사용하는 제거의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code slotId}: 슬롯·식별자 ({@code String}).
   *   <li>{@code probability}: 확률 ({@code Ratio}).
   *   <li>{@code remaining}: 남은 ({@code State}).
   *   <li>{@code candidates}: 후보목록 ({@code List<Candidate>}).
   * </ul>
   */
  public record Removal(
      String slotId, Ratio probability, State remaining, List<Candidate> candidates) {}

  /**
   * {@code AutomaticAbyssSearch}에서 사용하는 다음의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code currency}: 화폐 ({@code WorkbenchCurrency}).
   *   <li>{@code omens}: 징조목록 ({@code Set<String>}).
   *   <li>{@code removalBranches}: 제거·분기목록 ({@code int}).
   *   <li>{@code candidateCount}: 후보·개수 ({@code int}).
   * </ul>
   */
  public record Next(
      WorkbenchCurrency currency, Set<String> omens, int removalBranches, int candidateCount) {}

  /**
   * {@code AutomaticAbyssSearch}에서 사용하는 전이의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code source}: 출처 ({@code State}).
   *   <li>{@code currency}: 화폐 ({@code WorkbenchCurrency}).
   *   <li>{@code omens}: 징조목록 ({@code Set<String>}).
   *   <li>{@code removals}: removals ({@code List<Removal>}).
   * </ul>
   */
  public record Transition(
      State source, WorkbenchCurrency currency, Set<String> omens, List<Removal> removals) {}

  /**
   * {@code AutomaticAbyssSearch}에서 사용하는 경로의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code strategy}: 전략 ({@code AbyssStrategyPlanner.Strategy}).
   *   <li>{@code prepared}: 준비된 ({@code State}).
   *   <li>{@code assumed}: assumed ({@code State}).
   *   <li>{@code transitions}: 전이목록 ({@code List<Transition>}).
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   * </ul>
   */
  public record Route(
      AbyssStrategyPlanner.Strategy strategy,
      State prepared,
      State assumed,
      List<Transition> transitions,
      String status) {}

  private final ItemCatalog catalog;
  private final WorkbenchSimulator simulator;
  private final List<AutomaticCraftingAction> inventory;

  public AutomaticAbyssSearch(
      ItemCatalog catalog, WorkbenchSimulator simulator, List<AutomaticCraftingAction> inventory) {
    this.catalog = catalog;
    this.simulator = simulator;
    this.inventory = List.copyOf(inventory);
  }

  public List<Route> expand(List<AbyssStrategyPlanner.Strategy> strategies) {
    var result = new ArrayList<Route>();
    for (var strategy : strategies) {
      var prepared = new State(strategy.prepared(), Map.of(), "UNREVEALED_SLOT");
      var transitions = new ArrayList<Transition>();
      transitions.add(expand(prepared, strategy.nextCurrency(), strategy.activeOmens()));
      State assumed = null;
      if (strategy.modifierSelection() != null) {
        var blocker = catalog.find(strategy.modifierSelection().blockerModifierId()).orElseThrow();
        var slot =
            prepared.item().slots().stream()
                .filter(s -> s.desecrated() && s.modifier() == null)
                .findFirst()
                .orElseThrow();
        var modifier = carrier(blocker);
        var selected =
            prepared
                .item()
                .observe(slot.id(), List.of(modifier, modifier, modifier), catalog)
                .choose(0, catalog);
        assumed =
            new State(
                selected, Map.of(blocker.id(), ranges(blocker)), "IF_REVEAL_CHOOSES_MODIFIER");
        transitions.add(expand(assumed, strategy.nextCurrency(), strategy.activeOmens()));
      }
      result.add(
          new Route(strategy, prepared, assumed, List.copyOf(transitions), "CONDITIONAL_FRONTIER"));
    }
    return List.copyOf(result);
  }

  public Transition expand(State source, WorkbenchCurrency currency, Set<String> omens) {
    source.item().validate(catalog);
    var plan = simulator.abyssPlan(source.item(), currency, omens);
    var removals = new ArrayList<Removal>();
    if (!plan.available()) return new Transition(source, currency, omens, List.of());
    for (var branch : plan.branches()) {
      var remainingRanges = retainedRanges(source.ranges(), branch.remaining());
      var remaining = new State(branch.remaining(), remainingRanges, source.condition());
      long denominator =
          branch.uniformSelection()
              ? branch.candidates().size()
              : branch.candidates().stream().mapToLong(ModifierDefinition::weight).sum();
      var candidates = new ArrayList<Candidate>();
      for (var definition : branch.candidates()) {
        long numerator = branch.uniformSelection() ? 1 : definition.weight();
        if (numerator <= 0) continue;
        var appended =
            branch.remaining().append(nextSlotId(branch.remaining()), carrier(definition), catalog);
        var nextRanges = new LinkedHashMap<>(remainingRanges);
        nextRanges.put(definition.id(), ranges(definition));
        var state = new State(appended, Map.copyOf(nextRanges), source.condition());
        candidates.add(
            new Candidate(definition.id(), new Ratio(numerator, denominator), state, next(state)));
      }
      removals.add(
          new Removal(
              branch.removedSlotId(),
              new Ratio(1, plan.branches().size()),
              remaining,
              List.copyOf(candidates)));
    }
    return new Transition(source, currency, omens, List.copyOf(removals));
  }

  private static String nextSlotId(AbyssCraftingState state) {
    var ids = state.slots().stream().map(AbyssCraftingState.Slot::id).collect(Collectors.toSet());
    int index = 0;
    while (ids.contains("automatic-added-" + index)) index++;
    return "automatic-added-" + index;
  }

  public static long retainedRecords(List<Route> routes) {
    if (routes == null) return 0;
    return routes.stream()
        .mapToLong(
            r ->
                2
                    + (r.assumed() == null ? 0 : 1)
                    + r.transitions().stream()
                        .mapToLong(
                            t ->
                                1
                                    + t.removals().stream()
                                        .mapToLong(
                                            b ->
                                                1
                                                    + b.candidates().stream()
                                                        .mapToLong(c -> 1 + c.next().size())
                                                        .sum())
                                        .sum())
                        .sum())
        .sum();
  }

  private List<Next> next(State state) {
    var result = new ArrayList<Next>();
    var seen = new HashSet<String>();
    for (var action : inventory) {
      if (!seen.add(action.currency() + ":" + action.omenIds())) continue;
      // Range carriers must never decide a value-sensitive removal rule.
      if (!state.ranges().isEmpty() && action.omens().contains(WorkbenchOmen.WHITTLING)) continue;
      var plan = simulator.abyssPlan(state.item(), action.currency(), action.omenIds());
      if (plan.available())
        result.add(
            new Next(
                action.currency(),
                action.omenIds(),
                plan.branches().size(),
                plan.branches().stream().mapToInt(b -> b.candidates().size()).sum()));
    }
    return List.copyOf(result);
  }

  private static Map<String, List<Range>> retainedRanges(
      Map<String, List<Range>> source, AbyssCraftingState state) {
    var ids =
        state.slots().stream()
            .map(AbyssCraftingState.Slot::modifier)
            .filter(Objects::nonNull)
            .map(ModifierInstance::modifierId)
            .collect(Collectors.toSet());
    return source.entrySet().stream()
        .filter(e -> ids.contains(e.getKey()))
        .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static List<Range> ranges(ModifierDefinition definition) {
    return definition.stats().stream().map(s -> new Range(s.id(), s.min(), s.max())).toList();
  }

  private static ModifierInstance carrier(ModifierDefinition definition) {
    return new ModifierInstance(
        definition.id(),
        definition.stats().stream()
            .collect(
                Collectors.toMap(
                    ModifierDefinition.StatRange::id, ModifierDefinition.StatRange::min)));
  }
}
