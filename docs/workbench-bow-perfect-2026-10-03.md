# Crude Bow Perfect Essences

Six individually verified Perfect Essences now use the shared Workbench replacement path on Crude Bow only. Ordinary Bow catalog/raw/detail files remain byte-for-byte unchanged: 140 positive-spawn definitions, prefix weight44755 + suffix52277 =97032. Six special results have zero natural-spawn weight. Bow now exposes146 definitions,46 material actions and57 implemented support records; Solar218/49/60 and Stocky194/57/68 remain unchanged. Craft Support and State Explorer remain Solar only.

Registry220 now has101 implemented identities (Currency20, Essence62, Alloy8, Omen11). Current development inventory155 has93 implemented and62 pending. The same65 excluded records, including eight implemented deferred Alloys, remain intact. Pending principal barriers: other-target35, rule/data20, engine/state2 and reintroduction5. Inventory registration does not establish complete game coverage or obtainability.

## Primary source results

Captured directly on2026-10-03 from [current Bows](https://poe2db.tw/us/Bows), each material card and each public modifier detail. Bundled `catalog/crude-bow/perfect-essences.raw.json` retains source rows, detail HTML, card hashes, timestamps and exact compared effects. Raw SHA256: `dd984fc65df04439eccc7eebd00df46ee31d36cdacb047d5c32b8aaa58505c25`. The ordinary pool was deep-compared with the fresh source and all140 rows matched.

| Perfect Essence | Bow effect / source bounds | Slot / family | Exact modifier Code |
|---|---|---|---|
| [Abrasion](https://poe2db.tw/us/Perfect_Essence_of_Abrasion) | Gain15–20% Damage as Extra Physical | Prefix / MartialWeaponGainedDamage | EssenceDamageasExtraPhysical1 |
| [Flames](https://poe2db.tw/us/Perfect_Essence_of_Flames) | Gain15–20% Damage as Extra Fire | Prefix / MartialWeaponGainedDamage | EssenceDamageasExtraFire1 |
| [Ice](https://poe2db.tw/us/Perfect_Essence_of_Ice) | Gain15–20% Damage as Extra Cold | Prefix / MartialWeaponGainedDamage | EssenceDamageasExtraCold1 |
| [Electricity](https://poe2db.tw/us/Perfect_Essence_of_Electricity) | Gain15–20% Damage as Extra Lightning | Prefix / MartialWeaponGainedDamage | EssenceDamageasExtraLightning1 |
| [Battle](https://poe2db.tw/us/Perfect_Essence_of_Battle) | +2 to Level of all Attack Skills, fixed | Suffix / IncreaseSocketedGemLevel | EssenceAttackSkillLevel1H1 |
| [Haste](https://poe2db.tw/us/Perfect_Essence_of_Haste) |20–25% chance to gain Onslaught on Killing Hits with this Weapon | Suffix / Onslaught | EssenceOnslaughtonKill1 |

All six source modifier levels are72; effective character requirement/card table57 is recorded separately. Supported simulator item level starts72 conservatively. Actual lower-item-level essence behavior remains unverified; this is not a claim that the game imposes the simulator's gate. Bow is a distinct card target: do not substitute the25–33% extra damage or +3 Attack Skill level shown for other two-handed weapons. Source detail labels all six stats Global, including Haste's stat identifier beginning `local_`; identifiers and source locality are preserved without inferring combat semantics. Craft tags come from the source `fossil_no` list. Source range separators render as an em dash; original HTML/effect text remain in the evidence.

## Conditions and probability boundary

Rare only; removes one eligible non-Fractured explicit modifier and adds the fixed sourced target. Every possible removal must leave a valid family and slot state; any invalid branch refuses unchanged rather than silently pruning the random sample. Four gained-damage targets share one family. A full prefix side can therefore block unrestrained prefix insertion even when removing a prefix alone would succeed. Matching Crystallisation restricts removal to unlocked prefixes/suffixes; its conflicting pair is refused atomically. Unrelated Omens remain, and only matching successful Crystallisation is consumed. Existing Fractured values remain exact.

Eligible removal weights are unpublished: model uniform1/N in `uniform-removal-v1`. Guaranteed modifier selection is1. Each nonfixed single-stat source range uses the existing explicitly UNVERIFIED uniform source-integer assumption `assumed-source-integer-roll-v1`; precision/interior game distributions remain WB-003. Battle +2 is fixed and emits no numeric-roll assumption. Natural rolls retain published ordinary weights and never draw zero-weight special definitions. No partial unknown-weight pool is mixed in.

This delivery assigns equipment affixes; it does not calculate weapon damage, resulting skill levels or Onslaught triggering/duration. Bow quality increments, sockets/resources and pasted Bow mapping remain unsupported. See WB-028 in [ISSUES](../ISSUES.md).

## Compatibility and executed validation

Rule `bow-workbench-perfect-essence-v2`, ledger `bow-unverified-numeric-assumptions-v1`. New additive snapshot `poe2db-crude-bow-normal-2026-10-03-417390c5ebe4+bow-perfect-20261003-dd984fc65df0` explicitly admits the original Bow v1 snapshot. Existing history repository validates old concrete frames before making a current-snapshot request. Reload alone preserves stored old IDs and evidence; actual crafting upgrades compatible state IDs, retains historical evidence IDs and keeps the original future frames when creating a separate film. This is the existing identity-only upgrade boundary, not direct API acceptance of arbitrary old snapshot IDs. No storage format change or login implementation.

Project Docker validation on final runtime:

- Backend full `spotlessApply check generateJooq bootJar`:296 unit/architecture +6 integration =302; zero failures/errors/skips. Thirteen new cases cover all six targets, scalar/fixed behavior, level/refusal/Fracture, both Crystallisations and pair conflicts, same-family safe refusal, compatibility declaration and natural-spawn exclusion.
- Frontend lint/typecheck/format/build passed. Actual API fixture contracts cover all six, fixed Battle without numeric ledger, and missing/extra/out-of-range/source/missing-ledger rejection. Exact JSON key sets/values are compared independently of order; earlier Bow contract now includes missing and out-of-range event data. Affected five-file check51 passed; final complete suite205 tests across34 files passed, zero failures.
- Live API85 assertions: six actions/rarities/levels/Omens/Fracture, unsupported Solar/Stocky targets, shared-family branch rules, compatible snapshot declaration, scope and three-base counts.
- Actual Chromium50 assertions, zero page errors: six successful uses/Shift retention/repeat refusal/Escape cancellation; old Bow v1 reload and old-step film split with original future retained; exact post-craft reload; Alt inline range and keyup;390px lower-level refusal/no document overflow; network error preserves film and retry succeeds; existing Stocky Artificer and Solar Transmutation. Desktop and narrow screenshots reviewed.
- Source/scope proof:212 unrelated registry entries unchanged; all65 deferred records unchanged. Ordinary Bow three source files unchanged. Original checkout remains untouched; QA app/frontend replaced only, existing DB/volumes preserved.

QA scripts, raw captures, results and screenshots are under `E:\WORK\Exile-Hephaistos\codex\qa-20261003\bow-perfect-*`. Runtime JAR SHA256 `4466902f767085249fd74e3f048bfa6848d3704c5fd62d3d7382e4d7eaa5b075`; frontend bundle `index-dPZth0MG.js`, QA image manifest `54152eaad4d6540192dbcbce0b9e4caa0fa1cdf5de55dfdb2b97460d8f71cbb0`.

Unchanged legacy browser30/cachepersist and every old material browser matrix were not rerun for this batch. Earlier checkpoint checks are historical; current results above distinguish actual reruns. First new test run had fixture constructor/order errors and incorrectly expected a numeric assumption for fixed Battle; corrected and rerun. Browser setup initially used full-slot Alchemy items, which correctly refused some branches; final success setups use Transmutation/Regal and separately validate refusals.
