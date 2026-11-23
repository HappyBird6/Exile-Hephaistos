# 새 카드 90회 요청과 응답

[집계·해석](new-base-survey.md) · [카드별 사전 기대와 검증 통과 실행열](new-base-routes.json)

아래 호출 번호는 이번 90회 안에서의 번호다. 목표와 기대 실행열은 카드별 최초 요청 전에 고정했다. 각 요청의 실제 메시지는 아래의 시스템 문안과 카드 입력 JSON이다. `response_format`은 `json_schema`이며 이름은 `candidate_routes`, `strict: true`를 사용했다. 모델은 `gpt-4.1-mini-2025-04-14`, temperature 0, 출력 상한 3,200토큰, store false였다. 결과가 나온 화폐·옵션 순서를 한 실행열로 채점했다.

## 공통 시스템 문안

### 문안 bde3aec34834

```text
PoE2 제작 경로를 제안한다. 각 경로는 현재 아이템부터 카드 완성까지 사용할 전략.대상 토큰의 배열이다. 각 토큰은 그 결과가 나온 분기를 뜻한다. acquire의 옵션은 각각 한 번 획득하고 keep은 끝까지 보존하며 remove는 모두 제거한다. 최종 희귀도와 추가 조건도 충족한다. 같은 family를 동시에 붙일 수 없다. 불확실하거나 확률이 낮아도 후보로 남긴다. 검증과 비용·확률은 백엔드가 맡는다.
처음 사용할 화폐나 획득 순서를 바꿔 다른 완성 경로도 탐색한다. 주요 경로와 다른 시작·후속 행동을 갖는 경로를 각각 나열한다.
```

### 문안 70289bfe0774

```text
PoE2 제작 경로를 제안한다. 각 경로는 현재 아이템부터 카드 완성까지 사용할 전략.대상 토큰의 배열이다. 각 토큰은 그 결과가 나온 분기를 뜻한다. acquire의 옵션은 각각 한 번 획득하고 keep은 끝까지 보존하며 remove는 모두 제거한다. 최종 희귀도와 추가 조건도 충족한다. 같은 family를 동시에 붙일 수 없다. 불확실하거나 확률이 낮아도 후보로 남긴다. 검증과 비용·확률은 백엔드가 맡는다.
먼저 하나의 유효한 완성 경로를 기록한다. 앞 응답의 실패 지점에서 현재 슬롯, 보유 family, 희귀도를 확인해 다른 후보를 별도로 만든다. 빈 목록은 이번에 찾지 못했다는 뜻이다. 각 획득이 목표 하나를 채운다. 모든 화폐 종류를 소진할 필요는 없다. Magic에서 증강은 선택 사항이고 바로 Regal로 Rare가 될 수 있다. 희귀도별 접두·접미 한도를 각각 센다.
```

### 문안 e82e9011115d

```text
PoE2 제작 경로를 제안한다. 각 경로는 현재 아이템부터 카드 완성까지 사용할 전략.대상 토큰의 배열이다. 각 토큰은 그 결과가 나온 분기를 뜻한다. acquire의 옵션은 각각 한 번 획득하고 keep은 끝까지 보존하며 remove는 모두 제거한다. 최종 희귀도와 추가 조건도 충족한다. 같은 family를 동시에 붙일 수 없다. 불확실하거나 확률이 낮아도 후보로 남긴다. 검증과 비용·확률은 백엔드가 맡는다.
가장 단순한 성공 경로를 먼저 완성한다. 출력에서 임의 토큰, 상태명, 설명을 생략하고 실제 화폐 단계만 적는다. 각 획득이 목표 하나를 채운다. 모든 화폐 종류를 소진할 필요는 없다. Magic에서 증강은 선택 사항이고 바로 Regal로 Rare가 될 수 있다. 희귀도별 접두·접미 한도를 각각 센다. 각 첫 획득 칸에서 모든 목표마다 번호|화폐.대상을 직접 선택한다. 번호는 1,2,3을 한 번씩 써야 한다. 서로 다른 순서를 별도 후보로 제안한다.
```

### 문안 e38f7345a845

```text
PoE2 제작 경로를 제안한다. 각 경로는 현재 아이템부터 카드 완성까지 사용할 전략.대상 토큰의 배열이다. 각 토큰은 그 결과가 나온 분기를 뜻한다. acquire의 옵션은 각각 한 번 획득하고 keep은 끝까지 보존하며 remove는 모두 제거한다. 최종 희귀도와 추가 조건도 충족한다. 같은 family를 동시에 붙일 수 없다. 불확실하거나 확률이 낮아도 후보로 남긴다. 검증과 비용·확률은 백엔드가 맡는다.
먼저 하나의 유효한 완성 경로를 기록한다. 앞 응답의 실패 지점에서 현재 슬롯, 보유 family, 희귀도를 확인해 다른 후보를 별도로 만든다. 빈 목록은 이번에 찾지 못했다는 뜻이다. 각 획득이 목표 하나를 채운다. 모든 화폐 종류를 소진할 필요는 없다. Magic에서 증강은 선택 사항이고 바로 Regal로 Rare가 될 수 있다. 희귀도별 접두·접미 한도를 각각 센다. Regal 승급은 정확히 한 번이다. first와 upgrade, other에 실제 화폐와 대상을 지정하고 중복 없는 실행 번호를 고른다. 같은 옵션을 반복할 수 없다.
```

### 문안 fdefcc2650c7

```text
PoE2 제작 경로를 제안한다. 각 경로는 현재 아이템부터 카드 완성까지 사용할 전략.대상 토큰의 배열이다. 각 토큰은 그 결과가 나온 분기를 뜻한다. acquire의 옵션은 각각 한 번 획득하고 keep은 끝까지 보존하며 remove는 모두 제거한다. 최종 희귀도와 추가 조건도 충족한다. 같은 family를 동시에 붙일 수 없다. 불확실하거나 확률이 낮아도 후보로 남긴다. 검증과 비용·확률은 백엔드가 맡는다.
먼저 하나의 유효한 완성 경로를 기록한다. 앞 응답의 실패 지점에서 현재 슬롯, 보유 family, 희귀도를 확인해 다른 후보를 별도로 만든다. 빈 목록은 이번에 찾지 못했다는 뜻이다.
```

### 문안 b1d2f2dc0221

```text
PoE2 제작 경로를 제안한다. 각 경로는 현재 아이템부터 카드 완성까지 사용할 전략.대상 토큰의 배열이다. 각 토큰은 그 결과가 나온 분기를 뜻한다. acquire의 옵션은 각각 한 번 획득하고 keep은 끝까지 보존하며 remove는 모두 제거한다. 최종 희귀도와 추가 조건도 충족한다. 같은 family를 동시에 붙일 수 없다. 불확실하거나 확률이 낮아도 후보로 남긴다. 검증과 비용·확률은 백엔드가 맡는다.
가장 단순한 성공 경로를 먼저 완성한다. 출력에서 임의 토큰, 상태명, 설명을 생략하고 실제 화폐 단계만 적는다.
```

### 문안 fe60b9718f6c

```text
PoE2 제작 경로를 제안한다. 각 경로는 현재 아이템부터 카드 완성까지 사용할 전략.대상 토큰의 배열이다. 각 토큰은 그 결과가 나온 분기를 뜻한다. acquire의 옵션은 각각 한 번 획득하고 keep은 끝까지 보존하며 remove는 모두 제거한다. 최종 희귀도와 추가 조건도 충족한다. 같은 family를 동시에 붙일 수 없다. 불확실하거나 확률이 낮아도 후보로 남긴다. 검증과 비용·확률은 백엔드가 맡는다.
처음 사용할 화폐나 획득 순서를 바꿔 다른 완성 경로도 탐색한다. 주요 경로와 다른 시작·후속 행동을 갖는 경로를 각각 나열한다. 각 행동 뒤 acquire에서 실제로 새로 붙은 옵션을 모두 지운다. 마지막 화폐는 아직 남아 있는 대상을 골라야 한다. 모든 목표를 채운 순간 경로를 끝낸다.
```

## 호출별 기대·입력·원문·판정

응답은 `raw` 문자열에 모델이 반환한 원문 전체를 보존한다. 후보 번호는 최상위 필드 순서대로 각 배열을 이어서 1부터 센다. `bad`는 첫 실패 지점, `prefix`는 완성 후 남은 꼬리의 길이다. 각 카드 첫 등장에 사전 기대 전체를 적었고, 재요청에서도 같은 기대를 사용했다.

### 호출 1 · Time-Lost Ruby · room-1 · survey

기대 경로 6개.
```json
[["annul.X","exalt.A","exalt.B"],["annul.X","exalt.B","exalt.A"],["exalt.B","annul.X","exalt.A"],["exalt.B","sideAnnul.X","exalt.A"],["sideAnnul.X","exalt.A","exalt.B"],["sideAnnul.X","exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `ade86c24d889` · 입력 JSON
```json
{"base":"Time-Lost Ruby","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[2,2]},"implicit":{"effect":"local jewel effect base radius [1000]","tags":["unscalable","passive-radius-condition"],"stats":[{"id":"local_jewel_effect_base_radius","min":1000,"max":1000}]},"mods":{"X":{"side":"PREFIX","effect":"Notable Passive Skills in Radius also grant (3—7)% increased Ignite Magnitude","family":["IgniteEffect"],"tags":["elemental_damage","damage","elemental","fire","ailment","passive-radius-condition"]},"K0":{"side":"PREFIX","effect":"Small Passive Skills in Radius also grant (1—2)% increased Damage with Maces","family":["SpecificWeaponDamage"],"tags":["damage","attack","passive-radius-condition"]},"A":{"side":"PREFIX","effect":"Small Passive Skills in Radius also grant 1% chance to inflict Bleeding on Hit","family":["BleedChanceIncrease"],"tags":["bleed","physical","ailment","passive-radius-condition"]},"B":{"side":"SUFFIX","effect":"Small Passive Skills in Radius also grant (2—3)% increased Warcry Speed","family":["WarcrySpeed"],"tags":["speed","passive-radius-condition"]}},"start":["X","K0"],"acquire":["A","B"],"remove":["X"],"keep":["K0"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","annul.X","sideAnnul.X"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","annul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","sideAnnul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다. 접두만 제거"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\",\"annul.X\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\",\"annul.X\"],[\"exalt.A\",\"sideAnnul.X\",\"exalt.B\"],[\"exalt.B\",\"sideAnnul.X\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[4],"uniqueFull":1,"missingFull":5,"bad":[[1,"RARE/K0,X 에서 exalt.A 실행 불가"],[2,"RARE/B,K0,X 에서 exalt.A 실행 불가"],[3,"RARE/K0,X 에서 exalt.A 실행 불가"]],"prefix":[]}
```

### 호출 2 · Flexed Crossbow · tag-1 · survey

기대 경로 6개.
```json
[["exalt.A","exalt.B"],["exalt.A","tagExalt.B"],["exalt.B","exalt.A"],["exalt.B","tagExalt.A"],["tagExalt.A","exalt.B"],["tagExalt.A","tagExalt.B"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `ba524f263568` · 입력 JSON
```json
{"base":"Flexed Crossbow","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"(20—30)% increased Bolt Speed","tags":[],"stats":[{"id":"projectile_speed_+%_with_crossbow_skills","min":20,"max":30}]},"mods":{"K":{"side":"SUFFIX","effect":"Leeches (8—8.9)% of Physical Damage as Mana","family":["ManaLeech"],"tags":["mana","physical","attack"]},"A":{"side":"PREFIX","effect":"Adds (112—124) to (168—189) Cold Damage","family":["ColdDamage"],"tags":["damage","elemental","cold","attack"]},"B":{"side":"PREFIX","effect":"(100—119)% increased Elemental Damage with Attacks","family":["IncreasedWeaponElementalDamagePercent"],"tags":["damage","elemental","fire","cold","lightning"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","tagExalt.A","tagExalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","tagExalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 현재 implicit·explicit 태그 합집합이 비지 않으면 그중 하나 이상 겹치는 옵션만 추가","tagExalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 현재 implicit·explicit 태그 합집합이 비지 않으면 그중 하나 이상 겹치는 옵션만 추가"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"tagExalt.A\",\"exalt.B\"],[\"exalt.A\",\"tagExalt.B\"],[\"tagExalt.A\",\"tagExalt.B\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3,4],"uniqueFull":4,"missingFull":2,"bad":[],"prefix":[]}
```

### 호출 3 · Plate Belt · grade-1 · survey

기대 경로 10개.
```json
[["transmute.A","augment.C","regal.B"],["transmute.A","regal.B","exalt.C"],["transmute.A","regal.C","exalt.B"],["transmute.B","augment.C","regal.A"],["transmute.B","regal.A","exalt.C"],["transmute.B","regal.C","exalt.A"],["transmute.C","augment.A","regal.B"],["transmute.C","augment.B","regal.A"],["transmute.C","regal.A","exalt.B"],["transmute.C","regal.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `9775aa364937` · 입력 JSON
```json
{"base":"Plate Belt","ilvl":82,"rarity":"NORMAL","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"+(140—180) to Armour","tags":["defences","armour"],"stats":[{"id":"base_physical_damage_reduction_rating","min":140,"max":180}]},"mods":{"A":{"side":"PREFIX","effect":"+(226—267) to Armour","family":["IncreasedPhysicalDamageReductionRating"],"tags":["defences","armour"]},"B":{"side":"PREFIX","effect":"(1—2) to (3—4) Physical Thorns damage","family":["Thorns"],"tags":["physical_damage","damage","physical"]},"C":{"side":"SUFFIX","effect":"+(254—304) to Stun Threshold","family":["StunThreshold"],"tags":[]}},"start":[],"acquire":["A","B","C"],"remove":[],"keep":[],"goalRarity":"RARE","actions":["transmute.A","transmute.B","transmute.C","augment.A","augment.B","augment.C","regal.A","regal.B","regal.C","exalt.A","exalt.B","exalt.C"],"actionRules":{"transmute.A":"Normal → Magic, 일반 옵션 하나 추가.","transmute.B":"Normal → Magic, 일반 옵션 하나 추가.","transmute.C":"Normal → Magic, 일반 옵션 하나 추가.","augment.A":"Magic, Magic 자리 한도를 유지하고 일반 옵션 하나 추가.","augment.B":"Magic, Magic 자리 한도를 유지하고 일반 옵션 하나 추가.","augment.C":"Magic, Magic 자리 한도를 유지하고 일반 옵션 하나 추가.","regal.A":"Magic → Rare, Rare 한도로 일반 옵션 하나 추가.","regal.B":"Magic → Rare, Rare 한도로 일반 옵션 하나 추가.","regal.C":"Magic → Rare, Rare 한도로 일반 옵션 하나 추가.","exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.C":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"transmute.A\",\"augment.B\",\"regal.C\",\"exalt.A\"]],\"alternatives\":[[\"transmute.B\",\"augment.A\",\"regal.B\",\"exalt.C\"],[\"transmute.C\",\"augment.C\",\"regal.A\",\"exalt.B\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[],"uniqueFull":0,"missingFull":10,"bad":[[1,"MAGIC/A 에서 augment.B 실행 불가"],[2,"MAGIC/B 에서 augment.A 실행 불가"],[3,"MAGIC/C 에서 augment.C 실행 불가"]],"prefix":[]}
```

### 호출 4 · Rambler Jacket · swap-1 · survey

기대 경로 5개.
```json
[["annul.X","exalt.A","exalt.B"],["annul.X","exalt.B","exalt.A"],["chaos.X>A","exalt.B"],["exalt.B","annul.X","exalt.A"],["exalt.B","chaos.X>A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `19f6d754cc2f` · 입력 JSON
```json
{"base":"Rambler Jacket","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"+(7—13)% to Chaos Resistance","tags":["chaos_resistance","chaos","resistance"],"stats":[{"id":"base_chaos_damage_resistance_%","min":7,"max":13}]},"mods":{"A":{"side":"PREFIX","effect":"+(15—21) to Evasion Rating\n+(7—8) to maximum Energy Shield\n(21—26)% increased Evasion and Energy Shield","family":["BaseLocalDefencesAndDefencePercent"],"tags":["defences","evasion","energy_shield"]},"X":{"side":"PREFIX","effect":"+(4—14) to Evasion Rating\n+(5—6) to maximum Energy Shield\n(14—20)% increased Evasion and Energy Shield","family":["BaseLocalDefencesAndDefencePercent"],"tags":["defences","evasion","energy_shield"]},"K":{"side":"SUFFIX","effect":"+(17—20) to Dexterity","family":["Dexterity"],"tags":["attribute"]},"B":{"side":"SUFFIX","effect":"+(36—40)% to Fire Resistance","family":["FireResistance"],"tags":["elemental_resistance","fire_resistance","elemental","fire","resistance"]}},"start":["K","X"],"acquire":["A","B"],"remove":["X"],"keep":["K"],"goalRarity":"RARE","actions":["annul.X","exalt.A","exalt.B","chaos.X>A"],"actionRules":{"annul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","chaos.X>A":"Rare에서 지정한 기존 옵션이 제거되고 이어 목표 옵션이 뽑힌 한 번의 교체."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"annul.X\",\"exalt.A\",\"exalt.B\"],[\"chaos.X>A\"]],\"alternatives\":[[\"exalt.A\",\"annul.X\",\"exalt.B\"],[\"exalt.B\",\"annul.X\",\"exalt.A\"],[\"annul.X\",\"chaos.X>A\",\"exalt.A\",\"exalt.B\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,4],"uniqueFull":2,"missingFull":3,"bad":[[2,"목표 미달 RARE/A,K"],[3,"RARE/K,X 에서 exalt.A 실행 불가"],[5,"RARE/K 에서 chaos.X>A 실행 불가"]],"prefix":[]}
```

### 호출 5 · Dark Staff · hybrid-1 · survey

기대 경로 2개.
```json
[["exalt.A","exalt.B"],["exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `16e0b76b083f` · 입력 JSON
```json
{"base":"Dark Staff","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"A":{"side":"SUFFIX","effect":"(8—12)% increased Mana Regeneration Rate\n5% increased Light Radius","family":["LightRadiusAndManaRegeneration"],"tags":["mana"]},"B":{"side":"PREFIX","effect":"(209—238)% increased Cold Damage","family":["WeaponDamageTypePrefix"],"tags":["damage","elemental","cold"]},"K":{"side":"PREFIX","effect":"+(159—178) to maximum Mana","family":["IncreasedMana"],"tags":["mana"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","requiredStatRows":["light_radius_+%","mana_regeneration_rate_+%","cold_damage_+%"],"actions":["exalt.A","exalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2],"uniqueFull":2,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 6 · Visceral Quiver · side-1 · survey

기대 경로 8개.
```json
[["dextral_exaltation.B","exalt.A"],["dextral_exaltation.B","sinistral_exaltation.A"],["exalt.A","dextral_exaltation.B"],["exalt.A","exalt.B"],["exalt.B","exalt.A"],["exalt.B","sinistral_exaltation.A"],["sinistral_exaltation.A","dextral_exaltation.B"],["sinistral_exaltation.A","exalt.B"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `8e0e79b42f01` · 입력 JSON
```json
{"base":"Visceral Quiver","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"(20—30)% increased Critical Hit Chance for Attacks","tags":["attack","critical"],"stats":[{"id":"attack_critical_strike_chance_+%","min":20,"max":30}]},"mods":{"K":{"side":"PREFIX","effect":"+(124—167) to Accuracy Rating","family":["IncreasedAccuracy"],"tags":["attack"]},"A":{"side":"PREFIX","effect":"Adds (4—6) to (8—11) Physical Damage to Attacks","family":["PhysicalDamage"],"tags":["damage","physical","attack"]},"B":{"side":"SUFFIX","effect":"(21—23)% chance to Pierce an Enemy","family":["ChanceToPierce"],"tags":[]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","sinistral_exaltation.A","dextral_exaltation.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","sinistral_exaltation.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 접두만 추가","dextral_exaltation.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 접미만 추가"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"sinistral_exaltation.A\",\"exalt.B\"],[\"exalt.A\",\"dextral_exaltation.B\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3],"uniqueFull":3,"missingFull":5,"bad":[],"prefix":[]}
```

### 호출 7 · Maji Talisman · implicit-1 · survey

기대 경로 2개.
```json
[["exalt.A","exalt.B"],["exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `16e0b76b083f` · 입력 JSON
```json
{"base":"Maji Talisman","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"+(7—10) to Maximum Rage","tags":[],"stats":[{"id":"maximum_rage","min":7,"max":10}]},"mods":{"A":{"side":"PREFIX","effect":"Adds (46—57) to (70—88) Cold Damage","family":["ColdDamage"],"tags":["damage","elemental","cold","attack"]},"B":{"side":"SUFFIX","effect":"+(28—30) to Intelligence","family":["Intelligence"],"tags":["attribute"]},"K":{"side":"PREFIX","effect":"Adds (56—70) to (84—107) Fire Damage","family":["FireDamage"],"tags":["damage","elemental","fire","attack"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","keepImplicitStat":{"id":"maximum_rage","min":7,"max":10},"actions":["exalt.A","exalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2],"uniqueFull":2,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 8 · Akoyan Spear · fracture-1 · survey

기대 경로 4개.
```json
[["exalt.A","exalt.B","fracture.K"],["exalt.A","fracture.K","exalt.B"],["exalt.B","exalt.A","fracture.K"],["exalt.B","fracture.K","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `5906d0002371` · 입력 JSON
```json
{"base":"Akoyan Spear","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"local display grants spear throw skill [1]","tags":[],"stats":[{"id":"local_display_grants_spear_throw_skill","min":1,"max":1}]},"mods":{"K":{"side":"PREFIX","effect":"Adds (26—39) to (44—66) Physical Damage","family":["PhysicalDamage"],"tags":["damage","physical","attack"]},"J":{"side":"SUFFIX","effect":"+(3.81—4.4)% to Critical Hit Chance","family":["CriticalStrikeChanceIncrease"],"tags":["attack","critical"]},"L":{"side":"PREFIX","effect":"(45—54)% increased Physical Damage\n+(98—123) to Accuracy Rating","family":["LocalIncreasedPhysicalDamagePercentAndAccuracyRating"],"tags":["damage","physical","attack"]},"A":{"side":"SUFFIX","effect":"Grants 4 Life per Enemy Hit","family":["LifeGainPerTarget"],"tags":["life","attack"]},"B":{"side":"SUFFIX","effect":"20% reduced Attribute Requirements","family":["LocalAttributeRequirements"],"tags":[]}},"start":["K","J","L"],"acquire":["A","B"],"remove":[],"keep":["K","J","L"],"goalRarity":"RARE","mustFracture":"K","actions":["exalt.A","exalt.B","fracture.K"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","fracture.K":"Rare 명시 4개 이상에서 보유 K가 분열되는 결과. 옵션 수는 그대로다."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\",\"fracture.K\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\",\"fracture.K\"],[\"exalt.A\",\"fracture.K\",\"exalt.B\"],[\"exalt.B\",\"fracture.K\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3,4],"uniqueFull":4,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 9 · Shrine Sceptre · room-2 · survey

기대 경로 15개.
```json
[["annul.X","annul.Y","exalt.A","exalt.B"],["annul.X","annul.Y","exalt.B","exalt.A"],["annul.X","exalt.A","annul.Y","exalt.B"],["annul.X","exalt.A","exalt.B","annul.Y"],["annul.X","exalt.B","annul.Y","exalt.A"],["annul.X","exalt.B","exalt.A","annul.Y"],["annul.Y","annul.X","exalt.A","exalt.B"],["annul.Y","annul.X","exalt.B","exalt.A"],["annul.Y","exalt.B","annul.X","exalt.A"],["doubleAnnul.X+Y","exalt.A","exalt.B"],["doubleAnnul.X+Y","exalt.B","exalt.A"],["exalt.B","annul.X","annul.Y","exalt.A"],["exalt.B","annul.X","exalt.A","annul.Y"],["exalt.B","annul.Y","annul.X","exalt.A"],["exalt.B","doubleAnnul.X+Y","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `740f5afe337e` · 입력 JSON
```json
{"base":"Shrine Sceptre","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"X":{"side":"SUFFIX","effect":"Allies in your Presence Regenerate (29.1—33) Life per second","family":["LifeRegeneration"],"tags":["resource","life"]},"K0":{"side":"SUFFIX","effect":"Allies in your Presence have (9—12)% increased Cast Speed","family":["IncreasedCastSpeed"],"tags":["caster_speed","caster","speed"]},"K1":{"side":"SUFFIX","effect":"(20—29)% increased Mana Regeneration Rate","family":["ManaRegeneration"],"tags":["resource","mana"]},"A":{"side":"SUFFIX","effect":"35% reduced Attribute Requirements","family":["LocalAttributeRequirements"],"tags":[]},"B":{"side":"PREFIX","effect":"Allies in your Presence deal (17—19) to (27—30) added Attack Fire Damage","family":["FireDamage"],"tags":["elemental_damage","damage","elemental","fire","attack"]},"Y":{"side":"PREFIX","effect":"(23—26)% increased Spirit+(29—33) to maximum Mana","family":["LocalIncreasedSpiritAndMana"],"tags":[]}},"start":["X","K0","K1","Y"],"acquire":["A","B"],"remove":["X","Y"],"keep":["K0","K1"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","annul.X","annul.Y","doubleAnnul.X+Y"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","annul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","annul.Y":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","doubleAnnul.X+Y":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다. 한 번에 서로 다른 두 개 제거"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"annul.X\",\"exalt.A\",\"exalt.B\",\"annul.Y\"]],\"alternatives\":[[\"annul.Y\",\"exalt.A\",\"exalt.B\",\"annul.X\"],[\"doubleAnnul.X+Y\",\"exalt.A\",\"exalt.B\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,3],"uniqueFull":2,"missingFull":13,"bad":[[2,"RARE/K0,K1,X 에서 exalt.A 실행 불가"]],"prefix":[]}
```

### 호출 10 · Ancestral Tiara · tag-2 · survey

기대 경로 6개.
```json
[["exalt.A","exalt.B"],["exalt.A","tagExalt.B"],["exalt.B","exalt.A"],["exalt.B","tagExalt.A"],["tagExalt.A","exalt.B"],["tagExalt.A","tagExalt.B"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `ba524f263568` · 입력 JSON
```json
{"base":"Ancestral Tiara","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"K":{"side":"PREFIX","effect":"+(42—47) to maximum Energy Shield","family":["BaseLocalDefences"],"tags":["defences","energy_shield"]},"A":{"side":"PREFIX","effect":"(14—20)% increased Energy Shield\n+(9—16) to maximum Mana","family":["BaseLocalDefencesAndMana"],"tags":["defences","resource","mana","energy_shield"]},"B":{"side":"PREFIX","effect":"+(85—99) to maximum Life","family":["IncreasedLife"],"tags":["resource","life"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","tagExalt.A","tagExalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","tagExalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 현재 implicit·explicit 태그 합집합이 비지 않으면 그중 하나 이상 겹치는 옵션만 추가","tagExalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 현재 implicit·explicit 태그 합집합이 비지 않으면 그중 하나 이상 겹치는 옵션만 추가"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"],[\"tagExalt.A\",\"exalt.B\"],[\"exalt.A\",\"tagExalt.B\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3,4],"uniqueFull":4,"missingFull":2,"bad":[],"prefix":[]}
```

### 호출 11 · Blacksteel Crest Shield · grade-2 · survey

기대 경로 10개.
```json
[["transmute.A","augment.C","regal.B"],["transmute.A","regal.B","exalt.C"],["transmute.A","regal.C","exalt.B"],["transmute.B","augment.C","regal.A"],["transmute.B","regal.A","exalt.C"],["transmute.B","regal.C","exalt.A"],["transmute.C","augment.A","regal.B"],["transmute.C","augment.B","regal.A"],["transmute.C","regal.A","exalt.B"],["transmute.C","regal.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `9775aa364937` · 입력 JSON
```json
{"base":"Blacksteel Crest Shield","ilvl":82,"rarity":"NORMAL","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"A":{"side":"SUFFIX","effect":"+(21—25)% to Lightning Resistance","family":["LightningResistance"],"tags":["elemental","lightning","resistance"]},"B":{"side":"SUFFIX","effect":"+(20—25)% of Armour also applies to Elemental Damage","family":["ArmourAppliesToElementalDamage"],"tags":["armour","elemental"]},"C":{"side":"PREFIX","effect":"(101—110)% increased Armour and Energy Shield","family":["DefencesPercent"],"tags":["armour","energy_shield"]}},"start":[],"acquire":["A","B","C"],"remove":[],"keep":[],"goalRarity":"RARE","actions":["transmute.A","transmute.B","transmute.C","augment.A","augment.B","augment.C","regal.A","regal.B","regal.C","exalt.A","exalt.B","exalt.C"],"actionRules":{"transmute.A":"Normal → Magic, 일반 옵션 하나 추가.","transmute.B":"Normal → Magic, 일반 옵션 하나 추가.","transmute.C":"Normal → Magic, 일반 옵션 하나 추가.","augment.A":"Magic, Magic 자리 한도를 유지하고 일반 옵션 하나 추가.","augment.B":"Magic, Magic 자리 한도를 유지하고 일반 옵션 하나 추가.","augment.C":"Magic, Magic 자리 한도를 유지하고 일반 옵션 하나 추가.","regal.A":"Magic → Rare, Rare 한도로 일반 옵션 하나 추가.","regal.B":"Magic → Rare, Rare 한도로 일반 옵션 하나 추가.","regal.C":"Magic → Rare, Rare 한도로 일반 옵션 하나 추가.","exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.C":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"transmute.C\",\"augment.C\",\"regal.C\",\"exalt.C\"]],\"alternatives\":[[\"transmute.A\",\"augment.A\",\"regal.A\",\"exalt.A\"],[\"transmute.B\",\"augment.B\",\"regal.B\",\"exalt.B\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[],"uniqueFull":0,"missingFull":10,"bad":[[1,"MAGIC/C 에서 augment.C 실행 불가"],[2,"MAGIC/A 에서 augment.A 실행 불가"],[3,"MAGIC/B 에서 augment.B 실행 불가"]],"prefix":[]}
```

### 호출 12 · Tawhoan Greatclub · swap-2 · survey

기대 경로 5개.
```json
[["annul.X","exalt.A","exalt.B"],["annul.X","exalt.B","exalt.A"],["chaos.X>A","exalt.B"],["exalt.B","annul.X","exalt.A"],["exalt.B","chaos.X>A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `19f6d754cc2f` · 입력 JSON
```json
{"base":"Tawhoan Greatclub","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"Warcries Empower 1 additional Attacks","tags":["attack"],"stats":[{"id":"warcry_empowers_next_x_melee_attacks","min":1,"max":1}]},"mods":{"A":{"side":"SUFFIX","effect":"Gain (6—9) Mana per enemy killed","family":["ManaGainedFromEnemyDeath"],"tags":["mana"]},"X":{"side":"SUFFIX","effect":"Gain (4—5) Mana per enemy killed","family":["ManaGainedFromEnemyDeath"],"tags":["mana"]},"K":{"side":"PREFIX","effect":"(100—119)% increased Elemental Damage with Attacks","family":["IncreasedWeaponElementalDamagePercent"],"tags":["damage","elemental","fire","cold","lightning"]},"B":{"side":"PREFIX","effect":"Adds (5—8) to (10—15) Physical Damage","family":["PhysicalDamage"],"tags":["damage","physical","attack"]}},"start":["K","X"],"acquire":["A","B"],"remove":["X"],"keep":["K"],"goalRarity":"RARE","actions":["annul.X","exalt.A","exalt.B","chaos.X>A"],"actionRules":{"annul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","chaos.X>A":"Rare에서 지정한 기존 옵션이 제거되고 이어 목표 옵션이 뽑힌 한 번의 교체."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"annul.X\",\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"annul.X\",\"chaos.X>A\",\"exalt.B\"],[\"annul.X\",\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,3],"uniqueFull":2,"missingFull":3,"bad":[[2,"RARE/K 에서 chaos.X>A 실행 불가"]],"prefix":[]}
```

### 호출 13 · Blacksteel Sabatons · hybrid-2 · survey

기대 경로 2개.
```json
[["exalt.A","exalt.B"],["exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `16e0b76b083f` · 입력 JSON
```json
{"base":"Blacksteel Sabatons","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"A":{"side":"PREFIX","effect":"+(47—71) to Armour\n+(42—64) to Evasion Rating","family":["BaseLocalDefences"],"tags":["defences","armour","evasion"]},"B":{"side":"SUFFIX","effect":"+(36—40)% to Fire Resistance","family":["FireResistance"],"tags":["elemental_resistance","fire_resistance","elemental","fire","resistance"]},"K":{"side":"SUFFIX","effect":"(9.1—13) Life Regeneration per second","family":["LifeRegeneration"],"tags":["flat_life_regen","resource","life"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","requiredStatRows":["local_base_evasion_rating","local_base_physical_damage_reduction_rating","base_fire_damage_resistance_%"],"actions":["exalt.A","exalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2],"uniqueFull":2,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 14 · Desert Buckler · side-2 · survey

기대 경로 8개.
```json
[["dextral_exaltation.A","exalt.B"],["dextral_exaltation.A","sinistral_exaltation.B"],["exalt.A","exalt.B"],["exalt.A","sinistral_exaltation.B"],["exalt.B","dextral_exaltation.A"],["exalt.B","exalt.A"],["sinistral_exaltation.B","dextral_exaltation.A"],["sinistral_exaltation.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `fd90d7030706` · 입력 JSON
```json
{"base":"Desert Buckler","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"K":{"side":"SUFFIX","effect":"+3% to Maximum Cold Resistance","family":["MaximumColdResist"],"tags":["elemental","cold","resistance"]},"A":{"side":"SUFFIX","effect":"+(207—253) to Stun Threshold","family":["StunThreshold"],"tags":[]},"B":{"side":"PREFIX","effect":"+(150—174) to maximum Life","family":["IncreasedLife"],"tags":["life"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","dextral_exaltation.A","sinistral_exaltation.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","dextral_exaltation.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 접미만 추가","sinistral_exaltation.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 접두만 추가"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"dextral_exaltation.A\",\"exalt.B\"],[\"sinistral_exaltation.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3],"uniqueFull":3,"missingFull":5,"bad":[],"prefix":[]}
```

### 호출 15 · Guardian Bow · implicit-2 · survey

기대 경로 2개.
```json
[["exalt.A","exalt.B"],["exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `16e0b76b083f` · 입력 JSON
```json
{"base":"Guardian Bow","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"(25—35)% chance to Chain an additional time","tags":[],"stats":[{"id":"local_additional_attack_chain_chance_%","min":25,"max":35}]},"mods":{"A":{"side":"SUFFIX","effect":"+(31—33) to Dexterity","family":["Dexterity"],"tags":["attribute"]},"B":{"side":"PREFIX","effect":"Adds (4—6) to (7—10) Fire Damage","family":["FireDamage"],"tags":["elemental_damage","damage","elemental","fire","attack"]},"K":{"side":"SUFFIX","effect":"+(25—50)% Surpassing chance to fire an additional Arrow","family":["AdditionalArrows"],"tags":["attack"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","keepImplicitStat":{"id":"local_additional_attack_chain_chance_%","min":25,"max":35},"actions":["exalt.A","exalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2],"uniqueFull":2,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 16 · Aegis Quarterstaff · fracture-2 · survey

기대 경로 4개.
```json
[["exalt.A","exalt.B","fracture.K"],["exalt.A","fracture.K","exalt.B"],["exalt.B","exalt.A","fracture.K"],["exalt.B","fracture.K","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `5906d0002371` · 입력 JSON
```json
{"base":"Aegis Quarterstaff","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"+(12—18)% to Block chance","tags":[],"stats":[{"id":"additional_block_%","min":12,"max":18}]},"mods":{"K":{"side":"PREFIX","effect":"(40—49)% increased Physical Damage","family":["LocalPhysicalDamagePercent"],"tags":["damage","physical","attack"]},"J":{"side":"SUFFIX","effect":"+(25—27) to Intelligence","family":["Intelligence"],"tags":["attribute"]},"L":{"side":"SUFFIX","effect":"Causes (31—40)% increased Stun Buildup","family":["StunDamageIncrease"],"tags":[]},"A":{"side":"PREFIX","effect":"Adds (25—30) to (38—46) Cold Damage","family":["ColdDamage"],"tags":["damage","elemental","cold","attack"]},"B":{"side":"PREFIX","effect":"Adds (102—130) to (155—198) Fire Damage","family":["FireDamage"],"tags":["damage","elemental","fire","attack"]}},"start":["K","J","L"],"acquire":["A","B"],"remove":[],"keep":["K","J","L"],"goalRarity":"RARE","mustFracture":"K","actions":["exalt.A","exalt.B","fracture.K"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","fracture.K":"Rare 명시 4개 이상에서 보유 K가 분열되는 결과. 옵션 수는 그대로다."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\",\"fracture.K\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\",\"fracture.K\"],[\"exalt.A\",\"fracture.K\",\"exalt.B\"],[\"exalt.B\",\"fracture.K\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3,4],"uniqueFull":4,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 17 · Plate Belt · grade-1 · repair

기대 경로는 호출 3의 10개와 같다.

시스템 문안 `70289bfe0774` · 응답 스키마 `15e44c597944` · 입력 JSON
```json
{"base":"Plate Belt","ilvl":82,"rarity":"NORMAL","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"+(140—180) to Armour","tags":["defences","armour"],"stats":[{"id":"base_physical_damage_reduction_rating","min":140,"max":180}]},"mods":{"A":{"side":"PREFIX","effect":"+(226—267) to Armour","family":["IncreasedPhysicalDamageReductionRating"],"tags":["defences","armour"]},"B":{"side":"PREFIX","effect":"(1—2) to (3—4) Physical Thorns damage","family":["Thorns"],"tags":["physical_damage","damage","physical"]},"C":{"side":"SUFFIX","effect":"+(254—304) to Stun Threshold","family":["StunThreshold"],"tags":[]}},"start":[],"acquire":["A","B","C"],"remove":[],"keep":[],"goalRarity":"RARE","actions":["transmute.A","transmute.B","transmute.C","augment.A","augment.B","augment.C","regal.A","regal.B","regal.C","exalt.A","exalt.B","exalt.C"],"priorRejected":[[["transmute.A","augment.B","regal.C","exalt.A"],"MAGIC/A 에서 augment.B 실행 불가"],[["transmute.B","augment.A","regal.B","exalt.C"],"MAGIC/B 에서 augment.A 실행 불가"],[["transmute.C","augment.C","regal.A","exalt.B"],"MAGIC/C 에서 augment.C 실행 불가"]],"remainingSides":{"A":"PREFIX","B":"PREFIX","C":"SUFFIX"},"availableCurrency":{"TRANSMUTATION":"Normal → Magic, 일반 옵션 하나 추가.","AUGMENTATION":"Magic, Magic 자리 한도를 유지하고 일반 옵션 하나 추가.","REGAL":"Magic → Rare, Rare 한도로 일반 옵션 하나 추가.","EXALTED":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"witness\":[[\"transmute.A\",\"transmute.B\",\"transmute.C\"],[\"regal.A\",\"regal.B\",\"regal.C\"]],\"other\":[]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[],"uniqueFull":0,"missingFull":10,"bad":[[1,"MAGIC/A 에서 transmute.B 실행 불가"],[2,"NORMAL/ 에서 regal.A 실행 불가"]],"prefix":[]}
```

### 호출 18 · Blacksteel Crest Shield · grade-2 · repair

기대 경로는 호출 11의 10개와 같다.

시스템 문안 `70289bfe0774` · 응답 스키마 `15e44c597944` · 입력 JSON
```json
{"base":"Blacksteel Crest Shield","ilvl":82,"rarity":"NORMAL","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"A":{"side":"SUFFIX","effect":"+(21—25)% to Lightning Resistance","family":["LightningResistance"],"tags":["elemental","lightning","resistance"]},"B":{"side":"SUFFIX","effect":"+(20—25)% of Armour also applies to Elemental Damage","family":["ArmourAppliesToElementalDamage"],"tags":["armour","elemental"]},"C":{"side":"PREFIX","effect":"(101—110)% increased Armour and Energy Shield","family":["DefencesPercent"],"tags":["armour","energy_shield"]}},"start":[],"acquire":["A","B","C"],"remove":[],"keep":[],"goalRarity":"RARE","actions":["transmute.A","transmute.B","transmute.C","augment.A","augment.B","augment.C","regal.A","regal.B","regal.C","exalt.A","exalt.B","exalt.C"],"priorRejected":[[["transmute.C","augment.C","regal.C","exalt.C"],"MAGIC/C 에서 augment.C 실행 불가"],[["transmute.A","augment.A","regal.A","exalt.A"],"MAGIC/A 에서 augment.A 실행 불가"],[["transmute.B","augment.B","regal.B","exalt.B"],"MAGIC/B 에서 augment.B 실행 불가"]],"remainingSides":{"A":"SUFFIX","B":"SUFFIX","C":"PREFIX"},"availableCurrency":{"TRANSMUTATION":"Normal → Magic, 일반 옵션 하나 추가.","AUGMENTATION":"Magic, Magic 자리 한도를 유지하고 일반 옵션 하나 추가.","REGAL":"Magic → Rare, Rare 한도로 일반 옵션 하나 추가.","EXALTED":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"witness\":[[\"transmute.C\",\"augment.C\",\"regal.C\"]],\"other\":[]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[],"uniqueFull":0,"missingFull":10,"bad":[[1,"MAGIC/C 에서 augment.C 실행 불가"]],"prefix":[]}
```

### 호출 19 · Plate Belt · grade-1 · compact

기대 경로는 호출 3의 10개와 같다.

시스템 문안 `e82e9011115d` · 응답 스키마 `c2b4d1b28c15` · 입력 JSON
```json
{"base":"Plate Belt","ilvl":82,"rarity":"NORMAL","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"+(140—180) to Armour","tags":["defences","armour"],"stats":[{"id":"base_physical_damage_reduction_rating","min":140,"max":180}]},"mods":{"A":{"side":"PREFIX","effect":"+(226—267) to Armour","family":["IncreasedPhysicalDamageReductionRating"],"tags":["defences","armour"]},"B":{"side":"PREFIX","effect":"(1—2) to (3—4) Physical Thorns damage","family":["Thorns"],"tags":["physical_damage","damage","physical"]},"C":{"side":"SUFFIX","effect":"+(254—304) to Stun Threshold","family":["StunThreshold"],"tags":[]}},"start":[],"acquire":["A","B","C"],"remove":[],"keep":[],"goalRarity":"RARE","actions":["transmute.A","transmute.B","transmute.C","augment.A","augment.B","augment.C","regal.A","regal.B","regal.C","exalt.A","exalt.B","exalt.C"],"remainingSides":{"A":"PREFIX","B":"PREFIX","C":"SUFFIX"},"availableCurrency":{"TRANSMUTATION":"Normal → Magic, 일반 옵션 하나 추가.","AUGMENTATION":"Magic, Magic 자리 한도를 유지하고 일반 옵션 하나 추가.","REGAL":"Magic → Rare, Rare 한도로 일반 옵션 하나 추가.","EXALTED":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"A\":[{\"A\":\"1|transmute.A\",\"B\":\"2|regal.B\",\"C\":\"3|regal.C\"},{\"A\":\"1|transmute.A\",\"B\":\"3|regal.B\",\"C\":\"2|regal.C\"},{\"A\":\"1|transmute.A\",\"B\":\"2|augment.B\",\"C\":\"3|regal.C\"},{\"A\":\"1|transmute.A\",\"B\":\"3|exalt.B\",\"C\":\"2|regal.C\"}],\"B\":[{\"A\":\"2|regal.A\",\"B\":\"1|transmute.B\",\"C\":\"3|regal.C\"},{\"A\":\"3|regal.A\",\"B\":\"1|transmute.B\",\"C\":\"2|regal.C\"},{\"A\":\"2|augment.A\",\"B\":\"1|transmute.B\",\"C\":\"3|regal.C\"},{\"A\":\"3|exalt.A\",\"B\":\"1|transmute.B\",\"C\":\"2|regal.C\"}],\"C\":[{\"A\":\"2|regal.A\",\"B\":\"2|regal.B\",\"C\":\"1|transmute.C\"},{\"A\":\"3|regal.A\",\"B\":\"2|augment.B\",\"C\":\"1|transmute.C\"},{\"A\":\"2|augment.A\",\"B\":\"3|regal.B\",\"C\":\"1|transmute.C\"},{\"A\":\"3|exalt.A\",\"B\":\"3|exalt.B\",\"C\":\"1|transmute.C\"}]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[4,8,10,11],"uniqueFull":4,"missingFull":6,"bad":[[1,"RARE/A,B 에서 regal.C 실행 불가"],[2,"RARE/A,C 에서 regal.B 실행 불가"],[3,"MAGIC/A 에서 augment.B 실행 불가"],[5,"RARE/A,B 에서 regal.C 실행 불가"],[6,"RARE/B,C 에서 regal.A 실행 불가"],[7,"MAGIC/B 에서 augment.A 실행 불가"],[9,"NORMAL/ 에서 invalidRank 실행 불가"],[12,"NORMAL/ 에서 invalidRank 실행 불가"]],"prefix":[]}
```

### 호출 20 · Blacksteel Crest Shield · grade-2 · compact

기대 경로는 호출 11의 10개와 같다.

시스템 문안 `e82e9011115d` · 응답 스키마 `c2b4d1b28c15` · 입력 JSON
```json
{"base":"Blacksteel Crest Shield","ilvl":82,"rarity":"NORMAL","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"A":{"side":"SUFFIX","effect":"+(21—25)% to Lightning Resistance","family":["LightningResistance"],"tags":["elemental","lightning","resistance"]},"B":{"side":"SUFFIX","effect":"+(20—25)% of Armour also applies to Elemental Damage","family":["ArmourAppliesToElementalDamage"],"tags":["armour","elemental"]},"C":{"side":"PREFIX","effect":"(101—110)% increased Armour and Energy Shield","family":["DefencesPercent"],"tags":["armour","energy_shield"]}},"start":[],"acquire":["A","B","C"],"remove":[],"keep":[],"goalRarity":"RARE","actions":["transmute.A","transmute.B","transmute.C","augment.A","augment.B","augment.C","regal.A","regal.B","regal.C","exalt.A","exalt.B","exalt.C"],"remainingSides":{"A":"SUFFIX","B":"SUFFIX","C":"PREFIX"},"availableCurrency":{"TRANSMUTATION":"Normal → Magic, 일반 옵션 하나 추가.","AUGMENTATION":"Magic, Magic 자리 한도를 유지하고 일반 옵션 하나 추가.","REGAL":"Magic → Rare, Rare 한도로 일반 옵션 하나 추가.","EXALTED":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"A\":[{\"A\":\"1|transmute.A\",\"B\":\"2|regal.B\",\"C\":\"3|regal.C\"},{\"A\":\"1|transmute.A\",\"B\":\"3|regal.B\",\"C\":\"2|regal.C\"},{\"A\":\"1|transmute.A\",\"B\":\"2|augment.B\",\"C\":\"3|regal.C\"},{\"A\":\"1|transmute.A\",\"B\":\"3|exalt.B\",\"C\":\"2|regal.C\"}],\"B\":[{\"A\":\"2|regal.A\",\"B\":\"1|transmute.B\",\"C\":\"3|regal.C\"},{\"A\":\"3|regal.A\",\"B\":\"1|transmute.B\",\"C\":\"2|regal.C\"},{\"A\":\"2|augment.A\",\"B\":\"1|transmute.B\",\"C\":\"3|regal.C\"},{\"A\":\"3|exalt.A\",\"B\":\"1|transmute.B\",\"C\":\"2|regal.C\"}],\"C\":[{\"A\":\"2|regal.A\",\"B\":\"3|regal.B\",\"C\":\"1|transmute.C\"},{\"A\":\"3|regal.A\",\"B\":\"2|regal.B\",\"C\":\"1|transmute.C\"},{\"A\":\"2|augment.A\",\"B\":\"3|regal.B\",\"C\":\"1|transmute.C\"},{\"A\":\"3|exalt.A\",\"B\":\"2|regal.B\",\"C\":\"1|transmute.C\"}]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[4,8,11,12],"uniqueFull":4,"missingFull":6,"bad":[[1,"RARE/A,B 에서 regal.C 실행 불가"],[2,"RARE/A,C 에서 regal.B 실행 불가"],[3,"MAGIC/A 에서 augment.B 실행 불가"],[5,"RARE/A,B 에서 regal.C 실행 불가"],[6,"RARE/B,C 에서 regal.A 실행 불가"],[7,"MAGIC/B 에서 augment.A 실행 불가"],[9,"RARE/A,C 에서 regal.B 실행 불가"],[10,"RARE/B,C 에서 regal.A 실행 불가"]],"prefix":[]}
```

### 호출 21 · Strife Pick · room-3 · survey

기대 경로 6개.
```json
[["annul.X","exalt.A","exalt.B"],["annul.X","exalt.B","exalt.A"],["exalt.B","annul.X","exalt.A"],["exalt.B","sideAnnul.X","exalt.A"],["sideAnnul.X","exalt.A","exalt.B"],["sideAnnul.X","exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `ade86c24d889` · 입력 JSON
```json
{"base":"Strife Pick","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"+(5—10)% to Critical Damage Bonus","tags":["damage","attack","critical"],"stats":[{"id":"local_critical_strike_multiplier_+","min":5,"max":10}]},"mods":{"X":{"side":"PREFIX","effect":"(65—84)% increased Physical Damage","family":["LocalPhysicalDamagePercent"],"tags":["damage","physical","attack"]},"K0":{"side":"PREFIX","effect":"Adds (6—9) to (10—16) Cold Damage","family":["ColdDamage"],"tags":["damage","elemental","cold","attack"]},"K1":{"side":"PREFIX","effect":"(63—72)% increased Elemental Damage with Attacks","family":["IncreasedWeaponElementalDamagePercent"],"tags":["damage","elemental","fire","cold","lightning"]},"A":{"side":"PREFIX","effect":"Adds (13—20) to (23—35) Physical Damage","family":["PhysicalDamage"],"tags":["damage","physical","attack"]},"B":{"side":"SUFFIX","effect":"Leeches (5—5.9)% of Physical Damage as Mana","family":["ManaLeech"],"tags":["mana","physical","attack"]}},"start":["X","K0","K1"],"acquire":["A","B"],"remove":["X"],"keep":["K0","K1"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","annul.X","sideAnnul.X"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","annul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","sideAnnul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다. 접두만 제거"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\",\"annul.X\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\",\"annul.X\"],[\"exalt.A\",\"sideAnnul.X\",\"exalt.B\"],[\"exalt.B\",\"sideAnnul.X\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[4],"uniqueFull":1,"missingFull":5,"bad":[[1,"RARE/K0,K1,X 에서 exalt.A 실행 불가"],[2,"RARE/B,K0,K1,X 에서 exalt.A 실행 불가"],[3,"RARE/K0,K1,X 에서 exalt.A 실행 불가"]],"prefix":[]}
```

### 호출 22 · Tasalian Focus · tag-3 · survey

기대 경로 6개.
```json
[["exalt.A","exalt.B"],["exalt.A","tagExalt.B"],["exalt.B","exalt.A"],["exalt.B","tagExalt.A"],["tagExalt.A","exalt.B"],["tagExalt.A","tagExalt.B"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `ba524f263568` · 입력 JSON
```json
{"base":"Tasalian Focus","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"K":{"side":"SUFFIX","effect":"+(6—10)% to Cold Resistance","family":["ColdResistance"],"tags":["elemental","cold","resistance"]},"A":{"side":"PREFIX","effect":"(35—44)% increased Lightning Damage","family":["WeaponDamageTypePrefix"],"tags":["damage","elemental","lightning"]},"B":{"side":"PREFIX","effect":"(25—34)% increased Spell Damage","family":["WeaponCasterDamagePrefix"],"tags":["damage","caster"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","tagExalt.A","tagExalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","tagExalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 현재 implicit·explicit 태그 합집합이 비지 않으면 그중 하나 이상 겹치는 옵션만 추가","tagExalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 현재 implicit·explicit 태그 합집합이 비지 않으면 그중 하나 이상 겹치는 옵션만 추가"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"],[\"tagExalt.A\",\"exalt.B\"],[\"exalt.A\",\"tagExalt.B\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3,4],"uniqueFull":4,"missingFull":2,"bad":[],"prefix":[]}
```

### 호출 23 · Volant Quiver · grade-3 · compact

기대 경로 10개.
```json
[["transmute.A","augment.C","regal.B"],["transmute.A","regal.B","exalt.C"],["transmute.A","regal.C","exalt.B"],["transmute.B","augment.C","regal.A"],["transmute.B","regal.A","exalt.C"],["transmute.B","regal.C","exalt.A"],["transmute.C","augment.A","regal.B"],["transmute.C","augment.B","regal.A"],["transmute.C","regal.A","exalt.B"],["transmute.C","regal.B","exalt.A"]]
```

시스템 문안 `e82e9011115d` · 응답 스키마 `c2b4d1b28c15` · 입력 JSON
```json
{"base":"Volant Quiver","ilvl":82,"rarity":"NORMAL","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"(20—30)% increased Arrow Speed","tags":["attack","speed"],"stats":[{"id":"base_arrow_speed_+%","min":20,"max":30}]},"mods":{"A":{"side":"PREFIX","effect":"Adds (1—2) to 3 Fire damage to Attacks","family":["FireDamage"],"tags":["damage","elemental","fire","attack"]},"B":{"side":"PREFIX","effect":"Adds (5—7) to (9—13) Physical Damage to Attacks","family":["PhysicalDamage"],"tags":["damage","physical","attack"]},"C":{"side":"SUFFIX","effect":"Gain (15—20) Mana per enemy killed","family":["ManaGainedFromEnemyDeath"],"tags":["mana"]}},"start":[],"acquire":["A","B","C"],"remove":[],"keep":[],"goalRarity":"RARE","actions":["transmute.A","transmute.B","transmute.C","augment.A","augment.B","augment.C","regal.A","regal.B","regal.C","exalt.A","exalt.B","exalt.C"],"remainingSides":{"A":"PREFIX","B":"PREFIX","C":"SUFFIX"},"availableCurrency":{"TRANSMUTATION":"Normal → Magic, 일반 옵션 하나 추가.","AUGMENTATION":"Magic, Magic 자리 한도를 유지하고 일반 옵션 하나 추가.","REGAL":"Magic → Rare, Rare 한도로 일반 옵션 하나 추가.","EXALTED":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"A\":[{\"A\":\"1|transmute.A\",\"B\":\"2|regal.B\",\"C\":\"3|regal.C\"}],\"B\":[{\"A\":\"2|regal.A\",\"B\":\"1|transmute.B\",\"C\":\"3|regal.C\"}],\"C\":[{\"A\":\"2|regal.A\",\"B\":\"2|regal.B\",\"C\":\"1|transmute.C\"}]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[],"uniqueFull":0,"missingFull":10,"bad":[[1,"RARE/A,B 에서 regal.C 실행 불가"],[2,"RARE/A,B 에서 regal.C 실행 불가"],[3,"NORMAL/ 에서 invalidRank 실행 불가"]],"prefix":[]}
```

### 호출 24 · Elegant Crossbow · swap-3 · survey

기대 경로 5개.
```json
[["annul.X","exalt.A","exalt.B"],["annul.X","exalt.B","exalt.A"],["chaos.X>A","exalt.B"],["exalt.B","annul.X","exalt.A"],["exalt.B","chaos.X>A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `19f6d754cc2f` · 입력 JSON
```json
{"base":"Elegant Crossbow","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"(20—30)% chance to Pierce an Enemy","tags":[],"stats":[{"id":"base_chance_to_pierce_%","min":20,"max":30}]},"mods":{"A":{"side":"PREFIX","effect":"Adds (1—4) to (53—76) Lightning Damage","family":["LightningDamage"],"tags":["damage","elemental","lightning","attack"]},"X":{"side":"PREFIX","effect":"Adds (1—3) to (31—43) Lightning Damage","family":["LightningDamage"],"tags":["damage","elemental","lightning","attack"]},"K":{"side":"SUFFIX","effect":"Gain (19—28) Life per enemy killed","family":["LifeGainedFromEnemyDeath"],"tags":["life"]},"B":{"side":"SUFFIX","effect":"+(10—11)% to Critical Damage Bonus","family":["CriticalStrikeMultiplier"],"tags":["damage","attack","critical"]}},"start":["K","X"],"acquire":["A","B"],"remove":["X"],"keep":["K"],"goalRarity":"RARE","actions":["annul.X","exalt.A","exalt.B","chaos.X>A"],"actionRules":{"annul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","chaos.X>A":"Rare에서 지정한 기존 옵션이 제거되고 이어 목표 옵션이 뽑힌 한 번의 교체."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"annul.X\",\"exalt.A\",\"exalt.B\"],[\"chaos.X>A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.A\",\"annul.X\",\"exalt.B\"],[\"exalt.B\",\"annul.X\",\"exalt.A\"],[\"chaos.X>A\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,4],"uniqueFull":3,"missingFull":2,"bad":[[3,"RARE/K,X 에서 exalt.A 실행 불가"],[5,"RARE/A,K 에서 exalt.A 실행 불가"]],"prefix":[]}
```

### 호출 25 · Death Mail · hybrid-3 · survey

기대 경로 2개.
```json
[["exalt.A","exalt.B"],["exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `16e0b76b083f` · 입력 JSON
```json
{"base":"Death Mail","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"A":{"side":"PREFIX","effect":"+(150—170) to Armour\n+(142—161) to Evasion Rating","family":["BaseLocalDefences"],"tags":["defences","armour","evasion"]},"B":{"side":"SUFFIX","effect":"+(25—27) to Strength","family":["Strength"],"tags":["attribute"]},"K":{"side":"SUFFIX","effect":"+(31—35)% to Fire Resistance","family":["FireResistance"],"tags":["elemental_resistance","fire_resistance","elemental","fire","resistance"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","requiredStatRows":["local_base_evasion_rating","local_base_physical_damage_reduction_rating","additional_strength"],"actions":["exalt.A","exalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2],"uniqueFull":2,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 26 · Forking Belt · side-3 · survey

기대 경로 8개.
```json
[["dextral_exaltation.B","exalt.A"],["dextral_exaltation.B","sinistral_exaltation.A"],["exalt.A","dextral_exaltation.B"],["exalt.A","exalt.B"],["exalt.B","exalt.A"],["exalt.B","sinistral_exaltation.A"],["sinistral_exaltation.A","dextral_exaltation.B"],["sinistral_exaltation.A","exalt.B"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `8e0e79b42f01` · 입력 JSON
```json
{"base":"Forking Belt","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"Adds 1 to (20—30) Lightning damage to Attacks\nHas 1 Charm Slots","tags":["elemental_damage","damage","elemental","lightning","attack","charm"],"stats":[{"id":"attack_maximum_added_lightning_damage","min":20,"max":30},{"id":"attack_minimum_added_lightning_damage","min":1,"max":1},{"id":"local_charm_slots","min":1,"max":1}]},"mods":{"K":{"side":"PREFIX","effect":"+(20—29) to maximum Life","family":["IncreasedLife"],"tags":["resource","life"]},"A":{"side":"PREFIX","effect":"+(35—54) to maximum Mana","family":["IncreasedMana"],"tags":["resource","mana"]},"B":{"side":"SUFFIX","effect":"(17—22)% increased Charm Charges gained","family":["BeltCharmChargesGained"],"tags":["charm"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","sinistral_exaltation.A","dextral_exaltation.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","sinistral_exaltation.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 접두만 추가","dextral_exaltation.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 접미만 추가"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"],[\"sinistral_exaltation.A\",\"dextral_exaltation.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"],[\"dextral_exaltation.B\",\"sinistral_exaltation.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3,4],"uniqueFull":4,"missingFull":4,"bad":[],"prefix":[]}
```

### 호출 27 · Time-Lost Sapphire · implicit-3 · survey

기대 경로 2개.
```json
[["exalt.A","exalt.B"],["exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `16e0b76b083f` · 입력 JSON
```json
{"base":"Time-Lost Sapphire","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[2,2]},"implicit":{"effect":"local jewel effect base radius [1000]","tags":["unscalable","passive-radius-condition"],"stats":[{"id":"local_jewel_effect_base_radius","min":1000,"max":1000}]},"mods":{"A":{"side":"PREFIX","effect":"Notable Passive Skills in Radius also grant (5—10)% increased Magnitude of Damaging Ailments you inflict with Critical Hits","family":["CriticalAilmentEffect"],"tags":["damage","critical","ailment","passive-radius-condition"]},"B":{"side":"SUFFIX","effect":"Small Passive Skills in Radius also grant Minions have +(1—2)% to all Elemental Resistances","family":["MinionElementalResistances"],"tags":["cold_resistance","elemental_resistance","fire_resistance","lightning_resistance","minion_resistance","elemental","fire","cold","lightning","resistance","minion","passive-radius-condition"]},"K":{"side":"PREFIX","effect":"Small Passive Skills in Radius also grant Offerings have (2—3)% increased Maximum Life","family":["OfferingLife"],"tags":["resource","life","minion","passive-radius-condition"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","keepImplicitStat":{"id":"local_jewel_effect_base_radius","min":1000,"max":1000},"actions":["exalt.A","exalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2],"uniqueFull":2,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 28 · Grinning Mask · fracture-3 · survey

기대 경로 4개.
```json
[["exalt.A","exalt.B","fracture.K"],["exalt.A","fracture.K","exalt.B"],["exalt.B","exalt.A","fracture.K"],["exalt.B","fracture.K","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `5906d0002371` · 입력 JSON
```json
{"base":"Grinning Mask","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"K":{"side":"PREFIX","effect":"+(40—59) to maximum Life","family":["IncreasedLife"],"tags":["resource","life"]},"J":{"side":"SUFFIX","effect":"(1—2) Life Regeneration per second","family":["LifeRegeneration"],"tags":["flat_life_regen","resource","life"]},"L":{"side":"PREFIX","effect":"(33—38)% increased Evasion and Energy Shield\n+(27—32) to maximum Mana","family":["BaseLocalDefencesAndMana"],"tags":["defences","resource","mana","evasion","energy_shield"]},"A":{"side":"SUFFIX","effect":"+(25—27) to Intelligence","family":["Intelligence"],"tags":["attribute"]},"B":{"side":"SUFFIX","effect":"+(21—25)% to Lightning Resistance","family":["LightningResistance"],"tags":["elemental_resistance","lightning_resistance","elemental","lightning","resistance"]}},"start":["K","J","L"],"acquire":["A","B"],"remove":[],"keep":["K","J","L"],"goalRarity":"RARE","mustFracture":"K","actions":["exalt.A","exalt.B","fracture.K"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","fracture.K":"Rare 명시 4개 이상에서 보유 K가 분열되는 결과. 옵션 수는 그대로다."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\",\"fracture.K\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\",\"fracture.K\"],[\"exalt.A\",\"fracture.K\",\"exalt.B\"],[\"exalt.B\",\"fracture.K\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3,4],"uniqueFull":4,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 29 · Ruby · room-4 · survey

기대 경로 15개.
```json
[["annul.X","annul.Y","exalt.A","exalt.B"],["annul.X","annul.Y","exalt.B","exalt.A"],["annul.X","exalt.A","annul.Y","exalt.B"],["annul.X","exalt.A","exalt.B","annul.Y"],["annul.X","exalt.B","annul.Y","exalt.A"],["annul.X","exalt.B","exalt.A","annul.Y"],["annul.Y","annul.X","exalt.A","exalt.B"],["annul.Y","annul.X","exalt.B","exalt.A"],["annul.Y","exalt.B","annul.X","exalt.A"],["doubleAnnul.X+Y","exalt.A","exalt.B"],["doubleAnnul.X+Y","exalt.B","exalt.A"],["exalt.B","annul.X","annul.Y","exalt.A"],["exalt.B","annul.X","exalt.A","annul.Y"],["exalt.B","annul.Y","annul.X","exalt.A"],["exalt.B","doubleAnnul.X+Y","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `740f5afe337e` · 입력 JSON
```json
{"base":"Ruby","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[2,2]},"implicit":null,"mods":{"X":{"side":"SUFFIX","effect":"(15—25)% increased Stun Buildup with Maces","family":["MaceStun"],"tags":["attack"]},"K0":{"side":"SUFFIX","effect":"(10—20)% increased Warcry Speed","family":["WarcrySpeed"],"tags":["speed"]},"A":{"side":"SUFFIX","effect":"(10—20)% increased Flammability Magnitude","family":["IgniteChanceIncrease"],"tags":["elemental","fire","ailment"]},"B":{"side":"PREFIX","effect":"(10—20)% increased Damage with Warcries","family":["WarcryDamage"],"tags":["damage"]},"Y":{"side":"PREFIX","effect":"(5—15)% increased Damage with Plant Skills","family":["DamageForm"],"tags":["damage"]}},"start":["X","K0","Y"],"acquire":["A","B"],"remove":["X","Y"],"keep":["K0"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","annul.X","annul.Y","doubleAnnul.X+Y"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","annul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","annul.Y":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","doubleAnnul.X+Y":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다. 한 번에 서로 다른 두 개 제거"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\",\"annul.X\",\"annul.Y\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\",\"annul.Y\",\"annul.X\"],[\"exalt.A\",\"annul.X\",\"exalt.B\",\"annul.Y\"],[\"exalt.B\",\"annul.Y\",\"exalt.A\",\"annul.X\"],[\"doubleAnnul.X+Y\",\"exalt.A\",\"exalt.B\"],[\"doubleAnnul.X+Y\",\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[5,6],"uniqueFull":2,"missingFull":13,"bad":[[1,"RARE/K0,X,Y 에서 exalt.A 실행 불가"],[2,"RARE/B,K0,X,Y 에서 exalt.A 실행 불가"],[3,"RARE/K0,X,Y 에서 exalt.A 실행 불가"],[4,"RARE/B,K0,X 에서 exalt.A 실행 불가"]],"prefix":[]}
```

### 호출 30 · Hallowed Sceptre · tag-4 · survey

기대 경로 6개.
```json
[["exalt.A","exalt.B"],["exalt.A","tagExalt.B"],["exalt.B","exalt.A"],["exalt.B","tagExalt.A"],["tagExalt.A","exalt.B"],["tagExalt.A","tagExalt.B"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `ba524f263568` · 입력 JSON
```json
{"base":"Hallowed Sceptre","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"K":{"side":"SUFFIX","effect":"(30—39)% increased Mana Regeneration Rate","family":["ManaRegeneration"],"tags":["resource","mana"]},"A":{"side":"SUFFIX","effect":"Minions have (21—25)% increased maximum Life","family":["MinionLife"],"tags":["resource","life","minion"]},"B":{"side":"SUFFIX","effect":"+1 to Level of all Minion Skills","family":["IncreaseSocketedGemLevel"],"tags":["minion","gem"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","tagExalt.A","tagExalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","tagExalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 현재 implicit·explicit 태그 합집합이 비지 않으면 그중 하나 이상 겹치는 옵션만 추가","tagExalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 현재 implicit·explicit 태그 합집합이 비지 않으면 그중 하나 이상 겹치는 옵션만 추가"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"],[\"tagExalt.A\",\"exalt.B\"],[\"exalt.A\",\"tagExalt.B\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3,4],"uniqueFull":4,"missingFull":2,"bad":[],"prefix":[]}
```

### 호출 31 · Faithful Leggings · grade-4 · compact

기대 경로 10개.
```json
[["transmute.A","augment.C","regal.B"],["transmute.A","regal.B","exalt.C"],["transmute.A","regal.C","exalt.B"],["transmute.B","augment.C","regal.A"],["transmute.B","regal.A","exalt.C"],["transmute.B","regal.C","exalt.A"],["transmute.C","augment.A","regal.B"],["transmute.C","augment.B","regal.A"],["transmute.C","regal.A","exalt.B"],["transmute.C","regal.B","exalt.A"]]
```

시스템 문안 `e82e9011115d` · 응답 스키마 `c2b4d1b28c15` · 입력 JSON
```json
{"base":"Faithful Leggings","ilvl":82,"rarity":"NORMAL","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"A":{"side":"SUFFIX","effect":"+(41—45)% to Cold Resistance","family":["ColdResistance"],"tags":["cold_resistance","elemental_resistance","elemental","cold","resistance"]},"B":{"side":"SUFFIX","effect":"(56—60)% reduced Freeze Duration on you","family":["ReducedAilmentDuration"],"tags":["elemental","cold","ailment"]},"C":{"side":"PREFIX","effect":"+(120—149) to maximum Life","family":["IncreasedLife"],"tags":["resource","life"]}},"start":[],"acquire":["A","B","C"],"remove":[],"keep":[],"goalRarity":"RARE","actions":["transmute.A","transmute.B","transmute.C","augment.A","augment.B","augment.C","regal.A","regal.B","regal.C","exalt.A","exalt.B","exalt.C"],"remainingSides":{"A":"SUFFIX","B":"SUFFIX","C":"PREFIX"},"availableCurrency":{"TRANSMUTATION":"Normal → Magic, 일반 옵션 하나 추가.","AUGMENTATION":"Magic, Magic 자리 한도를 유지하고 일반 옵션 하나 추가.","REGAL":"Magic → Rare, Rare 한도로 일반 옵션 하나 추가.","EXALTED":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"A\":[{\"A\":\"1|transmute.A\",\"B\":\"2|regal.B\",\"C\":\"3|regal.C\"}],\"B\":[{\"A\":\"2|regal.A\",\"B\":\"1|transmute.B\",\"C\":\"3|regal.C\"}],\"C\":[{\"A\":\"2|regal.A\",\"B\":\"3|regal.B\",\"C\":\"1|transmute.C\"}]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[],"uniqueFull":0,"missingFull":10,"bad":[[1,"RARE/A,B 에서 regal.C 실행 불가"],[2,"RARE/A,B 에서 regal.C 실행 불가"],[3,"RARE/A,C 에서 regal.B 실행 불가"]],"prefix":[]}
```

### 호출 32 · Bolting Quarterstaff · swap-4 · survey

기대 경로 5개.
```json
[["annul.X","exalt.A","exalt.B"],["annul.X","exalt.B","exalt.A"],["chaos.X>A","exalt.B"],["exalt.B","annul.X","exalt.A"],["exalt.B","chaos.X>A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `19f6d754cc2f` · 입력 JSON
```json
{"base":"Bolting Quarterstaff","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"local weapon implicit hidden added maximum lightning damage [100]\nlocal weapon implicit hidden added minimum lightning damage [1]","tags":[],"stats":[{"id":"local_weapon_implicit_hidden_added_maximum_lightning_damage","min":100,"max":100},{"id":"local_weapon_implicit_hidden_added_minimum_lightning_damage","min":1,"max":1}]},"mods":{"A":{"side":"SUFFIX","effect":"+(13—16) to Intelligence","family":["Intelligence"],"tags":["attribute"]},"X":{"side":"SUFFIX","effect":"+(5—8) to Intelligence","family":["Intelligence"],"tags":["attribute"]},"K":{"side":"PREFIX","effect":"(20—24)% increased Physical Damage\n+(21—46) to Accuracy Rating","family":["LocalIncreasedPhysicalDamagePercentAndAccuracyRating"],"tags":["damage","physical","attack"]},"B":{"side":"PREFIX","effect":"+(347—450) to Accuracy Rating","family":["IncreasedAccuracy"],"tags":["attack"]}},"start":["K","X"],"acquire":["A","B"],"remove":["X"],"keep":["K"],"goalRarity":"RARE","actions":["annul.X","exalt.A","exalt.B","chaos.X>A"],"actionRules":{"annul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","chaos.X>A":"Rare에서 지정한 기존 옵션이 제거되고 이어 목표 옵션이 뽑힌 한 번의 교체."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"annul.X\",\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"chaos.X>A\",\"exalt.B\"],[\"annul.X\",\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3],"uniqueFull":3,"missingFull":2,"bad":[],"prefix":[]}
```

### 호출 33 · Fungal Talisman · hybrid-4 · survey

기대 경로 2개.
```json
[["exalt.A","exalt.B"],["exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `16e0b76b083f` · 입력 JSON
```json
{"base":"Fungal Talisman","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"A":{"side":"PREFIX","effect":"(35—44)% increased Physical Damage\n+(73—97) to Accuracy Rating","family":["LocalIncreasedPhysicalDamagePercentAndAccuracyRating"],"tags":["damage","physical","attack"]},"B":{"side":"SUFFIX","effect":"+(17—20) to Intelligence","family":["Intelligence"],"tags":["attribute"]},"K":{"side":"SUFFIX","effect":"Causes (71—80)% increased Stun Buildup","family":["StunDamageIncrease"],"tags":[]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","requiredStatRows":["local_accuracy_rating","local_physical_damage_+%","additional_intelligence"],"actions":["exalt.A","exalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2],"uniqueFull":2,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 34 · Perching Staff · side-4 · survey

기대 경로 8개.
```json
[["dextral_exaltation.A","exalt.B"],["dextral_exaltation.A","sinistral_exaltation.B"],["exalt.A","exalt.B"],["exalt.A","sinistral_exaltation.B"],["exalt.B","dextral_exaltation.A"],["exalt.B","exalt.A"],["sinistral_exaltation.B","dextral_exaltation.A"],["sinistral_exaltation.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `fd90d7030706` · 입력 JSON
```json
{"base":"Perching Staff","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"K":{"side":"SUFFIX","effect":"(26—31)% increased Cast Speed","family":["IncreasedCastSpeed"],"tags":["caster","speed"]},"A":{"side":"SUFFIX","effect":"(81—90)% increased chance to Shock","family":["ShockChanceIncrease"],"tags":["elemental","lightning","ailment"]},"B":{"side":"PREFIX","effect":"(69—88)% increased Fire Damage","family":["WeaponDamageTypePrefix"],"tags":["damage","elemental","fire"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","dextral_exaltation.A","sinistral_exaltation.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","dextral_exaltation.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 접미만 추가","sinistral_exaltation.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 접두만 추가"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"dextral_exaltation.A\",\"exalt.B\"],[\"sinistral_exaltation.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3],"uniqueFull":3,"missingFull":5,"bad":[],"prefix":[]}
```

### 호출 35 · Grand Spear · implicit-4 · survey

기대 경로 2개.
```json
[["exalt.A","exalt.B"],["exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `16e0b76b083f` · 입력 JSON
```json
{"base":"Grand Spear","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"local display grants spear throw skill [1]\n25% increased Melee Strike Range with this weapon","tags":[],"stats":[{"id":"local_display_grants_spear_throw_skill","min":1,"max":1},{"id":"local_+%_weapon_range","min":25,"max":25}]},"mods":{"A":{"side":"SUFFIX","effect":"(20—22)% increased Stun Duration","family":["StunDurationIncreasePercent"],"tags":[]},"B":{"side":"PREFIX","effect":"Adds (3—5) to (6—9) Cold Damage","family":["ColdDamage"],"tags":["damage","elemental","cold","attack"]},"K":{"side":"SUFFIX","effect":"Leeches (5—5.9)% of Physical Damage as Mana","family":["ManaLeech"],"tags":["mana","physical","attack"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","keepImplicitStat":{"id":"local_display_grants_spear_throw_skill","min":1,"max":1},"actions":["exalt.A","exalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2],"uniqueFull":2,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 36 · Fanatic Bow · fracture-4 · survey

기대 경로 4개.
```json
[["exalt.A","exalt.B","fracture.K"],["exalt.A","fracture.K","exalt.B"],["exalt.B","exalt.A","fracture.K"],["exalt.B","fracture.K","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `5906d0002371` · 입력 JSON
```json
{"base":"Fanatic Bow","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"local weapon implicit hidden added maximum chaos damage [64]\nlocal weapon implicit hidden added minimum chaos damage [28]","tags":[],"stats":[{"id":"local_weapon_implicit_hidden_added_maximum_chaos_damage","min":64,"max":64},{"id":"local_weapon_implicit_hidden_added_minimum_chaos_damage","min":28,"max":28}]},"mods":{"K":{"side":"PREFIX","effect":"Adds (16—24) to (28—42) Physical Damage","family":["PhysicalDamage"],"tags":["physical_damage","damage","physical","attack"]},"J":{"side":"SUFFIX","effect":"+(12—13)% to Critical Damage Bonus","family":["CriticalStrikeMultiplier"],"tags":["damage","attack","critical"]},"L":{"side":"SUFFIX","effect":"Gain (2—3) Mana per enemy killed","family":["ManaGainedFromEnemyDeath"],"tags":["resource","mana"]},"A":{"side":"PREFIX","effect":"Adds (1—3) to (55—60) Lightning Damage","family":["LightningDamage"],"tags":["elemental_damage","damage","elemental","lightning","attack"]},"B":{"side":"PREFIX","effect":"Adds (13—19) to (21—29) Fire Damage","family":["FireDamage"],"tags":["elemental_damage","damage","elemental","fire","attack"]}},"start":["K","J","L"],"acquire":["A","B"],"remove":[],"keep":["K","J","L"],"goalRarity":"RARE","mustFracture":"K","actions":["exalt.A","exalt.B","fracture.K"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","fracture.K":"Rare 명시 4개 이상에서 보유 K가 분열되는 결과. 옵션 수는 그대로다."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\",\"fracture.K\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\",\"fracture.K\"],[\"exalt.A\",\"fracture.K\",\"exalt.B\"],[\"exalt.B\",\"fracture.K\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3,4],"uniqueFull":4,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 37 · Volant Quiver · grade-3 · upgrade

기대 경로는 호출 23의 10개와 같다.

시스템 문안 `e38f7345a845` · 응답 스키마 `5e3d5b1e5e31` · 입력 JSON
```json
{"base":"Volant Quiver","ilvl":82,"rarity":"NORMAL","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"(20—30)% increased Arrow Speed","tags":["attack","speed"],"stats":[{"id":"base_arrow_speed_+%","min":20,"max":30}]},"mods":{"A":{"side":"PREFIX","effect":"Adds (1—2) to 3 Fire damage to Attacks","family":["FireDamage"],"tags":["damage","elemental","fire","attack"]},"B":{"side":"PREFIX","effect":"Adds (5—7) to (9—13) Physical Damage to Attacks","family":["PhysicalDamage"],"tags":["damage","physical","attack"]},"C":{"side":"SUFFIX","effect":"Gain (15—20) Mana per enemy killed","family":["ManaGainedFromEnemyDeath"],"tags":["mana"]}},"start":[],"acquire":["A","B","C"],"remove":[],"keep":[],"goalRarity":"RARE","actions":["transmute.A","transmute.B","transmute.C","augment.A","augment.B","augment.C","regal.A","regal.B","regal.C","exalt.A","exalt.B","exalt.C"],"priorRejected":[[["transmute.A","regal.B","regal.C"],"RARE/A,B 에서 regal.C 실행 불가"],[["transmute.B","regal.A","regal.C"],"RARE/A,B 에서 regal.C 실행 불가"],[["invalidRank"],"NORMAL/ 에서 invalidRank 실행 불가"]],"remainingSides":{"A":"PREFIX","B":"PREFIX","C":"SUFFIX"},"availableCurrency":{"TRANSMUTATION":"Normal → Magic, 일반 옵션 하나 추가.","AUGMENTATION":"Magic, Magic 자리 한도를 유지하고 일반 옵션 하나 추가.","REGAL":"Magic → Rare, Rare 한도로 일반 옵션 하나 추가.","EXALTED":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"A\":[{\"first\":\"1|transmute.A\",\"upgrade\":\"2|regal.B\",\"other\":\"3|exalt.C\"}],\"B\":[{\"first\":\"1|transmute.B\",\"upgrade\":\"2|regal.A\",\"other\":\"3|exalt.C\"}],\"C\":[{\"first\":\"1|transmute.C\",\"upgrade\":\"2|regal.A\",\"other\":\"3|exalt.B\"}]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3],"uniqueFull":3,"missingFull":7,"bad":[],"prefix":[]}
```

### 호출 38 · Faithful Leggings · grade-4 · upgrade

기대 경로는 호출 31의 10개와 같다.

시스템 문안 `e38f7345a845` · 응답 스키마 `5e3d5b1e5e31` · 입력 JSON
```json
{"base":"Faithful Leggings","ilvl":82,"rarity":"NORMAL","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"A":{"side":"SUFFIX","effect":"+(41—45)% to Cold Resistance","family":["ColdResistance"],"tags":["cold_resistance","elemental_resistance","elemental","cold","resistance"]},"B":{"side":"SUFFIX","effect":"(56—60)% reduced Freeze Duration on you","family":["ReducedAilmentDuration"],"tags":["elemental","cold","ailment"]},"C":{"side":"PREFIX","effect":"+(120—149) to maximum Life","family":["IncreasedLife"],"tags":["resource","life"]}},"start":[],"acquire":["A","B","C"],"remove":[],"keep":[],"goalRarity":"RARE","actions":["transmute.A","transmute.B","transmute.C","augment.A","augment.B","augment.C","regal.A","regal.B","regal.C","exalt.A","exalt.B","exalt.C"],"priorRejected":[[["transmute.A","regal.B","regal.C"],"RARE/A,B 에서 regal.C 실행 불가"],[["transmute.B","regal.A","regal.C"],"RARE/A,B 에서 regal.C 실행 불가"],[["transmute.C","regal.A","regal.B"],"RARE/A,C 에서 regal.B 실행 불가"]],"remainingSides":{"A":"SUFFIX","B":"SUFFIX","C":"PREFIX"},"availableCurrency":{"TRANSMUTATION":"Normal → Magic, 일반 옵션 하나 추가.","AUGMENTATION":"Magic, Magic 자리 한도를 유지하고 일반 옵션 하나 추가.","REGAL":"Magic → Rare, Rare 한도로 일반 옵션 하나 추가.","EXALTED":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"A\":[{\"first\":\"1|transmute.A\",\"upgrade\":\"2|regal.B\",\"other\":\"3|exalt.C\"}],\"B\":[{\"first\":\"1|transmute.B\",\"upgrade\":\"2|regal.A\",\"other\":\"3|exalt.C\"}],\"C\":[{\"first\":\"1|transmute.C\",\"upgrade\":\"2|regal.A\",\"other\":\"3|exalt.B\"}]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3],"uniqueFull":3,"missingFull":7,"bad":[],"prefix":[]}
```

### 호출 39 · Golden Targe · room-5 · survey

기대 경로 6개.
```json
[["annul.X","exalt.A","exalt.B"],["annul.X","exalt.B","exalt.A"],["exalt.B","annul.X","exalt.A"],["exalt.B","sideAnnul.X","exalt.A"],["sideAnnul.X","exalt.A","exalt.B"],["sideAnnul.X","exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `ade86c24d889` · 입력 JSON
```json
{"base":"Golden Targe","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"X":{"side":"PREFIX","effect":"(80—91)% increased Armour and Evasion","family":["DefencesPercent"],"tags":["armour","evasion"]},"K0":{"side":"PREFIX","effect":"(1—2) to (3—4) Physical Thorns damage","family":["Thorns"],"tags":["damage","physical"]},"K1":{"side":"PREFIX","effect":"+(47—71) to Armour\n+(42—64) to Evasion Rating","family":["BaseLocalDefences"],"tags":["armour","evasion"]},"A":{"side":"PREFIX","effect":"+(85—99) to maximum Life","family":["IncreasedLife"],"tags":["life"]},"B":{"side":"SUFFIX","effect":"Gain Deflection Rating equal to (18—20)% of Evasion Rating","family":["EvasionAppliesToDeflection"],"tags":["evasion"]}},"start":["X","K0","K1"],"acquire":["A","B"],"remove":["X"],"keep":["K0","K1"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","annul.X","sideAnnul.X"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","annul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","sideAnnul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다. 접두만 제거"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\",\"annul.X\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\",\"annul.X\"],[\"exalt.A\",\"sideAnnul.X\",\"exalt.B\"],[\"exalt.B\",\"sideAnnul.X\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[4],"uniqueFull":1,"missingFull":5,"bad":[[1,"RARE/K0,K1,X 에서 exalt.A 실행 불가"],[2,"RARE/B,K0,K1,X 에서 exalt.A 실행 불가"],[3,"RARE/K0,K1,X 에서 exalt.A 실행 불가"]],"prefix":[]}
```

### 호출 40 · Fanatic Greathammer · tag-5 · survey

기대 경로 6개.
```json
[["exalt.A","exalt.B"],["exalt.A","tagExalt.B"],["exalt.B","exalt.A"],["exalt.B","tagExalt.A"],["tagExalt.A","exalt.B"],["tagExalt.A","tagExalt.B"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `ba524f263568` · 입력 JSON
```json
{"base":"Fanatic Greathammer","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"Strikes deal Splash Damage","tags":["attack"],"stats":[{"id":"melee_splash","min":1,"max":1}]},"mods":{"K":{"side":"PREFIX","effect":"Adds 1 to (7—10) Lightning Damage","family":["LightningDamage"],"tags":["damage","elemental","lightning","attack"]},"A":{"side":"SUFFIX","effect":"Leeches (5—5.9)% of Physical Damage as Mana","family":["ManaLeech"],"tags":["mana","physical","attack"]},"B":{"side":"SUFFIX","effect":"Gain (36—45) Mana per enemy killed","family":["ManaGainedFromEnemyDeath"],"tags":["mana"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","tagExalt.A","tagExalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","tagExalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 현재 implicit·explicit 태그 합집합이 비지 않으면 그중 하나 이상 겹치는 옵션만 추가","tagExalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 현재 implicit·explicit 태그 합집합이 비지 않으면 그중 하나 이상 겹치는 옵션만 추가"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"tagExalt.A\",\"exalt.B\"],[\"exalt.A\",\"tagExalt.B\"],[\"tagExalt.A\",\"tagExalt.B\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3,4],"uniqueFull":4,"missingFull":2,"bad":[],"prefix":[]}
```

### 호출 41 · Mail Belt · swap-5 · survey

기대 경로 5개.
```json
[["annul.X","exalt.A","exalt.B"],["annul.X","exalt.B","exalt.A"],["chaos.X>A","exalt.B"],["exalt.B","annul.X","exalt.A"],["exalt.B","chaos.X>A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `19f6d754cc2f` · 입력 JSON
```json
{"base":"Mail Belt","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"(10—15)% reduced Flask Charges used","tags":["flask"],"stats":[{"id":"flask_charges_used_+%","min":-15,"max":-10}]},"mods":{"A":{"side":"PREFIX","effect":"+(35—54) to maximum Mana","family":["IncreasedMana"],"tags":["resource","mana"]},"X":{"side":"PREFIX","effect":"+(25—34) to maximum Mana","family":["IncreasedMana"],"tags":["resource","mana"]},"K":{"side":"SUFFIX","effect":"(11—16)% increased Flask Charges gained","family":["BeltFlaskChargesGained"],"tags":["flask"]},"B":{"side":"SUFFIX","effect":"(4.1—6) Life Regeneration per second","family":["LifeRegeneration"],"tags":["flat_life_regen","resource","life"]}},"start":["K","X"],"acquire":["A","B"],"remove":["X"],"keep":["K"],"goalRarity":"RARE","actions":["annul.X","exalt.A","exalt.B","chaos.X>A"],"actionRules":{"annul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","chaos.X>A":"Rare에서 지정한 기존 옵션이 제거되고 이어 목표 옵션이 뽑힌 한 번의 교체."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"annul.X\",\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"chaos.X>A\",\"exalt.B\"],[\"annul.X\",\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3],"uniqueFull":3,"missingFull":2,"bad":[],"prefix":[]}
```

### 호출 42 · Shrine Sceptre · hybrid-5 · survey

기대 경로 2개.
```json
[["exalt.A","exalt.B"],["exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `16e0b76b083f` · 입력 JSON
```json
{"base":"Shrine Sceptre","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"A":{"side":"PREFIX","effect":"(27—30)% increased Spirit+(34—37) to maximum Mana","family":["LocalIncreasedSpiritAndMana"],"tags":[]},"B":{"side":"SUFFIX","effect":"Allies in your Presence have (35—38)% increased Critical Hit Chance","family":["CriticalStrikeChanceIncrease"],"tags":["critical"]},"K":{"side":"SUFFIX","effect":"Allies in your Presence have (5—8)% increased Cast Speed","family":["IncreasedCastSpeed"],"tags":["caster_speed","caster","speed"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","requiredStatRows":["local_spirit_+%","base_maximum_mana","allies_in_presence_critical_strike_chance_+%"],"actions":["exalt.A","exalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2],"uniqueFull":2,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 43 · Jade Talisman · side-5 · survey

기대 경로 8개.
```json
[["dextral_exaltation.B","exalt.A"],["dextral_exaltation.B","sinistral_exaltation.A"],["exalt.A","dextral_exaltation.B"],["exalt.A","exalt.B"],["exalt.B","exalt.A"],["exalt.B","sinistral_exaltation.A"],["sinistral_exaltation.A","dextral_exaltation.B"],["sinistral_exaltation.A","exalt.B"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `8e0e79b42f01` · 입력 JSON
```json
{"base":"Jade Talisman","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"K":{"side":"PREFIX","effect":"Adds (1—13) to (168—231) Lightning Damage","family":["LightningDamage"],"tags":["damage","elemental","lightning","attack"]},"A":{"side":"PREFIX","effect":"(55—64)% increased Physical Damage\n+(124—149) to Accuracy Rating","family":["LocalIncreasedPhysicalDamagePercentAndAccuracyRating"],"tags":["damage","physical","attack"]},"B":{"side":"SUFFIX","effect":"(23—25)% increased Attack Speed","family":["IncreasedAttackSpeed"],"tags":["attack","speed"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","sinistral_exaltation.A","dextral_exaltation.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","sinistral_exaltation.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 접두만 추가","dextral_exaltation.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 접미만 추가"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"sinistral_exaltation.A\",\"exalt.B\"],[\"exalt.A\",\"dextral_exaltation.B\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3],"uniqueFull":3,"missingFull":5,"bad":[],"prefix":[]}
```

### 호출 44 · Flowing Raiment · implicit-5 · survey

기대 경로 2개.
```json
[["exalt.A","exalt.B"],["exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `16e0b76b083f` · 입력 JSON
```json
{"base":"Flowing Raiment","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"(40—50)% increased Mana Regeneration Rate","tags":["resource","mana"],"stats":[{"id":"mana_regeneration_rate_+%","min":40,"max":50}]},"mods":{"A":{"side":"PREFIX","effect":"+(10—17) to maximum Energy Shield","family":["BaseLocalDefences"],"tags":["defences","energy_shield"]},"B":{"side":"SUFFIX","effect":"+(31—35)% to Cold Resistance","family":["ColdResistance"],"tags":["cold_resistance","elemental_resistance","elemental","cold","resistance"]},"K":{"side":"PREFIX","effect":"+(175—189) to maximum Life","family":["IncreasedLife"],"tags":["resource","life"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","keepImplicitStat":{"id":"mana_regeneration_rate_+%","min":40,"max":50},"actions":["exalt.A","exalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2],"uniqueFull":2,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 45 · Desolate Crossbow · fracture-5 · survey

기대 경로 4개.
```json
[["exalt.A","exalt.B","fracture.K"],["exalt.A","fracture.K","exalt.B"],["exalt.B","exalt.A","fracture.K"],["exalt.B","fracture.K","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `5906d0002371` · 입력 JSON
```json
{"base":"Desolate Crossbow","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"K":{"side":"PREFIX","effect":"Adds (1—2) to (19—27) Lightning Damage","family":["LightningDamage"],"tags":["damage","elemental","lightning","attack"]},"J":{"side":"SUFFIX","effect":"Gain (10—18) Life per enemy killed","family":["LifeGainedFromEnemyDeath"],"tags":["life"]},"L":{"side":"PREFIX","effect":"(120—139)% increased Elemental Damage with Attacks","family":["IncreasedWeaponElementalDamagePercent"],"tags":["damage","elemental","fire","cold","lightning"]},"A":{"side":"SUFFIX","effect":"Gain (4—5) Mana per enemy killed","family":["ManaGainedFromEnemyDeath"],"tags":["mana"]},"B":{"side":"SUFFIX","effect":"Leeches (9—9.9)% of Physical Damage as Life","family":["LifeLeech"],"tags":["life","physical","attack"]}},"start":["K","J","L"],"acquire":["A","B"],"remove":[],"keep":["K","J","L"],"goalRarity":"RARE","mustFracture":"K","actions":["exalt.A","exalt.B","fracture.K"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","fracture.K":"Rare 명시 4개 이상에서 보유 K가 분열되는 결과. 옵션 수는 그대로다."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\",\"fracture.K\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\",\"fracture.K\"],[\"exalt.A\",\"fracture.K\",\"exalt.B\"],[\"exalt.B\",\"fracture.K\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3,4],"uniqueFull":4,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 46 · Cryptic Crown · room-6 · survey

기대 경로 15개.
```json
[["annul.X","annul.Y","exalt.A","exalt.B"],["annul.X","annul.Y","exalt.B","exalt.A"],["annul.X","exalt.A","annul.Y","exalt.B"],["annul.X","exalt.A","exalt.B","annul.Y"],["annul.X","exalt.B","annul.Y","exalt.A"],["annul.X","exalt.B","exalt.A","annul.Y"],["annul.Y","annul.X","exalt.A","exalt.B"],["annul.Y","annul.X","exalt.B","exalt.A"],["annul.Y","exalt.B","annul.X","exalt.A"],["doubleAnnul.X+Y","exalt.A","exalt.B"],["doubleAnnul.X+Y","exalt.B","exalt.A"],["exalt.B","annul.X","annul.Y","exalt.A"],["exalt.B","annul.X","exalt.A","annul.Y"],["exalt.B","annul.Y","annul.X","exalt.A"],["exalt.B","doubleAnnul.X+Y","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `740f5afe337e` · 입력 JSON
```json
{"base":"Cryptic Crown","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"X":{"side":"SUFFIX","effect":"+(13—16) to Intelligence","family":["Intelligence"],"tags":["attribute"]},"K0":{"side":"SUFFIX","effect":"+(36—40)% to Cold Resistance","family":["ColdResistance"],"tags":["cold_resistance","elemental_resistance","elemental","cold","resistance"]},"K1":{"side":"SUFFIX","effect":"(3.1—4) Life Regeneration per second","family":["LifeRegeneration"],"tags":["flat_life_regen","resource","life"]},"A":{"side":"SUFFIX","effect":"+(11—15)% to Lightning Resistance","family":["LightningResistance"],"tags":["elemental_resistance","lightning_resistance","elemental","lightning","resistance"]},"B":{"side":"PREFIX","effect":"+(11—32) to Accuracy Rating","family":["IncreasedAccuracy"],"tags":["attack"]},"Y":{"side":"PREFIX","effect":"+(9—16) to Armour\n+(5—8) to maximum Energy Shield","family":["BaseLocalDefences"],"tags":["defences","armour","energy_shield"]}},"start":["X","K0","K1","Y"],"acquire":["A","B"],"remove":["X","Y"],"keep":["K0","K1"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","annul.X","annul.Y","doubleAnnul.X+Y"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","annul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","annul.Y":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","doubleAnnul.X+Y":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다. 한 번에 서로 다른 두 개 제거"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"annul.X\",\"exalt.A\",\"exalt.B\",\"annul.Y\"],[\"annul.Y\",\"exalt.A\",\"exalt.B\",\"annul.X\"],[\"doubleAnnul.X+Y\",\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.A\",\"exalt.B\",\"annul.X\",\"annul.Y\"],[\"exalt.B\",\"exalt.A\",\"annul.Y\",\"annul.X\"],[\"annul.X\",\"annul.Y\",\"exalt.A\",\"exalt.B\"],[\"annul.Y\",\"annul.X\",\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,3,6,7],"uniqueFull":4,"missingFull":11,"bad":[[2,"RARE/K0,K1,X 에서 exalt.A 실행 불가"],[4,"RARE/K0,K1,X,Y 에서 exalt.A 실행 불가"],[5,"RARE/B,K0,K1,X,Y 에서 exalt.A 실행 불가"]],"prefix":[]}
```

### 호출 47 · Warmonger Bow · tag-6 · survey

기대 경로 6개.
```json
[["exalt.A","exalt.B"],["exalt.A","tagExalt.B"],["exalt.B","exalt.A"],["exalt.B","tagExalt.A"],["tagExalt.A","exalt.B"],["tagExalt.A","tagExalt.B"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `ba524f263568` · 입력 JSON
```json
{"base":"Warmonger Bow","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"K":{"side":"PREFIX","effect":"(19—35)% increased Elemental Damage with Attacks","family":["IncreasedWeaponElementalDamagePercent"],"tags":["elemental_damage","has_attack_mod","damage","elemental","fire","cold","lightning"]},"A":{"side":"SUFFIX","effect":"+(23—25)% to Critical Damage Bonus","family":["CriticalStrikeMultiplier"],"tags":["damage","attack","critical"]},"B":{"side":"SUFFIX","effect":"+(3.11—3.8)% to Critical Hit Chance","family":["CriticalStrikeChanceIncrease"],"tags":["attack","critical"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","tagExalt.A","tagExalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","tagExalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 현재 implicit·explicit 태그 합집합이 비지 않으면 그중 하나 이상 겹치는 옵션만 추가","tagExalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 현재 implicit·explicit 태그 합집합이 비지 않으면 그중 하나 이상 겹치는 옵션만 추가"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"tagExalt.A\",\"exalt.B\"],[\"exalt.A\",\"tagExalt.B\"],[\"tagExalt.A\",\"tagExalt.B\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3,4],"uniqueFull":4,"missingFull":2,"bad":[],"prefix":[]}
```

### 호출 48 · Daggerfoot Shoes · swap-6 · survey

기대 경로 5개.
```json
[["annul.X","exalt.A","exalt.B"],["annul.X","exalt.B","exalt.A"],["chaos.X>A","exalt.B"],["exalt.B","annul.X","exalt.A"],["exalt.B","chaos.X>A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `19f6d754cc2f` · 입력 JSON
```json
{"base":"Daggerfoot Shoes","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"A":{"side":"SUFFIX","effect":"Gain Deflection Rating equal to (21—23)% of Evasion Rating","family":["EvasionAppliesToDeflection"],"tags":["defences","evasion"]},"X":{"side":"SUFFIX","effect":"Gain Deflection Rating equal to (12—14)% of Evasion Rating","family":["EvasionAppliesToDeflection"],"tags":["defences","evasion"]},"K":{"side":"PREFIX","effect":"+(20—29) to maximum Life","family":["IncreasedLife"],"tags":["resource","life"]},"B":{"side":"PREFIX","effect":"(21—26)% increased Evasion and Energy Shield\n+(25—40) to Stun Threshold","family":["DefencesPercentAndStunThreshold"],"tags":[]}},"start":["K","X"],"acquire":["A","B"],"remove":["X"],"keep":["K"],"goalRarity":"RARE","actions":["annul.X","exalt.A","exalt.B","chaos.X>A"],"actionRules":{"annul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","chaos.X>A":"Rare에서 지정한 기존 옵션이 제거되고 이어 목표 옵션이 뽑힌 한 번의 교체."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"annul.X\",\"exalt.A\",\"exalt.B\"],[\"chaos.X>A\"]],\"alternatives\":[[\"exalt.B\",\"annul.X\",\"exalt.A\"],[\"exalt.A\",\"chaos.X>A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,3],"uniqueFull":2,"missingFull":3,"bad":[[2,"목표 미달 RARE/A,K"],[4,"RARE/K,X 에서 exalt.A 실행 불가"]],"prefix":[]}
```

### 호출 49 · Tawhoan Tower Shield · hybrid-6 · survey

기대 경로 2개.
```json
[["exalt.A","exalt.B"],["exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `16e0b76b083f` · 입력 JSON
```json
{"base":"Tawhoan Tower Shield","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"A":{"side":"PREFIX","effect":"(21—26)% increased Armour\n+(25—40) to Stun Threshold","family":["DefencesPercentAndStunThreshold"],"tags":[]},"B":{"side":"SUFFIX","effect":"+(73—97) to Stun Threshold","family":["StunThreshold"],"tags":[]},"K":{"side":"SUFFIX","effect":"+1% to Maximum Lightning Resistance","family":["MaximumLightningResistance"],"tags":["elemental","lightning","resistance"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","requiredStatRows":["stun_threshold_+","local_physical_damage_reduction_rating_+%","stun_threshold_+"],"actions":["exalt.A","exalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2],"uniqueFull":2,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 50 · Flying Spear · side-6 · survey

기대 경로 8개.
```json
[["dextral_exaltation.A","exalt.B"],["dextral_exaltation.A","sinistral_exaltation.B"],["exalt.A","exalt.B"],["exalt.A","sinistral_exaltation.B"],["exalt.B","dextral_exaltation.A"],["exalt.B","exalt.A"],["sinistral_exaltation.B","dextral_exaltation.A"],["sinistral_exaltation.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `fd90d7030706` · 입력 JSON
```json
{"base":"Flying Spear","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"local display grants spear throw skill [1]\n(25—35)% increased Projectile Speed with this Weapon","tags":[],"stats":[{"id":"local_display_grants_spear_throw_skill","min":1,"max":1},{"id":"local_projectile_speed_+%","min":25,"max":35}]},"mods":{"K":{"side":"SUFFIX","effect":"+(9—12) to Strength","family":["Strength"],"tags":["attribute"]},"A":{"side":"SUFFIX","effect":"(14—16)% increased Attack Speed","family":["IncreasedAttackSpeed"],"tags":["attack","speed"]},"B":{"side":"PREFIX","effect":"Adds (1—12) to (202—234) Lightning Damage","family":["LightningDamage"],"tags":["damage","elemental","lightning","attack"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","dextral_exaltation.A","sinistral_exaltation.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","dextral_exaltation.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 접미만 추가","sinistral_exaltation.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 접두만 추가"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"dextral_exaltation.A\",\"exalt.B\"],[\"sinistral_exaltation.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3],"uniqueFull":3,"missingFull":5,"bad":[],"prefix":[]}
```

### 호출 51 · Broadhead Quiver · implicit-6 · survey

기대 경로 2개.
```json
[["exalt.A","exalt.B"],["exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `16e0b76b083f` · 입력 JSON
```json
{"base":"Broadhead Quiver","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"Adds 1 to 3 Physical Damage to Attacks","tags":["damage","physical","attack"],"stats":[{"id":"attack_maximum_added_physical_damage","min":3,"max":3},{"id":"attack_minimum_added_physical_damage","min":1,"max":1}]},"mods":{"A":{"side":"SUFFIX","effect":"(15—19)% increased Critical Damage Bonus for Attack Damage","family":["CriticalStrikeMultiplier"],"tags":["damage","attack","critical"]},"B":{"side":"PREFIX","effect":"Adds 1 to (23—27) Lightning damage to Attacks","family":["LightningDamage"],"tags":["damage","elemental","lightning","attack"]},"K":{"side":"SUFFIX","effect":"(15—19)% increased Critical Hit Chance for Attacks","family":["CriticalStrikeChanceIncrease"],"tags":["attack","critical"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","keepImplicitStat":{"id":"attack_maximum_added_physical_damage","min":3,"max":3},"actions":["exalt.A","exalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2],"uniqueFull":2,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 52 · Ravenous Staff · fracture-6 · survey

기대 경로 4개.
```json
[["exalt.A","exalt.B","fracture.K"],["exalt.A","fracture.K","exalt.B"],["exalt.B","exalt.A","fracture.K"],["exalt.B","fracture.K","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `5906d0002371` · 입력 JSON
```json
{"base":"Ravenous Staff","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"K":{"side":"PREFIX","effect":"Gain (37—42)% of Damage as Extra Fire Damage","family":["FireDamage"],"tags":["damage","elemental","fire"]},"J":{"side":"SUFFIX","effect":"Gain (41—53) Life per enemy killed","family":["LifeGainedFromEnemyDeath"],"tags":["life"]},"L":{"side":"SUFFIX","effect":"Gain (4—5) Mana per enemy killed","family":["ManaGainedFromEnemyDeath"],"tags":["mana"]},"A":{"side":"PREFIX","effect":"+(29—48) to maximum Mana","family":["IncreasedMana"],"tags":["mana"]},"B":{"side":"PREFIX","effect":"(109—128)% increased Chaos Damage","family":["WeaponDamageTypePrefix"],"tags":["damage","chaos"]}},"start":["K","J","L"],"acquire":["A","B"],"remove":[],"keep":["K","J","L"],"goalRarity":"RARE","mustFracture":"K","actions":["exalt.A","exalt.B","fracture.K"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","fracture.K":"Rare 명시 4개 이상에서 보유 K가 분열되는 결과. 옵션 수는 그대로다."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\",\"fracture.K\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\",\"fracture.K\"],[\"exalt.A\",\"fracture.K\",\"exalt.B\"],[\"exalt.B\",\"fracture.K\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3,4],"uniqueFull":4,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 53 · Ruination Maul · room-7 · survey

기대 경로 6개.
```json
[["annul.X","exalt.A","exalt.B"],["annul.X","exalt.B","exalt.A"],["exalt.B","annul.X","exalt.A"],["exalt.B","sideAnnul.X","exalt.A"],["sideAnnul.X","exalt.A","exalt.B"],["sideAnnul.X","exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `ade86c24d889` · 입력 JSON
```json
{"base":"Ruination Maul","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"Causes Enemies to Explode on Critical kill, for 10% of their Life as Physical Damage","tags":[],"stats":[{"id":"local_explode_on_kill_with_crit_%_physical_damage_to_deal","min":10,"max":10}]},"mods":{"X":{"side":"PREFIX","effect":"(20—24)% increased Physical Damage\n+(21—46) to Accuracy Rating","family":["LocalIncreasedPhysicalDamagePercentAndAccuracyRating"],"tags":["damage","physical","attack"]},"K0":{"side":"PREFIX","effect":"Adds (135—156) to (205—236) Fire Damage","family":["FireDamage"],"tags":["damage","elemental","fire","attack"]},"K1":{"side":"PREFIX","effect":"Adds (32—43) to (49—66) Cold Damage","family":["ColdDamage"],"tags":["damage","elemental","cold","attack"]},"A":{"side":"PREFIX","effect":"(155—169)% increased Physical Damage","family":["LocalPhysicalDamagePercent"],"tags":["damage","physical","attack"]},"B":{"side":"SUFFIX","effect":"+(17—19)% to Critical Damage Bonus","family":["CriticalStrikeMultiplier"],"tags":["damage","attack","critical"]}},"start":["X","K0","K1"],"acquire":["A","B"],"remove":["X"],"keep":["K0","K1"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","annul.X","sideAnnul.X"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","annul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","sideAnnul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다. 접두만 제거"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\",\"annul.X\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\",\"annul.X\"],[\"exalt.A\",\"annul.X\",\"exalt.B\"],[\"exalt.B\",\"annul.X\",\"exalt.A\"],[\"annul.X\",\"exalt.A\",\"exalt.B\"],[\"annul.X\",\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[4,5,6],"uniqueFull":3,"missingFull":3,"bad":[[1,"RARE/K0,K1,X 에서 exalt.A 실행 불가"],[2,"RARE/B,K0,K1,X 에서 exalt.A 실행 불가"],[3,"RARE/K0,K1,X 에서 exalt.A 실행 불가"]],"prefix":[]}
```

### 호출 54 · Akoyan Club · tag-7 · survey

기대 경로 6개.
```json
[["exalt.A","exalt.B"],["exalt.A","tagExalt.B"],["exalt.B","exalt.A"],["exalt.B","tagExalt.A"],["tagExalt.A","exalt.B"],["tagExalt.A","tagExalt.B"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `ba524f263568` · 입력 JSON
```json
{"base":"Akoyan Club","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"Always Hits","tags":["attack"],"stats":[{"id":"local_always_hit","min":1,"max":1}]},"mods":{"K":{"side":"SUFFIX","effect":"+3 to Level of all Melee Skills","family":["IncreaseSocketedGemLevel"],"tags":["attack"]},"A":{"side":"PREFIX","effect":"(15—19)% increased Physical Damage\n+(16—20) to Accuracy Rating","family":["LocalIncreasedPhysicalDamagePercentAndAccuracyRating"],"tags":["damage","physical","attack"]},"B":{"side":"PREFIX","effect":"(36—52)% increased Elemental Damage with Attacks","family":["IncreasedWeaponElementalDamagePercent"],"tags":["damage","elemental","fire","cold","lightning"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","tagExalt.A","tagExalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","tagExalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 현재 implicit·explicit 태그 합집합이 비지 않으면 그중 하나 이상 겹치는 옵션만 추가","tagExalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 현재 implicit·explicit 태그 합집합이 비지 않으면 그중 하나 이상 겹치는 옵션만 추가"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"],[\"tagExalt.A\",\"exalt.B\"],[\"exalt.A\",\"tagExalt.B\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3,4],"uniqueFull":4,"missingFull":2,"bad":[],"prefix":[]}
```

### 호출 55 · Penetrating Quiver · swap-7 · survey

기대 경로 5개.
```json
[["annul.X","exalt.A","exalt.B"],["annul.X","exalt.B","exalt.A"],["chaos.X>A","exalt.B"],["exalt.B","annul.X","exalt.A"],["exalt.B","chaos.X>A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `19f6d754cc2f` · 입력 JSON
```json
{"base":"Penetrating Quiver","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"100% chance to Pierce an Enemy","tags":[],"stats":[{"id":"base_chance_to_pierce_%","min":100,"max":100}]},"mods":{"A":{"side":"PREFIX","effect":"Adds 1 to (10—15) Lightning damage to Attacks","family":["LightningDamage"],"tags":["damage","elemental","lightning","attack"]},"X":{"side":"PREFIX","effect":"Adds 1 to (4—6) Lightning damage to Attacks","family":["LightningDamage"],"tags":["damage","elemental","lightning","attack"]},"K":{"side":"SUFFIX","effect":"(15—19)% increased Critical Damage Bonus for Attack Damage","family":["CriticalStrikeMultiplier"],"tags":["damage","attack","critical"]},"B":{"side":"SUFFIX","effect":"(18—20)% chance to Pierce an Enemy","family":["ChanceToPierce"],"tags":[]}},"start":["K","X"],"acquire":["A","B"],"remove":["X"],"keep":["K"],"goalRarity":"RARE","actions":["annul.X","exalt.A","exalt.B","chaos.X>A"],"actionRules":{"annul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","chaos.X>A":"Rare에서 지정한 기존 옵션이 제거되고 이어 목표 옵션이 뽑힌 한 번의 교체."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"annul.X\",\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"annul.X\",\"chaos.X>A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1],"uniqueFull":1,"missingFull":4,"bad":[[2,"RARE/K 에서 chaos.X>A 실행 불가"]],"prefix":[]}
```

### 호출 56 · Gemini Crossbow · hybrid-7 · survey

기대 경로 2개.
```json
[["exalt.A","exalt.B"],["exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `16e0b76b083f` · 입력 JSON
```json
{"base":"Gemini Crossbow","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"Loads an additional bolt","tags":[],"stats":[{"id":"base_number_of_crossbow_bolts","min":1,"max":1}]},"mods":{"A":{"side":"SUFFIX","effect":"+(41—60) to Accuracy Rating\n15% increased Light Radius","family":["LightRadiusAndAccuracy"],"tags":["attack"]},"B":{"side":"PREFIX","effect":"Adds (11—17) to (20—30) Physical Damage","family":["PhysicalDamage"],"tags":["damage","physical","attack"]},"K":{"side":"PREFIX","effect":"+(85—123) to Accuracy Rating","family":["IncreasedAccuracy"],"tags":["attack"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","requiredStatRows":["light_radius_+%","local_accuracy_rating","local_maximum_added_physical_damage","local_minimum_added_physical_damage"],"actions":["exalt.A","exalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2],"uniqueFull":2,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 57 · Freebooter Cap · side-7 · survey

기대 경로 8개.
```json
[["dextral_exaltation.B","exalt.A"],["dextral_exaltation.B","sinistral_exaltation.A"],["exalt.A","dextral_exaltation.B"],["exalt.A","exalt.B"],["exalt.B","exalt.A"],["exalt.B","sinistral_exaltation.A"],["sinistral_exaltation.A","dextral_exaltation.B"],["sinistral_exaltation.A","exalt.B"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `8e0e79b42f01` · 입력 JSON
```json
{"base":"Freebooter Cap","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"K":{"side":"PREFIX","effect":"(21—26)% increased Evasion Rating\n+(20—25) to maximum Life","family":["BaseLocalDefencesAndLife"],"tags":["defences","resource","life","evasion"]},"A":{"side":"PREFIX","effect":"+(80—89) to maximum Mana","family":["IncreasedMana"],"tags":["resource","mana"]},"B":{"side":"SUFFIX","effect":"+(41—45)% to Lightning Resistance","family":["LightningResistance"],"tags":["elemental_resistance","lightning_resistance","elemental","lightning","resistance"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","sinistral_exaltation.A","dextral_exaltation.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","sinistral_exaltation.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 접두만 추가","dextral_exaltation.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 접미만 추가"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"sinistral_exaltation.A\",\"exalt.B\"],[\"exalt.A\",\"dextral_exaltation.B\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3],"uniqueFull":3,"missingFull":5,"bad":[],"prefix":[]}
```

### 호출 58 · Sinew Belt · implicit-7 · survey

기대 경로 2개.
```json
[["exalt.A","exalt.B"],["exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `16e0b76b083f` · 입력 JSON
```json
{"base":"Sinew Belt","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"+(15—20) to Strength\nHas 1 Charm Slots","tags":["attribute","charm"],"stats":[{"id":"additional_strength","min":15,"max":20},{"id":"local_charm_slots","min":1,"max":1}]},"mods":{"A":{"side":"PREFIX","effect":"+(150—174) to maximum Life","family":["IncreasedLife"],"tags":["resource","life"]},"B":{"side":"SUFFIX","effect":"+(31—35)% to Cold Resistance","family":["ColdResistance"],"tags":["cold_resistance","elemental_resistance","elemental","cold","resistance"]},"K":{"side":"PREFIX","effect":"+(312—351) to Armour","family":["IncreasedPhysicalDamageReductionRating"],"tags":["defences","armour"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","keepImplicitStat":{"id":"additional_strength","min":15,"max":20},"actions":["exalt.A","exalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2],"uniqueFull":2,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 59 · Feathered Raiment · fracture-7 · survey

기대 경로 4개.
```json
[["exalt.A","exalt.B","fracture.K"],["exalt.A","fracture.K","exalt.B"],["exalt.B","exalt.A","fracture.K"],["exalt.B","fracture.K","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `5906d0002371` · 입력 JSON
```json
{"base":"Feathered Raiment","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"(5—10)% of Damage is taken from Mana before Life","tags":["resource","life","mana"],"stats":[{"id":"base_damage_removed_from_mana_before_life_%","min":5,"max":10}]},"mods":{"K":{"side":"PREFIX","effect":"+(190—199) to maximum Life","family":["IncreasedLife"],"tags":["resource","life"]},"J":{"side":"SUFFIX","effect":"+(98—124) to Stun Threshold","family":["StunThreshold"],"tags":[]},"L":{"side":"PREFIX","effect":"(39—42)% increased Energy Shield\n+(42—49) to maximum Life","family":["BaseLocalDefencesAndLife"],"tags":["defences","resource","life","energy_shield"]},"A":{"side":"SUFFIX","effect":"+(16—20)% to Cold Resistance","family":["ColdResistance"],"tags":["cold_resistance","elemental_resistance","elemental","cold","resistance"]},"B":{"side":"SUFFIX","effect":"+(12—15)% to Chaos Resistance","family":["ChaosResistance"],"tags":["chaos_resistance","chaos","resistance"]}},"start":["K","J","L"],"acquire":["A","B"],"remove":[],"keep":["K","J","L"],"goalRarity":"RARE","mustFracture":"K","actions":["exalt.A","exalt.B","fracture.K"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","fracture.K":"Rare 명시 4개 이상에서 보유 K가 분열되는 결과. 옵션 수는 그대로다."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\",\"fracture.K\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\",\"fracture.K\"],[\"exalt.A\",\"fracture.K\",\"exalt.B\"],[\"exalt.B\",\"fracture.K\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3,4],"uniqueFull":4,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 60 · Reflecting Staff · room-8 · survey

기대 경로 15개.
```json
[["annul.X","annul.Y","exalt.A","exalt.B"],["annul.X","annul.Y","exalt.B","exalt.A"],["annul.X","exalt.A","annul.Y","exalt.B"],["annul.X","exalt.A","exalt.B","annul.Y"],["annul.X","exalt.B","annul.Y","exalt.A"],["annul.X","exalt.B","exalt.A","annul.Y"],["annul.Y","annul.X","exalt.A","exalt.B"],["annul.Y","annul.X","exalt.B","exalt.A"],["annul.Y","exalt.B","annul.X","exalt.A"],["doubleAnnul.X+Y","exalt.A","exalt.B"],["doubleAnnul.X+Y","exalt.B","exalt.A"],["exalt.B","annul.X","annul.Y","exalt.A"],["exalt.B","annul.X","exalt.A","annul.Y"],["exalt.B","annul.Y","annul.X","exalt.A"],["exalt.B","doubleAnnul.X+Y","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `740f5afe337e` · 입력 JSON
```json
{"base":"Reflecting Staff","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"X":{"side":"SUFFIX","effect":"Gain (15—20) Mana per enemy killed","family":["ManaGainedFromEnemyDeath"],"tags":["mana"]},"K0":{"side":"SUFFIX","effect":"Gain (69—84) Life per enemy killed","family":["LifeGainedFromEnemyDeath"],"tags":["life"]},"K1":{"side":"SUFFIX","effect":"(51—60)% increased Freeze Buildup","family":["FreezeDamageIncrease"],"tags":["elemental","cold","ailment"]},"A":{"side":"SUFFIX","effect":"+1 to Level of all Lightning Spell Skills","family":["IncreaseSocketedGemLevel"],"tags":["elemental","lightning","caster","gem"]},"B":{"side":"PREFIX","effect":"Gain (49—54)% of Damage as Extra Lightning Damage","family":["LightningDamage"],"tags":["damage","elemental","lightning"]},"Y":{"side":"PREFIX","effect":"(69—88)% increased Cold Damage","family":["WeaponDamageTypePrefix"],"tags":["damage","elemental","cold"]}},"start":["X","K0","K1","Y"],"acquire":["A","B"],"remove":["X","Y"],"keep":["K0","K1"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","annul.X","annul.Y","doubleAnnul.X+Y"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","annul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","annul.Y":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","doubleAnnul.X+Y":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다. 한 번에 서로 다른 두 개 제거"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"annul.X\",\"exalt.A\",\"exalt.B\"],[\"annul.Y\",\"exalt.A\",\"exalt.B\"],[\"doubleAnnul.X+Y\",\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.A\",\"exalt.B\",\"annul.X\",\"annul.Y\"],[\"exalt.B\",\"exalt.A\",\"doubleAnnul.X+Y\"],[\"annul.X\",\"annul.Y\",\"exalt.A\",\"exalt.B\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[3,6],"uniqueFull":2,"missingFull":13,"bad":[[1,"목표 미달 RARE/A,B,K0,K1,Y"],[2,"RARE/K0,K1,X 에서 exalt.A 실행 불가"],[4,"RARE/K0,K1,X,Y 에서 exalt.A 실행 불가"],[5,"RARE/B,K0,K1,X,Y 에서 exalt.A 실행 불가"]],"prefix":[]}
```

### 호출 61 · Drakeskin Boots · tag-8 · survey

기대 경로 6개.
```json
[["exalt.A","exalt.B"],["exalt.A","tagExalt.B"],["exalt.B","exalt.A"],["exalt.B","tagExalt.A"],["tagExalt.A","exalt.B"],["tagExalt.A","tagExalt.B"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `ba524f263568` · 입력 JSON
```json
{"base":"Drakeskin Boots","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"K":{"side":"SUFFIX","effect":"+(16—19)% to Chaos Resistance","family":["ChaosResistance"],"tags":["chaos_resistance","chaos","resistance"]},"A":{"side":"SUFFIX","effect":"+(6—10)% to Cold Resistance","family":["ColdResistance"],"tags":["cold_resistance","elemental_resistance","elemental","cold","resistance"]},"B":{"side":"SUFFIX","effect":"(51—55)% reduced Chill Duration on you","family":["ReducedAilmentDuration"],"tags":["elemental","cold","ailment"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","tagExalt.A","tagExalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","tagExalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 현재 implicit·explicit 태그 합집합이 비지 않으면 그중 하나 이상 겹치는 옵션만 추가","tagExalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 현재 implicit·explicit 태그 합집합이 비지 않으면 그중 하나 이상 겹치는 옵션만 추가"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"],[\"tagExalt.A\",\"exalt.B\"],[\"exalt.A\",\"tagExalt.B\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3,4],"uniqueFull":4,"missingFull":2,"bad":[],"prefix":[]}
```

### 호출 62 · Omen Sceptre · swap-8 · survey

기대 경로 5개.
```json
[["annul.X","exalt.A","exalt.B"],["annul.X","exalt.B","exalt.A"],["chaos.X>A","exalt.B"],["exalt.B","annul.X","exalt.A"],["exalt.B","chaos.X>A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `19f6d754cc2f` · 입력 JSON
```json
{"base":"Omen Sceptre","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"A":{"side":"SUFFIX","effect":"Allies in your Presence have (9—12)% increased Cast Speed","family":["IncreasedCastSpeed"],"tags":["caster_speed","caster","speed"]},"X":{"side":"SUFFIX","effect":"Allies in your Presence have (5—8)% increased Cast Speed","family":["IncreasedCastSpeed"],"tags":["caster_speed","caster","speed"]},"K":{"side":"PREFIX","effect":"(27—32)% increased Spirit","family":["LocalIncreasedSpiritPercent"],"tags":[]},"B":{"side":"PREFIX","effect":"Allies in your Presence deal (9—11) to (14—17) added Attack Fire Damage","family":["FireDamage"],"tags":["elemental_damage","damage","elemental","fire","attack"]}},"start":["K","X"],"acquire":["A","B"],"remove":["X"],"keep":["K"],"goalRarity":"RARE","actions":["annul.X","exalt.A","exalt.B","chaos.X>A"],"actionRules":{"annul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","chaos.X>A":"Rare에서 지정한 기존 옵션이 제거되고 이어 목표 옵션이 뽑힌 한 번의 교체."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"annul.X\",\"exalt.A\",\"exalt.B\"],[\"chaos.X>A\"]],\"alternatives\":[[\"exalt.A\",\"exalt.B\",\"annul.X\"],[\"exalt.B\",\"annul.X\",\"exalt.A\"],[\"annul.X\",\"chaos.X>A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,4],"uniqueFull":2,"missingFull":3,"bad":[[2,"목표 미달 RARE/A,K"],[3,"RARE/K,X 에서 exalt.A 실행 불가"],[5,"RARE/K 에서 chaos.X>A 실행 불가"]],"prefix":[]}
```

### 호출 63 · Gladiatorial Helm · hybrid-8 · survey

기대 경로 2개.
```json
[["exalt.A","exalt.B"],["exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `16e0b76b083f` · 입력 JSON
```json
{"base":"Gladiatorial Helm","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"A":{"side":"SUFFIX","effect":"+(21—40) to Accuracy Rating10% increased Light Radius","family":["LightRadiusAndAccuracy"],"tags":["attack"]},"B":{"side":"PREFIX","effect":"(8—11)% increased Rarity of Items found","family":["ItemFoundRarityIncreasePrefix"],"tags":["drop"]},"K":{"side":"PREFIX","effect":"+(105—124) to maximum Mana","family":["IncreasedMana"],"tags":["resource","mana"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","requiredStatRows":["accuracy_rating","light_radius_+%","base_item_found_rarity_+%"],"actions":["exalt.A","exalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2],"uniqueFull":2,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 64 · Mail Coat · side-8 · survey

기대 경로 8개.
```json
[["dextral_exaltation.A","exalt.B"],["dextral_exaltation.A","sinistral_exaltation.B"],["exalt.A","exalt.B"],["exalt.A","sinistral_exaltation.B"],["exalt.B","dextral_exaltation.A"],["exalt.B","exalt.A"],["sinistral_exaltation.B","dextral_exaltation.A"],["sinistral_exaltation.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `fd90d7030706` · 입력 JSON
```json
{"base":"Mail Coat","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"(20—30)% reduced Slowing Potency of Debuffs on You","tags":[],"stats":[{"id":"base_slow_potency_+%","min":-30,"max":-20}]},"mods":{"K":{"side":"SUFFIX","effect":"(46—50)% reduced Poison Duration on you","family":["ReducedAilmentDuration"],"tags":["poison","chaos","ailment"]},"A":{"side":"SUFFIX","effect":"+(50—72) to Stun Threshold","family":["StunThreshold"],"tags":[]},"B":{"side":"PREFIX","effect":"+(11—18) to Evasion Rating","family":["BaseLocalDefences"],"tags":["defences","evasion"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","dextral_exaltation.A","sinistral_exaltation.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","dextral_exaltation.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 접미만 추가","sinistral_exaltation.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 접두만 추가"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\",\"dextral_exaltation.A\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\",\"sinistral_exaltation.B\"],[\"dextral_exaltation.A\",\"exalt.A\",\"exalt.B\"],[\"sinistral_exaltation.B\",\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[],"uniqueFull":0,"missingFull":8,"bad":[[1,"RARE/A,B,K 에서 dextral_exaltation.A 실행 불가"],[2,"RARE/A,B,K 에서 sinistral_exaltation.B 실행 불가"],[3,"RARE/A,K 에서 exalt.A 실행 불가"],[4,"RARE/B,K 에서 exalt.B 실행 불가"]],"prefix":[[1,2,1],[2,2,1]]}
```

### 호출 65 · Siege Crossbow · implicit-8 · survey

기대 경로 2개.
```json
[["exalt.A","exalt.B"],["exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `16e0b76b083f` · 입력 JSON
```json
{"base":"Siege Crossbow","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"Grenade Skills Fire 1 additional Projectiles","tags":[],"stats":[{"id":"grenade_skill_number_of_additional_projectiles","min":1,"max":1}]},"mods":{"A":{"side":"SUFFIX","effect":"Grants 3 Life per Enemy Hit","family":["LifeGainPerTarget"],"tags":["life","attack"]},"B":{"side":"PREFIX","effect":"Adds (16—23) to (25—35) Cold Damage","family":["ColdDamage"],"tags":["damage","elemental","cold","attack"]},"K":{"side":"SUFFIX","effect":"+(10—11)% to Critical Damage Bonus","family":["CriticalStrikeMultiplier"],"tags":["damage","attack","critical"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","keepImplicitStat":{"id":"grenade_skill_number_of_additional_projectiles","min":1,"max":1},"actions":["exalt.A","exalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\",\"exalt.A\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\",\"exalt.B\"],[\"exalt.A\",\"exalt.A\",\"exalt.B\"],[\"exalt.B\",\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[],"uniqueFull":0,"missingFull":2,"bad":[[1,"RARE/A,B,K 에서 exalt.A 실행 불가"],[2,"RARE/A,B,K 에서 exalt.B 실행 불가"],[3,"RARE/A,K 에서 exalt.A 실행 불가"],[4,"RARE/B,K 에서 exalt.B 실행 불가"]],"prefix":[[1,2,1],[2,2,1]]}
```

### 호출 66 · Clasped Sceptre · fracture-8 · survey

기대 경로 4개.
```json
[["exalt.A","exalt.B","fracture.K"],["exalt.A","fracture.K","exalt.B"],["exalt.B","exalt.A","fracture.K"],["exalt.B","fracture.K","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `5906d0002371` · 입력 JSON
```json
{"base":"Clasped Sceptre","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"K":{"side":"PREFIX","effect":"Allies in your Presence deal (90—104)% increased Damage","family":["AllDamage"],"tags":["damage"]},"J":{"side":"SUFFIX","effect":"Allies in your Presence Regenerate (6.1—9) Life per second","family":["LifeRegeneration"],"tags":["resource","life"]},"L":{"side":"SUFFIX","effect":"Allies in your Presence have (10—14)% increased Critical Damage Bonus","family":["CriticalStrikeMultiplier"],"tags":["damage","critical"]},"A":{"side":"PREFIX","effect":"(20—26)% increased Spirit","family":["LocalIncreasedSpiritPercent"],"tags":[]},"B":{"side":"PREFIX","effect":"Allies in your Presence deal (14—15) to (22—24) added Attack Cold Damage","family":["ColdDamage"],"tags":["elemental_damage","damage","elemental","cold","attack"]}},"start":["K","J","L"],"acquire":["A","B"],"remove":[],"keep":["K","J","L"],"goalRarity":"RARE","mustFracture":"K","actions":["exalt.A","exalt.B","fracture.K"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","fracture.K":"Rare 명시 4개 이상에서 보유 K가 분열되는 결과. 옵션 수는 그대로다."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\",\"fracture.K\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\",\"fracture.K\"],[\"exalt.A\",\"fracture.K\",\"exalt.B\"],[\"exalt.B\",\"fracture.K\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3,4],"uniqueFull":4,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 67 · Toxic Quiver · room-9 · survey

기대 경로 6개.
```json
[["annul.X","exalt.A","exalt.B"],["annul.X","exalt.B","exalt.A"],["exalt.B","annul.X","exalt.A"],["exalt.B","sideAnnul.X","exalt.A"],["sideAnnul.X","exalt.A","exalt.B"],["sideAnnul.X","exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `ade86c24d889` · 입력 JSON
```json
{"base":"Toxic Quiver","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"(20—30)% chance to Poison on Hit with Attacks","tags":["chaos","attack","ailment"],"stats":[{"id":"chance_to_poison_on_hit_with_attacks_%","min":20,"max":30}]},"mods":{"X":{"side":"PREFIX","effect":"Adds 1 to (23—27) Lightning damage to Attacks","family":["LightningDamage"],"tags":["damage","elemental","lightning","attack"]},"K0":{"side":"PREFIX","effect":"(34—41)% increased Projectile Speed","family":["ProjectileSpeed"],"tags":["speed"]},"K1":{"side":"PREFIX","effect":"+(168—236) to Accuracy Rating","family":["IncreasedAccuracy"],"tags":["attack"]},"A":{"side":"PREFIX","effect":"Adds (5—7) to (9—13) Physical Damage to Attacks","family":["PhysicalDamage"],"tags":["damage","physical","attack"]},"B":{"side":"SUFFIX","effect":"+(41—60)% Surpassing chance to fire an additional Arrow","family":["AdditionalArrows"],"tags":["attack"]}},"start":["X","K0","K1"],"acquire":["A","B"],"remove":["X"],"keep":["K0","K1"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","annul.X","sideAnnul.X"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","annul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","sideAnnul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다. 접두만 제거"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\",\"annul.X\",\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\",\"annul.X\",\"exalt.B\",\"exalt.A\"],[\"exalt.A\",\"annul.X\",\"exalt.B\",\"exalt.A\",\"exalt.B\"],[\"exalt.B\",\"annul.X\",\"exalt.A\",\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[],"uniqueFull":0,"missingFull":6,"bad":[[1,"RARE/K0,K1,X 에서 exalt.A 실행 불가"],[2,"RARE/B,K0,K1,X 에서 exalt.A 실행 불가"],[3,"RARE/K0,K1,X 에서 exalt.A 실행 불가"],[4,"RARE/A,B,K0,K1 에서 exalt.B 실행 불가"]],"prefix":[[4,3,2]]}
```

### 호출 68 · Double Belt · tag-9 · survey

기대 경로 6개.
```json
[["exalt.A","exalt.B"],["exalt.A","tagExalt.B"],["exalt.B","exalt.A"],["exalt.B","tagExalt.A"],["tagExalt.A","exalt.B"],["tagExalt.A","tagExalt.B"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `ba524f263568` · 입력 JSON
```json
{"base":"Double Belt","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"(20—30)% increased Charm Charges gained","tags":["charm"],"stats":[{"id":"charm_charges_gained_+%","min":20,"max":30}]},"mods":{"K":{"side":"SUFFIX","effect":"(14—16)% reduced Flask Charges used","family":["BeltFlaskChargesUsed"],"tags":["flask"]},"A":{"side":"PREFIX","effect":"(11—16)% increased Flask Mana Recovery rate","family":["BeltFlaskRecoveryRate"],"tags":["flask","resource","mana"]},"B":{"side":"PREFIX","effect":"+(55—64) to maximum Mana","family":["IncreasedMana"],"tags":["resource","mana"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","tagExalt.A","tagExalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","tagExalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 현재 implicit·explicit 태그 합집합이 비지 않으면 그중 하나 이상 겹치는 옵션만 추가","tagExalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 현재 implicit·explicit 태그 합집합이 비지 않으면 그중 하나 이상 겹치는 옵션만 추가"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"tagExalt.A\",\"exalt.B\"],[\"exalt.A\",\"tagExalt.B\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3],"uniqueFull":3,"missingFull":3,"bad":[],"prefix":[]}
```

### 호출 69 · Obliterator Bow · swap-9 · survey

기대 경로 5개.
```json
[["annul.X","exalt.A","exalt.B"],["annul.X","exalt.B","exalt.A"],["chaos.X>A","exalt.B"],["exalt.B","annul.X","exalt.A"],["exalt.B","chaos.X>A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `19f6d754cc2f` · 입력 JSON
```json
{"base":"Obliterator Bow","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"50% reduced Projectile Range","tags":[],"stats":[{"id":"projectile_attack_range_+%","min":-50,"max":-50}]},"mods":{"A":{"side":"PREFIX","effect":"Adds (1—2) to (36—52) Lightning Damage","family":["LightningDamage"],"tags":["elemental_damage","damage","elemental","lightning","attack"]},"X":{"side":"PREFIX","effect":"Adds 1 to (13—19) Lightning Damage","family":["LightningDamage"],"tags":["elemental_damage","damage","elemental","lightning","attack"]},"K":{"side":"SUFFIX","effect":"+(10—11)% to Critical Damage Bonus","family":["CriticalStrikeMultiplier"],"tags":["damage","attack","critical"]},"B":{"side":"SUFFIX","effect":"Leeches (7—7.9)% of Physical Damage as Life","family":["LifeLeech"],"tags":["resource","life","physical","attack"]}},"start":["K","X"],"acquire":["A","B"],"remove":["X"],"keep":["K"],"goalRarity":"RARE","actions":["annul.X","exalt.A","exalt.B","chaos.X>A"],"actionRules":{"annul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","chaos.X>A":"Rare에서 지정한 기존 옵션이 제거되고 이어 목표 옵션이 뽑힌 한 번의 교체."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"annul.X\",\"exalt.A\",\"exalt.B\"],[\"chaos.X>A\"]],\"alternatives\":[[\"exalt.A\",\"exalt.B\",\"annul.X\"],[\"exalt.B\",\"annul.X\",\"exalt.A\"],[\"chaos.X>A\",\"annul.X\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,4],"uniqueFull":2,"missingFull":3,"bad":[[2,"목표 미달 RARE/A,K"],[3,"RARE/K,X 에서 exalt.A 실행 불가"],[5,"RARE/A,K 에서 annul.X 실행 불가"]],"prefix":[]}
```

### 호출 70 · Gemini Bow · hybrid-9 · survey

기대 경로 2개.
```json
[["exalt.A","exalt.B"],["exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `16e0b76b083f` · 입력 JSON
```json
{"base":"Gemini Bow","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"+50% Surpassing chance to fire an additional Arrow","tags":["attack"],"stats":[{"id":"chance_to_fire_1_additional_projectile_%_with_rollover_with_bow_attacks","min":50,"max":50}]},"mods":{"A":{"side":"PREFIX","effect":"(55—64)% increased Physical Damage+(124—149) to Accuracy Rating","family":["LocalIncreasedPhysicalDamagePercentAndAccuracyRating"],"tags":["physical_damage","damage","physical","attack"]},"B":{"side":"SUFFIX","effect":"Leeches (5—5.9)% of Physical Damage as Mana","family":["ManaLeech"],"tags":["resource","mana","physical","attack"]},"K":{"side":"SUFFIX","effect":"+(14—16)% to Critical Damage Bonus","family":["CriticalStrikeMultiplier"],"tags":["damage","attack","critical"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","requiredStatRows":["local_accuracy_rating","local_physical_damage_+%","local_mana_leech_from_physical_damage_permyriad"],"actions":["exalt.A","exalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2],"uniqueFull":2,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 71 · Diamond · side-9 · survey

기대 경로 8개.
```json
[["dextral_exaltation.B","exalt.A"],["dextral_exaltation.B","sinistral_exaltation.A"],["exalt.A","dextral_exaltation.B"],["exalt.A","exalt.B"],["exalt.B","exalt.A"],["exalt.B","sinistral_exaltation.A"],["sinistral_exaltation.A","dextral_exaltation.B"],["sinistral_exaltation.A","exalt.B"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `8e0e79b42f01` · 입력 JSON
```json
{"base":"Diamond","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[2,2]},"implicit":null,"mods":{"K":{"side":"PREFIX","effect":"(8—12)% increased Area of Effect of Curses","family":["CurseAreaOfEffect"],"tags":["caster","curse"]},"A":{"side":"PREFIX","effect":"(5—15)% increased Magnitude of Ailments you inflict","family":["AilmentEffect"],"tags":["damage","ailment"]},"B":{"side":"SUFFIX","effect":"(10—20)% increased Totem Placement speed","family":["SummonTotemCastSpeed"],"tags":["speed"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","sinistral_exaltation.A","dextral_exaltation.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","sinistral_exaltation.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 접두만 추가","dextral_exaltation.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 접미만 추가"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"sinistral_exaltation.A\",\"dextral_exaltation.B\"],[\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3],"uniqueFull":3,"missingFull":5,"bad":[],"prefix":[]}
```

### 호출 72 · Fine Belt · implicit-9 · survey

기대 경로 2개.
```json
[["exalt.A","exalt.B"],["exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `16e0b76b083f` · 입력 JSON
```json
{"base":"Fine Belt","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"Flasks gain 0.17 charges per Second","tags":[],"stats":[{"id":"generate_x_charges_for_any_flask_per_minute","min":10,"max":10}]},"mods":{"A":{"side":"PREFIX","effect":"(16—21)% increased Charm Effect Duration","family":["BeltCharmDuration"],"tags":["charm"]},"B":{"side":"SUFFIX","effect":"+(21—25)% to Cold Resistance","family":["ColdResistance"],"tags":["cold_resistance","elemental_resistance","elemental","cold","resistance"]},"K":{"side":"PREFIX","effect":"+(108—140) to Armour","family":["IncreasedPhysicalDamageReductionRating"],"tags":["defences","armour"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","keepImplicitStat":{"id":"generate_x_charges_for_any_flask_per_minute","min":10,"max":10},"actions":["exalt.A","exalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2],"uniqueFull":2,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 73 · Tasalian Greaves · fracture-9 · survey

기대 경로 4개.
```json
[["exalt.A","exalt.B","fracture.K"],["exalt.A","fracture.K","exalt.B"],["exalt.B","exalt.A","fracture.K"],["exalt.B","fracture.K","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `5906d0002371` · 입력 JSON
```json
{"base":"Tasalian Greaves","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"K":{"side":"PREFIX","effect":"+(120—149) to maximum Life","family":["IncreasedLife"],"tags":["resource","life"]},"J":{"side":"SUFFIX","effect":"+(6—11) to Stun Threshold","family":["StunThreshold"],"tags":[]},"L":{"side":"PREFIX","effect":"(21—26)% increased Armour\n+(25—40) to Stun Threshold","family":["DefencesPercentAndStunThreshold"],"tags":[]},"A":{"side":"SUFFIX","effect":"+(12—15)% to Chaos Resistance","family":["ChaosResistance"],"tags":["chaos_resistance","chaos","resistance"]},"B":{"side":"SUFFIX","effect":"(41—45)% reduced Shock duration on you","family":["ReducedAilmentDuration"],"tags":["elemental","lightning","ailment"]}},"start":["K","J","L"],"acquire":["A","B"],"remove":[],"keep":["K","J","L"],"goalRarity":"RARE","mustFracture":"K","actions":["exalt.A","exalt.B","fracture.K"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","fracture.K":"Rare 명시 4개 이상에서 보유 K가 분열되는 결과. 옵션 수는 그대로다."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\",\"fracture.K\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\",\"fracture.K\"],[\"fracture.K\",\"exalt.A\",\"exalt.B\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2],"uniqueFull":2,"missingFull":2,"bad":[[3,"RARE/J,K,L 에서 fracture.K 실행 불가"]],"prefix":[]}
```

### 호출 74 · Engraved Crossbow · room-10 · survey

기대 경로 15개.
```json
[["annul.X","annul.Y","exalt.A","exalt.B"],["annul.X","annul.Y","exalt.B","exalt.A"],["annul.X","exalt.A","annul.Y","exalt.B"],["annul.X","exalt.A","exalt.B","annul.Y"],["annul.X","exalt.B","annul.Y","exalt.A"],["annul.X","exalt.B","exalt.A","annul.Y"],["annul.Y","annul.X","exalt.A","exalt.B"],["annul.Y","annul.X","exalt.B","exalt.A"],["annul.Y","exalt.B","annul.X","exalt.A"],["doubleAnnul.X+Y","exalt.A","exalt.B"],["doubleAnnul.X+Y","exalt.B","exalt.A"],["exalt.B","annul.X","annul.Y","exalt.A"],["exalt.B","annul.X","exalt.A","annul.Y"],["exalt.B","annul.Y","annul.X","exalt.A"],["exalt.B","doubleAnnul.X+Y","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `740f5afe337e` · 입력 JSON
```json
{"base":"Engraved Crossbow","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"X":{"side":"SUFFIX","effect":"(5—7)% increased Attack Speed","family":["IncreasedAttackSpeed"],"tags":["attack","speed"]},"K0":{"side":"SUFFIX","effect":"Gain (6—9) Mana per enemy killed","family":["ManaGainedFromEnemyDeath"],"tags":["mana"]},"K1":{"side":"SUFFIX","effect":"Leeches (9—9.9)% of Physical Damage as Life","family":["LifeLeech"],"tags":["life","physical","attack"]},"A":{"side":"SUFFIX","effect":"Grants 3 Life per Enemy Hit","family":["LifeGainPerTarget"],"tags":["life","attack"]},"B":{"side":"PREFIX","effect":"(135—154)% increased Physical Damage","family":["LocalPhysicalDamagePercent"],"tags":["damage","physical","attack"]},"Y":{"side":"PREFIX","effect":"Adds (25—30) to (38—46) Cold Damage","family":["ColdDamage"],"tags":["damage","elemental","cold","attack"]}},"start":["X","K0","K1","Y"],"acquire":["A","B"],"remove":["X","Y"],"keep":["K0","K1"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","annul.X","annul.Y","doubleAnnul.X+Y"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","annul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","annul.Y":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","doubleAnnul.X+Y":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다. 한 번에 서로 다른 두 개 제거"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"annul.X\",\"exalt.A\",\"exalt.B\"],[\"annul.Y\",\"exalt.A\",\"exalt.B\"],[\"doubleAnnul.X+Y\",\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.A\",\"exalt.B\",\"annul.X\"],[\"exalt.A\",\"exalt.B\",\"annul.Y\"],[\"exalt.A\",\"exalt.B\",\"doubleAnnul.X+Y\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[3],"uniqueFull":1,"missingFull":14,"bad":[[1,"목표 미달 RARE/A,B,K0,K1,Y"],[2,"RARE/K0,K1,X 에서 exalt.A 실행 불가"],[4,"RARE/K0,K1,X,Y 에서 exalt.A 실행 불가"],[5,"RARE/K0,K1,X,Y 에서 exalt.A 실행 불가"],[6,"RARE/K0,K1,X,Y 에서 exalt.A 실행 불가"]],"prefix":[]}
```

### 호출 75 · Sacramental Robe · tag-10 · survey

기대 경로 6개.
```json
[["exalt.A","exalt.B"],["exalt.A","tagExalt.B"],["exalt.B","exalt.A"],["exalt.B","tagExalt.A"],["tagExalt.A","exalt.B"],["tagExalt.A","tagExalt.B"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `ba524f263568` · 입력 JSON
```json
{"base":"Sacramental Robe","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"(20—25)% increased Energy Shield Recharge Rate","tags":["defences","energyshield"],"stats":[{"id":"energy_shield_recharge_rate_+%","min":20,"max":25}]},"mods":{"K":{"side":"PREFIX","effect":"(101—151) to (152—220) Physical Thorns damage","family":["Thorns"],"tags":["physical_damage","damage","physical"]},"A":{"side":"PREFIX","effect":"(21—26)% increased Energy Shield\n+(20—25) to maximum Life","family":["BaseLocalDefencesAndLife"],"tags":["defences","resource","life","energy_shield"]},"B":{"side":"PREFIX","effect":"+(20—29) to maximum Life","family":["IncreasedLife"],"tags":["resource","life"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","tagExalt.A","tagExalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","tagExalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 현재 implicit·explicit 태그 합집합이 비지 않으면 그중 하나 이상 겹치는 옵션만 추가","tagExalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 현재 implicit·explicit 태그 합집합이 비지 않으면 그중 하나 이상 겹치는 옵션만 추가"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"],[\"tagExalt.A\",\"exalt.B\"],[\"exalt.A\",\"tagExalt.B\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3,4],"uniqueFull":4,"missingFull":2,"bad":[],"prefix":[]}
```

### 호출 76 · Sanctified Staff · swap-10 · survey

기대 경로 5개.
```json
[["annul.X","exalt.A","exalt.B"],["annul.X","exalt.B","exalt.A"],["chaos.X>A","exalt.B"],["exalt.B","annul.X","exalt.A"],["exalt.B","chaos.X>A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `19f6d754cc2f` · 입력 JSON
```json
{"base":"Sanctified Staff","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"A":{"side":"SUFFIX","effect":"(50—59)% increased Critical Hit Chance for Spells","family":["SpellCriticalStrikeChanceIncrease"],"tags":["caster","critical"]},"X":{"side":"SUFFIX","effect":"(40—49)% increased Critical Hit Chance for Spells","family":["SpellCriticalStrikeChanceIncrease"],"tags":["caster","critical"]},"K":{"side":"PREFIX","effect":"(30—38)% increased Spell Damage\n+(34—40) to maximum Mana","family":["SpellDamageAndMana"],"tags":["mana","damage","caster"]},"B":{"side":"PREFIX","effect":"(69—88)% increased Cold Damage","family":["WeaponDamageTypePrefix"],"tags":["damage","elemental","cold"]}},"start":["K","X"],"acquire":["A","B"],"remove":["X"],"keep":["K"],"goalRarity":"RARE","actions":["annul.X","exalt.A","exalt.B","chaos.X>A"],"actionRules":{"annul.X":"지정된 제거 결과가 나온 분기. 제거 이후에만 자리가 빈다.","exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","chaos.X>A":"Rare에서 지정한 기존 옵션이 제거되고 이어 목표 옵션이 뽑힌 한 번의 교체."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"annul.X\",\"exalt.A\",\"exalt.B\"],[\"chaos.X>A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"annul.X\",\"exalt.A\"],[\"exalt.A\",\"chaos.X>A\",\"exalt.B\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3],"uniqueFull":3,"missingFull":2,"bad":[[4,"RARE/K,X 에서 exalt.A 실행 불가"]],"prefix":[]}
```

### 호출 77 · Shrine Sceptre · hybrid-10 · survey

기대 경로 2개.
```json
[["exalt.A","exalt.B"],["exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `16e0b76b083f` · 입력 JSON
```json
{"base":"Shrine Sceptre","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"A":{"side":"PREFIX","effect":"(27—30)% increased Spirit+(34—37) to maximum Mana","family":["LocalIncreasedSpiritAndMana"],"tags":[]},"B":{"side":"SUFFIX","effect":"(20—29)% increased Mana Regeneration Rate","family":["ManaRegeneration"],"tags":["resource","mana"]},"K":{"side":"SUFFIX","effect":"Allies in your Presence have (13—16)% increased Cast Speed","family":["IncreasedCastSpeed"],"tags":["caster_speed","caster","speed"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","requiredStatRows":["local_spirit_+%","base_maximum_mana","mana_regeneration_rate_+%"],"actions":["exalt.A","exalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2],"uniqueFull":2,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 78 · Sekhema Sandals · side-10 · survey

기대 경로 8개.
```json
[["dextral_exaltation.A","exalt.B"],["dextral_exaltation.A","sinistral_exaltation.B"],["exalt.A","exalt.B"],["exalt.A","sinistral_exaltation.B"],["exalt.B","dextral_exaltation.A"],["exalt.B","exalt.A"],["sinistral_exaltation.B","dextral_exaltation.A"],["sinistral_exaltation.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `fd90d7030706` · 입력 JSON
```json
{"base":"Sekhema Sandals","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"K":{"side":"SUFFIX","effect":"(36—40)% reduced Chill Duration on you","family":["ReducedAilmentDuration"],"tags":["elemental","cold","ailment"]},"A":{"side":"SUFFIX","effect":"+(41—45)% to Lightning Resistance","family":["LightningResistance"],"tags":["elemental_resistance","lightning_resistance","elemental","lightning","resistance"]},"B":{"side":"PREFIX","effect":"+(25—34) to maximum Mana","family":["IncreasedMana"],"tags":["resource","mana"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","actions":["exalt.A","exalt.B","dextral_exaltation.A","sinistral_exaltation.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","dextral_exaltation.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 접미만 추가","sinistral_exaltation.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 접두만 추가"}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"dextral_exaltation.A\",\"exalt.B\"],[\"sinistral_exaltation.B\",\"exalt.A\"]],\"alternatives\":[[\"exalt.A\",\"exalt.B\"],[\"dextral_exaltation.A\",\"dextral_exaltation.A\"],[\"sinistral_exaltation.B\",\"sinistral_exaltation.B\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3],"uniqueFull":3,"missingFull":5,"bad":[[4,"RARE/A,K 에서 dextral_exaltation.A 실행 불가"],[5,"RARE/B,K 에서 sinistral_exaltation.B 실행 불가"]],"prefix":[]}
```

### 호출 79 · Serrated Quiver · implicit-10 · survey

기대 경로 2개.
```json
[["exalt.A","exalt.B"],["exalt.B","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `16e0b76b083f` · 입력 JSON
```json
{"base":"Serrated Quiver","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"Attacks have (20—30)% chance to cause Bleeding","tags":["physical","attack","ailment"],"stats":[{"id":"bleed_on_hit_with_attacks_%","min":20,"max":30}]},"mods":{"A":{"side":"SUFFIX","effect":"Gain (41—53) Life per enemy killed","family":["LifeGainedFromEnemyDeath"],"tags":["life"]},"B":{"side":"PREFIX","effect":"Adds (11—16) to (21—26) Fire damage to Attacks","family":["FireDamage"],"tags":["damage","elemental","fire","attack"]},"K":{"side":"SUFFIX","effect":"(11—13)% increased Attack Speed","family":["IncreasedAttackSpeed"],"tags":["attack","speed"]}},"start":["K"],"acquire":["A","B"],"remove":[],"keep":["K"],"goalRarity":"RARE","keepImplicitStat":{"id":"bleed_on_hit_with_attacks_%","min":20,"max":30},"actions":["exalt.A","exalt.B"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2],"uniqueFull":2,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 80 · Wide Belt · fracture-10 · survey

기대 경로 4개.
```json
[["exalt.A","exalt.B","fracture.K"],["exalt.A","fracture.K","exalt.B"],["exalt.B","exalt.A","fracture.K"],["exalt.B","fracture.K","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `5906d0002371` · 입력 JSON
```json
{"base":"Wide Belt","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"(20—30)% increased Flask Charges gained","tags":["flask"],"stats":[{"id":"flask_charges_gained_+%","min":20,"max":30}]},"mods":{"K":{"side":"PREFIX","effect":"+(25—34) to maximum Mana","family":["IncreasedMana"],"tags":["resource","mana"]},"J":{"side":"SUFFIX","effect":"+(11—15)% to Lightning Resistance","family":["LightningResistance"],"tags":["elemental_resistance","lightning_resistance","elemental","lightning","resistance"]},"L":{"side":"SUFFIX","effect":"(9.1—13) Life Regeneration per second","family":["LifeRegeneration"],"tags":["flat_life_regen","resource","life"]},"A":{"side":"PREFIX","effect":"(10—15)% increased Charm Effect Duration","family":["BeltCharmDuration"],"tags":["charm"]},"B":{"side":"PREFIX","effect":"+(69—107) to Armour","family":["IncreasedPhysicalDamageReductionRating"],"tags":["defences","armour"]}},"start":["K","J","L"],"acquire":["A","B"],"remove":[],"keep":["K","J","L"],"goalRarity":"RARE","mustFracture":"K","actions":["exalt.A","exalt.B","fracture.K"],"actionRules":{"exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","fracture.K":"Rare 명시 4개 이상에서 보유 K가 분열되는 결과. 옵션 수는 그대로다."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"exalt.A\",\"exalt.B\",\"fracture.K\"]],\"alternatives\":[[\"exalt.B\",\"exalt.A\",\"fracture.K\"],[\"exalt.A\",\"fracture.K\",\"exalt.B\"],[\"exalt.B\",\"fracture.K\",\"exalt.A\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3,4],"uniqueFull":4,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 81 · Linen Belt · batch-1 · survey

기대 경로 10개.
```json
[["exalt.A","exalt.B","exalt.C"],["exalt.A","exalt.C","exalt.B"],["exalt.A","greaterEx.B+C"],["exalt.B","exalt.A","exalt.C"],["exalt.B","exalt.C","exalt.A"],["exalt.C","exalt.A","exalt.B"],["exalt.C","exalt.B","exalt.A"],["exalt.C","greaterEx.A+B"],["greaterEx.A+B","exalt.C"],["greaterEx.B+C","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `6402785b9126` · 입력 JSON
```json
{"base":"Linen Belt","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"(20—30)% increased Mana Recovery from Flasks","tags":["flask","resource","mana"],"stats":[{"id":"flask_mana_to_recover_+%","min":20,"max":30}]},"mods":{"A":{"side":"PREFIX","effect":"(29—34)% increased Flask Life Recovery rate","family":["BeltFlaskRecoveryRate"],"tags":["flask","resource","life"]},"B":{"side":"SUFFIX","effect":"+(41—45)% to Fire Resistance","family":["FireResistance"],"tags":["elemental_resistance","fire_resistance","elemental","fire","resistance"]},"C":{"side":"PREFIX","effect":"+(226—267) to Armour","family":["IncreasedPhysicalDamageReductionRating"],"tags":["defences","armour"]},"K":{"side":"SUFFIX","effect":"(13.1—18) Life Regeneration per second","family":["LifeRegeneration"],"tags":["flat_life_regen","resource","life"]}},"start":["K"],"acquire":["A","B","C"],"remove":[],"keep":["K"],"goalRarity":"RARE","actions":["greaterEx.A+B","greaterEx.B+C","exalt.A","exalt.B","exalt.C"],"actionRules":{"greaterEx.A+B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 자리가 두 개 이상이면 서로 다른 두 옵션을 한 번의 엑잘에서 부여한다. A+B는 두 옵션이 함께 붙은 결과다.","greaterEx.B+C":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 자리가 두 개 이상이면 서로 다른 두 옵션을 한 번의 엑잘에서 부여한다. A+B는 두 옵션이 함께 붙은 결과다.","exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.C":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"greaterEx.A+B\",\"greaterEx.B+C\"]],\"alternatives\":[[\"exalt.A\",\"exalt.B\",\"exalt.C\"],[\"greaterEx.A+B\",\"exalt.C\"],[\"exalt.A\",\"greaterEx.B+C\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[2,3,4],"uniqueFull":3,"missingFull":7,"bad":[[1,"RARE/A,B,K 에서 greaterEx.B+C 실행 불가"]],"prefix":[]}
```

### 호출 82 · Seastorm Mantle · batch-2 · survey

기대 경로 2개.
```json
[["alchemy.A+B+C+D","exalt.E"],["alchemy.A+B+C+E","exalt.D"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `afc927b649b7` · 입력 JSON
```json
{"base":"Seastorm Mantle","ilvl":82,"rarity":"NORMAL","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"(8—14)% of Damage taken Recouped as Life","tags":["resource","life"],"stats":[{"id":"damage_taken_goes_to_life_over_4_seconds_%","min":8,"max":14}]},"mods":{"A":{"side":"PREFIX","effect":"+(60—69) to maximum Life","family":["IncreasedLife"],"tags":["resource","life"]},"B":{"side":"SUFFIX","effect":"+(21—24) to Strength","family":["Strength"],"tags":["attribute"]},"C":{"side":"PREFIX","effect":"+(54—56) to Spirit","family":["BaseSpirit"],"tags":[]},"D":{"side":"SUFFIX","effect":"+(6—10)% to Fire Resistance","family":["FireResistance"],"tags":["elemental_resistance","fire_resistance","elemental","fire","resistance"]},"E":{"side":"PREFIX","effect":"(11—16) to (17—23) Physical Thorns damage","family":["Thorns"],"tags":["physical_damage","damage","physical"]}},"start":[],"acquire":["A","B","C","D","E"],"remove":[],"keep":[],"goalRarity":"RARE","actions":["alchemy.A+B+C+D","alchemy.A+B+C+E","exalt.D","exalt.E"],"actionRules":{"alchemy.A+B+C+D":"Normal 또는 Magic을 Rare로 만들며 일반 옵션 네 개를 부여한다. 이 토큰은 명시된 네 옵션을 함께 획득한 결과다.","alchemy.A+B+C+E":"Normal 또는 Magic을 Rare로 만들며 일반 옵션 네 개를 부여한다. 이 토큰은 명시된 네 옵션을 함께 획득한 결과다.","exalt.D":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.E":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"alchemy.A+B+C+D\",\"exalt.D\"],[\"alchemy.A+B+C+E\",\"exalt.E\"]],\"alternatives\":[[\"alchemy.A+B+C+D\",\"exalt.E\"],[\"alchemy.A+B+C+E\",\"exalt.D\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[3,4],"uniqueFull":2,"missingFull":0,"bad":[[1,"RARE/A,B,C,D 에서 exalt.D 실행 불가"],[2,"RARE/A,B,C,E 에서 exalt.E 실행 불가"]],"prefix":[]}
```

### 호출 83 · Ornate Belt · batch-3 · survey

기대 경로 10개.
```json
[["exalt.A","exalt.B","exalt.C"],["exalt.A","exalt.C","exalt.B"],["exalt.A","greaterEx.B+C"],["exalt.B","exalt.A","exalt.C"],["exalt.B","exalt.C","exalt.A"],["exalt.C","exalt.A","exalt.B"],["exalt.C","exalt.B","exalt.A"],["exalt.C","greaterEx.A+B"],["greaterEx.A+B","exalt.C"],["greaterEx.B+C","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `6402785b9126` · 입력 JSON
```json
{"base":"Ornate Belt","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"(10—15)% reduced Charm Charges used","tags":["charm"],"stats":[{"id":"charm_charges_used_+%","min":-15,"max":-10}]},"mods":{"A":{"side":"PREFIX","effect":"+(268—311) to Armour","family":["IncreasedPhysicalDamageReductionRating"],"tags":["defences","armour"]},"B":{"side":"SUFFIX","effect":"(5—10)% increased Charm Charges gained","family":["BeltCharmChargesGained"],"tags":["charm"]},"C":{"side":"PREFIX","effect":"+(150—174) to maximum Life","family":["IncreasedLife"],"tags":["resource","life"]},"K":{"side":"SUFFIX","effect":"+(11—15)% to Fire Resistance","family":["FireResistance"],"tags":["elemental_resistance","fire_resistance","elemental","fire","resistance"]}},"start":["K"],"acquire":["A","B","C"],"remove":[],"keep":["K"],"goalRarity":"RARE","actions":["greaterEx.A+B","greaterEx.B+C","exalt.A","exalt.B","exalt.C"],"actionRules":{"greaterEx.A+B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 자리가 두 개 이상이면 서로 다른 두 옵션을 한 번의 엑잘에서 부여한다. A+B는 두 옵션이 함께 붙은 결과다.","greaterEx.B+C":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 자리가 두 개 이상이면 서로 다른 두 옵션을 한 번의 엑잘에서 부여한다. A+B는 두 옵션이 함께 붙은 결과다.","exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.C":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"greaterEx.A+B\",\"exalt.C\"]],\"alternatives\":[[\"greaterEx.B+C\",\"exalt.A\"],[\"exalt.A\",\"greaterEx.B+C\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2,3],"uniqueFull":3,"missingFull":7,"bad":[],"prefix":[]}
```

### 호출 84 · Shrouded Mail · batch-4 · survey

기대 경로 2개.
```json
[["alchemy.A+B+C+D","exalt.E"],["alchemy.A+B+C+E","exalt.D"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `afc927b649b7` · 입력 JSON
```json
{"base":"Shrouded Mail","ilvl":82,"rarity":"NORMAL","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"+(20—25)% to Fire Resistance","tags":["elemental_resistance","fire_resistance","elemental","fire","resistance"],"stats":[{"id":"base_fire_damage_resistance_%","min":20,"max":25}]},"mods":{"A":{"side":"PREFIX","effect":"(11—16) to (17—23) Physical Thorns damage","family":["Thorns"],"tags":["physical_damage","damage","physical"]},"B":{"side":"SUFFIX","effect":"(56—60)% reduced Duration of Bleeding on You","family":["ReducedAilmentDuration"],"tags":["bleed","physical","ailment"]},"C":{"side":"PREFIX","effect":"(43—55)% increased Armour and Evasion","family":["DefencesPercent"],"tags":["defences","armour","evasion"]},"D":{"side":"SUFFIX","effect":"+(36—40)% to Lightning Resistance","family":["LightningResistance"],"tags":["elemental_resistance","lightning_resistance","elemental","lightning","resistance"]},"E":{"side":"PREFIX","effect":"+(24—32) to Armour\n+(22—29) to Evasion Rating\n(27—32)% increased Armour and Evasion","family":["BaseLocalDefencesAndDefencePercent"],"tags":["defences","armour","evasion"]}},"start":[],"acquire":["A","B","C","D","E"],"remove":[],"keep":[],"goalRarity":"RARE","actions":["alchemy.A+B+C+D","alchemy.A+B+C+E","exalt.D","exalt.E"],"actionRules":{"alchemy.A+B+C+D":"Normal 또는 Magic을 Rare로 만들며 일반 옵션 네 개를 부여한다. 이 토큰은 명시된 네 옵션을 함께 획득한 결과다.","alchemy.A+B+C+E":"Normal 또는 Magic을 Rare로 만들며 일반 옵션 네 개를 부여한다. 이 토큰은 명시된 네 옵션을 함께 획득한 결과다.","exalt.D":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.E":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"alchemy.A+B+C+D\",\"exalt.D\"],[\"alchemy.A+B+C+E\",\"exalt.E\"]],\"alternatives\":[[\"alchemy.A+B+C+D\",\"exalt.E\"],[\"alchemy.A+B+C+E\",\"exalt.D\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[3,4],"uniqueFull":2,"missingFull":0,"bad":[[1,"RARE/A,B,C,D 에서 exalt.D 실행 불가"],[2,"RARE/A,B,C,E 에서 exalt.E 실행 불가"]],"prefix":[]}
```

### 호출 85 · Warlord Cuirass · batch-5 · survey

기대 경로 10개.
```json
[["exalt.A","exalt.B","exalt.C"],["exalt.A","exalt.C","exalt.B"],["exalt.A","greaterEx.B+C"],["exalt.B","exalt.A","exalt.C"],["exalt.B","exalt.C","exalt.A"],["exalt.C","exalt.A","exalt.B"],["exalt.C","exalt.B","exalt.A"],["exalt.C","greaterEx.A+B"],["greaterEx.A+B","exalt.C"],["greaterEx.B+C","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `6402785b9126` · 입력 JSON
```json
{"base":"Warlord Cuirass","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"+(15—25)% of Armour also applies to Elemental Damage","tags":["defences","armour","elemental"],"stats":[{"id":"armour_%_applies_to_fire_cold_lightning_damage","min":15,"max":25}]},"mods":{"A":{"side":"PREFIX","effect":"(40—60) to (61—92) Physical Thorns damage","family":["Thorns"],"tags":["physical_damage","damage","physical"]},"B":{"side":"SUFFIX","effect":"(36—40)% reduced Ignite Duration on you","family":["ReducedAilmentDuration"],"tags":["elemental","fire","ailment"]},"C":{"side":"PREFIX","effect":"+(58—75) to Armour(33—38)% increased Armour","family":["BaseLocalDefencesAndDefencePercent"],"tags":["defences","armour"]},"K":{"side":"SUFFIX","effect":"+(36—40)% to Fire Resistance","family":["FireResistance"],"tags":["elemental_resistance","fire_resistance","elemental","fire","resistance"]}},"start":["K"],"acquire":["A","B","C"],"remove":[],"keep":["K"],"goalRarity":"RARE","actions":["greaterEx.A+B","greaterEx.B+C","exalt.A","exalt.B","exalt.C"],"actionRules":{"greaterEx.A+B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 자리가 두 개 이상이면 서로 다른 두 옵션을 한 번의 엑잘에서 부여한다. A+B는 두 옵션이 함께 붙은 결과다.","greaterEx.B+C":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 자리가 두 개 이상이면 서로 다른 두 옵션을 한 번의 엑잘에서 부여한다. A+B는 두 옵션이 함께 붙은 결과다.","exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.C":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"greaterEx.A+B\",\"greaterEx.B+C\"]],\"alternatives\":[[\"exalt.A\",\"greaterEx.B+C\"],[\"greaterEx.A+B\",\"exalt.C\"],[\"exalt.A\",\"exalt.B\",\"exalt.C\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[2,3,4],"uniqueFull":3,"missingFull":7,"bad":[[1,"RARE/A,B,K 에서 greaterEx.B+C 실행 불가"]],"prefix":[]}
```

### 호출 86 · Wyrmscale Coat · batch-6 · survey

기대 경로 2개.
```json
[["alchemy.A+B+C+D","exalt.E"],["alchemy.A+B+C+E","exalt.D"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `afc927b649b7` · 입력 JSON
```json
{"base":"Wyrmscale Coat","ilvl":82,"rarity":"NORMAL","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"(30—40)% increased Elemental Ailment Threshold","tags":["ailment"],"stats":[{"id":"ailment_threshold_+%","min":30,"max":40}]},"mods":{"A":{"side":"PREFIX","effect":"(33—38)% increased Evasion Rating\n+(33—41) to maximum Life","family":["BaseLocalDefencesAndLife"],"tags":["defences","resource","life","evasion"]},"B":{"side":"SUFFIX","effect":"+(13—16) to Dexterity","family":["Dexterity"],"tags":["attribute"]},"C":{"side":"PREFIX","effect":"+(88—116) to Evasion Rating","family":["BaseLocalDefences"],"tags":["defences","evasion"]},"D":{"side":"SUFFIX","effect":"(3.1—4) Life Regeneration per second","family":["LifeRegeneration"],"tags":["flat_life_regen","resource","life"]},"E":{"side":"PREFIX","effect":"+(57—61) to Spirit","family":["BaseSpirit"],"tags":[]}},"start":[],"acquire":["A","B","C","D","E"],"remove":[],"keep":[],"goalRarity":"RARE","actions":["alchemy.A+B+C+D","alchemy.A+B+C+E","exalt.D","exalt.E"],"actionRules":{"alchemy.A+B+C+D":"Normal 또는 Magic을 Rare로 만들며 일반 옵션 네 개를 부여한다. 이 토큰은 명시된 네 옵션을 함께 획득한 결과다.","alchemy.A+B+C+E":"Normal 또는 Magic을 Rare로 만들며 일반 옵션 네 개를 부여한다. 이 토큰은 명시된 네 옵션을 함께 획득한 결과다.","exalt.D":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.E":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"alchemy.A+B+C+D\",\"exalt.D\"],[\"alchemy.A+B+C+E\",\"exalt.E\"]],\"alternatives\":[[\"alchemy.A+B+C+D\"],[\"alchemy.A+B+C+E\"],[\"exalt.D\"],[\"exalt.E\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[],"uniqueFull":0,"missingFull":2,"bad":[[1,"RARE/A,B,C,D 에서 exalt.D 실행 불가"],[2,"RARE/A,B,C,E 에서 exalt.E 실행 불가"],[3,"목표 미달 RARE/A,B,C,D"],[4,"목표 미달 RARE/A,B,C,E"],[5,"NORMAL/ 에서 exalt.D 실행 불가"],[6,"NORMAL/ 에서 exalt.E 실행 불가"]],"prefix":[]}
```

### 호출 87 · Wyrmscale Coat · batch-6 · repair

기대 경로는 호출 86의 2개와 같다.

시스템 문안 `fdefcc2650c7` · 응답 스키마 `c667f0e492c6` · 입력 JSON
```json
{"base":"Wyrmscale Coat","ilvl":82,"rarity":"NORMAL","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"(30—40)% increased Elemental Ailment Threshold","tags":["ailment"],"stats":[{"id":"ailment_threshold_+%","min":30,"max":40}]},"mods":{"A":{"side":"PREFIX","effect":"(33—38)% increased Evasion Rating\n+(33—41) to maximum Life","family":["BaseLocalDefencesAndLife"],"tags":["defences","resource","life","evasion"]},"B":{"side":"SUFFIX","effect":"+(13—16) to Dexterity","family":["Dexterity"],"tags":["attribute"]},"C":{"side":"PREFIX","effect":"+(88—116) to Evasion Rating","family":["BaseLocalDefences"],"tags":["defences","evasion"]},"D":{"side":"SUFFIX","effect":"(3.1—4) Life Regeneration per second","family":["LifeRegeneration"],"tags":["flat_life_regen","resource","life"]},"E":{"side":"PREFIX","effect":"+(57—61) to Spirit","family":["BaseSpirit"],"tags":[]}},"start":[],"acquire":["A","B","C","D","E"],"remove":[],"keep":[],"goalRarity":"RARE","actions":["alchemy.A+B+C+D","alchemy.A+B+C+E","exalt.D","exalt.E"],"actionRules":{"alchemy.A+B+C+D":"Normal 또는 Magic을 Rare로 만들며 일반 옵션 네 개를 부여한다. 이 토큰은 명시된 네 옵션을 함께 획득한 결과다.","alchemy.A+B+C+E":"Normal 또는 Magic을 Rare로 만들며 일반 옵션 네 개를 부여한다. 이 토큰은 명시된 네 옵션을 함께 획득한 결과다.","exalt.D":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.E":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."},"priorRejected":[[["alchemy.A+B+C+D","exalt.D"],"RARE/A,B,C,D 에서 exalt.D 실행 불가"],[["alchemy.A+B+C+E","exalt.E"],"RARE/A,B,C,E 에서 exalt.E 실행 불가"],[["alchemy.A+B+C+D"],"목표 미달 RARE/A,B,C,D"]]}
```

실제 응답
```json
{"raw":"{\"witness\":[[\"alchemy.A+B+C+E\"]],\"other\":[]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[],"uniqueFull":0,"missingFull":2,"bad":[[1,"목표 미달 RARE/A,B,C,E"]],"prefix":[]}
```

### 호출 88 · Wyrmscale Coat · batch-6 · compact

기대 경로는 호출 86의 2개와 같다.

시스템 문안 `b1d2f2dc0221` · 응답 스키마 `de0f259c50f1` · 입력 JSON
```json
{"base":"Wyrmscale Coat","ilvl":82,"rarity":"NORMAL","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"(30—40)% increased Elemental Ailment Threshold","tags":["ailment"],"stats":[{"id":"ailment_threshold_+%","min":30,"max":40}]},"mods":{"A":{"side":"PREFIX","effect":"(33—38)% increased Evasion Rating\n+(33—41) to maximum Life","family":["BaseLocalDefencesAndLife"],"tags":["defences","resource","life","evasion"]},"B":{"side":"SUFFIX","effect":"+(13—16) to Dexterity","family":["Dexterity"],"tags":["attribute"]},"C":{"side":"PREFIX","effect":"+(88—116) to Evasion Rating","family":["BaseLocalDefences"],"tags":["defences","evasion"]},"D":{"side":"SUFFIX","effect":"(3.1—4) Life Regeneration per second","family":["LifeRegeneration"],"tags":["flat_life_regen","resource","life"]},"E":{"side":"PREFIX","effect":"+(57—61) to Spirit","family":["BaseSpirit"],"tags":[]}},"start":[],"acquire":["A","B","C","D","E"],"remove":[],"keep":[],"goalRarity":"RARE","actions":["alchemy.A+B+C+D","alchemy.A+B+C+E","exalt.D","exalt.E"],"actionRules":{"alchemy.A+B+C+D":"Normal 또는 Magic을 Rare로 만들며 일반 옵션 네 개를 부여한다. 이 토큰은 명시된 네 옵션을 함께 획득한 결과다.","alchemy.A+B+C+E":"Normal 또는 Magic을 Rare로 만들며 일반 옵션 네 개를 부여한다. 이 토큰은 명시된 네 옵션을 함께 획득한 결과다.","exalt.D":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.E":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"routes\":[[\"alchemy.A+B+C+D\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[],"uniqueFull":0,"missingFull":2,"bad":[[1,"목표 미달 RARE/A,B,C,D"]],"prefix":[]}
```

### 호출 89 · Wyrmscale Coat · batch-6 · remaining

기대 경로는 호출 86의 2개와 같다.

시스템 문안 `fe60b9718f6c` · 응답 스키마 `d16fe369fd8d` · 입력 JSON
```json
{"base":"Wyrmscale Coat","ilvl":82,"rarity":"NORMAL","caps":{"magic":[1,1],"rare":[3,3]},"implicit":{"effect":"(30—40)% increased Elemental Ailment Threshold","tags":["ailment"],"stats":[{"id":"ailment_threshold_+%","min":30,"max":40}]},"mods":{"A":{"side":"PREFIX","effect":"(33—38)% increased Evasion Rating\n+(33—41) to maximum Life","family":["BaseLocalDefencesAndLife"],"tags":["defences","resource","life","evasion"]},"B":{"side":"SUFFIX","effect":"+(13—16) to Dexterity","family":["Dexterity"],"tags":["attribute"]},"C":{"side":"PREFIX","effect":"+(88—116) to Evasion Rating","family":["BaseLocalDefences"],"tags":["defences","evasion"]},"D":{"side":"SUFFIX","effect":"(3.1—4) Life Regeneration per second","family":["LifeRegeneration"],"tags":["flat_life_regen","resource","life"]},"E":{"side":"PREFIX","effect":"+(57—61) to Spirit","family":["BaseSpirit"],"tags":[]}},"start":[],"acquire":["A","B","C","D","E"],"remove":[],"keep":[],"goalRarity":"RARE","actions":["alchemy.A+B+C+D","alchemy.A+B+C+E","exalt.D","exalt.E"],"actionRules":{"alchemy.A+B+C+D":"Normal 또는 Magic을 Rare로 만들며 일반 옵션 네 개를 부여한다. 이 토큰은 명시된 네 옵션을 함께 획득한 결과다.","alchemy.A+B+C+E":"Normal 또는 Magic을 Rare로 만들며 일반 옵션 네 개를 부여한다. 이 토큰은 명시된 네 옵션을 함께 획득한 결과다.","exalt.D":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.E":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."},"gains":{"alchemy.A+B+C+D":["A","B","C","D"],"alchemy.A+B+C+E":["A","B","C","E"],"exalt.D":["D"],"exalt.E":["E"]}}
```

실제 응답
```json
{"raw":"{\"routes\":[{\"opening\":\"1|alchemy.A+B+C+D\",\"finishing\":\"2|exalt.E\"},{\"opening\":\"1|alchemy.A+B+C+E\",\"finishing\":\"2|exalt.D\"}]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[1,2],"uniqueFull":2,"missingFull":0,"bad":[],"prefix":[]}
```

### 호출 90 · Sleek Jacket · batch-7 · survey

기대 경로 10개.
```json
[["exalt.A","exalt.B","exalt.C"],["exalt.A","exalt.C","exalt.B"],["exalt.A","greaterEx.B+C"],["exalt.B","exalt.A","exalt.C"],["exalt.B","exalt.C","exalt.A"],["exalt.C","exalt.A","exalt.B"],["exalt.C","exalt.B","exalt.A"],["exalt.C","greaterEx.A+B"],["greaterEx.A+B","exalt.C"],["greaterEx.B+C","exalt.A"]]
```

시스템 문안 `bde3aec34834` · 응답 스키마 `6402785b9126` · 입력 JSON
```json
{"base":"Sleek Jacket","ilvl":82,"rarity":"RARE","caps":{"magic":[1,1],"rare":[3,3]},"implicit":null,"mods":{"A":{"side":"PREFIX","effect":"(56—67)% increased Evasion and Energy Shield","family":["DefencesPercent"],"tags":["defences","evasion","energy_shield"]},"B":{"side":"SUFFIX","effect":"(9.1—13) Life Regeneration per second","family":["LifeRegeneration"],"tags":["flat_life_regen","resource","life"]},"C":{"side":"PREFIX","effect":"+(42—64) to Evasion Rating\n+(16—21) to maximum Energy Shield","family":["BaseLocalDefences"],"tags":["defences","evasion","energy_shield"]},"K":{"side":"SUFFIX","effect":"+(9—12) to Intelligence","family":["Intelligence"],"tags":["attribute"]}},"start":["K"],"acquire":["A","B","C"],"remove":[],"keep":["K"],"goalRarity":"RARE","actions":["greaterEx.A+B","greaterEx.B+C","exalt.A","exalt.B","exalt.C"],"actionRules":{"greaterEx.A+B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 자리가 두 개 이상이면 서로 다른 두 옵션을 한 번의 엑잘에서 부여한다. A+B는 두 옵션이 함께 붙은 결과다.","greaterEx.B+C":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가. 자리가 두 개 이상이면 서로 다른 두 옵션을 한 번의 엑잘에서 부여한다. A+B는 두 옵션이 함께 붙은 결과다.","exalt.A":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.B":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가.","exalt.C":"Rare, 목표 일반 명시 옵션을 빈 접두/접미 자리에 추가."}}
```

실제 응답
```json
{"raw":"{\"main\":[[\"greaterEx.A+B\",\"greaterEx.B+C\"]],\"alternatives\":[[\"greaterEx.B+C\",\"greaterEx.A+B\"],[\"exalt.A\",\"exalt.B\",\"exalt.C\"]]}"}
```

비교 결과. 접두 구간은 첫 행동부터 목표를 충족한 단계까지만 인정했다.
```json
{"fullCandidate":[3],"uniqueFull":1,"missingFull":9,"bad":[[1,"RARE/A,B,K 에서 greaterEx.B+C 실행 불가"],[2,"RARE/B,C,K 에서 greaterEx.A+B 실행 불가"]],"prefix":[]}
```

## 응답 스키마

동일 스키마는 한 번만 수록했다. `enum`의 행동은 화폐 전략과 대상을 함께 지정한다.

### 스키마 ade86c24d889

```json
{"type":"object","properties":{"main":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","exalt.B","annul.X","sideAnnul.X"]}}},"alternatives":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","exalt.B","annul.X","sideAnnul.X"]}}}},"required":["main","alternatives"],"additionalProperties":false}
```

### 스키마 ba524f263568

```json
{"type":"object","properties":{"main":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","exalt.B","tagExalt.A","tagExalt.B"]}}},"alternatives":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","exalt.B","tagExalt.A","tagExalt.B"]}}}},"required":["main","alternatives"],"additionalProperties":false}
```

### 스키마 9775aa364937

```json
{"type":"object","properties":{"main":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["transmute.A","transmute.B","transmute.C","augment.A","augment.B","augment.C","regal.A","regal.B","regal.C","exalt.A","exalt.B","exalt.C"]}}},"alternatives":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["transmute.A","transmute.B","transmute.C","augment.A","augment.B","augment.C","regal.A","regal.B","regal.C","exalt.A","exalt.B","exalt.C"]}}}},"required":["main","alternatives"],"additionalProperties":false}
```

### 스키마 19f6d754cc2f

```json
{"type":"object","properties":{"main":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["annul.X","exalt.A","exalt.B","chaos.X>A"]}}},"alternatives":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["annul.X","exalt.A","exalt.B","chaos.X>A"]}}}},"required":["main","alternatives"],"additionalProperties":false}
```

### 스키마 16e0b76b083f

```json
{"type":"object","properties":{"main":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","exalt.B"]}}},"alternatives":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","exalt.B"]}}}},"required":["main","alternatives"],"additionalProperties":false}
```

### 스키마 8e0e79b42f01

```json
{"type":"object","properties":{"main":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","exalt.B","sinistral_exaltation.A","dextral_exaltation.B"]}}},"alternatives":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","exalt.B","sinistral_exaltation.A","dextral_exaltation.B"]}}}},"required":["main","alternatives"],"additionalProperties":false}
```

### 스키마 5906d0002371

```json
{"type":"object","properties":{"main":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","exalt.B","fracture.K"]}}},"alternatives":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","exalt.B","fracture.K"]}}}},"required":["main","alternatives"],"additionalProperties":false}
```

### 스키마 740f5afe337e

```json
{"type":"object","properties":{"main":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","exalt.B","annul.X","annul.Y","doubleAnnul.X+Y"]}}},"alternatives":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","exalt.B","annul.X","annul.Y","doubleAnnul.X+Y"]}}}},"required":["main","alternatives"],"additionalProperties":false}
```

### 스키마 fd90d7030706

```json
{"type":"object","properties":{"main":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","exalt.B","dextral_exaltation.A","sinistral_exaltation.B"]}}},"alternatives":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["exalt.A","exalt.B","dextral_exaltation.A","sinistral_exaltation.B"]}}}},"required":["main","alternatives"],"additionalProperties":false}
```

### 스키마 15e44c597944

```json
{"type":"object","properties":{"witness":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["transmute.A","transmute.B","transmute.C","augment.A","augment.B","augment.C","regal.A","regal.B","regal.C","exalt.A","exalt.B","exalt.C"]},"minItems":3,"maxItems":3}},"other":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["transmute.A","transmute.B","transmute.C","augment.A","augment.B","augment.C","regal.A","regal.B","regal.C","exalt.A","exalt.B","exalt.C"]},"minItems":3,"maxItems":3}}},"required":["witness","other"],"additionalProperties":false}
```

### 스키마 c2b4d1b28c15

```json
{"type":"object","properties":{"A":{"type":"array","items":{"type":"object","properties":{"A":{"type":"string","enum":["1|transmute.A"]},"B":{"type":"string","enum":["2|regal.B","2|augment.B","3|regal.B","3|exalt.B"]},"C":{"type":"string","enum":["2|regal.C","2|augment.C","3|regal.C","3|exalt.C"]}},"required":["A","B","C"],"additionalProperties":false}},"B":{"type":"array","items":{"type":"object","properties":{"A":{"type":"string","enum":["2|regal.A","2|augment.A","3|regal.A","3|exalt.A"]},"B":{"type":"string","enum":["1|transmute.B"]},"C":{"type":"string","enum":["2|regal.C","2|augment.C","3|regal.C","3|exalt.C"]}},"required":["A","B","C"],"additionalProperties":false}},"C":{"type":"array","items":{"type":"object","properties":{"A":{"type":"string","enum":["2|regal.A","2|augment.A","3|regal.A","3|exalt.A"]},"B":{"type":"string","enum":["2|regal.B","2|augment.B","3|regal.B","3|exalt.B"]},"C":{"type":"string","enum":["1|transmute.C"]}},"required":["A","B","C"],"additionalProperties":false}}},"required":["A","B","C"],"additionalProperties":false}
```

### 스키마 5e3d5b1e5e31

```json
{"type":"object","properties":{"A":{"type":"array","items":{"type":"object","properties":{"first":{"type":"string","enum":["1|transmute.A"]},"upgrade":{"type":"string","enum":["2|regal.B","2|regal.C","3|regal.B","3|regal.C"]},"other":{"type":"string","enum":["2|augment.B","2|augment.C","3|exalt.B","3|exalt.C"]}},"required":["first","upgrade","other"],"additionalProperties":false}},"B":{"type":"array","items":{"type":"object","properties":{"first":{"type":"string","enum":["1|transmute.B"]},"upgrade":{"type":"string","enum":["2|regal.A","2|regal.C","3|regal.A","3|regal.C"]},"other":{"type":"string","enum":["2|augment.A","2|augment.C","3|exalt.A","3|exalt.C"]}},"required":["first","upgrade","other"],"additionalProperties":false}},"C":{"type":"array","items":{"type":"object","properties":{"first":{"type":"string","enum":["1|transmute.C"]},"upgrade":{"type":"string","enum":["2|regal.A","2|regal.B","3|regal.A","3|regal.B"]},"other":{"type":"string","enum":["2|augment.A","2|augment.B","3|exalt.A","3|exalt.B"]}},"required":["first","upgrade","other"],"additionalProperties":false}}},"required":["A","B","C"],"additionalProperties":false}
```

### 스키마 6402785b9126

```json
{"type":"object","properties":{"main":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["greaterEx.A+B","greaterEx.B+C","exalt.A","exalt.B","exalt.C"]}}},"alternatives":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["greaterEx.A+B","greaterEx.B+C","exalt.A","exalt.B","exalt.C"]}}}},"required":["main","alternatives"],"additionalProperties":false}
```

### 스키마 afc927b649b7

```json
{"type":"object","properties":{"main":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["alchemy.A+B+C+D","alchemy.A+B+C+E","exalt.D","exalt.E"]}}},"alternatives":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["alchemy.A+B+C+D","alchemy.A+B+C+E","exalt.D","exalt.E"]}}}},"required":["main","alternatives"],"additionalProperties":false}
```

### 스키마 c667f0e492c6

```json
{"type":"object","properties":{"witness":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["alchemy.A+B+C+D","alchemy.A+B+C+E","exalt.D","exalt.E"]}}},"other":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["alchemy.A+B+C+D","alchemy.A+B+C+E","exalt.D","exalt.E"]}}}},"required":["witness","other"],"additionalProperties":false}
```

### 스키마 de0f259c50f1

```json
{"type":"object","properties":{"routes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["alchemy.A+B+C+D","alchemy.A+B+C+E","exalt.D","exalt.E"]}}}},"required":["routes"],"additionalProperties":false}
```

### 스키마 d16fe369fd8d

```json
{"type":"object","properties":{"routes":{"type":"array","items":{"type":"object","properties":{"opening":{"type":"string","enum":["1|alchemy.A+B+C+D","1|alchemy.A+B+C+E"]},"finishing":{"type":"string","enum":["2|exalt.D","2|exalt.E"]}},"required":["opening","finishing"],"additionalProperties":false}}},"required":["routes"],"additionalProperties":false}
```
