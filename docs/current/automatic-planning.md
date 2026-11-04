# 자동 탐색

기준: 애플리케이션 `dbd76a7`, 문서 통합 2026-10-28. 코드 연결을 확인한 명세이며 이번 문서 작업에서 앱 테스트를 재실행한 결과는 아닙니다.


## 입력과 정책

현행 자동 탐색은 request version 2, 검증된 시작 ItemState와 목표, 일치하는 provenance를 요구한다. 요청 activeOmens는 비어 있어야 하며 정책에서 가능한 변형을 생성한다. 검토 카탈로그와 지원 family의 kernel을 사용하므로 Solar 전용이라는 옛 설명은 현행 범위를 나타내지 않는다.

`maximumExploredExplicits=6`이며 5·6옵션 시작 상태와 목표를 처리한다. 시작 상태가 6개를 넘거나 장비/희귀도 규칙을 어기면 INVALID_ITEM, 목표의 최소 필요 옵션이 6개를 넘으면 GOAL_EXCEEDS_SIX_EXPLICIT_SCOPE로 거절한다. 단순 목표 행 수가 아닌 [목표 조건](goals.md)의 의미를 검증한다.

## 확률과 계산 한계

후보 자격을 통과한 pool에 대해 게시 weight/합계의 정확한 분수 또는 명시적으로 허용된 균등 선택을 사용한다. 임의의 독립성을 가정해 여러 roll 분포를 합치지 않는다. 미지원 결합 roll은 JOINT_ROLL_MODEL_NOT_IMPLEMENTED 경계로 남는다. 예산 제한을 이유로 확률질량을 조용히 버리지 않는다.

6옵션 확장은 기존 계산 예산을 무제한으로 늘리지 않았다. 큰 분기·순환·tier 준비 목표는 부분 계산이나 제한에 도달할 수 있다. [6옵션 검증 기록](../development.md#validation)의 `06-tier-preparation` 120000 단계 제한 실패는 기존 기준에서도 확인된 한계이며 과거 정책의 기록이다. 최신 첫 옵션 카오스 정책에서는 같은 06 목표가 허용 경로 없음·확률 0으로 정상 완료되므로 현재의 단계 제한 실패로 해석하지 않는다. 화면 노드 수와 엔진 계산 상태 수는 다르다.

## 저장과 재개

현행 LocalAutomaticJobRepository는 method-job-directory 아래 `automatic-v1` JSON을 사용한다. 구형 certified SQLite 저장과 별개다. owner/clientRequestId의 중복 요청 일치 여부, revision/generation을 검사해 상태 충돌을 방지한다.

지원되는 구형 결과는 LEGACY_READ_ONLY, canResume=false로 읽으며 새 규칙으로 자동 재개하지 않는다. 호환되지 않는 결과는 RECALCULATION_REQUIRED 경계다. 모든 구형 형식을 무조건 호환한다고 주장하지 않는다. 과거 graph/kernel/goal/rarity 버전용 reader가 유지하는 범위는 구현을 기준으로 한다.

취소·재개·재로딩과 4옵션 회귀, 5·6옵션 시작/목표, 잘못된 7옵션·희귀도, legacy 읽기 전용 처리는 SixAffixScenariosTest 및 위 검증 기록에서 확인한다. 이는 무제한 목표의 계산 완료 보장이 아니다.


## 구현 근거

[AutomaticSearchService.java](../../backend/src/main/java/com/poe2craft/crafting/application/pathsearch/AutomaticSearchService.java) · [AutomaticCraftingKernel.java](../../backend/src/main/java/com/poe2craft/crafting/domain/AutomaticCraftingKernel.java) · [LocalAutomaticJobRepository.java](../../backend/src/main/java/com/poe2craft/crafting/infrastructure/LocalAutomaticJobRepository.java) · [SavedAutomaticResultReader.java](../../backend/src/main/java/com/poe2craft/crafting/application/pathsearch/SavedAutomaticResultReader.java) · [SixAffixScenariosTest.java](../../backend/src/test/java/com/poe2craft/bootstrap/SixAffixScenariosTest.java)

[문서 지도](../README.md)

## 첫 옵션 카오스 정책

2026-10-28 확정 규칙과 구현은 [사용자 제작 규칙 R11](user-crafting-rules.md)에 기록한다. 자동 탐색의 카오스는 시작 단계에서 실제 explicit modifier가 정확히 1개인 희귀 아이템의 첫 목표 옵션 획득용 반복에만 허용한다. 시작 0옵션의 기본 준비→1옵션 경로는 유지하며, 후속 상태나 옵션 감소 후에는 시작 단계로 되돌아가지 않는다. 수동 Workbench는 이 자동 탐색 정책과 별개다.

정책 v6·그래프 v13 이전의 v12 저장 결과는 읽기 전용으로 보존하고 새 정책으로 재개하지 않는다. 과거 `07-rarity-preparation`의 매직 1옵션→제왕 2옵션→카오스 경로는 새 정책에서 허용되지 않는다. 06·07의 옛 목표 도달 기대값은 새 정책에 맞춰 정상 완료·금지 경로 없음·정확한 도달 확률 0으로 명시적으로 검사한다. 전체 검사 결과와 한계는 제작 규칙 문서의 검증 기록을 따른다.
