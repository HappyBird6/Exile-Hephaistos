package com.poe2craft.crafting.presentation;

import com.poe2craft.crafting.application.GraphExplorer;
import com.poe2craft.crafting.application.TransitionCache;
import com.poe2craft.crafting.domain.CraftingAction;
import com.poe2craft.crafting.domain.CraftingEngine;
import com.poe2craft.crafting.domain.StateBucket;
import com.poe2craft.item.ItemCatalog;
import com.poe2craft.item.ModifierDefinition;
import com.poe2craft.item.SolarAmulet;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crafting")
public final class CraftingController {
  private final ItemCatalog catalog;
  private final CraftingEngine engine;
  private final TransitionCache transitions;
  private final GraphExplorer explorer;

  public CraftingController(
      ItemCatalog catalog,
      CraftingEngine engine,
      TransitionCache transitions,
      GraphExplorer explorer) {
    this.catalog = catalog;
    this.engine = engine;
    this.transitions = transitions;
    this.explorer = explorer;
  }

  @GetMapping("/initial")
  public Initial initial(@RequestParam(defaultValue = "82") int itemLevel) {
    var bucket = StateBucket.from(SolarAmulet.initial(catalog, itemLevel, 15));
    return new Initial(
        CraftingEngine.RULE_VERSION,
        catalog.metadata(),
        bucket.id(),
        bucket,
        catalog.modifiers(),
        engine.actions(bucket));
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

  public record Initial(
      String ruleVersion,
      ItemCatalog.Metadata metadata,
      String id,
      StateBucket state,
      Map<String, ModifierDefinition> modifiers,
      List<CraftingEngine.Availability> actions) {}

  public record TransitionRequest(StateBucket state, CraftingAction action) {}

  public record ExploreRequest(
      StateBucket state, List<CraftingAction> plan, int maxNodes, int maxEdges, int maxMillis) {}
}
