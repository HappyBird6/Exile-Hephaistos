# 소켓과 Augment

2026-11-05. Workbench 시뮬레이션 정책이며 실제 게임의 화폐 사용 한도나 드롭 확률을 보장하지 않는다.

## 소켓 정책

사용자 요청에 따라 등록된 일반 장비에서 통상 소켓 한도에 추가 1개를 허용한다. 별도 Exceptional 플래그·드롭 재현·아이템 레벨 조건을 요구하지 않는다. `Artificer's Orb` 1회는 빈 소켓 1개만 추가한다.

| Workbench 한도 | 지원 class |
|---|---|
| 2 | Gloves, Boots, Helmets, Wands, Shields, Bucklers, One Hand Maces, Spears |
| 3 | Body Armours, Bows, Crossbows, Two Hand Maces, Quarterstaves, Staves, Talismans |
| 미지원 | Amulets, Rings, Belts, Jewels, Quivers, Sceptres, Foci 및 미등록 베이스 |

등록된 179개 베이스 중 94개를 지원한다. class 전체 게임 지원 주장이 아니다. `base-policies.json`과 등록된 베이스만 대상이다. Sceptres/Foci는 이번에 장착 자격·상한을 확정하지 않았다. Unique, Runeforged, Runemastered, 소켓 변환·훼손으로 추가하는 별도 작업은 구현하지 않았다.

[Exceptional Item](https://poe2db.tw/us/Exceptional_Item)의 추가 소켓 설명과 장비별 Exceptional 노드가 추가 1개를 허용하는 정책의 배경이다. 이것이 일반 오브로 추가 한도를 뚫을 수 있다는 게임 증거는 아니다. [Craft of Exile](https://beta.craftofexile.com/?game=poe2&group=2&class=12&item=3000)에서 일반 Tasalian Greaves의 소켓 1개, 상한 안내, Desert Rune 장착 후 +14% Fire Resistance를 직접 확인했다. 본 Workbench는 추가 소켓을 조건 없이 실험할 수 있도록 차이를 카드에 표시한다.

## 장착 데이터와 상태

63종 Rune(Lesser 16·일반 16·Greater 16·Perfect 15)과 16종 일반 Soul Core를 등록한다. Perfect Tempered Rune는 검토된 일반 효과 행이 없어 제외했다. 전체 식별자와 장비별 효과는 `frontend/src/features/crafting/augments.json` 및 동일한 Backend `catalog/augments.json`에 있다. 장비별 `socketable` 행만 사용하며 `bonded` 행을 일반 효과로 합치지 않는다. 해당 class의 효과 행이 없는 조합은 거절한다.

2026-11-05 선택 정책: 검토된 Perfect 일반 효과가 있는 15개 Rune 계열은 새 선택·검색 결과·즐겨찾기 등록·장착에서 Perfect만 제공한다. Perfect 일반 효과가 검증되지 않은 Tempered는 기존 Lesser·일반·Greater 3종을 유지하며, 일반 Soul Core 16종도 유지한다. 새 선택 대상은 총 34종이다. `selectionEnabled`는 이 작업대 정책이며 게임 등급이나 드롭 규칙을 재정의하지 않는다. 전체 79종의 ID·효과는 보존하므로 기존 하위 등급 장착 상태의 검증·열람·일반 제작은 계속 가능하다. 기록을 삭제·변환하거나 과거 ruleset identity를 새 값으로 재라벨링하지 않는다. 새 장착 API는 정책으로 제한된 등급을 422로 거절한다.

피드백의 Rune/Soul Core 효과는 PoE2DB의 [Rune 구분](https://poe2db.tw/kr/Rune)을 따라 무도 무기, 마법봉 또는 지팡이, 방어구로 묶는다. 같은 그룹 내 동일 효과를 한 번 표시하고, 실제 효과나 번역 문구가 다른 경우 해당 부위와 원문 값을 보존한다. 검증 데이터에 없는 장신구 효과는 만들지 않는다. 표시된 그룹명·효과는 재료 검색에도 사용한다. 그룹 표시는 장착 자격을 넓히지 않는다.

`augmentSockets`는 총 구멍 수, `installedAugments`는 `slotIndex`와 `augmentId`의 목록이다. 효과 문구와 수치는 검토 카탈로그에서 조회한다. 클라이언트가 제출한 임의 효과는 받지 않는다. 범위 밖 슬롯·중복 슬롯·미등록 ID·부적격 class를 거절한다. 일반 Augment 교체는 해당 슬롯만 바꾸며, 일반 제작은 장착물을 보존한다. 제작 state는 실제 게임 화폐를 소비하지 않는다.

Augment 효과는 카드의 별도 행으로 표시하며 접두·접미 한도에 넣지 않는다. **표시하는 값은 일반 기본 소켓 효과다.** 캐릭터 전체 능력치, 전투/DPS, 최종 Armour 합계, 소켓 효과 증가·부위 대체·Sanctification 배율, Bonded 효과는 계산하지 않는다. 장착물이 있으면 기존 방어도 합계 투영도 사용하지 않는다. 조건·미해석 관측·Desecrated 슬롯이 있는 아이템에 새 Augment를 장착하는 경로는 이번 범위에서 거절한다. 장착물이 있는 상태는 자동 탐색의 기존 strict 입력 경로에서 수동 Workbench 대상으로 거절하며, 이전 입력의 생략된 장착 목록은 빈 목록으로 읽는다.

Idol, Abyssal Eye, Congealed Mist, 특수 Rune, Socket-bound, Ancient Augment의 캐릭터 전체 제한, 소켓 변환·추출·Runeforging은 미지원이다. 기본 Rune과 Soul Core의 요구 캐릭터 레벨을 아이템 레벨 자격으로 추측하지 않는다.

## 보존과 출처

누락된 구버전 소켓 수는 계속 미상이며 자동으로 0이 되지 않는다. 새로 놓은 지원 장비만 명시적 0으로 초기화한다. Clipboard의 소켓 수·미해석 Augment 효과는 `observations`에 별도 보존하며 장착 ID로 추정하지 않는다. 새 catalog identity가 저장된 과거 기록을 재라벨링하지 않는다. 기존 기록 열람·복원·단계 이동 경계를 유지한다.

새로 수집한 원문 HTML과 기존 i18n 원문의 파일별 SHA-256은 `docs/evidence/0.5.5/sockets/sources.json`에 연결한다. `scripts/prepare-socket-catalog.py`가 장비별 일반 효과를 추출한다. 자료 수집일은 2026-11-05이며 경로의 0.5.5는 프로젝트 근거 묶음이다. 모든 페이지가 같은 시점의 게임 규칙이라는 보장은 없다.

이미지는 `frontend/public/assets/augments/sources.json`에 원본 페이지·이미지 URL·SHA-256을 기록한다. 원저작권은 Grinding Gear Games 등 각 권리자에게 있다. PoE2DB의 위키 본문 고지는 [CC BY-NC-SA 3.0](https://creativecommons.org/licenses/by-nc-sa/3.0/)이며, 출처 표기가 게임 이미지의 별도 재배포 허가를 의미하지 않는다. 수집하지 못한 아이콘은 프로젝트가 작성한 공통 SVG를 사용한다.

소켓은 중앙 아이템 칸 왼쪽에 세로로 배치한다. 사용자 제공 스크린샷을 built-in imagegen으로 추출·정제한 `socket-frame.png`는 금속 테두리와 검은 내부를 포함하며 바깥만 투명하다. 생성 지시·출처·자산 해시는 같은 폴더의 `socket-frame.source.json`에 기록한다. 장착 아이콘은 검은 내부 위에 표시하며 기존 소켓 수·교체·키보드 이름·focus 복귀는 유지한다.
