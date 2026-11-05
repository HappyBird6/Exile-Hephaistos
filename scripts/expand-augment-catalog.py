"""Build ordinary Augments from the exhaustive six-language source inventory.

Special state-changing operations are explicitly deferred, never keyword-filtered.
The original 79 entries remain byte-for-value compatible for saved item states.
"""
import copy
import hashlib
import json
import re
import subprocess
from pathlib import Path
from augment_source import cards

ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / 'docs/evidence/0.5.5/augment-expansion-20261105'
LOCALES = {'en': 'us', 'ko': 'kr', 'zh-CN': 'cn', 'zh-TW': 'tw', 'ja': 'jp', 'es': 'sp'}
MARTIAL = ['Bows', 'Crossbows', 'One Hand Maces', 'Two Hand Maces', 'Quarterstaves', 'Spears', 'Talismans']
CASTER = ['Wands', 'Staves']
ARMOUR = ['Gloves', 'Boots', 'Helmets', 'Body Armours', 'Shields', 'Bucklers']
CLASSES = MARTIAL + CASTER + ARMOUR
TARGETS = {name: [name] for name in CLASSES + ['Sceptres', 'Foci']}
TARGETS.update({
    'Martial Weapon': MARTIAL, 'Wand or Staff': CASTER, 'Caster Weapon': CASTER + ['Sceptres'],
    'Armour': ARMOUR, 'Weapon': MARTIAL + CASTER + ['Sceptres'],
    'Martial Weapon, Wand or Staff': MARTIAL + CASTER,
    'All Equipment': CLASSES + ['Sceptres', 'Foci'],
    'Shields and Bucklers': ['Shields', 'Bucklers'],
    'Crossbow, Bow or Spear': ['Crossbows', 'Bows', 'Spears'],
    'One Hand Mace or Quarterstaff': ['One Hand Maces', 'Quarterstaves'],
    'One Hand Mace, Two Hand Mace or Talisman': ['One Hand Maces', 'Two Hand Maces', 'Talismans'],
    'Quarterstaff or Spear': ['Quarterstaves', 'Spears'],
})

# Each decision identifies an actual operation requiring its own implementation.
DEFERRED = {
    'Masterwork_Rune': 'Socketed Rune upgrade; exact tier mapping and transformation are deferred.',
    'Fox_Idol': 'Enables Bonded effects on socketed Idols; Bonded activation is deferred.',
    'Serles_Triumph': 'Changes suffix and total affix limits; affix-limit engine changes are deferred.',
    'Cadigans_Epiphany': 'Destroys Augment sockets and creates a Jewel socket; socket conversion is deferred.',
    'Astrids_Creativity': 'Changes the crafted-modifier limit; crafted-modifier rules are deferred.',
    'Aldurs_Legacy': 'Destroys a Unique item to create a Rune; Unique recipe mapping and conversion are deferred.',
    'Legacy_of_Serles_Grit': 'Changes maximum equipment quality to 40%; quality-limit behavior is deferred.',
    'Legacy_of_Runeseekers_Call': 'Scales other socketed Runes; effect scaling and rounding are deferred.',
    'Raven-Touched_Shard': 'Socket-bound Raven-Touched effect; bound replacement behavior and the special effect are deferred.',
}
for identity in ['Passion_of_Aldur', 'Breath_of_Aldur', 'Ire_of_Aldur', 'Betrayal_of_Aldur']:
    DEFERRED[identity] = 'Transforms elemental modifiers; verified equivalent modifier mapping and transformation are deferred.'
for identity in ['Atziris_Soul_Core_of_Devotion', 'Atziris_Soul_Core_of_Vitality', 'Atziris_Soul_Core_of_Alacrity', 'Atziris_Soul_Core_of_Inoculation']:
    DEFERRED[identity] = 'Guarantees a corruption change and is Socket-bound; corruption outcomes and bound replacement rules are deferred.'
for identity, pool in {'Uhtreds_Sidereus': 'Chronomancy', 'Kolrs_Hunt': 'Marksman', 'Voranas_Carnage': 'Berserking', 'Thruds_Might': 'Destruction', 'Medveds_Tending': 'Soul', 'Katlas_Gloom': 'Decay'}.items():
    DEFERRED[identity] = f'Unlocks the {pool} modifier pool and is Socket-bound; pool mapping, rolling and bound replacement rules are deferred.'

REASONS_KO = {
    'Masterwork_Rune': '장착 Rune 업그레이드 보류. 정확한 등급 전환 대응도 추가 검증 필요.',
    'Fox_Idol': '같이 장착한 Idol의 Bonded 활성화 보류.',
    'Serles_Triumph': '접미어·전체 접사 한도 변경 보류.',
    'Cadigans_Epiphany': 'Augment 홈 파괴·Jewel 홈 생성 보류.',
    'Astrids_Creativity': 'Crafted Modifier 한도 변경 보류.',
    'Aldurs_Legacy': 'Unique 파괴·결과 Rune 생성 보류. Unique별 대응표도 필요.',
    'Legacy_of_Serles_Grit': '최대 Quality 40%로 변경하는 동작 보류.',
    'Legacy_of_Runeseekers_Call': '다른 Rune 효과 증폭 보류. 적용 대상·반올림도 추가 검증 필요.',
    'Raven-Touched_Shard': 'Raven-Touched 특수 효과·Socket-bound 교체 금지 보류.',
}
for identity in ['Passion_of_Aldur', 'Breath_of_Aldur', 'Ire_of_Aldur', 'Betrayal_of_Aldur']:
    REASONS_KO[identity] = '원소 접사 변환·Socket-bound 교체 금지 보류. 동등 Modifier 대응표도 필요.'
for identity in ['Atziris_Soul_Core_of_Devotion', 'Atziris_Soul_Core_of_Vitality', 'Atziris_Soul_Core_of_Alacrity', 'Atziris_Soul_Core_of_Inoculation']:
    REASONS_KO[identity] = '타락 시 반드시 변화하는 규칙·Socket-bound 교체 금지 보류.'
for identity, pool in {'Uhtreds_Sidereus': 'Chronomancy', 'Kolrs_Hunt': 'Marksman', 'Voranas_Carnage': 'Berserking', 'Thruds_Might': 'Destruction', 'Medveds_Tending': 'Soul', 'Katlas_Gloom': 'Decay'}.items():
    REASONS_KO[identity] = f'{pool} 접사 풀 해금·추첨·Socket-bound 교체 금지 보류.'


def write(path, value):
    path.write_bytes((json.dumps(value, ensure_ascii=False, indent=2) + '\n').encode('utf-8'))


def effect_text(line):
    # Source uses ASCII or full-width colon depending on locale.
    parts = re.split(r'[:：]', line, maxsplit=1)
    if len(parts) != 2 or not parts[1].strip():
        raise ValueError('Missing source target/effect: ' + line)
    return parts[1].strip()


def build():
    source = {locale: cards(EVIDENCE / f'Augment.{code}.html') for locale, code in LOCALES.items()}
    english = source['en']
    assert len(english) == 313
    for locale, entries in source.items():
        assert set(entries) == set(english), locale
        localized_targets = {}
        for identity, card in entries.items():
            assert len(card['ordinary']) == len(english[identity]['ordinary']), (locale, identity)
            for index, line in enumerate(card['ordinary']):
                target = english[identity]['ordinary'][index].split(':', 1)[0]
                localized_target = re.split(r'[:：]', line, maxsplit=1)[0]
                assert localized_targets.setdefault(target, localized_target) == localized_target, (locale, identity, target)
    kinds = {}
    for kind in ['Rune', 'Idol', 'Soul_Core', 'Abyssal_Eye', 'Congealed_Mist']:
        for identity in cards(EVIDENCE / f'{kind}.us.html'):
            assert identity not in kinds, identity
            kinds[identity] = kind
    assert set(kinds) == set(english), 'Collection pages must exactly partition the Augment inventory'
    legacy = {entry['id']: entry for entry in json.loads((EVIDENCE / 'legacy-catalog.json').read_text(encoding='utf-8'))}
    assert len(legacy) == 79 and set(legacy) <= set(english)
    icons = json.loads((ROOT / 'frontend/public/assets/augments/sources.json').read_text(encoding='utf-8'))
    catalog, audit = [], []
    for identity, card in english.items():
        detail = {
            'id': identity, 'name': card['name'], 'kind': kinds[identity],
            'sourceUrl': 'https://poe2db.tw/us/' + identity,
            'ordinary': card['ordinary'], 'properties': card['properties'],
            'socketBound': card['socketBound'],
        }
        if identity.startswith('DNT-Unused_'):
            assert not card['ordinary']
            detail.update(status='UNUSED', reason='Source explicitly marks DNT-Unused and supplies no ordinary effect.')
            audit.append(detail)
            continue
        if identity in DEFERRED:
            detail.update(status='DEFERRED_SPECIAL', reason=DEFERRED[identity])
            audit.append(detail)
            continue
        assert not card['socketBound'], 'Unreviewed Socket-bound operation: ' + identity
        effects, unsupported_targets = {}, []
        for index, line in enumerate(card['ordinary']):
            target = line.split(':', 1)[0]
            assert target in TARGETS, target
            for item_class in TARGETS[target]:
                if item_class not in CLASSES:
                    unsupported_targets.append(item_class)
                    continue
                assert item_class not in effects, (identity, item_class)
                effects[item_class] = {locale: effect_text(entries[identity]['ordinary'][index]) for locale, entries in source.items()}
        if not effects:
            detail.update(status='UNSUPPORTED_TARGET', reason='Only Sceptres/Foci have ordinary effects; socket eligibility/capacity for those classes is not reviewed.')
            audit.append(detail)
            continue
        entry = copy.deepcopy(legacy.get(identity)) if identity in legacy else {
            'id': identity, 'names': {locale: entries[identity]['name'] for locale, entries in source.items()},
            'effects': effects,
            'sources': {item_class: {locale: f'docs/evidence/0.5.5/augment-expansion-20261105/Augment.{code}.html' for locale, code in LOCALES.items()} for item_class in effects},
            'image': '/assets/augments/' + identity.replace('%', '_') + '.webp' if icons.get(identity) else '/assets/augments/rune.svg',
        }
        entry['kind'] = kinds[identity]
        if icons.get(identity):
            entry['image'] = '/assets/augments/' + identity.replace('%', '_') + '.webp'
        entry['properties'] = {locale: entries[identity]['properties'] for locale, entries in source.items()}
        limit = next((re.fullmatch(r'Limited to: (\d+)(?: (.+))?', text) for text in card['properties'] if text.startswith('Limited to:')), None)
        if limit:
            entry['limit'] = {'count': int(limit[1]), 'group': limit[2] or identity}
            if identity in legacy:
                entry['legacyLimitExempt'] = True
        entry['sourceUrl'] = detail['sourceUrl']
        catalog.append(entry)
        detail.update(status='SUPPORTED_LEGACY' if identity in legacy else 'ADDED_ORDINARY', reason='Ordinary base socket effects; combat/character totals and Bonded effects are not evaluated.', supportedClasses=list(entry['effects']))
        if unsupported_targets:
            detail['unsupportedTargets'] = sorted(set(unsupported_targets))
        if entry.get('limit'):
            detail['limit'] = entry['limit']
            detail['limitScope'] = 'Current item only; no character equipment/loadout state exists.'
        audit.append(detail)
    perfect = {entry['id'].removeprefix('Perfect_') for entry in catalog if entry['id'].startswith('Perfect_')}
    for entry in catalog:
        family = re.sub(r'^(Lesser_|Greater_|Perfect_)', '', entry['id'])
        entry['selectionEnabled'] = family not in perfect or entry['id'].startswith('Perfect_')
    # Preserve original ordering as well as original effect/name/source data.
    order = {identity: index for index, identity in enumerate(legacy)}
    catalog.sort(key=lambda entry: (0, order[entry['id']]) if entry['id'] in order else (1, entry['id']))
    for destination in ['backend/src/main/resources/catalog/augments.json', 'frontend/src/features/crafting/augments.json']:
        write(ROOT / destination, catalog)
    tooltips_path = ROOT / 'frontend/src/features/crafting/materialTooltips.json'
    terms_path = ROOT / 'frontend/src/shared/i18n/gameTerms.json'
    tooltips = json.loads(tooltips_path.read_text(encoding='utf-8'))
    terms = json.loads(terms_path.read_text(encoding='utf-8'))
    for entry in catalog:
        identity = entry['id']
        if identity in legacy:
            continue
        tooltips[identity] = {'name': entry['names']['en'], 'lines': english[identity]['ordinary'], 'sourceUrl': entry['sourceUrl']}
        for locale, code in LOCALES.items():
            terms[locale][identity] = {'name': entry['names'][locale], 'lines': source[locale][identity]['ordinary'], 'sourceUrl': f'https://poe2db.tw/{code}/{identity}', 'itemKey': identity}
    write(tooltips_path, tooltips)
    write(terms_path, terms)
    # Use the repository's existing formatter for both sealed mirrors and shared i18n.
    subprocess.run(['node', str(ROOT / 'frontend/node_modules/prettier/bin/prettier.cjs'), '--write',
        str(ROOT / 'backend/src/main/resources/catalog/augments.json'),
        str(ROOT / 'frontend/src/features/crafting/augments.json'),
        str(tooltips_path), str(terms_path)], check=True, stdout=subprocess.DEVNULL)
    write(EVIDENCE / 'audit.json', {'sourceCount': len(english), 'registeredCount': len(catalog), 'selectableCount': sum(entry['selectionEnabled'] for entry in catalog), 'specialOperations': 'Deferred by user instruction; ordinary effects and necessary installation limits only.', 'entries': audit})
    lines = ['# Augment 전수 대조', '', '2026-11-05 수집. [PoE2DB 원문](https://poe2db.tw/kr/Augment) 313종을 모두 분류했다.', '',
        '일반 효과 198종 추가(Rune 129·Idol 33·Soul Core 32·Abyssal Eye 4), 기존 79종 보존으로 총 277종이다. 신규 선택은 232종이며 기존 Perfect 정책으로 45종을 숨긴다.', '',
        '특수 동작 23종은 사용자 지시로 보류한다. Sceptres/Foci 전용 5종은 장착 자격·홈 상한 미검증, DNT-Unused 8종은 원문에 일반 효과가 없다.', '',
        '표시하는 것은 기본 소켓 효과다. 캐릭터 능력치·전투 결과·Bonded는 계산하지 않는다. 같은 아이템의 중복/공유 그룹 제한만 검사하며 전체 캐릭터 장비 제한은 확인하지 않는다. 기존 제한 누락 상태를 보존하기 위해 원래 79종 중 제한이 확인된 Soul Core 9종은 상태 읽기에서 기존 중복을 허용하지만 새 장착은 제한한다.', '',
        '6개 언어 원문·원본 효과·장비별 자격·제한은 [audit.json](../evidence/0.5.5/augment-expansion-20261105/audit.json)과 두 실행 카탈로그에 연결한다. 원문 HTML 및 이미지의 SHA-256을 별도 보존한다.', '']
    for status, title in [('ADDED_ORDINARY', '추가한 일반 증강물'), ('SUPPORTED_LEGACY', '보존한 기존 증강물'), ('DEFERRED_SPECIAL', '보류한 특수 동작'), ('UNSUPPORTED_TARGET', '지원 베이스가 없는 항목'), ('UNUSED', '명시적 미사용 항목')]:
        rows = [row for row in audit if row['status'] == status]
        lines.extend([f'## {title} ({len(rows)})', '', '| ID | 한국어 원문 이름 | 지원 class 또는 사유 |', '|---|---|---|'])
        for row in rows:
            reason = ', '.join(row.get('supportedClasses', []))
            if status == 'DEFERRED_SPECIAL':
                reason = REASONS_KO[row['id']]
            elif status == 'UNSUPPORTED_TARGET':
                reason = '일반 효과가 Sceptres/Foci에만 있음. 해당 class의 홈 자격·상한 미검증.'
            elif status == 'UNUSED':
                reason = 'DNT-Unused 명시, 일반 효과 없음.'
            lines.append(f"| `{row['id']}` | {source['ko'][row['id']]['name']} | {reason} |")
        lines.append('')
    (ROOT / 'docs/current/augment-catalog-audit.md').write_bytes(('\n'.join(lines)).encode('utf-8'))
    checksums = {path.name: hashlib.sha256(path.read_bytes()).hexdigest() for path in sorted(EVIDENCE.glob('*.html'))}
    write(EVIDENCE / 'inventory-sources.json', {'retrievedAt': '2026-11-05', 'sha256': checksums})
    print('Registered', len(catalog), 'selectable', sum(entry['selectionEnabled'] for entry in catalog))
    print('Audit:', {status: sum(row['status'] == status for row in audit) for status in sorted({row['status'] for row in audit})})


if __name__ == '__main__':
    build()
