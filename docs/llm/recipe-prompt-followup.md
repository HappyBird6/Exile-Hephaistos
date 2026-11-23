# 제작 순서 프롬프트 후속 실험

## 결과와 선택

같은 모델로 이번에 **실제 API 호출 50회**를 수행했다. 개발용 S1·S2·S3·S4·S5·S8 여섯 입력에 P0~P6를 각 한 번 적용했다(42회). 이후 문안을 고정하고 기존 대조 사례 네 개와 이번에 새로 만든 H1~H4에 P6를 한 번씩 적용했다(8회). 정상 종료 50회, 전체 38,225 입력·1,053 출력 토큰이다. 이전 실험의 20회는 별도다.

| 공통 개발 사례 6개 | 기대 방향 회수 | 기대를 모두 회수하고 잘못된 방향도 없는 사례 | 입력 / 출력 토큰 |
|---|---:|---:|---:|
| P0 · 기준 행동 쌍 | 3/20 | 1/6 | 5,435 / 234 |
| P1 · 완성 예시 | 10/20 | 2/6 | 4,487 / 307 |
| P2 · 문자 코드 | 13/20 | 2/6 | 4,580 / 62 |
| P3 · 목표별 대안·자격 | 6/20 | 1/6 | 4,514 / 59 |
| P4 · 순서와 보조 코드 | 13/20 | 3/6 | 4,472 / 133 |
| P5 · 목표 순서만 | 16/20 | 4/6 | 4,260 / 81 |
| P6 · 목표 스키마 | 18/20 | 4/6 | 4,801 / 93 |

P0~P3은 모델이 행동열을 직접 생성했다. P4~P6은 모델이 제안한 목표 순서 안에서만 수신기가 행동 후보를 전개한다. 따라서 P6의 18/20은 **프롬프트와 전개 계약을 함께 적용한 결과**다. 엄격한 행동열 직접 생성만 비교하면 P2가 13/20으로 가장 높았지만 잘못된 순서도 반환했다. P6는 P0보다 개발 사례 출력이 234→93토큰으로 줄었다. 전체 토큰량에는 입력도 포함되며 모델의 비용 우위나 장기 평균은 이 단발 실험만으로 판단할 수 없다.

| 최종 P6 평가 구분 | 기대 방향 회수 | 기대를 모두 회수하고 잘못된 방향도 없는 사례 |
|---|---:|---:|
| 개발 6개 | 18/20 | 4/6 |
| 기존 대조 S6·S7·S9·S10 | 2/4 | 2/4 |
| 새 사례 H1~H4 | 12/13 | 3/4 |
| 전체 14개 | 32/37 | 9/14 |

**채택안.** P6는 거친 획득 순서를 제안하는 용도로 쓴다. 예를 들어 분열 목걸이 S2에서 모델이 직접 제안한 것은 생명력 우선·방어 우선 2행이다. 수신기에서 5개 기대 방향을 모두 복원했다. S4는 시전 속도 우선·중간 획득 방향이 여전히 빠졌고, S3는 Magic 장갑에서 합금을 먼저 쓰는 잘못된 역순도 남겼다. 모델이 제안하지 않은 목표 순서는 전개기가 대신 생성하지 않는다.

**도달 불가의 경계.** P6는 알려진 불가 사례 S7(분열 계열 충돌), S10(축성 수치 미달), H2(제작 한도 부족) 세 건 모두에서 불가를 정확히 보고하지 못했다. 실제 제품에서는 기존 검증기가 이미 증명한 완성·불가 상태를 선처리하고, 모델의 빈 목록 또는 불가능하다는 판단을 제작 불가의 단독 근거로 사용하지 않는다. S4처럼 가능한 순서가 모델 출력에서 빠질 때를 위한 후속 실험은 첫 목표를 나눈 개별 요청과 그 추가 비용을 비교하는 것이다. 이번 50회에는 포함하지 않았다.

**실험 계약.** P5/P6 oneOf는 목표별 행동 대안이다. 두 모델 출력에 대해 같은 전개 로직으로 동일 목표 순서 내 출처 조합만 펼치고, 허용된 한도 확장은 실제 필요 직전에, 허용된 실패 복구는 시도 직후에만 선택적으로 넣었다. P4에서는 모델이 명시한 보조 문자만 사용했다. 유효성이 확인된 추가 방향은 보존하며 사전 기대 점수에 더하지 않았다. 상태 충돌이 명백한 음성 대조는 사전에 정한 종료 기대와 비교했다. 현재 이 전개는 실험용이며 제품에서 구현·통합한 결과가 아니다. 각 사례의 출력은 모델 메시지 원문 그대로다.

**재현 정보.** OpenAI Chat Completions. P0~P5는 일반 텍스트 출력에서 JSON을 파싱했다. P6는 `response_format.type=json_schema`, strict=true, 필수 속성 orders만 있는 객체, 추가 속성 금지, 배열의 배열, 내부 원소는 plan.need의 목표 이름 enum(빈 need일 때 string)이다. P0/P1은 사례 입력 그대로, P2는 menu 추가, P3/P4는 menu와 해당 버전 plan을 사용했다. P5/P6은 중복 menu를 제거하고 사례 입력의 plan을 표시된 P5/P6 plan으로 교체했다. 모델은 실험 이름·기대 응답·다른 호출의 응답을 받지 않았다.

## 사전 등록

이전 [20회 실험](recipe-prompt-experiment-20261123.md)의 마지막 프롬프트로 기준선을 다시 측정한다. 모델은 같은 `gpt-4.1-mini-2025-04-14`, temperature 0, 출력 한도 3,200토큰, store false다. 독립 호출마다 시스템 문안과 해당 입력 한 개만 전송한다. 항상 동일한 행동 사전을 주며 순서 정답은 요청에서 제외한다. 시나리오 입력과 기대 방향은 이번 첫 호출 전에 고정했다. S는 기존 사례, H는 수정안을 만든 뒤에만 확인할 신규 사례다. 시작 시 open은 선판정하지 않았다는 뜻이고, 이미 완성되거나 도달 불가능한 실제 상태도 포함한다.

기대 방향 회수는 **적법하고 완성 가능한 후보** 중 기대 목록과 일치하는 서로 다른 방향 수다. 형식 오류·불법 후보·중복과 기대 밖의 적법한 대안을 별도로 센다. S4는 두 번째 Crafted 획득 전에 단 한 번 한도를 늘리면 준비의 위치를 동등하게 처리한다. 조건부 실패 복구는 대응 시도 직후에 붙인 별도 후보다. 판정은 이 실험에서 제공한 추상 행동의 성공 분기와 명시 전제를 대상으로 한다. 게임 전체에 대한 도달 확률 검사가 아니다.

## 문안 P0 · 기준

```text
�ʴ� PoE2 ���� �ĺ��� ã�´�. ��ǥ�� ���� �����ۿ��� �̴� ��ǥ�� ��� Ȯ���ϴ� �ൿ ������. Ȯ��������� �鿣�尡 ����Ѵ�.

�Է��� plan�� �鿣�尡 ��ó�� ���� ���·� ����� �� �ൿ �����̴�. plan.start�� done�̸� [[]], blocked�̸� []�� ��� ��ȯ�Ѵ�. �� �ܿ��� plan.need���� �����Ѵ�. held�� �̹� Ȯ���� �ɼ��̹Ƿ� �ٽ� ���� �ʴ´�.

�� �ĺ��� �� �������� �����. ���� �ൿ�� plan.moves���� ������ �״�� �����Ѵ�. ��ȣ �� �� ���ڿ��� ��ü�ϰų� �� ���� ������ �ʴ´�. acquire �ൿ �ϳ��� �����ϸ� �ش� ��� �ϳ��� need���� �����. ���� ��ǥ�� ��� �ٸ� �ൿ�� �ٸ� �ĺ��� ���� �Ǵ� �ļ����� �����Ѵ�. ���� �ٸ� ��ǥ�� ���� �ٴ� �� ������ �����Ѵ�.

�� �ܰ� ���� plan.gates�� baseRules�� �����Ѵ�. Crafted �ڸ��� ������ �� ��° ���� �ɼ��� ���̱� ���� cap_up�� �����Ѵ�. �ѵ� Ȯ���� �̸� �ص� �ȴ�. �غ�ǰ������ġ ������ ���õ� ȿ���� �����Ѵ�. ���Ǻ� ������ ���� ����� �õ� �ٷ� �ڿ� ������� ���̸� ���� ��ο� �ʼ��� ���� �ʴ´�. ��� need�� �����Ǹ� ��� �� �ĺ��� �ݴ´�. ���� ��ǥ�� ���� ��, ���� ������ �� �´� ���� �������� �ʴ´�.

JSON �迭�� ��ȯ�Ѵ�. �� �ĺ��� �ൿ ���� �迭�̴�. ����� [[["�ൿ1","���1"]],[["�ൿ2","���1"]]]�̴�. �̹� �ϼ�=[[]], ������ �ĺ��� ������ �� ����=[].
```

## 문안 P1 · 행동마다 끝까지 전개

기준 재측정에서 S2·S3·S4·S5·S8 응답은 대부분 한 단계 조각이었다. 입력은 유지하고 분기마다 처음 상태를 복제해 모든 목표가 찰 때까지 이어 붙이도록 수정했다.

```text
너는 PoE2 제작 플래너다. 한 후보는 시작 아이템에서 모든 목표가 채워지는 하나의 완결된 화폐 실행 순서다. 확률과 비용은 백엔드가 검증한다.
1. 매 후보는 별도의 시작 상태에서 출발한다. start.mods와 수치 조건까지 이미 맞으면 [[]], 불변 상태나 계열 충돌 등으로 완성할 수 없으면 []다. plan.start=open은 판정 전이다.
2. 첫 수 후보마다 하나씩 갈라진다. 가능한 행동을 하나 골랐으면 그 결과에서 가능한 다음 수를 각각 이어 붙인다. 아직 need가 남아 있으면 그 줄을 답에 쓰지 말고 계속한다. 같은 목표의 다른 출처, 다른 목표가 먼저 붙는 순서도 각각 끝까지 완성한다.
3. moves에 있는 [전략,대상]만 복사한다. 각 후보 안에서 희귀도·한도·분열·훼손 슬롯·품질을 갱신한다. 이미 얻은 목표를 다시 얻지 말고, 필요한 준비는 자격이 필요한 행동 앞에 둔다. 같은 소모품 순서의 반복만 중복으로 지운다.
4. 적법한 완성 경로에 실패 복구가 가능하면 원래 후보와 시도 직후 복구를 붙인 후보를 모두 낸다. 먼저 찾은 후보 하나로 종료하지 않는다.
5. 출력은 후보 배열만. 예컨대 x를 얻을 수단 a,b, y를 얻을 수단 c가 있고 둘 다 아무 순서로 가능하면 답은 [[["a","x"],["c","y"]],[["b","x"],["c","y"]],[["c","y"],["a","x"]],[["c","y"],["b","x"]]]다. 이 예시의 ID는 쓰지 말고 실제 입력의 ID만 쓴다.
```

## 문안 P2 · 수단 선택 후 순열 · 짧은 행동 코드

P1에서 S2는 생명력 선취득만, S8은 생명력 첫 단계만 출력했다. 동일한 행동·대상에 입력 순서대로 A부터 코드를 붙이고, 수단 선택과 순서 전개를 분리했다. 전체 JSON에는 menu를 추가한다. 출력 코드는 백엔드가 이미 가진 쌍으로 무손실 복원한다.

```text
너는 제작 경로를 열거한다. menu의 문자 하나는 백엔드가 실행할 [전략,대상] 한 쌍이다. 답은 완성 경로들의 JSON 문자열 배열이다. "AC"는 A 실행 후 C 실행이라는 뜻이다.
① 시작부터 완성이면 [""], 고정 아이템으로 목표가 불가능하면 []를 반환한다. open은 미판정이다. 이미 가진 옵션은 유지한다.
② 미달 목표별로 획득 수단을 하나씩 고른다. 같은 목표의 A와 B는 대안이라 각각 별도 조합이다. 목표가 하나면 그 하나를 달성한 즉시 경로가 끝난다. 수치·품질 작업이 필요하면 그것도 각각 완수한다.
③ 각 수단 조합별로, 적법한 첫 행동을 하나씩 바꿔 남은 행동의 모든 의미 있는 순서를 검토한다. 예를 들어 A 또는 B로 x, C로 y를 얻고 순서가 자유면 ["AC","CA","BC","CB"]. 입력의 첫 목표나 menu의 첫 문자에 우선권은 없다.
④ 매번 rules·baseRules·plan.gates로 희귀도, Crafted 한도, 첫 획득 전용 여부, 계열 충돌, 훼손 슬롯을 검사한다. 필요 준비를 의존 행동 전에 한 번 넣는다. 무관한 준비 위치만 바뀐 경로는 합쳐도 된다. onMiss 수단이 적법하면 원래 경로도 남기고 해당 시도 바로 뒤에 복구 문자 하나를 넣은 변형도 남긴다.
⑤ 각 문자열을 독립적으로 읽어 처음부터 끝까지 모두 가능한지, 마지막에 목표가 전부 충족되는지 점검한다. menu 문자만 사용한다. JSON 외에는 출력하지 않는다.
```

## 문안 P3 · 목표별 대안·행동별 자격을 분리

P2는 한 단계 사례를 회복했지만 Magic에서 합금을 먼저 쓰고, 아직 목표가 빠진 접두 경로도 출력했다. P3부터 moves의 중복 목록 대신 oneOf(목표별 수단), extra(준비·복구), guards(각 행동 직전 자격)를 제공한다. 이 표에는 완성 순서가 없다.

```text
당신은 아이템 제작 순서를 찾는다. JSON의 menu 문자 하나는 [행동,대상]이다. 비용과 확률은 계산하지 않는다.
시작 아이템으로 목표가 이미 맞으면 [""]. 축성된 미달, 제거 불가능한 계열 충돌 등으로 달성 불가능하면 []. open은 미판정이다.
완성 후보를 다음 두 단계로 만든다.
1. plan.oneOf의 각 목표에서 정확히 한 문자씩 선택한다. 한 조합에 모든 목표의 문자가 반드시 들어가야 한다. 같은 목표의 문자끼리는 대안이다. 모든 조합을 확인한다.
2. 각 조합의 문자들이 나올 수 있는 순서를 검토한다. 첫 목표를 입력 순서로 고정하지 않는다. 각 문자 직전의 상태가 guards와 rules를 만족하는 순서만 남긴다. 'opening only'는 첫 획득에만, 'Magic -> Rare'는 Magic일 때 한 번만 가능하다. Crafted 자리가 부족해질 때는 extra의 한도 확장을 미리 끼운다. 이미 소모된 훼손 슬롯은 다시 쓸 수 없다.
적법한 기본 순서를 먼저 기록한 뒤 optional on miss가 가능한 줄은 해당 문자 바로 다음에 복구를 넣은 복사본도 추가한다. 준비 위치만 다른 중복은 합쳐도 된다. 출력 직전 각 문자열이 모든 미달 목표를 정확히 한 번씩 채우는지, 자격을 충족하는지 확인한다.
출력은 완성된 순서들의 JSON 문자열 배열 하나. 각 문자열은 menu 문자만 이어 쓴다. 설명, 중간 상태, 미완성 조각은 출력하지 않는다.
```

## 문안 P4 · 목표 순서 묶음과 선택적 보조 행동

P3는 코드 복사 오류와 미완성 조각이 다시 생겼다. 실험적 응답 계약을 바꿨다. LLM은 목표를 실제로 얻을 순서를 제안하고 선택할 보조 코드를 명시한다. 수신기는 모델이 제안한 순서 안에서만 각 oneOf의 출처 대안을 조합하고 전제를 검사한다. 서로 다른 목표의 순서를 새로 추가하지 않는다. 필요 시 허용된 cap_up을 두 번째 Crafted 직전에 한 번, whittle_miss는 대응하는 실패 시도 직후 별도 대안으로 펼친다. 이후 회수율에는 이 결정적 전개 효과가 포함된다.

```text
너는 PoE2 제작 방향을 찾는다. 모델이 정할 것은 목표의 획득 순서와 사용할 보조 전략이다. 화폐 세부 선택·실행 자격·확률·비용은 백엔드가 계산한다.
응답은 {"orders":[...],"extras":[...]} JSON 하나다. 각 order는 plan.need의 목표 이름을 달성 순으로 한 번씩 적은 배열이다. 행동 코드나 획득처를 order에 쓰지 않는다. 예: 두 목표 x,y의 선후가 모두 가능하면 [["x","y"],["y","x"]]. 한 목표는 [[목표]]. 시작부터 완성이면 orders=[[]], 명백히 불가능하면 orders=[]이다. plan.start=open은 미판정이다.
각 목표를 첫 번째로 놓아 보고, 가능한 남은 목표 순서도 계속 바꿔 본다. 그 순서 안에서 plan.oneOf의 수단을 하나씩 골라 합법적인 조합이 하나라도 있으면 해당 순서를 남긴다. 모든 수단이 처음부터 가능할 필요는 없다. Magic→Rare, 이미 보유한 분열, 계열 충돌, Crafted 한도, 훼손 슬롯, 품질을 확인한다.
extras에는 그 순서들을 성립시키거나 실패 복구 대안을 남기는 plan.extra의 문자만 넣는다. 백엔드는 같은 목표의 여러 적격 출처를 펼친다. 선택한 한도 확장은 필요한 획득 전에, 선택한 실패 복구는 대응 시도 직후 조건부로 적용한다. 원래의 목표 순서는 백엔드가 대신 생성하지 않는다. 중복 순서와 설명은 출력하지 않는다.
```

## 문안 P5 · 목표 순서만 반환 · 적격 수단과 보조 작업은 수신기에서 전개

P4의 S1·S3에서는 일반 행동을 extras에 써서 유효한 응답 전체를 잃었다. P5는 사용자 입력의 need·oneOf에 목표 이름과 실제 전략 ID를 쓰고 응답에서 extras 필드를 없앤다. 전개기는 입력에 허용된 준비·복구만 P4와 같은 위치에 검토한다. 목표 순서가 모델 출력에 없으면 그 순서의 출처나 복구도 복원되지 않는다.

```text
너는 PoE2의 제작 방향을 제안한다. 시작 아이템과 목표, 베이스 규칙, 허용 전략을 읽고 어떤 목표를 먼저 확보할지 결정하라.
출력은 목표 ID들의 순서를 담은 JSON 배열 하나다. 예: [["x","y"],["y","x"]]. 각 안쪽 배열은 독립된 완성 방향이다. plan.need의 이름을 정확히 한 번씩 사용한다. 전략 ID, 재료 이름, 설명은 출력하지 않는다. 이미 목표를 모두 충족하면 [[]], 고정 시작 상태에서 모든 방향이 불가능하면 []다. open은 미판정이다.
먼저 held와 실제 수치·잠금 상태를 확인한다. 미달 목표 하나하나를 첫 목표로 놓고, 그 뒤 나머지 목표의 순서를 바꿔 검토한다. 세 목표라면 여섯 상대 순서에서 출발한다. 한 목표라도 빠진 미완성 줄과 중복은 버린다.
각 순서에는 그 순서를 실제로 성립시킬 수단이 하나만 있어도 된다. oneOf는 한 목표를 얻는 대안이다. start와 rules에 따라 등급 상승, 제작 한도 확장, 분열 보존, 사용한 훼손 슬롯, 품질·수치 조정을 반영한다. 서로 독립이면 양쪽 선취득 순서를 모두 남긴다. 모든 수단이 막히는 순서만 제외한다.
백엔드는 네가 반환한 순서 안에서 oneOf의 적격 수단과 입력에 있는 helpers를 펼친다. 준비 행동은 필요할 때 선행하고, 실패 복구는 대응 시도 직후의 선택지다. 백엔드가 목표 순서를 새로 만들지는 않는다.
```

## 문안 P6 · 목표 ID 출력 스키마 · 순서별 첫 전이 확인

P5 마법봉이 목표 자리에 준비 행동 cap_up을 출력했고, Magic 장갑이 합금을 먼저 쓰는 역순까지 포함했다. P5와 같은 입력으로 목표 이름을 JSON Schema의 enum에 고정하고, 첫 수의 희귀도와 후속 준비 자격을 별도로 점검한다. 빈 need에서는 문자열 타입만 허용한다. 출력 스키마는 orders(필수, 배열의 배열)만 가진 객체이고 추가 속성 금지, strict=true다. 대조·미사용 사례는 이 문안을 고정한 뒤 실행한다.

```text
너는 PoE2 제작 경로의 목표 획득 순서를 제안한다. 고정 시작 아이템으로 만들 수 있는 서로 다른 방향을 넓게 찾는다. 출력은 orders 하나다. 각 행에는 plan.need의 목표 이름만 정확히 한 번씩 적는다. 이미 완성하면 {"orders":[[]]}, 모든 방향이 불가능하면 {"orders":[]}다. open은 아직 판정하지 않았다는 뜻이다.
1. 현재 수치와 품질, 변경 불가 상태, 이미 보유한 목표부터 확인한다. 잠긴 아이템에서 미달하거나 제거할 수 없는 옵션과 필수 목표의 계열이 충돌하면 완성할 수 없다.
2. 미달 목표를 어떤 순서로 얻을지 고려한다. 첫 목표 후보마다 실제 start에서 쓸 수 있는 획득 수단이 하나라도 있는지 확인한다. 필요 준비가 제공됐으면 준비 후 상태로 본다. 예컨대 Magic에서 첫 목표의 수단이 Rare 전용이고 제공된 승급이 다른 목표를 획득한다면 그 다른 목표가 먼저다.
3. 첫 획득 후 희귀도, 남은 접두·접미, 제작 한도, 훼손 슬롯을 갱신해 다음 목표도 같은 방식으로 확인한다. 순서를 성립시키는 수단이 하나라도 있으면 남긴다. 세 목표는 첫 목표를 각각 바꿔 나머지 두 목표의 양방향까지 검토한다.
4. 같은 목표를 얻는 여러 출처, 더미·한도 확장·실패 복구는 백엔드가 해당 순서 안에서 oneOf와 helpers로 펼친다. 이 보조 작업을 목표 자리에 쓰지 않는다. 품질이나 수치가 need에 독립 목표로 명시됐으면 그 이름은 반드시 포함한다.
마지막으로 중복과 미완성 순서를 제거한다. 응답 스키마에 맞는 목표 순서만 반환한다.
```


## S1 · Rare �� ����� �� �Ϲ� ������ 1��

입력

```json
{"base":"Solar Amulet","start":{"rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]}},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{}},"strategies":{"first_chaos":"ù ���� ���� �ɼ� �ϳ��� ī������ �ݺ��Ͽ� �Ϲ� ��ǥ ȹ��. �п� ����. �ʿ��� ��ޡ�ù �ɼ� �غ� ����.","base_add":"Rare�� �� �ڸ��� �Ϲ� ��ǥ�� ���߷� �õ�.","reveal":"����� �Ѽ� ���� ������ ����� ���� ��Ͽ��� ��ǥ�� ����."},"baseRules":[],"rules":["�Ѽ� �� ����. �Ϲ� �ɼ��� �̹� ���� �������� ���߷ε� ȹ�� ����."],"plan":{"start":"open","held":[],"need":["life"],"moves":{"acquire":[["first_chaos","life"],["base_add","life"],["reveal","life"]]},"gates":[]}}
```

호출 전 기대 방향

```text
first_chaos.life
base_add.life
reveal.life
```

단일 목표에서 서로 다른 출처의 세 후보를 실제로 분리하는지 확인한다. P6 원문 한 행은 세 가지 화폐 전략으로 전개된다.

요청별 행동 코드표

```json
{"A":["first_chaos","life"],"B":["base_add","life"],"C":["reveal","life"]}
```

P3/P4에서 실제 사용한 plan

```json
{"start":"open","held":[],"need":["life"],"gates":[],"oneOf":{"life":["A","B","C"]},"extra":{},"guards":{"A":"opening only","B":"Rare; empty affix slot","C":"one shared desecrated slot"}}
```

P5/P6에서 실제 사용한 plan

```json
{"start":"open","held":[],"need":["life"],"oneOf":{"life":["first_chaos","base_add","reveal"]},"helpers":[],"gates":[]}
```

**P0 실제 응답**

```json
[
  [
    ["first_chaos", "life"]
  ],
  [
    ["base_add", "life"]
  ],
  [
    ["reveal", "life"]
  ]
]
```

기대 3/3 · 사전 기대 모두 회수, 잘못된 방향 없음.

**P1 실제 응답**

```json
[[["first_chaos","life"],["reveal","life"]],[["base_add","life"],["reveal","life"]]]
```

기대 0/3 · 누락 `first_chaos.life`, `base_add.life`, `reveal.life` · 오류 이미 확보한 목표 또는 대상 오류 2건.

**P2 실제 응답**

```json
["A","B","C"]
```

기대 3/3 · 사전 기대 모두 회수, 잘못된 방향 없음.

**P3 실제 응답**

```json
["A","B","C","B"]
```

기대 3/3 · 동등 후보 중복 1건 · 사전 기대 모두 회수, 잘못된 방향 없음.

**P4 실제 응답**

```json
{"orders":[["life"]],"extras":["A","B","C"]}
```

기대 0/3 · 누락 `first_chaos.life`, `base_add.life`, `reveal.life` · 오류 허용되지 않은 보조 코드 또는 필드 1건.

**P5 실제 응답**

```json
[["life"]]
```

기대 3/3 · 사전 기대 모두 회수, 잘못된 방향 없음.

**P6 실제 응답**

```json
{"orders":[["life"]]}
```

기대 3/3 · 사전 기대 모두 회수, 잘못된 방향 없음.

## S2 · �п� �� �� �����°� ��� Ȯ��, ���� ���� ����

입력

```json
{"base":"Solar Amulet","start":{"rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"strength","fractured":true}]}},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"strength":["base","Preserved Collarbone","Altered Collarbone"]}},"strategies":{"first_chaos":"ù ���� ���� �ɼ� �ϳ��� ī������ �ݺ��Ͽ� �Ϲ� ��ǥ ȹ��. �п� ����. �ʿ��� ��ޡ�ù �ɼ� �غ� ����.","base_add":"Rare�� �� �ڸ��� �Ϲ� ��ǥ�� ���߷� �õ�.","reveal":"����� �Ѽ� ���� ������ ����� ���� ��Ͽ��� ��ǥ�� ����.","safe_ess":"Rare�� ��� Perfect Essence ���. �ʿ� �� ���ſ� ���̿� ���� ¡���� �غ��� ���� ��ǥ ����.","whittle_miss":"���� base_add ���� �ÿ��� ���� ī������ ���� ����� ����. �����ϸ� ����."},"baseRules":["Rare 3���Ρ�3����. Crafted �ѵ� 1."],"rules":["�п��� ù ���� ���� �ɼ� ������ �����Ѵ�. def�� Crafted. �Ϲ� ��÷�� ���� ���� �Ŀ��� ���� �ڰ��� �˻��Ѵ�."],"plan":{"start":"open","held":["strength"],"need":["life","def"],"moves":{"acquire":[["first_chaos","life"],["base_add","life"],["reveal","life"],["safe_ess","def"]],"onMiss":[["whittle_miss","life"]]},"gates":["first_chaos는 첫 획득만 가능. whittle_miss는 같은 대상의 base_add 바로 뒤에만 선택 가능."]}}
```

호출 전 기대 방향

```text
first_chaos.life safe_ess.def
reveal.life safe_ess.def
safe_ess.def reveal.life
safe_ess.def base_add.life
safe_ess.def base_add.life whittle_miss.life
```

P6의 두 목표 순서를 전개하면 사전 5방향 외에 일반 생명력 엑잘 선취득과 그 실패 복구 후 에센스도 남는다. 원문에는 2행이 있고 5개 기대 레시피를 모델이 직접 적은 것은 아니다.

요청별 행동 코드표

```json
{"A":["first_chaos","life"],"B":["base_add","life"],"C":["reveal","life"],"D":["safe_ess","def"],"E":["whittle_miss","life"]}
```

P3/P4에서 실제 사용한 plan

```json
{"start":"open","held":["strength"],"need":["life","def"],"gates":["first_chaos는 첫 획득만 가능. whittle_miss는 같은 대상의 base_add 바로 뒤에만 선택 가능."],"oneOf":{"life":["A","B","C"],"def":["D"]},"extra":{"E":"whittle_miss"},"guards":{"A":"opening only","B":"Rare; empty affix slot","C":"one shared desecrated slot","D":"Rare; costs one Crafted slot","E":"optional immediately after B on miss"}}
```

P5/P6에서 실제 사용한 plan

```json
{"start":"open","held":["strength"],"need":["life","def"],"oneOf":{"life":["first_chaos","base_add","reveal"],"def":["safe_ess"]},"helpers":[["whittle_miss","life"]],"gates":["first_chaos는 첫 획득만 가능. whittle_miss는 같은 대상의 base_add 바로 뒤에만 선택 가능."]}
```

**P0 실제 응답**

```json
[
  [["first_chaos","life"]],
  [["base_add","life"]],
  [["reveal","life"]],
  [["safe_ess","def"]]
]
```

기대 0/5 · 누락 `first_chaos.life safe_ess.def`, `reveal.life safe_ess.def`, `safe_ess.def reveal.life`, `safe_ess.def base_add.life`, `safe_ess.def base_add.life whittle_miss.life` · 오류 완성에 필요한 단계 누락 4건.

**P1 실제 응답**

```json
[
  [
    ["first_chaos","life"],
    ["safe_ess","def"]
  ],
  [
    ["base_add","life"],
    ["safe_ess","def"]
  ],
  [
    ["base_add","life"],
    ["whittle_miss","life"],
    ["safe_ess","def"]
  ],
  [
    ["reveal","life"],
    ["safe_ess","def"]
  ]
]
```

기대 2/5 · 누락 `safe_ess.def reveal.life`, `safe_ess.def base_add.life`, `safe_ess.def base_add.life whittle_miss.life` · 추가 유효 `base_add.life safe_ess.def`, `base_add.life whittle_miss.life safe_ess.def`.

**P2 실제 응답**

```json
["A","B","C","AD","BD","CD","AE","BE","CE"]
```

기대 2/5 · 누락 `safe_ess.def reveal.life`, `safe_ess.def base_add.life`, `safe_ess.def base_add.life whittle_miss.life` · 추가 유효 `base_add.life safe_ess.def` · 오류 완성에 필요한 단계 누락 4건, 복구 대상과 직전 시도 불일치 2건.

**P3 실제 응답**

```json
["BD","BDE"]
```

기대 0/5 · 누락 `first_chaos.life safe_ess.def`, `reveal.life safe_ess.def`, `safe_ess.def reveal.life`, `safe_ess.def base_add.life`, `safe_ess.def base_add.life whittle_miss.life` · 추가 유효 `base_add.life safe_ess.def` · 오류 복구 대상과 직전 시도 불일치 1건.

**P4 실제 응답**

```json
{"orders":[["life","def"],["def","life"]],"extras":["E"]}
```

기대 5/5 · 추가 유효 `base_add.life safe_ess.def`, `base_add.life whittle_miss.life safe_ess.def` · 출처 전개 중 탈락 1개 · 사전 기대 모두 회수, 잘못된 방향 없음.

**P5 실제 응답**

```json
[["life","def"],["def","life"]]
```

기대 5/5 · 추가 유효 `base_add.life safe_ess.def`, `base_add.life whittle_miss.life safe_ess.def` · 출처 전개 중 탈락 1개 · 사전 기대 모두 회수, 잘못된 방향 없음.

**P6 실제 응답**

```json
{"orders":[["life","def"],["def","life"]]}
```

기대 5/5 · 추가 유효 `base_add.life safe_ess.def`, `base_add.life whittle_miss.life safe_ess.def` · 출처 전개 중 탈락 1개 · 사전 기대 모두 회수, 잘못된 방향 없음.

## S3 · Magic �尩 �� ��� ��� ���� �� ����

입력

```json
{"base":"Stocky Mitts","start":{"rarity":"magic","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"filler"}]}},"targets":{"prefix":{"life":["base","Greater Essence of the Body"],"penetration":["Prismatic Alloy"]},"suffix":{}},"strategies":{"upgrade_ess":"Magic�� Rare�� �ø��� ��� ������ �ɼ� ȹ��.","regal_base":"Magic�� Rare�� �ø��� �Ϲ� ��ǥ�� ���� ��� ä��.","safe_alloy":"Rare�� ��� Alloy ���. �ʿ� �� ���ſ� ���̸� �غ��ϰ� ���� ��ǥ�� ���� ����� ä��."},"baseRules":[],"rules":["�� Magic���� �� ��� ��� ������ ���� ����̴�. life�� �Ϲ� �ɼ�, penetration�� Crafted. ��ǥ �� ���̰� ���Ƶ� �ȴ�."],"plan":{"start":"open","held":[],"need":["life","penetration"],"moves":{"acquire":[["upgrade_ess","life"],["regal_base","life"],["safe_alloy","penetration"]]},"gates":[]}}
```

호출 전 기대 방향

```text
upgrade_ess.life safe_alloy.penetration
regal_base.life safe_alloy.penetration
```

P6의 두 번째 행은 Magic에서 합금을 먼저 써야 하므로 탈락한다. 첫 번째 행은 승급 에센스와 제왕 두 대안을 허용한다. 회수 2/2에도 잘못된 원문 방향 1개가 있다.

요청별 행동 코드표

```json
{"A":["upgrade_ess","life"],"B":["regal_base","life"],"C":["safe_alloy","penetration"]}
```

P3/P4에서 실제 사용한 plan

```json
{"start":"open","held":[],"need":["life","penetration"],"gates":[],"oneOf":{"life":["A","B"],"penetration":["C"]},"extra":{},"guards":{"A":"Magic -> Rare","B":"Magic -> Rare","C":"Rare; costs one Crafted slot"}}
```

P5/P6에서 실제 사용한 plan

```json
{"start":"open","held":[],"need":["life","penetration"],"oneOf":{"life":["upgrade_ess","regal_base"],"penetration":["safe_alloy"]},"helpers":[],"gates":[]}
```

**P0 실제 응답**

```json
[
  [
    ["upgrade_ess", "life"]
  ],
  [
    ["regal_base", "life"]
  ],
  [
    ["safe_alloy", "penetration"]
  ]
]
```

기대 0/2 · 누락 `upgrade_ess.life safe_alloy.penetration`, `regal_base.life safe_alloy.penetration` · 오류 완성에 필요한 단계 누락 2건, Rare가 되기 전 Rare 전용 화폐 사용 1건.

**P1 실제 응답**

```json
[[["upgrade_ess","life"],["safe_alloy","penetration"]],[["regal_base","life"],["safe_alloy","penetration"]]]
```

기대 2/2 · 사전 기대 모두 회수, 잘못된 방향 없음.

**P2 실제 응답**

```json
["AC","CA","BC","CB"]
```

기대 2/2 · 오류 Rare가 되기 전 Rare 전용 화폐 사용 2건.

**P3 실제 응답**

```json
["AC","BC","AC\u0000C","BC\u0000C"]
```

기대 0/2 · 누락 `upgrade_ess.life safe_alloy.penetration`, `regal_base.life safe_alloy.penetration` · 오류 메뉴 범위 밖의 문자 또는 잘못된 코드 형식 1건.

**P4 실제 응답**

```json
{"orders":[["life","penetration"],["penetration","life"]],"extras":["upgrade_ess","regal_base","safe_alloy"]}
```

기대 0/2 · 누락 `upgrade_ess.life safe_alloy.penetration`, `regal_base.life safe_alloy.penetration` · 오류 허용되지 않은 보조 코드 또는 필드 1건.

**P5 실제 응답**

```json
[["life","penetration"],["penetration","life"]]
```

기대 2/2 · 오류 해당 목표 순서를 완성할 적격 행동 조합 없음 1건 · 출처 전개 중 탈락 2개.

**P6 실제 응답**

```json
{"orders":[["life","penetration"],["penetration","life"]]}
```

기대 2/2 · 오류 해당 목표 순서를 완성할 적격 행동 조합 없음 1건 · 출처 전개 중 탈락 2개.

## S4 · ������ �� �ձ�, ������, �Ϲ� ���� �ӵ�

입력

```json
{"base":"Attuned Wand","start":{"rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]}},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"],"speed":["base"]}},"strategies":{"safe_alloy":"Rare�� ��� Alloy ���. �ʿ� �� ���ſ� ���̸� �غ��ϰ� ���� ��ǥ�� ���� ����� ä��.","safe_ess":"Rare�� ��� Perfect Essence ���. �ʿ� �� ���ſ� ���̿� ���� ¡���� �غ��� ���� ��ǥ ����.","cap_up":"��� ������ ���Ͽ� Astrid's Creativity ��ġ, Crafted �ѵ� +1. ��� item.","base_add":"Rare�� �� �ڸ��� �Ϲ� ��ǥ�� ���߷� �õ�."},"baseRules":["���� Crafted �ѵ� 1. �� ���� 1��. Astrid's Creativity�� �ѵ� 2 ����."],"rules":["mana_skill�� skill�� ���� Crafted 1���� ����. ���� �ٸ� �迭�̸� �� �� �����Ѵ�. speed�� �Ϲ� ����."],"plan":{"start":"open","held":[],"need":["mana_skill","skill","speed"],"moves":{"acquire":[["safe_alloy","mana_skill"],["safe_ess","skill"],["base_add","speed"]],"prepare":[["cap_up","item"]]},"gates":["mana_skill·skill은 Crafted, speed는 일반. 시작 한도 1. 두 Crafted 중 두 번째를 얻는 시점에는 cap_up이 이미 순서에 있어야 한다."]}}
```

호출 전 기대 방향

```text
safe_alloy.mana_skill safe_ess.skill base_add.speed
safe_ess.skill safe_alloy.mana_skill base_add.speed
base_add.speed safe_alloy.mana_skill safe_ess.skill
safe_alloy.mana_skill base_add.speed safe_ess.skill
```

P6는 두 Crafted를 먼저 얻고 시전 속도를 마지막에 얻는 두 방향만 제안했다. 시전 속도를 먼저 또는 중간에 붙이는 사전 기대 2개가 빠졌다. cap_up은 전개기가 두 번째 Crafted 직전에 삽입했다.

요청별 행동 코드표

```json
{"A":["safe_alloy","mana_skill"],"B":["safe_ess","skill"],"C":["base_add","speed"],"D":["cap_up","item"]}
```

P3/P4에서 실제 사용한 plan

```json
{"start":"open","held":[],"need":["mana_skill","skill","speed"],"gates":["mana_skill·skill은 Crafted, speed는 일반. 시작 한도 1. 두 Crafted 중 두 번째를 얻는 시점에는 cap_up이 이미 순서에 있어야 한다."],"oneOf":{"mana_skill":["A"],"skill":["B"],"speed":["C"]},"extra":{"D":"cap_up"},"guards":{"A":"Rare; costs one Crafted slot","B":"Rare; costs one Crafted slot","C":"Rare; empty affix slot","D":"add one Crafted slot once"}}
```

P5/P6에서 실제 사용한 plan

```json
{"start":"open","held":[],"need":["mana_skill","skill","speed"],"oneOf":{"mana_skill":["safe_alloy"],"skill":["safe_ess"],"speed":["base_add"]},"helpers":[["cap_up","item"]],"gates":["mana_skill·skill은 Crafted, speed는 일반. 시작 한도 1. 두 Crafted 중 두 번째를 얻는 시점에는 cap_up이 이미 순서에 있어야 한다."]}
```

**P0 실제 응답**

```json
[
  [["safe_alloy", "mana_skill"]],
  [["safe_ess", "skill"]],
  [["base_add", "speed"]],
  [["cap_up", "item"]]
]
```

기대 0/4 · 누락 `safe_alloy.mana_skill safe_ess.skill base_add.speed`, `safe_ess.skill safe_alloy.mana_skill base_add.speed`, `base_add.speed safe_alloy.mana_skill safe_ess.skill`, `safe_alloy.mana_skill base_add.speed safe_ess.skill` · 오류 완성에 필요한 단계 누락 4건.

**P1 실제 응답**

```json
[[["safe_alloy","mana_skill"],["cap_up","item"],["safe_ess","skill"],["base_add","speed"]],[["safe_ess","skill"],["cap_up","item"],["safe_alloy","mana_skill"],["base_add","speed"]]]
```

기대 2/4 · 누락 `base_add.speed safe_alloy.mana_skill safe_ess.skill`, `safe_alloy.mana_skill base_add.speed safe_ess.skill`.

**P2 실제 응답**

```json
["ADBC","ADCB","DABC","DACB"]
```

기대 2/4 · 누락 `safe_ess.skill safe_alloy.mana_skill base_add.speed`, `base_add.speed safe_alloy.mana_skill safe_ess.skill` · 동등 후보 중복 2건.

**P3 실제 응답**

```json
["C","D","A","B","C","B"]
```

기대 0/4 · 누락 `safe_alloy.mana_skill safe_ess.skill base_add.speed`, `safe_ess.skill safe_alloy.mana_skill base_add.speed`, `base_add.speed safe_alloy.mana_skill safe_ess.skill`, `safe_alloy.mana_skill base_add.speed safe_ess.skill` · 오류 완성에 필요한 단계 누락 6건.

**P4 실제 응답**

```json
{"orders":[["mana_skill","speed","skill"],["speed","mana_skill","skill"]],"extras":["D"]}
```

기대 2/4 · 누락 `safe_alloy.mana_skill safe_ess.skill base_add.speed`, `safe_ess.skill safe_alloy.mana_skill base_add.speed`.

**P5 실제 응답**

```json
[["mana_skill","cap_up","skill","speed"],["skill","cap_up","mana_skill","speed"]]
```

기대 0/4 · 누락 `safe_alloy.mana_skill safe_ess.skill base_add.speed`, `safe_ess.skill safe_alloy.mana_skill base_add.speed`, `base_add.speed safe_alloy.mana_skill safe_ess.skill`, `safe_alloy.mana_skill base_add.speed safe_ess.skill` · 오류 목표 목록에 준비 행동·중복·누락 포함 2건.

**P6 실제 응답**

```json
{"orders":[["mana_skill","skill","speed"],["skill","mana_skill","speed"]]}
```

기대 2/4 · 누락 `base_add.speed safe_alloy.mana_skill safe_ess.skill`, `safe_alloy.mana_skill base_add.speed safe_ess.skill`.

## S5 · Weighted Sum �� Ƽ��� �̹� Ȯ��, ��ġ�� ǰ���� ����

입력

```json
{"base":"Solar Amulet","start":{"rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","raw":99}],"suffix":[{"id":"strength","raw":7}]}},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"]},"suffix":{"strength":["base","Preserved Collarbone","Altered Collarbone"]}},"strategies":{"flesh":"Flesh Catalyst�� ǰ�� 20 ����. ��� item.","divine":"�Ϲ� Divine Orb�� ���� ������ �ɼ��� ���� Ƽ�� ���� �ȿ��� �籼��. ��ǥ ��ġ�� �´� ��� ä��. ��� item."},"baseRules":[],"rules":["�ϼ��� Flesh ǰ�� 20, ������ 115~124, �� 7~14, �� 127~129. ���� ǰ�� 0. Flesh ���� �� �����¸� �ݿø�(���� ����1.2). ����� Flesh�� ���� 119+7=126. ���� Ƽ���� ���� �� 99�� 8�� �� 127. Divine�� Flesh�� ��� �����ε� ���� ����."],"plan":{"start":"open","held":["life","strength"],"need":["Flesh20","수치조건"],"moves":{"prepare":[["flesh","item"],["divine","item"]]},"gates":["flesh는 Flesh20, divine의 적격 결과는 수치조건을 충족. 어느 순서든 두 작업이 필요."]}}
```

호출 전 기대 방향

```text
flesh.item divine.item
divine.item flesh.item
```

품질과 수치 조건은 이미 확보한 두 옵션과 별개의 미달 목표다. 마지막 문안은 두 순서를 반환했다.

요청별 행동 코드표

```json
{"A":["flesh","item"],"B":["divine","item"]}
```

P3/P4에서 실제 사용한 plan

```json
{"start":"open","held":["life","strength"],"need":["Flesh20","수치조건"],"gates":["flesh는 Flesh20, divine의 적격 결과는 수치조건을 충족. 어느 순서든 두 작업이 필요."],"oneOf":{"Flesh20":["A"],"수치조건":["B"]},"extra":{},"guards":{}}
```

P5/P6에서 실제 사용한 plan

```json
{"start":"open","held":["life","strength"],"need":["Flesh20","수치조건"],"oneOf":{"Flesh20":["flesh"],"수치조건":["divine"]},"helpers":[],"gates":["flesh는 Flesh20, divine의 적격 결과는 수치조건을 충족. 어느 순서든 두 작업이 필요."]}
```

**P0 실제 응답**

```json
[
  [
    ["flesh", "item"]
  ],
  [
    ["divine", "item"]
  ]
]
```

기대 0/2 · 누락 `flesh.item divine.item`, `divine.item flesh.item` · 오류 완성에 필요한 단계 누락 2건.

**P1 실제 응답**

```json
[
  [
    ["flesh","item"],
    ["divine","item"]
  ],
  [
    ["divine","item"],
    ["flesh","item"]
  ]
]
```

기대 2/2 · 사전 기대 모두 회수, 잘못된 방향 없음.

**P2 실제 응답**

```json
["AB","BA"]
```

기대 2/2 · 사전 기대 모두 회수, 잘못된 방향 없음.

**P3 실제 응답**

```json
["AB","AB"]
```

기대 1/2 · 누락 `divine.item flesh.item` · 동등 후보 중복 1건.

**P4 실제 응답**

```json
{"orders":[["Flesh20","수치조건"],["수치조건","Flesh20"]],"extras":[]}
```

기대 2/2 · 사전 기대 모두 회수, 잘못된 방향 없음.

**P5 실제 응답**

```json
[["Flesh20","수치조건"],["수치조건","Flesh20"]]
```

기대 2/2 · 사전 기대 모두 회수, 잘못된 방향 없음.

**P6 실제 응답**

```json
{"orders":[["Flesh20","수치조건"],["수치조건","Flesh20"]]}
```

기대 2/2 · 사전 기대 모두 회수, 잘못된 방향 없음.

## S6 · �̹� ��ġ�ϴ� �༺ ����

입력

```json
{"base":"Iron Ring","start":{"rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","value":90}],"suffix":[]}},"targets":{"prefix":{"life":["base"]},"suffix":{}},"strategies":{"base_add":"Rare�� �� �ڸ��� �Ϲ� ��ǥ�� ���߷� �õ�.","divine":"�Ϲ� Divine Orb�� ���� ������ �ɼ��� ���� Ƽ�� ���� �ȿ��� �籼��. ��ǥ ��ġ�� �´� ��� ä��. ��� item."},"baseRules":[],"rules":["�̹� �༺. ���� ������ 90�� ������ ��ǥ(��Ȯ�� 90)�� �����ϸ� �ٸ� ���ǵ� ����."],"plan":{"start":"open","held":["life"],"need":[],"moves":{"acquire":[["base_add","life"]],"prepare":[["divine","item"]]},"gates":[]}}
```

호출 전 기대 방향

```text
DONE
```

open을 줬지만 입력상 이미 완성된 축성 반지여서 빈 동작 경로 한 개가 맞다.

요청별 행동 코드표

```json
{"A":["base_add","life"],"B":["divine","item"]}
```

P5/P6에서 실제 사용한 plan

```json
{"start":"open","held":["life"],"need":[],"oneOf":{},"helpers":[],"gates":[]}
```

**P6 실제 응답**

```json
{"orders":[[]]}
```

기대 1/1 · 사전 기대 모두 회수, 잘못된 방향 없음.

## S7 · �п� ������ �ձ� �迭�� �浹

입력

```json
{"base":"Attuned Wand","start":{"rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"beryl","fractured":true}],"suffix":[]}},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{}},"strategies":{"safe_alloy":"Rare�� ��� Alloy ���. �ʿ� �� ���ſ� ���̸� �غ��ϰ� ���� ��ǥ�� ���� ����� ä��.","cap_up":"��� ������ ���Ͽ� Astrid's Creativity ��ġ, Crafted �ѵ� +1. ��� item."},"baseRules":["�� ���� 1. �ѵ� ������ �迭 �浹�� �ٲ��� �ʴ´�."],"rules":["�п� beryl�� mana_skill�� ���� ��Ÿ �迭. �п��� ���š���ü�� �� ����. �ٸ� ���� ���̽��� ������� �ʴ´�."],"plan":{"start":"open","held":["beryl"],"need":["mana_skill"],"moves":{"acquire":[["safe_alloy","mana_skill"]],"prepare":[["cap_up","item"]]},"gates":[]}}
```

호출 전 기대 방향

```text
NONE
```

한도 확장으로 같은 계열의 분열 마나를 제거할 수 없다. P6가 합금 목표를 제안한 것을 오답으로 남겼다.

요청별 행동 코드표

```json
{"A":["safe_alloy","mana_skill"],"B":["cap_up","item"]}
```

P5/P6에서 실제 사용한 plan

```json
{"start":"open","held":["beryl"],"need":["mana_skill"],"oneOf":{"mana_skill":["safe_alloy"]},"helpers":[["cap_up","item"]],"gates":[]}
```

**P6 실제 응답**

```json
{"orders":[["mana_skill"]]}
```

기대 0/1 · 누락 `NONE` · 오류 해당 목표 순서를 완성할 적격 행동 조합 없음 1건 · 출처 전개 중 탈락 1개.

## S8 · ���� �� �� �Ϲ� ��ǥ �� ��� ���� ���� ä������

입력

```json
{"base":"Iron Ring","start":{"rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]}},"targets":{"prefix":{"life":["base"]},"suffix":{"cold":["base"]}},"strategies":{"first_chaos":"ù ���� ���� �ɼ� �ϳ��� ī������ �ݺ��Ͽ� �Ϲ� ��ǥ ȹ��. �п� ����. �ʿ��� ��ޡ�ù �ɼ� �غ� ����.","base_add":"Rare�� �� �ڸ��� �Ϲ� ��ǥ�� ���߷� �õ�."},"baseRules":[],"rules":["�� ������ �Ϲ� Ǯ ��θ� ���Ѵ�. ù ī������ ���� �ܰ迡�� �� ���� ��� ����."],"plan":{"start":"open","held":[],"need":["life","cold"],"moves":{"acquire":[["first_chaos","life"],["base_add","life"],["first_chaos","cold"],["base_add","cold"]]},"gates":[]}}
```

호출 전 기대 방향

```text
first_chaos.life base_add.cold
first_chaos.cold base_add.life
base_add.life base_add.cold
base_add.cold base_add.life
```

엑잘과 첫 카오스의 모든 단순 조합을 허용하면 후반 첫 카오스도 생긴다. 수신기 전개에서 첫 획득 제약을 적용한다.

요청별 행동 코드표

```json
{"A":["first_chaos","life"],"B":["base_add","life"],"C":["first_chaos","cold"],"D":["base_add","cold"]}
```

P3/P4에서 실제 사용한 plan

```json
{"start":"open","held":[],"need":["life","cold"],"gates":[],"oneOf":{"life":["A","B"],"cold":["C","D"]},"extra":{},"guards":{"A":"opening only","B":"Rare; empty affix slot","C":"opening only","D":"Rare; empty affix slot"}}
```

P5/P6에서 실제 사용한 plan

```json
{"start":"open","held":[],"need":["life","cold"],"oneOf":{"life":["first_chaos","base_add"],"cold":["first_chaos","base_add"]},"helpers":[],"gates":[]}
```

**P0 실제 응답**

```json
[
  [
    ["first_chaos", "life"]
  ],
  [
    ["base_add", "life"]
  ],
  [
    ["first_chaos", "cold"]
  ],
  [
    ["base_add", "cold"]
  ]
]
```

기대 0/4 · 누락 `first_chaos.life base_add.cold`, `first_chaos.cold base_add.life`, `base_add.life base_add.cold`, `base_add.cold base_add.life` · 오류 완성에 필요한 단계 누락 4건.

**P1 실제 응답**

```json
[[["first_chaos","life"],["first_chaos","cold"]],[["first_chaos","life"],["base_add","cold"]],[["base_add","life"],["first_chaos","cold"]],[["base_add","life"],["base_add","cold"]]]
```

기대 2/4 · 누락 `first_chaos.cold base_add.life`, `base_add.cold base_add.life` · 오류 첫 카오스 후반 재진입 2건.

**P2 실제 응답**

```json
["AC","AD","BC","BD"]
```

기대 2/4 · 누락 `first_chaos.cold base_add.life`, `base_add.cold base_add.life` · 오류 첫 카오스 후반 재진입 2건.

**P3 실제 응답**

```json
["AC","AD","BC","BD"]
```

기대 2/4 · 누락 `first_chaos.cold base_add.life`, `base_add.cold base_add.life` · 오류 첫 카오스 후반 재진입 2건.

**P4 실제 응답**

```json
{"orders":[["life","cold"],["cold","life"]],"extras":[]}
```

기대 4/4 · 출처 전개 중 탈락 4개 · 사전 기대 모두 회수, 잘못된 방향 없음.

**P5 실제 응답**

```json
[["life","cold"],["cold","life"]]
```

기대 4/4 · 출처 전개 중 탈락 4개 · 사전 기대 모두 회수, 잘못된 방향 없음.

**P6 실제 응답**

```json
{"orders":[["life","cold"],["cold","life"]]}
```

기대 4/4 · 출처 전개 중 탈락 4개 · 사전 기대 모두 회수, 잘못된 방향 없음.

## S9 · �̻�� �� �� �������� �ձ� ���ΰ� �̹� ����

입력

```json
{"base":"Attuned Wand","start":{"rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"mana_skill","crafted":true}],"suffix":[]}},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"]}},"strategies":{"safe_alloy":"Rare�� ��� Alloy ���. �ʿ� �� ���ſ� ���̸� �غ��ϰ� ���� ��ǥ�� ���� ����� ä��.","safe_ess":"Rare�� ��� Perfect Essence ���. �ʿ� �� ���ſ� ���̿� ���� ¡���� �غ��� ���� ��ǥ ����.","cap_up":"��� ������ ���Ͽ� Astrid's Creativity ��ġ, Crafted �ѵ� +1. ��� item."},"baseRules":["���� 1/1 Crafted. �� ���Ͽ� Astrid's Creativity ��ġ �� 2�� ����."],"rules":["�� ��ǥ ��� Crafted. ���� mana_skill�� �����Ѵ�."],"plan":{"start":"open","held":["mana_skill"],"need":["skill"],"moves":{"acquire":[["safe_ess","skill"]],"prepare":[["cap_up","item"]]},"gates":[]}}
```

호출 전 기대 방향

```text
cap_up.item safe_ess.skill
```

분열이 아닌 기존 Crafted 접두가 자리를 사용한다. 목표 순서는 skill 하나이고, 전개기가 추가 Crafted를 붙이기 직전에 한도를 늘린다.

요청별 행동 코드표

```json
{"A":["safe_ess","skill"],"B":["cap_up","item"]}
```

P5/P6에서 실제 사용한 plan

```json
{"start":"open","held":["mana_skill"],"need":["skill"],"oneOf":{"skill":["safe_ess"]},"helpers":[["cap_up","item"]],"gates":[]}
```

**P6 실제 응답**

```json
{"orders":[["skill"]]}
```

기대 1/1 · 사전 기대 모두 회수, 잘못된 방향 없음.

## S10 · �̻�� �� �� �༺ ������ ��ġ �̴�

입력

```json
{"base":"Iron Ring","start":{"rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","value":89}],"suffix":[]}},"targets":{"prefix":{"life":["base"]},"suffix":{}},"strategies":{"base_add":"Rare�� �� �ڸ��� �Ϲ� ��ǥ�� ���߷� �õ�.","divine":"�Ϲ� Divine Orb�� ���� ������ �ɼ��� ���� Ƽ�� ���� �ȿ��� �籼��. ��ǥ ��ġ�� �´� ��� ä��. ��� item."},"baseRules":[],"rules":["�̹� �༺. ���� ���¸� ���Ǹ� ������ ��ǥ�� ��Ȯ�� 90."],"plan":{"start":"open","held":["life"],"need":["수치조건"],"moves":{"prepare":[["divine","item"]]},"gates":[]}}
```

호출 전 기대 방향

```text
NONE
```

미달 수치를 가진 축성 반지의 정답은 빈 후보 목록이다. 모델은 수치 목표를 한 행에서 두 번 반복했다. JSON Schema의 enum은 중복·도달 가능성을 검증하지 않는다.

요청별 행동 코드표

```json
{"A":["divine","item"]}
```

P5/P6에서 실제 사용한 plan

```json
{"start":"open","held":["life"],"need":["수치조건"],"oneOf":{"수치조건":["divine"]},"helpers":[],"gates":[]}
```

**P6 실제 응답**

```json
{"orders":[["수치조건","수치조건"]]}
```

기대 0/1 · 누락 `NONE` · 오류 목표 목록에 준비 행동·중복·누락 포함 1건.

## H1 · Magic 장갑 · 한 목표를 얻는 두 등급 상승 수단

입력

```json
{"base":"Stocky Mitts","start":{"rarity":"magic","ilvl":82,"mods":{"prefix":[],"suffix":[{"id":"filler"}]}},"targets":{"prefix":{"life":["base","Greater Essence of the Body"]},"suffix":{}},"strategies":{"upgrade_ess":"Magic�� Rare�� �ø��� ��� ������ �ɼ� ȹ��.","regal_base":"Magic�� Rare�� �ø��� �Ϲ� ��ǥ�� ���� ��� ä��."},"baseRules":[],"rules":["일반 생명력을 확보하고 Rare로 마치면 완성. 기존 접미 유지."],"plan":{"start":"open","held":[],"need":["life"],"moves":{"acquire":[["regal_base","life"],["upgrade_ess","life"]]},"gates":[]}}
```

호출 전 기대 방향

```text
upgrade_ess.life
regal_base.life
```

목표 순서 한 행에서 등급 상승의 두 적격 수단을 복원했다.

요청별 행동 코드표

```json
{"A":["regal_base","life"],"B":["upgrade_ess","life"]}
```

P5/P6에서 실제 사용한 plan

```json
{"start":"open","held":[],"need":["life"],"oneOf":{"life":["regal_base","upgrade_ess"]},"helpers":[],"gates":[]}
```

**P6 실제 응답**

```json
{"orders":[["life"]]}
```

기대 2/2 · 사전 기대 모두 회수, 잘못된 방향 없음.

## H2 · 마법봉 · 소켓 없음, 두 Crafted 목표

입력

```json
{"base":"Attuned Wand","start":{"rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]}},"targets":{"prefix":{"mana_skill":["Celestial Alloy"]},"suffix":{"skill":["Perfect Essence of Sorcery"]}},"strategies":{"safe_alloy":"Rare�� ��� Alloy ���. �ʿ� �� ���ſ� ���̸� �غ��ϰ� ���� ��ǥ�� ���� ����� ä��.","safe_ess":"Rare�� ��� Perfect Essence ���. �ʿ� �� ���ſ� ���̿� ���� ¡���� �غ��� ���� ��ǥ ����."},"baseRules":["한도 1. 소켓과 한도 증가 수단 없음."],"rules":["각 전용 목표는 Crafted 1자리를 쓴다. 두 옵션을 동시에 유지해야 한다."],"plan":{"start":"open","held":[],"need":["mana_skill","skill"],"moves":{"acquire":[["safe_alloy","mana_skill"],["safe_ess","skill"]]},"gates":[]}}
```

호출 전 기대 방향

```text
NONE
```

소켓과 확장 행동이 없으므로 어느 순서에서도 두 Crafted를 유지할 수 없다. P6는 두 순서 모두를 제안했다.

요청별 행동 코드표

```json
{"A":["safe_alloy","mana_skill"],"B":["safe_ess","skill"]}
```

P5/P6에서 실제 사용한 plan

```json
{"start":"open","held":[],"need":["mana_skill","skill"],"oneOf":{"mana_skill":["safe_alloy"],"skill":["safe_ess"]},"helpers":[],"gates":[]}
```

**P6 실제 응답**

```json
{"orders":[["mana_skill","skill"],["skill","mana_skill"]]}
```

기대 0/1 · 누락 `NONE` · 오류 해당 목표 순서를 완성할 적격 행동 조합 없음 2건 · 출처 전개 중 탈락 2개.

## H3 · 목걸이 · 목표 4개, 분열 힘과 생명력 이미 보유

입력

```json
{"base":"Solar Amulet","start":{"rarity":"rare","ilvl":82,"mods":{"prefix":[{"id":"life","value":90}],"suffix":[{"id":"strength","fractured":true}]}},"targets":{"prefix":{"life":["base","Greater Essence of the Body","Preserved Collarbone","Ancient Collarbone","Altered Collarbone"],"def":["Perfect Essence of Enhancement"]},"suffix":{"strength":["base","Preserved Collarbone","Altered Collarbone"],"cold":["base","Preserved Collarbone","Altered Collarbone"]}},"strategies":{"base_add":"Rare�� �� �ڸ��� �Ϲ� ��ǥ�� ���߷� �õ�.","reveal":"����� �Ѽ� ���� ������ ����� ���� ��Ͽ��� ��ǥ�� ����.","safe_ess":"Rare�� ��� Perfect Essence ���. �ʿ� �� ���ſ� ���̿� ���� ¡���� �غ��� ���� ��ǥ ����."},"baseRules":["Rare 3���Ρ�3����. Crafted �ѵ� 1."],"rules":["기존 목표를 보존한다. cold는 일반 추첨 또는 적격 훼손 공개, def는 보호형 에센스로 얻는다. 소모성 더미 준비는 safe_ess에 포함."],"plan":{"start":"open","held":["life","strength"],"need":["def","cold"],"moves":{"acquire":[["base_add","cold"],["reveal","cold"],["safe_ess","def"]]},"gates":[]}}
```

호출 전 기대 방향

```text
safe_ess.def base_add.cold
safe_ess.def reveal.cold
base_add.cold safe_ess.def
reveal.cold safe_ess.def
```

최종 옵션은 네 개이고 그중 두 개는 시작부터 확보했다. 나머지 둘의 두 순서에서 네 기대 방향을 모두 복원했다.

요청별 행동 코드표

```json
{"A":["base_add","cold"],"B":["reveal","cold"],"C":["safe_ess","def"]}
```

P5/P6에서 실제 사용한 plan

```json
{"start":"open","held":["life","strength"],"need":["def","cold"],"oneOf":{"def":["safe_ess"],"cold":["base_add","reveal"]},"helpers":[],"gates":[]}
```

**P6 실제 응답**

```json
{"orders":[["def","cold"],["cold","def"]]}
```

기대 4/4 · 사전 기대 모두 회수, 잘못된 방향 없음.

## H4 · 반지 · 2개 목표에 훼손을 각각 쓸 수 있으나 한 슬롯을 공유

입력

```json
{"base":"Iron Ring","start":{"rarity":"rare","ilvl":82,"mods":{"prefix":[],"suffix":[]}},"targets":{"prefix":{"life":["base","Preserved Collarbone"]},"suffix":{"cold":["base","Preserved Collarbone"]}},"strategies":{"base_add":"Rare�� �� �ڸ��� �Ϲ� ��ǥ�� ���߷� �õ�.","reveal":"적격 재료로 한 옵션을 훼손 공개한다."},"baseRules":[],"rules":["훼손 슬롯은 하나이며 이미 공개한 목표 옵션을 보존한다. 일반 엑잘은 빈 양쪽에 가능."],"plan":{"start":"open","held":[],"need":["life","cold"],"moves":{"acquire":[["reveal","cold"],["base_add","life"],["reveal","life"],["base_add","cold"]]},"gates":[]}}
```

호출 전 기대 방향

```text
base_add.life base_add.cold
base_add.cold base_add.life
reveal.life base_add.cold
base_add.cold reveal.life
base_add.life reveal.cold
reveal.cold base_add.life
```

두 목표의 순서 두 개에서 일반 풀과 훼손을 펼쳤다. 훼손을 두 번 쓰는 두 조합을 제외한 여섯 기대 방향이 남았다.

요청별 행동 코드표

```json
{"A":["reveal","cold"],"B":["base_add","life"],"C":["reveal","life"],"D":["base_add","cold"]}
```

P5/P6에서 실제 사용한 plan

```json
{"start":"open","held":[],"need":["life","cold"],"oneOf":{"life":["base_add","reveal"],"cold":["reveal","base_add"]},"helpers":[],"gates":[]}
```

**P6 실제 응답**

```json
{"orders":[["life","cold"],["cold","life"]]}
```

기대 6/6 · 출처 전개 중 탈락 2개 · 사전 기대 모두 회수, 잘못된 방향 없음.
