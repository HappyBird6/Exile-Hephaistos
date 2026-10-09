# 기본 화폐 first-hit 실험 UI

기준 Backend SHA: `8e56fee71b73f15935466c7fc124bf097e07aa11`.
작업 branch: `crafting/first-hit-ui-20261009`. master는 변경하지 않는다.
API 계약과 확률 공식은 [first-hit 명세](basic-currency-first-hit.md), 지원 경계는 [단일 전이 명세](basic-currency-transitions.md)를 그대로 사용한다.

## 배치와 입력

기존 CraftSupport의 CraftStart 아래, 기존 Advanced family / tier comparison 앞에 접을 수 있는 실험 계산 섹션을 추가한다. 거래소형 goal filter, Workbench 동작과 시작 아이템 편집 구조는 유지한다. 계산 섹션은 CraftStart의 검증된 concrete item, catalog와 ruleset identity를 전달받는다. legacy Bucket 또는 표시용 parser 결과를 concrete state로 변환하지 않는다. 원문을 편집하면 기존 검증이 해제되므로 재검증 전 계산할 수 없다. 초기 실제 아이템, roll, rarity와 목표를 자동 변경하지 않는다.

목표는 현재 catalog의 이름·tier·원문 효과를 표시하는 select에서 exact explicit ID 하나를 선택한다. tier-or-better, family, 숫자 합계 목표로 확대하지 않는다. API ID 문자열 직접 입력은 요구하지 않는다. Apprentice’s (`amulet:prefix:apprentice-s`)는 주문피해이며 +3 minion 목표가 아니다. 기본 목표는 미선택이다.

16종 action을 순서대로 추가·삭제하고 SINGLE_PASS 또는 REPEAT_CYCLE을 선택한다. 목록 길이 32는 API 정책 입력 크기 한도이며 제작 사용 횟수 상한이 아니다. 100/300/500 조회점은 고정되어 있으며 최대 제작 횟수·예산은 없다. 기본 action은 CHAOS, 본경로 mode는 반복, 복구 mode는 단회이고 사용자가 변경할 수 있다. 적용 불가 action에서 정책이 종료하며 자동으로 다른 action을 선택하지 않는다.

복구 입력은 사용자가 현재 concrete item을 실패 상태로 저장하고 별도의 현재 아이템을 정확한 checkpoint로 저장하는 버튼으로 지정한다. 기존 시작 아이템 편집기를 이용하여 원하는 실패 상태·checkpoint를 준비하고 각각 저장할 수 있다. 복구 목표는 저장한 full-state checkpoint 또는 사용자가 선택한 exact modifier 조건이다. checkpoint는 source rolls, rarity, quality, sockets, conditions와 provenance를 포함한다. 서버 저장·자동 발견·추천·실패 분기 자동 선정은 없다. 이 section의 Zustand 메모리에만 보존하며 컴포넌트 제거 시 소멸한다. 패널을 접었다 펼쳐도 입력은 유지하지만 계산 실행과 결과는 폐기한다.

Solar 외 base와 활성 Omen, 검증 전 원문, ruleset 불일치는 계산을 막고 미지원 사유를 표시한다. multi-stat 신규 roll 및 미지원 roll 모델은 API 경계·blocker와 unresolved 질량으로 드러난다. 서버 응답은 TanStack Query로 관리한다. Backend 계약 보완, 신규 게임 규칙, 전체 추천 탐색, 서버 checkpoint 저장은 추가하지 않았다.

## 표시 및 격리 계약

- 본경로와 조건부 복구는 별도 query와 별도 결과 영역으로 표시한다. 둘의 확률을 더하지 않는다.
- COMPLETE는 정확한 분수와 절삭 근사 비율을 표시한다. PARTIAL/UNKNOWN은 하한·상한·unresolved를 구분한다. active와 policy-ended 질량도 표시하며 global 제작 불가로 해석하지 않는다.
- 분자·분모 문자열을 각각 Number로 바꾸지 않는다. BigInt 나눗셈으로 percentage의 소수 다섯 자리를 절삭한다. 양수 극소 확률은 `<0.00001%`, 1 미만은 최대 `≈99.99999%`, 정확히 1만 `100%`다. 정확한 분수는 disclosure로 확인할 수 있다.
- 응답의 ruleset header, 전체 provenance, purpose, recoveryIncludedInMain=false, 요청의 full state/target/policy/observations 및 정확한 질량 보존과 status 일관성을 검증한다. ItemState의 canonical modifier 순서·선택 필드의 null/false 기본값만 동등하게 비교하며 원문 roll을 보정하지 않는다.
- 입력·시작 상태·원문·history revision·ruleset/refetch 변경은 결과와 실행 선택을 폐기한다. 늦은 응답은 취소된 query에 격리한다. 같은 provenance로 돌아와도 이전 실행을 자동 재개하지 않는다.
- 자동 retry·focus/reconnect refetch·재마운트 결과 refetch는 하지 않는다. 에러 후 계산 버튼으로 명시적 재시도할 수 있고 중복 클릭은 같은 요청을 중복 실행하지 않는다. 패널 닫기·탭 비활성화는 요청과 결과를 폐기한다.
- native details/select/button, fieldset/legend, label, alert/status 및 닫기 후 summary focus 복귀를 사용한다. 700px 이하 한 열, 긴 분수·JSON wrapping과 스크롤을 적용한다. 실제 좁은 화면·키보드 브라우저 검증은 아래 미완료 경계에 해당한다.
- 기존 6개 locale dictionary와 useI18n을 사용한다. AGENTS.md의 제품 UI 영어 정책에 따라 신규 문구는 여섯 locale 모두 영어다. 기존 catalog ID와 기술명은 그대로 유지한다.

## 검증 근거와 남은 작업

Frontend npm ci와 Docker lint/typecheck/format/build PASS. 첫 전체 실행은 1977 PASS/2 FAIL로 기존 CraftSupport catalog 대기 assertion 두 개가 실패했다. 동일 assertion·timeout으로 `--maxWorkers=2`의 해당 CraftSupport 6개 재검사는 모두 통과했다. 최종 전체 `npm run test -- --run --maxWorkers=2`는 **78 files / 1984 tests PASS**였다. assertion·timeout·skip은 변경하지 않았다. 전체 검사 이후 자체 리뷰의 action 삭제 focus 복귀 보완을 포함한 추가 검사도 **lint/typecheck/format + 2 files / 15 tests PASS**다. 마지막 production build는 이 focus 보완을 포함한 소스로 통과했다.

자체 리뷰는 입력/provenance 변경 후 이전 실행이 재활성화되는 경계, main/recovery 분리, 오류 뒤 재시도/중복 클릭, request echo와 질량 보존, fractional truncation, markup과 focus 복귀를 확인하고 보완했다. 문서와 HTTP 결과는 구현 소스와 함께 commit한다. HTTP QA 컨테이너 3개와 신규 임시 DB volume·전용 네트워크는 제거했고 기존 서비스·DB volume은 사용하지 않았다.

영향받은 Backend는 Java 21 Docker에서 `test --tests "*BasicPath*" --tests "*BasicCurrency*" --tests "*ChaosRenewal*" --tests "*FirstHit*" bootJar` PASS. Backend 소스는 기준 SHA와 동일하며 신규 전체 check/generateJooq/integration 실행을 주장하지 않는다. 기준 Backend의 592+7 근거는 기존 명세에 있다.

[HTTP probe 결과](evidence/first-hit-ui-20261009/http-probe.json)는 전용 QA PostgreSQL·Redis·Java Backend와 production Frontend adapter를 호출했다. manufactured Solar catalog 상태만 사용하며 개인 원문을 저장하지 않았다. 100/300/500 single explicit Rare/plain Chaos와 Apprentice’s 목표는 COMPLETE/unresolved0이고 비율은 절삭하여 44.78751% / 83.16892% / 94.86918%다. 마지막 분자·분모는 각각 2163자리다. 두 explicit Rare는 PARTIAL, Omen은 UNKNOWN/UNSUPPORTED blocker, 별도 실패 아이템에서 full checkpoint로 SINGLE_PASS 복구는 PARTIAL로 구분했다. 복구가 본경로에 합산되지 않음을 검증했다.

[재현 probe](evidence/first-hit-ui-20261009/http-probe.mjs)는 npm ci 이후 `node docs/evidence/first-hit-ui-20261009/http-probe.mjs`로 실행한다. 기준 Backend를 localhost:18089에서 실행해야 한다. production API adapter와 ruleset header 검증 코드를 TypeScript transpile로 로딩하며 UI translate 함수만 headless 오류 표시용으로 대체한다. 결과 JSON은 같은 경로를 갱신한다. 브라우저 테스트가 아니다.

**미완료:** 이 실행 환경의 computer-use inventory는 browser 0개이며 in-app browser 생성은 `Browser is not available: iab`로 실패했다. 따라서 실제 브라우저 happy/partial/unsupported/recovery/대용량 분수/stale 응답, 좁은 화면과 keyboard 시각 검증을 수행하지 못했다. DOM 테스트와 HTTP 근거를 실제 브라우저 PASS로 간주하지 않는다. 독립 리뷰도 별도 완료 주장하지 않는다.

## 독립 리뷰 안내

`frontend/src/features/crafting/basic-paths/BasicPaths.tsx`의 query 실행/취소·입력 store·checkpoint 선택, `api.ts`의 lossless 요청/응답 검증과 BigInt 표시, `basic-paths.css`의 좁은 화면 처리를 확인한다. 접점은 `goal-filter/CraftStart.tsx`의 BasicPaths props 한 곳이다. tests는 동일 디렉터리의 BasicPaths.test.tsx 및 api.test.ts에 있다. API DTO 기준은 BasicPathController.Response와 BasicPathService.Request다.

실제 브라우저에서는 Craft Support→실험 section을 열고 초기 아이템 미변경·미선택 목표를 확인한다. Solar Rare 단일 nonfractured explicit에서 Apprentice’s 주문피해 tier/CHAOS 반복을 선택해 100/300/500 COMPLETE를 확인한다. 복수 explicit 상태의 partial, 다른 base·Omen 미지원, 사용자가 저장한 실패/복구 checkpoint와 별도 확률을 확인한다. 긴 분수 disclosure, 375px 화면, Tab/Enter/Space/select 탐색, 닫기 focus 복귀를 확인한다. 응답을 지연하여 목표·원문·history·ruleset 변경/패널 닫기/중복 클릭 이후 stale 결과가 나타나지 않는지 확인한다.
