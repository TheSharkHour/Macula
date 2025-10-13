package net.mine_diver.macula.rendering;

import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class TextureManager {
    public static void bindPostprocessingTextures() {
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

    public static void bindEnvironmentTextures(Minecraft minecraft) {
        GL13.glActiveTexture(GL13.GL_TEXTURE2);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, minecraft.textureManager.getTextureId("/terrain_nh.png"));
        GL13.glActiveTexture(GL13.GL_TEXTURE3);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, minecraft.textureManager.getTextureId("/terrain_s.png"));
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
    }
}
