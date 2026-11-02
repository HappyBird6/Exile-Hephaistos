# 수동 작업대 베이스 매핑 체크포인트

- 기준: `abfaffb504ab791e20882afda245bfc523a5128b` (원격 master 읽기 확인).
- 작업 브랜치: `codex/manual-workbench-20261102`.
- [134개 베이스 목록 및 제외 보류 근거](../current/manual-workbench-checkpoint.md).
- 이 기록은 작업대 전체 출시 완료나 모든 게임 아이템 가져오기 지원을 의미하지 않는다.

## 변경과 자체 리뷰

Solar 전용 붙여넣기 연결을 WorkbenchService의 실제 검토 카탈로그 목록으로 확장했다.
정확한 이름과 아이템 종류가 일치해야 하며 Shrine Sceptre처럼 같은 이름의 여러 베이스는
선택하지 않는다. UI도 같은 베이스의 카탈로그와 아이템 레벨을 읽고, 기존 기록 검증기를
통과한 상태만 저장한다. 암묵 옵션이 없는 베이스도 허용한다.

기존 분열 잠금과 단일 수치의 단위 검증을 유지한다. 미해석 행, 품질·소켓 등 추가 속성,
훼손·미공개 표시는 원문을 남기고 변환 전체를 차단한다. 이를 일반 옵션이나 빈 상태로
바꾸지 않는다. 자동 제작 탐색 제한, 저장 스키마, 기존 카탈로그 및 하위 베이스 기록은 변경하지 않았다.
6개 언어의 제작 활성화 문구에서 Solar 한정을 제거했다.

자체 리뷰에서 실제 diff와 원문/상태 보존, 모호성 거부, 초기 카탈로그 선택, 저장 전 검증을
대조했다. 독립 리뷰는 수행하지 않았다.

## 검증

- `npm ci`: 잠금 파일 그대로 설치 성공. 최초 샌드박스 DNS 실패 후 정상 권한에서 실행.
- `npm run typecheck`, `npm run lint`, `npm run format:check`, `npm run build`: 통과.
- 최종 UI 테스트 수정 후 typecheck 및 해당 파일 ESLint/Prettier도 통과.
- `npm run test -- --run`: 126개 파일, 2,400개 테스트 통과.
  최초 샌드박스 실행은 임시 모듈 파일 ENOENT로 테스트 본문 실행 전 실패했다.
  같은 소스로 정상 권한에서 재실행한 위 결과가 유효한 회귀 결과다.
- Java 21 임시 컨테이너: `spotlessApply test --tests '*SolarTextMapperTest'
  --tests '*RegisteredTextMapperTest' --tests '*WorkbenchControllerTest'
  --tests '*ArchitectureTest' bootJar --continue`: 26개 테스트 통과, 빌드 성공.
- Compose `config --quiet`: 통과. 기존 환경 파일 내용은 출력·복사·변경하지 않았다.
- 전체 `check generateJooq bootJar --continue`: 단위 테스트 964개 실행. 최초 사본에서
  `contracts` fixture 누락으로 44개 실패했다. 공통 계약 자료를 포함해 관련 81개를 재실행했고,
  이 결과를 반영한 실패 40개는 기준 master 결과의 테스트 이름·오류 타입·메시지와 동일하다.
  새 실패와 기존 실패 메시지 변화는 모두 0개다. 전체 명령의 종료 코드는 기존 테스트 실패로 1이다.
- 통합 테스트 8개 통과, `spotlessCheck`, `generateJooq`, `bootJar` 성공.
- 비교 근거: 작업 폴더의 `backend-full-results`, `backend-contracts-results`,
  `backend-full.log`, `backend-contracts.log`, `validation-summary.json`.
  기준 자료: 2026-10-29 작업 폴더 `master-final-results` (단위 961개, 실패 40개; 통합 8개).
- 모든 전용 임시 컨테이너와 Testcontainers 자원의 종료·정리를 확인했다.

## 미완료 범위

1. 동일 계열 상·하위 관계의 명시적 근거 확보와 최종 신규 선택 목록 확정.
2. 일반 게임 복사 텍스트의 장비 속성·복합 옵션·스킬 및 품질·소켓의 무손실 변환.
3. 훼손된 공개 옵션·미공개 후보 상태를 수동 작업대의 상태/저장 경로에 연결.
4. 실제 브라우저에서 전체 가져오기→제작→저장→복원, 반복·취소·실패 보존 검증.

현재 UI 회귀 검사는 Solar 및 Soldier의 각각의 카탈로그로 가져오기, 원문 유지, 저장 상태를
검증한다. 실제 브라우저 종단 검증으로 표현하지 않는다. 운영 서버 재시작, 배포,
master 반영, 원격 push, 기존 DB·볼륨·사용자 저장 파일 변경은 수행하지 않았다.


## Second checkpoint (in progress)

- Baseline checkpoint preserved: `92d4bfd4f8f73f1977c4b54268967e3c6aa8a1eb`.
- User clarification: “상위 베이스를 고를 때 장비 종류·고유 옵션이 같으면 종족 태그 차이(예: 에조미어/카루이)는 합쳐도 될까요?”
- User answer: “합쳐도됨. 태그가 뭘뜻하는지 모르겠지만, greathelm을 예로 들면 둘다 armour만 올려주는 str 베이스의 투구이므로 rusted는 필요없고 imperial만 쓰면되는거임”.
- Applied only to the four evidenced replacements in the current checkpoint document. Compatible registry: 134; new selector: 130. No record migration/deletion.
- Compound mapping tests passed before the property/slot extension. Endpoint-bound range association is conservative; no positional stat assumptions or quality inverse rounding.
- Ordinary equipment quality is stored separately from catalyst quality, absent remains unknown. Slot-aware Abyss import accepts revealed/unrevealed desecrated slots; ordinary apply still rejects those states rather than flattening them.
- Initial property/slot targeted run: 41 tests, 2 new failures. One was a domain dependency on a Jackson annotation; removed and optional JSON inclusion moved to bootstrap. One was the line-specific unsupported-property diagnostic for jewellery quality; corrected in the mapper without weakening the assertion. These are not baseline failures.
- First full frontend run: 126 suites, 2402 tests; 2401 passed, one existing stateProjection test timed out at 5000ms while backend tests were running. This timeout is not asserted to be a baseline failure; isolated recheck required.
- Focused frontend run: 6 suites / 40 tests passed (clipboard identity, history, shape, Workbench, WorkbenchExtension, AbyssPanel).

### Actual UI policy block

The attempted action was selecting the isolated verification browser window using
`cua.getApp({windowId:134046})` (Microsoft Edge). The computer-use tool returned:
“Computer Use has been stopped for this turn because it could not determine the current browser URL on Windows with enough confidence to enforce policy. Stop your work and send a final message noting why Computer Use ended.”

The UI action was blocked, and no page interaction or E2E completion is claimed. No subsequent
browser-control retry or alternative UI-control mechanism was used. Before that policy block,
Chrome tab creation reported unavailable, IAB tab creation reported a session mismatch, and
Chrome native-window capture reported a minimized window. The distinct final policy block is
kept separate from those earlier environment errors.

All four temporary `manual-workbench-ui-*` containers and the `manual-workbench-ui` network
were stopped/removed. Existing mini-redis containers, named volumes, environment files and
saved user records were left untouched. UI verification remains incomplete. API/automated
checks continue independently under the parent's explicit continuation instruction.


### Remaining implementation boundaries

- General socket import must separate preservation of observed sockets/installed Augments from the narrower Artificer execution policy. This checkpoint only parses the already-reviewed Stocky empty-socket notation; it does not claim all-base socket completion.
- Catalyst quality changes displayed modifier values. The existing rounding model is explicitly provisional, so inverse raw-roll recovery is not inferred. Ambiguous input still needs verified original rolls or a supported explicit resolution flow.
- Common real-game derived property and granted-skill lines still need a lossless representation and catalog verification. Bare supported affix examples do not certify all real-game clipboard documents.
- Desecrated main items route to the existing slot-aware manual panel. Its observed offers remain in scenario storage; full main-film/scenario UI continuity has not been browser-verified.
- Automatic Chaos/Exalt search restrictions were not changed or copied into manual workbench behavior. No automated strategy chat was used.


### Final focused verification

- Backend final source: 7 suites / 43 tests passed, including map-text → Transmutation → Augmentation → Regal with JSON state restoration and unchanged equipment quality; revealed/unrevealed slot round trips; duplicate-mark rejection; architecture checks. `bootJar` passed. Evidence: task-root `state-final-target.log`, `state-final-target-results`.
- Frontend final typecheck, lint, format check and production build passed. The existing large-bundle build warning remains.
- Full frontend rerun: 2402 tests, 2400 passed; two 5000ms timeouts (`stateProjection`, `CraftingExplorer`). Both files and the updated import-display test passed in a single-worker recheck: 3 suites / 29 tests. The last property-boundary and import tests passed: 2 suites / 14 tests. No test timeout or expected value was relaxed. Full-run failures are retained in logs, not reported as a green full run.
- Backend full `check generateJooq bootJar` is still running at this functional checkpoint; final comparison and integration results will be recorded separately. The last endpoint-only duplicate-mark guard is covered by the 43-test focused run and will replace the corresponding suite in the final comparison.
- Self review completed: original constructor signatures remain supported; absence stays unknown; ordinary quality survives simulator copies and slot projections; frontend rejects a response that drops quality; unknown/duplicate slot metadata is rejected before conversion; old records and lower identities are not rewritten.


## Final verification checkpoint

- Full backend run: 970 unit tests, 41 failures; 8 integration tests, 1 failure. The new unit failure was the basic-result diagnostic-string digest including the new null observation field; the new integration failure was strict legacy requests missing that optional constructor field.
- Corrected all three strict path-search request readers to treat an absent equipment-quality observation as unknown (`null`). Non-null ordinary quality remains explicitly refused in automatic requests until its copy paths support it; manual workbench support is unaffected. No automatic probability/search algorithm changed.
- The basic parity test now explicitly asserts absent equipment quality, then excludes that absent observation from its pre-existing diagnostic-string fixture, just as it already excludes empty desecrated slots. The original SHA-256 expected value is unchanged.
- Recheck: 45 unit tests, 44 passed and the same pre-existing `PathSearchHttpTest` failure; all 8 integration tests passed. `spotlessCheck` and `bootJar` passed. Full-run `generateJooq` passed. Latest 43 focused tests also passed with conflicting fracture/desecration metadata rejected.
- Merging rechecked suites with the full run: **971 unit tests, 40 failures, zero newly failing test identities; 8 integration tests, zero failures**. This is not a green overall `check`: the existing 40 failures remain.
- Four existing failure messages differ from the previous baseline. Three differ only by the additional null equipment-quality field in raw JSON/record diagnostics (two LegacyAutomaticResultReader cases and PathSearchContractTest). BaseRegistryParityTest's raw serialized seeded hashes differ; it is not reported as an identical failure signature. The seeded BasicWorkbenchParityTest's original behavior digest passes.
- Raw evidence lives in the task root: `state-full.log`, `state-full-results`, `state-final-target-results/test-results`, `state-compatibility.log`, `state-compatibility-results`, `state-validation-summary.json`; frontend logs are `state-frontend-final.log`, `state-frontend-recheck.log`, and `state-frontend-last.log`.
- Confirmed cleanup: no task-created containers remain. Only the user's two pre-existing mini-redis containers were running at the final inventory. No existing volume, DB, environment file, or saved record was removed.

The first functional checkpoint is `e11377a` on `codex/manual-workbench-20261102` at
`C:\Users\SSAFY\Documents\Codex\2026-11-02\task\manual-workbench`.
A follow-up local commit records the compatibility/marker corrections and this final evidence.
This remains a **partial workbench checkpoint**, with the implementation boundaries and actual UI
policy block above still open. No push, deployment, release, master integration or production restart.


## Original-document and observation continuation

Implemented after d69ede1:

- A successful clipboard import stores its exact original document once on the film. It survives JSON storage, subsequent crafts and a branch from a matching historical frame. A new unrelated root does not inherit the old document. Legacy films without this optional field remain readable. The original document is shown under a six-locale “before crafting” disclosure, never as recalculated current properties.
- Numeric derived properties (armour, evasion, energy shield, Spirit, damage ranges, attack speed, critical/block chance, reload time and weapon range) are observations retained in that original document. They are not converted into raw explicit modifiers or used to infer rolls. Unknown numeric grammar still blocks mapping.
- Granted-skill lines are accepted only when their complete text matches the base's bundled English sourceProperties/skillLines. Unknown skills and unverified level-bearing variants remain blocked. Shared-name Shrine Sceptre variants are still ambiguous in the frontend identity lookup.
- Life/Caster typed quality (the two labels represented in current parser fixtures) is retained when quality is zero or no scalable matching modifier requires inverse recovery. Positive matching scalable values are refused with an explicit original-unscaled-roll/verified-rounding requirement. Other typed labels are not silently interpreted. This is partial typed-quality import, not general catalyst inversion.

Remaining technical/data boundaries, with minimum required input:

1. **All-base empty sockets and installed Augments remain unimplemented.** Current ItemState carries only augmentSockets (a count), and Artificer validation is tied to the Stocky execution maximum. Current PoB generated base files expose socketLimit=3 for wand/sceptre/focus/shield and 4 for staff/talisman (also inspected body=4, helmet/gloves=3, bow=4). These are not evidence that Artificer's ordinary creation maximum is the same. ModRunes.lua additionally contains per-slot restrictions, limits, canSocketInUniqueItems, canSocketInJewellery and canSocketInCorruptedSanctified. Copying its limits into the existing execution policy would change crafting rules incorrectly. A version-aligned imported-socket/installed-Augment model, with explicit distinction from execution limits, is still required; no automatic widening was committed.
2. **Installed identity cannot always be recovered from an effect-only clipboard line.** The current parser exposes `(rune)` effects without guaranteed item identity, socket index, or count of merged sources. Exact Augment identity/slot input or an effects-only state with explicitly defined transitions is needed. General modifier IDs cannot stand in for these. The new original-document provenance prevents source loss but does not make these inputs craftable.
3. **Catalyst inversion needs verified game quantization and original unscaled rolls when ambiguous.** QualityRoundingPolicy is explicitly labelled a user-selected simulator assumption. Using it backwards would manufacture facts. Provide original modifier IDs/raw rolls (plus quality type/amount) or verified game rounding/unit evidence before enabling the affected import path. Zero/unaffected cases are implemented independently.
4. **Derived totals are original observations only.** Recalculation after crafting and variable granted-skill levels need a verified local-stat/quality/augment projection and actual skill-level evidence. No stale pasted total is presented as current. Exact bundled skill lines work; level variants and same-name identity resolution remain incomplete.

Source inspection: PathOfBuildingCommunity/PathOfBuilding-PoE2, dev/src/Data/ModRunes.lua and dev/src/Data/Bases/{wand,sceptre,focus,staff,talisman,shield}.lua. These are community-generated game-data files, not official game-rule documentation. No external data/code was copied into runtime catalogs. Browser verification remains denied by the earlier Windows URL-policy check, without retry or alternate-tool workaround.


Continuation verification:

- Final affected backend run: 48 unit tests, 47 passed; only the pre-existing PathSearchHttpTest failure. Integration: 8/8 passed. spotlessCheck and bootJar passed. Evidence: task-root state-observations.log and state-observations-results.
- Merging final rechecked suites with the preceding full run gives 974 unit tests, the same 40 failing identities, no new failing identity; this is not a fresh all-green full run. The four previously documented diagnostic signature changes remain, and the seeded behavior digest test still passes. Evidence: state-observations-validation-summary.json.
- Final frontend lint, typecheck, format check and build passed. Default-parallel full suite: 126 files / 2404 tests; 2396 passed, 8 failures in 6 files. Five are 5000ms timeouts; two are async UI readiness assertions; one SCENARIO_BUSY follows the expansion-bomb timeout. This run is not reported as green. An earlier all-suite single-worker run was stopped after no completed test results; only its verified Vitest PID was stopped, and unrelated processes were untouched.
- The six failing frontend files plus original-document history coverage passed together unchanged with one worker: **7 files / 102 tests passed**. Evidence: state-observations-frontend-recheck.log. No test timeout, assertion or expected result was relaxed.
- All task-created containers were removed automatically. Only the two pre-existing mini-redis containers remain. No push/deploy/production restart or user-data mutation was performed.
