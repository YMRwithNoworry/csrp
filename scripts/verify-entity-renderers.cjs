const fs = require("node:fs");
const path = require("node:path");

// MC 26.3 has no fallback renderer for an unregistered entity type:
// EntityRenderDispatcher.getRenderer(entity) is a plain map lookup and extractEntity() dereferences the
// result, so any entity type that reaches the client without a renderer crashes it.  Every id this mod
// registers must therefore be covered here.
const root = path.resolve(__dirname, "..");
const failures = [];

const entitiesPath = path.join(root, "src/main/java/alku/csrp/registry/ModEntities.java");
const clientPath = path.join(root, "src/main/java/alku/csrp/client/ClientModEvents.java");
const entities = fs.readFileSync(entitiesPath, "utf8");
const client = fs.readFileSync(clientPath, "utf8");

const ids = new Set();
for (const pattern of [/(?:monster|projectile|misc|entity|register)\s*\(\s*"([a-z0-9_]+)"/g,
    /entityType\s*\(\s*"([a-z0-9_]+)"/g]) {
    let match;
    while ((match = pattern.exec(entities))) ids.add(match[1]);
}

// field -> id, tolerant of multi-line declarations.
const fieldToId = new Map();
let match;
const declaration = /([A-Z][A-Z0-9_]*)\s*=\s*[^;]*?"([a-z0-9_]+)"/gs;
while ((match = declaration.exec(entities))) {
    if (ids.has(match[2]) && !fieldToId.has(match[1])) fieldToId.set(match[1], match[2]);
}

const renderedFields = new Set();
const registration = /registerEntityRenderer\(\s*ModEntities\.([A-Z0-9_]+)/g;
while ((match = registration.exec(client))) renderedFields.add(match[1]);

const renderedIds = new Set();
for (const field of renderedFields) {
    const id = fieldToId.get(field);
    if (id) renderedIds.add(id);
    else failures.push(`ClientModEvents registers a renderer for ModEntities.${field}, which is not a registered entity field`);
}

if (ids.size === 0) failures.push("no entity ids were found in ModEntities.java");
const missing = [...ids].filter((id) => !renderedIds.has(id)).sort();
if (missing.length > 0) {
    failures.push(`entity ids without a renderer (${missing.length}): ${missing.join(", ")}`);
}

if (failures.length) {
    for (const failure of failures) console.error(failure);
    process.exit(1);
}
console.log(`entity renderer coverage ok (${renderedIds.size}/${ids.size} ids)`);
