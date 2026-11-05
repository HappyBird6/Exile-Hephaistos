package com.poe2craft.shared.crafting.api;

import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

/** Existing Solar bootstrap shared by the workbench, support and compatibility explorer. */
@RestController
@RequestMapping("/api/v1/crafting")
public final class CraftingCatalogController {
  private final ItemCatalog catalog;
  private final CraftingEngine engine;
  private final CraftingOperations workbench;

  public CraftingCatalogController(
      ItemCatalog catalog, CraftingEngine engine, CraftingOperations workbench) {
    this.catalog = catalog;
    this.engine = engine;
    this.workbench = workbench;
  }

  public Initial initial(int itemLevel) {
    return initial(itemLevel, false);
  }

  @GetMapping("/initial")
  public Initial initial(
      @RequestParam(defaultValue = "82") int itemLevel,
      @RequestParam(defaultValue = "false") boolean manualReveal) {
    var displayCatalog = manualReveal ? ManualRevealCatalog.enrich(catalog) : catalog;
    var bucket = StateBucket.from(SolarAmulet.initial(catalog, itemLevel, 15));
    return new Initial(
        CraftingEngine.RULE_VERSION,
        displayCatalog.metadata(),
        bucket.id(),
        bucket,
        displayCatalog.modifiers(),
        engine.actions(bucket),
        catalog.compatibleSnapshotIds(),
        catalog.base(),
        workbench.initial("solar", itemLevel, manualReveal).baseMaterialActions());
  }

  public record Initial(
      String ruleVersion,
      ItemCatalog.Metadata metadata,
      String id,
      StateBucket state,
      Map<String, ModifierDefinition> modifiers,
      List<CraftingEngine.Availability> actions,
      List<String> compatibleSnapshotIds,
      ItemCatalog.BaseItem baseRules,
      List<WorkbenchCurrency> baseMaterialActions) {}
}
