최신 base 확장: [Amulet 7종](workbench-amulets-bundle-2026-10-04.md). Workbench 61 bases; jewelry는 distinct implicit sidegrade 선택 범위이며 전체 catalog 완료가 아니다. 신규 Amulet7의 전체 ordinary209·source-valid special8·Catalyst13·6locale·기존54개 film을 보존한다. Support/Explorer는 Solar-only, active170/deferred50은 유지한다. Wand/Sceptre skill family·Belt 및 다른 equipment category는 후속 source 검증 범위다.


> 최신 완료 집계 (2026-10-04): registry220 = active170(implemented170 = default163 + opt-in legacy7, pending0) + deferred50(보존 구현8 + 미구현42). Catalyst26는 검증된 제한 base만 IMPLEMENTED로 정리했다. 전체 구현178은 deferred8을 포함하므로 현재 사용 가능 수가 아니다. 아래의 이전 집계는 checkpoint 이력이다. [정의·base 제한·검증](workbench-catalyst-registry-2026-10-04.md).

Latest checkpoint: [Ancient Liquid and Time-Lost Jewel](workbench-ancient-liquid-2026-10-04.md). Time-Lost Ruby53/Emerald77/Sapphire60/Diamond160 ordinary candidates, Ancient Liquid13/13/13/3 with Crafted outcomes14/14/14/4. All26 craft Liquids and reviewed18 ordinary currencies on eight Jewel bases have sourced positive paths. Fixed base radius implicit is preserved; passive-tree effects are conditional text only. Earlier dated scopes below are superseded for these bases.

Previous checkpoint: [Basic Jewel four bases and Potent Liquid](workbench-basic-jewel-potent-2026-10-04.md). Ruby50/Emerald74/Sapphire58/Diamond160 ordinary candidates, base-specific Liquid13/13/13/3; provisional Ferocity projection and Contempt overflow preservation.

# 지원 범위

현재 지원하는 기능은 제작 작업대의 재료 표시·선택·검색·즐겨찾기, 영어 아이템 복사 텍스트 분석과 Solar Amulet 확률 탐색이다.
`POST /api/v1/items/parse`는 원문·미해석 행·경고를 보존한다. catalog 검증 및 계산용 ItemState 변환은 지원하지 않는다.

계산용 모델의 첫 베이스는 Solar Amulet이다. 2026-09-29 수집한 PoE2DB Amulets의 Base Prefix 81개·Base Suffix 128개와 Spirit implicit을 번들 스냅샷으로 보존한다. 초기 아이템 레벨을 선택하고 화폐별 가능한 결과·확률을 조회하거나 결과 하나로 이동·복귀할 수 있다. UI는 최대 3단계 순서를 제한된 그래프로 탐색한다. [상세 명세](item-state.md).

일반 Normal/Magic/Rare와 Base 속성, 일반 Transmutation·Augmentation·Regal·Exalted·Annulment·Chaos의 전이·확률을 지원한다. 수치 roll을 합친 버킷이므로 특정 수치 이상을 달성할 확률은 계산하지 않는다. 특수 제작, 실제 화폐 소모, 자동 최적 경로 탐색은 지원하지 않는다. 화면의 재료 목록 전체가 실제 제작 지원 목록은 아니다.

PoE2DB 게시 가중치를 계산 기준으로 채택한다. 데이터 누락을 임의의 weight 또는 0%로 보정하지 않는다. 스냅샷에 확인되지 않은 시즌·패치 번호를 추정해 부여하지 않는다.

## Workbench refined catalyst 범위

Sapphire Magic/Rare의 제한된 기존 아이템 편집과 refined catalyst13을 지원한다. 일반 catalyst는 검증된 Ring/Amulet 범위이며 refined는 Sapphire에만 적용한다. 빈 affix와 일치하지 않는 유형의 NO_MATCH는 유효하다. 일반 Jewel crafting·특수 Jewel·CraftSupport/StateExplorer 확장은 현재 포함하지 않는다. 사용자 정정으로 Solar cap40 modifier 제거 후 기존 품질40을 보존하며 입력은 확인된 도달 범위로 검증한다. Catalyst 재사용·전환은 max(existing,currentCap)의 simulator 편의 정책이다. [정정 명세와 검증](workbench-quality-preserve-2026-10-04.md). Jewel 기본 제작과 Liquid는 사용자 요청에 따라 [다음 묶음](workbench-jewel-liquid-next-2026-10-04.md)에서 재개하며 Essence는 Jewel에 적용하지 않는다.


## Sapphire 일반 생성 및 Basic Liquid 범위 갱신

2026-10-04: 위 existing-suffix 편집 제한은 Sapphire Workbench에서 superseded. normal58 후보와 Magic1P/1S·Rare2P/2S, 기본 화폐18종·Basic Liquid10을 지원한다. weight는 사용자 승인 균등 simulator 모델이며 실제 game weight가 아니다. Crafted는 item당 하나이고 ordinary 생성에서 제외하며 기존 film ID/roll을 보존한다. [상세 구현·출처·가역 정책·잔여 범위](workbench-sapphire-generation-liquid-2026-10-04.md). CraftSupport/StateExplorer·다른 Jewel bases·Potent·Ancient는 이 갱신에 포함하지 않는다.
