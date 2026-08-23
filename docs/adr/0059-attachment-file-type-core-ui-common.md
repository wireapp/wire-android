# ADR 0059: Own attachment file-type classification in core UI common

**Status:** Accepted
**Baseline:** `6a0b2f368`, `chore/android-modularization`

## Decision

Move `AttachmentFileType` and its extension/MIME classification from
`:features:cells` to `:core:ui-common`. Preserve the existing
`com.wire.android.feature.cells.domain.model.AttachmentFileType` package as a
compatibility namespace, so current app and Cells imports remain stable while
the physical owner becomes neutral.

Keep the `icon()` and `previewSupported()` extensions in `:features:cells`.
`icon()` remains the sole owner of Cells `R.drawable.ic_file_type_*` mapping;
the core contract has no Cells resources or feature dependency.

## Consequences

The conversation feature already exposes `:core:ui-common`, so a later quoted
multipart message extraction can classify MIME types without adding a
conversation-to-Cells Gradle edge. Cells already depends on core UI common and
continues to own its presentation mapping. No resources or Gradle declarations
change in this prerequisite.

The retained package name is intentional for this review-friendly transition.
A package rename is separate work and must not be coupled with the ownership
move.

Focused tests preserve MIME and extension classification, the `text/csv`
spreadsheet precedence, Cells icon mapping, media preview support, and the
physical ownership boundary.

## Verification and stop conditions

Run from the repository root with JDK 21:

```sh
git diff --check HEAD
git diff --summary --find-renames=80% HEAD
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :core:ui-common:testDebugUnitTest \
  --tests com.wire.android.feature.cells.domain.model.AttachmentFileTypeTest \
  :features:cells:testDebugUnitTest \
  --tests com.wire.android.feature.cells.domain.model.AttachmentFileTypePresentationTest \
  --tests com.wire.android.feature.cells.domain.model.AttachmentFileTypeOwnershipSourceTest
JAVA_HOME=/Users/jakub.zerko/.jenv/versions/21.0 ./gradlew \
  :core:ui-common:compileDebugKotlin \
  :features:cells:compileDebugKotlin \
  :features:conversation:compileDebugKotlin \
  :app:compileDevDebugKotlin \
  :app:compileFdroidDebugKotlin
```

Stop rather than widening this slice if a caller requires the Cells icon
extension from a neutral module, an app-to-core dependency is needed, or a
resource-backed mapping remains in core.
