# Stocky Mitts: Perfect Grounding and Perfect Opulence

This activates two source-proven glove affixes on the existing Stocky Mitts base, under the conservative supported item-level boundary below. Solar Amulet does not list or apply these targets. No additional glove bases or character/combat/loot simulator are added.

## Source evidence and eligibility boundary

| Material | Primary target | Exact modifier evidence | Boundary |
|---|---|---|---|
| [Perfect Grounding](https://poe2db.tw/us/Perfect_Essence_of_Grounding) | Gloves; Lightning damage Recoup fraction 26..30 percent | [EssenceLightningRecoupLife1](https://poe2db.tw/us/hover?s=Data%5CMods%2FEssenceLightningRecoupLife1), suffix, `LightningDamageTakenRecoupedAsLife`, Global stat `lightning damage taken goes to life over 4 seconds %` | Affix assignment only; recovery amount/timing is not simulated. The internal stat caption does not establish a simulated four-second duration. |
| [Perfect Opulence](https://poe2db.tw/us/Perfect_Essence_of_Opulence) | Gloves; extra Gold quantity from slain enemies 10..15 percent | [EssenceGoldDropped1](https://poe2db.tw/us/hover?s=Data%5CMods%2FEssenceGoldDropped1), suffix, `EssenceGoldDropped`, Global stat `gold +% from enemies` | Affix assignment only; no Gold-drop or item-rarity multiplication is calculated. |

Both current primary material cards specify Rare replacement: remove one modifier and add the guaranteed effect. Their captured glove rows have `IsPerfect: 1`, `Removes: true`, modifier `Level: 72`, `DropChance: 0`, and `reqlvl: 57`. Detailed pages say level 72 with effective requirement 57; the material tables display Required Level 57. **57 is not asserted as the currency's minimum item level.** The current catalog's supported modifier-level convention activates these results for **ilvl 72+**. Lower item-level use remains unverified/unsupported (WB-017), rather than being declared illegal in the actual game.

Proofs captured 2026-10-03: detailed Grounding 06:33:03.656Z and Opulence 06:33:04.092Z; material cards 06:50:18.15Z and 06:50:18.998Z. The primary pages were also cross-checked through browsing during this stage. Full retained row/HTML/parsed details are `catalog/stocky-mitts/perfect-grounding-opulence.raw.json`; primary `materialBlocks` and page URL/retrieval/SHA are `perfect-grounding-opulence-material-proof.json`. Same-name monster skills and the detailed Fists of Stone transformation blocks do not define these affixes. No independent official patch-version or lower-ilvl eligibility proof is claimed.

## Implementation and probability boundaries

- Rule `stocky-workbench-perfect-glove-v22`; numeric ledger remains `stocky-unverified-numeric-assumptions-v11`.
- Rare, supported ilvl 72+, at least one eligible unlocked explicit; every eligible removal branch must leave room for the suffix and its family. A surviving same-family target blocks the entire action; a sole unlocked target can replace itself. No removal branches are pruned to make an action fit.
- Sinistral/Dextral Crystallisation restrict removal to the matching side. The pair is blocked; matching consumption occurs only on success. Untouched rolls, Fractures and unrelated omens are preserved. The two different families allow Grounding and Opulence to coexist.
- One guaranteed target has selection probability 1. Removal remains the explicitly recorded unpublished 1/N assumption. Variable scalar ranges use the existing **UNVERIFIED source-unit integer model**: five modeled Grounding values or six Opulence values. Endpoints do not prove step size, display precision or game distribution; no modeled probability is promoted to game evidence.
- Client acceptance additionally requires the exact numeric model ID, stat binding, min/max/count, empty outcome-choice list, detailed primary URL and UNVERIFIED label. It rejects missing/duplicate/altered evidence, values outside the modeled range, unsupported ilvl/base and the other material's target. This does not invent a combat or loot calculation.
- Screen text explicitly states the supported level and computation limit. Normal display shows the rolled percentage, Alt shows the source bounds, keyup restores the roll, and the full assumption ledger survives real history/reload.

## Catalog, history and source-byte preservation

Current Stocky definitions: **187 = 182 unchanged positive-weight ordinary + five dedicated zero-spawn** (two Abyss, Horror, Grounding, Opulence). Normal prefix/suffix weights remain **63,700/84,500**. Hysteria's independently published ordinary weight 500 remains intact. No special-only result enters Alchemy/Chaos/Exalted weighted additions.

New raw SHA-256 `27d72d44f22ce03d63626b3a93d49d1430fdd833a2dae5fc4c9760962a688d30`; new files are written with LF before checksum calculation. Horror raw bytes and its narrow Git attribute remain unchanged: SHA-256 `871c235ca9af82a1b4caccdfdcfe27ad28ba57c2946f35417dc270eb68e1d50f`. The Git-tree archive was extracted and read in Docker, proving both stored resources match their reviewed catalog checksums and retain source Code/HTML/locality/material provenance. No newline workaround edits or discards source evidence.

Active snapshot `stocky-special-f8caf300c834d9cf59ae1e889cc5100a000f3966025ddb55ac07f58529d79111`; only intact normal, Abyss and Horror snapshots are compatible. The previous 185 definitions and base properties are retained exactly. Restore/view does not rewrite stored bytes. Actual crafting may upgrade validated state snapshot identity only; original source evidence/future frames/inactive archives remain intact. Unknown snapshots are retained and refused rather than rewritten. Solar snapshot and 218 definitions are unchanged.

Actual APIs confirm **67 unique implemented materials of 220 registered**: 19 currencies, 11 omens, 36 Essences and one Alloy. Stocky **60 support records/49 actions**, Solar **60/49**. Do not sum per-base records or treat the registration inventory as the full game list.

## Executed verification

- Docker BE `spotlessApply check generateJooq bootJar`: **215 = 209 unit + six integration**, zero failures/errors/skips. Seven new tests cover exact sources/levels/families, all modeled integer/removal outcomes, conservative level/slot/family/lock blocks, omens, coexistence, both Fractures and subsequent Divine/Chaos/Annul, exact prior catalog/source checksum and Solar rejection. Existing full backend regression passes.
- Docker FE lint/typecheck/format/full tests/build: **129 tests in 24 files**, all pass. Eight new contracts include ledger provenance/label preservation, unsupported level/base, bounds/fraction errors, exact percent display and wrong material/omen response rejection. Existing history storage-failure/capacity/cancellation and older material contracts remain included.
- Actual Docker Chromium new materials: **107 checks**, zero page errors. Six material/omen contexts, real ilvl 72 Alchemy/Fracturing preparation, normal/magic and ilvl 57/71 refusal, complete removal evidence, exact new modeled domain/UNVERIFIED ledger, lock/other-roll preservation, matching-only consumption/pair prevention, Shift/repeat eligibility, frame/reload, Alt source bounds, computation limits and 390px central-button/material geometry. Both screenshots were visually reviewed.
- Actual history compatibility: **11 checks**, zero page errors. Constructed wrappers use three captured **successful** v21 Fracture/Horror/repeat frames, with an old past selected and a successful original future retained. Restore/view preserves bytes/evidence; actual v22 Divine forks a linear film and upgrades only approved identities. v18/v19 inactive archives, unknown-snapshot refusal/preservation and reload are checked.
- Actual current Stocky general **60**, prior Horror **49**, Solar Workbench **30**, cross-feature **21**, all zero page errors. Counts reflect this run, including bounded preparation attempts.
- Actual API probe reaches and Fractures both new affixes. Divine/Chaos/Annul retain their exact locked values; replacement with a surviving locked family is refused; Alchemy/Chaos ordinary additions stay positive-weight. This verifies the simulator, not game frequencies.
- Git-tree source archive integrity passed in Docker: old/new reviewed SHA values, retained raw source Code/HTML/locality, both material card records and timestamps/URLs.

Evidence under `codex/qa-20261003` uses the `stocky-perfect-glove-` prefix: backend/artifact/runtime-scope/source-archive validation JSON, browser/snapshot/pool-lock/general-regression/Horror-regression/Workbench/cross-feature results and screenshots. Final immutable QA JAR `codex/qa-20261002/runtime/poe2craft-aa554db275c1.jar`, SHA-256 `aa554db275c1cef134258896a7c7767c8436996e691daf50fd3913dfc2601ba9`; the artifact record is `stocky-perfect-glove-artifact-validation.json`; frontend bundle `index-qPv6hsf8.js`, image manifest `748a1702fac6bbb9b770f8701cc05ecd2ec4cc07ecac631767f26996bf5e26e5`.

Not rerun: separate full basic/special Essence browser matrices, dedicated older malformed-evidence migration browsers and cache-persistence smoke. Their execution paths/cache mechanisms were not changed; current full unit suites and the affected Workbench/Horror/Solar/history browsers were run. No actual game precision/distribution, lower-ilvl eligibility, recovery timing or Gold-drop calculation was tested. A final registry wording-only adjustment uses “published effective requirement 57”; Docker `bootJar` repacks it without changing executable rules or repeating unchanged test suites.

Original DB/volumes and master remain preserved. Only isolated QA app/frontend are refreshed. No remote push/merge/deployment. Rollback must preserve user films and proof resources; a catalog without these affixes cannot safely apply a saved item containing them.

## Next independent work

The [seven source-reviewed Alloys](workbench-stocky-special-evidence-2026-10-03.md) remain candidates. Start with a simple source-proven single-result modifier; separate conditional effects from use prerequisites, retain Swift's two family conflicts and Sovereign's Local Ward/scaling boundary. WB-010 basic Infinite remains blocked; WB-003 precision and WB-004/WB-005 property/scaling/special-state gaps remain open. WB-017 lower-ilvl eligibility does not block independently supported higher-level work.
