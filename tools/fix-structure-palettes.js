/*
 * Rewrites the legacy SRParasites 1.10.8 structure templates so their palette
 * references block ids that exist in the current mod.
 *
 * IMPORTANT: the NBT tag types must be preserved exactly. Minecraft reads the
 * template with strict accessors such as
 *
 *     CompoundTag.getList("size", Tag.TAG_INT)
 *     CompoundTag.getList("pos",  Tag.TAG_INT)
 *
 * and getList() returns an EMPTY list when the stored tag is not a ListTag of
 * that element type. Serialising those integer lists as TAG_Int_Array silently
 * produces a zero-sized template that places nothing in world, so this tool
 * keeps the original tag type of every node instead of inferring it from the
 * JavaScript value.
 *
 * Usage: node tools/fix-structure-palettes.js <in.nbt> <out.nbt> [...more pairs]
 */

const fs = require('fs');
const zlib = require('zlib');

const TAG_END = 0;
const TAG_BYTE = 1;
const TAG_SHORT = 2;
const TAG_INT = 3;
const TAG_LONG = 4;
const TAG_FLOAT = 5;
const TAG_DOUBLE = 6;
const TAG_BYTE_ARRAY = 7;
const TAG_STRING = 8;
const TAG_LIST = 9;
const TAG_COMPOUND = 10;
const TAG_INT_ARRAY = 11;
const TAG_LONG_ARRAY = 12;

const TAG_NAMES = {
    0: 'END', 1: 'BYTE', 2: 'SHORT', 3: 'INT', 4: 'LONG', 5: 'FLOAT', 6: 'DOUBLE',
    7: 'BYTE_ARRAY', 8: 'STRING', 9: 'LIST', 10: 'COMPOUND', 11: 'INT_ARRAY', 12: 'LONG_ARRAY'
};

/** Data version of Minecraft 1.21.1; keeps DataFixerUpper from migrating the palette. */
const CURRENT_DATA_VERSION = 3955;

// ---------------------------------------------------------------- NBT reading

class NbtReader {
    constructor(buf) {
        this.buf = buf;
        this.pos = 0;
    }

    readRoot() {
        const type = this.buf[this.pos++];
        if (type !== TAG_COMPOUND) {
            throw new Error('root tag must be a compound, got ' + TAG_NAMES[type]);
        }
        const nameLen = this.buf.readUInt16BE(this.pos);
        this.pos += 2;
        this.pos += nameLen;
        return { type: TAG_COMPOUND, name: '', value: this.readCompoundBody() };
    }

    readTag() {
        const type = this.buf[this.pos++];
        if (type === TAG_END) return { type: TAG_END, name: null, value: null };
        const nameLen = this.buf.readUInt16BE(this.pos);
        this.pos += 2;
        const name = this.buf.slice(this.pos, this.pos + nameLen).toString('utf8');
        this.pos += nameLen;
        return { type, name, value: this.readPayload(type) };
    }

    readCompoundBody() {
        const entries = [];
        for (;;) {
            const tag = this.readTag();
            if (tag.type === TAG_END) return { entries };
            entries.push(tag);
        }
    }

    readPayload(type) {
        switch (type) {
            case TAG_BYTE: return this.buf.readInt8(this.pos++);
            case TAG_SHORT: { const v = this.buf.readInt16BE(this.pos); this.pos += 2; return v; }
            case TAG_INT: { const v = this.buf.readInt32BE(this.pos); this.pos += 4; return v; }
            case TAG_LONG: { const v = this.buf.readBigInt64BE(this.pos); this.pos += 8; return v; }
            case TAG_FLOAT: { const v = this.buf.readFloatBE(this.pos); this.pos += 4; return v; }
            case TAG_DOUBLE: { const v = this.buf.readDoubleBE(this.pos); this.pos += 8; return v; }
            case TAG_BYTE_ARRAY: {
                const len = this.buf.readInt32BE(this.pos); this.pos += 4;
                const v = this.buf.slice(this.pos, this.pos + len); this.pos += len;
                return v;
            }
            case TAG_STRING: {
                const len = this.buf.readUInt16BE(this.pos); this.pos += 2;
                const v = this.buf.slice(this.pos, this.pos + len).toString('utf8'); this.pos += len;
                return v;
            }
            case TAG_LIST: {
                const elementType = this.buf[this.pos++];
                const len = this.buf.readInt32BE(this.pos); this.pos += 4;
                const items = [];
                for (let i = 0; i < len; i++) items.push(this.readPayload(elementType));
                return { elementType, items };
            }
            case TAG_COMPOUND: return this.readCompoundBody();
            case TAG_INT_ARRAY: {
                const len = this.buf.readInt32BE(this.pos); this.pos += 4;
                const v = [];
                for (let i = 0; i < len; i++) { v.push(this.buf.readInt32BE(this.pos)); this.pos += 4; }
                return v;
            }
            case TAG_LONG_ARRAY: {
                const len = this.buf.readInt32BE(this.pos); this.pos += 4;
                const v = [];
                for (let i = 0; i < len; i++) { v.push(this.buf.readBigInt64BE(this.pos)); this.pos += 8; }
                return v;
            }
            default: throw new Error('unknown NBT tag ' + type);
        }
    }
}

// ---------------------------------------------------------------- NBT writing

class NbtWriter {
    writeRoot(root) {
        return this.writeTag(root);
    }

    writeTag(tag) {
        const header = [Buffer.from([tag.type])];
        if (tag.type === TAG_END) return Buffer.from([TAG_END]);
        const name = Buffer.from(tag.name || '', 'utf8');
        const nameLength = Buffer.alloc(2);
        nameLength.writeUInt16BE(name.length);
        return Buffer.concat([...header, nameLength, name, this.writePayload(tag)]);
    }

    writePayload(tag) {
        const { type, value } = tag;
        switch (type) {
            case TAG_BYTE: { const b = Buffer.alloc(1); b.writeInt8(value); return b; }
            case TAG_SHORT: { const b = Buffer.alloc(2); b.writeInt16BE(value); return b; }
            case TAG_INT: { const b = Buffer.alloc(4); b.writeInt32BE(value); return b; }
            case TAG_LONG: { const b = Buffer.alloc(8); b.writeBigInt64BE(BigInt(value)); return b; }
            case TAG_FLOAT: { const b = Buffer.alloc(4); b.writeFloatBE(value); return b; }
            case TAG_DOUBLE: { const b = Buffer.alloc(8); b.writeDoubleBE(value); return b; }
            case TAG_BYTE_ARRAY: {
                const len = Buffer.alloc(4); len.writeInt32BE(value.length);
                return Buffer.concat([len, value]);
            }
            case TAG_STRING: {
                const raw = Buffer.from(value, 'utf8');
                const len = Buffer.alloc(2); len.writeUInt16BE(raw.length);
                return Buffer.concat([len, raw]);
            }
            case TAG_LIST: {
                const len = Buffer.alloc(4); len.writeInt32BE(value.items.length);
                const parts = [Buffer.from([value.elementType]), len];
                for (const item of value.items) parts.push(this.writePayload({ type: value.elementType, value: item }));
                return Buffer.concat(parts);
            }
            case TAG_COMPOUND: {
                const parts = [];
                for (const entry of value.entries) parts.push(this.writeTag(entry));
                parts.push(Buffer.from([TAG_END]));
                return Buffer.concat(parts);
            }
            case TAG_INT_ARRAY: {
                const len = Buffer.alloc(4); len.writeInt32BE(value.length);
                const parts = [len];
                for (const item of value) { const b = Buffer.alloc(4); b.writeInt32BE(item); parts.push(b); }
                return Buffer.concat(parts);
            }
            case TAG_LONG_ARRAY: {
                const len = Buffer.alloc(4); len.writeInt32BE(value.length);
                const parts = [len];
                for (const item of value) { const b = Buffer.alloc(8); b.writeBigInt64BE(BigInt(item)); parts.push(b); }
                return Buffer.concat(parts);
            }
            default: throw new Error('cannot write NBT tag ' + type);
        }
    }
}

// ------------------------------------------------------------- palette remap

// Every `srparasites:<id>` used by the shipped templates maps onto a `csrp:<id>`.
// Blocks whose 1.10.8 form was split into one block per variant need an explicit
// alias; everything else is a plain namespace rename (the legacy ids are all
// registered by ModBlocks#registerLegacyBlocks).
const VARIANT_BLOCKS = {
    parasitestain: {
        flesh: 'parasitestain_flesh', spore: 'parasitestain_spore', mud: 'parasitestain_mud',
        dirt: 'parasitestain_dirt', feeler: 'parasitestain_feeler', red: 'parasitestain_red',
        sackflesh: 'parasitestain_sackflesh'
    },
    parasiterubble: {
        bone: 'parasiterubble_bone', flesh: 'parasiterubble_flesh', stone: 'parasiterubble_stone',
        stonedebris: 'parasiterubble_stonedebris', bricks: 'parasiterubble_bricks',
        metal: 'parasiterubble_metal', wood: 'parasiterubble_wood', fungus: 'parasiterubble_fungus',
        obsidian: 'parasiterubble_obsidian', weathb: 'parasiterubble_weathb',
        weathbc: 'parasiterubble_weathbc', weathfs: 'parasiterubble_weathfs'
    },
    parasiterubbleslabhalf: {
        bone: 'parasiterubbleslabhalf_bone', flesh: 'parasiterubbleslabhalf_flesh',
        stone: 'parasiterubbleslabhalf_stone', stonedebris: 'parasiterubbleslabhalf_stonedebris',
        bricks: 'parasiterubbleslabhalf_bricks', metal: 'parasiterubbleslabhalf_metal',
        wood: 'parasiterubbleslabhalf_wood', fungus: 'parasiterubbleslabhalf_fungus',
        obsidian: 'parasiterubbleslabhalf_obsidian'
    },
    // The 1.10.8 item-model slab names were never block ids; collapse them onto the
    // registered generic slab blocks.
    slabsflesh: { '*': 'parasitestainslabhalf' },
    slabspore: { '*': 'parasitestainslabhalf' },
    slabstone: { '*': 'parasitestainslabhalf' },
    slabmud: { '*': 'parasitestainslabhalf' },
    slabdirt: { '*': 'parasitestainslabhalf' },
    slabfeeler: { '*': 'parasitestainslabhalf' },
    slabred: { '*': 'parasitestainslabhalf' },
    slabsackflesh: { '*': 'parasitestainslabhalf' },
    slabsflesh_double: { '*': 'parasitestainslabdouble' },
    slabspore_double: { '*': 'parasitestainslabdouble' },
    slabstone_double: { '*': 'parasitestainslabdouble' },
    slabmud_double: { '*': 'parasitestainslabdouble' },
    slabdirt_double: { '*': 'parasitestainslabdouble' },
    slabfeeler_double: { '*': 'parasitestainslabdouble' },
    slabred_double: { '*': 'parasitestainslabdouble' },
    slabsackflesh_double: { '*': 'parasitestainslabdouble' },
    slabbone: { '*': 'parasiterubbleslabhalf_stone' },
    slabflesh: { '*': 'parasiterubbleslabhalf_stone' },
    slabstonedebris: { '*': 'parasiterubbleslabhalf_stone' },
    slabbricks: { '*': 'parasiterubbleslabhalf_stone' },
    slabmetal: { '*': 'parasiterubbleslabhalf_stone' },
    slabwood: { '*': 'parasiterubbleslabhalf_stone' },
    slabfungus: { '*': 'parasiterubbleslabhalf_stone' },
    slabobsidian: { '*': 'parasiterubbleslabhalf_stone' },
    parasiteplank: { deadhead: 'parasiteplank_deadhead' },
    parasiterubbledense: { wall: 'parasiterubbledense' }
};

/** Properties that no longer map onto the modern block and are safe to drop. */
const ALWAYS_DROPPED = new Set(['variant', 'shape']);

/**
 * 1.12 stored every colour variant of a block as one id plus a `color` property;
 * 1.13 flattened them into one id per colour. Values were also renamed
 * (silver -> light_gray).
 */
const COLOUR_FAMILIES = {
    wool: '{color}_wool',
    carpet: '{color}_carpet',
    stained_glass: '{color}_stained_glass',
    stained_glass_pane: '{color}_stained_glass_pane',
    stained_hardened_clay: '{color}_terracotta',
    concrete: '{color}_concrete',
    concrete_powder: '{color}_concrete_powder'
};
const COLOUR_RENAMES = { silver: 'light_gray' };

/** Other well known 1.12 -> 1.13 renames that may appear in the legacy templates. */
const FLATTENED_IDS = {
    grass: 'grass_block',
    deadbush: 'dead_bush',
    waterlily: 'lily_pad',
    noteblock: 'note_block',
    tallgrass: 'grass',
    red_flower: 'poppy',
    yellow_flower: 'dandelion',
    bed: 'red_bed',
    wooden_door: 'oak_door',
    trapdoor: 'oak_trapdoor',
    wooden_button: 'oak_button',
    wooden_pressure_plate: 'oak_pressure_plate',
    fence: 'oak_fence',
    fence_gate: 'oak_fence_gate',
    planks: 'oak_planks',
    sapling: 'oak_sapling',
    log: 'oak_log',
    log2: 'dark_oak_log',
    leaves: 'oak_leaves',
    leaves2: 'dark_oak_leaves',
    stonebrick: 'stone_bricks',
    brick_block: 'bricks',
    nether_brick: 'nether_bricks',
    quartz_block: 'quartz_block'
};

/** Targets that are slabs and therefore use `type` instead of the 1.10.8 `half`. */
function isSlabTarget(id) {
    return id.endsWith('slabhalf') || id.endsWith('slabdouble') || id.includes('_slab');
}

/**
 * 1.12 walls stored their connections as booleans; 1.13+ uses the `WallSide` enum,
 * so `false` becomes `none` and `true` becomes a low connection.
 */
function isWallTarget(id) {
    return !id.endsWith('stairs') && (id.endsWith('_wall') || id.endsWith('wall'));
}

const WALL_SIDE_PROPERTIES = new Set(['north', 'south', 'east', 'west']);

function flattenVanilla(id, props) {
    const colour = COLOUR_FAMILIES[id];
    if (colour && props.color) {
        const name = COLOUR_RENAMES[props.color] || props.color;
        return { id: colour.replace('{color}', name), dropColor: true };
    }
    if (FLATTENED_IDS[id]) {
        return { id: FLATTENED_IDS[id], dropColor: false };
    }
    return { id, dropColor: false };
}

function remapPaletteEntry(entry) {
    // `entry` is the raw payload of a TAG_Compound list element, i.e. { entries: [...] }.
    const properties = entry.entries.find((e) => e.name === 'Properties');
    const nameEntry = entry.entries.find((e) => e.name === 'Name');
    if (!nameEntry) return entry;

    const fullName = String(nameEntry.value);
    const namespace = fullName.includes(':') ? fullName.split(':')[0] : 'minecraft';
    const base = fullName.includes(':') ? fullName.split(':')[1] : fullName;

    const props = {};
    if (properties) {
        for (const prop of properties.value.entries) props[prop.name] = String(prop.value);
    }

    let target;
    let dropVariant = false;
    let dropColor = false;
    if (namespace === 'srparasites') {
        const variants = VARIANT_BLOCKS[base];
        target = variants ? (variants[props.variant] || variants['*'] || base) : base;
        dropVariant = true;
        nameEntry.value = 'csrp:' + target;
    } else if (namespace === 'minecraft') {
        const flat = flattenVanilla(base, props);
        target = flat.id;
        dropColor = flat.dropColor;
        nameEntry.value = 'minecraft:' + target;
    } else {
        return entry;
    }

    if (properties) {
        const rewritten = [];
        for (const prop of properties.value.entries) {
            if (ALWAYS_DROPPED.has(prop.name)) continue;
            if (prop.name === 'variant' && dropVariant) continue;
            if (prop.name === 'color' && dropColor) continue;
            if (prop.name === 'half') {
                if (isSlabTarget(target)) {
                    // SlabBlock uses SLAB_TYPE (bottom/top/double); the double ids are
                    // already double blocks.
                    const value = target.endsWith('slabdouble') || target.endsWith('_slab_double')
                            ? 'double' : String(prop.value);
                    rewritten.push({ type: TAG_STRING, name: 'type', value });
                    continue;
                }
            }
            if (isWallTarget(target) && WALL_SIDE_PROPERTIES.has(prop.name)) {
                rewritten.push({
                    type: TAG_STRING,
                    name: prop.name,
                    value: String(prop.value) === 'true' ? 'low' : 'none'
                });
                continue;
            }
            rewritten.push(prop);
        }
        properties.value.entries = rewritten;
        if (rewritten.length === 0) {
            entry.entries = entry.entries.filter((e) => e !== properties);
        }
    }
    return entry;
}

function rewriteTemplate(buf) {
    const root = new NbtReader(buf).readRoot();
    const fields = root.value.entries;

    const palette = fields.find((f) => f.name === 'palette');
    if (!palette || palette.type !== TAG_LIST || palette.value.elementType !== TAG_COMPOUND) {
        throw new Error('missing compound palette');
    }
    for (const entry of palette.value.items) remapPaletteEntry(entry);

    const dataVersion = fields.find((f) => f.name === 'DataVersion');
    if (dataVersion) dataVersion.value = CURRENT_DATA_VERSION;

    const size = fields.find((f) => f.name === 'size');
    if (!size || size.type !== TAG_LIST || size.value.elementType !== TAG_INT) {
        throw new Error('size is not a LIST<INT>');
    }
    return new NbtWriter().writeRoot(root);
}

// -------------------------------------------------------------------- driver

const args = process.argv.slice(2);
if (args.length === 0 || args.length % 2 !== 0) {
    console.error('usage: node tools/fix-structure-palettes.js <in.nbt> <out.nbt> [...]');
    process.exit(2);
}
for (let i = 0; i < args.length; i += 2) {
    const input = args[i];
    const output = args[i + 1];
    let buf = fs.readFileSync(input);
    let gzipped = false;
    if (buf[0] === 0x1f && buf[1] === 0x8b) {
        buf = zlib.gunzipSync(buf);
        gzipped = true;
    }
    const rewritten = rewriteTemplate(buf);
    fs.writeFileSync(output, gzipped ? zlib.gzipSync(rewritten) : rewritten);
    console.log('rewrote ' + output + ' (' + rewritten.length + ' bytes)');
}
