package net.mine_diver.macula.utils;

import net.mine_diver.macula.rendering.pipeline.ShaderUniform;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for managing OpenGL shader uniforms with caching.
 */
public class Uniforms {
    private static final int MATRIX_SIZE = 16;
    private static final boolean TRANSPOSE = false;
    private static final int PROGRAM_ID_SHIFT = 16;

    private static final Map<Integer, Integer> uniformLocationCache = new HashMap<>();
    private static final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(MATRIX_SIZE);

    /**
     * Caches uniform locations for shader program.
     *
     * @param programId OpenGL shader program ID
     */
    public static void cacheUniformLocations(int programId) {
        for (ShaderUniform uniform : ShaderUniform.values()) {
            int location = GL20.glGetUniformLocation(programId, uniform.getName());
            if (location != -1) {
                int key = (programId << PROGRAM_ID_SHIFT) | uniform.ordinal();
                uniformLocationCache.put(key, location);
            }
        }
    }

    /**
     * Gets cached uniform location.
     *
     * @param programId OpenGL shader program ID
     * @param uniform   shader uniform
     * @return uniform location or -1 if not found
     */
    private static int getUniformLocation(int programId, ShaderUniform uniform) {
        int key = (programId << PROGRAM_ID_SHIFT) | uniform.ordinal();
        return uniformLocationCache.getOrDefault(key, -1);
    }

    /**
     * Sets integer uniform value.
     *
     * @param programId OpenGL shader program ID
     * @param uniform   shader uniform
     * @param n         integer value
     */
    public static void setProgramUniform1i(int programId, ShaderUniform uniform, int n) {
        int location = getUniformLocation(programId, uniform);
        if (location != -1) GL20.glUniform1i(location, n);
    }

    /**
     * Sets float uniform value.
     *
     * @param programId OpenGL shader program ID
     * @param uniform   shader uniform
     * @param x         float value
     */
    public static void setProgramUniform1f(int programId, ShaderUniform uniform, float x) {
        int location = getUniformLocation(programId, uniform);
        if (location != -1) GL20.glUniform1f(location, x);
    }

    /**
     * Sets 3D vector uniform value.
     *
     * @param programId OpenGL shader program ID
     * @param uniform   shader uniform
     * @param vec3      3D vector value
     */
    public static void setProgramUniform3f(int programId, ShaderUniform uniform, Vector3f vec3) {
        int location = getUniformLocation(programId, uniform);
        if (location != -1) GL20.glUniform3f(location, vec3.x, vec3.y, vec3.z);
    }

    /**
     * Sets 4x4 matrix uniform value.
     *
     * @param programId OpenGL shader program ID
     * @param uniform   shader uniform
     * @param mat4      4x4 matrix value
     */
    public static void setProgramUniformMatrix4(int programId, ShaderUniform uniform, Matrix4f mat4) {
        int location = getUniformLocation(programId, uniform);

        if (location == -1) return;

        matrixBuffer.clear();
        mat4.get(matrixBuffer);
        GL20.glUniformMatrix4(location, TRANSPOSE, matrixBuffer);
    }

    /**
     * Clears uniform location cache.
     */
    public static void clearUniformLocation() {
        uniformLocationCache.clear();
    }
}