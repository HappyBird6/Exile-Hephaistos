# 제작 레시피 후보의 요청·응답 JSON 초안

완성 카드 한 장을 기준으로 어떤 화폐를 어떤 순서로 쓸지 제안받는다. 요청은 목표 판정과 출처 배정에 필요한 정보를 담고, 기대 응답은 제작자의 선택과 화폐 사용 후 관측 결과를 분기로 표현한다. 아래 그래프는 사람이 작성한 비교 예시다. 실제 모델 응답과 백엔드의 경로별 확률·비용은 이후 실험에서 비교한다. [후보 생성 규칙안](recipe-candidate-rules.md)

## 요청에 남길 조건 정보

입력 길이 때문에 목표 판정이나 획득처 구분에 필요한 값을 생략하지 않는다. 공통 문안은 후보 생성과 종료 시점을 설명하고, 카드 입력은 시작 상태, 접두·접미별 목표, 가능한 화폐·대상과 해당 제작 규칙을 전달한다. 모델이 행동 순서를 선택하면 백엔드가 각 단계와 최종 수치를 검증한다.

| 카드에서 구분해야 하는 것 | 입력으로 표현할 내용 |
|---|---|
| 같은 옵션의 여러 출처 | 옵션 참조별로 가능한 전략과 대상, 허용 결과 티어를 연결한다. 같은 화폐가 한 단계에서 제거와 부여를 함께 수행하면 두 대상을 함께 표시한다. |
| 원본 수치와 표시 수치 | 시작 원본값, 원본 범위, 표시 단위와 변환 계수, 목표 범위를 구분한다. pseudo는 직접 기여하는 옵션과 고정 옵션 기여도를 표시한다. |
| 동시에 만족해야 하는 행 | 활성 그룹·행, 연산 버전, 행별 범위, 가중치와 그룹 범위를 전달한다. 이미 붙은 옵션이 점수 또는 개수 상한을 바꾸는 경우에도 시작값을 포함한다. |
| 특정 행동의 자격이나 결과 | 분열 여부, 접두·접미 자리, family, 레벨 하한과 예외 등 해당 카드에 필요한 상태를 전달한다. 획득 전략과 이미 보유한 옵션의 수치를 다시 굴리는 행동을 구분한다. |

실제 비교에서는 보강 입력이 간략 입력에서 누락된 양성 카드 17장을 회수했다. 간략 입력에서만 회수된 카드도 5장이므로 이미 검증한 후보를 같은 시작 상태·목표·재고·규칙에서 보존한다. 어떤 필드를 카드별로 생략해도 되는지는 별도 검증 대상이다. [100회 비교와 실제 입력 예시](context-ab-100.md)

### 옵션 출처 라벨

각 티어의 `sources`는 가능한 획득처의 배열이다. 각 항목에 `label`을 쓰고, 재료가 있는 네 라벨에는 재료를 식별하는 `id`를 하나씩 쓴다. 백엔드는 이 ID로 재료를 조회하므로 별도 `kind`는 보내지 않는다.

| `label` | 획득 방법 | `id` 예시 |
|---|---|---|
| `base` | 일반 옵션 풀에서 추첨한다. 알려진 일반 풀 가중치는 `weight`에 담는다. | 생략 |
| `essence` | 하위·일반·상위 에센스로 Magic → Rare 승급 시 일반 명시 옵션을 얻는다. 제작 옵션 한도 소모는 0이다. | `GREATER_ESSENCE_BODY` |
| `crafted` | 완벽한 에센스 등 교체형 에센스, 합금, 감정액으로 제작 옵션을 얻는다. 세 재료군은 같은 제작 옵션 한도를 쓴다. | `PERFECT_ESSENCE_ENHANCEMENT`, `CELESTIAL_ALLOY`, `LIQUID_ENVY` |
| `desecrated` | 훼손 재료를 쓰고 공개된 후보 중에서 선택한다. | `Preserved_Collarbone` |
| `augment` | 해당 증강물을 장착해 열린 전용 풀에서 추첨한다. | `Kolrs_Hunt` |

에센스·합금·감정액의 `id`는 기존 Workbench 화폐 action이고, 훼손 재료와 증강물에는 등록된 재료 ID를 쓴다. 베이스 고정 옵션(`implicit`)은 획득처 목록에서 제외한다. 목표 수치에 기여하는 고정값은 시작 상태에 담는다. [화폐와 제작 규칙](../v0.5.5/game-rule/currency.md)

## 1. 생명력 정확히 90인 태양의 목걸이 요청

시작 아이템은 레벨 82 Rare Solar Amulet이며 명시 옵션은 생명력 +90과 힘 +6이다. 사용자는 생명력 90과 일반 방어도·회피·에너지 보호막 증가 25%를 동시에 요구한다. 여기서 선택한 완성 형태는 품질 0에 목표 밖 명시 옵션이 없는 경우다. 힘 +6이 남아 있다면 사용자 필터를 통과해도 이 완성 형태에는 도달하지 못한다.

```json
{
  "ruleset": "v0.5.5",
  "cardId": "solar-01",
  "start": {
    "base": "Metadata/Items/Amulets/FourAmulet9",
    "ilvl": 82,
    "rarity": "rare",
    "affixes": [{"mod": "amulet:prefix:rotund", "value": 90},{"mod": "amulet:suffix:of-the-brute", "value": 6}]
  },
  "goal": {"rarity": "rare", "groups": [
    {
      "op": "AND",
      "rows": [
        {"stat": "base_maximum_life", "min": 90, "max": 90},
        {"stat": "global_armour_evasion_energy_shield_+%", "min": 25, "max": 25}
      ]
    }
  ]},
  "card": {
    "quality": 0,
    "otherAffixes": {"prefix": 0, "suffix": 0},
    "options": [
      {
        "key": "life",
        "stat": "base_maximum_life",
        "side": "prefix",
        "tiers": [
          {
            "tier": 3,
            "mod": "amulet:prefix:rotund",
            "roll": [85, 99],
            "sources": [
              {"label": "base", "weight": 1000},
              {"label": "essence", "id": "GREATER_ESSENCE_BODY"},
              {"label": "desecrated", "id": "Preserved_Collarbone"},
              {"label": "desecrated", "id": "Ancient_Collarbone"},
              {"label": "desecrated", "id": "Altered_Collarbone"}
            ]
          }
        ]
      },
      {
        "key": "defences",
        "stat": "global_armour_evasion_energy_shield_+%",
        "side": "prefix",
        "tiers": [
          {
            "tier": 1,
            "mod": "amulet:prefix:essence-global-defences",
            "roll": [20, 30],
            "sources": [{"label": "crafted", "id": "PERFECT_ESSENCE_ENHANCEMENT"}]
          }
        ]
      }
    ]
  }
}
```

- `goal`의 범위는 최종 수치에 적용한다. `roll`은 해당 티어 원래의 굴림 범위다. 방어가 24%로 붙어도 같은 티어에서 25%를 다시 굴릴 수 있다.
- `sources`는 해당 티어를 공급할 수 있는 모든 획득처다. 개별 노드의 아이템 상태에서 실제로 그 화폐를 쓸 수 있는지는 별도로 검사한다.
- 요청은 버전에 맞는 관련 화폐의 효과와 자격 조건을 공통 지침으로 공유한다는 전제다. 예를 들어 Greater Essence of the Body는 Magic 아이템을 Rare로 올릴 때 쓰고, Perfect Essence of Enhancement는 Rare 아이템의 옵션을 교체한다.
- 두 예시 모두 생략된 시작 품질은 0이고 분열·축성·훼손은 없다. 베이스의 정신력 고정 옵션은 목표 행과 화폐 선택에 영향을 주지 않아 전송에서 생략했다. `life`와 `defences`는 이 요청 안에서만 사용하는 참조다. [베이스](../v0.5.5/game-data/catalog/amulets/solar-amulet/base.json) · [옵션](../v0.5.5/game-data/modifiers/amulets.json) · [재료별 효과](../v0.5.5/i18n/materials/en.json)

## 2. 정확히 90인 카드의 기대 응답

```json
{
  "cardId": "solar-01",
  "recipes": [
    {"id": "protect-life", "intent": "생명력 90을 보존하고 방어를 추가", "entry": "protected-essence"},
    {"id": "plain-essence", "intent": "징조 없이 방어를 추가하고 제거 결과에 따라 진행", "entry": "plain-essence"}
  ],
  "nodes": {
    "protected-essence": {
      "use": "PERFECT_ESSENCE_ENHANCEMENT",
      "omens": ["Omen_of_Dextral_Crystallisation"],
      "on": [
        {"when": "goal_and_card_match", "to": "done"},
        {"when": "both_target_mods_present", "to": "roll-values"},
        {"when": "otherwise", "to": "review"}
      ]
    },
    "plain-essence": {
      "use": "PERFECT_ESSENCE_ENHANCEMENT",
      "on": [
        {"when": "goal_and_card_match", "to": "done"},
        {"when": "removed:amulet:suffix:of-the-brute", "to": "roll-values"},
        {"when": "removed:amulet:prefix:rotund", "to": "clear-strength"},
        {"when": "otherwise", "to": "review"}
      ]
    },
    "roll-values": {
      "requires": ["card_shape_matches", "target_values_rerollable"],
      "use": "DIVINE",
      "on": [
        {"when": "goal_and_card_match", "to": "done"},
        {"when": "same_shape_and_target_values_rerollable", "to": "roll-values"},
        {"when": "otherwise", "to": "review"}
      ]
    },
    "clear-strength": {
      "requires": ["only_removable_suffix:amulet:suffix:of-the-brute"],
      "use": "ANNULMENT",
      "omens": ["Omen_of_Dextral_Annulment"],
      "on": [
        {"when": "only_explicit:defences", "to": "recover-life"},
        {"when": "otherwise", "to": "review"}
      ]
    },
    "recover-life": {"choose": ["roll-base-life", "prepare-desecration"]},
    "roll-base-life": {
      "use": "EXALTED",
      "target": "life",
      "on": [
        {"when": "goal_and_card_match", "to": "done"},
        {"when": "card_shape_matches", "to": "roll-values"},
        {"when": "otherwise", "to": "review"}
      ]
    },
    "prepare-desecration": {
      "chooseOneSource": {"option": "life", "tier": 3, "label": "desecrated"},
      "on": [
        {"when": "unrevealed_prefix_available", "to": "observe-life"},
        {"when": "otherwise", "to": "review"}
      ]
    },
    "observe-life": {
      "observe": "desecrated_offers",
      "ifOfferedSelect": "amulet:prefix:rotund",
      "on": [
        {"when": "goal_and_card_match", "to": "done"},
        {"when": "card_shape_matches_and_target_values_rerollable", "to": "roll-values"},
        {"when": "otherwise", "to": "review"}
      ]
    }
  },
  "inapplicableAt": [
    {
      "node": "recover-life",
      "source": "GREATER_ESSENCE_BODY",
      "reason": "requires_magic_item_but_current_item_is_rare"
    }
  ]
}
```

`entry`와 `choose`는 제작자가 선택하고, `on`은 행동 직후 실제 상태에서 위부터 처음 맞는 조건을 따른다. `requires`가 맞지 않거나 동작을 적용할 수 없으면 그 상태를 `review`로 돌린다. `card_shape_matches`는 허용 티어와 품질, 목표 밖 옵션 개수를 포함한 카드 형태를 검사한다. `done`은 원래 필터와 카드가 함께 맞을 때의 종료점이다. `review`는 중간 상태를 보존하고 후속 경로를 재검토하는 종료점이다.

접미 제거 징조를 쓰면 힘만 사라진다. 에센스 단독 사용으로 생명력이 빠진 경우에는 남은 힘을 정리한 뒤 생명력을 복구하는 후보를 제시했다. Divine Orb 사용 후에는 방어와 생명력이 모두 바뀔 수 있어 둘 다 검사한다. Collarbone은 재료별 대안이며, 실제로 목표가 제시된 뒤 선택한다. 공개 후보의 분포는 미정이다.

## 3. 여러 티어가 같은 조건을 만족하는 실제 카드

같은 시작 아이템과 방어 목표를 유지하고 생명력 조건에서 상한만 없앴다. 아래는 품질 0, 목표 밖 옵션 0개인 완성 형태의 두 번째 **전체 요청**이다. 첫 번째 예시와 독립적으로 읽을 수 있다.

```json
{
  "ruleset": "v0.5.5",
  "cardId": "solar-02",
  "start": {
    "base": "Metadata/Items/Amulets/FourAmulet9",
    "ilvl": 82,
    "rarity": "rare",
    "affixes": [{"mod": "amulet:prefix:rotund", "value": 90},{"mod": "amulet:suffix:of-the-brute", "value": 6}]
  },
  "goal": {"rarity": "rare", "groups": [
    {
      "op": "AND",
      "rows": [
        {"stat": "base_maximum_life", "min": 90},
        {"stat": "global_armour_evasion_energy_shield_+%", "min": 25, "max": 25}
      ]
    }
  ]},
  "card": {
    "quality": 0,
    "otherAffixes": {"prefix": 0, "suffix": 0},
    "options": [
      {
        "key": "life",
        "stat": "base_maximum_life",
        "side": "prefix",
        "tiers": [
          {
            "tier": 1,
            "mod": "amulet:prefix:athlete-s",
            "roll": [120, 149],
            "sources": [
              {"label": "base", "weight": 1000},
              {"label": "desecrated", "id": "Preserved_Collarbone"},
              {"label": "desecrated", "id": "Ancient_Collarbone"},
              {"label": "desecrated", "id": "Altered_Collarbone"}
            ]
          },
          {
            "tier": 2,
            "mod": "amulet:prefix:virile",
            "roll": [100, 119],
            "sources": [
              {"label": "base", "weight": 1000},
              {"label": "desecrated", "id": "Preserved_Collarbone"},
              {"label": "desecrated", "id": "Ancient_Collarbone"},
              {"label": "desecrated", "id": "Altered_Collarbone"}
            ]
          },
          {
            "tier": 3,
            "mod": "amulet:prefix:rotund",
            "roll": [85, 99],
            "sources": [
              {"label": "base", "weight": 1000},
              {"label": "essence", "id": "GREATER_ESSENCE_BODY"},
              {"label": "desecrated", "id": "Preserved_Collarbone"},
              {"label": "desecrated", "id": "Ancient_Collarbone"},
              {"label": "desecrated", "id": "Altered_Collarbone"}
            ]
          }
        ]
      },
      {
        "key": "defences",
        "stat": "global_armour_evasion_energy_shield_+%",
        "side": "prefix",
        "tiers": [
          {
            "tier": 1,
            "mod": "amulet:prefix:essence-global-defences",
            "roll": [20, 30],
            "sources": [{"label": "crafted", "id": "PERFECT_ESSENCE_ENHANCEMENT"}]
          }
        ]
      }
    ]
  }
}
```

| 생명력 티어 | 원래 굴림 | 목표를 만족하는 값 | 일반 풀 | Greater Essence of the Body | 훼손 재료 |
|---|---:|---:|---|---|---|
| T1 Athlete’s | 120~149 | 120~149 | weight 1000 | 해당 없음 | Preserved, Ancient, Altered Collarbone |
| T2 Virile | 100~119 | 100~119 | weight 1000 | 해당 없음 | 같은 세 종류 |
| T3 Rotund | 85~99 | 90~99 | weight 1000 | 획득 가능 | 같은 세 종류 |

옵션 단위로 획득처를 합치면 에센스가 T1·T2도 공급하는 것으로 읽히게 된다. 현재 요청 초안에서는 티어마다 출처를 보관하므로 세 티어를 한 카드에 담으면서 이 차이를 보존한다. T3에서 85~89가 나오면 올바른 티어를 획득했지만 생명력 목표는 아직 미달이다.

### 기대 응답에서 달라지는 부분

두 번째 응답의 `cardId`는 `solar-02`다. 첫 번째 그래프에서 아래 두 노드를 교체하면 된다. 단일 티어 예시의 `tier: 3`과 Rotund 고정 선택을 그대로 복사하면 실제 미공개 슬롯에 T1이나 T2가 제시돼도 놓친다.

```json
{
  "prepare-desecration": {
    "chooseOneSource": {"option": "life", "label": "desecrated"},
    "on": [
      {"when": "unrevealed_prefix_available", "to": "observe-life"},
      {"when": "otherwise", "to": "review"}
    ]
  },
  "observe-life": {
    "observe": "desecrated_offers",
    "ifOfferedSelect": {"option": "life", "fromChosenSource": true},
    "on": [
      {"when": "goal_and_card_match", "to": "done"},
      {"when": "card_shape_matches_and_target_values_rerollable", "to": "roll-values"},
      {"when": "otherwise", "to": "review"}
    ]
  }
}
```

`chooseOneSource`에서 `tier`를 생략하면 이 카드의 허용 티어 전체를 대상으로 해당 종류의 재료를 찾는다. 한 번에 하나의 재료를 선택하며, 그 재료가 실제로 공급하는 티어와의 대응을 유지한다. `fromChosenSource`는 그 대응과 실제로 제시된 옵션의 교집합에서 선택한다. 적격 후보가 여럿이면 각각을 선택할 수 있다. 아무것도 없으면 `review`로 간다. 일반 풀 추첨의 `target: "life"`도 동일한 카드의 세 티어를 가리킨다. 시작 시 제거 이벤트의 Rotund ID는 실제로 붙어 있던 옵션이므로 그대로 사용한다.

### 확인 결과와 적용 범위

카드 탐색 작업 브랜치의 빌드에서 `GoalFilterService.preview`에 두 목표와 동일한 시작 아이템을 각각 전달했다. 품질이 없고 생명력과 전용 방어 접두가 있는 카드 한 장씩을 확인하고, 두 카드 모두 잡옵 접두·접미 0개를 허용함을 확인했다. `optionTiers`의 각 명시 옵션 ID로 `sources`와 카탈로그 정의를 조회해 위 두 요청의 티어·범위·가중치·화폐 ID 집합을 대조했다. 첫 카드의 티어 조합 1개와 두 번째 카드의 티어 조합 3개는 원래 목표 평가에서 모두 `MATCH`였다. 두 번째 카드의 목표 명시 옵션에 연결된 획득처는 생명력 T1·T2·T3에 각각 4·4·5개, 방어 T1에 1개다. [카드 조회 코드](https://github.com/HappyBird6/Exile-Hephaistos/blob/219bbc27/backend/src/main/java/com/poe2craft/craftingsupport/goal/application/GoalFilterService.java) · [여러 생명력 티어의 기존 회귀 사례](https://github.com/HappyBird6/Exile-Hephaistos/blob/219bbc27/backend/src/test/java/com/poe2craft/bootstrap/NonResistanceGoalPreviewTest.java)

이 예시는 방어가 T1 하나여서 생명력 각 티어와의 조합을 전부 표현한다. 아래에서는 두 옵션의 티어가 서로 종속되는 카드를 검증한다.

## 4. 기폭제와 Weighted Sum으로 두 옵션의 티어가 결합되는 카드

레벨 82 태양의 목걸이에서 품질 종류를 지정하지 않고 품질 20 이상을 요구했다. 능력치 필터는 생명력 115~124, 힘 7~14이고 두 행의 가중치가 각각 1인 Weighted Sum V1의 합은 127~129다. 시작점은 명시 옵션과 품질이 없는 Rare다. 미리보기에서 **Flesh 품질 20, 일반 생명력 접두와 힘 접미, 목표 밖 옵션 0개**인 형태를 골랐다. 사용자의 품질 목표는 요청의 `goal`에, 이 카드의 품질 종류와 수치는 `card`에 남긴다.

### 검증에서 발견해 수정한 부분

| 검토한 표현 | 재현된 문제 | 초안에 반영한 수정 |
|---|---|---|
| 옵션별로 가능한 티어를 독립 나열 | 생명력 T2·힘 T6가 들어간다. 최소 굴림도 Flesh 20에서 120 + 13 = 133이다. | `joint.order`와 `joint.tiers`에 한 상태에서 성립하는 티어 조합을 담는다. |
| 미리보기의 `tierVariants`를 그대로 허용 목록으로 사용 | 대표 상태 4개에는 생명력 T3·힘 T8이 없다. 원래 굴림 99·8이면 표시 수치 119·8, 합 127로 통과한다. | 전체 조합을 검증한 경우만 `coverage: "complete"`로 표시한다. 일부 대표 상태만 있으면 `partial`로 표시하고 목록 밖은 검증 대상으로 남긴다. |
| 양쪽 티어가 맞으면 성공 처리 | T3·T8에서 99·8은 통과하지만 98·8은 118 + 8 = 126으로 탈락한다. | 행은 **도달 가능한 수치가 하나 이상 있음**을 뜻한다. 종료 조건은 실제 표시 수치로 원래 그룹과 카드 형태를 다시 검사한다. |
| 같은 아이템에 서로 다른 기폭제의 효과를 합침 | 원래 굴림 98·8에서 Flesh의 생명력 118과 Adaptive의 힘 10을 섞으면 합 128로 잘못 통과한다. 실제 두 상태의 합은 각각 126과 108이다. | 한 조합에 기폭제 종류·양·해당 수치 투영을 함께 고정한다. 다른 품질의 카드는 별도로 평가한다. |
| 조합만 보고 단계별 출처와 상태를 생략 | Ancient Collarbone을 낮은 힘 티어에 쓰거나, 두 옵션을 각각 훼손으로 얻거나, Rare에서 등급 상승용 에센스를 쓰는 경로가 생긴다. | 티어별 출처·현재 자격·관측한 후보를 교차 확인한다. 기존 훼손 슬롯을 보존하면서 두 번째 슬롯을 요구하는 분기는 중단한다. |

### 대조 결과

카드 탐색 빌드의 `GoalFilterService.preview`로 실제 카드·티어·출처·허용 점유 수를 조회했다. 별도로 해당 레벨에서 적격인 일반 생명력 9티어 × 일반 힘 8티어의 모든 정수 굴림을 조합했다. 기폭제마다 4,060개를 실제 목표 평가기에 넣고, 원래 굴림에서 독립 산출한 표시 수치·행 범위·가중합 판정과 대조했다. 두 기폭제 8,120개 상태에서 불일치는 0건이었다.

| 고정한 품질 | 생명력/힘 티어와 목표를 통과한 원래 굴림 쌍 개수 | 합계 |
|---|---|---:|
| Flesh 20 · 전송 카드 | T2/T7 1개, T2/T8 5개, T3/T6 3개, T3/T7 8개, T3/T8 1개 | 18 |
| Adaptive 20 · 대조 | T1/T8 5개, T2/T7 9개, T2/T8 4개 | 18 |

Flesh 카드에서 5개 티어 조합과 원래 목표 판정을 함께 적용하면 18개 성공 상태가 모두 보존된다. `joint`의 행만 검사하면 수치 미달이 섞인다. 같은 18개라는 개수만 비교해도 기폭제가 바뀌어 달라지는 실제 상태를 구별하지 못한다. 추가로 작업대에서 Flesh를 적용하고 일반 엑잘의 적격 결과를 T3 생명력 → T8 힘, 역순으로 각각 지정한 뒤 일반 Divine Orb로 재굴림해 99·8의 성공 상태에 도달했다. 지정 추첨은 그 결과의 도달 가능성을 확인하기 위한 검사다. 기대 응답은 무작위 엑잘로 표현한다. [미리보기의 대표 상태 수집](https://github.com/HappyBird6/Exile-Hephaistos/blob/219bbc27/backend/src/main/java/com/poe2craft/craftingsupport/goal/application/GoalExamples.java) · [기폭제별 복합 목표 회귀 사례](https://github.com/HappyBird6/Exile-Hephaistos/blob/219bbc27/backend/src/test/java/com/poe2craft/bootstrap/NonResistanceGoalPreviewTest.java)

### 최종 전송 프롬프트 예시

다음 지시문과 요청 JSON을 한 호출에 함께 보낸다. 범위의 양 끝은 포함한다. 동일한 출처 집합은 한 번 정의하고 앞에서 정의한 집합만 확장한다. `joint`의 값은 해당 옵션의 티어 번호이며 옵션별로 하나의 modifier에 대응한다. 같은 옵션·번호에 서로 다른 정의가 있는 카드는 행에서 modifier ID를 참조해야 한다.

다음 시작 아이템에서 지정한 완성 카드에 도달할 제작 레시피 후보를 JSON으로 제안하라. 유효한 순서를 폭넓게 남기고 확률과 비용은 백엔드에 맡긴다.

sourceSet에 include가 있으면 참조 집합을 먼저 펼친 뒤 sources 배열의 항목을 더하라. label은 획득 방법이고 id는 사용할 재료를 식별한다. 획득처와 지금 쓸 수 있는 화폐를 구분하라. joint의 각 행은 order 순서로 읽는 최종 티어 조합이다. complete이면 이 카드에서 가능한 행을 모두 담았다. partial이면 목록 밖은 미검증으로 보낸다. 부분 상태는 보존할 옵션과 양립하는 모든 행으로 이어라. 행에 들어 있어도 실제 수치를 다시 판정하라.

목표는 같은 아이템의 표시 수치로 계산한다. 이 카드에서는 Flesh Catalyst로 품질 20이 되면 생명력만 1.2배로 반올림하고 힘은 원래 값이다. 두 행 범위와 가중합 범위를 모두 확인하라. 다른 기폭제의 수치를 섞지 마라.

이 Rare 목걸이에서 Exalted Orb는 일반 옵션 하나를 무작위로 추가한다. 첫 변경 가능 옵션 하나를 노리는 단계에서는 기본 Chaos Orb로 다시 뽑을 수 있다. Collarbone은 미공개 훼손 슬롯 하나를 만들며 실제 공개 목록에서 선택한다. 훼손 슬롯은 하나만 허용한다. 두 에센스는 Magic에서 Rare로 올릴 때 쓴다. 일반 Divine Orb는 변경 가능한 수치를 티어 안에서 다시 굴린다.

응답에는 cardId, 전략별 시작 노드, 화폐·관측·선택과 결과별 다음 노드를 넣어라. choose는 제작자의 선택, on은 관측 결과의 우선순위다. 각 행동은 실제 적용 가능할 때만 쓴다. 재시도는 같은 행동의 자격이 남을 때만 허용한다. 처음부터, 그리고 각 행동 후에 원래 목표와 카드가 함께 맞으면 done이다. 미관측·미지원·복구 미정은 현재 상태를 보존해 review로 보낸다.

```json
{
  "ruleset":"v0.5.5","cardId":"solar-weighted-flesh20",
  "start":{"base":"Metadata/Items/Amulets/FourAmulet9","ilvl":82,"rarity":"rare","affixes":[],"quality":0},
  "goal":{"rarity":"rare","minQuality":20,"groups":[
    {"op":"WEIGHTED_V1","min":127,"max":129,"rows":[
      {"stat":"base_maximum_life","min":115,"max":124,"weight":1},
      {"stat":"additional_strength","min":7,"max":14,"weight":1}
    ]}
  ]},
  "sourceSets":{
    "L2":{"sources":[
      {"label":"base","weight":1000},
      {"label":"desecrated","id":"Preserved_Collarbone"},
      {"label":"desecrated","id":"Ancient_Collarbone"},
      {"label":"desecrated","id":"Altered_Collarbone"}
    ]},
    "L3":{"include":"L2","sources":[{"label":"essence","id":"GREATER_ESSENCE_BODY"}]},
    "S68":{"sources":[
      {"label":"base","weight":1000},
      {"label":"desecrated","id":"Preserved_Collarbone"},
      {"label":"desecrated","id":"Altered_Collarbone"}
    ]},
    "S7":{"include":"S68","sources":[{"label":"essence","id":"LESSER_ESSENCE_INFINITE"}]}
  },
  "card":{
    "quality":{"type":"FLESH","amount":20},"otherAffixes":{"prefix":0,"suffix":0},
    "options":[
      {"key":"life","stat":"base_maximum_life","side":"prefix","tiers":[
        {"tier":2,"mod":"amulet:prefix:virile","roll":[100,119],"sourceSet":"L2"},
        {"tier":3,"mod":"amulet:prefix:rotund","roll":[85,99],"sourceSet":"L3"}
      ]},
      {"key":"str","stat":"additional_strength","side":"suffix","tiers":[
        {"tier":6,"mod":"amulet:suffix:of-the-bear","roll":[13,16],"sourceSet":"S68"},
        {"tier":7,"mod":"amulet:suffix:of-the-wrestler","roll":[9,12],"sourceSet":"S7"},
        {"tier":8,"mod":"amulet:suffix:of-the-brute","roll":[5,8],"sourceSet":"S68"}
      ]}
    ],
    "joint":{"order":["life","str"],"coverage":"complete","tiers":[[2,7],[2,8],[3,6],[3,7],[3,8]]}
  }
}
```

### 기대 응답 JSON

```json
{
  "cardId":"solar-weighted-flesh20",
  "recipes":[
    {"id":"base-first","entry":"exalt"},
    {"id":"desecrated-first","entry":"bone"}
  ],
  "nodes":{
    "exalt":{"use":"EXALTED","next":"inspect"},
    "inspect":{"on":[
      {"when":"allowed_pair_and_shape","to":"quality"},
      {"when":"one_compatible_target_and_no_junk","to":"second"},
      {"when":"opening_one_mutable_base_affix","to":"chaos"},
      {"when":"otherwise","to":"review"}
    ]},
    "chaos":{"requires":["opening_one_mutable_base_affix"],"use":"CHAOS","next":"inspect"},
    "second":{"choose":["exalt","bone"]},
    "bone":{"requires":["no_desecrated_slot"],"chooseOneSource":{"label":"desecrated","for":"missing_options_in_compatible_rows"},
      "on":[{"when":"eligible_unrevealed_slot","to":"reveal"},{"when":"otherwise","to":"review"}]},
    "reveal":{"observe":"desecrated_offers","chooseEach":"offered_mod_from_used_source_in_compatible_rows",
      "on":[{"when":"selected","to":"inspect"},{"when":"otherwise","to":"review"}]},
    "quality":{"on":[{"when":"already_flesh20","to":"values"},{"when":"otherwise","to":"flesh"}]},
    "flesh":{"use":"CATALYST_FLESH","next":"values"},
    "values":{"on":[{"when":"goal_and_card_match","to":"done"},{"when":"same_pair_can_reach_goal_by_divine","to":"divine"},{"when":"otherwise","to":"review"}]},
    "divine":{"use":"DIVINE","next":"values"}
  },
  "inapplicableAt":[{"state":"current_rare","sources":["GREATER_ESSENCE_BODY","LESSER_ESSENCE_INFINITE"],"reason":"requires_magic"}]
}
```

`allowed_pair_and_shape`는 허용 티어 쌍과 잡옵 개수를 검사한다. 이 중간 단계에서 품질과 굴림은 이후에 바뀔 수 있다. `one_compatible_target_and_no_junk`는 현재 보존할 단일 옵션의 티어와 양립하는 행을 **전부** 유지한다. `second`는 실제 적용 가능한 행동만 펼친다. 화폐 적용 조건을 충족하지 못한 단계, 남는 선택이 없는 노드, 검증할 수 없는 공개 수치에서는 관측된 상태를 `review`로 보낸다. `same_pair_can_reach_goal_by_divine`는 해당 **실제 상태**에서 재굴림 가능한 옵션만으로 성공 수치에 도달 가능한지 백엔드가 확인한다. 분열·훼손 등으로 고정된 수치까지 자유롭게 바꿀 수 있다고 간주하지 않는다. 그래프는 현행 수동 작업대에서 검토할 후보이며, 자동 제작에 적용할 때는 그 정책의 엑잘·카오스 등 허용 조건도 확인한다.

마지막으로 Markdown에 저장한 JSON을 다시 파싱해 요청만으로 복원한 원래 굴림·modifier ID 쌍의 집합과 평가기가 통과시킨 집합을 비교했다. Flesh 카드에서 누락 0개, 과다 포함 0개였다. 출처 집합 참조를 펼친 5개 티어의 재료 목록도 카드 응답과 일치했고, 기대 그래프의 10개 노드가 모두 시작점에서 연결됐다.

이 검증의 완전성은 선택한 베이스·레벨·두 옵션 계열·고정 품질에서 **최종 티어와 수치 조합**에 해당한다. 제작 순서 전체의 도달 가능성과 미관측 훼손 분포는 별도 검증 대상이다. 제품 코드와 실제 LLM 호출은 후속 단계다.
