# Stocky Mitts complete ordinary catalog — 2026-10-03

Status: bundled preparation only; not selected by the default loader or exposed as a supported Workbench base.

Current [PoE2DB strength gloves](https://poe2db.tw/us/Gloves_str) still contains 182 ordinary rows: 83 prefixes / 99 suffixes, published weights 63700 / 84500. Fresh comparison found no row changes from the previous capture. No rows were removed and no weights were renormalised. POE2DB_AS_PUBLISHED denotes published selection weights, not an independently verified game extraction.

The missing six details were located using public PathOfBuilding-PoE2 ModItem.lua at pinned commit bb52d6b368307457eb9c54bb13f1829993d390b1. That derivative file was a Code locator only. Adopted numeric evidence comes from PoE2DB's public detail endpoint, matched against current row name, level, affix generation, family and rendered effect. Similar belt and HandWraps variants were rejected.

| Current row | Exact Code | Source stat and bounds |
|---|---|---|
| Encased, level 54, prefix | LocalIncreasedPhysicalDamageReductionRating7__ | local base physical damage reduction rating 160–190, Local |
| of the Worthy, 24, suffix | ReducedLocalAttributeRequirements1 | local attribute requirements +% -15, Local |
| of the Apt, 32, suffix | ReducedLocalAttributeRequirements2 | same stat -20 |
| of the Talented, 40, suffix | ReducedLocalAttributeRequirements3 | same stat -25 |
| of the Skilled, 52, suffix | ReducedLocalAttributeRequirements4 | same stat -30 |
| of the Proficient, 60, suffix | ReducedLocalAttributeRequirements5 | same stat -35 |

Detail URL form: https://poe2db.tw/us/hover?s=Data%5CMods%2FReducedLocalAttributeRequirements1 (replace the Code for each row). Complete proof: codex/qa-20261003/gloves-public-code-proof-missing-six.json and gloves-confirmed-missing-six-comparison.json. Current pool comparison: gloves-current-pool-comparison.json. Pinned public source and manifest remain in that QA directory.

Bundled catalog contains all 182 definitions, with 116 freshly matched details and 66 exact-row matched retained details. Retained captures are labelled retained, not newly fetched. details.raw.json preserves original HTML, locality and provenance; catalog stat IDs normalise the captured source labels with underscores. Catalog metadata hashes the exact raw and detail files. There are 42 multi-stat definitions (34 both variable, eight one variable) and no implicit. Base Armour 15 is recorded separately as a base property; no final computed Armour total is claimed.

Numeric boundaries alone do not prove interior roll increments or display rounding. roll-model-review.json records the inactive status, all multi-stat IDs, approved conjectural shared-ratio model, and unresolved single-stat precision review. In particular, permyriad source units must not silently become an asserted 91-value or ten-value game domain. Full runtime activation, action dispatch, numeric display and browser regression remain WB-003/WB-006 work. User approval of 10001 ticks/HALF_UP is a preference review, not game verification.

Validation: StockyMittsCatalogTest checks complete row/weight/family/affix coverage, recovered signed bounds, checksum rejection, 42 multi-stat definitions and unchanged default Solar count. Full project Docker backend results are recorded after execution in ISSUES.md. FE/runtime/browser behavior has not changed in this catalog-only stage; prior checks remain prior.
