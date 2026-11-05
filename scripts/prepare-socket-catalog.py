"""Extract reviewed ordinary socket effects. Never read Bonded rows as ordinary effects."""
import hashlib
import html
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / 'docs/evidence/0.5.5/sockets'
LOCALES = {'en': 'us', 'ko': 'kr', 'zh-CN': 'cn', 'zh-TW': 'tw', 'ja': 'jp', 'es': 'sp'}

def clean(value):
    return html.unescape(re.sub('<[^>]*>', '', value.replace('<br>', '\n').replace('<br />', '\n'))).strip()

sources = {}
entries = {}
groups = {'Gloves': 'Gloves_str', 'Boots': 'Boots_str', 'Helmets': 'Helmets_str', 'Body Armours': 'Body_Armours_str', 'Wands': 'Wands', 'Bows': 'Bows'}
old = ROOT / 'docs/evidence/0.5.5/i18n/sources'
for locale, code in LOCALES.items():
    paths = [(group, EVIDENCE / (slug + '.' + code + '.html')) for group, slug in groups.items()]
    paths += [(group, old / locale.lower() / (slug + '.' + locale.lower() + '.html')) for group, slug in {
        'Crossbows': 'crossbows-crossbows', 'One Hand Maces': 'maces-one-hand-maces',
        'Two Hand Maces': 'maces-two-hand-maces', 'Quarterstaves': 'quarterstaves-spears-quarterstaves',
        'Spears': 'quarterstaves-spears-spears', 'Wands': 'wands-wands', 'Staves': 'staves-talismans-staves',
        'Talismans': 'staves-talismans-talismans', 'Shields': 'offhands-shields-str', 'Bucklers': 'offhands-bucklers',
    }.items()]
    for group, path in paths:
        if not path.exists():
            continue
        raw = path.read_bytes()
        text = raw.decode('utf-8')
        if 'new ModsView(' not in text:
            continue
        data = json.JSONDecoder().raw_decode(text.split('new ModsView(', 1)[1])[0]
        source = str(path.relative_to(ROOT)).replace('\\', '/')
        sources[source] = hashlib.sha256(raw).hexdigest()
        for row in data.get('socketable', []):
            link = re.search(r'href="([^"]+)"[^>]*>(.*?)</a>', row['Name'])
            if not link:
                continue
            slug, name = link.groups()
            if not (re.fullmatch(r'(?:(?:Lesser|Greater|Perfect)_)?(?:Desert|Glacial|Storm|Iron|Body|Mind|Rebirth|Inspiration|Stone|Vision|Adept|Charging|Resolve|Robust|Tempered|Ward)_Rune', slug) or slug.startswith('Soul_Core_of_')):
                continue
            effect = clean(row['str'])
            if any(word in effect.lower() for word in ('socket', 'modifier', 'corrupt', 'limit', 'bonded')):
                continue
            entry = entries.setdefault(slug, {'id': slug, 'names': {}, 'effects': {}, 'sources': {}, 'image': '/assets/augments/rune.svg'})
            entry['names'][locale] = clean(name)
            entry['effects'].setdefault(group, {})[locale] = effect
            entry['sources'].setdefault(group, {})[locale] = source

catalog = list(entries.values())
icon_sources = ROOT / 'frontend/public/assets/augments/sources.json'
if icon_sources.exists():
    icons = json.loads(icon_sources.read_text(encoding='utf-8'))
    for entry in catalog:
        if icons.get(entry['id']):
            entry['image'] = '/assets/augments/' + entry['id'] + '.webp'
for dest in ('backend/src/main/resources/catalog/augments.json', 'frontend/src/features/crafting/augments.json'):
    (ROOT / dest).write_bytes((json.dumps(catalog, ensure_ascii=False, indent=2) + '\n').encode('utf-8'))
(EVIDENCE / 'sources.json').write_text(json.dumps({'sources': sources, 'policy': 'ordinary socketable rows only; no Bonded, special Runes or combat total evaluation'}, indent=2) + '\n', encoding='utf-8')
print('Registered', len(catalog), 'ordinary Rune/Soul Core entries')
