# c740b66f 후속 검토 수정 및 재검증

검토 기준은 `c740b66fc8cb97d52add7db9767efa3aea5b7623`, 최초 구현 전 기준은 `11c145ae69cb005774f7fc1f08bb8d3584f5317a`다. 작업 시작 시 `codex/automatic-policy-candidates`, HEAD c740b66f, 깨끗한 작업 트리와 AGENTS.md를 확인하고 같은 원격 브랜치를 fetch했다. 범위를 넓히거나 목적함수·제작 제약·확률 모델을 변경하지 않았다.

## 네 가지 수정

### 중간 관측의 하한

`AutomaticMethodGraph.evaluationPoint()`가 관측을 세 가지로 구분한다.

1. 이미 발행된 관측: 저장된 세션 정책 결과를 사용한다.
2. 목표 관측 횟수의 boundary 또는 absorbed boundary에 도달했지만 아직 미발행: 완성 분포에 희귀 정책을 적용한 뒤 사용한다.
3. 아직 미완성: 희귀 종료 대상이 될 수 있는 CHECKPOINT/ACTIVE 질량은 UNRESOLVED로 옮긴다. 이미 발견된 후속 노드에도 이 질량을 전달하지 않는다. 확실한 목표 결과와 실패는 유지하며, 부분 분포만으로 REBASE를 확정하지 않는다.

정책 평가와 화면에 공개하는 중간 관측이 같은 처리를 사용한다. 사용량과 전체 질량은 유지한다. `AutomaticGraphReviewTest`는 알려진 성공률 1인 S, S에 1/128·실패에 127/128로 가는 M으로 반례를 구성한다. 1%/5% 설정의 최종 성공률 0과, 그룹 한도를 0.1%로 낮춰 희귀 종료에서 제외한 최종 성공률 1/128을 모두 부분 평가 구간이 포함한다. 완성 미발행·발행 이후 결과도 비교한다.

### 제한된 cursor의 실행 슬롯

`AutomaticMethodGraph.advance()`는 resourceLimited 실행을 먼저 제외하고 진행 중 슬롯과 실행 후보를 계산한다. 제한된 cursor의 계산 상태는 보관하되 다른 메서드의 슬롯을 점유하지 않는다. 우선순위와 FIFO 모두 실행 가능한 집합에서 선택한다. 나머지 작업까지 마치고 진행 가능한 실행이 없으면 제한 상태로 전환한다.

회귀 검사는 넓은 분포를 가진 A/B의 한도를 1로 설정하고 작은 C가 완료되는지 확인한다. 이후 한도를 1000으로 올리면 A/B가 재개되고 각 메서드의 재화 사용량은 정확히 1, 질량도 1이다. 기존 예산 증액 시 cursor 재생성 계약을 유지한다.

### 구형 flow의 기본 정책

`restoreFlowPolicy()`는 저장 flow에서 생략된 설정을 disabled로 해석한다. `automaticApi.create()`의 생략 기본값도 disabled다. 신규 flow만 기존 enabled 기본값이나 사용자의 명시적인 선호를 전달한다.

구형 flow에 서버 작업이 남아 있는 경우에는 기존 결과를 읽고, 저장 작업 포인터가 없는 경우에는 disabled를 명시해 생성한다. 저장 포인터가 가리키는 작업이 404인 경우는 기존 만료 안내를 유지하며 enabled 기본값으로 자동 재생성하지 않는다. 신규 flow는 enabled 기본값을 명시하는 별도 검사도 있다.

### 취소 확인과 실패 재시도

`cancelAutomatic()`은 CANCELLED·ENDED·FAILED를 반환한다. COMPLETE/PARTIAL 및 취소 완료를 구분하며, 실패를 정상 완료처럼 반환하지 않는다. revision 충돌은 최신 조회 후 최대 두 번 시도하고 그래도 실패하면 FAILED다.

새 세션 전환은 취소 확인 또는 이미 종료된 작업에서만 수행한다. 실패하면 기존 세션에 남아 8개 언어로 오류를 표시하고 같은 버튼으로 재시도할 수 있다. 실패 시 세션 종료 기록·정책 선호 변경·새 요청을 만들지 않는다. 기존 STOP 처리도 실패를 오류로 표시하도록 새 결과 타입에 맞췄다. 이전 요청에서 늦게 도착한 취소 완료의 mounted/token 검사도 유지한다.

API와 React 검사는 네트워크 오류, HTTP 500, 연속 revision 충돌을 각각 확인한다. 세 경우 모두 이전 READY 작업이 취소됐다고 오인하거나 새 세션으로 이동하지 않고, 명시적인 재시도 성공 후에만 전환한다.

핵심 수정은 `b451cabe`, `e92915bb`, `a0b0aff8`에 나눠 commit하고 같은 원격 브랜치에 push했다. 백엔드의 두 새 반례 검사는 수정 전 c740b66f에서도 직접 실행해 둘 다 실패함을 확인했다([기록](baseline-regressions.json)).

## 성능 재측정

동일 머신, JDK21, Gradle8.14.3, 동일 실제 Solar amulet NORMAL ilvl82/life>=1, 동일 그래프 한도와 희귀 정책 1%/5%로 c740b66f와 수정본을 순차 측정했다. 다른 전체 검사 시작 전에 측정했다. `AutomaticRequestLatencyTest`는 각 run마다 별도 로컬 저장 디렉토리를 사용한다. 카탈로그 fixture 초기화는 시간 밖이고, 요청 생성의 runtime/kernel 준비·저장, 진행·평가, 응답 DTO와 JSON 인코딩은 시간 안이다. HTTP 전송과 브라우저 렌더링 시간은 포함하지 않는다.

생성 후 READY 동안 ADVANCE하여 종료하고 GET 30회를 수행한다. 첫 후보는 API가 비어 있지 않은 후보 집합을 최초 반환한 시점이다. 성공 하한이 양수인 후보의 최초 반환 시점도 별도로 기록했다. 4회 중 첫 실행을 제외한 3회 중앙값과 첫 실행을 함께 공개한다. 시간 단위는 ms다.

| 구간 | 기준 첫 실행 | 수정 첫 실행 | 기준 warm 중앙값 | 수정 warm 중앙값 |
| --- | ---: | ---: | ---: | ---: |
| 생성 | 529.847 | 615.329 | 215.677 | 183.774 |
| 첫 후보까지 누적 | 987.202 | 1047.125 | 499.536 | 433.460 |
| 양수 성공 하한 후보까지 누적 | 1450.168 | 1444.215 | 499.536 | 433.460 |
| 전체 진행 요청 | 919.940 | 828.710 | 283.858 | 247.146 |
| 30회 조회 | 1314.617 | 1339.748 | 1112.711 | 971.124 |
| 생성+진행+조회 전체 | 2764.816 | 2783.996 | 1603.702 | 1389.918 |

첫 실행 전체는 **19.180ms 증가**했다. 생성은 +85.483ms, 조회는 +25.132ms, 진행은 -91.230ms였다. 첫 후보 반환은 +59.923ms였지만 양수 성공 하한 후보의 반환은 5.953ms 빨랐다. warm run 중에도 진행 구간이 각각 4.442ms, 5.531ms 늘어난 run이 있다. warm 중앙값이 줄었다고 모든 구간이나 모든 입력이 빨라졌다고 주장하지 않는다.

두 버전 모두 최종 COMPLETE, operations=105, primitive 호출 102/결과 2187, 상태 33/메서드 3/관측 전이 행 352다. 첫 실행 ADVANCE 2회, warm 각각 1회다. warm 한 run의 모든 응답 JSON 합계는 기준 43,521,403 bytes, 수정 43,521,527 bytes다. 조회 구간에는 큰 응답의 JSON 인코딩과 저장 문서 읽기도 포함된다. 내부 responseNanos만 조회 전체 시간으로 해석하지 않는다.

앞선 최초 구현 보고서의 tiny fixture에서는 조회 시간이 65.935→79.237ms로 늘었다. 그 기록을 삭제하거나 이번 다른 fixture의 결과로 대체하지 않았다. 이번 비교는 **c740b66f 대비 네 문제 수정본**의 동일 입력 비교이며, 원래 11c145ae 대비 최초 구현 전체의 속도 보장을 뜻하지 않는다.

[측정 전 원본](latency-before.xml), [측정 후 원본](latency-after.xml). 재현 명령:

```sh
AUTOMATIC_BENCHMARK=true ./gradlew test --tests '*AutomaticRequestLatencyTest'
```

c740b66f worktree에는 현재 benchmark 파일만 복사해 같은 명령으로 실행한다. 측정에서는 진단용 timeout/fork 설정을 사용하지 않았다.

## 전체 검사와 실패 분류

전체 unit/ArchUnit 및 Docker integration을 두 버전에서 각각 실행하고, 수정본은 generateJooq·bootJar·spotlessCheck도 실행한다. 각 버전의 작업 저장 디렉토리를 서로 다른 임시 경로로 지정한다. 원래 파일/DB를 삭제하지 않는다.

이전 전체 실행은 오래 걸리는 정확 분수 계산에서 중단돼 뒤의 테스트 결과가 없었다. 이번 진단 실행은 [full-suite.gradle](full-suite.gradle)을 명시적으로 적용해 기본 timeout 30초, class별 새 JVM, 병렬 worker 2개로 실행했다. 기존 명시적 `@Timeout`은 그대로 우선하며 StrictExaltedScenarioTest의 90초 제한도 유지된다. 이는 제품 계산 한도나 assertion 변경이 아니다. 시간 초과를 통과나 기대된 실패로 바꾸지 않는다. 시간 제한에 걸린 검사는 별도로 분류하며 정확성 검증 완료로 간주하지 않는다.

실행 명령:

```sh
CRAFTING_METHOD_JOB_DIRECTORY=/tmp/<version-specific-jobs> ./gradlew \
  -I <full-suite.gradle> check generateJooq bootJar --continue
```

### 백엔드 결과

| 범위 | 현재 통과 / 실패 / 생략 | c740b66f 통과 / 실패 / 생략 |
| --- | ---: | ---: |
| unit 및 ArchUnit (1,686개) | 1,581 / 86 / 19 | 1,581 / 86 / 19 |
| Docker 통합 (35개) | 33 / 2 / 0 | 33 / 2 / 0 |
| 합계 (1,721개) | 1,614 / 88 / 19 | 1,614 / 88 / 19 |

19개 생략은 명시적으로 켜야 하는 성능 검사다. 이번에 정확성 검사를 새로 skip하지 않았다. c740b66f에는 두 새 회귀 검사와 동일 성능 검사 파일만 복사했고 제품 코드는 변경하지 않았다. 두 회귀 검사는 기준에서 실패하고 현재에서 통과했다. 반대로 아래 두 검사는 기준에서 통과했으나 현재에서는 진단 시간 제한에 걸렸다. 따라서 총계가 같아도 각 검사 결과가 같은 것은 아니다.

| 현재 실패 분류 | 건수 | 근거 |
| --- | ---: | --- |
| c740b66f에서 재현 | 78 | 같은 테스트·실패 타입·테스트 코드 위치에서 실패. unit 76, 통합 2 |
| 환경 및 시간 제한 | 10 | 시간 초과 9, 검사용 저장 경로 환경변수 1 |
| 새 변경에 의한 확정 실패 | 0 | 새 assertion 실패는 관측하지 않음. 시간 초과 원인이 회귀인지까지 배제한 것은 아님 |
| 미분류 | 0 | 실행된 현재 실패 88건 모두 위 범주에 배정 |

실패별 이름, 오류, 코드 위치, 기준 결과는 [failure-classification.json](failure-classification.json)에 기록했다. [현재 전체 목록](current-tests.json)과 [기준 전체 목록](baseline-tests.json)은 통과·생략도 포함한다. parameterized test의 같은 표시 이름이 여러 메서드에서 쓰이는 경우 스택의 테스트 메서드 위치로 구분했다.

재현된 78건에는 검토 카탈로그/장비 fixture, 구형 저장 모델, 기존 정확 계산/경로 계약 검사들이 포함된다. 통합 실패는 `InfrastructureIntegrationTest`의 migration 개수 4 기대/11 실제, `GoalFilterIntegrationTest`의 HTTP 404 기대/409 실제다. 범위 밖의 모델·fixture·기대값을 수정하지 않았다.

시간 초과 9건은 다음과 같다. 이름을 줄여 적었으며 정확한 메서드명과 오류는 실패별 목록에 있다.

| 검사 | c740b66f 결과 |
| --- | --- |
| StrictExaltedScenarioTest.onlyFirstMutableSlotOrWitnessedSideDummyIsAllowed | 시간 초과 (명시적 90초) |
| TerminalBranchingScenariosTest.fourGoalsKeepPartialEssenceStatesWithoutInventingFinishingExalt | 시간 초과 |
| TerminalContinuationApiTest.cancelsResumesAndReloads… | 시간 초과 |
| CertifiedJobRestartTest.continuedRunnerMatches… | 시간 초과 |
| CertifiedMethodSearchEngineTest.completeSmallMap… | **통과** |
| DeferredCalculationTest.jointFamilyCells… | 시간 초과 |
| DeferredCalculationTest.internalOrbitMixtures… | 시간 초과 |
| DeferredProductionTest.realSolarCells… | **통과** |
| PathSearchServiceTest.solarOracle… | 시간 초과 |

`BackendSettingsTest.springAndDirectConstructionUseTheSameShippedDefaults`는 진단 실행의 `CRAFTING_METHOD_JOB_DIRECTORY`가 Spring 설정에 반영되지만 직접 구성한 기본값에는 반영되지 않아 양쪽에서 실패했다. 이 환경변수 없이 해당 클래스 5개를 재실행해 모두 통과했다([기록](settings-retry.json)). 시간 초과 9건은 완료된 정확성 검증으로 계산하지 않는다. 특히 기준만 통과한 두 건은 성능 변동과 회귀를 이 자료만으로 구별하지 못한다.

최초 구현 전 `11c145ae`에서도 관련 실패를 직접 선별 재현했다. [첫 묶음 15개](original-selected-tests.json)는 1 통과/13 assertion·요청 거절 실패/1 시간 초과, [활성 엔진 14개](original-engine-tests.json)는 10 통과/4 실패였다. 후자는 tier 공유, chaos preparation, changing chaos law, finite 100 strategy 검사를 포함한다. 이 29개만 직접 재실행했으므로 c740b66f에서 재현된 78건 전부가 11c145ae에서도 재현됐다고 주장하지 않는다.

현재 전체 명령은 `test`와 `integrationTest` 실패로 실패 종료했다. `--continue`로 `spotlessCheck`, `generateJooq`, `bootJar`는 성공했다. generateJooq는 migration을 적용한 임시 DB를 사용했다. 기준 전체 실행은 `test integrationTest --continue`였다. 진단 설정을 사용하지 않은 직접 영향 범위 68개(새 회귀 및 ArchUnit 포함)는 통과했다. 모든 백엔드 검사가 통과한 상태는 아니다.

### 프런트엔드 결과

`npm ci`, lint, typecheck, format:check, build가 통과했다([종료 코드](frontend-checks.json)). build의 기존 큰 chunk 경고는 남아 있다. 자동 탐색 UI/API, flow, scenarioCode 직접 영향 범위 4개 파일 **81개 검사는 모두 통과**했다.

전체 실행은 3,513개 중 3,493 통과/20 실패였다. 이 실행은 백엔드 전체 검사와 동시에 진행됐다. 20건 중 새 세션 검사 4건에서는 이전 검사의 localStorage가 남는 fixture 격리 문제를 발견해 `afterEach`에서 정리했다. 제품 동작이나 assertion·timeout을 완화하지 않았다. 이후 직접 영향 범위 81개를 다시 모두 통과했다.

전체 실행의 실패 20건을 기존 timeout 그대로 단일 worker로 다시 선택 실행해 15 통과/5 실패였다. 남은 5건은 기준 c740b66f에서 모두 통과했고, 백엔드 부하가 사라진 현재 코드에서 같은 5건을 다시 실행해 모두 통과했다. 마지막 5건은 FeedbackTooltip, StashBaseSelection, WorkbenchLayout, ChaosRepeatBatchPage의 기존 UI 검사다. [재실행 기록](frontend-retries.json)에 실패와 각 재실행을 함께 보존했다. 일부 실행의 pending 수는 이름 필터로 선택하지 않은 테스트이며 새 skip이 아니다.

마지막 상태에서 전체 3,513개를 한 번에 다시 실행하지는 않았다. 따라서 전체 suite의 단일 실행 성공으로 표기하지 않는다. 관측된 실패 20건은 최종 수정본에서 개별 재실행을 통과했지만, 기존 UI 검사의 부하/타이밍 민감성은 남아 있다.

## 변경 파일과 남은 범위

- `AutomaticMethodGraph.java`: 미발행/부분 관측 구분과 안전한 평가 입력, 제한 cursor를 제외한 진행 가능 메서드 선택.
- `AutomaticGraphReviewTest.java`: 1/128 하한 반례와 A/B 제한 중 C 완료·예산 증액 회귀.
- `automaticApi.ts`, `craftingFlow.ts`, `policyCandidates.ts`: 구형 flow의 disabled 의미, 취소 결과 구분.
- `AutomaticMethodSearch.tsx`, `PolicyCandidates.tsx`, `policyCandidateMessages.ts`: 취소 확인 뒤에만 세션 전환, 오류와 재시도, 8개 언어 안내.
- `AutomaticMethodSearch.test.tsx`, `automaticApi.test.ts`: 복원·새 flow·취소 오류·충돌·재시도 검증.
- `AutomaticRequestLatencyTest.java`: 명시적으로 켜는 실제 카탈로그 생성/진행/조회 측정.
- 현행 세션 정책 문서 및 이 보고서: 수정 계약, 원본 측정, 실패 분류.

이번 후속 작업은 요청한 네 문제와 검증에 한정했다. 기존 희귀 기준(동등 결과 그룹의 조건부 확률과 행위별 총 REBASE 한도), 목표 우선 판정, 확률 질량, 기대 재화, 상위 정책 후보의 성공 확률 목적함수와 평가 범위를 확장하거나 바꾸지 않았다. REBASE 이후 새 세션의 전역 최적성은 보장하지 않는다.

미완료 검증은 시간 초과 9건의 완료 결과와 전체 프런트엔드 단일 재실행이다. 기준에서도 실패하는 78건은 이 범위에서 해결하지 않았다. 브라우저 수동 E2E 및 HTTP 전송을 포함한 성능 측정은 수행하지 않았으며, 세션 전환은 API mock 및 React 통합 회귀로 검증했다. 실측 fixture 밖의 성능 개선을 보장하지 않는다.
