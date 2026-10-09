# Goal filter 통합 결과

기준 master는 `7d27d6ce0a9fe35ab8f7d5dad15366416c10c725`이다. 계약 `8c4f758`, FE `705d749`, BE `91f6e18`을 `codex/goal-filter-integration`에 통합했다. master 병합과 운영 배포는 수행하지 않았다. 사용자 승인에 따라 한국어를 우선하고 영어 fallback을 사용한다.

## 실제 연결과 입력 보존

`CraftSupport.tsx`는 기존 family/tier 입력과 API를 유지하면서 전용 `ConnectedGoalFilter`를 연결한다. 텍스트 검증에서 받은 전체 ConcreteItem의 실제 values, implicit/explicit, quality/conditions를 별도로 보존한다. 수동 tier에는 실제 roll이 없으므로 수치 판정을 차단한다. 기존 Bucket을 수치 입력으로 보내지 않는다.

전용 adapter는 registry의 ACTIVE supportedBases와 workbenchBases에서 서버 base 목록을 얻고 각 base의 실제 initial context를 조회한다. 새 생성 적격성, 현재 아이템의 layer별 존재, 평가 지원 상태를 따로 표시한다. 존재하는 부적격 stat도 목표에 추가할 수 있으며 서버가 진단한다. 다른 시작 base 선택 시 목표 행을 보존하고 base 불일치를 표시한다. 사용자가 현재 base를 목표에 반영할 수 있다. 서버 base는 실제 implicit과 빈 explicit을 가진 시작 상태이며 Workbench의 현재 제작 아이템을 대신하지 않는다.

HTTP context에는 snapshot/base/level만 보낸다. 평가와 추천에는 전체 ItemState를 보낸다. 합성 mock과 fixture stat은 실제 화면에 사용하지 않는다. catalogVersion을 자동 대체하지 않는다. 편집·context·item·omen·budget identity가 달라지면 기존 결과를 숨기고 이전 Query 요청을 취소한다. 미완 숫자 편집도 결과를 숨긴다. editor는 Support session 동안 유지되며 탭 전환과 잘못된 item level 편집으로 목표를 잃지 않는다.

`SupportConfiguration.java`의 새 Bean은 WorkbenchService를 주입하고 BundledGoalCatalogs 기반 GoalCatalogIndex를 만든다. Controller는 기존 ObjectMapper Bean의 전용 strict copy를 사용한다. 공용 ItemState/Bucket/registry/App/i18n/Workbench CSS와 기존 family 추천 엔진은 수정하지 않았다. 고장 PC의 미푸시 134-base 변경과 실제 대조는 불가능했다.

## 완료 범위와 남은 numeric 확률

실제 catalog 편집, validation, 실제 아이템 숫자 판정과 원인 표시는 연결됐다. Solar cold 10 + all elemental 12 = 22는 실제 map-text/evaluate API와 브라우저로 확인한다. 품질 및 특수 조건의 수치 효과는 미지원이다. 실제 값이 없는 수동 tier를 가짜 roll로 채우지 않는다.

생산 numeric 추천은 모든 경우 `UNSUPPORTED / NUMERIC_DISTRIBUTION_NOT_IMPLEMENTED`, comparisons=[], rankingCertified=false, comparedSequences=0, totalSequences=null이다. 완료된 전체 1차 확률 추천이라고 보고할 수 없다. 기존 family/minimumTier/N-of-M 추천은 별도 정상 경로다.

후속 작업은 backend의 domain/application/infrastructure goalfilter 경로에서 numeric kernel, joint-roll ledger, projection, 별도 cache로 시작할 수 있다. AdditionRules.plan/AdditionTransitions.transition의 modifier 선택 비율에 근거가 검증된 joint-roll outcome을 결합하고 full ItemState와 omen 상태, first-hit 흡수, 미해결 질량을 유지해야 한다. 현재 ExactNumericDistribution은 테스트 oracle이며 생산 분포가 아니다. single-stat uniform ledger와 CoupledStatRollModel 추측을 hybrid/derived/conditional 근거로 확대할 수 없다. snapshot/base/rules/ledger/AST/catalog/roll-projection 버전으로 cache를 분리해야 한다.

추가 공유 파일 수정이 필요해지면 StateBucket, ItemState/Validator, AdditionRules/Transitions, ModifierPoolResolver, AdditionPoolCache/Store/Jdbc store, SupportRecommendations, WorkbenchSimulator, CoupledStatRollModel, registry-v2.json 경계를 먼저 보고해야 한다. 지금 통합에서는 해당 변경이 필요하지 않았다.

## 검증 근거

최종 FE tree에서 npm ci, lint, typecheck, format:check, 전체 test와 build를 실행한다. BE는 check generateJooq bootJar를 전용 컨테이너와 임시 PostgreSQL/Redis에서 실행했다: unit/ArchUnit 411, integration 7, 실패·오류·skip 0. 테스트 기대값이나 ArchUnit을 완화하지 않았다.

브라우저 검증은 전용 localhost 18081/18080과 전용 DB/Redis를 사용한다. 기존 localhost8081/API8080/Redis6381과 데이터 volume을 보존한다. 실제 Solar MATCH/NO_MATCH, 반복/Enter, 미완 숫자, 수동 tier 수치 소실, 다른 base와 부적격 행 보존, 미지원 stat, 입력 오류와 서비스 실패, 390px 화면을 확인한다. 상세 실행 로그와 XML 및 화면 근거는 worktree 외부 `../goal-filter-integration-evidence/`에 보존한다.

기존 source-map-js 개발 전이 의존성 high audit 1건과 Vite 대형 chunk 경고는 남아 있다. 의존성·lockfile 변경은 이 통합 범위 밖이다.
