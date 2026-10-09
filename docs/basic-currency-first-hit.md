# 기본 화폐 first-hit 및 조건부 복구

지정된 기본 화폐 정책을 그대로 평가한다. 전역 최적 경로나 추천 순위를 계산하지 않으며 Workbench와 게임 규칙을 변경하지 않는다. [단일 전이 모델](basic-currency-transitions.md)의 Solar/base/Omen/다중-stat 신규 roll 미지원 경계를 그대로 사용한다. 수치는 선언된 simulator 모델 확률이며 검증된 게임 확률이 아니다.

## 입력 및 API

- `GET /api/v1/crafting/basic-paths/provenance`: 현재 full-state provenance.
- `POST /api/v1/crafting/basic-paths/first-hit`: 본목표의 최초 도달 누적확률.
- `POST /api/v1/crafting/basic-paths/recovery`: 명시한 실패 상태를 조건으로 한 복구목표 최초 도달확률.

POST에는 현재 `X-Crafting-Ruleset`이 필수다. 다른 시즌/모델/catalog provenance나 유효하지 않은 concrete state는 실행하지 않는다. 표시용 parser 결과나 legacy bucket을 full state로 변환하지 않는다.

알 수 없는 필드, numeric enum ordinal, float→integer 및 일반 scalar 강제변환을 거부한다. observation만 `0` 또는 선행 0 없는 양의 십진 정수 문자열을 별도로 Long 범위 검사 후 허용한다. `100.9`, `"100.9"`, boolean, 범위 초과값은 거부하며 item numeric 필드의 문자열/float도 보정하지 않는다. 수정 전 HTTP에서 `[100.9]`가 200/100으로 수용되는 회귀를 재현했고 이후 422로 거부한다.

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

일반 경로는 흡수 목표를 full-state별로 검사하고 매회 서로 다른 상태 분포를 전파한다. 모든 시행을 같은 p로 가정하지 않는다. 동일 상태는 provenance를 포함한 완전 동등성으로만 합친다. 이미 성공한 root는 0회부터 확률 1이다. SINGLE_PASS 종료는 추가 전이 평가 없이 분류하며 마지막 실제 action에서 평가 budget을 모두 사용해도 완료된 나머지 질량을 UNKNOWN으로 바꾸지 않는다.

### 검증된 단일 explicit Chaos renewal

`REPEAT_CYCLE`의 action 목록이 `[CHAOS]` 하나이고, Rare의 explicit이 nonfractured 1개이며, 징조가 없고 목표가 explicit ID 하나일 때만 좁은 최적화를 시도한다. 기존 단일 전이의 전체 후보 preflight로 base/특수 상태/weight/모든 신규 roll 지원을 먼저 검사한다. 양의 weight를 가진 joint/oversize roll 후보 하나라도 미지원이면 증명은 실패하고 일반 경로의 UNSUPPORTED 질량을 유지한다.

공유 Workbench plan의 유일 삭제 분기가 모든 비explicit 필드(implicit/source roll/rarity/item level/snapshot/base/conditions/socket/quality)를 보존하는 동일한 full empty-explicit 상태인지 검사한다. 현재 finish는 copy와 `PRESERVE_EXISTING` identity quality 정책이므로 roll 값에 따른 다른 비explicit 효과가 없으며, 각 후보 양 끝의 실제 finished 상태와 후속 plan에서도 같은 empty 상태 및 전체 eligible 분포를 다시 확인한다. 신규 explicit은 항상 하나의 nonfractured instance이므로 실패 후에도 같은 삭제 후 커널로 돌아온다. 이 고정 모델의 구조적 동치 증명이며 일반적인 독립 시행 가정이나 숫자 roll 지원의 우회가 아니다.

증명한 경우 `p = target ID eligible weight / 전체 eligible weight`, `CDF(n) = 1 - (1-p)^n`을 정확한 BigInteger 분수로 계산한다. 모든 후보/roll의 질량은 그대로 포함하며 target이 eligible이 아니면 p=0이다. 응답 `renewalProof`는 full empty state/provenance, target ID, 정확한 p, eligible ID 목록 및 proofVersion을 제공한다. 조건을 만족하지 않거나 증명이 깨지는 분열/복수 explicit/다른 action/징조/변경 필드/미지원 roll은 일반 상태 전파로 fallback한다.

분수 출력·중간 연산의 보수적인 bit 예산을 거듭제곱 전에 검사한다. huge Long 조회점은 안전한 계산 prefix의 도달 하한과 남은 생존 질량을 unresolved로 반환하며 upper=1을 유지한다. p=0/1과 이미 성공한 root는 거대한 power를 할당하지 않는다. 이 출력 예산은 제작 횟수 제한이 아니다. renewal의 `evaluations=0`은 그래프 전파 평가가 없음을 뜻하며 domain 증명의 후보/plan 검사가 없다는 뜻이 아니다.

## 계산 자원과 저장 경계

현재 서버 한도는 transition당 elementary outcomes 1,000개, 누적 상태 평가 10,000회, frontier 1,000개, 보수적인 fraction 연산 복잡도 65,536 bits다. 이 값은 제작 횟수 상한이 아니다. 한도를 넘는 분기는 정확한 질량 그대로 unresolved로 이동하며 분모를 바꾸거나 실패로 재분류하지 않는다. fraction 곱셈 전에 현재 frontier 및 누적 분모 복잡도를 보수적으로 검사한다. 반환 지표 `evaluations`, `peakFrontier`, `peakFractionBits`, `reason`으로 중단 근거를 확인한다. `peakFractionBits`는 관측한 저장 확률·누적값의 일부 측정치이며 커널 내부·모든 임시 arithmetic 할당을 포함한 완전한 high-water mark가 아니다. 응답의 `fractionMetricScope`도 이를 명시한다. 이 한도가 서비스 지연이나 큰 그래프 메모리 상한을 증명하지는 않는다.

기존 bounded 단일 전이 cache만 사용한다. key에는 full-state/provenance/action이 있으며 caller elementary 한도를 확인한다. 목표·정책·조회점에 의존하는 first-hit 결과는 저장하지 않아 서로 다른 요청 사이에 재사용되지 않는다. partial 전이는 cache에 저장하지 않는다.

이번 API에는 서버 checkpoint 저장과 continuation token이 없다. 중단 요청은 같은 시작 상태·목표·정책·조회점을 다시 계산할 수 있으나, 이미 unresolved로 반환한 질량을 계산된 것으로 복원하지 않는다. 저장 checkpoint를 제공할 후속 묶음에서는 ruleset/model/catalog/full root/목표/정책/action phase/조회 기준/정확한 frontier 질량을 함께 고정하고 부분 layer의 중복 반영을 방지해야 한다.

## 검증과 측정 (2026-10-09)

Java 21.0.12.1 Docker의 `check generateJooq bootJar`는 unit/ArchUnit 584개 및 Docker integration 7개를 통과했다. Frontend npm ci/lint/typecheck/format/test/build는 기존 76 files/1970 tests를 통과했고 Compose quiet 검사도 통과했다. Backend native 임시 복사본과 검증 source의 차이는 작업 디렉터리뿐이며 formatter는 원본 전용 worktree에 적용한 후 복사했다. Windows 클래스 로딩 I/O로 느려진 초기 검사는 최종 검사 결과로 대체했으며 skip/기대값 완화는 하지 않았다.

독립 Java 21 JVM, `-Xmx512m`에서의 단일 probe:

| 사례 | 시간 | 상태 평가 | 최대 frontier | 관측한 저장 fraction bits | 결과 |
| --- | ---: | ---: | ---: | ---: | --- |
| synthetic p=1/2, 500회 state propagation | 48.235ms | 500 | 1 | 501 | COMPLETE, geometric oracle와 정확히 일치 |
| Solar level82 Rare, explicit 1개, 반복 CHAOS, 다른 explicit ID 목표, 100/300/500 조회 | 3076.556ms | 996 | 995 | 21 | PARTIAL, FRACTION_COMPLEXITY_LIMIT, 미계산 질량 20982/21107 |

두 계산 이후 측정한 heap 사용량은 129.278MiB, JVM heap 상한은 512MiB였다. 이는 종료 시점 heap이며 실제 high-water mark나 최악 사례 메모리 보증이 아니다. fraction complexity는 개별 분수 bit 길이뿐 아니라 frontier 및 연산 분모의 보수적인 합산 비용을 검사하므로 관측값 21 bits에서도 중단할 수 있다. 위 표는 renewal 이전 일반 전파의 기록이다. 일반 경로에서는 이처럼 조회값 전체에 넓은 미계산 경계가 남을 수 있으며 500회 exact 확률을 계산했다고 표시하지 않는다.

### 독립 리뷰 후 renewal 및 경계 보완

Java 21 Docker `check generateJooq bootJar`: **592 unit/ArchUnit + 7 integration PASS**, failures/errors/skips=0. 기존 Frontend 소스/lockfile은 변경하지 않아 앞선 1970개 전체 회귀 근거를 재사용한다. HTTP scalar/ordinal/정수 문자열, 실제 Solar renewal HTTP DTO와 quality20 보존, 최종 action=마지막 evaluation 및 이후 관측, 다중 action cycle phase, renewal와 작은 catalog 일반 열거의 완전 일치, joint 후보/분열/복수 explicit/다른 action fallback, huge Long 및 p=0/1/이미 성공을 추가 검증했다.

같은 Java 21 및 `-Xmx512m`의 새 JVM에서 실제 Solar level82 단일 explicit Rare의 plain Chaos를 측정했다. 209개 eligible 후보 전체를 포함한 `p=125/21107`이며 exact CDF는 `1-(20982/21107)^n`이다. 도메인 증명과 100/300/500 세 조회를 포함한 시간은 **113.150ms**, unresolved는 모두 0이고 관측한 저장 fraction 크기는 최대 7183 bits였다. 그래프 전파 평가 0, frontier 표현은 생존 질량 한 개다. 두 계산 및 보조 사례 이후 heap은 **110.770MiB**였으며 종료값이다. 한 번의 probe이고 초기화/GC/호스트 부하가 달라 최악 시간이나 모든 상태의 보장을 뜻하지 않는다.

| 조회 | exact 계산 상태 | 미계산 질량 | 표시용 근삿값 |
| --- | --- | --- | ---: |
| 100 | COMPLETE | 0 | 약 44.78752% |
| 300 | COMPLETE | 0 | 약 83.16893% |
| 500 | COMPLETE | 0 | 약 94.86919% |

표의 소수는 읽기 위한 표시값이며 API와 계산은 정확한 분수를 유지한다. Catalyst FLESH quality20 사례도 full quality를 보존하며 COMPLETE였다. 단일 fractured explicit은 renewal 증명이 없고 일반 경로에서 CHAOS UNAVAILABLE로 종료했다. 현재 Solar catalog의 양의 weight joint 정의는 0개이며, joint 미지원 경계는 synthetic 양의 후보 추가 테스트에서 전체 unresolved=1로 확인했다. 이 결과를 다른 게임 상태·다중 옵션·numeric 목표·다른 화폐에 일반화하지 않는다.

## 후속 제품 결정

주 추적 기록: [ISSUES.md의 WB-051](../ISSUES.md). 이미 생성한 [GitHub #1](https://github.com/HappyBird6/Exile-Hephaistos/issues/1)은 복구목표 선택 및 중단·재개 checkpoint 계약의 보조 기록으로 유지한다.

자동 복구목표 선정은 제공하지 않는다. 목표 체크포인트의 선택, 정책 자동 추천, 실패 분기별 정책 전환, 영구적 도달불가 증명, checkpoint 중단·재개는 별도 후속 이슈다. UI는 본경로/조건부 복구를 별도 표시하고 UNKNOWN/PARTIAL을 0% 또는 100%로 단정하지 않아야 한다. 이번 묶음은 대규모 Frontend 변경을 포함하지 않는다.
