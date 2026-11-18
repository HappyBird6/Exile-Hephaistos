package com.poe2craft.craftingsupport.search.domain;

import com.poe2craft.shared.crafting.domain.AbyssCraftingPlan;
import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.crafting.domain.AbyssProbabilityComparison;
import com.poe2craft.shared.crafting.domain.AutomaticCraftingAction;
import com.poe2craft.shared.crafting.domain.AutomaticCraftingPlan;
import com.poe2craft.shared.crafting.domain.AutomaticCurrencyPolicy;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.crafting.domain.CoupledStatRollModel;
import com.poe2craft.shared.crafting.domain.QualityLimitRules;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.BasicJewel;
import com.poe2craft.shared.item.CatalystQuality;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.probability.Fraction;
import java.math.BigInteger;
import java.util.*;

/**
 * 명시된 단일·복합 수치 가정 아래 유한 제작 전이를 계산한다.
 *
 * <p>기존 계약: Finite primitive laws under declared scalar/coupled assumptions, never sampled game
 * probabilities.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code VERSION}: 버전; 초기값 {@code "automatic-finite-kernel-v10-strict-exalted"} ({@code
 *       String}).
 *   <li>{@code policy}: 정책 ({@code AutomaticCurrencyPolicy}).
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code simulator}: 공통 제작 규칙 실행기 ({@code WorkbenchSimulator}).
 *   <li>{@code family}: 옵션계열 ({@code String}).
 *   <li>{@code identity}: 정체성 ({@code Identity}).
 *   <li>{@code terminalRolls}: 목표 판정 구간이 같은 복합 옵션의 실제 굴림 예시를 옵션 ID별로 보관한다.
 *   <li>{@code partition}: 분할 ({@code RollPartition}).
 *   <li>{@code qualityGoal}: 품질·목표 ({@code int}).
 *   <li>{@code tierGroupingSafe}: 티어·Grouping·안전범위 ({@code boolean}).
 *   <li>{@code primitiveNanos}: 개별행동·나노초 ({@code long}).
 *   <li>{@code primitiveCalls}: 개별행동·호출횟수 ({@code long}).
 *   <li>{@code primitiveOutcomes}: 개별행동·결과목록 ({@code long}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code identity()}: 현재 값 또는 규칙의 정체성 식별자를 조회한다. 반환: {@code Identity}.
 *   <li>{@code retainedModifierRoles(item)}: 보유·옵션·Roles 처리에서 {@code item.explicits}, {@code
 *       catalog.modifiers}을(를) 사용한다. 입력: {@code item}(아이템). 반환: {@code List<RetainedModifierRole>}.
 *   <li>{@code modifierTier(id)}: 옵션·티어 처리에서 {@code catalog.modifiers}을(를) 사용한다. 입력: {@code
 *       id}(식별자). 반환: {@code int}.
 *   <li>{@code attributeKey(modifier)}: 속성·키 처리에서 {@code catalog.modifiers}, {@code
 *       modifier.modifierId}을(를) 사용한다. 입력: {@code modifier}(옵션). 반환: {@code Object}.
 *   <li>{@code behaviorKey(item)}: behavior·키 처리에서 {@code item.explicits}, {@code
 *       catalog.modifiers}을(를) 사용한다. 입력: {@code item}(아이템). 반환: {@code Object}.
 *   <li>{@code attributeContext(item)}: 속성·문맥 처리에서 {@code item.explicits}, {@code
 *       Comparator.comparing}을(를) 사용한다. 입력: {@code item}(아이템). 반환: {@code Object}.
 *   <li>{@code withQualityGoal(quality)}: 품질·목표을(를) 반영한 값을 구한다. 입력: {@code quality}(품질). 반환: {@code
 *       AutomaticCraftingKernel}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code qualityGoal()}: {@code qualityGoal} 값을 조회한다. 반환: {@code int}.
 *   <li>{@code workedQuality(item, abstractQuality)}: worked·품질 처리에서 {@code
 *       item.catalystQuality}을(를) 사용한다. 입력: {@code item}(아이템), {@code abstractQuality}(추상화된·품질).
 *       반환: {@code int}.
 *   <li>{@code baseQualitySupported(item)}: 베이스·품질·지원 처리에서 {@code CatalystQuality.supportedBase},
 *       {@code item.baseItemId}을(를) 사용한다. 입력: {@code item}(아이템). 반환: {@code boolean}.
 *   <li>{@code abyssStrategies(item, evaluate, inventory)}: 훼손·전략목록의 입력에 따른 분기·결과를 처리한다. 입력: {@code
 *       item}(아이템), {@code evaluate}(평가), {@code inventory}(등록자료). 반환: {@code
 *       List<AbyssStrategyPlanner.Strategy>}.
 *   <li>{@code abyssInventory(inventory)}: 훼손·등록자료 처리에서 {@code inventory.stream}, {@code
 *       AutomaticCurrencyPolicy.Context}을(를) 사용한다. 입력: {@code inventory}(등록자료). 반환: {@code
 *       List<AutomaticCraftingAction>}.
 *   <li>{@code automaticAbyss(item, evaluate, inventory)}: 자동·훼손 처리에서 {@code action.currency}을(를)
 *       사용한다. 입력: {@code item}(아이템), {@code evaluate}(평가), {@code inventory}(등록자료). 반환: {@code
 *       List<AutomaticAbyssSearch.Route>}.
 *   <li>{@code terminalRollExample(item)}: 종료된 제작의 아이템을 같은 목표 구간의 실제 굴림으로 표현한다.
 *   <li>{@code rollBands(item)}: 수치굴림·Bands 처리에서 {@code item.explicits}, {@code
 *       catalog.modifiers}을(를) 사용한다. 입력: {@code item}(아이템). 반환: {@code Map<String, RollBand>}.
 *   <li>{@code initialChaosOnly()}: 초기값·카오스·제한 값을 조회·구성한다. 반환: {@code boolean}.
 *   <li>{@code mutableAffixCount(item)}: 가변·접두접미옵션·개수 처리에서 {@code item.explicits}, {@code
 *       m.fractured}을(를) 사용한다. 입력: {@code item}(아이템). 반환: {@code long}.
 *   <li>{@code sameFractures(first, second)}: 동일·Fractures 처리에서 {@code first.explicits}, {@code
 *       m.fractured}을(를) 사용한다. 입력: {@code first}(최초), {@code second}(두번째). 반환: {@code boolean}.
 *   <li>{@code invalidInitialChaos(item, currency)}: 유효하지않은·초기값·카오스 처리에서 {@code item.rarity}을(를)
 *       사용한다. 입력: {@code item}(아이템), {@code currency}(화폐). 반환: {@code boolean}.
 *   <li>{@code desecrationActions(item)}: 훼손·행동목록 처리에서 {@code item.desecrated}, {@code
 *       item.rarity}을(를) 사용한다. 입력: {@code item}(아이템). 반환: {@code List<AutomaticCraftingAction>}.
 *   <li>{@code withSelectionComparison(item, action, evaluate)}: 선택·비교을(를) 반영한 값을 구한다. 입력: {@code
 *       item}(아이템), {@code action}(행동), {@code evaluate}(평가). 반환: {@code AutomaticCraftingAction}.
 *   <li>{@code slotPlan(item, action)}: 슬롯·계획 처리에서 {@code simulator.abyssPlan}, {@code
 *       AbyssCraftingState.from}을(를) 사용한다. 입력: {@code item}(아이템), {@code action}(행동). 반환: {@code
 *       AutomaticCraftingPlan}.
 *   <li>{@code strictExaltedAllowed(item, action)}: 엄격파서·엑잘티드·허용된 처리에서 {@code action.currency},
 *       {@code action.purpose}을(를) 사용한다. 입력: {@code item}(아이템), {@code action}(행동). 반환: {@code
 *       boolean}.
 *   <li>{@code plan(item, action)}: 지원 규칙 아래 실행할 분기를 계획한다. 입력: {@code item}(아이템), {@code
 *       action}(행동). 반환: {@code AutomaticCraftingPlan}.
 *   <li>{@code representatives(plan)}: 대표상태목록 처리에서 {@code plan.branches}, {@code
 *       branch.candidates}을(를) 사용한다. 입력: {@code plan}(계획). 반환: {@code List<ItemState>}.
 *   <li>{@code withRollPartition(partition, objective)}: 수치굴림·분할을(를) 반영한 값을 구한다. 입력: {@code
 *       partition}(분할), {@code objective}(목적). 반환: {@code AutomaticCraftingKernel}.
 *   <li>{@code AutomaticCraftingKernel(policy, catalog, simulator, family)}: 기본 정책을 보충해 공통 생성자로
 *       위임한다. 입력: {@code policy}(정책), {@code catalog}(카탈로그), {@code simulator}(제작시뮬레이터), {@code
 *       family}(옵션계열).
 *   <li>{@code AutomaticCraftingKernel(policy, catalog, simulator, family, partition,
 *       partitionIdentity)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code policy}(정책), {@code catalog}(카탈로그),
 *       {@code simulator}(제작시뮬레이터), {@code family}(옵션계열), {@code partition}(분할), {@code
 *       partitionIdentity}(분할·정체성).
 *   <li>{@code expand(state, execution, outcomeBudget)}: 다음 상태 또는 분포 구간을 확장한다. 입력: {@code
 *       state}(상태), {@code execution}(실행), {@code outcomeBudget}(결과·계산예산). 반환: {@code Result}.
 *   <li>{@code expandDeferred(state, tiers, execution, outcomeBudget)}: 확장·지연계산의 입력에 따른 분기·결과를
 *       처리한다. 입력: {@code state}(상태), {@code tiers}(티어목록), {@code execution}(실행), {@code
 *       outcomeBudget}(결과·계산예산). 반환: {@code Result}.
 *   <li>{@code metrics()}: 측정값 값을 조회·구성한다. 반환: {@code Metrics}.
 *   <li>{@code expand(state, retainedTiers, execution, outcomeBudget, deferred, lineage)}: 다음 상태 또는
 *       분포 구간을 확장한다. 입력: {@code state}(상태), {@code retainedTiers}(보유·티어목록), {@code execution}(실행),
 *       {@code outcomeBudget}(결과·계산예산), {@code deferred}(지연계산), {@code lineage}(출처이력). 반환: {@code
 *       Result}.
 *   <li>{@code deferredCursor(state, tiers, action, budget)}: 지연계산·진행커서의 입력에 따른 분기·결과를 처리한다. 입력:
 *       {@code state}(상태), {@code tiers}(티어목록), {@code action}(행동), {@code budget}(계산예산). 반환:
 *       {@code PrimitiveCursor}.
 *   <li>{@code cursor(state, tiers, action, budget, deferred, lineage)}: 진행커서의 입력에 따른 분기·결과를 처리한다.
 *       입력: {@code state}(상태), {@code tiers}(티어목록), {@code action}(행동), {@code budget}(계산예산),
 *       {@code deferred}(지연계산), {@code lineage}(출처이력). 반환: {@code PrimitiveCursor}.
 *   <li>{@code prepareCursor(state, retainedTiers, execution, outcomeBudget, deferred, lineage)}:
 *       진행커서을(를) 준비한다. 입력: {@code state}(상태), {@code retainedTiers}(보유·티어목록), {@code
 *       execution}(실행), {@code outcomeBudget}(결과·계산예산), {@code deferred}(지연계산), {@code
 *       lineage}(출처이력). 반환: {@code PrimitiveCursor}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code flatten(source, expand)}: flatten 처리에서 {@code Collections.emptyIterator}, {@code
 *       current.hasNext}을(를) 사용한다. 입력: {@code source}(출처), {@code expand}(확장). 반환: {@code
 *       Iterator<B>}. 사용하는 예외: {@code NoSuchElementException}.
 *   <li>{@code result(outcomes, boundary, emitted, assumptions, reason)}: 최종 또는 현재 계산 결과를 구성한다. 입력:
 *       {@code outcomes}(결과목록), {@code boundary}(경계), {@code emitted}(내보낸), {@code
 *       assumptions}(계산가정), {@code reason}(이유). 반환: {@code Result}.
 *   <li>{@code blocked(status, reason)}: 차단의 입력에 따른 분기·결과를 처리한다. 입력: {@code status}(상태), {@code
 *       reason}(이유). 반환: {@code Result}.
 *   <li>{@code hasQualityCap(item)}: 품질·상한을(를) 보유 여부를 판정한다. 입력: {@code item}(아이템). 반환: {@code
 *       boolean}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Identity}: {@code AutomaticCraftingKernel}에서 사용하는 정체성의 불변 값이다.
 *   <li>{@code Status}: {@code AutomaticCraftingKernel}의 상태 구분값을 정의한다.
 *   <li>{@code Outcome}: {@code AutomaticCraftingKernel}에서 사용하는 결과의 불변 값이다.
 *   <li>{@code TierState}: {@code AutomaticCraftingKernel}에서 사용하는 티어·상태의 불변 값이다.
 *   <li>{@code Result}: {@code AutomaticCraftingKernel}에서 사용하는 결과의 불변 값이다.
 *   <li>{@code RetainedModifierRole}: {@code AutomaticCraftingKernel}에서 사용하는 보유·옵션·역할의 불변 값이다.
 *   <li>{@code Role}: {@code AutomaticCraftingKernel}에서 사용하는 역할의 불변 값이다.
 *   <li>{@code Behavior}: {@code AutomaticCraftingKernel}에서 사용하는 Behavior의 불변 값이다.
 *   <li>{@code RollBand}: {@code AutomaticCraftingKernel}에서 사용하는 수치굴림·구간의 불변 값이다.
 *   <li>{@code Metrics}: {@code AutomaticCraftingKernel}에서 사용하는 측정값의 불변 값이다.
 *   <li>{@code Generated}: {@code AutomaticCraftingKernel}에서 사용하는 생성된의 불변 값이다.
 *   <li>{@code PrimitiveCursor}: {@code AutomaticCraftingKernel} 내부의 개별행동·진행커서 처리를 맡는다.
 * </ul>
 */
public final class AutomaticCraftingKernel {
  public static final String VERSION = "automatic-finite-kernel-v10-strict-exalted";

  /**
   * {@code AutomaticCraftingKernel}에서 사용하는 정체성의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code kernelVersion}: 전이커널·버전 ({@code String}).
   *   <li>{@code policyVersion}: 정책·버전 ({@code String}).
   *   <li>{@code catalogDigest}: 카탈로그·다이제스트 ({@code String}).
   *   <li>{@code family}: 옵션계열 ({@code String}).
   *   <li>{@code rollPartition}: 수치굴림·분할 ({@code String}).
   *   <li>{@code model}: 모형 ({@code WorkbenchSimulator.AutomaticIdentity}).
   * </ul>
   */
  public record Identity(
      String kernelVersion,
      String policyVersion,
      String catalogDigest,
      String family,
      String rollPartition,
      WorkbenchSimulator.AutomaticIdentity model) {}

  public Identity identity() {
    return identity;
  }

  /**
   * {@code AutomaticCraftingKernel}의 상태 구분값을 정의한다.
   *
   * <p>필드·상수·열거값:
   *
   * <ul>
   *   <li>{@code COMPLETE}: 완료 구분값.
   *   <li>{@code PARTIAL}: 부분 구분값.
   *   <li>{@code UNAVAILABLE}: 사용불가 구분값.
   *   <li>{@code UNSUPPORTED}: 미지원 구분값.
   *   <li>{@code POLICY_REJECTED}: 거절된 구분값.
   * </ul>
   */
  public enum Status {
    COMPLETE,
    PARTIAL,
    UNAVAILABLE,
    UNSUPPORTED,
    POLICY_REJECTED
  }

  /**
   * {@code AutomaticCraftingKernel}에서 사용하는 결과의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code item}: 아이템 ({@code ItemState}).
   *   <li>{@code probability}: 확률 ({@code Fraction}).
   *   <li>{@code tiers}: 티어목록 ({@code Map<String, TierCell>}).
   *   <li>{@code retained}: 보유 ({@code Set<String>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Outcome(item, probability, tiers, retained)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       item}(아이템), {@code probability}(확률), {@code tiers}(티어목록), {@code retained}(보유).
   *   <li>{@code Outcome(item, probability, tiers)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
   *       item}(아이템), {@code probability}(확률), {@code tiers}(티어목록).
   *   <li>{@code Outcome(item, probability)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code item}(아이템),
   *       {@code probability}(확률).
   * </ul>
   */
  public record Outcome(
      ItemState item, Fraction probability, Map<String, TierCell> tiers, Set<String> retained) {
    public Outcome {
      tiers = Map.copyOf(tiers);
      retained = Set.copyOf(retained);
    }

    public Outcome(ItemState item, Fraction probability, Map<String, TierCell> tiers) {
      this(
          item,
          probability,
          tiers,
          item.explicits().stream()
              .map(ModifierInstance::modifierId)
              .collect(java.util.stream.Collectors.toSet()));
    }

    public Outcome(ItemState item, Fraction probability) {
      this(item, probability, Map.of());
    }
  }

  /**
   * {@code AutomaticCraftingKernel}에서 사용하는 티어·상태의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code item}: 아이템 ({@code ItemState}).
   *   <li>{@code tiers}: 티어목록 ({@code Map<String, TierCell>}).
   *   <li>{@code retained}: 보유 ({@code Set<String>}).
   *   <li>{@code lineage}: 출처이력 ({@code boolean}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code TierState(item, tiers, retained, lineage)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
   *       item}(아이템), {@code tiers}(티어목록), {@code retained}(보유), {@code lineage}(출처이력).
   * </ul>
   */
  private record TierState(
      ItemState item, Map<String, TierCell> tiers, Set<String> retained, boolean lineage) {
    TierState {
      tiers = Map.copyOf(tiers);
    }
  }

  /**
   * {@code AutomaticCraftingKernel}에서 사용하는 결과의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code status}: 현재 처리 상태 ({@code Status}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code outcomes}: 결과목록 ({@code List<Outcome>}).
   *   <li>{@code boundary}: 경계 ({@code Fraction}).
   *   <li>{@code unresolved}: 미해결 ({@code Fraction}).
   *   <li>{@code assumptions}: 확률·수치 계산에 사용한 명시적 가정 ({@code List<WorkbenchSimulator.Assumption>}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Result(status, reason, outcomes, boundary, unresolved, assumptions)}: 전달받은 값으로
   *       구성원을 초기화한다. 입력: {@code status}(상태), {@code reason}(이유), {@code outcomes}(결과목록), {@code
   *       boundary}(경계), {@code unresolved}(미해결), {@code assumptions}(계산가정). 사용하는 예외: {@code
   *       IllegalArgumentException}.
   * </ul>
   */
  public record Result(
      Status status,
      String reason,
      List<Outcome> outcomes,
      Fraction boundary,
      Fraction unresolved,
      List<WorkbenchSimulator.Assumption> assumptions) {
    public Result {
      outcomes = List.copyOf(outcomes);
      assumptions = List.copyOf(assumptions);
      var total = boundary.add(unresolved);
      for (var outcome : outcomes) total = total.add(outcome.probability());
      if (!total.equals(Fraction.ONE))
        throw new IllegalArgumentException("Primitive mass must equal one");
    }
  }

  private final AutomaticCurrencyPolicy policy;
  private final ItemCatalog catalog;
  private final WorkbenchSimulator simulator;
  private final String family;
  private final Identity identity;
  // 작업의 목표 경계별로 복합 옵션의 실제 표시용 굴림을 재사용한다.
  private final Map<String, Map<ModifierInstance, ModifierInstance>> terminalRolls =
      new HashMap<>();
  private final RollPartition partition;
  private int qualityGoal;
  private final boolean tierGroupingSafe;

  /**
   * {@code AutomaticCraftingKernel}에서 사용하는 보유·옵션·역할의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code side}: 접두접미 ({@code ModifierDefinition.AffixType}).
   *   <li>{@code families}: 옵션계열목록 ({@code Set<String>}).
   *   <li>{@code fractured}: 분열고정 ({@code boolean}).
   * </ul>
   */
  public record RetainedModifierRole(
      ModifierDefinition.AffixType side, Set<String> families, boolean fractured) {}

  /** Tier-independent identity used only after a caller proves every future transition. */
  public List<RetainedModifierRole> retainedModifierRoles(ItemState item) {
    return item.explicits().stream()
        .map(
            m -> {
              var definition = Objects.requireNonNull(catalog.modifiers().get(m.modifierId()));
              return new RetainedModifierRole(
                  definition.affixType(), Set.copyOf(definition.familyIds()), m.fractured());
            })
        .sorted(
            Comparator.comparing((RetainedModifierRole r) -> r.side().name())
                .thenComparing(r -> new TreeSet<>(r.families()).toString())
                .thenComparing(RetainedModifierRole::fractured))
        .toList();
  }

  /**
   * {@code AutomaticCraftingKernel}에서 사용하는 역할의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code attribute}: 속성 ({@code Object}).
   *   <li>{@code tags}: 태그목록 ({@code Set<String>}).
   *   <li>{@code region}: 영역 ({@code Object}).
   * </ul>
   */
  private record Role(Object attribute, Set<String> tags, Object region) {}

  /**
   * {@code AutomaticCraftingKernel}에서 사용하는 Behavior의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code context}: 문맥 ({@code ItemState}).
   *   <li>{@code roles}: roles ({@code List<Role>}).
   * </ul>
   */
  private record Behavior(ItemState context, List<Role> roles) {}

  public int modifierTier(String id) {
    return Objects.requireNonNull(catalog.modifiers().get(id)).tier();
  }

  public Object attributeKey(ModifierInstance modifier) {
    var definition = Objects.requireNonNull(catalog.modifiers().get(modifier.modifierId()));
    return List.of(
        definition.affixType(),
        new TreeSet<>(definition.familyIds()),
        new TreeSet<>(modifier.values().keySet()),
        modifier.fractured());
  }

  /** 종료된 제작의 복합 옵션은 목표 경계가 같은 실제 결합 굴림 하나로 표시한다. */
  ItemState terminalRollExample(ItemState item) {
    var explicits = new ArrayList<ModifierInstance>();
    for (var modifier : item.explicits()) {
      if (modifier.values().size() < 2 || modifier.fractured()) {
        explicits.add(modifier);
        continue;
      }
      var representative =
          terminalRolls
              .computeIfAbsent(
                  modifier.modifierId(),
                  id -> {
                    var examples = new HashMap<Map<String, Object>, ModifierInstance>();
                    var result = new HashMap<ModifierInstance, ModifierInstance>();
                    simulator
                        .automaticJointRolls(catalog.modifiers().get(id))
                        .ifPresent(
                            cells -> {
                              for (var cell : cells) {
                                var regions = new TreeMap<String, Object>();
                                cell.modifier()
                                    .values()
                                    .forEach(
                                        (stat, value) ->
                                            regions.put(
                                                stat, partition.automaticRegion(stat, value)));
                                result.put(
                                    cell.modifier(),
                                    examples.computeIfAbsent(regions, ignored -> cell.modifier()));
                              }
                            });
                    return result;
                  })
              .getOrDefault(modifier, modifier);
      explicits.add(representative);
    }
    return explicits.equals(item.explicits())
        ? item
        : simulator.finishBasicCurrency(item, explicits);
  }

  public Object behaviorKey(ItemState item) {
    if (!tierGroupingSafe) return item;
    var roles =
        item.explicits().stream()
            .map(
                m -> {
                  var definition = Objects.requireNonNull(catalog.modifiers().get(m.modifierId()));
                  var regions = new TreeMap<String, Object>();
                  m.values()
                      .forEach(
                          (stat, value) ->
                              regions.put(stat, partition.automaticRegion(stat, value)));
                  return new Role(attributeKey(m), definition.tags(), regions);
                })
            .sorted(Comparator.comparing(Object::toString))
            .toList();
    var context =
        new ItemState(
                item.snapshotId(),
                item.baseItemId(),
                item.itemLevel(),
                item.rarity(),
                item.implicits(),
                List.of(),
                item.conditions(),
                item.augmentSockets(),
                item.catalystQuality(),
                item.desecrated())
            .withAugments(item.installedAugments())
            .withQualityInfused(item.qualityInfused());
    return new Behavior(context, roles);
  }

  public Object attributeContext(ItemState item) {
    var roles =
        item.explicits().stream()
            .map(this::attributeKey)
            .sorted(Comparator.comparing(Object::toString))
            .toList();
    var context =
        new ItemState(
                item.snapshotId(),
                item.baseItemId(),
                item.itemLevel(),
                item.rarity(),
                item.implicits(),
                List.of(),
                item.conditions(),
                item.augmentSockets(),
                item.catalystQuality(),
                item.desecrated())
            .withAugments(item.installedAugments())
            .withQualityInfused(item.qualityInfused());
    return List.of(context, roles);
  }

  public AutomaticCraftingKernel withQualityGoal(int quality) {
    if (quality < 0 || quality > 40) throw new IllegalArgumentException("Invalid quality goal");
    var configured =
        new AutomaticCraftingKernel(
            policy, catalog, simulator, family, partition, identity.rollPartition());
    configured.qualityGoal = quality;
    return configured;
  }

  public int qualityGoal() {
    return qualityGoal;
  }

  public int workedQuality(ItemState item, int abstractQuality) {
    return Math.max(
        abstractQuality, item.catalystQuality() == null ? 0 : item.catalystQuality().amount());
  }

  public boolean baseQualitySupported(ItemState item) {
    return CatalystQuality.supportedBase(item.baseItemId());
  }

  public List<AbyssStrategyPlanner.Strategy> abyssStrategies(
      ItemState item,
      java.util.function.Function<ItemState, AutomaticMethodGraph.Progress> evaluate,
      List<AutomaticCraftingAction> inventory) {
    var permitted = abyssInventory(inventory);
    return new AbyssStrategyPlanner(catalog, simulator, evaluate).recommend(item, permitted);
  }

  private List<AutomaticCraftingAction> abyssInventory(List<AutomaticCraftingAction> inventory) {
    return inventory.stream()
        .filter(
            action -> {
              var context =
                  new AutomaticCurrencyPolicy.Context(family, action.purpose(), action.currency());
              return policy.evaluate(action.currency().name(), context)
                      == AutomaticCurrencyPolicy.Verdict.PERMITTED
                  && action.omens().stream()
                      .allMatch(
                          omen ->
                              policy.evaluate("OMEN:" + omen.name(), context)
                                  == AutomaticCurrencyPolicy.Verdict.PERMITTED);
            })
        .toList();
  }

  public List<AutomaticAbyssSearch.Route> automaticAbyss(
      ItemState item,
      java.util.function.Function<ItemState, AutomaticMethodGraph.Progress> evaluate,
      List<AutomaticCraftingAction> inventory) {
    // Desecration is a preparation step; it must not reopen the initial Chaos stage.
    var permitted =
        abyssInventory(inventory).stream()
            .filter(action -> action.currency() != WorkbenchCurrency.CHAOS)
            .toList();
    return new AutomaticAbyssSearch(catalog, simulator, permitted)
        .expand(new AbyssStrategyPlanner(catalog, simulator, evaluate).recommend(item, permitted));
  }

  /**
   * {@code AutomaticCraftingKernel}에서 사용하는 수치굴림·구간의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code stat}: 수치 ({@code String}).
   *   <li>{@code min}: 허용·표시 범위의 하한 ({@code long}).
   *   <li>{@code max}: 허용·표시 범위의 상한 ({@code long}).
   * </ul>
   */
  public record RollBand(String stat, long min, long max) {}

  public Map<String, RollBand> rollBands(ItemState item) {
    var result = new LinkedHashMap<String, RollBand>();
    for (var modifier : item.explicits()) {
      var definition = catalog.modifiers().get(modifier.modifierId());
      if (definition == null || definition.stats().size() != 1) continue;
      var stat = definition.stats().getFirst().id();
      var value = modifier.values().get(stat);
      if (value == null) continue;
      long end = partition.end(definition, value);
      if (end > value) result.put(modifier.modifierId(), new RollBand(stat, value, end));
    }
    return Map.copyOf(result);
  }

  public boolean initialChaosOnly() {
    return policy.document().entries().stream()
        .anyMatch(
            e ->
                e.id().equals("CHAOS")
                    && e.condition() == AutomaticCurrencyPolicy.Condition.FIRST_AFFIX_ACQUISITION);
  }

  public static long mutableAffixCount(ItemState item) {
    return item.explicits().stream().filter(m -> !m.fractured()).count()
        + item.desecrated().stream().filter(s -> s.modifierId() == null).count();
  }

  public static boolean sameFractures(ItemState first, ItemState second) {
    return new HashSet<>(first.explicits().stream().filter(m -> m.fractured()).toList())
        .equals(new HashSet<>(second.explicits().stream().filter(m -> m.fractured()).toList()));
  }

  private boolean invalidInitialChaos(ItemState item, WorkbenchCurrency currency) {
    return initialChaosOnly()
        && currency == WorkbenchCurrency.CHAOS
        && (item.rarity() != ItemState.Rarity.RARE || mutableAffixCount(item) != 1);
  }

  /** Eligibility and prerequisite projection, before allocating a numeric probability law. */
  public List<AutomaticCraftingAction> desecrationActions(ItemState item) {
    if (!item.desecrated().isEmpty()
        || item.rarity() != ItemState.Rarity.RARE
        || item.explicitSlotCount() >= 6
        || !item.conditions().isEmpty()
        || BasicJewel.supportedCrafting(item.baseItemId())
        || item.catalystQuality() != null) return List.of();
    var slots = new ItemStateValidator(catalog).slots(item);
    var result = new ArrayList<AutomaticCraftingAction>();
    if (slots.remainingPrefixes() > 0)
      result.add(AutomaticCraftingAction.desecrate(ModifierDefinition.AffixType.PREFIX));
    if (slots.remainingSuffixes() > 0)
      result.add(AutomaticCraftingAction.desecrate(ModifierDefinition.AffixType.SUFFIX));
    return List.copyOf(result);
  }

  /** Conditional next-modifier sensitivity, not a distribution over reveal offers. */
  public AutomaticCraftingAction withSelectionComparison(
      ItemState item,
      AutomaticCraftingAction action,
      java.util.function.Function<ItemState, AutomaticMethodGraph.Progress> evaluate) {
    if (action.currency() != WorkbenchCurrency.EXALTED || !action.omens().isEmpty()) return action;
    var source = AbyssCraftingState.from(item, catalog);
    var slot =
        source.slots().stream().filter(s -> s.desecrated() && s.modifier() == null).findFirst();
    if (slot.isEmpty()) return action;
    var comparison = new AbyssProbabilityComparison(catalog);
    var before = evaluate.apply(item);
    var targets = new HashSet<String>();
    for (var candidate : comparison.pool(source, action.currency(), null)) {
      var values = new HashMap<String, Long>();
      candidate.stats().forEach(stat -> values.put(stat.id(), stat.max()));
      var after =
          evaluate.apply(
              source
                  .append("comparison-probe", new ModifierInstance(candidate.id(), values), catalog)
                  .knownItem());
      if (GoalDirectedPlanner.matched(after) > GoalDirectedPlanner.matched(before))
        targets.add(candidate.id());
    }
    if (targets.isEmpty()) return action;
    var result = comparison.compare(source, slot.get().id(), action.currency(), null, targets);
    return result
        .map(
            c ->
                new AutomaticCraftingAction(
                    action.currency(), action.omens(), action.purpose(), null, null, c))
        .orElse(action);
  }

  private AutomaticCraftingPlan slotPlan(ItemState item, AutomaticCraftingAction action) {
    var p =
        simulator.abyssPlan(
            AbyssCraftingState.from(item, catalog), action.currency(), action.omenIds());
    if (!p.available())
      return new AutomaticCraftingPlan(
          AutomaticCraftingPlan.Status.UNAVAILABLE, p.reason(), List.of(), false, "");
    return new AutomaticCraftingPlan(
        AutomaticCraftingPlan.Status.READY,
        "",
        p.branches().stream()
            .map(
                b ->
                    new AutomaticCraftingPlan.Branch(
                        b.remaining().storedItem(), b.candidates(), b.probabilityDenominator()))
            .toList(),
        p.branches().stream().allMatch(AbyssCraftingPlan.Branch::uniformSelection),
        "");
  }

  private boolean strictExaltedAllowed(ItemState item, AutomaticCraftingAction action) {
    if (action.currency() != WorkbenchCurrency.EXALTED) return true;
    if (action.purpose() == AutomaticCurrencyPolicy.Purpose.REFILL_AFTER_ANNULMENT
        || action.purpose() == AutomaticCurrencyPolicy.Purpose.LAST_RESORT_EXALTED)
      return action.omens().isEmpty();
    if (action.purpose() == AutomaticCurrencyPolicy.Purpose.GOAL_ACQUISITION)
      return mutableAffixCount(item) == 0 && action.omens().isEmpty();
    return action.purpose() == AutomaticCurrencyPolicy.Purpose.TEMPORARY_AFFIX_FOR_REMOVAL
        && (action.omens().equals(Set.of(WorkbenchOmen.SINISTRAL_EXALTATION))
            || action.omens().equals(Set.of(WorkbenchOmen.DEXTRAL_EXALTATION))
            || (mutableAffixCount(item) == 0 && action.omens().isEmpty()));
  }

  public AutomaticCraftingPlan plan(ItemState item, AutomaticCraftingAction action) {
    if (!strictExaltedAllowed(item, action))
      return new AutomaticCraftingPlan(
          AutomaticCraftingPlan.Status.UNAVAILABLE,
          "EXALTED_REQUIRES_FIRST_AFFIX_OR_PLANNED_REMOVAL",
          List.of(),
          false,
          "");
    if (action.operation() != null)
      return new AutomaticCraftingPlan(
          desecrationActions(item).contains(action)
              ? AutomaticCraftingPlan.Status.READY
              : AutomaticCraftingPlan.Status.UNAVAILABLE,
          "",
          List.of(),
          false,
          "");
    if (action.purpose() == AutomaticCurrencyPolicy.Purpose.QUALITY_BASE_CHECKPOINT)
      return new AutomaticCraftingPlan(
          AutomaticCraftingPlan.Status.UNAVAILABLE, "ABSTRACT_QUALITY_WORK", List.of(), false, "");
    var context = new AutomaticCurrencyPolicy.Context(family, action.purpose(), action.currency());
    if (invalidInitialChaos(item, action.currency())
        || policy.evaluate(action.currency().name(), context)
            != AutomaticCurrencyPolicy.Verdict.PERMITTED
        || action.omens().stream()
            .anyMatch(
                o ->
                    policy.evaluate("OMEN:" + o.name(), context)
                        != AutomaticCurrencyPolicy.Verdict.PERMITTED))
      return new AutomaticCraftingPlan(
          AutomaticCraftingPlan.Status.UNAVAILABLE, "POLICY_REJECTED", List.of(), false, "");
    return item.desecrated().isEmpty()
        ? simulator.automaticPlan(item, action)
        : slotPlan(item, action);
  }

  /**
   * Every scalar goal cell or declared joint tuple is considered, without selection probability.
   */
  public List<ItemState> representatives(AutomaticCraftingPlan plan) {
    var result = new ArrayList<ItemState>();
    for (var branch : plan.branches())
      for (var candidate : branch.candidates()) {
        if (candidate.stats().size() != 1) {
          for (var cell : simulator.automaticJointRolls(candidate).orElse(List.of())) {
            var explicits = new ArrayList<>(branch.remaining().explicits());
            explicits.add(cell.modifier());
            if (policy.withinExplorationBoundary(explicits.size()))
              result.add(simulator.finishBasicCurrency(branch.remaining(), explicits));
          }
          continue;
        }
        var range = candidate.stats().getFirst();
        for (long value = range.min(); ; ) {
          var explicits = new ArrayList<>(branch.remaining().explicits());
          explicits.add(new ModifierInstance(candidate.id(), Map.of(range.id(), value)));
          if (policy.withinExplorationBoundary(explicits.size()))
            result.add(simulator.finishBasicCurrency(branch.remaining(), explicits));
          long end = partition.end(candidate, value);
          if (end >= range.max()) break;
          value = end + 1;
        }
      }
    return List.copyOf(result);
  }

  public AutomaticCraftingKernel withRollPartition(RollPartition partition, String objective) {
    return new AutomaticCraftingKernel(
        policy, catalog, simulator, family, partition, RollPartition.VERSION + ":" + objective);
  }

  public AutomaticCraftingKernel(
      AutomaticCurrencyPolicy policy,
      ItemCatalog catalog,
      WorkbenchSimulator simulator,
      String family) {
    this(policy, catalog, simulator, family, RollPartition.exact(), "exact");
  }

  private AutomaticCraftingKernel(
      AutomaticCurrencyPolicy policy,
      ItemCatalog catalog,
      WorkbenchSimulator simulator,
      String family,
      RollPartition partition,
      String partitionIdentity) {
    this.policy = Objects.requireNonNull(policy);
    tierGroupingSafe =
        AutomaticCraftingAction.candidates(policy).stream()
            .noneMatch(a -> a.omens().contains(WorkbenchOmen.WHITTLING));
    this.catalog = Objects.requireNonNull(catalog);
    this.simulator = Objects.requireNonNull(simulator);
    this.family = Objects.requireNonNull(family);
    this.partition = Objects.requireNonNull(partition);
    identity =
        new Identity(
            VERSION,
            policy.document().policyVersion(),
            BasicCurrencyState.catalogDigest(catalog),
            family,
            partitionIdentity,
            simulator.automaticIdentity());
  }

  public Result expand(ItemState state, AutomaticCraftingAction execution, long outcomeBudget) {
    return expand(state, Map.of(), execution, outcomeBudget, false, false);
  }

  /** Keep each conditional tier law attached to its state, including through removal. */
  public Result expandDeferred(
      ItemState state,
      Map<String, TierCell> tiers,
      AutomaticCraftingAction execution,
      long outcomeBudget) {
    return expand(state, tiers, execution, outcomeBudget, tierGroupingSafe, true);
  }

  private long primitiveNanos, primitiveCalls, primitiveOutcomes;

  /**
   * {@code AutomaticCraftingKernel}에서 사용하는 측정값의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code nanos}: 나노초 ({@code long}).
   *   <li>{@code calls}: 호출횟수 ({@code long}).
   *   <li>{@code outcomes}: 결과목록 ({@code long}).
   * </ul>
   */
  public record Metrics(long nanos, long calls, long outcomes) {}

  public Metrics metrics() {
    return new Metrics(primitiveNanos, primitiveCalls, primitiveOutcomes);
  }

  private Result expand(
      ItemState state,
      Map<String, TierCell> retainedTiers,
      AutomaticCraftingAction execution,
      long outcomeBudget,
      boolean deferred,
      boolean lineage) {
    var cursor = cursor(state, retainedTiers, execution, outcomeBudget, deferred, lineage);
    while (!cursor.finished() && !cursor.limited()) cursor.advance(256);
    return cursor.result();
  }

  public PrimitiveCursor deferredCursor(
      ItemState state, Map<String, TierCell> tiers, AutomaticCraftingAction action, long budget) {
    return cursor(state, tiers, action, budget, tierGroupingSafe, true);
  }

  private PrimitiveCursor cursor(
      ItemState state,
      Map<String, TierCell> tiers,
      AutomaticCraftingAction action,
      long budget,
      boolean deferred,
      boolean lineage) {
    long start = System.nanoTime();
    try {
      return prepareCursor(state, tiers, action, budget, deferred, lineage);
    } finally {
      primitiveCalls++;
      primitiveNanos += System.nanoTime() - start;
    }
  }

  private PrimitiveCursor prepareCursor(
      ItemState state,
      Map<String, TierCell> retainedTiers,
      AutomaticCraftingAction execution,
      long outcomeBudget,
      boolean deferred,
      boolean lineage) {
    if (outcomeBudget < 1) throw new IllegalArgumentException("Positive outcome budget required");
    if (!strictExaltedAllowed(state, execution))
      return new PrimitiveCursor(
          blocked(Status.POLICY_REJECTED, "EXALTED_REQUIRES_FIRST_AFFIX_OR_PLANNED_REMOVAL"));
    if (execution.operation() != null) {
      if (!desecrationActions(state).contains(execution))
        return new PrimitiveCursor(blocked(Status.UNAVAILABLE, "DESECRATION_UNAVAILABLE"));
      var target =
          AbyssCraftingState.from(state, catalog)
              .addUnrevealed("desecrated-" + execution.side().name(), execution.side(), catalog)
              .storedItem();
      return new PrimitiveCursor(
          new Result(
              Status.COMPLETE,
              "",
              List.of(new Outcome(target, Fraction.ONE, retainedTiers)),
              Fraction.ZERO,
              Fraction.ZERO,
              List.of()));
    }
    if (execution.purpose() == AutomaticCurrencyPolicy.Purpose.QUALITY_BASE_CHECKPOINT) {
      if (execution.currency() != WorkbenchCurrency.ESSENCE_BREACH
          || !execution.omens().isEmpty()
          || !baseQualitySupported(state))
        return new PrimitiveCursor(blocked(Status.UNAVAILABLE, "BASE_QUALITY_NOT_VERIFIED"));
      return new PrimitiveCursor(
          new Result(
              Status.COMPLETE,
              "",
              List.of(new Outcome(state, Fraction.ONE, retainedTiers)),
              Fraction.ZERO,
              Fraction.ZERO,
              List.of()));
    }
    var context =
        new AutomaticCurrencyPolicy.Context(family, execution.purpose(), execution.currency());
    if (invalidInitialChaos(state, execution.currency())
        || policy.evaluate(execution.currency().name(), context)
            != AutomaticCurrencyPolicy.Verdict.PERMITTED
        || execution.omens().stream()
            .anyMatch(
                o ->
                    policy.evaluate("OMEN:" + o.name(), context)
                        != AutomaticCurrencyPolicy.Verdict.PERMITTED))
      return new PrimitiveCursor(blocked(Status.POLICY_REJECTED, "AUTOMATIC_POLICY_REJECTED"));
    if (!policy.withinExplorationBoundary(state.explicitSlotCount()))
      return new PrimitiveCursor(
          new Result(
              Status.PARTIAL,
              "EXPLICIT_MODIFIER_BOUNDARY",
              List.of(),
              Fraction.ONE,
              Fraction.ZERO,
              List.of()));
    if (execution.purpose() == AutomaticCurrencyPolicy.Purpose.QUALITY_CHECKPOINT) {
      if (execution.currency() != WorkbenchCurrency.ESSENCE_BREACH || !execution.omens().isEmpty())
        return new PrimitiveCursor(blocked(Status.POLICY_REJECTED, "INVALID_QUALITY_WORK"));
      if (!hasQualityCap(state))
        return new PrimitiveCursor(
            blocked(Status.UNAVAILABLE, "QUALITY_40_REQUIRES_MAXIMUM_QUALITY_MODIFIER"));
      var limit = QualityLimitRules.describe(state, catalog);
      if (limit == null || limit.maximumQuality() < 40)
        return new PrimitiveCursor(blocked(Status.UNSUPPORTED, "QUALITY_40_CAP_NOT_VERIFIED"));
      return new PrimitiveCursor(
          new Result(
              Status.COMPLETE,
              "",
              List.of(new Outcome(state, Fraction.ONE, retainedTiers)),
              Fraction.ZERO,
              Fraction.ZERO,
              List.of(
                  new WorkbenchSimulator.Assumption(
                      "abstract-quality-work-v1",
                      "quality checkpoint",
                      1,
                      List.of(),
                      null,
                      null,
                      "https://poe2db.tw/us/Essence_of_the_Breach",
                      "User-approved abstract work to 40 quality; catalyst type, count and numeric scaling are not inferred."))));
    }
    var plan =
        state.desecrated().isEmpty()
            ? simulator.automaticPlan(state, execution)
            : slotPlan(state, execution);
    if (plan.status() != AutomaticCraftingPlan.Status.READY)
      return new PrimitiveCursor(
          blocked(
              plan.status() == AutomaticCraftingPlan.Status.UNAVAILABLE
                  ? Status.UNAVAILABLE
                  : Status.UNSUPPORTED,
              plan.reason()));
    if (execution.currency() == WorkbenchCurrency.ANNULMENT) {
      var mass = Fraction.of(1, plan.branches().size());
      var outcomes =
          plan.branches().stream()
              .map(
                  branch -> {
                    var kept = new HashMap<>(retainedTiers);
                    kept.keySet()
                        .retainAll(
                            branch.remaining().explicits().stream()
                                .map(ModifierInstance::modifierId)
                                .toList());
                    return new Outcome(branch.remaining(), mass, kept);
                  })
              .toList();
      return new PrimitiveCursor(
          new Result(Status.COMPLETE, "", outcomes, Fraction.ZERO, Fraction.ZERO, List.of()));
    }
    boolean uniform =
        plan.uniformCandidates()
            || Set.of(
                    "UNVERIFIED_GAME_WEIGHTS_EXPLICIT_UNIFORM_ELIGIBLE_CANDIDATES",
                    "USER_APPROVED_UNIFORM_CANDIDATES_NOT_GAME_WEIGHTS")
                .contains(catalog.metadata().weightPolicy());
    if (!uniform && !catalog.metadata().weightPolicy().equals("POE2DB_AS_PUBLISHED"))
      return new PrimitiveCursor(
          blocked(Status.UNSUPPORTED, "SELECTION_WEIGHT_MODEL_NOT_IMPLEMENTED"));
    // Unsupported rolls retain their selection mass in the same eligible denominator.
    String unsupportedRoll = "";
    for (var branch : plan.branches()) {
      if (branch.candidates().isEmpty())
        return new PrimitiveCursor(blocked(Status.UNSUPPORTED, "EMPTY_CANDIDATE_LAW"));
      for (var candidate : branch.candidates()) {
        if (candidate.stats().size() != 1) {
          if (simulator.automaticJointRolls(candidate).isEmpty())
            unsupportedRoll = "JOINT_ROLL_MODEL_NOT_IMPLEMENTED";
          continue;
        }
        var range = candidate.stats().getFirst();
        if (BigInteger.valueOf(range.max())
                .subtract(BigInteger.valueOf(range.min()))
                .add(BigInteger.ONE)
                .compareTo(BigInteger.valueOf(Long.MAX_VALUE))
            > 0) unsupportedRoll = "ROLL_DOMAIN_NOT_IMPLEMENTED";
      }
    }
    if (plan.branches().isEmpty())
      return new PrimitiveCursor(blocked(Status.UNAVAILABLE, "NO_REMOVAL_CANDIDATE"));
    for (var branch : plan.branches()) {
      long total = 0;
      for (var candidate : branch.candidates())
        total = Math.addExact(total, uniform ? 1 : candidate.weight());
      if (total <= 0)
        return new PrimitiveCursor(blocked(Status.UNSUPPORTED, "INVALID_SELECTION_WEIGHT"));
    }
    return new PrimitiveCursor(
        plan, retainedTiers, execution, outcomeBudget, deferred, lineage, uniform, unsupportedRoll);
  }

  /**
   * {@code AutomaticCraftingKernel}에서 사용하는 생성된의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code TierState}).
   *   <li>{@code mass}: 확률질량 ({@code Fraction}).
   * </ul>
   */
  private record Generated(TierState state, Fraction mass) {}

  // Stream.flatMap().iterator() may buffer a whole inner stream on hasNext().
  // This explicit concatenation keeps at most one result group in look-ahead.
  private static <A, B> Iterator<B> flatten(
      Iterator<A> source, java.util.function.Function<A, Iterator<B>> expand) {
    return new Iterator<>() {
      Iterator<B> current = Collections.emptyIterator();

      public boolean hasNext() {
        while (!current.hasNext() && source.hasNext()) current = expand.apply(source.next());
        return current.hasNext();
      }

      public B next() {
        if (!hasNext()) throw new NoSuchElementException();
        return current.next();
      }
    };
  }

  /**
   * {@code AutomaticCraftingKernel} 내부의 개별행동·진행커서 처리를 맡는다.
   *
   * <p>기존 계약: A suspended primitive retains its exact remaining mass; the runner charges it only
   * once.
   *
   * <p>필드·상수:
   *
   * <ul>
   *   <li>{@code outcomes}: 결과목록 ({@code Map<TierState, Fraction>}).
   *   <li>{@code assumptions}: 확률·수치 계산에 사용한 명시적 가정 ({@code Set<WorkbenchSimulator.Assumption>}).
   *   <li>{@code boundary}: 경계 ({@code Fraction}).
   *   <li>{@code emitted}: 내보낸 ({@code Fraction}).
   *   <li>{@code budget}: 계산예산 ({@code long}).
   *   <li>{@code count}: 개수 ({@code long}).
   *   <li>{@code generatedCount}: 생성된·개수 ({@code long}).
   *   <li>{@code iterator}: iterator ({@code Iterator<Generated>}).
   *   <li>{@code unsupportedRoll}: 미지원·수치굴림 ({@code String}).
   *   <li>{@code completed}: 완료 ({@code Result}).
   *   <li>{@code limited}: 제한된 ({@code boolean}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code PrimitiveCursor(result)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code result}(결과).
   *   <li>{@code PrimitiveCursor(plan, retainedTiers, execution, budget, deferred, lineage,
   *       uniform, unsupportedRoll)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code plan}(계획), {@code
   *       retainedTiers}(보유·티어목록), {@code execution}(실행), {@code budget}(계산예산), {@code
   *       deferred}(지연계산), {@code lineage}(출처이력), {@code uniform}(균등), {@code
   *       unsupportedRoll}(미지원·수치굴림). 사용하는 예외: {@code IllegalArgumentException}, {@code
   *       NoSuchElementException}.
   *   <li>{@code generated(state, mass)}: 생성된의 입력에 따른 분기·결과를 처리한다. 입력: {@code state}(상태), {@code
   *       mass}(확률질량). 반환: {@code Generated}.
   *   <li>{@code advance(allowance)}: 허용된 계산량만큼 작업을 진행한다. 입력: {@code allowance}(allowance). 사용하는
   *       예외: {@code IllegalArgumentException}.
   *   <li>{@code finished()}: 종료 값을 조회·구성한다. 반환: {@code boolean}.
   *   <li>{@code limited()}: {@code limited} 값을 조회한다. 반환: {@code boolean}.
   *   <li>{@code retainedStates()}: 보유·상태목록 값을 조회·구성한다. 반환: {@code long}.
   *   <li>{@code result()}: 최종 또는 현재 계산 결과를 구성한다. 반환: {@code Result}.
   * </ul>
   */
  public final class PrimitiveCursor {
    private final Map<TierState, Fraction> outcomes = new LinkedHashMap<>();
    private final Set<WorkbenchSimulator.Assumption> assumptions = new LinkedHashSet<>();
    private Fraction boundary = Fraction.ZERO, emitted = Fraction.ZERO;
    private final long budget;
    private long count, generatedCount;
    private final Iterator<Generated> iterator;
    private final String unsupportedRoll;
    private Result completed;
    private boolean limited;

    private PrimitiveCursor(Result result) {
      completed = result;
      iterator = Collections.emptyIterator();
      budget = 0;
      unsupportedRoll = "";
      primitiveOutcomes += result.outcomes().size();
    }

    private PrimitiveCursor(
        AutomaticCraftingPlan plan,
        Map<String, TierCell> retainedTiers,
        AutomaticCraftingAction execution,
        long budget,
        boolean deferred,
        boolean lineage,
        boolean uniform,
        String unsupportedRoll) {
      this.budget = budget;
      this.unsupportedRoll = unsupportedRoll;
      if (execution.omens().contains(WorkbenchOmen.WHITTLING)) {
        var source =
            policy.document().entries().stream()
                .filter(e -> e.id().equals("OMEN:WHITTLING"))
                .findFirst()
                .orElseThrow();
        assumptions.add(
            new WorkbenchSimulator.Assumption(
                "declared-whittling-priority-v1",
                "eligible lowest modifier-level removal",
                plan.branches().size(),
                List.of(),
                null,
                null,
                source.sourceUrl(),
                "Lowest required modifier level after any affix-side restriction. Fractured combinations remain unsupported. Side+Whittling order is a declared composition, not independently established game odds."));
      }
      boolean targetFirst = plan.branches().stream().anyMatch(b -> b.probabilityDenominator() > 0);
      if (plan.branches().size() > 1)
        assumptions.add(
            new WorkbenchSimulator.Assumption(
                targetFirst ? "target-first-replacement-v1" : "uniform-removal-v1",
                targetFirst
                    ? "outcome and conditional removal branch"
                    : "eligible explicit instance",
                plan.branches().size(),
                List.of(),
                null,
                null,
                plan.sourceUrl(),
                "Uniform outcome selection followed by uniform eligible removal for essence replacements; other operations use uniform eligible removal. No published removal weights."));
      iterator =
          flatten(
              plan.branches().iterator(),
              branch -> {
                var branchMass =
                    Fraction.of(
                        1,
                        branch.probabilityDenominator() > 0
                            ? branch.probabilityDenominator()
                            : plan.branches().size());
                var emittedCells = new HashSet<TierCell>();
                var posterior =
                    deferred
                        ? new AutomaticTierPartition(partition)
                            .members(branch.candidates(), uniform, budget)
                        : Map.<TierPartition.BandKey, TierPartition.Member>of();
                var candidateIds =
                    branch.candidates().stream()
                        .map(ModifierDefinition::id)
                        .collect(java.util.stream.Collectors.toSet());
                var retainedIds =
                    branch.remaining().explicits().stream()
                        .map(ModifierInstance::modifierId)
                        .collect(java.util.stream.Collectors.toSet());
                var keptTiers = new HashMap<>(retainedTiers);
                keptTiers.keySet().retainAll(retainedIds);
                long sum = 0;
                for (var candidate : branch.candidates())
                  sum = Math.addExact(sum, uniform ? 1 : candidate.weight());
                final long total = sum;
                if (total <= 0) throw new IllegalArgumentException("Invalid selection weight");
                if (uniform)
                  assumptions.add(
                      new WorkbenchSimulator.Assumption(
                          "uniform-individual-candidate-v1",
                          "eligible individual modifier tier",
                          branch.candidates().size(),
                          branch.candidates().stream().map(ModifierDefinition::id).toList(),
                          null,
                          null,
                          plan.sourceUrl(),
                          plan.uniformCandidates()
                              ? "Fixed sourced outcomes use 1/n: ordinary spawn weight is not a fixed-material selection weight."
                              : "Catalog explicitly declares uniform eligible modifier-tier candidates; game weights are unavailable."));
                return flatten(
                    branch.candidates().iterator(),
                    candidate -> {
                      var selection =
                          branchMass.multiply(Fraction.of(uniform ? 1 : candidate.weight(), total));
                      if (candidate.stats().size() != 1) {
                        var law = simulator.automaticJointRolls(candidate);
                        if (law.isEmpty()) return Collections.<Generated>emptyIterator();
                        if (candidate.stats().stream().anyMatch(r -> r.min() != r.max()))
                          assumptions.add(CoupledStatRollModel.assumption(candidate, null));
                        if (!policy.withinExplorationBoundary(
                            branch.remaining().explicits().size() + 1)) {
                          boundary = boundary.add(selection);
                          emitted = emitted.add(selection);
                          return Collections.<Generated>emptyIterator();
                        }
                        return law.get().stream()
                            .map(
                                cell -> {
                                  var explicits = new ArrayList<>(branch.remaining().explicits());
                                  explicits.add(cell.modifier());
                                  var tiers = new HashMap<>(keptTiers);
                                  tiers.remove(candidate.id());
                                  return generated(
                                      new TierState(
                                          simulator.finishBasicCurrency(
                                              branch.remaining(), explicits),
                                          tiers,
                                          lineage ? Set.copyOf(retainedIds) : Set.of(),
                                          lineage),
                                      selection.multiply(
                                          Fraction.of(
                                              cell.ticks(), CoupledStatRollModel.RESOLUTION + 1)));
                                })
                            .iterator();
                      }
                      var range = candidate.stats().getFirst();
                      var size =
                          BigInteger.valueOf(range.max())
                              .subtract(BigInteger.valueOf(range.min()))
                              .add(BigInteger.ONE);
                      if (size.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0)
                        return Collections.<Generated>emptyIterator();
                      var roll = simulator.singleStatRollAssumption(candidate);
                      if (roll != null) assumptions.add(roll);
                      if (!policy.withinExplorationBoundary(
                          branch.remaining().explicits().size() + 1)) {
                        boundary = boundary.add(selection);
                        emitted = emitted.add(selection);
                        return Collections.<Generated>emptyIterator();
                      }
                      var mass = selection.multiply(new Fraction(BigInteger.ONE, size));
                      var bands =
                          new Iterator<Generated>() {
                            long value = range.min();
                            boolean done;

                            public boolean hasNext() {
                              return !done;
                            }

                            public Generated next() {
                              if (done) throw new NoSuchElementException();
                              long start = value, end = partition.end(candidate, start);
                              done = end == range.max();
                              if (!done) value = end + 1;
                              var explicits = new ArrayList<>(branch.remaining().explicits());
                              explicits.add(
                                  new ModifierInstance(candidate.id(), Map.of(range.id(), start)));
                              var tiers = new HashMap<>(keptTiers);
                              tiers.remove(candidate.id());
                              var cellMass =
                                  mass.multiply(
                                      new Fraction(
                                          BigInteger.valueOf(end)
                                              .subtract(BigInteger.valueOf(start))
                                              .add(BigInteger.ONE),
                                          BigInteger.ONE));
                              var member =
                                  deferred
                                      ? posterior.get(
                                          new TierPartition.BandKey(candidate.id(), start, end))
                                      : null;
                              if (member != null
                                  && member.cell().variants().stream()
                                      .allMatch(v -> candidateIds.contains(v.modifierId()))) {
                                if (!emittedCells.add(member.cell())) return null;
                                var representative = member.cell().representative(false);
                                explicits.removeLast();
                                explicits.add(representative);
                                tiers.put(representative.modifierId(), member.cell());
                                cellMass =
                                    cellMass.multiply(
                                        new Fraction(
                                            member
                                                .cellWeight()
                                                .numerator()
                                                .multiply(member.intervalWeight().denominator()),
                                            member
                                                .cellWeight()
                                                .denominator()
                                                .multiply(member.intervalWeight().numerator())));
                              }
                              return generated(
                                  new TierState(
                                      simulator.finishBasicCurrency(branch.remaining(), explicits),
                                      tiers,
                                      lineage ? Set.copyOf(retainedIds) : Set.of(),
                                      lineage),
                                  cellMass);
                            }
                          };
                      return java.util.stream.StreamSupport.stream(
                              Spliterators.spliteratorUnknownSize(bands, Spliterator.ORDERED),
                              false)
                          .filter(Objects::nonNull)
                          .iterator();
                    });
              });
    }

    private Generated generated(TierState state, Fraction mass) {
      generatedCount++;
      primitiveOutcomes++;
      return new Generated(state, mass);
    }

    public void advance(int allowance) {
      if (allowance < 1) throw new IllegalArgumentException("Positive batch required");
      if (completed != null || limited) return;
      long start = System.nanoTime();
      try {
        for (int i = 0; i < allowance; i++) {
          if (!iterator.hasNext()) {
            completed =
                AutomaticCraftingKernel.result(
                    outcomes,
                    boundary,
                    emitted,
                    assumptions,
                    !unsupportedRoll.isEmpty()
                        ? unsupportedRoll
                        : boundary.equals(Fraction.ZERO) ? "" : "EXPLICIT_MODIFIER_BOUNDARY");
            return;
          }
          if (count >= budget) {
            limited = true;
            return;
          }
          var next = iterator.next();
          outcomes.merge(next.state(), next.mass(), Fraction::add);
          emitted = emitted.add(next.mass());
          count++;
        }
      } finally {
        primitiveNanos += System.nanoTime() - start;
      }
    }

    public boolean finished() {
      return completed != null;
    }

    public boolean limited() {
      return limited;
    }

    public long retainedStates() {
      return outcomes.size() + Math.max(0, generatedCount - count);
    }

    public Result result() {
      if (completed != null) return completed;
      return AutomaticCraftingKernel.result(
          outcomes,
          boundary,
          emitted,
          assumptions,
          limited ? "OUTCOME_BUDGET" : "ENUMERATION_PENDING");
    }
  }

  private static Result result(
      Map<TierState, Fraction> outcomes,
      Fraction boundary,
      Fraction emitted,
      Set<WorkbenchSimulator.Assumption> assumptions,
      String reason) {
    var unresolved =
        new Fraction(emitted.denominator().subtract(emitted.numerator()), emitted.denominator());
    return new Result(
        unresolved.equals(Fraction.ZERO) && boundary.equals(Fraction.ZERO)
            ? Status.COMPLETE
            : Status.PARTIAL,
        reason,
        outcomes.entrySet().stream()
            .map(
                e ->
                    e.getKey().lineage()
                        ? new Outcome(
                            e.getKey().item(),
                            e.getValue(),
                            e.getKey().tiers(),
                            e.getKey().retained())
                        : new Outcome(e.getKey().item(), e.getValue(), e.getKey().tiers()))
            .toList(),
        boundary,
        unresolved,
        List.copyOf(assumptions));
  }

  private static Result blocked(Status status, String reason) {
    return new Result(status, reason, List.of(), Fraction.ZERO, Fraction.ONE, List.of());
  }

  public static boolean hasQualityCap(ItemState item) {
    return item.explicits().stream()
        .anyMatch(
            m ->
                m.modifierId().equals(QualityLimitRules.BREACH_ID)
                    && Long.valueOf(20).equals(m.values().get(QualityLimitRules.STAT_ID)));
  }
}
