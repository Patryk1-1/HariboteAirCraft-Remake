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

            com.haribote.hac.entity.AircraftBlockAccess blockAccess = entity.getBlockAccess();

            for (CapturedBlock cb : blocks) {
                IBlockState state = cb.state;
                if (state == null || state.getRenderType() != EnumBlockRenderType.MODEL) {
                    continue;
                }

                // Compute actual state (resolves wall connections, fences, panes, stairs)
                IBlockState renderState = state;
                try {
                    renderState = state.getBlock().getActualState(state, blockAccess, cb.getRelativePos());
                } catch (Exception ignored) {}

                IBakedModel model = dispatcher.getModelForState(renderState);
                double bx = cb.dx - 0.5D;
                double by = cb.dy;
                double bz = cb.dz - 0.5D;

                int color = Minecraft.getMinecraft().getBlockColors().colorMultiplier(renderState, entity.world, new BlockPos(entity.posX + cb.dx, entity.posY + cb.dy, entity.posZ + cb.dz), 0);
                float red = (float) ((color >> 16) & 255) / 255.0F;
                float green = (float) ((color >> 8) & 255) / 255.0F;
                float blue = (float) (color & 255) / 255.0F;

                for (EnumFacing facing : EnumFacing.VALUES) {
                    for (BakedQuad quad : model.getQuads(renderState, facing, 0L)) {
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

                for (BakedQuad quad : model.getQuads(renderState, null, 0L)) {
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

        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityHacAircraft entity) {
        return TextureMap.LOCATION_BLOCKS_TEXTURE;
    }
}
