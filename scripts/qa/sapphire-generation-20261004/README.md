# Sapphire generation / Basic Liquid QA

Sole writer `workbench/20261002`, baseline `3208ff40d583d72ab7c77eb1baea595d644fb9a9`. Owned evidence/runtime copy: `E:/WORK/Exile-Hephaistos/codex/sapphire-generation-20261004`. Preserve all earlier QA outputs and localhost18080/18081.

Source is read-only at `/source`, checked copies and new evidence at `/evidence`. Use existing `mcr.microsoft.com/devcontainers/java:21-bookworm`, `node:24-bookworm`, `exile-workbench-browser:20261002` images. Frontend requires Node24/npm11; never override engine requirements. Docker integration/codegen uses disposable containers and the Docker socket, with `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal`. Runtime Compose owns its network, tmpfs PostgreSQL and Redis; localhost18780/18781 only. No live or original build output is mounted writable.

Run heavy checks sequentially: Backend `spotlessApply check generateJooq bootJar`, Frontend `npm ci` then lint/typecheck/format:check/test/build, isolated runtime, API, browser. Formatting is synced only to this writer's modified files. Compare final checked source bytes to the worktree before commit. The `prepare.cjs` scaffolder is used only once for a new owned QA directory; do not rerun it over historical outputs.

`extract.cjs` parses the targeted Sapphire `new ModsView` JSON. `build-catalog.cjs` imports only normal58 and ordinary Liquid10; zero crafted generation weight and explicit modeled equal ordinary weights. `localize.cjs` matches six-language rows by exact family/side/level/tags/bounds; Spanish lacks a Liquid section and uses the identical ordinary row as its verified display template. No display-name eligibility matching.

`api.cjs` exercises all18 basic currencies and all10 Liquid positive paths, slot/family filtering, tiered exception, Omen composition, Crafted removal/reroll, refusal preservation and Problem Details. Registration alone does not pass these checks.

`browser.cjs` uses a fresh context with synthetic QA films. It exercises Normal→Magic→Rare→four slots, all10 Liquid UI applications, Crafted colour and persisted identity, undo/redo/reload, Alt/Shift, Whittling orange candidates and six-language 1440/390 screenshots. Inspect generated screenshots, not only assertions. Never reuse a user's browser profile/storage.

Keep engine/test/API/browser failure logs and screenshots when recovering. Stop only this Compose project normally after evidence collection; no DB reset or external deployment.
