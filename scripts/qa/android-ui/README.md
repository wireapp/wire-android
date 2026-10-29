# Android UI Test CI

The workflows below share the execution, retry, deflake and reporting pipeline in
[qa-android-regression-tests.yml](../../../.github/workflows/qa-android-regression-tests.yml).

## Workflows and defaults

Schedules use `Europe/Berlin`, including daylight-saving changes. Prod and
F-Droid currently run manually only; F-Droid's schedule is commented out.

| Workflow | Default flavor / selector | Schedule (Berlin) | Device group | Additional retries |
| --- | --- | --- | --- | --- |
| [QA Android Alpha Regression](../../../.github/workflows/qa-android-regression-tests.yml) | `alpha release candidate` / `@regression` | Daily, 00:00 | `e2eTests` | 2 |
| [QA Android Beta Regression RC](../../../.github/workflows/qa-android-beta-tests.yml) | `alpha beta` / `@RC` | Daily, 04:00 | `e2eTests` | 2 |
| [QA Android Prod Regression RC](../../../.github/workflows/qa-android-prod-tests.yml) | `production` / `@RC` | Manual only | `e2eTests` | 2 |
| [QA Android Emulator Critical Flow Tests](../../../.github/workflows/qa-android-emulator-critical-flow-tests.yml) | `alpha release candidate` / `@criticalFlow` | Daily, 07:00 | `criticalFlow` | 1 |
| [QA Android F-Droid Prod Regression](../../../.github/workflows/qa-android-fdroid-tests.yml) | `fdroid` / `@fdroid` | Manual only | `GrapheneOS` | 1 |

All workflows support **Normal tests** and **Deflake failed tests**. The table
shows normal-run defaults; deflake defaults to **0 additional retries**.
Latest APK is the default build selection.

## Start a manual run

1. In GitHub **Actions**, select the workflow and click **Run workflow**.
2. Use the repository's default branch (currently `develop`).
3. Keep **Normal tests** selected and leave `sourceRunId` empty.
4. Adjust the inputs you need, then start the run.

Common inputs:

- `appBuildNumber`: `latest` or a specific build number.
- `TAGS`: one category, such as `@RC` or `@regression`, or one test ID,
  such as `@TC-8603`. Only the first nonempty comma-separated selector is
  used; `@key:value` selectors are not supported.
- `androidDeviceGroup`: the configured group to run on.
- `androidDeviceId`: optional serial to target one online device in that group.
- `testServiceUrl`: optional override for an existing test-service instance.
  Leave empty to use the configured default.
- `rerunFailedCount`: `0`–`3` additional attempts, or blank for the
  workflow's default above.

For normal Alpha, Beta, Prod and F-Droid runs, leaving `TAGS` empty selects
all tests, not the workflow's default category. Upgrade tests are handled
according to the `isUpgrade` setting. Keep or enter a tag to run only that
category, or enter a test ID to run one case.

For Emulator Critical Flow, leaving `TAGS` empty uses `@criticalFlow`; it does
not select all tests.

### Upgrade tests

Alpha, Beta and Prod include upgrade tests by default. F-Droid defaults to
upgrade mode off. The Emulator Critical Flow workflow does not offer an upgrade
toggle; its default managed-pool runs exclude upgrade tests.

- Turn off `isUpgrade` to skip upgrade-tagged tests in a category run.
- Leave `oldBuildNumber` empty to automatically select the previous APK,
  or supply a specific old build number.
- For a category run with upgrades enabled, non-upgrade tests run first on
  the new APK. The matching upgrade subset runs afterward on one device.
- Each upgrade-tagged test starts from the old APK and upgrades to the new APK;
  it must not inherit the upgraded installation from the previous test.
- When targeting an upgrade test directly by ID, enable `isUpgrade` so both
  APKs are available. Selecting `@upgrade` also enables upgrade mode outside
  managed-emulator runs.

### Testiny reporting

Prod runs always prepare one shared Testiny run before parallel device execution. With
`testinyRunName` left empty, its title is generated from the resolved APK
version, Berlin date and GitHub run number, for example:

```text
v4.37.0-47150-prod-regression-28/10/2026-run-25
```

Enter a name to override it. An existing open run with that title is reused;
otherwise a run is created.

Prod deflake reuses the source run's Testiny title so results update the same
Testiny run. The manual name input does not override the original title.

## Retry failed tests or run manual deflake

Automatic retries happen within the current run and execute only tests that
remain unsuccessful. A count of `2` means one initial execution and up to two
additional attempts. Allure `failed`, `broken` and `unknown` results are
included in the failed-test list.

## Manual deflake

Rerun the remaining failures from a previous run:

1. Open **Run workflow** in the **same workflow** as the source run.
2. Choose **Deflake failed tests**.
3. Enter the source run's `manual deflake id`, found in its summary, or leave
   `sourceRunId` empty for that workflow's latest completed run on `develop`.
4. Select the device group used by the source run.
5. Leave `rerunFailedCount` blank for no additional retries, or enter
   `0`–`3`.
6. Start the run.

The manual deflake ID is the GitHub Actions run ID. It appears when the source
run exports the `android-ui-test-deflake-input` artifact.

Deflake reuses the source APKs, flavor, selector, upgrade settings, device
selection and test-source revision. It runs only the remaining failures and
publishes a separate report. The current hardened CI harness is retained;
the saved source revision supplies `testsCore` and `testsSupport` sources.

A normal or earlier deflake run can be the source, provided it belongs to the
same workflow on the repository's default branch and completed with success
or failure. If its artifact is unavailable or there are no remaining failures,
the workflow stops; it does not silently select an older run. Cancelled and
timed-out source runs are rejected.

GitHub shows the same input form in both modes. Normal-only settings are
ignored during deflake. The retry count belongs to the current deflake run,
not the source run.

## Test-service configuration

The selected URL is compiled into the instrumentation APK, not the Wire app.
The devices must be able to reach it.

- Normal CI runs use the GitHub Actions variable `TEST_SERVICE_URL`, unless
  `testServiceUrl` is supplied.
- Deflake uses the source artifact's URL, unless explicitly overridden.
  Legacy artifacts without a URL require an explicit override.
- The URL is recorded in the run summary and deflake artifact. It must be an
  HTTP(S) origin with an optional port; credentials, path prefixes, queries
  and fragments are rejected.
- Selecting a URL does not deploy or update the test service.

For local runs, use the ignored root `local.properties`:

```properties
testServiceUrl=http://192.168.2.18:8082
```

Local precedence is `TEST_SERVICE_URL` environment variable, then
`-PtestServiceUrl=...`, then `local.properties`. See the
[local test guide](../../../tests/testsCore/README.md) for instrumentation
commands and local Allure reports.

## Devices and runner setup

Device groups are configured in the repository variable
`ANDROID_DEVICE_GROUPS_JSON`. Group names must match its keys exactly,
and a serial cannot belong to multiple groups.

For existing-device groups, CI uses the online devices in the selected group.
It warns if some are offline and fails if none are online. A specific serial
must be online and belong to that group. A single test ID uses one device;
broader selections are sharded across the selected online devices.

The Emulator Critical Flow workflow manages four pre-provisioned emulators
when `androidDeviceGroup=criticalFlow`:

- AVDs: `critical-flow-1` through `critical-flow-4`.
- Serials: `emulator-5554`, `emulator-5556`, `emulator-5558`, `emulator-5560`.
- These four serials must be the `criticalFlow` entries in the group variable.
- CI validates, starts, checks, collects diagnostics from and stops the pool.
  Required runner resources and AVD settings are checked by
  [emulator_pool.py](emulator_pool.py); the workflow does not provision AVDs.

Entering another configured group in the Emulator Critical Flow workflow uses
that group's existing devices without starting the managed pool. F-Droid also
allows another configured group instead of its `GrapheneOS` default.

Group-level locks prevent overlapping runs for the same group. A shared office
runner lock also serializes execution jobs across suites, so physical-device
tests and the managed emulator pool do not compete for runner resources.

Flavor-to-APK-folder and package mappings come from
`/etc/android-qa/flavors.json` on the runner. Secret-bearing execution jobs use
the protected `android-e2e` GitHub environment. Configure required reviewers
and default-branch restrictions there; workflow validation alone is not the
security boundary.

## Reports and failure visibility

Each suite publishes its own Allure history on GitHub Pages and sends a report
link to the QA Wire conversation. Find the link in the workflow summary or
Wire notification. Normal and deflake runs share the suite's reports page,
but each run has its own report.

Automatic attempts are merged using the latest outcome per logical test.
A test that later passes is reported as passed and labelled
`passed_on_rerun=true`. Summaries distinguish initial failures, recovered tests
and remaining failures. Deflake also reports skipped or missing results;
missing selected results are not counted as passes.

Published reports are sanitized: screenshots and other attachments, failure
messages, stack traces and test parameters are removed, and physical device
serials are anonymized. A sanitized report alone cannot explain a CI-only
failure. Keep GitHub Pages access as restricted as the repository supports.

## Maintainer references

- [validation.sh](validation.sh): input, retry and selector validation.
- [execution_setup.sh](execution_setup.sh): runner, APK, device and secret setup.
- [run_ui_tests.sh](run_ui_tests.sh): sharding, instrumentation and retries.
- [reporting.sh](reporting.sh): Allure publication, deflake bundles and cleanup.
- [prepare_testiny_run.py](prepare_testiny_run.py): prepare Prod's shared Testiny run.
- [validate_source_run.py](validate_source_run.py) and
  [inspect_deflake_bundle.py](inspect_deflake_bundle.py): validate a deflake source.
- [resolve_test_service_url.py](resolve_test_service_url.py): validate service URLs.

Run the existing script tests locally without contacting Testiny or starting
instrumentation:

```bash
python3 -B -m unittest discover -s scripts/qa/android-ui -p 'test_*.py'
```
