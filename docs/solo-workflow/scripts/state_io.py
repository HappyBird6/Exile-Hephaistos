#!/usr/bin/env python3
"""Cooperative local JSON records. Caller enforces ownership and permissions."""
import argparse
import json
import os
from pathlib import Path
import sys
import tempfile


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('operation', choices=['write', 'claim', 'release'])
    parser.add_argument('path', type=Path)
    parser.add_argument('owner', nargs='?')
    args = parser.parse_args()
    path = args.path.absolute()
    if args.operation == 'write':
        value = json.load(sys.stdin)
        path.parent.mkdir(parents=True, exist_ok=True)
        fd, temporary = tempfile.mkstemp(prefix=path.name + '.', dir=path.parent)
        try:
            with os.fdopen(fd, 'w', encoding='utf-8') as stream:
                json.dump(value, stream, ensure_ascii=False, indent=2)
                stream.write('\n')
                stream.flush()
                os.fsync(stream.fileno())
            os.replace(temporary, path)
        finally:
            if os.path.exists(temporary):
                os.unlink(temporary)
    else:
        if not args.owner:
            parser.error('owner required for claim/release')
        if args.operation == 'claim':
            path.parent.mkdir(parents=True, exist_ok=True)
            from datetime import datetime, timezone
            with path.open('x', encoding='utf-8') as stream:
                json.dump({'owner': args.owner, 'acquired_at': datetime.now(timezone.utc).isoformat()}, stream)
                stream.flush()
                os.fsync(stream.fileno())
        else:
            with path.open(encoding='utf-8') as stream:
                existing = json.load(stream)
            if existing.get('owner') != args.owner:
                raise PermissionError('lock owner mismatch; lock retained')
            path.unlink()
    print(json.dumps({'status': 'ok', 'operation': args.operation}))


if __name__ == '__main__':
    try:
        main()
    except (OSError, ValueError) as exc:
        print(json.dumps({'status': 'error', 'message': str(exc)}), file=sys.stderr)
        sys.exit(1)
