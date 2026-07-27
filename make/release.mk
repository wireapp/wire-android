.PHONY: release-notes release-notes/prepare release-notes/format signing-lineage

release-notes: release-notes/prepare release-notes/format

release-notes/prepare:
	bash scripts/release/prepare-release-notes.sh

release-notes/format:
	python3 scripts/release/generate-whatsnew-notes.py

signing-lineage:
	bash scripts/signing/lineage-sign-apks.sh
