# Goal filter 거래소 디자인

## 범위와 기준

`codex/goal-filter-numeric`의 `fc0fd61fda003ce42c6bb635e0f821707364dee8`에서 작업했다. 사용자 지정 Kakao PoE2 거래소의 로그인된 실제 필터 화면과 기존 구현을 같은 1264×800 viewport에서 확인했다. 외부 검색 제출, 거래, 연락, 조건 저장은 하지 않았다.

변경은 `GoalFilterPanel.tsx`, `goal-filter.css`와 관련 UI 테스트에 한정했다. registry, ItemState, 공통 CSS/i18n, CraftSupport, backend, 계약과 lockfile은 그대로다.

## 시각적 변경

- 기존 1:3 열을 동일 폭의 일반/능력치 필터 두 열로 바꿨다.
- 검은 패널, 각진 다크 입력, 얇은 테두리와 32px 행으로 밀도를 맞췄다.
- 그룹 제목/연산/활성화/접기/삭제를 한 헤더에 배치했다. 연산 설명은 select tooltip과 접근성 텍스트로 유지한다.
- 능력치 검색은 해당 그룹의 행 아래에서 열린다. 닫힌 후보 목록은 렌더하지 않는다.
- 행 활성화는 왼쪽, Min/Max/Weight와 삭제는 오른쪽에 붙였다. Pseudo 라벨과 catalog의 원래 값/단위는 유지한다.
- 700px 이하에서는 한 열로 전환하고 행 이름 아래에 숫자 입력을 배치한다.

거래소의 전체 배너·메뉴·검색 결과와 판매자/가격/온라인/지도 필터는 범위 밖이다. 우리의 catalog 라벨과 단위, 여섯 연산 선택, 제작 평가와 지원 경고 때문에 완전한 픽셀 복제는 아니다. 일반 필터도 우리가 지원하는 base/level/rarity만 표시한다.

## 검증과 운영

Docker에서 `npm ci`, lint, typecheck, format:check, 전체 tests, build를 확인했다. 전체 58 files / 1766 tests, goal-filter 관련 27 tests가 통과했다. 검색 ArrowDown/Enter/Escape, Space 활성화, 반복 그룹 추가/접기/삭제와 focus 복귀, 중간 숫자 입력과 역전 범위, 실제 catalog 평가, base 변경 경고와 기존 family/tier 경로를 확인했다. Backend와 Compose는 변경하지 않아 해당 검사는 실행하지 않았다.

기존 서비스와 DB/Redis volume을 보존하고, 별도 Docker network와 PostgreSQL tmpfs/Redis를 사용했다. QA UI는 `http://localhost:18093`, 비교 기준 UI는 `http://localhost:18092`, QA API는 `http://localhost:18080`이다. master 병합과 기존 서비스 교체는 하지 않았다.

증거와 상세 운영 체크포인트는 worktree 인접 `../goal-filter-trade-design-evidence/`에 둔다. `reference-1264.jpg`, `before-1264.jpg`, `after-final-1264.jpg`, `after-final-390.jpg`와 `*-final.log`를 참조한다. 스크린샷과 임시 QA 설정은 Git에 포함하지 않는다.
