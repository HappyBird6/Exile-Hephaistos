# 장비 제작 전략 후속 실험

## 결과

실제 API 호출 50회로 8개 장비 종류의 베이스 ID 31개를 검사했다. **첫 응답에서 완주 후보가 있었던 카드는 18/31장, 재요청을 포함하면 27/31장**이다. Invoking Belt의 응답은 목표를 완성한 뒤에 행동이 더 있었다. 그 지점에서 멈추는 기존 판정까지 포함하면 28/31장이다. 세 카드는 여전히 실행 가능한 모델 후보가 0개다.

[기대·전송·실제 응답과 거절 사유](equipment-strategy-responses.md) · [카드 사전, 사전 기대 경로, 통과 후보와 원문 위치](equipment-strategy-cards.json)

| 문제 | 호출 | 검사 카드 | 첫 응답 완주 카드 | 누적 완주 카드 | 완성 시점 종료 포함 카드 | 원문 경로 | 완성 시점 종료 포함 경로 | 기대 경로 |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| 고정·명시 수치 재굴림 | 10 | 5 | 2 | 4 | 4 | 11 | 11 | 27 |
| 최저 레벨 카오스 교체 | 10 | 4 | 1 | 3 | 3 | 25 | 25 | 39 |
| 합금용 제거 대상 확보 | 10 | 5 | 1 | 3 | 4 | 3 | 4 | 14 |
| 홈 개방과 룬 장착 | 10 | 9 | 8 | 9 | 9 | 14 | 14 | 75 |
| 증강물 전용 옵션 풀 활성화 | 10 | 8 | 6 | 8 | 8 | 21 | 21 | 24 |
| 합계 | 50 | 31 | 18 | 27 | 28 | 74 | 75 | 179 |

기대 179개 중 75개를 회수했고 104개의 실행 순서가 빠졌다. 첫 응답이 0개였던 13장 중 9장은 이후 응답에서 완주했다. 한 장은 이미 완성된 응답의 꼬리를 제외해 회수했다. 사전 등록한 양성 49장 중 재요청 때문에 모델에 보내지 않은 18장은 분모에서 제외했다.

### 후보가 끝까지 0개였던 카드

| 카드 | 기대와 실제 응답 | 검토 결과 |
|---|---|---|
| Primed Quiver | 0/3. 고정 공격 속도 10%와 명시 관통 확률 14%를 요구했다. 예를 들어 Blessed Divine으로 고정 옵션을 올리고, 엑잘로 A를 12%에 얻은 뒤 일반 Divine으로 14%까지 올리면 된다. 16번은 마지막 재굴림을 누락했고, 17번은 이미 있는 B를 획득하려 했다. | 전송 문구에도 결함이 있었다. 구조화된 시작 상태는 B만 보유하고 A는 미보유인데, 공통 문장이 A를 낮은 값으로 보유했다고 설명한 뒤 예외 문장에서 번복했다. 다음 초안에서는 보유·미보유를 시작 상태 한 곳에서만 표현해야 한다. 이 수정의 모델 효과는 아직 측정하지 않았다. |
| Wrath Sceptre | 0/8. X(레벨 16)를 일반 Whittling 카오스로 A(75)로 바꾸고, 이어 Y(25)를 B(21)로 바꾸는 경로가 성립한다. 22·24번은 한 번의 교체에서 멈췄고 23번에는 첫 행동에서 최저 레벨이 아닌 Y를 제거하려는 후보가 있었다. | 두 번째 교체는 첫 교체를 적용한 뒤 남은 X·Y와 새로 붙은 A·B의 요구 레벨을 함께 봐야 한다. 후속 초안에는 각 교체 후 남은 제거 대상과 획득 대상을 명시하는 방안을 남긴다. |
| Kinetic Ring | 0/2. 시작부터 최대 마나 X가 있다. Runic Alloy로 X를 최대 Runic Ward A로 바꾸고 엑잘로 처치 시 마나 B를 붙이거나, B를 먼저 붙인 뒤 X를 교체할 수 있다. 35번은 이미 있는 X를 다시 뽑거나 필요한 B를 합금으로 제거했다. | 합금 묶음의 열 번째 호출에서 발견했다. 기존 X가 이미 지불 가능한 제거 대상이라는 사실을 입력에 직접 표시하는 방안은 미검증이다. |

### 회수한 경로와 판정 근거

- **Bone Wand.** 처음에는 제거할 명시 옵션이 없다. 10번은 중간에서 멈췄지만 11번은 엑잘로 임시 X를 획득하고 **Transcendent Alloy로 X를 A로 교체한 뒤 엑잘로 B를 획득**하는 세 단계를 모두 반환했다. 네 기대 경로 중 하나를 회수했다.
- **Sacral Quiver.** 12·13번은 한 번의 교체만 제안했다. 각 첫 행동을 별도 필드로 제공하고 완성 경로 전체를 적게 한 14번이 12/12를 회수했다. 같은 방식을 사용한 Wrath Sceptre는 0/8이었다.
- **Invoking Belt.** 원문 완주는 0/2였다. 21번에 모델이 쓴 Runic Alloy로 X → A 교체, 엑잘로 최대 생명력 B 획득까지가 유효했다. 완성 후의 행동을 제외해 1/2로 보존했다.
- **전용 옵션 풀.** 빈 홈에 Thrud’s Might 또는 Medved’s Tending을 장착해야 A를 추첨할 수 있다. 일반 B는 장착 전에도 얻을 수 있다. 작업대에 장착 전후의 실제 Exalted 후보 풀을 적용했다. 첫 응답이 0개였던 Acrid Wand와 Offering Wand는 피드백 다음 호출에서 각각 2/3을 회수했다.

### 검증 범위와 예산

기준 커밋은 `9e7d3b56`이다. 이전 실험에 사전 등록한 베이스 90개의 카탈로그 파일과 이번 양성 카드의 파일을 대조했다. 같은 표시명은 원본 베이스 ID로 구분했다. 이번 기대 경로는 각 카드에 적은 재료 재고와 전략의 성공 결과를 전제로 한다. 모든 게임 행동이나 모든 수치 결과를 열거한 수치로 해석할 수 없다.

Java 21에서 현재 카탈로그를 읽고 `WorkbenchSimulator`의 실제 상태 전이를 실행했다. 홈 장착은 `SocketAugments.install`, 장착별 추첨 풀은 서비스와 같은 `forItem`, 최저 레벨 교체는 `automaticPlan`의 후보와 실제 카오스 실행을 대조했다. 별도 계산에서 상태·재고·목표를 따라 첫 달성까지 열거한 결과가 양성 49장의 사전 기대와 일치했다. 비용과 확률은 산출하지 않았다. 같은 카드에서 모델이 직접 제안한 경로만 합집합에 누적했다. 이전 218개 및 그보다 앞선 49개 경로의 오프라인 검산도 다시 통과했다.

이번에는 한 건씩 응답을 확인하고 최초 완주가 0개인 카드에 같은 문제의 남은 예산에서 보완 요청을 먼저 보냈다. 최초 네 묶음은 한 카드에서 기본·피드백·첫 행동별 형식까지 검사한 후 다음 카드를 진행했으므로, 미회수 카드 하나에 예산 전부를 쓰지는 않았다. 첫 행동별 형식도 실패한 Primed Quiver와 Wrath Sceptre, 합금 한도에서 발견된 Kinetic Ring은 보류한다. 다섯 문제는 각각 10회에서 멈췄다. 기존 보류 문제를 다시 호출한 횟수는 0회다. 누적 199/300회, 미사용 101회다.

## 호출 전 고정한 카드와 기대 응답

아래 각 실행열은 이번 카드에 제공한 재료 수와 명시한 성공 결과에서 목표에 처음 도달하는 순서다. Java 작업대에서 해당 결과로 난수 추첨을 고정해 실행하고, 별도로 재료 재고·슬롯·수치·제거 레벨을 추적해 같은 집합이 나오는지 대조했다. 40장의 기대 경로를 실제 요청 전에 고정했다.

| 카드 | 베이스 | 분류 | 시작 명시 옵션 → 최종 명시 옵션 | 기대 수 |
|---|---|---|---|---:|
| roll-1 | Topaz Ring | 고정·명시 수치 재굴림 | A → A, B | 6 |
| alloy-1 | Lament Amulet | 합금용 제거 대상 확보 | X → A, B | 2 |
| whittle-1 | Rawhide Belt | 최저 레벨 카오스 교체 | X, Y → A, B | 7 |
| socket-1 | Slipstrike Vest | 홈 개방과 룬 장착 | 없음 → A | 12 |
| roll-2 | Conjurer Mantle | 고정·명시 수치 재굴림 | A → A, B | 6 |
| alloy-2 | Bone Wand | 합금용 제거 대상 확보 | 없음 → A, B | 4 |
| whittle-2 | Sacral Quiver | 최저 레벨 카오스 교체 | X, Y → A, B | 12 |
| socket-2 | Blacksteel Gauntlets | 홈 개방과 룬 장착 | 없음 → A | 3 |
| roll-3 | Primed Quiver | 고정·명시 수치 재굴림 | B → A, B | 3 |
| alloy-3 | Invoking Belt | 합금용 제거 대상 확보 | X → A, B | 2 |
| whittle-3 | Wrath Sceptre | 최저 레벨 카오스 교체 | X, Y → A, B | 8 |
| socket-3 | Polished Bracers | 홈 개방과 룬 장착 | 없음 → A | 3 |
| roll-4 | Azure Amulet | 고정·명시 수치 재굴림 | A → A, B | 6 |
| alloy-4 | Adherent Cuffs | 합금용 제거 대상 확보 | 없음 → A, B | 4 |
| whittle-4 | Lazuli Ring | 최저 레벨 카오스 교체 | X, Y → A, B | 12 |
| socket-4 | Utzaal Cuirass | 홈 개방과 룬 장착 | 없음 → A | 15 |
| roll-5 | Heavy Belt | 고정·명시 수치 재굴림 | A → A, B | 6 |
| alloy-5 | Kinetic Ring | 합금용 제거 대상 확보 | X → A, B | 2 |
| whittle-5 | Amber Amulet | 최저 레벨 카오스 교체 | X, Y → A, B | 7 |
| socket-5 | Death Mantle | 홈 개방과 룬 장착 | 없음 → A | 12 |
| roll-6 | Two-Stone Ring | 고정·명시 수치 재굴림 | B → A, B | 3 |
| alloy-6 | Corsair Coat | 합금용 제거 대상 확보 | 없음 → A, B | 4 |
| whittle-6 | Shrouded Mail | 최저 레벨 카오스 교체 | X, Y → A, B | 12 |
| socket-6 | Massive Mitts | 홈 개방과 룬 장착 | 없음 → A | 3 |
| roll-7 | Dastard Armour | 고정·명시 수치 재굴림 | A → A, B | 6 |
| alloy-7 | Two-Point Quiver | 합금용 제거 대상 확보 | X → A, B | 2 |
| whittle-7 | Volatile Wand | 최저 레벨 카오스 교체 | X, Y → A, B | 8 |
| socket-7 | Falconer's Jacket | 홈 개방과 룬 장착 | 없음 → A | 12 |
| roll-8 | Pearlescent Amulet | 고정·명시 수치 재굴림 | A → A, B | 6 |
| alloy-8 | Stoic Sceptre | 합금용 제거 대상 확보 | 없음 → A, B | 4 |
| whittle-8 | War Wraps | 최저 레벨 카오스 교체 | X, Y → A, B | 12 |
| socket-8 | Sirenscale Gloves | 홈 개방과 룬 장착 | 없음 → A | 3 |
| roll-9 | Blunt Quiver | 고정·명시 수치 재굴림 | B → A, B | 3 |
| alloy-9 | Gloam Ring | 합금용 제거 대상 확보 | X → A, B | 2 |
| whittle-9 | Critical Wand | 최저 레벨 카오스 교체 | X, Y → A, B | 7 |
| socket-9 | Wolfskin Mantle | 홈 개방과 룬 장착 | 없음 → A | 12 |
| roll-10 | Long Belt | 고정·명시 수치 재굴림 | A → A, B | 6 |
| alloy-10 | Lunar Amulet | 합금용 제거 대상 확보 | 없음 → A, B | 4 |
| whittle-10 | Emerald Ring | 최저 레벨 카오스 교체 | X, Y → A, B | 12 |
| socket-10 | Thane Mail | 홈 개방과 룬 장착 | 없음 → A | 15 |

## 전용 옵션 풀의 추가 사전 검사

기존 홈 하나가 열린 카드에서 증강물을 장착한 뒤에만 A를 추첨할 수 있다. B는 일반 옵션이다. 원본 풀 연결에서 고른 10개 중 Rattling Sceptre는 현재 작업대의 홈·장착 자격 검사를 통과하지 않아 양성 목록에서 제외했다. 나머지 아홉 베이스에서 각 3개 경로가 실제 작업대와 독립 판정으로 일치한 후 추가 요청을 시작했다.

| 카드 | 베이스 | 필요한 증강물 | 기대 수 |
|---|---|---|---:|
| pool-1 | Dueling Wand | Thruds_Might | 3 |
| pool-2 | Abyssal Cuirass | Medveds_Tending | 3 |
| pool-4 | Galvanic Wand | Thruds_Might | 3 |
| pool-5 | Rusted Cuirass | Medveds_Tending | 3 |
| pool-6 | Acrid Wand | Thruds_Might | 3 |
| pool-7 | Primordial Wand | Thruds_Might | 3 |
| pool-8 | Offering Wand | Thruds_Might | 3 |
| pool-9 | Shrouded Mail | Medveds_Tending | 3 |
| pool-10 | Siphoning Wand | Thruds_Might | 3 |
