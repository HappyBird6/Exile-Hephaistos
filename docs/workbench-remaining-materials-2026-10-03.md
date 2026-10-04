Latest checkpoint: [Rattling Sceptre and four Command Essences](workbench-rattling-sceptre-2026-10-03.md), overall116 / current-scope155: implemented108, pending47, excluded65. Earlier counts describe their dated checkpoints.

Latest checkpoint: [Rusted Cuirass](workbench-rusted-cuirass-2026-10-03.md) adds three Perfect results; current104/155 implemented,51 pending, exclusions65 unchanged. Counts below describe the earlier checkpoint.

# Remaining Workbench materials and probability evidence — 2026-10-03

This is an evidence and UI correction checkpoint after `7ea00574`, not a material implementation or completion claim. Registry220, current inventory155, current implemented101/pending54, overall implemented109 and effective exclusions65 remain unchanged. All backend catalog bytes, rule/snapshot versions and the four-base film schema are preserved.

## Remaining Essence classification

Current individual PoE2DB material cards were captured on2026-10-03. Fresh Amulets, Gloves_str, Bows and Wands tables have40/63/45/30 Essence rows respectively; none references any of these twelve remaining IDs. [Per-card effects, affixes, character levels, source URLs/hashes and four-table proof](evidence/workbench-remaining-essence-review-2026-10-03.json).

| Material | Verified target | Blocker |
|---|---|---|
| [Perfect Body](https://poe2db.tw/us/Perfect_Essence_of_the_Body) | Body Armour;8–10% maximum Life, prefix | Additional base and complete ordinary/special catalog |
| [Perfect Mind](https://poe2db.tw/us/Perfect_Essence_of_the_Mind) | Ring;4–6% maximum Mana, prefix | Additional base and complete catalog |
| [Perfect Ruin](https://poe2db.tw/us/Perfect_Essence_of_Ruin) | Body Armour;10–15% physical hits taken as chaos, prefix | Additional base and complete catalog |
| [Perfect Seeking](https://poe2db.tw/us/Perfect_Essence_of_Seeking) | Body Armour;40–50% reduced incoming critical damage bonus, suffix | Additional base and complete catalog |
| [Delirium](https://poe2db.tw/us/Essence_of_Delirium) | Body Armour;allocates random Notable, prefix | Additional base; eligible Notable outcomes/allocation interactions not verified |
| [Insanity](https://poe2db.tw/us/Essence_of_Insanity) | Belt;two enchantments on corruption, suffix | Additional base; Vaal operation user-deferred; enchantment candidates/pairing unverified |
| [Perfect Insulation](https://poe2db.tw/us/Perfect_Essence_of_Insulation) | Belt;26–30% fire taken recouped as Life, suffix | Additional base and complete catalog |
| [Perfect Thawing](https://poe2db.tw/us/Perfect_Essence_of_Thawing) | Helmet;26–30% cold taken recouped as Life, suffix | Additional base and complete catalog |
| [Lesser Command](https://poe2db.tw/us/Lesser_Essence_of_Command) | Sceptre;35–44% allied damage in Presence, prefix | Additional base and complete catalog |
| [Command](https://poe2db.tw/us/Essence_of_Command) | Sceptre;55–64% allied damage in Presence, prefix | Additional base and complete catalog |
| [Greater Command](https://poe2db.tw/us/Greater_Essence_of_Command) | Sceptre;75–89% allied damage in Presence, prefix | Additional base and complete catalog |
| [Perfect Command](https://poe2db.tw/us/Perfect_Essence_of_Command) | Sceptre;15–20% Aura magnitude, suffix | Additional base and complete catalog |

Classification: confirmed existing-base results0; extra target required12; secondary incomplete outcome evidence2. A verified effect card alone does not establish complete modifier identity, item-level gates, natural pool, conflicts, numeric precision or probabilities for a new base. Character requirements are not item-level requirements. No extra base, invented outcome, altered exclusion or implied support is added. Insanity remains in current pending inventory; its deferred dependency is recorded without silently changing exclusions65.

## WB-029 weight field investigation

[Audit evidence](evidence/workbench-weight-field-review-2026-10-03.json) matches185 exact Wand Codes, names, family/effect and levels. Main table levels equal matched detail requirements185/185. Applicable detail Spawn Tag numbers are all1; main weights have12 distinct ratios (50–1000), so neither a single normalization factor nor differing matched levels explains the discrepancy. Exact ordered base tags uniquely resolve ambiguous details; no alternative eligible detail has been substituted.

The public [ModsView client](https://cdn.poe2db.tw/js/ModsView.f39fca410dd746d3.js), captured2026-10-03, SHA256 `967efb19b218b4d92a7129fb60ee694ea3fee3a3015dd75cf7dd64d6cbb0a2a5`, uses eligible `DropChance` sums and cumulative weighted selection. It explains client use, not server derivation or cache/extraction provenance. Generic client Fossil scaffolding is not evidence of active PoE2 Fossil crafting. The cause of different fields and actual game probabilities remain NOT_ESTABLISHED. PoE2DB states modifier weights cannot be obtained from game files.

Adopted field: complete main class-table `DropChance`, consistently under the user's POE2DB_AS_PUBLISHED source-model policy. Detail ordered Spawn Tags establish eligibility only. No mixed units, incomplete-weight pool or substituted detail numeric weights. Exact arithmetic is exact for this published-weight model, not a verified in-game probability.

The Workbench evidence panel now names this model limitation, links the selected base's table and states which fields are used. Guaranteed Essence/Alloy additions are described as verified eligible material targets; their ADD event is no longer mislabeled as a weighted natural draw. Uniform removal/target choices and unverified numeric ledgers remain separate. Fracture and other non-removal operations are no longer called uniform removal.

## Remaining current materials

[Individual54-item blocker ledger](evidence/workbench-active-material-blockers-2026-10-03.json) preserves the existing principal totals: additional target27 (Essence12, Refined Catalyst13, weapon/caster quality2), rule/data20 (normal Catalyst13, current Omens5, Armourer's Scrap and Chance), engine state2 (Wisdom/Extraction), reintroduction unverified5 (legacy directional Alchemy/Coronation and Greater Annulment). Only the twelve Essence effects/targets are newly source-reviewed here; remaining ledger entries carry forward unresolved evidence and are not declared freshly verified or implemented.

Existing numeric source-unit/shared-ratio models remain UNVERIFIED; fixed +2/+3 results have no numeric lottery. Source modifier item-level gates remain conservative simulator boundaries. Solar/Stocky/Bow/Wand support, four-base films, exact JSON value semantics and exclusions65 are unchanged. A next special-result implementation needs a reviewed additional equipment base; Body Armour would unlock three bounded Perfect effects before the separate Delirium outcome work. No base choice is made in this checkpoint.

## Validation

Project Docker: full frontend240 tests/36files, lint/typecheck/format/build passed. Final visual CSS correction was followed by format and typechecking production build; no behavior changed after the240-test pass. Actual API123 assertions and final Chromium160 assertions passed with zero page errors. Browser covers all11 Wand materials and their probability basis, Shift/repeat/cancel, split films/future preservation/reload, all four bases, Alt, low-level refusal,503retry and390px expanded evidence without overflow. Desktop and narrow screenshots were visually reviewed; source links now have readable gold color and keyboard focus outline. Fixed/variable numeric boundaries remain intact.

The first browser probe clicked the non-action evidence summary before checking Shift retention; that correctly cancelled selection. The probe was changed to read evidence without clicking until repeat checks complete, preserving the product cancellation rule. All browser/API/test containers closed normally. Heavy checks/builds and browser runs were sequential following the user's explicit resume; COM Surrogate cause remains unconfirmed, and no OS process/settings were changed.

Final frontend `index-CHLbAjRa.js`/`index-Do7CpyPH.css`; image manifest `07e79f7102e05131f2ba704cd514bc4077218f56eef9793b2e660c918927dfbc`. Backend runtime JAR SHA256 remains `eb4f280b7a9db276b0f14402327ed3a452ac7ad55e610df11f9ddf890845e65f`. Backend code/catalogs are unchanged; previous317 backend passes are historical, not rerun claims. Legacy browser30/cachepersist and exhaustive old material browser matrices are not rerun. Exact reports/scripts/screenshots are under `E:WORKExile-Hephaistoscodexqa-20261003probability-*`; all12 card captures and fresh class tables are there as `remaining-*`. Existing checkout remains clean at e937ddcc; no remote write, original DB/volume change or new material completion claim.
