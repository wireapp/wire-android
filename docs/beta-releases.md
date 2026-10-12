# Beta releases

Beta releases use tags such as `v4.36.0-beta.1` on commits merged into `develop`.
The base version must match `AndroidApp.versionName` in that commit. Beta numbers
are positive integers without leading zeros. Merges into `develop` still publish
alpha builds; pushes to `main` no longer publish beta builds.

## Release from GitHub Actions

Open **Actions > Release beta > Run workflow** and select `develop`. Leave the
fields empty to use that develop commit and the next available beta number.
Optionally provide a commit SHA on develop or an explicit beta number.

```sh
gh workflow run release-beta.yml --ref develop
gh workflow run release-beta.yml --ref develop -f commit=<commit-sha> -f beta-number=3
```

The action creates an annotated tag and explicitly starts **Beta release** at
that tag. It uses `GITHUB_TOKEN` and needs no personal access token.

## Release by pushing a tag

```sh
git fetch origin develop --tags
git tag -a v4.36.0-beta.1 <commit-sha> -m "Beta 4.36.0, build 1"
git push origin refs/tags/v4.36.0-beta.1
```

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
gh workflow run build-beta.yml --ref v4.36.0-beta.1
```

A retry may rebuild and generate a new version code. For code changes, use a new
tag. If the manual action created its tag before failing, dispatch the build for
that tag instead of rerunning tag creation. Do not move release tags or rerun a
published release.

Merge these workflows into `develop` before creating beta tags. Release runs
queue and serialize deployment to the beta channel. RC and production keep
their existing workflows.
