"""Exercise dev.ps1 with Windows PowerShell 5.1 and an isolated Docker stub."""

import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]


@unittest.skipUnless(os.name == "nt", "Windows PowerShell 5.1 is required")
class DevScriptTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="poe2 dev ")
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / "scripts").mkdir()
        shutil.copyfile(ROOT / "scripts/dev.ps1", self.root / "scripts/dev.ps1")
        shutil.copyfile(ROOT / ".env.example", self.root / ".env.example")
        self.env_file = self.root / ".env"
        self.log = self.root / "calls.jsonl"

    def run_script(self, target, *, existing_volume=False, failure=""):
        env = os.environ.copy()
        env.update(TEST_ROOT=str(self.root), TEST_LOG=str(self.log),
                   TEST_EXISTING="yes" if existing_volume else "no", TEST_FAILURE=failure,
                   COMPOSE_PROJECT_NAME="unrelated-project")
        command = r"""
if ($PSVersionTable.PSVersion.Major -ne 5) { throw 'Test must run on PowerShell 5.1' }
function docker {
    $global:LASTEXITCODE = 0
    $items = @($args)
    ConvertTo-Json -InputObject $items -Compress | Add-Content -LiteralPath $env:TEST_LOG
    if ($items -contains 'config') {
        if ($env:TEST_FAILURE -eq 'config') { $global:LASTEXITCODE = 9; return }
        '{"volumes":{"postgres-data":{"name":"fixture-volume"}}}'
    } elseif ($items[0] -eq 'volume' -and $items[1] -eq 'ls') {
        if ($env:TEST_EXISTING -eq 'yes') { 'fixture-volume' }
    } elseif ($items -contains 'up' -and $env:TEST_FAILURE -eq 'up') {
        $global:LASTEXITCODE = 17
    }
}
& (Join-Path $env:TEST_ROOT 'scripts/dev.ps1') TARGET
""".replace("TARGET", target)
        result = subprocess.run(
            ["powershell.exe", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", command],
            cwd=ROOT.parent, env=env, capture_output=True, text=True,
            encoding="utf-8", errors="replace", timeout=30,
        )
        calls = [json.loads(line) for line in self.log.read_text().splitlines()] if self.log.exists() else []
        return result, calls

    def test_first_start_creates_matching_random_passwords_and_volume(self):
        result, calls = self.run_script("stack")
        self.assertEqual(result.returncode, 0, result.stderr)
        content = self.env_file.read_text(encoding="utf-8")
        values = dict(line.split("=", 1) for line in content.splitlines() if line and not line.startswith("#"))
        self.assertRegex(values["POSTGRES_PASSWORD"], r"^[a-f0-9]{48}$")
        self.assertEqual(values["POSTGRES_PASSWORD"], values["SPRING_DATASOURCE_PASSWORD"])
        self.assertNotIn(values["POSTGRES_PASSWORD"], result.stdout + result.stderr)
        self.assertFalse(self.env_file.read_bytes().startswith(b"\xef\xbb\xbf"))
        self.assertIn(["volume", "create", "fixture-volume"], calls)
        for call in calls:
            if call[0] == "compose":
                self.assertEqual(call[call.index("--project-name") + 1], "exile-hephaistos")
                self.assertEqual(Path(call[call.index("-f") + 1]).resolve(), (self.root / "infra/compose.yaml").resolve())
                self.assertEqual(Path(call[call.index("--env-file") + 1]).resolve(), self.env_file.resolve())
        self.assertTrue(any("up" in c and "--build" in c for c in calls))

    def test_existing_environment_and_volume_are_preserved(self):
        original = b"POSTGRES_PASSWORD=local-test-value\n"
        self.env_file.write_bytes(original)
        result, calls = self.run_script("infra", existing_volume=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.env_file.read_bytes(), original)
        self.assertFalse(any(c[:2] == ["volume", "create"] for c in calls))

    def test_invalid_configuration_never_creates_volume_or_starts_stack(self):
        result, calls = self.run_script("stack", failure="config")
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(any(c[0] == "volume" or "up" in c for c in calls))

    def test_docker_failure_is_not_reported_as_success(self):
        result, _ = self.run_script("stack", existing_volume=True, failure="up")
        self.assertNotEqual(result.returncode, 0)

    def test_stop_does_not_create_environment(self):
        result, calls = self.run_script("stop")
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(self.env_file.exists())
        self.assertEqual(calls, [])

    def test_stop_only_stops_fixed_project(self):
        self.env_file.write_text("POSTGRES_PASSWORD=local-test-value\n")
        result, calls = self.run_script("stop")
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(len(calls), 1)
        self.assertEqual(calls[0][-1], "stop")
        self.assertNotIn("down", calls[0])


if __name__ == "__main__":
    unittest.main()
