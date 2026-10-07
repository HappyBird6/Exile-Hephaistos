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

The Docker command was not recognized on PATH, and `C:/Program Files/Docker/Docker/resources/bin/docker.exe` was absent. This does not establish that Docker is absent at every possible location; daemon status was not checked. WSL distribution enumeration returned `Wsl/EnumerateDistros/Service/E_ACCESSDENIED`, so no distribution inventory was obtained. Only Java 8 was found at the inspected Zulu path. No Docker/WSL retry, installation or security-setting change was performed after the stop instruction. Therefore JUnit, formatter, Docker integration and the required `check generateJooq bootJar` have **not run**. Static fixture checks and `git diff --check` are not substitutes for those checks. Run the following in the project Docker development environment before integration:

```text
./gradlew --no-daemon check generateJooq bootJar
```

## Remaining work

Executable verification remains blocked. The existing comparison UI already displays up to five route cards, complete success probabilities or partial bounds, verified failure and unresolved mass, route selection, step guidance and calculation sources. Its existing frontend test fixture supplies three sample routes and exercises selection and partial reports. A separate sample comparison mockup would duplicate that implemented flow, so none was added. This is a source review conclusion, not a claim of executed UI validation. App integration and shared-file edits remain blocked by the unavailable changes from the other PC and require coordination.
