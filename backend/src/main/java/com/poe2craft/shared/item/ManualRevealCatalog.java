package com.poe2craft.shared.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.ruleset.SealedResources;
import java.util.*;

/**
 * 수동 공개에서 사용할 추가 정의를 공유한다. 출현 표시는 표본 가중치로 사용하지 않는다.
 *
 * <p>기존 계약: Additive manual-only definitions. Published eligibility markers are never sampling
 * weights.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code BASES}: 베이스목록 ({@code Map<String, List<ModifierDefinition>>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ManualRevealCatalog()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code load()}: 검증된 자료를 로딩한다. 반환: {@code Map<String, List<ModifierDefinition>>}. 사용하는 예외:
 *       {@code IllegalArgumentException}, {@code IllegalStateException}.
 *   <li>{@code enrich(catalog)}: enrich 처리에서 {@code BASES.getOrDefault}, {@code catalog.base}을(를)
 *       사용한다. 입력: {@code catalog}(카탈로그). 반환: {@code ItemCatalog}.
 * </ul>
 */
public final class ManualRevealCatalog {
  private ManualRevealCatalog() {}

  private static final Map<String, List<ModifierDefinition>> BASES = load();

  private static Map<String, List<ModifierDefinition>> load() {
    try (var in = SealedResources.open("/catalog/manual-reveal.json")) {
      var mapper = new ObjectMapper();
      var data = mapper.readTree(in);
      var definitions = new HashMap<String, ModifierDefinition>();
      for (var node : data.path("definitions")) {
        var d = mapper.treeToValue(node, ModifierDefinition.class);
        if (d.weight() != 0 || !d.tags().contains("reveal_only"))
          throw new IllegalArgumentException("Invalid manual reveal definition");
        definitions.put(d.id(), d);
      }
      var result = new HashMap<String, List<ModifierDefinition>>();
      data.path("bases")
          .fields()
          .forEachRemaining(
              entry -> {
                var list = new ArrayList<ModifierDefinition>();
                entry
                    .getValue()
                    .forEach(id -> list.add(Objects.requireNonNull(definitions.get(id.asText()))));
                result.put(entry.getKey(), List.copyOf(list));
              });
      return Map.copyOf(result);
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Cannot read manual reveal definitions", e);
    }
  }

  public static ItemCatalog enrich(ItemCatalog catalog) {
    return enrich(catalog, false);
  }

  /** 제작 목표에는 해당 베이스의 별세계 정의만 추가한다. */
  public static ItemCatalog enrichOtherworldly(ItemCatalog catalog) {
    return enrich(catalog, true);
  }

  /** 수동 공개 및 목표 선택에 필요한 정의를 동일한 베이스별 원본에서 가져온다. */
  private static ItemCatalog enrich(ItemCatalog catalog, boolean otherworldlyOnly) {
    var extra =
        BASES.getOrDefault(catalog.base().id(), List.of()).stream()
            .filter(d -> !otherworldlyOnly || d.tags().contains("otherworldly"))
            .filter(d -> catalog.find(d.id()).isEmpty())
            .toList();
    if (extra.isEmpty()) return catalog;
    var definitions = new ArrayList<>(catalog.modifiers().values());
    definitions.addAll(extra);
    var old = catalog.metadata();
    int prefixes =
        (int)
            extra.stream()
                .filter(d -> d.affixType() == ModifierDefinition.AffixType.PREFIX)
                .count();
    // Ordinary snapshot identity and weights stay intact. The sealed ruleset includes this additive
    // resource.
    return new ItemCatalog(
        new ItemCatalog.Metadata(
            old.snapshotId(),
            old.retrievedAt(),
            old.sourceUrl(),
            old.weightPolicy(),
            old.rawSha256(),
            old.detailsSha256(),
            old.prefixCount() + prefixes,
            old.suffixCount() + extra.size() - prefixes,
            old.prefixWeight(),
            old.suffixWeight()),
        catalog.base(),
        definitions,
        catalog.compatibleSnapshotIds());
  }
}
