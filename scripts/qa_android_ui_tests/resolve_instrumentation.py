#!/usr/bin/env python3
"""Resolve one Android instrumentation component for an exact target app."""

from __future__ import annotations

import os
import re


INSTRUMENTATION_PATTERN = re.compile(
    r"^instrumentation:(?P<component>\S+) \(target=(?P<target>[^)]+)\)$"
)


class InstrumentationResolutionError(ValueError):
    """Raised when instrumentation cannot be resolved unambiguously."""


def parse_instrumentations(raw_output: str) -> list[tuple[str, str]]:
    parsed = []
    for raw_line in raw_output.splitlines():
        match = INSTRUMENTATION_PATTERN.fullmatch(raw_line.strip())
        if match:
            parsed.append((match.group("component"), match.group("target")))
    return parsed


def resolve_instrumentation(raw_output: str, target_app_id: str) -> str:
    target = target_app_id.strip()
    if not target:
        raise InstrumentationResolutionError("TARGET_APP_ID is empty")

    candidates = [
        component
        for component, instrumentation_target in parse_instrumentations(raw_output)
        if instrumentation_target == target
    ]
    preferred = [component for component in candidates if component.endswith(".TaggedTestRunner")]

    if len(preferred) == 1:
        return preferred[0]
    if len(preferred) > 1:
        raise InstrumentationResolutionError(
            f"Multiple TaggedTestRunner components target '{target}': {', '.join(preferred)}"
        )
    if len(candidates) == 1:
        return candidates[0]
    if not candidates:
        raise InstrumentationResolutionError(f"No instrumentation targets '{target}'")
    raise InstrumentationResolutionError(
        f"Multiple instrumentation components target '{target}': {', '.join(candidates)}"
    )


def main() -> None:
    try:
        component = resolve_instrumentation(
            os.environ.get("INSTRUMENTATION_LIST", ""),
            os.environ.get("TARGET_APP_ID", ""),
        )
    except InstrumentationResolutionError as error:
        raise SystemExit(f"ERROR: {error}") from error
    print(component)


if __name__ == "__main__":
    main()
