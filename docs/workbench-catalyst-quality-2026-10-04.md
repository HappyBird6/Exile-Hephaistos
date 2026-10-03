# Typed catalyst quality foundation — 2026-10-04 KST

This implements already present quality state, bounded derived display and film preservation, not catalyst application. Nine bases, current111/155, pending44 and exclusions65 (including eight preserved Alloys) are unchanged. The thirteen ordinary catalysts remain pending actions.

## Verified evidence and limits

- [PoE2DB Catalysts](https://poe2db.tw/us/Catalysts): thirteen ordinary types target Rings/Amulets and replace other quality types; refined types target Jewels. One active type is represented, never a stack of independent qualities.
- [Quality](https://poe2db.tw/us/Quality): default maximum20. [Breach Ring](https://poe2db.tw/us/Breach_Ring) increases maximum by20; [Refined Breach Ring](https://poe2db.tw/us/Refined_Breach_Ring) by25. These two bases are evidence only, not additional supported catalogs. Existing Solar Essence of the Breach retains its fixed +20 cap modifier, unscaled.
- [PoE2 Wiki Quality](https://www.poe2wiki.net/wiki/Quality): matching modifier magnitudes receive a separate multiplier, after other magnitude changes, followed by truncation. The current bounded model has no Corruption/Sanctification magnitude state and refuses special conditions.
- [PoB Item.lua](https://github.com/PathOfBuildingCommunity/PathOfBuilding-PoE2/blob/dev/src/Classes/Item.lua#L12-L58): independent tag implementation; any matching tag applies the scalar once, missing tags and unscalable modifiers are excluded. This is secondary implementation evidence, not GGG engine certification.

Fresh HTTP200 captures, retrieval times and SHA256 are in `codex/qa-20261003/catalyst-quality/manifest.json`; Wiki revision134140 was captured successfully in project Docker although the web reader could not open it. The raw Item.lua capture and hash pin the mutable dev URL's inspected contents. Numeric eligibility checks are separately recorded in `stat-proof.json`. No new network fetch is performed by the application.

The first current cached-CDN ordinary detail probe returned403. No access restriction was bypassed and no successful fresh255-row recrawl is claimed. All255 approved ordinary definitions were instead directly checked against their existing reviewed captured stat metadata, preserving its provenance/hash and original units; the Backend/Frontend22-unit whitelists agree. The current public `EssenceBreach` detail returned200 and explicitly confirms the cap is unscalable. This is the existing snapshot model, not a replacement with a newly crawled modifier pool.

## Contract

`ItemState.catalystQuality` is optional/null or `{ "type": "FLESH", "amount": 20 }`. Missing/null legacy fields stay unknown/unsupplied; they are not migrated to an assumed zero or catalyst type. A supplied zero retains its type. Types are `FLESH`, `NEURAL`, `CARAPACE`, `UUL_NETOL`, `XOPH`, `TUL`, `ESH`, `CHAYULA`, `REAVER`, `SIBILANT`, `SKITTERING`, `ADAPTIVE`, `NECROTIC`.

Only the existing Solar Amulet and Iron Ring accept this state. Ordinary supported states cap at20; Solar with the actual validated Breach modifier caps at40. Unique, corrupted, mirrored, unidentified and sanctified states remain outside support. Iron Ring now has a reviewed20 maximum; Belt stays unknown and other existing cap policies are unchanged. No Breach Ring base expansion is implied.

`ModifierInstance.values`, source ranges, tags, weights, fractured flags and snapshots stay unchanged. `CatalystQualityDisplay` produces a detached view: for a reviewed matching nonnegative whole-number stat, `floor(original * (100 + amount) / 100)` using integer arithmetic. The approved ordinary single-stat unit list is explicit in Backend/Frontend; numeric source units such as per-minute regeneration, permyriad leech and skill levels are excluded. Unreviewed multi-stat modifiers are retained without scaling. The sole reviewed multi-stat exception is Iron Ring's fixed1/4 physical implicit. Cap modifiers are always unscalable. Unknown/missing tags never inherit a match. No inverse division of copied text attempts to reconstruct lost original rolls.

Projection statuses distinguish `NO_TYPED_QUALITY`, `NO_MATCH`, `UNSCALABLE`, `UNREVIEWED_NUMERIC_SEMANTICS` and `SCALED_INTEGER`. A matching tag is necessary but insufficient to admit a numeric stat. A future extension must verify its source units, unscalable metadata and edge rounding before changing this bounded list.

`POST /api/v1/crafting/workbench/quality-display` accepts `{ "state": <ItemState> }` and returns `ruleVersion`, unchanged concrete `state`, reviewed `qualityLimit`, and original/displayed values and status per modifier. Invalid/unknown state or additional quality fields return Problem Details; fractional/string quality and fractional/string modifier values are rejected before Jackson can coerce them. Existing `quality`/`qualityType` ad hoc fields remain unsupported and preserved in stored archives.

Actions/apply accept valid typed states but refuse every currency interaction, preserving the state, active Omens and empty consumption/roll/event ledgers. Even typed quality0 does not imply a verified interaction. The common affix-only `StateBucket.from` rejects typed quality instead of silently losing it. Support/Explorer are not expanded.

## UI and history

Existing `Edit item → Place base` provides an optional catalyst type and current0..20 amount for Solar/Iron. This supplies already present quality on the existing Normal starting base; it is not a catalyst-use button. The amount follows the reviewed cap of that base. API/history can retain validated Magic/Rare items with original rolls and Solar cap40. Clipboard items retain raw text and remain display-only when quality cannot be mapped losslessly; existing catalog mapping does not reverse truncated values.

The card shows type/amount and computed eligible magnitudes, with original rolls and projection status in modifier details. Alt retains source ranges. Existing film v1 roots and frames preserve quality on save/reload and selection; invalid/future archives are not rewritten. No storage/schema/DB migration and no volume reset.

## Remaining facts

Per-use item-level increments/unique exceptions, exact replacement reset/increment, negative/decimal/mixed-stat rounding, ordinary-currency interaction and Catalysing Exaltation quality-to-weight function remain unverified. PoE1 rarity5/2/1, universal+1, historical weight bonuses and invented1/N rules are not used. Catalyst use and Omen consumption remain deferred.

## Validation

### Final boundary review (2026-10-04 KST)

No new product defect was found in the requested final review; existing successful checks were not rerun. Supporting paths:

- Original rolls are authoritative: `CraftingPage` calls `catalystProjection` with each stored `m.values`; the result is used only for text. Backend `CatalystQualityDisplay` returns a detached map. The original-roll unit test proves29 remains29 while displayed34 is separate; browser type switching, Alt and reload checks preserve film bytes and raw29. Re-rendering does not feed34 into a subsequent projection.
- Multiple matching tags apply once: Frontend's existing defence test combines armour/defences/energyshield and expects29→34, not sequential multipliers. Backend `Type.matches` is boolean `anyMatch`, followed by a single scalar; its multi-tag test and13-type checks passed.
- Unscalable cap stays raw20 (maximum40 with default20), covered by Backend cap test and actual API/browser cap40 checks. Existing Frontend parameterised tests preserve unscalable/missing-tag, negative-domain, non-whole-unit regeneration and unreviewed multi-stat values. Backend approves only source-reviewed integer units and excludes negative source minima/current values. Fractional/string input is rejected before integer coercion in API74; history rejects fractional quality. Decimal/negative/mixed game rounding is not claimed. Iron's sourced fixed1/4 implicit is the sole explicitly reviewed multi-stat exception.
- Existing-quality starting form/reload and typed/legacy film byte preservation are covered by the nine new Frontend tests and browser56. Backend refusal test preserves the same state, Omens and empty ledgers without drawing randomness; API74 covers all thirteen typed states and unchanged refusal; Frontend rejects before fetch.

The refusal boundary is specifically non-null `catalystQuality` on the two supported jewellery catalogs, including explicitly supplied0. It is not a global prohibition on ordinary crafting: API74 includes successful ordinary initial/craft checks for every existing base, and all320 existing Frontend tests passed. No game evidence currently establishes which actions preserve/reset quality or how resulting original magnitudes should behave, so no arbitrary per-action allowance is introduced. Null/missing legacy quality remains unsupplied rather than being guessed. State inspection, starting input, display, film selection/reload and source-range inspection remain usable on typed states. The quality foundation is complete within this contract; per-click catalyst application and typed-quality crafting interactions remain unsupported pending specific evidence. No further UI or guards were added during this final review.

Project Docker Backend spotless/check/generateJooq/bootJar passed354 tests (348 unit, six integration). Frontend npm ci, lint, typecheck, format and build passed. The full run passed all320 existing tests; after correcting two new fixture aliases without weakening validation, the targeted nine new tests passed, giving329 unique passing tests across those runs, not a claimed single329-test rerun. Final CSS-only contrast/focus changes passed format/build and five targeted browser checks.

Runtime API74 assertions and representative browser56 assertions passed with zero page errors; the additional link/focus browser five checks also passed. Actual manual input, all thirteen type displays, reviewed original/derived rolls, cap40/unscalable handling, unchanged refusal, narrow layout and all nine legacy film selections were checked. The nine source catalogs retain1487 original weights/ranges/tags; catalog Git diff is empty. Historical browser993 is not represented as a new run. [Machine-readable completion evidence](evidence/workbench-catalyst-quality-validation-2026-10-04.json) records scope, source hashes and exact run distinctions.
