# Craft Support 시작 화면

이전 디자인 commit `718a2414808a9579510b649f4bf34e1e0eccce7c`에서 이어서 작업했다. 변경은 `CraftSupport.tsx`와 전용 `goal-filter/CraftStart` 연결·스타일·테스트에 한정한다. backend, registry, ItemState, 공통 CSS/i18n은 변경하지 않는다.

실제 Kakao PoE2 거래소의 렌더링 DOM/computed CSS를 확인했다. 1264×800에서 일반/능력치 열은 각각 599.5px, 시작 위치는 x=15/634.5다. 헤더·필터·입력은 30px, 숫자 칸은 약 64px, 접힌 그룹은 33px(헤더 30px+간격 3px)이다. 입력 배경은 `#1e2124`, 라벨은 `#a38d6d`, 선은 `#3b3e3f`다. FontinSmallcaps와 배너 자산은 복사하지 않고 기존 글꼴을 사용한다.

## 화면과 상태

- 첫 행은 전체 폭의 거래소형 필터다. 기존 AND/NOT/IF/COUNT/WEIGHTED V1/V2, catalog 검색·판정·지원 경고는 유지한다. 일반 필터는 지원되는 base/level/rarity만 제공한다.
- 두 번째 행은 왼쪽 equipment type/base/텍스트 편집기와 큰 ItemCard다. base 선택은 서버 초기 상태와 표시 텍스트를 함께 갱신한다. 붙여넣기는 기존 parser로 표시 구조를 얻고 Solar catalog mapping API로 검증한다. 늦은 응답·취소 요청은 입력을 덮어쓰지 않는다.
- 기존 pasted mapping은 Solar Amulet만 지원한다. 다른 base의 서버 기본 텍스트는 서버 초기 상태에서 생성한 정확한 문자열에 한해서 확인할 수 있다. 그 외 붙여넣기는 표시와 원문을 보존하고 제작 시작을 차단한다. unknown base를 Solar로 대체하지 않는다.
- modifier 편집은 기존 Solar catalog의 tier·level·family·affix 제한을 사용한다. 선택 후 수치는 명시적으로 catalog 최소값으로 시작하며 편집할 수 있다. 실제 게임 roll이나 확률을 생성한 것으로 표시하지 않는다.
- Start Crafting은 설정을 접고 현재 ItemCard를 최상위 루트로 보존한다. 아래에는 `No crafting paths yet.`만 표시한다. 경로·확률 API 호출이나 결과 노드를 추가하지 않는다. 재편집과 반복 시작은 설정 상태를 보존하고 기존 루트 1개를 교체한다.
- 숫자 평가는 기존 Evaluate 버튼에서만 실행한다. 이전 family/tier 비교는 `Advanced family / tier comparison`에서 사용하며 시작 화면과 별개의 기존 Solar 입력을 유지한다.
- 기존 omen 선택과 계산 시간 설정은 숫자 평가와 family 비교에서 계속 공유한다.
- 700px 이하에서는 필터와 생성 폼·미리보기가 한 열로 표시된다. desktop 전용 바깥 여백 보정은 Craft Support 안에서만 적용한다.

## 검증 환경

전용 QA 주소는 `http://localhost:18093`다. `18092`는 a9fe 기존 화면 비교용이고, `18091`은 Vite 서버가 실행되지 않는 Node 도구 컨테이너의 포트 예약이다. 기존 `8081`/`8080` 서비스·DB/Redis volume은 교체하지 않는다. 기준 backend는 기존 a9fe 이미지이며 전용 tmpfs PostgreSQL/Redis와 `18080`에서 실행한다.

증거는 worktree 밖 `../goal-filter-trade-design-evidence/`에 저장한다. 후속 캡처는 `start-reference-*`, `start-after-*`, `start-tree-*`이고 검증 로그는 `start-final-*`다. 이미지와 개인 아이템 원문은 Git에 추가하지 않는다. Backend/Compose/Windows 스크립트는 변경하지 않아 해당 검사는 이번 변경에서 실행하지 않는다.

## 확인 결과

Docker에서 `npm ci`, lint, typecheck, format:check, 전체 59 files / 1770 tests, build가 통과했다. 브라우저에서 base↔text 동기화, 실제 붙여넣기의 Solar 자동 선택, unknown base·미해석 행 보존과 시작 차단, modifier 추가, 시작/재편집/반복 시작의 단일 루트 유지, 검색 keyboard focus, 6종 그룹 전환, 접기·삭제 후 focus 복귀, 잘못된 숫자 차단을 확인했다. 실제 숫자 평가도 `NO_MATCH`와 `PARTIAL/BUDGET_EXHAUSTED` 응답을 확인했다. 390px에서 document scrollWidth는 375px(clientWidth 375px)이며 가로 넘침이 없다.

기존 npm audit high 1건과 Vite 500kB bundle 경고는 남아 있다. 거래소 전체 페이지의 배너·전용 글꼴·미지원 일반 필터는 구현 범위 밖이다. 390px에서는 참고 거래소의 고정 폭·가로 스크롤 대신 한 열 배치를 사용한다. 초기 빈 그룹의 서버 검증 경고는 기존 계약 그대로 표시된다.
