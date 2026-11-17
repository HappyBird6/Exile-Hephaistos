package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner.*;
import com.poe2craft.shared.probability.Fraction;
import java.util.*;

/**
 * 전체 상태 또는 불변 조건부 분포의 복귀가 증명된 갱신 결과다.
 *
 * <p>기존 계약: Exact renewal shortcuts with full-state return or invariant conditional-distribution
 * proofs.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code RenewalMethodObservation()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code repeatInvariant(before, after, count)}: 반복·불변조건 처리에서 {@code after.cycles}, {@code
 *       before.cycles}을(를) 사용한다. 입력: {@code before}(변경전), {@code after}(변경후), {@code count}(개수).
 *       반환: {@code Optional<Observation<S, A>>}. 사용하는 예외: {@code IllegalStateException}.
 *   <li>{@code divide(a, b)}: 두 값의 비를 계산한다. 입력: {@code a}(a), {@code b}(b). 반환: {@code Fraction}.
 *   <li>{@code monotone(before, after)}: monotone 처리에서 {@code before.entrySet}, {@code
 *       entry.getValue}을(를) 사용한다. 입력: {@code before}(변경전), {@code after}(변경후). 반환: {@code boolean}.
 *   <li>{@code scale(values, factor)}: 단위배율 처리에서 {@code values.forEach}, {@code value.multiply}을(를)
 *       사용한다. 입력: {@code values}(값목록), {@code factor}(배율). 반환: {@code Map<K, Fraction>}.
 *   <li>{@code accumulated(before, after, factor)}: accumulated 처리에서 {@code before.keySet}, {@code
 *       keys.addAll}을(를) 사용한다. 입력: {@code before}(변경전), {@code after}(변경후), {@code factor}(배율). 반환:
 *       {@code Map<K, Fraction>}.
 *   <li>{@code repeat(cycle, source, count)}: 반복 처리에서 {@code cycle.cycles}, {@code
 *       cycle.initialUses}을(를) 사용한다. 입력: {@code cycle}(주기), {@code source}(출처), {@code count}(개수).
 *       반환: {@code Observation<S, A>}. 사용하는 예외: {@code IllegalArgumentException}, {@code
 *       IllegalStateException}.
 * </ul>
 */
public final class RenewalMethodObservation {
  private RenewalMethodObservation() {}

  /**
   * Caller must supply adjacent boundaries of the same time-homogeneous full-state cycle. If
   * T*u=q*u exactly, linearity proves all subsequent live distributions q^k*u. No equality of
   * displayed goal buckets or empirical sampling is sufficient.
   */
  public static <S, A> Optional<Observation<S, A>> repeatInvariant(
      Observation<S, A> before, Observation<S, A> after, int count) {
    if (after.cycles() != before.cycles() + 1
        || count < after.cycles()
        || before.active().isEmpty()
        || !before.initialUses().equals(after.initialUses())) return Optional.empty();
    if (!after.active().isEmpty() && !before.active().keySet().equals(after.active().keySet()))
      return Optional.empty();
    var anchor =
        before.active().entrySet().stream()
            .filter(e -> !e.getValue().equals(Fraction.ZERO))
            .findFirst();
    if (anchor.isEmpty()) return Optional.empty();
    // One positive component determines q; equality of every component still proves T*u=q*u.
    var q =
        divide(
            after.active().getOrDefault(anchor.get().getKey(), Fraction.ZERO),
            anchor.get().getValue());
    if (q.numerator().compareTo(q.denominator()) > 0) return Optional.empty();
    int positive = 0;
    for (var entry : before.active().entrySet()) {
      if (entry.getValue().equals(Fraction.ZERO) || q.equals(Fraction.ZERO)) continue;
      var actual = after.active().get(entry.getKey());
      if (actual == null || !entry.getValue().multiplyEquals(q, actual)) return Optional.empty();
      positive++;
    }
    if (positive != after.active().size()) return Optional.empty();
    if (!before.mass().equals(Fraction.ONE) || !after.mass().equals(Fraction.ONE))
      return Optional.empty();
    if (!monotone(before.cycleUses(), after.cycleUses())) return Optional.empty();
    for (var kind : Exit.values())
      if (!monotone(before.exits().get(kind), after.exits().get(kind))) return Optional.empty();
    int repeats = Math.toIntExact(count - before.cycles());
    var survival = q.pow(repeats);
    var factor =
        q.equals(Fraction.ONE)
            ? Fraction.of(repeats, 1)
            : divide(RenewalFirstHit.complement(survival), RenewalFirstHit.complement(q));
    var exits = new EnumMap<Exit, Map<S, Fraction>>(Exit.class);
    for (var kind : Exit.values())
      exits.put(kind, accumulated(before.exits().get(kind), after.exits().get(kind), factor));
    var result =
        new Observation<>(
            count,
            Map.copyOf(exits),
            scale(before.active(), survival),
            before.initialUses(),
            accumulated(before.cycleUses(), after.cycleUses(), factor));
    if (!result.mass().equals(Fraction.ONE))
      throw new IllegalStateException("Invariant renewal mass not conserved");
    return Optional.of(result);
  }

  private static Fraction divide(Fraction a, Fraction b) {
    return a.divide(b);
  }

  private static <K> boolean monotone(Map<K, Fraction> before, Map<K, Fraction> after) {
    return before.entrySet().stream()
        .allMatch(
            entry -> {
              var a = entry.getValue();
              var b = after.getOrDefault(entry.getKey(), Fraction.ZERO);
              return b.numerator()
                      .multiply(a.denominator())
                      .compareTo(a.numerator().multiply(b.denominator()))
                  >= 0;
            });
  }

  private static <K> Map<K, Fraction> scale(Map<K, Fraction> values, Fraction factor) {
    var scaled = new LinkedHashMap<K, Fraction>();
    values.forEach(
        (key, value) -> {
          var mass = value.multiply(factor);
          if (!mass.equals(Fraction.ZERO)) scaled.put(key, mass);
        });
    return Map.copyOf(scaled);
  }

  private static <K> Map<K, Fraction> accumulated(
      Map<K, Fraction> before, Map<K, Fraction> after, Fraction factor) {
    var result = new LinkedHashMap<K, Fraction>(before);
    var keys = new HashSet<K>(before.keySet());
    keys.addAll(after.keySet());
    for (var key : keys) {
      var a = before.getOrDefault(key, Fraction.ZERO);
      var b = after.getOrDefault(key, Fraction.ZERO);
      var increment =
          new Fraction(
              b.numerator()
                  .multiply(a.denominator())
                  .subtract(a.numerator().multiply(b.denominator())),
              a.denominator().multiply(b.denominator()));
      var value = a.add(increment.multiply(factor));
      if (!value.equals(Fraction.ZERO)) result.put(key, value);
    }
    return Map.copyOf(result);
  }

  public static <S, A> Observation<S, A> repeat(Observation<S, A> cycle, S source, int count) {
    if (count < 1
        || cycle.cycles() != 1
        || !cycle.initialUses().isEmpty()
        || (!cycle.active().isEmpty() && !cycle.active().keySet().equals(Set.of(source)))
        || !cycle.mass().equals(Fraction.ONE))
      throw new IllegalArgumentException("Full return proof required");
    var r = cycle.active().getOrDefault(source, Fraction.ZERO);
    var survival = r.pow(count);
    var stop = RenewalFirstHit.complement(r);
    var totalStop = RenewalFirstHit.complement(survival);
    var factor = r.equals(Fraction.ONE) ? Fraction.of(count, 1) : totalStop.divide(stop);
    var exits = new EnumMap<Exit, Map<S, Fraction>>(Exit.class);
    cycle
        .exits()
        .forEach(
            (kind, values) -> {
              var scaled = new LinkedHashMap<S, Fraction>();
              values.forEach((state, mass) -> scaled.put(state, mass.multiply(factor)));
              exits.put(kind, Map.copyOf(scaled));
            });
    var uses = new LinkedHashMap<A, Fraction>();
    cycle.cycleUses().forEach((action, mass) -> uses.put(action, mass.multiply(factor)));
    var result =
        new Observation<>(
            count,
            Map.copyOf(exits),
            survival.equals(Fraction.ZERO) ? Map.<S, Fraction>of() : Map.of(source, survival),
            Map.<A, Fraction>of(),
            Map.copyOf(uses));
    if (!result.mass().equals(Fraction.ONE))
      throw new IllegalStateException("Renewal mass not conserved");
    return result;
  }
}
