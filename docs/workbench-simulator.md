최신 base 확장: [Belt distinct implicit 13종](workbench-belts-bundle-2026-10-05.md). Workbench **91 bases**. 기존78종과 Rawhide film·canonical rolls·source IDs를 보존한다. 신규 ordinary135 + special4, source-backed selection weights, six locales, Divine/Blessed·기존 currency/Essence/Omen 경로를 검증한다. Belt Catalyst와 game quality cap은 지원하지 않는다. 공통 Charm-slot source range와 Breach fixed stat을 보존하며 최종 slot 합산/확률은 미모델링이다. Support/Explorer는 Solar-only, registry220/deferred50는 유지한다. [다음 Crossbow/melee/Shields/Bucklers/Foci/Quivers 및 Staff/Talisman 후보](workbench-next-equipment-plan-2026-10-05.md)를 계속 남긴다. whole-game complete가 아니다. 아래 dated checkpoint는 과거 범위다.

Latest checkpoint: [Rattling Sceptre and four Command Essences](workbench-rattling-sceptre-2026-10-03.md), overall116 / current-scope155: implemented108, pending47, excluded65. Earlier counts describe their dated checkpoints.

Latest checkpoint: [bounded Attuned Wand and11 Essence paths](workbench-attuned-wand-2026-10-03.md), overall109 / current-scope155: implemented101, pending54, excluded65. Prior counts below describe their dated checkpoints.

Latest checkpoint: [six Bow Perfect Essences](workbench-bow-perfect-2026-10-03.md), overall101 / current-scope155: implemented93, pending62, excluded65. Earlier counts and candidate statements below describe their dated checkpoints.

Latest runtime: [Crude Bow v1 and three-base compatibility](workbench-crude-bow-2026-10-03.md); overall implemented95, current-scope155/87/68, same65 exclusions. The Artificer75 and earlier counts below are historical checkpoints.

> Current 2026-10-03 checkpoint: Solar rules v16, Stocky `stocky-workbench-artificer-v26`; registered220 / implemented75. Ordinary Stocky Artificer adds one empty socket, legacy count unknown stays unknown. [Delivery and executed checks](workbench-stocky-artificer-2026-10-03.md), [current user development scope155/67/88](workbench-development-deferrals-2026-10-03.md). Historical v1/v2 sections below are not current implementation counts.
# Solar Amulet Workbench simulator

현재 활성 규칙은 `solar-workbench-affix-v2`, registry는 `equipment-crafting-registry-v2`다. 아래의 첫 구현 단계 설명은 v1 기록이며, 현재 지원 범위는 이 절과 [전체 목록](workbench-support-v2.md)을 따른다.

## v2 현재 범위

Solar Amulet에서 커런시 17종과 Omen 8종을 지원한다. 일반 Transmutation, Augmentation, Regal, Exalted, Chaos 각각에 Greater/Perfect를 추가하고, 일반 Annulment와 Divine을 지원한다. 활성 Omen은 같은 작업에 하나만 허용한다. 상위 커런시와 일치하는 Omen의 조합은 검증되지 않아 차단한다. 관계없는 활성 Omen은 유지하고, 성공한 작업에 해당하는 Omen만 소비한다. 실패는 아이템과 활성 Omen 모두 보존한다. 규칙 차단 이유는 정상 응답의 reason으로 표시하며, 네트워크·서버·응답 검증 실패와 구분한다.

최소 modifier level은 Transmutation/Augmentation의 Greater 44, Perfect 70이고 Regal/Exalted/Chaos는 35, 50이다. 아이템 레벨이 최소값 아래면 사용할 수 없다. 후보는 requiredItemLevel로 비교하며 Tier 숫자를 사용하지 않는다. 특정 속성 그룹이 전부 제외되는 예외는 PoE2DB의 generation type와 첫 modifier family 그룹별 최고 eligible level을 유지한다. 이 Solar snapshot은 각 정의가 단일 family라 그룹이 명확하다. 다중 family 해석은 확장하지 않는다.

Divine은 implicit과 explicit의 수치만 재굴림한다. Blessed가 활성화되어 있으면 implicit만 재굴림하고 explicit ID와 수치를 보존한다. Whittling은 가장 낮은 requiredItemLevel의 explicit을 제거하며 동률은 ledger의 uniform-removal 가정을 적용한다. Erasure는 제거할 affix만 제한하고 Chaos의 새 속성은 별도의 정상 eligible pool에서 선택한다.

복사 텍스트 연결은 Solar 이름·레벨·rarity·속성·family·수치 범위가 모두 검증되는 경우에만 허용한다. 원문은 Edit item에 유지한다. 화면 범위와 raw stat 범위가 직접 일치하는 정수 수치만 역매핑하며 재생 속성처럼 변환·반올림되는 단위는 차단한다. 알 수 없는 행, 상태 플래그, 애매한 속성 후보를 제거하거나 추정하지 않는다.

v1의 두 uniform ledger 항목을 그대로 사용한다. 추가 속성 선택은 PoE2DB weight이고 수치 재굴림과 제거의 균등 가정은 서로 다른 후보 단위와 N으로 기록한다. 혼합 known/unknown weight 정책은 미정이며 효과·후보 자체가 불명확한 규칙에 1/N을 대입하지 않는다.

출처: [Currency](https://poe2db.tw/us/Currency), [Omen](https://poe2db.tw/us/Omen), [최소 modifier level](https://poe2db.tw/us/Minimum_Modifier_Level), [그룹 정의 JS](https://cdn.poe2db.tw/js/ModsView.f39fca410dd746d3.js), [GGG 0.3.0](https://www.pathofexile.com/forum/view-thread/3826682). GGG 문서는 웹 도구로 확인했으나 직접 다운로드는 403이어서 hash가 없다. 과거 삭제된 항목의 재도입과 현재 획득 가능성은 주장하지 않는다.

## Support 현재 구현과 이전 검증 기록

2026-10-02 현재 독립 Support UI와 실제 추천 API가 구현되어 있다. 서버 Solar base, catalog 검증 텍스트, 수동 modifier tier로 시작 상태를 입력하고 필수 family AND 후보 중 N개·최소 tier 목표를 설정한다. 정상 추가 12종의 고정 순서별 first-hit 질량을 합산하며 성공 상태는 즉시 흡수한다. 목표 독립 pool은 PostgreSQL과 bounded memory cache에서 재사용한다. 실제 backend 프로세스 재시작 뒤 다른 목표로 PostgreSQL hit 3개·memory hit 0개·새 계산 0개를 확인하고 확률 일치도 검증했다. 자세한 현재 동작은 [Support 계산·저장](support-transition-design.md), 검증과 안전중단은 날짜별 STOP_CHECKPOINT를 따른다.

`IncreaseSocketedGemLevel`의 Melee/Projectile/Minion/Spell은 같은 family의 대안이다. UI는 고유 tier만 선택하게 하고 어느 효과든 포함한다는 의미와 출처 예시를 표시한다. 특정 스킬 효과 필터는 미지원이다. 실제 modifier 입력의 각 대안은 보존한다.

아래 준비 단계 검사 수는 이전 단계 기록이며 현재 검사 수를 뜻하지 않는다.

검증 체크포인트: Docker Java 21에서 단위/API/아키텍처 93개와 통합 5개가 실패·skip 없이 통과했다. format check, jOOQ 생성, bootJar도 통과했다. Docker Node 24에서 프런트엔드 66개 테스트, lint, typecheck, format check와 production build를 통과했다. 실제 별도 QA 스택(18080/18081)과 Chrome에서 화면 검사 20개, API 불변식 5개, 런타임 오류 0개를 확인했다. 390px 화면의 Omen 목록은 활성 개수가 보이는 펼침 영역으로 수정하고 재검증했다. 브라우저 검사는 공개 fixture 텍스트만 사용하며 원본 프로젝트의 서비스·데이터를 사용하지 않았다.

정상 유한 추가 경로는 Transmutation, Augmentation, Regal, Exalted와 각각 Greater/Perfect: 총 12 액션이다. `AdditionRules`로 Workbench와 슬롯·rarity·family·최소 레벨·weight 후보 규칙을 공유하고 `AdditionTransitions`에서 전부 열거하는 정확 분포를 구현했다. 기존 Explorer 화면은 일반 4종 추가를 사용하며 Support는 12종을 계산 대상으로 사용한다. 일반 Exalted의 Sinistral/Dextral 단독 Omen 전이도 검증했으며 상위와의 조합은 제외한다. [전이 검증·정량 상태 수·저장 설계](support-transition-design.md)를 참고한다.

Annulment와 세 Chaos는 복구 분류다. 제거·대체와 해당 Annulment/Erasure/Whittling Omen은 새 검증 상태로 점프하며 정상 추가 경로 성공 확률에 곱하지 않는다. Divine/Blessed는 수치 재굴림 분류로 최초 Support의 속성 ID 목표에서는 제외한다. Alchemy, Essence, 특수 규칙은 현재 계산 대상이 아니다.

승인된 목표는 필수 family 조건 AND 후보 중 서로 다른 N개, 하나의 목표이며 각 조건에 최소 티어를 적용한다. 실제 수치 하한은 다음 단계로 미룬다. 같은 family의 여러 tier는 중복 카운트하지 않는다. 카탈로그에서 확인한 방향에 따라 UI는 “T2 or better (T1–T2)”처럼 표시한다. 고정 화폐 순서별 최초 달성 상태를 흡수해 즉시 종료하고 모든 최초 성공 결과의 확률을 합산한다. 상위 3~5개 순서와 중간 경로를 비교한다. 정상 추가는 매 단계 explicit 개수가 증가하여 최대 6회 안에 자연 종료한다. 탐색 예산으로 계산이 중단되면 미해결 확률 질량과 커버리지를 보여주고 0확률로 오인시키지 않는다. 재료 가격·비용은 모델에 없으므로 추천 기준은 성공 확률이며 비용 최적화를 주장하지 않는다.

2026-10-01 · 첫 구현 단계 · Support 목표 추천은 후속 단계

Workbench는 현재 아이템에 일반 화폐 6종을 실제 적용하고 같은 탭에 결과를 표시한다. State explorer는 별도 상태를 유지하는 조건부 확률 탐색기이며 Workbench 적용으로 초기화하지 않는다. 붙여넣은 아이템은 catalog 매핑이 검증되기 전까지 표시용이다.

## Registry와 ledger

`backend/src/main/resources/crafting/registry-v1.json`은 `equipment-crafting-registry-v1`이다. 기존 표시 inventory 201개와 장비 제작 index의 추가 후보 19개를 등록한다. 전체 장비 제작 수단의 수집 완료를 주장하지 않는다 (`inventoryComplete: false`). 추가 후보의 canonical ID·현재 획득 가능 여부는 미검증이다. 지도·스킬 젬 제작은 구현 범위에서 제외한다.

등록 상태, 효과 구현 상태, Solar 검증 상태를 각각 보존한다. 6개 항목만 실제 구현하고 검증한다. 나머지는 효과·후보·베이스 적격성·확률을 확인하기 전 실행하지 않는다. 표시 툴팁은 실행 규칙으로 간주하지 않는다. 기존 툴팁 수집 시각과 source hash를 보존하며 새로운 수집으로 표시하지 않는다.

속성 선택은 기존 Solar snapshot의 PoE2DB 게시 weight를 그대로 사용한다. 가중치가 일부만 알려진 후보를 균등 분포와 섞는 정책은 보류한다. 효과와 적격 후보 자체가 불명확하면 1/N으로 대체하지 않는다.

`solar-uniform-assumptions-v1` ledger의 두 가정:

- `uniform-removal-v1`: 현재 적격 explicit 인스턴스가 N개일 때 각각 1/N. 제거 후보 ID와 N, 해당 화폐 출처, 가정 이유를 응답에 기록한다. 게임 서버의 공식 제거 확률이라는 주장이 아니다.
- `uniform-integer-roll-v1`: 단일 stat의 원본 정수 구간 [min,max] 안의 각 정수가 후보이며 N = max - min + 1, 각각 1/N. 해당 stat 단위·구간·N·속성 상세 출처·가정 이유를 응답에 기록한다. 현재 snapshot 210개 정의 모두 단일 stat이며, 복수 stat의 joint domain은 임의로 생성하지 않고 거부한다. 고정값은 별도 확률 가정이 필요 없다.

결과는 rule/ledger/snapshot 버전과 실제 이벤트 및 적용한 가정을 반환한다. UI는 마지막 제작의 가정과 출처를 표시한다. 숫자 원본 단위가 표시 문구의 단위와 일치하지 않으면 원본 stat 이름·실제 값과 `source units`를 표시하며 단위를 추측하지 않는다.

## 실제 적용

- `GET /api/v1/crafting/workbench/registry`: versioned inventory와 ledger.
- `POST /api/v1/crafting/workbench/apply`: `{ state: ItemState, action }` → 구체적인 ItemState, 적용 여부·불가 이유, ADD/REMOVE 이벤트, 선택 확률과 가정.
- Normal → Transmutation, Magic → Augmentation/Regal, Rare → Exalted/Chaos, Magic/Rare → Annulment. 기존 슬롯·family·레벨·snapshot 검증을 재사용한다.
- 변경하지 않은 속성의 수치는 보존한다. Chaos는 제거 이벤트를 먼저 샘플링한 후 가중치로 새 속성을 고르고 수치를 새로 굴린다. 같은 속성 ID가 다시 붙어 버킷이 같아져도 기존 수치를 재사용하지 않는다.
- 적용 불가·통신 오류·잘못된 응답이면 현재 아이템을 유지한다. 중복 클릭은 요청 중 차단하고, 베이스 교체나 페이지 이탈 시 요청을 취소하여 늦은 응답을 폐기한다.
- 수량·가격·비용·재료 inventory는 아직 모델링하지 않는다. 제작 이벤트와 아이템 변화만 시뮬레이션한다. 랜덤 seed는 공개 API 입력으로 받지 않는다.

## 현재 남은 단계

현재 17종/8종과 정상 추가 12종 밖의 제작 수단은 registry 상태·효과·적격 후보·출처를 확인한 뒤 확장한다. Support의 Annulment/Chaos 실행·복구 분기 선택은 남아 있으며 정상 추가 경로 확률에 복구 확률을 합치지 않는다. 특정 스킬 효과 목표, 수치 하한 목표, 다른 장비 확장도 미지원이다. 큰 Normal-root 결과는 예산 내 partial일 수 있으며 현재 continuation token이나 별도 goal-result cache는 없다. 재료 가격·다른 가치 아이템 추천·가격 예측은 범위 밖이다.
