"""Contract fixture oracle only; no application, network or probability engine."""
import json
import math
from pathlib import Path

data = json.loads(Path(__file__).with_name("fixtures.json").read_text(encoding="utf-8"))
assert data["version"] == 1 and data["synthetic"]
templates = data["entryTemplates"]
stats = {s["statId"]: s for s in data["stats"]}
assert len(stats) == len(data["stats"])


def within(value, bounds):
    return ((bounds["min"] is None or value >= bounds["min"])
            and (bounds["max"] is None or value <= bounds["max"]))


def observe(stat_id, observed):
    contributions = stats[stat_id]["contributions"]
    if not contributions:
        if stat_id not in observed:
            return "ABSENT", None
        value = observed[stat_id]
        return ("UNKNOWN", None) if value is None else ("PRESENT", value)
    parts = [(observe(c["statId"], observed), c["coefficient"]) for c in contributions]
    if any(p[0] == "UNKNOWN" for p, _ in parts):
        return "UNKNOWN", None
    if all(p[0] == "ABSENT" for p, _ in parts):
        return "ABSENT", None
    return "PRESENT", sum(p[1] * coefficient for p, coefficient in parts if p[0] == "PRESENT")


for case in data["cases"]:
    rows = [(name, templates[name]) for name in case["entries"]
            if name not in case.get("disabledEntries", [])]
    observations = [observe(row["statId"], case["observedStats"]) for _, row in rows]
    matched = [None if presence == "UNKNOWN" else presence == "PRESENT" and within(value, row["range"])
               for (_, row), (presence, value) in zip(rows, observations)]
    kind = case["type"]
    score = count = None
    if kind in ("AND", "IF"):
        checks = [True if kind == "IF" and presence == "ABSENT" else match
                  for match, (presence, _) in zip(matched, observations)]
        result = False if False in checks else None if None in checks else True
    elif kind == "NOT":
        result = False if True in matched else None if None in matched else True
    elif kind == "COUNT":
        count = sum(m is True for m in matched)
        possible = range(count, count + sum(m is None for m in matched) + 1)
        checks = [within(n, case["range"]) for n in possible]
        result = True if all(checks) else False if not any(checks) else None
    else:
        assert kind in ("WEIGHTED_V1", "WEIGHTED_V2")
        assert len(case["weights"]) == len(rows)
        score = sum(value * weight for match, (presence, value), weight
                    in zip(matched, observations, case["weights"]) if match is True)
        if kind == "WEIGHTED_V1" and any(p == "PRESENT" and m is False for m, (p, _) in zip(matched, observations)):
            result = False
        elif None in matched:
            result = None
        else:
            result = within(score, case["range"]) and (kind == "WEIGHTED_V1" or any(p == "PRESENT" for p, _ in observations))
    status = "UNKNOWN" if result is None else "MATCH" if result else "NO_MATCH"
    assert status == case["expected"], (case["id"], status)
    if "expectedScore" in case:
        assert score == case["expectedScore"], case["id"]
    if "expectedCount" in case:
        assert count == case["expectedCount"], case["id"]
    if "expectedValue" in case:
        assert observations[0][1] == case["expectedValue"], case["id"]

assert len({c["id"] for c in data["cases"] + data["validationCases"]}) == len(data["cases"]) + len(data["validationCases"])
for case in data["validationCases"]:
    mutation = case["mutation"]
    row = {**templates["cold"], **mutation}
    if mutation.get("version", 1) != 1:
        code = "UNSUPPORTED_VERSION"
    elif mutation.get("catalogVersion", data["catalogVersion"]) != data["catalogVersion"]:
        code = "CATALOG_VERSION_MISMATCH"
    elif mutation.get("disableAllGroups"):
        code = "EMPTY_GOAL"
    elif mutation.get("disableAllEntries"):
        code = "EMPTY_GROUP"
    elif mutation.get("duplicateEntry"):
        code = "DUPLICATE_STAT"
    elif row["statId"] not in stats:
        code = "UNKNOWN_STAT"
    elif row["unit"] != stats[row["statId"]]["unit"]:
        code = "UNIT_MISMATCH"
    elif row["range"]["min"] > row["range"]["max"]:
        code = "INVALID_RANGE"
    elif mutation.get("type") == "COUNT" and any(n is not None and (n < 0 or int(n) != n) for n in row["range"].values()):
        code = "INVALID_COUNT_RANGE"
    else:
        raise AssertionError((case["id"], "No validation error"))
    assert code == case["expectedCode"], (case["id"], code)

for name, example in data["apiExamples"].items():
    if "goal" in example:
        goal = example["goal"]
        assert goal["version"] == 1 and goal["catalogVersion"] == data["catalogVersion"]
        for group in goal["groups"]:
            for row in group["entries"]:
                assert row["unit"] == stats[row["statId"]]["unit"]
    if "probability" not in example:
        continue
    status = example["probability"]["status"]
    if status in ("UNKNOWN", "UNSUPPORTED"):
        assert example["comparisons"] == [] and example["totalSequences"] is None
    for comparison in example["comparisons"]:
        lower, upper, failure, unresolved = [comparison[k] for k in
                                             ("successLower", "successUpper", "failureProbability", "unresolvedProbability")]
        assert all(math.isfinite(n) and 0 <= n <= 1 for n in (lower, upper, failure, unresolved))
        assert math.isclose(lower + failure + unresolved, 1)
        assert math.isclose(upper, lower + unresolved)
        assert not comparison["complete"] or unresolved == 0
    assert not example["rankingCertified"]

print(f"PASS: {len(data['cases'])} evaluation fixtures; API shapes and probability mass checked.")
print(f"PASS: {len(data['validationCases'])} validation fixture vectors checked by contract oracle.")
