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
	@printf "  lint                        Run Android lint\n"
	@printf "  style                       Run detekt checks\n"
	@printf "  unit-tests                  Run build logic tests and unit coverage\n"
	@printf "  unit-tests/build-logic      Run buildSrc and build-logic tests\n"
	@printf "  unit-tests/source           Run source unit tests with coverage\n"
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
	@printf "  release-notes/prepare       Prepare raw Play release notes\n"
	@printf "  release-notes/format        Format latest Play release notes\n"
	@printf "  beta-release/validate-source Validate beta source against live release-cycle tips\n"
	@printf "  release-log/check           Run release log unit tests\n"
	@printf "  release-log/context         Write the release log context record (event=event.json output=context.json)\n"
	@printf "  release-log/build           Write the signed APK record (event= flavor= variant= output=)\n"
	@printf "  release-log/qa-export       Write the production regression record from the QA step environment (output=)\n"
	@printf "  release-log/collect         Collect release log evidence for a workflow run (run_id= run_attempt= output=)\n"
	@printf "  release-log/publish         Send a release log entry to Confluence (payload= summary= dry_run=1)\n"
	@printf "  qa-deflake                  Run Android UI deflake entrypoint\n"
	@printf "  new-feature name=foo        Create a feature module\n"
