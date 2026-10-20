# Kalium Metro measurements

Source-only tooling compares the explicit baseline and integrated Android/SDK snapshots through real ViewModel, consumer and SQLite paths. Both final full commit IDs are required. Generated tools, archives, logs and results stay under the dedicated ignored output directory.

Independent reviews corrected consumer retention and complete archive provenance. Integration requires strict teardown of retained user resources and the independently owned Android network observer, with separate cleanup barrier and actual adapter regressions before any measurement cohorts. The original R1/R2/R3 packages remain frozen evidence. Actual run results and limitations belong in the migration assessment and measurement report; utility tests alone establish no savings.

## Files and output placement

- `src/com/wire/metro/measure/Probe.java`, `ConstructorAgent.java`: bootstrap counter utility and load-time ASM agent.
- `build-tools.py --smoke`: standalone `javac`, cached ASM packaging, verified Java smoke, report-unit and source-archive guard checks. No Gradle/downloads/project compilation.
- `fixtures/current.kt`: actual Android `ViewModelProvider`/`ViewModelStoreOwner` → production consumer adapter/`createConsumerViewModel` → narrow generated Metro graph and production `KaliumConsumerModule` → real SDK consumer use cases.
- `fixtures/baseline.kt`: actual Android owner/provider → narrow generated graph and the existing `WireMetroViewModelFactory` base → legacy conversation details/message observer/message lookup getters. Its map-factory subclass mirrors the old session factory body without importing the full app graph.
- `harness.py`, `measurement.init.gradle`, `parent-commands.sh`: parent-only guarded staging, serial builds/execution, provenance, JUnit capture and cleanup. No shared configuration changes. The init script uses Kover's public `disable()` API for the measurement invocation; the probe rejects uncontrolled startup Java agents.
- `run-owner-drain-regression.py`, `owner-drain-regression.init.gradle`: six cleanup barrier/failure tests plus a separate actual two-cycle SDK adapter test. The adapter captures and drains the eager network observer and unregisters only its callback while preserving an unrelated registration. Exactly seven fresh passing testcase identities are required before cohorts.
- `archive_manifest.py`: inventories every staged Android + SDK archive entry before the first build and verifies content, type, permission modes and symlink targets on every baseline reuse and before/after every fork. No source directory or hidden file is excluded from that initial inventory.
- `summarize.py`: explicit units, separate ENTRY/RETURN identity reports, median/range distributions and validity gates. Instrumented elapsed times/bytes are not performance summaries.
- `parse-only.py`: Kotlin PSI syntax parsing and Groovy grammar parsing; no type checking/Metro generation/Gradle evaluation.
- `smoke/`: standalone Java/parser sources, report checks using real standalone Java output, and `check_archives.py` failure tests on a tiny synthetic source tree. The latter replaces the Gradle-launch function with a mock; it never archives repositories or injects SDK fixtures. Actual utility evidence is emitted as `smoke/archive-checks.json` and `smoke/archive-fixture-source-manifest.json`. Those are utility checks, not SDK results.
- `package-review.py`: whitelist-only source packaging under ignored output, with SHA-256 manifest and `--check`. It never installs into the tracked destination.
- `repo_paths.py`: finds the enclosing Android repository or uses explicit `KALIUM_MEASURE_ROOT`. Runtime outputs default to `kalium/build/metro-verification/measurements/20261020/generated-v2`, below module-discovery depth. Set `KALIUM_MEASURE_OUTPUT` to another dedicated ignored directory if needed. The root tools directory stays source-only.

The v2 output directory separates these metrics/receipts from the previous ENTRY-only harness. Incomplete/legacy data cannot silently acquire identity units. Package checks permit ignored Python bytecode caches but reject other unexpected files.

## Commands that only verify/package utilities

From the existing ignored source directory:

```bash
ROOT=/Users/mohamadjaara/.t3/worktrees/wire-android/refactor-kalium-metro-di-consumer-lifetimes
MEASURE="$ROOT/kalium/build/metro-verification/measurements/20261020"
python3 "$MEASURE/build-tools.py" --smoke
python3 "$MEASURE/parse-only.py"
python3 "$MEASURE/package-review.py" --label review-r3
python3 "$MEASURE/package-review.py" --label review-r3 --check
```

The proposed source package is then at:

```text
<ROOT>/kalium/build/metro-verification/measurements/20261020/generated-v2/review-r3/tools/kalium-metro-measurements
```

The parent can run its `build-tools.py --smoke` directly at that staged location before copying it. `package-review.py` refuses to overwrite a previous package; use a distinct `--label` for any further review snapshot. Generated utilities and metrics are written to the ignored output, not copied into the source package. Set `KALIUM_MEASURE_OUTPUT` to a fresh ignored subdirectory for isolated utility verification.

## Parent-only actual measurement commands — do not run before final integration

After review/integration, choose `MEASURE` as either the staged source directory or the parent-installed root tools directory. Set `AFTER_ANDROID` and `AFTER_SDK` to the **explicit final full SHAs**. Then:

```bash
: "${AFTER_ANDROID:?Set the exact final integrated Android commit}"
: "${AFTER_SDK:?Set the exact final integrated SDK commit}"
KALIUM_MEASURE_ROOT="$ROOT" \
KALIUM_MEASURE_OUTPUT="$ROOT/kalium/build/metro-verification/measurements/20261020/generated-v2/final-integrated-r3" \
AFTER_ANDROID="$AFTER_ANDROID" AFTER_SDK="$AFTER_SDK" \
RUN_LABEL=20261020-final-v2-r3 \
bash "$MEASURE/parent-commands.sh"
```

This serial workflow builds standalone tools, parses fixtures, stages a new exact baseline source archive and the current fixture, runs before/after × startup-only/VM × timing/constructors, summarizes and removes only the unchanged current injection. It defaults to three test-JVM forks, three complete warmup cycles per fork, seven measured owner/session cycles, ten fresh warm VMs per session and a 250 ms observation interval. Set installed `JAVA_HOME`/`ANDROID_HOME` explicitly as necessary. No GC is forced. Record power/thermal/host-load conditions with the results.

For individual parent commands, export the same fresh `KALIUM_MEASURE_OUTPUT` throughout staging, utility building, running and cleanup (for example the `final-integrated-r3` directory above):

```bash
python3 "$MEASURE/harness.py" stage-baseline --android-commit "$AFTER_ANDROID" --sdk-commit "$AFTER_SDK"
python3 "$MEASURE/harness.py" stage-current --android-commit "$AFTER_ANDROID" --sdk-commit "$AFTER_SDK"
ANDROID_HOME="$HOME/Library/Android/sdk" python3 "$MEASURE/harness.py" run \
  --variant current --scenario vm --pass constructors --label current-vm-constructors-v2-r3 \
  --identity-limit 250000
```

`run` also accepts `--pass timing`, `--scenario startup-only`, `--forks`, `--warmups`, `--repetitions`, `--warm-vms`, `--observation-ms` and extra Gradle arguments after `--`. The identity cap is command-scoped and recorded in each row. Do not add filters/coverage agents. The actual Gradle command is retained in each fork's `command.json`. The runner pins command-scoped `kalium.publish.version` to the source SDK SHA, avoiding archive version inference from an enclosing repository. Summarize using `python3 "$MEASURE/summarize.py" /absolute/ignored-output/results/RUN_LABEL`, then `python3 "$MEASURE/harness.py" cleanup --variant current`.

### Real Robolectric callback gate

Each measurement fork runs a positive callback control **before CoreLogic, account, SQL seed, mock-port or session setup**. `WireKaliumMeasurementRunner` extends the standard Robolectric runner configuration with `doNotAcquirePackage("com.wire.metro.measure.")`. The sandbox delegates that package to its parent, which resolves the bootstrap JAR; SDK and fixture classes retain their normal sandbox ownership. Both timing and constructor modes use this same runner.

Bootstrap placement alone is insufficient. A focused reproduction of R8's zero-callback symptom under Robolectric 4.16.1 showed a selected constructor containing both direct callbacks, but symbolic `Probe` resolution produced a second definition in `AndroidSandbox$SdkSandboxClassLoader`. The agent registered constructors in the bootstrap definition; ENTRY calls incremented the sandbox definition instead. Its `installed=false` state also discarded RETURN identities. The fixture's explicit bootstrap `Class.forName` therefore observed neither callback. The package delegation fixes that split without reflective callbacks in the constructor hot path. Robolectric's `SandboxClassLoader.loadClass`, `InstrumentationConfiguration.shouldAcquire`/`Builder.doNotAcquirePackage` and `RobolectricTestRunner.createClassLoaderConfig` provide the loader integration.

The control proves its subject is defined by the real Robolectric sandbox and that symbolic resolution from that loader returns the exact bootstrap `Probe` used by the fixture. It creates two distinct instances through two constructor overloads each. In constructor mode the bootstrap probe requires exactly four ENTRY calls on the caller thread, both exact returned references in both identity sets, and one runtime-class bucket. The separate `callback_control` row records these facts and releases all identity maps through the same window teardown as samples. Final provenance records the control result and subject loader type. The report requires the successful control as well as all existing cold-session, coverage, identity, retention, phase, JUnit and cleanup gates. Control rows never enter SDK sample distributions; their four entries remain visible in cumulative constructor facts.

Timing mode has **no constructor agent**, requires zero ENTRY delivery, and emits null constructor/identity fields. The control's reflection, class loading, two constructions and row I/O precede every measured window in both modes. Constructor callbacks remain direct static calls, with the existing counter/identity-lock overhead confined to the constructor pass. Constructor-mode elapsed time and caller bytes remain excluded from performance comparisons.

For a bounded real-sandbox check after ordinary guarded staging, use a fresh label and one fork per mode:

```bash
python3 "$MEASURE/harness.py" run --variant current --pass constructors --control-only --forks 1 --label callback-constructors -- --console=plain
python3 "$MEASURE/harness.py" run --variant current --pass timing --control-only --forks 1 --label callback-timing -- --console=plain
```

`--control-only` executes the actual measurement testcase only through this gate, finishes provenance and returns before SDK setup. The harness immediately requires exactly one fresh passing testcase, one valid bootstrap/control JVM, and zero SDK samples. It preserves prior XML separately before each fork. `summarize.py` explicitly rejects control-only runs as cohorts. `parent-commands.sh` requires one constructor control fork for each variant before the cleanup regressions or any timing/constructor cohorts. Any delivery/loader mismatch therefore stops the serial workflow before full cohort work. Use a new ignored R9 directory and freshly staged baseline archive for subsequent integrated execution; the rejected R8 evidence and its archive must remain unchanged. Utility smoke additionally checks the control API and report rejection against actual standalone Java rows; it does not replace the real Robolectric check.

Staging checks both Git top-levels before interpreting commit/cleanliness. It refuses mismatches, tracked/index edits, existing fixture files, receipts, archive paths or result labels. Cleanup deletes only an exact hash-matching injection at its recorded snapshot; it refuses user edits/changed commits. The original workspace and `/tmp` ADR checkout are read-only inputs. No Git worktrees/config/index changes are made. Baseline archives/receipts remain ignored for guarded reuse; optional baseline cleanup removes only its fixture/receipt and deliberately does not recreate an existing archive in place.

Baseline staging records `baseline-source-manifest.json` outside the archive after the documented patch and fixture injection, before any build. Its SHA-256, file/entry counts and commits are bound into the stage receipt. Every source file, build script, dependency catalog, properties file, nested hidden source/config file, directory mode and symlink target in BOTH extracted repositories is inventoried. Every reuse verifies all required entries first; this includes source paths under names such as `build/`. Added source/config paths, deletions, content/type/mode/link changes and manifest changes fail closed. Each fork is checked before launch and after exit, and the full source manifest is copied into baseline run evidence. The two fixture/patch hashes alone do not establish source integrity.

Archive manifest schema 3 records exact generated-output paths from the initial archive layout. A project root must contain a recorded regular `build.gradle[.kts]` or `settings.gradle[.kts]` outside all `src/` source trees; source scripts inside `src/` do not establish project roots. Only `<project>/build/`, `<project>/.gradle/` and `<project>/.kotlin/` are permitted, and only when that repository's recorded root `.gitignore` contains the corresponding directory rule. The manifest retains project roots, source roots and these exact paths as evidence; reuse derives the policy from the recorded entries, so new build scripts cannot grant exceptions. An arbitrary `build/` below `app/src/main/kotlin/` or `kalium/logic/src/commonMain/kotlin/` is source, not an output exception. The walker inspects nested directories, including permitted output trees, and rejects new symlinks at every depth. Kalium also configures the exact `data/protobuf/generated` output outside build/. Schema 3 permits that SDK path only with the recorded protobuf project, regular build script containing its inspected output/source-set markers, and recorded `generated/` ignore rule. The policy records both evidence hashes. This never grants a general generated-directory exception; pre-existing files remain required source entries and new nested symlinks are rejected. This models the pinned project layout rather than evaluating arbitrary Gradle code.

Android's three exact ignored generated asset/version paths are also permitted; their new parent directories admit no unrelated siblings. Initial archive entries are always content/type/mode/link checked, including entries inside any output-looking directory. New regular generated files/directories at the permitted paths remain allowed, including generated Kotlin/Java compilation inputs; their contents are not source-hashed. This is not complete generated-input or binary attestation. The policy recognizes this recorded standard project layout rather than evaluating arbitrary Gradle source-set/task declarations. It is deliberately narrower than all Git ignore rules: new `local.properties`, IDE/config files or other unexpected output shapes require inspection. Content/permission/type/link checks are drift detection, not a cryptographically immutable archive or a live-filesystem lock. R1 receipts without a complete manifest and R2/schema-1 manifests are rejected; choose a fresh ignored output directory to stage new archives. No old archive is retrofitted or changed by these fixes.

Standalone regression checks cover the three independently reproduced R2 cases after the receipt/manifest are recorded: `app/src/main/kotlin/build/Unlisted.kt`, `kalium/logic/src/commonMain/kotlin/build/Unlisted.kt` and `app/src/main/kotlin/build/external-source`. They also cover additions and symlinks inside existing output-named source directories, source-script/unknown-project exclusions, nested output symlinks, allowed module/root outputs and exact assets, required-source hashes/modes/links, and all before/after-fork guards. These use synthetic trees with a mocked Gradle-launch function; they are not SDK executions.

The baseline archive uses Android `38bbefb59d0c858fbc97f2eab9cec345aabca21b` and SDK ADR `92e54877ee318fe6c46aae3a3832e78b3b1adf8b` (on `fe0e7a8dd95a3abc4da773d194dc1c777b34a4e9`). It never changes/reuses the existing read-only `archives/adr-base`. One archive-only persistence composite substitution and command-scoped test dependency expose existing public SQLite seed APIs; their hashes/purpose are recorded. No compiler/pin upgrade is silently applied to baseline source.

## Exact units: ENTRY calls, selected RETURN identities, caller bytes

The JSON schema is version 2. Existing ENTRY field names remain unchanged:

- `constructor_entries_all_threads`, `constructor_entries_caller_thread`: exact selected `<init>` ENTRY callback counts by declaring binary type + descriptor. The callback is inserted before `super()`/`this()` without passing uninitialized `this`. Default/delegating constructors, superclass calls and failed constructors can each add entries. These are **calls**, never silently renamed allocations or objects. Descriptor totals aggregate defining loaders; coverage records retain loader-kind evidence.
- `constructor_return_unique_identities_all_threads`, `constructor_return_unique_identities_caller_thread`: cardinalities of reference-identity sets for selected constructor normal-RETURN callbacks accepted within one window. The `ALOAD 0` callback runs immediately before bytecode `RETURN`, where `this` is initialized. Repeated default/delegating/superclass callbacks for the same object are counted once in the global all-selected set and once in the caller subset, as applicable.
- `constructor_return_unique_identities_by_runtime_class`: disjoint buckets by **actual runtime Class identity**, separated by defining-loader IDs local to that window. Same-named classes from different loaders remain distinct. IDs are not comparable across windows/forks. Summary binary-name rollups explicitly sum loader-distinct classes and retain the raw per-window class/loader buckets.
- `caller_allocated_bytes`: supported/enabled `ThreadMXBean` heap-byte approximation for the timed caller platform thread **only in the uninstrumented timing pass**, or null if unavailable. Instrumented rows deliberately emit null. It is not object count or whole-JVM/session allocated bytes.

RETURN identities are **selection-coverage observations, not complete JVM allocation/object totals** and not proof of successfully completed outermost construction. A selected superclass can return and count a subclass instance before that subclass throws. A selected delegated inner overload can return before its outer overload throws. A constructor that throws before any selected RETURN contributes ENTRY facts but no RETURN identity, despite having required allocation. A selected superclass can also observe an unselected runtime subclass. Arrays/unselected paths are not covered. An object entering before a window can return within it; a chain spanning windows can appear once in each. Do not sum per-window cardinalities into a unique-run allocation total.

Caller attribution describes the thread executing the callback, not the allocating thread. All-thread observations include async startup/other workers only within their acceptance window; they cannot be assigned causally to VM resolution. Identity membership linearizes under one lock shared by begin/end/callbacks. A delayed callback is classified when admitted under that lock. ENTRY deltas remain per-counter snapshot intervals and do not share an atomic global snapshot with RETURN identities. Identity start/end monotonic timestamps and narrower timing timestamps are explicit. There is globally at most one active window: nested/cross-thread overlaps, foreign end and repeated end are rejected.

### Retention and instrumented-pass limits

`IdentityHashMap` provides reference equality without invoking measured objects' `equals`/`hashCode`. Exact deduplication **strongly retains observed objects during an active instrumented window**. It changes reachability, GC/finalization/resource-cleaner timing, contention and instrumentation allocation. This pass is for selected ENTRY/RETURN facts; its elapsed time is diagnostic and its byte field is null. Compare wall time/caller-byte approximation in the separate pass without the agent. “Uninstrumented” means without this constructor measurement agent; Robolectric/MockK framework behavior remains part of the controlled fixture. Both passes retain bootstrap probe scaffolding; wrapper/bookkeeping overhead is not fabricated away.

There are no retained measured object/Class/ClassLoader handles between windows or in emitted rows. Span maps are cleared and nulled before row I/O, including action failure/end failure. Callbacks outside a window retain nothing. Finishing with an abandoned window clears it and invalidates the run. A positive, recorded default cap of 250,000 unique identities bounds each window (caller references are a subset, so at most twice the cap in the two identity maps). If the cap or callback fails, maps are released immediately, identity fields become null/invalid, ENTRY facts remain named as such, and end/finish invalidate execution. There is no silent sampling/truncation. Metadata strings and cumulative numeric ENTRY counters do not retain measured resource objects. No JVM allocation sampler/JFR estimate is labelled exact.

Explicit Robolectric delegation plus bootstrap loading preserve shared callback state; the real-sandbox gate verifies delivery before SDK setup. Selected types absent from final transformation coverage are missing/unloaded, not certified zero. Transform/counter errors, incomplete phases, nonzero retained references, invalid identity totals, legacy schemas, failed/skipped JUnit execution or incomplete Gradle forks prevent a successful report. Global identity totals must equal the sum of runtime-class buckets. Medians/min/max/raw counts remain separate for ENTRY and RETURN units; timing reports also retain per-session and per-fork medians. Warm VMs share a session/JVM and are correlated.

## Sessions, actual VM path and startup overlap

Each cycle seeds the same persisted conversation/message content and external ports with a fresh CoreLogic/qualified UUID user and unregistered client. SQL seed, CoreLogic/global setup and token registration precede `cold_session_get_or_create`; it times synchronous `CoreLogic.getSessionScope`, so this is a cold **session owner in a warmed Robolectric JVM**, not a process-cold launch. The inspected baseline/current provider implementations store tokens without opening that scope; the constructor pass also requires the cold span to enter `UserSessionScope`.

The startup-only control opens the session then observes `mandatory_startup_observation_no_vm`, requesting no VM/use case/read. The VM cohort measures first actual VM creation with a new store (including first narrow factory setup), retained lookup of the same key/owner, a fixed `mandatory_startup_overlap_observation` after persisted-read validation, then genuinely NEW VMs in NEW stores using the existing factory/session. Identity/factory-count assertions distinguish retention from creation. Actual persisted observer/lookup reads happen outside creation timing. No mandatory worker is removed or replaced to improve measurements.

These fixed observation intervals are **not startup-complete or idle barriers**. Mandatory startup can overlap session creation, first/warm VM construction, validation, unmeasured gaps and cleanup. Do not subtract controls and call the result pure DI work; lazy races, contention and scheduling differ. Final cumulative ENTRY facts include setup/warmups/reads/cleanup, while RETURN identity sets exist only in windows.

The fixture is derived from verified Android integration test `671eddea...`: real SDK/SQLite/repositories plus actual Android owner/provider and production current consumer adapter. AVS static initialization, WorkManager external job execution and the SDK's existing mock HTTP engine are the substituted external ports. Full registered-client/native crypto, full production application graph, physical-device/app-launch performance and actual background job execution are outside this controlled fixture.

Current-fixture bookkeeping uses a scalar consumer-creation count, with one temporary handoff reference for construction-failure cleanup. After the first window that reference is transferred to the retained first VM's cycle lifetime. Every warm store is cleared and its consumer drained/released outside all probe windows, including construction/read failures and cancellation; no list retains cleared consumers or grows inside VM creation timing. Same-owner/key identity checks, persisted-read checks and the real helper/adapter are unchanged. Real per-VM consumer/graph creation remains part of the migration cohort, while the legacy cohort still uses its actual session-owned accessors. The small count/handoff bookkeeping remains explicit fixture overhead.

Cleanup clears stores and drains consumers outside all windows, retaining the actual returned session. It captures that owner's already acquired database/network resources and lazy crypto wrappers before eviction. The shared cleanup helper requires completed global/session/initialized-Cells jobs, repeats the SDK shutdown after any best-effort timeout, then closes only acquired/initialized resources in the existing native/network/database order. Failed drain prevents resource closure; failed cleanup invalidates the run. It never opens a session or initializes an unused Cells/calling/crypto resource to clean it up.

Both templates embed a separate `WireKaliumMeasurementOwnerDrainTest` class. Six deterministic pre-cohort regressions hold non-cancellable session/Cells work across virtual provider timeouts and test cleanup deadline/failure behavior. `run-owner-drain-regression.py` runs that class separately, preserves logs/XML, and requires all six cases to execute and pass. Cohorts still select exactly one measurement testcase; their summary gate is unchanged. The reflection adapter is fixture-only and must execute successfully against both pinned SDK versions; no production API or test hook is added.

## Provenance and remaining execution limits

Baseline Kotlin `2.3.20` versus current `2.4.20`, Metro/KSP/coroutines/AGP/analyzer pins and provider policy are explicit confounds. Preserve Android's same cache policy across cohorts (`GLOBAL`, versus standalone SDK `LOCAL`). A whole-migration comparison can be useful; it cannot establish a DI-only causal saving when compilers/pins/runtime differ. A matched-toolchain control would be a separate parent-owned archive/build, never a silent baseline change.

Captured evidence includes exact Android/SDK SHAs, the complete baseline archive source manifest, original property/catalog text and hashes, fixture/tool/selection hashes, javac/ASM identity, JVM arguments, actual Test runtime classpath paths/sizes/mtimes, logs, commands and copied JUnit XML. Classpath entries are not content-hashed; preserve outputs/caches for stronger binary reproduction. Host load/thermal/power facts still need parent annotation. Actual migration observations, exact source snapshots and limits are recorded separately in the parent migration assessment/report; utility checks alone establish no migration result.

Primary facts: local ADR 0017/ledger/inventory and inspected committed factory/provider source; old/current emitted `CoreLogic` protected getter signatures; [JDK ThreadMXBean](https://docs.oracle.com/en/java/javase/21/docs/api/jdk.management/com/sun/management/ThreadMXBean.html); [IdentityHashMap reference equality](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/IdentityHashMap.html); [JVMS constructor initialization/return verification](https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-4.html#jvms-4.10.1.9.return); cached ASM 9.9.1/Kover 0.9.11 primary sources. The verifier smoke exercises the implemented before-super ENTRY and initialized-this RETURN insertion, rather than assuming bytecode validity.

Allowed verification: standalone Java build/`-Xverify:all` smoke (delegation, superclasses, equality independence, loader identity, worker/caller attribution, selected/unselected/failing constructors, overlapping/foreign/double windows, failure/abandonment/cap cleanup), report-unit rejection checks on that actual utility output, standalone source-manifest drift/fork-guard checks, Python/shell checks and Kotlin/Groovy syntax parsing. Android fixture type checking, Metro graph generation, Gradle init evaluation, actual SDK constructor/RETURN coverage and measurement execution remain parent-controlled. Their results and exact snapshots belong in the migration evidence. Standalone utility checks do not certify those paths.
