"""Synthetic HTTP responses test capture boundaries without actual game data."""

import hashlib
import json
from pathlib import Path
from urllib.error import URLError

import pytest

from poe2etl.crawl import (
    DISCLAIMER,
    MAX_BYTES,
    ORIGIN,
    PAGES,
    ROBOTS,
    CaptureError,
    Collector,
    NoRedirect,
    Response,
    fetch,
    retry_wait,
)

HTML = (Path(__file__).parent / "fixtures/synthetic/source.html").read_bytes()
ROBOTS_RESPONSE = Response(200, {"content-type": "text/plain"}, b"User-agent: *\nAllow: /\n")
PAGE_RESPONSE = Response(200, {"content-type": "text/html", "set-cookie": "synthetic"}, HTML)


class FakeTransport:
    def __init__(self, overrides=None):
        self.calls = []
        self.overrides = overrides or {}

    def __call__(self, url):
        self.calls.append(url)
        path = url.removeprefix(ORIGIN)
        responses = self.overrides.get(path)
        if responses:
            return responses.pop(0)
        return ROBOTS_RESPONSE if path == ROBOTS else PAGE_RESPONSE


def manifest_at(output):
    return json.loads(next(output.glob("*/manifest.json")).read_text(encoding="utf-8"))


def test_capture_preserves_bytes_provenance_and_duplicate_page_deduplication(tmp_path):
    transport = FakeTransport()
    sleeps = []
    run = Collector(transport, sleeps.append).capture(tmp_path, ["currency", "currency", "amulets"])
    manifest = manifest_at(tmp_path)
    assert transport.calls == [ORIGIN + p for p in [ROBOTS, DISCLAIMER, *PAGES.values()]]
    assert sleeps == [2, 2, 2]
    assert manifest["status"] == "RAW_CAPTURED"
    assert manifest["targetPatch"] is None
    assert manifest["patchVerified"] is False
    assert manifest["productionEligible"] is False
    for entry in manifest["sources"]:
        body = (run / entry["file"]).read_bytes()
        assert hashlib.sha256(body).hexdigest() == entry["sha256"]
        assert "set-cookie" not in entry["headers"]
        assert entry["fetchedAt"]


def test_repeat_capture_does_not_overwrite_existing_evidence(tmp_path):
    collector = Collector(FakeTransport(), lambda _: None)
    first = collector.capture(tmp_path, ["currency"])
    before = {p.relative_to(first): p.read_bytes() for p in first.rglob("*") if p.is_file()}
    second = Collector(FakeTransport(), lambda _: None).capture(tmp_path, ["currency"])
    assert first != second
    assert before == {p.relative_to(first): p.read_bytes() for p in first.rglob("*") if p.is_file()}


def test_robots_denial_prevents_all_content_requests(tmp_path):
    transport = FakeTransport(
        {ROBOTS: [Response(200, {"content-type": "text/plain"}, b"User-agent: *\nDisallow: /us/")]}
    )
    with pytest.raises(CaptureError, match="ROBOTS_DISALLOWED"):
        Collector(transport, lambda _: None).capture(tmp_path, ["currency"])
    assert transport.calls == [ORIGIN + ROBOTS]
    assert manifest_at(tmp_path)["status"] == "FAILED"


@pytest.mark.parametrize(
    "response,error",
    [
        (Response(200, {"content-type": "text/html"}, HTML), "INVALID_ROBOTS_CONTENT_TYPE"),
        (Response(200, {"content-type": "text/plain"}, b""), "INVALID_ROBOTS"),
        (Response(404, {}, b"missing"), "HTTP_404"),
    ],
)
def test_missing_or_invalid_robots_fails_closed(tmp_path, response, error):
    transport = FakeTransport({ROBOTS: [response]})
    with pytest.raises(CaptureError, match=error):
        Collector(transport, lambda _: None).capture(tmp_path, ["currency"])
    assert len(transport.calls) == 1


@pytest.mark.parametrize("status", [301, 302, 401, 403, 404])
def test_blocked_or_redirected_page_is_not_retried(tmp_path, status):
    transport = FakeTransport({PAGES["currency"]: [Response(status, {}, b"blocked")]})
    with pytest.raises(CaptureError, match=f"HTTP_{status}"):
        Collector(transport, lambda _: None).capture(tmp_path, ["currency", "amulets"])
    assert len(transport.calls) == 3
    assert manifest_at(tmp_path)["errorCode"] == f"HTTP_{status}"


def test_rate_limit_retries_once_with_backoff_and_records_both_attempts(tmp_path):
    sleeps = []
    transport = FakeTransport(
        {PAGES["currency"]: [Response(429, {"retry-after": "10"}, b"limited"), PAGE_RESPONSE]}
    )
    Collector(transport, sleeps.append).capture(tmp_path, ["currency"])
    assert sleeps == [2, 2, 10, 2]
    assert [s["status"] for s in manifest_at(tmp_path)["sources"]] == [200, 200, 429, 200]


def test_repeated_transient_failure_stops_batch(tmp_path):
    transport = FakeTransport({PAGES["currency"]: [Response(503, {}, b"busy")] * 2})
    with pytest.raises(CaptureError, match="HTTP_503"):
        Collector(transport, lambda _: None).capture(tmp_path, ["currency", "amulets"])
    assert len(transport.calls) == 4


def test_crawl_delay_is_respected(tmp_path):
    robots = Response(
        200, {"content-type": "text/plain"}, b"User-agent: *\nAllow: /\nCrawl-delay: 7\n"
    )
    sleeps = []
    Collector(FakeTransport({ROBOTS: [robots]}), sleeps.append).capture(tmp_path, ["amulets"])
    assert sleeps == [7, 7]


@pytest.mark.parametrize(
    "response,error",
    [
        (Response(200, {"content-type": "application/json"}, b"{}"), "INVALID_HTML_CONTENT_TYPE"),
        (Response(200, {"content-type": "text/html"}, b""), "EMPTY_HTML"),
        (Response(200, {}, b"x" * (MAX_BYTES + 1)), "RESPONSE_TOO_LARGE"),
    ],
)
def test_bad_content_is_not_a_successful_capture(tmp_path, response, error):
    transport = FakeTransport({PAGES["currency"]: [response]})
    with pytest.raises(CaptureError, match=error):
        Collector(transport, lambda _: None).capture(tmp_path, ["currency"])
    assert manifest_at(tmp_path)["status"] == "FAILED"


def test_network_failure_leaves_failed_manifest(tmp_path):
    def unavailable(url):
        raise CaptureError("NETWORK_ERROR")

    with pytest.raises(CaptureError, match="NETWORK_ERROR"):
        Collector(unavailable, lambda _: None).capture(tmp_path, ["amulets"])
    assert manifest_at(tmp_path)["sources"] == []
    assert manifest_at(tmp_path)["errorCode"] == "NETWORK_ERROR"


def test_unsupported_selection_creates_no_output(tmp_path):
    with pytest.raises(ValueError):
        Collector(FakeTransport()).capture(tmp_path, ["all"])
    assert not list(tmp_path.iterdir())


def test_transport_rejects_unlisted_url():
    with pytest.raises(CaptureError, match="URL_NOT_ALLOWED"):
        fetch("https://example.invalid/")


def test_transport_does_not_follow_redirects():
    assert (
        NoRedirect().redirect_request(None, None, 302, "", {}, "https://example.invalid/") is None
    )


def test_transport_classifies_connection_failure(monkeypatch):
    class Unavailable:
        def open(self, *args, **kwargs):
            raise URLError("synthetic failure")

    monkeypatch.setattr("poe2etl.crawl.build_opener", lambda *args: Unavailable())
    with pytest.raises(CaptureError, match="NETWORK_ERROR"):
        fetch(ORIGIN + ROBOTS)


@pytest.mark.parametrize("value,error", [("invalid", "INVALID_RETRY_AFTER"), ("61", "EXCEEDS")])
def test_bad_retry_after_stops_instead_of_retrying_early(value, error):
    with pytest.raises(CaptureError, match=error):
        retry_wait(value)
