package com.poe2craft.crafting.presentation.goalfilter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.poe2craft.crafting.application.goalfilter.GoalFilterService;
import com.poe2craft.crafting.domain.goalfilter.*;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Context;
import com.poe2craft.item.ItemState;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crafting/support/goal-filters")
public final class GoalFilterController {
  private final GoalFilterService service;
  private final ObjectMapper json;

  public GoalFilterController(GoalFilterService service, ObjectMapper json) {
    this.service = service;
    this.json =
        json.copy()
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
  }

  @GetMapping("/catalog")
  public GoalCatalog catalog(
      @RequestParam String snapshotId,
      @RequestParam String baseItemId,
      @RequestParam Integer itemLevel) {
    return service.catalog(new Context(snapshotId, baseItemId, itemLevel));
  }

  @PostMapping("/validate")
  public GoalValidator.Validation validate(@RequestBody JsonNode body) {
    var request = decode(body, ValidateRequest.class);
    return service.validate(request.context(), request.goal());
  }

  @PostMapping("/evaluate")
  public GoalEvaluator.Evaluation evaluate(@RequestBody JsonNode body) {
    var request = decode(body, EvaluateRequest.class);
    return service.evaluate(request.item(), request.goal());
  }

  @PostMapping("/recommend")
  public GoalFilterService.Recommendation recommend(@RequestBody JsonNode body) {
    var request = decode(body, RecommendRequest.class);
    return service.recommend(
        request.item(), request.goal(), request.activeOmens(), request.limits());
  }

  private <T> T decode(JsonNode body, Class<T> type) {
    if (body == null || !body.isObject()) throw new MalformedPayload();
    try {
      return json.treeToValue(body, type);
    } catch (ValueInstantiationException error) {
      throw new GoalFilterService.InvalidGoal(
          List.of(
              GoalFilter.Issue.error(
                  "INVALID_ITEM", "/item", "ItemState properties are invalid.")));
    } catch (JsonProcessingException error) {
      throw new MalformedPayload();
    }
  }

  public static final class MalformedPayload extends IllegalArgumentException {}

  public record ValidateRequest(Context context, GoalFilter goal) {}

  public record EvaluateRequest(ItemState item, GoalFilter goal) {}

  public record RecommendRequest(
      ItemState item, GoalFilter goal, Set<String> activeOmens, GoalFilterService.Limits limits) {}
}
