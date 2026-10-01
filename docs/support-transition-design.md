# Solar Support transitions and calculation coverage

2026-10-01 checkpoint. Implementation: `AdditionRules` and `AdditionTransitions`. Support recommendation service and goal UI are the next checkpoint; this document distinguishes implemented transitions from proposed search/storage.

## Implemented distribution

All 12 finite addition actions are implemented: ordinary/Greater/Perfect Transmutation, Augmentation, Regal, Exalted. Workbench sampling and exact transitions share `AdditionRules` eligibility, rarity change, affix capacities, family exclusion, minimum modifier level and its highest-eligible-group exception. Ordinary Sinistral/Dextral Exaltation can restrict addition; matching omens are consumed, unrelated omens remain. Unverified tiered interactions/combinations block with the same reason and preserve source/omens. Recovery and rerolls return explicitly unavailable outside this model.

Each outcome retains the exact integer published-weight numerator and pool denominator. Numeric rolls integrate to one and are marginalized; explicit IDs/tiers remain in `StateBucket`. This is an exhaustive one-action modifier distribution, not Monte Carlo and not yet an end-to-end Support result. Goal probabilities will use numerical accumulation with an explicitly checked mass tolerance; edge integer ratios do not mean all later floating-point sums are rational arithmetic.

Docker Java 21 validation: 98 unit/API/architecture tests + 5 integration tests, zero failures/errors/skips; format and bootJar passed. New tests check all 12 probability sums, every candidate's sampler selection boundary and output bucket, parity of ordinary four with the existing engine, minimum-level exception/below-floor preservation, single-omen eligibility/consumption and unverified combinations, affix exhaustion, recovery/reroll exclusion. No controller/UI API for Support is claimed in this checkpoint.

## Approved goal and tier direction

The approved goal is required family conditions AND at least N distinct candidate families, with a minimum tier on each condition. Actual numerical stat thresholds are deferred. Same family across multiple tiers counts once. The bundled catalog has 30 explicit families; in every family the tier ordering has non-increasing required item level as tier numbers increase. For example Life T1 is level 60, T2 level 54, T9 level 1; Spirit T1 level 54, T5 level 16. Thus the UI must label **“T2 or better (T1–T2)”**, rather than ambiguous integer “tier >= 2”. Future catalogs must revalidate this ranking or provide an explicit rank map. Tier direction is established from this catalog, not from an arbitrary numerical convention.

The evaluator should test the goal at the root and after every addition. Already-satisfied roots absorb with probability 1 and need no currency. First-hit successes absorb and stop; subsequent descendants do not add a second success. All first-hit masses for each fixed currency sequence are summed. One goal only. Annulment/Chaos recovery remains a validated new-root state jump, separate from success probability; Divine/Blessed remain outside these modifier-family/tier goals.

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

Proposed evaluator: forward dynamic programming per fixed sequence, merging equal goal states, with success absorption after each step. A computation budget limits expanded states/edges and optionally wall time. On interruption return computed first-hit mass as a lower bound and lower bound + pending unexpanded mass as an upper bound, plus verified terminal-failure mass. Preserve `success + failure + unresolved = 1` within tolerance. Display partial status, expanded/remaining mass, evaluated/total sequence count. Never label unfinished work “0%”, “no path”, or exact. Top 3–5 ordering is certified only when relevant intervals separate; otherwise show provisional comparison and continuation. Excluding an eligible currency grade for budget reasons must be visible, not a silent fallback to four ordinary actions.

## Goal-independent transition storage and invalidation

Proposed persistent/lazy cache key: projection-schema version + catalog snapshot ID + raw/details source hashes + rule version + assumption ledger version/content digest + base ID + item level + rarity + concrete implicit identity/values + conditions + occupied family mask + action + active-omen context. Store eligible modifier/tier channels and integer weight/totalWeight, next occupied family, rarity and omen consumption. Reconstruct concrete ID buckets only where needed. This goal-independent projection can reuse pools across previous tier choices; preserve source evidence with every stored record.

Goal first-hit probabilities are a separate cache keyed by that transition namespace + normalized family/threshold/N goal + initial qualification state + fixed sequence + evaluator schema/version. They must include completion/coverage metadata and must not overwrite exact results with partial ones. Modifying sources, rules, grouping, tier ranks, ledger, base capacities or projection schema changes the namespace; stale entries are not served. Changing a goal invalidates goal-result cache only, not verified goal-independent transitions. Bounded eviction and lazy materialization are needed: even 295,412 occupancies times 12 actions and up to 209 channels is too large to eagerly instantiate without a measured storage budget. No DB migration or persistent cache has been added in this checkpoint.
