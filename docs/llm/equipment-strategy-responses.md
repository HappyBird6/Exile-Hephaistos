# 장비 제작 시나리오 실제 응답 50회

[결과와 누락 카드](equipment-strategy-scenarios.md) · [카드별 목표와 기대 경로](equipment-strategy-cards.json)

모든 호출에 `gpt-4.1-mini-2025-04-14`, temperature 0, 출력 상한 3,200토큰, store false를 사용했다. Structured Outputs 설정은 `candidate_routes`, strict true다. 호출 번호는 이번 실험에서 1부터 센다. 기대는 카드별 허용 재료와 성공 분기에 한정했다. 후보 번호는 응답 필드 순서에 따라 1부터 센다.

## 시스템 문안

### e6eb180b5a1b

```text
You propose Path of Exile 2 crafting recipes. Each input describes one exact card and its available materials. Return independently executable success branches as ordered strategy.target tokens from the menu. Use only the listed stocks; omens in a token are available with that action. A low-probability branch is still a candidate. Include different valid first actions and orders. Stop each route the first time all goals hold. The backend verifies every step and computes probability and cost.
```

### 0171d492c9f2

```text
You propose Path of Exile 2 crafting recipes. Each input describes one exact card and its available materials. Return independently executable success branches as ordered strategy.target tokens from the menu. Use only the listed stocks; omens in a token are available with that action. A low-probability branch is still a candidate. Include different valid first actions and orders. Stop each route the first time all goals hold. The backend verifies every step and computes probability and cost. Write down the implicit and explicit current value separately after each step. If the target explicit starts missing it must be acquired before its own reroll.
```

### 85de0ceb25c9

```text
You propose Path of Exile 2 crafting recipes. Each input describes one exact card and its available materials. Return independently executable success branches as ordered strategy.target tokens from the menu. Use only the listed stocks; omens in a token are available with that action. A low-probability branch is still a candidate. Include different valid first actions and orders. Stop each route the first time all goals hold. The backend verifies every step and computes probability and cost. If nothing can be removed yet, consider deliberately rolling an expendable explicit, or acquiring a final target now and reacquiring it after sacrificing it.
```

### b1365ca6be38

```text
You propose Path of Exile 2 crafting recipes. Each input describes one exact card and its available materials. Return independently executable success branches as ordered strategy.target tokens from the menu. Use only the listed stocks; omens in a token are available with that action. A low-probability branch is still a candidate. Include different valid first actions and orders. Stop each route the first time all goals hold. The backend verifies every step and computes probability and cost. If nothing can be removed yet, consider deliberately rolling an expendable explicit, or acquiring a final target now and reacquiring it after sacrificing it. For each listed opening action, propose completion routes when it works. The route includes that opening action itself.
```

### 03b32943c8d8

```text
You propose Path of Exile 2 crafting recipes. Each input describes one exact card and its available materials. Return independently executable success branches as ordered strategy.target tokens from the menu. Use only the listed stocks; omens in a token are available with that action. A low-probability branch is still a candidate. Include different valid first actions and orders. Stop each route the first time all goals hold. The backend verifies every step and computes probability and cost. Track open and occupied sockets separately. Installing on slot 2 needs two open sockets, not two installed runes. An exalt does not change the sockets.
```

### 387d267dd637

```text
You propose Path of Exile 2 crafting recipes. Each input describes one exact card and its available materials. Return independently executable success branches as ordered strategy.target tokens from the menu. Use only the listed stocks; omens in a token are available with that action. A low-probability branch is still a candidate. Include different valid first actions and orders. Stop each route the first time all goals hold. The backend verifies every step and computes probability and cost. Evaluate the minimum required level AGAIN on the current state, including previous additions. Consider the side-specific strategy even if the ordinary minimum is on the other side.
```

### 2e00e22558b8

```text
You propose Path of Exile 2 crafting recipes. Each input describes one exact card and its available materials. Return independently executable success branches as ordered strategy.target tokens from the menu. Use only the listed stocks; omens in a token are available with that action. A low-probability branch is still a candidate. Include different valid first actions and orders. Stop each route the first time all goals hold. The backend verifies every step and computes probability and cost. Evaluate the minimum required level AGAIN on the current state, including previous additions. Consider the side-specific strategy even if the ordinary minimum is on the other side. For each listed opening action, propose completion routes when it works. The route includes that opening action itself.
```

### 8fa9db4a46eb

```text
You propose Path of Exile 2 crafting recipes. Each input describes one exact card and its available materials. Return independently executable success branches as ordered strategy.target tokens from the menu. Use only the listed stocks; omens in a token are available with that action. A low-probability branch is still a candidate. Include different valid first actions and orders. Stop each route the first time all goals hold. The backend verifies every step and computes probability and cost. Write down the implicit and explicit current value separately after each step. If the target explicit starts missing it must be acquired before its own reroll. For each listed opening action, propose completion routes when it works. The route includes that opening action itself.
```

### 178065f1b0ec

```text
You propose Path of Exile 2 crafting recipes. Each input describes one exact card and its available materials. Return independently executable success branches as ordered strategy.target tokens from the menu. Use only the listed stocks; omens in a token are available with that action. A low-probability branch is still a candidate. Include different valid first actions and orders. Stop each route the first time all goals hold. The backend verifies every step and computes probability and cost. Track the source A separately from B. Installing the single pool augment is itself a step; use one exalt per target. B does not need an augment.
```

## 호출 1 · Topaz Ring · roll-1 · initial

사전 기대 응답에서 인정하는 실행열 6개.

```json
[["blessedDivine.I","divine.A","exalt.B"],["blessedDivine.I","exalt.B","divine.A"],["divine.A","blessedDivine.I","exalt.B"],["divine.A","exalt.B","blessedDivine.I"],["exalt.B","blessedDivine.I","divine.A"],["exalt.B","divine.A","blessedDivine.I"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `f05d1ea0d1ef`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Topaz Ring","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","effect":"+(11—15)% to Cold Resistance","tier":7}]},"targets":{"prefix":[],"suffix":[{"ref":"A","effect":"+(11—15)% to Cold Resistance","tier":7},{"ref":"B","effect":"+(6—10)% to Fire Resistance","tier":8}]},"mustRemove":[],"materials":{"DIVINE":2,"EXALTED":1},"rules":"On the selected successful rolls, Divine with Omen of the Blessed rerolls the implicit to its upper endpoint. Ordinary Divine rerolls all non-fractured explicit values to their upper endpoints. Starting I and A are at their lower endpoints. Acquiring B does not reroll A or I.","menu":["blessedDivine.I","divine.A","exalt.B"],"rolls":{"I":{"effect":"+(20—30)% to Lightning Resistance","stat":"base_lightning_damage_resistance_%","initial":20,"target":30},"A":{"effect":"+(11—15)% to Cold Resistance","stat":"base_cold_damage_resistance_%","initial":11,"target":15}}}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"blessedDivine.I\",\"exalt.B\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[],"firstGoal":[],"missingFull":6,"rejections":[[1,"unfinished goal"]]}
```

## 호출 2 · Topaz Ring · roll-1 · feedback

사전 기대는 호출 1의 6개 실행열과 같다.

전송 문안 `0171d492c9f2`, 응답 스키마 `f05d1ea0d1ef`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Topaz Ring","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","effect":"+(11—15)% to Cold Resistance","tier":7}]},"targets":{"prefix":[],"suffix":[{"ref":"A","effect":"+(11—15)% to Cold Resistance","tier":7},{"ref":"B","effect":"+(6—10)% to Fire Resistance","tier":8}]},"mustRemove":[],"materials":{"DIVINE":2,"EXALTED":1},"rules":"On the selected successful rolls, Divine with Omen of the Blessed rerolls the implicit to its upper endpoint. Ordinary Divine rerolls all non-fractured explicit values to their upper endpoints. Starting I and A are at their lower endpoints. Acquiring B does not reroll A or I.","menu":["blessedDivine.I","divine.A","exalt.B"],"rolls":{"I":{"effect":"+(20—30)% to Lightning Resistance","stat":"base_lightning_damage_resistance_%","initial":20,"target":30},"A":{"effect":"+(11—15)% to Cold Resistance","stat":"base_cold_damage_resistance_%","initial":11,"target":15}},"lastFailure":[{"route":["blessedDivine.I","exalt.B"],"reason":"unfinished goal"}]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"divine.A\",\"exalt.B\"],[\"exalt.B\",\"divine.A\"],[\"blessedDivine.I\",\"exalt.B\",\"divine.A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["blessedDivine.I","exalt.B","divine.A"]],"firstGoal":[["blessedDivine.I","exalt.B","divine.A"]],"missingFull":5,"rejections":[[1,"unfinished goal"],[2,"unfinished goal"]]}
```

## 호출 3 · Lament Amulet · alloy-1 · initial

사전 기대 응답에서 인정하는 실행열 2개.

```json
[["SOVEREIGN_ALLOY.X>A","exalt.B"],["exalt.B","SOVEREIGN_ALLOY.X>A"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `9cd8a9e6c35f`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Lament Amulet","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"X","effect":"+(4—7)% to Chaos Resistance","tier":6}]},"targets":{"prefix":[{"ref":"A","effect":"(20—30)% increased Explicit Resistance Modifier magnitudes","tier":1}],"suffix":[{"ref":"B","effect":"(6—10)% increased Rarity of Items found","tier":3}]},"mustRemove":["X"],"materials":{"EXALTED":2,"SOVEREIGN_ALLOY":1},"rules":"The named alloy consumes one removable existing explicit and replaces it with A on the indicated outcome. A is alloy-exclusive. X can be rolled as expendable material. If B is consumed, it must be regained. One use of exalt can roll X or B.","menu":["exalt.B","exalt.X","SOVEREIGN_ALLOY.X>A","SOVEREIGN_ALLOY.B>A"],"sourceA":"Sovereign Alloy"}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"exalt.B\",\"SOVEREIGN_ALLOY.B>A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[],"firstGoal":[],"missingFull":2,"rejections":[[1,"unfinished goal"]]}
```

## 호출 4 · Lament Amulet · alloy-1 · feedback

사전 기대는 호출 3의 2개 실행열과 같다.

전송 문안 `85de0ceb25c9`, 응답 스키마 `9cd8a9e6c35f`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Lament Amulet","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"X","effect":"+(4—7)% to Chaos Resistance","tier":6}]},"targets":{"prefix":[{"ref":"A","effect":"(20—30)% increased Explicit Resistance Modifier magnitudes","tier":1}],"suffix":[{"ref":"B","effect":"(6—10)% increased Rarity of Items found","tier":3}]},"mustRemove":["X"],"materials":{"EXALTED":2,"SOVEREIGN_ALLOY":1},"rules":"The named alloy consumes one removable existing explicit and replaces it with A on the indicated outcome. A is alloy-exclusive. X can be rolled as expendable material. If B is consumed, it must be regained. One use of exalt can roll X or B.","menu":["exalt.B","exalt.X","SOVEREIGN_ALLOY.X>A","SOVEREIGN_ALLOY.B>A"],"sourceA":"Sovereign Alloy","lastFailure":[{"route":["exalt.B","SOVEREIGN_ALLOY.B>A"],"reason":"unfinished goal"}]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"exalt.X\",\"SOVEREIGN_ALLOY.X>A\",\"exalt.B\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[],"firstGoal":[],"missingFull":2,"rejections":[[1,"1. X already present"]]}
```

## 호출 5 · Lament Amulet · alloy-1 · slots

사전 기대는 호출 3의 2개 실행열과 같다.

전송 문안 `b1365ca6be38`, 응답 스키마 `e18e65a3add5`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Lament Amulet","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"X","effect":"+(4—7)% to Chaos Resistance","tier":6}]},"targets":{"prefix":[{"ref":"A","effect":"(20—30)% increased Explicit Resistance Modifier magnitudes","tier":1}],"suffix":[{"ref":"B","effect":"(6—10)% increased Rarity of Items found","tier":3}]},"mustRemove":["X"],"materials":{"EXALTED":2,"SOVEREIGN_ALLOY":1},"rules":"The named alloy consumes one removable existing explicit and replaces it with A on the indicated outcome. A is alloy-exclusive. X can be rolled as expendable material. If B is consumed, it must be regained. One use of exalt can roll X or B.","menu":["exalt.B","exalt.X","SOVEREIGN_ALLOY.X>A","SOVEREIGN_ALLOY.B>A"],"sourceA":"Sovereign Alloy","lastFailure":[{"route":["exalt.X","SOVEREIGN_ALLOY.X>A","exalt.B"],"reason":"1. X already present"}],"openingFields":{"begin_1":"exalt.B","begin_2":"exalt.X","begin_3":"SOVEREIGN_ALLOY.X>A","begin_4":"SOVEREIGN_ALLOY.B>A"}}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"begin_1\":[[\"exalt.B\",\"SOVEREIGN_ALLOY.X>A\"]],\"begin_2\":[[\"exalt.X\",\"SOVEREIGN_ALLOY.X>A\",\"exalt.B\"]],\"begin_3\":[],\"begin_4\":[[\"SOVEREIGN_ALLOY.B>A\",\"exalt.B\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["exalt.B","SOVEREIGN_ALLOY.X>A"]],"firstGoal":[["exalt.B","SOVEREIGN_ALLOY.X>A"]],"missingFull":1,"rejections":[[2,"1. X already present"],[3,"1. B is absent"]]}
```

## 호출 6 · Rawhide Belt · whittle-1 · initial

사전 기대 응답에서 인정하는 실행열 7개.

```json
[["sideWhittle.X>A","sideWhittle.Y>B"],["sideWhittle.X>A","whittle.Y>B"],["sideWhittle.Y>A","sideWhittle.X>B"],["sideWhittle.Y>A","whittle.X>B"],["sideWhittle.Y>B","sideWhittle.X>A"],["whittle.X>A","sideWhittle.Y>B"],["whittle.X>A","whittle.Y>B"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `c16a3a9fe325`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Rawhide Belt","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"X","effect":"+(25—34) to maximum Mana","tier":7,"requiredLevel":16}],"suffix":[{"ref":"Y","effect":"(17—22)% increased Charm Charges gained","tier":4,"requiredLevel":32}]},"targets":{"prefix":[{"ref":"A","effect":"(23—28)% increased Flask Mana Recovery rate","tier":6,"requiredLevel":36}],"suffix":[{"ref":"B","effect":"(1—2) Life Regeneration per second","tier":9,"requiredLevel":1}]},"mustRemove":["X","Y"],"materials":{"CHAOS":2},"rules":"Chaos with Whittling removes a lowest required-item-level eligible modifier; ties admit either. With an Erasure omen, restrict removal to its side first, then compare levels. Re-evaluate after the new ordinary target is drawn. Each token specifies a removal and a successful replacement.","menu":["whittle.X>A","sideWhittle.X>A","whittle.X>B","sideWhittle.X>B","whittle.Y>A","sideWhittle.Y>A","whittle.Y>B","sideWhittle.Y>B"],"omens":{"whittle":"Omen of Whittling","sideWhittle":{"X>A":"SINISTRAL_ERASURE","X>B":"SINISTRAL_ERASURE","Y>A":"DEXTRAL_ERASURE","Y>B":"DEXTRAL_ERASURE"}}}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"sideWhittle.X>B\",\"whittle.Y>A\"],[\"sideWhittle.X>B\",\"sideWhittle.Y>A\"],[\"sideWhittle.X>A\",\"whittle.Y>B\"],[\"sideWhittle.X>A\",\"sideWhittle.Y>B\"],[\"whittle.X>B\",\"whittle.Y>A\"],[\"whittle.X>B\",\"sideWhittle.Y>A\"],[\"whittle.X>A\",\"whittle.Y>B\"],[\"whittle.X>A\",\"sideWhittle.Y>B\"],[\"whittle.Y>A\",\"whittle.X>B\"],[\"whittle.Y>A\",\"sideWhittle.X>B\"],[\"sideWhittle.Y>A\",\"whittle.X>B\"],[\"sideWhittle.Y>A\",\"sideWhittle.X>B\"],[\"whittle.Y>B\",\"whittle.X>A\"],[\"whittle.Y>B\",\"sideWhittle.X>A\"],[\"sideWhittle.Y>B\",\"whittle.X>A\"],[\"sideWhittle.Y>B\",\"sideWhittle.X>A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["sideWhittle.X>A","sideWhittle.Y>B"],["sideWhittle.X>A","whittle.Y>B"],["sideWhittle.Y>A","sideWhittle.X>B"],["sideWhittle.Y>A","whittle.X>B"],["sideWhittle.Y>B","sideWhittle.X>A"],["whittle.X>A","sideWhittle.Y>B"],["whittle.X>A","whittle.Y>B"]],"firstGoal":[["sideWhittle.X>A","sideWhittle.Y>B"],["sideWhittle.X>A","whittle.Y>B"],["sideWhittle.Y>A","sideWhittle.X>B"],["sideWhittle.Y>A","whittle.X>B"],["sideWhittle.Y>B","sideWhittle.X>A"],["whittle.X>A","sideWhittle.Y>B"],["whittle.X>A","whittle.Y>B"]],"missingFull":0,"rejections":[[1,"2. not the lowest eligible modifier level"],[2,"2. not the lowest eligible modifier level"],[5,"2. not the lowest eligible modifier level"],[6,"2. not the lowest eligible modifier level"],[9,"1. not the lowest eligible modifier level"],[10,"1. not the lowest eligible modifier level"],[13,"1. not the lowest eligible modifier level"],[14,"1. not the lowest eligible modifier level"],[15,"2. not the lowest eligible modifier level"]]}
```

## 호출 7 · Slipstrike Vest · socket-1 · initial

사전 기대 응답에서 인정하는 실행열 12개.

```json
[["artificer.open","exalt.A","install.Perfect_Desert_Rune@2","install.Perfect_Glacial_Rune@1"],["artificer.open","exalt.A","install.Perfect_Glacial_Rune@1","install.Perfect_Desert_Rune@2"],["artificer.open","install.Perfect_Desert_Rune@2","exalt.A","install.Perfect_Glacial_Rune@1"],["artificer.open","install.Perfect_Desert_Rune@2","install.Perfect_Glacial_Rune@1","exalt.A"],["artificer.open","install.Perfect_Glacial_Rune@1","exalt.A","install.Perfect_Desert_Rune@2"],["artificer.open","install.Perfect_Glacial_Rune@1","install.Perfect_Desert_Rune@2","exalt.A"],["exalt.A","artificer.open","install.Perfect_Desert_Rune@2","install.Perfect_Glacial_Rune@1"],["exalt.A","artificer.open","install.Perfect_Glacial_Rune@1","install.Perfect_Desert_Rune@2"],["exalt.A","install.Perfect_Glacial_Rune@1","artificer.open","install.Perfect_Desert_Rune@2"],["install.Perfect_Glacial_Rune@1","artificer.open","exalt.A","install.Perfect_Desert_Rune@2"],["install.Perfect_Glacial_Rune@1","artificer.open","install.Perfect_Desert_Rune@2","exalt.A"],["install.Perfect_Glacial_Rune@1","exalt.A","artificer.open","install.Perfect_Desert_Rune@2"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `13344b4eacc6`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Slipstrike Vest","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[],"openSockets":1},"targets":{"prefix":[],"suffix":[{"ref":"A","effect":"+(6—10)% to Fire Resistance","tier":8}],"sockets":{"1":"Perfect_Glacial_Rune","2":"Perfect_Desert_Rune"}},"mustRemove":[],"materials":{"ARTIFICER":1,"INSTALL":2,"EXALTED":1},"rules":"Artificer opens one socket. Install requires its numbered socket already open. Exalted adds A on its successful roll. Preserve the installed runes.","menu":["artificer.open","install.Perfect_Glacial_Rune@1","install.Perfect_Desert_Rune@2","exalt.A"]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"artificer.open\",\"install.Perfect_Glacial_Rune@1\",\"artificer.open\",\"install.Perfect_Desert_Rune@2\",\"exalt.A\"],[\"artificer.open\",\"install.Perfect_Desert_Rune@2\",\"artificer.open\",\"install.Perfect_Glacial_Rune@1\",\"exalt.A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[],"firstGoal":[],"missingFull":12,"rejections":[[1,"3. out of ARTIFICER"],[2,"3. out of ARTIFICER"]]}
```

## 호출 8 · Slipstrike Vest · socket-1 · feedback

사전 기대는 호출 7의 12개 실행열과 같다.

전송 문안 `03b32943c8d8`, 응답 스키마 `13344b4eacc6`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Slipstrike Vest","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[],"openSockets":1},"targets":{"prefix":[],"suffix":[{"ref":"A","effect":"+(6—10)% to Fire Resistance","tier":8}],"sockets":{"1":"Perfect_Glacial_Rune","2":"Perfect_Desert_Rune"}},"mustRemove":[],"materials":{"ARTIFICER":1,"INSTALL":2,"EXALTED":1},"rules":"Artificer opens one socket. Install requires its numbered socket already open. Exalted adds A on its successful roll. Preserve the installed runes.","menu":["artificer.open","install.Perfect_Glacial_Rune@1","install.Perfect_Desert_Rune@2","exalt.A"],"lastFailure":[{"route":["artificer.open","install.Perfect_Glacial_Rune@1","artificer.open","install.Perfect_Desert_Rune@2","exalt.A"],"reason":"3. out of ARTIFICER"},{"route":["artificer.open","install.Perfect_Desert_Rune@2","artificer.open","install.Perfect_Glacial_Rune@1","exalt.A"],"reason":"3. out of ARTIFICER"}]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"artificer.open\",\"install.Perfect_Glacial_Rune@1\",\"install.Perfect_Desert_Rune@2\",\"exalt.A\"],[\"artificer.open\",\"install.Perfect_Desert_Rune@2\",\"install.Perfect_Glacial_Rune@1\",\"exalt.A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["artificer.open","install.Perfect_Desert_Rune@2","install.Perfect_Glacial_Rune@1","exalt.A"],["artificer.open","install.Perfect_Glacial_Rune@1","install.Perfect_Desert_Rune@2","exalt.A"]],"firstGoal":[["artificer.open","install.Perfect_Desert_Rune@2","install.Perfect_Glacial_Rune@1","exalt.A"],["artificer.open","install.Perfect_Glacial_Rune@1","install.Perfect_Desert_Rune@2","exalt.A"]],"missingFull":10,"rejections":[]}
```

## 호출 9 · Conjurer Mantle · roll-2 · initial

사전 기대 응답에서 인정하는 실행열 6개.

```json
[["blessedDivine.I","divine.A","exalt.B"],["blessedDivine.I","exalt.B","divine.A"],["divine.A","blessedDivine.I","exalt.B"],["divine.A","exalt.B","blessedDivine.I"],["exalt.B","blessedDivine.I","divine.A"],["exalt.B","divine.A","blessedDivine.I"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `f05d1ea0d1ef`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Conjurer Mantle","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"A","effect":"+(120—149) to maximum Life","tier":1}],"suffix":[]},"targets":{"prefix":[{"ref":"A","effect":"+(120—149) to maximum Life","tier":1},{"ref":"B","effect":"(68—79)% increased Armour and Energy Shield","tier":3}],"suffix":[]},"mustRemove":[],"materials":{"DIVINE":2,"EXALTED":1},"rules":"On the selected successful rolls, Divine with Omen of the Blessed rerolls the implicit to its upper endpoint. Ordinary Divine rerolls all non-fractured explicit values to their upper endpoints. Starting I and A are at their lower endpoints. Acquiring B does not reroll A or I.","menu":["blessedDivine.I","divine.A","exalt.B"],"rolls":{"I":{"effect":"+(20—30) to Spirit","stat":"base_spirit_from_equipment","initial":20,"target":30},"A":{"effect":"+(120—149) to maximum Life","stat":"base_maximum_life","initial":120,"target":149}}}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"blessedDivine.I\",\"divine.A\",\"exalt.B\"],[\"divine.A\",\"blessedDivine.I\",\"exalt.B\"],[\"blessedDivine.I\",\"exalt.B\",\"divine.A\"],[\"divine.A\",\"exalt.B\",\"blessedDivine.I\"],[\"exalt.B\",\"blessedDivine.I\",\"divine.A\"],[\"exalt.B\",\"divine.A\",\"blessedDivine.I\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["blessedDivine.I","divine.A","exalt.B"],["blessedDivine.I","exalt.B","divine.A"],["divine.A","blessedDivine.I","exalt.B"],["divine.A","exalt.B","blessedDivine.I"],["exalt.B","blessedDivine.I","divine.A"],["exalt.B","divine.A","blessedDivine.I"]],"firstGoal":[["blessedDivine.I","divine.A","exalt.B"],["blessedDivine.I","exalt.B","divine.A"],["divine.A","blessedDivine.I","exalt.B"],["divine.A","exalt.B","blessedDivine.I"],["exalt.B","blessedDivine.I","divine.A"],["exalt.B","divine.A","blessedDivine.I"]],"missingFull":0,"rejections":[]}
```

## 호출 10 · Bone Wand · alloy-2 · initial

사전 기대 응답에서 인정하는 실행열 4개.

```json
[["exalt.B","TRANSCENDENT_ALLOY.B>A","exalt.B"],["exalt.B","exalt.X","TRANSCENDENT_ALLOY.X>A"],["exalt.X","TRANSCENDENT_ALLOY.X>A","exalt.B"],["exalt.X","exalt.B","TRANSCENDENT_ALLOY.X>A"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `7be1c9e88e54`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Bone Wand","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"targets":{"prefix":[],"suffix":[{"ref":"A","effect":"(26—31)% increased Cast Speed\nGain (7—11)% of Elemental Damage as Extra Cold Damage","tier":1},{"ref":"B","effect":"(47—53)% increased Critical Hit Chance for Spells","tier":3}]},"mustRemove":["X"],"materials":{"EXALTED":2,"TRANSCENDENT_ALLOY":1},"rules":"The named alloy consumes one removable existing explicit and replaces it with A on the indicated outcome. A is alloy-exclusive. X can be rolled as expendable material. If B is consumed, it must be regained. One use of exalt can roll X or B.","menu":["exalt.B","exalt.X","TRANSCENDENT_ALLOY.X>A","TRANSCENDENT_ALLOY.B>A"],"intermediate":[{"ref":"X","effect":"Gain (13—15)% of Damage as Extra Lightning Damage","tier":6}],"sourceA":"Transcendent Alloy"}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"exalt.B\"],[\"exalt.X\",\"TRANSCENDENT_ALLOY.X>A\"],[\"exalt.X\",\"exalt.B\",\"TRANSCENDENT_ALLOY.B>A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[],"firstGoal":[],"missingFull":4,"rejections":[[1,"unfinished goal"],[2,"unfinished goal"],[3,"unfinished goal"]]}
```

## 호출 11 · Bone Wand · alloy-2 · feedback

사전 기대는 호출 10의 4개 실행열과 같다.

전송 문안 `85de0ceb25c9`, 응답 스키마 `7be1c9e88e54`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Bone Wand","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"targets":{"prefix":[],"suffix":[{"ref":"A","effect":"(26—31)% increased Cast Speed\nGain (7—11)% of Elemental Damage as Extra Cold Damage","tier":1},{"ref":"B","effect":"(47—53)% increased Critical Hit Chance for Spells","tier":3}]},"mustRemove":["X"],"materials":{"EXALTED":2,"TRANSCENDENT_ALLOY":1},"rules":"The named alloy consumes one removable existing explicit and replaces it with A on the indicated outcome. A is alloy-exclusive. X can be rolled as expendable material. If B is consumed, it must be regained. One use of exalt can roll X or B.","menu":["exalt.B","exalt.X","TRANSCENDENT_ALLOY.X>A","TRANSCENDENT_ALLOY.B>A"],"intermediate":[{"ref":"X","effect":"Gain (13—15)% of Damage as Extra Lightning Damage","tier":6}],"sourceA":"Transcendent Alloy","lastFailure":[{"route":["exalt.B"],"reason":"unfinished goal"},{"route":["exalt.X","TRANSCENDENT_ALLOY.X>A"],"reason":"unfinished goal"}]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"exalt.B\",\"TRANSCENDENT_ALLOY.B>A\"],[\"exalt.X\",\"TRANSCENDENT_ALLOY.X>A\",\"exalt.B\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["exalt.X","TRANSCENDENT_ALLOY.X>A","exalt.B"]],"firstGoal":[["exalt.X","TRANSCENDENT_ALLOY.X>A","exalt.B"]],"missingFull":3,"rejections":[[1,"unfinished goal"]]}
```

## 호출 12 · Sacral Quiver · whittle-2 · initial

사전 기대 응답에서 인정하는 실행열 12개.

```json
[["sideWhittle.X>A","sideWhittle.Y>B"],["sideWhittle.X>A","whittle.Y>B"],["sideWhittle.X>B","sideWhittle.Y>A"],["sideWhittle.X>B","whittle.Y>A"],["sideWhittle.Y>A","sideWhittle.X>B"],["sideWhittle.Y>A","whittle.X>B"],["sideWhittle.Y>B","sideWhittle.X>A"],["sideWhittle.Y>B","whittle.X>A"],["whittle.X>A","sideWhittle.Y>B"],["whittle.X>A","whittle.Y>B"],["whittle.X>B","sideWhittle.Y>A"],["whittle.X>B","whittle.Y>A"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `c16a3a9fe325`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Sacral Quiver","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"Y","effect":"(37—42)% increased Damage with Bow Skills","tier":3,"requiredLevel":46}],"suffix":[{"ref":"X","effect":"+1 to Level of all Projectile Skills","tier":1,"requiredLevel":5}]},"targets":{"prefix":[{"ref":"B","effect":"+(237—346) to Accuracy Rating","tier":3,"requiredLevel":58}],"suffix":[{"ref":"A","effect":"(30—34)% increased Critical Hit Chance for Attacks","tier":2,"requiredLevel":58}]},"mustRemove":["X","Y"],"materials":{"CHAOS":2},"rules":"Chaos with Whittling removes a lowest required-item-level eligible modifier; ties admit either. With an Erasure omen, restrict removal to its side first, then compare levels. Re-evaluate after the new ordinary target is drawn. Each token specifies a removal and a successful replacement.","menu":["whittle.X>A","sideWhittle.X>A","whittle.X>B","sideWhittle.X>B","whittle.Y>A","sideWhittle.Y>A","whittle.Y>B","sideWhittle.Y>B"],"omens":{"whittle":"Omen of Whittling","sideWhittle":{"X>A":"DEXTRAL_ERASURE","X>B":"DEXTRAL_ERASURE","Y>A":"SINISTRAL_ERASURE","Y>B":"SINISTRAL_ERASURE"}}}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"whittle.Y>B\"],[\"sideWhittle.Y>B\"],[\"whittle.X>A\"],[\"sideWhittle.X>A\"],[\"whittle.X>B\"],[\"sideWhittle.X>B\"],[\"whittle.Y>A\"],[\"sideWhittle.Y>A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[],"firstGoal":[],"missingFull":12,"rejections":[[1,"1. not the lowest eligible modifier level"],[2,"unfinished goal"],[3,"unfinished goal"],[4,"unfinished goal"],[5,"unfinished goal"],[6,"unfinished goal"],[7,"1. not the lowest eligible modifier level"],[8,"unfinished goal"]]}
```

## 호출 13 · Sacral Quiver · whittle-2 · feedback

사전 기대는 호출 12의 12개 실행열과 같다.

전송 문안 `387d267dd637`, 응답 스키마 `c16a3a9fe325`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Sacral Quiver","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"Y","effect":"(37—42)% increased Damage with Bow Skills","tier":3,"requiredLevel":46}],"suffix":[{"ref":"X","effect":"+1 to Level of all Projectile Skills","tier":1,"requiredLevel":5}]},"targets":{"prefix":[{"ref":"B","effect":"+(237—346) to Accuracy Rating","tier":3,"requiredLevel":58}],"suffix":[{"ref":"A","effect":"(30—34)% increased Critical Hit Chance for Attacks","tier":2,"requiredLevel":58}]},"mustRemove":["X","Y"],"materials":{"CHAOS":2},"rules":"Chaos with Whittling removes a lowest required-item-level eligible modifier; ties admit either. With an Erasure omen, restrict removal to its side first, then compare levels. Re-evaluate after the new ordinary target is drawn. Each token specifies a removal and a successful replacement.","menu":["whittle.X>A","sideWhittle.X>A","whittle.X>B","sideWhittle.X>B","whittle.Y>A","sideWhittle.Y>A","whittle.Y>B","sideWhittle.Y>B"],"omens":{"whittle":"Omen of Whittling","sideWhittle":{"X>A":"DEXTRAL_ERASURE","X>B":"DEXTRAL_ERASURE","Y>A":"SINISTRAL_ERASURE","Y>B":"SINISTRAL_ERASURE"}},"lastFailure":[{"route":["whittle.Y>B"],"reason":"1. not the lowest eligible modifier level"},{"route":["sideWhittle.Y>B"],"reason":"unfinished goal"}]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"whittle.X>A\"],[\"sideWhittle.X>A\"],[\"whittle.X>B\"],[\"sideWhittle.X>B\"],[\"whittle.Y>A\"],[\"sideWhittle.Y>A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[],"firstGoal":[],"missingFull":12,"rejections":[[1,"unfinished goal"],[2,"unfinished goal"],[3,"unfinished goal"],[4,"unfinished goal"],[5,"1. not the lowest eligible modifier level"],[6,"unfinished goal"]]}
```

## 호출 14 · Sacral Quiver · whittle-2 · slots

사전 기대는 호출 12의 12개 실행열과 같다.

전송 문안 `2e00e22558b8`, 응답 스키마 `651d0d0f2613`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Sacral Quiver","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"Y","effect":"(37—42)% increased Damage with Bow Skills","tier":3,"requiredLevel":46}],"suffix":[{"ref":"X","effect":"+1 to Level of all Projectile Skills","tier":1,"requiredLevel":5}]},"targets":{"prefix":[{"ref":"B","effect":"+(237—346) to Accuracy Rating","tier":3,"requiredLevel":58}],"suffix":[{"ref":"A","effect":"(30—34)% increased Critical Hit Chance for Attacks","tier":2,"requiredLevel":58}]},"mustRemove":["X","Y"],"materials":{"CHAOS":2},"rules":"Chaos with Whittling removes a lowest required-item-level eligible modifier; ties admit either. With an Erasure omen, restrict removal to its side first, then compare levels. Re-evaluate after the new ordinary target is drawn. Each token specifies a removal and a successful replacement.","menu":["whittle.X>A","sideWhittle.X>A","whittle.X>B","sideWhittle.X>B","whittle.Y>A","sideWhittle.Y>A","whittle.Y>B","sideWhittle.Y>B"],"omens":{"whittle":"Omen of Whittling","sideWhittle":{"X>A":"DEXTRAL_ERASURE","X>B":"DEXTRAL_ERASURE","Y>A":"SINISTRAL_ERASURE","Y>B":"SINISTRAL_ERASURE"}},"lastFailure":[{"route":["whittle.X>A"],"reason":"unfinished goal"},{"route":["sideWhittle.X>A"],"reason":"unfinished goal"}],"openingFields":{"begin_1":"whittle.X>A","begin_2":"sideWhittle.X>A","begin_3":"whittle.X>B","begin_4":"sideWhittle.X>B","begin_5":"whittle.Y>A","begin_6":"sideWhittle.Y>A","begin_7":"whittle.Y>B","begin_8":"sideWhittle.Y>B"}}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"begin_1\":[[\"whittle.X>A\",\"whittle.Y>A\"],[\"whittle.X>A\",\"sideWhittle.Y>A\"],[\"whittle.X>A\",\"whittle.Y>B\"],[\"whittle.X>A\",\"sideWhittle.Y>B\"]],\"begin_2\":[[\"sideWhittle.X>A\",\"sideWhittle.Y>A\"],[\"sideWhittle.X>A\",\"whittle.Y>A\"],[\"sideWhittle.X>A\",\"sideWhittle.Y>B\"],[\"sideWhittle.X>A\",\"whittle.Y>B\"]],\"begin_3\":[[\"whittle.X>B\",\"whittle.Y>A\"],[\"whittle.X>B\",\"sideWhittle.Y>A\"],[\"whittle.X>B\",\"whittle.Y>B\"],[\"whittle.X>B\",\"sideWhittle.Y>B\"]],\"begin_4\":[[\"sideWhittle.X>B\",\"sideWhittle.Y>A\"],[\"sideWhittle.X>B\",\"whittle.Y>A\"],[\"sideWhittle.X>B\",\"sideWhittle.Y>B\"],[\"sideWhittle.X>B\",\"whittle.Y>B\"]],\"begin_5\":[[\"whittle.Y>A\",\"whittle.X>A\"],[\"whittle.Y>A\",\"sideWhittle.X>A\"],[\"whittle.Y>A\",\"whittle.X>B\"],[\"whittle.Y>A\",\"sideWhittle.X>B\"]],\"begin_6\":[[\"sideWhittle.Y>A\",\"sideWhittle.X>A\"],[\"sideWhittle.Y>A\",\"whittle.X>A\"],[\"sideWhittle.Y>A\",\"sideWhittle.X>B\"],[\"sideWhittle.Y>A\",\"whittle.X>B\"]],\"begin_7\":[[\"whittle.Y>B\",\"whittle.X>A\"],[\"whittle.Y>B\",\"sideWhittle.X>A\"],[\"whittle.Y>B\",\"whittle.X>B\"],[\"whittle.Y>B\",\"sideWhittle.X>B\"]],\"begin_8\":[[\"sideWhittle.Y>B\",\"sideWhittle.X>A\"],[\"sideWhittle.Y>B\",\"whittle.X>A\"],[\"sideWhittle.Y>B\",\"sideWhittle.X>B\"],[\"sideWhittle.Y>B\",\"whittle.X>B\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["sideWhittle.X>A","sideWhittle.Y>B"],["sideWhittle.X>A","whittle.Y>B"],["sideWhittle.X>B","sideWhittle.Y>A"],["sideWhittle.X>B","whittle.Y>A"],["sideWhittle.Y>A","sideWhittle.X>B"],["sideWhittle.Y>A","whittle.X>B"],["sideWhittle.Y>B","sideWhittle.X>A"],["sideWhittle.Y>B","whittle.X>A"],["whittle.X>A","sideWhittle.Y>B"],["whittle.X>A","whittle.Y>B"],["whittle.X>B","sideWhittle.Y>A"],["whittle.X>B","whittle.Y>A"]],"firstGoal":[["sideWhittle.X>A","sideWhittle.Y>B"],["sideWhittle.X>A","whittle.Y>B"],["sideWhittle.X>B","sideWhittle.Y>A"],["sideWhittle.X>B","whittle.Y>A"],["sideWhittle.Y>A","sideWhittle.X>B"],["sideWhittle.Y>A","whittle.X>B"],["sideWhittle.Y>B","sideWhittle.X>A"],["sideWhittle.Y>B","whittle.X>A"],["whittle.X>A","sideWhittle.Y>B"],["whittle.X>A","whittle.Y>B"],["whittle.X>B","sideWhittle.Y>A"],["whittle.X>B","whittle.Y>A"]],"missingFull":0,"rejections":[[1,"2. A already present"],[2,"2. A already present"],[5,"2. A already present"],[6,"2. A already present"],[11,"2. B already present"],[12,"2. B already present"],[15,"2. B already present"],[16,"2. B already present"],[17,"1. not the lowest eligible modifier level"],[18,"1. not the lowest eligible modifier level"],[19,"1. not the lowest eligible modifier level"],[20,"1. not the lowest eligible modifier level"],[21,"2. A already present"],[22,"2. A already present"],[25,"1. not the lowest eligible modifier level"],[26,"1. not the lowest eligible modifier level"],[27,"1. not the lowest eligible modifier level"],[28,"1. not the lowest eligible modifier level"],[31,"2. B already present"],[32,"2. B already present"]]}
```

## 호출 15 · Blacksteel Gauntlets · socket-2 · initial

사전 기대 응답에서 인정하는 실행열 3개.

```json
[["artificer.open","exalt.A","install.Perfect_Desert_Rune@1"],["artificer.open","install.Perfect_Desert_Rune@1","exalt.A"],["exalt.A","artificer.open","install.Perfect_Desert_Rune@1"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `18c4e5caafce`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Blacksteel Gauntlets","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[],"openSockets":0},"targets":{"prefix":[],"suffix":[{"ref":"A","effect":"Gain (10—14) Mana per enemy killed","tier":5}],"sockets":{"1":"Perfect_Desert_Rune"}},"mustRemove":[],"materials":{"ARTIFICER":1,"INSTALL":1,"EXALTED":1},"rules":"Artificer opens one socket. Install requires its numbered socket already open. Exalted adds A on its successful roll. Preserve the installed runes.","menu":["artificer.open","install.Perfect_Desert_Rune@1","exalt.A"]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"artificer.open\",\"install.Perfect_Desert_Rune@1\",\"exalt.A\"],[\"artificer.open\",\"exalt.A\",\"install.Perfect_Desert_Rune@1\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["artificer.open","exalt.A","install.Perfect_Desert_Rune@1"],["artificer.open","install.Perfect_Desert_Rune@1","exalt.A"]],"firstGoal":[["artificer.open","exalt.A","install.Perfect_Desert_Rune@1"],["artificer.open","install.Perfect_Desert_Rune@1","exalt.A"]],"missingFull":1,"rejections":[]}
```

## 호출 16 · Primed Quiver · roll-3 · initial

사전 기대 응답에서 인정하는 실행열 3개.

```json
[["blessedDivine.I","exalt.A","divine.A"],["exalt.A","blessedDivine.I","divine.A"],["exalt.A","divine.A","blessedDivine.I"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `bc10f5f17fec`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Primed Quiver","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"B","effect":"(11—13)% increased Attack Speed","tier":2}]},"targets":{"prefix":[],"suffix":[{"ref":"A","effect":"(12—14)% chance to Pierce an Enemy","tier":5},{"ref":"B","effect":"(11—13)% increased Attack Speed","tier":2}]},"mustRemove":[],"materials":{"DIVINE":2,"EXALTED":1},"rules":"On the selected successful rolls, Divine with Omen of the Blessed rerolls the implicit to its upper endpoint. Ordinary Divine rerolls all non-fractured explicit values to their upper endpoints. Starting I and A are at their lower endpoints. Acquiring B does not reroll A or I. B is present instead of A on this card. Exalt.A selects the lower endpoint for the new A; the A goal requires the upper endpoint.","menu":["blessedDivine.I","divine.A","exalt.B","exalt.A"],"rolls":{"I":{"effect":"(7—10)% increased Attack Speed","stat":"attack_speed_+%","initial":7,"target":10},"A":{"effect":"(12—14)% chance to Pierce an Enemy","stat":"base_chance_to_pierce_%","initial":null,"target":14}}}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"exalt.A\",\"blessedDivine.I\"],[\"blessedDivine.I\",\"exalt.A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[],"firstGoal":[],"missingFull":3,"rejections":[[1,"unfinished goal"],[2,"unfinished goal"]]}
```

## 호출 17 · Primed Quiver · roll-3 · feedback

사전 기대는 호출 16의 3개 실행열과 같다.

전송 문안 `0171d492c9f2`, 응답 스키마 `bc10f5f17fec`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Primed Quiver","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"B","effect":"(11—13)% increased Attack Speed","tier":2}]},"targets":{"prefix":[],"suffix":[{"ref":"A","effect":"(12—14)% chance to Pierce an Enemy","tier":5},{"ref":"B","effect":"(11—13)% increased Attack Speed","tier":2}]},"mustRemove":[],"materials":{"DIVINE":2,"EXALTED":1},"rules":"On the selected successful rolls, Divine with Omen of the Blessed rerolls the implicit to its upper endpoint. Ordinary Divine rerolls all non-fractured explicit values to their upper endpoints. Starting I and A are at their lower endpoints. Acquiring B does not reroll A or I. B is present instead of A on this card. Exalt.A selects the lower endpoint for the new A; the A goal requires the upper endpoint.","menu":["blessedDivine.I","divine.A","exalt.B","exalt.A"],"rolls":{"I":{"effect":"(7—10)% increased Attack Speed","stat":"attack_speed_+%","initial":7,"target":10},"A":{"effect":"(12—14)% chance to Pierce an Enemy","stat":"base_chance_to_pierce_%","initial":null,"target":14}},"lastFailure":[{"route":["exalt.A","blessedDivine.I"],"reason":"unfinished goal"},{"route":["blessedDivine.I","exalt.A"],"reason":"unfinished goal"}]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"exalt.B\",\"blessedDivine.I\",\"divine.A\"],[\"exalt.B\",\"divine.A\"],[\"blessedDivine.I\",\"exalt.B\",\"divine.A\"],[\"blessedDivine.I\",\"divine.A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[],"firstGoal":[],"missingFull":3,"rejections":[[1,"1. B already present"],[2,"1. B already present"],[3,"2. B already present"],[4,"unfinished goal"]]}
```

## 호출 18 · Primed Quiver · roll-3 · slots

사전 기대는 호출 16의 3개 실행열과 같다.

전송 문안 `8fa9db4a46eb`, 응답 스키마 `d65b5b3a6684`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Primed Quiver","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"B","effect":"(11—13)% increased Attack Speed","tier":2}]},"targets":{"prefix":[],"suffix":[{"ref":"A","effect":"(12—14)% chance to Pierce an Enemy","tier":5},{"ref":"B","effect":"(11—13)% increased Attack Speed","tier":2}]},"mustRemove":[],"materials":{"DIVINE":2,"EXALTED":1},"rules":"On the selected successful rolls, Divine with Omen of the Blessed rerolls the implicit to its upper endpoint. Ordinary Divine rerolls all non-fractured explicit values to their upper endpoints. Starting I and A are at their lower endpoints. Acquiring B does not reroll A or I. B is present instead of A on this card. Exalt.A selects the lower endpoint for the new A; the A goal requires the upper endpoint.","menu":["blessedDivine.I","divine.A","exalt.B","exalt.A"],"rolls":{"I":{"effect":"(7—10)% increased Attack Speed","stat":"attack_speed_+%","initial":7,"target":10},"A":{"effect":"(12—14)% chance to Pierce an Enemy","stat":"base_chance_to_pierce_%","initial":null,"target":14}},"lastFailure":[{"route":["exalt.B","blessedDivine.I","divine.A"],"reason":"1. B already present"},{"route":["exalt.B","divine.A"],"reason":"1. B already present"}],"openingFields":{"begin_1":"blessedDivine.I","begin_2":"divine.A","begin_3":"exalt.B","begin_4":"exalt.A"}}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"begin_1\":[[\"blessedDivine.I\",\"divine.A\"]],\"begin_2\":[[\"divine.A\"]],\"begin_3\":[],\"begin_4\":[[\"exalt.A\",\"divine.A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[],"firstGoal":[],"missingFull":3,"rejections":[[1,"unfinished goal"],[2,"unfinished goal"],[3,"unfinished goal"]]}
```

## 호출 19 · Invoking Belt · alloy-3 · initial

사전 기대 응답에서 인정하는 실행열 2개.

```json
[["RUNIC_ALLOY.X>A","exalt.B"],["exalt.B","RUNIC_ALLOY.X>A"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `5b6ae221958e`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Invoking Belt","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"X","effect":"(16—21)% increased Charm Effect Duration","tier":3}],"suffix":[]},"targets":{"prefix":[{"ref":"A","effect":"(15—20)% increased Runic Ward Regeneration Rate","tier":1},{"ref":"B","effect":"+(30—39) to maximum Life","tier":8}],"suffix":[]},"mustRemove":["X"],"materials":{"EXALTED":2,"RUNIC_ALLOY":1},"rules":"The named alloy consumes one removable existing explicit and replaces it with A on the indicated outcome. A is alloy-exclusive. X can be rolled as expendable material. If B is consumed, it must be regained. One use of exalt can roll X or B.","menu":["exalt.B","exalt.X","RUNIC_ALLOY.X>A","RUNIC_ALLOY.B>A"],"sourceA":"Runic Alloy"}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"exalt.B\",\"RUNIC_ALLOY.B>A\"],[\"exalt.X\",\"RUNIC_ALLOY.X>A\",\"exalt.B\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[],"firstGoal":[],"missingFull":2,"rejections":[[1,"unfinished goal"],[2,"1. X already present"]]}
```

## 호출 20 · Invoking Belt · alloy-3 · feedback

사전 기대는 호출 19의 2개 실행열과 같다.

전송 문안 `85de0ceb25c9`, 응답 스키마 `5b6ae221958e`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Invoking Belt","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"X","effect":"(16—21)% increased Charm Effect Duration","tier":3}],"suffix":[]},"targets":{"prefix":[{"ref":"A","effect":"(15—20)% increased Runic Ward Regeneration Rate","tier":1},{"ref":"B","effect":"+(30—39) to maximum Life","tier":8}],"suffix":[]},"mustRemove":["X"],"materials":{"EXALTED":2,"RUNIC_ALLOY":1},"rules":"The named alloy consumes one removable existing explicit and replaces it with A on the indicated outcome. A is alloy-exclusive. X can be rolled as expendable material. If B is consumed, it must be regained. One use of exalt can roll X or B.","menu":["exalt.B","exalt.X","RUNIC_ALLOY.X>A","RUNIC_ALLOY.B>A"],"sourceA":"Runic Alloy","lastFailure":[{"route":["exalt.B","RUNIC_ALLOY.B>A"],"reason":"unfinished goal"},{"route":["exalt.X","RUNIC_ALLOY.X>A","exalt.B"],"reason":"1. X already present"}]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"exalt.X\",\"RUNIC_ALLOY.X>A\",\"exalt.B\"],[\"exalt.B\",\"RUNIC_ALLOY.B>A\",\"exalt.X\",\"RUNIC_ALLOY.X>A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[],"firstGoal":[],"missingFull":2,"rejections":[[1,"1. X already present"],[2,"3. X already present"]]}
```

## 호출 21 · Invoking Belt · alloy-3 · slots

사전 기대는 호출 19의 2개 실행열과 같다.

전송 문안 `b1365ca6be38`, 응답 스키마 `7dea0d7d4b59`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Invoking Belt","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"X","effect":"(16—21)% increased Charm Effect Duration","tier":3}],"suffix":[]},"targets":{"prefix":[{"ref":"A","effect":"(15—20)% increased Runic Ward Regeneration Rate","tier":1},{"ref":"B","effect":"+(30—39) to maximum Life","tier":8}],"suffix":[]},"mustRemove":["X"],"materials":{"EXALTED":2,"RUNIC_ALLOY":1},"rules":"The named alloy consumes one removable existing explicit and replaces it with A on the indicated outcome. A is alloy-exclusive. X can be rolled as expendable material. If B is consumed, it must be regained. One use of exalt can roll X or B.","menu":["exalt.B","exalt.X","RUNIC_ALLOY.X>A","RUNIC_ALLOY.B>A"],"sourceA":"Runic Alloy","lastFailure":[{"route":["exalt.X","RUNIC_ALLOY.X>A","exalt.B"],"reason":"1. X already present"},{"route":["exalt.B","RUNIC_ALLOY.B>A","exalt.X","RUNIC_ALLOY.X>A"],"reason":"3. X already present"}],"openingFields":{"begin_1":"exalt.B","begin_2":"exalt.X","begin_3":"RUNIC_ALLOY.X>A","begin_4":"RUNIC_ALLOY.B>A"}}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"begin_1\":[[\"exalt.B\",\"RUNIC_ALLOY.B>A\"]],\"begin_2\":[[\"exalt.X\",\"RUNIC_ALLOY.X>A\",\"exalt.B\",\"RUNIC_ALLOY.B>A\"]],\"begin_3\":[[\"RUNIC_ALLOY.X>A\",\"exalt.B\",\"RUNIC_ALLOY.B>A\"]],\"begin_4\":[[\"RUNIC_ALLOY.B>A\",\"exalt.X\",\"RUNIC_ALLOY.X>A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[],"firstGoal":[["RUNIC_ALLOY.X>A","exalt.B"]],"missingFull":2,"rejections":[[1,"unfinished goal"],[2,"1. X already present"],[3,"continues after goal"],[4,"1. B is absent"]]}
```

## 호출 22 · Wrath Sceptre · whittle-3 · initial

사전 기대 응답에서 인정하는 실행열 8개.

```json
[["sideWhittle.X>A","sideWhittle.Y>B"],["sideWhittle.X>A","whittle.Y>B"],["sideWhittle.Y>A","sideWhittle.X>B"],["sideWhittle.Y>A","whittle.X>B"],["sideWhittle.Y>B","sideWhittle.X>A"],["sideWhittle.Y>B","whittle.X>A"],["whittle.X>A","sideWhittle.Y>B"],["whittle.X>A","whittle.Y>B"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `c16a3a9fe325`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Wrath Sceptre","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"X","effect":"Allies in your Presence deal (45—54)% increased Damage","tier":6,"requiredLevel":16}],"suffix":[{"ref":"Y","effect":"+2 to Level of all Minion Skills","tier":3,"requiredLevel":25}]},"targets":{"prefix":[{"ref":"A","effect":"(56—60)% increased Spirit","tier":2,"requiredLevel":75}],"suffix":[{"ref":"B","effect":"Allies in your Presence have (15—19)% increased Critical Damage Bonus","tier":5,"requiredLevel":21}]},"mustRemove":["X","Y"],"materials":{"CHAOS":2},"rules":"Chaos with Whittling removes a lowest required-item-level eligible modifier; ties admit either. With an Erasure omen, restrict removal to its side first, then compare levels. Re-evaluate after the new ordinary target is drawn. Each token specifies a removal and a successful replacement.","menu":["whittle.X>A","sideWhittle.X>A","whittle.X>B","sideWhittle.X>B","whittle.Y>A","sideWhittle.Y>A","whittle.Y>B","sideWhittle.Y>B"],"omens":{"whittle":"Omen of Whittling","sideWhittle":{"X>A":"SINISTRAL_ERASURE","X>B":"SINISTRAL_ERASURE","Y>A":"DEXTRAL_ERASURE","Y>B":"DEXTRAL_ERASURE"}}}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"sideWhittle.X>A\"],[\"sideWhittle.X>B\"],[\"sideWhittle.Y>A\"],[\"sideWhittle.Y>B\"],[\"whittle.X>A\"],[\"whittle.X>B\"],[\"whittle.Y>A\"],[\"whittle.Y>B\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[],"firstGoal":[],"missingFull":8,"rejections":[[1,"unfinished goal"],[2,"unfinished goal"],[3,"unfinished goal"],[4,"unfinished goal"],[5,"unfinished goal"],[6,"unfinished goal"],[7,"1. not the lowest eligible modifier level"],[8,"1. not the lowest eligible modifier level"]]}
```

## 호출 23 · Wrath Sceptre · whittle-3 · feedback

사전 기대는 호출 22의 8개 실행열과 같다.

전송 문안 `387d267dd637`, 응답 스키마 `c16a3a9fe325`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Wrath Sceptre","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"X","effect":"Allies in your Presence deal (45—54)% increased Damage","tier":6,"requiredLevel":16}],"suffix":[{"ref":"Y","effect":"+2 to Level of all Minion Skills","tier":3,"requiredLevel":25}]},"targets":{"prefix":[{"ref":"A","effect":"(56—60)% increased Spirit","tier":2,"requiredLevel":75}],"suffix":[{"ref":"B","effect":"Allies in your Presence have (15—19)% increased Critical Damage Bonus","tier":5,"requiredLevel":21}]},"mustRemove":["X","Y"],"materials":{"CHAOS":2},"rules":"Chaos with Whittling removes a lowest required-item-level eligible modifier; ties admit either. With an Erasure omen, restrict removal to its side first, then compare levels. Re-evaluate after the new ordinary target is drawn. Each token specifies a removal and a successful replacement.","menu":["whittle.X>A","sideWhittle.X>A","whittle.X>B","sideWhittle.X>B","whittle.Y>A","sideWhittle.Y>A","whittle.Y>B","sideWhittle.Y>B"],"omens":{"whittle":"Omen of Whittling","sideWhittle":{"X>A":"SINISTRAL_ERASURE","X>B":"SINISTRAL_ERASURE","Y>A":"DEXTRAL_ERASURE","Y>B":"DEXTRAL_ERASURE"}},"lastFailure":[{"route":["sideWhittle.X>A"],"reason":"unfinished goal"},{"route":["sideWhittle.X>B"],"reason":"unfinished goal"}]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"whittle.Y>A\"],[\"whittle.Y>B\"],[\"whittle.X>A\"],[\"whittle.X>B\"],[\"sideWhittle.Y>A\"],[\"sideWhittle.Y>B\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[],"firstGoal":[],"missingFull":8,"rejections":[[1,"1. not the lowest eligible modifier level"],[2,"1. not the lowest eligible modifier level"],[3,"unfinished goal"],[4,"unfinished goal"],[5,"unfinished goal"],[6,"unfinished goal"]]}
```

## 호출 24 · Wrath Sceptre · whittle-3 · slots

사전 기대는 호출 22의 8개 실행열과 같다.

전송 문안 `2e00e22558b8`, 응답 스키마 `651d0d0f2613`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Wrath Sceptre","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"X","effect":"Allies in your Presence deal (45—54)% increased Damage","tier":6,"requiredLevel":16}],"suffix":[{"ref":"Y","effect":"+2 to Level of all Minion Skills","tier":3,"requiredLevel":25}]},"targets":{"prefix":[{"ref":"A","effect":"(56—60)% increased Spirit","tier":2,"requiredLevel":75}],"suffix":[{"ref":"B","effect":"Allies in your Presence have (15—19)% increased Critical Damage Bonus","tier":5,"requiredLevel":21}]},"mustRemove":["X","Y"],"materials":{"CHAOS":2},"rules":"Chaos with Whittling removes a lowest required-item-level eligible modifier; ties admit either. With an Erasure omen, restrict removal to its side first, then compare levels. Re-evaluate after the new ordinary target is drawn. Each token specifies a removal and a successful replacement.","menu":["whittle.X>A","sideWhittle.X>A","whittle.X>B","sideWhittle.X>B","whittle.Y>A","sideWhittle.Y>A","whittle.Y>B","sideWhittle.Y>B"],"omens":{"whittle":"Omen of Whittling","sideWhittle":{"X>A":"SINISTRAL_ERASURE","X>B":"SINISTRAL_ERASURE","Y>A":"DEXTRAL_ERASURE","Y>B":"DEXTRAL_ERASURE"}},"lastFailure":[{"route":["whittle.Y>A"],"reason":"1. not the lowest eligible modifier level"},{"route":["whittle.Y>B"],"reason":"1. not the lowest eligible modifier level"}],"openingFields":{"begin_1":"whittle.X>A","begin_2":"sideWhittle.X>A","begin_3":"whittle.X>B","begin_4":"sideWhittle.X>B","begin_5":"whittle.Y>A","begin_6":"sideWhittle.Y>A","begin_7":"whittle.Y>B","begin_8":"sideWhittle.Y>B"}}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"begin_1\":[[\"whittle.X>A\"]],\"begin_2\":[[\"sideWhittle.X>A\"]],\"begin_3\":[[\"whittle.X>B\"]],\"begin_4\":[[\"sideWhittle.X>B\"]],\"begin_5\":[],\"begin_6\":[],\"begin_7\":[],\"begin_8\":[]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[],"firstGoal":[],"missingFull":8,"rejections":[[1,"unfinished goal"],[2,"unfinished goal"],[3,"unfinished goal"],[4,"unfinished goal"]]}
```

## 호출 25 · Polished Bracers · socket-3 · initial

사전 기대 응답에서 인정하는 실행열 3개.

```json
[["artificer.open","exalt.A","install.Perfect_Glacial_Rune@1"],["artificer.open","install.Perfect_Glacial_Rune@1","exalt.A"],["exalt.A","artificer.open","install.Perfect_Glacial_Rune@1"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `932d6845f04a`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Polished Bracers","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[],"openSockets":0},"targets":{"prefix":[],"suffix":[{"ref":"A","effect":"Leech (8—8.9)% of Physical Attack Damage as Mana","tier":1}],"sockets":{"1":"Perfect_Glacial_Rune"}},"mustRemove":[],"materials":{"ARTIFICER":1,"INSTALL":1,"EXALTED":1},"rules":"Artificer opens one socket. Install requires its numbered socket already open. Exalted adds A on its successful roll. Preserve the installed runes.","menu":["artificer.open","install.Perfect_Glacial_Rune@1","exalt.A"]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"artificer.open\",\"install.Perfect_Glacial_Rune@1\",\"exalt.A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["artificer.open","install.Perfect_Glacial_Rune@1","exalt.A"]],"firstGoal":[["artificer.open","install.Perfect_Glacial_Rune@1","exalt.A"]],"missingFull":2,"rejections":[]}
```

## 호출 26 · Azure Amulet · roll-4 · initial

사전 기대 응답에서 인정하는 실행열 6개.

```json
[["blessedDivine.I","divine.A","exalt.B"],["blessedDivine.I","exalt.B","divine.A"],["divine.A","blessedDivine.I","exalt.B"],["divine.A","exalt.B","blessedDivine.I"],["exalt.B","blessedDivine.I","divine.A"],["exalt.B","divine.A","blessedDivine.I"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `f05d1ea0d1ef`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Azure Amulet","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","effect":"+(21—25)% to Fire Resistance","tier":5}]},"targets":{"prefix":[{"ref":"B","effect":"+(70—84) to maximum Life","tier":4}],"suffix":[{"ref":"A","effect":"+(21—25)% to Fire Resistance","tier":5}]},"mustRemove":[],"materials":{"DIVINE":2,"EXALTED":1},"rules":"On the selected successful rolls, Divine with Omen of the Blessed rerolls the implicit to its upper endpoint. Ordinary Divine rerolls all non-fractured explicit values to their upper endpoints. Starting I and A are at their lower endpoints. Acquiring B does not reroll A or I.","menu":["blessedDivine.I","divine.A","exalt.B"],"rolls":{"I":{"effect":"(20—30)% increased Mana Regeneration Rate","stat":"mana_regeneration_rate_+%","initial":20,"target":30},"A":{"effect":"+(21—25)% to Fire Resistance","stat":"base_fire_damage_resistance_%","initial":21,"target":25}}}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"blessedDivine.I\",\"exalt.B\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[],"firstGoal":[],"missingFull":6,"rejections":[[1,"unfinished goal"]]}
```

## 호출 27 · Azure Amulet · roll-4 · feedback

사전 기대는 호출 26의 6개 실행열과 같다.

전송 문안 `0171d492c9f2`, 응답 스키마 `f05d1ea0d1ef`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Azure Amulet","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","effect":"+(21—25)% to Fire Resistance","tier":5}]},"targets":{"prefix":[{"ref":"B","effect":"+(70—84) to maximum Life","tier":4}],"suffix":[{"ref":"A","effect":"+(21—25)% to Fire Resistance","tier":5}]},"mustRemove":[],"materials":{"DIVINE":2,"EXALTED":1},"rules":"On the selected successful rolls, Divine with Omen of the Blessed rerolls the implicit to its upper endpoint. Ordinary Divine rerolls all non-fractured explicit values to their upper endpoints. Starting I and A are at their lower endpoints. Acquiring B does not reroll A or I.","menu":["blessedDivine.I","divine.A","exalt.B"],"rolls":{"I":{"effect":"(20—30)% increased Mana Regeneration Rate","stat":"mana_regeneration_rate_+%","initial":20,"target":30},"A":{"effect":"+(21—25)% to Fire Resistance","stat":"base_fire_damage_resistance_%","initial":21,"target":25}},"lastFailure":[{"route":["blessedDivine.I","exalt.B"],"reason":"unfinished goal"}]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"blessedDivine.I\",\"exalt.B\"],[\"exalt.B\",\"blessedDivine.I\"],[\"divine.A\",\"exalt.B\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[],"firstGoal":[],"missingFull":6,"rejections":[[1,"unfinished goal"],[2,"unfinished goal"],[3,"unfinished goal"]]}
```

## 호출 28 · Azure Amulet · roll-4 · slots

사전 기대는 호출 26의 6개 실행열과 같다.

전송 문안 `8fa9db4a46eb`, 응답 스키마 `dac81e42e7ae`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Azure Amulet","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","effect":"+(21—25)% to Fire Resistance","tier":5}]},"targets":{"prefix":[{"ref":"B","effect":"+(70—84) to maximum Life","tier":4}],"suffix":[{"ref":"A","effect":"+(21—25)% to Fire Resistance","tier":5}]},"mustRemove":[],"materials":{"DIVINE":2,"EXALTED":1},"rules":"On the selected successful rolls, Divine with Omen of the Blessed rerolls the implicit to its upper endpoint. Ordinary Divine rerolls all non-fractured explicit values to their upper endpoints. Starting I and A are at their lower endpoints. Acquiring B does not reroll A or I.","menu":["blessedDivine.I","divine.A","exalt.B"],"rolls":{"I":{"effect":"(20—30)% increased Mana Regeneration Rate","stat":"mana_regeneration_rate_+%","initial":20,"target":30},"A":{"effect":"+(21—25)% to Fire Resistance","stat":"base_fire_damage_resistance_%","initial":21,"target":25}},"lastFailure":[{"route":["blessedDivine.I","exalt.B"],"reason":"unfinished goal"},{"route":["exalt.B","blessedDivine.I"],"reason":"unfinished goal"}],"openingFields":{"begin_1":"blessedDivine.I","begin_2":"divine.A","begin_3":"exalt.B"}}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"begin_1\":[[\"blessedDivine.I\",\"exalt.B\"]],\"begin_2\":[[\"divine.A\",\"blessedDivine.I\",\"exalt.B\"],[\"divine.A\",\"exalt.B\",\"blessedDivine.I\"]],\"begin_3\":[[\"exalt.B\",\"blessedDivine.I\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["divine.A","blessedDivine.I","exalt.B"],["divine.A","exalt.B","blessedDivine.I"]],"firstGoal":[["divine.A","blessedDivine.I","exalt.B"],["divine.A","exalt.B","blessedDivine.I"]],"missingFull":4,"rejections":[[1,"unfinished goal"],[4,"unfinished goal"]]}
```

## 호출 29 · Adherent Cuffs · alloy-4 · initial

사전 기대 응답에서 인정하는 실행열 4개.

```json
[["exalt.B","SWIFT_ALLOY.B>A","exalt.B"],["exalt.B","exalt.X","SWIFT_ALLOY.X>A"],["exalt.X","SWIFT_ALLOY.X>A","exalt.B"],["exalt.X","exalt.B","SWIFT_ALLOY.X>A"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `336b071008b6`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Adherent Cuffs","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"targets":{"prefix":[],"suffix":[{"ref":"A","effect":"(9—12)% increased Cast Speed","tier":1},{"ref":"B","effect":"+(34—36) to Dexterity","tier":1}]},"mustRemove":["X"],"materials":{"EXALTED":2,"SWIFT_ALLOY":1},"rules":"The named alloy consumes one removable existing explicit and replaces it with A on the indicated outcome. A is alloy-exclusive. X can be rolled as expendable material. If B is consumed, it must be regained. One use of exalt can roll X or B.","menu":["exalt.B","exalt.X","SWIFT_ALLOY.X>A","SWIFT_ALLOY.B>A"],"intermediate":[{"ref":"X","effect":"+(12—15)% to Chaos Resistance","tier":4}],"sourceA":"Swift Alloy"}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"exalt.X\",\"SWIFT_ALLOY.X>A\",\"exalt.B\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["exalt.X","SWIFT_ALLOY.X>A","exalt.B"]],"firstGoal":[["exalt.X","SWIFT_ALLOY.X>A","exalt.B"]],"missingFull":3,"rejections":[]}
```

## 호출 30 · Lazuli Ring · whittle-4 · initial

사전 기대 응답에서 인정하는 실행열 12개.

```json
[["sideWhittle.X>A","sideWhittle.Y>B"],["sideWhittle.X>A","whittle.Y>B"],["sideWhittle.X>B","sideWhittle.Y>A"],["sideWhittle.X>B","whittle.Y>A"],["sideWhittle.Y>A","sideWhittle.X>B"],["sideWhittle.Y>A","whittle.X>B"],["sideWhittle.Y>B","sideWhittle.X>A"],["sideWhittle.Y>B","whittle.X>A"],["whittle.X>A","sideWhittle.Y>B"],["whittle.X>A","whittle.Y>B"],["whittle.X>B","sideWhittle.Y>A"],["whittle.X>B","whittle.Y>A"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `c16a3a9fe325`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Lazuli Ring","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"Y","effect":"+(35—54) to maximum Mana","tier":9,"requiredLevel":25}],"suffix":[{"ref":"X","effect":"Gain (4—5) Mana per enemy killed","tier":5,"requiredLevel":12}]},"targets":{"prefix":[{"ref":"B","effect":"(12—15)% increased Rarity of Items found","tier":2,"requiredLevel":29}],"suffix":[{"ref":"A","effect":"+(24—27)% to Chaos Resistance","tier":1,"requiredLevel":81}]},"mustRemove":["X","Y"],"materials":{"CHAOS":2},"rules":"Chaos with Whittling removes a lowest required-item-level eligible modifier; ties admit either. With an Erasure omen, restrict removal to its side first, then compare levels. Re-evaluate after the new ordinary target is drawn. Each token specifies a removal and a successful replacement.","menu":["whittle.X>A","sideWhittle.X>A","whittle.X>B","sideWhittle.X>B","whittle.Y>A","sideWhittle.Y>A","whittle.Y>B","sideWhittle.Y>B"],"omens":{"whittle":"Omen of Whittling","sideWhittle":{"X>A":"DEXTRAL_ERASURE","X>B":"DEXTRAL_ERASURE","Y>A":"SINISTRAL_ERASURE","Y>B":"SINISTRAL_ERASURE"}}}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"whittle.Y>A\"],[\"sideWhittle.Y>A\"],[\"whittle.X>A\"],[\"sideWhittle.X>A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[],"firstGoal":[],"missingFull":12,"rejections":[[1,"1. not the lowest eligible modifier level"],[2,"unfinished goal"],[3,"unfinished goal"],[4,"unfinished goal"]]}
```

## 호출 31 · Lazuli Ring · whittle-4 · feedback

사전 기대는 호출 30의 12개 실행열과 같다.

전송 문안 `387d267dd637`, 응답 스키마 `c16a3a9fe325`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Lazuli Ring","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"Y","effect":"+(35—54) to maximum Mana","tier":9,"requiredLevel":25}],"suffix":[{"ref":"X","effect":"Gain (4—5) Mana per enemy killed","tier":5,"requiredLevel":12}]},"targets":{"prefix":[{"ref":"B","effect":"(12—15)% increased Rarity of Items found","tier":2,"requiredLevel":29}],"suffix":[{"ref":"A","effect":"+(24—27)% to Chaos Resistance","tier":1,"requiredLevel":81}]},"mustRemove":["X","Y"],"materials":{"CHAOS":2},"rules":"Chaos with Whittling removes a lowest required-item-level eligible modifier; ties admit either. With an Erasure omen, restrict removal to its side first, then compare levels. Re-evaluate after the new ordinary target is drawn. Each token specifies a removal and a successful replacement.","menu":["whittle.X>A","sideWhittle.X>A","whittle.X>B","sideWhittle.X>B","whittle.Y>A","sideWhittle.Y>A","whittle.Y>B","sideWhittle.Y>B"],"omens":{"whittle":"Omen of Whittling","sideWhittle":{"X>A":"DEXTRAL_ERASURE","X>B":"DEXTRAL_ERASURE","Y>A":"SINISTRAL_ERASURE","Y>B":"SINISTRAL_ERASURE"}},"lastFailure":[{"route":["whittle.Y>A"],"reason":"1. not the lowest eligible modifier level"},{"route":["sideWhittle.Y>A"],"reason":"unfinished goal"}]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"whittle.X>B\"],[\"sideWhittle.X>B\"],[\"whittle.Y>B\"],[\"sideWhittle.Y>B\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[],"firstGoal":[],"missingFull":12,"rejections":[[1,"unfinished goal"],[2,"unfinished goal"],[3,"1. not the lowest eligible modifier level"],[4,"unfinished goal"]]}
```

## 호출 32 · Lazuli Ring · whittle-4 · slots

사전 기대는 호출 30의 12개 실행열과 같다.

전송 문안 `2e00e22558b8`, 응답 스키마 `651d0d0f2613`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Lazuli Ring","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"Y","effect":"+(35—54) to maximum Mana","tier":9,"requiredLevel":25}],"suffix":[{"ref":"X","effect":"Gain (4—5) Mana per enemy killed","tier":5,"requiredLevel":12}]},"targets":{"prefix":[{"ref":"B","effect":"(12—15)% increased Rarity of Items found","tier":2,"requiredLevel":29}],"suffix":[{"ref":"A","effect":"+(24—27)% to Chaos Resistance","tier":1,"requiredLevel":81}]},"mustRemove":["X","Y"],"materials":{"CHAOS":2},"rules":"Chaos with Whittling removes a lowest required-item-level eligible modifier; ties admit either. With an Erasure omen, restrict removal to its side first, then compare levels. Re-evaluate after the new ordinary target is drawn. Each token specifies a removal and a successful replacement.","menu":["whittle.X>A","sideWhittle.X>A","whittle.X>B","sideWhittle.X>B","whittle.Y>A","sideWhittle.Y>A","whittle.Y>B","sideWhittle.Y>B"],"omens":{"whittle":"Omen of Whittling","sideWhittle":{"X>A":"DEXTRAL_ERASURE","X>B":"DEXTRAL_ERASURE","Y>A":"SINISTRAL_ERASURE","Y>B":"SINISTRAL_ERASURE"}},"lastFailure":[{"route":["whittle.X>B"],"reason":"unfinished goal"},{"route":["sideWhittle.X>B"],"reason":"unfinished goal"}],"openingFields":{"begin_1":"whittle.X>A","begin_2":"sideWhittle.X>A","begin_3":"whittle.X>B","begin_4":"sideWhittle.X>B","begin_5":"whittle.Y>A","begin_6":"sideWhittle.Y>A","begin_7":"whittle.Y>B","begin_8":"sideWhittle.Y>B"}}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"begin_1\":[[\"whittle.X>A\"]],\"begin_2\":[[\"sideWhittle.X>A\"]],\"begin_3\":[[\"whittle.X>B\",\"whittle.Y>A\"],[\"whittle.X>B\",\"sideWhittle.Y>A\"]],\"begin_4\":[[\"sideWhittle.X>B\",\"whittle.Y>A\"],[\"sideWhittle.X>B\",\"sideWhittle.Y>A\"]],\"begin_5\":[[\"whittle.Y>A\"]],\"begin_6\":[[\"sideWhittle.Y>A\"]],\"begin_7\":[[\"whittle.Y>B\",\"whittle.X>A\"],[\"whittle.Y>B\",\"sideWhittle.X>A\"]],\"begin_8\":[[\"sideWhittle.Y>B\",\"whittle.X>A\"],[\"sideWhittle.Y>B\",\"sideWhittle.X>A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["sideWhittle.X>B","sideWhittle.Y>A"],["sideWhittle.X>B","whittle.Y>A"],["sideWhittle.Y>B","sideWhittle.X>A"],["sideWhittle.Y>B","whittle.X>A"],["whittle.X>B","sideWhittle.Y>A"],["whittle.X>B","whittle.Y>A"]],"firstGoal":[["sideWhittle.X>B","sideWhittle.Y>A"],["sideWhittle.X>B","whittle.Y>A"],["sideWhittle.Y>B","sideWhittle.X>A"],["sideWhittle.Y>B","whittle.X>A"],["whittle.X>B","sideWhittle.Y>A"],["whittle.X>B","whittle.Y>A"]],"missingFull":6,"rejections":[[1,"unfinished goal"],[2,"unfinished goal"],[7,"1. not the lowest eligible modifier level"],[8,"unfinished goal"],[9,"1. not the lowest eligible modifier level"],[10,"1. not the lowest eligible modifier level"]]}
```

## 호출 33 · Utzaal Cuirass · socket-4 · initial

사전 기대 응답에서 인정하는 실행열 15개.

```json
[["artificer.open","artificer.open","exalt.A","install.Perfect_Glacial_Rune@1","install.Perfect_Storm_Rune@2"],["artificer.open","artificer.open","exalt.A","install.Perfect_Storm_Rune@2","install.Perfect_Glacial_Rune@1"],["artificer.open","artificer.open","install.Perfect_Glacial_Rune@1","exalt.A","install.Perfect_Storm_Rune@2"],["artificer.open","artificer.open","install.Perfect_Glacial_Rune@1","install.Perfect_Storm_Rune@2","exalt.A"],["artificer.open","artificer.open","install.Perfect_Storm_Rune@2","exalt.A","install.Perfect_Glacial_Rune@1"],["artificer.open","artificer.open","install.Perfect_Storm_Rune@2","install.Perfect_Glacial_Rune@1","exalt.A"],["artificer.open","exalt.A","artificer.open","install.Perfect_Glacial_Rune@1","install.Perfect_Storm_Rune@2"],["artificer.open","exalt.A","artificer.open","install.Perfect_Storm_Rune@2","install.Perfect_Glacial_Rune@1"],["artificer.open","exalt.A","install.Perfect_Glacial_Rune@1","artificer.open","install.Perfect_Storm_Rune@2"],["artificer.open","install.Perfect_Glacial_Rune@1","artificer.open","exalt.A","install.Perfect_Storm_Rune@2"],["artificer.open","install.Perfect_Glacial_Rune@1","artificer.open","install.Perfect_Storm_Rune@2","exalt.A"],["artificer.open","install.Perfect_Glacial_Rune@1","exalt.A","artificer.open","install.Perfect_Storm_Rune@2"],["exalt.A","artificer.open","artificer.open","install.Perfect_Glacial_Rune@1","install.Perfect_Storm_Rune@2"],["exalt.A","artificer.open","artificer.open","install.Perfect_Storm_Rune@2","install.Perfect_Glacial_Rune@1"],["exalt.A","artificer.open","install.Perfect_Glacial_Rune@1","artificer.open","install.Perfect_Storm_Rune@2"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `2f281f581ddf`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Utzaal Cuirass","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[],"openSockets":0},"targets":{"prefix":[],"suffix":[{"ref":"A","effect":"+(13—16) to Strength","tier":6}],"sockets":{"1":"Perfect_Glacial_Rune","2":"Perfect_Storm_Rune"}},"mustRemove":[],"materials":{"ARTIFICER":2,"INSTALL":2,"EXALTED":1},"rules":"Artificer opens one socket. Install requires its numbered socket already open. Exalted adds A on its successful roll. Preserve the installed runes.","menu":["artificer.open","install.Perfect_Glacial_Rune@1","install.Perfect_Storm_Rune@2","exalt.A"]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"artificer.open\",\"install.Perfect_Glacial_Rune@1\",\"artificer.open\",\"install.Perfect_Storm_Rune@2\",\"exalt.A\"],[\"artificer.open\",\"install.Perfect_Storm_Rune@2\",\"artificer.open\",\"install.Perfect_Glacial_Rune@1\",\"exalt.A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["artificer.open","install.Perfect_Glacial_Rune@1","artificer.open","install.Perfect_Storm_Rune@2","exalt.A"]],"firstGoal":[["artificer.open","install.Perfect_Glacial_Rune@1","artificer.open","install.Perfect_Storm_Rune@2","exalt.A"]],"missingFull":14,"rejections":[[2,"2. socket 2 not open"]]}
```

## 호출 34 · Heavy Belt · roll-5 · initial

사전 기대 응답에서 인정하는 실행열 6개.

```json
[["blessedDivine.I","divine.A","exalt.B"],["blessedDivine.I","exalt.B","divine.A"],["divine.A","blessedDivine.I","exalt.B"],["divine.A","exalt.B","blessedDivine.I"],["exalt.B","blessedDivine.I","divine.A"],["exalt.B","divine.A","blessedDivine.I"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `f05d1ea0d1ef`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Heavy Belt","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"A","effect":"+(105—124) to maximum Mana","tier":1}],"suffix":[]},"targets":{"prefix":[{"ref":"A","effect":"+(105—124) to maximum Mana","tier":1},{"ref":"B","effect":"(23—28)% increased Flask Life Recovery rate","tier":5}],"suffix":[]},"mustRemove":[],"materials":{"DIVINE":2,"EXALTED":1},"rules":"On the selected successful rolls, Divine with Omen of the Blessed rerolls the implicit to its upper endpoint. Ordinary Divine rerolls all non-fractured explicit values to their upper endpoints. Starting I and A are at their lower endpoints. Acquiring B does not reroll A or I.","menu":["blessedDivine.I","divine.A","exalt.B"],"rolls":{"I":{"effect":"(20—30)% increased Stun Threshold","stat":"stun_threshold_+%","initial":20,"target":30},"A":{"effect":"+(105—124) to maximum Mana","stat":"base_maximum_mana","initial":105,"target":124}}}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"blessedDivine.I\",\"divine.A\",\"exalt.B\"],[\"divine.A\",\"blessedDivine.I\",\"exalt.B\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["blessedDivine.I","divine.A","exalt.B"],["divine.A","blessedDivine.I","exalt.B"]],"firstGoal":[["blessedDivine.I","divine.A","exalt.B"],["divine.A","blessedDivine.I","exalt.B"]],"missingFull":4,"rejections":[]}
```

## 호출 35 · Kinetic Ring · alloy-5 · initial

사전 기대 응답에서 인정하는 실행열 2개.

```json
[["RUNIC_ALLOY.X>A","exalt.B"],["exalt.B","RUNIC_ALLOY.X>A"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `5b6ae221958e`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Kinetic Ring","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"X","effect":"+(15—24) to maximum Mana","tier":11}],"suffix":[]},"targets":{"prefix":[{"ref":"A","effect":"+(37—49) to maximum Runic Ward","tier":1}],"suffix":[{"ref":"B","effect":"Gain (2—3) Mana per enemy killed","tier":6}]},"mustRemove":["X"],"materials":{"EXALTED":2,"RUNIC_ALLOY":1},"rules":"The named alloy consumes one removable existing explicit and replaces it with A on the indicated outcome. A is alloy-exclusive. X can be rolled as expendable material. If B is consumed, it must be regained. One use of exalt can roll X or B.","menu":["exalt.B","exalt.X","RUNIC_ALLOY.X>A","RUNIC_ALLOY.B>A"],"sourceA":"Runic Alloy"}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"exalt.X\",\"RUNIC_ALLOY.X>A\",\"exalt.B\"],[\"exalt.B\",\"RUNIC_ALLOY.B>A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[],"firstGoal":[],"missingFull":2,"rejections":[[1,"1. X already present"],[2,"unfinished goal"]]}
```

## 호출 36 · Death Mantle · socket-5 · initial

사전 기대 응답에서 인정하는 실행열 12개.

```json
[["artificer.open","exalt.A","install.Perfect_Glacial_Rune@2","install.Perfect_Storm_Rune@1"],["artificer.open","exalt.A","install.Perfect_Storm_Rune@1","install.Perfect_Glacial_Rune@2"],["artificer.open","install.Perfect_Glacial_Rune@2","exalt.A","install.Perfect_Storm_Rune@1"],["artificer.open","install.Perfect_Glacial_Rune@2","install.Perfect_Storm_Rune@1","exalt.A"],["artificer.open","install.Perfect_Storm_Rune@1","exalt.A","install.Perfect_Glacial_Rune@2"],["artificer.open","install.Perfect_Storm_Rune@1","install.Perfect_Glacial_Rune@2","exalt.A"],["exalt.A","artificer.open","install.Perfect_Glacial_Rune@2","install.Perfect_Storm_Rune@1"],["exalt.A","artificer.open","install.Perfect_Storm_Rune@1","install.Perfect_Glacial_Rune@2"],["exalt.A","install.Perfect_Storm_Rune@1","artificer.open","install.Perfect_Glacial_Rune@2"],["install.Perfect_Storm_Rune@1","artificer.open","exalt.A","install.Perfect_Glacial_Rune@2"],["install.Perfect_Storm_Rune@1","artificer.open","install.Perfect_Glacial_Rune@2","exalt.A"],["install.Perfect_Storm_Rune@1","exalt.A","artificer.open","install.Perfect_Glacial_Rune@2"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `9685ced97c74`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Death Mantle","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[],"openSockets":1},"targets":{"prefix":[],"suffix":[{"ref":"A","effect":"+(98—124) to Stun Threshold","tier":5}],"sockets":{"1":"Perfect_Storm_Rune","2":"Perfect_Glacial_Rune"}},"mustRemove":[],"materials":{"ARTIFICER":1,"INSTALL":2,"EXALTED":1},"rules":"Artificer opens one socket. Install requires its numbered socket already open. Exalted adds A on its successful roll. Preserve the installed runes.","menu":["artificer.open","install.Perfect_Storm_Rune@1","install.Perfect_Glacial_Rune@2","exalt.A"]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"artificer.open\",\"install.Perfect_Storm_Rune@1\",\"install.Perfect_Glacial_Rune@2\",\"exalt.A\"],[\"artificer.open\",\"install.Perfect_Glacial_Rune@2\",\"install.Perfect_Storm_Rune@1\",\"exalt.A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["artificer.open","install.Perfect_Glacial_Rune@2","install.Perfect_Storm_Rune@1","exalt.A"],["artificer.open","install.Perfect_Storm_Rune@1","install.Perfect_Glacial_Rune@2","exalt.A"]],"firstGoal":[["artificer.open","install.Perfect_Glacial_Rune@2","install.Perfect_Storm_Rune@1","exalt.A"],["artificer.open","install.Perfect_Storm_Rune@1","install.Perfect_Glacial_Rune@2","exalt.A"]],"missingFull":10,"rejections":[]}
```

## 호출 37 · Massive Mitts · socket-6 · initial

사전 기대 응답에서 인정하는 실행열 3개.

```json
[["artificer.open","exalt.A","install.Perfect_Desert_Rune@1"],["artificer.open","install.Perfect_Desert_Rune@1","exalt.A"],["exalt.A","artificer.open","install.Perfect_Desert_Rune@1"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `18c4e5caafce`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Massive Mitts","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[],"openSockets":0},"targets":{"prefix":[{"ref":"A","effect":"+(85—99) to maximum Life","tier":3}],"suffix":[],"sockets":{"1":"Perfect_Desert_Rune"}},"mustRemove":[],"materials":{"ARTIFICER":1,"INSTALL":1,"EXALTED":1},"rules":"Artificer opens one socket. Install requires its numbered socket already open. Exalted adds A on its successful roll. Preserve the installed runes.","menu":["artificer.open","install.Perfect_Desert_Rune@1","exalt.A"]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"artificer.open\",\"install.Perfect_Desert_Rune@1\",\"exalt.A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["artificer.open","install.Perfect_Desert_Rune@1","exalt.A"]],"firstGoal":[["artificer.open","install.Perfect_Desert_Rune@1","exalt.A"]],"missingFull":2,"rejections":[]}
```

## 호출 38 · Falconer's Jacket · socket-7 · initial

사전 기대 응답에서 인정하는 실행열 12개.

```json
[["artificer.open","exalt.A","install.Perfect_Glacial_Rune@2","install.Perfect_Storm_Rune@1"],["artificer.open","exalt.A","install.Perfect_Storm_Rune@1","install.Perfect_Glacial_Rune@2"],["artificer.open","install.Perfect_Glacial_Rune@2","exalt.A","install.Perfect_Storm_Rune@1"],["artificer.open","install.Perfect_Glacial_Rune@2","install.Perfect_Storm_Rune@1","exalt.A"],["artificer.open","install.Perfect_Storm_Rune@1","exalt.A","install.Perfect_Glacial_Rune@2"],["artificer.open","install.Perfect_Storm_Rune@1","install.Perfect_Glacial_Rune@2","exalt.A"],["exalt.A","artificer.open","install.Perfect_Glacial_Rune@2","install.Perfect_Storm_Rune@1"],["exalt.A","artificer.open","install.Perfect_Storm_Rune@1","install.Perfect_Glacial_Rune@2"],["exalt.A","install.Perfect_Storm_Rune@1","artificer.open","install.Perfect_Glacial_Rune@2"],["install.Perfect_Storm_Rune@1","artificer.open","exalt.A","install.Perfect_Glacial_Rune@2"],["install.Perfect_Storm_Rune@1","artificer.open","install.Perfect_Glacial_Rune@2","exalt.A"],["install.Perfect_Storm_Rune@1","exalt.A","artificer.open","install.Perfect_Glacial_Rune@2"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `9685ced97c74`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Falconer's Jacket","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[],"openSockets":1},"targets":{"prefix":[],"suffix":[{"ref":"A","effect":"+(31—33) to Dexterity","tier":2}],"sockets":{"1":"Perfect_Storm_Rune","2":"Perfect_Glacial_Rune"}},"mustRemove":[],"materials":{"ARTIFICER":1,"INSTALL":2,"EXALTED":1},"rules":"Artificer opens one socket. Install requires its numbered socket already open. Exalted adds A on its successful roll. Preserve the installed runes.","menu":["artificer.open","install.Perfect_Storm_Rune@1","install.Perfect_Glacial_Rune@2","exalt.A"]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"artificer.open\",\"install.Perfect_Storm_Rune@1\",\"install.Perfect_Glacial_Rune@2\",\"exalt.A\"],[\"artificer.open\",\"install.Perfect_Glacial_Rune@2\",\"install.Perfect_Storm_Rune@1\",\"exalt.A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["artificer.open","install.Perfect_Glacial_Rune@2","install.Perfect_Storm_Rune@1","exalt.A"],["artificer.open","install.Perfect_Storm_Rune@1","install.Perfect_Glacial_Rune@2","exalt.A"]],"firstGoal":[["artificer.open","install.Perfect_Glacial_Rune@2","install.Perfect_Storm_Rune@1","exalt.A"],["artificer.open","install.Perfect_Storm_Rune@1","install.Perfect_Glacial_Rune@2","exalt.A"]],"missingFull":10,"rejections":[]}
```

## 호출 39 · Sirenscale Gloves · socket-8 · initial

사전 기대 응답에서 인정하는 실행열 3개.

```json
[["artificer.open","exalt.A","install.Perfect_Storm_Rune@1"],["artificer.open","install.Perfect_Storm_Rune@1","exalt.A"],["exalt.A","artificer.open","install.Perfect_Storm_Rune@1"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `e53d4e331c5e`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Sirenscale Gloves","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[],"openSockets":0},"targets":{"prefix":[],"suffix":[{"ref":"A","effect":"(15—19)% increased Critical Damage Bonus","tier":4}],"sockets":{"1":"Perfect_Storm_Rune"}},"mustRemove":[],"materials":{"ARTIFICER":1,"INSTALL":1,"EXALTED":1},"rules":"Artificer opens one socket. Install requires its numbered socket already open. Exalted adds A on its successful roll. Preserve the installed runes.","menu":["artificer.open","install.Perfect_Storm_Rune@1","exalt.A"]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"artificer.open\",\"install.Perfect_Storm_Rune@1\",\"exalt.A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["artificer.open","install.Perfect_Storm_Rune@1","exalt.A"]],"firstGoal":[["artificer.open","install.Perfect_Storm_Rune@1","exalt.A"]],"missingFull":2,"rejections":[]}
```

## 호출 40 · Wolfskin Mantle · socket-9 · initial

사전 기대 응답에서 인정하는 실행열 12개.

```json
[["artificer.open","exalt.A","install.Perfect_Glacial_Rune@2","install.Perfect_Storm_Rune@1"],["artificer.open","exalt.A","install.Perfect_Storm_Rune@1","install.Perfect_Glacial_Rune@2"],["artificer.open","install.Perfect_Glacial_Rune@2","exalt.A","install.Perfect_Storm_Rune@1"],["artificer.open","install.Perfect_Glacial_Rune@2","install.Perfect_Storm_Rune@1","exalt.A"],["artificer.open","install.Perfect_Storm_Rune@1","exalt.A","install.Perfect_Glacial_Rune@2"],["artificer.open","install.Perfect_Storm_Rune@1","install.Perfect_Glacial_Rune@2","exalt.A"],["exalt.A","artificer.open","install.Perfect_Glacial_Rune@2","install.Perfect_Storm_Rune@1"],["exalt.A","artificer.open","install.Perfect_Storm_Rune@1","install.Perfect_Glacial_Rune@2"],["exalt.A","install.Perfect_Storm_Rune@1","artificer.open","install.Perfect_Glacial_Rune@2"],["install.Perfect_Storm_Rune@1","artificer.open","exalt.A","install.Perfect_Glacial_Rune@2"],["install.Perfect_Storm_Rune@1","artificer.open","install.Perfect_Glacial_Rune@2","exalt.A"],["install.Perfect_Storm_Rune@1","exalt.A","artificer.open","install.Perfect_Glacial_Rune@2"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `9685ced97c74`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Wolfskin Mantle","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[],"openSockets":1},"targets":{"prefix":[{"ref":"A","effect":"+(54—56) to Spirit","tier":2}],"suffix":[],"sockets":{"1":"Perfect_Storm_Rune","2":"Perfect_Glacial_Rune"}},"mustRemove":[],"materials":{"ARTIFICER":1,"INSTALL":2,"EXALTED":1},"rules":"Artificer opens one socket. Install requires its numbered socket already open. Exalted adds A on its successful roll. Preserve the installed runes.","menu":["artificer.open","install.Perfect_Storm_Rune@1","install.Perfect_Glacial_Rune@2","exalt.A"]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"artificer.open\",\"install.Perfect_Storm_Rune@1\",\"install.Perfect_Glacial_Rune@2\",\"exalt.A\"],[\"artificer.open\",\"install.Perfect_Glacial_Rune@2\",\"install.Perfect_Storm_Rune@1\",\"exalt.A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["artificer.open","install.Perfect_Glacial_Rune@2","install.Perfect_Storm_Rune@1","exalt.A"],["artificer.open","install.Perfect_Storm_Rune@1","install.Perfect_Glacial_Rune@2","exalt.A"]],"firstGoal":[["artificer.open","install.Perfect_Glacial_Rune@2","install.Perfect_Storm_Rune@1","exalt.A"],["artificer.open","install.Perfect_Storm_Rune@1","install.Perfect_Glacial_Rune@2","exalt.A"]],"missingFull":10,"rejections":[]}
```

## 호출 41 · Dueling Wand · pool-1 · initial

사전 기대 응답에서 인정하는 실행열 3개.

```json
[["exalt.B","install.Thruds_Might@1","exalt.A"],["install.Thruds_Might@1","exalt.A","exalt.B"],["install.Thruds_Might@1","exalt.B","exalt.A"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `b172e0aab556`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Dueling Wand","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[],"openSockets":1},"targets":{"prefix":[{"ref":"B","effect":"Gain (13—15)% of Damage as Extra Cold Damage","tier":6}],"suffix":[{"ref":"A","effect":"(15—20)% increased Explicit Lightning Modifier magnitudes","tier":0}],"sockets":{"1":"Thruds_Might"}},"mustRemove":[],"materials":{"EXALTED":2,"INSTALL":1},"rules":"The existing socket is open and empty. A belongs exclusively to the destruction pool enabled by Thruds_Might. Exalted can draw A only while that augment is installed. B is an ordinary modifier. The installed augment stays socketed.","menu":["install.Thruds_Might@1","exalt.A","exalt.B"]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"install.Thruds_Might@1\",\"exalt.A\"],[\"exalt.B\",\"install.Thruds_Might@1\",\"exalt.A\"],[\"install.Thruds_Might@1\",\"exalt.B\",\"exalt.A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["exalt.B","install.Thruds_Might@1","exalt.A"],["install.Thruds_Might@1","exalt.B","exalt.A"]],"firstGoal":[["exalt.B","install.Thruds_Might@1","exalt.A"],["install.Thruds_Might@1","exalt.B","exalt.A"]],"missingFull":1,"rejections":[[1,"unfinished goal"]]}
```

## 호출 42 · Abyssal Cuirass · pool-2 · initial

사전 기대 응답에서 인정하는 실행열 3개.

```json
[["exalt.B","install.Medveds_Tending@1","exalt.A"],["install.Medveds_Tending@1","exalt.A","exalt.B"],["install.Medveds_Tending@1","exalt.B","exalt.A"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `6d93a6cc8d14`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Abyssal Cuirass","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[],"openSockets":1},"targets":{"prefix":[{"ref":"A","effect":"(1—20)% increased maximum Life","tier":0}],"suffix":[{"ref":"B","effect":"+(21—25)% to Fire Resistance","tier":5}],"sockets":{"1":"Medveds_Tending"}},"mustRemove":[],"materials":{"EXALTED":2,"INSTALL":1},"rules":"The existing socket is open and empty. A belongs exclusively to the soul pool enabled by Medveds_Tending. Exalted can draw A only while that augment is installed. B is an ordinary modifier. The installed augment stays socketed.","menu":["install.Medveds_Tending@1","exalt.A","exalt.B"]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"install.Medveds_Tending@1\",\"exalt.A\",\"exalt.B\"],[\"install.Medveds_Tending@1\",\"exalt.B\",\"exalt.A\"],[\"exalt.B\",\"install.Medveds_Tending@1\",\"exalt.A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["exalt.B","install.Medveds_Tending@1","exalt.A"],["install.Medveds_Tending@1","exalt.A","exalt.B"],["install.Medveds_Tending@1","exalt.B","exalt.A"]],"firstGoal":[["exalt.B","install.Medveds_Tending@1","exalt.A"],["install.Medveds_Tending@1","exalt.A","exalt.B"],["install.Medveds_Tending@1","exalt.B","exalt.A"]],"missingFull":0,"rejections":[]}
```

## 호출 43 · Galvanic Wand · pool-4 · initial

사전 기대 응답에서 인정하는 실행열 3개.

```json
[["exalt.B","install.Thruds_Might@1","exalt.A"],["install.Thruds_Might@1","exalt.A","exalt.B"],["install.Thruds_Might@1","exalt.B","exalt.A"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `b172e0aab556`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Galvanic Wand","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[],"openSockets":1},"targets":{"prefix":[{"ref":"B","effect":"(15—19)% increased Spell Damage+(17—20) to maximum Mana","tier":7}],"suffix":[{"ref":"A","effect":"(15—20)% increased Explicit Cold Modifier magnitudes","tier":0}],"sockets":{"1":"Thruds_Might"}},"mustRemove":[],"materials":{"EXALTED":2,"INSTALL":1},"rules":"The existing socket is open and empty. A belongs exclusively to the destruction pool enabled by Thruds_Might. Exalted can draw A only while that augment is installed. B is an ordinary modifier. The installed augment stays socketed.","menu":["install.Thruds_Might@1","exalt.A","exalt.B"]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"install.Thruds_Might@1\",\"exalt.A\",\"exalt.B\"],[\"install.Thruds_Might@1\",\"exalt.B\",\"exalt.A\"],[\"exalt.B\",\"install.Thruds_Might@1\",\"exalt.A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["exalt.B","install.Thruds_Might@1","exalt.A"],["install.Thruds_Might@1","exalt.A","exalt.B"],["install.Thruds_Might@1","exalt.B","exalt.A"]],"firstGoal":[["exalt.B","install.Thruds_Might@1","exalt.A"],["install.Thruds_Might@1","exalt.A","exalt.B"],["install.Thruds_Might@1","exalt.B","exalt.A"]],"missingFull":0,"rejections":[]}
```

## 호출 44 · Rusted Cuirass · pool-5 · initial

사전 기대 응답에서 인정하는 실행열 3개.

```json
[["exalt.B","install.Medveds_Tending@1","exalt.A"],["install.Medveds_Tending@1","exalt.A","exalt.B"],["install.Medveds_Tending@1","exalt.B","exalt.A"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `6d93a6cc8d14`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Rusted Cuirass","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[],"openSockets":1},"targets":{"prefix":[{"ref":"A","effect":"(6—52)% increased Armour\n+(1—24) to Spirit","tier":0}],"suffix":[{"ref":"B","effect":"+(4—7)% to Chaos Resistance","tier":6}],"sockets":{"1":"Medveds_Tending"}},"mustRemove":[],"materials":{"EXALTED":2,"INSTALL":1},"rules":"The existing socket is open and empty. A belongs exclusively to the soul pool enabled by Medveds_Tending. Exalted can draw A only while that augment is installed. B is an ordinary modifier. The installed augment stays socketed.","menu":["install.Medveds_Tending@1","exalt.A","exalt.B"]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"install.Medveds_Tending@1\",\"exalt.A\",\"exalt.B\"],[\"install.Medveds_Tending@1\",\"exalt.B\",\"exalt.A\"],[\"exalt.B\",\"install.Medveds_Tending@1\",\"exalt.A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["exalt.B","install.Medveds_Tending@1","exalt.A"],["install.Medveds_Tending@1","exalt.A","exalt.B"],["install.Medveds_Tending@1","exalt.B","exalt.A"]],"firstGoal":[["exalt.B","install.Medveds_Tending@1","exalt.A"],["install.Medveds_Tending@1","exalt.A","exalt.B"],["install.Medveds_Tending@1","exalt.B","exalt.A"]],"missingFull":0,"rejections":[]}
```

## 호출 45 · Acrid Wand · pool-6 · initial

사전 기대 응답에서 인정하는 실행열 3개.

```json
[["exalt.B","install.Thruds_Might@1","exalt.A"],["install.Thruds_Might@1","exalt.A","exalt.B"],["install.Thruds_Might@1","exalt.B","exalt.A"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `b172e0aab556`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Acrid Wand","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[],"openSockets":1},"targets":{"prefix":[{"ref":"B","effect":"(65—74)% increased Spell Physical Damage","tier":4}],"suffix":[{"ref":"A","effect":"(15—20)% increased Explicit Fire Modifier magnitudes","tier":0}],"sockets":{"1":"Thruds_Might"}},"mustRemove":[],"materials":{"EXALTED":2,"INSTALL":1},"rules":"The existing socket is open and empty. A belongs exclusively to the destruction pool enabled by Thruds_Might. Exalted can draw A only while that augment is installed. B is an ordinary modifier. The installed augment stays socketed.","menu":["install.Thruds_Might@1","exalt.A","exalt.B"]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"install.Thruds_Might@1\",\"exalt.A\"],[\"install.Thruds_Might@1\",\"exalt.B\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[],"firstGoal":[],"missingFull":3,"rejections":[[1,"unfinished goal"],[2,"unfinished goal"]]}
```

## 호출 46 · Acrid Wand · pool-6 · feedback

사전 기대는 호출 45의 3개 실행열과 같다.

전송 문안 `178065f1b0ec`, 응답 스키마 `b172e0aab556`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Acrid Wand","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[],"openSockets":1},"targets":{"prefix":[{"ref":"B","effect":"(65—74)% increased Spell Physical Damage","tier":4}],"suffix":[{"ref":"A","effect":"(15—20)% increased Explicit Fire Modifier magnitudes","tier":0}],"sockets":{"1":"Thruds_Might"}},"mustRemove":[],"materials":{"EXALTED":2,"INSTALL":1},"rules":"The existing socket is open and empty. A belongs exclusively to the destruction pool enabled by Thruds_Might. Exalted can draw A only while that augment is installed. B is an ordinary modifier. The installed augment stays socketed.","menu":["install.Thruds_Might@1","exalt.A","exalt.B"],"lastFailure":[{"route":["install.Thruds_Might@1","exalt.A"],"reason":"unfinished goal"},{"route":["install.Thruds_Might@1","exalt.B"],"reason":"unfinished goal"}]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"install.Thruds_Might@1\",\"exalt.A\",\"exalt.B\"],[\"install.Thruds_Might@1\",\"exalt.B\",\"exalt.A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["install.Thruds_Might@1","exalt.A","exalt.B"],["install.Thruds_Might@1","exalt.B","exalt.A"]],"firstGoal":[["install.Thruds_Might@1","exalt.A","exalt.B"],["install.Thruds_Might@1","exalt.B","exalt.A"]],"missingFull":1,"rejections":[]}
```

## 호출 47 · Primordial Wand · pool-7 · initial

사전 기대 응답에서 인정하는 실행열 3개.

```json
[["exalt.B","install.Thruds_Might@1","exalt.A"],["install.Thruds_Might@1","exalt.A","exalt.B"],["install.Thruds_Might@1","exalt.B","exalt.A"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `b172e0aab556`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Primordial Wand","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[],"openSockets":1},"targets":{"prefix":[{"ref":"B","effect":"+(125—149) to maximum Mana","tier":2}],"suffix":[{"ref":"A","effect":"(15—20)% increased Explicit Chaos Modifier magnitudes","tier":0}],"sockets":{"1":"Thruds_Might"}},"mustRemove":[],"materials":{"EXALTED":2,"INSTALL":1},"rules":"The existing socket is open and empty. A belongs exclusively to the destruction pool enabled by Thruds_Might. Exalted can draw A only while that augment is installed. B is an ordinary modifier. The installed augment stays socketed.","menu":["install.Thruds_Might@1","exalt.A","exalt.B"]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"install.Thruds_Might@1\",\"exalt.A\",\"exalt.B\"],[\"install.Thruds_Might@1\",\"exalt.B\",\"exalt.A\"],[\"exalt.B\",\"install.Thruds_Might@1\",\"exalt.A\"],[\"exalt.B\",\"exalt.A\",\"install.Thruds_Might@1\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["exalt.B","install.Thruds_Might@1","exalt.A"],["install.Thruds_Might@1","exalt.A","exalt.B"],["install.Thruds_Might@1","exalt.B","exalt.A"]],"firstGoal":[["exalt.B","install.Thruds_Might@1","exalt.A"],["install.Thruds_Might@1","exalt.A","exalt.B"],["install.Thruds_Might@1","exalt.B","exalt.A"]],"missingFull":0,"rejections":[[4,"2. exclusive pool inactive"]]}
```

## 호출 48 · Offering Wand · pool-8 · initial

사전 기대 응답에서 인정하는 실행열 3개.

```json
[["exalt.B","install.Thruds_Might@1","exalt.A"],["install.Thruds_Might@1","exalt.A","exalt.B"],["install.Thruds_Might@1","exalt.B","exalt.A"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `b172e0aab556`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Offering Wand","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[],"openSockets":1},"targets":{"prefix":[{"ref":"B","effect":"(35—39)% increased Spell Damage+(34—37) to maximum Mana","tier":3}],"suffix":[{"ref":"A","effect":"(15—20)% increased Explicit Cold Modifier magnitudes","tier":0}],"sockets":{"1":"Thruds_Might"}},"mustRemove":[],"materials":{"EXALTED":2,"INSTALL":1},"rules":"The existing socket is open and empty. A belongs exclusively to the destruction pool enabled by Thruds_Might. Exalted can draw A only while that augment is installed. B is an ordinary modifier. The installed augment stays socketed.","menu":["install.Thruds_Might@1","exalt.A","exalt.B"]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"install.Thruds_Might@1\",\"exalt.A\"],[\"exalt.B\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[],"firstGoal":[],"missingFull":3,"rejections":[[1,"unfinished goal"],[2,"unfinished goal"]]}
```

## 호출 49 · Offering Wand · pool-8 · feedback

사전 기대는 호출 48의 3개 실행열과 같다.

전송 문안 `178065f1b0ec`, 응답 스키마 `b172e0aab556`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Offering Wand","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[],"openSockets":1},"targets":{"prefix":[{"ref":"B","effect":"(35—39)% increased Spell Damage+(34—37) to maximum Mana","tier":3}],"suffix":[{"ref":"A","effect":"(15—20)% increased Explicit Cold Modifier magnitudes","tier":0}],"sockets":{"1":"Thruds_Might"}},"mustRemove":[],"materials":{"EXALTED":2,"INSTALL":1},"rules":"The existing socket is open and empty. A belongs exclusively to the destruction pool enabled by Thruds_Might. Exalted can draw A only while that augment is installed. B is an ordinary modifier. The installed augment stays socketed.","menu":["install.Thruds_Might@1","exalt.A","exalt.B"],"lastFailure":[{"route":["install.Thruds_Might@1","exalt.A"],"reason":"unfinished goal"},{"route":["exalt.B"],"reason":"unfinished goal"}]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"install.Thruds_Might@1\",\"exalt.A\",\"exalt.B\"],[\"install.Thruds_Might@1\",\"exalt.B\",\"exalt.A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["install.Thruds_Might@1","exalt.A","exalt.B"],["install.Thruds_Might@1","exalt.B","exalt.A"]],"firstGoal":[["install.Thruds_Might@1","exalt.A","exalt.B"],["install.Thruds_Might@1","exalt.B","exalt.A"]],"missingFull":1,"rejections":[]}
```

## 호출 50 · Shrouded Mail · pool-9 · initial

사전 기대 응답에서 인정하는 실행열 3개.

```json
[["exalt.B","install.Medveds_Tending@1","exalt.A"],["install.Medveds_Tending@1","exalt.A","exalt.B"],["install.Medveds_Tending@1","exalt.B","exalt.A"]]
```

전송 문안 `e6eb180b5a1b`, 응답 스키마 `6d93a6cc8d14`. 사용자 메시지 JSON은 아래와 같다.

```json
{"base":"Shrouded Mail","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[],"openSockets":1},"targets":{"prefix":[{"ref":"B","effect":"+(33—41) to Armour\n+(30—39) to Evasion Rating\n(33—38)% increased Armour and Evasion","tier":2}],"suffix":[{"ref":"A","effect":"+(3—31)% to Lightning and Chaos Resistances","tier":0}],"sockets":{"1":"Medveds_Tending"}},"mustRemove":[],"materials":{"EXALTED":2,"INSTALL":1},"rules":"The existing socket is open and empty. A belongs exclusively to the soul pool enabled by Medveds_Tending. Exalted can draw A only while that augment is installed. B is an ordinary modifier. The installed augment stays socketed.","menu":["install.Medveds_Tending@1","exalt.A","exalt.B"]}
```

실제 응답 전체를 원문 문자열로 보존했다.

```json
{"raw":"{\"routes\":[[\"install.Medveds_Tending@1\",\"exalt.A\",\"exalt.B\"],[\"install.Medveds_Tending@1\",\"exalt.B\",\"exalt.A\"],[\"exalt.B\",\"install.Medveds_Tending@1\",\"exalt.A\"]]}"}
```

검증 결과. `full`은 원문 완주, `firstGoal`은 최초 목표 달성에서 종료한 경로까지 포함한다. `rejections`는 첫 실패 또는 완성 뒤 추가 행동의 사유다.

```json
{"full":[["exalt.B","install.Medveds_Tending@1","exalt.A"],["install.Medveds_Tending@1","exalt.A","exalt.B"],["install.Medveds_Tending@1","exalt.B","exalt.A"]],"firstGoal":[["exalt.B","install.Medveds_Tending@1","exalt.A"],["install.Medveds_Tending@1","exalt.A","exalt.B"],["install.Medveds_Tending@1","exalt.B","exalt.A"]],"missingFull":0,"rejections":[]}
```

## 호출에서 사용한 스키마

### f05d1ea0d1ef

```json
{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["blessedDivine.I","divine.A","exalt.B"]}}}},"required":["routes"],"additionalProperties":false}
```

### 9cd8a9e6c35f

```json
{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","exalt.X","SOVEREIGN_ALLOY.X>A","SOVEREIGN_ALLOY.B>A"]}}}},"required":["routes"],"additionalProperties":false}
```

### e18e65a3add5

```json
{"type":"object","properties":{"begin_1":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","exalt.X","SOVEREIGN_ALLOY.X>A","SOVEREIGN_ALLOY.B>A"]}}},"begin_2":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","exalt.X","SOVEREIGN_ALLOY.X>A","SOVEREIGN_ALLOY.B>A"]}}},"begin_3":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","exalt.X","SOVEREIGN_ALLOY.X>A","SOVEREIGN_ALLOY.B>A"]}}},"begin_4":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","exalt.X","SOVEREIGN_ALLOY.X>A","SOVEREIGN_ALLOY.B>A"]}}}},"required":["begin_1","begin_2","begin_3","begin_4"],"additionalProperties":false}
```

### c16a3a9fe325

```json
{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["whittle.X>A","sideWhittle.X>A","whittle.X>B","sideWhittle.X>B","whittle.Y>A","sideWhittle.Y>A","whittle.Y>B","sideWhittle.Y>B"]}}}},"required":["routes"],"additionalProperties":false}
```

### 13344b4eacc6

```json
{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["artificer.open","install.Perfect_Glacial_Rune@1","install.Perfect_Desert_Rune@2","exalt.A"]}}}},"required":["routes"],"additionalProperties":false}
```

### 7be1c9e88e54

```json
{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","exalt.X","TRANSCENDENT_ALLOY.X>A","TRANSCENDENT_ALLOY.B>A"]}}}},"required":["routes"],"additionalProperties":false}
```

### 651d0d0f2613

```json
{"type":"object","properties":{"begin_1":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["whittle.X>A","sideWhittle.X>A","whittle.X>B","sideWhittle.X>B","whittle.Y>A","sideWhittle.Y>A","whittle.Y>B","sideWhittle.Y>B"]}}},"begin_2":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["whittle.X>A","sideWhittle.X>A","whittle.X>B","sideWhittle.X>B","whittle.Y>A","sideWhittle.Y>A","whittle.Y>B","sideWhittle.Y>B"]}}},"begin_3":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["whittle.X>A","sideWhittle.X>A","whittle.X>B","sideWhittle.X>B","whittle.Y>A","sideWhittle.Y>A","whittle.Y>B","sideWhittle.Y>B"]}}},"begin_4":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["whittle.X>A","sideWhittle.X>A","whittle.X>B","sideWhittle.X>B","whittle.Y>A","sideWhittle.Y>A","whittle.Y>B","sideWhittle.Y>B"]}}},"begin_5":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["whittle.X>A","sideWhittle.X>A","whittle.X>B","sideWhittle.X>B","whittle.Y>A","sideWhittle.Y>A","whittle.Y>B","sideWhittle.Y>B"]}}},"begin_6":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["whittle.X>A","sideWhittle.X>A","whittle.X>B","sideWhittle.X>B","whittle.Y>A","sideWhittle.Y>A","whittle.Y>B","sideWhittle.Y>B"]}}},"begin_7":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["whittle.X>A","sideWhittle.X>A","whittle.X>B","sideWhittle.X>B","whittle.Y>A","sideWhittle.Y>A","whittle.Y>B","sideWhittle.Y>B"]}}},"begin_8":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["whittle.X>A","sideWhittle.X>A","whittle.X>B","sideWhittle.X>B","whittle.Y>A","sideWhittle.Y>A","whittle.Y>B","sideWhittle.Y>B"]}}}},"required":["begin_1","begin_2","begin_3","begin_4","begin_5","begin_6","begin_7","begin_8"],"additionalProperties":false}
```

### 18c4e5caafce

```json
{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["artificer.open","install.Perfect_Desert_Rune@1","exalt.A"]}}}},"required":["routes"],"additionalProperties":false}
```

### bc10f5f17fec

```json
{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["blessedDivine.I","divine.A","exalt.B","exalt.A"]}}}},"required":["routes"],"additionalProperties":false}
```

### d65b5b3a6684

```json
{"type":"object","properties":{"begin_1":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["blessedDivine.I","divine.A","exalt.B","exalt.A"]}}},"begin_2":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["blessedDivine.I","divine.A","exalt.B","exalt.A"]}}},"begin_3":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["blessedDivine.I","divine.A","exalt.B","exalt.A"]}}},"begin_4":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["blessedDivine.I","divine.A","exalt.B","exalt.A"]}}}},"required":["begin_1","begin_2","begin_3","begin_4"],"additionalProperties":false}
```

### 5b6ae221958e

```json
{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","exalt.X","RUNIC_ALLOY.X>A","RUNIC_ALLOY.B>A"]}}}},"required":["routes"],"additionalProperties":false}
```

### 7dea0d7d4b59

```json
{"type":"object","properties":{"begin_1":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","exalt.X","RUNIC_ALLOY.X>A","RUNIC_ALLOY.B>A"]}}},"begin_2":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","exalt.X","RUNIC_ALLOY.X>A","RUNIC_ALLOY.B>A"]}}},"begin_3":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","exalt.X","RUNIC_ALLOY.X>A","RUNIC_ALLOY.B>A"]}}},"begin_4":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","exalt.X","RUNIC_ALLOY.X>A","RUNIC_ALLOY.B>A"]}}}},"required":["begin_1","begin_2","begin_3","begin_4"],"additionalProperties":false}
```

### 932d6845f04a

```json
{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["artificer.open","install.Perfect_Glacial_Rune@1","exalt.A"]}}}},"required":["routes"],"additionalProperties":false}
```

### dac81e42e7ae

```json
{"type":"object","properties":{"begin_1":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["blessedDivine.I","divine.A","exalt.B"]}}},"begin_2":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["blessedDivine.I","divine.A","exalt.B"]}}},"begin_3":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["blessedDivine.I","divine.A","exalt.B"]}}}},"required":["begin_1","begin_2","begin_3"],"additionalProperties":false}
```

### 336b071008b6

```json
{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","exalt.X","SWIFT_ALLOY.X>A","SWIFT_ALLOY.B>A"]}}}},"required":["routes"],"additionalProperties":false}
```

### 2f281f581ddf

```json
{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["artificer.open","install.Perfect_Glacial_Rune@1","install.Perfect_Storm_Rune@2","exalt.A"]}}}},"required":["routes"],"additionalProperties":false}
```

### 9685ced97c74

```json
{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["artificer.open","install.Perfect_Storm_Rune@1","install.Perfect_Glacial_Rune@2","exalt.A"]}}}},"required":["routes"],"additionalProperties":false}
```

### e53d4e331c5e

```json
{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["artificer.open","install.Perfect_Storm_Rune@1","exalt.A"]}}}},"required":["routes"],"additionalProperties":false}
```

### b172e0aab556

```json
{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["install.Thruds_Might@1","exalt.A","exalt.B"]}}}},"required":["routes"],"additionalProperties":false}
```

### 6d93a6cc8d14

```json
{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["install.Medveds_Tending@1","exalt.A","exalt.B"]}}}},"required":["routes"],"additionalProperties":false}
```
