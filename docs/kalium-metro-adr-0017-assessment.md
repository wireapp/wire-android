# ADR 0017 implementation assessment

ADR status remains Proposed. This assessment tracks implementation and evidence; it does not accept or merge the ADR. The migration ledger records exact source snapshots, delegated task IDs, findings and responses. The ownership inventory describes the baseline captures.

| Decision | Implementation | Verification state |
| --- | --- | --- |
| Individual Kalium construction | Qualified bindings replace global, user-root and every inventoried feature facade construction list. Public facades delegate to these bindings. Fresh facade graphs represent actual distinct captures/caches rather than package boundaries. | SDK/Android production graphs and generated code inspected independently. Style is now clean; current full-suite background SQLite failure and closure review remain pending. |
| Global/user/consumer lifetimes | Per-CoreLogic global graph, qualified-user generation graph and explicitly owned consumer extensions. Only selected ephemeral wrappers are consumer-scoped; existing user/global caches and external providers retain their owners. | Identity, factory, alias and concurrent first-resolution tests; actual generated scoped caches independently inspected. |
| Consumer construction | Three read paths construct their use cases inside the consumer graph: conversation details, message observation and message lookup. Use cases stay fresh; selected repositories share within one consumer. | Two consumers share underlying user resources while keeping separate wrappers; same-consumer message use cases share one repository. SQL-backed Android integration and owner/key retention/disposal tests pass. |
| Actual ViewModel ownership | Direct, assisted, manual and retained UI factory paths create a consumer only for a new ViewModel. Existing ViewModelStoreOwner/key determines retention. The activity-retained graph holds factories. | Factory/manual/integration and new API wiring tests: 31 passed on the fourth-layer source checkpoint; ten stability gates passed. |
| Legacy captures and facade behavior | Individual qualified capture providers retain aliases, cache distinctions, raw functions/Lazy values, and fresh-operation snapshots. Cells and authentication APIs borrow existing owners. | Per-family identity tests, additive ABI references, 4,461 governed Android members preserved. Swift/export execution remains pending. |
| Explicit runtime startup | Individual worker dependencies replace UserSessionScope init feature-container paths. LongWork's public constructor retains startup; prepared session ownership starts its monitor explicitly after registration. | Runtime idempotence/order/failure and provider lifecycle tests pass on JVM. Current Native ownership/runtime selection: 135 tests passed; Work 16 and Cells 322 passed. |
| Invalidation and cleanup | Private construction/startup ownership, ready-publication gate, generation invalidation, drain, failed-cleanup quarantine/retry and original platform cleanup order retained. Consumer teardown does not close parent resources or resolve unused lazies. | Full JVM suite passed 4,260 on the initial layer snapshot; corrected-source rerun observed one background SQLite failure. Investigation and closure pending. Current Apple ownership and restricted-runtime execution pending. |
| Provider cache policy | Storage/authenticated-network providers retain LOCAL/GLOBAL caches, exact removal and resource ownership outside graph SingleIn. | Fourth-layer LOCAL and GLOBAL storage 3/network 11, and account-resource access 15 pass. |
| KMP integration | Shared contracts/bindings/extensions and final Android/JVM/iOS device/iOS simulator/macOS roots; Work retains JS. Kotlin 2.4.20 required for Metro 1.4.5 Native ABI, with associated compiler-tool compatibility changes in the foundation. | Standalone and Android consumer production compilation passed; final response production/ABI rerun pending. Native/JNI and Swift integration pending. |
| Measured results | Reviewable constructor ENTRY and selected RETURN-identity instrumentation, separate uninstrumented time/caller-byte cohorts, actual retained ViewModel/SQL fixtures and startup-only controls. | Utility checks passed. Two measurement review rounds found fixture retention/provenance defects; retention and nested-source provenance closed by fresh reviews; actual runs pending. No savings claimed. |

Consumer disposal coverage deliberately remains the three specified read paths. New user and fresh-operation APIs retain the legacy host's account-quiescence obligations. They do not rewire prebuilt parent use cases into a consumer. Extending the consumer API to multi-repository operations, downstream flows or deferred paging requires whole-operation/load admission and drain; no partial guard is claimed here.

The earlier SQLite IOERR_FSTAT account/key/collector was not uniquely identified. The test fixture had a source-supported teardown gap and now retains/drains actual owners, closes captured drivers and then removes files. Current suite passes do not establish unique historical attribution or an environmental cause.

Remaining completion milestones:

- [x] Foundation global/user/consumer graphs and real ViewModel creation paths, with fresh review closure.
- [x] Cached/global and fresh/authentication/operation/Cells/Work construction, with independent checkpoint reviews.
- [x] User-root individual bindings, Audio worker and all audited Android DI bridges implemented and reviewed for correctness.
- [ ] Close the fourth-layer style response on exact follow-up source and complete relevant reruns.
- [ ] Execute current Native/Apple lifecycle, restricted extension/Swift receiving and SDK export compatibility checks.
- [ ] Complete provider-policy gates, measurement tooling closure and actual before/after cohorts; publish results and limits.
- [ ] Fresh final identity/platform closure reviews, final PR descriptions/registration verification and ADR assessment.

Eight registered draft implementation PRs currently form two four-layer stacks: Kalium #4563 → #4564 → #4566 → #4567; Android #5418 → #5419 → #5421 → #5422. Kalium's first layer is based on ADR #4558. No merge or deployment is authorized.
