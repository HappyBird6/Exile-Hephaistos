package com.poe2craft.item;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.List;

/** Reviewed ordinary socket effects; Bonded and special socket-changing resources are excluded. */
public final class SocketAugments {
  private SocketAugments() {}

  private static final JsonNode CATALOG = load();

  private static JsonNode load() {
    try (var input = com.poe2craft.support.SealedResources.open("/catalog/augments.json")) {
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
    var next = new java.util.ArrayList<>(state.installedAugments());
    next.removeIf(a -> a.slotIndex() == slot);
    next.add(new ItemState.InstalledAugment(slot, id));
    next.sort(java.util.Comparator.comparingInt(ItemState.InstalledAugment::slotIndex));
    return state.withAugments(next);
  }
}
