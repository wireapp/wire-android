.PHONY: release-notes release-notes/prepare release-notes/format signing-lineage beta-release/validate-source

beta-release/validate-source:
	@python3 scripts/release/validate-beta-source.py

release-notes: release-notes/prepare release-notes/format

release-notes/prepare:
	bash scripts/release/prepare-release-notes.sh

release-notes/format:
	python3 scripts/release/generate-whatsnew-notes.py

signing-lineage:
	bash scripts/signing/lineage-sign-apks.sh
