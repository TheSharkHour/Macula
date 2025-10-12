package net.mine_diver.macula.utils;

import net.mine_diver.macula.core.ShaderCore;
import net.mine_diver.macula.rendering.pipeline.ShaderProgram;
import net.mine_diver.macula.rendering.pipeline.ShaderProgramType;
import net.mine_diver.macula.rendering.pipeline.ShaderUniform;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;

/**
 * Utility for OpenGL operations and shader management.
 */
public class GL {
    public static final String GL_VERSION_STRING = GL11.glGetString(GL11.GL_VERSION);
    public static final String GL_VENDOR_STRING = GL11.glGetString(GL11.GL_VENDOR);
    public static final String GL_RENDERER_STRING = GL11.glGetString(GL11.GL_RENDERER);

    private static final float QUAD_TEX_COORD_MIN = 0.0f;
    private static final float QUAD_TEX_COORD_MAX = 1.0f;
    private static final float QUAD_VERTEX_MIN = 0.0f;
    private static final float QUAD_VERTEX_MAX = 1.0f;
    private static final float QUAD_DEPTH = 0.0f;

    private static final int MATRIX_BUFFER_SIZE = 16;

    // Screen orthographic matrix
    private static final Matrix4f SCREEN_ORTHO_MATRIX = new Matrix4f().setOrtho(0f, 1f, 0f, 1f, 0f, 1f);
    private static final FloatBuffer SCREEN_ORTHO_BUFFER;

    static {
        SCREEN_ORTHO_BUFFER = BufferUtils.createFloatBuffer(MATRIX_BUFFER_SIZE);
        SCREEN_ORTHO_MATRIX.get(SCREEN_ORTHO_BUFFER);
    }

    /**
     * Wrapper for glEnable with shader state management.
     *
     * @param cap OpenGL capability to enable
     */
    public static void glEnableWrapper(int cap) {
        GL11.glEnable(cap);
        if (cap == GL11.GL_TEXTURE_2D) {
            if (ShaderProgram.activeShaderProgram == ShaderProgramType.BASIC) {
                ShaderProgram.useShaderProgram(ShaderProgramType.TEXTURED);
            }
        } else if (cap == GL11.GL_FOG) {
            ShaderCore.fogEnabled = true;
            Uniforms.setProgramUniform1i(ShaderProgram.shaderProgramId.get(ShaderProgram.activeShaderProgram),
                    ShaderUniform.FOG_MODE, GL11.glGetInteger(GL11.GL_FOG_MODE));
        }
    }

    /**
     * Wrapper for glDisable with shader state management.
     *
     * @param cap OpenGL capability to disable
     */
    public static void glDisableWrapper(int cap) {
        GL11.glDisable(cap);
        if (cap == GL11.GL_TEXTURE_2D) {
            if (ShaderProgram.activeShaderProgram == ShaderProgramType.TEXTURED ||
                    ShaderProgram.activeShaderProgram == ShaderProgramType.TEXTURED_LIT) {
                ShaderProgram.useShaderProgram(ShaderProgramType.BASIC);
            }
        } else if (cap == GL11.GL_FOG) {
            ShaderCore.fogEnabled = false;
            Uniforms.setProgramUniform1i(ShaderProgram.shaderProgramId.get(ShaderProgram.activeShaderProgram),
                    ShaderUniform.FOG_MODE, 0);
        }
    }

    /**
     * Draws full-screen quad.
     */
    public static void glDrawQuad() {
        GL11.glBegin(GL11.GL_TRIANGLES);

        GL11.glTexCoord2f(QUAD_TEX_COORD_MIN, QUAD_TEX_COORD_MIN);
        GL11.glVertex3f(QUAD_VERTEX_MIN, QUAD_VERTEX_MIN, QUAD_DEPTH);

        GL11.glTexCoord2f(QUAD_TEX_COORD_MAX, QUAD_TEX_COORD_MIN);
        GL11.glVertex3f(QUAD_VERTEX_MAX, QUAD_VERTEX_MIN, QUAD_DEPTH);

        GL11.glTexCoord2f(QUAD_TEX_COORD_MAX, QUAD_TEX_COORD_MAX);
        GL11.glVertex3f(QUAD_VERTEX_MAX, QUAD_VERTEX_MAX, QUAD_DEPTH);

        GL11.glTexCoord2f(QUAD_TEX_COORD_MIN, QUAD_TEX_COORD_MIN);
        GL11.glVertex3f(QUAD_VERTEX_MIN, QUAD_VERTEX_MIN, QUAD_DEPTH);

        GL11.glTexCoord2f(QUAD_TEX_COORD_MAX, QUAD_TEX_COORD_MAX);
        GL11.glVertex3f(QUAD_VERTEX_MAX, QUAD_VERTEX_MAX, QUAD_DEPTH);

        GL11.glTexCoord2f(QUAD_TEX_COORD_MIN, QUAD_TEX_COORD_MAX);
        GL11.glVertex3f(QUAD_VERTEX_MIN, QUAD_VERTEX_MAX, QUAD_DEPTH);

        GL11.glEnd();
    }

    /**
     * Prints OpenGL program info log.
     *
     * @param program OpenGL program ID
     * @return true if compiled successfully, false otherwise
     */
    public static boolean printLogInfo(int program) {
        int logLength = GL20.glGetProgrami(program, GL20.GL_INFO_LOG_LENGTH);

        if (logLength == 0) return true;

        String log = GL20.glGetProgramInfoLog(program, logLength);
        if (!log.trim().isEmpty()) {
            System.out.println("Info log:\n" + log);
            return false;
        }
        return true;
    }

    /**
     * Clears OpenGL color and depth buffers.
     *
     * @param red   red component (0.0 to 1.0)
     * @param green green component (0.0 to 1.0)
     * @param blue  blue component (0.0 to 1.0)
     * @param alpha alpha component (0.0 to 1.0)
     */
    public static void glClearBuffer(float red, float green, float blue, float alpha) {
        GL11.glClearColor(red, green, blue, alpha);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    /**
     * Loads 4x4 matrix into OpenGL current matrix.
     *
     * @param matrix matrix to load
     */
    public static void loadMatrixToOpenGL(Matrix4f matrix) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
        matrix.get(buffer);
        GL11.glLoadMatrix(buffer);
    }

    /**
     * Sets up orthographic projection for screen-space rendering.
     */
    public static void setupScreenOrthographicProjection() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadMatrix(SCREEN_ORTHO_BUFFER);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();
    }

    /**
     * Creates depth buffer for framebuffer operations.
     *
     * @param width  buffer width
     * @param height buffer height
     * @return OpenGL renderbuffer ID
     */
    public static int glCreateDepthBuffer(int width, int height) {
        int depthBuffer = ARBFramebufferObject.glGenRenderbuffers();
        ARBFramebufferObject.glBindRenderbuffer(ARBFramebufferObject.GL_RENDERBUFFER, depthBuffer);
        ARBFramebufferObject.glRenderbufferStorage(
                ARBFramebufferObject.GL_RENDERBUFFER,
                GL11.GL_DEPTH_COMPONENT,
                width,
                height
        );
        return depthBuffer;
    }
}