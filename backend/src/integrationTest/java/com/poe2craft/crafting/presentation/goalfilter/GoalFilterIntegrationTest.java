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
