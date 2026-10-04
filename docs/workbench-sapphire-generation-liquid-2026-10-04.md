# Sapphire 일반 생성과 Basic Liquid 10

기준 SHA: `3208ff40d583d72ab7c77eb1baea595d644fb9a9`. 이전 Annulment/Divine 전용 결과를 확장한다. 이번 구현은 Sapphire에 한정하며 전체 Jewel/Liquid 요청의 완료가 아니다.

## 구현

[Sapphire](https://poe2db.tw/us/Sapphire)의 실제 HTML `new ModsView` JSON에서 `normal` 58행만 가져왔다. 모두 `intjewel` spawn tag와 level 1이며 Prefix 23·Suffix 35개다. corrupted·desecrated·Liquid 결과는 ordinary 후보에 섞지 않는다. `Bestial`과 `Overgrown`은 `DamageForm` family를 공유하므로 함께 생성할 수 없다. 원문 행·source URL·family·side·tags·bounds를 `catalog/sapphire/base.raw.json`에 보존하고 SHA256으로 검증한다.

Magic 1P/1S·Rare 2P/2S(총 2/4)를 적용한다. [PoB Item.lua](https://raw.githubusercontent.com/PathOfBuildingCommunity/PathOfBuilding-PoE2/dev/src/Classes/Item.lua)의 Jewel affixLimit 4 및 rarity/side 처리와 [Crafting wiki](https://www.poe2wiki.net/wiki/Crafting)를 보조 근거로 삼았다. 게임 raw base 자체에는 slot 필드가 없으므로 이 출처 수준을 확정 raw game data와 혼동하지 않는다. Normal은 사용자 승인된 가역 simulator 시작 상태이며, PoE2DB base의 enabled_rarity가 magic/rare/unique인 점을 명시한다.

Transmutation/Augmentation/Regal/Exalted/Chaos의 ordinary·Greater·Perfect 15종, Alchemy·Annulment·Divine를 실제 실행한다. [Minimum Modifier Level](https://poe2db.tw/us/Minimum_Modifier_Level)의 modifier type 전체 제외 예외를 기존 family/side 최상위 level 보존 규칙으로 적용하므로 level 1 Sapphire 후보를 tiered 화폐에서 모두 삭제하지 않는다. [Currency](https://poe2db.tw/us/Currency)의 Alchemy는 Normal 또는 Magic을 Rare 4옵션으로 만든다.

PoE2DB가 game modifier weight를 얻을 수 없다고 명시하므로 weight 1은 실제 game weight 또는 `DropChance=1`의 검증된 의미가 아니다. 사용자 승인된 후보별 균등 1/N 모델의 계산 입력이다. 모든 ordinary ADD에 `sapphire-uniform-candidates-v1` ledger로 현재 candidate set과 이 구분을 표시한다. 한 행의 공개 numeric magnitude를 내부 display-unit key로 보존한다. 실제 game stat ID를 추정하지 않는다. Minion attack/cast speed 한 행도 두 독립 roll을 만들지 않는다. integer roll 및 Catalyst 표시 precision은 simulator 모델이다.

## Basic Liquid

[Crafted Modifiers](https://poe2db.tw/us/Crafted_Modifiers) 및 [GGG 0.5.0 patch](https://www.pathofexile.com/forum/view-thread/3932540)의 Crafted cap과 Liquid 설명을 적용한다. Rare Basic Sapphire에 다음 10종을 지원한다.

| Material | Source Code | Side | 표시 범위 |
|---|---|---|---|
| Diluted Liquid Ire | JewelEnergyShield | P | maximum Energy Shield 10–20% |
| Diluted Liquid Guilt | JewelColdDamage | P | Cold Damage 5–15% |
| Diluted Liquid Greed | JewelChaosDamage | P | Chaos Damage 7–13% |
| Liquid Paranoia | JewelCastSpeed | S | Cast Speed 2–4% |
| Liquid Envy | JewelSpellDamage | P | Spell Damage 5–15% |
| Liquid Disgust | JewelManaonKill | S | maximum Mana on Kill 1–2% |
| Liquid Despair | JewelSpellCriticalChance | S | Spell Critical Hit Chance 5–15% |
| Concentrated Liquid Fear | JewelSpellCriticalDamage | S | Critical Spell Damage Bonus 10–20% |
| Concentrated Liquid Suffering | JewelAreaofEffect | P | Area of Effect 4–6% |
| Concentrated Liquid Isolation | JewelMaximumColdResistance | S | Maximum Cold Resistance +1% |

Crafted는 영속 modifier ID `sapphire:crafted:<source Code>`와 catalog `crafted` tag로 구분한다. 별도 불명확 property를 받아 지우지 않는다. zero weight로 ordinary pool에서 제외하고, 일반 옵션과 같은 family·side 제한 및 item당 하나의 Crafted cap을 검증한다. UI는 기존 Crafted 색상을 사용하며 film·undo/redo·reload·Divine에서도 이 identity를 유지한다. Annulment·Chaos는 다른 explicit처럼 제거할 수 있다.

가역 정책: **추가 결과가 side·family 조건을 만족하는 제거 분기만 먼저 구해 균등 선택한다.** 무효 분기는 실제 게임 retry로 주장하지 않는다. ledger의 `uniform-removal-v1`에 legal candidate set 및 game failure/retry 미검증을 기록한다. 기존 Crafted가 있으면 두 번째 Liquid는 상태 변경·Omen 소비 없이 거절한다. 이 정책은 향후 게임 실패 소비/재시도 근거가 나오면 독립적으로 교체할 수 있다. Liquid는 Essence용 Crystallisation Omen을 소비하지 않는다.

## 유지·잔여 범위

기존 Cast Speed ID·원래 2–4 roll을 유지하며 이전 Sapphire snapshot film은 concrete validation 후 새 snapshot identity로만 호환 갱신한다. cap20이 되어도 저장 quality40을 보존하는 기존 정책과 Catalyst 반복·type 변경 `max(existing,currentCap)` 정책, 6개 언어, Alt/Shift, 주황 Omen preview는 유지한다.

Liquid inventory 27은 **Basic13 + Ancient13 + Jewel 제작과 무관한 Liquid Verisium1**이다. Sapphire Liquid raw 15는 13개 currency의 결과 행 수(Ferocity·Contempt 각각 두 결과)이고, Time-Lost Sapphire raw 14도 13개 Ancient currency의 결과 행 수(Contempt 두 결과)다. currency 수와 outcome 행 수를 혼동하지 않는다.

다음 작업: Ruby/Emerald/Diamond의 base별 실제 ordinary pool과 Basic Liquid 대상, Potent Melancholy 조건부 tree 효과·Ferocity opposite-side scaling·Contempt 추가 slot과 cap-loss overflow 유지, Ancient13의 Time-Lost base/radius/Small·Notable 범위. Fracturing/socket 특례·Essence-on-Jewel·Vaal·Hinekora·Desecration 및 기존 deferred 범위는 이번 구현에 포함하지 않는다. 운영 18080/18081에는 배포하지 않는다.

검증 evidence는 `docs/evidence/workbench-sapphire-generation-liquid-validation-2026-10-04.json`에 기록한다. 새 격리 project `exile-sapphire-generation-20261004`의 API 18780·UI 18781에서 기본 화폐 18종과 Basic Liquid 10종의 성공 경로를 확인했다. API 752개, 브라우저 83개 확인과 6개 언어의 desktop/mobile 스크린샷을 생성했다. 기존 quality40 cap-loss 보존·Omen 회귀 API도 9개 기존 base에서 640개 확인을 통과했다. 운영 18080/18081은 갱신하지 않았다.

실패 이력은 새 evidence 디렉터리에 보존한다. Node22 engine 오류는 요구된 Node24로 수정했고, 옛 단일 Sapphire modifier 테스트는 실제 68개 정의를 엄격히 검사하도록 갱신했다. QA의 Whittling 입력은 기존 Chaos 트리거로 수정했으며, Liquid 이름은 제품과 같은 registry fallback을 사용했다. 최종 자체 리뷰를 수행했으며 독립 리뷰를 수행했다고 주장하지 않는다.
