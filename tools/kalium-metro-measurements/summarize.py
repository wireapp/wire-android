#!/usr/bin/env python3
"""Validate parent execution evidence, then summarize only non-warmup observations by pass."""
import argparse, collections, json, pathlib, statistics, xml.etree.ElementTree as ET
PHASES = ['cold_session_get_or_create','first_vm_new_store','retained_vm_lookup','mandatory_startup_overlap_observation','warm_vm_new_store']

def stats(values):
    return {'n':len(values),'median':statistics.median(values),'min':min(values),'max':max(values)} if values else None

UNITS = {
    'elapsed_ns': 'timing-pass wall nanoseconds; constructor-pass timing is diagnostic only',
    'caller_allocated_bytes': 'uninstrumented caller platform-thread ThreadMXBean heap-byte approximation, not objects or whole-JVM bytes',
    'constructor_entries': 'exact selected <init> ENTRY callbacks by descriptor; delegation/failed construction can enter multiple times; not allocations',
    'constructor_return_unique_identities': 'unique reference identities at selected normal RETURN callbacks accepted within one window, not all JVM allocations or completed outer constructions',
    'runtime_class': 'actual runtime class separated by defining-loader window ID; IDs are local to a window and not stable across samples',
    'all_threads': 'observations in the window, including async startup; no VM-causal attribution',
    'caller_thread': 'observations executing on the timed caller thread, not allocation-thread attribution',
}
IDENTITY_ALL='constructor_return_unique_identities_all_threads'
IDENTITY_CALLER='constructor_return_unique_identities_caller_thread'
IDENTITY_CLASSES='constructor_return_unique_identities_by_runtime_class'

def validate_sample_units(row):
    def require(ok,message):
        if not ok: raise ValueError(message)
    require(row.get('metric_schema')==2, 'Metric schema 2 required; legacy ENTRY-only records cannot supply RETURN identities')
    require(not row.get('transform_errors') and not row.get('counter_errors'), 'Transformation/counter failure')
    require(row.get('retained_identity_references_after_end')==0, 'Identity references remained after window end')
    if row.get('pass')=='timing':
        require(not row.get('constructor_return_identities_valid'), 'Timing pass must not instrument identities')
        for field in [IDENTITY_ALL,IDENTITY_CALLER,IDENTITY_CLASSES,'constructor_entries_all_threads','constructor_entries_caller_thread']:
            require(row.get(field) is None, 'Instrumented data mixed into timing pass: '+field)
        value=row.get('caller_allocated_bytes')
        require(value is None or type(value) is int and value>=0, 'Invalid caller byte approximation')
        return
    require(row.get('pass')=='constructors', 'Unknown measurement pass')
    require(row.get('constructor_return_identities_valid') is True, 'Missing/invalid RETURN identity observations')
    require(row.get('caller_allocated_bytes') is None, 'Instrumented byte values must not enter byte reports')
    total=row.get(IDENTITY_ALL); caller=row.get(IDENTITY_CALLER); classes=row.get(IDENTITY_CLASSES)
    require(type(total) is int and type(caller) is int and 0<=caller<=total, 'Invalid unique RETURN identity totals')
    require(isinstance(classes,list), 'Missing runtime-class identity records')
    seen=set(); summed_all=0; summed_caller=0
    for item in classes:
        key=(item.get('runtime_class'),item.get('defining_loader_window_id'))
        require(isinstance(key[0],str) and type(key[1]) is int and key[1]>=0 and key not in seen, 'Invalid/duplicate runtime-class identity bucket')
        seen.add(key)
        a=item.get('all_threads'); c=item.get('caller_thread')
        require(type(a) is int and type(c) is int and 0<=c<=a, 'Invalid runtime-class identity counts')
        summed_all+=a; summed_caller+=c
    require((summed_all,summed_caller)==(total,caller), 'Runtime-class counts do not equal global unique identities')
    for field in ['constructor_entries_all_threads','constructor_entries_caller_thread']:
        require(isinstance(row.get(field),dict) and all(type(v) is int and v>=0 for v in row[field].values()), 'Invalid ENTRY callback counts')

def identity_report(rows):
    by_name=[]
    for row in rows:
        grouped={}
        for item in row[IDENTITY_CLASSES]:
            values=grouped.setdefault(item['runtime_class'],[0,0])
            values[0]+=item['all_threads']; values[1]+=item['caller_thread']
        by_name.append(grouped)
    names=sorted(set().union(*(x.keys() for x in by_name)))
    return {
        IDENTITY_ALL:stats([r[IDENTITY_ALL] for r in rows]),
        IDENTITY_CALLER:stats([r[IDENTITY_CALLER] for r in rows]),
        'runtime_binary_name_rollup':{
            name:{'all_threads':stats([r.get(name,[0,0])[0] for r in by_name]),
                  'caller_thread':stats([r.get(name,[0,0])[1] for r in by_name])} for name in names},
        'runtime_binary_name_rollup_unit':'sums distinct defining-loader classes sharing a binary name; raw runtime-class/loader records preserved below',
        'runtime_class_windows':[{'fork':r.get('fork'),'iteration':r['iteration'],
                                  'start_monotonic_ns':r['identity_window_start_monotonic_ns'],
                                  'end_monotonic_ns':r['identity_window_end_monotonic_ns'],
                                  'classes':r[IDENTITY_CLASSES]} for r in rows],
    }

def summarize(root):
    root=pathlib.Path(root).resolve()
    run=json.loads((root/'run.json').read_text()); cfg=run['config']
    phases=PHASES if cfg['scenario']=='vm' else ['cold_session_get_or_create','mandatory_startup_observation_no_vm']
    if not run.get('complete'): raise ValueError('Run did not complete: '+str(root))
    observations=[]; coverage=[]; jvms=[]
    for i in range(run['requested_forks']):
        fork=root/('fork-%02d'%i)
        if json.loads((fork/'exit.json').read_text())['returncode'] != 0: raise ValueError('Failed fork: '+str(fork))
        tests=[]
        for f in (fork/'junit-xml').glob('*.xml'):
            tests += [t for t in ET.parse(f).getroot().iter('testcase') if t.get('classname') == 'com.wire.android.di.metro.WireKaliumMeasurementTest']
        if len(tests)!=1 or any(t.find(x) is not None for t in tests for x in ['failure','error','skipped']):
            raise ValueError('Expected exactly one executed, passing measurement JUnit testcase: '+str(fork))
        files=list(fork.glob('metrics-*.jsonl'))
        if len(files)!=1: raise ValueError('Expected one measurement test JVM: '+str(fork))
        rows=[json.loads(s) for s in files[0].read_text().splitlines()]
        if rows[-1].get('type')!='jvm': raise ValueError('Missing final provenance record')
        meta=rows[-1]; jvms.append(meta)
        if meta.get('metric_schema')!=2 or not meta['bootstrap_probe'] or meta['pass']!=cfg['pass'] or meta['transform_errors'] or meta.get('counter_errors') or meta.get('retained_identity_references')!=0:
            raise ValueError('Probe/transform failure: '+str(fork))
        coverage.append(meta['coverage'])
        if cfg['pass']=='constructors' and not any(x.startswith('com.wire.kalium.logic.feature.UserSessionScope#<init>') for x in meta['coverage']):
            raise ValueError('No instrumented UserSessionScope constructor: '+str(fork))
        samples=[x for x in rows if x['type']=='sample']
        for row in samples: validate_sample_units(row)
        expected_count=(cfg['warmups']+cfg['repetitions'])*(cfg['warmVms']+4 if cfg['scenario']=='vm' else 2)
        if len(samples)!=expected_count: raise ValueError('Unexpected total sample count: '+str(fork))
        if any(not x['success'] or x['pass']!=cfg['pass'] or x['transform_errors'] for x in samples): raise ValueError('Failed sample')
        for iteration in range(cfg['warmups']+cfg['repetitions']):
            cycle=[x for x in samples if x['iteration']==iteration]
            expected={p:(cfg['warmVms'] if p=='warm_vm_new_store' else 1) for p in phases}
            if dict(collections.Counter(x['phase'] for x in cycle)) != expected: raise ValueError('Missing/extra phases in '+str(fork))
            if any(x['warmup']!=(iteration<cfg['warmups']) for x in cycle): raise ValueError('Wrong warmup flags')
        if cfg['pass']=='constructors':
            for row in samples:
                if row['phase']=='cold_session_get_or_create' and not any(
                    key.startswith('com.wire.kalium.logic.feature.UserSessionScope#<init>') and count > 0
                    for key,count in row['constructor_entries_caller_thread'].items()):
                    raise ValueError('Cold session span did not enter UserSessionScope constructor; setup may have opened it already')
        for row in samples:
            if not row['warmup']: observations.append(row|{'fork':i})
    summary={'schema':2,'units':UNITS,'run':str(root),'config':cfg,'provenance':str(root/'provenance.json'),'jvms':jvms,
             'aggregation':'timing: raw observations, medians per session cycle, then per fork; counts: separate ENTRY-call and RETURN-identity distributions; warm VMs share session/JVM and are not independent',
             'phases':{},'constructor_coverage_by_fork':coverage}
    text=['Run: '+str(root), 'Pass: '+cfg['pass']+'; variant: '+cfg['variant'],
          'Warmups excluded. ThreadMXBean bytes: approximation for caller thread only.',
          'ENTRY counts are calls; RETURN counts are unique selected-callback identities per window. Neither is a complete JVM allocation/object total.',
          'RETURN identities strongly retain selected objects until window close; instrumented timing/bytes are excluded.',
          'Session creation starts mandatory work. First/warm VM and fixed observation windows can overlap that work.']
    for phase in phases:
        rows=[x for x in observations if x['phase']==phase]; report={}
        if cfg['pass']=='timing':
            for field in ['elapsed_ns','caller_allocated_bytes']:
                values=[x[field] for x in rows if x[field] is not None]
                cycles=collections.defaultdict(list)
                for row in rows:
                    if row[field] is not None: cycles[(row['fork'],row['iteration'])].append(row[field])
                cycle_medians={str(k):statistics.median(v) for k,v in cycles.items()}
                fork_values=collections.defaultdict(list)
                for (fork,iteration),v in cycles.items(): fork_values[fork].append(statistics.median(v))
                report[field]={'raw':stats(values),'session_cycle_medians':cycle_medians,
                               'fork_median_of_cycle_medians':{str(k):statistics.median(v) for k,v in fork_values.items()}}
                text.append(phase+' '+field+': '+json.dumps(report[field]['raw']))
        else:
            for field in ['constructor_entries_caller_thread','constructor_entries_all_threads']:
                # Sum descriptors per binary class; raw descriptor maps remain in metrics JSONL.
                by_class=[]
                for row in rows:
                    grouped=collections.Counter()
                    for key,value in row[field].items(): grouped[key.split('#')[0]] += value
                    by_class.append(grouped)
                classes=sorted(set().union(*(x.keys() for x in by_class)))
                report[field]={c:stats([x.get(c,0) for x in by_class]) for c in classes}
                text.append(phase+' '+field+': '+json.dumps(report[field],sort_keys=True))
            report['selected_constructor_return_identities']=identity_report(rows)
            text.append(phase+' '+IDENTITY_ALL+': '+json.dumps(report['selected_constructor_return_identities'][IDENTITY_ALL]))
            text.append(phase+' '+IDENTITY_CALLER+': '+json.dumps(report['selected_constructor_return_identities'][IDENTITY_CALLER]))
        summary['phases'][phase]=report
    (root/'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
    (root/'summary.txt').write_text('\n'.join(text)+'\n')
    print(root/'summary.json')

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__); p.add_argument('runs',nargs='+'); a=p.parse_args()
    for root in a.runs: summarize(root)
