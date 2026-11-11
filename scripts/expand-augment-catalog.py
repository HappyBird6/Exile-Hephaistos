"""Audit current canonical Augments against the historical source inventory; never rewrite catalogs.

The old six-language generator overwrote later crafting rules and translations.
Edit reviewed backend catalog data explicitly, then use sync-workbench-data.mjs --write.
The historical source inventory remains evidence, not today's support policy.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

def audit():
    catalog = json.loads((ROOT / 'backend/src/main/resources/catalog/augments.json').read_text())
    rules = json.loads((ROOT / 'backend/src/main/resources/catalog/augment-crafting.json').read_text())
    source = json.loads((ROOT / 'docs/evidence/0.5.5/augment-expansion-20261105/cards.us.json').read_text())
    ids = {row['id'] for row in catalog}
    assert len(ids) == len(catalog), 'Duplicate Augment IDs'
    assert ids <= {row['id'] for row in source}, 'New source identities require source review'
    assert set(rules['effects']) <= ids, 'Crafting Augment removed from canonical catalog'
    assert set(rules['socketBound']) <= ids, 'Socket-bound Augment removed from catalog'
    print(json.dumps({'registered': len(catalog), 'selectable': sum(row['selectionEnabled'] for row in catalog), 'craftingRules': len(rules['effects']), 'writes': False}))

if __name__ == '__main__':
    audit()
