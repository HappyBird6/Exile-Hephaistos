# 제작지원 정책 후보 구현·검증 기록

이 문서는 `c740b66f`까지의 최초 작업 기록이다. 이후 발견된 네 가지 문제의 수정, 전체 실패 분류와 새 성능 측정은 [후속 검토 보고서](../automatic-policy-review/report.md)를 따른다.

## 기준과 실제 실행 경로

작업 시작 시 로컬 `work` 브랜치 HEAD는 `673510c7`, 작업 트리는 깨끗했다. 루트 AGENTS.md를 확인하고 원격 master를 fetch했다. `codex/automatic-policy-candidates` 전용 브랜치를 만든 뒤 원격 `11c145ae69cb005774f7fc1f08bb8d3584f5317a`로 fast-forward했다. 검토 기준 `2846ca4b`로 되돌리지 않았다.

활성 경로는 `CraftSupport → CraftStart → AutomaticMethodSearch → automaticApi → /api/v2/crafting/method-searches/automatic → AutomaticSearchService → AutomaticMethodGraph → GoalDirectedPlanner / AutomaticMethodExecution → AutomaticCraftingKernel → CertifiedOutcomeStrategy`였다. 기존 `view()`는 매번 strategy를 만들고 16회 refine했다. 동일 그래프의 30회 추가 조회도 이 작업을 반복했다. 활성 커널은 기존 BasicCurrencyTransitions.PrimitiveCursor를 사용하지 않고 결과를 먼저 전개했다.

엄격한 AutomaticSearchInputReader, 공통 ItemStateWireDefaults, 선택적 boolean 읽기 계약, 시작 베이스 상세 지연 요청, catalog generation/cache/429, 빈 목표 검증 억제는 유지했다. RAG/LLM/의존성/가격 가중치/전역 플레이어 재화 예산은 추가하지 않았다.

## 변경 단위와 주요 파일

| 커밋 | 주요 파일·동작 |
| --- | --- |
| `324588eb` | AutomaticSearchService/AutomaticMethodGraph: 평가 진행과 응답 생성 분리, revision·설정·버전별 evaluator 재사용, 계측. AutomaticSearchBenchmarkTest. |
| `2077e58c` | RareOutcomePolicy, ExactMethodRunner.Exit.REBASE, graph 종료 분류·저장 v18/구형 v17 호환, CertifiedOutcomeStrategy.choiceValues, AutomaticPolicyCandidates, 입력·저장 reader와 관련 테스트. 영향도 우선 스케줄링과 top-k bounds 가지치기. |
| `a7850b89` | AutomaticMethodSearch/PolicyCandidates/automaticApi/craftingFlow/scenarioCode: 정책 후보 카드·분기별 결정·확률·기대 소모량, 희귀 종료 설정, 새 세션 전환, 늦은 취소/응답 방어. policyCandidateMessages의 8개 언어. |
| `140e7e06` | AutomaticCraftingKernel/AutomaticMethodExecution: 결과 그룹 cursor를 활성 경로에 연결, 한 번에 64개 생성, 아직 완성되지 않은 law는 미계산 유지. cursor 복원/예산 재개 검증과 catalog benchmark. |
| `6b78d381` | AutomaticSearchService: 관련 없는 저장 작업의 미래 정책 버전이 새 요청 admission을 막지 않도록 header와 실제 정책 복원 구분. |

자세한 계약과 API 예시는 [현행 설계 문서](../../current/automatic-session-policies.md)에 있다.

## 희귀 판정과 후보 해석

새 UI 기본값은 `rare-conditional-groups-v1`, enabled=true, groupPartsPerMillion=10000(1%), maximumMethodPartsPerMillion=50000(5%)다. 화면 설정을 바꾸면 새 요청으로 계산한다. 구형 요청/저장 데이터의 필드 생략은 disabled다.

완전히 계산된 메서드의 조건부 결과를 기존 behaviorKey·목표 판정·품질 문맥으로 묶어 그룹 상한으로 판정한다. 같은 확률의 그룹은 함께 선택하거나 함께 남긴다. 목표 달성을 먼저 처리하고, 미계산/미지원은 UNRESOLVED로 남긴다. REBASE 확률과 그 행위의 사용량을 보존하며 재정규화하지 않는다. 5%는 한 메서드의 종료 한도다. 여러 단계 누적 REBASE는 더 커질 수 있고 후보별로 집계한다.

후보는 도달 가능한 상태마다 메서드를 지정하며 그 메서드의 모든 결과를 따라 다음 결정을 실행한다. 기본 3개이며 성공 하한/상한으로 정렬한다. 비용은 도달 확률로 가중해 별도 표시한다. 구간 중첩은 순위 미확정으로 표시한다. 미계산이나 순환 꼬리가 있으면 기대 사용량의 상한은 null이다. 이번 세션·지원 규칙·허용 정책·희귀 종료 설정 안의 평가이며, REBASE 이후 새 세션을 포함하는 전역 최적성을 뜻하지 않는다.

## 같은 입력의 전후 측정

같은 머신/JDK 21/Gradle 8.14.3, 같은 baseline commit·fixture·그래프 한도, 희귀 처리 disabled로 측정했다. 마지막 비교는 다른 테스트 작업이 끝난 뒤 순차 실행했다. 각 JVM의 첫 run은 준비 비용으로 제외했다. 짧은 측정의 변동이 커서 일반적인 속도 향상률이나 SLA로 해석하지 않는다. 초기 병목 확인 로그도 함께 보존했다.

반복 조회: 동일 tiny fixture에서 create 1회, ADVANCE 1회, GET 30회. 상태 3/메서드 2/관측 전이 행 8로 동일하다. 아래는 warm 3회 중앙값이며 시간은 ms다.

| 지표 | 변경 전 | 최종 변경 후 |
| --- | ---: | ---: |
| 평가기 생성 횟수 | 32 | 2 |
| refinement 횟수 | 512 (호출 구조로 확인) | 32 |
| 평가+refinement 누적 시간 | 5.570 | 1.042 |
| 30회 GET 전체 시간 | 65.935 | 79.237 |
| 응답 DTO 생성 계측 | 20.372 | 13.177 |
| 그래프 확장 시간 | 39.464 | 51.999 |
| 상태/메서드/관측 전이 행 | 3/2/8 | 3/2/8 |

평가 중복은 제거됐지만 이 fixture의 **조회 전체 시간은 개선되지 않았다**. 응답 계측의 변경 전 값은 평가 작업을 포함하고 변경 후는 분리되어 있으므로 순수 직렬화 성능 비교가 아니다. HTTP 전송·브라우저 렌더링은 측정하지 않았다. 초기 baseline 계측의 refinementIterations는 미연결 카운터여서 XML의 0을 실제 호출 수로 해석하면 안 된다. baseline primitive 카운터는 공유 커널의 누적치라 run 사이 차이를 사용해야 한다(각 요청 실제 2회/2결과).

실제 catalog: Solar amulet normal ilvl82, life>=1, 동일 graph fixture, 최대 2000 operations, 실제 105 operations. warm 2회 수치다.

| 지표 | 변경 전 run1 / run2 | 최종 변경 후 run1 / run2 |
| --- | --- | --- |
| primitive 생성 시간(ms) | 159.105 / 337.514 | 122.528 / 96.892 |
| 그래프 확장 시간(ms, primitive 포함) | 277.993 / 499.772 | 243.383 / 187.126 |
| 평가+refinement(ms) | 2.125 / 1.883 | 5.468 / 5.598 |
| primitive 호출 / 생성 결과 | 102 / 2187 | 102 / 2187 |
| 상태 / 메서드 / 관측 전이 행 | 33 / 3 / 352 | 33 / 3 / 352 |

변경 전 primitive가 그래프 확장의 약 57~68%를 차지해 지연 열거를 구현했다. 최종 평가 시간에는 신규 후보 평가도 포함된다. 결과 총수는 줄지 않았다. cursor의 목적은 한 번에 전부 아이템으로 만드는 비용을 분할하는 것이며, 모든 문제가 위 수치만큼 빨라진다는 뜻은 아니다. 별도 512결과 완전분포 테스트에서 3개만 생성하고 나머지 509/512를 미계산으로 유지하는 것을 확인했다.

최종 원본: [조회 전](repeated-read-before-final.xml), [조회 후](repeated-read-after.xml), [catalog 전](catalog-before-final.xml), [catalog 후](catalog-after.xml). 초기 측정과 cache-only 단계는 나머지 XML에 보존했다. [baseline 계측 patch](baseline-instrumentation.patch)는 계산 의미를 바꾸지 않고 세 파일의 시간·횟수만 계측한다.

재현: 별도 worktree의 `11c145ae`에 계측 patch를 `git apply --unidiff-zero`로 적용하고 현재 AutomaticSearchBenchmarkTest 및 AutomaticPolicyPerformanceTest를 복사한다. baseline의 catalog test에서 없는 `graph.policyCandidates(3)` 호출만 제외한다. 전후 각각 아래 선택 테스트를 실행한다. 결과 XML의 system-out에 수치가 남는다.

```sh
AUTOMATIC_BENCHMARK=true ./gradlew test \
  --tests '*AutomaticSearchBenchmarkTest.benchmark' \
  --tests '*AutomaticPolicyPerformanceTest.measuredCatalogGraph'
```

## 검증 결과

최종 변경 영역 backend 63개 + ArchUnit 3개, 총 **66개 통과**. spotlessCheck와 bootJar도 통과했다. [선택 검사 결과 목록](focused-test-results.json). JDK21과 저장소 Gradle wrapper를 사용했다.

검증한 핵심 계약:

- 완전한 작은 diamond 정책: 성공 3/4, REBASE 1/8, 실패 1/8, 기대 사용량 4로 직접 계산과 일치.
- 완전히 알려진 5개 행동 문제의 상위 3개가 완전 열거 순위와 일치. 동일 행동 중복 제거, 구간 중첩 대안 보존.
- 작은 REBASE 여러 개의 누적 확률, 그룹 합산·동률·한도, 희귀 목표의 성공 우선, 질량 보존·재정규화 금지·재화 보존.
- disabled는 기존 observation 그대로 반환. 실제 graph 종료·저장 복원 결과 일치, v17 설정 생략 disabled.
- 동일 graph GET/중복 command에서 추가 refinement 없음, graph 변경 시 새 평가, 설정 변경 request ID 충돌.
- primitive 512개 정확한 분포와 batch 결과 일치, 미열거/예산 제한은 UNRESOLVED, 저장 복원·예산 증액 후 한 번만 비용 반영.
- 새 세션의 새 request ID, 취소 완료 전에 재시작하지 않음, 다른 요청으로 이동한 뒤 도착한 취소/응답 무시.
- REBASE 및 후보/설정의 시나리오 압축·복원, 기존 시나리오 읽기.

Frontend: lockfile 그대로 npm ci, lint, typecheck, format:check, build 통과. 변경 영역 두 파일 **57개 통과**. 전체 Vitest는 3498개 중 3496개 통과, 2개가 5초 timeout이었다. 해당 workbenchApplication.test.tsx와 stash/WorkbenchLayout.test.tsx만 재실행해 **18개 모두 통과**했다. 전체 단일 실행이 전부 통과했다고 기록하지 않는다. build에는 기존 대형 chunk 안내가 남는다.

전체 backend `spotlessApply check generateJooq bootJar --continue`도 실행했다. unit 실행은 17분 이상 기존 DeferredCalculationTest의 큰 정확 분수 계산에 머물러 해당 테스트 worker를 종료했다(exit143). 종료 시 중간 집계는 361개/67실패/19skip이었다. 이 중 개발 중 cursor fixture 3실패는 수정 후 위 최종 검사에서 통과했다. 이 전체 검사 결과는 최종 변경의 완전한 회귀 통과를 의미하지 않는다. generateJooq는 임시 DB에 migration을 적용해 성공했다.

전체 integration 35개 중 33개 통과, 2개 실패: InfrastructureIntegrationTest의 migration 수 기대값(4 대 11), GoalFilterIntegrationTest의 automatic endpoint HTTP 상태 불일치. 최종 코드를 기본 파일 저장 경로에서 단독 재실행해도 초기 202 대신 409였다. 기존 파일을 삭제하지 않고 `CRAFTING_METHOD_JOB_DIRECTORY`를 새 임시 경로로 지정해 다시 실행하면 생성은 통과했고, 이후 쿠키 없는 GET의 404 기대값에서 409로 실패했다(line142). 기준 커밋의 새 작업 경로에서도 같은 line142 실패를 재현했다. 이 테스트는 DB/Redis만 격리하고 작업 파일은 기본 경로를 공유하므로, 기본 경로 실행과 새 경로 실행을 구분했다. Infrastructure migration 수 실패도 기준 커밋에서 재현했다.

기준 worktree에서도 직접 재현한 기존 실패: AutomaticSearchServiceTest의 두 역사 모델 fixture, LegacyAutomaticResultReaderTest 8개 중 7개, BaseRegistryParityTest, CatalogLoadOrderTest, EmptyRareApiRegressionTest, fracturedWhittling scenario, amulet/helmet/body/gloves 대표 fixture 각각 1개, TerminalContinuationApiTest의 COMPLETE 대 PARTIAL. 모든 전체 실패를 기존 문제라고 단정하지 않는다. 나머지 실패의 원인별 분류는 미완료다.

초기 isolated build directory에서 ArchUnit이 test class까지 가져와 실패한 실행이 있었다. 정상 backend/build 경로의 최종 재검증에서 3개 모두 통과했다. 검사 규칙이나 기대값을 약화하지 않았다.

## 남는 제약

- 모든 정책 조합의 전역 top-3가 아니다. 현재 proper witness를 기반으로 시작/후속 결정 한 개를 바꾼 대안 최대 32개 중 현재 확보한 후보를 반환한다. 공개 API 개수는 기본 3으로 고정, domain top(count)는 1..10을 지원한다.
- 순환 정책은 128단계까지 전파하고 꼬리를 UNRESOLVED로 남긴다. 기대값을 확정할 수 없으면 하한만 제공한다.
- eligible pool, 제한된 tier posterior, 선언된 유한 joint-roll 목록은 일부 선계산한다. cursor 자체의 checkpoint는 없어 진행 중 primitive는 복원 시 다시 생성한다.
- evaluator는 동일 그래프에서 재사용하지만 그래프가 변하면 전체 재생성한다. 서버 재시작/working-set eviction을 넘는 evaluator 영속화는 하지 않는다.
- actual item 입력은 기존 검증된 편집기로 연결한다. 결과 그룹의 대표 아이템을 실제 결과로 자동 확정하지 않는다. 브라우저에서 실제 게임 아이템을 넣는 수동 E2E는 수행하지 않았고 React/API 계약 테스트로 흐름을 검증했다.
- 전체 backend suite 완주·전체 실패 원인 분류는 미완료다. 외부 배포/원격 push/PR 생성은 하지 않았다.
