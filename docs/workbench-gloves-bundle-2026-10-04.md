# Highest-tier Gloves 실제 구현 묶음 — 2026-10-04

이 문서는 source preflight 이후 실제 runtime 추가를 기록한다. 기존 19개 base와 snapshot/film identity를 유지하고 아래 3개를 추가하여 22개가 된다. 기존 Stocky Mitts를 교체하지 않는다.

| Base | Defence | Character level | Attributes | Source |
|---|---|---|---|---|
| Massive Mitts | 187 Armour | 80 | 101 Str | [PoE2DB](https://poe2db.tw/us/Massive_Mitts) |
| Sirenscale Gloves | 54 Energy Shield | 80 | 101 Int | [PoE2DB](https://poe2db.tw/us/Sirenscale_Gloves) |
| Adherent Cuffs | 98 Armour, 27 Energy Shield | 80 | 55 Str, 55 Int | [PoE2DB](https://poe2db.tw/us/Adherent_Cuffs) |

Normal base popup에 implicit이 없으므로 빈 implicit으로 등록한다. Quality maximum은 20이며 socket은 미지원/null이다. 표시하는 Defence는 source base 값이며 quality/affix로 계산한 최종 Defence가 아니다. Craft Support/Explorer는 Solar-only를 유지한다.

Adherent Cuffs와 Tethering Bands는 같은 Defence/attribute 값이다. Tethering Bands는 `Unique1` metadata이므로 일반 Endgame metadata의 Adherent Cuffs를 선택했다. Base 목록의 source-backed 비교는 [preflight](workbench-armour-source-preflight-2026-10-04.md)를 따른다. DropChance, 높은 character level, 이름만으로 acquisition이나 game weight를 추론하지 않는다.

## Catalog와 번역

`scripts/import-gloves-bundle.mjs`는 exact family/name/required level/generation/effect로 기존 정의를 재사용하고, 필요한 ES/hybrid 정의만 추가한다. 각각 182/178/188개 일반 modifier와 class가 확인된 특수 modifier 4개를 담는다. 기존 catalog 파일과 template binding은 보존한다.

추가 정의 39개 중 35개는 public source Code/detail의 Local/unit/range/spawn tags로 확인한다. 나머지 hybrid flat Defence 4개는 공개된 두 numeric span을 이미 검증된 local Armour 및 local Energy Shield stat unit에 연결한다. 해당 근거와 원문은 `PUBLISHED_HYBRID_FLAT_DEFENCE_EXACT_LOCAL_UNITS_AND_RANGES` provenance로 남긴다. Normal-section eligible row를 누락하거나 highest tier만 남기는 modifier pool pruning을 하지 않는다.

게임 spawn weight가 검증되지 않은 새 pool은 일반 row weight 1과 `UNVERIFIED_GAME_WEIGHTS_EXPLICIT_UNIFORM_ELIGIBLE_CANDIDATES` 정책을 사용한다. 실행 응답에 실제 family/side/level 제한 이후 후보 ID와 N 및 1/N 가정을 남긴다. 이는 게임 확률이 아닌 사용자가 승인한 simulator 모델이다. 기존 base weight/snapshot 의미는 바꾸지 않는다.

영어·한국어·중국어 간체·중국어 번체·일본어·스페인어 base names, requirements, modifier templates를 source에서 연결했다. Compound template value order는 numeric span과 stat bound를 대조한다. 기본 Essence는 source Code가 가리키는 각 base target을 사용하며 Infinite의 복수 target을 유지한다. Perfect Grounding/Opulence와 Abyss는 class가 확인된 기존 특수 정의를 재사용한다. Body-only Perfect Essence, Horror, Artificer/socket, Runic Alloy, Catalyst는 새 Gloves에서 허용하지 않는다.

## 격리 검증과 복구 기록

QA project는 `exile-gloves-20261004`, API/UI는 localhost 19380/19381이다. 원본 tree runtime, live 18080/18081, 사용자 browser/localStorage와 DB volume을 사용하지 않는다. QA 출력은 새 `codex/gloves-qa-20261004` 경로에 둔다. 기존 출력 덮어쓰기 거부는 계속 준수한다.

Source importer의 최초 perfect/Abyss 검사는 축약된 section text를 full effect와 비교하여 실패했다. Source Code, family, generation, level과 public detail의 exact effect를 비교하도록 수정했고 실패 로그를 보존했다. Backend check 1은 Gradle provisioning 중 최종 소스 반영을 위해 중단했으며 assertion failure가 아니다. 이후 검증 결과는 완료 후 아래에 기록한다.

최종 검증:

| 영역 | 결과 | 근거 |
|---|---|---|
| Backend `spotlessApply check generateJooq bootJar` | unit/ArchUnit 396 + Docker integration 6 = 402 통과 | QA `backend-check-4.log`, XML |
| Frontend `npm ci`, lint, typecheck, format:check, test -- --run, build | 59 files, 1,756 tests 통과 | QA `frontend-check-6.log`; npm ci 성공은 check 3 |
| 여섯 언어 completeness | 정의 2,353개, compound binding 244개, 이름 220/220 | [JSON](evidence/gloves-runtime-bundle-2026-10-04/display-coverage.json) |
| 실제 API | 217 assertions, 22 initial bases | [JSON](evidence/gloves-runtime-bundle-2026-10-04/api-results.json) |
| 실제 browser | 223 checks, error 0 | [JSON](evidence/gloves-runtime-bundle-2026-10-04/browser-results.json) |
| 화면 | 3 bases × 6 locales × 2 widths = 36 captures, orange preview 3개 | QA `browser-attempt-2`; 여섯 언어 대표 화면 시각 검토 |
| Compose | 격리 profile `config --quiet` 통과 | QA `compose.yaml` |
| Importer cached replay | 9개 JSON 문서의 완전 동등성, ID/hash/원래 timestamp 유지 | [JSON](evidence/gloves-runtime-bundle-2026-10-04/importer-replay-results.json) |

API는 새 base의 전체 정의·1/N ledger, 일반 화폐 6종, Alchemy/Fracturing 및 Chaos lock 보존, basic Essence 28종씩, Perfect Grounding/Opulence/Hysteria/Abyss, omen 충돌의 원자적 거부, 저레벨 경계, 미지원 class/quality 경로를 확인한다. 기존 registry 220개와 과거 game terms/template/base 속성은 original commit과 완전 비교한다. Browser는 selector 22개, old Stocky/Body/Helmet film의 원래 ID 유지·복원·제작, 새 film의 분기·원래 미래 유지·reload, Shift/Alt, fractured, 빈 implicit, ES/hybrid roll 번역, target 검증 및 local orange preview를 확인한다. 기존 전체 테스트에 포함된 quality overflow와 중앙 rounding 검사를 유지했다.

회복 기록을 삭제하지 않았다:

- Frontend check 1: Node 22가 engine 요구 범위를 벗어나 npm ci 종료. Node 24.21.0/npm 11.19.0으로 재실행했다.
- Check 2: 새 테스트의 strict nullable dictionary 접근을 수정했다. 누락된 이름/ID는 계속 실패한다.
- Check 3: 격리 pretest의 catalog root를 `HEPHAISTOS_TRANSLATION_ROOT=/source`로 연결했다.
- Check 4: 기존 exact corpus count 1,522/228/103을 source 증가분 39/16/23과 함께 1,561/244/126으로 갱신했다. 모든 기존/새 binding의 여섯 언어 min/middle/max assertions를 유지했다. Check 5와 최종 6은 모두 통과했다.
- API check 1: Abyss의 기본 Amulet target routing이 새 Gloves에 적용되어 거부됐다. 검증된 Gloves target 두 개를 override에 연결하고 세 base의 성공·1/2 확률·omen 충돌 회귀 검사를 추가했다. importer에서 proof record와 definition 배열을 혼동한 첫 수정 실패도 별도 log에 보존했다. 완성 전 compile-only check 3을 중단하고 최종 Backend check 4를 전부 실행했다.
- API check 2: PowerShell text pipeline이 만든 과거 Unicode fixture가 손상됐다. 손상본을 별도 보존하고 Git blob을 native byte redirect로 다시 내보냈다. 비교 assertion을 바꾸지 않았으며 check 3에서 모두 통과했다.
- Browser attempt 1: Omen 선택을 활성화로 잘못 취급한 fixture가 orange preview에서 timeout됐다. 실제 즐겨찾기 배치/우클릭 활성화 흐름으로 바로잡고 별도 `browser-attempt-2`에서 전부 재실행했다. 최초 실패 JSON/PNG와 36개 캡처를 보존했다.

실제 게임 spawn/numeric roll 확률은 여전히 검증되지 않았다. build의 큰 chunk 안내는 남아 있으나 build는 성공했다. UI/서버/DB volume을 배포하거나 업데이트하지 않았다. 구현 checkpoint는 `5eaad014bfa7e78a35a425d76669c24a9eb484fd`; 후속 commit은 Abyss 수정과 최종 검증 근거를 담는다. 남은 armour는 아래 후보이며 현재 세 base에 대한 source/runtime blocker는 없다.

격리 QA services만 중지·제거했고 QA slot을 해제했다. 기존 live 18080/18081의 네 container는 계속 실행 중이다. 사용자 browser/localStorage, 기존 DB volume, 원본 repository와 remote branch는 변경하지 않았다. QA의 로그·XML·JAR·36개 최종 locale captures·3개 orange captures·실패 이력은 `codex/gloves-qa-20261004`에 남긴다.

## 다음 묶음

남은 armour 후보 19개:

- Body: Slipstrike Vest, Vile Robe, Death Mail, Wolfskin Mantle, Sleek Jacket.
- Helmet: Freebooter Cap, Ancestral Tiara, Gladiatorial Helm, Cryptic Crown, Grinning Mask.
- Gloves: Polished Bracers, Blacksteel Gauntlets, War Wraps.
- Boots: Tasalian Greaves, Drakeskin Boots, Sekhema Sandals, Blacksteel Sabatons, Faithful Leggings, Daggerfoot Shoes.

Sleek Jacket은 Primal Markings보다 source Defence와 요구치가 유리하다. War Wraps와 Faithful Leggings는 동일 Defence의 더 높은 요구치 변형을 모두 등록하지 않는 추천이다. 다음에는 같은 importer를 EV/hybrid-EV Gloves에 확장하고, Body/Helmet/Boots별 source eligibility를 검증한다. Weapon은 Warmonger/Guardian/Gemini/Fanatic/Obliterator Bow 후보를 비교한 뒤 실제 highest-tier/sidegrade/implicit을 확인한다. Ring/Amulet은 각 implicit과 eligible pool을 별도로 검증한다. deferred 50 mechanics를 확장하지 않는다.
