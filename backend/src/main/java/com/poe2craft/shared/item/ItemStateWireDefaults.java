package com.poe2craft.shared.item;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 생략된 선택 boolean 필드만 false로 보충한다. 명시적 null은 유효값으로 바꾸지 않는다.
 *
 * <p>기존 계약: Optional boolean state flags have one wire default: omitted=false, explicit null is
 * invalid.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code ITEM_FLAGS}: 상태플래그 ({@code Set<String>}).
 *   <li>{@code MODIFIER_FLAGS}: 상태플래그 ({@code Set<String>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ItemStateWireDefaults()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code booleanFlags(record)}: boolean·상태플래그 처리에서 {@code Arrays.stream}, {@code
 *       record.getRecordComponents}을(를) 사용한다. 입력: {@code record}(기록). 반환: {@code Set<String>}.
 *   <li>{@code normalized(input)}: 입력 JSON 사본에 선택 필드 기본값을 채운다. 입력: {@code input}(입력). 반환: {@code
 *       JsonNode}.
 *   <li>{@code normalizeOwned(node)}: 파서가 방금 생성해 단독 소유한 트리에만 사용한다. 공유 응답·호출자 트리는 normalized로 복사한다.
 *       입력: {@code node}(노드). 사용하는 예외: {@code IllegalArgumentException}.
 * </ul>
 */
public final class ItemStateWireDefaults {
  public static final Set<String> ITEM_FLAGS = booleanFlags(ItemState.class);
  public static final Set<String> MODIFIER_FLAGS = booleanFlags(ModifierInstance.class);

  private ItemStateWireDefaults() {}

  private static Set<String> booleanFlags(Class<?> record) {
    return Arrays.stream(record.getRecordComponents())
        .filter(c -> c.getType() == boolean.class)
        .map(java.lang.reflect.RecordComponent::getName)
        .collect(Collectors.toUnmodifiableSet());
  }

  public static JsonNode normalized(JsonNode input) {
    var copy = input.deepCopy();
    normalizeOwned(copy);
    return copy;
  }

  /** 파서가 방금 생성해 단독 소유한 트리에만 사용한다. 공유 응답·호출자 트리는 normalized로 복사한다. */
  public static void normalizeOwned(JsonNode node) {
    if (node == null) throw new IllegalArgumentException("Item document required");
    if (node instanceof ObjectNode object) {
      Set<String> flags =
          object.path("baseItemId").isTextual() && object.path("explicits").isArray()
              ? ITEM_FLAGS
              : object.path("modifierId").isTextual() && object.path("values").isObject()
                  ? MODIFIER_FLAGS
                  : Set.of();
      for (String flag : flags) {
        if (!object.has(flag)) object.put(flag, false);
        else if (!object.get(flag).isBoolean())
          throw new IllegalArgumentException("Boolean item flag required");
      }
    }
    if (node.isContainerNode())
      node.elements().forEachRemaining(ItemStateWireDefaults::normalizeOwned);
  }
}
