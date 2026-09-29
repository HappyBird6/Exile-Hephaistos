# 지원 범위

현재 지원하는 기능은 제작 작업대의 재료 표시·선택·검색·즐겨찾기와 영어 아이템 복사 텍스트 분석이다.
`POST /api/v1/items/parse`는 원문·미해석 행·경고를 보존한다. catalog 검증 및 계산용 ItemState 변환은 지원하지 않는다.

계산용 모델의 첫 베이스는 Solar Amulet이다. 2026-09-29 수집한 PoE2DB Amulets의 Base Prefix 81개·Base Suffix 128개와 Spirit implicit을 번들 스냅샷으로 보존한다. 불변 ItemState, 초기 상태 생성과 상태 검증은 Java 코드에서 사용할 수 있으며, API와 화면은 아직 연결하지 않았다. [상세 명세](item-state.md).

이번 데이터·검증 범위는 일반 Normal/Magic/Rare와 Base 속성이다. 특수 제작, 상태 전이 handler, 확률 계산과 실제 화폐 소모는 아직 지원하지 않는다. 다음 화폐 검증 범위는 일반 Transmutation·Augmentation·Regal·Exalted·Annulment·Chaos이며, 화면의 재료 목록은 실제 제작 지원 목록이 아니다.

PoE2DB 게시 가중치를 계산 기준으로 채택한다. 데이터 누락을 임의의 weight 또는 0%로 보정하지 않는다. 스냅샷에 확인되지 않은 시즌·패치 번호를 추정해 부여하지 않는다.
