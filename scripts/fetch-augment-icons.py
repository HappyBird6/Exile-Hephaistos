"""Capture source-backed icons for registered Augments without replacing old assets."""
import concurrent.futures
import hashlib
import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'frontend/public/assets/augments'
EVIDENCE = ROOT / 'docs/evidence/0.5.5/augment-expansion-20261105'


def main():
    source = {entry['id']: entry for entry in json.loads((EVIDENCE / 'cards.us.json').read_text(encoding='utf-8'))}
    catalog = json.loads((ROOT / 'frontend/src/features/crafting/augments.json').read_text(encoding='utf-8'))
    metadata_path = ASSETS / 'sources.json'
    metadata = json.loads(metadata_path.read_text(encoding='utf-8'))

    def capture(entry):
        identity = entry['id']
        if metadata.get(identity):
            return None
        image = source[identity]['image']
        assert image.startswith('https://cdn.poe2db.tw/image/') and image.endswith('.webp')
        destination = ASSETS / (identity.replace('%', '_') + '.webp')
        subprocess.run(['curl.exe', '-L', '--fail', '--silent', '--show-error', '--max-time', '60', '--retry', '2', image, '-o', str(destination)], check=True)
        raw = destination.read_bytes()
        assert raw[:4] == b'RIFF' and raw[8:12] == b'WEBP', identity
        return identity, {'page': 'https://poe2db.tw/us/' + identity, 'image': image, 'retrievedAt': '2026-11-05', 'sha256': hashlib.sha256(raw).hexdigest()}

    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as executor:
        for result in executor.map(capture, catalog):
            if result:
                metadata[result[0]] = result[1]
    metadata_path.write_bytes((json.dumps(metadata, ensure_ascii=False, indent=2) + '\n').encode('utf-8'))
    print('Captured icon metadata:', len(metadata))


if __name__ == '__main__':
    main()
