# ItemModels·제작대 개선 인계

- 작업/범위: 사용자 요청 1~9번. 이전 작업 `01a0c78b-901a-7b52-bda4-9bc57faefd21`의 미커밋 모델 전환을 이어받아 재료 검색/정렬/제외, 확대 입력 모달, 원문 상태 확장 경계, 우클릭 차단, PoE2DB 툴팁 구현.
- 상태: 구현·검증·자체 리뷰 완료, 작업 브랜치 반영 준비. master 반영·push·기존 Docker stack 재빌드 미실행.
- Worker: `01a0cd1d-1647-79c2-b234-3526cd507550` /root. 하위 에이전트 없음. 소유 범위: item 모델·parser의 모델 참조·해당 테스트/OpenAPI, crafting frontend 및 관련 테스트/문서, 툴팁 출처 JSON.
- worktree: `C:/SSAFY/PYJ/새 폴더/Exile-Hephaistos` (기존 전용 branch 재사용). 기존 작업은 idle/interrupted 확인 후 인계받음.
- 기준 master/HEAD: `3fda53992e31e10aa536503ea46788a30e0ba678`.
- branch: `codex/item-model-and-stash-refinement`. 구현 commit: `924bf88` (인계 문서 commit은 후속).

## 결과

- `ItemModels.Item`으로 응답 통합, `modifiers[].type` 계약으로 Backend/OpenAPI/Frontend 일치. 중복 ItemTextModels 삭제. 파서의 해석 범위/규칙은 변경하지 않음.
- 검색창은 재료 목록 스크롤 밖. 검색어는 탭별 유지. Essence 19종×4티어, 특수 6개는 중앙 2행×3열. Alloy 13개 독립 탭, Omen 18개 제외 후 32개. 15칸 즐겨찾기 유지.
- native dialog 확대, X/바깥/Escape 닫기 및 포커스 복원. 성공 분석 후 닫기, 실패 시 유지. 현재 아이템과 원문은 성공 응답을 받을 때 함께 교체. 편집/실패/취소 시 배치 아이템 유지, 늦은 응답 폐기. 원문 document 타입은 후속 checkpoint 설계를 고정하지 않음.
- 제작대 context menu 차단. 201개 표시 화폐/재료에 출처 툴팁, hover/focus/Escape 및 접근성 연결. 출처는 표시 전용으로 Engine 규칙에 사용하지 않음.

## 검증

- Frontend 최종 소스: `frontend/`에서 `npm.cmd run lint`, `typecheck`, `test -- --run`, `build`, `format:check` 모두 PASS. 6개 test file, 50 tests, 실패/skip 없음.
- Backend 최종 Java 소스: `backend/gradlew.bat -I ../data-pipeline/captures/tooltip-20260923/validation.gradle check --offline` PASS. unit/ArchUnit 65 + PostgreSQL/Redis integration 9 = 74 tests, 실패/skip 없음. XML 직접 확인.
- Java 21: `C:/Users/SSAFY/.codex/visualizations/2026/09/21/01a0c22b-e82d-7df3-be69-bfbddafce945/toolchains/jdk-21.0.12.1+1`; GRADLE_USER_HOME `C:/Users/SSAFY/.gradle`.
- 임시 검증 init: buildDirectory를 `%TEMP%/exile-item-model-01a0cd1d/build`로 지정. 기존 Byte Buddy agent 1.17.8을 테스트 JVM 시작에 지정. Spotless는 ItemModels 및 item/testparser main/test만 검사. 테스트 source 제외/skip 없음, 영구 build 설정 변경 없음. 결과 경로 `%TEMP%/exile-item-model-01a0cd1d/build/test-results/`.
- 환경 이슈: 기본 한글 build 경로에서는 test worker ClassNotFoundException. 첫 ASCII 경로 실행은 JDK 동적 attach에서 Byte Buddy 하위 프로세스 대기. 해당 작업의 테스트 Worker/attacher만 종료하고 위 설정으로 성공. 처음 전역 Spotless가 변경한 범위 밖 ExaltedAction 포맷은 원래 bytes로 복원.
- 브라우저: 임시 Vite 15174에서 desktop Essence 4열/특수 6개/검색 결과/확대 모달/닫기/Chaos 툴팁 확인. 390px 화면에서 탭·검색·특수 아이콘·공용 슬롯 확인, viewport 복원. 임시 탭/서버 종료.
- live API smoke: 기존 localhost:8080 연동에서는 분석 오류가 나와 성공 통합 smoke로 기록하지 않음. 기존 실행 서버의 응답/버전 원인은 확정하지 않음. 새 계약은 MockMvc 및 Frontend mock 회귀로 검증됨. 기존 실행 컨테이너를 재빌드하지 않았으므로 Backend/Frontend 동시 재빌드 후 live smoke 필요.
- 자체 리뷰: 기준 SHA..924bf88 diff와 계약/입력 보존/늦은 응답/출처/테스트 증거 대조, 차단 결함 없음. 독립 리뷰 미실행(사용자 요청 없음). `git diff --check` PASS.

## 출처·남은 범위

- `frontend/public/assets/materials/tooltip-sources.json`에 공개 영문 Currency/Essence/Omen/Catalysts/Liquid_Emotions 및 robots/General disclaimer URL·시각·SHA-256·parserVersion 보존. raw는 git 제외 `data-pipeline/captures/tooltip-20260923/`에 있음. 접근 차단 우회 없음.
- 기존 이미지 미확보 2개(Perfect Essence of the Mind, Reaver Catalyst)는 이번 범위에서 재수집하지 않았으며 fallback 유지.
- 실제 Engine 결과 연결/직접 구조화 아이템 편집/원문 serializer/체크포인트 UI·영구 저장은 후속. 앞으로 확정된 item과 대응 text를 함께 적용해야 함. 게임 catalog/patch 검증이나 공개 배포 권한을 추정하지 않음.
- 수정 횟수: 이번 재개에서 테스트의 기존 5탭 기대값 1회 수정. 이전 세션의 수정 횟수는 기록 부재로 미확인; 초기화로 간주하지 않음. 환경 재시도는 위 별도 기록.
- 다음 행동: 사용자 검토 후 Git 전용 채팅에서 대상 SHA 확인/통합. `modifiers[].kind` → `type`이므로 Backend/Frontend를 같이 반영. 로컬 실행 반영 시 루트 `scripts/dev.ps1 stack` 후 실제 분석 smoke. 운영 DB/volume 변경 없음.
