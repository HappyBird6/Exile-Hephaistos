# 세션 히스토리·휴대 파일·서버 공유·PNG 검증

2026-11-05. 기준 `ceab37535c2ae8fcb3b3432b65bbb822b7a35ace`, branch `codex/session-history-sharing`.

## 결과와 경계

- 별도 `/history/:sessionId`, 서버 공유 `/history/shared/:opaqueId`, 기본 4열 ㄹ 모양·2열·1열, 기보 공통 라벨, 원본 상태와 미해석 데이터 보존을 구현했다.
- 서버에 실제 snapshot을 저장하므로 브라우저 저장소가 없는 별도 context에서도 공유 링크가 열린다. 기본 저장소는 app-data의 `session-snapshots-v1`이며 자동 job과 분리한다. 링크 소지자에게 원문까지 공개되는 범위를 화면에 설명한다. localhost 서버에서 인터넷 공개나 다른 기기 접근이 자동으로 가능해지는 것은 아니다.
- JSON 미리보기·충돌 ID 분리·같은 충돌 파일의 재가져오기 중복 방지·기존 세션과 quota 보존을 구현했다. 원래 ID는 바깥 저장 metadata에 남겨 Film의 알려지지 않은 필드를 덮어쓰지 않는다.
- PNG는 공유용 텍스트 카드와 기보 이름을 4개 상태씩 분할한다. JSON이 복원용 원본이다. 이미지 높이·합산 pixel 예산을 초과하면 일부 내용을 자르지 않고 실패한다.
- 2번 양방향 텍스트, 원본 UI·룬 배치·이미지, 운영 배포는 변경하지 않았다. 표시 goldens의 값은 유지하고 변경된 CraftingPage source hash만 갱신했다.

## 검사

| 검사 | 결과 |
|---|---|
| npm ci | 통과, 의존성·잠금 파일 변경 없음 |
| Frontend lint / typecheck / format:check / build | 통과 |
| Frontend 전수 4 workers | 3,011개 중 2,996개 통과, 15개 실패; 아래 구분 참고 |
| 직접 영향 단일 worker | 10개 파일, 229개 통과 |
| 최종 transfer 검사 | 4개 통과: 원문·확장 데이터·legacy 보존, 충돌·재가져오기, 파일 및 합산 quota 방어 |
| 전수에서 일시 실패한 CraftSupport/CraftStart | 기준 코드 28개 통과, 현재 코드도 같은 단일 worker 조건에서 28개 통과 |
| Backend SessionSnapshot·ArchUnit | 5개 통과 |
| Docker 통합 검사 | 8개 통과 |
| generateJooq / bootJar / Spotless | 통과, migration을 적용한 임시 DB만 사용 |
| 실제 서버·Edge·PNG | 29개 통과, pageerror 없음. [checks.json](checks.json) |

전수 실패 15개 중 기존 ItemTextImport focus 8개와 Sapphire 1개는 그대로 구분했다. UI 3개 시간 초과는 WorkbenchLayout·MaterialStash 직접 영향 재검사에서 통과했다. CraftSupport·CraftStart 2개는 기준 코드와 현재 코드의 단일 worker 검사에서 모두 통과했다. source hash 1개는 표시 기대값을 변경하지 않고 정확한 변경 소스 해시를 갱신한 뒤 통과했다. 병렬 부하가 있는 전수 결과를 전체 통과로 보고하지 않는다.

전체 Backend `check generateJooq bootJar --continue`는 기존 `BaseRegistryParityTest`·`EmptyRareApiRegressionTest` 실패를 확인했고 자동 탐색 계산이 장시간 지속되어 중단했다. 이후 직접 영향·ArchUnit·통합·codegen·빌드는 별도로 완료했다. skip·timeout·기대값 완화는 하지 않았다. Compose와 Windows 실행 스크립트는 변경하지 않았다.

## 실제 화면

합성 공개 베이스와 실제 임시 서버의 제작 응답으로 9개 상태를 만들었다. 개인 clipboard·기존 browser storage는 사용하지 않았다. [재현 검사](../../../scripts/qa-session-history.mjs)는 격리된 Edge context를 사용한다. 검증용 Playwright는 node_modules 내부 별도 directory에만 설치했다.

- [Desktop](desktop.png)
- [Mobile](mobile.png)
- [PNG 첫 장](snapshot-1.png)

검증 포트는 Backend 19310, Frontend 19311, PostgreSQL 19332, Redis 19379이며 컨테이너 이름은 `session-history-postgres`, `session-history-redis`였다. 자신의 임시 서버·컨테이너·저장소를 정리했고 운영 9000/9001과 다른 작업의 자원은 보존했다. 검사 로그는 Git에 포함되지 않는 backend/build/session-history-verification에 보존했다. 푸시·원본 checkout 통합·외부 배포는 수행하지 않았다.
