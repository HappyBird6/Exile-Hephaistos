package com.poe2craft.crafting.presentation;

import com.fasterxml.jackson.databind.*;
import com.poe2craft.crafting.application.BasicPathService;
import com.poe2craft.crafting.application.FirstHitCalculator;
import com.poe2craft.crafting.domain.BasicCurrencyState;
import com.poe2craft.crafting.domain.goalfilter.ExactNumericDistribution.Fraction;
import java.io.IOException;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/crafting/basic-paths")
public final class BasicPathController {
  private final BasicPathService service;
  private final ObjectReader reader;

  public BasicPathController(BasicPathService service, ObjectMapper mapper) {
    this.service = service;
    reader =
        mapper
            .readerFor(BasicPathService.Request.class)
            .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  @GetMapping("/provenance")
  public BasicCurrencyState.Provenance provenance() {
    return service.provenance();
  }

  @PostMapping("/first-hit")
  public Response firstHit(@RequestBody JsonNode body) {
    var request = request(body);
    return response(
        request, service.calculate(request, false, () -> Thread.currentThread().isInterrupted()));
  }

  @PostMapping("/recovery")
  public Response recovery(@RequestBody JsonNode body) {
    var request = request(body);
    return response(
        request, service.calculate(request, true, () -> Thread.currentThread().isInterrupted()));
  }

  private BasicPathService.Request request(JsonNode body) {
    if (body == null || !body.isObject())
      throw new IllegalArgumentException("Explicit path request required");
    try {
      return reader.readValue(body);
    } catch (IOException e) {
      throw new IllegalArgumentException("Invalid or unsupported path request fields");
    }
  }

  public record Probability(String numerator, String denominator) {
    static Probability of(Fraction f) {
      return new Probability(f.numerator().toString(), f.denominator().toString());
    }
  }

  public record Point(
      String attempts,
      Probability lower,
      Probability upper,
      Probability active,
      Probability dead,
      Probability unresolved,
      String status,
      String reachability) {
    static Point of(FirstHitCalculator.Point p) {
      return new Point(
          Long.toString(p.attempts()),
          Probability.of(p.lower()),
          Probability.of(p.upper()),
          Probability.of(p.active()),
          Probability.of(p.dead()),
          Probability.of(p.unresolved()),
          p.status(),
          !p.lower().equals(Fraction.ZERO)
              ? "REACHABLE_WITHIN_OBSERVATION"
              : !p.unresolved().equals(Fraction.ZERO)
                  ? "UNKNOWN"
                  : "NOT_REACHED_WITHIN_OBSERVATION");
    }
  }

  public record Response(
      String purpose,
      BasicPathService.Request request,
      BasicCurrencyState.Provenance provenance,
      String interpretation,
      List<Point> points,
      long evaluations,
      int peakFrontier,
      int peakFractionBits,
      String reason,
      BasicPathService.Limits computationLimits,
      List<BasicPathService.Blocker> blockers,
      boolean blockersTruncated,
      boolean recoveryIncludedInMain) {}

  private static Response response(
      BasicPathService.Request request, BasicPathService.Result result) {
    var d = result.distribution();
    return new Response(
        result.purpose(),
        request,
        result.provenance(),
        result.interpretation(),
        d.points().stream().map(Point::of).toList(),
        d.evaluations(),
        d.peakFrontier(),
        d.peakFractionBits(),
        d.reason(),
        BasicPathService.LIMITS,
        result.blockers(),
        result.blockersTruncated(),
        false);
  }
}
