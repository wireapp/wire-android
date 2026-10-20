#!/usr/bin/env python3
"""Build ONLY standalone probe/agent and optional tiny self-test. Never invokes Gradle."""
import argparse, hashlib, json, os, pathlib, shutil, subprocess, sys, zipfile
from repo_paths import SOURCE as HERE, OUTPUT
p = argparse.ArgumentParser(); p.add_argument('--smoke', action='store_true'); a = p.parse_args()
java_home = pathlib.Path(os.environ.get('JAVA_HOME') or subprocess.check_output(['/usr/libexec/java_home', '-v', '21'], text=True).strip())
cache = pathlib.Path.home()/'.gradle/caches/modules-2/files-2.1/org.ow2.asm/asm'
jars = sorted(x for x in cache.glob('*/*/asm-*.jar') if not any(s in x.name for s in ['sources', 'javadoc']))
if not jars: raise SystemExit('No cached ASM core JAR; set up caches before running. No downloading is attempted.')
# Reproducible cached choice rather than lexicographic version ordering.
asm = next((x for x in jars if x.name == 'asm-9.9.1.jar'), jars[-1])
build = OUTPUT/'tools'; build.mkdir(parents=True, exist_ok=True)
classes = build/'classes'; classes.mkdir(exist_ok=True)
source = HERE/'src/com/wire/metro/measure'
subprocess.run([str(java_home/'bin/javac'), '--release', '17', '-cp', str(asm), '-d', str(classes), str(source/'Probe.java'), str(source/'ConstructorAgent.java')], check=True)
with zipfile.ZipFile(build/'probe.jar', 'w', zipfile.ZIP_DEFLATED) as jar:
    for f in sorted(classes.rglob('Probe*.class')): jar.write(f, f.relative_to(classes).as_posix())
with zipfile.ZipFile(build/'constructors-agent.jar', 'w', zipfile.ZIP_DEFLATED) as jar:
    jar.writestr('META-INF/MANIFEST.MF', 'Manifest-Version: 1.0\nPremain-Class: com.wire.metro.measure.ConstructorAgent\nCan-Redefine-Classes: false\nCan-Retransform-Classes: false\n\n')
    for f in sorted(classes.rglob('ConstructorAgent*.class')): jar.write(f, f.relative_to(classes).as_posix())
    with zipfile.ZipFile(asm) as upstream:
        for name in upstream.namelist():
            if name.startswith('org/objectweb/asm/') and name.endswith('.class'): jar.writestr(name, upstream.read(name))
hashfile = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
(build/'build-provenance.json').write_text(json.dumps({'java_home': str(java_home), 'javac': subprocess.check_output([str(java_home/'bin/javac'), '-version'], text=True).strip(), 'asm': str(asm), 'asm_sha256': hashfile(asm), 'sources': {str(x.relative_to(HERE)):hashfile(x) for x in source.glob('*.java')}, 'jars':{x.name:hashfile(x) for x in build.glob('*.jar')}}, indent=2)+'\n')
if a.smoke:
    smoke = OUTPUT/'smoke'; smoke.mkdir(parents=True, exist_ok=True)
    selection = smoke/'selection.txt'
    selection.write_text('\n'.join('smoke.Smoke$'+name for name in ['Base','Subject','PlainFail','OuterFail','DelegatingOuterFail','ThrowingBase','ChildOfThrowingBase'])+'\n')
    subprocess.run([str(java_home/'bin/javac'), '--release', '17', '-cp', str(build/'probe.jar'), '-d', str(smoke/'classes'), str(HERE/'smoke/Smoke.java')], check=True)
    def execute(mode, case='normal'):
        out = smoke/(mode if case == 'normal' else case)
        if out.exists(): shutil.rmtree(out) # ONLY generated standalone self-test outputs.
        cmd = [str(java_home/'bin/java'), '-Xverify:all', '-Xbootclasspath/a:'+str(build/'probe.jar'), '-Dmetro.measure.output='+str(out), '-Dsmoke.case='+case]
        if mode == 'constructors': cmd += ['-javaagent:'+str(build/'constructors-agent.jar')+'='+str(selection)]
        if case == 'limit': cmd += ['-Dmetro.measure.identityLimit=2']
        subprocess.run(cmd+['-cp', str(smoke/'classes'), 'smoke.Smoke'], check=True)
        rows = [json.loads(line) for x in out.glob('*.jsonl') for line in x.read_text().splitlines()]
        assert rows[-1]['bootstrap_probe'] and rows[-1]['retained_identity_references'] == 0
        assert not rows[-1]['transform_errors']
        return rows
    for mode in ['timing', 'constructors']:
        rows = execute(mode)
        assert not rows[-1]['counter_errors']
        samples = {r['phase']:r for r in rows if r['type']=='sample'}
        assert len(samples) == 4
        first = samples['standalone_identity_semantics']
        assert first['success'] and not samples['standalone_action_failure']['success']
        for row in samples.values():
            assert row['metric_schema'] == 2 and row['retained_identity_references_after_end'] == 0
            assert (row['caller_allocated_bytes'] is not None) == (mode == 'timing')
            assert row['constructor_return_identities_valid'] == (mode == 'constructors')
        if mode == 'constructors':
            assert sum(first['constructor_entries_all_threads'].values()) == 19, first
            assert sum(first['constructor_entries_caller_thread'].values()) == 17, first
            assert first['constructor_entries_all_threads']['smoke.Smoke$Base#<init>()V'] == 7
            assert first['constructor_return_unique_identities_all_threads'] == 8, first
            assert first['constructor_return_unique_identities_caller_thread'] == 7, first
            types = first['constructor_return_unique_identities_by_runtime_class']
            assert sum(x['all_threads'] for x in types) == 8 and sum(x['caller_thread'] for x in types) == 7
            subject = [x for x in types if x['runtime_class']=='smoke.Smoke$Subject']
            assert len(subject)==2 and len({x['defining_loader_window_id'] for x in subject})==2
            assert sorted((x['all_threads'],x['caller_thread']) for x in subject)==[(1,1),(3,2)]
            for name in ['Base','OuterFail','DelegatingOuterFail','UnselectedChild']:
                assert sum(x['all_threads'] for x in types if x['runtime_class']=='smoke.Smoke$'+name)==1
            assert not any(x['runtime_class'].endswith(('PlainFail','ThrowingBase')) for x in types)
            for phase, entries, identities in [('standalone_second_window',3,1),('standalone_empty_window',0,0),('standalone_action_failure',3,1)]:
                row=samples[phase]
                assert sum(row['constructor_entries_all_threads'].values())==entries
                assert row['constructor_return_unique_identities_all_threads']==identities
        else:
            assert first['constructor_entries_all_threads'] is None
            assert first['constructor_return_unique_identities_all_threads'] is None
            assert first['constructor_return_unique_identities_by_runtime_class'] is None
    for case in ['limit','abandon']:
        rows=execute('constructors',case)
        assert rows[-1]['counter_errors']
        if case=='limit':
            sample=next(x for x in rows if x['type']=='sample')
            assert not sample['success'] and not sample['constructor_return_identities_valid']
            assert sample['constructor_return_unique_identities_all_threads'] is None
            assert sum(sample['constructor_entries_all_threads'].values())==9
            assert sample['retained_identity_references_after_end']==0
    subprocess.run([sys.executable, str(HERE/'smoke/check_reports.py')], check=True)
    subprocess.run([sys.executable, str(HERE/'smoke/check_archives.py')], check=True)
    print('Standalone ASM smoke passed: ENTRY/RETURN identity semantics, loaders, threads, failures, bounded retention and window guards. Not SDK measurements.')
print(str(build/'probe.jar')); print(str(build/'constructors-agent.jar'))
