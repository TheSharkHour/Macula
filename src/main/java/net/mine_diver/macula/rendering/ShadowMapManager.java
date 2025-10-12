package net.mine_diver.macula.rendering;

import net.mine_diver.macula.core.ShaderCore;
import net.mine_diver.macula.shaders.uniform.MatrixUniforms;
import net.mine_diver.macula.utils.GL;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.ARBFramebufferObject.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_CLAMP_TO_BORDER;

public class ShadowMapManager {

    public static boolean shadowEnabled = false;
    public static int shadowResolution = 1024;
    public static float shadowMapHalfPlane = 64f;

    private static final float FAR = 256f;
    private static final float NEAR = 1f / 16384f;

    public static boolean isShadowPass = false;

    public static int shadowFramebufferId = 0;
    public static int shadowDepthTextureId = 0;
    private static int shadowDepthBufferId = 0;
    
    public static Matrix4f shadowModelView = new Matrix4f();

    public static void setupShadowViewport(float f, Vector3f cameraPosition) {
        glViewport(0, 0, shadowResolution, shadowResolution);

        Matrix4f shadowProjectionMatrix = new Matrix4f().setOrtho(
                -shadowMapHalfPlane,
                shadowMapHalfPlane,
                -shadowMapHalfPlane,
                shadowMapHalfPlane,
                NEAR,
                FAR
        );
        
        MatrixUniforms.updateShadowProjection(shadowProjectionMatrix);

        // Load projection matrix to OpenGL
        glMatrixMode(GL_PROJECTION);
        GL.loadMatrixToOpenGL(shadowProjectionMatrix);

        Matrix4f lightModelView = new Matrix4f();
        lightModelView.translate(0f, 0f, -128f);
        lightModelView.rotate((float) Math.toRadians(90f), 0f, 0f, -1f);

        float angle = Math.round(ShaderCore.MINECRAFT.world.method_198(f) * 360f);
        if (angle < 90f || angle > 270f) lightModelView.rotate(
                (float) Math.toRadians(angle - 90f),
                -1f,
                0f,
                0f
        ); // Daytime
        else lightModelView.rotate(
                (float) Math.toRadians(angle + 90f),
                -1f,
                0f,
                0f
        ); // Nighttime

        Vector3f cameraInLightSpace = new org.joml.Vector3f();
        lightModelView.transformPosition(cameraPosition, cameraInLightSpace);

        // Texel Snapping
        float texelSize = (2f * shadowMapHalfPlane) / shadowResolution;
        Vector3f snappedCenter = new Vector3f(
                cameraInLightSpace.x - Math.round(cameraInLightSpace.x / texelSize) * texelSize,
                cameraInLightSpace.y - Math.round(cameraInLightSpace.y / texelSize) * texelSize,
                0f
        );

        Matrix4f snapTransform = new Matrix4f().translation(snappedCenter);
        snapTransform.mul(lightModelView, shadowModelView);

        MatrixUniforms.updateShadowModelView(shadowModelView);

        // Load model-view matrix to OpenGL
        glMatrixMode(GL_MODELVIEW);
        GL.loadMatrixToOpenGL(shadowModelView);
    }

    public static void initializeShadowMap() {
        if (!shadowEnabled) return;

        createShadowFramebuffer();

        createShadowDepthBuffer();

        createShadowDepthTexture();

        int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
        if (status != GL_FRAMEBUFFER_COMPLETE) System.err.println(
                "Failed creating shadow framebuffer! (Status " + status + ")"
        );
    }

    private static void createShadowFramebuffer() {
        if (shadowFramebufferId != 0) glDeleteFramebuffers(shadowFramebufferId);

        shadowFramebufferId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, shadowFramebufferId);

        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);
    }

    private static void createShadowDepthBuffer() {
        if (shadowDepthBufferId != 0) glDeleteFramebuffers(shadowDepthBufferId);

        shadowDepthBufferId = GL.glCreateDepthBuffer(
                shadowResolution,
                shadowResolution
        );

        glFramebufferRenderbuffer(
                GL_FRAMEBUFFER,
                GL_DEPTH_ATTACHMENT,
                GL_RENDERBUFFER,
                shadowDepthBufferId
        );
    }

    private static void createShadowDepthTexture() {
        if (shadowDepthTextureId != 0) glDeleteTextures(shadowDepthTextureId);

        shadowDepthTextureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, shadowDepthTextureId);

        // Set texture wrapping
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);

        // Set linear filtering
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        ByteBuffer shadowMapBuffer = ByteBuffer.allocateDirect(
                shadowResolution * shadowResolution * 4
        );

        glTexImage2D(
                GL_TEXTURE_2D,
                0,
                GL_DEPTH_COMPONENT,
                shadowResolution,
                shadowResolution,
                0,
                GL_DEPTH_COMPONENT,
                GL_FLOAT,
                shadowMapBuffer
        );

        glFramebufferTexture2D(
                GL_FRAMEBUFFER,
                GL_DEPTH_ATTACHMENT,
                GL_TEXTURE_2D,
                shadowDepthTextureId,
                0
        );
    }
}