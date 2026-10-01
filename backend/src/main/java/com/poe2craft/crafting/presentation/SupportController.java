package com.poe2craft.crafting.presentation;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.ItemCatalog;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crafting/support")
public final class SupportController {
  private final SupportGoals goals;

  public SupportController(ItemCatalog catalog) {
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
