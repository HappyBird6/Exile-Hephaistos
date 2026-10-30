# 베이스 복구 및 자동 제작 409 수정 — 2026-10-30

기준: de99900 (구현 9c7d6ed). 별도 제작 정책 변경 없음.

## 원인과 수정

- 페이지 재진입 뒤 카탈로그 새로고침은 저장된 rulesetIdentity와 새 identity의 차이를 오류로 취급했고, 성공 시에도 identity를 갱신하지 않았다. 현재 베이스/스냅샷/옵션을 새 카탈로그 검증기로 검사한 뒤 기존 아이템·수치·분열·원문을 그대로 두고 identity와 표시 카드를 갱신한다. 사라진 베이스는 Solar로 대체하지 않는다. 늦은 응답은 revision 검사로 폐기한다. 갱신 자체는 제작을 시작하지 않는다.
- 운영 접근 로그의 실제 요청은 POST /api/v2/crafting/method-searches/automatic, 409였다. 첨부 이미지 픽셀도 확인: 목표 불가 판정이 아닌 ‘계산 오류 · 입력 보존됨’ 화면이다.
- 운영 기록의 읽기 전용 사본을 격리 QA에 넣어 POST 409 / RECALCULATION_REQUIRED를 재현했다. 이전 기록의 독립 TEMPORARY_AFFIX_FOR_REMOVAL Template가 현재 ‘후속 제거 작업 필수’ 검증에 걸린다. 신규 생성의 전체 저장 기록 순회가 그래프까지 역직렬화하여 다른 기록의 오류가 새 계산까지 차단했다.
- 신규 요청 중복 확인과 실행 슬롯 계산을 저장 헤더 읽기로 분리했다. 현재 요청의 유효성·소유자·요청 ID 중복·자원 한도는 유지한다. 이전 기록을 삭제/재작성하지 않는다. 해당 이전 기록 자체를 복원할 때는 기존 재계산 안내를 유지한다.
- 실제 사용자 브라우저의 요청 본문은 운영에서 수집하지 않았다. 운영 로그의 메서드/URL/상태와 격리 원본 기록 재현을 대조했다. 격리 UI의 제작 클릭은 수정 전 POST 409, 수정 후 탭 재진입 자동 생성은 POST 202 및 후속 control 200이었다.

## 검증

- 프런트 관련 고유 50개: CraftStart 29개 + AutomaticMethodSearch 21개. 반복 재마운트/카탈로그 새로고침, 기존 입력·run 보존, 사라진 베이스, 유효하지 않은 저장 아이템, 저장 탐색 404/409 후 명시적 재계산을 포함한다.
- lint, typecheck, format:check, build 통과. 전체 프런트 테스트는 이번 수정에서 재실행하지 않았다.
- 백엔드 관련 24개: AutomaticSearchService 14, LocalAutomaticJobRepository 2, StrictExaltedScenario 5, AutomaticNormalStart 1, Architecture 2. 이전 템플릿이 있는 상태에서 신규 요청/다른 소유자/같은 요청 재시도와 원본 바이트 보존을 확인한다.
- 통합 8개, spotlessCheck, generateJooq, bootJar 통과. 첫 통합 실행은 Ryuk의 Docker 호스트 172.17.0.1 접근 실패였으며 Windows Docker Desktop의 host.docker.internal을 지정해 통과했다. 검사를 생략하지 않았다.
- 이전 전체 백엔드 기준 실패 40개는 이번 검사와 별개다. 전체 check 통과를 주장하지 않는다.
- 격리 QA 원본 시나리오: 65 상태/58 작업, PAUSED / RESOURCE_BUDGET. 분열 불변, 훼손 상태, 마무리 Exalted 배제, GET 재읽기 동일 확인. 목표 불가 증명이 아닌 자원 예산 종료다.
- 실제 격리 UI: 이전 저장 오류 화면에서 재진입 후 일반 시작→진화/제왕→훼손 중간 경로 생성. 중단 후에도 그래프 유지. 별도 목표 불가 필터 변경은 필요하지 않았다. UI 재현은 첨부와 완전히 동일한 목표 입력이 아닌 기존 QA 목표를 사용했다.

## 배포

검증 후보를 로컬 commit 후 최신 운영 데이터 일관 백업 및 격리 PostgreSQL 복원 검증을 거쳐 9000/9001에 반영한다. 운영 검증은 health/GET/실행 JAR·프런트 산출물 해시만 사용한다. 운영 쓰기 QA는 하지 않는다. 배포 후 결과는 별도 기록한다.
