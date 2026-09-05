#!/usr/bin/env node
/** Convert the extracted Bedrock geometry intermediary to Citadel's native model.json. */
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const root = path.resolve(__dirname, '..');
const sourceDir = path.join(root, 'src/main/resources/assets/csrp/geo');
const outputDir = path.join(root, 'build/generated/tabula-json');
fs.rmSync(outputDir, { recursive: true, force: true });
fs.mkdirSync(outputDir, { recursive: true });

function vector(object, key) {
  const value = object[key];
  return Array.isArray(value) ? value.map(Number) : [0, 0, 0];
}
function textureOffset(cube, size) {
  const uv = cube.uv;
  if (Array.isArray(uv)) return [Math.round(uv[0]), Math.round(uv[1])];
  if (!uv || typeof uv !== 'object') return [0, 0];
  const face = uv.west || Object.values(uv)[0];
  if (!face || !Array.isArray(face.uv)) return [0, 0];
  return [Math.round(face.uv[0]), Math.round(face.uv[1] - size[2])];
}
function idFor(file, name, index) {
  return crypto.createHash('sha1').update(`${file}:${name}:${index}`).digest('hex').slice(0, 24);
}
function convert(file) {
  const id = path.basename(file, '.geo.json');
  const root = JSON.parse(fs.readFileSync(file, 'utf8'))['minecraft:geometry'][0];
  const bones = root.bones || [];
  const byName = new Map(bones.map(b => [b.name, b]));
  const children = new Map(bones.map(b => [b.name, []]));
  for (const bone of bones) if (bone.parent && children.has(bone.parent)) children.get(bone.parent).push(bone);
  const roots = bones.filter(b => !b.parent || !byName.has(b.parent));
  let cubeCount = 0;
  function convertBone(bone, index) {
    const pivot = vector(bone, 'pivot');
    const parent = bone.parent && byName.has(bone.parent) ? byName.get(bone.parent) : null;
    const parentPivot = parent ? vector(parent, 'pivot') : [0, 24, 0];
    const rotation = vector(bone, 'rotation');
    const isCoordinateRoot = bone.name === 'srp_coordinate_root';
    const result = {
      name: bone.name,
      dimensions: [1, 1, 1],
      position: isCoordinateRoot ? [0, 24, 0] : pivot.map((v, i) => -(v - parentPivot[i])),
      offset: [0, 0, 0],
      rotation: isCoordinateRoot ? [0, 0, 0] : [-rotation[0], rotation[1], -rotation[2]],
      scale: [1, 1, 1],
      txOffset: [0, 0],
      txMirror: Boolean(bone.mirror),
      mcScale: 0,
      opacity: 100,
      hidden: Boolean(bone.never_render),
      metadata: [],
      children: [],
      identifier: idFor(id, bone.name, index)
    };
    const cubes = bone.cubes || [];
    if (cubes.length) {
      // Tabula's container is one cube per node. Keep the first box on the
      // bone and represent additional boxes as sibling child nodes.
      const makeCube = (cube, cubeIndex) => {
        cubeCount++;
        const origin = vector(cube, 'origin');
        const size = vector(cube, 'size');
        const offset = origin.map((v, i) => -(v - pivot[i] + size[i]));
        return {
          name: cubes.length === 1 ? bone.name : `${bone.name}_${cubeIndex}`,
          dimensions: size.map(v => Math.round(v)),
          position: result.position,
          offset,
          rotation: result.rotation,
          scale: [1, 1, 1],
          txOffset: textureOffset(cube, size),
          txMirror: cube.mirror == null ? Boolean(bone.mirror) : Boolean(cube.mirror),
          mcScale: Number(cube.inflate || 0),
          opacity: 100,
          hidden: Boolean(bone.never_render),
          metadata: [],
          children: [],
          identifier: idFor(id, bone.name, `${index}-${cubeIndex}`)
        };
      };
      const boxes = cubes.map(makeCube);
      Object.assign(result, boxes[0]);
      result.name = boxes[0].name;
      result.children = boxes[0].children;
      for (const extra of boxes.slice(1)) result.children.push(extra);
    } else {
      // A no-cube bone is retained as a 1x1x1 hidden node so animation targets
      // and parent transforms remain addressable by name.
      result.hidden = true;
    }
    const childBones = children.get(bone.name) || [];
    for (let i = 0; i < childBones.length; i++) result.children.push(convertBone(childBones[i], `${index}.${i}`));
    return result;
  }
  const cubes = roots.map((bone, i) => convertBone(bone, i));
  const description = root.description || {};
  const model = {
    modelName: `csrp_${id}`,
    authorName: 'SRParasites 1.10.8 Tabula export',
    projVersion: 4,
    metadata: [],
    textureWidth: Number(description.texture_width || 64),
    textureHeight: Number(description.texture_height || 32),
    scale: [1, 1, 1],
    cubeGroups: [],
    cubes,
    anims: [],
    cubeCount
  };
  fs.writeFileSync(path.join(outputDir, `${id}.json`), JSON.stringify(model));
}
for (const file of fs.readdirSync(sourceDir).filter(f => f.endsWith('.geo.json')).sort()) convert(path.join(sourceDir, file));
console.log(`Converted ${fs.readdirSync(outputDir).length} Tabula models to ${outputDir}`);

// Package the generated model.json together with the exact source animation
// transcription. Citadel reads model.json; the runtime animation library reads
// animations.json from the same archive so geometry and animation cannot drift.
const { spawnSync } = require('child_process');
const archiveDir = path.join(root, 'src/main/resources/assets/csrp/tabula');
const stagingDir = path.join(root, 'build/generated/tabula-archives');
fs.rmSync(archiveDir, { recursive: true, force: true });
fs.rmSync(stagingDir, { recursive: true, force: true });
fs.mkdirSync(archiveDir, { recursive: true });
fs.mkdirSync(stagingDir, { recursive: true });
const animationDir = path.join(root, 'src/main/resources/assets/csrp/animations');
const jar = process.env.JAVA_HOME
  ? path.join(process.env.JAVA_HOME, 'bin', process.platform === 'win32' ? 'jar.exe' : 'jar')
  : (process.platform === 'win32' ? 'jar.exe' : 'jar');
for (const file of fs.readdirSync(outputDir).filter(f => f.endsWith('.json')).sort()) {
  const id = path.basename(file, '.json');
  const stage = path.join(stagingDir, id);
  fs.mkdirSync(stage, { recursive: true });
  fs.copyFileSync(path.join(outputDir, file), path.join(stage, 'model.json'));
  const sharedAnimations = {
    biomass_pod: 'biomass.animation.json',
    biomass_venkrol: 'biomass.animation.json',
    tendril_canra: 'tendril_static.animation.json',
    tendril_dragonelw: 'tendril_static.animation.json',
    tendril_dragonerw: 'tendril_static.animation.json'
  };
  const animation = path.join(animationDir, sharedAnimations[id] || `${id}.animation.json`);
  const args = ['cf', path.join(archiveDir, `${id}.tbl`), 'model.json'];
  if (fs.existsSync(animation)) {
    fs.copyFileSync(animation, path.join(stage, 'animations.json'));
    args.push('animations.json');
  }
  const result = spawnSync(jar, args, { cwd: stage, stdio: 'inherit' });
  if (result.status !== 0) throw new Error(`jar failed for ${id}`);
}
// Shared animation IDs are used by model sets whose geometry has several variants.
// Alias archives keep every runtime animation lookup on the same Citadel path.
for (const [alias, base] of Object.entries({ biomass: 'biomass_pod', tendril_static: 'tendril_canra' })) {
  fs.copyFileSync(path.join(archiveDir, base + '.tbl'), path.join(archiveDir, alias + '.tbl'));
}
console.log(`Packaged ${fs.readdirSync(archiveDir).length} Citadel .tbl resources`);
