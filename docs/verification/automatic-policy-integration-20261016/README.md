# Automatic policy production integration — 2026-10-16

This is bounded integration evidence, not a claim that the full requested search space is complete. See [implementation contract](../../crafting-automatic-policy-implementation.md).

## Delivered behavior

- The Craft Support start form uses the automatic API. Original goals, six locales and independent Workbench behavior are preserved.
- Durable, owner-isolated jobs support revision/generation checks, duplicate commands, cancellation, reconnection, bounded resume and explicit recalculation. Selected-node editing starts a new job with the goal and abstract-quality context preserved.
- All candidate actions remain alternatives; multiple methods between the same nodes remain visible. Observations distinguish 100/300/500 method cycles from primitive costs and incomplete probability bounds.
- Safe scalar roll cells preserve conditional uniform distributions; unverified joint rolls retain unresolved mass. Unavailable actions are distinct from unsupported rules. Late shorter merges propagate depth changes.
- Breach maximum-quality work is an abstract checkpoint with separately tracked completion, not an invented catalyst count or scaling formula.

## Source verification

The final backend snapshot was copied into a Linux filesystem and tested in the project Docker environment. The frontend was likewise tested from a frozen Linux copy. Exact source hashes and machine-readable results are stored alongside this document.

- Backend: automatic/kernel/graph/service/repository/runner focused tests, then `check generateJooq bootJar` (6 min 19 s), then `spotlessCheck`. All 779 unit tests and eight integration tests passed, with zero failures, errors or skips.
- Frontend: lint, TypeScript checking, formatting, 95 test files / 2,171 tests, production build. The existing large-bundle warning remains (approximately 4.58 MB uncompressed JavaScript).
- Windows scripts: six unittest cases passed. Compose stack configuration validation passed without starting the application stack or touching its volumes.
- Tests cover independent small diamond/oracle cases, disjoint mass, first-hit, roll-cell reconstruction, late merge depth, quality work, storage rollback, restart, stale commands and HTTP contracts. Existing strategy tests cover adaptive outcome choices.

[Check summary](checks.json) records the final outcomes. Both source manifests were compared with the files committed here: zero hash mismatches. Backend and frontend checks finished with exit zero.

During real HTTP verification, a required ruleset header and controller error-advice registration were corrected. A later cyclic strategy request exposed outward-rounded, mutually exclusive masses whose merged upper bound exceeded one. The merged target interval is now intersected with the probability domain before strategy construction; a three-way merge regression and the actual API scenario both pass. Storage exhaustion returns a paused durable cursor instead of a broken job.

## Actual API and browser

The final JAR and frontend build were served by one disposable application server, with isolated PostgreSQL and Redis instances. No HTTP search response was mocked in these probes.

[Browser results](browser-results.json) confirm create/recreate 202, distinct job identities, stop, resume, non-root selection, editing, goal preservation and a second stop. Desktop and 390-pixel mobile viewports were inspected; see [desktop](browser-desktop.png) and [mobile](browser-mobile.png). Automated stale-response tests supplement this flow. The long method list is still a usability/scaling limitation; this was not a UI redesign.

[Solar HTTP timings](solar-times.json): empty rare ilvl 82 Solar Amulet, explicit projectile level ≥3 AND explicit Spirit ≥47. Create took about 1.405 s. Eight bounded advances took roughly 0.400–1.490 s each and returned six nodes / 128 methods, still READY and incomplete. The slice's 300 ms target is not a hard response deadline: primitive work, checkpoint serialization and response construction can exceed it. This run overlapped other verification work, so these are observations, not a latency guarantee.

[Life + Chaos results](life-results.json): a rare starting item already satisfying explicit life ≥100 retains root condition statuses MATCH/NO_MATCH. Three advances produced 64 nodes / 128 methods and PAUSED, with loss outcomes and six goal-matching state witnesses. The 1,164 loss rows include repeated horizons/methods and are not unique paths. Goal witnesses are not a claim of resolved success probability. Local recommendations remained zero; completion remained false.

## Bounded real-catalog scope probe

[Scope results](scope-results.json) and [probe source](ScopeProbe.java) use the final JAR's classes and reviewed Solar data. The approved 187-material inventory yields 76 candidate execution variants, all queried on the starting state, including variants that are unavailable, unsupported or rejected by policy. This is not proof that every material/base interaction is implemented. Abyss and jewel scope remain outside this benchmark.

The initial graph budget was 64 nodes, 128 methods, 20,000 retained states and 5,000 primitive outcomes. One bounded continuation used 128/256/50,000/10,000 respectively. The loops also had explicit operation limits. No unrestricted complete exploration was attempted. The retained count can cross its admission threshold by an already-open atomic batch (observed 50,005).

The root graph found A and B: first sample 10 nodes / 128 methods, A=1 and B=3; second sample 15 nodes / 256 methods, A=3 and B=3. Both samples had C=0 and complete=false; the second paused. Elapsed samples were approximately 1.439 s and 2.145 s. Independently continued A and B each reached 64 nodes and had respectively two and one full-goal exit witnesses, but neither displayed C within its bounded node budget. These exits include `PREPARATION_FIRST_HIT_POLICY_UNDECIDED` and therefore are not resolved success claims. The tiny exhaustive test proves the structural diamond and parallel-method behavior; the full real-catalog root→A/B→C map has **not** been completed by this benchmark.

The probe's sum of JVM heap-pool peak usage was 210,891,312 bytes (about 201 MiB). This is not process RSS or a production peak-memory guarantee. First local recommendation and overall completion were not observed; their timings are unavailable, not zero.

## Remaining decisions and limits

1. Whether a temporary Regal/Exalted affix that completes the full goal should immediately stop or must still be replaced remains undecided. Those branches stay UNRESOLVED. No choice was invented.
2. Material/base interactions without verified rules, unknown joint-roll laws, and quality-cap removal before abstract quality work remain explicitly unresolved/unsupported.
3. The working graph uses bounded in-memory laws and atomic JSON checkpoints, not the legacy SQLite streaming transition store. The one/two-action template grammar and resource-limited runs are not a completeness proof over arbitrary crafting programs.
4. Full real-catalog diamond convergence, first local recommendation, final global recommendation and large-scale completion remain unverified. Budget exhaustion and unresolved mass are never reported as successful completion.

## Operations

Verification used only task-owned disposable containers; unrelated Redis services and existing database volumes were preserved. Temporary files stayed under the designated `codex` directory. Source and this evidence are committed locally. No push, PR, master merge or deployment was performed for this integration unit.
