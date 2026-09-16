#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D DiffuseSampler;

layout(std140) uniform VhsConfig {
    float Time;
    float Intensity;
    float ScanReduction;
    float VignetteStrength;
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

float random(vec2 uv) {
    return fract(sin(dot(uv, vec2(12.9898, 78.233))) * 43758.5453123);
}

void main() {
    vec4 screen = texture(DiffuseSampler, texCoord);
    vec2 noiseUv = texCoord * (Time + 0.37);
    float tapeNoise = random(noiseUv + vec2(Time * 0.017, Time * 0.031));

    float scanlineFine = sin((texCoord.y + Time) * 500.0) * ScanReduction - ScanReduction;
    float scanlineCoarse = sin((texCoord.y + Time * 0.4) * 25.0)
            * ScanReduction - ScanReduction;

    vec2 centered = abs(texCoord - vec2(0.5)) * 2.0;
    float vignette = smoothstep(0.35, 1.35, length(centered)) * VignetteStrength;

    vec3 tapeColor = screen.rgb * mix(0.72, 1.18, tapeNoise);
    tapeColor += vec3(scanlineFine + scanlineCoarse);
    tapeColor *= 1.0 - vignette;

    fragColor = vec4(mix(screen.rgb, clamp(tapeColor, 0.0, 1.0), Intensity), screen.a);
}
