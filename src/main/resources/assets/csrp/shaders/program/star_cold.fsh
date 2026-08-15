#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;

out vec4 fragColor;

uniform vec2 InSize;
uniform float SRP_Time;
uniform float SRP_Exposure;
uniform float SRP_Fade;
uniform float SRP_HandLight;

float saturate(float x) {
    return clamp(x, 0.0, 1.0);
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(157.1, 421.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);

    f = f * f * (3.0 - 2.0 * f);

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;

    v += noise(p) * a;
    p *= 2.03;
    a *= 0.5;

    v += noise(p) * a;
    p *= 2.11;
    a *= 0.5;

    v += noise(p) * a;
    p *= 2.17;
    a *= 0.5;

    v += noise(p) * a;

    return v;
}

void main() {
    vec2 uv = texCoord;
    float t = SRP_Time;
    float exposure = saturate(SRP_Exposure) * saturate(SRP_Fade);
    float handLight = saturate(SRP_HandLight);

    vec2 centered = uv * 2.0 - 1.0;
    float dist = length(centered);

    float edgeFog = smoothstep(0.28, 1.05, dist);
    float cornerFog = smoothstep(0.75, 1.35, dist);
    float bottomFog = smoothstep(1.00, 0.10, uv.y) * 0.75;

    float ovalDist = length(vec2(centered.x * 0.78, (centered.y + 0.28) * 1.16));

    float clearCenter = 1.0 - smoothstep(0.22, 0.58, ovalDist);


    float lightProtection = 1.0 - handLight * 0.95;

    float ovalStrength = clearCenter * mix(1.0, 0.35, handLight);

    float fogAllowed = 1.0 - ovalStrength;
    fogAllowed = mix(0.06, 1.0, fogAllowed);

    vec2 p1 = vec2(uv.x * 2.3 + t * 0.020, uv.y * 3.2 - t * 0.010);
    vec2 p2 = vec2(uv.x * 5.1 - t * 0.034, uv.y * 6.8 + t * 0.018);
    vec2 p3 = vec2(uv.x * 10.5 + t * 0.060, uv.y * 13.8 - t * 0.040);
    vec2 p4 = vec2(uv.x * 19.0 - t * 0.095, uv.y * 22.0 + t * 0.055);

    float farFog  = fbm(p1);
    float midFog  = fbm(p2);
    float nearFog = fbm(p3);
    float frost   = fbm(p4);

    float fogBanks = 0.0;
    fogBanks += smoothstep(0.25, 0.82, farFog)  * 0.28;
    fogBanks += smoothstep(0.35, 0.88, midFog)  * 0.34;
    fogBanks += smoothstep(0.45, 0.94, nearFog) * 0.30;

    float drifting = sin((uv.x + midFog * 0.22) * 9.0 + t * 0.35) * 0.5 + 0.5;
    float frostStreaks = smoothstep(0.62, 0.92, frost) * 0.14;

    float fogMask = 0.0;
    fogMask += edgeFog * 0.32;
    fogMask += cornerFog * 0.16;
    fogMask += bottomFog * 0.22;
    fogMask += fogBanks * 0.50;
    fogMask += drifting * 0.10;
    fogMask += frostStreaks;

    fogMask = saturate(fogMask);
    fogMask *= fogAllowed;
    fogMask *= lightProtection;
    fogMask *= exposure;

    vec2 warp = uv;
    warp.x += (nearFog - 0.5) * 0.0024 * fogAllowed * exposure;
    warp.y += (midFog - 0.5) * 0.0015 * fogAllowed * exposure;
    warp = clamp(warp, 0.001, 0.999);

    vec4 scene = texture(DiffuseSampler, warp);

    float gray = dot(scene.rgb, vec3(0.299, 0.587, 0.114));
    vec3 finalRgb = mix(scene.rgb, vec3(gray), 0.30 * exposure);

    vec3 coldBlue = vec3(0.58, 0.75, 1.00);
    vec3 paleMist = vec3(0.82, 0.92, 1.00);
    vec3 frostWhite = vec3(0.93, 0.97, 1.00);

    finalRgb = mix(finalRgb, coldBlue, fogMask * 0.28);
    finalRgb = mix(finalRgb, paleMist, fogMask * 0.24);
    finalRgb = mix(finalRgb, frostWhite, frostStreaks * fogAllowed * exposure * 0.16);

    finalRgb.b += 0.060 * exposure;
    finalRgb.g += 0.018 * exposure;
    finalRgb.r -= 0.024 * exposure;

    float clarityBoost = ovalStrength * exposure * (0.65 + handLight * 0.35);
    finalRgb = mix(finalRgb, scene.rgb, clarityBoost * 0.55);
    finalRgb += vec3(0.010, 0.014, 0.020) * clarityBoost;

    float vignette = smoothstep(0.40, 1.20, dist) * exposure;
    finalRgb = mix(finalRgb, finalRgb * vec3(0.90, 0.95, 1.07), vignette * 0.16);

    fragColor = vec4(finalRgb, 1.0);
}