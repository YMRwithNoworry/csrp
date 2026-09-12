// Scans PNG textures for a given dominant colour pair, to locate placeholder/odd art.
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

function decodePNG(file) {
    const buf = fs.readFileSync(file);
    let pos = 8;
    let width = 0, height = 0, bitDepth = 0, colorType = 0;
    let palette = null;
    const idat = [];
    while (pos + 8 <= buf.length) {
        const len = buf.readUInt32BE(pos);
        const type = buf.slice(pos + 4, pos + 8).toString('ascii');
        const data = buf.slice(pos + 8, pos + 8 + len);
        if (type === 'IHDR') {
            width = data.readUInt32BE(0); height = data.readUInt32BE(4);
            bitDepth = data[8]; colorType = data[9];
        } else if (type === 'PLTE') palette = data;
        else if (type === 'IDAT') idat.push(data);
        else if (type === 'IEND') break;
        pos += 12 + len;
    }
    if (bitDepth !== 8 || (colorType !== 6 && colorType !== 2 && colorType !== 3)) {
        return { width, height, skipped: true, bitDepth, colorType };
    }
    const channels = { 2: 3, 3: 1, 6: 4 }[colorType];
    const raw = zlib.inflateSync(Buffer.concat(idat));
    const stride = width * channels;
    const out = Buffer.alloc(height * stride);
    let rp = 0;
    for (let y = 0; y < height; y++) {
        const filter = raw[rp++];
        const line = raw.slice(rp, rp + stride); rp += stride;
        const prev = y === 0 ? Buffer.alloc(stride) : out.slice((y - 1) * stride, y * stride);
        const cur = out.slice(y * stride, (y + 1) * stride);
        for (let x = 0; x < stride; x++) {
            const a = x >= channels ? cur[x - channels] : 0;
            const b = prev[x];
            const c = x >= channels ? prev[x - channels] : 0;
            let v = line[x];
            if (filter === 1) v = (v + a) & 0xff;
            else if (filter === 2) v = (v + b) & 0xff;
            else if (filter === 3) v = (v + ((a + b) >> 1)) & 0xff;
            else if (filter === 4) {
                const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
                v = (v + (pa <= pb && pa <= pc ? a : pb <= pc ? b : c)) & 0xff;
            }
            cur[x] = v;
        }
    }
    return { width, height, channels, colorType, pixels: out, palette };
}

function rgbAt(img, i) {
    if (img.colorType === 3) {
        const idx = img.pixels[i];
        return [img.palette[idx * 3], img.palette[idx * 3 + 1], img.palette[idx * 3 + 2]];
    }
    const o = i * img.channels;
    return [img.pixels[o], img.pixels[o + 1], img.pixels[o + 2]];
}

const isCyan = ([r, g, b]) => g > 140 && b > 140 && r < 120 && Math.abs(g - b) < 90;
const isWhite = ([r, g, b]) => r > 225 && g > 225 && b > 225;

function walk(dir, out = []) {
    for (const entry of fs.readdirSync(dir)) {
        const p = path.join(dir, entry);
        const st = fs.statSync(p);
        if (st.isDirectory()) walk(p, out);
        else if (entry.endsWith('.png')) out.push(p);
    }
    return out;
}

const root = process.argv[2] || 'src/main/resources/assets';
const hits = [];
for (const file of walk(root)) {
    let img;
    try { img = decodePNG(file); } catch { continue; }
    if (img.skipped) continue;
    const count = img.width * img.height;
    let cyan = 0, white = 0;
    for (let i = 0; i < count; i++) {
        const c = rgbAt(img, i);
        if (isCyan(c)) cyan++;
        else if (isWhite(c)) white++;
    }
    const cyanShare = cyan / count;
    const whiteShare = white / count;
    if (cyanShare + whiteShare > 0.85 && cyanShare > 0.2 && whiteShare > 0.2) {
        hits.push({ file, cyanShare, whiteShare, size: img.width + 'x' + img.height });
    }
}
hits.sort((a, b) => (b.cyanShare + b.whiteShare) - (a.cyanShare + a.whiteShare));
console.log('textures dominated by cyan+white:', hits.length);
for (const h of hits.slice(0, 40)) {
    console.log('  ' + (h.cyanShare * 100).toFixed(0) + '% cyan / ' + (h.whiteShare * 100).toFixed(0)
            + '% white  ' + h.size + '  ' + h.file);
}
