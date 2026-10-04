Latest checkpoint: [bounded Attuned Wand and11 Essence paths](workbench-attuned-wand-2026-10-03.md), overall109 / current-scope155: implemented101, pending54, excluded65. Prior counts below describe their dated checkpoints.

Latest checkpoint: [six Bow Perfect Essences](workbench-bow-perfect-2026-10-03.md), overall101 / current-scope155: implemented93, pending62, excluded65. Earlier counts and candidate statements below describe their dated checkpoints.

Current bounded Workbench extension: [Crude Bow ordinary catalog and basic Essences](workbench-crude-bow-2026-10-03.md), without computed weapon totals or socket/resource state. Existing Solar/Stocky films retain their format.

> 2026-10-03 Workbench extension: optional nullable `augmentSockets` preserves a reviewed ordinary Stocky empty-socket count0/1. Missing/null on old items means unknown; only fresh Stocky placement supplies zero. All ordinary Workbench copies/crafts/refusals retain it. Support/Explorer bucket projection remains affix-only and does not create socket state. [Delivered Artificer path, compatibility and evidence](workbench-stocky-artificer-2026-10-03.md).
> 2026-10-04 correction: optional `catalystQuality` retains one type and amount on reviewed Solar/Iron/Sapphire states; absent/null legacy fields remain unsupplied. User-confirmed cap loss preserves existing quality40 when the cap becomes20. The current cap is not a blanket stored-value ceiling: Solar accepts reviewed reachable0–40, Iron/Sapphire0–20. Catalyst actions set `max(existing,currentCap)` and replace the type under an explicit simulator convenience policy. Supported Workbench actions preserve typed quality and canonical rolls. Derived effects use a replaceable provisional HALF_UP policy. Support/Explorer remain affix-only. [Current correction and validation](workbench-quality-preserve-2026-10-04.md), [earlier foundation evidence](workbench-catalyst-quality-2026-10-04.md).

# Solar Amulet ItemState

2026-10-01 추가: 독립 Workbench에서 일반 화폐 6종의 실제 수치 roll과 적용을 지원한다. 아래 버킷 탐색 모델과 별도이며 [Workbench simulator 명세](workbench-simulator.md)를 따른다. 붙여넣기 catalog 매핑과 특수 제작은 아직 미지원이다.

2026-09-29 · 개발 계획 1~9단계 구현 범위

## 범위

첫 베이스는 Solar Amulet (`Metadata/Items/Amulets/FourAmulet9`)이다. Normal, Magic, Rare와 PoE2DB Amulets의 **Base Prefix / Base Suffix**만 다룬다. 이 문서의 데이터는 조회 시점의 스냅샷이며, 확인하지 않은 게임 패치 번호를 부여하지 않는다.

일반 Transmutation, Augmentation, Regal, Exalted, Annulment, Chaos의 후보·확률·상태 전이를 구현한다. Greater/Perfect 화폐, Essence, Omen, 타락·성역화·분열·제작 속성 등 특수 제작은 제외한다. 화면의 전체 재료 목록은 지원 화폐 목록이 아니다.

API·UI는 기본 Solar Amulet에서 시작하는 확률 탐색에 연결한다. 기존 텍스트 파서의 catalog 식별은 후속 단계다. `/api/v1/items/parse` 응답은 계속 표시용 데이터이며, 파싱 성공을 유효한 ItemState로 간주하지 않는다. 입력한 아이템에서는 기본 베이스로 돌아가야 확률을 탐색할 수 있다.

## 시작 상태

`ItemCatalogLoader.loadDefault()`로 번들 스냅샷을 읽고 `SolarAmulet.initial(catalog)`로 시작 상태를 생성한다.

- 베이스: Solar Amulet
- 아이템 레벨: 82 (초기 프리셋 선택; 게임의 필수 레벨이 아님)
- 희귀도: Normal
- implicit: Spirit 15 (10~15 범위 안의 고정 프리셋 값; 무작위 추첨이 아님)
- explicit: 없음
- 특수 상태: 없음

`SolarAmulet.initial(catalog, itemLevel, spirit)`로 레벨과 implicit 값을 지정할 수 있다. 화면은 아이템 레벨 1~100을 입력받고 implicit은 15로 고정한다. 수치가 명확한 실제 아이템만 `ModifierInstance`로 표현한다. 미래의 explicit은 버킷에서 정의 ID와 범위로 표시하며 임의의 수치를 실제 roll로 저장하지 않는다.

## 데이터 스냅샷

위치: `backend/src/main/resources/catalog/solar-amulet/`

| 파일 | 내용 |
|---|---|
| `base.raw.json` | 페이지의 `normal`(Base) 배열 209행을 UTF-8/LF JSON으로 보존 |
| `details.raw.json` | 각 상세 페이지의 출처·이름·family·레벨·원본 Stats 구간과 Solar implicit |
| `catalog.json` | 계산 모델용 정의와 베이스, 수집 시각·출처·검증용 SHA-256·행 수·가중치 합계 |

접두 81행, 접미 128행이며 모든 레벨을 포함한 가중치 합계는 각각 72,200 / 96,656이다. 이 합계는 수집 검증값이고 특정 아이템의 확률 분모가 아니다. 실제 분모는 후보 선택 단계에서 레벨·슬롯·충돌을 적용한 뒤 구한다.

가중치는 사용자의 결정에 따라 **PoE2DB 게시값 그대로** 사용한다. `DropChance`가 1인 행도 1로 보존한다. 상세 페이지 Spawn Tags의 1/0은 Base 표의 가중치를 대신하지 않는다. 별도의 공식 확률이나 같은 확률이라는 가정을 덧붙이지 않는다.

표의 tier는 PoE2DB `ModsView`의 표시 기준과 일치시켰다. Base 내에서 접두/접미와 첫 family를 기준으로 묶고, 서로 다른 `Level`을 내림차순 정렬한 1-based 위치다. 아이템 레벨 필터를 바꿔도 tier를 재번호 매기지 않는다. tier는 직접 가중치가 아니며 원본 Level과 함께 보존한다.

원본 표는 내부 modifier code를 제공하지 않으므로 `amulet:<prefix|suffix>:<영문 이름 slug>`를 이 스냅샷의 내부 ID로 사용한다. 현재 209행은 이름과 내부 ID가 각각 유일하다. 캐시 hover URL의 해시는 게임 modifier ID로 취급하지 않는다. 후속 수집에서 이름 변경·ID 충돌이 생기면 검토 후 새 스냅샷으로 반영한다.

Stats의 ID는 상세 페이지의 stat label에서 공백을 `_`로 바꾼 식별자다. 공식 game code라는 의미는 없다. 값은 상세 Stats의 정수 단위를 그대로 사용한다. 예를 들어 내부 수치가 분당 단위인 경우 화면의 초당 수치를 그대로 이 값으로 넣으면 안 된다. 원본 표시 문구와 원본 Stats 구간을 함께 보존하며 표시용 파서 수치를 자동으로 연결하지 않는다.

## 모델과 검증

- `ModifierDefinition`: layer, 접두/접미, family 집합, 출현 레벨, weight, tier, stat 범위, 태그와 출처.
- `ModifierInstance`: 정의 ID와 실제 stat 값. Map을 방어적으로 복사한다.
- `ItemState`: snapshot ID, base ID, item level, rarity, implicit/explicit 인스턴스와 특수 condition. 리스트를 복사하고 정렬해 표시 순서에 무관한 값 동등성을 제공한다.
- `ItemCatalog`: 불변 정의 인덱스. 중복 ID, 베이스 implicit 누락, 행 수/가중치 합계 불일치를 거부한다.
- `ItemStateValidator`: 오류 목록을 반환하며 입력을 수정하거나 속성을 제거하지 않는다.

확률·화폐 이력·부모 노드·누적 비용은 ItemState에 넣지 않는다. 도메인 모델은 Spring/Jackson/DB/네트워크에 의존하지 않는다. Jackson 기반 파일 로더만 infrastructure에 둔다.

검증 규칙:

1. state와 catalog의 snapshot/base가 일치해야 한다.
2. 초기 지원 범위는 특수 condition 없는 Normal/Magic/Rare다. Unique와 알려진 특수 condition은 오류로 보고한다.
3. Solar Spirit implicit 하나가 필요하며 explicit 슬롯을 점유하지 않는다.
4. explicit 슬롯은 Normal 0/0, Magic 1/1, Rare 3/3이며 베이스 정의에서 읽는다.
5. explicit 인스턴스끼리 family가 하나라도 겹치면 충돌이다. 태그나 표시 문구 유사성으로 판단하지 않는다. implicit의 같은 family는 이 검사에서 제외한다.
6. 알려진 modifier ID, 올바른 layer, 정확한 stat 집합과 범위를 검사한다. 여러 stat를 가진 modifier도 슬롯은 하나다.
7. explicit이 0개인 Magic/Rare를 허용하고 희귀도를 임의 변경하지 않는다.
8. `validate`는 기존 아이템의 출현 레벨 조건을 강요하지 않는다. `validateForGeneration`은 **전체 explicit을 새로 생성한 상태**의 레벨 적합성을 추가 검사한다. 화폐 후보 선택기는 새로 붙일 속성만 출현 레벨을 검사한다.

`slots`는 검증된 상태에서 사용한다. 슬롯 여유만으로 어떤 화폐가 사용 가능한지 판단할 수 없다. unknown modifier가 있는 상태는 검증 오류부터 처리해야 한다.

## 수집 방식과 후속 크롤러

이번에는 공개 페이지와 각 속성 상세를 한 번 수집·검토했다. 앱 런타임 네트워크 접근, 주기적 크롤러, Python 서비스는 추가하지 않았다.

후속 수집기는 동일한 세 파일을 생성하면 된다. 갱신할 때 행 수·가중치·tier·충돌 그룹·stat 변경을 비교하고, 검증을 통과한 새 snapshot ID로 교체한다. 실행 중 카탈로그를 부분 수정하거나 같은 ID에 다른 데이터를 덮어쓰지 않는다. 기존 ItemState는 생성 당시의 snapshot과 함께 해석한다.

PostgreSQL·Redis 기반은 유지하며 이번 계산에 DB 저장이나 분산 캐시는 추가하지 않는다. 추후 카탈로그 공급 경로가 바뀌어도 순수 모델은 재사용한다.

## 출처와 표시

- [PoE2DB Amulets Base modifier 표](https://poe2db.tw/us/Amulets#ModifiersCalc)
- [PoE2DB Solar Amulet](https://poe2db.tw/us/Solar_Amulet)
- [PoE2DB 가중치 수집 설명](https://poe2db.tw/us/weightings)
- [PoE2DB tier 표시 구현](https://cdn.poe2db.tw/js/ModsView.f39fca410dd746d3.js)
- [PoE2DB 제작 규칙](https://poe2db.tw/Crafting)

확률 UI에는 `Probabilities are calculated using PoE2DB modifier weights.`와 출처 링크·데이터 갱신일을 표시한다. 게임 서버의 공식 확률이나 결과 보장을 뜻하지 않는다.

게임 데이터의 권리는 Grinding Gear Games에 있다. PoE2DB 출처와 개별 상세 링크를 보존한다. PoE2DB의 wiki 설명은 별도 표시가 없으면 [CC BY-NC-SA 3.0](https://creativecommons.org/licenses/by-nc-sa/3.0/)로 안내되어 있다. 이 스냅샷을 자체 제작한 원본 게임 데이터로 표시하지 않는다.

## 검증 기준

전체 209행을 보존 원본과 비교해 이름·방향·레벨·weight·family·tag·tier를 대조하고, 상세 210건의 stat 범위를 비교한다. raw checksum과 누락 필드도 검사한다. 상태 테스트는 빈 희귀도, 슬롯 한도, 충돌, implicit 공존, 생성 레벨 경계, 값 범위, 미지원 입력, 불변성·순서 동등성, 다중 family와 hybrid fixture를 포함한다. ArchUnit은 새 모델의 도메인 의존성을 검사한다.

## 4단계: 후보와 화폐 전이

`ModifierPoolResolver`는 스냅샷별로 레벨 1~100, 접두/접미, family의 BitSet 인덱스를 한 번 만든다. 현재 상태에서 새로 붙일 수 있는 속성만 남긴 뒤 `weight / 후보 weight 합계`로 확률을 계산한다. 접두/접미를 먼저 50:50으로 뽑지 않는다. 기존 속성과 family 집합이 하나라도 겹치거나 해당 방향 슬롯이 가득 차면 후보에서 제외한다. 슬롯 한도는 베이스 정의를 사용한다.

| 화폐 | 대상과 전이 |
|---|---|
| Transmutation | Normal → Magic으로 바꾼 뒤 1개 추가 |
| Augmentation | Magic에 빈 슬롯과 후보가 있으면 1개 추가 |
| Regal | Magic → Rare로 바꾼 뒤 Rare 슬롯 기준으로 1개 추가 |
| Exalted | Rare에 빈 슬롯과 후보가 있으면 1개 추가 |
| Annulment | Magic/Rare의 explicit 중 1개 제거, 희귀도 유지 |
| Chaos | Rare에서 1개 제거한 후 그 중간 상태의 후보로 1개 추가 |

Annulment/Chaos의 제거 단계는 현재 explicit 각각에 `1 / explicit 개수`를 적용한다. 제거 가중치로 출현 weight를 쓰지 않는다. Chaos의 재추첨에는 제거된 family와 같은 속성도 다시 들어갈 수 있다. 각 제거 경로의 확률과 추가 확률을 곱하고, 같은 결과 상태로 모이면 더한다. 결과가 원상태인 self-loop도 남긴다. 이 범위는 특수 화폐 효과가 없는 PoE2DB 제작 규칙 모델이다.

후보가 없거나 희귀도·슬롯이 맞지 않으면 `available=false`와 이유, 빈 결과 배열을 반환한다. 잘못된 상태를 사용 불가능한 정상 상태처럼 취급하지 않는다. 유효한 버킷인지 먼저 검증한다.

## 5단계: 버킷과 동일 상태

`StateBucket`은 snapshot/base/level/rarity/implicit/condition과 정렬한 explicit 정의 ID를 가진다. `ItemState`에서 **explicit의 실제 수치만** 제거한다. tier·family·접두/접미 정보는 정의 ID로 보존한다. 따라서 서로 충돌이나 미래 후보가 다른 속성을 단순히 “엑잘티드 속성” 하나로 합치지 않는다.

현재 6종 화폐는 explicit 수치에 의존하지 않으므로 이 사영으로 다음 버킷 분포를 보존한다. 임의의 대표 수치로 실제 아이템을 만들어 검증하지 않는다. 같은 상태는 추가 순서와 관계없이 동등하며 SHA-256 ID로 노드를 공유한다. 규칙 버전은 캐시 키와 초기 응답에 별도로 포함한다.

특정 수치 이상 달성 확률, 수치 reroll, 수치에 의존하는 미래 화폐가 필요하면 수치 분포를 별도 모델링해야 한다. 현재 버킷은 그런 질의의 정답을 보장하지 않는다. 별도 가족 버킷 등 더 강한 압축은 전이 분포 보존을 증명하기 전에는 적용하지 않는다.

## 6단계: 제한된 그래프 탐색

`GraphExplorer`는 요청한 **고정 화폐 순서**를 단계별로 전개한다. 같은 단계의 같은 버킷으로 들어오는 확률을 합치고, 전체 그래프에서 노드를 공유한다. 순환이 있으므로 전역 visited로 재방문을 차단하지 않는다. 전이 edge에는 단계·화폐·조건부 확률을 기록한다. 목표 조건별 성공 확률이나 최적 화폐 순서를 자동 선택하는 planner는 이번 범위가 아니다.

API 상한은 4단계 / 노드 2,000 / edge 10,000 / 시간 1,500ms다. UI는 3단계 / 노드 2,000 / edge 10,000 / 시간 1,000ms를 요청한다. 노드·edge 한도를 초과하는 전이는 일부만 잘라서 재정규화하지 않고 그 입력 확률 전체를 `DEFERRED`로 남긴다. 시간 제한도 전이 경계에서 확인하므로 한 번의 원자적 전이 계산·응답 직렬화 시간은 추가될 수 있다. 브라우저 취소는 fetch를 중단하며, 서버의 이미 진행 중인 계산은 자체 한도로 종료한다.

- `COMPLETE`: 지정한 순서를 끝낸 확률.
- `BLOCKED`: 중간 화폐가 사용 불가능해서 멈춘 확률.
- `DEFERRED`: 자원 제한으로 계산하지 못한 확률.

세 질량 합은 부동소수점 허용 오차 내에서 1이다. `complete=true`는 미탐색 질량이 없다는 뜻이며 전부 성공했다는 뜻이 아니다. 같은 상태도 단계와 종료 사유가 다르면 별도 terminal로 표현한다. UI는 전체 합계와 상위 20개 endpoint를 구분한다.

폭넓은 순서는 마지막 단계에 도달하기 전에 한도를 모두 사용할 수 있다. 실제 기본 Normal → Transmutation → Augmentation → Regal 탐색도 2,000노드 제한에서 완료 질량이 0일 수 있다. 이 경우 수를 줄인 순서로 재조회하거나 한 결과를 선택한 뒤 그 상태에서 이어서 탐색한다. 전체 가능한 제작 경로를 사전 열거한 결과로 해석하지 않는다.

사전 계산으로 모든 상태를 생성하지 않는다. 후보 필터는 BitSet 교집합·차집합과 남은 후보 열거, 단일 전이는 최대 explicit 개수(6) × 후보 수에 비례한다. 출력 정렬은 결과 수 K에 대해 O(K log K)다. 다단계는 펼친 edge 수에 비례하는 확률 전파와 각 캐시 미스의 전이 계산이 필요하며 출력 크기를 위 한도로 제한한다.

## 7단계: API와 화면

모든 경로는 `/api/v1/crafting` 아래다. 필드는 Java record 이름과 같다. `StateBucket` JSON은 아래 형태다. 예제 implicit ID는 실제 초기 응답에서 가져와야 한다.

```text
{ snapshotId, baseItemId, itemLevel, rarity,
  implicits: [{ modifierId, values: { statId: integer } }],
  modifierIds: [definitionId, ...], conditions: [] }
```

| 요청 | 입력 | 응답 |
|---|---|---|
| GET `/initial?itemLevel=82` | 선택적 1~100 레벨 | ruleVersion, metadata, id, state, modifiers, actions |
| POST `/actions` | StateBucket | 6종 `{action, available, reason}` 배열 |
| POST `/transitions` | `{state, action}` | `{fromId, action, available, reason, outcomes:[{id,state,probability}]}` |
| POST `/explore` | `{state, plan, maxNodes, maxEdges, maxMillis}` | nodes(ID→버킷), edges, terminals와 세 확률 합계·complete |

action은 `TRANSMUTATION`, `AUGMENTATION`, `REGAL`, `EXALTED`, `ANNULMENT`, `CHAOS`다. 잘못된 JSON/enum/버킷 형식은 400 `MALFORMED_REQUEST`, 읽어들인 상태의 규칙 위반·필수 요청 필드 누락·탐색 한도 위반은 422 `INVALID_CRAFTING_REQUEST` Problem Details다. 요청별 외부 조회나 DB 쓰기는 없다.

화면에서 현재 상태·사용 가능 화폐·불가 이유와 다음 결과 확률을 표시한다. 결과는 처음 20개와 더 보기로 나누고 선택하면 다음 상태로 이동한다. 이전 상태 복귀와 선택 경로의 조건부 확률 곱을 제공한다. 확률은 소수점 최대 6자리 백분율로 표시하며 0.000001%보다 작은 양수는 지수 표기해 0%와 구분한다. 서버 계산은 double 원값을 유지한다.

수치 미결정 explicit은 원본 표시 범위로 나타낸다. 로딩/실패/재시도/취소를 제공하고 이전 요청의 늦은 응답은 현재 상태를 바꾸지 않는다. 미지원 재료 선택과 표시용 아이템의 제작 요청은 기존 아이템을 보존하면서 이유를 알린다.

## 8단계: 캐시와 성능

`TransitionCache`는 `(ruleVersion, StateBucket, action)`을 키로 쓰는 프로세스 메모리 LRU다. StateBucket 자체에 snapshot이 들어가므로 다른 스냅샷이 재사용되지 않는다. 최대 256개 키와 전체 결과 25,000개를 동시에 제한한다. 계산은 lock 밖에서 실행하며 동시 미스는 같은 계산을 중복 수행할 수 있지만 캐시 등록·축출은 동기화한다. 사용자 원문은 키나 로그에 넣지 않는다.

현재 데이터셋에서 분산 캐시의 운영 비용을 정당화할 병목이 없어 Redis/PostgreSQL에 계산 결과를 쓰지 않는다. 서버 재시작 시 캐시가 비워져도 결과 정확성은 달라지지 않는다. 테스트는 실제 209개 데이터로 6옵션 Rare Chaos를 워밍업 5회 후 20회 측정하고 캐시 hit와 미사용 계산 p95를 출력한다. 이는 이 개발 PC의 도메인 계산 시간으로, HTTP·렌더링·동시 사용자 지연 보장은 아니다.

## 9단계: 검증과 자체 리뷰

`CraftingEngineTest`는 접두/접미 가중치 합산, 레벨 경계, family와 슬롯 제외, Regal 슬롯 변경, 균등 제거, Chaos 제거 후 후보 재계산과 self-loop 합산을 검증한다. 작은 별도 fixture의 순서 있는 3단계 모든 경로를 직접 열거한 분포와 버킷 그래프 분포를 대조한다. 수치만 다른 ItemState의 동등 전이, 순환 그래프 재방문, 중단 확률 보존, LRU 상한·snapshot 분리와 실제 데이터 성능도 확인한다.

API 테스트는 번들 초기 상태, 전체 전이, 잘못된 요청·상태, 탐색 상한과 부분 결과를 검증한다. UI 테스트는 원본 표시·출처, 분포·분기 이동·복귀, 일부만 탐색한 결과, 취소 후 늦은 응답, 잘못된 분포·다른 snapshot 응답 거부, 아이템 레벨과 중앙 화폐 선택 연결을 포함한다. 기존 텍스트 입력·재료 탭·즐겨찾기 검사를 함께 실행한다.

자체 리뷰에서 장시간 열린 화면의 복귀 기록이 개별 캐시 GC로 사라질 수 있는 구조를 발견해, 관찰 중인 현재 탐색 Query에 기록을 함께 보관하도록 수정했다. 순서 탐색 실패의 재시도 누락도 보완했다. 독립 리뷰로 표현하지 않는다.

2026-09-29 최종 검증: backend에서 `./gradlew.bat check generateJooq bootJar` 성공, unit/ArchUnit/API와 격리된 PostgreSQL·Redis 통합 검사 합계 81개 통과(실패·skip 0). frontend에서 Node 24 기반 `npm ci`, `format:check`, `lint`, `typecheck`, `test -- --run`, `build` 성공, 테스트 54개 통과. 실제 데이터 Chaos 도메인 계산은 워밍업 후 20회에서 미캐시 p95 5.513ms / 캐시 p95 0.018ms였다.

기존 서버와 별도인 임시 PostgreSQL·Redis·backend·frontend에 프로덕션 빌드를 연결하고 Chrome headless로 실제 6종 화폐, 209개 초기 결과·더 보기, 상태 이동·키보드 복귀, 레벨 변경, 그래프 한도·확률 합계와 390px 모바일을 확인했다. 브라우저 런타임 오류 0. 화면 확인에서 발견한 극소 확률 0% 반올림은 지수 표기로 수정하고 회귀 테스트를 추가했다. 이 검증은 단일 브라우저 smoke이며 부하 테스트나 게임 서버 확률 검증은 아니다.

## 작업 화면과 티어 그룹

기본 Crafting Workbench와 State explorer는 헤더 아래 별도 탭이다. 탭 전환은 탐색 경로를 유지한다. Workbench에서 시작 아이템에 화폐를 미리보면 표시된 시작 아이템에서 새 경로를 시작한다. 탐색 화면은 방문 상태를 세로로 표시하고 현재 상태를 구분하며, 이전 단계로 돌아가 다른 분기를 선택할 수 있다. 각 단계는 원래 조건부 확률을 보존하고 선택 경로의 확률은 그 곱이다.

화폐 미리보기는 작업대 자산을 재사용하며 접근 가능한 이름·선택 상태·사용 불가 사유를 표시한다. 티어 결과는 단일 전이 응답 안에서 snapshot·base·level·rarity·implicit·condition과 속성 layer·affix·family ID·stat ID·tag·수치를 제외한 문구가 같을 때만 합산한다. 고정 수치는 문구의 모든 숫자가 선언된 고정 stat 값과 대응할 때만 정규화하고, 그 외 상수는 보존한다. 식별 자료가 없으면 개별 결과로 남긴다. 그룹은 원래 확률의 합을 표시하며 펼치면 티어 순서로 원래 수치·확률을 보여준다. 다음 상태로 이동할 때는 그룹 평균이 아닌 개별 결과를 선택한다. 순서 탐색의 terminal 총합과 API·엔진 의미는 유지한다.

## Sapphire 기존 아이템과 quality cap 변경

Workbench 한정 Sapphire Magic/Rare 시작 아이템은 옵션 없음 또는 source-backed Cast Speed suffix 한 개를 입력할 수 있다. Normal/Unique/특수 Jewel·전체 Jewel pool·일반 Jewel crafting은 현재 미지원이며 0P/1S는 제품 편집 제한이다. refined13은 한 번 사용 시 max20, 유형 교체, 원래 roll 보존과 중앙 HALF_UP 표시 정책을 따른다. 사용자 확인에 따라 cap modifier 제거 후 기존 품질·유형을 보존하고 cap 증가 시 자동 충전하지 않는다. 제거 후보/확률은 그대로이며 film은 before/after 및 기존 probability ledger를 보존한다. [정정 규칙](workbench-quality-preserve-2026-10-04.md). Jewel 기본 제작과 Liquid는 [다음 묶음](workbench-jewel-liquid-next-2026-10-04.md)에서 재개하며 Essence는 제외한다.
