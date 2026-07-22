#!/usr/bin/env python3
"""Fetch the minimum 1Password fields required by Android E2E tests."""

import json
import os
import re
import subprocess
import sys

vault = os.environ.get("OP_VAULT", "Test Automation")
out_path = os.environ.get("SECRETS_JSON_PATH") or "secrets.json"


DEVICE_SECRET_SECTIONS = {
    "CALLINGSERVICE_BASIC_AUTH",
    "KEYCLOAK_QA_AUTOMATION",
    "SOCKS_PROXY_PASSWORD",
}
HOST_SECRET_SECTIONS = {"TESTINY_API_KEY_ANDROID"}
PASSWORD_FIELD = {"PASSWORD"}
BACKEND_FIELDS = {
    "ACMEDISCOVERYURL",
    "BACKENDURL",
    "BACKENDWEBSOCKET",
    "BASICAUTH",
    "BASICAUTHPASSWORD",
    "BASICAUTHUSERNAME",
    "DEEPLINK",
    "DOMAIN",
    "INBUCKETPASSWORD",
    "INBUCKETURL",
    "INBUCKETUSERNAME",
    "K8SNAMESPACE",
    "KEYCLOAKURL",
    "SOCKSPROXY",
    "WEBAPPURL",
}


def sanitize(value: str) -> str:
    """Match the BuildConfig key normalization used by testsSupport."""
    return re.sub(r"[.\-\s]+", "_", value).upper()


def allowed_fields_for_title(title: str) -> set[str] | None:
    section = sanitize(title)
    if section.startswith("BACKENDCONNECTION_"):
        return BACKEND_FIELDS
    if section in DEVICE_SECRET_SECTIONS or section in HOST_SECRET_SECTIONS:
        return PASSWORD_FIELD
    return None


def filter_item_fields(data: dict, allowed_fields: set[str]) -> dict:
    fields_list = data.get("fields") or []
    fields_map = {}
    for idx, field in enumerate(fields_list):
        label = field.get("label")
        if not isinstance(label, str) or sanitize(label) not in allowed_fields:
            continue
        key = label if label not in fields_map else f"{label}_{idx}"
        fields_map[key] = {"type": field.get("type"), "value": field.get("value")}

    filtered = {"id": data.get("id"), "title": data.get("title")}
    filtered["fields"] = fields_map
    return filtered


def run_op(cmd: list[str]) -> str:
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        sys.stderr.write(result.stderr or result.stdout or "op command failed\n")
        sys.exit(result.returncode)
    return result.stdout


def main() -> None:
    list_out = run_op(["op", "item", "list", "--vault", vault, "--format", "json"])
    try:
        items = json.loads(list_out)
    except json.JSONDecodeError as exc:
        raise SystemExit(f"Failed to parse op item list output: {exc.msg}") from exc

    if not isinstance(items, list):
        raise SystemExit("Unexpected op item list output format")

    combined = {}
    for item in items:
        item_id = item.get("id")
        item_title = item.get("title")
        if not isinstance(item_id, str) or not isinstance(item_title, str):
            continue

        allowed_fields = allowed_fields_for_title(item_title)
        if allowed_fields is None:
            continue

        out = run_op(["op", "item", "get", item_id, "--vault", vault, "--format", "json"])
        data = json.loads(out)
        section = sanitize(str(data.get("title") or item_title))
        if section in combined:
            raise SystemExit(f"Duplicate normalized 1Password item title: {section}")
        combined[section] = filter_item_fields(data, allowed_fields)

    if not any(section.startswith("BACKENDCONNECTION_") for section in combined):
        raise SystemExit("No BackendConnection items found in the configured 1Password vault")

    # Create the runtime secret file with restrictive permissions immediately;
    # chmod after a normal open leaves a short world-readable creation window.
    file_descriptor = os.open(out_path, os.O_WRONLY | os.O_CREAT | os.O_TRUNC, 0o600)
    with os.fdopen(file_descriptor, "w", encoding="utf-8") as handle:
        json.dump(combined, handle, ensure_ascii=True, indent=2)


if __name__ == "__main__":
    main()
