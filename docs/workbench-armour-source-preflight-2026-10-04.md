# 방어구 확장 source preflight — 2026-10-04

Branch는 `workbench/top-bases-20261004`, 시작 HEAD는 `30141976788d52b4955b9fefa852c0fbee2267a2`이다. **이 commit은 source 조사·검증 자료이며 런타임 확장 완료가 아니다. 새로 등록한 base는 0개이고 기존 19개와 모든 ID/snapshot/film은 그대로다.** 구현·전체 Backend/Frontend 검사·API/browser 검증은 남아 있다.

## 확보한 자료

[원본 후보 bundle](evidence/armour-source-bundle-2026-10-04/bundle.json)은 기존 inventory의 대표24개와 동급 대안5개, 총29개 base의6언어 source174개를 보존한다. [교정된 roster](evidence/armour-source-bundle-2026-10-04/reviewed-roster.json)는 Sleek Jacket source6개를 추가했다. 총30개 base/180개 locale record에 URL, retrievedAt, HTML SHA256, normal popup, 요구치, Type·tags·quality가 있다.

[Pool summary](evidence/armour-source-bundle-2026-10-04/pool-summary.json)는24개 slot/archetype의 전체 `ModsView.normal` 행을 별도 파일로 보존한다. Name/level/generation/family/effect가 정확히 일치하는 행만 기존 definition과 연결했다. 이는 재사용 후보 식별이며 **ordered spawn eligibility·special essence class 적용·실제 확률을 증명하지 않는다.** 미확인 행을 STR definition으로 채우지 않았다. 아래 합계는 pool별 출현 횟수로, 고유 modifier 개수가 아니다.

| Slot | Armour | Evasion | ES | Armour/Evasion | Armour/ES | Evasion/ES |
|---|---:|---:|---:|---:|---:|---:|
| Body | 0 | 37 | 34 | 34 | 31 | 37 |
| Helmet | 0 | 32 | 28 | 29 | 25 | 30 |
| Gloves | 0 | 25 | 21 | 22 | 18 | 23 |
| Boots | 28 | 47 | 43 | 44 | 40 | 45 |

CDN hover는HTTP403을 반환했다. 공개 `https://poe2db.tw/us/hover?s=Data%5CMods%2FMovementVelocity1`은 detail을 반환한다. `LocalEvasionRating1`, `LocalEnergyShield1` 같은 추측 code는HTTP200이지만 빈 응답이었다. HTTP200만으로 검증하지 않는다. 올바른 code/detail을 source row와 대조해야 한다.

## Source에 따른 가역적 권고

- **Sleek Jacket을 DEX/ES Body 대표로 권고한다.** [Sleek Jacket](https://poe2db.tw/us/Sleek_Jacket)은285 Evasion/87 ES,level65/67 Dex·Int다. [Primal Markings](https://poe2db.tw/us/Primal_Markings)는268/82,level70/같은 능력치다. Primal 원본을 삭제하거나 registry를 바꾸지 않았다.
- **Wolfskin Mantle을 일반 STR/INT Body 대표로 유지한다.** inventory의749/220 [Ornate Ringmail](https://poe2db.tw/us/Ornate_Ringmail)은 `FourBodyStrIntUnique1`과 `Uniques/Loreweave` icon을 사용한다. [Outlier source](evidence/armour-source-bundle-2026-10-04/Ornate_Ringmail.outlier.json)를 보존했다. `Mods.enable_rarity`의normal만으로 일반 획득을 추론하지 않는다.
- **Adherent Cuffs를 STR/INT Gloves 대표로 권고한다.** Tethering도98 Armour/27 ES,55 Str·Int이고level65로 낮지만 `FourGlovesStrIntUnique1`이다. 일반 획득 source가 확인되기 전에는normal Endgame metadata인Adherent(level80)를 선택한다.
- **War Wraps를 DEX/INT Gloves 대표로 권고한다.** Secured와 모두94 Evasion/29 ES지만War는level65/44 Dex·Int,Secured는level80/55 Dex·Int다.
- **Faithful Leggings를 STR/INT Boots 대표로 권고한다.** 네 대안 모두134 Armour/37 ES다. Faithful은level65/47 Str·Int,Apostle은70/51,Warlock은75/56,Cryptic은80/59다.

검토한 위 대안 popup에는 별도 implicit 문구가 없었다. 최종 importer에서 implicit definition·roll·class 적용 검증을 별도로 완료해야 한다. 원본 `implicitSource` 필드는 metadata의Mods 관련 필드 모음이며 implicit 검증 완료를 뜻하지 않는다.

굵은 두 base만 이미 등록되어 있고 나머지22개는 다음 구현 대상이다.

| Slot | Armour | Evasion | ES | Armour/Evasion | Armour/ES | Evasion/ES |
|---|---|---|---|---|---|---|
| Body | **Soldier Cuirass** | Slipstrike Vest | Vile Robe | Death Mail | Wolfskin Mantle | Sleek Jacket(교정 권고) |
| Helmet | **Imperial Greathelm** | Freebooter Cap | Ancestral Tiara | Gladiatorial Helm | Cryptic Crown | Grinning Mask |
| Gloves | Massive Mitts | Polished Bracers | Sirenscale Gloves | Blacksteel Gauntlets | Adherent Cuffs | War Wraps |
| Boots | Tasalian Greaves | Drakeskin Boots | Sekhema Sandals | Blacksteel Sabatons | Faithful Leggings | Daggerfoot Shoes |

## 수집·검증 경로

- `scripts/armour-source.mjs`: 공통 source parser. base별 규칙을 수동 복제하지 않는다.
- `scripts/collect-armour-bundle.mjs <new-owned-directory>`: 원본 후보 수집. 기존 출력 디렉터리는 거절한다.
- `scripts/audit-armour-pools.mjs <directory>`: 전체 ordinary pool과 기존 definition 대조. 기존 pool 출력은 덮어쓰지 않는다.
- `scripts/review-armour-roster.mjs <directory>`: Sleek의 6locale source 추가, 예외·대안 교정, 선정 24개의 최고 일반 방어값 검증. 기존 review 출력은 덮어쓰지 않는다.
- `scripts/verify-armour-evidence.mjs <directory>`: source/locale/pool/교정 결론 integrity 검사. runtime import 승인으로 표시하지 않는다.

공통 parser 추출 후 기존 source record를 덮어쓰는 재수집은 하지 않았다. 원본 후보와 교정 결과를 별도로 보존한다.

## 검증과 실패 이력

모든 Node 실행은 project Docker의 일회성 container에서 수행했다. 새 출력 경로는 `../armour-source-qa-20261004`다. 무거운 Backend/Frontend 검사와 QA stack은 시작하지 않았고 QA slot도 점유하지 않았다. 운영 18080/18081, DB volume, 사용자 browser/storage, original repository, 다른 service는 변경하지 않았다. source container는 `--rm`으로 종료했다. push/merge/deploy/server update는 실행하지 않았다.

첫 pool audit은 Jewel의 object 형식 `base.raw.json`을 armour 배열로 읽어 TypeError가 발생했다. armour 증거로 사용할 수 없는 다른 형식을 명시적으로 제외했다. source/modifier assertion은 완화하지 않았다.

첫 integrity 검사는 Wolfskin의 최고치 비교에서 실패했다. Ornate의 Unique metadata를 실제 source로 확인했으며, 같은 검사에서 드러난 Primal/Sleek 차이도 추가 source로 검증했다. 원본 29개 record와 원본 `highestDefence:false`는 유지한다. 교정된 일반 대표 24개는 모든 defence 최고값과 다시 대조한다. 최초 빈 `verification.json`은 보존하고 후속 결과는 `verification-2.json`이다. 제품 런타임 실패가 아니다.

이번 [검증 결과](evidence/armour-source-bundle-2026-10-04/verification.json)는 5개 script syntax 검사와 1,643개 source integrity assertion 통과다. 180개 locale record와 24개 pool을 검사했으며 미확인 행은 pool 출현 횟수 기준 673개다. [실패 이력](evidence/armour-source-bundle-2026-10-04/failure-history.json)도 보존한다.

기존 Backend 396/Frontend 1749/Browser 125/API 87은 시작 HEAD의 이전 결과이며 이번 commit에서 재실행한 결과가 아니다. 이번 변경은 앱 코드/catalog를 수정하지 않은 source preflight다. lint/typecheck/test/build/API/browser screenshot은 런타임 구현 후 필수로 남아 있다.

## 다음 구현과 weapon/jewelry 계획

먼저 Massive Mitts와 나머지 5개 Gloves를 실제 구현 묶음으로 완성한다. Massive의 187 Armour를 사용하고 Runeforged의 78 Armour/127 Ward를 섞지 않는다. STR Gloves의 182개 normal 행은 기존 definition과 모두 일치한다. 다른 Gloves의 18–25개 신규 행을 code/detail, ordered spawn, range, 6언어 template까지 검증해야 한다. Stocky의 특수 결과를 다른 archetype에 자동 복제하지 않는다.

`loadTopBase`, WorkbenchService의 두 base 고정 분기, frontend union/selector/class/property, registry supportedBases를 검증된 manifest에 맞춰 함께 확장한다. historical ID/snapshot은 유지한다. weight가 미확인이면 검증된 eligible 후보 안에서만 명시적인 1/N을 사용하며 published DropChance를 게임 확률로 재표기하지 않는다. implicit stats/roll/source/6locale도 확인한다. Craft Support/Explorer는 Solar-only, deferred 50은 유지한다.

Gloves 다음 Helmet 5개, Body 5개, Boots 6개를 pool 단위로 완성한다. 낮은 item level, 활성 currency/omen class refusal, quality overflow/중앙 rounding, Shift/Alt/fractured/local orange preview, 6locale, old/new film/reload, selection/keyboard/focus를 검증한다. Backend/Frontend 전체 검사와 isolated API/browser screenshot QA를 순차 실행하고 review/local commit 후 깨끗한 SHA를 인계한다.

방어구 다음에는 기존 inventory로 Bow의 Warmonger(일반), Guardian(Chain), Gemini(추가 Arrow), Fanatic(Chaos), Obliterator(별도 range)를 source별로 검증한다. DPS 우열은 추측하지 않는다. 이어 Ring/Amulet distinct implicit을 묶는다. Ring 후보는 Kinetic/Vitalic/Mnemonic/Pearl/Amethyst/Prismatic/단일 저항/Two-Stone, Amulet 후보는 Stellar/단일 능력치/Bloodstone/Lunar/Azure/Crimson/Pearlescent다. variable implicit/source/6locale를 확인하고 capacity/socket-transfer/Runic Ward를 자동 확장하지 않는다. Wand/Sceptre의 skill family는 별도 검증 대상이다.
