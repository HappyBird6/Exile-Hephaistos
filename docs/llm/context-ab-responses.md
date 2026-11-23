# 조건 정보 A/B 실험 원문 100회

[요약과 판단](context-ab-100.md) · [호출 전에 고정한 카드·기대 및 회수 근거](context-ab-cards.json)

모델 `gpt-4.1-mini-2025-04-14`, temperature 0, max_completion_tokens 3200, store false. 응답은 strict JSON Schema `candidate_routes`다. 모든 호출은 같은 시스템 문안을 사용했다.

```text
You are a Path of Exile 2 crafting route planner. Use the supplied starting item, stock, goal filters and available successful outcome tokens. A candidate is an ordered list of actions with their targets, starting from the original item. Enumerate distinct feasible first actions, acquisition orders and material choices, including low-probability ones. Return complete candidates in routes. Stop each route at the first state matching all active groups. Omit state-preserving moves. If uncertain about legality, include the candidate for backend replay. An empty list means no candidate found. The backend validates states and computes odds and cost.
```

## 호출 1 · if-1 · Rattling Sceptre · 간략

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["chaos.A>B"],["divine.high","annul.A","exalt.B"],["divine.high","chaos.A>B"],["divine.high","exalt.B"],["exalt.B","annul.A"],["exalt.B","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Rattling Sceptre","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":28,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"B","stat":"Maximum Mana","unit":"flat","range":[10,14]}],"suffix":[{"ref":"A","stat":"Strength","unit":"flat","range":[28,30]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false},{"op":"IF","rows":[{"ref":"A","min":30,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1,"DIVINE":1},"menu":["exalt.B","annul.A","chaos.A>B","divine.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","annul.A","chaos.A>B","divine.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B\",\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":470,"completion_tokens":13,"total_tokens":483,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.B","divine.high"]],"acceptedIncludingGoalPrefix":[["exalt.B","divine.high"]],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"full","step":2}]}
```

## 호출 2 · if-1 · Rattling Sceptre · 보강

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["chaos.A>B"],["divine.high","annul.A","exalt.B"],["divine.high","chaos.A>B"],["divine.high","exalt.B"],["exalt.B","annul.A"],["exalt.B","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Rattling Sceptre","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":28,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"B","stat":"Maximum Mana","unit":"flat","range":[10,14]}],"suffix":[{"ref":"A","stat":"Strength","unit":"flat","range":[28,30]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false},{"op":"IF","rows":[{"ref":"A","min":30,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1,"DIVINE":1},"menu":["exalt.B","annul.A","chaos.A>B","divine.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"IF: an absent row passes. A present row must satisfy its range. Every active group must pass.","modifierFacts":{"A":{"family":["Strength"],"tier":2,"requiresItemLevel":66,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[28,30]},"B":{"family":["IncreasedMana"],"tier":11,"requiresItemLevel":1,"acquireVia":["exalt.B","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[10,14]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":10},"annul.A":{"consume":"ANNULMENT","remove":"A"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":10},"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","annul.A","chaos.A>B","divine.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B\"],[\"chaos.A>B\"],[\"annul.A\",\"exalt.B\"],[\"annul.A\",\"chaos.A>B\"],[\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":687,"completion_tokens":39,"total_tokens":726,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["annul.A","exalt.B"],["chaos.A>B"]],"acceptedIncludingGoalPrefix":[["annul.A","exalt.B"],["chaos.A>B"]],"missingIncludingGoalPrefix":5,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"full","step":1},{"i":3,"result":"full","step":2},{"i":4,"result":"invalid","step":2},{"i":5,"result":"unfinished","step":1}]}
```

## 호출 3 · not-1 · Fungal Talisman · 보강

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["chaos.A>B"],["divine.low","annul.A","exalt.B"],["divine.low","chaos.A>B"],["divine.low","exalt.B"],["exalt.B","annul.A"],["exalt.B","divine.low"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Fungal Talisman","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":16,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Intelligence","unit":"flat","range":[13,16]},{"ref":"B","stat":"Mana gained per enemy killed","unit":"flat","range":[4,5]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false},{"op":"NOT","rows":[{"ref":"A","min":16,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1,"DIVINE":1},"menu":["exalt.B","annul.A","chaos.A>B","divine.low"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"NOT matches if none of its enabled rows are both present and in range. A present value outside the forbidden interval can remain.","modifierFacts":{"A":{"family":["Intelligence"],"tier":6,"requiresItemLevel":22,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[13,16]},"B":{"family":["ManaGainedFromEnemyDeath"],"tier":7,"requiresItemLevel":12,"acquireVia":["exalt.B","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[4,5]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":4},"annul.A":{"consume":"ANNULMENT","remove":"A"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":4},"divine.low":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"low"}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","annul.A","chaos.A>B","divine.low"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B\"],[\"chaos.A>B\"],[\"annul.A\"]]}","finish_reason":"stop","usage":{"prompt_tokens":698,"completion_tokens":20,"total_tokens":718,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["chaos.A>B"]],"acceptedIncludingGoalPrefix":[["chaos.A>B"]],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"full","step":1},{"i":3,"result":"unfinished","step":1}]}
```

## 호출 4 · not-1 · Fungal Talisman · 간략

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["chaos.A>B"],["divine.low","annul.A","exalt.B"],["divine.low","chaos.A>B"],["divine.low","exalt.B"],["exalt.B","annul.A"],["exalt.B","divine.low"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Fungal Talisman","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":16,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Intelligence","unit":"flat","range":[13,16]},{"ref":"B","stat":"Mana gained per enemy killed","unit":"flat","range":[4,5]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false},{"op":"NOT","rows":[{"ref":"A","min":16,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1,"DIVINE":1},"menu":["exalt.B","annul.A","chaos.A>B","divine.low"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","annul.A","chaos.A>B","divine.low"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"annul.A\",\"chaos.A>B\",\"exalt.B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":473,"completion_tokens":18,"total_tokens":491,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":7,"assessment":[{"i":1,"result":"invalid","step":2}]}
```

## 호출 5 · count-1 · Broadhead Quiver · 간략

사전 기대 실행열 6개.

```json
[["annul.A","exalt.C"],["annul.B","exalt.C"],["chaos.A>C"],["chaos.B>C"],["exalt.C","annul.A"],["exalt.C","annul.B"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Broadhead Quiver","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"A","sourceValue":33,"fractured":false}],"suffix":[{"ref":"B","sourceValue":9,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"A","stat":"Accuracy Rating","unit":"flat","range":[33,60]}],"suffix":[{"ref":"B","stat":"Dexterity","unit":"flat","range":[9,12]},{"ref":"C","stat":"Mana gained per enemy killed","unit":"flat","range":[6,9]}]},"goalGroups":[{"op":"COUNT","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false},{"ref":"C","min":null,"max":null,"weight":null,"disabled":false}],"min":2,"max":2,"disabled":false},{"op":"AND","rows":[{"ref":"C","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1},"menu":["exalt.C","annul.A","annul.B","chaos.A>C","chaos.B>C"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.C","annul.A","annul.B","chaos.A>C","chaos.B>C"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"chaos.A>C\"]]}","finish_reason":"stop","usage":{"prompt_tokens":552,"completion_tokens":10,"total_tokens":562,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["chaos.A>C"]],"acceptedIncludingGoalPrefix":[["chaos.A>C"]],"missingIncludingGoalPrefix":5,"assessment":[{"i":1,"result":"full","step":1}]}
```

## 호출 6 · count-1 · Broadhead Quiver · 보강

사전 기대 실행열 6개.

```json
[["annul.A","exalt.C"],["annul.B","exalt.C"],["chaos.A>C"],["chaos.B>C"],["exalt.C","annul.A"],["exalt.C","annul.B"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Broadhead Quiver","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"A","sourceValue":33,"fractured":false}],"suffix":[{"ref":"B","sourceValue":9,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"A","stat":"Accuracy Rating","unit":"flat","range":[33,60]}],"suffix":[{"ref":"B","stat":"Dexterity","unit":"flat","range":[9,12]},{"ref":"C","stat":"Mana gained per enemy killed","unit":"flat","range":[6,9]}]},"goalGroups":[{"op":"COUNT","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false},{"ref":"C","min":null,"max":null,"weight":null,"disabled":false}],"min":2,"max":2,"disabled":false},{"op":"AND","rows":[{"ref":"C","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1},"menu":["exalt.C","annul.A","annul.B","chaos.A>C","chaos.B>C"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"COUNT counts enabled rows that are present and in range. Both minimum and maximum are inclusive; compare the count again after each removal or acquisition.","modifierFacts":{"A":{"family":["IncreasedAccuracy"],"tier":8,"requiresItemLevel":11,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[33,60]},"B":{"family":["Dexterity"],"tier":7,"requiresItemLevel":11,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[9,12]},"C":{"family":["ManaGainedFromEnemyDeath"],"tier":4,"requiresItemLevel":23,"acquireVia":["exalt.C","chaos.A>C","chaos.B>C"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[6,9]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.C":{"consume":"EXALTED","gain":"C","sourceValue":6},"annul.A":{"consume":"ANNULMENT","remove":"A"},"annul.B":{"consume":"ANNULMENT","remove":"B"},"chaos.A>C":{"consume":"CHAOS","remove":"A","gain":"C","sourceValue":6},"chaos.B>C":{"consume":"CHAOS","remove":"B","gain":"C","sourceValue":6}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.C","annul.A","annul.B","chaos.A>C","chaos.B>C"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.C\"],[\"chaos.A>C\"],[\"chaos.B>C\"],[\"annul.A\",\"exalt.C\"],[\"annul.B\",\"exalt.C\"],[\"annul.A\",\"chaos.B>C\"],[\"annul.B\",\"chaos.A>C\"]]}","finish_reason":"stop","usage":{"prompt_tokens":850,"completion_tokens":59,"total_tokens":909,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["annul.A","exalt.C"],["annul.B","exalt.C"],["chaos.A>C"],["chaos.B>C"]],"acceptedIncludingGoalPrefix":[["annul.A","exalt.C"],["annul.B","exalt.C"],["chaos.A>C"],["chaos.B>C"]],"missingIncludingGoalPrefix":2,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"full","step":1},{"i":3,"result":"full","step":1},{"i":4,"result":"full","step":2},{"i":5,"result":"full","step":2},{"i":6,"result":"unfinished","step":2},{"i":7,"result":"unfinished","step":2}]}
```

## 호출 7 · w1-1 · Stoic Sceptre · 보강

사전 기대 실행열 6개.

```json
[["annul.A","exalt.B.high"],["chaos.A>B","divine.high"],["divine.high","annul.A","exalt.B.high"],["divine.high","exalt.B.high"],["exalt.B.high","annul.A"],["exalt.B.high","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Stoic Sceptre","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":28,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"B","stat":"Maximum Mana","unit":"flat","range":[55,64]}],"suffix":[{"ref":"A","stat":"Strength","unit":"flat","range":[28,30]}]},"goalGroups":[{"op":"WEIGHTED_V1","rows":[{"ref":"A","min":30,"max":null,"weight":1,"disabled":false},{"ref":"B","min":null,"max":null,"weight":0.5,"disabled":false}],"min":32.0,"max":null,"disabled":false},{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"DIVINE":1,"CHAOS":1},"menu":["exalt.B.high","annul.A","divine.high","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"WEIGHTED_V1: absent rows are optional. Every present enabled row must pass its own range, and the sum of present value times weight must lie in the group range. Weights are score coefficients.","modifierFacts":{"A":{"family":["Strength"],"tier":2,"requiresItemLevel":66,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[28,30]},"B":{"family":["IncreasedMana"],"tier":7,"requiresItemLevel":33,"acquireVia":["exalt.B.high","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[55,64]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.B.high":{"consume":"EXALTED","gain":"B","sourceValue":64},"annul.A":{"consume":"ANNULMENT","remove":"A"},"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":55}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B.high","annul.A","divine.high","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B.high\"],[\"chaos.A>B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":740,"completion_tokens":16,"total_tokens":756,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"unfinished","step":1}]}
```

## 호출 8 · w1-1 · Stoic Sceptre · 간략

사전 기대 실행열 6개.

```json
[["annul.A","exalt.B.high"],["chaos.A>B","divine.high"],["divine.high","annul.A","exalt.B.high"],["divine.high","exalt.B.high"],["exalt.B.high","annul.A"],["exalt.B.high","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Stoic Sceptre","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":28,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"B","stat":"Maximum Mana","unit":"flat","range":[55,64]}],"suffix":[{"ref":"A","stat":"Strength","unit":"flat","range":[28,30]}]},"goalGroups":[{"op":"WEIGHTED_V1","rows":[{"ref":"A","min":30,"max":null,"weight":1,"disabled":false},{"ref":"B","min":null,"max":null,"weight":0.5,"disabled":false}],"min":32.0,"max":null,"disabled":false},{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"DIVINE":1,"CHAOS":1},"menu":["exalt.B.high","annul.A","divine.high","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B.high","annul.A","divine.high","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B.high\"],[\"annul.A\",\"exalt.B.high\"],[\"chaos.A>B\",\"exalt.B.high\"],[\"divine.high\",\"exalt.B.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":499,"completion_tokens":41,"total_tokens":540,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["annul.A","exalt.B.high"],["divine.high","exalt.B.high"]],"acceptedIncludingGoalPrefix":[["annul.A","exalt.B.high"],["divine.high","exalt.B.high"]],"missingIncludingGoalPrefix":4,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"full","step":2},{"i":3,"result":"invalid","step":2},{"i":4,"result":"full","step":2}]}
```

## 호출 9 · w2-1 · Faithful Leggings · 간략

사전 기대 실행열 5개.

```json
[["annul.A","exalt.B.high"],["chaos.A>B","divine.high"],["divine.high","annul.A","exalt.B.high"],["divine.high","exalt.B.high"],["exalt.B.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Faithful Leggings","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":31,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Lightning Resistance","unit":"percent","range":[31,35]},{"ref":"B","stat":"Cold Resistance","unit":"percent","range":[16,20]}]},"goalGroups":[{"op":"WEIGHTED_V2","rows":[{"ref":"A","min":35,"max":null,"weight":1,"disabled":false},{"ref":"B","min":null,"max":null,"weight":0.5,"disabled":false}],"min":10.0,"max":null,"disabled":false},{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"DIVINE":1,"CHAOS":1},"menu":["exalt.B.high","annul.A","divine.high","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B.high","annul.A","divine.high","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"annul.A\",\"chaos.A>B\",\"exalt.B.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":499,"completion_tokens":19,"total_tokens":518,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":5,"assessment":[{"i":1,"result":"invalid","step":2}]}
```

## 호출 10 · w2-1 · Faithful Leggings · 보강

사전 기대 실행열 5개.

```json
[["annul.A","exalt.B.high"],["chaos.A>B","divine.high"],["divine.high","annul.A","exalt.B.high"],["divine.high","exalt.B.high"],["exalt.B.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Faithful Leggings","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":31,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Lightning Resistance","unit":"percent","range":[31,35]},{"ref":"B","stat":"Cold Resistance","unit":"percent","range":[16,20]}]},"goalGroups":[{"op":"WEIGHTED_V2","rows":[{"ref":"A","min":35,"max":null,"weight":1,"disabled":false},{"ref":"B","min":null,"max":null,"weight":0.5,"disabled":false}],"min":10.0,"max":null,"disabled":false},{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"DIVINE":1,"CHAOS":1},"menu":["exalt.B.high","annul.A","divine.high","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"WEIGHTED_V2: at least one enabled row must be present. Only present rows inside their own range contribute value times weight. A present out-of-range row adds zero and does not veto the group.","modifierFacts":{"A":{"family":["LightningResistance"],"tier":3,"requiresItemLevel":60,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[31,35]},"B":{"family":["ColdResistance"],"tier":6,"requiresItemLevel":26,"acquireVia":["exalt.B.high","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[16,20]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.B.high":{"consume":"EXALTED","gain":"B","sourceValue":20},"annul.A":{"consume":"ANNULMENT","remove":"A"},"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":16}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B.high","annul.A","divine.high","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B.high\"],[\"chaos.A>B\"],[\"annul.A\",\"exalt.B.high\"],[\"annul.A\",\"chaos.A>B\"],[\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":740,"completion_tokens":41,"total_tokens":781,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["annul.A","exalt.B.high"],["exalt.B.high"]],"acceptedIncludingGoalPrefix":[["annul.A","exalt.B.high"],["exalt.B.high"]],"missingIncludingGoalPrefix":3,"assessment":[{"i":1,"result":"full","step":1},{"i":2,"result":"unfinished","step":1},{"i":3,"result":"full","step":2},{"i":4,"result":"invalid","step":2},{"i":5,"result":"unfinished","step":1}]}
```

## 호출 11 · pseudo-1 · Prismatic Ring · 보강

사전 기대 실행열 4개.

```json
[["exalt.A","divine.high","exalt.B"],["exalt.A","exalt.B"],["exalt.B","divine.high","exalt.A"],["exalt.B","exalt.A"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Prismatic Ring","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Strength","unit":"flat","range":[17,20]},{"ref":"B","stat":"All Attributes","unit":"flat","range":[5,7]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"P","min":22,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":2,"DIVINE":1},"menu":["exalt.A","exalt.B","divine.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","derived":{"P":{"label":"Total Strength","sum":["A","B"],"implicitContribution":0}},"filterRule":"P is a derived display stat. Sum its direct layer-specific contributions once. A missing contribution adds zero. The derived stat is absent if all contributions are absent. P is not an affix to roll.","modifierFacts":{"A":{"family":["Strength"],"tier":5,"requiresItemLevel":33,"acquireVia":["exalt.A"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[17,20]},"B":{"family":["AllAttributes"],"tier":3,"requiresItemLevel":11,"acquireVia":["exalt.B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[5,7]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.A":{"consume":"EXALTED","gain":"A","sourceValue":17},"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":5},"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","exalt.B","divine.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.A\",\"exalt.B\"],[\"exalt.B\",\"exalt.A\"],[\"exalt.A\",\"divine.high\"],[\"exalt.B\",\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":629,"completion_tokens":40,"total_tokens":669,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.A","exalt.B"],["exalt.B","exalt.A"]],"acceptedIncludingGoalPrefix":[["exalt.A","exalt.B"],["exalt.B","exalt.A"]],"missingIncludingGoalPrefix":2,"assessment":[{"i":1,"result":"full","step":2},{"i":2,"result":"full","step":2},{"i":3,"result":"unfinished","step":2},{"i":4,"result":"unfinished","step":2}]}
```

## 호출 12 · pseudo-1 · Prismatic Ring · 간략

사전 기대 실행열 4개.

```json
[["exalt.A","divine.high","exalt.B"],["exalt.A","exalt.B"],["exalt.B","divine.high","exalt.A"],["exalt.B","exalt.A"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Prismatic Ring","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Strength","unit":"flat","range":[17,20]},{"ref":"B","stat":"All Attributes","unit":"flat","range":[5,7]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"P","min":22,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":2,"DIVINE":1},"menu":["exalt.A","exalt.B","divine.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","derived":{"P":"Total Strength"}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","exalt.B","divine.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.A\"],[\"exalt.B\"],[\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":397,"completion_tokens":19,"total_tokens":416,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":4,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"unfinished","step":1},{"i":3,"result":"invalid","step":1}]}
```

## 호출 13 · unit-1 · Distorted Amulet · 간략

사전 기대 실행열 2개.

```json
[["divine.high","exalt.B"],["exalt.B","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Distorted Amulet","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":246,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Life Regeneration per second","unit":"flat","range":[246,360]},{"ref":"B","stat":"Chaos Resistance","unit":"percent","range":[4,7]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":6.0,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"DIVINE":1,"EXALTED":1},"menu":["divine.high","exalt.B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.high","exalt.B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":425,"completion_tokens":9,"total_tokens":434,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":2,"assessment":[{"i":1,"result":"unfinished","step":1}]}
```

## 호출 14 · unit-1 · Distorted Amulet · 보강

사전 기대 실행열 2개.

```json
[["divine.high","exalt.B"],["exalt.B","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Distorted Amulet","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":246,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Life Regeneration per second","unit":"flat","range":[246,360]},{"ref":"B","stat":"Chaos Resistance","unit":"percent","range":[4,7]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":6.0,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"DIVINE":1,"EXALTED":1},"menu":["divine.high","exalt.B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"The action rolls source integers; compare goal ranges in display units after dividing by the stat-specific divisor. Do not round a near miss into a match.","modifierFacts":{"A":{"family":["LifeRegeneration"],"tier":7,"requiresItemLevel":17,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":60,"sourceRange":[246,360]},"B":{"family":["ChaosResistance"],"tier":6,"requiresItemLevel":16,"acquireVia":["exalt.B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[4,7]}},"rarePrefixSuffixCapacity":[3,2],"actions":{"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"},"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":4}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.high","exalt.B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B\",\"divine.high\"],[\"divine.high\",\"exalt.B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":610,"completion_tokens":22,"total_tokens":632,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["divine.high","exalt.B"],["exalt.B","divine.high"]],"acceptedIncludingGoalPrefix":[["divine.high","exalt.B"],["exalt.B","divine.high"]],"missingIncludingGoalPrefix":0,"assessment":[{"i":1,"result":"full","step":2},{"i":2,"result":"full","step":2}]}
```

## 호출 15 · disabled-1 · Crimson Amulet · 보강

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["annul.A","exalt.C","exalt.B"],["chaos.A>B"],["exalt.B"],["exalt.C","annul.A","exalt.B"],["exalt.C","chaos.A>B"],["exalt.C","exalt.B"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Crimson Amulet","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":12,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"B","stat":"Maximum Mana","unit":"flat","range":[125,149]}],"suffix":[{"ref":"A","stat":"All Elemental Resistances","unit":"percent","range":[12,14]},{"ref":"C","stat":"Level of all Projectile Skills","unit":"flat","range":[2,2]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false},{"ref":"C","min":null,"max":null,"weight":null,"disabled":true}],"min":null,"max":null,"disabled":false},{"op":"NOT","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":true}],"stock":{"EXALTED":2,"ANNULMENT":1,"CHAOS":1},"menu":["exalt.B","exalt.C","annul.A","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"Disabled groups and rows are ignored when evaluating the goal. Their modifiers may remain. A legal intermediate step is allowed if the goal has not yet been met.","modifierFacts":{"A":{"family":["AllResistances"],"tier":3,"requiresItemLevel":54,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[12,14]},"B":{"family":["IncreasedMana"],"tier":4,"requiresItemLevel":65,"acquireVia":["exalt.B","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[125,149]},"C":{"family":["IncreaseSocketedGemLevel"],"tier":2,"requiresItemLevel":41,"acquireVia":["exalt.C"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[2,2]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":125},"exalt.C":{"consume":"EXALTED","gain":"C","sourceValue":2},"annul.A":{"consume":"ANNULMENT","remove":"A"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":125}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","exalt.C","annul.A","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"annul.A\",\"exalt.B\"],[\"chaos.A>B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":792,"completion_tokens":19,"total_tokens":811,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["annul.A","exalt.B"],["chaos.A>B"]],"acceptedIncludingGoalPrefix":[["annul.A","exalt.B"],["chaos.A>B"]],"missingIncludingGoalPrefix":5,"assessment":[{"i":1,"result":"full","step":2},{"i":2,"result":"full","step":1}]}
```

## 호출 16 · disabled-1 · Crimson Amulet · 간략

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["annul.A","exalt.C","exalt.B"],["chaos.A>B"],["exalt.B"],["exalt.C","annul.A","exalt.B"],["exalt.C","chaos.A>B"],["exalt.C","exalt.B"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Crimson Amulet","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":12,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"B","stat":"Maximum Mana","unit":"flat","range":[125,149]}],"suffix":[{"ref":"A","stat":"All Elemental Resistances","unit":"percent","range":[12,14]},{"ref":"C","stat":"Level of all Projectile Skills","unit":"flat","range":[2,2]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false},{"ref":"C","min":null,"max":null,"weight":null,"disabled":true}],"min":null,"max":null,"disabled":false},{"op":"NOT","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":true}],"stock":{"EXALTED":2,"ANNULMENT":1,"CHAOS":1},"menu":["exalt.B","exalt.C","annul.A","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","exalt.C","annul.A","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"annul.A\",\"chaos.A>B\",\"exalt.B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":510,"completion_tokens":18,"total_tokens":528,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":7,"assessment":[{"i":1,"result":"invalid","step":2}]}
```

## 호출 17 · floor-1 · Slipstrike Vest · 간략

사전 기대 실행열 2개.

```json
[["exalt.A","perfectExalt.B"],["perfectExalt.B","exalt.A"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Slipstrike Vest","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Life Regeneration per second","unit":"flat","range":[126,180]},{"ref":"B","stat":"Dexterity","unit":"flat","range":[28,30]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"PERFECT_EXALTED":1},"menu":["exalt.A","perfectExalt.A","exalt.B","perfectExalt.B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","perfectExalt.A","exalt.B","perfectExalt.B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.A\"],[\"perfectExalt.A\"],[\"exalt.B\"],[\"perfectExalt.B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":431,"completion_tokens":26,"total_tokens":457,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":2,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"invalid","step":1},{"i":3,"result":"unfinished","step":1},{"i":4,"result":"unfinished","step":1}]}
```

## 호출 18 · floor-1 · Slipstrike Vest · 보강

사전 기대 실행열 2개.

```json
[["exalt.A","perfectExalt.B"],["perfectExalt.B","exalt.A"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Slipstrike Vest","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Life Regeneration per second","unit":"flat","range":[126,180]},{"ref":"B","stat":"Dexterity","unit":"flat","range":[28,30]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"PERFECT_EXALTED":1},"menu":["exalt.A","perfectExalt.A","exalt.B","perfectExalt.B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"Perfect Exalted has minimum modifier level 50. The highest eligible modifier level in each side/family is also allowed below the floor. Ordinary Exalted uses the ordinary eligible pool.","modifierFacts":{"A":{"family":["LifeRegeneration"],"tier":9,"requiresItemLevel":5,"acquireVia":["exalt.A","perfectExalt.A"],"preservedByFracture":false,"sourceDivisor":60,"sourceRange":[126,180],"highestEligibleLevelInFamilyAtStart":81},"B":{"family":["Dexterity"],"tier":3,"requiresItemLevel":66,"acquireVia":["exalt.B","perfectExalt.B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[28,30],"highestEligibleLevelInFamilyAtStart":74}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.A":{"consume":"EXALTED","gain":"A","sourceValue":126},"perfectExalt.A":{"consume":"PERFECT_EXALTED","gain":"A","sourceValue":126},"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":28},"perfectExalt.B":{"consume":"PERFECT_EXALTED","gain":"B","sourceValue":28}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","perfectExalt.A","exalt.B","perfectExalt.B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"perfectExalt.A\",\"perfectExalt.B\"],[\"perfectExalt.B\",\"perfectExalt.A\"],[\"exalt.A\",\"exalt.B\"],[\"exalt.B\",\"exalt.A\"],[\"perfectExalt.A\",\"exalt.B\"],[\"perfectExalt.B\",\"exalt.A\"],[\"exalt.A\",\"perfectExalt.B\"],[\"exalt.B\",\"perfectExalt.A\"]]}","finish_reason":"stop","usage":{"prompt_tokens":698,"completion_tokens":84,"total_tokens":782,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.A","perfectExalt.B"],["perfectExalt.B","exalt.A"]],"acceptedIncludingGoalPrefix":[["exalt.A","perfectExalt.B"],["perfectExalt.B","exalt.A"]],"missingIncludingGoalPrefix":0,"assessment":[{"i":1,"result":"invalid","step":1},{"i":2,"result":"invalid","step":2},{"i":3,"result":"invalid","step":2},{"i":4,"result":"invalid","step":2},{"i":5,"result":"invalid","step":1},{"i":6,"result":"full","step":2},{"i":7,"result":"full","step":2},{"i":8,"result":"invalid","step":2}]}
```

## 호출 19 · locked-1 · Two-Stone Ring · 보강

사전 기대 실행열 1개.

```json
[["divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Two-Stone Ring","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"B","sourceValue":12,"fractured":false}],"suffix":[{"ref":"A","sourceValue":2,"fractured":true}]},"optionRefs":{"prefix":[{"ref":"B","stat":"Increased Rarity of Items found","unit":"percent","range":[12,15]}],"suffix":[{"ref":"A","stat":"Mana gained per enemy killed","unit":"flat","range":[2,3]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":2,"max":2,"weight":null,"disabled":false},{"ref":"B","min":15,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"DIVINE":1,"ANNULMENT":1,"CHAOS":1},"menu":["divine.high","annul.A","annul.B","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"A fractured explicit remains at its original value during Divine rerolls and cannot be removed by annul or chaos. Other explicit values can still change.","modifierFacts":{"A":{"family":["ManaGainedFromEnemyDeath"],"tier":6,"requiresItemLevel":1,"acquireVia":[],"preservedByFracture":true,"sourceDivisor":1,"sourceRange":[2,3]},"B":{"family":["ItemFoundRarityIncreasePrefix"],"tier":2,"requiresItemLevel":29,"acquireVia":["chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[12,15]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"},"annul.A":{"consume":"ANNULMENT","remove":"A"},"annul.B":{"consume":"ANNULMENT","remove":"B"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":12}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.high","annul.A","annul.B","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"chaos.A>B\",\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":691,"completion_tokens":14,"total_tokens":705,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":1,"assessment":[{"i":1,"result":"invalid","step":1}]}
```

## 호출 20 · locked-1 · Two-Stone Ring · 간략

사전 기대 실행열 1개.

```json
[["divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Two-Stone Ring","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"B","sourceValue":12,"fractured":false}],"suffix":[{"ref":"A","sourceValue":2,"fractured":true}]},"optionRefs":{"prefix":[{"ref":"B","stat":"Increased Rarity of Items found","unit":"percent","range":[12,15]}],"suffix":[{"ref":"A","stat":"Mana gained per enemy killed","unit":"flat","range":[2,3]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":2,"max":2,"weight":null,"disabled":false},{"ref":"B","min":15,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"DIVINE":1,"ANNULMENT":1,"CHAOS":1},"menu":["divine.high","annul.A","annul.B","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.high","annul.A","annul.B","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":466,"completion_tokens":9,"total_tokens":475,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["divine.high"]],"acceptedIncludingGoalPrefix":[["divine.high"]],"missingIncludingGoalPrefix":0,"assessment":[{"i":1,"result":"full","step":1}]}
```

## 호출 21 · if-2 · Lazuli Ring · 보강

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["chaos.A>B"],["divine.high","annul.A","exalt.B"],["divine.high","chaos.A>B"],["divine.high","exalt.B"],["exalt.B","annul.A"],["exalt.B","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Lazuli Ring","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":31,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Strength","unit":"flat","range":[31,33]},{"ref":"B","stat":"Chaos Resistance","unit":"percent","range":[4,7]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false},{"op":"IF","rows":[{"ref":"A","min":33,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1,"DIVINE":1},"menu":["exalt.B","annul.A","chaos.A>B","divine.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"IF: an absent row passes. A present row must satisfy its range. Every active group must pass.","modifierFacts":{"A":{"family":["Strength"],"tier":1,"requiresItemLevel":74,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[31,33]},"B":{"family":["ChaosResistance"],"tier":6,"requiresItemLevel":16,"acquireVia":["exalt.B","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[4,7]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":4},"annul.A":{"consume":"ANNULMENT","remove":"A"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":4},"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","annul.A","chaos.A>B","divine.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B\"],[\"chaos.A>B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":684,"completion_tokens":15,"total_tokens":699,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["chaos.A>B"]],"acceptedIncludingGoalPrefix":[["chaos.A>B"]],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"full","step":1}]}
```

## 호출 22 · if-2 · Lazuli Ring · 간략

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["chaos.A>B"],["divine.high","annul.A","exalt.B"],["divine.high","chaos.A>B"],["divine.high","exalt.B"],["exalt.B","annul.A"],["exalt.B","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Lazuli Ring","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":31,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Strength","unit":"flat","range":[31,33]},{"ref":"B","stat":"Chaos Resistance","unit":"percent","range":[4,7]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false},{"op":"IF","rows":[{"ref":"A","min":33,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1,"DIVINE":1},"menu":["exalt.B","annul.A","chaos.A>B","divine.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","annul.A","chaos.A>B","divine.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B\"],[\"chaos.A>B\",\"exalt.B\"],[\"annul.A\",\"chaos.A>B\",\"exalt.B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":468,"completion_tokens":33,"total_tokens":501,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[["chaos.A>B"]],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"tail","step":1},{"i":3,"result":"invalid","step":2}]}
```

## 호출 23 · not-2 · Tawhoan Greatclub · 간략

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["chaos.A>B"],["divine.high","annul.A","exalt.B"],["divine.high","chaos.A>B"],["divine.high","exalt.B"],["exalt.B","annul.A"],["exalt.B","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Tawhoan Greatclub","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":21,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Strength","unit":"flat","range":[21,24]},{"ref":"B","stat":"Mana gained per enemy killed","unit":"flat","range":[2,3]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false},{"op":"NOT","rows":[{"ref":"A","min":null,"max":21,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1,"DIVINE":1},"menu":["exalt.B","annul.A","chaos.A>B","divine.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","annul.A","chaos.A>B","divine.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"chaos.A>B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":473,"completion_tokens":10,"total_tokens":483,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["chaos.A>B"]],"acceptedIncludingGoalPrefix":[["chaos.A>B"]],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"full","step":1}]}
```

## 호출 24 · not-2 · Tawhoan Greatclub · 보강

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["chaos.A>B"],["divine.high","annul.A","exalt.B"],["divine.high","chaos.A>B"],["divine.high","exalt.B"],["exalt.B","annul.A"],["exalt.B","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Tawhoan Greatclub","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":21,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Strength","unit":"flat","range":[21,24]},{"ref":"B","stat":"Mana gained per enemy killed","unit":"flat","range":[2,3]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false},{"op":"NOT","rows":[{"ref":"A","min":null,"max":21,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1,"DIVINE":1},"menu":["exalt.B","annul.A","chaos.A>B","divine.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"NOT matches if none of its enabled rows are both present and in range. A present value outside the forbidden interval can remain.","modifierFacts":{"A":{"family":["Strength"],"tier":4,"requiresItemLevel":44,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[21,24]},"B":{"family":["ManaGainedFromEnemyDeath"],"tier":8,"requiresItemLevel":1,"acquireVia":["exalt.B","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[2,3]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":2},"annul.A":{"consume":"ANNULMENT","remove":"A"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":2},"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","annul.A","chaos.A>B","divine.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B\"],[\"chaos.A>B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":697,"completion_tokens":15,"total_tokens":712,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["chaos.A>B"]],"acceptedIncludingGoalPrefix":[["chaos.A>B"]],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"full","step":1}]}
```

## 호출 25 · count-2 · Death Mantle · 보강

사전 기대 실행열 8개.

```json
[["annul.A","chaos.B>C"],["annul.A","exalt.C"],["annul.B","chaos.A>C"],["annul.B","exalt.C"],["chaos.A>C"],["chaos.B>C"],["exalt.C","annul.A"],["exalt.C","annul.B"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Death Mantle","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"A","sourceValue":54,"fractured":false}],"suffix":[{"ref":"B","sourceValue":31,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"A","stat":"Spirit","unit":"flat","range":[54,56]}],"suffix":[{"ref":"B","stat":"Intelligence","unit":"flat","range":[31,33]},{"ref":"C","stat":"Cold Resistance","unit":"percent","range":[16,20]}]},"goalGroups":[{"op":"COUNT","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false},{"ref":"C","min":null,"max":null,"weight":null,"disabled":false}],"min":1,"max":2,"disabled":false},{"op":"AND","rows":[{"ref":"C","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1},"menu":["exalt.C","annul.A","annul.B","chaos.A>C","chaos.B>C"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"COUNT counts enabled rows that are present and in range. Both minimum and maximum are inclusive; compare the count again after each removal or acquisition.","modifierFacts":{"A":{"family":["BaseSpirit"],"tier":2,"requiresItemLevel":65,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[54,56]},"B":{"family":["Intelligence"],"tier":1,"requiresItemLevel":74,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[31,33]},"C":{"family":["ColdResistance"],"tier":6,"requiresItemLevel":26,"acquireVia":["exalt.C","chaos.A>C","chaos.B>C"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[16,20]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.C":{"consume":"EXALTED","gain":"C","sourceValue":16},"annul.A":{"consume":"ANNULMENT","remove":"A"},"annul.B":{"consume":"ANNULMENT","remove":"B"},"chaos.A>C":{"consume":"CHAOS","remove":"A","gain":"C","sourceValue":16},"chaos.B>C":{"consume":"CHAOS","remove":"B","gain":"C","sourceValue":16}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.C","annul.A","annul.B","chaos.A>C","chaos.B>C"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.C\"],[\"chaos.A>C\"],[\"chaos.B>C\"],[\"annul.A\"],[\"annul.B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":840,"completion_tokens":31,"total_tokens":871,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["chaos.A>C"],["chaos.B>C"]],"acceptedIncludingGoalPrefix":[["chaos.A>C"],["chaos.B>C"]],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"full","step":1},{"i":3,"result":"full","step":1},{"i":4,"result":"unfinished","step":1},{"i":5,"result":"unfinished","step":1}]}
```

## 호출 26 · count-2 · Death Mantle · 간략

사전 기대 실행열 8개.

```json
[["annul.A","chaos.B>C"],["annul.A","exalt.C"],["annul.B","chaos.A>C"],["annul.B","exalt.C"],["chaos.A>C"],["chaos.B>C"],["exalt.C","annul.A"],["exalt.C","annul.B"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Death Mantle","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"A","sourceValue":54,"fractured":false}],"suffix":[{"ref":"B","sourceValue":31,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"A","stat":"Spirit","unit":"flat","range":[54,56]}],"suffix":[{"ref":"B","stat":"Intelligence","unit":"flat","range":[31,33]},{"ref":"C","stat":"Cold Resistance","unit":"percent","range":[16,20]}]},"goalGroups":[{"op":"COUNT","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false},{"ref":"C","min":null,"max":null,"weight":null,"disabled":false}],"min":1,"max":2,"disabled":false},{"op":"AND","rows":[{"ref":"C","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1},"menu":["exalt.C","annul.A","annul.B","chaos.A>C","chaos.B>C"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.C","annul.A","annul.B","chaos.A>C","chaos.B>C"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"chaos.A>C\"]]}","finish_reason":"stop","usage":{"prompt_tokens":547,"completion_tokens":10,"total_tokens":557,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["chaos.A>C"]],"acceptedIncludingGoalPrefix":[["chaos.A>C"]],"missingIncludingGoalPrefix":7,"assessment":[{"i":1,"result":"full","step":1}]}
```

## 호출 27 · w1-2 · Azure Amulet · 간략

사전 기대 실행열 6개.

```json
[["annul.A","exalt.B.high"],["chaos.A>B","divine.high"],["divine.high","annul.A","exalt.B.high"],["divine.high","exalt.B.high"],["exalt.B.high","annul.A"],["exalt.B.high","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Azure Amulet","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":15,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"All Elemental Resistances","unit":"percent","range":[15,16]},{"ref":"B","stat":"Chaos Resistance","unit":"percent","range":[24,27]}]},"goalGroups":[{"op":"WEIGHTED_V1","rows":[{"ref":"A","min":16,"max":null,"weight":1,"disabled":false},{"ref":"B","min":null,"max":null,"weight":2,"disabled":false}],"min":54,"max":null,"disabled":false},{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"DIVINE":1,"CHAOS":1},"menu":["exalt.B.high","annul.A","divine.high","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B.high","annul.A","divine.high","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":497,"completion_tokens":10,"total_tokens":507,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"unfinished","step":1}]}
```

## 호출 28 · w1-2 · Azure Amulet · 보강

사전 기대 실행열 6개.

```json
[["annul.A","exalt.B.high"],["chaos.A>B","divine.high"],["divine.high","annul.A","exalt.B.high"],["divine.high","exalt.B.high"],["exalt.B.high","annul.A"],["exalt.B.high","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Azure Amulet","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":15,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"All Elemental Resistances","unit":"percent","range":[15,16]},{"ref":"B","stat":"Chaos Resistance","unit":"percent","range":[24,27]}]},"goalGroups":[{"op":"WEIGHTED_V1","rows":[{"ref":"A","min":16,"max":null,"weight":1,"disabled":false},{"ref":"B","min":null,"max":null,"weight":2,"disabled":false}],"min":54,"max":null,"disabled":false},{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"DIVINE":1,"CHAOS":1},"menu":["exalt.B.high","annul.A","divine.high","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"WEIGHTED_V1: absent rows are optional. Every present enabled row must pass its own range, and the sum of present value times weight must lie in the group range. Weights are score coefficients.","modifierFacts":{"A":{"family":["AllResistances"],"tier":2,"requiresItemLevel":68,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[15,16]},"B":{"family":["ChaosResistance"],"tier":1,"requiresItemLevel":81,"acquireVia":["exalt.B.high","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[24,27]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.B.high":{"consume":"EXALTED","gain":"B","sourceValue":27},"annul.A":{"consume":"ANNULMENT","remove":"A"},"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":24}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B.high","annul.A","divine.high","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B.high\"],[\"chaos.A>B\"],[\"annul.A\",\"exalt.B.high\"],[\"annul.A\",\"chaos.A>B\"],[\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":740,"completion_tokens":41,"total_tokens":781,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["annul.A","exalt.B.high"]],"acceptedIncludingGoalPrefix":[["annul.A","exalt.B.high"]],"missingIncludingGoalPrefix":5,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"unfinished","step":1},{"i":3,"result":"full","step":2},{"i":4,"result":"invalid","step":2},{"i":5,"result":"unfinished","step":1}]}
```

## 호출 29 · w2-2 · Kinetic Ring · 보강

사전 기대 실행열 5개.

```json
[["annul.A","exalt.B.high"],["chaos.A>B","divine.high"],["divine.high","annul.A","exalt.B.high"],["divine.high","exalt.B.high"],["exalt.B.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Kinetic Ring","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":9,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"B","stat":"Maximum Mana","unit":"flat","range":[15,24]}],"suffix":[{"ref":"A","stat":"Increased Cast Speed","unit":"percent","range":[9,12]}]},"goalGroups":[{"op":"WEIGHTED_V2","rows":[{"ref":"A","min":12,"max":null,"weight":1,"disabled":false},{"ref":"B","min":null,"max":null,"weight":2,"disabled":false}],"min":48,"max":null,"disabled":false},{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"DIVINE":1,"CHAOS":1},"menu":["exalt.B.high","annul.A","divine.high","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"WEIGHTED_V2: at least one enabled row must be present. Only present rows inside their own range contribute value times weight. A present out-of-range row adds zero and does not veto the group.","modifierFacts":{"A":{"family":["IncreasedCastSpeed"],"tier":5,"requiresItemLevel":1,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[9,12]},"B":{"family":["IncreasedMana"],"tier":11,"requiresItemLevel":6,"acquireVia":["exalt.B.high","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[15,24]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.B.high":{"consume":"EXALTED","gain":"B","sourceValue":24},"annul.A":{"consume":"ANNULMENT","remove":"A"},"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":15}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B.high","annul.A","divine.high","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B.high\"],[\"chaos.A>B\"],[\"annul.A\",\"exalt.B.high\"],[\"annul.A\",\"chaos.A>B\"],[\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":740,"completion_tokens":41,"total_tokens":781,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["annul.A","exalt.B.high"],["exalt.B.high"]],"acceptedIncludingGoalPrefix":[["annul.A","exalt.B.high"],["exalt.B.high"]],"missingIncludingGoalPrefix":3,"assessment":[{"i":1,"result":"full","step":1},{"i":2,"result":"unfinished","step":1},{"i":3,"result":"full","step":2},{"i":4,"result":"invalid","step":2},{"i":5,"result":"unfinished","step":1}]}
```

## 호출 30 · w2-2 · Kinetic Ring · 간략

사전 기대 실행열 5개.

```json
[["annul.A","exalt.B.high"],["chaos.A>B","divine.high"],["divine.high","annul.A","exalt.B.high"],["divine.high","exalt.B.high"],["exalt.B.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Kinetic Ring","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":9,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"B","stat":"Maximum Mana","unit":"flat","range":[15,24]}],"suffix":[{"ref":"A","stat":"Increased Cast Speed","unit":"percent","range":[9,12]}]},"goalGroups":[{"op":"WEIGHTED_V2","rows":[{"ref":"A","min":12,"max":null,"weight":1,"disabled":false},{"ref":"B","min":null,"max":null,"weight":2,"disabled":false}],"min":48,"max":null,"disabled":false},{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"DIVINE":1,"CHAOS":1},"menu":["exalt.B.high","annul.A","divine.high","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B.high","annul.A","divine.high","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B.high\"],[\"chaos.A>B\",\"exalt.B.high\"],[\"annul.A\",\"exalt.B.high\"],[\"divine.high\",\"exalt.B.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":496,"completion_tokens":41,"total_tokens":537,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["annul.A","exalt.B.high"],["divine.high","exalt.B.high"],["exalt.B.high"]],"acceptedIncludingGoalPrefix":[["annul.A","exalt.B.high"],["divine.high","exalt.B.high"],["exalt.B.high"]],"missingIncludingGoalPrefix":2,"assessment":[{"i":1,"result":"full","step":1},{"i":2,"result":"invalid","step":2},{"i":3,"result":"full","step":2},{"i":4,"result":"full","step":2}]}
```

## 호출 31 · pseudo-2 · Solar Amulet · 간략

사전 기대 실행열 3개.

```json
[["exalt.A"],["exalt.B","divine.high","exalt.A"],["exalt.B","exalt.A"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Solar Amulet","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Dexterity","unit":"flat","range":[25,27]},{"ref":"B","stat":"All Attributes","unit":"flat","range":[2,4]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"P","min":25,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":2,"DIVINE":1},"menu":["exalt.A","exalt.B","divine.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","derived":{"P":"Total Dexterity"}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","exalt.B","divine.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.A\"],[\"exalt.B\",\"exalt.A\"],[\"exalt.B\",\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":399,"completion_tokens":27,"total_tokens":426,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.A"],["exalt.B","exalt.A"]],"acceptedIncludingGoalPrefix":[["exalt.A"],["exalt.B","exalt.A"]],"missingIncludingGoalPrefix":1,"assessment":[{"i":1,"result":"full","step":1},{"i":2,"result":"full","step":2},{"i":3,"result":"unfinished","step":2}]}
```

## 호출 32 · pseudo-2 · Solar Amulet · 보강

사전 기대 실행열 3개.

```json
[["exalt.A"],["exalt.B","divine.high","exalt.A"],["exalt.B","exalt.A"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Solar Amulet","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Dexterity","unit":"flat","range":[25,27]},{"ref":"B","stat":"All Attributes","unit":"flat","range":[2,4]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"P","min":25,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":2,"DIVINE":1},"menu":["exalt.A","exalt.B","divine.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","derived":{"P":{"label":"Total Dexterity","sum":["A","B"],"implicitContribution":0}},"filterRule":"P is a derived display stat. Sum its direct layer-specific contributions once. A missing contribution adds zero. The derived stat is absent if all contributions are absent. P is not an affix to roll.","modifierFacts":{"A":{"family":["Dexterity"],"tier":3,"requiresItemLevel":55,"acquireVia":["exalt.A"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[25,27]},"B":{"family":["AllAttributes"],"tier":9,"requiresItemLevel":1,"acquireVia":["exalt.B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[2,4]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.A":{"consume":"EXALTED","gain":"A","sourceValue":25},"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":2},"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","exalt.B","divine.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.A\"],[\"exalt.B\"],[\"exalt.B\",\"exalt.A\"],[\"exalt.A\",\"exalt.B\"],[\"exalt.A\",\"divine.high\"],[\"exalt.B\",\"divine.high\"],[\"exalt.A\",\"exalt.B\",\"divine.high\"],[\"exalt.B\",\"exalt.A\",\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":632,"completion_tokens":76,"total_tokens":708,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.A"],["exalt.B","exalt.A"]],"acceptedIncludingGoalPrefix":[["exalt.A"],["exalt.B","exalt.A"]],"missingIncludingGoalPrefix":1,"assessment":[{"i":1,"result":"full","step":1},{"i":2,"result":"unfinished","step":1},{"i":3,"result":"full","step":2},{"i":4,"result":"tail","step":1},{"i":5,"result":"tail","step":1},{"i":6,"result":"unfinished","step":2},{"i":7,"result":"tail","step":1},{"i":8,"result":"tail","step":2}]}
```

## 호출 33 · unit-2 · Forking Belt · 보강

사전 기대 실행열 6개.

```json
[["exalt.A","divine.high","exalt.B"],["exalt.A","exalt.B","divine.high"],["exalt.A.high","exalt.B"],["exalt.B","divine.high","exalt.A.high"],["exalt.B","exalt.A","divine.high"],["exalt.B","exalt.A.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Forking Belt","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Life Regeneration per second","unit":"flat","range":[786,1080]},{"ref":"B","stat":"Strength","unit":"flat","range":[5,8]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":18.0,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"DIVINE":1,"EXALTED":2},"menu":["divine.high","exalt.B","exalt.A","exalt.A.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"The action rolls source integers; compare goal ranges in display units after dividing by the stat-specific divisor. Do not round a near miss into a match.","modifierFacts":{"A":{"family":["LifeRegeneration"],"tier":3,"requiresItemLevel":47,"acquireVia":["exalt.A","exalt.A.high"],"preservedByFracture":false,"sourceDivisor":60,"sourceRange":[786,1080]},"B":{"family":["Strength"],"tier":9,"requiresItemLevel":1,"acquireVia":["exalt.B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[5,8]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"},"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":5},"exalt.A":{"consume":"EXALTED","gain":"A","sourceValue":786},"exalt.A.high":{"consume":"EXALTED","gain":"A","sourceValue":1080}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.high","exalt.B","exalt.A","exalt.A.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B\",\"exalt.A.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":661,"completion_tokens":14,"total_tokens":675,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.B","exalt.A.high"]],"acceptedIncludingGoalPrefix":[["exalt.B","exalt.A.high"]],"missingIncludingGoalPrefix":5,"assessment":[{"i":1,"result":"full","step":2}]}
```

## 호출 34 · unit-2 · Forking Belt · 간략

사전 기대 실행열 6개.

```json
[["exalt.A","divine.high","exalt.B"],["exalt.A","exalt.B","divine.high"],["exalt.A.high","exalt.B"],["exalt.B","divine.high","exalt.A.high"],["exalt.B","exalt.A","divine.high"],["exalt.B","exalt.A.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Forking Belt","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Life Regeneration per second","unit":"flat","range":[786,1080]},{"ref":"B","stat":"Strength","unit":"flat","range":[5,8]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":18.0,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"DIVINE":1,"EXALTED":2},"menu":["divine.high","exalt.B","exalt.A","exalt.A.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.high","exalt.B","exalt.A","exalt.A.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B\",\"divine.high\"],[\"exalt.A.high\",\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":427,"completion_tokens":23,"total_tokens":450,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"unfinished","step":2},{"i":2,"result":"invalid","step":2}]}
```

## 호출 35 · disabled-2 · Tasalian Greaves · 간략

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["annul.A","exalt.C","exalt.B"],["chaos.A>B"],["exalt.B"],["exalt.C","annul.A","exalt.B"],["exalt.C","chaos.A>B"],["exalt.C","exalt.B"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Tasalian Greaves","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"A","sourceValue":10,"fractured":false}],"suffix":[]},"optionRefs":{"prefix":[{"ref":"A","stat":"Maximum Life","unit":"flat","range":[10,19]},{"ref":"B","stat":"Maximum Mana","unit":"flat","range":[10,14]}],"suffix":[{"ref":"C","stat":"Cold Resistance","unit":"percent","range":[31,35]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false},{"ref":"C","min":null,"max":null,"weight":null,"disabled":true}],"min":null,"max":null,"disabled":false},{"op":"NOT","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":true}],"stock":{"EXALTED":2,"ANNULMENT":1,"CHAOS":1},"menu":["exalt.B","exalt.C","annul.A","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","exalt.C","annul.A","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"chaos.A>B\",\"exalt.B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":504,"completion_tokens":14,"total_tokens":518,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[["chaos.A>B"]],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"tail","step":1}]}
```

## 호출 36 · disabled-2 · Tasalian Greaves · 보강

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["annul.A","exalt.C","exalt.B"],["chaos.A>B"],["exalt.B"],["exalt.C","annul.A","exalt.B"],["exalt.C","chaos.A>B"],["exalt.C","exalt.B"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Tasalian Greaves","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"A","sourceValue":10,"fractured":false}],"suffix":[]},"optionRefs":{"prefix":[{"ref":"A","stat":"Maximum Life","unit":"flat","range":[10,19]},{"ref":"B","stat":"Maximum Mana","unit":"flat","range":[10,14]}],"suffix":[{"ref":"C","stat":"Cold Resistance","unit":"percent","range":[31,35]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false},{"ref":"C","min":null,"max":null,"weight":null,"disabled":true}],"min":null,"max":null,"disabled":false},{"op":"NOT","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":true}],"stock":{"EXALTED":2,"ANNULMENT":1,"CHAOS":1},"menu":["exalt.B","exalt.C","annul.A","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"Disabled groups and rows are ignored when evaluating the goal. Their modifiers may remain. A legal intermediate step is allowed if the goal has not yet been met.","modifierFacts":{"A":{"family":["IncreasedLife"],"tier":9,"requiresItemLevel":1,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[10,19]},"B":{"family":["IncreasedMana"],"tier":9,"requiresItemLevel":1,"acquireVia":["exalt.B","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[10,14]},"C":{"family":["ColdResistance"],"tier":3,"requiresItemLevel":60,"acquireVia":["exalt.C"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[31,35]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":10},"exalt.C":{"consume":"EXALTED","gain":"C","sourceValue":31},"annul.A":{"consume":"ANNULMENT","remove":"A"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":10}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","exalt.C","annul.A","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"chaos.A>B\",\"exalt.B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":782,"completion_tokens":14,"total_tokens":796,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[["chaos.A>B"]],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"tail","step":1}]}
```

## 호출 37 · floor-2 · Serrated Quiver · 보강

사전 기대 실행열 4개.

```json
[["exalt.A","exalt.B"],["exalt.A","perfectExalt.B"],["exalt.B","exalt.A"],["perfectExalt.B","exalt.A"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Serrated Quiver","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Mana gained per enemy killed","unit":"flat","range":[4,5]},{"ref":"B","stat":"Level of all Projectile Skills","unit":"flat","range":[1,1]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":2,"PERFECT_EXALTED":1},"menu":["exalt.A","perfectExalt.A","exalt.B","perfectExalt.B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"Perfect Exalted has minimum modifier level 50. The highest eligible modifier level in each side/family is also allowed below the floor. Ordinary Exalted uses the ordinary eligible pool.","modifierFacts":{"A":{"family":["ManaGainedFromEnemyDeath"],"tier":5,"requiresItemLevel":12,"acquireVia":["exalt.A","perfectExalt.A"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[4,5],"highestEligibleLevelInFamilyAtStart":56},"B":{"family":["IncreaseSocketedGemLevel"],"tier":1,"requiresItemLevel":5,"acquireVia":["exalt.B","perfectExalt.B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[1,1],"highestEligibleLevelInFamilyAtStart":5}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.A":{"consume":"EXALTED","gain":"A","sourceValue":4},"perfectExalt.A":{"consume":"PERFECT_EXALTED","gain":"A","sourceValue":4},"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":1},"perfectExalt.B":{"consume":"PERFECT_EXALTED","gain":"B","sourceValue":1}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","perfectExalt.A","exalt.B","perfectExalt.B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.A\",\"exalt.B\"],[\"exalt.B\",\"exalt.A\"],[\"perfectExalt.A\",\"exalt.B\"],[\"perfectExalt.B\",\"exalt.A\"],[\"exalt.A\",\"perfectExalt.B\"],[\"exalt.B\",\"perfectExalt.A\"],[\"perfectExalt.A\",\"perfectExalt.B\"],[\"perfectExalt.B\",\"perfectExalt.A\"]]}","finish_reason":"stop","usage":{"prompt_tokens":709,"completion_tokens":84,"total_tokens":793,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.A","exalt.B"],["exalt.A","perfectExalt.B"],["exalt.B","exalt.A"],["perfectExalt.B","exalt.A"]],"acceptedIncludingGoalPrefix":[["exalt.A","exalt.B"],["exalt.A","perfectExalt.B"],["exalt.B","exalt.A"],["perfectExalt.B","exalt.A"]],"missingIncludingGoalPrefix":0,"assessment":[{"i":1,"result":"full","step":2},{"i":2,"result":"full","step":2},{"i":3,"result":"invalid","step":1},{"i":4,"result":"full","step":2},{"i":5,"result":"full","step":2},{"i":6,"result":"invalid","step":2},{"i":7,"result":"invalid","step":1},{"i":8,"result":"invalid","step":2}]}
```

## 호출 38 · floor-2 · Serrated Quiver · 간략

사전 기대 실행열 4개.

```json
[["exalt.A","exalt.B"],["exalt.A","perfectExalt.B"],["exalt.B","exalt.A"],["perfectExalt.B","exalt.A"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Serrated Quiver","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Mana gained per enemy killed","unit":"flat","range":[4,5]},{"ref":"B","stat":"Level of all Projectile Skills","unit":"flat","range":[1,1]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":2,"PERFECT_EXALTED":1},"menu":["exalt.A","perfectExalt.A","exalt.B","perfectExalt.B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","perfectExalt.A","exalt.B","perfectExalt.B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.A\",\"exalt.B\"],[\"exalt.B\",\"exalt.A\"],[\"perfectExalt.A\",\"exalt.B\"],[\"perfectExalt.B\",\"exalt.A\"],[\"exalt.A\",\"perfectExalt.B\"],[\"exalt.B\",\"perfectExalt.A\"],[\"perfectExalt.A\",\"perfectExalt.B\"],[\"perfectExalt.B\",\"perfectExalt.A\"]]}","finish_reason":"stop","usage":{"prompt_tokens":436,"completion_tokens":84,"total_tokens":520,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.A","exalt.B"],["exalt.A","perfectExalt.B"],["exalt.B","exalt.A"],["perfectExalt.B","exalt.A"]],"acceptedIncludingGoalPrefix":[["exalt.A","exalt.B"],["exalt.A","perfectExalt.B"],["exalt.B","exalt.A"],["perfectExalt.B","exalt.A"]],"missingIncludingGoalPrefix":0,"assessment":[{"i":1,"result":"full","step":2},{"i":2,"result":"full","step":2},{"i":3,"result":"invalid","step":1},{"i":4,"result":"full","step":2},{"i":5,"result":"full","step":2},{"i":6,"result":"invalid","step":2},{"i":7,"result":"invalid","step":1},{"i":8,"result":"invalid","step":2}]}
```

## 호출 39 · locked-2 · Sirenscale Gloves · 간략

사전 기대 실행열 1개.

```json
[["divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Sirenscale Gloves","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":34,"fractured":true},{"ref":"B","sourceValue":4,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Dexterity","unit":"flat","range":[34,36]},{"ref":"B","stat":"Mana gained per enemy killed","unit":"flat","range":[4,5]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":34,"max":34,"weight":null,"disabled":false},{"ref":"B","min":5,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"DIVINE":1,"ANNULMENT":1,"CHAOS":1},"menu":["divine.high","annul.A","annul.B","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.high","annul.A","annul.B","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"chaos.A>B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":460,"completion_tokens":10,"total_tokens":470,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":1,"assessment":[{"i":1,"result":"invalid","step":1}]}
```

## 호출 40 · locked-2 · Sirenscale Gloves · 보강

사전 기대 실행열 1개.

```json
[["divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Sirenscale Gloves","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":34,"fractured":true},{"ref":"B","sourceValue":4,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Dexterity","unit":"flat","range":[34,36]},{"ref":"B","stat":"Mana gained per enemy killed","unit":"flat","range":[4,5]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":34,"max":34,"weight":null,"disabled":false},{"ref":"B","min":5,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"DIVINE":1,"ANNULMENT":1,"CHAOS":1},"menu":["divine.high","annul.A","annul.B","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"A fractured explicit remains at its original value during Divine rerolls and cannot be removed by annul or chaos. Other explicit values can still change.","modifierFacts":{"A":{"family":["Dexterity"],"tier":1,"requiresItemLevel":81,"acquireVia":[],"preservedByFracture":true,"sourceDivisor":1,"sourceRange":[34,36]},"B":{"family":["ManaGainedFromEnemyDeath"],"tier":7,"requiresItemLevel":12,"acquireVia":["chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[4,5]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"},"annul.A":{"consume":"ANNULMENT","remove":"A"},"annul.B":{"consume":"ANNULMENT","remove":"B"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":4}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.high","annul.A","annul.B","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"chaos.A>B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":681,"completion_tokens":10,"total_tokens":691,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":1,"assessment":[{"i":1,"result":"invalid","step":1}]}
```

## 호출 41 · if-3 · Flexed Crossbow · 간략

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["chaos.A>B"],["divine.high","annul.A","exalt.B"],["divine.high","chaos.A>B"],["divine.high","exalt.B"],["exalt.B","annul.A"],["exalt.B","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Flexed Crossbow","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":6,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Mana gained per enemy killed","unit":"flat","range":[6,9]},{"ref":"B","stat":"Strength","unit":"flat","range":[25,27]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false},{"op":"IF","rows":[{"ref":"A","min":9,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1,"DIVINE":1},"menu":["exalt.B","annul.A","chaos.A>B","divine.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","annul.A","chaos.A>B","divine.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B\"],[\"chaos.A>B\",\"exalt.B\"],[\"annul.A\",\"chaos.A>B\",\"exalt.B\"],[\"divine.high\",\"exalt.B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":471,"completion_tokens":42,"total_tokens":513,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["divine.high","exalt.B"]],"acceptedIncludingGoalPrefix":[["chaos.A>B"],["divine.high","exalt.B"]],"missingIncludingGoalPrefix":5,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"tail","step":1},{"i":3,"result":"invalid","step":2},{"i":4,"result":"full","step":2}]}
```

## 호출 42 · if-3 · Flexed Crossbow · 보강

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["chaos.A>B"],["divine.high","annul.A","exalt.B"],["divine.high","chaos.A>B"],["divine.high","exalt.B"],["exalt.B","annul.A"],["exalt.B","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Flexed Crossbow","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":6,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Mana gained per enemy killed","unit":"flat","range":[6,9]},{"ref":"B","stat":"Strength","unit":"flat","range":[25,27]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false},{"op":"IF","rows":[{"ref":"A","min":9,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1,"DIVINE":1},"menu":["exalt.B","annul.A","chaos.A>B","divine.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"IF: an absent row passes. A present row must satisfy its range. Every active group must pass.","modifierFacts":{"A":{"family":["ManaGainedFromEnemyDeath"],"tier":6,"requiresItemLevel":23,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[6,9]},"B":{"family":["Strength"],"tier":3,"requiresItemLevel":55,"acquireVia":["exalt.B","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[25,27]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":25},"annul.A":{"consume":"ANNULMENT","remove":"A"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":25},"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","annul.A","chaos.A>B","divine.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B\"],[\"chaos.A>B\",\"exalt.B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":691,"completion_tokens":19,"total_tokens":710,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[["chaos.A>B"]],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"tail","step":1}]}
```

## 호출 43 · not-3 · Absent Amulet · 보강

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["chaos.A>B"],["divine.low","annul.A","exalt.B"],["divine.low","chaos.A>B"],["divine.low","exalt.B"],["exalt.B","annul.A"],["exalt.B","divine.low"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Absent Amulet","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":22,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"B","stat":"Maximum Energy Shield","unit":"flat","range":[21,24]}],"suffix":[{"ref":"A","stat":"All Attributes","unit":"flat","range":[21,22]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false},{"op":"NOT","rows":[{"ref":"A","min":22,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1,"DIVINE":1},"menu":["exalt.B","annul.A","chaos.A>B","divine.low"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"NOT matches if none of its enabled rows are both present and in range. A present value outside the forbidden interval can remain.","modifierFacts":{"A":{"family":["AllAttributes"],"tier":2,"requiresItemLevel":75,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[21,22]},"B":{"family":["IncreasedEnergyShield"],"tier":8,"requiresItemLevel":16,"acquireVia":["exalt.B","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[21,24]}},"rarePrefixSuffixCapacity":[2,2],"actions":{"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":21},"annul.A":{"consume":"ANNULMENT","remove":"A"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":21},"divine.low":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"low"}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","annul.A","chaos.A>B","divine.low"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"annul.A\",\"exalt.B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":692,"completion_tokens":13,"total_tokens":705,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["annul.A","exalt.B"]],"acceptedIncludingGoalPrefix":[["annul.A","exalt.B"]],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"full","step":2}]}
```

## 호출 44 · not-3 · Absent Amulet · 간략

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["chaos.A>B"],["divine.low","annul.A","exalt.B"],["divine.low","chaos.A>B"],["divine.low","exalt.B"],["exalt.B","annul.A"],["exalt.B","divine.low"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Absent Amulet","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":22,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"B","stat":"Maximum Energy Shield","unit":"flat","range":[21,24]}],"suffix":[{"ref":"A","stat":"All Attributes","unit":"flat","range":[21,22]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false},{"op":"NOT","rows":[{"ref":"A","min":22,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1,"DIVINE":1},"menu":["exalt.B","annul.A","chaos.A>B","divine.low"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","annul.A","chaos.A>B","divine.low"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B\",\"annul.A\"]]}","finish_reason":"stop","usage":{"prompt_tokens":469,"completion_tokens":13,"total_tokens":482,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.B","annul.A"]],"acceptedIncludingGoalPrefix":[["exalt.B","annul.A"]],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"full","step":2}]}
```

## 호출 45 · count-3 · Amber Amulet · 간략

사전 기대 실행열 6개.

```json
[["annul.A","exalt.C"],["annul.B","exalt.C"],["chaos.A>C"],["chaos.B>C"],["exalt.C","annul.A"],["exalt.C","annul.B"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Amber Amulet","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"A","sourceValue":3,"fractured":false}],"suffix":[{"ref":"B","sourceValue":21,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"A","stat":"Increased maximum Mana","unit":"percent","range":[3,4]}],"suffix":[{"ref":"B","stat":"All Attributes","unit":"flat","range":[21,22]},{"ref":"C","stat":"Chaos Resistance","unit":"percent","range":[24,27]}]},"goalGroups":[{"op":"COUNT","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false},{"ref":"C","min":null,"max":null,"weight":null,"disabled":false}],"min":2,"max":2,"disabled":false},{"op":"AND","rows":[{"ref":"C","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1},"menu":["exalt.C","annul.A","annul.B","chaos.A>C","chaos.B>C"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.C","annul.A","annul.B","chaos.A>C","chaos.B>C"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"chaos.B>C\",\"exalt.C\"]]}","finish_reason":"stop","usage":{"prompt_tokens":550,"completion_tokens":14,"total_tokens":564,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[["chaos.B>C"]],"missingIncludingGoalPrefix":5,"assessment":[{"i":1,"result":"tail","step":1}]}
```

## 호출 46 · count-3 · Amber Amulet · 보강

사전 기대 실행열 6개.

```json
[["annul.A","exalt.C"],["annul.B","exalt.C"],["chaos.A>C"],["chaos.B>C"],["exalt.C","annul.A"],["exalt.C","annul.B"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Amber Amulet","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"A","sourceValue":3,"fractured":false}],"suffix":[{"ref":"B","sourceValue":21,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"A","stat":"Increased maximum Mana","unit":"percent","range":[3,4]}],"suffix":[{"ref":"B","stat":"All Attributes","unit":"flat","range":[21,22]},{"ref":"C","stat":"Chaos Resistance","unit":"percent","range":[24,27]}]},"goalGroups":[{"op":"COUNT","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false},{"ref":"C","min":null,"max":null,"weight":null,"disabled":false}],"min":2,"max":2,"disabled":false},{"op":"AND","rows":[{"ref":"C","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1},"menu":["exalt.C","annul.A","annul.B","chaos.A>C","chaos.B>C"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"COUNT counts enabled rows that are present and in range. Both minimum and maximum are inclusive; compare the count again after each removal or acquisition.","modifierFacts":{"A":{"family":["MaximumManaIncreasePercent"],"tier":3,"requiresItemLevel":33,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[3,4]},"B":{"family":["AllAttributes"],"tier":2,"requiresItemLevel":75,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[21,22]},"C":{"family":["ChaosResistance"],"tier":1,"requiresItemLevel":81,"acquireVia":["exalt.C","chaos.A>C","chaos.B>C"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[24,27]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.C":{"consume":"EXALTED","gain":"C","sourceValue":24},"annul.A":{"consume":"ANNULMENT","remove":"A"},"annul.B":{"consume":"ANNULMENT","remove":"B"},"chaos.A>C":{"consume":"CHAOS","remove":"A","gain":"C","sourceValue":24},"chaos.B>C":{"consume":"CHAOS","remove":"B","gain":"C","sourceValue":24}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.C","annul.A","annul.B","chaos.A>C","chaos.B>C"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.C\"],[\"chaos.A>C\"],[\"chaos.B>C\"],[\"annul.A\",\"chaos.B>C\"],[\"annul.B\",\"chaos.A>C\"]]}","finish_reason":"stop","usage":{"prompt_tokens":845,"completion_tokens":41,"total_tokens":886,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["chaos.A>C"],["chaos.B>C"]],"acceptedIncludingGoalPrefix":[["chaos.A>C"],["chaos.B>C"]],"missingIncludingGoalPrefix":4,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"full","step":1},{"i":3,"result":"full","step":1},{"i":4,"result":"unfinished","step":2},{"i":5,"result":"unfinished","step":2}]}
```

## 호출 47 · w1-3 · Flowing Raiment · 보강

사전 기대 실행열 6개.

```json
[["annul.A","exalt.B.high"],["chaos.A>B","divine.high"],["divine.high","annul.A","exalt.B.high"],["divine.high","exalt.B.high"],["exalt.B.high","annul.A"],["exalt.B.high","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Flowing Raiment","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"A","sourceValue":38,"fractured":false}],"suffix":[]},"optionRefs":{"prefix":[{"ref":"A","stat":"Spirit","unit":"flat","range":[38,42]}],"suffix":[{"ref":"B","stat":"Intelligence","unit":"flat","range":[31,33]}]},"goalGroups":[{"op":"WEIGHTED_V1","rows":[{"ref":"A","min":42,"max":null,"weight":1,"disabled":false},{"ref":"B","min":null,"max":null,"weight":0.5,"disabled":false}],"min":16.5,"max":null,"disabled":false},{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"DIVINE":1,"CHAOS":1},"menu":["exalt.B.high","annul.A","divine.high","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"WEIGHTED_V1: absent rows are optional. Every present enabled row must pass its own range, and the sum of present value times weight must lie in the group range. Weights are score coefficients.","modifierFacts":{"A":{"family":["BaseSpirit"],"tier":3,"requiresItemLevel":33,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[38,42]},"B":{"family":["Intelligence"],"tier":1,"requiresItemLevel":74,"acquireVia":["exalt.B.high","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[31,33]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.B.high":{"consume":"EXALTED","gain":"B","sourceValue":33},"annul.A":{"consume":"ANNULMENT","remove":"A"},"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":31}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B.high","annul.A","divine.high","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B.high\"],[\"chaos.A>B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":739,"completion_tokens":16,"total_tokens":755,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"unfinished","step":1}]}
```

## 호출 48 · w1-3 · Flowing Raiment · 간략

사전 기대 실행열 6개.

```json
[["annul.A","exalt.B.high"],["chaos.A>B","divine.high"],["divine.high","annul.A","exalt.B.high"],["divine.high","exalt.B.high"],["exalt.B.high","annul.A"],["exalt.B.high","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Flowing Raiment","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"A","sourceValue":38,"fractured":false}],"suffix":[]},"optionRefs":{"prefix":[{"ref":"A","stat":"Spirit","unit":"flat","range":[38,42]}],"suffix":[{"ref":"B","stat":"Intelligence","unit":"flat","range":[31,33]}]},"goalGroups":[{"op":"WEIGHTED_V1","rows":[{"ref":"A","min":42,"max":null,"weight":1,"disabled":false},{"ref":"B","min":null,"max":null,"weight":0.5,"disabled":false}],"min":16.5,"max":null,"disabled":false},{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"DIVINE":1,"CHAOS":1},"menu":["exalt.B.high","annul.A","divine.high","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B.high","annul.A","divine.high","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"chaos.A>B\",\"exalt.B.high\"],[\"chaos.A>B\",\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":498,"completion_tokens":25,"total_tokens":523,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["chaos.A>B","divine.high"]],"acceptedIncludingGoalPrefix":[["chaos.A>B","divine.high"]],"missingIncludingGoalPrefix":5,"assessment":[{"i":1,"result":"invalid","step":2},{"i":2,"result":"full","step":2}]}
```

## 호출 49 · w2-3 · Double Belt · 간략

사전 기대 실행열 5개.

```json
[["annul.A","exalt.B.high"],["chaos.A>B","divine.high"],["divine.high","annul.A","exalt.B.high"],["divine.high","exalt.B.high"],["exalt.B.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Double Belt","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":31,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"B","stat":"Maximum Mana","unit":"flat","range":[15,24]}],"suffix":[{"ref":"A","stat":"Fire Resistance","unit":"percent","range":[31,35]}]},"goalGroups":[{"op":"WEIGHTED_V2","rows":[{"ref":"A","min":35,"max":null,"weight":1,"disabled":false},{"ref":"B","min":null,"max":null,"weight":0.5,"disabled":false}],"min":12.0,"max":null,"disabled":false},{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"DIVINE":1,"CHAOS":1},"menu":["exalt.B.high","annul.A","divine.high","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B.high","annul.A","divine.high","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":497,"completion_tokens":10,"total_tokens":507,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.B.high"]],"acceptedIncludingGoalPrefix":[["exalt.B.high"]],"missingIncludingGoalPrefix":4,"assessment":[{"i":1,"result":"full","step":1}]}
```

## 호출 50 · w2-3 · Double Belt · 보강

사전 기대 실행열 5개.

```json
[["annul.A","exalt.B.high"],["chaos.A>B","divine.high"],["divine.high","annul.A","exalt.B.high"],["divine.high","exalt.B.high"],["exalt.B.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Double Belt","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":31,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"B","stat":"Maximum Mana","unit":"flat","range":[15,24]}],"suffix":[{"ref":"A","stat":"Fire Resistance","unit":"percent","range":[31,35]}]},"goalGroups":[{"op":"WEIGHTED_V2","rows":[{"ref":"A","min":35,"max":null,"weight":1,"disabled":false},{"ref":"B","min":null,"max":null,"weight":0.5,"disabled":false}],"min":12.0,"max":null,"disabled":false},{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"DIVINE":1,"CHAOS":1},"menu":["exalt.B.high","annul.A","divine.high","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"WEIGHTED_V2: at least one enabled row must be present. Only present rows inside their own range contribute value times weight. A present out-of-range row adds zero and does not veto the group.","modifierFacts":{"A":{"family":["FireResistance"],"tier":3,"requiresItemLevel":60,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[31,35]},"B":{"family":["IncreasedMana"],"tier":8,"requiresItemLevel":6,"acquireVia":["exalt.B.high","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[15,24]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.B.high":{"consume":"EXALTED","gain":"B","sourceValue":24},"annul.A":{"consume":"ANNULMENT","remove":"A"},"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":15}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B.high","annul.A","divine.high","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B.high\"],[\"chaos.A>B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":739,"completion_tokens":16,"total_tokens":755,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.B.high"]],"acceptedIncludingGoalPrefix":[["exalt.B.high"]],"missingIncludingGoalPrefix":4,"assessment":[{"i":1,"result":"full","step":1},{"i":2,"result":"unfinished","step":1}]}
```

## 호출 51 · pseudo-3 · Blacksteel Crest Shield · 보강

사전 기대 실행열 4개.

```json
[["exalt.A","divine.high","exalt.B"],["exalt.A","exalt.B"],["exalt.B","divine.high","exalt.A"],["exalt.B","exalt.A"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Blacksteel Crest Shield","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Cold Resistance","unit":"percent","range":[6,10]},{"ref":"B","stat":"All Elemental Resistances","unit":"percent","range":[15,16]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"P","min":21,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":2,"DIVINE":1},"menu":["exalt.A","exalt.B","divine.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","derived":{"P":{"label":"Total Cold Resistance","sum":["A","B"],"implicitContribution":0}},"filterRule":"P is a derived display stat. Sum its direct layer-specific contributions once. A missing contribution adds zero. The derived stat is absent if all contributions are absent. P is not an affix to roll.","modifierFacts":{"A":{"family":["ColdResistance"],"tier":8,"requiresItemLevel":1,"acquireVia":["exalt.A"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[6,10]},"B":{"family":["AllResistances"],"tier":2,"requiresItemLevel":68,"acquireVia":["exalt.B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[15,16]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.A":{"consume":"EXALTED","gain":"A","sourceValue":6},"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":15},"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","exalt.B","divine.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.A\",\"exalt.B\",\"divine.high\"],[\"exalt.B\",\"exalt.A\",\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":638,"completion_tokens":30,"total_tokens":668,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[["exalt.A","exalt.B"],["exalt.B","exalt.A"]],"missingIncludingGoalPrefix":2,"assessment":[{"i":1,"result":"tail","step":2},{"i":2,"result":"tail","step":2}]}
```

## 호출 52 · pseudo-3 · Blacksteel Crest Shield · 간략

사전 기대 실행열 4개.

```json
[["exalt.A","divine.high","exalt.B"],["exalt.A","exalt.B"],["exalt.B","divine.high","exalt.A"],["exalt.B","exalt.A"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Blacksteel Crest Shield","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Cold Resistance","unit":"percent","range":[6,10]},{"ref":"B","stat":"All Elemental Resistances","unit":"percent","range":[15,16]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"P","min":21,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":2,"DIVINE":1},"menu":["exalt.A","exalt.B","divine.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","derived":{"P":"Total Cold Resistance"}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","exalt.B","divine.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.A\"],[\"exalt.B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":403,"completion_tokens":14,"total_tokens":417,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":4,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"unfinished","step":1}]}
```

## 호출 53 · unit-3 · Austere Garb · 간략

사전 기대 실행열 2개.

```json
[["divine.high","exalt.B"],["exalt.B","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Austere Garb","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":1386,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Life Regeneration per second","unit":"flat","range":[1386,1740]},{"ref":"B","stat":"Lightning Resistance","unit":"percent","range":[16,20]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":29.0,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"DIVINE":1,"EXALTED":1},"menu":["divine.high","exalt.B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.high","exalt.B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":429,"completion_tokens":9,"total_tokens":438,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":2,"assessment":[{"i":1,"result":"unfinished","step":1}]}
```

## 호출 54 · unit-3 · Austere Garb · 보강

사전 기대 실행열 2개.

```json
[["divine.high","exalt.B"],["exalt.B","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Austere Garb","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":1386,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Life Regeneration per second","unit":"flat","range":[1386,1740]},{"ref":"B","stat":"Lightning Resistance","unit":"percent","range":[16,20]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":29.0,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"DIVINE":1,"EXALTED":1},"menu":["divine.high","exalt.B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"The action rolls source integers; compare goal ranges in display units after dividing by the stat-specific divisor. Do not round a near miss into a match.","modifierFacts":{"A":{"family":["LifeRegeneration"],"tier":2,"requiresItemLevel":68,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":60,"sourceRange":[1386,1740]},"B":{"family":["LightningResistance"],"tier":6,"requiresItemLevel":25,"acquireVia":["exalt.B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[16,20]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"},"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":16}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.high","exalt.B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B\",\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":616,"completion_tokens":13,"total_tokens":629,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.B","divine.high"]],"acceptedIncludingGoalPrefix":[["exalt.B","divine.high"]],"missingIncludingGoalPrefix":1,"assessment":[{"i":1,"result":"full","step":2}]}
```

## 호출 55 · disabled-3 · Wolfskin Mantle · 보강

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["annul.A","exalt.C","exalt.B"],["chaos.A>B"],["exalt.B"],["exalt.C","annul.A","exalt.B"],["exalt.C","chaos.A>B"],["exalt.C","exalt.B"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Wolfskin Mantle","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"A","sourceValue":34,"fractured":false}],"suffix":[]},"optionRefs":{"prefix":[{"ref":"A","stat":"Spirit","unit":"flat","range":[34,37]}],"suffix":[{"ref":"B","stat":"Fire Resistance","unit":"percent","range":[16,20]},{"ref":"C","stat":"Chaos Resistance","unit":"percent","range":[12,15]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false},{"ref":"C","min":null,"max":null,"weight":null,"disabled":true}],"min":null,"max":null,"disabled":false},{"op":"NOT","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":true}],"stock":{"EXALTED":2,"ANNULMENT":1,"CHAOS":1},"menu":["exalt.B","exalt.C","annul.A","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"Disabled groups and rows are ignored when evaluating the goal. Their modifiers may remain. A legal intermediate step is allowed if the goal has not yet been met.","modifierFacts":{"A":{"family":["BaseSpirit"],"tier":4,"requiresItemLevel":25,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[34,37]},"B":{"family":["FireResistance"],"tier":6,"requiresItemLevel":24,"acquireVia":["exalt.B","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[16,20]},"C":{"family":["ChaosResistance"],"tier":4,"requiresItemLevel":44,"acquireVia":["exalt.C"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[12,15]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":16},"exalt.C":{"consume":"EXALTED","gain":"C","sourceValue":12},"annul.A":{"consume":"ANNULMENT","remove":"A"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":16}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","exalt.C","annul.A","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"chaos.A>B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":779,"completion_tokens":10,"total_tokens":789,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["chaos.A>B"]],"acceptedIncludingGoalPrefix":[["chaos.A>B"]],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"full","step":1}]}
```

## 호출 56 · disabled-3 · Wolfskin Mantle · 간략

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["annul.A","exalt.C","exalt.B"],["chaos.A>B"],["exalt.B"],["exalt.C","annul.A","exalt.B"],["exalt.C","chaos.A>B"],["exalt.C","exalt.B"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Wolfskin Mantle","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"A","sourceValue":34,"fractured":false}],"suffix":[]},"optionRefs":{"prefix":[{"ref":"A","stat":"Spirit","unit":"flat","range":[34,37]}],"suffix":[{"ref":"B","stat":"Fire Resistance","unit":"percent","range":[16,20]},{"ref":"C","stat":"Chaos Resistance","unit":"percent","range":[12,15]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false},{"ref":"C","min":null,"max":null,"weight":null,"disabled":true}],"min":null,"max":null,"disabled":false},{"op":"NOT","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":true}],"stock":{"EXALTED":2,"ANNULMENT":1,"CHAOS":1},"menu":["exalt.B","exalt.C","annul.A","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","exalt.C","annul.A","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":503,"completion_tokens":9,"total_tokens":512,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.B"]],"acceptedIncludingGoalPrefix":[["exalt.B"]],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"full","step":1}]}
```

## 호출 57 · floor-3 · Bone Wand · 간략

사전 기대 실행열 2개.

```json
[["exalt.A","perfectExalt.B"],["perfectExalt.B","exalt.A"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Bone Wand","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Mana gained per enemy killed","unit":"flat","range":[4,5]},{"ref":"B","stat":"Intelligence","unit":"flat","range":[31,33]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"PERFECT_EXALTED":1},"menu":["exalt.A","perfectExalt.A","exalt.B","perfectExalt.B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","perfectExalt.A","exalt.B","perfectExalt.B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.A\",\"exalt.B\"],[\"exalt.B\",\"exalt.A\"],[\"perfectExalt.A\",\"exalt.B\"],[\"perfectExalt.B\",\"exalt.A\"],[\"exalt.A\",\"perfectExalt.B\"],[\"exalt.B\",\"perfectExalt.A\"],[\"perfectExalt.A\",\"perfectExalt.B\"],[\"perfectExalt.B\",\"perfectExalt.A\"]]}","finish_reason":"stop","usage":{"prompt_tokens":430,"completion_tokens":84,"total_tokens":514,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.A","perfectExalt.B"],["perfectExalt.B","exalt.A"]],"acceptedIncludingGoalPrefix":[["exalt.A","perfectExalt.B"],["perfectExalt.B","exalt.A"]],"missingIncludingGoalPrefix":0,"assessment":[{"i":1,"result":"invalid","step":2},{"i":2,"result":"invalid","step":2},{"i":3,"result":"invalid","step":1},{"i":4,"result":"full","step":2},{"i":5,"result":"full","step":2},{"i":6,"result":"invalid","step":2},{"i":7,"result":"invalid","step":1},{"i":8,"result":"invalid","step":2}]}
```

## 호출 58 · floor-3 · Bone Wand · 보강

사전 기대 실행열 2개.

```json
[["exalt.A","perfectExalt.B"],["perfectExalt.B","exalt.A"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Bone Wand","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Mana gained per enemy killed","unit":"flat","range":[4,5]},{"ref":"B","stat":"Intelligence","unit":"flat","range":[31,33]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"PERFECT_EXALTED":1},"menu":["exalt.A","perfectExalt.A","exalt.B","perfectExalt.B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"Perfect Exalted has minimum modifier level 50. The highest eligible modifier level in each side/family is also allowed below the floor. Ordinary Exalted uses the ordinary eligible pool.","modifierFacts":{"A":{"family":["ManaGainedFromEnemyDeath"],"tier":7,"requiresItemLevel":12,"acquireVia":["exalt.A","perfectExalt.A"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[4,5],"highestEligibleLevelInFamilyAtStart":78},"B":{"family":["Intelligence"],"tier":1,"requiresItemLevel":74,"acquireVia":["exalt.B","perfectExalt.B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[31,33],"highestEligibleLevelInFamilyAtStart":74}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.A":{"consume":"EXALTED","gain":"A","sourceValue":4},"perfectExalt.A":{"consume":"PERFECT_EXALTED","gain":"A","sourceValue":4},"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":31},"perfectExalt.B":{"consume":"PERFECT_EXALTED","gain":"B","sourceValue":31}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","perfectExalt.A","exalt.B","perfectExalt.B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.A\",\"exalt.B\"],[\"exalt.B\",\"exalt.A\"],[\"perfectExalt.A\",\"exalt.B\"],[\"perfectExalt.B\",\"exalt.A\"],[\"exalt.A\",\"perfectExalt.B\"],[\"exalt.B\",\"perfectExalt.A\"],[\"perfectExalt.A\",\"perfectExalt.B\"],[\"perfectExalt.B\",\"perfectExalt.A\"]]}","finish_reason":"stop","usage":{"prompt_tokens":700,"completion_tokens":84,"total_tokens":784,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.A","perfectExalt.B"],["perfectExalt.B","exalt.A"]],"acceptedIncludingGoalPrefix":[["exalt.A","perfectExalt.B"],["perfectExalt.B","exalt.A"]],"missingIncludingGoalPrefix":0,"assessment":[{"i":1,"result":"invalid","step":2},{"i":2,"result":"invalid","step":2},{"i":3,"result":"invalid","step":1},{"i":4,"result":"full","step":2},{"i":5,"result":"full","step":2},{"i":6,"result":"invalid","step":2},{"i":7,"result":"invalid","step":1},{"i":8,"result":"invalid","step":2}]}
```

## 호출 59 · locked-3 · Lunar Amulet · 보강

사전 기대 실행열 1개.

```json
[["divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Lunar Amulet","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":15,"fractured":true},{"ref":"B","sourceValue":17,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Increased Rarity of Items found","unit":"percent","range":[15,18]},{"ref":"B","stat":"Strength","unit":"flat","range":[17,20]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":15,"max":15,"weight":null,"disabled":false},{"ref":"B","min":20,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"DIVINE":1,"ANNULMENT":1,"CHAOS":1},"menu":["divine.high","annul.A","annul.B","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"A fractured explicit remains at its original value during Divine rerolls and cannot be removed by annul or chaos. Other explicit values can still change.","modifierFacts":{"A":{"family":["ItemFoundRarityIncrease"],"tier":1,"requiresItemLevel":40,"acquireVia":[],"preservedByFracture":true,"sourceDivisor":1,"sourceRange":[15,18]},"B":{"family":["Strength"],"tier":5,"requiresItemLevel":33,"acquireVia":["chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[17,20]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"},"annul.A":{"consume":"ANNULMENT","remove":"A"},"annul.B":{"consume":"ANNULMENT","remove":"B"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":17}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.high","annul.A","annul.B","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"chaos.A>B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":680,"completion_tokens":10,"total_tokens":690,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":1,"assessment":[{"i":1,"result":"invalid","step":1}]}
```

## 호출 60 · locked-3 · Lunar Amulet · 간략

사전 기대 실행열 1개.

```json
[["divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Lunar Amulet","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":15,"fractured":true},{"ref":"B","sourceValue":17,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Increased Rarity of Items found","unit":"percent","range":[15,18]},{"ref":"B","stat":"Strength","unit":"flat","range":[17,20]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":15,"max":15,"weight":null,"disabled":false},{"ref":"B","min":20,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"DIVINE":1,"ANNULMENT":1,"CHAOS":1},"menu":["divine.high","annul.A","annul.B","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.high","annul.A","annul.B","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"annul.B\",\"chaos.A>B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":461,"completion_tokens":14,"total_tokens":475,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":1,"assessment":[{"i":1,"result":"invalid","step":2}]}
```

## 호출 61 · if-4 · Sacramental Robe · 보강

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["chaos.A>B"],["divine.high","annul.A","exalt.B"],["divine.high","chaos.A>B"],["divine.high","exalt.B"],["exalt.B","annul.A"],["exalt.B","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Sacramental Robe","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"A","sourceValue":30,"fractured":false}],"suffix":[]},"optionRefs":{"prefix":[{"ref":"A","stat":"Maximum Life","unit":"flat","range":[30,39]}],"suffix":[{"ref":"B","stat":"Lightning Resistance","unit":"percent","range":[21,25]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false},{"op":"IF","rows":[{"ref":"A","min":39,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1,"DIVINE":1},"menu":["exalt.B","annul.A","chaos.A>B","divine.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"IF: an absent row passes. A present row must satisfy its range. Every active group must pass.","modifierFacts":{"A":{"family":["IncreasedLife"],"tier":7,"requiresItemLevel":16,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[30,39]},"B":{"family":["LightningResistance"],"tier":5,"requiresItemLevel":37,"acquireVia":["exalt.B","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[21,25]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":21},"annul.A":{"consume":"ANNULMENT","remove":"A"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":21},"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","annul.A","chaos.A>B","divine.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B\",\"divine.high\"],[\"chaos.A>B\",\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":688,"completion_tokens":23,"total_tokens":711,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.B","divine.high"]],"acceptedIncludingGoalPrefix":[["chaos.A>B"],["exalt.B","divine.high"]],"missingIncludingGoalPrefix":5,"assessment":[{"i":1,"result":"full","step":2},{"i":2,"result":"tail","step":1}]}
```

## 호출 62 · if-4 · Sacramental Robe · 간략

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["chaos.A>B"],["divine.high","annul.A","exalt.B"],["divine.high","chaos.A>B"],["divine.high","exalt.B"],["exalt.B","annul.A"],["exalt.B","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Sacramental Robe","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"A","sourceValue":30,"fractured":false}],"suffix":[]},"optionRefs":{"prefix":[{"ref":"A","stat":"Maximum Life","unit":"flat","range":[30,39]}],"suffix":[{"ref":"B","stat":"Lightning Resistance","unit":"percent","range":[21,25]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false},{"op":"IF","rows":[{"ref":"A","min":39,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1,"DIVINE":1},"menu":["exalt.B","annul.A","chaos.A>B","divine.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","annul.A","chaos.A>B","divine.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B\",\"chaos.A>B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":470,"completion_tokens":14,"total_tokens":484,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":7,"assessment":[{"i":1,"result":"invalid","step":2}]}
```

## 호출 63 · not-4 · Hallowed Sceptre · 간략

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["chaos.A>B"],["divine.high","annul.A","exalt.B"],["divine.high","chaos.A>B"],["divine.high","exalt.B"],["exalt.B","annul.A"],["exalt.B","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Hallowed Sceptre","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":25,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Intelligence","unit":"flat","range":[25,27]},{"ref":"B","stat":"Strength","unit":"flat","range":[28,30]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false},{"op":"NOT","rows":[{"ref":"A","min":null,"max":25,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1,"DIVINE":1},"menu":["exalt.B","annul.A","chaos.A>B","divine.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","annul.A","chaos.A>B","divine.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B\"],[\"chaos.A>B\",\"exalt.B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":469,"completion_tokens":19,"total_tokens":488,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[["chaos.A>B"]],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"tail","step":1}]}
```

## 호출 64 · not-4 · Hallowed Sceptre · 보강

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["chaos.A>B"],["divine.high","annul.A","exalt.B"],["divine.high","chaos.A>B"],["divine.high","exalt.B"],["exalt.B","annul.A"],["exalt.B","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Hallowed Sceptre","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":25,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Intelligence","unit":"flat","range":[25,27]},{"ref":"B","stat":"Strength","unit":"flat","range":[28,30]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false},{"op":"NOT","rows":[{"ref":"A","min":null,"max":25,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1,"DIVINE":1},"menu":["exalt.B","annul.A","chaos.A>B","divine.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"NOT matches if none of its enabled rows are both present and in range. A present value outside the forbidden interval can remain.","modifierFacts":{"A":{"family":["Intelligence"],"tier":3,"requiresItemLevel":55,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[25,27]},"B":{"family":["Strength"],"tier":2,"requiresItemLevel":66,"acquireVia":["exalt.B","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[28,30]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":28},"annul.A":{"consume":"ANNULMENT","remove":"A"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":28},"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","annul.A","chaos.A>B","divine.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"chaos.A>B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":689,"completion_tokens":10,"total_tokens":699,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["chaos.A>B"]],"acceptedIncludingGoalPrefix":[["chaos.A>B"]],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"full","step":1}]}
```

## 호출 65 · count-4 · Fine Belt · 보강

사전 기대 실행열 8개.

```json
[["annul.A","chaos.B>C"],["annul.A","exalt.C"],["annul.B","chaos.A>C"],["annul.B","exalt.C"],["chaos.A>C"],["chaos.B>C"],["exalt.C","annul.A"],["exalt.C","annul.B"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Fine Belt","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"A","sourceValue":60,"fractured":false}],"suffix":[{"ref":"B","sourceValue":16,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"A","stat":"Maximum Life","unit":"flat","range":[60,69]},{"ref":"C","stat":"Maximum Mana","unit":"flat","range":[65,79]}],"suffix":[{"ref":"B","stat":"Cold Resistance","unit":"percent","range":[16,20]}]},"goalGroups":[{"op":"COUNT","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false},{"ref":"C","min":null,"max":null,"weight":null,"disabled":false}],"min":1,"max":2,"disabled":false},{"op":"AND","rows":[{"ref":"C","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1},"menu":["exalt.C","annul.A","annul.B","chaos.A>C","chaos.B>C"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"COUNT counts enabled rows that are present and in range. Both minimum and maximum are inclusive; compare the count again after each removal or acquisition.","modifierFacts":{"A":{"family":["IncreasedLife"],"tier":6,"requiresItemLevel":33,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[60,69]},"B":{"family":["ColdResistance"],"tier":6,"requiresItemLevel":26,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[16,20]},"C":{"family":["IncreasedMana"],"tier":4,"requiresItemLevel":38,"acquireVia":["exalt.C","chaos.A>C","chaos.B>C"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[65,79]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.C":{"consume":"EXALTED","gain":"C","sourceValue":65},"annul.A":{"consume":"ANNULMENT","remove":"A"},"annul.B":{"consume":"ANNULMENT","remove":"B"},"chaos.A>C":{"consume":"CHAOS","remove":"A","gain":"C","sourceValue":65},"chaos.B>C":{"consume":"CHAOS","remove":"B","gain":"C","sourceValue":65}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.C","annul.A","annul.B","chaos.A>C","chaos.B>C"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.C\"],[\"annul.A\",\"chaos.A>C\"],[\"annul.B\",\"chaos.B>C\"]]}","finish_reason":"stop","usage":{"prompt_tokens":842,"completion_tokens":29,"total_tokens":871,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":8,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"invalid","step":2},{"i":3,"result":"invalid","step":2}]}
```

## 호출 66 · count-4 · Fine Belt · 간략

사전 기대 실행열 8개.

```json
[["annul.A","chaos.B>C"],["annul.A","exalt.C"],["annul.B","chaos.A>C"],["annul.B","exalt.C"],["chaos.A>C"],["chaos.B>C"],["exalt.C","annul.A"],["exalt.C","annul.B"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Fine Belt","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"A","sourceValue":60,"fractured":false}],"suffix":[{"ref":"B","sourceValue":16,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"A","stat":"Maximum Life","unit":"flat","range":[60,69]},{"ref":"C","stat":"Maximum Mana","unit":"flat","range":[65,79]}],"suffix":[{"ref":"B","stat":"Cold Resistance","unit":"percent","range":[16,20]}]},"goalGroups":[{"op":"COUNT","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false},{"ref":"C","min":null,"max":null,"weight":null,"disabled":false}],"min":1,"max":2,"disabled":false},{"op":"AND","rows":[{"ref":"C","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1},"menu":["exalt.C","annul.A","annul.B","chaos.A>C","chaos.B>C"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.C","annul.A","annul.B","chaos.A>C","chaos.B>C"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"chaos.A>C\"]]}","finish_reason":"stop","usage":{"prompt_tokens":547,"completion_tokens":10,"total_tokens":557,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["chaos.A>C"]],"acceptedIncludingGoalPrefix":[["chaos.A>C"]],"missingIncludingGoalPrefix":7,"assessment":[{"i":1,"result":"full","step":1}]}
```

## 호출 67 · w1-4 · Tawhoan Tower Shield · 간략

사전 기대 실행열 6개.

```json
[["annul.A","exalt.B.high"],["chaos.A>B","divine.high"],["divine.high","annul.A","exalt.B.high"],["divine.high","exalt.B.high"],["exalt.B.high","annul.A"],["exalt.B.high","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Tawhoan Tower Shield","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":17,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"B","stat":"Maximum Life","unit":"flat","range":[100,119]}],"suffix":[{"ref":"A","stat":"Strength","unit":"flat","range":[17,20]}]},"goalGroups":[{"op":"WEIGHTED_V1","rows":[{"ref":"A","min":20,"max":null,"weight":1,"disabled":false},{"ref":"B","min":null,"max":null,"weight":2,"disabled":false}],"min":238,"max":null,"disabled":false},{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"DIVINE":1,"CHAOS":1},"menu":["exalt.B.high","annul.A","divine.high","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B.high","annul.A","divine.high","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B.high\"],[\"chaos.A>B\"],[\"annul.A\"]]}","finish_reason":"stop","usage":{"prompt_tokens":496,"completion_tokens":21,"total_tokens":517,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"unfinished","step":1},{"i":3,"result":"unfinished","step":1}]}
```

## 호출 68 · w1-4 · Tawhoan Tower Shield · 보강

사전 기대 실행열 6개.

```json
[["annul.A","exalt.B.high"],["chaos.A>B","divine.high"],["divine.high","annul.A","exalt.B.high"],["divine.high","exalt.B.high"],["exalt.B.high","annul.A"],["exalt.B.high","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Tawhoan Tower Shield","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":17,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"B","stat":"Maximum Life","unit":"flat","range":[100,119]}],"suffix":[{"ref":"A","stat":"Strength","unit":"flat","range":[17,20]}]},"goalGroups":[{"op":"WEIGHTED_V1","rows":[{"ref":"A","min":20,"max":null,"weight":1,"disabled":false},{"ref":"B","min":null,"max":null,"weight":2,"disabled":false}],"min":238,"max":null,"disabled":false},{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"DIVINE":1,"CHAOS":1},"menu":["exalt.B.high","annul.A","divine.high","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"WEIGHTED_V1: absent rows are optional. Every present enabled row must pass its own range, and the sum of present value times weight must lie in the group range. Weights are score coefficients.","modifierFacts":{"A":{"family":["Strength"],"tier":5,"requiresItemLevel":33,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[17,20]},"B":{"family":["IncreasedLife"],"tier":4,"requiresItemLevel":54,"acquireVia":["exalt.B.high","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[100,119]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.B.high":{"consume":"EXALTED","gain":"B","sourceValue":119},"annul.A":{"consume":"ANNULMENT","remove":"A"},"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":100}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B.high","annul.A","divine.high","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B.high\"],[\"chaos.A>B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":737,"completion_tokens":16,"total_tokens":753,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"unfinished","step":1}]}
```

## 호출 69 · w2-4 · Grinning Mask · 보강

사전 기대 실행열 5개.

```json
[["annul.A","exalt.B.high"],["chaos.A>B","divine.high"],["divine.high","annul.A","exalt.B.high"],["divine.high","exalt.B.high"],["exalt.B.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Grinning Mask","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":21,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Intelligence","unit":"flat","range":[21,24]},{"ref":"B","stat":"Lightning Resistance","unit":"percent","range":[31,35]}]},"goalGroups":[{"op":"WEIGHTED_V2","rows":[{"ref":"A","min":24,"max":null,"weight":1,"disabled":false},{"ref":"B","min":null,"max":null,"weight":2,"disabled":false}],"min":70,"max":null,"disabled":false},{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"DIVINE":1,"CHAOS":1},"menu":["exalt.B.high","annul.A","divine.high","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"WEIGHTED_V2: at least one enabled row must be present. Only present rows inside their own range contribute value times weight. A present out-of-range row adds zero and does not veto the group.","modifierFacts":{"A":{"family":["Intelligence"],"tier":4,"requiresItemLevel":44,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[21,24]},"B":{"family":["LightningResistance"],"tier":3,"requiresItemLevel":60,"acquireVia":["exalt.B.high","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[31,35]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.B.high":{"consume":"EXALTED","gain":"B","sourceValue":35},"annul.A":{"consume":"ANNULMENT","remove":"A"},"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":31}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B.high","annul.A","divine.high","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B.high\"],[\"chaos.A>B\"],[\"annul.A\"]]}","finish_reason":"stop","usage":{"prompt_tokens":735,"completion_tokens":21,"total_tokens":756,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.B.high"]],"acceptedIncludingGoalPrefix":[["exalt.B.high"]],"missingIncludingGoalPrefix":4,"assessment":[{"i":1,"result":"full","step":1},{"i":2,"result":"unfinished","step":1},{"i":3,"result":"unfinished","step":1}]}
```

## 호출 70 · w2-4 · Grinning Mask · 간략

사전 기대 실행열 5개.

```json
[["annul.A","exalt.B.high"],["chaos.A>B","divine.high"],["divine.high","annul.A","exalt.B.high"],["divine.high","exalt.B.high"],["exalt.B.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Grinning Mask","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":21,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Intelligence","unit":"flat","range":[21,24]},{"ref":"B","stat":"Lightning Resistance","unit":"percent","range":[31,35]}]},"goalGroups":[{"op":"WEIGHTED_V2","rows":[{"ref":"A","min":24,"max":null,"weight":1,"disabled":false},{"ref":"B","min":null,"max":null,"weight":2,"disabled":false}],"min":70,"max":null,"disabled":false},{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"DIVINE":1,"CHAOS":1},"menu":["exalt.B.high","annul.A","divine.high","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B.high","annul.A","divine.high","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"chaos.A>B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":494,"completion_tokens":10,"total_tokens":504,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":5,"assessment":[{"i":1,"result":"unfinished","step":1}]}
```

## 호출 71 · pseudo-4 · Pearl Ring · 간략

사전 기대 실행열 3개.

```json
[["exalt.A"],["exalt.B","divine.high","exalt.A"],["exalt.B","exalt.A"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Pearl Ring","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Cold Resistance","unit":"percent","range":[36,40]},{"ref":"B","stat":"All Elemental Resistances","unit":"percent","range":[9,11]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"P","min":36,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":2,"DIVINE":1},"menu":["exalt.A","exalt.B","divine.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","derived":{"P":"Total Cold Resistance"}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","exalt.B","divine.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.A\"],[\"exalt.B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":402,"completion_tokens":14,"total_tokens":416,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.A"]],"acceptedIncludingGoalPrefix":[["exalt.A"]],"missingIncludingGoalPrefix":2,"assessment":[{"i":1,"result":"full","step":1},{"i":2,"result":"unfinished","step":1}]}
```

## 호출 72 · pseudo-4 · Pearl Ring · 보강

사전 기대 실행열 3개.

```json
[["exalt.A"],["exalt.B","divine.high","exalt.A"],["exalt.B","exalt.A"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Pearl Ring","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Cold Resistance","unit":"percent","range":[36,40]},{"ref":"B","stat":"All Elemental Resistances","unit":"percent","range":[9,11]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"P","min":36,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":2,"DIVINE":1},"menu":["exalt.A","exalt.B","divine.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","derived":{"P":{"label":"Total Cold Resistance","sum":["A","B"],"implicitContribution":0}},"filterRule":"P is a derived display stat. Sum its direct layer-specific contributions once. A missing contribution adds zero. The derived stat is absent if all contributions are absent. P is not an affix to roll.","modifierFacts":{"A":{"family":["ColdResistance"],"tier":2,"requiresItemLevel":71,"acquireVia":["exalt.A"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[36,40]},"B":{"family":["AllResistances"],"tier":3,"requiresItemLevel":40,"acquireVia":["exalt.B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[9,11]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.A":{"consume":"EXALTED","gain":"A","sourceValue":36},"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":9},"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","exalt.B","divine.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.A\"],[\"exalt.B\",\"exalt.A\"],[\"exalt.A\",\"exalt.B\"],[\"exalt.B\",\"divine.high\",\"exalt.A\"],[\"exalt.A\",\"divine.high\"],[\"exalt.B\",\"exalt.A\",\"divine.high\"],[\"exalt.A\",\"exalt.B\",\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":637,"completion_tokens":75,"total_tokens":712,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.A"],["exalt.B","divine.high","exalt.A"],["exalt.B","exalt.A"]],"acceptedIncludingGoalPrefix":[["exalt.A"],["exalt.B","divine.high","exalt.A"],["exalt.B","exalt.A"]],"missingIncludingGoalPrefix":0,"assessment":[{"i":1,"result":"full","step":1},{"i":2,"result":"full","step":2},{"i":3,"result":"tail","step":1},{"i":4,"result":"full","step":3},{"i":5,"result":"tail","step":1},{"i":6,"result":"tail","step":2},{"i":7,"result":"tail","step":1}]}
```

## 호출 73 · unit-4 · Amethyst Ring · 보강

사전 기대 실행열 6개.

```json
[["exalt.A","divine.high","exalt.B"],["exalt.A","exalt.B","divine.high"],["exalt.A.high","exalt.B"],["exalt.B","divine.high","exalt.A.high"],["exalt.B","exalt.A","divine.high"],["exalt.B","exalt.A.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Amethyst Ring","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"optionRefs":{"prefix":[{"ref":"B","stat":"Increased Chaos Damage","unit":"percent","range":[27,30]}],"suffix":[{"ref":"A","stat":"Physical Attack Damage leeched as Mana","unit":"percent","range":[600,690]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":6.9,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"DIVINE":1,"EXALTED":2},"menu":["divine.high","exalt.B","exalt.A","exalt.A.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"The action rolls source integers; compare goal ranges in display units after dividing by the stat-specific divisor. Do not round a near miss into a match.","modifierFacts":{"A":{"family":["ManaLeech"],"tier":1,"requiresItemLevel":38,"acquireVia":["exalt.A","exalt.A.high"],"preservedByFracture":false,"sourceDivisor":100,"sourceRange":[600,690]},"B":{"family":["IncreasedChaosDamage"],"tier":1,"requiresItemLevel":75,"acquireVia":["exalt.B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[27,30]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"},"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":27},"exalt.A":{"consume":"EXALTED","gain":"A","sourceValue":600},"exalt.A.high":{"consume":"EXALTED","gain":"A","sourceValue":690}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.high","exalt.B","exalt.A","exalt.A.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B\",\"exalt.A.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":666,"completion_tokens":14,"total_tokens":680,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.B","exalt.A.high"]],"acceptedIncludingGoalPrefix":[["exalt.B","exalt.A.high"]],"missingIncludingGoalPrefix":5,"assessment":[{"i":1,"result":"full","step":2}]}
```

## 호출 74 · unit-4 · Amethyst Ring · 간략

사전 기대 실행열 6개.

```json
[["exalt.A","divine.high","exalt.B"],["exalt.A","exalt.B","divine.high"],["exalt.A.high","exalt.B"],["exalt.B","divine.high","exalt.A.high"],["exalt.B","exalt.A","divine.high"],["exalt.B","exalt.A.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Amethyst Ring","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"optionRefs":{"prefix":[{"ref":"B","stat":"Increased Chaos Damage","unit":"percent","range":[27,30]}],"suffix":[{"ref":"A","stat":"Physical Attack Damage leeched as Mana","unit":"percent","range":[600,690]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":6.9,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"DIVINE":1,"EXALTED":2},"menu":["divine.high","exalt.B","exalt.A","exalt.A.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.high","exalt.B","exalt.A","exalt.A.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B\",\"divine.high\"],[\"exalt.A\",\"divine.high\"],[\"exalt.A.high\",\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":431,"completion_tokens":32,"total_tokens":463,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"unfinished","step":2},{"i":2,"result":"unfinished","step":2},{"i":3,"result":"invalid","step":2}]}
```

## 호출 75 · disabled-4 · Linen Belt · 간략

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["annul.A","exalt.C","exalt.B"],["chaos.A>B"],["exalt.B"],["exalt.C","annul.A","exalt.B"],["exalt.C","chaos.A>B"],["exalt.C","exalt.B"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Linen Belt","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":28,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"C","stat":"Maximum Mana","unit":"flat","range":[10,14]}],"suffix":[{"ref":"A","stat":"Strength","unit":"flat","range":[28,30]},{"ref":"B","stat":"Fire Resistance","unit":"percent","range":[11,15]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false},{"ref":"C","min":null,"max":null,"weight":null,"disabled":true}],"min":null,"max":null,"disabled":false},{"op":"NOT","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":true}],"stock":{"EXALTED":2,"ANNULMENT":1,"CHAOS":1},"menu":["exalt.B","exalt.C","annul.A","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","exalt.C","annul.A","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B\"],[\"exalt.C\"],[\"annul.A\",\"chaos.A>B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":502,"completion_tokens":24,"total_tokens":526,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.B"]],"acceptedIncludingGoalPrefix":[["exalt.B"]],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"full","step":1},{"i":2,"result":"unfinished","step":1},{"i":3,"result":"invalid","step":2}]}
```

## 호출 76 · disabled-4 · Linen Belt · 보강

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["annul.A","exalt.C","exalt.B"],["chaos.A>B"],["exalt.B"],["exalt.C","annul.A","exalt.B"],["exalt.C","chaos.A>B"],["exalt.C","exalt.B"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Linen Belt","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":28,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"C","stat":"Maximum Mana","unit":"flat","range":[10,14]}],"suffix":[{"ref":"A","stat":"Strength","unit":"flat","range":[28,30]},{"ref":"B","stat":"Fire Resistance","unit":"percent","range":[11,15]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false},{"ref":"C","min":null,"max":null,"weight":null,"disabled":true}],"min":null,"max":null,"disabled":false},{"op":"NOT","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":true}],"stock":{"EXALTED":2,"ANNULMENT":1,"CHAOS":1},"menu":["exalt.B","exalt.C","annul.A","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"Disabled groups and rows are ignored when evaluating the goal. Their modifiers may remain. A legal intermediate step is allowed if the goal has not yet been met.","modifierFacts":{"A":{"family":["Strength"],"tier":3,"requiresItemLevel":66,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[28,30]},"B":{"family":["FireResistance"],"tier":7,"requiresItemLevel":12,"acquireVia":["exalt.B","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[11,15]},"C":{"family":["IncreasedMana"],"tier":9,"requiresItemLevel":1,"acquireVia":["exalt.C"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[10,14]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":11},"exalt.C":{"consume":"EXALTED","gain":"C","sourceValue":10},"annul.A":{"consume":"ANNULMENT","remove":"A"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":11}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","exalt.C","annul.A","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"chaos.A>B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":778,"completion_tokens":10,"total_tokens":788,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["chaos.A>B"]],"acceptedIncludingGoalPrefix":[["chaos.A>B"]],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"full","step":1}]}
```

## 호출 77 · floor-4 · Ancestral Tiara · 보강

사전 기대 실행열 4개.

```json
[["exalt.A","exalt.B"],["exalt.A","perfectExalt.B"],["exalt.B","exalt.A"],["perfectExalt.B","exalt.A"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Ancestral Tiara","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"optionRefs":{"prefix":[{"ref":"A","stat":"Accuracy Rating","unit":"flat","range":[11,32]}],"suffix":[{"ref":"B","stat":"Increased Rarity of Items found","unit":"percent","range":[15,18]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":2,"PERFECT_EXALTED":1},"menu":["exalt.A","perfectExalt.A","exalt.B","perfectExalt.B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"Perfect Exalted has minimum modifier level 50. The highest eligible modifier level in each side/family is also allowed below the floor. Ordinary Exalted uses the ordinary eligible pool.","modifierFacts":{"A":{"family":["IncreasedAccuracy"],"tier":9,"requiresItemLevel":1,"acquireVia":["exalt.A","perfectExalt.A"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[11,32],"highestEligibleLevelInFamilyAtStart":76},"B":{"family":["ItemFoundRarityIncrease"],"tier":1,"requiresItemLevel":40,"acquireVia":["exalt.B","perfectExalt.B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[15,18],"highestEligibleLevelInFamilyAtStart":40}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.A":{"consume":"EXALTED","gain":"A","sourceValue":11},"perfectExalt.A":{"consume":"PERFECT_EXALTED","gain":"A","sourceValue":11},"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":15},"perfectExalt.B":{"consume":"PERFECT_EXALTED","gain":"B","sourceValue":15}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","perfectExalt.A","exalt.B","perfectExalt.B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"perfectExalt.A\",\"exalt.B\"],[\"exalt.A\",\"exalt.B\"],[\"perfectExalt.B\",\"exalt.A\"],[\"exalt.B\",\"exalt.A\"]]}","finish_reason":"stop","usage":{"prompt_tokens":705,"completion_tokens":42,"total_tokens":747,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.A","exalt.B"],["exalt.B","exalt.A"],["perfectExalt.B","exalt.A"]],"acceptedIncludingGoalPrefix":[["exalt.A","exalt.B"],["exalt.B","exalt.A"],["perfectExalt.B","exalt.A"]],"missingIncludingGoalPrefix":1,"assessment":[{"i":1,"result":"invalid","step":1},{"i":2,"result":"full","step":2},{"i":3,"result":"full","step":2},{"i":4,"result":"full","step":2}]}
```

## 호출 78 · floor-4 · Ancestral Tiara · 간략

사전 기대 실행열 4개.

```json
[["exalt.A","exalt.B"],["exalt.A","perfectExalt.B"],["exalt.B","exalt.A"],["perfectExalt.B","exalt.A"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Ancestral Tiara","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"optionRefs":{"prefix":[{"ref":"A","stat":"Accuracy Rating","unit":"flat","range":[11,32]}],"suffix":[{"ref":"B","stat":"Increased Rarity of Items found","unit":"percent","range":[15,18]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":2,"PERFECT_EXALTED":1},"menu":["exalt.A","perfectExalt.A","exalt.B","perfectExalt.B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","perfectExalt.A","exalt.B","perfectExalt.B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.A\",\"exalt.B\"],[\"exalt.B\",\"exalt.A\"],[\"perfectExalt.A\",\"exalt.B\"],[\"perfectExalt.B\",\"exalt.A\"]]}","finish_reason":"stop","usage":{"prompt_tokens":435,"completion_tokens":42,"total_tokens":477,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.A","exalt.B"],["exalt.B","exalt.A"],["perfectExalt.B","exalt.A"]],"acceptedIncludingGoalPrefix":[["exalt.A","exalt.B"],["exalt.B","exalt.A"],["perfectExalt.B","exalt.A"]],"missingIncludingGoalPrefix":1,"assessment":[{"i":1,"result":"full","step":2},{"i":2,"result":"full","step":2},{"i":3,"result":"invalid","step":1},{"i":4,"result":"full","step":2}]}
```

## 호출 79 · locked-4 · Siege Crossbow · 간략

사전 기대 실행열 1개.

```json
[["divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Siege Crossbow","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":31,"fractured":true},{"ref":"B","sourceValue":69,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Dexterity","unit":"flat","range":[31,33]},{"ref":"B","stat":"Life gained per enemy killed","unit":"flat","range":[69,84]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":31,"max":31,"weight":null,"disabled":false},{"ref":"B","min":84,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"DIVINE":1,"ANNULMENT":1,"CHAOS":1},"menu":["divine.high","annul.A","annul.B","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.high","annul.A","annul.B","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":460,"completion_tokens":9,"total_tokens":469,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["divine.high"]],"acceptedIncludingGoalPrefix":[["divine.high"]],"missingIncludingGoalPrefix":0,"assessment":[{"i":1,"result":"full","step":1}]}
```

## 호출 80 · locked-4 · Siege Crossbow · 보강

사전 기대 실행열 1개.

```json
[["divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Siege Crossbow","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":31,"fractured":true},{"ref":"B","sourceValue":69,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Dexterity","unit":"flat","range":[31,33]},{"ref":"B","stat":"Life gained per enemy killed","unit":"flat","range":[69,84]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":31,"max":31,"weight":null,"disabled":false},{"ref":"B","min":84,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"DIVINE":1,"ANNULMENT":1,"CHAOS":1},"menu":["divine.high","annul.A","annul.B","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"A fractured explicit remains at its original value during Divine rerolls and cannot be removed by annul or chaos. Other explicit values can still change.","modifierFacts":{"A":{"family":["Dexterity"],"tier":1,"requiresItemLevel":74,"acquireVia":[],"preservedByFracture":true,"sourceDivisor":1,"sourceRange":[31,33]},"B":{"family":["LifeGainedFromEnemyDeath"],"tier":1,"requiresItemLevel":77,"acquireVia":["chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[69,84]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"},"annul.A":{"consume":"ANNULMENT","remove":"A"},"annul.B":{"consume":"ANNULMENT","remove":"B"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":69}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.high","annul.A","annul.B","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":681,"completion_tokens":9,"total_tokens":690,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["divine.high"]],"acceptedIncludingGoalPrefix":[["divine.high"]],"missingIncludingGoalPrefix":0,"assessment":[{"i":1,"result":"full","step":1}]}
```

## 호출 81 · if-5 · Drakeskin Boots · 간략

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["chaos.A>B"],["divine.high","annul.A","exalt.B"],["divine.high","chaos.A>B"],["divine.high","exalt.B"],["exalt.B","annul.A"],["exalt.B","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Drakeskin Boots","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":11,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Increased Rarity of Items found","unit":"percent","range":[11,14]},{"ref":"B","stat":"Dexterity","unit":"flat","range":[31,33]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false},{"op":"IF","rows":[{"ref":"A","min":14,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1,"DIVINE":1},"menu":["exalt.B","annul.A","chaos.A>B","divine.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","annul.A","chaos.A>B","divine.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B\"],[\"chaos.A>B\",\"exalt.B\"],[\"annul.A\",\"chaos.A>B\",\"exalt.B\"],[\"divine.high\",\"exalt.B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":474,"completion_tokens":42,"total_tokens":516,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["divine.high","exalt.B"]],"acceptedIncludingGoalPrefix":[["chaos.A>B"],["divine.high","exalt.B"]],"missingIncludingGoalPrefix":5,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"tail","step":1},{"i":3,"result":"invalid","step":2},{"i":4,"result":"full","step":2}]}
```

## 호출 82 · if-5 · Drakeskin Boots · 보강

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["chaos.A>B"],["divine.high","annul.A","exalt.B"],["divine.high","chaos.A>B"],["divine.high","exalt.B"],["exalt.B","annul.A"],["exalt.B","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Drakeskin Boots","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":11,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Increased Rarity of Items found","unit":"percent","range":[11,14]},{"ref":"B","stat":"Dexterity","unit":"flat","range":[31,33]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false},{"op":"IF","rows":[{"ref":"A","min":14,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1,"DIVINE":1},"menu":["exalt.B","annul.A","chaos.A>B","divine.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"IF: an absent row passes. A present row must satisfy its range. Every active group must pass.","modifierFacts":{"A":{"family":["ItemFoundRarityIncrease"],"tier":2,"requiresItemLevel":24,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[11,14]},"B":{"family":["Dexterity"],"tier":2,"requiresItemLevel":74,"acquireVia":["exalt.B","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[31,33]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":31},"annul.A":{"consume":"ANNULMENT","remove":"A"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":31},"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","annul.A","chaos.A>B","divine.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B\"],[\"chaos.A>B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":694,"completion_tokens":15,"total_tokens":709,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["chaos.A>B"]],"acceptedIncludingGoalPrefix":[["chaos.A>B"]],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"full","step":1}]}
```

## 호출 83 · not-5 · Falconer's Jacket · 보강

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["chaos.A>B"],["divine.low","annul.A","exalt.B"],["divine.low","chaos.A>B"],["divine.low","exalt.B"],["exalt.B","annul.A"],["exalt.B","divine.low"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Falconer's Jacket","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":16,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Intelligence","unit":"flat","range":[13,16]},{"ref":"B","stat":"Lightning Resistance","unit":"percent","range":[6,10]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false},{"op":"NOT","rows":[{"ref":"A","min":16,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1,"DIVINE":1},"menu":["exalt.B","annul.A","chaos.A>B","divine.low"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"NOT matches if none of its enabled rows are both present and in range. A present value outside the forbidden interval can remain.","modifierFacts":{"A":{"family":["Intelligence"],"tier":6,"requiresItemLevel":22,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[13,16]},"B":{"family":["LightningResistance"],"tier":8,"requiresItemLevel":1,"acquireVia":["exalt.B","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[6,10]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":6},"annul.A":{"consume":"ANNULMENT","remove":"A"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":6},"divine.low":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"low"}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","annul.A","chaos.A>B","divine.low"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"annul.A\",\"exalt.B\"],[\"chaos.A>B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":690,"completion_tokens":19,"total_tokens":709,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["annul.A","exalt.B"],["chaos.A>B"]],"acceptedIncludingGoalPrefix":[["annul.A","exalt.B"],["chaos.A>B"]],"missingIncludingGoalPrefix":5,"assessment":[{"i":1,"result":"full","step":2},{"i":2,"result":"full","step":1}]}
```

## 호출 84 · not-5 · Falconer's Jacket · 간략

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["chaos.A>B"],["divine.low","annul.A","exalt.B"],["divine.low","chaos.A>B"],["divine.low","exalt.B"],["exalt.B","annul.A"],["exalt.B","divine.low"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Falconer's Jacket","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":16,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Intelligence","unit":"flat","range":[13,16]},{"ref":"B","stat":"Lightning Resistance","unit":"percent","range":[6,10]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false},{"op":"NOT","rows":[{"ref":"A","min":16,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1,"DIVINE":1},"menu":["exalt.B","annul.A","chaos.A>B","divine.low"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","annul.A","chaos.A>B","divine.low"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"annul.A\"]]}","finish_reason":"stop","usage":{"prompt_tokens":469,"completion_tokens":9,"total_tokens":478,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":7,"assessment":[{"i":1,"result":"unfinished","step":1}]}
```

## 호출 85 · count-5 · Shrine Sceptre · 간략

사전 기대 실행열 6개.

```json
[["annul.A","exalt.C"],["annul.B","exalt.C"],["chaos.A>C"],["chaos.B>C"],["exalt.C","annul.A"],["exalt.C","annul.B"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Shrine Sceptre","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":28,"fractured":false},{"ref":"B","sourceValue":31,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"C","stat":"Maximum Mana","unit":"flat","range":[125,149]}],"suffix":[{"ref":"A","stat":"Strength","unit":"flat","range":[28,30]},{"ref":"B","stat":"Intelligence","unit":"flat","range":[31,33]}]},"goalGroups":[{"op":"COUNT","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false},{"ref":"C","min":null,"max":null,"weight":null,"disabled":false}],"min":2,"max":2,"disabled":false},{"op":"AND","rows":[{"ref":"C","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1},"menu":["exalt.C","annul.A","annul.B","chaos.A>C","chaos.B>C"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.C","annul.A","annul.B","chaos.A>C","chaos.B>C"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"chaos.A>C\",\"exalt.C\"],[\"chaos.B>C\",\"exalt.C\"]]}","finish_reason":"stop","usage":{"prompt_tokens":548,"completion_tokens":24,"total_tokens":572,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[["chaos.A>C"],["chaos.B>C"]],"missingIncludingGoalPrefix":4,"assessment":[{"i":1,"result":"tail","step":1},{"i":2,"result":"tail","step":1}]}
```

## 호출 86 · count-5 · Shrine Sceptre · 보강

사전 기대 실행열 6개.

```json
[["annul.A","exalt.C"],["annul.B","exalt.C"],["chaos.A>C"],["chaos.B>C"],["exalt.C","annul.A"],["exalt.C","annul.B"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Shrine Sceptre","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":28,"fractured":false},{"ref":"B","sourceValue":31,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"C","stat":"Maximum Mana","unit":"flat","range":[125,149]}],"suffix":[{"ref":"A","stat":"Strength","unit":"flat","range":[28,30]},{"ref":"B","stat":"Intelligence","unit":"flat","range":[31,33]}]},"goalGroups":[{"op":"COUNT","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false},{"ref":"C","min":null,"max":null,"weight":null,"disabled":false}],"min":2,"max":2,"disabled":false},{"op":"AND","rows":[{"ref":"C","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"CHAOS":1},"menu":["exalt.C","annul.A","annul.B","chaos.A>C","chaos.B>C"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"COUNT counts enabled rows that are present and in range. Both minimum and maximum are inclusive; compare the count again after each removal or acquisition.","modifierFacts":{"A":{"family":["Strength"],"tier":2,"requiresItemLevel":66,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[28,30]},"B":{"family":["Intelligence"],"tier":1,"requiresItemLevel":74,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[31,33]},"C":{"family":["IncreasedMana"],"tier":2,"requiresItemLevel":65,"acquireVia":["exalt.C","chaos.A>C","chaos.B>C"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[125,149]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.C":{"consume":"EXALTED","gain":"C","sourceValue":125},"annul.A":{"consume":"ANNULMENT","remove":"A"},"annul.B":{"consume":"ANNULMENT","remove":"B"},"chaos.A>C":{"consume":"CHAOS","remove":"A","gain":"C","sourceValue":125},"chaos.B>C":{"consume":"CHAOS","remove":"B","gain":"C","sourceValue":125}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.C","annul.A","annul.B","chaos.A>C","chaos.B>C"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.C\"],[\"annul.A\",\"chaos.B>C\"],[\"annul.B\",\"chaos.A>C\"]]}","finish_reason":"stop","usage":{"prompt_tokens":841,"completion_tokens":29,"total_tokens":870,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"unfinished","step":2},{"i":3,"result":"unfinished","step":2}]}
```

## 호출 87 · w1-5 · Dueling Wand · 보강

사전 기대 실행열 6개.

```json
[["annul.A","exalt.B.high"],["chaos.A>B","divine.high"],["divine.high","annul.A","exalt.B.high"],["divine.high","exalt.B.high"],["exalt.B.high","annul.A"],["exalt.B.high","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Dueling Wand","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"A","sourceValue":15,"fractured":false}],"suffix":[]},"optionRefs":{"prefix":[{"ref":"A","stat":"Maximum Mana","unit":"flat","range":[15,24]}],"suffix":[{"ref":"B","stat":"Increased Cast Speed","unit":"percent","range":[9,12]}]},"goalGroups":[{"op":"WEIGHTED_V1","rows":[{"ref":"A","min":24,"max":null,"weight":1,"disabled":false},{"ref":"B","min":null,"max":null,"weight":0.5,"disabled":false}],"min":6.0,"max":null,"disabled":false},{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"DIVINE":1,"CHAOS":1},"menu":["exalt.B.high","annul.A","divine.high","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"WEIGHTED_V1: absent rows are optional. Every present enabled row must pass its own range, and the sum of present value times weight must lie in the group range. Weights are score coefficients.","modifierFacts":{"A":{"family":["IncreasedMana"],"tier":10,"requiresItemLevel":6,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[15,24]},"B":{"family":["IncreasedCastSpeed"],"tier":7,"requiresItemLevel":1,"acquireVia":["exalt.B.high","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[9,12]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.B.high":{"consume":"EXALTED","gain":"B","sourceValue":12},"annul.A":{"consume":"ANNULMENT","remove":"A"},"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":9}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B.high","annul.A","divine.high","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B.high\"],[\"chaos.A>B\"],[\"divine.high\",\"exalt.B.high\"],[\"divine.high\",\"chaos.A>B\"],[\"annul.A\",\"exalt.B.high\"],[\"annul.A\",\"chaos.A>B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":744,"completion_tokens":56,"total_tokens":800,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["annul.A","exalt.B.high"],["divine.high","exalt.B.high"]],"acceptedIncludingGoalPrefix":[["annul.A","exalt.B.high"],["divine.high","exalt.B.high"]],"missingIncludingGoalPrefix":4,"assessment":[{"i":1,"result":"unfinished","step":1},{"i":2,"result":"unfinished","step":1},{"i":3,"result":"full","step":2},{"i":4,"result":"unfinished","step":2},{"i":5,"result":"full","step":2},{"i":6,"result":"invalid","step":2}]}
```

## 호출 88 · w1-5 · Dueling Wand · 간략

사전 기대 실행열 6개.

```json
[["annul.A","exalt.B.high"],["chaos.A>B","divine.high"],["divine.high","annul.A","exalt.B.high"],["divine.high","exalt.B.high"],["exalt.B.high","annul.A"],["exalt.B.high","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Dueling Wand","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"A","sourceValue":15,"fractured":false}],"suffix":[]},"optionRefs":{"prefix":[{"ref":"A","stat":"Maximum Mana","unit":"flat","range":[15,24]}],"suffix":[{"ref":"B","stat":"Increased Cast Speed","unit":"percent","range":[9,12]}]},"goalGroups":[{"op":"WEIGHTED_V1","rows":[{"ref":"A","min":24,"max":null,"weight":1,"disabled":false},{"ref":"B","min":null,"max":null,"weight":0.5,"disabled":false}],"min":6.0,"max":null,"disabled":false},{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"DIVINE":1,"CHAOS":1},"menu":["exalt.B.high","annul.A","divine.high","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B.high","annul.A","divine.high","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"chaos.A>B\",\"exalt.B.high\"],[\"chaos.A>B\",\"divine.high\"],[\"annul.A\",\"chaos.A>B\",\"exalt.B.high\"],[\"annul.A\",\"chaos.A>B\",\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":500,"completion_tokens":54,"total_tokens":554,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["chaos.A>B","divine.high"]],"acceptedIncludingGoalPrefix":[["chaos.A>B","divine.high"]],"missingIncludingGoalPrefix":5,"assessment":[{"i":1,"result":"invalid","step":2},{"i":2,"result":"full","step":2},{"i":3,"result":"invalid","step":2},{"i":4,"result":"invalid","step":2}]}
```

## 호출 89 · w2-5 · Penetrating Quiver · 간략

사전 기대 실행열 5개.

```json
[["annul.A","exalt.B.high"],["chaos.A>B","divine.high"],["divine.high","annul.A","exalt.B.high"],["divine.high","exalt.B.high"],["exalt.B.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Penetrating Quiver","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"A","sourceValue":347,"fractured":false}],"suffix":[]},"optionRefs":{"prefix":[{"ref":"A","stat":"Accuracy Rating","unit":"flat","range":[347,450]}],"suffix":[{"ref":"B","stat":"Mana gained per enemy killed","unit":"flat","range":[15,20]}]},"goalGroups":[{"op":"WEIGHTED_V2","rows":[{"ref":"A","min":450,"max":null,"weight":1,"disabled":false},{"ref":"B","min":null,"max":null,"weight":0.5,"disabled":false}],"min":10.0,"max":null,"disabled":false},{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"DIVINE":1,"CHAOS":1},"menu":["exalt.B.high","annul.A","divine.high","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B.high","annul.A","divine.high","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"chaos.A>B\",\"exalt.B.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":503,"completion_tokens":15,"total_tokens":518,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":5,"assessment":[{"i":1,"result":"invalid","step":2}]}
```

## 호출 90 · w2-5 · Penetrating Quiver · 보강

사전 기대 실행열 5개.

```json
[["annul.A","exalt.B.high"],["chaos.A>B","divine.high"],["divine.high","annul.A","exalt.B.high"],["divine.high","exalt.B.high"],["exalt.B.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Penetrating Quiver","ilvl":82,"rarity":"Rare","start":{"prefix":[{"ref":"A","sourceValue":347,"fractured":false}],"suffix":[]},"optionRefs":{"prefix":[{"ref":"A","stat":"Accuracy Rating","unit":"flat","range":[347,450]}],"suffix":[{"ref":"B","stat":"Mana gained per enemy killed","unit":"flat","range":[15,20]}]},"goalGroups":[{"op":"WEIGHTED_V2","rows":[{"ref":"A","min":450,"max":null,"weight":1,"disabled":false},{"ref":"B","min":null,"max":null,"weight":0.5,"disabled":false}],"min":10.0,"max":null,"disabled":false},{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"ANNULMENT":1,"DIVINE":1,"CHAOS":1},"menu":["exalt.B.high","annul.A","divine.high","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"WEIGHTED_V2: at least one enabled row must be present. Only present rows inside their own range contribute value times weight. A present out-of-range row adds zero and does not veto the group.","modifierFacts":{"A":{"family":["IncreasedAccuracy"],"tier":2,"requiresItemLevel":67,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[347,450]},"B":{"family":["ManaGainedFromEnemyDeath"],"tier":2,"requiresItemLevel":45,"acquireVia":["exalt.B.high","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[15,20]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.B.high":{"consume":"EXALTED","gain":"B","sourceValue":20},"annul.A":{"consume":"ANNULMENT","remove":"A"},"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":15}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B.high","annul.A","divine.high","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B.high\"],[\"chaos.A>B\"],[\"annul.A\",\"exalt.B.high\"],[\"annul.A\",\"chaos.A>B\"],[\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":749,"completion_tokens":41,"total_tokens":790,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["annul.A","exalt.B.high"],["exalt.B.high"]],"acceptedIncludingGoalPrefix":[["annul.A","exalt.B.high"],["exalt.B.high"]],"missingIncludingGoalPrefix":3,"assessment":[{"i":1,"result":"full","step":1},{"i":2,"result":"unfinished","step":1},{"i":3,"result":"full","step":2},{"i":4,"result":"invalid","step":2},{"i":5,"result":"unfinished","step":1}]}
```

## 호출 91 · pseudo-5 · Corona Amulet · 보강

사전 기대 실행열 4개.

```json
[["exalt.A","divine.high","exalt.B"],["exalt.A","exalt.B"],["exalt.B","divine.high","exalt.A"],["exalt.B","exalt.A"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Corona Amulet","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Fire Resistance","unit":"percent","range":[36,40]},{"ref":"B","stat":"All Elemental Resistances","unit":"percent","range":[12,14]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"P","min":48,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":2,"DIVINE":1},"menu":["exalt.A","exalt.B","divine.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","derived":{"P":{"label":"Total Fire Resistance","sum":["A","B"],"implicitContribution":0}},"filterRule":"P is a derived display stat. Sum its direct layer-specific contributions once. A missing contribution adds zero. The derived stat is absent if all contributions are absent. P is not an affix to roll.","modifierFacts":{"A":{"family":["FireResistance"],"tier":2,"requiresItemLevel":71,"acquireVia":["exalt.A"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[36,40]},"B":{"family":["AllResistances"],"tier":3,"requiresItemLevel":54,"acquireVia":["exalt.B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[12,14]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.A":{"consume":"EXALTED","gain":"A","sourceValue":36},"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":12},"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","exalt.B","divine.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.A\",\"exalt.B\"],[\"exalt.B\",\"exalt.A\"]]}","finish_reason":"stop","usage":{"prompt_tokens":637,"completion_tokens":22,"total_tokens":659,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.A","exalt.B"],["exalt.B","exalt.A"]],"acceptedIncludingGoalPrefix":[["exalt.A","exalt.B"],["exalt.B","exalt.A"]],"missingIncludingGoalPrefix":2,"assessment":[{"i":1,"result":"full","step":2},{"i":2,"result":"full","step":2}]}
```

## 호출 92 · pseudo-5 · Corona Amulet · 간략

사전 기대 실행열 4개.

```json
[["exalt.A","divine.high","exalt.B"],["exalt.A","exalt.B"],["exalt.B","divine.high","exalt.A"],["exalt.B","exalt.A"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Corona Amulet","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Fire Resistance","unit":"percent","range":[36,40]},{"ref":"B","stat":"All Elemental Resistances","unit":"percent","range":[12,14]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"P","min":48,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":2,"DIVINE":1},"menu":["exalt.A","exalt.B","divine.high"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","derived":{"P":"Total Fire Resistance"}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","exalt.B","divine.high"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"exalt.A\",\"","finish_reason":"length","usage":{"prompt_tokens":402,"completion_tokens":3200,"total_tokens":3602,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":4,"assessment":{"result":"length","credited":0}}
```

## 호출 93 · unit-5 · Blacksteel Gauntlets · 간략

사전 기대 실행열 2개.

```json
[["divine.high","exalt.B"],["exalt.B","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Blacksteel Gauntlets","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":600,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"B","stat":"Accuracy Rating","unit":"flat","range":[61,84]}],"suffix":[{"ref":"A","stat":"Physical Attack Damage leeched as Mana","unit":"percent","range":[600,690]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":6.9,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"DIVINE":1,"EXALTED":1},"menu":["divine.high","exalt.B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.high","exalt.B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":428,"completion_tokens":9,"total_tokens":437,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":2,"assessment":[{"i":1,"result":"unfinished","step":1}]}
```

## 호출 94 · unit-5 · Blacksteel Gauntlets · 보강

사전 기대 실행열 2개.

```json
[["divine.high","exalt.B"],["exalt.B","divine.high"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Blacksteel Gauntlets","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":600,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"B","stat":"Accuracy Rating","unit":"flat","range":[61,84]}],"suffix":[{"ref":"A","stat":"Physical Attack Damage leeched as Mana","unit":"percent","range":[600,690]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":6.9,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"DIVINE":1,"EXALTED":1},"menu":["divine.high","exalt.B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"The action rolls source integers; compare goal ranges in display units after dividing by the stat-specific divisor. Do not round a near miss into a match.","modifierFacts":{"A":{"family":["ManaLeech"],"tier":3,"requiresItemLevel":38,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":100,"sourceRange":[600,690]},"B":{"family":["IncreasedAccuracy"],"tier":7,"requiresItemLevel":18,"acquireVia":["exalt.B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[61,84]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"},"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":61}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.high","exalt.B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.B\",\"divine.high\"],[\"divine.high\",\"exalt.B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":614,"completion_tokens":22,"total_tokens":636,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["divine.high","exalt.B"],["exalt.B","divine.high"]],"acceptedIncludingGoalPrefix":[["divine.high","exalt.B"],["exalt.B","divine.high"]],"missingIncludingGoalPrefix":0,"assessment":[{"i":1,"result":"full","step":2},{"i":2,"result":"full","step":2}]}
```

## 호출 95 · disabled-5 · Offering Wand · 보강

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["annul.A","exalt.C","exalt.B"],["chaos.A>B"],["exalt.B"],["exalt.C","annul.A","exalt.B"],["exalt.C","chaos.A>B"],["exalt.C","exalt.B"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Offering Wand","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":13,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"C","stat":"Maximum Mana","unit":"flat","range":[10,14]}],"suffix":[{"ref":"A","stat":"Increased Cast Speed","unit":"percent","range":[13,16]},{"ref":"B","stat":"Increased Mana Regeneration Rate","unit":"percent","range":[50,59]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false},{"ref":"C","min":null,"max":null,"weight":null,"disabled":true}],"min":null,"max":null,"disabled":false},{"op":"NOT","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":true}],"stock":{"EXALTED":2,"ANNULMENT":1,"CHAOS":1},"menu":["exalt.B","exalt.C","annul.A","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"Disabled groups and rows are ignored when evaluating the goal. Their modifiers may remain. A legal intermediate step is allowed if the goal has not yet been met.","modifierFacts":{"A":{"family":["IncreasedCastSpeed"],"tier":6,"requiresItemLevel":15,"acquireVia":[],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[13,16]},"B":{"family":["ManaRegeneration"],"tier":2,"requiresItemLevel":55,"acquireVia":["exalt.B","chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[50,59]},"C":{"family":["IncreasedMana"],"tier":11,"requiresItemLevel":1,"acquireVia":["exalt.C"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[10,14]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":50},"exalt.C":{"consume":"EXALTED","gain":"C","sourceValue":10},"annul.A":{"consume":"ANNULMENT","remove":"A"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":50}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","exalt.C","annul.A","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"annul.A\",\"exalt.B\"],[\"chaos.A>B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":788,"completion_tokens":19,"total_tokens":807,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["annul.A","exalt.B"],["chaos.A>B"]],"acceptedIncludingGoalPrefix":[["annul.A","exalt.B"],["chaos.A>B"]],"missingIncludingGoalPrefix":5,"assessment":[{"i":1,"result":"full","step":2},{"i":2,"result":"full","step":1}]}
```

## 호출 96 · disabled-5 · Offering Wand · 간략

사전 기대 실행열 7개.

```json
[["annul.A","exalt.B"],["annul.A","exalt.C","exalt.B"],["chaos.A>B"],["exalt.B"],["exalt.C","annul.A","exalt.B"],["exalt.C","chaos.A>B"],["exalt.C","exalt.B"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Offering Wand","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":13,"fractured":false}]},"optionRefs":{"prefix":[{"ref":"C","stat":"Maximum Mana","unit":"flat","range":[10,14]}],"suffix":[{"ref":"A","stat":"Increased Cast Speed","unit":"percent","range":[13,16]},{"ref":"B","stat":"Increased Mana Regeneration Rate","unit":"percent","range":[50,59]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"B","min":null,"max":null,"weight":null,"disabled":false},{"ref":"C","min":null,"max":null,"weight":null,"disabled":true}],"min":null,"max":null,"disabled":false},{"op":"NOT","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":true}],"stock":{"EXALTED":2,"ANNULMENT":1,"CHAOS":1},"menu":["exalt.B","exalt.C","annul.A","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.B","exalt.C","annul.A","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"chaos.A>B\",\"exalt.B\"]]}","finish_reason":"stop","usage":{"prompt_tokens":508,"completion_tokens":14,"total_tokens":522,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[["chaos.A>B"]],"missingIncludingGoalPrefix":6,"assessment":[{"i":1,"result":"tail","step":1}]}
```

## 호출 97 · floor-5 · Tasalian Focus · 간략

사전 기대 실행열 2개.

```json
[["exalt.A","perfectExalt.B"],["perfectExalt.B","exalt.A"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Tasalian Focus","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"optionRefs":{"prefix":[{"ref":"A","stat":"Maximum Mana","unit":"flat","range":[15,24]}],"suffix":[{"ref":"B","stat":"Increased Cast Speed","unit":"percent","range":[29,32]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"PERFECT_EXALTED":1},"menu":["exalt.A","perfectExalt.A","exalt.B","perfectExalt.B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","perfectExalt.A","exalt.B","perfectExalt.B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.A\",\"exalt.B\"],[\"exalt.B\",\"exalt.A\"],[\"perfectExalt.A\",\"exalt.B\"],[\"perfectExalt.B\",\"exalt.A\"],[\"exalt.A\",\"perfectExalt.B\"],[\"exalt.B\",\"perfectExalt.A\"],[\"perfectExalt.A\",\"perfectExalt.B\"],[\"perfectExalt.B\",\"perfectExalt.A\"]]}","finish_reason":"stop","usage":{"prompt_tokens":430,"completion_tokens":84,"total_tokens":514,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.A","perfectExalt.B"],["perfectExalt.B","exalt.A"]],"acceptedIncludingGoalPrefix":[["exalt.A","perfectExalt.B"],["perfectExalt.B","exalt.A"]],"missingIncludingGoalPrefix":0,"assessment":[{"i":1,"result":"invalid","step":2},{"i":2,"result":"invalid","step":2},{"i":3,"result":"invalid","step":1},{"i":4,"result":"full","step":2},{"i":5,"result":"full","step":2},{"i":6,"result":"invalid","step":2},{"i":7,"result":"invalid","step":1},{"i":8,"result":"invalid","step":2}]}
```

## 호출 98 · floor-5 · Tasalian Focus · 보강

사전 기대 실행열 2개.

```json
[["exalt.A","perfectExalt.B"],["perfectExalt.B","exalt.A"]]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Tasalian Focus","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[]},"optionRefs":{"prefix":[{"ref":"A","stat":"Maximum Mana","unit":"flat","range":[15,24]}],"suffix":[{"ref":"B","stat":"Increased Cast Speed","unit":"percent","range":[29,32]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":null,"max":null,"weight":null,"disabled":false},{"ref":"B","min":null,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"EXALTED":1,"PERFECT_EXALTED":1},"menu":["exalt.A","perfectExalt.A","exalt.B","perfectExalt.B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"Perfect Exalted has minimum modifier level 50. The highest eligible modifier level in each side/family is also allowed below the floor. Ordinary Exalted uses the ordinary eligible pool.","modifierFacts":{"A":{"family":["IncreasedMana"],"tier":10,"requiresItemLevel":6,"acquireVia":["exalt.A","perfectExalt.A"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[15,24],"highestEligibleLevelInFamilyAtStart":70},"B":{"family":["IncreasedCastSpeed"],"tier":1,"requiresItemLevel":70,"acquireVia":["exalt.B","perfectExalt.B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[29,32],"highestEligibleLevelInFamilyAtStart":70}},"rarePrefixSuffixCapacity":[3,3],"actions":{"exalt.A":{"consume":"EXALTED","gain":"A","sourceValue":15},"perfectExalt.A":{"consume":"PERFECT_EXALTED","gain":"A","sourceValue":15},"exalt.B":{"consume":"EXALTED","gain":"B","sourceValue":29},"perfectExalt.B":{"consume":"PERFECT_EXALTED","gain":"B","sourceValue":29}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","perfectExalt.A","exalt.B","perfectExalt.B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"exalt.A\",\"exalt.B\"],[\"exalt.A\",\"perfectExalt.B\"],[\"perfectExalt.A\",\"exalt.B\"],[\"perfectExalt.A\",\"perfectExalt.B\"],[\"exalt.B\",\"exalt.A\"],[\"exalt.B\",\"perfectExalt.A\"],[\"perfectExalt.B\",\"exalt.A\"],[\"perfectExalt.B\",\"perfectExalt.A\"]]}","finish_reason":"stop","usage":{"prompt_tokens":699,"completion_tokens":84,"total_tokens":783,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[["exalt.A","perfectExalt.B"],["perfectExalt.B","exalt.A"]],"acceptedIncludingGoalPrefix":[["exalt.A","perfectExalt.B"],["perfectExalt.B","exalt.A"]],"missingIncludingGoalPrefix":0,"assessment":[{"i":1,"result":"invalid","step":2},{"i":2,"result":"full","step":2},{"i":3,"result":"invalid","step":1},{"i":4,"result":"invalid","step":1},{"i":5,"result":"invalid","step":2},{"i":6,"result":"invalid","step":2},{"i":7,"result":"full","step":2},{"i":8,"result":"invalid","step":2}]}
```

## 호출 99 · locked-5 · Obliterator Bow · 보강

사전 기대 실행열 0개.

```json
[]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Obliterator Bow","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":10,"fractured":true},{"ref":"B","sourceValue":29,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Mana gained per enemy killed","unit":"flat","range":[10,14]},{"ref":"B","stat":"Life gained per enemy killed","unit":"flat","range":[29,40]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":14,"max":14,"weight":null,"disabled":false},{"ref":"B","min":40,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"DIVINE":1,"ANNULMENT":1,"CHAOS":1},"menu":["divine.high","annul.A","annul.B","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together.","filterRule":"A fractured explicit remains at its original value during Divine rerolls and cannot be removed by annul or chaos. Other explicit values can still change.","modifierFacts":{"A":{"family":["ManaGainedFromEnemyDeath"],"tier":5,"requiresItemLevel":34,"acquireVia":[],"preservedByFracture":true,"sourceDivisor":1,"sourceRange":[10,14]},"B":{"family":["LifeGainedFromEnemyDeath"],"tier":4,"requiresItemLevel":44,"acquireVia":["chaos.A>B"],"preservedByFracture":false,"sourceDivisor":1,"sourceRange":[29,40]}},"rarePrefixSuffixCapacity":[3,3],"actions":{"divine.high":{"consume":"DIVINE","rerollAllMutableExplicitsTo":"high"},"annul.A":{"consume":"ANNULMENT","remove":"A"},"annul.B":{"consume":"ANNULMENT","remove":"B"},"chaos.A>B":{"consume":"CHAOS","remove":"A","gain":"B","sourceValue":29}}},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.high","annul.A","annul.B","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"chaos.A>B\",\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":688,"completion_tokens":14,"total_tokens":702,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":0,"assessment":[{"i":1,"result":"invalid","step":1}]}
```

## 호출 100 · locked-5 · Obliterator Bow · 간략

사전 기대 실행열 0개.

```json
[]
```

전송한 사용자 메시지와 응답 스키마.

```json
{"input":{"base":"Obliterator Bow","ilvl":82,"rarity":"Rare","start":{"prefix":[],"suffix":[{"ref":"A","sourceValue":10,"fractured":true},{"ref":"B","sourceValue":29,"fractured":false}]},"optionRefs":{"prefix":[],"suffix":[{"ref":"A","stat":"Mana gained per enemy killed","unit":"flat","range":[10,14]},{"ref":"B","stat":"Life gained per enemy killed","unit":"flat","range":[29,40]}]},"goalGroups":[{"op":"AND","rows":[{"ref":"A","min":14,"max":14,"weight":null,"disabled":false},{"ref":"B","min":40,"max":null,"weight":null,"disabled":false}],"min":null,"max":null,"disabled":false}],"stock":{"DIVINE":1,"ANNULMENT":1,"CHAOS":1},"menu":["divine.high","annul.A","annul.B","chaos.A>B"],"outcomes":"Unless marked high, additions roll the lower endpoint. Divine.low/high rerolls eligible explicits to the named endpoints. X>Y denotes removing X and rolling Y with the same currency. Goal ranges are inclusive in display units. All active groups must hold together."},"schema":{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.high","annul.A","annul.B","chaos.A>B"]}}}},"required":["routes"],"additionalProperties":false}}
```

모델 응답 원문과 사용량.

```json
{"raw":"{\"routes\":[[\"annul.A\",\"chaos.A>B\",\"divine.high\"]]}","finish_reason":"stop","usage":{"prompt_tokens":463,"completion_tokens":18,"total_tokens":481,"prompt_tokens_details":{"cached_tokens":0,"audio_tokens":0},"completion_tokens_details":{"reasoning_tokens":0,"audio_tokens":0,"accepted_prediction_tokens":0,"rejected_prediction_tokens":0}}}
```

후보 순번은 1부터 센다. full은 완주, tail은 첫 완성 뒤 꼬리가 붙은 후보, invalid는 첫 실행 불가 단계, unfinished는 유효 단계를 실행하고도 목표 미달을 뜻한다. length는 유효 JSON을 반환하기 전에 출력이 끊긴 경우다.

```json
{"acceptedFull":[],"acceptedIncludingGoalPrefix":[],"missingIncludingGoalPrefix":0,"assessment":[{"i":1,"result":"invalid","step":1}]}
```
