# 관리자 원본 수집

관리자 1명이 PostgreSQL에 수집 대상을 저장하고 수동으로 Python 원본 수집 작업을 실행한다. 비교·정제·최신화·게임 catalog 발행은 지원하지 않는다. 외부 사이트 접근 조건 확인은 운영자의 책임이며 기본 실행 설정은 비활성이다. 이번 구현 검증은 HTTP fixture와 mock runner만 사용하며 live crawl을 실행하지 않는다.

## 설정과 로그인

`.env`에 `APP_ADMIN_USERNAME`, `APP_ADMIN_PASSWORD`를 함께 설정한다. 둘 다 비어 있으면 session 조회 외 관리자 API가 비활성이다. 한쪽만 있거나 아래 조건을 위반하면 시작에 실패한다.

- Username: ASCII 영문·숫자·`_.-`, 3–64자.
- Password: 최소 16자, UTF-8 인코딩 기준 최대 72바이트(BCrypt 제한). 영문 대문자·소문자·숫자를 포함하고 제어 문자는 제외한다. Username과 `password`, `replace` 문자열을 포함하지 않는다. 다국어 문자는 여러 바이트를 사용하므로 72자 이하라도 상한을 초과할 수 있다.
- 한 프로세스에서 로그인 요청은 분당 최대 10회다. IP 헤더를 신뢰하지 않는 보수적 전체 제한이며 여러 인스턴스의 합산 제한은 제공하지 않는다. 로컬 Compose는 단일 Backend 인스턴스를 사용한다.

로그인은 Spring Security 서버 session이다. session cookie는 Secure/HttpOnly/SameSite=Lax이며 CSRF가 로그인·로그아웃·설정 저장·수집 실행 모두에 적용된다. HTTP localhost 개발에서만 `APP_SESSION_COOKIE_SECURE=false`를 명시한다. 이 옵션을 외부 HTTP 서비스에 사용하지 않는다. 계정은 DB 사용자 계정이나 OAuth 공급자가 아니다.

`GET /api/v1/admin/session`의 CSRF token/header를 mutation 요청에 전달한다. 로그인 후 새 token을 조회한다. 세션 만료 후에도 다시 조회한다. probe 이외의 기존 endpoint 거부 정책은 유지한다. API 계약은 [openapi-admin.yaml](openapi-admin.yaml)에 있다.

## 실행과 보관

`APP_CRAWL_ENABLED=true`일 때만 실행 버튼의 요청을 받는다. Compose Backend 이미지에 Python 3.12와 `poe2etl` 원본이 포함되며 별도 Python 서버를 실행하지 않는다. 호스트 실행은 `backend`에서 시작하며 기본 보관 위치 `../data-pipeline/captures`는 Git에서 제외된다. 다른 작업 디렉터리에서 실행한다면 보관 경로를 절대 경로로 지정한다. 호스트 실행에서는 `APP_CRAWL_PYTHON`으로 Python 실행 파일을 지정하고 `PYTHONPATH`를 저장소의 `data-pipeline/src` 절대 경로로 지정한다. `APP_CRAWL_DIRECTORY`는 서버 운영자가 지정하는 디렉터리이고 API 인수로 변경할 수 없다.

기본 대상은 `Currency`, `Amulets` 두 항목이다. 대상은 최대 20개이며 URL은 `https://poe2db.tw/(us|kr)/`와 안전한 단일 slug(영문·숫자·`_`·`-`)만 허용한다. URL 전체 길이는 1024자로 제한한다. query, fragment, port, userinfo, encoded path, 다른 host는 거부한다. 설정 저장은 전체 목록 교체이며 version 충돌은 409로 반환한다. 활성 대상이 없으면 실행은 422다.

각 작업에는 실행 시점 활성 대상 snapshot을 저장한다. DB unique partial index와 singleton 설정 행 잠금으로 여러 요청·인스턴스의 중복 활성 작업을 막는다. 백그라운드 실행은 enqueue transaction이 끝난 후 시작하며 고정 argv로 `python -m poe2etl crawl --targets-file ABS_JSON --output JOBDIR`를 호출한다. shell을 사용하지 않으며 DB·관리자 등 secret 환경변수를 자식 프로세스로 전달하지 않는다.

프로세스는 최대 570초 후 종료하고 DB의 10분 stale 회수보다 여유를 둔다. stdout은 64KiB, 결과 manifest는 256KiB로 제한하고 stderr는 저장·노출하지 않는다. 종료 code, JSON 상태, job 하위 실제 manifest 경로, source metadata를 검증한다. 실패는 `CAPTURE_FAILED` 등 정해진 code만 응답하며 외부 오류 원문은 반환하지 않는다. 정상 종료 때 자식 프로세스를 종료한다. 비정상 재시작 이후 남은 활성 작업은 10분 경과 후 다음 조회/실행 때 `RUN_EXPIRED`로 정리한다. 자동 재실행은 하지 않는다.

Compose `crawl-captures` volume은 원본과 manifest를 보존한다. API는 URL·상태·hash·크기·시간 metadata만 제공하며 원본 HTML을 서빙하지 않는다. 자동 삭제·보존 기간 정리는 구현하지 않았다. `docker compose down -v`는 이 volume을 삭제할 수 있으므로 데이터 보존 시 사용하지 않는다. 기존 PostgreSQL 외부 volume 정책은 유지한다.

## 상태와 오류

`QUEUED → RUNNING → RAW_CAPTURED | FAILED`. 원본 수집 완료는 게임 데이터 검증·발행 완료가 아니다. 목록은 최근 50개를 반환한다. 상세 조회는 `sources` metadata도 포함한다. 미완료/실패 작업의 `sourceCount`는 null이며 0 성공으로 표시하지 않는다. Python이 실패 전에 저장한 raw 증거는 volume에 보존되지만 실패 작업의 source 요약 API는 제공하지 않는다.

409: `STALE_SETTINGS`, `RUN_ACTIVE`. 422: `INVALID_TARGETS`, `INVALID_VERSION`, `NO_ENABLED_TARGETS`. 503: `RUNNER_DISABLED`, `RUNNER_UNAVAILABLE`, `CRAWL_STORAGE_UNAVAILABLE`. 인증·CSRF 실패와 로그인 제한도 Problem Details 형태로 반환한다.
