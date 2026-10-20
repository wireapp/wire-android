"""Complete staged archive inventory; no Git metadata, archive creation or project builds.

Inventory EVERY entry before the first build, including hidden files and files in
ignored directories. Subsequently added output is permitted only at exact standard
output paths of projects evidenced by the recorded source layout and root ignore
rules. Required source entries are always checked first, including inside outputs.
"""
import hashlib
import os
from pathlib import Path, PurePosixPath
import re
import stat


SCHEMA = 3
# Deliberately narrower than .gitignore: local.properties, IDE state, arbitrary
# *.log/*.class, etc. are not allowed source-tree mutations. Fail closed on new
# output shapes until reviewed. These names apply ONLY directly below recorded
# project roots, never to arbitrary same-named directories in the source tree.
OUTPUT_DIRS = ('build', '.gradle', '.kotlin')
PROJECT_SCRIPTS = ('build.gradle', 'build.gradle.kts', 'settings.gradle', 'settings.gradle.kts')
# Android's build writes these ignored assets outside build/. Exact paths only.
ANDROID_OUTPUT_FILES = ('app/src/main/assets/version.txt',
                        'app/src/main/assets/dependencies_version.json', 'app/version.txt')


class SourceDrift(ValueError):
    pass


def digest(path):
    value = hashlib.sha256()
    with path.open('rb') as source:
        for block in iter(lambda: source.read(1024 * 1024), b''):
            value.update(block)
    return value.hexdigest()


def entry(path):
    info = path.lstat()  # Never dereference a symlink to hash its target as source.
    mode = stat.S_IMODE(info.st_mode)
    if stat.S_ISREG(info.st_mode):
        return {'kind': 'file', 'mode': mode, 'sha256': digest(path)}
    if stat.S_ISDIR(info.st_mode):
        return {'kind': 'directory', 'mode': mode}
    if stat.S_ISLNK(info.st_mode):
        return {'kind': 'symlink', 'mode': mode, 'target': os.readlink(path)}
    raise SourceDrift('Unsupported archive entry: ' + str(path))


def walk(root):
    for path in sorted(root.iterdir()):
        value = entry(path)
        yield path, value
        if value['kind'] == 'directory':
            yield from walk(path)


def output_policy(root, entries):
    policy = []
    for prefix in ('', 'kalium'):
        # Derive project locations from the pre-build inventory, not from files
        # discovered during reuse. Precompiled scripts under src/ are source,
        # not additional projects. No general .gitignore basename matching.
        local_entries = {(name[len(prefix) + 1:] if prefix else name): value
                         for name, value in entries.items()
                         if (name.startswith('kalium/') if prefix else
                             name != 'kalium' and not name.startswith('kalium/'))}
        source_roots = sorted(name for name, value in local_entries.items()
                              if value['kind'] == 'directory' and PurePosixPath(name).name == 'src')
        projects = sorted({PurePosixPath(name).parent.as_posix()
                           for name, value in local_entries.items()
                           if value['kind'] == 'file' and PurePosixPath(name).name in PROJECT_SCRIPTS
                           and 'src' not in PurePosixPath(name).parts[:-1]})
        projects = ['' if name == '.' else name for name in projects]
        ignore = root / prefix / '.gitignore'
        lines = set(ignore.read_text().splitlines()) if ignore.is_file() else set()
        directories = sorted((PurePosixPath(project) / name).as_posix()
                             for project in projects for name in OUTPUT_DIRS if name + '/' in lines)
        configured = []
        # Kalium's committed protobuf project deliberately generates Kotlin outside build/.
        # This is one exact, evidenced output; generated/ is never a basename exclusion.
        script = 'data/protobuf/build.gradle.kts'
        if (prefix == 'kalium' and 'data/protobuf' in projects and 'generated/' in lines
                and local_entries.get(script, {}).get('kind') == 'file'
                and local_entries.get('.gitignore', {}).get('kind') == 'file'):
            text = (root / prefix / script).read_text()
            markers = [r'^val generatedFilesBaseDir = file\("generated"\)$',
                       r'^\s*kotlin\.srcDir\(generatedFilesBaseDir\)\s*$',
                       r'^\s*val targetDirectory = File\(generatedFilesBaseDir, generatedDirectory\.name\)\s*$']
            if all(re.search(marker, text, re.MULTILINE) for marker in markers):
                directories = sorted(directories + ['data/protobuf/generated'])
                configured.append({'directory': 'data/protobuf/generated', 'build_script': script,
                                   'build_script_sha256': local_entries[script]['sha256'],
                                   'ignore_file': '.gitignore', 'ignore_rule': 'generated/',
                                   'ignore_sha256': local_entries['.gitignore']['sha256']})
        files = [name for name in ANDROID_OUTPUT_FILES if not prefix and name in lines]
        policy.append({'root': prefix, 'project_roots': projects, 'source_roots': source_roots,
                       'directories': directories, 'files': files, 'configured_outputs': configured})
    return policy


def capture(root, android_commit, sdk_commit):
    root = Path(root)
    if root.is_symlink() or not root.is_dir():
        raise SourceDrift('Expected a real archive directory')
    entries = {path.relative_to(root).as_posix(): value for path, value in walk(root)}
    return {'schema': SCHEMA, 'android_commit': android_commit, 'sdk_commit': sdk_commit,
            'added_output_policy': output_policy(root, entries), 'entries': entries}


def valid_path(name):
    path = PurePosixPath(name)
    return bool(name) and not path.is_absolute() and path.as_posix() == name and '..' not in path.parts


def allowed_output(name, policy, is_directory):
    # Each repository uses its own root rules; Android's policy must not mask an
    # SDK path whose own .gitignore does not permit that output shape.
    rules = policy[1] if name.startswith('kalium/') else policy[0]
    local = name[len(rules['root']) + 1:] if rules['root'] else name
    return ((not is_directory and local in rules['files'])
            or any(local.startswith(directory + '/') or (is_directory and local == directory)
                   for directory in rules['directories']))


def output_parent(name, policy):
    rules = policy[1] if name.startswith('kalium/') else policy[0]
    local = name[len(rules['root']) + 1:] if rules['root'] else name
    return any(target.startswith(local + '/') for target in rules['files'])


def verify(root, manifest):
    root = Path(root)
    if root.is_symlink() or not root.is_dir() or manifest.get('schema') != SCHEMA:
        raise SourceDrift('Missing/unsupported archive source manifest or root')
    expected = manifest.get('entries')
    if not isinstance(expected, dict) or not expected:
        raise SourceDrift('Empty/missing archive source inventory')
    # Parent directories precede descendants; a replaced parent symlink is
    # rejected before any child is opened. Tracked files under build/ still count.
    for name in sorted(expected, key=lambda name: (len(PurePosixPath(name).parts), name)):
        if not valid_path(name):
            raise SourceDrift('Invalid manifest source path: ' + name)
        parent = PurePosixPath(name).parent.as_posix()
        if parent != '.' and expected.get(parent, {}).get('kind') != 'directory':
            raise SourceDrift('Missing manifest parent directory: ' + name)
        try:
            actual = entry(root / name)
        except FileNotFoundError:
            raise SourceDrift('Baseline source missing: ' + name) from None
        if actual != expected[name]:
            raise SourceDrift('Baseline source changed (content/type/mode/link): ' + name)
    policy = manifest.get('added_output_policy')
    if policy != output_policy(root, expected):
        raise SourceDrift('Baseline generated-output policy changed')

    def check_additions(directory):
        for path in sorted(directory.iterdir()):
            name = path.relative_to(root).as_posix()
            # Walk directories even under permitted output roots. This also
            # checks nested additions/symlinks without hashing generated files.
            info = path.lstat()
            is_directory = stat.S_ISDIR(info.st_mode)
            if name not in expected:
                if stat.S_ISLNK(info.st_mode):
                    raise SourceDrift('Unexpected baseline symlink: ' + name)
                allowed = (is_directory or stat.S_ISREG(info.st_mode)) and allowed_output(name, policy, is_directory)
                # Only exact Android asset paths admit otherwise absent parents;
                # recursive inspection still rejects any unrelated siblings.
                if not allowed and not (is_directory and output_parent(name, policy)):
                    raise SourceDrift('Unexpected baseline source entry: ' + name)
            if is_directory:
                check_additions(path)
    check_additions(root)
    return {'source_entries_verified': len(expected),
            'source_files_verified': sum(value['kind'] == 'file' for value in expected.values())}
