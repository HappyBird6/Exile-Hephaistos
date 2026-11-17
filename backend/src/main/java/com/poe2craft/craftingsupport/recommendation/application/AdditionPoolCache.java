package com.poe2craft.craftingsupport.recommendation.application;

import com.poe2craft.shared.crafting.domain.AdditionRules;
import com.poe2craft.shared.crafting.domain.CraftingEngine;
import com.poe2craft.shared.crafting.domain.StateBucket;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 목표와 무관한 옵션 계열 점유 상태별 추가 풀을 캐시한다.
 *
 * <p>기존 계약: Goal-independent family occupancy cache. Returned channels are attached to the caller's
 * tiers.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code PROJECTION_VERSION}: 버전; 초기값 {@code "solar-family-pool-v2-omen-composition"} ({@code
 *       String}).
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code rules}: 규칙목록 ({@code AdditionRules}).
 *   <li>{@code validator}: 상태·입력 검증기 ({@code CraftingEngine}).
 *   <li>{@code store}: 저장소 ({@code AdditionPoolStore}).
 *   <li>{@code namespace}: 이름공간 ({@code String}).
 *   <li>{@code memory}: 메모리 ({@code LinkedHashMap<String, Data>}).
 *   <li>{@code maxEntries}: 최대값·항목목록 ({@code int}).
 *   <li>{@code hits}: 도달목록 ({@code AtomicLong}).
 *   <li>{@code computed}: computed ({@code AtomicLong}).
 *   <li>{@code persistedHits}: persisted·도달목록 ({@code AtomicLong}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code AdditionPoolCache(catalog, store, ruleVersion, ledgerDigest, maxEntries)}: 전달받은 값으로
 *       구성원을 초기화한다. 입력: {@code catalog}(카탈로그), {@code store}(저장소), {@code ruleVersion}(규칙·버전),
 *       {@code ledgerDigest}(규칙장부·다이제스트), {@code maxEntries}(최대값·항목목록).
 *   <li>{@code get(state, action, activeOmens)}: 키 또는 위치에 해당하는 값을 조회한다. 입력: {@code state}(상태),
 *       {@code action}(행동), {@code activeOmens}(활성·징조목록). 반환: {@code Data}.
 *   <li>{@code remember(key, data)}: remember 처리에서 {@code memory.put}, {@code memory.size}을(를)
 *       사용한다. 입력: {@code key}(키), {@code data}(데이터).
 *   <li>{@code validateData(data)}: 데이터을(를) 검증한다. 입력: {@code data}(데이터). 사용하는 예외: {@code
 *       IllegalStateException}.
 *   <li>{@code namespace()}: {@code namespace} 값을 조회한다. 반환: {@code String}.
 *   <li>{@code stats()}: 수치목록 값을 조회·구성한다. 반환: {@code Stats}.
 *   <li>{@code hash(value)}: 해시 처리에서 {@code HexFormat.of}, {@code MessageDigest.getInstance}을(를)
 *       사용한다. 입력: {@code value}(값). 반환: {@code String}. 사용하는 예외: {@code IllegalStateException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Stats}: {@code AdditionPoolCache}에서 사용하는 수치목록의 불변 값이다.
 *   <li>{@code Channel}: {@code AdditionPoolCache}에서 사용하는 Channel의 불변 값이다.
 *   <li>{@code Data}: {@code AdditionPoolCache}에서 사용하는 데이터의 불변 값이다.
 * </ul>
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

  /**
   * {@code AdditionPoolCache}에서 사용하는 수치목록의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code memoryHits}: 메모리·도달목록 ({@code long}).
   *   <li>{@code persistedHits}: persisted·도달목록 ({@code long}).
   *   <li>{@code computedPools}: computed·후보풀목록 ({@code long}).
   * </ul>
   */
  public record Stats(long memoryHits, long persistedHits, long computedPools) {}

  /**
   * {@code AdditionPoolCache}에서 사용하는 Channel의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code modifierId}: 실제 부여된 옵션 정의 식별자 ({@code String}).
   *   <li>{@code weight}: 후보 선택에 사용하는 가중치 ({@code long}).
   *   <li>{@code totalWeight}: 합계·가중치 ({@code long}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code probability()}: 현재 조건의 확률을 조회한다. 반환: {@code double}.
   * </ul>
   */
  public record Channel(String modifierId, long weight, long totalWeight) {
    public double probability() {
      return (double) weight / totalWeight;
    }
  }

  /**
   * {@code AdditionPoolCache}에서 사용하는 데이터의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code available}: 사용가능 ({@code boolean}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code rarity}: 현재 아이템 희귀도 ({@code ItemState.Rarity}).
   *   <li>{@code channels}: channels ({@code List<Channel>}).
   *   <li>{@code consumedOmens}: 이번 실행으로 소비된 징조 ID 목록 ({@code List<String>}).
   *   <li>{@code remainingOmens}: 실행 뒤 남은 징조 ID 목록 ({@code List<String>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Data(available, reason, rarity, channels, consumedOmens, remainingOmens)}: 전달받은
   *       값으로 구성원을 초기화한다. 입력: {@code available}(사용가능), {@code reason}(이유), {@code rarity}(희귀도),
   *       {@code channels}(channels), {@code consumedOmens}(소비된·징조목록), {@code
   *       remainingOmens}(남은·징조목록).
   * </ul>
   */
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
