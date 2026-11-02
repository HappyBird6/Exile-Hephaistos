# B: 심연 작업대 연결

기준 커밋: `6040357faf4e39cc989991d44ef8d04c7119dbd0`.
공통 파일은 수정하지 않았다. 전용 기존 파일과 `abyss-workbench/`, `public/assets/abyss-runes/`만 변경했다.

## 신규 화폐 선택 목록 (A / 통합 담당)

`policy.ts`의 `excludedWorkbenchBoneIds`는 정확히 다음 세 ID다.

- `Gnawed_Rib`: 갉힌 갈비뼈
- `Gnawed_Jawbone`: 갉힌 턱뼈
- `Gnawed_Collarbone`: 갉힌 빗장뼈

```ts
import { isExcludedWorkbenchBone } from './abyss-workbench/policy'

const availableMaterials = materials.filter(
  (material) => !isExcludedWorkbenchBone(material.id),
)
```

신규 창고·검색·선택 목록에만 적용한다. `boneNames`, `isBone`, 저장 데이터와 서버의 과거 화폐 식별자는 그대로 유지한다. 과거 즐겨찾기/세션에서 보이는 이름·메모·원문을 지우거나 ID를 바꾸지 않는다. 기존 저장 레코드를 불러온 것만으로 신규 화폐를 소모하지 않는다. 이 브랜치는 목록 소유 파일을 수정하지 않으므로 목록에서 실제로 숨기는 연결은 통합 담당이 해야 한다.

과거 즐겨찾기에 제외 ID가 남아 있어도 저장 슬롯을 자동 삭제하지 않는다. 그 슬롯에서 신규 화폐 선택/사용으로 진입하는 동작에는 `isExcludedWorkbenchBone(id)` 가드를 적용하고, 사용자가 다른 재료로 교체할 수 있게 한다. 이 정책은 과거 제작 기록의 열람/복원을 거부하는 서버 검증으로 사용하지 않는다.

## 뼈 사용과 드러내기

- 기존 `applyBone(...)`과 `/abyss/desecrate` 계약을 유지했다. 화폐를 사용하면 `modifier: null`, `offer: null`인 미공개 슬롯으로 기록한다. 별도 `/select` 호출은 사용자가 드러내기 후 옵션을 적용할 때만 보낸다.
- `AbyssPanel`의 별도 시작 버튼과 `/add`를 호출하는 무료 슬롯 추가 UI를 제거했다. 기존 `allowAdd` prop은 호출부 타입 호환용으로만 남아 있다.
- 기존 호출부의 `directSelection` 모드는 ‘드러내기’를 누르기 전 후보 입력과 `/eligible` 조회를 하지 않는다. 닫힌 상태에는 `/plan` 요청도 하지 않는다. 사용자는 미공개 상태로 다른 작업/저장/복원을 할 수 있다.
- `directSelection`에서는 다음 화폐 선택/확률 비교 UI도 표시하지 않는다. 기존 제작지원의 prepared route 및 실제 관측 후보 3개 기록은 유지한다.
- 접두·접미, 가득 찬 아이템의 제거 대상, Putrefaction의 실제 관측 슬롯 개수는 기존 관측 입력 계약을 유지한다. 임의 확률이나 가짜 접두·접미를 만들지 않았다. 이 입력까지 나중으로 미루려면 공통 ItemState/저장/API에 별도 ‘종류 미정’ 모델이 필요하므로 현재 계약에서 임의의 PREFIX/SUFFIX를 넣으면 안 된다.

현행 CraftingPage의 `AbyssPanel` 호출은 그대로 호환된다. 패널은 뼈로 만든 슬롯 또는 저장/준비 상태가 있을 때만 렌더링한다.

## 중앙 ItemCard 룬 표시 (D / 통합 담당)

사용자 이미지 수신·실제 픽셀 조회 성공. 원본 이미지를 `frontend/public/assets/abyss-runes/user-reference-20261102.png`로 보존했고, CSS에서 초록 룬 영역만 표시한다. 신규 이미지 첨부는 더 필요 없다.

```tsx
import { UnrevealedRune } from './abyss-workbench/UnrevealedRune'

// ItemCard의 해당 미공개 줄 본문 렌더링 위치
{
  line.unrevealedDesecrated ? <UnrevealedRune /> : line.text
}
```

`line.unrevealedDesecrated?: boolean`은 D 소유 `ItemCardLine`에 추가할 연결 제안이다. 이미 동등한 플래그를 만들었다면 그것을 사용한다. 텍스트 일치나 `kind === 'desecrated'`만으로 판단하면 공개된 실제 훼손 옵션까지 룬으로 덮으므로 사용하지 않는다.

통합 담당은 중앙 카드 projection에서 `concrete.desecrated.filter(slot => slot.modifierId === null)`의 각 슬롯에 다음 줄을 추가한다.

```ts
{
  id: slot.id,
  kind: 'desecrated' as const,
  text: abyssWorkbenchMessages[locale].hidden,
  affixLabel: slot.side === 'PREFIX' ? 'P' : 'S',
  unrevealedDesecrated: true,
}
```

`abyssWorkbenchMessages`는 `./abyss-workbench/messages`에서 import한다. 카드 줄의 텍스트는 접근성/기록 용도로 유지하고 실제 화면은 룬 컴포넌트로 대체한다. 룬 컴포넌트 자체도 6개 언어의 접근성 이름을 갖는다. 패널 비임베드 슬롯에는 이미 직접 연결했다. embedded 중앙 카드는 ItemCard 소유권 때문에 통합 연결이 필요하다.

기존 제작지원 `method-search/craftingFlow.ts`의 `abyssCard`도 미공개 슬롯을 알기 때문에 위 플래그를 해당 줄에만 붙일 수 있다. 이 파일은 B 배정 파일이 아니어서 수정하지 않았다. 별도 플래그 없이 모든 기존 저장 원문을 변경하는 방식은 사용하지 않는다.

## 통합 검증

**백엔드 공통 seal 차단:** 기준 `6040357`의 `catalog/manual-reveal.json`은 LF SHA `f1e3af497c5527269607c481bd1947c7741f0c74b8cb6d8f2edff54f96b8cdf9`인데 `crafting/ruleset-v1.json`은 원본 CRLF SHA `61dc6fb49236efe8d8a5f7b0537d59b1bef13f337cd3ac68848388ec5308f844`를 참조한다. JSON 객체는 동일하다. 이 때문에 심연 백엔드 테스트 23개가 초기화 단계에서 실패했다. 공통 JSON/manifest를 B에서 수정하거나 검증을 우회하지 않았다. 통합 담당이 실제 소비 바이트/manifest를 정합화한 후 재검증해야 한다. 자세한 결과는 [VERIFICATION.md](./VERIFICATION.md)에 있다.

- 목록에서 갉힌 3종은 선택할 수 없고 보존된 과거 세션은 읽힌다.
- 뼈 사용 직후 슬롯은 미공개이고 `/select`, `/eligible`, `/add`를 자동 호출하지 않는다.
- ‘드러내기’ 후에만 적격 옵션 직접 선택이 열리고, 완료 후에도 동일 slot ID/제약이 보존된다.
- 중앙 카드와 제작지원 카드에서 미공개만 초록 룬으로 표시되고 공개된 훼손 옵션은 실제 텍스트로 표시된다.
- Windows Vitest는 `TEMP`/`TMP`를 사용자 Temp 아래의 `abyss-b-tests` 같은 공통 절대 경로로 설정해야 sandbox 임시 IPC 파일 ENOENT를 피할 수 있다. 테스트 기대값/설정을 완화하지 않았다.
- 브라우저 검증 및 서버 변경은 위임 조건대로 수행하지 않는다.
