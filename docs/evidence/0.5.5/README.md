# 0.5.5 자료 찾기

무엇을 바꾸거나 확인하려는지에 따라 아래 폴더를 연다. 파일은 소문자·하이픈 이름을 쓰며 날짜를 붙이지 않는다. 특정 아이템의 이름은 파일명에만 남긴다.

| 하고 싶은 일 | 위치 | 자료 내용 |
|---|---|---|
| 번역·언어별 표시 확인 | `i18n/sources/<언어>/` | 아이템·옵션 페이지의 언어별 JSON/HTML. en, ko, zh-cn, zh-tw, ja, es |
| 복합 옵션·단위·문구 대응 확인 | `i18n/mappings/` | 숫자 자리, 원본 단위, 용어와 표시 매핑 |
| 번역 누락 검사 결과 확인 | `i18n/checks/` | 현재 표시 검사 결과 |
| 옵션 수치·가중치·생성 자격 확인 | `catalog/modifiers/` | 실제 옵션명·원본 코드·레벨이 파일명에 있는 modifier 근거 |
| 베이스 원문 확인 | `catalog/bases/` | 언어별 자료와 분리된 기본 속성·공통 source 데이터 |
| 희귀도·용량·지원 대상 확인 | `catalog/eligibility/` | 베이스 선택과 적용 자격·후보 검토 |
| 훼손·드러내기 확인 | `crafting/abyss/` | reveal 정의와 검토된 상태·효과 근거 |
| 품질·축성·소켓 확인 | `crafting/quality/`, `crafting/sanctification/`, `crafting/sockets/` | 해당 기능의 원문과 stat 의미 |
| 이미지 출처 확인 | `assets/` | 화폐·그림의 실제 URL·내용 해시·확보 상태 |
| importer 입력·회귀 확인 | `tooling/catalog-import/` | 가중치·번역 binder의 독립 비교 입력 |
| 현재 전체 구조/검사 결과 확인 | `catalog/checks/`, `assets/checks/`, `release/checks/` | 이동 후 내용·경로·해시와 실행한 검사 결과 |

추가 자료가 생기면 `crafting/currency`, `crafting/essence`, `search/transitions`, `search/first-hit`, `search/automatic`, `search/probability`, `workbench/import`, `workbench/history`, `workbench/layout`, `workbench/feedback`의 해당 기능에 둔다. 이전 실행 기록으로 빈 폴더를 채우지는 않는다.

## 이름과 원문

- `i18n/sources/en/maces-one-hand-maces.en.json`: 영어 옵션 풀 페이지.
- `catalog/modifiers/`의 파일: 대상 pool·옵션 종류·옵션 이름·원본 코드로 구별한다. 실제 출처의 ID와 언어 표현을 JSON 안에서 유지한다.
- 값·가중치·원문·URL·source hash를 유지하며, 과거 수집 날짜·freshness 기록·오래된 QA/배포 결과는 제거한다.
- `0.5.5`는 관리 버전이다. 새로운 게임 확률·가중치·지원 상태를 만들어 넣거나 구형 원문을 새 검증 결과로 표시하지 않는다.

## 도구가 파일을 찾는 방법

[file-index.json](file-index.json)은 도구의 안정적인 입력 ID와 실제 기능별 파일 위치를 연결한다. 입력 ID는 데이터 조합을 식별하며 물리적인 아이템 폴더가 아니다. 과거 날짜 경로나 수집 이력은 인덱스에 넣지 않는다.

기존 수집·import 도구는 `scripts/evidence-files.cjs`를 통해 같은 입력을 읽는다. 예를 들어 `evidence:maces-source/One_Hand_Maces.us.json`은 위 영어 자료로, 같은 입력 조합의 modifier detail은 `catalog/modifiers`로 연결된다. 하나의 도구가 여러 기능의 자료를 사용해도 파일을 복제하지 않는다. 새 파일을 만들 때는 기능에 맞는 이름과 위치를 먼저 정하고 인덱스를 갱신한다.

`node scripts/check-display-coverage.mjs`, `node --test scripts/tests/evidence-files.test.mjs scripts/tests/reviewed-catalog-importer.test.mjs`, `node frontend/src/features/crafting/base-picker/collectBaseImageSources.mjs --check`로 현재 입력·도구 경로·이미지 출처를 확인한다. `--check` 없는 이미지 도구는 인덱스를 갱신하므로 읽기 검사와 구별한다.
