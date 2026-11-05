package com.poe2craft.shared.crafting.application;

import com.poe2craft.shared.item.ItemCatalog;
import java.util.List;

/** Read-only reviewed catalog access shared by workbench, goal evaluation and search. */
public interface CraftingCatalogs {
  String STOCKY_BASE_ID = "Metadata/Items/Armours/Gloves/FourGlovesStr1";

  List<ItemCatalog> reviewedCatalogs();
}
