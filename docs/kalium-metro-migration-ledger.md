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
- [ ] Record getter declarations, constructor captures, identity aliases and runtime startup baseline.
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

- Android selects Kotlin `2.3.20`, Metro `1.4.5`, and provider cache policy `GLOBAL`; Kalium selects Kotlin `2.3.20` and provider policy `LOCAL`.
- The existing ADR checkout reports a Git top-level inside the original submodule Git directory. It is not safe for mutations and is being read only.
- Metro tag `1.4.5` primary documentation inspected: `docs/dependency-graphs.md`, `docs/scopes.md`, `docs/multiplatform.md` at https://github.com/ZacSweers/metro/tree/1.4.5/docs.
- No measurement or behavioral-test success is claimed yet.
