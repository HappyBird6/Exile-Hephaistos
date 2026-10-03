# Ordinary Stocky Artificer delivery (2026-10-03)

## Implemented user path

Choose Stocky Mitts under Edit item and place the base. A fresh ordinary base explicitly starts at `augmentSockets:0`. Artificer's Orb is visible in Currency and adds exactly one empty Augment Socket; the property card displays0/1 then1/1. Holding Shift retains the selected material. Using it again returns a safe refusal without state/history or omen changes. Solar remains ineligible.

This implements one registered material, not the whole socket/resource family. Stocky Normal/Magic/Rare states with explicitly known zero sockets are supported; fully validated special affixes and Fractures remain intact. Unique, Corrupted, Mirrored, Unidentified, Sanctified, exceptional counts, socketed Augments, Augment effects and Extraction are unsupported. Those are implementation boundaries, not blanket claims that every listed state is illegal in the game. Other armour/weapon types require another complete base/catalog; that is distinct from unknown game rules.

## Source evidence and conditions

| Fact / scope | Evidence | Decision |
|---|---|---|
| Adds one socket; armour/wand/staff/Martial Weapon classes | Current full [Artificer card](https://poe2db.tw/us/Artificers_Orb), captured primary HTML/hash | One deterministic state transition; narrower short metadata does not override full card |
| Stocky is armour/gloves | [Stocky Mitts](https://poe2db.tw/us/Stocky_Mitts), captured tags/class | Supported base; Solar Jewellery cannot inherit the rule |
| Ordinary gloves maximum1; Jewellery cannot receive Artificer sockets | Secondary PoE2 [Augment socket](https://www.poe2wiki.net/wiki/Item_socket) and [Crafting](https://www.poe2wiki.net/wiki/Crafting), independently read via search-index reader |0→1 allowed; already1 refused. Direct secondary HTTP access403, not claimed as a raw capture |
| Exceptional/corruption sockets may exceed ordinary limits | Official [0.3.0](https://www.pathofexile.com/forum/view-thread/3826682), plus socket article | Do not clamp exceptional data to1 or infer empty sockets on a past item |
| Subsequent major patch conflict check | Official [0.5.0](https://www.pathofexile.com/forum/view-thread/3932540), no Artificer change in checked text | No conflicting Artificer restriction found; not a claim to have read every minor patch |
| Metadata tuple1:5:100 and currency DropLevel5 | Raw PoE2DB metadata | Tuple semantics still unverified; neither value5 becomes a minimum item-level requirement |
| Ordinary corrupted/mirrored modification restriction | Secondary Crafting plus existing special-state validator | Safe refusal; no special-state crafting engine introduced |
| Multi-omen interaction | Existing sourced omens trigger other operations, none Artificer | Unrelated active omens stay unconsumed; no random roll, eligible pool or probability assumption |

The operation has a single confirmed result (one empty socket), probability1. There is no arbitrary1/N assumption or modifier-weight change. Source metadata/hashes are retained in [the earlier capture manifest](evidence/quality-socket-source-review-2026-10-03.json); this follow-up fills the ordinary-limit gap through the independently read PoE2 secondary pages. It does not give a meaning to the socket tuple or finish exceptional/socketed-resource rules. Armourer's Scrap remains unsupported: current secondary evidence says quality increments depend on item level, but the complete formula remains absent; legacy rarity5/2/1 is rejected.

## State and compatibility

`ItemState.augmentSockets` is nullable. Missing/null means unknown, not zero; the seven-field legacy constructor retains that distinction. Known0/1 is valid only for reviewed Stocky. HTTP input requires an actual integral count, rejecting fractional/string/overflow coercion, negative and exceptional counts; unsupported quality/socket-resource properties are still rejected before conversion.

Fresh Workbench initial metadata supplies zero separately from the six-action Support/Explorer bucket projection. Only Workbench uses/copies the concrete socket state; all existing Workbench craft/copy/refusal paths retain it. Support/Explorer's affix projection is unchanged. Stocky rule version is `stocky-workbench-artificer-v26`; ledger/catalog/snapshot identities and normal weight pools stay unchanged.

Old films without the field remain valid and show `Augment Sockets: unknown`. Artificer refuses, while existing ordinary crafting remains available. No history migration invents zero. Socket crafting from a past zero frame creates a new linear film, preserves the old future, and survives reload/localStorage persistence. Stored exceptional/resource states stay preserved but unsupported; no drop/sacrifice/destruction is simulated.

## Scope and validation

Registry220/overall implemented75 = Currency20 + Essence36 + Alloy8 + Omen11. Solar60 support records/49 actions and Stocky68/57. Stocky194 definitions,182 positive ordinary definitions and148200 normal weight unchanged; no catalog/source resource was modified. New user deferrals are tracked independently: current-development inventory155 / implemented67 / unimplemented88, plus65 exclusions (eight already implemented Alloys retained). [Exact Korean/ID scope mapping](workbench-development-deferrals-2026-10-03.md).

Final executed checks, QA runtime identifiers and browser findings follow after the final reviewed build. Original DB/volumes and original master are preserved; only local checkpoint commit, no remote push/merge/deploy.
## Executed verification

Project Docker full backend `spotlessApply check generateJooq bootJar`:259 unit +6 integration tests =265, zero failures/errors. Full frontend lint/typecheck/format +185 tests across32 files +production build passed. After visual-review copy/placement corrections, targeted40 UI/state cases, typecheck and build were rerun; final actual browser exercises the corrected served bundle. Full frontend suite was not redundantly repeated after those narrow display-only corrections.

Fresh runtime API42 assertions covers ilvl1/4/5/82/100, deterministic0→1, repeat, ordinary/legacy/Solar, numeric coercion/exceptional/special-state refusal, unrelated omen preservation, Alchemy/Fracturing/Divine/Chaos/Annul/Horror socket/lock preservation and implementation/action counts. Guard regression: API22 and actual browser11 passed with unknown quality/socket-resource data preserved, invalid response refusal and retry; zero page errors. Breach cap regression: two default caps and five source-bound/refused/Fractured transitions pass. The quality inventory17 remains registered pending, including two now-deferred Vaal infusers; current non-Vaal quality candidates15.

Source/scope check verifies65 exact Korean names/hashes, distinct64 direct +one dependency, retained8 Alloys, current155/67/88, and219 unrelated registry entries semantically unchanged. Current principal blockers88 =61 other targets +20 rule/data +2 engine/state +5 reintroduction gaps. Source-bound raw catalog files/weights/snapshots are untouched; no redundant raw-source staging roundtrip is claimed for this batch.

API/build/guard results and source captures live in `codex/qa-20261003`: `artificer-api-results.json`, `artificer-runtime-scope-validation.json`, `artificer-scope-proof-validation.json`, `artificer-quality-cap-regression.json`, `quality-socket-boundary-*-results.json`, plus final `artificer-browser-results.json` and desktop/narrow screenshots. Full unchanged legacy browser30, individual material matrices and cache-persistence suite were not rerun; required backend integration and complete frontend regression were rerun.

Immutable QA JAR SHA-256 `7619da145a0b2076a521ef562a93b204eeb5385a3a1a6a61d8b28d74db8b78bc`. Final frontend bundle/image and final browser assertion count are appended after the placement correction is served and verified.
Final served frontend `index-DRldwlYH.js`, QA image manifest `abcec73ed94e8437ae0bbaebde99e299f4635c4209a390261542c146fd6b1ba9`. Final actual Artificer browser **26** assertions, zero page errors. Desktop geometry verifies no overlap between the new icon, central item and favorites.390px actual use/repeat/Escape works through the inventory's existing horizontal scrolling; property card is readable, document does not overflow. Desktop and390px final screenshots were visually inspected. A first icon position partially overlapped the central card despite successful clicking; it was corrected to source-canvas position574/240 before this final verification. The stale “sockets unsupported” paragraph now correctly limits the unsupported scope to socketed Augment effects.