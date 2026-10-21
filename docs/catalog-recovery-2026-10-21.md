# 규칙 변경 후 카탈로그 복구

기준 커밋은 `3572dd853ae15e3f0983da216dc55fb55ca556ea`, 작업 브랜치는 `codex/automatic-refinements-20261021`다. 검증·QA 반영 당시 미커밋 상태였으며, 이후 사용자 승인으로 같은 작업 브랜치에 원격 보존한다. 커밋 SHA는 Git 이력을 기준으로 한다.

## 확인한 현상과 원인

사용자 로그는 구형 QA 번들 `index-DL-TJmSs.js`의 `POST /api/v1/crafting/support/goal-filters/validate` 422였다. 그 로그만으로 사용자의 실제 요청 payload/응답 본문을 확정할 수는 없다. 연결 가능한 브라우저가 없어 해당 탭의 네트워크 본문은 직접 열지 못했다. 대신 실제 QA에서 요청 유형별 응답을 재현하고 컴포넌트 상태를 회귀 검사했다.

catalog GET 장애로 단정하지 않았다. registry와 베이스 초기화 134개는 모두 200이고 초기화의 응답 헤더/본문 ruleset identity가 일치했다. 직접 catalog GET과 provenance도 정상이다.

실제 응답은 다음과 같다.

| 요청 | 응답 |
| --- | --- |
| 최신 catalog + 유효 목표, quality 생략 또는 null | 200 |
| quality 20만 있는 유효 목표 | 200 |
| 빈 활성 목표 그룹 | 422 `INVALID_GOAL_FILTER`, issue `EMPTY_GROUP` |
| quality 41 | 422 `INVALID_GOAL_FILTER`, issue `INVALID_QUALITY` |
| 구형 catalogVersion + 현재 ruleset 헤더 | 409 `CATALOG_VERSION_MISMATCH` |
| 구형 ruleset 헤더 | 422 `RULESET_IDENTITY_MISMATCH`. 실제 응답에는 `issues` 필드가 없음 |

확인된 화면 복구 결함은 제작 시작 inventory의 무기한 캐시와 이전 규칙의 저장 초안에 대해 경고만 표시하고 최신 bootstrap을 받아 새 제작으로 전환하는 동작이 없던 점이다. 구형 초안과 이미 캐시된 inventory 두 경우를 DOM 검사로 재현했다. 오류 응답의 `issues: []`가 명시적 ruleset 오류 코드를 덮어쓰는 별도 parser 결함도 확인하여 보완했지만, 이를 사용자의 실제 응답이라고 주장하지 않는다. 모든 422를 규칙 변경으로 분류하지 않는다.

## 수정

- 6개 언어의 “카탈로그를 다시 불러와 새 제작 시작” 동작을 추가했다. registry·initial·catalog·provenance를 새로 받고 초기 상태와 provenance의 규칙 및 catalog context가 맞을 때만 새 서버 preset으로 전환한다.
- 이전 아이템에 새 규칙 identity를 덧씌우지 않는다. 실패 시 기존 초안과 저장 기록을 보존하며 다시 시도할 수 있다. 재조회 중 기존 실행 화면/새 제작 제출은 막는다.
- 목표 편집기와 bootstrap 복구가 같은 HTTP adapter의 검증 identity를 공유한다. 새 catalog를 읽은 뒤 validate는 새 identity를 사용한다. 기존 목표는 검증을 거쳐 catalogVersion만 갱신한다.
- registry/initial/catalog/provenance 재조회는 HTTP 캐시를 우회하고 진입 HTML은 `no-cache, must-revalidate`로 응답한다. 해시가 붙은 JS/CSS 파일명은 유지한다.
- 명시적인 `RULESET_IDENTITY_MISMATCH`는 issues 필드가 없거나 빈 배열이어도 보존한다. 일반 `EMPTY_GROUP`/`INVALID_QUALITY` 422는 입력 오류로 유지하며 catalog 재조회 루프를 만들지 않는다.

## 검증과 QA 반영

Docker 집중 검사 67개가 통과했고, 최종 프런트 전체 **2,296개**와 lint/typecheck/format/6개 언어 자료 검사/build가 모두 통과했다. 복원된 구형 초안, 캐시된 inventory, catalog 조회 실패 후 재시도, 혼합 규칙 bootstrap 거부, 기존 run 보존, 실제 목표 편집기에서 새 catalog/provenance와 품질 조건을 담은 새 제작 제출을 검사했다. 전체 검사 중 provenance 캐시 옵션의 옛 요청 tuple 단언은 새 `cache: no-store`를 명시하도록 갱신했으며 최종 전체 실행은 실패 0이다.

프런트 코드와 nginx 설정만 바꿨다. 따라서 기존 백엔드 전체 검사 근거를 재사용했고 백엔드나 DB/Redis는 다시 빌드·기동하지 않았다. QA 18081 프런트만 교체했다.

- 주소: `http://localhost:18081`; QA 백엔드 18080 유지, 8080 유지, 8081은 중지 상태 유지.
- 실행 이미지: `sha256:813518bc096d708e3abf26372ff77a2af13eaab034cb9098ba1ab7ce5600c320`.
- 실행 JS: `index-QMtz8qAN.js`, CSS: `index-Su0xNYxn.css`.
- 이미지의 정적 파일 227개가 최종 테스트 빌드와 일치했다. 실행 이미지 ID와 nginx 설정도 소스와 대조했다. `/`, `/admin`, `/index.html`의 HTML 및 캐시 헤더, JS/CSS 응답 바이트, nginx 설정 문법을 확인했다.
- 실제 QA HTTP/API 19건: 구형 헤더 422와 구형 catalog 409 구분 → 최신 catalog 재조회 → validate 200 → 새 일반 목걸이 제작 생성 202 → 계산 COMPLETE까지 확인했다. 새 결과는 구체 상태 1,801개, 방법 3개였다.
- 기존 저장 파일 35개가 모두 같은 SHA-256으로 유지됐다. 검증용 새 계산 파일만 1개 추가했다. 환경 파일 해시·기존 app/DB/Redis의 ID·시작 시간·마운트는 같고, 8081 프런트는 계속 중지다. 이전 QA 프런트 이미지는 복구 태그로 보존했다.

근거는 작업 폴더 `evidence/catalog-recovery/`의 `validate-matrix.json`, `all-initial-before.json`, `full-final-results.xml`, `build-final-proof.json`, `live-proof.json`, `source-final.json`과 실제 새 계산 JSON이다. 원본 저장 파일 백업과 빌드 파일은 저장소 밖에 보존한다.

실제 브라우저 탭/픽셀 확인은 미실시다. 사용 가능한 앱·브라우저 목록이 비어 있었으며 권한 우회를 하지 않았다. 이미 열린 구형 JS 탭의 DOM을 직접 변경했다고 주장하지 않는다. 위 반영은 최신 QA 응답과 테스트된 복구 흐름에 대한 확인이다. 기존 완료 회귀 05/06/07은 이번 문제와 별개이며 해결된 것으로 보고하지 않는다.


## 원격 보존 범위

사용자 승인에 따라 이 복구 수정의 소스·회귀 검사·관련 문서 13개 파일만 선택하여 작업 브랜치에 커밋한다. 실행 QA와 검증 manifest의 내용을 대조하고 민감정보 및 diff 공백 검사를 수행한다. 코드 기능은 원격 보존 단계에서 더 바꾸지 않는다. 실제 저장 파일·DB/Redis·환경 설정·자격증명·백업·빌드 파일과 대용량 QA 결과는 제외한다. 검증 근거는 위 결과를 재사용하며 서비스 재시작·추가 배포·master 병합·force push를 하지 않는다. 원격 SHA와 CI 검사 유무는 push 뒤 별도로 확인한다.
