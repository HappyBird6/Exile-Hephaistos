# Wisdom identification checkpoint — 2026-10-03

**BLOCKED: use text confirmed; hidden-payload provenance and complete preservation/eligibility contract unresolved.** No material implemented. Nine bases; registry220/implemented119; development155/implemented111/pending44; deferred65 unchanged.

## Sources checked

| Source | Confirmed | Limits |
|---|---|---|
| [PoE2DB Wisdom](https://poe2db.tw/us/Scroll_of_Wisdom), Docker HTTP200 | Identifies an item; instruction targets an unidentified item; currency metadata | No exact retained-field contract, eligible rarity/special-condition matrix, hidden payload or generation timing |
| [PoE2DB currency list](https://poe2db.tw/us/Stackable_Currency), Docker HTTP200 | Matching currency identity/effect | No complete reveal contract |
| [Official API](https://www.pathofexile.com/developer/docs/reference#type-Item), web read | identified boolean, PoE2 unidentifiedTier, optional implicitMods/explicitMods, separate condition fields | No documented hidden payload or identification operation; unidentifiedTier is not an affix tier |
| [Official PoE2 item changes](https://www.pathofexile.com/forum/view-thread/3774647), web read; Docker403 | Dropped Magic/Rare tiers and low-level modifier culling | No Wisdom retention rules; ordinary crafting pools do not establish dropped-item distribution. Docker403 is an access result, not page absence |
| [PoE2 Wiki Wisdom](https://www.poe2wiki.net/wiki/Scroll_of_Wisdom), search excerpt; direct open failed | Secondary description says identification reveals modifiers | Not authoritative proof of all retained fields/eligibility |

PoE1 user/forum/wiki claims were not transferred to PoE2. Search results were frequently irrelevant; only the listed pages contribute findings. No account API, credentials, private item, access bypass or new dependency was used. This investigation does not prove suitable evidence cannot exist elsewhere.

The use instruction does not justify drawing new affixes from ordinary crafting pools. Official drop-tier/culling information additionally prevents assuming those pools model unidentified drops. This checkpoint does not assert when the game internally generates modifiers.

## Existing state and executed check

ItemState has concrete modifiers and UNIDENTIFIED, but no hidden payload/provenance. ItemStateValidator rejects special conditions; SolarTextMapper rejects flags. WorkbenchController rejects unknown root fields before conversion. workbenchApi.ts/workbenchHistory.ts require empty conditions. Relaxing one guard would not deliver a lossless reveal/film path.

Sequential project Docker API probe: nine initial endpoints returned200; synthetic conditions=[UNIDENTIFIED] returned422 INVALID_CRAFTING_REQUEST on actions and apply for each base: **18 refusal probes passed**. This proves the existing refusal boundary only, not Wisdom or retained attributes. Generic Problem Details still names Solar for other bases; this existing wording is outside the checkpoint.

Public HTML and synthetic responses: codex/qa-20261003/wisdom/wisdom.html, currency.html, official-item-changes.html (403 challenge), api-boundaries.json. No app rebuild/restart, data/volume mutation or private text capture. Owned --rm containers exited normally.

## Reversible recommendation — not applied engine code

Distinguish unknown contents from known contents hidden from display. An unidentified pasted item with absent modifiers remains unknown, never an empty explicit list. Only a validated source-bound concrete payload can support deterministic reveal; an opaque ID without a resolver cannot.

After source/input gates are met, use a Workbench visibility envelope around validated ItemState, recording provenance, snapshot/base identity and known/unknown payload. Avoid broadening Support/Explorer or shared special-condition validation. Wisdom requires unidentified + known payload + source-proven eligibility, changes visibility only, preserves exact instances/values/Fractures/implicits/socket knowledge, and emits a reveal event without random draw/probability ledger. Unknown payload, repeat use and unverified condition combinations refuse atomically without consumption.

Every film frame must preserve the envelope; past-frame crafting branches without rewriting the future, reload restores visibility. Old validated films with no envelope retain identified semantics; raw unidentified parser items must never be migrated to identified. Update history compatibility only alongside the implemented action, preserving unsupported archives. A hide-known-item demonstration alone would not establish actual unidentified input or implement Wisdom, so none was added. Rollback is documentation-only; no engine/schema/UI/archive/status changed.

## Exact resume gates and planned checks

1. PoE2 authoritative rules or reproducible same-item before/after evidence for reveal and exact modifier/implicit/value/rarity/level/socket retention, plus unidentified target eligibility. Corrupted/mirrored/sanctified combinations need separate evidence.
2. A lawful concrete input route for a pre-existing hidden payload on these nine bases, with stable item identity/catalog/source-unit matching. Current unidentified clipboard text supplies no payload; this task does not authorise account integration.
3. If simulated unidentified drops are desired instead, complete drop-tier/pool/count/value-generation evidence is required. That generation is separate from Wisdom and cannot be solved by a UX decision or uniform probability.

After gates: unit exact-preservation/unknown/repeat-refusal tests; API forged or mismatched envelopes/nine-base dispatch; representative browser reveal/branch/reload/non-consumption/keyboard checks. **Not executed:** these feature checks because no justified action/schema exists; full BE/FE/browser regressions because executable code is unchanged. Historical BE348/FE320/browser993/cachepersist remain historical, not new passes.
