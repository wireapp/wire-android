PURPLE := \033[0;35m
NC := \033[0m # No Color (reset)

# Staging apk
STAGING_APK_PATH := $(wildcard app/build/outputs/apk/staging/debug/com.*.apk)

assemble/staging-debug:
	@echo "🔧️$(PURPLE)Assembling staging debug build...$(NC)"
	./gradlew assembleStagingDebug

install/staging-debug:
	@echo "🚀$(PURPLE)Installing staging debug build on connected device...$(NC)"
	adb install -r $(STAGING_APK_PATH)

emm/install/staging-debug:
	@echo "🚀$(PURPLE)Installing staging debug build on connected device on work-profile...$(NC)"
	adb install --user 10 -r $(STAGING_APK_PATH)
