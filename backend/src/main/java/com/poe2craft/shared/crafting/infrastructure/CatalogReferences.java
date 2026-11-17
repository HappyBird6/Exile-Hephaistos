package com.poe2craft.shared.crafting.infrastructure;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.item.BaseRegistry;
import com.poe2craft.shared.ruleset.api.RulesetBoundary;
import java.util.*;
import org.springframework.stereotype.Service;

/**
 * 번들 베이스 범위로 보유량을 제한한 불변 표시용 카탈로그를 공유한다.
 *
 * <p>기존 계약: Content-addressed immutable projections. Retention is bounded by the bundled base
 * registry.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code crafting}: 제작 ({@code CraftingOperations}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code aliases}: aliases ({@code Map<String, String>}).
 *   <li>{@code content}: 내용 ({@code Map<String, Projection>}).
 *   <li>{@code indexed}: 색인된 ({@code boolean}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code CatalogReferences(crafting, json)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code crafting}(제작),
 *       {@code json}(JSON).
 *   <li>{@code reference(base, reveal)}: 참조 처리에서 {@code BaseRegistry.registeredKeys}, {@code
 *       aliases.computeIfAbsent}을(를) 사용한다. 입력: {@code base}(베이스), {@code reveal}(공개). 반환: {@code
 *       String}. 사용하는 예외: {@code IllegalArgumentException}, {@code IllegalStateException}.
 *   <li>{@code find(hash)}: 조건에 맞는 항목을 찾는다. 입력: {@code hash}(해시). 반환: {@code JsonNode}.
 *   <li>{@code canonical(node, field)}: 정규형 처리에서 {@code node.isObject}, {@code
 *       json.createObjectNode}을(를) 사용한다. 입력: {@code node}(노드), {@code field}(필드). 반환: {@code
 *       JsonNode}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Projection}: {@code CatalogReferences}에서 사용하는 투영의 불변 값이다.
 * </ul>
 */
@Service
public final class CatalogReferences {
  private final CraftingOperations crafting;
  private final ObjectMapper json;
  private final Map<String, String> aliases = new HashMap<>();

  /**
   * {@code CatalogReferences}에서 사용하는 투영의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code base}: 베이스 ({@code String}).
   *   <li>{@code reveal}: 공개 ({@code boolean}).
   * </ul>
   */
  private record Projection(String base, boolean reveal) {}

  private final Map<String, Projection> content = new HashMap<>();
  private boolean indexed;

  public CatalogReferences(CraftingOperations crafting, ObjectMapper json) {
    this.crafting = crafting;
    this.json = json;
  }

  public synchronized String reference(String base, boolean reveal) {
    if (!BaseRegistry.registeredKeys().contains(base))
      throw new IllegalArgumentException("Unknown base");
    return aliases.computeIfAbsent(
        base + ":" + reveal,
        ignored -> {
          JsonNode definitions = json.valueToTree(crafting.initial(base, 82, reveal).modifiers());
          try {
            String hash =
                HexFormat.of()
                    .formatHex(
                        java.security.MessageDigest.getInstance("SHA-256")
                            .digest(json.writeValueAsBytes(canonical(definitions, ""))));
            content.putIfAbsent(hash, new Projection(base, reveal));
            return hash;
          } catch (java.io.IOException | java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
          }
        });
  }

  public synchronized JsonNode find(String hash) {
    if (!hash.matches("[a-f0-9]{64}")) return null;
    // A different replica may receive a content URL before receiving its initial request.
    // Scan the finite registry at most once; arbitrary hashes never grow either map.
    if (!content.containsKey(hash) && !indexed) {
      for (String base : BaseRegistry.registeredKeys()) {
        reference(base, false);
        reference(base, true);
      }
      indexed = true;
    }
    var projection = content.get(hash);
    return projection == null
        ? null
        : json.createObjectNode()
            .put("rulesetIdentity", RulesetBoundary.identity())
            .set(
                "modifiers",
                json.valueToTree(
                    crafting.initial(projection.base(), 82, projection.reveal()).modifiers()));
  }

  private JsonNode canonical(JsonNode node, String field) {
    if (node.isObject()) {
      var result = json.createObjectNode();
      var names = new TreeSet<String>();
      node.fieldNames().forEachRemaining(names::add);
      names.forEach(name -> result.set(name, canonical(node.get(name), name)));
      return result;
    }
    if (node.isArray()) {
      var children = new ArrayList<JsonNode>();
      node.forEach(v -> children.add(canonical(v, "")));
      if (field.equals("tags")) children.sort(Comparator.comparing(JsonNode::asText));
      var result = json.createArrayNode();
      children.forEach(result::add);
      return result;
    }
    return node;
  }
}
