package com.poe2craft.shared.crafting.api;

import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.ruleset.api.RulesetBoundary;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 규칙 버전별 불변 카탈로그를 응답하고 전송 형식별 HTTP 캐시를 구분한다.
 *
 * <p>기존 계약: Immutable presentation reference. The version in the URL must match the running engine.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code crafting}: 제작 ({@code CraftingOperations}).
 *   <li>{@code references}: 참조목록 ({@code
 *       com.poe2craft.shared.crafting.infrastructure.CatalogReferences}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code VersionedCatalogController(crafting, references)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       crafting}(제작), {@code references}(참조목록).
 *   <li>{@code shared(identity, hash, ids)}: 공유 카탈로그 자료를 조회한다. 입력: {@code identity}(정체성), {@code
 *       hash}(해시), {@code ids}(식별자목록). 반환: {@code ResponseEntity<?>}.
 *   <li>{@code catalog(identity, base, manualReveal, ids)}: 카탈로그 처리에서 {@code
 *       RulesetBoundary.identity}, {@code ResponseEntity.status}을(를) 사용한다. 입력: {@code
 *       identity}(정체성), {@code base}(베이스), {@code manualReveal}(수동·공개), {@code ids}(식별자목록). 반환:
 *       {@code ResponseEntity<?>}.
 *   <li>{@code tag(original, ids)}: 태그 처리에서 {@code GameIdMappings.active}, {@code
 *       original.substring}을(를) 사용한다. 입력: {@code original}(원본), {@code ids}(식별자목록). 반환: {@code
 *       String}.
 * </ul>
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
        .eTag(tag("W/\"" + hash + "\"", ids))
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
        .eTag(tag("\"" + identity + ":" + base + ":" + manualReveal + "\"", ids))
        .body(Map.of("rulesetIdentity", identity, "modifiers", initial.modifiers()));
  }

  private static String tag(String original, String ids) {
    return com.poe2craft.shared.ids.GameIdMappings.active().identity().equals(ids)
        ? original.substring(0, original.length() - 1) + ":" + ids + "\""
        : original;
  }
}
