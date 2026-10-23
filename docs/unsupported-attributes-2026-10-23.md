# 속성 목표 판정 복구 — 2026-10-23

> 후속 작업에서 남은 재생·흡수율3개의 원자료 환산을 확인해 지원했다.
> 최신 구현·검증·실행 상태는 [재생·흡수율 목표 환산](reviewed-rate-goals-2026-10-23.md)을 참고한다.
> 아래40/43 및 kernel v6 기록은 이전 QA 반영 시점의 이력이다.

## 적용 범위

NTU `exile-refinements`, `codex/automatic-refinements-20261021`, HEAD
`84b94438b6c2f56f5688a17635e1d020aa35b482` 이후 미커밋 작업이다. 기존 결합
티어 분포·제왕/엑잘 정책·좌버튼 패닝·Ctrl+휠 변경을 보존했다. 원격 push 없음.
사용자가 특정 속성명을 아직 제공하지 않아, 확인된 반지 미지원 항목부터 조사했다.

- goal definitions 17→37: 원본 수치와 표시 단위가 직접 대응하는 20개 항목 추가.
  물리/화염/냉기/번개 추가 공격 피해의 최솟값·최댓값 8개, 회피, 처치 시 생명력·마나,
  마나 재생 증가, 희귀도, 시야, 화염/냉기/번개/카오스 피해 증가, 시전 속도,
  최대 마나 증가. 반지 실제 카탈로그 43개 중 40개 판정 지원.
- 숫자가 둘 이상인 속성도, 기존 Workbench에서 명시적으로 선택한 수치 모델이 있는
  경우 자동 계산에서 실제 수치 조합과 그 가중치를 사용한다. 독립 곱이나 균등 조합으로
  바꾸지 않는다. 미선언 가변 복수 수치 분포는 여전히 미해결 질량으로 보존한다.
- 대표 상태 계획, primitive 확률 전개, 목표 AND 판정, 저장 결과의 결합 출처 및 화면
  수치 표를 연결했다. 한 열만 바꿔 서로 다른 티어의 수치를 합성하던 표시는 전체 조합을
  투영하도록 수정했다. 고정된 단일 조합에는 불필요한 티어 표를 만들지 않는다.
- `rollModels`는 실제 실행에서 사용한 모델만 응답/저장 코드에 기록한다. 자동 계산 화면은
  해당 모델이 쓰이면 게임 내 상관관계 미검증 안내와 계산 가정을 6개 언어로 표시한다.
- kernel: `automatic-finite-kernel-v6-declared-coupled-rolls`. 이전 v5 결과는 읽기 전용이다.
  `definitions-v3-readonly.json`에 기존 17개 정의의 정확한 바이트를 동결했다. 이전 목표
  catalogVersion을 임의로 새 버전으로 바꾸지 않는다. 리소스 봉인 목록은 464개다.

주요 구현: `AutomaticCraftingKernel`, `CoupledStatRollModel`, `WorkbenchSimulator`,
`AutomaticMethodGraph`, `SavedAutomaticResultReader`, `SupportConfiguration`,
`GoalDefinitionsLoader`, `SealedResources`, goal-filter JSON, `NumericRollNotice`,
`jointAttributePresentation`, `automaticTierLaws`, `automaticApi`, `scenarioCode`.

## 모델 근거와 한계

`ISSUES.md` WB-001 및 2026-10-03 User review completed 기록은 사용자가 기존 권고안인
**10001개 공통 비율 틱·HALF_UP 및 명시적인 미검증 표시**를 선택했다고 기록한다.
이번 변경은 이 기존 Workbench opt-in 계약을 자동 계산에 연결한 것이다.

`user-coupled-ratio-half-up-v1`: 정수 틱 0…10000 중 하나를 균등하게 선택하고, 한 속성의
모든 숫자에 같은 틱을 사용한다. 각 원본 min/max 사이를 보간하고 정수 HALF_UP
(음수의 절반도 0에서 멀어지는 방향)으로 반올림한다. 동일한 반올림 조합을 만드는
틱 수를 합산하므로 조합은 균등하지 않다. 속성 선택 weight와 수치 틱 확률은 별도다.

이것은 실제 게임 확률의 검증이 아니다. 원본의 표시 단위와 양 끝값을 확인해도 내부
롤 분포나 상관관계가 입증되지는 않는다. 10001틱 전수 오라클과의 일치는 **같은 가정의
별도 구현과 일치**한다는 뜻이다. 게임 관측과의 일치가 아니다.

## 검증

모든 Docker 검사는 별도 컨테이너/임시 테스트 DB에서 수행했다. 별도 미리보기는
18090/18091과 전용 PostgreSQL·Redis·볼륨을 사용했다.

| 검사 | 최종 결과 |
|---|---|
| 백엔드 집중 | 26/26 통과 (`focused-r6`) |
| 백엔드 전체 unit/ArchUnit | 900개 중 899 통과, 기존 06 1개 실패 (`backend-full-r2`) |
| integration | 8/8 통과 (`integration-full-r2`) |
| spotless / generateJooq / bootJar | 통과. 전체 Gradle 종료코드는 위 unit 1개 실패 때문에 1 |
| 프런트 lint/typecheck/format/build | 통과 |
| 프런트 전체 | 2350/2350, 0 skipped (`frontend-final-r2.xml`) |
| 실제 API 그래프→프런트 파서/수치 조합 렌더/모델 안내 | 추가 1개 통과 (`live-ui-r6.xml`) |
| 구형 v8/9/10 및 v12 | 단위 검사 통과. 실제 v12 3개는 노드·방법 확률 동일, 읽기 전용 복원 |
| 격리 실제 API | 33요청: 신규 20개 판정, 복수 수치 AND 일치/불일치, 모델 확률 오라클 |
| 교체 QA 실제 API | 26요청: 신규 20개 및 복수 수치 일치/불일치 재확인 |
| QA UI/health/정적 자산 | HTTP 9개, 정적 파일 227개 일치 |
| Chrome | 실제 QA 복합 목표 불일치/일치, 새 계산, 모델 안내 및 티어 수치 표 확인 |

구체적 모델 오라클: 빈 희귀 반지 ilvl82에서 접두 전용 엑잘의 추가 물리 피해 최소3,
최대6 목표는 `580068/6950695` ≈8.3454676115%. API의 인증 확률 구간이 독립 구현한
공통 틱 전수 계산값을 포함한다. 전체 경로 완결을 뜻하지 않는다.

최초 전체 검사에서 현재 catalog 예제와 봉인 개수 기대값도 실패했다. 예제는 실제
현재 서비스에서 다시 생성하되 원래 아이템·목표·수치22·MATCH 결과는 유지했고,
464개 신규 봉인 계약을 반영했다. 최종 재검사에서 이 둘은 통과했다. 기대값 완화나
기존 테스트 삭제, 예산 증가는 없다. 05·07은 최종 전체 검사에서 통과했고, 06은
120000스텝 관찰 범위 내 `graph.finished()`가 false여서 실패했다. 해결됐다고 주장하지 않는다.

추가 실제 그래프의 모든 노드에 티어 표가 있어야 한다는 임시 검사 가정은 부정확했다.
고정 수치 노드와 분포 노드를 구별한 최종 검사는 실제 복수 수치 카드 모두를 파싱하고,
분포 노드의 티어 표 및 미완료 상태의 내보내기 금지를 확인한다.

## 남은 미지원·성능 제한

- `base_life_regeneration_rate_per_minute`,
  `base_life_leech_from_physical_attack_damage_permyriad`,
  `base_mana_leech_from_physical_attack_damage_permyriad`: 표시 변환 근거를 확정하지 않아
  미지원 유지. 단위명만 보고 변환하거나 분포를 추측하지 않았다.
- `maximum_mana_+%`는 기존 원본 완전 에센스 행 4…6%의 판정을 지원한다. 일반 생성
  weight=0 및 eligible=false는 유지한다. 현재 아이템에 있는 값의 판정 지원이 일반
  화폐로 생성 가능하다는 뜻은 아니다. 해당 에센스 경로를 새로 구현한 것은 아니다.
- 기폭제 품질이 수치에 미치는 영향, 미선언 모델, 미검토 장비·조건부 속성은 이번에
  임의로 해결하지 않았다. 모든 장비의 모든 속성을 지원한다고 주장하지 않는다.
- 빈 반지 복수 수치 목표의 전체 그래프는 기본 예산에서 5456연산/17노드/6방법으로
  PAUSED였다. Chrome의 자동 이어 계산도 실제 최대 자원 한도에 도달하는 사례를 확인했다.
  직접 화폐 확률을 계산할 수 있어도 모든 후속 경로의 완결을 보장하지 않는다.
- 해당 전체 API 결과는 약23.7MB여서 기존 16MiB raw/500000 tree 항목 등의 시나리오
  내보내기 한도에 걸렸다. 작은 PAUSED/CANCELLED 결과도 기존 완료 상태 계약상
  내보낼 수 없다. 완료/부분완료 모델 metadata의 압축 왕복은 프런트 회귀 검사에서 통과.
  실제 대형 결과 내보내기는 미해결이며 한도를 올리지 않았다.

## 실행·보존

QA 주소: http://localhost:18081 / backend http://localhost:18080.

- 실행 source fingerprint: `59feaab5bea9a185d700101c4044ca581833462067a68b73c15c4d2343b1d147`
- ruleset: `reviewed-equipment-20261009-v2-6346262eb5667dafc37a65907bb03f16469d2ce976559de50a4b6cb1a7a2d727`
- app image: `sha256:625b1c29679f1ca80697a76cdded8f020af36b59dd59c97a1b30e2007c53465f`
- frontend image: `sha256:31b4cecaa984be234d6dac52c94b59ec6495baaf1441536af6a481eedd4f904c`
- 실행 JAR SHA256: `1535871bb04353f81be944369dc1f5c604f22a4596b52b1c08462e6dc7dcd9c8`.
  검증 JAR의 내부 파일 1356개 전부 일치. 정적 파일 227개 전부 일치.
- JS `index-D8ELWgUz.js`, CSS `index-Bi14gc7Z.css`.
- 기존 저장 결과28개 바이트 동일. QA 검증용 새 계산은 별도 추가하며 기존 파일은 삭제하지 않는다.
  PostgreSQL/Redis 컨테이너와 마운트, `.env` 및 QA 설정 해시 동일. 8081은 계속 종료.
- 사용자 개인 브라우저는 건드리지 않고 별도 headless Chrome 프로필로 검증했다.
  실제 하드웨어 휠로 영역 밖 브라우저 확대는 앞선 Ctrl+휠 작업과 동일하게 미확인이다.
- Java 서식 파일 7개 일괄 덮어쓰기는 자동 승인 검토에서 거부되어 실행되지 않았다.
  이후 별도 경로에 복사하고 전체 diff·토큰 동일성을 확인한 후 필요한 patch만 적용했다.
  해당 일괄 덮어쓰기를 재시도하지 않았으며 현재 미결 승인 차단은 없다.

증거는 Git 밖 작업 디렉터리 `../evidence/attributes-qa/`에 있다. `final-proof.json`,
`build-proof.json`, `tested-source.json`, `api-proof-final.json`, `evaluation-proof-qa.json`,
`legacy-api-proof.json`, XML들과 Chrome JSON/PNG를 참고한다. 쿠키·실제 저장 결과·JAR·이미지
및 검증용 빌드 산출물은 소스에 포함하거나 원격에 보내지 않는다.

추가 작업은 미지원 세 원본 수치의 정확한 표시 변환 근거와 사용자가 지목할 속성 확인,
복수 수치 모델의 상태/저장 크기 개선이다. 현재 확률과 상관관계를 보존해야 하며, 게임
검증 전에는 기존 가정을 검증된 분포로 바꾸지 않는다.
