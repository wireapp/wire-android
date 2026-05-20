# Baseline measurements

The `<date>-pre-profile/` directory is the fixed reference baseline, captured once during initial setup.
It measures the full user-facing flow (startup + login → conversation list visible) WITHOUT any baseline profile applied.

The checked-in directory layout is intentionally device-agnostic. Use a stable leaf such as
`debugAndroidTest/connected/device/` rather than the physical handset name reported by Gradle
(for example `SM-G973F - 12`). The benchmark JSON already contains the real device metadata under
`context.build`, so renaming the folder does not lose comparability data.

## Contents

Each directory contains:
- `com.wire.benchmark.test-benchmarkData.json` — metrics including `timeToInitialDisplayMs.median` (cold-start latency)
- Perfetto trace files (`.perfetto-trace`) — flame graphs for profiling
- Test output logs — summary data

## Comparison

All refresh PRs compare their post-profile measurements against this single pre-profile baseline.
The percentage improvement is reported in the PR comment (e.g., "12% faster on cold start").

Example baseline metric:
```json
{
  "metrics": {
    "timeToInitialDisplayMs": {
      "median": 850.5,
      "p95": 920.3
    }
  }
}
```

Post-profile measurements are ephemeral (not versioned) — they are used only to calculate the improvement percentage in each refresh PR.
