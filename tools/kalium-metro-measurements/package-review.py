#!/usr/bin/env python3
"""Create a source-only proposed tools/kalium-metro-measurements tree INSIDE ignored output."""
import argparse, hashlib, json, re, shutil
from repo_paths import SOURCE, OUTPUT
FILES = ['.gitignore','README.md','build-tools.py','repo_paths.py','harness.py','archive_manifest.py','summarize.py',
         'measurement.init.gradle','constructors.txt','parent-commands.sh','parse-only.py','package-review.py',
         'fixtures/current.kt','fixtures/baseline.kt','src/com/wire/metro/measure/Probe.java',
         'src/com/wire/metro/measure/ConstructorAgent.java','smoke/Smoke.java',
         'smoke/ParseKotlin.java','smoke/ParseGroovy.java','smoke/check_reports.py','smoke/check_archives.py']
def digest(path): return hashlib.sha256(path.read_bytes()).hexdigest()
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--label',default='review-staging');p.add_argument('--check',action='store_true');a=p.parse_args()
if not re.fullmatch('[A-Za-z0-9_-]+',a.label): raise SystemExit('Use a simple staging label')
dest=OUTPUT/a.label/'tools/kalium-metro-measurements'
if a.check:
    manifest=json.loads((dest/'source-manifest.json').read_text())
    if set(manifest['files'])!=set(FILES): raise SystemExit('Manifest whitelist mismatch')
    for name,digest_value in manifest['files'].items():
        path=dest/name
        if path.is_symlink() or digest(path)!=digest_value: raise SystemExit('Staged source changed: '+name)
    actual={str(x.relative_to(dest)) for x in dest.rglob('*') if x.is_file() and '__pycache__' not in x.parts}
    if actual!=set(FILES)|{'source-manifest.json'}: raise SystemExit('Unexpected staged artifacts')
    print('Source-only review package manifest verified: '+str(dest))
else:
    for name in FILES:
        if not (SOURCE/name).is_file() or (SOURCE/name).is_symlink(): raise SystemExit('Missing/linked source: '+name)
    dest.mkdir(parents=True,exist_ok=False)
    for name in FILES:
        target=dest/name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(SOURCE/name,target)
    (dest/'source-manifest.json').write_text(json.dumps({'proposed_repository_path':'tools/kalium-metro-measurements',
      'scope':'source tooling + fixtures only; no archives/binaries/reference-source/results',
      'files':{name:digest(dest/name) for name in FILES}},indent=2)+'\n')
    print(dest)
