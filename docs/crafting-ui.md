# 제작 작업대 UI

2026-09-22 사용자 승인 범위: 화면과 선택/클릭 인터랙션. 기본 베이스는 태양의 목걸이이며 규칙과 레벨·옵션을 추정하지 않는다. 2026-09-23 요청으로 영문 텍스트 분석 API를 연결해 아이템 정보와 미해석 행을 표시한다. 결과는 catalog 미검증 초안이며 원문을 보존한다. 탭 전환으로 원문 입력은 지워지지 않으며 명시적 베이스 배치는 현재 아이템을 교체한다. 현재 draft는 브라우저 메모리에만 저장된다. UI 언어 설정·확장 및 입력 흐름은 [Frontend i18n 문서](frontend-i18n.md)를 따른다.

## 이미지와 구성

[PoE2DB 한국어 Currency](https://poe2db.tw/kr/Currency)를 2026-09-22 조회해 표시용 이름·배치·이미지 URL을 확인했다. Currency Tab 37종에서 화폐 조각 4종과 사용자가 X로 제외한 감정 주문서를 제외한 32종이다. 상위·완벽 화폐는 페이지가 사용하는 같은 원본 이미지에 II/III 표시를 덧붙인다. 효과나 지원 mechanic 목록을 의미하지 않는다.

다운로드 출처·SHA-256은 `frontend/public/assets/currency/sources.json`에 저장한다. 사용자 승인한 해당 페이지의 소수 이미지 다운로드만 수행했고 관리자 수집 기능은 실행하지 않았다. 게임 아트의 권리는 원저작권자에게 있으며 이 기록은 재배포 허가를 의미하지 않는다.

- 실제 확보 파일: 화폐 및 배경·목걸이 이미지 24개.
- 이전 CDN 403으로 미확보였던 `CurrencyRerollSocketNumbers02.webp`(상위 쥬얼러 오브), `HinekorasLock.webp`(히네코라의 머리카락)는 사용자 승인 재시도로 2026-09-22 각 HTTP 200 응답을 받아 확보했다. URL 변경이나 차단 우회 없이 정상 접근했다.
- 두 파일 모두 RIFF/WEBP signature와 실제 이미지 디코딩·시각 확인을 통과했다. HTTP Content-Type은 응답 도구에서 확인되지 않았다.
- 런타임 이미지 로딩이 실패하면 슬롯/포인터에 이미지 미확보 안내를 표시하는 폴백은 유지한다.

TODO(domain): 파싱 초안의 catalog 검증·화폐 효과 계약 필요 / supported-mechanics 및 검증 snapshot / 제작 사용 결과 / 현재 사용 요청 안내만 표시하고 아이템·화폐 수량을 변경하지 않음.

## 검증 범위

프론트 테스트는 32종 표시, 제외 항목, 우클릭·클릭 선택, Esc/버튼 취소, 사용 시 상태 불변, 빈 텍스트·UTF-8 크기 제한, 파싱 정보와 미해석 원문 보존, 오류·재시도·늦은 응답 폐기를 확인한다. 언어 전환·저장소 장애 및 기존 관리자 테스트도 함께 실행한다. React 버튼의 기본 키보드 동작과 명시적 선택 상태를 사용한다. 모바일은 클릭 선택과 선택 해제 버튼을 제공한다.


## 아이템 카드 (2026-09-23)

사용자 제공 거래소 화면을 참고한 검정 바탕의 `ItemCard`로 기본 베이스와 파싱 초안을 표시한다. 장식 테두리는 보류하고 일반 회색·매직 파랑·레어 금색·고유 주황의 단순 테두리와 헤더 색만 적용한다. 레어/고유의 확인된 베이스 이름은 두 번째 줄에 표시한다. 거래 가격 (`~b/o`, `B/O`, `~price`)은 카드에서 제외하되 원문 증거는 그대로 남긴다.

`itemCardData.ts`의 표시 계약은 순수 props이며 별도 상태 저장소를 만들지 않는다. 파싱 결과는 `toItemCard`가 변환한다. 향후 엔진의 확정 결과 상태를 같은 계약으로 변환해 props를 교체하면 이름·희귀도·속성·옵션·플래그가 함께 갱신된다. 확률 평가만으로 현재 아이템을 교체하지 않는다. 실제 엔진과 연결하는 adapter는 엔진 계약 확정 후 구현한다.

백엔드가 분류한 modifier kind와 명시된 advanced copy의 affix/tier를 표시한다. 프론트는 문장 정규식으로 옵션을 다시 분류하지 않는다. 상세 펼치기에서 원본 metadata와 roll 범위를 확인하고, 미해석 행은 중립색 원문으로 보존한다. 경고는 카드 밖에 계속 표시하고 줄 번호·파싱 정보·전체 원문은 접이식 영역에서 확인한다. 아이템 데이터는 번역하지 않으며 UI label은 현재 en 리소스만 사용한다.

카드 회귀 테스트는 일반→매직→레어 props 변경 시 오래된 수치·옵션 제거, 가격 제외, 미해석 원문 보존, HTML 비실행, 상세 펼치기, UI 언어 전환을 확인한다.

## 재료 탭과 공용 즐겨찾기 (2026-09-23)

사용자 승인으로 오른쪽 상세 sidebar와 stash-inspector를 제거하고 같은 작업대 안에 Item Card만 왼쪽 아래로 옮겼다. 원문·미해석 경고·입력 오류는 중앙 아이템의 `Edit item` 버튼으로 여는 입력 패널에 남긴다. 오른쪽 아래는 설정용으로 비워둔다. 기존 사용자가 제거한 가시적 상태 안내·부가 라벨은 복원하지 않는다. 선택·등록·효과 미연결 안내는 screen reader용 live region으로 알린다.

Currency에서 Mirror of Kalandra, Orb of Chance, 품질 화폐 5종, Artificer's Orb, Jeweller 3종을 제외해 21종을 표시한다. Hinekora's Lock은 기존 Chance 좌표로 옮겼다. 오른쪽 즐겨찾기 15칸은 **5행 × 3열**이며 왼쪽 Transmutation–Chaos의 5행과 동일한 슬롯 크기·열/행 간격이다. 중앙 아이템 슬롯 하단은 Chaos 행 하단에 맞춘다. 좁은 화면에서는 카드가 슬롯 아래 전체 폭으로 표시된다.

- 기본 Currency 탭 외 Essence, Omen, Catalysts, Liquid Emotions를 단순 정렬 목록으로 제공한다. 탭 목록은 좌우/Home/End 키로 이동한다.
- 새 네 탭에서 좌클릭/Enter/Space로 재료를 들고 공용 칸을 클릭하면 등록한다. 기존 재료가 있으면 덮어쓰고 든 상태를 해제한다. 원래 목록은 유지한다.
- 탭을 바꿔도 든 재료·등록 목록·현재 아이템은 유지한다. Esc는 들기/사용 선택을 취소하고 열린 입력을 닫는다.
- 등록된 칸은 클릭 또는 우클릭으로 제작용 선택을 한다. 사용 선택은 등록용 들기와 구분되므로 다른 빈 칸을 클릭해도 복제되지 않는다.
- 즐겨찾기는 페이지 메모리에만 존재한다. 새로고침/페이지 이탈 후 복원하지 않는다. 실제 제작 효과는 연결하지 않는다.

### 표시용 재료 출처

사용자 제공 [Essence](https://poe2db.tw/kr/Essence), [Omen](https://poe2db.tw/kr/Omen), [Catalysts](https://poe2db.tw/kr/Catalysts), [Liquid Emotions](https://poe2db.tw/kr/Liquid_Emotions)의 영어 `/us/` 대응 페이지에서 **주 item 카탈로그**만 추출했다. Ref·passive·minimap·제작 recipe 결과는 제외한다. 수집일과 페이지 SHA-256, 아이콘 원본 URL·SHA-256·미확보 상태는 `frontend/public/assets/materials/sources.json`에 보존한다. `materials.ts`는 이름·표시용 ID(페이지 slug)·분류·로컬 이미지 경로만 포함한다. 게임용 catalog ID, 현재 획득 가능성, 효과·시즌 지원 여부를 뜻하지 않는다.

| 페이지의 주 카탈로그 | 표시 수 |
|---|---:|
| Essence /95 | 95 |
| Omen Item /50 | 50 |
| Catalyst Item /26 | 26 |
| Liquid Item /27 | 27 |

Essence에는 해당 페이지가 함께 나열하는 Alloy 13종이 포함된다. Liquid에는 Ancient 계열·Liquid Verisium이 포함된다. 분류를 새로 추정하지 않고 원본 페이지의 카탈로그 구성을 보존한다. 추출한 주 영역에서 명시적 disabled/unobtainable/legacy 표시를 발견하지 않았으나 현재 게임에서 전부 사용 가능하다는 의미는 아니다.

198개 항목 중 아이콘 196개를 확보했다. 다음 두 URL은 동일 URL 1회 재확인도 HTTP 403이므로 추가 재시도·우회를 하지 않았다. 이름/버튼은 유지하고 이미지 로딩 실패 시 `Image unavailable`로 표시한다.

- Perfect Essence of the Mind: `https://cdn.poe2db.tw/image/Art/2DItems/Currency/Essence/ManaEssencePerfect.webp`
- Reaver Catalyst: `https://cdn.poe2db.tw/image/Art/2DItems/Currency/Breach/BreachCatalystAttack.webp`

재료 효과 설명·확률·수량은 수집/구현하지 않았다. 게임 아트 권리는 원저작권자에게 있으며 출처 기록은 재배포 허가를 의미하지 않는다. 새로운 재료의 런타임 수집이나 관리자 crawling 기능을 추가하지 않았다.

회귀 검증은 21종 제외/이동, 다섯 탭의 목록, 15개 공유 칸, 탭 간 들기·등록 유지, 덮어쓰기·취소·사용 분리, 새 페이지 초기화, 실패 아이콘 교체, 입력 포커스/닫기, 카드/파싱 회귀를 포함한다.


## 제작대 개선 (2026-09-23)

- Currency 21개, Essence 82개(일반 76개와 특수 6개), Alloy 13개, Omen 32개, Catalysts 26개, Liquid Emotions 27개를 표시한다. Alloy는 독립 탭이며 사용자 지정 Saga 5개 및 Omen 13개를 표시 목록에서 제외했다. 기존 출처 이미지 기록은 보존한다.
- 비화폐 탭은 스크롤 목록 위의 고정 검색창을 제공하고 검색어는 탭별 메모리 상태로 유지한다. Essence는 Lesser/기본/Greater/Perfect를 한 행으로 검색하며 특수 6개는 중앙 상단 2행×3열이다. 즐겨찾기 5행×3열은 모든 탭에서 공유한다.
- 입력은 확대된 native dialog다. X/바깥 클릭/Escape로 닫고 포커스를 돌려준다. Analyze 성공 시에만 닫으며 실패 시 오류와 편집 중인 텍스트를 유지한다. 상세 진단 패널은 표시하지 않는다. 제작대 내부의 브라우저 기본 context menu는 차단한다.
- 총 201개 표시 항목은 마우스 hover와 키보드 focus로 PoE2DB 설명과 출처 링크를 표시한다. 정적 출처는 `frontend/public/assets/materials/tooltip-sources.json`, 표시 데이터는 `frontend/src/features/crafting/materialTooltips.json`. 공개 영문 목록 5개 및 robots/General disclaimer를 확인했고 원문은 git 제외 `data-pipeline/captures/tooltip-20260923/`에 보존했다. 각 URL/수집 시각/SHA-256/parserVersion을 기록했다.
- 설명은 출처 페이지의 게임 항목 표시 자료로, 검증된 patch/snapshot·weight·제작 handler 데이터가 아니다. 실제 제작 지원 범위를 넓히지 않는다. PoE2DB/GGG 권리 고지는 기존 정책대로 유지하며 공개 배포 허가를 추정하지 않는다.
- 현재 아이템 원문과 편집 입력의 소유권·향후 checkpoint 확장 경계는 `item-text-parsing.md` 참조. 체크포인트 UI/영구 저장/제작 실행은 이번 변경에 포함되지 않는다.
