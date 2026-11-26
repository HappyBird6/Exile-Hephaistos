package com.poe2craft.craftingsupport.goal.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.bootstrap.http.GameIdTransport;
import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.craftingsupport.goal.application.GoalFilterService;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.infrastructure.GoalDefinitionsLoader;
import com.poe2craft.craftingsupport.testroutes.application.TestRouteService;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.QualityLimitRules;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.ids.GameIdCodec;
import com.poe2craft.shared.ids.GameIdMappings;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.item.infrastructure.ItemCatalogLoader;
import com.poe2craft.shared.ruleset.api.RulesetBoundary;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** 실제 목표 판정·경로 계산·HTTP 읽기 경계에 같은 제작 시작 요청을 넣어 검증한다. */
class TestRouteControllerTest {
  private static final String URL = "/api/v1/crafting/support/goal-filters/test-routes";
  private static final String LIFE = "base_maximum_life";

  /** HTTP 입력에 사용하는 작업대와 같은 검증된 카탈로그다. */
  private final ItemCatalog catalog = ItemCatalogLoader.loadDefault();

  private final ObjectMapper json = new ObjectMapper();
  private final ObjectNode body;
  private final MockMvc mvc;

  TestRouteControllerTest() {
    var index = new GoalCatalogIndex(List.of(catalog), GoalDefinitionsLoader.load());
    var simulator = new WorkbenchSimulator(catalog, new CraftingEngine(catalog));
    var workbench = mock(CraftingOperations.class);
    when(workbench.baseSimulator(any())).thenReturn(simulator);
    var settings = BackendSettings.defaults();
    var service =
        new TestRouteService(workbench, new GoalFilterService(index), settings.testRoutes());
    mvc =
        MockMvcBuilders.standaloneSetup(new TestRouteController(service, json))
            .setControllerAdvice(new GoalFilterErrors(), new GameIdTransport(json, settings))
            .addFilters(new RulesetBoundary(json))
            .build();
    var initial = SolarAmulet.initial(catalog);
    var item =
        new ItemState(
            initial.snapshotId(),
            initial.baseItemId(),
            initial.itemLevel(),
            ItemState.Rarity.RARE,
            initial.implicits(),
            List.of(new ModifierInstance("amulet:prefix:rotund", Map.of(LIFE, 90L))),
            Set.of());
    var row =
        new GoalFilter.Entry(
            "life",
            GoalCatalogIndex.id("explicit", LIFE),
            "flat",
            new GoalFilter.Range(BigDecimal.valueOf(90), null),
            null,
            false);
    var goal =
        new GoalFilter(
            1,
            index.version(),
            new GoalFilter.General(
                item.baseItemId(),
                new GoalFilter.Range(null, null),
                List.of(ItemState.Rarity.RARE)),
            List.of(new GoalFilter.Group("group", GoalFilter.Type.AND, false, null, List.of(row))));
    body = json.valueToTree(Map.of("item", item, "goal", goal));
  }

  private ResultActions request(ObjectNode data) throws Exception {
    return mvc.perform(
        post(URL)
            .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(data)));
  }

  /** 양쪽 경계에 걸친 티어를 보존하고, 화면은 그 티어의 전체 수치 범위를 표시할 수 있다. */
  @Test
  void bothGoalBoundsIncludePartiallyOverlappingTiers() throws Exception {
    var input = body.deepCopy();
    ((ObjectNode) input.at("/goal/groups/0/entries/0/range")).put("min", 90).put("max", 111);
    request(input)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cards[0].options[0].hasLowerBound").value(true))
        .andExpect(jsonPath("$.cards[0].options[0].hasUpperBound").value(true))
        .andExpect(
            jsonPath(
                "$.cards[0].options[0].modifierIds",
                org.hamcrest.Matchers.hasItems("amulet:prefix:rotund", "amulet:prefix:virile")))
        .andExpect(
            jsonPath(
                "$.cards[0].options[0].modifierIds",
                org.hamcrest.Matchers.not(
                    org.hamcrest.Matchers.hasItem("amulet:prefix:athlete-s"))));
  }

  /** 82레벨 일반 태양의 목걸이에서 전체 방어 목표와 선택적인 명시 정신력 하한을 함께 보낸다. */
  private JsonNode solarDefenceRoutes(boolean withSpirit) throws Exception {
    var input = body.deepCopy();
    input.set("item", json.valueToTree(SolarAmulet.initial(catalog, 82, 15)));
    var entries = ((ObjectNode) input.at("/goal/groups/0")).putArray("entries");
    entries.add(
        json.<JsonNode>valueToTree(
            new GoalFilter.Entry(
                "defences",
                GoalCatalogIndex.id("explicit", "global_armour_evasion_energy_shield_+%"),
                "percent",
                new GoalFilter.Range(null, null),
                null,
                false)));
    if (withSpirit) {
      entries.add(
          json.<JsonNode>valueToTree(
              new GoalFilter.Entry(
                  "spirit",
                  GoalCatalogIndex.id("explicit", SolarAmulet.SPIRIT_STAT),
                  "flat",
                  new GoalFilter.Range(BigDecimal.valueOf(47), null),
                  null,
                  false)));
    }
    var response = request(input).andExpect(status().isOk()).andReturn().getResponse();
    return json.readTree(response.getContentAsByteArray());
  }

  /** 승급 결과에 상관없이 무징조 완벽한 강화 에센스가 유일한 목표를 확정으로 부여한다. */
  @Test
  void normalSolarAmuletHasGuaranteedGlobalDefencesAfterPromotion() throws Exception {
    var result = solarDefenceRoutes(false);
    assertThat(result.required("routes"))
        .anySatisfy(
            route -> {
              var steps = route.required("steps");
              assertThat(steps)
                  .extracting(s -> s.required("action").asText())
                  .containsExactly("TRANSMUTATION", "REGAL", "PERFECT_ESSENCE_ENHANCEMENT");
              assertThat(steps).allMatch(s -> s.required("omens").isEmpty());
              assertThat(route.required("probability").doubleValue()).isEqualTo(1d);
              assertThat(steps.get(2).at("/chance/probability").doubleValue()).isEqualTo(1d);
            });
  }

  /** 고정 정신력 +15와 별도로 명시 +47 이상과 전체 방어가 함께 붙은 세션에 확률을 반환한다. */
  @Test
  void normalSolarAmuletHasMeasuredRoutesForGlobalDefencesAndExplicitSpirit47() throws Exception {
    var result = solarDefenceRoutes(true);
    assertThat(result.required("routes"))
        .anySatisfy(
            route -> {
              var probability = route.required("probability");
              assertThat(probability.isNumber()).isTrue();
              assertThat(probability.doubleValue()).isGreaterThan(0d).isLessThanOrEqualTo(1d);
              var frames = route.required("stateIndices");
              var finalState =
                  result.required("states").required(frames.get(frames.size() - 1).intValue());
              assertThat(finalState.required("rarity").asText()).isEqualTo("RARE");
              assertThat(finalState.required("explicits"))
                  .anyMatch(m -> m.path("values").path(SolarAmulet.SPIRIT_STAT).asInt() >= 47)
                  .anyMatch(m -> m.path("values").has("global_armour_evasion_energy_shield_+%"));
            });
  }

  /** 기본 경로의 등급 하나만 바꾼 실제 HTTP 입력으로 선택 화폐와 결과 상태를 돌려받는다. */
  @Test
  void gradesOneExaltedStepWithoutReplacingTheMethodOrTrustingClientOdds() throws Exception {
    var input = body.deepCopy();
    ((ObjectNode) input.path("item")).putArray("explicits");
    ((ObjectNode) input.at("/goal/groups/0/entries/0/range")).putNull("min");
    input.put("cardIndex", 0);
    var step = input.putArray("steps").addObject();
    step.put("action", "GREATER_EXALTED");
    step.put("originalAction", "EXALTED");
    step.putArray("omens");
    step.putArray("acquired").add(1);
    step.putArray("lost");
    grade(input)
        .andExpect(status().isOk())
        .andExpect(header().string(RulesetBoundary.HEADER, RulesetBoundary.identity()))
        .andExpect(jsonPath("$.routes.length()").value(1))
        .andExpect(jsonPath("$.routes[0].steps[0].action").value("GREATER_EXALTED"))
        .andExpect(jsonPath("$.routes[0].steps[0].originalAction").doesNotExist())
        .andExpect(jsonPath("$.routes[0].steps[0].chance.probability").isNumber())
        .andExpect(jsonPath("$.routes[0].stateIndices.length()").value(1));
    step.put("originalAction", "CHAOS");
    grade(input).andExpect(status().isBadRequest());
    step.remove("originalAction");
    step.put("maxUses", 1000);
    grade(input).andExpect(status().isBadRequest());
    step.remove("maxUses");
    step.put("probability", 1);
    grade(input).andExpect(status().isBadRequest());
  }

  /** 합법적인 화폐여도 정해 둔 단계의 획득 기록과 맞지 않으면 다른 경로를 돌려주지 않는다. */
  @Test
  void unavailableFixedMethodDoesNotGenerateAlternativeRoutes() throws Exception {
    var input = body.deepCopy();
    ((ObjectNode) input.path("item")).putArray("explicits");
    input.put("cardIndex", 0);
    var step = input.putArray("steps").addObject();
    step.put("action", "PERFECT_REGAL");
    step.putArray("omens");
    step.putArray("acquired").add(1);
    step.putArray("lost");
    grade(input)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.complete").value(true))
        .andExpect(jsonPath("$.routes").isEmpty());
  }

  /** 같은 등급의 카오스라도 1회 적중과 최대 1,000회 내 적중을 구분해 반환한다. */
  @Test
  void repeatedChaosKeepsItsSingleUseOddsAndRecomputesConsumptionAtTheSelectedGrade()
      throws Exception {
    var input = body.deepCopy();
    ((ObjectNode) input.path("item"))
        .withArray("explicits")
        .removeAll()
        .add(
            json.valueToTree(
                new ModifierInstance(
                    "amulet:suffix:of-the-seal", Map.of("base_cold_damage_resistance_%", 6L))));
    ((ObjectNode) input.at("/goal/groups/0/entries/0/range")).putNull("min");
    input.put("cardIndex", 0);
    var step = input.putArray("steps").addObject();
    step.put("action", "GREATER_CHAOS").put("maxUses", 1000);
    step.putArray("omens");
    step.putArray("acquired").add(1);
    step.putArray("lost");
    var result =
        grade(input)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.routes.length()").value(1))
            .andExpect(jsonPath("$.routes[0].steps[0].maxUses").value(1000))
            .andReturn()
            .getResponse();
    var route = json.readTree(result.getContentAsByteArray()).at("/routes/0");
    var single = route.at("/steps/0/chance/probability").doubleValue();
    assertThat(single).isStrictlyBetween(0d, 1d);
    assertThat(route.get("probability").doubleValue())
        .isGreaterThan(single)
        .isLessThanOrEqualTo(1d);
    assertThat(route.at("/steps/0/expectedUses").doubleValue()).isBetween(1d, 1000d);
    step.put("maxUses", 1001);
    grade(input).andExpect(status().isBadRequest());
    step.remove("maxUses");
    step.put("repeatUntilHit", true).put("repeatTarget", 1);
    grade(input)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routes[0].steps[0].repeatUntilHit").value(true))
        .andExpect(jsonPath("$.routes[0].steps[0].repeatTarget").value(1))
        .andExpect(jsonPath("$.routes[0].steps[0].chance.probability").value(1d))
        .andExpect(jsonPath("$.routes[0].steps[0].maxUses").doesNotExist());
    step.put("repeatTarget", 2);
    grade(input).andExpect(status().isBadRequest());
  }

  /** 5개 기존 옵션과 마지막 미공개 접두 칸을 넣고, +85~99 생명력 한 티어에 해당하는 목표를 만든다. */
  private ObjectNode hiddenLifeRequest(int minimum, int maximum) {
    var input = body.deepCopy();
    var item = (ObjectNode) input.get("item");
    var mods = item.putArray("explicits");
    for (var stat :
        List.of(
            "base_maximum_mana",
            "base_maximum_energy_shield",
            "base_chaos_damage_resistance_%",
            "base_cold_damage_resistance_%",
            "base_fire_damage_resistance_%")) {
      var definition =
          catalog.modifiers().values().stream()
              .filter(
                  d ->
                      d.weight() > 0
                          && d.stats().size() == 1
                          && d.stats().getFirst().id().equals(stat))
              .min(java.util.Comparator.comparingInt(ModifierDefinition::requiredItemLevel))
              .orElseThrow();
      mods.add(
          json.valueToTree(
              new ModifierInstance(
                  definition.id(), Map.of(stat, definition.stats().getFirst().min()))));
    }
    item.putArray("desecrated")
        .addObject()
        .put("id", "hidden-prefix")
        .put("side", "PREFIX")
        .putNull("modifierId");
    ((ObjectNode) input.at("/goal/groups/0/entries/0/range"))
        .put("min", minimum)
        .put("max", maximum);
    return input;
  }

  /** 기존 슬롯에서는 적격한 생명력 티어를 한 번에 공개하고, 원래 다섯 옵션과 입력한 수치 기준을 보존한다. */
  @Test
  void revealsAtTheTierFloorAndUsesDivineForAnInteriorValue() throws Exception {
    var input = hiddenLifeRequest(85, 99);
    var data =
        json.readTree(
            request(input)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray());
    assertThat(data.at("/cards/0/options/0/modifierIds"))
        .extracting(id -> id.asText())
        .containsExactly("amulet:prefix:rotund");
    assertThat(data.required("routes"))
        .allSatisfy(
            route ->
                assertThat(route.required("steps"))
                    .allMatch(step -> !step.path("omens").toString().contains("Omen_of_Light")));
    var direct =
        java.util.stream.StreamSupport.stream(data.required("routes").spliterator(), false)
            .filter(
                r ->
                    r.path("steps").size() == 1
                        && r.at("/steps/0/action").asText().equals("REVEAL"))
            .findFirst()
            .orElseThrow();
    assertThat(direct.required("probability").doubleValue()).isEqualTo(1d);
    assertThat(direct.at("/steps/0/chance/probability").doubleValue()).isEqualTo(1d);
    assertThat(direct.at("/steps/0").has("maxUses")).isFalse();
    var end = data.required("states").required(direct.at("/stateIndices/0").asInt());
    assertThat(end.at("/desecrated/0/modifierId").asText()).isEqualTo("amulet:prefix:rotund");
    assertThat(end.required("explicits"))
        .hasSize(6)
        .anySatisfy(
            mod -> {
              assertThat(mod.get("modifierId").asText()).isEqualTo("amulet:prefix:rotund");
              assertThat(mod.at("/values/base_maximum_life").intValue()).isEqualTo(85);
            });
    var storedMods = json.treeToValue(end.required("explicits"), ModifierInstance[].class);
    var startMods = json.treeToValue(input.at("/item/explicits"), ModifierInstance[].class);
    assertThat(storedMods).contains(startMods);
    // +90만 요구하면 공개 시 +85에서 시작한다. 이후 Divine 한 번의 적중률은 1/15이다.
    var exact = hiddenLifeRequest(90, 90);
    exact.put("cardIndex", 0);
    var reveal = exact.putArray("steps").addObject();
    reveal.put("action", "REVEAL").putArray("omens");
    reveal.putArray("acquired").add(1);
    reveal.putArray("lost");
    grade(exact).andExpect(status().isOk()).andExpect(jsonPath("$.routes").isEmpty());
    var divine = exact.withArray("steps").addObject();
    divine.put("action", "DIVINE");
    divine.putArray("omens");
    divine.putArray("acquired");
    divine.putArray("lost");
    var graded =
        grade(exact)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.routes.length()").value(1))
            .andExpect(jsonPath("$.routes[0].steps[0].chance.probability").value(1d))
            .andExpect(jsonPath("$.routes[0].steps[1].chance.probability").value(1d / 15d))
            .andExpect(jsonPath("$.routes[0].steps[1].expectedUses").value(15d))
            .andReturn()
            .getResponse();
    var replay = json.readTree(graded.getContentAsByteArray());
    var witness =
        replay.required("states").required(replay.at("/routes/0/stateIndices/1").intValue());
    assertThat(witness.required("explicits"))
        .anyMatch(
            mod ->
                mod.path("modifierId").asText().equals("amulet:prefix:rotund")
                    && mod.path("values").path(LIFE).intValue() == 90);
    // 과거 반복 공개 표기는 신규 고정 경로 입력으로 받아들이지 않는다.
    reveal.put("maxUses", 1000);
    grade(exact).andExpect(status().isBadRequest());
  }

  /** 과거의 빛·소멸 → 새 뼈 → 공개 순서는 재생 요청으로 보내도 동일 아이템에서 실행할 후보가 없다. */
  @Test
  void replayCannotInsertAnAnnulWithLightBeforeTheReveal() throws Exception {
    var input = hiddenLifeRequest(85, 99);
    input.put("cardIndex", 0);
    var steps = input.putArray("steps");
    for (var action : List.of("ANNULMENT", "Preserved_Collarbone", "REVEAL")) {
      var step = steps.addObject();
      step.put("action", action);
      var omens = step.putArray("omens");
      if (action.equals("ANNULMENT")) omens.add("Omen_of_Light");
      var acquired = step.putArray("acquired");
      if (action.equals("REVEAL")) acquired.add(1);
      step.putArray("lost");
    }
    grade(input).andExpect(status().isOk()).andExpect(jsonPath("$.routes").isEmpty());
  }

  /** 사용자가 선택한 화폐 등급의 고정 경로만 재검증하는 API에 JSON을 보낸다. */
  private ResultActions grade(ObjectNode data) throws Exception {
    return mvc.perform(
        post(URL + "/grade")
            .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
            .contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsBytes(data)));
  }

  /** 이미 만족한 일반 조건에 활성 Total 행이 더 있으면 부분 목표로 경로를 만들지 않는다. */
  @Test
  void retiredTotalStopsRouteCreationBeforeTheAlreadySatisfiedShortcut() throws Exception {
    var input = body.deepCopy();
    var entries =
        (com.fasterxml.jackson.databind.node.ArrayNode) input.at("/goal/groups/0/entries");
    var total = ((ObjectNode) entries.get(0)).deepCopy();
    total.put("id", "saved-total").put("statId", GoalCatalogIndex.id("pseudo", "total_strength"));
    entries.add(total);
    request(input)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.issues[0].code").value("RETIRED_TOTAL_GOAL"));
    total.put("disabled", true);
    request(input).andExpect(status().isOk()).andExpect(jsonPath("$.routes[0].steps").isEmpty());
  }

  @Test
  void aSatisfiedGoalWithOmittedOptionalFlagsHasAZeroActionRoute() throws Exception {
    var input = body.deepCopy();
    ((ObjectNode) input.path("item")).remove("qualityInfused");
    var affix = (ObjectNode) input.path("item").path("explicits").get(0);
    affix.remove(List.of("fractured", "crafted"));
    request(input)
        .andExpect(status().isOk())
        .andExpect(header().string(RulesetBoundary.HEADER, RulesetBoundary.identity()))
        .andExpect(jsonPath("$.cards").isNotEmpty())
        .andExpect(jsonPath("$.routes[0].probability").value(1d))
        .andExpect(jsonPath("$.routes[0].order").isEmpty())
        .andExpect(jsonPath("$.routes[0].steps").isEmpty());
  }

  /** 제거할 수 없는 분열이 카드에 없으면 다른 목표 수치가 이미 충족되어도 1차에서 제외한다. */
  @Test
  void anUnrelatedFractureRejectsEvenAnAlreadySatisfiedGoal() throws Exception {
    var input = body.deepCopy();
    var item = (ObjectNode) input.path("item");
    item.withArray("explicits")
        .add(
            json.valueToTree(
                new ModifierInstance(
                    "amulet:suffix:of-the-seal",
                    Map.of("base_cold_damage_resistance_%", 6L),
                    true)));
    request(input)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.complete").value(true))
        .andExpect(jsonPath("$.cards").isEmpty())
        .andExpect(jsonPath("$.routes").isEmpty());
  }

  /** 기존 최소 품질 요청도 활성 옵션의 원래 수치와 함께 판정한다. */
  @Test
  void historicalMinimumQualityCanBeCombinedWithAnAffix() throws Exception {
    var input = body.deepCopy();
    ((ObjectNode) input.path("goal").path("general")).put("quality", 5);
    request(input)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routes[0].probability").value(1d))
        .andExpect(
            jsonPath("$.routes[0].steps[0].action")
                .value(org.hamcrest.Matchers.startsWith("CATALYST_")));
  }

  /** 활성 NOT은 새 탐색에서 거절하고 저장된 NOT의 고정 경로는 기존 판정으로 읽는다. */
  @Test
  void onlyNewSearchRejectsNonAndGroups() throws Exception {
    var input = body.deepCopy();
    var group = (ObjectNode) input.path("goal").path("groups").get(0);
    group.put("type", "NOT");
    ((ObjectNode) group.path("entries").get(0).path("range")).put("min", 1);
    request(input)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.issues[0].path").value("/goal/groups/0/type"))
        .andExpect(jsonPath("$.issues[0].code").value("AND_GROUP_REQUIRED"));
    input.put("cardIndex", 0);
    input.putArray("steps");
    grade(input).andExpect(status().isOk());
  }

  /** 생명력 기폭제가 붙은 분열 생명력 90은 표시 보정 전 원래 수치로만 인정한다. */
  @Test
  void typedStartingQualityPreservesRawAffixGoalAndTierSelection() throws Exception {
    var input = body.deepCopy();
    ((ObjectNode) input.at("/item"))
        .putObject("catalystQuality")
        .put("type", "FLESH")
        .put("amount", 40);
    ((ObjectNode) input.at("/item/explicits/0")).put("fractured", true);
    ((ObjectNode) input.at("/goal/general")).put("quality", 40);
    input.put("qualityCatalyst", "Flesh_Catalyst");
    request(input)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routes[0].steps").isEmpty())
        .andExpect(jsonPath("$.routes[0].probability").value(1d));
    ((ObjectNode) input.at("/goal/groups/0/entries/0/range")).put("min", 91);
    request(input)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cards").isEmpty())
        .andExpect(jsonPath("$.complete").value(true));
  }

  /** 지정 기폭제로 품질 40에 도달했고 균열의 상한 접두만 남은 희귀 아이템이다. */
  private ObjectNode alreadyAtFortyWithCap() {
    var input = qualityOnlyRequest();
    ((ObjectNode) input.at("/goal/general")).put("quality", 40);
    input.put("qualityCatalyst", "Flesh_Catalyst");
    var item = (ObjectNode) input.path("item");
    item.putObject("catalystQuality").put("type", "FLESH").put("amount", 40);
    item.putArray("explicits")
        .add(
            json.valueToTree(
                new ModifierInstance(
                    QualityLimitRules.BREACH_ID,
                    Map.of(QualityLimitRules.STAT_ID, 20L),
                    false,
                    true)));
    return input;
  }

  /** 빈 목표 카드에서는 이미 품질 40이어도 남은 접두를 제거하며, 그 전에 끝낸 등급 재생은 성공하지 않는다. */
  @Test
  void qualityOnlyAtFortyStillRemovesTheCapInSearchAndGradeReplay() throws Exception {
    var input = alreadyAtFortyWithCap();
    var result = response(request(input));
    assertThat(result.at("/cards/0/options")).isEmpty();
    assertThat(result.required("routes"))
        .isNotEmpty()
        .allSatisfy(
            route -> {
              assertThat(route.required("steps"))
                  .singleElement()
                  .satisfies(
                      step -> assertThat(step.required("action").asText()).isEqualTo("ANNULMENT"));
              var state =
                  result.required("states").required(route.at("/stateIndices/0").intValue());
              assertThat(state.required("explicits")).isEmpty();
              assertThat(state.path("catalystQuality"))
                  .isEqualTo(input.at("/item/catalystQuality"));
            });

    input.put("cardIndex", 0);
    var steps = input.putArray("steps");
    grade(input).andExpect(status().isOk()).andExpect(jsonPath("$.routes").isEmpty());
    var cleanup = steps.addObject();
    cleanup.put("action", "ANNULMENT");
    cleanup.putArray("omens");
    cleanup.putArray("acquired");
    cleanup.putArray("lost");
    grade(input)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routes[0].steps[0].action").value("ANNULMENT"))
        .andExpect(jsonPath("$.routes[0].probability").value(1d));
  }

  /** 품질 40과 현재 상한 접두를 모두 유지하도록 요청한다. */
  private ObjectNode explicitlyTargetedCapAtForty() {
    var input = alreadyAtFortyWithCap();
    var row =
        new GoalFilter.Entry(
            "cap",
            GoalCatalogIndex.id("explicit", QualityLimitRules.STAT_ID),
            "percent",
            new GoalFilter.Range(null, null),
            null,
            false);
    ((ObjectNode) input.path("goal"))
        .withArray("groups")
        .add(
            json.valueToTree(
                new GoalFilter.Group("keep-cap", GoalFilter.Type.AND, false, null, List.of(row))));
    return input;
  }

  /** 상한 옵션 자체를 명시적으로 선택한 목표에서는 이미 보유한 상한과 품질 40을 그대로 인정한다. */
  @Test
  void explicitlyTargetedCapMayRemainAtForty() throws Exception {
    var input = explicitlyTargetedCapAtForty();
    request(input)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routes[0].steps").isEmpty())
        .andExpect(jsonPath("$.routes[0].probability").value(1d));
    input.put("cardIndex", 0);
    input.putArray("steps");
    grade(input)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routes[0].steps").isEmpty())
        .andExpect(jsonPath("$.routes[0].probability").value(1d));
  }

  /** 현재 API 요청이 성공하면 JSON 응답을 읽는다. */
  private JsonNode response(ResultActions call) throws Exception {
    return json.readTree(
        call.andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray());
  }

  /** 직접 목표인 상한을 보존하면서 희귀 정신력 접두를 얻는 경로를 검색하고 같은 단계로 다시 계산한다. */
  @Test
  void searchAndReplayKeepTheExplicitCapWhileAcquiringSpirit47() throws Exception {
    var input = explicitlyTargetedCapAtForty();
    var spirit =
        new GoalFilter.Entry(
            "spirit",
            GoalCatalogIndex.id("explicit", SolarAmulet.SPIRIT_STAT),
            "flat",
            new GoalFilter.Range(BigDecimal.valueOf(47), null),
            null,
            false);
    ((ObjectNode) input.at("/goal/groups/0")).withArray("entries").add(json.valueToTree(spirit));
    var result = response(request(input));
    assertThat(result.required("routes")).isNotEmpty();
    var route = result.at("/routes/0");
    assertThat(route.required("steps"))
        .extracting(step -> step.required("action").textValue())
        .containsExactly("Preserved_Collarbone", "REVEAL");
    var cap = json.treeToValue(input.at("/item/explicits/0"), ModifierInstance.class);
    var frames = route.required("stateIndices");
    for (var frame : frames) {
      var state = result.required("states").required(frame.intValue());
      assertThat(json.treeToValue(state.required("explicits"), ModifierInstance[].class))
          .contains(cap);
      assertThat(state.path("catalystQuality")).isEqualTo(input.at("/item/catalystQuality"));
    }
    var end = result.required("states").required(frames.required(frames.size() - 1).intValue());
    assertThat(end.required("explicits"))
        .anyMatch(m -> m.path("values").path(SolarAmulet.SPIRIT_STAT).asInt() >= 47);
    assertThat(route.required("probability").doubleValue()).isPositive();

    input.set("cardIndex", route.required("cardIndex"));
    var steps = input.putArray("steps");
    for (var step : route.required("steps"))
      steps.add(((ObjectNode) step).deepCopy().retain("action", "omens", "acquired", "lost"));
    var replay = response(grade(input));
    assertThat(replay.at("/routes/0/probability")).isEqualTo(route.required("probability"));
    var replayFrames = replay.at("/routes/0/stateIndices");
    assertThat(
            replay
                .required("states")
                .required(replayFrames.required(replayFrames.size() - 1).intValue()))
        .isEqualTo(end);
  }

  /** 기폭제 한 번으로 달성할 수 있는 품질 목표를 만든다. */
  private ObjectNode qualityOnlyRequest() {
    var input = body.deepCopy();
    var goal = (ObjectNode) input.path("goal");
    ((ObjectNode) goal.path("general")).put("quality", 1);
    goal.withArray("groups").removeAll();
    return input;
  }

  /** 옵션 수치를 요구하지 않은 품질 목표는 기폭제 한 번으로 검증할 수 있다. */
  @Test
  void aQualityOnlyGoalShowsCatalystWithoutAnOptionAcquisition() throws Exception {
    request(qualityOnlyRequest())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routes[0].probability").value(1d))
        .andExpect(jsonPath("$.routes[0].order").isEmpty())
        .andExpect(
            jsonPath("$.routes[0].steps[0].action")
                .value(org.hamcrest.Matchers.containsString("CATALYST")));
  }

  /** 단계가 가리키는 압축 상태의 베이스·옵션·스탯을 복원해 같은 API의 시작 아이템으로 다시 읽는다. */
  @Test
  void negotiatedRouteStatesRoundTripAndCanStartANewRequest() throws Exception {
    var input = qualityOnlyRequest();
    var legacyResponse = request(input).andExpect(status().isOk()).andReturn().getResponse();
    var legacy = json.readTree(legacyResponse.getContentAsByteArray());
    var mappings = GameIdMappings.active();
    var codec = new GameIdCodec(mappings);
    var compactResponse =
        mvc.perform(
                post(URL)
                    .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                    .header(GameIdMappings.HEADER, mappings.identity())
                    .header(GameIdTransport.BODY_HEADER, "compact")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(codec.encode(input))))
            .andExpect(status().isOk())
            .andExpect(header().string(GameIdMappings.HEADER, mappings.identity()))
            .andReturn()
            .getResponse();
    var compact = json.readTree(compactResponse.getContentAsByteArray());
    var decoded = codec.decode(compact);
    assertThat(decoded).isEqualTo(legacy);

    var states = compact.required("states");
    var routes = compact.required("routes");
    assertThat(routes).anyMatch(route -> route.at("/stateIndices/0").asInt(-1) == 0);
    for (var route : routes) {
      assertThat(route.required("steps")).hasSize(1);
      var indices = route.required("stateIndices");
      assertThat(indices).hasSize(1);
      assertThat(indices.required(0).isIntegralNumber()).isTrue();
      var index = indices.get(0).intValue();
      assertThat(index).isBetween(0, states.size() - 1);
      var state = states.required(index);
      assertThat(state.required("baseItemId").textValue()).isEqualTo("b005a");
      assertThat(state.at("/explicits/0/modifierId").textValue()).isEqualTo("m002k");
      assertThat(state.at("/explicits/0/values"))
          .isEqualTo(json.createObjectNode().put("s0028", 90));
    }

    var firstState = routes.required(0).at("/stateIndices/0").intValue();
    input.set("item", decoded.required("states").required(firstState));
    request(input)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routes[0].probability").value(1d))
        .andExpect(jsonPath("$.routes[0].steps").isEmpty())
        .andExpect(jsonPath("$.routes[0].stateIndices").isEmpty());
  }

  /** 승급 에센스로 +85~99 티어를 얻으면 +85에서 시작하고, +90 이상은 Divine 한 번에 10/15로 적중한다. */
  @Test
  void essenceObtainsTheTierAndDivineRollsItsFinalValue() throws Exception {
    var input = body.deepCopy();
    var item = (ObjectNode) input.path("item");
    item.put("rarity", "MAGIC");
    item.withArray("explicits")
        .removeAll()
        .add(
            json.valueToTree(
                new ModifierInstance(
                    "amulet:suffix:of-the-seal", Map.of("base_cold_damage_resistance_%", 6L))));
    var response = request(input).andExpect(status().isOk()).andReturn().getResponse();
    var result = json.readTree(response.getContentAsByteArray());
    assertThat(result.path("routes"))
        .anySatisfy(
            route -> {
              var steps = route.path("steps");
              assertThat(steps.size()).isEqualTo(2);
              assertThat(steps.get(0).path("action").asText()).isEqualTo("GREATER_ESSENCE_BODY");
              assertThat(route.path("order").toString()).isEqualTo("[1]");
              assertThat(steps.get(1).path("action").asText()).isEqualTo("DIVINE");
              var acquired =
                  result.required("states").required(route.at("/stateIndices/0").intValue());
              assertThat(acquired.required("explicits"))
                  .anyMatch(
                      mod ->
                          mod.path("modifierId").asText().equals("amulet:prefix:rotund")
                              && mod.path("values").path(LIFE).intValue() == 85);
              assertThat(steps.get(1).path("chance").path("probability").doubleValue())
                  .isCloseTo(10d / 15d, org.assertj.core.data.Offset.offset(1e-12));
              var option =
                  result
                      .path("cards")
                      .get(route.path("cardIndex").intValue())
                      .path("options")
                      .get(0);
              assertThat(option.path("number").intValue()).isEqualTo(1);
              assertThat(option.path("modifierIds"))
                  .anyMatch(id -> id.asText().equals("amulet:prefix:rotund"));
            });
  }

  @Test
  void rejectsUnknownInputsMalformedFlagsAndStaleCatalogs() throws Exception {
    var input = body.deepCopy();
    input.put("routes", "unverified");
    request(input).andExpect(status().isBadRequest());

    input = body.deepCopy();
    var affix = (ObjectNode) input.path("item").path("explicits").get(0);
    affix.putNull("crafted");
    request(input)
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    affix.put("crafted", "false");
    request(input).andExpect(status().isUnprocessableEntity());

    input = body.deepCopy();
    ((ObjectNode) input.path("item")).put("snapshotId", "unregistered");
    request(input).andExpect(status().isUnprocessableEntity());

    input = body.deepCopy();
    ((ObjectNode) input.path("goal")).put("catalogVersion", "old");
    request(input)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("CATALOG_VERSION_MISMATCH"));
    mvc.perform(
            post(URL)
                .header(RulesetBoundary.HEADER, "old")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(body)))
        .andExpect(status().isUnprocessableEntity());
  }
}
