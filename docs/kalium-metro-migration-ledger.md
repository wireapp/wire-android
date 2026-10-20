# Kalium Metro implementation ledger

Controller: this implementation thread. No merge or deployment is authorized.

## Bound snapshots

- Android start: `38bbefb59d0c858fbc97f2eab9cec345aabca21b`.
- Committed Android Kalium pin: `4fa20b0b28936143e93bf44ab2763933e4dc76c8` (not the implementation base).
- Investigated Kalium develop: `fe0e7a8dd95a3abc4da773d194dc1c777b34a4e9`.
- ADR stack base: `92e54877ee318fe6c46aae3a3832e78b3b1adf8b`, branch `docs/adr-metro-di-consumer-lifetimes`, PR https://github.com/wireapp/kalium/pull/4558.
- Owned checkout: this Android worktree and its `kalium/` checkout. Verify Kalium's Git top-level before every mutation. The original Android workspace and `/tmp/kalium-metro-di-adr-20261020` are read-only inputs.

## Milestones

- [x] Read ADR 0017 in full and relevant ADRs 0011–0013 and architecture guidance.
- [x] Initialize owned Kalium checkout and implementation branch on the explicit ADR base.
- [x] Record getter declarations, constructor captures, identity aliases and runtime startup inventory (timing baseline still pending).
- [ ] Add supported KMP Metro integration and global/user/consumer graph ownership.
- [ ] Migrate individual conversation/message bindings and compatibility accessors.
- [ ] Migrate all remaining broad feature wiring and audit fresh scope factories.
- [ ] Extract equivalent explicit runtime startup with individual worker dependencies.
- [ ] Integrate consumer lifetime with actual Android ViewModel creation/disposal.
- [ ] Verify identity, concurrency, generation invalidation, disposal and provider cache policies.
- [ ] Measure cold startup, first/warm ViewModel construction and allocation counts.
- [ ] Run affected Android/JVM/Apple, ABI, style and lifecycle/extension checks.
- [ ] Obtain fresh review closure for the final exact commits.
- [ ] Publish and link coherent implementation PRs; verify linked PR inventory.

## Review ledger

Every review round uses a new task and includes prior findings/responses. Reviewers are independent of writers. A prior SHA's review does not approve a later snapshot.

### Architecture round 1 (read-only, no implementation patch)

Snapshots: Android start and Kalium ADR stack base above.

- Identity reviewer task: `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-arch-identity-r1-20261020`.
- Platform/runtime reviewer task: `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-arch-platform-r1-20261020`.
- Both reviews complete; implementation findings remain open until exact-patch reviews and verification.
- Required responses: final roots in Android/JVM and each Apple leaf (not `appleMain`); preserve Apple open versus preparation and background suspension transitions; retain explicit initialized-resource and failed-cleanup records; keep restricted extensions separate; retain provider cache acquisition/removal.
- Required identity responses: qualified legacy captures and message/transport aliases; preserve Nomad paging coordination as user state; serialize generation-aware lifecycle transitions and reject stale-origin logout; enforce consumer admission and owned-operation disposal; test two use cases sharing the same repository type.
- Startup inventory includes every current `init` worker plus constructor-started `PendingProposalSchedulerImpl` work. Mandatory startup remains measured separately from requested dependency construction.

### Android consumer adapter writer

- Task: `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-android-vm-consumer-layer1-20261020`.
- Exclusive ownership: Android DI/ViewModel factory files. No Kalium edits or independent builds/commits. Result pending.

## Evidence and unresolved work

### First implementation checkpoint

- Kalium: `64dc94cbead9127bba62bd97195eb827b68b1976`, based directly on the ADR head. Android adapter and submodule pin are checkpointed in this ledger's containing commit.
- Conversation: 40 qualified capture providers and 77 individual member bindings; retained typing repositories, sender and work queue remain user-owned.
- Message: 118 providers, covering 48 captures, 67 members and 3 helpers. Public facades delegate while getter/cached identities remain explicit.
- Startup: explicit `UserSessionRuntime.start`, all original workers, and explicit proposal scheduler start. Platform factories start the runtime before publication.
- Consumer contract: two observer APIs plus `getMessageById`; guarded work/flows, creation admission and invalidation, consumer-only cancellation/draining. The existing message capture's paging coordinator is shared with the new consumer wrappers; unrelated legacy factory captures remain distinct.
- Android: 22 ViewModel binding containers moved to a creation graph; direct, assisted, manual-assisted and Resaca paths attach close ownership to the actual VM.
- Standalone `:logic:compileKotlinJvm` exited 0 and produced Metro-generated root/user/consumer graph classes. Log: `/tmp/kalium-metro-compile-jvm-layer1.log`.
- Focused JVM tests running. Android compilation, Apple compilation, ABI/style checks and measurements pending.
- Known open work: common session registry generation/origin serialization; all remaining feature/facade migration; expand consumer API beyond initial paths; complete all verification/measurement and fresh review closure. This checkpoint does not complete ADR 0017.

Implementation tasks (all complete, writers paused for review):

- Roots/consumer owner: `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-kalium-roots-consumer-layer1-20261020`.
- Message migration: `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-message-bindings-layer1-20261020`.
- Android adapter task recorded above.

### Checkpoint patch review round 1

- Exact reviewed Android: `3c4fb7b0dca9a3222bdf994b3cc1affc6aeb724f`.
- Exact reviewed Kalium: `64dc94cbead9127bba62bd97195eb827b68b1976`.
- Identity task: `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-patch-identity-r1-64dc94c-20261020`.
- Platform task: `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-patch-platform-r1-64dc94c-20261020`.
- Writers were paused for both reviews. Platform review found no new source defect, while retaining verification/completeness objections.
- P1: consumer drain before session cancellation can spend the shutdown timeout and skip session cancellation. Response: extracted `UserSessionShutdown`; preserve original owner cancellation order and drain consumers afterwards. Added regression with blocked non-cancellable consumer cleanup plus an ordinary session worker. Verification/closure pending.
- P1: Android fake omitted newly required `closeAndJoin`. Response: implemented it; rerun pending.
- Additional controller finding: a retained Android session dispatcher used latest-by-user lookup, permitting a delayed G1 factory to switch to G2. Fix task below anchors generation and admits actual ViewModel assembly. Verification/closure pending.
- Review does not cover later fixes. Common registry, remaining migrations, runtime integration tests, measurements and full platform/ABI evidence remain open.

### Active follow-up tasks

- Generation registry: `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-generation-registry-fix-r1-20261020` (provider registry and lifecycle tests only).
- Android generation/assembly: `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-android-generation-assembly-fix-20261020` (Android factory and framework-neutral consumer assembly API).
- Remaining inventory completed: `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-remaining-inventory-20261020`. Remaining scope includes 331 user-owner properties, global/auth/session wiring, cached and fresh feature facades, Cells and LongWork. Fresh backup mutexes/caches, call-facade caches, API-version snapshots, deferred Channels providers and domain operation owners need explicit preservation.
- Native compatibility investigation completed: `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-native-runtime-compat-20261020`.

### Verification and toolchain follow-up

- Initial focused JVM run: 36 tests, zero failures/errors/skips (consumer 8, concurrency 2, proposal 15, subgroup proposals 9, existing logout 2).
- Initial Android production `compileDevDebugKotlin` passed, including generated full application graph. `compileDevDebugUnitTestKotlin` failed on the missing fake `closeAndJoin`; no Android test pass claimed.
- Provider checks: LOCAL storage 3/network 4 tests passed; GLOBAL storage/network tests passed. AccountResourceAccess 15 JVM tests passed. Logs `/tmp/kalium-metro-providers-local.log` and `/tmp/kalium-metro-providers-global.log`.
- Initial Native compilation failed because the published Metro 1.4.5 runtime KLIB has ABI `2.4.0`, produced by Kotlin `2.4.20`. Kotlin `2.3.20` accepts ABI up to `2.3.0`. Cached and Central SHA-1 match `238a1a8029d1374de23f71ebf6662d25c2fc3f45`; this is an actual toolchain mismatch, not an absent file or unavailable Apple platform.
- Native primary evidence: [Central metadata](https://repo.maven.apache.org/maven2/dev/zacsweers/metro/runtime-iossimulatorarm64/1.4.5/runtime-iossimulatorarm64-1.4.5.module), [Metro runtime skew policy](https://github.com/ZacSweers/metro/blob/1.4.5/docs/stability.md#generated-code-stability), [older ABI publication issue](https://github.com/ZacSweers/metro/issues/144).
- User made Kotlin 2.4 available if required. Owned catalogs now select Kotlin `2.4.20`, Metro `1.4.5`; Kalium SKIE `0.10.15` and Mokkery `3.5.1` explicitly support this compiler. KSP stays `2.3.11` pending build evidence. No original workspace versions changed.
- A sequential JVM/iOS-simulator/macOS compilation is running under the revised pins. Source fixes still require final exact-commit verification and fresh review closure.

- Original Android integration selected Kotlin `2.3.20`, Metro `1.4.5`, and provider cache policy `GLOBAL`; standalone Kalium selects provider policy `LOCAL`. Revised compiler pins and their verification are recorded above.
- The existing ADR checkout reports a Git top-level inside the original submodule Git directory. It is not safe for mutations and is being read only.
- Metro tag `1.4.5` primary documentation inspected: `docs/dependency-graphs.md`, `docs/scopes.md`, `docs/multiplatform.md` at https://github.com/ZacSweers/metro/tree/1.4.5/docs.
- No measurement or behavioral-test success is claimed yet.

### Revised compiler and lifecycle checkpoint (uncommitted until test completion)

- Sequential standalone `:logic:compileKotlinJvm :logic:compileKotlinIosSimulatorArm64 :logic:compileKotlinMacosArm64` exited 0 under Kotlin 2.4.20. Log `/tmp/kalium-metro-kotlin2420-platforms-r2.log`. This verifies generated production graph compilation on those three targets, not Native linking, Swift export or tests.
- Kotlin 2.4 ABI-validation DSL no longer has `enabled.set(true)`; retaining `abiValidation {}` enables validation. No ABI check was disabled.
- The newer compiler rejected existing impossible edit-content tests after narrowing a sendable message to `Message.Regular`. Computing the same edit predicate before narrowing preserves behavior and compiles for Native; the predicate and sending branches are unchanged.
- Generation registry writer completed; Android assembly writer completed. Both are paused. Their tests and combined patch still need verification and fresh closure review.
- Direct runtime tests task: `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-runtime-startup-tests-20261020` (new test file only; no production mutations/builds).
- Conversation/message public extension getters now delegate to additional individual graph bindings. New generated-graph tests check lazy capture resolution, fresh use cases, independent user captures and the canonical conversation-to-message alias.

- Additional concrete counterexample: common cleanup's legacy worker timeout could permit provider removal while admitted consumer construction/non-cancellable cleanup was still unwinding. Response: retain the legacy worker timeout, then await consumer drain outside its catch before removing resources. A blocked-assembly JVM regression asserts parent storage is retained beyond the timeout. Apple still propagates the existing shutdown timeout and retains its lease/quarantine. Verification and closure review pending.

- Android's first revised-toolchain build failed in the existing Compose stability analyzer 0.8.0 (`StabilityAnalyzerPluginRegistrar`, removed compiler extension registration API). KSP completed the Kalium processing tasks; this was not a KSP failure. Selected analyzer 0.15.0, whose [tagged release](https://github.com/skydoves/compose-stability-analyzer/releases/tag/0.15.0) explicitly requires Kotlin 2.4.20. Stability baselines must be regenerated and checked after successful compilation; no analyzer checks were disabled. Log `/tmp/kalium-metro-android-kotlin2420-r2.log`.

- Android revised-toolchain build `:app:compileDevDebugKotlin :app:compileDevDebugUnitTestKotlin` passed (468 tasks; production Metro/KSP graph and test compilation), log `/tmp/kalium-metro-android-kotlin2420-r3.log`. No Android test execution claimed by this compile check.
- Runtime writer completed 12 direct startup tests covering all 21 actions, repeated start, deferred resolution/client gating, migration/factory/reset failures, cancellation cleanup and actual worker effects. All writers are now paused. The combined focused JVM suite is running; this is still not review closure.

- Focused JVM run first failed test compilation under Mokkery 3.5.1: 1,845 obsolete `eq` uses and 107 obsolete `matching` uses across the logic test sources, plus one new test generic inference error. No tests executed in that attempt. Parent fixed the explicit Throwable list type; mechanical matcher migration task `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-mokkery351-compat-20261020` handles existing test sources across modules. No deprecation suppression or skipped test compilation. Log `/tmp/kalium-metro-jvm-lifecycle-r2.log`.

- Android factory tests passed: 15 tests, zero failures/errors/skips, including existing owner/key behavior and direct/assisted/manual/Resaca plus invalidation and rejected-VM cleanup paths. Production application graph compilation is separate evidence; factory tests still mock the graph factory. Log `/tmp/kalium-metro-android-vm-tests-r2.log`.
- Added `docs/kalium-metro-binding-inventory.md` as a durable owner/capture inventory of all remaining facade families and cross-feature paths.

- Refreshed all ten checked-in Android stability baselines under analyzer 0.15.0. Running dump and check for the same module in one invocation exposed Gradle's missing task-dependency validation in the external analyzer; the build correctly rejected that invocation. Checks are rerun separately against the produced baselines. No task dependency or check suppression was added to the project. Logs `/tmp/kalium-metro-android-stability-r2.log`, `/tmp/kalium-metro-android-stability-check-r2.log`.

- All ten Android stability checks passed in a separate invocation after the required baseline refresh. Log `/tmp/kalium-metro-android-stability-check-r3.log`. Changes to these baselines reflect the analyzer/compiler upgrade; they are not allocation/performance measurements.

- Mokkery compatibility task completed: 244 existing test files; 1,845 eq removals (including aliases/typed nulls) and 107 matching-to-matches renames, parsed and structurally checked. JVM test compilation now succeeds. First execution: 67 tests, three fixture failures (duplicate server config seeding in two multi-account tests; one runtime test incorrectly counted already-verified Mokkery calls again). Fixed fixtures without changing production behavior; rerun pending.
- ABI update succeeded once standalone Kalium was given the existing Android SDK path through command-scoped ANDROID_HOME. KLIB additions are consumer API/createConsumer only. New JVM reporter omits constants from internal/private companions even though javap confirms their existing public static classfile fields remain. Kotlin 2.4 also changed the default module name and thus one synthetic bridge; pinned logic's JVM module name to its old project name following the [official migration guidance](https://kotlinlang.org/docs/whatsnew24.html#consistent-module-names-across-platforms). ABI dumps must be regenerated/check rerun after that pin.

- Combined focused JVM suite now passes: 67 tests, zero failures/errors/skips (consumer 12, consumer concurrency 4, legacy identity 3, startup 12, shutdown 1, registry/logout 11, proposals 15+9). Log `/tmp/kalium-metro-jvm-lifecycle-r4.log`.
- Revised final-root production compilation passed for iOS arm64 device, iOS simulator arm64 and macOS arm64; ABI dumps regenerated under the preserved JVM module name. Log `/tmp/kalium-metro-abi-native-r4.log`. This still does not establish Native linking/execution or Swift export compatibility. ABI check and standalone style check running next.

### Ready for fresh patch closure review

- Toolchain commits: Kalium `cad495859f8acb1c231781b7ab6d24fbbbf7e795`; Android `f274aa48e` (full SHA available in Git). Mechanical matcher changes are a separate commit from lifecycle fixes.
- Combined standalone `:logic:checkKotlinAbi detekt :logic:jvmTest` with the eight focused filters passed after formatting cleanup. The same 67 tests passed with zero failures/errors/skips. Log `/tmp/kalium-metro-checkpoint-r6.log`.
- Initial full style check found 126 findings in the new binding/consumer files. Targeted formatting and unused-import cleanup resolved them. Declarative binding-interface size/parameter metrics and catch-all cleanup that preserves original failures have local documented suppressions; no style configuration or baseline was changed. A required HttpClient import removed during cleanup was restored before the passing compile/test run.
- ABI validation now also generates an Android reference dump under Kotlin 2.4. JVM bridge mangling is preserved. All nine constants omitted by the new reporter remain public static fields in emitted JVM binaries; explicit javap evidence is in `/tmp/kalium-metro-jvm-constant-abi-evidence.log`. KLIB API delta remains additive.
- Native linking/execution, Swift export, full JVM suite, Android style analysis, production resource/capture integration and measurements remain open. The remaining global/auth/user-owner and feature migration inventory is still required before ADR completion.
