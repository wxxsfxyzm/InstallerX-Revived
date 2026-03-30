// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package com.rosan.installer.ui.library.effect

const val OS2_BG_FRAG = """
    uniform vec2 uResolution;
    uniform shader uTex;
    uniform shader uTexBitmap;
    uniform vec2 uTexWH;
    //uniform shader uPerlinTex;
    
    // 新版参数
    uniform float uAnimTime;
    uniform vec4 uBound;
    uniform float uTranslateY;
    uniform vec3 uPoints[4];
    uniform vec4 uColors[4];
    uniform float uAlphaMulti;
    uniform float uNoiseScale;
    uniform float uPointOffset;
    uniform float uPointRadiusMulti;
    uniform float uSaturateOffset;
    uniform float uLightOffset;
    uniform float uAlphaOffset;
    uniform float uShadowColorMulti;
    uniform float uShadowColorOffset;
    uniform float uShadowNoiseScale;
    uniform float uShadowOffset;
    
    vec3 hsl2rgb(in vec3 c)
    {
        vec3 rgb = clamp(abs(mod(c.x*6.0+vec3(0.0, 4.0, 2.0), 6.0)-3.0)-1.0, 0.0, 1.0);
    
        return c.z + c.y * (rgb-0.5)*(1.0-abs(2.0*c.z-1.0));
    }
    
    vec3 HueShift (in vec3 Color, in float Shift)
    {
        vec3 P = vec3(0.55735)*dot(vec3(0.55735), Color);
    
        vec3 U = Color-P;
    
        vec3 V = cross(vec3(0.55735), U);
    
        Color = U*cos(Shift*6.2832) + V*sin(Shift*6.2832) + P;
    
        return vec3(Color);
    }
    
    vec3 rgb2hsl(in vec3 c){
        float h = 0.0;
        float s = 0.0;
        float l = 0.0;
        float r = c.r;
        float g = c.g;
        float b = c.b;
        float cMin = min(r, min(g, b));
        float cMax = max(r, max(g, b));
    
        l = (cMax + cMin) / 2.0;
        if (cMax > cMin) {
            float cDelta = cMax - cMin;
    
            //s = l < .05 ? cDelta / ( cMax + cMin ) : cDelta / ( 2.0 - ( cMax + cMin ) ); Original
            s = l < .0 ? cDelta / (cMax + cMin) : cDelta / (2.0 - (cMax + cMin));
    
            if (r == cMax) {
                h = (g - b) / cDelta;
            } else if (g == cMax) {
                h = 2.0 + (b - r) / cDelta;
            } else {
                h = 4.0 + (r - g) / cDelta;
            }
    
            if (h < 0.0) {
                h += 6.0;
            }
            h = h / 6.0;
        }
        return vec3(h, s, l);
    }
    
    vec3 rgb2hsv(vec3 c)
    {
        vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
        vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
        vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    
        float d = q.x - min(q.w, q.y);
        float e = 1.0e-10;
        return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
    }
    
    vec3 hsv2rgb(vec3 c)
    {
        vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
        vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
        return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
    }
    
    float hash(vec2 p) {
        vec3 p3 = fract(vec3(p.xyx) * 0.13);
        p3 += dot(p3, p3.yzx + 3.333);
        return fract((p3.x + p3.y) * p3.z);
    }
    
    float perlin(vec2 x) {
        vec2 i = floor(x);
        vec2 f = fract(x);
    
        float a = hash(i);
        float b = hash(i + vec2(1.0, 0.0));
        float c = hash(i + vec2(0.0, 1.0));
        float d = hash(i + vec2(1.0, 1.0));
    
        vec2 u = f * f * (3.0 - 2.0 * f);
        return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
    }
    
    vec4 srcOver(vec4 src, vec4 dst){
        return src + dst * (1.0 - src.a);
    }
    
    vec4 blendSrcOver(vec4 src, vec4 dst) {
        if (src.a == 0.0) {
            return dst;
        }
    
        float srcAlpha = src.a;
        float dstAlpha = dst.a * (1.0 - srcAlpha);
        float outAlpha = srcAlpha + dstAlpha;
    
        if (outAlpha == 0.0) {
            return vec4(0, 0, 0, 0);
        }
    
        vec4 outColor = (src * srcAlpha + dst * dstAlpha) / outAlpha;
        return vec4(outColor.rgb, outAlpha);
    }
    
    float gradientNoise(in vec2 uv)
    {
        return fract(52.9829189 * fract(dot(uv, vec2(0.06711056, 0.00583715))));
    }
    
    vec4 main(vec2 fragCoord){
    
        vec2 vUv = fragCoord/uResolution;
        vUv.y = 1.0-vUv.y;
        vec2 uv = vUv;
        uv -= vec2(0., uTranslateY);
    
        uv.xy -= uBound.xy;
        uv.xy /= uBound.zw;
    
        vec3 hsv;
    
    //    vec4 color = vec4(1, 1, 1, 0.);
        vec4 color = vec4(0.0);
    
        float noiseValue = perlin(vUv * uNoiseScale + vec2(-uAnimTime, -uAnimTime));
    //    float noiseValue = uPerlinTex.eval(vUv * vec2(128.0) + vec2(-uAnimTime, -uAnimTime)*50.0).r;
    
        // draw circles
        for (int i = 0; i < 4; i++){
            vec4 pointColor = uColors[i];
            pointColor.rgb *= pointColor.a;
            vec2 point = uPoints[i].xy;
            float rad = uPoints[i].z * uPointRadiusMulti;
    
            point.x += sin(uAnimTime + point.y) * uPointOffset;
            point.y += cos(uAnimTime + point.x) * uPointOffset;
    
            float d = distance(uv, point);
            float pct = smoothstep(rad, 0., d);
            //float pct = smoothstep(rad, rad - 0.01, d);
    
            // color = blendSrcOver(color, pointColor);
            // color = blendSrcOver(pointColor, color);
    
            color.rgb = mix(color.rgb, pointColor.rgb, pct);
    
            // color.a += (1. - color.a) * pointColor.a;
            color.a = mix(color.a, pointColor.a, pct);
        }
    
        float oppositeNoise = smoothstep(0., 1., noiseValue);
        color.rgb /= color.a;
        hsv = rgb2hsv(color.rgb);
        hsv.y = mix(hsv.y, 0.0, oppositeNoise * uSaturateOffset);
    //    hsv.y += oppositeNoise * uSaturateOffset;
        color.rgb = hsv2rgb(hsv);
    
        color.rgb += oppositeNoise * uLightOffset;
    //    color.rgb = mix(color.rgb, min(color.rgb + oppositeNoise * uLightOffset, vec3(1.)), oppositeNoise);
        // color.a += noiseValue * uAlphaOffset;
    
        color.a = clamp(color.a, 0., 1.);
        color.a *= uAlphaMulti;
    
        vec4 texColor = uTexBitmap.eval(vec2(vUv.x, 1.0 - vUv.y)*uTexWH);
        vec4 uiColor = uTex.eval(vec2(vUv.x, 1.0 - vUv.y)*uResolution);
    
        vec4 fragColor;
    
        // 显示uBound区域
        //float debugColor = 1.;
        //debugColor *= step(0., uv.x);
        //debugColor *= step(0., uv.y);
        //debugColor *= step(uv.x, 1.);
        //debugColor *= step(uv.y, 1.);
        //color = mix(color, vec4(1., 0., 0., 1.), debugColor * 0.5);
    
        color += (10.0 / 255.0) * gradientNoise(fragCoord.xy) - (5.0 / 255.0);
    
        if (uiColor.a < 0.01) {
            fragColor = color;
        } else {
            fragColor = uiColor;
        }
    
        //        return vec4(0.0);
        //        return vec4(vUv,0.0,1.0);
        //        return vec4(abs(sin(uAnimTime)).rrr, 1.0);
        //            return texColor;
    //    return vec4(noiseValue.rrr,1.0);
        return vec4(fragColor.rgb*fragColor.a, fragColor.a);
        return vec4(color.rgb*color.a, color.a);
    }
"""
