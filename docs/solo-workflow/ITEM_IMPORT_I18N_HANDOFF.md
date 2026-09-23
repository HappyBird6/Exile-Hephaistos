# 아이템 입력 API·Frontend·i18n 인계

- 작업: 파싱 서비스 필요 클래스 정리, Frontend 텍스트 입력→아이템 정보 표시, 영어/한국어 i18n 및 언어 확장 구조.
- 상태: 구현·통합 검증·독립 리뷰 완료. 사용자 지시대로 local master에 commit. Push 및 기존 Docker 이미지 재배포는 하지 않음.
- 관리자: 현재 작업. 내부 작업자 item_api(backend 및 API 문서), item_ui(frontend 및 관련 문서). 동일 worktree에서 순차 쓰기. 독립 리뷰어 item_integration_review(읽기 전용).
- 작업 경로: C:\SSAFY\PYJ\새 폴더\Exile-Hephaistos
- 기준 SHA: 76957526f3d4d5cf8c8e5dbe12e174deec37a2a4
- Backend commit: e56144d31e9fdec1a0550e8eac823b00b9570c71
- Frontend 및 통합 리뷰 대상 commit: 44686ff48ee20f90d897684fb6893b4e1787e5e5
- 변경: item.api.ItemTextModels 공개 불변 DTO, 읽기 전용 익명 POST /api/v1/items/parse, 400/422/413 Problem Details, 정확한 POST만 CSRF 제외. 관리자 보안 유지. OpenAPI 문서 추가.
- Frontend: 이름/종류/등급/base/level/요구사항/속성/미해석 행/경고/원문 표시. TanStack Query가 결과, Zustand가 입력 소유. AbortController·revision·응답 원문 대조로 오래된 결과 차단.
- i18n: en/ko locale resource, typed key 계약, 언어 registry, 화면/관리자/aria/오류/알림/화폐 이름 번역. 언어 코드만 저장하고 document.lang 갱신. 아이템 원문은 변환하지 않음.
- 사용자 기존 Item.java/Modifier.java/ItemState.java 초안과 item/api/ItemState.java 삭제는 보존하고 commit에서 제외. 작업 중 사용자 초안이 주석 처리된 현재 상태에서 전체 backend source 검사. source 제외 없음.

## 검증 증거

- Java21: C:\Users\SSAFY\.codex\visualizations\2026\09\21\01a0c22b-e82d-7df3-be69-bfbddafce945\toolchains\jdk-21.0.12.1+1
- Backend: JAVA_HOME을 위 경로로 지정, GRADLE_USER_HOME은 사용자 .gradle. backend/gradlew.bat -p backend -I %TEMP%/item-api-validation.gradle check --offline. unit61 + integration9 =70건 모두 통과. parser/API/기존 admin security/ArchUnit/임시 PostgreSQL·Redis 포함. 소유 Java 파일 Spotless 통과.
- 임시 init: ASCII buildDir %TEMP%/poe2-item-api/build와 Spotless 대상만 지정. main/test 소스 제외 없음. 결과 XML test-results/test 및 test-results/integrationTest를 관리자가 직접 확인(실패·오류·skip 0). bootJar 추가 성공.
- Frontend: npm ci --ignore-scripts (lock 유지), npm run lint, npm run typecheck, npm run format:check, npm run test -- --run (32건), npm run build 모두 통과. git diff --check 통과.
- 독립 리뷰: e56144d + 44686ff의 API 계약·보안·원문 보존·stale response·i18n·회귀 테스트 검토, actionable findings 없음. 리뷰어는 읽기만 수행.
- 실제 브라우저 통합 확인: 별도 Vite127.0.0.1:15173→별도 JVM127.0.0.1:18080. 기존 로컬 DB 설정은 출력하지 않고 사용, Flyway 비활성/DDL validate/관리자 및 수집 비활성. 기존 Docker 컨테이너 재시작 없음.
- 실제 UI 결과: 영문 synthetic 텍스트→이름/base/level42/요구사항/표식옵션/미해석 행 표시; 영어 UI로 전환 후 원문과 결과 보존; 한국어 synthetic 텍스트→같은 정보 및 타락 표기; 잘못된 입력422→이전 결과 숨김과 영어 오류; 한국어로 전환→오류 문구도 한국어; 관리자 페이지 이동 후 언어 유지 및 영어 전환. 영어 레이아웃 screenshot 시각 확인. 검증 서버 두 개와 agent 브라우저 탭 종료.

## 남은 범위와 실행

- 실제 catalog ID/tier/접두·접미/roll 검증 및 계산용 ItemState 변환은 아직 미지원. 일반 옵션·한국어 세부 property 등 미해석 정보는 원문과 경고로 보존.
- 기술 명세의 OpenAPI DTO 자동 생성은 기존 admin 방식처럼 수동 DTO+runtime validation으로 대응 중. docs/frontend-i18n.md에 후속 TODO 명시.
- AntPathRequestMatcher deprecation 경고 존재(기능·보안 테스트 통과).
- 실행 중 기존 Docker 화면에 새 이미지 반영: 프로젝트 루트에서 .\scripts\dev.ps1 stack. 현재 작업에서 기존 운영 중 개발 stack을 교체하지 않음.
- 새 언어 추가 방법: docs/frontend-i18n.md. API 계약: docs/openapi-item.yaml.
- 리뷰 후 수정 횟수: 0. 남은 차단 결함 없음. branch 이동 및 push는 후속 사용자 지시.
