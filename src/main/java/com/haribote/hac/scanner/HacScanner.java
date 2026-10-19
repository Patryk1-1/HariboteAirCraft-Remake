package com.haribote.hac.scanner;

import com.haribote.hac.entity.EntityHacAircraft;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

import java.util.*;

public class HacScanner {
    public static final int MAX_BLOCKS = 2048;

    public static void scanAndLaunch(World world, BlockPos origin, EntityPlayer pilot) {
        if (world.isRemote) {
            return;
        }

        IBlockState pyxisState = world.getBlockState(origin);
        EnumFacing pyxisFacing = EnumFacing.SOUTH;
        if (pyxisState.getBlock() instanceof com.haribote.hac.block.BlockPyxis) {
            pyxisFacing = pyxisState.getValue(com.haribote.hac.block.BlockPyxis.FACING);
        }
        float initialYaw = pyxisFacing.getHorizontalAngle();

        net.minecraft.util.Rotation toLocalRotation = net.minecraft.util.Rotation.NONE;
        if (pyxisFacing == EnumFacing.WEST) {
            toLocalRotation = net.minecraft.util.Rotation.COUNTERCLOCKWISE_90;
        } else if (pyxisFacing == EnumFacing.NORTH) {
            toLocalRotation = net.minecraft.util.Rotation.CLOCKWISE_180;
        } else if (pyxisFacing == EnumFacing.EAST) {
            toLocalRotation = net.minecraft.util.Rotation.CLOCKWISE_90;
        }

        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        List<CapturedBlock> captured = new ArrayList<>();
        List<BlockPos> scannedPositions = new ArrayList<>();

        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty() && captured.size() < MAX_BLOCKS) {
            BlockPos current = queue.poll();
            IBlockState state = world.getBlockState(current);
            Block block = state.getBlock();

            if (isIgnored(block, state)) {
                continue;
            }

            NBTTagCompound tileNBT = null;
            TileEntity te = world.getTileEntity(current);
            if (te != null) {
                tileNBT = new NBTTagCompound();
                te.writeToNBT(tileNBT);
            }

            int dx = current.getX() - origin.getX();
            int dy = current.getY() - origin.getY();
            int dz = current.getZ() - origin.getZ();

            int lx = dx;
            int lz = dz;
            if (pyxisFacing == EnumFacing.WEST) {
                lx = dz;
                lz = -dx;
            } else if (pyxisFacing == EnumFacing.NORTH) {
                lx = -dx;
                lz = -dz;
            } else if (pyxisFacing == EnumFacing.EAST) {
                lx = -dz;
                lz = dx;
            }

            IBlockState localState = state;
            if (toLocalRotation != net.minecraft.util.Rotation.NONE && localState != null) {
                try {
                    localState = localState.withRotation(toLocalRotation);
                } catch (Exception ignored) {}
            }

            captured.add(new CapturedBlock(lx, dy, lz, localState, tileNBT));
            scannedPositions.add(current);

            for (EnumFacing facing : EnumFacing.VALUES) {
                BlockPos neighbor = current.offset(facing);
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    IBlockState neighborState = world.getBlockState(neighbor);
                    if (!isIgnored(neighborState.getBlock(), neighborState)) {
                        queue.add(neighbor);
                    }
                }
            }
        }

        if (captured.isEmpty()) {
            return;
        }

        // Spawn Aircraft Entity
        EntityHacAircraft aircraft = new EntityHacAircraft(world, origin, captured, initialYaw);
        world.spawnEntity(aircraft);

        // Remove blocks safely from the world
        for (BlockPos blockPos : scannedPositions) {
            TileEntity te = world.getTileEntity(blockPos);
            if (te != null) {
                world.removeTileEntity(blockPos);
            }
            world.setBlockState(blockPos, Blocks.AIR.getDefaultState(), 2);
        }

        pilot.rotationYaw = initialYaw;
        pilot.prevRotationYaw = initialYaw;
        pilot.startRiding(aircraft, true);
        if (pilot instanceof net.minecraft.entity.player.EntityPlayerMP) {
            com.haribote.hac.network.HacPacketHandler.INSTANCE.sendTo(
                    new com.haribote.hac.network.PacketMountAircraft(aircraft.getEntityId()),
                    (net.minecraft.entity.player.EntityPlayerMP) pilot
            );
        }
        pilot.sendMessage(new net.minecraft.util.text.TextComponentString(
                net.minecraft.util.text.TextFormatting.GOLD + "[HariboteAirCraft] " +
                net.minecraft.util.text.TextFormatting.GREEN + "Wystartowano (" + captured.size() + " bloków)!\n" +
                net.minecraft.util.text.TextFormatting.YELLOW + "W/S: Przód/Tył | A/D: Na boki | Spacja: Góra | C / Ctrl: Dół | R: Lądowanie"
        ));
    }

    private static boolean isIgnored(Block block, IBlockState state) {
        if (block == Blocks.AIR) return true;
        if (block == Blocks.BEDROCK) return true;
        if (block == Blocks.BARRIER) return true;
        if (block == Blocks.PORTAL || block == Blocks.END_PORTAL || block == Blocks.END_PORTAL_FRAME) return true;
        Material material = state.getMaterial();
        return material == Material.WATER || material == Material.LAVA;
    }
}
