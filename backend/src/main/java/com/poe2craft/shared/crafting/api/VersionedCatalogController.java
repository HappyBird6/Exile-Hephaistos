package com.poe2craft.shared.crafting.api;

import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.ruleset.api.RulesetBoundary;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 규칙 판본에 대응하는 카탈로그를 HTTP 캐시 헤더와 함께 제공한다. shared는 references에서 해시로 공용 자료를 찾고 catalog는 crafting에서
 * 베이스와 공개 설정에 해당하는 옵션 목록을 조회한다.
 */
@RestController
@RequestMapping("/api/v1/crafting/catalogs")
public final class VersionedCatalogController {
  private final CraftingOperations crafting;
  private final com.poe2craft.shared.crafting.infrastructure.CatalogReferences references;

  public VersionedCatalogController(
      CraftingOperations crafting,
      com.poe2craft.shared.crafting.infrastructure.CatalogReferences references) {
    this.references = references;
    this.crafting = crafting;
  }

  @GetMapping("/{identity}/shared/{hash}")
  public ResponseEntity<?> shared(
      @PathVariable String identity,
      @PathVariable String hash,
      @RequestHeader(name = com.poe2craft.shared.ids.GameIdMappings.HEADER, required = false)
          String ids) {
    if (!RulesetBoundary.identity().equals(identity))
      return ResponseEntity.status(409).cacheControl(CacheControl.noStore()).build();
    var body = references.find(hash);
    if (body == null) return ResponseEntity.notFound().build();
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
        .varyBy(com.poe2craft.shared.ids.GameIdMappings.HEADER)
        .eTag(
            com.poe2craft.shared.ids.GameIdMappings.active().catalogTag("W/\"" + hash + "\"", ids))
        .body(body);
  }

  @GetMapping("/{identity}/{base}")
  public ResponseEntity<?> catalog(
      @PathVariable String identity,
      @PathVariable String base,
      @RequestParam(defaultValue = "false") boolean manualReveal,
      @RequestHeader(name = com.poe2craft.shared.ids.GameIdMappings.HEADER, required = false)
          String ids) {
    if (!RulesetBoundary.identity().equals(identity))
      return ResponseEntity.status(409).cacheControl(CacheControl.noStore()).build();
    var initial = crafting.initial(base, 82, manualReveal);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
        .varyBy(com.poe2craft.shared.ids.GameIdMappings.HEADER)
        .eTag(
            com.poe2craft.shared.ids.GameIdMappings.active()
                .catalogTag("\"" + identity + ":" + base + ":" + manualReveal + "\"", ids))
        .body(Map.of("rulesetIdentity", identity, "modifiers", initial.modifiers()));
  }
}
