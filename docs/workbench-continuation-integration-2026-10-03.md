# Workbench continuation integration — 2026-10-03

This batch restores the canonical Perfect Mind icon and executes bounded combined regression. Registry220/implemented119, current155/implemented111/pending44 and exclusions65 remain unchanged. It does not claim complete game coverage or a verified game distribution.

## Delivered and reviewed

- WB-034: the exact existing [canonical art URL](https://cdn.poe2db.tw/image/Art/2DItems/Currency/Essence/ManaEssencePerfect.webp) returned HTTP200 in project Docker. Original WebP is tracked with SHA-256 `0f7f33951fb9436ad90ab782900156970f4d8e57e4d0af57d0f9a8fc2863e6cd`; existing source/license attribution and load-failure fallback remain. Browser verified a loaded image, and actual390px screenshot was visually reviewed.
- [Pending44 triage](workbench-remaining-gaps-2026-10-03.md#implementation-triage-after-handoff):42 primary evidence routes, two primary state/design routes, zero established code-only omissions. The gates overlap; approving design does not establish game facts. No new base, guessed Scrap formula, retired Omen or deferred operation is enabled.
- Self-review: the only product change is the original bitmap plus its manifest status/hash. No executable Backend/Frontend logic, lockfile, item/film schema, probability ledger or migration changed. The manifest hash matches tracked bytes. Numeric assumptions, DropChance model versus SpawnTags eligibility and unsupported-state boundaries remain visible.

## Executed checks

All application validation uses project Docker. Frontend `npm ci`, lint, typecheck, format check, all320 tests in41 files and build passed. Bundle remains `index-BJMv7zkp.js` because executable code is unchanged. The restored asset was copied into the existing local QA nginx container for actual browser verification; Docker image rebuild was not required for this asset probe.

Sequential Chromium checks, all with zero page errors:

| Probe | Passed | Scope |
|---|---:|---|
| Maintained Workbench regression |30|Selection, Shift, cancellation, pair prevention, film split/archived future/reload, quota-write failure, corrupt-storage preservation,390px local scroll/44px targets/complete central button|
| Nine-base combined probe |362|Ring sourced Essence/retry/unsupported gates, all nine bases across Transmutation/Augmentation/Regal/Exalted/Divine/Chaos/Annulment/Alchemy, Sinistral consumption/deactivation, source-model explanations, implicit preservation, all-film reload, actual icon loading|
| Old snapshot regression |11|Captured v18/v19/v24 Stocky films, current v26 craft from a past frame, original future/roll/lock/evidence retention, unknown snapshot preservation and safe refusal, reload|
| Nine-base material-family matrix |214|One reviewed Perfect result per base, Normal refusal, actual Fracture and exact locked-value preservation through Chaos/Annulment/Divine (Belt Divine excluded by its documented boundary), unsupported Perfect target refusal, film reload|

The original legacy browser entrypoint initially failed its obsolete660px assertion. Existing WB-014 already establishes600px to keep the central item button visible. The maintained probe checks current usability rather than the retired geometry. The initial cross-base probe incorrectly expected Belt Divine success; the final probe asserts documented refusal with byte-identical films because the Belt implicit is unknown. These are test-expectation corrections; no product guard or existing unit expectation was weakened.

Machine evidence: [continuation validation](evidence/workbench-continuation-validation-2026-10-03.json). Full synthetic captures/scripts/screenshots stay under `E:\WORK\Exile-Hephaistos\codex\qa-20261003`: `integration-workbench-browser*`, `integration-nine-base-browser*`, `integration-material-family-browser*`, `integration-old-snapshot-browser*`, `ring-narrow.png` and `perfect-mind-retry.webp`. Probe wrappers preserve inherited checks; the old-snapshot expected rule was updated from v25 to the existing v26 Java rule.

The maintained cachepersist smoke also passed before/after normal QA app restart: identical comparisons/assessment and namespace, one persisted hit, zero computed pools,10 states/410 edges. Files are `codex/qa-20261002/cache-before-continuation.json` and `cache-after-continuation.json`; `verify-cache.cjs -continuation` confirms equality. This is existing-regression verification, not Craft Support feature development.

## Limits and next work

Backend check/generateJooq/bootJar and Ring API49 are historical baseline results, not rerun passes in this asset/documentation batch. Backend code/catalog/contracts are unchanged. Windows script tests and Compose config checks were not rerun because those files were unchanged. No browser exhaustive Cartesian product of every material across every base is claimed: the combined matrix covers eight ordinary operations, one consuming Omen and representative Perfect/Fracture/unsupported-target cases across nine bases, plus Ring/legacy interactions. Other old per-material probes and newer malformed-snapshot combinations remain candidates for the next targeted batch.

Keep Workbench ahead of Craft Support/State Explorer feature work. Next priority is representative basic/Greater Essence and multi-Omen combinations on existing eligible bases, followed by independent complete source evidence for the pending44. The bounded Perfect/Fracture matrix is now passed; do not repeat it unchanged merely to increase assertion counts. Do not add more bases to bypass evidence/state gates. QA app restart for the existing cachepersist smoke preserves PostgreSQL/Redis and volumes; no production data, Windows process or original checkout was modified. All temporary containers exit normally with `--rm`. No push, merge or remote deployment.
