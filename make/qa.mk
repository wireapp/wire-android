.PHONY: qa-deflake qa-ui-setup qa-ui-validate qa-ui-run qa-ui-report

qa-deflake:
	bash $(QA_ANDROID_UI_DIR)/run_ui_tests.sh

qa-ui-setup:
	bash $(QA_ANDROID_UI_DIR)/execution_setup.sh $(cmd)

qa-ui-validate:
	bash $(QA_ANDROID_UI_DIR)/validation.sh $(cmd)

qa-ui-run:
	bash $(QA_ANDROID_UI_DIR)/run_ui_tests.sh

qa-ui-report:
	bash $(QA_ANDROID_UI_DIR)/reporting.sh $(cmd)
