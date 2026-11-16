package com.poe2craft.craftingsupport.rag;

import com.fasterxml.jackson.databind.*;
import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.rag.application.*;
import com.poe2craft.craftingsupport.rag.infrastructure.BundledStrategyKnowledge;
import com.poe2craft.craftingsupport.search.application.*;
import com.poe2craft.craftingsupport.search.domain.*;
import com.poe2craft.craftingsupport.search.infrastructure.AutomaticCurrencyPolicyLoader;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.ItemState;
import java.time.*;
import java.util.*;
import java.util.function.Function;

public final class RagFixture {
  public final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  public final BasicCurrencyFixture tiny = new BasicCurrencyFixture();
  public final AutomaticCurrencyPolicy policy = AutomaticCurrencyPolicyLoader.load();
  public final StrategyKnowledge knowledge = BundledStrategyKnowledge.load(json);
  public final ItemState root = tiny.item(ItemState.Rarity.RARE, List.of());
  public final AutomaticCraftingKernel kernel =
      new AutomaticCraftingKernel(
          policy,
          tiny.catalog,
          new WorkbenchSimulator(tiny.catalog, new CraftingEngine(tiny.catalog)),
          "amulets");
  public final Function<ItemState, AutomaticMethodGraph.Progress> evaluate =
      item -> {
        var hit = item.explicits().stream().anyMatch(m -> m.modifierId().equals("a"));
        var status = hit ? GoalFilter.Status.MATCH : GoalFilter.Status.NO_MATCH;
        return new AutomaticMethodGraph.Progress(status, List.of(status));
      };
  public final AutomaticRuntimeFactory runtimes =
      new AutomaticRuntimeFactory() {
        @Override
        public AutomaticSearchService.Runtime create(MethodSearchProtocol.Create request) {
          return new AutomaticSearchService.Runtime(kernel, evaluate, "test-goal");
        }

        @Override
        public AutomaticSearchService.Runtime historical(MethodSearchProtocol.Create request) {
          throw new AssertionError();
        }
      };

  public BackendSettings.SupportRag settings(Map<String, Object> overrides) {
    var tree =
        (com.fasterxml.jackson.databind.node.ObjectNode)
            json.valueToTree(BackendSettings.defaults().supportRag());
    tree.put("enabled", true);
    overrides.forEach((k, v) -> tree.set(k, json.valueToTree(v)));
    try {
      return json.treeToValue(tree, BackendSettings.SupportRag.class);
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  public RagComparisonService.Input input() {
    var goal =
        new GoalFilter(
            1,
            "test-goal",
            new GoalFilter.General(
                root.baseItemId(),
                new GoalFilter.Range(null, null),
                List.of(ItemState.Rarity.RARE)),
            List.of());
    return new RagComparisonService.Input(
        new MethodSearchProtocol.Create(
            2, UUID.randomUUID().toString(), tiny.state(root), goal, List.of()),
        true,
        0,
        StrategyKnowledge.Mode.ALL,
        "ko");
  }

  public StrategyEvidence.Facts facts() {
    var inventory = AutomaticCraftingAction.candidates(policy);
    return new StrategyEvidence(
            kernel,
            evaluate,
            inventory,
            20000,
            System.nanoTime() + Duration.ofSeconds(10).toNanos())
        .build(root, true, 0, inventory);
  }

  public List<GoalDirectedPlanner.Choice> expected() {
    return new GoalDirectedPlanner(kernel, evaluate, AutomaticCraftingAction.candidates(policy))
        .choices(root);
  }

  public StrategyModel.Result answer(StrategyEvidence.Facts facts) {
    var ids = new HashMap<AutomaticCraftingAction, String>();
    facts.actions().forEach(f -> ids.put(f.action(), f.id()));
    return new StrategyModel.Result(
        "COMPLETE",
        expected().stream()
            .map(
                c ->
                    new StrategyModel.Proposal(
                        c.initial().stream().map(ids::get).toList(),
                        c.cycle().stream().map(ids::get).toList(),
                        List.of("support.common-policy", "support.first-affix-chaos"),
                        c.initial().stream()
                                    .anyMatch(a -> a.currency() == WorkbenchCurrency.EXALTED)
                                && c.cycle().getFirst().currency() != WorkbenchCurrency.CHAOS
                            ? "REQUIRE_REPLACEMENT"
                            : "STOP_ON_FIRST_HIT",
                        "문서 기반 테스트 전략"))
            .toList(),
        100,
        20);
  }

  public RagComparisonService service(StrategyModel model, Map<String, Object> settings) {
    return new RagComparisonService(
        runtimes, policy, knowledge, model, settings(settings), json, Clock.systemUTC());
  }
}
