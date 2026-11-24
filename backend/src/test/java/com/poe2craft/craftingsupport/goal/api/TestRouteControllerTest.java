package com.poe2craft.craftingsupport.goal.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.ids.GameIdCodec;
import com.poe2craft.shared.ids.GameIdMappings;
import com.poe2craft.shared.item.ItemState;
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
  private final ObjectMapper json = new ObjectMapper();
  private final ObjectNode body;
  private final MockMvc mvc;

  TestRouteControllerTest() {
    var catalog = ItemCatalogLoader.loadDefault();
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

  /** 기본 경로의 등급 하나만 바꾼 실제 HTTP 입력으로 선택 화폐와 결과 상태를 돌려받는다. */
  @Test
  void gradesOneExaltedStepWithoutReplacingTheMethodOrTrustingClientOdds() throws Exception {
    var input = body.deepCopy();
    ((ObjectNode) input.path("item")).putArray("explicits");
    ((ObjectNode) input.at("/goal/groups/0/entries/0/range")).putNull("min");
    input.put("cardIndex", 0);
    var step = input.putArray("steps").addObject();
    step.put("action", "GREATER_EXALTED");
    step.putArray("omens");
    step.putArray("acquired").add(1);
    step.putArray("lost");
    grade(input)
        .andExpect(status().isOk())
        .andExpect(header().string(RulesetBoundary.HEADER, RulesetBoundary.identity()))
        .andExpect(jsonPath("$.routes.length()").value(1))
        .andExpect(jsonPath("$.routes[0].steps[0].action").value("GREATER_EXALTED"))
        .andExpect(jsonPath("$.routes[0].steps[0].chance.probability").isNumber())
        .andExpect(jsonPath("$.routes[0].stateIndices.length()").value(1));
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

  /** 현재 획득 탐색에는 기폭제나 목표 옵션 제거만으로 끝나는 순서가 없어 불가능이라고 단정하지 않는다. */
  @Test
  void unmodeledQualityAndRemovalReportIncomplete() throws Exception {
    var input = body.deepCopy();
    ((ObjectNode) input.path("goal").path("general")).put("quality", 5);
    request(input)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routes").isEmpty())
        .andExpect(jsonPath("$.complete").value(false));

    input = body.deepCopy();
    var group = (ObjectNode) input.path("goal").path("groups").get(0);
    group.put("type", "NOT");
    ((ObjectNode) group.path("entries").get(0).path("range")).put("min", 1);
    request(input).andExpect(status().isOk()).andExpect(jsonPath("$.complete").value(false));
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

  /** 승급 에센스의 +85~99 중 +90 이상인 열 가지 굴림만 같은 1회 경로의 성공 질량에 포함된다. */
  @Test
  void numericGoalCountsOnlyPassingRollsOfTheAcquiredOption() throws Exception {
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
              assertThat(steps.size()).isEqualTo(1);
              assertThat(steps.get(0).path("action").asText()).isEqualTo("GREATER_ESSENCE_BODY");
              assertThat(route.path("order").toString()).isEqualTo("[1]");
              assertThat(route.path("probability").doubleValue())
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
