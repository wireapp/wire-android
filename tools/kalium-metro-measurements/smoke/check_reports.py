#!/usr/bin/env python3
"""Use REAL standalone Java smoke rows to check unit/failure reporting. No synthetic SDK results."""
import copy, importlib.util, json, pathlib, sys
HERE=pathlib.Path(__file__).resolve().parent.parent
sys.path.insert(0,str(HERE))
from repo_paths import OUTPUT
spec=importlib.util.spec_from_file_location('measurement_summary',HERE/'summarize.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
def rows(case):
    return [json.loads(s) for p in (OUTPUT/'smoke'/case).glob('metrics-*.jsonl') for s in p.read_text().splitlines()]
def rejects(row):
    try: m.validate_sample_units(row)
    except ValueError: return
    raise AssertionError('invalid metric row was accepted')
for mode in ['timing','constructors']:
    samples=[r for r in rows(mode) if r['type']=='sample']
    assert len(samples)==4
    for row in samples: m.validate_sample_units(row)
first=next(r for r in rows('constructors') if r.get('phase')=='standalone_identity_semantics')
report=m.identity_report([first])
assert report[m.IDENTITY_ALL]['median']==8 and report[m.IDENTITY_CALLER]['median']==7
assert len([c for c in report['runtime_class_windows'][0]['classes'] if c['runtime_class']=='smoke.Smoke$Subject'])==2
assert report['runtime_binary_name_rollup']['smoke.Smoke$Subject']['all_threads']['median']==4
bad=copy.deepcopy(first);bad[m.IDENTITY_ALL]+=1;rejects(bad)
bad=copy.deepcopy(first);bad['retained_identity_references_after_end']=1;rejects(bad)
bad=copy.deepcopy(first);bad['caller_allocated_bytes']=32;rejects(bad)
bad=copy.deepcopy(first);bad['metric_schema']=1;rejects(bad)
bad=copy.deepcopy(first);bad[m.IDENTITY_CLASSES].append(copy.deepcopy(bad[m.IDENTITY_CLASSES][0]));rejects(bad)
for row in rows('limit'):
    if row['type']=='sample': rejects(row)
for mode in ['timing','constructors']:
    control=next(r for r in rows(mode+'-control') if r['type']=='callback_control')
    m.validate_callback_control(control)
    for field in ['success','constructor_entries_all_threads','constructor_return_unique_identities_all_threads']:
        bad=copy.deepcopy(control)
        if field=='success': bad[field]=False
        elif mode=='constructors':
            if isinstance(bad[field],dict): bad[field]={k:0 for k in bad[field]}
            else: bad[field]=0
        else: continue
        try: m.validate_callback_control(bad)
        except ValueError: pass
        else: raise AssertionError('failed callback control was accepted: '+field)
print('Report checks passed using standalone Java data: units, loader distinction, totals, failure and legacy-schema rejection. No SDK results created.')
