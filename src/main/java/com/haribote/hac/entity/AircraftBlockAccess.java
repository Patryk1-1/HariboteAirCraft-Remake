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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class AircraftBlockAccess implements IBlockAccess {
    private final EntityHacAircraft aircraft;
    private final Map<BlockPos, CapturedBlock> blockMap = new HashMap<>();

    public AircraftBlockAccess(EntityHacAircraft aircraft) {
        this.aircraft = aircraft;
        if (aircraft != null && aircraft.getBlocks() != null) {
            for (CapturedBlock cb : aircraft.getBlocks()) {
                blockMap.put(new BlockPos(cb.dx, cb.dy, cb.dz), cb);
            }
        }
    }

    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        return null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getCombinedLight(BlockPos pos, int lightValue) {
        if (aircraft != null && aircraft.world != null) {
            return aircraft.world.getCombinedLight(pos, lightValue);
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
