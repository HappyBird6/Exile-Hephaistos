# 제작 순서 프롬프트 실험 · 2026-11-23

## 결과

10개 시나리오, 4개 베이스, 목표 1~3개로 OpenAI Chat Completions에 독립 요청 20회를 보냈다. 같은 모델 `gpt-4.1-mini-2025-04-14`, temperature 0, 출력 한도 3,200토큰, store false를 사용했다. 백엔드 출처와 행동을 시나리오별로 선별한 입력만 user 메시지에 넣었고, 버전별 공통 문안은 system 메시지로 보냈다. 전체 입력 11,889토큰, 출력 1,023토큰이며 20건 모두 정상 응답했다. 아래 실제 응답은 API가 반환한 메시지 본문이다.

| 단계 | 호출 수 | 사전 기대 방향 회수 | 해석 |
|---|---:|---:|---|
| 초안 S1~S8 | 8 | 3/22 | S3에서 두 등급 상승 대안을 모두 회수했다. 대안과 단계 혼동, 불법 중복, 한도 선행 누락이 관측됐다. |
| 1차 수정 S1~S8 | 8 | 1/22 | 세 겹 배열 형식은 6/8에서 8/8로 개선됐지만, 같은 입력에서 유효한 기대 방향은 줄었다. |
| 1차 수정의 미사용 입력 S9~S10 | 2 | 1/2 | 축성 미달은 맞았고, 이미 합금을 보유한 마법봉은 실패했다. |
| 최종 수정 S2·S4 | 2 | 5/9 | 같은 두 사례의 이전 두 버전은 각각 0/9. 대상이 고정된 행동 사전으로 상태 오류가 줄었지만 S2는 1/5에 그쳤다. |

회수율은 응답을 수정하거나 잘못된 배열을 자동 보정하지 않고, 처음 적은 기대 경로를 포함하는지 비교한 값이다. S4의 한도 확장 위치만 위상 순서에 따라 동등하게 처리했다. 실제 응답 중 사전 기대에 없는 후보는 점수 분자에 추가하지 않았다. 각 호출은 단발이며 최종 버전은 어려웠던 두 사례만 다시 실행했다. 이번 실험은 프롬프트의 행동 순서와 알려진 전제를 검토했고, 모든 반환 경로를 제작 엔진으로 재생하지는 않았다.

**판단.** 호출 상한에서 중단했다. 다음 검증의 기준은 마지막으로 실제 호출한 아래 최종 문안이다. 특히 S2의 누락이 남아 있어 전 범위 적용을 확정할 수 없다. 후속 개선 가설은 **시작 가능한 서로 다른 행동·대상 쌍별로 완성 후보를 한 번씩 탐색한 뒤 중복을 합치는 것**이다. 이는 이번 20회에 시험한 지시문과 구분한다. plan.start도 신뢰할 수 있는 기존 판정이 있을 때만 done 또는 blocked로 보내며, 미확정은 open으로 둔다.

## 비교 방법

새 대화 한 번에 공통 지시문과 아래 시나리오 입력 하나만 보낸다. 기대 응답과 다른 시나리오의 응답은 보내지 않는다. 출처는 주어진 목표·선택한 행동 범위에서 비교한다. 장비·화폐는 저장소 카탈로그를 따르고 시작 아이템은 실험용이다. [설계 원칙](recipe-candidate-rules.md) · [품질·가중합 실측](card-recipe-json-draft.md)

첫 호출 전에 아래 기대 경로를 고정했다. 각 줄은 하나의 후보이며 `행동.대상`을 JSON의 `[행동, 대상]`으로 읽는다. DONE은 `[[]]`, NONE은 `[]`이다. 기대 목록은 관측하려는 방향이며 가능한 모든 레시피의 목록은 아니다. S4에서는 한도 확장이 두 번째 Crafted 획득 전에 있는 합법적인 위치를 모두 동등하게 인정하고 표에 적은 세 옵션의 획득 순서로 비교한다. 기대 밖의 적법한 추가 후보와 잘못된 후보를 구별한다.

S1~S8을 초안과 수정안에 각각 독립 요청한다. S9~S10은 수정안에만 사용할 미사용 입력이다. 최대 20회이며 기본 비교는 18회다. 같은 응답의 여러 후보는 한 호출로 센다.

## 최초 공통 프롬프트

```text
너는 PoE2 제작 순서 플래너다. 입력의 고정 시작 아이템에서 목표를 만드는 서로 다른 순서를 제안한다. 가능성, 무작위 결과, 비용은 백엔드가 검증한다.
1. start.mods와 targets의 접두·접미를 비교해 보유 목표를 살린다. 이미 완성은 [[]], 후보 없음은 []다.
2. 전용 출처가 필요한 목표에 수단을 배정하고, 다른 목표는 가능한 여러 출처와 선취득 순서를 검토한다. 희귀 옵션이나 다른 유효 옵션이 먼저 붙는 경우도 남긴다.
3. baseRules와 rules를 따르고 제공된 strategies만 쓴다. 준비·보존·조건부 복구가 달라지면 별도 후보로 남긴다. 같은 전략의 대체 재료는 백엔드가 펼친다.
4. 응답은 후보별 [전략 ID, 대상 ID] 단계로 구성된 JSON 배열만 반환한다. 옵션 대상은 targets 키, 아이템 전체 작업은 "item"을 쓴다.
```

## 수정 공통 프롬프트

초안의 실제 결과를 읽고, 재시험 전에 다음 지시문을 고정했다. 변경 근거는 대안과 단계 혼동(S1·S2·S5), 한도 확장 지연(S4), 이미 완성/불가 혼동(S6·S7), 순서 편향(S8)이었다. 입력과 기대 경로는 그대로 유지했다.

```text
너는 PoE2 제작 순서 플래너다. 이 요청의 start를 고정하고 targets를 함께 달성하는 서로 다른 후보를 제안한다. 입력의 strategies·baseRules·rules만 사용한다. 확률·비용·세부 난수 분기는 백엔드가 계산한다.

1. 먼저 현재 옵션과 수치·품질까지 읽어라. 이미 완성이면 즉시 [[]]. 불변 상태·분열 계열 충돌 등으로 모든 경로가 명백히 막혔으면 []. 단지 어렵거나 확률을 모르는 경우는 배제 근거가 아니다.
2. 미확보 목표마다 출처에 대응하는 시작 행동을 하나씩 바꿔 본다. 같은 목표를 얻는 서로 다른 방법은 별도 후보이며, 이미 확보한 같은 옵션을 다시 붙이지 않는다. 일반 옵션 A와 B 중 어느 쪽도 먼저 나올 수 있다면 A→B와 B→A를 따로 검토한다. 준비가 가능한 출처도 포함한다.
3. 후보 한 줄을 처음부터 따라가며 현재 희귀도·빈 접두/접미·분열·Crafted 사용 수와 한도를 갱신한다. 행동의 자격은 실행 직전에 만족해야 한다. 한도를 초과할 획득 전에 확장 행동을 배치한다. 회수가 불가능한 기존 목표를 희생하거나 목표가 빠진 미완성 줄은 버린다.
4. 먼저 얻는 옵션, 출처, 보존 방법, 적법한 실패 복구가 달라지면 별도 후보로 남긴다. 복구 전용 행동은 대응하는 시도의 실패 직후에만 붙인다. 품질과 수치만 남으면 가능한 작업 순서를 비교한다. 재료 이름만 다른 같은 방향은 합친다.
5. 출력은 JSON 배열 하나다. 바깥 배열=대안들, 안쪽 배열=완성까지의 단계들, 각 단계=[허용된 전략 ID, 목표 ID 또는 "item"]. 한 단계짜리 대안 두 개의 모양은 [[["actionA","x"]],[["actionB","x"]]]이다. 예시 ID는 출력하지 않는다. 마크다운·설명 없이 반환한다.
```

## 최종 공통 프롬프트

18회까지의 결과에서 자연어로 행동 자격을 강조해도 이미 확보한 힘을 다시 붙이거나 옵션용 행동을 item에 적용하는 오류가 남았다. 마지막 두 호출은 기존 S2와 S4의 동일한 목표·기대 목록에 아래 plan 요약을 추가한다. 같은 모델에서 프롬프트와 입력 표현을 함께 바꾸는 실험이다. plan에는 개별 행동의 대상과 전제만 넣으며 기대 레시피 순서는 전송하지 않는다.

```text
너는 PoE2 제작 후보를 찾는다. 목표는 시작 아이템에서 미달 목표를 모두 확보하는 행동 순서다. 확률·비용은 백엔드가 계산한다.

입력의 plan은 백엔드가 출처와 시작 상태로 만들어 준 행동 사전이다. plan.start가 done이면 [[]], blocked이면 []를 즉시 반환한다. 그 외에는 plan.need에서 시작한다. held는 이미 확보한 옵션이므로 다시 얻지 않는다.

각 후보를 빈 순서에서 만든다. 다음 행동은 plan.moves에서 대상까지 그대로 복사한다. 괄호 안 두 문자열을 교체하거나 새 쌍을 만들지 않는다. acquire 행동 하나가 성공하면 해당 대상 하나를 need에서 지운다. 같은 목표를 얻는 다른 행동은 다른 후보의 시작 또는 후속으로 고려한다. 서로 다른 목표가 먼저 붙는 두 순서도 검토한다.

매 단계 전에 plan.gates와 baseRules를 적용한다. Crafted 자리가 없으면 두 번째 전용 옵션을 붙이기 전에 cap_up을 실행한다. 한도 확장은 미리 해도 된다. 준비·품질·수치 조정은 명시된 효과만 적용한다. 조건부 복구는 같은 대상의 시도 바로 뒤에 대안으로 붙이며 성공 경로에 필수로 넣지 않는다. 모든 need가 충족되면 즉시 그 후보를 닫는다. 아직 목표가 빠진 줄, 실행 전제가 안 맞는 줄은 내보내지 않는다.

JSON 배열만 반환한다. 각 후보는 행동 쌍의 배열이다. 모양은 [[["행동1","대상1"]],[["행동2","대상1"]]]이다. 이미 완성=[[]], 가능한 후보를 제안할 수 없음=[].
```

### S2 추가 입력

```json
{"plan":{"start":"open","held":["strength"],"need":["life","def"],"moves":{"acquire":[["first_chaos","life"],["base_add","life"],["reveal","life"],["safe_ess","def"]],"onMiss":[["whittle_miss","life"]]},"gates":["first_chaos는 첫 획득만 가능. whittle_miss는 같은 대상의 base_add 바로 뒤에만 선택 가능."]}}
```

### S4 추가 입력

```json
{"plan":{"start":"open","held":[],"need":["mana_skill","skill","speed"],"moves":{"acquire":[["safe_alloy","mana_skill"],["safe_ess","skill"],["base_add","speed"]],"prepare":[["cap_up","item"]]},"gates":["mana_skill·skill은 Crafted, speed는 일반. 시작 한도 1. 두 Crafted 중 두 번째를 얻는 시점에는 cap_up이 이미 순서에 있어야 한다."]}}
```

## S1 · Rare 빈 목걸이 · 일반 생명력 1개

**전송 입력**

```json
{"base":"Solar Amulet","start":{"rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]}},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{}},"strategies":{"first_chaos":"첫 변경 가능 옵션 하나를 카오스로 반복하여 일반 목표 획득. 분열 보존. 필요한 등급·첫 옵션 준비 포함.","base_add":"Rare의 빈 자리에 일반 목표를 엑잘로 시도.","reveal":"대상의 훼손 재료로 슬롯을 만들고 공개 목록에서 목표를 선택."},"baseRules":[],"rules":["훼손 한 슬롯. 일반 옵션은 이번 수동 검증에서 엑잘로도 획득 가능."]}
```

**호출 전 기대 응답**

```text
first_chaos.life
base_add.life
reveal.life
```

**초안 실제 응답 · 기대 방향 0/3**

```json
[
  [["first_chaos", "life"], ["base_add", "life"]]
]
```

세 대안 중 첫 카오스와 엑잘을 한 줄로 결합해 생명력을 중복 획득하려 한다. 훼손도 누락했다.

**1차 수정 실제 응답 · 기대 방향 0/3**

```json
[[["first_chaos","life"],["base_add","item"]],[["base_add","life"],["first_chaos","item"]],[["reveal","life"],["base_add","item"]]]
```

세 출처를 분리한 뒤 각 경로에 옵션 획득 행동을 item 대상으로 덧붙였다. 생명력 하나가 붙으면 이미 완성이다.

## S2 · 분열 힘 · 생명력과 방어 확보, 엑잘 실패 복구

**전송 입력**

```json
{"base":"Solar Amulet","start":{"rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"strength","fractured":true}]}},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"strength":["base","Preserved Collarbone","Altered Collarbone"]}},"strategies":{"first_chaos":"첫 변경 가능 옵션 하나를 카오스로 반복하여 일반 목표 획득. 분열 보존. 필요한 등급·첫 옵션 준비 포함.","base_add":"Rare의 빈 자리에 일반 목표를 엑잘로 시도.","reveal":"대상의 훼손 재료로 슬롯을 만들고 공개 목록에서 목표를 선택.","safe_ess":"Rare에 대상 Perfect Essence 사용. 필요 시 제거용 더미와 방향 징조를 준비해 기존 목표 보존.","whittle_miss":"직전 base_add 실패 시에만 절사 카오스로 동일 대상을 복구. 적중하면 생략."},"baseRules":["Rare 3접두·3접미. Crafted 한도 1."],"rules":["분열은 첫 변경 가능 옵션 수에서 제외한다. def는 Crafted. 일반 추첨의 실제 실패 후에만 절사 자격을 검사한다."]}
```

**호출 전 기대 응답**

```text
first_chaos.life safe_ess.def
reveal.life safe_ess.def
safe_ess.def reveal.life
safe_ess.def base_add.life
safe_ess.def base_add.life whittle_miss.life
```

**초안 실제 응답 · 기대 방향 0/5**

```json
[
  ["first_chaos", "life"],
  ["first_chaos", "def"],
  ["first_chaos", "strength"],
  ["base_add", "life"],
  ["base_add", "strength"],
  ["safe_ess", "def"],
  ["whittle_miss", "life"],
  ["whittle_miss", "strength"]
]
```

후보와 단계 사이의 배열 한 겹이 없다. def를 카오스 대상으로, 분열로 이미 보유한 힘을 새 획득 대상으로 지정했다. 완성 순서로 소비할 수 없다.

**1차 수정 실제 응답 · 기대 방향 0/5**

```json
[[["first_chaos","life"],["safe_ess","def"],["base_add","strength"]],[["first_chaos","life"],["base_add","strength"],["safe_ess","def"]],[["first_chaos","life"],["reveal","def"],["base_add","strength"]],[["first_chaos","life"],["safe_ess","def"],["reveal","strength"]],[["first_chaos","life"],["reveal","def"],["reveal","strength"]]]
```

세 겹 배열이 됐으나 5개 후보 모두 이미 분열된 힘을 재획득하려 한다. 일부는 훼손 출처가 없는 def도 공개하려 한다.

**최종 실제 응답 · 기대 방향 1/5**

```json
[
  [["first_chaos","life"],["safe_ess","def"]]
]
```

첫 카오스 생명력 → 에센스 방어 1개는 입력상 적법하다. 훼손을 먼저 하는 경로, 방어 후 훼손·엑잘, 실패 복구 포함 경로까지 기대 4개가 빠졌다. 금지된 행동·대상 쌍은 사라졌지만 다양성은 부족하다.

## S3 · Magic 장갑 · 등급 상승 수단 두 종류

**전송 입력**

```json
{"base":"Stocky Mitts","start":{"rarity":"magic","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"filler"}]}},"targets":{"prefix":{"life":["base","Greater Essence of the Body"],"penetration":["Prismatic Alloy"]},"suffix":{}},"strategies":{"upgrade_ess":"Magic을 Rare로 올리며 대상 에센스 옵션 획득.","regal_base":"Magic을 Rare로 올리며 일반 목표가 붙은 결과 채택.","safe_alloy":"Rare에 대상 Alloy 사용. 필요 시 제거용 더미를 준비하고 기존 목표가 남는 결과를 채택."},"baseRules":[],"rules":["이 Magic에서 두 등급 상승 수단은 서로 대안이다. life는 일반 옵션, penetration은 Crafted. 목표 밖 접미가 남아도 된다."]}
```

**호출 전 기대 응답**

```text
upgrade_ess.life safe_alloy.penetration
regal_base.life safe_alloy.penetration
```

**초안 실제 응답 · 기대 방향 2/2**

```json
[
  [
    ["upgrade_ess", "life"],
    ["safe_alloy", "penetration"]
  ],
  [
    ["regal_base", "life"],
    ["safe_alloy", "penetration"]
  ]
]
```

두 Magic 획득 수단을 각각 먼저 사용하고 Rare에서 합금을 사용했다. 기대 두 방향을 모두 회수했다.

**1차 수정 실제 응답 · 기대 방향 1/2**

```json
[[["upgrade_ess","life"],["safe_alloy","penetration"]],[["safe_alloy","penetration"],["upgrade_ess","life"]]]
```

에센스 → 합금은 맞다. 합금 → 에센스는 Magic에서 합금을 먼저 쓰므로 막힌다. 초안이 회수했던 제왕 → 합금이 사라졌다.

## S4 · 마법봉 · 합금, 에센스, 일반 시전 속도

**전송 입력**

```json
{"base":"Attuned Wand","start":{"rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]}},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"strategies":{"safe_alloy":"Rare에 대상 Alloy 사용. 필요 시 제거용 더미를 준비하고 기존 목표가 남는 결과를 채택.","safe_ess":"Rare에 대상 Perfect Essence 사용. 필요 시 제거용 더미와 방향 징조를 준비해 기존 목표 보존.","cap_up":"사용 가능한 소켓에 Astrid's Creativity 설치, Crafted 한도 +1. 대상 item.","base_add":"Rare의 빈 자리에 일반 목표를 엑잘로 시도."},"baseRules":["시작 Crafted 한도 1. 빈 소켓 1개. Astrid's Creativity로 한도 2 가능."],"rules":["mana_skill과 skill은 각각 Crafted 1개를 쓴다. 서로 다른 계열이며 둘 다 보존한다. speed는 일반 접미."]}
```

**호출 전 기대 응답**

```text
safe_alloy.mana_skill safe_ess.skill base_add.speed
safe_ess.skill safe_alloy.mana_skill base_add.speed
base_add.speed safe_alloy.mana_skill safe_ess.skill
safe_alloy.mana_skill base_add.speed safe_ess.skill
```

**초안 실제 응답 · 기대 방향 0/4**

```json
[
  [["safe_alloy", "mana_skill"], ["safe_ess", "skill"], ["cap_up", "item"], ["base_add", "speed"]],
  [["safe_alloy", "mana_skill"], ["safe_ess", "skill"], ["base_add", "speed"]],
  [["safe_alloy", "mana_skill"], ["base_add", "speed"], ["safe_ess", "skill"]],
  [["safe_ess", "skill"], ["safe_alloy", "mana_skill"], ["cap_up", "item"], ["base_add", "speed"]],
  [["safe_ess", "skill"], ["safe_alloy", "mana_skill"], ["base_add", "speed"]],
  [["safe_ess", "skill"], ["base_add", "speed"], ["safe_alloy", "mana_skill"]]
]
```

6개 후보 모두 두 번째 Crafted 전에 한도를 확장하지 않았다. 확장이 있더라도 너무 늦다.

**1차 수정 실제 응답 · 기대 방향 0/4**

```json
[[["safe_alloy","mana_skill"],["safe_ess","skill"],["cap_up","item"],["base_add","speed"]],[["safe_ess","skill"],["safe_alloy","mana_skill"],["cap_up","item"],["base_add","speed"]]]
```

두 후보 모두 확장 전에 두 전용 옵션을 붙였다. 실행 전제 설명을 늘린 것만으로 순서 오류를 막지 못했다.

**최종 실제 응답 · 기대 방향 4/4**

```json
[
  [["cap_up","item"],["safe_alloy","mana_skill"],["safe_ess","skill"],["base_add","speed"]],
  [["cap_up","item"],["safe_alloy","mana_skill"],["base_add","speed"],["safe_ess","skill"]],
  [["cap_up","item"],["safe_ess","skill"],["safe_alloy","mana_skill"],["base_add","speed"]],
  [["cap_up","item"],["safe_ess","skill"],["base_add","speed"],["safe_alloy","mana_skill"]],
  [["cap_up","item"],["base_add","speed"],["safe_alloy","mana_skill"],["safe_ess","skill"]],
  [["cap_up","item"],["base_add","speed"],["safe_ess","skill"],["safe_alloy","mana_skill"]]
]
```

한도를 먼저 확장하고 세 옵션의 획득 순서를 6가지로 나눴다. 사전 기대 4개를 모두 회수했다. 나머지 2개도 시나리오의 추상 행동 전제에서 유효한 추가 후보다. 실제 게임 도달 확률은 별도 검증 대상이다.

## S5 · Weighted Sum · 티어는 이미 확보, 수치와 품질만 남음

**전송 입력**

```json
{"base":"Solar Amulet","start":{"rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","raw":99}],"suffix":[{"id":"strength","raw":7}]}},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"strength":["base","Preserved Collarbone","Altered Collarbone"]}},"strategies":{"flesh":"Flesh Catalyst로 품질 20 적용. 대상 item.","divine":"일반 Divine Orb로 변경 가능한 옵션의 원래 티어 범위 안에서 재굴림. 목표 수치가 맞는 결과 채택. 대상 item."},"baseRules":[],"rules":["완성은 Flesh 품질 20, 생명력 115~124, 힘 7~14, 합 127~129. 현재 품질 0. Flesh 적용 시 생명력만 반올림(원래 값×1.2). 현재는 Flesh만 쓰면 119+7=126. 같은 티어의 원래 값 99와 8은 합 127. Divine과 Flesh는 어느 순서로도 적용 가능."]}
```

**호출 전 기대 응답**

```text
flesh.item divine.item
divine.item flesh.item
```

**초안 실제 응답 · 기대 방향 0/2**

```json
[
  ["flesh", "item"],
  ["divine", "item"]
]
```

Flesh 후 Divine 순서 자체는 읽히지만 최상위 후보 배열이 빠졌다. 역순도 없다. 엄격한 소비 계약 기준으로 0건이다.

**1차 수정 실제 응답 · 기대 방향 0/2**

```json
[[["flesh","item"],["divine","life"],["divine","strength"]],[["divine","life"],["flesh","item"],["divine","strength"]],[["divine","strength"],["flesh","item"],["divine","life"]],[["flesh","item"],["divine","strength"],["divine","life"]],[["divine","life"],["divine","strength"],["flesh","item"]],[["divine","strength"],["divine","life"],["flesh","item"]]]
```

6개 후보 모두 Divine을 item 대신 life 또는 strength에 개별 적용했다. 실제 Divine은 변경 가능한 수치를 함께 재굴림한다.

## S6 · 이미 일치하는 축성 반지

**전송 입력**

```json
{"base":"Iron Ring","start":{"rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","value":90}],"suffix":[]}},"targets":{"prefix":{"life":["base"]},"suffix":{}},"strategies":{"base_add":"Rare의 빈 자리에 일반 목표를 엑잘로 시도.","divine":"일반 Divine Orb로 변경 가능한 옵션의 원래 티어 범위 안에서 재굴림. 목표 수치가 맞는 결과 채택. 대상 item."},"baseRules":[],"rules":["이미 축성. 현재 생명력 90이 유일한 목표(정확히 90)를 충족하며 다른 조건도 충족."]}
```

**호출 전 기대 응답**

```text
DONE
```

**초안 실제 응답 · 기대 방향 0/1**

```json
[[["divine","item"]]]
```

이미 완성된 축성 아이템에 Divine을 사용했다.

**1차 수정 실제 응답 · 기대 방향 0/1**

```json
[[["divine","item"]]]
```

완성 조건 우선 지시를 강화해도 Divine을 반환했다.

## S7 · 분열 마나가 합금 계열과 충돌

**전송 입력**

```json
{"base":"Attuned Wand","start":{"rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"beryl","fractured":true}],"suffix":[]}},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{}},"strategies":{"safe_alloy":"Rare에 대상 Alloy 사용. 필요 시 제거용 더미를 준비하고 기존 목표가 남는 결과를 채택.","cap_up":"사용 가능한 소켓에 Astrid's Creativity 설치, Crafted 한도 +1. 대상 item."},"baseRules":["빈 소켓 1. 한도 증가는 계열 충돌을 바꾸지 않는다."],"rules":["분열 beryl과 mana_skill은 같은 배타 계열. 분열은 제거·교체할 수 없다. 다른 시작 베이스는 허용하지 않는다."]}
```

**호출 전 기대 응답**

```text
NONE
```

**초안 실제 응답 · 기대 방향 0/1**

```json
[
  [
    ["safe_alloy", "mana_skill"],
    ["cap_up", "item"]
  ]
]
```

Crafted 한도 증가와 같은 계열 분열의 충돌 해제를 혼동했다.

**1차 수정 실제 응답 · 기대 방향 0/1**

```json
[[["safe_alloy","mana_skill"],["cap_up","item"]]]
```

같은 계열의 분열 충돌이 있는데도 합금과 한도 증가를 반환했다.

## S8 · 반지 · 두 일반 목표 중 어느 것을 먼저 채택할지

**전송 입력**

```json
{"base":"Iron Ring","start":{"rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]}},"targets":{"prefix":{"life":["base"]},"suffix":{"cold":["base"]}},"strategies":{"first_chaos":"첫 변경 가능 옵션 하나를 카오스로 반복하여 일반 목표 획득. 분열 보존. 필요한 등급·첫 옵션 준비 포함.","base_add":"Rare의 빈 자리에 일반 목표를 엑잘로 시도."},"baseRules":[],"rules":["이 실험의 일반 풀 경로만 비교한다. 첫 카오스는 시작 단계에서 한 번만 사용 가능."]}
```

**호출 전 기대 응답**

```text
first_chaos.life base_add.cold
first_chaos.cold base_add.life
base_add.life base_add.cold
base_add.cold base_add.life
```

**초안 실제 응답 · 기대 방향 1/4**

```json
[
  [["first_chaos", "life"], ["base_add", "cold"]]
]
```

생명력 첫 카오스 → 냉기 엑잘 1개만 반환했다. 냉기 선취득과 엑잘 두 번 경로를 놓쳤다.

**1차 수정 실제 응답 · 기대 방향 0/4**

```json
[[["first_chaos","life"],["first_chaos","cold"]],[["first_chaos","cold"],["first_chaos","life"]]]
```

먼저 노리는 옵션을 바꾼 두 줄은 생겼지만 두 옵션 모두 첫 카오스로 얻으려 한다. 첫 적중 후 두 번째 첫 카오스를 사용할 수 없다.

## S9 · 미사용 평가 · 마법봉에 합금 접두가 이미 있음

**전송 입력**

```json
{"base":"Attuned Wand","start":{"rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]}},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"]}},"strategies":{"safe_alloy":"Rare에 대상 Alloy 사용. 필요 시 제거용 더미를 준비하고 기존 목표가 남는 결과를 채택.","safe_ess":"Rare에 대상 Perfect Essence 사용. 필요 시 제거용 더미와 방향 징조를 준비해 기존 목표 보존.","cap_up":"사용 가능한 소켓에 Astrid's Creativity 설치, Crafted 한도 +1. 대상 item."},"baseRules":["현재 1/1 Crafted. 빈 소켓에 Astrid's Creativity 설치 시 2개 가능."],"rules":["두 목표 모두 Crafted. 기존 mana_skill을 보존한다."]}
```

**호출 전 기대 응답**

```text
cap_up.item safe_ess.skill
```

**1차 수정 실제 응답 · 기대 방향 0/1**

```json
[[["safe_alloy","mana_skill"],["safe_ess","skill"]],[["safe_ess","skill"],["safe_alloy","mana_skill"]]]
```

이미 있는 합금까지 다시 적용하고 필요한 cap_up을 두 후보 모두 누락했다.

## S10 · 미사용 평가 · 축성 반지의 수치 미달

**전송 입력**

```json
{"base":"Iron Ring","start":{"rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","value":89}],"suffix":[]}},"targets":{"prefix":{"life":["base"]},"suffix":{}},"strategies":{"base_add":"Rare의 빈 자리에 일반 목표를 엑잘로 시도.","divine":"일반 Divine Orb로 변경 가능한 옵션의 원래 티어 범위 안에서 재굴림. 목표 수치가 맞는 결과 채택. 대상 item."},"baseRules":[],"rules":["이미 축성. 현재 상태만 허용되며 생명력 목표는 정확히 90."]}
```

**호출 전 기대 응답**

```text
NONE
```

**1차 수정 실제 응답 · 기대 방향 1/1**

```json
[]
```

축성된 미달 상태에 []를 반환했다. 사전 기대와 일치한다.
