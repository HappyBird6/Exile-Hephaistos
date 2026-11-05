package com.poe2craft.craftingsupport.recommendation.api;

import com.poe2craft.craftingsupport.recommendation.domain.SupportGoals;
import com.poe2craft.shared.crafting.domain.StateBucket;
import com.poe2craft.shared.item.ItemCatalog;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crafting/support")
public final class SupportAssessmentController {
  private final SupportGoals goals;

  public SupportAssessmentController(ItemCatalog catalog) {
    goals = new SupportGoals(catalog);
  }

  @GetMapping("/families")
  public List<SupportGoals.Family> families() {
    return goals.families();
  }

  @PostMapping("/assess")
  public SupportGoals.Assessment assess(@RequestBody Request request) {
    return goals.assess(request.state(), request.goal());
  }

  public record Request(StateBucket state, SupportGoals.Goal goal) {}
}
