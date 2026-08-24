# Authentication feature

`:features:authentication` is the Android-first authentication library. Its public contracts use
feature-owned values and semantic outcomes so the boundary can later move to Kotlin Multiplatform
without exposing application or Kalium implementation types.

```text
:app -> :features:authentication -> core modules
```

## Feature ownership

The feature owns the authentication state engines, gateway interfaces and substantive Compose
presentation for Welcome, legacy Login, New Login, Create Account, register/remove device and E2EI
enrollment. It also owns serialized authentication route DTOs that are independent of the host
navigation runtime, verification-code primitives, authentication dialogs and their exclusive
localized resources.

Important public presentation entries include `WelcomeScreenContent`, `LoginScreenContent`,
`LoginEmailContent`, `NewLoginContent`, the Create Account `*Content` composables,
`RegisterDeviceContent`, `RemoveDeviceContent`, `E2EIEnrollmentContent`, `ServerTitleContent`,
`LoginErrorDialog`, `SsoIdentityChangedDialog` and `AuthenticationFailureDialogContent`.

## Permanent application adapters

The application remains the composition root. It owns Navigation 3 entry registration, router and
back-stack mutation, OAuth/deep-link ingress, Custom Tabs and activity/lifecycle integration,
Metro scopes, account/session switching, analytics, `BuildConfig` policy, datastore providers and
all concrete Kalium gateways.

Thin app presentation adapters are intentionally retained for:

- `ServerTitle`, which maps `ServerConfig.Links` and host-localized server details into
  `ServerTitlePresentation`;
- `LoginErrorDialogMapper`, which maps `CoreFailure`, SSO failure codes and update-app actions into
  feature dialog presentations;
- `AuthenticationFailureDialog`, which maps shared host error strings into the feature renderer;
- `AuthenticationLegacyMappers`, which bridges host `ServerConfig` and deep-link values to the
  serialized feature route contracts;
- `ServerConfigAuthenticationExtensions`, which contains proxy policy for host `ServerConfig`.

`E2eiCertificateDetailsRoute` remains an explicit host exception: the same serialized route serves
both the during-login certificate flow and the active settings device-details screen. Moving or
splitting it without a coordinated route migration would change restored back-stack identity and
settings ownership. Its payload is KMP-safe and contains no Kalium type.

`InitialSync` and legacy registration are feature-owned. The feature owns their route identity,
state machines, validation, registration policy, substantive Compose surfaces and auth-exclusive
localized resources. The app retains only the Navigation 3 completion action, Custom Tabs/dialog
slots, Metro construction and concrete Kalium, datastore, analytics and automated-login gateways.
Initial-sync completion is emitted only after the host gateway has persisted its durable marker and
consumed any in-memory automated-login backgrounding request.

## Forbidden dependencies

Feature production code must not import or depend on `:app`, Kalium, Metro, app datastore/config,
`BuildConfig`, concrete Navigation 3 runtime/router/style APIs, or unrelated feature modules. Public
feature APIs must not expose host implementation types.

## Required gates

Run these gates for authentication changes:

```text
./gradlew -Dorg.gradle.java.home=/Users/jakub.zerko/.jenv/versions/21.0 \
  :features:authentication:testDebugUnitTest \
  :features:authentication:lintDebug \
  :app:compileDevDebugKotlin
```

Add focused app adapter, route and host tests for the slice being changed. The feature-wide boundary
test enforces production import/dependency purity, permanent adapter ownership, deleted bridges and
documented host route exceptions.
