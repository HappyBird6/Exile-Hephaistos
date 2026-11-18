package com.poe2craft.workbench.application;

import com.poe2craft.shared.crafting.domain.QualityLimitRules;
import com.poe2craft.shared.crafting.domain.WorkbenchSimulator;
import com.poe2craft.shared.item.ItemState;
import java.util.List;

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
