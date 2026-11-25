package com.poe2craft.craftingsupport.goal.domain;

import com.poe2craft.craftingsupport.goal.domain.GoalCatalog.*;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.*;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ModifierDefinition;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * 베이스별 목표 수치와 의사 수치를 색인하고 카탈로그 버전을 계산한다.
 *
 * <p>기존 계약: Catalog membership proves base applicability; level eligibility only applies to new
 * generation.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code bases}: 베이스목록 ({@code Map<String, ItemCatalog>}).
 *   <li>{@code version}: 저장·실행 계약의 버전 ({@code String}).
 *   <li>{@code definitions}: 정의목록 ({@code GoalDefinitions}).
 *   <li>{@code MAX_KNOWN_CONTEXTS}: 사용자 입력 조합 캐시의 상한(16)이다.
 *   <li>{@code knownContexts}: 최근 사용한 문맥의 불변 수치 색인이다. 같은 문맥은 전체 베이스를 다시 순회하지 않는다.
 *   <li>{@code directStats}: 직접·수치목록 ({@code Map<String, GoalDefinitions.Direct>}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code GoalCatalogIndex(catalogs, definitions)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
 *       catalogs}(카탈로그목록), {@code definitions}(정의목록).
 *   <li>{@code GoalCatalogIndex(catalogs, definitions, rulesetIdentity)}: 전달받은 값으로 구성원을 초기화한다. 입력:
 *       {@code catalogs}(카탈로그목록), {@code definitions}(정의목록), {@code rulesetIdentity}(규칙묶음·정체성).
 *       사용하는 예외: {@code IllegalArgumentException}, {@code IllegalStateException}.
 *   <li>{@code version()}: 현재 데이터·계산 계약의 버전을 조회한다. 반환: {@code String}.
 *   <li>{@code itemCatalog(context)}: 아이템·카탈로그 처리에서 {@code bases.get}, {@code
 *       context.baseItemId}을(를) 사용한다. 입력: {@code context}(문맥). 반환: {@code Optional<ItemCatalog>}.
 *   <li>{@code catalog(context)}: 카탈로그 처리에서 {@code issues.add}, {@code Issue.warning}을(를) 사용한다. 입력:
 *       {@code context}(문맥). 반환: {@code GoalCatalog}.
 *   <li>{@code buildKnownStats(context)}: 전체 베이스의 수치를 모으고 선택 베이스의 적격 정보를 덮어써 불변 색인을 만든다.
 *   <li>{@code knownStats(context)}: 한도 있는 캐시를 확인하고 없는 문맥만 계산한다. 알려진·수치목록 처리에서 {@code
 *       bases.values}, {@code context.itemLevel}을(를) 사용한다. 입력: {@code context}(문맥). 반환: {@code
 *       Map<String, Stat>}.
 *   <li>{@code id(layer, source)}: 식별자 처리에서 {@code layer.toLowerCase}을(를) 사용한다. 입력: {@code
 *       layer}(옵션계층), {@code source}(출처). 반환: {@code String}.
 *   <li>{@code mapping(stat)}: 매핑 처리에서 {@code stat.kind}, {@code stat.support}을(를) 사용한다. 입력: {@code
 *       stat}(수치). 반환: {@code Optional<Mapping>}.
 *   <li>{@code reviewedAmuletEssenceTarget(modifier)}: 검토된·목걸이·에센스·대상 처리에서 {@code
 *       PERFECT_ESSENCE_ENHANCEMENT.replacementEssenceModifiers}, {@code modifier.id}을(를) 사용한다. 입력:
 *       {@code modifier}(옵션). 반환: {@code boolean}.
 *   <li>{@code stats(catalog, level)}: 수치목록 처리에서 {@code catalog.modifiers}, {@code
 *       modifier.stats}을(를) 사용한다. 입력: {@code catalog}(카탈로그), {@code level}(레벨). 반환: {@code
 *       List<Stat>}.
 *   <li>{@code addPseudo(result, name, label, unit, raw)}: 의사수치을(를) 추가한다. 입력: {@code result}(결과),
 *       {@code name}(이름), {@code label}(표시이름), {@code unit}(단위), {@code raw}(원문).
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Signature}: append의 연결 순서를 유지해 과거 버전 해시를 재현하되 전체 카탈로그 문자열은 보관하지 않는다.
 * </ul>
 */
public final class GoalCatalogIndex {
  private static final int MAX_KNOWN_CONTEXTS = 16;
  private final Map<Context, Map<String, Stat>> knownContexts =
      new LinkedHashMap<>(16, 0.75f, true);
  private final Map<String, ItemCatalog> bases;
  private final String version;
  private final GoalDefinitions definitions;
  private final Map<String, GoalDefinitions.Direct> directStats;

  public GoalCatalogIndex(List<ItemCatalog> catalogs, GoalDefinitions definitions) {
    this(catalogs, definitions, null);
  }

  public GoalCatalogIndex(
      List<ItemCatalog> catalogs, GoalDefinitions definitions, String rulesetIdentity) {
    this.definitions = Objects.requireNonNull(definitions);
    directStats = definitions.directStats();
    var indexed = new TreeMap<String, ItemCatalog>();
    for (var catalog : catalogs) {
      if (indexed.put(catalog.base().id(), catalog) != null)
        throw new IllegalArgumentException("Duplicate base catalog");
    }
    bases = Collections.unmodifiableMap(indexed);
    var signature = new Signature().append(definitions.ruleVersion());
    if (rulesetIdentity != null
        && !rulesetIdentity.equals(
            "reviewed-equipment-20261009-v1-81b47766ab181f81a31f8fa1452c6ce99074a00e0b5609c4b36279d79724fae8"))
      signature.append(rulesetIdentity);
    // Retain deployed goal versions for the exact pre-refactor definitions; any data edit
    // automatically invalidates that compatibility boundary, even without a manual version bump.
    try {
      var digest =
          HexFormat.of()
              .formatHex(
                  MessageDigest.getInstance("SHA-256")
                      .digest(definitions.signature().getBytes(StandardCharsets.UTF_8)));
      if (!digest.equals("7c7b34693b448cbe7f8921ef38faa9a4c077e2456bbe59843367cd5acd1d3cfa"))
        signature.append(digest);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
    indexed
        .values()
        .forEach(
            c -> {
              signature.append(c.metadata()).append(c.base());
              c.modifiers().values().stream()
                  .sorted(Comparator.comparing(ModifierDefinition::id))
                  .forEach(
                      d -> {
                        signature
                            .append(d.id())
                            .append(d.name())
                            .append(d.layer())
                            .append(d.affixType())
                            .append(d.familyIds().stream().sorted().toList())
                            .append(d.requiredItemLevel())
                            .append(d.weight())
                            .append(d.tier())
                            .append(d.text())
                            .append(d.stats())
                            .append(d.tags().stream().sorted().toList())
                            .append(d.sourceUrl());
                      });
            });
    version = "hephaistos-goal-v1-" + signature.finish();
  }

  public String version() {
    return version;
  }

  public Optional<ItemCatalog> itemCatalog(Context context) {
    var catalog = bases.get(context.baseItemId());
    return catalog != null && catalog.metadata().snapshotId().equals(context.snapshotId())
        ? Optional.of(catalog)
        : Optional.empty();
  }

  public GoalCatalog catalog(Context context) {
    var itemCatalog = itemCatalog(context).orElse(null);
    var issues = new ArrayList<Issue>();
    if (itemCatalog == null)
      issues.add(
          Issue.warning(
              bases.containsKey(context.baseItemId()) ? "UNSUPPORTED_SNAPSHOT" : "UNSUPPORTED_BASE",
              "/context",
              "No reviewed catalog exists for this context."));
    boolean numeric = NumericAdditionKernel.supports(itemCatalog);
    var groups =
        Arrays.stream(Type.values())
            .map(
                t ->
                    new GroupSupport(
                        t,
                        Capability.SUPPORTED,
                        numeric ? Capability.SUPPORTED : Capability.UNSUPPORTED,
                        numeric ? null : "NUMERIC_BASE_NOT_IMPLEMENTED"))
            .toList();
    return new GoalCatalog(
        1,
        version,
        context,
        groups,
        itemCatalog == null ? List.of() : stats(itemCatalog, context.itemLevel()),
        List.copyOf(issues),
        itemCatalog == null ? Map.of() : itemCatalog.modifiers());
  }

  /** Retains known IDs across base changes so validation can diagnose ineligibility. */
  public Map<String, Stat> knownStats(Context context) {
    synchronized (knownContexts) {
      var cached = knownContexts.get(context);
      if (cached != null) return cached;
    }
    // 큰 색인 계산 중 다른 문맥의 캐시 조회를 막지 않는다. 공개 직전에 중복 계산을 확인한다.
    var computed = buildKnownStats(context);
    synchronized (knownContexts) {
      var cached = knownContexts.get(context);
      if (cached != null) return cached;
      knownContexts.put(context, computed);
      if (knownContexts.size() > MAX_KNOWN_CONTEXTS)
        knownContexts.remove(knownContexts.keySet().iterator().next());
      return computed;
    }
  }

  private Map<String, Stat> buildKnownStats(Context context) {
    var known = new TreeMap<String, Stat>();
    for (var base : bases.values())
      for (var stat : stats(base, context.itemLevel()))
        known.putIfAbsent(
            stat.statId(),
            new Stat(
                stat.statId(),
                stat.label(),
                stat.unit(),
                stat.kind(),
                stat.support(),
                false,
                "STAT_NOT_ON_SELECTED_BASE",
                stat.sourceStatIds(),
                stat.contributions(),
                stat.sourceUrls(),
                stat.sourceDivisor()));
    catalog(context).stats().forEach(s -> known.put(s.statId(), s));
    return Collections.unmodifiableMap(known);
  }

  public static String id(String layer, String source) {
    return "hephaistos:v1:" + layer.toLowerCase(Locale.ROOT) + ":" + source;
  }

  public static Optional<Mapping> mapping(Stat stat) {
    if (stat.kind() == Kind.PSEUDO || stat.support().evaluation() != Capability.SUPPORTED)
      return Optional.empty();
    return Optional.of(
        new Mapping(
            stat.sourceStatIds().getFirst(),
            stat.kind().name(),
            1,
            stat.sourceDivisor() == null ? 1 : stat.sourceDivisor()));
  }

  /**
   * Fixed essence targets are selectable without entering the ordinary weighted roll pool. IDs must
   * also occur in the selected base catalog; item-level eligibility remains enforced.
   */
  private static boolean reviewedAmuletEssenceTarget(ModifierDefinition modifier) {
    return WorkbenchCurrency.PERFECT_ESSENCE_ENHANCEMENT
            .replacementEssenceModifiers()
            .contains(modifier.id())
        || WorkbenchCurrency.PERFECT_ESSENCE_INFINITE
            .replacementEssenceModifiers()
            .contains(modifier.id());
  }

  private List<Stat> stats(ItemCatalog catalog, int level) {
    var definitions = new TreeMap<String, List<ModifierDefinition>>();
    for (var modifier : catalog.modifiers().values())
      for (var stat : modifier.stats())
        definitions
            .computeIfAbsent(
                id(modifier.layer().name(), modifier.goalSource(stat)), key -> new ArrayList<>())
            .add(modifier);
    var result = new TreeMap<String, Stat>();
    definitions.forEach(
        (key, modifiers) -> {
          var first =
              modifiers.stream()
                  .min(
                      Comparator.comparingInt(ModifierDefinition::tier)
                          .thenComparing(ModifierDefinition::id))
                  .orElseThrow();
          var raw =
              first.stats().stream()
                  .map(first::goalSource)
                  .filter(s -> id(first.layer().name(), s).equals(key))
                  .findFirst()
                  .orElseThrow();
          if (first.tags().contains("otherworldly")) {
            boolean eligible = first.requiredItemLevel() <= level;
            boolean presence = first.stats().getFirst().id().equals("display_reveal_present");
            result.put(
                key,
                new Stat(
                    key,
                    first.text(),
                    presence ? "presence" : "roll",
                    Kind.EXPLICIT,
                    new Support(
                        Capability.SUPPORTED,
                        Capability.UNSUPPORTED,
                        "NUMERIC_DISTRIBUTION_NOT_IMPLEMENTED"),
                    eligible,
                    eligible ? null : "NO_ELIGIBLE_GENERATION_MODIFIER",
                    List.of(raw),
                    List.of(),
                    List.of(first.sourceUrl())));
            return;
          }
          var direct = directStats.get(raw);
          boolean supported = direct != null;
          boolean eligible =
              modifiers.stream()
                  .anyMatch(
                      d ->
                          d.layer() == ModifierDefinition.Layer.IMPLICIT
                              || (d.requiredItemLevel() <= level
                                  && (d.weight() > 0 || reviewedAmuletEssenceTarget(d))));
          result.put(
              key,
              new Stat(
                  key,
                  supported ? direct.label() : first.text(),
                  supported ? direct.unit() : "source",
                  Kind.valueOf(first.layer().name()),
                  new Support(
                      supported ? Capability.SUPPORTED : Capability.UNSUPPORTED,
                      Capability.UNSUPPORTED,
                      supported
                          ? "NUMERIC_DISTRIBUTION_NOT_IMPLEMENTED"
                          : "UNIT_OR_EFFECT_NOT_REVIEWED"),
                  eligible,
                  eligible ? null : "NO_ELIGIBLE_GENERATION_MODIFIER",
                  List.of(raw),
                  List.of(),
                  modifiers.stream()
                      .map(ModifierDefinition::sourceUrl)
                      .distinct()
                      .sorted()
                      .toList(),
                  supported ? direct.sourceDivisor() : null));
        });
    for (var pseudo : this.definitions.pseudos())
      addPseudo(result, pseudo.id(), pseudo.label(), pseudo.unit(), pseudo.sourceStatIds());
    if (NumericAdditionKernel.supports(catalog)) {
      result.replaceAll(
          (key, stat) ->
              stat.support().evaluation() == Capability.SUPPORTED
                      && !stat.unit().equals("presence")
                      && !stat.unit().equals("roll")
                      && stat.sourceStatIds().stream()
                          .noneMatch(
                              raw ->
                                  directStats.containsKey(raw)
                                      && Boolean.TRUE.equals(directStats.get(raw).evaluationOnly()))
                  ? new Stat(
                      stat.statId(),
                      stat.label(),
                      stat.unit(),
                      stat.kind(),
                      new Support(Capability.SUPPORTED, Capability.SUPPORTED, null),
                      stat.eligible(),
                      stat.eligibilityReason(),
                      stat.sourceStatIds(),
                      stat.contributions(),
                      stat.sourceUrls(),
                      stat.sourceDivisor())
                  : stat);
    }
    return List.copyOf(result.values());
  }

  private static void addPseudo(
      Map<String, Stat> result, String name, String label, String unit, List<String> raw) {
    // Only direct layer-scoped sources are allowed; pseudo-to-pseudo edges cannot double count.
    var sources =
        result.values().stream()
            .filter(s -> s.kind() != Kind.PSEUDO && raw.contains(s.sourceStatIds().getFirst()))
            .toList();
    if (sources.isEmpty()) return;
    boolean eligible = sources.stream().anyMatch(Stat::eligible);
    var contributions =
        sources.stream().map(s -> new Contribution(s.statId(), BigDecimal.ONE)).toList();
    result.put(
        id("pseudo", name),
        new Stat(
            id("pseudo", name),
            label,
            unit,
            Kind.PSEUDO,
            new Support(
                Capability.SUPPORTED,
                Capability.UNSUPPORTED,
                "NUMERIC_DISTRIBUTION_NOT_IMPLEMENTED"),
            eligible,
            eligible ? null : "NO_ELIGIBLE_GENERATION_MODIFIER",
            sources.stream().map(Stat::statId).toList(),
            contributions,
            sources.stream().flatMap(s -> s.sourceUrls().stream()).distinct().sorted().toList()));
  }

  /**
   * append의 연결 순서를 유지해 과거 버전 해시를 재현하되 전체 카탈로그 문자열은 보관하지 않는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code digest}: 다이제스트 ({@code MessageDigest}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Signature()}: 전달받은 값으로 구성원을 초기화한다. 사용하는 예외: {@code IllegalStateException}.
   *   <li>{@code append(part)}: 뒤에 값이나 계산 행을 추가한다. 입력: {@code part}(part). 반환: {@code Signature}.
   *   <li>{@code finish()}: 진행 중인 계산 또는 누적 처리를 마무리한다. 반환: {@code String}.
   * </ul>
   */
  private static final class Signature {
    private final MessageDigest digest;

    Signature() {
      try {
        digest = MessageDigest.getInstance("SHA-256");
      } catch (NoSuchAlgorithmException error) {
        throw new IllegalStateException(error);
      }
    }

    Signature append(Object part) {
      digest.update(String.valueOf(part).getBytes(StandardCharsets.UTF_8));
      return this;
    }

    String finish() {
      return HexFormat.of().formatHex(digest.digest());
    }
  }
}
