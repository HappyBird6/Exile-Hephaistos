# Quality cap 감소 정정

사용자 확인(2026-10-04): “최대 퀄40인채로 20으로줄어들어도 퀄40은 유지돼.” 이전 `min(existingQuality,newCap)` clamp 가정은 폐기한다. 게임 원문은 maximum quality를 설명하지만 이 변경의 직접 근거는 사용자의 인게임 확인이다.

`QualityCapChangePolicy.DEFAULT=PRESERVE_EXISTING`, ruleVersion suffix `quality-cap-preserve-v2`를 사용한다. accepted operation은 현재 품질·유형과 살아남은 원본 roll을 보존한다. cap 삭제 후보를 제외하거나 전체 action을 거절하지 않으며 기존 후보 확률·Omen 소비 규칙을 유지한다. cap 증가만으로 품질을 자동 충전하지 않는다. 이전 clamp assumption ledger는 새 결과에 생성하지 않는다. `REJECT_OVERCAP`은 명시적 과거 정책 비교용 constructor toggle로만 남긴다.

현재 cap과 저장 수치의 유효 범위는 다르다. Solar는 확인된 도달 가능한 수치인 정수 0–40을 허용한다. 현재 cap20인 저장 상태도40을 가질 수 있다. Ring/Sapphire는 확인된 0–20 범위를 유지한다.41 이상 Solar,21 이상 Ring/Sapphire, 소수, 음수, 잘못된 유형/형태·base·snapshot·modifier roll은 계속 거절한다. Stateless ItemState에는 게임 획득 이력이 없으므로 과거 Breach 적용을 암호학적으로 증명하는 모델은 추가하지 않는다. 새 입력으로 임의의100을 허용하는 변경이 아니다. 기존 시작 editor의20 입력 경계는 유지한다.

Catalyst 재사용·유형 변경의 실제 게임 결과는 이번 사용자 문장에 완전히 명시되지 않았다. Simulator의 일관된 가역 정책으로 `amount=max(existingAmount,currentCap)`을 중앙 정책에 두고 선택된 type으로 교체한다. 따라서 inherited40을 다시20으로 낮추지 않는다. 이는 기존 한 번 사용으로 cap까지 설정하는 simulator 편의 정책의 확장이며 실제 한 번 사용 효과라고 주장하지 않는다. 실제 게임의 재사용/전환 증거를 얻으면 이 함수를 교체한다.

Backend 검증, API 응답 수치 검증, 품질 projection, 여섯 언어 안내, 저장 film 검증·복원은 같은 보존 규칙을 적용한다. 기존 clamp로 이미 저장된20은 소실된 값을 추측하여40으로 되돌리지 않는다. 개인 item 원문·storage·기존 evidence는 수정하지 않는다.

검증 결과는 [validation evidence](evidence/workbench-quality-preserve-validation-2026-10-04.json), 다음 범위는 [Jewel/Liquid 계획](workbench-jewel-liquid-next-2026-10-04.md)에 기록한다. 이 작업은 local branch commit까지만 수행하며 사용자 서버18080/18081는 갱신하지 않는다.
