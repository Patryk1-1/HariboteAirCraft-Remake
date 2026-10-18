package com.haribote.hac.network;

import com.haribote.hac.client.ClientMountHelper;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketMountAircraft implements IMessage {
    public int entityId;

    public PacketMountAircraft() {}

    public PacketMountAircraft(int entityId) {
        this.entityId = entityId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
    }

    public static class Handler implements IMessageHandler<PacketMountAircraft, IMessage> {
        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketMountAircraft message, MessageContext ctx) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                ClientMountHelper.queueMount(message.entityId);
            });
            return null;
        }
    }
}
