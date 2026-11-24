# android-release quick start

The `android-release` skill cuts a Wire Android beta or ships a production version from a `release/cycle-<cycle>` branch. An agent drives `scripts/release.sh` and asks you before every outward step. `SKILL.md` has the full steps and the guards behind them.

## Start it

The skill is user-invoked only (`disable-model-invocation: true`). An agent never starts it on its own.

- Claude Code: type `/android-release beta <ver>` or `/android-release prod <ver>`.
- Codex: type `$android-release`.

Start a new session after you pull, so the skill appears in the list. `<ver>` is the release version, for example `4.39.0`. The cycle is the minor line, `4.39`.

## Safety model

- Commands that write print `PLAN: ...` and stop. That covers branches, tags and workflow dispatches, plus the local `worktree` and `cleanup` commands.
- A command writes only when `--yes` is the last argument.
- The agent shows you the plan and asks before each `--yes` run. A yes for one beta does not cover the next.
- A refusal (`error: ...`) means a guard failed. The agent reports it and does not work around it.

## Beta flow

1. `check <ver>` confirms the version and the previous production tags.
2. Confirm in Jira that every ticket with Fix Version `<ver>` is Done.
3. First beta of the cycle only: `plan <ver>`, then `cut`, then `verify <ver>`.
4. `validate <ver>`, then `tag <ver> <n> <tip-sha>`.
5. `tag-kalium <ver> <n>`. Push the Android tag first.
6. Watch the `Beta release` run.
7. `changelog`, then `announce`. Proofread the notes, then post them yourself.
8. Once per cycle, open a PR that bumps `versionName` on `develop`.

## Production flow

1. `prod-check <ver>` shows the tip, the Kalium pin, the commits since the last beta and the release-note status.
2. The copywriters write the English and German notes. Run `worktree <ver> notes`, then `notes <ver> <en-file> <de-file>`. Open a PR into `release/cycle-<cycle>` and merge it.
3. Run `prod-check <ver>` again. It must exit 0.
4. `tag <ver> prod <tip-sha>`. The tag push publishes the GitHub release, which starts `Production`. Play holds the build under Managed publishing.
5. `tag-kalium <ver> prod`.
6. Wait for `Production` to succeed.
7. `regression <ver>` dispatches the QA prod regression.

## Run the script directly

From the main checkout:

```sh
.claude/skills/android-release/scripts/release.sh help
.claude/skills/android-release/scripts/release.sh prod-check 4.39.0
```

These commands are read-only. For outward commands, run without `--yes` first and read the plan.

## What stays manual

- The QA verdict.
- The Play Console publish and rollout.
- The GitHub Latest flag.
- The Jira release status.

## Prerequisites

- `gh` authenticated for the Wire repositories.
- Git push rights for tags on wire-android.
- `jq`.
- `python3`.
