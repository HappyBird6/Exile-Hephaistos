# Rusted Greathelm Workbench — 2026-10-03

Rusted Greathelm is a usable eighth Workbench base with Perfect Thawing and19 common currency paths. Registry220 has118 implemented identities; current inventory155 has110 implemented and45 pending. Exclusions65 and eight implemented deferred Alloys are unchanged. Source-preparation data and actual support are counted separately: Iron Ring remains a candidate, not implemented. Registered inventory is not the complete game inventory.

## Candidate comparison and source review

Fresh [Helmet](https://poe2db.tw/us/Helmets_str) and [Ring](https://poe2db.tw/us/Rings) tables have137 and203 positive-weight ordinary rows, respectively. Helmet requires200 Code candidates,141 cached/59 new public detail lookups; Ring initially required391 candidates/106 new lookups. Helmet was chosen first because the full pool is smaller and verified Armour metadata fits the existing architecture. All137 Helmet rows uniquely match exact Name, effect, generation, family, modifier level and the base's first matching positive ordered Spawn Tag. No unresolved/noninteger definitions remain. The pinned PoB2 commit bb52d6b368307457eb9c54bb13f1829993d390b1 supplies Code locators only; PoE2DB supplies numerical truth.

The normal pool has59 prefixes/weight56200 and78 suffixes/70000, total126200. One zero-weight special suffix yields138 definitions,59 prefixes/79 suffixes. Fifteen multi-stat definitions reuse the authorised UNVERIFIED shared-ratio ticks0..10000/HALF_UP model. Scalar source-unit sampling is also UNVERIFIED. No special result enters a normal roll.

All137 main DropChance values differ from the applicable detail Spawn Tag weights. Complete main weights define the POE2DB_AS_PUBLISHED model; first matching ordered detail tags establish eligibility only. Actual game odds and the origin of the difference remain NOT_ESTABLISHED. No partial weight mixture, invented origin or Cartesian multi-stat distribution is introduced.

[Rusted Greathelm](https://poe2db.tw/us/Rusted_Greathelm), Metadata/Items/Armours/Helmets/FourHelmetStr1, DropLevel1, has tags str_armour, ezomyte_basetype, helmet, armour. Armour29 and Quality.max_quality20 are reviewed base facts. The normal base has no numeric implicit projection. Computed Armour, applied quality, sockets/Artificer, pasted mapping and combat are unsupported. The raw socket string does not establish a socket operation; augmentSockets remains null/unknown. The quality cap20 is explicitly whitelisted only after this base review, not inherited for unknown bases. Belt's null cap/Divine refusal and all seven previous catalogs remain unchanged.

Primary Helmet HTML SHA256 `5ffa7e69dfeb94d16b76c194f83bedde83c88d5b82f107be0d6979f1b507e2d9`; base HTML `cc4b74212d4d674b9c1de4a9f0cae4128196b226015371fbf30a593cc1a087d3`. Raw rows, exact detail HTML, special card/Code/range and base proof are bundled in backend/src/main/resources/catalog/rusted-greathelm/.

## Perfect result and supported conditions

[Perfect Thawing](https://poe2db.tw/us/Perfect_Essence_of_Thawing) exactly matches EssenceColdRecoupLife1, family ColdDamageTakenRecoupedAsLife, suffix, `(26–30)% of Cold Damage taken Recouped as Life`, underlying stat `cold_damage_taken_goes_to_life_over_4_seconds_%`. Modifier level72 is the conservative simulator gate; character requirement57 and the currency's published DropLevel1 are distinct facts. Lower-level game applicability remains unverified; the gate is not represented as an official use restriction.

A Rare state uniformly removes one eligible unlocked affix (model1/N) then adds the guaranteed suffix (1). Sinistral/Dextral Crystallisation select the removal side, conflict atomically as a pair and consume only the matching Omen. Fractures and unrelated Omens survive. Any invalid surviving-family/slot removal branch refuses the whole action; invalid branches are not silently pruned.

Normal Divine rerolls explicit values using existing scalar/coupled assumptions, preserving affix identities and the implicit-free projection. Blessed has no implicit target and refuses atomically; it receives no new Helmet support tag. There are30 implemented support records:19 currencies,10 Omens,one Essence, with20 material actions. Unknown input properties, applied quality/Armour fields and incompatible socket data are rejected before conversion/crafting, not dropped.

The selector and restored-film dispatch retain the same linear per-session history and localStorage schema. Crafting from a prior step starts a separate film while preserving the original future; browsing alone does not split it. No login, DB transition or branching tree was added. Support/Explorer keep their Solar catalog.

## Executed validation

Sequential project Docker checks passed:336 unit/architecture +6 integration =342 JUnit XML cases, zero failures/errors/skips; frontend295 tests/40files plus lint/typecheck/format/build; actual API44 assertions; actual Chromium78 assertions, zero page errors. The first frontend typecheck found the new selector omitted Helmet from one existing function union; this was corrected before the final full suite. The source-audit helper's regexp escaping was fixed and its successful rerun verified all source hashes and preserved data; it was not a product defect.

Backend/API cover complete weighted normal pools, exact Perfect source/numeric bounds, level/rarity/family refusals, both Crystallisation sides/pair conflict, fractures, ordinary Divine, Blessed no-target refusal, unknown-field refusal and dispatch. New frontend contract tests consume actual unmodified initial and Perfect/Divine responses, and refuse source/ledger/range/value/base/level forgeries and null/40 quality caps. Browser checks cover exact sourced effect, Shift/repeat/refusal/cancel, past-state new films with original futures preserved, reload, source-only Armour/quality facts, Divine, Alt/keyup,390px refusal/link focus/no overflow,503/retry,seven previous-base operations and all-eight-base byte-exact film reload. Perfect/desktop/narrow screenshots were visually reviewed.

All71 existing catalog files and old base metadata are byte-preserved. Thirty registry records changed only for bounded Helmet support;190 unrelated and65 excluded records are preserved. Current45 barriers: other target16, rule/data22, engine state2, reintroduction5. The remaining Essences are Perfect Mind, Delirium and Insanity; their unresolved effects/pools/dependencies are not guessed.

Legacy browser30/cachepersist and exhaustive browser checks of every older material were not rerun. Temporary browser/validation containers closed normally. Testcontainers used disposable test DBs; original/QA DB volumes were preserved and only app/frontend QA services replaced. No push/merge/deployment. [Machine-readable executed evidence](evidence/workbench-helmet-validation-2026-10-03.json); source/review/build/API/browser/audit scripts, HTML, captures and screenshots are under codex/qa-20261003/helmet-*.

## Ring next and final gap preparation

[Iron Ring](https://poe2db.tw/us/Iron_Ring), FourRing1, DropLevel1, has fixed source endpoints attack maximum added physical damage4–4 and minimum1–1. These avoid variable implicit re-roll uncertainty; both endpoints must be preserved if represented as one two-stat implicit. Existing modifier validation separates implicit and explicit family conflicts, so an ordinary explicit sharing PhysicalDamage is not inherently excluded by the fixed implicit. The quality cap is absent in reviewed fields and must stay unknown. Unset Ring is a DropLevel44 alternative with fixed skill-slot stat, introducing unnecessary skill scope.

After the Helmet cache, Ring requires77 new public detail lookups instead of106; the prepared review script reuses the expanded cache at concurrency2. This is a next candidate, not a complete Ring pool or implemented Perfect Mind. Once reviewed and integrated, the remaining individual blocker ledger and combined existing-base validation can be used to prepare the final gap report; no unknown rule becomes implemented just to reduce that list.
