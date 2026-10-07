# Support goal verification checkpoint

Baseline: `7d27d6ce0a9fe35ab8f7d5dad15366416c10c725`.

## Existing implementation

- `SupportGoals` already assesses required families AND at least `candidateCount` distinct candidate families, with an individual minimum tier for each condition. Lower tier numbers qualify as better tiers.
- Duplicate families across either condition list are invalid, including different thresholds for one family. Malformed states containing multiple modifiers in the same family are rejected before assessment.
- Assessment reports insufficient item level, occupied families below target, and remaining prefix/suffix capacity. Feasibility applies to finite additions, not replacement or reroll strategies.
- `CraftSupport` already exposes goal editing, assessment and calculated sequence comparison. `SupportRecommendations` and its existing tests provide bounded addition-sequence evaluation; this checkpoint does not introduce another probability engine.

## Added verification

`SupportGoalContractTest` compares the existing assessor with a separate set-based satisfaction oracle. It covers every eligible Solar family variant at every published threshold, required/candidate tier combinations and quorum boundaries, duplicate candidate tiers, and invalid same-family item states. The oracle checks satisfaction only; it does not assert probability or addition feasibility.

No product source, shared type, registry, i18n, App wiring, Workbench file, catalog, migration or dependency is changed. This avoids conflict with unavailable work from the other PC.

## Validation status

The current execution environment has no Docker command or installation at the standard Docker Desktop path. WSL enumeration returns `E_ACCESSDENIED`; only Java 8 is installed at the inspected Zulu path. Therefore JUnit, formatter, Docker integration and the required `check generateJooq bootJar` have **not run**. Static fixture checks and `git diff --check` are not substitutes for those checks. Run the following in the project Docker development environment before integration:

```text
./gradlew --no-daemon check generateJooq bootJar
```

## Remaining work

After executable verification, review whether an additional sample comparison mockup adds value beyond the existing calculated comparison. If requested for that next checkpoint, keep it isolated and label all sample probabilities and routes as illustrative, with no API calls or claim of calculated ranking. A sample mockup has not been delivered in this first verification bundle. App integration and shared-file edits remain blocked by the unavailable changes from the other PC and require coordination.
