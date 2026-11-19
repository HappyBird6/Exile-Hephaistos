# Exile-Hephaistos 개발 지침

## 운영

Git 전용 채팅은 연결된 Git 통합 절차도 따른다. 사용자 명시 지시가 이 문서보다 우선한다.
설명은 한국어로 하고 클래스·패키지·기술명은 영어를 유지한다. 사용자 승인 정책에 따라 제품 UI는 English, 한국어, 简体中文, 繁體中文, 日本語, Español, Русский, ไทย의 8개 언어를 지원한다.
사용자가 알 필요 없는 제약·제한 사항을 사이트 UI에 적어 넣지 않는다. 개별 개발상 설명은 작업 채팅이나 개발 문서로 전달하며, AGENTS.md에는 이 UI 작성 규칙 자체를 기록한다. 필요한 사용 안내와 실제 오류·입력 검증·데이터 손실/안전 경고는 보존한다. 이 규칙은 이후 작업을 다른 채팅에 중계할 때도 포함한다.
작업 시작 시 branch·HEAD·미커밋 변경과 관련 파일을 확인한다. 전용 branch/worktree에서 자기 변경만 구현·검증·commit한다. 다른 작업의 파일·컨테이너·데이터를 임의 변경하지 않는다.

수정 전에 관련 코드를 읽고 변경할 사항과 검증 계획을 사용자에게 먼저 설명한다. 구현 후 자체 리뷰에서 발견한 문제와 검증 결과를 사용자에게 전달한다.

## 현재 범위

- React 제작 작업대: 재료 표시·검색·툴팁·선택·즐겨찾기·아이템 입력.
- Java 아이템 텍스트 분석: 영어 게임 복사 텍스트를 표시용 구조로 변환하며 원문·미해석 행·경고를 보존한다.
- `/admin`: 서버에 ADMIN_PASSWORD가 설정된 경우에만 활성화되는 비밀번호 보호 운영 대시보드.
- PostgreSQL·Redis 및 기존 JPA·jOOQ·Flyway 기반은 다음 작업을 위해 유지한다.
- 주석만 있는 제작 관련 준비 파일은 사용자가 다음 작업에서 사용할 것이므로 유지한다.
- 검토된 카탈로그의 ItemState·검증기·Workbench와 최대 6옵션 자동 탐색 API·UI를 제공한다. [README](README.md)를 참고한다. 표시용 파싱과 Workbench catalog 매핑은 별도 검증 경로다.

요청 없이 미래 기능·추상화·외부 연동·의존성·CI를 추가하지 않는다. 새 기능은 실제 요청 시 명세와 필요한 구현을 함께 작성한다.
게임 규칙은 [버전별 규칙](docs/v0.5.5/game-rule/), 실행·데이터 갱신과 현행 지원 요약은 [README](README.md)를 따른다. 과거 세부 명세는 docs/legacy에서 조회한다.

제작지원 결과 목록과 오버뷰는 작업대 히스토리·게시판의 세션 표시 구성요소를 재사용한다. 세션의 목표 달성 확률과 선택한 경로의 개별 결과 확률을 구분하고, 화폐 후보와 제거 대상은 작업대 적용 규칙과 대조한다.

## 데이터와 동작

- 게임 효과·확률·weight·tier 의미를 추측하지 않는다. 근거가 없으면 미지원과 필요한 증거를 명시한다.
- 표시용 파싱 결과는 catalog 미검증 데이터다. Workbench는 catalog 검증 후 시뮬레이션 상태를 변경하며 실제 게임의 화폐를 소모하지 않는다.
- UI에서 원문 값을 임의 보정하거나 미해석 옵션을 삭제하지 않는다. 이미지·툴팁 출처와 라이선스를 유지한다.
- 비밀값·개인 아이템 원문을 로그나 Git에 남기지 않는다. `.env`와 기존 DB volume·로컬 원본은 보존한다.
- 이미 적용된 migration SQL을 수정·삭제하지 않는다. 기존 SQL의 사용하지 않는 schema/table은 실행 기능이 아닌 이력이다. 데이터 삭제나 DB 초기화는 별도 명시적 요청 없이 수행하지 않는다.

## 구현 기준

Java 21, Spring Boot 3.x, Gradle Wrapper, React, TypeScript strict, Vite를 사용한다. 기존 잠금 파일과 formatter를 따른다.
Backend Controller는 입력·응답을 담당하고 파싱 로직은 서비스에 둔다. 오류는 Problem Details로 반환하며 내부 예외·SQL·비밀값을 노출하지 않는다.
Frontend는 서버 응답을 TanStack Query, 편집 입력을 Zustand에서 관리한다. 늦은 응답 폐기·취소·키보드 조작·label·focus 복귀를 유지한다.
사용하는 모듈 경계와 순환 금지는 ArchUnit으로 검증한다. 준비 파일을 채우기 위해 빈 구현을 추가하지 않는다.
JPA/jOOQ는 동일 DataSource와 transaction을 사용한다. codegen은 migration을 적용한 임시 DB에서만 실행한다.
Docker 서비스는 localhost에만 노출한다. 관리자 API는 비밀번호 인증과 요청 제한을 적용한다. 운영에서는 별도 비밀번호와 HTTPS를 설정한다.

## 검증과 완료

- Backend: `./gradlew.bat check generateJooq bootJar` (Unix는 `./gradlew`). `check`는 formatter·unit·ArchUnit·Docker 통합 검사를 포함한다.
- Frontend: `npm ci`, `npm run lint`, `npm run typecheck`, `npm run format:check`, `npm run test -- --run`, `npm run build`.
- Windows 실행 스크립트 변경: `python -m unittest discover -s scripts/tests -v`. 이 Python은 개발 스크립트 테스트용이며 앱 실행에는 필요 없다.
- Compose 설정 검사: `docker compose --env-file .env -f infra/compose.yaml --profile stack config --quiet`. 비밀값이 출력되는 일반 config 결과는 공유하지 않는다.

변경 영역·직접 영향 영역을 검증한다. 단순 문서 수정에는 불필요한 테스트를 추가하지 않는다. 검증 실패를 skip이나 기대값 완화로 숨기지 않는다.
구현·테스트·자체 리뷰·commit·최종 반영을 구분한다. 실행하지 못한 검증은 이유와 함께 보고한다. Git 반영은 운영 명세의 경계를 따른다.

## 제작 상태와 반복 제작의 변경 계약

신규 화폐·메커니즘·상태 필드는 단일 제작만 검증하고 완료하지 않는다. 일반/훼손 제작, 카오스 반복, 저장·재로딩, 구버전 세션 이어하기, 아이템 표시·텍스트·스냅샷의 직접 영향 범위를 확인한다.

아이템 JSON 기본값은 `shared/item/ItemStateWireDefaults`에서 공통 처리한다. 선택적 boolean 상태 플래그는 생략 시 false이고, 명시적 null/문자열은 거절한다. 반복 API만의 아이템 필드 예외나 기본값 목록을 추가하지 않는다. 새 필드의 기본값이 false가 아니거나 수치·객체라면 과거 저장 상태의 의미를 먼저 정하고 공통 읽기 계약에 명시한다.

상태 필드나 JSON 직렬화 정책 변경 시 실제 Spring 직렬화 결과를 단일·반복 제작의 공통 읽기 경로에 재입력하는 계약 검사를 수행한다. 카오스 반복은 일반/훼손 양쪽, 상태 필드 생략·명시·잘못된 타입, 최대 1,000회 실행, 처리 후 상태 보존을 포함한다. 규칙/지원 범위/표시 정책 변경은 관련 버전·해시·현재 안내 문구도 함께 검토한다.


## 문서와 데이터의 단일 관리 기준

- 게임 JSON·번역의 편집 원본은 `docs/v0.5.5/game-data/`, `i18n/`이다. 게임 규칙은 `game-rule/`의 MD에 기록한다. backend/frontend의 대응 JSON은 manifest에 명시한 호환용 복사본이며 직접 편집하지 않는다. 새 데이터 파일은 해당 원본 폴더에 추가한다. manifest의 파일 목록과 실행 경로는 폴더 규칙으로 자동 생성하며 직접 편집하지 않는다.
- 런타임 불변 데이터는 공유하고 패치 가능한 제작 정책은 JSON에서 읽는다. API의 짧은 ID는 동일 사전 협상을 전제로 HTTP 경계에서만 변환한다. 내부·저장 ID와 카오스 반복의 원문 바이트 계약은 보존한다. 전송 사전은 기존 매핑에서 생성하고 서버·프런트 사본을 직접 편집하지 않는다. 기존 게임 값·배열 순서·원문·기록을 보존한다. 실행 자원 목록 변경 시 ruleset 버전·seal과 현행 검증 사례를 검토하되 과거 호환 ID를 현재 값으로 덮어쓰지 않는다.
- catalog는 `장비종류/베이스/`로 구성한다. 공통 정의는 `game-data/modifiers/<종류>.json`, 공통 옵션 목록은 각 장비 종류의 `modifier-pools.json`에서 관리한다. 기존 문맥 차이는 variants·snapshot 목록에 보존하고 임의로 통일하지 않는다. 베이스 정체성은 각 `base.json`, 옵션 연결은 `catalog.json`에서 관리하며 실행 JSON·개수·가중치 합계는 자동 생성한다. raw는 `docs/legacy/raw-data/`에 보관하고 서버·배포 자원에서는 제외한다.
- `mappings/`의 ID는 종류별 접두사와 base36 일련번호다. 발급 후 재할당·재사용하지 않는다. 제거는 retired tombstone으로 유지하며 기존 매핑을 삭제한 뒤 재생성하지 않는다. 다음 데이터 버전도 이전 매핑을 승계한다. 문자열이 같다는 이유로 다른 종류를 합치지 않는다. 게임 이름·설명은 i18n에서 유지한다.
- `node scripts/versioned-game-data.mjs --write` 한 명령으로 ruleset digest·ID·manifest·runtime/browser 복사본·공개 계약·README 요약을 순서대로 갱신한다. engine/ledger/ruleset 의미 버전은 먼저 검토한다. `--check`는 읽기 전용이다. MD 문구 변경에는 생성이나 해시 갱신이 필요 없다. 복사본을 수정해 drift를 숨기지 않는다.
- 일반 관리 안내는 README, 에이전트 규칙은 AGENTS.md에 통합한다. 새 작업 계획·완료 보고·인계·별도 유지보수 문서를 만들지 않는다. 게임 변경은 관련 JSON·규칙 MD만 갱신하고 중복 목록·개수·매핑 digest는 자동 생성한다.
- 기존 문서·HTML·검증·라이선스는 `docs/legacy/`에 보관한다. 사용자 재평가와 삭제 지시 전에는 제거하지 않는다. 과거 자료는 수집 당시 증거이며 현재 규칙의 원본으로 재생성하거나 덮어쓰지 않는다. 봉인된 JSON의 옛 `docs/...` 출처는 현재 `docs/legacy/...`에서 조회한다.
- API 제한·지원 언어의 정책 원본은 `backend/src/main/resources/contracts/`, 운영 값은 `backend-settings.yaml`이다. frontend mirror·Nginx 제한·README 현행 요약은 생성 결과다. 게임 알고리즘을 범용 JSON 실행기로 바꾸지 않는다.
- 등록은 구현의 증거가 아니다. 신규 화폐·징조 trigger 및 상태 필드는 strict loader, 단일/반복 제작, 일반/훼손, 저장·표시·확률의 영향을 검증한다. 과거 호환 digest·snapshot을 임의로 현재 값으로 덮어쓰지 않는다.

## 백엔드 리팩토링 추가 기준

- 현재 단계는 백엔드 코드·메모리 경량화다. 동일한 불변 카탈로그·옵션 정의·검증 자료는 번들 범위에서 공유하고 같은 파일을 반복 로딩하지 않는다. 사용자별 가변 상태는 공유하지 않으며 입력 조합 캐시는 크기를 제한한다. 객체 수·할당량 또는 힙 측정으로 개선을 확인한다.
- 하드코딩된 게임 데이터는 우선 점검·보고한다. 이 중 에센스·증강물·합금 등 제작 화폐의 패치 가능한 대상·수치·후보·제한은 JSON 원본으로 이동한다. 알고리즘·상태 전이는 Java에 둔다. 검증을 없애거나 지원하지 않는 메커니즘을 허용해 데이터 편집 문제를 숨기지 않는다.
- 직접 관리하는 각 Java 타입 상단에 한국어로 역할과 구성원 설명을 작성한다. 필드·상수·생성자·모든 메서드·중첩 타입·record 구성요소·enum 값·초기화 블록을 설명하고, 메서드 입력·결과·예외·변경 상태 및 지역변수 역할은 필요한 내부 주석으로 보충한다. 자동 생성 코드는 직접 편집하지 않는다.

## 직접 수정하기 쉬운 구조

- 경량화는 데이터 크기뿐 아니라 사람이 이해하고 수정하는 비용을 줄이는 작업이다. 기능 변경 시 찾아갈 파일·중간 계층·중복 원본을 줄이고 역할이 분명한 이름과 폴더를 사용한다. 단순한 작업에 범용 실행기·플러그인 구조·추상화를 추가하지 않는다.
- 현재 사용되는 공통 API schema·FE/BE 검증 사례는 `docs/contracts/`에 둔다. 게임 버전 데이터와 섞지 않는다. 설명용 README는 legacy에 보관하고 새 관리 설명은 루트 README에 통합한다. backend resources의 contracts는 실제 로드되는 정책 파일이며 별도다. 사용하지 않는 schema와 과거 버전에 고정된 fixture는 legacy/contracts에 보존한다. 현행 source fixture의 해시 갱신은 목표·옵션·확률 oracle 검증 후 수행한다.

- modifier 원본은 획득 종류별 implicit/normal/essence/desecrated/augment/alloy/liquid로 구분한다. 같은 분류·family·태그·stat 구성이 같은 티어만 groups로 묶고 단일·미확정 옵션은 definitions에 둔다. tiers의 키는 기존 modifier ID이며 그룹 이름·티어 번호를 ID로 대체하지 않는다. 복합 ranges의 길이·순서는 statIds와 일치해야 한다. 에센스가 일반 옵션을 부여하면 normal 정의를 참조하고 복제하지 않는다. variants는 원래 정의와 같은 분류에 둔다. 원본 구조만 바뀌고 생성 bytes가 같으면 ruleset·과거 snapshot 버전을 불필요하게 올리지 않는다.
- 생성 데이터 구조: 베이스 인덱스·주얼·공개 옵션·표시 JSON은 공통 원본에서 생성한다. display 별도 원본을 복원하지 않는다. 증강물·에센스·합금·감정액은 crafting 하위에서 관리하고 이름·툴팁·옵션 문장은 i18n에 둔다. 재료 적용 대상은 명시적 target-classes에서 읽으며 번역 문자열을 파싱하지 않는다.
- 출처 URL은 sources.json의 sourceRef로 연결한다. 기존 참조를 재사용하지 않는다. 분류 사실·앱 지원 정책·과거 호환 ID를 분리하고 확률 미확정은 null로 표현한다. 동일 목록만 공유하며 등록 order·옵션 순서·문맥별 variants를 보존한다. proof/review는 legacy/raw-data에서 테스트만 읽으며 배포하지 않는다.

## Git 통합

작업 브랜치에서 자기 변경만 검증·commit하고, 작업 완료 시 항상 해당 브랜치를 원격에 일반 push한다. 실패한 검증은 결과와 원인을 숨기지 않고 보고한다. master 최종 통합은 사용자가 지정한 Git 채팅이 전달받은 브랜치·범위를 순차 처리한다. 별도 인계 파일은 만들지 않고 채팅에 기준 SHA·완료 commit·검증·남은 문제를 보고한다. master 반영은 로컬 merge, merge·push는 확인된 원격으로의 일반 push까지다. 작업 브랜치 push는 기본 작업 범위이며 master 병합·master push·배포는 별도 지시를 따른다.
통합 전 최신 대상 SHA·작업 트리·의존성·diff·검증 결과를 확인하고, 대상 변경 시 결합 영향을 다시 검증한다. 필수 검사 불가·차단 결함은 숨기지 않는다. 강제 push·다른 작업 폐기·기존 DB/volume 삭제는 하지 않는다. 상세 과거 절차는 docs/legacy/solo-workflow/GIT_INTEGRATION.md에 보관한다.
