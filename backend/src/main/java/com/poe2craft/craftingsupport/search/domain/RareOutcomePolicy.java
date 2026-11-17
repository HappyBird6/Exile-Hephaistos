package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.craftingsupport.search.domain.ExactMethodRunner.Exit;
import java.util.*;
import java.util.function.Function;

/**
 * 전체 조건부 방법 분포에 적용하는 희귀 결과 경계 정책이다.
 *
 * <p>기존 계약: Session boundaries apply to complete conditional method laws, never unenumerated mass.
 *
 * <p>필드·상수·record 구성요소:
 *
 * <ul>
 *   <li>{@code version}: 저장·실행 계약의 버전 ({@code String}).
 *   <li>{@code enabled}: 사용여부 ({@code boolean}).
 *   <li>{@code groupPartsPerMillion}: 그룹·부분목록·단위별·Million ({@code int}).
 *   <li>{@code maximumMethodPartsPerMillion}: 최대값·제작방법·부분목록·단위별·Million ({@code int}).
 *   <li>{@code VERSION}: 버전; 초기값 {@code "rare-conditional-groups-v1"} ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code RareOutcomePolicy(version, enabled, groupPartsPerMillion,
 *       maximumMethodPartsPerMillion)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code version}(버전), {@code
 *       enabled}(사용여부), {@code groupPartsPerMillion}(그룹·부분목록·단위별·Million), {@code
 *       maximumMethodPartsPerMillion}(최대값·제작방법·부분목록·단위별·Million). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code disabled()}: 비활성 값을 조회·구성한다. 반환: {@code RareOutcomePolicy}.
 *   <li>{@code within(mass, parts)}: within 처리에서 {@code mass.upperUnits}, {@code
 *       BigInteger.valueOf}을(를) 사용한다. 입력: {@code mass}(확률질량), {@code parts}(부분목록). 반환: {@code
 *       boolean}.
 *   <li>{@code apply(point, equivalence, goal, includeActive)}: 주어진 행동 또는 변경을 적용한 결과를 구한다. 입력:
 *       {@code point}(지점), {@code equivalence}(equivalence), {@code goal}(목표), {@code
 *       includeActive}(include·활성). 반환: {@code CertifiedMethodRunner.Observation<S, A>}.
 * </ul>
 */
public record RareOutcomePolicy(
    String version, boolean enabled, int groupPartsPerMillion, int maximumMethodPartsPerMillion) {
  public static final String VERSION = "rare-conditional-groups-v1";

  public RareOutcomePolicy {
    if (!VERSION.equals(version)
        || groupPartsPerMillion < 0
        || groupPartsPerMillion > 1_000_000
        || maximumMethodPartsPerMillion < 0
        || maximumMethodPartsPerMillion > 1_000_000)
      throw new IllegalArgumentException("Invalid rare outcome policy");
  }

  public static RareOutcomePolicy disabled() {
    return new RareOutcomePolicy(VERSION, false, 0, 0);
  }

  private static boolean within(CertifiedInterval mass, int parts) {
    return mass.upperUnits()
            .multiply(java.math.BigInteger.valueOf(1_000_000))
            .compareTo(java.math.BigInteger.valueOf(parts).shiftLeft(mass.fractionalBits()))
        <= 0;
  }

  /** Deterministic whole-group selection; upper bounds must prove both configured limits. */
  public <S, A> CertifiedMethodRunner.Observation<S, A> apply(
      CertifiedMethodRunner.Observation<S, A> point,
      Function<S, Object> equivalence,
      java.util.function.Predicate<S> goal,
      boolean includeActive) {
    if (!enabled || !point.exits().getOrDefault(Exit.REBASE, Map.of()).isEmpty()) return point;
    int bits = point.mass().fractionalBits();
    var groups = new LinkedHashMap<Object, CertifiedInterval>();
    var eligible = new LinkedHashMap<S, Object>();
    var laws = new ArrayList<Map<S, CertifiedInterval>>();
    laws.add(point.exits().getOrDefault(Exit.CHECKPOINT, Map.of()));
    if (includeActive) laws.add(point.active());
    for (var law : laws)
      law.forEach(
          (state, mass) -> {
            if (!goal.test(state)) {
              var key = equivalence.apply(state);
              eligible.put(state, key);
              groups.merge(key, mass, CertifiedInterval::add);
            }
          });
    var selected = new HashSet<Object>();
    var used = CertifiedInterval.zero(bits);
    // Treat equal upper-mass groups together: iteration/hash order must never choose a
    // different boundary after restore or on a different JVM.
    var ranked = new TreeMap<java.math.BigInteger, List<Object>>();
    groups.forEach(
        (key, mass) -> {
          if (within(mass, groupPartsPerMillion))
            ranked.computeIfAbsent(mass.upperUnits(), ignored -> new ArrayList<>()).add(key);
        });
    for (var ties : ranked.values()) {
      var mass = CertifiedInterval.zero(bits);
      for (var key : ties) mass = mass.add(groups.get(key));
      if (within(used.add(mass), maximumMethodPartsPerMillion)) {
        selected.addAll(ties);
        used = used.add(mass);
      }
    }
    var exits = new EnumMap<Exit, Map<S, CertifiedInterval>>(Exit.class);
    for (var kind : Exit.values())
      exits.put(kind, new LinkedHashMap<>(point.exits().getOrDefault(kind, Map.of())));
    var active = new LinkedHashMap<>(point.active());
    for (var law :
        includeActive
            ? List.of(exits.get(Exit.CHECKPOINT), active)
            : List.of(exits.get(Exit.CHECKPOINT))) {
      for (var entry : new ArrayList<>(law.entrySet())) {
        if (goal.test(entry.getKey())) {
          law.remove(entry.getKey());
          exits.get(Exit.GOAL).merge(entry.getKey(), entry.getValue(), CertifiedInterval::add);
        } else if (selected.contains(eligible.get(entry.getKey()))) {
          law.remove(entry.getKey());
          exits.get(Exit.REBASE).merge(entry.getKey(), entry.getValue(), CertifiedInterval::add);
        }
      }
    }
    return new CertifiedMethodRunner.Observation<>(
        point.cycles(), exits, active, point.initialUses(), point.cycleUses(), point.mass());
  }
}
