package com.poe2craft.craftingsupport.goal.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.configuration.BackendSettings;
import com.poe2craft.craftingsupport.goal.application.GoalFilterService;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.craftingsupport.goal.infrastructure.GoalDefinitionsLoader;
import com.poe2craft.craftingsupport.testroutes.application.TestRouteService;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
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
            .setControllerAdvice(new GoalFilterErrors())
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

  /** 옵션 수치를 요구하지 않은 품질 목표는 기폭제 한 번으로 검증할 수 있다. */
  @Test
  void aQualityOnlyGoalShowsCatalystWithoutAnOptionAcquisition() throws Exception {
    var input = body.deepCopy();
    var goal = (ObjectNode) input.path("goal");
    ((ObjectNode) goal.path("general")).put("quality", 1);
    goal.withArray("groups").removeAll();
    request(input)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routes[0].probability").value(1d))
        .andExpect(jsonPath("$.routes[0].order").isEmpty())
        .andExpect(
            jsonPath("$.routes[0].steps[0].action")
                .value(org.hamcrest.Matchers.containsString("CATALYST")));
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
