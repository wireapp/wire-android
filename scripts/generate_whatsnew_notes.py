import os, re, pathlib

WORKSPACE = os.environ.get('GITHUB_WORKSPACE') or str(pathlib.Path.cwd().parent)
OUTDIR = os.path.join(WORKSPACE, 'build/whatsnew')
SEMVER_PATTERN = re.compile(r'^\d+\.\d+\.\d+\.txt$')

def is_semver_file(filename):
    return bool(SEMVER_PATTERN.match(filename))

def version_key(name: str):
    """Convert '1.12.5.txt' → (1, 12, 5) for correct sorting"""
    parts = re.findall(r'\d+', name)
    return tuple(int(p) for p in parts) if parts else (0,)

def list_files_recursively(path='../app/src/main/play/release-notes'):
    latest_releases = []

    for root, dirs, files in os.walk(path):
        semver_files = [f for f in files if is_semver_file(f)]

        if not semver_files:
            continue
            
        latest_file = max(semver_files, key=version_key)
        full_path = os.path.join(root, latest_file)

        latest_releases.append(full_path)
    
    return latest_releases

def main():
    for f in list_files_recursively():
        original_path = pathlib.Path(f)
        dest_dir = pathlib.Path(OUTDIR)
        dest_dir.mkdir(parents=True, exist_ok=True)
        destination = pathlib.Path(dest_dir / f'whatsnew-{original_path.parent.name}')
        destination.write_bytes(original_path.read_bytes())

if __name__ == "__main__":
    main()
