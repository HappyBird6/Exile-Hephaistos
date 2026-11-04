# Chaos batch frontend adapter

기준 `9ee4ee2b851ede1c014a31a01a44311caa5c94ab`, branch `codex/chaos-repeat-client-20261104`.
이 폴더는 실제 전용 HTTP 응답 검증과 receipt 발급을 구현한다. 기존 단일
`applyCurrency` 검증, Page, controls, feedback, stash, 표시 formatter는 수정하지 않는다.

## Page 연결

```ts
import { applyChaosRepeat } from './chaos-repeat/api'
import type { ChaosRepeatRequest, ChaosRepeatResult } from './chaos-repeat/api'

const result = await applyChaosRepeat(request, initial, signal)
// Existing Page owns single-flight, requestId/revision/source/Film/cursor/state guards.
if (result.kind === 'completed' || result.kind === 'partial') {
  const next = recordChaosRepeat(history, result.receipt, filmId, initial)
  // Page commits next, result.finalState and result.remainingOmens exactly once.
}
```

요청은 확정 wire 그대로다.

```ts
{
  requestId: string // UUID
  state: {kind: 'CONCRETE', item: ConcreteItem} | {kind: 'ABYSS', abyss: AbyssState}
  action: 'CHAOS' | 'GREATER_CHAOS' | 'PERFECT_CHAOS'
  activeOmens: string[]
  pattern: string
  locale: Locale
  maximumUses?: number // default 100, cap 1000
}
```

- `completed`: 검증된 MATCH/LIMIT. `actualUses > 0`, `finalState`, 확인된 aggregate
  `consumedOmens: string[]`, `remainingOmens`, `receipt`, 종료/오류 정보가 있다.
- `partial`: 검증된 positive INAPPLICABLE 또는 ERROR. 같은 positive 필드를 제공하되
  정상 완료로 바꾸지 않는다. `failure`와 `lastRejection`으로 Page가 오류를 구분한다.
- `no-use`: 검증된 INAPPLICABLE/ERROR의 0회. receipt·최종 상태 적용이 없다.
- `rejected`: 로컬 입력 거부 또는 서버 4xx. `source`, 서버면 `status`를 제공한다.
- `uncertain`: 통신 실패·abort·검증 가능한 부분 결과가 없는 HTTP 오류. 횟수를 추정하지 않는다.
- `invalid-response`: binding/schema/evidence 불일치. fallback 결과나 receipt가 없다.

`lastRejection`은 `{code:'CURRENCY_INAPPLICABLE', reason}` 또는 null,
`failure`는 `{code:'REPEAT_EXECUTION_FAILED', stage:'APPLY'|'MATCHER'}` 또는 null이다.
Page는 code와 소유 locale 문구를 사용하며 임의 응답 문자열을 오류 UI로 그대로 전달하지 않는다.

## 요청·응답 검증

요청과 catalog를 await 전에 복제한다. 요청 body를 `JSON.stringify`로 정확히 한 번
만들고 그 문자열의 UTF-8 bytes에 SHA256을 계산한다. fetch에는 같은 문자열을 보낸다.
기본 N을 생략한 요청은 body에도 생략하며 응답의 유효 N=100을 확인한다.
서버 requestId/raw digest/action/locale/N/matcherVersion과 HTTP/body ruleset을 대조한다.
자동 retry·서버 idempotency·cache는 없다.

matcherVersion은 `chaos-repeat-re2j-1.8-find-ci-v1`이다. frontend에서 사용자 regex를
JavaScript RegExp로 다시 실행하지 않는다. MATCH는 신뢰된 서버의 매칭 결과이며,
표시 동등성은 별도 C golden/서버 reference 검증을 따른다. 생략된 전이 chain,
처음 일치한 시점 또는 최후 한 번의 전이를 frontend가 재현했다고 주장하지 않는다.

전용 validator는 다음을 확인한다.

- 최종 union이 요청 종류와 같고, canonical shape·현재 definition 값·snapshot·catalog가 유효하다.
  알 수 없는 속성을 삭제/보정해 허용하지 않는다.
- 기존 Chaos의 불변 속성(베이스·레벨·희귀도·implicit·조건·관측·품질·socket·fracture 등)이 유지된다.
  ABYSS의 slot 수, 살아 있는 훼손 슬롯의 원래 제약/값, 관측 offer 보존을 검사한다.
  제거된 슬롯의 경로나 생략된 후보 선택은 추정하지 않는다.
- `actualUses`는 0..요청 N의 정수다. LIMIT=N, MATCH>0,
  INAPPLICABLE<N, ERROR stage의 count/HTTP 상태 조합을 확인한다.
- aggregate consumed의 알려진 Chaos omen ID·정수 count, 고유 remaining,
  원래 activeOmens와의 정확한 보존 관계를 검사한다. 마지막 소모는 aggregate의 일부이며
  N=1이면 두 소모가 같다. remaining은 실제 마지막 witness와 같다.
- CONCRETE witness는 state 없는 실제 마지막 성공 응답이다. 로컬 최종 state와
  ruleset을 결합하여 기존 `verifiedFrameEvidence`의 catalog/event/assumption/source 검증을
  재사용한다. 마지막 Chaos ADD/REMOVE와 확률 schema도 확인한다. 원래 시작 state로
  마지막 단일 전이를 검증하거나 기존 `/apply` validator를 완화하지 않는다.
- ABYSS witness는 실제 `{kind,action,applied,reason,consumedOmens,remainingOmens}`다.
  없는 event/assumption/quality evidence를 발명하지 않는다.
- 500 ProblemDetails의 `partialOutcome`도 같은 envelope 검증을 통과해야 한다.
  일반 500·훼손/누락 partial·통신 불확실을 성공 또는 0회로 추정하지 않는다.

## 기록과 저장 호환

전용 검증 완료 후 receipt 입력과 검증된 payload를 대조하는 callback으로만 runtime
receipt를 발급한다. Page가 `() => true`를 전달하지 않는다. 타입 강제 변환·JSON 복원
객체는 receipt가 아니며, record writer는 이를 적용하지 않는다.

새 저장 Frame은 기존 `chaosRepeat:{version:1,action,uses}`와 선택적
`chaosRepeatWitness`(실제 state-free witness)를 갖는다.
CONCRETE는 기존 evidence에 마지막 events/assumptions와 aggregate 소모를 보존하고,
별도 witness에는 마지막 회차 소모를 그대로 남긴다. ABYSS는 최종 `Frame.abyss`와
기존 manual aggregate evidence를 보존한다. 공통 state는 손실 없는 stored projection이다.
분기 root에도 원래 요청의 ABYSS offer/slots를 유지한다. 중간 Frame/state는 추가하지 않는다.

이전 concrete foundation의 count Frame에 witness가 없더라도 읽을 수 있으며,
기존 v1 Frame에 필드를 추가하거나 load 때 다시 직렬화하지 않는다. 새 ABYSS repeat는
실제 ABYSS witness가 필수다. 손상 metadata/witness, ABYSS와 공통 state 불일치는
load/save/ruleset 경계에서 거부하고 저장 bytes를 쓰지 않는다.

undo/redo는 batch 전체를 한 단계로 이동하고, 과거 cursor에서 기록하면 원래 Film과
미래를 보존하여 분기한다. 기존 세 Chaos 아이콘·N회·6locale·tooltip·행 이동 기보를 재사용한다.

## 검증 범위

합성 fixture와 메모리 storage만 사용한다. 초기 관련 검사 6 files / 141 tests는 통과했다.
이는 이번 API 검사이며 이전 foundation 검사와 별도다. 개발 중 첫 typecheck는
unknown 값 narrowing와 미사용 import 2건으로 실패했고 수정 후 통과했다.

전체 Vitest와 실제 Page/서버 연결은 root 최종 통합에서 수행한다. 이 Worker는
브라우저·서버·Docker·실제 저장 기록·DB·Redis·볼륨·원격 push를 조작하지 않는다.

## 최종 자체 검증 · 2026-11-04

실행 경로: `C:\Users\SSAFY\.codex\worktrees\def7\Exile-Hephaistos\frontend`.
기준과 package/lockfile이 같아 기존 설치된 node_modules를 재사용했다.

```text
npm run test -- --run src/features/crafting/chaos-repeat/api src/features/crafting/chaos-repeat/chaosRepeatReceipt.test.ts src/features/crafting/workbenchHistory.test.ts src/features/crafting/session-notation src/features/crafting/session-library src/features/crafting/rulesetBoundary.test.tsx src/features/crafting/workbenchStateShape.test.ts src/features/crafting/WorkbenchNotationIntegration.test.tsx src/features/crafting/WorkbenchIntegration.test.tsx src/features/crafting/Workbench.test.tsx src/features/crafting/feedback-tooltip/FeedbackPersistence.test.tsx src/features/crafting/abyss-workbench/continuousCurrency.test.ts --maxWorkers=1
```

- PASS: 최종 관련 **15 files / 213 tests**, 58.22초, 실패·skip 0. pretest locale coverage 통과.
- PASS: `npm run lint`, `npm run typecheck`, `npm run format:check`, `npm run build`, `git diff --check`.
  build에는 기존 500KB chunk 경고가 있다. 단일 workbenchApi.ts blob은 기준과 동일하다.
- 자체 리뷰: immutable body/digest와 request binding, count/termination 경계,
  actual concrete/Abyss witness, 집계와 남은 징조, 새 metadata 거부,
  ABYSS/common state 일치·hidden/revealed/offer 보존, old v1 무변환,
  한 Frame·undo/redo·원래 Film/미래 분기, 원래 기보·tooltip 흐름을 대조했다.
- 최종 검사 시작 후 실행 소스/테스트/설정/의존성 변경은 없다. 결과를 적는 이 문서만
  이후 추가했고 해당 문서 formatter를 다시 확인한다.
- 새 API 로그: 이 worktree의 `frontend/node_modules/.chaos-repeat-api-verification/`에
  `targeted-first.log`, `related-final.log`, `lint.log`, `typecheck.log`, `format.log`,
  `build.log`를 보존한다. 이전 foundation 로그와 구분한다. node_modules/dist도 보존한다.
- 미실행: 전체 Vitest(root 합본), 실제 Page/HTTP 연결, 실제 backend 계약/빌드 검증,
  브라우저 QA. Backend WIP 실행을 기다리거나 미검증 결과를 완료로 표현하지 않았다.
  이 작업은 확정 wire를 따른 독립 fixture/mock 검사이며 실제 엔드포인트 검증이 아니다.
