# Safe-stop handoff — 2026-10-12

## Stop reason and location

The user requested a safe stop before **17:30 KST today**. New development and
long checks were stopped at 17:15 KST. Do not resume automatically or start an
application server. The earlier server request was cancelled. No deployment,
master merge or force push is authorized.

Host: `DESKTOP-NTUJ7E4`.
Worktree: `C:\SSAFY\PYJ\PROJECT\Exile-Hephaistos\codex\bucket-method-search`.
Original checkout: `C:\SSAFY\PYJ\PROJECT\Exile-Hephaistos\Exile-Hephaistos`.
The original checkout and unrelated Docker services/data/volumes were preserved.

## Remote checkpoints

- Fully checked branch: `crafting/bucket-method-search-v2`.
- Fully checked SHA: `a7a45c4b53d91e5595264d392e9d007b6b16e386`.
- Additional partially checked work: `crafting/bucket-method-search-v2-wip-20261012`.
  The commit containing this document is the WIP checkpoint. It descends from
  the fully checked SHA; its full backend suite has **not** been rerun.

The main work branch contains the v2 method search, exact goal handling,
result-dependent strategy calculation, progressive API/UI, conditional detail
pages, deferred roll/tier cells and exact joint exchangeable-family cells.
Read `docs/crafting-bucket-method-v2.md` for contracts, proofs, scope and history.
The overall requested full production exploration is **not complete**.

## Verified results

At the fully checked SHA, Docker backend `check generateJooq bootJar` passed:
664 unit/contract tests and 7 integration tests. Frontend lint, typecheck,
format check, 2,152 tests and production build passed. The existing large bundle
warning remains. Browser verification was blocked by the previously recorded
launcher error (`HRESULT 0x80070003`); no new server was started.

The independent tiny complete-search oracle compares every method at 100/300/500
cycles: every exclusive exit, full conditional item law, active mass, initial and
cycle consumption, and strategy bounds. Its concrete four-family case has 24
states and 1,477 methods. Solar primitive checks cover all 16 actions, including
a joint two-family source, without-replacement assignment and initial observed
family exclusion. Concrete detail pages restore weighted outcomes exactly once.

The last fully checked isolated Solar probe reached 132,000 operations in
27,946ms. It represented 185,009 concrete items in 794 cells, with 786 unexpanded
states and 260/784 methods complete. Sampled process heap peak was 233,481,976
bytes; VmHWM was 460,344 KiB. First response/method/goal were 132/284/7,024ms.
Both A-to-C and B-to-C routes existed. No first local recommendation or overall
completion was reached; status was `PAUSED / COMPUTE_BUDGET` after ten resumes.

## WIP changes and their limited verification

1. `ExactNumericDistribution.Fraction` is now an immutable class with the same
   constructor/accessors, equality, hash, string and JSON shape. Multiplication
   cross-cancels first; addition reduces only the provably remaining common
   divisor. Powers and complements retain proven reducedness. Public inputs
   still normalize and validate. Renewal calculations use these operations.
2. `MethodSearchService.localRecommendations` certifies each observation count
   once source action discovery and all alternatives at that count are ready.
   It no longer waits for 300/500 observations before certifying 100. Tied best
   methods remain tied; missing methods and competitive unresolved mass block
   certification.

Passed for the arithmetic change: `ExactNumericDistributionTest`, `*Renewal*`,
`*Deferred*` and `OrbitCellTest`, including the full tiny oracle and JSON identity
checks. Passed after the local recommendation change: its two focused tests,
compilation and Spotless formatting. No full backend/integration suite was run
on the combined WIP because of the stop deadline. Frontend was not changed in
this WIP.

An isolated arithmetic-only probe performed the identical 132,000 operations
with identical state/method/support counts in 16,445ms (previous 27,946ms).
First response/method/goal were 131/284/3,830ms. Sampled heap peak was 239,167,000
bytes, VmHWM 465,140 KiB. This measurement predates the local recommendation
change. It still did not reach first local recommendation or full completion.

## Remaining issues / recommended next work

- Run the full backend suite on WIP before promoting it to the checked branch.
- Measure the observation-specific local recommendation change in the actual
  Solar job. No new extended probe was run; the local diagnostic driver was
  restored to its prior ten-resume/300MB guard version.
- Real Solar search still has a large unexpanded frontier. Do not label it
  complete, discard alternatives, or silently turn a server work budget into a
  global crafting currency cap. Continue exact state reduction/caching and
  scheduling work with full conditional-law oracle checks.
- Total retained runner/revision memory is not yet comprehensively budgeted;
  the primitive-outcome budget alone is not a total process-memory guarantee.
- Unsupported joint-roll/omen rules remain explicit unresolved support, not
  probability zero. Current reduced-state proof is scoped to reviewed Solar
  basic actions with the documented gates.
- Browser launcher failure remains; do not start a server without renewed user
  authorization. UI tests are not a claim of actual browser verification.

## Exact resume entry point (only after user asks to resume)

Use PowerShell on the same computer:

```powershell
Set-Location 'C:\SSAFY\PYJ\PROJECT\Exile-Hephaistos\codex\bucket-method-search'
$env:COMPUTERNAME
git status --short
git branch --show-current
git fetch origin
git log -1 --format='%H %s'
git ls-remote origin refs/heads/crafting/bucket-method-search-v2 refs/heads/crafting/bucket-method-search-v2-wip-20261012
```

Read the applicable checkout instructions before edits. The worktree is left on
the WIP branch with a clean Git status. Do not reset the original checkout or
force-update the checked branch. To verify the WIP in a frozen Docker copy:

```powershell
docker run --rm --name bucket-resume-check --mount 'type=bind,source=C:\SSAFY\PYJ\PROJECT\Exile-Hephaistos\codex\bucket-method-search,target=/source,readonly' --mount 'type=bind,source=C:\SSAFY\PYJ\PROJECT\Exile-Hephaistos\codex,target=/task-cache' --mount 'type=bind,source=/var/run/docker.sock,target=/var/run/docker.sock' -e TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal -e GRADLE_USER_HOME=/task-cache/bucket-gradle mcr.microsoft.com/devcontainers/java:21-bookworm bash -lc 'mkdir -p /codex/source/backend && cp -a /source/backend/src /source/backend/gradle /source/backend/*.kts /source/backend/gradlew /codex/source/backend/ && cp -a /source/contracts /codex/source/ && cd /codex/source/backend && result=0; bash gradlew --no-daemon check generateJooq bootJar || result=$?; cp -a build/test-results build/reports /task-cache/bucket-method-verification/; exit "$result"'
```

Local evidence (excluded from Git along with caches/temp files):

- `..\bucket-method-verification\test-results` and `reports`: fully checked v9
  backend results.
- `..\bucket-method-verification\frontend-orbit.log`: full frontend checks.
- `..\bucket-method-review\TEST-orbit-MethodSearchProbe.xml`: v9 probe.
- `..\bucket-method-review\TEST-rational-MethodSearchProbe.xml`: arithmetic probe.
- `..\bucket-method-review\probe\MethodSearchProbe.java` and `probe.gradle`:
  optional isolated diagnostic driver; not part of production tests.

All task-owned temporary containers used `--rm` and finished. At stop inventory,
no `bucket-*` container remained. Unrelated running `mini-redis-*` services and
all existing project databases/volumes were left untouched. The PC stays on.
