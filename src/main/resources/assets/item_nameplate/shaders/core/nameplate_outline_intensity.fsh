#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;
flat in ivec4 glyphBounds;

out vec4 fragColor;

void main() {
    vec2 pixel = 1.0 / vec2(textureSize(Sampler0, 0));
    ivec2 atlasPixel = ivec2(floor(texCoord0 * vec2(textureSize(Sampler0, 0))));
    float center = atlasPixel.x >= glyphBounds.x && atlasPixel.x < glyphBounds.y && atlasPixel.y >= glyphBounds.z && atlasPixel.y < glyphBounds.w
        ? texture(Sampler0, texCoord0).r
        : 0.0;
    float outline = 0.0;
    for (int y = -1; y <= 1; ++y) {
        for (int x = -1; x <= 1; ++x) {
            if (x == 0 && y == 0) {
                continue;
            }
            ivec2 samplePixel = atlasPixel + ivec2(x, y);
            if (samplePixel.x >= glyphBounds.x && samplePixel.x < glyphBounds.y && samplePixel.y >= glyphBounds.z && samplePixel.y < glyphBounds.w) {
                outline = max(outline, texture(Sampler0, texCoord0 + vec2(x, y) * pixel).r);
            }
        }
    }

    vec4 color = center >= 0.1
        ? vec4(vertexColor.rgb, vertexColor.a * center)
        : vec4(0.0, 0.0, 0.0, vertexColor.a * outline);
    color *= ColorModulator;
    if (color.a < 0.1) {
        discard;
    }
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
