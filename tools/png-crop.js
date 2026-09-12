// Crops and (nearest-neighbour) upscales a PNG so details can be inspected.
// usage: node tools/png-crop.js <in.png> <out.png> <x> <y> <w> <h> [scale]
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
    const raw = zlib.inflateSync(Buffer.concat(idat));
    const channels = { 0: 1, 2: 3, 3: 1, 4: 2, 6: 4 }[colorType];
    if (bitDepth !== 8) throw new Error('bitDepth ' + bitDepth + ' unsupported');
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
                default: throw new Error('bad filter ' + filter);
            }
            cur[x] = value;
        }
    }
    // normalise anything that is not plain RGB to RGB
    const rgb = Buffer.alloc(width * height * 3);
    for (let i = 0; i < width * height; i++) {
        let r;
        let g;
        let b;
        if (colorType === 3) {
            const idx = out[i];
            r = palette[idx * 3];
            g = palette[idx * 3 + 1];
            b = palette[idx * 3 + 2];
        } else if (colorType === 0 || colorType === 4) {
            r = g = b = out[i * channels];
        } else {
            r = out[i * channels];
            g = out[i * channels + 1];
            b = out[i * channels + 2];
        }
        rgb[i * 3] = r;
        rgb[i * 3 + 1] = g;
        rgb[i * 3 + 2] = b;
    }
    return { width, height, rgb };
}

function encodePNG(width, height, rgb) {
    const stride = width * 3;
    const raw = Buffer.alloc(height * (stride + 1));
    for (let y = 0; y < height; y++) {
        raw[y * (stride + 1)] = 0;
        rgb.copy(raw, y * (stride + 1) + 1, y * stride, (y + 1) * stride);
    }
    const chunks = [];
    const chunk = (type, data) => {
        const len = Buffer.alloc(4);
        len.writeUInt32BE(data.length, 0);
        const body = Buffer.concat([Buffer.from(type, 'ascii'), data]);
        const crc = Buffer.alloc(4);
        crc.writeUInt32BE(crc32(body) >>> 0, 0);
        chunks.push(len, body, crc);
    };
    const ihdr = Buffer.alloc(13);
    ihdr.writeUInt32BE(width, 0);
    ihdr.writeUInt32BE(height, 4);
    ihdr[8] = 8;
    ihdr[9] = 2;
    chunk('IHDR', ihdr);
    chunk('IDAT', zlib.deflateSync(raw, { level: 9 }));
    chunk('IEND', Buffer.alloc(0));
    return Buffer.concat([Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]), ...chunks]);
}

let crcTable = null;
function crc32(buf) {
    if (!crcTable) {
        crcTable = [];
        for (let n = 0; n < 256; n++) {
            let c = n;
            for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
            crcTable[n] = c;
        }
    }
    let c = 0xffffffff;
    for (let i = 0; i < buf.length; i++) c = crcTable[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
    return (c ^ 0xffffffff) >>> 0;
}

const [input, output, xs, ys, ws, hs, ss] = process.argv.slice(2);
const img = decodePNG(input);
const x0 = Math.max(0, parseInt(xs, 10));
const y0 = Math.max(0, parseInt(ys, 10));
const w = Math.min(parseInt(ws, 10), img.width - x0);
const h = Math.min(parseInt(hs, 10), img.height - y0);
const scale = ss ? parseInt(ss, 10) : 2;
const outW = w * scale;
const outH = h * scale;
const out = Buffer.alloc(outW * outH * 3);
for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
        const src = ((y0 + y) * img.width + (x0 + x)) * 3;
        for (let sy = 0; sy < scale; sy++) {
            for (let sx = 0; sx < scale; sx++) {
                const dst = ((y * scale + sy) * outW + (x * scale + sx)) * 3;
                out[dst] = img.rgb[src];
                out[dst + 1] = img.rgb[src + 1];
                out[dst + 2] = img.rgb[src + 2];
            }
        }
    }
}
fs.writeFileSync(output, encodePNG(outW, outH, out));
console.log('wrote ' + output + ' (' + outW + 'x' + outH + ') from ' + input + ' ' + img.width + 'x' + img.height);
