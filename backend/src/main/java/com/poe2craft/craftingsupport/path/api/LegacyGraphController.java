package com.poe2craft.craftingsupport.path.api;

import com.poe2craft.craftingsupport.path.application.GraphExplorer;
import com.poe2craft.craftingsupport.path.application.TransitionCache;
import com.poe2craft.shared.crafting.domain.CraftingAction;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.StateBucket;
import java.util.List;
import org.springframework.web.bind.annotation.*;

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

  public record TransitionRequest(StateBucket state, CraftingAction action) {}

  public record ExploreRequest(
      StateBucket state, List<CraftingAction> plan, int maxNodes, int maxEdges, int maxMillis) {}
}
