#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float Time;

in vec2 texCoord;

out vec4 fragColor;

float saturate(float value) {
    return clamp(value, 0.0, 1.0);
}

float hash1(float value) {
    return fract(sin(value * 127.1) * 43758.5453123);
}

float hash2(vec2 value) {
    return fract(sin(dot(value, vec2(127.1, 311.7))) * 43758.5453123);
}

float smoothNoise1(float value) {
    float whole = floor(value);
    float fraction = fract(value);
    fraction = fraction * fraction * (3.0 - 2.0 * fraction);
    return mix(hash1(whole), hash1(whole + 1.0), fraction);
}

void main() {
    vec2 baseUv = texCoord;
    float time = Time * 20.0;
    vec2 centered = baseUv * 2.0 - 1.0;
    float radius = length(centered);

    float edge = smoothstep(0.35, 1.05, radius);
    edge *= edge;
    edge = saturate(edge + smoothstep(0.85, 1.25, radius) * 0.25);

    float columnA = smoothNoise1(baseUv.x * 10.0);
    float columnB = smoothNoise1(baseUv.x * 22.0 + 7.3);
    float streak = saturate(columnA * 0.70 + columnB * 0.60);
    streak = streak * streak * streak;

    float wave1 = sin(time * 1.37 + baseUv.x * 4.0);
    float wave2 = sin(time * 0.83 + baseUv.x * 9.0 + baseUv.y * 2.0);
    float wave3 = sin(time * 1.91 + baseUv.x * 2.0);
    float animation = (wave1 * 0.45 + wave2 * 0.35 + wave3 * 0.20) * 0.5 + 0.5;
    float flutter = sin(time * 2.17 + hash2(baseUv * 50.0) * 6.2831853) * 0.5 + 0.5;

    float pull = (0.06 + streak * 0.32) * edge;
    pull *= 0.75 + 0.25 * animation;
    pull *= 0.90 + 0.10 * flutter;

    vec2 warpedUv = baseUv;
    warpedUv.x += sin(time * 1.11 + baseUv.y * 8.0) * 0.010
            * edge * (0.30 + 0.70 * streak);
    float lens = sin(time * 0.65 + radius * 6.0) * 0.006;
    warpedUv += normalize(centered + 0.0001) * lens * edge;
    warpedUv.y = clamp(baseUv.y - pull, 0.0, 1.0);
    warpedUv = clamp(warpedUv, 0.0, 1.0);

    vec2 quantization = InSize * 10.0;
    vec2 quantizedUv = floor(warpedUv * quantization) / quantization;
    float stabilization = (0.012 + streak * 0.020) * edge;
    stabilization *= 1.0 - (1.0 - edge) * 0.85;
    vec2 finalUv = mix(warpedUv, quantizedUv, saturate(stabilization));

    vec4 scene = texture(DiffuseSampler, finalUv);
    scene.rgb *= 1.0 - streak * edge * 0.09;
    fragColor = vec4(scene.rgb, scene.a);
}
