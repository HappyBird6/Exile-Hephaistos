package com.poe2craft.craftingsupport.path.api;

import com.poe2craft.craftingsupport.path.application.GraphExplorer;
import com.poe2craft.craftingsupport.path.application.TransitionCache;
import com.poe2craft.shared.crafting.domain.CraftingAction;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.StateBucket;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * 기존 행동 순서 기반 확률 그래프 API를 제공한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code engine}: 계산 실행기 ({@code CraftingEngine}).
 *   <li>{@code transitions}: 전이목록 ({@code TransitionCache}).
 *   <li>{@code explorer}: explorer ({@code GraphExplorer}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code LegacyGraphController(engine, transitions, explorer)}: 전달받은 값으로 구성원을 초기화한다. 입력:
 *       {@code engine}(계산엔진), {@code transitions}(전이목록), {@code explorer}(explorer).
 *   <li>{@code actions(state)}: 현재 상태에서 가능한 행동과 불가 이유를 조회한다. 입력: {@code state}(상태). 반환: {@code
 *       List<CraftingEngine.Availability>}.
 *   <li>{@code transitions(request)}: 가능한 다음 상태와 확률을 구한다. 입력: {@code request}(요청). 반환: {@code
 *       CraftingEngine.TransitionResult}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code explore(request)}: explore 처리에서 {@code engine.validate}, {@code request.state}을(를)
 *       사용한다. 입력: {@code request}(요청). 반환: {@code GraphExplorer.Result}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code TransitionRequest}: {@code LegacyGraphController}에서 사용하는 전이·요청의 불변 값이다.
 *   <li>{@code ExploreRequest}: {@code LegacyGraphController}에서 사용하는 Explore·요청의 불변 값이다.
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/crafting")
public final class LegacyGraphController {
  private final CraftingEngine engine;
  private final TransitionCache transitions;
  private final GraphExplorer explorer;

  public LegacyGraphController(
      CraftingEngine engine, TransitionCache transitions, GraphExplorer explorer) {
    this.engine = engine;
    this.transitions = transitions;
    this.explorer = explorer;
  }

  @PostMapping("/actions")
  public List<CraftingEngine.Availability> actions(@RequestBody StateBucket state) {
    return engine.actions(state);
  }

  @PostMapping("/transitions")
  public CraftingEngine.TransitionResult transitions(@RequestBody TransitionRequest request) {
    if (request.state() == null || request.action() == null)
      throw new IllegalArgumentException("State and action required");
    return transitions.get(request.state(), request.action());
  }

  @PostMapping("/explore")
  public GraphExplorer.Result explore(@RequestBody ExploreRequest request) {
    engine.validate(request.state());
    if (request.maxMillis() < 10 || request.maxMillis() > 1500)
      throw new IllegalArgumentException("Invalid time limit");
    long deadline = System.nanoTime() + request.maxMillis() * 1_000_000L;
    return explorer.explore(
        request.state(),
        request.plan(),
        request.maxNodes(),
        request.maxEdges(),
        () -> System.nanoTime() >= deadline || Thread.currentThread().isInterrupted());
  }

  /**
   * {@code LegacyGraphController}에서 사용하는 전이·요청의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code StateBucket}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code CraftingAction}).
   * </ul>
   */
  public record TransitionRequest(StateBucket state, CraftingAction action) {}

  /**
   * {@code LegacyGraphController}에서 사용하는 Explore·요청의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code StateBucket}).
   *   <li>{@code plan}: 계획 ({@code List<CraftingAction>}).
   *   <li>{@code maxNodes}: 최대값·노드목록 ({@code int}).
   *   <li>{@code maxEdges}: 최대값·간선목록 ({@code int}).
   *   <li>{@code maxMillis}: 최대값·밀리초 ({@code int}).
   * </ul>
   */
  public record ExploreRequest(
      StateBucket state, List<CraftingAction> plan, int maxNodes, int maxEdges, int maxMillis) {}
}
