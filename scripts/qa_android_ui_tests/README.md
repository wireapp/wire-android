# QA Android UI Tests Scripts

These scripts back the workflow:

- `.github/workflows/qa-android-ui-tests.yml`

The workflow now calls a small set of phase-oriented scripts instead of many tiny one-off files.

## Flavor Resolution Source

Flavor resolution is runner-driven, not hardcoded in the repo.

- Source of truth: `/etc/android-qa/flavors.json` (on the self-hosted runner)
- Executed via: `bash scripts/qa_android_ui_tests/execution_setup.sh resolve-flavor`
- Exports for later workflow steps: `S3_FOLDER`, `APP_ID`, `PACKAGES_TO_UNINSTALL`

## Primary Scripts

- `validation.sh`: input validation, TAG selector parsing, and resolved value logging.
- `execution_setup.sh`: runner prep, flavor/APK resolution, device prep, secrets fetch, and test artifact setup.
- `run_ui_tests.sh`: instrumentation execution/sharding across connected devices.
- `reporting.sh`: Allure pull/merge/generate/publish plus cleanup subcommands.

## Python Helpers

- `resolve_flavor.py`: parse `flavors.json` and export flavor-derived env vars.
- `select_apks.py`: resolve NEW/OLD APK keys based on input/build selection rules.
- `fetch_secrets_json.py`: build runtime `secrets.json` from 1Password vault items.
- `merge_allure_results.py`: merge per-device Allure outputs and attach metadata.
