package com.poe2craft.shared.crafting.api;

import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.ruleset.api.RulesetBoundary;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Immutable presentation reference. The version in the URL must match the running engine. */
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
  public ResponseEntity<?> shared(@PathVariable String identity, @PathVariable String hash) {
    if (!RulesetBoundary.identity().equals(identity))
      return ResponseEntity.status(409).cacheControl(CacheControl.noStore()).build();
    var body = references.find(hash);
    if (body == null) return ResponseEntity.notFound().build();
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
        .eTag("W/\"" + hash + "\"")
        .body(body);
  }

  @GetMapping("/{identity}/{base}")
  public ResponseEntity<?> catalog(
      @PathVariable String identity,
      @PathVariable String base,
      @RequestParam(defaultValue = "false") boolean manualReveal) {
    if (!RulesetBoundary.identity().equals(identity))
      return ResponseEntity.status(409).cacheControl(CacheControl.noStore()).build();
    var initial = crafting.initial(base, 82, manualReveal);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
        .eTag("\"" + identity + ":" + base + ":" + manualReveal + "\"")
        .body(Map.of("rulesetIdentity", identity, "modifiers", initial.modifiers()));
  }
}
