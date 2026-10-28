# 제작 안내 화면 가독성 개선 검증

## 변경
선택 아이템 → 제작 순서 → 다음 제작 선택의 순서로 오른쪽 패널을 구성했다. 중복 아이템 상태 제목, 내부 노드 번호와 GOAL 코드, API 및 계산 구조 설명을 기본 흐름에서 제거했다. 화폐 순서는 그대로 유지하며 1회 또는 N회 조건과 결과 확률을 방법 요약에 표시한다.

다른 아이템과 경로, 목표 희귀도, 상세 결과 확률 및 예상 사용량은 펼쳐 확인한다. 첫 제작 선택만 기본으로 펼치며 다른 선택을 삭제하거나 임의로 순위화하지 않았다. Exalted 그룹과 정확한 개별 결과 선택, Essence 결과, 시작 아이템으로 사용, 시작/목표 초기화, 로컬 복원과 취소/재개는 유지한다.

지원 범위, 허용/제외 화폐, 반복 전제, 예상 비용과 실제 지출의 차이는 기존 정책 창에 모았다. 옵션 제거 위험, 미해결 계산, 해법 없음, 최대 자원 한도와 오류는 작업 화면에 남겼다. 정책 링크는 정책 버튼 위치로 이동하며 버튼을 눌러 상세 설명을 연다.

기본 글자 1rem, 줄간격 1.65, 밝은 본문과 단색 배경, 44px 조작 높이, 여백과 focus 표시를 적용했다. 작은 화면은 아이템/제작 패널이 그래프보다 먼저 나온다. 상태 그래프는 작은 원 대신 글자를 담는 둥근 사각형을 사용한다. 기존 6개 언어를 유지한다.

## 검증과 한계
사용자가 제공한 318×765 원본 스크린샷을 직접 읽었다. 연결된 브라우저 목록은 비어 있어 변경 후 실제 픽셀·모바일 실기기 확인은 수행하지 못했다. DOM 테스트는 시각적 검수와 구분한다.

초기 전체 검사에서 목표 희귀도 표시 누락과 확률 assertion의 잘못된 DOM 범위를 발견했다. 목표 희귀도는 접힌 상세 영역에 복원하고 확률은 실제 전체 문장과 정확한 25.00%, 선택 상태 1, 화폐 이름을 검증하도록 고쳤다. 테스트를 단순 존재 검사로 약화하려던 수정은 자동 승인 검토에서 거부되어 실행되지 않았으며, 정확한 상태/확률 검증을 유지하는 수정으로 대체했다.

## 임시 판단
- 기본 화면은 선택 아이템과 다음 제작을 우선한다. 내부 식별자는 사용자 이름으로 바꾼다.
- 목표 희귀도는 의미 있는 목표 입력이므로 삭제하지 않고 접어 둔다.
- 첫 방법만 펼치되 기존 방법 순서와 모든 선택을 유지한다. 이것은 최적 경로 보증이 아니다.
- 구체적인 옵션 제거 경고는 정책으로 숨기지 않는다.
- 서버 확률과 반복 모델은 변경하지 않는다. 이번 변경은 프런트 표시와 정보 구조에 한정한다.
- 기존 backend·PostgreSQL·Redis를 재생성하지 않고 frontend만 localhost에 갱신한다. 원격 push/merge/deploy는 수행하지 않는다.
## 최종 결과
- 제품 commit: e896ed73b40c9d6ab023c22a901b999bb19a4a18.
- 최종 소스: npm ci, lint, typecheck, format:check 성공. 테스트 101개 파일, 2,216/2,216 성공. Vite build 성공(기존 큰 bundle 경고 유지).
- 검증 wrapper는 모든 명령과 dist 복사 이후 마지막 CR 빈 줄에서 exit 127을 반환했다. 테스트 JSON의 success=true 및 최종 dist 파일을 별도 확인했다. 검증 성공과 wrapper 오류를 구분한다.
- Backend는 변경하지 않았다. 이전 최종 소스의 836개 성공 결과를 유지하며 이번 UI 변경에서 새로 실행한 것으로 주장하지 않는다.
- localhost:8081 HTTP 200, backend readiness UP. 제공 JS /assets/index-BIoHlkec.js SHA256 E7451275C69404C21A5652BA075D444AB6E8B09077E18E73676640F052727E96가 검증 dist와 일치한다.
- frontend image exile-hephaistos-frontend:ux-e896ed7. app image ui-outcomes-962a3a9 유지. app·PostgreSQL·Redis 컨테이너 ID, .env, 기존 저장 작업 13개 hash 보존 확인.
- 증거: task-2/ux-tests-final.json, ux-check.log, ux-deployment-verification.json, ux-preserved-containers.txt.
- 남은 한계: 실제 브라우저 픽셀 및 터치 조작 검증 미실행. DOM 기반 검증을 시각 검수로 대체했다고 주장하지 않는다.
## 추가 실제 브라우저 시각 검증
기존 scripts/qa/ui-feedback-20261004/README.md와 browser.cjs에서 문서화된 Playwright 이미지 exile-workbench-browser:20261002를 발견했다. 로컬 이미지 sha256:9e71b7eb5aa30236994743fe29aa564c69cbb336942914370cab77df49718260을 --pull never로 재사용했다. 추가 설치나 보안 설정 변경은 없었다. 기존 frontend package에는 Playwright가 없고 jsdom 테스트만 있으나 scripts/qa의 별도 이미지에 실행 가능한 브라우저가 있었다. 앞선 브라우저 미검증 한계는 아래 확인 범위에 한해 해소되었다.

실제 localhost:8081 제공 번들을 새 Playwright context에서 실행했다. 기존 검증용 합성 시작/목표와 저장된 API 응답을 임시 context에 넣고 자동 탐색 API는 route.fulfill로 가로챘다. 사용자 프로필/localStorage 및 서버 작업은 수정하지 않았다. 1280px 화면과 375px 화면에서 우측 패널, 정책 창, 엑잘 그룹/개별 상태 펼침/개별 아이템 상세/마지막 결과 스크롤을 실행하고 내부 스크린샷 픽셀을 직접 읽었다.

- 좁은 패널 실제 폭 317px, scrollWidth 317px, 본문 16px. 선택 패널 하위 요소의 visible 가로 넘침 0개.
- 기본 본문 색 #f1f4f8, 패널 #232b35. 중복 상태 제목 및 내부 ID가 없고 긴 화폐 조합은 줄바꿈된다.
- 정책 창에서 지원 범위, 확률과 반복, 예상 비용과 기록이 별도 표시된다.
- 엑잘 그룹 실제 폭/scrollWidth 모두 317px. 개별 상태 펼침 후 버튼은 '결과 접기', 아이템 details open=true. 결과 확률, 목표 속성 상실 및 계산되지 않은 부분 표시가 남는다.
- 결과가 긴 그룹은 그래프 영역의 내부 세로 스크롤로 끝까지 접근한다. 전체 그룹 element screenshot은 스크롤 조상에 잘려 찍혀 viewport screenshot으로 상세와 마지막 행을 다시 확인했다.
- 별도 임시 페이지 빌드는 기존 rolldown native binding 누락으로 실패했다. 설치하지 않고 실제 서비스 번들 검증으로 전환했다. 처음 단일 결과 사례에서는 그룹이 생성되지 않아 그룹 screenshot 대기가 만료됐으며, 다중 결과가 실제 존재하는 기존 사례 3으로 다시 확인했다. 제품 실패로 숨기거나 입력 확률을 변경하지 않았다.
- 추가 제품 수정은 하지 않았다. 서비스는 e896ed7 그대로다. 한국어/두 viewport의 합성 입력 시각 확인이며 모든 언어·실기기·모든 제작 분기의 시각 품질을 보증하지 않는다.

내부 증거: task-2/ux-browser.cjs, ux-exalt-browser.cjs, ux-visual-measurements.json, ux-exalt-measurements.json, ux-inspector-desktop.png, ux-inspector-mobile.png, ux-policy-mobile.png, ux-exalt-detail-viewport.png, ux-exalt-last-viewport.png. 사용자용 스크린샷 업로드는 하지 않았다.