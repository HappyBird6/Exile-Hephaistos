# 배포 보안과 비용 보호

이 문서는 현재 방어 코드와 배포 시 연결할 설정의 기준이다. 게임 규칙은 별도 catalog 계약을 따른다.

## 적용 상태

- Nginx: IP별 요청·연결 제한, 응답별 전송 속도 제한, 본문·시간 제한. 429는 Nginx에서 종료하고 무작위 request ID 기준 약 1/16만 회전되는 컨테이너 로그에 남긴다. 이 표본은 관리자 DB 목록에 포함되지 않는다. 차단 건마다 백엔드·DB에 요청하지 않는다. 따라서 관리자 요청제한 목록에는 백엔드가 실제 수신한 거절만 나타난다.
- 백엔드: 전체 API 처리량과 동시 실행, 제작·반복 실행 예산, 정규식 복잡도 제한. 재전송 중복 실행은 Redis 영수증으로 방지한다.
- 관리자: 환경변수 비밀번호, 인증 실패 제한, 페이지 이탈 시 인증 해제. HTTP Basic을 사용하므로 HTTPS 필수. 운영 비밀번호는 개발용 1234를 사용하지 않는다.
- 상세 로그: 백엔드 429는 프로세스당 초당 최대 1건만 저장한다. 하루 상세 기록도 `backend.admin-logs.daily-records`로 제한한다. 제한/대기열 초과는 관리자 dropped에 합산한다. 목록·날짜별 그래프는 저장된 기록 통계이며 누락이 있으면 전체 실제 요청 수와 다르다. 하루 기록 예산은 프로세스 재시작 시 초기화되므로 DB 볼륨 용량 제한을 대체하지 않는다.
- DB는 한국시간 오늘과 지난 7일을 제공한다. 만료 기록은 압축 파일 저장 뒤 DB에서 삭제한다. 압축 파일은 기본 생성 후 30일/총 1 GiB 이내로 보관하며 오래된 파일부터 삭제한다. 정리 주기 사이에는 한 배치 크기만큼 초과할 수 있다. 보관 한도에 따라 다운로드 가능한 과거 기록이 삭제되는 정책이다.
- 작업대 배포 overlay는 서비스 CPU·메모리와 컨테이너 stdout/stderr 로그 회전(10 MB × 3)을 제한한다. 자동 증설은 구성하지 않는다. 메모리 상한은 클라우드 청구 상한이 아니다.

## 수정 위치

| 목적 | 원본 |
|---|---|
| IP·연결·전송 제한, CDN 앞단과 origin 경계 | `infra/docker/nginx.conf` |
| API/제작 예산, 로그 기록·보관 제한 | `backend/src/main/resources/backend-settings.yaml` |
| 반복 전송 계약 | `backend/src/main/resources/contracts/chaos-repeat.json` |
| 컨테이너 자원·로그 회전·Redis 영속성 | `infra/compose.workbench.yaml` |

```sh
docker compose --env-file .env -f infra/compose.yaml -f infra/compose.workbench.yaml --profile stack config --quiet
# 승인한 배포 호스트에서만 실행
# docker compose --env-file .env -f infra/compose.yaml -f infra/compose.workbench.yaml --profile stack up -d
```

## CDN/WAF 연결: 공급자 선정 후 적용

현재 클라우드 계정·도메인은 미정이며 외부 WAF나 청구 알림을 생성하지 않았다. 다음 설정을 실제 계정에 적용한 뒤 공개한다.

1. 관리형 DDoS 보호/WAF를 활성화하고 API 경로에 요청 제한을 적용한다. 정상 검색엔진·일반 방문자에 일괄 CAPTCHA를 적용하지 말고 과도한 요청에 challenge/block을 적용한다. 반복 제작 API는 일반 API보다 낮은 예산을 둔다.
2. 정적 `/assets/`는 CDN 캐시를 사용한다. `/api/admin/`, POST 제작, 오류 응답은 캐시하지 않는다. 게임 catalog는 규칙 identity가 포함된 URL만 장기 캐시한다.
3. 원본 서버는 private tunnel 또는 방화벽에서 CDN 출발지와 운영자 접근만 허용한다. 공개 origin IP/별도 포트로 CDN을 우회할 수 없어야 한다. 현재 Compose의 공개 포트는 loopback에만 bind한다. 이를 `0.0.0.0`으로 풀어 직접 공개하지 않는다.
4. CDN의 실제 클라이언트 IP 헤더는 해당 공급자의 검증된 출발지에서만 신뢰한다. Nginx real_ip 신뢰 범위와 Tomcat forwarding을 함께 확인한다. 이를 빠뜨리면 여러 사용자가 하나의 proxy IP로 제한된다.
5. HTTPS, 관리자 강한 비밀번호, 건강 상태 외 내부 actuator 비공개를 확인한다.

## 실제 비용 상한: 공급자 선정 후 적용

기본 운영은 인스턴스 1개로 시작하며 자동 증설·서버리스 무제한 실행을 켜지 않는다. 제공 서비스에서 최대 인스턴스 수 1과 예약된 CPU/메모리 크기를 명시한다. 사용자 수와 실측 결과에 따라 변경한다.

계정에서 월 예산, 50/80/100% 비용 알림, 전송량·요청량 알림을 설정한다. 알림은 비용 차단이 아니다. 공급자가 hard spending cap을 제공하는지 확인하고, 제공하지 않으면 임계치 도달 시 CDN에서 동적 API를 차단하거나 origin을 정지하는 공급자별 제어가 필요하다. 이 동작은 가용성을 중단하므로 운영자가 임계치·알림 수신처·복구 절차를 확정해야 한다. 현재 저장소에는 금액을 임의로 정하거나 원격 중단을 수행하는 자격증명이 없다.

DB/로그/Redis 볼륨은 별도 디스크 용량·사용량 알림을 설정한다. Docker 메모리 제한과 로그 회전은 DB 볼륨의 디스크 quota가 아니다. 압축 보관 실패·dropped·디스크 여유 공간을 관리자에서 확인한다. 로그는 인증 헤더·본문·쿼리 문자열을 담지 않지만 IP가 포함되므로 접근 권한과 보관 정책을 유지한다.

## 검증

- API request protection, workload admission, 관리자 인증 및 로그 저장 테스트.
- 거절 폭주에서 상세 기록량이 제한되는지 확인하고 누락 표시를 숨기지 않는다.
- 압축 보관 만료 파일 삭제와 현재 DB 날짜 보존, 경로 밖 파일 접근 금지 확인.
- Nginx `nginx -t` 및 실제 origin 직접 접근 실패, 위조 forwarded header 실패는 배포 환경에서 확인한다.
- 대시보드는 한국시간 하루 1,440분을 표시한다. 로그 탭은 필터·저장 기록 요약·목록으로 구성한다.
- 메모리는 JVM pool별 사용량/상한과 GC 횟수/시간을 표시한다. JVM pool 합계는 프로세스 RSS·컨테이너 전체 메모리와 같지 않다. pool 상한 미제공 시 `—`를 표시한다.

## 임시 아이디·비밀번호 로그인

`/api/account`는 관리자 인증과 별개다. 비밀번호는 BCrypt로 저장하며 Redis 서버 세션을 사용한다. 로그인 성공 시 기존 세션을 폐기하고 새 세션을 만들며, 로그아웃과 30분 비활동 만료 후에는 계정 데이터에 접근할 수 없다. 수정 요청에는 세션에 연결된 CSRF 토큰, 계정 저장 요청에는 현재 계정 ID도 필요하다. 비밀번호와 요청 본문은 운영 로그에 남기지 않는다.

로그인·회원가입은 IP별 분당 요청 수와 서버별 동시 비밀번호 연산 수를 제한한다. 쿠키 없이 `/api/account/me`·`/api/community/context`를 반복 호출해 Redis 로그인 세션을 늘리는 요청도 IP별로 합산하여 기본 분당 60개로 제한한다(`account.auth.new-sessions-per-minute`). 기존 쿠키를 사용하는 방문자는 이 신규 생성 예산을 소모하지 않는다. 여러 사용자가 같은 IP를 사용하는 환경에서는 이 값을 실제 유입량에 맞춰 조정한다. Redis에 연결할 수 없으면 인증을 우회하지 않고 실패한다. 설정 원본은 `backend-settings.yaml`의 `account` 항목이다. 쿠키는 HttpOnly·SameSite=Lax이며, HTTPS 배포에서는 `ACCOUNT_COOKIE_SECURE=true`로 설정한다. `ACCOUNT_AUTH_ENABLED=false`이면 사용자 인증과 계정 저장 API를 비활성화할 수 있다.

현재 임시 계정에는 비밀번호 재설정·이메일 복구 기능이 없다. Google 로그인 도입 시 기존 계정에 대한 인증을 확인한 뒤 연결해야 하며, 표시 이름만으로 계정을 합치면 안 된다. 관리자 인증과 기존 제작 API 권한은 변경하지 않는다.


## 의존성 보안 패치와 남은 경고

2026-11-12 검수에서 Boot 3.5 계열을 유지하면서 Jackson 2.21.7, Netty 4.1.137.Final, Log4j 2.25.5, Tomcat 10.1.59, PostgreSQL JDBC 42.7.12로 보안 패치를 적용했다. 원본은 `backend/build.gradle.kts`이며 같은 라이브러리 계열은 BOM으로 맞춘다. Boot BOM이 이 버전 이상을 제공하면 별도 패치 설정을 제거할 수 있다. Vite 8.3.8, Vitest 5.1.0 및 호환 가능한 전이 의존성 패치는 `frontend/package-lock.json`에 고정한다.

- OSV 실행 의존성 99개 재조회: 경고 대상 구성요소 8개 → 1개. Spring MVC 6.2.19에는 [XSLT view 경로 처리](https://github.com/advisories/GHSA-pc63-qcmh-9cmg), [SSE view fragment 처리](https://github.com/advisories/GHSA-j9f9-w8pj-32f8) 경고가 남아 있다. 현재 앱은 XsltView·SSE view fragments를 사용하지 않는다. 해당 기능을 추가하기 전에 수정 버전으로 전환해야 한다. 공개된 6.2 계열 패치가 확인되지 않아 Spring 7/Boot 4로 임의 변경하지 않았다.
- npm 재조회: high 4개 → 0개, 실행 의존성 경고 0개. 개발 도구에는 [minimatch globparts 증폭](https://github.com/advisories/GHSA-g3hv-75wg-m249)의 직접·간접 경고 10개(moderate)가 남는다. ESLint/TypeScript ESLint가 요구하는 minimatch 10 계열에는 확인된 수정 버전이 없다. lint glob에 외부 사용자 입력을 전달하지 않으며, 배포 산출물에는 이 도구를 포함하지 않는다. 상위 도구의 호환 패치를 추적하고 재검사한다.

경고가 남는 항목은 검사에서 숨기거나 allowlist로 통과시키지 않는다. 위 수치는 해당 일자의 조회 결과이며 향후 공지에 따라 달라질 수 있다.


계정 문서 묶음 조회는 `account.library.read-batch-size`(기본 5개)를 인덱스 응답 헤더로 알린다. 인증·CSRF·소유권을 확인하고, 요청한 버전 또는 세션 삭제 세대가 달라졌으면 전체 묶음을 409로 거절한다. 문서 조회는 계정 범위를 지정한 단일 SQL로 실행한다. API 전역 예산에서도 묶음 크기만큼 차감하여 요청 횟수 감소가 응답량 제한 우회로 이어지지 않게 한다. 기본 문서 크기 제한과 조합하면 응답 본문은 묶음당 약 1 MB 이하이며, 배포 중 기존 서버에 연결된 프런트엔드는 개별 조회로 호환한다.
