package com.poe2craft.workbench.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.AbyssCraftingPlan;
import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.crafting.domain.AbyssCurrencyExecutor;
import com.poe2craft.shared.crafting.domain.AbyssProbabilityComparison;
import com.poe2craft.shared.crafting.domain.DesecrationCurrency;
import com.poe2craft.shared.crafting.domain.QualityLimitRules;
import com.poe2craft.shared.crafting.domain.WorkbenchCurrency;
import com.poe2craft.shared.crafting.domain.WorkbenchOmen;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.crafting.infrastructure.CraftingRegistryLoader;
import com.poe2craft.shared.item.ItemState;
import com.poe2craft.shared.item.ItemStateValidator;
import com.poe2craft.shared.item.ItemTextEditor;
import com.poe2craft.shared.item.ModifierDefinition;
import com.poe2craft.shared.item.ModifierInstance;
import com.poe2craft.shared.item.SocketAugments;
import com.poe2craft.shared.item.SolarTextMapper;
import com.poe2craft.shared.item.text.ItemTextService;
import com.poe2craft.workbench.application.WorkbenchStateReader;
import java.security.SecureRandom;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.*;

/**
 * 초기 아이템·일반 제작·훼손 제작·텍스트 편집 요청을 공통 제작 서비스에 전달한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code simulator}: 공통 제작 규칙 실행기 ({@code CraftingOperations}).
 *   <li>{@code mapper}: 입력·출력 표현을 변환하는 구성요소 ({@code SolarTextMapper}).
 *   <li>{@code parser}: 파서 ({@code ItemTextService}).
 *   <li>{@code json}: 이 경계의 JSON 읽기·쓰기 변환기 ({@code ObjectMapper}).
 *   <li>{@code stateReader}: 공통 아이템 입력 파서 ({@code WorkbenchStateReader}).
 *   <li>{@code textPreparation}: 가져오기·편집에서 공유하는 텍스트 분석 서비스 ({@code
 *       com.poe2craft.workbench.application.ItemTextPreparation}).
 *   <li>{@code random}: 호출 순서대로 소비하는 난수 공급자 ({@code SecureRandom}).
 *   <li>{@code registry}: 등록목록 ({@code JsonNode}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code WorkbenchController(simulator, mapper, parser, json)}: 전달받은 값으로 구성원을 초기화한다. 입력:
 *       {@code simulator}(제작시뮬레이터), {@code mapper}(변환기), {@code parser}(파서), {@code json}(JSON).
 *   <li>{@code registry()}: 등록목록 값을 조회·구성한다. 반환: {@code JsonNode}.
 *   <li>{@code installAugment(request)}: install·증강물 처리에서 {@code request.slotIndex}, {@code
 *       request.state}을(를) 사용한다. 입력: {@code request}(요청). 반환: {@code AugmentResult}. 사용하는 예외:
 *       {@code IllegalArgumentException}.
 *   <li>{@code initial(base, itemLevel, manualReveal)}: 제작을 시작할 초기 상태를 만든다. 입력: {@code base}(베이스),
 *       {@code itemLevel}(아이템·레벨), {@code manualReveal}(수동·공개). 반환: {@code
 *       CraftingOperations.Initial}.
 *   <li>{@code apply(request)}: 주어진 행동 또는 변경을 적용한 결과를 구한다. 입력: {@code request}(요청). 반환: {@code
 *       WorkbenchSimulator.Result}.
 *   <li>{@code actions(request)}: 현재 상태에서 가능한 행동과 불가 이유를 조회한다. 입력: {@code request}(요청). 반환: {@code
 *       List<WorkbenchSimulator.Availability>}.
 *   <li>{@code qualityDisplay(request)}: 품질·표시 처리에서 {@code simulator.qualityDisplay}, {@code
 *       request.state}을(를) 사용한다. 입력: {@code request}(요청). 반환: {@code
 *       CraftingOperations.QualityDisplay}.
 *   <li>{@code concreteState(input)}: 일반 아이템 JSON을 엄격히 검증해 구체 상태로 읽는다. 입력: {@code input}(입력). 반환:
 *       {@code ItemState}.
 *   <li>{@code concreteState(input, allowDesecrated)}: 일반 아이템 JSON을 엄격히 검증해 구체 상태로 읽는다. 입력: {@code
 *       input}(입력), {@code allowDesecrated}(allow·훼손). 반환: {@code ItemState}.
 *   <li>{@code abyssState(input)}: 미공개 슬롯을 포함한 훼손 JSON을 검증해 상태로 읽는다. 입력: {@code input}(입력). 반환:
 *       {@code AbyssCraftingState}.
 *   <li>{@code abyssCandidates(input)}: 훼손·후보목록 처리에서 {@code input.isArray}, {@code input.size}을(를)
 *       사용한다. 입력: {@code input}(입력). 반환: {@code List<ModifierInstance>}. 사용하는 예외: {@code
 *       IllegalArgumentException}.
 *   <li>{@code validateAbyssNumbers(node)}: 훼손·Numbers을(를) 검증한다. 입력: {@code node}(노드). 사용하는 예외:
 *       {@code IllegalArgumentException}.
 *   <li>{@code validateAbyss(request)}: 훼손을(를) 검증한다. 입력: {@code request}(요청). 반환: {@code
 *       AbyssCraftingState}.
 *   <li>{@code importAbyss(request)}: import·훼손 처리에서 {@code request.state}, {@code
 *       simulator.abyssCatalog}을(를) 사용한다. 입력: {@code request}(요청). 반환: {@code AbyssCraftingState}.
 *       사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code desecrate(request)}: 훼손 처리에서 {@code request.state}, {@code
 *       simulator.abyssCatalog}을(를) 사용한다. 입력: {@code request}(요청). 반환: {@code AbyssCraftingState}.
 *       사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code addAbyss(request)}: 훼손을(를) 추가한다. 입력: {@code request}(요청). 반환: {@code
 *       AbyssCraftingState}.
 *   <li>{@code observeAbyss(request)}: 훼손을(를) 관측값을 반영한다. 입력: {@code request}(요청). 반환: {@code
 *       AbyssCraftingState}.
 *   <li>{@code eligibleReveal(request)}: 적격·공개 처리에서 {@code request.state}, {@code
 *       state.eligibleRevealIds}을(를) 사용한다. 입력: {@code request}(요청). 반환: {@code List<String>}.
 *   <li>{@code selectReveal(request)}: 선택·공개 처리에서 {@code request.state}, {@code
 *       request.candidate}을(를) 사용한다. 입력: {@code request}(요청). 반환: {@code AbyssCraftingState}. 사용하는
 *       예외: {@code IllegalArgumentException}, {@code java.io.IOException}.
 *   <li>{@code chooseAbyss(request)}: choose·훼손 처리에서 {@code request.state}, {@code
 *       request.candidateIndex}을(를) 사용한다. 입력: {@code request}(요청). 반환: {@code AbyssCraftingState}.
 *       사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code planAbyss(request)}: 계획·훼손 처리에서 {@code simulator.abyssPlan}, {@code
 *       request.state}을(를) 사용한다. 입력: {@code request}(요청). 반환: {@code AbyssCraftingPlan}.
 *   <li>{@code applyAbyss(request)}: 훼손 슬롯을 보존하며 화폐를 실행한다. 입력: {@code request}(요청). 반환: {@code
 *       AbyssCraftingState}.
 *   <li>{@code applyAbyssCurrency(request)}: 훼손 상태의 공통 화폐 실행기로 전달한다. 입력: {@code request}(요청). 반환:
 *       {@code AbyssCurrencyExecutor.Result}.
 *   <li>{@code compareAbyss(request)}: 훼손을(를) 비교한다. 입력: {@code request}(요청). 반환: {@code
 *       AbyssProbabilityComparison.Comparison}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code mapText(request)}: 맵·텍스트 처리에서 {@code mapper.map}, {@code parser.parseText}을(를) 사용한다.
 *       입력: {@code request}(요청). 반환: {@code SolarTextMapper.Result}.
 *   <li>{@code prepareText(request)}: 텍스트을(를) 준비한다. 입력: {@code request}(요청). 반환: {@code
 *       com.poe2craft.workbench.application.ItemTextPreparation.Prepared}.
 *   <li>{@code editText(request)}: 편집·텍스트 처리에서 {@code stateReader.concreteState}, {@code
 *       request.state}을(를) 사용한다. 입력: {@code request}(요청). 반환: {@code SolarTextMapper.Result}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code AugmentResult}: {@code WorkbenchController}에서 사용하는 증강물·결과의 불변 값이다.
 *   <li>{@code AugmentRequest}: {@code WorkbenchController}에서 사용하는 증강물·요청의 불변 값이다.
 *   <li>{@code ApplyRequest}: {@code WorkbenchController}에서 사용하는 적용·요청의 불변 값이다.
 *   <li>{@code ActionRequest}: {@code WorkbenchController}에서 사용하는 행동·요청의 불변 값이다.
 *   <li>{@code AbyssRequest}: {@code WorkbenchController}에서 사용하는 훼손·요청의 불변 값이다.
 *   <li>{@code DesecrateRequest}: {@code WorkbenchController}에서 사용하는 훼손·요청의 불변 값이다.
 *   <li>{@code RevealRequest}: {@code WorkbenchController}에서 사용하는 공개·요청의 불변 값이다.
 *   <li>{@code EditTextRequest}: {@code WorkbenchController}에서 사용하는 편집·텍스트·요청의 불변 값이다.
 *   <li>{@code TextRequest}: {@code WorkbenchController}에서 사용하는 텍스트·요청의 불변 값이다.
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/crafting/workbench")
public final class WorkbenchController {
  private final CraftingOperations simulator;
  private final SolarTextMapper mapper;
  private final ItemTextService parser;
  private final ObjectMapper json;
  private final WorkbenchStateReader stateReader;
  private final com.poe2craft.workbench.application.ItemTextPreparation textPreparation;
  private final SecureRandom random = new SecureRandom();
  private final JsonNode registry = CraftingRegistryLoader.load();

  public WorkbenchController(
      CraftingOperations simulator,
      SolarTextMapper mapper,
      ItemTextService parser,
      ObjectMapper json) {
    this.simulator = simulator;
    this.mapper = mapper;
    this.parser = parser;
    this.json = json;
    this.stateReader = new WorkbenchStateReader(simulator, json);
    this.textPreparation =
        new com.poe2craft.workbench.application.ItemTextPreparation(simulator, mapper, parser);
  }

  @GetMapping("/registry")
  public JsonNode registry() {
    return registry.deepCopy();
  }

  /**
   * {@code WorkbenchController}에서 사용하는 증강물·결과의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code ruleVersion}: 규칙·버전 ({@code String}).
   *   <li>{@code ledgerVersion}: 규칙장부·버전 ({@code String}).
   *   <li>{@code snapshotId}: 카탈로그 스냅샷 식별자 ({@code String}).
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code ItemState}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code String}).
   *   <li>{@code applied}: 요청한 변경의 실제 적용 여부 ({@code boolean}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code events}: 실제로 선택·적용한 변경 기록 ({@code List<WorkbenchSimulator.Event>}).
   *   <li>{@code assumptions}: 확률·수치 계산에 사용한 명시적 가정 ({@code List<WorkbenchSimulator.Assumption>}).
   *   <li>{@code consumedOmens}: 이번 실행으로 소비된 징조 ID 목록 ({@code List<String>}).
   *   <li>{@code remainingOmens}: 실행 뒤 남은 징조 ID 목록 ({@code List<String>}).
   *   <li>{@code qualityLimit}: 품질·한도 ({@code QualityLimitRules.Limit}).
   * </ul>
   */
  public record AugmentResult(
      String ruleVersion,
      String ledgerVersion,
      String snapshotId,
      ItemState state,
      String action,
      boolean applied,
      String reason,
      List<WorkbenchSimulator.Event> events,
      List<WorkbenchSimulator.Assumption> assumptions,
      List<String> consumedOmens,
      List<String> remainingOmens,
      QualityLimitRules.Limit qualityLimit) {}

  /**
   * {@code WorkbenchController}에서 사용하는 증강물·요청의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code JsonNode}).
   *   <li>{@code slotIndex}: 슬롯·색인 ({@code JsonNode}).
   *   <li>{@code augmentId}: 증강물·식별자 ({@code String}).
   * </ul>
   */
  public record AugmentRequest(JsonNode state, JsonNode slotIndex, String augmentId) {}

  @PostMapping("/augments/install")
  public AugmentResult installAugment(@RequestBody AugmentRequest request) {
    if (request.slotIndex() == null
        || !request.slotIndex().isIntegralNumber()
        || !request.slotIndex().canConvertToInt())
      throw new IllegalArgumentException("Integral socket index required");
    var state = concreteState(request.state(), true);
    var catalog = simulator.abyssCatalog(state);
    com.poe2craft.shared.crafting.domain.CraftingItemState.resolve(state, null, catalog);
    var next = SocketAugments.install(state, request.slotIndex().intValue(), request.augmentId());
    return new AugmentResult(
        "socket-augments-v1",
        "ordinary-socket-effects-v1",
        next.snapshotId(),
        next,
        "INSTALL_AUGMENT",
        true,
        "",
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        QualityLimitRules.describe(next, simulator.abyssCatalog(next)));
  }

  @GetMapping("/initial")
  public CraftingOperations.Initial initial(
      @RequestParam(defaultValue = "solar") String base,
      @RequestParam(defaultValue = "82") int itemLevel,
      @RequestParam(defaultValue = "false") boolean manualReveal) {
    return simulator.initial(base, itemLevel, manualReveal);
  }

  @PostMapping("/apply")
  public WorkbenchSimulator.Result apply(@RequestBody ApplyRequest request) {
    return simulator.apply(
        concreteState(
            request.state(),
            request.action() == WorkbenchCurrency.DIVINE
                && request.activeOmens() != null
                && request.activeOmens().contains("Omen_of_Sanctification")),
        request.action(),
        request.activeOmens(),
        random);
  }

  @PostMapping("/actions")
  public List<WorkbenchSimulator.Availability> actions(@RequestBody ActionRequest request) {
    return simulator.actions(concreteState(request.state(), true), request.activeOmens());
  }

  /**
   * {@code WorkbenchController}에서 사용하는 적용·요청의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code JsonNode}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code WorkbenchCurrency}).
   *   <li>{@code activeOmens}: 현재 실행에서 고려할 징조 ID 목록 ({@code Set<String>}).
   * </ul>
   */
  public record ApplyRequest(JsonNode state, WorkbenchCurrency action, Set<String> activeOmens) {}

  /**
   * {@code WorkbenchController}에서 사용하는 행동·요청의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code JsonNode}).
   *   <li>{@code activeOmens}: 현재 실행에서 고려할 징조 ID 목록 ({@code Set<String>}).
   * </ul>
   */
  public record ActionRequest(JsonNode state, Set<String> activeOmens) {}

  @PostMapping("/quality-display")
  public CraftingOperations.QualityDisplay qualityDisplay(@RequestBody ActionRequest request) {
    return simulator.qualityDisplay(concreteState(request.state(), true));
  }

  /** Unknown item properties must never vanish during Jackson conversion or crafting. */
  private ItemState concreteState(JsonNode input) {
    return stateReader.concreteState(input);
  }

  private ItemState concreteState(JsonNode input, boolean allowDesecrated) {
    return stateReader.concreteState(input, allowDesecrated);
  }

  /**
   * {@code WorkbenchController}에서 사용하는 훼손·요청의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code JsonNode}).
   *   <li>{@code slotId}: 슬롯·식별자 ({@code String}).
   *   <li>{@code side}: 접두접미 ({@code ModifierDefinition.AffixType}).
   *   <li>{@code candidates}: 후보목록 ({@code JsonNode}).
   *   <li>{@code candidateIndex}: 후보·색인 ({@code Integer}).
   *   <li>{@code action}: 실행할 화폐·제작 행동 ({@code WorkbenchCurrency}).
   *   <li>{@code activeOmens}: 현재 실행에서 고려할 징조 ID 목록 ({@code Set<String>}).
   *   <li>{@code targetIds}: 대상·식별자목록 ({@code Set<String>}).
   * </ul>
   */
  public record AbyssRequest(
      JsonNode state,
      String slotId,
      ModifierDefinition.AffixType side,
      JsonNode candidates,
      Integer candidateIndex,
      WorkbenchCurrency action,
      Set<String> activeOmens,
      Set<String> targetIds) {}

  private AbyssCraftingState abyssState(JsonNode input) {
    return stateReader.abyssState(input);
  }

  private List<ModifierInstance> abyssCandidates(JsonNode input) {
    if (input == null || !input.isArray() || input.size() != 3)
      throw new IllegalArgumentException("Three observed candidates required");
    validateAbyssNumbers(input);
    try {
      return json.readerFor(
              new com.fasterxml.jackson.core.type.TypeReference<List<ModifierInstance>>() {})
          .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .without(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_FLOAT_AS_INT)
          .readValue(input.traverse(json));
    } catch (java.io.IOException invalid) {
      throw new IllegalArgumentException("Invalid observed candidates");
    }
  }

  private void validateAbyssNumbers(JsonNode node) {
    if (node.isObject() && node.has("values")) {
      var values = node.get("values");
      if (!values.isObject()) throw new IllegalArgumentException("Integral stat values required");
      values
          .elements()
          .forEachRemaining(
              value -> {
                if (!value.isIntegralNumber() || !value.canConvertToLong())
                  throw new IllegalArgumentException("Integral stat values required");
              });
    }
    if (node.isContainerNode()) node.elements().forEachRemaining(this::validateAbyssNumbers);
  }

  @PostMapping("/abyss/validate")
  public AbyssCraftingState validateAbyss(@RequestBody AbyssRequest request) {
    return abyssState(request.state());
  }

  @PostMapping("/abyss/import")
  public AbyssCraftingState importAbyss(@RequestBody ActionRequest request) {
    var item = concreteState(request.state(), true);
    var catalog = simulator.abyssCatalog(item);
    var validationItem = item;
    if (item.conditions().equals(Set.of(ItemState.Condition.CORRUPTED))
        && !item.desecrated().isEmpty())
      validationItem =
          new ItemState(
                  item.snapshotId(),
                  item.baseItemId(),
                  item.itemLevel(),
                  item.rarity(),
                  item.implicits(),
                  item.explicits(),
                  Set.of(),
                  item.augmentSockets(),
                  item.catalystQuality(),
                  item.desecrated(),
                  item.equipmentQuality(),
                  item.observations())
              .withAugments(item.installedAugments())
              .withQualityInfused(item.qualityInfused());
    if (!new ItemStateValidator(catalog).validate(validationItem).isEmpty())
      throw new IllegalArgumentException("Invalid slot-aware item state");
    return AbyssCraftingState.from(item, catalog);
  }

  /**
   * {@code WorkbenchController}에서 사용하는 훼손·요청의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code JsonNode}).
   *   <li>{@code currency}: 화폐 ({@code String}).
   *   <li>{@code side}: 접두접미 ({@code ModifierDefinition.AffixType}).
   *   <li>{@code removedSlotId}: 제거된·슬롯·식별자 ({@code String}).
   *   <li>{@code activeOmens}: 현재 실행에서 고려할 징조 ID 목록 ({@code Set<String>}).
   *   <li>{@code observedSides}: 관측한·Sides ({@code List<ModifierDefinition.AffixType>}).
   * </ul>
   */
  public record DesecrateRequest(
      JsonNode state,
      String currency,
      ModifierDefinition.AffixType side,
      String removedSlotId,
      Set<String> activeOmens,
      List<ModifierDefinition.AffixType> observedSides) {}

  @PostMapping("/abyss/desecrate")
  public AbyssCraftingState desecrate(@RequestBody DesecrateRequest request) {
    var item = concreteState(request.state(), true);
    var catalog = simulator.abyssCatalog(item);
    if (request.activeOmens() != null && request.activeOmens().contains("Omen_of_Putrefaction"))
      return DesecrationCurrency.putrefy(
          item, request.currency(), request.observedSides(), request.activeOmens(), catalog);
    if (request.observedSides() != null && !request.observedSides().isEmpty())
      throw new IllegalArgumentException("Unexpected observed slots");
    return DesecrationCurrency.apply(
        item,
        request.currency(),
        request.side(),
        request.removedSlotId(),
        request.activeOmens(),
        catalog,
        random);
  }

  @PostMapping("/abyss/add")
  public AbyssCraftingState addAbyss(@RequestBody AbyssRequest request) {
    var state = abyssState(request.state());
    return state.addUnrevealed(
        request.slotId(), request.side(), simulator.abyssCatalog(state.item()));
  }

  @PostMapping("/abyss/observe")
  public AbyssCraftingState observeAbyss(@RequestBody AbyssRequest request) {
    var state = abyssState(request.state());
    return state.observe(
        request.slotId(),
        abyssCandidates(request.candidates()),
        simulator.abyssCatalog(state.item()));
  }

  /**
   * {@code WorkbenchController}에서 사용하는 공개·요청의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code JsonNode}).
   *   <li>{@code slotId}: 슬롯·식별자 ({@code String}).
   *   <li>{@code candidate}: 후보 ({@code JsonNode}).
   * </ul>
   */
  public record RevealRequest(JsonNode state, String slotId, JsonNode candidate) {}

  @PostMapping("/abyss/eligible")
  public List<String> eligibleReveal(@RequestBody RevealRequest request) {
    var state = abyssState(request.state());
    return state.eligibleRevealIds(request.slotId(), simulator.abyssCatalog(state.item()));
  }

  @PostMapping("/abyss/select")
  public AbyssCraftingState selectReveal(@RequestBody RevealRequest request)
      throws java.io.IOException {
    var state = abyssState(request.state());
    if (request.candidate() == null) throw new IllegalArgumentException("Candidate required");
    validateAbyssNumbers(request.candidate());
    var candidate =
        json.readerFor(ModifierInstance.class)
            .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .without(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .readValue(request.candidate().traverse(json));
    return state.selectDirect(
        request.slotId(), (ModifierInstance) candidate, simulator.abyssCatalog(state.item()));
  }

  @PostMapping("/abyss/choose")
  public AbyssCraftingState chooseAbyss(@RequestBody AbyssRequest request) {
    var state = abyssState(request.state());
    if (request.candidateIndex() == null) throw new IllegalArgumentException("Candidate required");
    return state.choose(request.candidateIndex(), simulator.abyssCatalog(state.item()));
  }

  @PostMapping("/abyss/plan")
  public AbyssCraftingPlan planAbyss(@RequestBody AbyssRequest request) {
    return simulator.abyssPlan(
        abyssState(request.state()), request.action(), request.activeOmens());
  }

  @PostMapping("/abyss/apply")
  public AbyssCraftingState applyAbyss(@RequestBody AbyssRequest request) {
    return simulator.applyAbyss(
        abyssState(request.state()), request.action(), request.activeOmens(), random);
  }

  @PostMapping("/abyss/currency")
  public AbyssCurrencyExecutor.Result applyAbyssCurrency(@RequestBody AbyssRequest request) {
    return simulator.applyAbyssCurrency(
        abyssState(request.state()), request.action(), request.activeOmens(), random);
  }

  @PostMapping("/abyss/compare")
  public AbyssProbabilityComparison.Comparison compareAbyss(@RequestBody AbyssRequest request) {
    var state = abyssState(request.state());
    if (request.targetIds() == null || request.targetIds().isEmpty())
      throw new IllegalArgumentException("Target modifiers required");
    var omenIds = request.activeOmens() == null ? Set.<String>of() : request.activeOmens();
    if (omenIds.size() > 1)
      throw new IllegalArgumentException("At most one addition omen supported");
    var omen = omenIds.isEmpty() ? null : WorkbenchOmen.fromId(omenIds.iterator().next());
    var plan = simulator.abyssPlan(state, request.action(), omenIds);
    if (!plan.available()) throw new IllegalArgumentException("Next addition unavailable");
    return new AbyssProbabilityComparison(simulator.abyssCatalog(state.item()))
        .compare(state, request.slotId(), request.action(), omen, request.targetIds())
        .orElse(null);
  }

  @PostMapping("/map-text")
  public SolarTextMapper.Result mapText(@RequestBody TextRequest request) {
    return mapper.map(parser.parseText(request.text()));
  }

  @PostMapping("/prepare-text")
  public com.poe2craft.workbench.application.ItemTextPreparation.Prepared prepareText(
      @RequestBody EditTextRequest request) {
    var before =
        request.state() == null || request.state().isNull()
            ? null
            : stateReader.concreteState(request.state(), true);
    return textPreparation.prepare(request.text(), before);
  }

  /**
   * {@code WorkbenchController}에서 사용하는 편집·텍스트·요청의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code state}: 현재 아이템 또는 계산 상태 ({@code JsonNode}).
   *   <li>{@code text}: 원문 또는 표시 문장 ({@code String}).
   * </ul>
   */
  public record EditTextRequest(JsonNode state, String text) {}

  @PostMapping("/edit-text")
  public SolarTextMapper.Result editText(@RequestBody EditTextRequest request) {
    var before = stateReader.concreteState(request.state(), true);
    var catalog =
        simulator.reviewedCatalogs().stream()
            .filter(c -> c.base().id().equals(before.baseItemId()))
            .findFirst()
            .orElseThrow();
    return ItemTextEditor.merge(before, mapper.map(parser.parseText(request.text())), catalog);
  }

  /**
   * {@code WorkbenchController}에서 사용하는 텍스트·요청의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code text}: 원문 또는 표시 문장 ({@code String}).
   * </ul>
   */
  public record TextRequest(String text) {}
}
