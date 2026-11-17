package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.probability.Fraction;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/**
 * 모든 기본 행동의 가중치가 같은 계열끼리만 교환 가능한 묶음을 만든다.
 *
 * <p>기존 계약: Permutations are allowed only for identical complete basic-action weight profiles.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "shared-pool-family-permutation-v3"} ({@code String}).
 *   <li>{@code members}: 구성원목록 ({@code Map<String, Member>}).
 *   <li>{@code cells}: 셀목록 ({@code Map<OrbitCell.Profile, Map<Map<Integer, Integer>, OrbitCell>>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code OrbitPartition(members)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code members}(구성원목록).
 *   <li>{@code empty()}: 빈 초기값 또는 비어 있는지 여부를 구한다. 반환: {@code OrbitPartition}.
 *   <li>{@code compile(catalog, root, rolls, eligibility)}: 입력 조건을 재사용할 평가·실행 표현으로 바꾼다. 입력: {@code
 *       catalog}(카탈로그), {@code root}(시작점), {@code rolls}(수치굴림목록), {@code eligibility}(생성적격). 반환:
 *       {@code OrbitPartition}. 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code digest(value)}: 다이제스트 처리에서 {@code HexFormat.of}, {@code
 *       MessageDigest.getInstance}을(를) 사용한다. 입력: {@code value}(값). 반환: {@code String}. 사용하는 예외:
 *       {@code IllegalStateException}.
 *   <li>{@code validateSource(source)}: 출처을(를) 검증한다. 입력: {@code source}(출처). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code profile(modifierId)}: 특성 처리에서 {@code members.get}, {@code Optional.empty}을(를) 사용한다.
 *       입력: {@code modifierId}(옵션·식별자). 반환: {@code Optional<OrbitCell.Profile>}.
 *   <li>{@code normalize(state, rolls, tiers)}: 정규화 처리에서 {@code state.item}, {@code
 *       members.get}을(를) 사용한다. 입력: {@code state}(상태), {@code rolls}(수치굴림목록), {@code tiers}(티어목록).
 *       반환: {@code DeferredItem}. 사용하는 예외: {@code IllegalStateException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Key}: {@code OrbitPartition}에서 사용하는 키의 불변 값이다.
 *   <li>{@code Member}: {@code OrbitPartition}에서 사용하는 구성원의 불변 값이다.
 * </ul>
 */
public final class OrbitPartition {
  public static final String VERSION = "shared-pool-family-permutation-v3";

  /**
   * {@code OrbitPartition}에서 사용하는 키의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code affix}: 접두접미옵션 ({@code ModifierDefinition.AffixType}).
   *   <li>{@code weights}: 가중치목록 ({@code Map<Integer, Long>}).
   * </ul>
   */
  private record Key(ModifierDefinition.AffixType affix, Map<Integer, Long> weights) {}

  /**
   * {@code OrbitPartition}에서 사용하는 구성원의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code profile}: 특성 ({@code OrbitCell.Profile}).
   *   <li>{@code category}: 분류 ({@code int}).
   * </ul>
   */
  private record Member(OrbitCell.Profile profile, int category) {}

  private final Map<String, Member> members;
  private final Map<OrbitCell.Profile, Map<Map<Integer, Integer>, OrbitCell>> cells =
      new HashMap<>();

  private OrbitPartition(Map<String, Member> members) {
    this.members = Map.copyOf(members);
  }

  static OrbitPartition empty() {
    return new OrbitPartition(Map.of());
  }

  static OrbitPartition compile(
      ItemCatalog catalog,
      ItemState root,
      RollPartition rolls,
      BasicCandidateEligibility eligibility) {
    var families = new LinkedHashMap<Set<String>, List<ModifierDefinition>>();
    catalog.modifiers().values().stream()
        .filter(d -> d.layer() == ModifierDefinition.Layer.EXPLICIT)
        .forEach(d -> families.computeIfAbsent(d.familyIds(), ignored -> new ArrayList<>()).add(d));
    var initial = new HashSet<String>();
    root.explicits()
        .forEach(m -> initial.addAll(catalog.modifiers().get(m.modifierId()).familyIds()));
    var groups = new LinkedHashMap<Key, List<OrbitCell.Family>>();
    boolean uniform =
        Set.of(
                "UNVERIFIED_GAME_WEIGHTS_EXPLICIT_UNIFORM_ELIGIBLE_CANDIDATES",
                "USER_APPROVED_UNIFORM_CANDIDATES_NOT_GAME_WEIGHTS")
            .contains(catalog.metadata().weightPolicy());
    for (var entry : families.entrySet()) {
      var ids = entry.getKey();
      if (!Collections.disjoint(ids, initial)
          || entry.getValue().stream()
              .flatMap(d -> d.stats().stream())
              .anyMatch(s -> rolls.relevant(s.id()))
          || families.keySet().stream()
              .anyMatch(other -> !other.equals(ids) && !Collections.disjoint(ids, other))) continue;
      var eligible =
          entry.getValue().stream()
              .filter(d -> d.weight() > 0 && d.requiredItemLevel() <= root.itemLevel())
              .toList();
      if (eligible.isEmpty()
          || eligible.stream().anyMatch(d -> d.stats().size() != 1)
          || eligible.stream().map(ModifierDefinition::affixType).distinct().count() != 1) continue;
      var byCategory = new TreeMap<Integer, List<ModifierDefinition>>();
      eligible.forEach(
          d ->
              byCategory
                  .computeIfAbsent(eligibility.category(d.id()), ignored -> new ArrayList<>())
                  .add(d));
      var weights = new TreeMap<Integer, Long>();
      var categories = new TreeMap<Integer, TierCell>();
      byCategory.forEach(
          (category, definitions) -> {
            long total = definitions.stream().mapToLong(d -> uniform ? 1 : d.weight()).sum();
            weights.put(category, total);
            categories.put(
                category,
                new TierCell(
                    definitions.stream()
                        .map(
                            d -> {
                              var range = d.stats().getFirst();
                              return new TierCell.Variant(
                                  d.id(),
                                  range.id(),
                                  range.min(),
                                  range.max(),
                                  Fraction.of(uniform ? 1 : d.weight(), total));
                            })
                        .toList()));
          });
      var key = new Key(eligible.getFirst().affixType(), Map.copyOf(weights));
      groups
          .computeIfAbsent(key, ignored -> new ArrayList<>())
          .add(new OrbitCell.Family(ids, categories));
    }
    var members = new HashMap<String, Member>();
    var identities = new HashMap<String, OrbitCell.Profile>();
    for (var entry : groups.entrySet()) {
      var sorted =
          entry.getValue().stream()
              .sorted(Comparator.comparing(f -> new TreeSet<>(f.ids()).toString()))
              .toList();
      // A finer partition keeps assignment metadata bounded even in future catalogs.
      for (int start = 0; start < sorted.size(); start += 6) {
        var batch = sorted.subList(start, Math.min(sorted.size(), start + 6));
        if (batch.size() < 2) continue;
        var signature =
            entry.getKey().affix()
                + ":"
                + new TreeMap<>(entry.getKey().weights())
                + ":"
                + batch.stream()
                    .map(f -> new TreeSet<>(f.ids()) + ":" + new TreeMap<>(f.categories()))
                    .toList();
        var profile =
            new OrbitCell.Profile(
                digest(signature), entry.getKey().affix(), batch, entry.getKey().weights());
        var old = identities.putIfAbsent(profile.id(), profile);
        if (old != null && !old.equals(profile))
          throw new IllegalStateException("Orbit identity collision");
        for (var family : batch)
          family
              .categories()
              .forEach(
                  (category, cell) ->
                      cell.variants()
                          .forEach(
                              v -> members.put(v.modifierId(), new Member(profile, category))));
      }
    }
    return new OrbitPartition(members);
  }

  private static String digest(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public void validateSource(DeferredItem source) {
    if (source.orbits().values().stream().anyMatch(OrbitCell::mixed))
      throw new IllegalArgumentException(
          "Expand conditional orbit components before primitive transport");
    var represented = new HashSet<String>();
    source
        .orbits()
        .values()
        .forEach(
            c ->
                c.representatives()
                    .forEach(
                        m -> {
                          var member = members.get(m.modifierId());
                          if (member == null || !member.profile().equals(c.profile()))
                            throw new IllegalArgumentException("Different orbit partition");
                          represented.add(m.modifierId());
                        }));
    if (source.item().explicits().stream()
        .anyMatch(
            m -> members.containsKey(m.modifierId()) && !represented.contains(m.modifierId())))
      throw new IllegalArgumentException(
          "A fixed observed family must not be treated as an anonymous orbit");
  }

  Optional<OrbitCell.Profile> profile(String modifierId) {
    var member = members.get(modifierId);
    return member == null ? Optional.empty() : Optional.of(member.profile());
  }

  public DeferredItem normalize(
      BasicCurrencyState state, Map<String, DeferredItem.Band> rolls, Map<String, TierCell> tiers) {
    var counts = new LinkedHashMap<OrbitCell.Profile, Map<Integer, Integer>>();
    var removed = new HashSet<String>();
    for (var modifier : state.item().explicits()) {
      var member = members.get(modifier.modifierId());
      if (member == null) continue;
      if (modifier.fractured())
        throw new IllegalStateException("Initial fractured family entered an orbit");
      removed.add(modifier.modifierId());
      counts
          .computeIfAbsent(member.profile(), ignored -> new TreeMap<>())
          .merge(member.category(), 1, Integer::sum);
    }
    if (counts.isEmpty()) return new DeferredItem(state, rolls, tiers);
    var orbitLaws = new TreeMap<String, OrbitCell>();
    var explicits =
        new ArrayList<>(
            state.item().explicits().stream()
                .filter(m -> !removed.contains(m.modifierId()))
                .toList());
    counts.forEach(
        (profile, values) -> {
          var key = Map.copyOf(values);
          var cell =
              cells
                  .computeIfAbsent(profile, ignored -> new HashMap<>())
                  .computeIfAbsent(key, ignored -> new OrbitCell(profile, key));
          orbitLaws.put(profile.id(), cell);
          explicits.addAll(cell.representatives());
        });
    var keptRolls = new HashMap<>(rolls);
    keptRolls.keySet().removeAll(removed);
    var keptTiers = new HashMap<>(tiers);
    keptTiers.keySet().removeAll(removed);
    var item = state.item();
    var representative =
        new BasicCurrencyState(
            new ItemState(
                    item.snapshotId(),
                    item.baseItemId(),
                    item.itemLevel(),
                    item.rarity(),
                    item.implicits(),
                    explicits,
                    item.conditions(),
                    item.augmentSockets(),
                    item.catalystQuality())
                .withAugments(item.installedAugments())
                .withQualityInfused(item.qualityInfused()),
            state.provenance());
    return new DeferredItem(representative, keptRolls, keptTiers, orbitLaws);
  }
}
