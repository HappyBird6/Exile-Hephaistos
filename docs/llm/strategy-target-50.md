# 전략과 대상을 직접 반환하는 제작 경로 실험

## 사전 등록

목걸이와 마법봉의 누락된 전략 순서를 먼저 평가한다. 모델이 직접 적은 완결된 후보만 채점한다. 일대일 문자열 ID는 정확한 행동 쌍으로 치환할 수 있으나, 준비 삽입·목표 순열 생성·액션 조합 확장·다른 응답의 부분 경로 결합은 금지한다. 묶음 요청은 반환된 완전한 후보들의 합집합만 집계한다. 적법한 중복은 한 번만 센다.

N은 분열된 힘을 보존하고 life와 def를 새로 획득한다. 첫 카오스, 일반 엑잘, 훼손, 보호형 에센스, 조건부 절사 복구를 비교한다. W는 마법봉의 세 옵션 실제 획득 순서 여섯 개가 기준이다. W·H2 기대 목록은 획득 순서만 표시하지만 실제 응답에는 반드시 두 번째 Crafted 이전에 `cap_up.item`이 정확히 한 번 있어야 한다. 적법한 준비 위치가 달라도 동일한 획득 순서로 센다. R은 일반/훼손 중 선택하되 훼손 두 번은 막힌다. 종결 규약은 이미 완성일 때 빈 행동열 한 개, 불가능하면 빈 후보 목록이다. 기대는 아래 입력과 행동 추상화에서 사전에 판단했으며 실제 게임의 모든 경로 목록은 아니다.

[직전 실험](recipe-prompt-followup.md)의 실제 전송 로그를 재검사해 N(P6)의 한국어 사용자 입력에서 대체 문자 265개를 확인했다. 이번에는 원본 자료에서 다시 작성한 입력과 공통 지시문에 대해 UTF-8 엄격 읽기와 대체 문자 검사 후 전송한다. 이전 점수를 깨끗한 입력과의 대조군으로 사용하지 않는다.

## 50회 결과

목걸이와 마법봉에서 **모델이 화폐 전략과 대상을 직접 적은 경로**를 평가했다. 최종 43·44번은 카드당 한 요청이다. 태양의 목걸이 7개와 마법봉 6개를 모두 회수했다. 태양의 목걸이에서는 유효한 경로 두 개가 반복됐고 훼손 직후 절사를 붙인 오답 하나도 있었다. 마법봉은 일반 시전 속도 선취득 2개, 중간 획득 2개, 마지막 획득 2개를 모두 제시했고, 각각 두 번째 Crafted 이전에 한도 확장을 직접 적었다.

| 최종 호출 | 입력 | 기대 대비 회수 | 추가로 나온 문제 |
|---|---|---:|---|
| 43 | 태양의 목걸이, 분열 힘 보유 | 7/7 | 훼손 뒤 절사 1개, 유효 후보 중복 2개 |
| 44 | 마법봉, 빈 적격 소켓 | 6/6 | 없음 |
| 45 | 마법 장갑 | 2/2 | Rare 승급 전에 합금을 쓴 경로 2개 |
| 46 | 반지, 일반/훼손 두 출처 | 6/6 | 같은 아이템에서 훼손을 두 번 쓴 경로 2개 |
| 47 | 목걸이, 생명력 보유 | 1/1 | 없음 |
| 48 | 마법봉, Crafted 마나 스킬 보유 | 0/2 | 두 후보 모두 남은 두 목표 중 하나만 획득 |
| 49 | 마법봉, Crafted 한도 1, 소켓 없음 | 불가능 판정 실패 | 미완성 또는 한도 초과 후보 4개 |
| 50 | 목걸이, 품질·수치 조건 | 2/2 | 유효한 순서 하나를 잘못된 첫 행동 칸에도 기재 |

최종 8회에서 양성 7장의 고유 기대 경로 26개 중 24개를 찾았다. 음성 1장의 정답은 모든 목록이 비어 있는 응답이다. 이 판정까지 합하면 24/27이다. 48번은 검증에서 오답을 걸러도 유효 후보가 하나도 남지 않는 사례다. 49번에서는 빈 소켓이 없고 한도가 1이라는 시작 상태만으로 한도 초과를 검증할 수 있다. 이 실험의 음성 사례 하나로 일반적인 불가능 판정 정확도를 추정할 수는 없다.

### 회차별 수정과 재검사

| 호출 | 바꾼 점 | 기대 대비 회수¹ | 규칙·출력 계약 위반 후보 | 관찰 |
|---|---|---:|---:|---|
| 1–6 A | 한 응답에 [전략, 대상] 쌍으로 나열 | 8/34 | 8 | 1번은 같은 생명력을 연속 획득했고, 4번은 한도 확장을 두 번 사용했다. |
| 7–12 B | 전략별 적용 대상 목록과 전략.대상 원자 ID | 9/34 | 6 | 마법 장갑의 두 승급 경로는 회수했지만 목걸이 7번은 여전히 생명력을 중복 획득했다. |
| 13–18 C | 첫 획득 전략별 응답 칸에서 전체 순서 반환 | 17/34 | 9 | 반지의 여섯 경로가 모두 나왔다. 마법봉은 14번에서 시전 속도 선취득 칸을 비웠다. |
| 19–25 C | 같은 형식에서 요청당 첫 획득 하나만 지정 | 11/13 | 10 | 4회 목걸이 합집합 7/7, 3회 마법봉 합집합 4/6. |
| 26–31 D | 남은 목표별 택일 전략과 Crafted 사용량 명시 | 15/34 | 46 | 마법봉 27·29번 모두 6/6. 장갑은 택일해야 할 승급 전략을 연속 사용하는 등 다른 카드에서 악화됐다. |
| 32–35 E | 첫 행동 뒤 남은 목표 ID와 hit/onMiss별 전체 경로 | 6/26 | 15 | 목걸이에서 한 경로에 대체 획득처를 여러 개 넣었다. 분기 칸만으로는 개선되지 않았다. |
| 36–42 F | 첫 행동별 남은 행에 전략 연결, 길이·사용 가능 ID 제한, 첫 획득별 개별 요청 | 10/13 | 3 | 목걸이 7/7, 마법봉 3/6. 복구 수단이 없는 칸에도 중복 행동으로 onMiss를 채웠다. |
| 43–50 최종 | Crafted 경합에는 D, 그 외에는 F. 해당 첫 행동에서 가능한 엑잘 복구가 없으면 스키마에서 onMiss 제외 | 24/27 | 12 | 카드당 한 번. 미사용 대조 카드 H1–H4 포함. 기존 Crafted 보유·한도 부족 사례가 남았다. |

¹ 같은 입력을 거꾸로 제시한 재검사도 별도 요청으로 센다. 개별 첫 획득 요청은 그 첫 획득으로 가능한 경로만 분모에 넣는다. 회차별 카드 구성이 달라 합산 점수는 단계 간 성능 추정치로 쓰지 않는다. 규칙·계약 위반 횟수에는 기대 경로와 같은 순서를 잘못된 칸에 쓴 경우도 포함한다.

### 최종 전송·응답 계약

- 공통 입력은 베이스·희귀도·아이템 레벨·접두/접미별 시작 옵션, 목표 옵션의 출처 목록, 카드별 규칙, 사용할 전략 사전이다. 요청에 기대 경로, 정답 개수, 계산 확률 또는 가격을 넣지 않았다. 첫 획득 후보 목록은 준비·복구를 제외한 카드의 행동 사전에서 뽑는다. 첫 후보로 제시돼도 시작 상태에서 실행할 수 없으면 빈 목록이 맞다.
- **여러 Crafted가 한도를 공유하는 카드**는 D 문안을 사용한다. `goals`는 남은 목표별 대안, `crafted.used`는 기존 점유, `crafted.uses`는 한도를 쓰는 전략, `openers`는 첫 획득 후보들이다. 실험에서의 선택 조건은 Perfect Essence 전략이 있고, Alloy 전략 또는 이미 붙은 Crafted가 있는 경우다. 다른 종류의 Crafted 경합은 이 실험에서 검증하지 않았다.
- **나머지 카드**는 F 문안을 사용한다. `afterFirst`는 첫 획득 행동을 키로 쓰고 그 행동이 담당한 목표를 뺀 나머지 행과 대체 전략들을 값으로 갖는다. 예를 들어 방어 에센스로 시작하면 생명력 행만 남는다. 후속 행동 순서를 입력에 넣지 않는다. 첫 카오스는 첫 획득 전용이므로 후속 행동용 응답 enum에서 제외한다. 엑잘 복구를 사용할 수 있는 첫 획득 그룹에만 `onMiss`를 연다. 동일 그룹에 훼손 대안도 있을 수 있으므로 **해당 경로에서 엑잘 바로 뒤에 복구가 오는지** 여전히 검증해야 한다.
- **응답은 두 형식 모두 실행 순서**다. `safe_ess.def`는 ‘보호형 Perfect Essence 전략으로 방어 획득’, `cap_up.item`은 ‘이 장비에 한도 확장 적용’이다. 점은 두 ID의 구분자다. 첫 행동을 키에 쓴 경우에도 배열 안에 반복한다. `hit`와 `onMiss`는 각각 시작부터 완성까지의 독립 배열이므로 경로를 이어 붙일 필요가 없다. 개별 원소는 정적 전략에 한 번 대응한다. 더미 준비 등을 내부에 포함한 전략의 실제 실행 가능성은 백엔드에서 따로 확인한다.
- **요청 비용과 적용 범위.** 50회 모두 `gpt-4.1-mini-2025-04-14`, temperature 0, strict JSON Schema, 토큰 상한 3,200, 저장 비활성으로 정상 종료했다. API usage는 입력 36,258·출력 4,049토큰이다. 최종 목걸이는 입력 1,039·출력 169, 마법봉은 입력 826·출력 146토큰이었다. 이 8장은 이전 설계 사례에서 재구성한 전략 수준의 고정 입력이다. 출처는 카드 옵션별 목록이며 티어 ID별 원본 행, 수치 범위 및 정확한 화폐별 확률을 전송한 실험은 아니다. 같은 훼손 전략의 여러 재료를 별도 경로로 세지 않았다.
- **검증 범위.** API 호출 전 8개 입력의 기대 목록을 기록했다. 응답을 받은 뒤 별도 상태 전이 열거기로 시작 희귀도, 필요 목표, 획득 중복, Crafted 점유·소켓, 훼손 1회, 절사 직전 행동을 확인했다. 8개 기준 집합이 사전 기대와 일치했고 실제 50개 응답의 회수·위반·중복도 재현했다. Java Workbench 실행·단계별 확률 계산은 이번 문서 실험의 검증에 포함되지 않는다. 이미 모든 조건을 만족하는 입력의 무행동 종결도 이번 50회에는 없었다.

### 후속 실험에 남긴 질문

48번은 두 적법한 순서 모두에서 기존 마나 스킬을 보존하고, 새 스킬을 붙이기 전에 한도를 올려야 한다. 기존 점유는 이미 보냈으므로 같은 정보를 더 보내기보다, **두 목표를 모두 끝낸 전체 경로**인지 검증한 후 누락 시 미달 목표와 거절 사유만 돌려주는 재요청을 비교할 수 있다. 49번처럼 한도 확장이 없는 경우는 백엔드 상태 검증으로 구별할 수 있다. 이 피드백 재요청은 50회에 포함되지 않았으며 성능 개선은 미검증이다. 후보가 없다는 모델 응답만으로 사용자에게 제작 불가를 선언할 근거는 없다.

## 전략 사전

```json
{
  "first_chaos": {
    "effect": "첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함.",
    "when": "첫 획득에서만. 분열 보존."
  },
  "base_add": {
    "effect": "빈자리에 엑잘로 목표 일반 옵션을 시도.",
    "when": "Rare"
  },
  "reveal": {
    "effect": "대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택.",
    "when": "훼손 슬롯이 아직 없어야 한다."
  },
  "safe_ess": {
    "effect": "Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함.",
    "when": "Rare, Crafted 자리 1개 필요"
  },
  "safe_alloy": {
    "effect": "Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택.",
    "when": "Rare, Crafted 자리 1개 필요"
  },
  "whittle_miss": {
    "effect": "해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략.",
    "when": "같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."
  },
  "cap_up": {
    "effect": "Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림.",
    "when": "빈 적격 소켓, 한 번만. 대상 item."
  },
  "upgrade_ess": {
    "effect": "등급 상승용 에센스로 목표 일반 옵션 획득.",
    "when": "Magic → Rare"
  },
  "regal_base": {
    "effect": "제왕으로 Magic을 Rare로 올리며 해당 일반 옵션이 뜬 결과 채택.",
    "when": "Magic → Rare"
  },
  "flesh": {
    "effect": "Flesh 품질 20 적용. 기존 수치 유지.",
    "when": "대상 item"
  },
  "divine": {
    "effect": "두 옵션 수치를 현재 티어 안에서 함께 굴린다.",
    "when": "변경 가능 아이템, 대상 item"
  }
}
```

## 전송한 문안

최종 선택은 아래 D(마법봉)와 F(목걸이)다. 43–50번은 이 두 문안을 그대로 사용하면서 카드에 맞는 입력과 응답 스키마를 선택했다.

### A · 전략과 대상 쌍

```text
역할: PoE2 제작 작업대의 후보 설계자.
입력: 시작 옵션을 보존하고 need를 모두 달성한다. actions의 각 항목은 허용된 전략.대상 한 쌍이다. 각 전략의 effect와 when, 카드별 rules를 따른다.
출력: recipes에 서로 다른 가능한 화폐 사용 순서를 나열한다. 각 단계는 [전략, 대상]이다. 조건부 복구와 필수 준비도 실제 위치에 적는다. 빈 후보 목록은 불가능을 뜻한다. 확률과 비용은 백엔드가 계산한다.
```

### B · 전략.대상 ID

```text
역할: PoE2 제작 경로 후보 설계자. 작업대는 후보를 검증하고 확률·비용을 계산한다.
입력: start.mods는 보존할 시작 옵션이다. need의 미달 목표마다 moves에서 적법한 획득 전략 하나를 선택한다. targets는 가능한 출처이고 한 목표의 대체 출처를 연속으로 쓰지 않는다.
탐색: 각 선택에서 가능한 첫 획득을 모두 바꾸고 남은 획득도 앞·중간·끝에 배치해 본다. 전략의 when, 카드 rules와 희귀도·슬롯·Crafted 점유를 매 단계 적용한다. 필요한 준비는 한 번만 실제 위치에 적는다. 조건부 복구가 가능하면 직접 적중 경로와 실패 복구 경로를 각각 만든다.
출력: recipes의 각 배열은 시작부터 완성까지의 실행 순서다. 단계는 moves의 전략과 대상 ID를 점으로 잇는 단일 문자열이다. 이미 완성이면 [[]], 도달 불가능하면 []를 반환한다.
```

### C · 첫 획득별 응답

```text
역할: PoE2 제작 작업대의 레시피 후보 설계자. 확률·비용 계산은 백엔드가 맡는다.
입력: start.mods와 이미 얻은 목표를 보존한다. need는 남은 조건이다. moves는 전략별 사용 조건·효과·적용 대상이다. 단계 ID는 전략.대상이다.
탐색: openers의 각 키를 첫 목표 획득으로 독립 탐색한다. 그 행동을 실행한 뒤 미달 목표 하나와 적법한 획득 전략 하나를 골라 반복한다. 남은 목표를 고르는 순서와 같은 목표를 얻는 대체 전략을 모두 바꿔 본다. 같은 목표를 두 번 획득하지 않는다. 필수 준비는 필요한 행동 직전에 한 번 기록한다. 엑잘 적중과 조건부 실패 복구 양쪽에서 완성까지 이어간다.
출력: 각 키에 해당 첫 획득으로 시작하는 완성 경로 배열을 넣는다. 각 경로에는 첫 행동부터 마지막 행동까지 실제 사용할 전략.대상을 전부 적는다. 성립하는 경로가 없는 키는 []다. 설명 없이 JSON만 반환한다.
```

### D · 목표별 대안과 Crafted 점유 · 최종 마법봉

```text
역할: PoE2 제작 경로 설계자. 보존 조건을 만족하는 다양한 완성 후보를 제시한다.
입력: goals의 각 키는 미달 조건이고 값은 그중 하나만 성공하면 해당 키가 끝나는 대체 전략.대상 목록이다. start는 현재 상태, targets는 출처, strategies는 실행 의미다. helpers는 준비·복구 전용이다. 모든 실험은 고정 시작 아이템을 쓴다.
순서: openers의 각 전략.대상으로 경우를 나눈다. 획득한 키를 지우고 남은 키 하나를 고르는 모든 순서와 그 키의 각 대체 전략을 시도한다. goals의 나열 순서에는 의미가 없다. 일반 옵션을 첫째·중간·끝에 두는 경로도 점검한다.
실행: 시작 Crafted 점유에서 출발한다. crafted.uses 전략은 자리를 하나씩 쓰며, 부족하면 그 전략 직전에 가능한 한도 확장을 한 번 적는다. 첫 카오스는 첫 획득 전용이다. 같은 아이템의 훼손 슬롯 하나를 공유한다. base_add 적중 경로마다 허용된 동일 대상 whittle_miss가 있으면 그 직후 복구를 넣은 경로도 따로 적는다.
출력: 각 opener 키에 해당 첫 획득을 포함한 전체 전략.대상 배열들을 반환한다. 각 경로가 goals의 키를 하나씩 모두 채웠는지 검토한다. 불가능한 키는 []다. 백엔드는 반환 경로의 전이·확률·비용을 검증한다.
```

### E · 남은 목표와 결과별 완성 경로

```text
역할: PoE2 작업대에서 쓸 화폐·대상 순서를 설계한다. 확률과 비용 계산은 백엔드가 맡는다.
1. 입력을 읽는다. start는 보존할 아이템이다. goals의 각 행은 필요한 조건 하나, 행 안의 행동은 택일이다. 전략.대상은 실행할 화폐 전략과 적용 대상을 함께 지정한다.
2. remainingAfterFirst의 각 키에서 새로 시작한다. 키의 행동으로 첫 조건을 채운다. 값에 적힌 나머지 조건을 각각 정확히 한 번 채운다. 다음 조건의 순서와 그 조건 안의 전략 선택을 모두 바꿔 본다.
3. 각 단계에서 strategies와 rules를 적용한다. 사용 중인 Crafted 자리까지 세고 자리가 부족해지는 행동 전에 허용된 준비를 적는다. helpers는 목표 획득 횟수에 포함하지 않는다.
4. 각 키의 응답에는 첫 획득과 준비까지 포함한 완성 경로만 적는다. hit에는 직접 적중 경로를 쓴다. onMiss에는 엑잘 직후 허용된 동일 대상 복구를 실행하는 전체 경로를 처음부터 다시 적는다. 이 분류가 없는 키에는 완성 경로 배열을 바로 쓴다. 가능한 경로가 없으면 빈 배열이다.
```

### F · 남은 목표의 대체 행동 · 최종 목걸이

```text
역할: PoE2 제작 작업대의 레시피 설계자다. 현재 아이템으로 시도할 화폐 사용 순서를 제안한다. 확률·비용은 백엔드가 계산한다.

입력: start와 targets에서 보존할 옵션과 획득처를 확인한다. strategies는 허용 전략, helpers는 준비·복구 행동이다. 단계 ID 전략.대상은 작업대에서 실행할 하나의 선택이다.

탐색: afterFirst의 각 키는 시도할 첫 획득이다. 키를 실행하고 그 아래 남은 목표 행마다 정확히 하나의 행동을 고른다. 남은 행이 두 개라면 두 순서 모두, 한 행의 대안이 두 개라면 각 대안을 별도 경로로 검토한다. 행이 없으면 첫 획득으로 완성된다. 첫 행동도 시작 상태에서 실행할 수 있어야 한다.

준비와 분기: strategies의 사용 조건, rules, 현재 Crafted 점유와 한도를 매 단계 확인한다. 필요한 준비를 사용 전에 직접 적는다. hit는 직접 적중, onMiss는 엑잘 실패 직후 같은 대상에 허용된 복구를 한 경우다. 복구 행동이 없으면 onMiss는 비워 둔다.

응답: 각 결과에 첫 행동·준비·후속 화폐까지 포함한 완전한 전략.대상 배열을 적는다. 분기 없는 요청은 배열을 바로 반환한다. 이미 확보한 목표는 다시 획득하지 않는다. 가능한 후보가 없는 목록은 []다.
```

## N · 분열 목걸이 · 방어 선취득·생명력 획득·실패 복구

사전에 고정한 입력 사실과 기대 경로. expected는 전송하지 않는다.

```json
{"title":"분열 목걸이 · 방어 선취득·생명력 획득·실패 복구","start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"need":["life","def"],"actions":["first_chaos.life","base_add.life","reveal.life","safe_ess.def","whittle_miss.life"],"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"expected":["first_chaos.life safe_ess.def","base_add.life safe_ess.def","base_add.life whittle_miss.life safe_ess.def","reveal.life safe_ess.def","safe_ess.def base_add.life","safe_ess.def base_add.life whittle_miss.life","safe_ess.def reveal.life"]}
```

### 요청 1 · A · 정순

기대 번호 1, 2, 3, 4, 5, 6, 7 (위 expected의 1부터 시작하는 순번). 회수 0/7. 누락 1, 2, 3, 4, 5, 6, 7. 규칙·출력 계약 위반 3개, 유효 중복 0개. 입력 600·출력 79토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"need":["life","def"],"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"actions":["first_chaos.life","base_add.life","reveal.life","safe_ess.def","whittle_miss.life"],"strategies":{"first_chaos":{"effect":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함.","when":"첫 획득에서만. 분열 보존."},"base_add":{"effect":"빈자리에 엑잘로 목표 일반 옵션을 시도.","when":"Rare"},"reveal":{"effect":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택.","when":"훼손 슬롯이 아직 없어야 한다."},"safe_ess":{"effect":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함.","when":"Rare, Crafted 자리 1개 필요"},"whittle_miss":{"effect":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략.","when":"같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."}}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"recipes":{"type":"array","items":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos","life","base_add","reveal","safe_ess","def","whittle_miss"]}}}}},"required":["recipes"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"recipes":[[["first_chaos","life"],["base_add","life"],["whittle_miss","life"],["safe_ess","def"]],[["first_chaos","life"],["base_add","life"],["safe_ess","def"]],[["first_chaos","life"],["reveal","life"],["safe_ess","def"]]]}
```

위반 경로 (채점 표기)

```text
first_chaos.life base_add.life whittle_miss.life safe_ess.def
first_chaos.life base_add.life safe_ess.def
first_chaos.life reveal.life safe_ess.def
```

### 요청 3 · A · 역순

기대 번호 1, 2, 3, 4, 5, 6, 7 (위 expected의 1부터 시작하는 순번). 회수 3/7. 누락 4, 5, 6, 7. 규칙·출력 계약 위반 0개, 유효 중복 0개. 입력 600·출력 57토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"def":["Perfect Essence of Enhancement"],"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"need":["def","life"],"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"actions":["whittle_miss.life","safe_ess.def","reveal.life","base_add.life","first_chaos.life"],"strategies":{"whittle_miss":{"effect":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략.","when":"같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."},"safe_ess":{"effect":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함.","when":"Rare, Crafted 자리 1개 필요"},"reveal":{"effect":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택.","when":"훼손 슬롯이 아직 없어야 한다."},"base_add":{"effect":"빈자리에 엑잘로 목표 일반 옵션을 시도.","when":"Rare"},"first_chaos":{"effect":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함.","when":"첫 획득에서만. 분열 보존."}}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"recipes":{"type":"array","items":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["whittle_miss","life","safe_ess","def","reveal","base_add","first_chaos"]}}}}},"required":["recipes"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"recipes":[[["base_add","life"],["whittle_miss","life"],["safe_ess","def"]],[["base_add","life"],["safe_ess","def"]],[["first_chaos","life"],["safe_ess","def"]]]}
```

### 요청 7 · B · 정순

기대 번호 1, 2, 3, 4, 5, 6, 7 (위 expected의 1부터 시작하는 순번). 회수 0/7. 누락 1, 2, 3, 4, 5, 6, 7. 규칙·출력 계약 위반 2개, 유효 중복 0개. 입력 696·출력 42토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"need":["life","def"],"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"moves":{"first_chaos":{"targets":["life"],"effect":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함.","when":"첫 획득에서만. 분열 보존."},"base_add":{"targets":["life"],"effect":"빈자리에 엑잘로 목표 일반 옵션을 시도.","when":"Rare"},"reveal":{"targets":["life"],"effect":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택.","when":"훼손 슬롯이 아직 없어야 한다."},"safe_ess":{"targets":["def"],"effect":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함.","when":"Rare, Crafted 자리 1개 필요"},"whittle_miss":{"targets":["life"],"effect":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략.","when":"같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."}}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"recipes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","base_add.life","reveal.life","safe_ess.def","whittle_miss.life"]}}}},"required":["recipes"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"recipes":[["first_chaos.life","base_add.life","safe_ess.def"],["first_chaos.life","base_add.life","whittle_miss.life","safe_ess.def"]]}
```

위반 경로 (채점 표기)

```text
first_chaos.life base_add.life safe_ess.def
first_chaos.life base_add.life whittle_miss.life safe_ess.def
```

### 요청 9 · B · 역순

기대 번호 1, 2, 3, 4, 5, 6, 7 (위 expected의 1부터 시작하는 순번). 회수 3/7. 누락 1, 2, 3, 4. 규칙·출력 계약 위반 3개, 유효 중복 0개. 입력 696·출력 86토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"def":["Perfect Essence of Enhancement"],"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"need":["def","life"],"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"moves":{"whittle_miss":{"targets":["life"],"effect":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략.","when":"같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."},"safe_ess":{"targets":["def"],"effect":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함.","when":"Rare, Crafted 자리 1개 필요"},"reveal":{"targets":["life"],"effect":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택.","when":"훼손 슬롯이 아직 없어야 한다."},"base_add":{"targets":["life"],"effect":"빈자리에 엑잘로 목표 일반 옵션을 시도.","when":"Rare"},"first_chaos":{"targets":["life"],"effect":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함.","when":"첫 획득에서만. 분열 보존."}}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"recipes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["whittle_miss.life","safe_ess.def","reveal.life","base_add.life","first_chaos.life"]}}}},"required":["recipes"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"recipes":[["safe_ess.def","base_add.life"],["safe_ess.def","base_add.life","whittle_miss.life"],["safe_ess.def","reveal.life"],["safe_ess.def","reveal.life","whittle_miss.life"],["safe_ess.def","first_chaos.life"],["safe_ess.def","first_chaos.life","whittle_miss.life"]]}
```

위반 경로 (채점 표기)

```text
safe_ess.def reveal.life whittle_miss.life
safe_ess.def first_chaos.life
safe_ess.def first_chaos.life whittle_miss.life
```

### 요청 13 · C · 정순

기대 번호 1, 2, 3, 4, 5, 6, 7 (위 expected의 1부터 시작하는 순번). 회수 3/7. 누락 3, 5, 6, 7. 규칙·출력 계약 위반 4개, 유효 중복 0개. 입력 896·출력 116토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"need":["life","def"],"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"moves":{"first_chaos":{"targets":["life"],"effect":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함.","when":"첫 획득에서만. 분열 보존."},"base_add":{"targets":["life"],"effect":"빈자리에 엑잘로 목표 일반 옵션을 시도.","when":"Rare"},"reveal":{"targets":["life"],"effect":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택.","when":"훼손 슬롯이 아직 없어야 한다."},"safe_ess":{"targets":["def"],"effect":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함.","when":"Rare, Crafted 자리 1개 필요"},"whittle_miss":{"targets":["life"],"effect":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략.","when":"같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."}},"openers":["first_chaos.life","base_add.life","reveal.life","safe_ess.def"]}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"first_chaos.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","base_add.life","reveal.life","safe_ess.def","whittle_miss.life"]}}},"base_add.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","base_add.life","reveal.life","safe_ess.def","whittle_miss.life"]}}},"reveal.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","base_add.life","reveal.life","safe_ess.def","whittle_miss.life"]}}},"safe_ess.def":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","base_add.life","reveal.life","safe_ess.def","whittle_miss.life"]}}}},"required":["first_chaos.life","base_add.life","reveal.life","safe_ess.def"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"first_chaos.life":[["first_chaos.life","safe_ess.def"],["first_chaos.life","base_add.life","safe_ess.def"],["first_chaos.life","reveal.life","safe_ess.def"]],"base_add.life":[["base_add.life","safe_ess.def"],["base_add.life","reveal.life","safe_ess.def"]],"reveal.life":[["reveal.life","safe_ess.def"],["reveal.life","base_add.life","safe_ess.def"]],"safe_ess.def":[]}
```

위반 경로 (채점 표기)

```text
first_chaos.life base_add.life safe_ess.def
first_chaos.life reveal.life safe_ess.def
base_add.life reveal.life safe_ess.def
reveal.life base_add.life safe_ess.def
```

### 요청 15 · C · 역순

기대 번호 1, 2, 3, 4, 5, 6, 7 (위 expected의 1부터 시작하는 순번). 회수 3/7. 누락 3, 4, 6, 7. 규칙·출력 계약 위반 1개, 유효 중복 0개. 입력 896·출력 65토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"def":["Perfect Essence of Enhancement"],"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"need":["def","life"],"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"moves":{"whittle_miss":{"targets":["life"],"effect":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략.","when":"같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."},"safe_ess":{"targets":["def"],"effect":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함.","when":"Rare, Crafted 자리 1개 필요"},"reveal":{"targets":["life"],"effect":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택.","when":"훼손 슬롯이 아직 없어야 한다."},"base_add":{"targets":["life"],"effect":"빈자리에 엑잘로 목표 일반 옵션을 시도.","when":"Rare"},"first_chaos":{"targets":["life"],"effect":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함.","when":"첫 획득에서만. 분열 보존."}},"openers":["safe_ess.def","reveal.life","base_add.life","first_chaos.life"]}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"safe_ess.def":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["whittle_miss.life","safe_ess.def","reveal.life","base_add.life","first_chaos.life"]}}},"reveal.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["whittle_miss.life","safe_ess.def","reveal.life","base_add.life","first_chaos.life"]}}},"base_add.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["whittle_miss.life","safe_ess.def","reveal.life","base_add.life","first_chaos.life"]}}},"first_chaos.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["whittle_miss.life","safe_ess.def","reveal.life","base_add.life","first_chaos.life"]}}}},"required":["safe_ess.def","reveal.life","base_add.life","first_chaos.life"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"safe_ess.def":[["safe_ess.def","base_add.life"]],"reveal.life":[["reveal.life","base_add.life"]],"base_add.life":[["base_add.life","safe_ess.def"]],"first_chaos.life":[["first_chaos.life","safe_ess.def"]]}
```

위반 경로 (채점 표기)

```text
reveal.life base_add.life
```

### 요청 19 · C · 정순 · first_chaos.life

기대 번호 1 (위 expected의 1부터 시작하는 순번). 회수 1/1. 누락 없음. 규칙·출력 계약 위반 5개, 유효 중복 0개. 입력 735·출력 120토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"need":["life","def"],"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"moves":{"first_chaos":{"targets":["life"],"effect":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함.","when":"첫 획득에서만. 분열 보존."},"base_add":{"targets":["life"],"effect":"빈자리에 엑잘로 목표 일반 옵션을 시도.","when":"Rare"},"reveal":{"targets":["life"],"effect":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택.","when":"훼손 슬롯이 아직 없어야 한다."},"safe_ess":{"targets":["def"],"effect":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함.","when":"Rare, Crafted 자리 1개 필요"},"whittle_miss":{"targets":["life"],"effect":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략.","when":"같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."}},"openers":["first_chaos.life"]}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"first_chaos.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","base_add.life","reveal.life","safe_ess.def","whittle_miss.life"]}}}},"required":["first_chaos.life"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"first_chaos.life":[["first_chaos.life","safe_ess.def"],["first_chaos.life","base_add.life","safe_ess.def"],["first_chaos.life","base_add.life","whittle_miss.life","safe_ess.def"],["first_chaos.life","reveal.life","safe_ess.def"],["first_chaos.life","reveal.life","base_add.life","safe_ess.def"],["first_chaos.life","reveal.life","base_add.life","whittle_miss.life","safe_ess.def"]]}
```

위반 경로 (채점 표기)

```text
first_chaos.life base_add.life safe_ess.def
first_chaos.life base_add.life whittle_miss.life safe_ess.def
first_chaos.life reveal.life safe_ess.def
first_chaos.life reveal.life base_add.life safe_ess.def
first_chaos.life reveal.life base_add.life whittle_miss.life safe_ess.def
```

### 요청 21 · C · 정순 · safe_ess.def

기대 번호 5, 6, 7 (위 expected의 1부터 시작하는 순번). 회수 3/3. 누락 없음. 규칙·출력 계약 위반 3개, 유효 중복 0개. 입력 733·출력 89토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"need":["life","def"],"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"moves":{"first_chaos":{"targets":["life"],"effect":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함.","when":"첫 획득에서만. 분열 보존."},"base_add":{"targets":["life"],"effect":"빈자리에 엑잘로 목표 일반 옵션을 시도.","when":"Rare"},"reveal":{"targets":["life"],"effect":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택.","when":"훼손 슬롯이 아직 없어야 한다."},"safe_ess":{"targets":["def"],"effect":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함.","when":"Rare, Crafted 자리 1개 필요"},"whittle_miss":{"targets":["life"],"effect":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략.","when":"같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."}},"openers":["safe_ess.def"]}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"safe_ess.def":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","base_add.life","reveal.life","safe_ess.def","whittle_miss.life"]}}}},"required":["safe_ess.def"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"safe_ess.def":[["safe_ess.def","base_add.life"],["safe_ess.def","reveal.life"],["safe_ess.def","first_chaos.life"],["safe_ess.def","base_add.life","whittle_miss.life"],["safe_ess.def","reveal.life","whittle_miss.life"],["safe_ess.def","first_chaos.life","whittle_miss.life"]]}
```

위반 경로 (채점 표기)

```text
safe_ess.def first_chaos.life
safe_ess.def reveal.life whittle_miss.life
safe_ess.def first_chaos.life whittle_miss.life
```

### 요청 23 · C · 정순 · base_add.life

기대 번호 2, 3 (위 expected의 1부터 시작하는 순번). 회수 2/2. 누락 없음. 규칙·출력 계약 위반 0개, 유효 중복 0개. 입력 731·출력 32토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"need":["life","def"],"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"moves":{"first_chaos":{"targets":["life"],"effect":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함.","when":"첫 획득에서만. 분열 보존."},"base_add":{"targets":["life"],"effect":"빈자리에 엑잘로 목표 일반 옵션을 시도.","when":"Rare"},"reveal":{"targets":["life"],"effect":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택.","when":"훼손 슬롯이 아직 없어야 한다."},"safe_ess":{"targets":["def"],"effect":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함.","when":"Rare, Crafted 자리 1개 필요"},"whittle_miss":{"targets":["life"],"effect":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략.","when":"같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."}},"openers":["base_add.life"]}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"base_add.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","base_add.life","reveal.life","safe_ess.def","whittle_miss.life"]}}}},"required":["base_add.life"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"base_add.life":[["base_add.life","safe_ess.def"],["base_add.life","whittle_miss.life","safe_ess.def"]]}
```

### 요청 25 · C · 정순 · reveal.life

기대 번호 4 (위 expected의 1부터 시작하는 순번). 회수 1/1. 누락 없음. 규칙·출력 계약 위반 2개, 유효 중복 0개. 입력 731·출력 50토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"need":["life","def"],"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"moves":{"first_chaos":{"targets":["life"],"effect":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함.","when":"첫 획득에서만. 분열 보존."},"base_add":{"targets":["life"],"effect":"빈자리에 엑잘로 목표 일반 옵션을 시도.","when":"Rare"},"reveal":{"targets":["life"],"effect":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택.","when":"훼손 슬롯이 아직 없어야 한다."},"safe_ess":{"targets":["def"],"effect":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함.","when":"Rare, Crafted 자리 1개 필요"},"whittle_miss":{"targets":["life"],"effect":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략.","when":"같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."}},"openers":["reveal.life"]}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"reveal.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","base_add.life","reveal.life","safe_ess.def","whittle_miss.life"]}}}},"required":["reveal.life"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"reveal.life":[["reveal.life","safe_ess.def"],["reveal.life","base_add.life","safe_ess.def"],["reveal.life","base_add.life","whittle_miss.life","safe_ess.def"]]}
```

위반 경로 (채점 표기)

```text
reveal.life base_add.life safe_ess.def
reveal.life base_add.life whittle_miss.life safe_ess.def
```

### 요청 26 · D · 정순

기대 번호 1, 2, 3, 4, 5, 6, 7 (위 expected의 1부터 시작하는 순번). 회수 3/7. 누락 3, 5, 6, 7. 규칙·출력 계약 위반 7개, 유효 중복 0개. 입력 985·출력 154토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"openers":["first_chaos.life","base_add.life","reveal.life","safe_ess.def"],"goals":{"life":["first_chaos.life","base_add.life","reveal.life"],"def":["safe_ess.def"]},"strategies":{"first_chaos":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함. 첫 획득에서만. 분열 보존.","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","whittle_miss":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략. 같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."},"helpers":["whittle_miss.life"],"crafted":{"used":0,"uses":["safe_ess"]}}
```

</details>

응답 JSON Schema은 요청 13과 동일하다.

실제 응답 원문

```json
{"first_chaos.life":[["first_chaos.life","safe_ess.def"],["first_chaos.life","base_add.life","safe_ess.def"],["first_chaos.life","reveal.life","safe_ess.def"]],"base_add.life":[["base_add.life","safe_ess.def"],["base_add.life","reveal.life","safe_ess.def"],["base_add.life","first_chaos.life","safe_ess.def"]],"reveal.life":[["reveal.life","safe_ess.def"],["reveal.life","base_add.life","safe_ess.def"],["reveal.life","first_chaos.life","safe_ess.def"]],"safe_ess.def":[["safe_ess.def"]]}
```

위반 경로 (채점 표기)

```text
first_chaos.life base_add.life safe_ess.def
first_chaos.life reveal.life safe_ess.def
base_add.life reveal.life safe_ess.def
base_add.life first_chaos.life safe_ess.def
reveal.life base_add.life safe_ess.def
reveal.life first_chaos.life safe_ess.def
safe_ess.def
```

### 요청 28 · D · 역순

기대 번호 1, 2, 3, 4, 5, 6, 7 (위 expected의 1부터 시작하는 순번). 회수 0/7. 누락 1, 2, 3, 4, 5, 6, 7. 규칙·출력 계약 위반 9개, 유효 중복 0개. 입력 985·출력 88토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"def":["Perfect Essence of Enhancement"],"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"openers":["safe_ess.def","reveal.life","base_add.life","first_chaos.life"],"goals":{"def":["safe_ess.def"],"life":["reveal.life","base_add.life","first_chaos.life"]},"strategies":{"whittle_miss":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략. 같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다.","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","first_chaos":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함. 첫 획득에서만. 분열 보존."},"helpers":["whittle_miss.life"],"crafted":{"used":0,"uses":["safe_ess"]}}
```

</details>

응답 JSON Schema은 요청 15과 동일하다.

실제 응답 원문

```json
{"safe_ess.def":[["safe_ess.def"]],"reveal.life":[["reveal.life"],["base_add.life","whittle_miss.life"],["base_add.life"],["first_chaos.life"]],"base_add.life":[["base_add.life"],["base_add.life","whittle_miss.life"],["first_chaos.life"]],"first_chaos.life":[["first_chaos.life"]]}
```

위반 경로 (채점 표기)

```text
safe_ess.def
reveal.life
base_add.life whittle_miss.life
base_add.life
first_chaos.life
base_add.life
base_add.life whittle_miss.life
first_chaos.life
first_chaos.life
```

### 요청 32 · E · 정순

기대 번호 1, 2, 3, 4, 5, 6, 7 (위 expected의 1부터 시작하는 순번). 회수 0/7. 누락 1, 2, 3, 4, 5, 6, 7. 규칙·출력 계약 위반 7개, 유효 중복 0개. 입력 1163·출력 185토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"goals":{"life":["first_chaos.life","base_add.life","reveal.life"],"def":["safe_ess.def"]},"strategies":{"first_chaos":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함. 첫 획득에서만. 분열 보존.","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","whittle_miss":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략. 같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."},"helpers":["whittle_miss.life"],"crafted":{"used":0,"uses":["safe_ess"]},"remainingAfterFirst":{"first_chaos.life":["def"],"base_add.life":["def"],"reveal.life":["def"],"safe_ess.def":["life"]}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"first_chaos.life":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","base_add.life","reveal.life","safe_ess.def","whittle_miss.life"]}}},"onMiss":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","base_add.life","reveal.life","safe_ess.def","whittle_miss.life"]}}}},"required":["hit","onMiss"],"additionalProperties":false},"base_add.life":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","base_add.life","reveal.life","safe_ess.def","whittle_miss.life"]}}},"onMiss":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","base_add.life","reveal.life","safe_ess.def","whittle_miss.life"]}}}},"required":["hit","onMiss"],"additionalProperties":false},"reveal.life":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","base_add.life","reveal.life","safe_ess.def","whittle_miss.life"]}}},"onMiss":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","base_add.life","reveal.life","safe_ess.def","whittle_miss.life"]}}}},"required":["hit","onMiss"],"additionalProperties":false},"safe_ess.def":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","base_add.life","reveal.life","safe_ess.def","whittle_miss.life"]}}},"onMiss":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","base_add.life","reveal.life","safe_ess.def","whittle_miss.life"]}}}},"required":["hit","onMiss"],"additionalProperties":false}},"required":["first_chaos.life","base_add.life","reveal.life","safe_ess.def"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"first_chaos.life":{"hit":[["first_chaos.life","safe_ess.def","base_add.life","reveal.life"]],"onMiss":[["first_chaos.life","whittle_miss.life","safe_ess.def","base_add.life","reveal.life"]]},"base_add.life":{"hit":[["base_add.life","safe_ess.def","reveal.life"]],"onMiss":[["base_add.life","whittle_miss.life","safe_ess.def","reveal.life"]]},"reveal.life":{"hit":[["reveal.life","safe_ess.def","base_add.life"]],"onMiss":[["reveal.life","whittle_miss.life","safe_ess.def","base_add.life"]]},"safe_ess.def":{"hit":[["safe_ess.def","first_chaos.life","base_add.life","reveal.life"]],"onMiss":[]}}
```

위반 경로 (채점 표기)

```text
first_chaos.life safe_ess.def base_add.life reveal.life
first_chaos.life whittle_miss.life safe_ess.def base_add.life reveal.life
base_add.life safe_ess.def reveal.life
base_add.life whittle_miss.life safe_ess.def reveal.life
reveal.life safe_ess.def base_add.life
reveal.life whittle_miss.life safe_ess.def base_add.life
safe_ess.def first_chaos.life base_add.life reveal.life
```

### 요청 34 · E · 역순

기대 번호 1, 2, 3, 4, 5, 6, 7 (위 expected의 1부터 시작하는 순번). 회수 0/7. 누락 1, 2, 3, 4, 5, 6, 7. 규칙·출력 계약 위반 8개, 유효 중복 0개. 입력 1163·출력 235토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"def":["Perfect Essence of Enhancement"],"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"goals":{"def":["safe_ess.def"],"life":["reveal.life","base_add.life","first_chaos.life"]},"strategies":{"whittle_miss":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략. 같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다.","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","first_chaos":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함. 첫 획득에서만. 분열 보존."},"helpers":["whittle_miss.life"],"crafted":{"used":0,"uses":["safe_ess"]},"remainingAfterFirst":{"safe_ess.def":["life"],"reveal.life":["def"],"base_add.life":["def"],"first_chaos.life":["def"]}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"safe_ess.def":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["whittle_miss.life","safe_ess.def","reveal.life","base_add.life","first_chaos.life"]}}},"onMiss":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["whittle_miss.life","safe_ess.def","reveal.life","base_add.life","first_chaos.life"]}}}},"required":["hit","onMiss"],"additionalProperties":false},"reveal.life":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["whittle_miss.life","safe_ess.def","reveal.life","base_add.life","first_chaos.life"]}}},"onMiss":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["whittle_miss.life","safe_ess.def","reveal.life","base_add.life","first_chaos.life"]}}}},"required":["hit","onMiss"],"additionalProperties":false},"base_add.life":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["whittle_miss.life","safe_ess.def","reveal.life","base_add.life","first_chaos.life"]}}},"onMiss":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["whittle_miss.life","safe_ess.def","reveal.life","base_add.life","first_chaos.life"]}}}},"required":["hit","onMiss"],"additionalProperties":false},"first_chaos.life":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["whittle_miss.life","safe_ess.def","reveal.life","base_add.life","first_chaos.life"]}}},"onMiss":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["whittle_miss.life","safe_ess.def","reveal.life","base_add.life","first_chaos.life"]}}}},"required":["hit","onMiss"],"additionalProperties":false}},"required":["safe_ess.def","reveal.life","base_add.life","first_chaos.life"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"safe_ess.def":{"hit":[["safe_ess.def","reveal.life","base_add.life","first_chaos.life"]],"onMiss":[["safe_ess.def","whittle_miss.life","reveal.life","base_add.life","first_chaos.life"]]},"reveal.life":{"hit":[["reveal.life","safe_ess.def","base_add.life","first_chaos.life"]],"onMiss":[["reveal.life","whittle_miss.life","safe_ess.def","base_add.life","first_chaos.life"]]},"base_add.life":{"hit":[["base_add.life","safe_ess.def","reveal.life","first_chaos.life"]],"onMiss":[["base_add.life","whittle_miss.life","safe_ess.def","reveal.life","first_chaos.life"]]},"first_chaos.life":{"hit":[["first_chaos.life","safe_ess.def","reveal.life","base_add.life"]],"onMiss":[["first_chaos.life","whittle_miss.life","safe_ess.def","reveal.life","base_add.life"]]}}
```

위반 경로 (채점 표기)

```text
safe_ess.def reveal.life base_add.life first_chaos.life
safe_ess.def whittle_miss.life reveal.life base_add.life first_chaos.life
reveal.life safe_ess.def base_add.life first_chaos.life
reveal.life whittle_miss.life safe_ess.def base_add.life first_chaos.life
base_add.life safe_ess.def reveal.life first_chaos.life
base_add.life whittle_miss.life safe_ess.def reveal.life first_chaos.life
first_chaos.life safe_ess.def reveal.life base_add.life
first_chaos.life whittle_miss.life safe_ess.def reveal.life base_add.life
```

### 요청 36 · F · 정순 · first_chaos.life

기대 번호 1 (위 expected의 1부터 시작하는 순번). 회수 1/1. 누락 없음. 규칙·출력 계약 위반 1개, 유효 중복 0개. 입력 816·출력 43토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"strategies":{"first_chaos":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함. 첫 획득에서만. 분열 보존.","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","whittle_miss":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략. 같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."},"helpers":["whittle_miss.life"],"crafted":{"used":0,"uses":["safe_ess"]},"afterFirst":{"first_chaos.life":{"def":["safe_ess.def"]}}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"first_chaos.life":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","safe_ess.def"]},"minItems":2,"maxItems":2}},"onMiss":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","safe_ess.def"]},"minItems":3,"maxItems":3}}},"required":["hit","onMiss"],"additionalProperties":false}},"required":["first_chaos.life"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"first_chaos.life":{"hit":[["first_chaos.life","safe_ess.def"]],"onMiss":[["first_chaos.life","safe_ess.def","safe_ess.def"]]}}
```

위반 경로 (채점 표기)

```text
first_chaos.life safe_ess.def safe_ess.def [branch]
```

### 요청 38 · F · 정순 · safe_ess.def

기대 번호 5, 6, 7 (위 expected의 1부터 시작하는 순번). 회수 3/3. 누락 없음. 규칙·출력 계약 위반 1개, 유효 중복 2개. 입력 833·출력 91토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"strategies":{"first_chaos":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함. 첫 획득에서만. 분열 보존.","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","whittle_miss":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략. 같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."},"helpers":["whittle_miss.life"],"crafted":{"used":0,"uses":["safe_ess"]},"afterFirst":{"safe_ess.def":{"life":["first_chaos.life","base_add.life","reveal.life"]}}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"safe_ess.def":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def","base_add.life","reveal.life"]},"minItems":2,"maxItems":2}},"onMiss":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def","base_add.life","reveal.life","whittle_miss.life"]},"minItems":3,"maxItems":3}}},"required":["hit","onMiss"],"additionalProperties":false}},"required":["safe_ess.def"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"safe_ess.def":{"hit":[["safe_ess.def","base_add.life"],["safe_ess.def","reveal.life"],["safe_ess.def","base_add.life"]],"onMiss":[["safe_ess.def","base_add.life","whittle_miss.life"],["safe_ess.def","reveal.life","whittle_miss.life"],["safe_ess.def","base_add.life","whittle_miss.life"]]}}
```

위반 경로 (채점 표기)

```text
safe_ess.def reveal.life whittle_miss.life
```

### 요청 40 · F · 정순 · base_add.life

기대 번호 2, 3 (위 expected의 1부터 시작하는 순번). 회수 2/2. 누락 없음. 규칙·출력 계약 위반 0개, 유효 중복 0개. 입력 814·출력 38토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"strategies":{"first_chaos":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함. 첫 획득에서만. 분열 보존.","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","whittle_miss":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략. 같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."},"helpers":["whittle_miss.life"],"crafted":{"used":0,"uses":["safe_ess"]},"afterFirst":{"base_add.life":{"def":["safe_ess.def"]}}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"base_add.life":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","safe_ess.def"]},"minItems":2,"maxItems":2}},"onMiss":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","safe_ess.def","whittle_miss.life"]},"minItems":3,"maxItems":3}}},"required":["hit","onMiss"],"additionalProperties":false}},"required":["base_add.life"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"base_add.life":{"hit":[["base_add.life","safe_ess.def"]],"onMiss":[["base_add.life","whittle_miss.life","safe_ess.def"]]}}
```

### 요청 42 · F · 정순 · reveal.life

기대 번호 4 (위 expected의 1부터 시작하는 순번). 회수 1/1. 누락 없음. 규칙·출력 계약 위반 1개, 유효 중복 0개. 입력 808·출력 37토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"rules":["시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."],"strategies":{"first_chaos":"첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함. 첫 획득에서만. 분열 보존.","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","whittle_miss":"해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략. 같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."},"helpers":["whittle_miss.life"],"crafted":{"used":0,"uses":["safe_ess"]},"afterFirst":{"reveal.life":{"def":["safe_ess.def"]}}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"reveal.life":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["reveal.life","safe_ess.def"]},"minItems":2,"maxItems":2}},"onMiss":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["reveal.life","safe_ess.def"]},"minItems":3,"maxItems":3}}},"required":["hit","onMiss"],"additionalProperties":false}},"required":["reveal.life"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"reveal.life":{"hit":[["reveal.life","safe_ess.def"]],"onMiss":[["reveal.life","safe_ess.def","safe_ess.def"]]}}
```

위반 경로 (채점 표기)

```text
reveal.life safe_ess.def safe_ess.def [branch]
```

### 요청 43 · G · 정순

기대 번호 1, 2, 3, 4, 5, 6, 7 (위 expected의 1부터 시작하는 순번). 회수 7/7. 누락 없음. 규칙·출력 계약 위반 1개, 유효 중복 2개. 입력 1039·출력 169토큰.

<details>
<summary>전송 JSON</summary>

```json
{
  "start": {
    "base": "Solar Amulet",
    "rarity": "rare",
    "ilvl": 82,
    "mods": {
      "prefix": [],
      "suffix": [
        {
          "id": "str",
          "fractured": true
        }
      ]
    },
    "craftedLimit": 1,
    "capacitySocket": 0
  },
  "targets": {
    "prefix": {
      "life": [
        "base",
        "Greater Essence of the Body",
        "Preserved Collarbone",
        "Ancient Collarbone",
        "Altered Collarbone"
      ],
      "def": [
        "Perfect Essence of Enhancement"
      ]
    },
    "suffix": {
      "str": [
        "base",
        "Preserved Collarbone",
        "Altered Collarbone"
      ]
    }
  },
  "rules": [
    "시작 베이스 고정. str은 이미 확보. 이 시나리오의 base_add는 첫 단계 및 후반에 모두 가능한 수동 작업대 후보."
  ],
  "strategies": {
    "first_chaos": "첫 카오스로 목표 일반 옵션 한 개 확보. 첫 변경 가능 옵션 준비 포함. 첫 획득에서만. 분열 보존.",
    "base_add": "빈자리에 엑잘로 목표 일반 옵션을 시도. Rare",
    "reveal": "대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다.",
    "safe_ess": "Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요",
    "whittle_miss": "해당 엑잘이 실패하고 제거 자격이 맞으면 절사 카오스로 같은 목표를 복구. 적중 시 생략. 같은 대상 base_add 바로 다음에만. 이 행동을 넣지 않는 기본 후보도 가능."
  },
  "helpers": [
    "whittle_miss.life"
  ],
  "crafted": {
    "used": 0,
    "uses": [
      "safe_ess"
    ]
  },
  "afterFirst": {
    "first_chaos.life": {
      "def": [
        "safe_ess.def"
      ]
    },
    "base_add.life": {
      "def": [
        "safe_ess.def"
      ]
    },
    "reveal.life": {
      "def": [
        "safe_ess.def"
      ]
    },
    "safe_ess.def": {
      "life": [
        "first_chaos.life",
        "base_add.life",
        "reveal.life"
      ]
    }
  }
}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"first_chaos.life":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["first_chaos.life","safe_ess.def"]},"minItems":2,"maxItems":2}}},"required":["hit"],"additionalProperties":false},"base_add.life":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","safe_ess.def"]},"minItems":2,"maxItems":2}},"onMiss":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","safe_ess.def","whittle_miss.life"]},"minItems":3,"maxItems":3}}},"required":["hit","onMiss"],"additionalProperties":false},"reveal.life":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["reveal.life","safe_ess.def"]},"minItems":2,"maxItems":2}}},"required":["hit"],"additionalProperties":false},"safe_ess.def":{"type":"object","properties":{"hit":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def","base_add.life","reveal.life"]},"minItems":2,"maxItems":2}},"onMiss":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def","base_add.life","reveal.life","whittle_miss.life"]},"minItems":3,"maxItems":3}}},"required":["hit","onMiss"],"additionalProperties":false}},"required":["first_chaos.life","base_add.life","reveal.life","safe_ess.def"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"first_chaos.life":{"hit":[["first_chaos.life","safe_ess.def"]] },"base_add.life":{"hit":[["base_add.life","safe_ess.def"]],"onMiss":[["base_add.life","whittle_miss.life","safe_ess.def"]]},"reveal.life":{"hit":[["reveal.life","safe_ess.def"]] },"safe_ess.def":{"hit":[["safe_ess.def","base_add.life"],["safe_ess.def","reveal.life"],["safe_ess.def","base_add.life"]] ,"onMiss":[["safe_ess.def","base_add.life","whittle_miss.life"],["safe_ess.def","reveal.life","whittle_miss.life"],["safe_ess.def","base_add.life","whittle_miss.life"]]}}
```

위반 경로 (채점 표기)

```text
safe_ess.def reveal.life whittle_miss.life
```

## W · 마법봉 · 시전 속도 선취득·중간 획득

사전에 고정한 입력 사실과 기대 경로. expected는 전송하지 않는다.

```json
{"title":"마법봉 · 시전 속도 선취득·중간 획득","start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"need":["mana_skill","skill","speed"],"actions":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed","cap_up.item"],"rules":["mana_skill과 skill은 서로 다른 Crafted. speed는 일반 접미. 세 옵션 모두 보존. 두 번째 Crafted 전 한도 확장 필수."],"expected":["safe_alloy.mana_skill safe_ess.skill base_add.speed","safe_alloy.mana_skill base_add.speed safe_ess.skill","safe_ess.skill safe_alloy.mana_skill base_add.speed","safe_ess.skill base_add.speed safe_alloy.mana_skill","base_add.speed safe_alloy.mana_skill safe_ess.skill","base_add.speed safe_ess.skill safe_alloy.mana_skill"]}
```

### 요청 2 · A · 정순

기대 번호 1, 2, 3, 4, 5, 6 (위 expected의 1부터 시작하는 순번). 회수 2/6. 누락 2, 4, 5, 6. 규칙·출력 계약 위반 0개, 유효 중복 0개. 입력 494·출력 61토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"need":["mana_skill","skill","speed"],"rules":["mana_skill과 skill은 서로 다른 Crafted. speed는 일반 접미. 세 옵션 모두 보존. 두 번째 Crafted 전 한도 확장 필수."],"actions":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed","cap_up.item"],"strategies":{"safe_alloy":{"effect":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택.","when":"Rare, Crafted 자리 1개 필요"},"safe_ess":{"effect":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함.","when":"Rare, Crafted 자리 1개 필요"},"base_add":{"effect":"빈자리에 엑잘로 목표 일반 옵션을 시도.","when":"Rare"},"cap_up":{"effect":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림.","when":"빈 적격 소켓, 한 번만. 대상 item."}}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"recipes":{"type":"array","items":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy","mana_skill","safe_ess","skill","base_add","speed","cap_up","item"]}}}}},"required":["recipes"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"recipes":[[["safe_alloy","mana_skill"],["cap_up","item"],["safe_ess","skill"],["base_add","speed"]],[["safe_ess","skill"],["cap_up","item"],["safe_alloy","mana_skill"],["base_add","speed"]]]}
```

### 요청 4 · A · 역순

기대 번호 1, 2, 3, 4, 5, 6 (위 expected의 1부터 시작하는 순번). 회수 0/6. 누락 1, 2, 3, 4, 5, 6. 규칙·출력 계약 위반 3개, 유효 중복 0개. 입력 493·출력 107토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"speed":["base"],"skill":["Perfect Essence of Sorcery"]}},"need":["speed","skill","mana_skill"],"rules":["mana_skill과 skill은 서로 다른 Crafted. speed는 일반 접미. 세 옵션 모두 보존. 두 번째 Crafted 전 한도 확장 필수."],"actions":["cap_up.item","base_add.speed","safe_ess.skill","safe_alloy.mana_skill"],"strategies":{"cap_up":{"effect":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림.","when":"빈 적격 소켓, 한 번만. 대상 item."},"base_add":{"effect":"빈자리에 엑잘로 목표 일반 옵션을 시도.","when":"Rare"},"safe_ess":{"effect":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함.","when":"Rare, Crafted 자리 1개 필요"},"safe_alloy":{"effect":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택.","when":"Rare, Crafted 자리 1개 필요"}}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"recipes":{"type":"array","items":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["cap_up","item","base_add","speed","safe_ess","skill","safe_alloy","mana_skill"]}}}}},"required":["recipes"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"recipes":[[["cap_up","item"],["base_add","speed"],["cap_up","item"],["safe_ess","skill"],["safe_alloy","mana_skill"]],[["cap_up","item"],["base_add","speed"],["safe_ess","skill"],["cap_up","item"],["safe_alloy","mana_skill"]],[["cap_up","item"],["base_add","speed"],["safe_alloy","mana_skill"],["cap_up","item"],["safe_ess","skill"]]]}
```

위반 경로 (채점 표기)

```text
cap_up.item base_add.speed cap_up.item safe_ess.skill safe_alloy.mana_skill [cap]
cap_up.item base_add.speed safe_ess.skill cap_up.item safe_alloy.mana_skill [cap]
cap_up.item base_add.speed safe_alloy.mana_skill cap_up.item safe_ess.skill [cap]
```

### 요청 8 · B · 정순

기대 번호 1, 2, 3, 4, 5, 6 (위 expected의 1부터 시작하는 순번). 회수 1/6. 누락 2, 3, 4, 5, 6. 규칙·출력 계약 위반 1개, 유효 중복 0개. 입력 587·출력 46토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"need":["mana_skill","skill","speed"],"rules":["mana_skill과 skill은 서로 다른 Crafted. speed는 일반 접미. 세 옵션 모두 보존. 두 번째 Crafted 전 한도 확장 필수."],"moves":{"safe_alloy":{"targets":["mana_skill"],"effect":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택.","when":"Rare, Crafted 자리 1개 필요"},"safe_ess":{"targets":["skill"],"effect":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함.","when":"Rare, Crafted 자리 1개 필요"},"base_add":{"targets":["speed"],"effect":"빈자리에 엑잘로 목표 일반 옵션을 시도.","when":"Rare"},"cap_up":{"targets":["item"],"effect":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림.","when":"빈 적격 소켓, 한 번만. 대상 item."}}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"recipes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed","cap_up.item"]}}}},"required":["recipes"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"recipes":[["safe_alloy.mana_skill","cap_up.item","safe_ess.skill","base_add.speed"],["safe_alloy.mana_skill","safe_ess.skill","cap_up.item","base_add.speed"]]}
```

위반 경로 (채점 표기)

```text
safe_alloy.mana_skill safe_ess.skill cap_up.item base_add.speed [cap]
```

### 요청 10 · B · 역순

기대 번호 1, 2, 3, 4, 5, 6 (위 expected의 1부터 시작하는 순번). 회수 1/6. 누락 1, 2, 3, 4, 5. 규칙·출력 계약 위반 0개, 유효 중복 1개. 입력 586·출력 46토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"speed":["base"],"skill":["Perfect Essence of Sorcery"]}},"need":["speed","skill","mana_skill"],"rules":["mana_skill과 skill은 서로 다른 Crafted. speed는 일반 접미. 세 옵션 모두 보존. 두 번째 Crafted 전 한도 확장 필수."],"moves":{"cap_up":{"targets":["item"],"effect":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림.","when":"빈 적격 소켓, 한 번만. 대상 item."},"base_add":{"targets":["speed"],"effect":"빈자리에 엑잘로 목표 일반 옵션을 시도.","when":"Rare"},"safe_ess":{"targets":["skill"],"effect":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함.","when":"Rare, Crafted 자리 1개 필요"},"safe_alloy":{"targets":["mana_skill"],"effect":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택.","when":"Rare, Crafted 자리 1개 필요"}}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"recipes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["cap_up.item","base_add.speed","safe_ess.skill","safe_alloy.mana_skill"]}}}},"required":["recipes"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"recipes":[["base_add.speed","cap_up.item","safe_ess.skill","safe_alloy.mana_skill"],["base_add.speed","safe_ess.skill","cap_up.item","safe_alloy.mana_skill"]]}
```

### 요청 14 · C · 정순

기대 번호 1, 2, 3, 4, 5, 6 (위 expected의 1부터 시작하는 순번). 회수 2/6. 누락 2, 4, 5, 6. 규칙·출력 계약 위반 0개, 유효 중복 0개. 입력 726·출력 62토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"need":["mana_skill","skill","speed"],"rules":["mana_skill과 skill은 서로 다른 Crafted. speed는 일반 접미. 세 옵션 모두 보존. 두 번째 Crafted 전 한도 확장 필수."],"moves":{"safe_alloy":{"targets":["mana_skill"],"effect":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택.","when":"Rare, Crafted 자리 1개 필요"},"safe_ess":{"targets":["skill"],"effect":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함.","when":"Rare, Crafted 자리 1개 필요"},"base_add":{"targets":["speed"],"effect":"빈자리에 엑잘로 목표 일반 옵션을 시도.","when":"Rare"},"cap_up":{"targets":["item"],"effect":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림.","when":"빈 적격 소켓, 한 번만. 대상 item."}},"openers":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed"]}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"safe_alloy.mana_skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed","cap_up.item"]}}},"safe_ess.skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed","cap_up.item"]}}},"base_add.speed":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed","cap_up.item"]}}}},"required":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"safe_alloy.mana_skill":[["safe_alloy.mana_skill","cap_up.item","safe_ess.skill","base_add.speed"]],"safe_ess.skill":[["safe_ess.skill","cap_up.item","safe_alloy.mana_skill","base_add.speed"]],"base_add.speed":[]}
```

### 요청 16 · C · 역순

기대 번호 1, 2, 3, 4, 5, 6 (위 expected의 1부터 시작하는 순번). 회수 1/6. 누락 1, 2, 3, 4, 5. 규칙·출력 계약 위반 2개, 유효 중복 0개. 입력 725·출력 75토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"speed":["base"],"skill":["Perfect Essence of Sorcery"]}},"need":["speed","skill","mana_skill"],"rules":["mana_skill과 skill은 서로 다른 Crafted. speed는 일반 접미. 세 옵션 모두 보존. 두 번째 Crafted 전 한도 확장 필수."],"moves":{"cap_up":{"targets":["item"],"effect":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림.","when":"빈 적격 소켓, 한 번만. 대상 item."},"base_add":{"targets":["speed"],"effect":"빈자리에 엑잘로 목표 일반 옵션을 시도.","when":"Rare"},"safe_ess":{"targets":["skill"],"effect":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함.","when":"Rare, Crafted 자리 1개 필요"},"safe_alloy":{"targets":["mana_skill"],"effect":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택.","when":"Rare, Crafted 자리 1개 필요"}},"openers":["base_add.speed","safe_ess.skill","safe_alloy.mana_skill"]}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"base_add.speed":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["cap_up.item","base_add.speed","safe_ess.skill","safe_alloy.mana_skill"]}}},"safe_ess.skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["cap_up.item","base_add.speed","safe_ess.skill","safe_alloy.mana_skill"]}}},"safe_alloy.mana_skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["cap_up.item","base_add.speed","safe_ess.skill","safe_alloy.mana_skill"]}}}},"required":["base_add.speed","safe_ess.skill","safe_alloy.mana_skill"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"base_add.speed":[["base_add.speed","cap_up.item","safe_ess.skill","safe_alloy.mana_skill"]],"safe_ess.skill":[["safe_ess.skill","cap_up.item","safe_alloy.mana_skill"]],"safe_alloy.mana_skill":[["safe_alloy.mana_skill","cap_up.item","safe_ess.skill"]]}
```

위반 경로 (채점 표기)

```text
safe_ess.skill safe_alloy.mana_skill
safe_alloy.mana_skill safe_ess.skill
```

### 요청 20 · C · 정순 · base_add.speed

기대 번호 5, 6 (위 expected의 1부터 시작하는 순번). 회수 1/2. 누락 6. 규칙·출력 계약 위반 0개, 유효 중복 0개. 입력 622·출력 27토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"need":["mana_skill","skill","speed"],"rules":["mana_skill과 skill은 서로 다른 Crafted. speed는 일반 접미. 세 옵션 모두 보존. 두 번째 Crafted 전 한도 확장 필수."],"moves":{"safe_alloy":{"targets":["mana_skill"],"effect":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택.","when":"Rare, Crafted 자리 1개 필요"},"safe_ess":{"targets":["skill"],"effect":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함.","when":"Rare, Crafted 자리 1개 필요"},"base_add":{"targets":["speed"],"effect":"빈자리에 엑잘로 목표 일반 옵션을 시도.","when":"Rare"},"cap_up":{"targets":["item"],"effect":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림.","when":"빈 적격 소켓, 한 번만. 대상 item."}},"openers":["base_add.speed"]}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"base_add.speed":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed","cap_up.item"]}}}},"required":["base_add.speed"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"base_add.speed":[["base_add.speed","cap_up.item","safe_alloy.mana_skill","safe_ess.skill"]]}
```

### 요청 22 · C · 정순 · safe_alloy.mana_skill

기대 번호 1, 2 (위 expected의 1부터 시작하는 순번). 회수 1/2. 누락 2. 규칙·출력 계약 위반 0개, 유효 중복 0개. 입력 628·출력 30토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"need":["mana_skill","skill","speed"],"rules":["mana_skill과 skill은 서로 다른 Crafted. speed는 일반 접미. 세 옵션 모두 보존. 두 번째 Crafted 전 한도 확장 필수."],"moves":{"safe_alloy":{"targets":["mana_skill"],"effect":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택.","when":"Rare, Crafted 자리 1개 필요"},"safe_ess":{"targets":["skill"],"effect":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함.","when":"Rare, Crafted 자리 1개 필요"},"base_add":{"targets":["speed"],"effect":"빈자리에 엑잘로 목표 일반 옵션을 시도.","when":"Rare"},"cap_up":{"targets":["item"],"effect":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림.","when":"빈 적격 소켓, 한 번만. 대상 item."}},"openers":["safe_alloy.mana_skill"]}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"safe_alloy.mana_skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed","cap_up.item"]}}}},"required":["safe_alloy.mana_skill"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"safe_alloy.mana_skill":[["safe_alloy.mana_skill","cap_up.item","safe_ess.skill","base_add.speed"]]}
```

### 요청 24 · C · 정순 · safe_ess.skill

기대 번호 3, 4 (위 expected의 1부터 시작하는 순번). 회수 2/2. 누락 없음. 규칙·출력 계약 위반 0개, 유효 중복 0개. 입력 624·출력 49토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"need":["mana_skill","skill","speed"],"rules":["mana_skill과 skill은 서로 다른 Crafted. speed는 일반 접미. 세 옵션 모두 보존. 두 번째 Crafted 전 한도 확장 필수."],"moves":{"safe_alloy":{"targets":["mana_skill"],"effect":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택.","when":"Rare, Crafted 자리 1개 필요"},"safe_ess":{"targets":["skill"],"effect":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함.","when":"Rare, Crafted 자리 1개 필요"},"base_add":{"targets":["speed"],"effect":"빈자리에 엑잘로 목표 일반 옵션을 시도.","when":"Rare"},"cap_up":{"targets":["item"],"effect":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림.","when":"빈 적격 소켓, 한 번만. 대상 item."}},"openers":["safe_ess.skill"]}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"safe_ess.skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed","cap_up.item"]}}}},"required":["safe_ess.skill"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"safe_ess.skill":[["safe_ess.skill","cap_up.item","safe_alloy.mana_skill","base_add.speed"],["safe_ess.skill","cap_up.item","base_add.speed","safe_alloy.mana_skill"]]}
```

### 요청 27 · D · 정순

기대 번호 1, 2, 3, 4, 5, 6 (위 expected의 1부터 시작하는 순번). 회수 6/6. 누락 없음. 규칙·출력 계약 위반 0개, 유효 중복 0개. 입력 826·출력 146토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"rules":["mana_skill과 skill은 서로 다른 Crafted. speed는 일반 접미. 세 옵션 모두 보존. 두 번째 Crafted 전 한도 확장 필수."],"openers":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed"],"goals":{"mana_skill":["safe_alloy.mana_skill"],"skill":["safe_ess.skill"],"speed":["base_add.speed"]},"strategies":{"safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item."},"helpers":["cap_up.item"],"crafted":{"used":0,"uses":["safe_alloy","safe_ess"]}}
```

</details>

응답 JSON Schema은 요청 14과 동일하다.

실제 응답 원문

```json
{"safe_alloy.mana_skill":[["safe_alloy.mana_skill","cap_up.item","safe_ess.skill","base_add.speed"],["safe_alloy.mana_skill","cap_up.item","base_add.speed","safe_ess.skill"]],"safe_ess.skill":[["safe_ess.skill","cap_up.item","safe_alloy.mana_skill","base_add.speed"],["safe_ess.skill","cap_up.item","base_add.speed","safe_alloy.mana_skill"]],"base_add.speed":[["base_add.speed","cap_up.item","safe_alloy.mana_skill","safe_ess.skill"],["base_add.speed","cap_up.item","safe_ess.skill","safe_alloy.mana_skill"]]}
```

### 요청 29 · D · 역순

기대 번호 1, 2, 3, 4, 5, 6 (위 expected의 1부터 시작하는 순번). 회수 6/6. 누락 없음. 규칙·출력 계약 위반 0개, 유효 중복 0개. 입력 826·출력 146토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"speed":["base"],"skill":["Perfect Essence of Sorcery"]}},"rules":["mana_skill과 skill은 서로 다른 Crafted. speed는 일반 접미. 세 옵션 모두 보존. 두 번째 Crafted 전 한도 확장 필수."],"openers":["base_add.speed","safe_ess.skill","safe_alloy.mana_skill"],"goals":{"speed":["base_add.speed"],"skill":["safe_ess.skill"],"mana_skill":["safe_alloy.mana_skill"]},"strategies":{"cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item.","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요"},"helpers":["cap_up.item"],"crafted":{"used":0,"uses":["safe_ess","safe_alloy"]}}
```

</details>

응답 JSON Schema은 요청 16과 동일하다.

실제 응답 원문

```json
{"base_add.speed":[["base_add.speed","cap_up.item","safe_ess.skill","safe_alloy.mana_skill"],["base_add.speed","cap_up.item","safe_alloy.mana_skill","safe_ess.skill"]],"safe_ess.skill":[["safe_ess.skill","cap_up.item","base_add.speed","safe_alloy.mana_skill"],["safe_ess.skill","cap_up.item","safe_alloy.mana_skill","base_add.speed"]],"safe_alloy.mana_skill":[["safe_alloy.mana_skill","cap_up.item","base_add.speed","safe_ess.skill"],["safe_alloy.mana_skill","cap_up.item","safe_ess.skill","base_add.speed"]]}
```

### 요청 33 · E · 정순

기대 번호 1, 2, 3, 4, 5, 6 (위 expected의 1부터 시작하는 순번). 회수 3/6. 누락 2, 4, 6. 규칙·출력 계약 위반 0개, 유효 중복 0개. 입력 781·출력 83토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"rules":["mana_skill과 skill은 서로 다른 Crafted. speed는 일반 접미. 세 옵션 모두 보존. 두 번째 Crafted 전 한도 확장 필수."],"goals":{"mana_skill":["safe_alloy.mana_skill"],"skill":["safe_ess.skill"],"speed":["base_add.speed"]},"strategies":{"safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item."},"helpers":["cap_up.item"],"crafted":{"used":0,"uses":["safe_alloy","safe_ess"]},"remainingAfterFirst":{"safe_alloy.mana_skill":["skill","speed"],"safe_ess.skill":["mana_skill","speed"],"base_add.speed":["mana_skill","skill"]}}
```

</details>

응답 JSON Schema은 요청 14과 동일하다.

실제 응답 원문

```json
{"safe_alloy.mana_skill":[["safe_alloy.mana_skill","cap_up.item","safe_ess.skill","base_add.speed"]],"safe_ess.skill":[["safe_ess.skill","cap_up.item","safe_alloy.mana_skill","base_add.speed"]],"base_add.speed":[["cap_up.item","base_add.speed","safe_alloy.mana_skill","safe_ess.skill"]]}
```

### 요청 35 · E · 역순

기대 번호 1, 2, 3, 4, 5, 6 (위 expected의 1부터 시작하는 순번). 회수 3/6. 누락 1, 3, 5. 규칙·출력 계약 위반 0개, 유효 중복 0개. 입력 781·출력 83토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"speed":["base"],"skill":["Perfect Essence of Sorcery"]}},"rules":["mana_skill과 skill은 서로 다른 Crafted. speed는 일반 접미. 세 옵션 모두 보존. 두 번째 Crafted 전 한도 확장 필수."],"goals":{"speed":["base_add.speed"],"skill":["safe_ess.skill"],"mana_skill":["safe_alloy.mana_skill"]},"strategies":{"cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item.","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요"},"helpers":["cap_up.item"],"crafted":{"used":0,"uses":["safe_ess","safe_alloy"]},"remainingAfterFirst":{"base_add.speed":["skill","mana_skill"],"safe_ess.skill":["speed","mana_skill"],"safe_alloy.mana_skill":["speed","skill"]}}
```

</details>

응답 JSON Schema은 요청 16과 동일하다.

실제 응답 원문

```json
{"base_add.speed":[["base_add.speed","cap_up.item","safe_ess.skill","safe_alloy.mana_skill"]],"safe_ess.skill":[["safe_ess.skill","cap_up.item","base_add.speed","safe_alloy.mana_skill"]],"safe_alloy.mana_skill":[["safe_alloy.mana_skill","cap_up.item","base_add.speed","safe_ess.skill"]]}
```

### 요청 37 · F · 정순 · base_add.speed

기대 번호 5, 6 (위 expected의 1부터 시작하는 순번). 회수 1/2. 누락 6. 규칙·출력 계약 위반 0개, 유효 중복 0개. 입력 695·출력 27토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"rules":["mana_skill과 skill은 서로 다른 Crafted. speed는 일반 접미. 세 옵션 모두 보존. 두 번째 Crafted 전 한도 확장 필수."],"strategies":{"safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item."},"helpers":["cap_up.item"],"crafted":{"used":0,"uses":["safe_alloy","safe_ess"]},"afterFirst":{"base_add.speed":{"mana_skill":["safe_alloy.mana_skill"],"skill":["safe_ess.skill"]}}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"base_add.speed":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.speed","safe_alloy.mana_skill","safe_ess.skill","cap_up.item"]},"minItems":3,"maxItems":4}}},"required":["base_add.speed"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"base_add.speed":[["base_add.speed","cap_up.item","safe_alloy.mana_skill","safe_ess.skill"]]}
```

### 요청 39 · F · 정순 · safe_alloy.mana_skill

기대 번호 1, 2 (위 expected의 1부터 시작하는 순번). 회수 1/2. 누락 2. 규칙·출력 계약 위반 0개, 유효 중복 0개. 입력 697·출력 30토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"rules":["mana_skill과 skill은 서로 다른 Crafted. speed는 일반 접미. 세 옵션 모두 보존. 두 번째 Crafted 전 한도 확장 필수."],"strategies":{"safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item."},"helpers":["cap_up.item"],"crafted":{"used":0,"uses":["safe_alloy","safe_ess"]},"afterFirst":{"safe_alloy.mana_skill":{"skill":["safe_ess.skill"],"speed":["base_add.speed"]}}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"safe_alloy.mana_skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy.mana_skill","safe_ess.skill","base_add.speed","cap_up.item"]},"minItems":3,"maxItems":4}}},"required":["safe_alloy.mana_skill"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"safe_alloy.mana_skill":[["cap_up.item","safe_alloy.mana_skill","safe_ess.skill","base_add.speed"]]}
```

### 요청 41 · F · 정순 · safe_ess.skill

기대 번호 3, 4 (위 expected의 1부터 시작하는 순번). 회수 1/2. 누락 4. 규칙·출력 계약 위반 0개, 유효 중복 0개. 입력 696·출력 28토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"rules":["mana_skill과 skill은 서로 다른 Crafted. speed는 일반 접미. 세 옵션 모두 보존. 두 번째 Crafted 전 한도 확장 필수."],"strategies":{"safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item."},"helpers":["cap_up.item"],"crafted":{"used":0,"uses":["safe_alloy","safe_ess"]},"afterFirst":{"safe_ess.skill":{"mana_skill":["safe_alloy.mana_skill"],"speed":["base_add.speed"]}}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"safe_ess.skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","safe_alloy.mana_skill","base_add.speed","cap_up.item"]},"minItems":3,"maxItems":4}}},"required":["safe_ess.skill"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"safe_ess.skill":[["cap_up.item","safe_ess.skill","safe_alloy.mana_skill","base_add.speed"]]}
```

### 요청 44 · G · 정순

기대 번호 1, 2, 3, 4, 5, 6 (위 expected의 1부터 시작하는 순번). 회수 6/6. 누락 없음. 규칙·출력 계약 위반 0개, 유효 중복 0개. 입력 826·출력 146토큰.

전송 JSON은 요청 27과 동일하다.

응답 JSON Schema은 요청 14과 동일하다.

실제 응답 원문

```json
{"safe_alloy.mana_skill":[["safe_alloy.mana_skill","cap_up.item","safe_ess.skill","base_add.speed"],["safe_alloy.mana_skill","cap_up.item","base_add.speed","safe_ess.skill"]],"safe_ess.skill":[["safe_ess.skill","cap_up.item","safe_alloy.mana_skill","base_add.speed"],["safe_ess.skill","cap_up.item","base_add.speed","safe_alloy.mana_skill"]],"base_add.speed":[["base_add.speed","cap_up.item","safe_alloy.mana_skill","safe_ess.skill"],["base_add.speed","cap_up.item","safe_ess.skill","safe_alloy.mana_skill"]]}
```

## G · Magic 장갑 · 생명력 승급 대안

사전에 고정한 입력 사실과 기대 경로. expected는 전송하지 않는다.

```json
{"title":"Magic 장갑 · 생명력 승급 대안","start":{"base":"Stocky Mitts","rarity":"magic","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"filler"}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body"],"pen":["Prismatic Alloy"]},"suffix":{}},"need":["life","pen"],"actions":["upgrade_ess.life","regal_base.life","safe_alloy.pen"],"rules":[],"expected":["upgrade_ess.life safe_alloy.pen","regal_base.life safe_alloy.pen"]}
```

### 요청 5 · A · 정순

기대 번호 1, 2 (위 expected의 1부터 시작하는 순번). 회수 0/2. 누락 1, 2. 규칙·출력 계약 위반 1개, 유효 중복 0개. 입력 408·출력 27토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Stocky Mitts","rarity":"magic","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"filler"}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body"],"pen":["Prismatic Alloy"]},"suffix":{}},"need":["life","pen"],"rules":[],"actions":["upgrade_ess.life","regal_base.life","safe_alloy.pen"],"strategies":{"upgrade_ess":{"effect":"등급 상승용 에센스로 목표 일반 옵션 획득.","when":"Magic → Rare"},"regal_base":{"effect":"제왕으로 Magic을 Rare로 올리며 해당 일반 옵션이 뜬 결과 채택.","when":"Magic → Rare"},"safe_alloy":{"effect":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택.","when":"Rare, Crafted 자리 1개 필요"}}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"recipes":{"type":"array","items":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["upgrade_ess","life","regal_base","safe_alloy","pen"]}}}}},"required":["recipes"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"recipes":[[["upgrade_ess","life"],["regal_base","life"],["safe_alloy","pen"]]]}
```

위반 경로 (채점 표기)

```text
upgrade_ess.life regal_base.life safe_alloy.pen
```

### 요청 11 · B · 정순

기대 번호 1, 2 (위 expected의 1부터 시작하는 순번). 회수 2/2. 누락 없음. 규칙·출력 계약 위반 0개, 유효 중복 0개. 입력 502·출력 26토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Stocky Mitts","rarity":"magic","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"filler"}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body"],"pen":["Prismatic Alloy"]},"suffix":{}},"need":["life","pen"],"rules":[],"moves":{"upgrade_ess":{"targets":["life"],"effect":"등급 상승용 에센스로 목표 일반 옵션 획득.","when":"Magic → Rare"},"regal_base":{"targets":["life"],"effect":"제왕으로 Magic을 Rare로 올리며 해당 일반 옵션이 뜬 결과 채택.","when":"Magic → Rare"},"safe_alloy":{"targets":["pen"],"effect":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택.","when":"Rare, Crafted 자리 1개 필요"}}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"recipes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["upgrade_ess.life","regal_base.life","safe_alloy.pen"]}}}},"required":["recipes"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"recipes":[["upgrade_ess.life","safe_alloy.pen"],["regal_base.life","safe_alloy.pen"]]}
```

### 요청 17 · C · 정순

기대 번호 1, 2 (위 expected의 1부터 시작하는 순번). 회수 2/2. 누락 없음. 규칙·출력 계약 위반 0개, 유효 중복 0개. 입력 629·출력 41토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Stocky Mitts","rarity":"magic","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"filler"}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body"],"pen":["Prismatic Alloy"]},"suffix":{}},"need":["life","pen"],"rules":[],"moves":{"upgrade_ess":{"targets":["life"],"effect":"등급 상승용 에센스로 목표 일반 옵션 획득.","when":"Magic → Rare"},"regal_base":{"targets":["life"],"effect":"제왕으로 Magic을 Rare로 올리며 해당 일반 옵션이 뜬 결과 채택.","when":"Magic → Rare"},"safe_alloy":{"targets":["pen"],"effect":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택.","when":"Rare, Crafted 자리 1개 필요"}},"openers":["upgrade_ess.life","regal_base.life","safe_alloy.pen"]}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"upgrade_ess.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["upgrade_ess.life","regal_base.life","safe_alloy.pen"]}}},"regal_base.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["upgrade_ess.life","regal_base.life","safe_alloy.pen"]}}},"safe_alloy.pen":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["upgrade_ess.life","regal_base.life","safe_alloy.pen"]}}}},"required":["upgrade_ess.life","regal_base.life","safe_alloy.pen"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"upgrade_ess.life":[["upgrade_ess.life","safe_alloy.pen"]],"regal_base.life":[["regal_base.life","safe_alloy.pen"]],"safe_alloy.pen":[]}
```

### 요청 30 · D · 정순

기대 번호 1, 2 (위 expected의 1부터 시작하는 순번). 회수 0/2. 누락 1, 2. 규칙·출력 계약 위반 18개, 유효 중복 0개. 입력 731·출력 307토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Stocky Mitts","rarity":"magic","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"filler"}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body"],"pen":["Prismatic Alloy"]},"suffix":{}},"rules":[],"openers":["upgrade_ess.life","regal_base.life","safe_alloy.pen"],"goals":{"life":["upgrade_ess.life","regal_base.life"],"pen":["safe_alloy.pen"]},"strategies":{"upgrade_ess":"등급 상승용 에센스로 목표 일반 옵션 획득. Magic → Rare","regal_base":"제왕으로 Magic을 Rare로 올리며 해당 일반 옵션이 뜬 결과 채택. Magic → Rare","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요"},"helpers":[],"crafted":{"used":0,"uses":["safe_alloy"]}}
```

</details>

응답 JSON Schema은 요청 17과 동일하다.

실제 응답 원문

```json
{"upgrade_ess.life":[["upgrade_ess.life","regal_base.life","safe_alloy.pen"],["upgrade_ess.life","safe_alloy.pen","regal_base.life"],["regal_base.life","upgrade_ess.life","safe_alloy.pen"],["regal_base.life","safe_alloy.pen","upgrade_ess.life"],["safe_alloy.pen","upgrade_ess.life","regal_base.life"],["safe_alloy.pen","regal_base.life","upgrade_ess.life"]],"regal_base.life":[["regal_base.life","upgrade_ess.life","safe_alloy.pen"],["regal_base.life","safe_alloy.pen","upgrade_ess.life"],["upgrade_ess.life","regal_base.life","safe_alloy.pen"],["upgrade_ess.life","safe_alloy.pen","regal_base.life"],["safe_alloy.pen","regal_base.life","upgrade_ess.life"],["safe_alloy.pen","upgrade_ess.life","regal_base.life"]],"safe_alloy.pen":[["safe_alloy.pen","upgrade_ess.life","regal_base.life"],["safe_alloy.pen","regal_base.life","upgrade_ess.life"],["upgrade_ess.life","safe_alloy.pen","regal_base.life"],["upgrade_ess.life","regal_base.life","safe_alloy.pen"],["regal_base.life","safe_alloy.pen","upgrade_ess.life"],["regal_base.life","upgrade_ess.life","safe_alloy.pen"]]}
```

위반 경로 (채점 표기)

```text
upgrade_ess.life regal_base.life safe_alloy.pen
upgrade_ess.life safe_alloy.pen regal_base.life
regal_base.life upgrade_ess.life safe_alloy.pen
regal_base.life safe_alloy.pen upgrade_ess.life
safe_alloy.pen upgrade_ess.life regal_base.life
safe_alloy.pen regal_base.life upgrade_ess.life
regal_base.life upgrade_ess.life safe_alloy.pen
regal_base.life safe_alloy.pen upgrade_ess.life
upgrade_ess.life regal_base.life safe_alloy.pen
upgrade_ess.life safe_alloy.pen regal_base.life
safe_alloy.pen regal_base.life upgrade_ess.life
safe_alloy.pen upgrade_ess.life regal_base.life
safe_alloy.pen upgrade_ess.life regal_base.life
safe_alloy.pen regal_base.life upgrade_ess.life
upgrade_ess.life safe_alloy.pen regal_base.life
upgrade_ess.life regal_base.life safe_alloy.pen
regal_base.life safe_alloy.pen upgrade_ess.life
regal_base.life upgrade_ess.life safe_alloy.pen
```

### 요청 45 · G · 정순

기대 번호 1, 2 (위 expected의 1부터 시작하는 순번). 회수 2/2. 누락 없음. 규칙·출력 계약 위반 2개, 유효 중복 0개. 입력 706·출력 63토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Stocky Mitts","rarity":"magic","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"filler"}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body"],"pen":["Prismatic Alloy"]},"suffix":{}},"rules":[],"strategies":{"upgrade_ess":"등급 상승용 에센스로 목표 일반 옵션 획득. Magic → Rare","regal_base":"제왕으로 Magic을 Rare로 올리며 해당 일반 옵션이 뜬 결과 채택. Magic → Rare","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요"},"crafted":{"used":0,"uses":["safe_alloy"]},"afterFirst":{"upgrade_ess.life":{"pen":["safe_alloy.pen"]},"regal_base.life":{"pen":["safe_alloy.pen"]},"safe_alloy.pen":{"life":["upgrade_ess.life","regal_base.life"]}}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"upgrade_ess.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["upgrade_ess.life","safe_alloy.pen"]},"minItems":2,"maxItems":2}},"regal_base.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["regal_base.life","safe_alloy.pen"]},"minItems":2,"maxItems":2}},"safe_alloy.pen":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_alloy.pen","upgrade_ess.life","regal_base.life"]},"minItems":2,"maxItems":2}}},"required":["upgrade_ess.life","regal_base.life","safe_alloy.pen"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"upgrade_ess.life":[["upgrade_ess.life","safe_alloy.pen"]],"regal_base.life":[["regal_base.life","safe_alloy.pen"]],"safe_alloy.pen":[["safe_alloy.pen","upgrade_ess.life"],["safe_alloy.pen","regal_base.life"]]}
```

위반 경로 (채점 표기)

```text
safe_alloy.pen upgrade_ess.life
safe_alloy.pen regal_base.life
```

## R · 반지 · 두 일반 목표와 훼손 슬롯 한 개

사전에 고정한 입력 사실과 기대 경로. expected는 전송하지 않는다.

```json
{"title":"반지 · 두 일반 목표와 훼손 슬롯 한 개","start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"cold":["base","Preserved Collarbone"]}},"need":["life","cold"],"actions":["base_add.life","reveal.life","base_add.cold","reveal.cold"],"rules":["이 요청은 엑잘과 훼손 획득 경로를 비교한다. 확보한 목표를 제거하지 않는다."],"expected":["base_add.life base_add.cold","base_add.cold base_add.life","reveal.life base_add.cold","base_add.cold reveal.life","base_add.life reveal.cold","reveal.cold base_add.life"]}
```

### 요청 6 · A · 정순

기대 번호 1, 2, 3, 4, 5, 6 (위 expected의 1부터 시작하는 순번). 회수 3/6. 누락 2, 4, 6. 규칙·출력 계약 위반 1개, 유효 중복 0개. 입력 404·출력 57토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"cold":["base","Preserved Collarbone"]}},"need":["life","cold"],"rules":["이 요청은 엑잘과 훼손 획득 경로를 비교한다. 확보한 목표를 제거하지 않는다."],"actions":["base_add.life","reveal.life","base_add.cold","reveal.cold"],"strategies":{"base_add":{"effect":"빈자리에 엑잘로 목표 일반 옵션을 시도.","when":"Rare"},"reveal":{"effect":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택.","when":"훼손 슬롯이 아직 없어야 한다."}}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"recipes":{"type":"array","items":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add","life","reveal","cold"]}}}}},"required":["recipes"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"recipes":[[["base_add","life"],["base_add","cold"]],[["reveal","life"],["base_add","cold"]],[["base_add","life"],["reveal","cold"]],[["reveal","life"],["reveal","cold"]]]}
```

위반 경로 (채점 표기)

```text
reveal.life reveal.cold
```

### 요청 12 · B · 정순

기대 번호 1, 2, 3, 4, 5, 6 (위 expected의 1부터 시작하는 순번). 회수 2/6. 누락 1, 2, 4, 6. 규칙·출력 계약 위반 0개, 유효 중복 0개. 입력 503·출력 24토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"cold":["base","Preserved Collarbone"]}},"need":["life","cold"],"rules":["이 요청은 엑잘과 훼손 획득 경로를 비교한다. 확보한 목표를 제거하지 않는다."],"moves":{"base_add":{"targets":["life","cold"],"effect":"빈자리에 엑잘로 목표 일반 옵션을 시도.","when":"Rare"},"reveal":{"targets":["life","cold"],"effect":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택.","when":"훼손 슬롯이 아직 없어야 한다."}}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"recipes":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","reveal.life","base_add.cold","reveal.cold"]}}}},"required":["recipes"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"recipes":[["reveal.life","base_add.cold"],["base_add.life","reveal.cold"]]}
```

### 요청 18 · C · 정순

기대 번호 1, 2, 3, 4, 5, 6 (위 expected의 1부터 시작하는 순번). 회수 6/6. 누락 없음. 규칙·출력 계약 위반 2개, 유효 중복 0개. 입력 680·출력 103토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"cold":["base","Preserved Collarbone"]}},"need":["life","cold"],"rules":["이 요청은 엑잘과 훼손 획득 경로를 비교한다. 확보한 목표를 제거하지 않는다."],"moves":{"base_add":{"targets":["life","cold"],"effect":"빈자리에 엑잘로 목표 일반 옵션을 시도.","when":"Rare"},"reveal":{"targets":["life","cold"],"effect":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택.","when":"훼손 슬롯이 아직 없어야 한다."}},"openers":["base_add.life","reveal.life","base_add.cold","reveal.cold"]}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"base_add.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","reveal.life","base_add.cold","reveal.cold"]}}},"reveal.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","reveal.life","base_add.cold","reveal.cold"]}}},"base_add.cold":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","reveal.life","base_add.cold","reveal.cold"]}}},"reveal.cold":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","reveal.life","base_add.cold","reveal.cold"]}}}},"required":["base_add.life","reveal.life","base_add.cold","reveal.cold"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"base_add.life":[["base_add.life","reveal.cold"],["base_add.life","base_add.cold"]],"reveal.life":[["reveal.life","base_add.cold"],["reveal.life","reveal.cold"]],"base_add.cold":[["base_add.cold","reveal.life"],["base_add.cold","base_add.life"]],"reveal.cold":[["reveal.cold","base_add.life"],["reveal.cold","reveal.life"]]}
```

위반 경로 (채점 표기)

```text
reveal.life reveal.cold
reveal.cold reveal.life
```

### 요청 31 · D · 정순

기대 번호 1, 2, 3, 4, 5, 6 (위 expected의 1부터 시작하는 순번). 회수 0/6. 누락 1, 2, 3, 4, 5, 6. 규칙·출력 계약 위반 12개, 유효 중복 0개. 입력 785·출력 197토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"cold":["base","Preserved Collarbone"]}},"rules":["이 요청은 엑잘과 훼손 획득 경로를 비교한다. 확보한 목표를 제거하지 않는다."],"openers":["base_add.life","reveal.life","base_add.cold","reveal.cold"],"goals":{"life":["base_add.life","reveal.life"],"cold":["base_add.cold","reveal.cold"]},"strategies":{"base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다."},"helpers":[],"crafted":{"used":0,"uses":[]}}
```

</details>

응답 JSON Schema은 요청 18과 동일하다.

실제 응답 원문

```json
{"base_add.life":[["base_add.life","reveal.life"],["base_add.life","base_add.cold","reveal.cold","reveal.life"],["base_add.life","reveal.cold","reveal.life"]],"reveal.life":[["reveal.life","base_add.life"],["reveal.life","base_add.cold","reveal.cold","base_add.life"],["reveal.life","reveal.cold","base_add.life"]],"base_add.cold":[["base_add.cold","reveal.cold"],["base_add.cold","base_add.life","reveal.life","reveal.cold"],["base_add.cold","reveal.life","reveal.cold"]],"reveal.cold":[["reveal.cold","base_add.cold"],["reveal.cold","base_add.life","reveal.life","base_add.cold"],["reveal.cold","reveal.life","base_add.cold"]]}
```

위반 경로 (채점 표기)

```text
base_add.life reveal.life
base_add.life base_add.cold reveal.cold reveal.life
base_add.life reveal.cold reveal.life
reveal.life base_add.life
reveal.life base_add.cold reveal.cold base_add.life
reveal.life reveal.cold base_add.life
base_add.cold reveal.cold
base_add.cold base_add.life reveal.life reveal.cold
base_add.cold reveal.life reveal.cold
reveal.cold base_add.cold
reveal.cold base_add.life reveal.life base_add.cold
reveal.cold reveal.life base_add.cold
```

### 요청 46 · G · 정순

기대 번호 1, 2, 3, 4, 5, 6 (위 expected의 1부터 시작하는 순번). 회수 6/6. 누락 없음. 규칙·출력 계약 위반 2개, 유효 중복 0개. 입력 759·출력 103토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Iron Ring","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"cold":["base","Preserved Collarbone"]}},"rules":["이 요청은 엑잘과 훼손 획득 경로를 비교한다. 확보한 목표를 제거하지 않는다."],"strategies":{"base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare","reveal":"대상 티어용 훼손 재료를 쓰고 목표가 제시되면 선택. 훼손 슬롯이 아직 없어야 한다."},"afterFirst":{"base_add.life":{"cold":["base_add.cold","reveal.cold"]},"reveal.life":{"cold":["base_add.cold","reveal.cold"]},"base_add.cold":{"life":["base_add.life","reveal.life"]},"reveal.cold":{"life":["base_add.life","reveal.life"]}}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"base_add.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.life","base_add.cold","reveal.cold"]},"minItems":2,"maxItems":2}},"reveal.life":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["reveal.life","base_add.cold","reveal.cold"]},"minItems":2,"maxItems":2}},"base_add.cold":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["base_add.cold","base_add.life","reveal.life"]},"minItems":2,"maxItems":2}},"reveal.cold":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["reveal.cold","base_add.life","reveal.life"]},"minItems":2,"maxItems":2}}},"required":["base_add.life","reveal.life","base_add.cold","reveal.cold"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"base_add.life":[["base_add.life","base_add.cold"],["base_add.life","reveal.cold"]],"reveal.life":[["reveal.life","base_add.cold"],["reveal.life","reveal.cold"]],"base_add.cold":[["base_add.cold","base_add.life"],["base_add.cold","reveal.life"]],"reveal.cold":[["reveal.cold","base_add.life"],["reveal.cold","reveal.life"]]}
```

위반 경로 (채점 표기)

```text
reveal.life reveal.cold
reveal.cold reveal.life
```

## H1 · 별도 평가 · 목걸이의 생명력과 분열 힘 이미 확보

사전에 고정한 입력 사실과 기대 경로. expected는 전송하지 않는다.

```json
{"title":"별도 평가 · 목걸이의 생명력과 분열 힘 이미 확보","start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","value":90}],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"need":["def"],"actions":["safe_ess.def"],"rules":[],"expected":["safe_ess.def"]}
```

### 요청 47 · G · 정순

기대 번호 1 (위 expected의 1부터 시작하는 순번). 회수 1/1. 누락 없음. 규칙·출력 계약 위반 0개, 유효 중복 0개. 입력 580·출력 13토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","value":90}],"suffix":[{"id":"str","fractured":true}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"rules":[],"strategies":{"safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요"},"crafted":{"used":0,"uses":["safe_ess"]},"afterFirst":{"safe_ess.def":{}}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"safe_ess.def":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.def"]},"minItems":1,"maxItems":1}}},"required":["safe_ess.def"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"safe_ess.def":[["safe_ess.def"]]}
```

## H2 · 별도 평가 · 느린 준비가 필요한 기존 합금 마법봉

사전에 고정한 입력 사실과 기대 경로. expected는 전송하지 않는다.

```json
{"title":"별도 평가 · 느린 준비가 필요한 기존 합금 마법봉","start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"need":["skill","speed"],"actions":["cap_up.item","safe_ess.skill","base_add.speed"],"rules":[],"expected":["safe_ess.skill base_add.speed","base_add.speed safe_ess.skill"]}
```

### 요청 48 · G · 정순

기대 번호 1, 2 (위 expected의 1부터 시작하는 순번). 회수 0/2. 누락 1, 2. 규칙·출력 계약 위반 2개, 유효 중복 0개. 입력 685·출력 27토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]},"craftedLimit":1,"capacitySocket":1},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"rules":[],"openers":["safe_ess.skill","base_add.speed"],"goals":{"skill":["safe_ess.skill"],"speed":["base_add.speed"]},"strategies":{"cap_up":"Astrid's Creativity로 이 장비의 Crafted 한도를 1 늘림. 빈 적격 소켓, 한 번만. 대상 item.","safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","base_add":"빈자리에 엑잘로 목표 일반 옵션을 시도. Rare"},"helpers":["cap_up.item"],"crafted":{"used":1,"uses":["safe_ess"]}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"safe_ess.skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["cap_up.item","safe_ess.skill","base_add.speed"]}}},"base_add.speed":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["cap_up.item","safe_ess.skill","base_add.speed"]}}}},"required":["safe_ess.skill","base_add.speed"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"safe_ess.skill":[["cap_up.item","safe_ess.skill"]],"base_add.speed":[["base_add.speed"]]}
```

위반 경로 (채점 표기)

```text
safe_ess.skill
base_add.speed [cap]
```

## H3 · 별도 평가 · 확장 소켓 없는 마법봉

사전에 고정한 입력 사실과 기대 경로. expected는 전송하지 않는다.

```json
{"title":"별도 평가 · 확장 소켓 없는 마법봉","start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"]}},"need":["mana_skill","skill"],"actions":["safe_ess.skill","safe_alloy.mana_skill"],"rules":["Crafted 한도를 올릴 수단이 없다."],"expected":["NONE"]}
```

### 요청 49 · G · 정순

기대 번호 1 (위 expected의 1부터 시작하는 순번). 회수 0/1. 누락 1. 규칙·출력 계약 위반 4개, 유효 중복 0개. 입력 673·출력 55토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Attuned Wand","rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"]}},"rules":["Crafted 한도를 올릴 수단이 없다."],"openers":["safe_ess.skill","safe_alloy.mana_skill"],"goals":{"mana_skill":["safe_alloy.mana_skill"],"skill":["safe_ess.skill"]},"strategies":{"safe_ess":"Perfect Essence로 목표 Crafted 옵션 획득. 제거용 더미와 보존 징조 준비 포함. Rare, Crafted 자리 1개 필요","safe_alloy":"Alloy로 목표 Crafted 옵션 획득. 더미 준비 포함, 기존 목표 보존 결과 채택. Rare, Crafted 자리 1개 필요"},"helpers":[],"crafted":{"used":0,"uses":["safe_ess","safe_alloy"]}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"safe_ess.skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","safe_alloy.mana_skill"]}}},"safe_alloy.mana_skill":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["safe_ess.skill","safe_alloy.mana_skill"]}}}},"required":["safe_ess.skill","safe_alloy.mana_skill"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"safe_ess.skill":[["safe_ess.skill"],["safe_alloy.mana_skill","safe_ess.skill"]],"safe_alloy.mana_skill":[["safe_alloy.mana_skill"],["safe_ess.skill","safe_alloy.mana_skill"]]}
```

위반 경로 (채점 표기)

```text
safe_ess.skill
safe_alloy.mana_skill safe_ess.skill
safe_alloy.mana_skill
safe_ess.skill safe_alloy.mana_skill
```

## H4 · 별도 평가 · 품질과 수치만 남은 목걸이

사전에 고정한 입력 사실과 기대 경로. expected는 전송하지 않는다.

```json
{"title":"별도 평가 · 품질과 수치만 남은 목걸이","start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","raw":99}],"suffix":[{"id":"str","raw":7}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"need":["Flesh20","values"],"actions":["flesh.item","divine.item"],"rules":["두 옵션은 보유. Flesh20과 원래 굴림 99·8이면 합 127로 완성. 현재 품질 0, 99·7은 품질만 올리면 126으로 미달. 품질과 재굴림은 어느 순서도 가능."],"expected":["flesh.item divine.item","divine.item flesh.item"]}
```

### 요청 50 · G · 정순

기대 번호 1, 2 (위 expected의 1부터 시작하는 순번). 회수 2/2. 누락 없음. 규칙·출력 계약 위반 1개, 유효 중복 0개. 입력 680·출력 38토큰.

<details>
<summary>전송 JSON</summary>

```json
{"start":{"base":"Solar Amulet","rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","raw":99}],"suffix":[{"id":"str","raw":7}]},"craftedLimit":1,"capacitySocket":0},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"str":["base","Preserved Collarbone","Altered Collarbone"]}},"rules":["두 옵션은 보유. Flesh20과 원래 굴림 99·8이면 합 127로 완성. 현재 품질 0, 99·7은 품질만 올리면 126으로 미달. 품질과 재굴림은 어느 순서도 가능."],"strategies":{"flesh":"Flesh 품질 20 적용. 기존 수치 유지. 대상 item","divine":"두 옵션 수치를 현재 티어 안에서 함께 굴린다. 변경 가능 아이템, 대상 item"},"afterFirst":{"flesh.item":{"values":["divine.item"]},"divine.item":{"Flesh20":["flesh.item"]}}}
```

</details>

<details>
<summary>응답 JSON Schema</summary>

```json
{"type":"object","properties":{"flesh.item":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["flesh.item","divine.item"]},"minItems":2,"maxItems":2}},"divine.item":{"type":"array","items":{"type":"array","items":{"type":"string","enum":["divine.item","flesh.item"]},"minItems":2,"maxItems":2}}},"required":["flesh.item","divine.item"],"additionalProperties":false}
```

</details>

실제 응답 원문

```json
{"flesh.item":[["flesh.item","divine.item"],["divine.item","flesh.item"]],"divine.item":[["divine.item","flesh.item"]]}
```

위반 경로 (채점 표기)

```text
divine.item flesh.item
```
