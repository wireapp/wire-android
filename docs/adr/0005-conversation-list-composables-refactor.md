# 5. Conversation list composables refactor

Date: 2024-10-01

## Status

Accepted

## Context

Conversation-list-related composables and screens (all conversations, archive) are overly 
complicated and have many leftovers after bottom tabs and multiple refactors, 
so in order to implement pagination, it's better to make a cleanup first.

## Decision

Simplify and unify composables related to creating screens that show conversation lists.
Remove old, unused code, like things that were implemented in order to make the bottom tabs or code 
leftover from previous refactors, and update to make it more readable, reusable and maintainable.
Make it more similar to what we have in other places - instead of having complicated "routers" or 
"bridges", just create "content" composable that can be used on multiple screens, 
but with the intention that common `ViewModel`s are incorporated into this content composable,
so that they do not have to be added each time, but make it possible to generate previews.
Also, make use of `AssistedInject` to reduce the number of functions and `LaunchedEffect`s needed.

## Consequences

- `ConversationRouterHomeBridge` is now replaced with simpler `ConversationsScreenContent`, 
  which is a single composable to be used to create multiple screens that show conversation lists.
- `ConversationListViewModel` uses `AssistedInject` to provide `ConversationsSource` and a `Flow`
  of search query `String`s directly in constructor instead of doing it using multiple functions.
- `ConversationListViewModel` and `ConversationCallListViewModel` are injected using unique keys,
  so that for each `ConversationsSource` there is a separate `ViewModel`. Thanks to that, together 
  with assisted injection, they are now dedicated to each type which makes it easier to maintain, 
  debug and even use on the same screen if needed.
- Both mentioned `ViewModel`s are now also interfaces that are implemented by the respective
  `ConversationListViewModelImpl` and `ConversationCallListViewModelImpl` and "preview" versions 
  of these interfaces are also created to make it possible to generate composable previews.
