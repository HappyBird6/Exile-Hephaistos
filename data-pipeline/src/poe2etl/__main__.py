"""Report the scaffold state without network or database access."""

import argparse
import json


def main() -> None:
    parser = argparse.ArgumentParser(description="PoE2 offline ETL foundation")
    parser.add_argument("--status", action="store_true", help="Print current support status")
    args = parser.parse_args()
    if args.status:
        print(json.dumps({"status": "NOT_CONFIGURED", "collectionEnabled": False}))
    else:
        parser.print_help()


if __name__ == "__main__":
    main()
