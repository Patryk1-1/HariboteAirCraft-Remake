package com.haribote.hac.entity;

import com.haribote.hac.scanner.CapturedBlock;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class AircraftBlockAccess implements IBlockAccess {
    private final EntityHacAircraft aircraft;
    private final Map<BlockPos, CapturedBlock> blockMap = new HashMap<>();
    private final Map<BlockPos, TileEntity> tileEntityMap = new HashMap<>();

    public AircraftBlockAccess(EntityHacAircraft aircraft) {
        this.aircraft = aircraft;
        if (aircraft != null && aircraft.getBlocks() != null) {
            for (CapturedBlock cb : aircraft.getBlocks()) {
                BlockPos relPos = new BlockPos(cb.dx, cb.dy, cb.dz);
                blockMap.put(relPos, cb);
                if (cb.tileNBT != null && aircraft.world != null) {
                    try {
                        TileEntity te = TileEntity.create(aircraft.world, cb.tileNBT);
                        if (te != null) {
                            te.setPos(relPos);
                            te.setWorld(aircraft.world);
                            if (cb.state != null) {
                                try {
                                    int meta = cb.state.getBlock().getMetaFromState(cb.state);
                                    ObfuscationReflectionHelper.setPrivateValue(TileEntity.class, te, meta, "blockMetadata", "field_145847_g");
                                } catch (Throwable ignored) {}
                            }
                            tileEntityMap.put(relPos, te);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        return tileEntityMap.get(pos);
    }

    public Map<BlockPos, TileEntity> getTileEntityMap() {
        return tileEntityMap;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getCombinedLight(BlockPos pos, int lightValue) {
        if (aircraft != null && aircraft.world != null) {
            double rad = Math.toRadians(-aircraft.rotationYaw);
            double cos = Math.cos(rad);
            double sin = Math.sin(rad);
            double rx = pos.getX() * cos - pos.getZ() * sin;
            double rz = pos.getX() * sin + pos.getZ() * cos;
            BlockPos worldPos = new BlockPos(aircraft.posX + rx, aircraft.posY + pos.getY(), aircraft.posZ + rz);
            return aircraft.world.getCombinedLight(worldPos, lightValue);
        }
        return 15 << 20 | 15 << 4;
    }

    @Override
    public IBlockState getBlockState(BlockPos pos) {
        CapturedBlock cb = blockMap.get(pos);
        if (cb != null && cb.state != null) {
            return cb.state;
        }
        return Blocks.AIR.getDefaultState();
    }

    @Override
    public boolean isAirBlock(BlockPos pos) {
        IBlockState state = getBlockState(pos);
        return state.getBlock().isAir(state, this, pos);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Biome getBiome(BlockPos pos) {
        return Biomes.PLAINS;
    }

    @Override
    public int getStrongPower(BlockPos pos, EnumFacing direction) {
        return 0;
    }

    @Override
    public WorldType getWorldType() {
        return WorldType.DEFAULT;
    }

    @Override
    public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean _default) {
        IBlockState state = getBlockState(pos);
        return state.isSideSolid(this, pos, side);
    }
}
