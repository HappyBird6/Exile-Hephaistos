# Adaptive, Swift and Sovereign Alloy bounded assignment (2026-10-03)

This batch implements three source-proven Stocky glove modifier assignments through the existing Rare replacement engine. It does not calculate character Ward conditions, attack/cast speed, Local Ward totals or Solar resistance magnitudes. Rule `stocky-workbench-reviewed-alloys-v25`; numeric ledger remains `stocky-unverified-numeric-assumptions-v11`.

## Classification and primary evidence

| Material / current PoE2 page | Exact glove Code / effect | Slot / family / locality | Catalog level / effective requirement | Required engine change and supported boundary |
|---|---|---|---|---|
| [Adaptive Alloy](https://poe2db.tw/us/Adaptive_Alloy) | [AlloyAttackSpeedIfMissingWardRecently1](https://poe2db.tw/us/hover?s=Data%5CMods%2FAlloyAttackSpeedIfMissingWardRecently1); Attack Speed +10–15% **while missing Runic Ward** | Suffix; IncreasedAttackSpeed; Global | 25 /20 | No new engine path. Stocky ilvl25+; preserve conditional text and never infer current missing-Ward state or invent a currency-use prerequisite |
| [Swift Alloy](https://poe2db.tw/us/Swift_Alloy) | [AlloyCastSpeedGloves1](https://poe2db.tw/us/hover?s=Data%5CMods%2FAlloyCastSpeedGloves1); Cast Speed +9–12% | Suffix; **IncreasedCastSpeed and IncreasedAttackSpeed**; Global | 45 /36 | Existing any-family overlap validation; one scalar stat, not two coupled speed rolls. Stocky ilvl45+ |
| [Sovereign Alloy](https://poe2db.tw/us/Sovereign_Alloy) | [AlloyLocalWardIncreasePercent1](https://poe2db.tw/us/hover?s=Data%5CMods%2FAlloyLocalWardIncreasePercent1); Runic Ward +24–30% | Prefix; LocalRunicWardPercent; **Local**; name Verisium | 25 /20 | No computed-property engine added. Stocky ilvl25+ affix assignment only; no Ward total, quality/socket scaling or additional Runic-Armour-only prerequisite inferred |

Each primary material card says Rare random removal and guaranteed addition. Adaptive/Swift cards explicitly list Gloves; Sovereign card says Armour and the current public item table explicitly lists Gloves. The strength-glove row independently identifies the exact target, IsAlloy/Removes true and ordinary spawn0. Different weapon/jewellery outcomes and Fists of Stone alternatives are excluded.

Required/effective levels are not proven forced-currency minimum item levels. Adaptive DropLevel23 and Sovereign DropLevel65 are also distinct fields, not eligible-item minimums. The implementation conservatively retains catalog levels25/45; lower eligibility stays WB-017, not an asserted game prohibition. Neither the absence of a computed Ward property nor an unevaluated conditional effect is turned into an invented use condition.

All three actions remain unsupported on Solar. Sovereign's source-proven Amulet result is instead +20–30% Explicit Resistance Modifier magnitudes: it needs a separately identified source target and a magnitude-scaling boundary. That independent work is WB-021. The glove Local Ward Code is never copied to Solar.

## Data and preserved rules

A single checksum-validated `reviewed-alloys.catalog.json` / `.raw.json` / `-material-proof.json` extension adds three zero-spawn definitions. Raw SHA256 `9d12e4a4beed7235a43dc682bca8d3b38761bcb5d7fbb83df029be51f591df4d`. Exact source families/tags/text remain data; the replacement engine is unchanged.

Every eligible unlocked removal branch must fit the target's affix capacity and every family. No invalid branch is pruned. Swift cannot coexist with ordinary attack-speed tiers or Adaptive even though its display only says Cast Speed. Its two families also exclude conflicting ordinary additions afterward. A sole unlocked conflicting attack-speed instance can be replaced; surviving or Fractured conflicts safely block. Synthetic unit fixtures isolate either Swift family individually and do not claim new game modifiers.

One guaranteed target has probability1. Removal uniform1/N remains an explicit assumption. Numeric integers Adaptive10..15/n6, Swift9..12/n4, Sovereign24..30/n7 remain WB-003 UNVERIFIED source-unit models; source bounds do not establish precision or distribution. Client requires exact numeric stat/range/count/source and unverified label and rejects preserved-instance family overlaps.

Crystallisation is unrelated to every Alloy and remains unconsumed, including the API pair. Existing UI prevents activating the conflicting Essence pair. All unaffected rolls, Fractures, conditions and implicits persist. No implicit Ward value or character condition is generated. UI shows complete conditional source text and a separate assignment-only boundary.

Current Stocky snapshot `stocky-special-dccae2f3af261af24a1ff568f74f78e7561c8355aeed00f66e63c913e26cb075`; six compatible identities preserve normal, Abyss, Horror, Perfect v22, Prismatic v23 and scalar-Alloy v24. Viewing old films is byte-preserving; actual craft from the past creates a new linear film, retaining original future and evidence. Unknown archives remain untouched.

## Actual scope and verification

Registry220 is registered inventory, not full game inventory or implementation. Unique IMPLEMENTED material IDs across bases now74 =19 Currency+11 Omen+36 Essence+8 Alloy. Stocky67 support records/56 actions, Solar60/49 unchanged. Support records include11 omens; executable actions exclude them. Stocky194 definitions =182 unchanged positive ordinary +12 zero-spawn special; normal weight totals63,700 prefix/84,500 suffix. Cross-base records are never summed as implementation counts.

All tests/runtime used project Docker and isolated QA services; temporary/cache/evidence files live under codex. Existing DB/volumes and original master preserved; no remote action or deployment.

- Fast affected BE26 tests passed,1m34 using retained Gradle cache copied under codex. Fast FE19 tests/two files +typecheck passed,15.26s. Per-material tests are parameterized for this batch.
- Final BE246 =240 unit+six integration, zero failures/errors/skips, plus spotless check, jOOQ and bootJar. One full backend run,4m59.
- Final FE160 tests/29files, lint/typecheck/format/build all pass. One whole frontend check for the batch.
- One combined actual Chromium flow70 assertions, zero page errors: all three supported-level crafts, active Crystallisation remains, normal/magic/lower-ilvl refusal, exact removal candidates/target/model, untouched rolls/locks, Shift/repeat/error, Alt range/keyup, ledger, films/reload and390px geometry. Adaptive/Sovereign narrow screenshots visually inspected: complete conditional/Local effect and unsupported-calculation text are readable; central use target is fully visible.
- Actual per-target API reaches/Fractures all three effects and preserves locked values through Divine/Chaos/Annul; ordinary additions remain positive-weight.
- Additional actual API invariant probe: ordinary attack-speed/Fractured attack-speed/Adaptive conflicts block Swift safely; Swift blocks Adaptive; sole ordinary conflict can be replaced by Swift; both Crystallisation omens remain unconsumed; condition/implicit state unchanged; all three Solar requests refuse safely. An initial manually constructed fixture compared noncanonical attribute ordering and an undefined property; fixture was corrected to server order/JSON form, with no application change.
- Old v24 actual-film browser11 assertions, zero page errors: past viewing, new linear film on craft, original future/evidence/inactive/unknown archives and reload preserved.
- Staged Git archive in Docker validates prior Horror/Perfect/Prismatic/scalar proofs and new reviewed checksum, exact three Codes, Alloy/removal/zero-weight flags, conditional text, Swift two families, Local Ward/name and all primary material applicability evidence. Unfiltered raw blob equals staged blob.

Evidence: `codex/qa-20261003/stocky-reviewed-alloys-*`. JAR SHA256 `ac5f6352fcc65cff8443b6ee5a8510e884a14e10602a9f7db43b552857f2cd64`; bundle `index-BNZtPr_K.js`; frontend manifest `6d742e652e9d52f83e90751f0f48b250da6b8795164fca72500fdadf3ad51c28`.

Not rerun: previous materials' individual browser matrices, every historical film browser, broad Support/Explorer screens or cachepersist. Shared behavior is covered by whole backend/client suites; affected new flows and immediately prior film received browser verification at the batch boundary. No actual-game precision/distribution/lower-level or combat/property observations. Narrower than390px not newly verified. Full crafting remains incomplete: WB-003 numeric models, WB-004/005 computed/quality/special state, WB-010 Infinite pool and WB-017 lower eligibility remain unresolved.

Next independent scope: source review of remaining equipment-relevant omens/Essences and exact Solar Sovereign target, separating modifier assignment from magnitude computation. All seven glove Alloy results in the previously captured 13-row/12-material special array are now assigned at their bounded scopes; this does not establish the complete game's crafting inventory. Root ISSUES.md stays the single issue list.