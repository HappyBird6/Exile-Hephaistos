# Workbench equipment rule audit — 2026-10-02

This audit distinguishes registered inventory from executable rules and from the game's complete inventory. It is not a declaration that all equipment crafting is implemented. Map and skill-gem crafting are excluded.

## Baseline and model

Fetched `HappyBird6/Exile-Hephaistos` master at `e937ddccf7493ea0d114139142ff36224c3a672c`. The checked-out registry contained 220 entries: 17 implemented currencies, 8 implemented omens, and 195 pending entries. Among pending entries, 23 were currencies; the visible Currency stash exposes only four pending currencies, not all 23. Registry membership does not assert current obtainability.

The supported equipment base remains Solar Amulet only, using snapshot `poe2db-amulets-base-2026-09-29-a4f439852790`, retrieved 2026-09-29, with 81 prefix and 128 suffix definitions. All explicit entries have positive published weights. Rare capacity is three prefixes and three suffixes, Magic capacity is one of each. Conditions such as Corrupted, Mirrored, Unidentified and Sanctified are rejected by the current validator. Other equipment types have no executable catalog in this implementation.

## Verified first expansion

| Rule | Effect and eligibility | Generation and probability | Interaction and scope | Evidence checked 2026-10-02 |
| --- | --- | --- | --- | --- |
| Orb of Alchemy | Normal or Magic equipment becomes Rare with exactly four new explicit modifiers. Existing Magic explicit modifiers are replaced; existing implicit values remain. Rare input is blocked. | Reuse the existing Solar level-eligible, family-conflict-free weighted pool at each draw. Remove occupied families and full affix sides between draws. Per-event selection probability is published weight / current pool total. Single-stat numeric rolls retain the existing uniform integer assumption with N and source URL. | No minimum-modifier-level tier is asserted. Only Solar Amulet is supported. None of the currently implemented eight omens triggers Alchemy; they remain active. Alchemy-specific omens remain unsupported and cannot be submitted as supported active omens. | [PoE2DB Currency](https://poe2db.tw/us/Currency), current generated item description; [GGG 0.3.1 patch](https://www.pathofexile.com/forum/view-thread/3862213), Magic replacement; [GGG 0.3.1 endgame announcement](https://www.pathofexile.com/forum/view-thread/3860076), four new modifiers. |

The older community-authored [PoE2DB Crafting page](https://poe2db.tw/us/Crafting) still lists Normal-only Alchemy, and its Dextral Coronation explanation incorrectly repeats prefix. Use the generated item descriptions and official patch over that stale summary. Do not import PoE1 Alchemy's variable affix count into this rule. No measured game distribution or alternate weighting of complete unordered four-affix sets is asserted; this simulator uses the same sequential conditional weighted generation model as its existing addition rules.

Alchemy regression: 100 deterministic seeds for each of Normal and Magic input, four generated modifiers, validator acceptance, exact event probabilities at every conditional pool, Magic replacement events, unchanged implicits and original inputs, and blocked Rare reapplication. This is coverage at level 82, not exhaustive coverage of every item level or every result.

Registry after this expansion: 220 registered entries, 18 implemented currencies, 8 implemented omens, 194 pending entries. Pending counts are not a full-game backlog count.

## Next equipment rules and explicit blockers

| Family | Evidence established | Still required before executable support |
| --- | --- | --- |
| Sinistral/Dextral Coronation | [Current PoE2DB Omen descriptions](https://poe2db.tw/us/Omen) constrain the next Regal addition to prefix/suffix respectively. | Extend supported omen contract and frontend validation, verify Greater/Perfect trigger semantics and matching-combination policy; regress conditional eligible pools. Ordinary Regal alone is independent of unresolved Greater/Perfect interactions. |
| Sinistral/Dextral Alchemy | Current Omen descriptions require maximum prefix/suffix count. | Establish the four-modifier affix distribution and dual-omen conflict semantics. Both maximums cannot fit a four-modifier Solar result. Do not silently choose execution order. |
| Greater Exaltation/Annulment | Current Omen descriptions specify two additions/removals. | Verify behavior with one remaining affix slot or one removable affix; combining count and side restrictions requires evidence. |
| Whittling + Erasure | Individual minimum-modifier-level and side restriction effects are sourced. | Determine whether both can activate and whether minimum level is computed globally or after side restriction. Current backend blocks multiple matching omens. UI draft prevents activation of these unverified same-trigger combinations. |
| Fracturing Orb | [PoE2DB Currency](https://poe2db.tw/us/Currency) describes random locking of an existing explicit on Rare equipment with at least four modifiers. | Add modifier-instance fracture identity and preservation rules across every existing removal/replacement/reroll path; verify eligible fracture candidates and restricted item states. Probability alone is not the only missing data. |
| Vaal Orb | Current Currency description establishes corruption and unpredictable modification. | Complete equipment-specific outcome set, corruption affix pools and flags, weighting/eligible-outcome evidence and interactions. The general description is insufficient to assign 1/N. |
| Hinekora's Lock | [Current PoE2DB item page](https://poe2db.tw/us/Hinekoras_Lock) identifies the item; [Stackable Currency](https://poe2db.tw/Stackable_Currency) describes foreseeing the next currency result and loss on modifying the item. | Persistent foreseen event state, supported action domain and invalidation semantics. Do not expose a client seed as a substitute. |
| Essences, catalysts, liquid emotions and other equipment methods | The existing registry is a display inventory, not a verified implementation. | Equipment-type-specific guaranteed mods, applicable layer and level rules, catalyst stat-tag/value transformations, annointment passive identity, candidate completeness and combination rules. Audit each family rather than counting every registered entry as an applicable action. |

## Version and obtainability caution

[Official 0.5.0 notes](https://www.pathofexile.com/forum/view-thread/3932540) show that Omen of Corruption and the two Homogenising omens have Standard-only exchange availability; the Recombinator is disabled and Omen of Recombination is removed. They also allow simultaneous map-specific chaotic omens, which does not establish the equipment Whittling/Erasure combination. These findings reinforce the need for league/version-specific inventory auditing. They do not change the existing supported eight equipment omen effects by inference. A page's continued presence on PoE2DB is not proof of current league obtainability.

## Current verification and blocker

Backend Docker Java 21: `spotlessApply check generateJooq bootJar` passed; XML totals 117 unit/API/architecture tests plus 6 integration tests, zero failures and zero skips. Testcontainers used disposable databases, not the original project's DB/volume.

Frontend first UI subset: Docker Node 24 `lint`, `typecheck`, `format:check`, 74 tests and production build passed, including Shift retention, empty stash cancellation, Alt-held ranges and keyup/blur restoration. Subsequent favorites-based Omen and side-by-side layout draft has not completed verification. Its initial test-helper `getByRole` options used unsupported `exact`; that option was removed. No successful post-fix runtime/typecheck is claimed.

Docker browser image export and QA backend build then failed with read-only filesystem / I/O errors. Read-only host inspection showed C: free space 10,997,760 bytes (about 10.5 MiB), while E: had about 95 GiB. No system cleanup, Docker-wide restart, reset, pruning or volume deletion was attempted. Actual browser QA, latest UI regression, session-history implementation and cache-persistence revalidation remain outstanding. Past-session browser/cache results are not current verification.
