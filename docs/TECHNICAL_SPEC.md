# PoE2 AI 아이템 제작 의사결정 지원 서비스 — 기술 명세서

문서 버전: 1.0 · 작성일: 2026-09-08 · 상태: 구현 기준 초안

이 문서는 기술·아키텍처 기준이며 실제 게임 규칙 데이터셋이 아니다. 사용자 요청으로 확정된 기술과 정책을 기준으로, 세부 구현 기본값을 제안한다. 아래 `제안`, `초기값`, `TODO`는 실측 또는 데이터 검증 후 수정할 수 있다. 게임 규칙·가중치·화폐 효과는 별도 출처와 검증 없이 확정하지 않는다. 이 파일은 프로젝트의 `docs/TECHNICAL_SPEC.md`, 함께 제공하는 `AGENTS.md`는 프로젝트 루트에 배치한다.

## 1. 목적과 제품 원칙

사용자가 현재 보유한 아이템 또는 원하는 목표 아이템을 입력하면, 가능한 제작 선택지의 성공 확률·목표 옵션 훼손 위험·예상 비용·후속 방향을 비교하도록 돕는다. 최종 선택은 사용자가 한다. 서비스는 실제 게임 조작이나 화폐 사용을 실행하지 않는다.

| 원칙 | 제품에 반영할 동작 |
|---|---|
| 사용자 선택 우대 | 목표·보존 옵션·예산을 직접 수정하고, 확률이 낮은 선택도 비교할 수 있다. 순위는 강제 실행이 아니다. |
| AI는 보조자 | 자연어를 구조화하고 검증된 결과를 설명한다. 확률과 화폐 효과는 Crafting Engine만 결정한다. |
| 입력 수단의 동등성 | 직접 설정, 아이템 텍스트, 자연어를 같은 `ItemState`와 `TargetItemState`로 정규화한다. |
| 불확실성 노출 | `EXACT`, `ESTIMATED`, `UNAVAILABLE`을 구분한다. 모르는 값은 0으로 표시하지 않는다. |
| 중간 제작 지원 | 깨끗한 Base뿐 아니라 기존 Modifier가 있는 상태에서 시작하고 실제 결과 입력 후 다시 계산한다. |

### 1.1 MVP 범위

1. 검증된 한 시즌·한 패치 데이터셋과 제한된 Base/Modifier/Currency 조합을 지원한다. 정확한 지원 목록은 데이터 검증 후 `docs/supported-mechanics.md`에 고정한다.
2. 직접 입력과 제한된 형식의 아이템 텍스트 파싱, AI 목표 초안 및 사용자 수정 UI를 제공한다. 파싱에 실패한 옵션은 누락하지 않고 확인 대상으로 남긴다.
3. 목표 제작과 쓸만한 아이템 만들기를 지원한다. 후자는 운영자가 등록한 사용 가능 옵션 목록·프리셋 중 사용자가 선택하는 방식이다. 옵션 점수 수동 입력은 필수가 아니다.
4. 다음 한 단계의 적용 가능 Action, 성공·보존 실패 확률, 화폐 소모량과 가능한 후속 방향을 비교한다. 프리셋 저장에는 로그인이 필요하고, 직접 계산은 익명으로 가능하다.
5. OpenAI 연동, 조건부 가격 연동, 버전이 고정된 계산 결과를 제공한다. API 장애 시 직접 설정과 규칙 계산은 계속 사용할 수 있다.

### 1.2 MVP 이후

| 기능 | 보류 이유 / 시작 조건 |
|---|---|
| 전체 유저 장비 수집·메타 학습 | 합법적으로 사용 가능한 공식 데이터 범위와 대표성 검증이 먼저다. API 제공을 가정하지 않는다. |
| GGG 캐릭터 가져오기 | 공식 PoE2 지원·OAuth 등록·scope 확인 후 추가한다. 서비스 로그인과 별도 연동이다. |
| 희귀 아이템 판매가·수익 예측 | currency 가격만으로 임의 Rare 아이템을 평가할 수 없다. MVP는 시세 기반 수익을 보장하지 않는다. |
| 전역 최적 제작 경로·Monte Carlo | 검증된 단일 단계 계산이 우선이다. 제한 깊이 탐색과 추정 계산은 별도 정확도 기준을 충족한 뒤 추가한다. |
| 공유·추천 통계 | 개인 프리셋이 먼저다. 공유 권한, 동의, 익명화, 표본 편향 처리를 정의한 뒤 확장한다. |

## 2. 기술 선택

### 2.1 애플리케이션

| 영역 | 선택 | 사용 기준 |
|---|---|---|
| Backend | Java 21 + Spring Boot 3.x + Spring MVC | 단일 애플리케이션, 명시적 도메인 모델, 동기 요청 중심. Java 21 toolchain을 고정한다. |
| 영속성 | PostgreSQL + JPA/Hibernate + jOOQ | JPA는 aggregate 저장, jOOQ는 복잡한 후보 검색·집계. Flyway로 schema를 관리한다. |
| Cache | Redis | 공개 가격·반복 계산 캐시부터 사용한다. 원본 DB를 대체하지 않는다. |
| Frontend | React + TypeScript + Vite | TanStack Query로 서버 상태, Zustand로 입력 중인 로컬 편집 상태를 관리한다. |
| AI / ETL | OpenAI API / Python + BeautifulSoup + pandas | AI는 Spring 내부 adapter, Python은 독립 실행 배치다. Python API 서버는 만들지 않는다. |

### 2.2 개발·운영

| 영역 | 선택 | 사용 기준 |
|---|---|---|
| Build | Gradle Wrapper, npm lockfile, Python lockfile | 제안 기본값. 정확한 버전은 첫 bootstrap에서 호환성을 검증하여 고정한다. |
| Backend test | JUnit5 + AssertJ + Testcontainers | PostgreSQL 실제 동작, Redis 장애·캐시, Flyway migration을 검증한다. |
| 구조·UI·ETL test | ArchUnit, Vitest + Testing Library, pytest | 모듈 경계, UI 확인 흐름, 파서 fixture 검증. 핵심 E2E에는 Playwright를 제안한다. |
| Runtime | Docker + Docker Compose | 로컬과 단일 호스트 배포를 일치시킨다. 외부 ETL은 batch profile로 실행한다. |
| Observability | Spring Actuator + Micrometer + 구조화 로그 | 운영 필요에 따라 Prometheus/Grafana를 붙인다. AWS는 후속 배포 대상이다. |

Spring Boot 3.5 계열 공식 요구사항을 참고해 Java 21과 호환되는 3.x patch를 선택한다. 이 문서는 특정 patch의 보안 지원 기간을 보장하지 않는다. 구현 시 지원 정책·BOM·jOOQ edition의 Java 요구사항까지 확인하며, 요청 없이 Boot 4로 변경하지 않는다. [Spring Boot 공식 요구사항](https://docs.spring.io/spring-boot/3.5/system-requirements.html)

### 2.3 jOOQ와 QueryDSL 선택 기준

| 기준 | jOOQ — 기본 선택 | QueryDSL — 대안 |
|---|---|---|
| 주 쿼리 | SQL 중심 복합 필터, CTE, window function, 집계 | JPA entity 중심 동적 predicate·일반 join |
| 도메인과 DB 관계 | migration 기반 schema와 query projection을 명시 | 기존 JPA entity 모델을 재사용 |
| 도입 비용 | code generation, Java/DB/edition 호환성 검증 필요 | 선택 배포판의 Jakarta·annotation processor·유지보수 상태 검증 필요 |
| 선택 조건 | Modifier 후보 풀과 가격 집계에서 SQL 제어가 중요 | 복잡 SQL 수요가 작고 JPA 중심 검색으로 충분하다는 근거가 있음 |
| 공존 정책 | JPA + jOOQ 두 경로까지만 유지 | 채택 시 ADR로 jOOQ 대체 범위를 명시; 세 도구 동시 도입 금지 |

jOOQ 생성 코드는 Flyway를 적용한 임시 DB에서 생성한다. 운영 DB에 code generation을 연결하지 않는다. 각 모듈 adapter에서 소유 테이블만 조회하며, 다른 모듈 테이블을 직접 join하지 않는다. 초기에는 각 모듈 공개 query API로 DTO를 모아 조립한다. 성능 때문에 cross-module read projection이 필요하면 소유자·갱신·정합성을 ADR로 정의한다.

JPA와 jOOQ는 동일 DataSource와 검증된 Spring transaction 구성을 사용한다. 같은 transaction에서 JPA 변경 직후 jOOQ로 읽는 예외 경로는 flush 시점과 rollback을 통합 테스트한다. jOOQ bulk write로 JPA persistence context를 우회하지 않는다. [jOOQ transaction 공식 문서](https://www.jooq.org/doc/latest/manual/sql-execution/transaction-management/)

## 3. 실행 아키텍처

**Modular Monolith**를 채택한다. 배포 단위는 하나의 Spring Boot 애플리케이션이며 내부에서는 도메인별 공개 계약과 데이터 소유권으로 경계를 유지한다.

```text
Browser: React / Vite
  └─ HTTPS → Reverse Proxy
                ├─ 정적 Frontend
                └─ /api → Spring Boot 한 프로세스
                            ├─ AI Orchestrator → OpenAI API
                            ├─ Crafting Application → Pure Crafting Engine
                            ├─ 모듈별 Application / Adapter
                            ├─ PostgreSQL
                            └─ Redis

poe2db → Python ETL → raw → staging → validation → diff
                                                 └─ 검토·승격 → production snapshot
poe.ninja 공식 economy API → Price adapter → Price snapshot
GGG 문서화된 API → User integration adapter (후속 범위)
```

모듈 간 통신은 공개 Java API 호출이다. 내부 HTTP, API Gateway, 서비스별 DB, 메시지 브로커는 필요하지 않다. Python ETL의 독립 실행은 수집 배치의 수명주기를 분리하는 것이며 MSA 전환이 아니다.

## 4. 모듈 경계와 의존성

### 4.1 카탈로그 모듈

| 모듈 / 패키지 | 소유 책임 | 공개 계약 | 허용하는 타 모듈 의존성 |
|---|---|---|---|
| Season / `season` | 시즌·패치·DataSnapshot manifest·활성 포인터 | `SeasonQuery`, `SnapshotQuery` | 없음 |
| Item / `item` | Base, 불변 `ItemState`, 텍스트 파싱·입력 검증 | `ItemCatalog`, `ItemParser` | `season.api`, `modifier.api` |
| Modifier / `modifier` | Modifier/Tier/Tag/Group, 버전별 후보 자료 | `ModifierCatalog` | `season.api` |
| Currency / `currency` | 화폐 식별자·명칭·시즌별 존재 여부 | `CurrencyCatalog` | `season.api` |
| CurrencyRule / `currencyrule` | 적용 조건·효과 선언·weight 정책·실행 handler 식별자 | `CurrencyRuleCatalog` | `season.api`, `currency.api`, `modifier.api` |

### 4.2 유스케이스 모듈

| 모듈 / 패키지 | 소유 책임 | 공개 계약 | 허용하는 타 모듈 의존성 |
|---|---|---|---|
| Price / `price` | league별 견적·환산·신선도·출처 | `PriceQuery` | `season.api`, `currency.api` |
| Crafting / `crafting` | 상태 전이·후보 적용·확률·비용·비교 결과 | `CraftingFacade` | `item.api`, `modifier.api`, `currency.api`, `currencyrule.api`, `season.api`, `price.api` |
| Preset / `preset` | 사용 가능 옵션 목록·목표 template·개인 저장 | `PresetQuery`, `PresetCommand` | `item.api`, `modifier.api`, `season.api` |
| AI / `ai` | 자연어 초안·tool 실행 조율·결과 설명 | `AiGoalService`, `AiExplanationService` | `item.api`, `modifier.api`, `currency.api`, `preset.api`, `crafting.api`, `season.api`, `price.api` |
| User / `user` | 서비스 사용자·외부 identity·OAuth token lifecycle | `CurrentUserQuery` | 없음 |

### 4.3 경계 규칙

1. 위 표에 없는 의존성은 기본 금지다. 공개 계약은 `<module>.api`만 사용한다. 타 모듈 `domain`, `application`, `infrastructure`, repository, JPA entity, jOOQ generated table import는 금지한다.
2. 모듈 내부는 `presentation → application → domain`, `infrastructure → application의 port / domain` 방향을 따른다. composition root만 adapter를 연결한다. `domain`은 HTTP/DB/Redis/OpenAI/Spring에 의존하지 않는다.
3. `api`에는 불변 DTO·value type·interface만 둔다. DTO는 타 모듈 내부 타입을 노출하지 않는다. `ItemState` 공유 계약은 `item.api`의 순수 Java record로 두고 persistence entity와 분리한다.
4. `shared`에는 식별자, 기술 오류 계약 등 최소 요소만 둔다. `CurrentActor`는 shared 계약으로 정의하고 `user`가 인증 context에서 제공한다. Preset/AI의 사용자 권한은 전달된 actor로 검사한다. User가 Preset을 조회하거나 Preset이 User DB를 조회하지 않는다.
5. 순환 의존성은 ArchUnit으로 CI에서 차단한다. Crafting은 AI·Preset·User를 호출하지 않는다. Preset은 목표 DTO로 전개한 뒤 Crafting에 전달한다. `currencyrule`은 ItemState를 평가하지 않고 선언만 제공하므로 Crafting과 순환하지 않는다.

계산에 필요한 Item/Modifier 자료는 Crafting application이 snapshot을 고정하여 읽고 내부 `EvaluationContext`로 변환한다. 순수 Engine은 이 context만 받으며 catalog 조회를 실행하지 않는다. 보존 옵션·사용자 목표는 데이터이며 외부 서비스를 호출하는 객체가 아니다.

## 5. Crafting Engine

### 5.1 핵심 모델

| 모델 | 필수 내용 |
|---|---|
| `ItemState` | baseId, itemLevel, rarity, modifier instance ID·실제 roll·명시/암묵 구분, 검증된 mechanic 상태, snapshotId |
| `TargetItemState` | 필수 조건, 허용 가능한 대체 옵션 그룹, 수치/티어 조건, 보존 조건. AND/OR 의미를 schema에 명시 |
| `RuleContext` | snapshotId, ruleSetVersion, engineVersion, 지원 handler registry, 검증된 후보·weight 자료 |
| `CraftAction` / `OutcomeDistribution` | actionId·입력 params, 적용 불가 사유, 다음 상태와 조건부 확률, 소모 화폐 |
| `DecisionResult` | calculationId, action별 결과, 계산 상태·근거·범위·버전, optional priceSnapshotId와 평가 시각 |

아이템의 접두/접미 최대 개수, 화폐별 희귀도 제한, mod group 충돌 등은 검증된 데이터에서 제공한다. PoE1 규칙을 PoE2에 복사하거나 전역 상수로 추정하지 않는다. tier 번호만으로 품질 순서를 가정하지 않고 명시적인 tier rank와 stat 범위를 사용한다. 다중 stat modifier의 실제 roll을 단일 문자열 비교로 판단하지 않는다.

### 5.2 실행 순서

1. 요청 버전과 입력을 검증하고 불변 snapshot 하나를 고정한다. 미지원 mechanic이나 불명확한 파싱이 계산에 영향을 주면 중단한다.
2. 적용 조건을 평가하고 후보 풀을 구성한다. Base·level·현재 group·태그·여유 슬롯·화폐 조건을 각 규칙 정의대로 결합한다.
3. 후보의 검증된 weight와 전이 규칙으로 outcome distribution을 생성한다. weight 합산/우선순위도 데이터의 의미에 따라 처리한다.
4. Target 만족, 보존 조건 훼손, 후속 적용 가능 여부를 평가한다. 비용 계산은 별도 가격 snapshot을 사용한다.
5. 결과·누락 데이터·계산 한계·버전을 반환한다. 사용자는 선택하거나 목표를 수정하고, 실제 게임 결과를 다시 입력한다.

### 5.3 deterministic 원칙

동일한 정규화 입력 + data snapshot + ruleSetVersion + engineVersion + calculation options는 동일한 규칙·확률 결과를 만든다. 비용 결과에는 동일한 priceSnapshotId·환산 통화·평가 시각도 필요하다. 응답의 requestId 같은 추적 필드는 재현 비교에서 제외한다.

Engine 내부에서 현재 시각, 네트워크, DB, LLM, 전역 mutable state를 읽지 않는다. 후보 정렬과 동률 정렬은 stable ID를 사용한다. 가격은 Engine 외부에서 불변 입력으로 주입한다. 단일 단계 정확 계산에는 난수가 필요 없다. 추후 sampling은 seed·PRNG algorithm·표본 수·병렬 분할 순서까지 고정하며 정확 계산과 별도 API mode로 둔다.

정수 weight는 overflow를 방지하는 `long` 검증 또는 `BigInteger`로 처리한다. 확률은 rational 또는 명시된 `MathContext`의 `BigDecimal`, 가격은 `BigDecimal`을 쓴다. UI 표시 시에만 반올림한다. 범위·총합·중복 상태 병합은 테스트로 검증한다. 표시 반올림 때문에 합이 100%와 달라질 수 있음을 UI에서 처리한다.

### 5.4 확률과 비용의 의미

검증된 단일 가중 추첨의 후보 집합이 C이고 목표 후보가 T이면 `P(T) = sum(weight(T)) / sum(weight(C))`다. 이는 모든 화폐 효과의 공통 공식이 아니다. 다단계 추첨·삭제 후 추가·조건부 선택은 중간 상태별 분포를 계산하여 결합해야 한다. roll 분포를 모르면 특정 수치 이상 확률을 임의 균등분포로 계산하지 않는다.

테스트 전용 가상 후보 A/B/C의 weight가 2/3/5이면 B 확률은 3/10이다. 이 fixture는 `synthetic`으로 표시하며 실제 Modifier 또는 화폐 효과로 제공하지 않는다.

| 결과 필드 | 의미 / 제한 |
|---|---|
| `successProbability` | 사용자가 확정한 Target 조건 충족 확률. 데이터 누락이면 null + reason |
| `protectedLossProbability` | 선택한 보존 조건이 깨질 확률. 성공과 겹칠 수 있으므로 서로 배타적인 pie chart로 묶지 않음 |
| `oneStepCost` | 해당 시도에 필요한 화폐량 및 선택적 시세 환산 |
| `expectedCost` | 종료·재시도·초기화 정책이 명시된 경우만 산출. 동일 상태 독립 반복 + 고정 비용 조건에서만 c/p를 사용 |
| `calculationStatus` | EXACT / ESTIMATED / UNAVAILABLE. EXACT도 제공된 데이터·모델 범위 내 정확이라는 뜻 |

가격 누락은 성공 확률을 무효화하지 않는다. 비용만 `UNAVAILABLE`로 두고 화폐 수량을 제공한다. `p=0`이면 기대비용은 유한 수가 아니므로 null과 사유를 반환한다. 제한 탐색은 depth/state/time budget과 `truncated`를 기록하고 전역 최적이라고 부르지 않는다. “쓸만함”은 사용자가 선택한 옵션 조건이며 시장 가치의 객관적 점수가 아니다.

## 6. 시즌·패치·CurrencyRule 버전

`Season`은 콘텐츠 시즌, `Patch`는 그 안의 게임 규칙 변경, `League`는 가격 시장 구분이다. 같은 시즌이라도 league별 가격은 다를 수 있다. 서로 대체 가능한 ID로 취급하지 않는다.

| 버전 필드 | 책임 |
|---|---|
| `seasonId`, `patchId` | 게임 콘텐츠 적용 범위 |
| `snapshotId` | 해당 패치에서 검증·승격한 카탈로그 및 규칙 묶음; 같은 패치 데이터 정정도 새 snapshot |
| `ruleSetVersion`, `handlerVersion` | 선언 규칙 묶음과 그 규칙을 해석하는 실행 구현 |
| `engineVersion` | 계산 algorithm 변경·오류 수정 버전 |
| `priceSnapshotId`, `observedAt` | 가격 근거와 관측 시점; 정적 데이터 버전과 독립 |

Currency는 ID·표시 정보이고, CurrencyRule은 동작 정의다. 규칙은 currencyId, 적용 범위, precondition, effect type, parameter schema, weight policy, handler key, provenance, 검증 상태를 가진다. JSON 문구를 실행 코드로 평가하거나 LLM이 생성한 script를 실행하지 않는다. 검증된 enum/DSL과 allowlist handler만 쓴다.

기존 handler가 지원하는 수치 변경은 데이터 release로 처리한다. 새 효과·새 상태 전이는 Java handler와 fixture를 추가한 code release가 필요하다. 알 수 없는 handler는 snapshot 승격을 거부한다. AI는 원문 변경 요약을 도울 수 있지만 학습이나 요약 결과만으로 production 규칙을 변경할 수 없다.

snapshot은 `DRAFT → VALIDATED → APPROVED → PUBLISHED`로 승격하고 실패 시 `REJECTED`로 남긴다. publish 후 payload는 불변이다. 활성 포인터만 원자적으로 바꾼다. 이전 snapshot을 삭제·덮어쓰지 않고 비활성화한다. 실행 중 요청은 시작 시 pin한 버전을 계속 쓴다. 이전 프리셋을 새 시즌으로 옮길 때 호환성 diff와 사용자 확인이 필요하다.

## 7. 데이터 파이프라인과 외부 소스

### 7.1 소스 정책

| 소스 | 허용 | 금지 / 실패 시 처리 |
|---|---|---|
| poe2db | 정적 페이지 기반 Python ETL, 출처 URL·수집 시각·content hash·parser version 보관 | 공식 GGG API로 부르지 않는다. 접근 정책 확인, 제한된 수집량·backoff 적용. 차단 우회나 요청 시 실시간 crawl 금지 |
| poe.ninja | 운영자가 문서화한 공식 economy API만 backend에서 호출 | builds/profiles/character/PoB 등 내부 API 및 역공학 금지. 문서·응답 변화 시 adapter 중단 또는 stale 가격 사용 |
| GGG | 문서화된 공식 API만, 승인된 client와 최소 scope | undocumented/internal endpoint reverse engineering 금지. PoE1 제공 기능의 PoE2 지원 추정 금지 |
| 운영자 입력 | 검증된 게임 옵션 중 사용 가능 옵션 목록·프리셋 작성 | 미확인 효과·weight 생성 금지. 원문 증거와 검토 이력을 남긴다. |

poe.ninja는 공개 economy 범위와 내부 API를 구분한다. 구현 시 해당 문서의 PoE2 league/exchange 경로와 허용 category를 확인하여 allowlist로 고정한다. 응답의 기준 통화 metadata를 읽고 Divine/Exalted 등을 하드코딩하지 않는다. ETag·cache header를 존중하며 사이트 운영자가 안내한 갱신 주기에 맞춘다. [poe.ninja 공식 API 문서](https://poe.ninja/docs/api)

GGG 문서에는 PoE1 전용 리소스가 명시되어 있다. 대규모 PoE2 장비 확보를 서비스 전제로 삼지 않는다. 서비스 로그인 OAuth와 향후 GGG 계정 연동 OAuth는 공급자별 계약과 token lifecycle을 분리한다. [GGG 공식 개발 문서](https://www.pathofexile.com/developer/docs)

poe2db 수집 허용 조건과 재배포 범위는 실제 ETL을 시작하기 전에 확인할 TODO다. 공개 페이지가 존재한다는 이유로 무제한 수집·재배포 권한을 가정하지 않는다. 이 문서 작성 과정에서는 실제 수집을 실행하지 않았다.

### 7.2 raw → staging → validation → diff → production

1. **raw:** 원본 HTML/JSON, URL, locale, fetchedAt, content hash, 응답 metadata를 불변 저장한다. 원본 확보 실패를 빈 데이터로 대체하지 않는다.
2. **staging:** BeautifulSoup으로 파싱하고 pandas로 표준화한다. stable source ID mapping, 타입·단위·null 처리를 명시한다. 모든 row에 importRunId와 source reference를 연결한다.
3. **validation:** FK·중복·수치 범위·weight 누락·미지원 handler·snapshot 일관성·파서 누락을 검사한다. 차단 오류가 하나라도 있으면 production으로 진행하지 않는다.
4. **diff:** 직전 published snapshot 대비 추가/삭제/효과/weight/조건 변경을 구분한다. 급격한 행 감소는 삭제가 아니라 파서 오류 가능성으로 표시한다. 검토자는 raw 근거·회귀 계산·diff를 보고 승인한다.
5. **production:** 제한된 publisher가 불변 release를 적재하고 manifest hash를 재검증한 뒤 활성 포인터를 transaction으로 교체한다. 새 cache key를 사용하고 실패 시 이전 포인터로 복구한다.

Python ETL 계정은 raw/staging만 쓸 수 있다. production 승격은 배포된 CLI/관리 작업으로 수행하며 공개 사용자 endpoint를 만들지 않는다. 새 snapshot 적재 중에는 활성 요청이 해당 데이터에 접근하지 않는다. 다중 모듈 참조 검증은 publisher 전용 offline 작업으로 허용하며 런타임 모듈 경계의 예외로 확대하지 않는다.

`source + rawHash + parserVersion + targetPatch`를 import idempotency key로 사용한다. 각 stage의 산출물 hash·schema version·행 수·경고·실패 사유·검토자·승격 시각을 기록한다. raw/staging 실패 시 그 단계부터 재시도하며 기존 published 데이터는 유지한다. 출처 확인이 안 된 weight는 null로 보관하고 해당 정량 계산을 막는다.

## 8. DB 설계

단일 PostgreSQL DB에서 schema를 모듈별로 나눈다. migration은 전역 순서로 실행하되 파일명에 모듈을 표시한다. 모듈별 schema 접근은 애플리케이션 규칙과 구조 테스트로 통제하고, ETL/publisher/app 계정은 DB 권한으로 분리한다.

### 8.1 카탈로그·버전 테이블

| 소유 모듈 | 주요 테이블 | 주요 제약 |
|---|---|---|
| season | season, patch, league, data_snapshot, active_snapshot | snapshot manifest hash unique, 활성 포인터 version, patch↔season FK |
| item | base_item, item_text_alias | `(snapshot_id, base_id)` unique, alias에 locale 포함 |
| modifier | modifier, modifier_tier, modifier_stat, modifier_tag, modifier_group, spawn_weight | snapshot 포함 복합 키, tier/stat 참조 FK, weight null과 0 구분 |
| currency | currency, currency_alias | `(snapshot_id, currency_id)` unique |
| currencyrule | currency_rule, rule_source_reference | snapshot/currency/rule key, handler/schema version, publication 전 지원 검증 |

### 8.2 사용자·계산·운영 테이블

| 소유 모듈 | 주요 테이블 | 주요 제약 |
|---|---|---|
| user | app_user, external_identity, oauth_connection | `(provider, subject)` unique, token 암호화, email을 identity PK로 쓰지 않음 |
| preset | preset, preset_revision, curated_option_set | ownerId, visibility=PRIVATE 기본값, optimistic version, pinned snapshot |
| crafting | calculation, calculation_result | canonical input hash, snapshot·engine·rule 버전, status, 결과 payload schemaVersion |
| price | price_snapshot, price_quote, external_id_mapping | league/source/asOf, 기준 통화·가격, mapping 미확인 항목은 quarantine |
| pipeline 전용 | import_run, validation_issue, release_review | 원본 hash, stage, diff hash, 승인 기록, 재실행 식별자 |

ItemState와 Target의 저장용 snapshot payload, 계산 결과는 schemaVersion이 있는 `jsonb`를 사용할 수 있다. 검색·join·제약 대상인 ID, level, tag, group, status, version은 관계형으로 관리한다. JSON 안에만 중요한 FK를 숨기지 않고 publish/application validation으로 내부 참조를 검증한다.

타 모듈 참조는 ID로 저장하고 JPA `@ManyToOne` 등 entity 탐색을 금지한다. 불변 catalog 간 참조는 `(snapshot_id, referenced_id)` 복합 FK로 다른 snapshot 혼합을 방지할 수 있다. mutable 사용자 데이터의 cross-module cascade 삭제는 금지하고 명시적 삭제 유스케이스로 처리한다.

조회 패턴에 맞춰 `(snapshot_id, base_id)`, `(snapshot_id, modifier_group_id)`, `(owner_id, updated_at)`, `(league_id, observed_at DESC)` 등을 인덱싱한다. weight 조건의 실제 분포를 확인한 뒤 EXPLAIN으로 index를 조정한다. JSONB GIN은 필요한 query에만 추가한다. N+1은 projection/fetch 전략으로 해결하고 무제한 목록 응답은 금지한다.

Flyway만 schema를 변경한다. Hibernate `ddl-auto=validate`, OSIV 비활성화를 기본값으로 한다. 적용된 migration은 수정하지 않는다. nullable 추가 → backfill → 검증 → 제약 강화처럼 호환성 있는 migration을 우선한다. schema 변경과 데이터 snapshot 승격은 서로 다른 release다.

## 9. API 계약

REST JSON, `/api/v1`, UTF-8, UTC ISO-8601 timestamp를 사용한다. OpenAPI를 계약으로 관리하고 Frontend DTO를 생성한다. 다음 경로는 제안하는 **서비스 자체 API**이며 외부 업체 endpoint가 아니다.

### 9.1 조회·입력

| Method / Path | 책임 |
|---|---|
| `GET /api/v1/seasons` | 시즌·패치·published snapshot·지원 상태 |
| `GET /api/v1/items/bases?snapshotId=...` | page/size가 제한된 Base 목록 |
| `GET /api/v1/modifiers?snapshotId=...&baseId=...` | 표시·선택용 옵션 목록; 최종 적용 가능성은 Engine이 재검증 |
| `POST /api/v1/items/parse` | text + locale + snapshotId → 해석 초안·ambiguities·unsupported fields |
| `GET /api/v1/currencies?snapshotId=...` | 해당 snapshot의 화폐 카탈로그 |

### 9.2 계산·AI

| Method / Path | 책임 |
|---|---|
| `POST /api/v1/crafting/evaluations` | startItem + target + snapshotId + optional priceSnapshotId → 한 단계 비교 |
| `GET /api/v1/crafting/evaluations/{id}` | 소유자 또는 안전한 익명 세션에 한정한 결과 재조회 |
| `POST /api/v1/ai/goal-drafts` | 자연어 → editable Target draft; 사용자 확인 전 확정하지 않음 |
| `POST /api/v1/ai/explanations` | 서버가 읽은 calculationId → 근거 참조 기반 설명 |
| `GET /api/v1/prices?leagueId=...` | 가격 출처·snapshot·freshness를 포함한 견적 |

### 9.3 사용자·프리셋

| Method / Path | 책임 |
|---|---|
| `GET /api/v1/me` | 로그인 상태와 최소 사용자 정보 |
| `GET /api/v1/presets` | 공개 curated 목록 또는 인증된 자신의 목록 |
| `POST /api/v1/presets` | 확인된 Target을 개인 프리셋으로 저장 |
| `PATCH /api/v1/presets/{id}` | owner 검증 + version/If-Match로 동시 수정 제어 |
| `DELETE /api/v1/presets/{id}` | owner 검증 후 삭제 |

계산 요청의 snapshotId는 필수다. 가격 snapshot 미지정 시 application이 요청 시작 시 한 번 선택하여 응답에 기록한다. 클라이언트가 보내는 price·probability·ownerId·rule text는 신뢰하지 않는다. 저장된 결과 ID도 권한 검증 없이 tool에서 조회할 수 없다.

평가 응답에는 `calculationId`, `inputHash`, `snapshotId`, `ruleSetVersion`, `engineVersion`, `priceSnapshotId`, `evaluatedAt`, `actions`, `warnings`, `truncated`를 포함한다. Action별 확률/비용은 각각 `status`, `value`, `reasonCode`를 가지며 한 필드의 누락이 다른 필드를 0으로 바꾸지 않는다. 정밀 decimal은 문자열로 전달하고 표시 경계에서 변환한다.

초기 제한 제안: text 16 KiB, page size 최대 100, AI tool call 최대 5회, AI 총 처리 예산 30초. 게임 modifier 수 제한은 이 숫자와 별개이며 데이터에서 검증한다. 계산 state/time budget은 benchmark 후 설정한다. 동기 계산이 예산을 넘으면 `CALCULATION_LIMIT_EXCEEDED`; 조용한 부정확 결과로 대체하지 않는다. 향후 job API는 필요가 실측된 뒤 도입한다.

## 10. OpenAI 연동

Responses API 기반 adapter를 기본으로 제안한다. tool/function calling은 서버 기능 요청, Structured Outputs는 Target draft 및 설명의 구조 제한에 사용한다. `strict: true`를 명시하고 지원되는 JSON Schema 범위에서 object에 `additionalProperties: false`, 필수 key, nullable 필드를 정의한다. schema 준수는 게임 의미의 정확성을 보장하지 않는다. refusal·incomplete 응답은 별도 처리한다. [Function calling](https://developers.openai.com/api/docs/guides/function-calling), [Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs)

### 10.1 tool 경계

| Tool | 허용 동작 |
|---|---|
| `search_modifiers` | pin된 snapshot의 후보 조회; 검색어·결과 수 제한 |
| `find_presets` | 공개 또는 현재 사용자가 소유한 preset 조회 |
| `evaluate_craft` | 서버에서 검증한 ItemState/Target으로 CraftingFacade 호출 |
| `get_calculation` | 권한 있는 계산 결과 조회; 설명 근거 제공 |
| `get_prices` | 서버 가격 snapshot 조회; 외부 URL을 인수로 받지 않음 |

1. 자연어와 최소한의 카탈로그 후보를 전달하여 `TargetDraft`를 얻는다. 사용자 identity/token·전체 DB·불필요한 아이템 텍스트는 전송하지 않는다.
2. 서버가 ID 존재·snapshot·수치·조건 호환성을 검증한다. ambiguous 항목은 UI에서 선택하게 한다. LLM 출력으로 검증을 우회하지 않는다.
3. UI에서 수정 가능한 초안을 보여주고 사용자가 “제작 방법 찾기”를 누르면 확정 입력으로 계산한다.
4. tool dispatcher는 allowlist, actor 권한, 인수 schema, snapshot, 호출 한도, timeout을 매번 검사한다. 임의 SQL/HTTP/file/shell tool은 없다.
5. 설명에는 `calculationId`, `actionId`, `evidenceRefs`, `limitations`를 받는다. 확률·비용은 Engine 응답을 UI에 직접 표시하고 생성 문장 속 수치도 일치 검증한다. 실패 시 template 설명으로 대체한다.

prompt/model/schema/tool 버전을 기록하되 기본 로그에는 원문 대화와 개인 아이템 payload를 남기지 않는다. `OPENAI_MODEL`은 검증된 허용 모델 설정으로 주입한다. 모델 변경은 golden eval 후 반영한다. 낮은 temperature가 deterministic 계산을 대체하지 않는다. 외부 페이지·사용자 입력·프리셋 설명은 모두 데이터이며 system instruction으로 실행하지 않는다.

LLM 호출은 DB transaction 밖에서 수행한다. AI timeout·429·5xx에 제한된 retry/backoff를 적용하고 한 요청의 비용·token·tool 횟수 예산을 넘기지 않는다. 저장·게임 실행 tool은 MVP에서 제공하지 않는다. API key가 없으면 AI 기능을 명시적으로 비활성화하고 직접 입력 경로를 유지한다.

## 11. Frontend 상태와 UX

| 상태 | 소유 |
|---|---|
| Base/Modifier/가격/프리셋/계산 응답 | TanStack Query, query key에 snapshotId·league·권한 범위 포함 |
| 편집 중 ItemState·Target·보존 조건·선택된 탭 | Zustand; 서버 응답을 중복 원본으로 저장하지 않음 |
| 폼 validation·focus·임시 입력 | component/form 상태 |
| 로그인 | 서버 session이 권위. token을 localStorage에 저장하지 않음 |
| snapshot 변경 | 호환성 경고, draft 검증, 이전 계산 stale 표시, 관련 query 취소/무효화 |

화면은 입력 → 초안 확인 → 선택지 비교 → 실제 결과 입력 흐름을 따른다. 낮은 성공 확률 Action을 숨기거나 자동 선택하지 않는다. 위험은 “나쁜 아이템” 같은 단정 대신 사용자가 지정한 보존 조건의 훼손으로 설명한다. 정량 근거 없는 판매가·별점은 표시하지 않는다.

입력 변경 시 이전 요청을 취소하거나 inputHash/request revision으로 늦게 도착한 응답을 폐기한다. AI 설명 실패와 계산 실패를 별도 표시한다. 키보드 조작, label, focus 이동, 색상 외 상태 표기를 제공한다. snapshot 전환·로그아웃 때 사용자 query cache를 정리한다.

## 12. 캐시 전략

| 대상 | key 핵심 | 초기 TTL 제안 | 무효화·장애 처리 |
|---|---|---|---|
| Modifier 자료 | schemaVersion + snapshotId + canonical filters | 24시간 | 불변 버전 key; release 변경 시 새 key |
| 규칙 계산 | engineVersion + ruleSetVersion + snapshotId + inputHash + options | 1시간 | 코드/데이터 변경 시 자연 분리; 개인 입력은 사용자 namespace 또는 cache 제외 |
| 가격 | source + leagueId + priceSnapshotId; latest pointer 별도 | latest pointer 15분 | 조건부 갱신. 초기 polling 60분, 원본 cache header 우선 |
| 비용 결합 | calculationHash + priceSnapshotId + quoteCurrency | 15분 | 확률 cache와 분리; 가격 교체 시 재계산 |
| 공개 curated preset | snapshotId + revision + filters | 10분 | 변경 commit 뒤 삭제; 개인 프리셋은 초기 공용 cache 제외 |

위 TTL은 서비스 기본 제안이며 외부 소스 제한이 우선한다. 가격 stale 허용 한도는 초기 2시간으로 제안하고 `observedAt`, `age`, `STALE`을 표시한다. 한도 초과는 비용 unavailable이다. 시세 갱신에 실패해도 오래된 견적을 새 시각으로 저장하지 않는다.

cache-aside를 기본으로 하고 DB가 원본이다. cache hit/miss의 기능 결과가 같아야 한다. 동시 갱신은 single-flight 또는 짧은 token lock으로 합치며 lock은 correctness의 근거가 아니다. Redis 장애 시 계산과 조회는 DB로 제한적으로 우회하되 rate limit은 보수적인 process-local 한도로 유지한다. Redis를 session 저장소로 확장하면 장애 시 인증 요청을 fail-closed로 처리하는 별도 정책이 필요하다.

## 13. 예외·로그·관측성

### 13.1 예외 계약

Spring `@RestControllerAdvice`에서 `application/problem+json`으로 변환한다. 필드는 `type`, `title`, `status`, `detail`, `instance`, `code`, `traceId`, 선택적 `fieldErrors`다. 내부 stack trace·SQL·token은 응답하지 않는다.

| HTTP | 대표 code | 처리 |
|---|---|---|
| 400 / 422 | INVALID_REQUEST / UNSUPPORTED_MECHANIC / INCOMPLETE_RULE_DATA | 문법 또는 도메인 입력 문제. 수정할 필드·누락 근거 표시 |
| 401 / 403 / 404 | AUTH_REQUIRED / FORBIDDEN / NOT_FOUND | 인증·권한 검사. 타인 resource 존재 노출을 막기 위해 404 정책 일관 적용 |
| 409 | SNAPSHOT_MISMATCH / PRESET_VERSION_CONFLICT | 버전 재선택·최신 데이터 확인 |
| 429 / 503 | RATE_LIMITED / AI_UNAVAILABLE / DATA_UNAVAILABLE | retry 가능 여부와 적절한 Retry-After |
| 500 | INTERNAL_ERROR | traceId로 운영 추적; 사용자에게 내부 정보 노출 금지 |

유효한 입력이지만 Action이 적용 불가능하면 전체 500이 아니라 비교 응답 안의 `applicable=false`와 reason이다. 성공 확률을 계산할 데이터가 없는 Action은 수치 null과 `UNAVAILABLE`을 반환한다. 요청 자체가 지원 범위 밖이면 422로 구분한다.

### 13.2 운영 기준

| 범주 | 수집 항목 |
|---|---|
| 요청 | traceId, route template, status, duration, errorCode |
| 계산 | snapshot/engine version, 계산 소요 시간, 탐색 상태 수, 실패 분류 |
| 외부 연동 | provider, timeout/429/5xx, retry 횟수, AI token·비용 |
| 데이터·캐시 | import 실패·diff 검토 대기·가격 age·cache hit ratio·Redis 오류 |
| 보안·변경 감사 | 인증 실패 집계, snapshot 승인/승격, 프리셋 권한 실패; 민감 payload 제외 |

metric label에 userId, calculationId, 전체 prompt, 무제한 URL을 넣지 않는다. 구조화 JSON 로그에 traceId를 연결하고 예외는 경계에서 한 번 기록한다. liveness는 프로세스 생존, readiness는 DB·활성 데이터셋 사용 가능 여부를 검사한다. OpenAI 장애는 전체 readiness 실패 사유로 삼지 않는다. Actuator 상세 정보는 내부망·인증으로 제한한다.

성능 목표는 보장치가 아닌 초기 수용 기준이다: 문서화한 CI 기준 머신·fixture에서 warm catalog 조회 p95 300ms 이하, 지원 범위 단일 단계 계산 p95 1초 이하, AI 전체 timeout 30초. 부하·동시성·fixture 크기를 benchmark 결과에 함께 기록하고 초과 원인을 먼저 측정한다.

## 14. 보안·OAuth·비밀정보

Spring Security 기반 서버 session + Secure/HttpOnly/SameSite cookie를 기본으로 제안한다. 로그인 공급자는 TODO이며 지원되는 OAuth/OIDC 흐름을 검증한다. OAuth state와 PKCE, OIDC 사용 시 nonce/issuer/audience를 검증한다. GGG가 OIDC를 지원한다고 가정하지 않는다. provider user-info와 공식 계약을 확인한다.

1. cookie 인증의 상태 변경 API는 CSRF를 검증하고 CORS는 정확한 origin allowlist를 사용한다. OAuth callback URL과 redirect 목적지를 고정한다.
2. 개인 preset·계산에는 owner 검사를 수행한다. 관리자 데이터 승격은 별도 role/실행 계정으로 제한한다. 익명 결과도 예측 어려운 ID만으로 보호하지 않고 세션 접근 정책을 둔다.
3. 외부 token은 server에 암호화 저장하고 만료·refresh·revoke·계정 삭제를 처리한다. 사용자 token과 API key를 브라우저, LLM, URL, 로그에 전달하지 않는다.
4. 외부 요청 host/path를 allowlist로 고정하고 사용자 제공 URL fetch를 금지한다. SQL은 binding, HTML/Markdown은 sanitize, 업로드·text 크기는 제한한다.
5. secret은 환경변수 또는 배포 secret store로 주입한다. `.env`·token dump·운영 DB 덤프를 commit하지 않는다. `.env.example`에는 빈 값/placeholder만 둔다.

환경변수 계약: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT`, `OPENAI_API_KEY`, `OPENAI_MODEL`, `APP_PUBLIC_URL`, `APP_CORS_ALLOWED_ORIGINS`, `OAUTH_CLIENT_ID`, `OAUTH_CLIENT_SECRET`, `TOKEN_ENCRYPTION_KEY`, `POE_NINJA_USER_AGENT`. OAuth provider별 추가 변수와 GGG 연동 변수는 연동 시 문서화한다. `VITE_*`는 공개 설정이므로 secret을 넣지 않는다. 필수 설정은 시작 시 검증하고 AI처럼 optional 기능은 명시적 disable 상태를 제공한다.

개인 입력은 계산에 필요한 최소 기간만 보관하도록 한다. 제안 기본값은 익명 결과 24시간, 로그인 계산 이력 30일이며 실제 공개 전 사용자 동의·삭제 정책을 확정한다. raw 게임 카탈로그 보관과 개인 대화 보관을 혼동하지 않는다. 데이터 export·삭제는 모듈 공개 command를 이용한 명시적 유스케이스로 구성한다.

## 15. 테스트와 완료 기준

### 15.1 핵심 정확성

| 종류 | 필수 검증 |
|---|---|
| Engine unit — JUnit5/AssertJ | 후보 제외·중복 group·level 경계·미지원 handler·빈 pool·0 weight 합·상태 불변성 |
| 확률 invariant | 모든 확률 ≥ 0, 총합 1, 동률 순서 안정, 동일 입력 재현, 조건부 경로 합산, overflow |
| 시즌 회귀 | 이전 snapshot 결과 불변, 혼합 snapshot 거부, 정정 release와 engineVersion 구분 |
| 비용 | 누락/0/오래된 가격 구분, quote currency 변환, p=0, c/p 전제 불충족 차단 |
| 구조 — ArchUnit | 순환 금지, `.api` 외 참조 금지, Engine의 Spring/DB/LLM 의존성 금지 |

### 15.2 통합·제품 검증

| 종류 | 필수 검증 |
|---|---|
| Testcontainers | PostgreSQL 제약·jOOQ 결과·JPA rollback/flush·Flyway 신규/기존 migration·Redis hit/miss/장애 |
| ETL — pytest | raw fixture→staging golden result, 파서 누락 차단, diff 삭제 폭증, idempotency, 승격 실패 rollback |
| AI contract/eval | JSON schema, 잘못된 ID, prompt injection, tool budget, refusal, 설명 수치 불일치, 직접 입력 fallback |
| Frontend | AI 초안 수정·snapshot 변경·late response 폐기·가격 없음·비로그인/타인 preset 거부 |
| 핵심 E2E | 입력→확인→계산→목표 수정→재계산, 승인 snapshot 전환 중 일관성 |

실제 게임 fixture는 source URL/hash, snapshot, 검토 기록을 함께 저장한다. synthetic fixture는 실데이터와 디렉터리·ID namespace를 분리한다. unit/CI 기본 테스트는 외부 API와 운영 자격증명이 없어도 실행되어야 한다. 외부 연동은 mock HTTP contract test를 기본으로 하고 live smoke는 명시적 opt-in으로 둔다.

변경 완료 조건은 관련 테스트 통과, OpenAPI/문서 갱신, migration 재현, 출처·버전 보존, 미확인 데이터에 대한 안전한 실패다. coverage 숫자만으로 규칙 정확성을 주장하지 않는다. 버그 수정은 재현 테스트부터 작성하고 UI 문구만 바꾸는 변경에는 불필요한 전체 테스트를 추가하지 않는다.

## 16. 패키지·저장소 구조

```text
/
├── AGENTS.md
├── docs/
│   ├── TECHNICAL_SPEC.md
│   ├── supported-mechanics.md
│   ├── data-sources.md
│   └── adr/
├── backend/
│   ├── build.gradle.kts
│   ├── gradlew / gradlew.bat
│   └── src/
│       ├── main/java/com/poe2craft/
│       │   ├── Poe2CraftApplication.java
│       │   ├── bootstrap/
│       │   ├── shared/
│       │   ├── item/ modifier/ currency/ currencyrule/ season/
│       │   ├── price/ preset/ ai/ user/
│       │   └── crafting/
│       │       ├── api/
│       │       ├── domain/{action,rule,probability,simulation,route}/
│       │       ├── application/{port,service}/
│       │       ├── infrastructure/{persistence,cache}/
│       │       └── presentation/
│       ├── main/resources/db/migration/
│       └── test/java/com/poe2craft/
├── frontend/src/{app,features,shared}/
├── data-pipeline/
│   ├── pyproject.toml
│   ├── src/poe2etl/{extract,transform,validate,diff,publish}/
│   └── tests/fixtures/{synthetic,verified}/
└── infra/{compose.yaml,docker}/
```

다른 Backend 모듈도 필요한 `api/domain/application/infrastructure/presentation`만 생성한다. 빈 폴더·추상 class를 일괄 생성하지 않는다. `route`는 확장 지점이며 MVP에 전역 탐색 구현을 요구하지 않는다. 단일 Gradle application에서 package 경계를 먼저 검증하고 필요 시 Gradle module로 강화한다.

## 17. 배포·운영 복구

### 17.1 로컬 / 초기 단일 호스트

Docker Compose에 app, PostgreSQL, Redis, reverse proxy/static frontend를 둔다. ETL은 batch profile이며 상시 실행하지 않는다. 개발 중에는 Vite dev server에서 API proxy를 사용한다. DB volume을 유지하고 PostgreSQL/Redis 포트를 production public interface에 노출하지 않는다. container는 non-root와 고정 image tag/digest를 사용한다.

CI는 lint/typecheck → unit/architecture test → migration/codegen/integration test → frontend build → image build 순서다. 배포는 migration 전용 작업을 1회 실행한 뒤 호환 app image를 교체하고 readiness·smoke test를 확인한다. production에서 여러 replica가 각자 migration을 시작하지 않게 한다.

### 17.2 AWS 후속 배포 제안

| 대상 | 제안 |
|---|---|
| Frontend | S3 + CloudFront 정적 호스팅 |
| Backend | 초기 EC2 + Compose 또는 운영 요구가 생기면 ECS/Fargate + ALB |
| Database / Cache | RDS PostgreSQL, 필요 시 ElastiCache Redis |
| 데이터·Secret | S3 raw snapshot, Secrets Manager 또는 Parameter Store, IAM 최소 권한 |
| 배치·로그 | 예약 batch 실행, CloudWatch 로그/metric; 선택 근거는 비용·운영시간 실측 후 ADR |

AWS 서비스 구성은 후속 제안이며 지금 리소스를 생성하지 않는다. app image rollback과 data snapshot pointer rollback을 분리한다. DB schema는 backward-compatible migration을 우선하며 destructive rollback 대신 forward fix를 기본으로 한다. 데이터 손실 위험 migration은 backup·복원 rehearsal·별도 승인 후 실행한다.

초기 복구 목표 제안: 운영 데이터 RPO 24시간/RTO 4시간. 정기 backup만으로 달성했다고 보지 않고 복원 시험으로 확인한다. 공개 서비스에서 더 짧은 RPO가 필요하면 PITR과 backup 보존을 별도로 정한다. 잘못된 snapshot은 이전 포인터로 되돌리고 해당 계산 결과에는 사용된 버전을 계속 표시한다.

## 18. MVP에서 제외하는 인프라

| 기술 | 지금 제외하는 이유 | 재검토 기준 |
|---|---|---|
| Kafka | durable 대량 event stream·복수 consumer 요구가 없음 | 유실 없는 비동기 처리·재생 요구와 규모가 입증됨; 먼저 DB outbox/job table 검토 |
| Kubernetes | 소수 container에 cluster 운영 복잡성이 큼 | 다수 workload·운영 조직·배포/격리 요구가 관리형 대안을 넘어섬 |
| Elasticsearch | ID/태그/조건 검색은 PostgreSQL로 시작 가능 | query 개선·index 후에도 검색 품질/SLO가 충족되지 않음 |
| Vector DB | 검증된 옵션 ID와 관계형 필터가 핵심 | 의미 검색이 평가셋에서 개선을 입증함; 먼저 기존 DB 확장 가능성 검토 |
| MSA | 독립 확장·팀 소유권 요구가 검증되지 않음 | 아래 분리 기준을 충족하는 특정 모듈만 추출 |

## 19. 향후 서비스 분리 기준

분리는 미래 가능성만으로 시작하지 않는다. 지표와 ADR에 독립 확장 필요, 변경 빈도, 장애 영향, 운영 비용, 데이터 소유권을 기록한다.

1. profiler·부하 테스트에서 특정 모듈의 CPU/latency/메모리 문제가 확인되고, cache·query 개선·작업 한도 조정으로 해결되지 않는다.
2. 모듈별 독립 배포 또는 장애 격리가 반복적으로 필요하며 운영 담당자와 배포 체계가 있다.
3. 공개 API와 데이터 소유자가 고정되어 있고 공유 transaction·cross-table 접근을 제거할 수 있다.
4. timeout/retry/idempotency/관측성/인증·데이터 migration 비용을 포함해 현재보다 유리함을 설명할 수 있다.
5. contract test와 단계적 전환·복구 계획을 만든 후 AI 또는 Price 같은 경계부터 검토한다. Crafting Engine은 가능한 한 순수 library로 유지한다.

## 20. 구현 시작 순서와 미결정 사항

### 20.1 구현 순서

1. **기반:** 저장소·Wrapper·lockfile·CI·Compose·Flyway·ArchUnit을 설정한다. 빈 DB에서 build/test가 재현되면 완료다.
2. **데이터:** 지원할 시즌·Base·화폐를 선택하고 raw fixture·검증·snapshot 승격을 만든다. 출처 없는 규칙이 publish되지 않으면 완료다.
3. **Engine:** synthetic fixture로 단일 단계 분포를 구현하고 검증된 게임 fixture를 추가한다. 동일 입력 재현과 미지원 처리 테스트가 완료 기준이다.
4. **제품:** 직접 설정·텍스트 파싱·프리셋·비교 UI·서비스 로그인을 연결한다. 사용자 수정과 실제 결과 재입력이 가능하면 완료다.
5. **연동:** Price freshness와 OpenAI 초안/설명을 추가한다. 두 외부 서비스가 실패해도 직접 계산이 동작해야 한다.

### 20.2 결정이 필요한 항목

| 항목 | 결정 전 기본 처리 |
|---|---|
| 첫 지원 시즌·패치·Base·Currency와 weight 출처 | 지원 manifest는 비워두고 synthetic 테스트만 실행. 실제 규칙을 추정하지 않음 |
| poe2db 수집·재배포 조건 | 실제 crawler 운영 전 문서화; 확인 안 된 범위는 수집 중단 |
| 로그인 공급자·OpenAI 모델·배포 예산 | adapter 설정을 외부화; 비용 발생 연동은 실제 구현 단계에서 설정 |
| “쓸만한 옵션” 목록·보존 정책 | 운영자 curated 옵션과 사용자 선택으로 한정; 메타 점수 invent 금지 |
| 운영 보존·성능·복구 기준 | 본문 초기 제안을 실측·사용자 요구로 확정하고 ADR 기록 |

### 20.3 참고 문서

공식 문서 확인일은 2026-09-08이다. API 경로·scope·지원 버전은 실제 연동 시 다시 확인한다. 본문 모듈 경계·TTL·DB 설계·MVP 제한은 이 프로젝트의 설계 결정이며 외부 문서가 보장하는 기능이 아니다.

| 자료 | 확인 목적 |
|---|---|
| [Spring Boot 요구사항](https://docs.spring.io/spring-boot/3.5/system-requirements.html) | Java/Boot 호환성 |
| [jOOQ transaction](https://www.jooq.org/doc/latest/manual/sql-execution/transaction-management/) | Spring transaction 통합 |
| [OpenAI Function calling](https://developers.openai.com/api/docs/guides/function-calling) / [Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs) | tool과 schema 계약 |
| [poe.ninja API](https://poe.ninja/docs/api) | 공개 economy 범위·내부 API 금지·cache 정책 |
| [GGG Developer Docs](https://www.pathofexile.com/developer/docs) | 문서화된 API·PoE별 지원·OAuth |
