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

Registry after Alchemy: 220 registered entries, 18 implemented currencies, 8 implemented omens, 194 pending entries. After the Fracturing expansion below: 19 currencies, 8 omens, 193 pending. Pending counts are not a full-game backlog count.

## Fracturing expansion

| Rule | Effect and eligibility | Generation and probability | Interaction and scope | Evidence checked 2026-10-02 |
| --- | --- | --- | --- | --- |
| Fracturing Orb | Rare Solar Amulet with at least four ordinary explicit instances and no existing fracture. Select one existing explicit, preserving its values and marking it permanently locked. Reapplication is blocked. | Eligible candidate IDs and N are listed in `uniform-fracture-v1`; select uniformly 1/N because published fracture selection weights are unavailable. No numeric reroll or affix-weight draw occurs. | The single locked explicit survives ordinary/Greater/Perfect addition, Chaos removal/replacement, Annulment and Divine. Locked-only removal is unavailable. Implicit fracture, multiple fractures, non-Rare fractured state, Corrupted/Mirrored/special layers and other equipment bases remain unsupported. Whittling plus fracture is blocked because global-minimum versus removable-minimum priority is unverified. | [GGG 0.2.0](https://www.pathofexile.com/forum/view-thread/3740562) establishes Rare/four-modifier random lock; [PoE2DB Fractured Modifiers](https://poe2db.tw/us/Fractured_Modifiers) says locked modifiers cannot be removed or altered; [current PoE2DB Fracturing Orb](https://poe2db.tw/Fracturing_Orb) description excludes already fractured items. |

Concrete `ModifierInstance` carries a boolean fracture flag; the existing two-argument constructor and older JSON without the field default to false. History stores the flag and validates its shape against the current catalog. Frontend rejects ordinary responses that lose/change a locked instance, invent locks or return inconsistent fracture evidence. The item card marks `[Fractured]` during both rolled and Alt-range display. Display-only clipboard fracture evidence is not newly mapped; existing strict mapper restrictions still apply.

Rule version is now `solar-workbench-fracture-v3`; ledger version is `solar-uniform-assumptions-v2`. Existing DB cache namespaces are preserved and fresh namespaces prevent using the old rule/ledger cache. The finite Support/Explorer bucket model is not expanded to represent locked explicit instances; Workbench uses concrete states independently.

Fracturing validation: 100 spaced deterministic seeds on each four/six-modifier input, every candidate reached, exact 1/N event/ledger, unchanged original values and implicit, one lock, repeated fracture refusal; 100 seeds across eight subsequent currency variants preserve the lock. API tests cover older JSON and fracture/Divine round-trip. An initial seed loop used closely spaced Java Random seeds whose first bounded draws were correlated and reached one candidate; test seeds were spaced without changing production random selection. Frontend adds three contract/history tests. No empirical game distribution is claimed.

Current Docker mandatory checks passed: 121 unit/API/architecture plus six integration tests; frontend 84 tests plus lint/typecheck/format/build. Actual Chromium passed the existing 30 checks plus 20 Fracturing checks with zero runtime errors. Evidence is under `codex/qa-20261002/backend-fracture-results` and `fracturing-browser-results.json`. Screenshot review found a stale prior-action status after changing saved sessions; history browsing/session selection now clears that status, with 84 frontend tests and the 20 Fracturing browser checks passing after the correction.

## Next equipment rules and explicit blockers

| Family | Evidence established | Still required before executable support |
| --- | --- | --- |
| Sinistral/Dextral Coronation | [PoE2DB Omen descriptions](https://poe2db.tw/us/Omen) retain the Regal prefix/suffix effects, but [official 0.3.0](https://www.pathofexile.com/forum/view-thread/3826682) says these two omens can no longer be obtained. | Do not enable from the retained description alone. Existing registry availability restriction remains intact. Reintroduction has not been established; any legacy-only mode would need an explicit scope decision. |
| Sinistral/Dextral Alchemy | PoE2DB retains maximum prefix/suffix effects; official 0.3.0 also disabled obtainability of both. | Same availability blocker, plus four-modifier distribution and dual-omen conflict semantics. Both maximums cannot fit a four-modifier Solar result. Do not silently choose execution order. |
| Greater Exaltation/Annulment | PoE2DB descriptions specify two additions/removals. Official 0.3.0 disabled Greater Annulment obtainability; this does not establish Greater Exaltation removal. | Prioritize Greater Exaltation separately. Verify behavior with one remaining affix slot and combining count/side restrictions. Greater Annulment remains availability-blocked. |
| Whittling + Erasure | Individual minimum-modifier-level and side restriction effects are sourced. | Determine whether both can activate and whether minimum level is computed globally or after side restriction. Current backend blocks multiple matching omens. UI draft prevents activation of these unverified same-trigger combinations. |
| Fracturing Orb | Supported concrete Rare Solar scope is implemented above. | Broader bases, special modifier layers/states and Whittling priority need independent evidence; do not extend the supported scope by inference. |
| Vaal Orb | Current Currency description establishes corruption and unpredictable modification. | Complete equipment-specific outcome set, corruption affix pools and flags, weighting/eligible-outcome evidence and interactions. The general description is insufficient to assign 1/N. |
| Hinekora's Lock | [Current PoE2DB item page](https://poe2db.tw/us/Hinekoras_Lock) identifies the item; [Stackable Currency](https://poe2db.tw/Stackable_Currency) describes foreseeing the next currency result and loss on modifying the item. | Persistent foreseen event state, supported action domain and invalidation semantics. Do not expose a client seed as a substitute. |
| Essences, catalysts, liquid emotions and other equipment methods | The existing registry is a display inventory, not a verified implementation. | Equipment-type-specific guaranteed mods, applicable layer and level rules, catalyst stat-tag/value transformations, annointment passive identity, candidate completeness and combination rules. Audit each family rather than counting every registered entry as an applicable action. |

## Version and obtainability caution

Uncertainties are separated from approval needs:

| Category | Items | Next action |
| --- | --- | --- |
| Additional source research | Vaal equipment outcome set and corruption modifier domains; Greater Exaltation with one open slot and matching omen combinations; Whittling/fracture priority; Hinekora preview correlation and omen-change/invalidation semantics. | Gather current PoE2-specific effects and complete eligible domains. Do not ask the user to approve guessed game effects. GGG 0.5.0 explicitly changed corruption numeric modification to multiply each current value, so older reroll implementations cannot be imported. |
| Implementation/model work | Other equipment catalogs, quality/catalyst stat tags and scaling, sockets/augments, item identity/copies, signed/persistent foresight and invalidation boundary. | Extend only a verified family's required schema. Existing Solar fields do not represent weapon/armour base stats, quality, sockets, unique outcomes or corruption enchantments. No destructive DB change is required by the current Fracturing implementation. |
| User scope decision if needed | A separate legacy/Standard-only mode for officially discontinued entries; probability policy if a future eligible pool mixes published and missing weights. | Current default keeps unavailable/unverified entries blocked and preserved. No legacy mode or mixed-weight policy is silently introduced. Neither decision currently blocks independent supported Workbench work. |

[The official patch index](https://www.pathofexile.com/forum/view-forum/2212) currently lists [0.5.5d](https://www.pathofexile.com/forum/view-thread/4008534), published 2026-09-27, above 0.5.5c/b and [0.5.5 Forbidden Rites](https://www.pathofexile.com/forum/view-thread/4000864), published 2026-09-02. PoE2DB's current homepage also identifies this 0.5.5 event. The checked 0.5.5 and b/c/d notes do not establish Coronation reintroduction or an Alchemy effect change. This is a limited patch cross-check, not a claim that every intermediate hotfix or every crafting method has been audited. A community forum's estimated 0.6 timeline is not an official release source.

[Official 0.5.0 notes](https://www.pathofexile.com/forum/view-thread/3932540) show that Omen of Corruption and the two Homogenising omens have Standard-only exchange availability; the Recombinator is disabled and Omen of Recombination is removed. They also allow simultaneous map-specific chaotic omens, which does not establish the equipment Whittling/Erasure combination. These findings reinforce the need for league/version-specific inventory auditing. They do not change the existing supported eight equipment omen effects by inference. A page's continued presence on PoE2DB is not proof of current league obtainability.

## Greater Exaltation expansion

| Rule | Effect and eligibility | Generation and probability | Interaction and scope | Evidence checked 2026-10-02 |
| --- | --- | --- | --- | --- |
| Omen of Greater Exaltation | Next ordinary Exalted adds two explicit modifiers on Rare Solar. Supported only with at least two open explicit slots. All first-addition branches must leave a verified second pool before any draw occurs. | Two draws from the existing level-eligible, family-conflict-free pool, each using published PoE2DB weight/current eligible weight sum. Recompute occupied families and prefix/suffix capacity between draws. Numeric range rolls retain the existing uniform integer ledger. This is the same sequential conditional model used for Alchemy, not a measured game distribution over unordered pairs. | One matching omen consumed after successful application; unrelated omens and existing rolls/fractures remain. One remaining slot, Greater/Perfect currency and simultaneous matching omens remain blocked pending evidence. Finite Support explicitly refuses this omen and does not expose it as a supported input. | [Current PoE2DB Omen](https://poe2db.tw/us/Omen) gives two random modifiers; [individual item](https://poe2db.tw/us/Omen_of_Greater_Exaltation) identifies `OmenOnExaltAddTwoMods`. |

Registry is now 220 registered entries: 19 implemented currencies, 9 implemented omens, 192 pending. Rule version `solar-workbench-double-exalt-v4`, ledger unchanged at `solar-uniform-assumptions-v2`; earlier namespaces remain stored. Neither count means complete game coverage.

Docker backend `spotlessApply check generateJooq bootJar`: 124 unit/API/architecture plus 6 integration tests passed, zero failures/errors. New tests sample 100 spaced seeds, verify both conditional event probabilities and immutable prior/fractured instances, block uncertain slots/currencies/combinations without consumption, and reject incorrect one-addition Support modeling. Docker frontend Alpine Node 24: lint/typecheck/format, 86 tests and production build passed. A first Debian Node run passed static checks but could not load existing Alpine native bindings; the same project Alpine image resolved the environment mismatch without changing dependency files.

Actual Chromium: existing Workbench 30 and Fracturing 20 regression checks passed; Greater Exaltation adds 14 checks, zero runtime errors, including favorites/conflicts, real two-add application, consumption, blocked scopes, one history frame and reload. Initial browser inspection caught a new frontend contract check incorrectly treating the server's canonical modifier order as addition order; checks now match prior instances and new events by modifier ID and values. Frontend 86 tests and all 14 new browser checks passed after correction. Evidence: `codex/qa-20261002/backend-greater-exaltation-results`, `greater-exaltation-browser-results.json` and `greater-exaltation-desktop.png` (visually reviewed).

## Current verification

### Body essence expansion

[Official 0.3.0](https://www.pathofexile.com/forum/view-thread/3826682) replaced tag-selected essence generation with fixed modifiers; Lesser/Normal/Greater upgrade Magic to Rare, whereas Perfect/corrupted remove and replace on Rare. Current [Essence descriptions](https://poe2db.tw/us/Essence) agree. This prevents importing pre-0.3 tag-weighted outcomes or PoE1 reroll behavior.

| Tier | Current Amulet outcome | Existing Solar definition | Supported catalog item level | Source |
| --- | --- | --- | --- | --- |
| Lesser Body | +20–29 maximum Life, prefix | `amulet:prefix:healthy`, `IncreasedLife` | 6+ | [Lesser Body](https://poe2db.tw/us/Lesser_Essence_of_the_Body) |
| Body | +70–84 maximum Life, prefix | `amulet:prefix:robust`, `IncreasedLife` | 38+ | [Body](https://poe2db.tw/us/Essence_of_the_Body) |
| Greater Body | +85–99 maximum Life, prefix | `amulet:prefix:rotund`, `IncreasedLife` | 46+ | [Greater Body](https://poe2db.tw/us/Greater_Essence_of_the_Body) |

Scope: Magic Solar whose fixed result is valid in the existing catalog and does not overlap an occupied family. Existing explicit and implicit values remain. Fixed modifier selection probability is 1; only its numeric integer range uses the existing uniform ledger. Same-family overlap and lower-item-level essence bypass behavior remain explicitly unsupported rather than asserted impossible in the game. The essence page's Required Level values (4/30/36) differ from catalog generation item levels; they are not silently treated as equivalent. Perfect Body targets Body Armour and is not executable on Solar. No new modifier catalog, essence tag pool, other base, or Support/Explorer transition was introduced.

Docker backend mandatory checks passed: 126 unit/API/architecture plus six integration tests (132 total, zero failures/errors). This includes 100 spaced seeds for each of three essence tiers on one/two-explicit Magic Solar inputs, fixed-ID/range/probability checks, preservation, ledger N, and unsupported rarity/overlap/low-level refusal. Frontend lint/typecheck/format, 86 tests and production build passed. Chromium passed 19 Body essence checks plus the existing 30 regression checks against v5, with zero runtime errors; desktop screenshot was visually reviewed. The previous Fracturing 20 and Greater Exaltation 14 browser checks last ran against v4; their backend/frontend regressions are included in the v5 mandatory suites. Evidence: `backend-body-essence-results`, `body-essence-browser-results.json`, `body-essence-desktop.png` under `codex/qa-20261002`.

Registry now has 19 implemented currencies, 9 omens and 3 essences, leaving 189 pending registered entries. Rule version is `solar-workbench-body-essence-v5`; the ledger remains v2. Scope restrictions above remain real limitations. The 132/86 checks are not exhaustive game-state coverage.

Backend Docker Java 21: `spotlessApply check generateJooq bootJar` passed; XML totals 117 unit/API/architecture tests plus 6 integration tests, zero failures and zero skips. Testcontainers used disposable databases, not the original project's DB/volume.

Frontend Docker Node 24: `lint`, `typecheck`, `format:check`, 81 tests and production build passed. Includes favorite-based omen activation/conflict prevention, Shift repeat, empty/non-action cancellation, Alt-held inline ranges and keyup/blur restoration, per-session linear history with original future preservation, reload restoration and storage failure handling.

Docker Chromium against the isolated QA backend passed 30 checks with zero runtime errors: real Alchemy/Exalted/Divine application, repeat and blocked results, controlled network failure, cancellation, omens, history browse/fork/reload/archive/new-base, real craft under simulated quota failure, corrupt-storage preservation, desktop side-by-side layout and 390px local stash scroll. Screenshots and machine-readable checks are under the workspace's `codex/qa-20261002` directory. Two browser helpers initially asserted text that the UI does not render; checks were corrected to the actual rarity class and load-failure message. No product behavior was changed to satisfy them.

History uses `hephaistos.workbench.films.v1` localStorage through a repository boundary. A new base archives older sessions. Viewing alone does not split; successful crafting from a past frame creates a new linear film while preserving the original future. Previous/next stays in the selected film. Capacity is capped at approximately 2 MB UTF-16 storage; write failures retain in-memory films, and corrupt or oversized existing storage is preserved without automatic overwrite. No login or DB history migration was added.

## Environment recovery and remaining validation

### Mind and Ruin essence expansion

The same fixed-result path is extended to six additional ordinary tiers. All require Magic Solar with the target result valid at its catalog item level and without family overlap. Fixed modifier selection is deterministic (probability 1); the single numeric range retains the uniform integer ledger. Mind adds a prefix; Ruin adds a suffix. Existing modifier/implicit values and unrelated supported omens remain. Source Required Level is recorded separately and is not substituted for catalog item level.

| Essence | Fixed Solar result | Catalog modifier / minimum supported item level | Source Required Level | Source checked 2026-10-02 |
| --- | --- | --- | --- | --- |
| Lesser Mind | +25–34 maximum Mana | `amulet:prefix:azure` / 16 | 12 | [Lesser Mind](https://poe2db.tw/us/Lesser_Essence_of_the_Mind) |
| Mind | +80–89 maximum Mana | `amulet:prefix:opalescent` / 46 | 36 | [Mind](https://poe2db.tw/us/Essence_of_the_Mind) |
| Greater Mind | +90–104 maximum Mana | `amulet:prefix:gentian` / 54 | 43 | [Greater Mind](https://poe2db.tw/us/Greater_Essence_of_the_Mind) |
| Lesser Ruin | +4–7% Chaos Resistance | `amulet:suffix:of-the-lost` / 16 | 12 | [Lesser Ruin](https://poe2db.tw/us/Lesser_Essence_of_Ruin) |
| Ruin | +8–11% Chaos Resistance | `amulet:suffix:of-banishment` / 30 | 24 | [Ruin](https://poe2db.tw/us/Essence_of_Ruin) |
| Greater Ruin | +16–19% Chaos Resistance | `amulet:suffix:of-expulsion` / 56 | 44 | [Greater Ruin](https://poe2db.tw/us/Greater_Essence_of_Ruin) |

Docker mandatory checks passed: 128 unit/API/architecture plus six integration tests (134 total, zero failures/errors); frontend lint/typecheck/format, 86 tests and build. New backend tests cover 100 seeds across six tiers on one/two-explicit Magic input, fixed ranges/affix types, immutable existing rolls, unrelated omen preservation, numeric ledger, each catalog-level boundary and below-boundary refusal, family overlap and wrong rarity. Actual Chromium passed 55 checks across all nine Body/Mind/Ruin tiers, zero runtime errors: real effects and evidence, right-click use, blocked Normal/Rare repeats, preservation and reload. Screenshot visually reviewed. Evidence is `codex/qa-20261002/backend-fixed-essence-results`, `fixed-essence-browser-results.json`, `fixed-essence-desktop.png`.

Current rule version is `solar-workbench-fixed-essence-v6`, ledger remains v2; registry now 19 implemented currencies, nine omens and nine essences (183 pending of 220 registered). No other equipment base or Perfect/corrupted essence is enabled by analogy. Existing 30 general browser regressions last ran against v5, 20 Fracturing/14 Greater Exaltation against v4; their backend/frontend regressions are included in the current mandatory suites. The 55 checks do not assert unverified low-level bypasses or family-overlap outcomes.

Docker originally failed with read-only filesystem / I/O errors when C: had about 10.5 MiB free. Following explicit approval, the user moved Docker data to E: using Docker Desktop. The agent performed one authorized official Docker Desktop stop, without force, and read-only checks; no pruning, volume deletion, DB reset or destructive migration occurred. Docker is healthy after recovery. The actual data disk is under `E:/Docker/WSL/DockerDesktopWSL/DockerDesktopWSL/disk`.

The original project's existing PostgreSQL volume was read-only checked against its two older successful migrations, then returned to its prior stopped state. Its older schema lacks the latest cache table; this is not evidence of data loss. Current runtime QA uses a separate project and volume. Existing-data full equivalence cannot be claimed from an incomplete pre-move inventory.

Current cache persistence was revalidated against the isolated QA app/DB. A bounded normal-Solar Support request expanded 10 states and 410 edges, computing one pool; after a normal restart of only the QA app, the identical request had one persisted hit, nine memory hits and zero computed pools under the same namespace `176af2393ce435c4b93e17398cc3f9be6b5cd0ab651d75299f456c629199ab6c`. A read-only QA SQL count confirmed one stored pool in one namespace. The bounded report is not a complete goal search or a validation of all support routes. The six integration tests separately check namespace isolation. Evidence: `codex/qa-20261002/cache-before.json` and `cache-after.json`.

Fracturing namespace cache recheck: `847b4c36c72b9c8353e52fed202f21df50f4c334f282739c39e2d41fe5028698`; before normal QA-app restart one computed pool, after restart one persisted hit and zero computations. Comparisons and assessment are exactly equal; evidence is `cache-before-v3.json` / `cache-after-v3.json`.

Greater Exaltation namespace cache recheck: `2967cb61d8f7d2969f733f82f7c1f29e32d78ab92d56f5231108ef6015c67946`; before normal QA-app restart one computed pool, after restart one persisted hit, nine memory hits and zero computations. Comparisons and assessment are exactly equal; evidence is `cache-before-v4.json` / `cache-after-v4.json`.

Body essence namespace cache recheck: `fcfafab62f6ee72aff130534fadab0eaf0bf2570ae3460332d3bcd5e5a00b025`; one computed pool before normal QA-app restart, one persisted hit/nine memory hits/zero computations afterwards; comparisons and assessment exactly equal. Evidence: `cache-before-v5.json` / `cache-after-v5.json`.

Mind/Ruin namespace cache recheck: `f3281e63519be58c25dbcb9390177a695a5c48fdc19ab90fb36a0227ee024409`; one computed pool before normal QA-app restart, one persisted hit/nine memory hits/zero computations afterwards; comparisons and assessment exactly equal. Evidence: `cache-before-v6.json` / `cache-after-v6.json`.
