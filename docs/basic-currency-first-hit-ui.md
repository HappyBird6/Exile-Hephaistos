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
- 입력·시작 상태·목표·history revision·실제 ruleset/provenance 변경은 결과와 실행 토큰을 폐기한다. 이전 query는 취소하고 격리한다. 변경 이전 provenance가 돌아와도 이전 실행을 자동 재개하지 않는다.
- 결과 query는 자동 retry·focus/reconnect refetch·재마운트 refetch를 하지 않는다. provenance는 focus/reconnect와 명시적 새로고침으로 재확인하며, 같은 검증값을 재조회하는 동안과 재조회 실패 시에는 기존 결과와 진행 중 요청을 유지한다. 실제 검증값이 바뀌면 폐기한다. 오류 후 계산 버튼으로 명시적 재시도할 수 있고 중복 클릭은 같은 요청을 중복 발행하지 않는다. 패널 닫기·탭 비활성화는 요청과 결과를 폐기한다.
- native details/select/button, fieldset/legend, label, alert/status 및 닫기 후 summary focus 복귀를 사용한다. 700px 이하 한 열, 긴 분수·JSON wrapping과 스크롤을 적용한다. 실제 좁은 화면·키보드 브라우저 검증은 아래 미완료 경계에 해당한다.
- 사용자 승인 정책에 따라 기존 6개 locale dictionary와 useI18n을 사용하고 신규 문구와 오류 표시를 각 언어로 번역한다. 기존 localizedAction과 검증된 modifier 번역 표시를 재사용한다. 원문 catalog ID와 확률근거는 그대로 보존한다. AGENTS.md의 오래된 영어 전용 문장은 승인된 6개 언어 정책으로 정정한다.

## 검증

현재 `BasicPaths.test.tsx`와 `api.test.ts`로 입력·응답·취소·복구·stale 응답·정확한 분수 표시를 검증한다. `scripts/check-display-coverage.mjs`는 공통 표시 자료를 대조한다. 실행한 검사와 실제 브라우저·서버 검증은 구별한다. 이전 버전의 일회성 HTTP probe·실행 결과·수집 이력은 제거했다.

실제 화면 검증에서는 초기 아이템·목표 미변경, 100/300/500회 결과의 complete/partial/unsupported 구분, 별도 복구 확률, 긴 분수 표시·좁은 화면·키보드 조작, 입력/규칙 변경 후 늦은 응답 폐기를 확인한다. 현재의 source·규칙은 [자료 분류](evidence/0.5.5/README.md)와 [통합 유지보수](development.md)를 따른다.
