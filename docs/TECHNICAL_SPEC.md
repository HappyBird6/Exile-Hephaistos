최신 base 확장: [Bow 5종](workbench-bows-bundle-2026-10-04.md). Workbench 46 bases; 선택된 highest-tier armour 24종과 Bow 5종을 제공한다. 개별 weapon property·implicit·source pool·6locale·기존 film을 보존하며 Support/Explorer는 Solar-only. Ring8·Amulet7·Wand/Sceptre·Belt 및 다른 equipment category는 후속 source 검증 범위다.

# Exile-Hephaistos 기술 명세

2026-09-29 · 현재 구현 기준

## 제품 범위

제작 작업대, 영어 아이템 텍스트 분석과 Solar Amulet의 확률 탐색을 제공한다. 확률 결과를 선택해 다음 상태를 탐색하며 화폐 소모나 무작위 수치 roll은 하지 않는다. 게임 규칙과 데이터는 출처 검증 없이 추정하지 않는다.

- `/`: 재료 탭·검색·툴팁·화폐 선택·공유 즐겨찾기·아이템 입력 dialog와 카드.
- `/admin`: 조건 없이 접근하는 최소 페이지. 현재 실행 가능한 관리 도구는 없다.
- `/admin/crawling`: 이전 주소로 들어오면 `/admin`으로 정리한다. 관련 서버 API는 없다.
- `POST /api/v1/items/parse`: 영어 게임 복사 텍스트를 표시용 Item으로 분석한다. 계약은 [openapi-item.yaml](openapi-item.yaml)을 따른다.
- `/api/v1/crafting`: 초기 상태·사용 가능 화폐·단일 전이·제한된 화폐 순서 탐색. 계약과 지원 범위는 [ItemState 명세](item-state.md)를 따른다.

## Backend

Java 21 / Spring Boot 3.5.16 / Gradle 8.14.3. 현재 파서는 네트워크·DB 조회 없이 실행한다.
`ItemModels`는 원문·속성·요구사항·modifier 표시 정보·경고를 담는다. `item.testparser`는 요청 검증, 파싱 및 오류 처리를 소유한다.
입력은 UTF-8 16 KiB로 제한한다. 잘못된 JSON은 400, 초과 입력은 413, 분석 불가능한 영어 텍스트는 422다. 오류에 원문·stack trace를 노출하지 않는다.
인증·로그인·세션 처리는 없다. 미등록 경로는 404이며 존재하는 경로의 잘못된 HTTP method는 405다.
Actuator는 health만 노출하며 상세 정보는 숨긴다. liveness는 프로세스, readiness는 앱·PostgreSQL·Redis 상태를 확인한다. 미구현 catalog 때문에 readiness를 강제로 실패시키지 않는다.

## 저장 기반

사용자 요청으로 PostgreSQL 17.6·Redis 7.4.5, JPA·jOOQ·Flyway·codegen과 격리된 통합 테스트를 유지한다. 다음 기능을 위한 연결 기반이며 현재 아이템 분석 결과를 DB에 저장하지 않는다.
Hibernate는 `ddl-auto=validate`, OSIV off다. jOOQ codegen은 임시 PostgreSQL에 migration을 적용해 생성한다.
기존 migration 두 개는 변경하지 않는다. 과거 기능의 table과 사용하지 않는 schema가 남아 있지만 실행 코드·API는 없고, 관련 schema를 위한 새 codegen은 하지 않는다. 기존 설치와 Flyway 이력 호환을 위한 보존이며 데이터 삭제를 자동 수행하지 않는다.
Solar Amulet용 불변 ItemState·modifier 모델·검증기와 PoE2DB Base 속성 JSON 스냅샷을 제공한다. CraftingEngine은 순수 Java로 화폐 6종의 전이를 계산하고 GraphExplorer는 같은 상태를 공유하며 확률을 누적한다. 스냅샷 로더는 번들 파일만 읽는다. 전이 결과는 개수 제한이 있는 메모리 LRU에 보관하며 DB 저장·Redis 캐시·런타임 외부 조회는 하지 않는다.
Modifier·CurrencyRuleDefinition·ExaltedAction의 나머지 준비 파일은 유지한다.

## Frontend

React / TypeScript strict / Vite. 표시 언어는 영어 고정이며 번역 Provider·언어 선택·언어 저장·자동 감지는 없다.
TanStack Query가 분석·확률 응답과 선택한 탐색 상태·복귀 기록을 관리하고 Zustand가 편집 입력을 관리한다. 응답을 두 곳의 원본으로 중복 보관하지 않는다.
분석 성공 시 카드와 원문을 함께 교체한다. 실패·취소·늦은 응답은 기존 아이템을 덮어쓰지 않는다. 입력 dialog를 닫으면 진행 중 요청을 무효화한다.
마우스와 키보드 조작, 접근성 label, focus 복귀, 이미지 실패 대체 표시를 유지한다. 재료 이름·효과 설명은 정적 표시 자료이며 검증된 제작 규칙이 아니다.

## 실행 및 검증

Docker Compose는 Frontend·Backend·PostgreSQL·Redis를 실행한다. Python 실행 환경과 외부 수집 배치는 없다. 서비스 포트는 localhost에만 노출한다.
`./scripts/dev.ps1`은 최초 실행에 필요한 DB·Redis 환경변수만 `.env`로 생성하며 기존 `.env`와 DB volume을 보존한다. `.env.example`과 GitHub Actions는 사용하지 않는다.
`.gitattributes`와 `.editorconfig`는 Windows/Linux 줄바꿈 및 편집 일관성을 위해 유지한다.
Backend unit·ArchUnit·DB/Redis 통합 검사, Frontend 동작 회귀·타입·lint·build 및 실행 스크립트 검사를 사용한다. 자세한 실행 명령은 [README](../README.md)를 따른다.

## 변경 원칙

현재 범위에 필요하지 않은 기능·설정·서비스는 미리 만들지 않는다. 실제 요청이 생겼을 때 해당 요구사항과 검증을 추가한다.
기존 데이터·출처·라이선스는 보존하며, 사용자 명시 요청 없이 DB 삭제·초기화·운영 배포를 하지 않는다.
