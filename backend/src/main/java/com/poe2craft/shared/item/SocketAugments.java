package com.poe2craft.shared.item;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.ruleset.SealedResources;
import java.util.HashSet;
import java.util.List;

/** Reviewed ordinary socket effects; special state-changing resources are deferred. */
public final class SocketAugments {
  private SocketAugments() {}

  private static final JsonNode CATALOG = load();

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
    for (var entry : CATALOG) if (entry.path("id").asText().equals(id)) return entry;
    throw new IllegalArgumentException("Unknown Augment");
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
    for (var entry : CATALOG) {
      if (entry.path("id").asText().equals(id))
        return entry.path("effects").path(policy.itemClass()).has("en");
    }
    return false;
  }

  public static ItemState install(ItemState state, int slot, String id) {
    boolean selectable = false;
    for (var entry : CATALOG) {
      if (entry.path("id").asText().equals(id)) {
        selectable = entry.path("selectionEnabled").asBoolean(false);
        break;
      }
    }
    if (!selectable)
      throw new IllegalArgumentException("Augment is unavailable for new installation");
    if (!state.conditions().isEmpty()
        || state.observations() != null
        || !state.desecrated().isEmpty())
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
