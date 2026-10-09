# Goal filter v1 implementation evidence

Production IDs use `hephaistos:v1:{explicit|implicit|pseudo}:...`; they are local identifiers.
Only reviewed checked-in ItemCatalog definitions are enumerated. Catalog version hashes the
entire loaded definition inventory and the source-unit rules revision. Snapshot/base mismatches
never fall back to Solar Amulet. Catalog `eligible` describes new generation (level and positive
spawn weight), not validity of an existing higher-level or zero-spawn modifier.

Reviewed direct source scales are 1/1. The resistance sources
`base_{cold|fire|lightning|chaos}_damage_resistance_%` and `base_resist_all_elements_%`
use percent, as shown in the source modifier text. Maximum life/mana/energy shield,
`base_spirit_from_equipment`, `additional_{strength|dexterity|intelligence|all_attributes}`,
and `accuracy_rating` use flat source units, as shown in their source text.
Every catalog row retains modifier source URLs. Other stats use the explicit `source` unit
and UNSUPPORTED / UNIT_OR_EFFECT_NOT_REVIEWED; their text is not guessed into a display unit.

Pseudo cold/fire/lightning totals add the corresponding elemental resistance and all-elemental
resistance; strength/dexterity/intelligence totals add the individual and all-attributes sources.
Both implicit and explicit direct sources contribute once per modifier/stat/layer.
No pseudo source is used as another pseudo's input. An all-absent pseudo stays absent;
present zero remains present; any unresolved source makes the total unresolved.

Quality and unsupported item conditions are UNSUPPORTED rather than guessed numeric values.
Malformed modifiers, duplicate families, invalid ranges and stat-set mismatch are item validation
errors. Existing modifier level is not checked against the new-generation level threshold.

## Numeric probability boundary

The HTTP recommendation returns UNSUPPORTED / NUMERIC_DISTRIBUTION_NOT_IMPLEMENTED with
no comparisons, no certified ranking and null model/ledger versions, even when the starting
item already matches. It does not run the occupied/qualified DP or use Workbench sampled rolls.

ExactNumericDistribution is a finite joint-outcome oracle used in tests. Its state key is the
entire immutable ItemState including actual values. Each supplied transition kernel must have
exact normalized rational mass. A matching state is absorbed before future actions; unknown
and unsupported evaluations retain unresolved mass. Budget truncation also retains that mass.
Hybrid tuples are passed as indivisible outcomes, never decomposed into independent stat rolls.

Production connection requires a reviewed addition-selection kernel plus per-modifier joint
roll distributions, action legality/cost/omen projection, and cache namespace covering
snapshot/base/rules/ledger/AST/catalog/joint-projection versions. The uniform single-stat integer
ledger cannot be extended to hybrid/derived/conditional stats. Existing CoupledStatRollModel
explicitly documents an unverified user conjecture; it is not adopted as a certified model here.
Shared StateBucket, SupportGoals, SupportRecommendations, ItemState, ItemStateValidator,
registry and roll/quality code are untouched.

Integration owner must register GoalFilterService with a GoalCatalogIndex built from
BundledGoalCatalogs.load(workbenchService) in bootstrap/SupportConfiguration.java. GoalFilterController and
its scoped Problem Details advice discover automatically. No legacy controller change is needed.

The loader calls the pure WorkbenchService.initial endpoint once per checked-in base during
bootstrap to obtain its reviewed metadata, definitions and compatibility identities. It reads
only the corresponding bundled base properties and validates their identity; it does not roll,
consume currency, write data or fetch remote sources. Request handling reuses the immutable
index and does not repeat this work. No cross-module item.infrastructure dependency is added.

## Supported evaluation inventory at item level 82

All probability capabilities are UNSUPPORTED. Counts include direct layer rows and pseudo rows.
Eligibility here covers reviewed ordinary addition candidates (positive source spawn weight and
required level), plus base implicits. Special zero-spawn results stay visible/evaluable but do
not count as ordinary addition candidates. This is not an item-specific free-slot/family pool.

| Checked-in base | Catalog rows | Evaluation SUPPORTED |
| --- | ---: | ---: |
| Solar Amulet | 46 | 21 |
| Stocky Mitts | 46 | 14 |
| Iron Ring | 43 | 18 |
| Rusted Greathelm | 24 | 14 |
| Rawhide Belt | 24 | 11 |
| Rusted Cuirass | 25 | 11 |
| Rattling Sceptre | 28 | 5 |
| Attuned Wand | 30 | 3 |
| Crude Bow | 31 | 2 |
| Diamond | 1 | 0 |
| Emerald | 2 | 0 |
| Ruby | 2 | 0 |
| Sapphire | 3 | 0 |
| Time-Lost Diamond | 3 | 0 |
| Time-Lost Emerald | 4 | 0 |
| Time-Lost Ruby | 4 | 0 |
| Time-Lost Sapphire | 4 | 0 |

The real catalog and real Solar ItemState API example are in production-example.json, including
cold resistance 10 plus all elemental resistance 12 yielding pseudo total 22. It uses the full
17-base inventory catalog version and is checked against runtime catalog/evaluation in tests.
Fixture IDs and observedStats remain unit-test-only; HTTP rejects observedStats and unknown fields.
HTTP activeOmens uses existing Workbench Omen_of_... IDs, not enum names.

## Remaining implementation and shared-file boundary

1. Keep numeric states as full ItemState or prove a smaller sufficient key including every source
   contribution/presence and remaining omen state. Occupied/qualified alone is insufficient.
2. Reuse read-only AdditionRules.plan / AdditionTransitions.transition published integer weight
   ratios for supported modifier selection. Expand each chosen modifier with a reviewed finite
   joint-roll ledger, retaining existing rolls. Add the numeric kernel in goalfilter code; do not
   turn the sampled WorkbenchSimulator or CoupledStatRollModel conjecture into a certified ledger.
3. Supply reviewed sources for single-stat integer assumptions and each multi-stat joint outcome.
   Missing distribution evidence must produce UNKNOWN; an unimplemented effect remains UNSUPPORTED.
4. Join action/omen legality and first-hit absorption with exact numeric observations. Budget
   truncation may become PARTIAL only after this model is validated against finite enumeration.
5. Add a goalfilter-specific cache including snapshot/base/rules/ledger/AST/catalog/roll-projection
   versions. Existing AdditionPoolCache/AdditionPoolStore/JdbcAdditionPoolStore are family caches;
   sharing them without a distinct projection namespace and payload would lose numeric state.

Shared-file edits are not required by this delivered evaluation module. The only immediate
integration edit is bootstrap/SupportConfiguration.java: inject WorkbenchService into a new
GoalFilterService Bean, construct GoalCatalogIndex(BundledGoalCatalogs.load(workbenchService)),
then construct the service. The controller also injects the existing ObjectMapper and makes a
private strict copy; no global Jackson behavior changes.

Potential later integration review targets (not edited, no amendment authorized by this delivery):
StateBucket.java, AdditionRules.java, AdditionTransitions.java, ModifierPoolResolver.java,
AdditionPoolCache.java, AdditionPoolStore.java, JdbcAdditionPoolStore.java, SupportRecommendations.java,
WorkbenchSimulator.java, CoupledStatRollModel.java, ItemState.java, ItemStateValidator.java and
registry-v2.json. Their current family/minimumTier/N-of-M behavior must remain unchanged.
The new numeric kernel/ledger/projection/cache can start in independent goalfilter files; report
any genuinely needed shared-file modification before implementing it.

No comparison with unavailable unpushed 134-base/registry/i18n/Workbench changes was possible.
No claim of absence of merge conflicts is made. URL import/export, frontend, main merge and
runtime deployment are outside this delivery.

## Validation and self-review, 2026-10-07

Final independent source test command (before any bootstrap change):
`./gradlew --no-daemon spotlessApply test --tests '*goalfilter*' --tests '*ArchitectureTest*'`.
All 25 goalfilter tests plus the existing ArchUnit test passed. The common fixture suite iterates
24 evaluation cases and nine validation mutations within those tests. Actual catalog/base,
source sum/presence, duplicate contribution, unsupported starting state, COUNT bounds, strict
HTTP input, Problem Details and exact joint outcome/first-hit/budget regressions passed.

A separate assembled copy then received only the new GoalFilterService Bean in
bootstrap/SupportConfiguration.java. Its `./gradlew --no-daemon check generateJooq bootJar`
passed with 411 unit/ArchUnit tests and seven PostgreSQL/Redis integration tests; zero failed,
errored or skipped. Spotless and disposable-DB jOOQ generation passed. This is the patched
assembly's result, not a claim that the unregistered worker branch passes full Spring startup.
The integration owner must register the Bean and validate the final combined FE/BE tree again.

Self-review corrected an initial prohibited item.infrastructure dependency by using public
WorkbenchService data, and a new integration test's missing existing db/testmigration path.
Neither ArchUnit nor test expectations were weakened. Final input review also covered disabled
required fields, numeric-string/fractional truncation, valid Workbench omen wire IDs, huge COUNT
minimums and unsupported-state COUNT tautologies. Only scoped new source/test/resource files
are included. Existing controllers, bootstrap, shared registry/roll/quality/state and frontend
files are not modified on this branch.

The existing project check image supplies Java 21/Gradle. Source is mounted read-only and copied
into a dedicated container; Testcontainers uses disposable PostgreSQL/Redis and random ports.
No Compose operations or existing data volumes/server ports were used. Evidence is preserved
outside Git in ../goal-filter-backend-evidence/ (relative to this worktree), including the
independent log, assembled full log, exact bootstrap.patch, JUnit XML and assembled JAR.
Only this worker's check containers are removed after preserving the evidence.
