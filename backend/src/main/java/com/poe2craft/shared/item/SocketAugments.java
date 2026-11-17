package com.poe2craft.shared.item;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.ruleset.SealedResources;
import java.util.HashSet;
import java.util.List;

/**
 * 증강물 정의를 한 번 읽고 ID로 조회하며 장착 상태와 설치 제한을 검증한다.
 *
 * <p>private 생성자는 생성을 막는다. CATALOG는 load가 읽은 봉인 JSON, BY_ID는 index가 만든 조회 map이다. index는 중복 ID를
 * 거절한다. definition은 없는 ID에 IllegalArgumentException을 던진다. kind는 종류, supports는 베이스 분류의 지원 여부다.
 * definitions는 패키지 내부 표시 계산의 읽기 뷰이며 수정하면 안 된다. 외부 catalog 호출은 deepCopy를 반환해 요청자가 공유 원본을 바꾸지 못하게 한다.
 *
 * <p>validate(base,count,installed)는 소켓 한도·슬롯 중복·베이스 지원·장착 제한을 검사한다. 과거 상태의 legacyLimitExempt만 보존하며
 * 새 설치에는 예외를 적용하지 않는다. validateLimit는 같은 제한 그룹의 장착 수를 JSON count와 대조한다. 캐릭터 전체 장비는 모델링하지 않는다.
 * install(state,slot,id)은 신규 설치 허용·상태·소켓 고정 조건을 확인하고 복사한 next 목록에 교체한 뒤 정렬하여 새 ItemState를 반환한다. 실패는
 * IllegalArgumentException이고 원본 상태는 바뀌지 않는다. load의 누락·I/O 실패는 IllegalStateException이다.
 */
public final class SocketAugments {
  private SocketAugments() {}

  private static final JsonNode CATALOG = load();
  private static final java.util.Map<String, JsonNode> BY_ID = index();

  private static java.util.Map<String, JsonNode> index() {
    var entries = new java.util.LinkedHashMap<String, JsonNode>();
    for (var entry : CATALOG) {
      if (entries.putIfAbsent(entry.path("id").asText(), entry) != null)
        throw new IllegalArgumentException("Duplicate Augment identity");
    }
    return java.util.Collections.unmodifiableMap(entries);
  }

  /** 같은 패키지의 표시 계산만 읽는 뷰다. 노드를 수정하지 않으며 외부 응답은 catalog의 복사본을 사용한다. */
  static Iterable<JsonNode> definitions() {
    return BY_ID.values();
  }

  private static JsonNode load() {
    try (var input = SealedResources.open("/catalog/augments.json")) {
      if (input == null) throw new IllegalStateException("Missing Augment catalog");
      return new ObjectMapper().readTree(input);
    } catch (java.io.IOException e) {
      throw new IllegalStateException("Invalid Augment catalog", e);
    }
  }

  public static JsonNode catalog() {
    return CATALOG.deepCopy();
  }

  public static void validate(
      String base, Integer count, List<ItemState.InstalledAugment> installed) {
    if (installed.isEmpty()) return;
    var maximum = BaseRegistry.socketExecutionMaximum(base);
    if (count == null || maximum == null || count > maximum)
      throw new IllegalArgumentException("Installed Augments exceed the supported socket capacity");
    var seen = new HashSet<Integer>();
    for (var entry : installed) {
      if (entry == null
          || count == null
          || entry.slotIndex() < 0
          || entry.slotIndex() >= count
          || !seen.add(entry.slotIndex())
          || !supports(base, entry.augmentId()))
        throw new IllegalArgumentException("Invalid or unsupported installed Augment");
    }
    // Preserve original catalog saved states; new installations enforce source limits.
    for (var entry : installed) {
      if (!definition(entry.augmentId()).path("legacyLimitExempt").asBoolean(false))
        validateLimit(installed, entry.augmentId());
    }
  }

  public static String kind(String id) {
    return definition(id).path("kind").asText();
  }

  private static JsonNode definition(String id) {
    var entry = BY_ID.get(id);
    if (entry == null) throw new IllegalArgumentException("Unknown Augment");
    return entry;
  }

  /** Only this item's sockets are available; character-wide equipment is not modeled. */
  private static void validateLimit(List<ItemState.InstalledAugment> installed, String id) {
    var limit = definition(id).path("limit");
    if (limit.isMissingNode()) return;
    var group = limit.path("group").asText();
    long count =
        installed.stream()
            .filter(
                a -> group.equals(definition(a.augmentId()).path("limit").path("group").asText()))
            .count();
    if (count > limit.path("count").asInt())
      throw new IllegalArgumentException("Augment installation exceeds this item's socketed limit");
  }

  public static boolean supports(String base, String id) {
    var policy = BaseRegistry.policy(base);
    if (id == null || policy == null || policy.socketExecutionMaximum() == null) return false;
    var entry = BY_ID.get(id);
    return entry != null && entry.path("effects").path(policy.itemClass()).has("en");
  }

  public static ItemState install(ItemState state, int slot, String id) {
    var selected = BY_ID.get(id);
    boolean selectable = selected != null && selected.path("selectionEnabled").asBoolean(false);
    if (!selectable)
      throw new IllegalArgumentException("Augment is unavailable for new installation");
    if (!state.conditions().isEmpty() || state.observations() != null)
      throw new IllegalArgumentException(
          "Augment installation requires verified ordinary equipment");
    var previous =
        state.installedAugments().stream().filter(a -> a.slotIndex() == slot).findFirst();
    if (previous.isPresent() && AugmentCraftingRules.socketBound(previous.get().augmentId()))
      throw new IllegalArgumentException("Socket-bound Augments cannot be removed or replaced");
    var next = new java.util.ArrayList<>(state.installedAugments());
    next.removeIf(a -> a.slotIndex() == slot);
    next.add(new ItemState.InstalledAugment(slot, id));
    next.sort(java.util.Comparator.comparingInt(ItemState.InstalledAugment::slotIndex));
    validateLimit(next, id);
    return state.withAugments(next);
  }
}
