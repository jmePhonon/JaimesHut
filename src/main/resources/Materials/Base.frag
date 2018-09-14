
#import "Common/ShaderLib/Lighting.glsllib"

in vec2 texCoord;
in vec3 wvPosition;
in vec3 wvNormal;

out vec4 outFragColor;

uniform vec4 g_LightData[NB_LIGHTS];
uniform vec4 g_Ambientlight_color;

uniform sampler2D m_ColorMap;



#define ENABLE_LIGHTING
void main(){
    outFragColor=texture(m_ColorMap,texCoord);
    if(outFragColor.a<0.3)discard;

    #ifdef ENABLE_LIGHTING

    vec3 light_sum=vec3(0);
    // for (int i =0;i < NB_LIGHTS; i+=3){
    //     vec4 light_color = g_LightData[i];
    //     vec4 light_data1 = g_LightData[i+1];                
    //     vec4 light_dir;
    //     vec3 light_vec;        

    //     lightComputeDir(wPosition, light_color.w, light_data1, light_dir, light_vec);
    //     float falloff =  computeSpotFalloff(g_LightData[i+2], light_vec)*light_dir.w;
    //     light_dir.xyz = normalize(light_dir.xyz);            

    //     float ndotl = max(0.0,dot(light_dir.xyz,wNormal));
    //     // float halflamb = (ndotl * 0.5)+0.5; 
    //     light_sum += ndotl*falloff * light_color.rgb*0.01;

    // }


     for (int i =0;i < NB_LIGHTS; i+=3){
                  
        
        // float is_positional = step(0.5, light_color.w);   
        // vec3 t = light_data.xyz * sign(is_positional - 0.5) - (wPosition * is_positional);

        // vec3 light_dir=normalize(t);

        vec4 lightData1 = g_LightData[i+1];  

        //lightData[i]
        vec3 light_color=g_LightData[i].rgb;              
        float light_type=g_LightData[i].w;

        //lightData[i+1]
        vec3 spot_position=g_LightData[i+1].xyz; // in viewspace when using SinglePass logic
        float spot_invradius=g_LightData[i+1].w; // -1 for directional

        //lightData[i+2]
        // vec3 spot_direction=normalize(g_LightData[i+2].xyz);
        // float spot_anglecos=g_LightData[i+2].w;        

        vec3 frag_position=wvPosition.xyz;

        bool is_positional = light_type!=0.0;
        vec3 light_vector=mix(-spot_position,spot_position-frag_position,vec3(is_positional));  
        
        float light_dist = length(light_vector);
        vec3 light_direction = normalize(light_vector);

        // spot_invradius=1./4.;
        // float radius=1.-spot_invradius;
        // attenuation=1.-(light_dist/radius);
        // float attenuation=spot_invradius * light_dist;
        // attenuation+=light_dist*0.1;
        // attenuation= smoothstep(0., 4., attenuation)  ;//clamp(attenuation,0.,1.);
        // attenuation=1.-attenuation;
        float radius=1./spot_invradius;

        float attenuation=1.0 / (1.0 + 0.*light_dist + 8.0*light_dist*light_dist) ;
        attenuation-=light_dist*spot_invradius;
        attenuation=clamp(attenuation,0.,1.);
        attenuation=mix(0.,attenuation,is_positional);

        // Spot falloff
        // float curAngleCos = dot(-light_direction, normalize(spot_direction));    
        // float innerAngleCos = floor(spot_anglecos) * 0.001;
        // float outerAngleCos = fract(spot_anglecos);
        // float innerMinusOuter = innerAngleCos - outerAngleCos;
        // float falloff = clamp((curAngleCos - outerAngleCos) / innerMinusOuter, step(spot_anglecos, 0.001), 1.0);
        //         falloff=mix(1.0,falloff,is_positional);

        // 

        
        float light = dot(wvNormal, light_direction)*.5+.5;
        light=pow(light,2.);
        light=clamp(light,0.,1.);

        light_sum+= light*light_color*attenuation;
  
    }
    outFragColor.rgb*=light_sum;
    #endif
}