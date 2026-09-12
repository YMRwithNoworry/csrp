// Dumps the dominant colours of a PNG so we can spot placeholder textures.
const fs = require('fs');
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
    let trns = null;
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
        else if (type === 'tRNS') trns = data;
        else if (type === 'IDAT') idat.push(data);
        else if (type === 'IEND') break;
        pos += 12 + len;
    }
    const raw = zlib.inflateSync(Buffer.concat(idat));
    const channels = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[colorType];
    if (bitDepth !== 8) return { width, height, bitDepth, colorType, unsupported: true };
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
                    const pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
                    value = (value + (pa <= pb && pa <= pc ? a : pb <= pc ? b : c)) & 0xff;
                    break;
                }
                default: throw new Error('bad filter ' + filter);
            }
            cur[x] = value;
        }
    }
    return { width, height, bitDepth, colorType, channels, pixels: out, palette, trns };
}

for (const file of process.argv.slice(2)) {
    let img;
    try {
        img = decodePNG(file);
    } catch (error) {
        console.log(file + ' -> decode failed: ' + error.message);
        continue;
    }
    if (img.unsupported) {
        console.log(file + ' -> bitDepth ' + img.bitDepth + ' colorType ' + img.colorType + ' (not decoded)');
        continue;
    }
    const counts = new Map();
    const frames = Math.floor(img.height / img.width) || 1;
    const first = img.width * img.width;
    for (let i = 0; i < first; i++) {
        const o = i * img.channels;
        let key;
        if (img.colorType === 3) {
            const idx = img.pixels[o];
            key = img.palette.slice(idx * 3, idx * 3 + 3).toString('hex');
        } else {
            key = [img.pixels[o], img.pixels[o + 1], img.pixels[o + 2]].map((v) => v.toString(16).padStart(2, '0')).join('');
        }
        counts.set(key, (counts.get(key) || 0) + 1);
    }
    const top = [...counts.entries()].sort((a, b) => b[1] - a[1]).slice(0, 6);
    console.log(file);
    console.log('   ' + img.width + 'x' + img.height + ' colorType=' + img.colorType
            + ' frames=' + frames + ' distinctColors=' + counts.size);
    console.log('   top colours: ' + top.map(([c, n]) => '#' + c + 'x' + n).join(' '));
}
