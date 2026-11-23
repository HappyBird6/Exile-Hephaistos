# 제작 경로 검증 피드백 30회

## 호출 전에 고정한 기대

기존 모델·설정과 원래 카드 정의는 [직전 실험](strategy-target-50.md)을 따른다. 성공 경로의 각 단계에는 모델이 직접 지정한 전략과 대상이 있어야 한다. 검증기는 순서를 생성하거나 준비 행동을 추가하지 않는다. 기대 목록과 기준 경로 수는 전송하지 않는다.

| 카드 | 기대 결과 |
|---|---|
| H2 기존 Crafted 마나 스킬, 빈 소켓 | 스킬 → 시전 속도, 시전 속도 → 스킬. 두 경로 모두 새 Crafted 스킬 전에 한도 확장이 있어야 한다. |
| H3 빈 소켓 없음, Crafted 목표 두 개 | 후보 없음. 한도 1. |
| N 태양의 목걸이 | 생명력 선취득 4개(첫 카오스, 엑잘, 엑잘 실패 복구, 훼손), 방어 선취득 3개(엑잘, 엑잘 실패 복구, 훼손). |
| G 마법 장갑 | 생명력을 승급 에센스 또는 제왕으로 얻고 합금 관통으로 끝내는 두 경로. |
| R 반지 | 생명력·냉기 두 순서 각각 일반+일반, 일반+훼손, 훼손+일반. |
| H4 품질과 수치 | Flesh와 Divine 두 단계의 양쪽 순서. |
| W 빈 마법봉 | 합금·에센스·시전 속도 획득 순서 여섯 가지. 두 번째 Crafted 전에 한도 확장. |
| H1 생명력 보유 목걸이 | 방어 에센스 한 번. |
| Q1 H2에서 소켓과 확장 수단만 제거 | 후보 없음. 기존 Crafted 보존 시 추가 Crafted 자리가 없다. |
| Q2 H3에 소켓 하나와 확장 수단 추가 | 합금 → 에센스, 에센스 → 합금. 두 번째 Crafted 전 한도 확장. |
| Q3 R에 이미 훼손 생명력 보유 | 일반 엑잘로 냉기 획득 한 경로. 두 번째 훼손 불가. |
| Q4 H1에서 방어까지 보유 | 이미 완성. 빈 행동열 하나. 불가능인 경우의 빈 후보 목록과 구분한다. |

실험은 30회로 제한한다. 1–6은 기존 문안, 7–12는 동일 입력의 실제 검증 결과를 붙인 재요청, 13–18은 수정안, 19–30은 고정한 최종안이다. 19–26에 기존 카드 8장, 27–30에 Q1–Q4를 한 번씩 넣는다. 사용 가능한 소켓이 있다는 이유만으로 불필요한 확장을 채점하지 않는다. 같은 획득 순서에서 필수 확장의 적법한 위치만 달라지면 동일 경로 한 개로 센다.

## 결과와 판단

실제 호출 30회 모두 정상 종료했다. 비교한 모델은 직전과 같은 `gpt-4.1-mini-2025-04-14`, temperature 0, strict JSON Schema, 상한 3,200토큰, 저장 비활성이다. API usage는 입력 22,448토큰, 출력 1,886토큰이었다. 검증 범위는 고정한 카드와 전략의 상태 전이다. Java Workbench 실행, 확률·비용 계산, 실제 게임의 전체 제작 경로는 이 수치에 포함되지 않는다.

| 호출 | 변경 | 동일한 6장 회수¹ | 동일한 6장 위반 후보 | 관찰 |
|---|---|---:|---:|---|
| 1–6 | 직전 최종 형식 그대로 | 19/20 | 12 | 기존 Crafted 마법봉은 직전 0/2에서 이번 2/2로 달라졌고, 확장 누락 후보도 2개 냈다. 소켓 없는 카드는 다시 실패했다. |
| 7–12 | 같은 입력에 직전 응답과 거절 사유를 추가 | 20/20 | 6 | 소켓 없는 마법봉은 모든 목록을 비웠다. 장갑·반지의 잘못된 후보도 사라졌다. 기존 Crafted 마법봉의 오답은 2개에서 4개로 늘었다. |
| 13–18 | 자원 수치와 목표별 택일 목록을 보내고 단일 recipes 배열로 응답 | 4/20 | 7 | 한도 부족은 판정했지만 목걸이 0/7, 반지 0/6. 독립 후보를 한 순서 안에 합쳤다. |
| 19–24 | 카드 상태로 첫 획득과 후속 대안을 좁힌 그룹별 응답 | 18/20 | 10 | 목걸이·장갑·반지·품질 카드는 전부 회수. 기존 Crafted 마법봉 1/2, 소켓 없는 마법봉은 다시 실패했다. |

¹ 20개 중 19개는 양성 경로, 하나는 빈 목록이어야 하는 음성 사례다. 기준선과 재요청은 **양성만 보면 모두 19/19**다. 재요청에서 개선된 것은 음성 판정과 오답 수였으며 추가 양성 회수 효과는 관측되지 않았다. 재요청은 같은 카드당 한 번 더 호출했고 6회 입력 5,592·출력 399토큰이 들었다. 이 실험에는 동일 입력을 피드백 없이 한 번 더 보내는 무작위 대조군이 없다. 모델의 응답 변동과 피드백 효과를 분리한 추정치는 아니다.

### 최종 문안을 고정한 12장

| 호출 | 카드 | 회수 | 위반 / 유효 중복 | 판정 근거 |
|---|---|---:|---:|---|
| 19 | H2 기존 Crafted 마법봉 | 1/2 | 5 / 0 | 시전 속도 먼저는 회수. 스킬부터 얻는 목록에서는 한도 확장이 스킬 뒤로 밀렸다. |
| 20 | H3 소켓 없는 마법봉 | 음성 실패 | 4 / 0 | 한도 1에 2 Crafted를 넣었다. |
| 21 | N 분열 목걸이 | 7/7 | 1 / 0 | 방어부터 얻는 실패 분기 하나에서 엑잘보다 절사가 먼저 나왔다. |
| 22 | G 마법 장갑 | 2/2 | 0 / 0 | 승급 전 합금 경로가 사라졌다. |
| 23 | R 반지 | 6/6 | 0 / 1 | 훼손 중복이 사라졌고 유효 후보 하나가 중복됐다. |
| 24 | H4 품질·수치 목걸이 | 2/2 | 0 / 0 | 두 순서가 각자의 첫 획득 칸에 들어갔다. |
| 25 | W 빈 마법봉 | 4/6 | 0 / 2 | 시전 속도 중간 획득 두 방향이 빠졌다. 직전 50회 실험의 최종 응답은 6/6이었다. |
| 26 | H1 생명력 보유 목걸이 | 1/1 | 0 / 0 | 방어만 추가했다. |
| 27 | Q1 기존 Crafted, 소켓 없음 | 음성 실패 | 4 / 0 | 보존 중인 옵션이 이미 한도를 점유하고 있다. |
| 28 | Q2 두 Crafted, 빈 소켓 있음 | 2/2 | 4 / 2 | 양쪽 획득 순서는 회수. 한도 확장이 늦은 후보와 중복도 있다. |
| 29 | Q3 이미 훼손된 반지 | 1/1 | 0 / 0 | 일반 엑잘로 냉기만 추가했다. |
| 30 | Q4 이미 완성된 목걸이 | 1/1 | 0 / 0 | 실제 응답은 `{"recipes":[[]]}`였다. |

양성 카드 10장에서 무행동 완성 1개를 포함해 **27/30** 경로를 회수했다. 음성 카드 2장의 성공은 **0/2**였다. 반환된 위반 후보 18개와 유효 중복 5개는 회수에 넣지 않았다. 27–30번 네 입력은 최종 문안 확정 후 처음 모델에 제시했다.

### 무엇을 채택할 수 있는가

- **상태에 따라 처음부터 쓸 수 없는 전략을 요청에서 덜어낸다.** 마법 장갑에서 승급 전 합금을 첫 획득 후보에서 빼고, 반지에서 훼손 뒤의 두 번째 훼손을 후속 대안에서 뺐다. 첫 선택지와 목표별 대안만 좁혔다. 전체 화폐 순서는 계속 모델이 작성했다. 이 방법은 이번 두 카드의 양성 경로를 모두 보존했다.
- **Crafted 한도 초과는 요청 전 독립 검사 후보로 둔다.** 보존해야 할 기존 점유 + 새 목표 중 모든 획득 대안이 Crafted인 행 수가 현재 한도 + 허용된 최대 확장량보다 크면 이 카드의 목표는 불가능하다. 한도가 부족한 시점에는 확장 외에 다른 지원 수단이 없는 고정 입력에서만 적용했다. 19–30번의 입력값으로 별도 재생하면 H3·Q1 두 음성 카드만 걸러지고 양성 10장은 모두 통과한다. 이 값은 모델의 음성 0/2와 별도로 집계한다. 여러 행을 하나의 옵션이 동시에 만족하거나, 보존 대상이 아닌 기존 Crafted를 교체할 수 있는 카드는 같은 방식으로 행 수를 합산하면 안 된다.
- **마법봉 공통 프롬프트 교체는 보류한다.** 단일 응답 배열이든 첫 획득별 배열이든 기존 Crafted 앞 준비와 시전 속도 중간 배치를 안정적으로 회수하지 못했다. 19번·25번의 누락 세 순서는 전부 해당 응답 스키마가 허용한다. 이번 입력에는 첫 ‘획득’과 첫 ‘실행’의 차이가 있어서 `safe_ess.skill` 그룹도 실제 첫 실행은 `cap_up.item`일 수 있다. 후속 실험에서는 이 차이를 `준비 → 첫 획득 → 나머지`로 명시하는 응답과, 시전 속도 중간 배치만 재요청하는 방식을 비교할 수 있다. 효과는 미검증이다.
- **피드백은 유효 후보를 보존한 뒤 사용한다.** 이 실험에서는 별도 재요청에 원응답과 거절된 순서·첫 위반 사유를 담았다. 전체 정답이나 누락 경로를 제공하지 않았다. 기존 응답과 재요청에서 각각 검증된 완성 경로만 합치면, 한 번 찾은 후보가 다음 응답에서 사라져도 보존할 수 있다. 위 표는 매회 원응답 단독 점수다.

### 자체 검증

기준선 여섯 요청의 문안·입력 JSON·응답 스키마가 직전 실험 기록과 동일한지 확인했다. 30개 원응답을 JSON Schema에 대조하고, 각 경로를 독립 상태 전이 열거기와 순차 검증기로 재채점했다. 최종 12장의 열거 가능한 **모든** 기준 경로가 해당 첫 획득·결과 칸의 스키마에 들어가는지도 검사했다. 기대 목록을 새 모델 호출에 넣지 않았으며 30회 이후 검사는 로컬 기록으로 수행했다.

## 전송 문안

기준선은 직전 D/F를 같은 카드에 그대로 적용했다. 재요청은 각 기준 문안 끝에 아래 지시를 덧붙였다.

```text
검증기의 prior와 rejected를 검토한다. 유효 후보를 유지하고 미달 목표와 위반 사유를 고친 전체 후보 목록을 다시 제출한다. 해결할 수 없는 첫 획득은 비워 둔다.
```

### 단일 배열 비교안

```text
역할: PoE2 제작 작업대에서 실행할 화폐 전략과 대상을 순서대로 제안한다.
먼저 slots를 확인한다. craftedUsed와 additionalCraftedNeededAtLeast의 합이 limitNow + expandBy보다 크면 후보는 없다. 기존 목표는 보존한다. 빈 목표 집합이면 이미 완성된 경로 하나를 반환한다.
needOnePerRow의 모든 행마다 전략.대상 한 개를 선택하고, 선택한 획득들의 가능한 순서를 바꿔 본다. 매 경로에서 현재 희귀도, 점유, 남은 소켓, 훼손 여부를 새로 추적한다. 두 번째 훼손은 금지하고, 기존 Crafted도 점유 수에 포함한다. 한도 확장이 필요하면 해당 전략 직전에 모델이 직접 cap_up.item을 적는다. 사용 조건과 rules를 따른다.
출력 recipes는 완성된 전략.대상 배열들의 목록이다. 엑잘의 조건부 절사 복구가 가능하면 엑잘 적중 경로와 같은 대상의 실패 복구 경로를 각각 처음부터 끝까지 적는다. 마지막으로 빠진 목표나 중복 경로를 제거한다. 불가능은 [], 이미 완성은 [[]]다. 확률과 비용은 백엔드에서 계산한다.
```

### 최종 검사 문안

```text
역할: PoE2 제작 작업대에서 시도할 완전한 화폐 사용 순서를 설계한다. 단계 ID는 전략.대상이다.
1. 시작 옵션을 보존한다. requiredRows가 비면 recipes에 빈 행동열 하나를 반환한다. Crafted의 기존 점유 + 필수 신규 자리 수가 현재 한도 + 확장 가능량을 넘으면 모든 목록을 비운다.
2. afterFirst의 각 키는 첫 획득 후보이고 그 아래 각 행은 이후 채워야 할 목표다. 키에 해당하는 행동도 응답 배열에 직접 적는다. 행마다 하나만 고르고 모든 행을 채운다. 남은 두 행의 순서를 양쪽으로 바꾸며 같은 행의 대체 전략도 각각 검토한다. 시작 상태는 경로마다 초기화한다.
3. 전략 사전과 카드 규칙을 따른다. 준비 행동은 목표를 채우지 않는다. Crafted 자리가 모자라면 해당 획득 전에 한도를 확장하는 단계를 직접 적는다. 각 후보를 끝까지 읽어 시작 옵션 보존, 희귀도, 훼손 슬롯, 모든 목표의 충족을 확인한다.
4. 각 목록에는 첫 획득과 준비를 포함한 전체 경로를 한 번씩 적는다. hit는 직접 적중, onMiss는 엑잘 실패 직후 동일 대상에 절사 복구를 사용한 경로다. 두 칸이 없는 경우 해당 첫 획득의 목록에 바로 적는다. 불가능하면 빈 목록을 반환한다. 확률·비용은 백엔드가 계산한다.
```

## 호출별 시나리오·기대·원응답·판정

각 번호는 한 API 호출이다. 기대는 목표 획득 순서를 나타낸다. 한도 확장 준비의 위치는 앞의 사전 등록 조건을 적용한다. NONE은 후보 없음, 빈 문자열은 이미 완성된 무행동 경로다.

### 1 · H2 · baseline

기대 ["safe_ess.skill base_add.speed","base_add.speed safe_ess.skill"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"rules":[],"openers":["safe_ess.skill","base_add.speed"],"goals":{"skill":["safe_ess.skill"],"speed":["base_add.speed"]},"strategies":{"cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare"},"helpers":["cap_up.item"],"crafted":{"used":1,"uses":["safe_ess"]}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"safe_ess.skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["cap_up.item","safe_ess.skill","base_add.speed"]}}},"base_add.speed":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["cap_up.item","safe_ess.skill","base_add.speed"]}}}},"required":["safe_ess.skill","base_add.speed"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"safe_ess.skill":[["cap_up.item","safe_ess.skill","base_add.speed"],["safe_ess.skill","base_add.speed"]],"base_add.speed":[["cap_up.item","base_add.speed","safe_ess.skill"],["base_add.speed","safe_ess.skill"]]}
```

회수 2/2 · 위반 2 · 유효 중복 0 · 입력/출력 토큰 685/60.

<details>
<summary>모델 경로와 첫 위반 사유</summary>

```json
[[["safe_ess.skill","base_add.speed"],"Crafted capacity exhausted"],[["base_add.speed","safe_ess.skill"],"Crafted capacity exhausted"]]
```

</details>

### 2 · H3 · baseline

기대 ["NONE"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"]}},"rules":["Crafted 한도를 올릴 수단이 없다."],"openers":["safe_ess.skill","safe_alloy.mana_skill"],"goals":{"mana_skill":["safe_alloy.mana_skill"],"skill":["safe_ess.skill"]},"strategies":{"safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요"},"helpers":[],"crafted":{"used":0,"uses":["safe_ess","safe_alloy"]}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"safe_ess.skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","safe_alloy.mana_skill"]}}},"safe_alloy.mana_skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","safe_alloy.mana_skill"]}}}},"required":["safe_ess.skill","safe_alloy.mana_skill"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"safe_ess.skill":[["safe_ess.skill","safe_alloy.mana_skill"],["safe_alloy.mana_skill","safe_ess.skill"]],"safe_alloy.mana_skill":[["safe_alloy.mana_skill","safe_ess.skill"],["safe_ess.skill","safe_alloy.mana_skill"]]}
```

회수 0/1 · 위반 4 · 유효 중복 0 · 입력/출력 토큰 673/67.

누락 ["NONE"]

<details>
<summary>모델 경로와 첫 위반 사유</summary>

```json
[[["safe_ess.skill","safe_alloy.mana_skill"],"Crafted capacity exhausted"],[["safe_alloy.mana_skill","safe_ess.skill"],"Crafted capacity exhausted"],[["safe_alloy.mana_skill","safe_ess.skill"],"Crafted capacity exhausted"],[["safe_ess.skill","safe_alloy.mana_skill"],"Crafted capacity exhausted"]]
```

</details>

### 3 · N · baseline

기대 ["first_chaos.life safe_ess.def","base_add.life safe_ess.def","base_add.life whittle_miss.life safe_ess.def","reveal.life safe_ess.def","safe_ess.def base_add.life","safe_ess.def base_add.life whittle_miss.life","safe_ess.def reveal.life"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"strategies":{"first_chaos":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함. 첫 획득에서만. 분열 보존.","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","whittle_miss":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략. 같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."},"helpers":["whittle_miss.life"],"crafted":{"used":0,"uses":["safe_ess"]},"afterFirst":{"first_chaos.life":{"def":["safe_ess.def"]},"base_add.life":{"def":["safe_ess.def"]},"reveal.life":{"def":["safe_ess.def"]},"safe_ess.def":{"life":["first_chaos.life","base_add.life","reveal.life"]}}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"first_chaos.life":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","safe_ess.def"]},"minItems":2,"maxItems":2}}},"required":["hit"],"additionalProperties":false},"base_add.life":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","safe_ess.def"]},"minItems":2,"maxItems":2}},"onMiss":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","safe_ess.def","whittle_miss.life"]},"minItems":3,"maxItems":3}}},"required":["hit","onMiss"],"additionalProperties":false},"reveal.life":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["reveal.life","safe_ess.def"]},"minItems":2,"maxItems":2}}},"required":["hit"],"additionalProperties":false},"safe_ess.def":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def","base_add.life","reveal.life"]},"minItems":2,"maxItems":2}},"onMiss":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def","base_add.life","reveal.life","whittle_miss.life"]},"minItems":3,"maxItems":3}}},"required":["hit","onMiss"],"additionalProperties":false}},"required":["first_chaos.life","base_add.life","reveal.life","safe_ess.def"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"first_chaos.life":{"hit":[["first_chaos.life","safe_ess.def"]]},"base_add.life":{"hit":[["base_add.life","safe_ess.def"]],"onMiss":[["base_add.life","whittle_miss.life","safe_ess.def"]]},"reveal.life":{"hit":[["reveal.life","safe_ess.def"]]},"safe_ess.def":{"hit":[["safe_ess.def","base_add.life"],["safe_ess.def","reveal.life"]],"onMiss":[["safe_ess.def","base_add.life","whittle_miss.life"],["safe_ess.def","reveal.life","whittle_miss.life"]]}}
```

회수 7/7 · 위반 1 · 유효 중복 0 · 입력/출력 토큰 1039/143.

<details>
<summary>모델 경로와 첫 위반 사유</summary>

```json
[[["safe_ess.def","reveal.life","whittle_miss.life"],"recovery requires preceding base_add on same target"]]
```

</details>

### 4 · G · baseline

기대 ["upgrade_ess.life safe_alloy.pen","regal_base.life safe_alloy.pen"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Stocky Mitts","rarity":"magic","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"filler"}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body"],"pen":["Prismatic Alloy"]},"suffix":{}},"rules":[],"strategies":{"upgrade_ess":"등급 상승용 에센스로 목표 일반 옵션 획득. Magic → Rare","regal_base":"제왕으로 Magic을 Rare로 올리며 해당 일반 옵션이 뜬 결과 채택. Magic → Rare","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요"},"crafted":{"used":0,"uses":["safe_alloy"]},"afterFirst":{"upgrade_ess.life":{"pen":["safe_alloy.pen"]},"regal_base.life":{"pen":["safe_alloy.pen"]},"safe_alloy.pen":{"life":["upgrade_ess.life","regal_base.life"]}}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"upgrade_ess.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["upgrade_ess.life","safe_alloy.pen"]},"minItems":2,"maxItems":2}},"regal_base.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["regal_base.life","safe_alloy.pen"]},"minItems":2,"maxItems":2}},"safe_alloy.pen":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy.pen","upgrade_ess.life","regal_base.life"]},"minItems":2,"maxItems":2}}},"required":["upgrade_ess.life","regal_base.life","safe_alloy.pen"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"upgrade_ess.life":[["upgrade_ess.life","safe_alloy.pen"]],"regal_base.life":[["regal_base.life","safe_alloy.pen"]],"safe_alloy.pen":[["safe_alloy.pen","upgrade_ess.life"],["safe_alloy.pen","regal_base.life"]]}
```

회수 2/2 · 위반 2 · 유효 중복 0 · 입력/출력 토큰 706/63.

<details>
<summary>모델 경로와 첫 위반 사유</summary>

```json
[[["safe_alloy.pen","upgrade_ess.life"],"Rare required"],[["safe_alloy.pen","regal_base.life"],"Rare required"]]
```

</details>

### 5 · R · baseline

기대 ["base_add.life base_add.cold","base_add.cold base_add.life","reveal.life base_add.cold","base_add.cold reveal.life","base_add.life reveal.cold","reveal.cold base_add.life"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"cold":["base","Preserved Collarbone"]}},"rules":["이 요청은 엑잘과 훼손 획득 경로를 비교한다. 확보한 목표를 제거하지 않는다."],"strategies":{"base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다."},"afterFirst":{"base_add.life":{"cold":["base_add.cold","reveal.cold"]},"reveal.life":{"cold":["base_add.cold","reveal.cold"]},"base_add.cold":{"life":["base_add.life","reveal.life"]},"reveal.cold":{"life":["base_add.life","reveal.life"]}}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"base_add.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","base_add.cold","reveal.cold"]},"minItems":2,"maxItems":2}},"reveal.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["reveal.life","base_add.cold","reveal.cold"]},"minItems":2,"maxItems":2}},"base_add.cold":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.cold","base_add.life","reveal.life"]},"minItems":2,"maxItems":2}},"reveal.cold":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["reveal.cold","base_add.life","reveal.life"]},"minItems":2,"maxItems":2}}},"required":["base_add.life","reveal.life","base_add.cold","reveal.cold"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"base_add.life":[["base_add.life","base_add.cold"],["base_add.life","reveal.cold"]],"reveal.life":[["reveal.life","base_add.cold"],["reveal.life","reveal.cold"]],"base_add.cold":[["base_add.cold","base_add.life"],["base_add.cold","reveal.life"]],"reveal.cold":[["reveal.cold","base_add.life"],["reveal.cold","reveal.life"]]}
```

회수 6/6 · 위반 2 · 유효 중복 0 · 입력/출력 토큰 759/103.

<details>
<summary>모델 경로와 첫 위반 사유</summary>

```json
[[["reveal.life","reveal.cold"],"Desecrated slot occupied"],[["reveal.cold","reveal.life"],"Desecrated slot occupied"]]
```

</details>

### 6 · H4 · baseline

기대 ["flesh.item divine.item","divine.item flesh.item"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","raw":99}],"suffix":[{"id":"str","raw":7}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"rules":["두 옵션은 보유. Flesh20과 원래 굴림 99·8이면 합 127로 완성. 현재 품질 0, 99·7은 품질만 올리면 126으로 미달. 품질과 재굴림은 어느 순서도 가능."],"strategies":{"flesh":"Flesh 품질 20 적용. 기존 수치 유지. 대상 item","divine":"두 옵션 수치를 현재 티어 안에서 함께 굴린다. 변경 가능 아이템, 대상 item"},"afterFirst":{"flesh.item":{"values":["divine.item"]},"divine.item":{"Flesh20":["flesh.item"]}}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"flesh.item":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["flesh.item","divine.item"]},"minItems":2,"maxItems":2}},"divine.item":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.item","flesh.item"]},"minItems":2,"maxItems":2}}},"required":["flesh.item","divine.item"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"flesh.item":[["flesh.item","divine.item"],["divine.item","flesh.item"]],"divine.item":[["divine.item","flesh.item"]]}
```

회수 2/2 · 위반 1 · 유효 중복 0 · 입력/출력 토큰 680/38.

<details>
<summary>모델 경로와 첫 위반 사유</summary>

```json
[[["divine.item","flesh.item"],"first acquisition disagrees with group"]]
```

</details>

### 7 · H2 · retry

기대 ["safe_ess.skill base_add.speed","base_add.speed safe_ess.skill"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"rules":[],"openers":["safe_ess.skill","base_add.speed"],"goals":{"skill":["safe_ess.skill"],"speed":["base_add.speed"]},"strategies":{"cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare"},"helpers":["cap_up.item"],"crafted":{"used":1,"uses":["safe_ess"]},"validator":{"prior":{"safe_ess.skill":[["cap_up.item","safe_ess.skill","base_add.speed"],["safe_ess.skill","base_add.speed"]],"base_add.speed":[["cap_up.item","base_add.speed","safe_ess.skill"],["base_add.speed","safe_ess.skill"]]},"rejected":[[["safe_ess.skill","base_add.speed"],"Crafted capacity exhausted"],[["base_add.speed","safe_ess.skill"],"Crafted capacity exhausted"]]}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"safe_ess.skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["cap_up.item","safe_ess.skill","base_add.speed"]}}},"base_add.speed":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["cap_up.item","safe_ess.skill","base_add.speed"]}}}},"required":["safe_ess.skill","base_add.speed"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"safe_ess.skill":[["cap_up.item","safe_ess.skill","base_add.speed"],["cap_up.item","safe_ess.skill"],["safe_ess.skill","base_add.speed"]],"base_add.speed":[["cap_up.item","base_add.speed","safe_ess.skill"],["cap_up.item","base_add.speed"],["base_add.speed","safe_ess.skill"]]}
```

회수 2/2 · 위반 4 · 유효 중복 0 · 입력/출력 토큰 837/79.

<details>
<summary>모델 경로와 첫 위반 사유</summary>

```json
[[["cap_up.item","safe_ess.skill"],"missing speed"],[["safe_ess.skill","base_add.speed"],"Crafted capacity exhausted"],[["cap_up.item","base_add.speed"],"missing skill"],[["base_add.speed","safe_ess.skill"],"Crafted capacity exhausted"]]
```

</details>

### 8 · H3 · retry

기대 ["NONE"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"]}},"rules":["Crafted 한도를 올릴 수단이 없다."],"openers":["safe_ess.skill","safe_alloy.mana_skill"],"goals":{"mana_skill":["safe_alloy.mana_skill"],"skill":["safe_ess.skill"]},"strategies":{"safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요"},"helpers":[],"crafted":{"used":0,"uses":["safe_ess","safe_alloy"]},"validator":{"prior":{"safe_ess.skill":[["safe_ess.skill","safe_alloy.mana_skill"],["safe_alloy.mana_skill","safe_ess.skill"]],"safe_alloy.mana_skill":[["safe_alloy.mana_skill","safe_ess.skill"],["safe_ess.skill","safe_alloy.mana_skill"]]},"rejected":[[["safe_ess.skill","safe_alloy.mana_skill"],"Crafted capacity exhausted"],[["safe_alloy.mana_skill","safe_ess.skill"],"Crafted capacity exhausted"],[["safe_alloy.mana_skill","safe_ess.skill"],"Crafted capacity exhausted"],[["safe_ess.skill","safe_alloy.mana_skill"],"Crafted capacity exhausted"]]}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"safe_ess.skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","safe_alloy.mana_skill"]}}},"safe_alloy.mana_skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","safe_alloy.mana_skill"]}}}},"required":["safe_ess.skill","safe_alloy.mana_skill"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"safe_ess.skill":[],"safe_alloy.mana_skill":[]}
```

회수 1/1 · 위반 0 · 유효 중복 0 · 입력/출력 토큰 878/15.

### 9 · N · retry

기대 ["first_chaos.life safe_ess.def","base_add.life safe_ess.def","base_add.life whittle_miss.life safe_ess.def","reveal.life safe_ess.def","safe_ess.def base_add.life","safe_ess.def base_add.life whittle_miss.life","safe_ess.def reveal.life"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"strategies":{"first_chaos":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함. 첫 획득에서만. 분열 보존.","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","whittle_miss":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략. 같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."},"helpers":["whittle_miss.life"],"crafted":{"used":0,"uses":["safe_ess"]},"afterFirst":{"first_chaos.life":{"def":["safe_ess.def"]},"base_add.life":{"def":["safe_ess.def"]},"reveal.life":{"def":["safe_ess.def"]},"safe_ess.def":{"life":["first_chaos.life","base_add.life","reveal.life"]}},"validator":{"prior":{"first_chaos.life":{"hit":[["first_chaos.life","safe_ess.def"]]},"base_add.life":{"hit":[["base_add.life","safe_ess.def"]],"onMiss":[["base_add.life","whittle_miss.life","safe_ess.def"]]},"reveal.life":{"hit":[["reveal.life","safe_ess.def"]]},"safe_ess.def":{"hit":[["safe_ess.def","base_add.life"],["safe_ess.def","reveal.life"]],"onMiss":[["safe_ess.def","base_add.life","whittle_miss.life"],["safe_ess.def","reveal.life","whittle_miss.life"]]}},"rejected":[[["safe_ess.def","reveal.life","whittle_miss.life"],"recovery requires preceding base_add on same target"]]}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"first_chaos.life":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","safe_ess.def"]},"minItems":2,"maxItems":2}}},"required":["hit"],"additionalProperties":false},"base_add.life":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","safe_ess.def"]},"minItems":2,"maxItems":2}},"onMiss":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","safe_ess.def","whittle_miss.life"]},"minItems":3,"maxItems":3}}},"required":["hit","onMiss"],"additionalProperties":false},"reveal.life":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["reveal.life","safe_ess.def"]},"minItems":2,"maxItems":2}}},"required":["hit"],"additionalProperties":false},"safe_ess.def":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def","base_add.life","reveal.life"]},"minItems":2,"maxItems":2}},"onMiss":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def","base_add.life","reveal.life","whittle_miss.life"]},"minItems":3,"maxItems":3}}},"required":["hit","onMiss"],"additionalProperties":false}},"required":["first_chaos.life","base_add.life","reveal.life","safe_ess.def"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"first_chaos.life":{"hit":[["first_chaos.life","safe_ess.def"]]},"base_add.life":{"hit":[["base_add.life","safe_ess.def"]],"onMiss":[["base_add.life","whittle_miss.life","safe_ess.def"]]},"reveal.life":{"hit":[["reveal.life","safe_ess.def"]]},"safe_ess.def":{"hit":[["safe_ess.def","base_add.life"],["safe_ess.def","reveal.life"]],"onMiss":[["safe_ess.def","base_add.life","whittle_miss.life"],["safe_ess.def","reveal.life","whittle_miss.life"]]}}
```

회수 7/7 · 위반 1 · 유효 중복 0 · 입력/출력 토큰 1268/143.

<details>
<summary>모델 경로와 첫 위반 사유</summary>

```json
[[["safe_ess.def","reveal.life","whittle_miss.life"],"recovery requires preceding base_add on same target"]]
```

</details>

### 10 · G · retry

기대 ["upgrade_ess.life safe_alloy.pen","regal_base.life safe_alloy.pen"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Stocky Mitts","rarity":"magic","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"filler"}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body"],"pen":["Prismatic Alloy"]},"suffix":{}},"rules":[],"strategies":{"upgrade_ess":"등급 상승용 에센스로 목표 일반 옵션 획득. Magic → Rare","regal_base":"제왕으로 Magic을 Rare로 올리며 해당 일반 옵션이 뜬 결과 채택. Magic → Rare","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요"},"crafted":{"used":0,"uses":["safe_alloy"]},"afterFirst":{"upgrade_ess.life":{"pen":["safe_alloy.pen"]},"regal_base.life":{"pen":["safe_alloy.pen"]},"safe_alloy.pen":{"life":["upgrade_ess.life","regal_base.life"]}},"validator":{"prior":{"upgrade_ess.life":[["upgrade_ess.life","safe_alloy.pen"]],"regal_base.life":[["regal_base.life","safe_alloy.pen"]],"safe_alloy.pen":[["safe_alloy.pen","upgrade_ess.life"],["safe_alloy.pen","regal_base.life"]]},"rejected":[[["safe_alloy.pen","upgrade_ess.life"],"Rare required"],[["safe_alloy.pen","regal_base.life"],"Rare required"]]}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"upgrade_ess.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["upgrade_ess.life","safe_alloy.pen"]},"minItems":2,"maxItems":2}},"regal_base.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["regal_base.life","safe_alloy.pen"]},"minItems":2,"maxItems":2}},"safe_alloy.pen":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy.pen","upgrade_ess.life","regal_base.life"]},"minItems":2,"maxItems":2}}},"required":["upgrade_ess.life","regal_base.life","safe_alloy.pen"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"upgrade_ess.life":[["upgrade_ess.life","safe_alloy.pen"]],"regal_base.life":[["regal_base.life","safe_alloy.pen"]],"safe_alloy.pen":[]}
```

회수 2/2 · 위반 0 · 유효 중복 0 · 입력/출력 토큰 859/41.

### 11 · R · retry

기대 ["base_add.life base_add.cold","base_add.cold base_add.life","reveal.life base_add.cold","base_add.cold reveal.life","base_add.life reveal.cold","reveal.cold base_add.life"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"cold":["base","Preserved Collarbone"]}},"rules":["이 요청은 엑잘과 훼손 획득 경로를 비교한다. 확보한 목표를 제거하지 않는다."],"strategies":{"base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다."},"afterFirst":{"base_add.life":{"cold":["base_add.cold","reveal.cold"]},"reveal.life":{"cold":["base_add.cold","reveal.cold"]},"base_add.cold":{"life":["base_add.life","reveal.life"]},"reveal.cold":{"life":["base_add.life","reveal.life"]}},"validator":{"prior":{"base_add.life":[["base_add.life","base_add.cold"],["base_add.life","reveal.cold"]],"reveal.life":[["reveal.life","base_add.cold"],["reveal.life","reveal.cold"]],"base_add.cold":[["base_add.cold","base_add.life"],["base_add.cold","reveal.life"]],"reveal.cold":[["reveal.cold","base_add.life"],["reveal.cold","reveal.life"]]},"rejected":[[["reveal.life","reveal.cold"],"Desecrated slot occupied"],[["reveal.cold","reveal.life"],"Desecrated slot occupied"]]}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"base_add.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","base_add.cold","reveal.cold"]},"minItems":2,"maxItems":2}},"reveal.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["reveal.life","base_add.cold","reveal.cold"]},"minItems":2,"maxItems":2}},"base_add.cold":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.cold","base_add.life","reveal.life"]},"minItems":2,"maxItems":2}},"reveal.cold":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["reveal.cold","base_add.life","reveal.life"]},"minItems":2,"maxItems":2}}},"required":["base_add.life","reveal.life","base_add.cold","reveal.cold"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"base_add.life":[["base_add.life","base_add.cold"],["base_add.life","reveal.cold"]],"reveal.life":[["reveal.life","base_add.cold"]],"base_add.cold":[["base_add.cold","base_add.life"],["base_add.cold","reveal.life"]],"reveal.cold":[["reveal.cold","base_add.life"]]}
```

회수 6/6 · 위반 0 · 유효 중복 0 · 입력/출력 토큰 956/83.

### 12 · H4 · retry

기대 ["flesh.item divine.item","divine.item flesh.item"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","raw":99}],"suffix":[{"id":"str","raw":7}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"rules":["두 옵션은 보유. Flesh20과 원래 굴림 99·8이면 합 127로 완성. 현재 품질 0, 99·7은 품질만 올리면 126으로 미달. 품질과 재굴림은 어느 순서도 가능."],"strategies":{"flesh":"Flesh 품질 20 적용. 기존 수치 유지. 대상 item","divine":"두 옵션 수치를 현재 티어 안에서 함께 굴린다. 변경 가능 아이템, 대상 item"},"afterFirst":{"flesh.item":{"values":["divine.item"]},"divine.item":{"Flesh20":["flesh.item"]}},"validator":{"prior":{"flesh.item":[["flesh.item","divine.item"],["divine.item","flesh.item"]],"divine.item":[["divine.item","flesh.item"]]},"rejected":[[["divine.item","flesh.item"],"first acquisition disagrees with group"]]}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"flesh.item":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["flesh.item","divine.item"]},"minItems":2,"maxItems":2}},"divine.item":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.item","flesh.item"]},"minItems":2,"maxItems":2}}},"required":["flesh.item","divine.item"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"flesh.item":[["flesh.item","divine.item"],["divine.item","flesh.item"]],"divine.item":[["divine.item","flesh.item"]]}
```

회수 2/2 · 위반 1 · 유효 중복 0 · 입력/출력 토큰 794/38.

<details>
<summary>모델 경로와 첫 위반 사유</summary>

```json
[[["divine.item","flesh.item"],"first acquisition disagrees with group"]]
```

</details>

### 13 · H2 · draft

기대 ["safe_ess.skill base_add.speed","base_add.speed safe_ess.skill"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"needOnePerRow":{"skill":["safe_ess.skill"],"speed":["base_add.speed"]},"strategies":{"cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare"},"rules":[],"slots":{"craftedUsed":1,"limitNow":1,"additionalCraftedNeededAtLeast":1,"expandBy":1,"desecratedUsed":0}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"recipes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["cap_up.item","safe_ess.skill","base_add.speed"]}}}},"required":["recipes"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"recipes":[["cap_up.item","safe_ess.skill","base_add.speed"],["cap_up.item","base_add.speed"]]}
```

회수 1/2 · 위반 1 · 유효 중복 0 · 입력/출력 토큰 573/27.

누락 ["base_add.speed safe_ess.skill"]

<details>
<summary>모델 경로와 첫 위반 사유</summary>

```json
[[["cap_up.item","base_add.speed"],"missing skill"]]
```

</details>

### 14 · H3 · draft

기대 ["NONE"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"]}},"needOnePerRow":{"mana_skill":["safe_alloy.mana_skill"],"skill":["safe_ess.skill"]},"strategies":{"safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요"},"rules":["Crafted 한도를 올릴 수단이 없다."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":2,"expandBy":0,"desecratedUsed":0}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"recipes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","safe_alloy.mana_skill"]}}}},"required":["recipes"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"recipes":[]}
```

회수 1/1 · 위반 0 · 유효 중복 0 · 입력/출력 토큰 556/4.

### 15 · N · draft

기대 ["first_chaos.life safe_ess.def","base_add.life safe_ess.def","base_add.life whittle_miss.life safe_ess.def","reveal.life safe_ess.def","safe_ess.def base_add.life","safe_ess.def base_add.life whittle_miss.life","safe_ess.def reveal.life"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"needOnePerRow":{"life":["first_chaos.life","base_add.life","reveal.life"],"def":["safe_ess.def"]},"strategies":{"first_chaos":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함. 첫 획득에서만. 분열 보존.","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","whittle_miss":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략. 같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."},"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":1,"expandBy":0,"desecratedUsed":0}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"recipes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","base_add.life","reveal.life","safe_ess.def","whittle_miss.life"]}}}},"required":["recipes"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"recipes":[["first_chaos.life","safe_ess.def","base_add.life"],["first_chaos.life","safe_ess.def","reveal.life"]]}
```

회수 0/7 · 위반 2 · 유효 중복 0 · 입력/출력 토큰 749/36.

누락 ["base_add.life safe_ess.def","base_add.life whittle_miss.life safe_ess.def","first_chaos.life safe_ess.def","reveal.life safe_ess.def","safe_ess.def base_add.life","safe_ess.def base_add.life whittle_miss.life","safe_ess.def reveal.life"]

<details>
<summary>모델 경로와 첫 위반 사유</summary>

```json
[[["first_chaos.life","safe_ess.def","base_add.life"],"target already covered"],[["first_chaos.life","safe_ess.def","reveal.life"],"target already covered"]]
```

</details>

### 16 · G · draft

기대 ["upgrade_ess.life safe_alloy.pen","regal_base.life safe_alloy.pen"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Stocky Mitts","rarity":"magic","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"filler"}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body"],"pen":["Prismatic Alloy"]},"suffix":{}},"needOnePerRow":{"life":["upgrade_ess.life","regal_base.life"],"pen":["safe_alloy.pen"]},"strategies":{"upgrade_ess":"등급 상승용 에센스로 목표 일반 옵션 획득. Magic → Rare","regal_base":"제왕으로 Magic을 Rare로 올리며 해당 일반 옵션이 뜬 결과 채택. Magic → Rare","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요"},"rules":[],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":1,"expandBy":0,"desecratedUsed":0}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"recipes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["upgrade_ess.life","regal_base.life","safe_alloy.pen"]}}}},"required":["recipes"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"recipes":[["upgrade_ess.life","safe_alloy.pen"]]}
```

회수 1/2 · 위반 0 · 유효 중복 0 · 입력/출력 토큰 574/15.

누락 ["regal_base.life safe_alloy.pen"]

### 17 · R · draft

기대 ["base_add.life base_add.cold","base_add.cold base_add.life","reveal.life base_add.cold","base_add.cold reveal.life","base_add.life reveal.cold","reveal.cold base_add.life"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"cold":["base","Preserved Collarbone"]}},"needOnePerRow":{"life":["base_add.life","reveal.life"],"cold":["base_add.cold","reveal.cold"]},"strategies":{"base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다."},"rules":["이 요청은 엑잘과 훼손 획득 경로를 비교한다. 확보한 목표를 제거하지 않는다."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":0}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"recipes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","reveal.life","base_add.cold","reveal.cold"]}}}},"required":["recipes"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"recipes":[["base_add.life","reveal.life","base_add.cold","reveal.cold"],["reveal.life","base_add.life","base_add.cold","reveal.cold"],["base_add.life","reveal.life","reveal.cold","base_add.cold"],["reveal.life","base_add.life","reveal.cold","base_add.cold"]]}
```

회수 0/6 · 위반 4 · 유효 중복 0 · 입력/출력 토큰 582/80.

누락 ["base_add.cold base_add.life","base_add.cold reveal.life","base_add.life base_add.cold","base_add.life reveal.cold","reveal.cold base_add.life","reveal.life base_add.cold"]

<details>
<summary>모델 경로와 첫 위반 사유</summary>

```json
[[["base_add.life","reveal.life","base_add.cold","reveal.cold"],"target already covered"],[["reveal.life","base_add.life","base_add.cold","reveal.cold"],"target already covered"],[["base_add.life","reveal.life","reveal.cold","base_add.cold"],"target already covered"],[["reveal.life","base_add.life","reveal.cold","base_add.cold"],"target already covered"]]
```

</details>

### 18 · H4 · draft

기대 ["flesh.item divine.item","divine.item flesh.item"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","raw":99}],"suffix":[{"id":"str","raw":7}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"needOnePerRow":{"Flesh20":["flesh.item"],"values":["divine.item"]},"strategies":{"flesh":"Flesh 품질 20 적용. 기존 수치 유지. 대상 item","divine":"두 옵션 수치를 현재 티어 안에서 함께 굴린다. 변경 가능 아이템, 대상 item"},"rules":["두 옵션은 보유. Flesh20과 원래 굴림 99·8이면 합 127로 완성. 현재 품질 0, 99·7은 품질만 올리면 126으로 미달. 품질과 재굴림은 어느 순서도 가능."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":0}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"recipes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["flesh.item","divine.item"]}}}},"required":["recipes"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"recipes":[["flesh.item","divine.item"]]}
```

회수 1/2 · 위반 0 · 유효 중복 0 · 입력/출력 토큰 615/13.

누락 ["divine.item flesh.item"]

### 19 · H2 · final

기대 ["safe_ess.skill base_add.speed","base_add.speed safe_ess.skill"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"strategies":{"cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare"},"rules":[],"slots":{"craftedUsed":1,"limitNow":1,"additionalCraftedNeededAtLeast":1,"expandBy":1,"desecratedUsed":0},"requiredRows":["skill","speed"],"afterFirst":{"safe_ess.skill":{"speed":["base_add.speed"]},"base_add.speed":{"skill":["safe_ess.skill"]}}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"safe_ess.skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","base_add.speed","cap_up.item"]},"minItems":3,"maxItems":3}},"base_add.speed":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.speed","safe_ess.skill","cap_up.item"]},"minItems":3,"maxItems":3}}},"required":["safe_ess.skill","base_add.speed"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"safe_ess.skill":[["safe_ess.skill","cap_up.item","base_add.speed"],["safe_ess.skill","base_add.speed","cap_up.item"],["safe_ess.skill","base_add.speed","base_add.speed"]],"base_add.speed":[["base_add.speed","cap_up.item","safe_ess.skill"],["base_add.speed","safe_ess.skill","cap_up.item"],["base_add.speed","safe_ess.skill","safe_ess.skill"]]}
```

회수 1/2 · 위반 5 · 유효 중복 0 · 입력/출력 토큰 694/97.

누락 ["safe_ess.skill base_add.speed"]

<details>
<summary>모델 경로와 첫 위반 사유</summary>

```json
[[["safe_ess.skill","cap_up.item","base_add.speed"],"Crafted capacity exhausted"],[["safe_ess.skill","base_add.speed","cap_up.item"],"Crafted capacity exhausted"],[["safe_ess.skill","base_add.speed","base_add.speed"],"Crafted capacity exhausted"],[["base_add.speed","safe_ess.skill","cap_up.item"],"Crafted capacity exhausted"],[["base_add.speed","safe_ess.skill","safe_ess.skill"],"Crafted capacity exhausted"]]
```

</details>

### 20 · H3 · final

기대 ["NONE"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"]}},"strategies":{"safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요"},"rules":["Crafted 한도를 올릴 수단이 없다."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":2,"expandBy":0,"desecratedUsed":0},"requiredRows":["mana_skill","skill"],"afterFirst":{"safe_alloy.mana_skill":{"skill":["safe_ess.skill"]},"safe_ess.skill":{"mana_skill":["safe_alloy.mana_skill"]}}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"safe_alloy.mana_skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill"]},"minItems":2,"maxItems":2}},"safe_ess.skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","safe_alloy.mana_skill"]},"minItems":2,"maxItems":2}}},"required":["safe_alloy.mana_skill","safe_ess.skill"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"safe_alloy.mana_skill":[["safe_alloy.mana_skill","safe_ess.skill"],["safe_alloy.mana_skill","safe_ess.skill"]],"safe_ess.skill":[["safe_ess.skill","safe_alloy.mana_skill"],["safe_ess.skill","safe_alloy.mana_skill"]]}
```

회수 0/1 · 위반 4 · 유효 중복 0 · 입력/출력 토큰 683/67.

누락 ["NONE"]

<details>
<summary>모델 경로와 첫 위반 사유</summary>

```json
[[["safe_alloy.mana_skill","safe_ess.skill"],"Crafted capacity exhausted"],[["safe_alloy.mana_skill","safe_ess.skill"],"Crafted capacity exhausted"],[["safe_ess.skill","safe_alloy.mana_skill"],"Crafted capacity exhausted"],[["safe_ess.skill","safe_alloy.mana_skill"],"Crafted capacity exhausted"]]
```

</details>

### 21 · N · final

기대 ["first_chaos.life safe_ess.def","base_add.life safe_ess.def","base_add.life whittle_miss.life safe_ess.def","reveal.life safe_ess.def","safe_ess.def base_add.life","safe_ess.def base_add.life whittle_miss.life","safe_ess.def reveal.life"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"strategies":{"first_chaos":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함. 첫 획득에서만. 분열 보존.","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","whittle_miss":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략. 같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."},"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":1,"expandBy":0,"desecratedUsed":0},"requiredRows":["life","def"],"afterFirst":{"first_chaos.life":{"def":["safe_ess.def"]},"base_add.life":{"def":["safe_ess.def"]},"reveal.life":{"def":["safe_ess.def"]},"safe_ess.def":{"life":["base_add.life","reveal.life"]}}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"first_chaos.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","safe_ess.def"]},"minItems":2,"maxItems":2}},"base_add.life":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","safe_ess.def"]},"minItems":2,"maxItems":2}},"onMiss":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","safe_ess.def","whittle_miss.life"]},"minItems":3,"maxItems":3}}},"required":["hit","onMiss"],"additionalProperties":false},"reveal.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["reveal.life","safe_ess.def"]},"minItems":2,"maxItems":2}},"safe_ess.def":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def","base_add.life","reveal.life"]},"minItems":2,"maxItems":2}},"onMiss":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def","base_add.life","whittle_miss.life"]},"minItems":3,"maxItems":3}}},"required":["hit","onMiss"],"additionalProperties":false}},"required":["first_chaos.life","base_add.life","reveal.life","safe_ess.def"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"first_chaos.life":[["first_chaos.life","safe_ess.def"]],"base_add.life":{"hit":[["base_add.life","safe_ess.def"]],"onMiss":[["base_add.life","whittle_miss.life","safe_ess.def"]]},"reveal.life":[["reveal.life","safe_ess.def"]],"safe_ess.def":{"hit":[["safe_ess.def","base_add.life"],["safe_ess.def","reveal.life"]],"onMiss":[["safe_ess.def","base_add.life","whittle_miss.life"],["safe_ess.def","whittle_miss.life","base_add.life"]]}}
```

회수 7/7 · 위반 1 · 유효 중복 0 · 입력/출력 토큰 1038/137.

<details>
<summary>모델 경로와 첫 위반 사유</summary>

```json
[[["safe_ess.def","whittle_miss.life","base_add.life"],"recovery requires preceding base_add on same target"]]
```

</details>

### 22 · G · final

기대 ["upgrade_ess.life safe_alloy.pen","regal_base.life safe_alloy.pen"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Stocky Mitts","rarity":"magic","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"filler"}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body"],"pen":["Prismatic Alloy"]},"suffix":{}},"strategies":{"upgrade_ess":"등급 상승용 에센스로 목표 일반 옵션 획득. Magic → Rare","regal_base":"제왕으로 Magic을 Rare로 올리며 해당 일반 옵션이 뜬 결과 채택. Magic → Rare","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요"},"rules":[],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":1,"expandBy":0,"desecratedUsed":0},"requiredRows":["life","pen"],"afterFirst":{"upgrade_ess.life":{"pen":["safe_alloy.pen"]},"regal_base.life":{"pen":["safe_alloy.pen"]}}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"upgrade_ess.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["upgrade_ess.life","safe_alloy.pen"]},"minItems":2,"maxItems":2}},"regal_base.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["regal_base.life","safe_alloy.pen"]},"minItems":2,"maxItems":2}}},"required":["upgrade_ess.life","regal_base.life"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"upgrade_ess.life":[["upgrade_ess.life","safe_alloy.pen"]],"regal_base.life":[["regal_base.life","safe_alloy.pen"]]}
```

회수 2/2 · 위반 0 · 유효 중복 0 · 입력/출력 토큰 684/35.

### 23 · R · final

기대 ["base_add.life base_add.cold","base_add.cold base_add.life","reveal.life base_add.cold","base_add.cold reveal.life","base_add.life reveal.cold","reveal.cold base_add.life"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"cold":["base","Preserved Collarbone"]}},"strategies":{"base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다."},"rules":["이 요청은 엑잘과 훼손 획득 경로를 비교한다. 확보한 목표를 제거하지 않는다."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":0},"requiredRows":["life","cold"],"afterFirst":{"base_add.life":{"cold":["base_add.cold","reveal.cold"]},"reveal.life":{"cold":["base_add.cold"]},"base_add.cold":{"life":["base_add.life","reveal.life"]},"reveal.cold":{"life":["base_add.life"]}}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"base_add.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","base_add.cold","reveal.cold"]},"minItems":2,"maxItems":2}},"reveal.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["reveal.life","base_add.cold"]},"minItems":2,"maxItems":2}},"base_add.cold":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.cold","base_add.life","reveal.life"]},"minItems":2,"maxItems":2}},"reveal.cold":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["reveal.cold","base_add.life"]},"minItems":2,"maxItems":2}}},"required":["base_add.life","reveal.life","base_add.cold","reveal.cold"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"base_add.life":[["base_add.life","base_add.cold"],["base_add.life","reveal.cold"],["base_add.life","reveal.cold"]],"reveal.life":[["reveal.life","base_add.cold"]],"base_add.cold":[["base_add.cold","base_add.life"],["base_add.cold","reveal.life"]],"reveal.cold":[["reveal.cold","base_add.life"]]}
```

회수 6/6 · 위반 0 · 유효 중복 1 · 입력/출력 토큰 790/93.

### 24 · H4 · final

기대 ["flesh.item divine.item","divine.item flesh.item"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","raw":99}],"suffix":[{"id":"str","raw":7}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"strategies":{"flesh":"Flesh 품질 20 적용. 기존 수치 유지. 대상 item","divine":"두 옵션 수치를 현재 티어 안에서 함께 굴린다. 변경 가능 아이템, 대상 item"},"rules":["두 옵션은 보유. Flesh20과 원래 굴림 99·8이면 합 127로 완성. 현재 품질 0, 99·7은 품질만 올리면 126으로 미달. 품질과 재굴림은 어느 순서도 가능."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":0},"requiredRows":["Flesh20","values"],"afterFirst":{"flesh.item":{"values":["divine.item"]},"divine.item":{"Flesh20":["flesh.item"]}}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"flesh.item":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["flesh.item","divine.item"]},"minItems":2,"maxItems":2}},"divine.item":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.item","flesh.item"]},"minItems":2,"maxItems":2}}},"required":["flesh.item","divine.item"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"flesh.item":[["flesh.item","divine.item"]],"divine.item":[["divine.item","flesh.item"]]}
```

회수 2/2 · 위반 0 · 유효 중복 0 · 입력/출력 토큰 731/29.

### 25 · W · final

기대 ["safe_alloy.mana_skill safe_ess.skill base_add.speed","safe_alloy.mana_skill base_add.speed safe_ess.skill","safe_ess.skill safe_alloy.mana_skill base_add.speed","safe_ess.skill base_add.speed safe_alloy.mana_skill","base_add.speed safe_alloy.mana_skill safe_ess.skill","base_add.speed safe_ess.skill safe_alloy.mana_skill"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"strategies":{"safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item."},"rules":["mana_skill과 skill은 서로 다른 Crafted. speed는 일반 접미. 세 옵션 모두 보존. 두 번째 Crafted 전 한도 확장 필수."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":2,"expandBy":1,"desecratedUsed":0},"requiredRows":["mana_skill","skill","speed"],"afterFirst":{"safe_alloy.mana_skill":{"skill":["safe_ess.skill"],"speed":["base_add.speed"]},"safe_ess.skill":{"mana_skill":["safe_alloy.mana_skill"],"speed":["base_add.speed"]},"base_add.speed":{"mana_skill":["safe_alloy.mana_skill"],"skill":["safe_ess.skill"]}}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"safe_alloy.mana_skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed","cap_up.item"]},"minItems":4,"maxItems":4}},"safe_ess.skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","safe_alloy.mana_skill","base_add.speed","cap_up.item"]},"minItems":4,"maxItems":4}},"base_add.speed":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.speed","safe_alloy.mana_skill","safe_ess.skill","cap_up.item"]},"minItems":4,"maxItems":4}}},"required":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"safe_alloy.mana_skill":[["safe_alloy.mana_skill","cap_up.item","safe_ess.skill","base_add.speed"],["safe_alloy.mana_skill","cap_up.item","safe_ess.skill","base_add.speed"]],"safe_ess.skill":[["safe_ess.skill","cap_up.item","safe_alloy.mana_skill","base_add.speed"],["safe_ess.skill","cap_up.item","safe_alloy.mana_skill","base_add.speed"]],"base_add.speed":[["base_add.speed","cap_up.item","safe_alloy.mana_skill","safe_ess.skill"],["base_add.speed","cap_up.item","safe_ess.skill","safe_alloy.mana_skill"]]}
```

회수 4/6 · 위반 0 · 유효 중복 2 · 입력/출력 토큰 860/146.

누락 ["safe_alloy.mana_skill base_add.speed safe_ess.skill","safe_ess.skill base_add.speed safe_alloy.mana_skill"]

### 26 · H1 · final

기대 ["safe_ess.def"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","value":90}],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"strategies":{"safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요"},"rules":[],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":1,"expandBy":0,"desecratedUsed":0},"requiredRows":["def"],"afterFirst":{"safe_ess.def":{}}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"safe_ess.def":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def"]},"minItems":1,"maxItems":1}}},"required":["safe_ess.def"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"safe_ess.def":[["safe_ess.def"]]}
```

회수 1/1 · 위반 0 · 유효 중복 0 · 입력/출력 토큰 614/13.

### 27 · Q1 · final

기대 ["NONE"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"strategies":{"safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare"},"rules":[],"slots":{"craftedUsed":1,"limitNow":1,"additionalCraftedNeededAtLeast":1,"expandBy":0,"desecratedUsed":0},"requiredRows":["skill","speed"],"afterFirst":{"safe_ess.skill":{"speed":["base_add.speed"]},"base_add.speed":{"skill":["safe_ess.skill"]}}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"safe_ess.skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","base_add.speed"]},"minItems":2,"maxItems":2}},"base_add.speed":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.speed","safe_ess.skill"]},"minItems":2,"maxItems":2}}},"required":["safe_ess.skill","base_add.speed"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"safe_ess.skill":[["safe_ess.skill","base_add.speed"],["base_add.speed","safe_ess.skill"]],"base_add.speed":[["base_add.speed","safe_ess.skill"],["safe_ess.skill","base_add.speed"]]}
```

회수 0/1 · 위반 4 · 유효 중복 0 · 입력/출력 토큰 653/52.

누락 ["NONE"]

<details>
<summary>모델 경로와 첫 위반 사유</summary>

```json
[[["safe_ess.skill","base_add.speed"],"Crafted capacity exhausted"],[["base_add.speed","safe_ess.skill"],"Crafted capacity exhausted"],[["base_add.speed","safe_ess.skill"],"Crafted capacity exhausted"],[["safe_ess.skill","base_add.speed"],"Crafted capacity exhausted"]]
```

</details>

### 28 · Q2 · final

기대 ["safe_alloy.mana_skill safe_ess.skill","safe_ess.skill safe_alloy.mana_skill"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"]}},"strategies":{"safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요","cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item."},"rules":[],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":2,"expandBy":1,"desecratedUsed":0},"requiredRows":["mana_skill","skill"],"afterFirst":{"safe_alloy.mana_skill":{"skill":["safe_ess.skill"]},"safe_ess.skill":{"mana_skill":["safe_alloy.mana_skill"]}}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"safe_alloy.mana_skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill","cap_up.item"]},"minItems":3,"maxItems":3}},"safe_ess.skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","safe_alloy.mana_skill","cap_up.item"]},"minItems":3,"maxItems":3}}},"required":["safe_alloy.mana_skill","safe_ess.skill"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"safe_alloy.mana_skill":[["safe_alloy.mana_skill","cap_up.item","safe_ess.skill"],["safe_alloy.mana_skill","safe_ess.skill","cap_up.item"],["safe_alloy.mana_skill","cap_up.item","safe_ess.skill"],["safe_alloy.mana_skill","safe_ess.skill","cap_up.item"]],"safe_ess.skill":[["safe_ess.skill","cap_up.item","safe_alloy.mana_skill"],["safe_ess.skill","safe_alloy.mana_skill","cap_up.item"],["safe_ess.skill","cap_up.item","safe_alloy.mana_skill"],["safe_ess.skill","safe_alloy.mana_skill","cap_up.item"]]}
```

회수 2/2 · 위반 4 · 유효 중복 2 · 입력/출력 토큰 713/151.

<details>
<summary>모델 경로와 첫 위반 사유</summary>

```json
[[["safe_alloy.mana_skill","safe_ess.skill","cap_up.item"],"Crafted capacity exhausted"],[["safe_alloy.mana_skill","safe_ess.skill","cap_up.item"],"Crafted capacity exhausted"],[["safe_ess.skill","safe_alloy.mana_skill","cap_up.item"],"Crafted capacity exhausted"],[["safe_ess.skill","safe_alloy.mana_skill","cap_up.item"],"Crafted capacity exhausted"]]
```

</details>

### 29 · Q3 · final

기대 ["base_add.cold"]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","desecrated":true}],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"cold":["base","Preserved Collarbone"]}},"strategies":{"base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다."},"rules":["이 요청은 엑잘과 훼손 획득 경로를 비교한다. 확보한 목표를 제거하지 않는다."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":1},"requiredRows":["cold"],"afterFirst":{"base_add.cold":{}}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"base_add.cold":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.cold"]},"minItems":1,"maxItems":1}}},"required":["base_add.cold"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"base_add.cold":[["base_add.cold"]]}
```

회수 1/1 · 위반 0 · 유효 중복 0 · 입력/출력 토큰 637/13.

### 30 · Q4 · final

기대 [""]

<details>
<summary>실제 전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","value":90},{"id":"def","crafted":true}],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"strategies":{},"rules":[],"slots":{"craftedUsed":1,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":0},"requiredRows":[],"afterFirst":{}}
```

</details>

<details>
<summary>실제 응답 스키마</summary>

```json
{"type":"object","properties":{"recipes":{"type":"array","items":{"type":"array","items":{"type":"string"}}}},"required":["recipes"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"recipes":[[]]}
```

회수 1/1 · 위반 0 · 유효 중복 0 · 입력/출력 토큰 568/5.
