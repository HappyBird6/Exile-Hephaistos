# Workbench quality family review and bounded maximum (2026-10-03)

## Current implementation boundary

This batch implements **source-reviewed maximum quality only**, common to the two supported Workbench bases. Default20; Solar's existing Breach explicit `local_maximum_quality_+20` gives maximum40. The domain returns `qualityLimit` metadata (`quality-limit-v1`) on initial/applied/refused results. It is not an applied quality amount and does not change immutable item state, sampling weights, numeric rolls or eligibility.

Workbench's property card displays `Maximum Quality: 20%` or `40%` from the validated concrete state and exact catalog definition. Unknown base/modifier/cap evidence cannot inherit a reviewed cap. Special and Unique states remain outside the existing model. The stat is fixed20, not quality-scaled: Armour/ward or jewellery magnitudes are not calculated by this batch.

New films preserve detached optional quality-limit evidence; old films without that field remain valid. A contradictory cap or unknown rule version invalidates optional evidence without rewriting item frames/storage. Viewing a past step recomputes its cap from that step's actual modifiers; actual crafting from a prior step still creates a new linear film and preserves the original future.

## Source timing and evidence

[Current PoE2DB Quality](https://poe2db.tw/us/Quality) supplies default maximum20 and armor quality's multiplicative effect. [Maximum Quality](https://poe2db.tw/us/Maximum_Quality) agrees on the default. [Current Breach item](https://poe2db.tw/us/Essence_of_the_Breach), the existing exact Amulet `EssenceBreach` row/detail and source-bound zero-spawn catalog supply +20 maximum. [Captured current source dates/hashes and limits](evidence/quality-limit-source-review-2026-10-03.json).

These are current public PoE2DB glossary/item-card observations (2026-10-03), not a guarantee that every inherited reference table is current PoE2 content or that a listed item is obtainable. Legacy Heist/Metamorph reference tables on the Catalyst page are not used. GGG0.3.0 and0.3.1 were read; neither establishes per-use quality increments in the checked material. Secondary poewiki PoE2 pages were inaccessible; no PoE1 fallback rule or assumed rarity-dependent increment is enabled. The five officially discontinued omens remain disabled without verified reintroduction.

## Relevant family: 17 pending identities plus existing Breach

| Material family | Relevant base | Verified effect boundary | Missing activation rules |
|---|---|---|---|
| Armourer's Scrap (1) | Stocky | Armour quality; default ordinary maximum20 | Per-use increment vs rarity/ilvl, complete special-state constraints; base property/rounding before derived Armour display |
| Ordinary Catalyst (13) | Solar | Life/Mana/Defence/Physical/Fire/Cold/Lightning/Chaos/Attack/Caster/Speed/Attribute/Minion quality; replaces other types | Per-use increment, replacement amount semantics, full affected stat/tag mapping, implicit/special applicability, precision/rounding |
| Vaal Armourer's Infuser (1) | Stocky | May exceed maximum by up to10 and may corrupt | Complete increment/outcome domain, probabilities, repeat/eligibility/corruption behavior; do not assume1/N over an unknown set |
| Vaal Catalysing Infuser (1) | Solar | Same over-cap/corruption description for ring/amulet | As above plus typed-quality interactions |
| Omen of Catalysing Exaltation (1) | Solar | Consumes Catalyst quality and increases corresponding modifier-type chance | Complete type/pool, weighting formula/order, multi-omen and Greater/Perfect interactions |
| Essence of the Breach (already implemented) | Solar | Rare replacement assigns fixed+20 maximum | This batch calculates the maximum only; does not enable Catalyst or applied quality |

13 ordinary Catalyst names: Flesh, Neural, Carapace, Uul-Netol's, Xoph's, Tul's, Esh's, Chayula's, Reaver, Sibilant, Skittering, Adaptive and Necrotic. Individual primary pages and hashes are retained in the146-record triage manifest. Thirteen Refined variants target Jewels and are not either supported base. Weapon/caster quality materials target other equipment; Liquid27 remain deferred by the user.

Minimum scope to enable actual quality later: explicit quality amount/type in immutable state; verified increment/cap/type replacement and complete applicable stat rules; unchanged source rolls vs derived display; bound effect and rounding; preservation across every copy/craft/refusal/import/history path; strict old-record migration without invented quality; omitted/unknown quality cannot silently become known zero. Catalysing/infuser paths additionally need complete outcomes/weight rules and special-state persistence. No login or DB migration is part of this boundary.

## Remaining route

This is a bounded part of **family1 Quality**, not a newly completed currency or the completion of WB-004. Registry unique implementation74 remains unchanged; registered220 with Liquid27 deferred, non-Liquid pending119. Solar60 support records/49 actions and Stocky67/56 remain unchanged; catalogs, source weights and compatible snapshot identities do not change.

Next execution priority: source-prove ordinary Armourer's Scrap increment and eligible states, then add actual Stocky quality without prematurely implementing defense totals. In parallel if independently provable, Solar ordinary Catalyst increments/type replacement and exact tag/scaling mapping. If those sources remain gated, source review of family2 Artificer socket limits/eligible states can proceed; no sockets are claimed ready yet. Families3..9 remain as mapped in the current triage. Solar Sovereign's precise target is known but magnitude boundary remains WB-021; Stocky Infinite outcomes remain WB-010.

## Verification

Execution counts and browser evidence are appended after this batch's required checks. Initial targeted run found a synthetic `solar` fixture used for a reviewed cap; the new test was corrected to use the actual supported base ID. Production rules were not relaxed to accept the synthetic identity.

- Docker targeted BE12 / FE30 pass; final BE249 (243 unit +6 integration), zero failures/errors/skips, including formatter/ArchUnit/check/generateJooq/bootJar. Final FE171 /30files plus lint/type/format/build pass. Full BE4m53; FE tests98.14s. No skipped test failures or game-rule relaxations.
- Actual shared browser28 assertions, zero page errors: Solar20->Breach40->removed20, Divine preservation, Shift/Escape, previous/next, prior-step new-film creation with original future byte preservation, detached new evidence, reload byte preservation, Stocky default20, actual archived v25 frame restoration and subsequent craft preserving its future and omitted legacy quality evidence. Actual390px property card visually inspected. Earlier separate browser/cachepersist matrices were not repeated.
- Actual API: two default-cap bases plus five source-bound transitions/refusals, including fractured Breach preservation and all-locked Annulment refusal. All17 pending quality identities remain disabled. Current source hashes4 and exact zero-spawn Breach definition checked. Runtime registry74/220, Solar60/49, Stocky67/56, full normal catalog counts/weights and snapshot compatibility unchanged.
- API/browser harness fixtures were corrected to include the canonical fractured:false default and use captured old Result.state rather than the enclosing Result object. These were harness corrections, not product failures or altered expectations about game rules.
- Immutable QA JAR0358029c5d8e034be2a124fa9308f34296653b99c26ff297a4e221caa7bd72d8; frontend index-D0z3Dgv7.js, image manifest5bc7623dfea5ae1d315c01430cd345df6c03774cec63ef011507f0776866ada6. Execution/source/API/browser records and desktop/narrow screenshots: codex/qa-20261003/quality-limit-*. Old immutable JARs retained. Only isolated QA app/frontend recreated; original DB/volumes preserved. No push/merge/deployment, migration or login changes.
