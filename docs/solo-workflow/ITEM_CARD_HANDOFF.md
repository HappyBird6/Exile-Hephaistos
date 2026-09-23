# Item card 인계

- 작업: 거래소 참고 아이템 카드. 사용자 지시로 장식은 보류하고 희귀도 색상만 적용.
- 상태: 구현·검증·독립 리뷰 완료. 사용자 승인한 원본 로컬 master 직접 작업. Push 미실행.
- 관리자: /root. 작업자: /root/item_card (frontend 및 crafting-ui 문서). 리뷰어: /root/item_card_review.
- 경로: C:/SSAFY/PYJ/새 폴더/Exile-Hephaistos
- 기준: 1e59aa3. 구현 commit: 0de5ac3.
- 변경: 공통 ItemCard와 표시 adapter, 일반/매직/레어 색상, 카드 가격 제외, 경고·원문 분리, 기존 i18n 유지. 기본 베이스와 파싱 결과에 연결.
- 검증: frontend lint/typecheck/format:check, Vitest 34개, build, diff check PASS (작업자 실행). props 교체·기존 값 제거·가격 제외·원문 보존·언어 전환 포함.
- 관리자 확인: diff 확인, 임시 Vite 15173 브라우저에서 기본 일반 카드 배치 시각 확인. 임시 서버와 탭 종료. 매직/레어 브라우저 시각 검증 및 실제 Engine 연동은 미실행.
- 독립 리뷰: 1e59aa3..0de5ac3, 수정 필요 결함 없음. 리뷰어는 검증 재실행 없이 코드와 증거 검토.
- 남은 경계: 실제 제작 Engine 없음. 현재 카드 props 교체만 지원. 접두/접미·tier·range는 추측하지 않으며 미해석 행은 중립색 원문 유지.
- 사용자 backend Item/ItemState/Modifier 변경은 보존하고 commit에서 제외. 새 dependency/API 변경 없음.
- 수정 횟수: 0. 기존 Docker stack 재빌드/재시작 미실행. 실행 중 Docker 화면 반영은 scripts/dev.ps1 stack 필요.
