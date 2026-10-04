# Stocky Prismatic Alloy checkpoint (2026-10-03)

Prismatic Alloy now uses the existing Rare replacement engine on Stocky Mitts. Solar remains unsupported for this material. Current rule: `stocky-workbench-prismatic-v23`; numeric ledger stays `stocky-unverified-numeric-assumptions-v11`. This is an incremental Workbench checkpoint, not completion of equipment crafting.

## Primary evidence and implementation boundary

| Evidence | Verified source field | Implementation |
|---|---|---|
| [Current PoE2DB material](https://poe2db.tw/us/Prismatic_Alloy) | Gloves: Damage Penetrates 9–15% Elemental Resistances, Prefix, Required Level 36; Rare random removal + guaranteed addition in captured material card | Stocky Rare replacement, single exact prefix |
| [Exact glove definition](https://poe2db.tw/us/hover?s=Data%5CMods%2FAlloyElementalPenetration1) | Code AlloyElementalPenetration1; name of the Stars; Prefix; family ElementalPenetration; Global reduce enemy elemental resistance % 9..15; ordinary weight 0 | Dedicated special definition, excluded from ordinary addition |
| Detailed row | Modifier Level 45, effective requirement 36, IsAlloy true, Removes true | Conservative supported ilvl 45+; lower eligibility remains WB-017, not a proven game minimum |
| Source probability limits | One guaranteed target; removal weights and numeric grain/distribution unpublished | Target probability 1; complete eligible-removal uniform 1/N ledger; numeric integers 9..15/n=7 explicitly UNVERIFIED under WB-003 |

DropLevel 45, effective/Required Level 36 and Runeshape Lv70 are distinct source fields, not interchangeable currency-use requirements. Other equipment results on the material page do not authorize other bases. The alternative Fists of Stone resistance effect is not this Alloy's effect. No resistance, damage, combat, quality or socket calculation is introduced. UI identifies this affix-only boundary.

Source resources: `prismatic-alloy.catalog.json`, `.raw.json`, and `-material-proof.json` under `backend/src/main/resources/catalog/stocky-mitts`. Detailed raw SHA-256: `38425decc7fc27e25aba0ff15d06b640f628387b32b61c9cee90b7b673d2a873`; material HTML SHA-256: `49970de77066d3460369d4a54d36ba0f452fc4c51e1c40e429b3c307dbda1978`. New raw bytes use LF before checksum computation; Git-tree archive rechecks original Horror, Perfect and new Alloy proof bytes in Docker.

All eligible unlocked removal branches must accept the new prefix with capacity/family checks; invalid branches are not silently pruned. Unchanged rolls and Fractures persist. Alloy is not Essence: neither individual Crystallisation nor the pair restricts removal or is consumed on Alloy. API/contract tests exercise the pair; the existing UI prevents selecting this pair globally for supported Essence conflicts. The same Alloy classification also preserves Solar Runic behavior.

Current Stocky snapshot: `stocky-special-cd0fbb24fa8e73d482b4baab53fd1b0d058197cc4b7766be9646c03ebc6cfdca`. Four compatible identities retain normal, Abyss, Horror and v22 Perfect. Old-film viewing is byte-preserving; actual crafting upgrades validated identity while preserving original evidence, future frames and inactive archives.

## Executed verification

All runtime/tests below used project Docker and isolated QA services. Original database/volumes preserved; no push, merge or deployment.

- BE final `spotlessApply check generateJooq bootJar`: 219 tests = 213 unit + six integration; zero failures/errors/skips.
- FE final lint/typecheck/format check/full test/build: 133 tests in 25 files, all pass. New four contract tests reject forged consumption, numeric evidence, bounds, lower level and Solar results.
- Actual Chromium Prismatic flow: 54 assertions, zero page errors. Normal/Magic/lower-ilvl refusal, Rare application with none/either Crystallisation, exact candidates/model, Fracture/untouched rolls, active red border/conflict UI, Shift repeat, failure preservation, Alt-held bounds/keyup restore, ledger, film/reload and 390px geometry. Narrow screenshot visually inspected: central button fully visible, card/boundary readable, local inventory scrolling retained.
- Shared Solar Runic Alloy regression: 48 assertions, zero page errors, including unrelated Crystallisation, repeat, film/reload and narrow display.
- Actual old v21 and v22 snapshot/history: 11 assertions each, zero page errors; past viewing, craft-created film, preserved future/evidence/inactive/unknown archives and reload.
- API scope: registry 220 registered, 68 unique implemented = 19 Currency + 11 Omen + 36 Essence + two Alloy. Solar 60 support records/49 actions/218 definitions; Stocky 61 records/50 actions/188 definitions. Cross-base support records are not summed as implementation counts.
- API locks: actual Alloy addition followed by Fracturing, Divine, Chaos and Annul; locked affix and values preserved. Ordinary pool remains 182 positive definitions, weights prefix 63,700/suffix 84,500; six special definitions weight zero. No ordinary pool contamination.
- Git staged archive: checksum/raw-code/type/locality/provenance of original Horror/Perfect and new Prismatic resources verified in Docker; unfiltered raw blob equals staged blob.

Evidence is under `codex/qa-20261003/stocky-prismatic-*`. Runtime JAR SHA-256 `c6e1f4db0e5a543327e87b48ea154361fe83f2e55a630dd2c849ee51485f059f`; frontend bundle `index-WzRVfM-9.js`, image manifest `2619e7c3f2ada76b663b76c93399bc133eb6e5d438d9cfe7e63f2b646a5e5d1a`.

Not rerun: every earlier material's individual browser matrix, broad Support/Explorer screens and cache-persistence browser. No change to their UI/history/cache paths; full unit coverage plus affected Alloy and history regressions were run. No game observation of lower-ilvl eligibility, numeric precision/distribution or calculated penetration damage. Other six glove Alloys are source-audited, not activated at this checkpoint.

## Observed time and next batching

The first full backend run took 2m30 and failed on my accidentally increased Solar action-count expectation (runtime correctly retained 49). Corrected final full backend took 4m26 including integration/codegen. FE full runs took 68.11s and 92.24s; jsdom environment creation represented 69% of tracked time. Production frontend image's build step took about seven seconds. These observations identify full-suite/environment and repeated validation overhead as the main visible cost; no claim is made that source research or browser time was separately profiled.

For future data additions, first run parameterized affected backend/client tests for all materials together, then one whole-project check and one shared browser flow at the batch boundary. UI/history changes still get their affected browser cases. Preserve source checksums and per-material family, slot, level, exact-target, ordinary-weight, lock and omen tests. Do not repeat unrelated screens for each row.

Next source-backed single-effect batch candidates: Expansive (`AlloyRemnantPickupRange1`, suffix35..50, level25), Cyclonic (`AlloyDamagingAilmentDuration1`, suffix20..25, level45), Mystic (`AlloyAttackAreaOfEffect1`, suffix10..15, level45). These share the verified Alloy replacement route, with effect assignment only and source-specific boundaries. Adaptive's conditional missing-Ward effect, Swift's two families and Sovereign's Local Ward/property boundary can follow once exact per-material eligibility/invariants are represented; they need not block the three independent candidates. WB-017 lower-ilvl uncertainty, WB-003 numeric model, WB-004/005 computed state and WB-010 Infinite eligible pool remain separate unresolved work.