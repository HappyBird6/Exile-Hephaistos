# 계약 구현 인계

2026-10-10 후속 보완: BE가 ordinary source snapshot과 loadDefault의 special 확장 snapshot 차이를 확인했다. `solar-source-fixture.json`의 source startItem은 그대로 유지하고 runtimeBindings/requiredRuntimeAcceptance에 별도 current-snapshot 테스트 입력 구성과 원본 SNAPSHOT_MISMATCH 거부 회귀를 명시했다. wire schema·제품 입력 정책·확률 oracle는 바뀌지 않는다. runtime 결과의 통과 여부는 BE 인계에서 별도로 확인한다.

기준: `15f02631df176aa191e95d1f19bb62110be7796e`.
작업 branch: `crafting/path-search-contract-v1`.
범위: 이 디렉터리와 `docs/crafting-path-search-v1.md`만. 제품 코드·dependency·migration·runtime 데이터 변경 없음. 원격 commit SHA는 완료 인계 응답에서 제공한다(자기 commit hash를 파일에 넣지 않음).

## 병렬 파일 소유권

| 담당 | 독립 소유 경로 |
|---|---|
| 계약 담당 | `contracts/crafting-paths-v1/**`, `docs/crafting-path-search-v1.md` |
| FE | 새 `frontend/src/features/crafting/path-tree/**`: types/API/mock, TanStack Query job/pages, 트리/추천/복구, 6언어 전용 문구, 전용 CSS/tests |
| BE | 새 `backend/src/main/java/com/poe2craft/crafting/application/pathsearch/**`, `domain/pathsearch/**`, `presentation/pathsearch/**`, 대응 test 경로 |

공용 파일은 한 담당만 수정한다. 부모가 다음 파일의 소유자를 명시한 뒤 통합한다.

- FE 통합: `goal-filter/CraftStart.tsx`, `ConnectedGoalFilter.tsx`, `editor.ts`. 목표와 start를 하나의 제출 snapshot으로 연결한다. UI 편집은 Zustand, 서버 결과는 Query가 소유한다.
- BE 계산 통합: `BasicCurrencyTransitions.java`, `BasicPathService.java`, `FirstHitCalculator.java`, `ChaosRenewalCalculator.java`, `BasicTransitionCache.java`, `GoalFilterService.java`. 기존 API 보존, 공통 predicate/정확한 continuation/renewal proof 연결이 필요할 수 있다. BE 담당 내부에서 순차 수정한다.
- bootstrap/경계: `bootstrap/SupportConfiguration.java`, controller 등록, `ArchitectureTest.java`, 기존 ruleset request 검사. 통합 담당 단독.
- 공유 위험 파일: `ItemState.java`, `ItemStateValidator.java`, 기존 goal definitions/registry/ruleset manifest, shared i18n, App/CraftSupport, 공용 CSS/lockfile. 첫 slice에 불필요하면 수정하지 않는다. 필요하면 정확한 파일과 이유를 부모에게 인계하고 단일 소유자를 정한다.

FE와 BE는 이 계약 commit에서 각자 전용 branch/worktree를 시작한다. FE mock은 정상 synthetic 예제만 사용하고 unsupported/unknown를 별도로 표시한다. BE는 DTO/schema 테스트와 독립 oracle를 먼저 연결한다. 새 사용자 결정 없이 정책 JSON 입력이나 특수 복구 화폐를 추가하지 않는다.

## 구현 순서와 실제 완료 조건

1. 공통 fixture 소비자 테스트, 고정 6개 후보군/capability, start+goal+provenance 제출 연결.
2. Solar Rare 1explicit의 기존 숫자 evaluator 연결과 후보별 renewal 증명. 현재 ordinary Chaos modifier-ID 최적화를 숫자 조건에 무검증 확대하지 않는다.
3. graph identity/phase·상위 노드 점진 표시·관측점별 추천. 전체 상태를 화면에 한 번에 펼치지 않는다.
4. bounded job/cancel/resume/고정 revision pagination. 첫 1,000 outcomes 이후 continuation과 unknown 질량을 숨기지 않는다.
5. 정확한 이전 checkpoint 선택의 조건부 복구, 본경로 불변.
6. 실제 새 endpoint의 Solar oracle 일치, FE 통합·접근성·6언어, 영향 영역의 정식 검증. 서버 배포나 master 병합은 별도다.

이후 Normal/Magic/복수 explicit과 나머지 기본 화폐를 추가한다. 모든 사례를 완전 계산한다고 약속하지 않는다. 지원 밖 상태/목표는 입력을 보존하고 명시적으로 거부한다. 신규 stat의 data-only 공통 회귀와 삭제 시 무효화는 기능 완료 조건이다.

## 이번 계약 검증

2026-10-10, 기존 설치된 Node/Ajv로 `node contracts/crafting-paths-v1/verify.mjs` 실행:

- 정상 schema/의미 예제 17개, 의도적 실패 예제 6개.
- binary 독립 열거 depth 0..12, exact 0/1/2/100/300/500 CDF와 질량 보존.
- state 공유/phase 분리, 고정 revision pagination과 중복 페이지, 조건부 복구 참조·본경로 미합산.
- cancel/resume 멱등 및 stale revision timeline.
- 실제 Solar source SHA/manifest identity/stat 참조/roll 범위와 독립 1회 oracle `650/21107`.

제품 코드 변경이 없어 Backend/Frontend 전체 빌드·실행 테스트는 이번 묶음에서 하지 않는다. 새 endpoint runtime·실제 cancellation race·continuation 동등성·renewal proof·UI 검증은 후속 구현에서 수행한다. 원본 DB/서버/볼륨은 접근하거나 변경하지 않는다.
