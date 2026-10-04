# Workbench 최신 범위와 legacy 징조 5종 — 2026-10-04

**최종 검증 재개가 필요한 로컬 구현 checkpoint**다. 최신 사용자 지시로 기폭제 서비스 제외가 취소됐으므로 기존 메뉴·typed 품질 입력·품질 미리보기 API를 보존한다. 실제 기폭제 적용과 새 UX는 피드백 수집 완료 후 조정한다. 사용자 테스트 중인 서버와 실제 브라우저 저장소는 변경하지 않았다.

최신 후속 checkpoint: [Omen composition](workbench-omen-composition-2026-10-04.md), [실행 근거](evidence/workbench-omen-composition-validation-2026-10-04.json), WB-042. 명시 승인 후 tiered currency와 한정된 병용·로컬 후보 표시를 구현했고 Backend362+6/필수 검사 및 FE 정적검사/build를 통과했다. 전체 FE343/344, 마지막 관련21/22, API18/browser8 이후 harness failure와 복구2회 한도로 중단했다. 최종 legacy5/9-base 실행완료 집계는 여전히 미완료이며 구현118/검증113을 유지한다. 아래 검증 절은 이전 checkpoint 이력이다. 같은-trigger 전체·Greater/Perfect 전체 거부는 최신 allowlist로 대체했고 미확인 게임 의미는 WB-042에 구체적으로 남겼다.

## 범위 검산

220개 고유 ID를 검산했다. 기존 active155에서 품질 화폐3 + 특수 Essence2 + Wisdom/Chance/Extraction3 + Catalysing1 + Necromancy2 =11개를 중복 없이 보류한다. 기폭제26은 개발 범위로 복귀했고 실제 적용은 아직 미구현이다. 기존 보류65는 그대로다. [ID별 검산](evidence/workbench-service-scope-2026-10-04.json).

| 구분 | 개수 | 의미 |
|---|---:|---|
| 등록 inventory |220|현재 획득 가능·서비스 제공을 의미하지 않음|
| 현재 개발 범위 |144|기본111 + opt-in legacy7 + 기폭제 미구현26|
| 현재 범위 구현체 |118|기본111 + legacy7; 이번5의 전체 runtime 검증은 미완료|
| 현재 범위 미구현 |26|일반/제련 Catalyst13+13 실제 적용|
| 보류 |76|기존65 + 신규11; 기존 구현 Alloy8 포함|
| registry 전체 구현체 |126|현재118 + 보존 Alloy8|
| registry 전체 미구현 |94|현재26 + 보류 미구현68|

9base·source modifier1487·기본 제작용 일반 품질 표시는 유지한다. 범위 상태는 registry `serviceScope`로 효과 구현 상태와 분리한다. 118은 전체 검증 완료 숫자가 아니다.

## 기폭제와 보존 경계

이번 작업의 기폭제 메뉴숨김·입력비활성·API 차단 변경은 철회했다. `Catalysts` 메뉴, 기존 Solar/Iron typed 품질 시작 입력, `POST /api/v1/crafting/workbench/quality-display` bounded 표시 계약을 유지한다. 타입·검증기·원래 roll·film·수치 모델과 테스트를 삭제하지 않았다. foundation 보존은 catalyst per-use 적용 완료가 아니다. typed state 화폐 상호작용과 품질0의 안전한 거부는 그대로다. 원문 역산·film 재작성·DB migration·volume reset은 없다.

Catalysing Exaltation과 Necromancy2는 계속 보류한다. 훼손(Desecration) 신규 조사·개발·기반 구축은 하지 않는다. 기폭제 재노출은 Catalysing 재도입을 뜻하지 않는다. 실제 적용의 미확인 규칙은 기존 품질 보고서에 남아 있으며 이번에 추측해서 구현하지 않았다.

## 징조 5종

[현재 PoE2DB Omen 원문](https://poe2db.tw/us/Omen)을 직접 확인했다. 개별 retired item 페이지에는 metadata만 남아 있어 효과는 aggregate 원문으로 확인했다. [공식 0.3 Item Changes](https://www.pathofexile.com/forum/view-thread/3826682)는 다섯 종류의 획득 중단을 명시한다. 이는 기존 개체 효과 삭제나 사용 불가의 증거가 아니다. 기존 개체 작동을 별도로 보증한 [Homogenising 0.4 문구](https://www.pathofexile.com/forum/view-thread/3883495/filter-account-type/staff)를 이번5에 자동 적용하지 않는다. 현재 획득 재개는 주장하지 않는다.

5종의 PoE2Wiki와 개별 PoE2DB 원문, aggregate Omen을 프로젝트 Docker에서 직접 읽었다(11 URL HTTP200). 캡처·조회시각·SHA256은 `codex/service-scope-20261004/source-manifest.json`에 보존한다. Wiki는 커뮤니티 자료이며 공식 GGG 작동 보증으로 취급하지 않는다.

| Omen | 확보한 효과 | 구현체와 제한 |
|---|---|---|
| Sinistral Alchemy |다음 Alchemy 결과에서 prefix 최대 개수|기존4-modifier 모델에서 prefix3/suffix1; family·ilvl·slot·published weight; 모든 family branch 완결성 선검사|
| Dextral Alchemy |다음 Alchemy 결과에서 suffix 최대 개수|대칭: suffix3/prefix1|
| Sinistral Coronation |다음 Regal이 prefix만 추가|기존 AdditionRules 방향 pool; Magic→Rare·기존 roll 유지|
| Dextral Coronation |다음 Regal이 suffix만 추가|대칭: suffix 추가|
| Greater Annulment |다음 Annulment가 modifier2개 제거|서로 다른 unlocked explicit2개; 단계별 제거 ledger; implicit·fracture·나머지 roll 유지|

별도 기폭제나 훼손 시스템 의존이 없다. 공통 base·slot·family·화폐 엔진을 재사용한다. 기존 Homogenising2와 이번5는 `Show legacy Omens` opt-in이다. 획득 가능 재료로 표시하지 않는다.

게임 사실 gap은 Alchemy 내부 추첨 순서·정확한 joint odds, Greater Annulment의 제거 가능1개 동작, 같은 trigger 조합, Greater/Perfect Regal, 실제 실패 소모다. 해당 예외만 원자적으로 거부한다. fractured Magic은 기존 validator가 거부하며 경계를 완화하지 않는다. 실패 자원 보존은 서비스 정책이고 실제 게임 실패 소모의 증거가 아니다.

사용자 승인된 가역적 모델 선택: Alchemy 최대방향3개 먼저·반대1개 후 conditional published weight draw. 내부 순서를 확인했다고 하지 않고 `legacy-alchemy-order-v1` UNVERIFIED assumption과 film evidence에 남긴다. 정확한 game odds로 표시하지 않는다. WB-041에 gap과 rollback을 기록했다.

## 검증 상태와 재개

[이번 실행 근거](evidence/workbench-service-validation-2026-10-04.json)는 실행과 scope 변경 전후를 구분한다. Backend 전체 필수 검사는 기폭제 scope 재변경 전 PASS(357 unit +6 integration, check/generateJooq/bootJar). legacy5 엔진 이후 변경은 없지만 기폭제 API·registry 복귀 때문에 최종 소스 전체 PASS라고 주장하지 않는다.

Frontend는 사용자 서버를 보존하려 상위 codex의 복사본에서 원본을 읽기 전용으로 연결해 검사했다. npm ci/lint/typecheck PASS 후 `Homogenising.test.tsx`의 변경 label format으로 중단했다. unit/build/이번5 runtime API/browser는 완료하지 못했다. Backend fixture 복구와 Frontend 복사본 준비 복구로 운영 명세의 작업 전체 재시도2회를 사용해 추가 재시도는 중단했다. label format은 소스에서 정리했다. 최신 scope의 검증 재개에는 후속 진행 결정이 필요하다. 기대값·보호 경계를 완화하지 않았다.

별도 Docker 환경 `codex/service-scope-20261004/compose.yaml`(18180/18181)은 **준비만 하고 기동하지 않았다.** 기존 사용자 서버18080/18081은 그대로다. 준비된 API/browser scripts는 철회된 기폭제 숨김 범위를 일부 기대하므로 최신 scope로 수정 후 실행해야 한다. 사용자 피드백 수집 후 최신 소스를 격리 복사·검증한다. 로컬 checkpoint만 허용하며 push·merge·deploy는 하지 않는다.
