# Highest-tier Bow 5종 — 2026-10-04

기준 HEAD `3d48354db103bb25cdd6f31a09a7a4a83bdbd75e`, branch `workbench/top-bases-20261004`에서 승인된 Bow 5종을 추가한다. 기존 41개 base·snapshot·modifier ID·film format을 보존하며 Workbench는 **46개 base**가 된다. Body·Helmet·Gloves·Boots의 선택된 highest-tier defence archetype 24종은 그대로다. Support/Explorer는 Solar-only, registry active170/deferred50은 그대로이며 remote push/merge/deploy는 수행하지 않는다.

## Source와 표시 범위

[이전 Bow inventory](evidence/top-base-inventory-2026-10-04/Bows.json)의 endgame 일반 대표와 [새 6locale snapshot](evidence/bows-source-bundle-2026-10-04/import-summary.json)을 대조한다. Base source 30개, class pool source 6개, implicit stat source 4개를 새 경로에 보존한다. 낮은 tier 전체나 Runeforged 변형을 자동 등록하지 않는다.

| Base | Type suffix | Physical Damage | Attacks/sec | Required character level / Dex | Distinct implicit |
|---|---|---|---|---|---|
| [Warmonger Bow](https://poe2db.tw/us/Warmonger_Bow) | FourBow8Endgame | 56–84 | 1.2 | 77 / 163 | 없음, 일반 물리 대표 |
| [Guardian Bow](https://poe2db.tw/us/Guardian_Bow) | FourBow3Endgame | 53–88 | 1.15 | 77 / 163 | Local Chain chance 25–35% |
| [Gemini Bow](https://poe2db.tw/us/Gemini_Bow) | FourBow6Endgame | 39–72 | 1.15 | 78 / 163 | Global +50% Surpassing additional Arrow chance |
| [Fanatic Bow](https://poe2db.tw/us/Fanatic_Bow) | FourBow7Endgame | 47–79 | 1.2 | 79 / 163 | Local hidden Chaos minimum28 / maximum64 |
| [Obliterator Bow](https://poe2db.tw/us/Obliterator_Bow) | FourBow9Endgame | 62–115 | 1.1 | 78 / 163 | Global Projectile Range −50% |

Type prefix는 `Metadata/Items/Weapons/TwoHandWeapons/Bows/`이며 전부 Critical Hit Chance5%, maximum quality20이다. Obliterator의 추가 `karui_basetype` tag도 보존한다. 높은 물리 range·짧은 range, Chain, 추가 Arrow, Chaos를 서로 대체하거나 DPS 순위로 해석하지 않는다.

Published item popup의 base property와 character requirement를 사용한다. 상세 metadata의 공통 `Weapon.minimum_damage=5`, `maximum_damage=10`, `weapon_speed=833`, `AttributeRequirements.strength_requirement=100`을 실제 5종 수치로 복사하지 않는다. UI는 각 locale의 세 property에 “Base … (not computed)” 표시를 붙인다. Source implicit의 Local/Global과 hidden stat ID·값을 보존한다. Local affix, Fanatic hidden Chaos, global gained damage·Arrow·range를 합쳐 최종 weapon totals나 전투 simulation을 만들지 않는다. Fanatic hidden implicit 원문은 별도 implicit line에 그대로 남는다.

Guardian variable implicit은 source range25–35에서 canonical35로 시작한다. 나머지 fixed implicit은 정확한 고정값이며 Warmonger는 implicit이 없다. 기존 stat 정수 단위, shared-ratio 복수 stat roll, source scalar/range 표시, original rolls, quality overflow와 중앙 rounding 정책을 유지한다. Socket metadata로 새 socket state를 추측하지 않는다.

## 전체 pool과 실행 경로

현재 Bows ordinary140행의 Name·Level·Generation·Family·DropChance·effect·tags와 기존 Crude Bow raw snapshot을 전체 대조하여 동일함을 확인한다. 기존 public-code detail의 ordered spawn에서 base tags와 default의 첫 일치가 모든 5종에 양수임을 확인한다. 전체 ordinary definition ID·weight·tier·range·source와 6locale template은 재사용한다. 게시 weight를 기존 policy대로 유지하며 새 armour의 1/N policy로 바꾸지 않는다. 이는 공식 게임 weight/수치 확률을 새로 검증했다는 주장이 아니다.

각 base는 ordinary140 + Perfect Essence6 + Abyss2, Guardian/Gemini/Fanatic/Obliterator는 각각 implicit1을 갖는다. Basic Essence21 action, Perfect Abrasion/Flames/Ice/Electricity/Battle/Haste6 action, Abyss1 action을 exact Code·source level·family·effect로 연결한다. Abyss table의 abbreviated “Mark”는 기존 동일 Code의 검증된 full detail “Bears the Mark” 정의를 유지한다. 원문 단축 표기를 새로운 효과로 추측하지 않는다.

일반 currency·상위 currency·source-valid Omen·Divine/Fracturing/Alchemy는 기존 엔진을 사용한다. Guardian은 variable implicit이 있어 Blessed Divine 양성 경로를 제공한다. 고정/빈 implicit의 다른 4종에는 Blessed를 새로 등록하지 않는다. Wrong class Essence·Hysteria·Horror·Artificer·Alloy·Catalyst와 deferred mechanics는 그대로 제한한다. Greater/Perfect 최소 modifier level과 기존 highest-family exception은 source level 기준이며 character requirement와 다르다. 기존 높은 tier roll을 낮은 ilvl item에서 삭제하지 않고 새 생성에만 level 제한을 적용한다.

## 격리 검증과 실패 이력

새 QA root `codex/bows-qa-20261004`, project `exile-bows-20261004`, localhost API/UI19780/19781을 사용한다. Heavy checks는 독점 슬롯에서 순차 수행한다. Live UI18081(`18a2a3d`)/API18080(`7d27d6c`), 사용자 browser/storage, DB volumes, 원본 repository와 이전 QA output을 보존한다. 자동 승인 거부가 있으면 우회하지 않으며 재실행은 새 attempt 로그에 남긴다.

최종 검증 수치와 종료 증거는 [validation](evidence/bows-runtime-bundle-2026-10-04/validation.json), [failure history](evidence/bows-runtime-bundle-2026-10-04/failure-history.json), [QA release](evidence/bows-runtime-bundle-2026-10-04/qa-release.json)에 기록한다. 별도 reviewer 없이 자체 리뷰한다.

## 남은 roster와 전체 coverage 한계

현재 runtime46개: Body7, Helmet7, Gloves7, Boots6, Bow6, Basic/Time-Lost Jewel8, Amulet1(Solar), Ring1(Iron), Belt1(Rawhide), Wand1(Attuned), Sceptre1(Rattling). 선택된 armour24종과 **이번 Bow5종**을 완료한 것이며 모든 장비 category의 최고 tier를 완료했다는 의미가 아니다. Bow inventory의 Ironwood/Heartwood Shortbow(fast attack), Cavalry, Greatbow(STR/DEX·critical chance) 및 다른 metadata variant는 보존하며 새 runtime base로 alias하지 않았다.

다음 권장 bundle은 **Ring8**이다. Variable implicit·source·6locale·전체 eligible pool을 먼저 검증한다. 이후 Amulet7, Wand/Sceptre skill families, Belt를 이어간다.

- **Ring8**: Kinetic, Vitalic, Mnemonic, Pearl, Amethyst, Prismatic, Ruby(single resistance 대표), Two-Stone(Fire/Cold 대표). Sapphire/Topaz와 Two-Stone Fire/Lightning·Cold/Lightning은 독립 metadata/implicit variant로 남긴다. Two-Stone 실제 Type을 preflight에서 먼저 확인한다.
- **Amulet7**: Stellar, Amber(single attribute 대표), Bloodstone, Lunar, Azure, Crimson, Pearlescent. Jade/Lapis의 별도 attribute implicit과 기존 Solar film을 보존한다.
- **Wand**: Attuned/Mana Drain(기존), Siphoning/Power Siphon, Volatile/Volatile Dead, Galvanic/Galvanic Field, Acrid/Decompose, Offering/Exsanguinate, Primordial/Wither, Dueling/Spellslinger, Twisted/Coiling Bolts. Frigid/Torture/Critical의 Chaos Bolt 대표는 metadata 대조 후 선택한다. Runic Fork는 확대하지 않는다.
- **Sceptre**: Hallowed/Skeletal Warrior, Stoic/Discipline, Omen/Malice, Shrine/Purity·Impurity 별도 variant, Clasped/Heart of Ice, Wrath/Fulmination. Spirit100만으로 서로 다른 skill family를 합치지 않는다.
- **Belt**: 기존 Rawhide의 Life Flask 대표와 별도로 Linen(Mana Flask), Wide(Flask Charges gained), Long(Charm Duration), Plate(Armour), Ornate(Charm Charges used), Mail(Flask Charges used), Double(Charm Charges gained), Heavy(Stun Threshold), Utility(instant Flask Recovery), Fine(Flask charges/sec), Golden Obi(Rarity), Invoking(Cast Speed), Sinew(Strength), Forking(Lightning attack damage) source 후보를 보존한다. [Belt inventory](evidence/top-base-inventory-2026-10-04/Belts.json)에 있는 Stalking(socketed items as Boots), Runemastered Heavy의 여러 Runic Ward/Flask variant도 기록에서 삭제하지 않는다. Charm Slots1–3와 variant의 “Has1”은 아직 미모델링이므로 실제 Type·암묵 효과·중복 line 의미를 검증하기 전 실행 범위를 확대하지 않는다. Stalking/socket-transfer·Runic Ward는 현재 deferred 경계를 유지한다.

위 roster에 없는 weapon/offhand 및 다른 equipment categories는 source pool·highest-tier 대표·runtime을 아직 확장하지 않았다. 해당 category를 완료/제외로 조용히 처리하지 않는다. 이번 묶음은 Bow 요청5종만 추가했으며 전체 장비 coverage는 후속 bundle의 별도 source 검증이 필요하다.

## 최종 결과

Backend aggregate `check generateJooq bootJar` 429개(unit/ArchUnit423 + Docker integration6), Frontend 1,794개와 lint/typecheck/format/build, importer replay23문서·ordinary700행, API811개(475+336), browser538개가 통과했다. 실제 양성 currency95경로·Omen76경로와 base당 Essence28 action, class/level/family/low-ilvl 음성 경로를 검증했다. 6locale × 2width × 5base 화면60개, orange5개, old-film28개와 card crop60개를 contact sheet21개에서 육안 확인했다. 추가 Blessed capture1개도 보존했다. Browser error0, test failure/error/skip0이다. 기존 Vite chunk size 경고는 유지된다.

검증 복사본과 현재 backend/src·frontend/src는 byte 단위로 동일하다. 실패한 attempt 로그와 수정 근거를 보존했으며 assertion 완화·검증 skip은 없다. 전용 QA는 `compose down`으로 정상 종료했고 volume 삭제 없이 독점 슬롯을 반환했다. Live4개 컨테이너의 ID·포트가 그대로이며 UI18081/API18080은 변경하지 않았다. 로컬 구현·자체 리뷰·검증 완료이며 최종 통합/배포는 수행하지 않았다.
