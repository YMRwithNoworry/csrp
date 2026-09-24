const fs = require("node:fs");

const read = (path) => fs.readFileSync(path, "utf8");
const failures = [];
const expect = (condition, message) => {
  if (!condition) failures.push(message);
};

const fog = read("src/main/java/alku/csrp/client/weather/SRPBlizzardFogRenderer.java");
const snow = read("src/main/java/alku/csrp/client/weather/SRPBlizzardRenderer.java");

// --- fog shells: 1.12.2 Tessellator -> 26.3 submitCustomGeometry ------------------------
for (const token of ["SHELL_COUNT = 8", "LONGITUDE_SEGMENTS = 32", "LATITUDE_SEGMENTS = 14",
  "SubmitCustomGeometryEvent", "RenderTypes.debugQuads()", "MAX_SHELL_ALPHA = 0.22F",
  "0.02F + strength * 0.14F", "waveA * waveB * 0.018"])
  expect(fog.includes(token), `SRPBlizzardFogRenderer is missing original shell geometry: ${token}`);
for (const token of ["lerp(14.0, 5.5, strength)", "lerp(48.0, 20.0, strength)",
  "lerp(0.78F, 0.055F, blackBlend)", "lerp(0.82F, 0.06F, blackBlend)",
  "lerp(0.86F, 0.07F, blackBlend)", "* 0.22F"])
  expect(fog.includes(token), `SRPBlizzardFogRenderer lost original fog tuning: ${token}`);
expect(fog.includes("smoothStep(intensity)") && /value \* value \* \(3\.0F - 2\.0F \* value\)/.test(fog),
  "SRPBlizzardFogRenderer no longer smooth-steps the blizzard intensity");
// The OptiFine-only gate had to be re-pointed at the shader APIs 26.3 can actually run.
expect(fog.includes("net.optifine.shaders.Shaders") && fog.includes("net.irisshaders.iris.Iris")
  && fog.includes("isPackInUseQuick"),
  "SRPBlizzardFogRenderer lost the shader-pack gate (OptiFine probe + Iris fallback)");
expect(!fog.includes("net.minecraft.client.shader") && !fog.includes("ShaderGroup"),
  "SRPBlizzardFogRenderer still depends on the removed 1.12.2 ShaderGroup pipeline");

// --- setupFog mixin -> ViewportEvent.RenderFog ------------------------------------------
for (const token of ["ViewportEvent.RenderFog", "FogType.ATMOSPHERIC", "72.0F - intensity * 56.0F",
  "fogEnd * 0.08F", "setNearPlaneDistance", "setFarPlaneDistance"])
  expect(fog.includes(token), `the 1.12.2 setupFog override is missing: ${token}`);
// MixinRenderGlobalBlizzardSky (hide sunrise/sunset colours) -> fog-colour wash out.
for (const token of ["ViewportEvent.ComputeFogColor", "setRed", "setGreen", "setBlue",
  "Mth.lerp(blend, event.getRed()"])
  expect(fog.includes(token), `the 1.12.2 blizzard sky override is missing: ${token}`);

// --- snow streaks -----------------------------------------------------------------------
for (const token of ["textures/environment/snow.png", "SubmitCustomGeometryEvent",
  "RenderTypes.entityTranslucent(SNOW_TEXTURE)", "Heightmap.Types.MOTION_BLOCKING",
  "MAX_STREAKS = 1500", "SRPBlizzardDirectionClient.getMotionPhase(partialTicks)"])
  expect(snow.includes(token), `SRPBlizzardRenderer is missing original snow behaviour: ${token}`);
for (const token of ["time * 0.0012", "Math.sin(time * 0.065)", "Math.sin(time * 0.017)",
  "(0.8 + intensity * 2.2)", "0.16 + intensity * 0.24", "0.34F + distanceFade * 0.58F",
  "0.11 + randomB * 0.12 + intensity * 0.03", "intensity > 0.7F ? 3 : 2",
  "intensity > 0.55F ? 1 : 2", "8 + Mth.floor(intensity * 6.0F)"])
  expect(snow.includes(token), `SRPBlizzardRenderer lost original streak tuning: ${token}`);
expect(/hash01\(int x, int z, int salt\)[\s\S]{0,240}341873128712L[\s\S]{0,240}1274126177L/.test(snow),
  "SRPBlizzardRenderer no longer uses the original per-column hash");
// The 1.12.2 draw ran with culling disabled, so every streak has to be submitted twice.
const streakVertexCalls = (snow.match(/vertex\(buffer, pose,/g) || []).length;
expect(streakVertexCalls === 8,
  `snow streaks must be submitted double-sided, found ${streakVertexCalls} vertex calls instead of 8`);
expect(snow.includes("player.getEyeY()") === false
  && snow.includes("Minecraft.getInstance().getCameraEntity()") === false
  && snow.includes("minecraft.getCameraEntity()"),
  "SRPBlizzardRenderer must gate rendering on the camera entity");

if (failures.length) {
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}
console.log("Verified 26.3 blizzard fog shells, fog distance/colour overrides and wind-driven snow streaks.");
