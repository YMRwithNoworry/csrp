/*
 * Validates the NBT structure templates under src/main/resources/data/csrp/structure.
 *
 * Minecraft reads a template with strict accessors such as
 *
 *     CompoundTag.getList("size",  Tag.TAG_INT)
 *     CompoundTag.getList("pos",   Tag.TAG_INT)
 *     CompoundTag.getList("blocks", Tag.TAG_COMPOUND)
 *
 * and getList() returns an EMPTY list when the stored tag is not a ListTag of that
 * element type. An empty "size" yields a 0x0x0 template that silently places
 * nothing in the world, which is exactly the failure this check exists to catch.
 *
 * Usage: node tools/validate-structures.js [dir ...]
 */

const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const TAG = {
    END: 0, BYTE: 1, SHORT: 2, INT: 3, LONG: 4, FLOAT: 5, DOUBLE: 6,
    BYTE_ARRAY: 7, STRING: 8, LIST: 9, COMPOUND: 10, INT_ARRAY: 11, LONG_ARRAY: 12
};
const TAG_NAMES = Object.fromEntries(Object.entries(TAG).map(([k, v]) => [v, k]));

function readRoot(buf) {
    let pos = 0;
    const type = buf[pos++];
    if (type !== TAG.COMPOUND) throw new Error('root is ' + TAG_NAMES[type] + ', expected COMPOUND');
    const nameLen = buf.readUInt16BE(pos); pos += 2 + nameLen;

    function readCompoundBody() {
        const entries = new Map();
        for (;;) {
            const tagType = buf[pos++];
            if (tagType === TAG.END) return entries;
            const len = buf.readUInt16BE(pos); pos += 2;
            const name = buf.slice(pos, pos + len).toString('utf8'); pos += len;
            entries.set(name, { type: tagType, value: readPayload(tagType) });
        }
    }

    function readPayload(t) {
        switch (t) {
            case TAG.BYTE: return buf.readInt8(pos++);
            case TAG.SHORT: { const v = buf.readInt16BE(pos); pos += 2; return v; }
            case TAG.INT: { const v = buf.readInt32BE(pos); pos += 4; return v; }
            case TAG.LONG: { const v = buf.readBigInt64BE(pos); pos += 8; return v; }
            case TAG.FLOAT: { const v = buf.readFloatBE(pos); pos += 4; return v; }
            case TAG.DOUBLE: { const v = buf.readDoubleBE(pos); pos += 8; return v; }
            case TAG.BYTE_ARRAY: { const l = buf.readInt32BE(pos); pos += 4 + l; return null; }
            case TAG.STRING: { const l = buf.readUInt16BE(pos); pos += 2; const v = buf.slice(pos, pos + l).toString('utf8'); pos += l; return v; }
            case TAG.LIST: {
                const elementType = buf[pos++];
                const l = buf.readInt32BE(pos); pos += 4;
                const items = [];
                for (let i = 0; i < l; i++) items.push(readPayload(elementType));
                return { elementType, items };
            }
            case TAG.COMPOUND: return readCompoundBody();
            case TAG.INT_ARRAY: { const l = buf.readInt32BE(pos); pos += 4; const v = []; for (let i = 0; i < l; i++) { v.push(buf.readInt32BE(pos)); pos += 4; } return v; }
            case TAG.LONG_ARRAY: { const l = buf.readInt32BE(pos); pos += 4; for (let i = 0; i < l; i++) pos += 8; return null; }
            default: throw new Error('unknown tag ' + t);
        }
    }
    return { type, value: readCompoundBody() };
}

/** Mirrors CompoundTag#getList: only a ListTag of the requested element type qualifies. */
function getList(compound, name, elementType) {
    const tag = compound.get(name);
    if (!tag || tag.type !== TAG.LIST || tag.value.elementType !== elementType) return [];
    return tag.value.items;
}

function readRegisteredBlocks() {
    const src = fs.readFileSync('src/main/java/alku/csrp/registry/ModBlocks.java', 'utf8');
    const ids = new Set();
    for (const m of src.matchAll(/register\(\s*"([a-z0-9_]+)"/g)) ids.add(m[1]);
    const legacy = src.match(/String\[\] ids = \{([\s\S]*?)\};/);
    if (legacy) for (const m of legacy[1].matchAll(/"([a-z0-9_]+)"/g)) ids.add(m[1]);
    for (const m of src.matchAll(
            /(?:slab|infestedStairs|infestedWall|infestedFence|infested|parasiticPlanks|woodButton|woodPressurePlate|woodLadder|woodBookshelf|glassPane|tintedGlass|parasiteLoot)\(\s*\n?\s*"([a-z0-9_]+)"/g)) {
        ids.add(m[1]);
    }
    return ids;
}

const VANILLA_NAMESPACES = new Set(['minecraft']);

function validate(file, registered) {
    const problems = [];
    let buf = fs.readFileSync(file);
    if (buf[0] === 0x1f && buf[1] === 0x8b) buf = zlib.gunzipSync(buf);
    const root = readRoot(buf);

    const size = getList(root.value, 'size', TAG.INT);
    if (size.length !== 3) {
        problems.push('size is not a 3 entry LIST<INT> (found ' + size.length + ') -> template would be empty');
    }
    const palette = getList(root.value, 'palette', TAG.COMPOUND);
    if (palette.length === 0) problems.push('palette is not a LIST<COMPOUND>');

    const blocks = getList(root.value, 'blocks', TAG.COMPOUND);
    if (blocks.length === 0) problems.push('blocks is not a LIST<COMPOUND>');

    const sources = new Set();
    const vanilla = new Set();
    for (const entry of palette) {
        const nameTag = entry.get('Name');
        if (!nameTag || nameTag.type !== TAG.STRING) { problems.push('palette entry without STRING Name'); continue; }
        const full = nameTag.value;
        const [namespace, id] = full.includes(':') ? full.split(':') : ['minecraft', full];
        sources.add(full);
        if (namespace === 'csrp') {
            if (!registered.has(id)) problems.push('palette references unregistered block ' + full);
        } else if (VANILLA_NAMESPACES.has(namespace)) {
            vanilla.add(full);
        } else {
            problems.push('palette references foreign namespace ' + full);
        }
    }

    let zeroPos = 0;
    for (const block of blocks) {
        const pos = getList(block, 'pos', TAG.INT);
        if (pos.length !== 3) { zeroPos++; continue; }
        const state = block.get('state');
        if (!state || ![TAG.BYTE, TAG.SHORT, TAG.INT, TAG.LONG].includes(state.type)) {
            problems.push('block without numeric state tag');
            break;
        }
    }
    if (zeroPos > 0) problems.push(zeroPos + ' of ' + blocks.length + ' blocks have pos that is not a LIST<INT>');

    return { size, palette: palette.length, blocks: blocks.length, sources, vanilla, problems };
}

const dirs = process.argv.slice(2);
const targets = dirs.length > 0 ? dirs : ['src/main/resources/data/csrp/structure'];
const registered = readRegisteredBlocks();
let failed = 0;
let checked = 0;

for (const dir of targets) {
    for (const file of fs.readdirSync(dir).filter((f) => f.endsWith('.nbt')).sort()) {
        const full = path.join(dir, file);
        checked++;
        try {
            const result = validate(full, registered);
            if (result.problems.length > 0) {
                failed++;
                console.log('BAD  ' + full);
                for (const p of result.problems) console.log('       - ' + p);
            } else {
                console.log('OK   ' + full.padEnd(58) + ' size=' + result.size.join('x')
                        + ' palette=' + result.palette + ' blocks=' + result.blocks);
            }
        } catch (error) {
            failed++;
            console.log('FAIL ' + full + ' : ' + error.message);
        }
    }
}
console.log(failed === 0
    ? 'All ' + checked + ' structure templates validated'
    : failed + ' of ' + checked + ' structure templates failed validation');
process.exit(failed === 0 ? 0 : 1);
