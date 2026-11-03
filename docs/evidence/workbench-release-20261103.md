# Workbench completion and publication — 2026-11-03

## Integration

- Preserved previous release: `8524b6b`; A checkpoint: `a031403`.
- C source `c57f6220f1ef870444ff3a37c925c7d5c7dd4e8f` integrated as `0286e6ff`.
- Original request JSON preservation: `eb37d631`.
- Confirmed accessory additions and temporary examples: `1e1ac7b3`.
- Original category tabs alone control the right content. Favorites remain below the whole surface in two rows of five.
- Rare-only page fallback now uses `baseStartingRarity`; initialized API rarity, clipboard/history acceptance and class-specific affix caps remain policy-controlled.
- Ring exclusions/additions, five Rare-start amulets, three Rare-start belts, implicit-specific body-armour representatives and card overflow corrections are retained from C.

## Historical compatibility

`AutomaticSearchService.original` exports an immutable copy of the stored request JSON,
while execution retains the existing five typed request fields and equality semantics.
The newer nullable ItemState fields are no longer injected into historical request exports.
Unknown client request fields remain rejected. Saved files, goal ASTs, modifier values,
field order and missing-versus-null distinctions are preserved; fixtures were not weakened.
The existing eight v8/v10 regression cases, fourteen automatic-service cases and three new
original-document cases passed in the focused run.

## Seventeen audited candidates

Six are supported: Biostatic, Oneiric, Dusk, Gloam, Penumbra and Tenebrous Ring.
GGG's [Dawn of the Hunt item-filter announcement](https://www.pathofexile.com/forum/view-thread/3740346)
and [Return of the Ancients item-filter announcement](https://www.pathofexile.com/forum/view-thread/3933291)
confirm the names. Captured six-locale game metadata supplies exact IDs and implicit stats;
ordinary explicit pools reuse the existing reviewed ring template without new weights.
The registry now has 162 top entries, 179 identities including legacy and 171 new choices.
This is simulator support, not a verified claim about live drop rarity or acquisition rates.

The remaining eleven are explicitly unresolved, rather than omitted from the audit:

| Candidates | Exact remaining boundary |
|---|---|
| Golden Hoop, Golden Obi, generic Ring, Abyssal Signet | Non-Unique acquisition/provenance proof is absent. Engine rarity-enable flags alone do not establish ordinary availability. |
| Veridical Chain, Runemastered Veridical Chain, Runemastered Heavy Belt | Runic Ward/Alloy variants and ordinary crafting provenance lack a reviewed end-to-end model; a name or rarity flag is insufficient. |
| Grasping Ring, Stalking Belt | Socketed-item bonus transfer and its interaction with the existing augment/quality model are not implemented or validated. |
| Breach Ring, Refined Breach Ring | Implicit +20/+25 maximum-quality facts are captured, but the current reviewed cap/retained-quality model lacks the base-bonus and interaction boundary. Do not silently use the ordinary base cap or infer combinations. |

Exact IDs, source captures and effects remain in [audit.json](base-audit-20261102/audit.json).
The original 454 nested catalog files are byte-identical. All 45 appended ordinary pools
match reviewed templates, all frontend mirrors agree and all 603 sealed resource digests match.

## Temporary amulet options

Lament/Portent/Absent retain one source-listed granted-skill example each. Generated amulet
cards and previews additionally show the explicitly labelled user-policy passive example
Heavy Ammunition. It is presentation only: no real tree node is allocated, no enchant/modifier
is inserted into ItemState, no liquid emotion is consumed and no passive numerical effect
or chance is inferred. Imported source text is unchanged. The named allocation has appeared
in a first-hand [amulet item listing](https://www.pathofexile.com/forum/view-thread/3637277);
that does not establish a current game balance rule or a property of these bases.

## Images

The C commit and the current local manifest still have placeholders for Distorted Amulet,
Flowing Raiment and Falconer's Jacket. Their previously denied image URLs were not retried.
The six additionally confirmed rings each had one new image-download attempt; HTTP errors
left accessible placeholders. No alternate endpoint or browser route was used to bypass a denial.
The supplied Fire Quiver and Abyss rune artwork remain connected.

## Verification boundary

Final frontend whole run: **2,483/2,483 pass**, one worker; focused integration 49/49.
Lint, strict typecheck, format and production build pass.

Backend full unit/architecture run: **1,018 tests, 15 failures**. Compared with the prior
40 failures, 28 are resolved and 12 remain with identical failure messages. The other three
were stale additive-catalog expectations; these were corrected without weakening the
frozen original registry digest or legacy request fixtures. The final expanded impact run
is **72/72 pass**, including all three corrected tests, the eight actual v8/v10 compatibility
cases, fourteen automatic-service cases and three raw-document cases. Java format and
`bootJar` pass. The full run was not repeated after these test-only changes; it is not
reported as a green whole backend suite. Exact identities and comparisons are in
[validation JSON](workbench-release-20261103-validation.json).

DB integration and `generateJooq` are not executed for this final revision. The earlier
container-to-host Docker socket mount was rejected by automatic approval review and did
not run. It was not retried, replaced with another daemon transport or bypassed. The approved
Temurin build containers expose no host Docker socket, app port, actual DB/Redis or user volume.
No new database-backed verification route was introduced. Browser automation remains deferred.

## Publication and server

Confirmed remote: `https://github.com/HappyBird6/Exile-Hephaistos.git`.
Fetched master: `abfaffb504ab791e20882afda245bfc523a5128b`, an ancestor of integration.
Other worktree changes are preserved. Ordinary fast-forward push only; no protection bypass.
Normal fast-forward push completed and `ls-remote` confirmed
`c5dcb7cb282f3e0369a0e3d7873d80717f0a7729`. At 00:24 UTC, combined statuses,
check runs and Actions runs for that SHA were all empty; no CI pass is claimed.
This repository has no checked-in GitHub Actions workflow.

The final JAR is 94,263,575 bytes, SHA-256
`11ff4fc1a6c7215e7f832e54aec9807eb36173f4acad3d1138ad02b7a0d8b7d8`.
Its sealed resources match current source and all three existing migration SQL files
are byte-identical to the previous server JAR. Frontend production assets are built.
Cold backups contain the previous JAR/static assets, PostgreSQL data and all 54 saved
app files with recorded per-file hashes. Actual DB/Redis volumes and configuration
were not changed or recreated; no saved record was used for QA.

**Server application/start is blocked and not executed.** Automatic approval rejected
`docker ps --format "{{.Names}} {{.Status}}"`, saying even this read-only host command
connects to the Docker socket and falls under the user's earlier socket retry restriction.
The command did not execute. No alternate daemon route or server-start workaround was
attempted. The already approved no-socket build process completed independently.
The prior server containers had been stopped when initially inspected; a subsequent
sandbox HTTP probe could not determine current health because network access was denied.
Host Docker authorization clarification is pending. Expected local URL remains
`http://localhost:9001`; it is **not reported as running**.

See [artifact evidence](workbench-release-20261103-artifact.json). The server JAR, served
asset hashes, live health/API and post-start saved-file comparison remain unverified
until the explicit host-command authorization boundary is resolved.
