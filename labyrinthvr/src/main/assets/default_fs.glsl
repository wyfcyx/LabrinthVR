precision mediump float;
varying vec2 v_UV;
uniform sampler2D u_Texture;
void main() {
    //gl_FragColor = vec4(1.0, 0.5, 0.2, 1.0);
    //gl_FragColor = vec4(v_UV.x, v_UV.y, 0.5, 1.0);
    gl_FragColor = texture2D(u_Texture, vec2(v_UV.x, 1.0 - v_UV.y));
}
