# B — 정정 25개 중 요청 4·5·6·18 자체 검증

기준 SHA `4fb6b89d0f2319444742aac336f9b1a1832b515c`에서 시작했다. 구현 경로는 `C:\Users\SSAFY\.codex\worktrees\fdcc\Exile-Hephaistos`, branch는 `codex/abyss-25-corrections-b-local-20261103`이다. 자체 리뷰이며 최종 통합 검사가 아니다.

## 최종 소스 검사

| 검사                                                                                                                                                                                                                                                       | 결과                                                                     |
| ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------ |
| Frontend `npm ci --cache C:\Users\SSAFY\AppData\Local\npm-cache --offline --no-audit --no-fund`                                                                                                                                                            | 239 packages 설치 성공; lockfile 변경 없음                               |
| `npm run typecheck`                                                                                                                                                                                                                                        | 성공                                                                     |
| `npm run lint`                                                                                                                                                                                                                                             | 성공                                                                     |
| `npm run format:check`                                                                                                                                                                                                                                     | 성공                                                                     |
| `npm run test -- --run --maxWorkers=1 src/features/crafting/abyss-workbench/continuousCurrency.test.ts src/features/crafting/DesecrationCurrency.test.tsx src/features/crafting/AbyssPanel.test.tsx src/features/crafting/abyss-workbench/policy.test.tsx` | 4 files / 28 tests 성공                                                  |
| `npm run build`                                                                                                                                                                                                                                            | 성공; 기존 대형 chunk 경고 유지                                          |
| CraftingPage 연결 patch                                                                                                                                                                                                                                    | 실제 공통 파일을 변경하지 않고 메모리상의 overlay로 TypeScript 검사 성공 |
| 공통/연동 patch 7개                                                                                                                                                                                                                                        | 기준 소스/최종 B 소스에 `git apply --check` 성공                         |
| Backend Java 21 no-network/no-socket `spotlessJavaApply test ... spotlessJavaCheck bootJar`                                                                                                                                                                | 성공; 관련 6 classes / 36 tests, 실패·오류·skip 0                        |

Backend 최종 실행은 기존 `eclipse-temurin:21-jdk-jammy` 이미지에 B의 backend만 `/workspace`로 연결하고 `--network none`을 지정했다. Docker socket은 연결하지 않았다. 기존 `exile-chaos-final-gradle` cache volume은 앞선 실행에서 읽기전용으로 복사했으며 최종 실행은 B의 `backend/build/gradle-b` 복사본만 사용했다. 운영 DB/Redis/volume/54개 저장파일/설정은 연결하거나 수정하지 않았다.

```sh
sh gradlew --offline --no-daemon spotlessJavaApply test \
  --tests com.poe2craft.crafting.DesecrationCurrencyTest \
  --tests com.poe2craft.crafting.AbyssCurrencyPlanTest \
  --tests com.poe2craft.crafting.AbyssContinuousCurrencyTest \
  --tests com.poe2craft.crafting.AbyssCraftingStateTest \
  --tests com.poe2craft.crafting.AbyssControllerTest \
  --tests com.poe2craft.crafting.AbyssProbabilityComparisonTest \
  spotlessJavaCheck bootJar
```

| Backend class                  | tests |
| ------------------------------ | ----: |
| DesecrationCurrencyTest        |    11 |
| AbyssCurrencyPlanTest          |     3 |
| AbyssContinuousCurrencyTest    |     7 |
| AbyssCraftingStateTest         |     5 |
| AbyssControllerTest            |     7 |
| AbyssProbabilityComparisonTest |     3 |

결과 XML은 B checkout의 `backend/build/test-results/test`에 있다. 사용자 원문이나 비밀값을 테스트 출력/Git에 기록하지 않았다.

## 검사 중 확인한 문제와 해결

- 기본 sandbox npm cache에 일부 패키지가 없어 최초 offline 설치가 실패했다. 기존 사용자 cache를 사용해 동일 lockfile로 설치했다.
- Windows sandbox TEMP 경로에서 Vitest IPC ENOENT로 첫 실행이 테스트 시작 전에 실패했다. TEMP/TMP를 B의 `frontend/node_modules/.tmp-b-25`로 지정하고 동일 테스트를 실행했다. 기대값·시간 제한을 완화하지 않았다.
- 기본 Windows Gradle cache에는 필요한 build plugin이 없었다. 기존 Gradle cache volume을 읽기전용으로 복사해 Java21/no-network 조건을 유지했다.
- 최초 backend 검사에서 표식 아이템 거부의 기존 오류 메시지를 generic probe 오류가 가렸다. 명시적 관측 side 경로는 기존 검증을 직접 호출하도록 수정하고 관련 36개 테스트를 다시 통과했다.
- 새 TypeScript fallback의 exactOptionalPropertyTypes와 테스트의 unused callback 인자를 수정했다. 공통 연결 patch의 사용하지 않는 jewelCapacity import도 메모리상 검사에서 찾아 제거했다.
- 자체 리뷰에서 catalyst 후 일반 제작의 과도한 차단과 동일 modifier ID 재생성 시 훼손 표식 오계승을 고치고 각각 회귀 테스트를 추가했다. 마지막 backend 실행은 이 최종 소스다.

## 검증한 보존 동작

양쪽/한쪽 접두·접미 가용성, Necromancy 강제 side, reveal 미실행, ancient minimum level, 숨은 슬롯의 일반 추가/제거/Chaos, 분열 보호, catalyst 후 연속 추가, 같은 ID 재생성 시 표식 제거, offer와 활성 omen의 실패 보존, 요청 취소, film 저장·복원, Sanctified 슬롯의 읽기와 조작 거부를 검사했다. Preserved Cranium은 source URL·SHA와 실제 이미지를 확인했다.

## 미실행 및 통합자의 필수 확인

- 사용자 지시대로 backend `check` 전체의 DB/Redis integrationTest와 `generateJooq`를 실행하지 않았다. no-socket 검사는 이 DB/codegen 차단을 해소하지 않는다.
- 전체 frontend/backend 전수 검사는 통합자에게 배정되어 실행하지 않았다. 브라우저 자동 QA는 수행하지 않았다.
- 공통 화면/Service/Controller patch는 직접 적용하지 않았다. C의 Sanctification 메타 보존 patch와 추가 C 연동 테스트 patch는 C 통합 후 실제 컴파일·실행해야 한다. 해당 검사를 B의 36 tests 통과에 포함하지 않았다.
- C 메타가 있는 숨은 슬롯/공개 슬롯/기존 offer의 restore 및 후속 거부를 통합 상태에서 확인해야 한다. B의 baseline 검사만으로 C와의 최종 상호작용 통과를 주장하지 않는다.
- 서버 시작·변경·remote push는 수행하지 않았다. 자산 첨부가 필요한 실제 UI 노출 7종과 연결 절차는 [INTEGRATION-25.md](./INTEGRATION-25.md)를 따른다.
