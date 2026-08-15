#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;

out vec4 fragColor;

uniform vec2 InSize;
uniform float SRP_Time;
uniform float SRP_Exposure;
uniform float SRP_Fade;

float saturate(float x) {
    return clamp(x, 0.0, 1.0);
}

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
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
    p *= 2.02;
    a *= 0.5;

    v += noise(p) * a;
    p *= 2.07;
    a *= 0.5;

    v += noise(p) * a;
    p *= 2.11;
    a *= 0.5;

    v += noise(p) * a;

    return v;
}

void main() {
    vec2 uv = texCoord;
    float t = SRP_Time;
    float exposure = saturate(SRP_Exposure) * saturate(SRP_Fade);

    vec2 centered = uv * 2.0 - 1.0;
    float oval = length(vec2(centered.x * 0.72, centered.y * 1.18));

    float clearCenter = 1.0 - smoothstep(0.30, 0.62, oval);
    float edgeMask = smoothstep(0.28, 1.02, oval);
    float cornerMask = smoothstep(0.70, 1.30, length(centered));

    float lowerHeat = smoothstep(1.00, 0.10, uv.y);
    float midHeat = smoothstep(0.92, 0.18, uv.y) * 0.55;

    float heatMask = saturate(edgeMask * 0.90 + cornerMask * 0.60);
    heatMask *= saturate(lowerHeat + midHeat);
    heatMask *= (1.0 - clearCenter);
    heatMask *= exposure;

    float n1 = fbm(vec2(uv.x * 6.0,  uv.y * 8.0  - t * 0.35));
    float n2 = fbm(vec2(uv.x * 13.0, uv.y * 17.0 - t * 0.62));
    float n3 = fbm(vec2(uv.x * 26.0, uv.y * 33.0 - t * 1.00));

    float band1 = sin(uv.x * 28.0 + n1 * 5.0 + t * 0.20);
    float band2 = sin(uv.x * 54.0 + n2 * 7.0 - t * 0.10);
    float band3 = sin(uv.x * 92.0 + n3 * 8.0 + t * 0.08);

    float shimmer = band1 * 0.55 + band2 * 0.28 + band3 * 0.17 + (n3 - 0.5) * 0.45;
    float cross = sin(uv.y * 16.0 - t * 0.14 + n2 * 2.8);

    float pulse = 0.90 + 0.10 * sin(t * 0.80);
    float strength = 0.0068 * heatMask * pulse;

    vec2 warped = uv;
    warped.y += shimmer * strength;
    warped.x += cross * strength * 0.22;
    warped = clamp(warped, 0.001, 0.999);

    vec4 scene = texture(DiffuseSampler, warped);

    vec3 warmTint = vec3(1.085, 1.020, 0.900);
    vec3 dustyHeat = vec3(1.000, 0.900, 0.700);

    scene.rgb = mix(scene.rgb, scene.rgb * warmTint, 0.090 * exposure);
    scene.rgb = mix(scene.rgb, dustyHeat, heatMask * 0.045);

    float glow = 0.022 * heatMask;
    scene.rgb += vec3(glow, glow * 0.70, glow * 0.30);

    fragColor = vec4(scene.rgb, 1.0);
}