.PHONY: new-feature tail-logcat apk-wrapper

new-feature:
	@test -n "$(name)" || (printf "Usage: make new-feature name=foo\n" >&2; exit 1)
	bash scripts/dev/new-feature-module.sh "$(name)"

tail-logcat:
	bash scripts/dev/tail-logcat.sh

apk-wrapper:
	bash scripts/dev/wire-apk-wrapper.sh $(args)
