package com.poe2craft.crafting.application;

import com.poe2craft.crafting.domain.*;
import com.poe2craft.item.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Goal-independent family occupancy cache. Returned channels are attached to the caller's tiers.
 */
public final class AdditionPoolCache {
  public static final String PROJECTION_VERSION = "solar-family-pool-v2-omen-composition";
  private final ItemCatalog catalog;
  private final AdditionRules rules;
  private final CraftingEngine validator;
  private final AdditionPoolStore store;
  private final String namespace;
  private final LinkedHashMap<String, Data> memory = new LinkedHashMap<>(16, .75f, true);
  private final int maxEntries;
  private final AtomicLong hits = new AtomicLong(),
      computed = new AtomicLong(),
      persistedHits = new AtomicLong();

  public AdditionPoolCache(
      ItemCatalog catalog,
      AdditionPoolStore store,
      String ruleVersion,
      String ledgerDigest,
      int maxEntries) {
    this.catalog = catalog;
    this.store = store;
    this.maxEntries = maxEntries;
    rules = new AdditionRules(catalog);
    validator = new CraftingEngine(catalog);
    String normalizedCatalog =
        catalog.modifiers().values().stream()
            .sorted(Comparator.comparing(ModifierDefinition::id))
            .map(
                d ->
                    d.id()
                        + ":"
                        + d.layer()
                        + ":"
                        + d.affixType()
                        + ":"
                        + d.familyIds().stream().sorted().toList()
                        + ":"
                        + d.requiredItemLevel()
                        + ":"
                        + d.weight()
                        + ":"
                        + d.tier()
                        + ":"
                        + d.stats())
            .reduce("", (a, b) -> a + "\n" + b);
    namespace =
        hash(
            PROJECTION_VERSION
                + "|"
                + catalog.metadata()
                + "|"
                + catalog.base()
                + "|"
                + ruleVersion
                + "|"
                + ledgerDigest
                + "|"
                + hash(normalizedCatalog));
  }

  public synchronized Data get(
      StateBucket state, WorkbenchCurrency action, Set<String> activeOmens) {
    validator.validate(state);
    Objects.requireNonNull(action);
    var omens = activeOmens == null ? Set.<String>of() : Set.copyOf(activeOmens);
    omens.forEach(WorkbenchOmen::fromId);
    var occupied = new TreeSet<String>();
    for (var id : state.modifierIds()) occupied.addAll(catalog.find(id).orElseThrow().familyIds());
    String key =
        hash(
            state.snapshotId()
                + "|"
                + state.baseItemId()
                + "|"
                + state.itemLevel()
                + "|"
                + state.rarity()
                + "|"
                + state.implicits()
                + "|"
                + state.conditions().stream().sorted().toList()
                + "|"
                + occupied
                + "|"
                + action
                + "|"
                + omens.stream().sorted().toList());
    var cached = memory.get(key);
    if (cached != null) {
      hits.incrementAndGet();
      return cached;
    }
    var stored = store.find(namespace, key);
    if (stored.isPresent()) {
      cached = stored.get();
      validateData(cached);
      persistedHits.incrementAndGet();
      remember(key, cached);
      return cached;
    }
    var plan = rules.plan(state, action, omens);
    long total = plan.candidates().stream().mapToLong(ModifierDefinition::weight).sum();
    cached =
        new Data(
            plan.available(),
            plan.reason(),
            plan.upgraded().rarity(),
            plan.candidates().stream().map(d -> new Channel(d.id(), d.weight(), total)).toList(),
            plan.consumedOmens(),
            plan.remainingOmens());
    validateData(cached);
    store.save(namespace, key, cached);
    computed.incrementAndGet();
    remember(key, cached);
    return cached;
  }

  private void remember(String key, Data data) {
    memory.put(key, data);
    while (memory.size() > maxEntries) memory.remove(memory.keySet().iterator().next());
  }

  private void validateData(Data data) {
    if (data.available() != !data.channels().isEmpty())
      throw new IllegalStateException("Cached pool availability mismatch");
    if (!data.available()) return;
    long sum = 0;
    var ids = new HashSet<String>();
    for (var c : data.channels()) {
      var d = catalog.find(c.modifierId()).orElseThrow();
      if (!ids.add(c.modifierId())
          || d.layer() != ModifierDefinition.Layer.EXPLICIT
          || c.weight() != d.weight()
          || c.weight() <= 0) throw new IllegalStateException("Invalid stored channel");
      sum = Math.addExact(sum, c.weight());
    }
    final long total = sum;
    if (data.channels().stream().anyMatch(c -> c.totalWeight() != total))
      throw new IllegalStateException("Cached pool mass mismatch");
  }

  public String namespace() {
    return namespace;
  }

  public Stats stats() {
    return new Stats(hits.get(), persistedHits.get(), computed.get());
  }

  public static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public record Stats(long memoryHits, long persistedHits, long computedPools) {}

  public record Channel(String modifierId, long weight, long totalWeight) {
    public double probability() {
      return (double) weight / totalWeight;
    }
  }

  public record Data(
      boolean available,
      String reason,
      ItemState.Rarity rarity,
      List<Channel> channels,
      List<String> consumedOmens,
      List<String> remainingOmens) {
    public Data {
      channels = List.copyOf(channels);
      consumedOmens = List.copyOf(consumedOmens);
      remainingOmens = List.copyOf(remainingOmens);
    }
  }
}
