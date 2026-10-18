package com.haribote.hac.client;

import com.haribote.hac.HACMod;
import com.haribote.hac.entity.EntityHacAircraft;
import com.haribote.hac.scanner.CapturedBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

import java.util.List;

@SideOnly(Side.CLIENT)
public class RenderHacAircraft extends Render<EntityHacAircraft> {
    private static final ResourceLocation ROTOR_TEXTURE = new ResourceLocation(HACMod.MODID, "textures/blocks/py_rotor.png");

    public RenderHacAircraft(RenderManager renderManager) {
        super(renderManager);
        this.shadowSize = 0.5F;
    }

    @Override
    public void doRender(EntityHacAircraft entity, double x, double y, double z, float entityYaw, float partialTicks) {
        List<CapturedBlock> blocks = entity.getBlocks();

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);

        float yaw = entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks;
        GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);

        if (!blocks.isEmpty()) {
            this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);

            BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
            int brightness = entity.getBrightnessForRender();
            if (brightness < 0x00F000F0) {
                brightness = 0x00F000F0;
            }

            for (CapturedBlock cb : blocks) {
                IBlockState state = cb.state;
                if (state == null || state.getRenderType() != EnumBlockRenderType.MODEL) {
                    continue;
                }

                IBakedModel model = dispatcher.getModelForState(state);
                double bx = cb.dx - 0.5D;
                double by = cb.dy;
                double bz = cb.dz - 0.5D;

                int color = Minecraft.getMinecraft().getBlockColors().colorMultiplier(state, entity.world, new BlockPos(entity.posX + cb.dx, entity.posY + cb.dy, entity.posZ + cb.dz), 0);
                float red = (float) ((color >> 16) & 255) / 255.0F;
                float green = (float) ((color >> 8) & 255) / 255.0F;
                float blue = (float) (color & 255) / 255.0F;

                for (EnumFacing facing : EnumFacing.VALUES) {
                    for (BakedQuad quad : model.getQuads(state, facing, 0L)) {
                        buffer.addVertexData(quad.getVertexData());
                        buffer.putPosition(bx, by, bz);
                        buffer.putBrightness4(brightness, brightness, brightness, brightness);
                        if (quad.hasTintIndex()) {
                            buffer.putColorRGB_F4(red, green, blue);
                        } else {
                            buffer.putColorRGB_F4(1.0F, 1.0F, 1.0F);
                        }
                    }
                }

                for (BakedQuad quad : model.getQuads(state, null, 0L)) {
                    buffer.addVertexData(quad.getVertexData());
                    buffer.putPosition(bx, by, bz);
                    buffer.putBrightness4(brightness, brightness, brightness, brightness);
                    if (quad.hasTintIndex()) {
                        buffer.putColorRGB_F4(red, green, blue);
                    } else {
                        buffer.putColorRGB_F4(1.0F, 1.0F, 1.0F);
                    }
                }
            }

            tessellator.draw();
            RenderHelper.enableStandardItemLighting();
            GlStateManager.disableBlend();
        }

        // Render spinning Pyxis rotor on top
        float rotorAngle = entity.prevRotorAngle + (entity.rotorAngle - entity.prevRotorAngle) * partialTicks;
        renderRotor(rotorAngle);

        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    private void renderRotor(float rotorAngle) {
        this.bindTexture(ROTOR_TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.translate(0.0D, 0.52D, 0.0D);
        GlStateManager.rotate(rotorAngle, 0.0F, 1.0F, 0.0F);

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        double s = 0.5D;
        buffer.pos(-s, 0.0D, -s).tex(0.0D, 0.0D).endVertex();
        buffer.pos(-s, 0.0D,  s).tex(0.0D, 1.0D).endVertex();
        buffer.pos( s, 0.0D,  s).tex(1.0D, 1.0D).endVertex();
        buffer.pos( s, 0.0D, -s).tex(1.0D, 0.0D).endVertex();

        // Draw back side too so it's visible from below
        buffer.pos( s, 0.0D, -s).tex(1.0D, 0.0D).endVertex();
        buffer.pos( s, 0.0D,  s).tex(1.0D, 1.0D).endVertex();
        buffer.pos(-s, 0.0D,  s).tex(0.0D, 1.0D).endVertex();
        buffer.pos(-s, 0.0D, -s).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityHacAircraft entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}
