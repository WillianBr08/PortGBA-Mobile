#!/usr/bin/env python3
"""Obtain the pinned upstream core and preserve the project's explicit I/O patch."""
from pathlib import Path
import subprocess

ROOT = Path(__file__).resolve().parents[1]
REVISION = '26b7884bc25a5933960f3cdcd98bac1ae14d42e2'
SOURCE = ROOT / 'build/mgba'
PATCH = ROOT / 'vendor/patches/mgba-state-io.patch'


def git(*args, check=True):
    return subprocess.run(['git', '-C', str(SOURCE), *args], check=check, capture_output=True, text=True)


def prepare():
    SOURCE.parent.mkdir(exist_ok=True)
    if not (SOURCE / '.git').exists():
        SOURCE.mkdir(exist_ok=True)
        git('init', '-q')
        git('remote', 'add', 'origin', 'https://github.com/mgba-emu/mgba.git')
        git('fetch', '--depth=1', 'origin', REVISION)
        git('checkout', '--detach', '-q', 'FETCH_HEAD')
    if git('rev-parse', 'HEAD').stdout.strip() != REVISION:
        raise SystemExit('Núcleo local em revisão inesperada. Guarde build/mgba e prepare novamente.')
    if git('apply', '--reverse', '--check', str(PATCH), check=False).returncode != 0:
        git('apply', '--check', str(PATCH))
        git('apply', str(PATCH))
    print('mGBA fixado e patch de I/O verificados.')


if __name__ == '__main__':
    prepare()
