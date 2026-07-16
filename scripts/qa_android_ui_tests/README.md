# QA Android UI Test CI Scripts

These scripts back the Android UI test workflows:

- `.github/workflows/qa-android-critical-flow-tests.yml`
- `.github/workflows/qa-android-ui-test-manual-deflake.yml`

The workflows call a small set of phase-oriented scripts instead of many tiny one-off files.

## Workflow Summary

### `qa-android-critical-flow-tests.yml`

Main critical-flow workflow.

- Supports `workflow_dispatch`.
- Runs nightly on `schedule` at `04:00 UTC`.
- Uses built-in defaults for non-manual runs:
  - latest APK
  - upgrade mode enabled
  - `alpha release candidate`
  - `@criticalFlow`
  - `criticalFlow` device group
  - failed-test rerun enabled with count `1`
- Exports one standard deflake artifact for later manual reruns.

### `qa-android-ui-test-manual-deflake.yml`

Manual workflow for rerunning only the leftover failed tests from an earlier run.

- Triggered with a `sourceRunId`.
- Accepts a previous critical-flow run or a previous manual deflake run.
- Downloads the standard deflake artifact from that selected run.
- Runs only the failed tests listed in the artifact.
- Publishes a fresh Allure report for the manual deflake run only.
- Exports a fresh deflake artifact again so a deflake run can be deflaked later.

## Manual Deflake ID

Every workflow run that exports the standard deflake bundle writes a copy-friendly summary entry:

- `manual deflake id: <github.run_id>`

This is the GitHub Actions run ID to paste into the manual deflake workflow input.

## Standard Deflake Artifact

Artifact name:

- `android-ui-test-deflake-input`

Bundle contents:

- `metadata.json`
- `failed-tests.txt`
- `failed-tests-first-attempt.txt`

`metadata.json` carries the run context needed by later manual deflake runs, including:

- workflow name and file
- run ID, run number, run attempt, branch, and commit
- flavor and selector
- build inputs and resolved build info
- upgrade flags
- device selection
- rerun configuration
- Testiny run name

## Device Groups

Physical devices are allocated through named, disjoint groups stored in the
GitHub repository variable `ANDROID_DEVICE_GROUPS_JSON`. Workflows select a
group through `androidDeviceGroup`; the critical-flow default is
`criticalFlow`.

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

## Flavor Resolution Source

Flavor resolution is runner-driven, not hardcoded in the repo.

- Source of truth: `/etc/android-qa/flavors.json` on the self-hosted runner
- Executed via: `bash scripts/qa_android_ui_tests/execution_setup.sh resolve-flavor`
- Exports for later workflow steps:
  - `S3_FOLDER`
  - `APP_ID`
  - `PACKAGES_TO_UNINSTALL`

## Primary Scripts

- `validation.sh`: input validation, selector parsing, and resolved value logging.
- `execution_setup.sh`: runner prep, flavor/APK resolution, device prep, secrets fetch, and test artifact setup.
- `run_ui_tests.sh`: instrumentation execution, sharding, failed-test auto-reruns, and manual-deflake failed-list execution.
- `reporting.sh`: Allure pull/merge/generate/publish, deflake bundle preparation, and cleanup.

## Retry Flow

The rerun feature is controlled by workflow inputs:

- `rerunFailedEnabled`: turn failed-test reruns on or off for this workflow run
- `rerunFailedCount`: maximum number of rerun attempts after attempt `0` completes; default is `1`

Execution flow:

1. Run attempt `0` on the selected device set using the resolved selector.
2. Pull Allure results immediately after that attempt finishes.
3. Extract only the failed test IDs in `Class#method` format.
4. Evenly assign those failed tests across the retry devices.
5. Run rerun attempts with explicit rerun lists so only the previously failed tests execute.
6. Merge all attempts into one final Allure dataset and keep the latest outcome per logical test.

Reporting behavior:

- A test that fails first and passes later is reported as `passed` in the final merged report.
- That recovered test also receives the Allure label `passed_on_rerun=true`.
- The merged `environment.properties` file records:
  - `failed_first_attempt`
  - `passed_on_rerun`
  - `failed_after_retries`

## Selector And Upgrade Behavior

The selector decides which tests are in scope. Upgrade mode decides how the
selected upgrade-tagged subset is executed.

- `@criticalFlow` runs only `criticalFlow` tests.
- `@regression` runs only `regression` tests.
- `@TC-1234` runs only that test case.

When `isUpgrade=true` and the selector is a broad category run:

1. the workflow runs the selected category first on the new APK and excludes tests tagged `upgrade`
2. the workflow then runs only the selected `upgrade` subset last on one device, starting from the old APK

Examples:

- `@criticalFlow` + `isUpgrade=true`:
  - normal phase: `criticalFlow` tests except `criticalFlow + upgrade`
  - upgrade phase: only `criticalFlow + upgrade`
- `@regression` + `isUpgrade=true`:
  - normal phase: `regression` tests except `regression + upgrade`
  - upgrade phase: only `regression + upgrade`

When `isUpgrade=false` and the selector is a category run:

- the workflow keeps the selected category scope
- tests also tagged `upgrade` are skipped instead of running in the normal phase

When the selector is left empty and `isUpgrade=true`:

- the workflow runs all non-upgrade tests first
- then runs all upgrade-tagged tests last

## Manual Deflake Flow

1. Critical flow or an earlier manual deflake run uploads `android-ui-test-deflake-input`.
2. A user copies the `manual deflake id` from the workflow summary.
3. The manual deflake workflow downloads the artifact for that selected run.
4. The workflow validates `metadata.json` and `failed-tests.txt`.
5. Only the leftover failed tests are executed.
6. A fresh Allure report is published for that manual deflake run.
7. A fresh deflake artifact is uploaded again for the next round if needed.

## Python Helpers

- `resolve_flavor.py`: parse `flavors.json` and export flavor-derived env vars.
- `select_apks.py`: resolve new and old APK keys based on input and build-selection rules.
- `fetch_secrets_json.py`: build runtime `secrets.json` from 1Password vault items.
- `merge_allure_results.py`: merge per-device Allure outputs and attach retry metadata.
- `extract_failed_tests.py`: extract failed test IDs (`Class#method`) from one attempt's Allure result files.
- `prepare_deflake_bundle.py`: build the standard deflake artifact and append the manual deflake ID summary.
- `inspect_deflake_bundle.py`: validate a downloaded deflake artifact and expose its resolved values to later workflow steps.
