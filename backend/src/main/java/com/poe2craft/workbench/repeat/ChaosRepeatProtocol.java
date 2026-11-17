package com.poe2craft.workbench.repeat;

import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.crafting.domain.AbyssCurrencyExecutor;
import com.poe2craft.shared.crafting.domain.QualityLimitRules;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemState;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 단일 제작 응답과 구분되는 반복 요청·최종 결과·중단 이유를 정의한다.
 *
 * <p>기존 계약: Batch values deliberately separate from the single-apply response. No intermediate
 * state field.
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code ChaosRepeatProtocol()}: 외부 인스턴스 생성이 필요 없는 유틸리티 생성자다.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code State}: {@code ChaosRepeatProtocol} 내부의 상태 처리를 맡는다.
 *   <li>{@code Concrete}: {@code ChaosRepeatProtocol}에서 사용하는 구체값의 불변 값이다.
 *   <li>{@code Abyss}: {@code ChaosRepeatProtocol}에서 사용하는 훼손의 불변 값이다.
 *   <li>{@code Input}: {@code ChaosRepeatProtocol}에서 사용하는 입력의 불변 값이다.
 *   <li>{@code Binding}: {@code ChaosRepeatProtocol}에서 사용하는 결합의 불변 값이다.
 *   <li>{@code Rejection}: {@code ChaosRepeatProtocol}에서 사용하는 거절의 불변 값이다.
 *   <li>{@code Failure}: {@code ChaosRepeatProtocol}에서 사용하는 실패의 불변 값이다.
 *   <li>{@code Witness}: {@code ChaosRepeatProtocol} 내부의 증명상태 처리를 맡는다.
 *   <li>{@code ConcreteWitness}: {@code ChaosRepeatProtocol}에서 사용하는 구체값·증명상태의 불변 값이다.
 *   <li>{@code AbyssWitness}: {@code ChaosRepeatProtocol}에서 사용하는 훼손·증명상태의 불변 값이다.
 *   <li>{@code Outcome}: {@code ChaosRepeatProtocol}에서 사용하는 결과의 불변 값이다.
 * </ul>
 */
public final class ChaosRepeatProtocol {
  private ChaosRepeatProtocol() {}

  /**
   * {@code ChaosRepeatProtocol} 내부의 상태 처리를 맡는다.
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code displayItem()}: 표시·아이템 값을 조회·구성한다. 반환: {@code ItemState}.
   * </ul>
   */
  public sealed interface State permits Concrete, Abyss {
    ItemState displayItem();
  }

  /**
   * {@code ChaosRepeatProtocol}에서 사용하는 구체값의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code item}: 아이템 ({@code ItemState}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Concrete(item)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code item}(아이템).
   *   <li>{@code displayItem()}: {@code item} 값을 조회한다. 반환: {@code ItemState}.
   * </ul>
   */
  public record Concrete(String kind, ItemState item) implements State {
    public Concrete(ItemState item) {
      this("CONCRETE", item);
    }

    public ItemState displayItem() {
      return item;
    }
  }

  /**
   * {@code ChaosRepeatProtocol}에서 사용하는 훼손의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code abyss}: 훼손 슬롯을 보존하는 제작 실행기 ({@code AbyssCraftingState}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Abyss(abyss)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code abyss}(훼손).
   *   <li>{@code displayItem()}: 표시·아이템 값을 조회·구성한다. 반환: {@code ItemState}.
   * </ul>
   */
  public record Abyss(String kind, AbyssCraftingState abyss) implements State {
    public Abyss(AbyssCraftingState abyss) {
      this("ABYSS", abyss);
    }

    public ItemState displayItem() {
      return abyss.storedItem();
    }
  }

  /**
   * {@code ChaosRepeatProtocol}에서 사용하는 입력의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code requestId}: 요청·식별자 ({@code String}).
   *   <li>{@code requestSha256}: 요청·SHA·256 ({@code String}).
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code State}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code WorkbenchCurrency}).
   *   <li>{@code activeOmens}: 현재 실행에서 고려할 징조 ID 목록 ({@code Set<String>}).
   *   <li>{@code pattern}: 패턴 ({@code String}).
   *   <li>{@code locale}: 언어코드 ({@code String}).
   *   <li>{@code maximumUses}: 최대값·사용량 ({@code int}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Input(requestId, requestSha256, state, action, activeOmens, pattern, locale,
   *       maximumUses)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code requestId}(요청·식별자), {@code
   *       requestSha256}(요청·SHA·256), {@code state}(상태), {@code action}(행동), {@code
   *       activeOmens}(활성·징조목록), {@code pattern}(패턴), {@code locale}(언어코드), {@code
   *       maximumUses}(최대값·사용량).
   * </ul>
   */
  public record Input(
      String requestId,
      String requestSha256,
      State state,
      WorkbenchCurrency action,
      Set<String> activeOmens,
      String pattern,
      String locale,
      int maximumUses) {
    public Input {
      activeOmens = Set.copyOf(activeOmens);
    }
  }

  /**
   * {@code ChaosRepeatProtocol}에서 사용하는 결합의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code requestId}: 요청·식별자 ({@code String}).
   *   <li>{@code requestSha256}: 요청·SHA·256 ({@code String}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code WorkbenchCurrency}).
   *   <li>{@code locale}: 언어코드 ({@code String}).
   *   <li>{@code maximumUses}: 최대값·사용량 ({@code int}).
   *   <li>{@code matcherVersion}: matcher·버전 ({@code String}).
   * </ul>
   */
  public record Binding(
      String requestId,
      String requestSha256,
      WorkbenchCurrency action,
      String locale,
      int maximumUses,
      String matcherVersion) {}

  /**
   * {@code ChaosRepeatProtocol}에서 사용하는 거절의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code code}: 코드 ({@code String}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   * </ul>
   */
  public record Rejection(String code, String reason) {}

  /**
   * {@code ChaosRepeatProtocol}에서 사용하는 실패의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code code}: 코드 ({@code String}).
   *   <li>{@code stage}: 진행단계 ({@code String}).
   * </ul>
   */
  public record Failure(String code, String stage) {}

  /**
   * {@code ChaosRepeatProtocol} 내부의 증명상태 처리를 맡는다.
   *
   * <p>직접 선언한 필드·생성자·메서드는 없다.
   */
  public sealed interface Witness permits ConcreteWitness, AbyssWitness {}

  /**
   * {@code ChaosRepeatProtocol}에서 사용하는 구체값·증명상태의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code ruleVersion}: 규칙·버전 ({@code String}).
   *   <li>{@code ledgerVersion}: 규칙장부·버전 ({@code String}).
   *   <li>{@code snapshotId}: 카탈로그 스냅샷 식별자 ({@code String}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code WorkbenchCurrency}).
   *   <li>{@code applied}: 요청한 변경의 실제 적용 여부 ({@code boolean}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code events}: 실제로 선택·적용한 변경 기록 ({@code List<WorkbenchSimulator.Event>}).
   *   <li>{@code assumptions}: 확률·수치 계산에 사용한 명시적 가정 ({@code List<WorkbenchSimulator.Assumption>}).
   *   <li>{@code consumedOmens}: 이번 실행으로 소비된 징조 ID 목록 ({@code List<String>}).
   *   <li>{@code remainingOmens}: 실행 뒤 남은 징조 ID 목록 ({@code List<String>}).
   *   <li>{@code qualityLimit}: 품질·한도 ({@code QualityLimitRules.Limit}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code from(result)}: 다른 표현의 입력을 현재 표현으로 바꾼다. 입력: {@code result}(결과). 반환: {@code
   *       ConcreteWitness}.
   * </ul>
   */
  public record ConcreteWitness(
      String kind,
      String ruleVersion,
      String ledgerVersion,
      String snapshotId,
      WorkbenchCurrency action,
      boolean applied,
      String reason,
      List<WorkbenchSimulator.Event> events,
      List<WorkbenchSimulator.Assumption> assumptions,
      List<String> consumedOmens,
      List<String> remainingOmens,
      QualityLimitRules.Limit qualityLimit)
      implements Witness {
    public static ConcreteWitness from(WorkbenchSimulator.Result result) {
      return result == null
          ? null
          : new ConcreteWitness(
              "CONCRETE",
              result.ruleVersion(),
              result.ledgerVersion(),
              result.snapshotId(),
              result.action(),
              result.applied(),
              result.reason(),
              result.events(),
              result.assumptions(),
              result.consumedOmens(),
              result.remainingOmens(),
              result.qualityLimit());
    }
  }

  /**
   * {@code ChaosRepeatProtocol}에서 사용하는 훼손·증명상태의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code WorkbenchCurrency}).
   *   <li>{@code applied}: 요청한 변경의 실제 적용 여부 ({@code boolean}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code consumedOmens}: 이번 실행으로 소비된 징조 ID 목록 ({@code List<String>}).
   *   <li>{@code remainingOmens}: 실행 뒤 남은 징조 ID 목록 ({@code List<String>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code from(result)}: 다른 표현의 입력을 현재 표현으로 바꾼다. 입력: {@code result}(결과). 반환: {@code
   *       AbyssWitness}.
   * </ul>
   */
  public record AbyssWitness(
      String kind,
      WorkbenchCurrency action,
      boolean applied,
      String reason,
      List<String> consumedOmens,
      List<String> remainingOmens)
      implements Witness {
    public static AbyssWitness from(AbyssCurrencyExecutor.Result result) {
      return new AbyssWitness(
          "ABYSS",
          result.action(),
          result.applied(),
          result.reason(),
          result.consumedOmens(),
          result.remainingOmens());
    }
  }

  /**
   * {@code ChaosRepeatProtocol}에서 사용하는 결과의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code schemaVersion}: 스키마·버전 ({@code int}).
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code rulesetIdentity}: 현재 실행 규칙 묶음의 식별자 ({@code String}).
   *   <li>{@code requestBinding}: 요청·결합 ({@code Binding}).
   *   <li>{@code finalState}: 최종·상태 ({@code State}).
   *   <li>{@code actualUses}: 실제값·사용량 ({@code int}).
   *   <li>{@code consumedOmens}: 이번 실행으로 소비된 징조 ID 목록 ({@code Map<String, Integer>}).
   *   <li>{@code remainingOmens}: 실행 뒤 남은 징조 ID 목록 ({@code Set<String>}).
   *   <li>{@code lastAppliedWitness}: 마지막·적용여부·증명상태 ({@code Witness}).
   *   <li>{@code termination}: 종료 ({@code String}).
   *   <li>{@code lastRejection}: 마지막·거절 ({@code Rejection}).
   *   <li>{@code failure}: 실패 ({@code Failure}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Outcome(schemaVersion, kind, rulesetIdentity, requestBinding, finalState,
   *       actualUses, consumedOmens, remainingOmens, lastAppliedWitness, termination,
   *       lastRejection, failure)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code schemaVersion}(스키마·버전), {@code
   *       kind}(종류), {@code rulesetIdentity}(규칙묶음·정체성), {@code requestBinding}(요청·결합), {@code
   *       finalState}(최종·상태), {@code actualUses}(실제값·사용량), {@code consumedOmens}(소비된·징조목록), {@code
   *       remainingOmens}(남은·징조목록), {@code lastAppliedWitness}(마지막·적용여부·증명상태), {@code
   *       termination}(종료), {@code lastRejection}(마지막·거절), {@code failure}(실패).
   * </ul>
   */
  public record Outcome(
      int schemaVersion,
      String kind,
      String rulesetIdentity,
      Binding requestBinding,
      State finalState,
      int actualUses,
      Map<String, Integer> consumedOmens,
      Set<String> remainingOmens,
      Witness lastAppliedWitness,
      String termination,
      Rejection lastRejection,
      Failure failure) {
    public Outcome {
      consumedOmens = Map.copyOf(consumedOmens);
      remainingOmens = Set.copyOf(remainingOmens);
    }
  }
}
