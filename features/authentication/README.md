# Authentication feature

This Android library owns authentication presentation contracts and, as the extraction
progresses, authentication state, UI, and behavior. It is intentionally Android-only today while
keeping its public boundary suitable for a later Kotlin Multiplatform migration.

Dependency direction:

```text
:app -> :features:authentication -> core modules
```

The application host retains Navigation 3 entry registration and back-stack mutation, OAuth and
deep-link ingress, activity/lifecycle integration, Metro root and session composition, concrete
Kalium-backed gateway implementations, account/session switching, analytics wiring, and
BuildConfig/flavor policy.

Feature public APIs must use feature-owned value types and semantic outcomes. They must not expose
Kalium implementation types or depend on `:app` or unrelated feature modules.

The feature owns register-device presentation and verification state, generic remove-device
presentation/state, E2EI enrollment state/content contracts, and pure post-login requirement
priority. The app supplies concrete device rows, dates/fingerprints, shared dialogs, OAuth,
Kalium gateways, session cancellation, and Navigation 3 route transitions.
