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
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import com.haribote.hac.block.BlockPyxis;
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

        float deltaYaw = entity.rotationYaw - entity.initialYaw;
        float prevDeltaYaw = entity.prevRotationYaw - entity.initialYaw;
        float yaw = prevDeltaYaw + (deltaYaw - prevDeltaYaw) * partialTicks;
        GlStateManager.rotate(-yaw, 0.0F, 1.0F, 0.0F);

        com.haribote.hac.entity.AircraftBlockAccess blockAccess = entity.getBlockAccess();

        if (!blocks.isEmpty()) {
            this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
            RenderHelper.disableStandardItemLighting();

            BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
            int brightness = entity.getBrightnessForRender();

            // PASS 1: Solid & Cutout (opaque blocks, doors, glass, panes, stairs, pyxis, etc.)
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
            GlStateManager.depthMask(true);

            renderBlockPass(blocks, blockAccess, dispatcher, brightness, false, entity);

            // PASS 2: Translucent (stained glass, stained glass panes, ice, slime, etc.)
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GlStateManager.enableAlpha();
            GlStateManager.alphaFunc(GL11.GL_GREATER, 0.003921569F);
            GlStateManager.depthMask(false);

            renderBlockPass(blocks, blockAccess, dispatcher, brightness, true, entity);

            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            RenderHelper.enableStandardItemLighting();
        }

        // Render all TileEntities (VariedCommodities, Props, Decocraft, CustomNPCs, Chests, Signs, Banners, Heads)
        if (blockAccess != null && !blockAccess.getTileEntityMap().isEmpty()) {
            RenderHelper.enableStandardItemLighting();
            for (java.util.Map.Entry<BlockPos, net.minecraft.tileentity.TileEntity> entry : blockAccess.getTileEntityMap().entrySet()) {
                BlockPos relPos = entry.getKey();
                net.minecraft.tileentity.TileEntity te = entry.getValue();
                if (te != null) {
                    double tx = relPos.getX() - 0.5D;
                    double ty = relPos.getY();
                    double tz = relPos.getZ() - 0.5D;
                    try {
                        net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher.instance.render(te, tx, ty, tz, partialTicks);
                    } catch (Exception ignored) {}
                }
            }
        }

        // Render compass arrows on top of Pyxis aligned with world cardinal directions
        for (CapturedBlock cb : blocks) {
            if (cb.state != null && cb.state.getBlock() instanceof BlockPyxis) {
                renderCompassArrows(cb.dx, cb.dy + 0.505D, cb.dz, yaw + entity.initialYaw);
            }
        }

        GlStateManager.popMatrix();
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    private boolean isTranslucent(IBlockState state) {
        try {
            return state.getBlock().canRenderInLayer(state, BlockRenderLayer.TRANSLUCENT);
        } catch (Throwable t) {
            return state.getBlock().getRenderLayer() == BlockRenderLayer.TRANSLUCENT;
        }
    }

    private void renderBlockPass(List<CapturedBlock> blocks, com.haribote.hac.entity.AircraftBlockAccess blockAccess,
                                 BlockRendererDispatcher dispatcher, int brightness, boolean translucentPass,
                                 EntityHacAircraft entity) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        boolean started = false;

        for (CapturedBlock cb : blocks) {
            IBlockState state = cb.state;
            if (state == null || state.getRenderType() != EnumBlockRenderType.MODEL) {
                continue;
            }

            if (isTranslucent(state) != translucentPass) {
                continue;
            }

            // Compute actual state (resolves wall connections, fences, panes, stairs)
            IBlockState renderState = state;
            try {
                renderState = state.getBlock().getActualState(state, blockAccess, cb.getRelativePos());
            } catch (Exception ignored) {}

            IBakedModel model = dispatcher.getModelForState(renderState);
            if (model == null) continue;

            if (!started) {
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
                buffer.setTranslation(-0.5D, 0.0D, -0.5D);
                started = true;
            }

            try {
                dispatcher.getBlockModelRenderer().renderModel(blockAccess, model, renderState, cb.getRelativePos(), buffer, false);
            } catch (Exception ex) {
                double bx = cb.dx;
                double by = cb.dy;
                double bz = cb.dz;
                for (EnumFacing facing : EnumFacing.VALUES) {
                    for (BakedQuad quad : model.getQuads(renderState, facing, 0L)) {
                        buffer.addVertexData(quad.getVertexData());
                        buffer.putPosition(bx, by, bz);
                        buffer.putBrightness4(brightness, brightness, brightness, brightness);
                    }
                }
                for (BakedQuad quad : model.getQuads(renderState, null, 0L)) {
                    buffer.addVertexData(quad.getVertexData());
                    buffer.putPosition(bx, by, bz);
                    buffer.putBrightness4(brightness, brightness, brightness, brightness);
                }
            }
        }

        if (started) {
            buffer.setTranslation(0.0D, 0.0D, 0.0D);
            tessellator.draw();
        }
    }

    private void renderCompassArrows(double px, double py, double pz, float yaw) {
        this.bindTexture(ROTOR_TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.translate(px, py, pz);
        // Rotate by +yaw to cancel aircraft's -yaw rotation, keeping arrows aligned with world cardinal directions!
        GlStateManager.rotate(yaw, 0.0F, 1.0F, 0.0F);

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        double s = 0.5D;

        // Top face
        buffer.pos(-s, 0.0D, -s).tex(0.0D, 0.0D).endVertex();
        buffer.pos(-s, 0.0D,  s).tex(0.0D, 1.0D).endVertex();
        buffer.pos( s, 0.0D,  s).tex(1.0D, 1.0D).endVertex();
        buffer.pos( s, 0.0D, -s).tex(1.0D, 0.0D).endVertex();

        // Bottom face
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
