"""Snapshot/check protected test-instance files while replacing only this addon."""
import argparse
import hashlib
import json
from pathlib import Path


def snapshot(instance):
    root = instance.resolve(strict=True)
    files = []
    for folder in ('mods', 'config', 'defaultconfigs', 'saves', 'PCL'):
        directory = root / folder
        if directory.exists():
            files.extend(path for path in directory.rglob('*') if path.is_file())
    files.extend(path for path in root.iterdir() if path.is_file())
    return {str(path.relative_to(root)): hashlib.sha256(path.read_bytes()).hexdigest()
            for path in sorted(set(files))
            if not (path.parent.name == 'mods' and
                    (path.name.startswith('lightning-tech-simulation-') or path.name.startswith('overload_sim-')))}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('instance', type=Path)
    parser.add_argument('baseline', type=Path)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    current = snapshot(args.instance)
    if args.check:
        previous = json.loads(args.baseline.read_text(encoding='utf-8'))
        changed = {name for name in previous.keys() | current.keys() if previous.get(name) != current.get(name)}
        if changed:
            raise SystemExit('Protected files changed: ' + ', '.join(sorted(changed)))
        print(f'PASS: all {len(current)} protected instance files unchanged.')
    else:
        args.baseline.parent.mkdir(parents=True, exist_ok=True)
        args.baseline.write_text(json.dumps(current, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
        print(f'Snapshotted {len(current)} protected instance files.')
