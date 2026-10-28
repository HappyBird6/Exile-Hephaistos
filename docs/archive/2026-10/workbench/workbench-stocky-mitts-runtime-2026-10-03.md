# Stocky Mitts Workbench activation — 2026-10-03

This stage connects the complete 182-row ordinary catalog to Workbench only. Solar's existing catalog and Support/Explorer engines remain separate. It does not complete every equipment crafting method or verify numeric game distributions.

| Area | Supported boundary |
|---|---|
| Base | Stocky Mitts, item level 1–100, no implicit; base Armour 15 is separately labelled, no computed total |
| Ordinary currency | 19 existing currencies with full published pool, rarity/slot/family/minimum-level rules |
| Basic Essence | Lesser/regular/Greater Enhancement and Greater Battle: Magic → Rare, one guaranteed prefix, conservative catalog-level/family/slot gate |
| Omens | Existing individually verified operation/affix rules; conflicting same-operation omens blocked, unrelated omens retained. Blessed + Divine has no numeric target on this base and is blocked. Crystallisation does not make an unsupported replacement essence available |
| Fracturing | Rare with four or more explicits, one lock; Divine/Chaos/Annulment preserve lock and values |
| Films | Same version-1 localStorage repository, original per-frame evidence; base-aware reload/archive selection, linear new film when actually crafting from past, original future preserved |
| Other scope | Other glove essences/special means, quality/socket transformations, computed Armour, pasted glove mapping and glove Support/Explorer are unsupported |

The four Essence Codes, affix types, levels, families, ranges and source URLs are in [the preparation evidence](workbench-stocky-mitts-preparation-2026-10-02.md). The [complete catalog recovery](workbench-stocky-mitts-catalog-2026-10-03.md) preserves every normal row and published weight. Registry entries now include supportedBases; four new implemented entries are stocky-only. Global registry registration remains incomplete and does not imply current obtainability.

Numeric boundaries and source IDs are evidence-backed. Interior numeric increments, conversion/rounding and distributions remain WB-003 OPEN. Under the user's reversible-model direction, single stats use explicitly unverified assumed-source-integer-roll-v1 (uniform inclusive source-unit integers). All 42 compound rows use the approved user-coupled-ratio-half-up-v1 (10001 shared ticks, HALF_UP). These count model candidates, not established game outcome sets. No rows are pruned, no missing weights are mixed and no Cartesian joint domain is asserted. Scalar text is substituted only when source/display bounds agree exactly; otherwise values use labelled source units. Alt shows the captured full ranges. Model assumptions, selected ratios and original rule/ledger are persisted per frame.

Runtime identifiers: stocky-workbench-source-model-v17 / stocky-unverified-numeric-assumptions-v11. Solar retains solar-workbench-abyss-essence-v16 / solar-uniform-assumptions-v10. Remove stocky service dispatch/selector to roll back activation; saved glove films stay preserved rather than being rewritten as Solar.

WorkbenchService owns explicit base dispatch. Unknown base/snapshot states are rejected; base-specific unsupported materials return unchanged items. Default ItemCatalog bean, text mapper and Support/Explorer keep Solar. StateBucket accepts zero or one implicit structurally, while catalog validation still enforces the exact base requirement. FE validates zero implicit and the exact glove base on glove initial responses; history validation counts the first explicit correctly without a preceding implicit. Pending base/session loads prevent concurrent crafts and stale placements, and are invalidated at page exit. New tests protect wrong-base responses and post-unmount storage/draft preservation.

A narrow pointer regression was discovered during actual browser evaluation (WB-009): tooltip clamping could cover its own currency trigger. The correction uses side space first, then constrained scrollable space above/below the trigger, preserving keyboard description and source links. Final browser and FE results are recorded in ISSUES.md after execution.

Docker backend full check/format/integration/generateJooq/bootJar: 189 tests (183 unit, six integration), zero failed/error/skipped. Immutable QA JAR SHA256 3cec0cba67b2e9d75a88c9f1c1fe59c41881c20fb8424b6739c964f22d287b58, qa-20261002/runtime/poe2craft-3cec0cba67b2.jar. QA results and scripts are in codex/qa-20261003. All original DB/volumes are preserved; only isolated QA app/frontend changed. No remote push, merge or deployment.

Final activation validation: FE lint/typecheck/format/full 110 tests/build passed. Final actual Chromium: Stocky 60, Solar general 30, Solar special cross-feature 25, all zero page errors. Fresh API scope: registry 220/64 implemented (19 currency, 11 omen, 33 essence, one alloy), Solar 60 and Stocky 32 supported; action listings Solar 49 / Stocky 23. Explicit numeric model status remains unverified. Historical evidence migration/invalid-evidence/browser cache persistence suites were not rerun in this stage; full FE regressions and actual session/archive/reload checks did run. Browser setup attempts affect cross-feature check count. Original narrow pointer failure and successful identical probe are retained as evidence, rather than counted as passes.

Final self-review corrected the Stocky preset text header to Gloves/Stocky Mitts instead of Solar. Full FE 110 and actual Stocky 60 passed after this Stocky-only text correction; the immediately preceding Solar general 30 and special cross-feature 25 were not repeated after that text-only correction (Solar and tooltip behavior unchanged). Final FE log: codex/qa-20261003/stocky-frontend-final-check.log. Original checkout remains clean master e937ddccf7493ea0d114139142ff36224c3a672c.

## Subsequent fixed-Essence checkpoint

The v18 extension adds 21 already-source-verified basic Essence results on the same glove pool, increasing supported fixed Essences 4 -> 25 and supported registry records 32 -> 53. [Latest evidence and validation](workbench-stocky-basic-essences-2026-10-03.md) supersedes the active scope above; the v17 results remain historical evidence.
