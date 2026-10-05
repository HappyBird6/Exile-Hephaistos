# Offhand highest-tier core 5종

기준 `ba9df63a0fddc6c15967103ebb4e2636b7488cd7`, branch `workbench/top-bases-20261004`. 기존 Workbench 97종에 아래 5종을 추가하여 **102 bases**를 제공한다. 최고 requirement의 Str/StrDex/StrInt Shield, Dex Buckler, Int Focus 대표 범위이며 전체 low-tier catalog를 채우는 작업은 아니다.

| Base | 실제 class / ID | Armour / Evasion / ES | Block | Level / Str / Dex / Int | 기본 skill |
|---|---|---|---:|---|---|
| Tawhoan Tower Shield | Shields / 26 | 264 / 0 / 0 | 26% | 80 / 115 / 0 / 0 | Raise Shield |
| Golden Targe | Shields / 26 | 145 / 132 / 0 | 25% | 80 / 63 / 63 / 0 | Raise Shield |
| Blacksteel Crest Shield | Shields / 26 | 145 / 0 / 40 | 25% | 80 / 63 / 0 / 63 | Raise Shield |
| Desert Buckler | Bucklers / 94 | 0 / 192 / 0 | 20% | 80 / 0 / 115 / 0 | Parry |
| Tasalian Focus | Foci / 81 | 0 / 0 / 91 | 없음 | 80 / 0 / 0 / 115 | 없음 |

정확한 ID는 각각 `Metadata/Items/Armours/Shields/FourShieldStr10Endgame`, `FourShieldStrDex10Endgame`, `FourShieldStrInt7Endgame`, `FourShieldDex9Endgame`, `Metadata/Items/Armours/Focii/FourFocus10Endgame`이다. `Focii`는 source metadata의 실제 철자를 유지한다. Shields/Bucklers/Foci는 weapon class나 pool alias가 아니다. 기본 movement 값은 Tawhoan −0.03, Golden/Blacksteel −0.015이며 Buckler/Focus 일반 popup에는 해당 property가 없다.

[6locale source bundle](evidence/offhands-source-bundle-2026-10-05/)은 일반 popup과 modifier page 원문 HTML, URL·SHA256·조회 시각, canonical stats·locality·ordered spawn을 보존한다. source class inventory와 normal popup은 5종 모두 해당 archetype 최고 defence 및 level80에 일치한다. 일반 popup만 선택하므로 Runeforged 카드의 낮은 defence·Runic Ward 수치를 섞지 않는다. [기존 source inventory](evidence/belts-source-bundle-2026-10-05/next-equipment/inventory.json)도 같은 수치를 독립적으로 기록한다.

[공식 GGG Third Edict item-filter release 명세](https://www.pathofexile.com/forum/view-thread/3828542)는 New Items의 Shields/Bucklers/Foci 아래에 정확한 5종을 모두 명시한다. [공식 GGG 0.3.0b](https://www.pathofexile.com/forum/view-thread/3840893)는 Shield/Buckler 4종의 Block 및 Raise Shield/Parry 수정과 기존 아이템의 차이를 확인한다. Tasalian Focus의 [실제 제작 사례](https://www.reddit.com/r/PathOfExile2/comments/1ojmm3r/24_total_tasalian_focus_and_still_no_4/)는 current normal popup·rarity·class와 함께 ordinary craftable base 검토를 보강한다. 제작 사례의 exceptional sockets는 ordinary 최대치의 근거가 아니다. 정확한 드롭 장소·확률은 제공하지 않는다. [출처 해석·불확실성·실패 이력](../scripts/offhands-source-provenance.json)을 보존한다.

## Pool과 적용 경계

ordinary 전체 행은 Tawhoan123 / Golden134 / Blacksteel132 / Desert118 / Tasalian140이다. 원본 selection weight와 family·level·canonical stats를 유지한다. source ordered spawn의 첫 matching tag가 positive인 후보만 허용하며 `shield`, `buckler`, `focus` 및 defence archetype tags를 직접 사용한다. 이미 존재하는 정의는 정확한 stats/family/affix/level/text가 일치할 때만 ID와 display binding을 재사용한다. 새 definition31개는 6locale templates를 갖는다. class별 weight가 다른 동일 modifier ID도 각 catalog의 게시 weight를 그대로 보존한다.

고정 Essence action은 Shield/Buckler 각각21, Focus27이다. generic Enhancement 문구가 Armour/Evasion/ES 전체를 나열해도 실제 individual/hybrid detail과 ordered spawn을 기준으로 해당 archetype의 target만 선택한다. Infinite의 Strength/Dexterity/Intelligence도 각 tag에 따라 제한한다. 기존 활성 Hysteria는 Shields/Buckler의 ordinary Block prefix 또는 Focus의 ordinary Energy Shield Regeneration suffix로 연결하며 ordinary weight를0으로 바꾸지 않는다. Abyss는 zero-weight special prefix/suffix2종을 각 class의 replacement target으로 제공한다. Focus에만 기존 Perfect Alacrity special1종이 추가된다. 새 action이나 excluded/deferred mechanics는 추가하지 않는다.

일반·Greater/Perfect currencies와 기존 Crystallisation/Coronation/Alchemy/Exaltation/Annulment/Whittling Omen을 class별로 검증한다. variable canonical item implicit이 없으므로 Blessed Omen은 5종에서 거부된다. source의 Block property와 granted skill implicit line은 UI의 기본값으로 표시하며 가짜 canonical item stat/family로 만들지 않는다. `ItemState.implicits`는 빈 상태다. source 표시와 immutable film은 유지하고 실제 combat·local defence total·skill scaling·block 결과를 계산하지 않는다.

Quality 최대20과 `Sockets.socket_info=1:5:100`의 ordinary socket maximum1을 모두 보존한다. supplied socket state·Artificer·socketed Augment 적용은 기존 Stocky-only 모델을 확장하지 않아 거부한다. Catalyst typed quality는 비지원이다. game spawn weight나 numeric roll distribution을 확정하지 않으며 기존 UNVERIFIED source-unit numeric model을 사용한다. published selection weight가 있는 이번 ordinary pool에 임의1/N을 적용하지 않는다. Abyss의 source weight가 없는 fixed-choice 경로는 기존 valid-candidate 제한 후 disclosed1/N 모델을 유지한다.

## 보존과 QA

[Preservation evidence](evidence/offhands-source-bundle-2026-10-05/preservation.json)는 기존97종의 manifest·Essence targets·6locale terms/templates·registry 값과 deferred50의 완전 일치를 검증한다. Solar-only Support/Explorer, quality overflow/HALF_UP, Shift/Alt/orange preview, 기존 UI와 i18n 경계는 유지한다. 기존 base catalog나 migration은 수정하지 않는다.

QA 전용 경로는 `codex/offhands-qa-20261005`, Compose project `exile-offhands-20261005`, localhost20580/20581이다. Backend/Frontend는 격리 복사본에서 실행하고 heavy 검사는 exclusive sequential이다. live18080(master7d27d6c)/18081(UI18a2a3d), DB volumes, 사용자 browser storage, 기존 QA 출력, 원본 repo와 다른 서비스를 보존한다. 검증 결과·최종 source 동등성·local commit은 완료 시 아래에 기록한다. push/merge/deploy는 승인 범위 밖이다.

## 선택하지 않은 profile과 다음 roster

이번 highest-tier level80 각 defence archetype에는 source상 다른 동급 ordinary profile이 확인되지 않았다. meaningful Shield sidegrade **Venerable Defender**는 level62, Block28%, Armour107/Evasion97로 별도 profile이지만 현재 core5 및 highest-tier 범위에는 추가하지 않았다. Golden Flame은 level15 Chaos-resistance implicit의 낮은 tier이고 Glacial Fortress는 level70 Armour223 profile이므로 이 bundle에 추가하지 않는다. Buckler의 ordinary20% Block/Parry, Focus의 일반 ES profile에는 새로운 distinct implicit family가 확인되지 않았다. Runeforged/Runemastered/unique·lower-tier 반복은 제외한다.

다음 정확한 core roster는 기존 연구를 유지한다.

- Quivers: **Visceral, Volant, Penetrating, Primed, Serrated, Toxic, Blunt, Two-Point, Sacral, Fire, Broadhead Quiver**. distinct implicit마다 대표1종이며 Penetrating level55/100% Pierce를 구분한다.
- One Hand Maces: **Fortified Hammer, Strife Pick, Akoyan Club**. distinct 후보 Molten Hammer, Marauding Mace, Crown Mace.
- Two Hand Maces: **Ruination Maul, Fanatic Greathammer, Tawhoan Greatclub**. distinct 후보 Ironwood Greathammer, Massive Greathammer, Sacred Maul.
- Quarterstaves: **Aegis, Bolting, Dreaming Quarterstaff**. distinct 후보 Striking, Razor, Skullcrusher.
- Spears: **Grand, Flying, Akoyan Spear**. distinct 후보 Stalking, Spiked, Guardian.
- Staves: **Permafrost, Reflecting, Dark, Ravenous, Perching, Sanctified Staff**. 추가 innate-skill 후보 Icicle, Gelid, Voltaic, Pyrophyte, Chiming, Rending, Reaping, Roaring, Paralysing. Icicle의 현재 source card는 Firebolt이며 이름으로 skill을 추측하지 않는다.
- Talismans: **Maji, Fungal, Jade Talisman**. 추가 distinct 후보 Fang, Thunder, Alpha, Ashbark, Spiny, Condemned, Wingbeat. shapeshift family는 detail과 ordinary availability를 확인한 뒤 결정한다.

Crossbow의 남은 distinct Stout/Dedalian/Cumbrous도 [이전 기록](workbench-crossbows-bundle-2026-10-05.md)에 유지한다. Claws/Daggers/One and Two Hand Swords/Axes/Flails/Traps는 release-unverified다. 이 roster는 후속 source 검증 범위이며 이번 구현이나 whole-game 완료를 의미하지 않는다.

## 최종 검증·자체 리뷰·QA 종료

전용 Docker 복사본의 최종 Backend `spotlessApply check generateJooq bootJar`가 통과했다. unit465·Docker integration6, failure0·skip0이며 formatter·ArchUnit·임시 DB migration/codegen·bootJar를 포함한다. 최종 Frontend Node24 환경에서 `npm ci`, lint, typecheck, format:check, test, build가 모두 통과했다. 64 files·1,870 tests이며 Vite의 기존 큰 chunk 경고는 성공 build와 구분해 남긴다. dependency·lockfile·CI는 변경하지 않았다.

실제 QA API **594 checks**, browser **703 checks**, legacy filled film **25 checks**가 통과했다. API는 complete source definitions/weights·일반/상위 화폐·모든 지원 fixed/replacement Essence와 source ilvl·활성 Omen·wrong-class/deferred 거절·Blessed 거절·socket/typed-quality422·Solar-only Support/Explorer를 검증했다. browser는 actual class·Focus-only Perfect Alacrity 긍정과 Shield/Buckler 거절·Command/Body 거절의 film 원자성·6locale names/properties/requirements·Raise Shield/Parry의 implicit 영역·Shift/Alt·orange preview·old97 film 복원·Solar overflow40/cap20/HALF_UP을 검증했다. 브라우저 page errors는 0이다.

신규5×6locale×desktop1440/mobile390의 card60·full60, legacy Wand/Sceptre/Hallowed×6locale card18·full18, orange preview6·Solar overflow6으로 **168 original screenshots**를 보존했다. contact sheets28개와 필요한 원본 crop을 시각 검토했다. 원문 movement metadata의 영어 label과 raw decimal은 추측 번역/단위 보정하지 않았다. [Screenshot hash inventory](evidence/offhands-source-bundle-2026-10-05/screenshots.json)는 전용 QA의 `browser-attempt-1`을 가리킨다.

초기 Node22 engine 실패, strict lookup/draft 누락 실패, historical binding-count 실패의 각 log·원인·수정 이력을 보존했다. 역사1883과 신규 포함1914를 별도 검사하며 기존 count·전체 locale content 검사·strict 설정을 약화하지 않았다. 수집/locale 매칭 실패는 `scripts/offhands-source-provenance.json`에 남긴다. [Final QA input equivalence](evidence/offhands-source-bundle-2026-10-05/final-qa-inputs.json)는 최종 runtime/test source659개와 jar SHA를 확인하며 [최종 보존 검사](evidence/offhands-source-bundle-2026-10-05/preservation-completion.json)는 old97/deferred50을 확인한다.

자체 리뷰에서 신규5의 source class/metadata·pool membership/weights·canonical stats/locale binding·registry scope와 old-source 보존을 대조했다. [QA completion evidence](evidence/offhands-source-bundle-2026-10-05/qa-completion.json)와 API/browser/legacy JSON을 함께 보존한다. 요구 범위의 남은 blocker는 없다. combat·최종 defence/block 합산·예외 sockets 등은 위에 명시한 미지원 경계다.

전용 `exile-offhands-20261005`의 네 서비스만 `docker compose stop`으로 정상 종료했다. app exit143(SIGTERM), 나머지 exit0이며 OOM은 없다. down/volume 삭제는 하지 않았다. live18080/18081의 서비스 ID·시작 시각·mounts는 시작 전후 정확히 동일하고 기존 네 서비스만 실행 중이다. 자신의 `heavy-qa-owner.json`은 active:false로 해제했다. 검증 파일·stopped QA는 다음 fresh chat을 위해 보존하며 다른 QA 결과를 덮어쓰지 않았다. 결과는 작업 branch의 local commit으로 완료하고 push/merge/deploy는 수행하지 않는다.

Git staging의 최초 newline 정규화도 수정했다. 이번 raw HTML에만 기존 방식의 .gitattributes -text를 적용하고 staged bytes를 다시 읽어 [Git index checksum proof](evidence/offhands-source-bundle-2026-10-05/git-index-proofs.json)로 captured SHA와 일치함을 확인했다. Git의 전체 whitespace 검사는 immutable raw HTML의 원문 trailing whitespace를 보고하며, 해당 원문은 보존한다. 코드·JSON·문서 등 non-raw staged diff 검사는 통과했다. runtime source는 변경하지 않아 최종 aggregate/API/browser 결과와 소스 동등성이 유지된다.
