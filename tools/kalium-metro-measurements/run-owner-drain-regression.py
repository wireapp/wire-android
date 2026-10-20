#!/usr/bin/env python3
"""Run the fixture's separate cleanup regression before any measurement cohorts."""
import argparse
import json
import pathlib
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET

import harness

p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--variant', choices=['baseline', 'current'], required=True)
p.add_argument('--label', required=True)
a = p.parse_args()
if not re.fullmatch('[A-Za-z0-9_.-]+', a.label):
    raise SystemExit('Use a simple unique regression label')
stage = harness.read_stage(a.variant)
root = pathlib.Path(stage['root'])
out = harness.OUTPUT / 'regressions' / a.label
out.mkdir(parents=True, exist_ok=False)
cmd = [str(root / 'gradlew'), '-I', str(harness.HERE / 'owner-drain-regression.init.gradle'),
       '-Pkalium.publish.version=' + stage['sdk_commit'], '--no-parallel', '--max-workers=2',
       ':app:testDevDebugUnitTest', '--tests', 'com.wire.android.di.metro.WireKaliumMeasurementOwnerDrainTest',
       '--console=plain']
(out / 'command.json').write_text(json.dumps(cmd, indent=2) + '\n')
(out / 'provenance.json').write_text(json.dumps(harness.provenance(stage), indent=2) + '\n')
with (out / 'gradle.log').open('wb') as log:
    status = subprocess.run(cmd, cwd=root, stdout=log, stderr=subprocess.STDOUT).returncode
(out / 'exit.json').write_text(json.dumps({'returncode': status}) + '\n')
harness.read_stage(a.variant)
xml = root / 'app/build/test-results/testDevDebugUnitTest'
if xml.exists():
    shutil.copytree(xml, out / 'junit-xml', ignore=shutil.ignore_patterns('binary'))
tests = []
for f in (out / 'junit-xml').glob('*.xml'):
    tests += [t for t in ET.parse(f).getroot().iter('testcase')
              if t.get('classname') == 'com.wire.android.di.metro.WireKaliumMeasurementOwnerDrainTest']
if status or len(tests) != 6 or any(t.find(k) is not None for t in tests for k in ['failure', 'error', 'skipped']):
    raise SystemExit('Cleanup regression failed or did not execute six passing tests: ' + str(out))
print('Six cleanup regressions passed: ' + str(out))
