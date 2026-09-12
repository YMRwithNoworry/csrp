// Finds every PNG under the given roots that contains pixels close to a colour.
// usage: node tools/find-texture-colour.js rrggbb [tolerance] [root...]
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

function decodePNG(file) {
    const buf = fs.readFileSync(file);
    let pos = 8;
    let width = 0;
    let height = 0;
    let bitDepth = 0;
    let colorType = 0;
    const idat = [];
    let palette = null;
    while (pos + 8 <= buf.length) {
        const len = buf.readUInt32BE(pos);
        const type = buf.slice(pos + 4, pos + 8).toString('ascii');
        const data = buf.slice(pos + 8, pos + 8 + len);
        if (type === 'IHDR') {
            width = data.readUInt32BE(0);
            height = data.readUInt32BE(4);
            bitDepth = data[8];
            colorType = data[9];
        } else if (type === 'PLTE') palette = data;
        else if (type === 'IDAT') idat.push(data);
        else if (type === 'IEND') break;
        pos += 12 + len;
    }
    if (bitDepth !== 8) return null;
    const raw = zlib.inflateSync(Buffer.concat(idat));
    const channels = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[colorType];
    if (!channels) return null;
    const stride = width * channels;
    const out = Buffer.alloc(height * stride);
    let rp = 0;
    for (let y = 0; y < height; y++) {
        const filter = raw[rp++];
        const line = raw.slice(rp, rp + stride);
        rp += stride;
        const prev = y === 0 ? Buffer.alloc(stride) : out.slice((y - 1) * stride, y * stride);
        const cur = out.slice(y * stride, (y + 1) * stride);
        for (let x = 0; x < stride; x++) {
            const a = x >= channels ? cur[x - channels] : 0;
            const b = prev[x];
            const c = x >= channels ? prev[x - channels] : 0;
            let value = line[x];
            switch (filter) {
                case 0: break;
                case 1: value = (value + a) & 0xff; break;
                case 2: value = (value + b) & 0xff; break;
                case 3: value = (value + ((a + b) >> 1)) & 0xff; break;
                case 4: {
                    const p = a + b - c;
                    const pa = Math.abs(p - a);
                    const pb = Math.abs(p - b);
                    const pc = Math.abs(p - c);
                    value = (value + (pa <= pb && pa <= pc ? a : pb <= pc ? b : c)) & 0xff;
                    break;
                }
                default: return null;
            }
            cur[x] = value;
        }
    }
    const pixels = [];
    for (let i = 0; i < width * height; i++) {
        if (colorType === 3) {
            const idx = out[i];
            pixels.push(palette[idx * 3], palette[idx * 3 + 1], palette[idx * 3 + 2]);
        } else if (colorType === 0 || colorType === 4) {
            pixels.push(out[i * channels], out[i * channels], out[i * channels]);
        } else {
            pixels.push(out[i * channels], out[i * channels + 1], out[i * channels + 2]);
        }
    }
    return { width, height, pixels };
}

const target = process.argv[2];
const tol = parseInt(process.argv[3] || '8', 10);
const roots = process.argv.slice(4);
const tr = parseInt(target.slice(0, 2), 16);
const tg = parseInt(target.slice(2, 4), 16);
const tb = parseInt(target.slice(4, 6), 16);

function walk(dir, out) {
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        const full = path.join(dir, entry.name);
        if (entry.isDirectory()) walk(full, out);
        else if (entry.name.toLowerCase().endsWith('.png')) out.push(full);
    }
    return out;
}

const files = [];
for (const root of roots) {
    if (!fs.existsSync(root)) continue;
    if (fs.statSync(root).isDirectory()) walk(root, files);
    else files.push(root);
}
const hits = [];
for (const file of files) {
    let img;
    try {
        img = decodePNG(file);
    } catch (error) {
        continue;
    }
    if (!img) continue;
    let count = 0;
    const seen = new Set();
    for (let i = 0; i < img.pixels.length; i += 3) {
        if (Math.abs(img.pixels[i] - tr) <= tol && Math.abs(img.pixels[i + 1] - tg) <= tol
                && Math.abs(img.pixels[i + 2] - tb) <= tol) {
            count++;
            if (seen.size < 4) seen.add('#' + [img.pixels[i], img.pixels[i + 1], img.pixels[i + 2]]
                    .map((v) => v.toString(16).padStart(2, '0')).join(''));
        }
    }
    if (count > 0) hits.push([count, file, [...seen].join(',')]);
}
hits.sort((a, b) => b[0] - a[0]);
console.log('searched ' + files.length + ' png files for #' + target + ' +-' + tol);
for (const [count, file, sample] of hits.slice(0, 40)) console.log(count + '\t' + file + '\t' + sample);
if (!hits.length) console.log('no matches');
