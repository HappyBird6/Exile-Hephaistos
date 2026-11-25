package com.poe2craft.craftingsupport.goal.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.bootstrap.http.GameIdTransport;
import com.poe2craft.craftingsupport.goal.domain.GoalCatalogIndex;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.QualityLimitRules;
import com.poe2craft.shared.ids.GameIdCodec;
import com.poe2craft.shared.ids.GameIdMappings;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.ruleset.api.RulesetBoundary;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** 제작지원 HTTP의 일반 획득 경로, 균열 상한과 퀄리티 작업, 수치 판정과 저장 상태 재입력을 검증한다. */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class AcquisitionQualityIntegrationTest {
  /** 테스트 프로필의 영속성 서비스로 앱을 초기화한다. */
  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.6-alpine");

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7.4.5-alpine").withExposedPorts(6379);

  /** 실제 브라우저가 사용하는 카탈로그와 계산 요청의 공통 경로, 표시용 원문 스탯 ID다. */
  private static final String ROOT = "/api/v1/crafting/support/goal-filters";

  private static final String DEFENCES = "global_armour_evasion_energy_shield_+%";
  private static final String PROJECTILE = "projectile_skill_gem_level_+";
  private static final String COLD = "base_cold_damage_resistance_%";
  private static final String FIRE = "base_fire_damage_resistance_%";

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired ItemCatalog items;
  @Autowired CraftingOperations workbench;

  /** 실제 HTTP에서 받은 현재 카탈로그 버전과 82레벨 일반 태양 목걸이다. */
  private String catalogVersion;

  private ItemState normal;

  /** 별도 DB·Redis의 주소를 테스트 애플리케이션에 제공한다. */
  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    registry.add(
        "spring.flyway.locations", () -> "classpath:db/migration,classpath:db/testmigration");
  }

  /** 사용자와 동일하게 현재 베이스 문맥에서 유효한 목표 필터 버전을 얻는다. */
  @BeforeEach
  void catalog() throws Exception {
    normal = SolarAmulet.initial(items, 82, 15);
    catalogVersion = catalogVersion(normal);
  }

  /** 장비별 목표 카탈로그 ID를 HTTP에서 얻는다. */
  private String catalogVersion(ItemState item) throws Exception {
    var response =
        mvc.perform(
                get(ROOT + "/catalog")
                    .param("snapshotId", item.snapshotId())
                    .param("baseItemId", item.baseItemId())
                    .param("itemLevel", "82"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    return json.readTree(response.getContentAsByteArray()).required("catalogVersion").asText();
  }

  /** 단일 AND 안에 다섯 행을 넣고, 럭키 표시를 끈 상태에서도 실제 희귀 아이템이 완성되는지 확인한다. */
  @Test
  void normalSolarAmuletHasAnOrdinaryFiveAffixRoute() throws Exception {
    var input =
        request(
            false,
            row(SolarAmulet.SPIRIT_STAT, "flat", 47, null),
            row(DEFENCES, "percent", null, null),
            row(PROJECTILE, "flat", 3, null),
            row(COLD, "percent", null, null),
            row(FIRE, "percent", null, null));
    var result = calculate(input, "");
    assertThat(result.required("cards")).hasSize(1);
    var route = ordinary(result);
    var finalItem = frame(result, route, route.required("steps").size() - 1);
    assertThat(finalItem.required("rarity").asText()).isEqualTo("RARE");
    assertThat(raw(finalItem, SolarAmulet.SPIRIT_STAT)).isGreaterThanOrEqualTo(47);
    assertThat(raw(finalItem, DEFENCES)).isPositive();
    assertThat(raw(finalItem, PROJECTILE)).isEqualTo(3);
    assertThat(raw(finalItem, COLD)).isPositive();
    assertThat(raw(finalItem, FIRE)).isPositive();
  }

  /** 일반 빈 베이스에서는 희귀 승급 뒤 균열 상한을 열고, 품질 작업 이후 다른 제작 옵션을 완성한다. */
  @Test
  void ordinaryQualityFromNormalCanTradeTheCapForCraftedDefences() throws Exception {
    var input = request(true, row(DEFENCES, "percent", null, null));
    var result = calculate(input, "");
    var route = ordinary(result);
    int at = stepIndex(route, "QUALITY_40");
    var steps = route.required("steps");
    assertThat(steps.valueStream().limit(at).map(s -> s.required("action").asText()))
        .contains("ESSENCE_BREACH");
    var before = frame(result, route, at - 1);
    assertThat(frame(result, route, at)).isEqualTo(before);
    assertThat(before.required("rarity").asText()).isEqualTo("RARE");
    assertThat(raw(before, QualityLimitRules.STAT_ID)).isEqualTo(20);
    var finished = frame(result, route, steps.size() - 1);
    assertThat(raw(finished, QualityLimitRules.STAT_ID)).isZero();
    assertThat(raw(finished, DEFENCES)).isPositive();
    assertThat(steps.valueStream().skip(at + 1).map(s -> s.required("action").asText()))
        .contains("PERFECT_ESSENCE_ENHANCEMENT");
  }

  /** 희귀 상한 보유 상태에서 지정한 기폭제의 타입·품질이 작업·고정 재생·상한 제거 후 재입력까지 이어진다. */
  @Test
  void typedQualitySurvivesTheCapAndReplaysTheSameRoute() throws Exception {
    var input = request(true, row(DEFENCES, "percent", null, null));
    var item = (ObjectNode) input.required("item");
    item.put("rarity", "RARE");
    item.withArray("explicits")
        .add(
            json.valueToTree(
                new ModifierInstance(
                    QualityLimitRules.BREACH_ID,
                    Map.of(QualityLimitRules.STAT_ID, 20L),
                    false,
                    true)))
        .add(json.valueToTree(new ModifierInstance("amulet:suffix:of-the-seal", Map.of(COLD, 6L))));
    input.put("qualityCatalyst", "Xophs_Catalyst");
    var result = calculate(input, "");
    var route = ordinary(result);
    int at = stepIndex(route, "CATALYST_XOPH");
    var after = frame(result, route, at);
    assertThat(route.required("steps").get(at).required("expectedUses").asDouble()).isEqualTo(40d);
    assertThat(after.at("/catalystQuality/type").asText()).isEqualTo("XOPH");
    assertThat(after.at("/catalystQuality/amount").asInt()).isEqualTo(40);
    var finished = frame(result, route, route.required("steps").size() - 1);
    assertThat(raw(finished, QualityLimitRules.STAT_ID)).isZero();
    assertThat(finished.required("catalystQuality")).isEqualTo(after.required("catalystQuality"));
    assertThat(raw(finished, DEFENCES)).isPositive();
    var fixed = input.deepCopy();
    fixed.set("cardIndex", route.required("cardIndex"));
    var fixedSteps = fixed.putArray("steps");
    for (var step : route.required("steps")) {
      var selected = fixedSteps.addObject();
      for (String field :
          List.of(
              "action",
              "omens",
              "acquired",
              "lost",
              "socketIndex",
              "maxUses",
              "repeatUntilHit",
              "repeatTarget")) if (step.has(field)) selected.set(field, step.get(field));
    }
    var replay = calculate(fixed, "/grade");
    var verified = ordinary(replay);
    assertThat(frame(replay, verified, verified.required("steps").size() - 1)).isEqualTo(finished);
    input.set("item", finished);
    var resumed = ordinary(calculate(input, ""));
    assertThat(resumed.required("steps")).isEmpty();
    assertThat(resumed.required("probability").asDouble()).isEqualTo(1d);
  }

  /** 원래 화염 저항 10의 분열 옵션은 같은 계열을 교체할 수 없고, Xoph 40의 표시 보정도 획득 조건에 더하지 않는다. */
  @Test
  void activeCatalystDoesNotChangeTheAcquisitionBoundsOnARawRoll() throws Exception {
    var input = request(true, row(FIRE, "percent", null, 10));
    var item = (ObjectNode) input.required("item");
    item.put("rarity", "RARE");
    item.withArray("explicits")
        .add(
            json.valueToTree(
                new ModifierInstance("amulet:suffix:of-the-whelpling", Map.of(FIRE, 10L), true)));
    item.putObject("catalystQuality").put("type", "XOPH").put("amount", 40);
    input.put("qualityCatalyst", "Xophs_Catalyst");
    assertThat(ordinary(calculate(input, "")).required("steps")).isEmpty();
    var range = (ObjectNode) input.at("/goal/groups/0/entries/0/range");
    range.put("min", 11).putNull("max");
    assertThat(calculate(input, "").required("routes")).isEmpty();
  }

  /** 정제·미등록 재료, 40 목표 없는 기폭제, 균열 상한을 제작할 수 없는 허리띠를 HTTP 경계에서 거절한다. */
  @Test
  void rejectsUnsupportedMaterialsAndQualityBases() throws Exception {
    var input = request(true, row(DEFENCES, "percent", null, null));
    for (String invalid : List.of("Refined_Xophs_Catalyst", "unknown-catalyst")) {
      input.put("qualityCatalyst", invalid);
      assertThat(send(input, "", status().isUnprocessableEntity()).at("/issues/0/code").asText())
          .isEqualTo("INVALID_QUALITY_CATALYST");
    }
    input.put("qualityCatalyst", "Xophs_Catalyst");
    ((ObjectNode) input.at("/goal/general")).remove("quality");
    assertThat(send(input, "", status().isUnprocessableEntity()).at("/issues/0/code").asText())
        .isEqualTo("QUALITY_40_REQUIRED");

    assertThat(
            send(rareQualityOnly("belt"), "", status().isUnprocessableEntity())
                .at("/issues/0/code")
                .asText())
        .isEqualTo("UNSUPPORTED_QUALITY_BASE");
  }

  /** 명시 옵션 없는 철제 반지와 진주 반지는 합법적인 균열 제거 대상을 마련한 후 같은 실제 기폭제 품질에 도달한다. */
  @Test
  void emptyRareRingsCanPrepareARealTargetForBreachQuality() throws Exception {
    for (String base : List.of("ring", "pearl")) {
      var input = rareQualityOnly(base);
      input.put("qualityCatalyst", "Xophs_Catalyst");
      var result = calculate(input, "");
      var route = ordinary(result);
      var breach = stepIndex(route, "ESSENCE_BREACH");
      var quality = stepIndex(route, "CATALYST_XOPH");
      assertThat(breach).as(base).isPositive();
      assertThat(quality).isGreaterThan(breach);
      assertThat(raw(frame(result, route, breach), QualityLimitRules.STAT_ID)).isEqualTo(20L);
      var finished = frame(result, route, route.required("steps").size() - 1);
      assertThat(finished.at("/catalystQuality/type").asText()).isEqualTo("XOPH");
      assertThat(finished.at("/catalystQuality/amount").asInt()).isEqualTo(40);
    }
  }

  /** 해당 베이스의 실제 암시 옵션만 가진 희귀 아이템과 빈 40 품질 목표를 구성한다. */
  private ObjectNode rareQualityOnly(String key) throws Exception {
    var initial = workbench.initial(key, 82).state();
    var item =
        new ItemState(
            initial.snapshotId(),
            initial.baseItemId(),
            initial.itemLevel(),
            ItemState.Rarity.RARE,
            initial.implicits(),
            List.of(),
            initial.conditions());
    var goal =
        new GoalFilter(
            1,
            catalogVersion(item),
            new GoalFilter.General(
                item.baseItemId(),
                new GoalFilter.Range(null, null),
                List.of(ItemState.Rarity.RARE),
                40),
            List.of());
    return json.valueToTree(Map.of("item", item, "goal", goal));
  }

  /** 최소·최대가 없는 행은 원본 명시 스탯의 존재를 요구한다. */
  private GoalFilter.Entry row(String raw, String unit, Integer min, Integer max) {
    return new GoalFilter.Entry(
        raw,
        GoalCatalogIndex.id("explicit", raw),
        unit,
        new GoalFilter.Range(
            min == null ? null : BigDecimal.valueOf(min),
            max == null ? null : BigDecimal.valueOf(max)),
        null,
        false);
  }

  /** 빈 일반 베이스의 명시 AND 목표와 선택적 40 품질을 전송 본문으로 만든다. */
  private ObjectNode request(boolean withQuality, GoalFilter.Entry... rows) {
    var goal =
        new GoalFilter(
            1,
            catalogVersion,
            new GoalFilter.General(
                normal.baseItemId(),
                new GoalFilter.Range(null, null),
                List.of(ItemState.Rarity.RARE)),
            List.of(
                new GoalFilter.Group("affixes", GoalFilter.Type.AND, false, null, List.of(rows))));
    ObjectNode input = json.valueToTree(Map.of("item", normal, "goal", goal));
    if (withQuality) ((ObjectNode) input.at("/goal/general")).put("quality", 40);
    return input;
  }

  /** 새 탐색 또는 등급 재생이 정상 계산된 응답을 읽는다. */
  private JsonNode calculate(ObjectNode body, String suffix) throws Exception {
    return send(body, suffix, status().isOk());
  }

  /** 브라우저와 같은 짧은 ID 협상으로 실제 제작지원 엔드포인트를 호출하고 HTTP 상태와 역변환 결과를 확인한다. */
  private JsonNode send(ObjectNode body, String suffix, ResultMatcher expected) throws Exception {
    var mappings = GameIdMappings.active();
    var codec = new GameIdCodec(mappings);
    var response =
        mvc.perform(
                post(ROOT + "/test-routes" + suffix)
                    .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                    .header(GameIdMappings.HEADER, mappings.identity())
                    .header(GameIdTransport.BODY_HEADER, "compact")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(codec.encode(body))))
            .andExpect(expected)
            .andReturn()
            .getResponse();
    return codec.decode(json.readTree(response.getContentAsByteArray()));
  }

  /** 럭키를 끈 사용자에게 실제로 노출될 수 있는 완주 경로를 선택한다. */
  private JsonNode ordinary(JsonNode result) {
    return result
        .required("routes")
        .valueStream()
        .filter(route -> !route.path("lucky").asBoolean())
        .findFirst()
        .orElseThrow(
            () ->
                new AssertionError(
                    "No ordinary route: " + result.path("diagnostics").path("stage").asText()));
  }

  /** 균열의 상한 확인과 아이템 스냅샷을 대조하기 위한 품질 작업의 단계 번호다. */
  private int stepIndex(JsonNode route, String action) {
    var steps = route.required("steps");
    for (int i = 0; i < steps.size(); i++)
      if (steps.get(i).required("action").asText().equals(action)) return i;
    throw new AssertionError("Missing route action " + action);
  }

  /** 단계별 인덱스로 응답에 공유된 실제 상태를 읽는다. */
  private JsonNode frame(JsonNode result, JsonNode route, int index) {
    return result
        .required("states")
        .required(route.required("stateIndices").required(index).intValue());
  }

  /** 해당 명시 스탯의 원본 합계다. 없는 스탯의 합은 0이다. */
  private long raw(JsonNode state, String stat) {
    return state
        .required("explicits")
        .valueStream()
        .mapToLong(mod -> mod.required("values").path(stat).asLong())
        .sum();
  }
}
