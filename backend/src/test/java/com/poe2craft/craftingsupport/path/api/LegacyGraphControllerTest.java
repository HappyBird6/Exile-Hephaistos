package com.poe2craft.craftingsupport.path.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.bootstrap.CatalogConfiguration;
import com.poe2craft.bootstrap.LegacyGraphConfiguration;
import com.poe2craft.bootstrap.http.CraftingErrors;
import com.poe2craft.shared.crafting.domain.StateBucket;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ManualRevealCatalog;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.ruleset.api.RulesetBoundary;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({
  LegacyGraphController.class,
  com.poe2craft.shared.crafting.api.CraftingCatalogController.class
})
@Import({CatalogConfiguration.class, LegacyGraphConfiguration.class, CraftingErrors.class})
class LegacyGraphControllerTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired ItemCatalog catalog;

  @Test
  void manualRevealBootstrapIsExplicitAndKeepsOrdinaryBootstrapUnchanged() throws Exception {
    var enriched = ManualRevealCatalog.enrich(catalog);
    mvc.perform(
            get("/api/v1/crafting/initial").param("itemLevel", "70").param("manualReveal", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state.itemLevel").value(70))
        .andExpect(jsonPath("$.metadata.prefixCount").value(enriched.metadata().prefixCount()))
        .andExpect(jsonPath("$.metadata.prefixWeight").value(catalog.metadata().prefixWeight()));
    mvc.perform(get("/api/v1/crafting/initial").param("itemLevel", "70"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.metadata.prefixCount").value(catalog.metadata().prefixCount()));
  }

  @Test
  void initialAndTransitionsUseTheBundledCatalog() throws Exception {
    mvc.perform(get("/api/v1/crafting/initial").param("itemLevel", "70"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state.itemLevel").value(70))
        .andExpect(jsonPath("$.state.rarity").value("NORMAL"))
        .andExpect(jsonPath("$.baseRules.magicPrefixes").value(catalog.base().magicPrefixes()))
        .andExpect(jsonPath("$.baseRules.magicSuffixes").value(catalog.base().magicSuffixes()))
        .andExpect(jsonPath("$.baseRules.rarePrefixes").value(catalog.base().rarePrefixes()))
        .andExpect(jsonPath("$.baseRules.rareSuffixes").value(catalog.base().rareSuffixes()))
        .andExpect(jsonPath("$.metadata.prefixCount").value(85))
        .andExpect(jsonPath("$.actions.length()").value(6));
    var root = StateBucket.from(SolarAmulet.initial(catalog));
    mvc.perform(
            post("/api/v1/crafting/transitions")
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    mapper.writeValueAsBytes(Map.of("state", root, "action", "TRANSMUTATION"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.available").value(true))
        .andExpect(jsonPath("$.outcomes.length()").value(209));
    mvc.perform(
            post("/api/v1/crafting/transitions")
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
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
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.stackTrace").doesNotExist());
    mvc.perform(
            post("/api/v1/crafting/transitions")
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
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
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(body)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.complete").value(false))
        .andExpect(jsonPath("$.unexploredProbability").value(1))
        .andExpect(jsonPath("$.edges").isEmpty());
    mvc.perform(
            post("/api/v1/crafting/explore")
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
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
