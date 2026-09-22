# poe2db 원본 수집기

현재 단계는 **원본 수집 전용**이며 관리자 대상 snapshot 입력을 지원한다. 테스트는 네트워크를 차단한 synthetic 응답으로 실행한다.
`--status`와 인수 없는 실행은 외부 요청을 보내지 않는다. 스케줄러·자동 실행·DB 쓰기는 없다.

## 실행 계약

Python 3.12와 `uv sync --frozen`으로 잠긴 개발 환경을 준비한다.
프로젝트의 [출처 정책](../docs/data-sources.md)을 확인한 후 실제 수집이 필요할 때만
`data-pipeline/`에서 다음 명령을 **직접 실행**한다. 이번 구현 검증에서는 실행하지 않았다.

```powershell
uv run --frozen python -m poe2etl --status
# 아래 명령부터 외부 네트워크 요청이 발생한다.
uv run --frozen python -m poe2etl crawl --pages currency amulets --output captures
```

`--pages`는 `currency`, `amulets`만 지원하며 중복은 제거한다. 기본값은 두 페이지다.
`--target-patch`는 선택적 조사 라벨이다. 값을 넣어도 사이트의 실제 패치와 일치한다고 검증하지 않는다.
사이트 전체를 순회하거나 Currency 페이지 목록이 게임의 모든 화폐라고 주장하지 않는다.
상세 페이지·이미지·스크립트·내부 API를 추가 요청하지 않는다.

## 요청과 실패 처리

- `https://poe2db.tw`의 robots, 이용 고지, 허용 형식의 지정 대상 페이지만 요청한다.
- robots 확인 실패·접근 금지 시 중단한다. User-Agent는 `Exile-Hephaistos-Research/0.1`이다.
- 최소 요청 간격은 2초이며 robots의 Crawl-delay/Request-rate가 더 길면 따른다.
- HTTP 요청 timeout은 30초, 응답 크기는 8 MiB까지다.
- 429/502/503/504는 최대 1회 재시도한다. Retry-After가 있으면 따르고, 없으면 5초 대기한다.
  60초를 넘는 대기 지시는 재시도하지 않고 이번 배치를 실패로 끝낸다.
- redirect·401·403 등은 우회하거나 재시도하지 않는다. 실패 시 종료 코드는 1이다.
- raw 원문에는 모든 응답을 보존한다. HTML 내용의 의미·봇 도전 페이지 여부는 검증 전이며,
  `RAW_CAPTURED`는 HTTP 원본 확보만 뜻한다. 후속 파서는 잘못된 페이지를 성공 데이터로 처리하면 안 된다.

## 산출물

```text
captures/<UTC-time>-<random-id>/
  manifest.json
  raw/<sequence>-<sha256>.bin
  raw/<sequence>-<sha256>.bin.json
```

새 실행은 새 폴더를 사용하며 기존 파일을 덮어쓰지 않는다. raw 파일은 수신 bytes 그대로다.
각 응답에 URL, locale, UTC 수집 시각, SHA-256, HTTP status, 허용된 응답 header를 기록한다.
manifest에는 importRunId, collectorVersion, 요청 횟수, targetPatch, 실패 이유를 남긴다.
`parserVersion=null`, `patchVerified=false`, `productionEligible=false`다.
실패 전 확보한 증거는 유지되며 실패한 배치를 빈 목록 성공으로 바꾸지 않는다.

정규화된 Currency/Modifier/weight 객체, staging·validation·diff·publisher는 후속 단계다.
그 단계의 import 멱등성 키는 명세대로 source + rawHash + parserVersion + targetPatch로 구성한다.
재수집 원본을 임의로 published snapshot에 합치지 않는다. raw는 Git 제외 경로에 로컬 보관한다.

## 오프라인 검증

```powershell
uv run --frozen ruff check .
uv run --frozen ruff format --check .
uv run --frozen python -m pytest
```

fixtures는 가상의 내용이며 실제 게임 데이터나 production seed가 아니다.

## 관리자 대상 snapshot 계약

관리자가 저장한 대상은 `python -m poe2etl crawl --targets-file ABS_JSON --output ABS_JOBDIR`로
전달한다. `--pages`를 명시하면 `--targets-file`과 함께 사용할 수 없다. 기존 기본 두 페이지는 유지한다.
JSON은 `{ "targets": [{ "id": "UUID", "name": "표시 이름", "url": "https://poe2db.tw/kr/Currency", "enabled": true }] }`다.
64 KiB 이하의 절대 경로 일반 파일만 읽고, 대상 1~20개, 중복 없는 UUID/URL, 필수 boolean enabled를
검사한다. 이름은 공백만으로 구성될 수 없고 최대 80자다. enabled 대상이 하나도 없으면 실패한다.

URL은 정확히 `https://poe2db.tw/(us|kr)/[A-Za-z0-9_-]+` 형식만 허용한다.
포트·userinfo·query·fragment·percent encoding·추가 경로는 거부한다. 대상 내용은 실행 명령이나
출력 경로로 사용하지 않는다. 공통 robots와 `/us/General_disclaimer`를 먼저 확보하고
모든 활성 대상의 robots 허용 여부를 검사한다. redirect는 계속 금지한다.

manifest의 `targets`는 비활성 대상을 포함한 검증된 입력 snapshot이다. 각 source의 `targetId`는
설정 대상 UUID이며 공통 정책 증거와 기존 `--pages`는 null이다. 입력 파일 검증 실패도 출력 아래
단일 실행 폴더에 실패 manifest를 남긴다(출력 폴더 자체를 만들거나 쓸 수 없는 OS 오류는 예외).
원본 비교·정규화·DB 최신화·게임 사실 검증은 실행하지 않는다. 이번 구현 검증은 네트워크 차단
fixture만 사용하며 live 수집을 수행하지 않는다.
