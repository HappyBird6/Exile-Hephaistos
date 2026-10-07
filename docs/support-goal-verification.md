# Support goal verification checkpoint

Baseline: `7d27d6ce0a9fe35ab8f7d5dad15366416c10c725`.

## Existing implementation

- `SupportGoals` already assesses required families AND at least `candidateCount` distinct candidate families, with an individual minimum tier for each condition. Lower tier numbers qualify as better tiers.
- Duplicate families across either condition list are invalid, including different thresholds for one family. Malformed states containing multiple modifiers in the same family are rejected before assessment.
- Assessment reports insufficient item level, occupied families below target, and remaining prefix/suffix capacity. Feasibility applies to finite additions, not replacement or reroll strategies.
- `CraftSupport` already exposes goal editing, assessment and calculated sequence comparison. `SupportRecommendations` and its existing tests provide bounded addition-sequence evaluation; this checkpoint does not introduce another probability engine.

## Added verification

`SupportGoalContractTest` compares the existing assessor and the recommendation engine's separate matcher with a set-based satisfaction oracle. It covers every eligible Solar family variant at every published threshold, required/candidate tier combinations and quorum boundaries, duplicate candidate tiers, and invalid same-family item states. An empty currency sequence isolates starting-state satisfaction: success is exactly zero or one, unresolved mass is zero, and the addition pool store must never be accessed. The oracle does not assert currency-transition probability or addition feasibility.

No product source, shared type, registry, i18n, App wiring, Workbench file, catalog, migration or dependency is changed. This avoids conflict with unavailable work from the other PC.

## Validation status

Final validation on 2026-10-07: **passed**, exit code zero, using the existing project image `exile-ui-qa-backend-check:latest` in a separate disposable verification container. The worktree was mounted read-only and its Backend source copied into the container. Testcontainers used temporary PostgreSQL/Redis resources; no existing application container or data volume was started, reset or replaced.

```text
./gradlew --no-daemon check generateJooq bootJar
```

The final run passed 390 unit/ArchUnit tests and six Docker integration tests with zero failures, errors or skipped tests, including all four new Support contract tests. Spotless, temporary-database jOOQ generation and `bootJar` passed. An initial full run failed only on the new test's formatter line wrapping; that file was corrected and the full command rerun successfully. Frontend and Compose files were unchanged; frontend checks and Compose configuration validation were not run for this Backend test-only change.

Docker access correction: the initial PATH lookup and standard Program Files path check were insufficient. Docker Desktop is installed per-user at `C:/Users/SSAFY/AppData/Local/Programs/DockerDesktop`, with running Desktop/backend processes. The executable is `resources/bin/docker.exe`; the inspected process PATH includes that directory. The sandbox refused process launch even by its absolute path. The approved normal-user execution environment successfully connected to client/server version `29.6.2`. No installation, administrator setting or security setting was changed. WSL enumeration had returned `Wsl/EnumerateDistros/Service/E_ACCESSDENIED`; that path was not retried or used for verification. The supplied Docker disk-image location was not manipulated.

The existing Compose project is named `exile-hephaistos`; its actual `app-1`, `frontend-1`, `postgres-1` and `redis-1` containers were stopped and refer to the original checkout's `infra` configuration. The running `tender_volhard` development container mounts the original checkout. Neither was reused for testing, to preserve the original source and other sessions. Verification logs and XML results are retained separately under the project's parent `codex/support-verification-evidence` directory, outside Git.

## Remaining work

The existing comparison UI already displays up to five route cards, complete success probabilities or partial bounds, verified failure and unresolved mass, route selection, step guidance and calculation sources. Its existing frontend test fixture supplies three sample routes and exercises selection and partial reports. A separate sample comparison mockup would duplicate that implemented flow, so none was added. This is a source review conclusion, not a claim of executed UI validation. App integration and shared-file edits remain blocked by the unavailable changes from the other PC and require coordination. No main merge or deployment was performed.
