# 전체 Omen 규칙 감사 — 2026-10-04

읽기 전용 감사입니다. source HEAD 0c04475, branch workbench/20261002 clean. 사용자 서버 18081/18080, DB·volume·브라우저·localStorage 무변경. 새 check/build/apply 및 재시작 없음. 보고서·raw 증거만 상위 codex에 저장합니다.

## 즉시 답변 및 사용자 지정 규칙

Sinistral Erasure + Whittling과 Omen × Greater/Perfect는 실제 게임 금지 근거로 막은 것이 아니라 미구현/미검증 공통 guard로 막혀 있습니다. 현재 API의 Erasure+Whittling 응답은 ordinary/Greater/Perfect Chaos 모두 multiple-omens verification refusal입니다. 단일 Whittling ordinary Chaos 허용, Greater/Perfect 거부; unrelated Blessed는 tiered Chaos를 막지 않습니다. family mapping은 정상입니다.

사용자가 이번 피드백에서 조합 순서를 직접 지정했습니다: **Sinistral Erasure 접두 삭제 후보 필터 → 그 안의 최저 modifier requiredItemLevel → 동률 1/N**. 이 조합의 순서 미정 blocker는 해소했습니다. 이는 사용자 지정 제품 규칙이며 독립적으로 증명한 게임 조합 규칙이라고 표시하지 않습니다. 주황 preview는 같은 candidate 순서로 로컬 계산하고 매번 API를 호출하지 않는 요구입니다. 특별 modifier에 level이 없는 경우·Fractured 정확한 상호작용·상위 화폐 조합까지 승인한 것으로 확대하지 않습니다. 지금 구현하지 않습니다.

## 관측과 근거

- local registry32 = ACTIVE IMPLEMENTED18 (default11 + opt-in legacy7) + DEFERRED14. 전체 registry220 서비스144/보류76와 구분합니다.
- live registry32 = IMPLEMENTED13 + PENDING19; legacy-five 미반영. live ruleVersion: solar-workbench-abyss-essence-v16-homogenising-legacy-v1. UI HTML bundle만 GET으로 확인; 브라우저 조작 안 함.
- 39 isolated synthetic POST /actions 및 GET registry/initial로 지원 ID/조합 boundary만 관측. Rare Solar fixture 하나이므로 Magic Regal 성공·Alchemy 성공 및 모든 base matrix를 새로 검증했다고 말하지 않습니다. no apply.
- 최신 [PoE2DB Omen](https://poe2db.tw/us/Omen) 원문에서 아래32 효과 확인. effect 카드만으로 조합 순서/정확 odds/실패 소모를 확정하지 않습니다.
- [공식0.3](https://www.pathofexile.com/forum/view-thread/3826682): legacy-five 획득 중단 및 Greater/Perfect addition minimum-level 도입. 획득 중단은 기존 개체 사용 불가의 증거가 아닙니다. 새5종 기존 개체 functional을 공식0.4 Homogenising 문구로 일반화하지 않습니다.
- [공식0.4](https://www.pathofexile.com/forum/view-thread/3883495/filter-account-type/staff): Homogenising2 drop 중단, existing functional을 명시. 현재 재도입은 주장하지 않음. 공식은 web read 성공, Docker raw fetch403 기록; 접근 우회 없음.
- [Wiki Omen](https://www.poe2wiki.net/wiki/Omen): crafting 효과 중첩을 설명하고 Sinistral Annulment+Greater Annulment 예시를 제시. 따라서 모든 same-trigger 게임금지 주장에는 근거 없음. Wiki는 community secondary source로 구분; 이번 보고서가 그 조합 구현을 승인하지 않음.
- [Wiki Whittling](https://www.poe2wiki.net/wiki/Omen_of_Whittling): lowest item-level requirement와 tier 구분. [Wiki Erasure](https://www.poe2wiki.net/wiki/Omen_of_Sinistral_Erasure)는 관련 fracture visual-bug 이력 제공. visual-bug 수정만으로 실제 선택 순서를 추론하지 않음.
- source captures: sources.json + raw HTML; runtime-registry.json/runtime-actions.json; 기존9-base Whittling fracture 진단은 ../service-scope-20261004/whittling-readonly.json.

## 32종 효과·서비스·등급 표

각 effect는 원문 요약, 등급 제한은 **현재 제품 제한**입니다. deferred source tooltip/read inventory는 보존하며 사용 가능과 구분합니다.

| Omen ID | 확인 효과 / trigger | 최신 서비스 | live effect | 현재 등급 경계 |
|---|---|---|---|---|
| Omen_of_Dextral_Crystallisation | 접미 제거 / Perfect/Corrupted Essence | ACTIVE | IMPLEMENTED; API 200 | 등급명이 Perfect인 모든 Essence가 아님: replacementEssenceModifiers가 있는 지원 Perfect/Corrupted만 발동 |
| Omen_of_Sinistral_Crystallisation | 접두 제거 / Perfect/Corrupted Essence | ACTIVE | IMPLEMENTED; API 200 | 등급명이 Perfect인 모든 Essence가 아님: replacementEssenceModifiers가 있는 지원 Perfect/Corrupted만 발동 |
| Omen_of_Whittling | 최저 요구 level 제거 / Chaos | ACTIVE | IMPLEMENTED; API 200 | 일반 Chaos만; Greater/Perfect 미검증 보호 차단 |
| Omen_of_Sinistral_Erasure | 접두 제거 / Chaos | ACTIVE | IMPLEMENTED; API 200 | 일반 Chaos만; Greater/Perfect 미검증 보호 차단 |
| Omen_of_Dextral_Erasure | 접미 제거 / Chaos | ACTIVE | IMPLEMENTED; API 200 | 일반 Chaos만; Greater/Perfect 미검증 보호 차단 |
| Omen_of_Sinistral_Alchemy | 접두 최대 / Alchemy | ACTIVE legacy opt-in | PENDING_RULE_VERIFICATION; API 422 | 일반 Alchemy만; 대응 Greater/Perfect Alchemy enum 없음 |
| Omen_of_Dextral_Alchemy | 접미 최대 / Alchemy | ACTIVE legacy opt-in | PENDING_RULE_VERIFICATION; API 422 | 일반 Alchemy만; 대응 Greater/Perfect Alchemy enum 없음 |
| Omen_of_Sinistral_Coronation | 접두 추가 / Regal | ACTIVE legacy opt-in | PENDING_RULE_VERIFICATION; API 422 | 일반 Regal/Exalted만; Greater/Perfect 조합 미검증 보호 차단 |
| Omen_of_Dextral_Coronation | 접미 추가 / Regal | ACTIVE legacy opt-in | PENDING_RULE_VERIFICATION; API 422 | 일반 Regal/Exalted만; Greater/Perfect 조합 미검증 보호 차단 |
| Omen_of_Corruption | 변경 보장 / Vaal | DEFERRED | PENDING_RULE_VERIFICATION; API 422 | 제공하지 않음; 게임의 등급/조합 적합성 미확인 |
| Omen_of_Greater_Exaltation | 두 옵션 추가 / Exalted | ACTIVE | IMPLEMENTED; API 200 | 일반 Exalted만; Greater/Perfect는 별도 보호 차단 |
| Omen_of_Sinistral_Exaltation | 접두 추가 / Exalted | ACTIVE | IMPLEMENTED; API 200 | 일반 Regal/Exalted만; Greater/Perfect 조합 미검증 보호 차단 |
| Omen_of_Dextral_Exaltation | 접미 추가 / Exalted | ACTIVE | IMPLEMENTED; API 200 | 일반 Regal/Exalted만; Greater/Perfect 조합 미검증 보호 차단 |
| Omen_of_Greater_Annulment | 두 옵션 제거 / Annulment | ACTIVE legacy opt-in | PENDING_RULE_VERIFICATION; API 422 | 일반 Annulment; 상위 등급 enum 없음 |
| Omen_of_Sinistral_Annulment | 접두 제거 / Annulment | ACTIVE | IMPLEMENTED; API 200 | 일반 Annulment; 상위 등급 enum 없음 |
| Omen_of_Dextral_Annulment | 접미 제거 / Annulment | ACTIVE | IMPLEMENTED; API 200 | 일반 Annulment; 상위 등급 enum 없음 |
| Omen_of_Answered_Prayers | 효과 추가 / Shrine | DEFERRED | PENDING_RULE_VERIFICATION; API 422 | 제작 화폐 등급 N/A |
| Omen_of_Secret_Compartments | 재개방 / Strongbox | DEFERRED | PENDING_RULE_VERIFICATION; API 422 | 제작 화폐 등급 N/A |
| Omen_of_the_Hunt | Spirit 발현 / Possessed kill | DEFERRED | PENDING_RULE_VERIFICATION; API 422 | 제작 화폐 등급 N/A |
| Omen_of_Reinforcements | 동료 소환 / Rogue Exile | DEFERRED | PENDING_RULE_VERIFICATION; API 422 | 제작 화폐 등급 N/A |
| Omen_of_Homogenising_Exaltation | 기존 type 추가 / Exalted | ACTIVE legacy opt-in | IMPLEMENTED; API 200 | 일반 Exalted/Regal만; Greater/Perfect 미검증 보호 차단 |
| Omen_of_Homogenising_Coronation | 기존 type 추가 / Regal | ACTIVE legacy opt-in | IMPLEMENTED; API 200 | 일반 Exalted/Regal만; Greater/Perfect 미검증 보호 차단 |
| Omen_of_the_Blessed | implicit만 재굴림 / Divine | ACTIVE | IMPLEMENTED; API 200 | 일반 Divine; 상위 등급 enum 없음 |
| Omen_of_Sanctification | Sanctify / Divine on Rare | DEFERRED | PENDING_RULE_VERIFICATION; API 422 | 제공하지 않음; 게임의 등급/조합 적합성 미확인 |
| Omen_of_Catalysing_Exaltation | 전체 catalyst quality 소모·편향 / Exalted | DEFERRED | PENDING_RULE_VERIFICATION; API 422 | Catalysing Omen은 명시 보류; catalyst26 foundation은 복원 유지, 실제 사용 미구현 |
| Omen_of_the_Sovereign | Ulaman 보장 / Weapon/Jewellery Desecration | DEFERRED | PENDING_RULE_VERIFICATION; API 422 | 명시 보류 또는 원래 보류65; 새 Desecration 개발/연구 안 함 |
| Omen_of_the_Liege | Amanamu 보장 / Weapon/Jewellery Desecration | DEFERRED | PENDING_RULE_VERIFICATION; API 422 | 명시 보류 또는 원래 보류65; 새 Desecration 개발/연구 안 함 |
| Omen_of_the_Blackblooded | Kurgal 보장 / Weapon/Jewellery Desecration | DEFERRED | PENDING_RULE_VERIFICATION; API 422 | 명시 보류 또는 원래 보류65; 새 Desecration 개발/연구 안 함 |
| Omen_of_Putrefaction | 미공개 최대6·Corrupt / Desecration | DEFERRED | PENDING_RULE_VERIFICATION; API 422 | 명시 보류 또는 원래 보류65; 새 Desecration 개발/연구 안 함 |
| Omen_of_Light | Desecrated만 제거 / Annulment | DEFERRED | PENDING_RULE_VERIFICATION; API 422 | 명시 보류 또는 원래 보류65; 새 Desecration 개발/연구 안 함 |
| Omen_of_Sinistral_Necromancy | 접두 추가 / Desecration | DEFERRED | PENDING_RULE_VERIFICATION; API 422 | 명시 보류 또는 원래 보류65; 새 Desecration 개발/연구 안 함 |
| Omen_of_Dextral_Necromancy | 접미 추가 / Desecration | DEFERRED | PENDING_RULE_VERIFICATION; API 422 | 명시 보류 또는 원래 보류65; 새 Desecration 개발/연구 안 함 |

## 각 Omen eligibility·후보 순서·소모·근거 표

공통 V: snapshot/base/conditions/roll domain validator 먼저; catalog 밖/미해석 modifier·없는 level을 임의0/T1로 보정하지 않음. 지원 추가 후보는 ilvl·weight·family·slot 필터, source missing weight 정책도 유지. 공통 C: 다른 trigger 동시활성 허용; 같은 trigger는 UI/Backend 차단. 유일한 기존 exception은 Greater Exaltation + Homogenising Exaltation 일반 Exalted. 공통 S: 성공 시 matching 각각1 소모, unrelated 유지; availability refusal은 state+전체 Omens 보존·random draw 전. unsupported ID는 Problem Details 거부. S는 서비스 원자성 계약이지 실제 게임 실패소모 증거가 아님.

| Omen ID | 대상/슬롯/Fractured/level | 후보 결합/순서 | 동시활성/소모 | 기존 테스트·의존 |
|---|---|---|---|---|
| Omen_of_Dextral_Crystallisation | Rare; unlocked 해당 side ≥1; 제거 각 branch에서 보장 target의 ilvl/family/slot 확인; Fractured 보존 | unlocked → side → 균등 제거 → 해당 Essence target/제품 수치모델; 실패 가능 branch가 하나라도 있으면 draw 전 거부 | C + S | CrystallisationTest, AbyssEssenceTest 및 9 base Essence 테스트 |
| Omen_of_Sinistral_Crystallisation | Rare; unlocked 해당 side ≥1; 제거 각 branch에서 보장 target의 ilvl/family/slot 확인; Fractured 보존 | unlocked → side → 균등 제거 → 해당 Essence target/제품 수치모델; 실패 가능 branch가 하나라도 있으면 draw 전 거부 | C + S | CrystallisationTest, AbyssEssenceTest 및 9 base Essence 테스트 |
| Omen_of_Whittling | Rare; min requiredItemLevel; tier 숫자가 아님; 동률 균등 가정; Fractured가 하나라도 있으면 전체 거부 | unlocked → min level → uniform 제거 → 남은 상태의 unrestricted addition pool → published-weight draw | C + S | WorkbenchExtensionTest.whittlingUsesModifierLevelRatherThanComparingTierNumbers |
| Omen_of_Sinistral_Erasure | Rare; 해당 side unlocked ≥1; Fractured 보존; 제거 후 해당 side만 추가하는 규칙 아님 | unlocked → side → uniform 제거 → 남은 상태의 unrestricted addition pool → published-weight draw | C + S | WorkbenchExtensionTest.affixOmensRestrictTheCorrectOperationAndConsumeOnlyTheMatchingOmen |
| Omen_of_Dextral_Erasure | Rare; 해당 side unlocked ≥1; Fractured 보존; 제거 후 해당 side만 추가하는 규칙 아님 | unlocked → side → uniform 제거 → 남은 상태의 unrestricted addition pool → published-weight draw | C + S | WorkbenchExtensionTest.affixOmensRestrictTheCorrectOperationAndConsumeOnlyTheMatchingOmen |
| Omen_of_Sinistral_Alchemy | Normal/Magic → Rare 4 mods; 최대 side3 + 반대1; 모든 family branch 완성 가능성 선검사; Fractured 보류 | guaranteed side 우선 conditional draws → 반대 side; 실제 게임 draw order/joint odds는 미확인, legacy-alchemy-order-v1 가정 표시 | C + S | LegacyFiveTest / LegacyFive.test.ts; Backend check PASS, 최종 FE/API/browser 미완료 |
| Omen_of_Dextral_Alchemy | Normal/Magic → Rare 4 mods; 최대 side3 + 반대1; 모든 family branch 완성 가능성 선검사; Fractured 보류 | guaranteed side 우선 conditional draws → 반대 side; 실제 게임 draw order/joint odds는 미확인, legacy-alchemy-order-v1 가정 표시 | C + S | LegacyFiveTest / LegacyFive.test.ts; Backend check PASS, 최종 FE/API/browser 미완료 |
| Omen_of_Sinistral_Coronation | Regal Magic→Rare / Exalted Rare; 해당 side slot·ilvl·family가 허용하는 후보 ≥1; 기존 Fractured 보존 | 일반 eligibility → affix side → ordinary published-weight draw; tiered는 현재 draw 전 거부 | C + S | AdditionTransitionsTest, WorkbenchExtensionTest; legacy Coronation은 LegacyFiveTest |
| Omen_of_Dextral_Coronation | Regal Magic→Rare / Exalted Rare; 해당 side slot·ilvl·family가 허용하는 후보 ≥1; 기존 Fractured 보존 | 일반 eligibility → affix side → ordinary published-weight draw; tiered는 현재 draw 전 거부 | C + S | AdditionTransitionsTest, WorkbenchExtensionTest; legacy Coronation은 LegacyFiveTest |
| Omen_of_Corruption | 대응 currency outcome/eligibility 시스템 미구현; 슬롯·고정mod edge 검증 안 함 | candidate/outcome 모델 없음; 소모 엔진 없음 | 활성화/API 사용 미지원; 소모 미구현 | 원래 보류65 유지; API enum 없는 ID는 거부 |
| Omen_of_Greater_Exaltation | Rare; free explicit slots ≥2; 모든 첫 추가 branch의 두 번째 후보 확보; 1-slot 게임 동작 미확인; Fractured 보존 | 1차 conditional weight → 업데이트 family/slots → 2차 conditional weight; Homogenising pair는 원래 tags를 고정 | C + S | GreaterExaltationTest, HomogenisingTest; Support multi-add는 미지원 |
| Omen_of_Sinistral_Exaltation | Regal Magic→Rare / Exalted Rare; 해당 side slot·ilvl·family가 허용하는 후보 ≥1; 기존 Fractured 보존 | 일반 eligibility → affix side → ordinary published-weight draw; tiered는 현재 draw 전 거부 | C + S | AdditionTransitionsTest, WorkbenchExtensionTest; legacy Coronation은 LegacyFiveTest |
| Omen_of_Dextral_Exaltation | Regal Magic→Rare / Exalted Rare; 해당 side slot·ilvl·family가 허용하는 후보 ≥1; 기존 Fractured 보존 | 일반 eligibility → affix side → ordinary published-weight draw; tiered는 현재 draw 전 거부 | C + S | AdditionTransitionsTest, WorkbenchExtensionTest; legacy Coronation은 LegacyFiveTest |
| Omen_of_Greater_Annulment | Magic/Rare; unlocked ≥2; 1개만 남은 상태는 게임 동작 미확인 보호 차단; Fractured/implicit 보존 | unlocked uniform 1차 → 해당 instance 제거 → remaining uniform 2차; distinct2 보장 | C + S | LegacyFiveTest.greaterAnnulmentRemovesTwoDistinctUnlockedInstancesAndRefusesOneBeforeDrawing |
| Omen_of_Sinistral_Annulment | Magic/Rare; 해당 side unlocked ≥1; Fractured/implicit 보존 | unlocked → side → uniform 제거1; published removal weights 없음 | C + S | WorkbenchExtensionTest; StockyWorkbenchTest |
| Omen_of_Dextral_Annulment | Magic/Rare; 해당 side unlocked ≥1; Fractured/implicit 보존 | unlocked → side → uniform 제거1; published removal weights 없음 | C + S | WorkbenchExtensionTest; StockyWorkbenchTest |
| Omen_of_Answered_Prayers | 게임 플레이 trigger; 제작 대상·slot·Fractured·modifier level N/A | 게임 플레이 서비스 없음; 효과 순서/소모 엔진 미구현 | 활성화/API 사용 미지원; 소모 미구현 | 원래 보류65 유지; API enum 없는 ID는 거부 |
| Omen_of_Secret_Compartments | 게임 플레이 trigger; 제작 대상·slot·Fractured·modifier level N/A | 게임 플레이 서비스 없음; 효과 순서/소모 엔진 미구현 | 활성화/API 사용 미지원; 소모 미구현 | 원래 보류65 유지; API enum 없는 ID는 거부 |
| Omen_of_the_Hunt | 게임 플레이 trigger; 제작 대상·slot·Fractured·modifier level N/A | 게임 플레이 서비스 없음; 효과 순서/소모 엔진 미구현 | 활성화/API 사용 미지원; 소모 미구현 | 원래 보류65 유지; API enum 없는 ID는 거부 |
| Omen_of_Reinforcements | 게임 플레이 trigger; 제작 대상·slot·Fractured·modifier level N/A | 게임 플레이 서비스 없음; 효과 순서/소모 엔진 미구현 | 활성화/API 사용 미지원; 소모 미구현 | 원래 보류65 유지; API enum 없는 ID는 거부 |
| Omen_of_Homogenising_Exaltation | Exalted Rare / Regal Magic; eligible slot/family/ilvl; Fractured 보존; tags는 implicit+explicit에서 얻음 | existing tag union → intersect candidates → original published weights; empty tags이면 unrestricted; pair의 신규 tags는 확장하지 않음 | C + S | HomogenisingTest, WorkbenchControllerTest.legacyHomogenisingWorksAcrossNineBasesAndPreservesUnmatchedOmen |
| Omen_of_Homogenising_Coronation | Exalted Rare / Regal Magic; eligible slot/family/ilvl; Fractured 보존; tags는 implicit+explicit에서 얻음 | existing tag union → intersect candidates → original published weights; empty tags이면 unrestricted; pair의 신규 tags는 확장하지 않음 | C + S | HomogenisingTest, WorkbenchControllerTest.legacyHomogenisingWorksAcrossNineBasesAndPreservesUnmatchedOmen |
| Omen_of_the_Blessed | variable implicit 필요; Solar/Stocky/Bow/Wand/Body/Sceptre 지원; Helmet/Ring는 no target, Belt는 variable implicit 미모델링으로 Divine 자체 거부 | implicit만 수치 재굴림; 모든 explicit 값/Fractured 그대로; 단일·coupled 숫자 분포는 별도 가정 | C + S | WorkbenchExtensionTest.divinePreservesIdsAndBlessedPreservesAllExplicitValues; helmet/ring no-target tests |
| Omen_of_Sanctification | 대응 currency outcome/eligibility 시스템 미구현; 슬롯·고정mod edge 검증 안 함 | candidate/outcome 모델 없음; 소모 엔진 없음 | 활성화/API 사용 미지원; 소모 미구현 | 원래 보류65 유지; API enum 없는 ID는 거부 |
| Omen_of_Catalysing_Exaltation | 전체 catalyst quality 소모 효과는 확인; quality→weight 함수·0-quality·multi-add 미확인 | 편향을 1/N으로 지우지 않음; 함수 근거와 catalyst 실제 apply가 의존성 | 활성화/API 사용 미지원; 소모 미구현 | 사용자 제외11 중 하나; typed-quality state interactions는 기존 안전 거부 |
| Omen_of_the_Sovereign | source card의 대상/방향만 확인; 공개3후보/일반+exclusive/포화/Fractured 상세는 미확인 | Desecration state/candidate/reveal 시스템 없음; 순서·소모 미구현 | 활성화/API 사용 미지원; 소모 미구현 | Necromancy2 명시 제외; 다른 Desecration Omen도 원래 보류 유지 |
| Omen_of_the_Liege | source card의 대상/방향만 확인; 공개3후보/일반+exclusive/포화/Fractured 상세는 미확인 | Desecration state/candidate/reveal 시스템 없음; 순서·소모 미구현 | 활성화/API 사용 미지원; 소모 미구현 | Necromancy2 명시 제외; 다른 Desecration Omen도 원래 보류 유지 |
| Omen_of_the_Blackblooded | source card의 대상/방향만 확인; 공개3후보/일반+exclusive/포화/Fractured 상세는 미확인 | Desecration state/candidate/reveal 시스템 없음; 순서·소모 미구현 | 활성화/API 사용 미지원; 소모 미구현 | Necromancy2 명시 제외; 다른 Desecration Omen도 원래 보류 유지 |
| Omen_of_Putrefaction | source card의 대상/방향만 확인; 공개3후보/일반+exclusive/포화/Fractured 상세는 미확인 | Desecration state/candidate/reveal 시스템 없음; 순서·소모 미구현 | 활성화/API 사용 미지원; 소모 미구현 | Necromancy2 명시 제외; 다른 Desecration Omen도 원래 보류 유지 |
| Omen_of_Light | source card의 대상/방향만 확인; 공개3후보/일반+exclusive/포화/Fractured 상세는 미확인 | Desecration state/candidate/reveal 시스템 없음; 순서·소모 미구현 | 활성화/API 사용 미지원; 소모 미구현 | Necromancy2 명시 제외; 다른 Desecration Omen도 원래 보류 유지 |
| Omen_of_Sinistral_Necromancy | source card의 대상/방향만 확인; 공개3후보/일반+exclusive/포화/Fractured 상세는 미확인 | Desecration state/candidate/reveal 시스템 없음; 순서·소모 미구현 | 활성화/API 사용 미지원; 소모 미구현 | Necromancy2 명시 제외; 다른 Desecration Omen도 원래 보류 유지 |
| Omen_of_Dextral_Necromancy | source card의 대상/방향만 확인; 공개3후보/일반+exclusive/포화/Fractured 상세는 미확인 | Desecration state/candidate/reveal 시스템 없음; 순서·소모 미구현 | 활성화/API 사용 미지원; 소모 미구현 | Necromancy2 명시 제외; 다른 Desecration Omen도 원래 보류 유지 |

## 확정된 코드/메타데이터 결함과 단순 제한 구분

1. **확정 metadata 불일치**: Greater Exaltation registry reason의 same-trigger combinations unsupported는 이미 구현된 Homogenising pair 예외를 누락합니다. Solar-only wording도 supportedBases9와 맞지 않음. 두 Crystallisation reason은 초기에 Solar/Stocky/Bow만 구체적으로 설명하지만 supportedBases9 전체를 기록합니다. rule implementation과 설명을 함께 정리할 수정 항목. gameplay bug가 확정됐다는 뜻은 아님.
2. **사용자 요구와 구현 gap 확정**: Erasure+Whittling 순서는 사용자 지정으로 해소됐지만 UI compatibleOmenPair와 Backend matches.size>1 guard 및 기존 테스트가 현재 거부를 유지합니다. 피드백 종합 시 구현/consumption/result validation/preview/tests를 같이 변경해야 함. 현재 feedback 대기라 수정하지 않았음.
3. **제품 제한 확인**: Greater/Perfect blanket guard, Greater Exaltation 1-slot refusal, Greater Annulment 1-removable refusal, Whittling fracture refusal는 게임 금지로 확정된 것이 아닙니다. 전부 별도 evidence/제품 정책 gap. 다른 trigger 유지와 enum mapping은 code/API에서 정상.
4. **server 차이**: 최신 legacy-five는 live에 없음. 이 차이를 runtime bug 또는 최신 구현 검증 성공으로 표시하지 않음.
5. **보류 노출**: latest Catalysing/Necromancy3는 inServiceScope로 picker 숨김, enum 없음/API ID 거부. 나머지 원래 보류11은 inventory/tooltips/favorite 등록 가능하나 toggleOmen에서 verified rule 없음 안내로 활성화 거부. source registry는 모두 보존. 이는 '읽기 inventory 보존+실행 차단' 정책이며 모두 숨김이 필요하다는 사용자 지시는 없음. live는 새 숨김정책 미반영.

## 근거가 필요한 항목

- Greater/Perfect의 minimum-added-level은 확인됐으나 Omen family 작동·필터 결합·게임 conflict 및 failure-consumption은 개별 조합 증거 필요. official description만으로 모든 same-family 조합 자동 허용하지 않음.
- Whittling Fractured: 일반 removal의 unlocked 필터와 lowest-level 대상 계산 관계; game highlight fix 이력만으로 해결 안 함. level없는 특수 modifier는 따로 보류.
- Greater Exaltation side-omens/1-slot; Greater Annulment side-omens/1-removable; opposing side pair는 문구 논리상 충돌 소지가 있으나 실제 게임 거부/소모 증거는 미확인.
- Homogenising: 'same type'→modifier-tag union은 기존 검토한 서비스 모델. 신규 tags 확장 안 함/implicit 포함/empty union fallback 및 weighted distribution은 게임 공동분포로 증명됐다고 표현하지 않음.
- legacy Alchemy side-first order는 명시 UNVERIFIED ledger; raw4-mod max3효과와 order/exact odds를 분리.
- Crystallisation 카드의 Perfect/Corrupted는 source 대응 supported replacementEssence 목록으로만 적용. unsupported bases/Essence를 일반적으로 발동한다고 주장하지 않음. 숫자 joint rolls는 기존 가정 유지.
- Catalysing 전체 quality소모는 확정, quality→weight 함수 미확정. catalyst 재노출과 이 Omen 재노출은 별개; 사용자 최신 Catalysing 보류 유지. Desecration 기반/공개후보/Fractured/포화는 사용자 보류로 추가 연구·개발 안 함.

## 사용자 판단/최종 수정 때 검증 계획

- 확정 사용자 요구: SinistralErasure+Whittling candidate side→minlevel→tie1/N, 로컬 preview. feedback 수집 끝난 뒤 반영. Dextral로 자동 확대/Fractured/특수level을 추정하지 않음.
- remaining same-trigger/tiered 각 조합을 지원/미지원 표로 검증한 뒤 scope별 허용; 모든 multi-activation 일괄허용 아님. 원래 보류 inventory의 표시 자체를 더 숨길지는 사용자 UI 피드백 사항.
- focused tests: prefix 안의 최저level이 전역 최저와 다른 case; ties; suffix 제외; no eligible; missing-level safe refusal; unrelated 유지; matching2성공 각각1소모; 거부 state/roll/omens 보존; current fracture refusal 유지.
- local preview API 호출횟수0/Backend 후보동일/keyboard & focus 및 response validator의2-omen 소모 검증. hover orange 후보가 확률을 사실처럼 표현하지 않도록 product label.
- tiered fixtures: ordinary35/50 cutoff·fallback group·family·slot·omen side와 minlevel 결합 matrix를 근거 확인 후 추가. all9 base coverage, full slot/one slot/Fractured/invalid level 포함.
- 최종 수정 이후 격리 Docker check/generateJooq/bootJar, FE mandatory suite, isolated API apply+browser 순차. 현재 사용자가 테스트 중인18081와 localStorage는 유지. 기존 checkpoint final FE/API/browser 미완료도 별도 보완.

## 코드·검증 파일 위치

project=../workbench-20261002. Backend: WorkbenchSimulator.java matching/availability/removalCandidates/apply, AdditionRules.java plan/pool/existingTags, WorkbenchOmen.java verifiedDoubleAddition, ItemStateValidator.java. FE: CraftingPage.tsx toggleOmen(429), workbenchApi.ts compatibleOmenPair(562)/baseWorkbenchAction/result checks, serviceScope.ts. ISSUES WB-038/039/040/041 참고. Backend357 unit+6integration check PASS 기존 증거 재사용; 이번에 새 테스트 안 돌림. 최종 catalyst rescope 후 FE full suite/isolatedAPI/browser 미완료를 그대로 유지.

## 변경/정리 상태

제품 코드·Git·원래 checkout·사용자 자원 변경 없음. audit32rows JSON은 표 coverage32/32 확인용; runtime source는13implemented임을 유지. 모든 audit Docker --rm 종료. remote push/merge/deploy 없음.
