# 새 베이스·옵션 조합에서 유효 후보가 사라지는 카드 검사

## 결과

**실제 90회, 새 베이스 ID 81개, 장비 종류 18개.** 첫 응답에서 완성 경로가 하나 이상 나온 카드는 73/81장이다. 같은 카드의 유효 응답을 누적하면 78/81장이다. 나머지 3장도 모델이 제시한 순서를 그대로 실행했을 때 중간에 목표를 완성했다. 처음 완성된 상태에서 멈추는 오프라인 판정까지 적용하면 81/81장이다. 사전 기대는 각 카드의 허용 행동으로 만든 유효한 성공 분기 461개다. 모델이 완전한 실행열로 반환한 것은 213개, 완성 시점 종료로 추가 회수한 것은 5개다. 합계 218/461이며 243개의 순서는 여전히 빠져 있다.

[호출별 기대·입력·응답·실패 지점](new-base-responses.md) · [카드·기대 경로·검증 통과 경로와 호출 근거 JSON](new-base-routes.json) · [이전 49개 경로의 보존 근거](recipe-issue-budget.md)

| 문제 | 실제 호출 | 서로 다른 카드 | 첫 응답에서 경로가 0개였던 카드 | 원문 완주가 있는 카드 | 원문 경로 | 완성 시점 종료 포함 | 기대 경로 |
|---|---:|---:|---:|---:|---:|---:|---:|
| 일반·마법 슬롯과 승급 시점 | 10 | 4 | 4 | 4 | 14 | 14 | 40 |
| 가득 찬 쪽의 잡옵 제거 | 10 | 10 | 1 | 9 | 17 | 18 | 105 |
| 균질화 태그 연결 | 10 | 10 | 0 | 10 | 39 | 39 | 60 |
| 방향 징조와 일반 엑잘 | 10 | 10 | 1 | 9 | 28 | 30 | 80 |
| 복합 옵션이 두 능력치 행을 충족 | 10 | 10 | 0 | 10 | 20 | 20 | 20 |
| 동일 family의 기존 옵션 교체 | 10 | 10 | 0 | 10 | 23 | 23 | 50 |
| 베이스 고정 능력치 보존 | 10 | 10 | 1 | 9 | 18 | 20 | 20 |
| 명시 옵션 네 개와 분열 시점 | 10 | 10 | 0 | 10 | 38 | 38 | 40 |
| 한 화폐로 복수 옵션 획득 | 10 | 7 | 1 | 7 | 16 | 16 | 46 |
| 합계 | 90 | 81 | 8 | 78 | 213 | 218 | 461 |

### 첫 응답에서 사라진 여덟 카드

| 카드 | 호출별 완주 경로 수 | 회수 근거 |
|---|---|---|
| Plate Belt | 3번 0개 · 17번 0개 · 19번 4개 | 첫 획득 대상별로 실행 번호·화폐·대상을 적어 4/10 회수. |
| Blacksteel Crest Shield | 11번 0개 · 18번 0개 · 20번 4개 | 첫 획득 대상별 응답으로 4/10 회수. |
| Volant Quiver | 23번 0개 · 37번 3개 | 첫 변환, 제왕 승급, 나머지 행동을 구분해 3/10 회수. |
| Faithful Leggings | 31번 0개 · 38번 3개 | 제왕을 쓸 대상과 순서를 직접 골라 3/10 회수. |
| Mail Coat | 64번 0개 | 8개 중 일반 엑잘 A → B, B → A 두 순서를 완성한 후 같은 대상을 다시 붙이려 했다. |
| Siege Crossbow | 65번 0개 | 고정 옵션을 보존하고 A → B, B → A를 모두 달성했다. 이후 추가된 행동을 제외하면 2/2. |
| Toxic Quiver | 67번 0개 | 네 번째 후보에서 B 획득 → X 제거 → A 획득까지 유효하다. 뒤의 B → A가 중복이다. 완성 시점 종료 기준 1/6. |
| Wyrmscale Coat | 86번 0개 · 87번 0개 · 88번 0개 · 89번 2개 | 연금술 하나로 붙는 옵션 목록을 입력에 명시하고 첫 화폐·마지막 화폐의 대상을 각각 고르게 했다. D를 얻은 뒤 E, E를 얻은 뒤 D 두 경로를 모두 회수했다. |

같은 카드에서 먼저 통과한 후보는 뒤의 요청 결과와 합집합으로 보존했다. 특히 후반 세 묶음에서는 대기 중이던 일괄 요청을 포함하여 10회가 찬 뒤에 0개 카드가 확인됐다. 해당 카드에 추가 요청할 기회가 없었던 점은 실험 순서의 한계다. 마지막 복수 부여 묶음은 한 장씩 호출해 Wyrmscale Coat의 누락을 확인한 즉시 같은 카드부터 보완했다.

### 검증과 적용 범위

- 각 카드에는 아이템 레벨 82, 구체적인 베이스 ID, 시작 희귀도·보유 옵션, 보존·제거·획득 조건, 허용 행동과 실제 modifier ID를 고정했다. 같은 영문 표시명을 쓰는 셉터 세 변형도 실제 베이스 ID로 구분했다. 첫 80장과 이어서 등록한 10장 중 9장은 같은 문제의 재요청에 예산을 써서 모델에 보내지 않았다.
- Java 21에서 현재 소스의 `AdditionRules`와 `WorkbenchSimulator`로 상태별 후보와 제거 분기를 열거했다. 분열은 실제 K가 분열되는 분기, 연금술·복수 엑잘은 실제 여러 목표가 붙는 분기를 실행해 확인했다. 별도 검산에서는 사전 등록한 90장 모두 실제 `WorkbenchSimulator.apply`에 재입력해 **각 카드의 기대 실행열 집합이 일치함**을 확인했다. 검산의 난수 선택은 해당 결과가 존재하는지 확인하기 위한 것이며 비용·발생 빈도 추정에는 쓰지 않았다. 복합 능력치는 지정한 modifier가 두 행을 제공하는 카드이며 임의 수치의 Weighted Sum 일반화는 이번 집계 범위 밖이다.
- 90개 원문이 엄격한 응답 스키마를 만족하는지 검사했다. 행동을 순서대로 적용해 중간에 한 번이라도 실패한 후보는 그 뒤의 성공처럼 보이는 부분도 폐기했다. 시작부터 모든 행동이 유효하고 목표를 처음 충족한 경우만 완성 시점까지의 경로로 보존했다. 모델이 반환하지 않은 정답, 재배열·조합으로 만든 정답은 회수 실적에 포함하지 않았다.
- 판정 단위는 **카드와 정확한 화폐·대상 순서**다. 동일 카드의 같은 순서는 한 번만 세며, 다른 베이스에서 성공한 순서를 가져오지 않는다. JSON에 원본 카탈로그 해시, 시작·목표·행동 사전, 기준 커밋, 후보별 호출·위치·제외된 꼬리를 기록했다. 종전 실험의 49개 경로는 별도의 기존 입력으로 오프라인 재검산했다. 새 응답으로 교체하지 않았다.
- 첫 묶음부터 누적 149/300회, 미사용 151회다. 이전에 보류한 문제에 대한 신규 호출은 0회다. 이번 9개 문제가 각각 10회에 도달하고, 사전 기준상 가능한 검사 카드 모두에 실제 모델이 제안한 유효 경로가 남아 종료했다. 비용·확률·전체 게임 행동의 완전성 및 다른 모델에서의 재현성은 측정하지 않았다.

### 프롬프트와 응답 형식에 반영할 사항

1. 기본 출력은 화폐와 대상을 함께 갖는 `전략.대상` 순서다. 여러 전략을 요청해도 각 후보를 독립적으로 검증하고, 같은 카드에서 이미 통과한 경로를 유지한다.
2. 일반에서 희귀로 올리는 세 목표 카드는 첫 목표별로 `first`, `upgrade`, `other`에 실제 단계 번호와 화폐·대상을 지정하는 형식으로 최소 한 경로를 찾았다. 여전히 4개 카드에서 14/40이므로 모든 후보를 확보했다고 볼 수 없다. 실제 전송 문안은 호출 37·38에 있다.
3. 한 화폐가 여러 옵션을 함께 주는 카드는 행동별 `gains`를 보내고 각 행동 뒤 남은 목표에서 다음 대상을 고르게 했다. Wyrmscale Coat에서는 일반적인 실패 피드백만으로 후보가 0개였고, 이 형태로 바꾼 호출 89에서 2/2였다. 이 단일 대조는 다른 베이스의 성능까지 입증하지 않는다.
4. 완성된 뒤 중복 행동이 붙은 세 카드는 추가 모델 호출 없이 살릴 수 있었다. 백엔드가 단계별 목표 판정을 할 때 이 규칙을 적용하는 것이 이번 실험에서 확인된 개선 후보다. 서비스에는 아직 적용하지 않았다.

## 사전 등록

직전 실험 이후 남은 실제 호출 예산은 241회다. 종전 보류 항목인 Crafted 한도, 축성 완료 판정, 목걸이의 엑잘 실패 복구와 티어별 출처 압축을 재시도하지 않는다. 새 베이스마다 서로 다른 실제 일반 옵션 ID를 카드로 고정하고, Java Workbench가 허용하는 경로가 하나 이상 있는 카드만 모델에 요청한다. 목표는 각 양성 카드에서 검증 통과 후보를 하나 이상 남기는 것이다. 전체 경로 회수는 별도 지표다.

사전 카드 풀은 아래와 같다. 같은 문제는 장비를 바꿔도 최초 요청과 보완을 합쳐 최대 10회다. 미검사 카드를 몇 장씩 조사한 후, 누적 유효 후보가 0인 카드를 먼저 보완한다. 남은 호출은 새 베이스에 사용한다. 부작용이 없는 성공 결과 분기만 다루며, 시작 옵션·허용 전략·기대 경로는 호출 후 변경하지 않는다. 제거와 추가, 분열은 해당 무작위 결과가 나올 수 있다는 의미다.

| 카드 | 분류 | 베이스 | 획득 대상 (실제 modifier ID) | 기대 경로 수 |
|---|---|---|---|---:|
| room-1 | 가득 찬 쪽의 잡옵 제거 | Time-Lost Ruby | time-lost-ruby:prefix:bleeding, time-lost-ruby:suffix:of-lungs | 6 |
| tag-1 | 균질화 태그 연결 | Flexed Crossbow | crossbow:prefix:crystalising:LocalAddedColdDamageTwoHand10, crossbow:prefix:overpowering:WeaponElementalDamageOnTwohandWeapon5 | 6 |
| grade-1 | 일반·마법 슬롯과 승급 시점 | Plate Belt | rawhide-belt:prefix:enveloped, rawhide-belt:prefix:thorny, rawhide-belt:suffix:of-obsidian-skin | 10 |
| swap-1 | 동일 family의 기존 옵션 교체 | Rambler Jacket | sleek-jacket:prefix:chaser-s, stocky-mitts:suffix:of-magma | 5 |
| hybrid-1 | 복합 옵션이 두 능력치 행을 충족 | Dark Staff | staves:suffix:of-warmth:LightRadiusAndManaRegeneration1, staves:prefix:cryomancer-s:ColdDamagePrefixOnTwoHandWeapon8 | 2 |
| side-1 | 방향 징조와 일반 엑잘 | Visceral Quiver | stocky-mitts:prefix:honed, quiver:suffix:of-skewering:ArrowPierceChance4 | 8 |
| implicit-1 | 베이스 고정 능력치 보존 | Maji Talisman | crossbow:prefix:glaciated:LocalAddedColdDamageTwoHand7, attuned-wand:suffix:of-the-savant | 2 |
| fracture-1 | 명시 옵션 네 개와 분열 시점 | Akoyan Spear | crude-bow:suffix:of-regrowth, attuned-wand:suffix:of-the-apt | 4 |
| room-2 | 가득 찬 쪽의 잡옵 제거 | Shrine Sceptre | rattling-sceptre:suffix:of-the-proficient, rattling-sceptre:prefix:incinerating | 15 |
| tag-2 | 균질화 태그 연결 | Ancestral Tiara | ancestral-tiara:prefix:serene, stocky-mitts:prefix:rotund | 6 |
| grade-2 | 일반·마법 슬롯과 승급 시점 | Blacksteel Crest Shield | stocky-mitts:suffix:of-the-thunderhead, stocky-mitts:suffix:of-sheathing, wolfskin-mantle:prefix:interpermeated | 10 |
| swap-2 | 동일 family의 기존 옵션 교체 | Tawhoan Greatclub | attuned-wand:suffix:of-infusion, crossbow:prefix:burnished:LocalAddedPhysicalDamageTwoHand2 | 5 |
| hybrid-2 | 복합 옵션이 두 능력치 행을 충족 | Blacksteel Sabatons | blacksteel-gauntlets:prefix:flexible, stocky-mitts:suffix:of-magma | 2 |
| side-2 | 방향 징조와 일반 엑잘 | Desert Buckler | rusted-cuirass:suffix:of-corundum-skin, rusted-greathelm:prefix:fecund | 8 |
| implicit-2 | 베이스 고정 능력치 보존 | Guardian Bow | crude-bow:suffix:of-the-phantom, crude-bow:prefix:smouldering | 2 |
| fracture-2 | 명시 옵션 네 개와 분열 시점 | Aegis Quarterstaff | crossbow:prefix:freezing:LocalAddedColdDamageTwoHand5, crossbow:prefix:cremating:LocalAddedFireDamageTwoHand9 | 4 |
| room-3 | 가득 찬 쪽의 잡옵 제거 | Strife Pick | crude-bow:prefix:annealed, crude-bow:suffix:of-the-parched | 6 |
| tag-3 | 균질화 태그 연결 | Tasalian Focus | attuned-wand:prefix:hissing, attuned-wand:prefix:apprentice-s | 6 |
| grade-3 | 일반·마법 슬롯과 승급 시점 | Volant Quiver | stocky-mitts:prefix:heated, stocky-mitts:prefix:gleaming, attuned-wand:suffix:of-consumption | 10 |
| swap-3 | 동일 family의 기존 옵션 교체 | Elegant Crossbow | crossbow:prefix:crackling:LocalAddedLightningDamageTwoHand4, crude-bow:suffix:of-ire | 5 |
| hybrid-3 | 복합 옵션이 두 능력치 행을 충족 | Death Mail | death-mail:prefix:versatile, stocky-mitts:suffix:of-the-goliath | 2 |
| side-3 | 방향 징조와 일반 엑잘 | Forking Belt | rawhide-belt:prefix:teal, rawhide-belt:suffix:of-fertility | 8 |
| implicit-3 | 베이스 고정 능력치 보존 | Time-Lost Sapphire | time-lost-sapphire:prefix:rancorous, time-lost-sapphire:suffix:of-acclimatisation | 2 |
| fracture-3 | 명시 옵션 네 개와 분열 시점 | Grinning Mask | amulet:suffix:of-the-sage, stocky-mitts:suffix:of-the-thunderhead | 4 |
| room-4 | 가득 찬 쪽의 잡옵 제거 | Ruby | ruby:suffix:of-ignition, ruby:prefix:yelling | 15 |
| tag-4 | 균질화 태그 연결 | Hallowed Sceptre | rattling-sceptre:suffix:of-the-mentor, rattling-sceptre:suffix:of-the-taskmaster | 6 |
| grade-4 | 일반·마법 슬롯과 승급 시점 | Faithful Leggings | stocky-mitts:suffix:of-haast, tasalian-greaves:suffix:of-thawing, stocky-mitts:prefix:athlete-s | 10 |
| swap-4 | 동일 family의 기존 옵션 교체 | Bolting Quarterstaff | attuned-wand:suffix:of-the-prodigy, crude-bow:prefix:ranger-s | 5 |
| hybrid-4 | 복합 옵션이 두 능력치 행을 충족 | Fungal Talisman | crossbow:prefix:mercenary-s:LocalIncreasedPhysicalDamagePercentAndAccuracyRating4, attuned-wand:suffix:of-the-augur | 2 |
| side-4 | 방향 징조와 일반 엑잘 | Perching Staff | attuned-wand:suffix:of-voltages, staves:prefix:sizzling:FireDamagePrefixOnTwoHandWeapon2___ | 8 |
| implicit-4 | 베이스 고정 능력치 보존 | Grand Spear | one-hand-maces:suffix:of-slamming:StunDuration4, crude-bow:prefix:chilled | 2 |
| fracture-4 | 명시 옵션 네 개와 분열 시점 | Fanatic Bow | crude-bow:prefix:sparking, crude-bow:prefix:burning | 4 |
| room-5 | 가득 찬 쪽의 잡옵 제거 | Golden Targe | stocky-mitts:prefix:rotund, blacksteel-gauntlets:suffix:of-diversion | 6 |
| tag-5 | 균질화 태그 연결 | Fanatic Greathammer | crude-bow:suffix:of-the-parched, attuned-wand:suffix:of-assimilation | 6 |
| grade-5 | 일반·마법 슬롯과 승급 시점 | Fortified Hammer | crude-bow:prefix:hunter-s, crude-bow:prefix:glinting, one-hand-maces:suffix:of-the-brawler:452f5fe1185c | 10 |
| swap-5 | 동일 family의 기존 옵션 교체 | Mail Belt | rawhide-belt:prefix:teal, rawhide-belt:suffix:of-the-starfish | 5 |
| hybrid-5 | 복합 옵션이 두 능력치 행을 충족 | Shrine Sceptre | rattling-sceptre:prefix:envoy-s, rattling-sceptre:suffix:of-unmaking | 2 |
| side-5 | 방향 징조와 일반 엑잘 | Jade Talisman | crossbow:prefix:conqueror-s:LocalIncreasedPhysicalDamagePercentAndAccuracyRating6, one-hand-maces:suffix:of-infamy:LocalIncreasedAttackSpeed7 | 8 |
| implicit-5 | 베이스 고정 능력치 보존 | Flowing Raiment | sirenscale-gloves:prefix:shining, stocky-mitts:suffix:of-the-polar-bear | 2 |
| fracture-5 | 명시 옵션 네 개와 분열 시점 | Desolate Crossbow | attuned-wand:suffix:of-osmosis, crude-bow:suffix:of-the-vampire | 4 |
| room-6 | 가득 찬 쪽의 잡옵 제거 | Cryptic Crown | stocky-mitts:suffix:of-the-squall, stocky-mitts:prefix:precise | 15 |
| tag-6 | 균질화 태그 연결 | Warmonger Bow | crude-bow:suffix:of-destruction, crude-bow:suffix:of-calamity | 6 |
| grade-6 | 일반·마법 슬롯과 승급 시점 | Dreaming Quarterstaff | crude-bow:suffix:of-fury, one-hand-maces:suffix:of-the-brawler:452f5fe1185c, crossbow:prefix:empowering:WeaponElementalDamageOnTwohandWeapon3 | 10 |
| swap-6 | 동일 family의 기존 옵션 교체 | Daggerfoot Shoes | polished-bracers:suffix:of-flexure, daggerfoot-shoes:prefix:telepath-s | 5 |
| hybrid-6 | 복합 옵션이 두 능력치 행을 충족 | Tawhoan Tower Shield | tasalian-greaves:prefix:armadillo-s, rusted-cuirass:suffix:of-steel-skin | 2 |
| side-6 | 방향 징조와 일반 엑잘 | Flying Spear | crude-bow:suffix:of-renown, crude-bow:prefix:vapourising | 8 |
| implicit-6 | 베이스 고정 능력치 보존 | Broadhead Quiver | quiver:suffix:of-anger:AttackCriticalStrikeMultiplier2, stocky-mitts:prefix:crackling | 2 |
| fracture-6 | 명시 옵션 네 개와 분열 시점 | Ravenous Staff | staves:prefix:cobalt:IncreasedManaTwoHandWeapon2_, staves:prefix:darkened:ChaosDamagePrefixOnTwoHandWeapon4 | 4 |
| room-7 | 가득 찬 쪽의 잡옵 제거 | Ruination Maul | crude-bow:prefix:tyrannical, crude-bow:suffix:of-fury | 6 |
| tag-7 | 균질화 태그 연결 | Akoyan Club | crossbow:prefix:squire-s:LocalIncreasedPhysicalDamagePercentAndAccuracyRating1, crude-bow:prefix:infusing | 6 |
| grade-7 | 일반·마법 슬롯과 승급 시점 | Crude Bow | crude-bow:prefix:deliberate, crude-bow:prefix:empowering, crude-bow:suffix:of-the-phantom | 10 |
| swap-7 | 동일 family의 기존 옵션 교체 | Penetrating Quiver | stocky-mitts:prefix:buzzing, quiver:suffix:of-puncturing:ArrowPierceChance3 | 5 |
| hybrid-7 | 복합 옵션이 두 능력치 행을 충족 | Gemini Crossbow | crossbow:suffix:of-radiance:LocalLightRadiusAndAccuracy3, crossbow:prefix:honed:LocalAddedPhysicalDamageTwoHand4 | 2 |
| side-7 | 방향 징조와 일반 엑잘 | Freebooter Cap | stocky-mitts:prefix:opalescent, stocky-mitts:suffix:of-ephij | 8 |
| implicit-7 | 베이스 고정 능력치 보존 | Sinew Belt | rawhide-belt:prefix:fecund, rawhide-belt:suffix:of-the-polar-bear | 2 |
| fracture-7 | 명시 옵션 네 개와 분열 시점 | Feathered Raiment | stocky-mitts:suffix:of-the-narwhal, stocky-mitts:suffix:of-eviction | 4 |
| room-8 | 가득 찬 쪽의 잡옵 제거 | Reflecting Staff | attuned-wand:suffix:of-sparks, staves:prefix:electrifying:SpellDamageGainedAsLightningTwoHand5 | 15 |
| tag-8 | 균질화 태그 연결 | Drakeskin Boots | stocky-mitts:suffix:of-the-seal, tasalian-greaves:suffix:of-dissipation | 6 |
| grade-8 | 일반·마법 슬롯과 승급 시점 | Emerald | emerald:suffix:of-suffering, emerald:suffix:of-chronomancy, emerald:prefix:surging | 10 |
| swap-8 | 동일 family의 기존 옵션 교체 | Omen Sceptre | rattling-sceptre:suffix:of-nimbleness, rattling-sceptre:prefix:burning | 5 |
| hybrid-8 | 복합 옵션이 두 능력치 행을 충족 | Gladiatorial Helm | rusted-greathelm:suffix:of-light, amulet:prefix:magpie-s | 2 |
| side-8 | 방향 징조와 일반 엑잘 | Mail Coat | rusted-cuirass:suffix:of-iron-skin, polished-bracers:prefix:agile | 8 |
| implicit-8 | 베이스 고정 능력치 보존 | Siege Crossbow | crude-bow:suffix:of-restoration, crossbow:prefix:frigid:LocalAddedColdDamageTwoHand4 | 2 |
| fracture-8 | 명시 옵션 네 개와 분열 시점 | Clasped Sceptre | rattling-sceptre:prefix:lord-s, rattling-sceptre:prefix:glaciated | 4 |
| room-9 | 가득 찬 쪽의 잡옵 제거 | Toxic Quiver | stocky-mitts:prefix:gleaming, quiver:suffix:of-splintering:AdditionalArrowChanceQuiver2 | 6 |
| tag-9 | 균질화 태그 연결 | Double Belt | rawhide-belt:prefix:stirring, rawhide-belt:prefix:cerulean | 6 |
| grade-9 | 일반·마법 슬롯과 승급 시점 | Permafrost Staff | staves:prefix:arctic:ColdDamagePrefixOnTwoHandWeapon6, staves:prefix:professor-s:SpellDamageOnTwoHandWeapon4, attuned-wand:suffix:of-the-pupil | 10 |
| swap-9 | 동일 family의 기존 옵션 교체 | Obliterator Bow | crude-bow:prefix:crackling, crude-bow:suffix:of-the-remora | 5 |
| hybrid-9 | 복합 옵션이 두 능력치 행을 충족 | Gemini Bow | crude-bow:prefix:conqueror-s, crude-bow:suffix:of-the-parched | 2 |
| side-9 | 방향 징조와 일반 엑잘 | Diamond | diamond:prefix:acrimonious, diamond:suffix:of-ancestry | 8 |
| implicit-9 | 베이스 고정 능력치 보존 | Fine Belt | rawhide-belt:prefix:progressive, rawhide-belt:suffix:of-the-yeti | 2 |
| fracture-9 | 명시 옵션 네 개와 분열 시점 | Tasalian Greaves | stocky-mitts:suffix:of-eviction, tasalian-greaves:suffix:of-insulation | 4 |
| room-10 | 가득 찬 쪽의 잡옵 제거 | Engraved Crossbow | crude-bow:suffix:of-restoration, crude-bow:prefix:cruel | 15 |
| tag-10 | 균질화 태그 연결 | Sacramental Robe | sirenscale-gloves:prefix:abbot-s, stocky-mitts:prefix:healthy | 6 |
| grade-10 | 일반·마법 슬롯과 승급 시점 | Rusted Greathelm | rusted-greathelm:suffix:of-expulsion, rusted-greathelm:suffix:of-the-bear, rusted-greathelm:prefix:collector-s | 10 |
| swap-10 | 동일 family의 기존 옵션 교체 | Sanctified Staff | staves:suffix:of-havoc:SpellCriticalStrikeChanceTwoHand2, staves:prefix:biting:ColdDamagePrefixOnTwoHandWeapon2 | 5 |
| hybrid-10 | 복합 옵션이 두 능력치 행을 충족 | Shrine Sceptre | rattling-sceptre:prefix:envoy-s, rattling-sceptre:suffix:of-joy | 2 |
| side-10 | 방향 징조와 일반 엑잘 | Sekhema Sandals | stocky-mitts:suffix:of-ephij, stocky-mitts:prefix:azure | 8 |
| implicit-10 | 베이스 고정 능력치 보존 | Serrated Quiver | attuned-wand:suffix:of-valour, stocky-mitts:prefix:scorching | 2 |
| fracture-10 | 명시 옵션 네 개와 분열 시점 | Wide Belt | rawhide-belt:prefix:transformative, rawhide-belt:prefix:fortified | 4 |

## 복수 부여 사전 등록

최초 80회 이후 한 화폐로 여러 목표를 동시에 획득하는 별도 문제를 추가했다. 다음 열 장의 기대 경로를 모두 열거한 뒤 한 장씩 호출하고, 0개인 카드가 생기면 다음 베이스보다 먼저 재시도한다.

| 카드 | 베이스 | 실제 목표 ID | 기대 경로 수 |
|---|---|---|---:|
| batch-1 | Linen Belt | rawhide-belt:prefix:rejuvenating, rawhide-belt:suffix:of-tzteosh, rawhide-belt:prefix:enveloped | 10 |
| batch-2 | Seastorm Mantle | stocky-mitts:prefix:stout, stocky-mitts:suffix:of-the-gorilla, rusted-cuirass:prefix:princess, stocky-mitts:suffix:of-the-whelpling, rusted-cuirass:prefix:barbed | 2 |
| batch-3 | Ornate Belt | rawhide-belt:prefix:abating, rawhide-belt:suffix:of-plenty, rawhide-belt:prefix:fecund | 10 |
| batch-4 | Shrouded Mail | rusted-cuirass:prefix:barbed, rusted-cuirass:suffix:of-staunching, blacksteel-gauntlets:prefix:fencer-s, stocky-mitts:suffix:of-the-lightning, death-mail:prefix:warrior-s | 2 |
| batch-5 | Warlord Cuirass | rusted-cuirass:prefix:spiked, rusted-cuirass:suffix:of-damping, rusted-cuirass:prefix:shelled | 10 |
| batch-6 | Wyrmscale Coat | polished-bracers:prefix:ibex-s, stocky-mitts:suffix:of-the-fox, polished-bracers:prefix:blurred, amulet:suffix:of-the-flatworm, rusted-cuirass:prefix:queen-s | 2 |
| batch-7 | Sleek Jacket | war-wraps:prefix:ephemeral, amulet:suffix:of-the-troll, war-wraps:prefix:sylph-s | 10 |
| batch-8 | Fire Quiver | quiver:prefix:quick:ProjectileSpeed3, quiver:suffix:of-menace:AttackCriticalStrikeChance1, quiver:prefix:acute:DamageWithBows1, quiver:suffix:of-puncturing:ArrowPierceChance3, stocky-mitts:prefix:glinting | 2 |
| batch-9 | Vile Robe | stocky-mitts:prefix:robust, stocky-mitts:suffix:of-the-volcano, rusted-cuirass:prefix:barbed | 10 |
| batch-10 | Austere Garb | war-wraps:prefix:shadowy, polished-bracers:suffix:of-flexure, war-wraps:prefix:troubadour-s, amulet:suffix:of-the-virtuoso, amulet:prefix:marchioness | 2 |
