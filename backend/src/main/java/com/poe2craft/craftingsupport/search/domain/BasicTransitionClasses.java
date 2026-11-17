package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.craftingsupport.goal.domain.GoalEvaluator;
import com.poe2craft.craftingsupport.goal.domain.GoalFilter.Status;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.crafting.domain.BasicCurrencyTransitions;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;
import java.util.function.Function;

/**
 * 기본 행동에서 같은 전이를 갖는 상태를 구분하고 구체 입력 분포를 보존한다.
 *
 * <p>기존 계약: Strong basic-action equivalence with separate exact-input transport. A class is not an
 * item distribution: replay always takes the actual incoming item, never an averaged
 * representative.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "indexed-basic-action-equivalence-with-input-transport-v2"}
 *       ({@code String}).
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code primitive}: 개별행동 ({@code BasicCurrencyTransitions}).
 *   <li>{@code partition}: 분할 ({@code RollPartition}).
 *   <li>{@code evaluate}: 평가 ({@code Function<ItemState, GoalEvaluator.Evaluation>}).
 *   <li>{@code exactFamilies}: 정확값·옵션계열목록 ({@code Map<String, String>}).
 *   <li>{@code classFamilies}: 동치류·옵션계열목록 ({@code Map<String, String>}).
 *   <li>{@code additionProfiles}: 추가·특성목록 ({@code LinkedHashMap<ProfileKey,
 *       List<AdditionProfile>>}).
 *   <li>{@code profileLimit}: 특성·한도 ({@code int}).
 *   <li>{@code profileHits}: 특성·도달목록 ({@code long}).
 *   <li>{@code profileMisses}: 특성·캐시실패수 ({@code long}).
 *   <li>{@code profileRows}: 특성·행목록 ({@code int}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code profileRetainedRows()}: {@code profileRows} 값을 조회한다. 반환: {@code int}.
 *   <li>{@code profileMetrics()}: 특성·측정값 값을 조회·구성한다. 반환: {@code ProfileMetrics}.
 *   <li>{@code BasicTransitionClasses(catalog, primitive, partition, evaluate)}: 기본 정책을 보충해 공통 생성자로
 *       위임한다. 입력: {@code catalog}(카탈로그), {@code primitive}(개별행동), {@code partition}(분할), {@code
 *       evaluate}(평가).
 *   <li>{@code BasicTransitionClasses(catalog, primitive, partition, evaluate, profileLimit)}: 전달받은
 *       값으로 구성원을 초기화한다. 입력: {@code catalog}(카탈로그), {@code primitive}(개별행동), {@code partition}(분할),
 *       {@code evaluate}(평가), {@code profileLimit}(특성·한도). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code key(state)}: 키 처리에서 {@code state.item}, {@code item.explicits}을(를) 사용한다. 입력: {@code
 *       state}(상태). 반환: {@code Key}.
 *   <li>{@code retainedRole(modifier)}: 보유·역할 처리에서 {@code catalog.modifiers}, {@code
 *       modifier.modifierId}을(를) 사용한다. 입력: {@code modifier}(옵션). 반환: {@code Role}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code classRole(modifier)}: 동치류·역할 처리에서 {@code catalog.modifiers}, {@code
 *       modifier.modifierId}을(를) 사용한다. 입력: {@code modifier}(옵션). 반환: {@code Role}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code raw(source, action)}: 원문 처리에서 {@code primitive.primitiveCursor}, {@code
 *       cursor.blocked}을(를) 사용한다. 입력: {@code source}(출처), {@code action}(행동). 반환: {@code
 *       BasicCurrencyTransitions.PrimitiveCursor}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code cursor(source, action)}: 진행커서의 입력에 따른 분기·결과를 처리한다. 입력: {@code source}(출처), {@code
 *       action}(행동). 반환: {@code Cursor}.
 *   <li>{@code replay(source, actualInput, action)}: replay 처리에서 {@code source.equals}을(를) 사용한다.
 *       입력: {@code source}(출처), {@code actualInput}(실제값·입력), {@code action}(행동). 반환: {@code
 *       BasicCurrencyTransitions.PrimitiveCursor}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code indexedCursor(source, action)}: 색인된·진행커서의 입력에 따른 분기·결과를 처리한다. 입력: {@code
 *       source}(출처), {@code action}(행동). 반환: {@code IndexedCursor}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Role}: {@code BasicTransitionClasses}에서 사용하는 역할의 불변 값이다.
 *   <li>{@code Key}: {@code BasicTransitionClasses}에서 사용하는 키의 불변 값이다.
 *   <li>{@code ProfileKey}: {@code BasicTransitionClasses}에서 사용하는 특성·키의 불변 값이다.
 *   <li>{@code AdditionProfile}: {@code BasicTransitionClasses}에서 사용하는 추가·특성의 불변 값이다.
 *   <li>{@code ProfileMetrics}: {@code BasicTransitionClasses}에서 사용하는 특성·측정값의 불변 값이다.
 *   <li>{@code Kernel}: {@code BasicTransitionClasses} 내부의 전이커널 처리를 맡는다.
 *   <li>{@code IndexedCursor}: {@code BasicTransitionClasses} 내부의 색인된·진행커서 처리를 맡는다.
 *   <li>{@code Cursor}: {@code BasicTransitionClasses} 내부의 진행커서 처리를 맡는다.
 * </ul>
 */
public final class BasicTransitionClasses {
  public static final String VERSION = "indexed-basic-action-equivalence-with-input-transport-v2";

  /**
   * {@code BasicTransitionClasses}에서 사용하는 역할의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code family}: 옵션계열 ({@code String}).
   *   <li>{@code region}: 영역 ({@code Object}).
   *   <li>{@code fractured}: 분열고정 ({@code boolean}).
   * </ul>
   */
  public record Role(String family, Object region, boolean fractured) {}

  /**
   * {@code BasicTransitionClasses}에서 사용하는 키의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code context}: 문맥 ({@code BasicCurrencyState}).
   *   <li>{@code modifiers}: 옵션목록 ({@code List<Role>}).
   *   <li>{@code observations}: 관측값목록 ({@code List<Status>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Key(context, modifiers, observations)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       context}(문맥), {@code modifiers}(옵션목록), {@code observations}(관측값목록).
   * </ul>
   */
  public record Key(BasicCurrencyState context, List<Role> modifiers, List<Status> observations) {
    public Key {
      modifiers = List.copyOf(modifiers);
      observations = List.copyOf(observations);
    }
  }

  private final ItemCatalog catalog;
  private final BasicCurrencyTransitions primitive;
  private final RollPartition partition;
  private final Function<ItemState, GoalEvaluator.Evaluation> evaluate;
  private final Map<String, String> exactFamilies = new HashMap<>();
  private final Map<String, String> classFamilies = new HashMap<>();

  /**
   * {@code BasicTransitionClasses}에서 사용하는 특성·키의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code candidates}: 후보목록 ({@code List<String>}).
   *   <li>{@code removalBranches}: 제거·분기목록 ({@code int}).
   * </ul>
   */
  private record ProfileKey(List<String> candidates, int removalBranches) {}

  /**
   * {@code BasicTransitionClasses}에서 사용하는 추가·특성의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code candidate}: 후보 ({@code int}).
   *   <li>{@code value}: 값 ({@code long}).
   *   <li>{@code mass}: 확률질량 ({@code Fraction}).
   * </ul>
   */
  private record AdditionProfile(int candidate, long value, Fraction mass) {}

  private final LinkedHashMap<ProfileKey, List<AdditionProfile>> additionProfiles =
      new LinkedHashMap<>(16, .75f, true);
  private final int profileLimit;
  private long profileHits, profileMisses;
  private int profileRows;

  /**
   * {@code BasicTransitionClasses}에서 사용하는 특성·측정값의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code profiles}: 특성목록 ({@code int}).
   *   <li>{@code retainedRows}: 보유·행목록 ({@code int}).
   *   <li>{@code hits}: 도달목록 ({@code long}).
   *   <li>{@code misses}: 캐시실패수 ({@code long}).
   * </ul>
   */
  public record ProfileMetrics(int profiles, int retainedRows, long hits, long misses) {}

  public int profileRetainedRows() {
    return profileRows;
  }

  public ProfileMetrics profileMetrics() {
    return new ProfileMetrics(additionProfiles.size(), profileRows, profileHits, profileMisses);
  }

  public BasicTransitionClasses(
      ItemCatalog catalog,
      BasicCurrencyTransitions primitive,
      RollPartition partition,
      Function<ItemState, GoalEvaluator.Evaluation> evaluate) {
    this(catalog, primitive, partition, evaluate, 128);
  }

  public BasicTransitionClasses(
      ItemCatalog catalog,
      BasicCurrencyTransitions primitive,
      RollPartition partition,
      Function<ItemState, GoalEvaluator.Evaluation> evaluate,
      int profileLimit) {
    if (profileLimit < 0) throw new IllegalArgumentException("Negative profile cache limit");
    this.profileLimit = profileLimit;
    this.catalog = catalog;
    this.primitive = primitive;
    this.partition = partition;
    this.evaluate = evaluate;
    catalog
        .modifiers()
        .values()
        .forEach(
            definition -> {
              var exact = definition.affixType() + ":" + new TreeSet<>(definition.familyIds());
              exactFamilies.put(definition.id(), exact);
              classFamilies.put(
                  definition.id(),
                  partition
                      .orbits()
                      .profile(definition.id())
                      .map(profile -> definition.affixType() + ":orbit:" + profile.id())
                      .orElse(exact));
            });
  }

  public Key key(BasicCurrencyState state) {
    var item = state.item();
    var roles = new ArrayList<Role>();
    for (var modifier : item.explicits()) {
      roles.add(classRole(modifier));
    }
    roles.sort(
        Comparator.comparing(Role::family)
            .thenComparing(r -> r.region().toString())
            .thenComparing(Role::fractured));
    var context =
        new BasicCurrencyState(
            new ItemState(
                    item.snapshotId(),
                    item.baseItemId(),
                    item.itemLevel(),
                    item.rarity(),
                    item.implicits(),
                    List.of(),
                    item.conditions(),
                    item.augmentSockets(),
                    item.catalystQuality())
                .withAugments(item.installedAugments())
                .withQualityInfused(item.qualityInfused()),
            state.provenance());
    var observation = evaluate.apply(item);
    var statuses = new ArrayList<Status>();
    statuses.add(observation.status());
    statuses.add(observation.generalStatus());
    observation
        .groups()
        .forEach(
            g -> {
              statuses.add(g.status());
              g.entries().forEach(e -> statuses.add(e.status()));
            });
    return new Key(context, roles, statuses);
  }

  /** Existing numeric laws may mix only within this exact family and observation region. */
  public Role retainedRole(ModifierInstance modifier) {
    var definition = catalog.modifiers().get(modifier.modifierId());
    if (definition == null) throw new IllegalArgumentException("Unknown modifier");
    return new Role(
        exactFamilies.get(definition.id()),
        partition.transitionRegion(definition, modifier),
        modifier.fractured());
  }

  private Role classRole(ModifierInstance modifier) {
    var definition = catalog.modifiers().get(modifier.modifierId());
    if (definition == null) throw new IllegalArgumentException("Unknown modifier");
    return new Role(
        (modifier.fractured() ? exactFamilies : classFamilies).get(definition.id()),
        partition.transitionRegion(definition, modifier),
        modifier.fractured());
  }

  private BasicCurrencyTransitions.PrimitiveCursor raw(
      BasicCurrencyState source, WorkbenchCurrency action) {
    var cursor = primitive.primitiveCursor(source, action, Set.of());
    if (!cursor.blocked() && !partition.allows(action))
      throw new IllegalArgumentException("Action outside proven partition scope");
    return cursor;
  }

  public Cursor cursor(BasicCurrencyState source, WorkbenchCurrency action) {
    return new Cursor(source, action);
  }

  public BasicCurrencyTransitions.PrimitiveCursor replay(
      Key source, BasicCurrencyState actualInput, WorkbenchCurrency action) {
    if (!source.equals(key(actualInput)))
      throw new IllegalArgumentException("Different input class");
    return raw(actualInput, action);
  }

  /**
   * {@code BasicTransitionClasses} 내부의 전이커널 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code source}: 출처 ({@code Key}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code WorkbenchCurrency}).
   *   <li>{@code outcomes}: 결과목록 ({@code Map<Key, Fraction>}).
   *   <li>{@code witnesses}: 증명상태목록 ({@code Map<Key, BasicCurrencyState>}).
   *   <li>{@code unresolved}: 미해결 ({@code Fraction}).
   *   <li>{@code unavailable}: 사용불가 ({@code boolean}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Kernel(cursor)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code cursor}(진행커서).
   *   <li>{@code Kernel(source, action, outcomes, witnesses, status)}: 전달받은 값으로 구성원을 초기화한다. 입력:
   *       {@code source}(출처), {@code action}(행동), {@code outcomes}(결과목록), {@code
   *       witnesses}(증명상태목록), {@code status}(상태). 사용하는 예외: {@code IllegalStateException}.
   *   <li>{@code outcomes()}: 가능한 결과와 확률을 조회한다. 반환: {@code Map<Key, Fraction>}.
   *   <li>{@code witnesses()}: {@code witnesses} 값을 조회한다. 반환: {@code Map<Key, BasicCurrencyState>}.
   *   <li>{@code unresolved()}: {@code unresolved} 값을 조회한다. 반환: {@code Fraction}.
   *   <li>{@code unavailable()}: {@code unavailable} 값을 조회한다. 반환: {@code boolean}.
   *   <li>{@code replay(actualInput)}: replay 처리에서 {@code this.replay}을(를) 사용한다. 입력: {@code
   *       actualInput}(실제값·입력). 반환: {@code BasicCurrencyTransitions.PrimitiveCursor}.
   * </ul>
   */
  public final class Kernel {
    private final Key source;
    private final WorkbenchCurrency action;
    private final Map<Key, Fraction> outcomes;
    private final Map<Key, BasicCurrencyState> witnesses;
    private final Fraction unresolved;
    private final boolean unavailable;

    private Kernel(Cursor cursor) {
      this(cursor.source, cursor.action, cursor.outcomes, cursor.witnesses, cursor.raw.status());
    }

    private Kernel(
        Key source,
        WorkbenchCurrency action,
        Map<Key, Fraction> outcomes,
        Map<Key, BasicCurrencyState> witnesses,
        BasicCurrencyTransitions.Status status) {
      this.source = source;
      this.action = action;
      this.outcomes = Map.copyOf(outcomes);
      this.witnesses = Map.copyOf(witnesses);
      unavailable = status == BasicCurrencyTransitions.Status.UNAVAILABLE;
      unresolved =
          status == BasicCurrencyTransitions.Status.UNSUPPORTED ? Fraction.ONE : Fraction.ZERO;
      var mass = outcomes.values().stream().reduce(unresolved, Fraction::add);
      if (!(unavailable ? mass.equals(Fraction.ZERO) : mass.equals(Fraction.ONE)))
        throw new IllegalStateException("Class kernel lost mass");
    }

    public Map<Key, Fraction> outcomes() {
      return outcomes;
    }

    public Map<Key, BasicCurrencyState> witnesses() {
      return witnesses;
    }

    public Fraction unresolved() {
      return unresolved;
    }

    public boolean unavailable() {
      return unavailable;
    }

    /** Exact concrete transport for one actual member; distinct entry laws remain distinct. */
    public BasicCurrencyTransitions.PrimitiveCursor replay(BasicCurrencyState actualInput) {
      return BasicTransitionClasses.this.replay(source, actualInput, action);
    }
  }

  public IndexedCursor indexedCursor(BasicCurrencyState source, WorkbenchCurrency action) {
    return new IndexedCursor(source, action);
  }

  /**
   * {@code BasicTransitionClasses} 내부의 색인된·진행커서 처리를 맡는다.
   *
   * <p>기존 계약: Aggregate equivalent generated roles before constructing complete item witnesses.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code source}: 출처 ({@code Key}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code WorkbenchCurrency}).
   *   <li>{@code raw}: 원문 ({@code BasicCurrencyTransitions.PrimitiveCursor}).
   *   <li>{@code branches}: 분기목록 ({@code List<BasicCurrencyTransitions.KernelBranch>}).
   *   <li>{@code outcomes}: 결과목록 ({@code Map<Key, Fraction>}).
   *   <li>{@code witnesses}: 증명상태목록 ({@code Map<Key, BasicCurrencyState>}).
   *   <li>{@code additions}: additions ({@code Map<Role, Addition>}).
   *   <li>{@code flush}: 기록반영 ({@code Iterator<AdditionProfile>}).
   *   <li>{@code profileKey}: 특성·키 ({@code ProfileKey}).
   *   <li>{@code branch}: 분기 ({@code int}).
   *   <li>{@code candidate}: 후보 ({@code int}).
   *   <li>{@code value}: 값 ({@code Long}).
   *   <li>{@code materializedWitnesses}: materialized·증명상태목록 ({@code long}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code assumptions()}: 계산가정 값을 조회·구성한다. 반환: {@code List<WorkbenchSimulator.Assumption>}.
   *   <li>{@code IndexedCursor(state, action)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code state}(상태), {@code
   *       action}(행동).
   *   <li>{@code complete()}: 완료 값을 조회·구성한다. 반환: {@code boolean}.
   *   <li>{@code materializedWitnesses()}: {@code materializedWitnesses} 값을 조회한다. 반환: {@code long}.
   *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다.
   *   <li>{@code accept(witness, mass)}: 수락 처리에서 {@code outcomes.merge}, {@code
   *       witnesses.putIfAbsent}을(를) 사용한다. 입력: {@code witness}(증명상태), {@code mass}(확률질량).
   *   <li>{@code result()}: 최종 또는 현재 계산 결과를 구성한다. 반환: {@code Kernel}. 사용하는 예외: {@code
   *       IllegalStateException}.
   * </ul>
   *
   * <p>중첩 타입:
   *
   * <ul>
   *   <li>{@code Addition}: {@code IndexedCursor} 내부의 추가 처리를 맡는다.
   * </ul>
   */
  public final class IndexedCursor {
    private final Key source;
    private final WorkbenchCurrency action;
    private final BasicCurrencyTransitions.PrimitiveCursor raw;
    private final List<BasicCurrencyTransitions.KernelBranch> branches;
    private final Map<Key, Fraction> outcomes = new LinkedHashMap<>();
    private final Map<Key, BasicCurrencyState> witnesses = new LinkedHashMap<>();
    private final Map<Role, Addition> additions = new LinkedHashMap<>();
    private Iterator<AdditionProfile> flush;
    private ProfileKey profileKey;
    private int branch, candidate;
    private Long value;
    private long materializedWitnesses;

    /**
     * {@code IndexedCursor} 내부의 추가 처리를 맡는다.
     *
     * <p>필드·상수:
     *
     * <ul>
     *   <li>{@code candidate}: 후보 ({@code int}).
     *   <li>{@code value}: 값 ({@code long}).
     *   <li>{@code mass}: 확률질량 ({@code Fraction}).
     * </ul>
     *
     * <p>생성자·메서드:
     *
     * <ul>
     *   <li>{@code Addition(candidate, value, mass)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
     *       candidate}(후보), {@code value}(값), {@code mass}(확률질량).
     * </ul>
     */
    private final class Addition {
      final int candidate;
      final long value;
      Fraction mass;

      Addition(int candidate, long value, Fraction mass) {
        this.candidate = candidate;
        this.value = value;
        this.mass = mass;
      }
    }

    public List<WorkbenchSimulator.Assumption> assumptions() {
      return raw.assumptions();
    }

    IndexedCursor(BasicCurrencyState state, WorkbenchCurrency action) {
      source = key(state);
      this.action = action;
      raw = raw(state, action);
      branches = raw.blocked() ? List.of() : raw.identity().orElseThrow().branches();
    }

    public boolean complete() {
      return raw.blocked() || branch == branches.size();
    }

    public long materializedWitnesses() {
      return materializedWitnesses;
    }

    public void step() {
      if (complete()) return;
      if (action == WorkbenchCurrency.ANNULMENT) {
        accept(
            new BasicCurrencyState(branches.get(branch).remaining(), primitive.provenance()),
            Fraction.of(1, branches.size()));
        branch++;
        return;
      }
      if (flush != null) {
        if (flush.hasNext()) {
          var addition = flush.next();
          accept(
              raw.candidateWitness(branch, addition.candidate(), addition.value()),
              addition.mass());
        }
        if (!flush.hasNext()) {
          branch++;
          candidate = 0;
          value = null;
          additions.clear();
          flush = null;
          profileKey = null;
        }
        return;
      }
      var candidates = branches.get(branch).candidates();
      if (profileKey == null) {
        profileKey =
            new ProfileKey(
                candidates.stream().map(ModifierDefinition::id).toList(), branches.size());
        var cached = additionProfiles.get(profileKey);
        if (cached != null) {
          profileHits++;
          flush = cached.iterator();
          return;
        }
        profileMisses++;
      }
      var definition = candidates.get(candidate);
      var range = definition.stats().getFirst();
      if (value == null) value = range.min();
      long last = partition.end(definition, value);
      var role = classRole(new ModifierInstance(definition.id(), Map.of(range.id(), value)));
      var mass = raw.candidateBandProbability(branch, candidate, value, last);
      var previous = additions.get(role);
      if (previous == null) additions.put(role, new Addition(candidate, value, mass));
      else previous.mass = previous.mass.add(mass);
      if (last == range.max()) {
        candidate++;
        value = null;
      } else value = last + 1;
      if (candidate == candidates.size()) {
        var profile =
            additions.values().stream()
                .map(a -> new AdditionProfile(a.candidate, a.value, a.mass))
                .toList();
        if (profileLimit > 0) {
          var previousProfile = additionProfiles.put(profileKey, profile);
          if (previousProfile != null)
            profileRows -= profileKey.candidates().size() + previousProfile.size();
          profileRows += profileKey.candidates().size() + profile.size();
          while (additionProfiles.size() > profileLimit || profileRows > 20000) {
            var removed = additionProfiles.pollFirstEntry();
            profileRows -= removed.getKey().candidates().size() + removed.getValue().size();
          }
        }
        flush = profile.iterator();
      }
    }

    private void accept(BasicCurrencyState witness, Fraction mass) {
      materializedWitnesses++;
      var target = key(witness);
      outcomes.merge(target, mass, Fraction::add);
      witnesses.putIfAbsent(target, witness);
    }

    public Kernel result() {
      if (!complete()) throw new IllegalStateException("Partial indexed class kernel");
      return new Kernel(source, action, outcomes, witnesses, raw.status());
    }
  }

  /**
   * {@code BasicTransitionClasses} 내부의 진행커서 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code source}: 출처 ({@code Key}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code WorkbenchCurrency}).
   *   <li>{@code raw}: 원문 ({@code BasicCurrencyTransitions.PrimitiveCursor}).
   *   <li>{@code outcomes}: 결과목록 ({@code Map<Key, Fraction>}).
   *   <li>{@code witnesses}: 증명상태목록 ({@code Map<Key, BasicCurrencyState>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Cursor(state, action)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code state}(상태), {@code
   *       action}(행동).
   *   <li>{@code complete()}: 완료 값을 조회·구성한다. 반환: {@code boolean}.
   *   <li>{@code step()}: 계산 또는 실행을 한 단계 진행한다.
   *   <li>{@code result()}: 최종 또는 현재 계산 결과를 구성한다. 반환: {@code Kernel}. 사용하는 예외: {@code
   *       IllegalStateException}.
   * </ul>
   */
  public final class Cursor {
    private final Key source;
    private final WorkbenchCurrency action;
    private final BasicCurrencyTransitions.PrimitiveCursor raw;
    private final Map<Key, Fraction> outcomes = new LinkedHashMap<>();
    private final Map<Key, BasicCurrencyState> witnesses = new LinkedHashMap<>();

    private Cursor(BasicCurrencyState state, WorkbenchCurrency action) {
      source = key(state);
      this.action = action;
      raw = raw(state, action);
    }

    public boolean complete() {
      return raw.blocked() || raw.complete();
    }

    public void step() {
      if (complete()) return;
      var result = raw.nextGrouped(partition::end).outcome();
      var target = key(result.state());
      outcomes.merge(target, result.probability(), Fraction::add);
      witnesses.putIfAbsent(target, result.state());
    }

    public Kernel result() {
      if (!complete()) throw new IllegalStateException("Partial class kernel");
      return new Kernel(this);
    }
  }
}
