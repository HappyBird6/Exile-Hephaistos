package com.poe2craft.bootstrap;

import static com.poe2craft.bootstrap.GoalDirectedPlannerScenariosTest.*;
import static org.assertj.core.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.infrastructure.BundledGoalCatalogs;
import com.poe2craft.craftingsupport.goal.infrastructure.GoalDefinitionsLoader;
import com.poe2craft.craftingsupport.search.api.AutomaticSearchController;
import com.poe2craft.craftingsupport.search.api.PathSearchErrors;
import com.poe2craft.craftingsupport.search.application.AutomaticSearchService;
import com.poe2craft.craftingsupport.search.application.MethodSearchProtocol;
import com.poe2craft.craftingsupport.search.application.MethodSearchService;
import com.poe2craft.craftingsupport.search.domain.CertifiedInterval;
import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner;
import com.poe2craft.craftingsupport.search.domain.GoalDirectedPlanner;
import com.poe2craft.shared.crafting.domain.AutomaticCraftingAction;
import com.poe2craft.shared.crafting.domain.AutomaticCurrencyPolicy;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.probability.Fraction;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

/**
 * Six·접두접미옵션·Scenarios 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code f}: f ({@code GoalDirectedPlannerScenariosTest}).
 *   <li>{@code directory}: 디렉터리 ({@code Path}).
 *   <li>{@code STATS}: 수치목록 ({@code List<String>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code root(count)}: 시작점 처리에서 {@code STATS.subList}, {@code catalog.modifiers}을(를) 사용한다.
 *       입력: {@code count}(개수). 반환: {@code ItemState}.
 *   <li>{@code productionAcceptsFiveAndSixStartsAndRejectsSevenOrInvalidRarity()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code priorFourAffixPolicyAndGoalCatalogRemainReadOnlyWithoutChangingSavedBytes()}: 이름에
 *       명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code fiveAndSixGoalsFromFourAffixesReachTheTargetWithFallbackExalts()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 *   <li>{@code sixGoalCancellationResumeAndReloadPreserveResults()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를
 *       기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code sixGoalsFromMagicLifeStayBoundedAndOrDoesNotRequireEveryEntry()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다.
 * </ul>
 */
@Timeout(90)
class SixAffixScenariosTest {
  final GoalDirectedPlannerScenariosTest f = new GoalDirectedPlannerScenariosTest();
  @TempDir Path directory;
  static final List<String> STATS =
      List.of(
          LIFE,
          "base_maximum_mana",
          "base_maximum_energy_shield",
          CHAOS,
          COLD,
          "base_fire_damage_resistance_%");

  ItemState root(int count) {
    var modifiers =
        STATS.subList(0, count).stream()
            .map(
                stat -> {
                  var definition =
                      catalog.modifiers().values().stream()
                          .filter(
                              m -> m.stats().size() == 1 && m.stats().getFirst().id().equals(stat))
                          .findFirst()
                          .orElseThrow();
                  return new ModifierInstance(
                      definition.id(), Map.of(stat, definition.stats().getFirst().min()));
                })
            .toList();
    return f.item(ItemState.Rarity.RARE, 82, modifiers);
  }

  /** 3접두·3접미에서 분열 접두를 남기고, 양쪽 제한 및 2회 제거 분포와 신규 목표를 채울 경로를 검증한다. */
  @Test
  void annulmentAndOmensRemoveOnlyEligibleAffixesAndOfferARefill() {
    var mods = new ArrayList<>(root(6).explicits());
    var locked = mods.getFirst();
    mods.set(0, new ModifierInstance(locked.modifierId(), locked.values(), true));
    var item = f.item(ItemState.Rarity.RARE, 82, mods);
    var goal = f.goal(item, Map.of("base_lightning_damage_resistance_%", 1L));
    var kernel = f.kernel(item, goal);
    for (var omens :
        List.of(
            Set.<WorkbenchOmen>of(),
            Set.of(WorkbenchOmen.SINISTRAL_ANNULMENT),
            Set.of(WorkbenchOmen.DEXTRAL_ANNULMENT),
            Set.of(WorkbenchOmen.GREATER_ANNULMENT),
            Set.of(WorkbenchOmen.DEXTRAL_ANNULMENT, WorkbenchOmen.GREATER_ANNULMENT))) {
      var action =
          new AutomaticCraftingAction(
              WorkbenchCurrency.ANNULMENT, omens, AutomaticCurrencyPolicy.Purpose.ORDINARY);
      var law = kernel.expand(item, action, 5000);
      int eligible =
          omens.contains(WorkbenchOmen.SINISTRAL_ANNULMENT)
              ? 2
              : omens.contains(WorkbenchOmen.DEXTRAL_ANNULMENT) ? 3 : 5;
      boolean doubleRemoval = omens.contains(WorkbenchOmen.GREATER_ANNULMENT);
      int combinations = doubleRemoval ? eligible * (eligible - 1) / 2 : eligible;
      assertThat(law.outcomes()).hasSize(combinations);
      for (var o : law.outcomes()) {
        assertThat(o.probability().denominator().intValueExact()).isEqualTo(combinations);
        assertThat(o.probability().numerator().intValueExact()).isEqualTo(1);
        assertThat(o.item().explicits()).hasSize(doubleRemoval ? 4 : 5).contains(mods.getFirst());
        var removed = mods.stream().filter(m -> !o.item().explicits().contains(m)).toList();
        if (omens.contains(WorkbenchOmen.DEXTRAL_ANNULMENT)
            || omens.contains(WorkbenchOmen.SINISTRAL_ANNULMENT))
          assertThat(removed)
              .allMatch(
                  m ->
                      catalog.find(m.modifierId()).orElseThrow().affixType()
                          == (omens.contains(WorkbenchOmen.DEXTRAL_ANNULMENT)
                              ? ModifierDefinition.AffixType.SUFFIX
                              : ModifierDefinition.AffixType.PREFIX));
      }
    }
    var choices =
        new GoalDirectedPlanner(
                kernel, f.evaluation(item, goal), AutomaticCraftingAction.candidates(policy))
            .choices(item);
    var pair =
        choices.stream()
            .filter(
                c ->
                    !c.initial().isEmpty()
                        && c.initial().getFirst().currency() == WorkbenchCurrency.ANNULMENT
                        && c.initial()
                            .getFirst()
                            .omens()
                            .equals(Set.of(WorkbenchOmen.DEXTRAL_ANNULMENT))
                        && c.cycle().getFirst().currency() == WorkbenchCurrency.EXALTED)
            .findFirst()
            .orElseThrow();
    var evaluate = f.evaluation(item, goal);
    var total = Fraction.ZERO;
    var success = Fraction.ZERO;
    for (var removal : kernel.expand(item, pair.initial().getFirst(), 5000).outcomes()) {
      for (var addition :
          kernel
              .expandDeferred(removal.item(), Map.of(), pair.cycle().getFirst(), 5000)
              .outcomes()) {
        assertThat(addition.item().explicits()).hasSize(6).contains(mods.getFirst());
        var probability = removal.probability().multiply(addition.probability());
        total = total.add(probability);
        if (evaluate.apply(addition.item()).goal() == GoalFilter.Status.MATCH)
          success = success.add(probability);
      }
    }
    assertThat(total).isEqualTo(Fraction.ONE);
    assertThat(success).isNotEqualTo(Fraction.ZERO).isNotEqualTo(Fraction.ONE);
    try (var graph = f.graph(item, goal)) {
      f.exhaust(graph);
      assertThat(graph.complete()).isTrue();
      var method =
          graph.methods().stream()
              .filter(
                  m ->
                      m.source() == 0
                          && m.template().initial().equals(pair.initial())
                          && m.template().cycle().equals(pair.cycle()))
              .findFirst()
              .orElseThrow();
      var goalMass =
          method.points().get(1).exits().get(ExactMethodRunner.Exit.GOAL).values().stream()
              .reduce(CertifiedInterval.zero(64), CertifiedInterval::add);
      assertThat(goalMass.contains(success)).isTrue();
    }
  }

  @Test
  void productionAcceptsFiveAndSixStartsAndRejectsSevenOrInvalidRarity() throws Exception {
    var json = new ObjectMapper();
    try (var legacy =
            new MethodSearchService(workbench.reviewedCatalogs(), goals, "six-affix", json);
        var service =
            new CraftingSupportConfiguration()
                .automaticSearchService(workbench, goals, legacy, json, directory.toString())) {
      var mvc =
          org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup(
                  new AutomaticSearchController(service, json))
              .setControllerAdvice(new PathSearchErrors())
              .build();
      var owner = "11111111-1111-4111-8111-111111111111";
      for (int count : List.of(5, 6)) {
        var item = root(count);
        var requirements = new LinkedHashMap<String, Long>();
        STATS.subList(0, count).forEach(s -> requirements.put(s, 1L));
        var goal = f.goal(item, requirements);
        assertThat(goals.minimumExplicitModifiers(item, goal)).isEqualTo(count);
        var request =
            new MethodSearchProtocol.Create(
                2,
                "start-" + count,
                new BasicCurrencyState(item, legacy.provenance(item.baseItemId())),
                goal,
                List.of());
        var payload = json.valueToTree(new AutomaticSearchService.Input(request, false));
        if (count == 5)
          ((com.fasterxml.jackson.databind.node.ObjectNode)
                  payload.path("request").path("start").path("item"))
              .remove(
                  "desecrated"); // Existing clients and stored scenarios omit the optional field.
        var response =
            mvc.perform(
                    org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                            "/api/v2/crafting/method-searches/automatic")
                        .cookie(new jakarta.servlet.http.Cookie("crafting_method_client", owner))
                        .contentType("application/json")
                        .content(json.writeValueAsString(payload)))
                .andExpect(
                    org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                        .isAccepted())
                .andReturn()
                .getResponse();
        var view =
            json.readValue(response.getContentAsByteArray(), AutomaticSearchService.View.class);
        assertThat(view.nodes().getFirst().item().explicits()).hasSize(count);
        assertThat(service.get(owner, view.id()).nodes()).isEqualTo(view.nodes());
      }
      var valid = root(6);
      var seven = new ArrayList<>(valid.explicits());
      seven.add(f.life());
      var fourPrefixes = new ArrayList<>(root(3).explicits());
      fourPrefixes.add(
          new ModifierInstance("amulet:prefix:adept-s", Map.of("spell_damage_+%", 8L)));
      for (var invalid :
          List.of(
              f.item(ItemState.Rarity.RARE, 82, seven),
              f.item(ItemState.Rarity.RARE, 82, fourPrefixes),
              f.item(ItemState.Rarity.MAGIC, 82, valid.explicits()),
              f.item(ItemState.Rarity.RARE, 82, List.of(f.life(), f.life())))) {
        var request =
            new MethodSearchProtocol.Create(
                2,
                UUID.randomUUID().toString(),
                new BasicCurrencyState(invalid, legacy.provenance(invalid.baseItemId())),
                f.goal(valid, Map.of(LIFE, 1L)),
                List.of());
        assertThatThrownBy(() -> service.create("six", request)).hasMessage("INVALID_ITEM");
      }
    }
  }

  @Test
  void priorFourAffixPolicyAndGoalCatalogRemainReadOnlyWithoutChangingSavedBytes()
      throws Exception {
    var json = new ObjectMapper();
    var previous =
        "reviewed-equipment-20261009-v2-c303fc1ea724ddf2ccca61f7c9512e6c9fdcd997cb9d4348fe5a5f8766f07d5a";
    var oldCatalog =
        new GoalCatalogIndex(
            BundledGoalCatalogs.load(workbench), GoalDefinitionsLoader.load(), previous);
    var productionGoals = new CraftingSupportConfiguration().goalFilterService(workbench);
    String id;
    try (var legacy =
            new MethodSearchService(workbench.reviewedCatalogs(), goals, "six-affix", json);
        var service =
            new CraftingSupportConfiguration()
                .automaticSearchService(workbench, goals, legacy, json, directory.toString())) {
      var item = root(4);
      var request =
          new MethodSearchProtocol.Create(
              2,
              "legacy-four",
              new BasicCurrencyState(item, legacy.provenance(item.baseItemId())),
              f.goal(item, Map.of(LIFE, 1L)),
              List.of());
      id = service.create("legacy", request).id();
    }
    var path = directory.resolve("automatic-v1").resolve(id + ".json");
    var disk = (com.fasterxml.jackson.databind.node.ObjectNode) json.readTree(path.toFile());
    ((com.fasterxml.jackson.databind.node.ObjectNode) disk.path("graph").path("identity"))
        .put("policyVersion", "approved-187-2026-10-21-rarity-v4");
    ((com.fasterxml.jackson.databind.node.ObjectNode) disk.path("request").path("goal"))
        .put("catalogVersion", oldCatalog.version());
    ((com.fasterxml.jackson.databind.node.ObjectNode) disk.path("graph"))
        .put("objective", json.writeValueAsString(disk.path("request").path("goal")));
    json.writeValue(path.toFile(), disk);
    var before = java.nio.file.Files.readAllBytes(path);
    try (var legacy =
            new MethodSearchService(
                workbench.reviewedCatalogs(), productionGoals, "six-affix", json);
        var service =
            new CraftingSupportConfiguration()
                .automaticSearchService(
                    workbench, productionGoals, legacy, json, directory.toString())) {
      var view = service.get("legacy", id);
      assertThat(view.reason()).isEqualTo("LEGACY_READ_ONLY");
      assertThat(view.canResume()).isFalse();
      for (var operation : List.of("ADVANCE", "RESUME", "CANCEL"))
        assertThatThrownBy(
                () ->
                    service.control(
                        "legacy",
                        id,
                        new AutomaticSearchService.Control(
                            operation, view.revision(), view.generation(), operation)))
            .hasMessage("RECALCULATION_REQUIRED");
    }
    assertThat(java.nio.file.Files.readAllBytes(path)).containsExactly(before);
  }

  @Test
  void fiveAndSixGoalsFromFourAffixesReachTheTargetWithFallbackExalts() {
    for (int count : List.of(5, 6)) {
      var item = root(4);
      var requirements = new LinkedHashMap<String, Long>();
      STATS.subList(0, count).forEach(s -> requirements.put(s, 1L));
      var goal = f.goal(item, requirements);
      long started = System.nanoTime();
      try (var graph = f.graph(item, goal)) {
        f.exhaust(graph);
        System.out.println(
            "SIX_AFFIX|goal="
                + count
                + "|complete="
                + graph.complete()
                + "|hasGoal="
                + graph.hasGoal()
                + "|resources="
                + graph.resources()
                + "|ms="
                + (System.nanoTime() - started) / 1000000);
        assertThat(graph.hasGoal()).isTrue();
        assertThat(graph.methods())
            .anyMatch(
                m ->
                    m.template().cycle().stream()
                        .anyMatch(
                            a ->
                                a.purpose()
                                    == AutomaticCurrencyPolicy.Purpose.LAST_RESORT_EXALTED));
        assertThat(graph.nodes().size()).isGreaterThan(1);
        assertThat(graph.nodes()).allMatch(n -> n.item().explicits().size() <= 6);
        assertThat(graph.nodes())
            .allMatch(n -> new ItemStateValidator(catalog).validate(n.item()).isEmpty());
        if (graph.resourceLimited()) assertThat(graph.complete()).isFalse();
      }
    }
  }

  @Test
  void sixGoalCancellationResumeAndReloadPreserveResults() throws Exception {
    var json = new ObjectMapper();
    var item = root(4);
    var requirements = new LinkedHashMap<String, Long>();
    STATS.forEach(stat -> requirements.put(stat, 1L));
    var goal = f.goal(item, requirements);
    AutomaticSearchService.View result;
    MethodSearchProtocol.Create request;
    try (var legacy =
            new MethodSearchService(workbench.reviewedCatalogs(), goals, "six-affix", json);
        var service =
            new CraftingSupportConfiguration()
                .automaticSearchService(workbench, goals, legacy, json, directory.toString())) {
      request =
          new MethodSearchProtocol.Create(
              2,
              "six-resume",
              new BasicCurrencyState(item, legacy.provenance(item.baseItemId())),
              goal,
              List.of());
      var view = service.create("resume", request);
      view =
          service.control(
              "resume",
              view.id(),
              new AutomaticSearchService.Control(
                  "cancel", view.revision(), view.generation(), "CANCEL"));
      assertThat(view.status()).isEqualTo("CANCELLED");
      view =
          service.control(
              "resume",
              view.id(),
              new AutomaticSearchService.Control(
                  "resume", view.revision(), view.generation(), "RESUME"));
      long maxMillis = 0;
      for (int i = 0; i < 100 && view.status().equals("READY"); i++) {
        long started = System.nanoTime();
        view =
            service.control(
                "resume",
                view.id(),
                new AutomaticSearchService.Control(
                    "advance-" + i, view.revision(), view.generation(), "ADVANCE"));
        maxMillis = Math.max(maxMillis, (System.nanoTime() - started) / 1000000);
      }
      assertThat(view.nodes().size()).isGreaterThan(1);
      assertThat(view.nodes()).anyMatch(n -> n.progress().goal() == GoalFilter.Status.MATCH);
      assertThat(view.nodes()).allMatch(n -> n.item().explicitSlotCount() <= 6);
      result = view;
      System.out.println(
          "SIX_API|status="
              + view.status()
              + "|maxAdvanceMs="
              + maxMillis
              + "|resources="
              + view.resources());
    }
    try (var legacy =
            new MethodSearchService(workbench.reviewedCatalogs(), goals, "six-affix", json);
        var service =
            new CraftingSupportConfiguration()
                .automaticSearchService(workbench, goals, legacy, json, directory.toString())) {
      var restored = service.get("resume", result.id());
      assertThat(restored.nodes()).isEqualTo(result.nodes());
      assertThat(restored.methods()).isEqualTo(result.methods());
    }
    var output = Path.of("build/six-affix-fixtures");
    java.nio.file.Files.createDirectories(output);
    json.writeValue(output.resolve("six-goal-request.json").toFile(), request);
    json.writeValue(output.resolve("six-goal-view.json").toFile(), result);
  }

  @Test
  void sixGoalsFromMagicLifeStayBoundedAndOrDoesNotRequireEveryEntry() {
    var item = f.item(ItemState.Rarity.MAGIC, 82, List.of(f.life()));
    var requirements = new LinkedHashMap<String, Long>();
    STATS.forEach(stat -> requirements.put(stat, 1L));
    var goal = f.goal(item, requirements);
    long started = System.nanoTime();
    try (var graph = f.graph(item, goal)) {
      f.exhaust(graph);
      System.out.println(
          "SIX_FROM_ONE|complete="
              + graph.complete()
              + "|hasGoal="
              + graph.hasGoal()
              + "|resources="
              + graph.resources()
              + "|ms="
              + (System.nanoTime() - started) / 1000000);
      assertThat(graph.finished() || graph.resourceLimited()).isTrue();
      assertThat(graph.nodes())
          .allMatch(n -> new ItemStateValidator(catalog).validate(n.item()).isEmpty());
      if (graph.resourceLimited()) assertThat(graph.complete()).isFalse();
    }
    var group = goal.groups().getFirst();
    var alternative =
        new GoalFilter(
            goal.version(),
            goal.catalogVersion(),
            goal.general(),
            List.of(
                new GoalFilter.Group(
                    group.id(),
                    GoalFilter.Type.COUNT,
                    false,
                    new GoalFilter.Range(java.math.BigDecimal.ONE, null),
                    group.entries())));
    assertThat(goals.minimumExplicitModifiers(item, alternative)).isZero();
    assertThat(f.evaluation(item, alternative).apply(root(5)).goal())
        .isEqualTo(GoalFilter.Status.MATCH);
  }
}
