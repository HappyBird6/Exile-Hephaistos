package com.poe2craft.craftingsupport.path.application;

import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.crafting.domain.BasicCurrencyTransitions;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import java.util.function.BooleanSupplier;

/**
 * 명시적으로 지원하는 고정 제작 경로와 실패 후 복구 확률을 계산한다.
 *
 * <p>기존 계약: Explicit fixed policies only. Recovery probabilities are conditional on the supplied
 * failure.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code LIMITS}: 한도목록 ({@code Limits}).
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code transitions}: 전이목록 ({@code BasicCurrencyTransitions}).
 *   <li>{@code cache}: 캐시 ({@code BasicTransitionCache}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code BasicPathService(catalog, identity)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       catalog}(카탈로그), {@code identity}(정체성).
 *   <li>{@code provenance()}: 출처이력 값을 조회·구성한다. 반환: {@code BasicCurrencyState.Provenance}.
 *   <li>{@code calculate(request, recovery, cancelled)}: 해당 모형의 계산 결과를 구한다. 입력: {@code
 *       request}(요청), {@code recovery}(복구), {@code cancelled}(취소여부). 반환: {@code Result}. 사용하는 예외:
 *       {@code IllegalArgumentException}.
 *   <li>{@code validate(state)}: 입력의 구조·값·지원 조건을 검증한다. 입력: {@code state}(상태). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code PolicyMode}: {@code BasicPathService}의 정책·모드 구분값을 정의한다.
 *   <li>{@code Policy}: {@code BasicPathService}에서 사용하는 정책의 불변 값이다.
 *   <li>{@code Target}: {@code BasicPathService}에서 사용하는 대상의 불변 값이다.
 *   <li>{@code Request}: {@code BasicPathService}에서 사용하는 요청의 불변 값이다.
 *   <li>{@code Blocker}: {@code BasicPathService}에서 사용하는 Blocker의 불변 값이다.
 *   <li>{@code Limits}: {@code BasicPathService}에서 사용하는 한도목록의 불변 값이다.
 *   <li>{@code Result}: {@code BasicPathService}에서 사용하는 결과의 불변 값이다.
 * </ul>
 */
public final class BasicPathService {
  /**
   * {@code BasicPathService}의 정책·모드 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code SINGLE_PASS}: PASS 구분값.
   *   <li>{@code REPEAT_CYCLE}: 주기 구분값.
   * </ul>
   */
  public enum PolicyMode {
    SINGLE_PASS,
    REPEAT_CYCLE
  }

  /**
   * {@code BasicPathService}에서 사용하는 정책의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code actions}: 행동목록 ({@code List<WorkbenchCurrency>}).
   *   <li>{@code mode}: 모드 ({@code PolicyMode}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Policy(actions, mode)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code actions}(행동목록), {@code
   *       mode}(모드). 사용하는 예외: {@code IllegalArgumentException}.
   * </ul>
   */
  public record Policy(List<WorkbenchCurrency> actions, PolicyMode mode) {
    public Policy {
      if (actions == null || actions.isEmpty() || actions.size() > 32 || mode == null)
        throw new IllegalArgumentException("Explicit policy with one to 32 actions required");
      if (actions.stream().anyMatch(Objects::isNull))
        throw new IllegalArgumentException("Non-null policy actions required");
      actions = List.copyOf(actions);
      if (!BasicCurrencyTransitions.supportedActions().containsAll(actions))
        throw new IllegalArgumentException("Policy action is outside the basic transition model");
    }
  }

  /**
   * {@code BasicPathService}에서 사용하는 대상의 불변 값이다.
   *
   * <p>기존 계약: Exact checkpoint OR presence of explicit modifier IDs; numeric goals are not
   * inferred.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code checkpoint}: 체크포인트 ({@code BasicCurrencyState}).
   *   <li>{@code explicitModifierIds}: 명시옵션·옵션·식별자목록 ({@code Set<String>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Target(checkpoint, explicitModifierIds)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       checkpoint}(체크포인트), {@code explicitModifierIds}(명시옵션·옵션·식별자목록). 사용하는 예외: {@code
   *       IllegalArgumentException}.
   *   <li>{@code matches(state)}: 일치결과 처리에서 {@code checkpoint.equals}, {@code state.item}을(를) 사용한다.
   *       입력: {@code state}(상태). 반환: {@code boolean}.
   * </ul>
   */
  public record Target(BasicCurrencyState checkpoint, Set<String> explicitModifierIds) {
    public Target {
      if (explicitModifierIds == null)
        throw new IllegalArgumentException("Explicit target required");
      explicitModifierIds = Set.copyOf(explicitModifierIds);
      if ((checkpoint == null) == explicitModifierIds.isEmpty())
        throw new IllegalArgumentException(
            "Specify checkpoint or nonempty modifier IDs, exclusively");
    }

    boolean matches(BasicCurrencyState state) {
      if (checkpoint != null) return checkpoint.equals(state);
      return state.item().explicits().stream()
          .map(ModifierInstance::modifierId)
          .toList()
          .containsAll(explicitModifierIds);
    }
  }

  /**
   * {@code BasicPathService}에서 사용하는 요청의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code start}: 시작값 ({@code BasicCurrencyState}).
   *   <li>{@code policy}: 정책 ({@code Policy}).
   *   <li>{@code target}: 대상 ({@code Target}).
   *   <li>{@code activeOmens}: 현재 실행에서 고려할 징조 ID 목록 ({@code Set<String>}).
   *   <li>{@code observations}: 관측값목록 ({@code List<Long>}).
   * </ul>
   */
  public record Request(
      BasicCurrencyState start,
      Policy policy,
      Target target,
      Set<String> activeOmens,
      List<Long> observations) {}

  /**
   * {@code BasicPathService}에서 사용하는 Blocker의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code BasicCurrencyState}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code WorkbenchCurrency}).
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   * </ul>
   */
  public record Blocker(
      BasicCurrencyState state, WorkbenchCurrency action, String status, String reason) {}

  /**
   * {@code BasicPathService}에서 사용하는 한도목록의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code evaluations}: 평가목록 ({@code long}).
   *   <li>{@code frontierStates}: 미탐색경계·상태목록 ({@code int}).
   *   <li>{@code fractionBits}: 유리수·비트수 ({@code int}).
   *   <li>{@code elementaryOutcomes}: 기본·결과목록 ({@code long}).
   * </ul>
   */
  public record Limits(
      long evaluations, int frontierStates, int fractionBits, long elementaryOutcomes) {}

  public static final Limits LIMITS = new Limits(10000, 1000, 65536, 1000);

  /**
   * {@code BasicPathService}에서 사용하는 결과의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code purpose}: 목적 ({@code String}).
   *   <li>{@code start}: 시작값 ({@code BasicCurrencyState}).
   *   <li>{@code policy}: 정책 ({@code Policy}).
   *   <li>{@code target}: 대상 ({@code Target}).
   *   <li>{@code provenance}: 출처이력 ({@code BasicCurrencyState.Provenance}).
   *   <li>{@code interpretation}: 해석 ({@code String}).
   *   <li>{@code distribution}: 확률분포 ({@code FirstHitCalculator.Result}).
   *   <li>{@code blockers}: 차단사유목록 ({@code List<Blocker>}).
   *   <li>{@code blockersTruncated}: 차단사유목록·Truncated ({@code boolean}).
   *   <li>{@code recoveryIncludedInMain}: 복구·Included·입력·Main ({@code boolean}).
   *   <li>{@code renewalProof}: 갱신·증명 ({@code BasicCurrencyTransitions.ChaosRenewal}).
   * </ul>
   */
  public record Result(
      String purpose,
      BasicCurrencyState start,
      Policy policy,
      Target target,
      BasicCurrencyState.Provenance provenance,
      String interpretation,
      FirstHitCalculator.Result distribution,
      List<Blocker> blockers,
      boolean blockersTruncated,
      boolean recoveryIncludedInMain,
      BasicCurrencyTransitions.ChaosRenewal renewalProof) {}

  private final ItemCatalog catalog;
  private final BasicCurrencyTransitions transitions;
  private final BasicTransitionCache cache;

  public BasicPathService(ItemCatalog catalog, String identity) {
    this.catalog = catalog;
    transitions = new BasicCurrencyTransitions(catalog, identity);
    cache = new BasicTransitionCache(transitions, 32, 20000);
  }

  public BasicCurrencyState.Provenance provenance() {
    return transitions.provenance();
  }

  public Result calculate(Request request, boolean recovery, BooleanSupplier cancelled) {
    if (request == null
        || request.policy() == null
        || request.target() == null
        || request.activeOmens() == null)
      throw new IllegalArgumentException("Explicit path request required");
    validate(request.start());
    if (request.target().checkpoint() != null) validate(request.target().checkpoint());
    for (var id : request.target().explicitModifierIds()) {
      var definition = catalog.modifiers().get(id);
      if (definition == null || definition.layer() != ModifierDefinition.Layer.EXPLICIT)
        throw new IllegalArgumentException(
            "Target modifier is not in the current explicit catalog");
    }
    FirstHitCalculator.observationPoints(request.observations());
    if (request.policy().mode() == PolicyMode.REPEAT_CYCLE
        && request.policy().actions().equals(List.of(WorkbenchCurrency.CHAOS))
        && request.activeOmens().isEmpty()
        && request.target().checkpoint() == null
        && request.target().explicitModifierIds().size() == 1) {
      var proof =
          transitions.proveSingleExplicitChaos(
              request.start(), request.target().explicitModifierIds().iterator().next());
      if (proof.isPresent()) {
        var renewal = proof.get();
        var distribution =
            ChaosRenewalCalculator.calculate(
                renewal.probability(),
                request.target().matches(request.start()),
                request.observations(),
                LIMITS.fractionBits(),
                cancelled);
        return new Result(
            recovery ? "CONDITIONAL_RECOVERY" : "MAIN_FIRST_HIT",
            request.start(),
            request.policy(),
            request.target(),
            provenance(),
            BasicCurrencyTransitions.INTERPRETATION,
            distribution,
            List.of(),
            false,
            false,
            renewal);
      }
    }
    var blockers = new ArrayList<Blocker>();
    boolean[] truncated = {false};
    var distribution =
        new FirstHitCalculator<BasicCurrencyState>()
            .calculate(
                request.start(),
                request.observations(),
                state ->
                    request.target().matches(state)
                        ? FirstHitCalculator.Match.HIT
                        : FirstHitCalculator.Match.MISS,
                (state, step) -> {
                  var action =
                      request
                          .policy()
                          .actions()
                          .get((int) (step % request.policy().actions().size()));
                  var expanded =
                      cache.get(state, action, request.activeOmens(), LIMITS.elementaryOutcomes());
                  if (expanded.status() != BasicCurrencyTransitions.Status.COMPLETE) {
                    if (blockers.size() < 32)
                      blockers.add(
                          new Blocker(state, action, expanded.status().name(), expanded.reason()));
                    else truncated[0] = true;
                  }
                  if (expanded.status() == BasicCurrencyTransitions.Status.UNAVAILABLE)
                    return new FirstHitCalculator.Kernel<>(List.of(), Fraction.ZERO, true);
                  return new FirstHitCalculator.Kernel<>(
                      expanded.outcomes().stream()
                          .map(
                              outcome ->
                                  new FirstHitCalculator.Edge<>(
                                      outcome.state(), outcome.probability()))
                          .toList(),
                      expanded.unresolved(),
                      false);
                },
                new FirstHitCalculator.Budget(
                    LIMITS.evaluations(), LIMITS.frontierStates(), LIMITS.fractionBits()),
                cancelled,
                step ->
                    request.policy().mode() == PolicyMode.SINGLE_PASS
                        && step >= request.policy().actions().size());
    return new Result(
        recovery ? "CONDITIONAL_RECOVERY" : "MAIN_FIRST_HIT",
        request.start(),
        request.policy(),
        request.target(),
        provenance(),
        BasicCurrencyTransitions.INTERPRETATION,
        distribution,
        List.copyOf(blockers),
        truncated[0],
        false,
        null);
  }

  private void validate(BasicCurrencyState state) {
    if (state == null || !provenance().equals(state.provenance()))
      throw new IllegalArgumentException("Current full-state provenance required");
    if (!new ItemStateValidator(catalog).validate(state.item()).isEmpty())
      throw new IllegalArgumentException("Invalid concrete item state");
  }
}
