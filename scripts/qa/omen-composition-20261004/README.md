# Omen composition QA harness — 2026-10-04

이 harness는 사용자 승인된 격리 QA project 전용이다. 원본/live server18080/18081에 실행하지 않는다. 공유 heavy slot을 받은 뒤 `codex/omen-bundle-20261004`의 복사본·jar·dist와 별도 Compose18280/18281을 사용한다. 임의 서버나 사용자 item/storage를 대상으로 실행하지 않는다.

- `api.cjs`: Node24 Docker runner에서 `/evidence`가 bundle-owned 폴더다. API는 `host.docker.internal:18280`로 고정한다.9base synthetic fixtures의 후보/weighted draw/원자적 거부/소모/legacy5를 검사한다.
- `browser-fixtures.cjs`:9개 초기 catalog를 읽어 synthetic film fixture를 만든다. API action PASS 결과가 아니다.
- `browser.cjs`: 기존 project Playwright image(`/qa/node_modules/playwright`)에서 새 context로18281만 검사한다. 기존 사용자 browser/localStorage는 사용하지 않는다.

실패를 숨기거나 assertions를 제거하지 않는다. API 실패는 `api-resumption-failure.json`에 마지막 request/raw response/parsed response/check를, browser 실패는 `browser-resumption-failure.json`에 마지막 request/response/film/DOM cursor/disabled 상태를 저장한다. 기존 최초 실패 파일은 덮어쓰지 않는다. synthetic 데이터만 캡처하며 private item text/secret는 다루지 않는다.

StateBucket→ItemState 비교는 이미 정해진 nullable `augmentSockets/catalystQuality`와 canonical list order만 맞춘다. 다른 필드·각 original roll·fractured flag는 유지하고 deep equality와 events/assumptions/소모 없음 검사를 그대로 적용한다. Browser restore는 다른 session의 catalog loading 뒤 active ID·DOM step 반영을 기다리고, 같은 active session은 재선택 없이 이전 frame으로 이동한다. 각 클릭 후 저장 cursor·DOM step 감소를 확인한다.

이 파일들은 한정 재개를 위해 준비됐고 아직 repaired runtime QA PASS를 의미하지 않는다. 원래 복구2회는 보존한다. 사용자의 명시 `중단없이 진행` 지시를 우선한 추가 bounded harness 복구1묶음으로 기록하며, stage/session으로 기존 횟수를 초기화하지 않는다. 반복 실패 시 캡처와 원인을 보고하고 임의 추가 반복하지 않는다. i18n slot 반환 전 heavy command는 실행하지 않는다.

관련 계약·이력: [모델과 checkpoint](../../../docs/workbench-omen-composition-2026-10-04.md), [검증 근거](../../../docs/evidence/workbench-omen-composition-validation-2026-10-04.json), [Worker 운영 명세](../../../docs/solo-workflow/MULTI_SESSION_WORKFLOW.md).
