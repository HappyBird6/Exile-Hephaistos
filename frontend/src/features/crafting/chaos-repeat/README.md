# Chaos 반복 기록 계약

반복 실행의 최종 결과를 기존 Film에 한 단계로 보존하는 저장 계약이다. 현재 API와 실행 UI는 별도 모듈로 연결되어 있으며 이 문서가 요청 상한·정규식·RNG 실행을 정의하지 않는다.

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

## API adapter와 기록 인터페이스

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

## 연결과 검증

현재 Page는 반복 controls와 전용 API adapter를 사용한다. 이 문서는 Film 저장 경계를 정의하며 HTTP·정규식·실행 UI는 각각의 전용 계약을 따른다. [현재 연결과 보존 규칙](../../../../../docs/development.md#chaos-repeat)을 참고한다.

검사에는 합성 fixture와 메모리 storage를 사용하며 실제 사용자 기록을 변경하지 않는다. 일반 단일 사용, 반복 receipt, ABYSS/CONCRETE, 실패·취소·늦은 응답, 복원과 기보 투영을 구별해 확인한다. 과거 Worker 실행 결과는 통합 전 Git 원문에서 조회한다.
