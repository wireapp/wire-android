# 18. Conversation feature module spine

Date: 2026-08-23

## Status

Accepted for the `chore/android-modularization` branch at baseline commit
`b2b5089465eb20552be41303efa9d0c4741aaea5`.

## Decision

Create the empty Android-only `:features:conversation` module in
`features/conversation`, with namespace `com.wire.android.feature.conversation`.
Root settings auto-discovery includes the module because it is a direct `features/*`
directory with `build.gradle.kts`; no settings change is required.

The module has no production source in this spine commit. Its only production
classpath is the enforced Compose BOM and Compose Runtime: the standard Compose compiler
is intentionally active, requires Runtime on every Kotlin compilation classpath, and the
terminal feature is Compose UI. All other production dependencies remain demand-driven.
The module applies the shared Android-library, Kover, JUnit5, Compose compiler, and
Compose stability conventions. Metro and Compose remain convention-enabled for the later
feature source; this commit intentionally does not add KSP, serialization, or parcelize.

The only new Gradle edge is one-way:

```text
:app -> :features:conversation
```

`:features:conversation` must never depend on `:app`.

## Terminal boundary

The strict candidate inventory is 250 production files, 59 unit-test files, and one
androidTest file under `app/src/**/ui/home/conversations`. The audited terminal
closure also contains `messagecomposer` (41 production, 13 unit, 2 androidTest),
`conversationslist` (27 production, 3 unit), and `gallery` (6 production, 1 unit),
plus the explicitly audited conversation mapper, audio, markdown, edit, emoji, and
imported-media slices. Existing declarations already owned by core modules remain
there even when their legacy Kotlin package names include `conversationslist`.

Kotlin packages are preserved during the terminal move to avoid source-import and
Navigation3 identity churn. This ADR authorizes no source, resource, manifest,
Navigation3, DI, Metro, Kalium, KMP, iOS, or behavior change in the spine commit.

## Planned gates

1. Spine: this empty feature and inbound app edge.
2. App-host ports and flavor/config injection.
3. Shared leaves and resource ownership.
4. Navigation3 feature contracts with app assembly adapters.
5. Terminal conversation SCC move and source/test/resource transfer.

## Terminal acceptance

The terminal change must prove the one-way dependency law and no remaining
conversation implementation in app:

```sh
git ls-files 'app/src/**/ui/home/conversations/**'
git ls-files 'app/src/**/ui/home/conversationslist/**'
git ls-files 'app/src/**/ui/home/messagecomposer/**'
git ls-files 'app/src/**/ui/home/gallery/**'
rg -n '^import com\.wire\.android\.(BuildConfig|R|ui\.calling|navigation\.runtime)' \
  features/conversation/src
./gradlew :features:conversation:testDebugUnitTest \
  :features:conversation:compileDebugKotlin :app:compileDevDebugKotlin
```

The first four commands must expose no conversation implementation, allowing only
explicitly documented app host adapters outside those paths. The final Gradle command
must succeed; representative nonfree and fdroid variants must also compile when the
terminal source/config work lands.

## Stop conditions

Stop rather than weakening the boundary if any feature source requires `:app`, app
`BuildConfig`, app `R`, app Metro assembly, a Navigation3 route/result identity
change, or a resource ownership conflict. Keep Workers, services, providers,
deep-links, Kalium changes, and unrelated calling extraction out of this effort.
