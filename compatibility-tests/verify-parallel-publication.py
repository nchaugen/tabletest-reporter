"""Check the parallel fixture strictly, despite other fixtures' intentional failures."""

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def require(condition, message):
    if not condition:
        raise SystemExit(f"ERROR: Parallel publication: {message}")


def verify(yaml_root, results_root):
    result_file = results_root / "TEST-com.example.ParallelTableTestsTest.xml"
    require(result_file.is_file(), f"missing test results: {result_file}")
    suite = ET.parse(result_file).getroot()
    cases = suite.findall("testcase")
    require(len(cases) == 4, f"expected four invocations, found {len(cases)}")
    require(
        all(case.find(status) is None for case in cases for status in ("failure", "error", "skipped"))
        and all(suite.get(status, "0") == "0" for status in ("failures", "errors", "skipped")),
        f"all four invocations must pass; see {result_file}",
    )

    indexes = list(yaml_root.rglob("TABLETEST-parallel-tables.yaml"))
    require(len(indexes) == 1, f"expected one class index, found {len(indexes)}")
    class_index = indexes[0]
    expected_tables = {
        "first-table": ("firstTable", ("First row", "Second row")),
        "second-table": ("secondTable", ("Third row", "Fourth row")),
    }
    expected_files = {"TABLETEST-parallel-tables.yaml"} | {
        f"TABLETEST-{slug}.yaml" for slug in expected_tables
    }
    actual_files = {path.name for path in class_index.parent.glob("TABLETEST-*.yaml")}
    require(actual_files == expected_files, f"expected exactly three YAML files, found {sorted(actual_files)}")
    index_text = class_index.read_text(encoding="utf-8")
    require(
        sorted(re.findall(r'"methodName": "([^"]+)"', index_text)) == ["firstTable", "secondTable"],
        "class index must list each method exactly once",
    )
    require(
        sorted(re.findall(r'"path": "([^"]+)"', index_text))
        == ["TABLETEST-first-table.yaml", "TABLETEST-second-table.yaml"],
        "class index must reference both table files exactly once",
    )

    for slug, (method, scenarios) in expected_tables.items():
        text = (class_index.parent / f"TABLETEST-{slug}.yaml").read_text(encoding="utf-8")
        require(text.startswith(f'"methodName": "{method}"\n'), f"wrong method attribution in {slug}")
        require(text.count('"rowResults":\n') == 1, f"missing or repeated rowResults in {slug}")
        table, results = text.split('"rowResults":\n')
        # Compare complete serialised result entries; accept either completion order.
        actual = re.findall(r'(?m)^- "rowIndex":.*(?:\n  .*)*', results)
        expected = [
            f'- "rowIndex": !!int "{index}"\n'
            '  "passed": !!bool "true"\n'
            f'  "displayName": "[{index}] {scenario}"'
            for index, scenario in enumerate(scenarios, start=1)
        ]
        require(sorted(actual) == sorted(expected), f"wrong or duplicate row results in {slug}: {actual}")
        require(table.count('- "passed"') == 6, f"all six cells must have the passed role in {slug}")
        require('"failed"' not in table, f"unexpected failed row role in {slug}")

    print("Verified parallel publication: four passing invocations, two tables and one class index")


if __name__ == "__main__":
    verify(Path(sys.argv[1]), Path(sys.argv[2]))
