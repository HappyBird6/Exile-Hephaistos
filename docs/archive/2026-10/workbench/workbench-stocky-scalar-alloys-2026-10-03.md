# Stocky scalar Alloy data batch (2026-10-03)

Expansive, Cyclonic and Mystic are implemented together through the existing Rare replacement engine. No new crafting engine behavior, base, history storage, login, Support or Explorer feature was introduced. Rule `stocky-workbench-scalar-alloys-v24`; numeric ledger v11 remains explicitly unverified.

## Exact current PoE2 source and bounded eligibility

| Material / primary page | Exact glove Code / family | Suffix effect, Global | Modifier Level / effective requirement | Supported scope |
|---|---|---|---|---|
| [Expansive Alloy](https://poe2db.tw/us/Expansive_Alloy) | [AlloyRemnantPickupRange1](https://poe2db.tw/us/hover?s=Data%5CMods%2FAlloyRemnantPickupRange1), RemnantPickupRadius | Remnants collected from 35–50% further away | 25 / 20 | Stocky ilvl25+ |
| [Cyclonic Alloy](https://poe2db.tw/us/Cyclonic_Alloy) | [AlloyDamagingAilmentDuration1](https://poe2db.tw/us/hover?s=Data%5CMods%2FAlloyDamagingAilmentDuration1), DamagingAilmentDuration | Damaging Ailment Duration on Enemies +20–25% | 45 / 36 | Stocky ilvl45+ |
| [Mystic Alloy](https://poe2db.tw/us/Mystic_Alloy) | [AlloyAttackAreaOfEffect1](https://poe2db.tw/us/hover?s=Data%5CMods%2FAlloyAttackAreaOfEffect1), IncreasedAttackAreaOfEffect | Area of Effect for Attacks +10–15% | 45 / 36 | Stocky ilvl45+ |

Current primary item-page glove entries match the individually captured detail rows (IsAlloy/Removes true). The material card establishes Rare random removal and guaranteed addition; every material has one proven glove result. Other equipment outcomes and similarly displayed Fists of Stone alternatives are excluded. Source name is of the Stars for all three; family identity, not displayed name, controls conflicts.

Level fields are not interchangeable: Expansive DropLevel23/effective requirement20 do not establish forced minimum item level. Conservative catalog levels25/45 bound implementation; lower use remains safely unsupported and unresolved in WB-017. No game minimum is claimed. Each modifier's effect is assigned/displayed; collection distance, actual ailment timing and attack area calculations are absent and labeled. Solar support is not inferred from glove Codes.

## Data and invariants

One checksum-validated extension (`scalar-alloys.catalog.json` / `.raw.json` / `-material-proof.json`) adds three definitions to the unchanged engine. Raw SHA-256 `1d0316cd396eee84f10d662c3b7ea220fbd93b56b66adbf332fd6a81a46d436b`. All ordinary spawn weights are zero for these special definitions, not special-selection probabilities.

Every eligible unlocked removal branch must fit suffix capacity and family before application; invalid branches are never pruned. Each exact target has probability1. Eligible removal retains the explicit uniform1/N assumption. Source integer rolls35..50/n16,20..25/n6,10..15/n6 remain WB-003 UNVERIFIED numeric models; source bounds are not proof of precision or game distribution. Client requires matching stat, endpoints, integer count, exact source and unverified label.

Alloy is not Essence: Crystallisation (individual or pair) is unrelated, unconsumed and cannot restrict removal. Existing global UI pair prevention remains for conflicting Essence use. Locks and untouched values persist. Actual Divine/Chaos/Annul retain a Fractured special modifier, while ordinary Chaos additions stay positive-weight.

Current Stocky snapshot `stocky-special-b07ca8d46cd920bfd4e6cd7890602983e4abd38a31f72f6a811caecb05389e5d`; compatible identities retain normal, Abyss, Horror, Perfect v22 and Prismatic v23. Saved films retain their original evidence and future frames. Actual crafting from the past starts a new linear film and upgrades only validated identities; unknown archives remain untouched.

## Counting definitions

Registry220 means registered inventory entries, not the entire game list and not implemented materials. Unique implementation counts each IMPLEMENTED material ID once across bases: now71 =19 Currency+11 Omen+36 Essence+5 Alloy, up3 from68.

Per-base support records count implemented materials supporting that base (including omens): Stocky64, up3 from61; Solar60 unchanged. Actions are executable currency/material operations, excluding11 omen records: Stocky53, up3 from50; Solar49 unchanged. Stocky definitions191 =182 positive ordinary +9 zero-spawn special, up3 from188. Ordinary prefix/suffix weights remain63,700/84,500. Cross-base support counts are never summed as unique implementation counts. Actual registry/action/catalog APIs check these separately.

## Verification and execution scope

All checks/runtime use project Docker and isolated QA services. Existing databases/volumes and original master remain preserved; local checkpoint only.

- Fast affected FE:16 tests/four Alloy files plus typecheck, all pass,22.71s.
- Fast affected BE:27 tests,26 pass; one existing test expected v23 instead of v24. Corrected before the single final whole-project check. First disposable Java container spent time downloading Gradle; final retained checker reuses its cache.
- Final BE:231 tests =225 unit +six integration, zero failure/error/skip, plus spotless check, jOOQ generation and bootJar.
- Final FE:lint/typecheck/format/full test/build;145 tests/28files pass. One whole FE test run for all three materials,100.35s. jsdom creation remains72% of tracked time; no unrelated infrastructure/test-performance redesign here.
- One combined new-Alloy Chromium script:70 assertions, zero page errors. All three actual material flows use conservative supported levels and active Sinistral Crystallisation. Normal/Magic/lower-ilvl refusal, prefix/suffix complete candidate calculation, exact target/numeric evidence, unaffected rolls/Fractures, unconsumed omens/conflict UI, Shift repeat/failed-repeat preservation, Alt range/keyup, assumptions, film/reload and390px geometry pass. Expansive narrow screenshot visually reviewed; central use target and card/boundary readable.
- Actual API independently reaches and Fractures each of the three targets and exercises Divine/Chaos/Annul lock and ordinary-pool preservation.
- Old v23 actual-film browser:11 assertions, zero page errors. Viewing past creates no film; actual craft branches into a new linear film preserving original future, evidence, inactive earlier archives and unknown snapshots; reload retains everything.
- Staged Git archive in Docker verifies Horror/Perfect/Prismatic raw checksum preservation and new batch SHA, exact three Codes, Alloy/removal flags, zero ordinary weight, Global locality and all three primary material proofs. Unfiltered new raw blob matches index.

QA evidence: `codex/qa-20261003/stocky-scalar-alloys-*`. JAR SHA256 `6416411adef6c919eeda2694619612fb50e492b1241c9b000fa3b9ff3f05ce37`; actual image bundle `index-C9EwfUWZ.js`, frontend manifest `fa018261cddb9422a7e4e489326ad354db1fb7391abafcf0f41b6d9f0e8ad721`.

Not rerun: prior Prismatic/Runic/material-specific full browser matrices, broad Support/Explorer screens, cachepersist and every historical snapshot browser. Shared engine paths are covered by final backend/frontend suites; affected new-Alloy and immediately prior film paths received actual browser tests once at the batch boundary. No actual-game measurement of lower-ilvl eligibility, numeric precision/distribution or computed effects. Narrower than390px was not newly verified.

## Remaining independent work

Adaptive has a source-proven conditional missing-Ward attack-speed effect; Swift has source-proven distinct CastSpeed and AttackSpeed families; Sovereign has source-proven Local Ward effect with property/quality boundary. Their captured source is not activation. Next review their per-material conditions and family/property invariants together; do not make missing state into invented currency prerequisites. WB-003 numeric, WB-004/005 computed/quality/special-state and WB-017 lower-level eligibility remain separate. WB-010 still blocks only the uncertain complete basic Infinite outcome set. Continue independent confirmed work without broad unchanged-screen regression per data row.