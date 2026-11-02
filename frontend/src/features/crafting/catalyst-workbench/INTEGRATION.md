# 담당 D 통합 안내

기준 커밋: `6040357faf4e39cc989991d44ef8d04c7119dbd0`.
브랜치: `worker/d-numeric-divine-catalyst`.

## 구현과 의미

- `localizedModifiers.ts`의 검증된 수치 변환과 `ItemCard`의 표시 속성을 소수 첫째 자리로 반올림한다. 정수, 단위, 범위, 원본 세부정보와 미해석 행은 보존한다. `numeric-display/attributeNumbers.ts`는 표시 전용이며 API나 저장 값으로 역변환하면 안 된다. 영어 복사 텍스트는 `ItemCardLine.numericLocale = 'en'`을 사용한다.
- 고정 옵션은 catalog의 `IMPLICIT`이다. `fractured`는 별도의 명시 옵션 잠금이며 동일 개념으로 취급하지 않았다. 일반 `DIVINE`은 재굴림 가능한 비분열 명시 옵션만 대상으로 하고 implicit은 그대로 둔다. 명시 옵션에 변동 범위가 없으면 적용하지 않는다. 프런트엔드 응답 검증도 일반 Divine의 implicit 변경과 implicit 재굴림 이벤트를 거절한다.
- 기존 소스와 `WorkbenchExtensionTest`에 명시된 `Omen_of_the_Blessed`의 implicit 전용 재굴림은 유지한다. 이 징조가 있을 때만 implicit 변경을 허용하고 명시 옵션 변경은 거절한다.
- 기폭제의 게임 1회 사용 효과를 새로 추정하지 않았다. 기존 simulator 정책(1회에 현재 허용 최대 품질까지 설정, 이미 더 높은 품질은 보존, 종류 교체)과 규칙 식별자를 유지한다. 기존 `applyCurrency` 경로로 적용하고 `catalystProjection`으로 표시만 투영한다.

## 통합자가 수정할 CraftingPage 연결

1. 기존 `apply()`는 이미 `workbenchCurrencyActions[selected.id]`를 읽어 `applyCurrency(...)`를 호출하고 `recordCraft(...)`로 저장한다. 이 경로를 기폭제 화폐 선택에도 그대로 유지한다. `catalystCurrencyAction(currencyId)`는 기폭제인지 별도로 구별할 때만 쓰는 선택적 resolver다. 별도의 품질 수정 API나 `setBase` 호출을 추가할 필요가 없다.
2. 베이스 폼의 `starting-quality-type`, `starting-quality-amount` 및 관련 품질 입력 검사와 `qualityType/qualityAmount` UI 상태를 제거한다. 새 베이스 배치는 기폭제 품질 없이 시작한다. `startBase`의 기폭제 인수 기본값은 이미 `null`이다. 기존 film 복원·불러온 텍스트의 `catalystQuality`는 삭제하거나 초기화하지 않는다.
3. 화폐 창고의 기존 `CATALYST` 분류에 일반/정제 기폭제 26종을 그대로 노출한다. 기존 `workbenchCurrencyActions`와 서버 availability를 사용하고 서버의 아이템 종류 제한(일반: 반지/목걸이, 정제: 검증된 기본 주얼)을 존중한다.
4. 기존 `catalystQuality.test.tsx`의 마지막 테스트는 베이스 폼의 품질 입력 UI를 검증한다. 폼 제거 통합 시 화폐 선택→기존 적용 버튼→품질 표시→reload 경로로 해당 테스트를 갱신해야 한다. 이 브랜치는 공통 화면을 수정하지 않아 기존 테스트를 보존했다.

## B의 미공개 룬 표시 연결점

`ItemCard.tsx`의 내부 `Lines`에서 `sourceLine`을 받고 `line.text` 표시값만 반올림한다. B의 새 미공개 컴포넌트는 해당 줄의 `summary`/텍스트 위치에 통합하면 된다. `line.detail`과 원본 상태를 유지해야 한다. D는 룬 문자열·이미지나 미공개 상태를 새로 구현하지 않았다. `itemCardData.ts`의 추가 필드 `numericLocale`은 기존 인터페이스와 호환되는 선택 필드다.

## 기존 backend 테스트의 의도 변경

`WorkbenchControllerTest.java`의 `catalystQuality...` 테스트 중 약 232행의 Divine 요청은 분열된 명시 옵션 하나만 가진 아이템이다. 기존에는 implicit을 굴려 `applied=true`였지만 이제 올바른 결과는 `applied=false`이며 상태/기폭제 품질은 그대로다. 이 파일은 담당 D 소유가 아니므로 변경하지 않았다. 통합자가 해당 assertion을 갱신해야 한다. 새 `DivineImplicitProtectionTest`가 일반 implicit 보존, 대상 없는 no-op, 분열 보호와 Blessed 예외를 검증한다.

검증 결과와 실행 환경 제한은 작업 완료 보고에 별도로 기록한다. 브라우저 검증·서버 변경·push는 수행하지 않는다.

## 전체 검사에서 발견한 추가 통합 사항

- `manualReveal.test.ts`의 표시 기대값 `maximumFractionDigits: 20`은 1로 갱신해야 한다. `storedModifierRoll`과 실제 저장 숫자의 정밀도 검사는 그대로 유지한다.
- `remainingDisplay.test.tsx`의 per-minute/permyriad 변환 표시 기대값도 한 자리로 맞춘다. 예: `15.55 → 15.6`.
- `ReviewedBelts.test.ts`의 Fine Belt 표시 기대값은 `0.17 → 0.2`다. 원본 분당 10 수치와 카탈로그는 그대로 유지한다.
- 위 파일은 담당 D 소유 밖이므로 변경하지 않았다. 전체 frontend 검사에서 이 세 가지의 기존 표시 기대값 실패와 부하 중 timeout 등 총 11개 실패를 확인했다. 담당 직접 회귀 50개는 통과했다.
- 기준 Git의 `catalog/manual-reveal.json` 바이트 SHA-256은 `f1e3af497c5527269607c481bd1947c7741f0c74b8cb6d8f2edff54f96b8cdf9`(LF)인데 `crafting/ruleset-v1.json` 봉인 값은 `61dc6fb49236efe8d8a5f7b0537d59b1bef13f337cd3ac68848388ec5308f844`(원본 CRLF)이다. 원본 작업트리의 파일은 봉인 값과 일치한다. 깨끗한 worktree에서도 존재하는 기준 커밋 문제이며, 통합자는 원본 봉인 바이트를 보존하도록 Git 속성/리소스 표현을 별도로 처리해야 한다. D는 공통 JSON/manifest를 수정하지 않았다. Java 검증은 이 원본 리소스를 임시 컨테이너에만 복사한 조건을 구분해서 보고한다.

## 최종 검증 결과

- Frontend 직접 회귀 50/50 통과. `lint`, `typecheck`, `format:check`, `build` 통과. 대형 번들 경고는 남아 있다. `rolledText`는 텍스트 내보내기에도 사용되므로 기존 정밀도를 유지하며 `localizedModifierText`와 `ItemCard`에서만 반올림한다.
- 전체 frontend 검사: 2,434개 중 2,423개 통과, 11개 실패. 위 표시 기대값 3건 외에 timeout 6건, `scenarioCode`의 `SCENARIO_BUSY` 1건, `goalFilter` UI 버튼 조회 1건을 확인했다. 이들을 기존 실패라고 단정하지 않았고 테스트를 삭제하거나 제한 시간을 늘리지 않았다.
- Java 21 임시 컨테이너에서 원본 봉인 리소스 사용 조건으로 관련 단위/API 회귀 54개 중 53개 통과. 새 `DivineImplicitProtectionTest`는 5/5 통과. 유일한 실패는 `WorkbenchControllerTest.typedQualityInspectionPreservesRollsAndRejectsLossyOrCoercedProperties`의 구형 `applied=true` 기대값이다. `spotlessCheck`와 `bootJar`는 통과했다.
- 원본 봉인 리소스 보정 전 전체 backend 검사는 988개 중 647개 실패했으며, 대다수는 위 체크섬 문제의 초기화 연쇄 실패다. 공통 JSON이나 manifest를 고쳐 성공으로 가장하지 않았다.
- `npm ci`는 registry DNS 실패로 완료하지 못해 동일 기준 저장소의 설치된 `node_modules`를 독립 작업폴더로 복사했다. Docker 통합/codegen(`check`의 integrationTest 및 `generateJooq`)은 이 Worker에서 실행하지 않았다. 브라우저 검증과 서버 변경, push도 수행하지 않았다.
