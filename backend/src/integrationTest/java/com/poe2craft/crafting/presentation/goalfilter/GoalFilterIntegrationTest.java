package com.poe2craft.crafting.presentation.goalfilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.domain.goalfilter.*;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.*;
import com.poe2craft.item.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Runs against the integration owner's bootstrap registration and disposable DB/Redis only. */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class GoalFilterIntegrationTest {
  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.6-alpine");

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7.4.5-alpine").withExposedPorts(6379);

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.flyway.locations", () -> "classpath:db/migration,classpath:db/testmigration");
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("spring.data.redis.host", REDIS::getHost);
    registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired ItemCatalog itemCatalog;
  final String root = "/api/v1/crafting/support/goal-filters";

  @Test
  void automaticRoutesPreserveGoalAndQualityContextAcrossCancelAndRecalculation() throws Exception {
    var initial = SolarAmulet.initial(itemCatalog);
    var item =
        new ItemState(
            initial.snapshotId(),
            initial.baseItemId(),
            initial.itemLevel(),
            ItemState.Rarity.RARE,
            initial.implicits(),
            List.of(),
            initial.conditions());
    var version =
        json.readTree(
                mvc.perform(
                        get(root + "/catalog")
                            .param("snapshotId", item.snapshotId())
                            .param("baseItemId", item.baseItemId())
                            .param("itemLevel", Integer.toString(item.itemLevel())))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("catalogVersion")
            .asText();
    var goal =
        new GoalFilter(
            1,
            version,
            new General(item.baseItemId(), new Range(null, null), List.of(ItemState.Rarity.RARE)),
            List.of(
                new Group(
                    "g",
                    Type.AND,
                    false,
                    null,
                    List.of(
                        new Entry(
                            "e",
                            GoalCatalogIndex.id("implicit", SolarAmulet.SPIRIT_STAT),
                            "flat",
                            new Range(BigDecimal.valueOf(30), null),
                            null,
                            false)))));
    var endpoint = "/api/v2/crafting/method-searches";
    var provenance =
        json.readTree(
            mvc.perform(get(endpoint + "/provenance").param("baseItemId", item.baseItemId()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    var input =
        Map.of(
            "request",
            Map.of(
                "version",
                2,
                "clientRequestId",
                UUID.randomUUID().toString(),
                "start",
                Map.of("item", item, "provenance", provenance),
                "goal",
                goal,
                "activeOmens",
                List.of()),
            "abstractQuality40",
            true);
    var created =
        mvc.perform(
                post(endpoint + "/automatic")
                    .header(
                        "X-Crafting-Ruleset",
                        com.poe2craft.crafting.presentation.RulesetBoundary.identity())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsBytes(input)))
            .andExpect(status().isAccepted())
            .andReturn()
            .getResponse();
    var cookieHeader = created.getHeader("Set-Cookie");
    org.assertj.core.api.Assertions.assertThat(cookieHeader)
        .contains("HttpOnly", "SameSite=Strict");
    var cookie =
        new jakarta.servlet.http.Cookie(
            "crafting_method_client", cookieHeader.split(";", 2)[0].split("=", 2)[1]);
    var job = json.readTree(created.getContentAsString());
    var url = endpoint + "/automatic/" + job.get("id").asText();
    mvc.perform(get(url)).andExpect(status().isNotFound());
    mvc.perform(get(url + "/request").cookie(cookie))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.abstractQuality40").value(true))
        .andExpect(jsonPath("$.request.goal.groups[0].entries[0].id").value("e"));
    var advanced =
        json.readTree(
            mvc.perform(
                    post(url + "/control")
                        .header(
                            "X-Crafting-Ruleset",
                            com.poe2craft.crafting.presentation.RulesetBoundary.identity())
                        .cookie(cookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                            json.writeValueAsBytes(
                                Map.of(
                                    "commandId",
                                    "advance",
                                    "revision",
                                    1,
                                    "generation",
                                    1,
                                    "operation",
                                    "ADVANCE"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    mvc.perform(
            post(url + "/control")
                .header(
                    "X-Crafting-Ruleset",
                    com.poe2craft.crafting.presentation.RulesetBoundary.identity())
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsBytes(
                        Map.of(
                            "commandId",
                            "cancel",
                            "revision",
                            advanced.get("revision").asLong(),
                            "generation",
                            1,
                            "operation",
                            "CANCEL"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELLED"))
        .andExpect(jsonPath("$.generation").value(2));
    mvc.perform(
            post(url + "/control")
                .header(
                    "X-Crafting-Ruleset",
                    com.poe2craft.crafting.presentation.RulesetBoundary.identity())
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"commandId\":\"late\",\"revision\":1,\"generation\":1,\"operation\":\"ADVANCE\"}"))
        .andExpect(status().isConflict());
    mvc.perform(
            post(url + "/control")
                .header(
                    "X-Crafting-Ruleset",
                    com.poe2craft.crafting.presentation.RulesetBoundary.identity())
                .cookie(cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"commandId\":\"bad\",\"revision\":1,\"generation\":1,\"operation\":null}"))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void registeredEndpointUsesRealItemValuesAndLegacyFamilyEndpointStillExists() throws Exception {
    var item = SolarAmulet.initial(itemCatalog);
    var catalogResponse =
        mvc.perform(
                get(root + "/catalog")
                    .param("snapshotId", item.snapshotId())
                    .param("baseItemId", item.baseItemId())
                    .param("itemLevel", "82"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var version = json.readTree(catalogResponse).get("catalogVersion").asText();
    var stat = GoalCatalogIndex.id("implicit", SolarAmulet.SPIRIT_STAT);
    var goal =
        new GoalFilter(
            1,
            version,
            new General(
                item.baseItemId(),
                new Range(BigDecimal.ONE, BigDecimal.valueOf(100)),
                List.of(ItemState.Rarity.NORMAL)),
            List.of(
                new Group(
                    "g",
                    Type.AND,
                    false,
                    null,
                    List.of(
                        new Entry(
                            "r",
                            stat,
                            "flat",
                            new Range(BigDecimal.valueOf(15), BigDecimal.valueOf(15)),
                            null,
                            false)))));
    mvc.perform(
            post(root + "/evaluate")
                .header(
                    "X-Crafting-Ruleset",
                    com.poe2craft.crafting.presentation.RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("item", item, "goal", goal))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("MATCH"))
        .andExpect(jsonPath("$.groups[0].entries[0].value").value(15));
    mvc.perform(
            post(root + "/recommend")
                .header(
                    "X-Crafting-Ruleset",
                    com.poe2craft.crafting.presentation.RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json.writeValueAsString(
                        Map.of(
                            "item",
                            item,
                            "goal",
                            goal,
                            "activeOmens",
                            List.of(),
                            "limits",
                            Map.of("maxStates", 100, "maxEdges", 100, "maxMillis", 100)))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.evaluation").value("MATCH"))
        .andExpect(jsonPath("$.probability.modelVersion").value("solar-numeric-addition-v1"))
        .andExpect(jsonPath("$.comparisons").isNotEmpty());
    mvc.perform(get("/api/v1/crafting/support/families"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").exists());
  }
}
