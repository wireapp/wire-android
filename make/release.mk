.PHONY: release-notes signing-lineage

release-notes:
	bash scripts/release/prepare-release-notes.sh
	python3 scripts/release/generate-whatsnew-notes.py

signing-lineage:
	bash scripts/signing/lineage-sign-apks.sh
