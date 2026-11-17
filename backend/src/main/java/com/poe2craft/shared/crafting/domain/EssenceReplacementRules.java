package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.*;
import java.util.*;

/**
 * 구체 아이템과 미공개 훼손 슬롯에 같은 대상 우선 교체 규칙을 적용한다.
 *
 * <p>기존 계약: One target-first replacement law for concrete items and unresolved desecrated slots.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code EssenceReplacementRules()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 *   <li>{@code plan(state, catalog, targets, omen)}: 지원 규칙 아래 실행할 분기를 계획한다. 입력: {@code state}(상태),
 *       {@code catalog}(카탈로그), {@code targets}(대상목록), {@code omen}(징조). 반환: {@code Plan}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Outcome}: {@code EssenceReplacementRules}에서 사용하는 결과의 불변 값이다.
 *   <li>{@code Plan}: {@code EssenceReplacementRules}에서 사용하는 계획의 불변 값이다.
 * </ul>
 */
public final class EssenceReplacementRules {
  private EssenceReplacementRules() {}

  /**
   * {@code EssenceReplacementRules}에서 사용하는 결과의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code target}: 대상 ({@code ModifierDefinition}).
   *   <li>{@code removals}: removals ({@code List<AbyssCraftingState.Slot>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Outcome(target, removals)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code target}(대상), {@code
   *       removals}(removals).
   * </ul>
   */
  public record Outcome(ModifierDefinition target, List<AbyssCraftingState.Slot> removals) {
    public Outcome {
      removals = List.copyOf(removals);
    }
  }

  /**
   * {@code EssenceReplacementRules}에서 사용하는 계획의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code outcomes}: 결과목록 ({@code List<Outcome>}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Plan(outcomes, reason)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code outcomes}(결과목록), {@code
   *       reason}(이유).
   *   <li>{@code available()}: 현재 조건에서 사용할 수 있는지 판정한다. 반환: {@code boolean}.
   * </ul>
   */
  public record Plan(List<Outcome> outcomes, String reason) {
    public Plan {
      outcomes = List.copyOf(outcomes);
    }

    public boolean available() {
      return reason.isEmpty();
    }
  }

  public static Plan plan(
      AbyssCraftingState state,
      ItemCatalog catalog,
      List<ModifierDefinition> targets,
      WorkbenchOmen omen) {
    var limits = new ItemStateValidator(catalog).slots(state.knownItem());
    var removable = state.removalCandidates(omen == null ? null : omen.affix());
    var outcomes = new ArrayList<Outcome>();
    for (var target : targets) {
      int capacity =
          target.affixType() == ModifierDefinition.AffixType.PREFIX
              ? limits.maxPrefixes()
              : limits.maxSuffixes();
      boolean full = state.count(target.affixType()) >= capacity;
      var removals =
          removable.stream().filter(slot -> !full || slot.side() == target.affixType()).toList();
      if (removals.isEmpty())
        return new Plan(List.of(), "No removable modifier remains on the required affix side.");
      var values = new HashMap<String, Long>();
      target.stats().forEach(s -> values.put(s.id(), s.min()));
      // Do not silently condition away family conflicts or invent a retry distribution.
      for (var removed : removals) {
        try {
          state
              .remove(removed.id(), null, catalog)
              .append(removed.id(), new ModifierInstance(target.id(), values).asCrafted(), catalog);
        } catch (IllegalArgumentException conflict) {
          return new Plan(
              List.of(),
              "An essence removal branch conflicts with a guaranteed modifier family or item constraints.");
        }
      }
      outcomes.add(new Outcome(target, removals));
    }
    return new Plan(outcomes, outcomes.isEmpty() ? "No verified essence outcome." : "");
  }
}
