# C: base audit feedback integration

Baseline: `8524b6b781651379a6cff9108525282a0691df25`. Worktree: `task-5/base-audit`, branch `workbench/base-audit-c`.

## Parent connection

`CraftingPage.tsx` and `crafting.css` are untouched. The parent should import `baseStartingRarity` from `baseRegistry` and replace the base-preview loading fallback's hardcoded `rarity: 'NORMAL'` (baseline line 2178) with `rarity: baseStartingRarity(catalogBase)`. Actual initialized state already comes from the policy-aware initial API. Keep the user's selected base's concrete initial rarity; only jewels expose the existing explicit rarity selector.

Cherry-pick the complete commit. Canonical catalog resources, frontend mirrors, registry capabilities, reveal eligibility, ruleset seal and source fixture provenance must move together. The fixture's goals and numerical oracles are unchanged. Re-run the seal refresh if integrating another branch changes any sealed resources.

## Implemented

- Source-verified six new rings: Lazuli, Sapphire, Topaz, Emerald, Gold, Unset.
- Source-verified thirteen amulets: Jade, Lapis, Gold, Twisted, Distorted, Absent, Portent, Lament, Dusk, Penumbra, Gloam, Corona, Tenebrous.
- User policy excludes Kinetic, Mnemonic, Vitalic and Two-Stone Fire/Cold from new choices only. Existing catalog IDs and saved records remain intact.
- User policy starts five amulets (Dusk, Penumbra, Gloam, Corona, Tenebrous) and Sinew/Invoking/Forking belts at Rare. Initial API, clipboard validation, craft validation, response validation and saved-frame acceptance enforce it. Existing Normal/Magic records are retained without rewriting and cannot be crafted.
- Source implicit affix capacities (including 4/2, 2/4, 5/1, 1/5, 2/3, 3/2, 2/2) flow through backend catalogs and frontend response/history/automatic-craft checks.
- Twenty body-armour representatives preserve distinct implicit-effect groups within defence archetypes. Three Shrouded Mail IDs retain distinct fire/cold/lightning implicits. Ambiguous clipboard names need the exact implicit line.
- Card rows reserve 56px and independent 40px image space; sparse subcategories and variant labels have a JSDOM CSS/DOM regression test. No global ItemCard changes.
- Lament/Portent/Absent use one source-listed granted skill each, explicitly marked as a temporary user-policy example in all six locales. Those skill effects are not inferred by the crafting engine.
- Historical automatic-result reading tries the frozen pre-expansion 134-base inventory as well as current membership. Old files and goal ASTs are never relabeled.

## Audit and limits

See `docs/evidence/base-audit-20261102/BASE_AUDIT.md`, `audit.json` and exact source captures. Fifty roster pages were compared. Thirty-nine identities were added; registry totals are 156 top bases / 173 including legacy / 165 new workbench choices.

Seventeen additional accessory candidates remain pending acquisition or socket/Runic Ward/quality-cap rules. They are explicitly listed in the audit. Do not claim every source roster item is supported. New source-only implicit goal predicates without reviewed definitions stay visibly unsupported; no probability or goal arithmetic was invented.

The requested base pages do not identify an `Allocates` notable. A specific allocated passive still needs an identified rule/source. The three temporary granted skills are implemented; passive allocation is pending.

Four ring exclusions and eight Rare-only restrictions are the user's crafting scope policy. The extracted engine rarity flags contain Normal/Magic/Rare/Unique and do not independently prove live acquisition. Sources identify exact six-locale names/metadata/stats, rather than certifying current GGG drop behavior.

## Images to attach

Chat: **베이스 선택·세션 History 메뉴**. Three image endpoints returned HTTP 403; no retry or bypass was performed. Accessible placeholders remain for:

- 왜곡된 목걸이 / Distorted Amulet — `Metadata/Items/Amulets/FourAmuletDelirium2`
- 흐르는 의복 / Flowing Raiment — `Metadata/Items/Armours/BodyArmours/FourBodyInt6Endgame`
- 매 조련사의 재킷 / Falconer's Jacket — `Metadata/Items/Armours/BodyArmours/FourBodyDexInt6Endgame`

Thirty-two other new image files are cached with source hashes. The prior user-provided Fire Quiver image is preserved.

## Verification boundary

Browser checks and server changes are deferred to the parent/user. No user database or browser storage was touched. Host Java is 8, so Java 21 compilation uses the existing Temurin container without exposing application ports.

Automatic approval review denied the proposed Docker host-socket mount for the full `check generateJooq bootJar` verification because it grants broad control of the host Docker daemon and other containers/volumes. That operation did not execute and was not retried or bypassed. DB integration checks and codegen remain unexecuted. Related no-socket unit checks and builds are recorded separately in `docs/evidence/base-audit-20261102/VERIFICATION.md`.
