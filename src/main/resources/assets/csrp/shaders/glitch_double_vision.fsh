#version 330
#extension GL_ARB_separate_shader_objects : require

// Port of "Glitch Double Vision" (Godot shader):
// https://godotshaders.com/shader/glitch-double-vision/
// Retro pixelated VHS glitch combined with a red/blue anaglyph double vision.

uniform sampler2D DiffuseSampler;

layout(std140) uniform GlitchConfig {
    float Time;
    float pixelsize;
    float vhs_intensity;
    float opacity;
    float double_vision_split;
};

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

float random(vec2 uv) {
    return fract(sin(dot(uv, vec2(12.9898, 78.233))) * 43758.5453123);
}

void main() {
    vec4 originalColor = texture(DiffuseSampler, texCoord);
    vec2 screenRes = vec2(textureSize(DiffuseSampler, 0));
    vec2 gridUv = round(texCoord * (screenRes / pixelsize)) / (screenRes / pixelsize);

    float timeStep = floor(Time * 15.0);
    float glitch = (random(vec2(timeStep, floor(gridUv.y * 30.0))) - 0.5)
            * 0.005 * vhs_intensity;

    vec2 uv = gridUv;
    uv.x += glitch;

    vec2 uvRed = uv + vec2(double_vision_split, 0.0);
    vec2 uvBlue = uv - vec2(double_vision_split, 0.0);

    vec4 texRed = texture(DiffuseSampler, uvRed);
    vec4 texBlue = texture(DiffuseSampler, uvBlue);

    vec3 redVision = vec3(texRed.r, 0.0, 0.0);
    vec3 blueVision = vec3(0.0, texBlue.g, texBlue.b);
    vec3 vhsColor = redVision + blueVision;

    vec3 finalColor = mix(originalColor.rgb, vhsColor, opacity);
    fragColor = vec4(finalColor, 1.0);
}
