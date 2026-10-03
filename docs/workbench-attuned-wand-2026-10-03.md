Latest checkpoint: [Rusted Cuirass](workbench-rusted-cuirass-2026-10-03.md) adds three Perfect results; current104/155 implemented,51 pending, exclusions65 unchanged. Counts below describe the earlier checkpoint.

# Attuned Wand bounded Workbench

One Wand base now uses the shared Workbench engine: nine basic fixed Essences and two Perfect replacement Essences. Eleven paths are supported, but only eight registry implementations are new: Seeking's three identities already worked on Bow. Overall registry220 now has109 implemented identities, while the current development inventory155 has101 implemented and54 pending. The same65 exclusions and eight implemented deferred Alloys remain intact. This is bounded equipment-affix support, not complete game inventory or obtainability coverage. Craft Support/State Explorer remain Solar only.

## Base and complete ordinary pool

[Attuned Wand](https://poe2db.tw/us/Attuned_Wand), ID `Metadata/Items/Weapons/OneHandWeapons/Wands/FourWand3`, drop level2, is the lowest reviewed unrestricted normal Wand base. Its source tags are `wand, onehand`; Base.tag adds `default`. [Withered Wand](https://poe2db.tw/us/Withered_Wand), drop level1, has `no_fire_spell_mods`, `no_cold_spell_mods`, `no_lightning_spell_mods`, `no_physical_spell_mods` and `chaos_implicit_skill` and cannot inherit a generic Wand pool. Runic Fork has unique/runeforged provenance and was not selected as a generic obtainable normal base. Siphoning Wand has the same unrestricted tags but a higher drop level11.

The current [Wands source](https://poe2db.tw/us/Wands) was captured on2026-10-03. All185 ordinary rows have positive published table weights and complete source levels; every row matches an exact public modifier detail by effect, name, generation, family and level. Eleven multiple-detail cases were reduced to exactly one eligible result using ordered Spawn Tags and the selected base's tags. Three duplicate-name pairs retain distinct IDs with source Code suffixes; no row is merged or lost. Path of Building data pinned at commit `bb52d6b368307457eb9c54bb13f1829993d390b1` is a code locator only; numerical and eligibility evidence comes from PoE2DB public detail endpoints.

Source hashes: ordinary raw `c0db9e3bcda5c22ceb37bc21397a96a969dbe8f3a9e0cc4fd5bcbca8aa19d806`; details `31b99d8eea5b8f652bb93f8fd18442b30492d87032905482a003ba04837a55d8`; Perfect raw `662df7573407ee448428d062a800bc641a23850262517f93c82fdb1e92b44bd7`. Prefix84/weight41400, suffix101/weight73000, total114400. Ten ordinary definitions use the existing authorised UNVERIFIED shared-ratio/HALF_UP model ([exact model/definition review](evidence/workbench-wand-roll-model-review-2026-10-03.json)); single-stat source-unit sampling remains UNVERIFIED. Two zero-spawn special definitions make187 total definitions, with30 material actions and41 implemented support records.

Important source distinction: all185 detailed Spawn Tag numeric fields differ from the main table's published weights. As in the existing Bow policy, detailed ordered tags determine eligibility only; all draws consistently use the complete main PoE2DB table's `DropChance` values under `POE2DB_AS_PUBLISHED`. The sources are not mixed or treated as identical weight units. PoE2DB itself states that modifier weight information cannot be obtained from game files; these are source-published modeling weights, not official game probability measurements. Both fields remain in the raw evidence; see WB-029.

Attuned Wand grants innate Mana Drain according to the base listing. This immutable base fact is displayed as “Grants Skill: Mana Drain (not simulated)”; it is not invented as a numeric implicit modifier or changed by Divine/Blessed. Skill level/progression, damage, combat triggers, applied quality, sockets/resources and pasted Wand mapping are outside this affix-only state. The source maximum quality20 is displayed separately from applied quality. No Bow/weapon socket limit is inferred for Wand.

## Eleven individually verified Essence paths

Each material card, Wand target row, source Code and public detail was separately captured and compared. `catalog/attuned-wand/essence-proof.json` retains exact effects, source stat IDs/bounds, families, card hashes and character requirements. All source character requirements below are distinct from modifier levels; supported minimum item levels conservatively use the modifier level. Lower-item-level game Essence behavior remains unverified, as in WB-028, and UI says so.

| Material | Exact Code | Effect | Slot | Supported source-mod-level gate / character requirement |
|---|---|---|---|---|
| [Lesser Sorcery](https://poe2db.tw/us/Lesser_Essence_of_Sorcery) | SpellDamageOnWeapon2 |35–44% increased Spell Damage | Prefix |8 /6 |
| [Sorcery](https://poe2db.tw/us/Essence_of_Sorcery) | SpellDamageOnWeapon4 |55–64% increased Spell Damage | Prefix |33 /26 |
| [Greater Sorcery](https://poe2db.tw/us/Greater_Essence_of_Sorcery) | SpellDamageOnWeapon6 |75–89% increased Spell Damage | Prefix |60 /48 |
| [Perfect Sorcery](https://poe2db.tw/us/Perfect_Essence_of_Sorcery) | EssenceSpellSkillLevel1H1 |+3 to Level of all Spell Skills, fixed | Suffix |72 /57 |
| [Lesser Seeking](https://poe2db.tw/us/Lesser_Essence_of_Seeking) | SpellCriticalStrikeChance2 |34–39% increased Critical Hit Chance for Spells | Suffix |21 /16 |
| [Seeking](https://poe2db.tw/us/Essence_of_Seeking) | SpellCriticalStrikeChance3 |40–46% increased Critical Hit Chance for Spells | Suffix |28 /22 |
| [Greater Seeking](https://poe2db.tw/us/Greater_Essence_of_Seeking) | SpellCriticalStrikeChance4 |47–53% increased Critical Hit Chance for Spells | Suffix |41 /32 |
| [Lesser Alacrity](https://poe2db.tw/us/Lesser_Essence_of_Alacrity) | IncreasedCastSpeed2 |13–16% increased Cast Speed | Suffix |15 /12 |
| [Alacrity](https://poe2db.tw/us/Essence_of_Alacrity) | IncreasedCastSpeed3 |17–20% increased Cast Speed | Suffix |30 /24 |
| [Greater Alacrity](https://poe2db.tw/us/Greater_Essence_of_Alacrity) | IncreasedCastSpeed5 |25–28% increased Cast Speed | Suffix |60 /48 |
| [Perfect Alacrity](https://poe2db.tw/us/Perfect_Essence_of_Alacrity) | EssenceManaCostReduction |18–20% increased Mana Cost Efficiency | Suffix |72 /57 |

Basic Sorcery family `WeaponCasterDamagePrefix`; Seeking `SpellCriticalStrikeChanceIncrease`; Alacrity `IncreasedCastSpeed`. Perfect Sorcery family `IncreaseSocketedGemLevel`, not the ordinary Spell Damage family; Perfect Alacrity `ManaCostEfficiency`. Seeking on Bow still has its different sourced weapon critical modifier/levels; Wand overrides only its own base's target.

Basic Essences upgrade Magic to Rare and add one guaranteed sourced modifier, selection probability1, retaining prior values. Perfect requires Rare, uniformly removes one eligible unlocked explicit (unpublished removal weights, assumed1/N) and adds its one guaranteed zero-spawn result with probability1. Any invalid surviving family/slot branch blocks the operation without pruning candidates. Sinistral/Dextral Crystallisation restrict removal direction, consume only on success and conflict atomically when combined. Fractured values and unrelated Omens remain. Fixed +3 has no numeric lottery; variable numeric ranges retain the existing explicit UNVERIFIED source-integer ledger.

## Validation and compatibility

Rule `wand-workbench-essence-v1`, ledger `wand-unverified-numeric-assumptions-v1`; snapshot `poe2db-attuned-wand-normal-20261003-c0db9e3bcda5+wand-perfect-20261003-662df7573407`. Solar218 definitions/49actions/60records, Stocky194/57/68 and Bow146/46/57 retain exact snapshots and rule versions. Nullable sockets remain unknown for Wand; existing Stocky0/1 behavior is unchanged. Unsupported-base Artificer wording now names an equipment base rather than incorrectly calling a Wand/Bow Jewellery.

Final project Docker results: backend311 unit/architecture +6 integration =317; zero failures/errors/skips. Frontend223 tests/35files plus lint/typecheck/format/build passed. Live API123 assertions and actual Chromium76 assertions passed, zero page errors; desktop/narrow screenshots reviewed. Tests include all11 actions and unchanged-base dispatch. Results are recorded with exact final counts in `E:\WORK\Exile-Hephaistos\codex\qa-20261003\wand-validation.json`; source/selection proofs, API captures, scripts and screenshots are `wand-*` there. Backend tests verify all11 target/rarity/level/family/numeric contracts, both replacement materials' Crystallisation/Fracture behavior, complete natural pool and four-base dispatch. FE fixtures capture actual11 responses and reject missing/extra/out-of-range values or forged numeric source/ledger; existing JSON order semantics and three-base tests remain. Browser covers11 uses, Shift/repeat/cancel, film split/future preservation/reload, Alt, narrow screen, failure/retry and three old bases.

The source/scope audit preserves179 unrelated registry records and all65 deferred records. No existing catalog/source file or film schema is rewritten. Current pending barriers: other-target27, rule/data20, engine/input-state2, reintroduction5 =54. This batch does not resolve lower-level game applicability or numeric precision, quality increments, Wisdom/Extraction state or combat calculations. The unchanged legacy browser30/cachepersist and exhaustive old material browser matrices are not rerun for this bounded batch.

Final runtime JAR SHA256 `eb4f280b7a9db276b0f14402327ed3a452ac7ad55e610df11f9ddf890845e65f`; frontend `index-BIJ9dvK8.js`, QA image manifest `cf42cc1f27492f154f47c16045ba63d5a840754fe54811c158d321753edf5858`. QA app/frontend only were replaced; original checkout and existing DB/volumes remain intact. First test fixtures violated Magic prefix capacity or supplied Solar as Stocky; corrected without changing crafting rules. Source field differences and duplicate-name rows were resolved explicitly as above, rather than silently discarded.

Follow-up WB-029 investigation and probability UI correction: [2026-10-03 remaining-material review](workbench-remaining-materials-2026-10-03.md). The source-weight cause remains unresolved; probabilities are explicitly model probabilities.
