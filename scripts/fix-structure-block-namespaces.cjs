/**
 * The structure NBTs shipped from the original mod still name their blocks in the
 * `srparasites:` namespace, while this port registers them as `csrp:`. A palette entry whose block
 * id does not exist makes the whole template unusable, so `meteor.nbt` and the deadhead-tree
 * templates silently placed nothing.
 *
 * This script rewrites the palette names to the port's namespace and reports every id it could not
 * find in `ModBlocks.java`.
 *
 *   node scripts/fix-structure-block-namespaces.cjs [--dry]
 */
const fs = require("fs");
const path = require("path");
const zlib = require("zlib");

const DRY = process.argv.includes("--dry");
const ROOT = path.resolve(__dirname, "..");
const STRUCTURE_DIR = path.join(ROOT, "src/main/resources/data/csrp/structure");
const BLOCKS_SOURCE = fs.readFileSync(path.join(ROOT, "src/main/java/alku/csrp/registry/ModBlocks.java"), "utf8");

// ---------------------------------------------------------------------------- minimal NBT codec

const TAG = {
  END: 0, BYTE: 1, SHORT: 2, INT: 3, LONG: 4, FLOAT: 5, DOUBLE: 6,
  BYTE_ARRAY: 7, STRING: 8, LIST: 9, COMPOUND: 10, INT_ARRAY: 11, LONG_ARRAY: 12
};

/** Reads a tag payload of the given type. Returns {value, offset}. */
const readPayload = (buffer, type, offset) => {
  switch (type) {
    case TAG.BYTE:
      return { value: buffer.readInt8(offset), offset: offset + 1 };
    case TAG.SHORT:
      return { value: buffer.readInt16BE(offset), offset: offset + 2 };
    case TAG.INT:
      return { value: buffer.readInt32BE(offset), offset: offset + 4 };
    case TAG.LONG:
      return { value: buffer.readBigInt64BE(offset), offset: offset + 8 };
    case TAG.FLOAT:
      return { value: buffer.readFloatBE(offset), offset: offset + 4 };
    case TAG.DOUBLE:
      return { value: buffer.readDoubleBE(offset), offset: offset + 8 };
    case TAG.BYTE_ARRAY: {
      const length = buffer.readInt32BE(offset);
      return { value: buffer.subarray(offset + 4, offset + 4 + length), offset: offset + 4 + length };
    }
    case TAG.STRING: {
      const length = buffer.readUInt16BE(offset);
      const value = buffer.toString("utf8", offset + 2, offset + 2 + length);
      return { value, offset: offset + 2 + length };
    }
    case TAG.LIST: {
      const elementType = buffer.readUInt8(offset);
      const length = buffer.readInt32BE(offset + 1);
      let cursor = offset + 5;
      const items = [];
      for (let index = 0; index < length; index += 1) {
        const read = readPayload(buffer, elementType, cursor);
        items.push(read.value);
        cursor = read.offset;
      }
      return { value: { elementType, items }, offset: cursor };
    }
    case TAG.COMPOUND: {
      const entries = new Map();
      let cursor = offset;
      for (;;) {
        const childType = buffer.readUInt8(cursor);
        cursor += 1;
        if (childType === TAG.END) {
          break;
        }
        const nameLength = buffer.readUInt16BE(cursor);
        const name = buffer.toString("utf8", cursor + 2, cursor + 2 + nameLength);
        cursor += 2 + nameLength;
        const read = readPayload(buffer, childType, cursor);
        entries.set(name, { type: childType, value: read.value });
        cursor = read.offset;
      }
      return { value: entries, offset: cursor };
    }
    case TAG.INT_ARRAY: {
      const length = buffer.readInt32BE(offset);
      const values = [];
      for (let index = 0; index < length; index += 1) {
        values.push(buffer.readInt32BE(offset + 4 + index * 4));
      }
      return { value: values, offset: offset + 4 + length * 4 };
    }
    case TAG.LONG_ARRAY: {
      const length = buffer.readInt32BE(offset);
      const values = [];
      for (let index = 0; index < length; index += 1) {
        values.push(buffer.readBigInt64BE(offset + 4 + index * 8));
      }
      return { value: values, offset: offset + 4 + length * 8 };
    }
    default:
      throw new Error(`unknown NBT tag type ${type} at ${offset}`);
  }
};

const writePayload = (chunks, type, value) => {
  switch (type) {
    case TAG.BYTE:
      chunks.push(Buffer.from([value & 0xff]));
      break;
    case TAG.SHORT: {
      const buffer = Buffer.alloc(2);
      buffer.writeInt16BE(value);
      chunks.push(buffer);
      break;
    }
    case TAG.INT: {
      const buffer = Buffer.alloc(4);
      buffer.writeInt32BE(value);
      chunks.push(buffer);
      break;
    }
    case TAG.LONG: {
      const buffer = Buffer.alloc(8);
      buffer.writeBigInt64BE(value);
      chunks.push(buffer);
      break;
    }
    case TAG.FLOAT: {
      const buffer = Buffer.alloc(4);
      buffer.writeFloatBE(value);
      chunks.push(buffer);
      break;
    }
    case TAG.DOUBLE: {
      const buffer = Buffer.alloc(8);
      buffer.writeDoubleBE(value);
      chunks.push(buffer);
      break;
    }
    case TAG.BYTE_ARRAY: {
      const length = Buffer.alloc(4);
      length.writeInt32BE(value.length);
      chunks.push(length, Buffer.from(value));
      break;
    }
    case TAG.STRING: {
      const encoded = Buffer.from(value, "utf8");
      const length = Buffer.alloc(2);
      length.writeUInt16BE(encoded.length);
      chunks.push(length, encoded);
      break;
    }
    case TAG.LIST: {
      const header = Buffer.alloc(5);
      header.writeUInt8(value.elementType, 0);
      header.writeInt32BE(value.items.length, 1);
      chunks.push(header);
      for (const item of value.items) {
        writePayload(chunks, value.elementType, item);
      }
      break;
    }
    case TAG.COMPOUND: {
      for (const [name, entry] of value) {
        chunks.push(Buffer.from([entry.type]));
        const encoded = Buffer.from(name, "utf8");
        const length = Buffer.alloc(2);
        length.writeUInt16BE(encoded.length);
        chunks.push(length, encoded);
        writePayload(chunks, entry.type, entry.value);
      }
      chunks.push(Buffer.from([TAG.END]));
      break;
    }
    case TAG.INT_ARRAY: {
      const length = Buffer.alloc(4);
      length.writeInt32BE(value.length);
      chunks.push(length);
      for (const item of value) {
        const buffer = Buffer.alloc(4);
        buffer.writeInt32BE(item);
        chunks.push(buffer);
      }
      break;
    }
    case TAG.LONG_ARRAY: {
      const length = Buffer.alloc(4);
      length.writeInt32BE(value.length);
      chunks.push(length);
      for (const item of value) {
        const buffer = Buffer.alloc(8);
        buffer.writeBigInt64BE(item);
        chunks.push(buffer);
      }
      break;
    }
    default:
      throw new Error(`unknown NBT tag type ${type}`);
  }
};

const readNbt = (buffer) => {
  const type = buffer.readUInt8(0);
  const nameLength = buffer.readUInt16BE(1);
  const name = buffer.toString("utf8", 3, 3 + nameLength);
  const read = readPayload(buffer, type, 3 + nameLength);
  return { type, name, value: read.value };
};

const writeNbt = (root) => {
  const chunks = [Buffer.from([root.type])];
  const encoded = Buffer.from(root.name, "utf8");
  const length = Buffer.alloc(2);
  length.writeUInt16BE(encoded.length);
  chunks.push(length, encoded);
  writePayload(chunks, root.type, root.value);
  return Buffer.concat(chunks);
};

// ---------------------------------------------------------------------------- rewrite

const registeredIds = new Set(
  [...BLOCKS_SOURCE.matchAll(/register\(\s*"([a-z0-9_]+)"/g)].map((match) => match[1])
);
const legacyIds = new Set(
  (() => {
    const start = BLOCKS_SOURCE.indexOf("registerLegacyBlocks()");
    const body = BLOCKS_SOURCE.slice(start, BLOCKS_SOURCE.indexOf("};", start));
    return [...body.matchAll(/"([a-z0-9_]+)"/g)].map((match) => match[1]);
  })()
);

const isKnown = (id) => registeredIds.has(id) || legacyIds.has(id);

/** Rewrites every string payload that still names the original namespace. */
const rewriteStrings = (type, value, renames) => {
  if (type === TAG.STRING) {
    if (value.startsWith("srparasites:")) {
      renames.push(value);
      return `csrp:${value.slice("srparasites:".length)}`;
    }
    return value;
  }
  if (type === TAG.COMPOUND) {
    for (const entry of value.values()) {
      entry.value = rewriteStrings(entry.type, entry.value, renames);
    }
    return value;
  }
  if (type === TAG.LIST) {
    value.items = value.items.map((item) => rewriteStrings(value.elementType, item, renames));
    return value;
  }
  return value;
};

let changedFiles = 0;
const unknown = new Map();
for (const file of fs.readdirSync(STRUCTURE_DIR).filter((name) => name.endsWith(".nbt")).sort()) {
  const full = path.join(STRUCTURE_DIR, file);
  const raw = fs.readFileSync(full);
  const root = readNbt(zlib.gunzipSync(raw));
  const renames = [];
  root.value = rewriteStrings(root.type, root.value, renames);
  if (!renames.length) {
    continue;
  }
  changedFiles += 1;
  const unique = [...new Set(renames)];
  console.log(`${file}: ${renames.length} references -> csrp:`);
  console.log(`   ${unique.slice(0, 12).join(" ")}${unique.length > 12 ? ` … (+${unique.length - 12})` : ""}`);
  for (const value of unique) {
    const local = value.slice("srparasites:".length);
    if (!isKnown(local)) {
      unknown.set(local, file);
    }
  }
  if (!DRY) {
    fs.writeFileSync(full, zlib.gzipSync(writeNbt(root)));
  }
}

console.log(`\n${DRY ? "[dry] would rewrite" : "rewrote"} ${changedFiles} structure files`);
if (unknown.size) {
  console.log(`palette ids with no matching block in ModBlocks (${unknown.size}):`);
  for (const [id, file] of unknown) {
    console.log(`   ${id}  (${file})`);
  }
}
