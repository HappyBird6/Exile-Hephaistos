"""Configurable capture uses bounded snapshots and synthetic responses only."""

import json
import socket
import uuid

import pytest
from test_crawl import FakeTransport, manifest_at

from poe2etl.crawl import (
    MAX_TARGET_FILE_BYTES,
    ORIGIN,
    CaptureError,
    Collector,
    fetch,
)


def target(**changes):
    return {
        "id": str(uuid.uuid4()),
        "name": "Synthetic target",
        "url": ORIGIN + "/kr/Synthetic_Test",
        "enabled": True,
        **changes,
    }


def capture(tmp_path, targets):
    source = tmp_path / "targets.json"
    source.write_text(json.dumps({"targets": targets}), encoding="utf-8")
    transport = FakeTransport()
    output = tmp_path / "output"
    collector = Collector(transport, lambda _: None)
    return collector, transport, output, source


def test_dynamic_targets_preserve_snapshot_and_skip_disabled(tmp_path):
    targets = [target(), target(url=ORIGIN + "/us/Skipped", enabled=False)]
    collector, transport, output, source = capture(tmp_path, targets)
    collector.capture(output, targets_file=source)
    manifest = manifest_at(output)
    assert manifest["targets"] == targets
    assert len(transport.calls) == 3
    assert transport.calls[-1] == targets[0]["url"]
    assert [entry["targetId"] for entry in manifest["sources"]] == [None, None, targets[0]["id"]]
    assert manifest["sources"][-1]["locale"] == "kr"
    assert manifest["parserVersion"] is None
    assert manifest["productionEligible"] is False


@pytest.mark.parametrize(
    "url",
    [
        "http://poe2db.tw/us/Test",
        "https://poe2db.tw:443/us/Test",
        "https://user@poe2db.tw/us/Test",
        "https://poe2db.tw/us/Test?",
        "https://poe2db.tw/us/Test#",
        "https://poe2db.tw/us/%54est",
        "https://poe2db.tw/us/../Test",
        "https://poe2db.tw/us/Test/Other",
        "https://poe2db.tw.evil/us/Test",
        "http://127.0.0.1:8000/us/Test",
        "https://poe2db.tw/us/Test\n",
        "https://poe2db.tw/robots.txt",
    ],
)
def test_forbidden_target_never_requests_network(tmp_path, url):
    collector, transport, output, source = capture(tmp_path, [target(url=url)])
    with pytest.raises(CaptureError, match="URL_NOT_ALLOWED"):
        collector.capture(output, targets_file=source)
    assert not transport.calls
    assert manifest_at(output)["status"] == "FAILED"


@pytest.mark.parametrize(
    "targets,error",
    [
        ([], "INVALID_TARGET_COUNT"),
        ({}, "INVALID_TARGET_COUNT"),
        ([target(enabled=False)], "NO_ENABLED_TARGETS"),
        ([target(enabled=1)], "INVALID_TARGET_SCHEMA"),
        ([target(id="bad")], "INVALID_TARGET_ID"),
        ([target(name=[])], "INVALID_TARGET_SCHEMA"),
        ([target(url=123)], "URL_NOT_ALLOWED"),
        ([target(), target()], "DUPLICATE_TARGET"),
        (
            [
                target(id="00000000-0000-0000-0000-000000000001"),
                target(id="00000000000000000000000000000001", url=ORIGIN + "/us/Other"),
            ],
            "DUPLICATE_TARGET",
        ),
        ([target(url=ORIGIN + f"/us/T{i}") for i in range(21)], "INVALID_TARGET_COUNT"),
    ],
)
def test_invalid_snapshot_fails_with_manifest(tmp_path, targets, error):
    collector, transport, output, source = capture(tmp_path, targets)
    with pytest.raises(CaptureError, match=error):
        collector.capture(output, targets_file=source)
    assert not transport.calls
    assert manifest_at(output)["errorCode"] == error


@pytest.mark.parametrize(
    "content,error",
    [
        (b"{", "INVALID_TARGET_JSON"),
        (b"x" * (MAX_TARGET_FILE_BYTES + 1), "TARGET_FILE_TOO_LARGE"),
        (b"{}", "INVALID_TARGET_SCHEMA"),
    ],
    ids=["malformed", "oversized", "schema"],
)
def test_bad_target_file(tmp_path, content, error):
    collector, transport, output, source = capture(tmp_path, [target()])
    source.write_bytes(content)
    with pytest.raises(CaptureError, match=error):
        collector.capture(output, targets_file=source)
    assert not transport.calls
    assert manifest_at(output)["errorCode"] == error


def test_relative_target_file_is_rejected_with_manifest(tmp_path):
    from pathlib import Path

    collector, transport, output, _ = capture(tmp_path, [target()])
    with pytest.raises(CaptureError, match="INVALID_TARGET_FILE_PATH"):
        collector.capture(output, targets_file=Path("targets.json"))
    assert not transport.calls
    assert manifest_at(output)["status"] == "FAILED"


def test_local_network_is_blocked_and_local_url_rejected():
    with pytest.raises(AssertionError, match="Network access is forbidden"):
        socket.getaddrinfo("127.0.0.1", 8000)
    with pytest.raises(CaptureError, match="URL_NOT_ALLOWED"):
        fetch("http://127.0.0.1:8000/us/Test")


def test_dynamic_target_robots_denial_stops_before_html(tmp_path):
    from poe2etl.crawl import ROBOTS, Response

    collector, transport, output, source = capture(tmp_path, [target()])
    transport.overrides[ROBOTS] = [
        Response(200, {"content-type": "text/plain"}, b"User-agent: *\nDisallow: /kr/\n")
    ]
    with pytest.raises(CaptureError, match="ROBOTS_DISALLOWED"):
        collector.capture(output, targets_file=source)
    assert transport.calls == [ORIGIN + ROBOTS]
    assert manifest_at(output)["targets"][0]["url"] == ORIGIN + "/kr/Synthetic_Test"
