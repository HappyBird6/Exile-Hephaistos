# AGENTS.md

## 0. 최우선: 1인 개발 운영 명세

**모든 작업은 먼저 `docs/solo-workflow/MULTI_SESSION_WORKFLOW.md`를 읽고 그 문서와 연결된 운영 문서를 기준으로 수행한다.** 이 절은 운영 명세를 요약할 뿐이며, 충돌하거나 해석이 다르면 해당 운영 명세가 우선한다.

프로젝트 문서의 우선순위는 **사용자 명시 지시와 상위 시스템·보안 정책 → `docs/solo-workflow/MULTI_SESSION_WORKFLOW.md` 및 연결된 운영 문서 → `AGENTS.md` → `docs/TECHNICAL_SPEC.md`(없으면 루트 `TECHNICAL_SPEC.md`) → 승인된 ADR/세부 명세**다. 제품 요구사항은 운영 기본값으로 변경하지 않는다. `CODEX_SESSION_GUIDE.md`는 이전 운영 방식의 참고 자료이며 역할·작업 순서의 실행 근거로 사용하지 않는다.

1. 관리자는 요구사항 정리·기능 세션 생성/재사용·배분·검증 증거 확인·독립 리뷰 조율·Git 통합을 맡는다. 코드·테스트·설정·문서의 직접 작성·수정은 금지하며, 사소한 변경도 기능 세션에 위임한다. 공용 운영 기록 작성과 통합 검증 실행은 관리자 역할에 포함한다. 상시 Test/Git 세션이나 채팅 이름 기반 역할 판별은 요구하지 않는다.
2. 구현이 필요한 요청은 별도의 기능 세션(worker)을 생성하거나 기존 담당을 재사용해 구현·필요한 테스트 작성/수정/실행·자기 변경 commit을 위임한다. 동시 구현 작업자는 최대 2개이며, 작업자는 추가 에이전트를 만들지 않는다. 실제 도구 정책상 위임할 수 없으면 BLOCKED로 보고하며 관리자 직접 구현으로 대체하지 않는다.
3. 역할은 사용자 또는 관리자의 최초 위임으로 정하고 실제 세션 ID에 연결한다. 이름은 표시용이며 역할을 역추론하는 근거가 아니다.
4. 기존의 앱 제공 공간이나 자기 소유 작업 공간이 있으면 상태와 소유권을 확인해 재사용한다. 공용 checkout에서 병렬 쓰기가 필요할 때만 작업자별 branch/worktree와 파일 소유권을 분리한다. 사용자 변경을 임의로 이동·폐기하지 않는다.
5. 구현 담당은 필요한 테스트의 작성·수정·실행과 검증·리뷰 결함 수정을 맡는다. 관리자는 통합 검증을 실행할 수 있지만 코드·테스트를 직접 고치지 않으며, 내용 판단이 필요한 충돌 해결도 기능 세션에 위임한다. 변경 영역과 직접 의존 영역을 우선 검증하고, 영향이 넓거나 프로젝트 필수 절차이면 전체 검증을 선택한다. 전체 검증에 별도 사용자 지시를 기본 요구하지 않는다.
6. 일반·고위험 변경은 독립 리뷰를 사용하고 사소한 변경은 근거를 남겨 생략할 수 있다. 고위험 변경에 독립 리뷰 수단이 없으면 가능한 구현·검증을 마친 뒤 반영을 차단하고 사용자 판단을 요청한다.
7. 검증 또는 리뷰에서 발견한 범위 내 결함은 작업 전체 최대 2회까지 수정·재검증한다. 범위 확대, 반복 무진전, 중요한 요구사항 모호성, 비용·파괴적 작업 또는 승인 범위 밖 변경은 사용자에게 보고한다.
8. 기본 완료 범위는 작업 브랜치의 로컬 commit이다. PR·최종 브랜치 반영·push·배포는 `SETUP.md`에 저장된 승인 정책과 실제 도구 승인 범위에서만 수행한다.

이 파일은 PoE2 AI 아이템 제작 의사결정 지원 프로젝트의 개발 지침이다. 한국어로 설명하고 클래스·패키지·기술명은 영어를 유지한다. 상세 설계는 `docs/TECHNICAL_SPEC.md`, 해당 파일이 없으면 루트 `TECHNICAL_SPEC.md`를 읽는다. 문서 이동은 문서 담당의 명시된 작업으로 수행하며 모든 세션이 자동으로 이동하지 않는다. 확인하지 않은 문서 내용을 추정하지 않는다.

## 1. 프로젝트 목적과 절대 원칙

현재 아이템과 사용자 목표를 바탕으로 가능한 제작 선택지의 확률·위험·비용·후속 방향을 제시한다. 사용자가 자신의 예산과 성향에 맞게 선택하도록 지원하며 실제 게임 조작·거래·화폐 사용은 실행하지 않는다.

1. **사용자 선택을 우대한다.** 직접 입력·수정·프리셋 선택을 제공하고 자연어 입력을 강요하지 않는다. 낮은 확률의 Action도 근거와 함께 비교할 수 있어야 한다.
2. **Crafting Engine이 규칙·확률의 최종 권위다.** AI는 자연어 해석·목표 초안·도구 호출·설명만 맡는다. LLM 지식·생성 숫자·자체 추론을 확률이나 화폐 효과의 근거로 쓰지 않는다.
3. **게임 규칙을 invent하지 않는다.** Modifier 효과·tier·weight·슬롯 제한·Currency 효과·시즌 변경의 데이터/명세가 없으면 TODO와 필요한 증거를 남기고 해당 계산을 지원하지 않는다. 결과에 영향을 주는 모호함은 질문한다.
4. **같은 입력과 버전은 같은 계산을 만든다.** data/rule/engine/price 버전을 보존한다. 가격 없음과 0, 미지원과 적용 불가, 추정과 정확을 구분한다.
5. **작게 변경하고 근거로 검증한다.** 요청 범위와 관련 없는 구조 변경·dependency 추가·인프라 도입을 하지 않는다. 기존 사용자 변경을 덮어쓰지 않는다.

## 2. 처음 열었을 때

1. 먼저 0절의 운영 명세와 현재 디렉터리, Git 상태, 작업 경로의 추가 지침, README, 관련 기술 명세·ADR·build/lockfile을 자신의 업무에 필요한 범위에서 읽는다. 실행 명령 확인은 실행 허가가 아니다.
2. 변경할 모듈과 공개 계약을 특정한다. 게임 동작 변경이면 `docs/supported-mechanics.md`, `docs/data-sources.md`, 해당 snapshot fixture의 출처부터 확인한다.
3. 구현 담당은 버그 재현 조건, 새 규칙의 출처와 적용/실패 조건, 필요한 fixture·검사 항목을 기록하고 필요한 테스트를 작성·실행한다.
4. 자신의 소유 범위만 구현한다. schema/API/설정 변경으로 다른 담당의 migration/OpenAPI/문서 갱신이 필요하면 관리자에게 알리고 순차 처리하거나 분리된 소유권으로 협업한다.
5. 완료 보고에는 바뀐 동작, 변경 파일·commit, 검증·리뷰 결과와 미검증 항목, 남은 TODO를 적는다. 구현·통합·테스트·리뷰·Git 반영 상태를 구분하며 실행하지 않은 검증을 통과했다고 말하지 않는다.

아직 저장소가 scaffold되지 않았으면 아래 구조와 명령은 **구현 목표**다. 없는 script나 endpoint가 이미 존재한다고 보고하지 않는다. 실데이터 없이 adapter 계약 구현과 synthetic 테스트는 배정된 구현 범위에서 수행할 수 있다.

## 3. 현재 MVP

| 포함 | 제한 |
|---|---|
| 한 시즌·패치의 검증된 catalog | 실제 지원 Base/Modifier/Currency 목록은 `docs/supported-mechanics.md`에 명시 |
| 직접 설정·아이템 텍스트·AI 초안 | 미해석 옵션을 조용히 삭제하지 않음; 초안은 사용자가 수정·확인 |
| 목표 제작·쓸만한 아이템 만들기 | 쓸만함은 curated 옵션 목록/프리셋에서 선택; 임의 메타 점수 없음 |
| 단일 단계 Action 비교·실제 결과 재입력 | 전역 최적 경로·모든 화폐·모든 특수 mechanic 지원을 약속하지 않음 |
| 개인 preset·로그인·가격·AI 보조 | 직접 계산은 익명 가능; 가격·AI 장애 시에도 직접 입력 계산 유지 |

후속 범위는 GGG 캐릭터 import, 데이터 확보 후 메타 통계, 제한 경로 탐색, Monte Carlo, 프리셋 공유다. 전체 유저 장비 수집·희귀 아이템 시세 예측·판매 수익 보장은 MVP가 아니다. 지원 대상과 동작을 넓히기 전에 명세와 데이터 검증 기준을 갱신한다.

## 4. 기술 스택과 저장소

| 영역 | 사용 기술 |
|---|---|
| Backend | Java 21, Spring Boot 3.x, Spring MVC, Spring Security, Gradle Wrapper |
| Data | PostgreSQL, JPA/Hibernate, jOOQ, Flyway, Redis |
| Frontend | React, TypeScript strict, Vite, TanStack Query, Zustand |
| AI / ETL | OpenAI API tool/function calling + Structured Outputs / Python, BeautifulSoup, pandas |
| 검증·배포 | JUnit5, AssertJ, Testcontainers, ArchUnit, Vitest/Testing Library, pytest, Docker/Compose; AWS 후속 |

Boot는 요청된 3.x를 유지하고 정확한 patch·BOM·jOOQ Java/edition 호환성을 검증해 고정한다. 자동으로 major version을 올리지 않는다. npm/Python dependency도 lockfile로 고정하고 기존 package manager를 존중한다. 신규 scaffold의 기본값은 npm, Python은 pyproject와 lockfile 기반 관리다.

JPA는 aggregate 저장과 단순 CRUD, jOOQ는 소유 모듈의 복잡 SQL 조회용이다. QueryDSL 대체는 JPA 중심 쿼리로 충분하며 선택 배포판의 Jakarta/processor 호환성과 유지보수를 검증한 경우 ADR로 결정한다. JPA+jOOQ+QueryDSL 세 도구를 동시에 추가하지 않는다.

```text
backend/src/main/java/com/poe2craft/
  bootstrap/
  shared/
  item/ modifier/ currency/ currencyrule/ season/
  crafting/ preset/ ai/ user/ price/
    api/
    domain/
    application/port/
    application/service/
    infrastructure/
    presentation/
backend/src/main/resources/db/migration/
backend/src/test/java/com/poe2craft/
frontend/src/{app,features,shared}/
data-pipeline/src/poe2etl/
data-pipeline/tests/fixtures/{synthetic,verified}/
infra/compose.yaml
docs/{TECHNICAL_SPEC.md,supported-mechanics.md,data-sources.md,adr/}
```

단일 Spring Boot 애플리케이션의 Modular Monolith다. 필요한 패키지만 생성한다. Python은 별도 배치이며 실시간 AI 서버가 아니다. 내부 모듈 통신을 HTTP로 만들지 않는다.

## 5. 모듈 책임과 허용 의존성

아래 표는 직접적인 **타 모듈** import 허용 목록이다. 각 모듈은 필요 시 최소 `shared` 계약에 의존할 수 있다. 표에 없는 의존성은 금지한다.

### 카탈로그

| 모듈 | 책임 | 허용 import |
|---|---|---|
| `season` | Season/Patch/League, snapshot manifest와 활성 포인터 | 없음 |
| `modifier` | Modifier/Tier/Stat/Tag/Group/weight 자료 | `season.api` |
| `item` | Base, 불변 ItemState 계약, 텍스트 파싱·검증 | `season.api`, `modifier.api` |
| `currency` | 화폐 ID·표시 정보·버전별 존재 여부 | `season.api` |
| `currencyrule` | 적용 조건·효과·weight 정책의 선언과 handler key | `season.api`, `currency.api`, `modifier.api` |

### 유스케이스

| 모듈 | 책임 | 허용 import |
|---|---|---|
| `price` | 공식 economy adapter·가격 snapshot·freshness·환산 | `season.api`, `currency.api` |
| `crafting` | 상태 전이·확률·목표 판정·비용 비교 | `item.api`, `modifier.api`, `currency.api`, `currencyrule.api`, `season.api`, `price.api` |
| `preset` | curated 옵션·Target template·개인 preset | `item.api`, `modifier.api`, `season.api` |
| `ai` | 목표 초안·tool dispatcher·근거 기반 설명 | `item.api`, `modifier.api`, `currency.api`, `preset.api`, `crafting.api`, `season.api`, `price.api` |
| `user` | 사용자·외부 identity·OAuth token lifecycle | 없음 |

### 금지 의존성 및 enforcement

1. 타 모듈의 repository/JPA entity/jOOQ table/`domain`/`application`/`infrastructure`를 import하거나 테이블을 직접 조회·수정하지 않는다. 공개 `<module>.api` interface와 immutable DTO를 사용한다.
2. 내부 방향은 `presentation → application → domain`, `infrastructure → port/domain`이다. composition root만 구현을 연결한다. 공개 DTO에 JPA entity, lazy collection, infrastructure type을 노출하지 않는다.
3. `crafting.domain`은 Spring/DB/Redis/HTTP/OpenAI에 의존하지 않는다. `crafting → ai/preset/user`, `currencyrule → crafting/item`, `user → preset` 방향은 금지다. 규칙 선언은 currencyrule, ItemState에 대한 실행은 Crafting이 맡는다.
4. `CurrentActor`는 shared 계약이며 User가 인증 context로 채운다. Preset·AI는 actor를 전달받아 권한을 검사한다. 순수 Engine에는 사용자 서비스가 필요 없다. Preset을 Target DTO로 전개하는 작업은 Engine 밖에서 한다.
5. ArchUnit으로 순환과 공개 API 우회 import를 차단한다. 타 모듈 SQL join이 필요해 보이면 먼저 공개 query DTO 조립을 사용한다. read projection 예외는 소유권·정합성·갱신 전략을 ADR로 승인한 뒤 도입한다.

offline publisher는 여러 catalog의 무결성을 검증할 수 있다. 이 권한은 batch 계정과 해당 작업에만 한정한다. 런타임 모듈 경계 우회의 근거로 쓰지 않는다.

## 6. Crafting Engine 구현 규칙

### 입력과 실행

1. `ItemState`는 불변이며 입력 객체를 수정하지 않는다. `item.api`의 순수 Java record 계약을 사용하고 persistence entity와 분리한다. Target은 필수 조건·대체 조건·보존 조건의 AND/OR 의미를 명시한다.
2. application에서 snapshot 하나를 pin하고 카탈로그를 읽어 순수 `EvaluationContext`로 변환한다. Engine은 이미 준비된 데이터만 받는다.
3. 적용 가능성 → 후보 풀 → 전이 분포 → Target/보존 조건 평가 순서로 실행한다. 적용 불가에는 reasonCode를 반환한다.
4. Base/level/tag/group/슬롯/roll/화폐 조건은 검증된 규칙에 따른다. tier 숫자 크기만으로 우열을 판단하거나 모든 roll이 균등분포라고 가정하지 않는다.
5. unknown mechanic·누락 weight·미지원 handler는 `UNAVAILABLE` 또는 명시적 도메인 오류로 처리한다. uniform weight, 0%, 100%, 성공한 척하는 stub으로 대체하지 않는다.

### 정확성과 재현

| 항목 | 필수 규칙 |
|---|---|
| 결정성 | canonical input + snapshotId + ruleSetVersion + engineVersion + options 고정; stable ID 정렬 |
| 비용 재현 | priceSnapshotId·quoteCurrency·평가 시각도 고정; 최신 가격을 Engine 안에서 읽지 않음 |
| 수치 | weight overflow 방지; rational/BigDecimal로 확률 계산; BigDecimal 가격; 표시 시 반올림 |
| 추정 | Monte Carlo는 후속 범위. 도입 시 seed/PRNG/표본 수/신뢰구간/추정 표시 필수 |
| 부작용 | domain에서 DB/네트워크/현재 시각/전역 난수/LLM 호출 금지 |

`c/p` 기대비용은 동일 시작 상태로 독립 반복하고 회당 비용이 일정하다는 조건이 성립할 때만 사용한다. 그 외에는 재시작·종료 정책을 모델링하거나 unavailable로 둔다. 성공 확률과 보존 조건 훼손 확률은 겹칠 수 있으므로 합이 100%라고 가정하지 않는다. 제한 탐색 결과를 전역 최적이라고 표시하지 않는다.

synthetic fixture에는 실제 game ID를 쓰지 않는다. fixture의 임의 weight는 테스트 전용이며 production seed로 복사하지 않는다. 실제 규칙 fixture는 source URL/hash, patch/snapshot, 검토 기록을 포함한다.

## 7. 데이터 버전과 외부 소스 정책

### 버전관리

1. Season/Patch/League를 구분한다. catalog/규칙은 `snapshotId`, 가격은 독립 `priceSnapshotId`로 pin한다. 같은 patch의 데이터 정정도 새 snapshot을 만든다.
2. Currency는 ID/표시 정보, CurrencyRule은 적용 조건·효과 선언이다. ruleSetVersion/handlerVersion/engineVersion을 분리한다. snapshot에 필요한 handler가 없으면 publish를 거부한다.
3. published snapshot을 덮어쓰거나 hard delete하지 않는다. 활성 포인터만 transaction으로 교체한다. 실행 중 요청은 기존 pin을 유지한다.
4. 기존 handler의 검증된 parameter 변경은 data release, 새 mechanic은 Java handler + 테스트 + code release다. AI 요약을 자동 production 규칙으로 승격하지 않는다.
5. preset·계산 결과에 사용 버전을 남긴다. 시즌 전환은 호환성을 검증하고 사용자에게 변경을 보여준다. 같은 이름으로 다른 시즌 ID를 암묵 매핑하지 않는다.

### 외부 소스

| 소스 | 반드시 지킬 정책 |
|---|---|
| poe2db | 정적 Python ETL만. 수집·재배포 조건 확인, 원본/URL/시간/hash 보존. 접근 차단 우회 금지 |
| poe.ninja | [공식 문서](https://poe.ninja/docs/api)에 공개된 economy API만. builds/profiles/character/PoB/auth 내부 endpoint 사용·역공학 금지 |
| GGG | [공식 문서](https://www.pathofexile.com/developer/docs)에 있는 API만. undocumented/internal endpoint reverse engineering 금지. PoE2 지원과 scope 확인 |
| 운영자 curated 목록 | 사용자가 고를 옵션과 preset을 편집할 수 있지만 게임 효과/weight는 증거 없이 생성 금지 |

poe.ninja는 backend adapter에서 호출하고 User-Agent/contact, ETag, Cache-Control, rate limit을 존중한다. 문서화된 category·host·path를 allowlist로 제한한다. 이름만으로 통화 매칭하지 않고 검증된 external ID mapping을 쓴다. Rare 아이템 판매가가 economy API에서 제공된다고 가정하지 않는다.

외부 API가 실패하면 mock 값을 실가격으로 보여주지 않는다. source/observedAt/freshness를 보존하고, 가격이 없어도 화폐 수량과 규칙 확률은 제공한다. 전체 PoE2 캐릭터 수집이 가능하다고 가정하지 않는다.

### ETL 필수 단계

1. `raw`: 원본·출처·hash·parser version을 불변 저장한다.
2. `staging`: BeautifulSoup/pandas로 타입·단위·ID를 정규화하고 importRunId로 추적한다.
3. `validation`: FK/중복/범위/누락/handler/snapshot 일관성을 검사한다. 실패를 빈 목록 성공으로 처리하지 않는다.
4. `diff`: 추가/삭제/효과/weight 변경을 분리하고 급격한 행 감소는 파서 오류 가능성을 확인한다. raw 증거·회귀 결과와 함께 검토한다.
5. `production`: 승인된 immutable snapshot만 제한된 publisher로 승격한다. hash와 참조 검증 후 active pointer를 원자 교체한다. 실패 시 기존 release 유지.

ETL 계정은 production 쓰기 권한이 없다. import는 source/rawHash/parserVersion/targetPatch로 멱등성을 보장한다. rule DSL은 allowlist enum/handler만 사용하고 Python/SpEL/JavaScript 등 임의 코드를 data field에서 실행하지 않는다.

## 8. 코드 스타일·DB·API

### 코드 스타일

| 영역 | 지침 |
|---|---|
| Java | Java 21, constructor injection, final 우선, DTO는 record 권장, entity에 무분별한 Lombok `@Data` 금지 |
| Naming | package 소문자, class PascalCase, method/variable camelCase, domain 의미가 드러나는 이름 |
| Layer | Controller는 검증/변환/응답, application은 유스케이스·transaction, domain은 규칙; giant Service 금지 |
| TypeScript | strict, `any`/무근거 type assertion 금지, OpenAPI DTO 재사용, 접근성 label/focus 유지 |
| Python | 타입 힌트, parse와 IO 분리, fixture test, 명시적 결측값 처리; broad exception으로 실패 은폐 금지 |

기존 formatter/linter가 우선이다. 신규 scaffold에서는 Java formatter, ESLint/Prettier, Python Ruff를 선택해 CI에 고정한다. 코드 주석은 동작 반복보다 규칙 출처·제약 이유를 설명한다. 일반화는 실제 반복이 나타난 뒤 한다.

### DB와 transaction

1. Flyway만 schema를 변경한다. 이미 적용된 migration을 수정하지 않고 새 파일을 추가한다. 파일명은 전역 unique version과 모듈명을 포함한다.
2. Hibernate는 `ddl-auto=validate`, OSIV off. catalog key에 snapshotId를 포함하고 FK/unique/check로 가능한 무결성을 보장한다. 핵심 검색 필드를 모두 jsonb로 밀어 넣지 않는다.
3. 타 모듈 entity 연관·cascade 삭제를 금지한다. 불변 catalog 복합 FK는 허용하되 애플리케이션의 타 모듈 접근 권한을 의미하지 않는다.
4. jOOQ 생성은 migration을 적용한 임시 DB에서 한다. JPA/jOOQ는 동일 DataSource와 검증된 Spring transaction을 쓴다. 혼합 read-after-write는 flush/rollback 테스트를 추가하고 외부 API 호출 동안 transaction을 열어두지 않는다.
5. 데이터 손실 가능 migration/대량 삭제는 concrete diff·backup·복구 절차를 준비한 뒤 사용자 확인을 받는다. 로컬 수정 파일이나 DB volume을 임의 삭제하지 않는다.

### API와 에러

REST `/api/v1`, JSON, UTC ISO-8601, bounded pagination, OpenAPI 계약을 사용한다. 구현과 schema를 함께 수정한다. 정밀 확률/가격은 decimal 문자열로 전달하며 null을 0으로 바꾸지 않는다. 클라이언트가 보낸 ownerId/price/probability/rule text는 신뢰하지 않는다.

`@RestControllerAdvice`에서 Problem Details를 반환한다: `type/title/status/detail/instance/code/traceId`, optional `fieldErrors`. `400`은 문법, `422`는 지원하지 않는 입력/규칙, `409`는 버전 충돌, `429`는 rate limit, `503`은 사용 불가능한 의존성이다. 예상한 도메인 실패를 전부 500으로 만들지 않는다.

개별 Action의 적용 불가는 정상 비교 결과의 reason으로 반환한다. 데이터 부족은 `UNAVAILABLE`과 null이다. private resource는 owner를 확인하고 존재 여부 노출 정책을 일관되게 적용한다. DTO에 stack trace·SQL·provider raw error·secret을 포함하지 않는다. 저장 mutation은 optimistic version과 필요 시 idempotency key를 검토한다.

## 9. AI 구현 지침

[Function calling](https://developers.openai.com/api/docs/guides/function-calling)과 [Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs)의 현재 계약을 확인한다. 기본 설계는 Responses API adapter다. schema 적합성과 게임 의미 검증을 분리한다.

1. tool은 allowlist 기반 `search_modifiers`, `find_presets`, `evaluate_craft`, `get_calculation`, `get_prices`로 제한한다. actor·snapshot은 서버 context에서 가져오고 tool 인수로 권한을 확대하지 않는다.
2. `strict: true`, object의 `additionalProperties: false`, required/nullable 정책을 명시한다. 응답 schema 검사 뒤 catalog ID·수치·snapshot·owner를 다시 검증한다.
3. AI 목표는 draft다. UI에서 사용자 수정·확인 후 계산한다. `evaluate_craft`도 동일한 CraftingFacade를 사용하여 직접 입력 경로와 의미를 일치시킨다.
4. 확률·비용은 Engine 결과를 직접 표시한다. 설명은 calculationId/actionId/evidenceRefs에 연결한다. 생성 숫자가 다르거나 근거가 없으면 template 설명으로 대체한다.
5. refusal/incomplete/timeout/429를 처리한다. 초기 tool 한도 5회, 총 시간 30초를 설정하고 비용 예산을 적용한다. AI 실패 시 수동 입력·계산을 유지한다.

LLM에 SQL/임의 HTTP/shell/file write/게임 실행 tool을 제공하지 않는다. 사용자 텍스트·외부 HTML·preset 설명은 신뢰하지 않는 데이터다. prompt injection이 권한과 tool 목록을 바꾸지 못하게 한다. AI가 만든 rule code를 실행하지 않는다.

model/prompt/schema/tool 버전을 기록하고 모델 변경은 eval로 검증한다. `OPENAI_MODEL`은 설정으로 주입하며 임의로 모델을 교체하지 않는다. 기본 테스트는 mock으로 실행한다. 실제 API 호출은 프로젝트의 명시적 설정·비용 범위 안에서만 수행한다.

## 10. Frontend·캐시·관측성

| 영역 | 지침 |
|---|---|
| 상태 | TanStack Query가 서버 상태, Zustand가 입력 draft를 소유. 동일 서버 객체를 두 store의 원본으로 관리하지 않음 |
| 오래된 응답 | inputHash/revision 또는 취소 처리로 이전 계산이 최신 입력을 덮어쓰지 않게 함 |
| snapshot 변경 | 관련 query key 분리, draft 재검증, 이전 계산 stale 표시; 몰래 변환 금지 |
| 개인 정보 | 로그인 token을 localStorage에 저장하지 않음. 로그아웃 시 개인 cache 정리 |
| UX | 확률/위험/가격 출처·시점 표시, 사용자 선택 유지, AI 실패와 Engine 실패를 분리 |

Redis는 cache-aside이며 DB가 원본이다. 규칙 cache key에는 snapshotId/ruleSetVersion/engineVersion/canonical input/options를, 비용 key에는 priceSnapshotId/quoteCurrency를 추가한다. 개인 입력 결과는 사용자 namespace로 격리하거나 cache하지 않는다. 원문 prompt·token을 cache key로 쓰지 않는다.

초기 TTL 제안은 Modifier 24시간, 규칙 계산 1시간, 가격 latest pointer 15분, 비용 15분, 공개 preset 10분이다. 가격 polling은 초기 60분이며 외부 cache/rate-limit 정책이 우선한다. stale 한도 초기 2시간 초과는 비용 unavailable이다. 이 값은 실측으로 수정 가능하며 변경 이유를 기록한다.

Redis 장애 시 DB 우회와 보수적 rate limit으로 동작한다. Redis를 session 저장에 쓰게 되면 인증 fail-closed 정책을 따로 구현한다. lock은 stampede 완화용이며 규칙 정합성의 권위가 아니다. cache hit/miss 결과 동등성을 테스트한다.

SLF4J 구조화 로그에 traceId/errorCode/duration/version을 기록한다. 원문 대화·개인 아이템·token·API key는 기본 로그에서 제외한다. metric label에 userId/calculationId 등 고유값을 넣지 않는다. Actuator 상세 endpoint는 공개하지 않는다. readiness는 DB/활성 snapshot을 기준으로 하고 OpenAI 장애만으로 전체 서비스를 내리지 않는다.

## 11. 보안·OAuth·환경변수

1. Spring Security server session을 기본으로 한다. Secure/HttpOnly/SameSite cookie, CSRF, 정확한 CORS origin을 적용한다. OAuth state/PKCE, OIDC 사용 시 nonce/issuer/audience를 검증한다.
2. 로그인 공급자는 확정 전 TODO다. GGG 계정 연결은 별도 OAuth integration이며 문서화된 scope/client 조건을 확인한다. GGG의 OIDC 지원을 추정하지 않는다.
3. 개인 preset/계산은 요청마다 owner 검사한다. 관리자 publisher와 일반 사용자 권한을 분리한다. 익명 결과도 세션 접근 정책을 적용한다.
4. 외부 OAuth token은 server에 암호화 저장하고 refresh/revoke/삭제를 처리한다. SQL binding·HTML sanitize·입력 크기 제한·외부 host/path allowlist를 적용한다.
5. secret은 환경변수/secret store만 사용한다. `.env`, private key, token dump, 운영 DB dump를 commit하거나 출력하지 않는다. `.env.example`에는 placeholder만 둔다. 노출 발견 시 값을 재출력하지 말고 폐기·교체 필요를 보고한다.

### 환경변수 계약

| 그룹 | 변수 |
|---|---|
| DB | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` |
| Redis | `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT`; 인증/TLS는 배포 시 명시 |
| AI | `OPENAI_API_KEY`, `OPENAI_MODEL` |
| 앱·OAuth | `APP_PUBLIC_URL`, `APP_CORS_ALLOWED_ORIGINS`, `OAUTH_CLIENT_ID`, `OAUTH_CLIENT_SECRET`, `TOKEN_ENCRYPTION_KEY` |
| 외부 소스 | `POE_NINJA_USER_AGENT`; GGG 전용 변수는 실제 연동 시 추가 |

`VITE_*`는 브라우저 공개 값이다. OpenAI/OAuth/DB secret을 넣지 않는다. 필수 설정은 시작 시 검증하고 AI 미설정은 명시적 disable 상태로 처리한다. DB 접속 문자열에 secret이 포함되어 있으면 오류 로그에서 마스킹한다.

## 12. 테스트·명령·완료 조건

### 변경 범위에 따라 수행할 필수 테스트

이 절과 앞 절의 모든 테스트·회귀·fixture 요구는 제품의 검증 기준이다. 구현 담당은 자기 변경에 필요한 테스트 작성·수정·실행을 맡고, 관리자는 최종 영향 범위 검증과 필요한 독립 리뷰를 조정한다. 병렬 작업에서는 명시된 파일 소유권을 벗어나지 않는다.

| 변경 | 실행할 검증 |
|---|---|
| Engine/규칙 | JUnit5/AssertJ: 경계값, 분포 합·음수·overflow, 상태 불변성, deterministic 재현, missing data |
| DB/query | Testcontainers PostgreSQL: migration 신규/업그레이드, FK/unique, jOOQ/JPA transaction, 실제 query 결과 |
| cache/외부 adapter | Redis hit/miss/장애, HTTP fixture schema, timeout/429/stale handling; 운영 API 호출 없이 실행 |
| ETL/시즌 | pytest fixture, 중복/누락/diff, idempotency, publish 실패 복구, 이전 snapshot 결과 불변 |
| API/UI/AI | OpenAPI·권한, 초안 수정, late response, unsupported 안내, schema/refusal/injection/tool limit, AI fallback |

구현 담당은 모듈 변경에 관련 ArchUnit을 포함하고 버그 변경은 재현 조건으로 회귀 테스트를 작성한다. 실패하면 원인을 기록하고 범위 내 결함은 운영 명세의 수정 예산 안에서 수정·재검증한다. mock이 구현을 그대로 복제하는 테스트보다 관찰 가능한 동작을 검증한다. 게임 규칙 fixture는 출처를 포함하고 잘못된 fixture를 맞추려고 구현을 왜곡하지 않는다.

### 초기 scaffold에서 제공할 명령 계약

아래는 프로젝트가 생성된 후 제공할 표준 명령 계약이며 자동 실행 목록이 아니다. 구현 담당 또는 관리자는 실제 파일/script와 허용 범위를 확인하고 범위 검증 명령을 선택한다. 전체 검증은 프로젝트 필수 조건, 넓은 영향, 고위험 변경 또는 범위 검증이 불충분할 때 실행한다. 아직 실행되지 않은 명령을 성공한 검증으로 보고하지 않는다.

| 위치 | 명령 | 목적 |
|---|---|---|
| `backend/` | Windows `./gradlew.bat check`, Unix `./gradlew check` | unit + architecture + 별도 등록한 integrationTest를 check에 연결 |
| `frontend/` | `npm ci`, `npm run lint`, `npm run typecheck` | 잠긴 의존성·정적 검사 |
| `frontend/` | `npm run test -- --run`, `npm run build` | Vitest와 production build |
| `data-pipeline/` | lockfile 동기화 후 `python -m pytest` | 선택한 Python 환경에서 fixture 검증 |
| 프로젝트 루트 | `docker compose -f infra/compose.yaml config` | 환경변수 설정 후 Compose 구조 검사; secret 출력은 공유 금지 |

`integrationTest` task 연결은 프로젝트의 build 설정과 테스트 계약에 맞춰 구성한다. Docker 등 필수 환경이 없어 실행 불가하면 구현 완료 여부와 미검증 사유를 분리해 `BLOCKED` 또는 `NEEDS_INPUT`으로 보고한다. 비용 발생 live smoke나 production 변경은 기본 test에 포함하지 않는다.

### 완료 체크

1. 요청한 동작이 구현되어 있고 직접 입력/AI 경로가 동일 Engine을 사용한다.
2. 변경된 규칙·데이터는 출처와 버전을 갖추고 미지원 항목을 정상처럼 처리하지 않는다.
3. 구현 완료에는 필요한 API/schema/env 문서와 협업 commit이 포함된다. 관련 test/lint/typecheck와 필요한 리뷰 결과를 별도 증거로 확인하며, 구현 완료를 전체 검증 완료로 표시하지 않는다.
4. diff에 secret·generated noise·요청 밖 리팩터링·사용자 변경 삭제가 없다.
5. 관리자는 대상 SHA별 구현·통합·테스트·리뷰·반영 상태와 남은 TODO를 구분한다. 범위 내 결함은 수정 예산 안에서 처리하고, 범위 확대·반복 무진전·필수 검증 불가·사용자 판단이 필요한 경우에 중단해 보고한다. 별개 작업의 기존 변경을 임의 정리하지 않는다.

## 13. 인프라 확장 금지와 예외

| 기본 금지 | 이유 / 재검토 근거 |
|---|---|
| MSA | 독립 확장·팀 소유권·장애 격리 요구의 실측과 데이터 경계가 먼저 |
| Kafka | durable event replay·복수 consumer 요구가 먼저; 단순 batch는 CLI/job table 검토 |
| Kubernetes / K8s | 소수 container는 Docker Compose 또는 후속 관리형 container로 충분 |
| Elasticsearch | PostgreSQL index/query 개선 후에도 검색 요구가 충족되지 않는 증거 필요 |
| Vector DB | 의미 검색 eval에서 효과가 입증되어야 함; 단순 AI 연동은 도입 사유가 아님 |

새 인프라·모듈 분리는 요청 없이 도입하지 않는다. 필요하면 측정 결과, 단순 대안, 비용, 소유권, failure/rollback 계획을 ADR 초안으로 제시한다. 기존 명시적 승인 범위는 다시 묻지 않는다. AWS 배포는 후속 범위이며 문서에 이름이 있다는 이유로 유료 리소스를 생성하지 않는다.

## 14. TODO 작성 기준

`TODO(domain): 확인할 규칙 / 필요한 데이터 또는 공식 문서 / 영향받는 기능 / 현재 처리` 형식을 사용한다. 예: `TODO(domain): 특정 effect의 추첨 순서 근거 필요. 확인 전 해당 action은 UNSUPPORTED_MECHANIC.`

효과를 모른다는 TODO 옆에 정상처럼 작동하는 guessed 구현을 남기지 않는다. 명세상 미지원 항목은 명시적으로 표현한다. 요청 구현에 필요한 근거 부족으로 진행이 막히면 즉시 보고하며 다른 기능으로 작업 범위를 바꾸지 않는다. 제품 선택은 승인된 범위의 합리적 기본값으로 진행할 수 있지만 게임 사실·확률·외부 API 권한은 추측으로 확정하지 않는다.

## Project-specific additions

사용자가 개발하면서 추가할 영역이다. 미기입 항목은 확정값이 아니다. 새 항목에는 결정일·근거·영향 범위를 적고 기존 명세와 충돌하면 관련 문서를 함께 갱신한다. 기존 사용자 기입 내용을 삭제하지 않는다.

| 항목 | 사용자 추가 내용 |
|---|---|
| 첫 지원 시즌/패치/Base/Currency 및 검증 출처 | TODO |
| 실제 실행 명령·고정 버전·CI 환경 | TODO |
| OAuth 공급자·OpenAI 모델·사용 한도 | TODO |
| curated 옵션 정책·보존 기간·운영/배포 기준 | TODO |
| 추가 코드 규칙·ADR·팀 협업 방식 | TODO |
