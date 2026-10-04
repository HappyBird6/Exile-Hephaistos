# Basic Jewel / Potent Liquid QA

이 bundle의 검증은 별도 복사본 `../basic-jewel-potent-20261004`와 Docker project `exile-basic-jewel-potent-20261004`, API/UI `18880/18881`에서 수행했다. 기존 서비스·사용자 browser/storage·DB volume을 사용하지 않는다. 증거 요약은 `docs/evidence/workbench-basic-jewel-potent-validation-2026-10-04.json`이다.

- `sources.cjs`: 각 base × six-language ModsView 원문을 새 QA 디렉터리에 수집한다.
- `build.cjs`, `wire-*.cjs`, `finish-wiring.cjs`: 이번 source ingestion/구현을 만든 일회성 변환 기록이다. 완성된 source에 다시 실행하지 않는다.
- `prepare.cjs`: 새 QA 복사본과 격리 Compose를 준비한다. 기존 evidence 경로를 덮어쓰지 말고 새 경로/project/ports를 선택한다.
- `api.cjs`: 합성 아이템으로 base applicability, ordinary currencies/refined catalysts, Liquid atomic policy, valid outcome 1/N, Potent directions, overflow removal와 derived projection을 검증한다.
- `capture-contract.cjs`: API 실제 응답을 deduplicate하여 Frontend 응답 검증/film 복원 계약 테스트를 만든다.
- `browser.cjs`: 별도 headless browser context에서 UI/Alt/Shift/film/local Omen와 여섯 언어 desktop/mobile screenshots를 검사한다.
- `finalize.cjs`: 이번 QA 복사본의 formatter 결과를 자기 변경에 반영하고 모든 backend/frontend source의 일치를 확인하여 evidence를 작성한다. 경로는 이번 bundle 전용이다.

무거운 검증은 Backend → Frontend → API → Frontend 계약 → browser 순으로 실행했다. 사용한 전체 명령과 실패/복구 로그는 QA 디렉터리에 보존했다. 종료 시 소유한 Compose 서비스만 정상 stop하고 `heavy-qa-owner.json`을 해제한다. 로컬 commit만 허용하며 remote push/merge/deploy는 하지 않는다.
