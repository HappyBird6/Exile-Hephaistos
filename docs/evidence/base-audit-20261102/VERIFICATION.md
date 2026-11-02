# Verification: base audit C

Baseline: `8524b6b781651379a6cff9108525282a0691df25`.

## Passed

- Frontend production build (`tsc --noEmit && vite build`). Vite reports the existing large bundle warning.
- Frontend lint and Prettier format checks.
- Frontend complete suite: 138 files, 2,475 tests passed. See `frontend-full-tests.log`.
- Focused CSS/DOM, rarity, preservation, source display and belt contracts: 51 tests passed before the redundant three impossible Normal/Magic quality-response cases were removed from the parameterized response-only test. Full suite above includes the final parameterization.
- Six-locale display coverage: 2,954 definitions, 447 compound bindings, all six language coverage reports complete.
- `node scripts/base-audit/verify.mjs`: 454 existing nested source/catalog files unchanged; all 39 new ordinary pools equal the existing class template; every zero-weight explicit definition is an exact previously reviewed definition; four semantic frontend mirrors agree; 585 sealed resources match their digests; source fixture changes only provenance, never goals or numerical oracles. See `preservation-check.json`.
- Java 21 main/test compilation and `bootJar` completed in the existing Temurin container. No host Docker socket, application port or database was used.

## Backend unit status

Final focused `BaseSelectionPolicyTest`, `BaseRegistryTest` and `ReviewedBeltsTest`, plus `spotlessCheck` and `bootJar`, passed (`BUILD SUCCESSFUL in 33s`). See `backend-policy-tests.json` for suite counts and command. Broader no-socket runs were intentionally stopped after identifying the limits below, rather than leaving the parent waiting indefinitely. They are not full-suite passes.

- Historical catalog expansion initially prevented old automatic records from resolving their exact goal catalog signature. The production fix tries frozen pre-expansion membership (134 IDs) alongside current membership (173 IDs), retaining all original IDs and ASTs.
- After that fix, the observed legacy v8/v10 tests successfully read the saved node/method observations and reached their strict original-request JSON comparison. They failed at `LegacyAutomaticResultReaderTest.java:196` and `:290`. These assertions were NOT changed or suppressed. Exact JSON roundtrip compatibility remains a reported verification issue; the suite was stopped before a final class report. `AutomaticSearchService.original`, `MethodSearchProtocol`, `BasicCurrencyState`, `GoalFilter` and `ItemState` are unchanged from baseline. Historical fixtures omit newer nullable ItemState fields; no claim that the baseline entire suite passed is made here.
- The broad goal catalog tests were stopped for execution time. The new source-only implicit predicates explicitly retain unsupported goal-evaluation/probability capabilities. Their exact IDs are listed in `unreviewed-implicit-goals.json`; no numeric model was inferred.
- The coverage test's formerly assumed production "unsupported stat exists" case now uses a deliberately unreviewed definition set, so growing reviewed coverage cannot remove the test premise. The amulet coverage test asserts unsupported capabilities for new source-only implicit predicates while retaining all existing supported-stat checks.
- Catalog count expectations reflect the exact appended inventory. Old rarity fixture bytes are preserved; the three Rare-only belts' captured Normal/Magic currency attempts are asserted rejected. No history regression was converted into a new passing expected observation.

## Not executed / denied

- Browser verification is reserved for the user. Server state was not changed.
- DB integration checks and `generateJooq` were not executed. Automatic approval review denied the proposed `docker run` host socket mount (`-v /var/run/docker.sock:/var/run/docker.sock`) for `check generateJooq bootJar`: the mount grants broad control over the host daemon and other containers/volumes beyond isolated verification authorization. The rejected command did not execute. It was not retried, substituted or bypassed.
- Optional Windows CIM process inspection was denied by the execution sandbox; it was not retried and has no effect on source or verification outcomes.

No browser storage, actual user sessions, original downloaded artwork, database, remote branch or running application server was modified.
