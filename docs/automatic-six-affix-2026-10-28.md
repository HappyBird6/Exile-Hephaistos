# Six explicit modifiers in automatic planning

The automatic policy is `approved-187-2026-10-28-six-affix-v5`; the sealed resource bundle is `reviewed-equipment-20261028-v3`. The maximum explored explicit count is six. The 187 policy decisions, candidate currency eligibility, modifier weights, item catalogs, numeric models, and resource budgets are unchanged.

Starting items still pass ItemStateValidator. Six is an exploration ceiling, not permission to exceed a base's rarity-specific prefix/suffix capacity, duplicate a modifier family, or use an ineligible modifier. A catalog-proven minimum above six rejects before job creation. The proof remains conservative for compound modifiers, duplicate conditions, and alternative/count groups.

The existing kernel applies its probability accounting to the expanded state range. Four-to-five and five-to-six transitions retain exact total mass. Full items cannot receive a seventh explicit through Exalted. Impossible four-suffix targets return no successful route. Resource-limited calculations remain partial; limits have not been raised to hide growth.

Scenario version 1 can import/export five and six explicit modifiers, including joint members and local conditional laws. Seven-explicit root and graph items are rejected. Starting-item errors now explain rarity, prefix/suffix capacity, duplicate families and the six-explicit ceiling in all six UI languages.

Existing v12 graphs under the four-explicit rarity-v4 policy are read-only. Their old goal catalog is reconstructed using unchanged definitions and the prior sealed bundle identity. Saved probabilities and original requests are preserved; advance/resume/cancel require recalculation rather than interpreting an old cursor under the expanded policy. Earlier supported read-only versions remain covered.

## Initial focused measurements

Docker, two CPU limit, Solar Amulet level 82, ordinary production currency policy. Goals are minimum 1 life/mana/energy shield and chaos/cold/fire resistance, with a legal three-prefix/three-suffix split. These fixed cases are not a worst-case performance guarantee.

| Case | Time | Computation states | Methods | Retained records | Result |
| --- | ---: | ---: | ---: | ---: | --- |
| Four-affix start, five goals | 154 ms | 1 | 1 | 6 | Complete, goal reached |
| Four-affix start, six goals | 225 ms | 3 | 3 | 36 | Complete, goal reached |
| Existing Magic life start, four goals | 304 ms | 7 | 10 | 83 | Complete, goal reached |
| Four independent suffix goals | — | 15 | 14 | 105 | Complete, no goal |

Default limits remain 64 computation states, 128 methods, 20000 retained records, 5000 primitive outcomes. Broader whole-suite and UI verification results are recorded with the implementation delivery.

The running 8081 service is not replaced by this implementation task.

## Verification and measured limits

- Full frontend run: 119 files, 2358 tests passed; an additional five-local-affix joint-law round trip passed in the 30-test scenario suite. Lint, typecheck, formatting and production build passed.
- Docker integration: all 8 PostgreSQL/Redis/application tests passed, with jOOQ generation, Spotless and bootJar passing. The independent source contract verifier passed 17 positive and 6 negative cases. Windows development-script tests passed all 6 cases; Compose config passed without exposing environment values.
- Six goals from a Magic life-only start: complete, goal reached, 31 computation states / 77 methods / 532 retained records, 4726 ms with one CPU during verification. No budget increase or probability pruning was used.
- Six-goal cancellation/resumption and reopening preserve nodes and method probability laws. Maximum measured ADVANCE call in this small case was 347 ms; the time slice is cooperative, not a hard per-step deadline.
- Browser QA used separate localhost port 18081 and a read-only scenario exported from the isolated API result. It displayed four, five and six explicit modifiers, both branches and their merged six-affix target. Hovering one five-affix branch highlighted that branch, its edge/currency label and the six-affix target, without the sibling or ancestor. Clearing hover, selecting the target and dragging the viewport preserved normal state. No live search job was created by browser QA.

The full backend unit run executed 908 tests. Its initial failures included 25 current source-fixture identity mismatches and one obsolete fifth-affix boundary assertion, corrected for the new contract and rechecked separately. The remaining `IndependentPlannerReviewTest.completedProductScenarios / 06-tier-preparation` fails its existing 120000-step completion assertion. A separate baseline run from `d6a2efaea6cfd6a18faacc6444cb0e10eba17edd`, filtered only to that same scenario, reproduced the failure. Both versions retained 1806 records, 30 nodes, 29 goal states, 2 methods and 1 computation state at 120000 operations; new/baseline measured times were 305839/322658 ms. This is a pre-existing completion/performance limitation, not a successful full `check` result. No step guard, probability model or computational budget was changed to suppress it.

Final affected-area reruns passed all 39 tests (12 contract/restart tests plus 27 service/kernel/six-affix tests), including rejection of four prefixes in a Rare amulet. No production container was replaced.
