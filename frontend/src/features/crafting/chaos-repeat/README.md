# Chaos 반복 기록 준비

기준 `3f4495e664d8112011cc5dcd9f17330446167a97`. 이 모듈은 반복 완료 결과를
저장하는 경계만 준비한다. 반복 실행 UI, HTTP 경로·응답 schema, regex,
요청 상한, 취소 정책과 RNG 실행은 구현하지 않는다.

## 저장 계약

기존 `hephaistos.workbench.films.v1`과 `Films.version: 1`을 유지한다.
신규 반복 Frame 하나에만 아래 선택 필드를 추가한다.

```ts
Frame.chaosRepeat?: {
  version: 1
  action: 'CHAOS' | 'GREATER_CHAOS' | 'PERFECT_CHAOS'
  uses: number
}
```

- `uses`는 정상적으로 적용된 횟수인 양의 safe integer다. `1`도 명시적인 반복이다.
  이 저장 경계는 500 등 사용자 요청 상한을 만들지 않는다.
- action은 Frame과 `evidence.action`에 정확히 일치해야 한다. evidence·ruleset·
  snapshot·rule/ledger version·events·assumptions·확인된 consumedOmens가 필요하다.
  manual evidence와 충돌하는 기록, 루트의 반복 메타데이터는 허용하지 않는다.
- 메타데이터가 없는 옛 성공 기록은 기존과 같이 단일 1회다. 인접한 Chaos 기록은 합치지 않는다.
  load는 기존 Frame에 필드를 추가하거나 저장 bytes를 다시 쓰지 않는다.
- 명시된 메타데이터의 null/undefined/누락 필드/잘못된 version·action·count는
  단일 기록으로 추정하지 않는다. load/save는 손상 확장을 거부하고 원본을 쓰지 않는다.
  `filmRulesetStatus`는 inconsistent, `verifiedFrameEvidence`는 undefined로 거부한다.
  기존 catalog 기반 `verifiedHistoryState`와 evidence 검사는 그대로 적용한다.
- 신규 성공 기록 저장 때도 이전 bytes를 기존 `.preserved` 규칙으로 보존한다.

## 다음 API 담당자가 연결할 인터페이스

```ts
type ChaosRepeatReceiptInput = {
  before: ConcreteItem
  action: ChaosRepeatAction
  uses: number
  finalWitness: AppliedItem
  consumedOmens: string[]
}

createChaosRepeatReceipt(
  input: ChaosRepeatReceiptInput,
  verifyRepeatResponse: (input: ChaosRepeatReceiptInput) => boolean,
): ChaosRepeatReceipt | null

recordChaosRepeat(
  films: Films,
  receipt: ChaosRepeatReceipt | null,
  id: string,
  initial: Initial,
): Films
```

1. API adapter가 **반복 전용** 응답을 요청·ruleset·원래 before와 대조하여 검증한다.
   실제 raw 응답 검증기를 closure로 연결해 required `verifyRepeatResponse`를 제공한다.
   callback은 정확한 성공 count, 최종 상태, 마지막 성공 witness와 반복 전체의
   소모 집계를 확인해야 한다. callback이 없거나 false/throw면 receipt가 나오지 않는다.
   테스트의 `() => true`는 합성 adapter fixture일 뿐 production 연결 예제가 아니다.
2. `before`는 batch 시작 상태다. `finalWitness.state`는 batch 최종 상태이며
   `events`/`assumptions`는 **마지막 성공 1회분**만 담는다. 전체 반복 trace를 만들지 않는다.
   `finalWitness.consumedOmens`는 마지막 회차, 별도 `consumedOmens`는 batch 전체의
   확인된 실제 소모 집계다. 각 ID는 소모 1건을 뜻하며 중복은 삭제하지 않는다.
   집계를 활성/남은 징조 또는 마지막 witness만으로 추정해서는 안 된다.
3. receipt는 in-memory runtime brand로 발급하며 입력/읽기 결과를 복제한다.
   타입 강제 변환이나 JSON 복원으로 만든 객체는 writer가 받지 않는다. 이는 검증기
   호출 경계를 강제하는 장치이며 최종 witness만으로 모든 중간 전이를 증명하지 않는다.
4. writer는 before·최종 state·evidence를 catalog로 검증하고 기존 ruleset 경계를 유지한다.
   기존 `recordCraft`의 append/분기 동작을 재사용해 마지막 Frame 하나에 count를 붙인다.
   일반 `/apply` 응답 검증을 완화하거나 batch 전체를 단일 적용 전이로 검증하지 않는다.
5. 적용 실패·uses 0·검증 실패에는 receipt가 없고, null receipt writer는 같은 Films를 돌려준다.
   취소/한도 도달 여부를 이 모듈이 판단하지 않는다. 한도 도달 때도 전용 검증기가
   확인한 마지막 성공 상태와 양의 N이 있으면 같은 writer로 저장한다.
   실제 응답을 받지 못한 취소를 임의 성공으로 만들지 않는다.
6. undo는 batch 전 상태로, redo는 batch 최종 상태로 이동한다. 과거에서 새 기록을
   만들면 기존 Film과 미래를 보존하고 새 Film으로 분기한다. 중간 Frame은 없다.
   UI의 remainingOmens 반영·동시 화폐 차단·늦은 응답 폐기·애니메이션은 별도 연결 책임이다.

## History 표시

`NotationStep.uses`와 `repeated`로 투영한다. 기존 일반 단일 표시는 바꾸지 않고,
반복만 해당 Chaos 아이콘 옆에 locale 숫자로 `N회`를 표시한다. 6개 locale의
count 문구, SR step 이름, 실제 화폐/징조 tooltip data attribute, 단계 이동,
현재 강조와 2열 스크롤을 유지한다. count는 큰 정수에서도 줄바꿈하도록 제한한다.

## 검증 경계

검사는 합성 fixture 및 jsdom/메모리 storage로만 수행한다. 실제 사용자 저장소,
서버·Docker·DB·Redis·볼륨·원격 push·브라우저 QA는 사용하지 않는다.
별도 전체 Vitest는 root 통합 시 수행한다. 실제 API 검증기와 실행 UI는 아직 연결 전이다.

## 2026-11-04 Worker 자체 검증

작업 경로는 `C:\Users\SSAFY\.codex\worktrees\def7\Exile-Hephaistos\frontend`이며,
branch는 `codex/chaos-repeat-history-20261104`다. Node `v24.21.0`, npm `11.19.0`.
기준 이후 package/lockfile 변화가 없어 이전 설치의 node_modules를 재사용했다.

관련 최종 검사:

```text
npm run test -- --run src/features/crafting/workbenchHistory.test.ts src/features/crafting/session-notation src/features/crafting/chaos-repeat src/features/crafting/session-library src/features/crafting/rulesetBoundary.test.tsx src/features/crafting/workbenchStateShape.test.ts src/features/crafting/WorkbenchNotationIntegration.test.tsx src/features/crafting/WorkbenchIntegration.test.tsx src/features/crafting/feedback-tooltip/FeedbackPersistence.test.tsx --maxWorkers=1
```

- PASS: 11 files / 122 tests, 65.56초, exit 0. pretest의 6개 언어 coverage도 통과했다.
- PASS: `npm run lint`, `npm run typecheck`, `npm run format:check`, `npm run build`,
  `git diff --check`. build에는 chunk 500KB 초과 경고가 있다.
- 첫 독립 검사: 4 files / 79 tests 중 76 통과·3 실패였다. 신규 레이아웃 검사의
  min-width 계산값이 `0px`인데 `0`을 기대한 문제로, CSSOM 표기에 맞춰 수정했다.
  기존 동작 기대값·검증·timeout·skip을 완화하지 않았다.
- 이후 관련 검사 11 files / 122 tests 통과 후 자체 리뷰에서 sparse 소모 배열을
  거부하도록 보강했다. 위 최종 결과는 보강 후 전체 관련 검사를 다시 실행한 결과다.
- 자체 리뷰: count/action/version·누락 evidence·충돌 manual·root 메타데이터,
  catalog/ruleset/evidence 거부, 실패/uses 0/unreceived/위조 receipt의 무기록,
  final witness와 aggregate 분리, legacy 원문 bytes 보존·저장 복원,
  batch 단위 undo/redo·원본 미래/Film 분기 보존, 3개 Chaos ID·1회/큰 정수,
  6locale/SR·tooltip·keyboard·2열/wrap 계약을 대조했다.
- 검사 시작 후 실행 소스·테스트·설정·의존성은 변경하지 않았다. 결과를 남기기 위한
  이 문서 추가만 이후 수행했으며 문서 format을 다시 확인한다.
- 로그는 이 worktree의 `frontend/node_modules/.chaos-repeat-verification/`에 보존한다.
  `tests.log`, `tests-final.log`, `lint-final.log`, `typecheck-final.log`,
  `format-final.log`, `build.log`를 포함한다. node_modules/dist와 이전 검증 기록도 보존한다.
- 미실행: 전체 Vitest(root 통합 시 조율), 실제 반복 API/UI 연결 검사,
  브라우저 QA(사용자 보류), Backend·Compose·실행 스크립트 검사(변경 영역 밖).
  jsdom style 확인은 실제 픽셀 overflow/clipping 검증이 아니다.
