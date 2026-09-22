"""CLI inspection never starts collection; captures require an explicit command."""

import json

import pytest

from poe2etl.__main__ import main


def test_status_explicitly_reports_no_automatic_collection(monkeypatch, capsys):
    monkeypatch.setattr("sys.argv", ["poe2etl", "--status"])
    main()
    assert json.loads(capsys.readouterr().out) == {
        "status": "RAW_ONLY",
        "automaticCollectionEnabled": False,
        "publicationEnabled": False,
    }


def test_no_arguments_shows_help(monkeypatch, capsys):
    monkeypatch.setattr("sys.argv", ["poe2etl"])
    main()
    assert "crawl" in capsys.readouterr().out


def test_invalid_page_never_starts_a_capture(monkeypatch, tmp_path):
    monkeypatch.setattr("sys.argv", ["poe2etl", "crawl", "--pages", "external-url"])
    with pytest.raises(SystemExit) as exc:
        main()
    assert exc.value.code == 2


def test_status_with_crawl_is_rejected(monkeypatch):
    monkeypatch.setattr("sys.argv", ["poe2etl", "--status", "crawl"])
    with pytest.raises(SystemExit) as exc:
        main()
    assert exc.value.code == 2


def test_targets_and_pages_are_mutually_exclusive(monkeypatch):
    monkeypatch.setattr(
        "sys.argv", ["poe2etl", "crawl", "--pages", "currency", "--targets-file", "targets.json"]
    )
    with pytest.raises(SystemExit) as exc:
        main()
    assert exc.value.code == 2


def test_dynamic_cli_emits_absolute_manifest(monkeypatch, capsys, tmp_path):
    from test_crawl import FakeTransport

    from poe2etl.crawl import Collector

    source = tmp_path / "targets.json"
    source.write_text(
        json.dumps(
            {
                "targets": [
                    {
                        "id": "00000000-0000-0000-0000-000000000001",
                        "name": "Synthetic",
                        "url": "https://poe2db.tw/us/Synthetic",
                        "enabled": True,
                    }
                ]
            }
        ),
        encoding="utf-8",
    )
    output = tmp_path / "job"
    monkeypatch.setattr(
        "poe2etl.__main__.Collector", lambda: Collector(FakeTransport(), lambda _: None)
    )
    monkeypatch.setattr(
        "sys.argv", ["poe2etl", "crawl", "--targets-file", str(source), "--output", str(output)]
    )
    main()
    result = json.loads(capsys.readouterr().out)
    assert result["status"] == "RAW_CAPTURED"
    assert result["manifest"] == str(next(output.glob("*/manifest.json")))
