PURPLE := \033[0;35m
NC := \033[0m # No Color (reset)

# Staging apk
STAGING_APK_PATH := $(wildcard app/build/outputs/apk/staging/debug/com.*.apk)

# Get user id for sample work profile
WORK_PROFILE := $(shell adb shell pm list users | grep Sample)
WORK_PROFILE_ID := $(shell echo "$(WORK_PROFILE)" | awk -F'[:{}]' '{print $$2}')

assemble/staging-debug:
	@echo "🔧️$(PURPLE)Assembling staging debug build...$(NC)"
	./gradlew assembleStagingDebug

install/staging-debug:
	@echo "🚀$(PURPLE)Installing staging debug build on connected device...$(NC)"
	adb install -r $(STAGING_APK_PATH)

emm/install/staging-debug:
	@echo "🚀$(PURPLE)Installing staging debug build on connected device on work-profile...$(NC)"
	adb install --user $(WORK_PROFILE_ID) -r $(STAGING_APK_PATH)
