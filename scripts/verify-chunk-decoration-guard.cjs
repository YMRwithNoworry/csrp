#!/usr/bin/env node
/**
 * Regression guard for the 2026-09-25 crash report (exit code 1 while the world was generating).
 *
 * Root cause: the cold-star decoration passes ran from ChunkEvent.Load and queried blocks in the
 * neighbouring chunk. `Level#getBlockState` / `getHeightmapPos` / `setBlock` resolve their chunk
 * with `requireChunk = true`, so that synchronously loaded the neighbour, which posted
 * ChunkEvent.Load again, which ran the handler inline (MinecraftServer#execute executes immediately
 * on the server thread) and re-entered the same pass:
 *
 *   ColdStarTreeHandler.findNormalTreePosition
 *     -> Level.getHeightmapPos -> ChunkMap -> ChunkEvent.Load
 *     -> StarBiomeGenerationEvents.convertNewChunk -> ColdStarTreeHandler.decorate -> ...
 *
 * The recursion is unbounded and ends in a StackOverflowError. Every decoration pass must therefore
 * only touch chunks that are already loaded, which is what GenerationChunkGuard encodes.
 */
'use strict';

const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '..');
const failures = [];
const read = (file) => {
    const full = path.join(root, file);
    if (!fs.existsSync(full)) {
        failures.push(`missing ${file}`);
        return '';
    }
    return fs.readFileSync(full, 'utf8');
};
const expect = (text, pattern, message) => {
    if (!pattern.test(text)) failures.push(message);
};

const guard = read('src/main/java/alku/csrp/world/GenerationChunkGuard.java');
const events = read('src/main/java/alku/csrp/world/StarBiomeGenerationEvents.java');
const trees = read('src/main/java/alku/csrp/world/ColdStarTreeHandler.java');
const balls = read('src/main/java/alku/csrp/world/ParasiteBallPlacer.java');
const village = read('src/main/java/alku/csrp/world/ColdStarVillageGenerator.java');
const placer = read('src/main/java/alku/csrp/world/DeadheadTreePlacer.java');
const fracture = read('src/main/java/alku/csrp/world/FracturedTerrainHandler.java');

// The guard helper itself must use the non-loading check.
expect(guard, /level\.hasChunk\(x >> 4, z >> 4\)/,
    'GenerationChunkGuard.isLoaded must use Level#hasChunk, which does not force a load');
expect(guard, /level\.hasChunksAt\(min, max\)/,
    'GenerationChunkGuard.isLoadedArea must use Level#hasChunksAt, which does not force a load');

// Tree pass: the sampled column and the snow radius must be guarded.
expect(trees, /GenerationChunkGuard\.isLoaded\(level, chunkOrigin\.getX\(\) \+ x, chunkOrigin\.getZ\(\) \+ z\)/,
    'the cold-star tree pass may sample a column in an unloaded neighbouring chunk again');
expect(trees, /addSnowUnderTree[\s\S]{0,900}?GenerationChunkGuard\.isLoaded\(level, centerX \+ dx, centerZ \+ dz\)/,
    'the tree snow radius must skip columns whose chunk is not loaded');

// Ball pass: the surface probe and the structure footprint must be guarded.
expect(balls, /surfaceAt\(ServerLevel level, int x, int z\)\s*\{\s*if \(!GenerationChunkGuard\.isLoaded\(level, x, z\)\)/,
    'ParasiteBallPlacer#surfaceAt must not force-load the probed chunk');
expect(balls, /hasLoadedFootprint\(level, surface, BALL_ANCHOR\)/,
    'the small ball must check its structure footprint before placing');
expect(balls, /hasLoadedFootprint\(level, surface, BIG_BALL_ANCHOR\)/,
    'the big ball must check its structure footprint before placing');
expect(balls, /GenerationChunkGuard\.isLoadedArea\(level, min, max\)/,
    'ParasiteBallPlacer#hasLoadedFootprint must use the loaded-area guard');

// Village: the whole footprint (well, houses and wall) must be loaded first.
expect(village, /GenerationChunkGuard\.isLoadedArea\(level, footprintMin, footprintMax\)/,
    'the cold-star village must check its whole footprint before building');
expect(village, /WALL_HALF_X = 25[\s\S]{0,120}WALL_HALF_Z = 20/,
    'the village footprint must be derived from the real wall half extents');

// The structure placer already had its own guard; keep it.
expect(placer, /hasChunksAt\(min, max\)/,
    'DeadheadTreePlacer#place must keep its loaded-chunk guard before touching the template');

// The fractured-terrain pass must stay chunk-local (it may only use the event chunk).
if (/level\.(getBlockState|setBlock|getHeightmapPos|getHeight)\(/.test(fracture)) {
    failures.push('FracturedTerrainHandler must only touch the event chunk, never Level block accessors');
}
expect(fracture, /chunk\.setBlockState\(/,
    'FracturedTerrainHandler must keep writing through the event chunk');

// Every decoration pass must still be driven from the chunk-load handler.
expect(events, /ColdStarTreeHandler\.decorate\(level, chunkX, chunkZ\)/,
    'the cold-star tree pass is no longer driven from the chunk-load handler');
expect(events, /ParasiteBallPlacer\.decorate\(level, chunkX, chunkZ\)/,
    'the parasite ball pass is no longer driven from the chunk-load handler');
expect(events, /ColdStarVillageGenerator\.generate\(level, chunkX, chunkZ\)/,
    'the cold-star village is no longer driven from the chunk-load handler');

if (failures.length) {
    console.error('Chunk decoration guard verification failed:');
    failures.forEach((failure) => console.error(`- ${failure}`));
    process.exit(1);
}
console.log('Chunk decoration guard verification passed.');
