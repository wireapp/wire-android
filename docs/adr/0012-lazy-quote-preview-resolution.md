# 12. Lazy Quote Preview Resolution

Date: 2026-06-09

## Status

Proposed

## Context

The main Kalium message-list query uses `MessageDetailsView` as a hot path for conversation rendering. Quote previews were hydrated directly in this view through extra `LEFT JOIN`s to the quoted message, quoted sender, and quoted content tables.

This made every message-list query pay the cost of quote hydration, even when most visible messages do not render a quote preview. The cost is more noticeable because the local database uses SQLCipher, where larger views and extra joins can be more expensive.

We still need to preserve current quote behavior:

- quoted message previews should update when the quoted message is edited
- deleted or unavailable quoted messages should render as deleted/unavailable
- invalid quote references must not expose quoted content
- cross-conversation quote previews should be able to show the source conversation name
- private reply drafts and quoted assets must keep using the quoted message conversation id
- we need to extewnd it to include conv name for the reply in private feature

## Decision

Remove quoted-message hydration from Kalium's `MessageDetailsView` and keep only quote reference data in the main message-list query:

- quoted message id
- quoted message conversation id
- quote verification state

Kalium message mappers will continue exposing quote references on text and multipart messages, but the main message-list query will no longer populate quoted message details.

The app will resolve quote previews lazily only when a rendered message needs one:

1. Map unresolved quote references to an app UI quote reference model.
2. Resolve that reference through an observer of the quoted message row.
3. Keep the resolver scoped to the current conversation screen lifetime.
4. Cache quote preview state by quoted conversation id, quoted message id, and verification state.
5. Render already resolved quote data directly, unavailable data with the current unavailable UI, and unresolved references through the lazy preview state holder.

For invalid quote references, the resolver returns invalid quoted content without exposing the quoted message body.

For cross-conversation quotes, the resolver may also observe conversation details to derive a source conversation display name:

- group/channel: conversation name
- one-to-one: other user display name
- failure or null: omit the source conversation name

The source conversation name is rendered in the quote header only when present.

## Consequences

### Positive

- The hot message-list query becomes smaller and avoids quote hydration joins.
- SQLCipher has less work to do for ordinary message-list rendering.
- Quote previews remain live with respect to quoted message edits and deletes.
- Cross-conversation quote previews can show source conversation context without adding more joins to the main view.
- Quote asset loading can use the quoted message conversation id instead of assuming the current conversation.

### Trade-offs

- Quote rendering now has a second-stage lazy resolution path in the app.
- Visible quoted messages may trigger additional per-quote observers.
- The app owns a conversation-scoped quote preview cache and must keep its lifecycle aligned with the conversation screen.
- Initial quote preview rendering may briefly show loading-style fallback until the quoted message observer emits.

### Maintenance rules

When changing quote preview behavior:

1. Keep `MessageDetailsView` limited to quote reference fields; do not reintroduce quoted-message hydration joins without revisiting this ADR.
2. Preserve live quote updates by observing the quoted message row, not by one-shot loading keyed only by message id.
3. Keep invalid quote references from exposing quoted content.
4. Include quoted conversation id in quote preview cache keys and asset loading paths.
5. Resolve source conversation names only for cross-conversation quotes.
6. Cover text and multipart quote references in mapper tests, and cover edited, deleted/unavailable, invalid, same-conversation, and cross-conversation cases in resolver tests.
