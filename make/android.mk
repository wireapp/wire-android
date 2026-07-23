# Staging apk
STAGING_APK_PATH = $(wildcard app/build/outputs/apk/staging/debug/com.*.apk)

# Get user id for sample work profile
WORK_PROFILE = $(shell adb shell pm list users | grep "Managed Profile")
WORK_PROFILE_ID = $(shell echo "$(WORK_PROFILE)" | awk -F'[:{}]' '{print $$2}')

.PHONY: assemble/staging-debug install/staging-debug emm/install/staging-debug
.PHONY: lint style unit-tests ui-tests build-dev build-prod-apk build-prod-bundle build-prod
.PHONY: compose-stability screenshots-verify screenshots-update baseline-profile

assemble/staging-debug:
	@printf "🔧️$(PURPLE)Assembling staging debug build...$(NC)\n"
	$(GRADLE) assembleStagingDebug

install/staging-debug:
	@printf "🚀$(PURPLE)Installing staging debug build on connected device...$(NC)\n"
	adb install -r $(STAGING_APK_PATH)

emm/install/staging-debug:
	@printf "🚀$(PURPLE)Installing staging debug build on connected device on work-profile...$(NC)\n"
	adb install --user $(WORK_PROFILE_ID) -r $(STAGING_APK_PATH)

lint:
	$(GRADLE) lint --no-daemon --no-configuration-cache -Pskip.aboutlibraries=true

style:
	$(GRADLE) detektAll

unit-tests:
	$(GRADLE) -p buildSrc test
	$(GRADLE) -p build-logic :plugins:test
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
