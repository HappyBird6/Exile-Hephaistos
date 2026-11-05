"""Source completeness, legacy compatibility and asset provenance contracts."""
import hashlib
import json
from pathlib import Path
import unittest
import sys
import re

ROOT = Path(__file__).resolve().parents[2]
EVIDENCE = ROOT / 'docs/evidence/0.5.5/augment-expansion-20261105'
LOCALES = {'en': 'us', 'ko': 'kr', 'zh-CN': 'cn', 'zh-TW': 'tw', 'ja': 'jp', 'es': 'sp'}
sys.path.insert(0, str(ROOT / 'scripts'))
from augment_source import cards


def read(path):
    return json.loads(path.read_text(encoding='utf-8'))


class AugmentCatalogTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.catalog = {entry['id']: entry for entry in read(ROOT / 'frontend/src/features/crafting/augments.json')}
        cls.audit = read(EVIDENCE / 'audit.json')

    def test_all_source_identities_have_an_explicit_decision(self):
        source = {entry['id'] for entry in read(EVIDENCE / 'cards.us.json')}
        audit = {entry['id']: entry for entry in self.audit['entries']}
        self.assertEqual(len(source), 313)
        self.assertEqual(set(audit), source)
        self.assertEqual(set(self.catalog), {identity for identity, row in audit.items() if row['status'] in ('SUPPORTED_LEGACY', 'ADDED_ORDINARY')})
        self.assertEqual(len(self.catalog), 277)
        self.assertEqual(sum(row['selectionEnabled'] for row in self.catalog.values()), 232)
        for identity in ['Cadigans_Epiphany', 'Serles_Triumph', 'Masterwork_Rune', 'Fox_Idol', 'Legacy_of_Serles_Grit']:
            self.assertEqual(audit[identity]['status'], 'DEFERRED_SPECIAL')
        self.assertEqual(audit['Owl_Idol']['status'], 'UNSUPPORTED_TARGET')
        self.assertEqual(audit['DNT-Unused_Rune_of_Chance']['status'], 'UNUSED')
        for identity in ['Greater_Rune_of_Leadership', 'Snake_Idol', 'Idol_of_Grold', 'Tecrods_Gaze', 'Emergent_Possibility']:
            self.assertIn(identity, self.catalog)

    def test_all_six_parsed_inventories_reproduce_the_raw_html(self):
        for code in LOCALES.values():
            actual = cards(EVIDENCE / f'Augment.{code}.html')
            expected = {row['id']: row for row in read(EVIDENCE / f'cards.{code}.json')}
            self.assertEqual(actual, expected, code)

    def test_localized_effect_order_keeps_the_same_equipment_targets(self):
        english = {row['id']: row for row in read(EVIDENCE / 'cards.us.json')}
        for code in LOCALES.values():
            labels = {}
            for card in read(EVIDENCE / f'cards.{code}.json'):
                original = english[card['id']]['ordinary']
                self.assertEqual(len(card['ordinary']), len(original))
                for index, line in enumerate(card['ordinary']):
                    target = original[index].split(':', 1)[0]
                    localized = re.split(r'[:：]', line, maxsplit=1)[0]
                    self.assertEqual(labels.setdefault(target, localized), localized, (code, card['id']))

    def test_old_names_effects_sources_ids_and_selection_are_preserved(self):
        for old in read(EVIDENCE / 'legacy-catalog.json'):
            for field in ['id', 'names', 'effects', 'sources', 'selectionEnabled']:
                self.assertEqual(self.catalog[old['id']][field], old[field], (old['id'], field))
        self.assertNotIn('Perfect_Tempered_Rune', self.catalog)
        self.assertTrue(self.catalog['Greater_Tempered_Rune']['selectionEnabled'])

    def test_mirrors_and_every_added_localized_effect_match_captured_source(self):
        self.assertEqual((ROOT / 'frontend/src/features/crafting/augments.json').read_bytes(), (ROOT / 'backend/src/main/resources/catalog/augments.json').read_bytes())
        old = {row['id'] for row in read(EVIDENCE / 'legacy-catalog.json')}
        terms = read(ROOT / 'frontend/src/shared/i18n/gameTerms.json')
        for locale, code in LOCALES.items():
            source = {row['id']: row for row in read(EVIDENCE / f'cards.{code}.json')}
            for identity, entry in self.catalog.items():
                self.assertTrue(entry['names'][locale], (identity, locale))
                if identity in old:
                    continue
                self.assertEqual(entry['names'][locale], source[identity]['name'])
                self.assertEqual(terms[locale][identity]['name'], source[identity]['name'])
                for effect in entry['effects'].values():
                    self.assertTrue(any(line.endswith(effect[locale]) for line in source[identity]['ordinary']), (identity, locale, effect[locale]))

    def test_assets_and_source_bytes_are_hash_bound(self):
        icons = read(ROOT / 'frontend/public/assets/augments/sources.json')
        for identity, entry in self.catalog.items():
            path = ROOT / 'frontend/public' / entry['image'].lstrip('/')
            self.assertTrue(path.is_file(), identity)
            if path.suffix == '.webp':
                self.assertEqual(hashlib.sha256(path.read_bytes()).hexdigest(), icons[identity]['sha256'], identity)
        for name, digest in read(EVIDENCE / 'inventory-sources.json')['sha256'].items():
            self.assertEqual(hashlib.sha256((EVIDENCE / name).read_bytes()).hexdigest(), digest, name)


if __name__ == '__main__':
    unittest.main()
