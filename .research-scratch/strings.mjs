import fs from 'node:fs';
import path from 'node:path';

const root = process.argv[2];
const files = [];
(function walk(dir) {
  const st = fs.statSync(dir);
  if (st.isFile()) {
    if (dir.endsWith('.class')) files.push(dir);
    return;
  }
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p);
    else if (e.name.endsWith('.class')) files.push(p);
  }
})(root);

const pattern = process.argv[3] ? new RegExp(process.argv[3], 'i') : null;

for (const f of files) {
  const buf = fs.readFileSync(f);
  const s = buf.toString('latin1');
  // printable ASCII runs of length >= 4
  const strs = s.match(/[\x20-\x7E]{4,}/g) || [];
  const hits = pattern ? strs.filter((x) => pattern.test(x)) : strs;
  if (hits.length) {
    console.log('=== ' + path.relative(root, f).replace(/\\/g, '/'));
    for (const h of [...new Set(hits)]) console.log('    ' + h);
  }
}
