package com.poe2craft.craftingsupport.path.api.dto;

import com.poe2craft.craftingsupport.path.application.BasicPathService;
import com.poe2craft.shared.crafting.domain.BasicCurrencyState;
import java.util.List;

/**
 * purpose, request, provenance, interpretation은 계산 목적·입력·규칙 출처·해석을 담는다. points는 시도 횟수별 확률이며
 * evaluations, peakFrontier, peakFractionBits는 평가 횟수와 최대 탐색 폭·분수 크기다. reason과 computationLimits는 중단
 * 사유와 계산 한도다. blockers와 blockersTruncated는 차단 사유와 목록 생략 여부다. recoveryIncludedInMain, renewalProof,
 * fractionMetricScope는 복구 포함 여부·갱신 증명·분수 측정 범위를 나타낸다. from은 계산 결과를 이 응답으로 변환한다.
 */
public record BasicPathResponse(
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
    String fractionMetricScope) {

  public static BasicPathResponse from(
      BasicPathService.Request request, BasicPathService.Result result) {
    var d = result.distribution();
    return new BasicPathResponse(
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
