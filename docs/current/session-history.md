# 세션 히스토리와 공유

2026-11-05. 제작 실행과 독립된 읽기 전용 기능이다.

## 페이지와 기록

현재 세션 또는 History/Favorites의 세션별 링크로 `/history/:sessionId`를 연다. 브라우저의 기존 `hephaistos.workbench.films.v1`을 읽으며 선택·cursor·draft를 변경하지 않는다. 최초 상태와 전체 Frame을 기본 4열의 ㄹ 모양으로 표시하고 좁은 화면에서는 2열·1열로 전환한다. DOM·키보드 순서는 시간순이다.

화폐·소비 징조·반복 횟수·장착 Rune 라벨은 기존 기보의 `NotationResources`와 동일하다. 관측 및 미확인 action도 상태를 생략하지 않는다. 반복 Chaos의 저장되지 않은 중간 상태를 복원하지 않는다. 현재 ruleset과 상태 검증이 모두 통과할 때만 현재 catalog로 옵션을 표시한다. 다른 규칙의 표시 가능한 저장값을 보존하고 전체 원본은 JSON export에 유지한다. 제작 흐름 본문에는 상태 카드와 화폐 라벨이 있는 연결 화살표만 표시하며, 원시 JSON·내부 identity·중복 기보 목록은 노출하지 않는다. 기존 Augment ID·품질·심연·축성·원문·미해석 관측값·알려지지 않은 확장 데이터는 파일에서 보존한다.

## 파일

`exile-session`, `version: 1` JSON은 `exportedAt`과 전체 `film`을 담는다. UTF-8 2,000,000 bytes, 2,000 Frames, 깊이 32, 노드 100,000 한도이며 위험한 prototype 키·미지원 schema·잘못된 기본 구조는 거부한다. 게임의 실제 획득·화폐 소비 증거를 인증하는 형식이 아니다.

가져오기는 미리보기 후 명시적으로 추가한다. 같은 ID·동일 내용은 기존 기록을 유지하고 다른 내용은 새 ID를 부여한다. 원래 ID는 Film 내부 필드를 덮어쓰지 않고 바깥의 `Films.importSources`에 보존한다. 충돌 때문에 ID를 바꾼 파일을 다시 가져와도 기존 복사본을 유지한다. 기존 세션·활성 선택·원래 저장 bytes를 보존하고 기존 저장소의 한도와 반복 receipt 검증도 거친다. 저장 실패는 완료로 표시하지 않는다. 원문을 임의로 삭제·보정하지 않는다.

## 서버 공유

명시적인 공유 버튼은 `POST /api/v1/session-snapshots`로 현재 Film을 서버에 저장한다. 응답의 192-bit 무작위 opaque ID를 `/history/shared/:id`에 넣는다. `GET /api/v1/session-snapshots/:id`는 해당 immutable document를 반환한다. 다른 브라우저의 저장소가 비어 있어도 같은 서버에 접근하면 읽을 수 있다. 공유 기록을 여는 행위가 로컬 Film을 추가하거나 제작 실행을 허용하지 않는다. 편집하려면 파일 export/import와 기존 catalog 검증을 거친다.

저장 위치는 `crafting.session-snapshot-directory`이며 기본 `./data/session-snapshots-v1`이다. Compose에서는 기존 app-data 안의 별도 directory이고 자동 job의 파일은 건드리지 않는다. 500개·합계 100,000,000 bytes·문서당 2,000,000 bytes로 제한한다. 임시 파일 완성 후 atomic move하며 자동 정리·삭제·수정 API는 제공하지 않는다. quota·저장 불가 시 503, 없는 ID는 404, 구조 오류는 422, 과대 요청은 413 Problem Details로 응답한다. 내부 경로·예외·원문은 응답/로그에 기록하지 않는다.

이 경로는 과거 규칙의 문서를 보관하므로 제작 POST의 ruleset 실행 경계 밖에 있다. 익명 로컬 도구이며 소유자 인증·공개 서비스 접근 통제를 추가하지 않는다. 링크를 가진 사람은 해당 서버에서 원문을 포함한 snapshot을 읽을 수 있다. localhost 바인딩에서는 다른 기기가 접근할 수 없고 인터넷 공개 URL이 생기는 것은 아니다. 서버 바인딩·인증·배포 설정은 변경하지 않는다. 로컬 세션 URL은 해당 브라우저 데이터가 필요하고 서버 공유 URL과 구분한다. JSON·PNG는 서버 접근과 무관하게 전달할 수 있다.

## PNG

화면에 렌더된 제작 흐름 DOM의 computed style·실제 이미지·폰트와 줄바꿈을 유지해 PNG로 캡처한다. 텍스트나 카드를 별도로 재작성하지 않는다. SVG 화살표는 화면의 같은 벡터를 측정 크기로 래스터화해 포함하므로 Chromium의 중첩 SVG 캡처 누락을 피한다. Desktop·Mobile 각각 현재 배치 그대로 저장한다. 소수점 좌표의 외곽 픽셀도 화면 캡처와 같은 경계로 포함한다.

긴 흐름은 완료된 카드 행 경계에서만 분할하며, 행 사이 방향 전환은 다음 페이지에 온전히 포함한다. 한 장이면 바로 다운로드하고 여러 장이면 상단 다운로드 링크에서 각 장을 저장한다. 이미지를 본문에 중복 표시하지 않는다. 크기·pixel 예산을 초과하면 일부 기록을 자르지 않고 실패를 알린다. 원본 복원은 기존 JSON export/import를 사용한다. 공유 링크의 원문 포함·읽기 범위 안내는 사용자가 공유 동작을 시작할 때만 표시한다. 모든 UI 문구는 기존 6개 locale을 지원한다.

백엔드 구현은 `workbench/history`에서 관리한다. API → `SessionSnapshotService` → `SessionSnapshotRepository` → `FileSessionSnapshotRepository`로 연결하며, 운영 설정과 각 소비 파일은 [backend-settings.yaml](../../backend/src/main/resources/backend-settings.yaml) 주석에 정리했다. 기존 `crafting.session-snapshot-directory`도 계속 지원한다.
