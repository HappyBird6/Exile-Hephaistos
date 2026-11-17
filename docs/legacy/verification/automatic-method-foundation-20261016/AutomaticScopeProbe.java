import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.*;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.infrastructure.*;
import java.util.*;
public class AutomaticScopeProbe {
  public static void main(String[] args) throws Exception {
    WorkbenchDefinitionsLoader.initialize();
    var catalog = ItemCatalogLoader.loadDefault();
    var initial = SolarAmulet.initial(catalog, 82, 15);
    var root = new ItemState(initial.snapshotId(), initial.baseItemId(), 82, ItemState.Rarity.RARE,
        initial.implicits(), List.of(), initial.conditions(), initial.augmentSockets(), initial.catalystQuality());
    var coupled = catalog.modifiers().values().stream().filter(m -> m.stats().size() > 1)
        .map(ModifierDefinition::id).collect(java.util.stream.Collectors.toSet());
    var policy = AutomaticCurrencyPolicyLoader.load();
    var kernel = new AutomaticCraftingKernel(policy, catalog,
        new WorkbenchSimulator(catalog, new CraftingEngine(catalog), coupled), "amulets");
    var rows = new ArrayList<Map<String,Object>>();
    for (var action : AutomaticCraftingAction.candidates(policy)) {
      var result = kernel.expand(root, action, 100000);
      rows.add(Map.of("currency", action.currency().name(), "omens", action.omens(),
          "status", result.status(), "reason", result.reason(), "outcomes", result.outcomes().size()));
    }
    System.out.println(new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(rows));
  }
}
