#!/usr/bin/env python3
"""Run separate owner-drain and real Robolectric adapter regressions before cohorts."""
import argparse
import json
import pathlib
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET

import harness

OWNER_CLASS = 'com.wire.android.di.metro.WireKaliumMeasurementOwnerDrainTest'
OBSERVER_CLASS = 'com.wire.android.di.metro.WireKaliumMeasurementNetworkObserverTest'
EXPECTED_TESTS = {
    (OWNER_CLASS, 'cancelOnlyEvictionCannotOutrunRetainedSessionChild'),
    (OWNER_CLASS, 'bestEffortProviderTimeoutCannotOutrunRetainedSessionChild'),
    (OWNER_CLASS, 'independentlyOwnedInitializedCellsMustAlsoDrain'),
    (OWNER_CLASS, 'cleanupDeadlineFailsWithoutClosingAnyCapturedResource'),
    (OWNER_CLASS, 'evictionFailureRemainsFatalAfterSafeResourceCleanup'),
    (OWNER_CLASS, 'resourceCloseFailureCannotReportSuccessfulCleanup'),
    (OBSERVER_CLASS, 'twoOrdinaryFixtureCyclesDrainTheEagerNetworkObserver'),
}

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
       ':app:testDevDebugUnitTest', '--tests', OWNER_CLASS, '--tests', OBSERVER_CLASS,
       '--console=plain']
(out / 'command.json').write_text(json.dumps(cmd, indent=2) + '\n')
(out / 'provenance.json').write_text(json.dumps(harness.provenance(stage), indent=2) + '\n')
(out / 'selection.json').write_text(json.dumps(sorted(EXPECTED_TESTS), indent=2) + '\n')
xml = root / 'app/build/test-results/testDevDebugUnitTest'
# Preserve prior evidence separately and require entirely new XML from this invocation.
# The init script also disables test output cache reuse and up-to-date acceptance.
if xml.exists():
    shutil.move(str(xml), str(out / 'previous-junit-xml'))
with (out / 'gradle.log').open('wb') as log:
    status = subprocess.run(cmd, cwd=root, stdout=log, stderr=subprocess.STDOUT).returncode
(out / 'exit.json').write_text(json.dumps({'returncode': status}) + '\n')
harness.read_stage(a.variant)
if xml.exists():
    shutil.copytree(xml, out / 'junit-xml', ignore=shutil.ignore_patterns('binary'))
tests = []
reports = []
for f in (out / 'junit-xml').glob('*.xml'):
    report = ET.parse(f).getroot()
    reports.append(report)
    tests.extend(report.iter('testcase'))
actual = [(t.get('classname'), t.get('name')) for t in tests]
invalid = any(report.find('.//' + kind) is not None
              for report in reports for kind in ['failure', 'error', 'skipped'])
if status or len(actual) != len(EXPECTED_TESTS) or set(actual) != EXPECTED_TESTS or invalid:
    raise SystemExit('Cleanup regressions require exactly seven fresh passing tests: ' + str(out))
print('Seven cleanup regressions passed (six helper, one real two-cycle adapter): ' + str(out))
