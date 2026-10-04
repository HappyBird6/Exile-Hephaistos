# 최상위 베이스 확장 — 첫 묶음

기준 SHA는 `2aac292d7a393a4c1f9b97086edf62e068c2e3d8`이며 전용 branch는 `workbench/top-bases-20261004`다. 기존 17개 베이스를 유지하면서 Soldier Cuirass와 Imperial Greathelm을 추가한다. Workbench만 확장하며 CraftSupport/Explorer는 Solar 전용으로 유지한다. 원격 push, master 반영, 기존 서비스 배포는 이 작업 범위에 없다.

## 선택 기준과 현재 후보

공식 [Early Access patch notes 목록](https://www.pathofexile.com/forum/view-forum/2212)은 현재 0.5.5d를 제시한다. 세부 base 수치·태그·카드는 PoE2DB의 2026-10-04 공개 표를 사용한다. 공식 목록은 개별 base의 드롭·weight를 증명하지 않는다. 일반 표에 나오는 것만으로 현재 획득 가능성이 확정되는 것도 아니다. 아래는 **구현 대상 후보 목록**이며 실제 support 선언은 별도 source 검증과 성공 경로 확인 후에만 추가한다.

요구 캐릭터 레벨, DropLevel, modifier requiredItemLevel, 사용자가 입력하는 Item level은 서로 다른 값이다. 최대 요구 레벨만으로 고르지 않는다. Runeforged/Runemastered, Grasping/socket-transfer, affix capacity 변경, Runic Ward 및 placeholder는 이번 일반 장비 묶음에서 제외하며 현재 제작 가능성과 추가 state 규칙을 별도로 검증해야 한다. unique·corrupted 전용·미출시 class는 일반 베이스로 등록하지 않는다. 인벤토리 수집 파일에는 비교를 위한 낮은 tier와 특수 행이 남지만 런타임에 등록하지 않는다.

| Slot | Armour | Evasion | ES | Armour/Evasion | Armour/ES | Evasion/ES |
|---|---|---|---|---|---|---|
| Body | **Soldier Cuirass** | Slipstrike Vest | Vile Robe | Death Mail | Wolfskin Mantle | Primal Markings |
| Helmet | **Imperial Greathelm** | Freebooter Cap | Ancestral Tiara | Gladiatorial Helm | Cryptic Crown | Grinning Mask |
| Gloves | Massive Mitts | Polished Bracers | Sirenscale Gloves | Blacksteel Gauntlets | Adherent Cuffs / Tethering Bands 비교 | War Wraps / Secured Wraps 비교 |
| Boots | Tasalian Greaves | Drakeskin Boots | Sekhema Sandals | Blacksteel Sabatons | Faithful / Apostle / Warlock / Cryptic Leggings 비교 | Daggerfoot Shoes |

굵은 두 베이스만 첫 묶음에서 구현한다. 각 slot/archetype 표의 원문 카드·URL·retrievedAt·HTML SHA256은 [수집 인벤토리](evidence/top-base-inventory-2026-10-04/inventory.json)에 있다. STR/INT Boots의 동일 134 Armour/37 ES, INT/Dex Gloves의 동일 94 Evasion/29 ES 및 일부 STR/INT Gloves는 요구치가 다르므로 높은 레벨 이름을 자동 우선하지 않는다. 개별 base metadata 검토 후 낮은 요구치의 동급 베이스를 추천한다.

Body implicit sidegrade는 별도 유지 후보다: STR Ornate Plate(재생), Utzaal Cuirass(Stun Threshold), Warlord Cuirass(Armour의 Elemental 적용); DEX Swiftstalker Coat(감속), Wyrmscale Coat(ailment threshold), Corsair Coat(이동); INT Flowing Raiment(mana regeneration), Sacramental Robe(recharge), Feathered Raiment(Mana before Life); STR/DEX Dastard Armour(Life), Shrouded Mail(세 저항 variant), Thane Mail(critical bonus 감소); STR/INT Conjurer Mantle(Spirit), Death Mantle(maximum resistances), Seastorm Mantle(recoup); DEX/INT Rambler Jacket(Chaos Resistance), Falconer's Jacket(이동), Austere Garb(ailment duration). 기본 방어 수치가 높은 베이스와 의미 있는 implicit 계열을 함께 제공하는 방향이며 이번에 implicit 모델을 추측해서 추가하지 않는다.

기존 지원 무기 class의 다음 후보는 [Bows](https://poe2db.tw/us/Bows)의 Warmonger Bow(일반 물리), Guardian Bow(Chain), Gemini Bow(추가 Arrow), Fanatic Bow(Chaos), Obliterator Bow(높은 물리/짧은 사거리), Ironwood/Heartwood Shortbow(속도 계열 비교)다. 실제 DPS·사거리 계산은 하지 않는다. [Wands](https://poe2db.tw/us/Wands)는 선형 tier가 아니라 innate skill별 Attuned/Mana Drain, Siphoning/Power Siphon, Volatile/Volatile Dead, Galvanic/Galvanic Field, Acrid/Decompose, Offering/Exsanguinate, Primordial/Wither, Dueling/Spellslinger, Twisted/Coiling Bolts를 유지 후보로 삼는다. Frigid/Torture/Critical의 Chaos Bolt 계열은 metadata 차이 검토 후 대표를 고른다. Runic Fork는 이번 대상에서 제외한다. [Sceptres](https://poe2db.tw/us/Sceptres)는 Hallowed/Skeletal Warrior와 Stoic/Discipline, Omen/Malice, Shrine의 Purity/Impurity variant, Clasped/Heart of Ice, Wrath/Fulmination을 별도 후보로 삼는다. Spirit는 목록에서 모두 100이며 최대 레벨만으로 모든 skill 계열을 대체하지 않는다.

[Rings](https://poe2db.tw/us/Rings)의 실용 후보는 Kinetic(물리), Vitalic(Life%), Mnemonic(Mana%), Pearl(cast speed), Emerald(accuracy), Amethyst(Chaos resistance), Prismatic(all elemental), Ruby/Sapphire/Topaz(single resistance), Two-Stone의 세 저항 조합, Biostatic(max resistance), Oneiric(Chaos damage), Unset(skill slot), Gold(rarity)이다. Iron Ring의 기존 기록은 유지한다. Breach/Refined Breach, Abyssal Signet, capacity 변경 및 socket-transfer base는 전용 규칙 검토가 필요하다.

[Amulets](https://poe2db.tw/us/Amulets)는 Solar(Spirit), Stellar(all attributes), Amber/Jade/Lapis(single attribute), Bloodstone(Life), Lunar(ES), Azure(mana regeneration), Crimson(life regeneration), Pearlescent(all elemental resistance), Gold(rarity)를 유지 후보로 삼는다. [Belts](https://poe2db.tw/us/Belts)는 Rawhide/Linen(flask recovery), Wide(flask charges), Long(charm duration), Plate(Armour), Ornate(charm charges used), Mail(flask charges used), Double(charm charges gained), Heavy(stun threshold), Utility(instant recovery), Fine(passive charges), Golden Obi(rarity)를 distinct family 후보로 삼는다. variable implicit와 charm slot을 고정값으로 추측하지 않는다. 신규 belt는 기존 Rawhide의 미해석 implicit 처리만 복제해 구현 완료로 표시하지 않는다.

다음 우선순위는 ① Massive Mitts 등 기존 STR Gloves 후속, ② 다른 defence archetype의 Helmet/Gloves와 Body, ③ Bow distinct implicit, ④ Ring/Amulet distinct implicit, ⑤ Sceptre/Wand skill families, ⑥ Boots와 아직 지원하지 않는 weapon/offhand class의 별도 source audit이다. 마지막 그룹의 최상위 후보는 이번 인벤토리가 완전하게 검증하지 않았으므로 미확인으로 남긴다.

## 첫 묶음의 출처와 동작 경계

Soldier Cuirass는 `FourBodyStr3Endgame`, Armour 570, 요구 캐릭터 레벨 65/STR 121, tags `str_armour, body_armour, armour`, implicit 없음이다. Imperial Greathelm은 `FourHelmetStr7Endgame`, Armour 374, 요구 캐릭터 레벨 80/STR 115, tags `str_armour, karui_basetype, helmet, armour`, implicit 없음이다. 두 source 모두 maximum quality 20이다. UI의 Item level 입력과 요구 캐릭터 레벨을 별도로 표시한다. Armour는 원문 기본 수치이며 affix·quality를 적용한 최종 방어 계산은 제공하지 않는다.

일반 후보는 현재 `new ModsView(...).normal`의 모든 Name/Level/Generation/Family/DropChance/effect/spawn tags를 기존 상세 검증된 풀과 대조했다. Body 144행과 Helmet 137행이 정확하게 일치한다. 각 상세 정의의 **순서 있는 첫 일치 Spawn Tag**에 새 base tags와 `default`를 대입했으며 모든 후보가 양수 eligibility를 유지한다. 기존 base-specific tag가 사라져도 전체 후보가 같음을 검증했다. [Body 확인](evidence/top-base-bundle-2026-10-04/rusted-cuirass.pool-proof.json), [Helmet 확인](evidence/top-base-bundle-2026-10-04/rusted-greathelm.pool-proof.json), [base/locale 증거](evidence/top-base-bundle-2026-10-04/bundle.json).

`collect-top-base-bundle.mjs`는 이 두 STR variant의 base/6locale source와 eligibility를 수집한다. `verify-top-base-pools.mjs`는 현재 일반 표가 달라지면 assert로 거절한다. `wire-top-base-bundle.mjs`는 검토된 증거로 backend/frontend manifest·6locale 이름·base 지원 기록을 생성한다. 서로 다른 archetype이나 implicit을 이 경로로 자동 허용하지 않는다. 새 snapshot은 새 source digest와 기존 검증된 옵션 풀 snapshot을 함께 식별한다. 기존 modifier ID는 동일한 source definition을 가리키므로 유지하고, old base ID/snapshot/film은 새 base로 alias하지 않는다. 새 catalog에는 legacy compatibleSnapshotIds를 넣지 않는다.

첫 묶음 이후 catalog는 19개: Amulet 1, Ring 1, Belt 1, Wand 1, Sceptre 1, Bow 1, Gloves 1, Body 2, Helmet 2, Basic Jewel 4, Time-Lost Jewel 4. 일반/특수 정의는 Soldier 144+3, Imperial 137+3이며 기존 definition과 6locale templates를 재사용한다. 수치 roll의 기존 UNVERIFIED 가정과 published table model policy는 그대로다. 신규 일반 weight를 1/N으로 대체하지 않았다. 지원 재료가 동일해지는 것은 검증된 같은 eligible pool과 같은 class 제한에 한정된다. Soldier의 Perfect Body/Ruin/Seeking과 Imperial의 기존 Helmet Perfect 경로만 허용하며 Bow 전용 Perfect Ice, Artificer/socket/alloy·catalyst·deferred mechanics를 추가하지 않는다. 기존 50 deferred 항목의 status와 scope를 바꾸지 않는다.

## 검증 기록

QA 증거는 `../top-bases-qa-20261004`에 보존한다. 무거운 검사는 자체 복사본에서 순차 실행했다. 기존 18080 API와 18081 UI, 사용자 browser storage, DB volume은 변경하지 않았다. 자체 Docker project는 정상 종료했다. 원격 push/merge/deploy는 수행하지 않았다.

- Backend: `spotlessApply check generateJooq bootJar` 성공. unit/ArchUnit 390개와 Docker integration 6개, 합계 396개, 실패/skip 0. 임시 DB에 migration 3개를 검증·적용하고 jOOQ를 생성했다. 운영 migration 파일은 변경하지 않았다.
- Frontend: `npm ci`, lint, typecheck, format:check, `test -- --run`, build 성공. 58개 파일/1,749개 테스트 통과. 표시 guard는 고유 definition 2,314개, compound binding 228개, 재료 220개의 6locale coverage를 확인했다. Vite의 큰 chunk 경고는 남아 있다.
- Browser: assertion 125개 통과. 두 새 베이스 실제 selector/Transmutation/Augmentation/Regal, Shift 반복 거부, Escape, Alt 범위, 과거 단계 분기와 미래 film 보존, 정확한 reload, old Body/Helmet film 복원 후 원래 ID로 제작을 확인했다. 새 context만 사용했다. 2베이스×6locale×1440/390px 스크린샷 24장을 직접 확인했으며 요구치·Item level 구분과 줄바꿈을 확인했다. document overflow/page error는 0.
- API: 19개 initial 조회 성공. Soldier 일반 화폐 6개, Alchemy, Fracturing/Chaos 잠금 보존, Perfect Body와 Sinistral Crystallisation assertion을 통과했다. 이어지는 충돌 거부 비교는 QA fixture가 생략한 `fractured:false`를 API가 명시하여 실패했다. 독립 진단에서 두 새 베이스 모두 `applied:false`와 원래 modifier ID/값 보존을 확인했다. 제품 결함으로 확인된 것은 아니지만 전체 API probe는 **미완료/실패**로 유지한다. 이후 Soldier Ruin/Seeking, Imperial Perfect, class refusal, quality endpoint, runtime registry 비교는 이 probe에서 실행되지 않았다. 두 베이스의 주요 적용/거부 경로는 Backend 테스트로 검증했다.
- 기존 registry entry 220개의 field/status/scope는 새 supportedBases 두 개를 제외하면 정확히 동일하다. 새 지원은 Soldier 38개/Imperial 35개에만 추가됐다. 기존 gameTerms 값은 모두 동일하며 Backend/Frontend manifest는 의미상 동일하다. 검증 복사본과 최종 변경 소스를 대조했다.

실패 이력: 첫 Backend 검사는 test fixture의 catalog 배치 오류로 4개 실패해 실제 `CraftingConfiguration` 조합으로 수정했다(수정 1). 두 번째는 새 snapshot ID가 기존 120자 제한을 초과해 4개 실패했고 pool digest로 짧게 식별하도록 수정했다(수정 2). 세 번째 전체 검사는 성공했다. 로그와 첫 두 failure XML을 보존했다. Source 수집의 ru/de 준비 오류는 실제 six locales로 다시 수집했고 인벤토리 파서는 normal whiteitem 카드 기준으로 준비했다.

[Worker 운영 명세](solo-workflow/MULTI_SESSION_WORKFLOW.md)의 “범위 내 결함 수정·재검증·필요한 재리뷰는 작업 전체 최대 2회” 한도를 사용했으므로 API fixture를 추가 수정·재실행하지 않았다. 다음 결정은 fixture에 명시적 `fractured:false`를 넣고 재검증할 **추가 수정/재검증 범위 승인**이다. 세션을 바꿔 이 한도를 초기화하지 않는다. Runtime registry의 독립 read-only 비교 시도는 escalated 사용자와 worktree 소유자가 달라 Git dubious ownership 검사에서 중단됐다. 대신 기존 entry 보존은 sandbox source 비교로 확인했다. 재개 시 전역 Git 설정을 바꾸지 않고 read-only 기준 registry fixture나 해당 경로만 지정한 `git -c safe.directory=...`를 사용할 수 있다.

증거 색인은 [validation JSON](evidence/workbench-top-bases-validation-2026-10-04.json)이다. 전체 API probe가 통과하기 전에는 merge-ready로 표시하지 않는다. 후속 category 확장과 이 미완료 검증을 구분한다.
