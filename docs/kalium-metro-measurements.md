# Kalium Metro construction measurements

Source under test: Android `ec721fadeb77965088ed6a8018bf1d864e2f57fc`, SDK `b36888728b35d6fc397a00ce2b466c21cb8274e5`. Baseline: Android `38bbefb59d0c858fbc97f2eab9cec345aabca21b`, SDK ADR base `92e54877ee318fe6c46aae3a3832e78b3b1adf8b`. Fresh R19 identity and platform reviewers independently recomputed all eight cohorts from raw rows and recounted the forty fresh executed cases on this exact source.

R9 accepted all eight cohorts and one complete comparison. All 24 measurement forks, two callback-control forks and fourteen cleanup regression cases passed: forty executed cases, zero failures/errors/skips. Source/archive guards and final current-fixture cleanup passed. R7/R8 evidence remains preserved; R8 was rejected and supplies no comparison.

Each baseline/current × startup-only/VM × timing/constructor cohort uses three fresh JVM forks, three warmup session cycles and seven measured cycles per fork. Each VM cycle creates a first ViewModel through a real store, looks up that retained instance, and creates ten new warm ViewModels in separate stores. Median first/cold/retained phases have 21 observations; warm creation has 210 correlated observations. The fixture uses real CoreLogic and SQLite with documented external AVS, WorkManager and HTTP substitutions.

## Observed VM-scenario medians

The timing pass has no constructor agent. Values below are wall milliseconds and caller-platform-thread `ThreadMXBean` heap-byte approximations; bytes on other threads are excluded.

| Phase | Baseline ms | Current ms | Baseline caller bytes | Current caller bytes |
| --- | ---: | ---: | ---: | ---: |
| Cold session get/create | 10.307750 | 10.887292 | 656,296 | 934,776 |
| First VM, new store | 0.037875 | 0.103250 | 1,528 | 7,016 |
| Warm VM, new store | 0.026042 | 0.111188 | 792 | 6,864 |
| Retained VM lookup | 0.003584 | 0.003875 | 64 | 64 |

The separate instrumented pass records selected constructor ENTRY callbacks and unique references reaching selected normal RETURN callbacks per window. These are different units; neither is a complete JVM allocation count. Delegation/superclass/failed construction can add entries; several selected returns can belong to one reference.

| Phase | ENTRY baseline/current | RETURN identities baseline/current |
| --- | ---: | ---: |
| Cold session get/create | 1,207 / 883 | 805 / 588 |
| First VM, new store | 6 / 13 | 3 / 11 |
| Warm VM, new store | 6 / 13 | 3 / 11 |
| Retained VM lookup | 0 / 0 | 0 / 0 |

The separate startup-only control records cold-session median wall time of 11.410250 / 12.227291 ms, ENTRY 1,207 / 886 and RETURN identities 805 / 590 (baseline/current). The fixed observation window is not a startup-complete or idle barrier; it cannot subtract asynchronous startup work from VM phases.

The current fixture records fewer selected cold-session identities and higher VM creation time/caller bytes. These results do not demonstrate an overall performance improvement. Baseline Kotlin 2.3.20 versus current 2.4.20 and other tool versions confound DI-only attribution. Warm observations share sessions/JVMs; no statistical-significance or device/process-cold claim is made. The ViewModel fixture covers the measured narrow consumer path, not every production ViewModel or the full app graph. Instrumented time is excluded from performance comparisons, and identity sets retain selected references only until each window closes.

## Evidence

Ignored local evidence: `kalium/build/metro-verification/measurements/20261020/generated-v2/final-simplified-r9/`. `measurement-report.json` links every cohort's raw metrics, per-session/per-fork distributions and actual XML; `accepted-phase-medians.txt` contains all phase medians/min/max/counts. `execution-binaries/` preserves the actual probe/agent bytes and hashes. `execution-result.json`, `preservation-validation.json` and `execution-receipts/` record commands, statuses, source guards and final cleanup. Allowed new generated compilation inputs are not content-attested; classpath paths/sizes/mtimes are captured, not a full dependency binary attestation.

Both independent R19 source/evidence reviews found no new actionable source issues. They validated 1,344 measured windows and preserved source/evidence hashes; the simplification ledger records their task IDs and scope. Native AVS crash attribution, current Swift receiving and Jira metadata remain separate open gates.
