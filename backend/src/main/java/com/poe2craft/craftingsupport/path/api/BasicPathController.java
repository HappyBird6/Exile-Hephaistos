package com.poe2craft.craftingsupport.path.api;

import com.fasterxml.jackson.databind.*;
import com.poe2craft.craftingsupport.path.application.BasicPathService;
import com.poe2craft.craftingsupport.path.application.FirstHitCalculator;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import com.poe2craft.shared.probability.Fraction;
import java.io.IOException;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/**
 * 고정 제작 경로의 확률·복구 요청을 계산 서비스에 전달한다.
 *
 * <p>필드·상수:
 *
 * <ul>
 *   <li>{@code service}: 요청 처리를 위임하는 서비스 ({@code BasicPathService}).
 *   <li>{@code reader}: 읽기변환기 ({@code ObjectReader}).
 * </ul>
 *
 * <p>생성자·메서드:
 *
 * <ul>
 *   <li>{@code BasicPathController(service, mapper)}: 전달받은 값으로 구성원을 초기화한다. 입력: {@code
 *       service}(서비스), {@code mapper}(변환기).
 *   <li>{@code provenance()}: 출처이력 값을 조회·구성한다. 반환: {@code BasicCurrencyState.Provenance}.
 *   <li>{@code firstHit(body)}: 최초·목표도달 처리에서 {@code service.calculate}, {@code
 *       Thread.currentThread}을(를) 사용한다. 입력: {@code body}(본문). 반환: {@code Response}.
 *   <li>{@code recovery(body)}: 복구 처리에서 {@code service.calculate}, {@code Thread.currentThread}을(를)
 *       사용한다. 입력: {@code body}(본문). 반환: {@code Response}.
 *   <li>{@code request(body)}: 계산·실행에 사용할 요청 값을 구성한다. 입력: {@code body}(본문). 반환: {@code
 *       BasicPathService.Request}. 사용하는 예외: {@code IllegalArgumentException}.
 *   <li>{@code response(request, result)}: 외부로 반환할 응답 값을 구성한다. 입력: {@code request}(요청), {@code
 *       result}(결과). 반환: {@code Response}.
 * </ul>
 *
 * <p>중첩 타입:
 *
 * <ul>
 *   <li>{@code Probability}: {@code BasicPathController}에서 사용하는 확률의 불변 값이다.
 *   <li>{@code Point}: {@code BasicPathController}에서 사용하는 지점의 불변 값이다.
 *   <li>{@code Response}: {@code BasicPathController}에서 사용하는 응답의 불변 값이다.
 *   <li>{@code RenewalProof}: {@code BasicPathController}에서 사용하는 갱신·증명의 불변 값이다.
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/crafting/basic-paths")
public final class BasicPathController {
  private final BasicPathService service;
  private final ObjectReader reader;

  public BasicPathController(BasicPathService service, ObjectMapper mapper) {
    this.service = service;
    reader =
        mapper
            .copy()
            .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
            .readerFor(BasicPathService.Request.class)
            .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .with(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
            .without(DeserializationFeature.ACCEPT_FLOAT_AS_INT);
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
      var normalized = body.deepCopy();
      var observations = normalized.get("observations");
      if (observations != null && observations.isArray()) {
        var array = (com.fasterxml.jackson.databind.node.ArrayNode) observations;
        for (int i = 0; i < array.size(); i++) {
          var value = array.get(i);
          if (value.isTextual()) {
            if (!value.textValue().matches("0|[1-9][0-9]*"))
              throw new IllegalArgumentException("Exact nonnegative decimal observations required");
            array.set(
                i,
                com.fasterxml.jackson.databind.node.LongNode.valueOf(
                    Long.parseLong(value.textValue())));
          } else if (!value.isIntegralNumber() || !value.canConvertToLong())
            throw new IllegalArgumentException("Exact integer observations required");
        }
      }
      return reader.readValue(normalized);
    } catch (IOException e) {
      throw new IllegalArgumentException("Invalid or unsupported path request fields");
    }
  }

  /**
   * {@code BasicPathController}에서 사용하는 확률의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code numerator}: 분자 ({@code String}).
   *   <li>{@code denominator}: 분모 ({@code String}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code of(f)}: 입력값으로 검증된 값 객체를 만든다. 입력: {@code f}(f). 반환: {@code Probability}.
   * </ul>
   */
  public record Probability(String numerator, String denominator) {
    static Probability of(Fraction f) {
      return new Probability(f.numerator().toString(), f.denominator().toString());
    }
  }

  /**
   * {@code BasicPathController}에서 사용하는 지점의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code attempts}: 시도수 ({@code String}).
   *   <li>{@code lower}: 하한 ({@code Probability}).
   *   <li>{@code upper}: 상한 ({@code Probability}).
   *   <li>{@code active}: 활성 ({@code Probability}).
   *   <li>{@code dead}: 종료된 ({@code Probability}).
   *   <li>{@code unresolved}: 미해결 ({@code Probability}).
   *   <li>{@code status}: 현재 처리 상태 ({@code String}).
   *   <li>{@code reachability}: reachability ({@code String}).
   * </ul>
   *
   * <p>생성자·메서드:
   *
   * <ul>
   *   <li>{@code of(p)}: 입력값으로 검증된 값 객체를 만든다. 입력: {@code p}(p). 반환: {@code Point}.
   * </ul>
   */
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

  /**
   * {@code BasicPathController}에서 사용하는 응답의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code purpose}: 목적 ({@code String}).
   *   <li>{@code request}: 작업 생성에 사용한 요청 ({@code BasicPathService.Request}).
   *   <li>{@code provenance}: 출처이력 ({@code BasicCurrencyState.Provenance}).
   *   <li>{@code interpretation}: 해석 ({@code String}).
   *   <li>{@code points}: 지점목록 ({@code List<Point>}).
   *   <li>{@code evaluations}: 평가목록 ({@code long}).
   *   <li>{@code peakFrontier}: peak·미탐색경계 ({@code int}).
   *   <li>{@code peakFractionBits}: peak·유리수·비트수 ({@code int}).
   *   <li>{@code reason}: 거절·중단·미지원 이유 ({@code String}).
   *   <li>{@code computationLimits}: computation·한도목록 ({@code BasicPathService.Limits}).
   *   <li>{@code blockers}: 차단사유목록 ({@code List<BasicPathService.Blocker>}).
   *   <li>{@code blockersTruncated}: 차단사유목록·Truncated ({@code boolean}).
   *   <li>{@code recoveryIncludedInMain}: 복구·Included·입력·Main ({@code boolean}).
   *   <li>{@code renewalProof}: 갱신·증명 ({@code RenewalProof}).
   *   <li>{@code fractionMetricScope}: 유리수·Metric·범위 ({@code String}).
   * </ul>
   */
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
      boolean recoveryIncludedInMain,
      RenewalProof renewalProof,
      String fractionMetricScope) {}

  /**
   * {@code BasicPathController}에서 사용하는 갱신·증명의 불변 값이다.
   *
   * <p>필드·상수·record 구성요소:
   *
   * <ul>
   *   <li>{@code emptyState}: 빈값·상태 ({@code BasicCurrencyState}).
   *   <li>{@code targetModifierId}: 대상·옵션·식별자 ({@code String}).
   *   <li>{@code probability}: 확률 ({@code Probability}).
   *   <li>{@code eligibleModifierIds}: 적격·옵션·식별자목록 ({@code List<String>}).
   *   <li>{@code proofVersion}: 증명·버전 ({@code String}).
   * </ul>
   */
  public record RenewalProof(
      BasicCurrencyState emptyState,
      String targetModifierId,
      Probability probability,
      List<String> eligibleModifierIds,
      String proofVersion) {}

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
        false,
        result.renewalProof() == null
            ? null
            : new RenewalProof(
                result.renewalProof().emptyState(),
                result.renewalProof().targetModifierId(),
                Probability.of(result.renewalProof().probability()),
                result.renewalProof().eligibleModifierIds(),
                result.renewalProof().proofVersion()),
        "OBSERVED_STORED_FRACTIONS_NOT_TOTAL_ARITHMETIC_OR_KERNEL_HIGH_WATER");
  }
}
