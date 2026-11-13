#!/usr/bin/env bash
# Beta release helper for wire-android. One entry point, one subcommand per step. Run `beta.sh help`.
#
# Commands that write (cut, recut, tag, tag-kalium, worktree, cleanup) check every guard, print their plan and stop.
# They write to GitHub only when --yes is the LAST argument. Get the user's approval first.
set -euo pipefail

KALIUM_REPO=wireapp/kalium
ANDROID_REPO=wireapp/wire-android
KALIUM_URL=https://github.com/$KALIUM_REPO
VERSION_FILE=build-logic/plugins/src/main/kotlin/AndroidCoordinates.kt

usage() {
  cat <<'EOF'
beta.sh <command> [args]            read-only
  check <ver> [<prev>]              1a/1b  versionName on develop, production tags of the previous release
  plan <ver>                        1d     Kalium pin vs tip, state of cycle branches and tags
  verify <ver>                      1d     branch pin equals Kalium branch tip, release files exist
  validate <ver>                    2      next beta number, repo variable, source validation
  changelog <ver> <n> <prev>        4      commits since the previous production release, app and Kalium
  announce <ver> <n>                4      paste-ready announcement for the pushed beta
beta.sh <command> [args] [--yes]    outward-facing: dry run unless --yes is the last argument
  cut <ver> <android-sha> <kalium-sha>
  recut <ver> <new-android-sha> <new-kalium-sha> <old-android-tip> <old-kalium-tip>
  tag <ver> <n> <tip-sha>           annotated tag v<ver>-beta.<n> (signed if your signing works), pushed with your own credentials
  tag-kalium <ver> <n>              android-v<ver>-beta<n>; needs the Android tag pushed first
  tag-kalium <ver> prod             production tag android-v<ver>, only to repair the previous release
  worktree <ver> <topic>            local only: isolated worktree on chore/beta-<cycle>-<topic>
  cleanup <ver> <topic>             local only: remove that worktree and branch once nothing in them is unsaved
beta.sh <command> [args]            local
  checks <ver>                      compile, unit tests and detekt in the worktree (slow, run in background)
EOF
}

die() { echo "error: $*" >&2; exit 1; }
need_ver() { [[ ${1:-} =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || die "version must look like 4.39.0, got '${1:-}'"; }
need_num() { [[ ${1:-} =~ ^[1-9][0-9]*$ ]] || die "beta number must be a positive integer, got '${1:-}'"; }
need_sha() { [[ ${1:-} =~ ^[0-9a-f]{40}$ ]] || die "expected a 40-character SHA, got '${1:-}'"; }
cycle_of() { echo "${1%.*}"; }   # 4.39.0 -> 4.39. Branch names use the minor line only.
root() { cd "$(git rev-parse --show-toplevel)"; }
version_at() { git show "$1:$VERSION_FILE" | sed -nE 's/.*versionName = "([^"]+)".*/\1/p'; }

# Kalium commit pinned by a remote wire-android ref. Reads the remote, never a stale local tag.
# "|| return 1" makes a failed fetch stop here. Without it the function relies on git emptying FETCH_HEAD, because
# set -e does not reach into a function that runs inside $( ).
pinned_kalium() { git fetch -q origin "$1" || return 1; git ls-tree FETCH_HEAD kalium | awk '{print $3}'; }

# Refuse when a ref exists. Call it as a plain statement. "[[ -z $(git ls-remote ...) ]]" would pass when ls-remote
# fails, so a network or auth error would look like "no such ref". The assignment below stops on failure instead.
require_absent() { # message ls-remote-args...
  local msg=$1 out; shift
  out=$(git ls-remote "$@") || die "git ls-remote $* failed. Cannot tell whether the ref exists."
  [[ -z $out ]] || die "$msg"
}

require_present() { # message ls-remote-args...
  local msg=$1 out; shift
  out=$(git ls-remote "$@") || die "git ls-remote $* failed. Cannot tell whether the ref exists."
  [[ -n $out ]] || die "$msg"
}

# Print the plan. Stop here unless --yes was the last argument.
confirm() {
  echo "PLAN: $*"
  [[ $yes == 1 ]] && return 0
  echo "Dry run. Nothing was written. After the user approves, repeat the command with --yes as the last argument."
  exit 0
}

cmd_check() {
  local ver=${1:-} prev=${2:-} actual app kal sha
  need_ver "$ver"; root
  git fetch -q origin develop    # not "git fetch --tags": stale local tags such as v4.38.0 make it fail
  actual=$(version_at origin/develop)
  echo "versionName on develop: $actual"
  [[ $actual == "$ver" ]] || die "develop has versionName $actual, expected $ver. Stop and ask."
  echo "== production tag candidates (remote)"
  git ls-remote --tags --refs origin 'refs/tags/v[0-9]*' | awk '{sub("refs/tags/", "", $2); print $2}' \
    | { grep -Ev -- '-(alpha|beta|rc)' || true; } | sort -V | tail -4
  [[ -n $prev ]] || { echo "Pass <prev> to check its tags."; return 0; }
  need_ver "$prev"
  # ls-remote exits non-zero on a network or auth failure, so set -e stops there. Empty output means no such tag.
  app=$(git ls-remote --tags origin "refs/tags/v$prev")
  kal=$(git ls-remote --tags "$KALIUM_URL" "refs/tags/android-v$prev")
  echo "== v$prev (wire-android): ${app:-MISSING}"
  echo "== android-v$prev (kalium): ${kal:-MISSING}"
  if [[ -z $app ]]; then
    echo "Nearby tags:"; git ls-remote --tags origin "refs/tags/*${prev%.*}*" | tail -8
    echo "The production app was not built. Stop. Contact the release owners and postpone the beta."
    exit 2
  fi
  if [[ -z $kal ]]; then
    sha=$(pinned_kalium "refs/tags/v$prev")
    echo "Kalium tag missing. Production app v$prev pins:"
    gh api "repos/$KALIUM_REPO/commits/$sha" --jq '.sha + " " + (.commit.message | split("\n")[0])'
    echo "Nearby Kalium tags:"; git ls-remote --tags "$KALIUM_URL" "refs/tags/android-v${prev%.*}*" | tail -8
    echo "Propose: beta.sh tag-kalium $prev prod"
    exit 3
  fi
}

cmd_plan() {
  local ver=${1:-} cycle a pin tip ahead abranch kbranch btags
  need_ver "$ver"; cycle=$(cycle_of "$ver"); root
  git fetch -q origin develop
  a=$(git rev-parse origin/develop)
  pin=$(git ls-tree origin/develop kalium | awk '{print $3}')
  tip=$(git ls-remote "$KALIUM_URL" refs/heads/develop | awk '{print $1}')
  need_sha "$pin"; need_sha "$tip"
  echo "android_develop=$a"; echo "kalium_pin=$pin"; echo "kalium_tip=$tip"
  echo "== kalium pin...tip"
  gh api "repos/$KALIUM_REPO/compare/$pin...$tip" --jq '"status=\(.status) ahead=\(.ahead_by)"'
  ahead=$(gh api "repos/$KALIUM_REPO/compare/$pin...$tip" --jq .ahead_by)
  # The compare API lists at most 250 commits. Beyond that the breaking-commit list below is incomplete.
  [[ $ahead -le 250 ]] || echo "WARNING: $ahead commits ahead. The API shows only 250, so the lists below are incomplete."
  gh api "repos/$KALIUM_REPO/compare/$pin...$tip" --jq '.commits[] | "\(.sha[0:9]) \(.commit.message | split("\n")[0])"'
  echo "== breaking commits (!)"
  gh api "repos/$KALIUM_REPO/compare/$pin...$tip" \
    --jq '.commits[] | select(.commit.message | split("\n")[0] | test("^[a-z]+(\\([^)]*\\))?!:")) | .sha[0:9]'
  echo "== existing cycle state"
  # Assign first: "echo $(...)" would print an empty value when the lookup fails, and look like "no branch".
  abranch=$(git ls-remote --heads origin "release/cycle-$cycle" | awk '{print $1}')
  kbranch=$(git ls-remote --heads "$KALIUM_URL" "release/android-cycle-$cycle" | awk '{print $1}')
  btags=$(git ls-remote --tags --refs origin "refs/tags/v$cycle.*-beta.*" | awk '{print $2}' | tr '\n' ' ')
  echo "android branch: $abranch"; echo "kalium branch:  $kbranch"; echo "beta tags:      $btags"
  if git ls-remote --exit-code --heads origin "release/cycle-$cycle" >/dev/null; then
    git fetch -q origin "release/cycle-$cycle"
    echo "develop commits the branch lacks: $(git rev-list --count "FETCH_HEAD..origin/develop")"
    echo "commits only on the branch:"; git log --oneline "origin/develop..FETCH_HEAD"
  fi
}

cmd_verify() {
  local ver=${1:-} cycle pin ktip rc=0 p
  need_ver "$ver"; cycle=$(cycle_of "$ver"); root
  git fetch -q origin "release/cycle-$cycle"
  pin=$(git ls-tree FETCH_HEAD kalium | awk '{print $3}')
  ktip=$(git ls-remote --heads "$KALIUM_URL" "release/android-cycle-$cycle" | awk '{print $1}')
  echo "android branch tip: $(git rev-parse FETCH_HEAD)"
  echo "android branch pin: $pin"
  echo "kalium branch tip:  $ktip"
  [[ -n $ktip && $pin == "$ktip" ]] || { echo "MISMATCH: pin and Kalium branch tip differ"; rc=1; }
  for p in .github/workflows/release-beta.yml .github/workflows/build-beta.yml \
           scripts/release/validate-beta-source.py make/release.mk; do
    if git cat-file -e "FETCH_HEAD:$p" 2>/dev/null; then echo "ok $p"; else echo "MISSING $p"; rc=1; fi
  done
  return $rc
}

cmd_validate() {
  local ver=${1:-} cycle last vars
  need_ver "$ver"; cycle=$(cycle_of "$ver"); root
  last=$(git ls-remote --tags --refs origin "refs/tags/v$ver-beta.*" | sed -nE 's/.*-beta\.([0-9]+)$/\1/p' | sort -n | tail -1)
  echo "next_beta=$(( ${last:-0} + 1 ))"
  vars=$(gh variable list -R "$ANDROID_REPO")   # capture first: "gh | grep -q" fails under pipefail (SIGPIPE)
  grep -qE '^BETA_APP_TRACK_INTERNAL[[:space:]]' <<<"$vars" || die "BETA_APP_TRACK_INTERNAL is not set. Ask a repo admin."
  git fetch -q origin "release/cycle-$cycle"
  echo "tip=$(git rev-parse FETCH_HEAD)"
  echo "kalium_pin=$(git ls-tree FETCH_HEAD kalium | awk '{print $3}')"
  echo "versionName=$(version_at FETCH_HEAD)"
  SOURCE_BRANCH="release/cycle-$cycle" SOURCE_COMMIT=FETCH_HEAD make beta-release/validate-source
}

# Both cut and recut need the Kalium commit to be the pin of Android develop. A Kalium bump comes later, by PR.
require_develop_state() { # android-sha kalium-sha
  git fetch -q origin develop
  [[ $(git rev-parse origin/develop) == "$1" ]] || die "develop moved. Run plan again."
  [[ $(git ls-tree origin/develop kalium | awk '{print $3}') == "$2" ]] \
    || die "$2 is not the Kalium commit that Android develop pins. Cut at the pin, then bump."
}

cmd_cut() {
  local ver=${1:-} a=${2:-} k=${3:-} cycle acur kcur plan=""
  need_ver "$ver"; need_sha "$a"; need_sha "$k"; cycle=$(cycle_of "$ver"); root
  git fetch -q origin develop
  [[ $(version_at origin/develop) == "$ver" ]] || die "develop has versionName $(version_at origin/develop), expected $ver."
  require_develop_state "$a" "$k"
  gh api "repos/$KALIUM_REPO/commits/$k" --jq .sha >/dev/null || die "commit $k not found in Kalium"
  # Safe to re-run after a partial failure: a branch already at the wanted SHA is skipped. Any other SHA is refused.
  acur=$(git ls-remote --heads origin "release/cycle-$cycle" | awk '{print $1}')
  kcur=$(git ls-remote --heads "$KALIUM_URL" "release/android-cycle-$cycle" | awk '{print $1}')
  [[ -z $acur || $acur == "$a" ]] || die "release/cycle-$cycle exists at $acur, not $a. See the re-cut flow."
  [[ -z $kcur || $kcur == "$k" ]] || die "release/android-cycle-$cycle exists at $kcur, not $k. See the re-cut flow."
  [[ -n $acur ]] || plan="create release/cycle-$cycle at $a in $ANDROID_REPO. "
  [[ -n $kcur ]] || plan="${plan}create release/android-cycle-$cycle at $k in $KALIUM_REPO."
  [[ -n $plan ]] || { echo "Both branches already exist at the wanted commits. Nothing to do."; return 0; }
  confirm "$plan"
  [[ -n $acur ]] || gh api "repos/$ANDROID_REPO/git/refs" -f ref="refs/heads/release/cycle-$cycle" -f sha="$a" --jq .ref
  [[ -n $kcur ]] || gh api "repos/$KALIUM_REPO/git/refs" -f ref="refs/heads/release/android-cycle-$cycle" -f sha="$k" --jq .ref
  git ls-remote --heads origin "release/cycle-$cycle"
  git ls-remote --heads "$KALIUM_URL" "release/android-cycle-$cycle"
}

# Destructive: a force update drops commits that are only on the branch. Only valid while no beta tag exists.
cmd_recut() {
  local ver=${1:-} a=${2:-} k=${3:-} olda=${4:-} oldk=${5:-} cycle
  need_ver "$ver"; need_sha "$a"; need_sha "$k"; need_sha "$olda"; need_sha "$oldk"
  cycle=$(cycle_of "$ver"); root
  require_absent "a beta tag exists. Never move the branches. Use cherry-picks." --tags --refs origin "refs/tags/v$cycle.*-beta.*"
  require_develop_state "$a" "$k"
  [[ $(git ls-remote --heads origin "release/cycle-$cycle" | awk '{print $1}') == "$olda" ]] || die "Android branch tip is not $olda."
  [[ $(git ls-remote --heads "$KALIUM_URL" "release/android-cycle-$cycle" | awk '{print $1}') == "$oldk" ]] || die "Kalium branch tip is not $oldk."
  confirm "FORCE-move release/cycle-$cycle $olda -> $a and release/android-cycle-$cycle $oldk -> $k"
  gh api -X PATCH "repos/$ANDROID_REPO/git/refs/heads/release/cycle-$cycle" -f sha="$a" -F force=true --jq .object.sha
  gh api -X PATCH "repos/$KALIUM_REPO/git/refs/heads/release/android-cycle-$cycle" -f sha="$k" -F force=true --jq .object.sha
  echo "If the second command failed, the pins disagree. Fix it before anything else, then run: beta.sh verify $ver"
}

# Say whether the tag is signed and whether the signature verifies. Never blocks the release.
report_signature() {
  local obj
  obj=$(git cat-file tag "$1")   # capture first: "git cat-file | grep -q" fails under pipefail (SIGPIPE)
  if grep -qE 'BEGIN (PGP|SSH) SIGNATURE' <<<"$obj"; then
    if git tag -v "$1" >/dev/null 2>&1; then echo "tag $1 is signed and the signature verifies"
    else echo "note: tag $1 is signed, but this machine cannot verify the signature. Pushing anyway."; fi
  else
    echo "note: tag $1 is unsigned"
  fi
}

cmd_tag() {
  local ver=${1:-} n=${2:-} expected=${3:-} cycle tag tip peeled last
  need_ver "$ver"; need_num "$n"; need_sha "$expected"
  cycle=$(cycle_of "$ver"); tag="v$ver-beta.$n"; root
  require_absent "$tag already exists on the remote. Never re-push a beta tag." --tags origin "refs/tags/$tag"
  last=$(git ls-remote --tags --refs origin "refs/tags/v$ver-beta.*" | sed -nE 's/.*-beta\.([0-9]+)$/\1/p' | sort -n | tail -1)
  [[ $n == $(( ${last:-0} + 1 )) ]] || die "the next beta number is $(( ${last:-0} + 1 )), not $n."
  git fetch -q origin "release/cycle-$cycle"
  tip=$(git rev-parse FETCH_HEAD)
  [[ $tip == "$expected" ]] || die "branch tip is $tip, expected $expected. The branch moved. Validate again."
  [[ $(version_at FETCH_HEAD) == "$ver" ]] || die "versionName at the tip is $(version_at FETCH_HEAD), expected $ver."
  if git show-ref --verify --quiet "refs/tags/$tag"; then   # left behind by a failed push
    [[ $(git cat-file -t "refs/tags/$tag") == tag ]] || die "local tag $tag is lightweight. Inspect it. Do not delete it blindly."
    [[ $(git rev-parse "$tag^{commit}") == "$tip" ]] || die "local tag $tag points elsewhere. Inspect it. Do not delete it blindly."
  fi
  confirm "create annotated tag $tag at $tip (signed if your git signing works, otherwise unsigned), then push it to origin with your git credentials; this starts the Beta release build"
  if ! git show-ref --verify --quiet "refs/tags/$tag"; then
    # Signing is best effort. The -c overrides stop "git tag -a" from trying to sign again when tag.gpgSign is on.
    git tag -s "$tag" "$tip" -m "Beta $ver, build $n" || {
      echo "note: could not sign the tag. Creating it unsigned."
      git -c tag.gpgSign=false -c tag.forceSignAnnotated=false tag -a "$tag" "$tip" -m "Beta $ver, build $n"
    }
  fi
  report_signature "$tag"
  # A "creations being restricted" notice from the remote is the ruleset bypass warning. It is not a failure.
  git push origin "refs/tags/$tag"
  peeled=$(git ls-remote origin "refs/tags/$tag^{}" | awk '{print $1}')
  [[ $peeled == "$tip" ]] || die "remote tag resolves to '$peeled', expected $tip"
  echo "ok $tag -> $tip"
}

cmd_tag_kalium() {
  local ver=${1:-} n=${2:-} sha name msg existing tag_sha peeled
  need_ver "$ver"; [[ -n $n ]] || die "usage: tag-kalium <ver> <n>   or   tag-kalium <ver> prod"; root
  if [[ $n == prod ]]; then
    # Production tag, for the previous release only. A forgotten <n> must not fall through to this.
    require_present "wire-android tag v$ver is missing. The production app was not built." --tags origin "refs/tags/v$ver"
    sha=$(pinned_kalium "refs/tags/v$ver"); name=android-v$ver; msg="Android $ver"
  else
    need_num "$n"
    require_present "push the Android tag v$ver-beta.$n first (beta.sh tag). Kalium is tagged at the commit it pins." \
      --tags origin "refs/tags/v$ver-beta.$n"
    sha=$(pinned_kalium "refs/tags/v$ver-beta.$n"); name=android-v$ver-beta$n; msg="Android $ver beta $n"
  fi
  need_sha "$sha"
  gh api "repos/$KALIUM_REPO/commits/$sha" --jq '"pinned commit: " + .sha + " " + (.commit.message | split("\n")[0])'
  # Match the exact ref. A prefix match would also hit android-v4.38.0-beta1 when tagging android-v4.38.0.
  existing=$(gh api "repos/$KALIUM_REPO/git/matching-refs/tags/$name" --jq ".[] | select(.ref == \"refs/tags/$name\") | .ref")
  [[ -z $existing ]] || die "Kalium tag $name already exists"
  confirm "create annotated tag $name at $sha in $KALIUM_REPO through the API"
  tag_sha=$(gh api "repos/$KALIUM_REPO/git/tags" -f tag="$name" -f message="$msg" -f object="$sha" -f type=commit --jq .sha)
  gh api "repos/$KALIUM_REPO/git/refs" -f ref="refs/tags/$name" -f sha="$tag_sha" --jq .ref
  peeled=$(git ls-remote "$KALIUM_URL" "refs/tags/$name^{}" | awk '{print $1}')
  [[ $peeled == "$sha" ]] || die "tag $name resolves to '$peeled', expected $sha"
  echo "ok $name -> $sha"
}

cmd_worktree() {
  local ver=${1:-} topic=${2:-} cycle main wt branch
  need_ver "$ver"; [[ $topic =~ ^[a-z0-9-]+$ ]] || die "topic must be lowercase letters, digits and dashes"
  cycle=$(cycle_of "$ver"); root; main=$PWD; wt="$main/../wire-android-beta-$cycle"
  [[ ! -e $wt ]] || die "$wt already exists"
  branch="chore/beta-$cycle-$topic"
  ! git show-ref --verify --quiet "refs/heads/$branch" || die "local branch $branch already exists"
  confirm "create worktree $wt on new local branch $branch from origin/release/cycle-$cycle, with the kalium submodule (local only, nothing is pushed)"
  git fetch -q origin "release/cycle-$cycle"
  git worktree add --no-track "$wt" -b "$branch" "origin/release/cycle-$cycle"   # no upstream: a bare "git push" must never reach the cycle branch
  cd "$wt"
  git submodule update --init --reference "$main/kalium" kalium
  if [[ -f $main/local.properties ]]; then cp "$main/local.properties" .; fi
  echo "worktree: $wt"
}

# Local only. Removes the scratch worktree and its branch once nothing in them is unsaved. Never touches the remote.
cmd_cleanup() {
  local ver=${1:-} topic=${2:-} cycle main wt branch wts unpushed merged
  need_ver "$ver"; [[ $topic =~ ^[a-z0-9-]+$ ]] || die "topic must be lowercase letters, digits and dashes"
  cycle=$(cycle_of "$ver"); root; main=$PWD
  wt=$(cd "$main/.." && pwd -P)/wire-android-beta-$cycle; branch="chore/beta-$cycle-$topic"
  [[ -d $wt ]] || die "$wt does not exist"
  wts=$(git worktree list --porcelain)
  grep -Fxq "worktree $wt" <<<"$wts" || die "$wt is not a worktree of this repository"
  [[ $(git -C "$wt" branch --show-current) == "$branch" ]] || die "$wt is not on $branch"
  [[ -z $(git -C "$wt" status --porcelain) ]] || die "uncommitted changes in $wt. Commit or discard them yourself first."
  unpushed=$(git rev-list --count "$branch" --not --remotes)
  if [[ $unpushed != 0 ]]; then   # a squash-merged PR leaves commits that no remote branch contains
    merged=$(gh pr list -R "$ANDROID_REPO" --head "$branch" --state merged --json number --jq length)
    [[ $merged -gt 0 ]] || die "$unpushed commits on $branch are on no remote branch, and no merged PR has them. Push them or open a PR first."
  fi
  confirm "remove worktree $wt (with its kalium checkout and build output) and delete local branch $branch. The remote is untouched."
  # --force: git refuses to remove a worktree with a submodule checked out. The guards above already proved nothing is unsaved.
  git worktree remove --force "$wt"
  git branch -D "$branch"
  git worktree prune
  echo "ok removed $wt and $branch"
}

# Raw material for the changelog: commits between the previous production release and this beta, app and Kalium.
# Uses the exact beta tags, not HEAD, so the list matches what shipped. Read-only.
cmd_changelog() {
  local ver=${1:-} n=${2:-} prev=${3:-} fmt='.commits[] | "\(.sha[0:9]) \(.commit.message | split("\n")[0])"'
  need_ver "$ver"; need_num "$n"; need_ver "$prev"; root
  require_present "wire-android tag v$prev is missing." --tags origin "refs/tags/v$prev"
  require_present "beta tag v$ver-beta.$n is missing." --tags origin "refs/tags/v$ver-beta.$n"
  require_present "Kalium tag android-v$prev is missing. Run: beta.sh tag-kalium $prev prod" --tags "$KALIUM_URL" "refs/tags/android-v$prev"
  require_present "Kalium tag android-v$ver-beta$n is missing. Run: beta.sh tag-kalium $ver $n" --tags "$KALIUM_URL" "refs/tags/android-v$ver-beta$n"
  # --paginate follows the API's page limit, so a long range is not cut off.
  echo "== wire-android v$prev..v$ver-beta.$n"
  gh api --paginate "repos/$ANDROID_REPO/compare/v$prev...v$ver-beta.$n" --jq "$fmt"
  echo "== kalium android-v$prev..android-v$ver-beta$n"
  gh api --paginate "repos/$KALIUM_REPO/compare/android-v$prev...android-v$ver-beta$n" --jq "$fmt"
}

# Print the announcement for the Releases and deployments group, ready to paste. Read-only.
cmd_announce() {
  local ver=${1:-} n=${2:-} tag ktag tip ksha
  need_ver "$ver"; need_num "$n"; tag="v$ver-beta.$n"; ktag="android-v$ver-beta$n"; root
  require_present "$tag is not on the remote yet. Push it first." --tags origin "refs/tags/$tag"
  require_present "$ktag is not on the Kalium remote yet. Run tag-kalium first." --tags "$KALIUM_URL" "refs/tags/$ktag"
  tip=$(git ls-remote origin "refs/tags/$tag^{}" | awk '{print $1}')
  ksha=$(git ls-remote "$KALIUM_URL" "refs/tags/$ktag^{}" | awk '{print $1}')
  echo "----- copy from here -----"
  echo "Android beta $ver, build $n is out."
  echo "Tag: $tag (${tip:0:9})"
  echo "Kalium: $ktag (${ksha:0:9})"
  echo "Release: https://github.com/$ANDROID_REPO/releases/tag/$tag"
  echo "Changes: <paste the changelog you drafted and proofread>"
  echo "----- copy to here -----"
}

cmd_checks() {
  local ver=${1:-} log rc=0
  need_ver "$ver"; [[ -x ./gradlew ]] || die "run this from the worktree root"
  log=/tmp/beta-$ver-checks.log; rm -f "$log"
  rm -rf app/build/test-results/testDevDebugUnitTest   # else a compile failure would report the counts of an older run
  ./gradlew :app:compileDevDebugKotlin :app:compileDevDebugUnitTestKotlin :app:testDevDebugUnitTest detektAll \
    --parallel --continue > "$log" 2>&1 || rc=$?
  echo "gradle exit=$rc  (log: $log)"
  grep -a -E 'BUILD (SUCCESSFUL|FAILED)|^e: |FAILED$' "$log" | head -30 || true   # -a: Gradle prefixes escape sequences
  echo "== tasks that reused an earlier result (UP-TO-DATE)"
  grep -a -E ':app:(compileDevDebugKotlin|compileDevDebugUnitTestKotlin|testDevDebugUnitTest) UP-TO-DATE' "$log" || echo none
  echo "== unit test counts (BUILD SUCCESSFUL alone proves nothing)"
  python3 -I -c '
import glob, xml.etree.ElementTree as ET
r = [ET.parse(p).getroot() for p in glob.glob("app/build/test-results/testDevDebugUnitTest/*.xml")]
f = lambda k: sum(int(x.get(k, 0)) for x in r)
print("suites", len(r), "tests", f("tests"), "failures", f("failures"), "errors", f("errors"))'
  echo "note: detektAll covers :app only, not Kalium sources. A compile failure means the tests never ran."
  return $rc
}

yes=0
if [[ ${!#:-} == --yes ]]; then yes=1; set -- "${@:1:$#-1}"; fi
cmd=${1:-help}; [[ $# -eq 0 ]] || shift
case $cmd in
  check|plan|verify|validate|changelog|announce|cut|recut|tag|tag-kalium|worktree|cleanup|checks) "cmd_${cmd//-/_}" "$@" ;;
  help|-h|--help) usage ;;
  *) usage >&2; exit 1 ;;
esac
