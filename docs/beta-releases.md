# Beta releases

Beta releases use tags such as `v4.37.0-beta.1` at the current remote tip of a
release-cycle branch named `release/cycle-*`, such as `release/cycle-4.37.0`.
The base version must match `AndroidApp.versionName` in that commit. Beta numbers
are positive integers without leading zeros. Merges into `develop` still publish
alpha builds; pushes to `main` no longer publish beta builds. Stabilization fixes
created or cherry-picked onto the release-cycle branch can be released without
being merged into `develop`. `develop`, `release/candidate`, and unrelated branches
are not selectable beta sources.

## Release from GitHub Actions

Open **Actions > Release beta > Run workflow** and select the release-cycle branch.
Leave the beta number empty to choose the next available number for that app
version, or provide an explicit positive number. The selected commit must still
be that branch's remote tip when validation runs. Arbitrary commit selection is
not supported; if the branch advances before validation, run the action again
on its latest commit.

```sh
gh workflow run release-beta.yml --ref release/cycle-4.37.0
gh workflow run release-beta.yml --ref release/cycle-4.37.0 -f beta-number=3
```

The action creates an annotated tag and explicitly starts **Beta release** at
that tag. It uses `GITHUB_TOKEN` and needs no personal access token.

## Release by pushing a tag

```sh
git fetch origin release/cycle-4.37.0 --tags
git tag -a v4.37.0-beta.1 FETCH_HEAD -m "Beta 4.37.0, build 1"
git push origin refs/tags/v4.37.0-beta.1
```

Pushed tags must point to the current tip of at least one remote `release/cycle-*`
branch. Being an ancestor of a release-cycle branch or sharing changes with
`develop` is insufficient. A commit shared by `develop` and a release-cycle tip
is eligible because it satisfies the release-cycle rule.

Both paths run the existing checks, build the tagged commit with its pinned
Kalium submodule, and deploy Beta APK/AAB artifacts to the existing S3 and Play
destinations. `BETA_APP_TRACK_INTERNAL` selects the Play track for
`com.wire.android.internal`. Existing signing, AWS, and Play secrets still apply.

After deployment succeeds, the workflow publishes a GitHub prerelease with
generated notes, APK, AAB, and version metadata. The installed app version matches
the tag. The existing version-file task assigns one version code per build.

## Retry a failed release

Use **Re-run failed jobs** in **Beta release**, or dispatch it at the existing tag:

```sh
gh workflow run build-beta.yml --ref v4.37.0-beta.1
```

Every executed source validation checks live remote tips. A fresh dispatch or
**Re-run all jobs** rejects an older tag after its release-cycle branch advances
or is deleted. **Re-run failed jobs** can reuse an already successful validation
when only downstream jobs failed. Once validation succeeds, branch advancement
does not change the tagged commit being built.

A retry may rebuild and generate a new version code. For code changes, use a new
tag. If the manual action created its tag before failing, dispatch the build for
that tag instead of rerunning tag creation. Do not move release tags or rerun a
published release.

Ensure both beta workflows and their shared validation helper and Make target
are present on the release-cycle branch before creating beta tags. Updating
`develop` alone does not update an already diverged release-cycle branch.
Release runs
queue and serialize deployment to the beta channel. RC and production keep
their existing workflows.

## Check the source locally

```sh
SOURCE_COMMIT=HEAD make beta-release/validate-source
SOURCE_BRANCH=release/cycle-4.37.0 SOURCE_COMMIT=HEAD make beta-release/validate-source
```

The validator reads live heads from `origin`, prints the accepted commit SHA,
and fails if no eligible tip matches. Set `GH_TOKEN` when HTTPS GitHub access
requires authentication; the helper does not persist credentials.
