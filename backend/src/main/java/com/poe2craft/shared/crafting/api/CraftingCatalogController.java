package com.poe2craft.shared.crafting.api;

import com.poe2craft.shared.crafting.api.dto.Initial;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import org.springframework.web.bind.annotation.*;

/**
 * 초기 카탈로그의 GET 요청을 처리한다. 생성자로 받은 catalog는 옵션 사전, engine은 행동 계산, workbench는 베이스 재료 조회를 제공한다.
 * initial은 아이템 레벨과 수동 공개 설정에 맞는 응답을 반환한다.
 */
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

  @GetMapping("/initial")
  public Initial initial(
      @RequestParam(defaultValue = "82") int itemLevel,
      @RequestParam(defaultValue = "false") boolean manualReveal) {
    return Initial.from(catalog, engine, workbench, itemLevel, manualReveal);
  }
}
