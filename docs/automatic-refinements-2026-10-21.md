# Automatic crafting refinements — 2026-10-21

This change follows the user's 2026-10-21 request. It supersedes the earlier automatic-policy choices for the three deferred omens; manual Workbench mechanics remain available. Runtime resource budgets are unchanged.

## Automatic policy and Exalted choices

`OMEN:SINISTRAL_ERASURE` (좌측 말소의 징조), `OMEN:DEXTRAL_ERASURE` (우측 말소의 징조), and `OMEN:WHITTLING` (절사의 징조) are DEFERRED. The fixed 187-row roster remains intact: USE 33, CONDITIONAL 15, EXCLUDED 110, DEFERRED 29. Tests of historical low-level omen behavior explicitly use a pre-deferral fixture rather than quietly reenabling those omens in production.

Exalted choices use the real eligible branches. An omen producing an identical eligible law is redundant and ordinary Exalted remains. Otherwise the ordinary choice is removed only when a complete, supported omen law preserves every improving concrete outcome with at least its original probability. Incomparable or incompletely modeled choices remain; no claim that every omen is beneficial is made.

## Chaos within 100 uses

The active interface uses the 100-cycle first-hit observation for a Chaos follower. Preparation is separate and is not charged 100 times. Other single-pass currencies retain their probabilities without an unnecessary single-attempt label. Historical frozen scenarios may retain their original observation horizons.

The calculation uses state transitions and absorbs the first goal/checkpoint. Failed Chaos rolls with no milestone change continue from their actual states, including multi-affix states. Losing an already matched milestone remains a loss. Only the established single non-fractured affix, complete-removal, unchanged-quality pool is folded to a renewal representative. Other outcomes retain their actual states and supported loss/unresolved semantics. Independent geometric and state-dependent urn examples separately verify 100-cycle mass; the geometric expression is not a blanket assumption imposed on every Chaos law.

New live calculations stop at 100 Chaos cycles. Surviving failures retain their exact internal law and are shown as “target not reached within 100 attempts”; they do not launch new item paths from every unsuccessful attempt. A route strategy treats this finite-horizon remainder as failure, rather than silently looping another 100 attempts until an unlimited-retry probability is obtained. Historical frozen 300/500-cycle observations remain readable. A deterministic quality-40 checkpoint is performed immediately after the required cap is acquired, before splitting into affix acquisitions; in the approved untyped model it leaves the item and affix laws unchanged.

## Minimum quality and separate work

`goal.general.quality` is an optional integer minimum from 0 to 40. Missing/null means no added quality condition, preserving earlier goal JSON. The goal is retained through editor storage, original requests, and scenario export/import. A separate `abstractQuality` value carries completed untyped work and is not an invented typed catalyst or numeric affix multiplier; missing historical values default only the work-completion metadata in memory.

For reviewed catalyst-quality equipment, a minimum up to 20 can use a distinct `WORK:QUALITY_20` step. It does not consume Essence of the Breach. Above 20, the existing approved abstract quality-40 workflow is retained: Essence of the Breach (`ESSENCE_BREACH`, 균열의 에센스) must first supply the verified `amulet:prefix:essence-maximum-quality` modifier, raising the cap by 20 to 40; this does not itself raise applied quality. The essence uses its existing rarity, equipment, removal, prefix-slot, and replacement restrictions. Separate `WORK:QUALITY_40` must finish before the modifier is removed. Afterwards quality work is preserved whether that modifier stays or is removed; removal is not forced.

The requested value is a minimum: for example, a minimum of 30 uses the previously approved work-to-40 path. The verified current per-base registry and kernel determine eligibility; an unreviewed quality path is UNSUPPORTED, not a successful route. No catalyst type, catalyst count, price, or stat-scaling effect is inferred. This does not expand the automatic policy to weapon/armour quality currencies or deferred jewel crafting.

Cost accounting keeps an actual Breach essence use separate from `WORK:QUALITY_20` / `WORK:QUALITY_40`. One work entry means one abstract completed task, not one catalyst. Its real catalyst count and price are not established by this project's approved model, so the result is not a complete real-material cost estimate. The current registry enables catalyst quality and the Breach cap workflow for reviewed rings and amulets. A quality-only goal can compare already known typed quality without pretending to implement affix scaling; goals that inspect quality-scaled affixes still remain unsupported.

## Persistence and verification scope

New checkpoints preserve quality work independently of item rolls. Historical v8/v9 observations are read-only and cannot be resumed under the new policy; old documents are not rewritten merely by reading them. Explicit recalculation uses the current policy. The live transport rejects execution with a stale ruleset; read-only historical results remain readable across the policy change.

Tests cover the current complete policy inventory, exact 100-use first-hit mass and restoration, useful Exalted omens, full prefix/suffix sides, quality 20 and above-20 prerequisites, cap retention/removal, optional goal JSON and scenario round trips. Runtime/UI verification results must be reported separately; this specification does not claim they have already passed.

## Remaining bounded-search regression

Exact multi-affix Chaos retries differ from the earlier one-pass loss absorption. The existing product scenarios `05-whittling-preserve-life`, `06-tier-preparation`, and `07-rarity-preparation` currently fail their unchanged full-exhaustion assertions. After the finite-horizon display/strategy correction, each uses only one expandable state: scenario 05 has not finished after its existing 120,000 cooperative test steps (1,048 retained records); scenarios 06 and 07 reach the 20,000 retained-record budget (20,065 and 20,032 observed respectively). Their goals are found, but their complete laws are not exhausted within the original bounds. The failed assertions remain enabled; finding a goal or pausing at a limit is not treated as a passing completion test.

The existing single-affix renewal proof and addition-only terminal sharing cannot safely merge these multi-affix failures: their retained modifier families can change the next pool and their later eligible actions. Extending conditional state transport to a multi-affix Markov quotient would be a larger representation change. Neither replacing these laws with an independent geometric formula, dropping their follow-up routes, nor raising resource limits is part of this patch. This regression blocks declaring the implementation ready for promotion.

Final verification and the distinction between fixed-test budgets and normal product continuation are recorded in [the verification report](automatic-refinements-verification-2026-10-21.md). QA followed the existing resume protocol without editing any limit: scenarios 06/07 also paused without further resume at the normal 50,000-record maximum; scenario 05 remained running after 234,571 steps within the bounded observation. The larger representation extension has not begun and awaits the user's decision.
