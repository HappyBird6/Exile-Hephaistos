# 제작 경로 v1 로컬 통합 검증

## 기준과 제품 연결

제품 `15f02631`, 계약 `b806a0b`, FE `e1d393c`, BE `8b0c521`을
`crafting/path-search-integration-v1`에서 merge했다. 공통 계약을 cherry-pick하지 않았으며
각 원본 commit을 ancestor로 보존한다. 다른 worktree와 18090 서비스는 수정하지 않았다.

`CraftStart`가 기존 `ConnectedGoalFilter`의 숫자 AST, full item, 현재 provenance를
시작 클릭 시 함께 복사한다. GoalFilterPanel의 입력 유효성과 서버 validation도 시작 조건이다.
숫자 입력이 미완성이거나 goal catalog/ruleset이 달라지면 이전 AST로 실행하지 않는다.
설정 편집, 비활성화, omen 변경, 데이터 갱신은 이전 PathTree를 해제한다.
PathTree의 기존 서버 cancel 및 늦은 응답 epoch 처리를 사용한다.
목표 AST, source snapshot, 실제 roll을 새 값으로 자동 변환하지 않는다.

기존 3열 시작 화면, rarity/item level/tier, 항상 표시하는 추가 입력, AND/OR와 6언어는 유지한다.
production은 실제 HTTP adapter만 사용하며 테스트 fixture를 제품으로 import하지 않는다.
개발 정책 입력이나 진단 메뉴를 추가하지 않았다.

## 지원 경계

Solar Rare, nonfractured explicit 1개, quality/conditions/omens 없는 시작 상태에서
기존 AND/COUNT(OR) 숫자 목표를 계산한다. 여섯 실제 정책 후보를 비교하고 UI에는 최대 다섯 개를 표시한다.
100/300/500은 화폐 사용 횟수 기준 first-hit CDF다. 정책별 renewal 증명이 완료된 경우에만
완료 확률과 후보군 내부 순위를 표시한다. 전역 최적을 주장하지 않는다.
표시 graph가 PARTIAL이어도 확률 계산은 전체 roll을 검사하며, 누락된 표시 분기를 0으로 취급하지 않는다.
복구는 failure full item에서 이전 checkpoint로 돌아가는 별도 조건부 job이다.
본 목표 확률이나 순위에 복구를 합산하지 않는다. 다른 상태·효과는 짧은 미지원 안내를 표시한다.

## 소켓 없는 재현

Frontend는 기존 pinned 이미지의 npm ci layer를 재사용한다.

```powershell
docker build -f frontend/src/features/crafting/path-tree/Verify.Dockerfile -t exile-path-integration-verify:v1 .
docker run --rm --network none exile-path-integration-verify:v1 sh -c 'npm run lint && npm run typecheck && npm run format:check && npm run test -- --run --maxWorkers=1 && npm run build'
```

Backend의 소켓 없는 범위는 `backend`에서 다음이다.

```powershell
./gradlew.bat spotlessCheck test bootJar
```

실제 HTTP acceptance는 테스트 전용 `PathSearchRuntimeHarness`를 사용한다.
제품 Controller, RulesetBoundary, error advice, Service, catalog, 숫자 evaluator, 비동기 executor를 사용한다.
DB를 요구하지 않는 MockMvc를 loopback HTTP로 연결하며 제품 Spring context 전체 기동을 대체하지 않는다.
제품 bootstrap과 같은 WorkbenchDefinitions 초기화를 수행한다. 테스트 work budget 777은
실제 비동기 pause를 재현하기 위한 것이며 제품 기본 예산 50,000은 변경하지 않는다.
fixture source의 snapshot은 별도 current-snapshot 테스트 입력에만 명시적으로 binding한다.

```powershell
# backend 작업 디렉터리; 승인된 Java 21 환경에서 실행
./gradlew.bat -I ../scripts/path-search-runtime.gradle pathSearchRuntime
# 같은 loopback network의 frontend 작업 디렉터리
npx vitest run --config path-search-runtime.config.ts
```

컨테이너 실행 시 source는 전용 컨테이너 내부로 복사하고 Gradle cache를 재사용한다.
Java 컨테이너는 `--network none`, FE acceptance 컨테이너는
`--network container:<전용 Java 컨테이너>`로 같은 loopback을 사용한다.
host 포트, Docker socket, 운영 DB/Redis/volume을 연결하지 않는다.
HTTP acceptance 결과는 해당 FE 컨테이너 `/tmp/path-search-runtime/result.json`에 남긴다.
테스트 스크립트와 harness는 제품 artifact에 포함되지 않는다.

## 확인한 증거 (2026-10-10)

- Frontend 전체: 88 files / 2,108 tests 통과, 실패/skip 0. 동일 lockfile의 npm ci cache 재사용.
  마지막 데이터 변경 안내 조건 정리 후 직접 영향 4 files / 36 tests를 추가로 통과했다.
  최종 소스의 lint/typecheck/format:check/build도 통과했다. acceptance 코드도 TypeScript strict 대상이다.
  기존 큰 bundle에 대한 Vite 500 kB 경고는 남는다.
- Backend unit/ArchUnit: 604건, 실패 0, skip 0. `test bootJar` 통과.
- 공유 계약: 정상 17개, 의도적 거부 6개와 binary/Solar oracle 통과.
- Backend DTO/실행 JSON Schema: 23개 payload 통과.
- 실제 HTTP + production adapter + PathTree DOM acceptance: 1건 통과, 테스트 본문 약 8.5초.
  여섯 후보, 51 full item states, 153 executions, graph 5 pages를 확인했다.
  ordinary Chaos의 0/1/2/100/300/500 CDF가 `650/21107` 독립 정수 oracle와 일치했다.
  실제 비동기 pause/cancel/resume의 recommendations와 rankings가 연속 결과와 같다.
  실제 HTTP read를 지연 전달해 cancel acknowledgement 뒤의 옛 응답이 폐기되는 것을 확인했다.
  stale revision, 삭제 stat/modifier, catalog version과 ordinary source snapshot 거부도 확인했다.
  복구 결과는 conditional=true/includedInMain=false이며 부모 snapshot은 동일했다.
  실제 완료 응답을 PathTree에 넣어 후보 버튼 5개와 100/300/500 전환을 확인했다.
- 기존 default 50,000 work 실행도 실제 HTTP에서 Solar CDF/6개 후보 완료를 확인했다.
  취소 비교는 빠른 revision 진행으로 생기는 비결정성을 없애기 위해 위 777 work harness에서 수행했다.

기존 테스트 fixture가 새 provenance/catalog 요청을 제공하지 않아 발생한 연결 회귀를 보완했다.
새 snapshot 테스트의 텍스트 선택자는 실제 접근성 label로 수정했다. 확률 기대값, skip,
기존 테스트 timeout은 변경하지 않았다. 최초 harness의 metadata 초기화 누락도 바로잡았다.
진단용 stack trace는 일회용 컨테이너 복사본에만 사용했고 최종 repository/검증 소스에는 없다.

## 남은 통합 검사와 화면 QA

Docker socket mount는 사용자 승인 대기 중이므로 우회하지 않았다.
승인된 격리 환경에서 아래 필수 검사를 완료해야 한다.

```powershell
cd backend
./gradlew.bat check generateJooq bootJar
```

이는 Testcontainers의 임시 PostgreSQL/Redis 및 migration 기반 codegen을 포함한다.
이번 소켓 없는 검사나 MockMvc HTTP bridge를 이 전체 검사 통과로 해석하면 안 된다.
Compose/Windows 실행 스크립트는 변경하지 않았다. 기존 DB/volume을 재사용해 검사하지 않는다.

이번 세션의 브라우저 inventory는 비어 있었다. IAB 실제 layout/모바일 QA는 미실행이다.
서버 변경 승인 후 통합 배포에서 desktop/mobile 3열 재배치, 6언어 긴 문구,
키보드·focus 복귀, 실제 후보 5개와 관측점 전환, 단계별 graph pagination,
취소/재개 중 편집, 별도 복구, catalog/ruleset 변경 후 무효화를 확인해야 한다.
현재 18090은 교체하지 않았다. 원격 push와 master 병합도 수행하지 않았다.

자체 리뷰만 수행했다. 실제 브라우저 화면 및 전체 Spring/DB 환경에 대한 독립 QA를 권장한다.
