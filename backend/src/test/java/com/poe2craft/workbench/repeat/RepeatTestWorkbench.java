package com.poe2craft.workbench.repeat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.bootstrap.CatalogConfiguration;
import com.poe2craft.shared.crafting.application.CatalogCraftingService;
import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.SolarAmulet;
import java.util.*;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/** Real reviewed bootstrap without Spring Boot, web listeners, DB, Redis or Docker. */
public final class RepeatTestWorkbench {
  private RepeatTestWorkbench() {}

  private static final AnnotationConfigApplicationContext CONTEXT =
      new AnnotationConfigApplicationContext(CatalogConfiguration.class);
  public static final CatalogCraftingService WORKBENCH =
      CONTEXT.getBean(CatalogCraftingService.class);
  public static final ObjectMapper JSON = new ObjectMapper();
  public static final ChaosDisplayResources DISPLAY = new ChaosDisplayResources(JSON);
  public static final String ID = "123e4567-e89b-42d3-a456-426614174000";

  public static ItemState rare() {
    var catalog =
        WORKBENCH.reviewedCatalogs().stream()
            .filter(c -> c.base().id().equals(SolarAmulet.BASE_ID))
            .findFirst()
            .orElseThrow();
    return WORKBENCH
        .apply(SolarAmulet.initial(catalog), WorkbenchCurrency.ALCHEMY, Set.of(), new Random(4))
        .state();
  }

  public static AbyssCraftingState abyss(boolean hidden) {
    var item = rare();
    var catalog = WORKBENCH.abyssCatalog(item);
    var state = AbyssCraftingState.from(item, catalog);
    if (!hidden) return state;
    var slots = new ArrayList<>(state.slots());
    var last = slots.getLast();
    slots.set(slots.size() - 1, new AbyssCraftingState.Slot(last.id(), last.side(), null, true));
    var result = new AbyssCraftingState(state.item(), slots, null);
    result.validate(catalog);
    return result;
  }

  public static ChaosRepeatProtocol.Input input(
      ChaosRepeatProtocol.State state,
      WorkbenchCurrency action,
      int max,
      Set<String> omens,
      String pattern,
      String locale) {
    return new ChaosRepeatProtocol.Input(
        ID, "0".repeat(64), state, action, omens, pattern, locale, max);
  }
}
