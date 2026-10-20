#!/usr/bin/env python3
"""Standalone source-drift checks on a tiny synthetic tree. NEVER launches Gradle,
archives a repository, or injects a fixture into an SDK/Android checkout.
"""
import json
import os
from pathlib import Path
import stat
import sys
import tempfile
from types import SimpleNamespace
import unittest
from unittest.mock import patch

HERE = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(HERE))
import archive_manifest
import harness
from repo_paths import OUTPUT

EXAMPLE_MANIFEST = None


class ArchiveChecks(unittest.TestCase):
    def setUp(self):
        global EXAMPLE_MANIFEST
        self.temp = tempfile.TemporaryDirectory(prefix='archive-guard-', dir=ARTIFACTS)
        self.output = Path(self.temp.name)
        self.output_patch = patch.object(harness, 'OUTPUT', self.output)
        self.output_patch.start()
        self.addCleanup(self.temp.cleanup)
        self.addCleanup(self.output_patch.stop)
        self.root = self.output / 'archives/baseline/android'
        self.root.mkdir(parents=True)
        self.sources = {
            '.gitignore': 'build/\n.gradle/\n.kotlin/\n' + '\n'.join(archive_manifest.ANDROID_OUTPUT_FILES) + '\n',
            'app/src/main/kotlin/Consumer.kt': '// synthetic Android source\n',
            'app/build.gradle.kts': '// synthetic Android build\n',
            # A precompiled source script does not make its directory a project.
            'app/src/main/kotlin/plugin/build.gradle.kts': '// synthetic source script\n',
            'settings.gradle.kts': '// Android settings\n',
            'gradle.properties': 'kotlin.code.style=official\n',
            'gradle/libs.versions.toml': '[versions]\nkotlin="synthetic"\n',
            'gradle/wrapper/gradle-wrapper.properties': 'distributionUrl=synthetic\n',
            '.github/workflows/check.yml': 'name: synthetic\n',
            'kalium/.gitignore': 'build/\n.gradle/\n.kotlin/\n',
            'kalium/logic/src/commonMain/kotlin/Consumer.kt': '// synthetic SDK source\n',
            'kalium/logic/build.gradle.kts': '// synthetic SDK module build\n',
            'kalium/build.gradle.kts': '// synthetic SDK build\n',
            'kalium/settings.gradle.kts': '// SDK settings\n',
            'kalium/gradle.properties': 'kalium.providerCacheScope=GLOBAL\n',
            'kalium/gradle/libs.versions.toml': '[versions]\nkotlin="synthetic"\n',
            'kalium/.settings/.nested/compiler.properties': 'language=synthetic\n',
            # Existing source is never excluded, even when its directory name
            # normally admits newly generated output after staging.
            'tools/build/required.conf': 'staged source below build/\n',
            '.gradle/required.conf': 'staged source below .gradle/\n',
            'kalium/.kotlin/required.conf': 'staged source below .kotlin/\n',
            'gradlew': '#!/bin/sh\nexit 99 # must never be executed\n',
            'include_builds.gradle.kts': '// synthetic archive-only patched build\n',
            harness.DEST.as_posix(): '// standalone guard placeholder; no SDK fixture injection\n',
        }
        for name, text in self.sources.items():
            self.put(name, text)
        (self.root / 'gradlew').chmod(0o755)
        (self.root / 'kalium/source-link').symlink_to('logic/src/commonMain/kotlin/Consumer.kt')
        self.stage = {'variant': 'baseline', 'root': str(self.root),
                      'android_commit': harness.BASE_ANDROID, 'sdk_commit': harness.BASE_SDK,
                      'injected': str(self.root / harness.DEST), 'sha256': harness.sha(self.root / harness.DEST),
                      'archive_source_manifest': harness.record_baseline_source(self.root),
                      'archive_only_patch': {'path': str(self.root / 'include_builds.gradle.kts'),
                                             'patched_sha256': harness.sha(self.root / 'include_builds.gradle.kts')}}
        harness.write_new(harness.staged_receipt('baseline'), self.stage)
        EXAMPLE_MANIFEST = json.loads(harness.baseline_source_manifest().read_text())
        self.assertEqual(harness.read_stage('baseline'), self.stage)

    def put(self, name, text):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text)

    def test_all_source_content_drift_rejected(self):
        for name, text in self.sources.items():
            with self.subTest(path=name):
                self.put(name, text + '\nchanged\n')
                with self.assertRaises(SystemExit):
                    harness.read_stage('baseline')
                self.put(name, text)
                harness.read_stage('baseline')

    def test_source_additions_and_deletions_rejected(self):
        for name in ('app/src/main/kotlin/New.kt', 'kalium/.hidden/.nested/config', 'local.properties'):
            with self.subTest(added=name):
                # Create new parents separately so the entire addition can be removed.
                manifest = archive_manifest.capture(self.root, harness.BASE_ANDROID, harness.BASE_SDK)
                self.put(name, 'unexpected source/config\n')
                with self.assertRaises(SystemExit):
                    harness.read_stage('baseline')
                path = self.root / name
                path.unlink()
                while path.parent != self.root and path.parent.relative_to(self.root).as_posix() not in manifest['entries']:
                    path.parent.rmdir()
                    path = path.parent
                harness.read_stage('baseline')
        name = 'kalium/logic/src/commonMain/kotlin/Consumer.kt'
        (self.root / name).unlink()
        with self.assertRaisesRegex(SystemExit, 'source missing'):
            harness.read_stage('baseline')

    def test_file_modes_and_symlink_targets_rejected(self):
        executable = self.root / 'gradlew'
        executable.chmod(stat.S_IMODE(executable.stat().st_mode) ^ stat.S_IXUSR)
        with self.assertRaisesRegex(SystemExit, 'content/type/mode/link'):
            harness.read_stage('baseline')
        executable.chmod(0o755)
        link = self.root / 'kalium/source-link'
        link.unlink()
        link.symlink_to('gradle.properties')
        with self.assertRaisesRegex(SystemExit, 'content/type/mode/link'):
            harness.read_stage('baseline')

    def assert_reproducer_rejected(self, name, symlink=False):
        # The original receipt + manifest are already on disk. Preserve their
        # hashes to prove rejection does not depend on re-recording provenance.
        paths = (harness.staged_receipt('baseline'), harness.baseline_source_manifest())
        before = [harness.sha(path) for path in paths]
        if symlink:
            external = self.output / 'external-kotlin'
            external.mkdir()
            (external / 'Unlisted.kt').write_text('package independent.repro\nclass Unlisted\n')
            target = self.root / name
            target.parent.mkdir(parents=True, exist_ok=True)
            target.symlink_to(external, target_is_directory=True)
        else:
            self.put(name, 'package independent.repro\nclass Unlisted\n')
        with self.assertRaisesRegex(SystemExit, 'Unexpected baseline source entry|Unexpected baseline symlink'):
            harness.read_stage('baseline')
        self.assertEqual([harness.sha(path) for path in paths], before)

    def test_r2_android_source_build_directory_addition_rejected(self):
        self.assert_reproducer_rejected('app/src/main/kotlin/build/Unlisted.kt')

    def test_r2_sdk_source_build_directory_addition_rejected(self):
        self.assert_reproducer_rejected('kalium/logic/src/commonMain/kotlin/build/Unlisted.kt')

    def test_r2_nested_source_build_symlink_addition_rejected(self):
        self.assert_reproducer_rejected('app/src/main/kotlin/build/external-source', symlink=True)

    def test_existing_output_named_source_directories_are_walked(self):
        # Unlike the exact R2 reproducers above, these directories predate the
        # manifest, so an addition must be detected INSIDE each recorded parent.
        for prefix in ('app/src/main/kotlin', 'kalium/logic/src/commonMain/kotlin'):
            for directory in archive_manifest.OUTPUT_DIRS:
                with self.subTest(prefix=prefix, directory=directory):
                    parent = prefix + '/' + directory
                    self.put(parent + '/Required.kt', '// required source\n')
                    manifest = archive_manifest.capture(self.root, harness.BASE_ANDROID, harness.BASE_SDK)
                    self.put(parent + '/Unlisted.kt', '// unlisted source\n')
                    with self.assertRaisesRegex(archive_manifest.SourceDrift, 'Unexpected baseline source entry.*Unlisted.kt'):
                        archive_manifest.verify(self.root, manifest)
                    (self.root / parent / 'Unlisted.kt').unlink()
                    (self.root / parent / 'external-source').symlink_to('../Consumer.kt')
                    with self.assertRaisesRegex(archive_manifest.SourceDrift, 'Unexpected baseline symlink'):
                        archive_manifest.verify(self.root, manifest)
                    (self.root / parent / 'external-source').unlink()
                    archive_manifest.verify(self.root, manifest)
                    (self.root / parent / 'Required.kt').unlink()
                    (self.root / parent).rmdir()
        harness.read_stage('baseline')

    def test_output_paths_require_recorded_project_evidence_outside_source(self):
        manifest = json.loads(harness.baseline_source_manifest().read_text())
        android, sdk = manifest['added_output_policy']
        self.assertEqual(android['project_roots'], ['', 'app'])
        self.assertEqual(sdk['project_roots'], ['', 'logic'])
        self.assertIn('app/src', android['source_roots'])
        self.assertIn('logic/src', sdk['source_roots'])
        for name in ('tools/build/Unlisted.kt', 'tools/.gradle/cache.bin',
                     'app/src/main/kotlin/plugin/build/Unlisted.kt', 'kalium/unknown/build/Fake.class'):
            with self.subTest(path=name):
                self.put(name, 'not at a recorded project output root\n')
                with self.assertRaisesRegex(SystemExit, 'Unexpected baseline source entry'):
                    harness.read_stage('baseline')
                path = self.root / name
                path.unlink()
                while path.parent != self.root and path.parent.relative_to(self.root).as_posix() not in manifest['entries']:
                    path.parent.rmdir()
                    path = path.parent
                harness.read_stage('baseline')

    def protobuf_manifest(self, configured=True, ignored=True, required=False):
        text = ('val generatedFilesBaseDir = file("generated")\n'
                '        kotlin.srcDir(generatedFilesBaseDir)\n'
                '        val targetDirectory = File(generatedFilesBaseDir, generatedDirectory.name)\n')
        self.put('kalium/data/protobuf/build.gradle.kts', text if configured else '// no configured output\n')
        if ignored:
            self.put('kalium/.gitignore', self.sources['kalium/.gitignore'] + 'generated/\n')
        if required:
            self.put('kalium/data/protobuf/generated/Required.kt', '// required staged source\n')
        return archive_manifest.capture(self.root, harness.BASE_ANDROID, harness.BASE_SDK)

    def test_configured_protobuf_output_allowed_with_recorded_evidence(self):
        manifest = self.protobuf_manifest()
        self.put('kalium/data/protobuf/generated/pbandk/Messages.kt', '// generated Kotlin\n')
        archive_manifest.verify(self.root, manifest)
        configured = manifest['added_output_policy'][1]['configured_outputs']
        self.assertEqual(configured[0]['directory'], 'data/protobuf/generated')
        self.assertEqual(configured[0]['build_script_sha256'],
                         manifest['entries']['kalium/data/protobuf/build.gradle.kts']['sha256'])

    def test_protobuf_output_requires_both_configuration_and_ignore_evidence(self):
        for configured, ignored in [(False, True), (True, False)]:
            with self.subTest(configured=configured, ignored=ignored):
                self.put('kalium/.gitignore', self.sources['kalium/.gitignore'])
                manifest = self.protobuf_manifest(configured=configured, ignored=ignored)
                self.put('kalium/data/protobuf/generated/New.kt', '// unapproved output\n')
                with self.assertRaisesRegex(archive_manifest.SourceDrift, 'Unexpected baseline source entry'):
                    archive_manifest.verify(self.root, manifest)
                (self.root / 'kalium/data/protobuf/generated/New.kt').unlink()
                (self.root / 'kalium/data/protobuf/generated').rmdir()

    def test_protobuf_exception_does_not_hide_other_generated_source_paths(self):
        manifest = self.protobuf_manifest()
        for name in ['kalium/logic/generated/New.kt', 'kalium/data/protobuf/src/main/kotlin/generated/New.kt']:
            with self.subTest(path=name):
                self.put(name, '// unlisted source\n')
                with self.assertRaisesRegex(archive_manifest.SourceDrift, 'Unexpected baseline source entry'):
                    archive_manifest.verify(self.root, manifest)
                path = self.root / name
                path.unlink()
                while path.parent.relative_to(self.root).as_posix() not in manifest['entries']:
                    path.parent.rmdir()
                    path = path.parent

    def test_required_sources_and_new_links_inside_protobuf_output_still_checked(self):
        manifest = self.protobuf_manifest(required=True)
        required = 'kalium/data/protobuf/generated/Required.kt'
        self.put(required, '// changed required source\n')
        with self.assertRaisesRegex(archive_manifest.SourceDrift, 'source changed'):
            archive_manifest.verify(self.root, manifest)
        self.put(required, '// required staged source\n')
        link = self.root / 'kalium/data/protobuf/generated/nested/external'
        link.parent.mkdir()
        link.symlink_to(self.output, target_is_directory=True)
        with self.assertRaisesRegex(archive_manifest.SourceDrift, 'Unexpected baseline symlink'):
            archive_manifest.verify(self.root, manifest)

    def test_parent_symlink_substitution_rejected(self):
        source_dir = self.root / 'kalium/logic'
        moved = self.output / 'moved-logic'
        source_dir.rename(moved)
        source_dir.symlink_to(moved, target_is_directory=True)
        with self.assertRaisesRegex(SystemExit, 'source changed.*kalium/logic'):
            harness.read_stage('baseline')

    def add_build_outputs(self):
        for name in ('build/cache/output.bin', 'app/build/classes/Fake.class',
                     'app/build/generated/ksp/kotlin/Fake.kt', 'app/.kotlin/cache/state.bin',
                     '.gradle/9.4/executionHistory.bin', 'kalium/.gradle/cache/state.bin',
                     'kalium/logic/build/generated/Fake.kt', 'kalium/.kotlin/cache/state.bin',
                     'app/src/main/assets/version.txt', 'app/src/main/assets/dependencies_version.json',
                     'app/version.txt'):
            self.put(name, 'standalone generated output, no project build\n')

    def test_generated_outputs_allowed_but_existing_sources_below_them_checked(self):
        self.add_build_outputs()
        harness.read_stage('baseline')
        for name in ('tools/build/required.conf', '.gradle/required.conf', 'kalium/.kotlin/required.conf'):
            self.put(name, 'changed source within an output-named directory\n')
            with self.assertRaisesRegex(SystemExit, 'source changed'):
                harness.read_stage('baseline')
            self.put(name, self.sources[name])
        harness.read_stage('baseline')

    def test_old_receipts_and_modified_manifest_rejected(self):
        receipt = harness.staged_receipt('baseline')
        old = dict(self.stage)
        old.pop('archive_source_manifest')
        receipt.write_text(json.dumps(old))
        with self.assertRaisesRegex(SystemExit, 'old archives/receipts cannot be reused'):
            harness.read_stage('baseline')
        receipt.write_text(json.dumps(self.stage))
        path = harness.baseline_source_manifest()
        path.write_text(path.read_text() + '\n')
        with self.assertRaisesRegex(SystemExit, 'manifest missing/changed'):
            harness.read_stage('baseline')

    def test_r2_manifest_schema_rejected(self):
        manifest = json.loads(harness.baseline_source_manifest().read_text())
        manifest['schema'] = 1
        with self.assertRaisesRegex(archive_manifest.SourceDrift, 'unsupported archive source manifest'):
            archive_manifest.verify(self.root, manifest)

    def test_allowed_asset_parent_does_not_hide_new_source(self):
        self.add_build_outputs()
        self.put('app/src/main/assets/unexpected-config.json', '{}\n')
        with self.assertRaisesRegex(SystemExit, 'Unexpected baseline source entry'):
            harness.read_stage('baseline')

    def test_output_exceptions_respect_entry_type(self):
        self.put('app/build', 'regular file, not an ignored output directory\n')
        with self.assertRaisesRegex(SystemExit, 'Unexpected baseline source entry'):
            harness.read_stage('baseline')
        (self.root / 'app/build').unlink()
        asset = self.root / 'app/version.txt'
        asset.mkdir()
        with self.assertRaisesRegex(SystemExit, 'Unexpected baseline source entry'):
            harness.read_stage('baseline')
        asset.rmdir()
        (self.root / 'app/build').symlink_to('src', target_is_directory=True)
        with self.assertRaises(SystemExit):
            harness.read_stage('baseline')

    def test_nested_generated_output_symlinks_rejected(self):
        self.add_build_outputs()
        harness.read_stage('baseline')
        for name in ('app/build/generated/external-source', 'kalium/.gradle/cache/external-source'):
            with self.subTest(path=name):
                link = self.root / name
                link.symlink_to(self.root / 'app/src', target_is_directory=True)
                with self.assertRaisesRegex(SystemExit, 'Unexpected baseline symlink'):
                    harness.read_stage('baseline')
                link.unlink()
                harness.read_stage('baseline')

    def prepare_run(self, label):
        tools = self.output / 'tools'
        tools.mkdir()
        for name in ('probe.jar', 'constructors-agent.jar'):
            (tools / name).write_text('standalone guard placeholder, NOT a usable JAR\n')
        harness.write_new(tools / 'build-provenance.json', {
            'sources': {}, 'jars': {name: harness.sha(tools / name) for name in ('probe.jar', 'constructors-agent.jar')}})
        return SimpleNamespace(variant='baseline', identity_limit=10, forks=2, warmups=0,
                               repetitions=1, warm_vms=1, observation_ms=0, label=label,
                               pass_name='timing', scenario='vm', gradle_args=[])

    def fake_gradle(self, *args, **kwargs):
        self.add_build_outputs()
        return 0

    def test_run_rechecks_every_fork_and_accepts_only_output_additions(self):
        args = self.prepare_run('allowed-output-test')
        with patch.dict(os.environ, {'ANDROID_HOME': '/standalone-test-no-sdk'}), \
                patch.object(harness, 'read_stage', wraps=harness.read_stage) as reads, \
                patch.object(harness, 'execute_gradle', side_effect=self.fake_gradle) as launches:
            harness.run(args)
        self.assertEqual(launches.call_count, 2)  # Mock calls; no Gradle process.
        self.assertEqual(reads.call_count, 5)  # Preflight, before/after EACH fork.
        result = self.output / 'results' / args.label
        self.assertTrue(json.loads((result / 'run.json').read_text())['complete'])
        self.assertEqual(harness.sha(result / 'baseline-source-manifest.json'),
                         self.stage['archive_source_manifest']['sha256'])

    def check_boundary_drift(self, after_read, expected_launches):
        args = self.prepare_run('drift-boundary-test')
        real_read = harness.read_stage
        calls = 0

        def read_then_drift(variant):
            nonlocal calls
            value = real_read(variant)
            calls += 1
            if calls == after_read:
                self.put('kalium/logic/src/commonMain/kotlin/Consumer.kt', '// changed between guard boundaries\n')
            return value

        with patch.dict(os.environ, {'ANDROID_HOME': '/standalone-test-no-sdk'}), \
                patch.object(harness, 'read_stage', side_effect=read_then_drift), \
                patch.object(harness, 'execute_gradle', side_effect=self.fake_gradle) as launches:
            with self.assertRaisesRegex(SystemExit, 'source changed'):
                harness.run(args)
        self.assertEqual(launches.call_count, expected_launches)
        self.assertFalse(json.loads((self.output / 'results' / args.label / 'run.json').read_text())['complete'])

    def test_drift_after_preflight_rejected_before_first_fork(self):
        self.check_boundary_drift(after_read=1, expected_launches=0)

    def test_drift_after_first_fork_rejected_before_second_fork(self):
        self.check_boundary_drift(after_read=3, expected_launches=1)

    def test_drift_during_final_fork_invalidates_run(self):
        args = self.prepare_run('final-fork-drift-test')
        args.forks = 1

        def mutate_during_fake_gradle(*args, **kwargs):
            self.put('.github/workflows/check.yml', 'changed: during final mock fork\n')
            return 0

        with patch.dict(os.environ, {'ANDROID_HOME': '/standalone-test-no-sdk'}), \
                patch.object(harness, 'execute_gradle', side_effect=mutate_during_fake_gradle):
            with self.assertRaisesRegex(SystemExit, 'source changed'):
                harness.run(args)
        self.assertFalse(json.loads((self.output / 'results' / args.label / 'run.json').read_text())['complete'])


if __name__ == '__main__':
    ARTIFACTS = OUTPUT / 'smoke'
    ARTIFACTS.mkdir(parents=True, exist_ok=True)
    suite = unittest.defaultTestLoader.loadTestsFromTestCase(ArchiveChecks)
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    example = ARTIFACTS / 'archive-fixture-source-manifest.json'
    example.write_text(json.dumps(EXAMPLE_MANIFEST, indent=2) + '\n')
    report = ARTIFACTS / 'archive-checks.json'
    report.write_text(json.dumps({'kind': 'standalone source archive guard checks; synthetic input tree; no SDK results',
                                 'tests_run': result.testsRun, 'failures': len(result.failures), 'errors': len(result.errors),
                                 'example_manifest': str(example), 'example_manifest_sha256': archive_manifest.digest(example),
                                 'source_entries': len(EXAMPLE_MANIFEST['entries']),
                                 'gradle_processes_launched': 0}, indent=2) + '\n')
    if not result.wasSuccessful():
        raise SystemExit(1)
    print('Standalone archive guard checks passed. Actual manifest/report: ' + str(report))
