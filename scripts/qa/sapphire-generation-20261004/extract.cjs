const fs = require('fs');
const root = 'E:/WORK/Exile-Hephaistos/codex/sapphire-generation-20261004/';
const s = fs.readFileSync(root + 'sapphire.html', 'utf8');
const start = s.indexOf('new ModsView(') + 'new ModsView('.length;
let depth = 0, quoted = false, escaped = false, end;
for (let i = start; i < s.length; i++) {
  const c = s[i];
  if (quoted) {
    if (escaped) escaped = false;
    else if (c === '\\') escaped = true;
    else if (c === '"') quoted = false;
  } else if (c === '"') quoted = true;
  else if (c === '{') depth++;
  else if (c === '}' && --depth === 0) { end = i + 1; break; }
}
const data = JSON.parse(s.slice(start, end));
fs.writeFileSync(root + 'mods.json', JSON.stringify(data, null, 2));
console.log(Object.keys(data));
console.log(JSON.stringify(data.normal).slice(0, 9000));
