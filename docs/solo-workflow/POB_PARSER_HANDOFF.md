# PoB parser 보강 인계

- 상태: 구현·검증·독립 리뷰 완료. 사용자 지시에 따라 원본 local master 직접 작업. Push 미실행.
- 요청: PoB 기반 파서와 frontend 보강, 영어 전용 전환, 모든 parser 전용 클래스를 item/testparser에 배치.
- 관리자 /root, 작업자 /root/pob_parser_upgrade (backend/frontend/API 문서), 리뷰어 /root/pob_upgrade_review.
- 작업 경로: C:/SSAFY/PYJ/새 폴더/Exile-Hephaistos
- 기준 bce8eab → 구현 8e211286dffe7ea7a4e002a4b0aa9810e3b38ae6.
- PoB 기준 commit ce566eac45ea8a86477f513c7ee65a1ebe60014e. 출처·fixture hash·라이선스는 docs/item-text-parsing.md 참조.
- 변경: 영어 header/property/requirements/flags 및 advanced metadata 처리, modifier 표시 DTO, prefix/suffix/tier 명시값 표시. 서비스·모델·Controller·오류 처리와 테스트를 item.testparser로 모음.
- 영어 locale만 활성화하고 ko 리소스 제거. 과거 ko 저장값은 English fallback, 언어 선택기는 한 언어일 때 숨김. 확장용 registry 유지.
- 검증: backend check 65 unit + 9 integration PASS (ArchUnit 포함), bootJar PASS. 관리자 XML 결과 확인. frontend lint/typecheck/format:check/test 38/build PASS (작업자 실행).
- 독립 리뷰: 기준..구현 SHA diff와 테스트 증거 확인, 수정 요구 없음. 리뷰어는 테스트 재실행하지 않음.
- 관리자 통합 smoke: 임시 JVM 18080와 Vite 15173에서 영문 advanced Rare 입력 → P1/S3/타락/색상/가격 미표시 확인. Magic 입력 교체 → 이전 값 제거·새 레벨/옵션·미확인 class/base 경고 확인. 화면 영어 및 언어 선택기 없음 확인. 임시 서버·탭 종료.
- 제한: PoB 전체 semantic parser나 catalog clone은 아님. Magic base/affix ID, 실제 옵션 효과·확률·수치 재계산은 검증 catalog 부재로 미지원. 원문/미해석 행 보존. 실제 제작 Engine 후속.
- diff-check 예외: 실제 source fixture의 Sockets 행 끝 공백 1개를 원문 hash 유지를 위해 보존.
- 사용자 Item/ItemState/Modifier 4개 변경 보존, commit 제외. 기존 Docker stack 재빌드/재시작 없음. 화면 반영 시 scripts/dev.ps1 stack 필요.
- 리뷰 후 수정 횟수 0. 다음 작업: 검증 catalog 연결 또는 실제 입력 사례 기반 파서 확장.
