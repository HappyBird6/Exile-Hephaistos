# 문제별 열 번 이내의 제작 경로 실험

## 사전 등록

직전 50회에서 유효 후보가 남은 양성 카드는 11/11, 고유 경로는 38/49였다. 가장 큰 누락부터 검사한다. 전체 실제 요청 상한은 300, 아래 **문제별 상한은 10**이다. 대조·검증·재시도·입력 순서 반전도 같은 문제에 합산한다. 한도에 닿으면 해당 문제는 종료하고 나머지 문제로 넘어간다. 사용량을 채우기 위한 추가 호출은 하지 않는다.

| 문제 | 대상과 사전 기대 |
|---|---|
| 일반·훼손 출처와 세 목표의 순서 | T3 반지 18경로. 생명력과 냉기 출처 조합 3가지 × 세 목표 순서 6가지. 같은 문제의 두 목표 반지 R은 6개, 이미 훼손된 Q3는 1개. |
| 획득 후 실패 복구 | N 목걸이 7경로. 방어 선취득 후 생명력 엑잘·절사까지 포함한다. |
| Crafted 한도와 일반 옵션 중간 배치 | W 6, H2·Q2·T2 각 2, T1은 1. H3·Q1은 도달 불가능. |
| 마법 장갑의 등급 전환 | G는 승급 에센스 또는 제왕으로 생명력을 확보한 뒤 합금을 쓰는 2경로. |
| 품질·수치 동시 달성 | H4는 기폭제와 Divine 양쪽 순서 2경로. |
| 무행동·종료 처리 | Q4는 빈 실행열 1개, H1은 이미 붙은 생명력을 보존하고 방어 에센스 1회. |

각 고유 경로를 사전 기대와 대조한다. 새 요청으로 얻은 경로만 이번 회수에 포함한다. 실험 중의 전체 합집합과 같은 방식을 다시 보냈을 때의 결과를 구별한다. 확정할 수 없는 빈 응답은 ‘후보 미발견’이다. 이 실험은 고정 카드의 상태 전이 검증이며 확률·비용은 산출하지 않는다.

31회차 전 전이 대조 입력을 추가했다. 각각 원래 문제의 예산에 포함한다. GR은 Rare 장갑에 생명력이 있어 합금만 1회, HF는 99·8로 품질만 1회, HD는 Flesh20이 이미 적용돼 Divine만 1회다. L90·L89는 이전 설계 실험 S6·S10의 축성 반지로 현재 생명력 90이면 빈 실행열 하나, 89이면 후보가 없어야 한다. 두 축성 카드에 같은 Divine 전략 사전을 제공한다.

50회차 전 별도 문제로 **동일 옵션의 티어별 획득처 손실**을 등록했다. [기존 실제 카드의 T1–T3 원본 표](card-recipe-json-draft.md)를 사용한다. 실험은 장비 레벨 82 태양의 목걸이의 빈 생명력 접두를 한 전략으로 얻는 범위다. Magic에서 생명력 90 이상은 제왕의 T1·T2·T3 또는 Greater Essence of the Body의 T3로 4개, 100 이상은 제왕의 T1·T2로 2개다. Rare의 90 이상은 엑잘 또는 세 Collarbone의 드러내기 각각 T1·T2·T3로 12개다. 성공 단위는 여기에서만 (화폐 전략, 허용 결과 티어) 쌍이며, T3가 선택되어도 실제 굴림은 목표값 이상이어야 한다. 앞의 두 목표 획득 순서나 Crafted 실패를 이 예산으로 재실험하지 않는다.

## 결과

총 **59/300회**, 문제별 최다 **10회**를 사용했다. 7개 문제가 모두 검사되었고, 한도에 도달한 문제는 이후 다른 이름으로 다시 호출하지 않았다. 수치의 단위는 모델이 직접 선택한 전략·대상의 적법한 획득 순서다. 비용이 높거나 검증에서 탈락한 후보의 수는 회수율을 낮추지 않는다.

| 문제 | 호출 | 확인한 개선과 보류 사항 |
|---|---:|---|
| 복수 목표·공유 훼손 슬롯 | 1–10 (10회) | 세 옵션 반지 T3 기준선 8/18. 훼손 미사용·생명력·냉기 세 칸으로 나누고 각 목표에 실행 번호와 화폐를 직접 쓰게 하자 4번에서 18/18. 순서만 뒤집은 6번은 9/18이었다. 접두·접미 및 행 ID 기준으로 입력을 정규화한 9·10번에서 각각 18/18. R 6/6, Q3 1/1. |
| 방어 선취득 후 엑잘 실패 복구 | 11–20 (10회) | 이번 기준선도 7/7로 직전의 누락은 매번 재현되지 않았다. 행 번호를 직접 쓰게 한 12번은 번호 충돌로 0/7이었다. 복구 선택별로 배열을 받고 다른 첫 획득 칸을 둔 19·20번은 각각 7/7. 같은 문안의 기본 입력 순서인 18번은 4/7. **입력 순서에 무관한 기본안 채택은 보류**한다. |
| 기존 Crafted 점유와 한도 확장 | 21–30 (10회) | W 6/6. prepare와 획득 단계에 확장을 중복 입력하는 오류를 줄이기 위해 두 표현을 스키마로 분리했다. H2는 0/2에서 1/2로 개선했으나 시전 속도 선취득 경로가 빠졌다. Q2는 두 목표 대신 같은 목표를 반복해 0/2. T1 1/1, T2 2/2. **이 수정안은 보류**한다. |
| Magic → Rare 승급 | 31–34 (4회) | 기존 문안과 변경안 모두 G 2/2. 입력 순서를 뒤집어도 두 경로를 유지했다. 이미 Rare이며 생명력이 붙은 GR은 관통 합금만 사용했다(1/1). 조기 종료. |
| 품질과 원래 수치 재굴림 | 35–39 (5회) | H4 2/2로 기폭제·Divine의 양쪽 순서를 유지했다. 품질만 남은 HF, 재굴림만 남은 HD도 각각 1/1. 조기 종료. |
| 현재 상태 충족·미발견 구분 | 40–49 (10회) | 빈 행동 사전에서 재료명을 만들어 내던 Q4는 상태와 실행열을 함께 받는 45·49번에서 모두 done과 빈 실행열을 냈다. 미달인 축성 반지 L89는 42·46번 모두 무행동 성공으로 잘못 보고했다. **모델의 종료 판정 채택은 보류**한다. |
| 같은 옵션의 티어별 화폐 | 50–59 (10회) | 원본 티어 행 기반 50–52번에서 Magic 생명력 ≥90은 4/4, ≥100은 2/2, Rare ≥90은 12/12. ≥100에서는 허용되지 않는 쌍도 출력했다. Rare ≥90은 재검사에서도 12/12. 압축한 56·59번에서 Magic ≥90의 제왕 → T3가 반복 누락됐다(3/4). **압축 입력 교체는 보류**한다. |

### 이전 49개 경로와의 비교

동일한 양성 11장에 대해 이번 실험의 **새 응답만** 카드별로 합쳤다. 46/49개(93.9%)를 회수했으며 카드 단위로는 10/11장에 유효 후보가 남았다. Q2는 새 응답에서 제작 가능한 두 경로가 모두 빠졌다. 직전은 38/49이면서 11/11장에 유효 후보가 있었다. 이번에는 문제당 호출 수가 다르고 13장에 동일한 최종 문안을 적용한 것이 아니므로, **단일 프롬프트의 성능 향상치로 해석할 수 없다**.

| 동일 카드 | 직전 → 이번 고유 경로 | 판단 |
|---|---:|---|
| T3 세 옵션 반지 | 8 → 18 | 종전 누락 10개 전부 회수 |
| N 분열 목걸이 | 6 → 7 | 종전 누락 1개 회수 |
| H2 기존 Crafted 마법봉 | 2 → 1 | 시전 속도부터 얻는 순서 누락 |
| Q2 두 Crafted 마법봉 | 2 → 0 | 제작 가능한 카드 전체 누락 |
| W·R·G·H4·Q4·T1·T2 | 합계 20 → 20 | 각각 6·6·2·2·1·1·2개로 동일 |

동일 입력·같은 검증 규칙에서 직전과 이번의 검증 통과 실행열을 오프라인에서 합치면 49/49다. 호출당 성능이나 새로운 카드에서의 보장은 아니다. 실제 서비스에서도 동일한 시작 아이템·목표·규칙 버전으로 재검증한 이전 후보를 보존하는 것이 Q2 같은 회귀를 막는 데 중요하다. 모델이 빈 목록 또는 실행 불가 경로만 보내면 화면에서 단정할 수 있는 것은 ‘유효 경로 미발견’까지다.

### 입력·출력에서 유지할 부분

세 옵션 반지에는 훼손 슬롯을 어느 옵션에 사용하는지만 범주로 건넸다. 구체적인 화폐·대상·순서를 모델이 채운다. 예를 들어 한 행의 `2|base_add.lightning`은 두 번째 획득에 일반 엑잘로 번개 저항을 노리겠다는 명시적 선택이다. 행 세 개의 번호에 중복이나 누락이 있으면 후보를 버린다. 백엔드가 빈 번호를 채우거나 다른 응답의 단계를 이어 붙이지 않는다. 입력 정규화 뒤의 두 호출은 같은 전송 내용의 반복이므로, 다른 카드 전반의 순서 독립성까지 입증하지 않는다.

목걸이에서는 `base_add.life>whittle_miss.life`가 엑잘 실패 직후 같은 대상에 절사 복구를 쓰는 선택이다. 19·20번은 방어를 얻는 행동을 이 두 행동 앞에 명시한 완성 경로를 냈다. 이 보완 응답을 기존 유효 후보에 더할 수 있지만 입력 정렬에 따라 누락이 남는다. 희귀도 승급과 품질 조정의 소규모 대조에서는 복잡한 형식을 추가할 이득을 확인하지 못했다.

### 검증과 적용 범위

모델은 전 회차 `gpt-4.1-mini-2025-04-14`, temperature 0, strict JSON Schema, 저장 비활성, 최대 출력 3,200토큰이었다. 59회 모두 정상 종료했고 총 입력 44,204·출력 6,376토큰을 사용했다. 호출 전에 원본 카드와 기대를 고정했다. 3번에만 이전 응답에서 실제 관측된 거절 사유·중복/미달 행 및 통과 후보를 전달했다. 정답 목록과 전체 순열을 모델에 보내지 않았다.

모든 원응답의 JSON 구조와 실행열을 다시 읽고 상태 전이 열거 결과와 대조했다. 각 전송 형식에 기준 실행열을 그대로 담을 수 있는지도 확인했다. 축성 대조에는 현재 값·변경 금지 조건을 별도로 적용했고, 티어 실험은 원본 표 및 사전 기대와 대조했다. 티어 실험의 12쌍은 12개 서로 다른 화폐 사용 순서를 의미하지 않는다. 4개 전략 각각에 허용되는 결과 티어 3개를 센 것이다. 한 전략에서 T3가 나와도 실제 수치가 90 미만이면 해당 결과는 목표 미달이다. 검증 범위는 실험 카드의 전략 수준이며 Java Workbench 실행과 단계별 확률 계산은 포함되지 않는다.

## 카드별 기대 결과

### T3

```json
{"expected":["base_add.cold base_add.life base_add.lightning","base_add.cold base_add.lightning base_add.life","base_add.cold base_add.lightning reveal.life","base_add.cold reveal.life base_add.lightning","base_add.life base_add.cold base_add.lightning","base_add.life base_add.lightning base_add.cold","base_add.life base_add.lightning reveal.cold","base_add.life reveal.cold base_add.lightning","base_add.lightning base_add.cold base_add.life","base_add.lightning base_add.cold reveal.life","base_add.lightning base_add.life base_add.cold","base_add.lightning base_add.life reveal.cold","base_add.lightning reveal.cold base_add.life","base_add.lightning reveal.life base_add.cold","reveal.cold base_add.life base_add.lightning","reveal.cold base_add.lightning base_add.life","reveal.life base_add.cold base_add.lightning","reveal.life base_add.lightning base_add.cold"],"state":"reachable"}
```

### R

```json
{"expected":["base_add.cold base_add.life","base_add.cold reveal.life","base_add.life base_add.cold","base_add.life reveal.cold","reveal.cold base_add.life","reveal.life base_add.cold"],"state":"reachable"}
```

### Q3

```json
{"expected":["base_add.cold"],"state":"reachable"}
```

### N

```json
{"expected":["base_add.life safe_ess.def","base_add.life whittle_miss.life safe_ess.def","first_chaos.life safe_ess.def","reveal.life safe_ess.def","safe_ess.def base_add.life","safe_ess.def base_add.life whittle_miss.life","safe_ess.def reveal.life"],"state":"reachable"}
```

### W

```json
{"expected":["base_add.speed safe_alloy.mana_skill safe_ess.skill","base_add.speed safe_ess.skill safe_alloy.mana_skill","safe_alloy.mana_skill base_add.speed safe_ess.skill","safe_alloy.mana_skill safe_ess.skill base_add.speed","safe_ess.skill base_add.speed safe_alloy.mana_skill","safe_ess.skill safe_alloy.mana_skill base_add.speed"],"state":"reachable"}
```

### H2

```json
{"expected":["base_add.speed safe_ess.skill","safe_ess.skill base_add.speed"],"state":"reachable"}
```

### Q2

```json
{"expected":["safe_alloy.mana_skill safe_ess.skill","safe_ess.skill safe_alloy.mana_skill"],"state":"reachable"}
```

### T1

```json
{"expected":["base_add.speed"],"state":"reachable"}
```

### T2

```json
{"expected":["base_add.speed safe_ess.skill","safe_ess.skill base_add.speed"],"state":"reachable"}
```

### H3

```json
{"expected":[],"state":"unreachable"}
```

### Q1

```json
{"expected":[],"state":"unreachable"}
```

### G

```json
{"expected":["regal_base.life safe_alloy.pen","upgrade_ess.life safe_alloy.pen"],"state":"reachable"}
```

### GR

```json
{"expected":["safe_alloy.pen"],"state":"reachable"}
```

### H4

```json
{"expected":["divine.item flesh.item","flesh.item divine.item"],"state":"reachable"}
```

### HF

```json
{"expected":["flesh.item"],"state":"reachable"}
```

### HD

```json
{"expected":["divine.item"],"state":"reachable"}
```

### Q4

```json
{"expected":[""],"state":"reachable"}
```

### H1

```json
{"expected":["safe_ess.def"],"state":"reachable"}
```

### L89

```json
{"expected":[],"state":"unreachable"}
```

### L90

```json
{"expected":[""],"state":"reachable"}
```

### TM90

```json
{"expected":["regal_base.life @T1","regal_base.life @T2","regal_base.life @T3","upgrade_ess_body.life @T3"],"state":"reachable"}
```

### TM100

```json
{"expected":["regal_base.life @T1","regal_base.life @T2"],"state":"reachable"}
```

### TR90

```json
{"expected":["base_add.life @T1","base_add.life @T2","base_add.life @T3","reveal_Altered_Collarbone.life @T1","reveal_Altered_Collarbone.life @T2","reveal_Altered_Collarbone.life @T3","reveal_Ancient_Collarbone.life @T1","reveal_Ancient_Collarbone.life @T2","reveal_Ancient_Collarbone.life @T3","reveal_Preserved_Collarbone.life @T1","reveal_Preserved_Collarbone.life @T2","reveal_Preserved_Collarbone.life @T3"],"state":"reachable"}
```

## 전송한 문안

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
역할: PoE2 제작 작업대가 검증할 레시피를 찾는다. 가능한 경로의 누락을 우선 줄인다. 불확실한 확률을 이유로 후보를 제외하지 않는다.
후보 하나는 모든 남은 목표 행을 한 번씩 채운다. 각 행에서 '실행번호|전략.대상'을 직접 고른다. 실행번호는 1부터 행 수까지 중복 없이 사용한다. >로 연결된 명시적 행동은 왼쪽부터 실행한다. 각 후보의 번호순 행동 전체가 하나의 레시피다. 행이 쓰인 순서는 실행 순서가 아니다.
같은 출처라도 먼저 확보할 목표를 바꾸고, 같은 순서에서도 대체 출처 및 허용된 실패 복구를 바꿔 본다. 이미 붙은 옵션은 보존한다. 필요한 한도 확장은 자리 소모 전에 명시한다. 복구가 붙은 후보는 같은 대상 엑잘 실패 분기다. 비용과 확률은 백엔드가 계산한다.
각 칸에 해당하는 독립 완성 후보를 나열한다. firstAcquisition이 있으면 그 행이 첫 획득이다. 이미 완성된 입력은 빈 실행열 하나, 찾지 못한 경우 빈 목록이다.
```

### P3

```text
역할: PoE2 제작 작업대가 검증할 레시피를 찾는다. 가능한 경로의 누락을 우선 줄인다. 불확실한 확률을 이유로 후보를 제외하지 않는다.
후보 하나는 모든 남은 목표 행을 한 번씩 채운다. 각 행에서 '실행번호|전략.대상'을 직접 고른다. 실행번호는 1부터 행 수까지 중복 없이 사용한다. >로 연결된 명시적 행동은 왼쪽부터 실행한다. 각 후보의 번호순 행동 전체가 하나의 레시피다. 행이 쓰인 순서는 실행 순서가 아니다.
같은 출처라도 먼저 확보할 목표를 바꾸고, 같은 순서에서도 대체 출처 및 허용된 실패 복구를 바꿔 본다. 이미 붙은 옵션은 보존한다. 필요한 한도 확장은 자리 소모 전에 명시한다. 복구가 붙은 후보는 같은 대상 엑잘 실패 분기다. 비용과 확률은 백엔드가 계산한다.
각 칸에 해당하는 독립 완성 후보를 나열한다. firstAcquisition이 있으면 그 행이 첫 획득이다. 이미 완성된 입력은 빈 실행열 하나, 찾지 못한 경우 빈 목록이다.
동일 행의 대체 출처를 연달아 선택하지 않는다. observed에 나온 완성 경로와 다른 이웃 행 조합과 순서를 찾아 각 행의 구체적인 화폐를 직접 적는다.
```

### P4

```text
역할: PoE2 제작 작업대가 검증할 레시피를 찾는다. 가능한 경로의 누락을 우선 줄인다. 불확실한 확률을 이유로 후보를 제외하지 않는다.
후보 하나는 모든 남은 목표 행을 한 번씩 채운다. 각 행에서 '실행번호|전략.대상'을 직접 고른다. 실행번호는 1부터 행 수까지 중복 없이 사용한다. >로 연결된 명시적 행동은 왼쪽부터 실행한다. 각 후보의 번호순 행동 전체가 하나의 레시피다. 행이 쓰인 순서는 실행 순서가 아니다.
같은 출처라도 먼저 확보할 목표를 바꾸고, 같은 순서에서도 대체 출처 및 허용된 실패 복구를 바꿔 본다. 이미 붙은 옵션은 보존한다. 필요한 한도 확장은 자리 소모 전에 명시한다. 복구가 붙은 후보는 같은 대상 엑잘 실패 분기다. 비용과 확률은 백엔드가 계산한다.
각 칸에 해당하는 독립 완성 후보를 나열한다. firstAcquisition이 있으면 그 행이 첫 획득이다. 이미 완성된 입력은 빈 실행열 하나, 찾지 못한 경우 빈 목록이다.
sourceScope가 plain이면 일반 출처만, 목표 행 이름이면 그 행에서만 훼손 출처를 선택한다. 구체적인 전략명인 칸은 그 획득 또는 복구 선택이다. 각 칸 안에서 가능한 획득 순서를 바꿔 본다.
```

### P5

```text
PoE2 제작 경로의 누락을 줄인다. lanes의 각 칸에서 allowedByGoal의 각 행을 정확히 한 번 채운다. 행의 값은 택일 가능한 전략.대상이다. 각 목표에서 하나씩 고른 뒤 화폐를 사용할 순서대로 한 배열에 적는다. 행이 나열된 순서와 실제 획득 순서는 독립이다. 보존할 수 있다면 획득 순서를 바꿔 별도 배열로 적는다.
>를 포함한 단계는 표시된 모든 화폐를 왼쪽부터 실행한다. cap_up은 다음 획득 전에 한도를 늘리고, whittle_miss는 앞 엑잘 실패 직후 실행하는 복구다. 다른 목표 화폐와 복구 사이의 위치도 확인한다. 첫 카오스는 첫 획득에만 가능하다.
이미 확보한 목표는 유지한다. 이미 완성된 아이템은 빈 실행열 하나를 준다. 빈 후보 목록은 이번에 경로를 찾지 못했다는 뜻이다. 확률과 비용은 백엔드가 계산한다.
```

### P6

```text
PoE2 제작 경로의 누락을 줄인다. lanes의 각 칸에서 allowedByGoal의 각 행을 정확히 한 번 채운다. 행의 값은 택일 가능한 전략.대상이다. 각 목표에서 하나씩 고른 뒤 화폐를 사용할 순서대로 한 배열에 적는다. 행이 나열된 순서와 실제 획득 순서는 독립이다. 보존할 수 있다면 획득 순서를 바꿔 별도 배열로 적는다.
>를 포함한 단계는 표시된 모든 화폐를 왼쪽부터 실행한다. cap_up은 다음 획득 전에 한도를 늘리고, whittle_miss는 앞 엑잘 실패 직후 실행하는 복구다. 다른 목표 화폐와 복구 사이의 위치도 확인한다. 첫 카오스는 첫 획득에만 가능하다.
이미 확보한 목표는 유지한다. 이미 완성된 아이템은 빈 실행열 하나를 준다. 빈 후보 목록은 이번에 경로를 찾지 못했다는 뜻이다. 확률과 비용은 백엔드가 계산한다.
각 출처 칸의 oneOpening에는 첫 획득을 하나 정해 완성 경로를 적는다. otherOpenings에는 다른 목표를 처음에 획득하고 같은 출처를 사용하는 완성 경로를 적는다. 각 배열에서 모든 화폐를 명시한다.
```

### P7

```text
역할: PoE2 제작 경로 설계자. 보존 조건을 만족하는 다양한 완성 후보를 제시한다.
입력: goals의 각 키는 미달 조건이고 값은 그중 하나만 성공하면 해당 키가 끝나는 대체 전략.대상 목록이다. start는 현재 상태, targets는 출처, strategies는 실행 의미다. helpers는 준비·복구 전용이다. 모든 실험은 고정 시작 아이템을 쓴다.
순서: openers의 각 전략.대상으로 경우를 나눈다. 획득한 키를 지우고 남은 키 하나를 고르는 모든 순서와 그 키의 각 대체 전략을 시도한다. goals의 나열 순서에는 의미가 없다. 일반 옵션을 첫째·중간·끝에 두는 경로도 점검한다.
실행: 시작 Crafted 점유에서 출발한다. crafted.uses 전략은 자리를 하나씩 쓰며, 부족하면 그 전략 직전에 가능한 한도 확장을 한 번 적는다. 첫 카오스는 첫 획득 전용이다. 같은 아이템의 훼손 슬롯 하나를 공유한다. base_add 적중 경로마다 허용된 동일 대상 whittle_miss가 있으면 그 직후 복구를 넣은 경로도 따로 적는다.
출력: 각 opener 키에 해당 첫 획득을 포함한 전체 전략.대상 배열들을 반환한다. 각 경로가 goals의 키를 하나씩 모두 채웠는지 검토한다. 불가능한 키는 []다. 백엔드는 반환 경로의 전이·확률·비용을 검증한다.
```

### P8

```text
PoE2 작업대에 제시할 화폐·대상의 실제 순서를 설계한다. 보유 중인 시작 옵션을 지키며 allowedByGoal의 행마다 전략 하나를 선택한다. prepare는 첫 획득 전에 쓸 준비 행동이다. 필요한 한도 확장을 지금 할 수 있다면 여기 직접 적는다. 이미 확장했거나 필요 없으면 none이다. steps의 >는 순서대로 실제 실행할 화폐들이다. 확장을 두 번 쓰지 않는다.
각 칸은 firstAcquisition에 적힌 목표를 처음 획득하는 경우다. 첫 준비는 그보다 먼저 가능하다. 다른 모든 목표를 채워 완성 후보를 만들고 나머지 목표들의 순서와 출처도 바꿔 본다. 가능한 후보는 모두 검증 대상으로 남긴다. 빈 목록은 탐색에서 찾지 못했다는 뜻이다.
```

### P9

```text
PoE2 작업대의 시작 아이템과 목표를 읽고 전체 화폐 순서를 recipes로 제안한다. 이미 현재 값이 모든 목표를 충족하면 아무 행동도 하지 않는 경로 하나인 [[]]를 반환한다. 현재 값으로 부족하고 아이템이 축성되어 변경이 막혔으면 후보 목록은 []다. 보존할 옵션이 있지만 아직 획득할 행이 남고 변경할 수 있으면 실제 전략.대상 순서를 채운다. 전략 사전은 사용할 수 있는 행동의 의미이며 시작 상태의 금지를 무효화하지 않는다. 빈 응답만으로 백엔드가 제작 불가를 확정하지 않는다.
```

### P10

```text
PoE2 작업대의 시작 아이템과 목표를 읽고 전체 화폐 순서를 recipes로 제안한다. 이미 현재 값이 모든 목표를 충족하면 아무 행동도 하지 않는 경로 하나인 [[]]를 반환한다. 현재 값으로 부족하고 아이템이 축성되어 변경이 막혔으면 후보 목록은 []다. 보존할 옵션이 있지만 아직 획득할 행이 남고 변경할 수 있으면 실제 전략.대상 순서를 채운다. 전략 사전은 사용할 수 있는 행동의 의미이며 시작 상태의 금지를 무효화하지 않는다. 빈 응답만으로 백엔드가 제작 불가를 확정하지 않는다.
먼저 현재 상태만으로 목표가 충족되는지 판정한다. result.status는 충족하면 done, 미달이며 제안할 실행이 없으면 noCandidate, 명시할 화폐 순서가 있으면 routes다. 해당 recipes도 함께 반환한다.
```

### P11

```text
PoE2 제작 작업대의 한 전략 후보를 제안한다. steps에 실행할 전략.대상을 직접 적고 acceptTiers에는 해당 화폐로 도달할 수 있고 목표 수치가 가능한 티어를 모두 적는다. 동일 티어를 여러 화폐로 얻을 수 있으면 각 화폐를 보존한다. 시작 희귀도, 티어별 출처와 굴림 범위를 확인한다. 실제 값과 확률은 백엔드가 검증한다.
```

### P12

```text
PoE2 제작 작업대의 한 전략 후보를 제안한다. steps에 실행할 전략.대상을 직접 적고 acceptTiers에는 해당 화폐로 도달할 수 있고 목표 수치가 가능한 티어를 모두 적는다. 동일 티어를 여러 화폐로 얻을 수 있으면 각 화폐를 보존한다. 시작 희귀도, 티어별 출처와 굴림 범위를 확인한다. 실제 값과 확률은 백엔드가 검증한다. sourceTiers는 원본 획득 가능 티어다. 각 티어 최대 굴림과 사용자의 하한을 비교하고, 가능한 결과만 acceptTiers에 적는다.
```

## 각 호출의 시나리오·기대·응답·문제

카드별 기대는 위 동명 절에 있다. 아래의 각 요청은 실제 호출 한 번이다. 규칙 위반과 이미 통과한 경로의 중복은 회수 개수에서 제외한다. 음성 대조에서는 빈 결과와 잘못 생성한 후보를 구분한다.

### 요청 1 · T3 · baseline

문안 P1 · 기본 입력 · 회수 8/18 · 거절 0 · 중복 6 · 입력/출력 토큰 932/239.

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

실제 응답

```json
{"base_add.life":[["base_add.life","base_add.cold","base_add.lightning"],["base_add.life","reveal.cold","base_add.lightning"],["base_add.life","base_add.cold","base_add.lightning"],["base_add.life","reveal.cold","base_add.lightning"]],"reveal.life":[["reveal.life","base_add.cold","base_add.lightning"],["reveal.life","base_add.cold","base_add.lightning"]],"base_add.cold":[["base_add.cold","base_add.life","base_add.lightning"],["base_add.cold","reveal.life","base_add.lightning"],["base_add.cold","base_add.life","base_add.lightning"],["base_add.cold","reveal.life","base_add.lightning"]],"reveal.cold":[["reveal.cold","base_add.life","base_add.lightning"],["reveal.cold","base_add.life","base_add.lightning"]],"base_add.lightning":[["base_add.lightning","base_add.life","base_add.cold"],["base_add.lightning","base_add.life","reveal.cold"]]}
```

누락 ["base_add.cold base_add.lightning base_add.life","base_add.cold base_add.lightning reveal.life","base_add.life base_add.lightning base_add.cold","base_add.life base_add.lightning reveal.cold","base_add.lightning base_add.cold base_add.life","base_add.lightning base_add.cold reveal.life","base_add.lightning reveal.cold base_add.life","base_add.lightning reveal.life base_add.cold","reveal.cold base_add.lightning base_add.life","reveal.life base_add.lightning base_add.cold"]

### 요청 2 · T3 · rows

문안 P2 · 기본 입력 · 회수 0/18 · 거절 2 · 중복 0 · 입력/출력 토큰 716/59.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"cold":["base","Preserved Collarbone"],"lightning":["base"]}},"needOnePerRow":{"life":["base_add.life","reveal.life"],"cold":["base_add.cold","reveal.cold"],"lightning":["base_add.lightning"]},"strategies":{"base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다."},"rules":["이 요청은 엑잘과 훼손 획득 경로를 비교한다. 확보한 목표를 제거하지 않는다."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":0},"lanes":{"any":{"sourceScope":"any","firstAcquisition":null}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"any":{"type":"array","items":{"type":"object","properties":{"life":{"type":"string","enum":["1|base_add.life","2|base_add.life","3|base_add.life","1|reveal.life","2|reveal.life","3|reveal.life"]},"cold":{"type":"string","enum":["1|base_add.cold","2|base_add.cold","3|base_add.cold","1|reveal.cold","2|reveal.cold","3|reveal.cold"]},"lightning":{"type":"string","enum":["1|base_add.lightning","2|base_add.lightning","3|base_add.lightning"]}},"required":["life","cold","lightning"],"additionalProperties":false}}},"required":["any"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"any":[{"life":"1|base_add.life","cold":"1|base_add.cold","lightning":"1|base_add.lightning"},{"life":"1|reveal.life","cold":"1|reveal.cold","lightning":"1|base_add.lightning"}]}
```

누락 ["base_add.cold base_add.life base_add.lightning","base_add.cold base_add.lightning base_add.life","base_add.cold base_add.lightning reveal.life","base_add.cold reveal.life base_add.lightning","base_add.life base_add.cold base_add.lightning","base_add.life base_add.lightning base_add.cold","base_add.life base_add.lightning reveal.cold","base_add.life reveal.cold base_add.lightning","base_add.lightning base_add.cold base_add.life","base_add.lightning base_add.cold reveal.life","base_add.lightning base_add.life base_add.cold","base_add.lightning base_add.life reveal.cold","base_add.lightning reveal.cold base_add.life","base_add.lightning reveal.life base_add.cold","reveal.cold base_add.life base_add.lightning","reveal.cold base_add.lightning base_add.life","reveal.life base_add.cold base_add.lightning","reveal.life base_add.lightning base_add.cold"]

<details>
<summary>거절된 후보와 사유</summary>

```json
[[[],"duplicate or missing execution rank"],[[],"duplicate or missing execution rank"]]
```

</details>

### 요청 3 · T3 · diag

문안 P3 · 기본 입력 · 회수 3/18 · 거절 1 · 중복 0 · 입력/출력 토큰 892/113.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"cold":["base","Preserved Collarbone"],"lightning":["base"]}},"needOnePerRow":{"life":["base_add.life","reveal.life"],"cold":["base_add.cold","reveal.cold"],"lightning":["base_add.lightning"]},"strategies":{"base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다."},"rules":["이 요청은 엑잘과 훼손 획득 경로를 비교한다. 확보한 목표를 제거하지 않는다."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":0},"lanes":{"any":{"sourceScope":"any","firstAcquisition":null}},"observed":{"accepted":[["base_add.cold","base_add.life","base_add.lightning"],["base_add.cold","reveal.life","base_add.lightning"],["base_add.life","base_add.cold","base_add.lightning"],["base_add.life","reveal.cold","base_add.lightning"],["base_add.lightning","base_add.life","base_add.cold"],["base_add.lightning","base_add.life","reveal.cold"],["reveal.cold","base_add.life","base_add.lightning"],["reveal.life","base_add.cold","base_add.lightning"]],"issues":[]}}
```

</details>

응답 스키마은 요청 2과 같다.

실제 응답

```json
{"any":[{"life":"1|base_add.life","cold":"2|base_add.cold","lightning":"3|base_add.lightning"},{"life":"1|reveal.life","cold":"2|base_add.cold","lightning":"3|base_add.lightning"},{"life":"1|base_add.life","cold":"2|reveal.cold","lightning":"3|base_add.lightning"},{"life":"1|reveal.life","cold":"2|reveal.cold","lightning":"3|base_add.lightning"}]}
```

누락 ["base_add.cold base_add.life base_add.lightning","base_add.cold base_add.lightning base_add.life","base_add.cold base_add.lightning reveal.life","base_add.cold reveal.life base_add.lightning","base_add.life base_add.lightning base_add.cold","base_add.life base_add.lightning reveal.cold","base_add.lightning base_add.cold base_add.life","base_add.lightning base_add.cold reveal.life","base_add.lightning base_add.life base_add.cold","base_add.lightning base_add.life reveal.cold","base_add.lightning reveal.cold base_add.life","base_add.lightning reveal.life base_add.cold","reveal.cold base_add.life base_add.lightning","reveal.cold base_add.lightning base_add.life","reveal.life base_add.lightning base_add.cold"]

<details>
<summary>거절된 후보와 사유</summary>

```json
[[["reveal.life","reveal.cold","base_add.lightning"],"Desecrated slot occupied"]]
```

</details>

### 요청 4 · T3 · split

문안 P4 · 기본 입력 · 회수 18/18 · 거절 0 · 중복 0 · 입력/출력 토큰 975/499.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"cold":["base","Preserved Collarbone"],"lightning":["base"]}},"needOnePerRow":{"life":["base_add.life","reveal.life"],"cold":["base_add.cold","reveal.cold"],"lightning":["base_add.lightning"]},"strategies":{"base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다."},"rules":["이 요청은 엑잘과 훼손 획득 경로를 비교한다. 확보한 목표를 제거하지 않는다."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":0},"lanes":{"plain":{"sourceScope":"plain","firstAcquisition":null},"life":{"sourceScope":"life","firstAcquisition":null},"cold":{"sourceScope":"cold","firstAcquisition":null}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"plain":{"type":"array","items":{"type":"object","properties":{"life":{"type":"string","enum":["1|base_add.life","2|base_add.life","3|base_add.life"]},"cold":{"type":"string","enum":["1|base_add.cold","2|base_add.cold","3|base_add.cold"]},"lightning":{"type":"string","enum":["1|base_add.lightning","2|base_add.lightning","3|base_add.lightning"]}},"required":["life","cold","lightning"],"additionalProperties":false}},"life":{"type":"array","items":{"type":"object","properties":{"life":{"type":"string","enum":["1|reveal.life","2|reveal.life","3|reveal.life"]},"cold":{"type":"string","enum":["1|base_add.cold","2|base_add.cold","3|base_add.cold"]},"lightning":{"type":"string","enum":["1|base_add.lightning","2|base_add.lightning","3|base_add.lightning"]}},"required":["life","cold","lightning"],"additionalProperties":false}},"cold":{"type":"array","items":{"type":"object","properties":{"life":{"type":"string","enum":["1|base_add.life","2|base_add.life","3|base_add.life"]},"cold":{"type":"string","enum":["1|reveal.cold","2|reveal.cold","3|reveal.cold"]},"lightning":{"type":"string","enum":["1|base_add.lightning","2|base_add.lightning","3|base_add.lightning"]}},"required":["life","cold","lightning"],"additionalProperties":false}}},"required":["plain","life","cold"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"plain":[{"life":"1|base_add.life","cold":"2|base_add.cold","lightning":"3|base_add.lightning"},{"life":"1|base_add.life","cold":"3|base_add.cold","lightning":"2|base_add.lightning"},{"life":"2|base_add.life","cold":"1|base_add.cold","lightning":"3|base_add.lightning"},{"life":"2|base_add.life","cold":"3|base_add.cold","lightning":"1|base_add.lightning"},{"life":"3|base_add.life","cold":"1|base_add.cold","lightning":"2|base_add.lightning"},{"life":"3|base_add.life","cold":"2|base_add.cold","lightning":"1|base_add.lightning"}],"life":[{"life":"1|reveal.life","cold":"2|base_add.cold","lightning":"3|base_add.lightning"},{"life":"1|reveal.life","cold":"3|base_add.cold","lightning":"2|base_add.lightning"},{"life":"2|reveal.life","cold":"1|base_add.cold","lightning":"3|base_add.lightning"},{"life":"2|reveal.life","cold":"3|base_add.cold","lightning":"1|base_add.lightning"},{"life":"3|reveal.life","cold":"1|base_add.cold","lightning":"2|base_add.lightning"},{"life":"3|reveal.life","cold":"2|base_add.cold","lightning":"1|base_add.lightning"}],"cold":[{"life":"1|base_add.life","cold":"2|reveal.cold","lightning":"3|base_add.lightning"},{"life":"1|base_add.life","cold":"3|reveal.cold","lightning":"2|base_add.lightning"},{"life":"2|base_add.life","cold":"1|reveal.cold","lightning":"3|base_add.lightning"},{"life":"2|base_add.life","cold":"3|reveal.cold","lightning":"1|base_add.lightning"},{"life":"3|base_add.life","cold":"1|reveal.cold","lightning":"2|base_add.lightning"},{"life":"3|base_add.life","cold":"2|reveal.cold","lightning":"1|base_add.lightning"}]}
```

### 요청 5 · T3 · grid

문안 P4 · 기본 입력 · 회수 14/18 · 거절 2 · 중복 0 · 입력/출력 토큰 1490/490.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"cold":["base","Preserved Collarbone"],"lightning":["base"]}},"needOnePerRow":{"life":["base_add.life","reveal.life"],"cold":["base_add.cold","reveal.cold"],"lightning":["base_add.lightning"]},"strategies":{"base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다."},"rules":["이 요청은 엑잘과 훼손 획득 경로를 비교한다. 확보한 목표를 제거하지 않는다."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":0},"lanes":{"plain@life":{"sourceScope":"plain","firstAcquisition":"life"},"plain@cold":{"sourceScope":"plain","firstAcquisition":"cold"},"plain@lightning":{"sourceScope":"plain","firstAcquisition":"lightning"},"life@life":{"sourceScope":"life","firstAcquisition":"life"},"life@cold":{"sourceScope":"life","firstAcquisition":"cold"},"life@lightning":{"sourceScope":"life","firstAcquisition":"lightning"},"cold@life":{"sourceScope":"cold","firstAcquisition":"life"},"cold@cold":{"sourceScope":"cold","firstAcquisition":"cold"},"cold@lightning":{"sourceScope":"cold","firstAcquisition":"lightning"}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"plain@life":{"type":"array","items":{"type":"object","properties":{"life":{"type":"string","enum":["1|base_add.life"]},"cold":{"type":"string","enum":["2|base_add.cold","3|base_add.cold"]},"lightning":{"type":"string","enum":["2|base_add.lightning","3|base_add.lightning"]}},"required":["life","cold","lightning"],"additionalProperties":false}},"plain@cold":{"type":"array","items":{"type":"object","properties":{"life":{"type":"string","enum":["2|base_add.life","3|base_add.life"]},"cold":{"type":"string","enum":["1|base_add.cold"]},"lightning":{"type":"string","enum":["2|base_add.lightning","3|base_add.lightning"]}},"required":["life","cold","lightning"],"additionalProperties":false}},"plain@lightning":{"type":"array","items":{"type":"object","properties":{"life":{"type":"string","enum":["2|base_add.life","3|base_add.life"]},"cold":{"type":"string","enum":["2|base_add.cold","3|base_add.cold"]},"lightning":{"type":"string","enum":["1|base_add.lightning"]}},"required":["life","cold","lightning"],"additionalProperties":false}},"life@life":{"type":"array","items":{"type":"object","properties":{"life":{"type":"string","enum":["1|reveal.life"]},"cold":{"type":"string","enum":["2|base_add.cold","3|base_add.cold"]},"lightning":{"type":"string","enum":["2|base_add.lightning","3|base_add.lightning"]}},"required":["life","cold","lightning"],"additionalProperties":false}},"life@cold":{"type":"array","items":{"type":"object","properties":{"life":{"type":"string","enum":["2|reveal.life","3|reveal.life"]},"cold":{"type":"string","enum":["1|base_add.cold"]},"lightning":{"type":"string","enum":["2|base_add.lightning","3|base_add.lightning"]}},"required":["life","cold","lightning"],"additionalProperties":false}},"life@lightning":{"type":"array","items":{"type":"object","properties":{"life":{"type":"string","enum":["2|reveal.life","3|reveal.life"]},"cold":{"type":"string","enum":["2|base_add.cold","3|base_add.cold"]},"lightning":{"type":"string","enum":["1|base_add.lightning"]}},"required":["life","cold","lightning"],"additionalProperties":false}},"cold@life":{"type":"array","items":{"type":"object","properties":{"life":{"type":"string","enum":["1|base_add.life"]},"cold":{"type":"string","enum":["2|reveal.cold","3|reveal.cold"]},"lightning":{"type":"string","enum":["2|base_add.lightning","3|base_add.lightning"]}},"required":["life","cold","lightning"],"additionalProperties":false}},"cold@cold":{"type":"array","items":{"type":"object","properties":{"life":{"type":"string","enum":["2|base_add.life","3|base_add.life"]},"cold":{"type":"string","enum":["1|reveal.cold"]},"lightning":{"type":"string","enum":["2|base_add.lightning","3|base_add.lightning"]}},"required":["life","cold","lightning"],"additionalProperties":false}},"cold@lightning":{"type":"array","items":{"type":"object","properties":{"life":{"type":"string","enum":["2|base_add.life","3|base_add.life"]},"cold":{"type":"string","enum":["2|reveal.cold","3|reveal.cold"]},"lightning":{"type":"string","enum":["1|base_add.lightning"]}},"required":["life","cold","lightning"],"additionalProperties":false}}},"required":["plain@life","plain@cold","plain@lightning","life@life","life@cold","life@lightning","cold@life","cold@cold","cold@lightning"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"plain@life":[{"life":"1|base_add.life","cold":"2|base_add.cold","lightning":"3|base_add.lightning"}],"plain@cold":[{"life":"2|base_add.life","cold":"1|base_add.cold","lightning":"3|base_add.lightning"},{"life":"3|base_add.life","cold":"1|base_add.cold","lightning":"2|base_add.lightning"}],"plain@lightning":[{"life":"2|base_add.life","cold":"3|base_add.cold","lightning":"1|base_add.lightning"},{"life":"3|base_add.life","cold":"2|base_add.cold","lightning":"1|base_add.lightning"}],"life@life":[{"life":"1|reveal.life","cold":"2|base_add.cold","lightning":"3|base_add.lightning"}],"life@cold":[{"life":"2|reveal.life","cold":"1|base_add.cold","lightning":"3|base_add.lightning"},{"life":"3|reveal.life","cold":"1|base_add.cold","lightning":"2|base_add.lightning"}],"life@lightning":[{"life":"2|reveal.life","cold":"3|base_add.cold","lightning":"1|base_add.lightning"},{"life":"3|reveal.life","cold":"2|base_add.cold","lightning":"1|base_add.lightning"}],"cold@life":[{"life":"1|base_add.life","cold":"2|reveal.cold","lightning":"3|base_add.lightning"},{"life":"1|base_add.life","cold":"3|reveal.cold","lightning":"2|base_add.lightning"}],"cold@cold":[{"life":"2|base_add.life","cold":"1|reveal.cold","lightning":"3|base_add.lightning"},{"life":"3|base_add.life","cold":"1|reveal.cold","lightning":"2|base_add.lightning"}],"cold@lightning":[{"life":"2|base_add.life","cold":"2|reveal.cold","lightning":"1|base_add.lightning"},{"life":"3|base_add.life","cold":"3|reveal.cold","lightning":"1|base_add.lightning"}]}
```

누락 ["base_add.life base_add.lightning base_add.cold","base_add.lightning base_add.life reveal.cold","base_add.lightning reveal.cold base_add.life","reveal.life base_add.lightning base_add.cold"]

<details>
<summary>거절된 후보와 사유</summary>

```json
[[[],"duplicate or missing execution rank"],[[],"duplicate or missing execution rank"]]
```

</details>

### 요청 6 · T3 · split

문안 P4 · 입력 역순 · 회수 9/18 · 거절 0 · 중복 0 · 입력/출력 토큰 975/256.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"cold":["base","Preserved Collarbone"],"lightning":["base"]}},"needOnePerRow":{"lightning":["base_add.lightning"],"cold":["base_add.cold","reveal.cold"],"life":["base_add.life","reveal.life"]},"strategies":{"base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다."},"rules":["이 요청은 엑잘과 훼손 획득 경로를 비교한다. 확보한 목표를 제거하지 않는다."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":0},"lanes":{"plain":{"sourceScope":"plain","firstAcquisition":null},"cold":{"sourceScope":"cold","firstAcquisition":null},"life":{"sourceScope":"life","firstAcquisition":null}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"plain":{"type":"array","items":{"type":"object","properties":{"lightning":{"type":"string","enum":["1|base_add.lightning","2|base_add.lightning","3|base_add.lightning"]},"cold":{"type":"string","enum":["1|base_add.cold","2|base_add.cold","3|base_add.cold"]},"life":{"type":"string","enum":["1|base_add.life","2|base_add.life","3|base_add.life"]}},"required":["lightning","cold","life"],"additionalProperties":false}},"cold":{"type":"array","items":{"type":"object","properties":{"lightning":{"type":"string","enum":["1|base_add.lightning","2|base_add.lightning","3|base_add.lightning"]},"cold":{"type":"string","enum":["1|reveal.cold","2|reveal.cold","3|reveal.cold"]},"life":{"type":"string","enum":["1|base_add.life","2|base_add.life","3|base_add.life"]}},"required":["lightning","cold","life"],"additionalProperties":false}},"life":{"type":"array","items":{"type":"object","properties":{"lightning":{"type":"string","enum":["1|base_add.lightning","2|base_add.lightning","3|base_add.lightning"]},"cold":{"type":"string","enum":["1|base_add.cold","2|base_add.cold","3|base_add.cold"]},"life":{"type":"string","enum":["1|reveal.life","2|reveal.life","3|reveal.life"]}},"required":["lightning","cold","life"],"additionalProperties":false}}},"required":["plain","cold","life"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"plain":[{"lightning":"1|base_add.lightning","cold":"2|base_add.cold","life":"3|base_add.life"},{"lightning":"2|base_add.lightning","cold":"1|base_add.cold","life":"3|base_add.life"},{"lightning":"3|base_add.lightning","cold":"1|base_add.cold","life":"2|base_add.life"}],"cold":[{"lightning":"1|base_add.lightning","cold":"2|reveal.cold","life":"3|base_add.life"},{"lightning":"2|base_add.lightning","cold":"1|reveal.cold","life":"3|base_add.life"},{"lightning":"3|base_add.lightning","cold":"1|reveal.cold","life":"2|base_add.life"}],"life":[{"lightning":"1|base_add.lightning","cold":"2|base_add.cold","life":"3|reveal.life"},{"lightning":"2|base_add.lightning","cold":"1|base_add.cold","life":"3|reveal.life"},{"lightning":"3|base_add.lightning","cold":"1|base_add.cold","life":"2|reveal.life"}]}
```

누락 ["base_add.life base_add.cold base_add.lightning","base_add.life base_add.lightning base_add.cold","base_add.life base_add.lightning reveal.cold","base_add.life reveal.cold base_add.lightning","base_add.lightning base_add.life base_add.cold","base_add.lightning base_add.life reveal.cold","base_add.lightning reveal.life base_add.cold","reveal.life base_add.cold base_add.lightning","reveal.life base_add.lightning base_add.cold"]

### 요청 7 · R · split

문안 P4 · 기본 입력 · 회수 6/6 · 거절 0 · 중복 0 · 입력/출력 토큰 828/115.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"cold":["base","Preserved Collarbone"]}},"needOnePerRow":{"life":["base_add.life","reveal.life"],"cold":["base_add.cold","reveal.cold"]},"strategies":{"base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다."},"rules":["이 요청은 엑잘과 훼손 획득 경로를 비교한다. 확보한 목표를 제거하지 않는다."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":0},"lanes":{"plain":{"sourceScope":"plain","firstAcquisition":null},"life":{"sourceScope":"life","firstAcquisition":null},"cold":{"sourceScope":"cold","firstAcquisition":null}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"plain":{"type":"array","items":{"type":"object","properties":{"life":{"type":"string","enum":["1|base_add.life","2|base_add.life"]},"cold":{"type":"string","enum":["1|base_add.cold","2|base_add.cold"]}},"required":["life","cold"],"additionalProperties":false}},"life":{"type":"array","items":{"type":"object","properties":{"life":{"type":"string","enum":["1|reveal.life","2|reveal.life"]},"cold":{"type":"string","enum":["1|base_add.cold","2|base_add.cold"]}},"required":["life","cold"],"additionalProperties":false}},"cold":{"type":"array","items":{"type":"object","properties":{"life":{"type":"string","enum":["1|base_add.life","2|base_add.life"]},"cold":{"type":"string","enum":["1|reveal.cold","2|reveal.cold"]}},"required":["life","cold"],"additionalProperties":false}}},"required":["plain","life","cold"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"plain":[{"life":"1|base_add.life","cold":"2|base_add.cold"},{"life":"2|base_add.life","cold":"1|base_add.cold"}],"life":[{"life":"1|reveal.life","cold":"2|base_add.cold"},{"life":"2|reveal.life","cold":"1|base_add.cold"}],"cold":[{"life":"1|base_add.life","cold":"2|reveal.cold"},{"life":"2|base_add.life","cold":"1|reveal.cold"}]}
```

### 요청 8 · Q3 · split

문안 P4 · 기본 입력 · 회수 1/1 · 거절 1 · 중복 0 · 입력/출력 토큰 700/27.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","desecrated":true}],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"cold":["base","Preserved Collarbone"]}},"needOnePerRow":{"cold":["base_add.cold","reveal.cold"]},"strategies":{"base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다."},"rules":["이 요청은 엑잘과 훼손 획득 경로를 비교한다. 확보한 목표를 제거하지 않는다."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":1},"lanes":{"plain":{"sourceScope":"plain","firstAcquisition":null},"cold":{"sourceScope":"cold","firstAcquisition":null}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"plain":{"type":"array","items":{"type":"object","properties":{"cold":{"type":"string","enum":["1|base_add.cold"]}},"required":["cold"],"additionalProperties":false}},"cold":{"type":"array","items":{"type":"object","properties":{"cold":{"type":"string","enum":["1|reveal.cold"]}},"required":["cold"],"additionalProperties":false}}},"required":["plain","cold"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"plain":[{"cold":"1|base_add.cold"}],"cold":[{"cold":"1|reveal.cold"}]}
```

<details>
<summary>거절된 후보와 사유</summary>

```json
[[["reveal.cold"],"Desecrated slot occupied"]]
```

</details>

### 요청 9 · T3 · stable

문안 P4 · 기본 입력 · 회수 18/18 · 거절 0 · 중복 0 · 입력/출력 토큰 975/499.

전송 JSON은 요청 4과 같다.

응답 스키마은 요청 4과 같다.

실제 응답

```json
{"plain":[{"life":"1|base_add.life","cold":"2|base_add.cold","lightning":"3|base_add.lightning"},{"life":"1|base_add.life","cold":"3|base_add.cold","lightning":"2|base_add.lightning"},{"life":"2|base_add.life","cold":"1|base_add.cold","lightning":"3|base_add.lightning"},{"life":"2|base_add.life","cold":"3|base_add.cold","lightning":"1|base_add.lightning"},{"life":"3|base_add.life","cold":"1|base_add.cold","lightning":"2|base_add.lightning"},{"life":"3|base_add.life","cold":"2|base_add.cold","lightning":"1|base_add.lightning"}],"life":[{"life":"1|reveal.life","cold":"2|base_add.cold","lightning":"3|base_add.lightning"},{"life":"1|reveal.life","cold":"3|base_add.cold","lightning":"2|base_add.lightning"},{"life":"2|reveal.life","cold":"1|base_add.cold","lightning":"3|base_add.lightning"},{"life":"2|reveal.life","cold":"3|base_add.cold","lightning":"1|base_add.lightning"},{"life":"3|reveal.life","cold":"1|base_add.cold","lightning":"2|base_add.lightning"},{"life":"3|reveal.life","cold":"2|base_add.cold","lightning":"1|base_add.lightning"}],"cold":[{"life":"1|base_add.life","cold":"2|reveal.cold","lightning":"3|base_add.lightning"},{"life":"1|base_add.life","cold":"3|reveal.cold","lightning":"2|base_add.lightning"},{"life":"2|base_add.life","cold":"1|reveal.cold","lightning":"3|base_add.lightning"},{"life":"2|base_add.life","cold":"3|reveal.cold","lightning":"1|base_add.lightning"},{"life":"3|base_add.life","cold":"1|reveal.cold","lightning":"2|base_add.lightning"},{"life":"3|base_add.life","cold":"2|reveal.cold","lightning":"1|base_add.lightning"}]}
```

### 요청 10 · T3 · stable

문안 P4 · 입력 역순 · 회수 18/18 · 거절 0 · 중복 0 · 입력/출력 토큰 975/499.

전송 JSON은 요청 4과 같다.

응답 스키마은 요청 4과 같다.

실제 응답

```json
{"plain":[{"life":"1|base_add.life","cold":"2|base_add.cold","lightning":"3|base_add.lightning"},{"life":"1|base_add.life","cold":"3|base_add.cold","lightning":"2|base_add.lightning"},{"life":"2|base_add.life","cold":"1|base_add.cold","lightning":"3|base_add.lightning"},{"life":"2|base_add.life","cold":"3|base_add.cold","lightning":"1|base_add.lightning"},{"life":"3|base_add.life","cold":"1|base_add.cold","lightning":"2|base_add.lightning"},{"life":"3|base_add.life","cold":"2|base_add.cold","lightning":"1|base_add.lightning"}],"life":[{"life":"1|reveal.life","cold":"2|base_add.cold","lightning":"3|base_add.lightning"},{"life":"1|reveal.life","cold":"3|base_add.cold","lightning":"2|base_add.lightning"},{"life":"2|reveal.life","cold":"1|base_add.cold","lightning":"3|base_add.lightning"},{"life":"2|reveal.life","cold":"3|base_add.cold","lightning":"1|base_add.lightning"},{"life":"3|reveal.life","cold":"1|base_add.cold","lightning":"2|base_add.lightning"},{"life":"3|reveal.life","cold":"2|base_add.cold","lightning":"1|base_add.lightning"}],"cold":[{"life":"1|base_add.life","cold":"2|reveal.cold","lightning":"3|base_add.lightning"},{"life":"1|base_add.life","cold":"3|reveal.cold","lightning":"2|base_add.lightning"},{"life":"2|base_add.life","cold":"1|reveal.cold","lightning":"3|base_add.lightning"},{"life":"2|base_add.life","cold":"3|reveal.cold","lightning":"1|base_add.lightning"},{"life":"3|base_add.life","cold":"1|reveal.cold","lightning":"2|base_add.lightning"},{"life":"3|base_add.life","cold":"2|reveal.cold","lightning":"1|base_add.lightning"}]}
```

### 요청 11 · N · baseline

문안 P1 · 기본 입력 · 회수 7/7 · 거절 1 · 중복 0 · 입력/출력 토큰 1038/137.

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

실제 응답

```json
{"first_chaos.life":[["first_chaos.life","safe_ess.def"]],"base_add.life":{"hit":[["base_add.life","safe_ess.def"]],"onMiss":[["base_add.life","whittle_miss.life","safe_ess.def"]]},"reveal.life":[["reveal.life","safe_ess.def"]],"safe_ess.def":{"hit":[["safe_ess.def","base_add.life"],["safe_ess.def","reveal.life"]],"onMiss":[["safe_ess.def","base_add.life","whittle_miss.life"],["safe_ess.def","whittle_miss.life","base_add.life"]]}}
```

<details>
<summary>거절된 후보와 사유</summary>

```json
[[["safe_ess.def","whittle_miss.life","base_add.life"],"recovery requires preceding base_add on same target"]]
```

</details>

### 요청 12 · N · split

문안 P4 · 기본 입력 · 회수 0/7 · 거절 8 · 중복 0 · 입력/출력 토큰 1125/185.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"needOnePerRow":{"life":["first_chaos.life","base_add.life","reveal.life"],"def":["safe_ess.def"]},"strategies":{"first_chaos":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함. 첫 획득에서만. 분열 보존.","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","whittle_miss":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략. 같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."},"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":1,"expandBy":0,"desecratedUsed":0},"lanes":{"first_chaos.life":{"sourceScope":"first_chaos.life","firstAcquisition":null},"base_add.life":{"sourceScope":"base_add.life","firstAcquisition":null},"base_add.life>whittle_miss.life":{"sourceScope":"base_add.life>whittle_miss.life","firstAcquisition":null},"reveal.life":{"sourceScope":"reveal.life","firstAcquisition":null}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"first_chaos.life":{"type":"array","items":{"type":"object","properties":{"life":{"type":"string","enum":["1|first_chaos.life","2|first_chaos.life"]},"def":{"type":"string","enum":["1|safe_ess.def","2|safe_ess.def"]}},"required":["life","def"],"additionalProperties":false}},"base_add.life":{"type":"array","items":{"type":"object","properties":{"life":{"type":"string","enum":["1|base_add.life","2|base_add.life"]},"def":{"type":"string","enum":["1|safe_ess.def","2|safe_ess.def"]}},"required":["life","def"],"additionalProperties":false}},"base_add.life>whittle_miss.life":{"type":"array","items":{"type":"object","properties":{"life":{"type":"string","enum":["1|base_add.life>whittle_miss.life","2|base_add.life>whittle_miss.life"]},"def":{"type":"string","enum":["1|safe_ess.def","2|safe_ess.def"]}},"required":["life","def"],"additionalProperties":false}},"reveal.life":{"type":"array","items":{"type":"object","properties":{"life":{"type":"string","enum":["1|reveal.life","2|reveal.life"]},"def":{"type":"string","enum":["1|safe_ess.def","2|safe_ess.def"]}},"required":["life","def"],"additionalProperties":false}}},"required":["first_chaos.life","base_add.life","base_add.life>whittle_miss.life","reveal.life"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"first_chaos.life":[{"life":"1|first_chaos.life","def":"1|safe_ess.def"},{"life":"2|first_chaos.life","def":"2|safe_ess.def"}],"base_add.life":[{"life":"1|base_add.life","def":"1|safe_ess.def"},{"life":"2|base_add.life","def":"2|safe_ess.def"}],"base_add.life>whittle_miss.life":[{"life":"1|base_add.life>whittle_miss.life","def":"1|safe_ess.def"},{"life":"2|base_add.life>whittle_miss.life","def":"2|safe_ess.def"}],"reveal.life":[{"life":"1|reveal.life","def":"1|safe_ess.def"},{"life":"2|reveal.life","def":"2|safe_ess.def"}]}
```

누락 ["base_add.life safe_ess.def","base_add.life whittle_miss.life safe_ess.def","first_chaos.life safe_ess.def","reveal.life safe_ess.def","safe_ess.def base_add.life","safe_ess.def base_add.life whittle_miss.life","safe_ess.def reveal.life"]

<details>
<summary>거절된 후보와 사유</summary>

```json
[[[],"duplicate or missing execution rank"],[[],"duplicate or missing execution rank"],[[],"duplicate or missing execution rank"],[[],"duplicate or missing execution rank"],[[],"duplicate or missing execution rank"],[[],"duplicate or missing execution rank"],[[],"duplicate or missing execution rank"],[[],"duplicate or missing execution rank"]]
```

</details>

### 요청 13 · N · split

문안 P4 · 입력 역순 · 회수 7/7 · 거절 1 · 중복 0 · 입력/출력 토큰 1125/185.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"needOnePerRow":{"def":["safe_ess.def"],"life":["first_chaos.life","base_add.life","reveal.life"]},"strategies":{"first_chaos":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함. 첫 획득에서만. 분열 보존.","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","whittle_miss":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략. 같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."},"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":1,"expandBy":0,"desecratedUsed":0},"lanes":{"reveal.life":{"sourceScope":"reveal.life","firstAcquisition":null},"base_add.life>whittle_miss.life":{"sourceScope":"base_add.life>whittle_miss.life","firstAcquisition":null},"base_add.life":{"sourceScope":"base_add.life","firstAcquisition":null},"first_chaos.life":{"sourceScope":"first_chaos.life","firstAcquisition":null}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"reveal.life":{"type":"array","items":{"type":"object","properties":{"def":{"type":"string","enum":["1|safe_ess.def","2|safe_ess.def"]},"life":{"type":"string","enum":["1|reveal.life","2|reveal.life"]}},"required":["def","life"],"additionalProperties":false}},"base_add.life>whittle_miss.life":{"type":"array","items":{"type":"object","properties":{"def":{"type":"string","enum":["1|safe_ess.def","2|safe_ess.def"]},"life":{"type":"string","enum":["1|base_add.life>whittle_miss.life","2|base_add.life>whittle_miss.life"]}},"required":["def","life"],"additionalProperties":false}},"base_add.life":{"type":"array","items":{"type":"object","properties":{"def":{"type":"string","enum":["1|safe_ess.def","2|safe_ess.def"]},"life":{"type":"string","enum":["1|base_add.life","2|base_add.life"]}},"required":["def","life"],"additionalProperties":false}},"first_chaos.life":{"type":"array","items":{"type":"object","properties":{"def":{"type":"string","enum":["1|safe_ess.def","2|safe_ess.def"]},"life":{"type":"string","enum":["1|first_chaos.life","2|first_chaos.life"]}},"required":["def","life"],"additionalProperties":false}}},"required":["reveal.life","base_add.life>whittle_miss.life","base_add.life","first_chaos.life"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"reveal.life":[{"def":"1|safe_ess.def","life":"2|reveal.life"},{"def":"2|safe_ess.def","life":"1|reveal.life"}],"base_add.life>whittle_miss.life":[{"def":"1|safe_ess.def","life":"2|base_add.life>whittle_miss.life"},{"def":"2|safe_ess.def","life":"1|base_add.life>whittle_miss.life"}],"base_add.life":[{"def":"1|safe_ess.def","life":"2|base_add.life"},{"def":"2|safe_ess.def","life":"1|base_add.life"}],"first_chaos.life":[{"def":"1|safe_ess.def","life":"2|first_chaos.life"},{"def":"2|safe_ess.def","life":"1|first_chaos.life"}]}
```

<details>
<summary>거절된 후보와 사유</summary>

```json
[[["safe_ess.def","first_chaos.life"],"opening acquisition only"]]
```

</details>

### 요청 14 · N · stable

문안 P4 · 기본 입력 · 회수 0/7 · 거절 8 · 중복 0 · 입력/출력 토큰 1125/185.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"needOnePerRow":{"def":["safe_ess.def"],"life":["first_chaos.life","base_add.life","reveal.life"]},"strategies":{"first_chaos":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함. 첫 획득에서만. 분열 보존.","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","whittle_miss":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략. 같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."},"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":1,"expandBy":0,"desecratedUsed":0},"lanes":{"base_add.life":{"sourceScope":"base_add.life","firstAcquisition":null},"base_add.life>whittle_miss.life":{"sourceScope":"base_add.life>whittle_miss.life","firstAcquisition":null},"first_chaos.life":{"sourceScope":"first_chaos.life","firstAcquisition":null},"reveal.life":{"sourceScope":"reveal.life","firstAcquisition":null}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"base_add.life":{"type":"array","items":{"type":"object","properties":{"def":{"type":"string","enum":["1|safe_ess.def","2|safe_ess.def"]},"life":{"type":"string","enum":["1|base_add.life","2|base_add.life"]}},"required":["def","life"],"additionalProperties":false}},"base_add.life>whittle_miss.life":{"type":"array","items":{"type":"object","properties":{"def":{"type":"string","enum":["1|safe_ess.def","2|safe_ess.def"]},"life":{"type":"string","enum":["1|base_add.life>whittle_miss.life","2|base_add.life>whittle_miss.life"]}},"required":["def","life"],"additionalProperties":false}},"first_chaos.life":{"type":"array","items":{"type":"object","properties":{"def":{"type":"string","enum":["1|safe_ess.def","2|safe_ess.def"]},"life":{"type":"string","enum":["1|first_chaos.life","2|first_chaos.life"]}},"required":["def","life"],"additionalProperties":false}},"reveal.life":{"type":"array","items":{"type":"object","properties":{"def":{"type":"string","enum":["1|safe_ess.def","2|safe_ess.def"]},"life":{"type":"string","enum":["1|reveal.life","2|reveal.life"]}},"required":["def","life"],"additionalProperties":false}}},"required":["base_add.life","base_add.life>whittle_miss.life","first_chaos.life","reveal.life"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"base_add.life":[{"def":"1|safe_ess.def","life":"1|base_add.life"},{"def":"2|safe_ess.def","life":"2|base_add.life"}],"base_add.life>whittle_miss.life":[{"def":"1|safe_ess.def","life":"1|base_add.life>whittle_miss.life"},{"def":"2|safe_ess.def","life":"2|base_add.life>whittle_miss.life"}],"first_chaos.life":[{"def":"1|safe_ess.def","life":"1|first_chaos.life"},{"def":"2|safe_ess.def","life":"2|first_chaos.life"}],"reveal.life":[{"def":"1|safe_ess.def","life":"1|reveal.life"},{"def":"2|safe_ess.def","life":"2|reveal.life"}]}
```

누락 ["base_add.life safe_ess.def","base_add.life whittle_miss.life safe_ess.def","first_chaos.life safe_ess.def","reveal.life safe_ess.def","safe_ess.def base_add.life","safe_ess.def base_add.life whittle_miss.life","safe_ess.def reveal.life"]

<details>
<summary>거절된 후보와 사유</summary>

```json
[[[],"duplicate or missing execution rank"],[[],"duplicate or missing execution rank"],[[],"duplicate or missing execution rank"],[[],"duplicate or missing execution rank"],[[],"duplicate or missing execution rank"],[[],"duplicate or missing execution rank"],[[],"duplicate or missing execution rank"],[[],"duplicate or missing execution rank"]]
```

</details>

### 요청 15 · N · stable

문안 P4 · 입력 역순 · 회수 0/7 · 거절 8 · 중복 0 · 입력/출력 토큰 1125/185.

전송 JSON은 요청 14과 같다.

응답 스키마은 요청 14과 같다.

실제 응답

```json
{"base_add.life":[{"def":"1|safe_ess.def","life":"1|base_add.life"},{"def":"2|safe_ess.def","life":"2|base_add.life"}],"base_add.life>whittle_miss.life":[{"def":"1|safe_ess.def","life":"1|base_add.life>whittle_miss.life"},{"def":"2|safe_ess.def","life":"2|base_add.life>whittle_miss.life"}],"first_chaos.life":[{"def":"1|safe_ess.def","life":"1|first_chaos.life"},{"def":"2|safe_ess.def","life":"2|first_chaos.life"}],"reveal.life":[{"def":"1|safe_ess.def","life":"1|reveal.life"},{"def":"2|safe_ess.def","life":"2|reveal.life"}]}
```

누락 ["base_add.life safe_ess.def","base_add.life whittle_miss.life safe_ess.def","first_chaos.life safe_ess.def","reveal.life safe_ess.def","safe_ess.def base_add.life","safe_ess.def base_add.life whittle_miss.life","safe_ess.def reveal.life"]

<details>
<summary>거절된 후보와 사유</summary>

```json
[[[],"duplicate or missing execution rank"],[[],"duplicate or missing execution rank"],[[],"duplicate or missing execution rank"],[[],"duplicate or missing execution rank"],[[],"duplicate or missing execution rank"],[[],"duplicate or missing execution rank"],[[],"duplicate or missing execution rank"],[[],"duplicate or missing execution rank"]]
```

</details>

### 요청 16 · N · sequence

문안 P5 · 기본 입력 · 회수 4/7 · 거절 0 · 중복 0 · 입력/출력 토큰 959/77.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"needOnePerRow":{"life":["first_chaos.life","base_add.life","reveal.life"],"def":["safe_ess.def"]},"strategies":{"first_chaos":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함. 첫 획득에서만. 분열 보존.","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","whittle_miss":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략. 같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."},"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":1,"expandBy":0,"desecratedUsed":0},"lanes":{"first_chaos.life":{"sourceScope":"first_chaos.life","allowedByGoal":{"life":["first_chaos.life"],"def":["safe_ess.def"]}},"base_add.life":{"sourceScope":"base_add.life","allowedByGoal":{"life":["base_add.life"],"def":["safe_ess.def"]}},"base_add.life>whittle_miss.life":{"sourceScope":"base_add.life>whittle_miss.life","allowedByGoal":{"life":["base_add.life>whittle_miss.life"],"def":["safe_ess.def"]}},"reveal.life":{"sourceScope":"reveal.life","allowedByGoal":{"life":["reveal.life"],"def":["safe_ess.def"]}}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"first_chaos.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","safe_ess.def"]},"minItems":2,"maxItems":2}},"base_add.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","safe_ess.def"]},"minItems":2,"maxItems":2}},"base_add.life>whittle_miss.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life>whittle_miss.life","safe_ess.def"]},"minItems":2,"maxItems":2}},"reveal.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["reveal.life","safe_ess.def"]},"minItems":2,"maxItems":2}}},"required":["first_chaos.life","base_add.life","base_add.life>whittle_miss.life","reveal.life"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"first_chaos.life":[["first_chaos.life","safe_ess.def"]],"base_add.life":[["base_add.life","safe_ess.def"]],"base_add.life>whittle_miss.life":[["base_add.life>whittle_miss.life","safe_ess.def"]],"reveal.life":[["reveal.life","safe_ess.def"]]}
```

누락 ["safe_ess.def base_add.life","safe_ess.def base_add.life whittle_miss.life","safe_ess.def reveal.life"]

### 요청 17 · N · sequence

문안 P5 · 입력 역순 · 회수 4/7 · 거절 0 · 중복 0 · 입력/출력 토큰 959/77.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"needOnePerRow":{"def":["safe_ess.def"],"life":["first_chaos.life","base_add.life","reveal.life"]},"strategies":{"first_chaos":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함. 첫 획득에서만. 분열 보존.","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","whittle_miss":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략. 같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."},"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":1,"expandBy":0,"desecratedUsed":0},"lanes":{"reveal.life":{"sourceScope":"reveal.life","allowedByGoal":{"def":["safe_ess.def"],"life":["reveal.life"]}},"base_add.life>whittle_miss.life":{"sourceScope":"base_add.life>whittle_miss.life","allowedByGoal":{"def":["safe_ess.def"],"life":["base_add.life>whittle_miss.life"]}},"base_add.life":{"sourceScope":"base_add.life","allowedByGoal":{"def":["safe_ess.def"],"life":["base_add.life"]}},"first_chaos.life":{"sourceScope":"first_chaos.life","allowedByGoal":{"def":["safe_ess.def"],"life":["first_chaos.life"]}}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"reveal.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def","reveal.life"]},"minItems":2,"maxItems":2}},"base_add.life>whittle_miss.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def","base_add.life>whittle_miss.life"]},"minItems":2,"maxItems":2}},"base_add.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def","base_add.life"]},"minItems":2,"maxItems":2}},"first_chaos.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def","first_chaos.life"]},"minItems":2,"maxItems":2}}},"required":["reveal.life","base_add.life>whittle_miss.life","base_add.life","first_chaos.life"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"reveal.life":[["reveal.life","safe_ess.def"]],"base_add.life>whittle_miss.life":[["base_add.life>whittle_miss.life","safe_ess.def"]],"base_add.life":[["base_add.life","safe_ess.def"]],"first_chaos.life":[["first_chaos.life","safe_ess.def"]]}
```

누락 ["safe_ess.def base_add.life","safe_ess.def base_add.life whittle_miss.life","safe_ess.def reveal.life"]

### 요청 18 · N · another

문안 P6 · 기본 입력 · 회수 4/7 · 거절 0 · 중복 0 · 입력/출력 토큰 1201/112.

전송 JSON은 요청 16과 같다.

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"first_chaos.life":{"type":"object","properties":{"oneOpening":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","safe_ess.def"]},"minItems":2,"maxItems":2}},"otherOpenings":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","safe_ess.def"]},"minItems":2,"maxItems":2}}},"required":["oneOpening","otherOpenings"],"additionalProperties":false},"base_add.life":{"type":"object","properties":{"oneOpening":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","safe_ess.def"]},"minItems":2,"maxItems":2}},"otherOpenings":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","safe_ess.def"]},"minItems":2,"maxItems":2}}},"required":["oneOpening","otherOpenings"],"additionalProperties":false},"base_add.life>whittle_miss.life":{"type":"object","properties":{"oneOpening":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life>whittle_miss.life","safe_ess.def"]},"minItems":2,"maxItems":2}},"otherOpenings":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life>whittle_miss.life","safe_ess.def"]},"minItems":2,"maxItems":2}}},"required":["oneOpening","otherOpenings"],"additionalProperties":false},"reveal.life":{"type":"object","properties":{"oneOpening":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["reveal.life","safe_ess.def"]},"minItems":2,"maxItems":2}},"otherOpenings":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["reveal.life","safe_ess.def"]},"minItems":2,"maxItems":2}}},"required":["oneOpening","otherOpenings"],"additionalProperties":false}},"required":["first_chaos.life","base_add.life","base_add.life>whittle_miss.life","reveal.life"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"first_chaos.life":{"oneOpening":[["first_chaos.life","safe_ess.def"]],"otherOpenings":[]},"base_add.life":{"oneOpening":[["base_add.life","safe_ess.def"]],"otherOpenings":[]},"base_add.life>whittle_miss.life":{"oneOpening":[["base_add.life>whittle_miss.life","safe_ess.def"]],"otherOpenings":[]},"reveal.life":{"oneOpening":[["reveal.life","safe_ess.def"]],"otherOpenings":[]}}
```

누락 ["safe_ess.def base_add.life","safe_ess.def base_add.life whittle_miss.life","safe_ess.def reveal.life"]

### 요청 19 · N · another

문안 P6 · 입력 역순 · 회수 7/7 · 거절 1 · 중복 0 · 입력/출력 토큰 1201/160.

전송 JSON은 요청 17과 같다.

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"reveal.life":{"type":"object","properties":{"oneOpening":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def","reveal.life"]},"minItems":2,"maxItems":2}},"otherOpenings":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def","reveal.life"]},"minItems":2,"maxItems":2}}},"required":["oneOpening","otherOpenings"],"additionalProperties":false},"base_add.life>whittle_miss.life":{"type":"object","properties":{"oneOpening":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def","base_add.life>whittle_miss.life"]},"minItems":2,"maxItems":2}},"otherOpenings":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def","base_add.life>whittle_miss.life"]},"minItems":2,"maxItems":2}}},"required":["oneOpening","otherOpenings"],"additionalProperties":false},"base_add.life":{"type":"object","properties":{"oneOpening":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def","base_add.life"]},"minItems":2,"maxItems":2}},"otherOpenings":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def","base_add.life"]},"minItems":2,"maxItems":2}}},"required":["oneOpening","otherOpenings"],"additionalProperties":false},"first_chaos.life":{"type":"object","properties":{"oneOpening":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def","first_chaos.life"]},"minItems":2,"maxItems":2}},"otherOpenings":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def","first_chaos.life"]},"minItems":2,"maxItems":2}}},"required":["oneOpening","otherOpenings"],"additionalProperties":false}},"required":["reveal.life","base_add.life>whittle_miss.life","base_add.life","first_chaos.life"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"reveal.life":{"oneOpening":[["reveal.life","safe_ess.def"]],"otherOpenings":[["safe_ess.def","reveal.life"]]},"base_add.life>whittle_miss.life":{"oneOpening":[["base_add.life>whittle_miss.life","safe_ess.def"]],"otherOpenings":[["safe_ess.def","base_add.life>whittle_miss.life"]]},"base_add.life":{"oneOpening":[["base_add.life","safe_ess.def"]],"otherOpenings":[["safe_ess.def","base_add.life"]]},"first_chaos.life":{"oneOpening":[["first_chaos.life","safe_ess.def"]],"otherOpenings":[["safe_ess.def","first_chaos.life"]]}}
```

<details>
<summary>거절된 후보와 사유</summary>

```json
[[["safe_ess.def","first_chaos.life"],"opening acquisition only"]]
```

</details>

### 요청 20 · N · another

문안 P6 · 입력 역순 · 회수 7/7 · 거절 1 · 중복 0 · 입력/출력 토큰 1201/160.

전송 JSON은 요청 17과 같다.

응답 스키마은 요청 19과 같다.

실제 응답

```json
{"reveal.life":{"oneOpening":[["reveal.life","safe_ess.def"]],"otherOpenings":[["safe_ess.def","reveal.life"]]},"base_add.life>whittle_miss.life":{"oneOpening":[["base_add.life>whittle_miss.life","safe_ess.def"]],"otherOpenings":[["safe_ess.def","base_add.life>whittle_miss.life"]]},"base_add.life":{"oneOpening":[["base_add.life","safe_ess.def"]],"otherOpenings":[["safe_ess.def","base_add.life"]]},"first_chaos.life":{"oneOpening":[["first_chaos.life","safe_ess.def"]],"otherOpenings":[["safe_ess.def","first_chaos.life"]]}}
```

<details>
<summary>거절된 후보와 사유</summary>

```json
[[["safe_ess.def","first_chaos.life"],"opening acquisition only"]]
```

</details>

### 요청 21 · W · baseline

문안 P7 · 기본 입력 · 회수 6/6 · 거절 0 · 중복 0 · 입력/출력 토큰 826/146.

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

실제 응답

```json
{"safe_alloy.mana_skill":[["safe_alloy.mana_skill","cap_up.item","safe_ess.skill","base_add.speed"],["safe_alloy.mana_skill","cap_up.item","base_add.speed","safe_ess.skill"]],"safe_ess.skill":[["safe_ess.skill","cap_up.item","safe_alloy.mana_skill","base_add.speed"],["safe_ess.skill","cap_up.item","base_add.speed","safe_alloy.mana_skill"]],"base_add.speed":[["base_add.speed","safe_alloy.mana_skill","cap_up.item","safe_ess.skill"],["base_add.speed","safe_ess.skill","cap_up.item","safe_alloy.mana_skill"]]}
```

### 요청 22 · H2 · warm

문안 P8 · 기본 입력 · 회수 0/2 · 거절 4 · 중복 0 · 입력/출력 토큰 691/89.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"needOnePerRow":{"skill":["safe_ess.skill"],"speed":["base_add.speed"]},"strategies":{"cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare"},"rules":[],"slots":{"craftedUsed":1,"limitNow":1,"additionalCraftedNeededAtLeast":1,"expandBy":1,"desecratedUsed":0},"lanes":{"any@skill":{"firstAcquisition":"skill","allowedByGoal":{"skill":["safe_ess.skill","cap_up.item>safe_ess.skill"],"speed":["base_add.speed","cap_up.item>base_add.speed"]}},"any@speed":{"firstAcquisition":"speed","allowedByGoal":{"skill":["safe_ess.skill","cap_up.item>safe_ess.skill"],"speed":["base_add.speed","cap_up.item>base_add.speed"]}}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"any@skill":{"type":"array","items":{"type":"object","properties":{"prepare":{"type":"string","enum":["none","cap_up.item"]},"steps":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","cap_up.item>safe_ess.skill","base_add.speed","cap_up.item>base_add.speed"]},"minItems":2,"maxItems":2}},"required":["prepare","steps"],"additionalProperties":false}},"any@speed":{"type":"array","items":{"type":"object","properties":{"prepare":{"type":"string","enum":["none","cap_up.item"]},"steps":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","cap_up.item>safe_ess.skill","base_add.speed","cap_up.item>base_add.speed"]},"minItems":2,"maxItems":2}},"required":["prepare","steps"],"additionalProperties":false}}},"required":["any@skill","any@speed"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"any@skill":[{"prepare":"cap_up.item","steps":["cap_up.item>safe_ess.skill","base_add.speed"]},{"prepare":"none","steps":["safe_ess.skill","base_add.speed"]}],"any@speed":[{"prepare":"cap_up.item","steps":["cap_up.item>base_add.speed","safe_ess.skill"]},{"prepare":"none","steps":["base_add.speed","safe_ess.skill"]}]}
```

누락 ["base_add.speed safe_ess.skill","safe_ess.skill base_add.speed"]

<details>
<summary>거절된 후보와 사유</summary>

```json
[[["cap_up.item","cap_up.item","safe_ess.skill","base_add.speed"],"no socket for capacity"],[["safe_ess.skill","base_add.speed"],"Crafted capacity exhausted"],[["cap_up.item","cap_up.item","base_add.speed","safe_ess.skill"],"no socket for capacity"],[["base_add.speed","safe_ess.skill"],"Crafted capacity exhausted"]]
```

</details>

### 요청 23 · Q2 · warm

문안 P8 · 기본 입력 · 회수 0/2 · 거절 4 · 중복 0 · 입력/출력 토큰 726/102.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"]}},"needOnePerRow":{"mana_skill":["safe_alloy.mana_skill"],"skill":["safe_ess.skill"]},"strategies":{"safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요","cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item."},"rules":[],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":2,"expandBy":1,"desecratedUsed":0},"lanes":{"any@mana_skill":{"firstAcquisition":"mana_skill","allowedByGoal":{"mana_skill":["safe_alloy.mana_skill","cap_up.item>safe_alloy.mana_skill"],"skill":["safe_ess.skill","cap_up.item>safe_ess.skill"]}},"any@skill":{"firstAcquisition":"skill","allowedByGoal":{"mana_skill":["safe_alloy.mana_skill","cap_up.item>safe_alloy.mana_skill"],"skill":["safe_ess.skill","cap_up.item>safe_ess.skill"]}}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"any@mana_skill":{"type":"array","items":{"type":"object","properties":{"prepare":{"type":"string","enum":["none","cap_up.item"]},"steps":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","cap_up.item>safe_alloy.mana_skill","safe_ess.skill","cap_up.item>safe_ess.skill"]},"minItems":2,"maxItems":2}},"required":["prepare","steps"],"additionalProperties":false}},"any@skill":{"type":"array","items":{"type":"object","properties":{"prepare":{"type":"string","enum":["none","cap_up.item"]},"steps":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","cap_up.item>safe_alloy.mana_skill","safe_ess.skill","cap_up.item>safe_ess.skill"]},"minItems":2,"maxItems":2}},"required":["prepare","steps"],"additionalProperties":false}}},"required":["any@mana_skill","any@skill"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"any@mana_skill":[{"prepare":"cap_up.item","steps":["cap_up.item>safe_alloy.mana_skill","safe_ess.skill"]},{"prepare":"none","steps":["safe_alloy.mana_skill","safe_ess.skill"]}],"any@skill":[{"prepare":"cap_up.item","steps":["cap_up.item>safe_ess.skill","safe_alloy.mana_skill"]},{"prepare":"none","steps":["safe_ess.skill","safe_alloy.mana_skill"]}]}
```

누락 ["safe_alloy.mana_skill safe_ess.skill","safe_ess.skill safe_alloy.mana_skill"]

<details>
<summary>거절된 후보와 사유</summary>

```json
[[["cap_up.item","cap_up.item","safe_alloy.mana_skill","safe_ess.skill"],"no socket for capacity"],[["safe_alloy.mana_skill","safe_ess.skill"],"Crafted capacity exhausted"],[["cap_up.item","cap_up.item","safe_ess.skill","safe_alloy.mana_skill"],"no socket for capacity"],[["safe_ess.skill","safe_alloy.mana_skill"],"Crafted capacity exhausted"]]
```

</details>

### 요청 24 · T1 · warm

문안 P8 · 기본 입력 · 회수 1/1 · 거절 0 · 중복 0 · 입력/출력 토큰 429/16.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"speed":["base"]}},"needOnePerRow":{"speed":["base_add.speed"]},"strategies":{"base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare"},"rules":[],"slots":{"craftedUsed":1,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":0},"lanes":{"any":{"firstAcquisition":null,"allowedByGoal":{"speed":["base_add.speed"]}}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"any":{"type":"array","items":{"type":"object","properties":{"prepare":{"type":"string","enum":["none"]},"steps":{"type":"array","items":{"type":"string","enum":["base_add.speed"]},"minItems":1,"maxItems":1}},"required":["prepare","steps"],"additionalProperties":false}}},"required":["any"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"any":[{"prepare":"none","steps":["base_add.speed"]}]}
```

### 요청 25 · T2 · warm

문안 P8 · 기본 입력 · 회수 0/2 · 거절 2 · 중복 0 · 입력/출력 토큰 597/45.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]},"craftedLimit":2,"capacitySocket":0},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"needOnePerRow":{"skill":["safe_ess.skill"],"speed":["base_add.speed"]},"strategies":{"safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare"},"rules":["시작 장비의 Crafted 한도 확장은 이미 적용되어 있다."],"slots":{"craftedUsed":1,"limitNow":2,"additionalCraftedNeededAtLeast":1,"expandBy":0,"desecratedUsed":0},"lanes":{"any@skill":{"firstAcquisition":"skill","allowedByGoal":{"skill":["safe_ess.skill"],"speed":["base_add.speed"]}},"any@speed":{"firstAcquisition":"speed","allowedByGoal":{"skill":["safe_ess.skill"],"speed":["base_add.speed"]}}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"any@skill":{"type":"array","items":{"type":"object","properties":{"prepare":{"type":"string","enum":["none"]},"steps":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","base_add.speed"]},"minItems":2,"maxItems":2}},"required":["prepare","steps"],"additionalProperties":false}},"any@speed":{"type":"array","items":{"type":"object","properties":{"prepare":{"type":"string","enum":["none"]},"steps":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","base_add.speed"]},"minItems":2,"maxItems":2}},"required":["prepare","steps"],"additionalProperties":false}}},"required":["any@skill","any@speed"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"any@skill":[{"prepare":"none","steps":["safe_ess.skill","safe_ess.skill"]}],"any@speed":[{"prepare":"none","steps":["base_add.speed","base_add.speed"]}]}
```

누락 ["base_add.speed safe_ess.skill","safe_ess.skill base_add.speed"]

<details>
<summary>거절된 후보와 사유</summary>

```json
[[["safe_ess.skill","safe_ess.skill"],"target already covered"],[["base_add.speed","base_add.speed"],"target already covered"]]
```

</details>

### 요청 26 · H3 · warm

문안 P8 · 기본 입력 · 음성 대조 · 거절 2 · 중복 0 · 입력/출력 토큰 616/52.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"]}},"needOnePerRow":{"mana_skill":["safe_alloy.mana_skill"],"skill":["safe_ess.skill"]},"strategies":{"safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요"},"rules":["Crafted 한도를 올릴 수단이 없다."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":2,"expandBy":0,"desecratedUsed":0},"lanes":{"any@mana_skill":{"firstAcquisition":"mana_skill","allowedByGoal":{"mana_skill":["safe_alloy.mana_skill"],"skill":["safe_ess.skill"]}},"any@skill":{"firstAcquisition":"skill","allowedByGoal":{"mana_skill":["safe_alloy.mana_skill"],"skill":["safe_ess.skill"]}}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"any@mana_skill":{"type":"array","items":{"type":"object","properties":{"prepare":{"type":"string","enum":["none"]},"steps":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill"]},"minItems":2,"maxItems":2}},"required":["prepare","steps"],"additionalProperties":false}},"any@skill":{"type":"array","items":{"type":"object","properties":{"prepare":{"type":"string","enum":["none"]},"steps":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill"]},"minItems":2,"maxItems":2}},"required":["prepare","steps"],"additionalProperties":false}}},"required":["any@mana_skill","any@skill"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"any@mana_skill":[{"prepare":"none","steps":["safe_alloy.mana_skill","safe_ess.skill"]}],"any@skill":[{"prepare":"none","steps":["safe_ess.skill","safe_alloy.mana_skill"]}]}
```

<details>
<summary>거절된 후보와 사유</summary>

```json
[[["safe_alloy.mana_skill","safe_ess.skill"],"Crafted capacity exhausted"],[["safe_ess.skill","safe_alloy.mana_skill"],"Crafted capacity exhausted"]]
```

</details>

### 요청 27 · Q1 · warm

문안 P8 · 기본 입력 · 음성 대조 · 거절 2 · 중복 0 · 입력/출력 토큰 582/45.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"needOnePerRow":{"skill":["safe_ess.skill"],"speed":["base_add.speed"]},"strategies":{"safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare"},"rules":[],"slots":{"craftedUsed":1,"limitNow":1,"additionalCraftedNeededAtLeast":1,"expandBy":0,"desecratedUsed":0},"lanes":{"any@skill":{"firstAcquisition":"skill","allowedByGoal":{"skill":["safe_ess.skill"],"speed":["base_add.speed"]}},"any@speed":{"firstAcquisition":"speed","allowedByGoal":{"skill":["safe_ess.skill"],"speed":["base_add.speed"]}}}}
```

</details>

응답 스키마은 요청 25과 같다.

실제 응답

```json
{"any@skill":[{"prepare":"none","steps":["safe_ess.skill","base_add.speed"]}],"any@speed":[{"prepare":"none","steps":["base_add.speed","safe_ess.skill"]}]}
```

<details>
<summary>거절된 후보와 사유</summary>

```json
[[["safe_ess.skill","base_add.speed"],"Crafted capacity exhausted"],[["base_add.speed","safe_ess.skill"],"Crafted capacity exhausted"]]
```

</details>

### 요청 28 · H2 · oneSetup

문안 P8 · 기본 입력 · 회수 1/2 · 거절 0 · 중복 3 · 입력/출력 토큰 784/89.

전송 JSON은 요청 22과 같다.

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"any@skill":{"type":"array","items":{"anyOf":[{"type":"object","properties":{"prepare":{"type":"string","enum":["cap_up.item"]},"steps":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","base_add.speed"]},"minItems":2,"maxItems":2}},"required":["prepare","steps"],"additionalProperties":false},{"type":"object","properties":{"prepare":{"type":"string","enum":["none"]},"steps":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","cap_up.item>safe_ess.skill","base_add.speed","cap_up.item>base_add.speed"]},"minItems":2,"maxItems":2}},"required":["prepare","steps"],"additionalProperties":false}]}},"any@speed":{"type":"array","items":{"anyOf":[{"type":"object","properties":{"prepare":{"type":"string","enum":["cap_up.item"]},"steps":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","base_add.speed"]},"minItems":2,"maxItems":2}},"required":["prepare","steps"],"additionalProperties":false},{"type":"object","properties":{"prepare":{"type":"string","enum":["none"]},"steps":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","cap_up.item>safe_ess.skill","base_add.speed","cap_up.item>base_add.speed"]},"minItems":2,"maxItems":2}},"required":["prepare","steps"],"additionalProperties":false}]}}},"required":["any@skill","any@speed"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"any@skill":[{"prepare":"cap_up.item","steps":["safe_ess.skill","base_add.speed"]},{"prepare":"none","steps":["cap_up.item>safe_ess.skill","base_add.speed"]}],"any@speed":[{"prepare":"cap_up.item","steps":["safe_ess.skill","base_add.speed"]},{"prepare":"none","steps":["cap_up.item>safe_ess.skill","base_add.speed"]}]}
```

누락 ["base_add.speed safe_ess.skill"]

### 요청 29 · Q2 · oneSetup

문안 P8 · 기본 입력 · 회수 0/2 · 거절 4 · 중복 0 · 입력/출력 토큰 825/102.

전송 JSON은 요청 23과 같다.

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"any@mana_skill":{"type":"array","items":{"anyOf":[{"type":"object","properties":{"prepare":{"type":"string","enum":["cap_up.item"]},"steps":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill"]},"minItems":2,"maxItems":2}},"required":["prepare","steps"],"additionalProperties":false},{"type":"object","properties":{"prepare":{"type":"string","enum":["none"]},"steps":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","cap_up.item>safe_alloy.mana_skill","safe_ess.skill","cap_up.item>safe_ess.skill"]},"minItems":2,"maxItems":2}},"required":["prepare","steps"],"additionalProperties":false}]}},"any@skill":{"type":"array","items":{"anyOf":[{"type":"object","properties":{"prepare":{"type":"string","enum":["cap_up.item"]},"steps":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill"]},"minItems":2,"maxItems":2}},"required":["prepare","steps"],"additionalProperties":false},{"type":"object","properties":{"prepare":{"type":"string","enum":["none"]},"steps":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","cap_up.item>safe_alloy.mana_skill","safe_ess.skill","cap_up.item>safe_ess.skill"]},"minItems":2,"maxItems":2}},"required":["prepare","steps"],"additionalProperties":false}]}}},"required":["any@mana_skill","any@skill"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"any@mana_skill":[{"prepare":"cap_up.item","steps":["safe_alloy.mana_skill","safe_alloy.mana_skill"]},{"prepare":"none","steps":["cap_up.item>safe_alloy.mana_skill","safe_alloy.mana_skill"]}],"any@skill":[{"prepare":"cap_up.item","steps":["safe_ess.skill","safe_ess.skill"]},{"prepare":"none","steps":["cap_up.item>safe_ess.skill","safe_ess.skill"]}]}
```

누락 ["safe_alloy.mana_skill safe_ess.skill","safe_ess.skill safe_alloy.mana_skill"]

<details>
<summary>거절된 후보와 사유</summary>

```json
[[["cap_up.item","safe_alloy.mana_skill","safe_alloy.mana_skill"],"target already covered"],[["cap_up.item","safe_alloy.mana_skill","safe_alloy.mana_skill"],"target already covered"],[["cap_up.item","safe_ess.skill","safe_ess.skill"],"target already covered"],[["cap_up.item","safe_ess.skill","safe_ess.skill"],"target already covered"]]
```

</details>

### 요청 30 · T2 · baseline

문안 P1 · 기본 입력 · 회수 2/2 · 거절 0 · 중복 2 · 입력/출력 토큰 668/52.

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

실제 응답

```json
{"safe_ess.skill":[["safe_ess.skill","base_add.speed"],["safe_ess.skill","base_add.speed"]],"base_add.speed":[["base_add.speed","safe_ess.skill"],["base_add.speed","safe_ess.skill"]]}
```

### 요청 31 · G · baseline

문안 P1 · 기본 입력 · 회수 2/2 · 거절 0 · 중복 0 · 입력/출력 토큰 684/35.

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

실제 응답

```json
{"upgrade_ess.life":[["upgrade_ess.life","safe_alloy.pen"]],"regal_base.life":[["regal_base.life","safe_alloy.pen"]]}
```

### 요청 32 · G · another

문안 P6 · 기본 입력 · 회수 2/2 · 거절 0 · 중복 0 · 입력/출력 토큰 679/34.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Stocky Mitts","rarity":"magic","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"filler"}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body"],"pen":["Prismatic Alloy"]},"suffix":{}},"needOnePerRow":{"life":["upgrade_ess.life","regal_base.life"],"pen":["safe_alloy.pen"]},"strategies":{"upgrade_ess":"등급 상승용 에센스로 목표 일반 옵션 획득. Magic → Rare","regal_base":"제왕으로 Magic을 Rare로 올리며 해당 일반 옵션이 뜬 결과 채택. Magic → Rare","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요"},"rules":[],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":1,"expandBy":0,"desecratedUsed":0},"lanes":{"any":{"sourceScope":"any","allowedByGoal":{"life":["upgrade_ess.life","regal_base.life"],"pen":["safe_alloy.pen"]}}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"any":{"type":"object","properties":{"oneOpening":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["upgrade_ess.life","regal_base.life","safe_alloy.pen"]},"minItems":2,"maxItems":2}},"otherOpenings":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["upgrade_ess.life","regal_base.life","safe_alloy.pen"]},"minItems":2,"maxItems":2}}},"required":["oneOpening","otherOpenings"],"additionalProperties":false}},"required":["any"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"any":{"oneOpening":[["upgrade_ess.life","safe_alloy.pen"]],"otherOpenings":[["regal_base.life","safe_alloy.pen"]]}}
```

### 요청 33 · G · another

문안 P6 · 입력 역순 · 회수 2/2 · 거절 0 · 중복 0 · 입력/출력 토큰 679/34.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Stocky Mitts","rarity":"magic","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"filler"}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body"],"pen":["Prismatic Alloy"]},"suffix":{}},"needOnePerRow":{"pen":["safe_alloy.pen"],"life":["upgrade_ess.life","regal_base.life"]},"strategies":{"upgrade_ess":"등급 상승용 에센스로 목표 일반 옵션 획득. Magic → Rare","regal_base":"제왕으로 Magic을 Rare로 올리며 해당 일반 옵션이 뜬 결과 채택. Magic → Rare","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요"},"rules":[],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":1,"expandBy":0,"desecratedUsed":0},"lanes":{"any":{"sourceScope":"any","allowedByGoal":{"pen":["safe_alloy.pen"],"life":["regal_base.life","upgrade_ess.life"]}}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"any":{"type":"object","properties":{"oneOpening":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy.pen","regal_base.life","upgrade_ess.life"]},"minItems":2,"maxItems":2}},"otherOpenings":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy.pen","regal_base.life","upgrade_ess.life"]},"minItems":2,"maxItems":2}}},"required":["oneOpening","otherOpenings"],"additionalProperties":false}},"required":["any"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"any":{"oneOpening":[["regal_base.life","safe_alloy.pen"]],"otherOpenings":[["upgrade_ess.life","safe_alloy.pen"]]}}
```

### 요청 34 · GR · another

문안 P6 · 기본 입력 · 회수 1/1 · 거절 0 · 중복 0 · 입력/출력 토큰 607/18.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Stocky Mitts","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life"}],"suffix":[{"id":"filler"}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body"],"pen":["Prismatic Alloy"]},"suffix":{}},"needOnePerRow":{"pen":["safe_alloy.pen"]},"strategies":{"safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요"},"rules":["이미 Rare이며 생명력은 보유한다. 남은 목표는 관통이다."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":1,"expandBy":0,"desecratedUsed":0},"lanes":{"any":{"sourceScope":"any","allowedByGoal":{"pen":["safe_alloy.pen"]}}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"any":{"type":"object","properties":{"oneOpening":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy.pen"]},"minItems":1,"maxItems":1}},"otherOpenings":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy.pen"]},"minItems":1,"maxItems":1}}},"required":["oneOpening","otherOpenings"],"additionalProperties":false}},"required":["any"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"any":{"oneOpening":[["safe_alloy.pen"]],"otherOpenings":[]}}
```

### 요청 35 · H4 · baseline

문안 P1 · 기본 입력 · 회수 2/2 · 거절 0 · 중복 0 · 입력/출력 토큰 731/29.

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

실제 응답

```json
{"flesh.item":[["flesh.item","divine.item"]],"divine.item":[["divine.item","flesh.item"]]}
```

### 요청 36 · H4 · another

문안 P6 · 기본 입력 · 회수 2/2 · 거절 0 · 중복 2 · 입력/출력 토큰 708/48.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","raw":99}],"suffix":[{"id":"str","raw":7}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"needOnePerRow":{"Flesh20":["flesh.item"],"values":["divine.item"]},"strategies":{"flesh":"Flesh 품질 20 적용. 기존 수치 유지. 대상 item","divine":"두 옵션 수치를 현재 티어 안에서 함께 굴린다. 변경 가능 아이템, 대상 item"},"rules":["두 옵션은 보유. Flesh20과 원래 굴림 99·8이면 합 127로 완성. 현재 품질 0, 99·7은 품질만 올리면 126으로 미달. 품질과 재굴림은 어느 순서도 가능."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":0},"lanes":{"any":{"sourceScope":"any","allowedByGoal":{"Flesh20":["flesh.item"],"values":["divine.item"]}}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"any":{"type":"object","properties":{"oneOpening":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["flesh.item","divine.item"]},"minItems":2,"maxItems":2}},"otherOpenings":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["flesh.item","divine.item"]},"minItems":2,"maxItems":2}}},"required":["oneOpening","otherOpenings"],"additionalProperties":false}},"required":["any"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"any":{"oneOpening":[["flesh.item","divine.item"],["divine.item","flesh.item"]],"otherOpenings":[["flesh.item","divine.item"],["divine.item","flesh.item"]]}}
```

### 요청 37 · H4 · another

문안 P6 · 입력 역순 · 회수 2/2 · 거절 0 · 중복 0 · 입력/출력 토큰 708/30.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","raw":99}],"suffix":[{"id":"str","raw":7}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"needOnePerRow":{"values":["divine.item"],"Flesh20":["flesh.item"]},"strategies":{"flesh":"Flesh 품질 20 적용. 기존 수치 유지. 대상 item","divine":"두 옵션 수치를 현재 티어 안에서 함께 굴린다. 변경 가능 아이템, 대상 item"},"rules":["두 옵션은 보유. Flesh20과 원래 굴림 99·8이면 합 127로 완성. 현재 품질 0, 99·7은 품질만 올리면 126으로 미달. 품질과 재굴림은 어느 순서도 가능."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":0},"lanes":{"any":{"sourceScope":"any","allowedByGoal":{"values":["divine.item"],"Flesh20":["flesh.item"]}}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"any":{"type":"object","properties":{"oneOpening":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.item","flesh.item"]},"minItems":2,"maxItems":2}},"otherOpenings":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.item","flesh.item"]},"minItems":2,"maxItems":2}}},"required":["oneOpening","otherOpenings"],"additionalProperties":false}},"required":["any"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"any":{"oneOpening":[["divine.item","flesh.item"]],"otherOpenings":[["flesh.item","divine.item"]]}}
```

### 요청 38 · HF · another

문안 P6 · 기본 입력 · 회수 1/1 · 거절 0 · 중복 0 · 입력/출력 토큰 622/17.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","raw":99}],"suffix":[{"id":"str","raw":8}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"needOnePerRow":{"Flesh20":["flesh.item"]},"strategies":{"flesh":"Flesh 품질 20 적용. 기존 수치 유지. 대상 item"},"rules":["현재 원래 굴림 99·8, 품질 0. Flesh20이면 합 127이다."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":0},"lanes":{"any":{"sourceScope":"any","allowedByGoal":{"Flesh20":["flesh.item"]}}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"any":{"type":"object","properties":{"oneOpening":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["flesh.item"]},"minItems":1,"maxItems":1}},"otherOpenings":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["flesh.item"]},"minItems":1,"maxItems":1}}},"required":["oneOpening","otherOpenings"],"additionalProperties":false}},"required":["any"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"any":{"oneOpening":[["flesh.item"]],"otherOpenings":[]}}
```

### 요청 39 · HD · another

문안 P6 · 기본 입력 · 회수 1/1 · 거절 0 · 중복 0 · 입력/출력 토큰 640/17.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","raw":99}],"suffix":[{"id":"str","raw":7}]},"craftedLimit":1,"capacitySocket":0,"quality":{"type":"Flesh","value":20}},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"needOnePerRow":{"values":["divine.item"]},"strategies":{"divine":"두 옵션 수치를 현재 티어 안에서 함께 굴린다. 변경 가능 아이템, 대상 item"},"rules":["Flesh20 적용 상태이고 현재 합은 126. 원래 굴림 99·8이면 합 127이다."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":0},"lanes":{"any":{"sourceScope":"any","allowedByGoal":{"values":["divine.item"]}}}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"any":{"type":"object","properties":{"oneOpening":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.item"]},"minItems":1,"maxItems":1}},"otherOpenings":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.item"]},"minItems":1,"maxItems":1}}},"required":["oneOpening","otherOpenings"],"additionalProperties":false}},"required":["any"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"any":{"oneOpening":[["divine.item"]],"otherOpenings":[]}}
```

### 요청 40 · Q4 · terminal

문안 P9 · 기본 입력 · 회수 1/1 · 거절 0 · 중복 0 · 입력/출력 토큰 368/5.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","value":90},{"id":"def","crafted":true}],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"needOnePerRow":{},"strategies":{},"rules":[],"slots":{"craftedUsed":1,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":0},"strategyMenu":[]}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"recipes":{"type":"array","items":{"type":"array","items":{"type":"string"}}}},"required":["recipes"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"recipes":[[]]}
```

### 요청 41 · H1 · terminal

문안 P9 · 기본 입력 · 회수 1/1 · 거절 0 · 중복 0 · 입력/출력 토큰 406/10.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","value":90}],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"needOnePerRow":{"def":["safe_ess.def"]},"strategies":{"safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요"},"rules":[],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":1,"expandBy":0,"desecratedUsed":0},"strategyMenu":["safe_ess.def"]}
```

</details>

응답 스키마은 요청 40과 같다.

실제 응답

```json
{"recipes":[["safe_ess.def"]]}
```

### 요청 42 · L89 · terminal

문안 P9 · 기본 입력 · 음성 대조 · 거절 1 · 중복 0 · 입력/출력 토큰 374/5.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","value":89}],"suffix":[]},"craftedLimit":1,"capacitySocket":0,"sanctified":true},"targets":{"prefix":{"life":["base"]},"suffix":{}},"needOnePerRow":{"values":["divine.item"]},"strategies":{"divine":"두 옵션 수치를 현재 티어 안에서 함께 굴린다. 변경 가능 아이템, 대상 item"},"rules":["이미 축성된 고정 시작 아이템은 변경할 수 없다. 현재 생명력 정확히 90이 목표다."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":0},"strategyMenu":["divine.item"]}
```

</details>

응답 스키마은 요청 40과 같다.

실제 응답

```json
{"recipes":[[]]}
```

<details>
<summary>거절된 후보와 사유</summary>

```json
[[[],"locked or current value below target"]]
```

</details>

### 요청 43 · L90 · terminal

문안 P9 · 기본 입력 · 회수 1/1 · 거절 0 · 중복 0 · 입력/출력 토큰 369/5.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","value":90}],"suffix":[]},"craftedLimit":1,"capacitySocket":0,"sanctified":true},"targets":{"prefix":{"life":["base"]},"suffix":{}},"needOnePerRow":{},"strategies":{"divine":"두 옵션 수치를 현재 티어 안에서 함께 굴린다. 변경 가능 아이템, 대상 item"},"rules":["이미 축성된 고정 시작 아이템은 변경할 수 없다. 현재 생명력 정확히 90이 목표다."],"slots":{"craftedUsed":0,"limitNow":1,"additionalCraftedNeededAtLeast":0,"expandBy":0,"desecratedUsed":0},"strategyMenu":["divine.item"]}
```

</details>

응답 스키마은 요청 40과 같다.

실제 응답

```json
{"recipes":[[]]}
```

### 요청 44 · Q4 · terminal

문안 P9 · 기본 입력 · 회수 0/1 · 거절 1 · 중복 0 · 입력/출력 토큰 368/26.

전송 JSON은 요청 40과 같다.

응답 스키마은 요청 40과 같다.

실제 응답

```json
{"recipes":[["Greater Essence of the Body","Perfect Essence of Enhancement","Preserved Collarbone","Altered Collarbone"]]}
```

누락 [""]

<details>
<summary>거절된 후보와 사유</summary>

```json
[[["Greater Essence of the Body","Perfect Essence of Enhancement","Preserved Collarbone","Altered Collarbone"],"unknown action"]]
```

</details>

### 요청 45 · Q4 · status

문안 P10 · 기본 입력 · 회수 1/1 · 거절 0 · 중복 0 · 입력/출력 토큰 525/12.

전송 JSON은 요청 40과 같다.

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"result":{"anyOf":[{"type":"object","properties":{"status":{"type":"string","enum":["done"]},"recipes":{"type":"array","items":{"type":"array","items":{"type":"string"},"maxItems":0},"minItems":1,"maxItems":1}},"required":["status","recipes"],"additionalProperties":false},{"type":"object","properties":{"status":{"type":"string","enum":["noCandidate"]},"recipes":{"type":"array","items":{"type":"array","items":{"type":"string"}},"maxItems":0}},"required":["status","recipes"],"additionalProperties":false},{"type":"object","properties":{"status":{"type":"string","enum":["routes"]},"recipes":{"type":"array","items":{"type":"array","items":{"type":"string"},"minItems":1},"minItems":1}},"required":["status","recipes"],"additionalProperties":false}]}},"required":["result"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"result":{"status":"done","recipes":[[]]}}
```

### 요청 46 · L89 · status

문안 P10 · 기본 입력 · 음성 대조 · 거절 1 · 중복 0 · 입력/출력 토큰 531/12.

전송 JSON은 요청 42과 같다.

응답 스키마은 요청 45과 같다.

실제 응답

```json
{"result":{"status":"done","recipes":[[]]}}
```

<details>
<summary>거절된 후보와 사유</summary>

```json
[[[],"locked or current value below target"]]
```

</details>

### 요청 47 · L90 · status

문안 P10 · 기본 입력 · 회수 1/1 · 거절 0 · 중복 0 · 입력/출력 토큰 526/12.

전송 JSON은 요청 43과 같다.

응답 스키마은 요청 45과 같다.

실제 응답

```json
{"result":{"status":"done","recipes":[[]]}}
```

### 요청 48 · H1 · status

문안 P10 · 기본 입력 · 회수 1/1 · 거절 0 · 중복 0 · 입력/출력 토큰 563/16.

전송 JSON은 요청 41과 같다.

응답 스키마은 요청 45과 같다.

실제 응답

```json
{"result":{"status":"routes","recipes":[["safe_ess.def"]]}}
```

### 요청 49 · Q4 · status

문안 P10 · 기본 입력 · 회수 1/1 · 거절 0 · 중복 0 · 입력/출력 토큰 525/12.

전송 JSON은 요청 40과 같다.

응답 스키마은 요청 45과 같다.

실제 응답

```json
{"result":{"status":"done","recipes":[[]]}}
```

### 요청 50 · TM90 · tierList

문안 P11 · 기본 입력 · 회수 4/4 · 거절 0 · 중복 0 · 입력/출력 토큰 601/44.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","ilvl":82,"rarity":"magic","mods":{"prefix":[],"suffix":[{"id":"filler"}]}},"targets":{"prefix":{"life":{"minimum":90,"tiers":[{"tier":1,"mod":"amulet:prefix:athlete-s","roll":[120,149],"sources":{"base":{"weight":1000},"desecrated":["Preserved_Collarbone","Ancient_Collarbone","Altered_Collarbone"]}},{"tier":2,"mod":"amulet:prefix:virile","roll":[100,119],"sources":{"base":{"weight":1000},"desecrated":["Preserved_Collarbone","Ancient_Collarbone","Altered_Collarbone"]}},{"tier":3,"mod":"amulet:prefix:rotund","roll":[85,99],"sources":{"base":{"weight":1000},"essence":["GREATER_ESSENCE_BODY"],"desecrated":["Preserved_Collarbone","Ancient_Collarbone","Altered_Collarbone"]}}]}},"suffix":{}},"strategies":{"regal_base.life":{"rarity":"magic","source":"base"},"upgrade_ess_body.life":{"rarity":"magic","source":"GREATER_ESSENCE_BODY"},"base_add.life":{"rarity":"rare","source":"base"},"reveal_Preserved_Collarbone.life":{"rarity":"rare","source":"Preserved_Collarbone"},"reveal_Ancient_Collarbone.life":{"rarity":"rare","source":"Ancient_Collarbone"},"reveal_Altered_Collarbone.life":{"rarity":"rare","source":"Altered_Collarbone"}},"allowed":["regal_base.life","upgrade_ess_body.life"],"rules":["한 획득 전략만 비교한다. 실제 생명력 굴림이 목표 이상일 때만 성공이다."]}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"recipes":{"type":"array","items":{"type":"object","properties":{"steps":{"type":"array","items":{"type":"string","enum":["regal_base.life","upgrade_ess_body.life"]}},"acceptTiers":{"type":"array","items":{"type":"string","enum":["T1","T2","T3"]}}},"required":["steps","acceptTiers"],"additionalProperties":false}}},"required":["recipes"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"recipes":[{"steps":["regal_base.life"],"acceptTiers":["T1","T2","T3"]},{"steps":["upgrade_ess_body.life"],"acceptTiers":["T3"]}]}
```

### 요청 51 · TM100 · tierList

문안 P11 · 기본 입력 · 회수 2/2 · 거절 2 · 중복 0 · 입력/출력 토큰 601/44.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","ilvl":82,"rarity":"magic","mods":{"prefix":[],"suffix":[{"id":"filler"}]}},"targets":{"prefix":{"life":{"minimum":100,"tiers":[{"tier":1,"mod":"amulet:prefix:athlete-s","roll":[120,149],"sources":{"base":{"weight":1000},"desecrated":["Preserved_Collarbone","Ancient_Collarbone","Altered_Collarbone"]}},{"tier":2,"mod":"amulet:prefix:virile","roll":[100,119],"sources":{"base":{"weight":1000},"desecrated":["Preserved_Collarbone","Ancient_Collarbone","Altered_Collarbone"]}},{"tier":3,"mod":"amulet:prefix:rotund","roll":[85,99],"sources":{"base":{"weight":1000},"essence":["GREATER_ESSENCE_BODY"],"desecrated":["Preserved_Collarbone","Ancient_Collarbone","Altered_Collarbone"]}}]}},"suffix":{}},"strategies":{"regal_base.life":{"rarity":"magic","source":"base"},"upgrade_ess_body.life":{"rarity":"magic","source":"GREATER_ESSENCE_BODY"},"base_add.life":{"rarity":"rare","source":"base"},"reveal_Preserved_Collarbone.life":{"rarity":"rare","source":"Preserved_Collarbone"},"reveal_Ancient_Collarbone.life":{"rarity":"rare","source":"Ancient_Collarbone"},"reveal_Altered_Collarbone.life":{"rarity":"rare","source":"Altered_Collarbone"}},"allowed":["regal_base.life","upgrade_ess_body.life"],"rules":["한 획득 전략만 비교한다. 실제 생명력 굴림이 목표 이상일 때만 성공이다."]}
```

</details>

응답 스키마은 요청 50과 같다.

실제 응답

```json
{"recipes":[{"steps":["regal_base.life"],"acceptTiers":["T1","T2","T3"]},{"steps":["upgrade_ess_body.life"],"acceptTiers":["T3"]}]}
```

<details>
<summary>거절된 후보와 사유</summary>

```json
[[["regal_base.life"],"ineligible T3"],[["upgrade_ess_body.life"],"ineligible T3"]]
```

</details>

### 요청 52 · TR90 · tierList

문안 P11 · 기본 입력 · 회수 12/12 · 거절 0 · 중복 0 · 입력/출력 토큰 645/106.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","ilvl":82,"rarity":"rare","mods":{"prefix":[],"suffix":[{"id":"filler"}]}},"targets":{"prefix":{"life":{"minimum":90,"tiers":[{"tier":1,"mod":"amulet:prefix:athlete-s","roll":[120,149],"sources":{"base":{"weight":1000},"desecrated":["Preserved_Collarbone","Ancient_Collarbone","Altered_Collarbone"]}},{"tier":2,"mod":"amulet:prefix:virile","roll":[100,119],"sources":{"base":{"weight":1000},"desecrated":["Preserved_Collarbone","Ancient_Collarbone","Altered_Collarbone"]}},{"tier":3,"mod":"amulet:prefix:rotund","roll":[85,99],"sources":{"base":{"weight":1000},"essence":["GREATER_ESSENCE_BODY"],"desecrated":["Preserved_Collarbone","Ancient_Collarbone","Altered_Collarbone"]}}]}},"suffix":{}},"strategies":{"regal_base.life":{"rarity":"magic","source":"base"},"upgrade_ess_body.life":{"rarity":"magic","source":"GREATER_ESSENCE_BODY"},"base_add.life":{"rarity":"rare","source":"base"},"reveal_Preserved_Collarbone.life":{"rarity":"rare","source":"Preserved_Collarbone"},"reveal_Ancient_Collarbone.life":{"rarity":"rare","source":"Ancient_Collarbone"},"reveal_Altered_Collarbone.life":{"rarity":"rare","source":"Altered_Collarbone"}},"allowed":["base_add.life","reveal_Preserved_Collarbone.life","reveal_Ancient_Collarbone.life","reveal_Altered_Collarbone.life"],"rules":["한 획득 전략만 비교한다. 실제 생명력 굴림이 목표 이상일 때만 성공이다."]}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"recipes":{"type":"array","items":{"type":"object","properties":{"steps":{"type":"array","items":{"type":"string","enum":["base_add.life","reveal_Preserved_Collarbone.life","reveal_Ancient_Collarbone.life","reveal_Altered_Collarbone.life"]}},"acceptTiers":{"type":"array","items":{"type":"string","enum":["T1","T2","T3"]}}},"required":["steps","acceptTiers"],"additionalProperties":false}}},"required":["recipes"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"recipes":[{"steps":["base_add.life"],"acceptTiers":["T1","T2","T3"]},{"steps":["reveal_Preserved_Collarbone.life"],"acceptTiers":["T1","T2","T3"]},{"steps":["reveal_Ancient_Collarbone.life"],"acceptTiers":["T1","T2","T3"]},{"steps":["reveal_Altered_Collarbone.life"],"acceptTiers":["T1","T2","T3"]}]}
```

### 요청 53 · TM100 · tierSlot

문안 P11 · 기본 입력 · 회수 2/2 · 거절 4 · 중복 2 · 입력/출력 토큰 677/94.

전송 JSON은 요청 51과 같다.

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"regal_base.life":{"type":"array","items":{"type":"object","properties":{"steps":{"type":"array","items":{"type":"string","enum":["regal_base.life","upgrade_ess_body.life"]}},"acceptTiers":{"type":"array","items":{"type":"string","enum":["T1","T2","T3"]}}},"required":["steps","acceptTiers"],"additionalProperties":false}},"upgrade_ess_body.life":{"type":"array","items":{"type":"object","properties":{"steps":{"type":"array","items":{"type":"string","enum":["regal_base.life","upgrade_ess_body.life"]}},"acceptTiers":{"type":"array","items":{"type":"string","enum":["T1","T2","T3"]}}},"required":["steps","acceptTiers"],"additionalProperties":false}}},"required":["regal_base.life","upgrade_ess_body.life"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"regal_base.life":[{"steps":["regal_base.life"],"acceptTiers":["T1","T2","T3"]},{"steps":["upgrade_ess_body.life"],"acceptTiers":["T3"]}],"upgrade_ess_body.life":[{"steps":["regal_base.life"],"acceptTiers":["T1","T2","T3"]},{"steps":["upgrade_ess_body.life"],"acceptTiers":["T3"]}]}
```

<details>
<summary>거절된 후보와 사유</summary>

```json
[[["regal_base.life"],"ineligible T3"],[["upgrade_ess_body.life"],"ineligible T3"],[["regal_base.life"],"ineligible T3"],[["upgrade_ess_body.life"],"ineligible T3"]]
```

</details>

### 요청 54 · TM90 · tierSlot

문안 P11 · 입력 역순 · 회수 2/4 · 거절 4 · 중복 0 · 입력/출력 토큰 677/145.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","ilvl":82,"rarity":"magic","mods":{"prefix":[],"suffix":[{"id":"filler"}]}},"targets":{"prefix":{"life":{"minimum":90,"tiers":[{"tier":3,"mod":"amulet:prefix:rotund","roll":[85,99],"sources":{"base":{"weight":1000},"essence":["GREATER_ESSENCE_BODY"],"desecrated":["Preserved_Collarbone","Ancient_Collarbone","Altered_Collarbone"]}},{"tier":2,"mod":"amulet:prefix:virile","roll":[100,119],"sources":{"base":{"weight":1000},"desecrated":["Preserved_Collarbone","Ancient_Collarbone","Altered_Collarbone"]}},{"tier":1,"mod":"amulet:prefix:athlete-s","roll":[120,149],"sources":{"base":{"weight":1000},"desecrated":["Preserved_Collarbone","Ancient_Collarbone","Altered_Collarbone"]}}]}},"suffix":{}},"strategies":{"regal_base.life":{"rarity":"magic","source":"base"},"upgrade_ess_body.life":{"rarity":"magic","source":"GREATER_ESSENCE_BODY"},"base_add.life":{"rarity":"rare","source":"base"},"reveal_Preserved_Collarbone.life":{"rarity":"rare","source":"Preserved_Collarbone"},"reveal_Ancient_Collarbone.life":{"rarity":"rare","source":"Ancient_Collarbone"},"reveal_Altered_Collarbone.life":{"rarity":"rare","source":"Altered_Collarbone"}},"allowed":["upgrade_ess_body.life","regal_base.life"],"rules":["한 획득 전략만 비교한다. 실제 생명력 굴림이 목표 이상일 때만 성공이다."]}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"upgrade_ess_body.life":{"type":"array","items":{"type":"object","properties":{"steps":{"type":"array","items":{"type":"string","enum":["upgrade_ess_body.life","regal_base.life"]}},"acceptTiers":{"type":"array","items":{"type":"string","enum":["T1","T2","T3"]}}},"required":["steps","acceptTiers"],"additionalProperties":false}},"regal_base.life":{"type":"array","items":{"type":"object","properties":{"steps":{"type":"array","items":{"type":"string","enum":["upgrade_ess_body.life","regal_base.life"]}},"acceptTiers":{"type":"array","items":{"type":"string","enum":["T1","T2","T3"]}}},"required":["steps","acceptTiers"],"additionalProperties":false}}},"required":["upgrade_ess_body.life","regal_base.life"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"upgrade_ess_body.life":[{"steps":["upgrade_ess_body.life"],"acceptTiers":["T3"]},{"steps":["upgrade_ess_body.life","regal_base.life"],"acceptTiers":["T2"]},{"steps":["upgrade_ess_body.life","regal_base.life","regal_base.life"],"acceptTiers":["T1"]}],"regal_base.life":[{"steps":["regal_base.life"],"acceptTiers":["T3"]},{"steps":["regal_base.life","regal_base.life"],"acceptTiers":["T2"]},{"steps":["regal_base.life","regal_base.life","regal_base.life"],"acceptTiers":["T1"]}]}
```

누락 ["regal_base.life @T1","regal_base.life @T2"]

<details>
<summary>거절된 후보와 사유</summary>

```json
[[["upgrade_ess_body.life","regal_base.life"],"missing or incomplete tier route"],[["upgrade_ess_body.life","regal_base.life","regal_base.life"],"missing or incomplete tier route"],[["regal_base.life","regal_base.life"],"missing or incomplete tier route"],[["regal_base.life","regal_base.life","regal_base.life"],"missing or incomplete tier route"]]
```

</details>

### 요청 55 · TR90 · tierSlot

문안 P11 · 입력 역순 · 회수 12/12 · 거절 0 · 중복 0 · 입력/출력 토큰 943/143.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","ilvl":82,"rarity":"rare","mods":{"prefix":[],"suffix":[{"id":"filler"}]}},"targets":{"prefix":{"life":{"minimum":90,"tiers":[{"tier":3,"mod":"amulet:prefix:rotund","roll":[85,99],"sources":{"base":{"weight":1000},"essence":["GREATER_ESSENCE_BODY"],"desecrated":["Preserved_Collarbone","Ancient_Collarbone","Altered_Collarbone"]}},{"tier":2,"mod":"amulet:prefix:virile","roll":[100,119],"sources":{"base":{"weight":1000},"desecrated":["Preserved_Collarbone","Ancient_Collarbone","Altered_Collarbone"]}},{"tier":1,"mod":"amulet:prefix:athlete-s","roll":[120,149],"sources":{"base":{"weight":1000},"desecrated":["Preserved_Collarbone","Ancient_Collarbone","Altered_Collarbone"]}}]}},"suffix":{}},"strategies":{"regal_base.life":{"rarity":"magic","source":"base"},"upgrade_ess_body.life":{"rarity":"magic","source":"GREATER_ESSENCE_BODY"},"base_add.life":{"rarity":"rare","source":"base"},"reveal_Preserved_Collarbone.life":{"rarity":"rare","source":"Preserved_Collarbone"},"reveal_Ancient_Collarbone.life":{"rarity":"rare","source":"Ancient_Collarbone"},"reveal_Altered_Collarbone.life":{"rarity":"rare","source":"Altered_Collarbone"}},"allowed":["reveal_Altered_Collarbone.life","reveal_Ancient_Collarbone.life","reveal_Preserved_Collarbone.life","base_add.life"],"rules":["한 획득 전략만 비교한다. 실제 생명력 굴림이 목표 이상일 때만 성공이다."]}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"reveal_Altered_Collarbone.life":{"type":"array","items":{"type":"object","properties":{"steps":{"type":"array","items":{"type":"string","enum":["reveal_Altered_Collarbone.life","reveal_Ancient_Collarbone.life","reveal_Preserved_Collarbone.life","base_add.life"]}},"acceptTiers":{"type":"array","items":{"type":"string","enum":["T1","T2","T3"]}}},"required":["steps","acceptTiers"],"additionalProperties":false}},"reveal_Ancient_Collarbone.life":{"type":"array","items":{"type":"object","properties":{"steps":{"type":"array","items":{"type":"string","enum":["reveal_Altered_Collarbone.life","reveal_Ancient_Collarbone.life","reveal_Preserved_Collarbone.life","base_add.life"]}},"acceptTiers":{"type":"array","items":{"type":"string","enum":["T1","T2","T3"]}}},"required":["steps","acceptTiers"],"additionalProperties":false}},"reveal_Preserved_Collarbone.life":{"type":"array","items":{"type":"object","properties":{"steps":{"type":"array","items":{"type":"string","enum":["reveal_Altered_Collarbone.life","reveal_Ancient_Collarbone.life","reveal_Preserved_Collarbone.life","base_add.life"]}},"acceptTiers":{"type":"array","items":{"type":"string","enum":["T1","T2","T3"]}}},"required":["steps","acceptTiers"],"additionalProperties":false}},"base_add.life":{"type":"array","items":{"type":"object","properties":{"steps":{"type":"array","items":{"type":"string","enum":["reveal_Altered_Collarbone.life","reveal_Ancient_Collarbone.life","reveal_Preserved_Collarbone.life","base_add.life"]}},"acceptTiers":{"type":"array","items":{"type":"string","enum":["T1","T2","T3"]}}},"required":["steps","acceptTiers"],"additionalProperties":false}}},"required":["reveal_Altered_Collarbone.life","reveal_Ancient_Collarbone.life","reveal_Preserved_Collarbone.life","base_add.life"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"reveal_Altered_Collarbone.life":[{"steps":["reveal_Altered_Collarbone.life"],"acceptTiers":["T1","T2","T3"]}],"reveal_Ancient_Collarbone.life":[{"steps":["reveal_Ancient_Collarbone.life"],"acceptTiers":["T1","T2","T3"]}],"reveal_Preserved_Collarbone.life":[{"steps":["reveal_Preserved_Collarbone.life"],"acceptTiers":["T1","T2","T3"]}],"base_add.life":[{"steps":["base_add.life"],"acceptTiers":["T1","T2","T3"]}]}
```

### 요청 56 · TM90 · tierShort

문안 P12 · 기본 입력 · 회수 3/4 · 거절 0 · 중복 0 · 입력/출력 토큰 468/52.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","ilvl":82,"rarity":"magic","mods":{"prefix":[],"suffix":[{"id":"filler"}]}},"targets":{"prefix":{"life":{"minimum":90,"rolls":{"T1":[120,149],"T2":[100,119],"T3":[85,99]}}},"suffix":{}},"allowed":["regal_base.life","upgrade_ess_body.life"],"rules":["한 획득 전략만 비교한다. 실제 생명력 굴림이 목표 이상일 때만 성공이다."],"sourceTiers":{"regal_base.life":["T1","T2","T3"],"upgrade_ess_body.life":["T3"]}}
```

</details>

응답 스키마은 요청 53과 같다.

실제 응답

```json
{"regal_base.life":[{"steps":["regal_base.life"],"acceptTiers":["T1","T2"]}],"upgrade_ess_body.life":[{"steps":["upgrade_ess_body.life"],"acceptTiers":["T3"]}]}
```

누락 ["regal_base.life @T3"]

### 요청 57 · TM100 · tierShort

문안 P12 · 기본 입력 · 회수 2/2 · 거절 1 · 중복 0 · 입력/출력 토큰 468/49.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","ilvl":82,"rarity":"magic","mods":{"prefix":[],"suffix":[{"id":"filler"}]}},"targets":{"prefix":{"life":{"minimum":100,"rolls":{"T1":[120,149],"T2":[100,119],"T3":[85,99]}}},"suffix":{}},"allowed":["regal_base.life","upgrade_ess_body.life"],"rules":["한 획득 전략만 비교한다. 실제 생명력 굴림이 목표 이상일 때만 성공이다."],"sourceTiers":{"regal_base.life":["T1","T2","T3"],"upgrade_ess_body.life":["T3"]}}
```

</details>

응답 스키마은 요청 53과 같다.

실제 응답

```json
{"regal_base.life":[{"steps":["regal_base.life"],"acceptTiers":["T1","T2"]}],"upgrade_ess_body.life":[{"steps":["upgrade_ess_body.life"],"acceptTiers":[]}]}
```

<details>
<summary>거절된 후보와 사유</summary>

```json
[[["upgrade_ess_body.life"],"missing or incomplete tier route"]]
```

</details>

### 요청 58 · TR90 · tierShort

문안 P12 · 기본 입력 · 회수 12/12 · 거절 0 · 중복 0 · 입력/출력 토큰 782/273.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","ilvl":82,"rarity":"rare","mods":{"prefix":[],"suffix":[{"id":"filler"}]}},"targets":{"prefix":{"life":{"minimum":90,"rolls":{"T1":[120,149],"T2":[100,119],"T3":[85,99]}}},"suffix":{}},"allowed":["base_add.life","reveal_Preserved_Collarbone.life","reveal_Ancient_Collarbone.life","reveal_Altered_Collarbone.life"],"rules":["한 획득 전략만 비교한다. 실제 생명력 굴림이 목표 이상일 때만 성공이다."],"sourceTiers":{"base_add.life":["T1","T2","T3"],"reveal_Preserved_Collarbone.life":["T1","T2","T3"],"reveal_Ancient_Collarbone.life":["T1","T2","T3"],"reveal_Altered_Collarbone.life":["T1","T2","T3"]}}
```

</details>

<details>
<summary>응답 스키마</summary>

```json
{"type":"object","properties":{"base_add.life":{"type":"array","items":{"type":"object","properties":{"steps":{"type":"array","items":{"type":"string","enum":["base_add.life","reveal_Preserved_Collarbone.life","reveal_Ancient_Collarbone.life","reveal_Altered_Collarbone.life"]}},"acceptTiers":{"type":"array","items":{"type":"string","enum":["T1","T2","T3"]}}},"required":["steps","acceptTiers"],"additionalProperties":false}},"reveal_Preserved_Collarbone.life":{"type":"array","items":{"type":"object","properties":{"steps":{"type":"array","items":{"type":"string","enum":["base_add.life","reveal_Preserved_Collarbone.life","reveal_Ancient_Collarbone.life","reveal_Altered_Collarbone.life"]}},"acceptTiers":{"type":"array","items":{"type":"string","enum":["T1","T2","T3"]}}},"required":["steps","acceptTiers"],"additionalProperties":false}},"reveal_Ancient_Collarbone.life":{"type":"array","items":{"type":"object","properties":{"steps":{"type":"array","items":{"type":"string","enum":["base_add.life","reveal_Preserved_Collarbone.life","reveal_Ancient_Collarbone.life","reveal_Altered_Collarbone.life"]}},"acceptTiers":{"type":"array","items":{"type":"string","enum":["T1","T2","T3"]}}},"required":["steps","acceptTiers"],"additionalProperties":false}},"reveal_Altered_Collarbone.life":{"type":"array","items":{"type":"object","properties":{"steps":{"type":"array","items":{"type":"string","enum":["base_add.life","reveal_Preserved_Collarbone.life","reveal_Ancient_Collarbone.life","reveal_Altered_Collarbone.life"]}},"acceptTiers":{"type":"array","items":{"type":"string","enum":["T1","T2","T3"]}}},"required":["steps","acceptTiers"],"additionalProperties":false}}},"required":["base_add.life","reveal_Preserved_Collarbone.life","reveal_Ancient_Collarbone.life","reveal_Altered_Collarbone.life"],"additionalProperties":false}
```

</details>

실제 응답

```json
{"base_add.life":[{"steps":["base_add.life"],"acceptTiers":["T1"]},{"steps":["base_add.life"],"acceptTiers":["T2"]},{"steps":["base_add.life"],"acceptTiers":["T3"]}],"reveal_Preserved_Collarbone.life":[{"steps":["reveal_Preserved_Collarbone.life"],"acceptTiers":["T1"]},{"steps":["reveal_Preserved_Collarbone.life"],"acceptTiers":["T2"]},{"steps":["reveal_Preserved_Collarbone.life"],"acceptTiers":["T3"]}],"reveal_Ancient_Collarbone.life":[{"steps":["reveal_Ancient_Collarbone.life"],"acceptTiers":["T1"]},{"steps":["reveal_Ancient_Collarbone.life"],"acceptTiers":["T2"]},{"steps":["reveal_Ancient_Collarbone.life"],"acceptTiers":["T3"]}],"reveal_Altered_Collarbone.life":[{"steps":["reveal_Altered_Collarbone.life"],"acceptTiers":["T1"]},{"steps":["reveal_Altered_Collarbone.life"],"acceptTiers":["T2"]},{"steps":["reveal_Altered_Collarbone.life"],"acceptTiers":["T3"]}]}
```

### 요청 59 · TM90 · tierShort

문안 P12 · 입력 역순 · 회수 3/4 · 거절 0 · 중복 0 · 입력/출력 토큰 468/52.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","ilvl":82,"rarity":"magic","mods":{"prefix":[],"suffix":[{"id":"filler"}]}},"targets":{"prefix":{"life":{"minimum":90,"rolls":{"T3":[85,99],"T2":[100,119],"T1":[120,149]}}},"suffix":{}},"allowed":["upgrade_ess_body.life","regal_base.life"],"rules":["한 획득 전략만 비교한다. 실제 생명력 굴림이 목표 이상일 때만 성공이다."],"sourceTiers":{"upgrade_ess_body.life":["T3"],"regal_base.life":["T3","T2","T1"]}}
```

</details>

응답 스키마은 요청 54과 같다.

실제 응답

```json
{"upgrade_ess_body.life":[{"steps":["upgrade_ess_body.life"],"acceptTiers":["T3"]}],"regal_base.life":[{"steps":["regal_base.life"],"acceptTiers":["T1","T2"]}]}
```

누락 ["regal_base.life @T3"]
