#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D DiffuseSampler;

layout(std140) uniform DarknessConfig {
    float SRP_Time;
    float Darkness;
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

float saturate(float x) {
    return clamp(x, 0.0, 1.0);
}

void main() {
    vec2 uv = texCoord;
    vec4 scene = texture(DiffuseSampler, uv);

    vec2 p = uv * 2.0 - 1.0;
    float r = length(p);

    // soft edge darkening, not a hard overlay
    float vignette = smoothstep(0.30, 1.15, r);

    // very slow breathing so it feels unnatural, but not annoying
    float pulse = sin(SRP_Time * 0.65) * 0.5 + 0.5;

    float edgeDark = vignette * (0.20 + pulse * 0.08);
    float globalDark = Darkness;

    vec3 color = scene.rgb;

    // desaturate slightly
    float gray = dot(color, vec3(0.299, 0.587, 0.114));
    color = mix(color, vec3(gray), 0.18);

    // darken but keep visibility
    color *= (1.0 - globalDark);
    color *= (1.0 - edgeDark);

    // subtle cold/dead tint
    color *= vec3(0.82, 0.88, 1.0);

    fragColor = vec4(color, scene.a);
}
