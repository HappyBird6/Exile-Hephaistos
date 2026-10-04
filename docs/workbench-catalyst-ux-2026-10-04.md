# Catalyst 정책 및 Workbench UX — 2026-10-04

기준 SHA `47b183a59b9802efa37f4cc53cb3222206affdf4`, branch `workbench/20261002`.
단독 작성으로 catalyst 적용/유형 교체 및 요청한 UI 피드백을 구현한다.
이전 typed-quality 문서의 전체 제작 거절/절삭 정책은 이 변경으로 대체한다.

## 정책과 범위

26개 stable catalyst ID를 각각의 Workbench action에 연결한다. ordinary13은 검증된 Solar Amulet/Iron Ring에 적용하며 현재 cap을 즉시 설정한다. refined13은 Jewel 전용이며 현재9개 catalog에는 Jewel이 없어 명시적으로 거절한다. 적용 가능 base의 확대나 실제 1회 사용 증가량은 추가하지 않는다.

원본 roll/범위/tag/weight는 변하지 않는다. 파생 표시는 원본에서 계산하며 multiple matching tags도 한 번만 적용한다. 소수 표시는 replaceable HALF_UP 정책에 모으고, 기존 compound ratio+round 모델과 일치시킨다. 이것은 사용자가 승인한 시뮬레이터 가정이다. 기존 검증된 numeric-unit whitelist 밖의 수치/음수/mixed-stat은 미검증 상태로 남긴다.

일반 제작은 품질을 보존하고 기존 canonical affix pool을 사용한다. Support/Explorer의 affix-only `StateBucket.from` 거절 경계는 유지하며 Workbench 내부에서만 명시적인 affix projection을 사용한다. Catalysing Exaltation 등 제외 Omen은 추가하지 않는다. 품질40 상태의 cap 옵션 제거는 게임 처리 근거가 없어 해당 후보가 실제 포함된 제거 동작만 거절한다.

## UX와 보존

Select base/New craft 버튼은 stash 위에 보인다. New craft는 현재 base/level의 새 Normal film을 만들며 기존 film/future를 보존한다. 과거 frame 제작은 기존 linear film 규칙을 유지한다. base dialog 종료 focus는 실제 진입 버튼으로 돌아온다.

Fractured affix는 일반 보기에서 다른 affix보다 위, Alt에서는 원래 prefix/suffix 순서에 표시한다. 회색과 screen-reader 의미는 양쪽에서 유지하며 `[Fractured]` 문자열은 보이지 않는다. item 설명은 중앙 item 영역 바로 아래로 옮기며 여유 화면 높이에서만 최대24px을 더한다. 확률 면책은 공통 craft-title 아래의 기존 여섯 언어 문구 하나를 유지한다. Last craft 영역은 제거하되 film evidence는 그대로 저장한다.

PoE2DB [Catalysts](https://poe2db.tw/us/Catalysts)/[Quality](https://poe2db.tw/us/Quality)와 기존 source snapshot의 출처/라이선스를 유지한다. English Catalyst 페이지는 재확인했으나 Korean 페이지 web reader는 실패했다. 기존 여섯 언어 dictionary를 보존하며 새 simulator policy 문구를 여섯 언어로 제공한다.

## 검증 기록

전용 QA 경로: `E:\WORK\Exile-Hephaistos\codex\catalyst-ux-20261004`.
소스를 읽기 전용 마운트해 QA 복사본에서만 runtime/build/test를 수행한다. 이전 원본 output overwrite 거절은 우회하지 않는다. user18080/18081, 사용자 browser/localStorage, 기존 DB volume와 원본 checkout은 보존한다. Heavy QA는 순차 실행한다.

## 완료 및 검증 결과

- Backend `spotlessApply check generateJooq bootJar`: unit/ArchUnit 364, Docker integration 6 PASS. skip/failure/error 0. formatter 결과만 source에 반영했다.
- Frontend 전체 `npm ci`, lint, typecheck, format:check, test, build: 49개 파일 362개 테스트 PASS. 이후 CSS 중앙 정렬과 기존 안내 3종의 여섯 언어 문구를 수정하고 lint/typecheck/format/관련 3개 파일 22개 테스트/build를 다시 PASS했다. 전체 suite와 마지막 영향 검사를 구분하며 중복 테스트 수를 더하지 않는다.
- 실제 API: 기존 Omen 640 + 새 catalyst 1,004 = 1,644 assertions PASS. 9개 base의 적용 제한, 26 action 경로, 반복/교체, 원본29→품질20 표시35 및 품질40 표시41, unscalable cap, above-cap 제거 거부와 typed-quality 일반 제작을 포함한다.
- 실제 브라우저: 기존 Omen 125 + 여섯 언어 224 + catalyst/UX 113 = 462 assertions PASS, page error 0. 마지막 CSS/안내 수정 후 영향을 받는 여섯 언어224 및 catalyst/UX113을 다시 PASS했다. 원본/future 보존, 과거 frame 제작, New craft, 복원, Alt 해제, Shift 반복, empty click, max40, 1440/390px 장문, resize, 1400px 여유/768px spacing 제거를 검사했다.
- 검사한 복사본과 최종 source의 Frontend338/Backend214 파일 SHA256이 모두 일치했다. [기계 판독 evidence](evidence/workbench-catalyst-ux-validation-2026-10-04.json)에 log, 실패 이력, 정확한 수와 동일성을 보존한다.
- 실제 screenshot `fracture-normal.png`, `fracture-alt.png`, `ko-catalyst-1440.png`, `es-catalyst-390.png`를 직접 검토하고 안내 정렬 및 오래된 미지원 문구를 고쳤다. 최종 여섯 언어 screenshot은 격리 QA 폴더에 있다. 작은 화면에서는 기존 stash 내부 가로 탐색과 자연스러운 페이지 스크롤을 유지하며 control을 잘라 no-scroll을 가장하지 않는다. 추가 여백은 전체 viewport에 여유가 있을 때만 0–24px이다.

실패 이력: 첫 Frontend formatter, 첫 전체 테스트의 dialog 중복 이름 selector, Backend의 이전 ruleVersion/action count/typed-quality 거부 기대값, 새 QA의 initial endpoint 오타와 숨김 preview locator 및 즐겨찾기 선택 누락을 기록하고 수정했다. 삭제 요청된 Last craft의 Omen 합성 label 검사는 보존된 API evidence로 옮겼다. 실패 log/JSON을 삭제하거나 기대 확률을 완화하지 않았다.

등록220/선택 가능144/유예76은 재료 목록 범위다. 긍정 동작 적용131은 기존118+ordinary catalyst13이며, 나머지 refined13은 Jewel 제한을 명시적으로 반환한다. 26 catalyst 모두 action 경로가 있지만 현재9개 catalog에는 검증된 Jewel이 없어 144종 모두 긍정 적용된다고 주장하지 않는다. Source/이미지/stable ID/catalog weight는 변경하지 않았다.

남은 경계와 되돌릴 수 있는 결정은 ISSUES.md WB-044에 기록했다. 다음 권고는 검증된 Jewel catalog의 별도 요청/근거 확보, 실제 품질 cap 제거와 소수 정밀도 확인이다. WB-042의 special/modifier-level-less/fractured 제거와 기존 번역 fallback 경계를 유지한다. 제외된 Desecration/Vaal/Hinekora/Liquid/Catalysing/Necromancy는 확장하지 않았다. 실제 1회 품질 증가 모델은 사용자 선택으로 구현하지 않는다.

자체 리뷰: 요청8항목, source diff, canonical roll 보존, API 입력/응답 검증, cap/적용 제한, film 무결성, accessible fracture 의미 및 실제 화면을 단독 구현자가 검토했다. 최종 통합은 작업 branch의 로컬 commit까지이며 remote push/merge/deploy/release는 수행하지 않는다. 전용 QA 서비스는 완료 시에만 종료하며 사용자18080/18081과 사용자 browser/localStorage/DB volume은 유지한다.
