package com.poe2craft.crafting.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe2craft.crafting.application.WorkbenchService;
import com.poe2craft.crafting.domain.WorkbenchCurrency;
import com.poe2craft.crafting.domain.WorkbenchSimulator;
import com.poe2craft.crafting.infrastructure.CraftingRegistryLoader;
import com.poe2craft.item.ItemState;
import com.poe2craft.item.SolarTextMapper;
import com.poe2craft.item.testparser.ItemTextService;
import java.security.SecureRandom;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crafting/workbench")
public final class WorkbenchController {
  private final WorkbenchService simulator;
  private final SolarTextMapper mapper;
  private final ItemTextService parser;
  private final ObjectMapper json;
  private final WorkbenchStateReader stateReader;
  private final SecureRandom random = new SecureRandom();
  private final JsonNode registry = CraftingRegistryLoader.load();

  public WorkbenchController(
      WorkbenchService simulator,
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

  @GetMapping("/initial")
  public WorkbenchService.Initial initial(
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
  public WorkbenchService.QualityDisplay qualityDisplay(@RequestBody ActionRequest request) {
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
      com.poe2craft.item.ModifierDefinition.AffixType side,
      JsonNode candidates,
      Integer candidateIndex,
      WorkbenchCurrency action,
      Set<String> activeOmens,
      Set<String> targetIds) {}

  private com.poe2craft.crafting.domain.AbyssCraftingState abyssState(JsonNode input) {
    return stateReader.abyssState(input);
  }

  private List<com.poe2craft.item.ModifierInstance> abyssCandidates(JsonNode input) {
    if (input == null || !input.isArray() || input.size() != 3)
      throw new IllegalArgumentException("Three observed candidates required");
    validateAbyssNumbers(input);
    try {
      return json.readerFor(
              new com.fasterxml.jackson.core.type.TypeReference<
                  List<com.poe2craft.item.ModifierInstance>>() {})
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
  public com.poe2craft.crafting.domain.AbyssCraftingState validateAbyss(
      @RequestBody AbyssRequest request) {
    return abyssState(request.state());
  }

  @PostMapping("/abyss/import")
  public com.poe2craft.crafting.domain.AbyssCraftingState importAbyss(
      @RequestBody ActionRequest request) {
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
              item.observations());
    if (!new com.poe2craft.item.ItemStateValidator(catalog).validate(validationItem).isEmpty())
      throw new IllegalArgumentException("Invalid slot-aware item state");
    return com.poe2craft.crafting.domain.AbyssCraftingState.from(item, catalog);
  }

  public record DesecrateRequest(
      JsonNode state,
      String currency,
      com.poe2craft.item.ModifierDefinition.AffixType side,
      String removedSlotId,
      Set<String> activeOmens,
      List<com.poe2craft.item.ModifierDefinition.AffixType> observedSides) {}

  @PostMapping("/abyss/desecrate")
  public com.poe2craft.crafting.domain.AbyssCraftingState desecrate(
      @RequestBody DesecrateRequest request) {
    var item = concreteState(request.state(), true);
    var catalog = simulator.abyssCatalog(item);
    if (request.activeOmens() != null && request.activeOmens().contains("Omen_of_Putrefaction"))
      return com.poe2craft.crafting.domain.DesecrationCurrency.putrefy(
          item, request.currency(), request.observedSides(), request.activeOmens(), catalog);
    if (request.observedSides() != null && !request.observedSides().isEmpty())
      throw new IllegalArgumentException("Unexpected observed slots");
    return com.poe2craft.crafting.domain.DesecrationCurrency.apply(
        item,
        request.currency(),
        request.side(),
        request.removedSlotId(),
        request.activeOmens(),
        catalog,
        random);
  }

  @PostMapping("/abyss/add")
  public com.poe2craft.crafting.domain.AbyssCraftingState addAbyss(
      @RequestBody AbyssRequest request) {
    var state = abyssState(request.state());
    return state.addUnrevealed(
        request.slotId(), request.side(), simulator.abyssCatalog(state.item()));
  }

  @PostMapping("/abyss/observe")
  public com.poe2craft.crafting.domain.AbyssCraftingState observeAbyss(
      @RequestBody AbyssRequest request) {
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
  public com.poe2craft.crafting.domain.AbyssCraftingState selectReveal(
      @RequestBody RevealRequest request) throws java.io.IOException {
    var state = abyssState(request.state());
    if (request.candidate() == null) throw new IllegalArgumentException("Candidate required");
    validateAbyssNumbers(request.candidate());
    var candidate =
        json.readerFor(com.poe2craft.item.ModifierInstance.class)
            .with(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .without(com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .readValue(request.candidate().traverse(json));
    return state.selectDirect(
        request.slotId(),
        (com.poe2craft.item.ModifierInstance) candidate,
        simulator.abyssCatalog(state.item()));
  }

  @PostMapping("/abyss/choose")
  public com.poe2craft.crafting.domain.AbyssCraftingState chooseAbyss(
      @RequestBody AbyssRequest request) {
    var state = abyssState(request.state());
    if (request.candidateIndex() == null) throw new IllegalArgumentException("Candidate required");
    return state.choose(request.candidateIndex(), simulator.abyssCatalog(state.item()));
  }

  @PostMapping("/abyss/plan")
  public com.poe2craft.crafting.domain.AbyssCraftingPlan planAbyss(
      @RequestBody AbyssRequest request) {
    return simulator.abyssPlan(
        abyssState(request.state()), request.action(), request.activeOmens());
  }

  @PostMapping("/abyss/apply")
  public com.poe2craft.crafting.domain.AbyssCraftingState applyAbyss(
      @RequestBody AbyssRequest request) {
    return simulator.applyAbyss(
        abyssState(request.state()), request.action(), request.activeOmens(), random);
  }

  @PostMapping("/abyss/currency")
  public com.poe2craft.crafting.domain.AbyssCurrencyExecutor.Result applyAbyssCurrency(
      @RequestBody AbyssRequest request) {
    return simulator.applyAbyssCurrency(
        abyssState(request.state()), request.action(), request.activeOmens(), random);
  }

  @PostMapping("/abyss/compare")
  public com.poe2craft.crafting.domain.AbyssProbabilityComparison.Comparison compareAbyss(
      @RequestBody AbyssRequest request) {
    var state = abyssState(request.state());
    if (request.targetIds() == null || request.targetIds().isEmpty())
      throw new IllegalArgumentException("Target modifiers required");
    var omenIds = request.activeOmens() == null ? Set.<String>of() : request.activeOmens();
    if (omenIds.size() > 1)
      throw new IllegalArgumentException("At most one addition omen supported");
    var omen =
        omenIds.isEmpty()
            ? null
            : com.poe2craft.crafting.domain.WorkbenchOmen.fromId(omenIds.iterator().next());
    var plan = simulator.abyssPlan(state, request.action(), omenIds);
    if (!plan.available()) throw new IllegalArgumentException("Next addition unavailable");
    return new com.poe2craft.crafting.domain.AbyssProbabilityComparison(
            simulator.abyssCatalog(state.item()))
        .compare(state, request.slotId(), request.action(), omen, request.targetIds())
        .orElse(null);
  }

  @PostMapping("/map-text")
  public SolarTextMapper.Result mapText(@RequestBody TextRequest request) {
    return mapper.map(parser.parseText(request.text()));
  }

  public record TextRequest(String text) {}
}
