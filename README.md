> 최신 완료 집계 (2026-10-04): registry220 = active170(implemented170 = default163 + opt-in legacy7, pending0) + deferred50(보존 구현8 + 미구현42). Catalyst26는 검증된 제한 base만 IMPLEMENTED로 정리했다. 전체 구현178은 deferred8을 포함하므로 현재 사용 가능 수가 아니다. 아래의 이전 집계는 checkpoint 이력이다. [정의·base 제한·검증](docs/workbench-catalyst-registry-2026-10-04.md).

# Exile-Hephaistos

> 최신 사용자 지시 (2026-10-04): 기폭제 서비스 제외 취소. 현재144 = 기본111 + legacy7 + 기폭제 미구현26, 보류76 = 기존65 + 신규11, registry220/구현체126. 기존 기폭제 메뉴·typed 품질 입력/API·film·모델·테스트 보존. 실제 적용과 UX는 사용자 피드백 수집 후 조정. Catalysing 및 Necromancy2·훼손 신규 개발은 계속 보류. 이번5는 구현 checkpoint이며 최종 FE/API/browser 검증 미완료. [최신 범위](docs/workbench-service-scope-2026-10-04.md).


최신 Workbench 사용자 상태는 [2026-10-04 보고서](docs/workbench-status-2026-10-04.md)를 기준으로 확인하세요. 9베이스, 기본 지원111 + opt-in legacy2, 미완료42이며 아래 단계별 설명은 이전 개발 이력을 포함합니다. 확률은 출처 snapshot 모델·명시된 가정이며 실제 게임 odds 검증을 뜻하지 않습니다.

영어 PoE2 아이템 복사 텍스트를 분석하고 Solar Amulet을 제작하거나 확률을 비교하는 작업대입니다. Workbench는 커런시 17종과 Omen 8종으로 실제 아이템 상태를 변경합니다. 독립 Craft Support는 시작 아이템과 필수 family AND 후보 N개·최소 tier 목표에 대해 정상 추가 화폐 순서를 비교하고 최초 목표 달성 확률을 합산합니다. 속성 선택은 PoE2DB Base 게시 가중치, 수치 roll과 제거는 명시한 균등 확률 가정을 사용합니다. 재료 수량·가격·비용은 계산하지 않습니다. 복사 텍스트는 catalog 검증을 통과한 경우에만 제작 상태로 사용할 수 있습니다. [Workbench·registry·가정 ledger](docs/workbench-simulator.md), [Support 계산·지원 한계](docs/support-transition-design.md), [ItemState·확률 탐색 명세](docs/item-state.md).

- `/`: 재료 탭·검색·툴팁·즐겨찾기·아이템 입력과 카드.
- Craft Support: 별도 시작 상태·목표, 최대 5개 정상 추가 순서 비교, 예산이 부족하면 미해결 질량과 임시 순위 표시. 복구 후 실제 상태를 새 root로 입력하며 이전 확률은 합치지 않습니다. 같은 family의 여러 스킬 효과 중 특정 효과만 고르는 목표는 아직 지원하지 않습니다.
- `/admin`: 로그인 없이 접근하는 최소 관리 페이지. 현재 관리 도구는 없습니다.
- Backend: `POST /api/v1/items/parse`로 영어 아이템 텍스트를 분석합니다.
- PostgreSQL·Redis와 저장 기반은 다음 작업을 위해 유지합니다.

## Docker 실행

Docker Desktop의 Linux container 모드를 켜고 Windows PowerShell 5.1에서 실행합니다.

```powershell
.\scripts\dev.ps1 stack
.\scripts\dev.ps1 status
.\scripts\dev.ps1 stop
```

실행 정책으로 차단될 때만 다음처럼 해당 프로세스에서 실행합니다.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\dev.ps1 stack
```

Compose 프로젝트 이름은 `exile-hephaistos`입니다. 운영 실행은 원본 checkout에서 하며 검증용 worktree에서 기존 프로젝트를 실행하지 않습니다.
Frontend: http://localhost:8081 · Backend liveness: http://localhost:8080/actuator/health/liveness
readiness는 앱·PostgreSQL·Redis 연결 상태를 확인합니다. 미구현 제작 계산의 준비 완료를 뜻하지 않습니다.

최초 실행 시 스크립트가 DB·Redis 설정과 무작위 로컬 DB 비밀번호를 `.env`에 생성합니다. 기존 `.env`는 덮어쓰지 않습니다. `.env.example`은 필요하지 않습니다.
`stop`은 컨테이너만 정지합니다. PostgreSQL은 외부 volume을 사용하며 기존 데이터를 삭제하지 않습니다. 코드 반영 시 `stack`을 다시 실행합니다.

## 기존 데이터 보존

기존 `.env`의 `POSTGRES_VOLUME_NAME`을 유지하세요. 새 환경의 기본 이름은 `exile-hephaistos_postgres-data`입니다. DB 이름·계정·비밀번호는 기존 volume의 값과 일치해야 하며 `.env`를 수정해도 DB 비밀번호가 자동 변경되지는 않습니다.
이번 기능 정리는 과거 DB table·schema, Docker volume, 로컬 원본 파일을 삭제하지 않습니다. 이미 적용된 migration SQL은 이력 호환을 위해 유지합니다. 제거된 기능은 앱에서 접근하거나 실행하지 않습니다.

## 호스트 개발

Java 21 (`JAVA_HOME`), Node 24.18.1 / npm 11.16.0, Docker를 사용합니다.

```powershell
.\scripts\dev.ps1 infra
# 별도 터미널
.\scripts\dev.ps1 backend
# 별도 터미널
.\scripts\dev.ps1 frontend
```

Vite는 http://localhost:5173 에서 실행하고 `/api`를 Backend 8080 포트로 전달합니다. Python은 앱 실행에 필요하지 않습니다.
로그인·세션·역할 검사는 없습니다. Compose 포트는 localhost에만 노출합니다.

## 검증

```powershell
cd backend
./gradlew.bat check generateJooq bootJar
cd ../frontend
npm ci
npm run lint
npm run typecheck
npm run format:check
npm run test -- --run
npm run build
```

Backend `check`는 Spotless·JUnit·ArchUnit·Testcontainers 통합 검사를 포함합니다. Docker가 필요하며 임시 PostgreSQL·Redis를 사용하고 기존 개발 DB는 변경하지 않습니다.
`generateJooq`도 임시 PostgreSQL에서만 실행합니다. JPA flush → jOOQ 조회 → 공유 transaction rollback 검증을 유지합니다.

Windows 실행 스크립트 변경은 Python 3.12로 아래 검사를 수행합니다. 개발 스크립트 테스트용이며 앱 런타임 의존성이 아닙니다.

```powershell
python -m unittest discover -s scripts/tests -v
docker compose --env-file .env -f infra/compose.yaml --profile stack config --quiet
```

GitHub Actions는 사용하지 않습니다. `.gitattributes`는 Windows/Linux 줄바꿈을, `.editorconfig`는 편집기 인코딩·들여쓰기를 통일합니다.

## 구현과 출처

- Java 21, Spring Boot 3.5.16, Gradle 8.14.3, PostgreSQL 17.6, Redis 7.4.5.
- Frontend 의존성은 `package-lock.json`으로 고정합니다.
- 주석만 있는 제작 관련 준비 파일은 다음 작업을 위해 유지합니다.
- [기술 명세](docs/TECHNICAL_SPEC.md), [아이템 분석](docs/item-text-parsing.md), [제작 자산 출처](docs/crafting-ui.md), [지원 범위](docs/supported-mechanics.md).
- [Worker 운영 명세](docs/solo-workflow/MULTI_SESSION_WORKFLOW.md).

Solar Workbench v2: 17 currencies and 8 omens. See [supported and blocked inventory](docs/workbench-support-v2.md) and [rules, assumptions, and Support preparation](docs/workbench-simulator.md).

최신 Workbench 확장(2026-10-03): Solar Amulet과 Stocky Mitts를 베이스별로 지원합니다. Stocky Mitts는 일반 화폐 19종과 확인된 Enhancement 3종·Greater Battle, 관련 징조·분열을 지원하며 일반 모드 182개와 PoE2DB 게시 가중치를 보존합니다. 장갑의 수치 롤은 원자료 단위 정수·공통 비율/HALF_UP **미검증 모델**로 명시하고 제작 단계별 가정을 복원합니다. 최종 Armour·품질·소켓·장갑 텍스트 매핑과 다른 장갑 특수 수단은 미지원입니다. Craft Support/State Explorer는 Solar 범위입니다. [최신 범위·검증·되돌리기](docs/workbench-stocky-mitts-runtime-2026-10-03.md), [단일 이슈 목록](ISSUES.md).

Stocky Mitts 기본 Essence 확장(v18): 기존 4종에 Body/Mind/Ruin/Insulation/Thawing/Grounding/Opulence 21종을 추가해 25종을 지원합니다. 두 베이스 전체 구현 목록은 64종으로 유지되며, 장갑 Infinite 결과 집합은 WB-010에서 근거 부족으로 차단합니다. [검증·효과 표·지원 경계](docs/workbench-stocky-basic-essences-2026-10-03.md).

장갑 Hysteria(v19): Rare/lvl45+에서 전용 of Fury 치명타 피해 보너스로 교체하며, Crystallisation 방향·전체 제거 분기·Fractured 보존을 검증했습니다. 장갑 지원 56종/45액션, 구현 고유 재료는 64종 그대로입니다. [근거·검증·후속 후보](docs/workbench-stocky-hysteria-2026-10-03.md).

장갑 Abyss(v20): 일반 모드 182개는 보존하고 zero-spawn 전용 접두/접미 2개만 별도 카탈로그로 추가했습니다. 이전 snapshot·필름의 수치/잠금/증거/미래 단계를 보존하며, 선택 1/2는 미공개 가중치에 대한 명시적 가정입니다. 구현 고유 재료 64종 유지, 장갑 지원 57종/46액션. [검증·호환 경계](docs/workbench-stocky-abyss-2026-10-03.md), [나머지 특수 결과 근거표](docs/workbench-stocky-special-evidence-2026-10-03.md).

2026-10-03 Workbench 후속: Stocky Mitts Horror의 고정 Local 60% suffix 부여를 지원합니다. 소켓 장착과 Rune/Soul Core 효과 계산은 미지원이며 화면에 경계를 표시합니다. 일반 182모드의 가중치는 유지했고, 기존 normal/Abyss 제작 필름을 보존합니다. 실제 구현 범위는 등록 220개 중 65개, Stocky 58 지원항목/47액션, Solar 60/49입니다. [룰 근거·Docker/브라우저 검증·미실행 검사·다음 우선순위](docs/workbench-stocky-horror-2026-10-03.md). 390px 중앙 사용 버튼 잘림도 수정했습니다.
2026-10-03 Workbench 추가: Perfect Grounding/Opulence의 장갑 전용 suffix 부여를 보수적 지원 범위인 Stocky ilvl 72+에서 활성화했습니다. 표의 effective/Required Level 57을 화폐 최소 아이템 레벨로 단정하지 않으며 낮은 ilvl은 WB-017 미확인입니다. 수치 정밀도는 UNVERIFIED 모델, Recoup/Gold 계산과 Solar 적용은 미지원입니다. 고유 구현 67종, Stocky 60 지원항목/49액션, Solar 60/49. [원문·checksum/필름 호환·검증과 미실행 범위](docs/workbench-stocky-perfect-glove-essences-2026-10-03.md).
