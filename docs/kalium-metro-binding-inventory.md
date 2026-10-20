# Kalium migration ownership inventory

Baseline: Kalium `92e54877ee318fe6c46aae3a3832e78b3b1adf8b` (ADR branch on investigated develop). The migration ledger records implementation/review snapshots and verification. This inventory describes existing owners; it does not mark all rows migrated.

| Existing owner/accessor | Declaration and capture behavior to preserve |
| --- | --- |
| `CoreLogic` / `GlobalKaliumScope` | One global owner/graph per CoreLogic. Global coroutine job, callback manager, configured providers and scheduler owners remain external instances. Global scope has both fresh repository/use-case getters and retained bindings. |
| Authentication provider | Separate cache keyed by server configuration plus proxy credentials. No authenticated user graph is needed for login. Authentication's retained current-server-config function remains live; its eager two-factor repository currently forces lazy network acquisition. |
| `UserSessionScope` | One active qualified user and generation. 331 property declarations include resources, state, factories, captures and use cases; moving just feature facade constructors does not migrate this owner. |
| `conversations` | Cached facade. Its repository capture differs from other fresh conversation wrappers. Its message repository aliases the cached message facade's capture. Incoming/outgoing typing state, sender, work queue and cached use cases retain user ownership. |
| `messages` | Cached facade. Fresh `messageRepository` getter captured once here. Send-text use case, confirmation/deletion handlers, timeline state, encoder and deferred cycles retain their captures and cached/getter distinctions. |
| Conversation/message repositories | Both user-owner getters are factories. Consumer wrappers deliberately share within one consumer. The old message facade's Nomad paging coordinator remains user-owned and shared by those consumers; other factory-captured coordinators remain distinct. |
| Draft/slow-sync/incremental-sync repositories | Cached user bindings, shared across consumers. Do not infer a transient lifetime from the conversation/message repository policy. |
| `client` | Cached facade; 27 constructor captures and 19 members. Startup recovery reaches this path. |
| `users` | Cached facade; 38 captures and 51 members. Public captured refresh-users binding, cached personal-to-team support, and getters returning captured team-URL/protocol use cases must retain aliases. |
| `channels` | Cached facade; four deferred function captures. Invoking a deferred provider must not turn it into a cached returned repository. |
| `search` / `meetings` | Cached facades. Search has seven captures/four members; meetings ten captures/ten members. Meeting facade repositories and separately constructed worker captures can differ. |
| `debug` | Cached facade; 32 captures, 32 members and two public functions. Distinct encoder/self-deletion captures and retained repair worker; startup reaches this path. |
| `team` / `service` / `apps` / `connections` | Fresh facades with fresh use-case getters. Preserve constructor captures per facade, including Apps' API-version snapshot at facade creation. |
| `calls` | Fresh facade with 18 captures and 47 members, including three cached members per fresh facade. Calling manager/native resources stay at their established owners. |
| `backup` | Fresh facade with 14 captures and 13 members. Each facade owns its mutex, security helper and cached crypto-backup/upload use cases. Do not promote these to a user singleton. |
| Multiplatform backup | Fresh facade; seven captures, two members and restore overload accepting a progress dispatcher. Export/import operation state and buffers retain operation ownership. |
| Registration / SSO / session | Existing fresh facades under authentication/global ownership. Registration has three captures/two members; SSO four/seven; session one/six. |
| Pending-login system settings | Temporary unauthenticated network per request; close in `finally`, including cancellation. |
| `CellsScope` | Cached user owner with its own SupervisorJob, download client, mutex and node-service cache. Six constructor captures, 69 properties, 53 public lazies; fresh file downloader can be captured by a cached download-version use case. Keep `isCellsInitialized` and public constructor behavior. |
| `LongWorkScope` | Cached owner with its own in-memory repository and two retained observers. Starts the initial-sync monitor on construction. Public constructor behavior must remain compatible. |
| Restricted Apple runtimes | NSE/share/notification processing remain below the full app logic runtime, with their lease/resource ownership. They must not acquire a full application user graph. |

Cross-feature paths also need individual bindings: asset audit to users; protocol/key managers to client/users; configuration sync to channels; calling handler to mute-call; logout to client/calls; loudness work to the message facade. Package names alone do not justify child graphs. A fresh facade/operation with its own captures, mutex or cached values is an actual ownership boundary.

Storage and authenticated networking continue to use their existing LOCAL/GLOBAL provider policies and exact removal keys. Kotlin objects, configured external caches, Apple asynchronous preparation, background suspension, failed-cleanup quarantine and native calling shutdown keep their existing owners.

## Construction wiring after simplification

The internal root recipes now live in six groups: constructor aliases/cache adapters, resource factories, sync, events, deferred callbacks, and explicit factories. These are binding interfaces composed into the same user graph, not additional runtime graphs or owners. The ten numeric file groups are removed.

Full-argument, exact-type constructors use `@Inject`. Only the original qualified API binding is cached where it was cached before; implicit implementation requests remain unscoped. Explicit recipes remain for omitted defaults, concrete-to-interface captures, repeated resource reads, callbacks, external/provider caches and order-sensitive construction. The four narrow owner resource/state APIs are composed through `@Includes` with their original qualified getter keys; raw function and `Lazy` values retain opaque wrappers.

Unused internal entrypoints can be removed without deleting their construction bindings or changing public facades. The simplification ledger records the exact checkpoint, code reduction and fresh validation separately from the preceding migration reviews.
