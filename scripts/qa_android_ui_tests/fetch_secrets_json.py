#!/usr/bin/env python3
"""Fetch all 1Password items in a vault and write a runtime secrets.json file."""

import json
import os
import subprocess
import sys

vault = os.environ.get("OP_VAULT", "Test Automation")
out_path = os.environ.get("SECRETS_JSON_PATH") or "secrets.json"


def run_op(cmd):
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        sys.stderr.write(result.stderr or result.stdout or "op command failed\n")
        sys.exit(result.returncode)
    return result.stdout


list_out = run_op(["op", "item", "list", "--vault", vault, "--format", "json"])
try:
    items = json.loads(list_out)
except Exception as exc:
    sys.stderr.write(f"Failed to parse op item list output: {exc}\n")
    sys.exit(1)

if not isinstance(items, list):
    sys.stderr.write("Unexpected op item list output format\n")
    sys.exit(1)

combined = {}
for item in items:
    item_id = item.get("id")
    if not item_id:
        continue
    out = run_op(["op", "item", "get", item_id, "--vault", vault, "--format", "json"])
    data = json.loads(out)
    fields_list = data.get("fields") or []
    fields_map = {}
    for idx, field in enumerate(fields_list):
        label = field.get("label")
        if not label:
            continue
        # Preserve duplicate labels by appending an index suffix.
        key = label if label not in fields_map else f"{label}_{idx}"
        fields_map[key] = {"type": field.get("type"), "value": field.get("value")}
    data["fields"] = fields_map
    title = data.get("title") or item.get("title") or item_id
    combined[title] = data

# Write output to the temporary runtime file path used by the test run.
with open(out_path, "w", encoding="utf-8") as handle:
    json.dump(combined, handle, ensure_ascii=True, indent=2)
