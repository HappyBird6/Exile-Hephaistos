# Highest-tier Body / remaining Helmet bundle — 2026-10-04

기준 HEAD는 `a03d3c41ff012bd335f2268fe407a198ed58b6d6`, 작업 branch는 `workbench/top-bases-20261004`다. 기존 28개 base의 identity와 snapshot/film을 유지하고 아래 7개를 추가하여 Workbench는 **35개 base**가 된다. Body와 Helmet의 일반 defence archetype 6종이 모두 갖춰진다. Support/Explorer는 Solar-only이며 deferred 50과 기존 excluded mechanics는 그대로다.

## 확정 roster와 source Type

[기존 handoff](workbench-evasion-armour-bundle-2026-10-04.md)와 [source preflight](workbench-armour-source-preflight-2026-10-04.md)의 reviewed roster를 먼저 확인했다. Base source 42개를 다시 수집하지 않고 기존 snapshot을 사용했다. Sleek Jacket은 Primal Markings보다 높은 일반 Evasion/ES, Wolfskin Mantle은 Unique metadata인 Ornate Ringmail을 제외한 일반 Armour/ES 대표다.

| Slot / archetype | Base | Type의 마지막 경로 | Armour / Evasion / ES | Character requirement | Ordinary pool |
|---|---|---|---|---|---:|
| Body / DEX | [Slipstrike Vest](https://poe2db.tw/us/Slipstrike_Vest) | `FourBodyDex6Endgame` | 0 / 519 / 0 | 70 / 121 Dex | 144 |
| Body / STR-DEX | [Death Mail](https://poe2db.tw/us/Death_Mail) | `FourBodyStrDex6Endgame` | 313 / 285 / 0 | 75 / 67 Str, 67 Dex | 155 |
| Body / DEX-INT | [Sleek Jacket](https://poe2db.tw/us/Sleek_Jacket) | `FourBodyDexInt2Endgame` | 0 / 285 / 87 | 65 / 67 Dex, 67 Int | 152 |
| Body / INT | [Vile Robe](https://poe2db.tw/us/Vile_Robe) | `FourBodyInt3Endgame` | 0 / 0 / 171 | 65 / 121 Int | 141 |
| Body / STR-INT | [Wolfskin Mantle](https://poe2db.tw/us/Wolfskin_Mantle) | `FourBodyStrInt2Endgame` | 313 / 0 / 87 | 65 / 67 Str, 67 Int | 152 |
| Helmet / INT | [Ancestral Tiara](https://poe2db.tw/us/Ancestral_Tiara) | `FourHelmetInt8Endgame` | 0 / 0 / 109 | 80 / 115 Int | 125 |
| Helmet / STR-INT | [Cryptic Crown](https://poe2db.tw/us/Cryptic_Crown) | `FourHelmetStrInt6Endgame` | 206 / 0 / 57 | 80 / 63 Str, 63 Int | 135 |

Body의 full Type prefix는 `Metadata/Items/Armours/BodyArmours/`, Helmet은 `Metadata/Items/Armours/Helmets/`다. Character requirement와 item level은 별개다. 일곱 source popup에는 implicit modifier가 없으며 빈 implicit을 보존한다. Base Movement Speed는 Body source의 `-0.03` 또는 `-0.04` 값을 그대로 source property로 표시한다. 퍼센트로 임의 변환하거나 실제 이동속도를 계산하지 않는다. Quality maximum 20, socket state null을 유지한다.

## Import와 실행 경로

[Body proof](evidence/body-helmets-runtime-bundle-2026-10-04/body/import-summary.json), [Helmet proof](evidence/body-helmets-runtime-bundle-2026-10-04/helmets/import-summary.json)는 총 1,004개의 ordinary row를 포함한다. Highest-tier base 선정은 modifier tier pruning을 의미하지 않는다. 새로운 ordinary definition 67개는 stable public source ID detail의 generation/family/level/stat/range/ordered spawn을 사용한다. 나머지는 검증된 기존 definition의 stable ID와 family/range/tags를 재사용한다. 어려운 hybrid defence/life/percent 행도 모두 포함하며 guessed Code와 임의 stat 조합은 사용하지 않는다.

새 67개 definition의 6locale template과 valueStats를 importer에서 함께 작성했다. Compound binding 52개와 single-stat binding 15개를 추가하여 전체 342/186개에 대해 min/middle/max, 원본 stat 순서와 source unit 보존을 검사한다. 기존 template과 term의 내용 보존은 baseline 비교로 별도 확인한다.

Ordinary 선택은 승인된 simulator policy인 eligible 후보의 동일 확률 1/N이다. 게임의 실제 spawn weight나 numeric-roll 확률로 주장하지 않는다. Body에는 각 21개 basic Essence와 source class가 확인된 Perfect Body/Ruin/Seeking, Helmet에는 각 27개 basic Essence와 Perfect Thawing을 연결했다. 최종 자체 리뷰에서 Body Hysteria의 Physical Thorns(level 63)와 Helmet Hysteria의 +1 Minion Skill(level 5)을 정확한 ordinary target과 연결했다. 두 class의 Abyss prefix/suffix는 기존 검증된 Mark definition을 재사용하며 명시된 simulator 동일 확률 1/2를 적용한다. 특수 zero-ordinary-weight 결과는 Body 5개, Helmet 3개다. Crystallisation의 제거 방향과 상충 Omen의 atomic refusal을 검사한다. Desecration/reveal follow-up은 기존 미지원 범위를 유지한다. Horror/Artificer/Runic Alloy/Catalyst 등의 class 지원을 임의 확대하지 않는다.

Loader/bootstrap/WorkbenchService/registry/API와 frontend base union/selector/draft/source properties/Essence response validation을 함께 연결했다. 기존 base ID, snapshot compatibility, film format, Shift/Alt, local orange preview, quality overflow와 중앙 rounding policy를 유지한다. Soldier의 원래 selector와 property 경로는 중복시키지 않는다.

## 격리 검증과 실패 이력

QA root는 `codex/body-helmets-qa-20261004`, project는 `exile-body-helmets-20261004`, localhost API/UI는 `19580/19581`이다. Project Docker에서 자기 isolated copy/profile/services만 사용하며 heavy checks는 순차 실행한다. Live `18080/18081`, 사용자 browser storage, 기존 DB volume, original repository와 다른 service는 보존한다.

Backend 첫 check의 새 양성 fixture 세 건은 무작위 Alchemy 결과를 모든 Perfect Essence의 양성 전제로 사용하여 실패했다: Death Mail/Vile의 Seeking, Cryptic의 Thawing. Source-verified level-1 Life와 Fire Resistance companion을 명시한 fixture로 수정했으며 applied/validator assertion을 그대로 유지했다. 원래 `backend-check-1.log`와 `backend-failure-1.xml`을 보존한다. QA 재실행 script 작성에서 PowerShell ConstrainedLanguage의 .NET writer가 허용되지 않아 launch가 한 번 실패했고, 같은 허용 경로에 native Set-Content로 script를 작성했다. `backend-launch-2.log`를 보존하며 그 launch에서는 test가 실행되지 않았다.

Frontend 첫 `npm ci`는 Node 22 image가 저장소의 Node 24/npm 11 engine 조건을 만족하지 않아 실패했다. `frontend-check-1.log`를 보존하고 Node 24 image로 재실행하며 engine 조건을 완화하지 않는다. Importer의 추가 roster assertion에서 일부 preferred record에만 존재하는 `topProperties`를 모든 record의 전제로 삼아 한 차례 실패했다. 모든 record가 가진 verified `locales.us.card`의 정확한 defence 값과 비교하도록 수정했다. `body-import-3.log`를 보존하며 source assertion을 삭제하거나 기대값을 완화하지 않았다.

Frontend의 기존 equipment binding 총수 1,652 assertion 한 건은 새 67개를 포함한 정확한 1,719로 증가시켰다. `frontend-check-2.log`의 1 failed/1,781 passed 이력을 보존하고 모든 unit을 다시 검사했다. 첫 browser run의 568 check/105 screenshot은 통과했지만 시각 검토에서 중국어 두 locale의 movement label이 번역되어 영어-only extractor가 Body 10개 property line을 누락한 것을 발견했다. 원문 snapshot의 정확한 `基础移动速度`/`基礎移動速度`와 값을 추출하도록 고쳤으며 모든 Body locale에 정확히 한 line이 있어야 한다는 importer/unit/browser assertion을 추가했다. 첫 browser artifact는 별도 `browser-attempt-1`에 그대로 보존한다. 이 보완은 표시용 metadata이며 engine/modpool/registry/DB/codegen 입력은 바뀌지 않았다. 최종 JAR는 `spotlessCheck bootJar`로 다시 package하고 Frontend/API/browser를 재검증한다.

최종 검증 수치와 QA 종료 증거는 이 문서의 후속 검증 기록 및 evidence summary에 기록한다. Local commit만 수행하며 remote push/merge/deploy는 수행하지 않는다.

## 다음 작업 handoff

남은 armour는 **Boots 6종**이다: Tasalian Greaves, Drakeskin Boots, Sekhema Sandals, Blacksteel Sabatons, Faithful Leggings, Daggerfoot Shoes. 기존 preflight의 일반 defence archetype별 source를 재사용한다. Faithful은 같은 defence의 Apostle/Warlock/Cryptic보다 낮은 character requirement인 일반 대표다.

Armour 이후 weapon/jewelry는 기존 researched roster를 따른다. Bow는 Warmonger(일반), Guardian(Chain), Gemini(추가 Arrow), Fanatic(Chaos), Obliterator(높은 range)를 DPS 우열로 합치지 않는다. Ring은 Kinetic/Vitalic/Mnemonic/Pearl/Amethyst/Prismatic/단일 저항/Two-Stone, Amulet은 Stellar/단일 능력치/Bloodstone/Lunar/Azure/Crimson/Pearlescent의 distinct implicit을 보존한다. Variable implicit/source/6locale 검증이 필요하며 capacity/socket transfer/Runic Ward를 자동 확대하지 않는다. Wand/Sceptre는 skill family별 별도 묶음이다.

## 최종 결과와 완료 경계

- Backend: `check generateJooq bootJar` 성공, unit/ArchUnit 412 + Docker integration 6 = **418**. 최종 source property metadata 반영 후 `spotlessCheck bootJar` 재성공.
- Frontend: Node 24/npm 11 `npm ci`, lint, typecheck, format, unit **1,782**, build 성공. 기존 bundle-size warning은 남아 있다.
- API: **469** assertion, initial bases **35**. 신규 7종 positive crafting, cross-class 거부, low ilvl, implicit 보존, applicable Essence/Omen/Abyss/Hysteria와 기존 registry/snapshot 보존 확인.
- Browser: **598** check, JavaScript 오류 **0**, 스크린샷 **105** = 6locale×7base×2viewport 84 + orange preview 7 + old film 14. Shift/Alt, 새/기존 film 및 reload, 원문 movement property, fallback/overflow를 확인했다.
- Cached importer replay: **28** 산출물 일치, ordinary rows **1,004**, 신규 exact detail **67**. 표시 coverage는 definition 2,511, compound 342/single 186, 6locale active material name 220/220이다.

[검증 요약](evidence/body-helmets-runtime-bundle-2026-10-04/validation.json), [API](evidence/body-helmets-runtime-bundle-2026-10-04/api-results.json), [Browser](evidence/body-helmets-runtime-bundle-2026-10-04/browser-results.json), [QA 종료](evidence/body-helmets-runtime-bundle-2026-10-04/qa-release.json)를 보존했다. 상세 로그와 전체 105 PNG는 격리 QA root에 유지한다. 첫 browser 재실행은 불필요한 QA root bind mount가 image Playwright 경로를 가려 시작 전에 실패했다. 해당 mount만 제거하고 동일 assertion으로 재실행했고 [실패 기록](evidence/body-helmets-runtime-bundle-2026-10-04/browser-launch-failure.log)을 남겼다.

대표 화면: [한국어 Slipstrike](evidence/body-helmets-runtime-bundle-2026-10-04/slipstrike-ko-390.png), [영어 Death Mail](evidence/body-helmets-runtime-bundle-2026-10-04/death-mail-en-1440.png), [일본어 Sleek](evidence/body-helmets-runtime-bundle-2026-10-04/sleek-ja-390.png), [중국어 간체 Vile](evidence/body-helmets-runtime-bundle-2026-10-04/vile-zh-CN-390.png), [스페인어 Wolfskin](evidence/body-helmets-runtime-bundle-2026-10-04/wolfskin-es-1440.png), [중국어 번체 Ancestral](evidence/body-helmets-runtime-bundle-2026-10-04/ancestral-zh-TW-390.png), [Cryptic](evidence/body-helmets-runtime-bundle-2026-10-04/cryptic-ja-1440.png), [orange preview](evidence/body-helmets-runtime-bundle-2026-10-04/sleek-orange-preview.png), [기존 War Wraps film](evidence/body-helmets-runtime-bundle-2026-10-04/old-film-war-wraps.png).

자체 리뷰 완료; 별도 reviewer는 없다. QA 서비스 4개와 자기 network를 정상 종료하고 heavy QA owner를 inactive로 해제했다. Docker에는 기존 18081/UI·18080/API 등 4개 서비스만 남아 있다. 기존 volume, 사용자 browser storage, 원본 repo, unrelated service는 변경하지 않았다. Remote push/merge/deploy는 수행하지 않았다. 다음 fresh chat은 Boots 6종부터 계속하며 오전 cutoff는 없다. Fast 설정을 변경하지 않았으며 다음 작업은 standard tier로 진행한다.
