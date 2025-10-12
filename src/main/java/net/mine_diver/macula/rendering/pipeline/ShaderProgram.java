package net.mine_diver.macula.rendering.pipeline;

import net.mine_diver.macula.utils.GL;
import net.mine_diver.macula.shaders.uniform.MatrixUniforms;
import net.mine_diver.macula.shaders.uniform.PositionUniforms;
import net.mine_diver.macula.core.ShaderCore;
import net.mine_diver.macula.utils.Uniforms;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.mine_diver.macula.rendering.ShadowMapManager;
import net.mine_diver.macula.shaders.compiler.ShaderCompiler;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL11;

import java.util.EnumMap;
import java.util.Map;

public class ShaderProgram {
    public static final EnumMap<ShaderProgramType, Integer> shaderProgramId = new EnumMap<>(ShaderProgramType.class);
    public static ShaderProgramType activeShaderProgram = ShaderProgramType.NONE;

    public static final int NO_PROGRAM_ID = 0;

    public static void initializeShaders() {
        shaderProgramId.put(ShaderProgramType.NONE, NO_PROGRAM_ID);

        ShaderProgramType[] shaderProgramTypes = ShaderProgramType.values();
        int shaderProgramTypesLength = shaderProgramTypes.length;

        for (int i = 1; i < shaderProgramTypesLength; i++) {
            ShaderProgramType shaderProgramType = shaderProgramTypes[i];
            String shaderProgramName = shaderProgramType.fileName;
            shaderProgramId.put(shaderProgramType,
                    createShaderProgram(shaderProgramName + ".vsh", shaderProgramName + ".fsh"));
        }
    }

    public static void resolveFallbacks() {
        for (ShaderProgramType shaderProgramType : ShaderProgramType.values()) {
            ShaderProgramType current = shaderProgramType;
            while (shaderProgramId.get(current) == NO_PROGRAM_ID) {
                if (current.fallback == null || current == current.fallback) break;
                current = current.fallback;
            }
            shaderProgramId.put(shaderProgramType, shaderProgramId.get(current));
        }
    }

    private static int createShaderProgram(String vertShaderPath, String fragShaderPath) {
        int programId = GL20.glCreateProgram();

        if (programId == NO_PROGRAM_ID) return NO_PROGRAM_ID;

        int vertShaderId = ShaderCompiler.createVertShader(vertShaderPath);
        int fragShaderId = ShaderCompiler.createFragShader(fragShaderPath);

        if (vertShaderId != NO_PROGRAM_ID || fragShaderId != NO_PROGRAM_ID) {
            if (vertShaderId != NO_PROGRAM_ID) GL20.glAttachShader(programId, vertShaderId);
            if (fragShaderId != NO_PROGRAM_ID) GL20.glAttachShader(programId, fragShaderId);
            if (ShaderCore.entityAttrib >= 0)
                GL20.glBindAttribLocation(programId, ShaderCore.entityAttrib, "mc_Entity");
            GL20.glLinkProgram(programId);
            GL20.glValidateProgram(programId);
            GL.printLogInfo(programId);

            Uniforms.cacheUniformLocations(programId);
        } else {
            GL20.glDeleteProgram(programId);
            return NO_PROGRAM_ID;
        }

        return programId;
    }

    public static void useShaderProgram(ShaderProgramType shaderProgramType) {
        if (activeShaderProgram == shaderProgramType) return;

        if (ShadowMapManager.isShadowPass) {
            activeShaderProgram = ShaderProgramType.NONE;
            GL20.glUseProgram(shaderProgramId.get(ShaderProgramType.NONE));
            return;
        }

        activeShaderProgram = shaderProgramType;
        GL20.glUseProgram(shaderProgramId.get(shaderProgramType));

        if (shaderProgramId.get(shaderProgramType) == NO_PROGRAM_ID) return;

        int programId = ShaderProgram.shaderProgramId.get(ShaderProgram.activeShaderProgram);
        switch (shaderProgramType) {
            case TEXTURED:
                Uniforms.setProgramUniform1i(programId, ShaderUniform.TEXTURE, 0);
                break;
            case TEXTURED_LIT:
            case HAND:
            case WEATHER:
                Uniforms.setProgramUniform1i(programId, ShaderUniform.TEXTURE, 0);
                Uniforms.setProgramUniform1i(programId, ShaderUniform.LIGHTMAP, 1);
                break;
            case TERRAIN:
            case WATER:
                Uniforms.setProgramUniform1i(programId, ShaderUniform.TEXTURE, 0);
                Uniforms.setProgramUniform1i(programId, ShaderUniform.LIGHTMAP, 1);
                Uniforms.setProgramUniform1i(programId, ShaderUniform.NORMALS, 2);
                Uniforms.setProgramUniform1i(programId, ShaderUniform.SPECULAR, 3);
                break;
            case COMPOSITE:
            case FINAL:
                Uniforms.setProgramUniform1i(programId, ShaderUniform.GCOLOR, 0);
                Uniforms.setProgramUniform1i(programId, ShaderUniform.GDEPTH, 1);
                Uniforms.setProgramUniform1i(programId, ShaderUniform.GNORMAL, 2);
                Uniforms.setProgramUniform1i(programId, ShaderUniform.COMPOSITE, 3);
                Uniforms.setProgramUniform1i(programId, ShaderUniform.GAUX1, 4);
                Uniforms.setProgramUniform1i(programId, ShaderUniform.GAUX2, 5);
                Uniforms.setProgramUniform1i(programId, ShaderUniform.GAUX3, 6);
                Uniforms.setProgramUniform1i(programId, ShaderUniform.SHADOW, 7);

                Uniforms.setProgramUniformMatrix4(programId, ShaderUniform.GB_PREVIOUS_PROJECTION, MatrixUniforms.previousProjection);
                Uniforms.setProgramUniformMatrix4(programId, ShaderUniform.GB_PROJECTION, MatrixUniforms.projection);
                Uniforms.setProgramUniformMatrix4(programId, ShaderUniform.GB_PROJECTION_INVERSE, MatrixUniforms.projectionInverse);

                Uniforms.setProgramUniformMatrix4(programId, ShaderUniform.GB_PREVIOUS_MODELVIEW, MatrixUniforms.previousModelView);

                if (ShadowMapManager.shadowEnabled) {
                    Uniforms.setProgramUniformMatrix4(programId, ShaderUniform.SHADOW_PROJECTION, MatrixUniforms.shadowProjection);
                    Uniforms.setProgramUniformMatrix4(programId, ShaderUniform.SHADOW_PROJECTION_INVERSE, MatrixUniforms.shadowProjectionInverse);

                    Uniforms.setProgramUniformMatrix4(programId, ShaderUniform.SHADOW_MODELVIEW, MatrixUniforms.shadowModelView);
                    Uniforms.setProgramUniformMatrix4(programId, ShaderUniform.SHADOW_MODELVIEW_INVERSE, MatrixUniforms.shadowModelViewInverse);
                }
                break;
        }

        ItemStack stack = ShaderCore.MINECRAFT.player.inventory.getSelectedItem();
        Uniforms.setProgramUniform1i(programId, ShaderUniform.HELD_ITEM_ID, stack == null ? -1 : stack.itemId);
        Uniforms.setProgramUniform1i(programId, ShaderUniform.HELD_BLOCK_LIGHT_VALUE,
                stack == null || stack.itemId >= Block.BLOCKS.length ? 0 : Block.BLOCKS_LIGHT_LUMINANCE[stack.itemId]);

        Uniforms.setProgramUniform1i(programId, ShaderUniform.FOG_MODE, ShaderCore.fogEnabled ? GL11.glGetInteger(GL11.GL_FOG_MODE) : 0);
        Uniforms.setProgramUniform1f(programId, ShaderUniform.RAIN_STRENGTH, ShaderCore.rainStrength);

        Uniforms.setProgramUniform1i(programId, ShaderUniform.WORLD_TIME, (int) (ShaderCore.MINECRAFT.world.getTime() % 24000));

        Uniforms.setProgramUniform1f(programId, ShaderUniform.ASPECT_RATIO, ShaderCore.aspectRatio);
        Uniforms.setProgramUniform1f(programId, ShaderUniform.VIEW_WIDTH, (float) ShaderCore.renderWidth);
        Uniforms.setProgramUniform1f(programId, ShaderUniform.VIEW_HEIGHT, (float) ShaderCore.renderHeight);

        Uniforms.setProgramUniform1f(programId, ShaderUniform.NEAR, 0.05F);
        Uniforms.setProgramUniform1f(programId, ShaderUniform.FAR, 256 >> ShaderCore.MINECRAFT.options.viewDistance);

        Uniforms.setProgramUniform3f(programId, ShaderUniform.SUN_POSITION, PositionUniforms.sunPosition);
        Uniforms.setProgramUniform3f(programId, ShaderUniform.MOON_POSITION, PositionUniforms.moonPosition);

        Uniforms.setProgramUniform3f(programId, ShaderUniform.PREVIOUS_CAMERA_POSITION, PositionUniforms.previousCameraPosition);
        Uniforms.setProgramUniform3f(programId, ShaderUniform.CAMERA_POSITION, PositionUniforms.cameraPosition);

        Uniforms.setProgramUniformMatrix4(programId, ShaderUniform.GB_MODELVIEW, MatrixUniforms.modelView);
        Uniforms.setProgramUniformMatrix4(programId, ShaderUniform.GB_MODELVIEW_INVERSE, MatrixUniforms.modelViewInverse);
    }

    public static void deleteShaders() {
        for (Map.Entry<ShaderProgramType, Integer> shaderEntry : shaderProgramId.entrySet()) {
            int programId = shaderEntry.getValue();
            if (programId != NO_PROGRAM_ID) {
                GL20.glDeleteProgram(programId);
                shaderEntry.setValue(NO_PROGRAM_ID);
            }
        }
    }
}