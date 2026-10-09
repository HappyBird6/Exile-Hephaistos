import fs from 'node:fs'
import assert from 'node:assert/strict'
import { execFileSync } from 'node:child_process'
const q = 'E:/WORK/Exile-Hephaistos/codex/base-registry-qa-20261005'
const e = 'docs/evidence/base-registry-refactor-2026-10-05'
const names = ['app', 'frontend', 'postgres', 'redis'].map(x => `exile-base-registry-20261005-${x}-1`)
const services = names.map(name => ({ name, state: JSON.parse(execFileSync('docker', ['inspect', '--format', '{{json .State}}', name], { encoding: 'utf8' })) }))
assert(services.every(s => !s.state.Running && !s.state.OOMKilled))
for (const f of ['live-before.json', 'live-after.json', 'importer-check-3.log', 'backend-check-2.log', 'frontend-check-1.log']) fs.copyFileSync(`${q}/${f}`, `${e}/${f}`, fs.constants.COPYFILE_EXCL)
fs.copyFileSync(`${q}/browser-negative-1/negative-jewel-results.json`, `${e}/negative-jewel-results.json`, fs.constants.COPYFILE_EXCL)
for (const f of ['qa-crossclass-filled-1.cjs', 'qa-crossclass-filled-2.cjs', 'qa-negative-jewel-2.cjs']) fs.copyFileSync(`${q}/${f}`, `${e}/${f}`, fs.constants.COPYFILE_EXCL)
const owner = JSON.parse(fs.readFileSync(`${q}/heavy-qa-owner.json`))
assert(owner.active)
owner.active = false; owner.releasedAt = new Date().toISOString()
fs.writeFileSync(`${q}/heavy-qa-owner.json`, JSON.stringify(owner, null, 2) + '\n')
const result = {
  passed: true, bases: 125, added: 0, pendingBases: ['Permafrost Staff', 'Reflecting Staff', 'Dark Staff', 'Ravenous Staff', 'Perching Staff', 'Sanctified Staff', 'Maji Talisman', 'Fungal Talisman', 'Jade Talisman'],
  backendUnit: 492, backendIntegration: 6, frontend: 1917, api: [2902, 582], browser: [1088, 481, 17], importerTests: 3, importerRows: 620,
  preservedCatalogFiles: 441, testedSourceInputs: 764, qaReleasedAt: owner.releasedAt, qaServices: services,
  filmFixtureCorrection: 'Attempt 1 incorrectly fractured Basic/Time-Lost Jewels. Existing validator rejected these correctly; storage-only assertion missed visible fallback. Attempt 2 uses allowed Jewel rolls and asserts rendered explicit plus no alert for all 24 representatives in six locales. Separate 17-check negative probe asserts unsupported Fractured Jewel warning and unchanged stored films. Runtime code unchanged by fixture correction.',
  visualReview: { allScreenshotsCaptured: true, exhaustivePixelReview: false, reviewedSheets: { attempt1Cards: [0, 6, 72, 78, 144, 150], attempt2Cards: [0, 18, 72, 84, 90, 120, 126, 138] }, reviewedOriginals: ['attempt2 Sapphire en', 'attempt2 Time-Lost Sapphire ko', 'attempt2 Fortified Hammer en'], finding: 'Representative six-locale names, properties, implicit/granted skill and roll display retained. Unsupported Jewel sample repaired. Contact-sheet overflow on long headings is a QA collage issue; original Fortified Hammer card checked intact.' },
  remainingManual: ['legacy17 loader/constructor mechanics', 'Basic Jewel/Liquid validation', 'Solar clipboard/Support/Explorer', 'source roster/provenance and eligible pool collection per bundle', 'source-specific implicit/skills and six-locale templates', 'new-class policy with explicit unsupported mechanics'],
  unrun: ['Original stack Compose env config: .env absent in worktree and parent; infra configuration unchanged. Dedicated QA Compose config --quiet passed.'],
  integration: 'Local branch checkpoint only. No push, merge or deploy. Next task adds 9 bases then performs bounded 134-base final coverage audit.'
}
fs.writeFileSync(`${e}/completion.json`, JSON.stringify(result, null, 2) + '\n', { flag: 'wx' })
const doc = 'docs/base-registry-refactor-2026-10-05.md'
let text = fs.readFileSync(doc, 'utf8').replaceAll('- [ ]', '- [x]')
text += '\n## 검증 결과와 인계\n\nBackend unit492 + integration6, Frontend1917, API2902 + registry582, browser1088 + 강화 film481 + negative Jewel17, importer3 tests/620 rows가 통과했다. 기존 catalog/i18n 등441파일은 시작 HEAD와 byte-identical이고 runtime/build 입력764개를 대조했다. 정확한 등록 수는 **125**, 이번 신규 base는 **0**이다. 상세 결과와 실패 보존·샘플 수정 경위는 `docs/evidence/base-registry-refactor-2026-10-05/completion.json`에 있다.\n\nFilm 첫 샘플은 Jewel에 미지원 Fractured를 넣었으므로 화면이 거부되었다. 기존 validator가 올바르게 거부한 것으로 확인했다. 저장값 보존만 검사하던 probe를 보강해24대표 ×6locale의 실제 옵션 표시와 alert 부재를 확인했다. 별도 negative probe는 거부 화면과 원본 film 보존을 검증했다. Runtime 규칙을 완화하지 않았다. 스크린샷은 전부 생성했고 시각 검토는 명시한 대표 contact sheets와 원본에 한정한다.\n\n전용 QA4서비스만 정상 stop했고 live 서비스 ID·시작시각·mount가 동일하다. QA slot은 해제했다. 원본 stack의 `.env`가 worktree/parent에 없어 해당 Compose 검사는 실행하지 않았다. infra 변경은 없으며 전용 QA Compose config는 통과했다.\n\n다음 독립 작업은 Staves6/Talismans3의 source roster·released availability와 풀·skill/implicit·6locale 증거를 수집하여 위 recipe로 추가한다. 신규 class policy는각1곳, canonical/mirror 동기화는1script로 처리한다. 그 뒤134 IDs와 기존125 snapshot/films 보존, 신규9의 ilvl1/20/82·currency/Essence/Omen positive/negative·skill/implicit6locale를 검증하고 전체 필수 checks를1회 실행한다. 마지막 coverage audit은 선택 roster 완결, 실제 released class 누락, representative sidegrade 의도적 생략, 실제 미지원 mechanics를 구분한다. 현재 checkpoint는 Staves/Talismans 완료나 게임 전체 지원을 주장하지 않는다.\n'
fs.writeFileSync(doc, text)
console.log('125-base checkpoint recorded; QA stopped and released; live unchanged')
