# 제작 레시피 후보의 요청·응답 JSON 초안

완성 카드 한 장을 기준으로 어떤 화폐를 어떤 순서로 쓸지 제안받는다. 요청은 목표 판정과 출처 배정에 필요한 정보를 담고, 기대 응답은 제작자의 선택과 화폐 사용 후 관측 결과를 분기로 표현한다. 아래 그래프는 사람이 작성한 비교 예시다. 실제 모델 응답과 백엔드의 경로별 확률·비용은 이후 실험에서 비교한다. [후보 생성 규칙안](recipe-candidate-rules.md)

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
  "goal": [
    {
      "op": "AND",
      "rows": [
        {"stat": "base_maximum_life", "min": 90, "max": 90},
        {"stat": "global_armour_evasion_energy_shield_+%", "min": 25, "max": 25}
      ]
    }
  ],
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
            "sources": {
              "base": {"weight": 1000},
              "essence": ["GREATER_ESSENCE_BODY"],
              "desecrated": ["Preserved_Collarbone", "Ancient_Collarbone", "Altered_Collarbone"]
            }
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
            "sources": {"essence": ["PERFECT_ESSENCE_ENHANCEMENT"]}
          }
        ]
      }
    ]
  }
}
```

- `goal`의 범위는 최종 수치에 적용한다. `roll`은 해당 티어 원래의 굴림 범위다. 방어가 24%로 붙어도 같은 티어에서 25%를 다시 굴릴 수 있다.
- `sources`는 해당 티어를 공급할 수 있는 모든 획득처다. 개별 노드의 아이템 상태에서 실제로 그 화폐를 쓸 수 있는지는 별도로 검사한다. `base.weight`는 일반 풀에서만 쓰는 원시 가중치다.
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
      "chooseOneSource": {"option": "life", "tier": 3, "kind": "desecrated"},
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
  "goal": [
    {
      "op": "AND",
      "rows": [
        {"stat": "base_maximum_life", "min": 90},
        {"stat": "global_armour_evasion_energy_shield_+%", "min": 25, "max": 25}
      ]
    }
  ],
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
            "sources": {
              "base": {"weight": 1000},
              "desecrated": ["Preserved_Collarbone", "Ancient_Collarbone", "Altered_Collarbone"]
            }
          },
          {
            "tier": 2,
            "mod": "amulet:prefix:virile",
            "roll": [100, 119],
            "sources": {
              "base": {"weight": 1000},
              "desecrated": ["Preserved_Collarbone", "Ancient_Collarbone", "Altered_Collarbone"]
            }
          },
          {
            "tier": 3,
            "mod": "amulet:prefix:rotund",
            "roll": [85, 99],
            "sources": {
              "base": {"weight": 1000},
              "essence": ["GREATER_ESSENCE_BODY"],
              "desecrated": ["Preserved_Collarbone", "Ancient_Collarbone", "Altered_Collarbone"]
            }
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
            "sources": {"essence": ["PERFECT_ESSENCE_ENHANCEMENT"]}
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
    "chooseOneSource": {"option": "life", "kind": "desecrated"},
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

이 예시는 방어가 T1 하나여서 생명력 각 티어와의 조합을 전부 표현한다. 두 옵션의 티어가 서로 종속되거나 기폭제 종류에 따라 함께 허용되는 조합이 달라질 때에는 카드에서 확인한 조합도 전달해야 한다. 그 경우의 전송 표기는 추가 검토 대상이다.
