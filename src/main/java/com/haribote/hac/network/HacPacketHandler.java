package com.haribote.hac.network;

import com.haribote.hac.HACMod;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class HacPacketHandler {
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(HACMod.MODID);
    private static int packetId = 0;

    public static void registerPackets() {
        INSTANCE.registerMessage(PacketPilotInput.Handler.class, PacketPilotInput.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(PacketMountAircraft.Handler.class, PacketMountAircraft.class, packetId++, Side.CLIENT);
    }
}
