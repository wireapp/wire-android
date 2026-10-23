# QA Android Regression CI

These Android UI test scripts back **QA Android Regression Tests**:

- `.github/workflows/qa-android-regression-tests.yml`

The workflow calls a small set of phase-oriented scripts instead of many tiny one-off files.

## Test Service URL

The test service URL is compiled into the instrumentation APK when it is built.
CI reads it from the GitHub Actions variable `TEST_SERVICE_URL`. Configure the
variable with the full URL, including scheme and port, that is reachable from
the test devices, for example `http://test-service.internal:8080`.

For a single manual Regression run, set the optional `testServiceUrl` input
to an existing service instance. An empty input (including scheduled runs)
uses `TEST_SERVICE_URL` from Actions variables. The chosen URL is validated
before setup and recorded in the run summary and deflake artifact. Use an
HTTP(S) origin with an optional port and trailing slash; credentials, path
prefixes, query strings, fragments and control characters are rejected.
This does not deploy a service or rebuild the Wire app: only the instrumentation
APK is built with the selected address. Existing trusted-ref and `android-e2e`
environment restrictions still apply.

Manual deflake inherits `test_service_url` from the selected run's artifact,
even if the Actions variable has since changed. Its optional `testServiceUrl`
input explicitly overrides that address, and the new artifact records the
effective URL for subsequent reruns. Legacy artifacts without a URL require
an explicit input: their original service cannot be inferred safely from the
current Actions variable. A stable URL alone does not pin the service binary;
use an instance deployed with an immutable image revision for reproducibility.

For local runs, set the URL once in the ignored root `local.properties` file:

```properties
testServiceUrl=http://10.0.2.2:8080
```

`10.0.2.2` reaches the host machine from the standard Android emulator. For a
USB-connected physical device, run `adb reverse tcp:8080 tcp:8080` and use
`http://127.0.0.1:8080`. Alternatively, use the laptop's LAN address when the
device is on the same network and the Docker port is exposed on that address.

For a one-off local override, pass
`-PtestServiceUrl=http://10.0.2.2:8080`. `TEST_SERVICE_URL` has the highest
precedence, followed by the Gradle property and then `local.properties`.

## Workflow Summary

### `qa-android-regression-tests.yml`

One Regression workflow for normal tests, automatic failed-test retries,
and manual deflake.

- Supports `workflow_dispatch`.
- Offers `runMode`: **Normal tests** (default) or **Deflake failed tests**.
- Manual normal runs default to `@regression`; replace `TAGS` with another
  category or test case ID when needed.
- Runs nightly on `schedule` at 01:00 in `Europe/Berlin`.
- Uses built-in defaults for non-manual runs:
  - latest APK
  - upgrade mode enabled
  - `alpha release candidate`
  - `@regression`
  - `e2eTests` device group
  - failed-test rerun enabled with count `1`
- Exports one standard deflake artifact for later manual reruns.
- Publishes normal and manual deflake reports on the same Regression
  reports page (`qa-ui-tests`), with a separate report for each run.
- Sends both modes to the same Wire conversation with distinct headings:
  `android regression tests` and `android regression tests manual deflake`.

### `qa-android-fdroid-tests.yml`

**QA Android F-Droid Tests** runs nightly at `03:00 UTC`, using the latest
`fdroid` APK, `@fdroid`, and one additional failed-test retry. Manual runs
allow custom tags or test IDs; the flavor is always `fdroid` and the only
supported device group is `GrapheneOS`.

This workflow reuses Regression's execution, retry, deflake, and reporting
jobs rather than maintaining a second pipeline.

Choose **Deflake failed tests** in this workflow to rerun failures from a
previous F-Droid run. Leave `sourceRunId` empty to use the latest completed run
on `develop`, or enter a specific run ID. Source runs must belong to
`qa-android-fdroid-tests.yml`. If the selected run has no remaining failures or
its artifact is unavailable, or it was cancelled or timed out, the workflow stops
rather than choosing an older run.
Normal and deflake reports share the F-Droid reports page (`qa-fdroid-tests`).
Wire notifications use the QA automation conversation with headings
`android fdroid tests` and `android fdroid tests manual deflake`.

### Deflake Mode

Deflake is an option in **QA Android Regression Tests**, rather than a
separate workflow. To launch it:

1. Open **Run workflow** on `develop` and choose **Deflake failed tests**.
2. Leave `sourceRunId` empty for the latest completed Regression run on
   `develop`, or enter a previous run's `manual deflake id`.
3. Select the source run's `androidDeviceGroup`.
4. Leave `rerunFailedCount` blank to execute the source failures once without
   retries, or enter `1`–`3` for that many additional attempts.
5. Click **Run workflow**.

- Accepts a previous normal or deflake run from this workflow, while its artifact
  is still available.
- Downloads the standard deflake artifact from that selected run.
- Runs only the failed tests listed in the artifact.
- Publishes its own Allure report on the shared Regression reports page (`qa-ui-tests`), without merging results into the source run.
- Sends results and the report link to the same Wire conversation with an `android regression tests manual deflake` heading.
- Exports a fresh deflake artifact again so a deflake run can be deflaked later.

Deflake always inherits the source build, flavor, test revision, selector,
upgrade settings, and device selection. It also inherits the Testiny run name:
if the source had a name, deflake updates that same run; if it had no name,
Testiny reporting stays disabled. The normal-run `testinyRunName` input cannot
override this behavior.

GitHub displays all inputs in both modes, so normal-only fields are labeled
accordingly and ignored during deflake. An empty `sourceRunId` selects the
latest completed run of this workflow, without falling back to older runs if
the selected run has no remaining failures or its artifact is unavailable.
Normal mode rejects a nonempty `sourceRunId`
to avoid accidentally starting normal tests when a deflake was intended.

The retry count means **additional attempts after the first execution**.
`0` executes the source failures once; `1` allows one extra attempt; `2`
allows two; `3` allows three. Leaving `rerunFailedCount` blank uses `0` for
deflake and `1` for normal runs. Each additional attempt runs only the tests
that still fail. The effective settings are recorded in the new artifact,
but each deflake run uses its own retry count rather than inheriting the
source run's count. Scheduled runs always use normal mode.

## Manual Deflake ID

Every workflow run that exports the standard deflake bundle writes a copy-friendly summary entry:

- `manual deflake id: <github.run_id>`

This is the GitHub Actions run ID to paste into `sourceRunId` after selecting
**Deflake failed tests** in **QA Android Regression Tests**.

## Standard Deflake Artifact

Artifact name:

- `android-ui-test-deflake-input`

Bundle contents:

- `metadata.json`
- `failed-tests.txt`
- `failed-tests-first-attempt.txt`

`metadata.json` carries the run context needed by later manual deflake runs, including:

- workflow name and file
- run ID, run number, run attempt, branch, and triggering commit
- actual checked-out test commit, preserved through chained deflake runs
- flavor and selector
- effective Test Service URL
- build inputs and resolved build info
- upgrade flags
- device selection
- rerun configuration
- Testiny run name

## Device Groups

Physical devices are allocated through named, disjoint groups stored in the
GitHub repository variable `ANDROID_DEVICE_GROUPS_JSON`. Workflows select a
group through `androidDeviceGroup`; the Regression default is
`e2eTests`.

Example repository-variable value:

```json
{
  "criticalFlow": [
    "device-serial-01",
    "device-serial-02"
  ],
  "e2eTests": [
    "device-serial-03",
    "device-serial-04"
  ],
  "GrapheneOS": [
    "device-serial-05"
  ]
}
```

Device selection works as follows:

1. Parse and validate the requested group from `ANDROID_DEVICE_GROUPS_JSON`.
2. Reject configurations that assign one serial to more than one group. This
   guarantees that independently locked group workflows cannot share a phone.
3. Intersect the configured group with devices currently online in `adb`.
4. Warn and continue when only part of the group is online; fail when none of
   the configured devices are online.
5. If `androidDeviceId` is set, require that device to be online and belong to
   the selected group.
6. If a single testcase is selected, use the first online device in configured
   group order. Otherwise shard across every online device in the group.

Concurrency is locked by group name, allowing disjoint groups such as
`criticalFlow`, `e2eTests`, and `GrapheneOS` to run at the same time. Adding,
removing, or moving a device does not require a repository change: update the
GitHub repository variable. Group names passed to workflows must match the JSON
keys.

Manual deflake asks for the group because GitHub evaluates concurrency before
the source artifact can be downloaded. It validates that the requested group
matches group metadata from newer source runs. For artifacts created before
device groups were introduced, select the appropriate group manually.

## Protected Environment

Both secret-bearing jobs use the GitHub environment `android-e2e`. Configure
that environment with required reviewers and restrict deployment branches to
the repository default branch. Store the AWS, 1Password, and notification
secrets used by this workflow as environment secrets rather than repository
secrets. The workflow also rejects manual runs from other refs and checks out
the default branch without persisting credentials, but environment protection
is the enforcement boundary that a modified branch workflow cannot remove.

## Flavor Resolution Source

Flavor resolution is runner-driven, not hardcoded in the repo.

- Source of truth: `/etc/android-qa/flavors.json` on the self-hosted runner
- Executed via: `bash scripts/qa/android-ui/execution_setup.sh resolve-flavor`
- Exports for later workflow steps:
  - `S3_FOLDER`
  - `APP_ID`
  - `PACKAGES_TO_UNINSTALL`

## Primary Scripts

- `validation.sh`: input validation, selector parsing, and resolved value logging.
- `execution_setup.sh`: runner prep, flavor/APK resolution, device prep, secrets fetch, and test artifact setup.
- `run_ui_tests.sh`: instrumentation execution, sharding, failed-test auto-reruns, and manual-deflake failed-list execution.
- `reporting.sh`: Allure pull/merge/generate/publish, deflake bundle preparation, and cleanup.

Test artifact setup exports both `TEST_APK_PATH` and the generated
`TEST_APP_ID`. The runner uses `TEST_APP_ID` to bind instrumentation to the
freshly built test package; `APP_ID` remains the Wire app driven by UI Automator.

## Retry Flow

The rerun feature is controlled by the `rerunFailedCount` workflow input:

- Enter `0` to disable retries, or `1`–`3` for the maximum number of additional
  attempts after attempt `0` completes.
- Leave it blank for the mode's default: `1` for normal runs and `0` for deflake.

There is no separate retry checkbox or retry-settings selector. Deflake uses
the count entered for the current run, not the source run's retry settings.

Execution flow:

1. Run attempt `0` on the selected device set using the resolved selector.
2. Pull Allure results immediately after that attempt finishes.
3. Extract only the failed test IDs in `Class#method` format.
4. Evenly assign those failed tests across the retry devices.
5. Run rerun attempts with explicit rerun lists so only the previously failed tests execute.
6. Merge all attempts into one final Allure dataset and keep the latest outcome per logical test.

Before report generation, the merged dataset is made safe for publication:

- screenshots and other attachments are omitted;
- test parameters and failure traces/messages are removed;
- physical device serials are replaced with per-run aliases.

GitHub Pages should still be configured with the narrowest available access
policy because test names and high-level outcomes remain visible.

Reporting behavior:

- A test that fails first and passes later is reported as `passed` in the final merged report.
- That recovered test also receives the Allure label `passed_on_rerun=true`.
- For normal runs, the merged `environment.properties` file records:
  - `failed_first_attempt`
  - `passed_on_rerun`
  - `failed_after_retries`
- For manual deflake, Allure, the workflow summary, and Wire show tests selected,
  passed, and still unsuccessful. Skipped or missing results are shown separately
  when present. Only explicit final passed results count as recovery, including
  tests that pass on the first deflake attempt. Missing selected results fail
  the summary step instead of being treated as passed.

## Selector And Upgrade Behavior

The selector decides which tests are in scope. Upgrade mode decides how the
selected upgrade-tagged subset is executed.

- `@criticalFlow` is the manual default and runs only `criticalFlow` tests.
- `@TC-1234` runs only that test case.

Manual runs can override the default; nightly runs always use `@criticalFlow`.
Only the first nonempty comma-separated selector is used. Clearing `TAGS`
removes the selector and selects the full suite, not just Critical Flow tests.

When `isUpgrade=true` and the selector is a broad category run:

1. the workflow runs the selected category first on the new APK and excludes tests tagged `upgrade`
2. the workflow then runs only the selected `upgrade` subset last on one device, starting from the old APK

Examples:

- `@criticalFlow` + `isUpgrade=true`:
  - normal phase: `criticalFlow` tests except `criticalFlow + upgrade`
  - upgrade phase: only `criticalFlow + upgrade`

When `isUpgrade=false` and the selector is a category run:

- the workflow keeps the selected category scope
- tests also tagged `upgrade` are skipped instead of running in the normal phase

When the selector is left empty and `isUpgrade=true`:

- the workflow runs all non-upgrade tests first
- then runs all upgrade-tagged tests last

## Manual Deflake Flow

1. Regression or an earlier manual deflake run uploads `android-ui-test-deflake-input`.
2. A user copies the `manual deflake id` from the workflow summary.
   They choose **Deflake failed tests** in the Regression workflow, enter
   that ID, select the source device group, and optionally set `rerunFailedCount`.
3. GitHub API metadata must identify a completed normal or deflake run of
   `qa-android-regression-tests.yml` from this repository's default branch.
4. The downloaded artifact's repository, run, workflow, and triggering SHA must
   match that API response.
5. The actual test commit saved by the source run is used to overlay
   `testsCore` and `testsSupport` sources onto the current hardened harness.
   Deflake does not substitute newer test changes from `develop`.
6. Only the leftover failed tests are executed.
7. A fresh sanitized Allure report is published for that manual deflake run.
8. A fresh deflake artifact is uploaded again for the next round if needed.
9. Results and the report link are sent to the same Wire conversation with the
   `android regression tests manual deflake` heading.

## Python Helpers

- `resolve_flavor.py`: parse `flavors.json` and export flavor-derived env vars.
- `select_apks.py`: resolve new and old APK keys based on input and build-selection rules.
- `fetch_secrets_json.py`: build runtime `secrets.json` from 1Password vault items.
- `merge_allure_results.py`: merge per-device Allure outputs and attach retry metadata.
- `extract_failed_tests.py`: extract failed test IDs (`Class#method`) from one attempt's Allure result files.
- `prepare_deflake_bundle.py`: build the standard deflake artifact and append the manual deflake ID summary.
- `inspect_deflake_bundle.py`: validate a downloaded deflake artifact and expose its resolved values to later workflow steps.
- `validate_source_run.py`: verify that the selected source run belongs to the
  requesting workflow and the repository's default branch.
- `build_wire_notification.py`: build the normal or manual deflake message using
  the explicit suite title supplied by the workflow.
