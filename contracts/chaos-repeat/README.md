# Chaos repeat protocol v1

`POST /api/v1/crafting/workbench/chaos-repeat`, JSON UTF-8, 필수 `X-Crafting-Ruleset`.

요청은 `{requestId,state,action,activeOmens,pattern,locale,maximumUses?}`다.
`requestId`는 UUID 문자열이다. state는 `{kind:"CONCRETE",item:ItemState}` 또는
`{kind:"ABYSS",abyss:AbyssCraftingState}`다. 기본 100, 정수 범위 1–1000이다.
지원 locale은 `en`, `ko`, `zh-CN`, `zh-TW`, `ja`, `es`다.

응답은 다음 필드를 가진다. 초기 상태, 직전 상태, 중간 상태 목록은 반환하지 않는다.

```text
schemaVersion: 1
kind: "CHAOS_REPEAT"
rulesetIdentity: string
requestBinding: {requestId,requestSha256,action,locale,maximumUses,matcherVersion}
finalState: 요청과 같은 state union
actualUses: 완료한 applied=true 횟수
consumedOmens: { [omenId]: 정상 소모 횟수 }
remainingOmens: string[]
lastAppliedWitness: ConcreteWitness | AbyssWitness | null
termination: "MATCH" | "LIMIT" | "INAPPLICABLE" | "ERROR"
lastRejection: {code:"CURRENCY_INAPPLICABLE",reason:string} | null
failure: {code:"REPEAT_EXECUTION_FAILED",stage:"APPLY"|"MATCHER"} | null
```

`matcherVersion`은 `chaos-repeat-re2j-1.8-find-ci-v1`이다.
`requestSha256`은 서버가 실제 수신한 UTF-8 body bytes의 SHA-256 소문자 hex다.
클라이언트는 JSON.stringify 결과를 한 번 캡처하여 전송과 hash에 같은 bytes를 쓴다.
JSON 재정렬/재직렬화 digest가 아니다. `request-body.json`과 `.sha256`은 공용 fixture다.
이 fixture는 UTF-8 byte binding 전용이며, `fixture-only` 식별자를 가진 catalog 비유효 입력이다.
실제 제작/전이 검증에는 새 단위테스트의 reviewed catalog 상태를 사용한다.

Witness는 하위 evidence/result wrapper 없이 flat object다.

- ConcreteWitness: `{kind:"CONCRETE",ruleVersion,ledgerVersion,snapshotId,action,applied:true,
  reason,events,assumptions,consumedOmens,remainingOmens,qualityLimit}`.
- AbyssWitness: `{kind:"ABYSS",action,applied:true,reason,consumedOmens,remainingOmens}`.

각 witness는 실제 마지막 성공 결과에서 state만 제외한 값이다. Abyss에는 없는
events/assumptions/version/source를 만들지 않는다. 0회면 witness는 null이다.
마지막 witness의 events/assumptions/소모는 한 번의 사실이고 반복 집계가 아니다.
frontend는 반환된 최종 상태를 해당 witness에 로컬로 결합한다.

정상 결과와 적용 불가는 HTTP 200이다. 내부 오류는 HTTP 500 Problem Details의
`partialOutcome`에 같은 envelope를 넣는다. 최상위 Problem code도
`REPEAT_EXECUTION_FAILED`이며 ruleset response header는 유지한다. exception 원문은 없다.

시작 전 요청 거절은 Problem Details이며 partialOutcome이 없다. body 초과 413,
실제 JSON/UTF-8 오류 400, 실행 admission 실패 429(`CHAOS_REPEAT_BUSY`),
입력/상태/문법/복잡도 거절 422다. pattern 코드는 `PATTERN_LENGTH`, `PATTERN_DEPTH`,
`PATTERN_COMPLEXITY`, `INVALID_REPEAT_PATTERN`이며 일반 입력 코드는
`INVALID_CHAOS_REPEAT_REQUEST`다. 표시 불가 코드는 `UNSUPPORTED_REPEAT_DISPLAY`다.

매회 적용 후 prefix/suffix 옵션 그룹 하나씩 CASE_INSENSITIVE find한다. 복합 다중행은
묶음 하나이며 MULTILINE/DOTALL은 false다. 시작 전 이미 일치해도 첫 적용을 건너뛰지 않는다.
정상 종료는 일치 또는 지정 횟수다. 취소/job/상태 저장/idempotency 보장은 없다.
통신 실패·불완전 응답은 완료 횟수 불명이다. 자동 재시도나 0회 완료로 표시하지 않는다.

정규식은 Unicode literal/range, `.`, `^$`, `|`, `(...)`, `(?:...)`, 일반/reluctant 반복,
기본 `\d\D\s\S\w\W`, `\n\r\t\f\v`, `\A\b\B\z`, hex `\xHH`/`\x{...}`와
메타문자 escape를 지원한다. `\w` 등은 RE2의 ASCII 의미이며 case folding은 Unicode simple
folding이다. inline flag, lookaround, backreference, possessive 반복, named group,
Unicode property/POSIX class/class intersection 등 확장은 거절한다. trim/소문자 변환/정규화는 없다.
길이 512 UTF-16 units, 그룹 깊이 16, syntax node 512, bounded expansion 및 compiled program
4096 제한을 첫 적용 전에 검사한다. counted repetition의 각 수는 RE2 문법상 1000 이하이다.
동시 repeat 한 개만 실행하며 대기열이 없다. 요청 body는 실제 읽은 UTF-8 bytes 65536까지다.
일반/ABYSS 분기는 각각 기존 apply/applyAbyssCurrency를 매회 사용한다. API의 activeOmens는
기존 executor가 인식하는 WorkbenchOmen ID이며, 기존 클라이언트의 dormant bone omen 분리/복원은
해당 클라이언트 경계에서 유지한다. 알 수 없는 ID를 서버가 조용히 삭제하지 않는다.

단일 apply validator는 유지한다. 전용 batch 검증은 final shape/catalog/ruleset,
request binding, count, omen conservation, termination과 witness의 한 번 의미를 확인한다.
중간 RNG 전이를 frontend가 재구성·증명한다고 주장하지 않는다.
