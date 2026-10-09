# Highest-tier Evasion Gloves / Helmets 묶음 — 2026-10-04

기준 HEAD는 `1c8fa7d3294f5ffdc86e2325dd263588aa38ece6`, 작업 branch는 `workbench/top-bases-20261004`다. 기존 22개 base의 ID·snapshot·catalog·film을 유지하고 아래 6개를 추가하여 Workbench 선택지가 28개가 된다. 낮은 tier의 전체 base 목록을 추가하지 않는다. Support/Explorer는 Solar-only다.

| Slot / archetype | 추가 base | 전체 ordinary pool |
|---|---|---:|
| Gloves / DEX | [Polished Bracers](https://poe2db.tw/us/Polished_Bracers) | 174 |
| Gloves / STR-DEX | [Blacksteel Gauntlets](https://poe2db.tw/us/Blacksteel_Gauntlets) | 184 |
| Gloves / DEX-INT | [War Wraps](https://poe2db.tw/us/War_Wraps) | 180 |
| Helmet / DEX | [Freebooter Cap](https://poe2db.tw/us/Freebooter_Cap) | 137 |
| Helmet / STR-DEX | [Gladiatorial Helm](https://poe2db.tw/us/Gladiatorial_Helm) | 147 |
| Helmet / DEX-INT | [Grinning Mask](https://poe2db.tw/us/Grinning_Mask) | 135 |

Gloves의 6개 일반 defence archetype이 모두 완료됐다. War Wraps는 Secured Wraps와 같은 최고 Defence를 갖지만 요구치가 낮은, 이미 검토한 sidegrade다. character level 65 / 44 Dex·Int를 item level 82로 보정하지 않는다. 대표 선정·implicit 부재의 근거는 [armour preflight](workbench-armour-source-preflight-2026-10-04.md)를 따른다. 기존 Stocky/Massive/Runeforged 등의 identity를 교체하지 않는다.

## Source 및 modifier 매핑

기존 [armour snapshot](evidence/armour-source-bundle-2026-10-04/bundle.json)을 사용했다. 필요한 modifier class table의 6locale 및 정확한 row detail만 추가로 확인했다. 넓은 base inventory 수집을 반복하지 않았다. [Gloves evidence](evidence/evasion-gloves-runtime-bundle-2026-10-04/import-summary.json)와 [Helmet evidence](evidence/evasion-helmets-runtime-bundle-2026-10-04/import-summary.json)에 source URL·timestamp·hash·family·generation·level·numeric ranges·tags·spawn order를 보존한다.

Code를 모르는 것은 stat 의미를 모르는 것과 다르다. Deflection의 guessed Code는 빈 응답이었지만, 원래 CDN hover의 stable source ID가 가리키는 공개 PoE2DB-domain detail에서 `base_deflection_rating_%_of_evasion_rating`, Global locality, 정확한 범위와 spawn tags를 확인했다. `EXACT_PUBLIC_SOURCE_ID_DETAIL`로 기록하며 Code를 만들어내지 않는다. HTTP 200만으로 검증 성공을 판단하지 않는다.

새 Gloves definition 70개와 Helmet definition 21개를 추가했다. Helmet은 name·family·generation·level·effect·range·tags가 일치하는 Gloves의 evasion/Deflection 정의를 재사용한다. 각 base의 모든 ordinary 행을 유지하며 어려운 modifier나 낮은 modifier tier를 제거하지 않는다. Base 최고 tier 선택과 modifier pool pruning은 별개다.

일반 pool은 사용자 승인 simulator 모델인 적격 후보 1/N이다. 실제 game spawn weight와 numeric roll 확률을 검증했다고 주장하지 않는다. family·prefix/suffix·item level 제한 후 실제 candidate ID 목록과 N을 ledger에 기록한다. 기존 catalog의 weight와 snapshot은 바꾸지 않는다.

6locale base names·requirements·affix templates를 제공한다. source popup에 없는 implicit·socket을 만들지 않으며, maximum quality 20과 socket null을 유지한다. Armour/Evasion/ES는 계산된 최종 Defence가 아닌 source base 값으로 표시한다. central rounding·quality overflow·local orange preview·Shift/Alt·fractured·film 분기/reload를 유지한다.

Gloves에는 28개 basic Essence action과 기존 class-specific Perfect Grounding/Opulence, Hysteria, Abyss 경로를 연결한다. Helmet에는 27개 basic Essence action과 source가 명시하는 **Perfect Essence of Thawing**의 cold recoup suffix를 연결한다. class별 fixed target set을 사용하고 registry 지원 목록도 같은 경로를 따른다. Body-only Perfect Essence·Horror·Artificer/socket·Runic Alloy·Catalyst를 새 base에 자동 상속하지 않는다. deferred 50은 확장하지 않는다.

## 격리 검증

QA root: `codex/evasion-armour-qa-20261004`. QA project: `exile-evasion-gloves-20261004`, localhost API/UI: `19480/19481`. 새 복사본·별도 로그·tmpfs DB를 사용하며 heavy 검증은 순차 실행한다. live `18080/18081`, 사용자 browser/localStorage, 기존 DB volume, 원본 repository와 다른 서비스를 수정하지 않는다.

최종 [검증 summary](evidence/evasion-armour-runtime-bundle-2026-10-04/validation.json)는 아래와 같다.

| 검증 | 결과 | 근거 |
|---|---|---|
| Backend `spotlessApply check generateJooq bootJar` | unit/ArchUnit 405 + Docker integration 6 = 411 통과 | QA `backend-check-3.log`, XML |
| 최종 registry packaging | `spotlessCheck bootJar` 성공; 실제 runtime JAR의 registry 일치 | QA `backend-final-package.log`, API check 2 |
| Frontend npm ci / lint / typecheck / format:check / unit / build | 59 files, 1,768 tests 통과 | QA `frontend-check-1.log` |
| 6locale completeness | 2,444 definitions, compound 290, single-stat 171; min/middle/max 유지 | [coverage](evidence/evasion-armour-runtime-bundle-2026-10-04/display-coverage.json), Frontend unit |
| API | 397 assertions, 28 initial bases | [results](evidence/evasion-armour-runtime-bundle-2026-10-04/api-results.json) |
| Browser | 449 checks, error 0 | [results](evidence/evasion-armour-runtime-bundle-2026-10-04/browser-results.json) |
| 화면 | 6 bases × 6 locales × 2 widths = 72; orange 6; old film 8 = 86 | QA `browser-attempt-1`; repository의 대표 9장 시각 확인 |
| cached importer replay | 24 JSON 일치, source ID/family/level/effect/tags 추가 assertion 통과 | [results](evidence/evasion-armour-runtime-bundle-2026-10-04/importer-replay-results.json) |
| Compose | 격리 profile `config --quiet` 통과 | QA `compose.yaml` |

실패 이력을 숨기지 않았다. guessed Deflection Code의 빈 detail과 Helmet의 단수 `helmet` tag를 확인해 정확한 source mapping으로 수정했다. Backend check 1은 신규 StateBucket/ItemState fixture 타입 오류였고, check 2는 cold recoup를 Ice로 잘못 연결하여 405개 중 신규 Helmet 3개가 실패했다. source Name에서 Thawing action을 도출하도록 고쳤으며 positive fixture와 모든 기존 assertion을 유지했다. `backend-check-1/2.log`, `backend-failure-2.xml`을 보존했다.

API check 1은 실제 crafting 경로를 통과한 뒤 registry 일치 검사에서 실패했다. 최종 shell-script의 Windows 줄바꿈 때문에 JAR 복사 목적지 이름에 carriage return이 붙었다. 잘못 복사된 파일과 이전 정상 이름 JAR를 보존하고, 이미 성공한 최종 build JAR를 정확한 QA 경로로 복사했다. 격리 app만 재시작한 API check 2가 397개 전부 통과했다. 검증 summary parser의 첫 시도는 ANSI 색상 escape 때문에 실제 Frontend 성공 줄을 인식하지 못했고, escape만 제거한 뒤 정확한 1,768개 성공을 확인했다. assertion count나 성공 조건은 완화하지 않았다. Build의 기존 large-chunk 안내는 실패가 아니며 남겨두었다.

대표 화면: [Polished / 한국어](evidence/evasion-armour-runtime-bundle-2026-10-04/polished-ko-390.png), [Blacksteel / Español](evidence/evasion-armour-runtime-bundle-2026-10-04/blacksteel-gloves-es-1440.png), [War Wraps / 日本語](evidence/evasion-armour-runtime-bundle-2026-10-04/war-wraps-ja-390.png), [Freebooter / 简体中文](evidence/evasion-armour-runtime-bundle-2026-10-04/freebooter-zh-CN-390.png), [Gladiatorial / English](evidence/evasion-armour-runtime-bundle-2026-10-04/gladiatorial-en-1440.png), [Grinning / 繁體中文](evidence/evasion-armour-runtime-bundle-2026-10-04/grinning-zh-TW-390.png). 나머지 전체 86개 화면·로그·XML·JAR·실패 이력은 QA root에 보존한다.

구현·전체 검증·자체 리뷰를 완료했다. [QA 종료 기록](evidence/evasion-armour-runtime-bundle-2026-10-04/qa-release.json)에 따라 격리 services만 정상 종료하고 독점 slot을 해제했다. live 18080/18081의 기존 4개 container는 유지했다. 사용자 browser/localStorage·DB volume·원본 작업 tree·다른 services는 변경하지 않았다. 작업 branch의 local commit으로 전달하며 master 반영·remote push·deploy는 하지 않는다.

## 남은 범위

검토한 armour roster 19개 중 이번에 6개를 추가하여 **13개가 남는다**.

- Body 5: Slipstrike Vest, Vile Robe, Death Mail, Wolfskin Mantle, Sleek Jacket.
- Helmet 2: Ancestral Tiara, Cryptic Crown.
- Boots 6: Tasalian Greaves, Drakeskin Boots, Sekhema Sandals, Blacksteel Sabatons, Faithful Leggings, Daggerfoot Shoes.

다음 우선순위는 shared evasion definitions를 재사용하는 Body의 Slipstrike Vest / Death Mail / Sleek Jacket 묶음이다. 이어 ES·Armour/ES Helmet 2개, Boots 6개를 진행한다. 확보된 source와 직접 detail 경로를 활용하며 source-only 단계로 되돌아가지 않는다. weapon/jewelry의 별도 researched roster와 deferred mechanics를 이 묶음에 섞지 않는다. remote push·merge·deploy는 수행하지 않는다.
