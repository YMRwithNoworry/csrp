// Rewrite the legacy meteor structure templates so every palette entry points
// at a block id that exists in the current mod.
const fs = require('fs');
const zlib = require('zlib');

function parseNbt(buf) {
    let p = 0;
    function readTag() {
        const type = buf[p++];
        if (type === 0) return { type: 0 };
        const nl = buf.readUInt16BE(p); p += 2;
        const name = buf.slice(p, p + nl).toString('utf8'); p += nl;
        return { type, name, value: readPayload(type) };
    }
    function readPayload(type) {
        let v;
        switch (type) {
            case 1: v = buf.readInt8(p); p += 1; break;
            case 2: v = buf.readInt16BE(p); p += 2; break;
            case 3: v = buf.readInt32BE(p); p += 4; break;
            case 4: v = buf.readBigInt64BE(p); p += 8; break;
            case 5: v = buf.readFloatBE(p); p += 4; break;
            case 6: v = buf.readDoubleBE(p); p += 8; break;
            case 7: { const l = buf.readInt32BE(p); p += 4; v = buf.slice(p, p + l); p += l; break; }
            case 8: { const l = buf.readUInt16BE(p); p += 2; v = buf.slice(p, p + l).toString('utf8'); p += l; break; }
            case 9: { const et = buf[p++]; const l = buf.readInt32BE(p); p += 4; v = []; for (let i = 0; i < l; i++) v.push(readPayload(et)); break; }
            case 10: { v = {}; while (true) { const s = readTag(); if (s.type === 0) break; v[s.name] = s.value; } break; }
            case 11: { const l = buf.readInt32BE(p); p += 4; v = []; for (let i = 0; i < l; i++) { v.push(buf.readInt32BE(p)); p += 4; } break; }
            case 12: { const l = buf.readInt32BE(p); p += 4; v = []; for (let i = 0; i < l; i++) { v.push(buf.readBigInt64BE(p)); p += 8; } break; }
            default: throw new Error('unknown NBT type ' + type);
        }
        return v;
    }
    return readTag();
}
function typeOf(v) {
    if (typeof v === 'string') return 8;
    if (typeof v === 'number') return Number.isInteger(v) ? 3 : 5;
    if (typeof v === 'bigint') return 4;
    if (Buffer.isBuffer(v)) return 7;
    if (Array.isArray(v)) { if (!v.length) return 9; const t = typeOf(v[0]); return t === 3 ? 11 : t === 4 ? 12 : 9; }
    if (v && typeof v === 'object') return 10;
    throw new Error('cannot encode ' + typeof v);
}
function writeTag(name, value) {
    const type = typeOf(value);
    const nameBuf = Buffer.from(name, 'utf8');
    const head = Buffer.alloc(3);
    head[0] = type; head.writeUInt16BE(nameBuf.length, 1);
    return Buffer.concat([head, nameBuf, writePayload(type, value)]);
}
function writePayload(type, value) {
    switch (type) {
        case 1: { const b = Buffer.alloc(1); b.writeInt8(value); return b; }
        case 2: { const b = Buffer.alloc(2); b.writeInt16BE(value); return b; }
        case 3: { const b = Buffer.alloc(4); b.writeInt32BE(value); return b; }
        case 4: { const b = Buffer.alloc(8); b.writeBigInt64BE(BigInt(value)); return b; }
        case 5: { const b = Buffer.alloc(4); b.writeFloatBE(value); return b; }
        case 6: { const b = Buffer.alloc(8); b.writeDoubleBE(value); return b; }
        case 7: { const len = Buffer.alloc(4); len.writeInt32BE(value.length); return Buffer.concat([len, value]); }
        case 8: { const raw = Buffer.from(value, 'utf8'); const len = Buffer.alloc(2); len.writeUInt16BE(raw.length); return Buffer.concat([len, raw]); }
        case 9: {
            const et = value.length ? typeOf(value[0]) : 0;
            const len = Buffer.alloc(4); len.writeInt32BE(value.length);
            const parts = [Buffer.from([et]), len];
            for (const item of value) parts.push(writePayload(et, item));
            return Buffer.concat(parts);
        }
        case 10: {
            const parts = [];
            for (const [k, v] of Object.entries(value)) parts.push(writeTag(k, v));
            parts.push(Buffer.from([0]));
            return Buffer.concat(parts);
        }
        case 11: {
            const len = Buffer.alloc(4); len.writeInt32BE(value.length);
            const parts = [len];
            for (const item of value) { const b = Buffer.alloc(4); b.writeInt32BE(item); parts.push(b); }
            return Buffer.concat(parts);
        }
        case 12: {
            const len = Buffer.alloc(4); len.writeInt32BE(value.length);
            const parts = [len];
            for (const item of value) { const b = Buffer.alloc(8); b.writeBigInt64BE(BigInt(item)); parts.push(b); }
            return Buffer.concat(parts);
        }
        default: throw new Error('cannot encode type ' + type);
    }
}
const encodeNbt = (root) => writeTag(root.name || '', root.value);

const STAINS = { flesh: 'parasitestain_flesh', spore: 'parasitestain_spore', mud: 'parasitestain_mud',
    dirt: 'parasitestain_dirt', feeler: 'parasitestain_feeler', red: 'parasitestain_red',
    sackflesh: 'parasitestain_sackflesh' };
const RUBBLE = { bone: 'parasiterubble_bone', flesh: 'parasiterubble_flesh', stone: 'parasiterubble_stone',
    stonedebris: 'parasiterubble_stonedebris', bricks: 'parasiterubble_bricks',
    metal: 'parasiterubble_metal', wood: 'parasiterubble_wood', fungus: 'parasiterubble_fungus',
    obsidian: 'parasiterubble_obsidian' };
const RUBBLE_SLAB = { bone: 'parasiterubbleslabhalf_bone', flesh: 'parasiterubbleslabhalf_flesh',
    stone: 'parasiterubbleslabhalf_stone', stonedebris: 'parasiterubbleslabhalf_stonedebris',
    bricks: 'parasiterubbleslabhalf_bricks', metal: 'parasiterubbleslabhalf_metal',
    wood: 'parasiterubbleslabhalf_wood', fungus: 'parasiterubbleslabhalf_fungus',
    obsidian: 'parasiterubbleslabhalf_obsidian' };
const KEEP = new Set(['parasitestain_mudstairs', 'parasitestain_fleshstairs',
    'parasitestain_feelerstairs', 'parasitestain_dirtstairs', 'parasiterubble_stonestairs',
    'parasiterubble_bonestairs', 'parasiterubble_fleshstairs', 'parasiterubble_bricksstairs',
    'parasiterubble_metalstairs', 'parasiterubble_woodstairs', 'parasiterubble_fungusstairs',
    'parasiterubble_obsidianstairs', 'parasitestain_flesh_wall', 'parasiterubble_metal_wall']);

function remap(entry) {
    const props = { ...(entry.Properties || {}) };
    const ns = entry.Name.includes(':') ? entry.Name.split(':')[0] : 'minecraft';
    const base = entry.Name.includes(':') ? entry.Name.split(':')[1] : entry.Name;
    if (ns === 'minecraft') return entry;
    let out = 'csrp:' + base;
    if (base === 'parasitestain') {
        out = 'csrp:' + (STAINS[props.variant] || 'parasitestain_flesh');
        delete props.variant;
    } else if (base === 'parasiterubble') {
        out = 'csrp:' + (RUBBLE[props.variant] || 'parasiterubble_stone');
        delete props.variant;
    } else if (KEEP.has(base)) {
        delete props.variant;
    } else if (base === 'parasitestainslabhalf' || base === 'parasitestainslabdouble') {
        delete props.variant; delete props.shape;
    } else if (base === 'parasiterubbleslabhalf' || base === 'parasiterubbleslabdouble') {
        out = 'csrp:' + (RUBBLE_SLAB[props.variant] || 'parasiterubbleslabhalf_stone');
        delete props.variant; delete props.shape;
    } else if (base === 'slabsflesh' || base === 'slabspore' || base === 'slabstone' || base === 'slabmud' || base === 'slabdirt' || base === 'slabfeeler' || base === 'slabred' || base === 'slabsackflesh') {
        out = 'csrp:parasitestainslabhalf';
        delete props.variant; delete props.shape;
    } else if (base === 'slabsflesh_double' || base === 'slabspore_double' || base === 'slabstone_double' || base === 'slabmud_double' || base === 'slabdirt_double' || base === 'slabfeeler_double' || base === 'slabred_double' || base === 'slabsackflesh_double') {
        out = 'csrp:parasitestainslabdouble';
        delete props.variant; delete props.shape;
    } else if (base === 'slabbone' || base === 'slabflesh' || base === 'slabstonedebris' || base === 'slabbricks' || base === 'slabmetal' || base === 'slabwood' || base === 'slabfungus' || base === 'slabobsidian') {
        out = 'csrp:parasiterubbleslabhalf_stone';
        delete props.variant; delete props.shape;
    } else if (base === 'infested_terracotta_slab' || base === 'parasitic_compressed_colony_stone_slab') {
        delete props.variant; delete props.shape;
    } else if (base === 'parasiterubbledense') {
        delete props.variant;
    } else if (base === 'infestedbush') {
        delete props.node; delete props.variant; delete props.end;
    } else if (base === 'deadblood') {
        delete props.level;
    } else if (base === 'alveoli') {
        delete props.depleted; delete props.active;
    }
    return { Name: out, ...(Object.keys(props).length ? { Properties: props } : {}) };
}

for (const file of process.argv.slice(2)) {
    let buf = fs.readFileSync(file);
    let gz = false;
    if (buf[0] === 0x1f && buf[1] === 0x8b) { buf = zlib.gunzipSync(buf); gz = true; }
    const root = parseNbt(buf);
    if (!root.value.palette) { console.log('skip ' + file); continue; }
    root.value.palette = root.value.palette.map(remap);
    root.value.DataVersion = 3955;
    delete root.value.ForgeDataVersion;
    if (root.value.author && String(root.value.author).includes('Parasitic')) root.value.author = 'csrp';
    const out = encodeNbt(root);
    fs.writeFileSync(file, gz ? zlib.gzipSync(out) : out);
    console.log('rewrote ' + file);
}
