package com.poe2craft.shared.crafting.domain;

import com.poe2craft.shared.item.AugmentCraftingRules;
import com.poe2craft.shared.item.BaseRegistry;
import com.poe2craft.shared.item.BasicJewel;
import com.poe2craft.shared.item.CatalystQuality;
import com.poe2craft.shared.item.EquipmentCurrencyCatalog;
import com.poe2craft.shared.item.ItemCatalog;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ManualRevealCatalog;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.QualitySanctification;
import com.poe2craft.shared.item.ReviewedBodies;
import com.poe2craft.shared.item.ReviewedBoots;
import com.poe2craft.shared.item.ReviewedGloves;
import com.poe2craft.shared.item.ReviewedHelmets;
import com.poe2craft.shared.item.ReviewedSceptres;
import com.poe2craft.shared.item.ReviewedWands;
import java.util.*;
import java.util.random.RandomGenerator;

/**
 * 검증된 카탈로그에서 한 번의 제작 결과와 자동 탐색용 분기를 계산한다. 변경하지 않은 옵션의 실제 수치는 보존한다.
 *
 * <p>사용자 아이템은 불변 값으로 받고 난수는 전달된 공급자만 소비한다. materials·rolls·abyss는 각각 재료 후보, 수치 분포, 훼손 실행을 담당한다.
 * socketSamplers는 자체 잠금 아래 MAX_SOCKET_SAMPLERS까지 보유하며 사용자 상태는 저장하지 않는다. engine 인자는 기존 생성자 호환을 위한
 * null 검사용이다. 잘못된 입력은 예외, 유효하지만 불가능한 행동은 결과 사유로 구별한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code RULE_VERSION}: 버전; 초기값 {@code "solar-workbench-abyss-essence-v24"} ({@code String}).
 *   <li>{@code LEDGER_VERSION}: 버전; 초기값 {@code "solar-uniform-assumptions-v14"} ({@code String}).
 *   <li>{@code catalog}: 이 인스턴스가 사용하는 검증된 카탈로그 ({@code ItemCatalog}).
 *   <li>{@code MAX_SOCKET_SAMPLERS}: SAMPLERS; 초기값 {@code 32} ({@code int}).
 *   <li>{@code socketSamplers}: 최근 사용 순서로 보관하는 증강물 문맥별 실행기 캐시 ({@code Map<String,
 *       WorkbenchSimulator>}).
 *   <li>{@code qualityCapChangePolicy}: 품질 상한 변화 시 기존 품질 처리 정책 ({@code QualityCapChangePolicy}).
 *   <li>{@code additionRules}: 표본 실행과 정확 계산이 공유하는 옵션 추가 규칙 ({@code AdditionRules}).
 *   <li>{@code materials}: 시작 시 해결한 재료별 추가·교체 후보 ({@code WorkbenchMaterials}).
 *   <li>{@code rolls}: 수치 굴림과 유한 결합 분포 계산기 ({@code WorkbenchRolls}).
 *   <li>{@code abyss}: 훼손 슬롯을 보존하는 제작 실행기 ({@code AbyssWorkbench}).
 *   <li>{@code validator}: 상태·입력 검증기 ({@code ItemStateValidator}).
 *   <li>{@code cachedCompatibilityId}: 지연 계산해 공유하는 규칙 호환 해시 ({@code String}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code WorkbenchSimulator(catalog, engine)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code
 *       catalog}(카탈로그), {@code engine}(계산엔진).
 *   <li>{@code WorkbenchSimulator(catalog, engine, coupledModifierIds)}: 기본 정책을 보충해 공통 생성자로 위임한다.
 *       입력: {@code catalog}(카탈로그), {@code engine}(계산엔진), {@code coupledModifierIds}(결합·옵션·식별자목록).
 *   <li>{@code WorkbenchSimulator(catalog, engine, coupledModifierIds, essenceTargetOverrides)}: 기본
 *       정책을 보충해 공통 생성자로 위임한다. 입력: {@code catalog}(카탈로그), {@code engine}(계산엔진), {@code
 *       coupledModifierIds}(결합·옵션·식별자목록), {@code essenceTargetOverrides}(에센스·대상·대체설정).
 *   <li>{@code WorkbenchSimulator(catalog, engine, coupledModifierIds, essenceTargetOverrides,
 *       replacementTargetOverrides)}: 기본 정책을 보충해 공통 생성자로 위임한다. 입력: {@code catalog}(카탈로그), {@code
 *       engine}(계산엔진), {@code coupledModifierIds}(결합·옵션·식별자목록), {@code
 *       essenceTargetOverrides}(에센스·대상·대체설정), {@code replacementTargetOverrides}(교체·대상·대체설정).
 *   <li>{@code WorkbenchSimulator(catalog, engine, coupledModifierIds, essenceTargetOverrides,
 *       replacementTargetOverrides, qualityCapChangePolicy)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       catalog}(카탈로그), {@code engine}(계산엔진), {@code coupledModifierIds}(결합·옵션·식별자목록), {@code
 *       essenceTargetOverrides}(에센스·대상·대체설정), {@code replacementTargetOverrides}(교체·대상·대체설정),
 *       {@code qualityCapChangePolicy}(품질·상한·Change·정책).
 *   <li>{@code forCatalog(enriched)}: 대상·카탈로그의 입력에 따른 분기·결과를 처리한다. 입력: {@code enriched}(enriched).
 *       반환: {@code WorkbenchSimulator}.
 *   <li>{@code catalog()}: {@code catalog} 값을 조회한다. 반환: {@code ItemCatalog}.
 *   <li>{@code forItem(state)}: 대상·아이템 처리에서 {@code catalog.augmentContextApplied}, {@code
 *       AugmentCraftingRules.affectsSampling}을(를) 사용한다. 입력: {@code state}(상태). 반환: {@code
 *       WorkbenchSimulator}.
 *   <li>{@code referenceEssenceTargets()}: 참조·에센스·대상목록 값을 조회·구성한다. 반환: {@code Map<String,
 *       List<String>>}.
 *   <li>{@code compatibilityId()}: 호환성·식별자 값을 조회·구성한다. 반환: {@code String}.
 *   <li>{@code ruleVersion()}: 규칙·버전 값을 조회·구성한다. 반환: {@code String}.
 *   <li>{@code baseRuleVersion()}: 베이스·규칙·버전 값을 조회·구성한다. 반환: {@code String}.
 *   <li>{@code ledgerVersion()}: 규칙장부·버전 값을 조회·구성한다. 반환: {@code String}.
 *   <li>{@code apply(state, action, random)}: 주어진 행동 또는 변경을 적용한 결과를 구한다. 입력: {@code state}(상태),
 *       {@code action}(행동), {@code random}(난수생성기). 반환: {@code Result}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code baseMaterialActions()}: 베이스·제작재료·행동목록 값을 조회·구성한다. 반환: {@code
 *       List<WorkbenchCurrency>}.
 *   <li>{@code actions(state, activeOmens)}: 현재 상태에서 가능한 행동과 불가 이유를 조회한다. 입력: {@code state}(상태),
 *       {@code activeOmens}(활성·징조목록). 반환: {@code List<Availability>}.
 *   <li>{@code validate(state)}: 입력의 구조·값·지원 조건을 검증한다. 입력: {@code state}(상태). 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code triggeredOmens(action, activeOmens)}: triggered·징조목록 처리에서 {@code
 *       WorkbenchOmens.matching}, {@code WorkbenchOmens.parseOmens}을(를) 사용한다. 입력: {@code
 *       action}(행동), {@code activeOmens}(활성·징조목록). 반환: {@code Set<String>}.
 *   <li>{@code availability(state, action, omens)}: 화폐의 현재 실행 가능 여부와 이유를 판정한다. 입력: {@code
 *       state}(상태), {@code action}(행동), {@code omens}(징조목록). 반환: {@code Availability}.
 *   <li>{@code blocked(action, reason)}: 차단의 입력에 따른 분기·결과를 처리한다. 입력: {@code action}(행동), {@code
 *       reason}(이유). 반환: {@code Availability}.
 *   <li>{@code alchemySide(state, omen)}: 연금·접두접미 처리에서 {@code omen.affix}, {@code catalog.base}을(를)
 *       사용한다. 입력: {@code state}(상태), {@code omen}(징조). 반환: {@code WorkbenchOmen}.
 *   <li>{@code completeLegacyAlchemy(state, omen, memo)}: 완료·구버전·연금 처리에서 {@code state.explicits},
 *       {@code catalog.find}을(를) 사용한다. 입력: {@code state}(상태), {@code omen}(징조), {@code memo}(memo).
 *       반환: {@code boolean}.
 *   <li>{@code canAddFixed(state, target)}: Add·고정을(를) 가능 여부를 판정한다. 입력: {@code state}(상태), {@code
 *       target}(대상). 반환: {@code boolean}.
 *   <li>{@code replacementRemovalCandidates(state, action, omen)}: 교체·제거·후보목록 처리에서 {@code
 *       action.isLiquid}, {@code materials.replacementTargets}을(를) 사용한다. 입력: {@code state}(상태),
 *       {@code action}(행동), {@code omen}(징조). 반환: {@code List<ModifierInstance>}.
 *   <li>{@code pool(state, currency, omen)}: 후보풀 처리에서 {@code additionRules.pool}, {@code
 *       StateBucket.forModifierEligibility}을(를) 사용한다. 입력: {@code state}(상태), {@code currency}(화폐),
 *       {@code omen}(징조). 반환: {@code List<ModifierDefinition>}.
 *   <li>{@code chaosRemovalCandidates(state, currency, omens)}: 지정한 카오스와 징조에서 제거 대상이 될 수 있는 실제 명시
 *       옵션을 조회한다.
 *   <li>{@code removalCandidates(state, omen)}: 제거·후보목록의 입력에 따른 분기·결과를 처리한다. 입력: {@code state}(상태),
 *       {@code omen}(징조). 반환: {@code List<ModifierInstance>}.
 *   <li>{@code removalCandidates(state, omens)}: 제거·후보목록의 입력에 따른 분기·결과를 처리한다. 입력: {@code
 *       state}(상태), {@code omens}(징조목록). 반환: {@code List<ModifierInstance>}.
 *   <li>{@code removalCandidates(state, action, omens)}: 제거·후보목록 처리에서 {@code state.explicits},
 *       {@code m.fractured}을(를) 사용한다. 입력: {@code state}(상태), {@code action}(행동), {@code
 *       omens}(징조목록). 반환: {@code List<ModifierInstance>}.
 *   <li>{@code upgrade(state, currency)}: upgrade 처리에서 {@code currency.baseAction}, {@code
 *       state.rarity}을(를) 사용한다. 입력: {@code state}(상태), {@code currency}(화폐). 반환: {@code
 *       ItemState.Rarity}.
 *   <li>{@code basicRarityAllows(rarity, kind)}: 기본·희귀도·Allows의 입력에 따른 분기·결과를 처리한다. 입력: {@code
 *       rarity}(희귀도), {@code kind}(종류). 반환: {@code boolean}.
 *   <li>{@code automaticIdentity()}: 자동·정체성 값을 조회·구성한다. 반환: {@code AutomaticIdentity}.
 *   <li>{@code automaticPlan(state, execution)}: 자동·계획 처리에서 {@code execution.currency}, {@code
 *       execution.omens}을(를) 사용한다. 입력: {@code state}(상태), {@code execution}(실행). 반환: {@code
 *       AutomaticCraftingPlan}.
 *   <li>{@code basicCurrencyPlan(state, action)}: 기본·화폐·계획 처리에서 {@code ACTIONS.contains}, {@code
 *       available.available}을(를) 사용한다. 입력: {@code state}(상태), {@code action}(행동). 반환: {@code
 *       BasicCurrencyPlan}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code finishBasicCurrency(remaining, explicits)}: 완료·기본·화폐 처리에서 {@code
 *       qualityCapChangePolicy.afterAcceptedOperation}, {@code remaining.rarity}을(를) 사용한다. 입력:
 *       {@code remaining}(남은), {@code explicits}(명시옵션목록). 반환: {@code ItemState}.
 *   <li>{@code basicSelectionAssumptions(candidates)}: 기본·선택·계산가정 처리에서 {@code
 *       rolls.recordSelectionAssumptions}을(를) 사용한다. 입력: {@code candidates}(후보목록). 반환: {@code
 *       List<Assumption>}.
 *   <li>{@code basicRemovalAssumption(candidates, action)}: 기본·제거·Assumption 처리에서 {@code
 *       candidates.size}, {@code candidates.stream}을(를) 사용한다. 입력: {@code candidates}(후보목록), {@code
 *       action}(행동). 반환: {@code Assumption}.
 *   <li>{@code apply(state, action, activeOmens, random)}: 주어진 행동 또는 변경을 적용한 결과를 구한다. 입력: {@code
 *       state}(상태), {@code action}(행동), {@code activeOmens}(활성·징조목록), {@code random}(난수생성기). 반환:
 *       {@code Result}.
 *   <li>{@code applyCustomExalt(state, action, activeOmens, random, modifierId)}: 사용자가 고른 옵션을 공통
 *       엑잘티드 규칙으로 부여한다. 입력: {@code state}(상태), {@code action}(행동), {@code activeOmens}(활성·징조목록),
 *       {@code random}(난수생성기), {@code modifierId}(옵션·식별자). 반환: {@code Result}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code exaltChoices(state, action, ids)}: 엑잘티드·선택목록 처리에서 {@code action.baseAction}, {@code
 *       WorkbenchOmens.parseOmens}을(를) 사용한다. 입력: {@code state}(상태), {@code action}(행동), {@code
 *       ids}(식별자목록). 반환: {@code List<ExaltChoice>}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code exaltAdditionCount(state, matched)}: 엑잘티드·추가·개수 처리에서 {@code
 *       WorkbenchOmen.verifiedDoubleAddition}, {@code matched.stream}을(를) 사용한다. 입력: {@code
 *       state}(상태), {@code matched}(일치한). 반환: {@code int}.
 *   <li>{@code exaltPool(original, state, action, omens, matched)}: 엑잘티드·후보풀 처리에서 {@code
 *       omens.isEmpty}, {@code WorkbenchOmen.verifiedDoubleAddition}을(를) 사용한다. 입력: {@code
 *       original}(원본), {@code state}(상태), {@code action}(행동), {@code omens}(징조목록), {@code
 *       matched}(일치한). 반환: {@code List<ModifierDefinition>}.
 *   <li>{@code apply(state, action, activeOmens, random, forcedModifierId)}: 주어진 행동 또는 변경을 적용한 결과를
 *       구한다. 입력: {@code state}(상태), {@code action}(행동), {@code activeOmens}(활성·징조목록), {@code
 *       random}(난수생성기), {@code forcedModifierId}(forced·옵션·식별자). 반환: {@code Result}. 사용하는 예외:
 *       {@code IllegalArgumentException}.
 *   <li>{@code lowersCapBelowQuality(state, removed)}: lowers·상한·Below·품질 처리에서 {@code
 *       removed.values}, {@code state.explicits}을(를) 사용한다. 입력: {@code state}(상태), {@code
 *       removed}(제거된). 반환: {@code boolean}.
 *   <li>{@code copy(s, rarity, implicits, explicits)}: 보존할 값의 사본을 만든다. 입력: {@code s}(s), {@code
 *       rarity}(희귀도), {@code implicits}(암시옵션목록), {@code explicits}(명시옵션목록). 반환: {@code ItemState}.
 *   <li>{@code automaticJointRolls(definition)}: 복합 수치의 유한 결합 분포를 구하고 번들 정의만 캐시한다. 입력: {@code
 *       definition}(정의). 반환: {@code Optional<List<CoupledStatRollModel.Cell>>}.
 *   <li>{@code singleStatRollAssumption(definition)}: 독립 단일 수치 굴림의 가정 정보를 구성한다. 입력: {@code
 *       definition}(정의). 반환: {@code Assumption}.
 *   <li>{@code abyssPlan(state, action, omens)}: 훼손 아이템의 다음 화폐 실행 가능성과 분기를 구한다. 입력: {@code
 *       state}(상태), {@code action}(행동), {@code omens}(징조목록). 반환: {@code AbyssCraftingPlan}.
 *   <li>{@code applyAbyss(state, action, omens, random)}: 훼손 슬롯을 보존하며 화폐를 실행한다. 입력: {@code
 *       state}(상태), {@code action}(행동), {@code omens}(징조목록), {@code random}(난수생성기). 반환: {@code
 *       AbyssCraftingState}.
 *   <li>{@code applyCustomExalt(state, action, omens, random, modifierId)}: 사용자가 고른 옵션을 공통 엑잘티드
 *       규칙으로 부여한다. 입력: {@code state}(상태), {@code action}(행동), {@code omens}(징조목록), {@code
 *       random}(난수생성기), {@code modifierId}(옵션·식별자). 반환: {@code AbyssCraftingState}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code AutomaticIdentity}: ruleVersion/ledgerVersion은 실행·가정 버전, definitions는 정의 묶음,
 *       fixedTargets/replacementTargets는 베이스별 추가/교체 후보, qualityPolicy는 품질 한도 감소 정책이다.
 *   <li>{@code ExaltChoice}: modifierId는 선택 가능한 고귀 옵션, probability는 현재 후보 풀에서의 선택 확률이다.
 *   <li>{@code Availability}: action은 요청 화폐, available은 가능 여부, reason은 불가능한 경우의 설명이다.
 *   <li>{@code Event}: kind는 사건 종류, modifierId는 선택 옵션, values는 실제 굴린 수치, selectionProbability는 선택
 *       확률이다. 생성자는 values를 방어 복사하여 실행 뒤의 변경을 차단한다.
 *   <li>{@code Assumption}: id는 가정 식별자, candidateUnit/n/candidates는 후보 단위·개수·식별자, min/max는 수치 범위,
 *       sourceUrl/reason은 근거, ratioTick은 복합 수치의 비율 눈금이다. 8인자 생성자는 ratioTick=null을 사용하고 기본 생성자는
 *       candidates를 방어 복사한다.
 *   <li>{@code Result}: ruleVersion/ledgerVersion/snapshotId는 재현 기준, state는 결과 상태, action은 화폐,
 *       applied/reason은 실행 여부·거부 사유, events/assumptions는 실제 사건·추정 근거, consumedOmens/remainingOmens는
 *       징조 처리 결과, qualityLimit는 적용 후 품질 한도다. 생성자는 사건·가정·징조 목록을 방어 복사한다.
 * </ul>
 */
public final class WorkbenchSimulator {
  public static final String RULE_VERSION = "solar-workbench-abyss-essence-v24";
  public static final String LEDGER_VERSION = "solar-uniform-assumptions-v14";
  private final ItemCatalog catalog;
  private static final int MAX_SOCKET_SAMPLERS = 32;
  private final Map<String, WorkbenchSimulator> socketSamplers =
      new LinkedHashMap<>(16, 0.75f, true);
  private final QualityCapChangePolicy qualityCapChangePolicy;
  private final AdditionRules additionRules;
  private final WorkbenchMaterials materials;
  private final WorkbenchRolls rolls;
  private final AbyssWorkbench abyss;
  private final ItemStateValidator validator;

  public WorkbenchSimulator(ItemCatalog catalog, CraftingEngine engine) {
    this(catalog, engine, Set.of());
  }

  /** Explicit opt-in for source-proven multi-stat definitions; never prunes a catalog. */
  public WorkbenchSimulator(
      ItemCatalog catalog, CraftingEngine engine, Set<String> coupledModifierIds) {
    this(catalog, engine, coupledModifierIds, Map.of());
  }

  public WorkbenchSimulator(
      ItemCatalog catalog,
      CraftingEngine engine,
      Set<String> coupledModifierIds,
      Map<WorkbenchCurrency, List<String>> essenceTargetOverrides) {
    this(catalog, engine, coupledModifierIds, essenceTargetOverrides, Map.of());
  }

  public WorkbenchSimulator(
      ItemCatalog catalog,
      CraftingEngine engine,
      Set<String> coupledModifierIds,
      Map<WorkbenchCurrency, List<String>> essenceTargetOverrides,
      Map<WorkbenchCurrency, List<String>> replacementTargetOverrides) {
    this(
        catalog,
        engine,
        coupledModifierIds,
        essenceTargetOverrides,
        replacementTargetOverrides,
        QualityCapChangePolicy.DEFAULT);
  }

  public WorkbenchSimulator(
      ItemCatalog catalog,
      CraftingEngine engine,
      Set<String> coupledModifierIds,
      Map<WorkbenchCurrency, List<String>> essenceTargetOverrides,
      Map<WorkbenchCurrency, List<String>> replacementTargetOverrides,
      QualityCapChangePolicy qualityCapChangePolicy) {
    this.catalog = ManualRevealCatalog.enrich(Objects.requireNonNull(catalog));
    this.qualityCapChangePolicy = Objects.requireNonNull(qualityCapChangePolicy);
    materials =
        new WorkbenchMaterials(
            catalog, this.catalog, essenceTargetOverrides, replacementTargetOverrides);
    rolls = new WorkbenchRolls(this.catalog, coupledModifierIds);
    abyss = new AbyssWorkbench(this.catalog, materials, rolls);
    Objects.requireNonNull(engine);
    additionRules = new AdditionRules(this.catalog);
    validator = new ItemStateValidator(this.catalog);
  }

  /** Preserve configured roll, target and quality-cap policies when adding reviewed outcomes. */
  public WorkbenchSimulator forCatalog(ItemCatalog enriched) {
    return new WorkbenchSimulator(
        enriched,
        new CraftingEngine(enriched),
        rolls.coupledModifierIds,
        materials.essenceTargetOverrides,
        materials.replacementTargetOverrides,
        qualityCapChangePolicy);
  }

  public ItemCatalog catalog() {
    return catalog;
  }

  public WorkbenchSimulator forItem(ItemState state) {
    if (catalog.augmentContextApplied() || !AugmentCraftingRules.affectsSampling(state))
      return this;
    String key =
        AugmentCraftingRules.suffixBonus(state)
            + ":"
            + new TreeSet<>(AugmentCraftingRules.pools(state));
    synchronized (socketSamplers) {
      var cached = socketSamplers.get(key);
      if (cached != null) return cached;
      var created = forCatalog(AugmentCraftingRules.forItem(catalog, state));
      if (socketSamplers.size() >= MAX_SOCKET_SAMPLERS)
        socketSamplers.remove(socketSamplers.keySet().iterator().next());
      socketSamplers.put(key, created);
      return created;
    }
  }

  /** The same resolved targets used by manual execution, limited to this catalog. */
  public Map<String, List<String>> referenceEssenceTargets() {
    var result = new TreeMap<String, List<String>>();
    var supported = Set.copyOf(baseMaterialActions());
    for (var action : WorkbenchCurrency.values()) {
      if (!supported.contains(action)
          || action.materialPolicy().kind() != WorkbenchDefinitions.MaterialKind.ESSENCE) continue;
      var targets =
          java.util.stream.Stream.concat(
                  materials.essenceTargets(action).stream(),
                  materials.replacementTargets(action).stream())
              .filter(id -> catalog.find(id).isPresent())
              .distinct()
              .sorted()
              .toList();
      if (!targets.isEmpty()) result.put(action.name(), targets);
    }
    return Collections.unmodifiableMap(result);
  }

  /** 해당 베이스의 실제 제작기에서 재료별로 부여할 수 있는 적격 옵션 ID를 조회한다. */
  public Map<WorkbenchCurrency, List<String>> sourceMaterialTargets(int itemLevel) {
    var result = new EnumMap<WorkbenchCurrency, List<String>>(WorkbenchCurrency.class);
    for (var action : baseMaterialActions()) {
      if (action.minimumModifierLevel() > itemLevel) continue;
      var targets =
          java.util.stream.Stream.concat(
                  materials.essenceTargets(action).stream(),
                  materials.replacementTargets(action).stream())
              .distinct()
              .toList();
      if (!targets.isEmpty()
          && targets.stream()
              .allMatch(
                  id ->
                      catalog.find(id).filter(d -> d.requiredItemLevel() <= itemLevel).isPresent()))
        result.put(action, targets);
    }
    return result;
  }

  /** Per-base mechanics identity; cosmetics and other catalogs never participate. */
  private volatile String cachedCompatibilityId;

  public String compatibilityId() {
    var value = cachedCompatibilityId;
    if (value != null) return value;
    synchronized (this) {
      if (cachedCompatibilityId == null)
        cachedCompatibilityId = ItemRulesetIdentity.of(catalog, this, rolls.coupledModifierIds);
      return cachedCompatibilityId;
    }
  }

  public String ruleVersion() {
    return (EquipmentCurrencyCatalog.targets(catalog.base().id()).isEmpty()
            ? ""
            : "equipment-currency-v1-")
        + baseRuleVersion()
        + "-homogenising-legacy-v1-legacy-five-v1-omen-composition-v2-fracture-whittling-v2-catalyst-max-v1-refined-sapphire-v1"
        + (qualityCapChangePolicy == QualityCapChangePolicy.PRESERVE_EXISTING
            ? "-quality-cap-preserve-v2"
            : "-quality-cap-reject-v1")
        + "-"
        + QualitySanctification.ASSUMPTION
        + "-"
        + QualityCurrencyRules.VERSION
        + "-"
        + QualityInfuserRules.VERSION
        + "-essence-full-side-v1-augment-weight-v1-resistance-magnitude-v1";
  }

  private String baseRuleVersion() {
    if (BaseRegistry.supports(catalog.base().id()))
      return BaseRegistry.policy(catalog.base().id()).ruleVersion();
    if (BasicJewel.supportedCrafting(catalog.base().id()))
      return BasicJewel.timeLost(catalog.base().id())
          ? "time-lost-ancient-v3-five-affix-chaos-overflow"
          : "basic-jewel-potent-v5-five-affix-chaos-overflow";
    if (catalog.base().id().equals(RingEssenceTargets.BASE_ID))
      return "ring-workbench-perfect-essence-v1";
    if (HelmetEssenceTargets.supports(catalog.base().id()))
      return "helmet-workbench-perfect-essence-v1";
    if (catalog.base().id().equals(BeltEssenceTargets.BASE_ID))
      return "belt-workbench-perfect-essence-v1";
    if ((catalog.base().id().equals(SceptreEssenceTargets.BASE_ID)
        || ReviewedSceptres.supports(catalog.base().id()))) return "sceptre-workbench-essence-v1";
    if (BodyEssenceTargets.supports(catalog.base().id()))
      return "body-workbench-perfect-essence-v1";
    if ((catalog.base().id().equals(WandEssenceTargets.BASE_ID)
        || ReviewedWands.supports(catalog.base().id()))) return "wand-workbench-essence-v1";
    if (catalog.base().id().equals(BowEssenceTargets.BASE_ID))
      return "bow-workbench-perfect-essence-v2";
    return rolls.coupledModifierIds.isEmpty() ? RULE_VERSION : "stocky-workbench-artificer-v26";
  }

  public String ledgerVersion() {
    if (catalog.metadata().weightPolicy().equals("POE2DB_AS_PUBLISHED")
        && (ReviewedBoots.supports(catalog.base().id())
            || ReviewedBodies.supports(catalog.base().id())
            || ReviewedHelmets.supports(catalog.base().id())
            || ReviewedGloves.supports(catalog.base().id())))
      return "poe2db-published-armour-assumptions-v1";
    if (BaseRegistry.supports(catalog.base().id()))
      return BaseRegistry.policy(catalog.base().id()).ledgerVersion();
    if (BasicJewel.supportedCrafting(catalog.base().id()))
      return "sapphire-uniform-candidates-and-rolls-v1";
    if (catalog.base().id().equals(RingEssenceTargets.BASE_ID))
      return "ring-unverified-numeric-assumptions-v1";
    if (HelmetEssenceTargets.supports(catalog.base().id()))
      return "helmet-unverified-numeric-assumptions-v1";
    if (catalog.base().id().equals(BeltEssenceTargets.BASE_ID))
      return "belt-unverified-numeric-assumptions-v1";
    if ((catalog.base().id().equals(SceptreEssenceTargets.BASE_ID)
        || ReviewedSceptres.supports(catalog.base().id())))
      return "sceptre-unverified-numeric-assumptions-v1";
    if (BodyEssenceTargets.supports(catalog.base().id()))
      return "body-unverified-numeric-assumptions-v1";
    if ((catalog.base().id().equals(WandEssenceTargets.BASE_ID)
        || ReviewedWands.supports(catalog.base().id())))
      return "wand-unverified-numeric-assumptions-v1";
    if (catalog.base().id().equals(BowEssenceTargets.BASE_ID))
      return "bow-unverified-numeric-assumptions-v1";
    return rolls.coupledModifierIds.isEmpty()
        ? LEDGER_VERSION
        : "stocky-unverified-numeric-assumptions-v11";
  }

  public Result apply(ItemState state, CraftingAction action, RandomGenerator random) {
    if (action == null) throw new IllegalArgumentException("Action required");
    return apply(state, WorkbenchCurrency.valueOf(action.name()), Set.of(), random);
  }

  /** Base/catalog eligibility only: no rarity, rolls, socket count, or active omens. */
  public List<WorkbenchCurrency> baseMaterialActions() {
    String base = catalog.base().id();
    return Arrays.stream(WorkbenchCurrency.values())
        .filter(
            action -> {
              if (action.catalystType() != null) {
                return action.refinedCatalyst()
                    ? BaseRegistry.refinedCatalyst(base)
                    : BaseRegistry.ordinaryCatalyst(base);
              }
              if (action.isLiquid()) {
                return BasicJewel.supportedCrafting(base)
                    && !materials.replacementTargets(action).isEmpty();
              }
              if (BasicJewel.supportedCrafting(base)
                  || (action.materialPolicy().kind() != WorkbenchDefinitions.MaterialKind.ESSENCE
                      && !action.isAlloy())) return false;
              var targets =
                  java.util.stream.Stream.concat(
                          materials.essenceTargets(action).stream(),
                          materials.replacementTargets(action).stream())
                      .toList();
              return !targets.isEmpty()
                  && targets.stream().allMatch(id -> catalog.find(id).isPresent());
            })
        .toList();
  }

  public List<Availability> actions(ItemState state, Set<String> activeOmens) {
    validate(state);
    var omens = WorkbenchOmens.parseOmens(activeOmens);
    return Arrays.stream(WorkbenchCurrency.values())
        .filter(a -> !a.isLiquid() || BasicJewel.supportedCrafting(state.baseItemId()))
        .filter(
            a ->
                a != WorkbenchCurrency.DIVINE
                    || BaseRegistry.policy(state.baseItemId()) == null
                    || BaseRegistry.policy(state.baseItemId()).divine())
        .filter(
            a ->
                (a != WorkbenchCurrency.ARTIFICER
                        || BaseRegistry.socketExecutionMaximum(state.baseItemId()) != null)
                    && java.util.stream.Stream.concat(
                            materials.essenceTargets(a).stream(),
                            materials.replacementTargets(a).stream())
                        .allMatch(id -> catalog.find(id).isPresent()))
        .map(a -> availability(state, a, omens))
        .toList();
  }

  private void validate(ItemState state) {
    if (state == null
        || !AugmentSocketRules.supportedState(state)
        || !validator.validate(state).isEmpty())
      throw new IllegalArgumentException("Unsupported concrete item state");
  }

  /** The operation's omen set is shared by ordinary and user-selected execution. */
  public Set<String> triggeredOmens(WorkbenchCurrency action, Set<String> activeOmens) {
    return WorkbenchOmens.matching(action, WorkbenchOmens.parseOmens(activeOmens)).stream()
        .map(WorkbenchOmen::id)
        .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
  }

  private Availability availability(
      ItemState state, WorkbenchCurrency action, List<WorkbenchOmen> omens) {
    if (state.conditions().contains(ItemState.Condition.SANCTIFIED))
      return blocked(action, "No supported modification is available for this Sanctified item.");
    if (QualityInfuserRules.supports(action)) {
      var reason = QualityInfuserRules.refusal(state, action, catalog);
      return reason.isEmpty() ? new Availability(action, true, "") : blocked(action, reason);
    }
    if (QualityCurrencyRules.isQuality(action)) {
      var refusal = QualityCurrencyRules.refusal(state, action);
      return refusal.isEmpty() ? new Availability(action, true, "") : blocked(action, refusal);
    }
    if (state.observations() != null
        && state.observations().needsAugmentResolution()
        && com.poe2craft.shared.item.SocketAugmentText.affectsCrafting(
            state.observations().augmentEffects())
        && !(action == WorkbenchCurrency.DIVINE && omens.contains(WorkbenchOmen.SANCTIFICATION)))
      return blocked(
          action,
          "Installed Augment effects are preserved. Provide their names and counts before crafting; their crafting effects are not resolved.");
    if (!materials.replacementTargets(action).isEmpty()
        && state.explicits().stream().filter(m -> AugmentCraftingRules.crafted(m, catalog)).count()
            >= AugmentCraftingRules.craftedLimit(state))
      return blocked(action, "Crafted modifier limit reached.");
    if (action.catalystType() != null) {
      if (action.refinedCatalyst()) {
        if (!BaseRegistry.refinedCatalyst(state.baseItemId()))
          return blocked(
              action,
              "Refined catalysts require a supported Basic Jewel with jewel_catalyst source tag.");
        return new Availability(action, true, "");
      }
      if (!BaseRegistry.ordinaryCatalyst(state.baseItemId()))
        return blocked(action, "Ordinary catalysts require a supported Ring or Amulet.");
      return new Availability(action, true, "");
    }
    if (BasicJewel.supportedCrafting(state.baseItemId())
        && action.baseAction() == null
        && action != WorkbenchCurrency.ALCHEMY
        && action != WorkbenchCurrency.DIVINE
        && !action.isLiquid())
      return blocked(
          action,
          "This operation remains outside the reviewed Jewel basic currency and Liquid scope.");
    if (action.isLiquid() && materials.replacementTargets(action).isEmpty())
      return blocked(action, "No sourced Liquid outcome for this Jewel category and base.");
    if (qualityCapChangePolicy == QualityCapChangePolicy.REJECT_OVERCAP
        && state.catalystQuality() != null
        && (action.baseAction() == CraftingAction.ANNULMENT
            || action.baseAction() == CraftingAction.CHAOS
            || !materials.replacementTargets(action).isEmpty())
        && removalCandidates(state, action, WorkbenchOmens.matching(action, omens)).stream()
            .anyMatch(m -> lowersCapBelowQuality(state, m)))
      return blocked(
          action,
          "Removing the maximum-quality modifier while quality exceeds the remaining cap needs game verification.");
    if (action == WorkbenchCurrency.DIVINE
        && BaseRegistry.policy(state.baseItemId()) != null
        && !BaseRegistry.policy(state.baseItemId()).divine())
      return blocked(
          action,
          "Belt variable implicits and Charm-slot rolls are not modeled; Divine is unsupported.");
    if (action == WorkbenchCurrency.ARTIFICER) {
      var reason = AugmentSocketRules.refusal(state);
      return reason.isEmpty() ? new Availability(action, true, "") : blocked(action, reason);
    }
    if (java.util.stream.Stream.concat(
            materials.essenceTargets(action).stream(),
            materials.replacementTargets(action).stream())
        .anyMatch(id -> catalog.find(id).isEmpty()))
      return blocked(action, "This material's results are not verified for this base.");
    var matches = WorkbenchOmens.matching(action, omens);
    if (WorkbenchOmen.verifiedDoubleAddition(matches)) {
      if (state.itemLevel() < action.minimumModifierLevel())
        return blocked(action, "Item level is below the currency's minimum modifier level.");
      if (state.rarity() != ItemState.Rarity.RARE)
        return blocked(action, "Exalted Orb requires a Rare item.");
      int additions = exaltAdditionCount(state, matches);
      if (additions == 0)
        return blocked(action, "No free affix slot remains for this currency and omen.");
      var first = exaltPool(state, state, action, omens, matches);
      if (first.isEmpty()) return blocked(action, "No verified first addition pool.");
      if (additions == 1) return new Availability(action, true, "");
      for (var candidate : first) {
        var next = new ArrayList<>(state.explicits());
        var values = new HashMap<String, Long>();
        for (var stat : candidate.stats()) values.put(stat.id(), stat.min());
        next.add(new ModifierInstance(candidate.id(), values));
        if (exaltPool(
                state, copy(state, state.rarity(), state.implicits(), next), action, omens, matches)
            .isEmpty())
          return blocked(action, "A first addition branch has no verified second addition pool.");
      }
      return new Availability(action, true, "");
    }
    if (AdditionRules.isAddition(action)) {
      var plan =
          additionRules.plan(
              StateBucket.forModifierEligibility(state),
              action,
              omens.stream().map(WorkbenchOmen::id).collect(java.util.stream.Collectors.toSet()));
      return new Availability(action, plan.available(), plan.reason());
    }
    if (matches.size() > 1
        && !WorkbenchOmen.sideWhittling(matches)
        && !WorkbenchOmen.sideDoubleRemoval(matches))
      return blocked(
          action,
          "Multiple omens for the same operation need combination verification. Deactivate all but one.");
    if (action.minimumModifierLevel() > 0
        && matches.stream().anyMatch(o -> !o.supportsTieredCurrency()))
      return blocked(
          action,
          "Omen interaction with Greater/Perfect currency is not verified. Deactivate the omen or use ordinary currency.");
    if (state.itemLevel() < action.minimumModifierLevel())
      return blocked(action, "Item level is below the currency's minimum modifier level.");
    var omen = matches.isEmpty() ? null : matches.getFirst();
    if (!materials.replacementTargets(action).isEmpty()) {
      if (!action.materialPolicy().allows(state))
        return blocked(
            action, "This material does not support the item rarity configured in its rule.");
      var targets =
          materials.replacementTargets(action).stream()
              .map(id -> catalog.find(id).orElseThrow())
              .toList();
      if (targets.stream().anyMatch(target -> state.itemLevel() < target.requiredItemLevel()))
        return blocked(action, "Essence below the catalog modifier item level is unsupported.");
      if (materials.targetFirstEssence(action)) {
        var plan =
            materials.essenceReplacementPlan(AbyssCraftingState.from(state, catalog), action, omen);
        return plan.available()
            ? new Availability(action, true, "")
            : blocked(action, plan.reason());
      }
      var candidates = replacementRemovalCandidates(state, action, omen);
      if (candidates.isEmpty()) return blocked(action, "No non-Fractured explicit can be removed.");
      for (var removed : candidates) {
        var rest = new ArrayList<>(state.explicits());
        rest.remove(removed);
        var afterRemoval = copy(state, state.rarity(), state.implicits(), rest);
        if (!action.isLiquid()
            && targets.stream().anyMatch(target -> !canAddFixed(afterRemoval, target)))
          return blocked(
              action,
              "An essence removal branch conflicts with a guaranteed modifier or available slots; this interaction is unsupported.");
      }
      return new Availability(action, true, "");
    }
    if (!materials.essenceTargets(action).isEmpty()) {
      if (!action.materialPolicy().allows(state))
        return blocked(
            action, "This essence does not support the item rarity configured in its rule.");
      var targets =
          materials.essenceTargets(action).stream()
              .map(id -> catalog.find(id).orElseThrow())
              .toList();
      if (targets.stream().anyMatch(target -> state.itemLevel() < target.requiredItemLevel()))
        return blocked(
            action,
            "Essence results below the catalog modifier's item level need verification; this low-level scope is unsupported.");
      var rare =
          copy(state, action.materialPolicy().result(state), state.implicits(), state.explicits());
      if (!pool(rare, action, null).containsAll(targets))
        return blocked(
            action,
            "An essence result conflicts with existing families or available affix slots; the overlap interaction is unsupported.");
      return new Availability(action, true, "");
    }
    if (action == WorkbenchCurrency.FRACTURING) {
      if (state.rarity() != ItemState.Rarity.RARE || state.explicits().size() < 4)
        return blocked(
            action, "Fracturing requires a Rare item with at least four explicit modifiers.");
      if (state.explicits().stream().anyMatch(ModifierInstance::fractured))
        return blocked(action, "Cannot fracture an already Fractured item.");
      return new Availability(action, true, "");
    }
    if (action == WorkbenchCurrency.ALCHEMY) {
      if (state.rarity() != ItemState.Rarity.NORMAL && state.rarity() != ItemState.Rarity.MAGIC)
        return blocked(action, "Alchemy requires a Normal or Magic item.");
      if (omen != null) {
        if (state.explicits().stream().anyMatch(ModifierInstance::fractured))
          return blocked(action, "Legacy Alchemy with Fractured modifiers is not verified.");
        if (!completeLegacyAlchemy(
            copy(state, ItemState.Rarity.RARE, state.implicits(), List.of()),
            omen,
            new HashMap<>()))
          return blocked(
              action, "Legacy Alchemy needs a complete maximum-affix four-modifier pool.");
      }
      return new Availability(action, true, "");
    }
    if (action == WorkbenchCurrency.DIVINE) {
      if (omen == WorkbenchOmen.SANCTIFICATION) {
        if (state.rarity() != ItemState.Rarity.RARE)
          return blocked(action, "Sanctification requires a Rare item.");
        return new Availability(action, true, "");
      }
      // Implicit (base) modifiers are distinct from Fractured explicit modifiers.
      // Only the existing Blessed omen redirects Divine to implicit rolls.
      var targets =
          omen == WorkbenchOmen.BLESSED
              ? state.implicits()
              : state.explicits().stream().filter(m -> !m.fractured()).toList();
      if (targets.stream()
          .noneMatch(
              m ->
                  catalog.find(m.modifierId()).orElseThrow().stats().stream()
                      .anyMatch(s -> s.max() > s.min())))
        return blocked(action, "No supported numeric range can be rerolled.");
      return new Availability(action, true, "");
    }
    var kind = action.baseAction();
    boolean rarity = basicRarityAllows(state.rarity(), kind);
    if (!rarity) return blocked(action, "This currency cannot be used on this rarity.");
    if (kind == CraftingAction.ANNULMENT || kind == CraftingAction.CHAOS) {
      var removals = removalCandidates(state, action, matches);
      if (removals.isEmpty())
        return blocked(action, "No eligible explicit modifier can be removed with this omen.");
      if (matches.contains(WorkbenchOmen.GREATER_ANNULMENT) && removals.size() < 2)
        return blocked(
            action,
            "Greater Annulment needs two removable modifiers; one-removal behavior is not verified.");
      if (kind == CraftingAction.CHAOS)
        for (var removed : removals) {
          var rest = new ArrayList<>(state.explicits());
          rest.remove(removed);
          if (pool(copy(state, state.rarity(), state.implicits(), rest), action, null).isEmpty())
            return blocked(action, "A removal branch has no verified replacement pool.");
        }
    } else if (pool(
            copy(state, upgrade(state, action), state.implicits(), state.explicits()), action, omen)
        .isEmpty())
      return blocked(
          action, "No eligible modifier or free affix slot remains for this currency and omen.");
    return new Availability(action, true, "");
  }

  private Availability blocked(WorkbenchCurrency action, String reason) {
    return new Availability(action, false, reason);
  }

  private WorkbenchOmen alchemySide(ItemState state, WorkbenchOmen omen) {
    int maximum =
        omen.affix() == ModifierDefinition.AffixType.PREFIX
            ? catalog.base().rarePrefixes()
            : catalog.base().rareSuffixes();
    return state.explicits().size() < maximum
        ? omen
        : omen == WorkbenchOmen.SINISTRAL_ALCHEMY
            ? WorkbenchOmen.DEXTRAL_ALCHEMY
            : WorkbenchOmen.SINISTRAL_ALCHEMY;
  }

  /**
   * Verify every family branch before drawing; tiers with identical families have identical future
   * eligibility.
   */
  private boolean completeLegacyAlchemy(
      ItemState state, WorkbenchOmen omen, Map<String, Boolean> memo) {
    if (state.explicits().size() == 4) return true;
    var key =
        state.explicits().stream()
                .flatMap(m -> catalog.find(m.modifierId()).orElseThrow().familyIds().stream())
                .sorted()
                .toList()
                .toString()
            + ":"
            + state.explicits().size();
    if (memo.containsKey(key)) return memo.get(key);
    var candidates = pool(state, WorkbenchCurrency.ALCHEMY, alchemySide(state, omen));
    if (candidates.isEmpty()) {
      memo.put(key, false);
      return false;
    }
    var families = new HashSet<Set<String>>();
    for (var d : candidates) {
      if (!families.add(d.familyIds())) continue;
      var next = new ArrayList<>(state.explicits());
      var values = new HashMap<String, Long>();
      for (var stat : d.stats()) values.put(stat.id(), stat.min());
      next.add(new ModifierInstance(d.id(), values));
      if (!completeLegacyAlchemy(
          copy(state, state.rarity(), state.implicits(), next), omen, memo)) {
        memo.put(key, false);
        return false;
      }
    }
    memo.put(key, true);
    return true;
  }

  private boolean canAddFixed(ItemState state, ModifierDefinition target) {
    var existing =
        state.explicits().stream().map(m -> catalog.find(m.modifierId()).orElseThrow()).toList();
    int capacity =
        target.affixType() == ModifierDefinition.AffixType.PREFIX
            ? catalog.base().rarePrefixes()
            : catalog.base().rareSuffixes();
    if (BasicJewel.supportedCrafting(state.baseItemId()))
      capacity +=
          BasicJewel.extra(
              state.explicits().stream().map(ModifierInstance::modifierId).toList(),
              target.affixType());
    return state.itemLevel() >= target.requiredItemLevel()
        && existing.stream().filter(d -> d.affixType() == target.affixType()).count() < capacity
        && existing.stream()
            .noneMatch(d -> !Collections.disjoint(d.familyIds(), target.familyIds()));
  }

  private List<ModifierInstance> replacementRemovalCandidates(
      ItemState state, WorkbenchCurrency action, WorkbenchOmen omen) {
    var candidates = removalCandidates(state, omen);
    if (!action.isLiquid()) return candidates;
    var targets =
        materials.replacementTargets(action).stream()
            .map(id -> catalog.find(id).orElseThrow())
            .toList();
    return candidates.stream()
        .filter(
            removed -> {
              var rest = new ArrayList<>(state.explicits());
              rest.remove(removed);
              return targets.stream()
                  .anyMatch(
                      target ->
                          canAddFixed(
                              copy(state, state.rarity(), state.implicits(), rest), target));
            })
        .toList();
  }

  public List<ModifierDefinition> pool(
      ItemState state, WorkbenchCurrency currency, WorkbenchOmen omen) {
    return additionRules.pool(StateBucket.forModifierEligibility(state), currency, omen);
  }

  private List<ModifierInstance> removalCandidates(ItemState state, WorkbenchOmen omen) {
    return removalCandidates(state, omen == null ? List.of() : List.of(omen));
  }

  private List<ModifierInstance> removalCandidates(ItemState state, List<WorkbenchOmen> omens) {
    return removalCandidates(state, null, omens);
  }

  /** 카오스의 분열·주얼 잠금·방향·요구 레벨 필터를 적용한 실제 제거 후보를 반환한다. 전체 화폐 사용 자격은 recipePlan에서 별도로 검사한다. */
  public List<ModifierInstance> chaosRemovalCandidates(
      ItemState state, WorkbenchCurrency currency, Set<WorkbenchOmen> omens) {
    if (currency.baseAction() != CraftingAction.CHAOS)
      throw new IllegalArgumentException("Expected a Chaos Orb");
    return removalCandidates(state, currency, List.copyOf(omens));
  }

  private List<ModifierInstance> removalCandidates(
      ItemState state, WorkbenchCurrency action, List<WorkbenchOmen> omens) {
    var eligible = state.explicits().stream().filter(m -> !m.fractured()).toList();
    if (action != null && action.baseAction() == CraftingAction.CHAOS) {
      var locked = BasicJewel.chaosLockedSides(state, catalog);
      eligible =
          eligible.stream()
              .filter(m -> !locked.contains(catalog.find(m.modifierId()).orElseThrow().affixType()))
              .toList();
    }
    if (omens.contains(WorkbenchOmen.LIGHT)) {
      var desecratedIds =
          state.desecrated().stream()
              .map(ItemState.DesecratedSlot::modifierId)
              .filter(Objects::nonNull)
              .collect(java.util.stream.Collectors.toSet());
      eligible = eligible.stream().filter(m -> desecratedIds.contains(m.modifierId())).toList();
    }
    var omen = omens.stream().filter(o -> o.affix() != null).findFirst().orElse(null);
    if (omen != null && omen.affix() != null)
      eligible =
          eligible.stream()
              .filter(m -> catalog.find(m.modifierId()).orElseThrow().affixType() == omen.affix())
              .toList();
    if (omens.contains(WorkbenchOmen.WHITTLING) && !eligible.isEmpty()) {
      int lowest =
          eligible.stream()
              .mapToInt(m -> catalog.find(m.modifierId()).orElseThrow().requiredItemLevel())
              .min()
              .orElseThrow();
      eligible =
          eligible.stream()
              .filter(m -> catalog.find(m.modifierId()).orElseThrow().requiredItemLevel() == lowest)
              .toList();
    }
    return eligible;
  }

  private ItemState.Rarity upgrade(ItemState state, WorkbenchCurrency currency) {
    return currency.baseAction() == CraftingAction.TRANSMUTATION
        ? ItemState.Rarity.MAGIC
        : currency.baseAction() == CraftingAction.REGAL ? ItemState.Rarity.RARE : state.rarity();
  }

  /** 징조 없는 기본 화폐의 희귀도 전제만 판정한다. 후보·슬롯 등 다른 조건은 계획 단계에서 검사한다. */
  static boolean basicRarityAllows(ItemState.Rarity rarity, CraftingAction kind) {
    return switch (kind) {
      case TRANSMUTATION -> rarity == ItemState.Rarity.NORMAL;
      case AUGMENTATION, REGAL -> rarity == ItemState.Rarity.MAGIC;
      case EXALTED, CHAOS -> rarity == ItemState.Rarity.RARE;
      case ANNULMENT -> rarity == ItemState.Rarity.MAGIC || rarity == ItemState.Rarity.RARE;
    };
  }

  /**
   * ruleVersion/ledgerVersion은 실행·가정 버전, definitions는 정의 묶음, fixedTargets/replacementTargets는 베이스별
   * 추가/교체 후보, qualityPolicy는 품질 한도 감소 정책이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code ruleVersion}: 규칙·버전 ({@code String}).
   *   <li>{@code ledgerVersion}: 규칙장부·버전 ({@code String}).
   *   <li>{@code definitions}: 정의목록 ({@code WorkbenchDefinitions.Document}).
   *   <li>{@code fixedTargets}: 고정·대상목록 ({@code Map<WorkbenchCurrency, List<String>>}).
   *   <li>{@code replacementTargets}: 교체·대상목록 ({@code Map<WorkbenchCurrency, List<String>>}).
   *   <li>{@code qualityPolicy}: 품질·정책 ({@code QualityCapChangePolicy}).
   * </ul>
   */
  public record AutomaticIdentity(
      String ruleVersion,
      String ledgerVersion,
      WorkbenchDefinitions.Document definitions,
      Map<WorkbenchCurrency, List<String>> fixedTargets,
      Map<WorkbenchCurrency, List<String>> replacementTargets,
      QualityCapChangePolicy qualityPolicy) {}

  /** 자동 탐색의 호환성 검사용으로 현재 규칙·후보·품질 정책의 불변 묶음을 제공한다. */
  public AutomaticIdentity automaticIdentity() {
    return new AutomaticIdentity(
        ruleVersion(),
        ledgerVersion(),
        WorkbenchDefinitions.active().document(),
        materials.essenceTargetOverrides,
        materials.replacementTargetOverrides,
        qualityCapChangePolicy);
  }

  /** Read-only exhaustive projection; the manual sampler remains independent of search policy. */
  public AutomaticCraftingPlan automaticPlan(ItemState state, AutomaticCraftingAction execution) {
    return currencyPlan(state, execution.currency(), List.copyOf(execution.omens()), false);
  }

  /** 특정 재료의 작업대 적용 자격을 확인한다. 전체 화면용 행동 목록은 생성하지 않는다. */
  public Availability availability(
      ItemState state, WorkbenchCurrency action, Set<WorkbenchOmen> omens) {
    validate(state);
    return availability(state, action, List.copyOf(new TreeSet<>(omens)));
  }

  /** 실험 카드의 한 번 사용을 작업대 후보·교체 규칙으로 전개한다. 자동 탐색의 지원 화폐 목록은 별도로 적용한다. */
  public AutomaticCraftingPlan recipePlan(
      ItemState state, WorkbenchCurrency action, Set<WorkbenchOmen> omens) {
    return currencyPlan(state, action, List.copyOf(new TreeSet<>(omens)), true);
  }

  /** 실행 방식에 따라 허용 화폐를 확인하고 각 제거 직후 상태와 실제 추가 풀을 반환한다. */
  private AutomaticCraftingPlan currencyPlan(
      ItemState state, WorkbenchCurrency action, List<WorkbenchOmen> omens, boolean recipe) {
    validate(state);
    if (action == WorkbenchCurrency.ANNULMENT) {
      var availability = availability(state, action, omens);
      if (!availability.available() || !WorkbenchOmens.matching(action, omens).containsAll(omens))
        return new AutomaticCraftingPlan(
            AutomaticCraftingPlan.Status.UNAVAILABLE, availability.reason(), List.of(), false, "");
      var eligible = removalCandidates(state, action, omens);
      boolean two = omens.contains(WorkbenchOmen.GREATER_ANNULMENT);
      var branches = new ArrayList<AutomaticCraftingPlan.Branch>();
      // 균등 비복원 추출이므로 순서가 다른 동일 조합은 한 번만 계산한다.
      for (int i = 0; i < eligible.size(); i++) {
        var remaining = new ArrayList<>(state.explicits());
        remaining.remove(eligible.get(i));
        if (!two) {
          branches.add(
              new AutomaticCraftingPlan.Branch(finishBasicCurrency(state, remaining), List.of()));
          continue;
        }
        for (int j = i + 1; j < eligible.size(); j++) {
          var pair = new ArrayList<>(remaining);
          pair.remove(eligible.get(j));
          branches.add(
              new AutomaticCraftingPlan.Branch(finishBasicCurrency(state, pair), List.of()));
        }
      }
      return new AutomaticCraftingPlan(
          AutomaticCraftingPlan.Status.READY,
          "",
          branches,
          false,
          "https://poe2db.tw/us/Orb_of_Annulment");
    }
    if (omens.isEmpty()
        && (action == WorkbenchCurrency.TRANSMUTATION
            || (recipe && BasicCurrencyPlan.ACTIONS.contains(action)))) {
      var plan = basicCurrencyPlan(state, action);
      return new AutomaticCraftingPlan(
          plan.available()
              ? AutomaticCraftingPlan.Status.READY
              : AutomaticCraftingPlan.Status.UNAVAILABLE,
          plan.reason(),
          plan.branches().stream()
              .map(b -> new AutomaticCraftingPlan.Branch(b.remaining(), b.candidates()))
              .toList(),
          false,
          "https://poe2db.tw/us/Orb_of_Transmutation");
    }
    boolean basic =
        action == WorkbenchCurrency.REGAL
            || action == WorkbenchCurrency.EXALTED
            || action == WorkbenchCurrency.CHAOS
            || (recipe && BasicCurrencyPlan.ACTIONS.contains(action));
    boolean replacement = !materials.replacementTargets(action).isEmpty();
    boolean essence = !materials.essenceTargets(action).isEmpty();
    if ((!recipe && (action == WorkbenchCurrency.ESSENCE_ABYSS || action.isLiquid()))
        || (!basic && !replacement && !essence)
        || (recipe && WorkbenchOmen.verifiedDoubleAddition(omens))
        || WorkbenchOmens.matching(action, omens).size() != omens.size())
      return new AutomaticCraftingPlan(
          AutomaticCraftingPlan.Status.UNSUPPORTED,
          "AUTOMATIC_OPERATION_NOT_IMPLEMENTED",
          List.of(),
          false,
          "");
    var required =
        action == WorkbenchCurrency.REGAL || essence
            ? ItemState.Rarity.MAGIC
            : ItemState.Rarity.RARE;
    if (basic ? !recipe && state.rarity() != required : !action.materialPolicy().allows(state))
      return new AutomaticCraftingPlan(
          AutomaticCraftingPlan.Status.UNAVAILABLE, "RARITY_NOT_APPLICABLE", List.of(), !basic, "");
    var available = availability(state, action, omens);
    if (!available.available())
      return new AutomaticCraftingPlan(
          Set.of(
                      "No non-Fractured explicit can be removed.",
                      "No eligible explicit modifier can be removed with this omen.",
                      "No eligible modifier or free affix slot remains for this currency and omen.",
                      "Item level is below the currency's minimum modifier level.")
                  .contains(available.reason())
              ? AutomaticCraftingPlan.Status.UNAVAILABLE
              : AutomaticCraftingPlan.Status.UNSUPPORTED,
          available.reason(),
          List.of(),
          !basic,
          "");
    var branches = new ArrayList<AutomaticCraftingPlan.Branch>();
    var omen = omens.isEmpty() ? null : omens.getFirst();
    if (action.baseAction() == CraftingAction.CHAOS) {
      for (var removed : removalCandidates(state, action, omens)) {
        var rest = new ArrayList<>(state.explicits());
        rest.remove(removed);
        var remaining = copy(state, state.rarity(), state.implicits(), rest);
        branches.add(new AutomaticCraftingPlan.Branch(remaining, pool(remaining, action, null)));
      }
    } else if (replacement && materials.targetFirstEssence(action)) {
      var slots = AbyssCraftingState.from(state, catalog);
      var plan = materials.essenceReplacementPlan(slots, action, omen);
      for (var outcome : plan.outcomes())
        for (var removed : outcome.removals())
          branches.add(
              new AutomaticCraftingPlan.Branch(
                  slots.remove(removed.id(), null, catalog).knownItem(),
                  List.of(outcome.target()),
                  (long) plan.outcomes().size() * outcome.removals().size()));
    } else if (replacement) {
      var targets =
          materials.replacementTargets(action).stream()
              .map(id -> catalog.find(id).orElseThrow())
              .toList();
      for (var removed : replacementRemovalCandidates(state, action, omen)) {
        var rest = new ArrayList<>(state.explicits());
        rest.remove(removed);
        var remaining = copy(state, state.rarity(), state.implicits(), rest);
        var allowed =
            action.isLiquid()
                ? targets.stream().filter(target -> canAddFixed(remaining, target)).toList()
                : targets;
        branches.add(new AutomaticCraftingPlan.Branch(remaining, allowed));
      }
    } else {
      var upgraded =
          copy(
              state,
              essence ? action.materialPolicy().result(state) : upgrade(state, action),
              state.implicits(),
              state.explicits());
      branches.add(
          new AutomaticCraftingPlan.Branch(
              upgraded,
              essence
                  ? materials.essenceTargets(action).stream()
                      .map(id -> catalog.find(id).orElseThrow())
                      .toList()
                  : pool(upgraded, action, omen)));
    }
    return new AutomaticCraftingPlan(
        AutomaticCraftingPlan.Status.READY,
        "",
        branches,
        !basic,
        replacement
            ? action.replacementSource()
            : essence
                ? Optional.ofNullable(action.essenceChoiceSource())
                    .orElse(branches.getFirst().candidates().getFirst().sourceUrl())
                : "https://poe2db.tw/us/"
                    + (action == WorkbenchCurrency.REGAL
                        ? "Regal_Orb"
                        : action == WorkbenchCurrency.CHAOS ? "Chaos_Orb" : "Exalted_Orb"));
  }

  BasicCurrencyPlan basicCurrencyPlan(ItemState state, WorkbenchCurrency action) {
    validate(state);
    if (!BasicCurrencyPlan.ACTIONS.contains(action))
      throw new IllegalArgumentException("Action is outside the basic currency transition scope");
    var available = availability(state, action, List.of());
    if (!available.available()) return new BasicCurrencyPlan(false, available.reason(), List.of());
    var branches = new ArrayList<BasicCurrencyPlan.Branch>();
    if (action.baseAction() == CraftingAction.ANNULMENT
        || action.baseAction() == CraftingAction.CHAOS) {
      for (var removed : removalCandidates(state, action, List.of())) {
        var explicits = new ArrayList<>(state.explicits());
        explicits.remove(removed);
        var remaining = copy(state, state.rarity(), state.implicits(), explicits);
        branches.add(
            new BasicCurrencyPlan.Branch(
                removed,
                remaining,
                action == WorkbenchCurrency.ANNULMENT ? List.of() : pool(remaining, action, null)));
      }
    } else {
      var upgraded = copy(state, upgrade(state, action), state.implicits(), state.explicits());
      branches.add(new BasicCurrencyPlan.Branch(null, upgraded, pool(upgraded, action, null)));
    }
    return new BasicCurrencyPlan(true, "", branches);
  }

  public ItemState finishBasicCurrency(ItemState remaining, List<ModifierInstance> explicits) {
    var result =
        qualityCapChangePolicy.afterAcceptedOperation(
            copy(remaining, remaining.rarity(), remaining.implicits(), explicits), catalog);
    validate(result);
    return result;
  }

  List<Assumption> basicSelectionAssumptions(List<ModifierDefinition> candidates) {
    var assumptions = new ArrayList<Assumption>();
    rolls.recordSelectionAssumptions(candidates, assumptions);
    return List.copyOf(assumptions);
  }

  Assumption basicRemovalAssumption(List<ModifierInstance> candidates, WorkbenchCurrency action) {
    return new Assumption(
        "uniform-removal-v1",
        "eligible explicit modifier instance",
        candidates.size(),
        candidates.stream().map(ModifierInstance::modifierId).toList(),
        null,
        null,
        "https://poe2db.tw/us/"
            + (action.baseAction() == CraftingAction.CHAOS ? "Chaos_Orb" : "Orb_of_Annulment"),
        "Uniform among eligible removal instances, after affix or lowest modifier-level restriction; no published removal weights.");
  }

  public Result apply(
      ItemState state, WorkbenchCurrency action, Set<String> activeOmens, RandomGenerator random) {
    return apply(state, action, activeOmens, random, null);
  }

  /** A user-selected modifier uses ordinary roll rules; this is explicitly a custom operation. */
  public Result applyCustomExalt(
      ItemState state,
      WorkbenchCurrency action,
      Set<String> activeOmens,
      RandomGenerator random,
      String modifierId) {
    if (modifierId == null
        || exaltChoices(state, action, activeOmens).stream()
            .noneMatch(c -> c.modifierId().equals(modifierId)))
      throw new IllegalArgumentException("Choose an eligible Exalted modifier");
    return apply(state, action, activeOmens, random, modifierId);
  }

  /**
   * modifierId는 선택 가능한 고귀 옵션, probability는 현재 후보 풀에서의 선택 확률이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code modifierId}: 실제 부여된 옵션 정의 식별자 ({@code String}).
   *   <li>{@code probability}: 확률 ({@code double}).
   * </ul>
   */
  public record ExaltChoice(String modifierId, double probability) {}

  /** Natural chance to see the selected tier in the operation, including a verified second draw. */
  public List<ExaltChoice> exaltChoices(
      ItemState state, WorkbenchCurrency action, Set<String> ids) {
    validate(state);
    if (action == null || action.baseAction() != CraftingAction.EXALTED)
      throw new IllegalArgumentException("Exalted currency required");
    var omens = WorkbenchOmens.parseOmens(ids);
    if (!availability(state, action, omens).available()) return List.of();
    var matched = WorkbenchOmens.matching(action, omens);
    var first = exaltPool(state, state, action, omens, matched);
    double total = first.stream().mapToLong(ModifierDefinition::weight).sum();
    var chances = new LinkedHashMap<String, Double>();
    for (var d : first) chances.put(d.id(), d.weight() / total);
    if (exaltAdditionCount(state, matched) == 2)
      for (var d : first) {
        var values = new LinkedHashMap<String, Long>();
        d.stats().forEach(r -> values.put(r.id(), r.min()));
        var mods = new ArrayList<>(state.explicits());
        mods.add(new ModifierInstance(d.id(), values));
        var second =
            exaltPool(
                state,
                copy(state, state.rarity(), state.implicits(), mods),
                action,
                omens,
                matched);
        double secondTotal = second.stream().mapToLong(ModifierDefinition::weight).sum();
        for (var target : second)
          if (chances.containsKey(target.id()))
            chances.merge(
                target.id(), d.weight() / total * target.weight() / secondTotal, Double::sum);
      }
    return chances.entrySet().stream()
        .map(e -> new ExaltChoice(e.getKey(), Math.min(1, e.getValue())))
        .toList();
  }

  /** Shared count for availability, custom odds, execution and omen consumption. */
  private int exaltAdditionCount(ItemState state, List<WorkbenchOmen> matched) {
    if (!WorkbenchOmen.verifiedDoubleAddition(matched)) return 1;
    var slots = new ItemStateValidator(catalog).slots(state);
    var side = matched.stream().map(WorkbenchOmen::affix).filter(Objects::nonNull).findFirst();
    int free =
        side.isEmpty()
            ? slots.maxPrefixes() + slots.maxSuffixes() - state.explicits().size()
            : (side.get() == ModifierDefinition.AffixType.PREFIX
                    ? slots.maxPrefixes()
                    : slots.maxSuffixes())
                - (int)
                    state.explicits().stream()
                        .filter(
                            m ->
                                catalog.find(m.modifierId()).orElseThrow().affixType()
                                    == side.get())
                        .count();
    // The reviewed directional composition falls back to one addition and retains Greater.
    // Keep the existing two-slot requirement for the other Greater compositions.
    return side.isPresent() ? Math.min(2, Math.max(0, free)) : free >= 2 ? 2 : 0;
  }

  private List<ModifierDefinition> exaltPool(
      ItemState original,
      ItemState state,
      WorkbenchCurrency action,
      List<WorkbenchOmen> omens,
      List<WorkbenchOmen> matched) {
    if (omens.isEmpty()) return basicCurrencyPlan(original, action).candidatesFor(state);
    if (WorkbenchOmen.verifiedDoubleAddition(matched)) {
      var tags =
          matched.stream().anyMatch(WorkbenchOmen::homogenising)
              ? additionRules.existingTags(StateBucket.forModifierEligibility(original))
              : Set.<String>of();
      var side = matched.stream().map(WorkbenchOmen::affix).filter(Objects::nonNull).findFirst();
      return additionRules
          .poolWithTags(StateBucket.forModifierEligibility(state), action, tags)
          .stream()
          .filter(d -> side.isEmpty() || d.affixType() == side.get())
          .toList();
    }
    return pool(state, action, matched.isEmpty() ? null : matched.getFirst());
  }

  private Result apply(
      ItemState state,
      WorkbenchCurrency action,
      Set<String> activeOmens,
      RandomGenerator random,
      String forcedModifierId) {
    validate(state);
    if (action == null || random == null)
      throw new IllegalArgumentException("Action and random required");
    var omens = WorkbenchOmens.parseOmens(activeOmens);
    var available = availability(state, action, omens);
    var allIds = omens.stream().map(WorkbenchOmen::id).toList();
    if (!available.available())
      return new Result(
          ruleVersion(),
          ledgerVersion(),
          state.snapshotId(),
          state,
          action,
          false,
          available.reason(),
          List.of(),
          List.of(),
          List.of(),
          allIds,
          QualityLimitRules.describe(state, catalog));
    if (QualityInfuserRules.supports(action)) {
      var next = QualityInfuserRules.apply(state, action, catalog);
      return new Result(
          ruleVersion(),
          ledgerVersion(),
          state.snapshotId(),
          next,
          action,
          true,
          "",
          List.of(),
          List.of(
              new Assumption(
                  QualityInfuserRules.VERSION,
                  "quality target percentage",
                  1,
                  List.of(),
                  (long) QualityInfuserRules.current(next, action),
                  (long) QualityInfuserRules.current(next, action),
                  "https://poe2db.tw/us/Stackable_Currency",
                  "Simulator: once per item, +10 quality points up to maximum quality +10, without corruption.")),
          List.of(),
          allIds,
          QualityLimitRules.describe(next, catalog));
    }
    if (QualityCurrencyRules.isQuality(action)) {
      var next = state.withEquipmentQuality(QualityCurrencyRules.nextQuality(state));
      var assumption =
          new Assumption(
              QualityCurrencyRules.VERSION,
              "equipment quality target percentage",
              1,
              List.of(),
              (long) next.equipmentQuality(),
              (long) next.equipmentQuality(),
              "https://poe2db.tw/us/Quality",
              "User-selected simulator policy: one use sets Normal, Magic and Rare equipment quality to the ordinary cap of 20; items already at or above the cap refuse unchanged. The referenced source supports the quality type and ordinary cap, but does not establish this simulator's one-use amount as an actual-game rule.");
      return new Result(
          ruleVersion(),
          ledgerVersion(),
          state.snapshotId(),
          next,
          action,
          true,
          "",
          List.of(),
          List.of(assumption),
          List.of(),
          allIds,
          QualityLimitRules.describe(next, catalog));
    }
    if (action == WorkbenchCurrency.DIVINE && omens.contains(WorkbenchOmen.SANCTIFICATION)) {
      var next = state.withSanctification(QualitySanctification.roll(state, catalog, random));
      var consumed = List.of(WorkbenchOmen.SANCTIFICATION.id());
      var assumption =
          new Assumption(
              QualitySanctification.ASSUMPTION,
              "numeric modifier stat",
              45,
              List.of(),
              78L,
              122L,
              "https://poe2db.tw/us/Sanctified_Items",
              "User-selected simulator policy: independent uniform integer percentages for eligible explicit stats only. Fractured modifiers, implicits and structural metadata remain unchanged. Round the sanctified value, then apply and round typed quality. Original rolls and factors remain stored.");
      return new Result(
          ruleVersion(),
          ledgerVersion(),
          state.snapshotId(),
          next,
          action,
          true,
          "",
          List.of(),
          List.of(assumption),
          consumed,
          allIds.stream().filter(id -> !consumed.contains(id)).toList(),
          QualityLimitRules.describe(next, catalog));
    }
    if (action.catalystType() != null) {
      var quality =
          new CatalystQuality(
              action.catalystType(),
              qualityCapChangePolicy.catalystAmount(
                  state, QualityLimitRules.describe(state, catalog).maximumQuality()));
      var next =
          new ItemState(
                  state.snapshotId(),
                  state.baseItemId(),
                  state.itemLevel(),
                  state.rarity(),
                  state.implicits(),
                  state.explicits(),
                  state.conditions(),
                  state.augmentSockets(),
                  quality,
                  state.desecrated(),
                  state.equipmentQuality(),
                  state.observations(),
                  state.sanctification())
              .withAugments(state.installedAugments())
              .withQualityInfused(state.qualityInfused())
              .withObservations(state.observations());
      return new Result(
          ruleVersion(),
          ledgerVersion(),
          state.snapshotId(),
          next,
          action,
          true,
          "Simulator policy: one use sets at least maximum quality, preserves inherited higher quality and replaces its type; this is not the actual one-use game effect.",
          List.of(),
          List.of(),
          List.of(),
          allIds,
          QualityLimitRules.describe(next, catalog));
    }
    var matched = WorkbenchOmens.matching(action, omens);
    if (action == WorkbenchCurrency.ARTIFICER) {
      var next =
          new ItemState(
                  state.snapshotId(),
                  state.baseItemId(),
                  state.itemLevel(),
                  state.rarity(),
                  state.implicits(),
                  state.explicits(),
                  state.conditions(),
                  state.augmentSockets() + 1,
                  state.catalystQuality(),
                  state.desecrated(),
                  state.equipmentQuality(),
                  state.observations(),
                  state.sanctification())
              .withAugments(state.installedAugments())
              .withQualityInfused(state.qualityInfused())
              .withEquipmentQuality(state.equipmentQuality())
              .withObservations(state.observations());
      return new Result(
          ruleVersion(),
          ledgerVersion(),
          state.snapshotId(),
          next,
          action,
          true,
          "",
          List.of(),
          List.of(),
          List.of(),
          allIds,
          QualityLimitRules.describe(next, catalog));
    }
    var omen = matched.isEmpty() ? null : matched.getFirst();
    var basicPlan =
        omens.isEmpty() && BasicCurrencyPlan.ACTIONS.contains(action)
            ? basicCurrencyPlan(state, action)
            : null;
    var implicits = new ArrayList<>(state.implicits());
    var explicits = new ArrayList<>(state.explicits());
    var events = new ArrayList<Event>();
    var assumptions = new ArrayList<Assumption>();
    var rarity =
        action == WorkbenchCurrency.ALCHEMY
            ? ItemState.Rarity.RARE
            : action.materialPolicy().mode() != WorkbenchDefinitions.MaterialMode.NONE
                ? action.materialPolicy().result(state)
                : upgrade(state, action);
    if (!materials.replacementTargets(action).isEmpty()) {
      var targetPlan =
          materials.targetFirstEssence(action)
              ? materials.essenceReplacementPlan(
                  AbyssCraftingState.from(state, catalog), action, omen)
              : null;
      var outcome =
          targetPlan == null
              ? null
              : targetPlan
                  .outcomes()
                  .get(
                      targetPlan.outcomes().size() == 1
                          ? 0
                          : random.nextInt(targetPlan.outcomes().size()));
      var candidates =
          outcome == null
              ? replacementRemovalCandidates(state, action, omen)
              : outcome.removals().stream().map(AbyssCraftingState.Slot::modifier).toList();
      var removed = candidates.get(random.nextInt(candidates.size()));
      explicits.remove(removed);
      events.add(new Event("REMOVE", removed.modifierId(), Map.of(), 1.0 / candidates.size()));
      assumptions.add(
          new Assumption(
              "uniform-removal-v1",
              "eligible explicit modifier instance",
              candidates.size(),
              candidates.stream().map(ModifierInstance::modifierId).toList(),
              null,
              null,
              omen == null ? action.replacementSource() : "https://poe2db.tw/us/" + omen.id(),
              action.isLiquid()
                  ? "USER-APPROVED SIMULATOR MODEL: uniform among removals that leave a legal Crafted side and family. Failed branches are excluded before sampling; actual game retry/failure and removal weights are unknown. Crafted capacity follows the equipped Augments."
                  : "Uniform among non-Fractured eligible instances conditional on the selected outcome. A full target side requires removal from that side. No published removal weights."));
      var afterRemoval = copy(state, state.rarity(), state.implicits(), explicits);
      var targets =
          materials.replacementTargets(action).stream()
              .filter(
                  id ->
                      !action.isLiquid()
                          || canAddFixed(afterRemoval, catalog.find(id).orElseThrow()))
              .toList();
      var definition =
          outcome != null
              ? outcome.target()
              : catalog
                  .find(targets.get(targets.size() == 1 ? 0 : random.nextInt(targets.size())))
                  .orElseThrow();
      if (targets.size() > 1 || action.isLiquid())
        assumptions.add(
            new Assumption(
                action.isLiquid() ? "uniform-liquid-outcomes-v1" : "uniform-essence-choice-v1",
                "valid sourced modifier outcome",
                targets.size(),
                targets,
                null,
                null,
                action.replacementSource(),
                action.isLiquid()
                    ? "USER-APPROVED SIMULATOR MODEL: 1/N among valid sourced Liquid outcomes after this removal and side/family eligibility. Actual outcome weights are unavailable."
                    : "Uniform among the sourced modifier outcomes; no published essence choice weights. Zero ordinary spawn weight is not an essence selection weight."));
      var rolled = rolls.roll(definition, random, assumptions).asCrafted();
      explicits.add(rolled);
      events.add(new Event("ADD", definition.id(), rolled.values(), 1.0 / targets.size()));
    } else if (!materials.essenceTargets(action).isEmpty()) {
      var candidates = materials.essenceTargets(action);
      var definition =
          catalog
              .find(candidates.get(candidates.size() == 1 ? 0 : random.nextInt(candidates.size())))
              .orElseThrow();
      if (candidates.size() > 1)
        assumptions.add(
            new Assumption(
                "uniform-essence-choice-v1",
                "fixed essence modifier outcome",
                candidates.size(),
                candidates,
                null,
                null,
                action.essenceChoiceSource() == null
                    ? catalog.metadata().sourceUrl()
                    : action.essenceChoiceSource(),
                "Uniform among the sourced modifier outcomes; no published essence choice weights. Ordinary affix pool weights are not asserted to be essence choice weights."));
      var rolled = rolls.roll(definition, random, assumptions);
      explicits.add(rolled);
      events.add(new Event("ADD", definition.id(), rolled.values(), 1.0 / candidates.size()));
    } else if (action == WorkbenchCurrency.FRACTURING) {
      int index = random.nextInt(explicits.size());
      var chosen = explicits.get(index);
      explicits.set(
          index,
          new ModifierInstance(chosen.modifierId(), chosen.values(), true, chosen.crafted()));
      events.add(
          new Event("FRACTURE", chosen.modifierId(), chosen.values(), 1.0 / explicits.size()));
      assumptions.add(
          new Assumption(
              "uniform-fracture-v1",
              "eligible explicit modifier instance",
              explicits.size(),
              explicits.stream().map(ModifierInstance::modifierId).toList(),
              null,
              null,
              "https://poe2db.tw/us/Fractured_Modifiers",
              "Uniform among supported explicit instances on this supported Rare base, including verified essence results; no published fracture selection weights."));
    } else if (action == WorkbenchCurrency.ALCHEMY) {
      if (omen != null)
        assumptions.add(
            new Assumption(
                "legacy-alchemy-order-v1",
                "affix draw order",
                4,
                List.of(),
                null,
                null,
                "https://poe2db.tw/us/Omen",
                "UNVERIFIED model order: fill the guaranteed maximum affix side first, then the other side, using published conditional weights. The card confirms maximum count, not game draw order or exact odds."));
      for (var old : explicits) events.add(new Event("REMOVE", old.modifierId(), Map.of(), 1));
      explicits.clear();
      for (int i = 0; i < 4; i++) {
        var intermediate = copy(state, rarity, implicits, explicits);
        var candidates =
            pool(intermediate, action, omen == null ? null : alchemySide(intermediate, omen));
        if (candidates.isEmpty())
          throw new IllegalArgumentException("Alchemy needs a complete four-modifier pool");
        rolls.recordSelectionAssumptions(candidates, assumptions);
        long total = candidates.stream().mapToLong(ModifierDefinition::weight).sum();
        long draw = random.nextLong(total);
        var chosen = candidates.getLast();
        for (var candidate : candidates) {
          draw -= candidate.weight();
          if (draw < 0) {
            chosen = candidate;
            break;
          }
        }
        var rolled = rolls.roll(chosen, random, assumptions);
        explicits.add(rolled);
        events.add(
            new Event("ADD", chosen.id(), rolled.values(), (double) chosen.weight() / total));
      }
    } else if (action == WorkbenchCurrency.DIVINE) {
      if (omen == WorkbenchOmen.BLESSED)
        for (int i = 0; i < implicits.size(); i++) {
          var current = implicits.get(i);
          var d = catalog.find(current.modifierId()).orElseThrow();
          if (d.stats().stream().anyMatch(s -> s.max() > s.min())) {
            var roll = rolls.roll(d, random, assumptions);
            implicits.set(i, roll);
            events.add(new Event("REROLL_IMPLICIT", d.id(), roll.values(), 1));
          }
        }
      if (omen != WorkbenchOmen.BLESSED)
        for (int i = 0; i < explicits.size(); i++) {
          var current = explicits.get(i);
          if (current.fractured()) continue;
          var d = catalog.find(current.modifierId()).orElseThrow();
          if (d.stats().stream().anyMatch(s -> s.max() > s.min())) {
            var roll = rolls.roll(d, random, assumptions);
            explicits.set(i, current.crafted() ? roll.asCrafted() : roll);
            events.add(new Event("REROLL_EXPLICIT", d.id(), roll.values(), 1));
          }
        }
    } else {
      if (action.baseAction() == CraftingAction.ANNULMENT
          || action.baseAction() == CraftingAction.CHAOS) {
        int removals = matched.contains(WorkbenchOmen.GREATER_ANNULMENT) ? 2 : 1;
        for (int removal = 0; removal < removals; removal++) {
          var candidates =
              basicPlan == null
                  ? removalCandidates(copy(state, rarity, implicits, explicits), action, matched)
                  : basicPlan.removals();
          var removed = candidates.get(random.nextInt(candidates.size()));
          explicits.remove(removed);
          events.add(new Event("REMOVE", removed.modifierId(), Map.of(), 1.0 / candidates.size()));
          if (basicPlan != null) assumptions.add(basicRemovalAssumption(candidates, action));
          else
            assumptions.add(
                new Assumption(
                    "uniform-removal-v1",
                    "eligible explicit modifier instance",
                    candidates.size(),
                    candidates.stream().map(ModifierInstance::modifierId).toList(),
                    null,
                    null,
                    omen == null
                        ? "https://poe2db.tw/us/"
                            + (action.baseAction() == CraftingAction.CHAOS
                                ? "Chaos_Orb"
                                : "Orb_of_Annulment")
                        : "https://poe2db.tw/us/" + omen.id(),
                    WorkbenchOmen.sideWhittling(matched)
                        ? "User-specified side-first composition (with symmetric suffix model): affix candidates first, then lowest modifier level, then uniform ties; not independently established game odds."
                        : "Uniform among eligible removal instances, after affix or lowest modifier-level restriction; no published removal weights."));
        }
      }
      if (action.baseAction() != CraftingAction.ANNULMENT) {
        int additions = exaltAdditionCount(state, matched);
        var homogenising = matched.stream().anyMatch(WorkbenchOmen::homogenising);
        var originalTags =
            homogenising
                ? additionRules.existingTags(StateBucket.forModifierEligibility(state))
                : Set.<String>of();
        for (int i = 0; i < additions; i++) {
          var intermediate = copy(state, rarity, implicits, explicits);
          var candidates =
              action.baseAction() == CraftingAction.EXALTED
                  ? exaltPool(state, intermediate, action, omens, matched)
                  : basicPlan != null
                      ? basicPlan.candidatesFor(intermediate)
                      : WorkbenchOmen.verifiedDoubleAddition(matched)
                          ? additionRules.poolWithTags(
                              StateBucket.forModifierEligibility(intermediate),
                              action,
                              originalTags)
                          : pool(
                              intermediate,
                              action,
                              action.baseAction() == CraftingAction.CHAOS ? null : omen);
          rolls.recordSelectionAssumptions(candidates, assumptions);
          long total = candidates.stream().mapToLong(ModifierDefinition::weight).sum();
          long draw = random.nextLong(total);
          var chosen = candidates.getLast();
          for (var candidate : candidates) {
            draw -= candidate.weight();
            if (draw < 0) {
              chosen = candidate;
              break;
            }
          }
          if (forcedModifierId != null && i == 0)
            chosen =
                candidates.stream()
                    .filter(d -> d.id().equals(forcedModifierId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Ineligible custom modifier"));
          var rolled = rolls.roll(chosen, random, assumptions);
          explicits.add(rolled);
          events.add(
              new Event("ADD", chosen.id(), rolled.values(), (double) chosen.weight() / total));
        }
      }
    }
    var beforeCapPolicy = copy(state, rarity, implicits, explicits);
    var result = qualityCapChangePolicy.afterAcceptedOperation(beforeCapPolicy, catalog);
    validate(result);
    if (BasicJewel.supportedCrafting(result.baseItemId())) {
      if (result.explicits().stream()
          .anyMatch(
              m ->
                  m.modifierId().endsWith("JewelPrefixEffect")
                      || m.modifierId().endsWith("JewelSuffixEffect")))
        assumptions.add(
            new Assumption(
                "jewel-effect-quality-projection-v1",
                "derived display only",
                1,
                List.of(),
                null,
                null,
                "https://poe2db.tw/us/Potent_Liquid_Ferocity",
                "PROVISIONAL: opposite-side effect and matching refined quality multiply from original rolls; central integer rounding once. Stored values never change. Meta and conditional modifiers are unscalable; no combat simulation."));
      var slots = new ItemStateValidator(catalog).slots(result);
      if (slots.usedPrefixes() > slots.maxPrefixes() || slots.usedSuffixes() > slots.maxSuffixes())
        assumptions.add(
            new Assumption(
                "jewel-cap-loss-preserve-v2-five-affix",
                "existing affix overflow",
                1,
                result.explicits().stream().map(ModifierInstance::modifierId).toList(),
                null,
                null,
                "https://mobalytics.gg/poe-2/profile/paintmaster/guides/recoup-chronomancer-gear-crafting-guide",
                "Secondary-evidence simulator policy: removing Contempt preserves existing excess affixes up to five total. Future insertion obeys current side capacity; removing the excess does not leave a permanent expansion. No primary proof of cap-loss behavior."));
    }
    if (action.minimumModifierLevel() > 0 && !matched.isEmpty())
      assumptions.add(
          new Assumption(
              "tiered-omen-composition-v1",
              "omen/currency effect composition",
              matched.size(),
              matched.stream().map(WorkbenchOmen::id).toList(),
              null,
              null,
              "https://poe2db.tw/us/Omen",
              "UNVERIFIED composition model: existing omen eligibility precedes the currency's minimum-added-level pool at each addition; multi-add tags remain frozen before the craft. This does not establish game combination odds or failure consumption."));
    var consumed =
        matched.stream()
            .filter(
                o ->
                    o != WorkbenchOmen.GREATER_EXALTATION
                        || exaltAdditionCount(state, matched) == 2)
            .map(WorkbenchOmen::id)
            .toList();
    var remaining = allIds.stream().filter(id -> !consumed.contains(id)).toList();
    return new Result(
        ruleVersion(),
        ledgerVersion(),
        state.snapshotId(),
        result,
        action,
        true,
        "",
        events,
        assumptions,
        consumed,
        remaining,
        QualityLimitRules.describe(result, catalog));
  }

  /** 품질 상한을 주던 옵션을 제거하면 현재 기폭제 품질이 새 상한을 넘는지 검사한다. */
  private boolean lowersCapBelowQuality(ItemState state, ModifierInstance removed) {
    if (!removed.values().containsKey(QualityLimitRules.STAT_ID)) return false;
    var remaining = state.explicits().stream().filter(m -> !m.equals(removed)).toList();
    var without =
        new ItemState(
                state.snapshotId(),
                state.baseItemId(),
                state.itemLevel(),
                state.rarity(),
                state.implicits(),
                remaining,
                state.conditions(),
                state.augmentSockets(),
                null)
            .withAugments(state.installedAugments())
            .withQualityInfused(state.qualityInfused());
    var limit = QualityLimitRules.describe(without, catalog);
    return limit == null || state.catalystQuality().amount() > limit.maximumQuality();
  }

  private static ItemState copy(
      ItemState s,
      ItemState.Rarity rarity,
      List<ModifierInstance> implicits,
      List<ModifierInstance> explicits) {
    return new ItemState(
            s.snapshotId(),
            s.baseItemId(),
            s.itemLevel(),
            rarity,
            implicits,
            explicits,
            s.conditions(),
            s.augmentSockets(),
            s.catalystQuality(),
            s.desecrated(),
            s.equipmentQuality(),
            s.observations(),
            s.sanctification())
        .withAugments(s.installedAugments())
        .withQualityInfused(s.qualityInfused());
  }

  /**
   * action은 요청 화폐, available은 가능 여부, reason은 불가능한 경우의 설명이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code WorkbenchCurrency}).
   *   <li>{@code available}: 사용가능 ({@code boolean}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   * </ul>
   */
  public record Availability(WorkbenchCurrency action, boolean available, String reason) {}

  /**
   * kind는 사건 종류, modifierId는 선택 옵션, values는 실제 굴린 수치, selectionProbability는 선택 확률이다. 생성자는 values를
   * 방어 복사하여 실행 뒤의 변경을 차단한다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code kind}: 종류 ({@code String}).
   *   <li>{@code modifierId}: 실제 부여된 옵션 정의 식별자 ({@code String}).
   *   <li>{@code values}: 출처 단위로 보존하는 수치 값 ({@code Map<String, Long>}).
   *   <li>{@code selectionProbability}: 선택·확률 ({@code double}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Event(kind, modifierId, values, selectionProbability)}: 전달받은 값으로 구성원을 초기화한다. 입력:
   *       {@code kind}(종류), {@code modifierId}(옵션·식별자), {@code values}(값목록), {@code
   *       selectionProbability}(선택·확률).
   * </ul>
   */
  public record Event(
      String kind, String modifierId, Map<String, Long> values, double selectionProbability) {
    public Event {
      values = Map.copyOf(values);
    }
  }

  /**
   * id는 가정 식별자, candidateUnit/n/candidates는 후보 단위·개수·식별자, min/max는 수치 범위, sourceUrl/reason은 근거,
   * ratioTick은 복합 수치의 비율 눈금이다. 8인자 생성자는 ratioTick=null을 사용하고 기본 생성자는 candidates를 방어 복사한다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code id}: 이 값의 식별자 ({@code String}).
   *   <li>{@code candidateUnit}: 후보·단위 ({@code String}).
   *   <li>{@code n}: n ({@code long}).
   *   <li>{@code candidates}: 후보목록 ({@code List<String>}).
   *   <li>{@code min}: 허용·표시 범위의 하한 ({@code Long}).
   *   <li>{@code max}: 허용·표시 범위의 상한 ({@code Long}).
   *   <li>{@code sourceUrl}: 검토에 사용한 원자료 URL ({@code String}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code ratioTick}: 비율·Tick ({@code Integer}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Assumption(id, candidateUnit, n, candidates, min, max, sourceUrl, reason)}: 기본 정책을
   *       보충해 공통 생성자로 위임한다. 입력: {@code id}(식별자), {@code candidateUnit}(후보·단위), {@code n}(n), {@code
   *       candidates}(후보목록), {@code min}(최소값), {@code max}(최대값), {@code sourceUrl}(출처·출처주소), {@code
   *       reason}(이유).
   *   <li>{@code Assumption(id, candidateUnit, n, candidates, min, max, sourceUrl, reason,
   *       ratioTick)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code id}(식별자), {@code candidateUnit}(후보·단위),
   *       {@code n}(n), {@code candidates}(후보목록), {@code min}(최소값), {@code max}(최대값), {@code
   *       sourceUrl}(출처·출처주소), {@code reason}(이유), {@code ratioTick}(비율·Tick).
   * </ul>
   */
  public record Assumption(
      String id,
      String candidateUnit,
      long n,
      List<String> candidates,
      Long min,
      Long max,
      String sourceUrl,
      String reason,
      Integer ratioTick) {
    public Assumption(
        String id,
        String candidateUnit,
        long n,
        List<String> candidates,
        Long min,
        Long max,
        String sourceUrl,
        String reason) {
      this(id, candidateUnit, n, candidates, min, max, sourceUrl, reason, null);
    }

    public Assumption {
      candidates = List.copyOf(candidates);
    }
  }

  /**
   * ruleVersion/ledgerVersion/snapshotId는 재현 기준, state는 결과 상태, action은 화폐, applied/reason은 실행 여부·거부
   * 사유, events/assumptions는 실제 사건·추정 근거, consumedOmens/remainingOmens는 징조 처리 결과, qualityLimit는 적용 후
   * 품질 한도다. 생성자는 사건·가정·징조 목록을 방어 복사한다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code ruleVersion}: 규칙·버전 ({@code String}).
   *   <li>{@code ledgerVersion}: 규칙장부·버전 ({@code String}).
   *   <li>{@code snapshotId}: 카탈로그 스냅샷 식별자 ({@code String}).
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code ItemState}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code WorkbenchCurrency}).
   *   <li>{@code applied}: 요청한 변경의 실제 적용 여부 ({@code boolean}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code events}: 실제로 선택·적용한 변경 기록 ({@code List<Event>}).
   *   <li>{@code assumptions}: 확률·수치 계산에 사용한 명시적 가정 ({@code List<Assumption>}).
   *   <li>{@code consumedOmens}: 이번 실행으로 소비된 징조 ID 목록 ({@code List<String>}).
   *   <li>{@code remainingOmens}: 실행 뒤 남은 징조 ID 목록 ({@code List<String>}).
   *   <li>{@code qualityLimit}: 품질·한도 ({@code QualityLimitRules.Limit}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code Result(ruleVersion, ledgerVersion, snapshotId, state, action, applied, reason,
   *       events, assumptions, consumedOmens, remainingOmens, qualityLimit)}: 전달받은 값으로 구성원을 초기화한다.
   *       입력: {@code ruleVersion}(규칙·버전), {@code ledgerVersion}(규칙장부·버전), {@code
   *       snapshotId}(스냅샷·식별자), {@code state}(상태), {@code action}(행동), {@code applied}(적용여부),
   *       {@code reason}(이유), {@code events}(이벤트목록), {@code assumptions}(계산가정), {@code
   *       consumedOmens}(소비된·징조목록), {@code remainingOmens}(남은·징조목록), {@code qualityLimit}(품질·한도).
   * </ul>
   */
  public record Result(
      String ruleVersion,
      String ledgerVersion,
      String snapshotId,
      ItemState state,
      WorkbenchCurrency action,
      boolean applied,
      String reason,
      List<Event> events,
      List<Assumption> assumptions,
      List<String> consumedOmens,
      List<String> remainingOmens,
      QualityLimitRules.Limit qualityLimit) {
    public Result {
      events = List.copyOf(events);
      assumptions = List.copyOf(assumptions);
      consumedOmens = List.copyOf(consumedOmens);
      remainingOmens = List.copyOf(remainingOmens);
    }
  }

  public Optional<List<CoupledStatRollModel.Cell>> automaticJointRolls(
      ModifierDefinition definition) {
    return rolls.automaticJointRolls(definition);
  }

  public Assumption singleStatRollAssumption(ModifierDefinition definition) {
    return rolls.singleStatRollAssumption(definition);
  }

  public AbyssCraftingPlan abyssPlan(
      AbyssCraftingState state, WorkbenchCurrency action, Set<String> omens) {
    return abyss.abyssPlan(state, action, omens);
  }

  public AbyssCraftingState applyAbyss(
      AbyssCraftingState state,
      WorkbenchCurrency action,
      Set<String> omens,
      RandomGenerator random) {
    return abyss.applyAbyss(state, action, omens, random);
  }

  public AbyssCraftingState applyCustomExalt(
      AbyssCraftingState state,
      WorkbenchCurrency action,
      Set<String> omens,
      RandomGenerator random,
      String modifierId) {
    return abyss.applyCustomExalt(state, action, omens, random, modifierId);
  }
}
