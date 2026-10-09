package com.poe2craft.crafting;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.bootstrap.CraftingConfiguration;
import com.poe2craft.crafting.domain.StateBucket;
import com.poe2craft.crafting.presentation.CraftingController;
import com.poe2craft.crafting.presentation.CraftingErrors;
import com.poe2craft.item.ItemCatalog;
import com.poe2craft.item.SolarAmulet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CraftingController.class)
@Import({CraftingConfiguration.class, CraftingErrors.class})
class CraftingControllerTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired ItemCatalog catalog;

  @Test
  void initialAndTransitionsUseTheBundledCatalog() throws Exception {
    mvc.perform(get("/api/v1/crafting/initial").param("itemLevel", "70"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state.itemLevel").value(70))
        .andExpect(jsonPath("$.state.rarity").value("NORMAL"))
        .andExpect(jsonPath("$.metadata.prefixCount").value(85))
        .andExpect(jsonPath("$.actions.length()").value(6));
    var root = StateBucket.from(SolarAmulet.initial(catalog));
    mvc.perform(
            post("/api/v1/crafting/transitions")
                .header(
                    "X-Crafting-Ruleset",
                    com.poe2craft.crafting.presentation.RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    mapper.writeValueAsBytes(Map.of("state", root, "action", "TRANSMUTATION"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available").value(true))
        .andExpect(jsonPath("$.outcomes.length()").value(209));
    mvc.perform(
            post("/api/v1/crafting/transitions")
                .header(
                    "X-Crafting-Ruleset",
                    com.poe2craft.crafting.presentation.RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(Map.of("state", root, "action", "EXALTED"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available").value(false))
        .andExpect(jsonPath("$.outcomes").isEmpty());
  }

  @Test
  void invalidRequestsAreProblemDetailsAndNeverExposeInternals() throws Exception {
    mvc.perform(get("/api/v1/crafting/initial").param("itemLevel", "0"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("INVALID_CRAFTING_REQUEST"));
    mvc.perform(get("/api/v1/crafting/initial").param("itemLevel", "wrong"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/v1/crafting/transitions")
                .header(
                    "X-Crafting-Ruleset",
                    com.poe2craft.crafting.presentation.RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.stackTrace").doesNotExist());
    mvc.perform(
            post("/api/v1/crafting/transitions")
                .header(
                    "X-Crafting-Ruleset",
                    com.poe2craft.crafting.presentation.RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
  }

  @Test
  void boundedExplorationReportsUnexploredMassInsteadOfRenormalizing() throws Exception {
    var root = StateBucket.from(SolarAmulet.initial(catalog));
    var body =
        Map.of(
            "state",
            root,
            "plan",
            List.of("TRANSMUTATION", "AUGMENTATION"),
            "maxNodes",
            1,
            "maxEdges",
            10,
            "maxMillis",
            1000);
    mvc.perform(
            post("/api/v1/crafting/explore")
                .header(
                    "X-Crafting-Ruleset",
                    com.poe2craft.crafting.presentation.RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(body)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.complete").value(false))
        .andExpect(jsonPath("$.unexploredProbability").value(1))
        .andExpect(jsonPath("$.edges").isEmpty());
    mvc.perform(
            post("/api/v1/crafting/explore")
                .header(
                    "X-Crafting-Ruleset",
                    com.poe2craft.crafting.presentation.RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    mapper.writeValueAsBytes(
                        Map.of(
                            "state",
                            root,
                            "plan",
                            List.of("TRANSMUTATION"),
                            "maxNodes",
                            2001,
                            "maxEdges",
                            10,
                            "maxMillis",
                            1000))))
        .andExpect(status().isUnprocessableEntity());
  }
}
