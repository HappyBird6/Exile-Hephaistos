"""The scaffold must not imply collection or publication is operational."""

import json

from poe2etl.__main__ import main


def test_status_explicitly_reports_collection_disabled(monkeypatch, capsys):
    monkeypatch.setattr("sys.argv", ["poe2etl", "--status"])
    main()
    assert json.loads(capsys.readouterr().out) == {
        "status": "NOT_CONFIGURED",
        "collectionEnabled": False,
    }
