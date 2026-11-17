package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.crafting.domain.BasicCurrencyTransitions;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.probability.Fraction;
import java.math.BigInteger;
import java.util.*;

/**
 * 기존 수치 변수를 유지하며 기본 행동 후 새 옵션 수치 셀을 추가한다.
 *
 * <p>기존 계약: Basic actions carry retained roll variables and append a fresh declared independent
 * roll cell.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code primitive}: 개별행동 ({@code BasicCurrencyTransitions}).
 *   <li>{@code partition}: 분할 ({@code RollPartition}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code DeferredTransitions(primitive, partition)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       primitive}(개별행동), {@code partition}(분할).
 *   <li>{@code primitiveCursor(source, action, omens)}: 개별행동·진행커서 처리에서 {@code partition.orbits},
 *       {@code primitive.primitiveCursor}을(를) 사용한다. 입력: {@code source}(출처), {@code action}(행동),
 *       {@code omens}(징조목록). 반환: {@code Cursor}. 사용하는 예외: {@code IllegalArgumentException}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Identity}: {@code DeferredTransitions}에서 사용하는 정체성의 불변 값이다.
 *   <li>{@code Cursor}: {@code DeferredTransitions} 내부의 진행커서 처리를 맡는다.
 * </ul>
 */
public final class DeferredTransitions {
  /**
   * {@code DeferredTransitions}에서 사용하는 정체성의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code kernel}: 전이커널 ({@code BasicCurrencyTransitions.KernelIdentity}).
   *   <li>{@code retainedLaws}: 보유·분포목록 ({@code List<Map<String, DeferredItem.Band>>}).
   *   <li>{@code retainedTiers}: 보유·티어목록 ({@code List<Map<String, TierCell>>}).
   *   <li>{@code retainedOrbits}: 보유·교환가능계열목록 ({@code List<Map<String, OrbitCell>>}).
   * </ul>
   */
  public record Identity(
      BasicCurrencyTransitions.KernelIdentity kernel,
      List<Map<String, DeferredItem.Band>> retainedLaws,
      List<Map<String, TierCell>> retainedTiers,
      List<Map<String, OrbitCell>> retainedOrbits) {}

  private final BasicCurrencyTransitions primitive;
  private final RollPartition partition;

  public DeferredTransitions(BasicCurrencyTransitions primitive, RollPartition partition) {
    this.primitive = primitive;
    this.partition = partition;
  }

  public Cursor primitiveCursor(DeferredItem source, WorkbenchCurrency action, Set<String> omens) {
    partition.orbits().validateSource(source);
    var cursor = primitive.primitiveCursor(source.representative(), action, omens);
    if (!cursor.blocked() && !partition.allows(action))
      throw new IllegalArgumentException(
          "Action is outside the partition's reachable rarity scope");
    return new Cursor(source, cursor);
  }

  /**
   * {@code DeferredTransitions} 내부의 진행커서 처리를 맡는다.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code source}: 출처 ({@code DeferredItem}).
   *   <li>{@code cursor}: 진행커서 ({@code BasicCurrencyTransitions.PrimitiveCursor}).
   *   <li>{@code emitted}: 내보낸 ({@code Set<TierEmission>}).
   *   <li>{@code next}: 다음 ({@code ExactMethodRunner.Outcome<DeferredItem>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Cursor(source, cursor)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code source}(출처), {@code
   *       cursor}(진행커서).
   *   <li>{@code blocked()}: 차단 값을 조회·구성한다. 반환: {@code boolean}.
   *   <li>{@code complete()}: 완료 값을 조회·구성한다. 반환: {@code boolean}.
   *   <li>{@code status()}: 현재 진행 상태를 조회한다. 반환: {@code BasicCurrencyTransitions.Status}.
   *   <li>{@code reason()}: 이유 값을 조회·구성한다. 반환: {@code String}.
   *   <li>{@code remainingOutcomes()}: 남은·결과목록 값을 조회·구성한다. 반환: {@code BigInteger}.
   *   <li>{@code assumptions()}: 계산가정 값을 조회·구성한다. 반환: {@code List<WorkbenchSimulator.Assumption>}.
   *   <li>{@code mayApplyAfter(action)}: may·적용·변경후 처리에서 {@code cursor.mayApplyAfter}을(를) 사용한다. 입력:
   *       {@code action}(행동). 반환: {@code boolean}.
   *   <li>{@code identity()}: 현재 값 또는 규칙의 정체성 식별자를 조회한다. 반환: {@code Optional<Identity>}.
   *   <li>{@code next()}: 다음 값을 조회·구성한다. 반환: {@code ExactMethodRunner.Outcome<DeferredItem>}. 사용하는
   *       예외: {@code IllegalStateException}.
   *   <li>{@code advance()}: 허용된 계산량만큼 작업을 진행한다. 사용하는 예외: {@code IllegalStateException}.
   * </ul>
   *
   * <p>중첩 타입:
   *
   * <ul>
   *   <li>{@code TierEmission}: {@code Cursor}에서 사용하는 티어·Emission의 불변 값이다.
   * </ul>
   */
  public final class Cursor {
    private final DeferredItem source;
    private final BasicCurrencyTransitions.PrimitiveCursor cursor;
    private final Set<TierEmission> emitted = new HashSet<>();
    private ExactMethodRunner.Outcome<DeferredItem> next;

    /**
     * {@code Cursor}에서 사용하는 티어·Emission의 불변 값이다.
     *
     * <p>필드·상수·record 구성요소:
     *
     * <ul>
     *   <li>{@code branch}: 분기 ({@code int}).
     *   <li>{@code cell}: 셀 ({@code TierCell}).
     * </ul>
     */
    private record TierEmission(int branch, TierCell cell) {}

    private Cursor(DeferredItem source, BasicCurrencyTransitions.PrimitiveCursor cursor) {
      this.source = source;
      this.cursor = cursor;
      if (!cursor.blocked()) advance();
    }

    public boolean blocked() {
      return cursor.blocked();
    }

    public boolean complete() {
      return !cursor.blocked() && next == null;
    }

    public BasicCurrencyTransitions.Status status() {
      return cursor.blocked()
          ? cursor.status()
          : complete()
              ? BasicCurrencyTransitions.Status.COMPLETE
              : BasicCurrencyTransitions.Status.PARTIAL;
    }

    public String reason() {
      return cursor.reason();
    }

    public BigInteger remainingOutcomes() {
      return cursor.remainingOutcomes();
    }

    public List<WorkbenchSimulator.Assumption> assumptions() {
      return cursor.assumptions();
    }

    public boolean mayApplyAfter(WorkbenchCurrency action) {
      return cursor.mayApplyAfter(action);
    }

    public Optional<Identity> identity() {
      return cursor
          .identity()
          .map(
              k ->
                  new Identity(
                      k,
                      k.branches().stream()
                          .map(
                              branch -> {
                                var retained = new HashMap<>(source.rolls());
                                retained
                                    .keySet()
                                    .retainAll(
                                        branch.remaining().explicits().stream()
                                            .map(m -> m.modifierId())
                                            .toList());
                                return Map.copyOf(retained);
                              })
                          .toList(),
                      k.branches().stream()
                          .map(
                              branch -> {
                                var retained = new HashMap<>(source.tiers());
                                retained
                                    .keySet()
                                    .retainAll(
                                        branch.remaining().explicits().stream()
                                            .map(ModifierInstance::modifierId)
                                            .toList());
                                return Map.copyOf(retained);
                              })
                          .toList(),
                      k.branches().stream()
                          .map(
                              branch -> {
                                var ids =
                                    branch.remaining().explicits().stream()
                                        .map(ModifierInstance::modifierId)
                                        .collect(java.util.stream.Collectors.toSet());
                                var retained = new HashMap<>(source.orbits());
                                retained
                                    .entrySet()
                                    .removeIf(
                                        entry ->
                                            entry.getValue().representatives().stream()
                                                .noneMatch(m -> ids.contains(m.modifierId())));
                                return Map.copyOf(retained);
                              })
                          .toList()));
    }

    public ExactMethodRunner.Outcome<DeferredItem> next() {
      if (next == null) throw new IllegalStateException("Cursor is not active");
      var result = next;
      advance();
      return result;
    }

    private void advance() {
      next = null;
      while (!cursor.complete()) {
        var group = cursor.nextGrouped(partition::end);
        var retained = new HashMap<>(source.rolls());
        var tiers = new HashMap<>(source.tiers());
        var ids =
            group.outcome().state().item().explicits().stream().map(m -> m.modifierId()).toList();
        retained.keySet().retainAll(ids);
        tiers.keySet().retainAll(ids);
        var state = group.outcome().state();
        var probability = group.outcome().probability();
        if (group.addedModifier() != null) {
          retained.remove(
              group.addedModifier()); // A removed-and-readded modifier gets a fresh roll law.
          tiers.remove(group.addedModifier());
          var member = partition.tiers().member(group.addedModifier(), group.min(), group.max());
          if (member.isPresent()) {
            var law = member.get();
            if (!emitted.add(new TierEmission(group.branch(), law.cell()))) continue;
            var candidates =
                cursor.identity().orElseThrow().branches().get(group.branch()).candidates().stream()
                    .map(ModifierDefinition::id)
                    .collect(java.util.stream.Collectors.toSet());
            if (law.cell().variants().stream().anyMatch(v -> !candidates.contains(v.modifierId())))
              throw new IllegalStateException("Tier partition crosses an eligibility boundary");
            var first = law.cell().representative(false);
            var explicits = new ArrayList<>(state.item().explicits());
            explicits.removeIf(m -> m.modifierId().equals(group.addedModifier()));
            explicits.add(first);
            var item = state.item();
            state =
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
            tiers.put(first.modifierId(), law.cell());
            probability =
                probability.multiply(
                    new Fraction(
                        law.cellWeight().numerator().multiply(law.intervalWeight().denominator()),
                        law.cellWeight().denominator().multiply(law.intervalWeight().numerator())));
          } else if (group.min() != group.max())
            retained.put(
                group.addedModifier(),
                new DeferredItem.Band(group.stat(), group.min(), group.max()));
        }
        next =
            new ExactMethodRunner.Outcome<>(
                partition.orbits().normalize(state, retained, tiers), probability);
        return;
      }
    }
  }
}
