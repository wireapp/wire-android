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
