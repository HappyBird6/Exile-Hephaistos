# PoE2 Codex 세션 운영 가이드

> **2026-09-16: 이전 운영 방식의 참고 자료. 이 문서의 세션 이름·초기 프롬프트·테스트 책임·작업 흐름을 실행하지 않는다. 현재 운영은 `MULTI_SESSION_WORKFLOW.md`를 최우선으로, 그다음 `AGENTS.md`를 따른다. 아래 본문은 변경 이력 참고용으로만 보존한다.**

이 문서는 사용자가 VS Code에서 직접 코드를 작성하고 최종 결정을 내리면서 Codex/Copilot을 장기 보조자로 사용하는 방법을 정리한다. 세션은 모듈과 역할의 문맥을 오래 유지하기 위한 작업 공간이지, 코드 구조를 미리 만들어 두기 위한 근거가 아니다.

> 현재 기준 문서 우선순위: `MULTI_SESSION_WORKFLOW.md` → `AGENTS.md` → `docs/TECHNICAL_SPEC.md`(없으면 루트 `TECHNICAL_SPEC.md`) → 승인된 ADR/세부 명세. 이 가이드는 실행 기준에서 제외한다.

## 1. 공통 운영 원칙

1. **사용자가 코드와 결정을 소유한다.** Codex는 분석, 선택지 제시, 구현 보조, 테스트, 검토를 수행하지만 요구사항·게임 규칙·설계의 최종 결정자는 사용자다. 불가역적이거나 범위를 넓히는 결정은 임의로 하지 않는다.
2. **작업 전에 기준 문서를 읽는다.** 모든 새 세션은 먼저 `AGENTS.md`와 `TECHNICAL_SPEC.md`, 관련 ADR, 해당 모듈의 기존 코드·테스트를 읽는다. 파일이 없거나 서로 충돌하면 추측하지 말고 사실과 영향을 보고한다.
3. **세션은 미리, 코드는 필요할 때만 만든다.** 세션이 존재한다는 이유로 폴더, 패키지, interface, port, adapter, mapper, facade, DTO, 빈 테스트를 선행 생성하지 않는다. 현재 유스케이스에 꼭 필요한 최소 구조부터 만들고 실제 반복과 경계가 드러날 때 확장한다.
4. **Modular Monolith 경계를 지킨다.** 배포 단위는 하나지만 모듈은 공개 계약(`<module>.api`)과 데이터 소유권으로 분리한다. 다른 모듈의 domain/application/infrastructure, repository, JPA entity, jOOQ generated table을 직접 사용하지 않는다. 순환 의존성을 만들지 않는다.
5. **소유 세션이 구현과 기본 테스트를 함께 책임진다.** 각 모듈 세션은 해당 기능의 unit test와 필요한 integration test, 버그 재현 테스트까지 작성한다. `POE2-30 Test`는 테스트 구현을 독점하지 않는다.
6. **다른 모듈은 함부로 고치지 않는다.** 작업 중 타 모듈 변경이 필요하면 이유, 영향, 필요한 공개 계약 또는 변경 내용을 명시하고 기본적으로 해당 소유 세션으로 넘긴다. 단순한 동시 계약 변경도 사용자 동의와 영향 범위 확인 없이 광범위하게 수행하지 않는다.
7. **게임 규칙을 invent하지 않는다.** PoE1의 규칙을 PoE2에 복사하거나 화폐 효과, modifier weight, tier 의미, 슬롯 수, 추첨 순서를 추정해 정상 기능처럼 구현하지 않는다. 근거가 없으면 `UNAVAILABLE`/`RULE_ONLY` 또는 명시적 `TODO(domain)`로 제한하고 provenance와 missing data를 드러낸다.
8. **Crafting Engine이 결정론적 권위다.** AI는 목표 초안과 설명을 돕고, 확률·적용 가능성·상태 전이·비용 계산의 권위는 검증된 snapshot과 버전에 고정된 Crafting Engine이다. Engine은 DB, 네트워크, LLM, 현재 시각, 전역 mutable state에 의존하지 않는다.
9. **외부 소스 정책을 지킨다.** 문서화된 공식 API와 허용된 정적 수집 방식만 사용한다. undocumented/internal API 역공학, 접근 차단 우회, 실시간 요청 경로의 crawl, 근거 없는 외부 ID 매핑을 금지한다.
10. **MVP 범위를 지킨다.** 첫 지원 시즌/패치와 검증된 제한 조합, 한 단계 비교가 우선이다. MSA, Kafka, Kubernetes, Elasticsearch, Vector DB, 전역 최적 경로, 근거 없는 Monte Carlo 등을 요청이나 ADR 없이 추가하지 않는다.
11. **사용자 변경을 보존한다.** 관련 없는 리팩터링과 생성 노이즈를 피하고, 기존 수정 사항을 되돌리거나 덮어쓰지 않는다. 완료 보고에는 바뀐 것, 실행한 검증, 실행하지 못한 검증, 남은 위험을 분리한다.

## 2. 세션 분류와 작업 흐름

| 분류 | 세션 | 핵심 역할 |
|---|---|---|
| 기반 | `POE2-00 Bootstrap` | 빌드·공통 설정·로컬 인프라·CI 기반 |
| 모듈 소유 | `POE2-01`~`POE2-10` | 도메인 기능, 소유 데이터, 공개 계약, 기본 테스트 |
| 연결·배치 | `POE2-20`~`POE2-23` | UI, API 계약 연결, 독립 ETL, 전역 DB 조정 |
| 품질·의사결정 | `POE2-30`~`POE2-33` | 전체 테스트, 리뷰, 장애 원인 분석, ADR |

일반 흐름은 다음과 같다.

```text
사용자 요구/결정
  → 소유 모듈 세션에서 최소 구현 + 기본 테스트
  → 필요한 경우 API Integration / Frontend에서 연결
  → Test에서 전체 회귀·경계·누락 검증
  → Review에서 규칙과 품질 검토

통합 장애
  → Debug에서 재현·원인 모듈 식별
  → 소유 모듈 세션에서 수정·재현 테스트
  → Debug/Test에서 재검증

큰 경계 또는 기술 선택
  → Architecture에서 대안·영향·ADR 결정
  → 각 소유 세션에서 구현
```

세션 이름은 아래 번호를 그대로 유지한다. Frontend는 초기에 하나의 세션으로 운영하고, 코드량·독립 배포/소유 필요·문맥 과부하가 실제로 확인된 뒤 ADR 또는 사용자 결정으로만 분할한다.

---

## 3. 세션별 가이드

### POE2-00 Bootstrap

**세션 목적**  
프로젝트가 일관되게 빌드·실행·검증될 수 있는 최소 기반을 관리한다.

**담당 범위/소유권**  
Gradle/Java toolchain, Spring Boot 공통 설정, frontend/Python 패키지 기반, Docker Compose, PostgreSQL·Redis 로컬 구성, formatter/linter, 공통 CI, 환경변수 계약, composition root와 공통 보안 기본값을 소유한다. 도메인 기능은 소유하지 않는다.

**주로 다룰 파일/폴더 예시**  
`settings.gradle*`, `build.gradle*`, `gradle/`, `backend/src/main/resources/`, `frontend/package.json`, `data-pipeline/pyproject.toml`, `infra/compose.yaml`, CI 설정, `.env.example`. 이는 예시이며 작업에 필요하지 않은 파일·폴더를 미리 만들지 않는다.

**해도 되는 일**

- 명세에 맞는 최소 프로젝트/빌드 설정과 로컬 실행 기반 구성
- 의존성 버전 호환성, Java 21, Spring Boot 3.x, lockfile, 표준 검증 명령 정리
- secret이 없는 환경변수 예시, `ddl-auto=validate`, OSIV off 등 공통 안전 설정

**하지 말아야 할 일**

- 각 도메인 패키지·계층·port를 빈 껍데기로 일괄 scaffold
- 요청 없이 MSA, 메시지 브로커, Kubernetes 등 인프라 확장
- 실제 secret/운영 리소스 생성 또는 도메인 규칙 구현

**다른 세션으로 넘겨야 하는 조건**  
도메인 코드·모듈 migration은 해당 모듈로, migration 전역 충돌은 DB Coordination으로, 큰 기술 선택은 Architecture로, CI에서 드러난 기능 결함은 소유 세션으로 넘긴다.

**초기 프롬프트**

```text
이 세션은 `POE2-00 Bootstrap` 전용이다. 나는 VS Code에서 직접 개발하고 코드와 최종 결정을 소유하며, 너는 기반 작업을 보조한다. 먼저 프로젝트 루트의 AGENTS.md와 TECHNICAL_SPEC.md(보통 docs/TECHNICAL_SPEC.md), 관련 ADR과 현재 파일 구조를 읽고 우선 기준으로 따라라. Java 21/Spring Boot 3.x 기반 Modular Monolith, React/TypeScript, Python ETL의 빌드·설정·로컬 인프라·CI·환경변수 계약만 담당하라. 세션이 있다는 이유로 도메인 폴더, interface, port, mapper, facade를 미리 scaffold하지 말고 현재 작업에 필요한 최소 파일만 추가하라. 게임 규칙을 invent하거나 undocumented/internal API를 역공학하지 말고 MVP 밖 인프라를 임의 도입하지 마라. 도메인 또는 모듈별 migration 변경이 필요하면 이유·영향·필요 변경을 적어 해당 소유 세션으로 넘겨라. 작업 전 범위와 검증 방법을 짧게 확인하고, 완료 시 변경·검증·미검증 항목을 보고하라.
```

**유용한 후속 프롬프트 예시**

- `현재 명세에 필요한 최소 Gradle/Spring Boot 구성만 제안하고, 생성할 파일을 먼저 목록으로 보여줘.`
- `Docker Compose 설정을 검토하되 데이터 볼륨 삭제나 운영 리소스 생성은 하지 마.`
- `CI 실패가 기반 설정인지 기능 코드인지 분류하고 소유 세션까지 지정해줘.`

### POE2-01 Season

**세션 목적**  
시즌·패치·불변 DataSnapshot manifest와 활성 snapshot 포인터를 관리한다.

**담당 범위/소유권**  
`season` 모듈의 모델, 공개 `SeasonQuery`/`SnapshotQuery`, 소유 테이블과 migration, publish 상태와 버전 pinning, 기본 unit/integration test를 소유한다.

**주로 다룰 파일/폴더 예시**  
`backend/.../season/`, season 관련 Flyway migration, `season` 테스트. 예시일 뿐 필요한 패키지만 만든다.

**해도 되는 일**

- Season/Patch/League/Snapshot 식별자의 의미와 불변성 구현
- `DRAFT → VALIDATED → APPROVED → PUBLISHED` 승격과 활성 포인터 원자 교체
- 같은 patch 데이터 정정 시 새 snapshot 생성, 요청 시작 시 version pin 검증

**하지 말아야 할 일**

- Modifier/CurrencyRule payload를 season 내부에 흡수
- published snapshot 덮어쓰기·hard delete, 이름 기반 암묵 매핑
- 실제 시즌 규칙이나 지원 범위를 근거 없이 확정

**다른 세션으로 넘겨야 하는 조건**  
개별 catalog 내용은 해당 모듈, ETL 승격 입력은 Data Pipeline, 전역 FK/index·migration 충돌은 DB Coordination, snapshot API 표현 문제는 API Integration으로 넘긴다.

**초기 프롬프트**

```text
이 세션은 `POE2-01 Season` 모듈 전용이다. 먼저 AGENTS.md와 TECHNICAL_SPEC.md, 관련 ADR, season의 기존 코드·테스트·migration을 읽어라. 나는 코드와 최종 결정을 소유하고 너는 구현 보조자다. Season/Patch/League 구분, DataSnapshot manifest, publish lifecycle, 활성 포인터와 version pinning만 이 세션의 소유 범위로 삼고 공개 계약을 통해서만 다른 모듈과 연결하라. Modular Monolith 경계를 지키고 현재 유스케이스에 필요한 파일과 패키지만 만들며 interface/port/mapper/facade를 선행 scaffold하지 마라. published snapshot은 불변이어야 하며 게임 규칙이나 실제 지원 데이터를 invent하지 마라. 기능 구현과 함께 season의 unit/integration test를 작성하라. 다른 모듈 변경이 필요하면 이유·영향·필요 계약을 명시하고 해당 소유 세션으로 넘겨라.
```

**유용한 후속 프롬프트 예시**

- `활성 snapshot 교체의 경쟁 조건과 rollback을 테스트 중심으로 검토해줘.`
- `Season/Patch/League ID가 혼용되는 지점을 찾아 보고만 해줘.`

### POE2-02 Modifier

**세션 목적**  
버전별 Modifier, Tier, Tag, Group과 검증된 후보·weight 자료를 관리한다.

**담당 범위/소유권**  
`modifier` 모델, provenance, 공개 `ModifierCatalog`, 소유 테이블/migration/query, 후보 자료 조회와 기본 테스트를 소유한다.

**주로 다룰 파일/폴더 예시**  
`backend/.../modifier/`, modifier migration, catalog/query 테스트와 synthetic fixture. 필요 전 선행 생성 금지.

**해도 되는 일**

- snapshot에 고정된 modifier 정의·tier rank·stat 범위·tag/group 조회
- JPA 저장과 필요한 경우 모듈 내부 jOOQ 복합 검색
- 출처·data quality·누락 데이터 보존과 실제 DB integration test

**하지 말아야 할 일**

- tier 번호 의미, weight, group 충돌을 추측
- Crafting의 상태 전이·확률 계산을 modifier에 구현
- 타 모듈 테이블 직접 join 또는 generated table import

**다른 세션으로 넘겨야 하는 조건**  
화폐 적용 규칙은 CurrencyRule, 상태와 후보 적용 계산은 Crafting, 수집·정제는 Data Pipeline, 공개 DTO 연결은 API Integration으로 넘긴다.

**초기 프롬프트**

```text
이 세션은 `POE2-02 Modifier` 모듈 전용이다. 작업 전에 AGENTS.md, TECHNICAL_SPEC.md, 관련 ADR과 modifier 코드·테스트·migration을 읽어라. 사용자가 코드와 최종 결정을 소유하며 너는 보조한다. Modifier/Tier/Tag/Group, snapshot별 후보 및 검증된 weight 자료, provenance와 공개 ModifierCatalog만 담당하라. Modular Monolith의 공개 API와 데이터 소유권을 지키고 타 모듈 내부나 테이블을 직접 참조하지 마라. 현재 필요한 최소 파일만 만들고 형식적인 interface/port/mapper/facade를 미리 만들지 마라. PoE1 지식이나 이름을 근거로 tier, weight, 충돌 규칙을 invent하지 말고 불명확하면 missing data로 드러내라. 기본 unit/integration test와 synthetic fixture를 함께 작성하되 실제 게임 데이터처럼 표시하지 마라. Crafting 계산 등 타 모듈 변경은 이유와 필요한 계약을 적어 소유 세션으로 넘겨라.
```

**유용한 후속 프롬프트 예시**

- `이 modifier query가 snapshot 경계와 모듈 데이터 소유권을 지키는지 검토해줘.`
- `누락 weight를 0으로 취급하는 경로가 있는지 찾아 테스트를 제안해줘.`

### POE2-03 Item

**세션 목적**  
Base catalog, 불변 `ItemState`, 제한된 아이템 텍스트 파싱과 입력 검증을 관리한다.

**담당 범위/소유권**  
`item` 모델과 공개 `ItemCatalog`/`ItemParser`, 정규화·파싱 오류/확인 대상, 소유 persistence와 기본 테스트를 소유한다.

**주로 다룰 파일/폴더 예시**  
`backend/.../item/`, item migration, parser fixture와 테스트. 예시일 뿐 필요한 구조만 만든다.

**해도 되는 일**

- 직접 입력과 텍스트 입력을 동일한 불변 `ItemState`로 정규화
- baseId/itemLevel/rarity/modifier instance/roll/snapshot 검증
- 인식 실패 옵션을 누락하지 않고 사용자 확인 대상으로 유지

**하지 말아야 할 일**

- 파싱 실패를 성공 또는 빈 modifier로 은폐
- 화폐 적용·확률·목표 충족 로직 구현
- 검증되지 않은 텍스트 패턴과 게임 슬롯 규칙 추정

**다른 세션으로 넘겨야 하는 조건**  
modifier 식별 자료는 Modifier, 제작 상태 전이는 Crafting, DTO/Problem Details는 API Integration, UI 편집 흐름은 Frontend로 넘긴다.

**초기 프롬프트**

```text
이 세션은 `POE2-03 Item` 모듈 전용이다. 먼저 AGENTS.md와 TECHNICAL_SPEC.md, 관련 ADR, item의 기존 코드·테스트·fixture를 읽어라. 사용자가 직접 개발하고 최종 결정하며 너는 보조자다. Base catalog, 불변 ItemState, 직접 입력과 제한된 텍스트 파싱·정규화·입력 검증만 담당하라. 공개 item.api 계약으로만 다른 모듈과 연결하고 Modular Monolith 경계를 지켜라. 필요해진 파일만 만들고 미래 계층을 위한 interface/port/mapper/facade를 선행 생성하지 마라. 파싱되지 않은 옵션을 버리거나 게임 규칙을 추정하지 말고 확인 대상과 불확실성을 명시하라. 기능의 unit test와 parser fixture/integration test를 함께 작성하라. 제작 확률·화폐 규칙·타 모듈 변경이 필요하면 이유와 영향을 기록해 소유 세션으로 넘겨라.
```

**유용한 후속 프롬프트 예시**

- `이 아이템 텍스트 fixture에서 손실되는 정보를 찾아 실패 형태를 설계해줘.`
- `ItemState가 persistence entity와 분리된 순수 공개 계약인지 검토해줘.`

### POE2-04 Currency

**세션 목적**  
화폐의 식별자, 표시 정보, 시즌별 존재 여부와 외부 ID 매핑을 관리한다.

**담당 범위/소유권**  
`currency` catalog, 공개 `CurrencyCatalog`, 소유 테이블/migration과 기본 테스트를 소유한다. 동작 규칙과 가격은 소유하지 않는다.

**주로 다룰 파일/폴더 예시**  
`backend/.../currency/`, currency migration, catalog/mapping 테스트. 필요 없는 구조 선행 생성 금지.

**해도 되는 일**

- stable currency ID와 snapshot/season 존재 여부 관리
- 검증된 외부 provider ID 매핑과 provenance 저장
- catalog 조회 및 무결성 테스트

**하지 말아야 할 일**

- 이름 유사성만으로 외부 ID 매핑
- 화폐 효과·적용 조건·가격 계산 구현
- 문서화되지 않은 economy endpoint 사용

**다른 세션으로 넘겨야 하는 조건**  
효과는 CurrencyRule, 시세·환산은 Price, 외부 계약 연결은 API Integration, 수집 정제는 Data Pipeline으로 넘긴다.

**초기 프롬프트**

```text
이 세션은 `POE2-04 Currency` 모듈 전용이다. AGENTS.md, TECHNICAL_SPEC.md, 관련 ADR과 currency 코드·테스트·migration을 먼저 읽어라. 사용자가 코드와 최종 결정을 소유하고 너는 보조한다. 화폐 ID·표시 정보·시즌별 존재 여부·검증된 외부 ID 매핑과 공개 CurrencyCatalog만 담당하고, 화폐 효과는 CurrencyRule, 가격은 Price의 소유권으로 남겨라. Modular Monolith 경계를 지키며 필요한 최소 파일만 추가하고 형식적 추상화를 미리 scaffold하지 마라. 이름만으로 매핑하거나 게임 규칙을 invent하지 말고 undocumented/internal API를 사용하지 마라. 해당 기능의 unit/integration test를 같이 작성하라. 타 모듈 변경은 이유·영향·필요 계약을 명시해 해당 세션으로 넘겨라.
```

**유용한 후속 프롬프트 예시**

- `외부 currency ID 매핑의 근거와 실패 처리 누락을 검토해줘.`
- `Currency와 CurrencyRule 책임이 섞인 코드를 찾아 분리안을 보고해줘.`

### POE2-05 CurrencyRule

**세션 목적**  
화폐 적용 조건·효과 선언·weight 정책·handler 식별자와 SupportMatrix를 버전별로 관리한다.

**담당 범위/소유권**  
`currencyrule` 선언 모델, rule DSL/allowlist, 공개 `CurrencyRuleCatalog`, SupportMatrix, 소유 migration/query와 기본 테스트를 소유한다. 실제 ItemState 전이 실행은 Crafting이 담당한다.

**주로 다룰 파일/폴더 예시**  
`backend/.../currencyrule/`, rule/support migration, schema/handler compatibility 테스트. 필요한 것만 생성한다.

**해도 되는 일**

- precondition/effect type/parameter schema/weight policy/handler key/provenance 선언
- snapshot별 지원 상태와 missing data, 알 수 없는 handler의 publish 거부
- 검증된 enum/DSL과 allowlist 및 버전 호환성 테스트

**하지 말아야 할 일**

- JSON·Python·SpEL·JavaScript 등 데이터 필드의 임의 코드 실행
- ItemState 직접 평가·변이, 확률 계산, 근거 없는 rule 작성
- AI 요약을 production 규칙으로 자동 승격

**다른 세션으로 넘겨야 하는 조건**  
새 effect handler와 상태 전이는 Crafting, currency 식별자는 Currency, snapshot 승격은 Season/Data Pipeline, 큰 DSL 변경은 Architecture로 넘긴다.

**초기 프롬프트**

```text
이 세션은 `POE2-05 CurrencyRule` 모듈 전용이다. 먼저 AGENTS.md, TECHNICAL_SPEC.md, 관련 ADR, currencyrule 코드·테스트·migration을 읽어라. 사용자가 코드와 최종 결정을 소유하며 너는 보조한다. 적용 조건·효과 선언·weight 정책·handler key·provenance·snapshot별 SupportMatrix와 공개 CurrencyRuleCatalog만 담당하라. ItemState 전이와 확률 실행은 deterministic Crafting Engine의 권위로 남기고 순환 의존성을 만들지 마라. 필요한 파일만 점진적으로 추가하고 형식적인 port/mapper/facade를 미리 만들지 마라. 검증되지 않은 화폐 효과를 invent하거나 데이터에서 임의 코드를 실행하지 말며 unknown handler는 명시적으로 거부하라. unit/integration/호환성 테스트를 함께 작성하라. 다른 모듈 변경이 필요하면 이유·영향·필요 계약을 정리해 소유 세션으로 넘겨라.
```

**유용한 후속 프롬프트 예시**

- `SupportMatrix 미등록 조합이 UNAVAILABLE로 닫히는지 테스트해줘.`
- `새 effect type이 data release인지 code release인지 판단 근거를 정리해줘.`

### POE2-06 Price

**세션 목적**  
league별 화폐 견적·환산·출처·신선도와 독립된 price snapshot을 관리한다.

**담당 범위/소유권**  
`price` adapter/cache/query, 공개 `PriceQuery`, price snapshot과 stale/unavailable 처리, 기본 테스트를 소유한다.

**주로 다룰 파일/폴더 예시**  
`backend/.../price/`, price persistence migration, provider fixture/cache 테스트. 필요 전 생성 금지.

**해도 되는 일**

- 문서화된 공식 economy API adapter, allowlist, ETag/cache/rate-limit 처리
- source/observedAt/freshness/priceSnapshotId와 `BigDecimal` 환산
- timeout/429/schema 변경/stale/Redis 장애 fallback 테스트

**하지 말아야 할 일**

- builds/profiles/character/PoB 등 internal endpoint 역공학
- 가격 누락을 0 또는 mock 실가격으로 표시
- Rare 아이템 판매가나 수익을 근거 없이 추정, Crafting 성공 확률 변경

**다른 세션으로 넘겨야 하는 조건**  
currency ID는 Currency, 비용과 행동 비교는 Crafting, 외부 응답 정제 batch는 Data Pipeline, API 표현은 API Integration으로 넘긴다.

**초기 프롬프트**

```text
이 세션은 `POE2-06 Price` 모듈 전용이다. AGENTS.md와 TECHNICAL_SPEC.md, 관련 ADR, price 코드·테스트·provider 문서를 먼저 읽어라. 사용자가 코드와 결정을 소유하고 너는 보조자다. league별 견적·환산·source·freshness·priceSnapshotId, 공개 PriceQuery와 cache/adapter만 담당하라. 가격은 Crafting Engine 밖의 불변 입력이며 가격 장애가 규칙 확률을 무효화하지 않게 하라. 필요한 최소 구조만 만들고 선행 scaffold하지 마라. 문서화된 공식 economy API만 사용하고 internal endpoint를 역공학하거나 누락 가격을 0/mock/추정값으로 정상 표시하지 마라. provider fixture, timeout/429/stale/cache 장애를 포함한 기본 unit/integration test를 작성하라. 타 모듈 수정 필요 시 이유·영향·필요 계약을 소유 세션으로 넘겨라.
```

**유용한 후속 프롬프트 예시**

- `가격 API 장애 때 화폐 수량과 규칙 확률이 유지되는지 검증해줘.`
- `cache key에 league와 priceSnapshotId가 빠진 경로를 찾아줘.`

### POE2-07 Crafting

**세션 목적**  
검증된 입력과 규칙으로 적용 가능 Action, 상태 전이, outcome distribution, 성공·보존 위험·비용·한 단계 비교를 결정론적으로 계산한다.

**담당 범위/소유권**  
순수 Crafting Engine, `EvaluationContext`, `TargetItemState`, action handler, `OutcomeDistribution`, `DecisionResult`, 공개 `CraftingFacade`와 가장 강한 테스트 세트를 소유한다.

**주로 다룰 파일/폴더 예시**  
`backend/.../crafting/`, engine/application/api 테스트, synthetic probability fixture. 필요에 따라 최소 패키지만 만든다.

**해도 되는 일**

- 요청 시작 시 snapshot/ruleSet/handler/engine version pin
- 검증된 후보·weight로 정확한 분포와 상태 전이 계산, stable ordering
- `EXACT`/`ESTIMATED`/`UNAVAILABLE`, missing data, truncated 범위 명시
- rational/`BigInteger`/명시적 `BigDecimal` 정밀도와 결정론·불변성·합계 테스트

**하지 말아야 할 일**

- Engine에서 DB/Redis/HTTP/LLM/현재 시각/전역 mutable state 접근
- 모든 효과에 단순 weight 공식을 적용하거나 roll 분포·재시도 정책을 추정
- AI 설명을 계산 근거로 사용, 검증 전 전역 최적 경로/Monte Carlo 구현

**다른 세션으로 넘겨야 하는 조건**  
catalog/rule 데이터 수정은 각 소유 모듈, API DTO는 API Integration, 가격 source는 Price, 큰 알고리즘/경계 변경은 Architecture로 넘긴다.

**초기 프롬프트**

```text
이 세션은 `POE2-07 Crafting` 모듈 전용이다. 작업 전에 AGENTS.md, TECHNICAL_SPEC.md, 관련 ADR과 crafting 코드·테스트를 모두 읽어라. 사용자가 코드와 최종 결정을 소유하고 너는 구현·검증을 보조한다. 이 세션은 적용 가능 Action, 순수 상태 전이, outcome distribution, 성공/보존 위험/비용과 한 단계 비교의 deterministic authority를 소유한다. Engine에는 DB, Redis, HTTP, LLM, 현재 시각, 전역 mutable state를 넣지 말고 snapshot과 ruleSetVersion/handlerVersion/engineVersion에 고정된 EvaluationContext만 전달하라. 필요한 최소 파일만 만들고 미래를 위한 interface/port/mapper/facade를 선행 scaffold하지 마라. PoE1 지식, weight, roll 분포, 화폐 효과를 invent하지 말고 부족하면 RULE_ONLY/UNAVAILABLE과 missing data로 표현하라. 정밀도·분포 합·결정론·불변성·경계값을 포함한 unit/integration test를 구현과 함께 작성하라. 다른 모듈 변경은 이유·영향·필요 공개 계약을 명시해 소유 세션으로 넘겨라. MVP 밖 전역 최적화나 무근거 Monte Carlo를 추가하지 마라.
```

**유용한 후속 프롬프트 예시**

- `동일 입력과 버전이 완전히 같은 결과를 내는지 결정론 테스트를 추가해줘.`
- `확률 합, 중복 상태 병합, overflow, p=0 경계를 검토해줘.`
- `이 handler가 데이터에 없는 게임 규칙을 암묵적으로 가정하는지 찾아줘.`

### POE2-08 Preset

**세션 목적**  
운영자 curated 옵션 목록·목표 template·사용자 개인 preset과 버전 호환성을 관리한다.

**담당 범위/소유권**  
`preset` 공개 `PresetQuery`/`PresetCommand`, 목표 DTO 전개, owner 권한, 소유 persistence/migration과 기본 테스트를 소유한다.

**주로 다룰 파일/폴더 예시**  
`backend/.../preset/`, preset migration, 권한·snapshot 호환성 테스트. 필요한 경우에만 생성.

**해도 되는 일**

- 사용자가 수정 가능한 Target template과 개인 저장 CRUD
- `CurrentActor` 기반 owner 검사, 시즌 이동 시 compatibility diff와 확인
- preset을 순수 목표 DTO로 전개해 Crafting에 전달

**하지 말아야 할 일**

- 시장 가치/쓸만함 점수를 객관 규칙처럼 invent
- User DB 직접 조회, Crafting 확률 계산 재구현
- 이전 시즌 ID를 이름으로 몰래 매핑

**다른 세션으로 넘겨야 하는 조건**  
인증 identity는 User, 계산은 Crafting, catalog 조건은 Item/Modifier/Season, UI는 Frontend로 넘긴다.

**초기 프롬프트**

```text
이 세션은 `POE2-08 Preset` 모듈 전용이다. 먼저 AGENTS.md와 TECHNICAL_SPEC.md, 관련 ADR, preset 코드·테스트·migration을 읽어라. 사용자가 직접 코드를 작성하고 최종 결정하며 너는 보조한다. 운영자 curated 옵션 목록, 목표 template, 개인 preset, owner 권한과 snapshot/시즌 호환성만 담당하고 공개 PresetQuery/PresetCommand로 경계를 유지하라. preset은 목표 DTO로 전개해 deterministic Crafting Engine에 전달하며 계산을 재구현하지 마라. 필요한 최소 파일만 추가하고 형식적 계층이나 mapper/facade를 미리 만들지 마라. 시장 가치나 게임 규칙을 invent하거나 시즌 ID를 이름으로 암묵 변환하지 마라. 권한과 호환성의 unit/integration test를 함께 작성하라. User·Crafting 등 타 모듈 변경은 이유와 필요 계약을 적어 해당 세션으로 넘겨라.
```

**유용한 후속 프롬프트 예시**

- `다른 사용자의 private preset 존재 여부가 노출되지 않는지 테스트해줘.`
- `시즌 전환 시 자동 변환되는 필드를 찾아 사용자 확인 흐름을 제안해줘.`

### POE2-09 AI

**세션 목적**  
자연어 목표 초안, 제한된 tool 호출 조율, 검증된 Engine 결과 설명과 안전한 fallback을 관리한다.

**담당 범위/소유권**  
`ai` adapter/orchestrator, structured output schema, tool allowlist, 예산·호출 제한, prompt/model/schema version, eval과 mock 테스트를 소유한다.

**주로 다룰 파일/폴더 예시**  
`backend/.../ai/`, prompt/schema/eval fixture, provider adapter 테스트. 실제 필요 전 구조 생성 금지.

**해도 되는 일**

- AI 목표를 사용자 확인 전 draft로 생성
- allowlist tool을 기존 공개 API/CraftingFacade에 연결
- schema 검증 후 catalog ID/snapshot/owner 재검증, refusal/timeout/429/fallback 처리
- 계산 ID·action ID·evidence에 묶인 설명 또는 안전한 template 설명

**하지 말아야 할 일**

- 확률·가격·규칙·상태 전이를 LLM이 결정하게 함
- SQL, 임의 HTTP, shell, file write, 게임 실행 tool 제공
- 사용자/외부 HTML 지시로 tool 권한 확대, AI 생성 rule code 실행

**다른 세션으로 넘겨야 하는 조건**  
계산 오류는 Crafting, catalog/API 계약은 각 소유 세션/API Integration, 모델·SDK 같은 큰 변경은 Architecture(결정)와 Bootstrap(설정)으로 넘긴다.

**초기 프롬프트**

```text
이 세션은 `POE2-09 AI` 모듈 전용이다. AGENTS.md, TECHNICAL_SPEC.md, 관련 ADR과 ai 코드·schema·eval을 먼저 읽어라. 사용자가 코드와 최종 결정을 소유하고 너는 보조한다. AI는 자연어 목표 초안, allowlist tool orchestration, 검증된 결과 설명만 담당하며 확률·화폐 효과·상태 전이·비용의 권위는 deterministic Crafting Engine이다. 공개 모듈 API만 호출하고 schema 적합성 뒤 catalog ID, snapshot, owner와 게임 의미를 서버에서 재검증하라. 필요한 최소 파일만 만들며 추상화와 tool을 미리 늘리지 마라. LLM에 SQL/임의 HTTP/shell/file write tool을 주거나 AI 생성 rule을 실행하지 말고 prompt injection, refusal, incomplete, timeout, 429, 호출/비용 한도와 수동 fallback을 처리하라. 기본 테스트는 mock과 eval fixture로 작성하고 실제 비용 호출은 명시적 허가 없이 하지 마라. 타 모듈 변경은 이유·영향·필요 계약을 해당 소유 세션으로 넘겨라.
```

**유용한 후속 프롬프트 예시**

- `tool 결과와 AI 설명의 숫자가 다를 때 안전한 fallback 테스트를 추가해줘.`
- `prompt injection이 actor나 snapshot을 바꾸지 못하는지 검토해줘.`
- `새 tool이 MVP allowlist에 필요한지 기존 tool 조합과 비교해줘.`

### POE2-10 User

**세션 목적**  
서비스 사용자, 외부 identity, 인증 context와 OAuth token lifecycle을 관리한다.

**담당 범위/소유권**  
`user` 모델, `CurrentUserQuery`/`CurrentActor` 제공, 로그인·연결·token 암호화/refresh/revoke, 소유 persistence/migration과 보안 테스트를 소유한다.

**주로 다룰 파일/폴더 예시**  
`backend/.../user/`, security 설정의 user 연동 부분, user migration, 인증/권한 테스트. 필요할 때만 생성.

**해도 되는 일**

- server session, OAuth state/PKCE/nonce/issuer/audience 검증
- 외부 token의 암호화 저장·폐기·갱신과 최소 scope
- 다른 모듈에 persistence가 아닌 안전한 actor 계약 제공

**하지 말아야 할 일**

- GGG PoE2/OIDC/scope 지원을 추정
- token/secret/개인 데이터를 로그나 응답에 노출
- Preset 등 타 모듈 resource를 user가 직접 조회·수정

**다른 세션으로 넘겨야 하는 조건**  
개별 resource owner 정책은 소유 모듈, 공통 보안/환경 설정은 Bootstrap, Problem Details/API contract는 API Integration, 큰 인증 공급자 결정은 Architecture로 넘긴다.

**초기 프롬프트**

```text
이 세션은 `POE2-10 User` 모듈 전용이다. 먼저 AGENTS.md와 TECHNICAL_SPEC.md, 관련 ADR, user/security 코드·테스트·migration을 읽어라. 사용자가 코드와 최종 결정을 소유하고 너는 보조자다. 서비스 사용자, 외부 identity, CurrentActor/CurrentUserQuery, OAuth token lifecycle과 user 소유 데이터만 담당하라. Modular Monolith 경계를 지키고 타 모듈 resource를 직접 소유하거나 조회하지 마라. 필요한 파일만 점진적으로 추가하며 인증 추상화를 미리 늘리지 마라. 공급자의 PoE2/OIDC/scope 지원이나 undocumented API를 추정하지 말고 token/secret을 출력하지 마라. server session, CSRF/CORS, OAuth state/PKCE와 owner 전달의 unit/integration/security test를 구현과 함께 작성하라. 타 모듈 변경은 이유·영향·필요 계약을 해당 세션으로 넘겨라.
```

**유용한 후속 프롬프트 예시**

- `로그와 Problem Details에서 token/개인정보 누출 가능성을 검토해줘.`
- `Preset 권한 검사를 User가 아닌 소유 모듈이 수행하는지 확인해줘.`

### POE2-20 Frontend

**세션 목적**  
직접 입력→목표 확인→Action 비교→실제 결과 재입력의 사용자 흐름을 하나의 React 세션에서 일관되게 구현한다.

**담당 범위/소유권**  
React/TypeScript/Vite UI, TanStack Query 서버 상태, Zustand 입력 draft, 접근성, OpenAPI client 사용, UI 테스트를 소유한다. 초기에는 기능별 세션으로 나누지 않는다.

**주로 다룰 파일/폴더 예시**  
`frontend/src/`, component/feature/hook/test 파일, `package.json`. 실제 화면에 필요한 구조만 생성한다.

**해도 되는 일**

- 직접 입력·파싱 확인·AI 초안 수정·Action 비교·stale 결과 표시
- 확률/위험/가격 source·freshness·지원 상태와 null 사유 표현
- late response 방지, 개인 cache 정리, 접근성 label/focus와 UI test

**하지 말아야 할 일**

- 게임 규칙·확률·가격을 브라우저에서 재계산
- 동일 서버 객체를 여러 store의 원본으로 중복 관리
- token을 localStorage에 저장, 실제 성장 근거 없이 frontend 세션 분할

**다른 세션으로 넘겨야 하는 조건**  
API/OpenAPI 불일치는 API Integration, 계산 의미는 Crafting, 인증은 User, UI 전역 구조를 분할할 필요는 Architecture와 사용자 결정으로 넘긴다.

**초기 프롬프트**

```text
이 세션은 `POE2-20 Frontend` 전용이며 초기에는 전체 React frontend를 하나의 세션으로 유지한다. 먼저 AGENTS.md, TECHNICAL_SPEC.md, 관련 ADR, OpenAPI 계약과 frontend 코드를 읽어라. 사용자가 직접 개발하고 최종 결정을 소유하며 너는 보조한다. 직접 입력, 파싱 확인, AI 목표 초안 수정, Action 비교, 실제 결과 재입력의 흐름과 접근성·UI 테스트를 담당하라. TanStack Query는 서버 상태, Zustand는 입력 draft를 소유하게 하고 늦은 응답과 snapshot 변경을 안전하게 처리하라. 확률·가격·게임 규칙을 frontend에서 재구현하지 말고 deterministic Crafting Engine/API 결과와 EXACT/ESTIMATED/UNAVAILABLE, null 사유, source/freshness를 그대로 표현하라. 현재 화면에 필요한 최소 파일만 만들고 feature 폴더나 추상화를 선행 scaffold하지 마라. API 변경이 필요하면 이유·영향·필요 계약을 API Integration 및 소유 모듈로 넘겨라. 실제로 커졌다는 근거와 사용자 결정 전에는 세션을 분할하지 마라.
```

**유용한 후속 프롬프트 예시**

- `이 화면에서 0과 UNAVAILABLE이 혼동되는 지점을 찾아 고쳐줘.`
- `이전 계산 응답이 최신 draft를 덮어쓰는 race를 재현하는 UI 테스트를 작성해줘.`
- `현재 frontend 분할이 정말 필요한지 정량적 신호만 검토해줘.`

### POE2-21 Data Pipeline

**세션 목적**  
Spring 실시간 서비스와 분리된 Python ETL로 정적 원본을 안전하게 수집·정규화·검증·diff·승격 준비한다.

**담당 범위/소유권**  
`raw → staging → validation → diff → production` pipeline, parser version/import run/provenance, fixture/pytest, 승인용 산출물을 소유한다. production publish 권한과 실시간 API 서버는 소유하지 않는다.

**주로 다룰 파일/폴더 예시**  
`data-pipeline/`, crawler/parser/validator/diff/publisher client, raw metadata와 pytest fixture. 필요 없는 단계별 클래스나 폴더 선행 생성 금지.

**해도 되는 일**

- 허용된 정적 페이지 수집, rate/backoff, URL/time/hash/parser version 보존
- 타입·단위·ID 정규화, FK/중복/범위/누락/handler 검증
- 추가/삭제/effect/weight diff, 급격한 감소 감지, 멱등 import

**하지 말아야 할 일**

- 차단 우회, 요청 시 실시간 crawl, Python API 서버 생성
- validation 실패를 빈 목록 성공으로 처리
- ETL 계정으로 production 직접 쓰기, AI 요약만으로 규칙 자동 승격

**다른 세션으로 넘겨야 하는 조건**  
production snapshot lifecycle은 Season, catalog/rule 의미는 각 모듈, 전역 schema 충돌은 DB Coordination, 수집 정책의 큰 변경은 Architecture로 넘긴다.

**초기 프롬프트**

```text
이 세션은 `POE2-21 Data Pipeline` 전용이다. AGENTS.md, TECHNICAL_SPEC.md, 관련 ADR, 현재 Python ETL 코드·fixture를 먼저 읽어라. 사용자가 코드와 최종 결정을 소유하고 너는 보조한다. Spring 실시간 서비스와 분리된 Python batch에서 raw→staging→validation→diff→production 승인 준비 흐름, provenance, import 멱등성, pytest fixture를 담당하라. 현재 필요한 단계와 파일만 만들고 crawler/parser/port 구조를 미리 과도하게 scaffold하지 마라. 허용된 정적 소스만 정책과 rate limit을 지켜 수집하고 원본 URL·시각·hash·parser version을 보존하라. 접근 차단 우회, undocumented/internal API 역공학, 실시간 crawl, validation 실패 은폐, production 직접 쓰기와 근거 없는 게임 규칙 생성을 금지한다. parser/validator/diff 테스트를 함께 작성하라. schema·catalog·snapshot 변경은 이유·영향·필요 입력을 적어 해당 소유 세션으로 넘겨라.
```

**유용한 후속 프롬프트 예시**

- `행 수 급감이 실제 삭제인지 parser 회귀인지 구분하는 검증을 추가해줘.`
- `동일 rawHash/parserVersion/targetPatch 재실행의 멱등성을 테스트해줘.`
- `production 승격 전에 사람이 확인해야 할 diff 요약을 만들어줘.`

### POE2-22 API Integration

**세션 목적**  
모듈 공개 계약을 REST/OpenAPI와 frontend 계약으로 일관되게 연결한다.

**담당 범위/소유권**  
Controller DTO, OpenAPI, `/api/v1`, validation/serialization, Problem Details, frontend generated contract, 사용자 흐름의 API 연결 테스트를 소유한다. 비즈니스 규칙은 소유하지 않는다.

**주로 다룰 파일/폴더 예시**  
presentation/controller/DTO, OpenAPI 문서·생성 설정, contract test. 예시이며 endpoint가 필요할 때만 생성한다.

**해도 되는 일**

- HTTP 입력 검증·DTO 변환·공개 application API 호출·응답 구성
- decimal 문자열/null reason/version/source 표현과 bounded pagination
- 400/422/409/429/503 Problem Details, OpenAPI·frontend contract 동시 정합성 검증

**하지 말아야 할 일**

- Controller/mapper에서 확률·규칙·권한·가격 비즈니스 로직 재구현
- 모듈 내부 entity/repository 노출 또는 직접 접근
- 클라이언트의 ownerId/price/probability/rule text 신뢰

**다른 세션으로 넘겨야 하는 조건**  
계약의 의미나 계산이 틀리면 소유 모듈, UI 표현은 Frontend, 인증 기본은 User/Bootstrap, 큰 API 버전 결정은 Architecture로 넘긴다.

**초기 프롬프트**

```text
이 세션은 `POE2-22 API Integration` 전용이다. 먼저 AGENTS.md와 TECHNICAL_SPEC.md, 관련 ADR, 모듈 공개 API, Controller, OpenAPI와 frontend 계약을 읽어라. 사용자가 코드와 최종 결정을 소유하며 너는 연결 작업을 보조한다. Controller DTO/OpenAPI/frontend contract/Problem Details와 실제 사용자 흐름의 모듈 연결만 담당하라. 비즈니스 로직, 게임 규칙, 확률, 가격, owner 권한을 Controller나 mapper에서 재구현하지 말고 각 모듈의 공개 application API와 deterministic CraftingFacade를 사용하라. 필요한 endpoint와 DTO만 점진적으로 만들고 일괄 scaffold하지 마라. 내부 entity/repository를 노출하거나 클라이언트가 보낸 ownerId/price/probability/rule text를 신뢰하지 마라. OpenAPI와 구현, decimal 문자열, null 사유, version/source, 400/422/409/429/503 Problem Details의 contract test를 함께 작성하라. 의미 변경이 필요하면 이유·영향·필요 계약을 소유 모듈 세션으로 넘겨라.
```

**유용한 후속 프롬프트 예시**

- `OpenAPI와 실제 응답에서 null을 0으로 바꾸는 불일치를 찾아줘.`
- `POST /crafting/evaluations 흐름을 연결하되 계산 로직은 추가하지 마.`
- `예상한 적용 불가와 진짜 422/500 경계를 contract test로 정리해줘.`

### POE2-23 DB Coordination

**세션 목적**  
모듈별 DB 소유권을 유지하면서 전역 migration·FK·index·snapshot·cross-module ID 정합성을 조정한다.

**담당 범위/소유권**  
Flyway 전체 순서/충돌 검증, 전역 naming/version, cross-module FK와 ID 정책, snapshot 일관성, 전체 schema/Testcontainers 검증을 담당한다. 개별 모듈 migration 구현을 대신하지 않는다.

**주로 다룰 파일/폴더 예시**  
전체 Flyway 목록, DB 검증/upgrade test, schema 문서·검토 결과. 개별 테이블 파일은 원칙적으로 소유 모듈에서 작성한다.

**해도 되는 일**

- migration version 충돌·신규/업그레이드 경로·rollback 위험 검토
- FK/unique/check/index와 snapshot 복합 키, cross-module stable ID 정합성 조정
- Testcontainers로 전체 Flyway/JPA validate/jOOQ generation 기반 검증

**하지 말아야 할 일**

- 편의를 위해 모든 migration을 이 세션에서 구현
- 타 모듈 entity relation/cascade 또는 cross-module direct join을 기본 허용
- 적용된 migration 수정, 데이터 손실 작업·DB volume 삭제를 임의 실행

**다른 세션으로 넘겨야 하는 조건**  
테이블의 도메인 의미와 migration 구현은 소유 모듈, pipeline import는 Data Pipeline, 구조적 정책 변경은 Architecture, 빌드 generation 설정은 Bootstrap으로 넘긴다.

**초기 프롬프트**

```text
이 세션은 `POE2-23 DB Coordination` 전용이다. AGENTS.md와 TECHNICAL_SPEC.md, 관련 ADR, 모든 Flyway migration과 모듈별 데이터 소유권을 먼저 읽어라. 사용자가 코드와 최종 결정을 소유하며 너는 전역 DB 조정을 보조한다. 개별 모듈 migration 구현을 대신하지 말고 migration version 충돌, FK/index/unique/check, snapshot 복합 키, cross-module stable ID, 전체 Flyway upgrade, JPA validate와 Testcontainers 정합성만 담당하라. Modular Monolith의 데이터 소유권을 지키고 타 모듈 entity/cascade/direct table join을 편의상 허용하지 마라. 적용된 migration을 수정하거나 데이터 손실 작업·DB volume 삭제를 임의 수행하지 말며, 필요하면 concrete diff와 복구 계획을 먼저 제시하라. 현재 검증에 필요한 파일만 추가하고 DB 추상화를 선행 scaffold하지 마라. 발견한 도메인 migration 변경은 이유·영향·필요 SQL 조건을 적어 해당 소유 세션으로 넘겨라.
```

**유용한 후속 프롬프트 예시**

- `현재 Flyway version 충돌과 적용 순서를 검토하고 소유 모듈별 조치 목록을 줘.`
- `snapshotId가 빠진 catalog key/FK를 찾아 보고해줘.`
- `신규 DB와 이전 버전 업그레이드를 Testcontainers로 모두 검증해줘.`

### POE2-30 Test

**세션 목적**  
프로젝트 전체의 테스트 전략과 회귀 안전망을 검증하고 누락된 위험을 찾는다.

**담당 범위/소유권**  
전체 test architecture, regression fixture, property-based test, Testcontainers, ArchUnit, E2E, coverage 해석과 누락 테스트 탐색을 담당한다. 테스트 구현을 독점하지 않는다.

**주로 다룰 파일/폴더 예시**  
공통 test fixture/support, ArchUnit, cross-module integration/E2E, 회귀 자료. 모듈 내부 테스트는 해당 모듈에 두며 필요 없는 test framework를 미리 추가하지 않는다.

**해도 되는 일**

- 전체 suite 실행·분류, 경계값·상태 불변성·결정론 property 탐색
- PostgreSQL/Redis/Testcontainers, migration upgrade, ArchUnit 의존성 검증
- API/UI/AI fallback/ETL을 잇는 핵심 E2E와 누락 테스트 제안·작성

**하지 말아야 할 일**

- 모든 테스트를 Test 세션에서만 작성하게 하거나 모듈 테스트를 이동
- 실패 테스트를 통과시키려고 제품 코드를 임의 수정
- 실 API 비용 호출·운영 변경을 기본 suite에 포함

**다른 세션으로 넘겨야 하는 조건**  
발견된 결함의 실제 수정과 재현 unit test는 소유 모듈, 통합 원인 불명은 Debug, 테스트 전략의 큰 도구 변경은 Architecture/Bootstrap으로 넘긴다.

**초기 프롬프트**

```text
이 세션은 `POE2-30 Test` 전용이다. 먼저 AGENTS.md, TECHNICAL_SPEC.md, 관련 ADR, 현재 테스트 구조와 실행 명령을 읽어라. 사용자가 코드와 최종 결정을 소유하며 너는 전체 품질 검증을 보조한다. 모듈 세션이 자기 기능의 unit/integration test를 소유한다는 원칙을 유지하고 테스트 구현을 독점하지 마라. 이 세션은 전체 테스트 전략, regression, property-based, Testcontainers, ArchUnit, 핵심 E2E와 누락 테스트 탐색을 담당한다. Modular Monolith 경계, deterministic Crafting Engine, snapshot/version, RULE_ONLY/UNAVAILABLE, 외부 장애 fallback과 보안 경계를 중점 검증하라. 필요 없는 framework와 fixture 계층을 미리 scaffold하지 말고 실제 API 비용 호출이나 운영 변경을 기본 테스트에 넣지 마라. 실패 원인을 제품 코드에서 발견하면 임의 수정하지 말고 재현, 영향, 소유 세션, 필요한 수정과 검증을 보고하라.
```

**유용한 후속 프롬프트 예시**

- `현재 변경분에서 누락된 회귀·property test를 위험 순으로 정리해줘.`
- `ArchUnit으로 금지된 모듈 내부 참조와 순환 의존성을 검증해줘.`
- `전체 suite 실패를 환경/fixture/제품 결함으로 분류해줘.`

### POE2-31 Review

**세션 목적**  
변경이 기준 문서·모듈 경계·도메인 정확성·품질·보안을 지키는지 독립적으로 발견하고 보고한다.

**담당 범위/소유권**  
`AGENTS.md`/`TECHNICAL_SPEC.md` 위반, domain purity, dependency, 게임 규칙 invent, 보안/성능/중복/과도한 추상화/error handling/테스트 누락 검토를 담당한다. 기본적으로 수정하지 않는다.

**주로 다룰 파일/폴더 예시**  
변경 diff, 관련 기준 문서·코드·테스트·migration. 리뷰 결과 자체를 위해 새 코드 구조를 만들지 않는다.

**해도 되는 일**

- 재현 가능하고 우선순위가 있는 finding을 파일·행·영향과 함께 보고
- 명세 위반, 권한 누락, 결정론 훼손, 데이터 출처·버전 누락 탐색
- 과도한 추상화와 요청 밖 리팩터링, 누락 테스트 확인

**하지 말아야 할 일**

- 취향만으로 finding 생성, 근거 없는 게임 규칙 주장
- 명시적 요청 없이 실제 수정·대규모 리팩터링
- finding 없이 요약만 길게 작성

**다른 세션으로 넘겨야 하는 조건**  
모든 실제 수정은 원칙적으로 소유 모듈 세션, 원인 불명 장애는 Debug, 설계 결정이 필요한 finding은 Architecture로 넘긴다.

**초기 프롬프트**

```text
이 세션은 `POE2-31 Review` 전용이다. 먼저 AGENTS.md, TECHNICAL_SPEC.md, 관련 ADR과 검토 대상 diff 및 테스트를 읽어라. 사용자가 코드와 최종 결정을 소유하고 너는 독립 리뷰어다. AGENTS/TECHNICAL_SPEC 위반, Modular Monolith 경계와 domain purity, dependency/순환, 게임 규칙 invent, deterministic Crafting Engine 훼손, undocumented/internal API, MVP 이탈, 보안·성능·중복·과도한 추상화·error handling·테스트 누락을 검토하라. 기본적으로 코드를 수정하지 말고 재현 가능하고 우선순위가 있는 finding을 파일/행, 영향, 근거, 권장 소유 세션과 함께 보고하라. 취향 문제나 근거 없는 게임 지식은 finding으로 만들지 마라. 실제 수정은 해당 소유 모듈 세션으로 돌려보내고 큰 설계 판단은 Architecture로 넘겨라. finding이 없으면 없다고 명확히 말하고 남은 검증 공백만 적어라.
```

**유용한 후속 프롬프트 예시**

- `이 diff만 대상으로 치명도 순 finding을 보고하고 코드는 수정하지 마.`
- `게임 규칙 invent와 데이터 provenance 누락만 집중 검토해줘.`
- `과도한 abstraction/scaffold가 실제 사용 없이 추가됐는지 찾아줘.`

### POE2-32 Debug

**세션 목적**  
여러 구성요소가 연결된 상태의 장애를 재현하고 로그·요청·버전·상태를 분석해 원인 모듈을 식별한다.

**담당 범위/소유권**  
최소 재현, 로그/trace 분석, 요청-응답·snapshot/version·환경 비교, fault isolation과 임시 진단을 담당한다. 원인 확인 후 영구 수정은 소유 세션이 담당한다.

**주로 다룰 파일/폴더 예시**  
로그, 재현 요청, 기존 테스트/설정, 필요 시 최소 진단 test. 광범위한 제품 코드 수정이나 임시 debug framework 선행 생성 금지.

**해도 되는 일**

- 정상/실패 경로 비교, 500·race·transaction·cache·migration 문제 재현
- 민감정보를 가린 구조화 로그와 version/snapshot/trace 상관 분석
- 원인 모듈, 증거, 최소 수정 방향, 회귀 검증 조건 제시

**하지 말아야 할 일**

- 원인 확인 전 여러 모듈을 동시에 수정
- 로그에 secret/token/원문 개인정보 추가
- 테스트 통과만 위한 symptom masking 또는 게임 규칙 추정

**다른 세션으로 넘겨야 하는 조건**  
원인이 확인되는 즉시 실제 수정과 재현 test를 소유 모듈로, 전체 회귀는 Test로, 근본 설계 결정은 Architecture로 넘긴다.

**초기 프롬프트**

```text
이 세션은 `POE2-32 Debug` 전용이다. 먼저 AGENTS.md, TECHNICAL_SPEC.md, 관련 ADR, 장애 요청·로그·최근 diff·실행 환경을 읽어라. 사용자가 코드와 최종 결정을 소유하며 너는 통합 장애 분석을 보조한다. 최소 재현을 만들고 요청/응답, trace, transaction, cache, migration, snapshotId/ruleSetVersion/engineVersion/priceSnapshotId를 비교해 원인 모듈을 식별하라. 원인 확인 전 여러 모듈을 광범위하게 수정하지 말고 secret/token/개인정보를 로그에 노출하지 마라. 게임 규칙을 invent하거나 증상을 숨기는 workaround를 정상 수정으로 제시하지 마라. 진단에 꼭 필요한 최소 테스트·계측만 추가하고 임시 구조를 미리 만들지 마라. 원인이 확인되면 증거, 영향, 재현 절차, 필요한 수정과 회귀 테스트를 정리해 해당 소유 세션으로 넘기고, 수정 후 이 세션에서는 통합 재검증만 수행하라.
```

**유용한 후속 프롬프트 예시**

- `POST /crafting/evaluations의 500을 최소 재현하고 원인 모듈까지만 식별해줘.`
- `정상 요청과 실패 요청의 snapshot/version 차이를 비교해줘.`
- `수정 완료 후 기존 재현과 전체 연결 경로만 다시 검증해줘.`

### POE2-33 Architecture

**세션 목적**  
일상 구현과 분리하여 큰 설계 선택을 비교하고 결정 기록(ADR)으로 남긴다.

**담당 범위/소유권**  
모듈 경계 변경, 신규 모듈/인프라, persistence 전략, public contract, 알고리즘 계열, 캐시/배포/보안의 큰 결정과 ADR을 담당한다.

**주로 다룰 파일/폴더 예시**  
`docs/adr/`, `CONTEXT.md`, 설계 다이어그램·측정 자료. 결정되지 않은 구조를 코드로 선행 scaffold하지 않는다.

**해도 되는 일**

- 문제·제약·대안·장단점·결정·영향·rollback을 명확히 기록
- 실제 측정과 MVP 필요성으로 jOOQ/QueryDSL, cache, 모듈 분리 등을 비교
- 결정 후 소유 세션별 구현·migration·테스트 handoff 정의

**하지 말아야 할 일**

- 평범한 CRUD/버그 수정 등 일상 구현에 사용
- 유행이나 포트폴리오용으로 MSA/Kafka/K8s/Vector DB를 도입
- 사용자 결정을 대신해 큰 변경을 코드부터 수행

**다른 세션으로 넘겨야 하는 조건**  
ADR이 승인되면 구현은 Bootstrap/모듈/API/DB/Test 등 각 소유 세션으로 넘긴다. 근거 수집 중 장애 재현이 필요하면 Debug/Test로 넘긴다.

**초기 프롬프트**

```text
이 세션은 `POE2-33 Architecture` 전용이며 일상 구현에는 사용하지 않는다. 먼저 AGENTS.md, TECHNICAL_SPEC.md, 기존 ADR, 관련 코드와 측정 자료를 읽어라. 사용자가 아키텍처와 최종 결정을 소유하고 너는 선택지를 구조화하는 보조자다. 모듈 경계, 공개 계약, persistence/캐시/배포, 새 인프라, 큰 알고리즘 변경처럼 장기 영향이 큰 사안만 다뤄라. 현재 Modular Monolith, 점진적 파일 생성, deterministic Crafting Engine, 게임 규칙 invent 금지, undocumented/internal API 역공학 금지와 MVP 범위를 기본 제약으로 삼아라. 문제·결정 동인·대안·장단점·결정·영향·rollback을 ADR 초안으로 제시하되 사용자 승인 전에 코드나 폴더를 선행 scaffold하지 마라. MSA/Kafka/Kubernetes/Elasticsearch/Vector DB 등은 실측 요구와 단순 대안 비교 없이 제안·도입하지 마라. 결정 후에는 구현을 직접 독점하지 말고 소유 세션별 변경·검증 handoff를 작성하라.
```

**유용한 후속 프롬프트 예시**

- `jOOQ 유지와 QueryDSL 전환을 현재 쿼리 증거로 비교한 ADR 초안을 작성해줘.`
- `새 모듈 분리가 필요한지 결합도·데이터 소유권·MVP 비용으로 검토해줘.`
- `결정된 ADR을 구현 세션별 작업과 검증 항목으로 나눠줘.`

---

## 4. Handoff 작성 형식

세션 간 전달 시 아래 형식을 사용하면 문맥 손실을 줄일 수 있다.

```text
[보내는 세션 → 받는 세션]

문제/요청:
관찰한 증거:
현재 범위에서 하지 않은 것:
필요한 변경 또는 공개 계약:
영향받는 모듈/API/DB:
재현 또는 검증 방법:
결정이 필요한 항목:
```

예시:

```text
[POE2-32 Debug → POE2-05 CurrencyRule]

문제/요청: 특정 snapshot에서 action 조회가 500을 반환한다.
관찰한 증거: 요청의 snapshotId에는 handler key가 있으나 SupportMatrix row가 없다.
현재 범위에서 하지 않은 것: CurrencyRule 코드와 migration은 수정하지 않았다.
필요한 변경 또는 공개 계약: 미등록 조합을 UNAVAILABLE로 반환하고 publish validation에서 누락을 거부한다.
영향받는 모듈/API/DB: currencyrule, action availability API.
재현 또는 검증 방법: 첨부한 fixture 요청과 integration test 조건.
결정이 필요한 항목: 기존 draft snapshot 보정 방식.
```

## 5. 세션 생성·유지 체크리스트

- 세션명은 `POE2-NN Name` 형식을 그대로 사용했는가?
- 첫 메시지에 기준 문서 우선, 사용자 소유권, 세션 소유 범위, 점진적 생성, 금지사항, handoff 원칙이 들어갔는가?
- 작업 시작 전에 실제 파일 구조와 기존 변경을 확인했는가?
- 현재 변경이 이 세션의 소유권 안에 있는가?
- 기능과 기본 테스트를 같은 모듈 세션에서 함께 다뤘는가?
- 검증되지 않은 게임 규칙·외부 API·가격·확률을 추정하지 않았는가?
- 타 모듈 변경은 이유와 계약을 명시해 handoff했는가?
- 완료 시 실행한 검증과 실행하지 못한 검증을 구분했는가?

이 구조의 목표는 세션 수를 늘리는 것이 아니라, 사용자가 특정 모듈이나 역할의 문맥으로 즉시 돌아가 작은 단위로 안전하게 개발할 수 있게 하는 것이다.
