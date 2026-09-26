// Resizes a PNG with an area-average (box) filter. Reuses the pure-JS codec from png-crop.js.
// usage: node tools/png-resize.js <in.png> <out.png> <size>
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
    const rgb = Buffer.alloc(width * height * 3);
    for (let i = 0; i < width * height; i++) {
        let r; let g; let b;
        if (colorType === 3) {
            const idx = out[i];
            r = palette[idx * 3]; g = palette[idx * 3 + 1]; b = palette[idx * 3 + 2];
        } else if (colorType === 0 || colorType === 4) {
            r = g = b = out[i * channels];
        } else {
            r = out[i * channels]; g = out[i * channels + 1]; b = out[i * channels + 2];
        }
        rgb[i * 3] = r; rgb[i * 3 + 1] = g; rgb[i * 3 + 2] = b;
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

// area average: every destination pixel integrates the exact source rectangle, so non-integer
// ratios (1254 -> 256) stay correct instead of dropping or duplicating rows
function resizeArea(img, outW, outH) {
    const out = Buffer.alloc(outW * outH * 3);
    const scaleX = img.width / outW;
    const scaleY = img.height / outH;
    for (let y = 0; y < outH; y++) {
        const y0 = y * scaleY;
        const y1 = Math.min(img.height, (y + 1) * scaleY);
        const sy0 = Math.floor(y0);
        const sy1 = Math.ceil(y1);
        for (let x = 0; x < outW; x++) {
            const x0 = x * scaleX;
            const x1 = Math.min(img.width, (x + 1) * scaleX);
            const sx0 = Math.floor(x0);
            const sx1 = Math.ceil(x1);
            let r = 0; let g = 0; let b = 0; let weight = 0;
            for (let sy = sy0; sy < sy1; sy++) {
                const wy = Math.min(y1, sy + 1) - Math.max(y0, sy);
                if (wy <= 0) continue;
                for (let sx = sx0; sx < sx1; sx++) {
                    const wx = Math.min(x1, sx + 1) - Math.max(x0, sx);
                    if (wx <= 0) continue;
                    const w = wx * wy;
                    const i = (sy * img.width + sx) * 3;
                    r += img.rgb[i] * w;
                    g += img.rgb[i + 1] * w;
                    b += img.rgb[i + 2] * w;
                    weight += w;
                }
            }
            const o = (y * outW + x) * 3;
            out[o] = Math.round(r / weight);
            out[o + 1] = Math.round(g / weight);
            out[o + 2] = Math.round(b / weight);
        }
    }
    return out;
}

const [input, output, sizeArg] = process.argv.slice(2);
const img = decodePNG(input);
const size = parseInt(sizeArg, 10);
if (!size || size < 1) throw new Error('size required');
const outW = size;
const outH = Math.max(1, Math.round(size * img.height / img.width));
fs.writeFileSync(output, encodePNG(outW, outH, resizeArea(img, outW, outH)));
console.log('wrote ' + output + ' (' + outW + 'x' + outH + ') from ' + input + ' ' + img.width + 'x' + img.height);
