# Ancient Liquid / Time-Lost QA

격리 복사본 `../ancient-liquid-20261004`, project `exile-ancient-liquid-20261004`, API/UI18980/18981만 사용한다. 기존 live18080/18081·사용자 browser/storage·DB volumes를 변경하지 않는다. heavy QA owner는 이 bundle 전용이며 종료 시 소유 서비스만 정상 stop하고 해제한다.

`sources.cjs`는 base별6언어 원문을 수집한다. `build.cjs`, `wire.cjs`, `localize.cjs`는 이번 ingestion/구현을 만든 일회성 변환 기록이다. 완성된 source에 재실행하지 않는다. `prepare.cjs`는 기존 output을 덮어쓰지 않고 새 격리 복사본과 Compose를 준비한다. `sync-backend.cjs`는 당시 실패 복구 기록이며 최종 source에 재실행하지 않는다.

검사는 Backend `spotlessApply check generateJooq bootJar` → Frontend `npm ci`, formatter, lint/typecheck/format:check/unit/build → 격리 API → actual-response Frontend contract → browser 순이다. `api.cjs`는 Ancient13 positive actions, cross-category, full/partial slots, radius family conflict, both Contempt directions, overflow/removal/reapply와18 ordinary currencies/Omen을 검증한다. `basic-regression-api.cjs`는 Basic Jewel·quality·Ferocity·Contempt 기존 계약을 다시 검증한다. `capture-contract.cjs`는 실제 API 응답을 deduplicate하고 film 복원·응답 validation 검사를 만든다. `browser.cjs`는 isolated headless context에서 UI/Shift/Alt/undo/redo/film/Omen와6언어 desktop/mobile screenshots를 검사한다.

실패 로그는 보존한다. 최종 결과와 source hash 비교는 `docs/evidence/workbench-ancient-liquid-validation-2026-10-04.json`을 따른다. 로컬 commit만 허용하며 live update·push·merge·release는 하지 않는다.
# Catalyst registry finishing checkpoint

After Ancient commit `e0e0c09`, `reconcile-catalysts.cjs` updates only Catalyst26 metadata and aggregate counts. `update-catalyst-docs.cjs` marks older document counts as checkpoint history. `validate-catalyst-registry.cjs` verifies unchanged non-Catalyst entries/source provenance, imports the checked Java test formatter output and writes a separate registry evidence file after Backend aggregate succeeds. The original Ancient evidence remains immutable; do not rerun `finalize.cjs` to replace that historical snapshot. No API/browser rerun or live service update is needed for this metadata reconciliation.
