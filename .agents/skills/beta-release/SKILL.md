---
name: beta-release
description: Cut a Wire Android internal beta from a release/cycle branch. Tags the build and Kalium, writes the changelog and announcement, bumps develop.
disable-model-invocation: true
---

# Beta release

Source: the Confluence how-to "Wire Android Beta version release" (space AR), plus `docs/beta-releases.md` and `docs/adr/0016-beta-releases-from-release-cycle-tips.md`. If this file conflicts with them, the repo docs win.

Betas ship at the end of each sprint, every two weeks, at the end of the week.

A beta is an annotated tag `vX.Y.Z-beta.N` on the **current remote tip** of a `release/cycle-*` branch. The tag starts `Beta release` (`build-beta.yml`). It builds the tagged commit with its pinned Kalium, deploys to S3 and the Play track, and publishes a GitHub prerelease. Kalium gets its own tag `android-vX.Y.Z-betaN` (no dot before `N`) that you create.

## The script

Every command lives in one script. Run it from the main checkout. `${CLAUDE_SKILL_DIR}` is this skill's folder. Agents that do not set it use `.agents/skills/beta-release`.
```sh
beta=${CLAUDE_SKILL_DIR}/scripts/beta.sh
$beta help
```
`ver` is the beta version, for example `4.39.0`. The cycle is the minor line, `4.39`. Read every SHA from script output. Never retype one or paste a version literal into a command.

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

An outward command, `worktree` and `cleanup` check every guard, prints `PLAN: ...` and stops. It writes only when `--yes` is the **last** argument. So: run it without `--yes`, show the user the plan, wait for an explicit yes, then repeat it with `--yes`. A refusal (`error: ...`) means a guard failed. Report it. Do not look for a way around it.

## Rules

- Creating a branch or tag, dispatching a workflow, pushing and opening a PR are outward-facing. Show the plan, wait for an explicit yes, and only then act. Approval for one beta does not cover the next.
- Never move, delete or re-push a published beta tag. Never rerun a published release. Existing tags and old branches stay as they are. Do not sign, replace or delete them.
- Never push to `develop` or a release-cycle branch directly. The version bump goes through a PR.
- Draft the Jira and chat messages in the conversation. Post nothing under the user's account.
- Push tags with the driver's own git credentials. Never ask for, print or store a token.
- Signing is best effort. Try `git commit -S` for commits. `beta.sh tag` tries to sign the tag. If signing is not set up or fails, continue unsigned (`git commit --no-gpg-sign`) and tell the user. Do not change the signing configuration. Do not stop the release over a signature.
- Put every message you draft for the user to post (Jira, chat, announcement) in a fenced block, with the real version and tag filled in, so they can copy it.
- Every beta needs its Kalium tag. Neither workflow creates it.

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

1. `worktree <ver> <topic>` creates `../wire-android-beta-<cycle>` on `chore/beta-<cycle>-<topic>` from `origin/release/cycle-<cycle>`, with the submodule and `local.properties`. Run it without `--yes` first, show the plan, then repeat with `--yes`. Nothing is pushed.
2. Make the change inside the worktree.
   - **Kalium bump:** `cd kalium && git fetch origin && git checkout <sha>`. Show the user `git log --oneline <old>..<new>` first and name any `!` commits. If the old pin is not an ancestor of the new one, stop and ask.
   - **Cherry-pick:** `git cherry-pick -x <sha>`. If it fails to compile, check the Kalium pin first.
3. Inside the worktree, run `$beta checks <ver>` in the background (use the absolute path of `beta.sh`). It compiles the app and its tests, runs the unit tests and `detektAll` in one Gradle invocation, online. Do not start a second Gradle build in the same worktree.
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

## Common failures

- `Beta source must equal the current remote tip`: the branch moved. Re-fetch and restart at step 2.
- `Tag must match the app version`: `versionName` at the tip differs from the tag version.
- `Set the BETA_APP_TRACK_INTERNAL repository variable`: ask a repo admin. Do not work around it.
- `git fetch --tags` rejects `v4.38.0`: local tags differ from the remote. Do not force it. The script reads remote tags.
- Test compile error after a cherry-pick onto the branch: the pinned Kalium may lack an API the picked change uses. Check the Kalium SHA before assuming a real bug.
