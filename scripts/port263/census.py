#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Census helper for the MC 26.3 port.

Runs javac over the whole source set with the *real* NeoForge/MC compile
classpath (materialised by `gradlew dumpCompileClasspath` into
build/compile-cp.txt) and prints an aggregated error breakdown.

javac stops at 100 errors by default, which is useless for a port of this size,
so -Xmaxerrs is raised. Gradle's own output is truncated by the harness, which
is the other reason this runs javac directly: we need the complete list to
drive codemods instead of fixing errors 100 at a time.

Usage:  python scripts/port263/census.py [--out .ref263/census.txt]
"""
import collections
import io
import json
import os
import re
import subprocess
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..'))
JAVA_HOME = r'D:\MC\jdk\graalvm-25.2.4+7.1'
CENSUS_DIR = os.path.join(ROOT, '.ref263')
HEADER = re.compile(r'^(.*?\.java):(\d+): error: (.*)$')


def compile_all(out_path):
    cp_file = os.path.join(ROOT, 'build', 'compile-cp.txt')
    if not os.path.exists(cp_file):
        sys.exit('missing build/compile-cp.txt - run: gradlew dumpCompileClasspath')
    cp = io.open(cp_file, encoding='utf-8').read().strip()

    sources = []
    for dp, _, fs in os.walk(os.path.join(ROOT, 'src', 'main', 'java')):
        sources += [os.path.join(dp, f) for f in fs if f.endswith('.java')]
    io.open(os.path.join(CENSUS_DIR, 'sources.txt'), 'w', encoding='utf-8', newline='\n').write(
        '\n'.join(sources))
    os.makedirs(os.path.join(CENSUS_DIR, 'classes'), exist_ok=True)

    cmd = [os.path.join(JAVA_HOME, 'bin', 'javac.exe'), '-J-Duser.language=en',
           '-Xmaxerrs', '30000', '-nowarn', '-proc:none', '-encoding', 'UTF-8',
           '-d', os.path.join(CENSUS_DIR, 'classes'), '-cp', cp,
           '@' + os.path.join(CENSUS_DIR, 'sources.txt')]
    r = subprocess.run(cmd, capture_output=True, text=True, errors='replace')
    io.open(out_path, 'w', encoding='utf-8', newline='\n').write(r.stdout + r.stderr)
    return len(sources)


def parse(path):
    errors = []
    cur = None
    for line in io.open(path, encoding='utf-8', errors='replace').read().splitlines():
        m = HEADER.match(line)
        if m:
            cur = {'file': m.group(1), 'line': int(m.group(2)), 'msg': m.group(3),
                   'symbol': None, 'loc': None}
            errors.append(cur)
        elif cur is not None:
            s = line.strip()
            if s.startswith('symbol:'):
                cur['symbol'] = s[7:].strip()
            elif s.startswith('location:'):
                cur['loc'] = s[9:].strip()
    return errors


def main():
    out = sys.argv[sys.argv.index('--out') + 1] if '--out' in sys.argv else \
        os.path.join(CENSUS_DIR, 'census.txt')
    n = compile_all(out)
    errors = parse(out)
    print('sources: %d   ERRORS: %d   files affected: %d' % (
        n, len(errors), len({e['file'] for e in errors})))
    print('\n=== TOP MESSAGES ===')
    for msg, c in collections.Counter(e['msg'] for e in errors).most_common(20):
        print('%6d  %s' % (c, msg))
    print('\n=== TOP MISSING SYMBOLS ===')
    sym = collections.Counter()
    for e in errors:
        if 'cannot find symbol' in e['msg'] and e['symbol']:
            sym[re.sub(r'^(class|variable|method|interface|enum)\s+', '', e['symbol'])] += 1
    for s, c in sym.most_common(40):
        print('%6d  %s' % (c, s))
    print('\n=== WORST FILES ===')
    for f, c in collections.Counter(e['file'] for e in errors).most_common(20):
        print('%6d  %s' % (c, os.path.relpath(f, ROOT)))
    json.dump(errors, io.open(out.replace('.txt', '.json'), 'w', encoding='utf-8'),
              ensure_ascii=False)


if __name__ == '__main__':
    main()
