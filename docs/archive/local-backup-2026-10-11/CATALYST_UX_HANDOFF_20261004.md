# Catalyst / Workbench UX 완료 인계 — 2026-10-04

- 작업 checkout: `E:\WORK\Exile-Hephaistos\codex\workbench-20261002`
- branch: `workbench/20261002`
- 시작: `47b183a59b9802efa37f4cc53cb3222206affdf4`
- 최종 로컬 commit: `1b8a8884aa1ce13e260c0b1d4422cfa6f9174349`
- clean worktree, 단독 작성·자체 리뷰. push/merge/deploy/release 없음.

26 catalyst stable ID 모두 action에 연결했다. ordinary13은 검증된 Ring/Amulet에 source-backed 최대 품질을 설정하고 타입을 교체한다. 원본 roll은 그대로이며 중앙 HALF_UP 정책으로 표시를 한 번 파생한다. refined13은 Jewel 제한을 유지한다. 현재9개 catalog에 Jewel이 없으므로 실제 긍정 적용131=기존118+ordinary13, Jewel 제한13, 선택144/등록220/유예76으로 구분한다.

Fracture 회색·일반 우선 정렬·Alt 원래 P/S 순서·accessible 의미, Select base/New craft 진입, item 아래 안내, 여유 높이 0–24px spacing, Last craft 영역 삭제와 단일 확률 안내를 구현했다. linear film과 original future, Omen preview/Erasure side→lowest level→tie1/N, Greater/Perfect/multi Omen, favorites/rightclick, Alt/Shift/empty click 및6개 언어를 유지했다.

검증: Backend364 unit/ArchUnit +6 Docker integration와 check/generateJooq/bootJar PASS. Frontend 전체49파일362테스트+필수 검사 PASS. 마지막 CSS/기존 안내 문구 수정 이후 lint/typecheck/format/관련3파일22테스트/build와 영향 browser224+113 재검증 PASS. API640+1004=1644, browser125+224+113=462(중복 재실행 수 제외), page error0. source338 FE/214 BE 파일이 checked copy와 SHA256 동일하다. 실패 log와 QA selector/fixture 수정 이력은 validation.json 및 repository evidence에 남아 있다.

문서: repository `docs/workbench-catalyst-ux-2026-10-04.md`, `docs/evidence/workbench-catalyst-ux-validation-2026-10-04.json`, ISSUES.md WB-044. 실제 fixtures/logs 및 screenshot은 이 QA 디렉터리에 있다. `fracture-normal.png`, `fracture-alt.png`, `ko-catalyst-1440.png`, `es-catalyst-390.png` 등을 직접 검토했다. 각6언어×2폭 screenshot도 보존한다.

남은 경계: refined 긍정 적용은 요청된 별도 Jewel catalog 근거가 필요하다. cap 제거로 기존 품질이 새 cap을 초과하는 게임 동작, 소수 정밀도, whitelist 밖 numeric unit/음수/mixed 표시, WB-042 special/modifier-level-less/fractured 제거, 기존 번역 fallback은 미확인이다. 실제 1회 품질 증가 모델은 사용자 요청으로 생략한다. Desecration/Vaal/Hinekora/Liquid/Catalysing/Necromancy 등 유예 항목은 확장하지 않았다.

전용 Compose `exile-catalyst-ux-20261004`(18480/18481)는 down 완료, 네트워크 제거 완료, heavy QA 슬롯 해제. 사용자4개 container(18080/18081 app/frontend, PostgreSQL healthy, Redis)는 실행 유지 확인. 사용자 browser/localStorage/.env/DB volume/원본 checkout은 변경하지 않았다. 이 QA 디렉터리와 소유한 Gradle cache volume은 evidence 재사용을 위해 보존한다.

다음 Git 반영은 이 SHA와 문서를 근거로 기존 Git 전용 절차에서 처리한다. 새 QA가 필요하면 이 격리 workflow만 사용하며 사용자 live 서비스나 원본 build output을 덮어쓰지 않는다.
