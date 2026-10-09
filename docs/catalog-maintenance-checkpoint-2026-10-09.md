# Catalog 유지보수 첫 안전 묶음

기준 master: `9229407c7d214d592bbe5fafbee36710c9d326f8`. 구현 commit: `36864787e482ed56ee95ccaa8298a9adf22cbc18`. branch: `refactor/catalog-maintenance-20261009`. 경로: `E:/WORK/Exile-Hephaistos/codex/catalog-maintenance-20261009`. 별도 원격 branch까지 전달하며 master 병합·배포는 수행하지 않는다.

## 결과

goal direct stat14/pseudo6/지원 base17을 typed JSON으로 분리했다. 의미 signature가 catalogVersion에 반영되며 현재 동일 정의의 배포 version은 유지한다. domain은 리소스를 읽지 않고 bootstrap에서 immutable 정의를 주입한다.

crafting 반복 whitelist는 명시 set34개로 정리하고 기존 `supportedBases` API로 펼친다. 기존 Blessed의 Elegant/Flexed Crossbow 중복2개 삭제와 추가된 ruleset metadata만 분리한 뒤 나머지 기존 값·배열 순서를 이전 digest oracle와 비교한다. 미구현 기능은 자동 지원하지 않는다. BaseRegistry는 확장117행+legacy17행이고 Workbench134개다.

공통 strict JSON reader와 BaseRegistry·Essence 검증으로 중복 field/ID, unknown field, 강제변환, 잘못된 shape, orphan override, 없는 base/target 참조 및 version mismatch를 조기에 거부한다. 기존 simulator의 modifier·layer·weight 검증은 유지한다.

`ruleset-v1.json`은 형식 schemaVersion과 내부 검토 rulesetVersion을 구분한다. 게임 시즌/패치는 UNVERIFIED이며 resource461개 digest와 engine/ledger version을 검증한다. identity는 goal version/cache 경계에 연결된다. 과거 film 전체의 시즌 실행 호환성은 아직 보증하지 않는다.

## 검증

| 검사 | 상태 | 근거 |
|---|---|---|
| Docker Backend `spotlessApply check generateJooq bootJar` | passed | unit/ArchUnit536 + integration7 =543, failure/error/skip0, 최종9m21s |
| Docker Frontend npm ci/lint/typecheck/format:check/test/build | passed |74 files /1957 tests |
| 기존125 base runtime parity | passed | ID/초기 상태/action/seeded trace oracle |
| goal production snapshot/numeric/확장 | passed | 기존 catalog/evaluate, 새 stat/pseudo data-only 추가, non-Solar numeric 미지원 |
| 순서 보존 | passed | Solar5/Stocky6 extension, snapshot 호환 목록·modifier 순서, Essence target 배열 전체 |
| 데이터/초기화 integrity | passed | duplicate key, 참조/단위, override/policy typo, shape/coercion, ruleset digest/engine/ledger mismatch |
| API smoke | passed | initial/base/snapshot/implicit134, goal catalog17, numeric Solar1 |
| UI smoke | passed | 검색/목표 추가/evaluate NO_MATCH, numeric PARTIAL/BUDGET_EXHAUSTED, page errors0 |
| Compose quiet config / FE mirror / diff check | passed | 기존 .env 출력 없음, sync-base-registry --check |
| Windows Python script 검사 | notrun | Windows 실행 script 수정 없음 |
| 원격 CI | 최종 SHA에서 별도 확인 | 새 CI는 추가하지 않음. 확인 결과는 완료 인계에 명시 |

개발 중 compile 오류와 DEFERRED의 source 미확인 null, 숫자→문자열 coercion 가정, Bow에 없는 Life source에 대한 test 가정을 수정했다. 최종 전체검사는 skip이나 일괄 기대치 갱신 없이 통과했다. 기존 npm high advisory1건과 bundle-size warning은 남는다.

QA는 전용 Docker project `exile-catalog-maintenance-20261009`, localhost21180/21181, tmpfs PostgreSQL을 사용했다. 기존 service·DB·volume·worktree·사용자 변경·.env·개인 film을 보존했다. local 근거는 `E:/WORK/Exile-Hephaistos/codex/catalog-maintenance-evidence-20261009`의 검사 log, backend-counts.json, API/UI result와 screenshot이다.

## 다음 묶음

[변경 가이드](catalog-data-maintenance.md)와 ISSUES.md WB-049에 상세 범위를 남겼다.

1. `WorkbenchCurrency.java`: enum constructor의 baseAction/minimumModifierLevel/fixedModifierId/choice IDs/source, replacement ID/source 배열이 데이터 추출 후보다. 모든 getter null/empty·target 순서·source URL과 availability/seeded 결과를 먼저 oracle로 고정한다. 실행 알고리즘을 JSON 표현식으로 바꾸지 않는다.
2. `WorkbenchOmen.java`: id/trigger/affix/tiered 지원 집합은 정적 데이터다. sideWhittling/sideDoubleRemoval/verifiedDoubleAddition의 검토된 조합을 일반화하지 않는다. 미검토 Omen에 실행을 부여하지 않는다.
3. root/film/request ruleset identity와 mismatch 거부·명시적 legacy 읽기/실행 호환 정책이 필요하다. 현재 frame snapshot과 evidence rule/ledgerVersion만으로 다른 시즌 실행을 완전히 막지 못한다. 과거 film과 explicit snapshot 호환 목록을 보존한다.
4. inventory legacy simulator와 variant wrapper version26개는 역할이 다르다. 이름/alias 계약을 명확히 하며 동일성 강제로 기존 snapshot을 바꾸지 않는다.

새 Craft Support 경로 탐색, 과거 시즌 실행 엔진, 새 게임정보를 근거로 한 확률·반올림·quality·화폐/징조 규칙 변경은 구현하지 않았다. 이 문서는 중요 잔여 개선이 있는 첫 완료 묶음이다.
