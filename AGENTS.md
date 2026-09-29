# Exile-Hephaistos 개발 지침

## 운영

먼저 [Worker 운영 명세](docs/solo-workflow/MULTI_SESSION_WORKFLOW.md)를 읽는다. Git 전용 채팅은 연결된 Git 통합 절차도 따른다. 사용자 명시 지시가 이 문서보다 우선한다.
설명은 한국어로 하고 클래스·패키지·기술명은 영어를 유지한다. 제품 UI는 영어만 사용한다.
작업 시작 시 branch·HEAD·미커밋 변경과 관련 파일을 확인한다. 전용 branch/worktree에서 자기 변경만 구현·검증·commit한다. 다른 작업의 파일·컨테이너·데이터를 임의 변경하지 않는다.

## 현재 범위

- React 제작 작업대: 재료 표시·검색·툴팁·선택·즐겨찾기·아이템 입력.
- Java 아이템 텍스트 분석: 영어 게임 복사 텍스트를 표시용 구조로 변환하며 원문·미해석 행·경고를 보존한다.
- `/admin`: 인증 없이 접근하는 최소 페이지. 현재 관리 도구는 없다.
- PostgreSQL·Redis 및 기존 JPA·jOOQ·Flyway 기반은 다음 작업을 위해 유지한다.
- 주석만 있는 제작 관련 준비 파일은 사용자가 다음 작업에서 사용할 것이므로 유지한다.

요청 없이 미래 기능·추상화·외부 연동·의존성·CI를 추가하지 않는다. 새 기능은 실제 요청 시 명세와 필요한 구현을 함께 작성한다.
상세 기준은 [기술 명세](docs/TECHNICAL_SPEC.md), [아이템 파싱](docs/item-text-parsing.md), [지원 범위](docs/supported-mechanics.md)를 따른다.

## 데이터와 동작

- 게임 효과·확률·weight·tier 의미를 추측하지 않는다. 근거가 없으면 미지원과 필요한 증거를 명시한다.
- 아이템 분석 결과는 catalog 미검증 표시 데이터다. 화폐 선택으로 옵션을 변경하거나 화폐를 소모하지 않는다.
- UI에서 원문 값을 임의 보정하거나 미해석 옵션을 삭제하지 않는다. 이미지·툴팁 출처와 라이선스를 유지한다.
- 비밀값·개인 아이템 원문을 로그나 Git에 남기지 않는다. `.env`와 기존 DB volume·로컬 원본은 보존한다.
- 이미 적용된 migration SQL을 수정·삭제하지 않는다. 기존 SQL의 사용하지 않는 schema/table은 실행 기능이 아닌 이력이다. 데이터 삭제나 DB 초기화는 별도 명시적 요청 없이 수행하지 않는다.

## 구현 기준

Java 21, Spring Boot 3.x, Gradle Wrapper, React, TypeScript strict, Vite를 사용한다. 기존 잠금 파일과 formatter를 따른다.
Backend Controller는 입력·응답을 담당하고 파싱 로직은 서비스에 둔다. 오류는 Problem Details로 반환하며 내부 예외·SQL·비밀값을 노출하지 않는다.
Frontend는 서버 응답을 TanStack Query, 편집 입력을 Zustand에서 관리한다. 늦은 응답 폐기·취소·키보드 조작·label·focus 복귀를 유지한다.
사용하는 모듈 경계와 순환 금지는 ArchUnit으로 검증한다. 준비 파일을 채우기 위해 빈 구현을 추가하지 않는다.
JPA/jOOQ는 동일 DataSource와 transaction을 사용한다. codegen은 migration을 적용한 임시 DB에서만 실행한다.
Docker 서비스는 localhost에만 노출한다. 현재 관리자 페이지에는 인증·세션이 없으며 공개 운영 서비스용 접근 통제를 제공하지 않는다.

## 검증과 완료

- Backend: `./gradlew.bat check generateJooq bootJar` (Unix는 `./gradlew`). `check`는 formatter·unit·ArchUnit·Docker 통합 검사를 포함한다.
- Frontend: `npm ci`, `npm run lint`, `npm run typecheck`, `npm run format:check`, `npm run test -- --run`, `npm run build`.
- Windows 실행 스크립트 변경: `python -m unittest discover -s scripts/tests -v`. 이 Python은 개발 스크립트 테스트용이며 앱 실행에는 필요 없다.
- Compose 설정 검사: `docker compose --env-file .env -f infra/compose.yaml --profile stack config --quiet`. 비밀값이 출력되는 일반 config 결과는 공유하지 않는다.

변경 영역·직접 영향 영역을 검증한다. 단순 문서 수정에는 불필요한 테스트를 추가하지 않는다. 검증 실패를 skip이나 기대값 완화로 숨기지 않는다.
구현·테스트·자체 리뷰·commit·최종 반영을 구분한다. 실행하지 못한 검증은 이유와 함께 보고한다. Git 반영은 운영 명세의 경계를 따른다.
