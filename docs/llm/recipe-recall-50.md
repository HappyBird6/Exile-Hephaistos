# 가능한 제작 경로의 누락을 측정한 추가 50회

## 사전 등록

판단 순서는 ① 검증을 통과한 고유 경로 / 사전 기대 경로, ② 가능한 카드에서 유효한 후보가 하나도 남지 않은 횟수, ③ 검증에서 탈락한 후보와 토큰 사용량이다. 불가능한 카드에서 빈 목록을 맞힌 횟수를 양성 회수에 더하지 않는다. 전체 획득 순서는 모델이 명시하며 준비 행동을 임의로 보충하지 않는다. 기대 목록은 평가 전용이다.

기준선은 [직전 30회](recipe-feedback-30.md)의 최종 요청 형식이다. 개발 카드 W(빈 마법봉 6), H2(기존 Crafted 마법봉 2), N(분열 목걸이 7), R(반지 6), G(마법 장갑 2), Q2(한도 확장이 필요한 두 Crafted 2)의 기대 합은 25개다. 동일 기준선 6회와 무변경 반복 6회, 첫 응답의 획득 위치·행동 관측을 활용한 추가 탐색 6회, 수정안 6회를 비교한다. 이후 카드당 두 번의 요청으로 13장에 같은 선택 규칙을 적용한다. 호출 25–37과 38–50은 각각 첫 요청과 보완 요청이다. 모든 응답의 완성 경로만 검증하고 합집합을 낸다.

최종 평가에는 위 여섯 장 외에 H4(품질·수치 순서 2), Q4(이미 완성된 무행동 1), 음성 대조 H3·Q1, 그리고 아래 새 카드 세 장을 포함한다. T1은 기존 Crafted 한도 1이 가득 찼어도 일반 시전 속도만 추가하면 되는 마법봉으로 1개, T2는 이미 한도 2이며 기존 Crafted를 보존하고 새 스킬·시전 속도를 어느 순서로도 얻는 마법봉으로 2개다. T3은 기존 R에 일반 번개 저항을 추가한 3옵션 반지다. 생명력과 냉기는 일반/훼손 중 선택하고 훼손 두 번을 제외하면 출처 조합 3가지 × 획득 순서 6가지로 18개다. 경로 수는 위 카드의 출처와 허용 전략을 기준으로 정했다. 최종 양성 분모는 49개(이미 완성 1개 포함)다.

## 25회차 전에 고정한 요청 선택 규칙

첫 요청은 기준선 형식을 쓰되 필수 Crafted 행이 둘 이상이고 남은 행이 셋 이상이면 이전 50회 실험의 D 문안을 사용한다. 두 번째 요청은 앞선 검증에서 통과한 후보로부터 미관측 획득 위치와 행동 ID를 계산한다. 기존 Crafted를 보유하고 확장 수단이 있으면 준비와 본 순서를 나누는 형식, 대체 출처 행이 있으면 기준 목표의 전략과 위치를 함께 묻는 형식, 나머지는 기준 목표 앞·해당 행동·뒤 세 부분을 묻는 형식을 쓴다. 13장 모두 두 요청을 보낸다. 두 번째 요청의 선택이나 내용에 평가용 정답을 사용하지 않는다.

## 결과

고정한 최종 절차로 **가능한 카드 11/11장에 유효 후보가 남았다.** 양성 카드의 기준 경로는 49개이고, 첫 요청 37개와 보완 요청 25개의 검증 통과 후보를 합치면 **38/49(77.6%)**다. 9장은 모든 기준 경로를 회수했다. 나머지 두 장의 누락은 분열 목걸이 1개, 3옵션 반지 10개다. 불가능한 카드 두 장은 이 분모에 넣지 않았다.

| 카드 | 기대 | 첫 요청 | 보완 요청 | 두 응답 합집합 |
|---|---:|---:|---:|---:|
| W 빈 마법봉 | 6 | 6 | 2 | 6 |
| H2 기존 Crafted 마법봉 | 2 | 1 | 2 | 2 |
| N 분열 목걸이 | 7 | 6 | 6 | 6 |
| R 두 옵션 반지 | 6 | 6 | 6 | 6 |
| G 마법 장갑 | 2 | 2 | 2 | 2 |
| Q2 두 Crafted 마법봉 | 2 | 2 | 2 | 2 |
| H4 품질·수치 목걸이 | 2 | 2 | 2 | 2 |
| Q4 이미 완성된 목걸이 | 1 | 1 | 0 | 1 |
| T1 기존 Crafted, 빈 소켓 없음, 일반 시전 속도 목표 | 1 | 1 | 1 | 1 |
| T2 이미 한도가 확장된 기존 Crafted 마법봉 | 2 | 2 | 2 | 2 |
| T3 생명력·냉기·번개 반지 | 18 | 8 | 0 | 8 |

최종 첫 요청과 보완 요청에서 상태·분기 검증을 통과하지 못한 후보는 각각 15개와 31개였다. 검증 후 후보가 하나도 없는 양성 카드는 첫 요청 0장, 보완 요청 단독 2장, 합집합 0장이다. 음성 H3·Q1에도 두 요청 모두 후보를 냈고 검증기가 모두 거절했다. 후보가 비거나 전부 거절된 상태는 탐색 결과만으로 제작 불가를 확정할 수 없다. 이미 완성된 Q4의 빈 실행열 하나는 유효한 결과다.

### 추가 요청이 실제로 가져온 경로

- H2의 첫 요청은 시전 속도 선취득만 찾았다. 보완 요청은 첫 준비에 `cap_up.item`을 명시하고 `safe_ess.skill → base_add.speed`를 반환해 빠졌던 방향을 채웠다. W에서는 첫 요청의 D 문안이 시전 속도 선·중·후 획득을 각 2개씩 회수했다.
- N은 첫 요청과 보완 요청 모두 `safe_ess.def → base_add.life → whittle_miss.life`가 빠졌다. 첫 응답에서 생명력의 두 위치와 모든 행동 ID를 각각 이미 봤기 때문에 보완 요청의 미관측 목록도 비었다. 개별 행동의 출현만으로 방어 선취득 **후** 엑잘 실패 복구 조합의 회수 여부를 판단할 수 없다.
- T3의 첫 요청은 8/18이었다. 보완 요청은 생명력 세 번째가 빠졌다는 힌트를 받았지만 12개 후보 모두 번개를 생략하고 냉기를 일반과 훼손으로 두 번 얻으려 했다. 스키마는 빠진 10개 경로도 모두 표현할 수 있었다. 다음 실험 후보는 *검증된 경로의 목표별 이웃 순서* 및 *거절된 경로에서 중복·미달된 목표 쌍*을 되돌려주는 방식이다. 이번 50회에서는 이 방식의 효과를 측정하지 않았다.
- Q4 보완 응답은 실험 스키마의 빈 행동 사전용 예비 문자열 `unused`를 행동으로 반환했다. 검증기는 이를 거절했고 첫 요청의 빈 실행열을 보존했다. 빈 결과와 이미 완성된 결과를 구분할 때 이 예비 문자열을 실행 대상으로 취급할 수 없다.

### 두 번째 요청 방식 비교

동일한 개발 카드 6장(25개 기준 경로)에 대해 첫 요청을 고정하고 두 번째 요청만 바꿨다. 무변경 반복은 모델·문안·입력·응답 스키마가 같다. 각 행은 두 호출의 유효 완성 경로를 합친 결과다.

| 요청 조합 | 회수 | 첫 요청 대비 새로 회수한 경로 |
|---|---:|---|
| 기준선 1–6 단독 | 21/25 | 해당 없음 |
| 기준선 + 무변경 반복 7–12 | 21/25 | 없음 |
| 기준선 + 위치 힌트 및 준비 분리 13–18 | 22/25 | 기존 Crafted 마법봉의 스킬 선취득 |
| 기준선 + 앞·기준 행동·뒤 분리 19–24 | 22/25 | 빈 마법봉의 시전 속도 중간 획득 한 방향 |

19–24는 13–18의 누락을 검토한 뒤 실행했다. 이 차수는 첫 응답의 힌트를 보내지 않고 앞뒤 획득 수를 스키마에 표시한 독립 비교다. 25회부터 고정한 방식은 카드 상태로 형식을 고르고, 보완 요청에는 실제 첫 응답의 관측만 제공했다. 최종 13장 전체에 보완을 보낸 비용은 입력 10,477·출력 1,622토큰이고 추가 회수는 1개였다. 새 카드 3장의 합집합은 11/21이다. 이 카드 구성이 실제 사용자 요청 분포를 대표하는지는 검증하지 않았다.

### 채점과 확인 범위

- 각 단계는 모델이 적은 화폐 전략과 대상이다. 준비 배열과 실행 배열, 또는 before·take·after 순으로 읽었다. setup/ifMiss가 명시된 경우만 해당 위치에 붙였고 후보를 이어 붙이거나 빠진 단계를 만들어 넣지 않았다. 두 응답에서 검증된 경로를 보존하고, 획득 순서가 같고 필요한 한도 확장의 적법한 위치만 다른 경우 중복을 제거했다.
- 경로 실행과 직접 관련 없는 위치 그룹의 잘못된 분류는 실행 가능한 후보를 잃지 않도록 회수 기준에서 제외했다. hit/onMiss 분기와 실제 실행 행동은 일치해야 한다. 위치 그룹까지 강제해도 최종 합집합은 같은 38개였다.
- 기준선·보완안 모두 **모델로 보낸 각 카드의 모든 기준 실행열**을 실제 응답 형식에 넣어 표현 가능한지 오프라인으로 확인했다. 슬롯 제약·희귀도·목표 중복·복구 직전 행동을 검사하는 순차 검증기 및 기존 상태 전이 열거기로 50개 응답을 대조했다. T3는 기존 반지에 일반 번개 저항 획득을 조합한 실험용 카드다. 이는 고정 전략에 대한 검증이며 Java Workbench·게임 전체 옵션 풀의 전수 검사는 수행하지 않았다.
- 실제 호출은 모두 `gpt-4.1-mini-2025-04-14`, temperature 0, strict JSON Schema, 출력 상한 3,200토큰, 저장 비활성으로 정상 종료했다. 총 입력 38,985·출력 5,208토큰이다. 가능한 경로 수와 평가용 경로 목록은 전송하지 않았다.

## 카드별 기대 경로

### W

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"remainingGoals":["mana_skill","skill","speed"],"expected":["safe_alloy.mana_skill safe_ess.skill base_add.speed","safe_alloy.mana_skill base_add.speed safe_ess.skill","safe_ess.skill safe_alloy.mana_skill base_add.speed","safe_ess.skill base_add.speed safe_alloy.mana_skill","base_add.speed safe_alloy.mana_skill safe_ess.skill","base_add.speed safe_ess.skill safe_alloy.mana_skill"]}
```

### H2

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"remainingGoals":["skill","speed"],"expected":["safe_ess.skill base_add.speed","base_add.speed safe_ess.skill"]}
```

### N

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"remainingGoals":["life","def"],"expected":["first_chaos.life safe_ess.def","base_add.life safe_ess.def","base_add.life whittle_miss.life safe_ess.def","reveal.life safe_ess.def","safe_ess.def base_add.life","safe_ess.def base_add.life whittle_miss.life","safe_ess.def reveal.life"]}
```

### R

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"remainingGoals":["life","cold"],"expected":["base_add.life base_add.cold","base_add.cold base_add.life","reveal.life base_add.cold","base_add.cold reveal.life","base_add.life reveal.cold","reveal.cold base_add.life"]}
```

### G

```json
{"start":{"base":"Stocky Mitts","rarity":"magic","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"filler"}]},"craftedLimit":1,"capacitySocket":0},"remainingGoals":["life","pen"],"expected":["upgrade_ess.life safe_alloy.pen","regal_base.life safe_alloy.pen"]}
```

### Q2

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"remainingGoals":["mana_skill","skill"],"expected":["safe_alloy.mana_skill safe_ess.skill","safe_ess.skill safe_alloy.mana_skill"]}
```

### H4

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","raw":99}],"suffix":[{"id":"str","raw":7}]},"craftedLimit":1,"capacitySocket":0},"remainingGoals":["Flesh20","values"],"expected":["flesh.item divine.item","divine.item flesh.item"]}
```

### Q4

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","value":90},{"id":"def","crafted":true}],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"remainingGoals":[],"expected":[""]}
```

### T1

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"remainingGoals":["speed"],"expected":["base_add.speed"]}
```

### T2

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]},"craftedLimit":2,"capacitySocket":0},"remainingGoals":["skill","speed"],"expected":["safe_ess.skill base_add.speed","base_add.speed safe_ess.skill"]}
```

### T3

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"remainingGoals":["life","cold","lightning"],"expected":["base_add.life base_add.cold base_add.lightning","base_add.life base_add.lightning base_add.cold","base_add.cold base_add.life base_add.lightning","base_add.cold base_add.lightning base_add.life","base_add.lightning base_add.life base_add.cold","base_add.lightning base_add.cold base_add.life","reveal.life base_add.cold base_add.lightning","reveal.life base_add.lightning base_add.cold","base_add.cold reveal.life base_add.lightning","base_add.cold base_add.lightning reveal.life","base_add.lightning reveal.life base_add.cold","base_add.lightning base_add.cold reveal.life","base_add.life reveal.cold base_add.lightning","base_add.life base_add.lightning reveal.cold","reveal.cold base_add.life base_add.lightning","reveal.cold base_add.lightning base_add.life","base_add.lightning base_add.life reveal.cold","base_add.lightning reveal.cold base_add.life"]}
```

### H3

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"remainingGoals":["mana_skill","skill"],"expected":["NONE"]}
```

### Q1

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"remainingGoals":["skill","speed"],"expected":["NONE"]}
```

## 실제 사용한 문안

### P1

```text
역할: PoE2 제작 작업대에서 시도할 완전한 화폐 사용 순서를 설계한다. 단계 ID는 전략.대상이다.
1. 시작 옵션을 보존한다. requiredRows가 비면 recipes에 빈 행동열 하나를 반환한다. Crafted의 기존 점유 + 필수 신규 자리 수가 현재 한도 + 확장 가능량을 넘으면 모든 목록을 비운다.
2. afterFirst의 각 키는 첫 획득 후보이고 그 아래 각 행은 이후 채워야 할 목표다. 키에 해당하는 행동도 응답 배열에 직접 적는다. 행마다 하나만 고르고 모든 행을 채운다. 남은 두 행의 순서를 양쪽으로 바꾸며 같은 행의 대체 전략도 각각 검토한다. 시작 상태는 경로마다 초기화한다.
3. 전략 사전과 카드 규칙을 따른다. 준비 행동은 목표를 채우지 않는다. Crafted 자리가 모자라면 해당 획득 전에 한도를 확장하는 단계를 직접 적는다. 각 후보를 끝까지 읽어 시작 옵션 보존, 희귀도, 훼손 슬롯, 모든 목표의 충족을 확인한다.
4. 각 목록에는 첫 획득과 준비를 포함한 전체 경로를 한 번씩 적는다. hit는 직접 적중, onMiss는 엑잘 실패 직후 동일 대상에 절사 복구를 사용한 경로다. 두 칸이 없는 경우 해당 첫 획득의 목록에 바로 적는다. 불가능하면 빈 목록을 반환한다. 확률·비용은 백엔드가 계산한다.
```

### P2

```text
역할: PoE2 제작에 쓸 화폐 전략과 대상의 다양한 실행 순서를 제안한다. 백엔드가 각 후보의 합법성·확률·비용을 검증한다. 불확실하거나 낮은 확률이라는 이유로 가능한 후보를 빼지 않는다.
needOnePerRow의 각 행을 대체 행동 하나로 충족한다. pivot이 몇 번째 목표로 획득되는지에 따라 at1, at2, …에 전체 경로를 나눠 적는다. 같은 위치에서도 다른 목표의 순서와 대체 출처를 바꿔 본다. coverage는 이전 응답이 아직 보여주지 않은 위치와 행동이다. 이미 본 위치에도 다른 순서가 있을 수 있다.
각 경로의 prepare는 첫 획득 전에 쓸 준비 행동 순서, steps는 이어서 쓸 모든 전략.대상 순서다. 두 배열을 이 순서대로 그대로 실행한다. 한도 확장이 필요하고 사용 가능하면 prepare에 cap_up.item을 적을 수 있다. 기존 Crafted도 보존한다. 첫 획득은 첫 준비 행동 뒤에 온다.
조건부 절사 복구가 가능하면 엑잘 적중 경로와 실패 직후 복구 경로를 각각 완성까지 적는다. 이미 완성이면 recipes에 빈 행동열 하나다. 빈 후보 목록은 찾은 후보가 없다는 뜻이다.
```

### P3

```text
역할: PoE2 제작 레시피 후보 설계자. 가능할 여지가 있는 화폐·대상 순서를 넓게 제시한다. 확률·비용과 최종 실행 가능성은 백엔드가 검증한다.
needOnePerRow의 행마다 성공 전략 하나를 고른다. pivot은 위치를 바꿔 살펴볼 목표다. 각 후보는 before의 행동들 → take의 행동 → after의 행동들을 차례로 실행하면 완성되는 독립 레시피다. take에는 pivot의 구체적인 전략.대상을 적는다. 나머지 목표의 앞뒤 순서와 각 출처 선택도 바꾼다. 동일한 위치에 여러 후보가 들어갈 수 있다.
각 단계가 객체면 setup을 먼저 실행하고 use를 실행한다. Crafted가 이미 가득 찼다면 새 Crafted를 얻기 전 setup에 가능한 한도 확장을 직접 적는다. ifMiss는 그 use가 빗나갔을 때만 즉시 쓸 동일 대상 복구 행동이다. none이면 추가 실행이 없다. 적중 경로와 복구 경로는 별도로 적는다.
coverage가 있으면 기존에 드문 위치와 전략부터 보충한다. 빈 배열은 이번에 제안할 후보가 없다는 뜻이다. 이미 완성된 시작 아이템은 빈 실행열 하나로 표현한다.
```

### P4

```text
역할: PoE2 제작 경로 설계자. 보존 조건을 만족하는 다양한 완성 후보를 제시한다.
입력: goals의 각 키는 미달 조건이고 값은 그중 하나만 성공하면 해당 키가 끝나는 대체 전략.대상 목록이다. start는 현재 상태, targets는 출처, strategies는 실행 의미다. helpers는 준비·복구 전용이다. 모든 실험은 고정 시작 아이템을 쓴다.
순서: openers의 각 전략.대상으로 경우를 나눈다. 획득한 키를 지우고 남은 키 하나를 고르는 모든 순서와 그 키의 각 대체 전략을 시도한다. goals의 나열 순서에는 의미가 없다. 일반 옵션을 첫째·중간·끝에 두는 경로도 점검한다.
실행: 시작 Crafted 점유에서 출발한다. crafted.uses 전략은 자리를 하나씩 쓰며, 부족하면 그 전략 직전에 가능한 한도 확장을 한 번 적는다. 첫 카오스는 첫 획득 전용이다. 같은 아이템의 훼손 슬롯 하나를 공유한다. base_add 적중 경로마다 허용된 동일 대상 whittle_miss가 있으면 그 직후 복구를 넣은 경로도 따로 적는다.
출력: 각 opener 키에 해당 첫 획득을 포함한 전체 전략.대상 배열들을 반환한다. 각 경로가 goals의 키를 하나씩 모두 채웠는지 검토한다. 불가능한 키는 []다. 백엔드는 반환 경로의 전이·확률·비용을 검증한다.
```

### P5

```text
역할: PoE2 제작 후보의 누락을 줄인다. 정확한 확률·비용·적법성은 백엔드가 검증한다.
응답 키 전략.대상@at번호는 pivot 목표를 어떤 전략으로 몇 번째에 획득할지 탐색할 칸이다. before → take → after의 세 부분에 직접 선택한 실행 단계를 적는다. take는 키의 전략.대상이다. 다른 모든 목표를 한 번씩 획득하고, 남은 행의 순서와 각 행의 대체 출처를 바꿔 가능한 별도 후보를 만든다. 희소하거나 보존 성공이 필요한 방향도 고려한다.
setup이 있으면 use 전에 해당 준비 화폐를 실행한다. ifMiss가 있으면 해당 use 실패 직후 명시한 복구를 실행한다. none은 해당 보조 행동 생략이다. 엑잘과 절사 복구가 있는 경우 적중 및 실패 복구 양쪽의 완전한 후보를 별도로 제안한다. 기존 검증에서 보이지 않은 위치나 행동은 우선 살펴보되, 이미 본 위치에서도 다른 후속 순서가 가능하다. 빈 목록은 이번 요청에서 찾은 후보가 없음을 뜻한다.
```

## 호출별 기대와 실제 응답

기대 집합은 각 카드의 동명 절에 한 번 기록했다. 각 호출에 모델이 받은 입력·형식, 실제 응답 원문과 채점 결과를 연결한다. 누락은 유효 기준 집합과의 차이다. 음성 대조의 분모는 0이므로 회수율을 계산하지 않는다.

### 요청 1 · W · base

문안 P1. 기대 W. 회수 4/6. 거절 0개, 유효 중복 2개. 입력/출력 860/146토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"strategies":{"safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item."},"rules":["mana_skill과 skill은 서로 다른 Crafted. speed는 일반 접미. 세 옵션 모두 보존. 두 번째 Crafted 전 한도 확장 필수."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":2,"expandBy":1,"desecratedUsed":0},"requiredRows":["mana_skill","skill","speed"],"afterFirst":{"safe_alloy.mana_skill":{"skill":["safe_ess.skill"],"speed":["base_add.speed"]},"safe_ess.skill":{"mana_skill":["safe_alloy.mana_skill"],"speed":["base_add.speed"]},"base_add.speed":{"mana_skill":["safe_alloy.mana_skill"],"skill":["safe_ess.skill"]}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"safe_alloy.mana_skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed","cap_up.item"]},"minItems":4,"maxItems":4}},"safe_ess.skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","safe_alloy.mana_skill","base_add.speed","cap_up.item"]},"minItems":4,"maxItems":4}},"base_add.speed":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.speed","safe_alloy.mana_skill","safe_ess.skill","cap_up.item"]},"minItems":4,"maxItems":4}}},"required":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"safe_alloy.mana_skill":[["safe_alloy.mana_skill","cap_up.item","safe_ess.skill","base_add.speed"],["safe_alloy.mana_skill","cap_up.item","safe_ess.skill","base_add.speed"]],"safe_ess.skill":[["safe_ess.skill","cap_up.item","safe_alloy.mana_skill","base_add.speed"],["safe_ess.skill","cap_up.item","safe_alloy.mana_skill","base_add.speed"]],"base_add.speed":[["base_add.speed","cap_up.item","safe_alloy.mana_skill","safe_ess.skill"],["base_add.speed","cap_up.item","safe_ess.skill","safe_alloy.mana_skill"]]}
```

누락된 유효 경로

```json
["safe_alloy.mana_skill base_add.speed safe_ess.skill","safe_ess.skill base_add.speed safe_alloy.mana_skill"]
```

### 요청 2 · H2 · base

문안 P1. 기대 H2. 회수 1/2. 거절 5개, 유효 중복 0개. 입력/출력 694/97토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"strategies":{"cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare"},"rules":[],"slots":{"craftedUsed":1,"limitNow":1,"additionalCraftedNeededAtLeast":1,"expandBy":1,"desecratedUsed":0},"requiredRows":["skill","speed"],"afterFirst":{"safe_ess.skill":{"speed":["base_add.speed"]},"base_add.speed":{"skill":["safe_ess.skill"]}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"safe_ess.skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","base_add.speed","cap_up.item"]},"minItems":3,"maxItems":3}},"base_add.speed":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.speed","safe_ess.skill","cap_up.item"]},"minItems":3,"maxItems":3}}},"required":["safe_ess.skill","base_add.speed"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"safe_ess.skill":[["safe_ess.skill","cap_up.item","base_add.speed"],["safe_ess.skill","base_add.speed","cap_up.item"],["safe_ess.skill","base_add.speed","base_add.speed"]],"base_add.speed":[["base_add.speed","cap_up.item","safe_ess.skill"],["base_add.speed","safe_ess.skill","cap_up.item"],["base_add.speed","safe_ess.skill","safe_ess.skill"]]}
```

누락된 유효 경로

```json
["safe_ess.skill base_add.speed"]
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["safe_ess.skill","cap_up.item","base_add.speed"],"Crafted capacity exhausted"],[["safe_ess.skill","base_add.speed","cap_up.item"],"Crafted capacity exhausted"],[["safe_ess.skill","base_add.speed","base_add.speed"],"Crafted capacity exhausted"],[["base_add.speed","safe_ess.skill","cap_up.item"],"Crafted capacity exhausted"],[["base_add.speed","safe_ess.skill","safe_ess.skill"],"Crafted capacity exhausted"]]
```

</details>

### 요청 3 · N · base

문안 P1. 기대 N. 회수 6/7. 거절 2개, 유효 중복 0개. 입력/출력 1038/139토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"strategies":{"first_chaos":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함. 첫 획득에서만. 분열 보존.","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","whittle_miss":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략. 같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."},"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":1,"expandBy":0,"desecratedUsed":0},"requiredRows":["life","def"],"afterFirst":{"first_chaos.life":{"def":["safe_ess.def"]},"base_add.life":{"def":["safe_ess.def"]},"reveal.life":{"def":["safe_ess.def"]},"safe_ess.def":{"life":["base_add.life","reveal.life"]}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"first_chaos.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","safe_ess.def"]},"minItems":2,"maxItems":2}},"base_add.life":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","safe_ess.def"]},"minItems":2,"maxItems":2}},"onMiss":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","safe_ess.def","whittle_miss.life"]},"minItems":3,"maxItems":3}}},"required":["hit","onMiss"],"additionalProperties":false},"reveal.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["reveal.life","safe_ess.def"]},"minItems":2,"maxItems":2}},"safe_ess.def":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def","base_add.life","reveal.life"]},"minItems":2,"maxItems":2}},"onMiss":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def","base_add.life","whittle_miss.life"]},"minItems":3,"maxItems":3}}},"required":["hit","onMiss"],"additionalProperties":false}},"required":["first_chaos.life","base_add.life","reveal.life","safe_ess.def"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"first_chaos.life":[["first_chaos.life","safe_ess.def"]],"base_add.life":{"hit":[["base_add.life","safe_ess.def"]],"onMiss":[["base_add.life","whittle_miss.life","safe_ess.def"]]},"reveal.life":[["reveal.life","safe_ess.def"]],"safe_ess.def":{"hit":[["safe_ess.def","base_add.life"],["safe_ess.def","reveal.life"]],"onMiss":[["safe_ess.def","whittle_miss.life","base_add.life"],["safe_ess.def","whittle_miss.life","safe_ess.def"]]} }
```

누락된 유효 경로

```json
["safe_ess.def base_add.life whittle_miss.life"]
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["safe_ess.def","whittle_miss.life","base_add.life"],"recovery requires preceding base_add on same target"],[["safe_ess.def","whittle_miss.life","safe_ess.def"],"recovery requires preceding base_add on same target"]]
```

</details>

### 요청 4 · R · base

문안 P1. 기대 R. 회수 6/6. 거절 0개, 유효 중복 1개. 입력/출력 790/93토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"cold":["base","Preserved Collarbone"]}},"strategies":{"base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다."},"rules":["이 요청은 엑잘과 훼손 획득 경로를 비교한다. 확보한 목표를 제거하지 않는다."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":0},"requiredRows":["life","cold"],"afterFirst":{"base_add.life":{"cold":["base_add.cold","reveal.cold"]},"reveal.life":{"cold":["base_add.cold"]},"base_add.cold":{"life":["base_add.life","reveal.life"]},"reveal.cold":{"life":["base_add.life"]}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"base_add.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","base_add.cold","reveal.cold"]},"minItems":2,"maxItems":2}},"reveal.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["reveal.life","base_add.cold"]},"minItems":2,"maxItems":2}},"base_add.cold":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.cold","base_add.life","reveal.life"]},"minItems":2,"maxItems":2}},"reveal.cold":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["reveal.cold","base_add.life"]},"minItems":2,"maxItems":2}}},"required":["base_add.life","reveal.life","base_add.cold","reveal.cold"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"base_add.life":[["base_add.life","base_add.cold"],["base_add.life","reveal.cold"],["base_add.life","reveal.cold"]],"reveal.life":[["reveal.life","base_add.cold"]],"base_add.cold":[["base_add.cold","base_add.life"],["base_add.cold","reveal.life"]],"reveal.cold":[["reveal.cold","base_add.life"]]}
```

### 요청 5 · G · base

문안 P1. 기대 G. 회수 2/2. 거절 0개, 유효 중복 0개. 입력/출력 684/35토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Stocky Mitts","rarity":"magic","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"filler"}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body"],"pen":["Prismatic Alloy"]},"suffix":{}},"strategies":{"upgrade_ess":"등급 상승용 에센스로 목표 일반 옵션 획득. Magic → Rare","regal_base":"제왕으로 Magic을 Rare로 올리며 해당 일반 옵션이 뜬 결과 채택. Magic → Rare","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요"},"rules":[],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":1,"expandBy":0,"desecratedUsed":0},"requiredRows":["life","pen"],"afterFirst":{"upgrade_ess.life":{"pen":["safe_alloy.pen"]},"regal_base.life":{"pen":["safe_alloy.pen"]}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"upgrade_ess.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["upgrade_ess.life","safe_alloy.pen"]},"minItems":2,"maxItems":2}},"regal_base.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["regal_base.life","safe_alloy.pen"]},"minItems":2,"maxItems":2}}},"required":["upgrade_ess.life","regal_base.life"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"upgrade_ess.life":[["upgrade_ess.life","safe_alloy.pen"]],"regal_base.life":[["regal_base.life","safe_alloy.pen"]]}
```

### 요청 6 · Q2 · base

문안 P1. 기대 Q2. 회수 2/2. 거절 2개, 유효 중복 0개. 입력/출력 713/83토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"]}},"strategies":{"safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요","cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item."},"rules":[],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":2,"expandBy":1,"desecratedUsed":0},"requiredRows":["mana_skill","skill"],"afterFirst":{"safe_alloy.mana_skill":{"skill":["safe_ess.skill"]},"safe_ess.skill":{"mana_skill":["safe_alloy.mana_skill"]}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"safe_alloy.mana_skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill","cap_up.item"]},"minItems":3,"maxItems":3}},"safe_ess.skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","safe_alloy.mana_skill","cap_up.item"]},"minItems":3,"maxItems":3}}},"required":["safe_alloy.mana_skill","safe_ess.skill"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"safe_alloy.mana_skill":[["safe_alloy.mana_skill","cap_up.item","safe_ess.skill"],["safe_alloy.mana_skill","safe_ess.skill","cap_up.item"]],"safe_ess.skill":[["safe_ess.skill","cap_up.item","safe_alloy.mana_skill"],["safe_ess.skill","safe_alloy.mana_skill","cap_up.item"]]}
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["safe_alloy.mana_skill","safe_ess.skill","cap_up.item"],"Crafted capacity exhausted"],[["safe_ess.skill","safe_alloy.mana_skill","cap_up.item"],"Crafted capacity exhausted"]]
```

</details>

### 요청 7 · W · base

문안 P1. 기대 W. 회수 4/6. 거절 0개, 유효 중복 2개. 입력/출력 860/146토큰.

전송 JSON은 요청 1과 동일하다.

응답 스키마은 요청 1과 동일하다.

실제 응답 원문

```json
{"safe_alloy.mana_skill":[["safe_alloy.mana_skill","cap_up.item","safe_ess.skill","base_add.speed"],["safe_alloy.mana_skill","cap_up.item","safe_ess.skill","base_add.speed"]],"safe_ess.skill":[["safe_ess.skill","cap_up.item","safe_alloy.mana_skill","base_add.speed"],["safe_ess.skill","cap_up.item","safe_alloy.mana_skill","base_add.speed"]],"base_add.speed":[["base_add.speed","cap_up.item","safe_alloy.mana_skill","safe_ess.skill"],["base_add.speed","cap_up.item","safe_ess.skill","safe_alloy.mana_skill"]]}
```

누락된 유효 경로

```json
["safe_alloy.mana_skill base_add.speed safe_ess.skill","safe_ess.skill base_add.speed safe_alloy.mana_skill"]
```

### 요청 8 · H2 · base

문안 P1. 기대 H2. 회수 1/2. 거절 5개, 유효 중복 0개. 입력/출력 694/97토큰.

전송 JSON은 요청 2과 동일하다.

응답 스키마은 요청 2과 동일하다.

실제 응답 원문

```json
{"safe_ess.skill":[["safe_ess.skill","cap_up.item","base_add.speed"],["safe_ess.skill","base_add.speed","cap_up.item"],["safe_ess.skill","base_add.speed","base_add.speed"]],"base_add.speed":[["base_add.speed","cap_up.item","safe_ess.skill"],["base_add.speed","safe_ess.skill","cap_up.item"],["base_add.speed","safe_ess.skill","safe_ess.skill"]]}
```

누락된 유효 경로

```json
["safe_ess.skill base_add.speed"]
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["safe_ess.skill","cap_up.item","base_add.speed"],"Crafted capacity exhausted"],[["safe_ess.skill","base_add.speed","cap_up.item"],"Crafted capacity exhausted"],[["safe_ess.skill","base_add.speed","base_add.speed"],"Crafted capacity exhausted"],[["base_add.speed","safe_ess.skill","cap_up.item"],"Crafted capacity exhausted"],[["base_add.speed","safe_ess.skill","safe_ess.skill"],"Crafted capacity exhausted"]]
```

</details>

### 요청 9 · N · base

문안 P1. 기대 N. 회수 5/7. 거절 1개, 유효 중복 0개. 입력/출력 1038/111토큰.

전송 JSON은 요청 3과 동일하다.

응답 스키마은 요청 3과 동일하다.

실제 응답 원문

```json
{"first_chaos.life":[["first_chaos.life","safe_ess.def"]],"base_add.life":{"hit":[["base_add.life","safe_ess.def"]],"onMiss":[["base_add.life","whittle_miss.life","safe_ess.def"]]},"reveal.life":[["reveal.life","safe_ess.def"]],"safe_ess.def":{"hit":[["safe_ess.def","base_add.life"]],"onMiss":[["safe_ess.def","whittle_miss.life","base_add.life"]]}}
```

누락된 유효 경로

```json
["safe_ess.def base_add.life whittle_miss.life","safe_ess.def reveal.life"]
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["safe_ess.def","whittle_miss.life","base_add.life"],"recovery requires preceding base_add on same target"]]
```

</details>

### 요청 10 · R · base

문안 P1. 기대 R. 회수 6/6. 거절 0개, 유효 중복 1개. 입력/출력 790/93토큰.

전송 JSON은 요청 4과 동일하다.

응답 스키마은 요청 4과 동일하다.

실제 응답 원문

```json
{"base_add.life":[["base_add.life","base_add.cold"],["base_add.life","reveal.cold"],["base_add.life","reveal.cold"]],"reveal.life":[["reveal.life","base_add.cold"]],"base_add.cold":[["base_add.cold","base_add.life"],["base_add.cold","reveal.life"]],"reveal.cold":[["reveal.cold","base_add.life"]]}
```

### 요청 11 · G · base

문안 P1. 기대 G. 회수 2/2. 거절 0개, 유효 중복 0개. 입력/출력 684/35토큰.

전송 JSON은 요청 5과 동일하다.

응답 스키마은 요청 5과 동일하다.

실제 응답 원문

```json
{"upgrade_ess.life":[["upgrade_ess.life","safe_alloy.pen"]],"regal_base.life":[["regal_base.life","safe_alloy.pen"]]}
```

### 요청 12 · Q2 · base

문안 P1. 기대 Q2. 회수 2/2. 거절 4개, 유효 중복 2개. 입력/출력 713/151토큰.

전송 JSON은 요청 6과 동일하다.

응답 스키마은 요청 6과 동일하다.

실제 응답 원문

```json
{"safe_alloy.mana_skill":[["safe_alloy.mana_skill","cap_up.item","safe_ess.skill"],["safe_alloy.mana_skill","safe_ess.skill","cap_up.item"],["safe_alloy.mana_skill","cap_up.item","safe_ess.skill"],["safe_alloy.mana_skill","safe_ess.skill","cap_up.item"]],"safe_ess.skill":[["safe_ess.skill","cap_up.item","safe_alloy.mana_skill"],["safe_ess.skill","safe_alloy.mana_skill","cap_up.item"],["safe_ess.skill","cap_up.item","safe_alloy.mana_skill"],["safe_ess.skill","safe_alloy.mana_skill","cap_up.item"]]}
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["safe_alloy.mana_skill","safe_ess.skill","cap_up.item"],"Crafted capacity exhausted"],[["safe_alloy.mana_skill","safe_ess.skill","cap_up.item"],"Crafted capacity exhausted"],[["safe_ess.skill","safe_alloy.mana_skill","cap_up.item"],"Crafted capacity exhausted"],[["safe_ess.skill","safe_alloy.mana_skill","cap_up.item"],"Crafted capacity exhausted"]]
```

</details>

### 요청 13 · W · pos

문안 P2. 기대 W. 회수 2/6. 거절 2개, 유효 중복 0개. 입력/출력 838/88토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"needOnePerRow":{"mana_skill":["safe_alloy.mana_skill"],"skill":["safe_ess.skill"],"speed":["base_add.speed"]},"strategies":{"safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item."},"rules":["mana_skill과 skill은 서로 다른 Crafted. speed는 일반 접미. 세 옵션 모두 보존. 두 번째 Crafted 전 한도 확장 필수."],"pivot":"speed","helpers":["cap_up.item"],"coverage":{"pivot":"speed","unseenPositions":[2],"unseenActions":[]}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"at1":{"type":"array","items":{"type":"object","properties":{"prepare":{"type":"array","items":{"type":"string","enum":["cap_up.item"]}},"steps":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed","cap_up.item"]}}},"required":["prepare","steps"],"additionalProperties":false}},"at2":{"type":"array","items":{"type":"object","properties":{"prepare":{"type":"array","items":{"type":"string","enum":["cap_up.item"]}},"steps":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed","cap_up.item"]}}},"required":["prepare","steps"],"additionalProperties":false}},"at3":{"type":"array","items":{"type":"object","properties":{"prepare":{"type":"array","items":{"type":"string","enum":["cap_up.item"]}},"steps":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed","cap_up.item"]}}},"required":["prepare","steps"],"additionalProperties":false}}},"required":["at1","at2","at3"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"at1":[{"prepare":["cap_up.item"],"steps":["base_add.speed"]}],"at2":[{"prepare":["cap_up.item"],"steps":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed"]},{"prepare":["cap_up.item"],"steps":["safe_ess.skill","safe_alloy.mana_skill","base_add.speed"]}],"at3":[{"prepare":[],"steps":[]}]}
```

누락된 유효 경로

```json
["base_add.speed safe_alloy.mana_skill safe_ess.skill","base_add.speed safe_ess.skill safe_alloy.mana_skill","safe_alloy.mana_skill base_add.speed safe_ess.skill","safe_ess.skill base_add.speed safe_alloy.mana_skill"]
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["cap_up.item","base_add.speed"],"missing mana_skill,skill"],[[],"missing mana_skill,skill,speed"]]
```

</details>

### 요청 14 · H2 · pos

문안 P2. 기대 H2. 회수 2/2. 거절 2개, 유효 중복 0개. 입력/출력 683/95토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"needOnePerRow":{"skill":["safe_ess.skill"],"speed":["base_add.speed"]},"strategies":{"cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare"},"rules":[],"pivot":"speed","helpers":["cap_up.item"],"coverage":{"pivot":"speed","unseenPositions":[2],"unseenActions":[]}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"at1":{"type":"array","items":{"type":"object","properties":{"prepare":{"type":"array","items":{"type":"string","enum":["cap_up.item"]}},"steps":{"type":"array","items":{"type":"string","enum":["cap_up.item","safe_ess.skill","base_add.speed"]}}},"required":["prepare","steps"],"additionalProperties":false}},"at2":{"type":"array","items":{"type":"object","properties":{"prepare":{"type":"array","items":{"type":"string","enum":["cap_up.item"]}},"steps":{"type":"array","items":{"type":"string","enum":["cap_up.item","safe_ess.skill","base_add.speed"]}}},"required":["prepare","steps"],"additionalProperties":false}}},"required":["at1","at2"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"at1":[{"prepare":["cap_up.item"],"steps":["base_add.speed","safe_ess.skill"]},{"prepare":["cap_up.item"],"steps":["safe_ess.skill","base_add.speed"]}],"at2":[{"prepare":["cap_up.item","cap_up.item"],"steps":["safe_ess.skill","base_add.speed"]},{"prepare":["cap_up.item","cap_up.item"],"steps":["base_add.speed","safe_ess.skill"]}]}
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["cap_up.item","cap_up.item","safe_ess.skill","base_add.speed"],"no socket for capacity"],[["cap_up.item","cap_up.item","base_add.speed","safe_ess.skill"],"no socket for capacity"]]
```

</details>

### 요청 15 · N · pos

문안 P2. 기대 N. 회수 2/7. 거절 4개, 유효 중복 0개. 입력/출력 858/161토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"needOnePerRow":{"life":["first_chaos.life","base_add.life","reveal.life"],"def":["safe_ess.def"]},"strategies":{"first_chaos":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함. 첫 획득에서만. 분열 보존.","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","whittle_miss":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략. 같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."},"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"pivot":"life","helpers":["whittle_miss.life"],"coverage":{"pivot":"life","unseenPositions":[],"unseenActions":[]}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"at1":{"type":"array","items":{"type":"object","properties":{"prepare":{"type":"array","items":{"type":"string"}},"steps":{"type":"array","items":{"type":"string","enum":["first_chaos.life","base_add.life","reveal.life","safe_ess.def","whittle_miss.life"]}}},"required":["prepare","steps"],"additionalProperties":false}},"at2":{"type":"array","items":{"type":"object","properties":{"prepare":{"type":"array","items":{"type":"string"}},"steps":{"type":"array","items":{"type":"string","enum":["first_chaos.life","base_add.life","reveal.life","safe_ess.def","whittle_miss.life"]}}},"required":["prepare","steps"],"additionalProperties":false}}},"required":["at1","at2"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"at1":[{"prepare":["first_chaos.life"],"steps":["first_chaos.life","base_add.life","safe_ess.def"]},{"prepare":["first_chaos.life"],"steps":["first_chaos.life","reveal.life","safe_ess.def"]},{"prepare":["first_chaos.life"],"steps":["first_chaos.life","base_add.life","whittle_miss.life","safe_ess.def"]},{"prepare":["first_chaos.life"],"steps":["first_chaos.life","reveal.life","whittle_miss.life","safe_ess.def"]}],"at2":[{"prepare":["safe_ess.def"],"steps":["base_add.life"]},{"prepare":["safe_ess.def"],"steps":["reveal.life"]}]}
```

누락된 유효 경로

```json
["base_add.life safe_ess.def","base_add.life whittle_miss.life safe_ess.def","first_chaos.life safe_ess.def","reveal.life safe_ess.def","safe_ess.def base_add.life whittle_miss.life"]
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["first_chaos.life","first_chaos.life","base_add.life","safe_ess.def"],"target already covered"],[["first_chaos.life","first_chaos.life","reveal.life","safe_ess.def"],"target already covered"],[["first_chaos.life","first_chaos.life","base_add.life","whittle_miss.life","safe_ess.def"],"target already covered"],[["first_chaos.life","first_chaos.life","reveal.life","whittle_miss.life","safe_ess.def"],"target already covered"]]
```

</details>

### 요청 16 · R · pos

문안 P2. 기대 R. 회수 0/6. 거절 5개, 유효 중복 0개. 입력/출력 678/127토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"cold":["base","Preserved Collarbone"]}},"needOnePerRow":{"life":["base_add.life","reveal.life"],"cold":["base_add.cold","reveal.cold"]},"strategies":{"base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다."},"rules":["이 요청은 엑잘과 훼손 획득 경로를 비교한다. 확보한 목표를 제거하지 않는다."],"pivot":"life","helpers":[],"coverage":{"pivot":"life","unseenPositions":[],"unseenActions":[]}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"at1":{"type":"array","items":{"type":"object","properties":{"prepare":{"type":"array","items":{"type":"string"}},"steps":{"type":"array","items":{"type":"string","enum":["base_add.life","reveal.life","base_add.cold","reveal.cold"]}}},"required":["prepare","steps"],"additionalProperties":false}},"at2":{"type":"array","items":{"type":"object","properties":{"prepare":{"type":"array","items":{"type":"string"}},"steps":{"type":"array","items":{"type":"string","enum":["base_add.life","reveal.life","base_add.cold","reveal.cold"]}}},"required":["prepare","steps"],"additionalProperties":false}}},"required":["at1","at2"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"at1":[{"prepare":["cap_up.item"],"steps":["base_add.life","reveal.life","base_add.cold","reveal.cold"]},{"prepare":["cap_up.item"],"steps":["reveal.life","base_add.life","base_add.cold","reveal.cold"]},{"prepare":[],"steps":["base_add.life","reveal.life","reveal.cold","base_add.cold"]}],"at2":[{"prepare":["cap_up.item"],"steps":["base_add.cold","reveal.cold"]},{"prepare":[],"steps":["reveal.cold","base_add.cold"]}]}
```

누락된 유효 경로

```json
["base_add.cold base_add.life","base_add.cold reveal.life","base_add.life base_add.cold","base_add.life reveal.cold","reveal.cold base_add.life","reveal.life base_add.cold"]
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["cap_up.item","base_add.life","reveal.life","base_add.cold","reveal.cold"],"unknown action"],[["cap_up.item","reveal.life","base_add.life","base_add.cold","reveal.cold"],"unknown action"],[["base_add.life","reveal.life","reveal.cold","base_add.cold"],"target already covered"],[["cap_up.item","base_add.cold","reveal.cold"],"unknown action"],[["reveal.cold","base_add.cold"],"target already covered"]]
```

</details>

### 요청 17 · G · pos

문안 P2. 기대 G. 회수 0/2. 거절 4개, 유효 중복 0개. 입력/출력 668/95토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Stocky Mitts","rarity":"magic","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"filler"}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body"],"pen":["Prismatic Alloy"]},"suffix":{}},"needOnePerRow":{"life":["upgrade_ess.life","regal_base.life"],"pen":["safe_alloy.pen"]},"strategies":{"upgrade_ess":"등급 상승용 에센스로 목표 일반 옵션 획득. Magic → Rare","regal_base":"제왕으로 Magic을 Rare로 올리며 해당 일반 옵션이 뜬 결과 채택. Magic → Rare","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요"},"rules":[],"pivot":"life","helpers":[],"coverage":{"pivot":"life","unseenPositions":[2],"unseenActions":[]}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"at1":{"type":"array","items":{"type":"object","properties":{"prepare":{"type":"array","items":{"type":"string"}},"steps":{"type":"array","items":{"type":"string","enum":["upgrade_ess.life","regal_base.life","safe_alloy.pen"]}}},"required":["prepare","steps"],"additionalProperties":false}},"at2":{"type":"array","items":{"type":"object","properties":{"prepare":{"type":"array","items":{"type":"string"}},"steps":{"type":"array","items":{"type":"string","enum":["upgrade_ess.life","regal_base.life","safe_alloy.pen"]}}},"required":["prepare","steps"],"additionalProperties":false}}},"required":["at1","at2"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"at1":[{"prepare":["upgrade_ess.life"],"steps":["regal_base.life","safe_alloy.pen"]},{"prepare":["regal_base.life"],"steps":["upgrade_ess.life","safe_alloy.pen"]}],"at2":[{"prepare":["upgrade_ess.life","regal_base.life"],"steps":["safe_alloy.pen"]},{"prepare":["regal_base.life","upgrade_ess.life"],"steps":["safe_alloy.pen"]}]}
```

누락된 유효 경로

```json
["regal_base.life safe_alloy.pen","upgrade_ess.life safe_alloy.pen"]
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["upgrade_ess.life","regal_base.life","safe_alloy.pen"],"target already covered"],[["regal_base.life","upgrade_ess.life","safe_alloy.pen"],"target already covered"],[["upgrade_ess.life","regal_base.life","safe_alloy.pen"],"target already covered"],[["regal_base.life","upgrade_ess.life","safe_alloy.pen"],"target already covered"]]
```

</details>

### 요청 18 · Q2 · pos

문안 P2. 기대 Q2. 회수 2/2. 거절 0개, 유효 중복 2개. 입력/출력 696/99토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"]}},"needOnePerRow":{"mana_skill":["safe_alloy.mana_skill"],"skill":["safe_ess.skill"]},"strategies":{"safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요","cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item."},"rules":[],"pivot":"mana_skill","helpers":["cap_up.item"],"coverage":{"pivot":"mana_skill","unseenPositions":[],"unseenActions":[]}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"at1":{"type":"array","items":{"type":"object","properties":{"prepare":{"type":"array","items":{"type":"string","enum":["cap_up.item"]}},"steps":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","safe_alloy.mana_skill","cap_up.item"]}}},"required":["prepare","steps"],"additionalProperties":false}},"at2":{"type":"array","items":{"type":"object","properties":{"prepare":{"type":"array","items":{"type":"string","enum":["cap_up.item"]}},"steps":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","safe_alloy.mana_skill","cap_up.item"]}}},"required":["prepare","steps"],"additionalProperties":false}}},"required":["at1","at2"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"at1":[{"prepare":["cap_up.item"],"steps":["safe_alloy.mana_skill","safe_ess.skill"]},{"prepare":["cap_up.item"],"steps":["safe_ess.skill","safe_alloy.mana_skill"]}],"at2":[{"prepare":["cap_up.item"],"steps":["safe_ess.skill","safe_alloy.mana_skill"]},{"prepare":["cap_up.item"],"steps":["safe_alloy.mana_skill","safe_ess.skill"]}]}
```

### 요청 19 · W · alt

문안 P3. 기대 W. 회수 2/6. 거절 1개, 유효 중복 0개. 입력/출력 1069/192토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"needOnePerRow":{"mana_skill":["safe_alloy.mana_skill"],"skill":["safe_ess.skill"],"speed":["base_add.speed"]},"strategies":{"safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item."},"rules":["mana_skill과 skill은 서로 다른 Crafted. speed는 일반 접미. 세 옵션 모두 보존. 두 번째 Crafted 전 한도 확장 필수."],"pivot":"speed","helpers":["cap_up.item"]}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"at1":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"object","properties":{"setup":{"type":"string","enum":["none","cap_up.item"]},"use":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill"]}},"required":["setup","use"],"additionalProperties":false},"minItems":0,"maxItems":0},"take":{"type":"object","properties":{"setup":{"type":"string","enum":["none","cap_up.item"]},"use":{"type":"string","enum":["base_add.speed"]}},"required":["setup","use"],"additionalProperties":false},"after":{"type":"array","items":{"type":"object","properties":{"setup":{"type":"string","enum":["none","cap_up.item"]},"use":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill"]}},"required":["setup","use"],"additionalProperties":false},"minItems":2,"maxItems":2}},"required":["before","take","after"],"additionalProperties":false}},"at2":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"object","properties":{"setup":{"type":"string","enum":["none","cap_up.item"]},"use":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill"]}},"required":["setup","use"],"additionalProperties":false},"minItems":1,"maxItems":1},"take":{"type":"object","properties":{"setup":{"type":"string","enum":["none","cap_up.item"]},"use":{"type":"string","enum":["base_add.speed"]}},"required":["setup","use"],"additionalProperties":false},"after":{"type":"array","items":{"type":"object","properties":{"setup":{"type":"string","enum":["none","cap_up.item"]},"use":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill"]}},"required":["setup","use"],"additionalProperties":false},"minItems":1,"maxItems":1}},"required":["before","take","after"],"additionalProperties":false}},"at3":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"object","properties":{"setup":{"type":"string","enum":["none","cap_up.item"]},"use":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill"]}},"required":["setup","use"],"additionalProperties":false},"minItems":2,"maxItems":2},"take":{"type":"object","properties":{"setup":{"type":"string","enum":["none","cap_up.item"]},"use":{"type":"string","enum":["base_add.speed"]}},"required":["setup","use"],"additionalProperties":false},"after":{"type":"array","items":{"type":"object","properties":{"setup":{"type":"string","enum":["none","cap_up.item"]},"use":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill"]}},"required":["setup","use"],"additionalProperties":false},"minItems":0,"maxItems":0}},"required":["before","take","after"],"additionalProperties":false}}},"required":["at1","at2","at3"],"additionalProperties":false}
```

</details>

실제 응답 원문 (탭과 줄바꿈을 보존한 JSON 문자열)

```json
{"raw":"{\"at1\":[{\"before\":[\n \t\t\t\t\n \t\t],\"take\":{\"setup\":\"cap_up.item\",\"use\":\"base_add.speed\"},\"after\":[\n \t\t\t{\"setup\":\"none\",\"use\":\"safe_alloy.mana_skill\"},\n \t\t\t{\"setup\":\"none\",\"use\":\"safe_ess.skill\"}\n \t\t]}],\"at2\":[{\"before\":[\n \t\t\t{\"setup\":\"none\",\"use\":\"safe_alloy.mana_skill\"}\n \t\t],\"take\":{\"setup\":\"cap_up.item\",\"use\":\"base_add.speed\"},\"after\":[\n \t\t\t{\"setup\":\"none\",\"use\":\"safe_ess.skill\"}\n \t\t]}],\"at3\":[{\"before\":[\n \t\t\t{\"setup\":\"none\",\"use\":\"safe_ess.skill\"}\n \t\t\t,{\"setup\":\"none\",\"use\":\"safe_alloy.mana_skill\"}\n \t\t],\"take\":{\"setup\":\"cap_up.item\",\"use\":\"base_add.speed\"},\"after\":[]}]}"}
```

누락된 유효 경로

```json
["base_add.speed safe_ess.skill safe_alloy.mana_skill","safe_alloy.mana_skill safe_ess.skill base_add.speed","safe_ess.skill base_add.speed safe_alloy.mana_skill","safe_ess.skill safe_alloy.mana_skill base_add.speed"]
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["safe_ess.skill","safe_alloy.mana_skill","cap_up.item","base_add.speed"],"Crafted capacity exhausted"]]
```

</details>

### 요청 20 · H2 · alt

문안 P3. 기대 H2. 회수 0/2. 거절 2개, 유효 중복 0개. 입력/출력 815/73토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"needOnePerRow":{"skill":["safe_ess.skill"],"speed":["base_add.speed"]},"strategies":{"cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare"},"rules":[],"pivot":"speed","helpers":["cap_up.item"]}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"at1":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"object","properties":{"setup":{"type":"string","enum":["none","cap_up.item"]},"use":{"type":"string","enum":["safe_ess.skill"]}},"required":["setup","use"],"additionalProperties":false},"minItems":0,"maxItems":0},"take":{"type":"object","properties":{"setup":{"type":"string","enum":["none","cap_up.item"]},"use":{"type":"string","enum":["base_add.speed"]}},"required":["setup","use"],"additionalProperties":false},"after":{"type":"array","items":{"type":"object","properties":{"setup":{"type":"string","enum":["none","cap_up.item"]},"use":{"type":"string","enum":["safe_ess.skill"]}},"required":["setup","use"],"additionalProperties":false},"minItems":1,"maxItems":1}},"required":["before","take","after"],"additionalProperties":false}},"at2":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"object","properties":{"setup":{"type":"string","enum":["none","cap_up.item"]},"use":{"type":"string","enum":["safe_ess.skill"]}},"required":["setup","use"],"additionalProperties":false},"minItems":1,"maxItems":1},"take":{"type":"object","properties":{"setup":{"type":"string","enum":["none","cap_up.item"]},"use":{"type":"string","enum":["base_add.speed"]}},"required":["setup","use"],"additionalProperties":false},"after":{"type":"array","items":{"type":"object","properties":{"setup":{"type":"string","enum":["none","cap_up.item"]},"use":{"type":"string","enum":["safe_ess.skill"]}},"required":["setup","use"],"additionalProperties":false},"minItems":0,"maxItems":0}},"required":["before","take","after"],"additionalProperties":false}}},"required":["at1","at2"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"at1":[{"before":[],"take":{"setup":"none","use":"base_add.speed"},"after":[{"setup":"none","use":"safe_ess.skill"}]}],"at2":[{"before":[{"setup":"none","use":"safe_ess.skill"}],"take":{"setup":"none","use":"base_add.speed"},"after":[]}]}
```

누락된 유효 경로

```json
["base_add.speed safe_ess.skill","safe_ess.skill base_add.speed"]
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["base_add.speed","safe_ess.skill"],"Crafted capacity exhausted"],[["safe_ess.skill","base_add.speed"],"Crafted capacity exhausted"]]
```

</details>

### 요청 21 · N · alt

문안 P3. 기대 N. 회수 3/7. 거절 1개, 유효 중복 0개. 입력/출력 911/119토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"needOnePerRow":{"life":["first_chaos.life","base_add.life","reveal.life"],"def":["safe_ess.def"]},"strategies":{"first_chaos":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함. 첫 획득에서만. 분열 보존.","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","whittle_miss":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략. 같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."},"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"pivot":"life","helpers":["whittle_miss.life"]}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"at1":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["safe_ess.def"]},"minItems":0,"maxItems":0},"take":{"type":"object","properties":{"use":{"type":"string","enum":["first_chaos.life","base_add.life","reveal.life"]},"ifMiss":{"type":"string","enum":["none","whittle_miss.life"]}},"required":["use","ifMiss"],"additionalProperties":false},"after":{"type":"array","items":{"type":"string","enum":["safe_ess.def"]},"minItems":1,"maxItems":1}},"required":["before","take","after"],"additionalProperties":false}},"at2":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["safe_ess.def"]},"minItems":1,"maxItems":1},"take":{"type":"object","properties":{"use":{"type":"string","enum":["first_chaos.life","base_add.life","reveal.life"]},"ifMiss":{"type":"string","enum":["none","whittle_miss.life"]}},"required":["use","ifMiss"],"additionalProperties":false},"after":{"type":"array","items":{"type":"string","enum":["safe_ess.def"]},"minItems":0,"maxItems":0}},"required":["before","take","after"],"additionalProperties":false}}},"required":["at1","at2"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"at1":[{"before":[],"take":{"use":"first_chaos.life","ifMiss":"none"},"after":["safe_ess.def"]},{"before":[],"take":{"use":"base_add.life","ifMiss":"whittle_miss.life"},"after":["safe_ess.def"]},{"before":[],"take":{"use":"reveal.life","ifMiss":"none"},"after":["safe_ess.def"]}],"at2":[{"before":["safe_ess.def"],"take":{"use":"first_chaos.life","ifMiss":"none"},"after":[]}]}
```

누락된 유효 경로

```json
["base_add.life safe_ess.def","safe_ess.def base_add.life","safe_ess.def base_add.life whittle_miss.life","safe_ess.def reveal.life"]
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["safe_ess.def","first_chaos.life"],"opening acquisition only"]]
```

</details>

### 요청 22 · R · alt

문안 P3. 기대 R. 회수 4/6. 거절 0개, 유효 중복 0개. 입력/출력 699/79토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"cold":["base","Preserved Collarbone"]}},"needOnePerRow":{"life":["base_add.life","reveal.life"],"cold":["base_add.cold","reveal.cold"]},"strategies":{"base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다."},"rules":["이 요청은 엑잘과 훼손 획득 경로를 비교한다. 확보한 목표를 제거하지 않는다."],"pivot":"life","helpers":[]}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"at1":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["base_add.cold","reveal.cold"]},"minItems":0,"maxItems":0},"take":{"type":"string","enum":["base_add.life","reveal.life"]},"after":{"type":"array","items":{"type":"string","enum":["base_add.cold","reveal.cold"]},"minItems":1,"maxItems":1}},"required":["before","take","after"],"additionalProperties":false}},"at2":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["base_add.cold","reveal.cold"]},"minItems":1,"maxItems":1},"take":{"type":"string","enum":["base_add.life","reveal.life"]},"after":{"type":"array","items":{"type":"string","enum":["base_add.cold","reveal.cold"]},"minItems":0,"maxItems":0}},"required":["before","take","after"],"additionalProperties":false}}},"required":["at1","at2"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"at1":[{"before":[],"take":"base_add.life","after":["reveal.cold"]},{"before":[],"take":"reveal.life","after":["base_add.cold"]}],"at2":[{"before":["base_add.cold"],"take":"base_add.life","after":[]},{"before":["reveal.cold"],"take":"base_add.life","after":[]}]}
```

누락된 유효 경로

```json
["base_add.cold reveal.life","base_add.life base_add.cold"]
```

### 요청 23 · G · alt

문안 P3. 기대 G. 회수 2/2. 거절 2개, 유효 중복 0개. 입력/출력 678/83토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Stocky Mitts","rarity":"magic","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"filler"}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body"],"pen":["Prismatic Alloy"]},"suffix":{}},"needOnePerRow":{"life":["upgrade_ess.life","regal_base.life"],"pen":["safe_alloy.pen"]},"strategies":{"upgrade_ess":"등급 상승용 에센스로 목표 일반 옵션 획득. Magic → Rare","regal_base":"제왕으로 Magic을 Rare로 올리며 해당 일반 옵션이 뜬 결과 채택. Magic → Rare","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요"},"rules":[],"pivot":"life","helpers":[]}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"at1":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["safe_alloy.pen"]},"minItems":0,"maxItems":0},"take":{"type":"string","enum":["upgrade_ess.life","regal_base.life"]},"after":{"type":"array","items":{"type":"string","enum":["safe_alloy.pen"]},"minItems":1,"maxItems":1}},"required":["before","take","after"],"additionalProperties":false}},"at2":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["safe_alloy.pen"]},"minItems":1,"maxItems":1},"take":{"type":"string","enum":["upgrade_ess.life","regal_base.life"]},"after":{"type":"array","items":{"type":"string","enum":["safe_alloy.pen"]},"minItems":0,"maxItems":0}},"required":["before","take","after"],"additionalProperties":false}}},"required":["at1","at2"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"at1":[{"before":[],"take":"upgrade_ess.life","after":["safe_alloy.pen"]},{"before":[],"take":"regal_base.life","after":["safe_alloy.pen"]}],"at2":[{"before":["safe_alloy.pen"],"take":"upgrade_ess.life","after":[]},{"before":["safe_alloy.pen"],"take":"regal_base.life","after":[]}]}
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["safe_alloy.pen","upgrade_ess.life"],"Rare required"],[["safe_alloy.pen","regal_base.life"],"Rare required"]]
```

</details>

### 요청 24 · Q2 · alt

문안 P3. 기대 Q2. 회수 2/2. 거절 2개, 유효 중복 0개. 입력/출력 828/152토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"]}},"needOnePerRow":{"mana_skill":["safe_alloy.mana_skill"],"skill":["safe_ess.skill"]},"strategies":{"safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요","cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item."},"rules":[],"pivot":"mana_skill","helpers":["cap_up.item"]}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"at1":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"object","properties":{"setup":{"type":"string","enum":["none","cap_up.item"]},"use":{"type":"string","enum":["safe_ess.skill"]}},"required":["setup","use"],"additionalProperties":false},"minItems":0,"maxItems":0},"take":{"type":"object","properties":{"setup":{"type":"string","enum":["none","cap_up.item"]},"use":{"type":"string","enum":["safe_alloy.mana_skill"]}},"required":["setup","use"],"additionalProperties":false},"after":{"type":"array","items":{"type":"object","properties":{"setup":{"type":"string","enum":["none","cap_up.item"]},"use":{"type":"string","enum":["safe_ess.skill"]}},"required":["setup","use"],"additionalProperties":false},"minItems":1,"maxItems":1}},"required":["before","take","after"],"additionalProperties":false}},"at2":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"object","properties":{"setup":{"type":"string","enum":["none","cap_up.item"]},"use":{"type":"string","enum":["safe_ess.skill"]}},"required":["setup","use"],"additionalProperties":false},"minItems":1,"maxItems":1},"take":{"type":"object","properties":{"setup":{"type":"string","enum":["none","cap_up.item"]},"use":{"type":"string","enum":["safe_alloy.mana_skill"]}},"required":["setup","use"],"additionalProperties":false},"after":{"type":"array","items":{"type":"object","properties":{"setup":{"type":"string","enum":["none","cap_up.item"]},"use":{"type":"string","enum":["safe_ess.skill"]}},"required":["setup","use"],"additionalProperties":false},"minItems":0,"maxItems":0}},"required":["before","take","after"],"additionalProperties":false}}},"required":["at1","at2"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"at1":[{"before":[],"take":{"setup":"none","use":"safe_alloy.mana_skill"},"after":[{"setup":"none","use":"safe_ess.skill"}]},{"before":[],"take":{"setup":"cap_up.item","use":"safe_alloy.mana_skill"},"after":[{"setup":"none","use":"safe_ess.skill"}]}],"at2":[{"before":[{"setup":"none","use":"safe_ess.skill"}],"take":{"setup":"none","use":"safe_alloy.mana_skill"},"after":[]},{"before":[{"setup":"none","use":"safe_ess.skill"}],"take":{"setup":"cap_up.item","use":"safe_alloy.mana_skill"},"after":[]}]}
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["safe_alloy.mana_skill","safe_ess.skill"],"Crafted capacity exhausted"],[["safe_ess.skill","safe_alloy.mana_skill"],"Crafted capacity exhausted"]]
```

</details>

### 요청 25 · W · primary

문안 P4. 기대 W. 회수 6/6. 거절 0개, 유효 중복 0개. 입력/출력 826/146토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"rules":["mana_skill과 skill은 서로 다른 Crafted. speed는 일반 접미. 세 옵션 모두 보존. 두 번째 Crafted 전 한도 확장 필수."],"openers":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed"],"goals":{"mana_skill":["safe_alloy.mana_skill"],"skill":["safe_ess.skill"],"speed":["base_add.speed"]},"strategies":{"safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item."},"helpers":["cap_up.item"],"crafted":{"used":0,"uses":["safe_alloy","safe_ess"]}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"safe_alloy.mana_skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed","cap_up.item"]}}},"safe_ess.skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed","cap_up.item"]}}},"base_add.speed":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed","cap_up.item"]}}}},"required":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"safe_alloy.mana_skill":[["safe_alloy.mana_skill","cap_up.item","safe_ess.skill","base_add.speed"],["safe_alloy.mana_skill","cap_up.item","base_add.speed","safe_ess.skill"]],"safe_ess.skill":[["safe_ess.skill","cap_up.item","safe_alloy.mana_skill","base_add.speed"],["safe_ess.skill","cap_up.item","base_add.speed","safe_alloy.mana_skill"]],"base_add.speed":[["base_add.speed","cap_up.item","safe_alloy.mana_skill","safe_ess.skill"],["base_add.speed","cap_up.item","safe_ess.skill","safe_alloy.mana_skill"]]}
```

### 요청 26 · H2 · primary

문안 P1. 기대 H2. 회수 1/2. 거절 5개, 유효 중복 0개. 입력/출력 694/97토큰.

전송 JSON은 요청 2과 동일하다.

응답 스키마은 요청 2과 동일하다.

실제 응답 원문

```json
{"safe_ess.skill":[["safe_ess.skill","cap_up.item","base_add.speed"],["safe_ess.skill","base_add.speed","cap_up.item"],["safe_ess.skill","base_add.speed","base_add.speed"]],"base_add.speed":[["base_add.speed","cap_up.item","safe_ess.skill"],["base_add.speed","safe_ess.skill","cap_up.item"],["base_add.speed","safe_ess.skill","safe_ess.skill"]]}
```

누락된 유효 경로

```json
["safe_ess.skill base_add.speed"]
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["safe_ess.skill","cap_up.item","base_add.speed"],"Crafted capacity exhausted"],[["safe_ess.skill","base_add.speed","cap_up.item"],"Crafted capacity exhausted"],[["safe_ess.skill","base_add.speed","base_add.speed"],"Crafted capacity exhausted"],[["base_add.speed","safe_ess.skill","cap_up.item"],"Crafted capacity exhausted"],[["base_add.speed","safe_ess.skill","safe_ess.skill"],"Crafted capacity exhausted"]]
```

</details>

### 요청 27 · N · primary

문안 P1. 기대 N. 회수 6/7. 거절 2개, 유효 중복 0개. 입력/출력 1038/139토큰.

전송 JSON은 요청 3과 동일하다.

응답 스키마은 요청 3과 동일하다.

실제 응답 원문

```json
{"first_chaos.life":[["first_chaos.life","safe_ess.def"]],"base_add.life":{"hit":[["base_add.life","safe_ess.def"]],"onMiss":[["base_add.life","whittle_miss.life","safe_ess.def"]]},"reveal.life":[["reveal.life","safe_ess.def"]],"safe_ess.def":{"hit":[["safe_ess.def","base_add.life"],["safe_ess.def","reveal.life"]],"onMiss":[["safe_ess.def","whittle_miss.life","base_add.life"],["safe_ess.def","whittle_miss.life","safe_ess.def"]]} }
```

누락된 유효 경로

```json
["safe_ess.def base_add.life whittle_miss.life"]
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["safe_ess.def","whittle_miss.life","base_add.life"],"recovery requires preceding base_add on same target"],[["safe_ess.def","whittle_miss.life","safe_ess.def"],"recovery requires preceding base_add on same target"]]
```

</details>

### 요청 28 · R · primary

문안 P1. 기대 R. 회수 6/6. 거절 0개, 유효 중복 3개. 입력/출력 790/113토큰.

전송 JSON은 요청 4과 동일하다.

응답 스키마은 요청 4과 동일하다.

실제 응답 원문

```json
{"base_add.life":[["base_add.life","base_add.cold"],["base_add.life","reveal.cold"],["base_add.life","reveal.cold"]],"reveal.life":[["reveal.life","base_add.cold"],["reveal.life","base_add.cold"]],"base_add.cold":[["base_add.cold","base_add.life"],["base_add.cold","reveal.life"],["base_add.cold","reveal.life"]],"reveal.cold":[["reveal.cold","base_add.life"]]}
```

### 요청 29 · G · primary

문안 P1. 기대 G. 회수 2/2. 거절 0개, 유효 중복 0개. 입력/출력 684/35토큰.

전송 JSON은 요청 5과 동일하다.

응답 스키마은 요청 5과 동일하다.

실제 응답 원문

```json
{"upgrade_ess.life":[["upgrade_ess.life","safe_alloy.pen"]],"regal_base.life":[["regal_base.life","safe_alloy.pen"]]}
```

### 요청 30 · Q2 · primary

문안 P1. 기대 Q2. 회수 2/2. 거절 2개, 유효 중복 0개. 입력/출력 713/83토큰.

전송 JSON은 요청 6과 동일하다.

응답 스키마은 요청 6과 동일하다.

실제 응답 원문

```json
{"safe_alloy.mana_skill":[["safe_alloy.mana_skill","cap_up.item","safe_ess.skill"],["safe_alloy.mana_skill","safe_ess.skill","cap_up.item"]],"safe_ess.skill":[["safe_ess.skill","cap_up.item","safe_alloy.mana_skill"],["safe_ess.skill","safe_alloy.mana_skill","cap_up.item"]]}
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["safe_alloy.mana_skill","safe_ess.skill","cap_up.item"],"Crafted capacity exhausted"],[["safe_ess.skill","safe_alloy.mana_skill","cap_up.item"],"Crafted capacity exhausted"]]
```

</details>

### 요청 31 · H4 · primary

문안 P1. 기대 H4. 회수 2/2. 거절 0개, 유효 중복 0개. 입력/출력 731/29토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","raw":99}],"suffix":[{"id":"str","raw":7}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"strategies":{"flesh":"Flesh 품질 20 적용. 기존 수치 유지. 대상 item","divine":"두 옵션 수치를 현재 티어 안에서 함께 굴린다. 변경 가능 아이템, 대상 item"},"rules":["두 옵션은 보유. Flesh20과 원래 굴림 99·8이면 합 127로 완성. 현재 품질 0, 99·7은 품질만 올리면 126으로 미달. 품질과 재굴림은 어느 순서도 가능."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":0},"requiredRows":["Flesh20","values"],"afterFirst":{"flesh.item":{"values":["divine.item"]},"divine.item":{"Flesh20":["flesh.item"]}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"flesh.item":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["flesh.item","divine.item"]},"minItems":2,"maxItems":2}},"divine.item":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.item","flesh.item"]},"minItems":2,"maxItems":2}}},"required":["flesh.item","divine.item"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"flesh.item":[["flesh.item","divine.item"]],"divine.item":[["divine.item","flesh.item"]]}
```

### 요청 32 · Q4 · primary

문안 P1. 기대 Q4. 회수 1/1. 거절 0개, 유효 중복 0개. 입력/출력 568/5토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","value":90},{"id":"def","crafted":true}],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"strategies":{},"rules":[],"slots":{"craftedUsed":1,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":0},"requiredRows":[],"afterFirst":{}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"recipes":{"type":"array","items":{"type":"array","items":{"type":"string"}}}},"required":["recipes"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"recipes":[[]]}
```

### 요청 33 · T1 · primary

문안 P1. 기대 T1. 회수 1/1. 거절 0개, 유효 중복 0개. 입력/출력 549/11토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"speed":["base"]}},"strategies":{"base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare"},"rules":[],"slots":{"craftedUsed":1,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":0},"requiredRows":["speed"],"afterFirst":{"base_add.speed":{}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"base_add.speed":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.speed"]},"minItems":1,"maxItems":1}}},"required":["base_add.speed"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"base_add.speed":[["base_add.speed"]]}
```

### 요청 34 · T2 · primary

문안 P1. 기대 T2. 회수 2/2. 거절 0개, 유효 중복 2개. 입력/출력 668/52토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]},"craftedLimit":2,"capacitySocket":0},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"strategies":{"safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare"},"rules":["시작 장비의 Crafted 한도 확장은 이미 적용되어 있다."],"slots":{"craftedUsed":1,"limitNow":2,"additionalCraftedNeededAtLeast":1,"expandBy":0,"desecratedUsed":0},"requiredRows":["skill","speed"],"afterFirst":{"safe_ess.skill":{"speed":["base_add.speed"]},"base_add.speed":{"skill":["safe_ess.skill"]}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"safe_ess.skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","base_add.speed"]},"minItems":2,"maxItems":2}},"base_add.speed":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.speed","safe_ess.skill"]},"minItems":2,"maxItems":2}}},"required":["safe_ess.skill","base_add.speed"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"safe_ess.skill":[["safe_ess.skill","base_add.speed"],["safe_ess.skill","base_add.speed"]],"base_add.speed":[["base_add.speed","safe_ess.skill"],["base_add.speed","safe_ess.skill"]]}
```

### 요청 35 · T3 · primary

문안 P1. 기대 T3. 회수 8/18. 거절 0개, 유효 중복 3개. 입력/출력 932/194토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"cold":["base","Preserved Collarbone"],"lightning":["base"]}},"strategies":{"base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다."},"rules":["이 요청은 엑잘과 훼손 획득 경로를 비교한다. 확보한 목표를 제거하지 않는다."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":0},"requiredRows":["life","cold","lightning"],"afterFirst":{"base_add.life":{"cold":["base_add.cold","reveal.cold"],"lightning":["base_add.lightning"]},"reveal.life":{"cold":["base_add.cold"],"lightning":["base_add.lightning"]},"base_add.cold":{"life":["base_add.life","reveal.life"],"lightning":["base_add.lightning"]},"reveal.cold":{"life":["base_add.life"],"lightning":["base_add.lightning"]},"base_add.lightning":{"life":["base_add.life","reveal.life"],"cold":["base_add.cold","reveal.cold"]}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"base_add.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","base_add.cold","reveal.cold","base_add.lightning"]},"minItems":3,"maxItems":3}},"reveal.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["reveal.life","base_add.cold","base_add.lightning"]},"minItems":3,"maxItems":3}},"base_add.cold":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.cold","base_add.life","reveal.life","base_add.lightning"]},"minItems":3,"maxItems":3}},"reveal.cold":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["reveal.cold","base_add.life","base_add.lightning"]},"minItems":3,"maxItems":3}},"base_add.lightning":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.lightning","base_add.life","reveal.life","base_add.cold","reveal.cold"]},"minItems":3,"maxItems":3}}},"required":["base_add.life","reveal.life","base_add.cold","reveal.cold","base_add.lightning"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"base_add.life":[["base_add.life","base_add.cold","base_add.lightning"],["base_add.life","reveal.cold","base_add.lightning"],["base_add.life","base_add.cold","base_add.lightning"],["base_add.life","reveal.cold","base_add.lightning"]],"reveal.life":[["reveal.life","base_add.cold","base_add.lightning"],["reveal.life","base_add.cold","base_add.lightning"]],"base_add.cold":[["base_add.cold","base_add.life","base_add.lightning"],["base_add.cold","reveal.life","base_add.lightning"]],"reveal.cold":[["reveal.cold","base_add.life","base_add.lightning"]],"base_add.lightning":[["base_add.lightning","base_add.life","base_add.cold"],["base_add.lightning","base_add.life","reveal.cold"]]}
```

누락된 유효 경로

```json
["base_add.cold base_add.lightning base_add.life","base_add.cold base_add.lightning reveal.life","base_add.life base_add.lightning base_add.cold","base_add.life base_add.lightning reveal.cold","base_add.lightning base_add.cold base_add.life","base_add.lightning base_add.cold reveal.life","base_add.lightning reveal.cold base_add.life","base_add.lightning reveal.life base_add.cold","reveal.cold base_add.lightning base_add.life","reveal.life base_add.lightning base_add.cold"]
```

### 요청 36 · H3 · primary

문안 P1. 기대 H3. 음성 대조. 거절 2개, 유효 중복 0개. 입력/출력 683/41토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"]}},"strategies":{"safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요"},"rules":["Crafted 한도를 올릴 수단이 없다."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":2,"expandBy":0,"desecratedUsed":0},"requiredRows":["mana_skill","skill"],"afterFirst":{"safe_alloy.mana_skill":{"skill":["safe_ess.skill"]},"safe_ess.skill":{"mana_skill":["safe_alloy.mana_skill"]}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"safe_alloy.mana_skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill"]},"minItems":2,"maxItems":2}},"safe_ess.skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","safe_alloy.mana_skill"]},"minItems":2,"maxItems":2}}},"required":["safe_alloy.mana_skill","safe_ess.skill"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"safe_alloy.mana_skill":[["safe_alloy.mana_skill","safe_ess.skill"]],"safe_ess.skill":[["safe_ess.skill","safe_alloy.mana_skill"]]}
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["safe_alloy.mana_skill","safe_ess.skill"],"Crafted capacity exhausted"],[["safe_ess.skill","safe_alloy.mana_skill"],"Crafted capacity exhausted"]]
```

</details>

### 요청 37 · Q1 · primary

문안 P1. 기대 Q1. 음성 대조. 거절 4개, 유효 중복 0개. 입력/출력 653/52토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"strategies":{"safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare"},"rules":[],"slots":{"craftedUsed":1,"limitNow":1,"additionalCraftedNeededAtLeast":1,"expandBy":0,"desecratedUsed":0},"requiredRows":["skill","speed"],"afterFirst":{"safe_ess.skill":{"speed":["base_add.speed"]},"base_add.speed":{"skill":["safe_ess.skill"]}}}
```

</details>

응답 스키마은 요청 34과 동일하다.

실제 응답 원문

```json
{"safe_ess.skill":[["safe_ess.skill","base_add.speed"],["safe_ess.skill","base_add.speed"]],"base_add.speed":[["base_add.speed","safe_ess.skill"],["base_add.speed","safe_ess.skill"]]}
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["safe_ess.skill","base_add.speed"],"Crafted capacity exhausted"],[["safe_ess.skill","base_add.speed"],"Crafted capacity exhausted"],[["base_add.speed","safe_ess.skill"],"Crafted capacity exhausted"],[["base_add.speed","safe_ess.skill"],"Crafted capacity exhausted"]]
```

</details>

### 요청 38 · W · secondary

문안 P3. 기대 W. 회수 2/6. 거절 1개, 유효 중복 0개. 입력/출력 1085/190토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"needOnePerRow":{"mana_skill":["safe_alloy.mana_skill"],"skill":["safe_ess.skill"],"speed":["base_add.speed"]},"strategies":{"safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item."},"rules":["mana_skill과 skill은 서로 다른 Crafted. speed는 일반 접미. 세 옵션 모두 보존. 두 번째 Crafted 전 한도 확장 필수."],"pivot":"speed","helpers":["cap_up.item"],"coverage":{"pivot":"speed","unseenPositions":[],"unseenActions":[]}}
```

</details>

응답 스키마은 요청 19과 동일하다.

실제 응답 원문 (탭과 줄바꿈을 보존한 JSON 문자열)

```json
{"raw":"{\"at1\":[{\"before\":[\n \t\t\t\t\n\t\t],\"take\":{\"setup\":\"cap_up.item\",\"use\":\"base_add.speed\"},\"after\":[\n\t\t\t{\"setup\":\"none\",\"use\":\"safe_alloy.mana_skill\"},\n\t\t\t{\"setup\":\"none\",\"use\":\"safe_ess.skill\"}\n\t\t]}],\"at2\":[{\"before\":[\n\t\t\t{\"setup\":\"none\",\"use\":\"safe_alloy.mana_skill\"}\n\t\t],\"take\":{\"setup\":\"cap_up.item\",\"use\":\"base_add.speed\"},\"after\":[\n\t\t\t{\"setup\":\"none\",\"use\":\"safe_ess.skill\"}\n\t\t]}],\"at3\":[{\"before\":[\n\t\t\t{\"setup\":\"none\",\"use\":\"safe_alloy.mana_skill\"},\n\t\t\t{\"setup\":\"none\",\"use\":\"safe_ess.skill\"}\n\t\t],\"take\":{\"setup\":\"cap_up.item\",\"use\":\"base_add.speed\"},\"after\":[]}]}"}
```

누락된 유효 경로

```json
["base_add.speed safe_ess.skill safe_alloy.mana_skill","safe_alloy.mana_skill safe_ess.skill base_add.speed","safe_ess.skill base_add.speed safe_alloy.mana_skill","safe_ess.skill safe_alloy.mana_skill base_add.speed"]
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["safe_alloy.mana_skill","safe_ess.skill","cap_up.item","base_add.speed"],"Crafted capacity exhausted"]]
```

</details>

### 요청 39 · H2 · secondary

문안 P2. 기대 H2. 회수 2/2. 거절 0개, 유효 중복 2개. 입력/출력 683/87토큰.

전송 JSON은 요청 14과 동일하다.

응답 스키마은 요청 14과 동일하다.

실제 응답 원문

```json
{"at1":[{"prepare":["cap_up.item"],"steps":["base_add.speed","safe_ess.skill"]},{"prepare":["cap_up.item"],"steps":["safe_ess.skill","base_add.speed"]}],"at2":[{"prepare":["cap_up.item"],"steps":["safe_ess.skill","base_add.speed"]},{"prepare":["cap_up.item"],"steps":["base_add.speed","safe_ess.skill"]}]}
```

### 요청 40 · N · secondary

문안 P5. 기대 N. 회수 6/7. 거절 3개, 유효 중복 0개. 입력/출력 1303/302토큰.

전송 JSON은 요청 15과 동일하다.

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"first_chaos.life@at1":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["safe_ess.def"]},"minItems":0,"maxItems":0},"take":{"type":"object","properties":{"use":{"type":"string","enum":["first_chaos.life"]},"ifMiss":{"type":"string","enum":["none","whittle_miss.life"]}},"required":["use","ifMiss"],"additionalProperties":false},"after":{"type":"array","items":{"type":"string","enum":["safe_ess.def"]},"minItems":1,"maxItems":1}},"required":["before","take","after"],"additionalProperties":false}},"first_chaos.life@at2":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["safe_ess.def"]},"minItems":1,"maxItems":1},"take":{"type":"object","properties":{"use":{"type":"string","enum":["first_chaos.life"]},"ifMiss":{"type":"string","enum":["none","whittle_miss.life"]}},"required":["use","ifMiss"],"additionalProperties":false},"after":{"type":"array","items":{"type":"string","enum":["safe_ess.def"]},"minItems":0,"maxItems":0}},"required":["before","take","after"],"additionalProperties":false}},"base_add.life@at1":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["safe_ess.def"]},"minItems":0,"maxItems":0},"take":{"type":"object","properties":{"use":{"type":"string","enum":["base_add.life"]},"ifMiss":{"type":"string","enum":["none","whittle_miss.life"]}},"required":["use","ifMiss"],"additionalProperties":false},"after":{"type":"array","items":{"type":"string","enum":["safe_ess.def"]},"minItems":1,"maxItems":1}},"required":["before","take","after"],"additionalProperties":false}},"base_add.life@at2":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["safe_ess.def"]},"minItems":1,"maxItems":1},"take":{"type":"object","properties":{"use":{"type":"string","enum":["base_add.life"]},"ifMiss":{"type":"string","enum":["none","whittle_miss.life"]}},"required":["use","ifMiss"],"additionalProperties":false},"after":{"type":"array","items":{"type":"string","enum":["safe_ess.def"]},"minItems":0,"maxItems":0}},"required":["before","take","after"],"additionalProperties":false}},"reveal.life@at1":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["safe_ess.def"]},"minItems":0,"maxItems":0},"take":{"type":"object","properties":{"use":{"type":"string","enum":["reveal.life"]},"ifMiss":{"type":"string","enum":["none","whittle_miss.life"]}},"required":["use","ifMiss"],"additionalProperties":false},"after":{"type":"array","items":{"type":"string","enum":["safe_ess.def"]},"minItems":1,"maxItems":1}},"required":["before","take","after"],"additionalProperties":false}},"reveal.life@at2":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["safe_ess.def"]},"minItems":1,"maxItems":1},"take":{"type":"object","properties":{"use":{"type":"string","enum":["reveal.life"]},"ifMiss":{"type":"string","enum":["none","whittle_miss.life"]}},"required":["use","ifMiss"],"additionalProperties":false},"after":{"type":"array","items":{"type":"string","enum":["safe_ess.def"]},"minItems":0,"maxItems":0}},"required":["before","take","after"],"additionalProperties":false}}},"required":["first_chaos.life@at1","first_chaos.life@at2","base_add.life@at1","base_add.life@at2","reveal.life@at1","reveal.life@at2"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"first_chaos.life@at1":[{"before":[],"take":{"use":"first_chaos.life","ifMiss":"none"},"after":["safe_ess.def"]},{"before":[],"take":{"use":"first_chaos.life","ifMiss":"whittle_miss.life"},"after":["safe_ess.def"]}],"first_chaos.life@at2":[{"before":["safe_ess.def"],"take":{"use":"first_chaos.life","ifMiss":"none"},"after":[]}],"base_add.life@at1":[{"before":[],"take":{"use":"base_add.life","ifMiss":"none"},"after":["safe_ess.def"]},{"before":[],"take":{"use":"base_add.life","ifMiss":"whittle_miss.life"},"after":["safe_ess.def"]}],"base_add.life@at2":[{"before":["safe_ess.def"],"take":{"use":"base_add.life","ifMiss":"none"},"after":[]}],"reveal.life@at1":[{"before":[],"take":{"use":"reveal.life","ifMiss":"none"},"after":["safe_ess.def"]},{"before":[],"take":{"use":"reveal.life","ifMiss":"whittle_miss.life"},"after":["safe_ess.def"]}],"reveal.life@at2":[{"before":["safe_ess.def"],"take":{"use":"reveal.life","ifMiss":"none"},"after":[]}]}
```

누락된 유효 경로

```json
["safe_ess.def base_add.life whittle_miss.life"]
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["first_chaos.life","whittle_miss.life","safe_ess.def"],"recovery requires preceding base_add on same target"],[["safe_ess.def","first_chaos.life"],"opening acquisition only"],[["reveal.life","whittle_miss.life","safe_ess.def"],"recovery requires preceding base_add on same target"]]
```

</details>

### 요청 41 · R · secondary

문안 P5. 기대 R. 회수 6/6. 거절 2개, 유효 중복 0개. 입력/출력 857/173토큰.

전송 JSON은 요청 16과 동일하다.

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"base_add.life@at1":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["base_add.cold","reveal.cold"]},"minItems":0,"maxItems":0},"take":{"type":"string","enum":["base_add.life"]},"after":{"type":"array","items":{"type":"string","enum":["base_add.cold","reveal.cold"]},"minItems":1,"maxItems":1}},"required":["before","take","after"],"additionalProperties":false}},"base_add.life@at2":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["base_add.cold","reveal.cold"]},"minItems":1,"maxItems":1},"take":{"type":"string","enum":["base_add.life"]},"after":{"type":"array","items":{"type":"string","enum":["base_add.cold","reveal.cold"]},"minItems":0,"maxItems":0}},"required":["before","take","after"],"additionalProperties":false}},"reveal.life@at1":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["base_add.cold","reveal.cold"]},"minItems":0,"maxItems":0},"take":{"type":"string","enum":["reveal.life"]},"after":{"type":"array","items":{"type":"string","enum":["base_add.cold","reveal.cold"]},"minItems":1,"maxItems":1}},"required":["before","take","after"],"additionalProperties":false}},"reveal.life@at2":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["base_add.cold","reveal.cold"]},"minItems":1,"maxItems":1},"take":{"type":"string","enum":["reveal.life"]},"after":{"type":"array","items":{"type":"string","enum":["base_add.cold","reveal.cold"]},"minItems":0,"maxItems":0}},"required":["before","take","after"],"additionalProperties":false}}},"required":["base_add.life@at1","base_add.life@at2","reveal.life@at1","reveal.life@at2"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"base_add.life@at1":[{"before":[],"take":"base_add.life","after":["base_add.cold"]},{"before":[],"take":"base_add.life","after":["reveal.cold"]}],"base_add.life@at2":[{"before":["base_add.cold"],"take":"base_add.life","after":[]},{"before":["reveal.cold"],"take":"base_add.life","after":[]}],"reveal.life@at1":[{"before":[],"take":"reveal.life","after":["base_add.cold"]},{"before":[],"take":"reveal.life","after":["reveal.cold"]}],"reveal.life@at2":[{"before":["base_add.cold"],"take":"reveal.life","after":[]},{"before":["reveal.cold"],"take":"reveal.life","after":[]}]}
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["reveal.life","reveal.cold"],"Desecrated slot occupied"],[["reveal.cold","reveal.life"],"Desecrated slot occupied"]]
```

</details>

### 요청 42 · G · secondary

문안 P5. 기대 G. 회수 2/2. 거절 2개, 유효 중복 0개. 입력/출력 821/113토큰.

전송 JSON은 요청 17과 동일하다.

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"upgrade_ess.life@at1":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["safe_alloy.pen"]},"minItems":0,"maxItems":0},"take":{"type":"string","enum":["upgrade_ess.life"]},"after":{"type":"array","items":{"type":"string","enum":["safe_alloy.pen"]},"minItems":1,"maxItems":1}},"required":["before","take","after"],"additionalProperties":false}},"upgrade_ess.life@at2":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["safe_alloy.pen"]},"minItems":1,"maxItems":1},"take":{"type":"string","enum":["upgrade_ess.life"]},"after":{"type":"array","items":{"type":"string","enum":["safe_alloy.pen"]},"minItems":0,"maxItems":0}},"required":["before","take","after"],"additionalProperties":false}},"regal_base.life@at1":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["safe_alloy.pen"]},"minItems":0,"maxItems":0},"take":{"type":"string","enum":["regal_base.life"]},"after":{"type":"array","items":{"type":"string","enum":["safe_alloy.pen"]},"minItems":1,"maxItems":1}},"required":["before","take","after"],"additionalProperties":false}},"regal_base.life@at2":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["safe_alloy.pen"]},"minItems":1,"maxItems":1},"take":{"type":"string","enum":["regal_base.life"]},"after":{"type":"array","items":{"type":"string","enum":["safe_alloy.pen"]},"minItems":0,"maxItems":0}},"required":["before","take","after"],"additionalProperties":false}}},"required":["upgrade_ess.life@at1","upgrade_ess.life@at2","regal_base.life@at1","regal_base.life@at2"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"upgrade_ess.life@at1":[{"before":[],"take":"upgrade_ess.life","after":["safe_alloy.pen"]}],"upgrade_ess.life@at2":[{"before":["safe_alloy.pen"],"take":"upgrade_ess.life","after":[]}],"regal_base.life@at1":[{"before":[],"take":"regal_base.life","after":["safe_alloy.pen"]}],"regal_base.life@at2":[{"before":["safe_alloy.pen"],"take":"regal_base.life","after":[]}]}
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["safe_alloy.pen","upgrade_ess.life"],"Rare required"],[["safe_alloy.pen","regal_base.life"],"Rare required"]]
```

</details>

### 요청 43 · Q2 · secondary

문안 P3. 기대 Q2. 회수 2/2. 거절 2개, 유효 중복 0개. 입력/출력 845/152토큰.

전송 JSON은 요청 18과 동일하다.

응답 스키마은 요청 24과 동일하다.

실제 응답 원문

```json
{"at1":[{"before":[],"take":{"setup":"none","use":"safe_alloy.mana_skill"},"after":[{"setup":"none","use":"safe_ess.skill"}]},{"before":[],"take":{"setup":"cap_up.item","use":"safe_alloy.mana_skill"},"after":[{"setup":"none","use":"safe_ess.skill"}]}],"at2":[{"before":[{"setup":"none","use":"safe_ess.skill"}],"take":{"setup":"none","use":"safe_alloy.mana_skill"},"after":[]},{"before":[{"setup":"none","use":"safe_ess.skill"}],"take":{"setup":"cap_up.item","use":"safe_alloy.mana_skill"},"after":[]}]}
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["safe_alloy.mana_skill","safe_ess.skill"],"Crafted capacity exhausted"],[["safe_ess.skill","safe_alloy.mana_skill"],"Crafted capacity exhausted"]]
```

</details>

### 요청 44 · H4 · secondary

문안 P3. 기대 H4. 회수 2/2. 거절 0개, 유효 중복 0개. 입력/출력 730/43토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","raw":99}],"suffix":[{"id":"str","raw":7}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"needOnePerRow":{"Flesh20":["flesh.item"],"values":["divine.item"]},"strategies":{"flesh":"Flesh 품질 20 적용. 기존 수치 유지. 대상 item","divine":"두 옵션 수치를 현재 티어 안에서 함께 굴린다. 변경 가능 아이템, 대상 item"},"rules":["두 옵션은 보유. Flesh20과 원래 굴림 99·8이면 합 127로 완성. 현재 품질 0, 99·7은 품질만 올리면 126으로 미달. 품질과 재굴림은 어느 순서도 가능."],"pivot":"Flesh20","helpers":[],"coverage":{"pivot":"Flesh20","unseenPositions":[],"unseenActions":[]}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"at1":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["divine.item"]},"minItems":0,"maxItems":0},"take":{"type":"string","enum":["flesh.item"]},"after":{"type":"array","items":{"type":"string","enum":["divine.item"]},"minItems":1,"maxItems":1}},"required":["before","take","after"],"additionalProperties":false}},"at2":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["divine.item"]},"minItems":1,"maxItems":1},"take":{"type":"string","enum":["flesh.item"]},"after":{"type":"array","items":{"type":"string","enum":["divine.item"]},"minItems":0,"maxItems":0}},"required":["before","take","after"],"additionalProperties":false}}},"required":["at1","at2"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"at1":[{"before":[],"take":"flesh.item","after":["divine.item"]}],"at2":[{"before":["divine.item"],"take":"flesh.item","after":[]}]}
```

### 요청 45 · Q4 · secondary

문안 P3. 기대 Q4. 회수 0/1. 거절 1개, 유효 중복 0개. 입력/출력 509/7토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","value":90},{"id":"def","crafted":true}],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"needOnePerRow":{},"strategies":{},"rules":[],"pivot":null,"helpers":[],"coverage":{"pivot":null,"unseenPositions":[],"unseenActions":[]}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"recipes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["unused"]}}}},"required":["recipes"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"recipes":[["unused"]]}
```

누락된 유효 경로

```json
[""]
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["unused"],"unknown action"]]
```

</details>

### 요청 46 · T1 · secondary

문안 P3. 기대 T1. 회수 1/1. 거절 0개, 유효 중복 0개. 입력/출력 524/18토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"speed":["base"]}},"needOnePerRow":{"speed":["base_add.speed"]},"strategies":{"base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare"},"rules":[],"pivot":"speed","helpers":[],"coverage":{"pivot":"speed","unseenPositions":[],"unseenActions":[]}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"at1":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["base_add.speed"]},"minItems":0,"maxItems":0},"take":{"type":"string","enum":["base_add.speed"]},"after":{"type":"array","items":{"type":"string","enum":["base_add.speed"]},"minItems":0,"maxItems":0}},"required":["before","take","after"],"additionalProperties":false}}},"required":["at1"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"at1":[{"before":[],"take":"base_add.speed","after":[]}]}
```

### 요청 47 · T2 · secondary

문안 P3. 기대 T2. 회수 2/2. 거절 0개, 유효 중복 0개. 입력/출력 665/45토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]},"craftedLimit":2,"capacitySocket":0},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"needOnePerRow":{"skill":["safe_ess.skill"],"speed":["base_add.speed"]},"strategies":{"safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare"},"rules":["시작 장비의 Crafted 한도 확장은 이미 적용되어 있다."],"pivot":"speed","helpers":[],"coverage":{"pivot":"speed","unseenPositions":[],"unseenActions":[]}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"at1":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["safe_ess.skill"]},"minItems":0,"maxItems":0},"take":{"type":"string","enum":["base_add.speed"]},"after":{"type":"array","items":{"type":"string","enum":["safe_ess.skill"]},"minItems":1,"maxItems":1}},"required":["before","take","after"],"additionalProperties":false}},"at2":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["safe_ess.skill"]},"minItems":1,"maxItems":1},"take":{"type":"string","enum":["base_add.speed"]},"after":{"type":"array","items":{"type":"string","enum":["safe_ess.skill"]},"minItems":0,"maxItems":0}},"required":["before","take","after"],"additionalProperties":false}}},"required":["at1","at2"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"at1":[{"before":[],"take":"base_add.speed","after":["safe_ess.skill"]}],"at2":[{"before":["safe_ess.skill"],"take":"base_add.speed","after":[]}]}
```

### 요청 48 · T3 · secondary

문안 P5. 기대 T3. 회수 0/18. 거절 12개, 유효 중복 0개. 입력/출력 1103/319토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"cold":["base","Preserved Collarbone"],"lightning":["base"]}},"needOnePerRow":{"life":["base_add.life","reveal.life"],"cold":["base_add.cold","reveal.cold"],"lightning":["base_add.lightning"]},"strategies":{"base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다."},"rules":["이 요청은 엑잘과 훼손 획득 경로를 비교한다. 확보한 목표를 제거하지 않는다."],"pivot":"life","helpers":[],"coverage":{"pivot":"life","unseenPositions":[3],"unseenActions":[]}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"base_add.life@at1":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["base_add.cold","reveal.cold","base_add.lightning"]},"minItems":0,"maxItems":0},"take":{"type":"string","enum":["base_add.life"]},"after":{"type":"array","items":{"type":"string","enum":["base_add.cold","reveal.cold","base_add.lightning"]},"minItems":2,"maxItems":2}},"required":["before","take","after"],"additionalProperties":false}},"base_add.life@at2":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["base_add.cold","reveal.cold","base_add.lightning"]},"minItems":1,"maxItems":1},"take":{"type":"string","enum":["base_add.life"]},"after":{"type":"array","items":{"type":"string","enum":["base_add.cold","reveal.cold","base_add.lightning"]},"minItems":1,"maxItems":1}},"required":["before","take","after"],"additionalProperties":false}},"base_add.life@at3":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["base_add.cold","reveal.cold","base_add.lightning"]},"minItems":2,"maxItems":2},"take":{"type":"string","enum":["base_add.life"]},"after":{"type":"array","items":{"type":"string","enum":["base_add.cold","reveal.cold","base_add.lightning"]},"minItems":0,"maxItems":0}},"required":["before","take","after"],"additionalProperties":false}},"reveal.life@at1":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["base_add.cold","reveal.cold","base_add.lightning"]},"minItems":0,"maxItems":0},"take":{"type":"string","enum":["reveal.life"]},"after":{"type":"array","items":{"type":"string","enum":["base_add.cold","reveal.cold","base_add.lightning"]},"minItems":2,"maxItems":2}},"required":["before","take","after"],"additionalProperties":false}},"reveal.life@at2":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["base_add.cold","reveal.cold","base_add.lightning"]},"minItems":1,"maxItems":1},"take":{"type":"string","enum":["reveal.life"]},"after":{"type":"array","items":{"type":"string","enum":["base_add.cold","reveal.cold","base_add.lightning"]},"minItems":1,"maxItems":1}},"required":["before","take","after"],"additionalProperties":false}},"reveal.life@at3":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["base_add.cold","reveal.cold","base_add.lightning"]},"minItems":2,"maxItems":2},"take":{"type":"string","enum":["reveal.life"]},"after":{"type":"array","items":{"type":"string","enum":["base_add.cold","reveal.cold","base_add.lightning"]},"minItems":0,"maxItems":0}},"required":["before","take","after"],"additionalProperties":false}}},"required":["base_add.life@at1","base_add.life@at2","base_add.life@at3","reveal.life@at1","reveal.life@at2","reveal.life@at3"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"base_add.life@at1":[{"before":[],"take":"base_add.life","after":["base_add.cold","reveal.cold"]},{"before":[],"take":"base_add.life","after":["reveal.cold","base_add.cold"]}],"base_add.life@at2":[{"before":["base_add.cold"],"take":"base_add.life","after":["reveal.cold"]},{"before":["reveal.cold"],"take":"base_add.life","after":["base_add.cold"]}],"base_add.life@at3":[{"before":["base_add.cold","reveal.cold"],"take":"base_add.life","after":[]},{"before":["reveal.cold","base_add.cold"],"take":"base_add.life","after":[]}],"reveal.life@at1":[{"before":[],"take":"reveal.life","after":["base_add.cold","reveal.cold"]},{"before":[],"take":"reveal.life","after":["reveal.cold","base_add.cold"]}],"reveal.life@at2":[{"before":["base_add.cold"],"take":"reveal.life","after":["reveal.cold"]},{"before":["reveal.cold"],"take":"reveal.life","after":["base_add.cold"]}],"reveal.life@at3":[{"before":["base_add.cold","reveal.cold"],"take":"reveal.life","after":[]},{"before":["reveal.cold","base_add.cold"],"take":"reveal.life","after":[]}]}
```

누락된 유효 경로

```json
["base_add.cold base_add.life base_add.lightning","base_add.cold base_add.lightning base_add.life","base_add.cold base_add.lightning reveal.life","base_add.cold reveal.life base_add.lightning","base_add.life base_add.cold base_add.lightning","base_add.life base_add.lightning base_add.cold","base_add.life base_add.lightning reveal.cold","base_add.life reveal.cold base_add.lightning","base_add.lightning base_add.cold base_add.life","base_add.lightning base_add.cold reveal.life","base_add.lightning base_add.life base_add.cold","base_add.lightning base_add.life reveal.cold","base_add.lightning reveal.cold base_add.life","base_add.lightning reveal.life base_add.cold","reveal.cold base_add.life base_add.lightning","reveal.cold base_add.lightning base_add.life","reveal.life base_add.cold base_add.lightning","reveal.life base_add.lightning base_add.cold"]
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["base_add.life","base_add.cold","reveal.cold"],"target already covered"],[["base_add.life","reveal.cold","base_add.cold"],"target already covered"],[["base_add.cold","base_add.life","reveal.cold"],"target already covered"],[["reveal.cold","base_add.life","base_add.cold"],"target already covered"],[["base_add.cold","reveal.cold","base_add.life"],"target already covered"],[["reveal.cold","base_add.cold","base_add.life"],"target already covered"],[["reveal.life","base_add.cold","reveal.cold"],"target already covered"],[["reveal.life","reveal.cold","base_add.cold"],"Desecrated slot occupied"],[["base_add.cold","reveal.life","reveal.cold"],"target already covered"],[["reveal.cold","reveal.life","base_add.cold"],"Desecrated slot occupied"],[["base_add.cold","reveal.cold","reveal.life"],"target already covered"],[["reveal.cold","base_add.cold","reveal.life"],"target already covered"]]
```

</details>

### 요청 49 · H3 · secondary

문안 P3. 기대 H3. 음성 대조. 거절 4개, 유효 중복 0개. 입력/출력 690/93토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"]}},"needOnePerRow":{"mana_skill":["safe_alloy.mana_skill"],"skill":["safe_ess.skill"]},"strategies":{"safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요"},"rules":["Crafted 한도를 올릴 수단이 없다."],"pivot":"mana_skill","helpers":[],"coverage":{"pivot":"mana_skill","unseenPositions":[1,2],"unseenActions":["safe_ess.skill","safe_alloy.mana_skill"]}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"at1":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["safe_ess.skill"]},"minItems":0,"maxItems":0},"take":{"type":"string","enum":["safe_alloy.mana_skill"]},"after":{"type":"array","items":{"type":"string","enum":["safe_ess.skill"]},"minItems":1,"maxItems":1}},"required":["before","take","after"],"additionalProperties":false}},"at2":{"type":"array","items":{"type":"object","properties":{"before":{"type":"array","items":{"type":"string","enum":["safe_ess.skill"]},"minItems":1,"maxItems":1},"take":{"type":"string","enum":["safe_alloy.mana_skill"]},"after":{"type":"array","items":{"type":"string","enum":["safe_ess.skill"]},"minItems":0,"maxItems":0}},"required":["before","take","after"],"additionalProperties":false}}},"required":["at1","at2"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"at1":[{"before":[],"take":"safe_alloy.mana_skill","after":["safe_ess.skill"]},{"before":[],"take":"safe_alloy.mana_skill","after":    ["safe_ess.skill"]}],"at2":[{"before":["safe_ess.skill"],"take":"safe_alloy.mana_skill","after":[]},{"before":["safe_ess.skill"],"take":"safe_alloy.mana_skill","after":[]}]}
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["safe_alloy.mana_skill","safe_ess.skill"],"Crafted capacity exhausted"],[["safe_alloy.mana_skill","safe_ess.skill"],"Crafted capacity exhausted"],[["safe_ess.skill","safe_alloy.mana_skill"],"Crafted capacity exhausted"],[["safe_ess.skill","safe_alloy.mana_skill"],"Crafted capacity exhausted"]]
```

</details>

### 요청 50 · Q1 · secondary

문안 P3. 기대 Q1. 음성 대조. 거절 4개, 유효 중복 0개. 입력/출력 662/80토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"needOnePerRow":{"skill":["safe_ess.skill"],"speed":["base_add.speed"]},"strategies":{"safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare"},"rules":[],"pivot":"speed","helpers":[],"coverage":{"pivot":"speed","unseenPositions":[1,2],"unseenActions":["safe_ess.skill","base_add.speed"]}}
```

</details>

응답 스키마은 요청 47과 동일하다.

실제 응답 원문

```json
{"at1":[{"before":[],"take":"base_add.speed","after":["safe_ess.skill"]},{"before":[],"take":"base_add.speed","after": ["safe_ess.skill"]}],"at2":[{"before":["safe_ess.skill"],"take":"base_add.speed","after":[]},{"before":["safe_ess.skill"],"take":"base_add.speed","after":[]}]}
```

<details>
<summary>거절된 실행열과 사유</summary>

```json
[[["base_add.speed","safe_ess.skill"],"Crafted capacity exhausted"],[["base_add.speed","safe_ess.skill"],"Crafted capacity exhausted"],[["safe_ess.skill","base_add.speed"],"Crafted capacity exhausted"],[["safe_ess.skill","base_add.speed"],"Crafted capacity exhausted"]]
```

</details>
