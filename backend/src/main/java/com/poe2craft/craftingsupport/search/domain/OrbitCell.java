package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.probability.Fraction;
import java.math.BigInteger;
import java.util.*;

/**
 * 교환 가능한 목표 무관 옵션 계열의 비복원 결합 분포다.
 *
 * <p>기존 계약: Joint, without-replacement law over exchangeable goal-irrelevant families.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code profile}: 특성 ({@code Profile}).
 *   <li>{@code counts}: 개수목록 ({@code Map<Integer, Integer>}).
 *   <li>{@code assignments}: assignments ({@code List<Assignment>}).
 *   <li>{@code cardinality}: cardinality ({@code BigInteger}).
 *   <li>{@code components}: 구성요소목록 ({@code Map<OrbitCell, Fraction>}).
 *   <li>{@code representatives}: 대표상태목록 ({@code List<ModifierInstance>}).
 *   <li>{@code hash}: 해시 ({@code int}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code OrbitCell(profile, counts)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code profile}(특성), {@code
 *       counts}(개수목록). 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code OrbitCell(components)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code components}(구성요소목록).
 *   <li>{@code mix(distribution)}: mix 처리에서 {@code distribution.isEmpty}, {@code
 *       distribution.keySet}을(를) 사용한다. 입력: {@code distribution}(확률분포). 반환: {@code OrbitCell}. 사용하는
 *       예외: {@code IllegalArgumentException}.
 *   <li>{@code mixed()}: mixed 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code occupancy()}: occupancy 값을 조회·구성한다. 반환: {@code int}.
 *   <li>{@code basis()}: basis 값을 조회·구성한다. 반환: {@code Map<OrbitCell, Fraction>}.
 *   <li>{@code enumerate(family, remaining, selected, result)}: enumerate 처리에서 {@code
 *       remaining.values}, {@code result.add}을(를) 사용한다. 입력: {@code family}(옵션계열), {@code
 *       remaining}(남은), {@code selected}(선택된), {@code result}(결과). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code profile()}: {@code profile} 값을 조회한다. 반환: {@code Profile}.
 *   <li>{@code counts()}: 개수목록 값을 조회·구성한다. 반환: {@code Map<Integer, Integer>}. 사용하는 예외: {@code
 *       IllegalStateException}.
 *   <li>{@code assignments()}: assignments 값을 조회·구성한다. 반환: {@code List<Assignment>}. 사용하는 예외:
 *       {@code IllegalStateException}.
 *   <li>{@code cardinality()}: {@code cardinality} 값을 조회한다. 반환: {@code BigInteger}.
 *   <li>{@code representatives()}: {@code representatives} 값을 조회한다. 반환: {@code
 *       List<ModifierInstance>}.
 *   <li>{@code concrete(offset)}: 구체값 처리에서 {@code offset.signum}, {@code offset.compareTo}을(를)
 *       사용한다. 입력: {@code offset}(시작위치). 반환: {@code Concrete}. 사용하는 예외: {@code
 *       IllegalArgumentException}, {@code IllegalStateException}.
 *   <li>{@code equals(other)}: 값의 동등성을 비교한다. 입력: {@code other}(비교대상). 반환: {@code boolean}.
 *   <li>{@code hashCode()}: 동등성 비교와 일치하는 해시를 반환한다. 반환: {@code int}.
 *   <li>{@code toString()}: 값을 문자열 표현으로 바꾼다. 반환: {@code String}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Family}: {@code OrbitCell}에서 사용하는 옵션계열의 불변 값이다.
 *   <li>{@code Profile}: {@code OrbitCell}에서 사용하는 특성의 불변 값이다.
 *   <li>{@code Assignment}: {@code OrbitCell}에서 사용하는 Assignment의 불변 값이다.
 *   <li>{@code Concrete}: {@code OrbitCell}에서 사용하는 구체값의 불변 값이다.
 * </ul>
 */
public final class OrbitCell {
  /**
   * {@code OrbitCell}에서 사용하는 옵션계열의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code ids}: 식별자목록 ({@code Set<String>}).
   *   <li>{@code categories}: 분류목록 ({@code Map<Integer, TierCell>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Family(ids, categories)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code ids}(식별자목록), {@code
   *       categories}(분류목록).
   * </ul>
   */
  public record Family(Set<String> ids, Map<Integer, TierCell> categories) {
    public Family {
      ids = Set.copyOf(ids);
      categories = Map.copyOf(categories);
    }
  }

  /**
   * {@code OrbitCell}에서 사용하는 특성의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code affix}: 접두접미옵션 ({@code ModifierDefinition.AffixType}).
   *   <li>{@code families}: 옵션계열목록 ({@code List<Family>}).
   *   <li>{@code weights}: 가중치목록 ({@code Map<Integer, Long>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Profile(id, affix, families, weights)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code id}(식별자),
   *       {@code affix}(접두접미옵션), {@code families}(옵션계열목록), {@code weights}(가중치목록).
   * </ul>
   */
  public record Profile(
      String id,
      ModifierDefinition.AffixType affix,
      List<Family> families,
      Map<Integer, Long> weights) {
    public Profile {
      families = List.copyOf(families);
      weights = Map.copyOf(weights);
    }
  }

  /**
   * {@code OrbitCell}에서 사용하는 Assignment의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code families}: 옵션계열목록 ({@code List<TierCell>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Assignment(families)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code families}(옵션계열목록).
   *   <li>{@code cardinality()}: cardinality 값을 조회·구성한다. 반환: {@code BigInteger}.
   * </ul>
   */
  public record Assignment(List<TierCell> families) {
    public Assignment {
      families = List.copyOf(families);
    }

    public BigInteger cardinality() {
      return families.stream()
          .map(TierCell::cardinality)
          .reduce(BigInteger.ONE, BigInteger::multiply);
    }
  }

  /**
   * {@code OrbitCell}에서 사용하는 구체값의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code modifiers}: 옵션목록 ({@code List<ModifierInstance>}).
   *   <li>{@code probability}: 확률 ({@code Fraction}).
   * </ul>
   */
  public record Concrete(List<ModifierInstance> modifiers, Fraction probability) {}

  private final Profile profile;
  private final Map<Integer, Integer> counts;
  private final List<Assignment> assignments;
  private final BigInteger cardinality;
  private final Map<OrbitCell, Fraction> components;
  private final List<ModifierInstance> representatives;
  private final int hash;

  public OrbitCell(Profile profile, Map<Integer, Integer> counts) {
    components = Map.of();
    this.profile = Objects.requireNonNull(profile);
    this.counts = Collections.unmodifiableMap(new TreeMap<>(counts));
    int occupied = counts.values().stream().mapToInt(Integer::intValue).sum();
    if (occupied < 1
        || occupied > profile.families().size()
        || counts.values().stream().anyMatch(n -> n < 1)
        || !profile.weights().keySet().containsAll(counts.keySet()))
      throw new IllegalArgumentException("Invalid family occupancy");
    var options = new ArrayList<Assignment>();
    enumerate(0, new TreeMap<>(counts), new ArrayList<>(), options);
    if (options.isEmpty()) throw new IllegalArgumentException("Empty orbit");
    assignments = List.copyOf(options);
    cardinality =
        assignments.stream().map(Assignment::cardinality).reduce(BigInteger.ZERO, BigInteger::add);
    representatives =
        assignments.getFirst().families().stream().map(c -> c.representative(false)).toList();
    hash = Objects.hash(profile.id(), this.counts);
  }

  private OrbitCell(Map<OrbitCell, Fraction> components) {
    var first = components.keySet().iterator().next();
    this.profile = first.profile;
    this.counts = first.counts;
    this.assignments = first.assignments;
    this.representatives = first.representatives;
    this.components = Collections.unmodifiableMap(new LinkedHashMap<>(components));
    this.cardinality =
        components.keySet().stream()
            .map(OrbitCell::cardinality)
            .reduce(BigInteger.ZERO, BigInteger::add);
    this.hash = Objects.hash(profile.id(), this.components);
  }

  /** Components have disjoint category-count support and equal family occupancy. */
  public static OrbitCell mix(Map<OrbitCell, Fraction> distribution) {
    if (distribution.isEmpty()) throw new IllegalArgumentException("Empty orbit mixture");
    var first = distribution.keySet().iterator().next();
    var flat = new TreeMap<OrbitCell, Fraction>(Comparator.comparing(c -> c.counts.toString()));
    var total = Fraction.ZERO;
    for (var entry : distribution.entrySet()) {
      var cell = entry.getKey();
      if (!cell.profile.equals(first.profile) || cell.occupancy() != first.occupancy())
        throw new IllegalArgumentException("Orbit mixture crosses a family or slot boundary");
      if (entry.getValue().numerator().signum() <= 0)
        throw new IllegalArgumentException("Positive orbit mixture weight required");
      total = total.add(entry.getValue());
      cell.basis()
          .forEach(
              (atom, weight) -> flat.merge(atom, weight.multiply(entry.getValue()), Fraction::add));
    }
    if (!total.equals(Fraction.ONE)) throw new IllegalArgumentException("Orbit mixture lost mass");
    return flat.size() == 1 ? flat.firstKey() : new OrbitCell(flat);
  }

  public boolean mixed() {
    return !components.isEmpty();
  }

  public int occupancy() {
    return counts.values().stream().mapToInt(Integer::intValue).sum();
  }

  public Map<OrbitCell, Fraction> basis() {
    return components.isEmpty() ? Map.of(this, Fraction.ONE) : components;
  }

  private void enumerate(
      int family,
      Map<Integer, Integer> remaining,
      List<TierCell> selected,
      List<Assignment> result) {
    if (remaining.values().stream().allMatch(n -> n == 0)) {
      result.add(new Assignment(selected));
      return;
    }
    if (family == profile.families().size()) return;
    enumerate(family + 1, remaining, selected, result);
    for (int category : counts.keySet()) {
      int left = remaining.get(category);
      if (left == 0) continue;
      var cell = profile.families().get(family).categories().get(category);
      if (cell == null) throw new IllegalArgumentException("Asymmetric category support");
      remaining.put(category, left - 1);
      selected.add(cell);
      enumerate(family + 1, remaining, selected, result);
      selected.removeLast();
      remaining.put(category, left);
    }
  }

  public Profile profile() {
    return profile;
  }

  public Map<Integer, Integer> counts() {
    if (mixed()) throw new IllegalStateException("A mixture has no single category occupancy");
    return counts;
  }

  public List<Assignment> assignments() {
    if (mixed()) throw new IllegalStateException("Expand weighted orbit components first");
    return assignments;
  }

  public BigInteger cardinality() {
    return cardinality;
  }

  public List<ModifierInstance> representatives() {
    return representatives;
  }

  public Concrete concrete(BigInteger offset) {
    if (offset.signum() < 0 || offset.compareTo(cardinality) >= 0)
      throw new IllegalArgumentException("Orbit offset out of range");
    if (mixed()) {
      for (var entry : components.entrySet()) {
        if (offset.compareTo(entry.getKey().cardinality()) >= 0) {
          offset = offset.subtract(entry.getKey().cardinality());
          continue;
        }
        var result = entry.getKey().concrete(offset);
        return new Concrete(result.modifiers(), result.probability().multiply(entry.getValue()));
      }
      throw new IllegalStateException("Missing orbit mixture component");
    }
    for (var assignment : assignments) {
      if (offset.compareTo(assignment.cardinality()) >= 0) {
        offset = offset.subtract(assignment.cardinality());
        continue;
      }
      var modifiers = new ArrayList<ModifierInstance>();
      var probability = Fraction.of(1, assignments.size());
      for (var cell : assignment.families()) {
        var divided = offset.divideAndRemainder(cell.cardinality());
        var concrete = cell.concrete(divided[1], false);
        modifiers.add(concrete.modifier());
        probability = probability.multiply(concrete.probability());
        offset = divided[0];
      }
      return new Concrete(List.copyOf(modifiers), probability);
    }
    throw new IllegalStateException("Missing orbit assignment");
  }

  @Override
  public boolean equals(Object other) {
    return this == other
        || other instanceof OrbitCell cell
            && profile.equals(cell.profile)
            && counts.equals(cell.counts)
            && components.equals(cell.components);
  }

  @Override
  public int hashCode() {
    return hash;
  }

  @Override
  public String toString() {
    return components.isEmpty()
        ? "OrbitCell[profile=" + profile.id() + ", counts=" + counts + "]"
        : "OrbitCell[profile=" + profile.id() + ", components=" + components + "]";
  }
}
