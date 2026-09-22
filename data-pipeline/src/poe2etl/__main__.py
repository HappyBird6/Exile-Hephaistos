"""Explicit raw-capture command; help and status never access the network."""

import argparse
import json
from pathlib import Path

from poe2etl.crawl import PAGES, CaptureError, Collector


def main() -> None:
    parser = argparse.ArgumentParser(description="PoE2 offline ETL foundation")
    parser.add_argument("--status", action="store_true", help="Print current support status")
    commands = parser.add_subparsers(dest="command")
    crawl = commands.add_parser("crawl", help="Capture selected poe2db pages (network access)")
    crawl.add_argument("--output", type=Path, default=Path("captures"))
    selection = crawl.add_mutually_exclusive_group()
    selection.add_argument("--pages", nargs="+", choices=sorted(PAGES))
    selection.add_argument("--targets-file", type=Path)
    crawl.add_argument("--target-patch", help="Operator label only; does not verify source patch")
    args = parser.parse_args()
    if args.status and args.command:
        parser.error("--status cannot be combined with a command")
    if args.status:
        print(
            json.dumps(
                {
                    "status": "RAW_ONLY",
                    "automaticCollectionEnabled": False,
                    "publicationEnabled": False,
                }
            )
        )
    elif args.command == "crawl":
        try:
            run = Collector().capture(
                args.output.resolve(),
                args.pages
                if args.pages is not None
                else (None if args.targets_file else list(PAGES)),
                args.target_patch,
                targets_file=args.targets_file,
            )
        except (CaptureError, OSError) as exc:
            parser.exit(1, f"Capture failed: {exc}\n")
        print(json.dumps({"status": "RAW_CAPTURED", "manifest": str(run / "manifest.json")}))
    else:
        parser.print_help()


if __name__ == "__main__":
    main()
