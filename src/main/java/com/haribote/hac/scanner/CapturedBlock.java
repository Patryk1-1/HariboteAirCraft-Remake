package com.haribote.hac.scanner;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;

public class CapturedBlock {
    public int dx;
    public int dy;
    public int dz;
    public IBlockState state;
    public NBTTagCompound tileNBT;

    public CapturedBlock(int dx, int dy, int dz, IBlockState state, NBTTagCompound tileNBT) {
        this.dx = dx;
        this.dy = dy;
        this.dz = dz;
        this.state = state;
        this.tileNBT = tileNBT;
    }

    public BlockPos getRelativePos() {
        return new BlockPos(dx, dy, dz);
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("dx", dx);
        tag.setInteger("dy", dy);
        tag.setInteger("dz", dz);
        tag.setInteger("blockId", Block.getStateId(state));
        if (tileNBT != null) {
            tag.setTag("tile", tileNBT);
        }
        return tag;
    }

    public static CapturedBlock readFromNBT(NBTTagCompound tag) {
        int dx = tag.getInteger("dx");
        int dy = tag.getInteger("dy");
        int dz = tag.getInteger("dz");
        int blockId = tag.getInteger("blockId");
        IBlockState state = Block.getStateById(blockId);
        NBTTagCompound tileNBT = tag.hasKey("tile") ? tag.getCompoundTag("tile") : null;
        return new CapturedBlock(dx, dy, dz, state, tileNBT);
    }

    public void writeToBuf(PacketBuffer buf) {
        buf.writeInt(dx);
        buf.writeInt(dy);
        buf.writeInt(dz);
        buf.writeInt(Block.getStateId(state));
        if (tileNBT != null) {
            buf.writeBoolean(true);
            buf.writeCompoundTag(tileNBT);
        } else {
            buf.writeBoolean(false);
        }
    }

    public static CapturedBlock readFromBuf(PacketBuffer buf) {
        int dx = buf.readInt();
        int dy = buf.readInt();
        int dz = buf.readInt();
        int blockId = buf.readInt();
        IBlockState state = Block.getStateById(blockId);
        NBTTagCompound tileNBT = null;
        try {
            if (buf.readBoolean()) {
                tileNBT = buf.readCompoundTag();
            }
        } catch (Exception ignored) {}
        return new CapturedBlock(dx, dy, dz, state, tileNBT);
    }
}
