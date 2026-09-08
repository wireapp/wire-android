# Get user id for sample work profile
WORK_PROFILE = $(shell adb shell pm list users | grep "Managed Profile")
WORK_PROFILE_ID = $(shell echo "$(WORK_PROFILE)" | awk -F'[:{}]' '{print $$2}')

.PHONY: lint style unit-tests unit-tests/build-logic unit-tests/source ui-tests
.PHONY: build-dev build-prod-apk build-prod-bundle build-prod
.PHONY: compose-stability screenshots-verify screenshots-update baseline-profile

lint:
	$(GRADLE) lint --no-daemon --no-configuration-cache -Pskip.aboutlibraries=true

style:
	$(GRADLE) detektAll

unit-tests: unit-tests/build-logic unit-tests/source

unit-tests/build-logic:
	$(GRADLE) -p buildSrc test
	$(GRADLE) -p build-logic :plugins:test

unit-tests/source:
	$(GRADLE) testCoverage

ui-tests:
	$(GRADLE) runAcceptanceTests

build-dev:
	$(GRADLE) assembleDevDebug

build-prod-apk:
	$(GRADLE) assembleProdRelease

build-prod-bundle:
	$(GRADLE) bundleProdRelease

build-prod: build-prod-apk build-prod-bundle

compose-stability:
	$(GRADLE) compileApp :app:devDebugStabilityCheck debugStabilityCheck --no-daemon --no-configuration-cache -Pskip.aboutlibraries=true

screenshots-verify:
	$(GRADLE) validateAlphaDebugScreenshotTest

screenshots-update:
	$(GRADLE) updateAlphaDebugScreenshotTest

baseline-profile:
	$(GRADLE) :app:generateProdCompatreleaseBaselineProfile \
		-Pandroid.testInstrumentationRunnerArguments.class=com.wire.benchmark.BaselineGenerator \
		-Pandroid.testInstrumentationRunnerArguments.BACKEND_NAME="$${BACKEND_NAME:-STAGING}" \
		-Pandroid.testInstrumentationRunnerArguments.TARGET_PACKAGE="$${TARGET_PACKAGE:-com.wire}" \
		--no-daemon \
		--no-configuration-cache
