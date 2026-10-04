# Highest-tier Boots 6종 — 2026-10-04

기준 HEAD는 `e8034894cce832d0a7397d0265cc1b1d68ee2628`, branch는 `workbench/top-bases-20261004`다. 기존 35개 base의 ID·snapshot·film 구조를 보존하고 Boots 6종을 추가하여 Workbench는 **41개 base**가 된다. Body·Helmet·Gloves·Boots의 일반 defence archetype 6종이 모두 갖춰진다. Support/Explorer는 Solar-only이며 deferred 50을 확대하지 않는다.

## Source와 전체 pool

[기존 preflight](workbench-armour-source-preflight-2026-10-04.md)의 [reviewed roster](evidence/armour-source-bundle-2026-10-04/reviewed-roster.json)와 36개 locale snapshot을 재사용했다. 새 base source를 덮어쓰거나 낮은 tier 전체를 등록하지 않았다. Faithful은 동급 Apostle/Warlock/Cryptic보다 낮은 요구치의 일반 대표다.

| Base / archetype | Type 마지막 경로 | Armour / Evasion / ES | Character requirement | Ordinary pool |
|---|---|---|---|---:|
| Tasalian Greaves / STR | `FourBootsStr6Endgame` | 280 / 0 / 0 | 80 / 108 Str | 129 |
| Drakeskin Boots / DEX | `FourBootsDex6Endgame` | 0 / 255 / 0 | 80 / 108 Dex | 129 |
| Sekhema Sandals / INT | `FourBootsInt6Endgame` | 0 / 0 / 83 | 80 / 108 Int | 125 |
| Blacksteel Sabatons / STR-DEX | `FourBootsStrDex4Endgame` | 154 / 140 / 0 | 80 / 59 Str, 59 Dex | 139 |
| Faithful Leggings / STR-INT | `FourBootsStrInt1Endgame` | 134 / 0 / 37 | 65 / 47 Str, 47 Int | 135 |
| Daggerfoot Shoes / DEX-INT | `FourBootsDexInt4Endgame` | 0 / 140 / 43 | 80 / 59 Dex, 59 Int | 135 |

Type prefix는 `Metadata/Items/Armours/Boots/`다. Class Boots·일반 metadata·source tags·quality maximum 20·요구치·빈 implicit을 확인했다. 번역 popup에서 요구치 뒤에 붙는 영어 base-name footer를 implicit으로 해석하지 않는다. Socket metadata가 있어도 augmentSockets 상태를 추측하지 않으며 null을 유지한다. 총 ordinary **792행**을 보존한다.

기존 importer에 `--boots` 경로를 추가하여 검증된 definition과 stable ID를 재사용했다. 신규 **58개 definition**은 stable public source-ID detail의 generation·level·family·stat·range·ordered spawn을 대조하며 6locale template과 valueStats를 함께 생성한다. 공통 MovementVelocity definition 6개는 Tasalian에서 검증한 뒤 다른 Boots에서 재사용한다. MovementVelocity는 ilvl **1/16/33/46/65/82**에 **10/15/20/25/30/35%**의 고정값이다. Character requirement와 modifier ilvl을 혼동하지 않는다.

같은 family·level·range·tags를 가진 다른 ailment 효과가 locale table에서 겹칠 수 있어 Boots row identity에는 source keyword 순서를 추가했다. Chill 효과를 Ignite 등 다른 효과의 번역으로 잘못 연결하지 않는다. 기존 Body/Helmet row identity는 유지한다. 처음 발견한 importer 실패 로그는 새 QA root에 보존한다. 기존 ItemState 명세대로 높은 tier의 기존 roll은 낮은 ilvl에서도 임의 삭제하지 않으며, source ilvl 제한은 새 affix 생성 후보에 적용한다. 각 이동속도 경계의 직전·정확한 level에서 전체 후보 집합을 비교한다.

## 적용 범위

각 base의 기본 Essence action 27개와 source가 확인된 **Hysteria(30% Movement Speed, ilvl65)**, **Abyss(prefix/suffix Mark)**를 연결했다. Boots source에는 Perfect Essence 결과가 게시되어 있지 않으므로 다른 class의 Perfect Essence를 복제하지 않는다. Horror/socket 효과·Artificer·Alloy·Catalyst·Runic Ward 및 capacity 변경은 확대하지 않는다.

Ordinary 선택은 기존 승인된 simulator의 eligible 후보 동일 확률 1/N이다. 실제 game weight나 numeric-roll 확률의 검증으로 주장하지 않는다. 이동속도 family 중복, source level과 범위 초과, Crystallisation 제거 방향과 상충 Omen의 atomic refusal을 검사한다. Blessed는 빈 implicit에 재굴림 대상이 없어 새 Boots supported-base 목록에 추가하지 않으며 API는 미소모·무변경 거부를 확인한다. 기존 quality overflow·중앙 rounding·Shift/Alt·local orange preview·새 UI·완료된 번역을 보존한다.

Loader/bootstrap/WorkbenchService/registry와 frontend union/selector/draft/class/property/Essence response validation을 함께 연결했다. 영화 저장 format과 이전 low-level base ID는 변경하지 않는다.

## 격리 QA와 완료 기록

QA root는 `codex/boots-qa-20261004`, project는 `exile-boots-20261004`, localhost API/UI는 `19680/19681`이다. Heavy checks는 순차 실행한다. Live UI18081(`18a2a3d`)/API18080(`7d27d6c`), 사용자 browser/storage, 기존 DB volume, 원본 repository, 다른 서비스와 이전 QA output은 보존한다. 재실행은 새 attempt 파일로 이력을 유지한다. standard service tier를 사용하며 오전 cutoff는 없다. Local commit만 수행한다.

Cached importer replay는 **25개 산출물·792 ordinary행·58 exact detail** 일치다. 전체 Backend/Frontend·API·Browser의 최종 수치와 pixel 검토·QA 종료는 아래 최종 기록으로 보완한다.

## 최종 검증·정리

[검증 요약](evidence/boots-runtime-bundle-2026-10-04/validation.json)에 실행 결과와 JAR SHA-256을 보존했다. Backend `check generateJooq bootJar`는 unit/ArchUnit 418개와 Docker integration 6개가 모두 통과했다. Frontend `npm ci`, lint, typecheck, format:check, test 1,794개, build가 통과했다. 기존 Vite chunk-size warning은 유지된다.

API 946개(일반 413 + Boots 계약 533), browser 532개가 통과했다. 이동 속도 10/15/20/25/30/35%의 정확한 range·생성 레벨 경계, 화폐·Essence·Omen 성공과 충돌/잘못된 입력의 atomic refusal, 기존 35개 metadata/snapshot 및 21개 armour film 복원을 확인했다. 6개 언어·1440/390 viewport의 72개 화면, 6개 orange preview, 기존 21개 film으로 총 99개 screenshot을 만들었다. 전체 contact sheet와 72개 item-card crop, 대표 원본 크기의 화면을 실제 pixel로 검토했다. Browser 오류는 0개다.

Importer replay는 25개 생성 문서·792개 ordinary 행·58개 exact detail을 재검증했다. [실패와 수정 이력](evidence/boots-runtime-bundle-2026-10-04/failure-history.json)을 보존했고 skip이나 계약 완화로 실패를 숨기지 않았다. 별도 reviewer 없이 자체 리뷰했다.

전용 `exile-boots-20261004` 서비스만 정상 `compose down`으로 종료하고 heavy QA 슬롯을 해제했다. [종료 증거](evidence/boots-runtime-bundle-2026-10-04/qa-release.json)에 기존 18080/18081 서비스와 네 개 live container가 유지됨을 기록했다. 기존 volume·사용자 browser/storage는 변경하지 않았고 remote push/merge/deploy는 하지 않았다. 전체 로그·99개 screenshot·23개 contact sheet는 `../boots-qa-20261004`에 별도로 보존했다.

## 다음 concrete 묶음

[기존 inventory 문서](workbench-top-bases-2026-10-04.md)와 [원본 cards](evidence/top-base-inventory-2026-10-04/inventory.json)를 기준으로 다음 순서를 유지한다. 아래는 다음 source 검증 roster이며 이번 runtime 등록에 포함하지 않는다.

- **Bow 5**: Warmonger Bow(일반 물리), Guardian Bow(Chain), Gemini Bow(추가 Arrow), Fanatic Bow(Chaos), Obliterator Bow(높은 물리 range/짧은 사거리). DPS 우열을 추측하지 않는다.
- **Ring 8개 대표 묶음**: Kinetic, Vitalic, Mnemonic, Pearl, Amethyst, Prismatic, Ruby(single resistance 대표), Two-Stone(Fire/Cold 대표). Sapphire/Topaz 및 Two-Stone의 Fire/Lightning·Cold/Lightning은 별도 metadata/implicit variant로 보존하고 서로 합치지 않는다. Two-Stone의 실제 Type은 다음 source preflight에서 먼저 확인한다.
- **Amulet 7개 대표 묶음**: Stellar, Amber(single attribute 대표), Bloodstone, Lunar, Azure, Crimson, Pearlescent. Jade/Lapis 대안의 독립 implicit은 보존한다. 기존 Solar와 low-level film은 유지한다.
- **Wand skill family**: Attuned/Mana Drain(기존), Siphoning/Power Siphon, Volatile/Volatile Dead, Galvanic/Galvanic Field, Acrid/Decompose, Offering/Exsanguinate, Primordial/Wither, Dueling/Spellslinger, Twisted/Coiling Bolts. Frigid/Torture/Critical의 Chaos Bolt는 metadata 비교 후 대표를 선택한다. Runic Fork는 제외한다.
- **Sceptre skill family**: Hallowed/Skeletal Warrior, Stoic/Discipline, Omen/Malice, Shrine/Purity·Impurity variant, Clasped/Heart of Ice, Wrath/Fulmination. Spirit100만으로 각 skill family를 대체하지 않는다.

Variable implicit의 range·source·6locale 및 모든 eligible pool을 다음 묶음에서 검증한다. capacity/socket-transfer/Runic Ward나 deferred50을 자동 확대하지 않는다. 다음 묶음은 새로운 QA copy에서 시작한다.
