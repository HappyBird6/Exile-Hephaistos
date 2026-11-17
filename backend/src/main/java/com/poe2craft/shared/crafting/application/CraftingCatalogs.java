package com.poe2craft.shared.crafting.application;

import com.poe2craft.shared.item.ItemCatalog;
import java.util.List;

/**
 * 작업대·목표·탐색이 공유하는 읽기 전용 카탈로그 조회 계약이다.
 *
 * <p>기존 계약: Read-only reviewed catalog access shared by workbench, goal evaluation and search.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code STOCKY_BASE_ID}: 식별자; 초기값 {@code "Metadata/Items/Armours/Gloves/FourGlovesStr1"}
 *       ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code reviewedCatalogs()}: 검토된·카탈로그목록 값을 조회·구성한다. 반환: {@code List<ItemCatalog>}.
 * </ul>
 */
public interface CraftingCatalogs {
  String STOCKY_BASE_ID = "Metadata/Items/Armours/Gloves/FourGlovesStr1";

  List<ItemCatalog> reviewedCatalogs();
}
