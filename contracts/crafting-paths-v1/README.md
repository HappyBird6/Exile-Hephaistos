# Crafting paths v1 공유 계약

상세 의미는 [명세](../../docs/crafting-path-search-v1.md)를 따른다. 이번 묶음은 계약만 고정하며 제품 endpoint나 runtime capability를 추가하지 않는다.

| 파일 | 용도 |
|---|---|
| schema.json | Draft-07 wire schema. endpoint별 `definitions`를 compile한다. |
| synthetic-fixtures.json | FE mock/BE DTO/의미 테스트 공용 예제, 의도적 실패 예제, 확률 oracle, 취소·재개 timeline |
| solar-source-fixture.json | 실제 bundled Solar source bytes/ID/ruleset에 고정한 별도 수학 oracle 및 runtime 연결 입력 |
| protocol-cases.json | 구현 후 소비자 테스트가 확인할 오류·취소 경합·재개·데이터 추가/삭제 수용 사례. 현재 runtime 실행 결과가 아님 |
| verify.mjs | schema·질량·순위·phase·페이지·복구·revision·source oracle 검사 |
| HANDOFF.md | 파일 소유권·통합 필요 파일·검증 기록과 후속 범위 |

실행:

```text
node contracts/crafting-paths-v1/verify.mjs
```

기존 frontend/node_modules의 Ajv를 사용한다. 준비되지 않은 환경에서는 먼저 기존 잠금 파일에 따라 frontend `npm ci`를 실행한다. 새 의존성·lockfile·CI는 추가하지 않는다. 검증기는 파일을 읽기만 하고 서버·DB·빌드를 시작하지 않는다.

synthetic-fixtures의 `examples[].definition`은 schema definition 이름이다. `expectedInvalid`가 없는 예제만 정상 mock/DTO 응답으로 사용한다. `schema` 실패와 `semantic` 실패 예제는 negative test 전용이며 UI 정상 결과로 표시하지 않는다. `SYNTHETIC_*` action과 synthetic provenance는 production 요청에서 허용되지 않는다. 숫자와 ID가 실제 게임처럼 보이지 않도록 명시적으로 분리했다.

partial 예제는 중단된 계산의 질량 계약을 보여주며 complete binary oracle의 최종 결과가 아니다. unknown 예제는 unknown 질량을 보존한다. unsupported 예제는 recommendations/rankings가 비어 있다. observation-ranking 예제는 합성 시간별 CDF를 비교하며 게임 화폐 간 순위를 주장하지 않는다. 모든 fraction은 기약분수다.

graph-page2는 graph-page1 뒤에 적용한다. 동일 ID는 merge 시 중복 계산하지 않는다. same-state-different-phase는 s0를 두 execution이 공유하는 예제다. 실제 동일 상태 합류는 full state 동등성 검증이 필요하고 source stat을 임의로 버리지 않는다.

Solar fixture의 `goalTemplate.catalogVersion` marker는 HTTP payload가 아니다. 같은 context의 현행 goal catalog 응답 값으로 bind하고 현행 basic-paths provenance를 조회해야 한다. pinned rulesetIdentity가 다르면 fixture를 최신 자료로 자동 재라벨링하지 않는다. `modelOracle`는 source를 독립 열거한 일반 Chaos 1회 성공률 `650/21107`이며 숫자 목표 renewal endpoint의 실행 성공 증거가 아니다. 실제 모델에서 root는 Cold Resistance 6, 목표는 explicit Cold Resistance≥20이고 pseudo total과 다르다. Greater/Perfect·Annul→Exalted는 이 oracle로 검증되었다고 주장하지 않는다.

FE/BE 소비자 테스트는 동일 JSON을 직접 읽는다. 타입/fixture를 각자 복사하여 drift를 만들지 않는다. 계약 수정은 단일 소유자에게 반환하며 두 구현을 함께 갱신한다. 의도적인 source 변경은 source fixture와 증거를 검토하고 새 ruleset/version으로 갱신한다. 기대값을 실패 은폐 목적으로 일괄 재생성하지 않는다.
