# Goal-directed probabilistic crafting planner — review handoff

Base: `94555fbde970d199c514eb7ad9d64b67b1e90a50`. Branch: `crafting/goal-directed-planner`.
Worktree: `E:\CODEX\codex\2026-10-19\task\goal-planner`.

## Handoff status — user requested wrapping up

Implementation is handed off now; do not treat the last full check as completed. No additional implementation is planned in this turn.

- Final frontend: 98 files, 2183 tests passed, with typecheck/lint/build/format PASS (155.37 seconds).
- New planner scenarios: 17/17 PASS in the focused final-production run (23.232 seconds), and 17/17 PASS in the latest completed full run (33.3 seconds).
- Latest completed full backend run: 795/796 unit tests passed; only the revision-expiry test setup failed. Its exact probability checks passed. The deterministic setup correction is included, but its final whole-suite rerun is still RUNNING.
- An earlier full run passed 796 unit + 8 integration tests, jOOQ generation and bootJar, before the final one-line production retry-classifier connection and revision-test preparation correction. Do not represent those earlier 8 integration results as final-source completion.
- Current final command: `check generateJooq bootJar --continue`, container `exile-goal-planner-validation`, exec session9150. At handoff it is running the unit test task (Gradle daemon PID131; test worker PID195 inside the container). It writes `validation-evidence/backend-final.log`, copies XML into `backend-full` when done, and removes its own validation container. It does not stop the local app services.
- `backend-full` currently contains the most recent completed unit run (795/796); its integration folder remains from the earlier successful run until the active command replaces results. Inspect the final log and XML timestamps before claiming success. A new Gradle run sharing the cache must wait for this one.
- The worktree `backend/build/libs/poe2craft.jar` is the earlier successful build until the active command exits0 and copies its final JAR. Do not use that host artifact as proof of the final retry code yet. The running app image was separately built from the final production sources, and both live goal cases passed on it.
- Local services remain running: frontend127.0.0.1:8081, app127.0.0.1:8080, PostgreSQL5432, Redis6379. DB/Redis container IDs and all three preexisting saved-job hashes are preserved. No push or remote deployment occurred.
- Live cases both COMPLETE: life+chaos (1 slice, 11 operations, 7 nodes, 3 methods, 6 goals); +cold (2 slices, 601 operations, 63 nodes, 32 methods, 48 goals, depth2). Default budgets, all three-goal hits have≥3 explicits.
- Windows script tests6/6 and Compose config PASS. Browser rendering remains unverified. npm ci still reports one dependency high-severity finding; bundle-size warning remains.

Continuation: read the tail of `E:\CODEX\codex\2026-10-19\task\validation-evidence\backend-final.log`; confirm exit status from its final BUILD line and sum fresh XML (expected796 unit and8 integration, zero failures/errors/skips) before marking the final check complete. The parent owns creation of the independent review chat. No additional review chat was created here.
## Change and temporary decisions

The previous root enumerated all currency pairs, filling 128 nodes/256 methods before children advanced. `GoalDirectedPlanner` considers the complete policy inventory, then selects acquisitions and necessary preparations using actual catalog eligibility and goal evaluation. Execution retains the full eligible probability distribution, including unfavorable outcomes. Child discovery is scheduled fairly. Checkpoints retain full ItemState and quality identity; useful direct additions become checkpoints rather than duplicate preparation pairs.

1. Policy `approved-187-2026-10-19-goal-v2` adds goal-purpose REGAL/EXALTED. Only CHAOS, WHITTLING, SINISTRAL_ERASURE, DEXTRAL_ERASURE change from excluded to conditional. Other exclusions/deferred entries remain. Four explicit modifiers remains the scope limit, not four currency uses.
2. Preparation STOP_ON_FIRST_HIT counts incidental complete goals as success. Bare CHAOS may prepare a higher-level removal target before CHAOS+WHITTLING when this changes removal priority while preserving matched predicates. The follower checks its prerequisite before being charged. Failed preparation/follower paths terminate; returning to the original item does not grant free preparation.
3. Only single-affix non-fractured bare CHAOS/WHITTLING-only retries without preparation or catalyst/quality conditions use a stationary law. Removing the sole affix yields the same pool. Side erasure is excluded from this equivalence. Unknown probability remains absorbing; general failures are terminal, with no assumed free recovery.
4. Terminal LOSS/DEAD items may share an absorbing sentinel because they have no future transitions. Probability, charged material counts, and GOAL maps are preserved. Failure compaction does not alter active/checkpoint/goal identity. The separate stationary retry law represents failed one-affix physical items by an equivalent future-law class, without physically restoring their starting affix. API failure targets are null, never the sentinel's source item.
5. Actual flat/percent units for `minion_skill_gem_level_+` and `strength_+%` are registered. This supports goal evaluation without inventing joint-roll odds. Resource manifests, production example and source-contract provenance are refreshed; original fixture goal values/oracles are unchanged.
6. Completion means supported milestone plans exhausted, not global optimality. Result reasons distinguish no supported plan, unresolved model/policy, and resource/storage pauses. Immutable maximum budgets disable Resume and reject meaningless resume with 409.

UI preserves the graph and adds visible unresolved incoming edges, rarity controls and six-locale explanations of supported scope, preparation conditions, retries and costs. Windows utility filename casing collisions are resolved. Dockerfiles include required cross-directory frontend inputs and writable application data.

## Limits and probability interpretation

Probabilities use reviewed catalog weights and declared roll assumptions, not independently measured game odds. Side+WHITTLING composition is an explicit simulator assumption. Fractured WHITTLING and unsupported joint rolls retain unresolved mass. Known + unresolved + boundary mass stays 1. Finite first-hit horizons remain 100/300/500; material use is reported, market and recovery prices are unknown.

Supported planning includes monotone milestones and a supported preparation/acquisition pair. Longer preparations, intentional goal loss/restoration, general recovery, and all possible COUNT/NOT/weighted-goal routes are not claimed complete. No-plan means no plan in this supported scope. Budgets were not raised to hide the original issue. Existing saved inputs/files stay intact; old policy/graph identities require recalculation.

## Reproducible scenario inputs

All scenarios use the full bundled production catalog and policy. Common input: Solar Amulet, ilvl82, implicit spirit15. Goals: AND, RARE-only, minimum1 unless stated. Exact test input/assertions: `backend/src/test/java/com/poe2craft/bootstrap/GoalDirectedPlannerScenariosTest.java`.

Modifiers: life120=`amulet:prefix:athlete-s`; life10=`amulet:prefix:hale`; chaos16=`amulet:suffix:of-expulsion`; cold6=`amulet:suffix:of-the-seal` (required level1); minion2=`amulet:suffix:of-the-despot`. Stat IDs: `base_maximum_life`, `base_chaos_damage_resistance_%`, `base_cold_damage_resistance_%`, `minion_skill_gem_level_+`, `strength_+%`.

| # | Input and goal/action | Expected assertion | Actual |
|---|---|---|---|
| 1 | MAGIC life120 → life+chaos | Greater Ruin certain success; graph exhausts with goal | PASS (final focused 17-case run) |
| 2 | MAGIC life120 → life+chaos+cold | Three explicits, depth≥2, both essence orders and merged goal, no two-affix false success | PASS (final focused 17-case run) |
| 3 | RARE life120 → minion≥3 | Deliberate CHAOS candidate with success and failure outcomes | PASS (final focused 17-case run) |
| 4 | RARE empty → strength≥7% | Prepared removable affix before replacement essence | PASS (final focused 17-case run) |
| 5 | MAGIC empty ilvl1 → chaos | Greater Ruin unavailable; no outcomes, unresolved=1 | PASS (final focused 17-case run) |
| 6 | RARE life120+cold6 → life+chaos, WHITTLING | Catalog level1 suffix removed, life preserved in every removal branch | PASS (final focused 17-case run) |
| 7 | RARE life120+chaos16 → life+chaos+cold, CHAOS | Life-loss risk retained; total probability=1 | PASS (final focused 17-case run) |
| 8 | RARE generated legal four-explicit state, EXALTED | Fifth explicit outside scope, boundary=1 | PASS (final focused 17-case run) |
| 9 | RARE life120 → chaos≥999 | Exhausted supported graph, no fabricated hit | PASS (final focused 17-case run) |
| 10 | RARE fractured life120, CHAOS+WHITTLING | Unsupported, unresolved=1 | PASS (final focused 17-case run) |
| 11 | Goal-purpose policy context | CHAOS permitted; ANNULMENT/GREATER_CHAOS excluded | PASS (final focused 17-case run) |
| 12 | RARE life120 → chaos, EXALTED budget1 | Truncated probability unresolved, total=1, no renormalization | PASS (final focused 17-case run) |
| 13 | RARE life120 → minion≥3, 100 CHAOS cycles | Independent finite-series oracle including absorbing unknowns; execution and actual graph both checked; mass=1 | PASS (final focused 17-case run) |
| 14 | RARE life10+minion2 → life+minion≥3 | Higher-tier preparation before WHITTLING without immediate goal gain | PASS (final focused 17-case run) |
| 15 | MAGIC minion2 → RARE minion≥3 | REGAL preparation then probabilistic CHAOS admitted | PASS (final focused 17-case run) |
| 16 | #14 executed through preparation/follower | GOAL+DEAD nonempty, mass=1, conditional follower charge, retained<2000 | PASS (final focused 17-case run) |
| 17 | RARE life120 → chaos, EXALTED full vs compact absorption | Equal exact LOSS probability/material/GOAL maps, one compact failure terminal | PASS (final focused 17-case run) |

These are domain/scenario/invariant tests, not 17 browser E2E tests. The previous full-unit artifact confirms all17 passed. Final rerun includes the deterministic cold6 fixture, quality-checkpoint cleanup, and production-graph retry classification. Self-review found that the runner-level retry test supplied LOSS directly while the graph classified no-progress later; retry detection now uses the same effective state classifier as execution. Scenario13 also checks the actual graph against the independent 100-cycle oracle.

## Reproduction commands and verification evidence

Requirements: Java21, Node24/npm, Docker. From worktree:

```powershell
cd backend
.\gradlew.bat check generateJooq bootJar
cd ..\frontend
npm ci
npm run typecheck
npm run lint
npm run test -- --run
npm run build
npm run format:check
cd ..
python -m unittest discover -s scripts/tests -v
```

Intentional resource metadata refresh helpers: `node scripts/update-ruleset-manifest.mjs` and `node scripts/refresh-source-fixture-identities.mjs`; these do not replace checks. Production example was regenerated using the real evaluator, preserving source inputs and goal/oracle numbers.

Host Java17/npm absence required pinned Temurin21 and Node24 Docker validation. Exact source snapshots are copied to native container storage to avoid Windows bind-mount latency. Gradle/Vitest use two workers; test scope and isolation are unchanged. Integration/codegen use disposable Testcontainers databases, never the user's database. Evidence directory: `E:\CODEX\codex\2026-10-19\task\validation-evidence`. Full XML: `backend-full`; frontend JSON: `frontend-tests.json`.

Final full checks: repeating after the final retry-classifier change. The preceding full run passed all 796 unit/architecture tests and all 8 integration tests with zero skips, plus formatting, jOOQ generation and bootJar (10m23s). Prior backend run: 796 tests, 769 passed, 27 failed (25 stale provenance fixture references, two changed catalog-count expectations). Those metadata references were corrected without weakening assertions. Final frontend: 98 files/2183 tests passed (155.37 seconds); typecheck, lint, build and format passed; formal frontend Docker image build passed. Windows scripts: 6 passed (2.574 seconds). Compose config --quiet passed. npm ci reported one existing high-severity dependency finding; package/lock files were not changed. The production build also retains its large-bundle warning.

## Runtime and independent review

Preserve original `.env`, external PostgreSQL volume, Redis and all existing jobs. Replace only local app/frontend containers from this worktree. Job bind directory: `E:\CODEX\codex\2026-10-19\task\exile-method-jobs`. No push or remote deployment.

Actual local runtime commands (the temporary override contains only the existing job-directory bind and environment path):

```powershell
docker compose --env-file E:\WORK\Exile-Hephaistos\Exile-Hephaistos\.env -f infra/compose.yaml --profile stack build app frontend
docker compose --env-file E:\WORK\Exile-Hephaistos\Exile-Hephaistos\.env -f infra/compose.yaml -f "$env:TEMP\exile-goal-runtime-override.yaml" --profile stack up -d --no-deps app frontend
```

The focused executable scenario command is `backend\gradlew.bat test --tests com.poe2craft.bootstrap.GoalDirectedPlannerScenariosTest` (run from `backend` as `.\gradlew.bat ...`). Final focused result: 17 tests passed, 0 failures/errors/skips, 23.232 seconds, including the actual graph retry comparison.

After serving this code:

```powershell
.\scripts\verify-goal-planner.ps1 -BaseUrl http://localhost:8081 -MaximumSlices 100
```

Creates two fresh synthetic jobs with separate sessions for rows1/2. Never resumes/cancels existing user jobs. Requires a terminated supported result with real goal nodes; three-goal success must have≥3 explicits. On slice exhaustion it cancels only its own job. Live results on the final production code:

| Goal | Status | Slices / operations | Nodes / methods | Goal nodes | Maximum depth | Retained states |
|---|---|---|---|---|---|---|
| life+chaos | COMPLETE / SUPPORTED_PLANS_COMPLETE | 1 / 11 | 7 / 3 | 6 | 1 | 25 |
| life+chaos+cold | COMPLETE / SUPPORTED_PLANS_COMPLETE | 2 / 601 | 63 / 32 | 48 | 2 | 347 |

IDs: `c9c98b4e-926e-4af2-87c6-61096f65c262` and `12aa7934-3dd3-4752-bc46-b0f838dae2f6`. All three-goal successes have at least three explicits. Default budgets were sufficient. Inputs normalize the intended goal to RARE-only and positive minima1; original stored inputs are preserved rather than overwritten.

HTTP smoke: liveness/readiness UP (200), HTML/JS/CSS 200. Served JS is `index-DcaDZUTF.js`. Existing PostgreSQL/Redis container IDs are unchanged; all three preexisting saved-job JSON SHA256 values are unchanged. The initial PowerShell readiness loop incorrectly treated the vendor-JSON response as an object; an explicit UTF8 decode confirmed the healthy response. This was a checker parsing issue, not an application health failure.

CUA reports no available browser, so actual browser rendering has not been verified. React tests cover interactions; HTTP checks cover assets/APIs. Parent creates the separate review chat; none is created here. Review should independently examine pruning soundness, full goal AST semantics, preparation charging, stationary-law assumptions, fixture identities and six-locale disclosures, and repeat at least ten varied product scenarios including the two original requests.

## Modified-file inventory

- `backend/src/main/java/com/poe2craft/crafting/application/pathsearch/AutomaticSearchService.java`
- `backend/src/main/java/com/poe2craft/crafting/domain/AutomaticCraftingAction.java`
- `backend/src/main/java/com/poe2craft/crafting/domain/AutomaticCraftingKernel.java`
- `backend/src/main/java/com/poe2craft/crafting/domain/AutomaticCurrencyPolicy.java`
- `backend/src/main/java/com/poe2craft/crafting/domain/pathsearch/AutomaticMethodExecution.java`
- `backend/src/main/java/com/poe2craft/crafting/domain/pathsearch/AutomaticMethodGraph.java`
- `backend/src/main/java/com/poe2craft/crafting/domain/pathsearch/CertifiedMethodRunner.java`
- `backend/src/main/java/com/poe2craft/crafting/domain/pathsearch/GoalDirectedPlanner.java`
- `backend/src/main/java/com/poe2craft/crafting/domain/WorkbenchSimulator.java`
- `backend/src/main/resources/crafting/automatic-currency-policy-v1.json`
- `backend/src/main/resources/crafting/goalfilter/definitions-v1.json`
- `backend/src/main/resources/crafting/goalfilter/production-example.json`
- `backend/src/main/resources/crafting/ruleset-v1.json`
- `backend/src/test/java/com/poe2craft/bootstrap/GoalDirectedPlannerScenariosTest.java`
- `backend/src/test/java/com/poe2craft/crafting/application/pathsearch/CertifiedSearchJobsTest.java`
- `backend/src/test/java/com/poe2craft/crafting/AutomaticCurrencyPolicyTest.java`
- `backend/src/test/java/com/poe2craft/crafting/domain/AutomaticMethodGraphTest.java`
- `backend/src/test/java/com/poe2craft/crafting/infrastructure/goalfilter/GoalDefinitionsLoaderTest.java`
- `contracts/crafting-paths-v1/solar-source-fixture.json`
- `docs/goal-directed-planner-2026-10-19.md`
- `frontend/src/features/crafting/goal-filter/catalogPickerParity.test.tsx`
- `frontend/src/features/crafting/goal-filter/GoalFilterPanel.tsx`
- `frontend/src/features/crafting/method-search/automaticApi.ts`
- `frontend/src/features/crafting/method-search/AutomaticMethodSearch.test.tsx`
- `frontend/src/features/crafting/method-search/AutomaticMethodSearch.tsx`
- `frontend/src/features/crafting/method-search/calculationState.ts`
- `frontend/src/features/crafting/method-search/calculationStatus.test.ts`
- `frontend/src/features/crafting/method-search/calculationStatus.ts`
- `frontend/src/features/crafting/method-search/CalculationStatus.tsx`
- `frontend/src/features/crafting/method-search/candidateRoute.ts`
- `frontend/src/features/crafting/method-search/CandidateRoute.tsx`
- `frontend/src/features/crafting/method-search/CurrencyPolicy.test.tsx`
- `frontend/src/features/crafting/method-search/CurrencyPolicy.tsx`
- `frontend/src/features/crafting/method-search/PlanningDisclosure.tsx`
- `frontend/src/features/crafting/method-search/planningMessages.ts`
- `frontend/src/features/crafting/method-search/routeSelection.ts`
- `frontend/src/features/crafting/method-search/supportStorage.test.ts`
- `infra/docker/backend.Dockerfile`
- `infra/docker/frontend.Dockerfile`
- `scripts/refresh-source-fixture-identities.mjs`
- `scripts/verify-goal-planner.ps1`

## Final self-review regression correction

One full rerun had 795/796 unit tests passing. All 17 new scenarios passed and all exact probability assertions in `CertifiedSearchJobsTest` passed. The single failure was at its revision-expiry assertion (line371 before this edit), via `rejected`, not probability bounds. It assumed 12000 worker steps necessarily published enough snapshots to evict the original revision. Publication counts vary with scheduling. The test now explicitly creates five distinct CANCEL generations, asserts that the revision window was exceeded, and retains the exact `REVISION_EXPIRED` assertions. The invalid-offset check uses the fresh revision. Product revision retention and probability code are unchanged.

An attempted concurrent isolated rerun never started tests because another Gradle container held the shared journal cache lock; that attempt is not counted as a test result. Gradle runs sharing this cache must be sequential. The failure XML and log remain in `validation-evidence/backend-revision-regression`. The final full rerun includes this deterministic test setup and remains pending until its result is recorded below.