#version 150

// Blackout pass that runs while the cold star blizzard wind reverses direction. A value of 0 leaves the
// scene untouched, so the effect is invisible whenever the storm is not flipping.

uniform sampler2D DiffuseSampler;
uniform float SRP_BlackBlend;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 scene = texture(DiffuseSampler, texCoord);
    float blackBlend = clamp(SRP_BlackBlend, 0.0, 1.0);
    float dim = 1.0 - blackBlend * 0.94;
    vec3 finalRgb = scene.rgb * dim;
    finalRgb -= vec3(0.011, 0.008, 0.002) * blackBlend;
    fragColor = vec4(max(finalRgb, vec3(0.0)), scene.a);
}
