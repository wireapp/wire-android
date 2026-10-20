"""Source/runtime paths shared by ignored staging and proposed root tools installation."""
import os
from pathlib import Path
SOURCE = Path(__file__).resolve().parent

def repository_root():
    configured = os.environ.get('KALIUM_MEASURE_ROOT')
    if configured:
        return Path(configured).resolve()
    for candidate in SOURCE.parents:
        if (candidate/'app/build.gradle.kts').is_file() and (candidate/'kalium/settings.gradle.kts').is_file():
            return candidate
    raise SystemExit('Set KALIUM_MEASURE_ROOT to the explicitly owned Android repository')

ROOT = repository_root()
# Generated outputs remain ignored/below module-discovery depth after parent copies sources to tools/.
OUTPUT = Path(os.environ.get('KALIUM_MEASURE_OUTPUT', ROOT/'kalium/build/metro-verification/measurements/20261020/generated-v2')).resolve()
if OUTPUT == SOURCE or OUTPUT in [ROOT, ROOT/'kalium']:
    raise SystemExit('Use a dedicated ignored KALIUM_MEASURE_OUTPUT directory')
