# Omen 조합과 로컬 후보 미리보기 — 2026-10-04

기준 `0c044759`, branch `workbench/20261002`. 기존 legacy5와 사용자 요청한 omen 조합/후보 미리보기 묶음의 구현·최종 검증을 완료했다. 기존 검증113에서5종을 추가 검증해 현재 범위 구현/검증118, pending26, deferred76이다. 사용자 서버18080/18081은 갱신하지 않았으므로 로컬 검증 완료와 live 제공을 구분한다. 게임 엔진 내부 joint odds/실패 소모까지 확인했다는 뜻은 아니다.

## 제거 순서와 미리보기

Sinistral Erasure + Whittling: unlocked explicit → prefix → 해당 집합의 최저 `requiredItemLevel` → 동률1/N → 제거 후 일반 방향의 weighted addition pool. 낮은 level의 suffix가 있어도 prefix 집합보다 우선하지 않는다. Greater/Perfect Chaos의 minimum-added-level35/50은 **추가 후보**에만 적용한다.

이는 명시 사용자 규칙이다. audit가 모든 실제 게임 순서와 확률을 독립 검증했다는 의미가 아니다. `uniform-removal-v1` reason도 사용자 지정 합성이라고 표시한다. frontend는 현재 ItemState와 catalog를 읽어 모든 후보를 orange로 표시한다. hover별 API 호출 없음. 후보 title과 모델1/N 설명을 함께 제공하고, 활성화 해제/아이템 변경 시 파생 표시를 갱신한다. 제품 UI는 현재 영어이며 병행 i18n 작업은 별도 worktree에서 진행한다.

## 허용·미확인 조합

| 조합 | 지원 | 근거/경계 |
|---|---|---|
| Sinistral/Dextral Erasure + Whittling | ordinary/Greater/Perfect Chaos | 사용자 지정 prefix→level 순서와 대칭 suffix 모델, 원자적 거부와 소모 검증 |
| Sinistral/Dextral Annulment + Greater Annulment | ordinary Annulment | audit의 Wiki prefix 예시와 대칭 suffix 모델; chosen side unlocked≥2; 서로 다른2개 순차 제거 |
| Homogenising Exaltation + Greater Exaltation | ordinary/Greater/Perfect Exalted | 기존 예외 보존·강화 화폐 합성; 시전 전 tag 집합 고정; 모든1차 branch에2차 pool 필요 |
| 단일 directional Coronation/Exaltation | ordinary/Greater/Perfect matching family | side eligibility 후 기존35/50·highest eligible type fallback |
| 단일 Erasure/Whittling | ordinary/Greater/Perfect Chaos | 제거 효과 후 기존 tiered replacement pool |
| 단일 Homogenising | ordinary/Greater/Perfect matching family | tag eligibility 후 기존35/50 pool; conditional published weight |
| 기타 same-trigger | 미지원 | 반대 side는 제품 제한 충돌; 다른 조합은 미확인. 일괄 허용 없음 |
| Greater Exaltation multi-add + Greater/Perfect | 지원 모델 | 단계별 minimum-level pool·conditional weight·완전한2-result branch 선검사 |

tiered 효과 합성과 Dextral 대칭은 사용자 승인된 가역 구현 선택이다. 실제 게임에서 모든 조합을 재현했다는 보증과 구분한다. tiered matching action은 `tiered-omen-composition-v1` UNVERIFIED ledger를 남긴다. source modifier weight/range/tag와 fallback 규칙은 변경하지 않는다. success는 matching omen각1소모, unrelated 유지. refusal은 draw 전 상태·omens 보존이라는 서비스 정책이며 게임 실패소모 사실을 주장하지 않는다.

유지하는 block의 정확한 의미: count2 + directional addition의 두 결과 모두 side를 따르는지와 side slot1개의 partial action/소모, total free slot1개 및 annul target1개의 partial 동작, Homogenising+directional의 tag/side priority와 zero-overlap 동작은 audit가 확립하지 않았다. 추천은 모든 draw에 제한을 적용하고 전체 branch 선검사 후 원자적으로 실행하는 모델이지만 이번 allowlist에는 audit 예시가 있는 제거 조합과 기존 Homogenising/count 예외만 넣는다. 다른 조합이 실제 게임에서 금지됐다고 표현하지 않는다.

## 남은 게임 검증

modifier level 없는 special affix와 Fractured Whittling은 사용자 인게임 검증까지 WB-042에 보류한다. 누락 level을0으로 채우지 않는다. Backend ModifierDefinition은1..100을 요구하며 frontend는 해당 level이 없거나 유효하지 않으면 preview를 중단한다. Fractured가 하나라도 있는 Whittling의 기존 refusal을 유지한다. legacy Alchemy draw order, Greater Annulment1-target, 다른 same-trigger 병용, 실제 실패소모도 별도 근거 gap이다.

## 검증 환경

`codex/omen-bundle-20261004`의 전용 복사본과 Compose18280/18281을 사용한다. PostgreSQL tmpfs와 전용 Redis, synthetic fixtures, 새 headless browser context를 사용하며 기존 volumes·DB·환경설정·사용자 storage를 보존한다. frontend 원본은 read-only mount로 읽고 `frontend-check`에서 npm/build를 실행한다. 이전 원본 출력 덮어쓰기 auto-review 거부를 우회하지 않는다. Backend/Frontend/API/browser는 공유 QA slot 승인 후 순차 실행한다.

실행 결과와 원본 실패는 [검증 근거](evidence/workbench-omen-composition-validation-2026-10-04.json)에 기록했다. 최종 Backend362 unit/ArchUnit +6integration 및 check/jooq/jar, FE npmci/lint/type/format/build PASS. FE는 전체 실행의 변경 없는322개 통과와 최종 MaterialStash22개 통과를 합쳐 고유344개를 검증했다. 소스537개 SHA 일치; 제품 소스 변화가 없어서 성공한 전체 검사를 반복하지 않았다. API640/9base, browser95/page errors0,390px/1440px candidate-card visual QA와 film reload PASS. 기존 full343/344와 관련21/22, 최초API18/browser8 후 harness 실패를 이력으로 보존한다. 격리 services는 최종 검사 후 정상 stop했고 사용자4 services·기존DB/storage는 보존했다.

## 완료한 한정 복구 이력

운영명세의 기본 복구2회 후 부모가 제시한 사용자 명시 `중단없이 진행` 지시와 사용자 지시 우선 조항을 대조해 기존 세 harness 결함만 추가 bounded1묶음으로 복구했다. 기존2회 횟수·실패를 삭제하거나 stage/session으로 초기화하지 않았다. repaired scripts를 먼저 commit한 뒤 i18n standalone QA가 slot을 반환할 때까지 heavy 검사를 하지 않았다. slot 승인 후 FE22→API→browser 순차 재검증이 모두 통과했다. 아래는 완료한 복구 내용이다.

1. 최종 정정된 `MaterialStash.test.tsx`22개 PASS. 기존 ordinary Essence19×4=76, 별도 special4와 Delirium/Insanity 제외를 유지했다. 마지막 failure는 추가한 special ID 순서의 fixture 오류였으며 실제 목록은 변경하지 않았다.
2. API 읽기 전용 재진단으로 Solar fixture의 배열은 이미 canonical 순서였음을 확인했다. 이전 배열순서 추론을 정정한다. initial.state는 `StateBucket`이라 optional `augmentSockets/catalystQuality`가 없고 apply.state는 `ItemState`라 null 필드가 생기는 계약 차이가 raw equality의 구체적인 원인이다. fixture에 기존 nullable defaults를 명시하고 기존 정렬 계약만 적용한다. 다른 모든 필드와 각 roll·fractured flag를 보존하며 원자적 events/assumptions/소모 검사는 유지한다. 실패 request/raw response/parsed response를 새 파일에 캡처하고 나머지9-base/legacy5 검사를 완료한다. repaired 실행 전까지 최초 실패를 PASS로 재표시하지 않는다.
3. browser restore에서 선택한 film의 active ID/cursor와 DOM step이 반영될 때까지 기다린 뒤 Previous를 조작한다. 같은 active film은 비동기 재선택을 생략한다. 각 Previous 후 cursor와 DOM 감소를 기다린다. Solar 성공8개는 보존하고 나머지9-base, legacy5, Greater+Homogenising tiered 실제 동작, 390px screenshot, reload 검사를 완료한다. 실패 시 마지막 request/response/film/DOM 상태를 새 파일로 캡처한다.

재현 script는 [QA harness](../scripts/qa/omen-composition-20261004/README.md)에 commit했다. 복구 중 product code 변경이나 assertion 삭제 없음. Node syntax3개 PASS와 API640/browser95 runtime PASS를 별도로 기록한다.390px에서 candidate 카드/안내와 document overflow 검사는 통과했지만 기존 고정 화폐 canvas 일부가 panel 내부에 잘리는 모습은 다음 요청된 UX bundle 검토 항목이다. 이번에 responsive redesign을 추가하지 않았다.

`codex/omen-bundle-20261004`의 최초 실패 scripts/logs와 최종 결과·screenshots·source-equivalence를 보존했다. Runtime jar와 frontend dist는 격리 폴더에만 있다. 병행 i18n 최종 branch `242954aa0822e572cb999a439d0b7a1abfb5f24a`는 부모 보고 기준351FE/224browser standalone PASS이며 이번 branch에 통합하지 않았다. 이후 통합 소스의 별도 검증이 필요하다. 이 로컬 완료는 push/merge/deploy 완료를 의미하지 않는다.

근거: 기존 `codex/omen-audit-20261004/OMEN_RULE_AUDIT_20261004.md`, `all-32-omens.json`; [서비스 범위](workbench-service-scope-2026-10-04.md); [WB-042](../ISSUES.md). broad research 재수행 없음. 다음 묶음은 catalyst 실제 적용과 UX, 이후 Korean-first i18n 통합이며 이번 bundle은 기존 영어 UI를 유지한다.
