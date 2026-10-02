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
| Sinistral/Dextral Coronation | [PoE2DB Omen descriptions](https://poe2db.tw/us/Omen) retain the Regal prefix/suffix effects, but [official 0.3.0](https://www.pathofexile.com/forum/view-thread/3826682) says these two omens can no longer be obtained. | Do not enable from the retained description alone. Existing registry availability restriction remains intact. Reintroduction has not been established; any legacy-only mode would need an explicit scope decision. |
| Sinistral/Dextral Alchemy | PoE2DB retains maximum prefix/suffix effects; official 0.3.0 also disabled obtainability of both. | Same availability blocker, plus four-modifier distribution and dual-omen conflict semantics. Both maximums cannot fit a four-modifier Solar result. Do not silently choose execution order. |
| Greater Exaltation/Annulment | PoE2DB descriptions specify two additions/removals. Official 0.3.0 disabled Greater Annulment obtainability; this does not establish Greater Exaltation removal. | Prioritize Greater Exaltation separately. Verify behavior with one remaining affix slot and combining count/side restrictions. Greater Annulment remains availability-blocked. |
| Whittling + Erasure | Individual minimum-modifier-level and side restriction effects are sourced. | Determine whether both can activate and whether minimum level is computed globally or after side restriction. Current backend blocks multiple matching omens. UI draft prevents activation of these unverified same-trigger combinations. |
| Fracturing Orb | [PoE2DB Currency](https://poe2db.tw/us/Currency) describes random locking of an existing explicit on Rare equipment with at least four modifiers. | Add modifier-instance fracture identity and preservation rules across every existing removal/replacement/reroll path; verify eligible fracture candidates and restricted item states. Probability alone is not the only missing data. |
| Vaal Orb | Current Currency description establishes corruption and unpredictable modification. | Complete equipment-specific outcome set, corruption affix pools and flags, weighting/eligible-outcome evidence and interactions. The general description is insufficient to assign 1/N. |
| Hinekora's Lock | [Current PoE2DB item page](https://poe2db.tw/us/Hinekoras_Lock) identifies the item; [Stackable Currency](https://poe2db.tw/Stackable_Currency) describes foreseeing the next currency result and loss on modifying the item. | Persistent foreseen event state, supported action domain and invalidation semantics. Do not expose a client seed as a substitute. |
| Essences, catalysts, liquid emotions and other equipment methods | The existing registry is a display inventory, not a verified implementation. | Equipment-type-specific guaranteed mods, applicable layer and level rules, catalyst stat-tag/value transformations, annointment passive identity, candidate completeness and combination rules. Audit each family rather than counting every registered entry as an applicable action. |

## Version and obtainability caution

[The official patch index](https://www.pathofexile.com/forum/view-forum/2212) currently lists [0.5.5d](https://www.pathofexile.com/forum/view-thread/4008534), published 2026-09-27, above 0.5.5c/b and [0.5.5 Forbidden Rites](https://www.pathofexile.com/forum/view-thread/4000864), published 2026-09-02. PoE2DB's current homepage also identifies this 0.5.5 event. The checked 0.5.5 and b/c/d notes do not establish Coronation reintroduction or an Alchemy effect change. This is a limited patch cross-check, not a claim that every intermediate hotfix or every crafting method has been audited. A community forum's estimated 0.6 timeline is not an official release source.

[Official 0.5.0 notes](https://www.pathofexile.com/forum/view-thread/3932540) show that Omen of Corruption and the two Homogenising omens have Standard-only exchange availability; the Recombinator is disabled and Omen of Recombination is removed. They also allow simultaneous map-specific chaotic omens, which does not establish the equipment Whittling/Erasure combination. These findings reinforce the need for league/version-specific inventory auditing. They do not change the existing supported eight equipment omen effects by inference. A page's continued presence on PoE2DB is not proof of current league obtainability.

## Current verification

Backend Docker Java 21: `spotlessApply check generateJooq bootJar` passed; XML totals 117 unit/API/architecture tests plus 6 integration tests, zero failures and zero skips. Testcontainers used disposable databases, not the original project's DB/volume.

Frontend Docker Node 24: `lint`, `typecheck`, `format:check`, 81 tests and production build passed. Includes favorite-based omen activation/conflict prevention, Shift repeat, empty/non-action cancellation, Alt-held inline ranges and keyup/blur restoration, per-session linear history with original future preservation, reload restoration and storage failure handling.

Docker Chromium against the isolated QA backend passed 30 checks with zero runtime errors: real Alchemy/Exalted/Divine application, repeat and blocked results, controlled network failure, cancellation, omens, history browse/fork/reload/archive/new-base, real craft under simulated quota failure, corrupt-storage preservation, desktop side-by-side layout and 390px local stash scroll. Screenshots and machine-readable checks are under the workspace's `codex/qa-20261002` directory. Two browser helpers initially asserted text that the UI does not render; checks were corrected to the actual rarity class and load-failure message. No product behavior was changed to satisfy them.

History uses `hephaistos.workbench.films.v1` localStorage through a repository boundary. A new base archives older sessions. Viewing alone does not split; successful crafting from a past frame creates a new linear film while preserving the original future. Previous/next stays in the selected film. Capacity is capped at approximately 2 MB UTF-16 storage; write failures retain in-memory films, and corrupt or oversized existing storage is preserved without automatic overwrite. No login or DB history migration was added.

## Environment recovery and remaining validation

Docker originally failed with read-only filesystem / I/O errors when C: had about 10.5 MiB free. Following explicit approval, the user moved Docker data to E: using Docker Desktop. The agent performed one authorized official Docker Desktop stop, without force, and read-only checks; no pruning, volume deletion, DB reset or destructive migration occurred. Docker is healthy after recovery. The actual data disk is under `E:/Docker/WSL/DockerDesktopWSL/DockerDesktopWSL/disk`.

The original project's existing PostgreSQL volume was read-only checked against its two older successful migrations, then returned to its prior stopped state. Its older schema lacks the latest cache table; this is not evidence of data loss. Current runtime QA uses a separate project and volume. Existing-data full equivalence cannot be claimed from an incomplete pre-move inventory.

Current cache persistence was revalidated against the isolated QA app/DB. A bounded normal-Solar Support request expanded 10 states and 410 edges, computing one pool; after a normal restart of only the QA app, the identical request had one persisted hit, nine memory hits and zero computed pools under the same namespace `176af2393ce435c4b93e17398cc3f9be6b5cd0ab651d75299f456c629199ab6c`. A read-only QA SQL count confirmed one stored pool in one namespace. The bounded report is not a complete goal search or a validation of all support routes. The six integration tests separately check namespace isolation. Evidence: `codex/qa-20261002/cache-before.json` and `cache-after.json`.
