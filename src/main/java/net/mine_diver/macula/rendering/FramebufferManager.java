package net.mine_diver.macula.rendering;

import net.mine_diver.macula.utils.GL;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.ARBTextureFloat;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class FramebufferManager {
    public static final float[] clearColor = new float[3];
    public static int colorAttachments = 0;

    public static IntBuffer defaultDrawBuffers = null;
    public static IntBuffer defaultTextures = null;
    public static IntBuffer defaultRenderBuffers = null;

    public static int defaultFramebufferId = 0;
    public static int renderWidth = 0;
    public static int renderHeight = 0;
    public static float aspectRatio = 0;
    private static int defaultDepthBufferId = 0;

    private static final int DEPTH_ATTACHMENT_INDEX = 1;

    public static void setupFrameBuffer() {
        if (defaultFramebufferId != 0) {
            ARBFramebufferObject.glDeleteFramebuffers(defaultFramebufferId);
            ARBFramebufferObject.glDeleteRenderbuffers(defaultRenderBuffers);
        }

        defaultFramebufferId = ARBFramebufferObject.glGenFramebuffers();
        ARBFramebufferObject.glBindFramebuffer(ARBFramebufferObject.GL_FRAMEBUFFER, defaultFramebufferId);

        ARBFramebufferObject.glGenRenderbuffers(defaultRenderBuffers);

        for (int i = 0; i < colorAttachments; ++i) {
            ARBFramebufferObject.glBindRenderbuffer(ARBFramebufferObject.GL_RENDERBUFFER,
                    defaultRenderBuffers.get(i));
            // Depth buffer
            if (i == DEPTH_ATTACHMENT_INDEX) {
                ARBFramebufferObject.glRenderbufferStorage(ARBFramebufferObject.GL_RENDERBUFFER,
                        ARBTextureFloat.GL_RGB32F_ARB, renderWidth, renderHeight);
            } else {
                ARBFramebufferObject.glRenderbufferStorage(ARBFramebufferObject.GL_RENDERBUFFER, GL11.GL_RGBA,
                        renderWidth, renderHeight);
            }
            ARBFramebufferObject.glFramebufferRenderbuffer(ARBFramebufferObject.GL_FRAMEBUFFER,
                    defaultDrawBuffers.get(i), ARBFramebufferObject.GL_RENDERBUFFER,
                    defaultRenderBuffers.get(i));
            ARBFramebufferObject.glFramebufferTexture2D(ARBFramebufferObject.GL_FRAMEBUFFER,
                    defaultDrawBuffers.get(i), GL11.GL_TEXTURE_2D,
                    defaultTextures.get(i), 0);
        }

        if (defaultDepthBufferId != 0)
            ARBFramebufferObject.glDeleteRenderbuffers(defaultDepthBufferId);
        defaultDepthBufferId = GL.glCreateDepthBuffer(renderWidth, renderHeight);

        ARBFramebufferObject.glFramebufferRenderbuffer(ARBFramebufferObject.GL_FRAMEBUFFER,
                ARBFramebufferObject.GL_DEPTH_ATTACHMENT, ARBFramebufferObject.GL_RENDERBUFFER,
                defaultDepthBufferId);

        int status = ARBFramebufferObject.glCheckFramebufferStatus(ARBFramebufferObject.GL_FRAMEBUFFER);
        if (status != ARBFramebufferObject.GL_FRAMEBUFFER_COMPLETE)
            System.err.println("Failed creating framebuffer! (Status " + status + ")");
    }

    public static void setupRenderTextures() {
        GL11.glDeleteTextures(defaultTextures);
        GL11.glGenTextures(defaultTextures);

        for (int i = 0; i < colorAttachments; ++i) {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, defaultTextures.get(i));
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            if (i == DEPTH_ATTACHMENT_INDEX) { // depth buffer
                ByteBuffer buffer = ByteBuffer.allocateDirect(renderWidth * renderHeight * 4 * 4);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, ARBTextureFloat.GL_RGB32F_ARB, renderWidth,
                        renderHeight, 0, GL11.GL_RGBA, GL11.GL_FLOAT,
                        buffer);
            } else {
                ByteBuffer buffer = ByteBuffer.allocateDirect(renderWidth * renderHeight * 4);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, renderWidth, renderHeight, 0,
                        GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE,
                        buffer);
            }
        }
    }

    public static void setClearColor(float red, float green, float blue) {
        clearColor[0] = red;
        clearColor[1] = green;
        clearColor[2] = blue;

        if (ShadowMapManager.isShadowPass) {
            GL.glClearBuffer(
                    clearColor[0], clearColor[1], clearColor[2], 1f);
            return;
        }

        GL20.glDrawBuffers(defaultDrawBuffers);
        GL.glClearBuffer(0f, 0f, 0f, 0f);

        GL20.glDrawBuffers(ARBFramebufferObject.GL_COLOR_ATTACHMENT0);
        GL.glClearBuffer(
                clearColor[0], clearColor[1], clearColor[2], 1f);

        GL20.glDrawBuffers(ARBFramebufferObject.GL_COLOR_ATTACHMENT1);
        GL.glClearBuffer(1f, 1f, 1f, 1f);

        GL20.glDrawBuffers(defaultDrawBuffers);
    }

    public static void resize(int width, int height) {
        renderWidth = width;
        renderHeight = height;

        aspectRatio = (float) renderWidth / (float) renderHeight;

        setupRenderTextures();
        setupFrameBuffer();
    }
}