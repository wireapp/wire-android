.DEFAULT_GOAL := help

MAKE_DIR := make

include $(MAKE_DIR)/common.mk
include $(MAKE_DIR)/android.mk
include $(MAKE_DIR)/dev.mk
include $(MAKE_DIR)/qa.mk
include $(MAKE_DIR)/release.mk

.PHONY: help
help:
	@printf "Available targets:\n"
	@printf "  assemble/staging-debug      Assemble the staging debug APK\n"
	@printf "  install/staging-debug       Install the staging debug APK on the connected device\n"
	@printf "  emm/install/staging-debug   Install the staging debug APK on the managed work profile\n"
	@printf "  lint                        Run Android lint\n"
	@printf "  style                       Run detekt checks\n"
	@printf "  unit-tests                  Run build logic tests and unit coverage\n"
	@printf "  ui-tests                    Run acceptance/UI tests\n"
	@printf "  build-dev                   Assemble dev debug APK\n"
	@printf "  build-prod-apk              Assemble prod release APK\n"
	@printf "  build-prod-bundle           Build prod release AAB\n"
	@printf "  build-prod                  Build prod APK and AAB\n"
	@printf "  compose-stability           Run Compose stability checks\n"
	@printf "  screenshots-verify          Validate screenshot tests\n"
	@printf "  screenshots-update          Update screenshot references\n"
	@printf "  baseline-profile            Generate baseline and startup profiles\n"
	@printf "  release-notes               Prepare Play release notes\n"
	@printf "  qa-deflake                  Run Android UI deflake entrypoint\n"
	@printf "  new-feature name=foo        Create a feature module\n"
