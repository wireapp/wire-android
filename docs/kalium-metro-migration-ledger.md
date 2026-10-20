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
- [x] Add supported KMP Metro integration and foundational global/user/consumer graph ownership (remaining global/user binding migration is separate).
- [x] Migrate individual conversation/message bindings and compatibility accessors (account/consumer lifecycle review closure remains separate).
- [ ] Migrate all remaining broad feature wiring and audit fresh scope factories.
- [ ] Extract equivalent explicit runtime startup with individual worker dependencies.
- [x] Integrate consumer lifetime with actual Android ViewModel creation/disposal; foundation verification and fresh exact-patch closure recorded below.
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

### Fresh patch review round 2 — request changes

- Exact reviewed Android: `8bca9a77e13a3dbf3cda417cea11f2e117910aad`; Kalium: `4fe0d0a44499e6211892287aa327fa01b17b13e7`. Both worktrees were committed and clean, writers paused.
- Android toolchain commit full ID: `f274aa48efd1c3573222158b26d0b1e8bf389db9`.
- Identity task: `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-patch-identity-r2-4fe0d0a-20261020`.
- Platform task: `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-patch-platform-r2-4fe0d0a-20261020`.
- Both reviewers confirm the R1 cancellation-order and fake `closeAndJoin` source fixes. No additional concrete conversation/message capture mismatch was found. This is not lifecycle or complete-migration approval.
- P1 identity: draining the entire published `viewModelScope` can wait for logout's own non-cancellable caller in `ClearSessionViewModel.cancelLogin`; ordinary logout callers also lose required continuations. Consumer/creator ownership and terminal operation regression tests are being corrected.
- P1 both reviewers: startup workers can legitimately request logout before the registry assigns its private owner. `isCurrent`/Apple's early `getRaw` then reject that request as stale.
- P1 platform: synchronous startup failure after workers launch loses the scope because the private owner is assigned only after factory return. Common cleanup can remove resources or reopen without quiescing that owner. Separate construction/startup with gated publication and retained failed-start cleanup is required.
- Consumer/terminal fix task: `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-consumer-terminal-owner-fix-r2-20261020`.
- Startup-owner fix task: `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-startup-owner-fix-r2-20261020`.
- Both are bounded non-overlapping implementation tasks; no delegated builds/commits. New source must receive a new independent closure review round after verification.

### Broader verification at the reviewed checkpoint

- Full JVM suite executed 4,167 tests: two architecture-check failures and twelve skips. Log `/tmp/kalium-metro-jvm-full-r2.log`. It is not a full-suite pass. Both failures involve composition code under feature package rules: Runtime's migration-worker import and LegacyMessageBindings' DAO input.
- Parent moved internal runtime composition to `logic.di` and added a documented file-local composition exception for the DAO binding; the business layer assertions themselves remain unchanged. Verification pending after both writers finish.
- Full Android `:app:detektAll` found four findings in the factory/test (two catch-all cleanup warnings and two wrapping findings). Terminal-owner writer owns their correction. Log `/tmp/kalium-metro-android-style-r2.log`.
- Native `:logic:macosArm64Test` with consumer/runtime/shutdown/Apple-account filters is compiling/linking from the reviewed `4fe0d0a44499` snapshot; no execution pass claimed. Log `/tmp/kalium-metro-native-tests-r2.log`; parent build session `10385`. Later source fixes require fresh verification.
- Platform reviewer independently confirmed the Native runtime ABI/compiler mismatch and root placement. Swift compatibility, real provider/capture integration and measurements remain open. The new Android ABI reference needs comparison with the previous AAR; its internal module-name bridge remains unpinned.

### Published drafts

- Kalium implementation layer: https://github.com/wireapp/kalium/pull/4563, branch `refactor/metro-di-consumer-lifetimes`, base `docs/adr-metro-di-consumer-lifetimes` (ADR PR 4558).
- Android adapter: https://github.com/wireapp/wire-android/pull/5418, branch `refactor/kalium-metro-di-consumer-lifetimes`, base `develop`, depends on Kalium 4563 and pins its commit.
- Both PRs registered immediately with `link_pull_request`; `list_thread_pull_requests` confirmed both links and draft/open status. Descriptions explicitly disclose the three P1 findings, incomplete checks and remaining full migration scope. Neither is ready; no merge/deployment authorized.

### Verification follow-up while R2 fixes are in progress

- macOS Native linking and filtered execution at Kalium `4fe0d0a44499e6211892287aa327fa01b17b13e7` completed with exit 0. Direct execution of that same emitted test executable confirmed 45 tests passed across six cases (consumer, legacy capture identity, runtime, shutdown and real Apple account/session resource tests). Logs `/tmp/kalium-metro-native-tests-r2.log` and `/tmp/kalium-metro-native-execution-r2.log`. This evidence predates the R2 lifecycle fixes.
- Compiled exact ADR-base source (`92e54877ee318fe6c46aae3a3832e78b3b1adf8b`) with its original Kotlin 2.3.20 pins in an ignored source archive under owned `kalium/build/metro-verification/archives/adr-base`. Android `CoreLogic` bytecode confirms the existing `$logic` module suffix; revised unpinned Android bytecode used `$com_wire_kalium_logic`. Moved the module-name preservation into shared KMP build configuration for every JVM/Android library target, replacing the earlier logic-JVM-only fix. Regeneration, binary comparison and checks pending. No existing external checkout or shared Git configuration changed.

### R2 lifecycle responses — writers paused for verification

- Startup writer completed and paused: private owner registration now precedes explicit runtime startup, public access waits for success, and failed/cancelled startup cancels and retains the owner. Cleanup timeout/cancellation blocks replacement until successful explicit retry. Apple ownership remains quarantined; background handoff does not resolve sync for a failed start. Added real JVM-provider startup/logout/failure/cleanup-retry barriers.
- Consumer writer completed and paused: the published ViewModel owns the consumer; only guarded SDK operations belong to consumer drain. Its caller job is not cancelled by consumer invalidation. An unpublished rejected ViewModel transfers only its discard job for cancellation/drain, including non-cancellable children and exceptional discard. Unreleased assemble parameter renamed `jobOnDiscard`. Added actual Android factory/manual `ClearSessionViewModel.cancelLogin` and continuation tests, plus real JVM provider/registry/logout deletion/reopen regressions. External logout effects remain stubbed in that JVM lifecycle fixture; Android factory tests still use a fake consumer. Full factory-plus-SDK Android integration remains open.
- Both writers report `git diff --check` clean, no builds/commits/staging. Parent owns combined verification and fresh R3 tasks. All writers are paused; neither response constitutes review closure.
- Parent running full `:logic:jvmTest detekt :logic:updateKotlinAbi :logic:compileAndroidMain` with command-scoped Android SDK after the shared JVM/Android module-name correction. Log `/tmp/kalium-metro-r2-fix-verification.log`; result pending.

- Verification setup correction: the first combined attempt stopped during configuration because Kalium discovers nested `build.gradle.kts` files and included the shallow baseline archive as a project. Moved that ignored archive below discovery depth; no project settings were changed. The rerun is `/tmp/kalium-metro-r2-fix-verification-r2.log`. No tests passed in the configuration-failed attempt.

- Added bounded Android real-factory/real-SDK integration task: `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-android-sdk-consumer-integration-r2-20261020`. Owns one new Android test file only; no production/SDK/config edits or delegated builds. It must report concrete platform limitations rather than substitute fake consumer coverage or skipped tests. Production writers remain paused.

- Android classfile comparison after recompilation preserved all 4,461 existing Android members selected by the previous published JVM API, matching name and JVM linkage descriptor; no missing signatures. Android-specific `CoreLogic` constructors were also compared with `javap`, and existing `$logic` internal/bridge names are restored. This comparison does not cover Swift/Native ABI or infer semantic compatibility. Evidence `/tmp/kalium-metro-android-binary-api-r3.json`; reproducible comparison script `/tmp/kalium-metro-compare-binary-api.py` selects the old governed API against old/new emitted Android classfiles. Kotlin ABI validation remains a separate required check.

- Combined full JVM execution reached 4,173 tests: two new lifecycle fixture failures, twelve existing skips; no architecture-check failures remain. First fixture asserted zero constructor children despite the inventoried existing LongWork monitor, then waited on a barrier never completed after that assertion failed. Removed that unsupported assertion; explicit runtime startup is still independently counted and asserted zero for invalidated construction. Second fixture mixed virtual cleanup timeout with real Default-dispatcher worker unwind; final unblocked drain/cleanup now runs on the real dispatcher while the blocked timeout assertion still uses virtual time. No production behavior changed to repair these fixtures. Focused provider/style/ABI rerun `/tmp/kalium-metro-r2-fix-targeted-r3.log` pending. Constructor-started LongWork work remains explicitly listed for its remaining migration slice; complete runtime extraction is not claimed.

### Verified lifecycle fix snapshot

- Kalium commit `c5e75810954fb6d664362561db08423bc5b95bd7`: startup-owner retention/public-ready gate, failed-start cleanup, creator/consumer ownership correction, composition architecture-check fixes and preserved JVM/Android module names. SDK worktree clean after commit.
- Full logic JVM suite passed: 4,173 tests, zero failures/errors, twelve existing skips. `:logic:checkKotlinAbi` passed in the same invocation. LOCAL provider suites passed (storage 3, network 11); account resource access passed (15). Log `/tmp/kalium-metro-r2-fix-full-r4.log`. Focused provider suite passed 14/14; standalone full style and ABI regeneration passed in `/tmp/kalium-metro-r2-fix-targeted-r3.log`.
- GLOBAL provider rerun `/tmp/kalium-metro-providers-global-r3.log` pending. New exact-commit Native execution, Android factory verification and fresh R3 review still required. Remaining migration/measurements/Swift work remains open.

- GLOBAL provider rerun passed with revised compiler pins: storage 3/network 11 tests, zero failures/errors/skips. Log `/tmp/kalium-metro-providers-global-r3.log`. These provider tests continue to exercise configured process-global versus local behavior outside graph caching.
- New Native filtered run started against exact SDK `c5e75810954fb6d664362561db08423bc5b95bd7`, log `/tmp/kalium-metro-native-tests-c5e7581-r3.log`, parent session `50452`; no new Native pass claimed yet.

- Android integration writer completed and paused with one new Robolectric test file. It uses actual SQL-backed conversation/message observers and lookup through the production consumer factory adapter and a generated narrow Metro graph; checks owner/key retention, disposal, parent/sibling usability, account replacement/stale rejection and fresh generation. External ports substituted are AVS static loading, WorkManager and existing mock HTTP engine. Full application graph, registered-client crypto and actual logout orchestration are outside this fixture. Compilation/execution/style are pending; no pass claimed.
- Checkpointing Android production adapter plus tests/pin for read-only R3 review while the exact SDK Native run continues. All writers are paused. Reviewers must treat Android verification as pending until its coordinated run finishes.

### Fresh foundation patch review round 3 — source findings closed

- Exact reviewed Android `71b1a66347c9fc699bd6ebccfdb7130627c2b228`; SDK `c5e75810954fb6d664362561db08423bc5b95bd7`. Both worktrees clean and all writers paused for independent read-only reviews.
- Identity task `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-patch-identity-r3-c5e7581-20261020`.
- Platform task `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-patch-platform-r3-c5e7581-20261020`.
- Both found no additional concrete foundation defects and closed the R1/R2 source findings on these exact SHAs: cancellation order, fake drain contract, retained G1 factory, terminal-caller ownership and private startup owner/failed-start retention. Neither review certifies complete ADR implementation, performance improvements or unexecuted Android/Swift checks.
- Platform reviewer independently compared the old/new Android classfiles: all 4,461 governed members retained signature and visibility/static/abstract flags; Android CoreLogic constructors match. It also verified Metro Native manifest ABI/compiler and Apple/restricted-runtime source ownership.
- SDK exact `c5e75810954f` Native run completed exit 0. Direct execution confirmed 46 tests passed across six cases, including real Apple session/resource/crypto ownership. Logs `/tmp/kalium-metro-native-tests-c5e7581-r3.log` and `/tmp/kalium-metro-native-execution-c5e7581-r3.log`. Both SDK/Android checkpoint commits pushed to the existing linked draft PRs; bodies updated with current evidence and remaining scope. GitHub connector body updates returned 403 (integration permission); existing GitHub CLI updates succeeded.
- Android first combined run stopped at five test-only style findings; separate test build then failed compilation because the new SQLite seed fixture requires the persistence module on the test compile classpath. Production application graph compiled. Logs `/tmp/kalium-metro-android-integration-r3.log`, `/tmp/kalium-metro-android-integration-r3-tests.log`. No latest Android test pass is claimed.
- Parent added the explicit test-only persistence dependency and included-build substitution; corrected wrapping, extracted the user seed fixture, and documented method-local size exceptions for sequential/concurrent lifecycle scenarios plus Robolectric's required constructible shadow. No style configuration/baseline changes or skipped tests. Combined rerun `/tmp/kalium-metro-android-integration-r4.log` pending. These later Android test/build edits are outside R3's reviewed SHA and require exact-patch review with the next checkpoint.

- Android combined rerun passed: production graph compilation remains current, unit-test compilation passed, full `:app:detektAll` passed, and all 26 selected tests passed with zero failures/errors/skips: factory 17, manual factory 8, real-SDK Robolectric integration 1. Log `/tmp/kalium-metro-android-integration-r4.log`. The integration fixture executed (not skipped) against real SQLite, SDK registry/consumer/repositories, public account replacement, and production consumer adapter/narrow generated graph. The external-port/full-application/crypto/logout limits above remain. No production source changed after the R3 review; only test fixture, test-only dependency/substitution and ledger changed. Exact final Android delta will receive fresh review before the next migration layer.

### Fresh foundation patch review round 4 — foundation closure

- Exact reviewed Android `671eddea26ec91686c534dd2456b8c94c120a33f`; unchanged SDK `c5e75810954fb6d664362561db08423bc5b95bd7`. Worktrees clean, production/test writers paused.
- Identity task `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-patch-identity-r4-671edde-20261020`.
- Platform task `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-patch-platform-r4-671edde-20261020`.
- Both found no additional concrete foundation findings and closed prior findings on these exact snapshots. Both independently checked actual XML: Android 26 passed including the executed real-SDK SQLite fixture; SDK JVM 4,173 with 12 existing skips; supplied Native log 46 passes. Platform reviewer independently confirmed all 4,461 governed Android members retain descriptor and visibility/static/final/abstract flags, plus old CoreLogic constructors/bridges and Metro Native manifest ABI/compiler.
- Closure is explicitly FOUNDATION ONLY: the fixture uses the shared production adapter/helper and consumer module through a narrow test graph, not the running full application graph. Distinct use cases alone do not establish repository identity (separate generated-graph counting tests do). Actual Android terminal logout remains separately fake/mocked while real SDK provider/SQL/logout orchestration is covered on JVM with external effects stubbed. Full migration, remaining startup extraction, Swift receiving-app compatibility and measurements remain required.
- All ten Android stability checks passed again on the current SDK/compiler/factory snapshot. Log `/tmp/kalium-metro-android-stability-c5e7581-r4.log`. No further baseline refresh was needed.
- Checkpoint pushed to the linked existing drafts. No merge/deployment authorized or performed.

### Measurement harness task

- Task `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-construction-measurement-harness-20261020` prepares runnable standalone counters/fixtures under ignored `kalium/build/metro-verification/measurements/20261020` only. It cannot mutate tracked source or run coordinated Gradle/Native builds. Exact constructor-entry counters and caller-thread allocated bytes must be distinguished; compiler/toolchain differences and asynchronous startup overlap remain explicit limits. Parent will execute after full migration.

### Cached feature/global binding layer — integration in progress

- New stack branches: SDK `refactor/metro-di-cached-feature-bindings` from `c5e75810954fb6d664362561db08423bc5b95bd7`; Android `refactor/kalium-metro-di-cached-feature-bindings` from foundation plus review-ledger commit `4a7a3dd42295a0566a846386268e09df0687798d`.
- Client/user writer completed and paused: task `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-cached-client-user-bindings-layer2-20261020`. Client has 27 retained constructor captures/19 members; user has 38 captures/51 members plus retained personal-to-team support. Six generated-graph scenarios added; unexecuted at handoff.
- Auxiliary writer completed and paused: task `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-cached-search-channels-meeting-debug-layer2-20261020`. Search 7 captures/4 members, channels 4 raw-function captures/4 members, meeting 10 captures/10 members, debug 32 captures/32 members plus operation ports. Ten generated-graph scenarios added; unexecuted at handoff.
- Parent integrated deferred capture adapters into the user graph, replaced six broad facade constructor calls, and routed startup/cross-feature access directly to individual bindings. Global facade now delegates to 38 individual bindings, retaining the original five caches and existing coroutine/scheduler owner. External providers remain supplied instances. Global SessionScope migration remains a separate unfinished family.
- Parent standalone JVM compile started in `/tmp/kalium-metro-cached-layer2-compile-r1.log`; no pass yet. Existing meeting integration fixture adapted to a narrow generated graph. Style, ABI, test execution, platform builds and fresh exact-patch reviews remain required.
- Next-family tasks are inspecting/preparing isolated patch text while tracked writes are held for this checkpoint: `metro-fresh-team-service-apps-connection-bindings-20261020` and `metro-authentication-session-bindings-20261020`, under the same full task-ID prefix above. These fresh/cached-auth lifetimes must not be collapsed into user ownership. No duplicate builds or additional controllers.

- SDK checkpoint `23fd95e8ff39eb9f64c2063b8ee73e83c5ab9633`: production JVM compilation passed. Initial test compilation rejected three private annotated test graph declarations; changed them to internal. Focused tests/style/ABI rerun `/tmp/kalium-metro-cached-layer2-targeted-r1.log` pending. No test pass claimed yet for this layer.
- Fresh/auth writers completed concrete staged patches with no tracked changes. Fresh facade patch `/tmp/kalium-metro-fresh-facades/fresh-facades.patch`; authentication/session patch `kalium/build/metro-verification/staging/auth-session/auth-session.patch`. Parent will apply after this exact-patch checkpoint. Source edits remain frozen for review.
- Owned-operation facade task `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-calls-backup-cells-operation-bindings-20261020` is preparing Calls/Backup/MultiplatformBackup/Cells changes under ignored staging only; no tracked changes or builds allowed during this checkpoint.
- Measurement task delivered runnable instrumentation and real-VM before/after fixtures with strict commit/source guards. Standalone utility/syntax checks passed; SDK/Android measurement compilation, execution and all performance results remain pending. No savings claimed.

### Cached/global patch review round 5 — pending

- SDK exact checkpoint `23fd95e8ff39eb9f64c2063b8ee73e83c5ab9633`; Android pin and ledger are in this containing commit. Foundation source unchanged from its closed round-four snapshot.
- New independent tasks use stable request IDs `metro-cached-identity-r5-23fd95e-20261020` and `metro-cached-platform-r5-23fd95e-20261020`, under the full task-ID prefix above. Verification still pending; reviews cannot certify unexecuted checks or full ADR completion.

- Both R5 reviewers completed with no new concrete correctness defect. Identity reviewer independently compared all six capture sets/adapters, global caches, Channels raw functions and deferred Debug state. Platform reviewer confirmed all five equivalent leaf factories and 114 governed changed-facade/CoreLogicCommon JVM methods retain descriptors/access flags. Both left P2 verification open: six detekt issues, current tests/ABI/platform checks incomplete.
- Parent corrected the six detekt reports locally: removed three redundant imports, wrapped one long line, and documented the one declarative graph-factory parameter-count exception on that method. No global rule/baseline was changed. Exact follow-up commit and review closure remain pending.
- Separate focused JVM run `/tmp/kalium-metro-cached-layer2-tests-r2.log` exited 0: 53 tests, zero failures/errors/skips across eight cases. Includes all 18 new cached/global identity scenarios, legacy identity 3, consumer 13, explicit runtime 12, shutdown 1 and SQL-backed meeting integration 6. The earlier combined run stopped at detekt and did not execute these tests; its old XML was not counted as new evidence.
- Corrected style plus ABI and Android/iOS device/iOS simulator/macOS production graph compilation started in `/tmp/kalium-metro-cached-layer2-platform-style-abi-r2.log`; no result claimed yet. Full SDK/Android/Native/Swift and final migration verification remain open.
- Draft stack layer published and immediately registered: SDK https://github.com/wireapp/kalium/pull/4564 (base #4563), Android https://github.com/wireapp/wire-android/pull/5419 (base #5418). Both descriptions disclose incomplete verification/scope; four implementation PR links were verified with list_thread_pull_requests. No watcher, merge or deployment requested.
- Remaining bounded writers are preparing staged implementation only: `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-user-owner-individual-bindings-20261020` and `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-direct-sdk-api-android-bridges-20261020`. Parent staged explicit LongWork startup/individual bindings under ignored staging. Operation-facade writer finished its guarded patch; its incomplete terminal summary was recovered through a bounded follow-up task `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Athread-send%3Ametro-operation-handoff-result-recovery-20261020`, confirming no additional finding and all build/runtime checks still unverified.

### Cached/global corrected checkpoint — fresh closure round 6

- SDK `22a1bcf7a469d905daaddfdb675b226aa0981ad3` contains only the four-file local style response after reviewed `23fd95e8ff39eb9f64c2063b8ee73e83c5ab9633`. Parent source and submodule pin are in this containing Android commit; future writers still have no tracked-write release.
- `/tmp/kalium-metro-cached-layer2-platform-style-abi-r2.log` exited 0: full standalone detekt, logic Kotlin ABI validation, Android/JVM and all three configured Apple production graph compilations passed. No rule/baseline or ABI reference change was needed. Combined exact-source command included checkKotlinAbi, compileAndroidMain, compileKotlinIosArm64, compileKotlinIosSimulatorArm64 and compileKotlinMacosArm64.
- Focused 53 JVM passes above remain current for behavior; a complete logic JVM run started against this checkpoint in `/tmp/kalium-metro-cached-layer2-full-jvm-r3.log`. Current Native test execution and Android consumer/full application integration reruns remain pending; Swift/measurements/full ADR remain open.
- Fresh independent closure tasks will use distinct stable request IDs `metro-cached-identity-r6-22a1bcf-20261020` and `metro-cached-platform-r6-22a1bcf-20261020`. They must inspect the actual new SHA and the R5 response/evidence, with no inference of complete ADR migration or tests still running.

- Both round-six reviews completed against SDK `22a1bcf7a469d905daaddfdb675b226aa0981ad3` / Android `3f25936f6d0dd4ffb0403bf1954e9e639de9be4f`: no new concrete source correctness defect; R5 style objection closed. Platform review independently reproduced the 4,461-member Android binary comparison and confirmed current generated ABI references. Current selected tests cover 53 scenarios; sequential narrow binding tests do not establish exhaustive concurrent resolution.
- Full JVM run `/tmp/kalium-metro-cached-layer2-full-jvm-r3.log` failed: 4,191 tests, one failure, twelve existing skips. `UserSessionScopeProviderLogoutTest.invalidationDuringCreationRejectsPublicationAndWaitsBeforeRemovingResources` reported SQLite `IOERR_FSTAT` through user `MetadataDAOImpl.valueByKeyFlow`, with a suppressed `valueByKey` failure. This remains an unresolved P2 verification objection, not an environment-flake classification or proof of a new production defect.
- Parent is rerunning the unchanged fourteen-test provider class in `/tmp/kalium-metro-cached-layer2-logout-baseline-r6.log`. Bounded read-only attribution task `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-sqlite-lifecycle-failure-attribution-20261020` must identify the user owner/key/collector and distinguish virtual-time account-drain timeout from incomplete fixture/global teardown. No duplicate build or weakened assertion is authorized.
- Direct SDK/Android API task `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Adelegate-task%3Ametro-direct-sdk-api-android-bridges-20261020` delivered concrete guarded patches for 179 cached-family member bridges plus twelve unexecuted tests. Follow-up implementation task `node:delegated-task:command%3Amcp%3A4d3a1ef2-d977-4425-a27f-c1c03ed8c791%3Athread-send%3Ametro-remaining-android-factory-bridges-implementation-20261020` owns separate staged remaining-family API/factory patches, preserving fresh facade and retained Cells ownership. All tracked source integration, index operations and builds remain parent-owned.
