package com.poe2craft.shared.ids;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;

/**
 * 식별자 필드·식별자 키만 변환한다. 설명·복사 텍스트·불명 값은 그대로 보존하고 키 충돌은 거절한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code mappings}: 매핑목록 ({@code GameIdMappings}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code GameIdCodec(mappings)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code mappings}(매핑목록).
 *   <li>{@code encode(input)}: 값을 저장·전송 표현으로 부호화한다. 입력: {@code input}(입력). 반환: {@code JsonNode}.
 *   <li>{@code decode(input)}: 저장·전송 표현을 내부 값으로 복원한다. 입력: {@code input}(입력). 반환: {@code JsonNode}.
 *   <li>{@code domain(field, parent)}: 필드 이름과 상위 객체 구조로 식별자 매핑 종류를 결정한다. 입력: {@code field}(필드),
 *       {@code parent}(상위값). 반환: {@code String}.
 *   <li>{@code visit(node, field, outer, parent, compact)}: 배열·객체를 순회하며 지정된 식별자만 변환하고 키 충돌을 거절한다.
 *       입력: {@code node}(노드), {@code field}(필드), {@code outer}(바깥필드), {@code parent}(상위값), {@code
 *       compact}(압축여부). 반환: {@code JsonNode}. 사용하는 예외: {@code IllegalArgumentException}.
 * </ul>
 */
public final class GameIdCodec {
  private final GameIdMappings mappings;

  public GameIdCodec(GameIdMappings mappings) {
    this.mappings = mappings;
  }

  public JsonNode encode(JsonNode input) {
    return visit(input, "", "", null, true);
  }

  public JsonNode decode(JsonNode input) {
    return visit(input, "", "", null, false);
  }

  private String domain(String field, JsonNode parent) {
    return switch (field) {
      case "baseItemId" -> "bases";
      case "modifierId", "implicitModifierId", "modifierIds" -> "modifiers";
      case "sourceStatId", "sourceStatIds", "statIds", "stat" -> "stats";
      case "familyIds" -> "families";
      case "tags" -> "tags";
      case "action", "baseAction", "baseMaterialActions" -> "actions";
      case "augmentId" -> "augments";
      case "itemClass" -> "itemClasses";
      case "id" ->
          parent != null && parent.has("affixType") && parent.has("stats")
              ? "modifiers"
              : parent != null && parent.has("rarePrefixes")
                  ? "bases"
                  : parent != null && parent.has("min") && parent.has("max") ? "stats" : "";
      default -> "";
    };
  }

  private JsonNode visit(
      JsonNode node, String field, String outer, JsonNode parent, boolean compact) {
    if (node == null) return NullNode.instance;
    if (node.isTextual())
      return TextNode.valueOf(mappings.convert(domain(field, parent), node.asText(), compact));
    if (node.isArray()) {
      var result = JsonNodeFactory.instance.arrayNode();
      for (var child : node) result.add(visit(child, field, outer, parent, compact));
      return result;
    }
    if (node.isObject()) {
      String keyDomain =
          field.equals("values")
              ? "stats"
              : field.equals("modifiers")
                  ? "modifiers"
                  : outer.equals("sanctification")
                          && (field.equals("implicits") || field.equals("explicits"))
                      ? "modifiers"
                      : (outer.equals("implicits") || outer.equals("explicits"))
                              && !node.has("modifierId")
                          ? "stats"
                          : "";
      var result = JsonNodeFactory.instance.objectNode();
      var entries = node.fields();
      while (entries.hasNext()) {
        var entry = entries.next();
        var key = mappings.convert(keyDomain, entry.getKey(), compact);
        if (result.has(key)) throw new IllegalArgumentException("Conflicting identifier keys");
        result.set(key, visit(entry.getValue(), entry.getKey(), field, node, compact));
      }
      return result;
    }
    return node;
  }
}
