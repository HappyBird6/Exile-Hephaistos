# 요구 4: 장비 품질 최대 적용

기준 `e82e246de18df1688e0eaba8a858b57a3c5b60ff`, Worker branch `codex/quality-maximum`.

새 정책 ID는 `user-selected-equipment-quality-maximum-v2`다. Armourers_Scrap / Blacksmiths_Whetstone 1회 사용으로 지원 장비의 ordinary 20 한도에 도달한다. 이미 20 이상이면 기존 값을 보존하고 재사용을 거부한다. Normal/Magic/Rare에 동일하며 Unique·미지원 유형·unknown quality·조건 플래그에 관한 기존 제한을 유지한다. 이는 사용자 simulator 정책이며 실제 게임 사용량 공식이 아니다.

`QualityLimitRules.describe` / frontend `maximumQuality`의 검토된 40 한도는 maximum-quality Amulet의 catalyst 경로다. 이 작업의 장비 품질 화폐에 40을 적용하거나 catalyst 범위를 넓히지 않는다. frontend matcher도 `maximumQuality(state, {}) === 20`인 ordinary equipment만 허용한다.

## 통합자 소유 변경 계약

- `registry-v2.json`: 두 품질 화폐 행의 `qualityPolicy`를 위 새 ID로 변경. `reason`: `User-selected simulator policy: one use sets supported equipment quality to its ordinary cap of 20 regardless of rarity. Quality already at or above the cap, unknown current quality and unsupported conditions refuse without changes. This is not an actual-game per-use amount.` 기존 source·지원 base 목록은 보존.
- `WorkbenchSimulator.ruleVersion()`의 manual suffix는 `-sanctification-independent-stat-percent-v1-user-selected-equipment-quality-maximum-v2`로 변경된다. e82 저장 model의 `...plus-one-v1` suffix와 더 오래된 알려진 suffix의 읽기 호환을 root의 historical adapter에서 유지. `BasicCurrencyTransitions`는 이미 manual suffix를 provenance에서 제거하므로 추가 변경 불필요.
- `workbenchApi.ts` props/model/signature 변경 없음. 새 assumption: `id` 새 정책 ID, `candidateUnit` = `equipment quality target percentage`, `n` = 1, `candidates` = [], `min` = `max` = 20. 기존 `qualityTransitionMatches(state, result, action)` 호출 유지.
- registry·rule source 변경 후 root가 seal/provenance 공통 manifest를 재생성한다. 이 Worker는 해당 파일을 수정하지 않는다.
- 기존 6언어의 품질 설명 중 +1%p 정책을 설명하는 항목은 위 사용자 정책에 맞게 root가 갱신한다. 제품 UI/class/props 추가 없음.

`root-tests.patch`는 통합자 소유의 테스트 9개 파일에 있는 현재 `ruleVersion` / registry `qualityPolicy` 기대 문자열만 새 ID로 변경한다. 게임 규칙 oracle나 historical fixture는 변경하지 않는다. `git apply --check frontend/src/features/crafting/equipment-quality/root-tests.patch` 후 적용한다. Historical adapter는 root가 별도로 처리해야 한다.

기존 `sanctification/verify-no-socket.sh`는 과거 작업의 검증 스크립트이며 옛 `QualityPlusOneTest` 경로를 포함한다. 이 작업은 그 파일을 소유하지 않으며, 현재 테스트 이름은 `QualityMaximumTest`다.

## 소유 파일 및 자체 리뷰

- Backend: `QualityCurrencyRules.java`, `WorkbenchSimulator.java`, `QualityMaximumTest.java` (기존 `QualityPlusOneTest.java`를 이름 변경), `QualitySanctificationTest.java`의 품질 관련 검증.
- Frontend: `qualityCurrency.ts`, `qualityCurrency.test.ts`, 이 폴더의 `INTEGRATION.md` / `root-tests.patch`.
- 공통 page/CSS/history/API/registry/seal/manifest/historical adapter는 수정하지 않았다.
- 자체 리뷰: 0/7/19 → 20, 20/30/40/100 → unchanged refuse, 반복 사용 refuse, Normal/Magic/Rare, wrong currency/class·unknown quality·condition·Unique 거부, hidden slot/원본 roll/fracture/관측/소켓/비관련 omen 보존을 대조했다. Amulet의 검토된 40 cap이 장비 품질에 확장되지 않는 Backend 회귀 검증을 포함한다.

## 검증 환경

- 호스트 기본 Java는 Zulu 8이다. 이번 검증은 별도 설치한 **Temurin 21.0.13+9 LTS**를 사용했다. 시스템 Java 설정은 변경하지 않았다.
- JDK 실제 경로: `C:/Users/SSAFY/.codex/worktrees/1210/Exile-Hephaistos/.toolchains/quality-maximum/jdk-21.0.13+9`.
- 다운로드 entrypoint: `https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse`.
- 확인한 공식 metadata: `https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse`.
- 고정 archive: `https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13%2B9/OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9.zip`.
- 공식 checksum URL: `https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.13%2B9/OpenJDK21U-jdk_x64_windows_hotspot_21.0.13_9.zip.sha256.txt`.
- `Get-FileHash -Algorithm SHA256 .toolchains/quality-maximum/jdk21.zip` 결과와 공식 metadata의 `binary.package.checksum`을 비교해 일치를 확인했다: `a4dbaa05fca4bd89d84584e58c459bd732efebb1722af80ab13c6516832004f9`.
- 준비 기록: ignored `.toolchains/quality-maximum/jdk-evidence.json`. JDK/archive/Gradle cache는 모두 ignored이며 Git에 포함하지 않는다.
- PowerShell 옵션: `$env:JAVA_HOME`을 위 경로, `$env:GRADLE_USER_HOME`을 `C:/Users/SSAFY/.codex/worktrees/1210/Exile-Hephaistos/.toolchains/quality-maximum/gradle-user-home`으로 지정하고 `--no-daemon`을 사용했다. Gradle Wrapper 8.14.3의 고정 SHA 검증을 유지했다. Docker나 DB를 호출하지 않는다.
- Frontend: Node 24.21.0 / npm 11.19.0. 현재 worktree에 `npm ci`로 lockfile 고정 의존성을 설치했다.

## 검사 결과

- PASS: frontend `npm ci`, `npm run typecheck`, `npm run lint`, `npm run format:check`, `npm run build`.
- PASS: frontend `npm run test -- --run src/features/crafting/qualityCurrency.test.ts src/features/crafting/qualityLimit.test.ts src/features/crafting/sanctification/transition.test.ts --maxWorkers=1`, 일반 호스트 실행, **3 files / 20 tests**. pretest의 6언어 display coverage도 통과했다.
- 초기 실패 후 수정: ConcreteItem은 Unique를 타입에서 제외하므로 중복 Unique 검사 때문에 발생한 TypeScript 오류를 제거했다. Backend의 담당 테스트 format 위반은 기존 Spotless로 수정했다. 기대값 완화나 skip은 없다.
- 환경 실패: 샌드박스 `npm ci`는 DNS `ENOTFOUND`로 실패했다. 샌드박스 Vitest는 IPC client 파일 `ENOENT`로 **0 tests / no tests**였고, 테스트를 실행한 실패 결과로 해석하지 않는다. 일반 호스트 재실행으로 위 PASS를 확인했다.
- PASS: Java 21 Backend **6 classes / 22 tests**, `spotlessCheck`, `bootJar`. 최종 명령: `./gradlew.bat --no-daemon test --tests '*QualityMaximumTest' --tests '*QualitySanctificationTest' --tests '*QualityLimitTest' --tests '*QualityCapChangeTest' --tests '*WorkbenchSimulatorTest' --tests '*ArchitectureTest' spotlessCheck bootJar --continue` (exit 0, BUILD SUCCESSFUL). 대상별 tests: QualityMaximum 5, QualitySanctification 3, QualityLimit 3, QualityCapChange 4, WorkbenchSimulator 5, Architecture 2. 첫 실행 뒤 담당 테스트 파일만 `spotlessApply`로 정리했고 최종 검사를 모두 통과했다. build 결과는 현재 worktree의 ignored `backend/build`에 있다. 공통 registry/seal 반영 후 통합자는 최종 소스로 다시 검증한다.
- PASS: `git diff --check`, `git apply --check frontend/src/features/crafting/equipment-quality/root-tests.patch`. 자체 리뷰 후 로컬 commit하며 완료 SHA는 최종 응답에 기록한다.
- 미실행: 전체 frontend/backend suite는 통합자 최종 검사 범위다. DB 통합 `integrationTest` / `generateJooq` 및 전체 `check generateJooq bootJar`는 Docker 소켓 제한으로 실행하지 않았다. Compose는 변경하지 않았고 검사하지 않았다. push/서버 반영/브라우저 조작은 하지 않았다.

## Docker 자동 승인 거부 기록

명령: `docker image inspect eclipse-temurin:21-jdk-jammy --format '{{.Id}}'`.

일반 sandbox 호출은 Docker Desktop Linux engine named pipe 연결 권한 오류를 반환했고 이미지 정보를 얻지 못했다. 이어진 `require_escalated` 호출은 **프로세스 실행 전에 자동 승인 검토가 거부**했다. 정확한 거부 사유:

> 이미지 조회 자체는 읽기 전용이지만, 사용자가 내부 Docker 소켓 연결 금지를 명시했으므로 Docker daemon을 통한 inspect는 승인 범위를 벗어납니다.

이후 Docker 조회·우회·컨테이너 실행을 하지 않았다. Java 검증은 위 공식 Windows JDK를 사용하는 독립적인 로컬 절차로 완료한다.
