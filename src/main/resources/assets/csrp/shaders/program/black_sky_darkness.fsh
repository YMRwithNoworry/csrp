#version 120

uniform sampler2D DiffuseSampler;
varying vec2 texCoord;

uniform vec2 InSize;
uniform float SRP_Time;
uniform float Darkness;

float saturate(float x) {
    return clamp(x, 0.0, 1.0);
}

void main() {
    vec2 uv = texCoord;
    vec4 scene = texture2D(DiffuseSampler, uv);

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

    gl_FragColor = vec4(color, scene.a);
}