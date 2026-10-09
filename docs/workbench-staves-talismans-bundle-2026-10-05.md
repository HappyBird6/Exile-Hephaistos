# Staves/Talismans selected highest-tier roster

중앙 registry refactor checkpoint `5287663f383d5199b43ff963fc51c55d70859270` 이후 Staves6/Talismans3를 데이터로 추가한다. 총 **134 bases**(기존125 + 신규9)이며, 기존125 IDs·snapshot·raw pools·API payloads·film 호환성을 보존한다. `Reviewed*` wrapper, Java base map, Frontend union/slug/class allowlist를 추가하지 않는다. 새로운 class 정책은 `base-policies.json`의 `staves`, `talismans` 두 행이다.

| Class | Selected bases | 선택 근거 |
|---|---|---|
| Staves | Permafrost75, Reflecting70, Dark65, Ravenous65, Perching65, Sanctified56 | ordinary 목록의 required character level 상위6. Heart of Ice, Mirror of Refraction, Dark Pact, Feast of Flesh, Spiraling Conspiracy, Consecrate의 서로 다른 skill family |
| Talismans | Maji79, Fungal78, Jade78 | ordinary 목록의 required character level 상위3. Maji rage implicit, Fungal attack speed, Jade physical damage profile의 의미 있는 sidegrade |

숫자는 required character level이며 itemLevel과 독립이다. highest-tier는 이 selected roster 기준이고 damage 최적 또는 전체 innate-skill family 완료를 뜻하지 않는다. Staves class14는 spellcasting weapon이고 Quarterstaves class58과 구분한다. Talismans class110의 attack properties는 source 표시이고 combat·shapeshift·passive-tree 동작을 계산하지 않는다. Maji는 canonical Maximum Rage +7–10 implicit을 보존하며 Fungal/Jade는 source implicit이 없다.

Staves ordinary185 + supported special4, Talismans ordinary158 + supported special8의 전체 eligible pools, families, tags, tier levels, ranges, source properties, requirements와 6locale names/templates를 포함한다. exact class ordered spawn을 검증한 후보만 사용한다. source의 published selection weights를 보존하며 공개되지 않은 실제 game weights와 numeric roll distribution을 확정하지 않는다. 근거 없는 후보 추가나 weight 추측 대신 기존 사용자 승인 valid-candidate 1/N 모델의 범위를 유지한다.

source는 [Staves](https://poe2db.tw/us/Staves), [Talismans](https://poe2db.tw/us/Talismans)와 각 base/hover/skill 페이지다. 원문 URL·시각·SHA와 6locale를 [evidence](evidence/staves-talismans-source-bundle-2026-10-05/roster-review.json)에 기록했다. [GGG 0.5.0 notes](https://www.pathofexile.com/forum/view-thread/3932540)는 released Talismans 및 Maji implicit 변경을 확인하고, [공식 patch index](https://www.pathofexile.com/forum/view-forum/2212)는 0.5.5d checkpoint를 제공한다. Current ordinary class list와 normal/magic/rare flags를 검증했으나 GGG trade API는 HTTP403이므로 unnamed trade entry 및 정확한 drop 경로/확률은 별도 확인이 남는다. `Unique`가 들어간 Staff metadata 문자열만으로 unique-only라고 추정하지 않는다.

원문 수집에서 Staff skill-level10행이 이전 Wand detail signature fallback과 충돌했다. 원래 수집 이력을 유지하고 `details-reconciled/Staves`에 exact Staff endpoint의 동일 text·stats·family·level·spawn order를 검증했다. Spanish special 표는 다른 locale와 행 순서/수가 달라 Code 및 family/level로 매칭했다. 이 차이를 기대값 완화나 원문 보정으로 숨기지 않았다.

socket source maximum은 표시용이며 Artificer/supplied socket execution은 Stocky의 empty0/1만 유지한다. 새9종의 typed Catalyst quality, Catalyst, socket execution과 class-ineligible Essence는 거절한다. quality20·overflow/HALF_UP·Shift/Alt/orange preview, legacy Jewel/Liquid, deferred50 및 Solar-only Support/Explorer는 유지한다.

## Bounded coverage audit

[coverage audit](evidence/staves-talismans-source-bundle-2026-10-05/coverage-audit.json)는 선택한 상위9 roster 완료와 미선정 ordinary variants를 분리한다. 현재 검증된 released equipment21 classes는 모두 representative가 있으나 전체 variants를 완료한 것은 아니다. 낮은 requirement의 Staff skill families, Talisman profiles 및 Crossbow Stout/Dedalian/Cumbrous 같은 sidegrade는 이번 selected 범위 밖이다.

Claws/Daggers/Swords/Axes/Flails/Traps는 Items index의 candidate 목록만으로 released/craftable이라 확정하지 않고 release-unverified로 유지한다. Flask/Charm·Unique mechanics·Runeforged/Runemastered·combat/shapeshift/passive-tree는 이번 제작 작업대 범위 밖이다. 무한 variants backlog나 whole-game 완료를 주장하지 않는다.

## 검증과 반영 경계

최종 검증은 Backend unit493/integration6, Frontend69 files1918 tests, API4300/registry582, browser1232/old-filled481/negative-Jewel17이다. 필수 check·codegen·build와 Frontend6명령이 통과했고 source 입력792개가 실제 검사 copy와 일치한다. 실패한 Node22 시도, historical count scope 수정 전 실패, CR wrapper 종료 실패, API fixture 정렬 실패는 격리 QA 로그에 보존했다. 마지막 standalone Docker build는 exit0이다.

6locale·1440/390px 전체 신규 카드와 화면, orange/Solar, 기존24class English 카드의 contact sheet30개를 pixel review했다. 잘림·겹침은 발견하지 않았다. Spanish source requirement는 Permafrost131 Int/Reflecting123 Int로 다른5locale114와 다르다. canonical 수치는 English source이며 localized 원문을 임의 보정하지 않았다. 해당 source reconciliation과 GGG trade403으로 막힌 실제 drop 경로 확인은 수동 검증 경계다. [completion](evidence/staves-talismans-source-bundle-2026-10-05/completion.json)에 검사·복구·live 보존·QA 종료 근거를 기록한다.

전용 Docker QA copy와 project `exile-staves-talismans-20261005`에서 Backend `check generateJooq bootJar`, Frontend 필수6검사·build, source/importer/registry parity, old125 seeded/API/film, new9 currency/Essence/Omen positive 및 rejection, 6locale desktop/mobile screenshots를 검증한다. 실행 결과와 제한은 evidence의 completion 결과를 따른다. `.env` 없는 원 stack config는 실행할 수 없으며 기존 disposable isolated Compose 설정만 사용한다.

live UI18081/API18080 서비스, DB volumes 및 사용자 browser storage를 보존한다. local commit까지만 수행하며 push/merge/deploy는 하지 않는다. parent의 최종 coverage audit 검토와 Git 통합은 별도 단계다.
