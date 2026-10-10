# Craft Support 시작 화면

## 현재 동작 (2026-10-10)

사용자 제공 3열 스케치와 트리 스케치의 실제 이미지를 기준으로 수정했다. 데스크톱에서는 시작 아이템 설정, 실시간 ItemCard, 기존 stat filter 순서로 배치하며 1100px 이하에서는 한 열로 표시한다. 아이템 텍스트는 모달에서 가져오고 확인한다. 모달을 닫으면 진행 중 요청을 취소하고 입력을 보존하며 열기 버튼에 focus를 돌려준다.

속성은 선택한 catalog의 `familyIds`, `affixType`, `layer`, 전체 stat ID 구성이 같은 항목끼리 묶고 별도로 tier를 선택한다. family 또는 stat 근거가 없는 항목은 modifier ID별로 분리한다. 표시 문구·번역·수치 범위를 그룹 식별자로 사용하지 않는다. 기존 level·family 충돌·affix/Crafted 용량·정수 roll 검증을 통과한 후보만 선택할 수 있으며 추가 시에도 재검증한다. 검색 combobox는 번역된 효과와 영어 원문/이름을 검색하고 ArrowUp/Down, Enter, Escape 및 한글 IME를 지원한다. 그룹 선택만으로 아이템을 변경하지 않는다.

기존 stat filter의 조건 편집과 판정은 유지한다. 별도 equipment filter는 없으며 목표 base를 시작 아이템에 맞춰 갱신하되 stat 행은 보존한다. compact 화면의 판정은 `/goal-filters/evaluate`만 호출한다. 실험 확률 계산, 수동 action policy·checkpoint·실패 상태, 별도의 family 비교와 개발 설명은 서비스 화면에 표시하지 않는다. 이전 계산기 API와 해당 모듈의 검증은 그대로 유지한다.

제작 시작은 설정을 접고 당시 카드·아이템·catalog 정의를 루트로 보존한다. 카드 위치에서 루트로 이동하는 애니메이션과 결과 focus/scroll을 제공하며 `prefers-reduced-motion`이면 애니메이션·smooth scroll을 사용하지 않는다. 반복 시작은 루트 하나를 교체한다. 재편집 시 이전 루트는 유지한다. 경로·확률을 만들어내지 않고 빈 트리 안내를 표시한다. 새 UI와 목표 필터 조작 레이블은 기존 6개 locale을 따른다. 생성한 원문과 검증된 ID/수치는 번역으로 변경하지 않는다.

자동 경로 연결의 후속 범위는 stat 목표에서 유효한 정책/후보를 생성하고, 실제 계산 결과로 경로의 상태·간선·확률 범위 및 추천 가능 여부를 반환하는 backend 계약이다. 현재 first-hit API는 명시적인 policy와 modifier/checkpoint 목표를 요구하므로 UI에서 임의 정책을 합성하지 않는다. 기존 Solar 외 붙여넣기 mapping 제한과 goal catalog 지원 범위는 확장하지 않았다.

현재 QA 주소는 `http://localhost:18090`이다. 아래 추가 수정은 기존 `exile-first-hit-20261010` 프로젝트의 frontend와 app을 갱신한다. PostgreSQL/Redis와 기존 volume은 보존한다.

### 검색·희귀도·목표 편집 추가 수정

시작 속성과 목표 능력치 검색은 문서 흐름에서 분리된 fixed popover 안에 표시한다. 화면 아래 공간이 부족하면 입력 위에 열며 viewport 너비에 맞춰 제한한다. 결과 개수는 설정 박스의 높이에 영향을 주지 않고 목록 내부에서 scroll한다. 목표 검색도 입력 focus를 유지한 ArrowUp/Down·Enter 선택, Escape 닫기, IME 조합 보호를 제공한다.

시작 희귀도는 Normal/Magic/Rare를 직접 선택한다. initial API의 추가 필드 `baseRules`는 `ItemStateValidator.slots`가 사용하는 동일 `ItemCatalog.BaseItem`이다. 장비 편집은 catalog의 Magic/Rare 접두·접미 한도를 사용하고 Jewel은 기존 `reviewedBasicJewel`/`jewelCapacity` 특수 규칙을 재사용한다. 한도 데이터가 없으면 Normal 외 편집을 허용하지 않는다. 기존 속성과 충돌하는 희귀도는 비활성화하고 사유를 표시한다. 속성 추가는 현재 희귀도를 그대로 사용하며 자동 승격하거나 속성을 삭제하지 않는다. 검증된 import, 직접 수정한 텍스트의 재검증, 미리보기, 시작 루트 모두 같은 ConcreteItem을 사용한다.

목표 그룹은 모두 필수, 지정 개수 충족, 제외를 사용자 언어로 표시한다. IF와 두 가중 방식은 고급 조건 option group으로 구분하며 기존 AST와 판정 의미를 유지한다. 그룹 설명과 최소/최대 입력을 함께 제공하고 percent 단위는 `%`, flat은 locale의 값 레이블로 표시한다. 일반·고정 속성과 유사 능력치 합계는 검색에서 구분하며 같은 표시 이름이라도 stat ID가 다르면 개별 행으로 남는다.

### 사용자 테스트 순서

1. `http://localhost:18090`의 제작 지원 화면에서 Solar Amulet을 선택한다. 1360×900에서는 설정/미리보기/목표 3열을, 390×844에서는 세 영역에 순서대로 접근할 수 있는지 확인한다.
2. 시작 희귀도를 마법으로 바꾸고 속성 추가를 연다. 검색 입력을 클릭하고 긴 목록을 scroll한다. Escape와 재열기를 반복해 설정 박스 크기가 목록 때문에 변하지 않는지 확인한다. 방향키로 선택하고 Enter 후 tier를 선택한다.
3. 속성이 있는 상태에서 일반 선택이 비활성화되고 사유가 나오는지 확인한다. 마법 한도를 채우면 같은 쪽 추가 후보가 사라지는지, 희귀로 직접 바꾼 뒤에는 허용되는 후보가 늘어나는지 확인한다. 추가로 속성을 넣고 마법 한도를 넘기면 마법으로 내릴 수 없어야 한다. 기존 속성과 수치는 유지되어야 한다.
4. 아이템 텍스트 가져오기에서 생성된 `Rarity`와 값을 확인한다. Solar 영문 게임 텍스트를 붙여넣거나 수동 수정한 후 검증한다. 성공한 희귀도가 선택기와 카드에 반영되어야 한다. 검증되지 않은 텍스트는 시작할 수 없어야 한다. Escape로 닫으면 입력과 열기 버튼 focus가 보존되어야 한다. Solar 외 임의 텍스트의 catalog 매핑은 아직 제한된다.
5. 목표 능력치 검색에서 Life 또는 Resistance를 입력한다. 일반 속성·고정 속성·유사 합계를 구분하고 필요한 행을 Enter 또는 추가 버튼으로 선택한다. 최소/최대 값을 수정하고 삭제 후 검색 focus를 확인한다. `%`와 값 단위를 확인한다.
6. 그룹을 추가해 모두 필수, 지정 개수 충족(최소 1, 최대 비움), 제외를 각각 선택한다. 고급 조건에서 IF와 가중 방식의 설명·가중치 입력도 확인한다. 그룹 접기/펼치기와 비활성화 후에도 입력이 보존되어야 한다. 판정 가능한 catalog의 실제 수치만 현재 아이템 판정으로 확인하고 미지원 안내를 성공이나 0%로 해석하지 않는다.
7. 제작 시작을 누르면 시작 노드 하나와 빈 트리 안내가 표시된다. 설정 재편집 중에는 이전 시작 노드가 유지되고 다시 시작하면 새 노드 하나로 교체되어야 한다. 자동 추천 경로·성공 확률은 이번 테스트 대상이 아니다.
8. 기존 6개 언어로 변경해 희귀도·그룹·검색 레이블을 확인한다. 각 화면 크기에서 검색 목록의 위/아래 경계, 긴 목록 scroll, 취소와 재시작을 반복한다.

이 순서는 사용자 확인 절차이며 실행 완료 증거가 아니다. 이번 작업 세션에서는 CUA가 `Browser is not available: iab`를 반환하고 브라우저 목록이 비어 있어 실제 브라우저 QA가 차단됐다. 자동 테스트와 별도로 위 화면 검증이 필요하다.

## 이전 구현 기록

아래 내용은 이전 화면의 구현·검증 이력이다. 현재 UI 동작과 QA 주소는 위 명세를 따른다.

이전 디자인 commit `718a2414808a9579510b649f4bf34e1e0eccce7c`에서 이어서 작업했다. 변경은 `CraftSupport.tsx`와 전용 `goal-filter/CraftStart` 연결·스타일·테스트에 한정한다. backend, registry, ItemState, 공통 CSS/i18n은 변경하지 않는다.

실제 Kakao PoE2 거래소의 렌더링 DOM/computed CSS를 확인했다. 1264×800에서 일반/능력치 열은 각각 599.5px, 시작 위치는 x=15/634.5다. 헤더·필터·입력은 30px, 숫자 칸은 약 64px, 접힌 그룹은 33px(헤더 30px+간격 3px)이다. 입력 배경은 `#1e2124`, 라벨은 `#a38d6d`, 선은 `#3b3e3f`다. FontinSmallcaps와 배너 자산은 복사하지 않고 기존 글꼴을 사용한다.

## 화면과 상태

- 첫 행은 전체 폭의 거래소형 필터다. 기존 AND/NOT/IF/COUNT/WEIGHTED V1/V2, catalog 검색·판정·지원 경고는 유지한다. 일반 필터는 지원되는 base/level/rarity만 제공한다.
- 두 번째 행은 왼쪽 equipment type/base/텍스트 편집기와 큰 ItemCard다. base 선택은 서버 초기 상태와 표시 텍스트를 함께 갱신한다. 붙여넣기는 기존 parser로 표시 구조를 얻고 Solar catalog mapping API로 검증한다. 늦은 응답·취소 요청은 입력을 덮어쓰지 않는다.
- 기존 pasted mapping은 Solar Amulet만 지원한다. 다른 base의 서버 기본 텍스트는 서버 초기 상태에서 생성한 정확한 문자열에 한해서 확인할 수 있다. 그 외 붙여넣기는 표시와 원문을 보존하고 제작 시작을 차단한다. unknown base를 Solar로 대체하지 않는다.
- modifier 편집은 제공되는 17개 base의 Initial definitions를 사용한다. 장비 Normal 0/0·Magic 1/1·Rare 3/3, family·ID 중복, layer·stat set·정수 범위를 검사한다. 주얼은 기존 `reviewedBasicJewel`와 `jewelCapacity`를 재사용하며 현재 slot 한도로 추가를 제한하고 Crafted cap 및 기존 cap-loss 상태를 구분한다. 선택 후 수치는 명시적으로 catalog 최소값으로 시작하며 편집할 수 있다. 실제 게임 roll이나 확률을 생성한 것으로 표시하지 않는다.
- Start Crafting은 설정을 접고 현재 ItemCard를 최상위 루트로 보존한다. 아래에는 `No crafting paths yet.`만 표시한다. 경로·확률 API 호출이나 결과 노드를 추가하지 않는다. 재편집과 반복 시작은 설정 상태를 보존하고 기존 루트 1개를 교체한다.
- 숫자 평가는 기존 Evaluate 버튼에서만 실행한다. 이전 family/tier 비교는 `Advanced family / tier comparison`에서 사용하며 시작 화면과 별개의 기존 Solar 입력을 유지한다.
- 기존 omen 선택과 계산 시간 설정은 숫자 평가와 family 비교에서 계속 공유한다.
- 700px 이하에서는 필터와 생성 폼·미리보기가 한 열로 표시된다. desktop 전용 바깥 여백 보정은 Craft Support 안에서만 적용한다.
- dropdown으로 만든 상태의 정확한 생성 문자열을 다시 Check하면 검증된 편집 상태를 재사용한다. textarea를 직접 수정하거나 새로 붙여넣으면 이 신뢰가 해제되어 non-Solar 임의 텍스트 mapping을 허용하지 않는다. `Amulet`/`Amulets`는 기존 Solar mapper 계약의 정확한 alias만 허용하며 fuzzy base matching은 하지 않는다.
- 이 편집기는 quality·특수 조건·fractured modifiers를 지원하지 않으며 시작을 차단하고 이유를 표시한다. catalog stat definitions가 없는 modifier와 비-EXPLICIT layer는 dropdown에서 제외한다. 사용 가능한 후보가 없으면 level·affix/Crafted capacity 안내를 표시한다.

## 검증 환경

전용 QA 주소는 `http://localhost:18093`다. `18092`는 a9fe 기존 화면 비교용이고, `18091`은 Vite 서버가 실행되지 않는 Node 도구 컨테이너의 포트 예약이다. 기존 `8081`/`8080` 서비스·DB/Redis volume은 교체하지 않는다. 기준 backend는 기존 a9fe 이미지이며 전용 tmpfs PostgreSQL/Redis와 `18080`에서 실행한다.

증거는 worktree 밖 `../goal-filter-trade-design-evidence/`에 저장한다. 후속 캡처는 `start-reference-*`, `start-after-*`, `start-tree-*`이고 검증 로그는 `start-final-*`다. 이미지와 개인 아이템 원문은 Git에 추가하지 않는다. Backend/Compose/Windows 스크립트는 변경하지 않아 해당 검사는 이번 변경에서 실행하지 않는다.

## 확인 결과

후속 modifier editor 검증: Docker `npm ci`, lint, typecheck, format:check, 전체 60 files / 1777 tests, build 통과. 17개 base 모두 실제 브라우저에서 catalog modifier 추가 → Start → 재편집을 통과했고, 같은 상태의 기존 backend actions 검증도 모두 HTTP 200이었다. Ruby 2 prefix/2 suffix 추가 후 후보가 비워지는 한도, Ring 생성 텍스트 재확인, 390px jewel 키보드 시작/재편집과 가로 넘침 없음도 확인했다. 후속 증거는 `base-editor-17-dom.json`, `base-editor-17-server.json`, `base-editor-ring-1264.jpg`, `base-editor-ring-root.jpg`, `base-editor-ruby-390.jpg`, `base-editor-*-final.log`이다. 공통 validator는 재사용만 했으며 registry/backend/global CSS/i18n은 변경하지 않았다.

Docker에서 `npm ci`, lint, typecheck, format:check, 전체 59 files / 1770 tests, build가 통과했다. 브라우저에서 base↔text 동기화, 실제 붙여넣기의 Solar 자동 선택, unknown base·미해석 행 보존과 시작 차단, modifier 추가, 시작/재편집/반복 시작의 단일 루트 유지, 검색 keyboard focus, 6종 그룹 전환, 접기·삭제 후 focus 복귀, 잘못된 숫자 차단을 확인했다. 실제 숫자 평가도 `NO_MATCH`와 `PARTIAL/BUDGET_EXHAUSTED` 응답을 확인했다. 390px에서 document scrollWidth는 375px(clientWidth 375px)이며 가로 넘침이 없다.

기존 npm audit high 1건과 Vite 500kB bundle 경고는 남아 있다. 거래소 전체 페이지의 배너·전용 글꼴·미지원 일반 필터는 구현 범위 밖이다. 390px에서는 참고 거래소의 고정 폭·가로 스크롤 대신 한 열 배치를 사용한다. 초기 빈 그룹의 서버 검증 경고는 기존 계약 그대로 표시된다.

## Home and remote integration (2026-10-09)

The starting-item selector and Add Modifier editor use all 134 reviewed Workbench bases from the shared base registry, preserving the original 17 entries and their order. The Goal Filter backend still exposes its original 17 reviewed catalogs; other contexts explicitly report unsupported catalog capability. Numeric addition remains restricted to the declared Solar model. Arbitrary pasted modifier mapping for non-Solar items and actual crafting path calculation remain unsupported.
