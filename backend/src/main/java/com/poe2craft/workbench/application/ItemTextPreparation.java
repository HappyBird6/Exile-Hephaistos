package com.poe2craft.workbench.application;

import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.item.*;
import com.poe2craft.shared.item.text.*;

/**
 * One parse/mapping operation shared by imports and edits. No mutation of the supplied checkpoint.
 */
public final class ItemTextPreparation {
  private final CraftingOperations crafting;
  private final SolarTextMapper mapper;
  private final ItemTextService parser;

  public ItemTextPreparation(
      CraftingOperations crafting, SolarTextMapper mapper, ItemTextService parser) {
    this.crafting = crafting;
    this.mapper = mapper;
    this.parser = parser;
  }

  public record Prepared(ItemModels.Item parsed, SolarTextMapper.Result mapping) {}

  public Prepared prepare(String text, ItemState before) {
    var parsed = parser.parseText(text);
    var mapped = mapper.map(parsed);
    if (mapped.mapped()
        && before != null
        && before.baseItemId().equals(mapped.state().baseItemId())) {
      boolean importingSanctified =
          parsed.flags().stream().anyMatch(line -> line.raw().equals("Sanctified"))
              && !before.conditions().contains(ItemState.Condition.SANCTIFIED);
      if (!importingSanctified) {
        var merged = ItemTextEditor.merge(before, mapped, crafting.abyssCatalog(before));
        // These existing edit restrictions mean a complete replacement, preserving the old film.
        boolean replacement =
            !merged.mapped()
                && !before.conditions().contains(ItemState.Condition.SANCTIFIED)
                && merged.issues().stream()
                    .anyMatch(
                        issue ->
                            issue.message().contains("Socket and Rune lines")
                                || issue
                                    .message()
                                    .contains("catalyst quality and special conditions")
                                || issue.message().contains("Modifier edits cannot reconstruct"));
        if (!replacement) mapped = merged;
      }
    }
    return new Prepared(parsed, mapped);
  }
}
