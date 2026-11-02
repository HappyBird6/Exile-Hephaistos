# 수동 작업대 베이스 매핑 체크포인트

- 기준: `abfaffb504ab791e20882afda245bfc523a5128b` (원격 master 읽기 확인).
- 작업 브랜치: `codex/manual-workbench-20261102`.
- [134개 베이스 목록 및 제외 보류 근거](../current/manual-workbench-checkpoint.md).
- 이 기록은 작업대 전체 출시 완료나 모든 게임 아이템 가져오기 지원을 의미하지 않는다.

## 변경과 자체 리뷰

Solar 전용 붙여넣기 연결을 WorkbenchService의 실제 검토 카탈로그 목록으로 확장했다.
정확한 이름과 아이템 종류가 일치해야 하며 Shrine Sceptre처럼 같은 이름의 여러 베이스는
선택하지 않는다. UI도 같은 베이스의 카탈로그와 아이템 레벨을 읽고, 기존 기록 검증기를
통과한 상태만 저장한다. 암묵 옵션이 없는 베이스도 허용한다.

기존 분열 잠금과 단일 수치의 단위 검증을 유지한다. 미해석 행, 품질·소켓 등 추가 속성,
훼손·미공개 표시는 원문을 남기고 변환 전체를 차단한다. 이를 일반 옵션이나 빈 상태로
바꾸지 않는다. 자동 제작 탐색 제한, 저장 스키마, 기존 카탈로그 및 하위 베이스 기록은 변경하지 않았다.
6개 언어의 제작 활성화 문구에서 Solar 한정을 제거했다.

자체 리뷰에서 실제 diff와 원문/상태 보존, 모호성 거부, 초기 카탈로그 선택, 저장 전 검증을
대조했다. 독립 리뷰는 수행하지 않았다.

## 검증

- `npm ci`: 잠금 파일 그대로 설치 성공. 최초 샌드박스 DNS 실패 후 정상 권한에서 실행.
- `npm run typecheck`, `npm run lint`, `npm run format:check`, `npm run build`: 통과.
- 최종 UI 테스트 수정 후 typecheck 및 해당 파일 ESLint/Prettier도 통과.
- `npm run test -- --run`: 126개 파일, 2,400개 테스트 통과.
  최초 샌드박스 실행은 임시 모듈 파일 ENOENT로 테스트 본문 실행 전 실패했다.
  같은 소스로 정상 권한에서 재실행한 위 결과가 유효한 회귀 결과다.
- Java 21 임시 컨테이너: `spotlessApply test --tests '*SolarTextMapperTest'
  --tests '*RegisteredTextMapperTest' --tests '*WorkbenchControllerTest'
  --tests '*ArchitectureTest' bootJar --continue`: 26개 테스트 통과, 빌드 성공.
- Compose `config --quiet`: 통과. 기존 환경 파일 내용은 출력·복사·변경하지 않았다.
- 전체 `check generateJooq bootJar --continue`: 단위 테스트 964개 실행. 최초 사본에서
  `contracts` fixture 누락으로 44개 실패했다. 공통 계약 자료를 포함해 관련 81개를 재실행했고,
  이 결과를 반영한 실패 40개는 기준 master 결과의 테스트 이름·오류 타입·메시지와 동일하다.
  새 실패와 기존 실패 메시지 변화는 모두 0개다. 전체 명령의 종료 코드는 기존 테스트 실패로 1이다.
- 통합 테스트 8개 통과, `spotlessCheck`, `generateJooq`, `bootJar` 성공.
- 비교 근거: 작업 폴더의 `backend-full-results`, `backend-contracts-results`,
  `backend-full.log`, `backend-contracts.log`, `validation-summary.json`.
  기준 자료: 2026-10-29 작업 폴더 `master-final-results` (단위 961개, 실패 40개; 통합 8개).
- 모든 전용 임시 컨테이너와 Testcontainers 자원의 종료·정리를 확인했다.

## 미완료 범위

1. 동일 계열 상·하위 관계의 명시적 근거 확보와 최종 신규 선택 목록 확정.
2. 일반 게임 복사 텍스트의 장비 속성·복합 옵션·스킬 및 품질·소켓의 무손실 변환.
3. 훼손된 공개 옵션·미공개 후보 상태를 수동 작업대의 상태/저장 경로에 연결.
4. 실제 브라우저에서 전체 가져오기→제작→저장→복원, 반복·취소·실패 보존 검증.

현재 UI 회귀 검사는 Solar 및 Soldier의 각각의 카탈로그로 가져오기, 원문 유지, 저장 상태를
검증한다. 실제 브라우저 종단 검증으로 표현하지 않는다. 운영 서버 재시작, 배포,
master 반영, 원격 push, 기존 DB·볼륨·사용자 저장 파일 변경은 수행하지 않았다.
