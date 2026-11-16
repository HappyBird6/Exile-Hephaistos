# 목표 진전과 후보 구성 순서

문서 ID: `support.goal-progress` · 적용: 주 자동 탐색 · 검색: 항상 포함 · 의존: [공통 정책](common-policy.md).

## 적용 조건

기존 목표 평가기의 전체 `goal`과 개별 `entries`를 사용한다. 이번 추출에는 새 필수/선호 점수나 손실 허용 UI를 추가하지 않는다. 후보 계산의 입력은 아이템뿐 아니라 시작 단계 여부, 품질 작업 이력, 현재 정책의 순서 있는 inventory를 포함한다.

전체 목표 또는 어느 행이든 `UNKNOWN/UNSUPPORTED`이면 `GoalDirectedPlanner.choices`는 빈 후보 목록을 반환한다. 이는 게임에서 목표가 불가능하다는 판정이 아니다.

## 일반 목표 진전 판정

`matched`는 현재 `MATCH` 행 수다. 실제 옵션 개수와 다르다.

- `preserves(before, after)`: 행 수가 같고, 이전의 모든 MATCH 행이 이후에도 MATCH여야 한다.
- `advances(source, target)`: 이후 전체 목표가 MATCH이거나, 기존 MATCH 행을 보존하면서 MATCH 행 수가 늘어야 한다.
- 일반 직접 후보는 kernel의 `plan`이 READY이고, 대표 결과 중 하나 이상이 `advances`를 만족하면 추가한다. **결과 하나가 유효하다는 뜻이지 모든 분기가 성공한다는 뜻은 아니다.**
- 전체 목표 MATCH가 우선이다. OR 등으로 일부 개별 행 판정이 바뀌어도 전체 목표를 달성한 경우를 임의로 실패로 바꾸지 않는다.
- 품질 상한 확보와 훼손 작업은 아래 별도 규칙이 있다. 일반 진전 조건을 모든 후보에 일괄 적용하지 않는다.

## 후보 구성 순서

1. 첫 변경 가능 목표 획득 여부와 카오스 허용 여부를 계산한다. 목표 판정 미지원 여부를 검사한다.
2. [품질 문서](quality-checkpoints.md)의 우선 품질 작업 조건에 해당하면 해당 작업만 반환한다. 다른 후보를 먼저 섞지 않는다.
3. 필요한 기본 품질 작업을 넣고 inventory 순서로 직접 후보와 준비 후보를 모은다. 시작 엑잘의 추가 제한, 품질 예외, 현재 정책/kernel 자격을 적용한다.
4. 준비 행동과 후속 획득 행동을 검토한다. 준비한 뒤 실제 후속 작업이 가능한 결과가 있고 그 작업이 목표를 진전시켜야 연결한다. 모든 두 화폐 조합을 무조건 만들지 않는다.
5. 준비와 같은 화폐의 직접 목표 획득 후보가 이미 있으면 일부 중복 준비 경로를 생략한다. `TARGET_PREPARATION`, 또는 변경 가능 옵션 0개에서 카오스를 준비하는 경우는 이 생략에서 제외한다.
6. 여기까지 결과가 비었을 때만 단독 `RARITY_UPGRADE`를 남긴다. 이후 훼손 후보가 추가될 수 있어도 이 판정 순서를 바꾸지 않는다.
7. kernel이 허용한 접두/접미 훼손 작업을 추가한다.
8. [추가 후보 우선순위](addition-ranking.md)를 엑잘, 제왕 순으로 적용한다.
9. 이미 훼손 슬롯이 있는 상태의 준비 없는 단일 행동에는 kernel의 조건부 비교 정보를 붙일 수 있다. [훼손 문서](desecration.md)를 따른다.

준비/후속 연결 시 후속 행동은 목표 획득 purpose이거나 실제 교체 옵션을 가진 화폐여야 한다. `QUALITY_CHECKPOINT`를 일반 준비의 후속 획득으로 연결하지 않는다. 현행 보류된 절사 준비의 분기는 활성 전략으로 추출하지 않는다.

`LinkedHashSet`의 삽입 순서와 동일 후보 제거를 보존한다. [등급 상승](rarity-upgrade.md)·[카오스](first-affix-chaos.md)·[더미](dummy-replacement.md)에서 세부 예외를 적용한다.

## 결과 분류와 종료

graph의 기본 분류 순서는 전체 목표 MATCH → 행 수 불일치/미지원 → 이전 MATCH 상실 → 필요한 희귀도 변화 → 훼손 슬롯 변화 → MATCH 수 증가 → 계속이다. 각각 `GOAL`, `UNRESOLVED`, `LOSS`, `CHECKPOINT`, 계속에 해당한다.

실행기는 준비 중 성공, 품질 작업, 카오스 반복에 추가 종료 규칙을 적용한다. 따라서 기본 분류만 복제해 성공 시 무조건 종료하지 않는다. 품질 준비 때문에 이미 맞춘 목표를 잃을 수 있는 예외는 [품질 문서](quality-checkpoints.md)에 기록한다.

graph는 루트와 후속 노드에서 모두 후보를 생성한다. 동일 아이템이 다시 나왔더라도 후속 노드는 시작 단계가 아니다. 문서 검색이나 캐시의 문맥에서 이 차이를 보존한다.

## 비교 사례

- 긍정: A가 MATCH인 상태에서 A를 보존하고 B를 얻는 결과가 있는 직접 행동은 후보가 될 수 있다. 실제 A 손실 분기는 계산에서 제거하지 않는다.
- 부정: MATCH 행 수만 같다는 이유로 A를 잃고 C를 얻은 상태를 보존 성공으로 취급한다.
- 부정: 검색 점수가 높은 문서부터 행동을 정렬하거나, 품질 우선 반환 뒤에 일반 화폐를 추가한다.
- 부정: 일부 목표 진전이 없는 훼손 준비를 일반 진전 조건으로 삭제한다.

## 근거

- [GoalDirectedPlanner](../../../backend/src/main/java/com/poe2craft/craftingsupport/search/domain/GoalDirectedPlanner.java): `matched`, `preserves`, `advances`, `choices`.
- [AutomaticMethodGraph](../../../backend/src/main/java/com/poe2craft/craftingsupport/search/domain/AutomaticMethodGraph.java): `intern`, `discover`, `classify`, `Method`.
- [AutomaticMethodExecution](../../../backend/src/main/java/com/poe2craft/craftingsupport/search/domain/AutomaticMethodExecution.java): `classifyState`.
- [GoalDirectedPlannerScenariosTest](../../../backend/src/test/java/com/poe2craft/bootstrap/GoalDirectedPlannerScenariosTest.java): 현재/후속 상태, 목표 획득, 부분 경로, 확률질량 검사. 이 클래스에 과거 정책 검사도 섞여 있음은 [검토 기록](../review-notes.md)을 참고한다.
