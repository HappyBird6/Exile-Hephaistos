package com.poe2craft.workbench.application;

import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import java.util.List;
import java.util.Optional;

/** Validates a new checkpoint. Saved films and original ruleset receipts remain client-owned. */
public final class SessionCompatibilityService {
  private final CraftingOperations crafting;

  public SessionCompatibilityService(CraftingOperations crafting) {
    this.crafting = crafting;
  }

  public record Compatibility(
      boolean compatible,
      boolean sameRules,
      String compatibilityId,
      ItemState state,
      AbyssCraftingState abyss,
      List<String> reasons) {}

  public Optional<ItemCatalog> catalog(String baseItemId) {
    return crafting.reviewedCatalogs().stream()
        .filter(c -> c.base().id().equals(baseItemId))
        .findFirst();
  }

  public Compatibility check(ItemState state, AbyssCraftingState abyss, String previousKey) {
    var catalog = crafting.abyssCatalog(state);
    if (abyss != null && !abyss.storedItem().equals(state)) return rejected("INCONSISTENT_ABYSS");
    if (abyss == null && !state.desecrated().isEmpty())
      abyss = AbyssCraftingState.from(state, catalog);
    if (abyss != null) abyss.validate(catalog);
    else {
      var violations = new ItemStateValidator(catalog).validate(state);
      if (!violations.isEmpty())
        return new Compatibility(
            false,
            false,
            null,
            null,
            null,
            violations.stream().map(v -> v.code().toString()).distinct().toList());
    }
    var key = crafting.simulator(state).compatibilityId();
    return new Compatibility(true, key.equals(previousKey), key, state, abyss, List.of());
  }

  public static Compatibility rejected(String reason) {
    return new Compatibility(false, false, null, null, null, List.of(reason));
  }
}
