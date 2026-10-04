# 지원 범위

현재 지원하는 기능은 제작 작업대의 재료 표시·선택·검색·즐겨찾기, 영어 아이템 복사 텍스트 분석과 Solar Amulet 확률 탐색이다.
`POST /api/v1/items/parse`는 원문·미해석 행·경고를 보존한다. catalog 검증 및 계산용 ItemState 변환은 지원하지 않는다.

계산용 모델의 첫 베이스는 Solar Amulet이다. 2026-09-29 수집한 PoE2DB Amulets의 Base Prefix 81개·Base Suffix 128개와 Spirit implicit을 번들 스냅샷으로 보존한다. 초기 아이템 레벨을 선택하고 화폐별 가능한 결과·확률을 조회하거나 결과 하나로 이동·복귀할 수 있다. UI는 최대 3단계 순서를 제한된 그래프로 탐색한다. [상세 명세](item-state.md).

일반 Normal/Magic/Rare와 Base 속성, 일반 Transmutation·Augmentation·Regal·Exalted·Annulment·Chaos의 전이·확률을 지원한다. 수치 roll을 합친 버킷이므로 특정 수치 이상을 달성할 확률은 계산하지 않는다. 특수 제작, 실제 화폐 소모, 자동 최적 경로 탐색은 지원하지 않는다. 화면의 재료 목록 전체가 실제 제작 지원 목록은 아니다.

PoE2DB 게시 가중치를 계산 기준으로 채택한다. 데이터 누락을 임의의 weight 또는 0%로 보정하지 않는다. 스냅샷에 확인되지 않은 시즌·패치 번호를 추정해 부여하지 않는다.

## Workbench refined catalyst 범위

Sapphire Magic/Rare의 제한된 기존 아이템 편집과 refined catalyst13을 지원한다. 일반 catalyst는 검증된 Ring/Amulet 범위이며 refined는 Sapphire에만 적용한다. 빈 affix와 일치하지 않는 유형의 NO_MATCH는 유효하다. 일반 Jewel crafting·특수 Jewel·CraftSupport/StateExplorer 확장은 포함하지 않는다. Solar cap40 modifier 제거 후 품질20 clamp는 game 검증 규칙이 아닌 교체 가능한 simulator 모델이다. [상세 명세와 검증](workbench-refined-catalyst-2026-10-04.md).
