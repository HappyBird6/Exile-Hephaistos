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
import java.security.SecureRandom;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crafting/workbench")
public final class WorkbenchController {
  private final CraftingOperations simulator;
  private final SolarTextMapper mapper;
  private final ItemTextService parser;
  private final ObjectMapper json;
  private final WorkbenchStateReader stateReader;
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
  }

  @GetMapping("/registry")
  public JsonNode registry() {
    return registry.deepCopy();
  }

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

  public record ApplyRequest(JsonNode state, WorkbenchCurrency action, Set<String> activeOmens) {}

  public record ActionRequest(JsonNode state, Set<String> activeOmens) {}

  @PostMapping("/quality-display")
  public CraftingOperations.QualityDisplay qualityDisplay(@RequestBody ActionRequest request) {
    return simulator.qualityDisplay(concreteState(request.state(), true));
  }

  public static final class UnsupportedItemProperties extends IllegalArgumentException {}

  /** Unknown item properties must never vanish during Jackson conversion or crafting. */
  private ItemState concreteState(JsonNode input) {
    return stateReader.concreteState(input);
  }

  private ItemState concreteState(JsonNode input, boolean allowDesecrated) {
    return stateReader.concreteState(input, allowDesecrated);
  }

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

  public record TextRequest(String text) {}
}
