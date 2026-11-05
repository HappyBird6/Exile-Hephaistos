package com.poe2craft.craftingsupport.recommendation.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.bootstrap.CatalogConfiguration;
import com.poe2craft.bootstrap.http.CraftingErrors;
import com.poe2craft.craftingsupport.recommendation.domain.SupportGoals;
import com.poe2craft.shared.crafting.domain.StateBucket;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.SolarAmulet;
import com.poe2craft.shared.ruleset.api.RulesetBoundary;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SupportAssessmentController.class)
@Import({CatalogConfiguration.class, CraftingErrors.class})
class SupportAssessmentControllerTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired ItemCatalog catalog;

  @Test
  void publishesCatalogFamiliesAndAssessesActualRoot() throws Exception {
    mvc.perform(get("/api/v1/crafting/support/families"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(30));
    var body =
        Map.of(
            "state",
            StateBucket.from(SolarAmulet.initial(catalog)),
            "goal",
            new SupportGoals.Goal(
                List.of(new SupportGoals.Condition("IncreasedLife", 2)), List.of(), 0));
    mvc.perform(
            post("/api/v1/crafting/support/assess")
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsBytes(body)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("READY"))
        .andExpect(jsonPath("$.achieved").value(false));
  }

  @Test
  void malformedStateUsesProblemDetailsInsteadOfAZeroProbability() throws Exception {
    mvc.perform(
            post("/api/v1/crafting/support/assess")
                .header("X-Crafting-Ruleset", RulesetBoundary.identity())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("INVALID_CRAFTING_REQUEST"));
  }
}
