# English item clipboard parser

## Scope and source

`backend/src/main/java/com/poe2craft/item/testparser/` contains the service, immutable parser DTOs, REST controller and error advice. These are parser-specific contracts, not catalog ItemState. This flat package was explicitly requested on 2026-09-23.

The display parsing approach is adapted from Path of Building Community PoE2 `ItemClass:ParseRaw` at commit `ce566eac45ea8a86477f513c7ee65a1ebe60014e`:

- https://github.com/PathOfBuildingCommunity/PathOfBuilding-PoE2/blob/ce566eac45ea8a86477f513c7ee65a1ebe60014e/src/Classes/Item.lua
- SHA-256 of retrieved Item.lua: `0ca39961256eefc960da7b30b4e5cb6eb45d08037aa72bc751201cc0630af2a4`.
- MIT copyright and permission notice: [PathOfBuilding-LICENSE.txt](third-party/PathOfBuilding-LICENSE.txt).
- Actual advanced clipboard regression fixture: https://github.com/PathOfBuildingCommunity/PathOfBuilding-PoE2/issues/2117.
- `backend/src/test/resources/item-text/pob-issue-2117.txt` SHA-256: `a8f674d8b3b2f4641eb6d9ee6eb5f58bcb8b18ca7e1c465e30b62009a698e457`. Retrieved 2026-09-23, original report's game patch/snapshot unspecified. It verifies text grammar only, never current item rules or production catalog facts.

Other tests use synthetic names/stats and do not assert game rules.

## Supported text

English only. Rarity and an item name are required; Item Class is optional as in PoB. Missing class stays empty with `MISSING_ITEM_CLASS` and is displayed as Unknown. No Korean translation or Korean-header parser is active.

The parser retains original UTF-8 text (16 KiB maximum), numbered lines, blank lines and sections. BOM and CRLF are handled. It extracts rarity, display names, optional item level, known raw properties including `Quality (Caster Modifiers)`, requirements with/without a colon and multiline requirements, exact flags, and modifier display records.

Trailing `(implicit)`, `(enchant)`, `(rune)`, `(desecrated)`, `(fractured)`, `(crafted)` and `(mutated)` markers classify modifier text. Advanced `{ ... Modifier ... }` and Enhancement headers apply to following lines until another header, a separator, a flag or malformed brace metadata. Explicit prefix/suffix names and `Tier: n` are exposed when actually present; tags and roll annotations remain in original metadata/text. Multiple lines can share one metadata block. Metadata is retained in evidence even if orphaned. The first eligible post-level modifier section supplies plain explicit candidates; later flavour sections remain unresolved. Unknown `Grants Skill:` lines remain unresolved because PoB needs a base catalog to identify implicit skills. Reminder text is preserved rather than discarded.

`modifiers` is the structured display contract. `markedModifiers` remains as original trailing-marker evidence for compatibility. Frontend uses the structured kind/affix/tier, never independently infers them from sentences. Raw values and roll annotations are never scaled, normalized to newer rolls or silently replaced with default quality. Sale price lines are excluded from the card only; source text is untouched.

## Deliberate differences from full PoB

This is not a full Lua parser clone or a semantic modifier calculator. PoB additionally uses itemBases, affix catalogs, ModParser and numeric calculation code. This project has no validated catalog yet, so it does not resolve magic-name bases, affix IDs, implicit skills, modifier effects, weights, requirement recalculation, variants or wiki/build formats. Normal names are display bases only (including an unverified Superior prefix), not resolved base IDs. Unknown lines remain visible with warnings. All results include `CATALOG_VALIDATION_REQUIRED` and cannot be used directly for crafting calculations.

TODO(domain): Verified versioned base/affix catalog and mapping evidence required / pinned source and game snapshot / semantic modifier parsing and ItemState conversion / currently unavailable.

## HTTP and UI

`POST /api/v1/items/parse` accepts `{ "text": "..." }`. OpenAPI: [openapi-item.yaml](openapi-item.yaml). Anonymous stateless read-only request, exact-route CSRF exemption, no input persistence. Errors: 400 malformed JSON, 413 size limit, 422 invalid English input; Problem Details preserve code/traceId without raw internals.

ItemCard is stateless. Replacing its props updates rarity/name/values/modifiers together; a future verified simulation outcome uses the same display contract. No crafting engine or fake outcome was added. Query mutations retain cancellation/revision protection, input stays in Zustand, and parsed response data is not duplicated there.
