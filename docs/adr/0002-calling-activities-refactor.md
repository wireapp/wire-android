# 2. Calling activity refactor

Date: 2024-08-01

## Status

Accepted

## Context

To support a second incoming call we need to refactor the code so we can handle the ongoing content
context, without losing the current context.

This is a retroactive decision record implemented
on https://github.com/wireapp/wire-android/pull/3264

## Decision

Create 2 separate activities, one for the Incoming/Outgoing calls and another for the ongoing call.
In this way, we can keep the context of the ongoing call and handle the incoming/outgoing calls.

The design and interaction will look like this:

<img src="https://github.com/user-attachments/assets/66f19cce-c2bc-4777-a0eb-b5cda035df8a"/>

## Consequences

- StartingActivity will handle Incoming and Outgoing calls content, these contents are disposable
  and can be recreated when receiving a new call.
- OngoingCallActivity will handle the ongoing call content, this content is not disposable and
  should be kept during the call.
