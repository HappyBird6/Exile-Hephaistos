# Workbench registered-material scope and remaining route (2026-10-03, v25)

This replaces the current interpretation of the historical v16 triage, without changing that historical evidence. Registry 220 is not the complete game inventory, and registered is not implemented or currently obtainable. Baseline: `7794b7356ca8fa7e1861e6cbbebeec6d762e4479`; runtime rules and catalogs are unchanged.

Implemented unique identities: **74** (19 Currency, 36 Essence, 8 Alloy, 11 Omen). Solar: **60 support records / 49 executable actions**; Stocky: **67 / 56**. Omens count as support records, not executable actions. Cross-base support counts overlap and must not be summed into unique implementation counts.

## Remaining 146 identities, one principal blocker each

| Principal blocker | Count | Meaning |
|---|---:|---|
| OTHER_TARGET_REQUIRED | 97 | 46 Essence, 5 Alloy, 13 Refined Catalyst, 26 Liquid Emotion, 7 Currency. Other equipment classes or crafting resources; not a request to expand bases. |
| RULE_DATA_GAP | 32 | 13 ordinary Catalyst, 8 Currency, 11 Omen. Current supported classes may be eligible, but complete outcomes, scaling, tags or probability rules are missing; engines may also be absent. |
| ENGINE_STATE_GAP | 4 | Wisdom identification, Mirror copy, Artificer sockets, Extraction/destruction. Principal blocker is missing state/inventory representation; exact constraints still require proof. |
| REINTRODUCTION_UNVERIFIED | 5 | Sinistral/Dextral Alchemy, Sinistral/Dextral Coronation, Greater Annulment. Official 0.3.0 discontinued obtainability; subsequent reintroduction has not been verified. |
| OUTSIDE_EQUIPMENT_SCOPE | 5 | Four encounter omens and Liquid Verisium. |
| USER_EXCLUDED | 2 | Vaal Orb and Hinekora's Lock. |
| EXCLUDED_OPERATION_DEPENDENCY | 1 | Omen of Corruption requires excluded Vaal Orb; not a third direct user exclusion. |

The source archive captured every pending identity's primary page (HTTP 200), plus Amulets. Nineteen currency registry URLs pointed at generic Stackable Currency: targeted item pages were fetched instead; generic evidence is retained separately. The Docker validator matched all 146 per-item SHA-256 values, uniqueness and bucket counts. [Machine-readable classification and provenance](evidence/workbench-v25-scope-classification-2026-10-03.json). Full HTML and execution scripts stay under `codex/qa-20261003`.

Current public cards describe 13 Basic-Jewel and 13 Time-Lost-Jewel Liquid Emotions as remove/add Crafted-modifier operations. They do **not** prove Solar Amulet instilling support, current availability or an implemented Jewel engine. Thirteen Refined Catalysts target Jewel quality, whereas 13 ordinary Catalysts target rings/amulets. Other-target crafting remains equipment scope.

[Official 0.3.0 patch](https://www.pathofexile.com/forum/view-thread/3826682) explicitly names the five discontinued omens. Current PoE2DB effect text cannot supersede that obtainability evidence. Coronation's simple prefix/suffix effect therefore does not authorize enabling a legacy rule. Search for reintroduction is not exhaustive and no claim of current permanent removal is made.

## Bounded implementation route

Nine engine/state families remain relevant to the currently supported classes; this is a route map, not nine ready specifications:

1. **Quality**: ordinary Catalyst for Solar and Armourer's Scrap for Stocky; then quality-dependent Catalysing Exaltation and Vaal infusers. Resolve WB-004 increments, caps, tag mapping, scaling and rounding first. Start with source review of ordinary quality; no combat calculator is needed.
2. **Sockets / resource inventory / destruction**: Artificer and Extraction; verify limits, Augments, socket-bound resources and persistence before mutating item state. Core Destabiliser is another-target resource operation.
3. **Identification**: Wisdom needs identified/unidentified state. Existing fully known simulator states cannot meaningfully identify again.
4. **Copy / Mirrored state**: Mirror needs a second persistent item identity and verified restrictions. A duplicate history row is insufficient.
5. **Unique / corruption outcomes**: Chance, Architect and Vaal Cultivation require complete unique/outcome sets and probabilities; do not replace unknown outcomes with an invented uniform list. Vaal Orb itself stays user-excluded.
6. **Sacrifice / corruption enchantments**: Kopec for Stocky and Kamasa for Solar. Eligible enchantment pools, upgrades, random removal and corruption state need proof.
7. **Desecration / reveal**: seven omens need complete boss/reveal pools and unrevealed/corrupted state. Existing Essence of the Abyss assignment does not implement this engine.
8. **Sanctification**: the Divine omen needs exact state, eligibility and downstream constraints.
9. **Modifier-type selection**: Homogenising Exaltation/Coronation need a proven definition of type and its selection distribution. Do not infer tags or a 1/N distribution over an unknown eligible set.

Independent per-base gaps remain even for globally implemented identities. Solar Sovereign now has an exact primary target (below); Stocky basic Infinite remains blocked on the complete eligible attribute outcomes (WB-010). Conservative special-affix level boundaries remain WB-017. The 97 other-target records are not invitations to implement unlimited new bases. The five legacy omens need reintroduction evidence before they can become candidates.

## Solar Sovereign: target found, magnitude rules still pending

[Current Amulet data](https://poe2db.tw/us/Amulets) contains `AlloyEffectOfResistanceMods1`, prefix, family `EnchantmentHeistArmour`, level 65 (effective required level 52), zero ordinary spawn weight, `IsAlloy=true`, `Removes=true`. [Exact modifier detail](https://poe2db.tw/us/hover?s=Data%5CMods%2FAlloyEffectOfResistanceMods1) gives 20..30% Explicit Resistance Modifier magnitudes, name Verisium, stat `heist enchantment resistance mod effect +%`, **Local / Unscalable Value**. This is not Stocky's `local_ward_+%` target.

[Captured row and detail provenance](evidence/solar-sovereign-target-2026-10-03.json). Exact target discovery resolves the missing-Code portion of WB-021. Eligible resistance stat coverage, underlying/displayed magnitude rules, roll/lock/reroll behavior and low-ilvl eligibility remain unresolved. Keep Solar unsupported until bounded affix assignment and display semantics can be represented without claiming a calculated resistance result. No modifier pool or catalog was changed by this review.

## Validation boundary

This source/classification checkpoint passed Docker integrity/uniqueness/count checks for all 146 pending-source records, current runtime scope checks and ten safe-refusal API probes (five legacy omens x two bases, HTTP400). Direct Docker retrieval of the official forum returned HTTP403; the web reader successfully verified the official 0.3.0 patch. No Docker forum HTML/hash is claimed. BE/FE suites and unrelated browser/cache/history matrices are not rerun for documentation-only changes; their prior 246/160/70/11 results belong to checkpoint 7794b735, not new execution. Runtime registry and snapshots remain unchanged. Original repository, DB volumes and archived films are preserved; no push, merge or deployment.

## Per-identity classification

| Record / primary page | Principal blocker | Reason |
|---|---|---|
| [Vaal_Orb](https://poe2db.tw/us/Vaal_Orb) | USER_EXCLUDED | Explicitly excluded by the user; retained registry record is not a completion target. |
| [Hinekoras_Lock](https://poe2db.tw/us/Hinekoras_Lock) | USER_EXCLUDED | Explicitly excluded by the user; retained registry record is not a completion target. |
| [Lesser_Essence_of_Abrasion](https://poe2db.tw/us/Lesser_Essence_of_Abrasion) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Lesser_Essence_of_Flames](https://poe2db.tw/us/Lesser_Essence_of_Flames) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Lesser_Essence_of_Ice](https://poe2db.tw/us/Lesser_Essence_of_Ice) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Lesser_Essence_of_Electricity](https://poe2db.tw/us/Lesser_Essence_of_Electricity) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Lesser_Essence_of_Battle](https://poe2db.tw/us/Lesser_Essence_of_Battle) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Lesser_Essence_of_Sorcery](https://poe2db.tw/us/Lesser_Essence_of_Sorcery) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Lesser_Essence_of_Haste](https://poe2db.tw/us/Lesser_Essence_of_Haste) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Essence_of_Abrasion](https://poe2db.tw/us/Essence_of_Abrasion) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Essence_of_Flames](https://poe2db.tw/us/Essence_of_Flames) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Essence_of_Ice](https://poe2db.tw/us/Essence_of_Ice) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Essence_of_Electricity](https://poe2db.tw/us/Essence_of_Electricity) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Essence_of_Battle](https://poe2db.tw/us/Essence_of_Battle) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Essence_of_Sorcery](https://poe2db.tw/us/Essence_of_Sorcery) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Essence_of_Haste](https://poe2db.tw/us/Essence_of_Haste) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Greater_Essence_of_Abrasion](https://poe2db.tw/us/Greater_Essence_of_Abrasion) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Greater_Essence_of_Flames](https://poe2db.tw/us/Greater_Essence_of_Flames) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Greater_Essence_of_Ice](https://poe2db.tw/us/Greater_Essence_of_Ice) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Greater_Essence_of_Electricity](https://poe2db.tw/us/Greater_Essence_of_Electricity) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Greater_Essence_of_Sorcery](https://poe2db.tw/us/Greater_Essence_of_Sorcery) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Greater_Essence_of_Haste](https://poe2db.tw/us/Greater_Essence_of_Haste) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Perfect_Essence_of_the_Body](https://poe2db.tw/us/Perfect_Essence_of_the_Body) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Perfect_Essence_of_the_Mind](https://poe2db.tw/us/Perfect_Essence_of_the_Mind) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Perfect_Essence_of_Abrasion](https://poe2db.tw/us/Perfect_Essence_of_Abrasion) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Perfect_Essence_of_Flames](https://poe2db.tw/us/Perfect_Essence_of_Flames) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Perfect_Essence_of_Ice](https://poe2db.tw/us/Perfect_Essence_of_Ice) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Perfect_Essence_of_Electricity](https://poe2db.tw/us/Perfect_Essence_of_Electricity) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Perfect_Essence_of_Ruin](https://poe2db.tw/us/Perfect_Essence_of_Ruin) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Perfect_Essence_of_Battle](https://poe2db.tw/us/Perfect_Essence_of_Battle) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Perfect_Essence_of_Sorcery](https://poe2db.tw/us/Perfect_Essence_of_Sorcery) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Perfect_Essence_of_Haste](https://poe2db.tw/us/Perfect_Essence_of_Haste) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Lesser_Essence_of_Seeking](https://poe2db.tw/us/Lesser_Essence_of_Seeking) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Essence_of_Seeking](https://poe2db.tw/us/Essence_of_Seeking) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Greater_Essence_of_Seeking](https://poe2db.tw/us/Greater_Essence_of_Seeking) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Perfect_Essence_of_Seeking](https://poe2db.tw/us/Perfect_Essence_of_Seeking) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Essence_of_Delirium](https://poe2db.tw/us/Essence_of_Delirium) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Essence_of_Insanity](https://poe2db.tw/us/Essence_of_Insanity) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Perfect_Essence_of_Insulation](https://poe2db.tw/us/Perfect_Essence_of_Insulation) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Perfect_Essence_of_Thawing](https://poe2db.tw/us/Perfect_Essence_of_Thawing) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Lesser_Essence_of_Alacrity](https://poe2db.tw/us/Lesser_Essence_of_Alacrity) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Essence_of_Alacrity](https://poe2db.tw/us/Essence_of_Alacrity) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Greater_Essence_of_Alacrity](https://poe2db.tw/us/Greater_Essence_of_Alacrity) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Perfect_Essence_of_Alacrity](https://poe2db.tw/us/Perfect_Essence_of_Alacrity) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Lesser_Essence_of_Command](https://poe2db.tw/us/Lesser_Essence_of_Command) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Essence_of_Command](https://poe2db.tw/us/Essence_of_Command) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Greater_Essence_of_Command](https://poe2db.tw/us/Greater_Essence_of_Command) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Perfect_Essence_of_Command](https://poe2db.tw/us/Perfect_Essence_of_Command) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Protective_Alloy](https://poe2db.tw/us/Protective_Alloy) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Celestial_Alloy](https://poe2db.tw/us/Celestial_Alloy) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Transcendent_Alloy](https://poe2db.tw/us/Transcendent_Alloy) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [The_Runebinders_Alloy](https://poe2db.tw/us/The_Runebinders_Alloy) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [The_Runefathers_Alloy](https://poe2db.tw/us/The_Runefathers_Alloy) | OTHER_TARGET_REQUIRED | Current item card lists no Amulet/Jewellery or Gloves target. Requires another complete base catalog; no base expansion authorized here. |
| [Omen_of_Sinistral_Alchemy](https://poe2db.tw/us/Omen_of_Sinistral_Alchemy) | REINTRODUCTION_UNVERIFIED | Official 0.3.0 explicitly discontinued obtainability; current item-card presence does not prove reintroduction. Legacy effect stays disabled. |
| [Omen_of_Dextral_Alchemy](https://poe2db.tw/us/Omen_of_Dextral_Alchemy) | REINTRODUCTION_UNVERIFIED | Official 0.3.0 explicitly discontinued obtainability; current item-card presence does not prove reintroduction. Legacy effect stays disabled. |
| [Omen_of_Sinistral_Coronation](https://poe2db.tw/us/Omen_of_Sinistral_Coronation) | REINTRODUCTION_UNVERIFIED | Official 0.3.0 explicitly discontinued obtainability; current item-card presence does not prove reintroduction. Legacy effect stays disabled. |
| [Omen_of_Dextral_Coronation](https://poe2db.tw/us/Omen_of_Dextral_Coronation) | REINTRODUCTION_UNVERIFIED | Official 0.3.0 explicitly discontinued obtainability; current item-card presence does not prove reintroduction. Legacy effect stays disabled. |
| [Omen_of_Corruption](https://poe2db.tw/us/Omen_of_Corruption) | EXCLUDED_OPERATION_DEPENDENCY | Requires user-excluded Vaal Orb operation; this is a dependency, not an additional explicit user exclusion. |
| [Omen_of_Greater_Annulment](https://poe2db.tw/us/Omen_of_Greater_Annulment) | REINTRODUCTION_UNVERIFIED | Official 0.3.0 explicitly discontinued obtainability; current item-card presence does not prove reintroduction. Legacy effect stays disabled. |
| [Omen_of_Answered_Prayers](https://poe2db.tw/us/Omen_of_Answered_Prayers) | OUTSIDE_EQUIPMENT_SCOPE | Encounter-only effect; no equipment crafting action. |
| [Omen_of_Secret_Compartments](https://poe2db.tw/us/Omen_of_Secret_Compartments) | OUTSIDE_EQUIPMENT_SCOPE | Encounter-only effect; no equipment crafting action. |
| [Omen_of_the_Hunt](https://poe2db.tw/us/Omen_of_the_Hunt) | OUTSIDE_EQUIPMENT_SCOPE | Encounter-only effect; no equipment crafting action. |
| [Omen_of_Reinforcements](https://poe2db.tw/us/Omen_of_Reinforcements) | OUTSIDE_EQUIPMENT_SCOPE | Encounter-only effect; no equipment crafting action. |
| [Omen_of_Homogenising_Exaltation](https://poe2db.tw/us/Omen_of_Homogenising_Exaltation) | RULE_DATA_GAP | Meaning of modifier type, eligible type set and selection distribution remain unverified. |
| [Omen_of_Homogenising_Coronation](https://poe2db.tw/us/Omen_of_Homogenising_Coronation) | RULE_DATA_GAP | Meaning of modifier type, eligible type set and selection distribution remain unverified. |
| [Omen_of_Sanctification](https://poe2db.tw/us/Omen_of_Sanctification) | RULE_DATA_GAP | Desecration/reveal or Sanctification state, complete eligible outcomes, interactions and distribution unverified (WB-005). |
| [Omen_of_Catalysing_Exaltation](https://poe2db.tw/us/Omen_of_Catalysing_Exaltation) | RULE_DATA_GAP | Quality-dependent eligible tags and probability formula unverified; quality engine also missing. |
| [Omen_of_the_Sovereign](https://poe2db.tw/us/Omen_of_the_Sovereign) | RULE_DATA_GAP | Desecration/reveal or Sanctification state, complete eligible outcomes, interactions and distribution unverified (WB-005). |
| [Omen_of_the_Liege](https://poe2db.tw/us/Omen_of_the_Liege) | RULE_DATA_GAP | Desecration/reveal or Sanctification state, complete eligible outcomes, interactions and distribution unverified (WB-005). |
| [Omen_of_the_Blackblooded](https://poe2db.tw/us/Omen_of_the_Blackblooded) | RULE_DATA_GAP | Desecration/reveal or Sanctification state, complete eligible outcomes, interactions and distribution unverified (WB-005). |
| [Omen_of_Putrefaction](https://poe2db.tw/us/Omen_of_Putrefaction) | RULE_DATA_GAP | Desecration/reveal or Sanctification state, complete eligible outcomes, interactions and distribution unverified (WB-005). |
| [Omen_of_Light](https://poe2db.tw/us/Omen_of_Light) | RULE_DATA_GAP | Desecration/reveal or Sanctification state, complete eligible outcomes, interactions and distribution unverified (WB-005). |
| [Omen_of_Sinistral_Necromancy](https://poe2db.tw/us/Omen_of_Sinistral_Necromancy) | RULE_DATA_GAP | Desecration/reveal or Sanctification state, complete eligible outcomes, interactions and distribution unverified (WB-005). |
| [Omen_of_Dextral_Necromancy](https://poe2db.tw/us/Omen_of_Dextral_Necromancy) | RULE_DATA_GAP | Desecration/reveal or Sanctification state, complete eligible outcomes, interactions and distribution unverified (WB-005). |
| [Flesh_Catalyst](https://poe2db.tw/us/Flesh_Catalyst) | RULE_DATA_GAP | Solar eligible class; complete tag applicability, quality increments/caps and magnitude/rounding rules remain unresolved (WB-004). |
| [Neural_Catalyst](https://poe2db.tw/us/Neural_Catalyst) | RULE_DATA_GAP | Solar eligible class; complete tag applicability, quality increments/caps and magnitude/rounding rules remain unresolved (WB-004). |
| [Carapace_Catalyst](https://poe2db.tw/us/Carapace_Catalyst) | RULE_DATA_GAP | Solar eligible class; complete tag applicability, quality increments/caps and magnitude/rounding rules remain unresolved (WB-004). |
| [Uul-Netols_Catalyst](https://poe2db.tw/us/Uul-Netols_Catalyst) | RULE_DATA_GAP | Solar eligible class; complete tag applicability, quality increments/caps and magnitude/rounding rules remain unresolved (WB-004). |
| [Xophs_Catalyst](https://poe2db.tw/us/Xophs_Catalyst) | RULE_DATA_GAP | Solar eligible class; complete tag applicability, quality increments/caps and magnitude/rounding rules remain unresolved (WB-004). |
| [Tuls_Catalyst](https://poe2db.tw/us/Tuls_Catalyst) | RULE_DATA_GAP | Solar eligible class; complete tag applicability, quality increments/caps and magnitude/rounding rules remain unresolved (WB-004). |
| [Eshs_Catalyst](https://poe2db.tw/us/Eshs_Catalyst) | RULE_DATA_GAP | Solar eligible class; complete tag applicability, quality increments/caps and magnitude/rounding rules remain unresolved (WB-004). |
| [Chayulas_Catalyst](https://poe2db.tw/us/Chayulas_Catalyst) | RULE_DATA_GAP | Solar eligible class; complete tag applicability, quality increments/caps and magnitude/rounding rules remain unresolved (WB-004). |
| [Reaver_Catalyst](https://poe2db.tw/us/Reaver_Catalyst) | RULE_DATA_GAP | Solar eligible class; complete tag applicability, quality increments/caps and magnitude/rounding rules remain unresolved (WB-004). |
| [Sibilant_Catalyst](https://poe2db.tw/us/Sibilant_Catalyst) | RULE_DATA_GAP | Solar eligible class; complete tag applicability, quality increments/caps and magnitude/rounding rules remain unresolved (WB-004). |
| [Skittering_Catalyst](https://poe2db.tw/us/Skittering_Catalyst) | RULE_DATA_GAP | Solar eligible class; complete tag applicability, quality increments/caps and magnitude/rounding rules remain unresolved (WB-004). |
| [Adaptive_Catalyst](https://poe2db.tw/us/Adaptive_Catalyst) | RULE_DATA_GAP | Solar eligible class; complete tag applicability, quality increments/caps and magnitude/rounding rules remain unresolved (WB-004). |
| [Necrotic_Catalyst](https://poe2db.tw/us/Necrotic_Catalyst) | RULE_DATA_GAP | Solar eligible class; complete tag applicability, quality increments/caps and magnitude/rounding rules remain unresolved (WB-004). |
| [Refined_Flesh_Catalyst](https://poe2db.tw/us/Refined_Flesh_Catalyst) | OTHER_TARGET_REQUIRED | Current Refined Catalyst card targets Jewel quality, not either supported base. |
| [Refined_Neural_Catalyst](https://poe2db.tw/us/Refined_Neural_Catalyst) | OTHER_TARGET_REQUIRED | Current Refined Catalyst card targets Jewel quality, not either supported base. |
| [Refined_Carapace_Catalyst](https://poe2db.tw/us/Refined_Carapace_Catalyst) | OTHER_TARGET_REQUIRED | Current Refined Catalyst card targets Jewel quality, not either supported base. |
| [Refined_Uul-Netols_Catalyst](https://poe2db.tw/us/Refined_Uul-Netols_Catalyst) | OTHER_TARGET_REQUIRED | Current Refined Catalyst card targets Jewel quality, not either supported base. |
| [Refined_Xophs_Catalyst](https://poe2db.tw/us/Refined_Xophs_Catalyst) | OTHER_TARGET_REQUIRED | Current Refined Catalyst card targets Jewel quality, not either supported base. |
| [Refined_Tuls_Catalyst](https://poe2db.tw/us/Refined_Tuls_Catalyst) | OTHER_TARGET_REQUIRED | Current Refined Catalyst card targets Jewel quality, not either supported base. |
| [Refined_Eshs_Catalyst](https://poe2db.tw/us/Refined_Eshs_Catalyst) | OTHER_TARGET_REQUIRED | Current Refined Catalyst card targets Jewel quality, not either supported base. |
| [Refined_Chayulas_Catalyst](https://poe2db.tw/us/Refined_Chayulas_Catalyst) | OTHER_TARGET_REQUIRED | Current Refined Catalyst card targets Jewel quality, not either supported base. |
| [Refined_Reaver_Catalyst](https://poe2db.tw/us/Refined_Reaver_Catalyst) | OTHER_TARGET_REQUIRED | Current Refined Catalyst card targets Jewel quality, not either supported base. |
| [Refined_Sibilant_Catalyst](https://poe2db.tw/us/Refined_Sibilant_Catalyst) | OTHER_TARGET_REQUIRED | Current Refined Catalyst card targets Jewel quality, not either supported base. |
| [Refined_Skittering_Catalyst](https://poe2db.tw/us/Refined_Skittering_Catalyst) | OTHER_TARGET_REQUIRED | Current Refined Catalyst card targets Jewel quality, not either supported base. |
| [Refined_Adaptive_Catalyst](https://poe2db.tw/us/Refined_Adaptive_Catalyst) | OTHER_TARGET_REQUIRED | Current Refined Catalyst card targets Jewel quality, not either supported base. |
| [Refined_Necrotic_Catalyst](https://poe2db.tw/us/Refined_Necrotic_Catalyst) | OTHER_TARGET_REQUIRED | Current Refined Catalyst card targets Jewel quality, not either supported base. |
| [Diluted_Liquid_Ire](https://poe2db.tw/us/Diluted_Liquid_Ire) | OTHER_TARGET_REQUIRED | Current card targets Rare Basic Jewel with a Crafted modifier. Not Amulet instilling. |
| [Diluted_Liquid_Guilt](https://poe2db.tw/us/Diluted_Liquid_Guilt) | OTHER_TARGET_REQUIRED | Current card targets Rare Basic Jewel with a Crafted modifier. Not Amulet instilling. |
| [Diluted_Liquid_Greed](https://poe2db.tw/us/Diluted_Liquid_Greed) | OTHER_TARGET_REQUIRED | Current card targets Rare Basic Jewel with a Crafted modifier. Not Amulet instilling. |
| [Liquid_Paranoia](https://poe2db.tw/us/Liquid_Paranoia) | OTHER_TARGET_REQUIRED | Current card targets Rare Basic Jewel with a Crafted modifier. Not Amulet instilling. |
| [Liquid_Envy](https://poe2db.tw/us/Liquid_Envy) | OTHER_TARGET_REQUIRED | Current card targets Rare Basic Jewel with a Crafted modifier. Not Amulet instilling. |
| [Liquid_Disgust](https://poe2db.tw/us/Liquid_Disgust) | OTHER_TARGET_REQUIRED | Current card targets Rare Basic Jewel with a Crafted modifier. Not Amulet instilling. |
| [Liquid_Despair](https://poe2db.tw/us/Liquid_Despair) | OTHER_TARGET_REQUIRED | Current card targets Rare Basic Jewel with a Crafted modifier. Not Amulet instilling. |
| [Concentrated_Liquid_Fear](https://poe2db.tw/us/Concentrated_Liquid_Fear) | OTHER_TARGET_REQUIRED | Current card targets Rare Basic Jewel with a Crafted modifier. Not Amulet instilling. |
| [Concentrated_Liquid_Suffering](https://poe2db.tw/us/Concentrated_Liquid_Suffering) | OTHER_TARGET_REQUIRED | Current card targets Rare Basic Jewel with a Crafted modifier. Not Amulet instilling. |
| [Concentrated_Liquid_Isolation](https://poe2db.tw/us/Concentrated_Liquid_Isolation) | OTHER_TARGET_REQUIRED | Current card targets Rare Basic Jewel with a Crafted modifier. Not Amulet instilling. |
| [Ancient_Diluted_Liquid_Ire](https://poe2db.tw/us/Ancient_Diluted_Liquid_Ire) | OTHER_TARGET_REQUIRED | Current card targets Rare Time-Lost Jewel with a Crafted modifier. Not Amulet instilling. |
| [Ancient_Diluted_Liquid_Guilt](https://poe2db.tw/us/Ancient_Diluted_Liquid_Guilt) | OTHER_TARGET_REQUIRED | Current card targets Rare Time-Lost Jewel with a Crafted modifier. Not Amulet instilling. |
| [Ancient_Diluted_Liquid_Greed](https://poe2db.tw/us/Ancient_Diluted_Liquid_Greed) | OTHER_TARGET_REQUIRED | Current card targets Rare Time-Lost Jewel with a Crafted modifier. Not Amulet instilling. |
| [Ancient_Liquid_Paranoia](https://poe2db.tw/us/Ancient_Liquid_Paranoia) | OTHER_TARGET_REQUIRED | Current card targets Rare Time-Lost Jewel with a Crafted modifier. Not Amulet instilling. |
| [Ancient_Liquid_Envy](https://poe2db.tw/us/Ancient_Liquid_Envy) | OTHER_TARGET_REQUIRED | Current card targets Rare Time-Lost Jewel with a Crafted modifier. Not Amulet instilling. |
| [Ancient_Liquid_Disgust](https://poe2db.tw/us/Ancient_Liquid_Disgust) | OTHER_TARGET_REQUIRED | Current card targets Rare Time-Lost Jewel with a Crafted modifier. Not Amulet instilling. |
| [Ancient_Liquid_Despair](https://poe2db.tw/us/Ancient_Liquid_Despair) | OTHER_TARGET_REQUIRED | Current card targets Rare Time-Lost Jewel with a Crafted modifier. Not Amulet instilling. |
| [Ancient_Concentrated_Liquid_Fear](https://poe2db.tw/us/Ancient_Concentrated_Liquid_Fear) | OTHER_TARGET_REQUIRED | Current card targets Rare Time-Lost Jewel with a Crafted modifier. Not Amulet instilling. |
| [Ancient_Concentrated_Liquid_Suffering](https://poe2db.tw/us/Ancient_Concentrated_Liquid_Suffering) | OTHER_TARGET_REQUIRED | Current card targets Rare Time-Lost Jewel with a Crafted modifier. Not Amulet instilling. |
| [Ancient_Concentrated_Liquid_Isolation](https://poe2db.tw/us/Ancient_Concentrated_Liquid_Isolation) | OTHER_TARGET_REQUIRED | Current card targets Rare Time-Lost Jewel with a Crafted modifier. Not Amulet instilling. |
| [Potent_Liquid_Melancholy](https://poe2db.tw/us/Potent_Liquid_Melancholy) | OTHER_TARGET_REQUIRED | Current card targets Rare Basic Jewel with a Crafted modifier. Not Amulet instilling. |
| [Potent_Liquid_Ferocity](https://poe2db.tw/us/Potent_Liquid_Ferocity) | OTHER_TARGET_REQUIRED | Current card targets Rare Basic Jewel with a Crafted modifier. Not Amulet instilling. |
| [Potent_Liquid_Contempt](https://poe2db.tw/us/Potent_Liquid_Contempt) | OTHER_TARGET_REQUIRED | Current card targets Rare Basic Jewel with a Crafted modifier. Not Amulet instilling. |
| [Ancient_Potent_Liquid_Melancholy](https://poe2db.tw/us/Ancient_Potent_Liquid_Melancholy) | OTHER_TARGET_REQUIRED | Current card targets Rare Time-Lost Jewel with a Crafted modifier. Not Amulet instilling. |
| [Ancient_Potent_Liquid_Ferocity](https://poe2db.tw/us/Ancient_Potent_Liquid_Ferocity) | OTHER_TARGET_REQUIRED | Current card targets Rare Time-Lost Jewel with a Crafted modifier. Not Amulet instilling. |
| [Ancient_Potent_Liquid_Contempt](https://poe2db.tw/us/Ancient_Potent_Liquid_Contempt) | OTHER_TARGET_REQUIRED | Current card targets Rare Time-Lost Jewel with a Crafted modifier. Not Amulet instilling. |
| [Liquid_Verisium](https://poe2db.tw/us/Liquid_Verisium) | OUTSIDE_EQUIPMENT_SCOPE | Encounter-only effect; no equipment crafting action. |
| [Blacksmiths_Whetstone](https://poe2db.tw/us/Blacksmiths_Whetstone) | OTHER_TARGET_REQUIRED | Current card target is weapon/caster equipment or Jewel, not Solar Amulet/Stocky Mitts. |
| [Arcanists_Etcher](https://poe2db.tw/us/Arcanists_Etcher) | OTHER_TARGET_REQUIRED | Current card target is weapon/caster equipment or Jewel, not Solar Amulet/Stocky Mitts. |
| [Armourers_Scrap](https://poe2db.tw/us/Armourers_Scrap) | RULE_DATA_GAP | Quality, unique/corrupted item or sacrifice outcomes require complete eligible outcomes and numeric/state rules; missing engine alone does not establish correctness. |
| [Scroll_of_Wisdom](https://poe2db.tw/us/Scroll_of_Wisdom) | ENGINE_STATE_GAP | Identification / unidentified state; identification is a utility, not an explicit-modifier crafting action. |
| [Orb_of_Chance](https://poe2db.tw/us/Orb_of_Chance) | RULE_DATA_GAP | Quality, unique/corrupted item or sacrifice outcomes require complete eligible outcomes and numeric/state rules; missing engine alone does not establish correctness. |
| [Mirror_of_Kalandra](https://poe2db.tw/us/Mirror_of_Kalandra) | ENGINE_STATE_GAP | Mirrored copy, inventory identity and copy restrictions. |
| [Artificers_Orb](https://poe2db.tw/us/Artificers_Orb) | ENGINE_STATE_GAP | Armour augment socket state, limits and socket-bound resource representation. |
| [Architects_Orb](https://poe2db.tw/us/Architects_Orb) | RULE_DATA_GAP | Quality, unique/corrupted item or sacrifice outcomes require complete eligible outcomes and numeric/state rules; missing engine alone does not establish correctness. |
| [Core_Destabiliser](https://poe2db.tw/us/Core_Destabiliser) | OTHER_TARGET_REQUIRED | Targets Soul Core resource, not an item of either supported base; augment crafting remains in equipment scope. |
| [Vaal_Cultivation_Orb](https://poe2db.tw/us/Vaal_Cultivation_Orb) | RULE_DATA_GAP | Quality, unique/corrupted item or sacrifice outcomes require complete eligible outcomes and numeric/state rules; missing engine alone does not establish correctness. |
| [Yaomacs_Orb_of_Sacrifice](https://poe2db.tw/us/Yaomacs_Orb_of_Sacrifice) | OTHER_TARGET_REQUIRED | Current card target is weapon/caster equipment or Jewel, not Solar Amulet/Stocky Mitts. |
| [Kopecs_Orb_of_Sacrifice](https://poe2db.tw/us/Kopecs_Orb_of_Sacrifice) | RULE_DATA_GAP | Quality, unique/corrupted item or sacrifice outcomes require complete eligible outcomes and numeric/state rules; missing engine alone does not establish correctness. |
| [Kamasas_Orb_of_Sacrifice](https://poe2db.tw/us/Kamasas_Orb_of_Sacrifice) | RULE_DATA_GAP | Quality, unique/corrupted item or sacrifice outcomes require complete eligible outcomes and numeric/state rules; missing engine alone does not establish correctness. |
| [Yuguls_Orb_of_Sacrifice](https://poe2db.tw/us/Yuguls_Orb_of_Sacrifice) | OTHER_TARGET_REQUIRED | Current card target is weapon/caster equipment or Jewel, not Solar Amulet/Stocky Mitts. |
| [Vaal_Armourers_Infuser](https://poe2db.tw/us/Vaal_Armourers_Infuser) | RULE_DATA_GAP | Quality, unique/corrupted item or sacrifice outcomes require complete eligible outcomes and numeric/state rules; missing engine alone does not establish correctness. |
| [Vaal_Blacksmiths_Infuser](https://poe2db.tw/us/Vaal_Blacksmiths_Infuser) | OTHER_TARGET_REQUIRED | Current card target is weapon/caster equipment or Jewel, not Solar Amulet/Stocky Mitts. |
| [Vaal_Arcanists_Infuser](https://poe2db.tw/us/Vaal_Arcanists_Infuser) | OTHER_TARGET_REQUIRED | Current card target is weapon/caster equipment or Jewel, not Solar Amulet/Stocky Mitts. |
| [Vaal_Catalysing_Infuser](https://poe2db.tw/us/Vaal_Catalysing_Infuser) | RULE_DATA_GAP | Quality, unique/corrupted item or sacrifice outcomes require complete eligible outcomes and numeric/state rules; missing engine alone does not establish correctness. |
| [Orb_of_Extraction](https://poe2db.tw/us/Orb_of_Extraction) | ENGINE_STATE_GAP | Augment extraction, item destruction and returned-resource inventory. |
