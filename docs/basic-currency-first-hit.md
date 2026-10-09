# 기본 화폐 first-hit 및 조건부 복구

지정된 기본 화폐 정책을 그대로 평가한다. 전역 최적 경로나 추천 순위를 계산하지 않으며 Workbench와 게임 규칙을 변경하지 않는다. [단일 전이 모델](basic-currency-transitions.md)의 Solar/base/Omen/다중-stat 신규 roll 미지원 경계를 그대로 사용한다. 수치는 선언된 simulator 모델 확률이며 검증된 게임 확률이 아니다.

## 입력 및 API

- `GET /api/v1/crafting/basic-paths/provenance`: 현재 full-state provenance.
- `POST /api/v1/crafting/basic-paths/first-hit`: 본목표의 최초 도달 누적확률.
- `POST /api/v1/crafting/basic-paths/recovery`: 명시한 실패 상태를 조건으로 한 복구목표 최초 도달확률.

POST에는 현재 `X-Crafting-Ruleset`이 필수다. 다른 시즌/모델/catalog provenance나 유효하지 않은 concrete state는 실행하지 않는다. 표시용 parser 결과나 legacy bucket을 full state로 변환하지 않는다.

두 POST의 입력 형식은 같다. `start`는 전체 `BasicCurrencyState { item, provenance }`이고 recovery 요청에서는 사용자가 지정한 실패 상태다. `policy.actions`는 16종 기본 전이 action 중 명시적 순서다. `SINGLE_PASS`는 목록을 한 번만 적용하고 종료한다. `REPEAT_CYCLE`은 목록을 계속 반복한다. 적용 불가능한 action에서는 정책이 종료하며 다른 action으로 자동 우회하지 않는다. `activeOmens`는 명시적으로 `[]`여야 지원된다.

`target`은 다음 두 종류 중 정확히 하나다.

- `{ "checkpoint": <전체 BasicCurrencyState>, "explicitModifierIds": [] }`: 같은 provenance와 모든 source roll/rarity/quality/socket 등을 포함한 정확한 이전 상태.
- `{ "checkpoint": null, "explicitModifierIds": ["<현재 catalog explicit modifier ID>"] }`: 모든 지정 ID가 존재하는 재개 조건. 숫자 합계, family, tier, joint roll 목표로 자동 확대하지 않는다.

다음은 입력 조립 예제다. `start`와 modifier ID는 현재 catalog에서 가져온 실제 값으로 채운다.

```javascript
const provenance = await fetch('/api/v1/crafting/basic-paths/provenance').then(r => r.json());
const request = {
  start: { item: concreteSolarItem, provenance },
  policy: { actions: ['CHAOS'], mode: 'REPEAT_CYCLE' },
  target: { checkpoint: null, explicitModifierIds: [desiredModifierId] },
  activeOmens: [],
  observations: [100, 300, 500]
};
const response = await fetch('/api/v1/crafting/basic-paths/first-hit', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json', 'X-Crafting-Ruleset': provenance.rulesetIdentity },
  body: JSON.stringify(request)
}).then(r => r.json());
// Recovery: start를 실패 상태로, target을 이전 checkpoint 또는 명시적 재개 조건으로
// 바꾸고 별도 /recovery를 호출한다. 본경로 확률에는 이 결과를 더하지 않는다.
```

`observations`는 서로 다른 0 이상의 횟수 1~32개이며 응답은 오름차순이다. 100/300/500은 조회 지점이다. **전체 최대 화폐 사용 횟수나 제작 예산은 없다.** `Long` 범위의 조회값을 받으며 계산 자원 한계에 도달하면 이후 조회점도 미계산 경계를 반환한다. JavaScript의 안전한 정수 범위를 넘는 입력은 정확한 십진 문자열로 전달해야 한다.

## 결과와 질량

각 `points`는 문자열 `attempts`, `lower`, `upper`, `active`, `dead`, `unresolved`, `status`, `reachability`를 반환한다. 확률은 `{ "numerator": "3", "denominator": "4" }`처럼 문자열 쌍이다. 소수 반올림은 API 확률 계산에 사용하지 않는다. `reachability`는 `REACHABLE_WITHIN_OBSERVATION` / `UNKNOWN` / `NOT_REACHED_WITHIN_OBSERVATION`이며 영구적 복구 가능/불가능을 주장하지 않는다.

- `lower`: 이미 처음 목표에 도달한 흡수 질량.
- `active`: 해당 횟수까지 미달성했지만 계산된 정책 진행 상태의 질량.
- `dead`: 명시된 정책에서 더 진행하지 못하는 질량. 전역 제작·복구 불가능의 증명이 아니다.
- `unresolved`: 미지원 전이/목표 또는 계산 제한/중단으로 더 계산하지 않은 질량.
- `upper = lower + unresolved`, `lower + active + dead + unresolved = 1`.
- `COMPLETE`: 해당 조회 횟수까지 미계산 질량이 없다. `PARTIAL`: 양의 도달 하한과 미계산 질량. `UNKNOWN`: 도달 하한은 0이며 미계산 질량이 있다.

`purpose`는 `MAIN_FIRST_HIT` 또는 `CONDITIONAL_RECOVERY`다. 요청의 시작 상태·목표·정책, provenance, 모델 해석 및 계산 지표를 함께 반환한다. `recoveryIncludedInMain`은 항상 false다. 단일 복구는 `SINGLE_PASS`의 한 action과 조회 1, 반복/다단계 복구는 명시적 목록·mode와 원하는 조회 횟수로 구별한다. 복구 결과를 주목표를 맞출 확률로 표현하지 않는다. 해당 횟수의 complete 0조차 모든 다른 정책에서 복구 불가능하다는 뜻은 아니다.

`computationLimits`는 현재 서버의 계산 한도다. `blockers`는 전이에서 관측한 미지원/적용 불가/부분 열거의 상태·action·reason 근거 최대 32건이며 `blockersTruncated`가 생략 여부를 명시한다. 전체 그래프나 모든 실패 분기의 목록이 아니다.

흡수 목표를 full-state별로 검사하고 매회 서로 다른 상태 분포를 전파한다. 같은 확률의 독립 시행으로 가정하지 않으며 geometric shortcut도 이번 구현에는 없다. 동일 상태는 provenance를 포함한 완전 동등성으로만 합친다. 이미 성공한 root는 0회부터 확률 1이다.

## 계산 자원과 저장 경계

현재 서버 한도는 transition당 elementary outcomes 1,000개, 누적 상태 평가 10,000회, frontier 1,000개, 보수적인 fraction 연산 복잡도 65,536 bits다. 이 값은 제작 횟수 상한이 아니다. 한도를 넘는 분기는 정확한 질량 그대로 unresolved로 이동하며 분모를 바꾸거나 실패로 재분류하지 않는다. fraction 곱셈 전에 현재 frontier 및 누적 분모 복잡도를 보수적으로 검사한다. 반환 지표 `evaluations`, `peakFrontier`, `peakFractionBits`, `reason`으로 실제 중단 근거를 확인한다. 이 한도가 서비스 지연이나 큰 그래프 메모리 상한을 증명하지는 않는다.

기존 bounded 단일 전이 cache만 사용한다. key에는 full-state/provenance/action이 있으며 caller elementary 한도를 확인한다. 목표·정책·조회점에 의존하는 first-hit 결과는 저장하지 않아 서로 다른 요청 사이에 재사용되지 않는다. partial 전이는 cache에 저장하지 않는다.

이번 API에는 서버 checkpoint 저장과 continuation token이 없다. 중단 요청은 같은 시작 상태·목표·정책·조회점을 다시 계산할 수 있으나, 이미 unresolved로 반환한 질량을 계산된 것으로 복원하지 않는다. 저장 checkpoint를 제공할 후속 묶음에서는 ruleset/model/catalog/full root/목표/정책/action phase/조회 기준/정확한 frontier 질량을 함께 고정하고 부분 layer의 중복 반영을 방지해야 한다.

## 검증과 측정 (2026-10-09)

Java 21.0.12.1 Docker의 `check generateJooq bootJar`는 unit/ArchUnit 584개 및 Docker integration 7개를 통과했다. Frontend npm ci/lint/typecheck/format/test/build는 기존 76 files/1970 tests를 통과했고 Compose quiet 검사도 통과했다. Backend native 임시 복사본과 검증 source의 차이는 작업 디렉터리뿐이며 formatter는 원본 전용 worktree에 적용한 후 복사했다. Windows 클래스 로딩 I/O로 느려진 초기 검사는 최종 검사 결과로 대체했으며 skip/기대값 완화는 하지 않았다.

독립 Java 21 JVM, `-Xmx512m`에서의 단일 probe:

| 사례 | 시간 | 상태 평가 | 최대 frontier | 실제 최대 fraction bits | 결과 |
| --- | ---: | ---: | ---: | ---: | --- |
| synthetic p=1/2, 500회 state propagation | 48.235ms | 500 | 1 | 501 | COMPLETE, geometric oracle와 정확히 일치 |
| Solar level82 Rare, explicit 1개, 반복 CHAOS, 다른 explicit ID 목표, 100/300/500 조회 | 3076.556ms | 996 | 995 | 21 | PARTIAL, FRACTION_COMPLEXITY_LIMIT, 미계산 질량 20982/21107 |

두 계산 이후 측정한 heap 사용량은 129.278MiB, JVM heap 상한은 512MiB였다. 이는 종료 시점 heap이며 실제 high-water mark나 최악 사례 메모리 보증이 아니다. fraction complexity는 개별 분수 bit 길이뿐 아니라 frontier 및 연산 분모의 보수적인 합산 비용을 검사하므로 실제 최대 21 bits에서도 중단할 수 있다. 많은 서로 다른 상태의 object 생성과 분모 결합을 줄이려고 목표와 roll을 임의 bucket으로 합치거나 확률을 반올림하지 않았다. 큰 상태 공간에서는 이 사례처럼 조회값 전체에 넓은 미계산 경계가 남을 수 있으며 500회 exact 확률을 계산했다고 표시하지 않는다.

## 후속 제품 결정

주 추적 기록: [ISSUES.md의 WB-051](../ISSUES.md). 이미 생성한 [GitHub #1](https://github.com/HappyBird6/Exile-Hephaistos/issues/1)은 복구목표 선택 및 중단·재개 checkpoint 계약의 보조 기록으로 유지한다.

자동 복구목표 선정은 제공하지 않는다. 목표 체크포인트의 선택, 정책 자동 추천, 실패 분기별 정책 전환, 영구적 도달불가 증명, checkpoint 중단·재개는 별도 후속 이슈다. UI는 본경로/조건부 복구를 별도 표시하고 UNKNOWN/PARTIAL을 0% 또는 100%로 단정하지 않아야 한다. 이번 묶음은 대규모 Frontend 변경을 포함하지 않는다.
