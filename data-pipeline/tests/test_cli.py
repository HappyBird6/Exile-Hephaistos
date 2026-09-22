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
