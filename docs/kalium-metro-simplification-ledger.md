# Metro wiring simplification

Requested on 2026-10-20 after the user identified the volume and arbitrary numbering of the handwritten bindings. This work reduces construction boilerplate while retaining ADR 0017's lifetimes, facade compatibility, resource ownership and startup requirements.

## Starting checkpoint

- Android: `13e865b3ab93e42c3c0ad6f04cf91a4412864c68`.
- Kalium: `a2d3ff4a08109dceba9f05747e14540404d33e9d`.
- Ten numbered `UserSessionBindings` files: 6,302 lines, 310 provider recipes, 1,013 `Provider<` occurrences.
- Source verification from previous checkpoints does not certify the simplification. Before/after performance measurements and the Swift receiving runtime check remain incomplete; no performance improvement is claimed.

## Milestones

- [x] Inspect actual recipes and tagged Metro 1.4.5 source.
- [x] Begin implementation: constructor injection for three internal self-conversation providers; include four narrow owner input APIs directly, removing the resource forwarding interface.
- [x] Bounded auxiliary implementation: 23 unscoped constructor-injection conversions; cached/default/shared-context recipes kept explicit.
- [x] Compile the integrated patch and resolve compiler findings.
- [x] Replace eligible root recipes with constructor injection and qualified aliases.
- [x] Remove arbitrary numbered binding groups and organize remaining exceptions by purpose.
- [ ] Independently review the exact committed patch for identity/concurrency and platform/startup/cleanup, fix findings, then obtain closure review.
- [ ] Verify affected JVM/Android/Native graphs, behavior, style and governed API.
- [ ] Update the implementation PRs, binding inventory and ADR assessment with exact reduction and remaining verification limits.

## Review and task ledger

| Task | Ownership / purpose | Status / evidence |
| --- | --- | --- |
| `node:delegated-task:command%3Amcp%3A312fb259-c842-4621-816a-387f348b1c3a%3Adelegate-task%3Ametro-simplify-architecture-identity-r14-20261020` | Independent architecture and identity audit, read-only, inherited model | Completed; three actionable findings and implementation responses recorded below. Closure on the final patch remains required. |
| `node:delegated-task:command%3Amcp%3A312fb259-c842-4621-816a-387f348b1c3a%3Adelegate-task%3Ametro-simplify-auxiliary-facades-layer1-20261020` | Four auxiliary binding families and eligible leaf constructors only; no builds or index writes | Completed and writers paused. 23 providers converted in 24 files, 29 explicit exceptions. Ignored artifact: `kalium/build/metro-verification/staging/simplification-auxiliary-layer1/HANDOFF.txt`. Static source checks only. |

## Decisions and open checks

- Constructor annotations preserve parameter order, types, visibility, defaults and bodies. Qualifiers retain the original dependency keys.
- Metro 1.4.5 rejects scopes on `@Binds` declarations (`BindingContainerCallableChecker.kt`, tag `1.4.5`, commit `0a5a42c5d7a700ec07ad545f674077576b318f4f`). Qualified caches remain on short scoped provider adapters; injected implementations stay unscoped. Annotation targets alone are insufficient evidence.
- `@Includes` imports resource accessors only, with the same qualified keys. Raw function and Kotlin `Lazy` values remain opaque wrappers; getters stay deferred. It does not import parent-built consumer use cases.
- No automatic conversion of omitted defaults, repeated resource reads, callbacks, external/provider caches, or multiple constructor recipes with different keys/lifetimes.
- One parent-owned Gradle build at a time. All source writers pause while the compiled/reviewed snapshot is frozen.

### Architecture round 14 response

The reviewer found three concrete hazards; all are treated as correctness findings, not style feedback.

1. Scoped `@Binds` is rejected by Metro. The initial alternative of scoping implementations was also withdrawn. All injected implementations remain unscoped. The original 22 qualified API caches use short `@Provides @SingleIn(UserLifetime)` adapters; implicit implementation requests remain fresh.
2. Thirteen superficially simple recipes pass concrete dependencies into interface-typed parameters. Those recipes remain explicit, including the six property data sources and the MLS conversation wrapper. No additional bridge aliases were added merely to force conversion.
3. Named arguments can resolve in a different order from constructor parameters. The stale-epoch and ACME certificate recipes remain explicit. No constructor parameters or defaults were reordered.

Implementation now converts 125 exact-type, full-argument root recipes (103 fresh aliases and 22 qualified cache adapters), plus the two existing typed factory handles. Their real deferred `Provider` parameters and `create` bodies are preserved. Ten numeric groups are replaced by six descriptive groups. A one-off source audit confirms all 310 distinct original qualified construction keys remain and 143 changed leaf files preserve their original non-import tokens after injection metadata is removed. This is static evidence; compilation and runtime verification are still pending.

Added focused generated-graph checks for all three self-conversation caches, separate graphs, distinct MLS/Proteus repository captures, fresh unqualified implementations, and concurrent first resolution of both repository-capturing caches. Existing repeated resource-read assertions remain unchanged.

The first quiet Gradle compile returned without diagnostics, but the process tool did not retain a conclusive exit marker. It is not counted as a passed check. Subsequent build commands will write an explicit exit receipt.

The internal API pass removed 108 unreferenced getters (279 to 171), retaining all binding definitions and public facade accessors. Its independent source/reference audit is pending final handoff; no build result is attributed to that task.

### Frozen source review round 15

SDK base `a2d3ff4a08109dceba9f05747e14540404d33e9d`, staged tree `4a6d2d81cc8758222168ed173cb2d8103a880331`; Android remains `13e865b3ab93e42c3c0ad6f04cf91a4412864c68`. All code writers are paused.

- Identity review: `node:delegated-task:command%3Amcp%3A312fb259-c842-4621-816a-387f348b1c3a%3Adelegate-task%3Ametro-simplify-identity-r15-4a6d2d8-20261020`.
- Platform/runtime review: `node:delegated-task:command%3Amcp%3A312fb259-c842-4621-816a-387f348b1c3a%3Adelegate-task%3Ametro-simplify-platform-r15-4a6d2d8-20261020`.
- Internal entrypoint author `node:delegated-task:command%3Amcp%3A312fb259-c842-4621-816a-387f348b1c3a%3Adelegate-task%3Ametro-simplify-unused-internal-entrypoints-20261020` completed, writes paused. Exact evidence in ignored `simplification-entrypoints/HANDOFF.txt` and manifest.

Focused JVM attempt 1 failed compilation because grouping omitted the private API migration version constant; its original value and private visibility were restored. Attempt 2 on this frozen tree failed compilation at six constructor parameters in four classes: those dependencies use legacy message/client/conversation/channel capture qualifiers rather than root keys. A complete audit of all 125 converted recipes identifies exactly those six mismatches. A guarded correction is prepared in ignored `simplification-root/qualifier-fix-manifest.json`; it will be applied after this frozen review round. Neither attempt ran tests. Explicit command/exit receipts are in `kalium/build/metro-verification/checkpoints/simplification-layer2/`.

Additional static checks confirm both typed factory `create` bodies are byte-identical and all 41 included owner input keys match their former forwarding recipes. These checks do not replace compilation or behavior tests.

Round 15 platform review completed on the exact frozen tree. Its P1 finding independently confirms the same six cross-family qualifier mismatches identified by compilation; the correction remains prepared and unapplied while the identity reviewer finishes. R14 findings are resolved in source. Independent checks retain all 310 root output keys, all 61 cache keys, 183 token-equivalent explicit recipes, all 41 input accessor keys/types, all 143 leaf implementations' non-injection tokens and internal constructor visibility. Startup, shutdown, Apple leases/preparation and restricted module dependencies are unchanged. Runtime/platform/API/performance verification remains open; source review is not a check pass.

Round 15 identity review independently reported exactly the same six missing qualifier keys and no additional source counterexample. Both reviewers verified the freeze on exit. The six corrections are now applied: each constructor requests its original legacy capture key, with the corresponding import. The guarded patch was independently checked to contain only those six annotation substitutions and import changes. Attempt 3 repeated the known compilation failure after an artifact hash guard refused the unsorted preview manifest; no source was applied by that failed guard. The manifest was corrected before application. Attempt 4 is running against the corrected tree; no test pass is yet claimed.

### Verification and public API correction

Focused JVM run 4 passed on staged tree `9ddfc767c5a274ef7ad3241a48e72cf65308eaed`: 79 actual tests in 14 DI/concurrency suites, zero failures/errors/skips. XML and explicit exit receipt are archived under `simplification-layer2/jvm-focused-r4-xml` and `jvm-focused-r4-exit.json`.

Style attempt 1 found 45 formatting/import issues; seven files were corrected without changing non-import tokens. Full standalone detekt then passed. The subsequent governed ABI check correctly rejected 120 added generated factory lines in each of the JVM and Android references: Metro generates public factory/companion types for eight public SDK classes even when their constructors remain internal. Those eight leaf recipes are now explicit again, and their source files are restored exactly to the baseline. No API reference was changed to accept the incidental expansion. They are narrow individual recipes, not broad feature constructors. Five internal declarations also retain conventional KDoc-before-annotation ordering.

Current construction conversion count is 121 ordinary root recipes plus the two existing typed factory handles, with 101 unscoped aliases and the original 22 scoped qualified cache adapters. The eight public exceptions comprise four root and four auxiliary recipes; 19 auxiliary conversions remain. All 310 root keys, 61 caches and 41 included owner inputs remain. Verification is rerunning on this corrected source before the final closure checkpoint.

### Committed simplification checkpoint

SDK commit `5ed5bc34ffd44eda693fce1331072e96dd7517ba` has tree `9a9bec6612d60d6825e4b0a561459387621f4326`. The final change has 160 files, 5,040 insertions and 6,546 deletions: 1,506 net lines removed. Six groups have 4,673 lines versus the former 6,302. All 310 distinct construction keys remain; 135 changed leaf files preserve original non-injection tokens. The constructor group contains 101 fresh aliases and 22 scoped API adapters, with zero `Provider<` constructor-list duplication there. The eight public exceptions preserve existing governed ABI; no API reference was regenerated.

`style-abi-android-r4-exit.json` records exit 0 for full standalone detekt, `:logic:checkKotlinAbi` and `:logic:compileAndroidMain` on this exact tree. Earlier focused execution records 79 tests, no failures/errors/skips. The six formatting issues following the public-class rollback were corrected before the final successful checks. Broader current-source execution and new exact-commit closure reviews are next; performance and Swift receiving runtime evidence remain incomplete.

### Round 16 closure and current execution

Both reviewers inspected SDK `a2d3ff4a08109dceba9f05747e14540404d33e9d..5ed5bc34ffd44eda693fce1331072e96dd7517ba` (tree `9a9bec6612d60d6825e4b0a561459387621f4326`) and Android `13e865b3ab93e42c3c0ad6f04cf91a4412864c68..8403229cea9610bbd2e7450076a8ede1e9046fa7`. Source was frozen and clean throughout. Both found no actionable source objections and closed the six original capture-qualifier findings. They independently preserved 310 root keys, 61 caches, unscoped implementation requests, constructor argument order, 41 included inputs, public-class rollbacks, platform composition and lifecycle ownership. Neither ran builds or claimed runtime closure.

- Identity: `node:delegated-task:command%3Amcp%3A312fb259-c842-4621-816a-387f348b1c3a%3Adelegate-task%3Ametro-simplify-identity-r16-5ed5bc3-20261020`.
- Platform: `node:delegated-task:command%3Amcp%3A312fb259-c842-4621-816a-387f348b1c3a%3Adelegate-task%3Ametro-simplify-platform-r16-5ed5bc3-20261020`.
- SDK execution: `node:delegated-task:command%3Amcp%3A312fb259-c842-4621-816a-387f348b1c3a%3Adelegate-task%3Ametro-simplify-sdk-validation-5ed5bc3-20261020`.
- Android execution: `node:delegated-task:command%3Amcp%3A312fb259-c842-4621-816a-387f348b1c3a%3Adelegate-task%3Ametro-simplify-android-validation-8403229-20261020`.

Current full JVM execution has 4,262 tests across 669 freshly written suites: 4,250 passed, zero failures/errors and the same twelve prior skips. The two additional cases cover qualified injected caches and concurrent first resolution; no prior cases disappeared. All three Apple production compile tasks completed successfully with up-to-date outputs. The affected Native executable was freshly linked, but its 162-test selection failed one calling lifecycle assertion. Framework linking and Swift API verification were not run after that failure. Receipts, actual JVM XML and Native binary results are under ignored `kalium/build/metro-verification/checkpoints/simplification-5ed5bc3/`; disabled Native XML is not counted as a pass.

Android at `8403229cea9610bbd2e7450076a8ede1e9046fa7` with SDK `5ed5bc34ffd44eda693fce1331072e96dd7517ba` executed app production/test compilation, app detekt and 31 fresh selected tests, all passing with no skips/errors. All ten existing stability checks executed and passed without baseline regeneration. Evidence is under `simplification-android-8403229/`. Both executors released their exclusive build leases with unchanged clean checkouts.

### Native calling runtime objection remains open

Read-only diagnosis: `node:delegated-task:command%3Amcp%3A312fb259-c842-4621-816a-387f348b1c3a%3Adelegate-task%3Ametro-native-call-failure-diagnosis-5ed5bc3-20261020`. Its terminal tool summary was truncated; the complete report and provenance were recovered from ignored `staging/native-call-failure-5ed5bc3/report.md` and `evidence.json`.

The original one-to-one queued-reject scenario recorded REJECTED, then observed INCOMING at the subsequent-state assertion on test line 188. Tagged AVS 10.5.33 source permits a ringing notification's media-manager round trip to finish after rejection. A direct five-iteration in-process diagnostic run passed twice, then crashed with SIGSEGV during iteration three. The matching macOS crash report shows native media-manager mute notification overlapping call destruction. Five separate-process diagnostic invocations passed; this does not erase either original counterexample.

The test, manager, replay, repository, bridge, dispatchers and AVS plugin/pin have identical blobs at the ADR base and current source. The test constructs the manager directly, without a Metro graph. No simplification source mechanism was found, but baseline runtime reproduction and the effect of the compiler/toolchain upgrade are unverified. These failures are not classified as environmental. Two source-supported external AVS candidates are staged only; no native dependency, shutdown behavior or assertion was changed. A validated AVS artifact and further controlled runtime evidence would be needed for those fixes.

### Alternate-host Swift retry

SDK follow-up `24ff8cb144561dfd78407eebf6b189c7b7f952dd` changes only the optional receiving CI job's timeout from 75 to 120 minutes and its link step from 45 to 90. The prior run timed out before the verifier started. The job's platform, commands, permissions and receiving assertions remain unchanged; ordinary jobs retain their existing behavior. Diff inspection and `git diff --check` passed. Dispatch and actual receiving/handoff results remain pending at this checkpoint. Production review evidence continues to refer to `5ed5bc34ffd44eda693fce1331072e96dd7517ba`.

### Native/Swift follow-up and measurement handoff

Task `node:delegated-task:command%3Amcp%3A312fb259-c842-4621-816a-387f348b1c3a%3Adelegate-task%3Ametro-native-swift-followup-24ff8cb-20261020` completed on SDK `24ff8cb144561dfd78407eebf6b189c7b7f952dd` / Android `12e7ce7812e15dcd1e6fb1b33ff366cc1fd7b37d`, with clean unchanged heads and an explicit build-lease release. Exactly one invocation of the unchanged current binary with the original twenty positive selection patterns passed 162/162 tests. A fresh macOS framework link executed successfully, followed by the sampled Swift SDK API typecheck; binary/header hashes and actual commands are in ignored `checkpoints/native-followup-24ff8cb/`.

A new exact ADR-base archive retained Kotlin 2.3.20, Mokkery 3.3.0, SKIE 0.10.12 and AVS 10.5.33, with no archive-only adaptations. Its Native test link passed; the calling target passed once and then 5/5 iterations in one repeat-process diagnostic. All 6,653 original source entries remained unchanged. Neither original current-source counterexample reproduced in these controls. The earlier assertion and SIGSEGV remain unresolved; later passes establish neither a fix nor baseline/dependency attribution.

Alternate-host receiving run [39503444825](https://github.com/wireapp/kalium/actions/runs/39503444825) was dispatched on `24ff8cb144561dfd78407eebf6b189c7b7f952dd` and is monitored by T3's PR watcher. Actual receiving/handoff and measurement cohorts remain pending. The separate Jira-description check failed because the PR title has no Jira issue key; the ADR/foundation PRs supply none. The controller requested the correct key without changing repository policy. This metadata dependency does not stop implementation verification.

### R7 measurement gate and R8 correction

Execution task `node:delegated-task:command%3Amcp%3A312fb259-c842-4621-816a-387f348b1c3a%3Adelegate-task%3Ametro-measurements-simplified-r7-0f0ea36-20261021` ran the actual baseline gate at Android `0f0ea36642e7cf1360082b8cd2f738a482c9561d` / SDK `24ff8cb144561dfd78407eebf6b189c7b7f952dd`. Seven fresh tests executed: five passed, two failed strict synthetic exception-identity assertions, with zero skips. The real two-cycle network-observer SDK adapter passed on the baseline. Assertions following the failed identity checks were not reached. Current regressions and all eight cohorts remained unrun; no measurement rows or accepted comparison exist. Finally cleanup matched the recorded fixture hash, removed only the current injection and left both repositories clean. R7 receipts, XML, source manifest, host snapshots and failed result remain frozen under `measurements/20261020/generated-v2/final-simplified-r7/`.

Bounded author `node:delegated-task:command%3Amcp%3A312fb259-c842-4621-816a-387f348b1c3a%3Adelegate-task%3Ametro-measurement-sentinel-fix-r8-20261021` changed only the two fixture templates and their installed manifest hashes, then paused writes. Each template uses a private test-only `CopyableThrowable` sentinel whose `createCopy()` returns null, preserving strict `assertSame`, cleanup flags, deadlines and all helper/adapter bodies. Cached coroutines 1.10.2 primary sources explicitly support this opt-out; R7 did not record recovery flags or caught cause identity, so its precise runtime mechanism remains inferred. All 23 installed source hashes match. Both fixture/Groovy syntax parses pass. A checkpoint-only, included-build-guarded init script executed the existing app detekt task against exactly the two fixture files and passed; its first unguarded init attempt failed configuration and remains preserved. Runtime acceptance requires a new complete gate, not a relabelled R7 result. Author handoff is in Android-root ignored `build/metro-verification/staging/measurement-sentinel-fix-r8/HANDOFF.txt`.

### JS CI lock correction

[JVM/JS CI 39503426395](https://github.com/wireapp/kalium/actions/runs/39503426395) passed its JVM phase but failed JS before testing at `kotlinStoreYarnLock`. The archived JVM XML has 6,459 cases: 6,412 passed, 47 skipped, zero failures/errors. SDK commit `a1255afadafff1cbb627622ea0de56a2512312ec` regenerates only `kotlin-js-store/yarn.lock` using the supported `kotlinUpgradeYarnLock` task. The generated changes cover Kotlin web helpers 3.3.0, Mocha 11.7.6, TypeScript 6.0.3 and three unchanged-version CJS alias splits; no package version was edited manually. Subsequent full `jsTest` exited zero with an unchanged lock: 131 fresh XML suites, 1,194 passed cases, 48 skipped, zero failures/errors. Existing disabled test tasks remain disabled. Actual commands, exits, diffs and XML are under ignored `checkpoints/js-lock-refresh-r1/` and `js-lock-verification-r1/`. New source review and a fresh R8 measurement attempt follow this checkpoint; no savings are claimed.

### Round 17 source closure and R8 runtime rejection

Fresh reviewers independently inspected Android `c975988d556c1d372c296dd84a34f705faf27ff2` (tree `bebc3b15d42e8729d764655faeeda6d26aa5cc94`) and SDK `a1255afadafff1cbb627622ea0de56a2512312ec` (tree `ba06429ad0bf776a6cefcb7f82f1e3bf9003e958`). Both found no actionable source objections. They verified the sentinel opt-out preserves strict identity assertions, all 23 installed source hashes, the inverse fixture diff, Kotlin's generated lock dependencies and the actual 1,194 JS passes/48 skips. Source closure does not certify measurement acceptance, receiving behavior or the historical Native failures.

- Identity review: `node:delegated-task:command%3Amcp%3A312fb259-c842-4621-816a-387f348b1c3a%3Adelegate-task%3Ametro-r8-sentinel-identity-r17-c975988-20261021`.
- Platform/tooling review: `node:delegated-task:command%3Amcp%3A312fb259-c842-4621-816a-387f348b1c3a%3Adelegate-task%3Ametro-r8-js-platform-r17-a1255af-20261021`.
- Baseline gate execution: `node:delegated-task:command%3Amcp%3A312fb259-c842-4621-816a-387f348b1c3a%3Adelegate-task%3Ametro-measurements-r8-baseline-gate-c975988-20261021`.
- Current gate/cohorts execution follow-up: `node:delegated-task:command%3Amcp%3A312fb259-c842-4621-816a-387f348b1c3a%3Athread-send%3Ametro-measurements-r8-current-cohorts-20261021`.

R8 executed both real seven-case cleanup gates: all fourteen cases passed with zero failures/errors/skips. All eight default cohorts and 24 Gradle forks also executed and exited zero. However, the summarizer exited 1 because cold-session spans lacked a UserSessionScope constructor entry. Every one of the twelve instrumented forks had zero cumulative selected constructor ENTRY callbacks and zero selected RETURN identity observations, despite nonempty registered coverage. Thus zero cohorts or comparisons are accepted, including the partial timing summaries; no savings are claimed. Standalone agent smoke success did not establish callback delivery through Robolectric. Bootstrap/sandbox loader separation and instrumentation rewriting remain hypotheses pending a real positive control.

All commands, fresh XML, metrics, source guards and the rejected report remain frozen under ignored `measurements/20261020/generated-v2/final-simplified-r8/`. Its `PHASE2-HANDOFF.txt`, `phase2-summary.json` and `phase2-constructor-callback-diagnostic.json` distinguish successful execution from failed acceptance. Guarded cleanup exited zero, removed the current injected fixture and preserved all original baseline/archive inputs. Both repositories remained clean at the reviewed commits; the execution worker explicitly released its build lease. Bounded R9 author `node:delegated-task:command%3Amcp%3A312fb259-c842-4621-816a-387f348b1c3a%3Adelegate-task%3Ametro-robolectric-constructor-delivery-fix-r9-20261021` now owns only measurement tooling and a minimal actual callback gate. No validator, lifecycle assertion or production SDK behavior may be weakened to accept the rejected data.

### Alternate-host receiving reached the verifier

[Receiving run 39503444825](https://github.com/wireapp/kalium/actions/runs/39503444825) on SDK `24ff8cb144561dfd78407eebf6b189c7b7f952dd` completed framework/fixture linking successfully, then failed the real receiving verifier. No receiving or handoff pass is claimed. Artifact audit `node:delegated-task:command%3Amcp%3A312fb259-c842-4621-816a-387f348b1c3a%3Adelegate-task%3Ametro-swift-receiving-ci-failure-audit-39503444825-20261021` is read-only and must identify the actual error and reached assertions before a correction. The user requested another host; no privileged local listener is required from them.

At SDK head `a1255afadafff1cbb627622ea0de56a2512312ec`, only CLA and Semgrep checks are reported. The current-head Actions inventory is empty; the cause is unestablished. Earlier `24ff8cb` test results and local JS verification are recorded separately and do not constitute current-head CI completion. Ordinary iOS run 39503426553 remains in its test-binary build at the latest inspection. The correct Jira key is still awaiting the user's reply; no ticket was invented and no repository check was bypassed.

The receiving artifact audit completed. Run 39503444825 linked in 35m 27s, then trapped at `Receive.swift:70`, “Missing eligible text for proteus.” The Proteus status/no-calls checks passed; text preview, replay, MLS and app handoff were not reached. The retained log lacks both predicate values and HTTP paths, so exact historical causality is unresolved. Primary Ktor 3.5.0 source plus the unchanged exporter/helper derives `/proteus//await`, whereas the fixture server accepts `/proteus/await`. These source blobs are identical to the ADR base; this is a preexisting fixture mismatch candidate, not established migration attribution. The downloaded nine-file artifact matches GitHub's SHA-256 digest `5dfd4f69ed07a11086d8033c0c00c8d80416cd44af5e84576ba6a7f2a444a6d6`.

The audit stages only a fixture WebSocket-base trailing-slash correction and diagnostic additions. All 21 Swift precondition call sites and 104 Python assert ASTs remain unchanged. Combined apply checks and Python parse pass; Swift compilation/runtime remain unrun. The parent will apply and review the actual patch after the current minimal measurement control releases its SDK snapshot freeze, then dispatch the existing alternate-host verifier. No production URL helper or receiving policy is changed. The complete report, precise assertion boundaries, source identities and unapplied patches are under ignored `checkpoints/swift-ci-39503444825/`.

### R9 real-sandbox callback correction and Swift fixture checkpoint

R9 author completed and released the source/build lease. A focused Robolectric 4.16.1 reproduction captures actual constructor bytecode containing both direct callbacks and proves a second sandbox-defined Probe: bootstrap installed=true/TOTAL=0 versus sandbox installed=false/TOTAL=2. This establishes the reproduced callback split; no callback-removal cause is claimed. Both templates now extend the ordinary runner configuration with `doNotAcquirePackage("com.wire.metro.measure.")`, delegating only measurement scaffolding to the parent/bootstrap while leaving SDK/test classes sandbox-owned. Constructor hot-path callbacks remain direct static calls.

Each fork checks callback delivery before SDK, SQL, account or mock-port setup. The real sandbox control requires four ENTRY callbacks from two constructor overloads and the two exact object references in both RETURN identity sets; it uses the existing bounded identity-window teardown. Timing uses the same loader configuration with no constructor agent and requires zero entries/null identity data. Control rows are separate from SDK distributions. One control-only fork per variant now precedes regressions and cohorts; the report refuses control-only runs as measurements. No cold-session, lifecycle, archive-source or identity acceptance assertion was relaxed.

Actual current-SDK controls pass: constructor 4 ENTRY/2 exact identities, uninstrumented timing, and baseline-template compatibility on current SDK. Missing-ENTRY and deliberately missing-RETURN controls both fail as required before SDK setup. The old-loader diagnostic passes by proving the split. Actual fresh XML, commands, bytecode, primary Robolectric source/JAR hashes and cleanup receipts are in Android-root ignored `build/metro-verification/staging/measurement-callback-r9/`. Targeted fixture detekt executed and passed; ASM smoke, report rejection, all 23 archive regressions and syntax/package checks pass. Both existing cleanup and seven regression bodies remain byte-identical. This is not pinned ADR-baseline runtime verification, a cold-session pass, a cohort comparison or a savings claim. R8 artifacts remain hash-verified unchanged. Source hashes for the thirteen modified packaged files are updated.

SDK `b36888728b35d6fc397a00ce2b466c21cb8274e5` (tree `68b52ce211bc05f44ac416b2a1f841e8d5f400e6`) applies the fixture WebSocket-base trim and receiving diagnostics. The REST base, production URL helper, receiving policy and success assertions are unchanged. Parent verification independently confirms all 104 Python assert ASTs and 21 Swift precondition lines remain unchanged; removing diagnostics reconstructs original Swift bytes. Swift parse and diff whitespace checks pass. Kotlin/Swift typechecking and alternate-host receiving still need execution on this new checkpoint. Fresh independent reviews inspect the actual committed patches next; no whole-migration closure is claimed.
