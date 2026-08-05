#version 150

// Port of "Aurora Sky Shader" (Godot sky shader):
// https://godotshaders.com/shader/aurora-sky-shader/
// Based on https://www.shadertoy.com/view/XtGGRt

uniform sampler2D ColorGradient;
uniform float AuroraTime;
uniform float Brightness;
uniform float Speed;
uniform float Height;
uniform float Scale;

in vec3 localPos;

out vec4 fragColor;

const float resolution_loop_count = 30.0;

mat2 mm2(float a) {
    float c = cos(a);
    float s = sin(a);
    return mat2(vec2(c, s), vec2(-s, c));
}

float tri(float x) {
    return clamp(abs(fract(x) - 0.5), 0.01, 0.49);
}

vec2 tri2(vec2 p) {
    return vec2(tri(p.x) + tri(p.y), tri(p.y + tri(p.x)));
}

float trinoise2d(vec2 p, float t) {
    float z = 1.8;
    float z2 = 2.5;
    float rz = 0.0;

    p *= mm2(p.x * 0.06);
    vec2 bp = p;
    mat2 rot = mm2(t);

    for (int i = 0; i < 4; i++) {
        vec2 dg = tri2(bp * 1.85) * 0.75;
        dg = dg * rot;
        p -= dg / z2;

        bp *= 1.3;
        z2 *= 0.45;
        z *= 0.42;

        p *= 1.21 + (rz - 1.0) * 0.02;
        rz += tri(p.x + tri(p.y)) * z;

        p *= mat2(vec2(-0.95534, -0.29552), vec2(0.29552, -0.95534));
    }

    return clamp(1.0 / pow(rz * 29.0, 1.3), 0.0, 0.55);
}

vec4 getAuroraColor(vec3 direction) {
    vec4 color = vec4(0.0);
    vec4 averageColor = vec4(0.0);
    float time = AuroraTime * Speed;

    float jitter = fract(sin(dot(direction.xz, vec2(13.0, 78.0))));
    float jitterStepScale = Height / resolution_loop_count;

    for (int i = 0; i < int(resolution_loop_count); i++) {
        float depthStep = (float(i) + jitter) * jitterStepScale;
        float depth = (Scale + pow(depthStep, 1.4) * 0.002) / (direction.y * 2.0 + 0.4);
        vec3 pos = depth * direction;
        vec2 curtainPos = pos.zx;
        curtainPos.x += time * 0.18;
        curtainPos.y += sin(curtainPos.x * 0.35 + time * 0.70) * 0.16;
        curtainPos.y += sin(curtainPos.x * 0.17 - time * 0.43) * 0.08;
        float noise = trinoise2d(curtainPos, time * 0.35);
        noise *= 0.88 + 0.12 * sin(time * 0.55 + pos.x * 0.31 + pos.z * 0.19);

        vec4 col = vec4(0.0);
        col.a = noise;

        float weight = exp2(-depthStep * 0.065 - 2.5) * smoothstep(0.0, 5.0, depthStep);

        col.rgb = texture(ColorGradient, vec2(depthStep / Height, 0.0)).rgb * noise;
        averageColor = mix(averageColor, col, 0.5);

        color += averageColor * weight * jitterStepScale;
    }

    color *= clamp(direction.y * 15.0 + 0.4, 0.0, 1.0);
    color *= Brightness;

    return color;
}

void main() {
    vec3 direction = normalize(localPos);
    if (direction.y <= -0.1) {
        fragColor = vec4(0.0);
        return;
    }

    vec4 aurora = getAuroraColor(direction);
    fragColor = vec4(aurora.rgb, clamp(aurora.a, 0.0, 1.0));
}
