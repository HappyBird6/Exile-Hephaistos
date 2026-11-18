package com.poe2craft.workbench.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.shared.crafting.application.CraftingOperations;
import com.poe2craft.shared.crafting.domain.AbyssCraftingPlan;
import com.poe2craft.shared.crafting.domain.AbyssCraftingState;
import com.poe2craft.shared.crafting.domain.AbyssCurrencyExecutor;
import com.poe2craft.shared.crafting.domain.AbyssProbabilityComparison;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.crafting.infrastructure.CraftingRegistryLoader;
import com.poe2craft.shared.item.SolarTextMapper;
import com.poe2craft.shared.item.text.ItemTextService;
import com.poe2craft.workbench.api.dto.AbyssRequest;
import com.poe2craft.workbench.api.dto.ActionRequest;
import com.poe2craft.workbench.api.dto.ApplyRequest;
import com.poe2craft.workbench.api.dto.AugmentRequest;
import com.poe2craft.workbench.api.dto.DesecrateRequest;
import com.poe2craft.workbench.api.dto.EditTextRequest;
import com.poe2craft.workbench.api.dto.RevealRequest;
import com.poe2craft.workbench.api.dto.TextRequest;
import com.poe2craft.workbench.application.AbyssWorkbenchService;
import com.poe2craft.workbench.application.AugmentResult;
import com.poe2craft.workbench.application.WorkbenchItemService;
import com.poe2craft.workbench.application.WorkbenchStateReader;
import java.security.SecureRandom;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * 작업대 HTTP 경로를 제작·텍스트 서비스에 연결한다. 생성자는 공유 카탈로그와 파서로 요청 처리기를 구성한다. registry는 등록 자료 사본을, initial은 선택한
 * 베이스의 초기 상태를 반환한다. simulator는 초기 상태, stateReader는 입력 검증을 담당한다. items는 installAugment, apply,
 * actions, qualityDisplay의 장착·제작·조회 요청을 처리한다. abyss는 importAbyss, desecrate, addAbyss,
 * observeAbyss, eligibleReveal, selectReveal, chooseAbyss, planAbyss, applyAbyss,
 * applyAbyssCurrency, compareAbyss 요청을 처리한다. validateAbyss는 훼손 상태를 읽고 textPreparation은 mapText,
 * prepareText, editText의 텍스트 해석·편집을 담당한다.
 */
@RestController
@RequestMapping("/api/v1/crafting/workbench")
public final class WorkbenchController {
  private final CraftingOperations simulator;
  private final WorkbenchItemService items;
  private final AbyssWorkbenchService abyss;
  private final WorkbenchStateReader stateReader;
  private final com.poe2craft.workbench.application.ItemTextPreparation textPreparation;
  private final JsonNode registry = CraftingRegistryLoader.load();

  public WorkbenchController(
      CraftingOperations simulator,
      SolarTextMapper mapper,
      ItemTextService parser,
      ObjectMapper json) {
    this.simulator = simulator;
    this.stateReader = new WorkbenchStateReader(simulator, json);
    var random = new SecureRandom();
    items = new WorkbenchItemService(simulator, stateReader, random);
    abyss = new AbyssWorkbenchService(simulator, stateReader, random);
    this.textPreparation =
        new com.poe2craft.workbench.application.ItemTextPreparation(simulator, mapper, parser);
  }

  @GetMapping("/registry")
  public JsonNode registry() {
    return registry.deepCopy();
  }

  @PostMapping("/augments/install")
  public AugmentResult installAugment(@RequestBody AugmentRequest request) {
    return items.installAugment(request.state(), request.slotIndex(), request.augmentId());
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
    return items.apply(request.state(), request.action(), request.activeOmens());
  }

  @PostMapping("/actions")
  public List<WorkbenchSimulator.Availability> actions(@RequestBody ActionRequest request) {
    return items.actions(request.state(), request.activeOmens());
  }

  @PostMapping("/quality-display")
  public CraftingOperations.QualityDisplay qualityDisplay(@RequestBody ActionRequest request) {
    return items.qualityDisplay(request.state());
  }

  /** Unknown item properties must never vanish during Jackson conversion or crafting. */
  @PostMapping("/abyss/validate")
  public AbyssCraftingState validateAbyss(@RequestBody AbyssRequest request) {
    return stateReader.abyssState(request.state());
  }

  @PostMapping("/abyss/import")
  public AbyssCraftingState importAbyss(@RequestBody ActionRequest request) {
    return abyss.importAbyss(request.state());
  }

  @PostMapping("/abyss/desecrate")
  public AbyssCraftingState desecrate(@RequestBody DesecrateRequest request) {
    return abyss.desecrate(
        request.state(),
        request.side(),
        request.activeOmens(),
        request.observedSides(),
        request.currency(),
        request.removedSlotId());
  }

  @PostMapping("/abyss/add")
  public AbyssCraftingState addAbyss(@RequestBody AbyssRequest request) {
    return abyss.addAbyss(request.state(), request.slotId(), request.side());
  }

  @PostMapping("/abyss/observe")
  public AbyssCraftingState observeAbyss(@RequestBody AbyssRequest request) {
    return abyss.observeAbyss(request.state(), request.slotId(), request.candidates());
  }

  @PostMapping("/abyss/eligible")
  public List<String> eligibleReveal(@RequestBody RevealRequest request) {
    return abyss.eligibleReveal(request.state(), request.slotId());
  }

  @PostMapping("/abyss/select")
  public AbyssCraftingState selectReveal(@RequestBody RevealRequest request)
      throws java.io.IOException {
    return abyss.selectReveal(request.state(), request.slotId(), request.candidate());
  }

  @PostMapping("/abyss/choose")
  public AbyssCraftingState chooseAbyss(@RequestBody AbyssRequest request) {
    return abyss.chooseAbyss(request.state(), request.candidateIndex());
  }

  @PostMapping("/abyss/plan")
  public AbyssCraftingPlan planAbyss(@RequestBody AbyssRequest request) {
    return abyss.planAbyss(request.state(), request.action(), request.activeOmens());
  }

  @PostMapping("/abyss/apply")
  public AbyssCraftingState applyAbyss(@RequestBody AbyssRequest request) {
    return abyss.applyAbyss(request.state(), request.action(), request.activeOmens());
  }

  @PostMapping("/abyss/currency")
  public AbyssCurrencyExecutor.Result applyAbyssCurrency(@RequestBody AbyssRequest request) {
    return abyss.applyAbyssCurrency(request.state(), request.action(), request.activeOmens());
  }

  @PostMapping("/abyss/compare")
  public AbyssProbabilityComparison.Comparison compareAbyss(@RequestBody AbyssRequest request) {
    return abyss.compareAbyss(
        request.state(),
        request.slotId(),
        request.action(),
        request.activeOmens(),
        request.targetIds());
  }

  @PostMapping("/map-text")
  public SolarTextMapper.Result mapText(@RequestBody TextRequest request) {
    return textPreparation.map(request.text());
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

  @PostMapping("/edit-text")
  public SolarTextMapper.Result editText(@RequestBody EditTextRequest request) {
    return textPreparation.edit(request.text(), stateReader.concreteState(request.state(), true));
  }
}
