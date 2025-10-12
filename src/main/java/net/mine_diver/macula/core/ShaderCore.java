// Code written by daxnitro.  Do what you want with it but give me some credit if you use it in whole or in part.

package net.mine_diver.macula.core;

import net.mine_diver.macula.config.ShaderConfig;
import net.mine_diver.macula.rendering.FramebufferManager;
import net.mine_diver.macula.rendering.ShadowMapManager;
import net.mine_diver.macula.rendering.pipeline.ShaderProgram;
import net.mine_diver.macula.rendering.pipeline.ShaderProgramType;
import net.mine_diver.macula.utils.GLUtils;
import net.mine_diver.macula.utils.MinecraftInstance;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

public class ShaderCore {

    public static final Minecraft MINECRAFT = MinecraftInstance.get();

    public static boolean isInitialized = false;

    public static int renderWidth = 0;
    public static int renderHeight = 0;
    public static float aspectRatio = 0;

    private static final float[] clearColor = new float[3];

    public static float rainStrength = 0.0f;

    public static boolean fogEnabled = true;

    public static int entityAttrib = -1;

    static {
        if (!ShaderConfig.configDir.exists())
            if (!ShaderConfig.configDir.mkdirs())
                throw new RuntimeException();
        ShaderConfig.loadConfig();
    }

    public static void init() {
        if (!(ShaderPack.shaderPackLoaded = !ShaderPack.currentShaderName.equals(ShaderPack.SHADER_DISABLED))) return;

        int maxDrawBuffers = GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS);

        System.out.println("GL_MAX_DRAW_BUFFERS = " + maxDrawBuffers);

        FramebufferManager.colorAttachments = 4;

        ShaderProgram.initializeShaders();

        if (FramebufferManager.colorAttachments > maxDrawBuffers) System.out.println("Not enough draw buffers!");

        ShaderProgram.resolveFallbacks();

        FramebufferManager.defaultDrawBuffers = BufferUtils.createIntBuffer(FramebufferManager.colorAttachments);
        for (int i = 0; i < FramebufferManager.colorAttachments; ++i)
            FramebufferManager.defaultDrawBuffers.put(i, ARBFramebufferObject.GL_COLOR_ATTACHMENT0 + i);

        FramebufferManager.defaultTextures = BufferUtils.createIntBuffer(FramebufferManager.colorAttachments);
        FramebufferManager.defaultRenderBuffers = BufferUtils.createIntBuffer(FramebufferManager.colorAttachments);

        resize();
        ShadowMapManager.initializeShadowMap();
        isInitialized = true;
    }

    public static void setClearColor(float red, float green, float blue) {
        clearColor[0] = red;
        clearColor[1] = green;
        clearColor[2] = blue;

        if (ShadowMapManager.isShadowPass) {
            GLUtils.glClearBuffer(clearColor[0], clearColor[1], clearColor[2], 1f);
            return;
        }

        GL20.glDrawBuffers(FramebufferManager.defaultDrawBuffers);
        GLUtils.glClearBuffer(0f, 0f, 0f, 0f);

        GL20.glDrawBuffers(ARBFramebufferObject.GL_COLOR_ATTACHMENT0);
        GLUtils.glClearBuffer(clearColor[0], clearColor[1], clearColor[2], 1f);

        GL20.glDrawBuffers(ARBFramebufferObject.GL_COLOR_ATTACHMENT1);
        GLUtils.glClearBuffer(1f, 1f, 1f, 1f);

        GL20.glDrawBuffers(FramebufferManager.defaultDrawBuffers);
    }

    private static void resize() {
        renderWidth = MINECRAFT.displayWidth;
        renderHeight = MINECRAFT.displayHeight;

        aspectRatio = (float) renderWidth / (float) renderHeight;

        FramebufferManager.setupRenderTextures();
        FramebufferManager.setupFrameBuffer();
    }

    public static void beginRender(Minecraft minecraft, float f, long l) {
        rainStrength = minecraft.world.getRainGradient(f);

        if (ShadowMapManager.isShadowPass) return;

        if (!isInitialized) init();
        if (!ShaderPack.shaderPackLoaded) return;
        if (MINECRAFT.displayWidth != renderWidth || MINECRAFT.displayHeight != renderHeight)
            resize();

        if (ShadowMapManager.shadowEnabled) {
            // do shadow pass
            boolean preShadowPassThirdPersonView = MINECRAFT.options.thirdPerson;
            MINECRAFT.options.thirdPerson = true;

            ShadowMapManager.isShadowPass = true;

            ARBFramebufferObject.glBindFramebuffer(ARBFramebufferObject.GL_FRAMEBUFFER,
                    ShadowMapManager.shadowFramebufferId);
            ShaderProgram.useShaderProgram(ShaderProgramType.NONE);
            MINECRAFT.gameRenderer.delta(f, l);
            GL11.glFlush();

            ShadowMapManager.isShadowPass = false;
            MINECRAFT.options.thirdPerson = preShadowPassThirdPersonView;
        }

        ARBFramebufferObject.glBindFramebuffer(ARBFramebufferObject.GL_FRAMEBUFFER,
                FramebufferManager.defaultFramebufferId);

        ShaderProgram.useShaderProgram(ShaderProgramType.TEXTURED);
    }

    public static void endRender() {
        if (ShadowMapManager.isShadowPass) return;

        GL11.glPushMatrix();

        GLUtils.setupScreenOrthographicProjection();

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        // composite

        GL11.glDisable(GL11.GL_BLEND);

        ShaderProgram.useShaderProgram(ShaderProgramType.COMPOSITE);

        GL20.glDrawBuffers(FramebufferManager.defaultDrawBuffers);

        bindPostprocessingTextures();
        GLUtils.glDrawQuad();

        // final

        ARBFramebufferObject.glBindFramebuffer(ARBFramebufferObject.GL_FRAMEBUFFER, 0);

        ShaderProgram.useShaderProgram(ShaderProgramType.FINAL);

        GLUtils.glClearBuffer(clearColor[0], clearColor[1], clearColor[2], 1f);

        bindPostprocessingTextures();
        GLUtils.glDrawQuad();

        GL11.glEnable(GL11.GL_BLEND);

        GL11.glPopMatrix();
        ShaderProgram.useShaderProgram(ShaderProgramType.NONE);
    }

    public static void beginTerrain() {
        ShaderProgram.useShaderProgram(ShaderProgramType.TERRAIN);
        bindEnvironmentTextures();
    }

    public static void endTerrain() {
        ShaderProgram.useShaderProgram(ShaderProgramType.TEXTURED);
    }

    public static void beginWater() {
        ShaderProgram.useShaderProgram(ShaderProgramType.WATER);
        bindEnvironmentTextures();
    }

    public static void endWater() {
        ShaderProgram.useShaderProgram(ShaderProgramType.TEXTURED);
    }

    public static void beginHand() {
        GL11.glEnable(GL11.GL_BLEND);
        ShaderProgram.useShaderProgram(ShaderProgramType.HAND);
    }

    public static void endHand() {
        GL11.glDisable(GL11.GL_BLEND);
        ShaderProgram.useShaderProgram(ShaderProgramType.TEXTURED);

        if (ShadowMapManager.isShadowPass)
            ARBFramebufferObject.glBindFramebuffer(ARBFramebufferObject.GL_FRAMEBUFFER,
                    ShadowMapManager.shadowFramebufferId); // was set to 0 in beginWeather()
    }

    public static void beginWeather() {
        GL11.glEnable(GL11.GL_BLEND);
        ShaderProgram.useShaderProgram(ShaderProgramType.WEATHER);

        if (ShadowMapManager.isShadowPass)
            ARBFramebufferObject.glBindFramebuffer(ARBFramebufferObject.GL_FRAMEBUFFER,
                    0); // will be set to sbf in endHand()
    }

    public static void endWeather() {
        GL11.glDisable(GL11.GL_BLEND);
        ShaderProgram.useShaderProgram(ShaderProgramType.TEXTURED);
    }

    private static void bindPostprocessingTextures() {
        for (byte i = 0; i < FramebufferManager.colorAttachments; i++) {
            GL13.glActiveTexture(GL13.GL_TEXTURE0 + i);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, FramebufferManager.defaultTextures.get(i));
        }

        if (ShadowMapManager.shadowEnabled) {
            GL13.glActiveTexture(GL13.GL_TEXTURE7);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, ShadowMapManager.shadowDepthTextureId);
        }

        GL13.glActiveTexture(GL13.GL_TEXTURE0);

        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void bindEnvironmentTextures() {
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, MINECRAFT.textureManager.getTextureId("/terrain_nh.png"));
        GL13.glActiveTexture(GL13.GL_TEXTURE3);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, MINECRAFT.textureManager.getTextureId("/terrain_s.png"));
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }
}

