# B 자체 검증

기준: `6040357faf4e39cc989991d44ef8d04c7119dbd0`.
브랜치: `codex/abyss-workbench-b-20261102`.
이 문서는 Worker 자체 리뷰이며 최종 통합/브라우저 검증을 의미하지 않는다.

## 프런트엔드

작업 경로: `task-4/abyss-worktree/frontend`.
Node `24.21.0`, npm `11.19.0` (package.json 지원 범위).

| 검사                                                                                                                                                                       | 결과                                                                             |
| -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------- |
| `npm ci --cache <local-user>\AppData\Local\npm-cache --offline --no-audit --no-fund`                                                                                       | 239 packages 설치, 성공                                                          |
| `npm run typecheck`                                                                                                                                                        | 최종 소스 성공                                                                   |
| `npm run lint`                                                                                                                                                             | 최종 소스 성공                                                                   |
| `npm run format:check`                                                                                                                                                     | 성공                                                                             |
| `npm run build`                                                                                                                                                            | 최종 소스 성공; 기존 대형 chunk 경고 유지                                        |
| `npm run test -- --run src/features/crafting/AbyssPanel.test.tsx src/features/crafting/DesecrationCurrency.test.tsx src/features/crafting/abyss-workbench/policy.test.tsx` | 관련 20 tests 통과                                                               |
| `npm run test -- --run`                                                                                                                                                    | 130 files 중 128 통과, 2 실패; 2,427 tests 중 2,425 통과, 2건은 기존 5초 timeout |

Vitest 실행 시 `TEMP`와 `TMP`를 모두 `<local-user>\AppData\Local\Temp\abyss-b-tests`로 지정했다. sandbox 고유 임시 경로에서 초기 IPC 파일 ENOENT가 발생했으므로 임시 경로만 맞췄으며 테스트 기대값/시간 제한은 바꾸지 않았다.

전체 검사 timeout 후 `--maxWorkers=1`로 관련 3파일과 실패한 2파일을 재실행했다. 관련 20개를 포함해 39개 통과, 동일 2건 timeout이었다.

- `WorkbenchExtension.test.tsx`의 `activates the reviewed Whittling pair...`: 최종 수정본에서 해당 테스트 단독 재실행 **통과** (`1 passed / 5 deselected`). 5초 제한을 늘리지 않았다.
- `method-search/stateProjection.test.tsx`의 `keeps terminal cards and bundle count stable...`: 기준 커밋의 원래 `AbyssPanel.tsx`를 사용한 비교에서도 **동일한 5초 timeout 재현**. 다른 소유 파일은 수정하지 않았다. 전체 테스트를 모두 통과했다고 보고하지 않는다.

기준 코드 대조는 이 Worker의 worktree에서만 수행했고 `try/finally`로 수정본 바이트를 복원했다. 비교 출력 파일은 `task-4/baseline-timeout-comparison.log`에 있다. 로그 출력 중 Windows cp949 인코딩 예외가 있었으나 파일 기록과 수정본 복원은 완료됐다.

## 백엔드

기존 Java 21 이미지 `eclipse-temurin:21-jdk-jammy`의 이 Worker 전용 임시 컨테이너에서 검사한다. 기존 Gradle 캐시는 읽기 전용으로 마운트하고 컨테이너 임시 디렉터리로 복제했다. 운영 서버/DB/설정은 변경하지 않았다.

```sh
./gradlew --offline --no-daemon test \
  --tests com.poe2craft.crafting.DesecrationCurrencyTest \
  --tests com.poe2craft.crafting.AbyssCraftingStateTest \
  --tests com.poe2craft.crafting.AbyssControllerTest \
  spotlessJavaCheck bootJar
```

메인/테스트 컴파일과 `bootJar` 실행 완료. 지정한 테스트 **23개는 모두 초기화 단계의 공통 봉인 리소스 오류로 실패**했으며 신규 테스트 본문의 통과를 주장하지 않는다. 오류는 `IllegalArgumentException: Consumed resource digest differs: catalog/manual-reveal.json`이다.

실제 바이트 비교 결과:

| 대상                                                           | SHA-256                                                            | 줄바꿈                |
| -------------------------------------------------------------- | ------------------------------------------------------------------ | --------------------- |
| 기준 Git blob / 이 Worker source / build resource              | `f1e3af497c5527269607c481bd1947c7741f0c74b8cb6d8f2edff54f96b8cdf9` | LF 10,857개, CRLF 0개 |
| 원본 작업 폴더 source / `crafting/ruleset-v1.json`의 기대 seal | `61dc6fb49236efe8d8a5f7b0537d59b1bef13f337cd3ac68848388ec5308f844` | CRLF 10,857개         |

양쪽 JSON을 파싱한 객체는 동일하다. 기준 커밋에 있는 LF 파일과 seal이 맞지 않는 문제이며 Worker 수정으로 생긴 catalog 변경이 아니다. 공통 JSON/manifest는 통합 담당 소유이므로 수정하거나 seal 검증을 우회하지 않았다. 생성 리소스를 바꿔 통과한 것으로 처리하지도 않았다.

테스트와 독립적인 `./gradlew --offline --no-daemon spotlessJavaCheck bootJar`는 별도의 이 Worker 임시 컨테이너에서 **BUILD SUCCESSFUL**. 패키지 생성 성공은 seal 오류 해결을 뜻하지 않는다. `check` 전체 Docker 통합 검사와 `generateJooq`는 수행하지 않았다. 공통 seal 정합성 해결 후 통합 담당이 관련 테스트와 최종 필수 검사를 수행해야 한다.

실패 XML은 `backend/build/test-results/test/TEST-com.poe2craft.crafting.{AbyssControllerTest,AbyssCraftingStateTest,DesecrationCurrencyTest}.xml`에 있다. 이 Worker의 임시 검증 컨테이너는 `--rm`으로 종료/제거됐다.

도메인/API 프로덕션 코드는 기존의 미공개 상태 계약을 그대로 사용한다. 신규 회귀 테스트는 뼈 사용 → 미공개 저장 → 후보 자격 조회 → 복원 → 명시적 직접 선택을 다룬다.

## 자체 리뷰와 통합 경계

- 실제 사용자 첨부 PNG 수신/픽셀 조회 성공. 원본 SHA-256: `71a9fcd6f1b66f47f8b85052053fccc0d46fdc55cceb3ca328518fae6ea75bec`.
- 미공개 룬은 원본 이미지의 해당 영역만 CSS에서 표시한다. 확률·가짜 문자·가짜 옵션을 생성하지 않는다.
- 갉힌 3종은 신규 선택용 정책으로 분리하고 기존 ID/저장 해석/서버 지원은 보존했다.
- `/add`를 실행하는 패널 UI와 별도 시작 버튼을 제거했다. 직접 선택은 사용자가 ‘드러내기’를 누른 후에만 연다.
- 공통 파일과 D 소유 ItemCard는 수정하지 않았다. 필요한 화폐 목록/중앙 카드 연결은 [INTEGRATION.md](./INTEGRATION.md)에 있다.
- 기존 데이터·사용자 원문·제목·메모·규칙 식별자·DB/설정을 변경하지 않았다.
- 브라우저 검증, 서버 변경, 원격 push, 공개 배포는 수행하지 않았다.
