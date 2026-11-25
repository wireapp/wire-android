---
name: android-release
description: Release Wire Android from a release/cycle branch. Beta path cuts the cycle branches, tags betas and Kalium, drafts the changelog and bumps develop. Production path lands the copywriters' release notes, tags the production build and Kalium, and starts the QA prod regression.
disable-model-invocation: true
---

# Android release

This skill has two paths. Steps 1 to 4 cut a **beta**. The **Production** section ships a production version from the same cycle branch. Ask which one the user wants if it is not clear.

Source: the Confluence how-tos "Wire Android Beta version release" and "Wire Android Production version release" (space AR). They are reference only and can be out of date. Also see `docs/beta-releases.md` and `docs/adr/0016-beta-releases-from-release-cycle-tips.md`. If this file conflicts with them, the repo docs win.

Betas ship at the end of each sprint, every two weeks, at the end of the week.

A beta is an annotated tag `vX.Y.Z-beta.N` on the **current remote tip** of a `release/cycle-*` branch. The tag starts `Beta release` (`build-beta.yml`). It builds the tagged commit with its pinned Kalium, deploys to S3 and the Play track, and publishes a GitHub prerelease. Kalium gets its own tag `android-vX.Y.Z-betaN` (no dot before `N`) that you create.

## The script

Every command lives in one script. Run it from the main checkout. `${CLAUDE_SKILL_DIR}` is this skill's folder. Agents that do not set it use `.agents/skills/android-release`.
```sh
rel=${CLAUDE_SKILL_DIR}/scripts/release.sh
$rel help
```
`ver` is the release version, for example `4.39.0`. The cycle is the minor line, `4.39`. Read every SHA from script output. Never retype one or paste a version literal into a command.

| Command | Effect | Step |
|---|---|---|
| `check <ver> [<prev>]` | read-only | 1a, 1b |
| `plan <ver>` | read-only | 1d |
| `verify <ver>` | read-only | 1d |
| `validate <ver>` | read-only | 2 |
| `changelog <ver> <n> <prev>` | read-only | 4 |
| `announce <ver> <n>` | read-only | 4 |
| `cut <ver> <android-sha> <kalium-sha>` | **outward** | 1d |
| `recut <ver> <new-a> <new-k> <old-a> <old-k>` | **outward, destructive** | 1d re-cut |
| `tag <ver> <n> <tip-sha>` | **outward**, tag (signed if signing works) | 2 |
| `tag-kalium <ver> <n>` or `<ver> prod` | **outward** | 1b, 2 |
| `worktree <ver> <topic>` | local, dry run first | worktree flow |
| `cleanup <ver> <topic>` | local, dry run first | worktree flow |
| `checks <ver>` | local, slow | worktree flow |
| `prod-check <ver>` | read-only | P1, P3 |
| `notes <ver> <en-file> <de-file>` | local | P2 |
| `tag <ver> prod <tip-sha>` | **outward**, starts Production | P4 |
| `regression <ver>` | **outward**, dispatches QA | P5 |

An outward command, `worktree` and `cleanup` check every guard, prints `PLAN: ...` and stops. It writes only when `--yes` is the **last** argument. So: run it without `--yes`, show the user the plan, wait for an explicit yes, then repeat it with `--yes`. A refusal (`error: ...`) means a guard failed. Report it. Do not look for a way around it.

## Rules

- Creating a branch or tag, dispatching a workflow, pushing and opening a PR are outward-facing. Show the plan, wait for an explicit yes, and only then act. Approval for one beta does not cover the next.
- Never move, delete or re-push a published beta tag. Never rerun a published release. Existing tags and old branches stay as they are. Do not sign, replace or delete them.
- Never push to `develop` or a release-cycle branch directly. The version bump goes through a PR.
- Draft the Jira and chat messages in the conversation. Post nothing under the user's account.
- Push tags with the driver's own git credentials. Never ask for, print or store a token.
- Signing is best effort. Try `git commit -S` for commits. `release.sh tag` tries to sign the tag. If signing is not set up or fails, continue unsigned (`git commit --no-gpg-sign`) and tell the user. Do not change the signing configuration. Do not stop the release over a signature.
- Put every message you draft for the user to post (Jira, chat, announcement) in a fenced block, with the real version and tag filled in, so they can copy it.
- Every beta and every production release needs its Kalium tag. No workflow creates it.

## 1. Prepare

Get `X.Y.Z` from the Release calendar. Ask the user if it is not given.

**a, b. Check the version and the previous production release.** Run `check <ver>` to list the candidates for the previous release. If it shows a hotfix (for example `4.37.1`), say so and let the user pick `prev`. Then run `check <ver> <prev>`. It reads remote tags, because local tags can be stale.
- Exit 2, wire-android tag `v<prev>` missing: the production app was not built and tested. **Stop.** Tell the user to contact the release owners for the production app and postpone the beta.
- Exit 3, only Kalium `android-v<prev>` missing: the app shipped but Kalium was not tagged. The script prints the commit the production app pinned. Show it, check that an older `android-vN` tag points at the commit pinned by wire-android `vN`, then run `tag-kalium <prev> prod` and, after a yes, `tag-kalium <prev> prod --yes`. Re-run the check. Kalium beta tags such as `android-v4.38.0-beta1` are not production tags.
- Version differs from `X.Y.Z`: stop and ask. Do not edit it yet.

**c. Check the fix-version tickets.** Every ticket with Fix Version `X.Y.Z` must be Done. You cannot read Jira from here, so output this message in a fenced block for the user to post, with the real versions filled in, and ask them to confirm the result:
```
Beta <ver> is cut today. Check every ticket with Fix Version <ver>.
- Still To Do or In Progress: update the status, or move the Fix Version to <next> if the work did not merge.
- Merged but still in QA or Design review: QA, Design and the dev agree who tests what is left.
Reply here when the list is clean.
```

**d. Create the release-cycle branches from `develop`** (first beta of the cycle only).

Name both branches with the minor line only: `release/cycle-<cycle>` and `release/android-cycle-<cycle>` (for example `release/cycle-4.39`). Do not add the patch number. One branch then carries `4.39.0`, and later `4.39.1`, `4.39.2` and so on. Each release bumps `versionName` on the branch. The workflow and `validate-beta-source.py` only require the `release/cycle-` prefix.

Run `plan <ver>` and show the user the result. The beta builds whatever Kalium commit the Android branch pins, and Kalium's `develop` can be ahead of it.
- `status=identical`: one candidate. Use `kalium_pin`.
- `status=ahead`: two candidates. **Suggest `kalium_pin`.** It is what `develop` builds and tests, so the app and Kalium are known to match. Offer `kalium_tip` if the user wants the newer fixes. List the `ahead` commits and name any breaking (`!`) commit. A breaking commit needs a matching app change, so say whether `develop` already has it.
- `status=behind` or `diverged`: stop and ask.

After the user chooses, run `cut <ver> <android_develop> <kalium_pin>`, show the plan, and repeat with `--yes` after approval. If a run stops after it created only one of the two branches, run it again. A branch already at the wanted commit is skipped. **Cut the Kalium branch at `kalium_pin` even when the user chose the tip.** The script refuses any other commit, because Android's branch pins `kalium_pin` until the bump lands. Move to the tip afterward through the worktree flow.

Then run `verify <ver>`, report the Kalium SHA and ask the user to confirm it is the intended commit. If it exits non-zero, stop.

**If the branches already exist, or `develop` moved after you cut them.** `plan` prints the branch tips, the beta tags, how many `develop` commits the branch lacks and which commits only the branch has.
- **No beta tag yet, and the user wants the branch to follow `develop`:** re-cut it. Show the user the commits only on the branch, because a force update drops them. Run `recut` with the old tips you showed, and repeat with `--yes` after approval. It moves both refs together and aborts if a tip moved or a beta tag exists.
- **A beta tag exists:** never move the branches. Take the wanted `develop` fixes onto the branch with cherry-picks in the worktree flow.
- **Old branches under a different name** (for example `release/cycle-4.39.0`, or `release/cycle-4.4` from an earlier run): create the new ones, then ask the user before deleting the old ones. Deleting a remote branch cannot be undone. `release/*` is protected, so the user may need to lift the rule.

For later betas of the same cycle, skip this step. Use the existing branches and apply stabilization fixes there (cherry-pick or commit on the release-cycle branch). They need not reach `develop`.

### Change the cycle branch in a worktree

Any change to a cycle branch needs local checks first. That covers a Kalium pointer bump and cherry-picked fixes. Work in an isolated worktree. Do not use the main checkout, whose branch and `kalium` pointer may be dirty.

1. `worktree <ver> <topic>` creates `../wire-android-release-<cycle>` on `chore/release-<cycle>-<topic>` from `origin/release/cycle-<cycle>`, with the submodule and `local.properties`. Run it without `--yes` first, show the plan, then repeat with `--yes`. Nothing is pushed.
2. Make the change inside the worktree.
   - **Kalium bump:** `cd kalium && git fetch origin && git checkout <sha>`. Show the user `git log --oneline <old>..<new>` first and name any `!` commits. If the old pin is not an ancestor of the new one, stop and ask.
   - **Cherry-pick:** `git cherry-pick -x <sha>`. If it fails to compile, check the Kalium pin first.
3. Inside the worktree, run `$rel checks <ver>` in the background (use the absolute path of `release.sh`). It compiles the app and its tests, runs the unit tests and `detektAll` in one Gradle invocation, online. Do not start a second Gradle build in the same worktree.
4. Read the output. It prints the exit code, compile errors, failures, tasks that reused an earlier result and the test count. A compile failure means the tests never ran, and a green `detektAll` then proves little. `detektAll` covers `:app` only. Report the exit code, each task's result and any failures. Do not fix unrelated failures. Say what you could not run.
   - A removed Kalium type shows up as `e: ... Unresolved reference`. Find the commit with `git -C kalium log --oneline -S<symbol> <old>..<new>`. Then either bump to its parent (after checking the old pin is an ancestor) or ask the user to bring in the matching app change. Do not patch the app on a cycle branch without asking.
5. **Land it:** after the checks are green, commit on the scratch branch (signed when possible, see Rules) and show the diff. With approval, push the scratch branch and open a PR **into `release/cycle-<cycle>`**. Merge the PR before releasing, then re-read the tip in step 2.
6. **Clean up:** when the checks are green and the user moves on (the PR is merged, or the user drops the change), run `cleanup <ver> <topic>`, show the plan, then repeat with `--yes`. It removes the worktree and the scratch branch, locally only. It refuses if the worktree has uncommitted changes, or if the branch has commits that no remote branch contains and no merged PR has. Do not clean up while the user still waits on a review or a re-run.

Do not split the checks across subagents or a second Gradle run. Gradle already runs independent tasks in parallel, and a second build in the same worktree waits on the first one's locks. A fresh-context subagent is worth using for an independent read of the diff (the pin range, the `!` commits, the cherry-picks) while the build runs.

## 2. Release

**Validate.** `validate <ver>` prints `next_beta`, `tip`, `kalium_pin` and `versionName`, checks that `BETA_APP_TRACK_INTERNAL` is set in wire-android and runs the repo's source validation. Beta numbers restart at 1 for each version. A second release from the same branch, such as `4.39.1`, first needs `versionName` bumped on `release/cycle-<cycle>` through the worktree flow. The tag then reads `v4.39.1-beta.1`.

**Confirm** the branch, `tip`, tag `vX.Y.Z-beta.N` and `kalium_pin` with the user. Then tag.

**Tag.** `tag <ver> <n> <tip>` guards that the branch did not move, `versionName` matches, the tag is free and `<n>` is the next number. Run it, show the plan, then repeat with `--yes`. It pushes with the driver's credentials. It signs the tag when signing works. Otherwise it says so and creates the tag unsigned. A "creations being restricted" notice from the remote is the ruleset bypass warning, not a failure. A tag left behind by a failed push is reused if it is annotated and points at the tip.

**Tag Kalium.** Push the Android tag first, because the Kalium tag follows the commit it pins. Run `tag-kalium <ver> <n>`, then repeat with `--yes` after approval. It creates `android-v<ver>-beta<n>` at the Kalium commit that the Android tag pins, through the API, and checks the result.

**Watch.** The tag push starts `Beta release`.
```sh
gh run list --workflow build-beta.yml --limit 1
gh run watch <run-id> --exit-status
gh release view vX.Y.Z-beta.N                         # prerelease exists
```
Report the final state of each job.

**Retry a failure**

| Situation | Action |
|---|---|
| Downstream job failed | **Re-run failed jobs** on `Beta release`. It reuses the passed validation. |
| Fresh run at the same tag | `gh workflow run build-beta.yml --ref vX.Y.Z-beta.N`. Rejected if the branch advanced. |
| Branch advanced after tagging | The old tag is dead. Release the next beta number from the new tip. |
| Code change needed | Fix on the release-cycle branch, then a new tag. Never reuse a tag. |

A retry may get a new version code. That is expected.

## 3. Bump the version on develop

Get the next version name from the Release calendar. Ask the user to confirm it was agreed at the Android standup. Then, after approval, branch `chore/bump-version-<next>` from `develop`, change `versionName` in `AndroidCoordinates.kt`, commit (signed when possible, see Rules), and open a PR into `develop`. Do this once per cycle, after the first beta, not after every beta.

## 4. Changelog and announce

**Collect.** After both tags are pushed, run `changelog <ver> <n> <prev>`. It lists the commits between the previous production tag and this beta, for the app and for Kalium. It uses the exact beta tags, not `HEAD`, so the list matches what shipped. `prev` is the previous production version you confirmed in step 1.

**Draft.** Write the notes yourself from that list. Group them as **New**, **Improved** and **Fixed**, and keep the WPB codes. The Confluence prompt for this step reads: "Summarize and categorize this ChangeLog, from a new release of the Wire application, an end-to-end encrypted communication app focused in governments and enterprises. The categories should be New, Improved and Fixed. Remove any changes that are not visible to the end-user, like dependency updates, or CI/CD changes, etc. Keep the associated WPB codes."
- Drop what end users cannot see: dependency updates, CI/CD, tests, refactors, localization syncs, performance profiles, version bumps.
- Drop iOS-only and Apple-only Kalium changes. This is the Android build.
- Treat a commit marked `!` (breaking) as a prompt to look for its user-visible effect.
- List anything behind a feature flag in a separate "Check before posting" list. A later commit can hide a feature that an earlier commit added.
- Write plain sentences about what the user sees, not commit subjects.

**Proofread.** The draft is a helper. The user proofreads it and decides what goes in the post. Say which entries you inferred from a commit subject alone, so they know where to look.

**Announce.** Run `announce <ver> <n>`. It prints the message for the Releases and deployments group between `copy from here` and `copy to here` markers: the version, both tags, the release link and a `Changes:` placeholder. Show the message in the chat in a fenced block with the proofread changelog in place of the placeholder. The user posts it.

## Production

A production version ships from the tip of `release/cycle-<cycle>`, the branch the betas came from. It is prepared about a week before the release date so QA can run the regression. Play Console uses Managed publishing for `com.wire`, so nothing reaches users until someone publishes it in Play Console after QA approves.

What the tag push does, checked on 4.38.1: pushing `v<ver>` starts `Generate Changelog`. That workflow publishes the GitHub release with the bot token, and the published release starts `Production` (`build-production.yml`). `Production` deploys to S3, to the Play `production` track at 10% (held by Managed publishing), to the GitHub release (`version.txt` and the APK) and to F-Droid. Do not create or publish the GitHub release by hand.

**P1. Check the tip.** Run `prod-check <ver>`. It prints the tip, the Kalium pin, the last beta, the commits on the tip since that beta, the release-note status and whether `v<ver>` and `android-v<ver>` are free. Show the commits since the last beta to the user. Name any commit that is not CI or docs, then ask the user whether to tag the tip or cut another beta first.

**P2. Land the release notes.** The copywriters write and approve them. Ask the user for the final English and German text, as two files or pasted text. Do not draft, shorten or translate it.
1. `worktree <ver> notes`, plan, then `--yes`.
2. Save the text to two files outside the repo, for example in the scratchpad. Inside the worktree run `$rel notes <ver> <en-file> <de-file>`. It writes `app/src/main/play/release-notes/en-US/<ver>.txt` and `de-DE/<ver>.txt` verbatim, runs `scripts/release/prepare-release-notes.sh` and reverts the `default.txt` files it generates. If a locale is over 500 characters, stop and tell the user. The copywriters shorten it.
3. Commit only the two `<ver>.txt` files (signed when possible) and show the diff. With approval, push the scratch branch and open a PR into `release/cycle-<cycle>`. The user merges it.

**P3. Re-check.** Run `prod-check <ver>` again. It must exit 0. Read `tip` from its output.

**P4. Tag.** Confirm the branch, `tip`, the tag `v<ver>` and the Kalium pin with the user. Run `tag <ver> prod <tip>`, show the plan, then repeat with `--yes`. It checks that the tip did not move, `versionName` matches, the notes are present and within the limit, and the tag is free. Then run `tag-kalium <ver> prod` and, after approval, repeat with `--yes`. It tags `android-v<ver>` at the Kalium commit `v<ver>` pins.

Watch the chain:
```sh
gh run list --workflow generate-changelog.yml --limit 1
gh run list --workflow build-production.yml --limit 1
gh run watch <run-id> --exit-status
gh release view v<ver>
```
Report the result of each job. If `Production` fails, use **Re-run failed jobs**. A full re-run builds a new version code and leaves a second APK on the release. Never move or re-push `v<ver>`.

**P5. Start the regression.** When `Production` succeeds, run `regression <ver>`. It reads the build number from the APK on the `v<ver>` release, for example `52601` in `com.wire.android-v4.38.1-52601-prod-compatrelease.apk`, and shows the dispatch plan for `QA Android Prod Regression RC` with `flavor=production`. Repeat with `--yes` after approval. Report the run link. The release log picks up the run by itself.

After this the release continues outside the skill: QA's verdict, the Play Console publish and rollout, the GitHub "Latest" flag and the Jira release status.

## Common failures

- `Beta source must equal the current remote tip`: the branch moved. Re-fetch and restart at step 2.
- `Tag must match the app version`: `versionName` at the tip differs from the tag version.
- `Set the BETA_APP_TRACK_INTERNAL repository variable`: ask a repo admin. Do not work around it.
- `git fetch --tags` rejects `v4.38.0`: local tags differ from the remote. Do not force it. The script reads remote tags.
- Test compile error after a cherry-pick onto the branch: the pinned Kalium may lack an API the picked change uses. Check the Kalium SHA before assuming a real bug.
