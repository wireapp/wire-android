#!/usr/bin/env python3
"""Resolve a Test Service HTTP origin without contacting the service."""

import ipaddress
import os
import re
from urllib.parse import urlsplit


def validate_url(value: str) -> str:
    if not isinstance(value, str) or any(ord(c) < 32 or ord(c) == 127 for c in value):
        raise ValueError("Test Service URL must be a single-line string.")
    value = value.strip()
    # Accept origins only: no credentials, query, fragment or path prefix.
    # This also keeps Actions outputs and summaries free of injected syntax.
    if not re.fullmatch(r"https?://[A-Za-z0-9.\-\[\]:]+/?", value):
        raise ValueError("Test Service URL must be an http(s) origin, optionally with a port.")
    try:
        parsed = urlsplit(value)
        host = parsed.hostname or ""
        port = parsed.port
        if not host or parsed.netloc.endswith(":") or port == 0:
            raise ValueError()
        if ":" in host:
            ipaddress.IPv6Address(host)
        elif not all(
            re.fullmatch(r"[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?", label)
            for label in host.rstrip(".").split(".")
        ):
            raise ValueError()
    except ValueError:
        raise ValueError("Test Service URL has an invalid host or port.") from None
    return value.rstrip("/")


def resolve_url(override: str, fallback: str, *, deflake: bool = False) -> str:
    # Reject control characters even around an otherwise valid URL.
    if any(ord(c) < 32 or ord(c) == 127 for c in override):
        raise ValueError("Test Service URL must be a single-line string.")
    selected = override if override.strip() else fallback
    if not selected:
        if deflake:
            raise ValueError("Source run has no Test Service URL. Set testServiceUrl explicitly for this legacy artifact.")
        raise ValueError("Set testServiceUrl or the TEST_SERVICE_URL GitHub Actions variable.")
    return validate_url(selected)


if __name__ == "__main__":
    try:
        url = resolve_url(os.environ.get("TEST_SERVICE_URL_INPUT", ""), os.environ.get("TEST_SERVICE_URL_DEFAULT", ""))
    except ValueError as error:
        raise SystemExit(f"ERROR: {error}") from None
    with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
        output.write(f"testServiceUrl={url}\n")
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as summary:
            summary.write(f"- Test Service URL: `{url}`\n")
