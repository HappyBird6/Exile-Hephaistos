> 최신 완료 집계 (2026-10-04): registry220 = active170(implemented170 = default163 + opt-in legacy7, pending0) + deferred50(보존 구현8 + 미구현42). Catalyst26는 검증된 제한 base만 IMPLEMENTED로 정리했다. 전체 구현178은 deferred8을 포함하므로 현재 사용 가능 수가 아니다. 아래의 이전 집계는 checkpoint 이력이다. [정의·base 제한·검증](workbench-catalyst-registry-2026-10-04.md).

# Ancient Liquid와 Time-Lost Jewel

기준 SHA `d36ef9a6a8a5b4371d59c09cf5d295eb1b2f0833`. 이번 묶음은 Ancient Liquid 13종과 Time-Lost Ruby/Emerald/Sapphire/Diamond의 실제 제작 경로를 추가한다. live 18080/18081, 사용자 browser/storage, 기존 DB volume은 변경하지 않는다. 로컬 commit만 허용하며 push·merge·deploy·release는 하지 않는다.

## 근거와 완료 범위

[GGG 0.5 patch notes](https://www.pathofexile.com/forum/view-thread/3932540), [Liquid Emotions](https://poe2db.tw/us/Liquid_Emotions), 각 [Time-Lost Ruby](https://poe2db.tw/us/Time-Lost_Ruby)·[Emerald](https://poe2db.tw/us/Time-Lost_Emerald)·[Sapphire](https://poe2db.tw/us/Time-Lost_Sapphire)·[Diamond](https://poe2db.tw/us/Time-Lost_Diamond)의 `ModsView.normal`과 `.liquid`를 직접 확인했다. Jewel 전체193행을 합치지 않는다. normal spawn tags, side, family, level, source bounds, source URL, Crafted Code를 보존한다. 동명 `of Potency`는 서로 다른 family와 bounds의 source signature로 구분한다. normal 행에는 Code가 없으므로 hover와 signature를 보존하고 존재하지 않는 game ID를 만들지 않는다.

| Base | Ordinary 후보 | Crafted outcome | 적용 가능한 Liquid 화폐 |
|---|---:|---:|---:|
| Ruby | 50 | 15 | Basic 13 |
| Emerald | 74 | 15 | Basic 13 |
| Sapphire | 58 | 15 | Basic 13 |
| Diamond | 160 | 5 | Basic 3 |
| Time-Lost Ruby | 53 | 14 | Ancient 13 |
| Time-Lost Emerald | 77 | 14 | Ancient 13 |
| Time-Lost Sapphire | 60 | 14 | Ancient 13 |
| Time-Lost Diamond | 160 | 4 | Ancient Potent 3 |

Liquid page의27개 inventory는 **Basic 13 + Ancient 13 + 별개 Liquid Verisium 1**이다. Ancient Inspiration이라는 추가 제작 화폐는 없다. Crafted outcome14는 Contempt 두 방향 때문에 currency13보다 하나 많다. Diamond에는 원문에 있는 base별 outcome만 허용한다. Basic↔Time-Lost Liquid 교차 적용은 unchanged/no spend이며, registry 등록만 추가한 지원이 아니다.

8개 Jewel base에서 기본18종(일반/Greater/Perfect Transmutation·Augmentation·Regal·Exalted·Chaos15 + Alchemy·Annulment·Divine3)을 지원한다. Magic 1P/1S, Rare 2P/2S, Crafted 하나가 ordinary slot 하나를 사용한다. Normal은 기존 사용자 승인 simulator starting state다. game drop rarity를 Normal이라고 추정하지 않는다. Artificer·Fracturing·Vaal·Hinekora·Desecration·Essence-on-Jewel과 unrelated deferred 범위는 지원 확대하지 않는다. Support/Explorer의 Solar 범위도 유지한다.

## Ancient 효과

일반 Ancient10종은 base별 Small 또는 Notable Passive Skills in Radius 효과다. Ruby Fear는 **Small** Warcry Speed이며 Sapphire/Emerald Fear는 **Notable** Critical bonus다. Suffering Ruby는 Notable Maximum Rage, Sapphire는 Notable Area, Emerald는 Notable Movement Speed다. Isolation은 각 base의 두 원소 damage increases/reductions를 해당 base 원소로 변환하는 조건부 효과다. passive-tree 상태를 입력받거나 합산하지 않는다. 원문 조건을 UI와 film에 보존한다.

- Ancient Potent Ferocity: Ruby Fire/Emerald Lightning/Sapphire Cold Resistance 5–7%, Diamond Chaos Resistance 4–5%를 radius 안의 Notable에 추가한다. **Basic Ferocity의 opposite-affix scaling이 아니다.** 해당 roll만 Divine로 재굴림하며 다른 canonical roll을 변경하지 않는다.
- Ancient Potent Melancholy: Radius를 Very Large로 변경하며 source `local jewel effect base radius [500]`를 fixed stat으로 보존한다. ordinary Medium `[150]`·Large `[300]`와 동일 `JewelRadiusLargerRadius` family이므로 기존 radius affix가 있으면 그 affix를 제거하는 branch만 유효하다.
- Ancient Potent Contempt: Prefix outcome은 +1 Suffix allowed, Suffix outcome은 +1 Prefix allowed다. 반대 side cap3·total5가 되며 Crafted 자체는 ordinary slot을 사용한다. removal 후 기존 overflow는 보존하고 신규 삽입은 현재 side/total cap을 따른다. 다시 Contempt를 적용할 수 있다.

네 Time-Lost base의 source implicit `local jewel effect base radius [1000]`도 fixed IMPLICIT로 보존한다. implicit은 Liquid/Annulment/Chaos/Divine로 삭제하거나 변경하지 않는다. `[1000]`을 임의의 radius label로 바꾸거나 explicit radius와 더하지 않는다. 실제 passive-tree 적용·거리 해석은 계산하지 않는다. `[150/300/500]`와 base implicit의 raw 값 및 조건은 source 데이터다.

Time-Lost base tags에는 `jewel_catalyst`가 없다. 현재 source로 Refined eligibility를 일반화하지 않고 Time-Lost Catalyst는 미지원으로 유지한다. Basic Jewel Refined Catalyst13과 Solar quality40→cap20 보존, Catalyst 재사용·유형변경 `max(existing,currentCap)`, Basic Ferocity의 중앙 결합 표시 정책은 그대로 유지한다.

## 공개된 권장 가정과 되돌릴 경계

actual spawn·removal·outcome weights는 확보되지 않았다. PoE2DB `DropChance=1`은 actual game weight로 사용하지 않는다. 기존 사용자 허용대로 적격 ordinary 후보1/N, **합법적인 outcome을 하나 이상 남기는 removal1/N → 해당 removal 후 유효한 source outcome1/N**을 사용한다. 실제 실패/retry/화폐 소모 모델이라고 주장하지 않는다. empty·wrong rarity·wrong category·family/slot 후보 부재·기존 Crafted는 원자적 unchanged/no spend이며 Omen도 보존한다. candidate IDs·n·확률·source·정책은 ledger에 남긴다.

Contempt cap-loss overflow와 Basic Ferocity × matching quality의 1회 중앙 HALF_UP rounding은 기존 잠정 simulator 정책이다. Small/Notable effect 확대를 다른 옵션의 roll에 추가로 적용하지 않는다. 조건부 transform presence marker1은 game stat이나 combat 값이 아니다. Radius fixed source stat은 unscalable이다. 실제 weight·거리·game precision 근거가 확보되면 새 catalog/rule version에서 정책을 변경할 수 있으며 기존 film의 canonical values·historical ledger는 다시 쓰지 않는다.

6언어 normal/Crafted 번역은 side·family·level·tags·bounds signature와 Crafted Code를 대조한다. Spanish source에 없는 Liquid row는 동일 ordinary source를 먼저 사용한다. Isolation·Ancient Potent·Emerald Life on Kill의 missing Spanish templates는 English semantics의 명시적 번역이며 원문에 없는 번역을 수집했다고 주장하지 않는다. base 이름은 각 locale의 Time-Lost source에서 가져온다.

## 검증과 잔여

browser 검증에서 quality cap이 없는 Jewel의 `qualityLimit:null`을 film 복원기가 거부하는 기존 문제를 발견했다. 계산된 cap도 null인 경우에만 받아들이도록 수정했고, 실제 API 응답 763개에 대해 state와 제작 evidence 복원을 함께 검증했다. 이전 실패 로그와 screenshot은 보존했다.

격리 복사본 `../ancient-liquid-20261004`, Docker project `exile-ancient-liquid-20261004`, API/UI18980/18981에서 무거운 검사를 순차 실행한다. 전체 결과·실패 이력·6언어 desktop/mobile screenshots·검사한 source 일치는 [검증 evidence](evidence/workbench-ancient-liquid-validation-2026-10-04.json)에 기록한다. 자기 변경만 formatter 결과를 동기화하고 자체 리뷰 후 commit한다.

요청된 제작 Liquid26와 reviewed Jewel 기본18종의 base별 실제 경로는 이 묶음으로 완료 범위에 들어간다. weights·passive-tree/거리·정밀 rounding·cap-loss game proof는 공개된 잠정 또는 미계산 경계이며, 알려진 제작 경로를 막는 추가 입력 요구로 사용하지 않는다. Liquid Verisium과 위 제외 기능은 이 완료 범위에 포함하지 않는다.
