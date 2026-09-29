# Solar Amulet ItemState

2026-09-29 · 개발 계획 1~3단계 구현 범위

## 범위

첫 베이스는 Solar Amulet (`Metadata/Items/Amulets/FourAmulet9`)이다. Normal, Magic, Rare와 PoE2DB Amulets의 **Base Prefix / Base Suffix**만 다룬다. 이 문서의 데이터는 조회 시점의 스냅샷이며, 확인하지 않은 게임 패치 번호를 부여하지 않는다.

다음 단계의 검증 대상 화폐는 일반 Transmutation, Augmentation, Regal, Exalted, Annulment, Chaos다. 이번 변경은 이 화폐들의 후보 추첨·확률·상태 전이를 구현하지 않는다. Greater/Perfect 화폐, Essence, Omen, 타락·성역화·분열·제작 속성 등 특수 제작은 제외한다. 화면의 전체 재료 목록은 지원 화폐 목록이 아니다.

API·UI 연결과 기존 텍스트 파서의 catalog 식별은 후속 단계다. 기존 `/api/v1/items/parse` 응답은 계속 표시용 데이터이며, 파싱 성공을 유효한 ItemState로 간주하지 않는다. 기존 태양의 목걸이 카드와 실행 중 서버도 이 단계에서 변경하지 않는다.

## 시작 상태

`ItemCatalogLoader.loadDefault()`로 번들 스냅샷을 읽고 `SolarAmulet.initial(catalog)`로 시작 상태를 생성한다.

- 베이스: Solar Amulet
- 아이템 레벨: 82 (초기 프리셋 선택; 게임의 필수 레벨이 아님)
- 희귀도: Normal
- implicit: Spirit 15 (10~15 범위 안의 고정 프리셋 값; 무작위 추첨이 아님)
- explicit: 없음
- 특수 상태: 없음

`SolarAmulet.initial(catalog, itemLevel, spirit)`로 레벨과 implicit 값을 지정할 수 있다. 제품 입력 연결은 API·화면 단계에서 한다. 수치가 명확한 실제 아이템만 `ModifierInstance`로 표현한다. 미래의 미결정 수치 범위는 후속 버킷 모델에서 다루며, 임의의 수치를 실제 roll로 저장하지 않는다.

## 데이터 스냅샷

위치: `backend/src/main/resources/catalog/solar-amulet/`

| 파일 | 내용 |
|---|---|
| `base.raw.json` | 페이지의 `normal`(Base) 배열 209행을 UTF-8/LF JSON으로 보존 |
| `details.raw.json` | 각 상세 페이지의 출처·이름·family·레벨·원본 Stats 구간과 Solar implicit |
| `catalog.json` | 계산 모델용 정의와 베이스, 수집 시각·출처·검증용 SHA-256·행 수·가중치 합계 |

접두 81행, 접미 128행이며 모든 레벨을 포함한 가중치 합계는 각각 72,200 / 96,656이다. 이 합계는 수집 검증값이고 특정 아이템의 확률 분모가 아니다. 실제 분모는 후속 후보 선택 단계에서 레벨·슬롯·충돌을 적용한 뒤 구한다.

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
8. `validate`는 기존 아이템의 출현 레벨 조건을 강요하지 않는다. `validateForGeneration`은 **전체 explicit을 새로 생성한 상태**의 레벨 적합성을 추가 검사한다. 추후 기존 아이템에 한 속성을 붙일 때에는 새 속성만 레벨 검사하도록 후보 선택기를 구현한다.

`slots`는 검증된 상태에서 사용한다. 슬롯 여유만으로 어떤 화폐가 사용 가능한지 판단할 수 없다. unknown modifier가 있는 상태는 검증 오류부터 처리해야 한다.

## 수집 방식과 후속 크롤러

이번에는 공개 페이지와 각 속성 상세를 한 번 수집·검토했다. 앱 런타임 네트워크 접근, 주기적 크롤러, Python 서비스는 추가하지 않았다.

후속 수집기는 동일한 세 파일을 생성하면 된다. 갱신할 때 행 수·가중치·tier·충돌 그룹·stat 변경을 비교하고, 검증을 통과한 새 snapshot ID로 교체한다. 실행 중 카탈로그를 부분 수정하거나 같은 ID에 다른 데이터를 덮어쓰지 않는다. 기존 ItemState는 생성 당시의 snapshot과 함께 해석한다.

PostgreSQL 저장, Redis 캐시, 버킷 병합, 상태 그래프는 아직 연결하지 않았다. 추후 카탈로그 공급 경로가 바뀌어도 순수 모델은 재사용한다.

## 출처와 표시

- [PoE2DB Amulets Base modifier 표](https://poe2db.tw/us/Amulets#ModifiersCalc)
- [PoE2DB Solar Amulet](https://poe2db.tw/us/Solar_Amulet)
- [PoE2DB 가중치 수집 설명](https://poe2db.tw/us/weightings)
- [PoE2DB tier 표시 구현](https://cdn.poe2db.tw/js/ModsView.f39fca410dd746d3.js)
- [PoE2DB 제작 규칙](https://poe2db.tw/Crafting)

향후 확률 UI에는 `Probabilities are calculated using PoE2DB modifier weights.`와 출처 링크·데이터 갱신일을 표시한다. 현재 단계에는 확률 UI 자체가 없다.

게임 데이터의 권리는 Grinding Gear Games에 있다. PoE2DB 출처와 개별 상세 링크를 보존한다. PoE2DB의 wiki 설명은 별도 표시가 없으면 [CC BY-NC-SA 3.0](https://creativecommons.org/licenses/by-nc-sa/3.0/)로 안내되어 있다. 이 스냅샷을 자체 제작한 원본 게임 데이터로 표시하지 않는다.

## 검증 기준

전체 209행을 보존 원본과 비교해 이름·방향·레벨·weight·family·tag·tier를 대조하고, 상세 210건의 stat 범위를 비교한다. raw checksum과 누락 필드도 검사한다. 상태 테스트는 빈 희귀도, 슬롯 한도, 충돌, implicit 공존, 생성 레벨 경계, 값 범위, 미지원 입력, 불변성·순서 동등성, 다중 family와 hybrid fixture를 포함한다. ArchUnit은 새 모델의 도메인 의존성을 검사한다.
