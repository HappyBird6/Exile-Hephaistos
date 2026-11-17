# 자동 제작 세션 정책

기준: `11c145ae69cb005774f7fc1f08bb8d3584f5317a` 이후 제작지원 개선.

## 실행 경로와 목적함수

`CraftSupport → CraftStart → AutomaticMethodSearch → automaticApi → /api/v2/crafting/method-searches/automatic → AutomaticSearchService → AutomaticMethodGraph → GoalDirectedPlanner / AutomaticMethodExecution → AutomaticCraftingKernel`을 사용한다. 구형 MethodSearchService/SupportRecommendationService로 우회하지 않는다. 입력은 AutomaticSearchInputReader와 shared/item/ItemStateWireDefaults를 그대로 거친다.

목적은 허용된 제작 정책의 **현재 세션 목표 도달 확률 최대화**다. 비용 가중 점수, 플레이어의 전역 재화 예산, RAG/LLM, 무작위 표본 추정은 추가하지 않는다. Fraction과 outward-rounded CertifiedInterval, 기존 상태 intern/behaviorKey/TerminalAdditionClasses 및 반복 횟수 제약을 유지한다.

## 평가 재사용과 계측

평가 키는 그래프 revision, observation cycles, precision, 그래프/평가기 버전, kernel identity(정책·카탈로그·목표 roll partition·모델), 희귀 종료 정책으로 구성한다. 그래프 인스턴스는 목표 평가 함수를 고정한다. 실행 slice에서 provisional law가 바뀌는 경우도 revision을 증가시킨다. 동일 revision의 조회와 중복 command 응답은 평가기를 재생성하거나 refinement를 반복하지 않는다. 생성/복원/계산 명령에서 평가를 진행하고 응답 생성은 그 상태를 읽는다. 그래프 변경은 전체 평가기를 재생성한다. 증분 고정점 엔진은 만들지 않았다.

평가 메모리는 서버 프로세스/working-set eviction을 넘겨 저장하지 않는다. 재로딩은 저장 그래프와 정책을 검증한 뒤 다시 평가하므로 다른 그래프의 이전 bounds를 재사용하지 않는다.

`AutomaticSearchService.diagnostics(owner,id)`와 `AutomaticMethodGraph.metrics()`는 primitive 생성 시간·호출·결과 수, 그래프 확장 시간, terminal equivalence proof 시간, 전략 생성/후보 평가/refinement 시간, refinement 횟수, 상태·메서드·저장 관측 전이 수, 응답 DTO 생성 시간을 제공한다. 내부 검사/벤치마크용이며 제품 문구에 노출하지 않는다. 그래프 확장 시간은 그 안에서 수행한 primitive/proof 시간을 포함한다. 전이 수는 저장된 observation 행 수이며 고유 전이나 미생성 결과 수가 아니다. 응답 시간에는 HTTP JSON 전송·브라우저 렌더링이 포함되지 않는다.

## REBASE의 의미와 설정

REBASE는 **결과와 그 확률은 알지만 이후 작업은 이번 세션에서 안내하지 않는 종료**다. 확률을 삭제하거나 다른 결과를 재정규화하지 않는다. 현재 세션의 성공 기여는 0이며, 그 결과를 만든 행위의 기대 재화 사용량은 보존한다. 실제 목표 달성과 기존 품질 완료 규칙을 먼저 적용한다. LOSS/DEAD, 미지원·미계산 UNRESOLVED와 구분한다.

완료된 메서드의 조건부 결과 분포에서 CHECKPOINT 및 후속 탐색 가능한 ACTIVE를 기존 behaviorKey + 품질 상태 + 목표 판정으로 묶는다. 목표가 아닌 옵션들을 임의로 하나로 합치지 않는다. 기존의 제한된 Chaos 반복 잔여 실패는 REBASE로 바꾸지 않는다. 아직 계산 중인 분포에는 희귀 판정을 하지 않는다. 이 경우 후속 노드 발행을 보류하고, 이미 발견된 후속 노드라도 희귀 종료 여부가 아직 정해지지 않은 질량은 UNRESOLVED로 평가한다. 완성됐지만 아직 발행하지 않은 관측은 완성 분포에 희귀 정책을 적용한 뒤 평가한다.

요청의 `rareOutcomePolicy`:

```json
{
  "version": "rare-conditional-groups-v1",
  "enabled": true,
  "groupPartsPerMillion": 10000,
  "maximumMethodPartsPerMillion": 50000
}
```

새 UI 세션의 기본값은 그룹 확률 1%, 한 메서드에서 종료할 총확률 5%다. 화면에서 끄거나 변경하고 **새 계산**을 시작할 수 있다. ppm 값은 0..1,000,000 정수이고 판정에는 확률 **상한**을 사용한다. 작은 그룹부터 선택하되 같은 상한을 가진 그룹들은 함께 선택하거나 함께 남긴다. 한도 때문에 hash/iteration 순서에 따라 특정 동률 그룹만 잘리는 일이 없다. 두 한도는 한 메서드의 분포에 대한 기준이다. 여러 단계의 **세션 누적 REBASE 확률은 5%보다 커질 수 있으며 후보별로 별도 계산·표시한다**.

판정은 메서드와 관측 횟수에 대한 조건부 분포에 고정된다. 루트에서의 특정 도착 경로에 따라 공유 노드의 분류를 바꾸지 않는다. 정책은 graph checkpoint/원요청 응답/시나리오에 저장된다. 동일 request ID에 다른 설정을 넣으면 REQUEST_ID_REUSED다. 알 수 없는 정책 버전은 거절한다. 구형 요청과 v17 checkpoint에서 필드가 없으면 disabled로 읽는다. 구형 프런트엔드 flow 복원과 직접 API 호출의 설정 생략도 disabled다. enabled 기본값은 신규 flow가 명시적으로 전달한다. 사라진 서버 작업은 기존 만료 안내를 유지하며 새 기본값으로 자동 재생성하지 않는다. 이미 종료 처리가 적용된 관측은 재발행 때 다시 잘라내지 않는다.

## 후보의 정책과 지표

`View.candidates` 기본 개수는 3개다. 각 후보의 `decisions`는 **도달 가능한 상태 → 선택 메서드**의 대응이다. 해당 메서드의 모든 `points[cycles=100].outcomes`에 따라 다른 상태의 결정을 실행하거나 GOAL/REBASE/LOSS/DEAD/UNRESOLVED로 끝난다. 다음 메서드가 아직 없는 열린 상태는 UNRESOLVED이고 닫힌 상태는 실패다. 이는 성공한 클릭 경로가 아니다. 메서드의 initial/cycle과 기존 100회 관측 제약을 그대로 따른다.

CertifiedOutcomeStrategy의 proper witness가 있으면 이를 사용하고, 나머지는 성공 하한/상한 기준으로 현재 실행 가능한 결정을 선택한다. 시작 결정과 도달 가능한 후속 결정의 대안을 최대 32개 평가한다. 이는 모든 정책 조합의 완전 열거가 아니므로 **전역 상위 3개 보장은 아니다**. 도달 불가능한 결정과 이름만 다른 동일 실행 결정을 제거한다. 후보를 제거하려면 서로 다른 k개 후보의 성공 하한이 그 후보의 비교 가능한 상한을 엄격히 넘어야 한다. 낮은 스케줄링 점수로 메서드/상태를 삭제하지 않는다.

표시 순서는 성공 하한, 성공 상한, 안정된 식별자 순이다. 기대 재화 사용량은 순위에 가중하지 않는다. 구간이 겹치면 OVERLAPPING으로 표시한다. SEPARATED_IN_CURRENT_SET도 현재 평가한 후보 집합에서의 분리일 뿐 전체 정책의 최적성 증명이 아니다.

각 고정 정책의 분기 도착 확률을 전달하며 목표/REBASE/실패/UNRESOLVED를 집계한다. 비용은 도착 확률 × 해당 메서드의 이미 확률 가중된 사용량을 합산한다. 최대 128번 전달 후 남은 순환 꼬리는 미계산 확률로 남기고 성공·종료 상한에도 보수적으로 반영한다. 미지원 결과, 미완성 메서드, 순환 꼬리, 실제 재화 수량이 정해지지 않은 추상 품질 작업이 있으면 `usageStatus=LOWER_BOUND_ONLY`, 사용량 `upper=null`이다. 확정되지 않은 기대값을 유한한 상한처럼 제시하지 않는다.

확률은 통계적 신뢰구간이 아니라 수학적 하한·상한이다. 성공 상한은 미계산 부분의 가능성을 포함하므로 표시된 지표의 상한들을 합해서 1로 해석하면 안 된다. 지원 규칙·허용 정책·현재 희귀 종료 정책 안에서의 세션 평가이며, REBASE 뒤 새 세션까지 포함한 전역 최적 확률/비용을 보장하지 않는다.

## 우선순위와 결과 cursor

상태의 도달 영향 추정, 성공 bounds 폭/상한, 미발견 선택지와 유지 상태 수를 비용 대용값으로 사용해 탐색을 정렬한다. 메서드는 진행 가능한 실행들 사이에서 주기적인 FIFO 차례를 유지한다. primitive 한도에 걸려 정지한 cursor는 보관하지만 진행 중 슬롯에는 포함하지 않는다. 다른 실행을 마친 후 진행 가능한 작업이 없으면 계산 제한 상태로 전환하고 예산 증액으로 재개한다. 이 근사 점수는 순서 선택 전용이다. 안전한 top-k 가지치기와 분리되어 있다.

실제 카탈로그 측정에서 primitive 생성 비중이 커서 활성 커널에 `PrimitiveCursor`를 연결했다. scalar 목표 구간 및 선언된 joint 결과 그룹을 제한된 개수씩 생성하고, 완성된 primitive law만 runner에 넘긴다. 일시 양보는 runner의 pending 항목과 비용을 바꾸지 않는다. 남은 확률은 UNRESOLVED이며 재개시에도 한 번만 재화를 반영한다. 예산 도달을 REBASE나 실패로 바꾸지 않는다.

이 cursor는 첫 요청에서 모든 구체 아이템을 생성하는 것을 피한다. **완료까지 필요한 총 결과 수를 줄이는 기법은 아니다.** eligible pool/유한 joint roll 목록/제한된 tier posterior의 사전 계산은 남아 있다. 생성 중인 primitive cursor는 checkpoint에 저장하지 않으며, 재로딩 시 아직 비용이 반영되지 않은 전이부터 재생성한다. 이미 완료된 runner 관측/비용은 그대로 복원한다.

## 실제 결과와 새 세션

REBASE 결과에서 세션 종료 안내를 표시한다. 사용자가 종료 버튼을 누르면 이전 응답을 무효화하고, 진행 중인 서버 작업이 CANCELLED로 확인되거나 이미 종료된 상태임을 확인한 뒤 기존 시작 아이템 편집기로 이동한다. 네트워크/서버 오류 또는 두 번 연속 revision 충돌은 취소 실패로 표시하고 같은 전환 버튼으로 재시도한다. 실패한 경우 세션 종료 기록과 새 요청을 만들지 않는다. 실제 결과의 옵션/수치를 입력하거나 새 베이스를 선택하고 기존 검증을 거쳐 새 request ID로 시작한다. 결과 그룹의 대표 아이템을 실제 결과로 자동 확정하지 않는다.

사용자 세션 종료 기록은 별도 `session-end:<requestId>`이고 서버 계산 상태와 다르다. 이미 COMPLETE/PARTIAL인 계산은 그 상태를 유지한다. 새 계산의 확률은 확인한 새 시작 상태에 대한 조건부 값이며 이전 세션과 곱하거나 합산하지 않는다. 요청별 컴포넌트 key, abort, revision/generation 검사와 취소 완료 후 mounted/token 검사를 함께 사용한다.

[검증·측정 기록](../verification/automatic-policy-candidates/report.md)

[후속 검토 수정·재검증](../verification/automatic-policy-review/report.md)
