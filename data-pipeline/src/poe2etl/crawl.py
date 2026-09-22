"""Bounded, offline-batch source capture; never publishes a game catalog."""

import hashlib
import json
import re
import time
import uuid
from dataclasses import dataclass
from datetime import UTC, datetime
from email.utils import parsedate_to_datetime
from http.client import HTTPException
from pathlib import Path
from typing import Protocol
from urllib.error import HTTPError, URLError
from urllib.request import HTTPRedirectHandler, Request, build_opener
from urllib.robotparser import RobotFileParser

ORIGIN = "https://poe2db.tw"
USER_AGENT = "Exile-Hephaistos-Research/0.1"
COLLECTOR_VERSION = "raw-2"
PAGES = {"currency": "/us/Currency", "amulets": "/us/Amulets"}
ROBOTS = "/robots.txt"
DISCLAIMER = "/us/General_disclaimer"
MAX_BYTES = 8 * 1024 * 1024
MAX_RETRY_WAIT = 60
MAX_TARGETS = 20
MAX_TARGET_FILE_BYTES = 64 * 1024
TARGET_URL = re.compile(r"https://poe2db[.]tw/(?:us|kr)/[A-Za-z0-9_-]+", re.ASCII)


class CaptureError(Exception):
    """Explicit capture failure with a stable, non-secret reason."""


def validate_url(url: object, *, policy: bool = False) -> str:
    if not isinstance(url, str) or not (
        TARGET_URL.fullmatch(url) or (policy and url == ORIGIN + ROBOTS)
    ):
        raise CaptureError("URL_NOT_ALLOWED")
    return url.removeprefix(ORIGIN)


def load_targets(path: Path) -> list[dict]:
    if not path.is_absolute() or not path.is_file() or path.is_symlink():
        raise CaptureError("INVALID_TARGET_FILE_PATH")
    with path.open("rb") as stream:
        raw = stream.read(MAX_TARGET_FILE_BYTES + 1)
    if len(raw) > MAX_TARGET_FILE_BYTES:
        raise CaptureError("TARGET_FILE_TOO_LARGE")
    try:
        document = json.loads(raw)
    except (ValueError, UnicodeError) as exc:
        raise CaptureError("INVALID_TARGET_JSON") from exc
    if not isinstance(document, dict) or set(document) != {"targets"}:
        raise CaptureError("INVALID_TARGET_SCHEMA")
    return validate_targets(document["targets"])


def validate_targets(targets: object) -> list[dict]:
    if not isinstance(targets, list) or not 1 <= len(targets) <= MAX_TARGETS:
        raise CaptureError("INVALID_TARGET_COUNT")
    ids, urls = set(), set()
    result = []
    for target in targets:
        if not isinstance(target, dict) or set(target) != {"id", "name", "url", "enabled"}:
            raise CaptureError("INVALID_TARGET_SCHEMA")
        if (
            not isinstance(target["id"], str)
            or not isinstance(target["name"], str)
            or not target["name"].strip()
            or len(target["name"]) > 80
            or type(target["enabled"]) is not bool
        ):
            raise CaptureError("INVALID_TARGET_SCHEMA")
        try:
            identity = str(uuid.UUID(target["id"]))
        except ValueError as exc:
            raise CaptureError("INVALID_TARGET_ID") from exc
        validate_url(target["url"])
        if identity in ids or target["url"] in urls:
            raise CaptureError("DUPLICATE_TARGET")
        ids.add(identity)
        urls.add(target["url"])
        result.append(dict(target))
    if not any(target["enabled"] for target in result):
        raise CaptureError("NO_ENABLED_TARGETS")
    return result


@dataclass(frozen=True)
class Response:
    status: int
    headers: dict[str, str]
    body: bytes


class Transport(Protocol):
    def __call__(self, url: str) -> Response: ...


class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        # A redirect must be reviewed, not followed to an unapproved host/path.
        return None


def fetch(url: str) -> Response:
    """One GET, no redirects/cookies/auth/proxy-discovery beyond urllib defaults."""
    validate_url(url, policy=True)
    request = Request(url, headers={"User-Agent": USER_AGENT, "Accept-Encoding": "identity"})
    opener = build_opener(NoRedirect())
    try:
        stream = opener.open(request, timeout=30)
    except HTTPError as exc:
        stream = exc
    except (URLError, TimeoutError, OSError, HTTPException) as exc:
        raise CaptureError("NETWORK_ERROR") from exc
    try:
        with stream:
            body = stream.read(MAX_BYTES + 1)
            if len(body) > MAX_BYTES:
                raise CaptureError("RESPONSE_TOO_LARGE")
            return Response(stream.code, {k.lower(): v for k, v in stream.headers.items()}, body)
    except (URLError, TimeoutError, OSError, HTTPException) as exc:
        raise CaptureError("NETWORK_READ_ERROR") from exc


def retry_wait(value: str | None) -> float:
    if value is None:
        return 5
    try:
        seconds = (
            float(value)
            if value.isascii() and value.isdigit()
            else (parsedate_to_datetime(value) - datetime.now(UTC)).total_seconds()
        )
    except (ValueError, TypeError, OverflowError) as exc:
        raise CaptureError("INVALID_RETRY_AFTER") from exc
    if seconds > MAX_RETRY_WAIT:
        raise CaptureError("RETRY_AFTER_EXCEEDS_BATCH_LIMIT")
    return max(0, seconds)


def write_json(path: Path, value: object) -> None:
    # Fresh run directory + exclusive files preserve earlier captures, even after failures.
    with path.open("x", encoding="utf-8", newline="\n") as stream:
        json.dump(value, stream, ensure_ascii=False, indent=2, allow_nan=False)
        stream.write("\n")


class Collector:
    def __init__(self, transport: Transport = fetch, sleep=time.sleep):
        self.transport = transport
        self.sleep = sleep
        self.interval = 2.0
        self.request_count = 0

    def get(
        self, path: str, run: Path, sources: list[dict], target_id: str | None = None
    ) -> Response:
        url = ORIGIN + path
        validate_url(url, policy=True)
        for attempt in range(2):
            if self.request_count:
                self.sleep(self.interval)
            self.request_count += 1
            response = self.transport(url)
            if len(response.body) > MAX_BYTES:
                raise CaptureError("RESPONSE_TOO_LARGE")
            digest = hashlib.sha256(response.body).hexdigest()
            filename = f"{len(sources):02d}-{digest}.bin"
            with (run / "raw" / filename).open("xb") as stream:
                stream.write(response.body)
            metadata = {
                "url": url,
                "locale": path.split("/")[1] if path.startswith(("/us/", "/kr/")) else None,
                "targetId": target_id,
                "fetchedAt": datetime.now(UTC).isoformat(),
                "status": response.status,
                "sha256": digest,
                "bytes": len(response.body),
                "file": "raw/" + filename,
                "attempt": attempt + 1,
                # Do not retain Set-Cookie or incidental server identifiers.
                "headers": {
                    k: v
                    for k, v in response.headers.items()
                    if k in {"content-type", "etag", "last-modified", "retry-after"}
                },
            }
            sources.append(metadata)
            write_json(run / "raw" / (filename + ".json"), metadata)
            if response.status in {429, 502, 503, 504} and attempt == 0:
                self.sleep(retry_wait(response.headers.get("retry-after")))
                continue
            if response.status != 200:
                raise CaptureError(f"HTTP_{response.status}")
            return response
        raise AssertionError("unreachable")

    def capture(
        self,
        output: Path,
        pages: list[str] | None = None,
        target_patch: str | None = None,
        *,
        targets_file: Path | None = None,
    ) -> Path:
        """Capture bounded configured pages plus robots and the policy notice."""
        if targets_file is None and (not pages or any(page not in PAGES for page in pages)):
            raise ValueError("Select currency and/or amulets")
        self.request_count = 0
        self.interval = 2.0
        if targets_file is not None and pages is not None:
            raise CaptureError("CONFLICTING_TARGET_SELECTION")
        selected = list(dict.fromkeys(pages or []))
        run_id = datetime.now(UTC).strftime("%Y%m%dT%H%M%SZ") + "-" + uuid.uuid4().hex[:12]
        run = output / run_id
        (run / "raw").mkdir(parents=True, exist_ok=False)
        sources: list[dict] = []
        manifest = {
            "schemaVersion": 1,
            "importRunId": run_id,
            "collectorVersion": COLLECTOR_VERSION,
            "parserVersion": None,
            "targetPatch": target_patch,
            "patchVerified": False,
            "pages": selected,
            "targets": [],
            "status": "FAILED",
            "productionEligible": False,
            "sources": sources,
            "limitations": [
                "Raw source capture only; no normalized currencies, modifiers or weights.",
                "No completeness, current-game availability or patch consistency assertion.",
                "Redistribution rights and game semantics require separate review.",
            ],
        }
        try:
            if targets_file is not None:
                targets = load_targets(targets_file)
                manifest["targets"] = targets
                entries = [
                    (validate_url(target["url"]), target["id"])
                    for target in targets
                    if target["enabled"]
                ]
            else:
                entries = [(PAGES[page], None) for page in selected]
            robots_response = self.get(ROBOTS, run, sources)
            if "text/plain" not in robots_response.headers.get("content-type", "").lower():
                raise CaptureError("INVALID_ROBOTS_CONTENT_TYPE")
            try:
                robots_text = robots_response.body.decode("utf-8-sig")
            except UnicodeDecodeError as exc:
                raise CaptureError("INVALID_ROBOTS_ENCODING") from exc
            if not any(
                line.strip().lower().startswith("user-agent:") for line in robots_text.splitlines()
            ):
                raise CaptureError("INVALID_ROBOTS")
            robots = RobotFileParser()
            robots.parse(robots_text.splitlines())
            entries = [(DISCLAIMER, None), *entries]
            paths = [path for path, _ in entries]
            if any(not robots.can_fetch(USER_AGENT, ORIGIN + path) for path in paths):
                raise CaptureError("ROBOTS_DISALLOWED")
            delay = robots.crawl_delay(USER_AGENT)
            rate = robots.request_rate(USER_AGENT)
            self.interval = max(2.0, delay or 0, rate.seconds / rate.requests if rate else 0)
            if self.interval > MAX_RETRY_WAIT:
                raise CaptureError("ROBOTS_DELAY_EXCEEDS_BATCH_LIMIT")
            for path, target_id in entries:
                response = self.get(path, run, sources, target_id)
                if "text/html" not in response.headers.get("content-type", "").lower():
                    raise CaptureError("INVALID_HTML_CONTENT_TYPE")
                if not response.body.strip():
                    raise CaptureError("EMPTY_HTML")
            manifest["status"] = "RAW_CAPTURED"
        except (CaptureError, OSError) as exc:
            manifest["errorCode"] = str(exc) if isinstance(exc, CaptureError) else "FILE_IO_ERROR"
            raise
        finally:
            manifest["requestCount"] = self.request_count
            manifest["finishedAt"] = datetime.now(UTC).isoformat()
            write_json(run / "manifest.json", manifest)
        return run
