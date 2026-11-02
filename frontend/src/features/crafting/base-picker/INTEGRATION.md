# 담당 C: 베이스 선택 연결

기준: `6040357faf4e39cc989991d44ef8d04c7119dbd0`. 공통 파일은 수정하지 않았다. `WorkbenchBasePicker.tsx`와 `../session-library/SessionLibrary.tsx`를 통합 담당이 `CraftingPage.tsx`에 연결한다. 기존 작업대/지원/탐색 탭 구조는 유지한다.

## 메뉴 상태와 위치

`WorkbenchBasePicker`는 `open`, `onOpenChange`, `value`, `onChange`, `level`, `onLevelChange`, `onPlace`가 모두 부모 제어형이다. 공통 `LeftShelfOverlay`는 body portal의 nonmodal `role="dialog"`로 왼쪽에 고정 배치된다. backdrop과 레이아웃 공간을 추가하지 않는다. 기본 폭은 최대 520px이고 창 크기에 맞춰 줄어든다. 부모가 `shelf` ref를 넘기면 화폐 창고의 세로 범위 안에서 최대 560px까지만 덮는다. 내용은 내부 스크롤한다. 바깥 클릭/닫기/Escape로 닫히며 트리거로 포커스가 돌아간다. 툴팁에 포커스가 있을 때 첫 Escape는 툴팁만 닫는다.

```tsx
import { WorkbenchBasePicker } from './base-picker/WorkbenchBasePicker'

const [leftMenu, setLeftMenu] = useState<'base' | 'history' | null>(null)
const baseAnchor = useRef<HTMLButtonElement>(null)
const historyAnchor = useRef<HTMLButtonElement>(null)
const currencyShelf = useRef<HTMLDivElement>(null)

// 기존 베이스 선택 진입 버튼. 아이템 텍스트 붙여넣기 경로는 유지한다.
<button
  ref={baseAnchor}
  type="button"
  aria-expanded={leftMenu === 'base'}
  aria-controls="workbench-base-picker"
  onClick={() => setLeftMenu(leftMenu === 'base' ? null : 'base')}
>{t('ui.select_base')}</button>

<WorkbenchBasePicker
  id="workbench-base-picker"
  anchor={baseAnchor}
  shelf={currencyShelf}
  open={leftMenu === 'base'}
  onOpenChange={(open) => setLeftMenu(open ? 'base' : null)}
  value={baseChoice}
  onChange={setBaseChoice}
  level={baseLevel}
  onLevelChange={setBaseLevel}
  disabled={applying !== null}
  valid={jewelControlsValid}
  extraControls={jewelControls}
  onPlace={() => {
    imported.invalidate()
    void startBase(
      Number(baseLevel),
      baseChoice,
      null,
      sapphireRarity,
      baseChoice === 'sapphire' && sapphireAffix && sapphireRarity !== 'NORMAL'
        ? Number(sapphireRoll)
        : null,
    )
    setPreviewRequest({ count: 0, action: null })
    setSelected(null)
    setLeftMenu(null)
  }}
/>
```

`currencyShelf`는 실제 화폐 창고 컨테이너에 연결한다. `anchor`는 오른쪽 아이템/설정 영역에서 메뉴를 여는 버튼이다. 가능한 경우 베이스와 History에 같은 `leftMenu` 상태를 사용해 두 메뉴가 동시에 열리지 않게 한다. 기존 베이스용 native `<dialog>` 부분을 제거하고 아이템 텍스트 입력 UI만 기존 경로에 남긴다. 모달 내부의 버튼을 anchor로 사용하면 modal top layer가 portal을 가리므로, 베이스 진입 시 먼저 기존 dialog를 닫고 화면에 남는 버튼을 anchor로 사용한다.

`jewelControls`는 기존 주얼 희귀도와 관측된 Sapphire 접미어/roll 입력만 옮긴 ReactNode이다. `jewelControlsValid`는 기존 Sapphire roll의 2~4 정수 조건만 유지한다. 레벨 1~100 검증과 76px 입력 너비는 컴포넌트에 포함된다. 별도 설정이 없으면 `valid`는 true이다.

**기폭제 설정을 베이스 메뉴에서 제거:** `qualityType`, `qualityAmount` 입력/검증을 이 메뉴에 넘기지 않고 새 베이스 생성의 세 번째 인자는 항상 `null`로 전달한다. 기폭제 화폐 사용 연결은 통합 담당/해당 담당이 처리한다. 이미 저장되거나 붙여넣은 아이템의 `catalystQuality`를 제거하지 않는다. `startBase`의 기존 시그니처나 기록을 변경할 필요가 없다.

카테고리는 무기/방어구/장신구/주얼, 소분류는 기존 registry의 장비 Class이다. `selectableBaseKeys`를 사용하므로 과거 stocky/bow/body/helmet 선택 제외 정책을 유지하며 기록의 과거 identity는 수정하지 않는다. hover와 키보드 focus 시 기존 `ItemCard`를 읽기 전용 툴팁으로 사용한다. 검증된 sourceProperties/requirements/implicit 템플릿의 범위만 보여주고 새 roll을 생성하지 않는다.

## 이미지

별도 `baseImageSources.json`과 `images/`는 **이 기능 전용**이다. 공통 카탈로그·manifest는 변경하지 않았다. 134개 기존/현재 베이스 identity를 출처와 연결했고 127개 고유 WEBP를 로컬 캐시했다. Solar Amulet은 기존 로컬 이미지를 재사용한다. 세션과 베이스 메뉴가 같은 `BaseImage`를 사용한다. 자산 실패 시 작은 접근성 이름이 있는 대체 표시를 사용하며 깨진 이미지 아이콘을 남기지 않는다.

Fire Quiver(`Metadata/Items/Quivers/FourQuiver2`) 원본 이미지 다운로드는 HTTP 403으로 거부되어 **재시도하지 않았다**. 런타임에서도 해당 URL 요청을 반복하지 않는다. 이미지가 필요하면 이 작업 채팅 **‘베이스 선택·세션 History 메뉴’**에 Fire Quiver(불 화살통) 이미지를 첨부받거나, 승인된 로컬 이미지를 `imageForBase={(key) => ...}`로 주입한다. 정확한 key/id는 manifest와 registry를 대조한다. 다른 베이스는 추가 첨부 없이 표시된다. 저작권·출처는 `NOTICE.md`를 유지한다.

유지보수 스크립트는 앱/빌드에서 자동 실행하지 않는다. `collectBaseImageSources.mjs`는 기존 evidence와 캐시를 보존하여 manifest를 재생성하며, `--fetch-missing`일 때만 출처 페이지를 가져온다. `cacheBaseImages.mjs`는 기존 캐시와 거부 기록을 건너뛴다.
