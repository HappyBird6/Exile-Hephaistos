# Solar Support transitions and calculation coverage

2026-10-02 implementation status. `AdditionRules` and `AdditionTransitions` implement exact one-action distributions; `SupportGoals`, `AdditionPoolCache`, `SupportRecommendations` and the independent Craft Support UI implement finite-sequence comparisons. Quantitative counts and microbenchmarks below are the earlier catalog investigation, not fresh full-state enumeration.

## Implemented distribution

All 12 finite addition actions are implemented: ordinary/Greater/Perfect Transmutation, Augmentation, Regal, Exalted. Workbench sampling and exact transitions share `AdditionRules` eligibility, rarity change, affix capacities, family exclusion, minimum modifier level and its highest-eligible-group exception. Ordinary Sinistral/Dextral Exaltation can restrict addition; matching omens are consumed, unrelated omens remain. Unverified tiered interactions/combinations block with the same reason and preserve source/omens. Recovery and rerolls return explicitly unavailable outside this model.

Each outcome retains the exact integer published-weight numerator and pool denominator. Numeric rolls integrate to one and are marginalized; explicit IDs/tiers remain in `StateBucket`. The exhaustive one-action distribution feeds the Support evaluator without Monte Carlo. Goal probabilities use floating-point accumulation with checked mass tolerance; edge integer ratios do not mean all later sums are rational arithmetic.

The earlier transition checkpoint passed 98 unit/API/architecture tests + 5 integration tests in Docker Java 21, checking all 12 probability sums, sampler boundaries, engine parity, minimum levels, omens and exclusions. Current validation and remaining work are recorded in the dated STOP_CHECKPOINT documents, superseding this historical count.

## Approved goal and tier direction

The approved goal is required family conditions AND at least N distinct candidate families, with a minimum tier on each condition. Actual numerical stat thresholds are deferred. Same family across multiple tiers counts once. The bundled catalog has 30 explicit families; in every family the tier ordering has non-increasing required item level as tier numbers increase. For example Life T1 is level 60, T2 level 54, T9 level 1; Spirit T1 level 54, T5 level 16. Thus the UI must label **“T2 or better (T1–T2)”**, rather than ambiguous integer “tier >= 2”. Future catalogs must revalidate this ranking or provide an explicit rank map. Tier direction is established from this catalog, not from an arbitrary numerical convention.

The evaluator tests the goal at the root and after every addition. Already-satisfied roots absorb with probability 1 and need no currency. First-hit successes absorb and stop; subsequent descendants do not add a second success. All first-hit masses for each fixed sequence are summed. Recovery currently accepts actual recovered modifiers or verified text as a new root and clears the previous result. Annulment/Chaos execution and branch-selection controls in Support are not implemented; their probabilities are never combined with the addition result. Divine/Blessed remain outside these goals.

`IncreaseSocketedGemLevel` contains 12 definitions with four distinct stat/effect alternatives: Melee, Projectile, Minion and Spell. The goal explicitly accepts **any effect in the selected family** at the selected tier or better; effect-specific filters are not supported. The family API preserves every actual modifier alternative and adds one source example per stat identity. Goal tier options are unique and sorted. Every family still counts once.

## Quantitative state investigation

Counts below use the reviewed catalog, positive-weight definitions at/below item level, family uniqueness, rare capacities 3 prefixes/3 suffixes, and marginalized numerical values. An exact family polynomial selects zero or one tier variant per family and caps each affix count. These are combinatorial counts, not benchmarked enumeration of all states, and do not assert support for other equipment.

| Item level | Eligible explicit definitions | Families | Valid rare ID/tier buckets, 0–6 explicits |
|---|---:|---:|---:|
| 35 | 93 | 30 | 177,023,808 |
| 44 | 113 | 30 | 481,168,704 |
| 50 | 129 | 30 | 1,165,178,140 |
| 70 | 177 | 30 | 7,815,904,448 |
| 82 / 100 | 209 | 30 | 19,990,445,910 |

At level 82, rare counts by explicit count 0…6 are 1; 209; 21,024; 1,355,570; 54,414,706; 1,349,763,400; 18,584,891,000. Magic has 10,578 valid buckets including empty. From a Normal root, rare states start at two explicits after Transmutation→Regal; rare 0/1 are valid manual roots but not reached by that path. Normal-root reachable rare combinations total 19,990,445,700 under ordinary additions. Higher-tier pools are subsets and do not enlarge that union.

Family occupancy without tier variants has only **295,412** rare combinations: 12 prefix and 18 suffix families, at most 3 of each. Occupied families, affix counts, rarity and item level determine all future addition pools; existing exact modifier tiers no longer affect those pools. Goal evaluation still retains which selected families qualified for their threshold. Merging goal states requires equal occupancy **and** equal goal qualification state, not just the same number of successful families. This is a valid finite-addition projection only; Whittling/recovery invalidate it.

295,412 is a pool-key count, not a complete goal-state or performance bound. Up to six occupied target families can carry qualification bits, giving a loose upper bound of 295,412×64 = 18,906,368 ephemeral goal states before initial-state restrictions/first-hit pruning. Shared fixed-sequence prefixes can reuse forward distributions, but their caches must include goal and qualification context. These reductions still require measured limits and explicit incomplete-coverage handling.

Microbenchmark in Docker Java 21: level-82 empty valid states, 200 warm-up calls, then 1,000 calls per action, no persistence/hash/HTTP/goal evaluation included. Total 1,528,000 outcome records. Mean times per action were 19.149–78.052 microseconds/call; returned candidate counts were 209 ordinary, 104 Greater Transmutation/Augmentation, 41 Perfect Transmutation/Augmentation, 118 Greater Regal/Exalted, 83 Perfect Regal/Exalted. This small hot-run measurement does not predict end-to-end search latency or justify enumerating 19.99 billion buckets. Benchmark helpers/log are outside tracked source/build artifacts.

## Finite sequences and safe budget semantics

Every normal addition increases explicit count by exactly one and never removes/replaces. The rank `6 - explicitCount` strictly decreases, so any valid path has at most six additions from an empty item. Rarity and per-affix capacities can terminate earlier. No arbitrary long sequence limit is needed for this domain. A branch with unavailable action is a verified terminal failure for that fixed sequence; an uncomputed branch is not.

Upper-bound full-length syntactic sequence count from a Normal empty root is **1,458**: Transmutation→Regal→4 Exalted and Transmutation→Augmentation→Regal→3 Exalted, each with three currency grades at every step (2×3^6). Item level/eligibility may reduce this. Rare roots with k explicits have at most 3^(6-k) Exalted-grade sequences. Prefix recommendations can be deduplicated when extending the sequence changes no first-hit success mass. Price/cost optimization is not modeled.

Implemented evaluator: forward dynamic programming per fixed sequence, merging equal occupancy and goal qualification, with success absorption after each step. Budgets limit expanded states/edges and wall time. On interruption it returns computed first-hit mass as a lower bound, lower bound + pending mass as an upper bound, and verified failure, checking `success + failure + unresolved = 1`. The UI shows partial status, unresolved mass and evaluated/total sequences. Up to five comparisons are ordered by success lower bound; ranking is certified only when all eligible sequences are fully calculated. Otherwise candidates are provisional and unexamined sequences retain the possible range 0–100%. No continuation token is implemented; retry recalculates using reusable pools. Eligible grades are never silently omitted.

## Goal-independent transition storage and invalidation

Implemented lazy cache namespace: projection version + catalog metadata/source hashes + normalized definitions/base capacities + rule version + ledger digest. Each key includes snapshot/base ID, item level, rarity, concrete implicit identity/values, conditions, occupied families, action and active omens. Pools store modifier/tier channels, integer weight/totalWeight, upgraded rarity and remaining omens. Reconstructed buckets preserve actual caller IDs/tiers. These goal-independent pools can be reused across tiers and goals.

Additive migration `V202610010001__support_addition_pool.sql` persists PostgreSQL pools, with a 2,048-entry in-memory LRU cache. Existing migrations/data are preserved. Changes to sources, rules, grouping, tiers, ledger, capacities or projection version change the namespace. Goal probabilities are recomputed, not cached separately. A future goal-result cache would require normalized family/tier/N, initial qualification, sequence/evaluator version and completion metadata; it must never overwrite exact results with partial results. Pools remain lazy because full eager materialization is unmeasured. On 2026-10-02, a QA backend process restart yielded three PostgreSQL hits, zero memory hits and zero computed pools for a different goal, then three memory hits for the original goal with unchanged probabilities.
