package com.poe2craft.workbench.application;

import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import java.util.*;
import org.springframework.stereotype.Service;

/** Read-only candidate projection. Execution predicates remain in the crafting domain. */
@Service
public final class ModifierReferenceService {
  private final CraftingOperations crafting;
  private final com.fasterxml.jackson.databind.ObjectMapper json =
      new com.fasterxml.jackson.databind.ObjectMapper();
  private final Map<String, Reference> states;
  private final Map<String, Reference> revisions;
  private final Map<ItemCatalog, Reference> staticReferences;

  private static <V> Map<String, V> bounded(int maximum) {
    return Collections.synchronizedMap(
        new LinkedHashMap<String, V>(16, .75f, true) {
          @Override
          protected boolean removeEldestEntry(Map.Entry<String, V> entry) {
            return size() > maximum;
          }
        });
  }

  private String digest(Object value) {
    try {
      return java.util.HexFormat.of()
          .formatHex(
              java.security.MessageDigest.getInstance("SHA-256")
                  .digest(json.writeValueAsBytes(value)));
    } catch (java.io.IOException | java.security.NoSuchAlgorithmException error) {
      throw new IllegalStateException(error);
    }
  }

  /**
   * Exclude only explicit numeric rolls: the execution resolver uses modifier identities for
   * eligibility. Every other state field (including future fields) remains in the key. Never
   * normalize the live item.
   */
  private String stateKey(ItemState state, boolean dynamic) {
    com.fasterxml.jackson.databind.node.ObjectNode tree = json.valueToTree(state);
    for (var modifier : tree.withArray("explicits"))
      ((com.fasterxml.jackson.databind.node.ObjectNode) modifier).remove("values");
    return digest(List.of(dynamic, tree));
  }

  public record Update(
      String fromRevision,
      String revision,
      boolean reset,
      List<Entry> entries,
      List<String> removed,
      String staticRevision,
      List<Group> groups) {}

  public record Group(boolean eligible, List<String> reasons, List<Integer> indices) {}

  private static String entryKey(Entry entry) {
    return entry.kind() + ":" + entry.modifierId();
  }

  public Update update(String previous, ItemState state) {
    return update(previous, state, (String) null);
  }

  public Update update(String previous, ItemState state, boolean staticBasis) {
    return update(staticBasis ? null : previous, state, staticBasis ? previous : null);
  }

  public Update update(String previous, ItemState state, String staticRevision) {
    var after = describe(state, true);
    var before = previous == null ? null : revisions.get(previous);
    if (before == null && staticRevision != null) {
      var basis = describe(state, false);
      if (staticRevision.equals(basis.revision())) {
        // Explicit ordinal groups are presentation data, not client-side eligibility rules.
        var ordered =
            after.entries().stream()
                .sorted(Comparator.comparing(ModifierReferenceService::entryKey))
                .toList();
        var groups = new LinkedHashMap<List<String>, List<Integer>>();
        for (int i = 0; i < ordered.size(); i++) {
          var entry = ordered.get(i);
          if (!entry.eligible())
            groups.computeIfAbsent(entry.reasons(), ignored -> new ArrayList<>()).add(i);
        }
        var changes =
            groups.entrySet().stream()
                .map(e -> new Group(false, e.getKey(), List.copyOf(e.getValue())))
                .toList();
        return new Update(
            staticRevision,
            after.revision(),
            false,
            List.of(),
            List.of(),
            after.staticRevision(),
            changes);
      }
    }
    if (before == null)
      return new Update(
          null,
          after.revision(),
          true,
          after.entries(),
          List.of(),
          after.staticRevision(),
          List.of());
    if (previous.equals(after.revision()))
      return new Update(
          previous,
          after.revision(),
          false,
          List.of(),
          List.of(),
          after.staticRevision(),
          List.of());
    var old = new HashMap<String, Entry>();
    before.entries().forEach(entry -> old.put(entryKey(entry), entry));
    var changed = new ArrayList<Entry>();
    for (var entry : after.entries())
      if (!entry.equals(old.remove(entryKey(entry)))) changed.add(entry);
    return new Update(
        previous,
        after.revision(),
        false,
        List.copyOf(changed),
        old.keySet().stream().sorted().toList(),
        after.staticRevision(),
        List.of());
  }

  private final Map<ItemCatalog, ModifierPoolResolver> resolvers;

  public ModifierReferenceService(CraftingOperations crafting) {
    this(crafting, com.poe2craft.configuration.BackendSettings.defaults());
  }

  @org.springframework.beans.factory.annotation.Autowired
  public ModifierReferenceService(
      CraftingOperations crafting, com.poe2craft.configuration.BackendSettings settings) {
    this.crafting = crafting;
    staticReferences =
        Collections.synchronizedMap(
            new LinkedHashMap<>(16, .75f, true) {
              @Override
              protected boolean removeEldestEntry(Map.Entry<ItemCatalog, Reference> e) {
                return size() > settings.workbench().referenceCatalogEntries();
              }
            });
    states = bounded(settings.workbench().referenceStateEntries());
    revisions = bounded(settings.workbench().referenceStateEntries());
    resolvers =
        Collections.synchronizedMap(
            new LinkedHashMap<>(16, 0.75f, true) {
              @Override
              protected boolean removeEldestEntry(
                  Map.Entry<ItemCatalog, ModifierPoolResolver> entry) {
                return size() > settings.workbench().referenceCatalogEntries();
              }
            });
  }

  public record Entry(
      String modifierId,
      String kind,
      boolean eligible,
      List<String> reasons,
      List<String> actions) {}

  public record Reference(List<Entry> entries, String revision, String staticRevision) {}

  public Reference describe(ItemState input, boolean dynamic) {
    var catalog = crafting.abyssCatalog(input);
    var abyss = AbyssCraftingState.from(input, catalog);
    abyss.validate(catalog);
    if (!dynamic) {
      var shared = staticReferences.get(catalog);
      if (shared != null) {
        revisions.put(shared.revision(), shared);
        return shared;
      }
    }
    var key = stateKey(input, dynamic);
    var cached = states.get(key);
    if (cached != null) {
      revisions.put(cached.revision(), cached);
      return cached;
    }
    var simulator = crafting.simulator(input);
    // A normal item has no explicit modifiers; show the first addition's pool.
    var state =
        input.rarity() == ItemState.Rarity.NORMAL
            ? new ItemState(
                input.snapshotId(),
                input.baseItemId(),
                input.itemLevel(),
                ItemState.Rarity.MAGIC,
                input.implicits(),
                input.explicits(),
                input.conditions(),
                input.augmentSockets(),
                input.catalystQuality(),
                input.desecrated(),
                input.equipmentQuality(),
                input.observations(),
                input.sanctification(),
                input.installedAugments(),
                input.qualityInfused())
            : input;
    var natural =
        !dynamic
            ? Set.<String>of()
            : (input.desecrated().stream().anyMatch(s -> s.modifierId() == null)
                    ? new AbyssProbabilityComparison(catalog)
                        .pool(abyss, WorkbenchCurrency.EXALTED, null)
                    : simulator.pool(state, WorkbenchCurrency.EXALTED, null))
                .stream().map(ModifierDefinition::id).collect(java.util.stream.Collectors.toSet());
    var hidden =
        abyss.slots().stream().filter(s -> s.desecrated() && s.modifier() == null).toList();
    var reveal = new HashSet<String>();
    if (dynamic) for (var slot : hidden) reveal.addAll(abyss.eligibleRevealIds(slot.id(), catalog));
    var targets = new TreeMap<String, List<String>>();
    simulator
        .referenceEssenceTargets()
        .forEach(
            (action, ids) ->
                ids.forEach(id -> targets.computeIfAbsent(id, k -> new ArrayList<>()).add(action)));
    var entries = new ArrayList<Entry>();
    var resolver = resolvers.computeIfAbsent(catalog, ModifierPoolResolver::new);
    for (var d : catalog.modifiers().values()) {
      if (d.layer() != ModifierDefinition.Layer.EXPLICIT) continue;
      if ((d.weight() > 0 || d.tags().contains("augment_pool"))
          && !d.tags().contains("reveal_only")) {
        var reasons = dynamic ? exclusions(state, abyss, resolver, d, false) : List.<String>of();
        if (dynamic && !natural.contains(d.id()) && reasons.isEmpty())
          reasons = List.of("STATE_RESTRICTION");
        entries.add(
            new Entry(
                d.id(), "ordinary", !dynamic || natural.contains(d.id()), reasons, List.of()));
      }
      if (d.tags().contains("reveal_only") || d.tags().contains("unveiled_mod")) {
        var reasons =
            dynamic ? exclusions(state, abyss, resolver, d, !hidden.isEmpty()) : List.<String>of();
        if (dynamic && !hidden.isEmpty() && !reveal.contains(d.id()) && reasons.isEmpty())
          reasons = List.of("REVEAL_POOL");
        entries.add(new Entry(d.id(), "desecrated", reasons.isEmpty(), reasons, List.of()));
      }
      if (targets.containsKey(d.id())) {
        var reasons = dynamic ? exclusions(state, abyss, resolver, d, false) : List.<String>of();
        entries.add(
            new Entry(
                d.id(),
                "essence",
                reasons.isEmpty(),
                reasons,
                targets.get(d.id()).stream().sorted().toList()));
      }
    }
    // Stable across replicas even when upstream maps use a different iteration order.
    entries.sort(Comparator.comparing(ModifierReferenceService::entryKey));
    var staticEntries =
        entries.stream()
            .map(e -> new Entry(e.modifierId(), e.kind(), true, List.of(), e.actions()))
            .toList();
    var staticRevision =
        digest(List.of(input.snapshotId(), input.baseItemId(), false, staticEntries));
    var staticReference = new Reference(staticEntries, staticRevision, staticRevision);
    staticReferences.put(catalog, staticReference);
    var reference =
        dynamic
            ? new Reference(
                List.copyOf(entries),
                digest(List.of(input.snapshotId(), input.baseItemId(), true, entries)),
                staticRevision)
            : staticReference;
    states.put(key, reference);
    revisions.put(reference.revision(), reference);
    return reference;
  }

  private List<String> exclusions(
      ItemState state,
      AbyssCraftingState abyss,
      ModifierPoolResolver resolver,
      ModifierDefinition d,
      boolean replacingHidden) {
    int reservedPrefixes =
        (int)
            abyss.slots().stream()
                .filter(
                    slot ->
                        slot.modifier() == null
                            && slot.side() == ModifierDefinition.AffixType.PREFIX)
                .count();
    int reservedSuffixes =
        (int)
            abyss.slots().stream()
                .filter(
                    slot ->
                        slot.modifier() == null
                            && slot.side() == ModifierDefinition.AffixType.SUFFIX)
                .count();
    return resolver.exclusions(
        StateBucket.forModifierEligibility(state),
        d,
        reservedPrefixes,
        reservedSuffixes,
        replacingHidden);
  }
}
