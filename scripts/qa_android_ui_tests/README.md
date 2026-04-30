# QA Android UI Tests Scripts

These scripts back the workflow:

- `.github/workflows/qa-android-critical-flow-tests.yml`

The workflow now calls a small set of phase-oriented scripts instead of many tiny one-off files.

## Flavor Resolution Source

Flavor resolution is runner-driven, not hardcoded in the repo.

- Source of truth: `/etc/android-qa/flavors.json` (on the self-hosted runner)
- Executed via: `bash scripts/qa_android_ui_tests/execution_setup.sh resolve-flavor`
- Exports for later workflow steps: `S3_FOLDER`, `APP_ID`, `PACKAGES_TO_UNINSTALL`

## Primary Scripts

- `validation.sh`: input validation, TAG selector parsing, and resolved value logging.
- `execution_setup.sh`: runner prep, flavor/APK resolution, device prep, secrets fetch, and test artifact setup.
- `run_ui_tests.sh`: instrumentation execution/sharding plus failed-test auto-reruns (explicit per-device retry lists with even count balancing).
- `reporting.sh`: Allure pull/merge/generate/publish plus cleanup subcommands.

## Retry Flow

The rerun feature is controlled by workflow inputs:

- `rerunFailedEnabled`: turn failed-test reruns on or off for this workflow run.
- `rerunFailedCount`: maximum number of rerun attempts after attempt `0` completes. Default is `2`.

Execution flow:

1. Run attempt `0` on the selected device set using the normal CI selector (`testCaseId` or `category`).
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

## Python Helpers

- `resolve_flavor.py`: parse `flavors.json` and export flavor-derived env vars.
- `select_apks.py`: resolve NEW/OLD APK keys based on input/build selection rules.
- `fetch_secrets_json.py`: build runtime `secrets.json` from 1Password vault items.
- `merge_allure_results.py`: merge per-device Allure outputs and attach metadata.
- `extract_failed_tests.py`: extract failed test IDs (`Class#method`) from one attempt's Allure result files.
