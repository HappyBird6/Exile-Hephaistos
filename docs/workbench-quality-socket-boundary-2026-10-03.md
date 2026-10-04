> Historical guard checkpoint. The ordinary Artificer gap is subsequently resolved by [the Stocky delivery](workbench-stocky-artificer-2026-10-03.md); exceptional/resource states and Scrap remain unresolved.

# Quality/socket source gaps and lossless Workbench boundary (2026-10-03)

## Source decision

Timeboxed follow-up to the quality review did not establish an ordinary Armourer's Scrap per-use increment. The [current item card](https://poe2db.tw/us/Armourers_Scrap), Quality/Maximum Quality glossary and official 0.3.0/0.3.1 patch text establish an armour quality effect and ordinary maximum20, but not the increment or its rarity/item-level dependence. Search hits supplying 5/2/1 were legacy PoE1 (2013/2015), rejected. PoE2 wiki access failed; no fallback guess.

The [current full Artificer's Orb item card](https://poe2db.tw/us/Artificers_Orb) adds one Augment Socket to Martial Weapons, wands, staves or Armour. Its shorter metadata description omits wands/staves, so the full card takes priority. Stocky Mitts is Armour; Solar Amulet is not an eligible class in this text. This confirms the operation and class boundary, not all use conditions.

[Stocky Mitts](https://poe2db.tw/us/Stocky_Mitts) exposes `Quality.max_quality=20` and `Sockets.socket_info=1:5:100`. The socket tuple's semantics are unverified: neither maximum1 nor minimum item level5 is inferred. Official 0.3.0 exceptional drop socket/quality changes distinguish dropped exceptional states from currency limits, but do not establish this base's complete current Artificer use cap. [Augment](https://poe2db.tw/us/Augment) distinguishes replacement from removal and Socket-bound permanence. Full cap, special-state eligibility and old-state migration remain unresolved.

Scrap and Artificer stay unimplemented. No new material, inferred probability, default-zero quality or default-empty socket state is claimed. Registry220/implemented74, Liquid27 deferred and non-Liquid pending119 remain unchanged. Vaal/Hinekora exclusions persist. No combat calculator.

## Reproduced data loss and repair

Before this change, an actual Docker API request carrying `quality:20`, `socketCount:1` or `sockets:[...]` returned200/applied and silently dropped the extra state. This was independently reproducible for Stocky on the previous checkpoint runtime.

Both `/workbench/apply` and `/workbench/actions` now reject unsupported root or modifier-instance properties before converting JSON into domain state. HTTP422 uses `UNSUPPORTED_ITEM_PROPERTIES`. The legacy derived `modifierIds` field remains accepted; authoritative state/modifier fields keep their existing contract.

Frontend request, response, mapped-state and history validation reject unsupported properties. The repository still retains raw stored frames: an unsupported saved frame cannot be crafted or projected into an affix-only state, and starting a supported new film preserves the archived frame. There is no destructive migration or invented quality/socket default. Existing supported histories remain valid.

## Evidence and validation

Source metadata/hashes: [capture manifest](evidence/quality-socket-source-review-2026-10-03.json). Full raw captures and before/after probes remain under `codex/qa-20261003`, outside tracked source.

Final checks and actual runtime/browser evidence are recorded below after execution. Gameplay pools, weights, catalog identities, rule versions and implementation counts are unchanged. Unchanged full material/browser matrices and cache-persistence suite are not rerun by this boundary change.
Executed in project Docker: backend `spotlessApply check generateJooq bootJar`, 249 unit +6 integration tests (255, zero failures/errors); frontend lint/typecheck/format,178 tests across31 files and production build. Targeted checks first passed18 backend/37 frontend cases. Two early harness issues were corrected (new test initially expected400 instead of existing422 policy; initial API probe omitted concrete `explicits`); no rule was loosened to pass them. A browser strict locator also distinguished the visible feedback from its accessibility status mirror.

Fresh QA API22 assertions: both bases, five unsupported root-property samples, both apply/actions reject422 with the specific code; ordinary Transmutation still applies and returns cap20. Fresh actual Chromium11 assertions: unsupported saved frame refusal; byte-exact desktop/390px reload; unchanged archive after a new supported craft; injected unsupported result refusal, unchanged saved films/selection and successful retry; previous/next/reload; zero page errors. The390px screenshot was visually inspected: warning readable and central item visible. No new claim about untested widths.

Fresh scope probe confirms registry220/implemented74, Solar60 support records/49 actions/218 definitions and Stocky67/56/194;182 Stocky positive definitions and148200 total normal weight. Immutable runtime JAR SHA-256 `640dae895e4905e038555458760f32187c5173a75b8bdb7ba8eb4b728f87fd80`; served frontend bundle `index-DTZUnqDa.js`, image `4ead51edcf8dda25821204ac9df93a72d3077832b56c4b960d60fe122e3b905a`.

QA artifacts: `quality-socket-unmodeled-state-before.json`, `quality-socket-boundary-api-results.json`, `quality-socket-boundary-browser-results.json`, `quality-socket-boundary-narrow.png`, and source captures under `codex/qa-20261003`. Previous full browser30/material matrices/cache-persistence suite were not rerun; full backend integration and frontend regression were rerun. Existing DB/volumes preserved; no login, migration, remote push/merge/deploy.