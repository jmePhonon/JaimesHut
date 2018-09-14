uniform mat4 g_WorldViewMatrix;
uniform mat3 g_NormalMatrix;
uniform mat4 g_WorldViewProjectionMatrix;

out vec2 texCoord;
out vec3 wvPosition;
out vec3 wvNormal;

in vec3 inPosition;
in vec2 inTexCoord;
in vec3 inNormal;


void main(){
    texCoord = inTexCoord;
    vec4 modelSpacePos = vec4(inPosition, 1.0);
    vec3 modelSpaceNorm = inNormal;

    #ifdef NUM_BONES
        Skinning_Compute(modelSpacePos, modelSpaceNorm);
    #endif

    vec4 vpos= (g_WorldViewMatrix*modelSpacePos);
    wvNormal = normalize(g_NormalMatrix * modelSpaceNorm).xyz;
    wvPosition=vpos.xyz;

    gl_Position = g_WorldViewProjectionMatrix*modelSpacePos;    
}