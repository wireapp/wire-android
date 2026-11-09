.PHONY: release-notes release-notes/prepare release-notes/format signing-lineage beta-release/validate-source release-log/check release-log/context release-log/build

beta-release/validate-source:
	@python3 scripts/release/validate-beta-source.py

release-notes: release-notes/prepare release-notes/format

release-notes/prepare:
	bash scripts/release/prepare-release-notes.sh

release-notes/format:
	python3 scripts/release/generate-whatsnew-notes.py

signing-lineage:
	bash scripts/signing/lineage-sign-apks.sh

release-log/check:
	python3 -m unittest discover -s scripts/release -p 'test_release_log_*.py'

release-log/context:
	python3 scripts/release/release_log_export.py context --event "$(event)" --output "$(output)"

release-log/build:
	python3 scripts/release/release_log_export.py build --event "$(event)" --flavor "$(flavor)" --variant "$(variant)" --output "$(output)"
