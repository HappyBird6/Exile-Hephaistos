package com.poe2craft.workbench.application;

import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.*;
import com.poe2craft.shared.item.*;
import java.util.*;
import org.springframework.stereotype.Service;

/**
 * 실행 규칙을 바꾸지 않고 읽기 전용 후보 옵션을 구성한다.
 *
 * <p>기존 계약: Read-only candidate projection. Execution predicates remain in the crafting domain.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code crafting}: 제작 ({@code CraftingOperations}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code com.fasterxml.jackson.databind.ObjectMapper}).
 *   <li>{@code states}: 상태목록 ({@code Map<String, Reference>}).
 *   <li>{@code revisions}: 리비전목록 ({@code Map<String, Reference>}).
 *   <li>{@code staticReferences}: static·참조목록 ({@code Map<ItemCatalog, Reference>}).
 *   <li>{@code resolvers}: resolvers ({@code Map<ItemCatalog, ModifierPoolResolver>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code bounded(maximum)}: 한도내 처리에서 {@code Collections.synchronizedMap}을(를) 사용한다. 입력: {@code
 *       maximum}(최대값). 반환: {@code Map<String, V>}.
 *   <li>{@code digest(value)}: 다이제스트 처리에서 {@code HexFormat.of}, {@code
 *       MessageDigest.getInstance}을(를) 사용한다. 입력: {@code value}(값). 반환: {@code String}. 사용하는 예외:
 *       {@code IllegalStateException}.
 *   <li>{@code stateKey(state, dynamic)}: 상태·키 처리에서 {@code json.valueToTree}, {@code
 *       tree.withArray}을(를) 사용한다. 입력: {@code state}(상태), {@code dynamic}(dynamic). 반환: {@code
 *       String}.
 *   <li>{@code entryKey(entry)}: 항목·키 처리에서 {@code entry.kind}, {@code entry.modifierId}을(를) 사용한다.
 *       입력: {@code entry}(항목). 반환: {@code String}.
 *   <li>{@code update(previous, state)}: 갱신의 입력에 따른 분기·결과를 처리한다. 입력: {@code previous}(이전), {@code
 *       state}(상태). 반환: {@code Update}.
 *   <li>{@code update(previous, state, staticBasis)}: 갱신의 입력에 따른 분기·결과를 처리한다. 입력: {@code
 *       previous}(이전), {@code state}(상태), {@code staticBasis}(static·Basis). 반환: {@code Update}.
 *   <li>{@code update(previous, state, staticRevision)}: 갱신 처리에서 {@code revisions.get}, {@code
 *       staticRevision.equals}을(를) 사용한다. 입력: {@code previous}(이전), {@code state}(상태), {@code
 *       staticRevision}(static·리비전). 반환: {@code Update}.
 *   <li>{@code ModifierReferenceService(crafting)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
 *       crafting}(제작).
 *   <li>{@code ModifierReferenceService(crafting, settings)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       crafting}(제작), {@code settings}(운영설정).
 *   <li>{@code describe(input, dynamic)}: describe 처리에서 {@code crafting.abyssCatalog}, {@code
 *       AbyssCraftingState.from}을(를) 사용한다. 입력: {@code input}(입력), {@code dynamic}(dynamic). 반환:
 *       {@code Reference}.
 *   <li>{@code exclusions(state, abyss, resolver, d, replacingHidden)}: exclusions 처리에서 {@code
 *       abyss.slots}, {@code slot.modifier}을(를) 사용한다. 입력: {@code state}(상태), {@code abyss}(훼손),
 *       {@code resolver}(resolver), {@code d}(d), {@code replacingHidden}(replacing·미공개). 반환:
 *       {@code List<String>}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Update}: {@code ModifierReferenceService}에서 사용하는 갱신의 불변 값이다.
 *   <li>{@code Group}: {@code ModifierReferenceService}에서 사용하는 그룹의 불변 값이다.
 *   <li>{@code Entry}: {@code ModifierReferenceService}에서 사용하는 항목의 불변 값이다.
 *   <li>{@code Reference}: {@code ModifierReferenceService}에서 사용하는 참조의 불변 값이다.
 * </ul>
 */
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

  /**
   * {@code ModifierReferenceService}에서 사용하는 갱신의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code fromRevision}: 원본·리비전 ({@code String}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code String}).
   *   <li>{@code reset}: reset ({@code boolean}).
   *   <li>{@code entries}: 항목목록 ({@code List<Entry>}).
   *   <li>{@code removed}: 제거된 ({@code List<String>}).
   *   <li>{@code staticRevision}: static·리비전 ({@code String}).
   *   <li>{@code groups}: 그룹목록 ({@code List<Group>}).
   * </ul>
   */
  public record Update(
      String fromRevision,
      String revision,
      boolean reset,
      List<Entry> entries,
      List<String> removed,
      String staticRevision,
      List<Group> groups) {}

  /**
   * {@code ModifierReferenceService}에서 사용하는 그룹의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code eligible}: 적격 ({@code boolean}).
   *   <li>{@code reasons}: 이유목록 ({@code List<String>}).
   *   <li>{@code indices}: indices ({@code List<Integer>}).
   * </ul>
   */
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

  /**
   * {@code ModifierReferenceService}에서 사용하는 항목의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code modifierId}: 실제 부여된 옵션 정의 식별자 ({@code String}).
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code eligible}: 적격 ({@code boolean}).
   *   <li>{@code reasons}: 이유목록 ({@code List<String>}).
   *   <li>{@code actions}: 행동목록 ({@code List<String>}).
   * </ul>
   */
  public record Entry(
      String modifierId,
      String kind,
      boolean eligible,
      List<String> reasons,
      List<String> actions) {}

  /**
   * {@code ModifierReferenceService}에서 사용하는 참조의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code entries}: 항목목록 ({@code List<Entry>}).
   *   <li>{@code revision}: 결과를 갱신할 때 비교하는 리비전 ({@code String}).
   *   <li>{@code staticRevision}: static·리비전 ({@code String}).
   * </ul>
   */
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
