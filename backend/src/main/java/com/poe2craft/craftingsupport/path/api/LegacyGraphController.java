package com.poe2craft.craftingsupport.path.api;

import com.poe2craft.craftingsupport.path.api.dto.ExploreRequest;
import com.poe2craft.craftingsupport.path.api.dto.TransitionRequest;
import com.poe2craft.craftingsupport.path.application.GraphExplorer;
import com.poe2craft.craftingsupport.path.application.TransitionCache;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.StateBucket;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * 기존 제작 그래프의 HTTP 진입점이다. actions는 engine에서 가능한 행동을, transitions는 전이 캐시에서 행동 결과를 조회한다. explore는 시작
 * 상태 검증 후 explorer에 경로 탐색을 요청한다.
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
    return explorer.explore(
        request.state(),
        request.plan(),
        request.maxNodes(),
        request.maxEdges(),
        request.maxMillis());
  }
}
