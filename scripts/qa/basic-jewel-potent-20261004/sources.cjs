const fs = require('fs'), assert = require('assert/strict');
const root = '../basic-jewel-potent-20261004/';
function parse(s) {
  const marker = 'new ModsView(', pos = s.indexOf(marker);
  assert(pos >= 0); const start = pos + marker.length;
  let depth = 0, q = false, e = false;
  for (let i = start; i < s.length; i++) {
    const c = s[i];
    if (q) { if (e) e = false; else if (c === '\\') e = true; else if (c === '"') q = false; }
    else if (c === '"') q = true;
    else if (c === '{') depth++;
    else if (c === '}' && --depth === 0) return JSON.parse(s.slice(start, i + 1));
  }
  throw Error('Missing ModsView JSON');
}
module.exports = { parse, root };
if (require.main === module) (async () => {
  fs.mkdirSync(root, { recursive: true });
  for (const base of ['Ruby', 'Emerald', 'Diamond', 'Sapphire']) {
    for (const [locale, prefix] of Object.entries({ en: 'us', ko: 'kr', 'zh-CN': 'cn', 'zh-TW': 'tw', ja: 'jp', es: 'sp' })) {
      const file = root + base.toLowerCase() + '-' + locale + '.html';
      if (!fs.existsSync(file)) {
        const r = await fetch('https://poe2db.tw/' + prefix + '/' + base); assert(r.ok);
        fs.writeFileSync(file, await r.text());
      }
      const data = parse(fs.readFileSync(file, 'utf8'));
      if (locale === 'en') { fs.writeFileSync(root + base.toLowerCase() + '-mods.json', JSON.stringify(data, null, 2)); console.log(base, data.baseitem, data.normal.length, data.liquid); }
    }
  }
})().catch(e => { console.error(e); process.exit(1); });
