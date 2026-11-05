# 기능 연결과 유지보수

2026-11-04 · 통합 기준 `be48f2eb6e71f3d9f018df9d4b641119bb007ba5`.

완료된 작업의 담당자별 통합 안내·중간 계획·검증 보고서에서 유지보수에 필요한 내용을 모았다. 아래는 현재 연결과 보존해야 할 계약이다. 과거 작업 브랜치의 패치를 다시 적용하라는 지시가 아니다. 이번 정리는 문서 변경이며 앱의 동작·규칙·테스트 결과를 새로 확정하지 않는다.

## 기준 문서

- 실행과 검사: [프로젝트 README](../README.md), [개발 지침](../AGENTS.md).
- 사용자 정책과 대체 관계: [사용자 제작 규칙](current/user-crafting-rules.md). 게임 원문 사실과 사용자 simulator 정책을 구별한다.
- 규칙 데이터·identity·봉인·mirror 변경: [Catalog 유지보수](catalog-data-maintenance.md).
- 상태·계산·목표: [ItemState](current/item-state.md), [자동 탐색](current/automatic-planning.md), [목표](current/goals.md).
- 별도 API·저장 wire는 [문서 지도](README.md)의 해당 계약을 따른다. 구형 엔진이 연결되어 있으므로 계약을 현행 자동 탐색과 임의로 합치지 않는다.

<a id="base-selection"></a>

## 베이스 선택과 텍스트 입력

### 등록과 신규 선택

통합 기준의 `catalog/top-bases.json`은 162개, `base-policies.json`의 legacy는 17개로 등록 identity는 총 179개다. 등록·새 작업대 선택·특정 화폐 자격·자동 탐색 지원은 서로 다른 집합이다. 11/02 감사 당시 새 선택은 171개였으며 현재 선택은 `BaseSelectionPolicy`와 Frontend `baseSelectionPolicy`로 결정한다. 과거 문서의 134개를 현행 전체 수치로 사용하지 않는다.

신규 45개 identity와 보류 후보의 출처·용량·희귀도 정책은 [베이스 감사](evidence/0.5.5/catalog/eligibility/base-selection-audit.md)에 보존한다. 서로 다른 implicit·granted skill·무기 동작·능력치 계열을 이름이나 레벨만으로 합치지 않는다. 동일 능력치 계열에서 대체된 낮은 베이스·Gnawed 화폐 등 신규 선택 제외는 과거 기록 삭제가 아니다. 제외된 identity를 읽거나 내보내는 경로는 유지한다.

`WorkbenchBasePicker`는 부모가 `open/onOpenChange`, `value/onChange`, `level/onLevelChange`, `onPlace`를 제어한다. `baseStartingRarity`를 preview fallback과 초기 상태에 일관되게 사용한다. Rare 전용 베이스를 Normal로 임시 표시하지 않는다. 주얼 등 실제 지원되는 희귀도 선택만 제공한다.

`LeftShelfOverlay`는 body portal의 nonmodal dialog다. 실제 anchor와 화폐 shelf의 rect로 위치를 정하며 바깥 클릭·닫기·Escape 후 트리거로 focus를 돌린다. 툴팁이 열려 있으면 첫 Escape는 툴팁만 닫는다. 베이스 메뉴는 왼쪽 필터·검색·레벨·Place/error, 오른쪽 한 열 옵션 구조이며 실제 목록이 내부 스크롤한다. 필터의 시각적 라벨을 생략해도 여섯 locale의 접근성 이름을 유지한다.

이미지는 `baseImageSources.json`의 정확한 identity·출처·SHA와 연결한다. [이미지 고지](../frontend/src/features/crafting/base-picker/NOTICE.md)를 유지한다. 원문 JSON/HTML을 탐색하는 `collectBaseImageSources.mjs`는 수집 도구이며 일반 화면 실행 경로가 아니다. 실패한 외부 이미지를 임의 아이콘이나 다른 identity로 대체하지 않는다.

### 파싱과 관측 상태

표시 파서와 계산 상태 매핑은 별도다. [파싱 명세](current/text-parsing.md)와 [OpenAPI](openapi-item.yaml)를 따른다. 정확한 등록 이름과 item class가 유일하게 일치할 때 해당 catalog로 매핑하며 Shrine Sceptre처럼 모호한 입력은 임의로 선택하지 않는다.

- advanced metadata 아래 여러 줄이 하나의 modifier이면 하나의 옵션으로 유지한다. 단일/복합 stat과 수치 범위는 유일한 catalog 대응이 검증될 때만 바인딩한다.
- 원문 roll·단위·분열·원문·미해석 행을 보존한다. 출처 없는 변환·stat 순서·확률을 추측하지 않는다.
- `ItemObservations`의 소켓 수·장착 Augment 원문/metadata·검토된 granted skill과 명시 level은 관측값이다. 누락된 count/level을 0이나 추정 진행표로 바꾸지 않는다.
- 관측 socket과 `augmentSockets`의 실행 용량은 다르다. 붙여넣기 성공이 화폐 적용 자격을 확장하지 않는다. Augment의 해석되지 않은 효과를 능력치로 추측하지 않는다.
- 관측한 방어 합계를 기본 방어 수치나 local flat으로 다시 사용하지 않는다. 원문 보존과 계산 표시를 분리한다.

관련 구현은 `WorkbenchService`, `SolarTextMapper`, `ItemStateValidator`, `CraftStart`, `baseRegistry`와 `workbenchHistory`다. catalog 새로고침은 현재 아이템을 검증하고 수치·분열·원문을 보존하며 identity만 갱신하는 경로와, 과거 규칙 기록의 읽기 전용 경로를 구별한다. 사라진 베이스를 Solar로 대체하지 않고 늦은 응답은 revision으로 폐기한다. 새로고침 자체가 제작을 시작해서는 안 된다.

<a id="layout"></a>

## 작업대 배치·창고·피드백

헤더는 Workbench와 Craft Support 두 작업 공간을 제공한다. State Explorer 전용 UI는 제거했으며 공유 Backend 전이·계산 엔진은 유지한다. 보관함 검색은 기존 이름·ID와 실제 표시 툴팁 설명(현지화 또는 영어 fallback)을 함께 검색한다. Essence의 특수 재료 4개는 같은 4행 스크롤 그리드의 첫 행이다.

`CraftingPage`가 창고·중앙 카드·베이스/세션 선택·기보·피드백과 요청 수명주기를 연결한다. 원래 상단 935×550 좌표계를 유지하며 75% zoom과 실제 표시 크기를 혼동하지 않는다. 기존 세 화폐 등급 열·다섯 계열 행과 중앙 카드의 원래 좌표를 다시 배치하는 초기 compact override는 폐기된 설계다.

- 기존 category selector 하나가 오른쪽 창고 내용을 정한다. 별도의 오른쪽 tab selector를 중복 생성하지 않는다.
- 즐겨찾기는 전체 제작 면 아래 두 행×다섯 칸이다. 동작·선택·과거 ID 보존을 유지한다.
- 메뉴 anchor는 실제 버튼 위치를 기준으로 한다. 빈 예약 영역 때문에 메뉴 위치나 공간이 바뀌지 않게 한다.
- 오른쪽 카드 아래 세션 기보가 열의 남은 높이를 채운다. 피드백은 4행 높이의 재료 보관함 아래 여백에 표시하며 오류와 재료 설명은 각각 스크롤한다. 전체 zoom에서는 화면상 크기가 다르다.
- 창고는 항목이 많아져도 grid의 내용 최소 크기로 무한히 늘어나지 않아야 한다. 제한된 실제 scroll 요소와 `min-height`/grid 행을 함께 확인한다. `wb-scrollbar`는 stash resources·베이스 목록·History/Favorites의 실제 스크롤 영역에 적용한다.
- 화폐 적용·활성 징조·부족/실패 설명과 tooltip은 기존 provider를 재사용한다. 내부 reason/버전/개발 진단을 제품 설명으로 직접 노출하지 않는다.
- 원본 stash 배경과 룬 이미지를 변경하지 않는다. 창 크기 대응·zoom·padding이 원본 이미지의 장식이나 실제 클릭 영역을 잘라내지 않는지 따로 확인한다.

레이아웃 구현은 `stash/`, `base-picker/`, `compact-viewport/`, `feedback-tooltip/`, `item-card.css`와 `crafting.css`에 있다. [피드백/레이아웃 상세](../frontend/src/features/crafting/feedback-tooltip/README.md)의 현행 계약도 따른다. jsdom의 선언·DOM 검사는 실제 픽셀 clipping/scroll/overflow 검증과 구별한다.

<a id="history"></a>

## Film·세션 라이브러리·기보

브라우저 저장 key `hephaistos.workbench.films.v1`과 Film version 1을 유지한다. 이름·즐겨찾기·삭제/복구 같은 metadata 변경이 frames·원문·ruleset identity·craft evidence·미지 필드를 재작성해서는 안 된다. 실패·손상·quota 오류에서 기존 bytes를 보존한다.

`SessionLibrary`는 부모 제어형 `open/onOpenChange`, `history`, `onSelect`, `onRename`, `onFavoriteChange`, `onDelete`를 사용한다. 부모가 draft·실제 cursor·catalog 검증·저장을 함께 갱신한다. 삭제 복구와 선택을 여러 번의 독립 저장으로 처리해 중간 상태가 남지 않도록 한다. 과거 지점의 새 제작은 새 Film으로 분기하고 원래 미래를 보존한다.

```tsx
<SessionNotation
  film={film}
  cursor={filmState.history.cursor}
  resources={stashResources}
  onNavigate={onNavigate}
  disabled={busy}
/>
```

`resources`에는 필터링 전 전체 표시 registry를 전달해 옛 화폐·징조의 ID도 표시한다. 기보는 읽기 전용 projection이며 storage를 수정하지 않는다.

- cursor 뒤까지 포함한 활성 Film의 전체 성공 기록을 유지하고 현재 강조만 이동한다. 초기 상태에는 현재 화폐 행이 없으며 비화폐 관측 단계에서는 이전 성공 행을 기준으로 표시한다.
- `recordCraft`의 evidence 또는 `recordAbyss`의 manual action이 Frame action과 일치할 때만 화폐로 취급한다. 루트·IMPORT·BASE_SELECTION·단순 관측은 화폐 사용이 아니다. 불충분하거나 상충하는 기록을 추측해 복원하지 않는다.
- consumedOmens의 명시 `[]`와 누락을 구별한다. 활성/남은 징조에서 과거 소비를 추정하지 않으며 확인된 중복 소비 ID도 개수대로 표시한다.
- 기보는 두 열과 원래 순서·단계 이동·접근성 이름을 유지한다. 등록된 material ID에만 기존 tooltip을 연결하며 이미지 실패 시 중립 표시와 접근성 이름을 남긴다.
- 반복 Chaos는 하나의 Frame·한 행에 실제 `uses`를 표시한다. `uses: 1`도 명시 반복이다. 인접 단일 사용을 합치거나 N개 Frame으로 펼치지 않는다. 확장이 손상됐으면 단일 1회로 조용히 바꾸지 않는다.

읽기 전용 구형 자동 작업과 새 요청을 구별한다. `AutomaticSearchService.original`의 원본 요청 export는 누락/null·원래 필드·순서를 보존한다. 현행 실행용 타입으로 읽었다는 이유로 과거 JSON에 새 null 필드를 넣지 않는다. 오래된 요청·소유자·같은 clientRequestId의 재시도 처리는 저장 계약과 함께 검사한다. 호환되지 않는 작업의 404/409를 목표 불가능이나 입력 삭제로 처리하지 말고 원본을 보존한 명시적 재계산으로 연결한다.

<a id="abyss"></a>

## 훼손·뼈·드러내기와 연속 제작

### 공통 상태와 수동 흐름

`ItemState.desecrated`는 실제 접두/접미 슬롯과 identity를 갖는다. 공개된 modifier와 분열은 실제 수치·충돌군을 유지한다. 미공개 슬롯에도 용량과 제거 대상의 의미는 있지만 가짜 stat·modifier 값·후보 분포를 만들지 않는다. 같은 슬롯에 분열과 훼손을 중복 지정하지 않는다.

초기 별도 심연 패널·NodeView 후처리·세 후보 입력 의무는 후속 구현으로 대체됐다. 현행 수동 작업대는 적용 가능한 후보를 직접 선택할 수 있다. 사용자 직접 선택이 실제 게임의 확정 획득 확률이라는 뜻은 아니다. 후보 pool·레벨·stat·슬롯·분열·family 검증은 서버에서 다시 수행한다. 최종 사용자 정책은 [제작 규칙](current/user-crafting-rules.md)을 우선한다.

장비의 Rib/Jawbone/Collarbone, 주얼의 Preserved Cranium, 장신구의 Altered Collarbone은 검토된 class·레벨·pool에서만 적용한다. 신규 UI는 Gnawed Rib/Jawbone/Collarbone을 제외하지만 과거 기록을 제거하지 않는다. Cranium은 기존 일반/Time-Lost 주얼 8종의 가변 용량과 overflow를 존중한다. Unique 주얼의 반복/파괴 규칙을 일반 주얼에 옮기지 않는다. Altered의 Otherworldly 후보도 봉인된 실제 정의와 revealPool로 검증한다.

`DesecrationCurrency.apply(..., RandomGenerator)`는 side/removal 생략과 기존 관측 입력을 모두 처리한다. 현재 simulator 정책은 전체 검증을 통과하는 제거 대상에서 균등 선택한 뒤 그 대상에서 가능한 side를 균등 선택한다. 빈 용량이 있으면 제거하지 않고, Necromancy가 있으면 해당 side만 허용한다. 반대 징조 동시 활성·분열 제거·잘못된 class/level/pool·슬롯 충돌은 거부한다. 이 분포는 검증된 게임 확률이라고 주장하지 않는다.

뼈 사용은 신규 슬롯의 `modifier: null`, `offer: null`을 만든다. reveal 후보를 자동 샘플링하지 않는다. Putrefaction의 관측 슬롯 수는 별도 계약이다. 원본 아이콘이 없는 화폐의 출처/확보 상태는 `frontend/public/assets/abyss-currency/sources.json`으로 확인하며 임의 그림이나 거부된 URL로 대체하지 않는다. [룬 원본 고지](../frontend/public/assets/abyss-runes/SOURCE.md)를 유지한다.

### 일반 화폐와 소비

`AbyssCurrencyExecutor`는 state/action/activeOmens를 받아 state/action/applied/reason/consumedOmens/remainingOmens를 반환한다. 별도 simulator를 생성하지 않고 Service의 기존 base별 simulator와 reviewed override를 사용한다.

- 숨은 슬롯의 Exalted·Annulment·검토된 replacement essence/alloy는 기존 slot-aware plan/apply를 사용한다. Chaos 제거 후보에도 숨은 슬롯이 포함되며 기존 조건부 addition pool·minimum level과 concrete roll sampler를 재사용한다.
- 공개된 상태의 일반 화폐와 허용된 Divine/Artificer/catalyst passthrough는 기존 concrete apply를 사용하고 slot·quality·socket·관측·축성 metadata를 보존한다.
- 미공개 modifier level이 필요한 Whittling이나 검토되지 않은 조합은 해당 작업을 거부한다. 하나의 미지원 조합을 이유로 모든 일반 화폐를 금지하지 않는다. 처리 중 offer가 있는 슬롯 변경에는 기존 선택 완료 조건을 적용한다.
- 실패·취소·예외·늦은 응답은 원래 item·Film·활성 징조를 유지한다. 성공한 응답을 검증한 뒤에만 실제 소비 ID를 기록하고 remainingOmens를 반영한다. 뼈용 Necromancy/Lich/Putrefaction과 다른 trigger의 징조를 혼동해 소비하지 않는다.

### 자동 탐색 경계

공통 훼손 작업은 기존 `choices → Method → ready → Node` 실행 경로에 들어간다. 구체 상태에서 생성하는 조건부 전이와 압축 혼합 상태·사용자 관측을 관통하는 전체 전략 최적화를 구별한다. 미공개 슬롯 생성의 100%를 드러내기 후보 확률로 해석하지 않는다. p%(~N%)는 후속 modifier 선택의 기본/비목표 차단 조건부 비교이며 전체 성공률·전체 기대비용이 아니다.

분열은 슬롯과 배타 그룹을 차지하지만 제거·변경 대상에서는 제외된다. 자동 Chaos의 첫 단계 조건은 분열을 제외한 변경 가능한 옵션 개수를 기준으로 하며 후반 상태가 옵션 감소 후 첫 단계로 재진입해서는 안 된다. Exalted는 첫 Chaos 준비 또는 적법한 후속 제거·추가 작업의 더미에 한정한다. 관련 정책·중간 경로·희귀도·6옵션의 정확한 정의는 사용자 규칙과 자동 탐색 명세를 따른다.

<a id="jewel-chaos"></a>

## 주얼의 Chaos overflow 보호

2026-11-04 사용자 규칙이다. +1 용량 속성이 사라져 현재 한도를 초과한 side는 Chaos 교체 후보에서 제외한다. 접두 3/2·접미 2/2이면 접두의 원본 옵션과 수치를 보존하고 접미만 교체한다. 반대도 동일하며 +1 속성이 남아 정상 3/3인 side는 보호하지 않는다.

일반/Greater/Perfect Chaos와 반복 실행 모두 같은 규칙을 사용한다. 미공개 슬롯도 점유 수에 포함하며 ID·표식·pool·minimum level을 보존한다. 후보는 overflow 보호 → side 징조 → Whittling 최저 레벨 순으로 좁힌다. 후보가 없으면 적용/징조 소비를 하지 않는다. Annulment에는 이 보호가 없으며 실제 초과 옵션을 제거해도 영구 +1 확장을 남기지 않는다.

추가 옵션은 현재 용량·충돌군·레벨·기존 weight를 따른다. 제거의 균등 선택은 기존 simulator 모델이다. Frontend의 단일·심연·반복 응답 검증도 보호 옵션/슬롯 변경을 거절한다. 저장 형식과 이전 기록의 버전 문자열은 재작성하지 않는다. 현재 주얼 rule ID는 `basic-jewel-potent-v5-five-affix-chaos-overflow`, Time-Lost는 `time-lost-ancient-v3-five-affix-chaos-overflow`다.

<a id="quality"></a>

## 수치·품질·Divine·축성

### 원본과 표시

숫자 표시의 한 자리 반올림은 표시 helper에서 수행한다. 원본 roll·API·저장 값·원문·단위/범위를 반올림된 표시값으로 바꾸지 않는다. 영어 복사 텍스트에는 영어 numeric locale을 사용한다. implicit와 fractured explicit는 다른 개념이다.

일반 Divine은 재굴림 가능한 비분열 explicit만 대상으로 하며 implicit는 보존한다. 변동 범위가 없는 상태에 가짜 reroll을 만들지 않는다. [품질 상한 보존](workbench-quality-preserve-2026-10-04.md)은 기존 보유량과 현재 cap을 구별한다.

`derivedDefences(state, definitions)`의 armour/evasion/energyShield는 `(base + local flat) * (1 + local increased / 100) * (1 + equipmentQuality / 100)`로 계산한다. 정확한 local stat ID를 사용하고 global stat·ring/jewel·관측 합계를 다시 더하지 않는다. hybrid는 해당 항목마다 한 번만 적용한다. 중간 반올림 없이 최종 formatter를 쓰며 결과를 modifier.values에 저장하지 않는다. 품질 null은 0으로 추정하지 않는다. `defenceObservation`은 관측/계산의 일치·충돌·미관측을 분리하며 원문을 보존한다.

### 장비 품질

통합 기준의 실행 정책은 희귀도와 무관하게 한 번에 ordinary 20에 도달시키는 것이다. 작업 중간의 +1%p 문구를 다시 적용하지 않는다. Armourer's Scrap은 검토 armour, Blacksmith's Whetstone은 검토 martial weapon에 적용한다. wand/staff/sceptre에 Whetstone을 적용하도록 확대하지 않는다. Normal/Magic/Rare 모두 동일하며 20 이상·unknown quality·수정 불가 condition은 거부하고 기존 20 초과 수치를 낮추지 않는다.

정책 ID는 Java `QualityCurrencyRules.VERSION`의 `user-selected-equipment-quality-maximum-v2`다. 성공의 events/consumedOmens는 비어 있고 remainingOmens 전체와 다른 상태를 보존한다. assumption으로 사용자 simulator 정책임을 밝힌다. 실제 게임에서 한 번에 20에 도달한다는 뜻이 아니다. UI의 최대 품질 설명과 ordinary 20 적용 한도를 혼동하지 않는다.

### 축성

Rare의 `Omen_of_Sanctification`은 DIVINE trigger와 기존 `SANCTIFIED` condition을 사용한다. 같은 trigger의 Blessed 조합은 거부하고 다른 trigger의 징조는 보존한다. `sanctification`은 `policy: independent-stat-percent-v1`과 implicit/explicit별 modifier/stat의 78..122 정수 percent를 별도 보관한다. 원래 modifier.values는 변경하지 않는다.

각 stat별 독립 균등 정수 배율·분열 표시·미공개 슬롯 보존은 simulator 정책이며 공개 게임 분포로 주장하지 않는다. 기본 품질·요구사항·socket/affix capacity·최대 품질·binary skill/socket·unscalable 효과에 임의 배율을 부여하지 않는다. 미해석 Augment 원문·분열·슬롯·관측을 삭제하지 않는다. Divine 미지원 base를 자동 확장하지 않으며 검토된 basic/Time-Lost 주얼만 기존 자격대로 포함한다.

축성된 상태는 읽기·저장·복원·내보내기를 유지하고 현재 지원에서 확인하지 못한 수정은 거절한다. 이것이 실제 게임의 모든 조작이 금지된다는 주장은 아니다. 일반 Divine의 implicit 보존과 구별하며 일반 Divine reroll을 먼저 적용하지 않는다.

SANCTIFIED 표시에서만 `preciseModifierProjection`으로 원본 정수 × catalyst × 기존 Jewel 효과 × 축성 percent를 정확한 정수비로 합성한다. 예: `source * (100 + matchingQuality) * (100 + existingJewelEffect) * sanctificationPercent / 1_000_000`. 이미 반올림된 catalyst 표시값에 다시 배율을 곱하지 않는다. 비축성 상태는 기존 사용자 정수 표시 정책을 유지한다. BigInt ratio/표시 projection 객체는 저장 JSON에 넣지 않고 원래 roll과 숫자형 metadata를 저장한다. 관련 구현은 `sanctifiedValues`, `validSanctification`, `QualityCurrencyRules`, `preciseModifierProjection`과 `localizedModifierText`다.

<a id="chaos-repeat"></a>

## 반복 Chaos

전용 [HTTP 계약](../contracts/chaos-repeat/README.md), [클라이언트 검증](../frontend/src/features/crafting/chaos-repeat/api/README.md), [표시 golden](../frontend/src/features/crafting/chaos-repeat/display/README.md)을 따른다. 기존 [Film 반복 기록](../frontend/src/features/crafting/chaos-repeat/README.md)은 저장 계약이며 초기 문서의 UI/API 미연결 문구를 현재 전체 기능 미구현으로 읽지 않는다.

UI의 1000회 버튼은 모드 토글이며 사용 동작 자체가 아니다. 기존 stash/favorite 선택과 중앙 아이템 사용이 batch를 시작한다. 현재 UI는 최대 1000을 명시하고 최소 한 번 적용 후 매치 시 조기 종료한다. 다른 화폐는 단일 경로를 유지한다. 별도 횟수 편집기를 추가하지 않는다. 요청 중 수정은 잠그고 기존 버튼의 계산 상태와 공통 피드백을 사용한다.

Page는 requestId·item/base revision·Film/cursor·locale·전체 activeOmens·catalog를 요청 전에 고정한다. adapter가 원문 body와 SHA256을 대조하고 전용 검증을 마친 receipt만 저장한다. 임의 `() => true` callback·타입 강제 변환·JSON 복원 객체로 receipt 검증을 대체하지 않는다. 브라우저의 JavaScript RegExp로 서버 RE2J 판정을 다시 하지 않는다.

최종 결과와 성공 횟수만 한 Frame으로 기록한다. aggregate consumedOmens와 마지막 성공 witness의 events/assumptions를 구별하며 횟수로 소비를 곱하지 않는다. CONCRETE/ABYSS, slot·분열·품질·축성·관측·overflow의 모든 상태를 보존한다. 실패·취소·stale 응답·불확실한 결과가 기존 Film을 덮어쓰지 않도록 한다.

<a id="validation"></a>

## 검증과 알려진 한계

각 변경은 [AGENTS.md](../AGENTS.md)의 영향 범위 검사를 따른다. 문서 정리에는 링크·참조·원자료 무변경 검사를 사용한다. 코드 변경의 전체 Backend `check`는 Docker 통합을 포함하며 generateJooq는 임시 PostgreSQL에서만 수행한다. source copy·unit·integration·build·실제 브라우저·실제 서버 반영은 별개의 결과다.

과거 보고서의 테스트 수·baseline 실패·timeout 재검사·배포·권한 거부는 해당 소스와 당시 환경의 기록이다. 현재 성공/실패나 다음 작업의 승인으로 전용하지 않는다. 10/28의 `06-tier-preparation` 120000단계 실패는 옛 정책의 측정이며 후속 첫 옵션 Chaos 정책에서 동일 목표가 허용 경로 없음·확률 0으로 완료되는 검사와 구별한다. 목표 도달·열거 완료·전략 인증·화면에 보이는 상태 수는 서로 다른 의미다.

자동 탐색은 계산 예산·분기·순환·joint roll·미관측 후보의 제한을 가진다. 확률 질량을 버리거나 partial을 실패 0/완료로 표시하지 않는다. [현행 자동 명세](current/automatic-planning.md), [미해결 이슈](../ISSUES.md), 구형 certified/store/lazy 계약의 한계를 유지한다. Wisdom의 hidden payload·정확한 보존/자격 근거는 [미완료 조사](workbench-wisdom-evidence-2026-10-03.md)를 따른다.

Frontend `npm test`의 pretest는 `docs/evidence`의 베이스 원문을 읽는다. `scripts/tests/reviewed-catalog-importer.test.mjs`도 원자료를 직접 소비한다. importer·원문 검증·이미지 출처 재생성은 동적 파일명을 사용하므로 evidence를 폴더명이나 동일 hash만으로 삭제하지 않는다. 현재 근거 자료는 [0.5.5 기능별 분류](evidence/0.5.5/README.md)를 따른다. 이전 버전의 수집·실행 이력은 제거했으며, 현재 입력과 새로 실행한 검사 결과를 구별한다.

<a id="document-history"></a>

## 정리된 기록의 조회와 문서 추가 기준

아래의 작업별 Markdown은 유효한 계약을 이 문서에 통합하고 중복 실행 이력은 삭제했다. 삭제 직전 원문은 모두 commit `be48f2eb6e71f3d9f018df9d4b641119bb007ba5`에 있다.

```sh
git show be48f2eb6e71f3d9f018df9d4b641119bb007ba5:docs/master-verification-20261030.md
git show be48f2eb6e71f3d9f018df9d4b641119bb007ba5:frontend/src/features/crafting/abyss-workbench/INTEGRATION-25.md
```

| 정리된 문서군 | 유지한 내용 |
|---|---|
| docs 루트의 10/28~30 검증·배포 기록 | 상태/분열/훼손·자동 정책·호환·검증 한계와 과거 기록의 조회 방법 |
| 심연 구현 계획, manual-workbench-checkpoint | 현재 베이스/매핑·관측/훼손 상태·후속 정책 우선순위 |
| docs/evidence의 날짜별 Markdown·base audit VERIFICATION | 유효한 수동 제작·입력·품질·기보·overflow·레이아웃 계약. base audit의 원본 정책 목록은 별도 유지 |
| docs/verification의 11/04 레이아웃/scroll 보고서 | 메뉴 anchor·순서·실제 내부 scroll·접근성·검증 구분 |
| Frontend INTEGRATION*.md·VERIFICATION*.md | 부모 제어 props·상태/저장 보존·정밀 수치·API/화면 연결 계약. 담당자·작업경로·옛 patch 적용 순서·중복 완료 보고는 제거 |

앞으로 같은 기능의 상태 변화는 현행 명세와 이 문서를 갱신한다. 날짜별 완료 Markdown을 반복 생성하지 않는다. API wire·source 원장·승인 원문·라이선스처럼 독립적인 계약/근거는 해당 파일을 유지하고 링크한다. 원문 측정/검증 결과는 소스 버전과 명령을 식별할 수 있는 기존 구조화 자료에 보존한다. 과거 문서 삭제는 결함 종결·게임 확률 검증·서버 배포를 뜻하지 않는다.
