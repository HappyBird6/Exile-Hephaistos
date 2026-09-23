# Material tabs 인계

- 상태: 구현·검증·독립 리뷰 완료. master 반영/push 미실행.
- 관리자 /root, 작업자 /root/material_tabs, 리뷰어 /root/material_tabs_review.
- 경로: C:/SSAFY/PYJ/새 폴더/Exile-Hephaistos. 사용자 요청으로 현재 checkout에 새 branch 생성.
- 기준 master d86ac8c. Branch codex/crafting-material-tabs. 구현 2cbbf9e, 모바일 정렬 수정 6437567.
- 사용자 CraftingPage.tsx 미커밋 삭제 사항을 보존하며 승인 범위 수정과 함께 commit. 작업 중 새로 나타난 backend 변경은 모두 제외.
- 내용: Currency21, Hinekora를 Chance 위치로 이동. Essence95/Omen50/Catalysts26/Liquid27 정렬 목록. 탭 공용 5행×3열 favorites, 좌클릭 들기/등록/덮어쓰기, Esc 취소, 탭 변경 유지, 등록 재료 선택. 메모리 상태만 유지.
- 배치: inspector 및 오른쪽 sidebar 제거, item 하단 Chaos 행 정렬, ItemCard만 좌하단, 우하단 공백. 입력/경고/원문은 Edit item 패널.
- 출처: frontend/public/assets/materials/sources.json. 198개 목록, 196개 이미지 hash 검증. Perfect Essence of the Mind와 Reaver Catalyst는 원본 CDN HTTP403, 동일 URL 재확인 후 중단. 항목명/fallback 유지. 목록은 페이지 주 카탈로그 기준으로 Alloy/Ancient 등 포함, 게임 효과 지원 의미 아님.
- 검증: frontend lint/typecheck/format:check/test45/build/diffcheck PASS. 독립 리뷰에서 196개 이미지 SHA 직접 대조 PASS. 테스트 실행은 작업자, 리뷰어는 코드/증거 확인.
- 관리자 브라우저: 기본 Currency21 및 15칸, Essence 목록, 재료 든 상태로 Currency 전환 후 등록, 다른 재료 덮어쓰기, 탭 유지, favorite 우클릭 선택, 입력 패널 열기/닫기 확인.
- 관리자 실측: desktop Chaos/favorite15 각86.890625 정사각형, item 포함 하단910.5625 동일. mobile390 각28.796875 정사각형, item 포함 하단500.203125 동일, 가로 overflow 없음. 초기 모바일 빈 칸 높이42 문제는 수정1회로 해결. viewport 복구 및 임시 Vite15173/탭 종료.
- 리뷰: d86ac8c..2cbbf9e 및 2cbbf9e..6437567 모두 추가 수정 요구 없음.
- 제한: 실제 제작 Engine 미연결. Backend는 변경하지 않았고 현재 사용자 backend 변경과 실제 API 통합 smoke는 이번 범위 밖. 기존 입력 흐름은 frontend mock 회귀로 확인. 기존 Docker 재빌드/재시작 없음.
- 다음 행동: 필요 시 사용자 검토 후 Git 통합. 추가 탭의 최종 UI는 미정으로 정렬 목록만 제공.
