#!/usr/bin/env python3
"""Parent-only fixture staging/execution. Default/help are read-only. Never edits tracked current files."""
import argparse, datetime, hashlib, json, os, pathlib, platform, re, subprocess, sys, tarfile, tempfile
from repo_paths import SOURCE as HERE, ROOT, OUTPUT
import archive_manifest
BASE_ANDROID = '38bbefb59d0c858fbc97f2eab9cec345aabca21b'
BASE_SDK = '92e54877ee318fe6c46aae3a3832e78b3b1adf8b'
DEST = pathlib.Path('app/src/test/kotlin/com/wire/android/di/metro/WireKaliumMeasurementTest.kt')

def fail(message): raise SystemExit(message)
def sha(path): return hashlib.sha256(path.read_bytes()).hexdigest()
def command(args, cwd): return subprocess.check_output(args, cwd=cwd, text=True).strip()
def git(root, *args): return subprocess.check_output(['git', *args], cwd=root, text=True, env=os.environ|{'GIT_OPTIONAL_LOCKS':'0'}).strip()
def write_new(path, value):
    with path.open('x') as f: f.write(json.dumps(value, indent=2)+'\n')
def verify_repo(root, commit, clean=True):
    if not re.fullmatch('[a-f0-9]{40}', commit): fail('Expected an explicit full 40-character commit ID')
    # Top-level BEFORE interpreting HEAD/status; this prevents accidental operations on the original submodule Git dir.
    if pathlib.Path(git(root, 'rev-parse', '--show-toplevel')).resolve() != root.resolve(): fail('Unexpected Git top-level: '+str(root))
    if git(root, 'rev-parse', 'HEAD') != commit: fail('Commit mismatch in '+str(root))
    if clean:
        for args in [('diff', '--quiet', '--ignore-submodules=all'), ('diff', '--cached', '--quiet', '--ignore-submodules=all')]:
            if subprocess.run(['git', *args], cwd=root, env=os.environ|{'GIT_OPTIONAL_LOCKS':'0'}).returncode != 0: fail('Tracked edits/index entries present; refusing to stage/run in '+str(root))

def current_guard(android, sdk):
    if ROOT == pathlib.Path('/Users/mohamadjaara/Desktop/wire/main/wire-android').resolve(): fail('Original workspace is a read-only input')
    verify_repo(ROOT, android); verify_repo(ROOT/'kalium', sdk)

def staged_receipt(variant): return OUTPUT/(variant+'-stage.json')
def baseline_source_manifest(): return OUTPUT/'baseline-source-manifest.json'

def record_baseline_source(root):
    manifest = archive_manifest.capture(root, BASE_ANDROID, BASE_SDK)
    path = baseline_source_manifest()
    write_new(path, manifest)
    return {'path':str(path), 'sha256':sha(path), 'schema':manifest['schema'],
            'entries':len(manifest['entries']),
            'files':sum(value['kind']=='file' for value in manifest['entries'].values())}

def verify_baseline_source(root, stage):
    recorded = stage.get('archive_source_manifest')
    if not recorded:
        fail('Baseline source manifest missing: old archives/receipts cannot be reused. Stage a fresh archive in a new ignored output directory.')
    path = baseline_source_manifest()
    if recorded.get('path') != str(path) or path.is_symlink() or not path.is_file() or sha(path) != recorded.get('sha256'):
        fail('Baseline source manifest missing/changed; refusing archive reuse')
    manifest = json.loads(path.read_text())
    if (manifest.get('android_commit'), manifest.get('sdk_commit')) != (BASE_ANDROID, BASE_SDK):
        fail('Baseline manifest commits differ from the recorded source archives')
    if (stage.get('android_commit'), stage.get('sdk_commit')) != (BASE_ANDROID, BASE_SDK):
        fail('Baseline stage commits differ from the source manifest')
    if (recorded.get('schema'),recorded.get('entries'),recorded.get('files')) != (
            manifest.get('schema'),len(manifest.get('entries',{})),
            sum(value['kind']=='file' for value in manifest.get('entries',{}).values())):
        fail('Baseline source manifest descriptor mismatch')
    try: return archive_manifest.verify(root, manifest)
    except archive_manifest.SourceDrift as failure: fail(str(failure))
def file_guard(root, variant):
    dest = root/DEST
    if dest.exists() or dest.is_symlink(): fail('Destination exists; refusing to overwrite '+str(dest))
    if variant == 'current' and subprocess.run(['git', 'ls-files', '--error-unmatch', str(DEST)], cwd=root, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL).returncode == 0:
        fail('Fixture destination is tracked; refusing to replace it')
    return dest

def stage_current(args):
    current_guard(args.android_commit, args.sdk_commit)
    OUTPUT.mkdir(parents=True, exist_ok=True)
    receipt = staged_receipt('current')
    if receipt.exists(): fail('Stage receipt already exists; inspect/clean it before staging again')
    dest = file_guard(ROOT, 'current'); dest.parent.mkdir(parents=True, exist_ok=True)
    data = (HERE/'fixtures/current.kt').read_bytes()
    with dest.open('xb') as f: f.write(data)
    write_new(receipt, {'variant':'current','root':str(ROOT),'android_commit':args.android_commit,'sdk_commit':args.sdk_commit,
                        'injected':str(dest),'sha256':sha(dest),'fixture_template_sha256':sha(HERE/'fixtures/current.kt')})
    print(receipt)

def extract_archive(repo, commit, destination):
    if destination.exists(): fail('Archive path already exists; refusing to replace '+str(destination))
    destination.mkdir(parents=True)
    with tempfile.TemporaryFile(dir=OUTPUT) as archive:
        subprocess.run(['git', 'archive', '--format=tar', commit], cwd=repo, stdout=archive, check=True)
        archive.seek(0)
        with tarfile.open(fileobj=archive) as tar:
            # data filter rejects escaping links/paths; no Git metadata/worktrees/config is created.
            tar.extractall(destination, filter='data')

def stage_baseline(args):
    current_guard(args.android_commit, args.sdk_commit)
    OUTPUT.mkdir(parents=True, exist_ok=True)
    receipt = staged_receipt('baseline')
    if receipt.exists(): fail('Baseline stage receipt already exists')
    if baseline_source_manifest().exists() or baseline_source_manifest().is_symlink(): fail('Baseline source manifest already exists; use a fresh ignored output directory')
    root = OUTPUT/'archives/baseline/android'
    # Exact Git source archives; a complete post-staging inventory guards reuse.
    # Never modify/reuse the pre-existing adr-base archive.
    extract_archive(ROOT, BASE_ANDROID, root)
    # git archive may emit the submodule's empty gitlink directory.
    sdk_dir = root/'kalium'
    if sdk_dir.exists():
        if sdk_dir.is_symlink() or not sdk_dir.is_dir() or any(sdk_dir.iterdir()): fail('Unexpected nonempty archive submodule path')
        sdk_dir.rmdir()
    extract_archive(ROOT/'kalium', BASE_SDK, sdk_dir)
    includes = root/'include_builds.gradle.kts'; before = sha(includes)
    text = includes.read_text()
    needle = '    dependencySubstitution {'
    if text.count(needle) != 1: fail('Baseline include_builds shape differs; inspect the archive manually')
    text = text.replace(needle, needle+'\n        substitute(module("com.wire.kalium:kalium-persistence")).using(project(":data:persistence"))', 1)
    includes.write_text(text)
    dest = file_guard(root, 'baseline'); dest.parent.mkdir(parents=True, exist_ok=True)
    with dest.open('xb') as f: f.write((HERE/'fixtures/baseline.kt').read_bytes())
    source_manifest = record_baseline_source(root) # All Android + SDK entries, including fixture/patch; before any build.
    write_new(receipt, {'variant':'baseline','root':str(root),'android_commit':BASE_ANDROID,'sdk_commit':BASE_SDK,
                        'injected':str(dest),'sha256':sha(dest),'fixture_template_sha256':sha(HERE/'fixtures/baseline.kt'),
                        'archive_source_manifest':source_manifest,
                        'archive_only_patch':{'path':str(includes),'original_sha256':before,'patched_sha256':sha(includes),
                                              'purpose':'test-only persistence dependency composite substitution'}})
    print(receipt)

def read_stage(variant):
    receipt = staged_receipt(variant)
    data = json.loads(receipt.read_text())
    root = pathlib.Path(data['root']); dest = pathlib.Path(data['injected'])
    if dest != root/DEST or not dest.is_file() or dest.is_symlink() or sha(dest) != data['sha256']:
        fail('Injected fixture changed/missing; refusing to run or delete user edits')
    if variant == 'current': current_guard(data['android_commit'], data['sdk_commit'])
    else:
        if root != OUTPUT/'archives/baseline/android' or (root/'.git').exists(): fail('Unexpected baseline archive root')
        verify_baseline_source(root, data) # Every read/reuse, including run preflight and EVERY fork.
        patch = data['archive_only_patch']
        if sha(pathlib.Path(patch['path'])) != patch['patched_sha256']: fail('Baseline composite substitution changed')
    return data

def cleanup(args):
    stage = read_stage(args.variant)
    pathlib.Path(stage['injected']).unlink() # Only the exact injected bytes; no git checkout/reset/clean.
    staged_receipt(args.variant).unlink()
    print('Removed only the unchanged injected fixture and its receipt. Archives/results retained.')

def provenance(stage):
    root = pathlib.Path(stage['root'])
    files = ['gradle/libs.versions.toml','gradle.properties','kalium/gradle/libs.versions.toml','kalium/gradle.properties',
             'gradle/wrapper/gradle-wrapper.properties','kalium/gradle/wrapper/gradle-wrapper.properties']
    facts = {f:{'sha256':sha(root/f),'text':(root/f).read_text()} for f in files if (root/f).is_file()}
    return {'schema':1,'stage':stage,'created_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),
            'host':{'platform':platform.platform(),'machine':platform.machine(),'cpu_count':os.cpu_count()},
            'JAVA_HOME':os.environ.get('JAVA_HOME'),'ANDROID_HOME':os.environ.get('ANDROID_HOME'),
            'build_inputs':facts,'harness_files':{str(f.relative_to(HERE)):sha(f) for f in sorted([*HERE.glob('*.py'), *HERE.glob('*.sh'), HERE/'README.md', HERE/'measurement.init.gradle', HERE/'constructors.txt', *HERE.glob('fixtures/*.kt'), *HERE.glob('src/com/wire/metro/measure/*.java')])},
            'tool_build':json.loads((OUTPUT/'tools/build-provenance.json').read_text()),
            'limits':['new user/session owner in a warmed Robolectric JVM, not process-cold application launch',
                      'ThreadMXBean bytes are an approximation for the timed caller platform thread only',
                      'ENTRY is calls, not allocations; RETURN identities dedupe within window only, may include later-failed outer constructors',
                      'RETURN identities strongly retain selected objects during instrumented windows, cap failure invalidates counts; cleared on close',
                      'count pass perturbs execution and is never merged into timing-pass elapsed/byte summaries',
                      'all-thread snapshots include asynchronous startup overlap and measurement boundary bookkeeping',
                      'fixed observation interval is not a quiescence/startup-complete barrier; warm VM uses existing session',
                      'same SQLite seed and external ports; no registered client/native crypto/full sync/production app graph',
                      'Kotlin BEFORE 2.3.20 vs current 2.4.20 plus pins: whole migration comparison, no DI-only causal claim']}

def execute_gradle(args, root, log):
    return subprocess.run(args,cwd=root,stdout=log,stderr=subprocess.STDOUT).returncode

def verify_control(fork, pass_name):
    import xml.etree.ElementTree as ET
    import summarize
    reports = [ET.parse(f).getroot() for f in (fork/'junit-xml').glob('*.xml')]
    tests = [t for report in reports for t in report.iter('testcase')]
    if len(tests) != 1 or tests[0].get('classname') != 'com.wire.android.di.metro.WireKaliumMeasurementTest' or any(
            report.find('.//'+kind) is not None for report in reports for kind in ['failure','error','skipped']):
        fail('Callback preflight requires exactly one fresh passing measurement testcase: '+str(fork))
    files = list(fork.glob('metrics-*.jsonl'))
    if len(files) != 1: fail('Callback preflight requires exactly one probe JVM: '+str(fork))
    rows = [json.loads(line) for line in files[0].read_text().splitlines()]
    meta = rows[-1] if rows else {}
    if (meta.get('type') != 'jvm' or meta.get('metric_schema') != 2 or meta.get('pass') != pass_name
            or meta.get('bootstrap_probe') is not True or meta.get('callback_control_passed') is not True
            or meta.get('callback_control_loader') != 'org.robolectric.internal.AndroidSandbox$SdkSandboxClassLoader'
            or meta.get('retained_identity_references') != 0 or meta.get('transform_errors') or meta.get('counter_errors')):
        fail('Callback preflight probe/loader failure: '+str(fork))
    controls = [row for row in rows if row.get('type') == 'callback_control']
    if len(controls) != 1 or controls[0].get('pass') != pass_name or any(row.get('type') == 'sample' for row in rows):
        fail('Callback-only preflight must contain one control and zero SDK samples: '+str(fork))
    summarize.validate_callback_control(controls[0])
    write_new(fork/'callback-control-validation.json', {'status':'PASS','pass':pass_name,'junit_cases':1,'sdk_samples':0})

def run(args):
    if args.identity_limit < 1: fail('identity limit must be positive')
    if args.forks < 1 or args.warmups < 0 or args.repetitions < 1 or args.warm_vms < 1 or args.observation_ms < 0: fail('Invalid run counts')
    if not os.environ.get('ANDROID_HOME'): fail('Parent must supply ANDROID_HOME (no local/shared config is written)')
    stage = read_stage(args.variant)
    for f in ['tools/probe.jar','tools/constructors-agent.jar','tools/build-provenance.json']:
        if not (OUTPUT/f).is_file(): fail('Build standalone utilities first: python3 build-tools.py --smoke')
    build = json.loads((OUTPUT/'tools/build-provenance.json').read_text())
    for name,digest in build['jars'].items():
        if sha(OUTPUT/'tools'/name) != digest: fail('Tool JAR differs from build provenance: '+name)
    for name,digest in build['sources'].items():
        if sha(HERE/name) != digest: fail('Utility source changed after build: '+name)
    result = (OUTPUT/'results'/args.label).resolve()
    if result.parent != (OUTPUT/'results').resolve() or not re.fullmatch('[A-Za-z0-9_.-]+', args.label): fail('Use a simple unique run label')
    result.mkdir(parents=True, exist_ok=False)
    import shutil
    shutil.copyfile(HERE/'constructors.txt', result/'constructors.txt')
    shutil.copyfile(pathlib.Path(stage['injected']), result/'fixture.kt')
    if args.variant == 'baseline': shutil.copyfile(baseline_source_manifest(), result/'baseline-source-manifest.json')
    config = {'root':stage['root'],'variant':args.variant,'pass':args.pass_name,'scenario':args.scenario,'controlOnly':args.control_only,'probe':str(OUTPUT/'tools/probe.jar'),
              'agent':str(OUTPUT/'tools/constructors-agent.jar'),'selection':str(result/'constructors.txt'),
              'warmups':args.warmups,'repetitions':args.repetitions,'warmVms':args.warm_vms,'observationMs':args.observation_ms,'identityLimit':args.identity_limit}
    write_new(result/'provenance.json',provenance(stage))
    write_new(result/'run.json', {'config':config,'requested_forks':args.forks,'complete':False})
    for i in range(args.forks):
        read_stage(args.variant)
        fork = result/('fork-%02d'%i); fork.mkdir()
        cfg = config|{'output':str(fork)}
        write_new(fork/'config.json',cfg)
        gradle = [str(pathlib.Path(stage['root'])/'gradlew'), '-I', str(HERE/'measurement.init.gradle'),
                  '-Dmetro.measure.config='+str(fork/'config.json'), '-Pkalium.publish.version='+stage['sdk_commit'], '--no-parallel', '--max-workers=2',
                  ':app:testDevDebugUnitTest','--tests','com.wire.android.di.metro.WireKaliumMeasurementTest']
        extra = args.gradle_args
        if extra[:1] == ['--']: extra = extra[1:]
        gradle += extra
        write_new(fork/'command.json',gradle)
        xml = pathlib.Path(stage['root'])/'app/build/test-results/testDevDebugUnitTest'
        if xml.exists(): shutil.move(str(xml), str(fork/'previous-junit-xml'))
        print('Parent build/execution: '+str(fork), flush=True)
        with (fork/'gradle.log').open('wb') as log:
            status = execute_gradle(gradle,stage['root'],log)
        write_new(fork/'exit.json',{'returncode':status})
        read_stage(args.variant) # Detect source mutation during a fork too, including the final one.
        # Preserve actual JUnit XML as evidence; no trusting exit code alone.
        if xml.exists():
            import shutil
            shutil.copytree(xml, fork/'junit-xml',ignore=shutil.ignore_patterns('binary'))
        if status: fail('Gradle failed; inspect '+str(fork/'gradle.log')+'. Run is incomplete and must not be summarized as passed.')
        if args.control_only: verify_control(fork, args.pass_name)
    (result/'run.json').write_text(json.dumps({'config':config,'requested_forks':args.forks,'complete':True},indent=2)+'\n')
    print(result)

p=argparse.ArgumentParser(description=__doc__); sub=p.add_subparsers(dest='command',required=True)
for name,func in [('stage-current',stage_current),('stage-baseline',stage_baseline)]:
    q=sub.add_parser(name); q.add_argument('--android-commit',required=True); q.add_argument('--sdk-commit',required=True); q.set_defaults(func=func)
q=sub.add_parser('cleanup'); q.add_argument('--variant',choices=['current','baseline'],required=True); q.set_defaults(func=cleanup)
q=sub.add_parser('run'); q.add_argument('--variant',choices=['current','baseline'],required=True); q.add_argument('--pass',dest='pass_name',choices=['timing','constructors'],required=True)
q.add_argument('--control-only',action='store_true',help='Run only the real-Robolectric callback control; never produces a cohort comparison')
q.add_argument('--scenario',choices=['vm','startup-only'],default='vm'); q.add_argument('--label',required=True); q.add_argument('--forks',type=int,default=3); q.add_argument('--warmups',type=int,default=3); q.add_argument('--repetitions',type=int,default=7)
q.add_argument('--identity-limit',type=int,default=250000); q.add_argument('--warm-vms',type=int,default=10); q.add_argument('--observation-ms',type=int,default=250); q.add_argument('gradle_args',nargs=argparse.REMAINDER); q.set_defaults(func=run)
if __name__=='__main__':
    args=p.parse_args(); args.func(args)
