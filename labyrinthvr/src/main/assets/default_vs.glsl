uniform mat4 u_MVP;
//attribute vec3 a_Position;
attribute vec4 a_Position;
void main() {
    //gl_Positon = u_MVP * vec4(a_Position.x, a_Position.y, a_Position.z, 1.0);
    //gl_Position = vec4(a_Position.x, a_Position.y, a_Position.z, 1.0);
    //gl_Position = a_Position;
    gl_Position = u_MVP * a_Position;
}