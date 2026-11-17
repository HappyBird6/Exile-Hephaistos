# 서버 정책 승계와 남은 검증

검토 기준 `1768e395633fe7623ce0af9e1dddc78b4bd10303`, 수정 코드 `e73fc5d7`. 브랜치는 `codex/automatic-policy-candidates`이며 시작 시 작업 트리는 깨끗했다. 기존 목적함수·정책 범위는 변경하지 않았다.

## 설정 승계

`AutomaticMethodSearch`는 run 포인터로 서버 작업을 GET 복원한 뒤, mounted/token 검사를 통과한 응답의 `rareOutcomePolicy`를 flow에 저장한다. 생략된 서버 정책은 기존 disabled 계약을 따른다. 전역 사용자 선호는 변경하지 않는다. 수동 이어가기는 이 flow를 복사하므로 서버에서 복원한 정책이 새 요청에 전달된다. 명시적 설정 변경은 별도 새 요청을 만들며 기존 응답의 토큰 검사와 컴포넌트 key가 유지된다.

회귀 검사는 flow 문서 없이 run만 저장하고 선호 enabled/서버 disabled를 구성한다. 실제 이어가기 버튼을 통해 새 요청 ID, 관측 아이템의 수치, disabled 전달, 전역 선호 보존을 확인한다. 해당 UI 테스트 파일 31개 통과.

## 두 백엔드 검사 단독 비교

비교 기준은 이전 실패 분류와 같은 `c740b66f`, 현재는 `1768e395` 이후 설정 승계 수정 작업 트리다. 이번 수정은 프런트엔드에 한정되어 현재 백엔드는 1768e395와 동일하다. JDK21, 같은 Gradle 캐시, worker 1개, 클래스별 새 JVM, 기존 진단 기본 timeout 30초 및 명시적 timeout을 유지했다. assertion은 수정하지 않았다. 프런트엔드 검사·브라우저 실행 등 다른 무거운 작업과 겹치지 않게 기준 두 검사, 현재 두 검사를 차례로 실행했다.

| 검사 | 기준 본문 시간 | 현재 본문 시간 | 결과 |
| --- | ---: | ---: | --- |
| CertifiedMethodSearchEngineTest.completeSmallMapContainsEveryExactLawAndPreservesMethodDepthAndAlternatives | 6.310초 | 6.193초 | 양쪽 통과 |
| DeferredProductionTest.realSolarCellsRestoreTheFullWeightedLawAndSplitGoalThresholds | 10.113초 | 10.368초 | 양쪽 통과 |

Gradle 시작·재컴파일 포함 전체 요청 시간은 각각 기준 45.258/29.696초, 현재 24.661/29.258초다. 컴파일/daemon 상태 영향을 받으므로 이 시간으로 엔진 성능을 비교하지 않는다. XML 네 개와 `backend-runs.json`을 보존했다. 첫 실행 시 메서드 필터만으로도 Gradle이 관계없는 클래스를 fork하는 것을 확인해 중단하고, 대상 두 클래스 include와 메서드 필터를 함께 적용하여 다시 실행했다. 중단된 실행은 위 결과에 포함하지 않았다.

재현 예시:

```sh
./gradlew -I isolated-tests.gradle test --tests '*CertifiedMethodSearchEngineTest.completeSmallMapContainsEveryExactLawAndPreservesMethodDepthAndAlternatives' --rerun-tasks
./gradlew -I isolated-tests.gradle test --tests '*DeferredProductionTest.realSolarCellsRestoreTheFullWeightedLawAndSplitGoalThresholds' --rerun-tasks
```

이번에는 시간 초과가 재현되지 않아 새 timeout 원인으로 분류할 근거가 없다. 이전 동시 부하 실행에서만 관측된 시간 제한으로 남기며, 모든 환경에서 회귀가 없다는 보장은 하지 않는다. [기존 보고서](../automatic-policy-review/report.md)의 기준 재현 78건과 당시 시간 초과 9건 기록은 유지한다. 그중 이번 두 검사는 완료 결과를 확보했고 나머지 7건은 이번에도 재검증하지 않았다.

## 실제 브라우저 확인

실제 Chromium을 Playwright로 실행해 React 화면과 입력/버튼 동작을 검사했다. 서버 오류와 응답 순서를 통제하기 위해 동일 컴포넌트를 임시 Vite 페이지에 올리고 API 결과를 fixture로 제공했다. 실제 백엔드에 연결한 전체 사이트 E2E나 공유 데스크톱 세션 검사는 아니다.

- flow 없이 run만 존재하고 선호 enabled인 상태에서 서버 disabled 복원.
- 세 후보에 수치 17 입력 → 관측 후보 반영 → 선택 → 이어가기. 새 요청 ID·실제 수치 17·disabled 확인.
- 희귀 설정 enabled를 명시적으로 선택하고 새 요청에 적용 확인.
- 취소 네트워크 실패 시 기존 화면·요청 유지, 오류 표시, 재시도 성공 후 새 요청 확인.
- 기존 RESUME 응답을 지연시키고 수동 이어가기로 새 요청 생성 후 이전 높은 revision 응답을 전달해도 새 세션 정책이 변하지 않음.

`browser-results.json`에 다섯 결과를 보존했다. `browser-harness.tsx.txt`는 검사 시 사용한 fixture 페이지 소스이고 `browser-check.py`는 실제 조작 스크립트다. 재현하려면 harness를 frontend/review-browser.tsx로 복사하고 이를 module script로 로드하는 임시 HTML을 만든 뒤 Vite 5177과 스크립트를 실행한다. 임시 앱 파일은 검증 후 삭제했다. 제품 의존성은 추가하지 않았다.

## 최종 프런트엔드 검사

수정 코드 커밋 e73fc5d7에서 `npm test -- --run` 전체 검사를 다른 무거운 작업 없이 단독 실행했다. **3,514개 모두 통과, 실패 0, 생략 0**이다. 개별 재실행 결과를 합친 수치가 아니라 한 번의 전체 실행 결과다. `frontend-full.json`에 파일별 상태와 전체 집계를 보존했다.

전체 테스트 종료 후 lint, typecheck, format:check, build를 순차 실행해 모두 통과했다(`frontend-checks.json`). 기존 build chunk 크기 경고는 남아 있다. 이 후속 작업에서는 백엔드 제품 코드·의존성을 바꾸지 않았으므로 백엔드 전체 1,721개를 다시 실행하지 않았으며, 기존 실패 78건을 해결한 것으로 간주하지 않는다.
