package com.poe2craft.shared.crafting.domain;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.poe2craft.bootstrap.CatalogConfiguration;
import com.poe2craft.bootstrap.http.CraftingErrors;
import com.poe2craft.shared.crafting.application.CatalogCraftingService;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ManualRevealCatalog;
import com.poe2craft.shared.item.ModifierDefinition.AffixType;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.text.ItemTextService;
import com.poe2craft.shared.ruleset.api.RulesetBoundary;
import com.poe2craft.workbench.api.WorkbenchController;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// Keep the real HTTP filters; mock only their external Redis dependency.
/**
 * 훼손·Controller 동작을 고정 입력·기대 결과로 검증한다. 각 테스트가 공유하는 준비 값과 시나리오는 아래와 같다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code mvc}: MVC검증기 ({@code MockMvc}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code crafting}: 제작 ({@code CatalogCraftingService}).
 *   <li>{@code fixture}: 검증사례 ({@code CurrentCatalogFixture}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code currentCatalogFixture()}: 각 검증에 필요한 독립 초기 상태와 의존성을 준비한다.
 *   <li>{@code directChoiceUsesEligiblePoolAndRestoresWithoutThreeCandidates()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code boneApplicationRequiresIdentityAndRestoresCurrencyConstraint()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code request()}: 계산·실행에 사용할 요청 값을 구성한다. 반환: {@code ObjectNode}.
 *   <li>{@code
 *       restoresAnUnrevealedSlotWithoutLosingItsOccupancyAndNeverReturnsRevealProbability()}: 이름에
 *       명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code unknownFieldsAndFractionalStatValuesAreRejectedBeforeConversion()}: 이름에 명시한 시나리오의 실제
 *       결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code missingRulesetCannotExecuteNewEndpoints()}: 이름에 명시한 시나리오의 실제 결과·보존 상태를 기대값과 비교한다.
 *       사용하는 예외: {@code Exception}.
 *   <li>{@code commonSlotsImportWithoutFlatteningAndOrdinaryEndpointStillRefuses()}: 이름에 명시한 시나리오의
 *       실제 결과·보존 상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 *   <li>{@code importDoesNotDiscardDuplicateMarksForOneRevealedModifier()}: 이름에 명시한 시나리오의 실제 결과·보존
 *       상태를 기대값과 비교한다. 사용하는 예외: {@code Exception}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code CurrentCatalogFixture}: {@code AbyssControllerTest} 내부의 현재·카탈로그·검증사례 처리를 맡는다.
 * </ul>
 */
@org.springframework.test.context.bean.override.mockito.MockitoBean(
    types = org.springframework.data.redis.core.StringRedisTemplate.class)
@WebMvcTest(WorkbenchController.class)
@Import({CatalogConfiguration.class, CraftingErrors.class, ItemTextService.class})
class AbyssControllerTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired CatalogCraftingService crafting;
  private CurrentCatalogFixture fixture;

  @BeforeEach
  void currentCatalogFixture() {
    fixture = new CurrentCatalogFixture();
  }

  // HTTP boundaries validate the current full catalog, not the legacy domain-test subset.
  /**
   * {@code AbyssControllerTest} 내부의 현재·카탈로그·검증사례 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code com.poe2craft.shared.item.ItemCatalog}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code rare(level, locked, ids)}: 희귀 처리에서 {@code crafting.initial}, {@code
   *       root.snapshotId}을(를) 사용한다. 입력: {@code level}(레벨), {@code locked}(잠긴), {@code ids}(식별자목록).
   *       반환: {@code ItemState}.
   * </ul>
   */
  private final class CurrentCatalogFixture {
    final com.poe2craft.shared.item.ItemCatalog catalog = crafting.abyssCatalog(rare(82, ""));

    ItemState rare(int level, String locked, String... ids) {
      var root = crafting.initial("solar", level, true).state();
      return new ItemState(
          root.snapshotId(),
          root.baseItemId(),
          level,
          ItemState.Rarity.RARE,
          root.implicits(),
          Arrays.stream(ids)
              .map(
                  id -> {
                    var definition = catalog.find(id).orElseThrow();
                    var values = new HashMap<String, Long>();
                    definition.stats().forEach(stat -> values.put(stat.id(), stat.min()));
                    return new ModifierInstance(id, values, id.equals(locked));
                  })
              .toList(),
          root.conditions());
    }
  }

  @Test
  void directChoiceUsesEligiblePoolAndRestoresWithoutThreeCandidates() throws Exception {
    var catalog = ManualRevealCatalog.enrich(fixture.catalog);
    var hidden =
        DesecrationCurrency.apply(
            fixture.rare(82, ""),
            "Preserved_Collarbone",
            AffixType.PREFIX,
            null,
            Set.of(),
            catalog);
    var slot = hidden.slots().getFirst().id();
    var eligibleResponse =
        mvc.perform(
                post("/api/v1/crafting/workbench/abyss/eligible")
                    .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(Map.of("state", hidden, "slotId", slot))))
            .andExpect(status().isOk())
            .andReturn();
    var ids = json.readTree(eligibleResponse.getResponse().getContentAsString());
    assertThat(ids.size()).isPositive();
    var manual =
        mvc.perform(
                get("/api/v1/crafting/workbench/initial")
                    .param("base", "solar")
                    .param("manualReveal", "true"))
            .andExpect(status().isOk())
            .andReturn();
    var extraId =
        catalog.modifiers().values().stream()
            .filter(d -> d.tags().contains("reveal_only"))
            .findFirst()
            .orElseThrow()
            .id();
    assertThat(
            json.readTree(manual.getResponse().getContentAsString()).path("modifiers").has(extraId))
        .isTrue();
    var ordinary =
        mvc.perform(get("/api/v1/crafting/workbench/initial").param("base", "solar"))
            .andExpect(status().isOk())
            .andReturn();
    assertThat(
            json.readTree(ordinary.getResponse().getContentAsString())
                .path("modifiers")
                .has(extraId))
        .isFalse();
    var definition = catalog.find(ids.get(0).asText()).orElseThrow();
    var values = new HashMap<String, Long>();
    definition.stats().forEach(stat -> values.put(stat.id(), stat.min()));
    var body =
        json.valueToTree(
            Map.of(
                "state",
                hidden,
                "slotId",
                slot,
                "candidate",
                new ModifierInstance(definition.id(), values)));
    var response =
        mvc.perform(
                post("/api/v1/crafting/workbench/abyss/select")
                    .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.offer").isEmpty())
            .andExpect(jsonPath("$.slots[0].modifier.modifierId").value(definition.id()))
            .andReturn();
    var restored = json.readTree(response.getResponse().getContentAsString());
    mvc.perform(
            post("/api/v1/crafting/workbench/abyss/validate")
                .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("state", restored))))
        .andExpect(status().isOk());
    ((ObjectNode) body.get("candidate")).put("fractured", true);
    mvc.perform(
            post("/api/v1/crafting/workbench/abyss/select")
                .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(body)))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void boneApplicationRequiresIdentityAndRestoresCurrencyConstraint() throws Exception {
    var body =
        json.valueToTree(
            Map.of(
                "state",
                fixture.rare(82, "", "amulet:prefix:hale"),
                "currency",
                "Ancient_Collarbone",
                "side",
                "SUFFIX",
                "activeOmens",
                List.of("Omen_of_Dextral_Necromancy")));
    mvc.perform(
            post("/api/v1/crafting/workbench/abyss/desecrate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(body)))
        .andExpect(status().isUnprocessableEntity());
    var response =
        mvc.perform(
                post("/api/v1/crafting/workbench/abyss/desecrate")
                    .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slots[1].minimumModifierLevel").value(40))
            .andExpect(jsonPath("$.slots[1].modifier").isEmpty())
            .andReturn();
    var next = json.readTree(response.getResponse().getContentAsString());
    mvc.perform(
            post("/api/v1/crafting/workbench/abyss/validate")
                .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("state", next))))
        .andExpect(status().isOk());
  }

  ObjectNode request() {
    var state =
        AbyssCraftingState.from(fixture.rare(82, "", "amulet:prefix:hale"), fixture.catalog)
            .addUnrevealed("abyss", AffixType.SUFFIX, fixture.catalog);
    return json.valueToTree(Map.of("state", state));
  }

  @Test
  void restoresAnUnrevealedSlotWithoutLosingItsOccupancyAndNeverReturnsRevealProbability()
      throws Exception {
    var request = request();
    var restored =
        mvc.perform(
                post("/api/v1/crafting/workbench/abyss/validate")
                    .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.slots[1].desecrated").value(true))
            .andReturn();
    request.set("state", json.readTree(restored.getResponse().getContentAsString()));
    request.put("slotId", "abyss");
    var candidate = fixture.rare(82, "", "amulet:suffix:of-the-brute").explicits().getFirst();
    request.set("candidates", json.valueToTree(List.of(candidate, candidate, candidate)));
    var observed =
        mvc.perform(
                post("/api/v1/crafting/workbench/abyss/observe")
                    .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.offer.candidates.length()").value(3))
            .andReturn();
    assertThat(observed.getResponse().getContentAsString())
        .doesNotContain("Probability", "probability");
  }

  @Test
  void unknownFieldsAndFractionalStatValuesAreRejectedBeforeConversion() throws Exception {
    var extra = request();
    ((ObjectNode) extra.path("state").path("slots").get(1)).put("inventedWeight", 10);
    var fractional = request();
    ((ObjectNode) fractional.path("state").path("slots").get(0).path("modifier").path("values"))
        .put("maximum_life", 1.5);
    for (var invalid : List.of(extra, fractional))
      mvc.perform(
              post("/api/v1/crafting/workbench/abyss/validate")
                  .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(json.writeValueAsBytes(invalid)))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void missingRulesetCannotExecuteNewEndpoints() throws Exception {
    mvc.perform(
            post("/api/v1/crafting/workbench/abyss/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(request())))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void commonSlotsImportWithoutFlatteningAndOrdinaryEndpointStillRefuses() throws Exception {
    var common =
        fixture
            .rare(82, "", "amulet:prefix:hale")
            .withDesecrated(
                List.of(new ItemState.DesecratedSlot("hidden", AffixType.SUFFIX, null)));
    var body =
        json.writeValueAsBytes(
            Map.of("state", common, "action", "EXALTED", "activeOmens", List.of()));
    mvc.perform(
            post("/api/v1/crafting/workbench/abyss/import")
                .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.slots[1].id").value("hidden"))
        .andExpect(jsonPath("$.slots[1].desecrated").value(true));
    mvc.perform(
            post("/api/v1/crafting/workbench/apply")
                .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void importDoesNotDiscardDuplicateMarksForOneRevealedModifier() throws Exception {
    var state =
        fixture
            .rare(82, "", "amulet:prefix:hale")
            .withDesecrated(
                List.of(
                    new ItemState.DesecratedSlot("one", AffixType.PREFIX, "amulet:prefix:hale"),
                    new ItemState.DesecratedSlot("two", AffixType.PREFIX, "amulet:prefix:hale")));
    mvc.perform(
            post("/api/v1/crafting/workbench/abyss/import")
                .header(RulesetBoundary.HEADER, RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(Map.of("state", state))))
        .andExpect(status().isUnprocessableEntity());
  }
}
