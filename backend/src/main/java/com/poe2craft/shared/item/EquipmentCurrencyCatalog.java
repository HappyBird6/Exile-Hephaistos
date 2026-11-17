package com.poe2craft.shared.item;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.ruleset.SealedResources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 장비 제작 화폐의 공통 옵션과 베이스별 연결을 공유한다. 일반 옵션의 출현 가중치는 바꾸지 않는다.
 *
 * <p>private 생성자는 생성을 막고, DATA는 봉인 JSON을 한 번 읽은 Document다. 정적 초기화 블록은 옵션을 값 단위로 공유하고 targets의 모든
 * map/list를 불변으로 복사한다. LinkedHashMap 순서는 기존 snapshot 해시를 위해 보존한다. 중복 옵션·잘못된 layer/weight·없는 대상은
 * 거절한다. 읽기 실패는 ExceptionInInitializerError로 서버 초기화를 중단한다.
 *
 * <p>definitions는 공유 옵션 목록, targets(base)는 해당 베이스의 화폐→옵션 목록(없으면 빈 map)을 반환한다. enrich는 아직 없는 대상만
 * 추가하고 기존 snapshot을 compatible 목록에 남긴다. ids는 추가 순서, definitions는 결과 옵션, digest는 결과 정체성, metadata는
 * 갱신한 집계다. 마지막으로 AugmentCraftingRules.enrich를 적용하며 입력 카탈로그와 일반 가중치는 변경하지 않는다.
 */
public final class EquipmentCurrencyCatalog {
  private EquipmentCurrencyCatalog() {}

  /** definitions는 공통 특수 옵션, targets는 베이스 ID→화폐 ID→옵션 ID 목록이다. 로더에서 중첩 컬렉션까지 불변으로 만든다. */
  public record Document(
      List<ModifierDefinition> definitions, Map<String, Map<String, List<String>>> targets) {}

  private static final Document DATA;

  static {
    try (var input = SealedResources.open("/catalog/equipment-currencies.json")) {
      var parsed = new ObjectMapper().readValue(input, Document.class);
      var targets = new LinkedHashMap<String, Map<String, List<String>>>();
      parsed
          .targets()
          .forEach(
              (base, actions) -> {
                var copied = new LinkedHashMap<String, List<String>>();
                actions.forEach((action, ids) -> copied.put(action, List.copyOf(ids)));
                targets.put(base, Collections.unmodifiableMap(copied));
              });
      DATA =
          new Document(
              parsed.definitions().stream()
                  .map(com.poe2craft.shared.item.infrastructure.ItemCatalogLoader::sharedDefinition)
                  .toList(),
              Collections.unmodifiableMap(targets));
      var definitions = new HashMap<String, ModifierDefinition>();
      for (var d : DATA.definitions()) {
        if (d.weight() != 0
            || d.layer() != ModifierDefinition.Layer.EXPLICIT
            || definitions.put(d.id(), d) != null)
          throw new IllegalArgumentException("Invalid equipment currency definition");
      }
      DATA.targets()
          .forEach(
              (base, actions) ->
                  actions.forEach(
                      (action, ids) -> {
                        if (ids.isEmpty()
                            || new HashSet<>(ids).size() != ids.size()
                            || !definitions.keySet().containsAll(ids))
                          throw new IllegalArgumentException("Invalid equipment currency targets");
                      }));
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  public static List<ModifierDefinition> definitions() {
    return DATA.definitions();
  }

  public static Map<String, List<String>> targets(String base) {
    return DATA.targets().getOrDefault(base, Map.of());
  }

  public static ItemCatalog enrich(ItemCatalog catalog) {
    var ids = new LinkedHashSet<String>();
    targets(catalog.base().id()).values().forEach(ids::addAll);
    ids.removeAll(catalog.modifiers().keySet());
    if (ids.isEmpty()) return AugmentCraftingRules.enrich(catalog);
    var definitions = new ArrayList<>(catalog.modifiers().values());
    DATA.definitions().stream().filter(d -> ids.contains(d.id())).forEach(definitions::add);
    var old = catalog.metadata();
    String digest;
    try {
      digest =
          HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest((old.snapshotId() + ids).getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
    var metadata =
        new ItemCatalog.Metadata(
            "equipment-currency-" + digest,
            old.retrievedAt(),
            old.sourceUrl(),
            old.weightPolicy(),
            old.rawSha256(),
            old.detailsSha256(),
            (int)
                definitions.stream()
                    .filter(d -> d.affixType() == ModifierDefinition.AffixType.PREFIX)
                    .count(),
            (int)
                definitions.stream()
                    .filter(d -> d.affixType() == ModifierDefinition.AffixType.SUFFIX)
                    .count(),
            old.prefixWeight(),
            old.suffixWeight());
    var compatible = new ArrayList<>(catalog.compatibleSnapshotIds());
    compatible.add(old.snapshotId());
    return AugmentCraftingRules.enrich(
        new ItemCatalog(metadata, catalog.base(), definitions, compatible));
  }
}
