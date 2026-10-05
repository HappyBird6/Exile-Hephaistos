const fs = require('node:fs'), { chromium } = require('/qa/node_modules/playwright')
;(async () => {
  const browser = await chromium.launch({ headless: true })
  const page = await browser.newPage({ viewport: { width: 1260, height: 1000 } })
  for (const [folder, label] of [['/evidence/cards', 'cards'], ['/evidence', 'full']]) {
    const files = fs.readdirSync(folder).filter(p => p.endsWith('.png') && !p.startsWith('contact-') && !p.startsWith('browser-failure'))
    for (let start = 0; start < files.length; start += 6) {
      const path = `/evidence/contact-${label}-${start}.png`
      if (fs.existsSync(path)) throw new Error('Preserve prior contact sheets')
      const cells = files.slice(start, start + 6).map(p => `<section><h2>${p}</h2><img src="data:image/png;base64,${fs.readFileSync(folder + '/' + p).toString('base64')}"></section>`).join('')
      await page.setContent('<style>body{margin:0;background:#222;color:white;font:16px sans-serif;display:grid;grid-template-columns:repeat(3,420px)}section{padding:8px}img{max-width:404px}h2{font-size:14px}</style>' + cells)
      await page.screenshot({ path, fullPage: true })
    }
  }
  await browser.close()
})().catch(e => { console.error(e); process.exitCode = 1 })
