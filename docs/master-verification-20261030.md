# 원격 master 반영 검증 — 2026-10-30

사용자 요청: 오늘 완료한 Exile 작업 전체를 HappyBird6/Exile-Hephaistos 원격 master에 반영.

## 범위와 보존

- 기준 origin/master: `4eb6ea3c5fa45bd1f24ce2d32ec96eaff220b30e`.
- 검증 구현 후보: `563a2d885f98eebee01ac95351880eb5dbdf7beb`, 선행 의존 변경 포함 21커밋/144파일. 이후 변경은 이 검증 기록과 문서 끝 빈 줄 정리뿐이다.
- 공통 훼손 상태·작업 전이, 분열 불변과 첫 일반 옵션 Chaos, 엄격 Exalted 정책, 미완료 중간 경로, 프런트 정리 및 티어/잡옵 집계, 베이스 갱신과 자동 생성 409 수정, 9000번대 기본 포트와 앱 데이터 영구 보존을 포함한다.
- 전체 관련 작업트리는 시작 시 clean. 다른 작업 브랜치의 오늘 커밋 누락 없음. 범위 밖 과거 문서 보관 커밋 d741f67은 포함하지 않는다. 읽기 전용 전략·작업대 조사에서 새 구현을 만들지 않았다.
- 현재 diff와 전체 푸시 커밋의 추가 내용에서 비밀키/토큰/서명 전송 URL/개인 EHSC2 원문 패턴을 검사했다. 해당 항목 없음. .env/백업/JAR/DB/실행 로그는 포함하지 않는다.
- 사용자 작업트리와 실행 서버를 유지하며 별도 master 통합 작업트리에서 fast-forward한다. 강제 push, 보호 설정 변경, 서버 재배포/재시작은 하지 않는다.

## 최종 검사

- 프런트 전체 125파일/2,397테스트: 2,395통과, 2개는 5초 시간 초과. 해당 WorkbenchExtension/stateProjection 2파일 20테스트를 동일 제한의 단일 worker로 재검증해 전부 통과. 기대값·제한·테스트 코드 변경 없음.
- lint/typecheck/format:check/build 통과. 기존 큰 번들 경고 유지.
- Backend `check generateJooq bootJar --continue`: 961개 중 921통과/40실패. 전체 check 자체는 실패다. 기준 32689f8의 955개/40실패 XML과 대조하여 실패 집합이 정확히 동일하고 신규 실패 없음.
- 통합 8개, spotlessCheck, generateJooq, bootJar 통과. Windows 스크립트 7개, Compose config --quiet 통과.
- 검사 결과는 모든 기존 실패가 해결됐다는 뜻이 아니다. 아래 기존 실패를 유지해 공개한다.

## 기존 실패 40개

- `BaseRegistryParityTest.allExistingInitialsActionsAndSeededTracesRemainExact()`
- `EmptyRareApiRegressionTest.completedApiRetainsPreparationRetryProbabilityAndOnlyConcreteGoalTargets()`
- `GoalCatalogParityTest.everyStartingCatalogSourceIsDiscoverableWithoutRarityOrFamilyPruning()`
- `LegacyAutomaticResultReaderTest.actualV10V3QualityAndChaosObservationsRemainUnchangedAndReadOnly()`
- `LegacyAutomaticResultReaderTest.readsActualV8CompletedResultAndOriginalRequestWithoutChangingAnyBytes()`
- `TerminalContinuationApiTest.cancelsResumesAndReloadsSharedConcreteOutcomesWithMeasuredRequestLatency()`
- `CertifiedJobRestartTest.continuedRunnerMatchesUninterruptedExitMassAndCurrencyUses()`
- `CertifiedJobRestartTest.jobReopensGraphAndPartialConditionalTransportWithNewGeneration(Path)`
- `CertifiedJobRestartTest.readRenewedLeaseSurvivesRestartWithoutRewritingTheGraph(Path)`
- `CertifiedMethodProductionTest.realSolarRenewalKeepsExactTruthInsideEveryRequestedHorizon()`
- `CertifiedSearchJobsTest.activeRequestsRenewTheIdleLeaseButAbandonedJobsStillExpire()`
- `CertifiedSearchJobsTest.boundedCommandReplayDoesNotLimitTheNumberOfResumptions()`
- `CertifiedSearchJobsTest.completedGoalPublishesStrategyAndStillHonorsEntryCancellation()`
- `CertifiedSearchJobsTest.expiryEvictionAndShutdownInvalidateQueuedWorkWithoutRunningClosedStores()`
- `CertifiedSearchJobsTest.realCatalogBoundsKeepExactConditionalLawAndGenerationControls()`
- `IndexedMethodSearchServiceTest.actualLowLevelSolarPreservesConcreteEntryLawAndCancelledGeneration()`
- `IndexedMethodSearchServiceTest.completedIndexedEnumerationCanCancelAndResumeEntryWorkWithoutStrategy()`
- `JointAttributeLawTest.correlatedWeightedCheckpointIsOneRealStateAndNextTransitionRetainsItsJointSource()`
- `MethodSearchLifecycleTest.expiredQueuedJobCannotPublishIntoANewJobWithTheSameRequestId()`
- `MethodSearchLifecycleTest.reconnectReadsCurrentRevisionAndRejectsExpiredPagesAndCrossRevisionCursors()`
- `MethodSearchLifecycleTest.shrinkingResumeBudgetIsARequestErrorWithoutChangingTheJob()`
- `MethodSearchLifecycleTest.staleQueuedGenerationCannotPublishAfterCancelAndResume()`
- `MethodSearchServiceTest.cancellationGenerationIdempotencyAndResumePreservePartialWork()`
- `MethodTransitionTest.countExclusionMethodExitsRetainRealExecutionsAndRecoverToRoot()`
- `MethodTransitionTest.emptyIntermediateGoalStopsAfterOneUseAndZeroSuccessHasNoHitEdges()`
- `MethodTransitionTest.lateSuccessSurvivesDisplayLimitAndHasStateSpecificAbsorption()`
- `MethodTransitionTest.omittedSuccessUnknownMassAndOddTwoActionObservationRemainDistinct()`
- `PathSearchContractTest.sharedWireFixturesRoundTripAndExactOracleAreConsumed()`
- `PathSearchHttpTest.strictInputRulesetAndDeletedReferencesKeepOriginalInput()`
- `PathSearchLifecycleTest.cancellationRacesWithActualExecutorAndInvalidatesQueuedGeneration()`
- `PathSearchLifecycleTest.ttlAndBoundedCapacityDoNotRecomputeOrMixOwners()`
- `PathSearchServiceTest.idempotencyIsolationConflictsAndExpiredRevisions()`
- `PathSearchServiceTest.solarOracleAndRealContinuationMatchWithoutLosingPhaseOrMass()`
- `AutomaticEquipmentTierLawTest.ringTiersWithDifferentCreationEligibilityShareOneAttributeAndOneFutureState()`
- `AutomaticPreparationRulesTest.chaosPreparationUsesExactlyOnePlainExaltOrRegalWithoutAssumingEqualProbabilities()`
- `AutomaticRefinementsTest.changingChaosFailuresUseTheirOwnLawFor100Attempts()`
- `AutomaticRefinementsTest.routeStrategyUsesTheSameFinite100ChaosProbabilityWithoutAnUnlimitedRetryLoop()`
- `AutomaticTierRenewalTest.oneHundredFirstHitLawMatchesIndependentChangingStateMarkovOracle()`
- `TerminalAdditionClassesTest.restoresBudgetPauseThenEnlargesWithoutChangingAnyOutcome()`
- `TerminalAdditionClassesTest.restoresRunningRepresentativeAndAliasesWithoutDuplicatingExecution()`
